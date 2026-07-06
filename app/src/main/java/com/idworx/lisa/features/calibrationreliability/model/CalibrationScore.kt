package com.idworx.lisa.features.calibrationreliability.model

enum class CalibrationQualityCategory {
    Excellent,
    Good,
    Acceptable,
    Poor,
    Failed;

    companion object {
        fun fromScore(score: Int): CalibrationQualityCategory = when {
            score >= 90 -> Excellent
            score >= 75 -> Good
            score >= 60 -> Acceptable
            score >= 40 -> Poor
            else -> Failed
        }
    }
}

data class StabilityScore(
    val value: Int,
    val stabilityEvents: Int,
    val gazeDeviationEvents: Int,
    val incompleteFixations: Int,
    val pauses: Int
)

data class RepeatabilityScore(
    val value: Int,
    val priorSessionScore: Int?,
    val variance: Int,
    val improved: Boolean,
    val regressed: Boolean
)

data class CalibrationScore(
    val overall: Int,
    val category: CalibrationQualityCategory,
    val sampleCompleteness: Int,
    val eyeStability: Int,
    val sampleConsistency: Int,
    val repeatability: Int,
    val coverage: Int,
    val trackingContinuity: Int,
    val stabilityScore: StabilityScore,
    val repeatabilityScore: RepeatabilityScore
)
