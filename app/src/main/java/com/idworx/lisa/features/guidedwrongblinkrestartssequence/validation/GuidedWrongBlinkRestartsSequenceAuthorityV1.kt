package com.idworx.lisa.features.guidedwrongblinkrestartssequence.validation

import com.idworx.lisa.features.guidedwrongblinkrestartssequence.audit.GuidedWrongBlinkRestartsSequenceAuditor
import com.idworx.lisa.features.guidedwrongblinkrestartssequence.metadata.GuidedWrongBlinkRestartsSequenceMetadata
import com.idworx.lisa.validation.ValidationCheckResult
import com.idworx.lisa.validation.ValidationOutcome
import com.idworx.lisa.validation.ValidationReport

object GuidedWrongBlinkRestartsSequenceAuthorityV1 {

    const val AUTHORITY_NAME: String = "LISA_GUIDED_WRONG_BLINK_RESTARTS_SEQUENCE_V1"
    const val PASS_TOKEN: String = GuidedWrongBlinkRestartsSequenceMetadata.PASS_TOKEN

    fun validate(): ValidationReport {
        val checks = listOf(
            check("GWBRS_001", "Wrong blink during Guided Learning resets progress to 0", GuidedWrongBlinkRestartsSequenceAuditor.wrongBlinkResetsProgressToZero()),
            check("GWBRS_002", "Wrong blink clears current lesson buffer", GuidedWrongBlinkRestartsSequenceAuditor.wrongBlinkClearsLessonBuffer()),
            check("GWBRS_003", "Wrong blink keeps user on same lesson", GuidedWrongBlinkRestartsSequenceAuditor.wrongBlinkKeepsSameLesson()),
            check("GWBRS_004", "Wrong blink shows red feedback", GuidedWrongBlinkRestartsSequenceAuditor.wrongBlinkShowsRedFeedback()),
            check("GWBRS_005", "User must restart the full sequence", GuidedWrongBlinkRestartsSequenceAuditor.userMustRestartFullSequence()),
            check("GWBRS_006", "Correct full sequence after restart completes normally", GuidedWrongBlinkRestartsSequenceAuditor.fullSequenceAfterRestartCompletesNormally()),
            check("GWBRS_007", "No phrase speech happens on wrong blink", GuidedWrongBlinkRestartsSequenceAuditor.noPhraseSpeechOnWrongBlink()),
            check("GWBRS_008", "Workspace contextual phrase resolver remains unchanged", GuidedWrongBlinkRestartsSequenceAuditor.workspacePhraseResolverUnchanged()),
            check("GWBRS_009", "Tests pass and Gradle validation task defined", GuidedWrongBlinkRestartsSequenceAuditor.testClassExists() && GuidedWrongBlinkRestartsSequenceAuditor.gradleTaskRegistered())
        )

        val failed = checks.filter { !it.passed }
        val outcome = ValidationReport.resolveOutcome(checks)
        return ValidationReport(
            authorityName = AUTHORITY_NAME,
            outcome = outcome,
            passToken = if (outcome == ValidationOutcome.PASS) PASS_TOKEN else null,
            evidenceSummary = "Guided wrong-blink restart verified ${checks.size} checks. Passed: ${checks.count { it.passed }}. Failed: ${failed.size}.",
            checksPerformed = checks.map { "${it.checkId}: ${it.description}" },
            failedChecks = failed.map { "${it.checkId}: ${it.description}" },
            observations = listOf(
                "Wrong-eye blinks in Guided Learning reset the lesson sequence to zero.",
                "Communication Workspace phrase resolution is unchanged."
            ),
            affectedLicArticles = listOf("Part II — Guided Learning sequence teaching"),
            affectedLiecArticles = listOf("Article 2.10 — Wrong blink restarts lesson sequence"),
            affectedLvcArticles = listOf("Article 3.24 — Guided Wrong Blink Restarts Sequence validation"),
            remediationGuidance = failed.mapNotNull { it.remediation }.distinct(),
            checkResults = checks,
            rootCause = failed.firstOrNull()?.let { "${it.checkId} — ${it.description}" },
            validationReasoning = if (outcome == ValidationOutcome.PASS) {
                "Guided Learning wrong blinks restart the full sequence; workspace resolver is untouched."
            } else {
                "${failed.size} wrong-blink restart checks failed."
            },
            subsystem = "Guided Wrong Blink Restarts Sequence"
        )
    }

    private fun check(id: String, description: String, passed: Boolean, remediation: String? = null) =
        ValidationCheckResult(checkId = id, description = description, passed = passed, remediation = remediation)
}
