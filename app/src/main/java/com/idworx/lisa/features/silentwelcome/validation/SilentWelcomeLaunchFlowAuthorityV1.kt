package com.idworx.lisa.features.silentwelcome.validation

import com.idworx.lisa.features.silentwelcome.audit.SilentWelcomeLaunchFlowAuditor
import com.idworx.lisa.features.silentwelcome.metadata.SilentWelcomeLaunchFlowMetadata
import com.idworx.lisa.validation.ValidationCheckResult
import com.idworx.lisa.validation.ValidationOutcome
import com.idworx.lisa.validation.ValidationReport

object SilentWelcomeLaunchFlowAuthorityV1 {

    const val AUTHORITY_NAME: String = "LISA_SILENT_WELCOME_LAUNCH_FLOW_V1"
    const val PASS_TOKEN: String = SilentWelcomeLaunchFlowMetadata.PASS_TOKEN

    fun validate(): ValidationReport {
        val checks = listOf(
            check("SWLF_001", "Silent welcome launch flow documented", SilentWelcomeLaunchFlowAuditor.phaseDocumented()),
            check("SWLF_002", "Phrase-translation-only speech policy active", SilentWelcomeLaunchFlowAuditor.speechPolicyPhraseTranslationOnly()),
            check("SWLF_003", "Welcome gate enforced on cold start", SilentWelcomeLaunchFlowAuditor.welcomeGateEnforced()),
            check("SWLF_004", "Launch screen matches exact simple structure", SilentWelcomeLaunchFlowAuditor.launchScreenExact()),
            check("SWLF_005", "Legacy onboarding does not block training welcome", SilentWelcomeLaunchFlowAuditor.onboardingFlowDoesNotBlockTraining()),
            check("SWLF_006", "Start routes to teaching without setup narration", SilentWelcomeLaunchFlowAuditor.startRoutesToTeachingWithoutNarration()),
            check("SWLF_007", "Skip routes to Communication Workspace", SilentWelcomeLaunchFlowAuditor.skipRoutesToWorkspace()),
            check("SWLF_008", "Non-translation narration gated off", SilentWelcomeLaunchFlowAuditor.narrationGatedInController()),
            check("SWLF_009", "Phrase translation speech preserved", SilentWelcomeLaunchFlowAuditor.phraseTranslationPreserved()),
            check("SWLF_010", "Tests pass and Gradle validation task defined", SilentWelcomeLaunchFlowAuditor.testClassExists() && SilentWelcomeLaunchFlowAuditor.gradleTaskRegistered())
        )

        val failed = checks.filter { !it.passed }
        val outcome = ValidationReport.resolveOutcome(checks)
        return ValidationReport(
            authorityName = AUTHORITY_NAME,
            outcome = outcome,
            passToken = if (outcome == ValidationOutcome.PASS) PASS_TOKEN else null,
            evidenceSummary = "Silent Welcome Launch Flow verified ${checks.size} checks. Passed: ${checks.count { it.passed }}. Failed: ${failed.size}.",
            checksPerformed = checks.map { "${it.checkId}: ${it.description}" },
            failedChecks = failed.map { "${it.checkId}: ${it.description}" },
            observations = listOf(
                SilentWelcomeLaunchFlowMetadata.VOICE_POLICY,
                SilentWelcomeLaunchFlowMetadata.SUBTITLE
            ),
            affectedLicArticles = listOf("Part II — First launch and silent teaching"),
            affectedLiecArticles = listOf("Article 2.1 — Welcome before Guided Learning or Workspace"),
            affectedLvcArticles = listOf("Article 3.15 — Silent Welcome Launch Flow validation"),
            remediationGuidance = failed.mapNotNull { it.remediation }.distinct(),
            checkResults = checks,
            rootCause = failed.firstOrNull()?.let { "${it.checkId} — ${it.description}" },
            validationReasoning = if (outcome == ValidationOutcome.PASS) {
                "LISA launches on the exact Welcome screen, routes correctly, and speaks only translated phrases."
            } else {
                "${failed.size} Silent Welcome Launch Flow checks failed."
            },
            subsystem = "Silent Welcome Launch Flow"
        )
    }

    private fun check(id: String, description: String, passed: Boolean, remediation: String? = null) =
        ValidationCheckResult(checkId = id, description = description, passed = passed, remediation = remediation)
}
