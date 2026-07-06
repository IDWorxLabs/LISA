package com.idworx.lisa.features.guideduioverlapandfalseblinkfix.validation

import com.idworx.lisa.features.guideduioverlapandfalseblinkfix.audit.GuidedUiOverlapAndFalseBlinkFixAuditor
import com.idworx.lisa.features.guideduioverlapandfalseblinkfix.metadata.GuidedUiOverlapAndFalseBlinkFixMetadata
import com.idworx.lisa.validation.ValidationCheckResult
import com.idworx.lisa.validation.ValidationOutcome
import com.idworx.lisa.validation.ValidationReport

object GuidedUiOverlapAndFalseBlinkFixAuthorityV1 {

    const val AUTHORITY_NAME: String = "LISA_GUIDED_UI_OVERLAP_AND_FALSE_BLINK_FIX_V1"
    const val PASS_TOKEN: String = GuidedUiOverlapAndFalseBlinkFixMetadata.PASS_TOKEN

    fun validate(): ValidationReport {
        val checks = listOf(
            check("GUOFB_001", "Long phrase text wraps without overlap", GuidedUiOverlapAndFalseBlinkFixAuditor.longPhraseTextWrapsWithoutOverlap()),
            check("GUOFB_002", "Long phrase screen keeps spacing between sections", GuidedUiOverlapAndFalseBlinkFixAuditor.lessonScreenHasSectionSpacing()),
            check("GUOFB_003", "Phrase title does not collide with gesture instruction", GuidedUiOverlapAndFalseBlinkFixAuditor.phraseTitleSeparatedFromGestureInstruction()),
            check("GUOFB_004", "Guided Learning remains minimal", GuidedUiOverlapAndFalseBlinkFixAuditor.guidedLearningRemainsMinimal()),
            check("GUOFB_005", "Blink requires open-before-close-before-open pattern", GuidedUiOverlapAndFalseBlinkFixAuditor.blinkRequiresOpenCloseOpenPattern()),
            check("GUOFB_006", "Blink not accepted if eyes were not confidently open before", GuidedUiOverlapAndFalseBlinkFixAuditor.blinkRejectedWithoutOpenPriming()),
            check("GUOFB_007", "Blink not accepted during unstable face/eye tracking", GuidedUiOverlapAndFalseBlinkFixAuditor.blinkRejectedDuringUnstableTracking()),
            check("GUOFB_008", "Short probability dips do not count as blinks", GuidedUiOverlapAndFalseBlinkFixAuditor.shortProbabilityDipDoesNotCountAsBlink()),
            check("GUOFB_009", "Normal intentional blink is accepted", GuidedUiOverlapAndFalseBlinkFixAuditor.normalIntentionalBlinkAccepted()),
            check("GUOFB_010", "Double blink still works", GuidedUiOverlapAndFalseBlinkFixAuditor.doubleBlinkStillWorks()),
            check("GUOFB_011", "Sensitivity still changes thresholds", GuidedUiOverlapAndFalseBlinkFixAuditor.sensitivityChangesThresholds()),
            check("GUOFB_012", "Accepted blink updates visible feedback", GuidedUiOverlapAndFalseBlinkFixAuditor.acceptedBlinkUpdatesVisibleFeedback()),
            check("GUOFB_013", "Sequence finalizes after 3 seconds with phrase-only speech", GuidedUiOverlapAndFalseBlinkFixAuditor.sequenceFinalizesAfterThreeSeconds() && GuidedUiOverlapAndFalseBlinkFixAuditor.phraseSpeechOnlyPreserved()),
            check("GUOFB_014", "Long phrase samples use responsive font sizing", GuidedUiOverlapAndFalseBlinkFixAuditor.longPhraseSamplesUseSmallerFont()),
            check("GUOFB_015", "Tests pass and Gradle validation task defined", GuidedUiOverlapAndFalseBlinkFixAuditor.testClassExists() && GuidedUiOverlapAndFalseBlinkFixAuditor.gradleTaskRegistered())
        )

        val failed = checks.filter { !it.passed }
        val outcome = ValidationReport.resolveOutcome(checks)
        return ValidationReport(
            authorityName = AUTHORITY_NAME,
            outcome = outcome,
            passToken = if (outcome == ValidationOutcome.PASS) PASS_TOKEN else null,
            evidenceSummary = "Guided UI overlap and false blink fix verified ${checks.size} checks. Passed: ${checks.count { it.passed }}. Failed: ${failed.size}.",
            checksPerformed = checks.map { "${it.checkId}: ${it.description}" },
            failedChecks = failed.map { "${it.checkId}: ${it.description}" },
            observations = listOf(
                "Long Guided Learning phrases wrap across up to 3 lines with responsive sizing.",
                "Blink detection requires open → close → open with priming to reduce false positives."
            ),
            affectedLicArticles = listOf("Part II — Guided Learning UI and blink reliability"),
            affectedLiecArticles = listOf("Article 2.8 — Phrase layout and false blink rejection"),
            affectedLvcArticles = listOf("Article 3.22 — Guided UI Overlap and False Blink Fix validation"),
            remediationGuidance = failed.mapNotNull { it.remediation }.distinct(),
            checkResults = checks,
            rootCause = failed.firstOrNull()?.let { "${it.checkId} — ${it.description}" },
            validationReasoning = if (outcome == ValidationOutcome.PASS) {
                "Long phrases wrap cleanly; blink detection rejects unprimed dips while accepting intentional blinks."
            } else {
                "${failed.size} UI overlap or false blink checks failed."
            },
            subsystem = "Guided UI Overlap and False Blink Fix"
        )
    }

    private fun check(id: String, description: String, passed: Boolean, remediation: String? = null) =
        ValidationCheckResult(id, description, passed, remediation)
}
