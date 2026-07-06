package com.idworx.lisa.features.androiddevicetesting.model

enum class DeviceTestOutcome {
    PASS,
    PASS_WITH_WARNING,
    FAIL,
    BLOCKED,
    NOT_TESTED
}

enum class DeviceTestingReadinessOutcome {
    DEVICE_TESTING_NOT_STARTED,
    DEVICE_TESTING_IN_PROGRESS,
    READY_FOR_MORE_DEVICE_TESTING,
    READY_FOR_CONTROLLED_USER_TESTING_WITH_SUPERVISION,
    NOT_READY_FOR_USER_TESTING,
    BLOCKED_BY_CRITICAL_DEVICE_FAILURE
}

enum class DeviceTestRiskSeverity {
    Low,
    Medium,
    High,
    Critical
}

data class DeviceProfile(
    val deviceModel: String = "",
    val androidVersion: String = "",
    val screenSizeInches: String = "",
    val cameraAvailable: Boolean = false,
    val frontCameraAvailable: Boolean = false,
    val ttsEngineAvailable: Boolean = false,
    val offlineTtsAvailable: Boolean = false,
    val batteryLevelStartPercent: Int? = null,
    val batteryLevelEndPercent: Int? = null,
    val sessionDurationMinutes: Int? = null,
    val orientation: String = "",
    val appVersion: String = "",
    val buildType: String = "",
    val networkState: String = "unknown"
)

data class DeviceTestEvidence(
    val evidenceId: String,
    val stepId: String,
    val recordedAtMs: Long,
    val notes: String,
    val observer: String = ""
)

data class DeviceTestStep(
    val stepId: String,
    val description: String,
    val outcome: DeviceTestOutcome = DeviceTestOutcome.NOT_TESTED,
    val evidenceNotes: String = "",
    val recordedAtMs: Long? = null
)

data class DeviceTestCase(
    val caseId: String,
    val title: String,
    val steps: List<DeviceTestStep>,
    val outcome: DeviceTestOutcome = DeviceTestOutcome.NOT_TESTED
)

data class DeviceTestSuite(
    val suiteId: String,
    val name: String,
    val cases: List<DeviceTestCase>,
    val outcome: DeviceTestOutcome = DeviceTestOutcome.NOT_TESTED
)

data class DeviceTestPlan(
    val planId: String,
    val protocolVersion: String,
    val suites: List<DeviceTestSuite>,
    val createdAtMs: Long
)

data class DeviceTestResult(
    val suiteId: String,
    val caseId: String,
    val stepId: String,
    val outcome: DeviceTestOutcome,
    val evidence: DeviceTestEvidence?,
    val recordedAtMs: Long
)

data class DeviceTestRisk(
    val riskId: String,
    val suiteId: String,
    val severity: DeviceTestRiskSeverity,
    val description: String,
    val evidence: String
)

data class DeviceTestRecommendation(
    val recommendationId: String,
    val message: String,
    val priority: String
)

data class DeviceTestSession(
    val sessionId: String,
    val startedAtMs: Long,
    val testerLabel: String,
    val testEnvironment: String,
    val lightingCondition: String,
    val phonePosition: String,
    val networkState: String,
    val deviceProfile: DeviceProfile,
    val plan: DeviceTestPlan,
    val results: List<DeviceTestResult> = emptyList(),
    val notes: String = ""
)

data class DeviceTestReport(
    val reportId: String,
    val generatedAtMs: Long,
    val session: DeviceTestSession,
    val deviceProfile: DeviceProfile,
    val suiteResults: List<DeviceTestSuite>,
    val stepResults: List<DeviceTestResult>,
    val evidence: List<DeviceTestEvidence>,
    val risks: List<DeviceTestRisk>,
    val failures: List<String>,
    val warnings: List<String>,
    val recommendations: List<DeviceTestRecommendation>,
    val overallOutcome: DeviceTestOutcome,
    val readinessOutcome: DeviceTestingReadinessOutcome,
    val summary: String,
    val honestAssessment: String
)
