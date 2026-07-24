package com.idworx.lisa

import com.idworx.lisa.features.zerotouchprinciple.audit.ZeroTouchFileProbe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EyeTrackingBannerDisplayTest {

    private val strings = LisaUiStrings.forLanguage(PreferredLanguage.English)

    private val eyeTrackingStates = listOf(
        LisaCommunicationState.WaitingForFace,
        LisaCommunicationState.Ready,
        LisaCommunicationState.Reset,
        LisaCommunicationState.Cancelled,
        LisaCommunicationState.Listening
    )

    @Test
    fun eyesDetected_resolvesToWatchingYourEyes_withNoSubtitle() {
        val display = LisaCommunicationState.Listening.toUserDisplay(
            strings = strings,
            pendingPhrase = null,
            countdown = null,
            eyeTrackingBanner = EyeTrackingBannerContext(
                faceDetected = true,
                eyesDetected = true
            )
        )

        assertEquals("Watching your eyes", display.headline)
        assertEquals("", display.subtitle)
    }

    @Test
    fun noFace_resolvesToNoFaceDetected_withNoSubtitle() {
        val display = LisaCommunicationState.WaitingForFace.toUserDisplay(
            strings = strings,
            pendingPhrase = null,
            countdown = null,
            eyeTrackingBanner = EyeTrackingBannerContext(faceDetected = false)
        )

        assertEquals("No face detected", display.headline)
        assertEquals("", display.subtitle)
    }

    @Test
    fun faceWithoutEyes_resolvesToLookAtCamera_withNoSubtitle() {
        val display = LisaCommunicationState.Listening.toUserDisplay(
            strings = strings,
            pendingPhrase = null,
            countdown = null,
            eyeTrackingBanner = EyeTrackingBannerContext(
                faceDetected = true,
                eyesDetected = false
            )
        )

        assertEquals("Please look at the camera", display.headline)
        assertEquals("", display.subtitle)
    }

    @Test
    fun calibrationActive_resolvesToCalibrating_withNoSubtitle() {
        val display = LisaCommunicationState.Listening.toUserDisplay(
            strings = strings,
            pendingPhrase = null,
            countdown = null,
            eyeTrackingBanner = EyeTrackingBannerContext(
                calibrationActive = true,
                faceDetected = true,
                eyesDetected = true
            )
        )

        assertEquals("Calibrating eye tracking", display.headline)
        assertEquals("", display.subtitle)
    }

    @Test
    fun trackingLost_resolvesToTrackingLost_withNoSubtitle() {
        val display = LisaCommunicationState.Listening.toUserDisplay(
            strings = strings,
            pendingPhrase = null,
            countdown = null,
            eyeTrackingBanner = EyeTrackingBannerContext(
                trackingLost = true,
                faceDetected = false
            )
        )

        assertEquals("Tracking lost", display.headline)
        assertEquals("", display.subtitle)
    }

    @Test
    fun eyeTrackingBannerStates_neverContainListening() {
        val contexts = listOf(
            EyeTrackingBannerContext(calibrationActive = true),
            EyeTrackingBannerContext(trackingLost = true),
            EyeTrackingBannerContext(faceDetected = false),
            EyeTrackingBannerContext(faceDetected = true, eyesDetected = false),
            EyeTrackingBannerContext(faceDetected = true, eyesDetected = true)
        )

        for (state in eyeTrackingStates) {
            for (context in contexts) {
                val display = state.toUserDisplay(
                    strings = strings,
                    pendingPhrase = null,
                    countdown = null,
                    eyeTrackingBanner = context
                )
                assertFalse(
                    "Unexpected LISTENING in $state: ${display.headline}",
                    display.headline.contains("LISTENING", ignoreCase = true)
                )
                assertFalse(
                    "Unexpected subtitle for $state: ${display.subtitle}",
                    display.subtitle.isNotBlank()
                )
            }
        }
    }

    @Test
    fun buildingMessageState_isUnchanged() {
        val display = LisaCommunicationState.BuildingMessage.toUserDisplay(
            strings = strings,
            pendingPhrase = null,
            countdown = null,
            leftWinkDots = 1,
            rightWinkDots = 0
        )

        assertEquals("BUILDING MESSAGE", display.headline)
        assertTrue(display.subtitle.contains("Left:"))
    }

    @Test
    fun runtimePath_wiresEyeTrackingBannerFromMainActivity() {
        val main = ZeroTouchFileProbe.readProjectFile("app/src/main/java/com/idworx/lisa/MainActivity.kt")
            ?: error("MainActivity.kt not found")

        assertTrue(main.contains("eyeTrackingBanner = eyeTrackingBannerContext()"))
        assertTrue(main.contains("private fun eyeTrackingBannerContext(): EyeTrackingBannerContext"))
    }

    @Test
    fun composeBannerPath_doesNotReferenceListeningHeadlineStrings() {
        val userDisplay = ZeroTouchFileProbe.readProjectFile("app/src/main/java/com/idworx/lisa/LisaUserDisplay.kt")
            ?: error("LisaUserDisplay.kt not found")
        val uiStrings = ZeroTouchFileProbe.readProjectFile("app/src/main/java/com/idworx/lisa/LisaUiStrings.kt")
            ?: error("LisaUiStrings.kt not found")
        val accessibilityUi = ZeroTouchFileProbe.readProjectFile("app/src/main/java/com/idworx/lisa/LisaAccessibilityUi.kt")
            ?: error("LisaAccessibilityUi.kt not found")

        assertFalse(userDisplay.contains("strings.listening"))
        assertFalse(userDisplay.contains("watchingYourEyes"))
        assertFalse(uiStrings.contains("LISTENING...\""))
        assertTrue(accessibilityUi.contains("userDisplay.headline"))
        assertTrue(accessibilityUi.contains("EverydayCommunicationPanel("))
    }

    @Test
    fun communicationWorkspace_doesNotRenderTimelineStatusPill() {
        val accessibilityUi = ZeroTouchFileProbe.readProjectFile("app/src/main/java/com/idworx/lisa/LisaAccessibilityUi.kt")
            ?: error("LisaAccessibilityUi.kt not found")

        assertFalse(accessibilityUi.contains("CompactTimelineChip("))
        assertFalse(accessibilityUi.contains("private fun CompactTimelineChip"))
        assertFalse(accessibilityUi.contains("timelineWatching"))
        assertFalse(accessibilityUi.contains("timelineListening"))
        assertTrue(accessibilityUi.contains("EverydayCommunicationPanel("))
    }
}
