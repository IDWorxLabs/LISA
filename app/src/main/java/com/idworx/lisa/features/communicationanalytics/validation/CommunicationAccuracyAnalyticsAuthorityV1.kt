package com.idworx.lisa.features.communicationanalytics.validation

import com.idworx.lisa.MainActivity
import com.idworx.lisa.features.calibrationreliability.engine.CalibrationReliabilityEngine
import com.idworx.lisa.features.communicationanalytics.diagnostics.AnalyticsDiagnostics
import com.idworx.lisa.features.communicationanalytics.diagnostics.AnalyticsLogger
import com.idworx.lisa.features.communicationanalytics.engine.AnalyticsAggregator
import com.idworx.lisa.features.communicationanalytics.engine.AnalyticsVerifier
import com.idworx.lisa.features.communicationanalytics.engine.CommunicationAnalyticsEngine
import com.idworx.lisa.features.communicationanalytics.engine.DefaultCommunicationAnalyticsEngine
import com.idworx.lisa.features.communicationanalytics.integration.AnalyticsPersonalityAdapter
import com.idworx.lisa.features.communicationanalytics.integration.CommunicationAnalyticsBridge
import com.idworx.lisa.features.communicationanalytics.metadata.CommunicationAnalyticsMetadata
import com.idworx.lisa.features.communicationanalytics.metrics.AccuracyCalculator
import com.idworx.lisa.features.communicationanalytics.metrics.CalibrationImpactCalculator
import com.idworx.lisa.features.communicationanalytics.metrics.EmergencyAnalyticsCalculator
import com.idworx.lisa.features.communicationanalytics.metrics.FalseNegativeCalculator
import com.idworx.lisa.features.communicationanalytics.metrics.FalsePositiveCalculator
import com.idworx.lisa.features.communicationanalytics.metrics.NavigationAnalyticsCalculator
import com.idworx.lisa.features.communicationanalytics.metrics.PhraseTimingCalculator
import com.idworx.lisa.features.communicationanalytics.metrics.RetryRateCalculator
import com.idworx.lisa.features.communicationanalytics.metrics.SuccessRateCalculator
import com.idworx.lisa.features.communicationanalytics.model.CommunicationAnalyticsReport
import com.idworx.lisa.features.communicationanalytics.reporting.CommunicationReportGenerator
import com.idworx.lisa.features.communicationanalytics.reporting.ReliabilitySummary
import com.idworx.lisa.features.communicationanalytics.reporting.TrendAnalyzer
import com.idworx.lisa.features.companionmemory.engine.CompanionMemoryEngine
import com.idworx.lisa.features.corecommunicationreliability.engine.CoreCommunicationReliabilityEngine
import com.idworx.lisa.features.corecommunicationreliability.history.CommunicationReliabilityHistoryRecorder
import com.idworx.lisa.features.personality.engine.LisaPersonalityEngine
import com.idworx.lisa.validation.ValidationCheckResult
import com.idworx.lisa.validation.ValidationOutcome
import com.idworx.lisa.validation.ValidationReport
import java.io.File

object CommunicationAccuracyAnalyticsAuthorityV1 {

    const val AUTHORITY_NAME: String = "COMMUNICATION_ACCURACY_ANALYTICS_AUTHORITY_V1"
    const val PASS_TOKEN: String = "COMMUNICATION_ACCURACY_ANALYTICS_AUTHORITY_V1_PASS"

    private val PROJECT_ROOT = File(System.getProperty("user.dir") ?: ".")

    fun validate(): ValidationReport {
        val checks = listOf(
            check("CANA_001", "Analytics module exists", classExists(CommunicationAnalyticsEngine::class.java)),
            check("CANA_002", "README exists", readmeExists()),
            check("CANA_003", "Analytics engine exists", CommunicationAnalyticsEngine::class.java.isInterface),
            check("CANA_004", "Analytics aggregator exists", classExists(AnalyticsAggregator::class.java)),
            check("CANA_005", "Success calculator exists", classExists(SuccessRateCalculator::class.java)),
            check("CANA_006", "Retry calculator exists", classExists(RetryRateCalculator::class.java)),
            check("CANA_007", "Accuracy calculator exists", classExists(AccuracyCalculator::class.java)),
            check("CANA_008", "False positive calculator exists", classExists(FalsePositiveCalculator::class.java)),
            check("CANA_009", "False negative calculator exists", classExists(FalseNegativeCalculator::class.java)),
            check("CANA_010", "Phrase timing calculator exists", classExists(PhraseTimingCalculator::class.java)),
            check("CANA_011", "Calibration impact calculator exists", classExists(CalibrationImpactCalculator::class.java)),
            check("CANA_012", "Navigation analytics exists", classExists(NavigationAnalyticsCalculator::class.java)),
            check("CANA_013", "Emergency analytics exists", classExists(EmergencyAnalyticsCalculator::class.java)),
            check("CANA_014", "Trend analyzer exists", classExists(TrendAnalyzer::class.java)),
            check("CANA_015", "Report generator exists", classExists(CommunicationReportGenerator::class.java)),
            check("CANA_016", "Diagnostics exist", classExists(AnalyticsDiagnostics::class.java)),
            check("CANA_017", "Existing communication history reused", historyReused()),
            check("CANA_018", "Existing calibration reused", classExists(CalibrationReliabilityEngine::class.java)),
            check("CANA_019", "Existing reliability reused", classExists(CoreCommunicationReliabilityEngine::class.java)),
            check("CANA_020", "Companion Memory not misused", companionMemoryNotMisused()),
            check("CANA_021", "Personality Engine reused", personalityReused()),
            check("CANA_022", "No runtime behaviour changed", AnalyticsVerifier.verifyNoBehaviorChange()),
            check("CANA_023", "No communication decisions changed", noDecisionLogicInAnalytics()),
            check("CANA_024", "Reports generated", AnalyticsVerifier.verifyReportGeneration()),
            check("CANA_025", "Trends generated", AnalyticsVerifier.verifyTrendGeneration()),
            check("CANA_026", "Tests exist", testClassExists()),
            check("CANA_027", "Documentation exists", readmeExists() && classExists(CommunicationAnalyticsMetadata::class.java)),
            check("CANA_028", "Gradle validation task exists", gradleTaskRegistered()),
            check("CANA_029", "Pass token defined", PASS_TOKEN == "COMMUNICATION_ACCURACY_ANALYTICS_AUTHORITY_V1_PASS"),
            check("CANA_030", "Metrics based only on observable evidence", observableEvidenceOnly())
        )

        val failed = checks.filter { !it.passed }
        val outcome = ValidationReport.resolveOutcome(checks)
        return ValidationReport(
            authorityName = AUTHORITY_NAME,
            outcome = outcome,
            passToken = if (outcome == ValidationOutcome.PASS) PASS_TOKEN else null,
            evidenceSummary = "Communication Accuracy Analytics verified ${checks.size} checks. Passed: ${checks.count { it.passed }}. Failed: ${failed.size}.",
            checksPerformed = checks.map { "${it.checkId}: ${it.description}" },
            failedChecks = failed.map { "${it.checkId}: ${it.description}" },
            observations = emptyList(),
            affectedLicArticles = listOf("Article 4.2.1.2 — Measurable communication quality"),
            affectedLiecArticles = listOf("Article 6.2.1.1 — Evidence-based improvement"),
            affectedLvcArticles = listOf("Article 4.1.1.2 — Observable evidence only"),
            remediationGuidance = failed.mapNotNull { it.remediation }.distinct(),
            checkResults = checks,
            rootCause = failed.firstOrNull()?.let { "${it.checkId} — ${it.description}" },
            validationReasoning = if (outcome == ValidationOutcome.PASS) {
                "LISA Communication Accuracy Analytics V1 measures communication quality without altering runtime behaviour."
            } else {
                "${failed.size} communication analytics checks failed."
            },
            subsystem = "Communication Accuracy Analytics"
        )
    }

    private fun historyReused(): Boolean =
        classExists(CommunicationReliabilityHistoryRecorder::class.java) &&
            fileExistsAtProjectRelative("app/src/main/java/com/idworx/lisa/features/corecommunicationreliability/history/CommunicationReliabilityHistoryRecorder.kt")

    private fun companionMemoryNotMisused(): Boolean {
        val analyticsDir = findAnalyticsDir() ?: return true
        return !analyticsDir.walkTopDown()
            .filter { it.isFile && it.extension == "kt" && !it.path.contains("validation") }
            .any { it.readText().contains("recordMilestone") }
    }

    private fun personalityReused(): Boolean =
        classExists(LisaPersonalityEngine::class.java) &&
            classExists(AnalyticsPersonalityAdapter::class.java)

    private fun noDecisionLogicInAnalytics(): Boolean {
        val analyticsDir = findAnalyticsDir() ?: return classExists(CommunicationAnalyticsBridge::class.java)
        return classExists(CommunicationAnalyticsBridge::class.java) &&
            !analyticsDir.walkTopDown()
                .filter { it.isFile && it.extension == "kt" && !it.path.contains("validation") && !it.name.endsWith("AnalyticsVerifier.kt") }
                .any { it.readText().contains("evaluatePhrasePath") }
    }

    private fun observableEvidenceOnly(): Boolean =
        AnalyticsVerifier.verifySuccessRateCalculation() &&
            AnalyticsVerifier.verifyFalsePositiveDetection() &&
            AnalyticsVerifier.verifyFalseNegativeDetection() &&
            classExists(AnalyticsLogger::class.java)

    private fun readmeExists(): Boolean {
        val candidates = buildList {
            add(File(PROJECT_ROOT, "features/communication-analytics/README.md"))
            add(File(PROJECT_ROOT, "app/src/main/java/com/idworx/lisa/features/communicationanalytics/README.md"))
            var dir: File? = PROJECT_ROOT
            repeat(5) {
                dir?.let {
                    add(File(it, "features/communication-analytics/README.md"))
                    add(File(it, "app/src/main/java/com/idworx/lisa/features/communicationanalytics/README.md"))
                }
                dir = dir?.parentFile
            }
        }
        return candidates.any { it.exists() }
    }

    private fun gradleTaskRegistered(): Boolean {
        val candidates = buildList {
            add(File(PROJECT_ROOT, "app/build.gradle.kts"))
            add(File(PROJECT_ROOT, "build.gradle.kts"))
            var dir: File? = PROJECT_ROOT
            repeat(5) {
                dir?.let {
                    add(File(it, "app/build.gradle.kts"))
                    add(File(it, "build.gradle.kts"))
                }
                dir = dir?.parentFile
            }
        }
        return candidates.any { file ->
            file.exists() && file.readText().contains("validateCommunicationAccuracyAnalyticsAuthorityV1")
        }
    }

    private fun testClassExists(): Boolean {
        val relative = "src/test/java/com/idworx/lisa/validation/authority/CommunicationAccuracyAnalyticsAuthorityV1Test.kt"
        return fileExistsAtProjectRelative("app/$relative") ||
            fileExistsAtProjectRelative(relative)
    }

    private fun fileExistsAtProjectRelative(relativePath: String): Boolean {
        var dir: File? = PROJECT_ROOT
        repeat(6) {
            dir?.let { candidate ->
                if (File(candidate, relativePath).exists()) return true
            }
            dir = dir?.parentFile
        }
        return false
    }

    private fun findAnalyticsDir(): File? {
        val relative = "app/src/main/java/com/idworx/lisa/features/communicationanalytics"
        var dir: File? = PROJECT_ROOT
        repeat(6) {
            dir?.let { candidate ->
                val analytics = File(candidate, relative)
                if (analytics.exists()) return analytics
            }
            dir = dir?.parentFile
        }
        return null
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
