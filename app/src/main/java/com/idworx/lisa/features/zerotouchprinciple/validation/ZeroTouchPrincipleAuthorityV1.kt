package com.idworx.lisa.features.zerotouchprinciple.validation

import com.idworx.lisa.features.zerotouchprinciple.audit.ZeroTouchAuditor
import com.idworx.lisa.features.zerotouchprinciple.experience.FirstConversationExperience
import com.idworx.lisa.features.zerotouchprinciple.metadata.ZeroTouchPrincipleMetadata
import com.idworx.lisa.validation.ValidationCheckResult
import com.idworx.lisa.validation.ValidationOutcome
import com.idworx.lisa.validation.ValidationReport
import java.io.File

object ZeroTouchPrincipleAuthorityV1 {

    const val AUTHORITY_NAME: String = "ZERO_TOUCH_PRINCIPLE_AUTHORITY_V1"
    const val PASS_TOKEN: String = "ZERO_TOUCH_PRINCIPLE_AUTHORITY_V1_PASS"

    private val PROJECT_ROOT = File(System.getProperty("user.dir") ?: ".")

    fun validate(): ValidationReport {
        val checks = listOf(
            check("ZTP_001", "Zero-Touch Principle documented", readmeExists()),
            check("ZTP_002", "Brain 1 constitutional rule exists", ZeroTouchAuditor.constitutionalRuleDocumented()),
            check("ZTP_003", "First Conversation replaces passive welcome screen", ZeroTouchAuditor.welcomeIsConversational()),
            check("ZTP_004", "Lisa introduces herself automatically", ZeroTouchAuditor.autoIntroductionOnWelcome() && ZeroTouchAuditor.firstConversationDialogueExists()),
            check("ZTP_005", "First launch begins without unnecessary touch interaction", ZeroTouchAuditor.welcomeIsConversational() && ZeroTouchAuditor.narrationSequenceWiredInMainActivity()),
            check("ZTP_006", "Camera setup is conversational", ZeroTouchAuditor.cameraSetupIsConversational()),
            check("ZTP_007", "Calibration is conversational", ZeroTouchAuditor.calibrationIntegrationReused()),
            check("ZTP_008", "Communication lessons are conversational", ZeroTouchAuditor.communicationLessonsAutoNarrate()),
            check("ZTP_009", "Navigation lessons remain eye controlled", ZeroTouchAuditor.navigationLessonsEyeControlled()),
            check("ZTP_010", "No Tap Continue assumptions remain in Guided Learning", ZeroTouchAuditor.noTapContinueInGuidedLearning()),
            check("ZTP_011", "Personality Engine drives onboarding dialogue", ZeroTouchAuditor.personalityDrivesOnboarding()),
            check("ZTP_012", "Companion Memory personalizes returning users", ZeroTouchAuditor.companionMemoryPersonalizesReturning() && ZeroTouchAuditor.returningUserWired()),
            check("ZTP_013", "Waiting behavior supports unlimited patience", ZeroTouchAuditor.waitingDialogueExists() && ZeroTouchAuditor.narrationAutoAdvanceExists()),
            check("ZTP_014", "User is never blamed for failed attempts", ZeroTouchAuditor.noUserBlameInCatalog()),
            check("ZTP_015", "Caregiver workflow minimized", ZeroTouchAuditor.caregiverWorkflowMinimized()),
            check("ZTP_016", "Existing Brain 1 systems reused", ZeroTouchAuditor.existingSystemsReused()),
            check("ZTP_017", "No Brain 2 dependency introduced", ZeroTouchAuditor.noBrain2Dependency()),
            check("ZTP_018", "No LLM dependency introduced", ZeroTouchAuditor.noLlmDependency()),
            check("ZTP_019", "No cloud dependency introduced", ZeroTouchAuditor.noCloudDependency()),
            check("ZTP_020", "Documentation completed", readmeExists() && classExists(ZeroTouchPrincipleMetadata::class.java) && ZeroTouchAuditor.firstConversationExperienceModuleExists()),
            check("ZTP_021", "Tests exist", testClassExists()),
            check("ZTP_022", "Pass token and Gradle validation task defined", PASS_TOKEN == "ZERO_TOUCH_PRINCIPLE_AUTHORITY_V1_PASS" && gradleTaskRegistered())
        )

        val failed = checks.filter { !it.passed }
        val outcome = ValidationReport.resolveOutcome(checks)
        return ValidationReport(
            authorityName = AUTHORITY_NAME,
            outcome = outcome,
            passToken = if (outcome == ValidationOutcome.PASS) PASS_TOKEN else null,
            evidenceSummary = "Zero-Touch Principle verified ${checks.size} checks. Passed: ${checks.count { it.passed }}. Failed: ${failed.size}.",
            checksPerformed = checks.map { "${it.checkId}: ${it.description}" },
            failedChecks = failed.map { "${it.checkId}: ${it.description}" },
            observations = listOfNotNull(
                ZeroTouchPrincipleMetadata.CORE_RULE,
                "First conversation stages: ${FirstConversationExperience.Stage.entries.joinToString { it.name }}"
            ),
            affectedLicArticles = listOf("Part I — Brain 1 is a communication partner, not an application"),
            affectedLiecArticles = listOf("Article 1.0 — Zero-Touch Principle governs all Brain 1 interaction"),
            affectedLvcArticles = listOf("Article 3.7.1.1 — Zero-Touch Principle validation"),
            remediationGuidance = failed.mapNotNull { it.remediation }.distinct(),
            checkResults = checks,
            rootCause = failed.firstOrNull()?.let { "${it.checkId} — ${it.description}" },
            validationReasoning = if (outcome == ValidationOutcome.PASS) {
                "LISA Zero-Touch Principle V1 establishes conversation-driven Brain 1 interaction without touch assumptions after caregiver setup."
            } else {
                "${failed.size} Zero-Touch Principle checks failed."
            },
            subsystem = "Zero-Touch Principle"
        )
    }

    private fun readmeExists(): Boolean {
        val candidates = buildList {
            add(File(PROJECT_ROOT, "features/zero-touch-principle/README.md"))
            add(File(PROJECT_ROOT, "app/src/main/java/com/idworx/lisa/features/zerotouchprinciple/README.md"))
            var dir: File? = PROJECT_ROOT
            repeat(5) {
                dir?.let {
                    add(File(it, "features/zero-touch-principle/README.md"))
                    add(File(it, "app/src/main/java/com/idworx/lisa/features/zerotouchprinciple/README.md"))
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
        return candidates.any { it.exists() && it.readText().contains("validateZeroTouchPrincipleAuthorityV1") }
    }

    private fun testClassExists(): Boolean =
        fileExistsAtProjectRelative("app/src/test/java/com/idworx/lisa/validation/authority/ZeroTouchPrincipleAuthorityV1Test.kt")

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
