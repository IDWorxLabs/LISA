package com.idworx.lisa.features.guidedtraininggesturemismatch.validation

import com.idworx.lisa.features.guidedtraininggesturemismatch.audit.GuidedTrainingGestureMismatchAuditor
import com.idworx.lisa.features.guidedtraininggesturemismatch.metadata.GuidedTrainingGestureMismatchMetadata
import com.idworx.lisa.features.guidednavigationaccessfloatingcard.validation.GuidedNavigationAccessFloatingCardAuthorityV1
import com.idworx.lisa.features.guidedtrainingclarityandtiming.validation.GuidedTrainingClarityAndTimingAuthorityV1
import com.idworx.lisa.features.guidedtraininglessonfocus.validation.GuidedTrainingLessonFocusAuthorityV1
import com.idworx.lisa.features.realworkspaceguidednavigationtraining.validation.RealWorkspaceGuidedNavigationTrainingAuthorityV1
import com.idworx.lisa.validation.ValidationCheckResult
import com.idworx.lisa.validation.ValidationOutcome
import com.idworx.lisa.validation.ValidationReport

object GuidedTrainingGestureMismatchAuthorityV1 {

    const val AUTHORITY_NAME: String = "LISA_GUIDED_TRAINING_GESTURE_MISMATCH_V1"
    const val PASS_TOKEN: String = GuidedTrainingGestureMismatchMetadata.PASS_TOKEN

    fun validate(): ValidationReport {
        val checks = listOf(
            check(
                "GTGM_001",
                "Every Guided Training category lesson gesture equals the real workspace category gesture",
                GuidedTrainingGestureMismatchAuditor.categoryLessonGestureEqualsRealWorkspaceGesture(),
                "Keep GuidedWorkspaceTrainingSpec.lessonCardGestureLabel(SelectCategory) returning GuidedCategoryShortcuts.sequenceLabelForCategory(conversationCategoryIndex), never a separately hardcoded/generic Select gesture."
            ),
            check(
                "GTGM_002",
                "Every Guided Training phrase lesson gesture equals the real workspace phrase gesture",
                GuidedTrainingGestureMismatchAuditor.phraseLessonGestureEqualsRealWorkspacePhraseGesture(),
                "Keep lessonCardGestureLabel(SelectPhrase, highlightedPhraseGesture) returning the exact highlightedPhraseGesture string it was given."
            ),
            check(
                "GTGM_003",
                "Every navigation lesson gesture equals the real workspace navigation gesture",
                GuidedTrainingGestureMismatchAuditor.navigationLessonGesturesEqualRealPanelGestures(),
                "Keep GuidedNavigationPanelSpec.panelActions and GuidedWorkspaceTrainingSpec.lessonCardGestureLabel both deriving their labels from the same GuidedModeNavigation/EMERGENCY_* constants."
            ),
            check(
                "GTGM_004",
                "Displayed floating-card gesture equals the accepted gesture",
                GuidedTrainingGestureMismatchAuditor.displayedGestureEqualsAcceptedGestureForEveryNavigationLesson(),
                "Keep every lessonCardGestureLabel(action) value classifying/resolving back to that same action via the real workspace's own gesture lookups."
            ),
            check(
                "GTGM_005",
                "Highlighted target gesture equals the lesson gesture",
                GuidedTrainingGestureMismatchAuditor.highlightedTargetGestureEqualsLessonGesture(),
                "Keep the floating card's phrase gesture sourced from the same guidedCategoryPage/visiblePhraseEntries the real GuidedVocabularyOverlay renders, and the category gesture sourced from GuidedCategoryShortcuts."
            ),
            check(
                "GTGM_006",
                "Wrong old/hardcoded gestures are rejected",
                GuidedTrainingGestureMismatchAuditor.wrongOrHardcodedCategoryGesturesAreRejected(),
                "Keep isNavigationLessonOffTargetAttempt's SelectCategory branch requiring GuidedCategoryShortcuts.categoryIndexForGesture(left, right) == conversationCategoryIndex, rejecting the old generic Select gesture and every other category's shortcut."
            ),
            check(
                "GTGM_007",
                "Correct real workspace gestures are accepted",
                GuidedTrainingGestureMismatchAuditor.correctRealWorkspaceCategoryGestureIsAccepted(),
                "Keep GuidedCategoryShortcuts.gestureForCategory(conversationCategoryIndex) passing GuidedTrainingFocusPolicy.isTargetAllowed for the Select Category lesson."
            ),
            check(
                "GTGM_008",
                "Normal workspace behavior after Guided Training uses the same gesture mapping the user was taught",
                GuidedTrainingGestureMismatchAuditor.normalWorkspaceUsesSameGestureMappingAfterTraining(),
                "Keep GuidedNavigationController.processCategoryMenuGesture's direct-shortcut branch (GuidedCategoryShortcuts.categoryIndexForGesture) opening the exact category Guided Training teaches, with no training-only special casing."
            ),
            check(
                "GTGM_009",
                "MainActivity's fine-grained lesson gate and dispatch are wired to the single source of truth",
                GuidedTrainingGestureMismatchAuditor.mainActivityFineGateUsesSingleSourceOfTruth(),
                "Keep isNavigationLessonOffTargetAttempt's SelectCategory branch and handleNavigationTrainingSequence's catch-all branch both driven by GuidedCategoryShortcuts.categoryIndexForGesture, with the catch-all branch calling verifyTrainingNavigation(SelectCategory) for the matched shortcut."
            ),
            check(
                "GTGM_010",
                "Regression coverage — existing Guided Training validations remain green",
                GuidedTrainingLessonFocusAuthorityV1.validate().outcome == ValidationOutcome.PASS &&
                    GuidedTrainingClarityAndTimingAuthorityV1.validate().outcome == ValidationOutcome.PASS &&
                    GuidedNavigationAccessFloatingCardAuthorityV1.validate().outcome == ValidationOutcome.PASS &&
                    RealWorkspaceGuidedNavigationTrainingAuthorityV1.validate().outcome == ValidationOutcome.PASS,
                "Re-run GuidedTrainingLessonFocusAuthorityV1, GuidedTrainingClarityAndTimingAuthorityV1, GuidedNavigationAccessFloatingCardAuthorityV1 and RealWorkspaceGuidedNavigationTrainingAuthorityV1, and fix any regression before shipping the gesture single-source-of-truth fix."
            ),
            check(
                "GTGM_011",
                "Test class exists and Gradle validation task is registered",
                GuidedTrainingGestureMismatchAuditor.testClassExists() &&
                    GuidedTrainingGestureMismatchAuditor.gradleTaskRegistered(),
                "Add GuidedTrainingGestureMismatchAuthorityV1Test and register ${GuidedTrainingGestureMismatchMetadata.GRADLE_TASK} in app/build.gradle.kts."
            )
        )

        val failed = checks.filter { !it.passed }
        val outcome = ValidationReport.resolveOutcome(checks)
        return ValidationReport(
            authorityName = AUTHORITY_NAME,
            outcome = outcome,
            passToken = if (outcome == ValidationOutcome.PASS) PASS_TOKEN else null,
            evidenceSummary = "Guided Training Gesture Mismatch V1 verified ${checks.size} checks. " +
                "Passed: ${checks.count { it.passed }}. Failed: ${failed.size}.",
            checksPerformed = checks.map { "${it.checkId}: ${it.description}" },
            failedChecks = failed.map { "${it.checkId}: ${it.description}" },
            observations = listOf(GuidedTrainingGestureMismatchMetadata.DESIGN_RULE),
            affectedLicArticles = listOf(
                "Article 1.4.1.3 — User must never become trapped"
            ),
            affectedLiecArticles = listOf(
                "Article 2.16 — Guided Learning teaches the real interface, not mock screens",
                "Article 2.20 — Guided Learning must never teach a gesture that differs from real workspace behavior"
            ),
            affectedLvcArticles = listOf(
                "Article 3.35 — Guided Training Gesture Mismatch V1 validation"
            ),
            remediationGuidance = failed.mapNotNull { it.remediation }.distinct(),
            checkResults = checks,
            rootCause = failed.firstOrNull()?.let { "${it.checkId} — ${it.description}" },
            validationReasoning = if (outcome == ValidationOutcome.PASS) {
                "Every Guided Training lesson's displayed gesture, accepted gesture, and " +
                    "validation target are now derived from the exact same function the real " +
                    "workspace control uses — category lessons from GuidedCategoryShortcuts, " +
                    "phrase lessons from the actual highlighted entry's own sequenceLabel, and " +
                    "every navigation-panel lesson from the same GuidedModeNavigation/EMERGENCY_* " +
                    "constants the real panel buttons render. The previously reported mismatch " +
                    "(lesson taught the generic Select gesture while the Conversation category " +
                    "row displayed its own direct shortcut) can no longer occur, and normal " +
                    "workspace use after training accepts exactly what was taught."
            } else {
                "${failed.size} Guided Training Gesture Mismatch V1 checks failed."
            },
            subsystem = "Guided Training Gesture Mismatch"
        )
    }

    private fun check(id: String, description: String, passed: Boolean, remediation: String? = null) =
        ValidationCheckResult(checkId = id, description = description, passed = passed, remediation = remediation)
}
