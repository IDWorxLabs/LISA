package com.idworx.lisa.features.securestorage

import android.content.Context
import android.content.SharedPreferences
import com.idworx.lisa.features.securestorage.LisaSecureTypedCodec.TypedValue

/**
 * One-time plaintext [lisa_prefs] → encrypted [lisa_secure_prefs] migration.
 *
 * Idempotent. On failure: plaintext preserved, migration not marked complete, no partial wipe.
 * Does not loop endlessly when Keystore is unavailable — records a meta flag and stops retrying
 * until the process is restarted after the flag is cleared (or Keystore recovers on a later cold start
 * when the unavailable flag is absent).
 */
object LisaSecurePreferencesMigration {

    sealed class Result {
        data object AlreadyComplete : Result()
        data object NothingToMigrate : Result()
        data object Success : Result()
        data class Failed(val reasonCode: String) : Result()
        data object KeystoreUnavailable : Result()
    }

    fun isMigrationComplete(context: Context): Boolean {
        val meta = metaPrefs(context)
        return meta.getBoolean(LisaSecureCryptoFormat.META_MIGRATION_COMPLETE, false)
    }

    fun runIfNeeded(
        context: Context,
        cipherFactory: () -> LisaValueCipher = { AndroidKeystoreAesGcmCipher() }
    ): Result {
        val app = context.applicationContext
        val meta = metaPrefs(app)
        if (meta.getBoolean(LisaSecureCryptoFormat.META_MIGRATION_COMPLETE, false)) {
            return Result.AlreadyComplete
        }
        if (meta.getBoolean(LisaSecureCryptoFormat.META_KEYSTORE_UNAVAILABLE, false)) {
            return Result.KeystoreUnavailable
        }

        val cipher = try {
            val created = cipherFactory()
            if (created is AndroidKeystoreAesGcmCipher) {
                created.ensureKeyAvailable()
            }
            created
        } catch (_: Throwable) {
            meta.edit()
                .putBoolean(LisaSecureCryptoFormat.META_KEYSTORE_UNAVAILABLE, true)
                .commit()
            return Result.KeystoreUnavailable
        }

        val legacy = app.getSharedPreferences(
            LisaSecureCryptoFormat.LEGACY_PREFS_NAME,
            Context.MODE_PRIVATE
        )
        val all = legacy.all
        if (all.isEmpty()) {
            markComplete(meta)
            // Ensure secure prefs file exists.
            LisaSecureSharedPreferences.create(app, cipher)
            return Result.NothingToMigrate
        }

        val secureDelegate = app.getSharedPreferences(
            LisaSecureCryptoFormat.SECURE_PREFS_NAME,
            Context.MODE_PRIVATE
        )
        val secure = LisaSecureSharedPreferences.wrap(secureDelegate, cipher)

        val snapshot = LinkedHashMap<String, TypedValue>()
        try {
            for ((key, raw) in all) {
                if (key == null || raw == null) continue
                snapshot[key] = toTyped(raw)
            }
        } catch (_: Throwable) {
            return Result.Failed("READ_LEGACY")
        }

        try {
            val editor = secure.edit()
            for ((key, typed) in snapshot) {
                when (typed) {
                    is TypedValue.StringValue -> editor.putString(key, typed.value)
                    is TypedValue.BooleanValue -> editor.putBoolean(key, typed.value)
                    is TypedValue.IntValue -> editor.putInt(key, typed.value)
                    is TypedValue.LongValue -> editor.putLong(key, typed.value)
                    is TypedValue.FloatValue -> editor.putFloat(key, typed.value)
                    is TypedValue.StringSetValue -> editor.putStringSet(key, typed.value.toMutableSet())
                }
            }
            if (!editor.commit()) {
                return Result.Failed("WRITE_SECURE")
            }
        } catch (_: Throwable) {
            return Result.Failed("ENCRYPT_WRITE")
        }

        // Verify every entry round-trips before touching plaintext.
        try {
            for ((key, original) in snapshot) {
                val readBack = when (original) {
                    is TypedValue.StringValue ->
                        TypedValue.StringValue(secure.getString(key, null) ?: return Result.Failed("VERIFY"))
                    is TypedValue.BooleanValue ->
                        TypedValue.BooleanValue(secure.getBoolean(key, !original.value))
                    is TypedValue.IntValue ->
                        TypedValue.IntValue(secure.getInt(key, original.value + 1))
                    is TypedValue.LongValue ->
                        TypedValue.LongValue(secure.getLong(key, original.value + 1L))
                    is TypedValue.FloatValue ->
                        TypedValue.FloatValue(secure.getFloat(key, original.value + 1f))
                    is TypedValue.StringSetValue ->
                        TypedValue.StringSetValue(
                            secure.getStringSet(key, null)?.toSet() ?: return Result.Failed("VERIFY")
                        )
                }
                if (!typedEquals(original, readBack)) {
                    return Result.Failed("VERIFY_MISMATCH")
                }
            }
        } catch (_: Throwable) {
            return Result.Failed("VERIFY_EXCEPTION")
        }

        // Only after full verification: mark complete, then clear plaintext.
        if (!markComplete(meta)) {
            return Result.Failed("MARK_COMPLETE")
        }
        legacy.edit().clear().commit()
        return Result.Success
    }

    private fun markComplete(meta: SharedPreferences): Boolean =
        meta.edit()
            .putBoolean(LisaSecureCryptoFormat.META_MIGRATION_COMPLETE, true)
            .putInt(
                LisaSecureCryptoFormat.META_MIGRATION_FORMAT,
                LisaSecureCryptoFormat.MIGRATION_FORMAT_VERSION
            )
            .remove(LisaSecureCryptoFormat.META_KEYSTORE_UNAVAILABLE)
            .commit()

    private fun metaPrefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(
            LisaSecureCryptoFormat.META_PREFS_NAME,
            Context.MODE_PRIVATE
        )

    private fun toTyped(raw: Any): TypedValue = when (raw) {
        is String -> TypedValue.StringValue(raw)
        is Boolean -> TypedValue.BooleanValue(raw)
        is Int -> TypedValue.IntValue(raw)
        is Long -> TypedValue.LongValue(raw)
        is Float -> TypedValue.FloatValue(raw)
        is Set<*> -> {
            @Suppress("UNCHECKED_CAST")
            TypedValue.StringSetValue((raw as Set<String>).toSet())
        }
        else -> throw LisaSecureStorageException("Unsupported preference type")
    }

    private fun typedEquals(a: TypedValue, b: TypedValue): Boolean = when {
        a is TypedValue.StringValue && b is TypedValue.StringValue -> a.value == b.value
        a is TypedValue.BooleanValue && b is TypedValue.BooleanValue -> a.value == b.value
        a is TypedValue.IntValue && b is TypedValue.IntValue -> a.value == b.value
        a is TypedValue.LongValue && b is TypedValue.LongValue -> a.value == b.value
        a is TypedValue.FloatValue && b is TypedValue.FloatValue ->
            a.value.toBits() == b.value.toBits()
        a is TypedValue.StringSetValue && b is TypedValue.StringSetValue -> a.value == b.value
        else -> false
    }
}
