package com.idworx.lisa

import com.idworx.lisa.features.intelligentstartup.authority.WelcomeEyeNavigationAuthority
import com.idworx.lisa.features.onboardingguide.model.TrainingPhase
import com.idworx.lisa.features.onboardingguide.model.TrainingProgress
import com.idworx.lisa.features.onboardingguide.navigation.GuidedTrainingNavigator
import com.idworx.lisa.features.onboardingguide.state.TrainingEvent
import com.idworx.lisa.features.universalsequenceexecution.CategorySequenceCatalog
import com.idworx.lisa.features.universalsequenceexecution.GuidedReadinessSequenceAuthority
import com.idworx.lisa.features.universalsequenceexecution.MenuSequenceCatalog
import com.idworx.lisa.features.universalsequenceexecution.SettingsRecalibrationRetrySequenceAuthority
import com.idworx.lisa.features.universalsequenceexecution.UniversalSequenceExecutionAuthority
import com.idworx.lisa.features.universalsequenceexecution.WelcomeSequenceCatalog
import com.idworx.lisa.features.zerotouchprinciple.audit.ZeroTouchFileProbe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * RC8.12 — Touch and blink must invoke the identical action for every sequence-labelled control.
 */
class Rc8_12UniversalSequenceExecutionAuthorityTest {

    private fun read(pathUnderMainJava: String): String {
        val path = "app/src/main/java/com/idworx/lisa/$pathUnderMainJava"
        return ZeroTouchFileProbe.readProjectFile(path)
            ?: error("Missing source: $path")
    }

    private fun mainActivity(): String = read("MainActivity.kt")

    // --- Authority parity model -----------------------------------------------------------------

    @Test
    fun auditedCatalogHasTouchAndBlinkParity() {
        val violations = UniversalSequenceExecutionAuthority.debugValidateAuditedCatalog()
        assertTrue(
            "Unexpected parity violations: $violations",
            violations.isEmpty()
        )
        assertTrue(UniversalSequenceExecutionAuthority.auditedCatalog().size >= 8)
    }

    @Test
    fun validateParityDetectsMissingBlinkWiring() {
        val broken = UniversalSequenceExecutionAuthority.BoundControl(
            controlId = "broken",
            surface = "Test",
            left = 2,
            right = 2,
            touchWired = true,
            blinkWired = false
        )
        val violations = UniversalSequenceExecutionAuthority.validateParity(listOf(broken))
        assertEquals(1, violations.size)
        assertTrue(violations.first().reason.contains("blink"))
    }

    @Test
    fun runSharedInvokesIdenticalActionForTouchAndBlink() {
        var count = 0
        val action = { count += 1 }
        UniversalSequenceExecutionAuthority.runShared(action)
        UniversalSequenceExecutionAuthority.runShared(action)
        assertEquals(2, count)
    }

    // --- Guided readiness touch == blink --------------------------------------------------------

    @Test
    fun readinessBackTouchAndBlinkResolveSameAction() {
        assertEquals(
            GuidedReadinessSequenceAuthority.Action.Back,
            GuidedReadinessSequenceAuthority.resolve(
                GuidedReadinessSequenceAuthority.BACK_LEFT,
                GuidedReadinessSequenceAuthority.BACK_RIGHT
            )
        )
        var touchEvent: TrainingEvent? = null
        var blinkEvent: TrainingEvent? = null
        GuidedReadinessSequenceAuthority.invoke(
            GuidedReadinessSequenceAuthority.Action.Back,
            onBack = { touchEvent = TrainingEvent.ReturnToWelcomeDestination },
            onContinue = { touchEvent = TrainingEvent.CompleteSetup }
        )
        GuidedReadinessSequenceAuthority.invokeFromBlink(
            left = GuidedReadinessSequenceAuthority.BACK_LEFT,
            right = GuidedReadinessSequenceAuthority.BACK_RIGHT,
            onBack = { blinkEvent = TrainingEvent.ReturnToWelcomeDestination },
            onContinue = { blinkEvent = TrainingEvent.CompleteSetup }
        )
        assertEquals(TrainingEvent.ReturnToWelcomeDestination, touchEvent)
        assertEquals(touchEvent, blinkEvent)
    }

    @Test
    fun readinessContinueTouchAndBlinkResolveSameAction() {
        assertEquals(
            GuidedReadinessSequenceAuthority.Action.Continue,
            GuidedReadinessSequenceAuthority.resolve(
                GuidedReadinessSequenceAuthority.CONTINUE_LEFT,
                GuidedReadinessSequenceAuthority.CONTINUE_RIGHT
            )
        )
        var touchEvent: TrainingEvent? = null
        var blinkEvent: TrainingEvent? = null
        GuidedReadinessSequenceAuthority.invoke(
            GuidedReadinessSequenceAuthority.Action.Continue,
            onBack = { touchEvent = TrainingEvent.ReturnToWelcomeDestination },
            onContinue = { touchEvent = TrainingEvent.CompleteSetup }
        )
        GuidedReadinessSequenceAuthority.invokeFromBlink(
            left = GuidedReadinessSequenceAuthority.CONTINUE_LEFT,
            right = GuidedReadinessSequenceAuthority.CONTINUE_RIGHT,
            onBack = { blinkEvent = TrainingEvent.ReturnToWelcomeDestination },
            onContinue = { blinkEvent = TrainingEvent.CompleteSetup }
        )
        assertEquals(TrainingEvent.CompleteSetup, touchEvent)
        assertEquals(touchEvent, blinkEvent)
    }

    @Test
    fun readinessBlinkHandlerUsesSharedAuthorityAndEvents() {
        val controller = read("features/onboardingguide/services/TrainingSessionController.kt")
        val handler = controller.substringAfter("fun handleSetupReadinessInteraction")
            .substringBefore("private fun clearWelcomeGestureResidue")
        assertTrue(handler.contains("GuidedReadinessSequenceAuthority"))
        assertTrue(handler.contains("invokeFromBlink"))
        assertTrue(handler.contains("onBack = { dispatch(TrainingEvent.ReturnToWelcomeDestination) }"))
        assertTrue(handler.contains("dispatch(TrainingEvent.CompleteSetup)"))
        // Same events as touch paths in GuidedTrainingFlow / TrainingSetupScreen.
        val flow = read("features/onboardingguide/ui/GuidedTrainingFlow.kt")
        assertTrue(flow.contains("TrainingEvent.ReturnToWelcomeDestination"))
        assertTrue(flow.contains("TrainingEvent.CompleteSetup"))
    }

    @Test
    fun finalizeSequenceRoutesAllTrainingPhasesIncludingSetup() {
        val main = mainActivity()
        val finalize = main.substringAfter("private fun finalizeSequence()")
            .substringBefore("private fun handlePracticeSequence")
        assertTrue(finalize.contains("shouldShowTraining()"))
        assertTrue(finalize.contains("handleTrainingSequence("))
        // RC8.12 must not gate Setup out of training routing.
        assertFalse(
            finalize.contains(
                "currentPhase == TrainingPhase.CommunicationLesson ||\n" +
                    "                trainingSession.state.progress.currentPhase == TrainingPhase.CommunicationMastery"
            )
        )
        assertTrue(finalize.contains("RC8.12"))
        assertTrue(main.contains("handleSetupReadinessInteraction"))
    }

    @Test
    fun readinessUiUsesSharedAuthorityForTouchAndLabels() {
        val setup = read("features/onboardingguide/ui/TrainingSetupScreen.kt")
        assertTrue(setup.contains("GuidedReadinessSequenceAuthority.backSequenceLabel()"))
        assertTrue(setup.contains("GuidedReadinessSequenceAuthority.continueSequenceLabel()"))
        assertTrue(setup.contains("GuidedReadinessSequenceAuthority.invoke("))
        assertTrue(setup.contains("Action.Back"))
        assertTrue(setup.contains("Action.Continue"))
        assertEquals("L2 R2", GuidedReadinessSequenceAuthority.backSequenceLabel())
        assertEquals("L1 R1", GuidedReadinessSequenceAuthority.continueSequenceLabel())
    }

    // --- Menu / category / welcome / recalibration catalog --------------------------------------

    @Test
    fun menuAndCategoryCatalogMatchGuidedModeNavigation() {
        assertEquals(GuidedModeNavigation.SELECT_LEFT, MenuSequenceCatalog.SELECT_LEFT)
        assertEquals(GuidedModeNavigation.SELECT_RIGHT, MenuSequenceCatalog.SELECT_RIGHT)
        assertEquals(GuidedModeNavigation.BACK_LEFT, MenuSequenceCatalog.BACK_LEFT)
        assertEquals(GuidedModeNavigation.BACK_RIGHT, MenuSequenceCatalog.BACK_RIGHT)
        assertEquals(GuidedModeNavigation.SELECT_LEFT, CategorySequenceCatalog.SELECT_LEFT)
        assertEquals(GuidedModeNavigation.BACK_LEFT, CategorySequenceCatalog.BACK_LEFT)
    }

    @Test
    fun welcomeCatalogMatchesWelcomeEyeNavigationAuthority() {
        assertEquals(WelcomeEyeNavigationAuthority.continueLeft, WelcomeSequenceCatalog.CONTINUE_LEFT)
        assertEquals(WelcomeEyeNavigationAuthority.startGuidedLearningLeft, WelcomeSequenceCatalog.START_GUIDED_LEFT)
        assertEquals(WelcomeEyeNavigationAuthority.skipToCommunicationRight, WelcomeSequenceCatalog.SKIP_RIGHT)
        assertEquals(WelcomeEyeNavigationAuthority.backLeft, WelcomeSequenceCatalog.BACK_LEFT)
    }

    @Test
    fun recalibrationRetryTouchAndBlinkShareAuthority() {
        assertTrue(
            SettingsRecalibrationRetrySequenceAuthority.matches(
                SettingsRecalibrationRetrySequenceAuthority.RETRY_LEFT,
                SettingsRecalibrationRetrySequenceAuthority.RETRY_RIGHT
            )
        )
        var touch = 0
        var blink = 0
        SettingsRecalibrationRetrySequenceAuthority.invokeRetry { touch += 1 }
        SettingsRecalibrationRetrySequenceAuthority.invokeRetry { blink += 1 }
        assertEquals(1, touch)
        assertEquals(1, blink)
        val main = mainActivity()
        assertTrue(main.contains("SettingsRecalibrationRetrySequenceAuthority"))
        assertTrue(main.contains("SettingsRecalibrationOutcome.Failed"))
        val ui = read("SettingsRecalibrationUi.kt")
        assertTrue(ui.contains("SettingsRecalibrationRetrySequenceAuthority.invokeRetry"))
    }

    @Test
    fun debugValidatorIsDebugOnly() {
        val validator = read("features/universalsequenceexecution/UniversalSequenceExecutionAuthority.kt")
        assertTrue(validator.contains("fun runIfDebug(isDebugBuild: Boolean"))
        assertTrue(validator.contains("if (!isDebugBuild) return"))
        val main = mainActivity()
        assertTrue(main.contains("UniversalSequenceExecutionDebugValidator"))
        assertTrue(main.contains("FLAG_DEBUGGABLE"))
    }

    @Test
    fun trainingButtonsDocumentSequenceParityRequirement() {
        val components = read("features/onboardingguide/ui/TrainingComponents.kt")
        assertTrue(components.contains("RC8.12"))
        assertTrue(components.contains("secondaryText"))
        assertTrue(components.contains("blink"))
    }

    @Test
    fun navigatorStillMapsReadinessEventsUnchanged() {
        val navigator = GuidedTrainingNavigator()
        val fromSetup = TrainingProgress(
            currentPhase = TrainingPhase.Setup,
            tutorialStarted = true,
            firstLaunchChoiceMade = true
        )
        val back = navigator.reduce(fromSetup, TrainingEvent.ReturnToWelcomeDestination)
        assertEquals(TrainingPhase.FirstLaunchChoice, back.currentPhase)
        val cont = navigator.reduce(fromSetup, TrainingEvent.CompleteSetup)
        assertEquals(TrainingPhase.CommunicationLesson, cont.currentPhase)
    }
}
