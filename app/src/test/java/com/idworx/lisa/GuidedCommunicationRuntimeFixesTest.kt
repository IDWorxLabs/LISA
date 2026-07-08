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
    fun responseTimeControls_alwaysVisible_notGuardedByGuidedTrainingFlag() {
        // Unlike the pre-existing guided-training-only settle time row, the new row for the
        // everyday workspace response time must not be hidden behind guidedResponseTimeControlsVisible.
        val ui = accessibilityUiSource()
        val newRowStart = ui.indexOf("onClick = onDecreaseResponseTime")
        val guardStart = ui.indexOf("if (guidedResponseTimeControlsVisible)")
        assertTrue(newRowStart in 0 until guardStart)
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
