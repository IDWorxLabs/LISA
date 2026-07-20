package com.idworx.lisa

import com.idworx.lisa.features.brain1interactionstandard.model.UniversalInteractionGestures
import com.idworx.lisa.features.eyetrackingstatus.EyeTrackingStatusUiMapper
import com.idworx.lisa.features.intelligentstartup.authority.WelcomeEyeNavigationAuthority
import com.idworx.lisa.features.intelligentstartup.authority.WelcomeStage
import com.idworx.lisa.features.intelligentstartup.authority.WelcomeStageAction
import com.idworx.lisa.features.onboardingguide.navigation.GuidedTrainingNavigator
import com.idworx.lisa.features.onboardingguide.state.TrainingEvent
import com.idworx.lisa.features.onboardingguide.ui.WelcomeDestinationLayoutAuthority
import com.idworx.lisa.features.onboardingguide.ui.WelcomeDestinationLayoutStyle
import com.idworx.lisa.features.onboardingguide.ui.WelcomeIntroductionLayoutAuthority
import com.idworx.lisa.features.onboardingguide.ui.WelcomeIntroductionLayoutStyle
import com.idworx.lisa.features.zerotouchprinciple.audit.ZeroTouchFileProbe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * RC7D.40 — Continue merges L1 R1; destination selection fits one non-scrolling screen.
 */
class Rc7D_40CompactWelcomeActionsTest {

    private val uiStrings = LisaUiStrings.forLanguage(PreferredLanguage.English)
    private val leftThenRight = listOf(true, false)
    private val destStyle = WelcomeDestinationLayoutStyle

    private fun welcomeSource(): String =
        readFile("features/onboardingguide/ui/TrainingWelcomeScreen.kt")

    private fun destLayoutSource(): String =
        readFile("features/onboardingguide/ui/WelcomeDestinationLayoutStyle.kt")

    private fun componentsSource(): String =
        readFile("features/onboardingguide/ui/TrainingComponents.kt")

    private fun introContinueBlock(): String {
        val source = welcomeSource()
        val start = source.indexOf("fun WelcomeIntroductionContinueAction")
        val end = source.indexOf("fun WelcomeDestinationSelectionScreen", start)
        return source.substring(start, end)
    }

    private fun destinationBlock(): String {
        val source = welcomeSource()
        val start = source.indexOf("fun WelcomeDestinationSelectionScreen")
        val end = source.indexOf("fun WelcomeBlinkNotationExplanation", start)
        return source.substring(start, end)
    }

    private fun choiceBlockSource(): String {
        val source = welcomeSource()
        val start = source.indexOf("fun WelcomeChoiceBlock")
        val end = source.indexOf("fun CaregiverAdvancedSkipLink", start)
        return source.substring(start, end)
    }

    // --- Continue button merge ---

    @Test
    fun continueButtonContainsContinueAndL1R1() {
        val block = introContinueBlock()
        assertTrue(block.contains("continueButtonLabel"))
        assertTrue(block.contains("secondaryText"))
        assertTrue(block.contains("continueSequenceLabel"))
        assertEquals("Continue", WelcomeEyeNavigationAuthority.continueButtonLabel())
        assertEquals("L1 R1", WelcomeEyeNavigationAuthority.continueSequenceLabel())
        assertTrue(componentsSource().contains("secondaryText"))
    }

    @Test
    fun l1R1IsNoLongerDetachedStandaloneBelowInstruction() {
        assertTrue(WelcomeDestinationLayoutAuthority.continueMergesSequenceIntoButton(welcomeSource()))
        val block = introContinueBlock()
        assertFalse(
            Regex(
                """continueInstruction\(\)[\s\S]*?Text\(\s*\n\s*text = WelcomeEyeNavigationAuthority\.continueSequenceLabel"""
            ).containsMatchIn(block)
        )
    }

    @Test
    fun blinkOnceWithEachEyeRemainsVisibleBelowButton() {
        assertEquals("Blink once with each eye", WelcomeEyeNavigationAuthority.continueInstruction())
        val block = introContinueBlock()
        assertTrue(block.contains("continueInstruction"))
        assertTrue(block.indexOf("TrainingPrimaryButton") < block.indexOf("continueInstruction"))
    }

    @Test
    fun continueRemainsOneClickableAction() {
        val block = introContinueBlock()
        assertEquals(1, Regex("TrainingPrimaryButton\\(").findAll(block).count())
        assertTrue(block.contains("continueContentDescription"))
        assertEquals(
            "Continue, blink once with each eye, L1 R1",
            WelcomeEyeNavigationAuthority.continueContentDescription()
        )
        assertFalse(block.contains("nested") && block.contains("clickable"))
    }

    @Test
    fun touchContinueStillRoutesToDestinationSelection() {
        assertTrue(welcomeSource().contains("onContinueToDestination"))
        val flow = readFile("features/onboardingguide/ui/GuidedTrainingFlow.kt")
        assertTrue(flow.contains("TrainingEvent.WelcomeContinueToDestination"))
    }

    @Test
    fun l1R1StillRoutesToDestinationSelection() {
        assertEquals(
            WelcomeStageAction.ContinueToDestinationSelection,
            WelcomeEyeNavigationAuthority.resolve(
                WelcomeStage.BlinkSequenceIntroduction,
                1,
                1,
                leftThenRight
            )
        )
    }

    @Test
    fun welcomeIntroductionRemainsNonScrolling() {
        assertTrue(
            WelcomeIntroductionLayoutAuthority.introductionSourceOmitsOuterVerticalScroll(welcomeSource())
        )
        assertTrue(WelcomeIntroductionLayoutStyle.fitsTargetViewportWithoutOuterScroll())
    }

    @Test
    fun continueRemainsInitiallyVisible() {
        assertTrue(WelcomeIntroductionLayoutAuthority.continueAnchoredBelowContent(welcomeSource()))
    }

    // --- Destination single-screen ---

    @Test
    fun destinationSelectionHasNoOuterScrolling() {
        assertTrue(
            WelcomeDestinationLayoutAuthority.destinationSourceOmitsOuterVerticalScroll(welcomeSource())
        )
        assertFalse(destinationBlock().contains(".verticalScroll(rememberScrollState())"))
    }

    @Test
    fun destinationTitleRemainsVisible() {
        assertEquals("Choose where to begin", WelcomeEyeNavigationAuthority.destinationSelectionTitle())
        assertTrue(destinationBlock().contains("destinationSelectionTitle"))
    }

    @Test
    fun destinationSubtitleRemainsVisible() {
        assertEquals(
            "Use your eyes or touch an option.",
            WelcomeEyeNavigationAuthority.destinationSelectionSubtitle()
        )
        assertTrue(destinationBlock().contains("destinationSelectionSubtitle"))
    }

    @Test
    fun startGuidedLearningRemainsVisible() {
        assertTrue(destinationBlock().contains("startGuidedLearning"))
        assertTrue(uiStrings.startGuidedLearning.isNotBlank())
    }

    @Test
    fun blinkLeftTwiceRemainsVisible() {
        assertEquals("Blink left twice", WelcomeEyeNavigationAuthority.startGuidedLearningInstruction())
        assertTrue(destinationBlock().contains("startGuidedLearningInstruction"))
        assertTrue(
            WelcomeEyeNavigationAuthority.combinedActionHint(
                WelcomeEyeNavigationAuthority.startGuidedLearningInstruction(),
                WelcomeEyeNavigationAuthority.startGuidedLearningSequenceLabel()
            ).contains("Blink left twice")
        )
    }

    @Test
    fun l2R0RemainsVisible() {
        assertEquals("L2 R0", WelcomeEyeNavigationAuthority.startGuidedLearningSequenceLabel())
        assertTrue(destinationBlock().contains("startGuidedLearningSequenceLabel"))
        assertTrue(choiceBlockSource().contains("combinedActionHint"))
    }

    @Test
    fun skipToCommunicationRemainsVisible() {
        assertTrue(destinationBlock().contains("skipToCommunication"))
        assertTrue(uiStrings.skipToCommunication.isNotBlank())
    }

    @Test
    fun blinkRightTwiceRemainsVisible() {
        assertEquals("Blink right twice", WelcomeEyeNavigationAuthority.skipToCommunicationInstruction())
        assertTrue(destinationBlock().contains("skipToCommunicationInstruction"))
    }

    @Test
    fun l0R2RemainsVisible() {
        assertEquals("L0 R2", WelcomeEyeNavigationAuthority.skipToCommunicationSequenceLabel())
        assertTrue(destinationBlock().contains("skipToCommunicationSequenceLabel"))
    }

    @Test
    fun backRemainsVisible() {
        assertTrue(destinationBlock().contains("uiStrings.back") || destinationBlock().contains("onBackToIntroduction"))
        assertTrue(destinationBlock().contains("backInstruction"))
    }

    @Test
    fun blinkLeftTwiceAndRightTwiceRemainsVisible() {
        assertEquals(
            "Blink left twice and right twice",
            WelcomeEyeNavigationAuthority.backInstruction()
        )
        assertTrue(destinationBlock().contains("backInstruction"))
    }

    @Test
    fun l2R2RemainsVisible() {
        assertEquals("L2 R2", WelcomeEyeNavigationAuthority.backSequenceLabel())
        assertTrue(destinationBlock().contains("backSequenceLabel"))
    }

    @Test
    fun caregiverTextRemainsVisibleInInitialViewport() {
        assertTrue(WelcomeDestinationLayoutAuthority.caregiverRemainsInDestinationScreen(welcomeSource()))
        assertEquals(
            "For caregivers: Skip to Navigation Training",
            uiStrings.caregiverAdvancedSkipNavigation
        )
        assertFalse(destinationBlock().contains(".verticalScroll("))
        assertFalse(destinationBlock().contains("rememberScrollState()"))
    }

    @Test
    fun normalTargetViewportFitsAllDestinationContent() {
        assertTrue(destStyle.fitsTargetViewportWithoutOuterScroll())
        assertTrue(
            destStyle.estimatedContentHeightDp() <= destStyle.TargetViewportHeight.value.toInt()
        )
        assertEquals(720, destStyle.TargetViewportHeight.value.toInt())
        assertEquals(360, destStyle.TargetViewportWidth.value.toInt())
    }

    @Test
    fun noContentClipsAtTargetDimensions() {
        assertTrue(destStyle.estimatedContentHeightDp() <= 720)
        assertTrue(WelcomeIntroductionLayoutStyle.fitsTargetViewportWithoutOuterScroll(720))
    }

    @Test
    fun noButtonsOverlap() {
        val choice = choiceBlockSource()
        assertTrue(choice.contains("ActionGroupSpacing") || destinationBlock().contains("ActionGroupSpacing"))
        assertTrue(destStyle.ActionGroupSpacing.value > 0f)
        assertTrue(destStyle.ButtonToInstructionSpacing.value >= 0f)
    }

    @Test
    fun noActionInstructionOverlapsAnotherAction() {
        assertTrue(destinationBlock().contains("Spacer(modifier = Modifier.height(style.ActionGroupSpacing))"))
        assertTrue(WelcomeDestinationLayoutAuthority.destinationUsesCombinedInstructionLine(welcomeSource()))
        assertEquals(
            "Blink left twice · L2 R0",
            WelcomeEyeNavigationAuthority.combinedActionHint("Blink left twice", "L2 R0")
        )
        assertEquals(
            "Blink right twice · L0 R2",
            WelcomeEyeNavigationAuthority.combinedActionHint("Blink right twice", "L0 R2")
        )
        assertEquals(
            "Blink left twice and right twice · L2 R2",
            WelcomeEyeNavigationAuthority.combinedActionHint(
                "Blink left twice and right twice",
                "L2 R2"
            )
        )
    }

    @Test
    fun excessiveBlankGapsAreRemoved() {
        assertTrue(destStyle.spacingMoreCompactThanPreRc7d40())
        assertTrue(WelcomeDestinationLayoutAuthority.destinationUsesLayoutTokens(welcomeSource()))
        assertTrue(destStyle.StatusToCardSpacing.value <= 6f)
        assertTrue(destStyle.ActionGroupSpacing.value <= 6f)
        assertTrue(destStyle.CardPadding.value <= 12f)
        // Pre-RC7D.40 used 16dp status gap, 10/12 action gaps, 24 card padding via TrainingCard.
        assertTrue(destLayoutSource().contains("ActionGroupSpacing"))
    }

    @Test
    fun buttonTouchTargetsRemainAccessible() {
        assertTrue(destStyle.PrimaryButtonHeight.value >= 48f)
        assertTrue(destStyle.SecondaryButtonHeight.value >= 48f)
        assertTrue(choiceBlockSource().contains("PrimaryButtonHeight"))
        assertTrue(choiceBlockSource().contains("SecondaryButtonHeight"))
    }

    @Test
    fun destinationRoutesRemainUnchanged() {
        assertEquals(
            WelcomeStageAction.StartGuidedLearning,
            WelcomeEyeNavigationAuthority.resolve(WelcomeStage.DestinationSelection, 2, 0)
        )
        assertEquals(
            WelcomeStageAction.SkipToCommunication,
            WelcomeEyeNavigationAuthority.resolve(WelcomeStage.DestinationSelection, 0, 2)
        )
        assertEquals(
            WelcomeStageAction.BackToIntroduction,
            WelcomeEyeNavigationAuthority.resolve(WelcomeStage.DestinationSelection, 2, 2)
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
    fun caregiverNavigationTrainingRouteRemainsUnchanged() {
        assertTrue(destinationBlock().contains("onSkipToNavigationTraining"))
        assertTrue(destinationBlock().contains("CaregiverAdvancedSkipLink"))
        val flow = readFile("features/onboardingguide/ui/GuidedTrainingFlow.kt")
        assertTrue(
            flow.contains("onSkipToNavigationTraining") ||
                flow.contains("SkipToNavigation") ||
                flow.contains("skipToNavigation")
        )
    }

    @Test
    fun sharedEyeTrackingStatusRemainsUnchanged() {
        assertTrue(destinationBlock().contains("CompactEyeTrackingHeader"))
        assertTrue(destinationBlock().contains("eyeTrackingStatus"))
        assertTrue(destinationBlock().contains("compact = true"))
        assertTrue(destinationBlock().contains("showSensitivityControls = true"))
    }

    @Test
    fun noSecondEyeTrackingAuthorityIsCreated() {
        val mapper = readFile("features/eyetrackingstatus/EyeTrackingStatusUiState.kt")
        assertTrue(mapper.contains("EyeTrackingStatusUiMapper"))
        assertFalse(mapper.contains("BlinkDetectionProcessor"))
        val state = EyeTrackingStatusUiMapper.fromComposerFeedback(
            uiStrings = uiStrings,
            feedback = ComposerEyeFeedback(
                eyeTrackingBanner = EyeTrackingBannerContext(faceDetected = true, eyesDetected = true),
                leftWinkCount = 0,
                rightWinkCount = 0,
                sensitivityLevel = 5,
                responseTimeSec = 3
            ),
            cameraActive = true
        )
        assertEquals(5, state.sensitivity)
    }

    @Test
    fun noBiometricLogicIsIntroduced() {
        listOf(welcomeSource(), destLayoutSource(), componentsSource()).forEach { source ->
            assertFalse(source.contains("biometric", ignoreCase = true))
            assertFalse(source.contains("FaceRecognition", ignoreCase = true))
            assertFalse(source.contains("faceId", ignoreCase = true))
        }
    }

    @Test
    fun existingRc7d35Through39ContractsRemainValid() {
        assertTrue(WelcomeIntroductionLayoutStyle.fitsTargetViewportWithoutOuterScroll())
        assertTrue(WelcomeIntroductionLayoutStyle.typographyLargerThanRc7d38Baseline())
        assertEquals(WelcomeStage.BlinkSequenceIntroduction, WelcomeEyeNavigationAuthority.initialStage())
        assertEquals("L1 R1", WelcomeEyeNavigationAuthority.continueSequenceLabel())
        assertEquals("L2 R0", WelcomeEyeNavigationAuthority.startGuidedLearningSequenceLabel())
        assertEquals("L0 R2", WelcomeEyeNavigationAuthority.skipToCommunicationSequenceLabel())
        assertEquals("L2 R2", WelcomeEyeNavigationAuthority.backSequenceLabel())
    }

    @Test
    fun existingGlobalAndEmergencySequencesRemainUnchanged() {
        assertEquals(2, UniversalInteractionGestures.OPTION_A_LEFT)
        assertEquals(0, UniversalInteractionGestures.OPTION_A_RIGHT)
        assertEquals(0, UniversalInteractionGestures.OPTION_B_LEFT)
        assertEquals(2, UniversalInteractionGestures.OPTION_B_RIGHT)
        assertEquals(1, UniversalInteractionGestures.CONFIRM_LEFT)
        assertEquals(1, UniversalInteractionGestures.CONFIRM_RIGHT)
        assertEquals(6, EMERGENCY_LEFT_WINKS)
        assertEquals(0, EMERGENCY_RIGHT_WINKS)
        assertEquals(2, GuidedModeNavigation.BACK_LEFT)
        assertEquals(2, GuidedModeNavigation.BACK_RIGHT)
    }

    private fun readFile(relativeUnderMainJava: String): String {
        val path = "app/src/main/java/com/idworx/lisa/$relativeUnderMainJava"
        return ZeroTouchFileProbe.readProjectFile(path)
            ?: error("Missing source file: $path")
    }
}
