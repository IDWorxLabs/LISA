package com.idworx.lisa.features.guidedcurriculumandnavigationcontext.validation

import com.idworx.lisa.features.guidedcurriculumandnavigationcontext.audit.GuidedCurriculumAndNavigationContextAuditor
import com.idworx.lisa.features.guidedcurriculumandnavigationcontext.metadata.GuidedCurriculumAndNavigationContextMetadata
import com.idworx.lisa.validation.ValidationCheckResult
import com.idworx.lisa.validation.ValidationOutcome
import com.idworx.lisa.validation.ValidationReport

object GuidedCurriculumAndNavigationContextAuthorityV1 {

    const val AUTHORITY_NAME: String = "LISA_GUIDED_CURRICULUM_AND_NAVIGATION_CONTEXT_V1"
    const val PASS_TOKEN: String = GuidedCurriculumAndNavigationContextMetadata.PASS_TOKEN

    fun validate(): ValidationReport {
        val checks = listOf(
            check("GCNC_001", "Guided Learning contains 10–15 phrase lessons before navigation", GuidedCurriculumAndNavigationContextAuditor.phraseCurriculumCountInRange()),
            check("GCNC_002", "Essential phrase curriculum matches catalog order", GuidedCurriculumAndNavigationContextAuditor.essentialPhrasesMatchCatalogOrder()),
            check("GCNC_003", "Essential phrase sequences are unique", GuidedCurriculumAndNavigationContextAuditor.essentialPhrasesUnique()),
            check("GCNC_004", "Navigation begins only after phrase curriculum", GuidedCurriculumAndNavigationContextAuditor.navigationBeginsAfterPhraseCurriculum()),
            check("GCNC_005", "Categories gesture during navigation training opens Categories", GuidedCurriculumAndNavigationContextAuditor.categoriesGestureOpensCategoriesDuringNavigationTraining()),
            check("GCNC_006", "Categories gesture is free of any legacy vocabulary phrase collision", GuidedCurriculumAndNavigationContextAuditor.categoriesGestureDoesNotResolveToShadowedLegacyPhrase()),
            check("GCNC_007", "Navigation training blocks workspace phrase resolver", GuidedCurriculumAndNavigationContextAuditor.navigationTrainingBlocksPhraseResolverInMainActivity()),
            check("GCNC_008", "Context priority: navigation lesson before phrase resolver", GuidedCurriculumAndNavigationContextAuditor.contextPriorityEnforced()),
            check("GCNC_009", "Phrase resolver resumes after Guided Learning completes", GuidedCurriculumAndNavigationContextAuditor.phraseResolverResumesAfterTraining()),
            check("GCNC_010", "NavigationTrainingGestureHandler defined", GuidedCurriculumAndNavigationContextAuditor.navigationHandlerExists()),
            check("GCNC_011", "Tests pass and Gradle validation task defined", GuidedCurriculumAndNavigationContextAuditor.testClassExists() && GuidedCurriculumAndNavigationContextAuditor.gradleTaskRegistered())
        )

        val failed = checks.filter { !it.passed }
        val outcome = ValidationReport.resolveOutcome(checks)
        return ValidationReport(
            authorityName = AUTHORITY_NAME,
            outcome = outcome,
            passToken = if (outcome == ValidationOutcome.PASS) PASS_TOKEN else null,
            evidenceSummary = "Guided curriculum and navigation context verified ${checks.size} checks. Passed: ${checks.count { it.passed }}. Failed: ${failed.size}.",
            checksPerformed = checks.map { "${it.checkId}: ${it.description}" },
            failedChecks = failed.map { "${it.checkId}: ${it.description}" },
            observations = listOf(
                GuidedCurriculumAndNavigationContextMetadata.CURRICULUM_RULE,
                GuidedCurriculumAndNavigationContextMetadata.NAVIGATION_CONTEXT_RULE
            ),
            affectedLicArticles = listOf("Part II — Guided Learning curriculum and navigation context"),
            affectedLiecArticles = listOf("Article 2.7 — Navigation lesson gesture ownership"),
            affectedLvcArticles = listOf("Article 3.21 — Guided Curriculum and Navigation Context validation"),
            remediationGuidance = failed.mapNotNull { it.remediation }.distinct(),
            checkResults = checks,
            rootCause = failed.firstOrNull()?.let { "${it.checkId} — ${it.description}" },
            validationReasoning = if (outcome == ValidationOutcome.PASS) {
                "15-phrase beginner curriculum precedes navigation; the shared Categories gesture opens Categories during nav lessons."
            } else {
                "${failed.size} curriculum or navigation context checks failed."
            },
            subsystem = "Guided Curriculum and Navigation Context"
        )
    }

    private fun check(id: String, description: String, passed: Boolean, remediation: String? = null) =
        ValidationCheckResult(id, description, passed, remediation)
}
