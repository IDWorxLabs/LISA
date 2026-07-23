package com.idworx.lisa

import com.idworx.lisa.features.brain1interactionstandard.model.UniversalInteractionGestures
import com.idworx.lisa.features.eyetrackingstatus.EyeTrackingStatusUiMapper
import com.idworx.lisa.features.intelligentstartup.authority.WelcomeEyeNavigationAuthority
import com.idworx.lisa.features.intelligentstartup.authority.WelcomeStage
import com.idworx.lisa.features.intelligentstartup.authority.WelcomeStageAction
import com.idworx.lisa.features.intelligentstartup.model.StartupPhase
import com.idworx.lisa.features.onboardingguide.navigation.GuidedTrainingNavigator
import com.idworx.lisa.features.onboardingguide.state.TrainingEvent
import com.idworx.lisa.features.onboardingguide.ui.WelcomeIntroductionLayoutAuthority
import com.idworx.lisa.features.onboardingguide.ui.WelcomeIntroductionLayoutStyle
import com.idworx.lisa.features.zerotouchprinciple.audit.ZeroTouchFileProbe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Rc7D_38SingleScreenWelcomeAccessibilityTest {

    private val uiStrings = LisaUiStrings.forLanguage(PreferredLanguage.English)
    private val leftThenRight = listOf(true, false)

    private fun welcomeSource(): String =
        readFile("features/onboardingguide/ui/TrainingWelcomeScreen.kt")

    private fun introBlock(): String {
        val source = welcomeSource()
        val start = source.indexOf("fun WelcomeBlinkSequenceIntroductionScreen")
        val end = source.indexOf("fun WelcomeDestinationSelectionScreen", start)
        return source.substring(start, end)
    }

    @Test
    fun introductionNoLongerRendersCircularLisaLogo() {
        assertTrue(WelcomeIntroductionLayoutAuthority.introductionSourceOmitsDecorativeLogo(welcomeSource()))
        assertFalse(introBlock().contains("TrainingLisaLogo"))
    }

    @Test
    fun welcomeToLisaRemainsPresent() {
        assertTrue(introBlock().contains("welcomeToLisa"))
    }

    @Test
    fun sharedEyeTrackingStatusRemainsPresent() {
        assertTrue(introBlock().contains("CompactEyeTrackingHeader"))
        assertTrue(introBlock().contains("eyeTrackingStatus"))
    }

    @Test
    fun leftAndRightBlinkCountersRemainPresent() {
        assertTrue(introBlock().contains("CompactEyeTrackingHeader"))
        val surfaces = readFile("features/eyetrackingstatus/EyeTrackingStatusSurfaces.kt")
        assertTrue(surfaces.contains("BlinkCounterRow"))
        assertTrue(surfaces.contains("leftBlinkCount"))
        assertTrue(surfaces.contains("rightBlinkCount"))
    }

    @Test
    fun sensitivityRemainsPresent() {
        assertTrue(introBlock().contains("showSensitivityControls = true"))
        assertTrue(introBlock().contains("onDecreaseSensitivity"))
    }

    @Test
    fun responseTimeRemainsPresent() {
        assertTrue(introBlock().contains("onDecreaseResponseTime"))
        assertTrue(introBlock().contains("onIncreaseResponseTime"))
    }

    @Test
    fun notationExplanationRemainsPresent() {
        assertTrue(introBlock().contains("WelcomeBlinkNotationExplanation"))
        assertTrue(welcomeSource().contains("howToChooseTitle"))
    }

    @Test
    fun l2R0CompleteExampleRemainsPresent() {
        assertTrue(
            WelcomeEyeNavigationAuthority.notationCompleteLeftExample().contains("L2 R0")
        )
        assertTrue(welcomeSource().contains("notationCompleteLeftExample"))
    }

    @Test
    fun l0R2CompleteExampleRemainsPresent() {
        assertTrue(
            WelcomeEyeNavigationAuthority.notationCompleteRightExample().contains("L0 R2")
        )
        assertTrue(welcomeSource().contains("notationCompleteRightExample"))
    }

    @Test
    fun continueRemainsPresent() {
        assertTrue(introBlock().contains("WelcomeIntroductionContinueAction"))
        assertEquals("Continue", WelcomeEyeNavigationAuthority.continueButtonLabel())
    }

    @Test
    fun blinkOnceWithEachEyeRemainsPresent() {
        // Duplicate under-button instruction removed; Continue button still shows L1 R1.
        assertEquals("", WelcomeEyeNavigationAuthority.continueInstruction())
        assertFalse(introBlock().contains("Blink once with each eye"))
        assertFalse(introBlock().contains("continueInstruction()"))
        assertTrue(introBlock().contains("continueSequenceLabel"))
    }

    @Test
    fun l1R1RemainsPresent() {
        assertEquals("L1 R1", WelcomeEyeNavigationAuthority.continueSequenceLabel())
        assertTrue(introBlock().contains("continueSequenceLabel"))
    }

    @Test
    fun ordinaryTargetViewportDoesNotRequireScrollingToReachContinue() {
        assertTrue(WelcomeIntroductionLayoutStyle.fitsTargetViewportWithoutOuterScroll())
        assertTrue(
            WelcomeIntroductionLayoutStyle.estimatedContentHeightDp() <=
                WelcomeIntroductionLayoutStyle.TargetViewportHeight.value.toInt()
        )
        assertTrue(
            WelcomeIntroductionLayoutAuthority.introductionSourceOmitsOuterVerticalScroll(welcomeSource())
        )
    }

    @Test
    fun continueIsInsideInitiallyVisibleLayoutHierarchy() {
        assertTrue(WelcomeIntroductionLayoutAuthority.continueAnchoredBelowContent(welcomeSource()))
        val block = introBlock()
        assertTrue(block.indexOf("WelcomeIntroductionContinueAction") >
            block.indexOf("WelcomeBlinkNotationExplanation"))
    }

    @Test
    fun screenDoesNotDependOnTouchScrolling() {
        val block = introBlock()
        assertFalse(Regex("""fillMaxSize\(\)\s*\n\s*\.verticalScroll""").containsMatchIn(block))
        assertTrue(block.contains("WelcomeIntroductionContinueAction"))
    }

    @Test
    fun actionSectionRemainsVisibleUnderConstrainedHeight() {
        val block = introBlock()
        assertTrue(block.contains("weight(1f)"))
        assertTrue(block.contains("WelcomeIntroductionContinueAction"))
        assertTrue(block.contains("ExplanationInternalScrollThreshold"))
        // Continue is a sibling after the weighted middle — not inside the scrollable explanation.
        val continueIdx = block.indexOf("WelcomeIntroductionContinueAction")
        val scrollIdx = block.indexOf("verticalScroll")
        assertTrue(continueIdx > scrollIdx || scrollIdx < 0)
    }

    @Test
    fun textDoesNotOverlapContinueAction() {
        val block = introBlock()
        assertTrue(block.contains("ContentToActionGap"))
        assertTrue(block.contains("Spacer(modifier = Modifier.height(style.ContentToActionGap))") ||
            block.contains("style.ContentToActionGap"))
    }

    @Test
    fun noContentClippedAtTargetDimensions() {
        assertTrue(WelcomeIntroductionLayoutStyle.fitsTargetViewportWithoutOuterScroll(720))
        // RC7D.39 uses larger type; ordinary target remains 720dp (not ultra-short 640dp).
        assertTrue(
            WelcomeIntroductionLayoutStyle.estimatedContentHeightDp() <=
                WelcomeIntroductionLayoutStyle.TargetViewportHeight.value.toInt()
        )
        assertEquals(360, WelcomeIntroductionLayoutStyle.TargetViewportWidth.value.toInt())
    }

    @Test
    fun duplicateStandaloneL2R2TeachingLinesRemainAbsent() {
        val source = welcomeSource()
        assertFalse(source.contains("L2 = Blink your left eye twice"))
        assertFalse(source.contains("R2 = Blink your right eye twice"))
    }

    @Test
    fun l1R1StillAdvancesOnlyToDestinationSelection() {
        assertEquals(
            WelcomeStageAction.ContinueToDestinationSelection,
            WelcomeEyeNavigationAuthority.resolve(
                WelcomeStage.BlinkSequenceIntroduction,
                1,
                1,
                leftThenRight
            )
        )
        assertEquals(
            WelcomeStageAction.None,
            WelcomeEyeNavigationAuthority.resolve(WelcomeStage.BlinkSequenceIntroduction, 2, 0)
        )
    }

    @Test
    fun touchContinueStillFollowsSameRoute() {
        assertTrue(introBlock().contains("onContinue"))
        assertTrue(welcomeSource().contains("onContinueToDestination"))
        val flow = readFile("features/onboardingguide/ui/GuidedTrainingFlow.kt")
        assertTrue(flow.contains("TrainingEvent.WelcomeContinueToDestination"))
    }

    @Test
    fun destinationSelectionBehaviorRemainsUnchanged() {
        val source = welcomeSource()
        val destStart = source.indexOf("fun WelcomeDestinationSelectionScreen")
        val destEnd = source.indexOf("fun WelcomeBlinkNotationExplanation", destStart)
        val dest = source.substring(destStart, destEnd)
        assertTrue(dest.contains("destinationSelectionTitle"))
        assertTrue(dest.contains("startGuidedLearning"))
        assertTrue(dest.contains("skipToCommunication"))
        assertTrue(dest.contains("onBackToIntroduction"))
        assertEquals("Choose where to begin", WelcomeEyeNavigationAuthority.destinationSelectionTitle())
    }

    @Test
    fun l2R0StillRoutesToGuidedLearning() {
        assertEquals(
            WelcomeStageAction.StartGuidedLearning,
            WelcomeEyeNavigationAuthority.resolve(WelcomeStage.DestinationSelection, 2, 0)
        )
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
    fun l2R2StillReturnsToExplanationScreen() {
        assertEquals(
            WelcomeStageAction.BackToIntroduction,
            WelcomeEyeNavigationAuthority.resolve(WelcomeStage.DestinationSelection, 2, 2)
        )
    }

    @Test
    fun sharedEyeTrackingAuthorityRemainsSingular() {
        val mapper = readFile("features/eyetrackingstatus/EyeTrackingStatusUiState.kt")
        assertTrue(mapper.contains("EyeTrackingStatusUiMapper"))
        assertFalse(mapper.contains("BlinkDetectionProcessor"))
        val state = EyeTrackingStatusUiMapper.fromComposerFeedback(
            uiStrings = uiStrings,
            feedback = ComposerEyeFeedback(
                eyeTrackingBanner = EyeTrackingBannerContext(faceDetected = true, eyesDetected = true),
                leftWinkCount = 1,
                rightWinkCount = 0,
                sensitivityLevel = 4,
                responseTimeSec = 3
            ),
            cameraActive = true
        )
        assertEquals(1, state.leftBlinkCount)
        assertEquals(4, state.sensitivity)
    }

    @Test
    fun noBiometricOrFaceRecognitionLogicIntroduced() {
        listOf(welcomeSource(), readFile("features/onboardingguide/ui/WelcomeIntroductionLayoutStyle.kt"))
            .forEach { source ->
                assertFalse(source.contains("biometric", ignoreCase = true))
                assertFalse(source.contains("FaceRecognition", ignoreCase = true))
            }
    }

    @Test
    fun existingRc7d35To37RemainValid() {
        assertEquals(StartupPhase.QuickCalibration.name, "QuickCalibration")
        assertEquals(WelcomeStage.BlinkSequenceIntroduction, WelcomeEyeNavigationAuthority.initialStage())
        assertEquals("L2 R0", WelcomeEyeNavigationAuthority.startGuidedLearningSequenceLabel())
        assertEquals("L0 R2", WelcomeEyeNavigationAuthority.skipToCommunicationSequenceLabel())
        assertEquals("L1 R1", WelcomeEyeNavigationAuthority.continueSequenceLabel())
        assertTrue(
            ZeroTouchFileProbe.fileExists(
                "app/src/test/java/com/idworx/lisa/Rc7D_37TwoStepWelcomeNavigationTest.kt"
            )
        )
    }

    @Test
    fun existingGlobalAndEmergencySequencesRemainUnchanged() {
        assertEquals(6, EMERGENCY_LEFT_WINKS)
        assertEquals(0, EMERGENCY_RIGHT_WINKS)
        assertEquals(2, GuidedModeNavigation.BACK_LEFT)
        assertEquals(2, GuidedModeNavigation.BACK_RIGHT)
        assertEquals(1, UniversalInteractionGestures.CONFIRM_LEFT)
        assertEquals(1, UniversalInteractionGestures.CONFIRM_RIGHT)
    }

    private fun readFile(relativeUnderMainJava: String): String {
        val path = "app/src/main/java/com/idworx/lisa/$relativeUnderMainJava"
        return ZeroTouchFileProbe.readProjectFile(path)
            ?: error("Missing source file: $path")
    }
}
