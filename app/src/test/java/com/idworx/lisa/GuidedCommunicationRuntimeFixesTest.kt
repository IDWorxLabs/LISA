package com.idworx.lisa

import com.idworx.lisa.features.zerotouchprinciple.audit.ZeroTouchFileProbe
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Structural checks for the parts of this fix set that live in Compose UI / the Activity and
 * can't be exercised directly in a pure JVM unit test (no Robolectric/Compose-test infra in this
 * project — see the existing per-feature `audit` objects for the established pattern). The pure
 * logic these wire up (response-time bounds/coercion, sequence idle-allowance math, and the
 * visibility-gated navigation controller) is covered directly in [SequenceProcessingDelayTest]
 * and [GuidedVocabularyPagerTest].
 */
class GuidedCommunicationRuntimeFixesTest {

    private fun mainActivitySource(): String =
        ZeroTouchFileProbe.readProjectFile("app/src/main/java/com/idworx/lisa/MainActivity.kt")
            ?: error("MainActivity.kt not found")

    private fun accessibilityUiSource(): String =
        ZeroTouchFileProbe.readProjectFile("app/src/main/java/com/idworx/lisa/LisaAccessibilityUi.kt")
            ?: error("LisaAccessibilityUi.kt not found")

    // --- C. Response-time +/- controls, same style as Sensitivity -----------------------------

    @Test
    fun responseTimeControls_addedNextToSensitivity_sameOutlinedButtonStyle() {
        val ui = accessibilityUiSource()
        val block = ui.substringAfter("private fun CompactSensitivityControls(")
        assertTrue("expected a dedicated response time -/+ row", block.contains("onDecreaseResponseTime"))
        assertTrue(block.contains("onIncreaseResponseTime"))
        assertTrue(block.contains("uiStrings.responseTimeDecrease"))
        assertTrue(block.contains("uiStrings.responseTimeIncrease"))
        assertTrue(block.contains("responseTimeSec > SequenceProcessingDelay.MIN_SECONDS"))
        assertTrue(block.contains("responseTimeSec < SequenceProcessingDelay.MAX_SECONDS"))
    }

    @Test
    fun responseTimeControls_exactlyOneRowRendered_neverBothAtOnce() {
        // Real-device testing found Guided Training's Navigation Lesson showing the response-time
        // row TWICE (the everyday workspace row plus the guided-only row underneath). Fix: they are
        // mutually exclusive branches of the same if/else, so exactly one ever renders — the guided
        // row while guidedResponseTimeControlsVisible is true (it's the value actually driving
        // gesture timing then), otherwise the everyday workspace row.
        val ui = accessibilityUiSource()
        val block = ui.substringAfter("private fun CompactSensitivityControls(")
            .substringAfter("if (guidedResponseTimeControlsVisible) {")
        val guidedRow = block.substringBefore("} else {")
        val workspaceRow = block.substringAfter("} else {").substringBefore("\n    }\n}")
        assertTrue("guided row must use the labelled onClick handler", guidedRow.contains("onClick = onDecreaseGuidedResponseTime"))
        assertTrue("guided row must use the labelled onClick handler", guidedRow.contains("onClick = onIncreaseGuidedResponseTime"))
        assertTrue("guided row must be clearly labelled, not bare -/+ symbols", guidedRow.contains("uiStrings.responseTimeDecrease"))
        assertTrue("guided row must be clearly labelled, not bare -/+ symbols", guidedRow.contains("uiStrings.responseTimeIncrease"))
        assertTrue(workspaceRow.contains("onClick = onDecreaseResponseTime"))
        assertTrue(workspaceRow.contains("onClick = onIncreaseResponseTime"))
        // Never both rows' onClick handlers appearing outside their own exclusive branch.
        assertTrue("workspace row's own handler must not leak into the guided branch", !guidedRow.contains("onClick = onDecreaseResponseTime"))
        assertTrue("guided row's own handler must not leak into the workspace branch", !workspaceRow.contains("onClick = onDecreaseGuidedResponseTime"))
    }

    @Test
    fun mainActivity_wiresResponseTimeButtons_toCoercedChangeResponseTime() {
        val main = mainActivitySource()
        assertTrue(main.contains("onResponseTimeDecrease = { changeResponseTime(-1) }"))
        assertTrue(main.contains("onResponseTimeIncrease = { changeResponseTime(1) }"))
        assertTrue(main.contains("private fun changeResponseTime(deltaSeconds: Int)"))
        val fn = main.substringAfter("private fun changeResponseTime(deltaSeconds: Int)")
        assertTrue(fn.contains("SequenceProcessingDelay.coerce"))
        assertTrue(fn.contains("applySequenceProcessingDelay"))
    }

    // --- A. Emergency cancel banner cleanup ----------------------------------------------------

    @Test
    fun emergencyBanner_wiredFromBrain1DecisionKind_inAccessibilityUi() {
        val ui = accessibilityUiSource()
        assertTrue(ui.contains("emergencyAwaitingConfirm = guidedTrainingState.brain1Decision.kind =="))
    }
}
