package com.idworx.lisa.features.brain1interactionstandard

import com.idworx.lisa.features.brain1interactionstandard.engine.Brain1DecisionEngine
import com.idworx.lisa.features.brain1interactionstandard.model.Brain1DecisionKind
import com.idworx.lisa.features.brain1interactionstandard.model.Brain1DecisionOutcome
import com.idworx.lisa.features.brain1interactionstandard.model.Brain1DecisionPhase
import com.idworx.lisa.features.brain1interactionstandard.model.isPromptOnly
import com.idworx.lisa.features.zerotouchprinciple.audit.ZeroTouchFileProbe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers the real-device bug: after Emergency is armed and the user does the cancel gesture
 * (R1 then L1), the red "awaiting confirm" banner stayed visible because only [Brain1DecisionEngine]'s
 * `phase` reset to Idle while `kind` stayed EmergencyMode — and the banner is driven by `kind`.
 */
class Brain1EmergencyCancelTest {

    private val confirmOrder = listOf(true, false) // left then right
    private val cancelOrder = listOf(false, true) // right then left

    @Test
    fun isPromptOnly_trueForPromptOnlyKinds_falseForChoiceKinds() {
        assertTrue(Brain1DecisionKind.EmergencyMode.isPromptOnly)
        assertTrue(Brain1DecisionKind.ResetLearningProgress.isPromptOnly)
        assertTrue(Brain1DecisionKind.ReplayLearning.isPromptOnly)
        assertTrue(Brain1DecisionKind.Recalibration.isPromptOnly)
        assertFalse(Brain1DecisionKind.FirstLaunchGuidedLearning.isPromptOnly)
        assertFalse(Brain1DecisionKind.FirstLaunchSkipWorkspace.isPromptOnly)
        assertFalse(Brain1DecisionKind.None.isPromptOnly)
    }

    @Test
    fun engine_cancelSequence_returnsChooseAgain_butLeavesKindOnState() {
        // Documents the engine's raw (kind-preserving) behavior — this is exactly why the caller
        // (TrainingSessionController) must special-case prompt-only kinds instead of blindly
        // re-arming via beginAwaitingBrain1Decision(updated.kind).
        val armed = Brain1DecisionEngine.beginAwaitingConfirm(Brain1DecisionKind.EmergencyMode)
        val (updated, outcome) = Brain1DecisionEngine.handleSequence(armed, 1, 1, cancelOrder)
        assertEquals(Brain1DecisionOutcome.ChooseAgain, outcome)
        assertEquals(Brain1DecisionKind.EmergencyMode, updated.kind)
        assertEquals(Brain1DecisionPhase.Idle, updated.phase)
    }

    @Test
    fun engine_confirmSequence_stillClearsAndReturnsConfirmed() {
        val armed = Brain1DecisionEngine.beginAwaitingConfirm(Brain1DecisionKind.EmergencyMode)
        val (updated, outcome) = Brain1DecisionEngine.handleSequence(armed, 1, 1, confirmOrder)
        assertEquals(Brain1DecisionOutcome.Confirmed(Brain1DecisionKind.EmergencyMode), outcome)
        assertEquals(Brain1DecisionKind.None, updated.kind)
        assertFalse(updated.isActive)
    }

    @Test
    fun engine_cancelOrderNeverAlsoMatchesConfirm() {
        val armed = Brain1DecisionEngine.beginAwaitingConfirm(Brain1DecisionKind.EmergencyMode)
        val (_, outcome) = Brain1DecisionEngine.handleSequence(armed, 1, 1, cancelOrder)
        assertTrue(outcome is Brain1DecisionOutcome.ChooseAgain)
        assertFalse(outcome is Brain1DecisionOutcome.Confirmed)
    }

    @Test
    fun controllerSource_clearsPromptOnlyDecisionsOnChooseAgain_insteadOfReArming() {
        // TrainingSessionController needs a live Android Context (SharedPreferences) to
        // instantiate directly in a pure JVM test, so this asserts the actual fix is wired in the
        // same way the rest of this codebase's `audit/*Auditor.kt` static checks do.
        val controller = ZeroTouchFileProbe.readProjectFile(
            "app/src/main/java/com/idworx/lisa/features/onboardingguide/services/TrainingSessionController.kt"
        )
        assertTrue(controller != null)
        assertTrue(controller!!.contains("updated.kind.isPromptOnly"))
        assertTrue(controller.contains("state = state.copy(brain1Decision = state.brain1Decision.clear())"))
        // Must not have regressed back to the buggy blanket re-arm for every kind.
        val chooseAgainIndex = controller.indexOf("Brain1DecisionOutcome.ChooseAgain ->")
        val promptOnlyIndex = controller.indexOf("updated.kind.isPromptOnly")
        val elseReArmIndex = controller.indexOf("else -> beginAwaitingBrain1Decision(updated.kind)")
        assertTrue(chooseAgainIndex in 0 until promptOnlyIndex)
        assertTrue(promptOnlyIndex in 0 until elseReArmIndex)
    }

    @Test
    fun emergencyBanner_isDrivenByBrain1DecisionKind_soClearingKindHidesItImmediately() {
        val ui = ZeroTouchFileProbe.readProjectFile("app/src/main/java/com/idworx/lisa/LisaAccessibilityUi.kt")
        assertTrue(ui != null)
        assertTrue(
            ui!!.contains("emergencyAwaitingConfirm = guidedTrainingState.brain1Decision.kind ==") &&
                ui.contains("Brain1DecisionKind.EmergencyMode")
        )
    }
}
