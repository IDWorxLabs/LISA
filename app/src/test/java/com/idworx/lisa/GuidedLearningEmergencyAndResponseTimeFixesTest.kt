package com.idworx.lisa

import com.idworx.lisa.features.zerotouchprinciple.audit.ZeroTouchFileProbe
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Structural checks (same [ZeroTouchFileProbe] pattern used throughout this codebase for
 * Activity/Compose UI wiring that can't be exercised in a pure JVM unit test) for two Guided
 * Learning issues found during real device testing:
 *
 * 1. The top control area showed "Response time" twice during a Navigation Lesson — one labelled
 *    row, and an extra near-identical row underneath.
 * 2. The Emergency lesson used a fake/practice-only shortcut instead of the real
 *    confirm/alarm/flash flow the normal Communication Workspace uses.
 */
class GuidedLearningEmergencyAndResponseTimeFixesTest {

    private fun mainActivitySource(): String =
        ZeroTouchFileProbe.readProjectFile("app/src/main/java/com/idworx/lisa/MainActivity.kt")
            ?: error("MainActivity.kt not found")

    private fun accessibilityUiSource(): String =
        ZeroTouchFileProbe.readProjectFile("app/src/main/java/com/idworx/lisa/LisaAccessibilityUi.kt")
            ?: error("LisaAccessibilityUi.kt not found")

    private fun trainingSessionControllerSource(): String =
        ZeroTouchFileProbe.readProjectFile(
            "app/src/main/java/com/idworx/lisa/features/onboardingguide/services/TrainingSessionController.kt"
        ) ?: error("TrainingSessionController.kt not found")

    // --- 1. Duplicate response-time display during Guided Learning --------------------------------

    @Test
    fun guidedTraining_responseTimeRowsAreMutuallyExclusive_neverBothAtOnce() {
        val ui = accessibilityUiSource()
        val fn = ui.substringAfter("private fun CompactSensitivityControls(")
        assertTrue(fn.contains("if (guidedResponseTimeControlsVisible) {"))
        assertTrue(fn.contains("} else {"))
        val guidedBranch = fn.substringAfter("if (guidedResponseTimeControlsVisible) {").substringBefore("} else {")
        assertTrue(
            "guided row must have its own labelled -/+ buttons, not bare symbols",
            guidedBranch.contains("uiStrings.responseTimeDecrease") && guidedBranch.contains("uiStrings.responseTimeIncrease")
        )
        assertTrue(guidedBranch.contains("onClick = onDecreaseGuidedResponseTime"))
        assertTrue(guidedBranch.contains("onClick = onIncreaseGuidedResponseTime"))
        // Exactly one "Response time: <n>s" labelled Text template exists per branch (two total,
        // never both rendered at the same time since the branches are mutually exclusive).
        val labelledRowCount = fn.split("text = \"\${uiStrings.responseTime}: \$").size - 1
        assertTrue("expected exactly 2 labelled response-time rows (one per branch), found $labelledRowCount", labelledRowCount == 2)
    }

    @Test
    fun guidedTraining_soleVisibleRowIsAdjustableWithMinusPlus() {
        val ui = accessibilityUiSource()
        val fn = ui.substringAfter("private fun CompactSensitivityControls(")
        val guidedBranch = fn.substringAfter("if (guidedResponseTimeControlsVisible) {").substringBefore("} else {")
        assertTrue(guidedBranch.contains("enabled = guidedResponseTimeSec > SequenceProcessingDelay.MIN_SECONDS"))
        assertTrue(guidedBranch.contains("enabled = guidedResponseTimeSec < SequenceProcessingDelay.MAX_SECONDS"))
    }

    @Test
    fun normalWorkspace_stillShowsExactlyOneResponseTimeControl_afterGuidedLearning() {
        // Outside a Navigation Lesson, guidedResponseTimeControlsVisible is always false, so the
        // else branch (the single, pre-existing workspace row) is the only one ever rendered.
        val ui = accessibilityUiSource()
        assertTrue(ui.contains("guidedTrainingActive && guidedTrainingState.phase == TrainingPhase.NavigationLesson"))
        assertTrue(ui.contains("guidedResponseTimeControlsVisible = guidedWorkspaceTrainingActive"))
    }

    @Test
    fun sensitivityRow_wasNotRemoved() {
        val ui = accessibilityUiSource()
        val fn = ui.substringAfter("private fun CompactSensitivityControls(")
        assertTrue(fn.contains("uiStrings.sensitivityDecrease") && fn.contains("uiStrings.sensitivityIncrease"))
    }

    @Test
    fun responseTimeDefault_remainsFiveSeconds() {
        assertTrue(SequenceProcessingDelay.DEFAULT_SECONDS == 5)
        assertTrue(SequenceProcessingDelay.GUIDED_DEFAULT_SECONDS == 5)
    }

    // --- 2. Real Emergency alarm/flash during the Guided Learning Emergency lesson -----------------

    @Test
    fun emergencyLesson_blinkPath_routesToRealConfirmFlow_beforeAnyTrainingInterception() {
        val main = mainActivitySource()
        val finalizeFn = main.substringAfter("private fun finalizeSequence() {").substringBefore("\n    private fun ")
        val emergencyBlock = finalizeFn.substringAfter("isEmergencySequence(capturedLeft, capturedRight)) {")
            .substringBefore("if (trainingSession.hasActiveBrain1Decision())")
        assertTrue(emergencyBlock.contains("trainingSession.expectedNavigationAction() == NavigationAction.TriggerEmergency"))
        assertTrue(emergencyBlock.contains("trainingSession.beginEmergencyConfirm()"))
        // The old fake/simulated shortcut must be gone from the emergency block entirely.
        assertFalse("real Emergency lesson path must never use the old fake shortcut", emergencyBlock.contains("verifyTrainingNavigation"))
    }

    @Test
    fun emergencyLesson_otherLessons_stillRejectOffTargetEmergencyGesture() {
        // Only the Emergency lesson's own target reaches the real flow — every other lesson keeps
        // routing an emergency-shaped gesture through the pre-existing training-reject gate
        // (acceptedByCurrentNavigationLesson, verified unchanged by GTLF_001/GTLF_007).
        val main = mainActivitySource()
        val finalizeFn = main.substringAfter("private fun finalizeSequence() {").substringBefore("\n    private fun ")
        assertTrue(finalizeFn.contains("val isEmergencyLessonTarget = trainingSession.isNavigationTrainingActive() &&"))
        assertTrue(finalizeFn.contains("if (trainingSession.isNavigationTrainingActive() && !isEmergencyLessonTarget) {"))
        assertTrue(
            finalizeFn.substringAfter("if (trainingSession.isNavigationTrainingActive() && !isEmergencyLessonTarget) {")
                .substringBefore("if (emergencyReport.finalOutcome")
                .contains("handleTrainingSequence(capturedLeft, capturedRight)")
        )
    }

    @Test
    fun emergencyLesson_touchPath_routesToRealConfirmFlow() {
        val main = mainActivitySource()
        val fn = main.substringAfter("private fun triggerGuidedEmergencyTouch() {").substringBefore("\n    private fun ")
        assertTrue(fn.contains("trainingSession.beginEmergencyConfirm()"))
        assertFalse("touch path must never fall back to the old fake shortcut", fn.contains("verifyTrainingNavigation(NavigationAction.TriggerEmergency)"))
        // The off-target rejection above it must still be intact and untouched.
        assertTrue(fn.contains("rejectNavigationTrainingGesture()"))
    }

    @Test
    fun emergencyConfirmed_startsRealAlarmAndOnlyThenAdvancesTheLesson() {
        val main = mainActivitySource()
        assertTrue(main.contains("trainingSession.onEmergencyConfirmed = {"))
        val wiring = main.substringAfter("trainingSession.onEmergencyConfirmed = {").substringBefore("\n        }")
        assertTrue(wiring.contains("startEmergencyMode()"))
        assertTrue(wiring.contains("verifyTrainingNavigation(NavigationAction.TriggerEmergency)"))
        // Real alarm must start before (never instead of) the lesson being allowed to advance.
        assertTrue(wiring.indexOf("startEmergencyMode()") < wiring.indexOf("verifyTrainingNavigation(NavigationAction.TriggerEmergency)"))
    }

    @Test
    fun emergencyCancel_sharedBrain1EngineTakesPriorityOverTrainingDispatch() {
        // Emergency's confirm/cancel is the SAME Brain1DecisionKind.EmergencyMode flow used
        // outside training. hasActiveBrain1Decision() is checked (and, if true, routed through
        // the shared engine) before the general isNavigationTrainingActive() dispatch runs, so an
        // armed Emergency banner's cancel gesture is never swallowed by navigation-lesson routing.
        val main = mainActivitySource()
        val finalizeFn = main.substringAfter("private fun finalizeSequence() {").substringBefore("\n    private fun ")
        val brain1CheckIndex = finalizeFn.indexOf("if (trainingSession.hasActiveBrain1Decision())")
        val generalNavigationTrainingCheckIndex = finalizeFn.lastIndexOf("trainingSession.isNavigationTrainingActive()")
        assertTrue(brain1CheckIndex >= 0 && generalNavigationTrainingCheckIndex >= 0)
        assertTrue(brain1CheckIndex < generalNavigationTrainingCheckIndex)
        // And cancelling a prompt-only decision (Emergency included) fully clears it rather than
        // silently re-arming — the fix that makes the red banner disappear immediately.
        assertTrue(trainingSessionControllerSource().contains("updated.kind.isPromptOnly -> {"))
        assertTrue(trainingSessionControllerSource().contains("state = state.copy(brain1Decision = state.brain1Decision.clear())"))
    }

    @Test
    fun nonEmergencyLessons_stillOnlyAdvanceOnTheirOwnExpectedGesture() {
        // The lesson-focus gate (acceptedByCurrentNavigationLesson) that protects every other
        // lesson from accidental cross-triggering is unchanged by this fix.
        val main = mainActivitySource()
        assertTrue(main.contains("private fun acceptedByCurrentNavigationLesson(left: Int, right: Int): Boolean {"))
        val gate = main.substringAfter("private fun acceptedByCurrentNavigationLesson(left: Int, right: Int): Boolean {")
            .substringBefore("\n    private fun ")
        assertTrue(gate.contains("val expected = trainingSession.expectedNavigationAction() ?: return true"))
        assertTrue(gate.contains("if (classified == expected) return true"))
    }
}
