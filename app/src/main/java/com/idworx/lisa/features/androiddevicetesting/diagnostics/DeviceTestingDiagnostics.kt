package com.idworx.lisa.features.androiddevicetesting.diagnostics

import com.idworx.lisa.features.androiddevicetesting.model.DeviceTestReport
import com.idworx.lisa.features.androiddevicetesting.reporting.DeviceTestSummary

object DeviceTestingLogger {

    private val log = mutableListOf<String>()

    fun log(message: String) {
        log.add("${System.currentTimeMillis()}: $message")
        while (log.size > 200) log.removeAt(0)
    }

    fun recent(limit: Int = 50): List<String> = log.takeLast(limit)

    fun clear() = log.clear()
}

object DeviceTestingDiagnostics {

    private var lastReport: DeviceTestReport? = null

    fun record(report: DeviceTestReport) {
        lastReport = report
        DeviceTestingLogger.log(
            "Device test ${report.reportId}: ${report.readinessOutcome.name} steps=${report.stepResults.size}"
        )
    }

    fun lastReport(): DeviceTestReport? = lastReport

    fun formatSummary(): String {
        val report = lastReport ?: return "No Android device testing diagnostics available"
        return buildString {
            appendLine("Android Device Testing Diagnostics")
            appendLine("Readiness: ${report.readinessOutcome.name}")
            appendLine("Overall: ${report.overallOutcome.name}")
            appendLine("Recorded steps: ${report.stepResults.count { it.outcome.name != "NOT_TESTED" }}")
            appendLine("Failures: ${report.failures.size}")
            appendLine()
            append(DeviceTestSummary.format(report))
        }
    }

    fun clear() {
        lastReport = null
        DeviceTestingLogger.clear()
    }
}
