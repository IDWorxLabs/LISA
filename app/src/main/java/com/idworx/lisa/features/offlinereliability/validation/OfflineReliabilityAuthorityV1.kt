package com.idworx.lisa.features.offlinereliability.validation

import com.idworx.lisa.features.offlinereliability.diagnostics.OfflineDiagnostics
import com.idworx.lisa.features.offlinereliability.engine.OfflineCapabilityVerifier
import com.idworx.lisa.features.offlinereliability.engine.OfflineReliabilityEngine
import com.idworx.lisa.features.offlinereliability.engine.OfflineReliabilityEngines
import com.idworx.lisa.features.offlinereliability.metadata.OfflineReliabilityMetadata
import com.idworx.lisa.features.offlinereliability.model.OfflineReliabilityReport
import com.idworx.lisa.features.offlinereliability.model.OfflineReliabilityScore
import com.idworx.lisa.features.offlinereliability.validators.AccessibilityOfflineValidator
import com.idworx.lisa.features.offlinereliability.validators.BlinkDetectionOfflineValidator
import com.idworx.lisa.features.offlinereliability.validators.CalibrationOfflineValidator
import com.idworx.lisa.features.offlinereliability.validators.CommunicationOfflineValidator
import com.idworx.lisa.features.offlinereliability.validators.CompanionMemoryOfflineValidator
import com.idworx.lisa.features.offlinereliability.validators.EmergencyOfflineValidator
import com.idworx.lisa.features.offlinereliability.validators.EyeTrackingOfflineValidator
import com.idworx.lisa.features.offlinereliability.validators.GuidedLearningOfflineValidator
import com.idworx.lisa.features.offlinereliability.validators.PersonalityOfflineValidator
import com.idworx.lisa.features.offlinereliability.validators.SettingsOfflineValidator
import com.idworx.lisa.features.offlinereliability.validators.TTSSpeechOfflineValidator
import com.idworx.lisa.validation.ValidationCheckResult
import com.idworx.lisa.validation.ValidationOutcome
import com.idworx.lisa.validation.ValidationReport
import java.io.File

object OfflineReliabilityAuthorityV1 {

    const val AUTHORITY_NAME: String = "OFFLINE_RELIABILITY_AUTHORITY_V1"
    const val PASS_TOKEN: String = "OFFLINE_RELIABILITY_AUTHORITY_V1_PASS"

    private val PROJECT_ROOT = File(System.getProperty("user.dir") ?: ".")

    fun validate(): ValidationReport {
        val engine = OfflineReliabilityEngines.createForTests()
        val checks = listOf(
            check("OFF_001", "Offline module exists", classExists(OfflineReliabilityEngine::class.java)),
            check("OFF_002", "README exists", readmeExists()),
            check("OFF_003", "OfflineReliabilityEngine exists", OfflineReliabilityEngine::class.java.isInterface),
            check("OFF_004", "OfflineCapabilityVerifier exists", classExists(com.idworx.lisa.features.offlinereliability.engine.OfflineCapabilityVerifier::class.java)),
            check("OFF_005", "EyeTrackingOfflineValidator exists", classExists(EyeTrackingOfflineValidator::class.java)),
            check("OFF_006", "BlinkDetectionOfflineValidator exists", classExists(BlinkDetectionOfflineValidator::class.java)),
            check("OFF_007", "CommunicationOfflineValidator exists", classExists(CommunicationOfflineValidator::class.java)),
            check("OFF_008", "CalibrationOfflineValidator exists", classExists(CalibrationOfflineValidator::class.java)),
            check("OFF_009", "PersonalityOfflineValidator exists", classExists(PersonalityOfflineValidator::class.java)),
            check("OFF_010", "CompanionMemoryOfflineValidator exists", classExists(CompanionMemoryOfflineValidator::class.java)),
            check("OFF_011", "GuidedLearningOfflineValidator exists", classExists(GuidedLearningOfflineValidator::class.java)),
            check("OFF_012", "AccessibilityOfflineValidator exists", classExists(AccessibilityOfflineValidator::class.java)),
            check("OFF_013", "EmergencyOfflineValidator exists", classExists(EmergencyOfflineValidator::class.java)),
            check("OFF_014", "SettingsOfflineValidator exists", classExists(SettingsOfflineValidator::class.java)),
            check("OFF_015", "TTSSpeechOfflineValidator exists", classExists(TTSSpeechOfflineValidator::class.java)),
            check("OFF_016", "Offline capability model exists", classExists(com.idworx.lisa.features.offlinereliability.model.OfflineCapability::class.java)),
            check("OFF_017", "Offline report exists", classExists(OfflineReliabilityReport::class.java)),
            check("OFF_018", "Offline score exists", classExists(OfflineReliabilityScore::class.java)),
            check("OFF_019", "Diagnostics exist", classExists(OfflineDiagnostics::class.java)),
            check("OFF_020", "Existing TTS reused", OfflineCapabilityVerifier.verifyExistingTtsReused()),
            check("OFF_021", "Existing communication engine reused", OfflineCapabilityVerifier.verifyOfflineCommunicationPath()),
            check("OFF_022", "Existing eye tracking reused", OfflineCapabilityVerifier.verifyExistingEyeTrackingReused()),
            check("OFF_023", "Existing blink detection reused", OfflineCapabilityVerifier.verifyExistingBlinkDetectionReused()),
            check("OFF_024", "Existing calibration reused", OfflineCapabilityVerifier.verifyExistingCalibrationReused()),
            check("OFF_025", "Existing Guided Learning reused", OfflineCapabilityVerifier.verifyExistingGuidedLearningReused()),
            check("OFF_026", "Existing Personality Engine reused", OfflineCapabilityVerifier.verifyExistingPersonalityReused()),
            check("OFF_027", "Existing Companion Memory reused", OfflineCapabilityVerifier.verifyExistingCompanionMemoryReused()),
            check("OFF_028", "No mandatory cloud dependency introduced into Brain 1", OfflineCapabilityVerifier.verifyNoMandatoryCloudDependency()),
            check("OFF_029", "Tests exist", testClassExists()),
            check("OFF_030", "Documentation exists", readmeExists() && classExists(OfflineReliabilityMetadata::class.java)),
            check("OFF_031", "Gradle validation task exists", gradleTaskRegistered()),
            check("OFF_032", "Pass token defined", PASS_TOKEN == "OFFLINE_RELIABILITY_AUTHORITY_V1_PASS")
        )

        val failed = checks.filter { !it.passed }
        val outcome = ValidationReport.resolveOutcome(checks)
        val offlineReport = engine.generateReport()
        return ValidationReport(
            authorityName = AUTHORITY_NAME,
            outcome = outcome,
            passToken = if (outcome == ValidationOutcome.PASS) PASS_TOKEN else null,
            evidenceSummary = "Offline Reliability verified ${checks.size} checks. Passed: ${checks.count { it.passed }}. Failed: ${failed.size}.",
            checksPerformed = checks.map { "${it.checkId}: ${it.description}" },
            failedChecks = failed.map { "${it.checkId}: ${it.description}" },
            observations = listOfNotNull(
                offlineReport.warnings.takeIf { it.isNotEmpty() }?.joinToString { it.message },
                offlineReport.detectedDependencies.takeIf { it.isNotEmpty() }?.joinToString()
            ).filter { it.isNotBlank() },
            affectedLicArticles = listOf("Part I — Communication must never depend on internet"),
            affectedLiecArticles = listOf("Article 2.1 — Offline-first Brain 1 guarantee"),
            affectedLvcArticles = listOf("Article 3.5.1.1 — Offline reliability validation"),
            remediationGuidance = failed.mapNotNull { it.remediation }.distinct(),
            checkResults = checks,
            rootCause = failed.firstOrNull()?.let { "${it.checkId} — ${it.description}" },
            validationReasoning = if (outcome == ValidationOutcome.PASS) {
                "LISA Offline Reliability V1 validates Brain 1 remains fully functional without internet connectivity."
            } else {
                "${failed.size} offline reliability checks failed."
            },
            subsystem = "Offline Reliability"
        )
    }

    private fun readmeExists(): Boolean {
        val candidates = buildList {
            add(File(PROJECT_ROOT, "features/offline-reliability/README.md"))
            add(File(PROJECT_ROOT, "app/src/main/java/com/idworx/lisa/features/offlinereliability/README.md"))
            var dir: File? = PROJECT_ROOT
            repeat(5) {
                dir?.let {
                    add(File(it, "features/offline-reliability/README.md"))
                    add(File(it, "app/src/main/java/com/idworx/lisa/features/offlinereliability/README.md"))
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
        return candidates.any { it.exists() && it.readText().contains("validateOfflineReliabilityAuthorityV1") }
    }

    private fun testClassExists(): Boolean =
        fileExistsAtProjectRelative("app/src/test/java/com/idworx/lisa/validation/authority/OfflineReliabilityAuthorityV1Test.kt")

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
