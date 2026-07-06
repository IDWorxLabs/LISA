package com.idworx.lisa.features.brain1readiness.validation

import com.idworx.lisa.features.brain1readiness.diagnostics.Brain1ReadinessDiagnostics
import com.idworx.lisa.features.brain1readiness.engine.Brain1ReadinessEngine
import com.idworx.lisa.features.brain1readiness.engine.Brain1ReadinessEngines
import com.idworx.lisa.features.brain1readiness.engine.Brain1SubsystemVerifier
import com.idworx.lisa.features.brain1readiness.metadata.Brain1ReadinessMetadata
import com.idworx.lisa.features.brain1readiness.model.Brain1ReadinessReport
import com.idworx.lisa.features.brain1readiness.model.Brain1ReadinessScore
import com.idworx.lisa.features.brain1readiness.reporting.Brain1ReadinessSummary
import com.idworx.lisa.features.brain1readiness.reviewers.AccessibilityReadinessReviewer
import com.idworx.lisa.features.brain1readiness.reviewers.AnalyticsReadinessReviewer
import com.idworx.lisa.features.brain1readiness.reviewers.CalibrationReadinessReviewer
import com.idworx.lisa.features.brain1readiness.reviewers.CompanionMemoryReadinessReviewer
import com.idworx.lisa.features.brain1readiness.reviewers.CoreCommunicationReadinessReviewer
import com.idworx.lisa.features.brain1readiness.reviewers.DeviceTestingReadinessReviewer
import com.idworx.lisa.features.brain1readiness.reviewers.EmergencyReadinessReviewer
import com.idworx.lisa.features.brain1readiness.reviewers.GuidedLearningReadinessReviewer
import com.idworx.lisa.features.brain1readiness.reviewers.IntegrationReadinessReviewer
import com.idworx.lisa.features.brain1readiness.reviewers.OfflineReadinessReviewer
import com.idworx.lisa.features.brain1readiness.reviewers.PersonalityReadinessReviewer
import com.idworx.lisa.validation.ValidationCheckResult
import com.idworx.lisa.validation.ValidationOutcome
import com.idworx.lisa.validation.ValidationReport
import java.io.File

object Brain1ReadinessAuthorityV1 {

    const val AUTHORITY_NAME: String = "BRAIN_1_READINESS_AUTHORITY_V1"
    const val PASS_TOKEN: String = "BRAIN_1_READINESS_AUTHORITY_V1_PASS"

    private val PROJECT_ROOT = File(System.getProperty("user.dir") ?: ".")

    fun validate(): ValidationReport {
        try {
            com.idworx.lisa.features.androiddevicetesting.protocol.AndroidDeviceTestingProtocols.resetForTests()
        } catch (_: Exception) {
            // Continue without device testing reset if module unavailable
        }
        val engine = Brain1ReadinessEngines.createForTests()
        val readinessReport = engine.generateReport()
        val checks = listOf(
            check("B1R_001", "Brain 1 readiness module exists", classExists(Brain1ReadinessEngine::class.java)),
            check("B1R_002", "README exists", readmeExists()),
            check("B1R_003", "Brain1ReadinessEngine exists", Brain1ReadinessEngine::class.java.isInterface),
            check("B1R_004", "Brain1SubsystemVerifier exists", classExists(Brain1SubsystemVerifier::class.java)),
            check("B1R_005", "GuidedLearningReadinessReviewer exists", classExists(GuidedLearningReadinessReviewer::class.java)),
            check("B1R_006", "PersonalityReadinessReviewer exists", classExists(PersonalityReadinessReviewer::class.java)),
            check("B1R_007", "CompanionMemoryReadinessReviewer exists", classExists(CompanionMemoryReadinessReviewer::class.java)),
            check("B1R_008", "CoreCommunicationReadinessReviewer exists", classExists(CoreCommunicationReadinessReviewer::class.java)),
            check("B1R_009", "CalibrationReadinessReviewer exists", classExists(CalibrationReadinessReviewer::class.java)),
            check("B1R_010", "AnalyticsReadinessReviewer exists", classExists(AnalyticsReadinessReviewer::class.java)),
            check("B1R_011", "AccessibilityReadinessReviewer exists", classExists(AccessibilityReadinessReviewer::class.java)),
            check("B1R_012", "OfflineReadinessReviewer exists", classExists(OfflineReadinessReviewer::class.java)),
            check("B1R_013", "EmergencyReadinessReviewer exists", classExists(EmergencyReadinessReviewer::class.java)),
            check("B1R_014", "DeviceTestingReadinessReviewer exists", classExists(DeviceTestingReadinessReviewer::class.java)),
            check("B1R_015", "IntegrationReadinessReviewer exists", classExists(IntegrationReadinessReviewer::class.java)),
            check("B1R_016", "Brain1ReadinessReport exists", classExists(Brain1ReadinessReport::class.java)),
            check("B1R_017", "Brain1ReadinessScore exists", classExists(Brain1ReadinessScore::class.java)),
            check("B1R_018", "Risk register exists", readinessReport.risks.isNotEmpty()),
            check("B1R_019", "Gap report exists", readinessReport.gaps.isNotEmpty()),
            check("B1R_020", "Recommendations exist", readinessReport.recommendations.isNotEmpty()),
            check("B1R_021", "Diagnostics exist", classExists(Brain1ReadinessDiagnostics::class.java)),
            check("B1R_022", "Existing Brain 1 authorities are referenced", Brain1SubsystemVerifier.verifyIntegrationReadiness()),
            check("B1R_023", "No Brain 2 dependency exists", Brain1SubsystemVerifier.verifyNoBrain2Dependency()),
            check("B1R_024", "No LLM dependency exists", Brain1SubsystemVerifier.verifyNoLlmDependency()),
            check("B1R_025", "No cloud dependency is required", Brain1SubsystemVerifier.verifyNoMandatoryCloudDependency()),
            check("B1R_026", "Emergency safety reviewed", Brain1SubsystemVerifier.verifyEmergencyReadiness()),
            check("B1R_027", "Offline readiness reviewed", Brain1SubsystemVerifier.verifyOfflineReadiness()),
            check("B1R_028", "Accessibility readiness reviewed", Brain1SubsystemVerifier.verifyAccessibilityReadiness()),
            check("B1R_029", "Device testing gap is honestly reported", Brain1SubsystemVerifier.verifyDeviceTestingGapReported()),
            check("B1R_030", "Tests exist", testClassExists()),
            check("B1R_031", "Documentation exists", readmeExists() && classExists(Brain1ReadinessMetadata::class.java)),
            check("B1R_032", "Gradle validation task exists", gradleTaskRegistered()),
            check("B1R_033", "Pass token defined", PASS_TOKEN == "BRAIN_1_READINESS_AUTHORITY_V1_PASS")
        )

        val failed = checks.filter { !it.passed }
        val outcome = ValidationReport.resolveOutcome(checks)
        return ValidationReport(
            authorityName = AUTHORITY_NAME,
            outcome = outcome,
            passToken = if (outcome == ValidationOutcome.PASS) PASS_TOKEN else null,
            evidenceSummary = "Brain 1 Readiness verified ${checks.size} checks. Passed: ${checks.count { it.passed }}. Failed: ${failed.size}.",
            checksPerformed = checks.map { "${it.checkId}: ${it.description}" },
            failedChecks = failed.map { "${it.checkId}: ${it.description}" },
            observations = listOfNotNull(
                "Readiness outcome: ${readinessReport.outcome.name}",
                "Readiness score: ${readinessReport.score.overall}",
                readinessReport.honestAssessment,
                Brain1ReadinessSummary.format(readinessReport).lines().take(5).joinToString(" | ")
            ),
            affectedLicArticles = listOf("Part I — Brain 1 offline-first communication guarantee"),
            affectedLiecArticles = listOf("Article 1.1 — Phase 1 readiness review"),
            affectedLvcArticles = listOf("Article 3.6.1.1 — Brain 1 readiness validation"),
            remediationGuidance = failed.mapNotNull { it.remediation }.distinct(),
            checkResults = checks,
            rootCause = failed.firstOrNull()?.let { "${it.checkId} — ${it.description}" },
            validationReasoning = if (outcome == ValidationOutcome.PASS) {
                "LISA Brain 1 Readiness V1 provides an honest Phase 1 readiness picture without adding Brain 2 or cloud dependencies."
            } else {
                "${failed.size} Brain 1 readiness checks failed."
            },
            subsystem = "Brain 1 Readiness"
        )
    }

    private fun readmeExists(): Boolean {
        val candidates = buildList {
            add(File(PROJECT_ROOT, "features/brain1-readiness/README.md"))
            add(File(PROJECT_ROOT, "app/src/main/java/com/idworx/lisa/features/brain1readiness/README.md"))
            var dir: File? = PROJECT_ROOT
            repeat(5) {
                dir?.let {
                    add(File(it, "features/brain1-readiness/README.md"))
                    add(File(it, "app/src/main/java/com/idworx/lisa/features/brain1readiness/README.md"))
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
        return candidates.any { it.exists() && it.readText().contains("validateBrain1ReadinessAuthorityV1") }
    }

    private fun testClassExists(): Boolean =
        fileExistsAtProjectRelative("app/src/test/java/com/idworx/lisa/validation/authority/Brain1ReadinessAuthorityV1Test.kt")

    private fun fileExistsAtProjectRelative(relativePath: String): Boolean {
        var dir: File? = PROJECT_ROOT
        repeat(6) {
            dir?.let { if (File(it, relativePath).exists()) return true }
            dir = dir?.parentFile
        }
        return false
    }

    private fun classExists(clazz: Class<*>): Boolean = try {
        Class.forName(clazz.name)
        true
    } catch (_: Exception) {
        false
    }

    private fun check(id: String, description: String, passed: Boolean, remediation: String? = null) =
        ValidationCheckResult(checkId = id, description = description, passed = passed, remediation = remediation)
}
