package com.idworx.lisa

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** RC5 — Version 1 simplification: caregiver linking removed from UI. */
class Rc5Version1SimplificationTest {

    private val english = LisaUiStrings(PreferredLanguage.English)

    @Test
    fun menuSectionsExcludeAssistanceAndDeveloper() {
        assertEquals("Communication", english.menuSectionCommunication)
        assertEquals("Application", english.menuSectionApplication)
        assertEquals("Support", english.menuSectionSupport)
    }

    @Test
    fun coreMenuLabelsRemainForVersion1() {
        assertEquals("Communication Profile", english.menuLabel(LisaPanel.MyCommunication))
        assertEquals("Phrase Management", english.menuLabel(LisaPanel.VocabularyTraining))
        assertEquals("Voice", english.menuLabel(LisaPanel.Voice))
        assertEquals("Settings", english.menuLabel(LisaPanel.Settings))
        assertEquals("Privacy Policy", english.menuLabel(LisaPanel.PrivacyPolicy))
        assertEquals("About LISA", english.menuLabel(LisaPanel.AboutLisa))
    }

    @Test
    fun onboardingNoLongerMentionsCaregiverLinking() {
        assertFalse(english.onboardingStartBody.contains("caregiver", ignoreCase = true))
    }

    @Test
    fun emergencyStringsRemainForLocalAlarm() {
        assertTrue(english.emergencySpeechPhrase.isNotBlank())
        assertTrue(english.guidedEmergencyAwaitingConfirmMessage.contains("confirm", ignoreCase = true))
    }
}
