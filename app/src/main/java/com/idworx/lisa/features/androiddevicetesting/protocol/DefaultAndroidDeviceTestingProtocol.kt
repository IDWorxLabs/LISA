package com.idworx.lisa.features.androiddevicetesting.protocol

import com.idworx.lisa.features.androiddevicetesting.diagnostics.DeviceTestingDiagnostics
import com.idworx.lisa.features.androiddevicetesting.model.DeviceProfile
import com.idworx.lisa.features.androiddevicetesting.model.DeviceTestEvidence
import com.idworx.lisa.features.androiddevicetesting.model.DeviceTestOutcome
import com.idworx.lisa.features.androiddevicetesting.model.DeviceTestPlan
import com.idworx.lisa.features.androiddevicetesting.model.DeviceTestReport
import com.idworx.lisa.features.androiddevicetesting.model.DeviceTestResult
import com.idworx.lisa.features.androiddevicetesting.model.DeviceTestSession
import com.idworx.lisa.features.androiddevicetesting.reporting.DeviceTestReportGenerator
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class DefaultAndroidDeviceTestingProtocol : AndroidDeviceTestingProtocol {

    private val sessionsInternal = ConcurrentHashMap<String, DeviceTestSession>()

    override fun createSession(
        deviceProfile: DeviceProfile,
        testerLabel: String,
        testEnvironment: String,
        lightingCondition: String,
        phonePosition: String,
        networkState: String
    ): DeviceTestSession {
        val plan = buildTestPlan()
        val session = DeviceTestSession(
            sessionId = UUID.randomUUID().toString(),
            startedAtMs = System.currentTimeMillis(),
            testerLabel = testerLabel,
            testEnvironment = testEnvironment,
            lightingCondition = lightingCondition,
            phonePosition = phonePosition,
            networkState = networkState,
            deviceProfile = deviceProfile,
            plan = plan
        )
        sessionsInternal[session.sessionId] = session
        return session
    }

    override fun buildTestPlan(): DeviceTestPlan = DeviceTestPlanBuilder.build()

    override fun recordStepResult(
        sessionId: String,
        suiteId: String,
        caseId: String,
        stepId: String,
        outcome: DeviceTestOutcome,
        evidenceNotes: String,
        observer: String
    ): DeviceTestSession? {
        if (!DeviceTestingEvidencePolicy.isValidRecording(outcome, evidenceNotes)) return null
        val session = sessionsInternal[sessionId] ?: return null
        val evidence = if (evidenceNotes.isNotBlank()) {
            DeviceTestEvidence(
                evidenceId = UUID.randomUUID().toString(),
                stepId = stepId,
                recordedAtMs = System.currentTimeMillis(),
                notes = evidenceNotes,
                observer = observer
            )
        } else null
        val result = DeviceTestResult(
            suiteId = suiteId,
            caseId = caseId,
            stepId = stepId,
            outcome = outcome,
            evidence = evidence,
            recordedAtMs = System.currentTimeMillis()
        )
        val updated = session.copy(results = session.results.filterNot {
            it.suiteId == suiteId && it.caseId == caseId && it.stepId == stepId
        } + result)
        sessionsInternal[sessionId] = updated
        return updated
    }

    override fun generateReport(sessionId: String): DeviceTestReport? {
        val session = sessionsInternal[sessionId] ?: return null
        val report = DeviceTestReportGenerator.generate(session)
        DeviceTestingDiagnostics.record(report)
        return report
    }

    override fun hasRecordedDeviceEvidence(): Boolean =
        sessionsInternal.values.any { session ->
            session.results.any { it.outcome != DeviceTestOutcome.NOT_TESTED && it.evidence != null }
        }

    override fun sessions(): List<DeviceTestSession> = sessionsInternal.values.toList()
}

object DeviceTestingEvidencePolicy {

    /**
     * PASS and PASS_WITH_WARNING require non-blank evidence.
     * FAIL and BLOCKED require evidence describing what happened.
     * NOT_TESTED requires no evidence (default state).
     */
    fun isValidRecording(outcome: DeviceTestOutcome, evidenceNotes: String): Boolean = when (outcome) {
        DeviceTestOutcome.NOT_TESTED -> evidenceNotes.isBlank()
        DeviceTestOutcome.PASS, DeviceTestOutcome.PASS_WITH_WARNING,
        DeviceTestOutcome.FAIL, DeviceTestOutcome.BLOCKED -> evidenceNotes.isNotBlank()
    }

    fun wouldFakePassWithoutEvidence(): Boolean = false
}

object AndroidDeviceTestingProtocols {
    @Volatile
    private var instance: DefaultAndroidDeviceTestingProtocol? = null

    val default: AndroidDeviceTestingProtocol
        get() = instance ?: DefaultAndroidDeviceTestingProtocol().also { instance = it }

    fun createForTests(): DefaultAndroidDeviceTestingProtocol {
        val protocol = DefaultAndroidDeviceTestingProtocol()
        instance = protocol
        return protocol
    }

    fun resetForTests() {
        instance = null
        DeviceTestingDiagnostics.clear()
    }
}
