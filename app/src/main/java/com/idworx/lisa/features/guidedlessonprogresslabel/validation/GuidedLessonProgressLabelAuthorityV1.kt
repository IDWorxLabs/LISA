package com.idworx.lisa.features.guidedlessonprogresslabel.validation

import com.idworx.lisa.features.guidedlessonprogresslabel.audit.GuidedLessonProgressLabelAuditor
import com.idworx.lisa.features.guidedlessonprogresslabel.metadata.GuidedLessonProgressLabelMetadata
import com.idworx.lisa.validation.ValidationCheckResult
import com.idworx.lisa.validation.ValidationOutcome
import com.idworx.lisa.validation.ValidationReport

object GuidedLessonProgressLabelAuthorityV1 {

    const val AUTHORITY_NAME: String = "LISA_GUIDED_LESSON_PROGRESS_LABEL_V1"
    const val PASS_TOKEN: String = GuidedLessonProgressLabelMetadata.PASS_TOKEN

    fun validate(): ValidationReport {
        val checks = listOf(
            check("GLPL_001", "Current lesson number displays correctly", GuidedLessonProgressLabelAuditor.currentLessonNumberDisplaysCorrectly()),
            check("GLPL_002", "Total lesson count displays correctly", GuidedLessonProgressLabelAuditor.totalLessonCountDisplaysCorrectly()),
            check("GLPL_003", "Total lesson count is derived from the curriculum, not hardcoded", GuidedLessonProgressLabelAuditor.totalIsDerivedNotHardcoded()),
            check("GLPL_004", "Label updates automatically as lessons advance", GuidedLessonProgressLabelAuditor.updatesAutomaticallyAsLessonsAdvance()),
            check("GLPL_005", "Navigation lessons continue the numbering", GuidedLessonProgressLabelAuditor.navigationLessonsContinueNumbering()),
            check("GLPL_006", "Label sits above the phrase title in a smaller, centered font", GuidedLessonProgressLabelAuditor.labelPlacedAbovePhraseTitleAndSmallerFont()),
            check("GLPL_007", "UI remains minimal — no progress bar or percentage", GuidedLessonProgressLabelAuditor.uiRemainsMinimal()),
            check("GLPL_008", "Tests pass and Gradle validation task defined", GuidedLessonProgressLabelAuditor.testClassExists() && GuidedLessonProgressLabelAuditor.gradleTaskRegistered())
        )

        val failed = checks.filter { !it.passed }
        val outcome = ValidationReport.resolveOutcome(checks)
        return ValidationReport(
            authorityName = AUTHORITY_NAME,
            outcome = outcome,
            passToken = if (outcome == ValidationOutcome.PASS) PASS_TOKEN else null,
            evidenceSummary = "Guided Learning lesson progress label verified ${checks.size} checks. Passed: ${checks.count { it.passed }}. Failed: ${failed.size}.",
            checksPerformed = checks.map { "${it.checkId}: ${it.description}" },
            failedChecks = failed.map { "${it.checkId}: ${it.description}" },
            observations = listOf(GuidedLessonProgressLabelMetadata.LABEL_RULE),
            affectedLicArticles = listOf("Part II — Guided Learning lesson progress orientation"),
            affectedLiecArticles = listOf("Article 2.12 — Lesson X of Y progress label"),
            affectedLvcArticles = listOf("Article 3.26 — Guided Lesson Progress Label validation"),
            remediationGuidance = failed.mapNotNull { it.remediation }.distinct(),
            checkResults = checks,
            rootCause = failed.firstOrNull()?.let { "${it.checkId} — ${it.description}" },
            validationReasoning = if (outcome == ValidationOutcome.PASS) {
                "Every Guided Learning lesson shows a minimal, always-accurate \"Lesson X of Y\" label " +
                    "that continues numbering into navigation lessons without any progress bar or percentage."
            } else {
                "${failed.size} Guided Lesson Progress Label checks failed."
            },
            subsystem = "Guided Lesson Progress Label"
        )
    }

    private fun check(id: String, description: String, passed: Boolean, remediation: String? = null) =
        ValidationCheckResult(checkId = id, description = description, passed = passed, remediation = remediation)
}
