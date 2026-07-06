package com.idworx.lisa.features.blinkdetectionreliability

/** Lightweight runtime diagnostics for developer mode during Guided Learning. */
data class BlinkDetectionDiagnostics(
    val cameraActive: Boolean = false,
    val eyesDetected: Boolean = false,
    val leftEyeSignal: String = "--",
    val rightEyeSignal: String = "--",
    val leftCandidate: Boolean = false,
    val rightCandidate: Boolean = false,
    val leftStreak: Int = 0,
    val rightStreak: Int = 0,
    val acceptedLeftCount: Int = 0,
    val acceptedRightCount: Int = 0,
    val skippedForJitter: Boolean = false
)
