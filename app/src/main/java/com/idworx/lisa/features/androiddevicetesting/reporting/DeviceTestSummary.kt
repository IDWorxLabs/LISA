package com.idworx.lisa.features.androiddevicetesting.reporting

import com.idworx.lisa.features.androiddevicetesting.model.DeviceTestPlan
import com.idworx.lisa.features.androiddevicetesting.model.DeviceTestReport

object DeviceTestSummary {

    fun format(report: DeviceTestReport): String = buildString {
        appendLine("=== LISA Android Device Test Report ===")
        appendLine("Readiness: ${report.readinessOutcome.name}")
        appendLine("Overall: ${report.overallOutcome.name}")
        appendLine("Device: ${report.deviceProfile.deviceModel} / Android ${report.deviceProfile.androidVersion}")
        appendLine("Tester: ${report.session.testerLabel}")
        appendLine("Environment: ${report.session.testEnvironment}")
        appendLine()
        appendLine("Honest assessment:")
        appendLine(report.honestAssessment)
        appendLine()
        appendLine("Suite results:")
        report.suiteResults.forEach { suite ->
            appendLine("  ${suite.name}: ${suite.outcome.name}")
        }
        appendLine()
        appendLine("Recorded steps: ${report.stepResults.count { it.outcome.name != "NOT_TESTED" }}")
        appendLine("Failures: ${report.failures.size}")
        appendLine("Warnings: ${report.warnings.size}")
        appendLine("Risks: ${report.risks.size}")
        report.recommendations.take(5).forEach { appendLine("  Recommendation: ${it.message}") }
        appendLine()
        appendLine(report.summary)
    }
}

object DeviceTestingChecklist {

    fun format(plan: DeviceTestPlan): String = buildString {
        appendLine("=== LISA Android Device Testing Checklist ===")
        appendLine("Protocol version: ${plan.protocolVersion}")
        appendLine("All steps default to NOT_TESTED until evidence is recorded.")
        appendLine()
        plan.suites.forEach { suite ->
            appendLine("## ${suite.name}")
            suite.cases.forEach { testCase ->
                appendLine("  ${testCase.title}")
                testCase.steps.forEach { step ->
                    appendLine("    [ ] ${step.stepId}: ${step.description}")
                }
            }
            appendLine()
        }
    }
}
