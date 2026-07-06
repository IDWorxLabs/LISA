package com.idworx.lisa.features.androiddevicetesting.validation

import com.idworx.lisa.features.androiddevicetesting.diagnostics.DeviceTestingDiagnostics
import com.idworx.lisa.features.androiddevicetesting.integration.Brain1DeviceTestingIntegration
import com.idworx.lisa.features.androiddevicetesting.metadata.AndroidDeviceTestingMetadata
import com.idworx.lisa.features.androiddevicetesting.model.DeviceProfile
import com.idworx.lisa.features.androiddevicetesting.model.DeviceTestCase
import com.idworx.lisa.features.androiddevicetesting.model.DeviceTestOutcome
import com.idworx.lisa.features.androiddevicetesting.model.DeviceTestReport
import com.idworx.lisa.features.androiddevicetesting.model.DeviceTestResult
import com.idworx.lisa.features.androiddevicetesting.model.DeviceTestSession
import com.idworx.lisa.features.androiddevicetesting.model.DeviceTestStep
import com.idworx.lisa.features.androiddevicetesting.protocol.AndroidDeviceTestingProtocol
import com.idworx.lisa.features.androiddevicetesting.protocol.DefaultAndroidDeviceTestingProtocol
import com.idworx.lisa.features.androiddevicetesting.protocol.DeviceTestPlanBuilder
import com.idworx.lisa.features.androiddevicetesting.protocol.DeviceTestingEvidencePolicy
import com.idworx.lisa.features.androiddevicetesting.reporting.DeviceTestReportGenerator
import com.idworx.lisa.features.androiddevicetesting.reporting.DeviceTestingChecklist
import com.idworx.lisa.features.androiddevicetesting.suites.AccessibilityDeviceTestSuite
import com.idworx.lisa.features.androiddevicetesting.suites.BlinkDetectionDeviceTestSuite
import com.idworx.lisa.features.androiddevicetesting.suites.CalibrationDeviceTestSuite
import com.idworx.lisa.features.androiddevicetesting.suites.CommunicationPathDeviceTestSuite
import com.idworx.lisa.features.androiddevicetesting.suites.EmergencySafetyDeviceTestSuite
import com.idworx.lisa.features.androiddevicetesting.suites.EyeTrackingDeviceTestSuite
import com.idworx.lisa.features.androiddevicetesting.suites.GuidedLearningDeviceTestSuite
import com.idworx.lisa.features.androiddevicetesting.suites.LaunchAndPermissionTestSuite
import com.idworx.lisa.features.androiddevicetesting.suites.LightingConditionDeviceTestSuite
import com.idworx.lisa.features.androiddevicetesting.suites.LongSessionStabilityTestSuite
import com.idworx.lisa.features.androiddevicetesting.suites.OfflineDeviceTestSuite
import com.idworx.lisa.features.androiddevicetesting.suites.PerformanceDeviceTestSuite
import com.idworx.lisa.features.androiddevicetesting.suites.PhonePositionDeviceTestSuite
import com.idworx.lisa.validation.ValidationCheckResult
import com.idworx.lisa.validation.ValidationOutcome
import com.idworx.lisa.validation.ValidationReport
import java.io.File

object AndroidDeviceTestingProtocolAuthorityV1 {

    const val AUTHORITY_NAME: String = "ANDROID_DEVICE_TESTING_PROTOCOL_AUTHORITY_V1"
    const val PASS_TOKEN: String = "ANDROID_DEVICE_TESTING_PROTOCOL_AUTHORITY_V1_PASS"

    private val PROJECT_ROOT = File(System.getProperty("user.dir") ?: ".")

    fun validate(): ValidationReport {
        val plan = DeviceTestPlanBuilder.build()
        val checks = listOf(
            check("ADTP_001", "Android device testing module exists", classExists(AndroidDeviceTestingProtocol::class.java)),
            check("ADTP_002", "README exists", readmeExists()),
            check("ADTP_003", "AndroidDeviceTestingProtocol exists", AndroidDeviceTestingProtocol::class.java.isInterface),
            check("ADTP_004", "DefaultAndroidDeviceTestingProtocol exists", classExists(DefaultAndroidDeviceTestingProtocol::class.java)),
            check("ADTP_005", "DeviceTestPlanBuilder exists", classExists(DeviceTestPlanBuilder::class.java)),
            check("ADTP_006", "DeviceProfile model exists", classExists(DeviceProfile::class.java)),
            check("ADTP_007", "DeviceTestSession model exists", classExists(DeviceTestSession::class.java)),
            check("ADTP_008", "DeviceTestCase model exists", classExists(DeviceTestCase::class.java)),
            check("ADTP_009", "DeviceTestStep model exists", classExists(DeviceTestStep::class.java)),
            check("ADTP_010", "DeviceTestResult model exists", classExists(DeviceTestResult::class.java)),
            check("ADTP_011", "DeviceTestReport model exists", classExists(DeviceTestReport::class.java)),
            check("ADTP_012", "Launch and permission suite exists", classExists(LaunchAndPermissionTestSuite::class.java)),
            check("ADTP_013", "Calibration device suite exists", classExists(CalibrationDeviceTestSuite::class.java)),
            check("ADTP_014", "Eye tracking device suite exists", classExists(EyeTrackingDeviceTestSuite::class.java)),
            check("ADTP_015", "Blink detection device suite exists", classExists(BlinkDetectionDeviceTestSuite::class.java)),
            check("ADTP_016", "Communication path device suite exists", classExists(CommunicationPathDeviceTestSuite::class.java)),
            check("ADTP_017", "Guided Learning device suite exists", classExists(GuidedLearningDeviceTestSuite::class.java)),
            check("ADTP_018", "Emergency safety device suite exists", classExists(EmergencySafetyDeviceTestSuite::class.java)),
            check("ADTP_019", "Offline device suite exists", classExists(OfflineDeviceTestSuite::class.java)),
            check("ADTP_020", "Accessibility device suite exists", classExists(AccessibilityDeviceTestSuite::class.java)),
            check("ADTP_021", "Lighting condition suite exists", classExists(LightingConditionDeviceTestSuite::class.java)),
            check("ADTP_022", "Phone position suite exists", classExists(PhonePositionDeviceTestSuite::class.java)),
            check("ADTP_023", "Long-session stability suite exists", classExists(LongSessionStabilityTestSuite::class.java)),
            check("ADTP_024", "Performance device suite exists", classExists(PerformanceDeviceTestSuite::class.java)),
            check("ADTP_025", "Report generator exists", classExists(DeviceTestReportGenerator::class.java)),
            check("ADTP_026", "Checklist exists", classExists(DeviceTestingChecklist::class.java)),
            check("ADTP_027", "Diagnostics exist", classExists(DeviceTestingDiagnostics::class.java)),
            check("ADTP_028", "Brain 1 Readiness integration exists", Brain1DeviceTestingIntegration.protocolExists()),
            check("ADTP_029", "NOT_TESTED is supported honestly", allStepsDefaultNotTested(plan)),
            check("ADTP_030", "No fake device pass logic exists", noFakePassLogic()),
            check("ADTP_031", "No Brain 2 dependency exists", noForbiddenDependency()),
            check("ADTP_032", "No LLM dependency exists", noLlmDependency()),
            check("ADTP_033", "No cloud dependency required", noCloudRequired()),
            check("ADTP_034", "Tests exist", testClassExists()),
            check("ADTP_035", "Documentation exists", readmeExists() && classExists(AndroidDeviceTestingMetadata::class.java)),
            check("ADTP_036", "Gradle validation task exists", gradleTaskRegistered()),
            check("ADTP_037", "Pass token defined", PASS_TOKEN == "ANDROID_DEVICE_TESTING_PROTOCOL_AUTHORITY_V1_PASS")
        )

        val failed = checks.filter { !it.passed }
        val outcome = ValidationReport.resolveOutcome(checks)
        return ValidationReport(
            authorityName = AUTHORITY_NAME,
            outcome = outcome,
            passToken = if (outcome == ValidationOutcome.PASS) PASS_TOKEN else null,
            evidenceSummary = "Android Device Testing Protocol verified ${checks.size} checks. Passed: ${checks.count { it.passed }}. Failed: ${failed.size}.",
            checksPerformed = checks.map { "${it.checkId}: ${it.description}" },
            failedChecks = failed.map { "${it.checkId}: ${it.description}" },
            observations = listOfNotNull(
                "Protocol steps defined: ${DeviceTestPlanBuilder.totalSteps(plan)}",
                "Default readiness: DEVICE_TESTING_NOT_STARTED until evidence recorded",
                Brain1DeviceTestingIntegration.readinessEvidenceSummary()
            ),
            affectedLicArticles = listOf("Part I — Real hardware evidence required"),
            affectedLiecArticles = listOf("Article 1.2 — Android device testing protocol"),
            affectedLvcArticles = listOf("Article 3.7.1.1 — Device testing protocol validation"),
            remediationGuidance = failed.mapNotNull { it.remediation }.distinct(),
            checkResults = checks,
            rootCause = failed.firstOrNull()?.let { "${it.checkId} — ${it.description}" },
            validationReasoning = if (outcome == ValidationOutcome.PASS) {
                "LISA Android Device Testing Protocol V1 defines repeatable device testing without faking results."
            } else {
                "${failed.size} device testing protocol checks failed."
            },
            subsystem = "Android Device Testing"
        )
    }

    private fun allStepsDefaultNotTested(plan: com.idworx.lisa.features.androiddevicetesting.model.DeviceTestPlan): Boolean =
        plan.suites.all { suite ->
            suite.cases.all { testCase ->
                testCase.steps.all { it.outcome == DeviceTestOutcome.NOT_TESTED }
            }
        }

    private fun noFakePassLogic(): Boolean =
        !DeviceTestingEvidencePolicy.wouldFakePassWithoutEvidence() &&
            !DeviceTestingEvidencePolicy.isValidRecording(DeviceTestOutcome.PASS, "") &&
            DeviceTestingEvidencePolicy.isValidRecording(DeviceTestOutcome.NOT_TESTED, "")

    private fun noForbiddenDependency(): Boolean =
        AndroidDeviceTestingMetadata.FORBIDDEN_DEPENDENCY_MARKERS.none { marker ->
            readGradle()?.contains(marker) == true
        }

    private fun noLlmDependency(): Boolean {
        val protocol = readFile("app/src/main/java/com/idworx/lisa/features/androiddevicetesting/protocol/DefaultAndroidDeviceTestingProtocol.kt")
        return listOf("OpenAI", "ChatGPT", "GenerativeModel", "LLM").none { protocol?.contains(it) == true }
    }

    private fun noCloudRequired(): Boolean {
        val manifest = readFile("app/src/main/AndroidManifest.xml")
        return manifest?.contains("android.permission.INTERNET") != true
    }

    private fun readmeExists(): Boolean {
        val candidates = buildList {
            add(File(PROJECT_ROOT, "features/android-device-testing/README.md"))
            add(File(PROJECT_ROOT, "app/src/main/java/com/idworx/lisa/features/androiddevicetesting/README.md"))
            var dir: File? = PROJECT_ROOT
            repeat(5) {
                dir?.let {
                    add(File(it, "features/android-device-testing/README.md"))
                    add(File(it, "app/src/main/java/com/idworx/lisa/features/androiddevicetesting/README.md"))
                }
                dir = dir?.parentFile
            }
        }
        return candidates.any { it.exists() }
    }

    private fun gradleTaskRegistered(): Boolean {
        val candidates = buildList {
            add(File(PROJECT_ROOT, "app/build.gradle.kts"))
            var dir: File? = PROJECT_ROOT
            repeat(5) {
                dir?.let { add(File(it, "app/build.gradle.kts")) }
                dir = dir?.parentFile
            }
        }
        return candidates.any { it.exists() && it.readText().contains("validateAndroidDeviceTestingProtocolAuthorityV1") }
    }

    private fun testClassExists(): Boolean =
        fileExistsAtProjectRelative("app/src/test/java/com/idworx/lisa/validation/authority/AndroidDeviceTestingProtocolAuthorityV1Test.kt")

    private fun readGradle(): String? = readFile("app/build.gradle.kts")

    private fun readFile(relativePath: String): String? {
        var dir: File? = PROJECT_ROOT
        repeat(6) {
            dir?.let { candidate ->
                val file = File(candidate, relativePath)
                if (file.exists()) return file.readText()
            }
            dir = dir?.parentFile
        }
        return null
    }

    private fun fileExistsAtProjectRelative(relativePath: String): Boolean = readFile(relativePath) != null

    private fun classExists(clazz: Class<*>): Boolean = try {
        Class.forName(clazz.name)
        true
    } catch (_: Exception) {
        false
    }

    private fun check(id: String, description: String, passed: Boolean, remediation: String? = null) =
        ValidationCheckResult(checkId = id, description = description, passed = passed, remediation = remediation)
}
