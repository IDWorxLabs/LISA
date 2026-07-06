package com.idworx.lisa.features.calibrationreliability.monitoring

import com.idworx.lisa.features.calibrationreliability.metadata.CalibrationReliabilityMetadata
import com.idworx.lisa.features.calibrationreliability.model.CalibrationHealthState
import com.idworx.lisa.features.calibrationreliability.model.CalibrationQualityCategory
import com.idworx.lisa.features.calibrationreliability.model.CalibrationReliabilityDefaults
import com.idworx.lisa.features.calibrationreliability.model.CalibrationResult
import com.idworx.lisa.features.calibrationreliability.scoring.DriftDetectionResult
import com.idworx.lisa.features.calibrationreliability.scoring.DriftDetector

object CalibrationHealthMonitor {

    fun evaluate(
        lastResult: CalibrationResult?,
        lastCompletedMs: Long?,
        drift: DriftDetectionResult,
        hasLegacySensitivity: Boolean,
        nowMs: Long = System.currentTimeMillis()
    ): CalibrationHealthState {
        if (lastResult == null) {
            return if (hasLegacySensitivity) CalibrationHealthState.Monitor else CalibrationHealthState.CalibrationRequired
        }

        val ageMs = if (lastCompletedMs != null) nowMs - lastCompletedMs else Long.MAX_VALUE
        val score = lastResult.score.overall

        if (lastResult.score.category == CalibrationQualityCategory.Failed) {
            return CalibrationHealthState.CalibrationInvalid
        }

        if (drift.driftDetected || score < CalibrationReliabilityMetadata.MIN_COMMUNICATION_SCORE) {
            return CalibrationHealthState.CalibrationInvalid
        }

        if (DriftDetector.shouldRecommendRecalibration(drift, lastResult.score)) {
            return CalibrationHealthState.RecommendRecalibration
        }

        if (ageMs > CalibrationReliabilityDefaults.MAX_CALIBRATION_AGE_MS) {
            return CalibrationHealthState.RecommendRecalibration
        }

        if (score < CalibrationReliabilityMetadata.MONITOR_SCORE_THRESHOLD ||
            lastResult.score.category == CalibrationQualityCategory.Poor
        ) {
            return CalibrationHealthState.Monitor
        }

        return CalibrationHealthState.Healthy
    }

    fun allowsCommunication(state: CalibrationHealthState): Boolean = when (state) {
        CalibrationHealthState.Healthy,
        CalibrationHealthState.Monitor,
        CalibrationHealthState.RecommendRecalibration -> true
        CalibrationHealthState.CalibrationRequired,
        CalibrationHealthState.CalibrationInvalid -> false
    }

    fun shouldPauseGuidedLearning(state: CalibrationHealthState): Boolean = when (state) {
        CalibrationHealthState.CalibrationRequired,
        CalibrationHealthState.CalibrationInvalid -> true
        else -> false
    }
}

object CalibrationLifecycleTracker {

    private val sessionStarts = mutableListOf<Long>()
    private val sessionCompletions = mutableListOf<Long>()
    private var totalRetries = 0

    fun recordSessionStart() {
        sessionStarts.add(System.currentTimeMillis())
    }

    fun recordSessionComplete() {
        sessionCompletions.add(System.currentTimeMillis())
    }

    fun recordRetry() {
        totalRetries++
    }

    fun retryCountSince(lastCompletedMs: Long?): Int {
        if (lastCompletedMs == null) return totalRetries
        return totalRetries
    }

    fun reset() {
        sessionStarts.clear()
        sessionCompletions.clear()
        totalRetries = 0
    }
}
