package com.idworx.lisa.features.intelligentstartup.authority

import com.idworx.lisa.features.blinkdetectionreliability.BlinkDetectionTuning
import com.idworx.lisa.features.intelligentstartup.model.CalibrationConfidenceLevel
import com.idworx.lisa.features.intelligentstartup.model.ProfileEyeCalibration
import com.idworx.lisa.features.intelligentstartup.model.QuickCalibrationStep
import com.idworx.lisa.features.intelligentstartup.model.StartupEvent
import com.idworx.lisa.features.intelligentstartup.model.StartupFlowState
import com.idworx.lisa.features.intelligentstartup.model.StartupPhase

/**
 * Pure production authority for eye-calibration thresholds and confidence.
 * No Compose / navigation side effects.
 */
object EyeCalibrationAuthority {

    const val HighConfidenceMinimum = 0.75f
    const val MaxCalibrationAgeMs: Long = 7L * 24L * 60L * 60L * 1000L

    fun confidenceLevel(
        calibration: ProfileEyeCalibration?,
        nowMs: Long
    ): CalibrationConfidenceLevel {
        if (calibration == null) return CalibrationConfidenceLevel.Missing
        if (!thresholdsLookValid(calibration)) return CalibrationConfidenceLevel.Low
        if (nowMs - calibration.calibratedAtMs > MaxCalibrationAgeMs) {
            return CalibrationConfidenceLevel.Low
        }
        return if (calibration.confidence >= HighConfidenceMinimum) {
            CalibrationConfidenceLevel.High
        } else {
            CalibrationConfidenceLevel.Low
        }
    }

    fun shouldSkipQuickCalibration(
        calibration: ProfileEyeCalibration?,
        nowMs: Long
    ): Boolean = confidenceLevel(calibration, nowMs) == CalibrationConfidenceLevel.High

    fun toBlinkTuning(
        calibration: ProfileEyeCalibration,
        base: BlinkDetectionTuning = BlinkDetectionTuning.default
    ): BlinkDetectionTuning = base.copy(
        closedEyeThreshold = (calibration.leftClosedEyeThreshold + calibration.rightClosedEyeThreshold) / 2f,
        openEyeThreshold = calibration.openEyeThreshold,
        requiredWinkFrames = calibration.requiredWinkFrames.coerceAtLeast(1),
        leftClosedEyeThreshold = calibration.leftClosedEyeThreshold,
        rightClosedEyeThreshold = calibration.rightClosedEyeThreshold
    )

    fun thresholdsLookValid(calibration: ProfileEyeCalibration): Boolean {
        val closedOk = calibration.leftClosedEyeThreshold in 0.05f..0.60f &&
            calibration.rightClosedEyeThreshold in 0.05f..0.60f
        val openOk = calibration.openEyeThreshold in 0.45f..0.95f
        val orderingOk = calibration.leftClosedEyeThreshold < calibration.openEyeThreshold &&
            calibration.rightClosedEyeThreshold < calibration.openEyeThreshold
        val durationOk = calibration.blinkDurationMs in 40L..900L
        val framesOk = calibration.requiredWinkFrames in 1..6
        return closedOk && openOk && orderingOk && durationOk && framesOk
    }
}

/**
 * Deterministic startup state reducer. Hosts dispatch [StartupEvent]s; UI never jumps phases.
 */
object StartupFlowAuthority {

    fun reduce(state: StartupFlowState, event: StartupEvent): StartupFlowState = when (event) {
        is StartupEvent.FacePresenceChanged -> when (state.phase) {
            StartupPhase.FaceDetection -> state.copy(
                faceDetected = event.present,
                lookingForFaceMessage = !event.present
            )
            StartupPhase.QuickCalibration,
            StartupPhase.CalibrationFailure -> state.copy(faceDetected = event.present)
            else -> state.copy(faceDetected = event.present)
        }

        StartupEvent.BeginConfidenceEvaluation -> {
            if (state.phase != StartupPhase.FaceDetection || !state.faceDetected) state
            else state.copy(phase = StartupPhase.EvaluatingConfidence)
        }

        is StartupEvent.ConfidenceEvaluated -> {
            if (state.phase != StartupPhase.EvaluatingConfidence) state
            else when (event.level) {
                CalibrationConfidenceLevel.High -> state.copy(
                    phase = StartupPhase.EyeTrackingReady,
                    skippedCalibration = true,
                    eyeControlActive = true
                )
                CalibrationConfidenceLevel.Missing,
                CalibrationConfidenceLevel.Low -> state.copy(
                    phase = StartupPhase.QuickCalibration,
                    calibrationStep = QuickCalibrationStep.LookNaturally,
                    blinksCollected = 0,
                    leftWinksCollected = 0,
                    rightWinksCollected = 0,
                    skippedCalibration = false
                )
            }
        }

        StartupEvent.AdvanceCalibrationStep -> {
            if (state.phase != StartupPhase.QuickCalibration) {
                state
            } else {
                val next = state.calibrationStep.next()
                if (next == null) {
                    state
                } else {
                    state.copy(
                        calibrationStep = next,
                        blinksCollected = 0,
                        leftWinksCollected = 0,
                        rightWinksCollected = 0
                    )
                }
            }
        }

        is StartupEvent.CalibrationCaptured -> state.copy(calibration = event.calibration)

        StartupEvent.CalibrationSucceeded -> {
            if (state.phase != StartupPhase.QuickCalibration) state
            else if (state.calibrationStep != QuickCalibrationStep.CalibrationComplete) state
            else state.copy(
                phase = StartupPhase.EyeTrackingReady,
                eyeControlActive = true,
                failureCount = 0
            )
        }

        StartupEvent.CalibrationFailed -> {
            if (state.phase != StartupPhase.QuickCalibration) state
            else state.copy(
                phase = StartupPhase.CalibrationFailure,
                failureCount = state.failureCount + 1,
                eyeControlActive = false
            )
        }

        StartupEvent.RetryCalibration -> {
            if (state.phase != StartupPhase.CalibrationFailure) state
            else state.copy(
                phase = StartupPhase.QuickCalibration,
                calibrationStep = QuickCalibrationStep.LookNaturally,
                blinksCollected = 0,
                leftWinksCollected = 0,
                rightWinksCollected = 0
            )
        }

        StartupEvent.AcknowledgeEyeTrackingReady -> {
            if (state.phase != StartupPhase.EyeTrackingReady) state
            else state.copy(
                phase = StartupPhase.Complete,
                isActive = false,
                eyeControlActive = true
            )
        }
    }
}
