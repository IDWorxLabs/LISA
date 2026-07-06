package com.idworx.lisa.features.experiencepolish.guidedlearningsimplification.validation

import com.idworx.lisa.features.experiencepolish.guidedlearningsimplification.audit.GuidedLearningSimplificationAuditor
import com.idworx.lisa.features.experiencepolish.guidedlearningsimplification.metadata.GuidedLearningSimplificationMetadata
import com.idworx.lisa.validation.ValidationCheckResult
import com.idworx.lisa.validation.ValidationOutcome
import com.idworx.lisa.validation.ValidationReport

object GuidedLearningSimplificationAuthorityV1 {

    const val AUTHORITY_NAME: String = "LISA_GUIDED_LEARNING_SIMPLIFICATION_V1"
    const val PASS_TOKEN: String = GuidedLearningSimplificationMetadata.PASS_TOKEN

    fun validate(): ValidationReport {
        val checks = listOf(
            check("GLS_001", "Guided Learning simplification documented", GuidedLearningSimplificationAuditor.phaseDocumented()),
            check("GLS_002", "First launch always reaches Welcome to Lisa", GuidedLearningSimplificationAuditor.firstLaunchEnforced()),
            check("GLS_003", "Welcome screen calm and minimal", GuidedLearningSimplificationAuditor.welcomeScreenSimplified()),
            check("GLS_004", "Lesson screen shows phrase, gesture, instruction only", GuidedLearningSimplificationAuditor.lessonScreenMinimal()),
            check("GLS_005", "No dashboard clutter in training flow", GuidedLearningSimplificationAuditor.noLessonDashboardInFlow()),
            check("GLS_006", "Friendly gesture labels not L/R codes", GuidedLearningSimplificationAuditor.friendlyGestureLabels()),
            check("GLS_007", "Workspace transition dialogues defined", GuidedLearningSimplificationAuditor.workspaceTransitionDialogues()),
            check("GLS_008", "Completion screen simplified for workspace handoff", GuidedLearningSimplificationAuditor.completionScreenSimplified()),
            check("GLS_009", "Completion narration uses workspace transition", GuidedLearningSimplificationAuditor.completionNarrationWired()),
            check("GLS_010", "Tests pass and Gradle validation task defined", GuidedLearningSimplificationAuditor.testClassExists() && GuidedLearningSimplificationAuditor.gradleTaskRegistered())
        )

        val failed = checks.filter { !it.passed }
        val outcome = ValidationReport.resolveOutcome(checks)
        return ValidationReport(
            authorityName = AUTHORITY_NAME,
            outcome = outcome,
            passToken = if (outcome == ValidationOutcome.PASS) PASS_TOKEN else null,
            evidenceSummary = "Guided Learning Simplification verified ${checks.size} checks. Passed: ${checks.count { it.passed }}. Failed: ${failed.size}.",
            checksPerformed = checks.map { "${it.checkId}: ${it.description}" },
            failedChecks = failed.map { "${it.checkId}: ${it.description}" },
            observations = listOf(
                GuidedLearningSimplificationMetadata.SIMPLIFICATION_PHILOSOPHY,
                GuidedLearningSimplificationMetadata.CLUTTER_RULE
            ),
            affectedLicArticles = listOf("Part II — Guided Learning simplification"),
            affectedLiecArticles = listOf("Article 2.3 — Lisa teaches, user does not manage"),
            affectedLvcArticles = listOf("Article 3.13 — Guided Learning Simplification validation"),
            remediationGuidance = failed.mapNotNull { it.remediation }.distinct(),
            checkResults = checks,
            rootCause = failed.firstOrNull()?.let { "${it.checkId} — ${it.description}" },
            validationReasoning = if (outcome == ValidationOutcome.PASS) {
                "Guided Learning feels like Lisa personally teaching — minimal UI, automatic progression."
            } else {
                "${failed.size} Guided Learning Simplification checks failed."
            },
            subsystem = "Guided Learning Simplification"
        )
    }

    private fun check(id: String, description: String, passed: Boolean, remediation: String? = null) =
        ValidationCheckResult(checkId = id, description = description, passed = passed, remediation = remediation)
}
