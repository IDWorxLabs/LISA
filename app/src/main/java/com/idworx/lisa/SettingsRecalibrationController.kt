package com.idworx.lisa

import com.idworx.lisa.features.intelligentstartup.engine.CalibrationFrameSample
import com.idworx.lisa.features.intelligentstartup.engine.QuickEyeCalibrationEngine
import com.idworx.lisa.features.intelligentstartup.model.ProfileEyeCalibration
import com.idworx.lisa.features.intelligentstartup.model.QuickCalibrationStep

/**
 * Manual recalibration from Primary Settings — reuses [QuickEyeCalibrationEngine]
 * (same engine as Intelligent Startup). Does not invent a second calibration model.
 */
enum class SettingsRecalibrationOutcome {
    Idle,
    InProgress,
    Succeeded,
    Failed
}

data class SettingsRecalibrationState(
    val active: Boolean = false,
    val step: QuickCalibrationStep = QuickCalibrationStep.LookNaturally,
    val outcome: SettingsRecalibrationOutcome = SettingsRecalibrationOutcome.Idle,
    val blinksCollected: Int = 0,
    val leftWinksCollected: Int = 0,
    val rightWinksCollected: Int = 0
)

class SettingsRecalibrationController(
    private val persistCalibration: (ProfileEyeCalibration) -> Unit,
    private val nowMs: () -> Long = { System.currentTimeMillis() },
    private val onStateChanged: (SettingsRecalibrationState) -> Unit = {},
    private val onSucceeded: () -> Unit = {},
    private val scheduleCompleteHandoff: (delayMs: Long, action: () -> Unit) -> Unit =
        { _, action -> action() }
) {
    private val engine = QuickEyeCalibrationEngine()
    private var lookNaturallyStartedMs: Long = 0L

    var state: SettingsRecalibrationState = SettingsRecalibrationState()
        private set

    val isActive: Boolean get() = state.active

    fun start() {
        engine.reset()
        lookNaturallyStartedMs = 0L
        state = SettingsRecalibrationState(
            active = true,
            step = QuickCalibrationStep.LookNaturally,
            outcome = SettingsRecalibrationOutcome.InProgress
        )
        publish()
    }

    /** Cancel without writing a new calibration — previous profile value is preserved. */
    fun cancel() {
        engine.reset()
        lookNaturallyStartedMs = 0L
        state = SettingsRecalibrationState()
        publish()
    }

    fun retry() {
        engine.reset()
        lookNaturallyStartedMs = 0L
        state = SettingsRecalibrationState(
            active = true,
            step = QuickCalibrationStep.LookNaturally,
            outcome = SettingsRecalibrationOutcome.InProgress
        )
        publish()
    }

    fun onFrameSample(
        leftOpenness: Float,
        rightOpenness: Float,
        faceWidthNormalized: Float,
        eyeSpacingProxy: Float = faceWidthNormalized * 0.35f
    ) {
        if (!state.active || state.outcome != SettingsRecalibrationOutcome.InProgress) return
        val step = state.step
        val sample = CalibrationFrameSample(
            leftOpenness = leftOpenness,
            rightOpenness = rightOpenness,
            faceWidthNormalized = faceWidthNormalized,
            eyeSpacingProxy = eyeSpacingProxy,
            timestampMs = nowMs()
        )
        val blinkCompleted = engine.onFrame(step, sample)
        if (step == QuickCalibrationStep.BlinkThreeTimes && blinkCompleted) {
            state = state.copy(blinksCollected = engine.blinkCount())
            publish()
            if (engine.blinksReady()) advanceStep()
            return
        }
        if (step == QuickCalibrationStep.LookNaturally) {
            if (lookNaturallyStartedMs == 0L) lookNaturallyStartedMs = nowMs()
            val elapsed = nowMs() - lookNaturallyStartedMs
            if (engine.baselineReady() && elapsed >= LOOK_NATURALLY_MIN_MS) {
                lookNaturallyStartedMs = 0L
                advanceStep()
            }
        }
    }

    fun onBothBlinkAccepted(closePeak: Float, durationMs: Long) {
        if (!state.active || state.step != QuickCalibrationStep.BlinkThreeTimes) return
        val count = engine.onBothBlinkAccepted(state.step, closePeak, durationMs)
        state = state.copy(blinksCollected = count)
        publish()
        if (engine.blinksReady()) advanceStep()
    }

    fun onLeftWinkAccepted(closePeak: Float) {
        if (!state.active || state.step != QuickCalibrationStep.LeftWinkTwice) return
        val count = engine.onLeftWinkAccepted(state.step, closePeak)
        state = state.copy(leftWinksCollected = count)
        publish()
        if (engine.leftWinksReady()) advanceStep()
    }

    fun onRightWinkAccepted(closePeak: Float) {
        if (!state.active || state.step != QuickCalibrationStep.RightWinkTwice) return
        val count = engine.onRightWinkAccepted(state.step, closePeak)
        state = state.copy(rightWinksCollected = count)
        publish()
        if (engine.rightWinksReady()) finalizeSuccess()
    }

    fun notifyTimeoutFailure() {
        if (!state.active) return
        state = state.copy(outcome = SettingsRecalibrationOutcome.Failed)
        publish()
    }

    private fun advanceStep() {
        val next = state.step.next() ?: return
        state = state.copy(
            step = next,
            blinksCollected = engine.blinkCount(),
            leftWinksCollected = engine.leftWinkCount(),
            rightWinksCollected = engine.rightWinkCount()
        )
        publish()
        if (next == QuickCalibrationStep.CalibrationComplete) {
            finalizeSuccess()
        }
    }

    private fun finalizeSuccess() {
        val built = engine.buildCalibration(nowMs())
        if (built == null) {
            state = state.copy(outcome = SettingsRecalibrationOutcome.Failed)
            publish()
            return
        }
        persistCalibration(built)
        state = state.copy(
            step = QuickCalibrationStep.CalibrationComplete,
            outcome = SettingsRecalibrationOutcome.Succeeded
        )
        publish()
        scheduleCompleteHandoff(SUCCESS_HANDOFF_MS) {
            if (state.outcome == SettingsRecalibrationOutcome.Succeeded) {
                state = SettingsRecalibrationState()
                publish()
                onSucceeded()
            }
        }
    }

    private fun publish() = onStateChanged(state)

    companion object {
        const val LOOK_NATURALLY_MIN_MS: Long = 1200L
        const val SUCCESS_HANDOFF_MS: Long = 1200L
        /** Retry uses Select (L1 R1) when failure is shown — same as Confirm elsewhere. */
        val RETRY_LEFT: Int = GuidedModeNavigation.SELECT_LEFT
        val RETRY_RIGHT: Int = GuidedModeNavigation.SELECT_RIGHT
    }
}
