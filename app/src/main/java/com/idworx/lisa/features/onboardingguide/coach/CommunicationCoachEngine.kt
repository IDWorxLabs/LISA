package com.idworx.lisa.features.onboardingguide.coach

import com.idworx.lisa.features.brain1interactionstandard.model.UniversalInteractionGestures
import com.idworx.lisa.features.onboardingguide.lessons.TrainingLessonCatalog
import com.idworx.lisa.features.onboardingguide.model.CommunicationLesson
import com.idworx.lisa.features.onboardingguide.model.TrainingPhase
import com.idworx.lisa.features.onboardingguide.model.TrainingProgress
import com.idworx.lisa.features.onboardingguide.services.AdaptiveLearningOffer
import com.idworx.lisa.features.onboardingguide.services.AdaptiveLearningService
import com.idworx.lisa.features.silentwelcome.LisaSpeechPolicy

object CommunicationCoachEngine {

    const val SUCCESSES_TO_MASTER: Int = 2
    const val SESSION_BREAK_THRESHOLD: Int = 5
    const val MAX_DIFFICULTY_JUMP: Int = 1
    const val BASE_REST_MS: Long = 2_500L
    private const val SLOWDOWN_EXTRA_MS: Long = 2_000L

    val dailyEssentialVocabularyIds: List<String> = listOf(
        "hello", "yes", "no", "please", "thank_you", "i_need_water", "i_need_help"
    )

    fun successesNeededToAdvance(phase: TrainingPhase): Int = when (phase) {
        TrainingPhase.CommunicationMastery -> 1
        TrainingPhase.CommunicationLesson -> if (LisaSpeechPolicy.PHRASE_TRANSLATION_ONLY) 1 else SUCCESSES_TO_MASTER
        else -> SUCCESSES_TO_MASTER
    }

    fun shouldAdvanceAfterSuccess(progress: TrainingProgress): Boolean =
        progress.currentLessonSuccessCount >= successesNeededToAdvance(progress.currentPhase)

    fun evaluateSuccess(
        progress: TrainingProgress,
        lesson: CommunicationLesson,
        previousLesson: CommunicationLesson?,
        phraseLabel: String
    ): CoachLessonDecision {
        val adaptive = AdaptiveLearningService.evaluate(progress)
        val successesOnPhrase = progress.currentLessonSuccessCount
        val shouldAdvance = shouldAdvanceAfterSuccess(progress)
        val shouldRepeat = !shouldAdvance
        val firstEver = progress.statistics.successfulAttempts == 0
        val isDailyEssential = lesson.vocabularyId in dailyEssentialVocabularyIds
        val levelComplete = shouldAdvance && isLastInDifficultyLevel(progress, lesson)

        val celebrationTier = when {
            firstEver -> CoachCelebrationTier.Major
            levelComplete -> CoachCelebrationTier.Milestone
            shouldAdvance && isDailyEssential -> CoachCelebrationTier.Milestone
            shouldAdvance -> CoachCelebrationTier.Standard
            successesOnPhrase == 1 -> CoachCelebrationTier.Standard
            else -> CoachCelebrationTier.Quiet
        }

        val suggestBreak = shouldAdvance &&
            progress.sessionLessonsThisVisit + 1 >= SESSION_BREAK_THRESHOLD &&
            !progress.tutorialCompleted

        val pacingAction = when {
            adaptive.showPatiencePrompt || progress.statistics.consecutiveFailures >= 2 ->
                CoachPacingAction.SlowDown
            suggestBreak -> CoachPacingAction.SuggestBreak
            shouldRepeat -> CoachPacingAction.RepeatSamePhrase
            shouldAdvance && gesturePatternBridgeNeeded(previousLesson, lesson) ->
                CoachPacingAction.RestBeforeNext
            shouldAdvance -> CoachPacingAction.RestBeforeNext
            else -> CoachPacingAction.Continue
        }

        val pacingDelayMs = instructionPacingDelay(progress, adaptive) +
            (if (pacingAction == CoachPacingAction.RestBeforeNext) BASE_REST_MS else 0L) +
            (if (pacingAction == CoachPacingAction.SlowDown) SLOWDOWN_EXTRA_MS else 0L)

        return CoachLessonDecision(
            shouldAdvance = shouldAdvance,
            shouldRepeatPhrase = shouldRepeat,
            celebrationTier = celebrationTier,
            pacingAction = pacingAction,
            pacingDelayMs = pacingDelayMs,
            learnerMessage = learnerMessageFor(
                pacingAction, shouldRepeat, shouldAdvance, phraseLabel,
                successesOnPhrase, previousLesson, lesson, suggestBreak
            ),
            caregiverNote = caregiverNoteFor(progress, phraseLabel, shouldAdvance, suggestBreak, isDailyEssential),
            slowNarration = pacingAction == CoachPacingAction.SlowDown || adaptive.instructionalPauseMultiplier > 1f,
            showCelebrationOverlay = celebrationTier >= CoachCelebrationTier.Milestone
        )
    }

    fun evaluateRetry(progress: TrainingProgress): CoachLessonDecision {
        val adaptive = AdaptiveLearningService.evaluate(progress)
        return CoachLessonDecision(
            shouldAdvance = false,
            shouldRepeatPhrase = true,
            celebrationTier = CoachCelebrationTier.Quiet,
            pacingAction = if (adaptive.showPatiencePrompt) CoachPacingAction.SlowDown else CoachPacingAction.RepeatSamePhrase,
            pacingDelayMs = instructionPacingDelay(progress, adaptive),
            learnerMessage = "Take your time. I'll wait while you try again.",
            caregiverNote = null,
            slowNarration = adaptive.instructionalPauseMultiplier > 1f,
            showCelebrationOverlay = false
        )
    }

    fun instructionPacingDelay(
        progress: TrainingProgress,
        adaptive: AdaptiveLearningOffer = AdaptiveLearningService.evaluate(progress)
    ): Long {
        val multiplier = adaptive.instructionalPauseMultiplier.coerceAtLeast(1f)
        val fatigueBonus = if (progress.sessionLessonsThisVisit >= 3) 500L else 0L
        return (BASE_REST_MS * multiplier).toLong() + fatigueBonus
    }

    fun gestureJumpAcceptable(from: CommunicationLesson?, to: CommunicationLesson): Boolean {
        if (from == null) return true
        return to.difficultyLevel - from.difficultyLevel in 0..MAX_DIFFICULTY_JUMP
    }

    fun gesturePatternBridgeNeeded(from: CommunicationLesson?, to: CommunicationLesson): Boolean {
        if (from == null) return false
        val sameCount = UniversalInteractionGestures.totalBlinks(from.left, from.right) ==
            UniversalInteractionGestures.totalBlinks(to.left, to.right)
        return sameCount && (from.left != to.left || from.right != to.right || from.blinkOrder != to.blinkOrder)
    }

    fun catalogHasNoOverwhelmingGestureJumps(): Boolean =
        TrainingLessonCatalog.communicationFundamentals.zipWithNext().all { (from, to) ->
            gestureJumpAcceptable(from, to)
        }

    val dailyEssentialsIntroIds: List<String> = listOf(
        "hello", "yes", "no", "please", "thank_you", "i_need_water"
    )

    fun dailyEssentialsFrontLoaded(): Boolean {
        val early = TrainingLessonCatalog.communicationFundamentals
            .take(dailyEssentialsIntroIds.size)
            .map { it.vocabularyId }
        return early == dailyEssentialsIntroIds
    }

    fun caregiverSnapshot(
        progress: TrainingProgress,
        currentLesson: CommunicationLesson?,
        phraseLabel: String?
    ): CaregiverProgressSnapshot {
        val essentialsCompleted = dailyEssentialVocabularyIds.count { vocabId ->
            TrainingLessonCatalog.communicationFundamentals
                .any { it.vocabularyId == vocabId && it.id in progress.completedLessonIds }
        }
        val needed = successesNeededToAdvance(progress.currentPhase)
        val suggestBreak = progress.sessionLessonsThisVisit >= SESSION_BREAK_THRESHOLD - 1 &&
            !progress.tutorialCompleted

        val summary = buildString {
            append(TrainingLessonCatalog.stageLabel(progress.currentPhase))
            phraseLabel?.let { append(" · practicing \"$it\"") }
            append(" · ${progress.completedLessonIds.size} phrases learned")
            if (essentialsCompleted < dailyEssentialVocabularyIds.size) {
                append(" · daily essentials $essentialsCompleted/${dailyEssentialVocabularyIds.size}")
            }
            if (suggestBreak) append(" · rest soon")
        }

        return CaregiverProgressSnapshot(
            stageLabel = TrainingLessonCatalog.stageLabel(progress.currentPhase),
            currentPhraseLabel = phraseLabel,
            phrasesCompletedCount = progress.completedLessonIds.size,
            dailyEssentialsCompleted = essentialsCompleted,
            dailyEssentialsTotal = dailyEssentialVocabularyIds.size,
            sessionLessonCount = progress.sessionLessonsThisVisit,
            successesOnCurrentPhrase = progress.currentLessonSuccessCount,
            successesNeededToAdvance = needed,
            suggestBreak = suggestBreak,
            coachSummary = summary
        )
    }

    fun previousCommunicationLesson(progress: TrainingProgress): CommunicationLesson? = when (progress.currentPhase) {
        TrainingPhase.CommunicationLesson ->
            TrainingLessonCatalog.previousCommunicationLesson(progress.communicationLessonIndex)
        TrainingPhase.CommunicationMastery -> {
            val order = TrainingLessonCatalog.parseMasteryOrder(progress.masteryPhraseOrder)
            val prevRound = progress.masteryRoundIndex - 1
            if (prevRound < 0) null
            else order.getOrNull(prevRound)?.let { TrainingLessonCatalog.communicationFundamentals.getOrNull(it) }
        }
        else -> null
    }

    fun currentCommunicationLesson(progress: TrainingProgress): CommunicationLesson? = when (progress.currentPhase) {
        TrainingPhase.CommunicationLesson ->
            TrainingLessonCatalog.communicationLessonAt(progress.communicationLessonIndex)
        TrainingPhase.CommunicationMastery ->
            TrainingLessonCatalog.masteryLessonAt(progress.masteryRoundIndex, progress.masteryPhraseOrder)
        else -> null
    }

    private fun isLastInDifficultyLevel(progress: TrainingProgress, lesson: CommunicationLesson): Boolean {
        if (progress.currentPhase != TrainingPhase.CommunicationLesson) return false
        val next = TrainingLessonCatalog.communicationLessonAt(progress.communicationLessonIndex + 1)
        return next == null || next.difficultyLevel > lesson.difficultyLevel
    }

    private fun learnerMessageFor(
        pacingAction: CoachPacingAction,
        shouldRepeat: Boolean,
        shouldAdvance: Boolean,
        phraseLabel: String,
        successesOnPhrase: Int,
        previousLesson: CommunicationLesson?,
        lesson: CommunicationLesson,
        suggestBreak: Boolean
    ): String? = when {
        suggestBreak && shouldAdvance ->
            "Wonderful work. We've practiced several phrases — rest when you need to, then we'll continue."
        pacingAction == CoachPacingAction.SlowDown && shouldRepeat ->
            "No rush. Let's try \"$phraseLabel\" once more together."
        shouldRepeat && successesOnPhrase == 1 ->
            "Nice. Let's say \"$phraseLabel\" one more time so it feels natural."
        shouldAdvance && gesturePatternBridgeNeeded(previousLesson, lesson) ->
            "Same number of blinks — just a gentle shift in the pattern. Ready when you are."
        shouldAdvance ->
            "You said \"$phraseLabel\" beautifully. I'll give us a quiet moment before the next phrase."
        else -> null
    }

    private fun caregiverNoteFor(
        progress: TrainingProgress,
        phraseLabel: String,
        shouldAdvance: Boolean,
        suggestBreak: Boolean,
        isDailyEssential: Boolean
    ): String? = when {
        shouldAdvance && suggestBreak ->
            "Caregiver: ${progress.sessionLessonsThisVisit + 1} phrases this session. A short break may help."
        shouldAdvance && isDailyEssential ->
            "Caregiver: daily phrase \"$phraseLabel\" mastered."
        shouldAdvance ->
            "Caregiver: \"$phraseLabel\" ready — ${progress.completedLessonIds.size + 1} phrases total."
        progress.currentLessonSuccessCount == 1 ->
            "Caregiver: first success on \"$phraseLabel\" — Lisa will repeat before advancing."
        else -> null
    }
}
