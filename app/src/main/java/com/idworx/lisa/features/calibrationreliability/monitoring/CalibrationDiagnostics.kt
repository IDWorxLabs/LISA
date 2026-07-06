package com.idworx.lisa.features.calibrationreliability.monitoring

import com.idworx.lisa.features.calibrationreliability.model.CalibrationHealthState
import com.idworx.lisa.features.calibrationreliability.model.CalibrationRecommendation
import com.idworx.lisa.features.calibrationreliability.model.CalibrationReliabilityReport
import com.idworx.lisa.features.calibrationreliability.model.CalibrationResult
import com.idworx.lisa.features.calibrationreliability.model.CalibrationScore
import com.idworx.lisa.features.calibrationreliability.scoring.DriftDetectionResult

object CalibrationDiagnostics {

    private var lastReport: CalibrationReliabilityReport? = null
    private var lastResult: CalibrationResult? = null
    private var lastDrift: DriftDetectionResult? = null

    fun record(report: CalibrationReliabilityReport, result: CalibrationResult?, drift: DriftDetectionResult) {
        lastReport = report
        lastResult = result
        lastDrift = drift
    }

    fun lastReport(): CalibrationReliabilityReport? = lastReport

    fun lastResult(): CalibrationResult? = lastResult

    fun lastDrift(): DriftDetectionResult? = lastDrift

    fun formatSummary(): String {
        val report = lastReport ?: return "No calibration diagnostics available"
        val score = report.lastScore
        return buildString {
            appendLine("Calibration Diagnostics")
            appendLine("Health: ${report.healthState}")
            appendLine("Overall score: ${score?.overall ?: "--"}")
            appendLine("Stability: ${score?.eyeStability ?: "--"}")
            appendLine("Repeatability: ${score?.repeatability ?: "--"}")
            appendLine("Drift: ${if (report.driftDetected) report.driftReason else "none"}")
            appendLine("Recommendation: ${report.recommendations.firstOrNull() ?: CalibrationRecommendation.None}")
            appendLine("Last duration ms: ${report.lastResult?.session?.durationMs ?: "--"}")
            appendLine("Rejected samples: ${report.lastResult?.session?.rejectedSamples ?: 0}")
        }
    }

    fun currentScore(): Int? = lastReport?.lastScore?.overall

    fun currentHealth(): CalibrationHealthState? = lastReport?.healthState

    fun stabilityScore(): Int? = lastReport?.lastScore?.eyeStability

    fun repeatabilityScore(): Int? = lastReport?.lastScore?.repeatability

    fun clear() {
        lastReport = null
        lastResult = null
        lastDrift = null
    }
}
