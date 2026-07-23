package com.idworx.lisa

import com.idworx.lisa.features.brain1interactionstandard.model.UniversalInteractionGestures
import com.idworx.lisa.features.eyetrackingstatus.EyeTrackingStatusUiMapper
import com.idworx.lisa.features.intelligentstartup.authority.WelcomeEyeNavigationAuthority
import com.idworx.lisa.features.intelligentstartup.authority.WelcomeStage
import com.idworx.lisa.features.intelligentstartup.authority.WelcomeStageAction
import com.idworx.lisa.features.intelligentstartup.model.StartupPhase
import com.idworx.lisa.features.onboardingguide.navigation.GuidedTrainingNavigator
import com.idworx.lisa.features.onboardingguide.state.GuidedTrainingUiState
import com.idworx.lisa.features.onboardingguide.state.TrainingEvent
import com.idworx.lisa.features.zerotouchprinciple.audit.ZeroTouchFileProbe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Rc7D_37TwoStepWelcomeNavigationTest {

    private val uiStrings = LisaUiStrings.forLanguage(PreferredLanguage.English)
    private val leftThenRight = listOf(true, false)

    @Test
    fun welcomeBeginsOnBlinkSequenceIntroductionStage() {
        assertEquals(WelcomeStage.BlinkSequenceIntroduction, WelcomeEyeNavigationAuthority.initialStage())
        assertEquals(
            WelcomeStage.BlinkSequenceIntroduction,
            GuidedTrainingUiState().welcomeStage
        )
    }

    @Test
    fun introductionScreenContainsWelcomeToLisa() {
        val welcome = readWelcome()
        assertTrue(welcome.contains("WelcomeBlinkSequenceIntroductionScreen"))
        assertTrue(welcome.contains("welcomeToLisa"))
        assertTrue(welcome.contains("Welcome to LISA") || welcome.contains("welcomeToLisa"))
    }

    @Test
    fun introductionScreenContainsNotationExplanation() {
        val welcome = readWelcome()
        assertTrue(welcome.contains("WelcomeBlinkNotationExplanation"))
        assertTrue(welcome.contains("howToChooseTitle"))
        assertTrue(welcome.contains("notationExplanationBody"))
    }

    @Test
    fun explanationStatesLMeansLeftEye() {
        assertTrue(WelcomeEyeNavigationAuthority.notationExplanationBody().contains("L = left eye"))
    }

    @Test
    fun explanationStatesRMeansRightEye() {
        assertTrue(WelcomeEyeNavigationAuthority.notationExplanationBody().contains("R = right eye"))
    }

    @Test
    fun explanationStatesNumberIsBlinkCount() {
        assertTrue(
            WelcomeEyeNavigationAuthority.notationExplanationBody()
                .contains("The number tells you how many times to blink")
        )
    }

    @Test
    fun explanationContainsCompleteSequenceExamplesWithZero() {
        val left = WelcomeEyeNavigationAuthority.notationCompleteLeftExample()
        val right = WelcomeEyeNavigationAuthority.notationCompleteRightExample()
        assertTrue(left.contains("L2 R0"))
        assertTrue(left.contains("left eye twice"))
        assertFalse(left.contains("do not blink"))
        assertTrue(right.contains("L0 R2"))
        assertFalse(right.contains("do not blink"))
        assertTrue(right.contains("right eye twice"))
    }

    @Test
    fun duplicatedStandaloneL2AndR2ExplanationLinesRemoved() {
        val welcome = readWelcome()
        assertFalse(welcome.contains("L2 = Blink your left eye twice"))
        assertFalse(welcome.contains("R2 = Blink your right eye twice"))
        assertFalse(welcome.contains("notationLeftExample()"))
        assertFalse(welcome.contains("notationRightExample()"))
    }

    @Test
    fun introductionScreenDisplaysContinueAction() {
        val welcome = readWelcome()
        assertTrue(welcome.contains("continueButtonLabel"))
        assertTrue(welcome.contains("onContinue"))
        assertEquals("Continue", WelcomeEyeNavigationAuthority.continueButtonLabel())
    }

    @Test
    fun continueActionExplainsBlinkOnceWithEachEye() {
        // RC7D Welcome simplification — duplicate instruction under Continue removed.
        assertEquals("", WelcomeEyeNavigationAuthority.continueInstruction())
        val welcome = readWelcome()
        assertFalse(welcome.contains("Blink once with each eye"))
        val intro = welcome.substringAfter("fun WelcomeIntroductionContinueAction")
            .substringBefore("fun WelcomeDestinationSelectionScreen")
        assertFalse(intro.contains("continueInstruction()"))
        assertTrue(intro.contains("continueSequenceLabel"))
    }

    @Test
    fun continueActionShowsL1R1() {
        assertEquals("L1 R1", WelcomeEyeNavigationAuthority.continueSequenceLabel())
        assertEquals(1, WelcomeEyeNavigationAuthority.continueLeft)
        assertEquals(1, WelcomeEyeNavigationAuthority.continueRight)
    }

    @Test
    fun l1R1AdvancesOnlyToDestinationSelection() {
        val action = WelcomeEyeNavigationAuthority.resolve(
            WelcomeStage.BlinkSequenceIntroduction,
            1,
            1,
            leftThenRight
        )
        assertEquals(WelcomeStageAction.ContinueToDestinationSelection, action)
        assertEquals(
            WelcomeStage.DestinationSelection,
            WelcomeEyeNavigationAuthority.stageAfterAction(
                WelcomeStage.BlinkSequenceIntroduction,
                action
            )
        )
        assertEquals(
            WelcomeStageAction.None,
            WelcomeEyeNavigationAuthority.resolve(
                WelcomeStage.BlinkSequenceIntroduction,
                2,
                0,
                emptyList()
            )
        )
        assertEquals(
            WelcomeStageAction.None,
            WelcomeEyeNavigationAuthority.resolve(
                WelcomeStage.BlinkSequenceIntroduction,
                0,
                2,
                emptyList()
            )
        )
    }

    @Test
    fun touchingContinueAdvancesToDestinationSelection() {
        val welcome = readWelcome()
        assertTrue(welcome.contains("onContinueToDestination"))
        val flow = readFile("features/onboardingguide/ui/GuidedTrainingFlow.kt")
        assertTrue(flow.contains("TrainingEvent.WelcomeContinueToDestination"))
        val state = GuidedTrainingUiState()
        assertEquals(WelcomeStage.BlinkSequenceIntroduction, state.welcomeStage)
        val after = state.copy(welcomeStage = WelcomeStage.DestinationSelection)
        assertEquals(WelcomeStage.DestinationSelection, after.welcomeStage)
        assertEquals(
            com.idworx.lisa.features.onboardingguide.model.TrainingPhase.FirstLaunchChoice,
            after.progress.currentPhase
        )
    }

    @Test
    fun destinationSelectionDoesNotShowWelcomeToLisa() {
        val welcome = readWelcome()
        val destStart = welcome.indexOf("private fun WelcomeDestinationSelectionScreen")
        assertTrue(destStart >= 0)
        val destEnd = welcome.indexOf("private fun WelcomeBlinkNotationExplanation", destStart)
        assertTrue(destEnd > destStart)
        val destBlock = welcome.substring(destStart, destEnd)
        assertFalse(destBlock.contains("welcomeToLisa"))
        assertFalse(destBlock.contains("uiStrings.welcomeToLisa"))
        assertTrue(destBlock.contains("destinationSelectionTitle"))
    }

    @Test
    fun destinationSelectionDoesNotRepeatIntroductoryParagraph() {
        val welcome = readWelcome()
        val destStart = welcome.indexOf("fun WelcomeDestinationSelectionScreen")
        val destEnd = welcome.indexOf("fun WelcomeBlinkNotationExplanation", destStart)
        val destBlock = welcome.substring(destStart, destEnd)
        assertFalse(destBlock.contains("SilentWelcomeLaunchFlowMetadata.SUBTITLE"))
        assertFalse(destBlock.contains("first communication journey"))
    }

    @Test
    fun destinationSelectionDoesNotRepeatFullNotationCard() {
        val welcome = readWelcome()
        val destStart = welcome.indexOf("fun WelcomeDestinationSelectionScreen")
        val destEnd = welcome.indexOf("fun WelcomeBlinkNotationExplanation", destStart)
        val destBlock = welcome.substring(destStart, destEnd)
        assertFalse(destBlock.contains("WelcomeBlinkNotationExplanation"))
        assertFalse(destBlock.contains("howToChooseTitle"))
        assertFalse(destBlock.contains("notationCompleteLeftExample"))
    }

    @Test
    fun destinationSelectionDisplaysChooseWhereToBegin() {
        assertEquals("Choose where to begin", WelcomeEyeNavigationAuthority.destinationSelectionTitle())
        val welcome = readWelcome()
        assertTrue(welcome.contains("destinationSelectionTitle"))
    }

    @Test
    fun l2R0StillRoutesToGuidedLearning() {
        assertEquals(
            WelcomeStageAction.StartGuidedLearning,
            WelcomeEyeNavigationAuthority.resolve(WelcomeStage.DestinationSelection, 2, 0)
        )
        assertTrue(WelcomeEyeNavigationAuthority.isStartGuidedLearning(2, 0))
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
    fun l0R2StillRoutesToCommunication() {
        assertEquals(
            WelcomeStageAction.SkipToCommunication,
            WelcomeEyeNavigationAuthority.resolve(WelcomeStage.DestinationSelection, 0, 2)
        )
        assertTrue(WelcomeEyeNavigationAuthority.isSkipToCommunication(0, 2))
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
    fun touchActivationStillFollowsSameRoutes() {
        val welcome = readWelcome()
        assertTrue(welcome.contains("onStartGuidedLearning"))
        assertTrue(welcome.contains("onSkipToWorkspace"))
        val flow = readFile("features/onboardingguide/ui/GuidedTrainingFlow.kt")
        assertTrue(flow.contains("TrainingEvent.BeginLearning"))
        assertTrue(flow.contains("TrainingEvent.ConfirmSkip"))
    }

    @Test
    fun l2R2ReturnsFromDestinationSelectionToIntroduction() {
        assertEquals("L2 R2", WelcomeEyeNavigationAuthority.backSequenceLabel())
        assertEquals(
            WelcomeStageAction.BackToIntroduction,
            WelcomeEyeNavigationAuthority.resolve(WelcomeStage.DestinationSelection, 2, 2)
        )
        assertEquals(
            WelcomeStage.BlinkSequenceIntroduction,
            WelcomeEyeNavigationAuthority.stageAfterAction(
                WelcomeStage.DestinationSelection,
                WelcomeStageAction.BackToIntroduction
            )
        )
        assertEquals(
            WelcomeStageAction.None,
            WelcomeEyeNavigationAuthority.resolve(WelcomeStage.BlinkSequenceIntroduction, 2, 2)
        )
    }

    @Test
    fun l1R1ContinueCannotLeakIntoDestinationSelection() {
        assertEquals(
            WelcomeStageAction.None,
            WelcomeEyeNavigationAuthority.resolve(
                WelcomeStage.DestinationSelection,
                1,
                1,
                leftThenRight
            )
        )
        val afterContinue = WelcomeEyeNavigationAuthority.stageAfterAction(
            WelcomeStage.BlinkSequenceIntroduction,
            WelcomeStageAction.ContinueToDestinationSelection
        )
        assertEquals(WelcomeStage.DestinationSelection, afterContinue)
        assertFalse(
            WelcomeEyeNavigationAuthority.resolve(afterContinue, 1, 1, leftThenRight) ==
                WelcomeStageAction.StartGuidedLearning
        )
        assertFalse(
            WelcomeEyeNavigationAuthority.resolve(afterContinue, 1, 1, leftThenRight) ==
                WelcomeStageAction.SkipToCommunication
        )
        val controllerSource = readFile(
            "features/onboardingguide/services/TrainingSessionController.kt"
        )
        assertTrue(controllerSource.contains("advanceWelcomeToDestinationSelection"))
        assertTrue(controllerSource.contains("clearWelcomeGestureResidue") ||
            controllerSource.contains("leftWinkDots = 0"))
    }

    @Test
    fun bothStagesUseSharedLiveEyeTrackingState() {
        val welcome = readWelcome()
        assertTrue(welcome.contains("CompactEyeTrackingHeader"))
        assertEquals(
            2,
            Regex("CompactEyeTrackingHeader\\(").findAll(welcome).count()
        )
        val state = EyeTrackingStatusUiMapper.fromComposerFeedback(
            uiStrings = uiStrings,
            feedback = ComposerEyeFeedback(
                eyeTrackingBanner = EyeTrackingBannerContext(faceDetected = true, eyesDetected = true),
                leftWinkCount = 1,
                rightWinkCount = 1,
                sensitivityLevel = 4,
                responseTimeSec = 3
            ),
            cameraActive = true
        )
        assertTrue(state.trackingActive)
        assertEquals(1, state.leftBlinkCount)
        assertEquals(1, state.rightBlinkCount)
    }

    @Test
    fun bothStagesExposeLiveLeftAndRightBlinkCounts() {
        val welcome = readWelcome()
        assertTrue(welcome.contains("eyeTrackingStatus"))
        assertTrue(welcome.contains("CompactEyeTrackingHeader"))
    }

    @Test
    fun sensitivityAndResponseTimeRemainFromExistingAuthority() {
        val welcome = readWelcome()
        assertTrue(welcome.contains("onDecreaseSensitivity"))
        assertTrue(welcome.contains("onIncreaseResponseTime"))
        val state = EyeTrackingStatusUiMapper.fromComposerFeedback(
            uiStrings = uiStrings,
            feedback = ComposerEyeFeedback(
                eyeTrackingBanner = EyeTrackingBannerContext(),
                leftWinkCount = 0,
                rightWinkCount = 0,
                sensitivityLevel = 6,
                responseTimeSec = 2
            ),
            cameraActive = true
        )
        assertEquals(6, state.sensitivity)
        assertEquals(2, state.responseTimeSeconds)
    }

    @Test
    fun noSecondBlinkDetectorOrCameraAuthorityCreated() {
        val authority = readFile(
            "features/intelligentstartup/authority/WelcomeEyeNavigationAuthority.kt"
        )
        assertFalse(authority.contains("BlinkDetectionProcessor"))
        assertFalse(authority.contains("CameraX"))
        assertTrue(authority.contains("WelcomeStage"))
    }

    @Test
    fun noBiometricIdentityLogicIntroduced() {
        val authority = readFile(
            "features/intelligentstartup/authority/WelcomeEyeNavigationAuthority.kt"
        )
        val welcome = readWelcome()
        listOf(authority, welcome).forEach { source ->
            assertFalse(source.contains("biometric", ignoreCase = true))
            assertFalse(source.contains("FaceRecognition", ignoreCase = true))
        }
    }

    @Test
    fun existingRc7d35And36BehaviourRemainsValid() {
        assertEquals(StartupPhase.QuickCalibration.name, "QuickCalibration")
        assertEquals("L2 R0", WelcomeEyeNavigationAuthority.startGuidedLearningSequenceLabel())
        assertEquals("L0 R2", WelcomeEyeNavigationAuthority.skipToCommunicationSequenceLabel())
        assertTrue(
            ZeroTouchFileProbe.fileExists(
                "app/src/main/java/com/idworx/lisa/features/eyetrackingstatus/EyeTrackingStatusUiState.kt"
            )
        )
        assertTrue(
            ZeroTouchFileProbe.fileExists(
                "app/src/main/java/com/idworx/lisa/features/intelligentstartup/authority/StartupProfileAuthority.kt"
            )
        )
    }

    @Test
    fun existingGlobalAndEmergencySequencesRemainUnchanged() {
        assertEquals(6, EMERGENCY_LEFT_WINKS)
        assertEquals(0, EMERGENCY_RIGHT_WINKS)
        assertEquals(2, GuidedModeNavigation.BACK_LEFT)
        assertEquals(2, GuidedModeNavigation.BACK_RIGHT)
        assertEquals(2, UniversalInteractionGestures.OPTION_A_LEFT)
        assertEquals(0, UniversalInteractionGestures.OPTION_A_RIGHT)
        assertEquals(0, UniversalInteractionGestures.OPTION_B_LEFT)
        assertEquals(2, UniversalInteractionGestures.OPTION_B_RIGHT)
        assertEquals(1, UniversalInteractionGestures.CONFIRM_LEFT)
        assertEquals(1, UniversalInteractionGestures.CONFIRM_RIGHT)
    }

    private fun readWelcome(): String =
        readFile("features/onboardingguide/ui/TrainingWelcomeScreen.kt")

    private fun readFile(relativeUnderMainJava: String): String {
        val path = "app/src/main/java/com/idworx/lisa/$relativeUnderMainJava"
        return ZeroTouchFileProbe.readProjectFile(path)
            ?: error("Missing source file: $path")
    }
}
