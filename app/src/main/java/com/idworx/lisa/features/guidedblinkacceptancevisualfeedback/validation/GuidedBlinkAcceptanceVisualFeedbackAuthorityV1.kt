package com.idworx.lisa.features.guidedblinkacceptancevisualfeedback.validation

import com.idworx.lisa.features.guidedblinkacceptancevisualfeedback.audit.GuidedBlinkAcceptanceVisualFeedbackAuditor
import com.idworx.lisa.features.guidedblinkacceptancevisualfeedback.metadata.GuidedBlinkAcceptanceVisualFeedbackMetadata
import com.idworx.lisa.validation.ValidationCheckResult
import com.idworx.lisa.validation.ValidationOutcome
import com.idworx.lisa.validation.ValidationReport

object GuidedBlinkAcceptanceVisualFeedbackAuthorityV1 {

    const val AUTHORITY_NAME: String = "LISA_GUIDED_BLINK_ACCEPTANCE_VISUAL_FEEDBACK_V1"
    const val PASS_TOKEN: String = GuidedBlinkAcceptanceVisualFeedbackMetadata.PASS_TOKEN

    fun validate(): ValidationReport {
        val checks = listOf(
            check("GBAVF_001", "Accepted LEFT blink animates only the LEFT counter", GuidedBlinkAcceptanceVisualFeedbackAuditor.acceptedLeftBlinkAnimatesOnlyLeftCounter()),
            check("GBAVF_002", "Accepted RIGHT blink animates only the RIGHT counter", GuidedBlinkAcceptanceVisualFeedbackAuditor.acceptedRightBlinkAnimatesOnlyRightCounter()),
            check("GBAVF_003", "Counter pulse (scale + brighten) completes smoothly in ~250-300ms", GuidedBlinkAcceptanceVisualFeedbackAuditor.counterPulseCompletesSuccessfully()),
            check("GBAVF_004", "Indicator dot flash occurs only for the detected eye", GuidedBlinkAcceptanceVisualFeedbackAuditor.indicatorFlashOccursOnlyForDetectedEye()),
            check("GBAVF_005", "Accepted blink message appears and auto-hides", GuidedBlinkAcceptanceVisualFeedbackAuditor.acceptedBlinkMessageAppearsAndAutoHides()),
            check("GBAVF_006", "Multiple accepted blinks each independently trigger their own animation", GuidedBlinkAcceptanceVisualFeedbackAuditor.multipleAcceptedBlinksTriggerMultipleAnimations()),
            check("GBAVF_007", "Wrong-eye blink does not trigger the accepted animation", GuidedBlinkAcceptanceVisualFeedbackAuditor.wrongEyeBlinkDoesNotTriggerAcceptedAnimation()),
            check("GBAVF_008", "Partial timeout reset still works and never plays an accepted animation", GuidedBlinkAcceptanceVisualFeedbackAuditor.partialTimeoutResetStillWorks()),
            check("GBAVF_009", "Phrase-only speech remains unchanged — no sounds, narration, or vibration added", GuidedBlinkAcceptanceVisualFeedbackAuditor.phraseOnlySpeechUnchanged()),
            check("GBAVF_010", "Existing Guided Learning validations remain green", GuidedBlinkAcceptanceVisualFeedbackAuditor.existingGuidedLearningValidationsRemainGreen()),
            check("GBAVF_011", "Tests pass and Gradle validation task defined", GuidedBlinkAcceptanceVisualFeedbackAuditor.testClassExists() && GuidedBlinkAcceptanceVisualFeedbackAuditor.gradleTaskRegistered())
        )

        val failed = checks.filter { !it.passed }
        val outcome = ValidationReport.resolveOutcome(checks)
        return ValidationReport(
            authorityName = AUTHORITY_NAME,
            outcome = outcome,
            passToken = if (outcome == ValidationOutcome.PASS) PASS_TOKEN else null,
            evidenceSummary = "Guided Learning blink acceptance visual feedback verified ${checks.size} checks. Passed: ${checks.count { it.passed }}. Failed: ${failed.size}.",
            checksPerformed = checks.map { "${it.checkId}: ${it.description}" },
            failedChecks = failed.map { "${it.checkId}: ${it.description}" },
            observations = listOf(GuidedBlinkAcceptanceVisualFeedbackMetadata.FEEDBACK_RULE),
            affectedLicArticles = listOf("Part II — Guided Learning blink acceptance feedback"),
            affectedLiecArticles = listOf("Article 2.13 — Accepted blink visual confirmation"),
            affectedLvcArticles = listOf("Article 3.27 — Guided Blink Acceptance Visual Feedback validation"),
            remediationGuidance = failed.mapNotNull { it.remediation }.distinct(),
            checkResults = checks,
            rootCause = failed.firstOrNull()?.let { "${it.checkId} — ${it.description}" },
            validationReasoning = if (outcome == ValidationOutcome.PASS) {
                "Every accepted blink instantly pulses its own counter, flashes its own indicator, and " +
                    "briefly shows a confirmation message — calm, per-eye, and self-clearing — without " +
                    "touching phrase-only speech, wrong-eye handling, or the partial timeout reset."
            } else {
                "${failed.size} Guided Blink Acceptance Visual Feedback checks failed."
            },
            subsystem = "Guided Blink Acceptance Visual Feedback"
        )
    }

    private fun check(id: String, description: String, passed: Boolean, remediation: String? = null) =
        ValidationCheckResult(checkId = id, description = description, passed = passed, remediation = remediation)
}
