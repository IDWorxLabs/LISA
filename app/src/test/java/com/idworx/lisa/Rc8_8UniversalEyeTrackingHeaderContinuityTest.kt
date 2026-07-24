package com.idworx.lisa

import com.idworx.lisa.features.zerotouchprinciple.audit.ZeroTouchFileProbe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * RC8.8 — UniversalEyeTrackingHeader is the single eye-tracking chrome authority from Welcome
 * through Guided Learning, Startup, Settings recalibration, and Communication.
 */
class Rc8_8UniversalEyeTrackingHeaderContinuityTest {

    private fun read(path: String): String =
        ZeroTouchFileProbe.readProjectFile(path) ?: error("Missing source: $path")

    private fun welcomeSource(): String =
        read("app/src/main/java/com/idworx/lisa/features/onboardingguide/ui/TrainingWelcomeScreen.kt")

    private fun lessonsSource(): String =
        read("app/src/main/java/com/idworx/lisa/features/onboardingguide/ui/TrainingLessonScreens.kt")

    private fun setupSource(): String =
        read("app/src/main/java/com/idworx/lisa/features/onboardingguide/ui/TrainingSetupScreen.kt")

    private fun startupSource(): String =
        read("app/src/main/java/com/idworx/lisa/features/intelligentstartup/ui/IntelligentStartupFlow.kt")

    private fun accessibilitySource(): String =
        read("app/src/main/java/com/idworx/lisa/LisaAccessibilityUi.kt")

    private fun surfacesSource(): String =
        read("app/src/main/java/com/idworx/lisa/features/eyetrackingstatus/EyeTrackingStatusSurfaces.kt")

    private fun universalHeaderSource(): String =
        read("app/src/main/java/com/idworx/lisa/features/eyetrackingstatus/UniversalEyeTrackingHeader.kt")

    private fun universalHeaderImplementation(): String =
        universalHeaderSource().substringAfter(
            "fun UniversalEyeTrackingHeader(\n    uiStrings: LisaUiStrings,"
        )

    // --- Surface wiring -------------------------------------------------------------------------

    @Test
    fun welcomeIntroductionUsesUniversalEyeTrackingHeader() {
        val welcome = welcomeSource()
        val introStart = welcome.indexOf("fun WelcomeBlinkSequenceIntroductionScreen")
        val destStart = welcome.indexOf("fun WelcomeDestinationSelectionScreen")
        assertTrue(introStart >= 0 && destStart > introStart)
        val intro = welcome.substring(introStart, destStart)
        assertTrue(intro.contains("UniversalEyeTrackingHeader("))
        assertFalse(intro.contains("EyeTrackingStatusPill("))
    }

    @Test
    fun welcomeDestinationSelectionUsesUniversalEyeTrackingHeader() {
        val welcome = welcomeSource()
        val destStart = welcome.indexOf("fun WelcomeDestinationSelectionScreen")
        val destEnd = welcome.indexOf("fun WelcomeBlinkNotationExplanation", destStart)
        assertTrue(destStart >= 0 && destEnd > destStart)
        val dest = welcome.substring(destStart, destEnd)
        assertTrue(dest.contains("UniversalEyeTrackingHeader("))
        assertFalse(dest.contains("EyeTrackingStatusPill("))
    }

    @Test
    fun guidedLearningReadinessUsesUniversalEyeTrackingHeader() {
        val setup = setupSource()
        assertTrue(setup.contains("ExpandedEyeTrackingStatusPanel("))
        val expanded = surfacesSource().substringAfter("fun ExpandedEyeTrackingStatusPanel(")
            .substringBefore("fun BlinkCounterRow(")
        assertTrue(expanded.contains("UniversalEyeTrackingHeader("))
        assertFalse(expanded.contains("EyeTrackingStatusPill("))
    }

    @Test
    fun guidedLearningLessonsUseUniversalEyeTrackingHeader() {
        val lessons = lessonsSource()
        assertTrue(lessons.contains("UniversalEyeTrackingHeader("))
        assertFalse(lessons.contains("EyeTrackingStatusPill("))
    }

    @Test
    fun communicationUsesUniversalEyeTrackingHeader() {
        val ui = accessibilitySource()
        val headerSlice = ui.substringAfter("if (showSharedBlinkStatusHeader)")
            .substringBefore("if (showGuidedVocabularyOverlay)")
        assertTrue(headerSlice.contains("UniversalEyeTrackingHeader("))
        assertFalse(headerSlice.contains("CompactSensitivityControls("))
        assertFalse(headerSlice.contains("SequenceProgressDots("))
    }

    @Test
    fun startupUsesUniversalEyeTrackingHeader() {
        val startup = startupSource()
        assertTrue(startup.contains("UniversalEyeTrackingHeader("))
        assertFalse(startup.contains("EyeTrackingStatusPill("))
    }

    // --- Deprecated pill path removed -----------------------------------------------------------

    @Test
    fun universalHeaderDoesNotUseEyeTrackingStatusPill() {
        assertFalse(universalHeaderSource().contains("EyeTrackingStatusPill"))
    }

    @Test
    fun compactHeaderOnlyDelegatesToUniversal() {
        val compact = surfacesSource().substringAfter("fun CompactEyeTrackingHeader(")
            .substringBefore("fun ExpandedEyeTrackingStatusPanel(")
        assertTrue(compact.contains("UniversalEyeTrackingHeader("))
        assertFalse(compact.contains("EyeTrackingStatusPill("))
        assertFalse(compact.contains("TrainingSensitivityControls"))
        assertFalse(compact.contains("BlinkCounterRow("))
    }

    @Test
    fun preCommunicationScreensDoNotCallEyeTrackingStatusPill() {
        assertFalse(welcomeSource().contains("EyeTrackingStatusPill("))
        assertFalse(lessonsSource().contains("EyeTrackingStatusPill("))
        assertFalse(startupSource().contains("EyeTrackingStatusPill("))
    }

    // --- Shared blink counts --------------------------------------------------------------------

    @Test
    fun universalHeaderUsesSharedLeftRightCountsNotLocalDetection() {
        val full = universalHeaderSource()
        val stateOverload = full.substringAfter("fun UniversalEyeTrackingHeader(\n    state: EyeTrackingStatusUiState,")
            .substringBefore("fun UniversalEyeTrackingHeader(\n    uiStrings: LisaUiStrings,")
        assertTrue(stateOverload.contains("leftBlinkCount = state.leftBlinkCount"))
        assertTrue(stateOverload.contains("rightBlinkCount = state.rightBlinkCount"))

        val universal = universalHeaderImplementation()
        assertTrue(universal.contains("uiStrings.leftDots(leftBlinkCount)"))
        assertTrue(universal.contains("uiStrings.rightDots(rightBlinkCount)"))
        assertFalse(full.contains("BlinkDetectionProcessor"))

        val accessibility = accessibilitySource()
        val headerSlice = accessibility.substringAfter("UniversalEyeTrackingHeader(")
            .substringBefore("if (developerMode)")
        assertTrue(headerSlice.contains("leftBlinkCount = userDisplay.leftWinkDots"))
        assertTrue(headerSlice.contains("rightBlinkCount = userDisplay.rightWinkDots"))
    }

    // --- Sensitivity / response-time controls ---------------------------------------------------

    @Test
    fun universalHeaderExposesSensitivityAndResponseTimeControls() {
        val universal = universalHeaderImplementation()
        assertTrue(universal.contains("uiStrings.sensitivityDecrease"))
        assertTrue(universal.contains("uiStrings.sensitivityIncrease"))
        assertTrue(universal.contains("onDecreaseSensitivity"))
        assertTrue(universal.contains("uiStrings.responseTimeDecrease"))
        assertTrue(universal.contains("uiStrings.responseTimeIncrease"))
        assertTrue(universal.contains("onDecreaseResponseTime"))
        assertTrue(universal.contains("if (guidedResponseTimeControlsVisible)"))
    }

    @Test
    fun communicationCallSiteStillPassesGuidedResponseTimeVisibility() {
        val accessibility = accessibilitySource()
        assertTrue(accessibility.contains("guidedResponseTimeControlsVisible = guidedWorkspaceTrainingActive"))
        assertTrue(accessibility.contains("onDecreaseGuidedResponseTime"))
        assertTrue(accessibility.contains("onIncreaseGuidedResponseTime"))
    }

    // --- Gestures unchanged (spot-check) --------------------------------------------------------

    @Test
    fun settingsHubBackAndEmergencySequencesUnchanged() {
        assertEquals(
            GuidedModeNavigation.BACK_LEFT to GuidedModeNavigation.BACK_RIGHT,
            SettingsAndControlsHubSequences.BACK
        )
        assertEquals(6 to 0, EMERGENCY_LEFT_WINKS to EMERGENCY_RIGHT_WINKS)
    }
}
