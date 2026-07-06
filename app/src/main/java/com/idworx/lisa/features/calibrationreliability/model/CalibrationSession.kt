package com.idworx.lisa.features.calibrationreliability.model

import java.util.UUID

data class CalibrationSession(
    val sessionId: String = UUID.randomUUID().toString(),
    val startTimeMs: Long = System.currentTimeMillis(),
    var endTimeMs: Long? = null,
    var state: CalibrationSessionState = CalibrationSessionState.InProgress,
    var pointsCompleted: Int = 0,
    var pointsSkipped: Int = 0,
    var successfulSamples: Int = 0,
    var rejectedSamples: Int = 0,
    var interruptions: Int = 0,
    var retries: Int = 0,
    var stabilityEvents: Int = 0,
    var gazeDeviationEvents: Int = 0,
    var incompleteFixations: Int = 0,
    var pauses: Int = 0,
    var trackingGaps: Int = 0,
    var totalPoints: Int = CalibrationReliabilityDefaults.DEFAULT_CALIBRATION_POINTS,
    val sensitivityLevel: Int? = null,
    val source: CalibrationSessionSource = CalibrationSessionSource.CalibrationUi
) {
    val durationMs: Long
        get() = (endTimeMs ?: System.currentTimeMillis()) - startTimeMs

    val totalSamples: Int
        get() = successfulSamples + rejectedSamples

    val completionRatio: Float
        get() = if (totalPoints > 0) pointsCompleted.toFloat() / totalPoints else 0f

    val sampleAcceptanceRatio: Float
        get() = if (totalSamples > 0) successfulSamples.toFloat() / totalSamples else 0f
}

enum class CalibrationSessionSource {
    CalibrationUi,
    SensitivityAdjustment,
    GuidedLearning,
    Recovery
}

object CalibrationReliabilityDefaults {
    const val DEFAULT_CALIBRATION_POINTS: Int = 5
    const val SAMPLES_PER_POINT: Int = 3
    const val MAX_CALIBRATION_AGE_MS: Long = 7L * 24 * 60 * 60 * 1000
    const val DRIFT_FAILURE_THRESHOLD: Int = 5
    const val DRIFT_RETRY_THRESHOLD: Int = 3
}
