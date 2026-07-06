package com.idworx.lisa.features.communicationanalytics.diagnostics

import com.idworx.lisa.features.communicationanalytics.model.CommunicationAnalyticsReport
import com.idworx.lisa.features.communicationanalytics.model.CommunicationAttemptAnalytics

object AnalyticsLogger {

    private val log = mutableListOf<String>()

    fun log(message: String) {
        log.add("${System.currentTimeMillis()}: $message")
        while (log.size > 300) log.removeAt(0)
    }

    fun recent(limit: Int = 50): List<String> = log.takeLast(limit)

    fun clear() = log.clear()
}

object AnalyticsDiagnostics {

    private var lastReport: CommunicationAnalyticsReport? = null
    private var lastAttempts: List<CommunicationAttemptAnalytics> = emptyList()

    fun record(report: CommunicationAnalyticsReport, attempts: List<CommunicationAttemptAnalytics>) {
        lastReport = report
        lastAttempts = attempts
        AnalyticsLogger.log("Report ${report.reportId}: ${attempts.size} attempts, success=${report.summary.overallSuccessRate.rate}")
    }

    fun lastReport(): CommunicationAnalyticsReport? = lastReport

    fun lastAttempts(): List<CommunicationAttemptAnalytics> = lastAttempts

    fun formatSummary(): String {
        val report = lastReport ?: return "No analytics diagnostics available"
        return buildString {
            appendLine("Communication Analytics Diagnostics")
            appendLine("Success rate: ${"%.1f".format(report.summary.overallSuccessRate.rate * 100)}%")
            appendLine("Retry rate: ${report.summary.averageRetries}")
            appendLine("False positives: ${"%.1f".format(report.summary.falsePositiveRate * 100)}%")
            appendLine("False negatives: ${"%.1f".format(report.summary.falseNegativeRate * 100)}%")
            appendLine("Avg completion: ${report.summary.averageCompletionTimeMs ?: "--"} ms")
            appendLine("Phrases tracked: ${report.phraseAnalytics.size}")
            appendLine("Emergency attempts: ${report.emergencyAnalytics.activationAttempts}")
            appendLine("Navigation attempts: ${report.navigationAnalytics.gestureAttempts}")
            appendLine("Trends: ${report.trends.joinToString { "${it.metricName}=${it.direction}" }}")
            report.phraseAnalytics.take(3).forEach {
                appendLine("Phrase ${it.phraseId}: ${"%.0f".format(it.successRate * 100)}% success")
            }
        }
    }

    fun clear() {
        lastReport = null
        lastAttempts = emptyList()
        AnalyticsLogger.clear()
    }
}
