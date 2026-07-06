package com.idworx.lisa.validation.authority

import com.idworx.lisa.features.brain1readiness.diagnostics.Brain1ReadinessDiagnostics
import com.idworx.lisa.features.brain1readiness.engine.Brain1ReadinessEngines
import com.idworx.lisa.features.brain1readiness.engine.Brain1SubsystemVerifier
import com.idworx.lisa.features.brain1readiness.model.Brain1Gap
import com.idworx.lisa.features.brain1readiness.model.Brain1ReadinessOutcome
import com.idworx.lisa.features.brain1readiness.model.Brain1Risk
import com.idworx.lisa.features.brain1readiness.model.Brain1RiskSeverity
import com.idworx.lisa.features.brain1readiness.model.Brain1Subsystem
import com.idworx.lisa.features.brain1readiness.model.Brain1SubsystemStatus
import com.idworx.lisa.features.brain1readiness.reporting.Brain1ReadinessReportGenerator
import com.idworx.lisa.features.brain1readiness.reporting.Brain1ReadinessSummary
import com.idworx.lisa.features.brain1readiness.reviewers.Brain1ReadinessReviewRunner
import com.idworx.lisa.features.brain1readiness.validation.Brain1ReadinessAuthorityV1
import com.idworx.lisa.validation.ValidationOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class Brain1ReadinessAuthorityV1Test {

    private lateinit var engine: com.idworx.lisa.features.brain1readiness.engine.DefaultBrain1ReadinessEngine

    @Before
    fun setUp() {
        Brain1ReadinessEngines.resetForTests()
        engine = Brain1ReadinessEngines.createForTests()
    }

    @Test
    fun fullValidation_passesAndEmitsPassToken() {
        val report = Brain1ReadinessAuthorityV1.validate()
        if (report.outcome != ValidationOutcome.PASS) {
            println("FAILED: ${report.failedChecks}")
        }
        assertEquals("Failed: ${report.failedChecks}", ValidationOutcome.PASS, report.outcome)
        assertEquals(Brain1ReadinessAuthorityV1.PASS_TOKEN, report.passToken)
        assertTrue(report.failedChecks.isEmpty())
        println(report.formatReport())
        println(Brain1ReadinessAuthorityV1.PASS_TOKEN)
    }

    @Test
    fun readinessScoreGeneration() {
        assertTrue(Brain1SubsystemVerifier.verifyScoreGeneration())
        val report = engine.generateReport()
        assertTrue(report.score.overall in 0..100)
        assertTrue(report.score.checksTotal > 0)
    }

    @Test
    fun readyWithWarningsOutcome_isHonest() {
        val report = engine.generateReport()
        assertEquals(
            "Phase 1 should report READY_WITH_WARNINGS until real device testing completes",
            Brain1ReadinessOutcome.READY_WITH_WARNINGS,
            report.outcome
        )
    }

    @Test
    fun readyOutcome_whenEvidenceSupports() {
        val outcome = Brain1SubsystemVerifier.simulateOutcome(
            score = 95,
            status = Brain1SubsystemStatus.Ready,
            risks = emptyList()
        )
        assertEquals(Brain1ReadinessOutcome.READY_FOR_DEVICE_TESTING, outcome)
    }

    @Test
    fun notReadyOutcome_whenSubsystemDegraded() {
        val outcome = Brain1SubsystemVerifier.simulateOutcome(
            score = 60,
            status = Brain1SubsystemStatus.Degraded,
            risks = emptyList()
        )
        assertEquals(Brain1ReadinessOutcome.NOT_READY, outcome)
    }

    @Test
    fun blockedOutcome_whenCriticalRisk() {
        val outcome = Brain1SubsystemVerifier.simulateOutcome(
            score = 95,
            status = Brain1SubsystemStatus.Ready,
            risks = listOf(
                Brain1Risk(
                    "TEST_R001", Brain1Subsystem.CoreCommunication,
                    Brain1RiskSeverity.Critical, "Simulated critical blocker", "test"
                )
            )
        )
        assertEquals(Brain1ReadinessOutcome.BLOCKED, outcome)
    }

    @Test
    fun subsystemStatusGeneration() {
        val results = Brain1ReadinessReviewRunner.runAllReviewers()
        assertTrue(results.size >= 10)
        assertTrue(results.all { it.checksPerformed > 0 })
    }

    @Test
    fun riskRegisterGeneration() {
        val report = engine.generateReport()
        assertTrue(report.risks.isNotEmpty())
        assertTrue(report.risks.any { it.severity == Brain1RiskSeverity.Medium || it.severity == Brain1RiskSeverity.High })
    }

    @Test
    fun gapReportGeneration() {
        val report = engine.generateReport()
        assertTrue(report.gaps.isNotEmpty())
        assertTrue(Brain1SubsystemVerifier.verifyDeviceTestingGapReported())
    }

    @Test
    fun deviceTestingGapReported() {
        val report = engine.generateReport()
        assertTrue(report.gaps.any { it.description.contains("Real Android device testing still needed") })
        assertTrue(report.honestAssessment.contains("device testing", ignoreCase = true))
    }

    @Test
    fun noBrain2Dependency() {
        assertTrue(Brain1SubsystemVerifier.verifyNoBrain2Dependency())
    }

    @Test
    fun noLlmDependency() {
        assertTrue(Brain1SubsystemVerifier.verifyNoLlmDependency())
    }

    @Test
    fun noMandatoryCloudDependency() {
        assertTrue(Brain1SubsystemVerifier.verifyNoMandatoryCloudDependency())
    }

    @Test
    fun emergencyReadiness() {
        assertTrue(Brain1SubsystemVerifier.verifyEmergencyReadiness())
    }

    @Test
    fun offlineReadiness() {
        assertTrue(Brain1SubsystemVerifier.verifyOfflineReadiness())
    }

    @Test
    fun accessibilityReadiness() {
        assertTrue(Brain1SubsystemVerifier.verifyAccessibilityReadiness())
    }

    @Test
    fun integrationReadiness() {
        assertTrue(Brain1SubsystemVerifier.verifyIntegrationReadiness())
    }

    @Test
    fun reportAndSummaryGeneration() {
        assertTrue(Brain1SubsystemVerifier.verifyReportGeneration())
        val report = engine.generateReport()
        val summary = Brain1ReadinessSummary.format(report)
        assertTrue(summary.contains("Brain 1 Readiness Review"))
        assertTrue(summary.contains("Risk register"))
        assertTrue(summary.contains("Gap report"))
    }

    @Test
    fun diagnostics_available() {
        engine.generateReport()
        assertNotNull(Brain1ReadinessDiagnostics.lastReport())
        assertTrue(Brain1ReadinessDiagnostics.formatSummary().contains("Brain 1 Readiness Diagnostics"))
    }

    @Test
    fun noCommunicationBehaviorChange() {
        assertTrue(Brain1SubsystemVerifier.verifyNoCommunicationBehaviorChange())
    }
}
