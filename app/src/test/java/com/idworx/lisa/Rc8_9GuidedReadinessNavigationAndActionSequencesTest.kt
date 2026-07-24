package com.idworx.lisa

import com.idworx.lisa.features.brain1interactionstandard.model.UniversalInteractionGestures
import com.idworx.lisa.features.intelligentstartup.authority.WelcomeStage
import com.idworx.lisa.features.onboardingguide.model.TrainingPhase
import com.idworx.lisa.features.onboardingguide.model.TrainingProgress
import com.idworx.lisa.features.onboardingguide.navigation.GuidedTrainingNavigator
import com.idworx.lisa.features.onboardingguide.services.TrainingSessionController
import com.idworx.lisa.features.onboardingguide.state.TrainingEvent
import com.idworx.lisa.features.zerotouchprinciple.audit.ZeroTouchFileProbe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * RC8.9 — Guided Learning readiness: remove duplicate Camera/Eyes summary, show L2 R2 / L1 R1
 * on Back / Continue, and return Back to Welcome destination selection.
 */
class Rc8_9GuidedReadinessNavigationAndActionSequencesTest {

    private val navigator = GuidedTrainingNavigator()

    private fun read(pathUnderMainJava: String): String {
        val path = "app/src/main/java/com/idworx/lisa/$pathUnderMainJava"
        return ZeroTouchFileProbe.readProjectFile(path)
            ?: error("Missing source: $path")
    }

    private fun readyBlock(setup: String): String {
        // Ready branch starts at the else of setup step when (eye detection vs ready).
        val elseIdx = setup.indexOf("else -> {")
        assertTrue("missing ready else branch", elseIdx >= 0)
        // RC8.11 — readiness ends after Continue (TrainingSettingsSection unwired).
        val end = setup.indexOf("private fun LisaUiStrings.t(", elseIdx)
        assertTrue(end > elseIdx)
        return setup.substring(elseIdx, end)
    }

    @Test
    fun duplicatedUpperCameraEyesSummaryRemovedFromReadyScreen() {
        val setup = read("features/onboardingguide/ui/TrainingSetupScreen.kt")
        val ready = readyBlock(setup)
        assertTrue(ready.contains("UniversalEyeTrackingHeader("))
        assertFalse(ready.contains("ExpandedEyeTrackingStatusPanel("))
        assertFalse(ready.contains("LessonEyeStatusPanel("))
        // Large status card remains under “Your eyes are visible.”
        assertTrue(ready.contains("SetupDetectionStatusRow("))
        assertTrue(ready.contains("Your eyes are visible."))
    }

    @Test
    fun largerCameraFaceEyesStatusCardRemains() {
        val setup = read("features/onboardingguide/ui/TrainingSetupScreen.kt")
        val ready = readyBlock(setup)
        assertTrue(ready.contains("SetupDetectionStatusRow("))
        val components = read("features/onboardingguide/ui/TrainingComponents.kt")
        val row = components.substringAfter("fun SetupDetectionStatusRow(")
            .substringBefore("fun TrainingSecondaryButton(")
        assertTrue(row.contains("\"Camera\""))
        assertTrue(row.contains("\"Face\""))
        assertTrue(row.contains("\"Eyes\""))
    }

    @Test
    fun backAndContinueDisplayCanonicalSequences() {
        val setup = read("features/onboardingguide/ui/TrainingSetupScreen.kt")
        val ready = readyBlock(setup)
        assertTrue(ready.contains("TrainingSecondaryButton("))
        assertTrue(ready.contains("TrainingPrimaryButton("))
        assertTrue(setup.contains("GuidedReadinessSequenceAuthority.backSequenceLabel()"))
        assertTrue(setup.contains("GuidedReadinessSequenceAuthority.continueSequenceLabel()"))
        assertTrue(ready.contains("secondaryText = backSequenceLabel"))
        assertTrue(ready.contains("secondaryText = continueSequenceLabel"))
        assertFalse(ready.contains("guidedBackHint"))
        assertFalse(ready.contains("2 Left + 2 Right"))
        assertEquals("L2 R2", formatWinkSequenceShort(2, 2))
        assertEquals(
            "L1 R1",
            formatWinkSequenceShort(
                UniversalInteractionGestures.CONFIRM_LEFT,
                UniversalInteractionGestures.CONFIRM_RIGHT
            )
        )
        assertEquals(GuidedModeNavigation.BACK_LEFT, 2)
        assertEquals(GuidedModeNavigation.BACK_RIGHT, 2)
    }

    @Test
    fun backReturnsToWelcomeDestinationSelectionWithoutResettingSession() {
        val fromSetup = TrainingProgress(
            firstLaunchChoiceMade = true,
            tutorialStarted = true,
            currentPhase = TrainingPhase.Setup,
            calibrationCompleted = false,
            preferences = com.idworx.lisa.features.onboardingguide.model.TrainingPreferences(
                guidedResponseTimeSec = 4
            )
        )
        val returned = navigator.reduce(fromSetup, TrainingEvent.ReturnToWelcomeDestination)
        assertEquals(TrainingPhase.FirstLaunchChoice, returned.currentPhase)
        assertTrue(returned.firstLaunchChoiceMade)
        assertTrue(returned.tutorialStarted)
        assertEquals(4, returned.preferences.guidedResponseTimeSec)
        // Does not wipe preference / session markers.
        assertFalse(returned.tutorialCompleted)

        val controller = read("features/onboardingguide/services/TrainingSessionController.kt")
        assertTrue(controller.contains("fun returnToWelcomeDestinationFromReadiness"))
        assertTrue(controller.contains("WelcomeStage.DestinationSelection"))
        assertTrue(controller.contains("TrainingEvent.ReturnToWelcomeDestination"))
        assertFalse(
            controller.substringAfter("fun returnToWelcomeDestinationFromReadiness")
                .substringBefore("fun handleSetupReadinessInteraction")
                .contains("store.reset()")
        )
    }

    @Test
    fun touchAndBlinkShareBackAndContinueActions() {
        val flow = read("features/onboardingguide/ui/GuidedTrainingFlow.kt")
        assertTrue(flow.contains("TrainingEvent.ReturnToWelcomeDestination"))
        assertTrue(flow.contains("TrainingEvent.CompleteSetup"))

        val controller = read("features/onboardingguide/services/TrainingSessionController.kt")
        val handler = controller.substringAfter("fun handleSetupReadinessInteraction")
            .substringBefore("private fun clearWelcomeGestureResidue")
        assertTrue(handler.contains("GuidedReadinessSequenceAuthority"))
        assertTrue(handler.contains("invokeFromBlink"))
        assertTrue(handler.contains("ReturnToWelcomeDestination"))
        assertTrue(handler.contains("CompleteSetup"))

        val main = read("MainActivity.kt")
        assertTrue(main.contains("handleSetupReadinessInteraction"))
        assertTrue(main.contains("shouldShowTraining()"))
    }

    @Test
    fun continueStartsFirstLessonViaCompleteSetup() {
        val after = navigator.reduce(
            TrainingProgress(currentPhase = TrainingPhase.Setup, tutorialStarted = true),
            TrainingEvent.CompleteSetup
        )
        assertEquals(TrainingPhase.CommunicationLesson, after.currentPhase)
        assertEquals(0, after.communicationLessonIndex)
    }

    @Test
    fun readinessScreenOrderUsesUniversalHeaderThenReadyContent() {
        val setup = read("features/onboardingguide/ui/TrainingSetupScreen.kt")
        val elseIdx = setup.indexOf("else -> {")
        assertTrue(elseIdx >= 0)
        val readySection = setup.substring(elseIdx, setup.indexOf("private fun LisaUiStrings.t(", elseIdx))
        val headerIdx = readySection.indexOf("UniversalEyeTrackingHeader(")
        val readyTitleIdx = readySection.indexOf("\"Ready to begin\"")
        val statusIdx = readySection.indexOf("SetupDetectionStatusRow(")
        val backIdx = readySection.indexOf("TrainingSecondaryButton(")
        val continueIdx = readySection.indexOf("TrainingPrimaryButton(")
        assertTrue(headerIdx >= 0)
        assertTrue(readyTitleIdx > headerIdx)
        assertTrue(statusIdx > readyTitleIdx)
        assertTrue(backIdx > statusIdx)
        assertTrue(continueIdx > backIdx)
        assertFalse(readySection.contains("TrainingSettingsSection("))
    }

    @Test
    fun setupStepConstantsUnchanged() {
        assertEquals(0, TrainingSessionController.SETUP_STEP_EYE_DETECTION)
        assertEquals(1, TrainingSessionController.SETUP_STEP_READY)
        assertEquals(WelcomeStage.DestinationSelection, WelcomeStage.DestinationSelection)
    }
}
