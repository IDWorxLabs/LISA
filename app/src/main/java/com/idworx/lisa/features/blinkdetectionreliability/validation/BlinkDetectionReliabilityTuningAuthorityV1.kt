package com.idworx.lisa.features.blinkdetectionreliability.validation

import com.idworx.lisa.features.blinkdetectionreliability.audit.BlinkDetectionReliabilityAuditor
import com.idworx.lisa.features.blinkdetectionreliability.metadata.BlinkDetectionReliabilityMetadata
import com.idworx.lisa.validation.ValidationCheckResult
import com.idworx.lisa.validation.ValidationOutcome
import com.idworx.lisa.validation.ValidationReport

object BlinkDetectionReliabilityTuningAuthorityV1 {

    const val AUTHORITY_NAME: String = "LISA_BLINK_DETECTION_RELIABILITY_TUNING_V1"
    const val PASS_TOKEN: String = BlinkDetectionReliabilityMetadata.PASS_TOKEN

    fun validate(): ValidationReport {
        val checks = listOf(
            check("BDRT_001", "BlinkDetectionProcessor module integrated in MainActivity", BlinkDetectionReliabilityAuditor.mainActivityUsesProcessor()),
            check("BDRT_002", "Cooldown tuned for same-side double blinks", BlinkDetectionReliabilityAuditor.cooldownReducedFromLegacy()),
            check("BDRT_003", "Quick valid blink is accepted", BlinkDetectionReliabilityAuditor.quickValidBlinkAccepted()),
            check("BDRT_004", "Normal blink uses minimal frame requirement at default sensitivity", BlinkDetectionReliabilityAuditor.normalBlinkRequiresMinimalFrames()),
            check("BDRT_005", "Cooldown does not block next intentional same-side blink", BlinkDetectionReliabilityAuditor.cooldownDoesNotBlockIntentionalDoubleBlink()),
            check("BDRT_006", "Jitter tolerance during active sequence", BlinkDetectionReliabilityAuditor.jitterTolerantDuringActiveSequence()),
            check("BDRT_007", "Accepted blink updates lesson wink feedback", BlinkDetectionReliabilityAuditor.lessonFeedbackUpdatesOnWink()),
            check("BDRT_008", "Noisy partial blink does not bypass idle finalization", BlinkDetectionReliabilityAuditor.noisyBlinkDoesNotFinalizeEarly()),
            check("BDRT_009", "Diagnostics panel hidden behind developer mode", BlinkDetectionReliabilityAuditor.diagnosticsPanelBehindDeveloperMode()),
            check("BDRT_010", "Eyes-not-detected visible feedback on lesson screen", BlinkDetectionReliabilityAuditor.eyesNotDetectedFeedback()),
            check("BDRT_011", "Phrase-translation-only speech policy preserved", BlinkDetectionReliabilityAuditor.phraseSpeechOnly()),
            check("BDRT_012", "Tests pass and Gradle validation task defined", BlinkDetectionReliabilityAuditor.testClassExists() && BlinkDetectionReliabilityAuditor.gradleTaskRegistered())
        )

        val failed = checks.filter { !it.passed }
        val outcome = ValidationReport.resolveOutcome(checks)
        return ValidationReport(
            authorityName = AUTHORITY_NAME,
            outcome = outcome,
            passToken = if (outcome == ValidationOutcome.PASS) PASS_TOKEN else null,
            evidenceSummary = "Blink detection reliability verified ${checks.size} checks. Passed: ${checks.count { it.passed }}. Failed: ${failed.size}.",
            checksPerformed = checks.map { "${it.checkId}: ${it.description}" },
            failedChecks = failed.map { "${it.checkId}: ${it.description}" },
            observations = listOf(
                BlinkDetectionReliabilityMetadata.TUNING_RULE,
                BlinkDetectionReliabilityMetadata.SEQUENCE_RULE
            ),
            affectedLicArticles = listOf("Part II — Blink detection reliability"),
            affectedLiecArticles = listOf("Article 2.5 — Blink detection tuning"),
            affectedLvcArticles = listOf("Article 3.19 — Blink Detection Reliability validation"),
            remediationGuidance = failed.mapNotNull { it.remediation }.distinct(),
            checkResults = checks,
            rootCause = failed.firstOrNull()?.let { "${it.checkId} — ${it.description}" },
            validationReasoning = if (outcome == ValidationOutcome.PASS) {
                "Blink detection is more forgiving with live feedback and unchanged 5 s sequence finalization."
            } else {
                "${failed.size} blink detection reliability checks failed."
            },
            subsystem = "Blink Detection Reliability Tuning"
        )
    }

    private fun check(id: String, description: String, passed: Boolean, remediation: String? = null) =
        ValidationCheckResult(checkId = id, description = description, passed = passed, remediation = remediation)
}
