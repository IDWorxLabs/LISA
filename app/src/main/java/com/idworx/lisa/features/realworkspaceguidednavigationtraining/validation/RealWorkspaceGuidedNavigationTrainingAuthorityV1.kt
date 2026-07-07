package com.idworx.lisa.features.realworkspaceguidednavigationtraining.validation

import com.idworx.lisa.features.realworkspaceguidednavigationtraining.audit.RealWorkspaceGuidedNavigationTrainingAuditor
import com.idworx.lisa.features.realworkspaceguidednavigationtraining.metadata.RealWorkspaceGuidedNavigationTrainingMetadata
import com.idworx.lisa.validation.ValidationCheckResult
import com.idworx.lisa.validation.ValidationOutcome
import com.idworx.lisa.validation.ValidationReport

object RealWorkspaceGuidedNavigationTrainingAuthorityV1 {

    const val AUTHORITY_NAME: String = "LISA_REAL_WORKSPACE_GUIDED_NAVIGATION_TRAINING_V1"
    const val PASS_TOKEN: String = RealWorkspaceGuidedNavigationTrainingMetadata.PASS_TOKEN

    fun validate(): ValidationReport {
        val checks = listOf(
            check(
                "RWGNT_001",
                "Navigation training no longer uses blank fake screens",
                RealWorkspaceGuidedNavigationTrainingAuditor.navigationTrainingDoesNotUseBlankScreen(),
                "Ensure trainingBlocksMainUi(NavigationLesson) is false and GuidedTrainingFlow is only invoked when it is true."
            ),
            check(
                "RWGNT_002",
                "After phrase lessons, Guided Learning enters the real Communication Workspace",
                RealWorkspaceGuidedNavigationTrainingAuditor.realWorkspaceOpensAfterPhraseLessons(),
                "Fix FundamentalsComplete transition and GuidedVocabularyOverlayVisibility.shouldShowOverlay's guidedWorkspaceTrainingActive gate."
            ),
            check(
                "RWGNT_003",
                "Workspace supports GUIDED_TRAINING mode",
                RealWorkspaceGuidedNavigationTrainingAuditor.workspaceSupportsGuidedTrainingMode(),
                "Define GuidedWorkspaceMode.NORMAL/GUIDED_TRAINING and thread it through GuidedVocabularyOverlay."
            ),
            check(
                "RWGNT_004",
                "Current lesson target is highlighted with a subtle blue outline",
                RealWorkspaceGuidedNavigationTrainingAuditor.currentLessonTargetIsHighlighted(),
                "Wire GuidedWorkspaceTrainingSpec.highlightTargetFor and the guidedTrainingHighlight modifier to every real control."
            ),
            check(
                "RWGNT_005",
                "Only the current lesson gesture is accepted during guided training",
                RealWorkspaceGuidedNavigationTrainingAuditor.onlyTargetGestureAccepted(),
                "Ensure acceptedByCurrentNavigationLesson gates handleNavigationTrainingSequence for every lesson's target gesture."
            ),
            check(
                "RWGNT_006",
                "Non-target gestures are ignored during guided training",
                RealWorkspaceGuidedNavigationTrainingAuditor.nonTargetGesturesIgnored(),
                "Ensure acceptedByCurrentNavigationLesson rejects every gesture that is not the active lesson's target."
            ),
            check(
                "RWGNT_007",
                "Open Categories lesson uses the real workspace Categories control",
                RealWorkspaceGuidedNavigationTrainingAuditor.openCategoriesUsesRealControl(),
                "Map navigation lesson 1 to NavigationAction.OpenCategories and highlight GuidedWorkspaceHighlightTarget.OpenCategories."
            ),
            check(
                "RWGNT_008",
                "Select category lesson uses the real category UI",
                RealWorkspaceGuidedNavigationTrainingAuditor.selectCategoryUsesRealControl(),
                "Map navigation lesson 2 to NavigationAction.SelectCategory and highlight GuidedWorkspaceHighlightTarget.CategoryRow."
            ),
            check(
                "RWGNT_009",
                "Select phrase lesson uses the real phrase row UI",
                RealWorkspaceGuidedNavigationTrainingAuditor.selectPhraseUsesRealControl(),
                "Map navigation lesson 3 to NavigationAction.SelectPhrase and highlight GuidedWorkspaceHighlightTarget.PhraseRow."
            ),
            check(
                "RWGNT_010",
                "Back / Next Page / Previous Page / Emergency lessons use real workspace controls",
                RealWorkspaceGuidedNavigationTrainingAuditor.backNextPreviousEmergencyUseRealControls(),
                "Map navigation lessons 4-7 to CloseMenu/NextPage/PreviousPage/TriggerEmergency with matching highlight targets."
            ),
            check(
                "RWGNT_011",
                "Normal workspace resolver does not consume guided training gestures first",
                RealWorkspaceGuidedNavigationTrainingAuditor.normalResolverDoesNotConsumeGuidedGesturesFirst(),
                "Ensure handleTrainingSequence returns from the navigation-training branch before reaching the normal resolver, and the lesson gate is the first statement of handleNavigationTrainingSequence."
            ),
            check(
                "RWGNT_012",
                "After guided training completes, workspace returns to normal mode",
                RealWorkspaceGuidedNavigationTrainingAuditor.workspaceReturnsToNormalModeAfterCompletion(),
                "Ensure advanceToNextLesson transitions to TrainingPhase.Completion once all navigation lessons are done."
            ),
            check(
                "RWGNT_013",
                "Phrase-only speech policy remains unchanged — no Brain 2, narration, or cloud added",
                RealWorkspaceGuidedNavigationTrainingAuditor.phraseOnlySpeechPolicyUnchangedNoBrain2NoCloud(),
                "Keep LisaSpeechPolicy.PHRASE_TRANSLATION_ONLY true and speak only completed phrases; do not introduce Brain 2 or cloud calls."
            ),
            check(
                "RWGNT_014",
                "Existing Guided Learning validations remain green",
                RealWorkspaceGuidedNavigationTrainingAuditor.existingGuidedLearningValidationsRemainGreen(),
                "Investigate and fix any regressed Guided Learning authority."
            ),
            check(
                "RWGNT_015",
                "Test class exists and Gradle validation task is registered",
                RealWorkspaceGuidedNavigationTrainingAuditor.testClassExists() &&
                    RealWorkspaceGuidedNavigationTrainingAuditor.gradleTaskRegistered(),
                "Add RealWorkspaceGuidedNavigationTrainingAuthorityV1Test and register validateLisaRealWorkspaceGuidedNavigationTrainingV1 in app/build.gradle.kts."
            )
        )

        val failed = checks.filter { !it.passed }
        val outcome = ValidationReport.resolveOutcome(checks)
        return ValidationReport(
            authorityName = AUTHORITY_NAME,
            outcome = outcome,
            passToken = if (outcome == ValidationOutcome.PASS) PASS_TOKEN else null,
            evidenceSummary = "Real Workspace Guided Navigation Training V1 verified ${checks.size} checks. " +
                "Passed: ${checks.count { it.passed }}. Failed: ${failed.size}.",
            checksPerformed = checks.map { "${it.checkId}: ${it.description}" },
            failedChecks = failed.map { "${it.checkId}: ${it.description}" },
            observations = listOf(RealWorkspaceGuidedNavigationTrainingMetadata.DESIGN_RULE),
            affectedLicArticles = listOf(
                "Article 1.4.1.3 — User must never become trapped",
                "Article 4.2.1.2 — Every visible action tappable if touch exists"
            ),
            affectedLiecArticles = listOf(
                "Article 2.3.1.1 — Guaranteed recovery routes",
                "Article 2.16 — Guided Learning teaches the real interface, not mock screens"
            ),
            affectedLvcArticles = listOf(
                "Article 3.31 — Real Workspace Guided Navigation Training V1 validation"
            ),
            remediationGuidance = failed.mapNotNull { it.remediation }.distinct(),
            checkResults = checks,
            rootCause = failed.firstOrNull()?.let { "${it.checkId} — ${it.description}" },
            validationReasoning = if (outcome == ValidationOutcome.PASS) {
                "After the 15 phrase lessons, Guided Learning's 8 navigation lessons now run inside " +
                    "the real Communication Workspace in GuidedWorkspaceMode.GUIDED_TRAINING: the real " +
                    "control is highlighted, only the target gesture is accepted, non-target gestures " +
                    "are ignored, and the old blank NavigationLessonScreen never renders. Completion " +
                    "returns the workspace to GuidedWorkspaceMode.NORMAL with phrase-only speech intact " +
                    "and every existing Guided Learning validation still green."
            } else {
                "${failed.size} Real Workspace Guided Navigation Training V1 checks failed."
            },
            subsystem = "Real Workspace Guided Navigation Training"
        )
    }

    private fun check(id: String, description: String, passed: Boolean, remediation: String? = null) =
        ValidationCheckResult(checkId = id, description = description, passed = passed, remediation = remediation)
}
