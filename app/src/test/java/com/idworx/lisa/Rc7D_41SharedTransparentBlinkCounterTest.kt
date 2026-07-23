package com.idworx.lisa

import com.idworx.lisa.features.eyetrackingstatus.EyeTrackingStatusUiMapper
import com.idworx.lisa.features.eyetrackingstatus.TransparentBlinkCounterAuthority
import com.idworx.lisa.features.eyetrackingstatus.TransparentBlinkCounterStyle
import com.idworx.lisa.features.zerotouchprinciple.audit.ZeroTouchFileProbe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Shared transparent blink-counter authority across Communication and pre-Communication screens.
 */
class Rc7D_41SharedTransparentBlinkCounterTest {

    private val uiStrings = LisaUiStrings.forLanguage(PreferredLanguage.English)

    private fun surfaces(): String =
        readFile("features/eyetrackingstatus/EyeTrackingStatusSurfaces.kt")

    private fun styleSource(): String =
        readFile("features/eyetrackingstatus/TransparentBlinkCounterStyle.kt")

    @Test
    fun canonicalSharedCounterIsBlinkCounterRow() {
        assertEquals("BlinkCounterRow", TransparentBlinkCounterStyle.CanonicalComposableName)
        val source = surfaces()
        assertTrue("missing BlinkCounterRow", source.contains("fun BlinkCounterRow"))
        assertTrue("missing TransparentBlinkCounterStyle", source.contains("TransparentBlinkCounterStyle"))
        assertTrue("missing leftDots", source.contains("leftDots"))
        assertTrue("missing rightDots", source.contains("rightDots"))
        assertTrue("missing SpaceBetween", source.contains("SpaceBetween"))
        assertTrue(TransparentBlinkCounterAuthority.styleAuthorityIsTransparent())
    }

    @Test
    fun counterUsesTransparentChromeNotFilledBlueCard() {
        assertTrue(TransparentBlinkCounterAuthority.counterUsesTransparentChrome(surfaces()))
        assertFalse(surfaces().contains("LisaBlueLight.copy(alpha = 0.85f)"))
    }

    @Test
    fun preCommunicationAndCommunicationShareSameStyleAuthority() {
        assertTrue(TransparentBlinkCounterAuthority.styleAuthorityIsTransparent())
        assertEquals(
            androidx.compose.ui.graphics.Color.Transparent,
            TransparentBlinkCounterStyle.Background
        )
        val accessibility = readFile("LisaAccessibilityUi.kt")
        val keyboard = readFile("EyeControlledKeyboard.kt")
        assertTrue(
            TransparentBlinkCounterAuthority.communicationUsesSharedCounter(accessibility, keyboard)
        )
        assertTrue(styleSource().contains("Color.Transparent"))
        assertFalse(styleSource().contains("Color.Black.copy"))
    }

    @Test
    fun sharedComponentHasNoOpaqueGreyOrBlackFill() {
        assertTrue(TransparentBlinkCounterAuthority.counterHasNoOpaqueGreyOrBlackFill(surfaces()))
        assertTrue(TransparentBlinkCounterAuthority.styleAuthorityIsTransparent())
        assertFalse(styleSource().contains("alpha = 0.32f"))
        // Style object's Background token must be Transparent (not a Black fill literal).
        assertTrue(
            styleSource().contains("val Background: Color = Color.Transparent")
        )
        assertFalse(
            styleSource().contains("val Background: Color = Color.Black")
        )
    }

    @Test
    fun communicationAndPreCommunicationShareSameImplementation() {
        val accessibility = readFile("LisaAccessibilityUi.kt")
        val keyboard = readFile("EyeControlledKeyboard.kt")
        assertTrue(
            TransparentBlinkCounterAuthority.communicationUsesSharedCounter(accessibility, keyboard)
        )
        assertTrue(accessibility.contains("BlinkCounterRow("))
        assertTrue(keyboard.contains("BlinkCounterRow("))
        assertTrue(readFile("LisaEmergencyUi.kt").contains("BlinkCounterRow("))
    }

    @Test
    fun preCommunicationSurfacesUseSharedCounterAuthority() {
        val welcome = readFile("features/onboardingguide/ui/TrainingWelcomeScreen.kt")
        val lessons = readFile("features/onboardingguide/ui/TrainingLessonScreens.kt")
        val setup = readFile("features/onboardingguide/ui/TrainingSetupScreen.kt")
        val startup = readFile("features/intelligentstartup/ui/IntelligentStartupFlow.kt")
        assertTrue(welcome.contains("CompactEyeTrackingHeader"))
        assertTrue(lessons.contains("CompactEyeTrackingHeader"))
        assertTrue(setup.contains("ExpandedEyeTrackingStatusPanel"))
        assertTrue(startup.contains("CompactEyeTrackingHeader"))
        assertTrue(startup.contains("StartupScreenWithSharedBlinkCounter"))
        assertTrue(surfaces().contains("BlinkCounterRow("))
    }

    @Test
    fun startupPhasesWireSharedCounter() {
        val startup = readFile("features/intelligentstartup/ui/IntelligentStartupFlow.kt")
        assertTrue(startup.contains("FaceDetectionStartupScreen"))
        assertTrue(startup.contains("CreatePrimaryUserScreen"))
        assertTrue(startup.contains("StartupProfilePickerScreen"))
        assertTrue(startup.contains("QuickEyeCalibrationScreen"))
        assertTrue(startup.contains("CalibrationFailureScreen"))
        assertTrue(startup.contains("EyeTrackingReadyScreen"))
        assertTrue(startup.contains("StartupScreenWithSharedBlinkCounter"))
        assertTrue(startup.contains("eyeTrackingStatus"))
    }

    @Test
    fun blinkCounterLabelsAndDotsAlwaysUseLisaBlueDark() {
        assertTrue(TransparentBlinkCounterAuthority.labelsUsePermanentLisaBlue())
        assertEquals(
            com.idworx.lisa.ui.theme.LisaBlueDark,
            TransparentBlinkCounterStyle.LabelColor
        )
        assertTrue(
            TransparentBlinkCounterAuthority.noDarkLightLabelBranch(styleSource(), surfaces())
        )
        assertTrue(surfaces().contains("style.LabelColor"))
        assertFalse(styleSource().substringBefore("object TransparentBlinkCounterAuthority").contains("LisaWhite"))
    }

    @Test
    fun feedbackKeyboardNavButtonsUseOutlinedChromeAuthority() {
        val workspace = readFile("MenuDestinationWorkspaceUi.kt")
        val style = readFile("ui/theme/LisaWorkspaceVisualStyle.kt")
        assertTrue(style.contains("OutlinedKeyboardNavBackground"))
        assertTrue(style.contains("OutlinedKeyboardNavBorder"))
        assertTrue(style.contains("OutlinedKeyboardNavContent"))
        assertTrue(
            com.idworx.lisa.ui.theme.LisaWorkspaceVisualStyle.OutlinedKeyboardNavBackground ==
                androidx.compose.ui.graphics.Color.Transparent
        )
        assertEquals(
            androidx.compose.ui.graphics.Color.White,
            com.idworx.lisa.ui.theme.LisaWorkspaceVisualStyle.OutlinedKeyboardNavBorder
        )
        assertEquals(
            com.idworx.lisa.ui.theme.LisaBlueDark,
            com.idworx.lisa.ui.theme.LisaWorkspaceVisualStyle.OutlinedKeyboardNavContent
        )
        val legendStart = workspace.indexOf("fun FeedbackKeyboardDirectionLegend")
        assertTrue(legendStart >= 0)
        val legendEnd = workspace.indexOf("fun FeedbackFieldReview", legendStart)
        val legend = workspace.substring(legendStart, legendEnd)
        assertTrue(legend.contains("OutlinedKeyboardNavBackground"))
        assertTrue(legend.contains("OutlinedKeyboardNavBorder"))
        assertTrue(legend.contains("OutlinedKeyboardNavContent"))
        assertFalse(legend.contains("NavPanelBackground"))
        assertFalse(legend.contains("LisaWhite"))
        val barStart = workspace.indexOf("fun KeyboardFocusedCommandBar")
        val bar = workspace.substring(barStart)
        assertTrue(bar.contains("OutlinedKeyboardNavBorder"))
        assertTrue(bar.contains("OutlinedKeyboardNavContent"))
        assertTrue(bar.contains("OutlinedKeyboardNavBackground"))
    }

    @Test
    fun liveLeftAndRightCountsRenderViaSharedFormatter() {
        assertEquals("Left: —", uiStrings.leftDots(0))
        assertEquals("Right: —", uiStrings.rightDots(0))
        assertEquals("Left: ●", uiStrings.leftDots(1))
        assertEquals("Right: ●●", uiStrings.rightDots(2))
        assertEquals("Left: ●●●", uiStrings.leftDots(3))
        assertTrue(surfaces().contains("uiStrings.leftDots(leftBlinkCount)"))
        assertTrue(surfaces().contains("uiStrings.rightDots(rightBlinkCount)"))
    }

    @Test
    fun zeroSingleAndBothEyeStatesMapFromExistingState() {
        val zero = EyeTrackingStatusUiMapper.fromComposerFeedback(
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
        assertEquals(0, zero.leftBlinkCount)
        assertEquals(0, zero.rightBlinkCount)

        val leftOnly = EyeTrackingStatusUiMapper.fromComposerFeedback(
            uiStrings = uiStrings,
            feedback = ComposerEyeFeedback(
                eyeTrackingBanner = EyeTrackingBannerContext(faceDetected = true, eyesDetected = true),
                leftWinkCount = 2,
                rightWinkCount = 0,
                sensitivityLevel = 5,
                responseTimeSec = 3
            ),
            cameraActive = true
        )
        assertEquals(2, leftOnly.leftBlinkCount)
        assertEquals(0, leftOnly.rightBlinkCount)

        val both = EyeTrackingStatusUiMapper.fromComposerFeedback(
            uiStrings = uiStrings,
            feedback = ComposerEyeFeedback(
                eyeTrackingBanner = EyeTrackingBannerContext(faceDetected = true, eyesDetected = true),
                leftWinkCount = 1,
                rightWinkCount = 3,
                sensitivityLevel = 5,
                responseTimeSec = 3
            ),
            cameraActive = true
        )
        assertEquals(1, both.leftBlinkCount)
        assertEquals(3, both.rightBlinkCount)
    }

    @Test
    fun noDuplicateBlinkSourceInSharedSurfaces() {
        assertTrue(TransparentBlinkCounterAuthority.noDuplicateBlinkSourceInSurfaces(surfaces()))
        assertTrue(styleSource().contains("TransparentBlinkCounterStyle"))
    }

    @Test
    fun physicalLeftRightMappingUnchanged() {
        assertEquals("Left: ●●", uiStrings.leftDots(2))
        assertEquals("Right: ●", uiStrings.rightDots(1))
        assertTrue(winkDots(0) == "—")
        assertTrue(winkDots(2) == "●●")
    }

    @Test
    fun welcomeScreensDoNotGainOuterScrollFromCounter() {
        val welcome = readFile("features/onboardingguide/ui/TrainingWelcomeScreen.kt")
        val introStart = welcome.indexOf("fun WelcomeBlinkSequenceIntroductionScreen")
        val destStart = welcome.indexOf("fun WelcomeDestinationSelectionScreen")
        val intro = welcome.substring(introStart, destStart)
        val destEnd = welcome.indexOf("fun WelcomeBlinkNotationExplanation", destStart)
        val dest = welcome.substring(destStart, destEnd)
        assertFalse(Regex("""fillMaxSize\(\)\s*\n\s*\.verticalScroll""").containsMatchIn(intro))
        assertFalse(dest.contains(".verticalScroll(rememberScrollState())"))
        assertTrue(intro.contains("CompactEyeTrackingHeader"))
        assertTrue(dest.contains("CompactEyeTrackingHeader"))
    }

    @Test
    fun lessonsDisableDuplicateAnimatedCounterVisual() {
        val lessons = readFile("features/onboardingguide/ui/TrainingLessonScreens.kt")
        assertTrue(lessons.contains("showBlinkCounters = false"))
        assertTrue(lessons.contains("CompactEyeTrackingHeader"))
        assertTrue(surfaces().contains("showBlinkCounters = false"))
    }

    @Test
    fun hierarchyKeepsStatusAboveCounterAboveSensitivity() {
        val compact = surfaces().substring(
            surfaces().indexOf("fun CompactEyeTrackingHeader"),
            surfaces().indexOf("fun ExpandedEyeTrackingStatusPanel")
        )
        val pillIdx = compact.indexOf("EyeTrackingStatusPill")
        val counterIdx = compact.indexOf("BlinkCounterRow")
        val sensIdx = compact.indexOf("TrainingSensitivityControls")
        assertTrue(pillIdx >= 0 && counterIdx > pillIdx && sensIdx > counterIdx)
    }

    private fun readFile(relativeUnderMainJava: String): String {
        val path = "app/src/main/java/com/idworx/lisa/$relativeUnderMainJava"
        return ZeroTouchFileProbe.readProjectFile(path)
            ?: error("Missing source file: $path")
    }
}
