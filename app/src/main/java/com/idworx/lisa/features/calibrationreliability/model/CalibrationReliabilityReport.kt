package com.idworx.lisa.features.calibrationreliability.model

data class CalibrationReliabilityReport(
    val sessionId: String?,
    val timestampMs: Long = System.currentTimeMillis(),
    val healthState: CalibrationHealthState,
    val lastScore: CalibrationScore?,
    val lastResult: CalibrationResult?,
    val driftDetected: Boolean,
    val driftReason: String?,
    val recommendations: List<CalibrationRecommendation>,
    val allowsCommunication: Boolean,
    val metrics: List<CalibrationMetric>,
    val evidenceSummary: String
)
