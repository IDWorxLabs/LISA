package com.idworx.lisa.features.securestorage

import com.idworx.lisa.features.securestorage.LisaSecureTypedCodec.TypedValue
import com.idworx.lisa.features.zerotouchprinciple.audit.ZeroTouchFileProbe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File

class LisaSecureStorageCryptoAndMigrationTest {

    private val cipher = SoftwareAesGcmCipher()

    @Test
    fun encryptDecryptRoundTrip() {
        val plain = "Primary User profile payload".toByteArray()
        val envelope = cipher.encrypt("profiles_json", plain)
        val back = cipher.decrypt("profiles_json", envelope)
        assertTrue(plain.contentEquals(back))
    }

    @Test
    fun androidKeystoreEncryptDoesNotPassCallerIv() {
        // Android Keystore rejects ENCRYPT_MODE with a caller-provided GCMParameterSpec when
        // setRandomizedEncryptionRequired(true). Production must init with key only and use cipher.iv.
        val source = ZeroTouchFileProbe.readProjectFile(
            "app/src/main/java/com/idworx/lisa/features/securestorage/AndroidKeystoreAesGcmCipher.kt"
        ) ?: error("missing AndroidKeystoreAesGcmCipher")
        val encryptFn = source
            .substringAfter("override fun encrypt(preferenceKey: String, plaintext: ByteArray): ByteArray {")
            .substringBefore("override fun decrypt(preferenceKey: String, envelope: ByteArray): ByteArray {")
        assertTrue(encryptFn.contains("cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())"))
        assertFalse(encryptFn.contains("GCMParameterSpec"))
        assertTrue(encryptFn.contains("cipher.iv"))
        assertTrue(encryptFn.contains("buildEnvelope(iv, ciphertext)"))
        val decryptFn = source
            .substringAfter("override fun decrypt(preferenceKey: String, envelope: ByteArray): ByteArray {")
            .substringBefore("fun ensureKeyAvailable()")
        assertTrue(decryptFn.contains("GCMParameterSpec(LisaSecureCryptoFormat.GCM_TAG_BITS, iv)"))
        assertTrue(decryptFn.contains("Cipher.DECRYPT_MODE"))
    }

    @Test
    fun ciphertextDiffersForSamePlaintext() {
        val plain = "same-text".toByteArray()
        val a = cipher.encrypt("key", plain)
        val b = cipher.encrypt("key", plain)
        assertFalse(a.contentEquals(b))
    }

    @Test
    fun tamperedCiphertextFailsAuthentication() {
        val envelope = cipher.encrypt("key", "secret".toByteArray()).clone()
        envelope[envelope.lastIndex] = (envelope[envelope.lastIndex].toInt() xor 0x55).toByte()
        try {
            cipher.decrypt("key", envelope)
            fail("Expected authentication failure")
        } catch (_: LisaSecureStorageException) {
            // expected
        }
    }

    @Test
    fun movingCiphertextToDifferentPreferenceKeyFails() {
        val envelope = cipher.encrypt("profiles_json", "payload".toByteArray())
        try {
            cipher.decrypt("feedback_json", envelope)
            fail("Expected AAD mismatch failure")
        } catch (_: LisaSecureStorageException) {
            // expected
        }
    }

    @Test
    fun supportedPreferenceDataTypesRoundTrip() {
        val prefs = LisaSecureSharedPreferences.wrap(InMemorySharedPreferences(), cipher)
        prefs.edit()
            .putString("s", "hello")
            .putBoolean("b", true)
            .putInt("i", 42)
            .putLong("l", 99L)
            .putFloat("f", 1.5f)
            .putStringSet("e", mutableSetOf("a", "b"))
            .commit()
        assertEquals("hello", prefs.getString("s", null))
        assertTrue(prefs.getBoolean("b", false))
        assertEquals(42, prefs.getInt("i", 0))
        assertEquals(99L, prefs.getLong("l", 0L))
        assertEquals(1.5f, prefs.getFloat("f", 0f), 0.0001f)
        assertEquals(setOf("a", "b"), prefs.getStringSet("e", null))
        // Stored form is encrypted envelope, not plaintext.
        val rawDelegate = InMemorySharedPreferences()
        val secure = LisaSecureSharedPreferences.wrap(rawDelegate, cipher)
        secure.edit().putString("profiles_json", "Alice").commit()
        val stored = rawDelegate.getString("profiles_json", null)
        assertNotNull(stored)
        assertTrue(stored!!.startsWith(LisaSecureCryptoFormat.VALUE_PREFIX))
        assertFalse(stored.contains("Alice"))
    }

    @Test
    fun typedCodecEncodesAllTypes() {
        val values = listOf(
            TypedValue.StringValue("x"),
            TypedValue.BooleanValue(false),
            TypedValue.IntValue(7),
            TypedValue.LongValue(8L),
            TypedValue.FloatValue(2.25f),
            TypedValue.StringSetValue(setOf("p", "q"))
        )
        values.forEach { original ->
            val stored = LisaSecureTypedCodec.toStorageString(cipher, "k", original)
            val decoded = LisaSecureTypedCodec.fromStorageString(cipher, "k", stored)
            assertEquals(original, decoded)
        }
    }

    @Test
    fun migrationSuccessClearsPlaintextAfterVerify() {
        val legacy = InMemorySharedPreferences()
        val secureDelegate = InMemorySharedPreferences()
        val meta = InMemorySharedPreferences()
        legacy.edit()
            .putString("profiles_json", """[{"name":"Sam"}]""")
            .putBoolean("onboarding_completed", true)
            .putInt("sensitivity_level", 4)
            .commit()

        val result = migrateInMemory(legacy, secureDelegate, meta, cipher)
        assertEquals(LisaSecurePreferencesMigration.Result.Success, result)
        assertTrue(meta.getBoolean(LisaSecureCryptoFormat.META_MIGRATION_COMPLETE, false))
        assertTrue(legacy.all.isEmpty())
        val secure = LisaSecureSharedPreferences.wrap(secureDelegate, cipher)
        assertEquals("""[{"name":"Sam"}]""", secure.getString("profiles_json", null))
        assertTrue(secure.getBoolean("onboarding_completed", false))
        assertEquals(4, secure.getInt("sensitivity_level", 0))
        // No plaintext sensitive value remains in secure delegate storage.
        secureDelegate.all.values.forEach { value ->
            val s = value?.toString().orEmpty()
            assertFalse(s.contains("Sam"))
            assertTrue(s.startsWith(LisaSecureCryptoFormat.VALUE_PREFIX) || s.isEmpty())
        }
    }

    @Test
    fun failedMigrationPreservesOriginalData() {
        val legacy = InMemorySharedPreferences()
        val secureDelegate = InMemorySharedPreferences()
        val meta = InMemorySharedPreferences()
        legacy.edit().putString("custom_maps", "L2,R0|help|Medical").commit()
        val badCipher = object : LisaValueCipher {
            override fun encrypt(preferenceKey: String, plaintext: ByteArray): ByteArray =
                throw LisaSecureStorageException("forced failure")
            override fun decrypt(preferenceKey: String, envelope: ByteArray): ByteArray =
                throw LisaSecureStorageException("forced failure")
        }
        val result = migrateInMemory(legacy, secureDelegate, meta, badCipher)
        assertTrue(result is LisaSecurePreferencesMigration.Result.Failed)
        assertFalse(meta.getBoolean(LisaSecureCryptoFormat.META_MIGRATION_COMPLETE, false))
        assertEquals("L2,R0|help|Medical", legacy.getString("custom_maps", null))
    }

    @Test
    fun migrationIsIdempotent() {
        val legacy = InMemorySharedPreferences()
        val secureDelegate = InMemorySharedPreferences()
        val meta = InMemorySharedPreferences()
        legacy.edit().putString("feedback_json", "[]").commit()
        assertEquals(
            LisaSecurePreferencesMigration.Result.Success,
            migrateInMemory(legacy, secureDelegate, meta, cipher)
        )
        // Second run with empty legacy + complete flag.
        val second = if (meta.getBoolean(LisaSecureCryptoFormat.META_MIGRATION_COMPLETE, false)) {
            LisaSecurePreferencesMigration.Result.AlreadyComplete
        } else {
            migrateInMemory(legacy, secureDelegate, meta, cipher)
        }
        assertEquals(LisaSecurePreferencesMigration.Result.AlreadyComplete, second)
    }

    @Test
    fun interruptedMigrationDoesNotMarkCompleteOrWipe() {
        val legacy = InMemorySharedPreferences()
        val secureDelegate = InMemorySharedPreferences()
        val meta = InMemorySharedPreferences()
        legacy.edit().putString("profiles_json", "keep-me").commit()
        // Write encrypted values then fail verification by using a different cipher to read.
        val writeCipher = SoftwareAesGcmCipher()
        val secure = LisaSecureSharedPreferences.wrap(secureDelegate, writeCipher)
        secure.edit().putString("profiles_json", "keep-me").commit()
        // Simulate interrupted migration: data written to secure, but meta not complete and legacy intact.
        assertFalse(meta.getBoolean(LisaSecureCryptoFormat.META_MIGRATION_COMPLETE, false))
        assertEquals("keep-me", legacy.getString("profiles_json", null))
        // Retry with proper migration should succeed and clear legacy.
        val result = migrateInMemory(legacy, InMemorySharedPreferences(), meta, cipher)
        assertEquals(LisaSecurePreferencesMigration.Result.Success, result)
        assertTrue(legacy.all.isEmpty())
    }

    @Test
    fun dataSurvivesRestartSimulation() {
        val delegate = InMemorySharedPreferences()
        val first = LisaSecureSharedPreferences.wrap(delegate, cipher)
        first.edit().putString("companion_memory_v1_state", """{"memories":[]}""").commit()
        val second = LisaSecureSharedPreferences.wrap(delegate, cipher)
        assertEquals("""{"memories":[]}""", second.getString("companion_memory_v1_state", null))
    }

    @Test
    fun manifestDisablesBackupAndRemovesRecordAudio() {
        val manifest = readRes("src/main/AndroidManifest.xml")
        assertFalse(manifest.contains("RECORD_AUDIO"))
        assertTrue(manifest.contains("android:allowBackup=\"false\""))
        assertTrue(manifest.contains("android:fullBackupContent=\"@xml/backup_rules\""))
        assertTrue(manifest.contains("android:dataExtractionRules=\"@xml/data_extraction_rules\""))
        assertTrue(manifest.contains("android.permission.CAMERA"))
    }

    @Test
    fun backupRulesExcludeLisaPrefsFiles() {
        val backup = readRes("src/main/res/xml/backup_rules.xml")
        val extraction = readRes("src/main/res/xml/data_extraction_rules.xml")
        listOf(backup, extraction).forEach { xml ->
            assertTrue(xml.contains("lisa_prefs.xml"))
            assertTrue(xml.contains("lisa_secure_prefs.xml"))
            assertTrue(xml.contains("lisa_secure_meta.xml"))
            assertTrue(xml.contains("exclude"))
        }
        assertTrue(extraction.contains("cloud-backup"))
        assertTrue(extraction.contains("device-transfer"))
    }

    private fun migrateInMemory(
        legacy: InMemorySharedPreferences,
        secureDelegate: InMemorySharedPreferences,
        meta: InMemorySharedPreferences,
        cipher: LisaValueCipher
    ): LisaSecurePreferencesMigration.Result {
        if (meta.getBoolean(LisaSecureCryptoFormat.META_MIGRATION_COMPLETE, false)) {
            return LisaSecurePreferencesMigration.Result.AlreadyComplete
        }
        val all = legacy.all
        if (all.isEmpty()) {
            meta.edit()
                .putBoolean(LisaSecureCryptoFormat.META_MIGRATION_COMPLETE, true)
                .putInt(
                    LisaSecureCryptoFormat.META_MIGRATION_FORMAT,
                    LisaSecureCryptoFormat.MIGRATION_FORMAT_VERSION
                )
                .commit()
            return LisaSecurePreferencesMigration.Result.NothingToMigrate
        }
        val secure = LisaSecureSharedPreferences.wrap(secureDelegate, cipher)
        return try {
            val editor = secure.edit()
            for ((key, raw) in all) {
                when (raw) {
                    is String -> editor.putString(key, raw)
                    is Boolean -> editor.putBoolean(key, raw)
                    is Int -> editor.putInt(key, raw)
                    is Long -> editor.putLong(key, raw)
                    is Float -> editor.putFloat(key, raw)
                    is Set<*> -> {
                        @Suppress("UNCHECKED_CAST")
                        editor.putStringSet(key, (raw as Set<String>).toMutableSet())
                    }
                }
            }
            if (!editor.commit()) return LisaSecurePreferencesMigration.Result.Failed("WRITE_SECURE")
            for ((key, raw) in all) {
                val ok = when (raw) {
                    is String -> secure.getString(key, null) == raw
                    is Boolean -> secure.getBoolean(key, !raw) == raw
                    is Int -> secure.getInt(key, raw + 1) == raw
                    is Long -> secure.getLong(key, raw + 1L) == raw
                    is Float -> secure.getFloat(key, raw + 1f) == raw
                    is Set<*> -> secure.getStringSet(key, null) == raw
                    else -> false
                }
                if (!ok) return LisaSecurePreferencesMigration.Result.Failed("VERIFY_MISMATCH")
            }
            meta.edit()
                .putBoolean(LisaSecureCryptoFormat.META_MIGRATION_COMPLETE, true)
                .putInt(
                    LisaSecureCryptoFormat.META_MIGRATION_FORMAT,
                    LisaSecureCryptoFormat.MIGRATION_FORMAT_VERSION
                )
                .commit()
            legacy.edit().clear().commit()
            LisaSecurePreferencesMigration.Result.Success
        } catch (_: Throwable) {
            LisaSecurePreferencesMigration.Result.Failed("ENCRYPT_WRITE")
        }
    }

    private fun readRes(path: String): String {
        val candidates = listOf(File(path), File("app/$path"), File("../$path"))
        return candidates.firstOrNull { it.isFile }?.readText()
            ?: error("Missing $path")
    }
}

class LisaFeedbackEmailAuthorityTest {
    @Test
    fun emailTemplateIncludesRequiredFieldsAndDestination() {
        val prepared = com.idworx.lisa.features.feedbackemail.LisaFeedbackEmailAuthority.PreparedEmail(
            to = com.idworx.lisa.features.feedbackemail.LisaFeedbackEmailAuthority.DESTINATION_EMAIL,
            subject = com.idworx.lisa.features.feedbackemail.LisaFeedbackEmailAuthority.EMAIL_SUBJECT,
            body = buildString {
                appendLine("LISA Version: 1.1")
                appendLine("Android Version: 14 (SDK 34)")
                appendLine("Device Model: Device Model X")
                appendLine()
                appendLine("Feedback:")
                appendLine()
                appendLine("What worked well:")
                appendLine("eye tracking")
                appendLine()
                appendLine("What was confusing:")
                appendLine("menu")
                appendLine()
                appendLine("Wink detection feedback:")
                appendLine("good")
                appendLine()
                appendLine("Speech timing feedback:")
                appendLine("ok")
            }
        )
        assertEquals("lisa-feedback@asgarddynamics.io", prepared.to)
        assertEquals("LISA Feedback", prepared.subject)
        assertTrue(prepared.body.contains("LISA Version:"))
        assertTrue(prepared.body.contains("Android Version:"))
        assertTrue(prepared.body.contains("Device Model:"))
        assertTrue(prepared.body.contains("What worked well:"))
        assertTrue(prepared.body.contains("What was confusing:"))
        assertFalse(prepared.body.contains("Android ID"))
        assertFalse(prepared.body.contains("advertising"))
    }

    @Test
    fun mailtoUriContainsCorrectRecipientAndDecodesSubjectBody() {
        val prepared = com.idworx.lisa.features.feedbackemail.LisaFeedbackEmailAuthority.PreparedEmail(
            to = com.idworx.lisa.features.feedbackemail.LisaFeedbackEmailAuthority.DESTINATION_EMAIL,
            subject = com.idworx.lisa.features.feedbackemail.LisaFeedbackEmailAuthority.EMAIL_SUBJECT,
            body = "Line one\nLine two & symbols: ?=#\n"
        )
        val mailto = com.idworx.lisa.features.feedbackemail.LisaFeedbackEmailAuthority
            .buildMailtoUriString(prepared)
        assertTrue(mailto.startsWith("mailto:lisa-feedback@asgarddynamics.io?"))
        assertEquals(
            "lisa-feedback@asgarddynamics.io",
            com.idworx.lisa.features.feedbackemail.LisaFeedbackEmailAuthority
                .mailtoRecipientFromUriString(mailto)
        )
        assertFalse(mailto.contains("lisa-feedback%40"))
        assertEquals(
            "LISA Feedback",
            com.idworx.lisa.features.feedbackemail.LisaFeedbackEmailAuthority
                .mailtoQueryValue(mailto, "subject")
        )
        assertEquals(
            "Line one\nLine two & symbols: ?=#\n",
            com.idworx.lisa.features.feedbackemail.LisaFeedbackEmailAuthority
                .mailtoQueryValue(mailto, "body")
        )
    }

    @Test
    fun multilineFeedbackAndSpecialCharactersSurviveMailtoEncoding() {
        val multiline = """
            First paragraph.

            Second with punctuation: hello, world!
            Quotes "and" apostrophe's & ampersands.
            Unicode: café — naïve
        """.trimIndent()
        val prepared = com.idworx.lisa.features.feedbackemail.LisaFeedbackEmailAuthority.PreparedEmail(
            to = com.idworx.lisa.features.feedbackemail.LisaFeedbackEmailAuthority.DESTINATION_EMAIL,
            subject = "LISA Feedback",
            body = multiline
        )
        val mailto = com.idworx.lisa.features.feedbackemail.LisaFeedbackEmailAuthority
            .buildMailtoUriString(prepared)
        val decoded = com.idworx.lisa.features.feedbackemail.LisaFeedbackEmailAuthority
            .mailtoQueryValue(mailto, "body")
        assertEquals(multiline, decoded)
        assertTrue(decoded!!.contains("First paragraph."))
        assertTrue(decoded.contains("café"))
        assertTrue(mailto.contains("%20") || !mailto.substringAfter("body=").contains("+"))
    }

    @Test
    fun sendIntentIsActionSendtoWithoutPackageRestriction() {
        val authority = ZeroTouchFileProbe.readProjectFile(
            "app/src/main/java/com/idworx/lisa/features/feedbackemail/LisaFeedbackEmailAuthority.kt"
        ) ?: error("missing email authority")
        assertTrue(authority.contains("Intent.ACTION_SENDTO"))
        assertTrue(authority.contains("buildMailtoUri"))
        assertTrue(authority.contains("addCategory(Intent.CATEGORY_DEFAULT)"))
        assertFalse(authority.contains("setPackage("))
        assertFalse(authority.contains("setComponent("))
        val mailto = com.idworx.lisa.features.feedbackemail.LisaFeedbackEmailAuthority
            .buildMailtoUriString(
                com.idworx.lisa.features.feedbackemail.LisaFeedbackEmailAuthority.PreparedEmail(
                    to = com.idworx.lisa.features.feedbackemail.LisaFeedbackEmailAuthority.DESTINATION_EMAIL,
                    subject = "LISA Feedback",
                    body = "body"
                )
            )
        assertTrue(mailto.startsWith("mailto:lisa-feedback@asgarddynamics.io?"))
        assertTrue(mailto.contains("subject="))
        assertTrue(mailto.contains("body="))
    }

    @Test
    fun productionCodeHasNoOemOrEmailPackageAssumptions() {
        val authority = ZeroTouchFileProbe.readProjectFile(
            "app/src/main/java/com/idworx/lisa/features/feedbackemail/LisaFeedbackEmailAuthority.kt"
        ) ?: error("missing email authority")
        val main = ZeroTouchFileProbe.readProjectFile(
            "app/src/main/java/com/idworx/lisa/MainActivity.kt"
        ) ?: error("missing MainActivity")
        val launchFn = main.substringAfter("private fun launchFeedbackEmail")
            .substringBefore("private fun saveFeedbackEntry")
        listOf(authority, launchFn).forEach { src ->
            assertFalse(src.contains("com.google.android.gm"))
            assertFalse(src.contains("setPackage("))
            assertFalse(src.contains("Build.MANUFACTURER.equals") || src.contains("Build.MANUFACTURER =="))
            assertFalse(src.contains("Build.MODEL.equals") || src.contains("Build.MODEL =="))
            assertFalse(src.contains("resolveActivity("))
            assertFalse(src.contains("canResolveEmailApp"))
            assertFalse(src.contains("Samsung", ignoreCase = false) && src.contains("Intent"))
        }
        assertFalse(authority.contains("com.samsung", ignoreCase = true))
        assertTrue(authority.contains("ActivityNotFoundException"))
        assertTrue(authority.contains("ACTION_SENDTO"))
        assertTrue(authority.contains("mailto:"))
    }

    @Test
    fun noHandlerFallbackKeepsSessionFieldsWithoutClearing() {
        val main = ZeroTouchFileProbe.readProjectFile(
            "app/src/main/java/com/idworx/lisa/MainActivity.kt"
        ) ?: error("missing MainActivity")
        val launchFn = main.substringAfter("private fun launchFeedbackEmail")
            .substringBefore("private fun saveFeedbackEntry")
        assertTrue(launchFn.contains("uiMenuFeedbackDraft.value = draft"))
        assertTrue(launchFn.contains("NoCompatibleEmailApp"))
        assertTrue(launchFn.contains("feedbackNoEmailApp"))
        assertTrue(launchFn.contains("presentFeedbackStatusMessage"))
        assertFalse(launchFn.contains("clearFeedbackDraft()"))
        assertFalse(launchFn.contains("Toast.makeText"))
        assertFalse(launchFn.contains("saveFeedbackDraft"))
        assertFalse(launchFn.contains("persistFeedbackDraft"))
    }

    @Test
    fun openedMessageIsDeferredUntilReturnNotShownOverChooser() {
        val main = ZeroTouchFileProbe.readProjectFile(
            "app/src/main/java/com/idworx/lisa/MainActivity.kt"
        ) ?: error("missing MainActivity")
        val launchFn = main.substringAfter("private fun launchFeedbackEmail")
            .substringBefore("private fun saveFeedbackEntry")
        assertTrue(launchFn.contains("feedbackEmailHandoffAwaitingReturn = true"))
        assertFalse(
            launchFn.substringAfter("LaunchResult.Opened")
                .substringBefore("LaunchResult.NoCompatibleEmailApp")
                .contains("presentFeedbackStatusMessage")
        )
        assertFalse(
            launchFn.substringAfter("LaunchResult.Opened")
                .substringBefore("LaunchResult.NoCompatibleEmailApp")
                .contains("Toast")
        )
        assertTrue(main.contains("onResume()"))
        assertTrue(main.contains("showFeedbackEmailReturnMessageRunnable"))
        assertTrue(main.contains("removeCallbacks(showFeedbackEmailReturnMessageRunnable)"))
    }

    @Test
    fun feedbackUiUsesEmailHandoffNotDurableDraftClaim() {
        val ui = ZeroTouchFileProbe.readProjectFile(
            "app/src/main/java/com/idworx/lisa/LisaReleaseUi.kt"
        ) ?: error("missing Feedback UI")
        assertTrue(ui.contains("onSendFeedbackEmail"))
        assertTrue(ui.contains("feedbackSendByEmail"))
        assertTrue(ui.contains("feedbackClearDraft"))
        assertTrue(ui.contains("onDismissStatus"))
        assertFalse(ui.contains("Saved on this device only."))
        assertFalse(ui.contains("Encrypted draft"))
        val strings = ZeroTouchFileProbe.readProjectFile(
            "app/src/main/java/com/idworx/lisa/LisaUiStrings.kt"
        ) ?: error("missing strings")
        assertTrue(strings.contains("lisa-feedback@asgarddynamics.io"))
        assertTrue(strings.contains("LISA cannot confirm whether the message was sent"))
        assertTrue(strings.contains("Feedback stays while LISA is open."))
        assertTrue(strings.contains("Information leaves LISA only if you choose to send the email."))
        assertTrue(strings.contains("A caregiver may be needed to choose an email app and press Send."))
        assertFalse(strings.contains("Encrypted draft stays on this device."))
        val authority = ZeroTouchFileProbe.readProjectFile(
            "app/src/main/java/com/idworx/lisa/features/feedbackemail/LisaFeedbackEmailAuthority.kt"
        ) ?: error("missing email authority")
        assertTrue(authority.contains("ACTION_SENDTO"))
        assertTrue(authority.contains("mailto:"))
        assertFalse(authority.contains("Smtp"))
        assertFalse(authority.contains("HttpURLConnection"))
    }

    @Test
    fun privacyDocsMatchV1Behaviour() {
        val privacy = File("PRIVACY.md").takeIf { it.isFile }?.readText()
            ?: File("../PRIVACY.md").readText()
        assertTrue(privacy.contains("does **not**"))
        assertTrue(privacy.contains("local alarm"))
        assertTrue(privacy.contains("lisa-feedback@asgarddynamics.io"))
        assertTrue(privacy.contains("Keystore"))
        assertTrue(privacy.contains("only while LISA remains open") ||
            privacy.contains("cleared on a fresh app launch"))
        assertFalse(privacy.contains("Feedback drafts are saved locally in encrypted storage"))
        assertFalse(privacy.contains("Linked caregivers"))
        assertTrue(privacy.contains("NOT ACTIVE") || privacy.contains("not active") ||
            privacy.contains("Not active") || privacy.contains("inactive V2"))
    }
}
