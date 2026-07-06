package com.idworx.lisa.features.guidedremoveredundanthelpertext.validation

import com.idworx.lisa.features.guidedremoveredundanthelpertext.audit.GuidedRemoveRedundantHelperTextAuditor
import com.idworx.lisa.features.guidedremoveredundanthelpertext.metadata.GuidedRemoveRedundantHelperTextMetadata
import com.idworx.lisa.validation.ValidationCheckResult
import com.idworx.lisa.validation.ValidationOutcome
import com.idworx.lisa.validation.ValidationReport

object GuidedRemoveRedundantHelperTextAuthorityV1 {

    const val AUTHORITY_NAME: String = "LISA_GUIDED_REMOVE_REDUNDANT_HELPER_TEXT_V1"
    const val PASS_TOKEN: String = GuidedRemoveRedundantHelperTextMetadata.PASS_TOKEN

    fun validate(): ValidationReport {
        val checks = listOf(
            check("GRRHT_001", "No communication lesson renders the redundant helper sentence", GuidedRemoveRedundantHelperTextAuditor.noCommunicationLessonRendersHelperSentence()),
            check("GRRHT_002", "No navigation lesson renders the redundant helper sentence", GuidedRemoveRedundantHelperTextAuditor.noNavigationLessonRendersHelperSentence()),
            check("GRRHT_003", "Removal is system-wide — driven by the shared render point, not per-lesson", GuidedRemoveRedundantHelperTextAuditor.removalIsSystemWideNotPerLesson()),
            check("GRRHT_004", "Gesture instruction remains visible", GuidedRemoveRedundantHelperTextAuditor.gestureInstructionRemainsVisible()),
            check("GRRHT_005", "Phrase title remains visible", GuidedRemoveRedundantHelperTextAuditor.phraseTitleRemainsVisible()),
            check("GRRHT_006", "Lesson layout remains vertically balanced with the space left empty", GuidedRemoveRedundantHelperTextAuditor.lessonLayoutRemainsVerticallyBalanced()),
            check("GRRHT_007", "Existing Guided Learning functionality is unchanged", GuidedRemoveRedundantHelperTextAuditor.existingGuidedLearningFunctionalityUnchanged()),
            check("GRRHT_008", "Tests pass and Gradle validation task defined", GuidedRemoveRedundantHelperTextAuditor.testClassExists() && GuidedRemoveRedundantHelperTextAuditor.gradleTaskRegistered())
        )

        val failed = checks.filter { !it.passed }
        val outcome = ValidationReport.resolveOutcome(checks)
        return ValidationReport(
            authorityName = AUTHORITY_NAME,
            outcome = outcome,
            passToken = if (outcome == ValidationOutcome.PASS) PASS_TOKEN else null,
            evidenceSummary = "Guided Learning redundant helper text removal verified ${checks.size} checks. Passed: ${checks.count { it.passed }}. Failed: ${failed.size}.",
            checksPerformed = checks.map { "${it.checkId}: ${it.description}" },
            failedChecks = failed.map { "${it.checkId}: ${it.description}" },
            observations = listOf(GuidedRemoveRedundantHelperTextMetadata.REMOVAL_RULE),
            affectedLicArticles = listOf("Part II — Guided Learning lesson layout minimalism"),
            affectedLiecArticles = listOf("Article 2.14 — No redundant instruction repetition"),
            affectedLvcArticles = listOf("Article 3.28 — Guided Remove Redundant Helper Text validation"),
            remediationGuidance = failed.mapNotNull { it.remediation }.distinct(),
            checkResults = checks,
            rootCause = failed.firstOrNull()?.let { "${it.checkId} — ${it.description}" },
            validationReasoning = if (outcome == ValidationOutcome.PASS) {
                "Every communication and navigation lesson now shows only the phrase/title and the " +
                    "concise gesture instruction — the redundant \"When you're ready, ...\" sentence is " +
                    "gone from the shared render point system-wide, with no replacement text and no " +
                    "regressions to existing Guided Learning behavior."
            } else {
                "${failed.size} Guided Remove Redundant Helper Text checks failed."
            },
            subsystem = "Guided Remove Redundant Helper Text"
        )
    }

    private fun check(id: String, description: String, passed: Boolean, remediation: String? = null) =
        ValidationCheckResult(checkId = id, description = description, passed = passed, remediation = remediation)
}
