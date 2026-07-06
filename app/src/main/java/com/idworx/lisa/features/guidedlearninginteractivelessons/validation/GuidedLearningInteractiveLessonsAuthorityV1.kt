package com.idworx.lisa.features.guidedlearninginteractivelessons.validation

import com.idworx.lisa.features.guidedlearninginteractivelessons.audit.GuidedLearningInteractiveLessonsAuditor
import com.idworx.lisa.features.guidedlearninginteractivelessons.metadata.GuidedLearningInteractiveLessonsMetadata
import com.idworx.lisa.validation.ValidationCheckResult
import com.idworx.lisa.validation.ValidationOutcome
import com.idworx.lisa.validation.ValidationReport

object GuidedLearningInteractiveLessonsAuthorityV1 {

    const val AUTHORITY_NAME: String = "LISA_GUIDED_LEARNING_INTERACTIVE_LESSONS_V1"
    const val PASS_TOKEN: String = GuidedLearningInteractiveLessonsMetadata.PASS_TOKEN

    fun validate(): ValidationReport {
        val checks = listOf(
            check("GLIL_001", "Lesson screen shows eye status pill and compact panel", GuidedLearningInteractiveLessonsAuditor.lessonScreenShowsEyeStatus()),
            check("GLIL_002", "Lesson screen shows live blink-count feedback", GuidedLearningInteractiveLessonsAuditor.lessonScreenShowsLiveBlinkFeedback()),
            check("GLIL_003", "Correct partial sequence updates progress label", GuidedLearningInteractiveLessonsAuditor.partialSequenceUpdatesProgress()),
            check("GLIL_004", "Completed sequence uses visual-then-speech success flow", GuidedLearningInteractiveLessonsAuditor.completedSequenceUsesInteractiveSuccessFlow()),
            check("GLIL_005", "Success shows visual Well done / You did it", GuidedLearningInteractiveLessonsAuditor.successShowsVisualWellDone()),
            check("GLIL_006", "Only translated phrase is spoken on success", GuidedLearningInteractiveLessonsAuditor.onlyPhraseSpeechOnSuccess()),
            check("GLIL_007", "No coaching narration in interactive lesson path", GuidedLearningInteractiveLessonsAuditor.noCoachingNarrationInInteractivePath()),
            check("GLIL_008", "Wrong blink resets with visual-only feedback", GuidedLearningInteractiveLessonsAuditor.wrongBlinkResetsWithVisualOnly()),
            check("GLIL_009", "Lesson advances automatically after phrase speech", GuidedLearningInteractiveLessonsAuditor.lessonAdvancesAfterSpeech()),
            check("GLIL_010", "Essential phrases progress into navigation teaching", GuidedLearningInteractiveLessonsAuditor.progressesFromPhrasesToNavigation()),
            check("GLIL_011", "UI remains minimal without dashboard or coach strip", GuidedLearningInteractiveLessonsAuditor.uiRemainsMinimal()),
            check("GLIL_012", "Ordered blink gestures validate partial progress", GuidedLearningInteractiveLessonsAuditor.lessonInteractionEngineValidatesOrderedGestures()),
            check("GLIL_013", "Phrase-translation-only voice policy active", GuidedLearningInteractiveLessonsAuditor.phraseTranslationOnly()),
            check("GLIL_014", "Tests pass and Gradle validation task defined", GuidedLearningInteractiveLessonsAuditor.testClassExists() && GuidedLearningInteractiveLessonsAuditor.gradleTaskRegistered())
        )

        val failed = checks.filter { !it.passed }
        val outcome = ValidationReport.resolveOutcome(checks)
        return ValidationReport(
            authorityName = AUTHORITY_NAME,
            outcome = outcome,
            passToken = if (outcome == ValidationOutcome.PASS) PASS_TOKEN else null,
            evidenceSummary = "Guided Learning interactive lessons verified ${checks.size} checks. Passed: ${checks.count { it.passed }}. Failed: ${failed.size}.",
            checksPerformed = checks.map { "${it.checkId}: ${it.description}" },
            failedChecks = failed.map { "${it.checkId}: ${it.description}" },
            observations = listOf(
                GuidedLearningInteractiveLessonsMetadata.INTERACTION_RULE,
                GuidedLearningInteractiveLessonsMetadata.MINIMAL_UI_RULE
            ),
            affectedLicArticles = listOf("Part II — Guided Learning lesson interaction"),
            affectedLiecArticles = listOf("Article 2.4 — Interactive Guided Learning lessons"),
            affectedLvcArticles = listOf("Article 3.18 — Guided Learning Interactive Lessons validation"),
            remediationGuidance = failed.mapNotNull { it.remediation }.distinct(),
            checkResults = checks,
            rootCause = failed.firstOrNull()?.let { "${it.checkId} — ${it.description}" },
            validationReasoning = if (outcome == ValidationOutcome.PASS) {
                "Guided Learning lessons respond visibly to blinks with phrase-only speech and auto-advance."
            } else {
                "${failed.size} Guided Learning interactive lesson checks failed."
            },
            subsystem = "Guided Learning Interactive Lessons"
        )
    }

    private fun check(id: String, description: String, passed: Boolean, remediation: String? = null) =
        ValidationCheckResult(checkId = id, description = description, passed = passed, remediation = remediation)
}
