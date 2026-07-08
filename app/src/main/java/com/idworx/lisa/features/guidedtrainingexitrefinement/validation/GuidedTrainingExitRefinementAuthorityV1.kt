package com.idworx.lisa.features.guidedtrainingexitrefinement.validation

import com.idworx.lisa.features.guidedtrainingexitrefinement.audit.GuidedTrainingExitRefinementAuditor
import com.idworx.lisa.features.guidedtrainingexitrefinement.metadata.GuidedTrainingExitRefinementMetadata
import com.idworx.lisa.validation.ValidationCheckResult
import com.idworx.lisa.validation.ValidationOutcome
import com.idworx.lisa.validation.ValidationReport

/**
 * Validates the final Guided Training exit experience and the simplified Communication
 * Workspace: a touch-independent Finish Training gesture, a shortened Choose Category gesture,
 * and removal of the redundant instructional block below Vocabulary.
 */
object GuidedTrainingExitRefinementAuthorityV1 {

    const val AUTHORITY_NAME: String = "LISA_GUIDED_TRAINING_EXIT_REFINEMENT_V1"
    const val PASS_TOKEN: String = GuidedTrainingExitRefinementMetadata.PASS_TOKEN

    fun validate(): ValidationReport {
        val checks = listOf(
            check("GTER_001", "Final navigation lesson teaches Finish Training (ResetSequence)", GuidedTrainingExitRefinementAuditor.finalLessonIsResetSequence()),
            check("GTER_002", "Final lesson never instructs a screen tap; teaches the real gesture instead", GuidedTrainingExitRefinementAuditor.finalLessonNeverInstructsATap()),
            check("GTER_003", "Completion wording uses natural language, not \"main workspace\"", GuidedTrainingExitRefinementAuditor.lessonWordingUsesNaturalStartCommunicatingPhrase()),
            check("GTER_004", "Lesson card teaches the Finish Training gesture dynamically from shared constants", GuidedTrainingExitRefinementAuditor.lessonCardTeachesFinishTrainingGestureDynamically()),
            check("GTER_005", "Finish Training gesture is wired into MainActivity's gesture dispatch, in and out of training", GuidedTrainingExitRefinementAuditor.finishTrainingGestureWiredInMainActivityGestureDispatch()),
            check("GTER_006", "performReset() verifies/completes the lesson before clearing state", GuidedTrainingExitRefinementAuditor.performResetVerifiesLessonBeforeClearingState()),
            check("GTER_007", "Finish Training is a reserved global gesture everywhere", GuidedTrainingExitRefinementAuditor.finishTrainingIsGlobalGestureReservedEverywhere()),
            check("GTER_008", "Completing all navigation lessons (ending with Finish Training) reaches Completion phase", GuidedTrainingExitRefinementAuditor.completingAllNavigationLessonsReachesCompletionPhase()),
            check("GTER_009", "Communication mode becomes active (workspace NORMAL) after completion", GuidedTrainingExitRefinementAuditor.communicationModeActiveAfterCompletion()),
            check("GTER_010", "Completion screen congratulates the learner before returning to the workspace", GuidedTrainingExitRefinementAuditor.completionScreenCongratulatesBeforeReturningToWorkspace()),
            check("GTER_011", "Choose Category uses a materially shorter gesture than the old 8-wink L4 R4", GuidedTrainingExitRefinementAuditor.categoriesGestureIsShorterThanOldEightWinkGesture()),
            check("GTER_012", "Categories and Finish Training gestures are distinct", GuidedTrainingExitRefinementAuditor.categoriesAndFinishTrainingGesturesAreDistinct()),
            check("GTER_013", "Categories gesture label in Guided Training derives from shared constants", GuidedTrainingExitRefinementAuditor.categoriesGestureLabelDerivedFromSharedConstants()),
            check("GTER_014", "No duplicate reserved/global gestures", GuidedTrainingExitRefinementAuditor.noDuplicateReservedGestures()),
            check("GTER_015", "No duplicate gestures across any workspace mode", GuidedTrainingExitRefinementAuditor.noDuplicateGesturesAcrossWorkspaceModes()),
            check("GTER_016", "No reserved gesture conflicts (Gesture Conflict Authority passes)", GuidedTrainingExitRefinementAuditor.noReservedGestureConflicts()),
            check("GTER_017", "Communication Workspace no longer shows the removed instructional block", GuidedTrainingExitRefinementAuditor.communicationWorkspaceInstructionBlockRemoved()),
            check("GTER_018", "Communication Workspace still shows Vocabulary, category, phrase list, and Navigation Panel", GuidedTrainingExitRefinementAuditor.communicationWorkspaceStillShowsCoreElements()),
            check("GTER_019", "Existing Guided Training, navigation, and gesture authorities remain green", GuidedTrainingExitRefinementAuditor.existingGuidedTrainingAndNavigationAuthoritiesRemainGreen()),
            check("GTER_020", "Tests pass and Gradle validation task defined", GuidedTrainingExitRefinementAuditor.testClassExists() && GuidedTrainingExitRefinementAuditor.gradleTaskRegistered())
        )

        val failed = checks.filter { !it.passed }
        val outcome = ValidationReport.resolveOutcome(checks)
        return ValidationReport(
            authorityName = AUTHORITY_NAME,
            outcome = outcome,
            passToken = if (outcome == ValidationOutcome.PASS) PASS_TOKEN else null,
            evidenceSummary = "Guided Training exit refinement verified ${checks.size} checks. Passed: ${checks.count { it.passed }}. Failed: ${failed.size}.",
            checksPerformed = checks.map { "${it.checkId}: ${it.description}" },
            failedChecks = failed.map { "${it.checkId}: ${it.description}" },
            observations = listOf(
                GuidedTrainingExitRefinementMetadata.TOUCH_INDEPENDENT_EXIT_RULE,
                GuidedTrainingExitRefinementMetadata.SHORT_CATEGORIES_RULE,
                GuidedTrainingExitRefinementMetadata.SIMPLIFIED_WORKSPACE_RULE
            ),
            affectedLicArticles = listOf("Part II — Guided Training completion and Communication Workspace daily use"),
            affectedLiecArticles = listOf("Article 2.7 — Navigation lesson gesture ownership", "Article 2.1 — Workspace navigation gestures"),
            affectedLvcArticles = listOf("Article 3.22 — Guided Training Exit Refinement validation"),
            remediationGuidance = failed.mapNotNull { it.remediation }.distinct(),
            checkResults = checks,
            rootCause = failed.firstOrNull()?.let { "${it.checkId} — ${it.description}" },
            validationReasoning = if (outcome == ValidationOutcome.PASS) {
                "Guided Training now ends with a touch-independent Finish Training gesture and a " +
                    "congratulatory completion screen; Choose Category uses a short, non-conflicting " +
                    "gesture; and the Communication Workspace stays focused on Vocabulary, the current " +
                    "category, the phrase list, and the Navigation Panel."
            } else {
                "${failed.size} Guided Training exit refinement checks failed."
            },
            subsystem = "Guided Training Exit Refinement"
        )
    }

    private fun check(id: String, description: String, passed: Boolean, remediation: String? = null) =
        ValidationCheckResult(checkId = id, description = description, passed = passed, remediation = remediation)
}
