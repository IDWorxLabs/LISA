package com.idworx.lisa.features.guidedtotalsequenceprogress.validation

import com.idworx.lisa.features.guidedtotalsequenceprogress.audit.GuidedTotalSequenceProgressAuditor
import com.idworx.lisa.features.guidedtotalsequenceprogress.metadata.GuidedTotalSequenceProgressMetadata
import com.idworx.lisa.validation.ValidationCheckResult
import com.idworx.lisa.validation.ValidationOutcome
import com.idworx.lisa.validation.ValidationReport

object GuidedTotalSequenceProgressAuthorityV1 {

    const val AUTHORITY_NAME: String = "LISA_GUIDED_TOTAL_SEQUENCE_PROGRESS_V1"
    const val PASS_TOKEN: String = GuidedTotalSequenceProgressMetadata.PASS_TOKEN

    fun validate(): ValidationReport {
        val checks = listOf(
            check("GTSP_001", "Detected Progress uses the total sequence length, not the current eye section", GuidedTotalSequenceProgressAuditor.progressUsesTotalSequenceLength()),
            check("GTSP_002", "Total equals left + right blink requirements", GuidedTotalSequenceProgressAuditor.totalEqualsLeftPlusRightRequirements()),
            check("GTSP_003", "Progress increments on every accepted blink", GuidedTotalSequenceProgressAuditor.progressIncrementsOnEveryAcceptedBlink()),
            check("GTSP_004", "Waiting label still reflects the next expected eye", GuidedTotalSequenceProgressAuditor.waitingLabelReflectsNextExpectedEye()),
            check("GTSP_005", "Final accepted blink shows the complete sequence before lesson completion", GuidedTotalSequenceProgressAuditor.finalAcceptedBlinkShowsCompleteSequence()),
            check("GTSP_006", "Existing Guided Learning validations remain green", GuidedTotalSequenceProgressAuditor.existingGuidedLearningValidationsRemainGreen()),
            check("GTSP_007", "Tests pass and Gradle validation task defined", GuidedTotalSequenceProgressAuditor.testClassExists() && GuidedTotalSequenceProgressAuditor.gradleTaskRegistered())
        )

        val failed = checks.filter { !it.passed }
        val outcome = ValidationReport.resolveOutcome(checks)
        return ValidationReport(
            authorityName = AUTHORITY_NAME,
            outcome = outcome,
            passToken = if (outcome == ValidationOutcome.PASS) PASS_TOKEN else null,
            evidenceSummary = "Guided Learning total sequence progress verified ${checks.size} checks. Passed: ${checks.count { it.passed }}. Failed: ${failed.size}.",
            checksPerformed = checks.map { "${it.checkId}: ${it.description}" },
            failedChecks = failed.map { "${it.checkId}: ${it.description}" },
            observations = listOf(GuidedTotalSequenceProgressMetadata.PROGRESS_RULE),
            affectedLicArticles = listOf("Part II — Guided Learning progress feedback accuracy"),
            affectedLiecArticles = listOf("Article 2.15 — Progress reflects the whole gesture, not a section"),
            affectedLvcArticles = listOf("Article 3.29 — Guided Total Sequence Progress validation"),
            remediationGuidance = failed.mapNotNull { it.remediation }.distinct(),
            checkResults = checks,
            rootCause = failed.firstOrNull()?.let { "${it.checkId} — ${it.description}" },
            validationReasoning = if (outcome == ValidationOutcome.PASS) {
                "Detected Progress now counts every accepted blink across the whole gesture " +
                    "(left + right combined) as \"X of Y blinks\", increments on each accepted blink, " +
                    "keeps the existing \"Waiting for:\" eye label, and shows \"\u2713 Sequence complete\" " +
                    "once the full gesture is matched — with no regressions to existing Guided Learning " +
                    "behavior."
            } else {
                "${failed.size} Guided Total Sequence Progress checks failed."
            },
            subsystem = "Guided Total Sequence Progress"
        )
    }

    private fun check(id: String, description: String, passed: Boolean, remediation: String? = null) =
        ValidationCheckResult(checkId = id, description = description, passed = passed, remediation = remediation)
}
