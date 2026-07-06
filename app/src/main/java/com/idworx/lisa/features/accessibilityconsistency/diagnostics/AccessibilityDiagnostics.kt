package com.idworx.lisa.features.accessibilityconsistency.diagnostics

import com.idworx.lisa.features.accessibilityconsistency.model.AccessibilityReport

object AccessibilityLogger {

    private val log = mutableListOf<String>()

    fun log(message: String) {
        log.add("${System.currentTimeMillis()}: $message")
        while (log.size > 200) log.removeAt(0)
    }

    fun recent(limit: Int = 50): List<String> = log.takeLast(limit)

    fun clear() = log.clear()
}

object AccessibilityDiagnostics {

    private var lastReport: AccessibilityReport? = null

    fun record(report: AccessibilityReport) {
        lastReport = report
        AccessibilityLogger.log("Audit ${report.reportId}: score=${report.score.overall} issues=${report.issues.size}")
    }

    fun lastReport(): AccessibilityReport? = lastReport

    fun formatSummary(): String {
        val report = lastReport ?: return "No accessibility diagnostics available"
        return buildString {
            appendLine("Accessibility Diagnostics")
            appendLine("Score: ${report.score.overall} (${report.score.band.name})")
            appendLine("Checks: ${report.audit.checksPassed}/${report.audit.checksPerformed}")
            appendLine("Issues: ${report.issues.size} (${report.score.metrics.criticalIssues} critical/error)")
            appendLine("Typography passed: ${report.score.metrics.typographyChecksPassed}")
            appendLine("Touch targets passed: ${report.score.metrics.touchTargetChecksPassed}")
            appendLine("Navigation passed: ${report.score.metrics.navigationChecksPassed}")
            appendLine("Guided Learning issues: ${report.issues.count { it.category.name == "GuidedLearning" }}")
            appendLine("Communication issues: ${report.issues.count { it.category.name == "Communication" }}")
            appendLine("Emergency issues: ${report.issues.count { it.category.name == "Emergency" }}")
            appendLine("Warnings: ${report.warnings.size}")
            report.recommendations.take(3).forEach { appendLine("Recommendation: ${it.message}") }
        }
    }

    fun clear() {
        lastReport = null
        AccessibilityLogger.clear()
    }
}
