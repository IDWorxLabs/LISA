package com.idworx.lisa.features.calibrationreliability.scoring

import com.idworx.lisa.features.calibrationreliability.model.CalibrationReliabilityDefaults
import com.idworx.lisa.features.calibrationreliability.model.CalibrationResult
import com.idworx.lisa.features.calibrationreliability.model.CalibrationScore

data class DriftDetectionResult(
    val driftDetected: Boolean,
    val reason: String?,
    val recentCommunicationFailures: Int,
    val recentLowConfidenceAttempts: Int,
    val calibrationAgeMs: Long,
    val retryFrequency: Int
)

object DriftDetector {

    fun detect(
        lastResult: CalibrationResult?,
        lastCompletedMs: Long?,
        recentCommunicationFailures: Int,
        recentLowConfidenceAttempts: Int,
        retryCountSinceLastCalibration: Int,
        nowMs: Long = System.currentTimeMillis()
    ): DriftDetectionResult {
        val ageMs = if (lastCompletedMs != null) nowMs - lastCompletedMs else Long.MAX_VALUE
        val score = lastResult?.score

        val ageDrift = ageMs > CalibrationReliabilityDefaults.MAX_CALIBRATION_AGE_MS
        val failureDrift = recentCommunicationFailures >= CalibrationReliabilityDefaults.DRIFT_FAILURE_THRESHOLD
        val retryDrift = retryCountSinceLastCalibration >= CalibrationReliabilityDefaults.DRIFT_RETRY_THRESHOLD
        val confidenceDrift = recentLowConfidenceAttempts >= CalibrationReliabilityDefaults.DRIFT_FAILURE_THRESHOLD
        val scoreDrift = score != null && score.overall < 60

        val reasons = buildList {
            if (ageDrift) add("Calibration age exceeds safe window")
            if (failureDrift) add("Repeated communication failures after calibration")
            if (retryDrift) add("Excessive calibration retry frequency")
            if (confidenceDrift) add("Repeated low-confidence communication")
            if (scoreDrift) add("Last calibration score below acceptable threshold")
        }

        return DriftDetectionResult(
            driftDetected = reasons.isNotEmpty(),
            reason = reasons.joinToString("; ").ifBlank { null },
            recentCommunicationFailures = recentCommunicationFailures,
            recentLowConfidenceAttempts = recentLowConfidenceAttempts,
            calibrationAgeMs = if (ageMs == Long.MAX_VALUE) -1L else ageMs,
            retryFrequency = retryCountSinceLastCalibration
        )
    }

    fun shouldRecommendRecalibration(drift: DriftDetectionResult, score: CalibrationScore?): Boolean =
        drift.driftDetected || (score != null && score.overall < 75)
}
