package com.idworx.lisa.features.guidednavigationaccessfloatingcard.validation

import com.idworx.lisa.features.guidednavigationaccessfloatingcard.audit.GuidedNavigationAccessFloatingCardAuditor
import com.idworx.lisa.features.guidednavigationaccessfloatingcard.metadata.GuidedNavigationAccessFloatingCardMetadata
import com.idworx.lisa.validation.ValidationCheckResult
import com.idworx.lisa.validation.ValidationOutcome
import com.idworx.lisa.validation.ValidationReport

object GuidedNavigationAccessFloatingCardAuthorityV1 {

    const val AUTHORITY_NAME: String = "LISA_GUIDED_NAVIGATION_ACCESS_AND_FLOATING_CARD_V1"
    const val PASS_TOKEN: String = GuidedNavigationAccessFloatingCardMetadata.PASS_TOKEN

    fun validate(): ValidationReport {
        val checks = listOf(
            check(
                "GNAFC_001",
                "Welcome/setup exposes Skip to Navigation Training",
                GuidedNavigationAccessFloatingCardAuditor.welcomeExposesSkipToNavigationTraining(),
                "Add a caregiver/testing 'Skip to Navigation Training' link to TrainingFirstLaunchChoiceScreen and wire it to TrainingEvent.SkipToNavigationTraining."
            ),
            check(
                "GNAFC_002",
                "Skip to Navigation Training starts at Lesson 16 of 23",
                GuidedNavigationAccessFloatingCardAuditor.skipStartsAtLesson16(),
                "Ensure GuidedTrainingNavigator routes SkipToNavigationTraining to TrainingPhase.NavigationLesson with navigationLessonIndex = 0."
            ),
            check(
                "GNAFC_003",
                "It enters real workspace GUIDED_TRAINING mode",
                GuidedNavigationAccessFloatingCardAuditor.entersRealWorkspaceGuidedTrainingMode(),
                "Ensure guidedWorkspaceTrainingActive is true for TrainingPhase.NavigationLesson so the real workspace renders in GuidedWorkspaceMode.GUIDED_TRAINING."
            ),
            check(
                "GNAFC_004",
                "It bypasses the 15 phrase lessons",
                GuidedNavigationAccessFloatingCardAuditor.bypassesPhraseLessons(),
                "SkipToNavigationTraining must route straight to TrainingPhase.NavigationLesson without visiting Setup or CommunicationLesson."
            ),
            check(
                "GNAFC_005",
                "Lesson card is not rendered behind the Listening banner",
                GuidedNavigationAccessFloatingCardAuditor.lessonCardNotBehindListeningBanner(),
                "Move the floating lesson card so it renders after (on top of) the Listening/Watching-your-eyes banner and docks at the bottom, not Alignment.TopCenter."
            ),
            check(
                "GNAFC_006",
                "Lesson card remains visible/readable",
                GuidedNavigationAccessFloatingCardAuditor.lessonCardRemainsVisibleReadable(),
                "Keep GuidedWorkspaceLessonCard opaque with elevation and the Lesson X of Y / title / Gesture: <gesture> text."
            ),
            check(
                "GNAFC_007",
                "Real workspace layout is not structurally changed",
                GuidedNavigationAccessFloatingCardAuditor.realWorkspaceLayoutNotStructurallyChanged(),
                "Do not alter GuidedVocabularyOverlay's internal rows/panel or the bottom Menu/Reset row; only add the floating card as an outer overlay."
            ),
            check(
                "GNAFC_008",
                "Highlighted target remains visible",
                GuidedNavigationAccessFloatingCardAuditor.highlightedTargetRemainsVisible(),
                "Dock the lesson card on the side opposite the highlighted control via GuidedWorkspaceTrainingSpec.cardDockFor, and keep trainingHighlight wired to GuidedVocabularyOverlay."
            ),
            check(
                "GNAFC_009",
                "Gesture filtering still works",
                GuidedNavigationAccessFloatingCardAuditor.gestureFilteringStillWorks(),
                "Do not regress MainActivity's acceptedByCurrentNavigationLesson gate or gesture classification."
            ),
            check(
                "GNAFC_010",
                "Existing full Guided Learning flow still works",
                GuidedNavigationAccessFloatingCardAuditor.existingFullGuidedLearningFlowStillWorks(),
                "Ensure BeginLearning still routes through Setup and CommunicationLesson, and RealWorkspaceGuidedNavigationTrainingAuthorityV1 remains green."
            ),
            check(
                "GNAFC_011",
                "Test class exists and Gradle validation task is registered",
                GuidedNavigationAccessFloatingCardAuditor.testClassExists() &&
                    GuidedNavigationAccessFloatingCardAuditor.gradleTaskRegistered(),
                "Add GuidedNavigationAccessFloatingCardAuthorityV1Test and register validateLisaGuidedNavigationAccessAndFloatingCardV1 in app/build.gradle.kts."
            )
        )

        val failed = checks.filter { !it.passed }
        val outcome = ValidationReport.resolveOutcome(checks)
        return ValidationReport(
            authorityName = AUTHORITY_NAME,
            outcome = outcome,
            passToken = if (outcome == ValidationOutcome.PASS) PASS_TOKEN else null,
            evidenceSummary = "Guided Navigation Training Access + Floating Lesson Card V1 verified ${checks.size} checks. " +
                "Passed: ${checks.count { it.passed }}. Failed: ${failed.size}.",
            checksPerformed = checks.map { "${it.checkId}: ${it.description}" },
            failedChecks = failed.map { "${it.checkId}: ${it.description}" },
            observations = listOf(GuidedNavigationAccessFloatingCardMetadata.DESIGN_RULE),
            affectedLicArticles = listOf(
                "Article 1.4.1.3 — User must never become trapped",
                "Article 4.2.1.2 — Every visible action tappable if touch exists"
            ),
            affectedLiecArticles = listOf(
                "Article 2.16 — Guided Learning teaches the real interface, not mock screens",
                "Article 2.17 — Caregiver/testing shortcuts never replace the primary user's simple choices"
            ),
            affectedLvcArticles = listOf(
                "Article 3.32 — Guided Navigation Training Access + Floating Lesson Card V1 validation"
            ),
            remediationGuidance = failed.mapNotNull { it.remediation }.distinct(),
            checkResults = checks,
            rootCause = failed.firstOrNull()?.let { "${it.checkId} — ${it.description}" },
            validationReasoning = if (outcome == ValidationOutcome.PASS) {
                "The Welcome screen exposes a caregiver/testing 'Skip to Navigation Training' shortcut " +
                    "that jumps to Lesson 16 of 23 in the real Communication Workspace's " +
                    "GuidedWorkspaceMode.GUIDED_TRAINING, bypassing all 15 phrase lessons. The floating " +
                    "lesson card now renders above the bottom Menu/Reset row, docked on whichever side " +
                    "keeps the highlighted real control visible, and never behind the Listening banner. " +
                    "The real workspace layout, gesture filtering, and the full Guided Learning flow are " +
                    "unchanged."
            } else {
                "${failed.size} Guided Navigation Training Access + Floating Lesson Card V1 checks failed."
            },
            subsystem = "Guided Navigation Training Access + Floating Lesson Card"
        )
    }

    private fun check(id: String, description: String, passed: Boolean, remediation: String? = null) =
        ValidationCheckResult(checkId = id, description = description, passed = passed, remediation = remediation)
}
