package com.idworx.lisa.features.intelligentstartup.model

/**
 * RC7D.34 — Intelligent Startup Flow phases.
 * Splash remains OS-owned; this machine begins immediately after process content is ready.
 */
enum class StartupPhase {
    FaceDetection,
    EvaluatingConfidence,
    QuickCalibration,
    CalibrationFailure,
    EyeTrackingReady,
    Complete
}

enum class QuickCalibrationStep {
    LookNaturally,
    BlinkThreeTimes,
    LeftWinkTwice,
    RightWinkTwice,
    CalibrationComplete;

    fun next(): QuickCalibrationStep? = when (this) {
        LookNaturally -> BlinkThreeTimes
        BlinkThreeTimes -> LeftWinkTwice
        LeftWinkTwice -> RightWinkTwice
        RightWinkTwice -> CalibrationComplete
        CalibrationComplete -> null
    }
}

enum class CalibrationConfidenceLevel {
    Missing,
    Low,
    High
}

/**
 * Per-profile eye calibration payload. Stored locally on [com.idworx.lisa.LisaUserProfile].
 */
data class ProfileEyeCalibration(
    val leftClosedEyeThreshold: Float,
    val rightClosedEyeThreshold: Float,
    val openEyeThreshold: Float,
    val blinkDurationMs: Long,
    val requiredWinkFrames: Int,
    val eyeOpennessBaseline: Float,
    val faceDistanceProxy: Float,
    val confidence: Float,
    val calibratedAtMs: Long
) {
    companion object {
        val EMPTY: ProfileEyeCalibration? = null
    }
}

data class StartupFlowState(
    val phase: StartupPhase = StartupPhase.FaceDetection,
    val faceDetected: Boolean = false,
    val lookingForFaceMessage: Boolean = true,
    val calibrationStep: QuickCalibrationStep = QuickCalibrationStep.LookNaturally,
    val blinksCollected: Int = 0,
    val leftWinksCollected: Int = 0,
    val rightWinksCollected: Int = 0,
    val failureCount: Int = 0,
    val eyeControlActive: Boolean = false,
    val calibration: ProfileEyeCalibration? = null,
    val skippedCalibration: Boolean = false,
    val isActive: Boolean = true
) {
    val blocksMainUi: Boolean
        get() = isActive && phase != StartupPhase.Complete
}

sealed class StartupEvent {
    data class FacePresenceChanged(val present: Boolean) : StartupEvent()
    data object BeginConfidenceEvaluation : StartupEvent()
    data class ConfidenceEvaluated(val level: CalibrationConfidenceLevel) : StartupEvent()
    data object AdvanceCalibrationStep : StartupEvent()
    data object CalibrationSucceeded : StartupEvent()
    data object CalibrationFailed : StartupEvent()
    data object RetryCalibration : StartupEvent()
    data object AcknowledgeEyeTrackingReady : StartupEvent()
    data class CalibrationCaptured(val calibration: ProfileEyeCalibration) : StartupEvent()
}
