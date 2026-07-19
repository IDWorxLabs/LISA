package com.idworx.lisa.features.intelligentstartup

import com.idworx.lisa.features.intelligentstartup.authority.EyeCalibrationAuthority
import com.idworx.lisa.features.intelligentstartup.authority.StartupFlowAuthority
import com.idworx.lisa.features.intelligentstartup.engine.CalibrationFrameSample
import com.idworx.lisa.features.intelligentstartup.engine.QuickEyeCalibrationEngine
import com.idworx.lisa.features.intelligentstartup.model.CalibrationConfidenceLevel
import com.idworx.lisa.features.intelligentstartup.model.ProfileEyeCalibration
import com.idworx.lisa.features.intelligentstartup.model.QuickCalibrationStep
import com.idworx.lisa.features.intelligentstartup.model.StartupEvent
import com.idworx.lisa.features.intelligentstartup.model.StartupFlowState
import com.idworx.lisa.features.intelligentstartup.model.StartupPhase

/**
 * Host-side controller for the intelligent startup flow.
 * Keeps transitions inside [StartupFlowAuthority] and sample learning inside [QuickEyeCalibrationEngine].
 */
class StartupSessionController(
    private val loadProfileCalibration: () -> ProfileEyeCalibration?,
    private val persistCalibration: (ProfileEyeCalibration) -> Unit,
    private val nowMs: () -> Long = { System.currentTimeMillis() },
    private val onStateChanged: (StartupFlowState) -> Unit = {},
    private val onEyeControlActivated: () -> Unit = {},
    private val onStartupComplete: () -> Unit = {},
    private val scheduleReadyHandoff: (delayMs: Long, action: () -> Unit) -> Unit = { _, action -> action() },
    private val scheduleAutoRetry: (delayMs: Long, action: () -> Unit) -> Unit = { _, action -> action() }
) {
    private val engine = QuickEyeCalibrationEngine()
    private var lookNaturallyStartedMs: Long = 0L

    var state: StartupFlowState = StartupFlowState()
        private set

    val isActive: Boolean get() = state.blocksMainUi
    val eyeControlEnabled: Boolean get() = state.eyeControlActive

    fun start() {
        engine.reset()
        state = StartupFlowState()
        publish()
    }

    fun onFacePresence(present: Boolean) {
        if (!isActive) return
        val previous = state.faceDetected
        dispatch(StartupEvent.FacePresenceChanged(present))
        if (state.phase == StartupPhase.FaceDetection && present && !previous) {
            dispatch(StartupEvent.BeginConfidenceEvaluation)
            evaluateConfidenceAndContinue()
        }
        if (state.phase == StartupPhase.CalibrationFailure && present) {
            // Auto-retry once face is stable again — never ask the user.
            scheduleAutoRetry(900L) {
                if (state.phase == StartupPhase.CalibrationFailure) {
                    engine.reset()
                    dispatch(StartupEvent.RetryCalibration)
                }
            }
        }
    }

    fun onFrameSample(
        leftOpenness: Float,
        rightOpenness: Float,
        faceWidthNormalized: Float
    ) {
        if (!isActive) return
        if (state.phase != StartupPhase.QuickCalibration) return
        val step = state.calibrationStep
        val sample = CalibrationFrameSample(
            leftOpenness = leftOpenness,
            rightOpenness = rightOpenness,
            faceWidthNormalized = faceWidthNormalized,
            timestampMs = nowMs()
        )
        val blinkCompleted = engine.onFrame(step, sample)
        if (step == QuickCalibrationStep.BlinkThreeTimes && blinkCompleted) {
            state = state.copy(blinksCollected = engine.blinkCount())
            publish()
            if (engine.blinksReady()) {
                dispatch(StartupEvent.AdvanceCalibrationStep)
            }
            return
        }

        if (step == QuickCalibrationStep.LookNaturally) {
            if (lookNaturallyStartedMs == 0L) lookNaturallyStartedMs = nowMs()
            val elapsed = nowMs() - lookNaturallyStartedMs
            if (engine.baselineReady() && elapsed >= LOOK_NATURALLY_MIN_MS) {
                lookNaturallyStartedMs = 0L
                dispatch(StartupEvent.AdvanceCalibrationStep)
            }
        }
    }

    /**
     * Both-eye blink accepted by the production blink detector during calibration.
     */
    fun onBothBlinkAccepted(closePeak: Float, durationMs: Long) {
        if (state.phase != StartupPhase.QuickCalibration) return
        if (state.calibrationStep != QuickCalibrationStep.BlinkThreeTimes) return
        val count = engine.onBothBlinkAccepted(state.calibrationStep, closePeak, durationMs)
        state = state.copy(blinksCollected = count)
        publish()
        if (engine.blinksReady()) {
            dispatch(StartupEvent.AdvanceCalibrationStep)
        }
    }

    fun onLeftWinkAccepted(closePeak: Float) {
        if (state.phase != StartupPhase.QuickCalibration) return
        if (state.calibrationStep != QuickCalibrationStep.LeftWinkTwice) return
        val count = engine.onLeftWinkAccepted(state.calibrationStep, closePeak)
        state = state.copy(leftWinksCollected = count)
        publish()
        if (engine.leftWinksReady()) {
            dispatch(StartupEvent.AdvanceCalibrationStep)
        }
    }

    fun onRightWinkAccepted(closePeak: Float) {
        if (state.phase != StartupPhase.QuickCalibration) return
        if (state.calibrationStep != QuickCalibrationStep.RightWinkTwice) return
        val count = engine.onRightWinkAccepted(state.calibrationStep, closePeak)
        state = state.copy(rightWinksCollected = count)
        publish()
        if (engine.rightWinksReady()) {
            finalizeCalibrationSuccess()
        }
    }

    fun notifyCalibrationTimeoutFailure() {
        if (state.phase != StartupPhase.QuickCalibration) return
        if (state.calibrationStep == QuickCalibrationStep.CalibrationComplete) return
        dispatch(StartupEvent.CalibrationFailed)
        scheduleAutoRetry(1600L) {
            if (state.phase == StartupPhase.CalibrationFailure && state.faceDetected) {
                engine.reset()
                lookNaturallyStartedMs = 0L
                dispatch(StartupEvent.RetryCalibration)
            }
        }
    }

    private fun evaluateConfidenceAndContinue() {
        val level = EyeCalibrationAuthority.confidenceLevel(loadProfileCalibration(), nowMs())
        dispatch(StartupEvent.ConfidenceEvaluated(level))
        if (level == CalibrationConfidenceLevel.High) {
            val existing = loadProfileCalibration()
            if (existing != null) {
                dispatch(StartupEvent.CalibrationCaptured(existing))
            }
            activateEyeControlAndScheduleWelcome()
        } else {
            engine.reset()
            lookNaturallyStartedMs = 0L
        }
    }

    private fun finalizeCalibrationSuccess() {
        val calibration = engine.buildCalibration(nowMs())
        if (calibration == null || !EyeCalibrationAuthority.thresholdsLookValid(calibration)) {
            notifyCalibrationTimeoutFailure()
            return
        }
        persistCalibration(calibration)
        dispatch(StartupEvent.CalibrationCaptured(calibration))
        dispatch(StartupEvent.AdvanceCalibrationStep) // → CalibrationComplete UI step
        scheduleReadyHandoff(SUCCESS_DISPLAY_MS) {
            if (state.phase == StartupPhase.QuickCalibration &&
                state.calibrationStep == QuickCalibrationStep.CalibrationComplete
            ) {
                dispatch(StartupEvent.CalibrationSucceeded)
                activateEyeControlAndScheduleWelcome()
            }
        }
    }

    private fun activateEyeControlAndScheduleWelcome() {
        if (state.eyeControlActive) {
            onEyeControlActivated()
        }
        scheduleReadyHandoff(READY_HANDOFF_MS) {
            if (state.phase == StartupPhase.EyeTrackingReady) {
                dispatch(StartupEvent.AcknowledgeEyeTrackingReady)
                onStartupComplete()
            }
        }
    }

    private fun dispatch(event: StartupEvent) {
        val previousEye = state.eyeControlActive
        state = StartupFlowAuthority.reduce(state, event)
        if (!previousEye && state.eyeControlActive) {
            onEyeControlActivated()
        }
        publish()
    }

    private fun publish() = onStateChanged(state)

    companion object {
        const val LOOK_NATURALLY_MIN_MS = 1500L
        const val SUCCESS_DISPLAY_MS = 1200L
        const val READY_HANDOFF_MS = 900L
    }
}
