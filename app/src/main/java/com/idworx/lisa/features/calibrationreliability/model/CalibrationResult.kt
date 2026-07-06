package com.idworx.lisa.features.calibrationreliability.model

data class CalibrationResult(
    val session: CalibrationSession,
    val score: CalibrationScore,
    val outcome: CalibrationReliabilityOutcome,
    val failureReasons: List<CalibrationFailureReason>,
    val recommendations: List<CalibrationRecommendation>,
    val evidenceSummary: String,
    val metrics: List<CalibrationMetric> = emptyList()
)
