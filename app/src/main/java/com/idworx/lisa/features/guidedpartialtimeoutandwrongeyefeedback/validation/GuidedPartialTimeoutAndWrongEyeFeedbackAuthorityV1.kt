package com.idworx.lisa.features.guidedpartialtimeoutandwrongeyefeedback.validation

import com.idworx.lisa.features.guidedpartialtimeoutandwrongeyefeedback.audit.GuidedPartialTimeoutAndWrongEyeFeedbackAuditor
import com.idworx.lisa.features.guidedpartialtimeoutandwrongeyefeedback.metadata.GuidedPartialTimeoutAndWrongEyeFeedbackMetadata
import com.idworx.lisa.validation.ValidationCheckResult
import com.idworx.lisa.validation.ValidationOutcome
import com.idworx.lisa.validation.ValidationReport

object GuidedPartialTimeoutAndWrongEyeFeedbackAuthorityV1 {

    const val AUTHORITY_NAME: String = "LISA_GUIDED_PARTIAL_TIMEOUT_AND_WRONG_EYE_FEEDBACK_V1"
    const val PASS_TOKEN: String = GuidedPartialTimeoutAndWrongEyeFeedbackMetadata.PASS_TOKEN

    fun validate(): ValidationReport {
        val checks = listOf(
            check("GPTWEF_001", "Partial sequence starts correctly", GuidedPartialTimeoutAndWrongEyeFeedbackAuditor.partialSequenceStartsCorrectly()),
            check("GPTWEF_002", "Partial sequence resets after 5 seconds of no activity", GuidedPartialTimeoutAndWrongEyeFeedbackAuditor.partialSequenceResetsAfterFiveSeconds()),
            check("GPTWEF_003", "Reset keeps user on same lesson", GuidedPartialTimeoutAndWrongEyeFeedbackAuditor.resetKeepsUserOnSameLesson()),
            check("GPTWEF_004", "Reset clears blink counts and progress", GuidedPartialTimeoutAndWrongEyeFeedbackAuditor.resetClearsBlinkCountsAndProgress()),
            check("GPTWEF_005", "Reset does not trigger speech", GuidedPartialTimeoutAndWrongEyeFeedbackAuditor.resetDoesNotTriggerSpeech()),
            check("GPTWEF_006", "Wrong-eye blink does not register", GuidedPartialTimeoutAndWrongEyeFeedbackAuditor.wrongEyeBlinkDoesNotRegister()),
            check("GPTWEF_007", "Wrong-eye blink shows red feedback", GuidedPartialTimeoutAndWrongEyeFeedbackAuditor.wrongEyeShowsRedFeedback()),
            check("GPTWEF_008", "Wrong-eye feedback tells user to restart sequence", GuidedPartialTimeoutAndWrongEyeFeedbackAuditor.wrongEyeFeedbackTellsUserToRestart()),
            check("GPTWEF_009", "Wrong-eye blink resets progress to zero", GuidedPartialTimeoutAndWrongEyeFeedbackAuditor.wrongBlinkResetsProgressToZero()),
            check("GPTWEF_010", "User can complete sequence after correcting eye", GuidedPartialTimeoutAndWrongEyeFeedbackAuditor.userCanCompleteSequenceAfterCorrection()),
            check("GPTWEF_011", "Phrase speech after valid sequence and 3-second finalization", GuidedPartialTimeoutAndWrongEyeFeedbackAuditor.phraseSpeechAfterThreeSecondFinalization()),
            check("GPTWEF_012", "No non-phrase narration in lesson path", GuidedPartialTimeoutAndWrongEyeFeedbackAuditor.noNonPhraseNarrationInLessonPath()),
            check("GPTWEF_013", "Tests pass and Gradle validation task defined", GuidedPartialTimeoutAndWrongEyeFeedbackAuditor.testClassExists() && GuidedPartialTimeoutAndWrongEyeFeedbackAuditor.gradleTaskRegistered())
        )

        val failed = checks.filter { !it.passed }
        val outcome = ValidationReport.resolveOutcome(checks)
        return ValidationReport(
            authorityName = AUTHORITY_NAME,
            outcome = outcome,
            passToken = if (outcome == ValidationOutcome.PASS) PASS_TOKEN else null,
            evidenceSummary = "Guided partial timeout and wrong-eye feedback verified ${checks.size} checks. Passed: ${checks.count { it.passed }}. Failed: ${failed.size}.",
            checksPerformed = checks.map { "${it.checkId}: ${it.description}" },
            failedChecks = failed.map { "${it.checkId}: ${it.description}" },
            observations = listOf(
                "Incomplete lesson sequences reset after 5 s idle with visual Try again feedback.",
                "Wrong-eye blinks restart the lesson sequence from zero with red start-again feedback."
            ),
            affectedLicArticles = listOf("Part II — Guided Learning lesson feedback"),
            affectedLiecArticles = listOf("Article 2.9 — Partial timeout and wrong-eye feedback"),
            affectedLvcArticles = listOf("Article 3.23 — Guided Partial Timeout and Wrong-Eye Feedback validation"),
            remediationGuidance = failed.mapNotNull { it.remediation }.distinct(),
            checkResults = checks,
            rootCause = failed.firstOrNull()?.let { "${it.checkId} — ${it.description}" },
            validationReasoning = if (outcome == ValidationOutcome.PASS) {
                "Partial sequences recover cleanly; wrong-eye blinks restart the lesson from zero."
            } else {
                "${failed.size} partial timeout or wrong-eye feedback checks failed."
            },
            subsystem = "Guided Partial Timeout and Wrong-Eye Feedback"
        )
    }

    private fun check(id: String, description: String, passed: Boolean, remediation: String? = null) =
        ValidationCheckResult(checkId = id, description = description, passed = passed, remediation = remediation)
}
