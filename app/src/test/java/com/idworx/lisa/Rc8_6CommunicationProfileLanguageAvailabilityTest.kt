package com.idworx.lisa

import com.idworx.lisa.features.zerotouchprinciple.audit.ZeroTouchFileProbe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * RC8.6 — Communication Profile language availability (English only in Version 1;
 * Afrikaans/isiZulu visible as Version 2 options without system TTS routing).
 */
class Rc8_6CommunicationProfileLanguageAvailabilityTest {

    private val english = LisaUiStrings.forLanguage(PreferredLanguage.English)

    private fun read(relativeUnderMainJava: String): String {
        val path = "app/src/main/java/com/idworx/lisa/$relativeUnderMainJava"
        return ZeroTouchFileProbe.readProjectFile(path)
            ?: error("Missing source file: $path")
    }

    private fun sampleProfile(
        language: PreferredLanguage = PreferredLanguage.English
    ) = LisaUserProfile(
        id = "primary",
        name = "Primary User",
        preferredLanguage = language,
        communicationLevel = CommunicationLevel.Beginner
    )

    @Test
    fun englishIsTheOnlySelectableLanguageInVersion1() {
        assertEquals(listOf(PreferredLanguage.English), PreferredLanguage.selectable)
        assertEquals(
            listOf(PreferredLanguage.English),
            LisaLanguageAvailabilityAuthority.version1SelectableLanguages
        )
        assertTrue(PreferredLanguage.English.isSelectableInVersion1)
        assertFalse(PreferredLanguage.Afrikaans.isSelectableInVersion1)
        assertFalse(PreferredLanguage.IsiZulu.isSelectableInVersion1)
    }

    @Test
    fun afrikaansAndIsiZuluRemainVisibleAsVersion2Languages() {
        val displayed = LisaLanguageAvailabilityAuthority.displayedLanguages
        assertTrue(PreferredLanguage.Afrikaans in displayed)
        assertTrue(PreferredLanguage.IsiZulu in displayed)
        assertTrue(PreferredLanguage.English in displayed)

        val actions = CommunicationProfileDestinationActionAuthority.actions(
            profiles = listOf(sampleProfile()),
            activeProfileId = "primary",
            uiStrings = english
        )
        val af = actions.first { it.id == MenuDestinationActionId.language("Afrikaans") }
        val zu = actions.first { it.id == MenuDestinationActionId.language("isiZulu") }
        val en = actions.first { it.id == MenuDestinationActionId.language("English") }

        assertEquals(english.languageVersion2StatusShort, af.supportingText)
        assertEquals(english.languageVersion2StatusShort, zu.supportingText)
        assertEquals(null, en.supportingText)
        assertTrue(en.selected)
        assertFalse(af.selected)
        assertFalse(zu.selected)
        assertTrue(af.canReceiveFocus)
        assertTrue(zu.canReceiveFocus)
    }

    @Test
    fun englishRemainsSelectedAndVersion2OptionsCannotBecomeActive() {
        val afrikaansProfile = sampleProfile(PreferredLanguage.Afrikaans)
        val actions = CommunicationProfileDestinationActionAuthority.actions(
            profiles = listOf(afrikaansProfile),
            activeProfileId = "primary",
            uiStrings = english
        )
        val en = actions.first { it.id == MenuDestinationActionId.language("English") }
        assertTrue(en.selected)
        // Catalog never marks Version 2 languages as the active check selection.
        assertFalse(
            actions.any {
                it.id == MenuDestinationActionId.language("Afrikaans") && it.selected
            }
        )
        assertFalse(
            actions.any {
                it.id == MenuDestinationActionId.language("isiZulu") && it.selected
            }
        )

        assertEquals(
            PreferredLanguage.English,
            LisaLanguageAvailabilityAuthority.coerceForVersion1(PreferredLanguage.Afrikaans)
        )
        assertEquals(
            PreferredLanguage.English,
            LisaLanguageAvailabilityAuthority.coerceForVersion1(PreferredLanguage.IsiZulu)
        )
        assertEquals(
            PreferredLanguage.English,
            LisaLanguageAvailabilityAuthority.coerceForVersion1(PreferredLanguage.English)
        )
    }

    @Test
    fun activatingFutureLanguageShowsVersion2InfoMessageWithoutChangingProfile() {
        val main = read("MainActivity.kt")
        val blockStart = main.indexOf("actionId.value.startsWith(\"profile.language.\")")
        assertTrue(blockStart >= 0)
        val block = main.substring(blockStart, blockStart + 900)
        assertTrue(block.contains("LisaLanguageAvailabilityAuthority.isSelectableInVersion1"))
        assertTrue(block.contains("version2ActivationMessage"))
        assertTrue(block.contains("Toast.LENGTH_LONG"))
        assertTrue(block.contains("preferredLanguage = language"))
        // English path still updates; Version 2 path must not fall through without the guard.
        assertTrue(block.indexOf("isSelectableInVersion1") < block.indexOf("updateActiveProfile"))
    }

    @Test
    fun uiUsesVersion2WordingNotUnavailableLabels() {
        assertFalse(english.languageVersion2StatusShort.contains("not yet", ignoreCase = true))
        assertFalse(english.languageVersion2StatusShort.contains("unavailable", ignoreCase = true))
        assertFalse(english.languageVersion2StatusShort.contains("unsupported", ignoreCase = true))
        assertFalse(english.languageVersion2InfoMessage.contains("not yet", ignoreCase = true))
        assertTrue(english.languageVersion2StatusShort.contains("Version 2"))
        assertTrue(english.languageVersion2InfoMessage.contains("downloadable"))

        val ui = read("LisaAccessibilityUi.kt")
        assertTrue(ui.contains("ProfileLanguageOptionGroup"))
        assertTrue(ui.contains("version2StatusLine"))
        assertFalse(ui.contains("Not available"))
        assertFalse(ui.contains("Coming soon"))
    }

    @Test
    fun unsupportedLegacyProfileLanguagesSafelyFallBackToEnglish() {
        val profiles = listOf(
            sampleProfile(PreferredLanguage.Afrikaans).copy(id = "a", name = "A"),
            sampleProfile(PreferredLanguage.IsiZulu).copy(id = "z", name = "Z"),
            sampleProfile(PreferredLanguage.English).copy(id = "e", name = "E")
        )
        val (recovered, didReset) =
            LisaLanguageAvailabilityAuthority.recoverProfilesForVersion1(profiles)
        assertTrue(didReset)
        assertEquals(3, recovered.size)
        assertEquals("A", recovered[0].name)
        assertEquals("Z", recovered[1].name)
        assertEquals("E", recovered[2].name)
        assertEquals(PreferredLanguage.English, recovered[0].preferredLanguage)
        assertEquals(PreferredLanguage.English, recovered[1].preferredLanguage)
        assertEquals(PreferredLanguage.English, recovered[2].preferredLanguage)
        assertTrue(english.languageResetToEnglishMessage.contains("English"))

        val store = read("LisaUserProfile.kt")
        assertTrue(store.contains("recoverProfilesForVersion1"))
        assertTrue(store.contains("preferredLanguageResetToEnglish"))
    }

    @Test
    fun noUnsupportedLanguageIsPassedToCurrentTtsEngine() {
        val ttsManager = read("LisaTtsVoiceManager.kt")
        val applyStart = ttsManager.indexOf("fun applyForProfile")
        assertTrue(applyStart >= 0)
        val applyBlock = ttsManager.substring(applyStart, applyStart + 700)
        assertTrue(applyBlock.contains("coerceForVersion1"))
        assertTrue(applyBlock.contains("Version 1: never route Afrikaans/isiZulu"))

        val main = read("MainActivity.kt")
        assertTrue(main.contains("coerceForVersion1"))
        assertTrue(
            main.contains("applyTtsForProfile(it.copy(preferredLanguage = safeLanguage))") ||
                main.contains("coerceForVersion1(language)")
        )
    }

    @Test
    fun version2ProductIntentIsDocumentedWithoutFakeDownloads() {
        val authority = read("LisaLanguageAvailability.kt")
        assertTrue(authority.contains("Downloadable language packs"))
        assertTrue(authority.contains("selectable TTS voices"))
        assertTrue(authority.contains("speech engines"))
        assertTrue(authority.contains("pronunciation validation"))
        assertTrue(authority.contains("Do not add fake download"))

        val accessibility = read("LisaAccessibilityUi.kt")
        assertFalse(accessibility.contains("Download language"))
        assertFalse(accessibility.contains("Voice store"))
    }

    @Test
    fun englishFunctionalPathAndCatalogOrderingRemainIntact() {
        val actions = CommunicationProfileDestinationActionAuthority.actions(
            profiles = listOf(sampleProfile(), sampleProfile().copy(id = "two", name = "Two")),
            activeProfileId = "primary",
            uiStrings = english
        )
        val languageIds = actions.filter { it.sectionId == "language" }.map { it.id }
        assertEquals(
            listOf(
                MenuDestinationActionId.language("English"),
                MenuDestinationActionId.language("Afrikaans"),
                MenuDestinationActionId.language("isiZulu")
            ),
            languageIds
        )
        assertNotEquals(
            MenuDestinationActionId.language("Afrikaans"),
            MenuDestinationActionId.language("isiZulu")
        )
        assertEquals(
            PreferredLanguage.English,
            PreferredLanguage.fromStored("English")
        )
    }
}
