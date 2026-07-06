package com.idworx.lisa.features.androiddevicetesting.protocol

import com.idworx.lisa.features.androiddevicetesting.model.DeviceProfile
import com.idworx.lisa.features.androiddevicetesting.model.DeviceTestPlan
import com.idworx.lisa.features.androiddevicetesting.model.DeviceTestReport
import com.idworx.lisa.features.androiddevicetesting.model.DeviceTestSession

interface AndroidDeviceTestingProtocol {
    fun createSession(
        deviceProfile: DeviceProfile,
        testerLabel: String,
        testEnvironment: String = "",
        lightingCondition: String = "",
        phonePosition: String = "",
        networkState: String = "unknown"
    ): DeviceTestSession

    fun buildTestPlan(): DeviceTestPlan

    fun recordStepResult(
        sessionId: String,
        suiteId: String,
        caseId: String,
        stepId: String,
        outcome: com.idworx.lisa.features.androiddevicetesting.model.DeviceTestOutcome,
        evidenceNotes: String,
        observer: String = ""
    ): DeviceTestSession?

    fun generateReport(sessionId: String): DeviceTestReport?

    fun hasRecordedDeviceEvidence(): Boolean

    fun sessions(): List<DeviceTestSession>
}
