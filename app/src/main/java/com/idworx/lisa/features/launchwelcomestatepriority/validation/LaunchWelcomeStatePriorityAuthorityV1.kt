package com.idworx.lisa.features.launchwelcomestatepriority.validation

import com.idworx.lisa.features.launchwelcomestatepriority.audit.LaunchWelcomeStatePriorityAuditor
import com.idworx.lisa.features.launchwelcomestatepriority.metadata.LaunchWelcomeStatePriorityMetadata
import com.idworx.lisa.validation.ValidationCheckResult
import com.idworx.lisa.validation.ValidationOutcome
import com.idworx.lisa.validation.ValidationReport

object LaunchWelcomeStatePriorityAuthorityV1 {

    const val AUTHORITY_NAME: String = "LISA_LAUNCH_WELCOME_STATE_PRIORITY_V1"
    const val PASS_TOKEN: String = LaunchWelcomeStatePriorityMetadata.PASS_TOKEN

    fun validate(): ValidationReport {
        val checks = listOf(
            check("LWSP_001", "Welcome state priority documented", LaunchWelcomeStatePriorityAuditor.phaseDocumented()),
            check("LWSP_002", "Cold launch gate always routes to Welcome", LaunchWelcomeStatePriorityAuditor.gateUsesFinishedNotFirstLaunchChoice()),
            check("LWSP_003", "Store applies in-memory cold launch gate on load", LaunchWelcomeStatePriorityAuditor.storeAppliesGateOnLoad()),
            check("LWSP_004", "Saved HELLO without finished training returns Welcome", LaunchWelcomeStatePriorityAuditor.runtime_savedHelloWithoutFinishedTraining_returnsWelcome()),
            check("LWSP_005", "guidedTrainingActive without finished training returns Welcome", LaunchWelcomeStatePriorityAuditor.runtime_guidedTrainingActiveWithoutFinished_returnsWelcome()),
            check("LWSP_006", "Old onboarding progress migrates to Welcome", LaunchWelcomeStatePriorityAuditor.runtime_oldOnboardingProgressMigratesToWelcome()),
            check("LWSP_007", "Start Guided Learning required before HELLO can appear", LaunchWelcomeStatePriorityAuditor.runtime_startRequiredBeforeHello()),
            check("LWSP_008", "Skip required before Workspace from Welcome", LaunchWelcomeStatePriorityAuditor.runtime_skipRequiredBeforeWorkspace()),
            check("LWSP_009", "Phrase-only voice policy remains active", LaunchWelcomeStatePriorityAuditor.phraseOnlyVoicePolicyActive()),
            check("LWSP_010", "Tests pass and Gradle validation task defined", LaunchWelcomeStatePriorityAuditor.testClassExists() && LaunchWelcomeStatePriorityAuditor.gradleTaskRegistered())
        )

        val failed = checks.filter { !it.passed }
        val outcome = ValidationReport.resolveOutcome(checks)
        return ValidationReport(
            authorityName = AUTHORITY_NAME,
            outcome = outcome,
            passToken = if (outcome == ValidationOutcome.PASS) PASS_TOKEN else null,
            evidenceSummary = "Launch Welcome State Priority verified ${checks.size} checks. Passed: ${checks.count { it.passed }}. Failed: ${failed.size}.",
            checksPerformed = checks.map { "${it.checkId}: ${it.description}" },
            failedChecks = failed.map { "${it.checkId}: ${it.description}" },
            observations = listOf(LaunchWelcomeStatePriorityMetadata.PRIORITY_RULE),
            affectedLicArticles = listOf("Part II — First launch state priority"),
            affectedLiecArticles = listOf("Article 2.1 — Welcome before Guided Learning or Workspace"),
            affectedLvcArticles = listOf("Article 3.16 — Launch Welcome State Priority validation"),
            remediationGuidance = failed.mapNotNull { it.remediation }.distinct(),
            checkResults = checks,
            rootCause = failed.firstOrNull()?.let { "${it.checkId} — ${it.description}" },
            validationReasoning = if (outcome == ValidationOutcome.PASS) {
                "Welcome wins on every cold launch; saved session state cannot bypass Welcome."
            } else {
                "${failed.size} Launch Welcome State Priority checks failed."
            },
            subsystem = "Launch Welcome State Priority"
        )
    }

    private fun check(id: String, description: String, passed: Boolean, remediation: String? = null) =
        ValidationCheckResult(checkId = id, description = description, passed = passed, remediation = remediation)
}
