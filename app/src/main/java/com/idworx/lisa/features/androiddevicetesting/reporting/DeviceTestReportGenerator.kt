package com.idworx.lisa.features.androiddevicetesting.reporting

import com.idworx.lisa.features.androiddevicetesting.model.DeviceTestEvidence
import com.idworx.lisa.features.androiddevicetesting.model.DeviceTestOutcome
import com.idworx.lisa.features.androiddevicetesting.model.DeviceTestRecommendation
import com.idworx.lisa.features.androiddevicetesting.model.DeviceTestReport
import com.idworx.lisa.features.androiddevicetesting.model.DeviceTestResult
import com.idworx.lisa.features.androiddevicetesting.model.DeviceTestRisk
import com.idworx.lisa.features.androiddevicetesting.model.DeviceTestRiskSeverity
import com.idworx.lisa.features.androiddevicetesting.model.DeviceTestSession
import com.idworx.lisa.features.androiddevicetesting.model.DeviceTestSuite
import com.idworx.lisa.features.androiddevicetesting.model.DeviceTestingReadinessOutcome
import java.util.UUID

object DeviceTestReportGenerator {

    fun applyResultsToPlan(session: DeviceTestSession): List<DeviceTestSuite> {
        val resultMap = session.results.associateBy { "${it.suiteId}:${it.caseId}:${it.stepId}" }
        return session.plan.suites.map { suite ->
            val updatedCases = suite.cases.map { testCase ->
                val updatedSteps = testCase.steps.map { step ->
                    val key = "${suite.suiteId}:${testCase.caseId}:${step.stepId}"
                    val result = resultMap[key]
                    if (result != null) {
                        step.copy(
                            outcome = result.outcome,
                            evidenceNotes = result.evidence?.notes ?: "",
                            recordedAtMs = result.recordedAtMs
                        )
                    } else step
                }
                testCase.copy(
                    steps = updatedSteps,
                    outcome = aggregateOutcome(updatedSteps.map { it.outcome })
                )
            }
            suite.copy(cases = updatedCases, outcome = aggregateOutcome(updatedCases.map { it.outcome }))
        }
    }

    fun generate(session: DeviceTestSession): DeviceTestReport {
        val suiteResults = applyResultsToPlan(session)
        val testedCount = session.results.count { it.outcome != DeviceTestOutcome.NOT_TESTED }
        val passCount = session.results.count { it.outcome == DeviceTestOutcome.PASS || it.outcome == DeviceTestOutcome.PASS_WITH_WARNING }
        val failCount = session.results.count { it.outcome == DeviceTestOutcome.FAIL || it.outcome == DeviceTestOutcome.BLOCKED }
        val evidence = session.results.mapNotNull { it.evidence }
        val failures = session.results.filter { it.outcome == DeviceTestOutcome.FAIL || it.outcome == DeviceTestOutcome.BLOCKED }
            .map { "${it.suiteId}/${it.stepId}: ${it.evidence?.notes ?: "failed"}" }
        val warnings = session.results.filter { it.outcome == DeviceTestOutcome.PASS_WITH_WARNING }
            .map { "${it.suiteId}/${it.stepId}: ${it.evidence?.notes ?: "warning"}" }
        val risks = buildRisks(session, suiteResults)
        val recommendations = buildRecommendations(session, suiteResults, failures)
        val overallOutcome = aggregateOutcome(session.results.map { it.outcome })
        val readinessOutcome = determineReadinessOutcome(testedCount, passCount, failCount, session.results)
        val honestAssessment = buildHonestAssessment(readinessOutcome, testedCount, session.results.size)

        return DeviceTestReport(
            reportId = UUID.randomUUID().toString(),
            generatedAtMs = System.currentTimeMillis(),
            session = session,
            deviceProfile = session.deviceProfile,
            suiteResults = suiteResults,
            stepResults = session.results,
            evidence = evidence,
            risks = risks,
            failures = failures,
            warnings = warnings,
            recommendations = recommendations,
            overallOutcome = overallOutcome,
            readinessOutcome = readinessOutcome,
            summary = "Device testing: ${readinessOutcome.name}; $testedCount steps recorded; $passCount passed; $failCount failed/blocked",
            honestAssessment = honestAssessment
        )
    }

    private fun determineReadinessOutcome(
        testedCount: Int,
        passCount: Int,
        failCount: Int,
        results: List<DeviceTestResult>
    ): DeviceTestingReadinessOutcome {
        if (testedCount == 0) return DeviceTestingReadinessOutcome.DEVICE_TESTING_NOT_STARTED
        if (results.any { it.outcome == DeviceTestOutcome.BLOCKED }) {
            return DeviceTestingReadinessOutcome.BLOCKED_BY_CRITICAL_DEVICE_FAILURE
        }
        if (failCount > 0) return DeviceTestingReadinessOutcome.NOT_READY_FOR_USER_TESTING
        val totalSteps = results.size.coerceAtLeast(1)
        val coverage = testedCount.toFloat() / totalSteps
        return when {
            coverage >= 0.9f && passCount == testedCount ->
                DeviceTestingReadinessOutcome.READY_FOR_CONTROLLED_USER_TESTING_WITH_SUPERVISION
            coverage >= 0.5f ->
                DeviceTestingReadinessOutcome.READY_FOR_MORE_DEVICE_TESTING
            else -> DeviceTestingReadinessOutcome.DEVICE_TESTING_IN_PROGRESS
        }
    }

    private fun buildHonestAssessment(
        readiness: DeviceTestingReadinessOutcome,
        testedCount: Int,
        totalRecorded: Int
    ): String = when (readiness) {
        DeviceTestingReadinessOutcome.DEVICE_TESTING_NOT_STARTED ->
            "No real Android device evidence recorded. This is device testing readiness, not clinical validation."
        DeviceTestingReadinessOutcome.DEVICE_TESTING_IN_PROGRESS ->
            "Device testing in progress ($testedCount steps with evidence). Not ready for user testing."
        DeviceTestingReadinessOutcome.READY_FOR_MORE_DEVICE_TESTING ->
            "Partial device evidence captured ($testedCount/$totalRecorded). Continue testing on additional devices and conditions."
        DeviceTestingReadinessOutcome.READY_FOR_CONTROLLED_USER_TESTING_WITH_SUPERVISION ->
            "Strong device evidence recorded. Ready for controlled supervised testing only — not patient-ready or clinically validated."
        DeviceTestingReadinessOutcome.NOT_READY_FOR_USER_TESTING ->
            "Device failures recorded. Address failures before any user testing."
        DeviceTestingReadinessOutcome.BLOCKED_BY_CRITICAL_DEVICE_FAILURE ->
            "Critical device failure blocked further testing progression."
    }

    private fun buildRisks(session: DeviceTestSession, suites: List<DeviceTestSuite>): List<DeviceTestRisk> {
        val risks = mutableListOf<DeviceTestRisk>()
        if (session.results.none { it.outcome != DeviceTestOutcome.NOT_TESTED }) {
            risks.add(
                DeviceTestRisk(
                    "RISK_NO_EVIDENCE", "ALL", DeviceTestRiskSeverity.High,
                    "No real device test evidence recorded yet", "Protocol default state"
                )
            )
        }
        suites.filter { it.outcome == DeviceTestOutcome.FAIL }.forEach { suite ->
            risks.add(
                DeviceTestRisk(
                    "RISK_${suite.suiteId}", suite.suiteId, DeviceTestRiskSeverity.High,
                    "Suite ${suite.name} failed on device", "Recorded device evidence"
                )
            )
        }
        if (session.lightingCondition.isBlank()) {
            risks.add(
                DeviceTestRisk(
                    "RISK_LIGHTING", "SUITE_LIGHTING", DeviceTestRiskSeverity.Medium,
                    "Lighting condition not documented for session", session.sessionId
                )
            )
        }
        return risks.distinctBy { it.riskId }
    }

    private fun buildRecommendations(
        session: DeviceTestSession,
        suites: List<DeviceTestSuite>,
        failures: List<String>
    ): List<DeviceTestRecommendation> {
        val recs = mutableListOf<DeviceTestRecommendation>()
        if (session.results.none { it.outcome != DeviceTestOutcome.NOT_TESTED }) {
            recs.add(
                DeviceTestRecommendation(
                    "REC_START", "Begin real Android device testing using the protocol checklist", "High"
                )
            )
        }
        suites.filter { it.outcome == DeviceTestOutcome.NOT_TESTED }.take(3).forEach { suite ->
            recs.add(
                DeviceTestRecommendation(
                    "REC_${suite.suiteId}", "Execute ${suite.name} suite on real hardware", "Medium"
                )
            )
        }
        failures.take(3).forEachIndexed { i, failure ->
            recs.add(DeviceTestRecommendation("REC_FAIL_$i", "Investigate failure: $failure", "Critical"))
        }
        recs.add(
            DeviceTestRecommendation(
                "REC_CLINICAL", "Do not claim patient or clinical readiness from device testing alone", "High"
            )
        )
        return recs.distinctBy { it.recommendationId }
    }

    fun aggregateOutcome(outcomes: List<DeviceTestOutcome>): DeviceTestOutcome = when {
        outcomes.isEmpty() || outcomes.all { it == DeviceTestOutcome.NOT_TESTED } -> DeviceTestOutcome.NOT_TESTED
        outcomes.any { it == DeviceTestOutcome.BLOCKED } -> DeviceTestOutcome.BLOCKED
        outcomes.any { it == DeviceTestOutcome.FAIL } -> DeviceTestOutcome.FAIL
        outcomes.any { it == DeviceTestOutcome.PASS_WITH_WARNING } -> DeviceTestOutcome.PASS_WITH_WARNING
        outcomes.all { it == DeviceTestOutcome.PASS || it == DeviceTestOutcome.PASS_WITH_WARNING } -> DeviceTestOutcome.PASS
        outcomes.any { it != DeviceTestOutcome.NOT_TESTED } -> DeviceTestOutcome.PASS_WITH_WARNING
        else -> DeviceTestOutcome.NOT_TESTED
    }
}
