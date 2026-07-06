package com.idworx.lisa.features.experiencepolish.patientcommunicationcoach

import com.idworx.lisa.features.experiencepolish.emotionalpresence.EmotionalPresenceEngine
import com.idworx.lisa.features.experiencepolish.patientcommunicationcoach.model.CoachPacingMode
import com.idworx.lisa.features.experiencepolish.patientcommunicationcoach.model.CoachUiState
import com.idworx.lisa.features.onboardingguide.coach.CaregiverProgressSnapshot
import com.idworx.lisa.features.onboardingguide.coach.CoachCelebrationTier
import com.idworx.lisa.features.onboardingguide.coach.CoachLessonDecision
import com.idworx.lisa.features.onboardingguide.coach.CoachPacingAction
import com.idworx.lisa.features.onboardingguide.coach.CommunicationCoachEngine
import com.idworx.lisa.features.onboardingguide.model.TrainingProgress
import com.idworx.lisa.features.onboardingguide.services.AdaptiveLearningOffer
import com.idworx.lisa.features.personality.model.AppFeature
import com.idworx.lisa.features.personality.model.DialogueContext

/**
 * Bridges [CommunicationCoachEngine] decisions to experience-polish UI and narration tags.
 */
object PatientCommunicationCoachEngine {

    const val REPEAT_THRESHOLD: Int = 2
    const val SLOW_DOWN_THRESHOLD: Int = 3
    const val FATIGUE_LESSON_INTERVAL: Int = CommunicationCoachEngine.SESSION_BREAK_THRESHOLD
    const val MAX_DIFFICULTY_JUMP: Int = CommunicationCoachEngine.MAX_DIFFICULTY_JUMP
    const val BASE_PACING_DELAY_MS: Long = CommunicationCoachEngine.BASE_REST_MS

    fun celebrationTier(decision: CoachLessonDecision): Int = when (decision.celebrationTier) {
        CoachCelebrationTier.Major -> 3
        CoachCelebrationTier.Milestone -> 2
        CoachCelebrationTier.Standard -> 2
        CoachCelebrationTier.Quiet -> 1
    }

    fun pacingDelayMs(offer: AdaptiveLearningOffer): Long =
        offer.pacingDelayMs

    fun buildCoachUiState(
        progress: TrainingProgress,
        offer: AdaptiveLearningOffer?,
        snapshot: CaregiverProgressSnapshot? = null,
        decision: CoachLessonDecision? = null
    ): CoachUiState? {
        val lesson = CommunicationCoachEngine.currentCommunicationLesson(progress) ?: return null
        val failures = progress.statistics.consecutiveFailures
        val pacing = pacingMode(decision, snapshot, failures)
        val lessonNum = when (progress.currentPhase) {
            com.idworx.lisa.features.onboardingguide.model.TrainingPhase.CommunicationLesson ->
                progress.communicationLessonIndex + 1
            com.idworx.lisa.features.onboardingguide.model.TrainingPhase.CommunicationMastery ->
                com.idworx.lisa.features.onboardingguide.lessons.TrainingLessonCatalog.lessonNumber(
                    progress.currentPhase,
                    progress.communicationLessonIndex,
                    0,
                    progress.masteryRoundIndex
                )
            else -> 0
        }
        val essentialsCompleted = CommunicationCoachEngine.dailyEssentialVocabularyIds.count { vocabId ->
            com.idworx.lisa.features.onboardingguide.lessons.TrainingLessonCatalog.communicationFundamentals
                .any { it.vocabularyId == vocabId && it.id in progress.completedLessonIds }
        }
        return CoachUiState(
            lessonNumber = lessonNum.coerceAtLeast(1),
            totalLessons = com.idworx.lisa.features.onboardingguide.lessons.TrainingLessonCatalog.communicationFundamentals.size,
            difficultyLevel = lesson.difficultyLevel,
            difficultyLabel = difficultyLabel(lesson.difficultyLevel),
            successfulAttempts = progress.statistics.successfulAttempts,
            consecutiveFailures = failures,
            caregiverHint = offer?.caregiverCoachHint ?: snapshot?.coachSummary ?: presenceCaregiverHint(
                progress,
                pacing
            ) ?: caregiverHint(
                lesson,
                progress,
                CommunicationCoachEngine.previousCommunicationLesson(progress),
                pacing
            ),
            pacingMode = pacing,
            showRepeatSuggestion = decision?.shouldRepeatPhrase == true || offer?.showRepeatPhrase == true,
            showSlowDownHint = decision?.pacingAction == CoachPacingAction.SlowDown || failures >= SLOW_DOWN_THRESHOLD,
            showDifficultyBridge = CommunicationCoachEngine.gesturePatternBridgeNeeded(
                CommunicationCoachEngine.previousCommunicationLesson(progress),
                lesson
            ),
            phrasesCompletedCount = progress.completedLessonIds.size,
            sessionLessonsCompleted = progress.sessionLessonsThisVisit,
            successesOnCurrentPhrase = progress.currentLessonSuccessCount,
            successesNeededToAdvance = CommunicationCoachEngine.successesNeededToAdvance(progress.currentPhase),
            dailyEssentialsCompleted = essentialsCompleted,
            dailyEssentialsTotal = CommunicationCoachEngine.dailyEssentialVocabularyIds.size
        )
    }

    fun enrichAdaptiveOffer(
        progress: TrainingProgress,
        base: AdaptiveLearningOffer
    ): AdaptiveLearningOffer {
        val failures = progress.statistics.consecutiveFailures
        val lesson = CommunicationCoachEngine.currentCommunicationLesson(progress)
        val previous = CommunicationCoachEngine.previousCommunicationLesson(progress)
        val pacing = pacingMode(null, null, failures)
        return base.copy(
            showRepeatPhrase = failures >= REPEAT_THRESHOLD && failures < SLOW_DOWN_THRESHOLD,
            showPatiencePrompt = base.showPatiencePrompt || failures >= SLOW_DOWN_THRESHOLD,
            caregiverCoachHint = lesson?.let {
                presenceCaregiverHint(progress, pacing) ?: caregiverHint(it, progress, previous, pacing)
            },
            pacingDelayMs = CommunicationCoachEngine.instructionPacingDelay(progress, base)
        )
    }

    private fun presenceCaregiverHint(
        progress: TrainingProgress,
        pacingMode: CoachPacingMode
    ): String? = EmotionalPresenceEngine.caregiverReassurance(
        DialogueContext(
            feature = AppFeature.GuidedLearning,
            locale = progress.preferences.narrationLanguage,
            currentLessonIndex = progress.communicationLessonIndex,
            completedLessonCount = progress.completedLessonIds.size,
            consecutiveFailures = progress.statistics.consecutiveFailures,
            caregiverVisible = true,
            fatigueSuggested = pacingMode == CoachPacingMode.RestSuggested
        )
    )

    private fun pacingMode(
        decision: CoachLessonDecision?,
        snapshot: CaregiverProgressSnapshot?,
        failures: Int
    ): CoachPacingMode = when {
        decision?.pacingAction == CoachPacingAction.SuggestBreak || snapshot?.suggestBreak == true ->
            CoachPacingMode.RestSuggested
        decision?.pacingAction == CoachPacingAction.SlowDown || failures >= SLOW_DOWN_THRESHOLD ->
            CoachPacingMode.Slow
        else -> CoachPacingMode.Normal
    }

    private fun difficultyLabel(level: Int): String = when (level) {
        1 -> "Daily phrase"
        2 -> "Everyday"
        3 -> "Building"
        4 -> "Advanced"
        else -> "Expert"
    }

    private fun caregiverHint(
        lesson: com.idworx.lisa.features.onboardingguide.model.CommunicationLesson,
        progress: TrainingProgress,
        previousLesson: com.idworx.lisa.features.onboardingguide.model.CommunicationLesson?,
        pacingMode: CoachPacingMode
    ): String = when {
        pacingMode == CoachPacingMode.RestSuggested ->
            "Short break suggested — Lisa will wait."
        progress.currentLessonSuccessCount == 1 &&
            !CommunicationCoachEngine.shouldAdvanceAfterSuccess(progress) ->
            "First success — Lisa will repeat before advancing."
        progress.statistics.consecutiveFailures >= SLOW_DOWN_THRESHOLD ->
            "Slow mode — extra time between tries."
        progress.statistics.consecutiveFailures >= REPEAT_THRESHOLD ->
            "Lisa may repeat this phrase — that's normal."
        CommunicationCoachEngine.gesturePatternBridgeNeeded(previousLesson, lesson) ->
            "New blink pattern — one step at a time."
        lesson.difficultyLevel <= 2 ->
            "Daily phrase · ${lesson.left + lesson.right} blinks"
        else ->
            "${difficultyLabel(lesson.difficultyLevel)} · ${lesson.left + lesson.right} blinks"
    }
}
