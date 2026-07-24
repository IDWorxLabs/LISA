package com.idworx.lisa

import com.idworx.lisa.features.zerotouchprinciple.audit.ZeroTouchFileProbe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * RC8.11 — exactly one “Watching your eyes” eye-tracking headline per surface, and
 * Guided Readiness ends after Back / Continue (no TrainingSettingsSection below).
 */
class Rc8_11NoDuplicateEyeHeaderAndMinimalGuidedReadinessTest {

    private fun read(pathUnderMainJava: String): String {
        val path = "app/src/main/java/com/idworx/lisa/$pathUnderMainJava"
        return ZeroTouchFileProbe.readProjectFile(path)
            ?: error("Missing source: $path")
    }

    private fun communicationHeaderSlice(): String {
        val ui = read("LisaAccessibilityUi.kt")
        return ui.substringAfter("if (showSharedBlinkStatusHeader)")
            .substringBefore("if (showGuidedVocabularyOverlay)")
    }

    private fun readyBranch(): String {
        val setup = read("features/onboardingguide/ui/TrainingSetupScreen.kt")
        val elseIdx = setup.indexOf("else -> {")
        assertTrue(elseIdx >= 0)
        val end = setup.indexOf("private fun LisaUiStrings.t(", elseIdx)
        return setup.substring(elseIdx, end)
    }

    // --- Duplicate Watching header ownership ----------------------------------------------------

    @Test
    fun communicationRendersExactlyOneUniversalEyeTrackingHeader() {
        val header = communicationHeaderSlice()
        assertEquals(1, Regex("UniversalEyeTrackingHeader\\(").findAll(header).count())
        assertTrue(header.contains("passiveEyeTrackingOwnedByUniversalHeader"))
        assertTrue(header.contains("isPassiveEyeTrackingHeadline"))
        assertFalse(header.contains("calmEyeStatusOnly"))
    }

    @Test
    fun everydayPanelSkippedForPassiveEyeTrackingBanners() {
        val ui = read("LisaAccessibilityUi.kt")
        assertTrue(ui.contains("fun isPassiveEyeTrackingHeadline("))
        assertTrue(ui.contains("eyeTrackingStatusWatching"))
        assertTrue(ui.contains("eyeTrackingStatusCalibrating"))
        assertTrue(ui.contains("eyeTrackingStatusNoFace"))
        assertTrue(ui.contains("eyeTrackingStatusLookAtCamera"))
        assertTrue(ui.contains("eyeTrackingStatusTrackingLost"))
        val header = communicationHeaderSlice()
        assertTrue(header.contains("if (!passiveEyeTrackingOwnedByUniversalHeader)"))
        assertTrue(header.contains("EverydayCommunicationPanel("))
    }

    @Test
    fun menuDoesNotComposeASecondWatchingHeader() {
        val ui = read("LisaAccessibilityUi.kt")
        val header = communicationHeaderSlice()
        assertEquals(1, Regex("UniversalEyeTrackingHeader\\(").findAll(header).count())
        val menuHost = ui.substringAfter("if (mainMenuActive) {")
            .substringBefore("if (phraseManagementActive)")
        assertFalse(menuHost.contains("UniversalEyeTrackingHeader("))
        assertFalse(menuHost.contains("eyeTrackingStatusWatching"))
        assertFalse(menuHost.contains("Watching your eyes"))
    }

    @Test
    fun majorDestinationsUseSingleUniversalHeaderAuthority() {
        assertTrue(read("features/intelligentstartup/ui/IntelligentStartupFlow.kt")
            .contains("UniversalEyeTrackingHeader("))
        assertTrue(read("features/onboardingguide/ui/TrainingWelcomeScreen.kt")
            .contains("UniversalEyeTrackingHeader("))
        assertTrue(read("features/onboardingguide/ui/TrainingSetupScreen.kt")
            .contains("UniversalEyeTrackingHeader("))
        assertTrue(read("features/onboardingguide/ui/TrainingLessonScreens.kt")
            .contains("UniversalEyeTrackingHeader("))
        assertTrue(read("SettingsRecalibrationUi.kt").contains("UniversalEyeTrackingHeader("))
        assertTrue(communicationHeaderSlice().contains("UniversalEyeTrackingHeader("))
        // Welcome / lessons / setup each compose Universal once in their primary screens —
        // no EverydayCommunicationPanel dual banner on those surfaces.
        assertFalse(read("features/onboardingguide/ui/TrainingWelcomeScreen.kt")
            .contains("EverydayCommunicationPanel("))
        assertFalse(read("features/onboardingguide/ui/TrainingLessonScreens.kt")
            .contains("EverydayCommunicationPanel("))
        assertFalse(read("features/onboardingguide/ui/TrainingSetupScreen.kt")
            .contains("EverydayCommunicationPanel("))
        assertFalse(read("features/intelligentstartup/ui/IntelligentStartupFlow.kt")
            .contains("EverydayCommunicationPanel("))
    }

    // --- Guided readiness minimal composition ---------------------------------------------------

    @Test
    fun guidedReadinessKeepsRequiredReadyContent() {
        val ready = readyBranch()
        assertTrue(ready.contains("UniversalEyeTrackingHeader("))
        assertTrue(ready.contains("Ready to begin"))
        assertTrue(ready.contains("Your eyes are visible."))
        assertTrue(ready.contains("SetupDetectionStatusRow("))
        assertTrue(ready.contains("TrainingSecondaryButton("))
        assertTrue(ready.contains("TrainingPrimaryButton("))
        assertTrue(ready.contains("secondaryText = backSequenceLabel"))
        assertTrue(ready.contains("secondaryText = continueSequenceLabel"))
        assertTrue(ready.contains("Continue to first lesson"))
        val components = read("features/onboardingguide/ui/TrainingComponents.kt")
        val row = components.substringAfter("fun SetupDetectionStatusRow(")
            .substringBefore("fun TrainingSecondaryButton(")
        assertTrue(row.contains("\"Camera\""))
        assertTrue(row.contains("\"Face\""))
        assertTrue(row.contains("\"Eyes\""))
        assertEquals("L2 R2", formatWinkSequenceShort(2, 2))
        assertEquals("L1 R1", formatWinkSequenceShort(1, 1))
    }

    @Test
    fun guidedReadinessDoesNotComposeLearningManagementBelowContinue() {
        val ready = readyBranch()
        assertFalse(ready.contains("TrainingSettingsSection("))
        assertFalse(ready.contains("LEARNING"))
        assertFalse(ready.contains("Getting Ready"))
        assertFalse(ready.contains("Replay tutorial") || ready.contains("replayLearningJourney"))
        assertFalse(ready.contains("Practice communication") || ready.contains("practiceCommunication"))
        assertFalse(ready.contains("Practice navigation") || ready.contains("practiceNavigation"))
        assertFalse(ready.contains("Clear progress") || ready.contains("clearLearningProgress"))
        assertFalse(ready.contains("Narration"))
        assertFalse(ready.contains("During training"))
        assertFalse(ready.contains("Voice speed") || ready.contains("voiceSpeed"))
        assertFalse(ready.contains("Voice volume") || ready.contains("voiceVolume"))
        val continueIdx = ready.indexOf("TrainingPrimaryButton(")
        assertTrue(continueIdx >= 0)
        assertFalse(ready.substring(continueIdx).contains("TrainingSettingsSection("))
    }

    @Test
    fun trainingSettingsSectionRemainsImplementedGlobally() {
        val section = read("features/onboardingguide/ui/TrainingSettingsSection.kt")
        assertTrue(section.contains("fun TrainingSettingsSection("))
        assertTrue(section.contains("replayLearningJourney") || section.contains("Replay"))
    }

    @Test
    fun rc8_9BackAndContinueRoutesRemainIntact() {
        val flow = read("features/onboardingguide/ui/GuidedTrainingFlow.kt")
        assertTrue(flow.contains("TrainingEvent.ReturnToWelcomeDestination"))
        assertTrue(flow.contains("TrainingEvent.CompleteSetup"))
        val controller = read("features/onboardingguide/services/TrainingSessionController.kt")
        assertTrue(controller.contains("fun returnToWelcomeDestinationFromReadiness"))
        assertTrue(controller.contains("fun handleSetupReadinessInteraction"))
        assertTrue(read("MainActivity.kt").contains("handleSetupReadinessInteraction"))
        val ready = readyBranch()
        assertFalse(ready.contains("ExpandedEyeTrackingStatusPanel("))
        assertFalse(ready.contains("LessonEyeStatusPanel("))
        assertFalse(ready.contains("2 Left + 2 Right"))
    }
}
