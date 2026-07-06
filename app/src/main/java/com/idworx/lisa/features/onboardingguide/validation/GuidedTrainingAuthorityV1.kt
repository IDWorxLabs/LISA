package com.idworx.lisa.features.onboardingguide.validation

import com.idworx.lisa.features.onboardingguide.lessons.TrainingLessonCatalog
import com.idworx.lisa.features.onboardingguide.metadata.TrainingMetadata
import com.idworx.lisa.features.onboardingguide.model.TrainingPhase
import com.idworx.lisa.features.onboardingguide.services.EncouragementEngine
import com.idworx.lisa.features.personality.model.LisaPersonalityProfile
import com.idworx.lisa.features.onboardingguide.services.TrainingProgressStore
import com.idworx.lisa.validation.ValidationCheckResult
import com.idworx.lisa.validation.ValidationOutcome
import com.idworx.lisa.validation.ValidationReport

object GuidedTrainingAuthorityV1 {

    const val AUTHORITY_NAME: String = "GUIDED_TRAINING_AUTHORITY_V1"
    const val PASS_TOKEN: String = "GUIDED_TRAINING_AUTHORITY_V1_PASS"

    fun validate(): ValidationReport {
        val checks = buildList {
            add(lessonCatalogComplete())
            add(communicationLessonsHaveSequences())
            add(navigationLessonsDefined())
            add(encouragementNeverPunishes())
            add(persistenceKeysDocumented())
            add(metadataConsistent())
            add(phasesDefined())
            add(totalLessonCount())
        }
        val failed = checks.filter { !it.passed }
        val outcome = ValidationReport.resolveOutcome(checks)
        return ValidationReport(
            authorityName = AUTHORITY_NAME,
            outcome = outcome,
            passToken = if (outcome == ValidationOutcome.PASS) PASS_TOKEN else null,
            evidenceSummary = "Guided Training Authority verified ${checks.size} structural and behavioral checks. " +
                "Passed: ${checks.count { it.passed }}. Failed: ${failed.size}.",
            checksPerformed = checks.map { "${it.checkId}: ${it.description}" },
            failedChecks = failed.map { "${it.checkId}: ${it.description}" },
            observations = emptyList(),
            affectedLicArticles = listOf(
                "Article 1.4.1.3 — User must never become trapped",
                "Article 4.2.1.2 — Every visible action tappable if touch exists"
            ),
            affectedLiecArticles = listOf(
                "Article 2.3.1.1 — Guaranteed recovery routes",
                "Article 6.2.1.1 — Recovery engine guarantees escape"
            ),
            affectedLvcArticles = listOf(
                "Article 4.1.1.2 — Evidence record requirements",
                "Article 5.1.1.1 — Official outcome taxonomy"
            ),
            remediationGuidance = failed.mapNotNull { it.remediation }.distinct(),
            checkResults = checks,
            rootCause = failed.firstOrNull()?.let { "${it.checkId} — ${it.description}" },
            validationReasoning = if (outcome == ValidationOutcome.PASS) {
                "Guided Onboarding & Training System V1 structure is complete and ready for integration."
            } else {
                "${failed.size} training authority checks failed."
            },
            subsystem = "Guided Training"
        )
    }

    private fun lessonCatalogComplete(): ValidationCheckResult = check(
        id = "TRAIN_001",
        description = "Communication lesson catalog contains ${TrainingMetadata.COMMUNICATION_LESSON_COUNT} lessons",
        passed = TrainingLessonCatalog.communicationLessons.size == TrainingMetadata.COMMUNICATION_LESSON_COUNT,
        remediation = "Ensure TrainingLessonCatalog defines 20 communication fundamentals."
    )

    private fun communicationLessonsHaveSequences(): ValidationCheckResult {
        val invalid = TrainingLessonCatalog.communicationLessons.filter {
            it.left < 0 || it.right < 0 || it.vocabularyId.isBlank()
        }
        return check(
            id = "TRAIN_002",
            description = "Every communication lesson has valid blink sequence and vocabulary ID",
            passed = invalid.isEmpty(),
            remediation = "Fix invalid lessons: ${invalid.map { it.id }.joinToString()}"
        )
    }

    private fun navigationLessonsDefined(): ValidationCheckResult = check(
        id = "TRAIN_003",
        description = "Navigation lesson catalog contains ${TrainingMetadata.NAVIGATION_LESSON_COUNT} lessons",
        passed = TrainingLessonCatalog.navigationLessons.size == TrainingMetadata.NAVIGATION_LESSON_COUNT,
        remediation = "Define all required navigation training steps."
    )

    private fun encouragementNeverPunishes(): ValidationCheckResult {
        val forbidden = LisaPersonalityProfile.DEFAULT_FORBIDDEN_PHRASES
        val samples = buildList {
            (0..15).forEach { seed ->
                add(EncouragementEngine.successMessage(seed))
                add(EncouragementEngine.retryMessage(seed))
                add(EncouragementEngine.almostMessage(seed))
            }
            addAll(EncouragementEngine.welcomeNarration())
            addAll(EncouragementEngine.completionNarration())
        }
        val hasForbidden = forbidden.any { word ->
            samples.any { it.contains(word, ignoreCase = true) }
        }
        return check(
            id = "TRAIN_004",
            description = "Encouragement engine never uses punitive language (via Personality Engine)",
            passed = !hasForbidden,
            remediation = "Remove punitive words from Personality Engine dialogue catalogs."
        )
    }

    private fun persistenceKeysDocumented(): ValidationCheckResult = check(
        id = "TRAIN_005",
        description = "TrainingProgressStore defines persistence for progress and preferences",
        passed = TrainingProgressStore::class.java.declaredFields.any { it.name.contains("KEY_") },
        remediation = "Implement TrainingProgressStore persistence keys."
    )

    private fun metadataConsistent(): ValidationCheckResult = check(
        id = "TRAIN_006",
        description = "TrainingMetadata version and counts match catalog",
        passed = TrainingMetadata.VERSION == "V1" &&
            TrainingMetadata.COMMUNICATION_LESSON_COUNT == TrainingLessonCatalog.communicationLessons.size,
        remediation = "Align TrainingMetadata with TrainingLessonCatalog."
    )

    private fun phasesDefined(): ValidationCheckResult = check(
        id = "TRAIN_007",
        description = "Learning Journey phases defined (7 stages + first launch + skip)",
        passed = TrainingPhase.entries.containsAll(
            listOf(
                TrainingPhase.FirstLaunchChoice,
                TrainingPhase.Welcome,
                TrainingPhase.Setup,
                TrainingPhase.Calibration,
                TrainingPhase.CommunicationLesson,
                TrainingPhase.CommunicationMastery,
                TrainingPhase.NavigationLesson,
                TrainingPhase.Completion,
                TrainingPhase.SkipConfirm
            )
        ),
        remediation = "Define missing TrainingPhase values."
    )

    private fun totalLessonCount(): ValidationCheckResult = check(
        id = "TRAIN_008",
        description = "Total lesson count equals fundamentals, mastery rounds, and navigation",
        passed = TrainingLessonCatalog.totalLessons ==
            TrainingMetadata.COMMUNICATION_LESSON_COUNT +
            TrainingMetadata.MASTERY_ROUND_COUNT +
            TrainingMetadata.NAVIGATION_LESSON_COUNT,
        remediation = "Fix totalLessons calculation in TrainingLessonCatalog."
    )

    private fun check(
        id: String,
        description: String,
        passed: Boolean,
        remediation: String? = null,
        observation: String? = null
    ): ValidationCheckResult = ValidationCheckResult(
        checkId = id,
        description = description,
        passed = passed,
        remediation = remediation,
        observation = observation
    )
}
