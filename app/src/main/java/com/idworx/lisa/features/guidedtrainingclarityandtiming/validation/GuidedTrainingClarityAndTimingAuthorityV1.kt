package com.idworx.lisa.features.guidedtrainingclarityandtiming.validation

import com.idworx.lisa.features.guidedtrainingclarityandtiming.audit.GuidedTrainingClarityAndTimingAuditor
import com.idworx.lisa.features.guidedtrainingclarityandtiming.metadata.GuidedTrainingClarityAndTimingMetadata
import com.idworx.lisa.validation.ValidationCheckResult
import com.idworx.lisa.validation.ValidationOutcome
import com.idworx.lisa.validation.ValidationReport

object GuidedTrainingClarityAndTimingAuthorityV1 {

    const val AUTHORITY_NAME: String = "LISA_GUIDED_TRAINING_CLARITY_AND_TIMING_V1"
    const val PASS_TOKEN: String = GuidedTrainingClarityAndTimingMetadata.PASS_TOKEN

    fun validate(): ValidationReport {
        val checks = listOf(
            check(
                "GTCT_001",
                "Response time can be adjusted independently of sensitivity",
                GuidedTrainingClarityAndTimingAuditor.responseTimeAdjustableIndependentlyOfSensitivity(),
                "Add Response Time +/- controls next to Sensitivity in UniversalEyeTrackingHeader."
            ),
            check(
                "GTCT_002",
                "Default guided response time is no longer 3s",
                GuidedTrainingClarityAndTimingAuditor.defaultGuidedResponseTimeIsSlowerThanThreeSeconds(),
                "Set TrainingPreferences.guidedResponseTimeSec default to 5-6s and source MainActivity's guided idle timeout from it, not a hardcoded lesson value."
            ),
            check(
                "GTCT_003",
                "Active gesture sequences are not interrupted by the response timer",
                GuidedTrainingClarityAndTimingAuditor.activeSequencesNotInterruptedByResponseTimer(),
                "Use effectiveSequenceIdleTimeoutMs()/effectiveSequenceMaxWindowMs() (Guided Mode's own settle time) for every finalize check, and keep restarting the idle window on every new wink."
            ),
            check(
                "GTCT_004",
                "Correct first guided sequence triggers positive feedback",
                GuidedTrainingClarityAndTimingAuditor.correctFirstGuidedSequenceTriggersPositiveFeedback(),
                "After verifyNavigation() dispatches NavigationActionCompleted, speak and show a rotating GuidedFeedbackPhrases.positive(...) acknowledgement before the next instruction."
            ),
            check(
                "GTCT_005",
                "Floating lesson bubble remains present",
                GuidedTrainingClarityAndTimingAuditor.floatingLessonBubbleRemainsPresent(),
                "Keep rendering GuidedWorkspaceLessonCard docked at the bottom, and let it show the new feedbackMessage state."
            ),
            check(
                "GTCT_006",
                "Guided workspace target is clearly represented in state/UI model",
                GuidedTrainingClarityAndTimingAuditor.guidedWorkspaceTargetClearlyRepresented(),
                "Keep trainingHighlight wired to GuidedVocabularyOverlay and de-emphasise non-target controls via Modifier.guidedTrainingDim while a lesson has an active highlight target."
            ),
            check(
                "GTCT_008",
                "Final navigation success emits visual feedback before Completion",
                GuidedTrainingClarityAndTimingAuditor.finalNavigationSuccessEmitsVisualFeedbackBeforeCompletion(),
                "On the FINAL navigation lesson, show/speak the feedback and set completionPendingFeedback BEFORE dispatching NavigationActionCompleted, and only dispatch it inside the delayed callback."
            ),
            check(
                "GTCT_009",
                "Completion still happens after the brief feedback delay",
                GuidedTrainingClarityAndTimingAuditor.completionStillHappensAfterFeedbackDelay(),
                "Keep dispatching NavigationActionCompleted for the final lesson inside the mainThreadDelayed(FINAL_NAVIGATION_COMPLETION_DELAY_MS) callback, and clear completionPendingFeedback once it fires."
            ),
            check(
                "GTCT_010",
                "Non-final navigation lessons continue normally (no artificial delay)",
                GuidedTrainingClarityAndTimingAuditor.nonFinalNavigationLessonsContinueNormally(),
                "Keep dispatching NavigationActionCompleted immediately for every non-final navigation lesson; only the final lesson holds for feedback."
            ),
            check(
                "GTCT_011",
                "Spoken feedback still works for normal and final navigation lessons",
                GuidedTrainingClarityAndTimingAuditor.spokenFeedbackStillWorksForNormalAndFinalLessons(),
                "Keep calling narration.speak(phrase) behind LisaSpeechPolicy.allowsNarration() in both applyNavigationCompletionFeedback and beginFinalNavigationCompletionFeedback."
            ),
            check(
                "GTCT_012",
                "No regression to guided response-time behavior",
                GuidedTrainingClarityAndTimingAuditor.noRegressionToGuidedResponseTimeBehavior(),
                "Keep the guided settle timer (effectiveSequenceIdleTimeoutMs/effectiveSequenceMaxWindowMs) and the slower-than-3s default untouched by the completion-feedback delay change."
            ),
            check(
                "GTCT_013",
                "Test class exists and Gradle validation task is registered",
                GuidedTrainingClarityAndTimingAuditor.testClassExists() &&
                    GuidedTrainingClarityAndTimingAuditor.gradleTaskRegistered(),
                "Add GuidedTrainingClarityAndTimingAuthorityV1Test and register validateLisaGuidedTrainingClarityAndTimingV1 in app/build.gradle.kts."
            )
        )

        val failed = checks.filter { !it.passed }
        val outcome = ValidationReport.resolveOutcome(checks)
        return ValidationReport(
            authorityName = AUTHORITY_NAME,
            outcome = outcome,
            passToken = if (outcome == ValidationOutcome.PASS) PASS_TOKEN else null,
            evidenceSummary = "Guided Training Clarity and Timing V1 verified ${checks.size} checks. " +
                "Passed: ${checks.count { it.passed }}. Failed: ${failed.size}.",
            checksPerformed = checks.map { "${it.checkId}: ${it.description}" },
            failedChecks = failed.map { "${it.checkId}: ${it.description}" },
            observations = listOf(GuidedTrainingClarityAndTimingMetadata.DESIGN_RULE),
            affectedLicArticles = listOf(
                "Article 1.4.1.3 — User must never become trapped",
                "Article 4.2.1.2 — Every visible action tappable if touch exists"
            ),
            affectedLiecArticles = listOf(
                "Article 2.16 — Guided Learning teaches the real interface, not mock screens",
                "Article 2.18 — Guided Learning paces itself to the learner, not a fixed clock"
            ),
            affectedLvcArticles = listOf(
                "Article 3.33 — Guided Training Clarity and Timing V1 validation"
            ),
            remediationGuidance = failed.mapNotNull { it.remediation }.distinct(),
            checkResults = checks,
            rootCause = failed.firstOrNull()?.let { "${it.checkId} — ${it.description}" },
            validationReasoning = if (outcome == ValidationOutcome.PASS) {
                "Guided Mode/Training now exposes its own adjustable Response Time control next to " +
                    "Sensitivity, defaulting to a slower settle time (5s) sourced from session " +
                    "preferences rather than a hardcoded lesson value. Every finalize check uses this " +
                    "guided settle timer while Guided Mode is active, and each new blink restarts it so " +
                    "multi-step gestures are never cut off mid-sequence. A correct first real-workspace " +
                    "navigation gesture now gets an immediate, varied, spoken-and-visual acknowledgement " +
                    "before the next instruction appears. The floating lesson bubble remains, and while " +
                    "it has an active target, every other real-workspace control is visually " +
                    "de-emphasised so the practice target is unambiguous."
            } else {
                "${failed.size} Guided Training Clarity and Timing V1 checks failed."
            },
            subsystem = "Guided Training Clarity and Timing"
        )
    }

    private fun check(id: String, description: String, passed: Boolean, remediation: String? = null) =
        ValidationCheckResult(checkId = id, description = description, passed = passed, remediation = remediation)
}
