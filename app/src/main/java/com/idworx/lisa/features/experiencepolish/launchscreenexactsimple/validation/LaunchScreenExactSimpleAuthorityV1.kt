package com.idworx.lisa.features.experiencepolish.launchscreenexactsimple.validation

import com.idworx.lisa.features.experiencepolish.launchscreenexactsimple.audit.LaunchScreenExactSimpleAuditor
import com.idworx.lisa.features.experiencepolish.launchscreenexactsimple.metadata.LaunchScreenExactSimpleMetadata
import com.idworx.lisa.validation.ValidationCheckResult
import com.idworx.lisa.validation.ValidationOutcome
import com.idworx.lisa.validation.ValidationReport

object LaunchScreenExactSimpleAuthorityV1 {

    const val AUTHORITY_NAME: String = "LISA_LAUNCH_SCREEN_EXACT_SIMPLE_V1"
    const val PASS_TOKEN: String = LaunchScreenExactSimpleMetadata.PASS_TOKEN

    fun validate(): ValidationReport {
        val checks = listOf(
            check("LSES_001", "Launch screen exact simple documented", LaunchScreenExactSimpleAuditor.phaseDocumented()),
            check("LSES_002", "First launch always reaches Welcome to Lisa", LaunchScreenExactSimpleAuditor.firstLaunchEnforced()),
            check("LSES_003", "Launch screen matches exact card layout", LaunchScreenExactSimpleAuditor.launchScreenLayoutExact()),
            check("LSES_004", "No gesture labels on launch screen", LaunchScreenExactSimpleAuditor.noGestureLabelsOnLaunch()),
            check("LSES_005", "No progress or clutter on launch screen", LaunchScreenExactSimpleAuditor.noLaunchClutter()),
            check("LSES_006", "Start Guided Learning routes to teaching path", LaunchScreenExactSimpleAuditor.startRoutesToTeaching()),
            check("LSES_007", "Skip routes to Communication Workspace", LaunchScreenExactSimpleAuditor.skipRoutesToWorkspace()),
            check("LSES_008", "Tests pass and Gradle validation task defined", LaunchScreenExactSimpleAuditor.testClassExists() && LaunchScreenExactSimpleAuditor.gradleTaskRegistered())
        )

        val failed = checks.filter { !it.passed }
        val outcome = ValidationReport.resolveOutcome(checks)
        return ValidationReport(
            authorityName = AUTHORITY_NAME,
            outcome = outcome,
            passToken = if (outcome == ValidationOutcome.PASS) PASS_TOKEN else null,
            evidenceSummary = "Launch Screen Exact Simple verified ${checks.size} checks. Passed: ${checks.count { it.passed }}. Failed: ${failed.size}.",
            checksPerformed = checks.map { "${it.checkId}: ${it.description}" },
            failedChecks = failed.map { "${it.checkId}: ${it.description}" },
            observations = listOf(LaunchScreenExactSimpleMetadata.DESIGN_RULE),
            affectedLicArticles = listOf("Part II — First launch experience"),
            affectedLiecArticles = listOf("Article 2.1 — Welcome before Guided Learning or Workspace"),
            affectedLvcArticles = listOf("Article 3.14 — Launch Screen Exact Simple validation"),
            remediationGuidance = failed.mapNotNull { it.remediation }.distinct(),
            checkResults = checks,
            rootCause = failed.firstOrNull()?.let { "${it.checkId} — ${it.description}" },
            validationReasoning = if (outcome == ValidationOutcome.PASS) {
                "First launch shows the exact simple Welcome to Lisa screen with correct routing."
            } else {
                "${failed.size} Launch Screen Exact Simple checks failed."
            },
            subsystem = "Launch Screen Exact Simple"
        )
    }

    private fun check(id: String, description: String, passed: Boolean, remediation: String? = null) =
        ValidationCheckResult(checkId = id, description = description, passed = passed, remediation = remediation)
}
