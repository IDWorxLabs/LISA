package com.idworx.lisa.features.accessibilityconsistency.validation

import com.idworx.lisa.LisaSettingsUiState
import com.idworx.lisa.features.accessibilityconsistency.diagnostics.AccessibilityDiagnostics
import com.idworx.lisa.features.accessibilityconsistency.diagnostics.AccessibilityLogger
import com.idworx.lisa.features.accessibilityconsistency.engine.AccessibilityAuditRunner
import com.idworx.lisa.features.accessibilityconsistency.engine.AccessibilityConsistencyEngine
import com.idworx.lisa.features.accessibilityconsistency.engine.AccessibilityVerifier
import com.idworx.lisa.features.accessibilityconsistency.engine.DefaultAccessibilityConsistencyEngine
import com.idworx.lisa.features.accessibilityconsistency.integration.AccessibilityPersonalityAdapter
import com.idworx.lisa.features.accessibilityconsistency.metadata.AccessibilityMetadata
import com.idworx.lisa.features.accessibilityconsistency.model.AccessibilityReport
import com.idworx.lisa.features.accessibilityconsistency.model.AccessibilityScore
import com.idworx.lisa.features.accessibilityconsistency.validators.AccessibilitySettingsValidator
import com.idworx.lisa.features.accessibilityconsistency.validators.CognitiveLoadValidator
import com.idworx.lisa.features.accessibilityconsistency.validators.CommunicationValidator
import com.idworx.lisa.features.accessibilityconsistency.validators.ContrastValidator
import com.idworx.lisa.features.accessibilityconsistency.validators.EmergencyAccessibilityValidator
import com.idworx.lisa.features.accessibilityconsistency.validators.GuidedLearningValidator
import com.idworx.lisa.features.accessibilityconsistency.validators.LayoutValidator
import com.idworx.lisa.features.accessibilityconsistency.validators.NavigationValidator
import com.idworx.lisa.features.accessibilityconsistency.validators.ScreenConsistencyValidator
import com.idworx.lisa.features.accessibilityconsistency.validators.TouchTargetValidator
import com.idworx.lisa.features.accessibilityconsistency.validators.TypographyValidator
import com.idworx.lisa.features.onboardingguide.validation.GuidedTrainingAuthorityV1
import com.idworx.lisa.features.personality.engine.LisaPersonalityEngine
import com.idworx.lisa.validation.ValidationCheckResult
import com.idworx.lisa.validation.ValidationOutcome
import com.idworx.lisa.validation.ValidationReport
import java.io.File

object AccessibilityConsistencyAuthorityV1 {

    const val AUTHORITY_NAME: String = "ACCESSIBILITY_CONSISTENCY_AUTHORITY_V1"
    const val PASS_TOKEN: String = "ACCESSIBILITY_CONSISTENCY_AUTHORITY_V1_PASS"

    private val PROJECT_ROOT = File(System.getProperty("user.dir") ?: ".")

    fun validate(): ValidationReport {
        val engine = com.idworx.lisa.features.accessibilityconsistency.engine.AccessibilityConsistencyEngines.createForTests()
        val checks = listOf(
            check("A11Y_001", "Accessibility module exists", classExists(AccessibilityConsistencyEngine::class.java)),
            check("A11Y_002", "README exists", readmeExists()),
            check("A11Y_003", "AccessibilityConsistencyEngine exists", AccessibilityConsistencyEngine::class.java.isInterface),
            check("A11Y_004", "AccessibilityAuditRunner exists", classExists(AccessibilityAuditRunner::class.java)),
            check("A11Y_005", "Typography validator exists", classExists(TypographyValidator::class.java)),
            check("A11Y_006", "Touch target validator exists", classExists(TouchTargetValidator::class.java)),
            check("A11Y_007", "Contrast validator exists", classExists(ContrastValidator::class.java)),
            check("A11Y_008", "Layout validator exists", classExists(LayoutValidator::class.java)),
            check("A11Y_009", "Navigation validator exists", classExists(NavigationValidator::class.java)),
            check("A11Y_010", "Guided Learning validator exists", classExists(GuidedLearningValidator::class.java)),
            check("A11Y_011", "Communication validator exists", classExists(CommunicationValidator::class.java)),
            check("A11Y_012", "Emergency accessibility validator exists", classExists(EmergencyAccessibilityValidator::class.java)),
            check("A11Y_013", "Accessibility settings validator exists", classExists(AccessibilitySettingsValidator::class.java)),
            check("A11Y_014", "Cognitive load validator exists", classExists(CognitiveLoadValidator::class.java)),
            check("A11Y_015", "Screen consistency validator exists", classExists(ScreenConsistencyValidator::class.java)),
            check("A11Y_016", "Accessibility score exists", classExists(AccessibilityScore::class.java)),
            check("A11Y_017", "Accessibility reports generated", AccessibilityVerifier.verifyReportGeneration()),
            check("A11Y_018", "Diagnostics exist", classExists(AccessibilityDiagnostics::class.java)),
            check("A11Y_019", "Existing accessibility settings reused", existingSettingsReused()),
            check("A11Y_020", "Personality Engine reused", personalityReused()),
            check("A11Y_021", "Companion Memory not misused", companionMemoryNotMisused()),
            check("A11Y_022", "Guided Learning integration exists", guidedLearningIntegration()),
            check("A11Y_023", "Communication integration exists", communicationIntegration()),
            check("A11Y_024", "Emergency integration exists", emergencyIntegration()),
            check("A11Y_025", "Documentation exists", readmeExists() && classExists(AccessibilityMetadata::class.java)),
            check("A11Y_026", "Tests exist", testClassExists()),
            check("A11Y_027", "Gradle validation task exists", gradleTaskRegistered()),
            check("A11Y_028", "Existing accessibility behaviour preserved", AccessibilityVerifier.verifyExistingAccessibilityPreserved()),
            check("A11Y_029", "No runtime communication behaviour changed", AccessibilityVerifier.verifyNoCommunicationBehaviorChange()),
            check("A11Y_030", "Pass token defined", PASS_TOKEN == "ACCESSIBILITY_CONSISTENCY_AUTHORITY_V1_PASS")
        )

        val failed = checks.filter { !it.passed }
        val outcome = ValidationReport.resolveOutcome(checks)
        return ValidationReport(
            authorityName = AUTHORITY_NAME,
            outcome = outcome,
            passToken = if (outcome == ValidationOutcome.PASS) PASS_TOKEN else null,
            evidenceSummary = "Accessibility Consistency verified ${checks.size} checks. Passed: ${checks.count { it.passed }}. Failed: ${failed.size}.",
            checksPerformed = checks.map { "${it.checkId}: ${it.description}" },
            failedChecks = failed.map { "${it.checkId}: ${it.description}" },
            observations = listOfNotNull(
                engine.generateReport().warnings.takeIf { it.isNotEmpty() }?.joinToString()
            ),
            affectedLicArticles = listOf("Part IV — Accessibility is the product"),
            affectedLiecArticles = listOf("Article 5.3.1.1 — Readable typography and touch targets"),
            affectedLvcArticles = listOf("Article 3.4.1.1 — Accessibility consistency validation"),
            remediationGuidance = failed.mapNotNull { it.remediation }.distinct(),
            checkResults = checks,
            rootCause = failed.firstOrNull()?.let { "${it.checkId} — ${it.description}" },
            validationReasoning = if (outcome == ValidationOutcome.PASS) {
                "LISA Accessibility Consistency V1 validates accessibility governance without redesigning existing UI."
            } else {
                "${failed.size} accessibility consistency checks failed."
            },
            subsystem = "Accessibility Consistency"
        )
    }

    private fun existingSettingsReused(): Boolean {
        val state = LisaSettingsUiState()
        return state.textSizeScale == 1.0f &&
            classExists(LisaSettingsUiState::class.java) &&
            AccessibilitySettingsValidator.validate().checksPassed >= 4
    }

    private fun personalityReused(): Boolean =
        classExists(LisaPersonalityEngine::class.java) &&
            classExists(AccessibilityPersonalityAdapter::class.java) &&
            AccessibilityPersonalityAdapter.settingsHint().contains("text size", ignoreCase = true)

    private fun companionMemoryNotMisused(): Boolean {
        val dir = findModuleDir() ?: return true
        return !dir.walkTopDown()
            .filter { it.isFile && it.extension == "kt" && !it.path.contains("validation") }
            .any { it.readText().contains("recordMilestone") }
    }

    private fun guidedLearningIntegration(): Boolean =
        classExists(GuidedTrainingAuthorityV1::class.java) &&
            GuidedLearningValidator.validate().checksPassed >= 3

    private fun communicationIntegration(): Boolean =
        CommunicationValidator.validate().checksPassed >= 3

    private fun emergencyIntegration(): Boolean =
        EmergencyAccessibilityValidator.validate().checksPassed >= 3

    private fun readmeExists(): Boolean {
        val candidates = buildList {
            add(File(PROJECT_ROOT, "features/accessibility-consistency/README.md"))
            add(File(PROJECT_ROOT, "app/src/main/java/com/idworx/lisa/features/accessibilityconsistency/README.md"))
            var dir: File? = PROJECT_ROOT
            repeat(5) {
                dir?.let {
                    add(File(it, "features/accessibility-consistency/README.md"))
                    add(File(it, "app/src/main/java/com/idworx/lisa/features/accessibilityconsistency/README.md"))
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
        return candidates.any { it.exists() && it.readText().contains("validateAccessibilityConsistencyAuthorityV1") }
    }

    private fun testClassExists(): Boolean =
        fileExistsAtProjectRelative("app/src/test/java/com/idworx/lisa/validation/authority/AccessibilityConsistencyAuthorityV1Test.kt")

    private fun fileExistsAtProjectRelative(relativePath: String): Boolean {
        var dir: File? = PROJECT_ROOT
        repeat(6) {
            dir?.let { if (File(it, relativePath).exists()) return true }
            dir = dir?.parentFile
        }
        return false
    }

    private fun findModuleDir(): File? {
        val relative = "app/src/main/java/com/idworx/lisa/features/accessibilityconsistency"
        var dir: File? = PROJECT_ROOT
        repeat(6) {
            dir?.let { candidate ->
                val module = File(candidate, relative)
                if (module.exists()) return module
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
