package com.idworx.lisa

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** RC3 — production readiness regression checks (presentation only). */
class Rc3ProductionReadinessTest {

    private val english = LisaUiStrings(PreferredLanguage.English)

    @Test
    fun privacyPolicyExplainsCameraAndOnDeviceProcessing() {
        assertTrue(english.privacyCameraBody.contains("eye tracking", ignoreCase = true))
        assertTrue(english.privacyOnDeviceBody.contains("on this device", ignoreCase = true))
        assertTrue(english.privacyOnDeviceBody.contains("does not upload", ignoreCase = true))
        assertTrue(english.privacyNoSellingBody.contains("not sold", ignoreCase = true))
    }

    @Test
    fun aboutPageHasVersionBuildCopyrightAndSupportPlaceholders() {
        val versionLine = english.versionAndBuildLabel("1.1", 1)
        assertTrue(versionLine.contains("Version 1.1"))
        assertTrue(versionLine.contains("Build 1"))
        assertTrue(english.copyrightNotice.contains("Asgard Dynamics"))
        assertFalse(english.aboutSupportWebsite.contains("Coming soon", ignoreCase = true))
        assertFalse(english.aboutSupportEmail.contains("Coming soon", ignoreCase = true))
    }

    @Test
    fun cameraPermissionCopyIsReassuringAndOnDevice() {
        assertTrue(english.onboardingCameraBody.contains("eye tracking", ignoreCase = true))
        assertTrue(english.onboardingCameraBody.contains("never uploaded", ignoreCase = true))
        assertTrue(english.cameraOnDeviceOnly.contains("does not upload", ignoreCase = true))
    }

    @Test
    fun userFacingErrorsAvoidTechnicalWording() {
        assertTrue(english.cameraStartupFailed.contains("couldn't access the camera", ignoreCase = true))
        assertFalse(english.cameraStartupFailed.contains("Exception", ignoreCase = true))
        assertTrue(english.speechEngineNotReady.contains("try again", ignoreCase = true))
    }

    @Test
    fun menuIncludesPrivacyPolicy() {
        assertTrue(english.privacyPolicy.isNotBlank())
        assertEquals("Privacy Policy", english.menuLabel(LisaPanel.PrivacyPolicy))
    }

    @Test
    fun voiceCopyAvoidsAiFeatureWording() {
        assertFalse(english.myVoiceIntro.contains("AI voice", ignoreCase = true))
        assertFalse(english.myVoiceStepLearn.contains("AI learns", ignoreCase = true))
    }
}
