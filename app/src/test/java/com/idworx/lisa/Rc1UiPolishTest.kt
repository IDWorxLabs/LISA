package com.idworx.lisa

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** RC1 — presentation-only polish regression checks (no behaviour changes). */
class Rc1UiPolishTest {

    private val english = LisaUiStrings(PreferredLanguage.English)

    @Test
    fun workspaceTitleUsesNaturalCommunicationLabel() {
        assertEquals("Communication", english.workspaceCommunicationTitle)
    }

    @Test
    fun resetActionUsesClearLabel() {
        assertEquals("Clear", english.reset)
    }

    @Test
    fun menuHasGroupedSectionLabels() {
        assertEquals("Communication", english.menuSectionCommunication)
        assertEquals("Application", english.menuSectionApplication)
        assertEquals("Support", english.menuSectionSupport)
    }

    @Test
    fun menuItemLabelsArePolished() {
        assertEquals("Communication Profile", english.myCommunication)
        assertEquals("Phrase Management", english.vocabularyTraining)
        assertEquals("Run Device Check", english.runDeviceCheckTitle)
    }

    @Test
    fun onboardingCopyRemovesDevelopmentLanguage() {
        assertFalse(english.onboardingStartBody.contains("local testing", ignoreCase = true))
        assertFalse(english.onboardingCameraBody.contains("testing build", ignoreCase = true))
        assertFalse(english.cameraOnDeviceOnly.contains("testing build", ignoreCase = true))
        assertFalse(english.feedbackIntro.contains("local testing", ignoreCase = true))
    }

    @Test
    fun skipToCommunicationUsesNaturalWording() {
        assertEquals("Skip to Communication", english.skipToCommunication)
        assertEquals("Let's go to Communication.", english.goToCommunication)
    }
}
