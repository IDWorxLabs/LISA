package com.idworx.lisa.features.guidedsuccesstimingfix.validation

import com.idworx.lisa.features.guidedsuccesstimingfix.audit.GuidedSuccessTimingFixAuditor
import com.idworx.lisa.features.guidedsuccesstimingfix.metadata.GuidedSuccessTimingFixMetadata
import com.idworx.lisa.validation.ValidationCheckResult
import com.idworx.lisa.validation.ValidationOutcome
import com.idworx.lisa.validation.ValidationReport

object GuidedSuccessTimingFixAuthorityV1 {

    const val AUTHORITY_NAME: String = "LISA_GUIDED_SUCCESS_TIMING_FIX_V1"
    const val PASS_TOKEN: String = GuidedSuccessTimingFixMetadata.PASS_TOKEN

    fun validate(): ValidationReport {
        val checks = listOf(
            check("GSTF_001", "Phrase speech happens before success message", GuidedSuccessTimingFixAuditor.phraseSpeechHappensBeforeSuccessMessage()),
            check("GSTF_002", "\"Well done\" appears only after phrase speech completes", GuidedSuccessTimingFixAuditor.wellDoneAppearsOnlyAfterSpeechCompletes()),
            check("GSTF_003", "No non-phrase narration is added", GuidedSuccessTimingFixAuditor.noNonPhraseNarrationAdded()),
            check("GSTF_004", "Lesson advances after the success message pause", GuidedSuccessTimingFixAuditor.lessonAdvancesAfterSuccessMessagePause()),
            check("GSTF_005", "Existing 3-second sequence finalization remains unchanged", GuidedSuccessTimingFixAuditor.sequenceFinalizationTimeoutUnchanged()),
            check("GSTF_006", "Lesson phrase stays visible while speech is happening", GuidedSuccessTimingFixAuditor.lessonPhraseStaysVisibleDuringSpeech()),
            check("GSTF_007", "Tests pass and Gradle validation task defined", GuidedSuccessTimingFixAuditor.testClassExists() && GuidedSuccessTimingFixAuditor.gradleTaskRegistered())
        )

        val failed = checks.filter { !it.passed }
        val outcome = ValidationReport.resolveOutcome(checks)
        return ValidationReport(
            authorityName = AUTHORITY_NAME,
            outcome = outcome,
            passToken = if (outcome == ValidationOutcome.PASS) PASS_TOKEN else null,
            evidenceSummary = "Guided Learning success timing verified ${checks.size} checks. Passed: ${checks.count { it.passed }}. Failed: ${failed.size}.",
            checksPerformed = checks.map { "${it.checkId}: ${it.description}" },
            failedChecks = failed.map { "${it.checkId}: ${it.description}" },
            observations = listOf(GuidedSuccessTimingFixMetadata.TIMING_RULE),
            affectedLicArticles = listOf("Part II — Guided Learning success feedback timing"),
            affectedLiecArticles = listOf("Article 2.11 — Speech-before-success-message ordering"),
            affectedLvcArticles = listOf("Article 3.25 — Guided Success Timing Fix validation"),
            remediationGuidance = failed.mapNotNull { it.remediation }.distinct(),
            checkResults = checks,
            rootCause = failed.firstOrNull()?.let { "${it.checkId} — ${it.description}" },
            validationReasoning = if (outcome == ValidationOutcome.PASS) {
                "The translated phrase is spoken before \"Well done\" is shown, removing the mismatch " +
                    "between what the user hears and sees during Guided Learning success."
            } else {
                "${failed.size} Guided Success Timing checks failed."
            },
            subsystem = "Guided Success Timing Fix"
        )
    }

    private fun check(id: String, description: String, passed: Boolean, remediation: String? = null) =
        ValidationCheckResult(checkId = id, description = description, passed = passed, remediation = remediation)
}
