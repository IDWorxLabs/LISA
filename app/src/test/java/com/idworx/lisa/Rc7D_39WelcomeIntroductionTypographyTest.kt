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
import androidx.compose.ui.text.font.FontWeight
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Rc7D_39WelcomeIntroductionTypographyTest {

    private val style = WelcomeIntroductionLayoutStyle
    private val leftThenRight = listOf(true, false)

    private fun welcomeSource(): String =
        readFile("features/onboardingguide/ui/TrainingWelcomeScreen.kt")

    private fun layoutSource(): String =
        readFile("features/onboardingguide/ui/WelcomeIntroductionLayoutStyle.kt")

    private fun introBlock(): String {
        val source = welcomeSource()
        val start = source.indexOf("fun WelcomeBlinkSequenceIntroductionScreen")
        val end = source.indexOf("fun WelcomeDestinationSelectionScreen", start)
        return source.substring(start, end)
    }

    @Test
    fun welcomeHeadingUsesLargerIntendedTypography() {
        assertEquals(26f, style.WelcomeTitleTextStyle.fontSize.value)
        assertEquals(FontWeight.Bold, style.WelcomeTitleTextStyle.fontWeight)
        assertTrue(style.WelcomeTitleTextStyle.fontSize.value >= 24f)
        assertTrue(introBlock().contains("WelcomeTitleTextStyle"))
    }

    @Test
    fun introParagraphUsesLargerIntendedTypography() {
        assertEquals(16f, style.WelcomeIntroTextStyle.fontSize.value)
        assertTrue(style.WelcomeIntroTextStyle.lineHeight!!.value >= 20f)
        assertTrue(introBlock().contains("WelcomeIntroTextStyle"))
    }

    @Test
    fun explanationTitleIsLargerAndBold() {
        assertEquals(20f, style.WelcomeExplanationTitleTextStyle.fontSize.value)
        assertEquals(FontWeight.Bold, style.WelcomeExplanationTitleTextStyle.fontWeight)
        assertTrue(welcomeSource().contains("WelcomeExplanationTitleTextStyle"))
    }

    @Test
    fun lrNumberLinesAreLargerThanRc7d38Baseline() {
        assertTrue(style.WelcomeExplanationBodyTextStyle.fontSize.value >= 16f)
        assertTrue(style.typographyLargerThanRc7d38Baseline())
    }

    @Test
    fun fullL2R0ExampleIsLargerAndSemibold() {
        assertEquals(17f, style.WelcomeSequenceExampleTextStyle.fontSize.value)
        assertEquals(FontWeight.SemiBold, style.WelcomeSequenceExampleTextStyle.fontWeight)
        assertTrue(
            WelcomeEyeNavigationAuthority.notationCompleteLeftExample().contains("L2 R0")
        )
    }

    @Test
    fun fullL0R2ExampleIsLargerAndSemibold() {
        assertEquals(17f, style.WelcomeSequenceExampleTextStyle.fontSize.value)
        assertEquals(FontWeight.SemiBold, style.WelcomeSequenceExampleTextStyle.fontWeight)
        assertTrue(
            WelcomeEyeNavigationAuthority.notationCompleteRightExample().contains("L0 R2")
        )
    }

    @Test
    fun continueTextIsLargerAndBold() {
        assertEquals(19f, style.WelcomeContinueTextStyle.fontSize.value)
        assertEquals(FontWeight.Bold, style.WelcomeContinueTextStyle.fontWeight)
        assertTrue(introBlock().contains("WelcomeContinueTextStyle"))
    }

    @Test
    fun blinkOnceWithEachEyeIsRemovedFromWelcome() {
        assertEquals("", WelcomeEyeNavigationAuthority.continueInstruction())
        assertFalse(introBlock().contains("WelcomeContinueInstructionTextStyle"))
        assertFalse(introBlock().contains("Blink once with each eye"))
    }

    @Test
    fun l1R1IsLargerAndVisuallyProminent() {
        assertEquals(18f, style.WelcomeContinueSequenceTextStyle.fontSize.value)
        assertEquals(FontWeight.Bold, style.WelcomeContinueSequenceTextStyle.fontWeight)
        assertTrue(introBlock().contains("WelcomeContinueSequenceTextStyle"))
    }

    @Test
    fun typographyUsesSharedTokensRatherThanScatteredMagicValues() {
        assertTrue(
            WelcomeIntroductionLayoutAuthority.introductionUsesSharedTypographyTokens(welcomeSource())
        )
        assertTrue(layoutSource().contains("WelcomeTitleTextStyle"))
        assertTrue(layoutSource().contains("WelcomeSequenceExampleTextStyle"))
    }

    @Test
    fun noLargeUnusedVerticalBlockRemainsBeneathExplanation() {
        assertTrue(
            WelcomeIntroductionLayoutAuthority.explanationExpandsToFillAvailableSpace(welcomeSource())
        )
        assertTrue(introBlock().contains("expandVertically"))
        assertTrue(introBlock().contains("SpaceEvenly") || welcomeSource().contains("SpaceEvenly"))
    }

    @Test
    fun continueRemainsInitiallyVisible() {
        assertTrue(WelcomeIntroductionLayoutAuthority.continueAnchoredBelowContent(welcomeSource()))
        assertTrue(introBlock().contains("WelcomeIntroductionContinueAction"))
    }

    @Test
    fun screenStillDoesNotRequireOuterScrolling() {
        assertTrue(style.fitsTargetViewportWithoutOuterScroll())
        assertTrue(
            WelcomeIntroductionLayoutAuthority.introductionSourceOmitsOuterVerticalScroll(welcomeSource())
        )
    }

    @Test
    fun noTextClips() {
        assertTrue(style.WelcomeTitleTextStyle.lineHeight!!.value > style.WelcomeTitleTextStyle.fontSize.value)
        assertTrue(
            style.WelcomeSequenceExampleTextStyle.lineHeight!!.value >
                style.WelcomeSequenceExampleTextStyle.fontSize.value
        )
        assertTrue(style.estimatedContentHeightDp() <= 720)
    }

    @Test
    fun noTextOverlaps() {
        assertTrue(introBlock().contains("ContentToActionGap"))
        assertTrue(introBlock().contains("CardVerticalSpacing"))
        assertTrue(style.ContentToActionGap.value >= 6f)
    }

    @Test
    fun explanationCardRemainsFullyReadable() {
        assertTrue(welcomeSource().contains("WelcomeBlinkNotationExplanation"))
        assertTrue(welcomeSource().contains("WelcomeExplanationBodyTextStyle"))
        assertEquals(
            "How to choose an option",
            WelcomeEyeNavigationAuthority.howToChooseTitle()
        )
    }

    @Test
    fun circularLisaLogoRemainsAbsent() {
        assertTrue(
            WelcomeIntroductionLayoutAuthority.introductionSourceOmitsDecorativeLogo(welcomeSource())
        )
    }

    @Test
    fun approvedWordingRemainsUnchanged() {
        val body = WelcomeEyeNavigationAuthority.notationExplanationBody()
        assertTrue(body.contains("L = left eye"))
        assertTrue(body.contains("R = right eye"))
        assertTrue(body.contains("The number tells you how many times to blink"))
        assertTrue(
            WelcomeEyeNavigationAuthority.notationCompleteLeftExample().contains("L2 R0")
        )
        assertTrue(
            WelcomeEyeNavigationAuthority.notationCompleteRightExample().contains("L0 R2")
        )
        assertFalse(
            WelcomeEyeNavigationAuthority.notationCompleteLeftExample().contains("do not blink")
        )
        assertFalse(
            WelcomeEyeNavigationAuthority.notationCompleteRightExample().contains("do not blink")
        )
        assertEquals("Continue", WelcomeEyeNavigationAuthority.continueButtonLabel())
        assertEquals("", WelcomeEyeNavigationAuthority.continueInstruction())
        assertEquals("L1 R1", WelcomeEyeNavigationAuthority.continueSequenceLabel())
    }

    @Test
    fun duplicateStandaloneL2R2TeachingLinesRemainAbsent() {
        val source = welcomeSource()
        assertFalse(source.contains("L2 = Blink your left eye twice"))
        assertFalse(source.contains("R2 = Blink your right eye twice"))
    }

    @Test
    fun l1R1RouteRemainsUnchanged() {
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
    fun l2R0RouteRemainsUnchanged() {
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
    fun l0R2RouteRemainsUnchanged() {
        assertEquals(
            WelcomeStageAction.SkipToCommunication,
            WelcomeEyeNavigationAuthority.resolve(WelcomeStage.DestinationSelection, 0, 2)
        )
    }

    @Test
    fun l2R2RouteRemainsUnchanged() {
        assertEquals(
            WelcomeStageAction.BackToIntroduction,
            WelcomeEyeNavigationAuthority.resolve(WelcomeStage.DestinationSelection, 2, 2)
        )
    }

    @Test
    fun sharedEyeTrackingStatusRemainsUnchanged() {
        assertTrue(introBlock().contains("UniversalEyeTrackingHeader"))
        assertTrue(introBlock().contains("compact = true"))
        assertTrue(introBlock().contains("showSensitivityControls = true"))
    }

    @Test
    fun sensitivityAndResponseTimeControlsRemainUnchanged() {
        assertTrue(introBlock().contains("onDecreaseSensitivity"))
        assertTrue(introBlock().contains("onIncreaseResponseTime"))
    }

    @Test
    fun destinationSelectionScreenRemainsUnchanged() {
        val source = welcomeSource()
        val destStart = source.indexOf("fun WelcomeDestinationSelectionScreen")
        val destEnd = source.indexOf("fun WelcomeBlinkNotationExplanation", destStart)
        val dest = source.substring(destStart, destEnd)
        assertTrue(dest.contains("destinationSelectionTitle"))
        assertFalse(dest.contains("WelcomeTitleTextStyle"))
        assertEquals("Choose where to begin", WelcomeEyeNavigationAuthority.destinationSelectionTitle())
    }

    @Test
    fun noSecondEyeTrackingAuthorityCreated() {
        val mapper = readFile("features/eyetrackingstatus/EyeTrackingStatusUiState.kt")
        assertTrue(mapper.contains("EyeTrackingStatusUiMapper"))
        assertFalse(mapper.contains("BlinkDetectionProcessor"))
        val state = EyeTrackingStatusUiMapper.fromComposerFeedback(
            uiStrings = LisaUiStrings.forLanguage(PreferredLanguage.English),
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
    fun noBiometricLogicIntroduced() {
        listOf(welcomeSource(), layoutSource()).forEach { source ->
            assertFalse(source.contains("biometric", ignoreCase = true))
            assertFalse(source.contains("FaceRecognition", ignoreCase = true))
        }
    }

    @Test
    fun existingRc7d35To38RemainValid() {
        assertEquals(StartupPhase.QuickCalibration.name, "QuickCalibration")
        assertEquals(WelcomeStage.BlinkSequenceIntroduction, WelcomeEyeNavigationAuthority.initialStage())
        assertTrue(style.fitsTargetViewportWithoutOuterScroll())
        assertTrue(
            ZeroTouchFileProbe.fileExists(
                "app/src/test/java/com/idworx/lisa/Rc7D_38SingleScreenWelcomeAccessibilityTest.kt"
            )
        )
        assertEquals(1, UniversalInteractionGestures.CONFIRM_LEFT)
        assertEquals(1, UniversalInteractionGestures.CONFIRM_RIGHT)
        assertEquals(6, EMERGENCY_LEFT_WINKS)
        assertEquals(0, EMERGENCY_RIGHT_WINKS)
    }

    private fun readFile(relativeUnderMainJava: String): String {
        val path = "app/src/main/java/com/idworx/lisa/$relativeUnderMainJava"
        return ZeroTouchFileProbe.readProjectFile(path)
            ?: error("Missing source file: $path")
    }
}
