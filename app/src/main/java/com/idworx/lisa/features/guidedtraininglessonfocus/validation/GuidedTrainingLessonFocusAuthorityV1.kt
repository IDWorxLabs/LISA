package com.idworx.lisa.features.guidedtraininglessonfocus.validation

import com.idworx.lisa.features.guidedtraininglessonfocus.audit.GuidedTrainingLessonFocusAuditor
import com.idworx.lisa.features.guidedtraininglessonfocus.metadata.GuidedTrainingLessonFocusMetadata
import com.idworx.lisa.validation.ValidationCheckResult
import com.idworx.lisa.validation.ValidationOutcome
import com.idworx.lisa.validation.ValidationReport

object GuidedTrainingLessonFocusAuthorityV1 {

    const val AUTHORITY_NAME: String = "LISA_GUIDED_TRAINING_LESSON_FOCUS_V1"
    const val PASS_TOKEN: String = GuidedTrainingLessonFocusMetadata.PASS_TOKEN

    fun validate(): ValidationReport {
        val checks = listOf(
            check(
                "GTLF_001",
                "During Open Categories, phrase gestures are blocked and do not speak phrases",
                GuidedTrainingLessonFocusAuditor.phraseGesturesBlockedDuringUnrelatedLesson(),
                "Keep the coarse acceptedByCurrentNavigationLesson gate as the first statement of handleNavigationTrainingSequence, and make rejectNavigationTrainingGesture() only show feedback + reset — never dispatch or speak."
            ),
            check(
                "GTLF_002",
                "During Select phrase, only the expected (highlighted) phrase can be selected/spoken",
                GuidedTrainingLessonFocusAuditor.onlyHighlightedPhraseSelectableDuringSelectPhraseLesson(),
                "Keep isNavigationLessonOffTargetAttempt comparing the attempted phrase gesture against WorkspacePhraseResolver.visibleEntriesForState(...).firstOrNull() before handleGuidedOverlaySequence runs."
            ),
            check(
                "GTLF_003",
                "Wrong gesture during a workspace lesson shows red wrong-sequence feedback",
                GuidedTrainingLessonFocusAuditor.wrongGestureShowsRedFeedback(),
                "Keep TrainingSessionController.applyNavigationWrongGestureFeedback() setting navigationWrongGestureMessage, and GuidedWorkspaceLessonCard rendering it in LisaEmergencyRed."
            ),
            check(
                "GTLF_004",
                "Wrong gesture resets the active sequence without advancing the lesson",
                GuidedTrainingLessonFocusAuditor.wrongGestureResetsSequenceWithoutAdvancing(),
                "Keep applyNavigationWrongGestureFeedback() free of any NavigationActionCompleted dispatch, and rejectNavigationTrainingGesture() calling resetSequence()."
            ),
            check(
                "GTLF_005",
                "Non-target dimmed items are also functionally inactive",
                GuidedTrainingLessonFocusAuditor.dimmedItemsAreFunctionallyInactive(),
                "Keep every dimmable row/button's clickable(...) gated by enabled = !trainingDimmed (or enabled && !trainingDimmed) alongside guidedTrainingDim(trainingDimmed)."
            ),
            check(
                "GTLF_006",
                "Normal workspace outside Guided Training still allows normal phrase speaking/navigation",
                GuidedTrainingLessonFocusAuditor.normalWorkspaceUnaffectedOutsideGuidedTraining(),
                "Keep the focus policy and functional dimming gated behind an active lesson (trainingDimActive/isNavigationTrainingActive) so NORMAL workspace use is never touched."
            ),
            check(
                "GTLF_007",
                "Correct expected gesture still completes the lesson",
                GuidedTrainingLessonFocusAuditor.correctExpectedGestureStillCompletesLesson(),
                "Keep both gates (acceptedByCurrentNavigationLesson, isNavigationLessonOffTargetAttempt) passing through to the existing when{} execution for the lesson's own highlighted target."
            ),
            check(
                "GTLF_008",
                "Existing response-time and positive-feedback behavior still passes",
                GuidedTrainingLessonFocusAuditor.existingResponseTimeAndPositiveFeedbackStillPasses(),
                "Re-run GuidedTrainingClarityAndTimingAuthorityV1 and fix any regression before shipping lesson-focus gating."
            ),
            check(
                "GTLF_009",
                "Emergency touch is rejected when another lesson is active, via the same focus policy",
                GuidedTrainingLessonFocusAuditor.emergencyTouchRejectedWhenOffTarget(),
                "Route triggerGuidedEmergencyTouch() through GuidedTrainingFocusPolicy.isTargetAllowed before ever calling beginEmergencyConfirm() — never a dedicated Emergency validator."
            ),
            check(
                "GTLF_010",
                "Emergency touch shows red wrong-gesture feedback, resets the sequence, never advances progress",
                GuidedTrainingLessonFocusAuditor.emergencyTouchShowsRedFeedbackAndResetsWithoutAdvancing(),
                "Keep the off-target Emergency touch branch calling the shared rejectNavigationTrainingGesture() helper and nothing else."
            ),
            check(
                "GTLF_011",
                "Emergency touch succeeds during the Emergency lesson without starting the real alarm flow",
                GuidedTrainingLessonFocusAuditor.emergencyTouchSucceedsDuringEmergencyLesson(),
                "Keep the allowed Emergency touch branch calling verifyTrainingNavigation(TriggerEmergency) directly instead of beginEmergencyConfirm()."
            ),
            check(
                "GTLF_012",
                "Existing blink-path Emergency behavior is unchanged",
                GuidedTrainingLessonFocusAuditor.blinkPathEmergencyBehaviorUnchanged(),
                "Keep handleNavigationTrainingSequence's isEmergencySequence branch calling verifyTrainingNavigation(TriggerEmergency) directly, never beginEmergencyConfirm()."
            ),
            check(
                "GTLF_013",
                "Test class exists and Gradle validation task is registered",
                GuidedTrainingLessonFocusAuditor.testClassExists() &&
                    GuidedTrainingLessonFocusAuditor.gradleTaskRegistered(),
                "Add GuidedTrainingLessonFocusAuthorityV1Test and register validateLisaGuidedTrainingLessonFocusV1 in app/build.gradle.kts."
            )
        )

        val failed = checks.filter { !it.passed }
        val outcome = ValidationReport.resolveOutcome(checks)
        return ValidationReport(
            authorityName = AUTHORITY_NAME,
            outcome = outcome,
            passToken = if (outcome == ValidationOutcome.PASS) PASS_TOKEN else null,
            evidenceSummary = "Guided Training Lesson Focus V1 verified ${checks.size} checks. " +
                "Passed: ${checks.count { it.passed }}. Failed: ${failed.size}.",
            checksPerformed = checks.map { "${it.checkId}: ${it.description}" },
            failedChecks = failed.map { "${it.checkId}: ${it.description}" },
            observations = listOf(GuidedTrainingLessonFocusMetadata.DESIGN_RULE),
            affectedLicArticles = listOf(
                "Article 1.4.1.3 — User must never become trapped",
                "Article 4.2.1.2 — Every visible action tappable if touch exists"
            ),
            affectedLiecArticles = listOf(
                "Article 2.16 — Guided Learning teaches the real interface, not mock screens",
                "Article 2.19 — Guided Learning teaches exactly one thing at a time"
            ),
            affectedLvcArticles = listOf(
                "Article 3.34 — Guided Training Lesson Focus V1 validation"
            ),
            remediationGuidance = failed.mapNotNull { it.remediation }.distinct(),
            checkResults = checks,
            rootCause = failed.firstOrNull()?.let { "${it.checkId} — ${it.description}" },
            validationReasoning = if (outcome == ValidationOutcome.PASS) {
                "Guided Training now teaches exactly one real workspace target at a time. Every " +
                    "navigation lesson passes through the existing coarse gesture-kind gate and a new " +
                    "row-level focus policy check for lessons with several visible candidates (Select " +
                    "Category, Select Phrase) — by blink and by touch — so only the highlighted row can " +
                    "ever act. Dimmed rows are functionally disabled, not just faded. Any blocked " +
                    "gesture or action shows a brief red acknowledgement, resets the active sequence, " +
                    "and never advances progress or speaks a phrase, while normal Communication " +
                    "Workspace use outside Guided Training remains completely unaffected."
            } else {
                "${failed.size} Guided Training Lesson Focus V1 checks failed."
            },
            subsystem = "Guided Training Lesson Focus"
        )
    }

    private fun check(id: String, description: String, passed: Boolean, remediation: String? = null) =
        ValidationCheckResult(checkId = id, description = description, passed = passed, remediation = remediation)
}
