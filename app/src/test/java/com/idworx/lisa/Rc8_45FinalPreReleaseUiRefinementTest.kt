package com.idworx.lisa

import com.idworx.lisa.features.guidednavigationaccessfloatingcard.audit.GuidedNavigationAccessFloatingCardAuditor
import com.idworx.lisa.features.guidedphraselessonpresentation.GuidedPhraseLessonPresentationAuthority
import com.idworx.lisa.features.intelligentstartup.authority.WelcomeEyeNavigationAuthority
import com.idworx.lisa.features.onboardingguide.lessons.TrainingLessonCatalog
import com.idworx.lisa.features.onboardingguide.model.TrainingProgress
import com.idworx.lisa.features.onboardingguide.navigation.GuidedTrainingNavigator
import com.idworx.lisa.features.onboardingguide.state.TrainingEvent
import com.idworx.lisa.features.onboardingguide.ui.WelcomeDestinationLayoutAuthority
import com.idworx.lisa.features.onboardingguide.ui.formatWinkGestureFriendly
import com.idworx.lisa.features.zerotouchprinciple.audit.ZeroTouchFileProbe
import com.idworx.lisa.formatWinkSequenceShort
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Final pre-release UI refinements:
 * 1) Calibration instruction group vertically centred in remaining workspace
 * 2) Caregiver Navigation Training shortcut removed from production destination UI
 * 3) Phrase lessons show To say / phrase / natural language / compact sequence
 */
class Rc8_45FinalPreReleaseUiRefinementTest {

    private val uiStrings = LisaUiStrings.forLanguage(PreferredLanguage.English)

    private fun read(path: String): String =
        ZeroTouchFileProbe.readProjectFile("app/src/main/java/com/idworx/lisa/$path")
            ?: error("Missing $path")

    private fun calibrationScreenBlock(): String {
        val source = read("features/intelligentstartup/ui/IntelligentStartupFlow.kt")
        val start = source.indexOf("fun QuickEyeCalibrationScreen(")
        val end = source.indexOf("fun CalibrationFailureScreen(", start)
            .takeIf { it > start } ?: source.length
        return source.substring(start, end)
    }

    private fun settingsRecalibrationBlock(): String {
        val source = read("SettingsRecalibrationUi.kt")
        val start = source.indexOf("fun SettingsRecalibrationPanel(")
        return source.substring(start)
    }

    private fun destinationBlock(): String {
        val source = read("features/onboardingguide/ui/TrainingWelcomeScreen.kt")
        val start = source.indexOf("fun WelcomeDestinationSelectionScreen")
        val end = source.indexOf("fun WelcomeBlinkNotationExplanation", start)
        return source.substring(start, end)
    }

    private fun communicationLessonBlock(): String {
        val source = read("features/onboardingguide/ui/TrainingLessonScreens.kt")
        val start = source.indexOf("fun CommunicationLessonScreen(")
        val end = source.indexOf("fun NavigationLessonScreen(", start)
        return source.substring(start, end)
    }

    private fun navigationLessonBlock(): String {
        val source = read("features/onboardingguide/ui/TrainingLessonScreens.kt")
        val start = source.indexOf("fun NavigationLessonScreen(")
        val end = source.indexOf("fun TrainingCalibrationScreen(", start)
            .takeIf { it > start } ?: source.length
        return source.substring(start, end)
    }

    private fun simplifiedGestureBlock(): String {
        val source = read("features/onboardingguide/ui/TrainingComponents.kt")
        val start = source.indexOf("fun SimplifiedGestureDisplay(")
        val end = source.indexOf("fun WinkSequenceDisplay(", start)
        return source.substring(start, end)
    }

    // --- Adjustment 1: Calibration centring ---

    @Test
    fun calibrationWorkspaceFillsRemainingHeightAndCentresInstructionGroup() {
        val block = calibrationScreenBlock()
        assertTrue(block.contains("UniversalEyeTrackingHeader"))
        assertTrue(block.contains("Camera active"))
        assertTrue(block.contains("weight(1f)"))
        assertTrue(block.contains("Arrangement.Center"))
        assertFalse(block.contains(".verticalScroll("))
        // Every calibration step shares the same centred instruction workspace.
        assertTrue(block.contains("QuickCalibrationStep.LookNaturally"))
        assertTrue(block.contains("QuickCalibrationStep.BlinkThreeTimes"))
        assertTrue(block.contains("QuickCalibrationStep.LeftWinkTwice"))
        assertTrue(block.contains("QuickCalibrationStep.RightWinkTwice"))
        assertTrue(block.contains("QuickCalibrationStep.CalibrationComplete"))
    }

    @Test
    fun settingsRecalibrationUsesSharedCentredWorkspace() {
        val block = settingsRecalibrationBlock()
        assertTrue(block.contains("weight(1f)"))
        assertTrue(block.contains("Arrangement.Center"))
        assertFalse(block.contains(".verticalScroll("))
        assertTrue(block.contains("UniversalEyeTrackingHeader"))
    }

    // --- Adjustment 2: Production destination — three actions only; no caregiver/research tools ---

    @Test
    fun destinationSelectionHasOnlyThreeProductionActions() {
        val block = destinationBlock()
        assertTrue(block.contains("startGuidedLearning"))
        assertTrue(block.contains("skipToCommunication"))
        assertTrue(block.contains("uiStrings.back") || block.contains("onBackToIntroduction"))
        assertFalse(block.contains("CaregiverAdvancedSkipLink"))
        assertFalse(block.contains("caregiverAdvancedSkipNavigation"))
        assertFalse(block.contains("onSkipToNavigationTraining"))
        assertFalse(block.contains("For caregivers"))
        assertFalse(block.contains("EyeTestModeAccess"))
        assertFalse(block.contains("SignalInvestigationAccess"))
        assertFalse(block.contains("GlassesCharacterisationAccess"))
        assertFalse(block.contains("PersonalisedEyeProfileAccess"))
        assertTrue(WelcomeDestinationLayoutAuthority.caregiverAbsentFromDestinationScreen(
            read("features/onboardingguide/ui/TrainingWelcomeScreen.kt")
        ))
        assertTrue(WelcomeDestinationLayoutAuthority.destinationOmitsResearchToolEntries(
            read("features/onboardingguide/ui/TrainingWelcomeScreen.kt")
        ))
        assertTrue(GuidedNavigationAccessFloatingCardAuditor.welcomeExposesSkipToNavigationTraining())
    }

    @Test
    fun destinationSequencesRemainL2R0_L0R2_L2R2() {
        assertEquals("L2 R0", WelcomeEyeNavigationAuthority.startGuidedLearningSequenceLabel())
        assertEquals("L0 R2", WelcomeEyeNavigationAuthority.skipToCommunicationSequenceLabel())
        assertEquals("L2 R2", WelcomeEyeNavigationAuthority.backSequenceLabel())
    }

    @Test
    fun skipToNavigationTrainingRemainsTestOnlyNavigatorEvent() {
        val flow = read("features/onboardingguide/ui/GuidedTrainingFlow.kt")
        assertFalse(flow.contains("onSkipToNavigationTraining"))
        val progress = GuidedTrainingNavigator().reduce(
            TrainingProgress(),
            TrainingEvent.SkipToNavigationTraining
        )
        assertEquals(
            com.idworx.lisa.features.onboardingguide.model.TrainingPhase.NavigationLesson,
            progress.currentPhase
        )
        assertEquals(0, progress.navigationLessonIndex)
    }

    // --- Adjustment 3: Phrase lesson wording ---

    @Test
    fun lesson1PhrasePresentationShowsToSayHelloNaturalAndCompact() {
        val hello = TrainingLessonCatalog.communicationLessons.first { it.id == "comm_hello" }
        assertEquals(2, hello.left)
        assertEquals(0, hello.right)
        assertEquals("To say", GuidedPhraseLessonPresentationAuthority.intentLabel(uiStrings))
        assertEquals(
            "Blink Left Twice",
            GuidedPhraseLessonPresentationAuthority.naturalLanguageInstruction(hello.left, hello.right)
        )
        assertEquals(
            "L2 R0",
            GuidedPhraseLessonPresentationAuthority.compactSequence(hello.left, hello.right)
        )
        val block = communicationLessonBlock()
        assertTrue(block.contains("GuidedPhraseLessonPresentationAuthority.intentLabel"))
        assertTrue(block.contains("GuidedLessonPhraseTitle"))
        assertTrue(block.contains("SimplifiedGestureDisplay"))
        val gesture = simplifiedGestureBlock()
        assertTrue(gesture.contains("naturalLanguageInstruction"))
        assertTrue(gesture.contains("compactSequence"))
    }

    @Test
    fun phraseLessonsDeriveCompactSequenceFromProductionMapping() {
        TrainingLessonCatalog.communicationLessons.forEach { lesson ->
            val expected = formatWinkSequenceShort(lesson.left, lesson.right)
            assertEquals(
                expected,
                GuidedPhraseLessonPresentationAuthority.compactSequence(lesson.left, lesson.right)
            )
            assertEquals(
                formatWinkGestureFriendly(lesson.left, lesson.right),
                GuidedPhraseLessonPresentationAuthority.naturalLanguageInstruction(
                    lesson.left,
                    lesson.right
                )
            )
        }
        val pain = TrainingLessonCatalog.communicationLessons.first { it.id == "comm_pain" }
        assertEquals("L2 R3", GuidedPhraseLessonPresentationAuthority.compactSequence(pain.left, pain.right))
        assertEquals(
            "Blink Left Twice and Right Three Times",
            GuidedPhraseLessonPresentationAuthority.naturalLanguageInstruction(pain.left, pain.right)
        )
    }

    @Test
    fun navigationLessonsDoNotIncludeToSayIntent() {
        val nav = navigationLessonBlock()
        assertFalse(nav.contains("GuidedPhraseLessonPresentationAuthority.intentLabel"))
        assertFalse(nav.contains("To say"))
        assertTrue(GuidedPhraseLessonPresentationAuthority.isPhraseSpeakLesson(true))
        assertFalse(GuidedPhraseLessonPresentationAuthority.isPhraseSpeakLesson(false))
    }

    @Test
    fun specialisedMismatchFeedbackStillPresentInCommunicationLesson() {
        val block = communicationLessonBlock()
        assertTrue(block.contains("retryVisualMessage"))
        assertTrue(block.contains("LisaEmergencyRed"))
        assertTrue(block.contains("successVisualMessage"))
    }
}
