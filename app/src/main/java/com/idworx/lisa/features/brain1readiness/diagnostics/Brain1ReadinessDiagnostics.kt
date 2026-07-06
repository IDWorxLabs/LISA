package com.idworx.lisa.features.brain1readiness.diagnostics

import com.idworx.lisa.features.brain1readiness.model.Brain1ReadinessReport
import com.idworx.lisa.features.brain1readiness.reporting.Brain1ReadinessSummary

object Brain1ReadinessLogger {

    private val log = mutableListOf<String>()

    fun log(message: String) {
        log.add("${System.currentTimeMillis()}: $message")
        while (log.size > 200) log.removeAt(0)
    }

    fun recent(limit: Int = 50): List<String> = log.takeLast(limit)

    fun clear() = log.clear()
}

object Brain1ReadinessDiagnostics {

    private var lastReport: Brain1ReadinessReport? = null

    fun record(report: Brain1ReadinessReport) {
        lastReport = report
        Brain1ReadinessLogger.log(
            "Brain1 review ${report.reportId}: outcome=${report.outcome.name} score=${report.score.overall}"
        )
    }

    fun lastReport(): Brain1ReadinessReport? = lastReport

    fun formatSummary(): String {
        val report = lastReport ?: return "No Brain 1 readiness diagnostics available"
        return buildString {
            appendLine("Brain 1 Readiness Diagnostics")
            appendLine("Outcome: ${report.outcome.name}")
            appendLine("Score: ${report.score.overall} (${report.score.band.name})")
            appendLine("Subsystems ready: ${report.score.subsystemsReady}/${report.score.subsystemsTotal}")
            appendLine("Risks: ${report.risks.size}")
            appendLine("Gaps: ${report.gaps.size}")
            appendLine("Recommendations: ${report.recommendations.size}")
            appendLine()
            append(Brain1ReadinessSummary.format(report))
        }
    }

    fun clear() {
        lastReport = null
        Brain1ReadinessLogger.clear()
    }
}
