package com.idworx.lisa.features.guidedprogresswordingpolish.validation

import com.idworx.lisa.features.guidedprogresswordingpolish.audit.GuidedProgressWordingPolishAuditor
import com.idworx.lisa.features.guidedprogresswordingpolish.metadata.GuidedProgressWordingPolishMetadata
import com.idworx.lisa.validation.ValidationCheckResult
import com.idworx.lisa.validation.ValidationOutcome
import com.idworx.lisa.validation.ValidationReport

object GuidedProgressWordingPolishAuthorityV1 {

    const val AUTHORITY_NAME: String = "LISA_GUIDED_PROGRESS_WORDING_POLISH_V1"
    const val PASS_TOKEN: String = GuidedProgressWordingPolishMetadata.PASS_TOKEN

    fun validate(): ValidationReport {
        val checks = listOf(
            check("GPWP_001", "Progress displays \"X of Y blinks\" wording", GuidedProgressWordingPolishAuditor.progressDisplaysXOfYBlinksWording()),
            check("GPWP_002", "Total blink count remains correct", GuidedProgressWordingPolishAuditor.totalBlinkCountRemainsCorrect()),
            check("GPWP_003", "Progress increments correctly after every accepted blink", GuidedProgressWordingPolishAuditor.progressIncrementsCorrectlyAfterEveryAcceptedBlink()),
            check("GPWP_004", "Waiting label remains unchanged", GuidedProgressWordingPolishAuditor.waitingLabelRemainsUnchanged()),
            check("GPWP_005", "Sequence complete still appears after the final blink", GuidedProgressWordingPolishAuditor.sequenceCompleteStillAppearsAfterFinalBlink()),
            check("GPWP_006", "Existing Guided Learning validations remain green", GuidedProgressWordingPolishAuditor.existingGuidedLearningValidationsRemainGreen()),
            check("GPWP_007", "Tests pass and Gradle validation task defined", GuidedProgressWordingPolishAuditor.testClassExists() && GuidedProgressWordingPolishAuditor.gradleTaskRegistered())
        )

        val failed = checks.filter { !it.passed }
        val outcome = ValidationReport.resolveOutcome(checks)
        return ValidationReport(
            authorityName = AUTHORITY_NAME,
            outcome = outcome,
            passToken = if (outcome == ValidationOutcome.PASS) PASS_TOKEN else null,
            evidenceSummary = "Guided Learning progress wording polish verified ${checks.size} checks. Passed: ${checks.count { it.passed }}. Failed: ${failed.size}.",
            checksPerformed = checks.map { "${it.checkId}: ${it.description}" },
            failedChecks = failed.map { "${it.checkId}: ${it.description}" },
            observations = listOf(GuidedProgressWordingPolishMetadata.WORDING_RULE),
            affectedLicArticles = listOf("Part II — Guided Learning progress feedback wording"),
            affectedLiecArticles = listOf("Article 2.16 — Progress wording counts total blinks, not gesture steps"),
            affectedLvcArticles = listOf("Article 3.30 — Guided Progress Wording Polish validation"),
            remediationGuidance = failed.mapNotNull { it.remediation }.distinct(),
            checkResults = checks,
            rootCause = failed.firstOrNull()?.let { "${it.checkId} — ${it.description}" },
            validationReasoning = if (outcome == ValidationOutcome.PASS) {
                "Detected Progress now reads \"X of Y blinks\" (e.g. \"1 of 4 blinks\") instead of " +
                    "\"Blink X of Y\", with the same total, same per-blink increments, the same " +
                    "\"Waiting for:\" eye label, and the same \"\u2713 Sequence complete\" completion " +
                    "state — a pure wording change with no regressions to existing Guided Learning " +
                    "behavior."
            } else {
                "${failed.size} Guided Progress Wording Polish checks failed."
            },
            subsystem = "Guided Progress Wording Polish"
        )
    }

    private fun check(id: String, description: String, passed: Boolean, remediation: String? = null) =
        ValidationCheckResult(checkId = id, description = description, passed = passed, remediation = remediation)
}
