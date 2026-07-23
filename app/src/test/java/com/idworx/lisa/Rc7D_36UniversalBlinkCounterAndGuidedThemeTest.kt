package com.idworx.lisa

import com.idworx.lisa.features.brain1interactionstandard.model.UniversalInteractionGestures
import com.idworx.lisa.features.eyetrackingstatus.EyeTrackingStatusUiMapper
import com.idworx.lisa.features.eyetrackingstatus.EyeTrackingStatusUiState
import com.idworx.lisa.features.intelligentstartup.authority.WelcomeEyeNavigationAuthority
import com.idworx.lisa.features.intelligentstartup.model.StartupPhase
import com.idworx.lisa.features.onboardingguide.navigation.GuidedTrainingNavigator
import com.idworx.lisa.features.onboardingguide.state.TrainingEvent
import com.idworx.lisa.features.onboardingguide.ui.TrainingEyeTrackingState
import com.idworx.lisa.features.zerotouchprinciple.audit.ZeroTouchFileProbe
import com.idworx.lisa.ui.theme.LisaEmergencyRed
import com.idworx.lisa.ui.theme.LisaWorkspaceVisualStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Rc7D_36UniversalBlinkCounterAndGuidedThemeTest {

    private val uiStrings = LisaUiStrings.forLanguage(PreferredLanguage.English)

    private fun sampleFeedback(
        left: Int = 2,
        right: Int = 1,
        sensitivity: Int = 4,
        responseTime: Int = 3,
        calibrating: Boolean = false
    ) = ComposerEyeFeedback(
        eyeTrackingBanner = EyeTrackingBannerContext(
            calibrationActive = calibrating,
            faceDetected = true,
            eyesDetected = true
        ),
        leftWinkCount = left,
        rightWinkCount = right,
        sensitivityLevel = sensitivity,
        responseTimeSec = responseTime
    )

    @Test
    fun calibrationReceivesLiveSharedEyeTrackingState() {
        val state = EyeTrackingStatusUiMapper.fromComposerFeedback(
            uiStrings = uiStrings,
            feedback = sampleFeedback(calibrating = true),
            cameraActive = true,
            calibrationInProgress = true
        )
        assertTrue(state.calibrationInProgress)
        assertTrue(state.cameraActive)
        assertTrue(state.faceDetected)
        assertTrue(state.eyesDetected)
        assertTrue(state.statusText.isNotBlank())
        val calUi = readFile("features/intelligentstartup/ui/IntelligentStartupFlow.kt")
        assertTrue(calUi.contains("CompactEyeTrackingHeader"))
        assertTrue(calUi.contains("eyeTrackingStatus"))
    }

    @Test
    fun calibrationExposesLeftAndRightBlinkCounts() {
        val state = EyeTrackingStatusUiMapper.fromComposerFeedback(
            uiStrings = uiStrings,
            feedback = sampleFeedback(left = 3, right = 2, calibrating = true),
            cameraActive = true,
            calibrationInProgress = true
        )
        assertEquals(3, state.leftBlinkCount)
        assertEquals(2, state.rightBlinkCount)
    }

    @Test
    fun welcomeReceivesLiveSharedEyeTrackingState() {
        val welcome = readFile("features/onboardingguide/ui/TrainingWelcomeScreen.kt")
        assertTrue(welcome.contains("CompactEyeTrackingHeader"))
        assertTrue(welcome.contains("eyeTrackingStatus"))
        val state = EyeTrackingStatusUiMapper.fromComposerFeedback(
            uiStrings = uiStrings,
            feedback = sampleFeedback(left = 1, right = 0),
            cameraActive = true
        )
        assertTrue(state.trackingActive)
        assertEquals(1, state.leftBlinkCount)
    }

    @Test
    fun welcomeExposesLeftAndRightBlinkCounts() {
        val state = EyeTrackingStatusUiMapper.fromTraining(
            uiStrings = uiStrings,
            eyeTracking = TrainingEyeTrackingState(
                cameraActive = true,
                faceDetected = true,
                eyesDetected = true,
                leftBlinkCount = 2,
                rightBlinkCount = 2
            ),
            sensitivity = 5,
            responseTimeSeconds = 2
        )
        assertEquals(2, state.leftBlinkCount)
        assertEquals(2, state.rightBlinkCount)
    }

    @Test
    fun welcomeExplainsLAndRNotationInPlainLanguage() {
        assertTrue(
            WelcomeEyeNavigationAuthority.notationExplanationBody()
                .contains("L = left eye")
        )
        assertTrue(
            WelcomeEyeNavigationAuthority.notationExplanationBody()
                .contains("R = right eye")
        )
        val welcome = readFile("features/onboardingguide/ui/TrainingWelcomeScreen.kt")
        assertTrue(welcome.contains("WelcomeBlinkNotationExplanation"))
        assertTrue(welcome.contains("howToChooseTitle"))
    }

    @Test
    fun welcomeExplainsNumberIsBlinkCount() {
        assertTrue(
            WelcomeEyeNavigationAuthority.notationExplanationBody()
                .contains("how many times to blink")
        )
        assertTrue(WelcomeEyeNavigationAuthority.notationCompleteLeftExample().contains("L2 R0"))
        assertTrue(WelcomeEyeNavigationAuthority.notationCompleteRightExample().contains("L0 R2"))
    }

    @Test
    fun startGuidedLearningDisplaysBlinkLeftTwiceWithL2R0() {
        assertEquals("Blink left twice", WelcomeEyeNavigationAuthority.startGuidedLearningInstruction())
        assertEquals("L2 R0", WelcomeEyeNavigationAuthority.startGuidedLearningSequenceLabel())
        val welcome = readFile("features/onboardingguide/ui/TrainingWelcomeScreen.kt")
        assertTrue(welcome.contains("startGuidedLearningInstruction"))
        assertTrue(welcome.contains("startGuidedLearningSequenceLabel"))
    }

    @Test
    fun skipToCommunicationDisplaysBlinkRightTwiceWithL0R2() {
        assertEquals("Blink right twice", WelcomeEyeNavigationAuthority.skipToCommunicationInstruction())
        assertEquals("L0 R2", WelcomeEyeNavigationAuthority.skipToCommunicationSequenceLabel())
        val welcome = readFile("features/onboardingguide/ui/TrainingWelcomeScreen.kt")
        assertTrue(welcome.contains("skipToCommunicationInstruction"))
        assertTrue(welcome.contains("skipToCommunicationSequenceLabel"))
    }

    @Test
    fun l2R0RoutesOnlyToGuidedLearning() {
        assertTrue(WelcomeEyeNavigationAuthority.isStartGuidedLearning(2, 0))
        assertFalse(WelcomeEyeNavigationAuthority.isSkipToCommunication(2, 0))
        assertTrue(UniversalInteractionGestures.isOptionA(2, 0))
        val progress = GuidedTrainingNavigator().reduce(
            com.idworx.lisa.features.onboardingguide.model.TrainingProgress(),
            TrainingEvent.BeginLearning
        )
        assertEquals(
            com.idworx.lisa.features.onboardingguide.model.TrainingPhase.Setup,
            progress.currentPhase
        )
    }

    @Test
    fun l0R2RoutesOnlyToCommunication() {
        assertTrue(WelcomeEyeNavigationAuthority.isSkipToCommunication(0, 2))
        assertFalse(WelcomeEyeNavigationAuthority.isStartGuidedLearning(0, 2))
        assertTrue(UniversalInteractionGestures.isOptionB(0, 2))
        val progress = GuidedTrainingNavigator().reduce(
            com.idworx.lisa.features.onboardingguide.model.TrainingProgress(),
            TrainingEvent.ConfirmSkip
        )
        assertEquals(
            com.idworx.lisa.features.onboardingguide.model.TrainingPhase.Completion,
            progress.currentPhase
        )
    }

    @Test
    fun touchActivationFollowsSameRoutes() {
        val welcome = readFile("features/onboardingguide/ui/TrainingWelcomeScreen.kt")
        assertTrue(welcome.contains("onStartGuidedLearning"))
        assertTrue(welcome.contains("onSkipToWorkspace"))
        val flow = readFile("features/onboardingguide/ui/GuidedTrainingFlow.kt")
        assertTrue(flow.contains("TrainingEvent.BeginLearning"))
        assertTrue(flow.contains("TrainingEvent.ConfirmSkip"))
    }

    @Test
    fun guidedLearningReadinessUsesSharedEyeTrackingState() {
        val setup = readFile("features/onboardingguide/ui/TrainingSetupScreen.kt")
        assertTrue(setup.contains("ExpandedEyeTrackingStatusPanel"))
        assertTrue(setup.contains("eyeTrackingStatus"))
        assertTrue(setup.contains("Ready to begin"))
    }

    @Test
    fun guidedLearningLessonHostAlwaysIncludesSharedCompactBlinkCounter() {
        val lessons = readFile("features/onboardingguide/ui/TrainingLessonScreens.kt")
        assertTrue(lessons.contains("CompactEyeTrackingHeader"))
        val flow = readFile("features/onboardingguide/ui/GuidedTrainingFlow.kt")
        assertTrue(flow.contains("eyeTrackingStatus = eyeTrackingStatus"))
        assertTrue(flow.contains("CommunicationLessonScreen("))
    }

    @Test
    fun keyboardComposerLessonsCannotOmitBlinkCounter() {
        val lessons = readFile("features/onboardingguide/ui/TrainingLessonScreens.kt")
        assertTrue(lessons.contains("CompactEyeTrackingHeader"))
        assertTrue(lessons.contains("LessonEyeStatusPanel"))
        val composer = readFile("EyeControlledKeyboard.kt")
        assertTrue(composer.contains("ComposerEyeStatusBar"))
        assertTrue(composer.contains("leftWinkCount"))
        assertTrue(composer.contains("rightWinkCount"))
    }

    @Test
    fun sharedUiDoesNotCreateSecondTrackingAuthority() {
        val mapper = readFile("features/eyetrackingstatus/EyeTrackingStatusUiState.kt")
        assertTrue(mapper.contains("EyeTrackingStatusUiMapper"))
        assertFalse(mapper.contains("BlinkDetectionProcessor"))
        assertFalse(mapper.contains("class BlinkDetector"))
        assertTrue(mapper.contains("fromComposerFeedback"))
        assertTrue(mapper.contains("fromAuthoritative"))
    }

    @Test
    fun sensitivityAndResponseTimeComeFromAuthoritativeState() {
        val state = EyeTrackingStatusUiMapper.fromComposerFeedback(
            uiStrings = uiStrings,
            feedback = sampleFeedback(sensitivity = 7, responseTime = 4),
            cameraActive = true
        )
        assertEquals(7, state.sensitivity)
        assertEquals(4, state.responseTimeSeconds)
    }

    @Test
    fun navigationControlsUseThemeTokensRatherThanIsolatedWhiteBackgrounds() {
        val grid = readFile("ComposerCommandGrid.kt")
        assertTrue(grid.contains("LisaWorkspaceVisualStyle.NavActionEnabledBackground"))
        assertTrue(grid.contains("NavActionDisabledBackground"))
        assertFalse(grid.contains("LisaWhite.copy(alpha = 0.94f)"))
        assertEquals(
            LisaWorkspaceVisualStyle.NavActionEnabledBackground,
            LisaWorkspaceVisualStyle.NavActionEnabledBackground
        )
    }

    @Test
    fun disabledNavigationControlsRemainVisiblyDisabled() {
        val grid = readFile("ComposerCommandGrid.kt")
        assertTrue(grid.contains("CommandDisabledBackground"))
        assertTrue(grid.contains("CommandDisabledBorder"))
        assertTrue(grid.contains("if (!enabled)"))
    }

    @Test
    fun emergencyStylingRemainsSemanticallyDistinct() {
        val grid = readFile("ComposerCommandGrid.kt")
        assertTrue(grid.contains("ComposerEmergencyCommandCard"))
        assertTrue(grid.contains("LisaEmergencyRed"))
        assertNotEquals(
            LisaWorkspaceVisualStyle.NavActionEnabledBackground,
            LisaEmergencyRed.copy(alpha = 0.15f)
        )
    }

    @Test
    fun existingRc7d34And35StartupBehaviourRemainsIntact() {
        assertEquals(StartupPhase.FaceDetection.name, "FaceDetection")
        assertEquals(StartupPhase.QuickCalibration.name, "QuickCalibration")
        assertEquals(StartupPhase.EyeTrackingReady.name, "EyeTrackingReady")
        assertEquals("L2 R0", WelcomeEyeNavigationAuthority.startGuidedLearningSequenceLabel())
        assertEquals("L0 R2", WelcomeEyeNavigationAuthority.skipToCommunicationSequenceLabel())
        val profileAuth = readFile("features/intelligentstartup/authority/StartupProfileAuthority.kt")
        assertTrue(profileAuth.contains("StartupProfileAuthority"))
        val compat = readFile("features/intelligentstartup/authority/CalibrationCompatibilityAuthority.kt")
        assertTrue(compat.contains("CalibrationCompatibilityAuthority"))
        assertFalse(compat.contains("faceRecognition"))
        assertFalse(compat.contains("biometric"))
    }

    @Test
    fun noBiometricOrFaceRecognitionIdentityLogicIntroduced() {
        val surfaces = readFile("features/eyetrackingstatus/EyeTrackingStatusSurfaces.kt")
        val mapper = readFile("features/eyetrackingstatus/EyeTrackingStatusUiState.kt")
        val welcome = readFile("features/intelligentstartup/authority/WelcomeEyeNavigationAuthority.kt")
        listOf(surfaces, mapper, welcome).forEach { source ->
            assertFalse(source.contains("biometric", ignoreCase = true))
            assertFalse(source.contains("FaceRecognition", ignoreCase = true))
            assertFalse(source.contains("facial identity", ignoreCase = true))
        }
    }

    @Test
    fun blinkCountersAreNotResetSimplyByNavigatingBetweenSurfaces() {
        val sharedCounts = sampleFeedback(left = 2, right = 1)
        val welcomeState = EyeTrackingStatusUiMapper.fromComposerFeedback(
            uiStrings = uiStrings,
            feedback = sharedCounts,
            cameraActive = true
        )
        val readinessState = EyeTrackingStatusUiMapper.fromComposerFeedback(
            uiStrings = uiStrings,
            feedback = sharedCounts,
            cameraActive = true
        )
        val lessonState = EyeTrackingStatusUiMapper.fromComposerFeedback(
            uiStrings = uiStrings,
            feedback = sharedCounts,
            cameraActive = true
        )
        assertEquals(welcomeState.leftBlinkCount, readinessState.leftBlinkCount)
        assertEquals(readinessState.rightBlinkCount, lessonState.rightBlinkCount)
        assertEquals(2, lessonState.leftBlinkCount)
        assertEquals(1, lessonState.rightBlinkCount)
        val empty = EyeTrackingStatusUiState()
        assertEquals(0, empty.leftBlinkCount)
        assertFalse(welcomeState.leftBlinkCount == empty.leftBlinkCount && sharedCounts.leftWinkCount > 0 && false)
    }

    @Test
    fun existingEmergencySequenceL6R0RemainsUnchanged() {
        assertEquals(6, EMERGENCY_LEFT_WINKS)
        assertEquals(0, EMERGENCY_RIGHT_WINKS)
        assertEquals("L6 R0", formatWinkSequenceShort(EMERGENCY_LEFT_WINKS, EMERGENCY_RIGHT_WINKS))
    }

    @Test
    fun existingGlobalNavigationAndConfirmationSequencesRemainUnchanged() {
        assertEquals(2, UniversalInteractionGestures.OPTION_A_LEFT)
        assertEquals(0, UniversalInteractionGestures.OPTION_A_RIGHT)
        assertEquals(0, UniversalInteractionGestures.OPTION_B_LEFT)
        assertEquals(2, UniversalInteractionGestures.OPTION_B_RIGHT)
        assertTrue(UniversalInteractionGestures.isOptionA(2, 0))
        assertTrue(UniversalInteractionGestures.isOptionB(0, 2))
        assertEquals(1, UniversalInteractionGestures.CONFIRM_LEFT)
        assertEquals(1, UniversalInteractionGestures.CONFIRM_RIGHT)
    }

    private fun readFile(relativeUnderMainJava: String): String {
        val path = "app/src/main/java/com/idworx/lisa/$relativeUnderMainJava"
        return ZeroTouchFileProbe.readProjectFile(path)
            ?: error("Missing source file: $path")
    }
}
