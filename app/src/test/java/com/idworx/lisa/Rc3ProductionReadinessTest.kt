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
    fun aboutPageHasVersionCopyrightAndSupportContacts() {
        val versionLine = english.aboutVersionLabel("1.1")
        assertEquals("Version 1.1", versionLine)
        assertFalse(versionLine.contains("Build", ignoreCase = true))
        assertEquals("Created by Asgard Dynamics", english.aboutCreatorBody)
        assertTrue(english.copyrightNotice.contains("Asgard Dynamics"))
        assertTrue(english.aboutSupportWebsite.contains("https://asgarddynamics.io"))
        assertTrue(english.aboutSupportEmail.contains("lisa-support@asgarddynamics.io"))
        assertTrue(english.aboutSupportFeedback.contains("lisa-feedback@asgarddynamics.io"))
        assertFalse(english.aboutSupportWebsite.contains("published at launch", ignoreCase = true))
        assertFalse(english.aboutSupportEmail.contains("published at launch", ignoreCase = true))
        assertFalse(english.aboutCreatorBody.contains("Lungelo", ignoreCase = true))
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
