package com.idworx.lisa.validation.authority

import com.idworx.lisa.features.androiddevicetesting.diagnostics.DeviceTestingDiagnostics
import com.idworx.lisa.features.androiddevicetesting.integration.Brain1DeviceTestingIntegration
import com.idworx.lisa.features.androiddevicetesting.model.DeviceProfile
import com.idworx.lisa.features.androiddevicetesting.model.DeviceTestOutcome
import com.idworx.lisa.features.androiddevicetesting.model.DeviceTestingReadinessOutcome
import com.idworx.lisa.features.androiddevicetesting.protocol.AndroidDeviceTestingProtocols
import com.idworx.lisa.features.androiddevicetesting.protocol.DeviceTestPlanBuilder
import com.idworx.lisa.features.androiddevicetesting.protocol.DeviceTestingEvidencePolicy
import com.idworx.lisa.features.androiddevicetesting.reporting.DeviceTestReportGenerator
import com.idworx.lisa.features.androiddevicetesting.reporting.DeviceTestingChecklist
import com.idworx.lisa.features.androiddevicetesting.suites.DeviceTestSuiteRegistry
import com.idworx.lisa.features.androiddevicetesting.validation.AndroidDeviceTestingProtocolAuthorityV1
import com.idworx.lisa.features.brain1readiness.reviewers.DeviceTestingReadinessReviewer
import com.idworx.lisa.validation.ValidationOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AndroidDeviceTestingProtocolAuthorityV1Test {

    private lateinit var protocol: com.idworx.lisa.features.androiddevicetesting.protocol.DefaultAndroidDeviceTestingProtocol

    @Before
    fun setUp() {
        AndroidDeviceTestingProtocols.resetForTests()
        protocol = AndroidDeviceTestingProtocols.createForTests()
    }

    @Test
    fun fullValidation_passesAndEmitsPassToken() {
        val report = AndroidDeviceTestingProtocolAuthorityV1.validate()
        if (report.outcome != ValidationOutcome.PASS) {
            println("FAILED: ${report.failedChecks}")
        }
        assertEquals("Failed: ${report.failedChecks}", ValidationOutcome.PASS, report.outcome)
        assertEquals(AndroidDeviceTestingProtocolAuthorityV1.PASS_TOKEN, report.passToken)
        assertTrue(report.failedChecks.isEmpty())
        println(report.formatReport())
        println(AndroidDeviceTestingProtocolAuthorityV1.PASS_TOKEN)
    }

    @Test
    fun deviceProfileCreation() {
        val profile = DeviceProfile(
            deviceModel = "Pixel Test",
            androidVersion = "14",
            cameraAvailable = true,
            frontCameraAvailable = true
        )
        assertEquals("Pixel Test", profile.deviceModel)
    }

    @Test
    fun testSessionCreation() {
        val session = protocol.createSession(
            deviceProfile = DeviceProfile(deviceModel = "Test Device"),
            testerLabel = "Tester A",
            testEnvironment = "Lab bench"
        )
        assertTrue(session.sessionId.isNotBlank())
        assertTrue(session.plan.suites.isNotEmpty())
    }

    @Test
    fun testPlanGeneration() {
        val plan = protocol.buildTestPlan()
        assertTrue(plan.suites.size >= 13)
        assertTrue(DeviceTestPlanBuilder.totalSteps(plan) > 50)
    }

    @Test
    fun suiteGeneration() {
        val suites = DeviceTestSuiteRegistry.allSuites()
        assertTrue(suites.size >= 13)
        assertTrue(suites.all { it.cases.isNotEmpty() })
    }

    @Test
    fun notTestedDefaultState() {
        val plan = protocol.buildTestPlan()
        plan.suites.forEach { suite ->
            suite.cases.forEach { testCase ->
                testCase.steps.forEach { step ->
                    assertEquals(DeviceTestOutcome.NOT_TESTED, step.outcome)
                }
            }
        }
        val session = protocol.createSession(DeviceProfile(), "Tester")
        val report = protocol.generateReport(session.sessionId)!!
        assertEquals(DeviceTestingReadinessOutcome.DEVICE_TESTING_NOT_STARTED, report.readinessOutcome)
        assertFalse(protocol.hasRecordedDeviceEvidence())
    }

    @Test
    fun manualEvidenceRecording_pass() {
        val session = protocol.createSession(DeviceProfile(deviceModel = "Pixel"), "Tester")
        val step = session.plan.suites.first().cases.first().steps.first()
        val suite = session.plan.suites.first()
        val testCase = session.plan.suites.first().cases.first()
        val updated = protocol.recordStepResult(
            session.sessionId, suite.suiteId, testCase.caseId, step.stepId,
            DeviceTestOutcome.PASS, "Observed successful launch on Pixel", "Tester"
        )
        assertNotNull(updated)
        assertTrue(protocol.hasRecordedDeviceEvidence())
    }

    @Test
    fun passWithoutEvidence_rejected() {
        val session = protocol.createSession(DeviceProfile(), "Tester")
        val step = session.plan.suites.first().cases.first().steps.first()
        val suite = session.plan.suites.first()
        val testCase = session.plan.suites.first().cases.first()
        val result = protocol.recordStepResult(
            session.sessionId, suite.suiteId, testCase.caseId, step.stepId,
            DeviceTestOutcome.PASS, "", "Tester"
        )
        assertNull(result)
        assertFalse(protocol.hasRecordedDeviceEvidence())
    }

    @Test
    fun failResultRecording() {
        val session = protocol.createSession(DeviceProfile(), "Tester")
        val suite = session.plan.suites.first()
        val testCase = suite.cases.first()
        val step = testCase.steps.first()
        val updated = protocol.recordStepResult(
            session.sessionId, suite.suiteId, testCase.caseId, step.stepId,
            DeviceTestOutcome.FAIL, "App crashed on camera init", "Tester"
        )
        assertNotNull(updated)
        val report = protocol.generateReport(session.sessionId)!!
        assertTrue(report.failures.isNotEmpty())
    }

    @Test
    fun blockedResultRecording() {
        val session = protocol.createSession(DeviceProfile(), "Tester")
        val suite = session.plan.suites.first()
        val testCase = suite.cases.first()
        val step = testCase.steps.first()
        protocol.recordStepResult(
            session.sessionId, suite.suiteId, testCase.caseId, step.stepId,
            DeviceTestOutcome.BLOCKED, "Critical camera failure", "Tester"
        )
        val report = protocol.generateReport(session.sessionId)!!
        assertEquals(DeviceTestingReadinessOutcome.BLOCKED_BY_CRITICAL_DEVICE_FAILURE, report.readinessOutcome)
    }

    @Test
    fun reportGeneration() {
        val session = protocol.createSession(DeviceProfile(deviceModel = "Samsung"), "Tester")
        val report = protocol.generateReport(session.sessionId)!!
        assertTrue(report.summary.isNotBlank())
        assertTrue(report.honestAssessment.contains("clinical", ignoreCase = true))
        assertTrue(report.recommendations.isNotEmpty())
        assertTrue(report.risks.isNotEmpty())
    }

    @Test
    fun riskAndRecommendationGeneration() {
        val session = protocol.createSession(DeviceProfile(), "Tester")
        val report = protocol.generateReport(session.sessionId)!!
        assertTrue(report.risks.any { it.description.contains("No real device", ignoreCase = true) })
        assertTrue(report.recommendations.any { it.message.contains("device testing", ignoreCase = true) })
    }

    @Test
    fun checklistGeneration() {
        val checklist = DeviceTestingChecklist.format(protocol.buildTestPlan())
        assertTrue(checklist.contains("NOT_TESTED"))
        assertTrue(checklist.contains("Launch and Permission"))
    }

    @Test
    fun brain1ReadinessIntegration() {
        assertTrue(Brain1DeviceTestingIntegration.protocolExists())
        assertTrue(Brain1DeviceTestingIntegration.deviceTestingGapStillApplies())
        val review = DeviceTestingReadinessReviewer.review()
        assertTrue(review.evidence.contains("protocol", ignoreCase = true))
    }

    @Test
    fun noFakePassWithoutEvidence() {
        assertFalse(DeviceTestingEvidencePolicy.wouldFakePassWithoutEvidence())
        assertFalse(DeviceTestingEvidencePolicy.isValidRecording(DeviceTestOutcome.PASS, ""))
    }

    @Test
    fun noBrain2OrLlmDependency() {
        val report = AndroidDeviceTestingProtocolAuthorityV1.validate()
        assertTrue(report.checkResults.find { it.checkId == "ADTP_031" }?.passed == true)
        assertTrue(report.checkResults.find { it.checkId == "ADTP_032" }?.passed == true)
    }

    @Test
    fun diagnostics_available() {
        val session = protocol.createSession(DeviceProfile(), "Tester")
        protocol.generateReport(session.sessionId)
        assertNotNull(DeviceTestingDiagnostics.lastReport())
        assertTrue(DeviceTestingDiagnostics.formatSummary().contains("Android Device Testing Diagnostics"))
    }

    @Test
    fun aggregateOutcomeLogic() {
        assertEquals(DeviceTestOutcome.NOT_TESTED, DeviceTestReportGenerator.aggregateOutcome(emptyList()))
        assertEquals(DeviceTestOutcome.PASS, DeviceTestReportGenerator.aggregateOutcome(listOf(DeviceTestOutcome.PASS)))
        assertEquals(DeviceTestOutcome.FAIL, DeviceTestReportGenerator.aggregateOutcome(listOf(DeviceTestOutcome.PASS, DeviceTestOutcome.FAIL)))
    }
}
