package com.idworx.lisa.features.onboardingguide.coach

enum class CoachCelebrationTier {
    Quiet,
    Standard,
    Milestone,
    Major
}

enum class CoachPacingAction {
    Continue,
    RepeatSamePhrase,
    RestBeforeNext,
    SuggestBreak,
    SlowDown
}

data class CoachLessonDecision(
    val shouldAdvance: Boolean,
    val shouldRepeatPhrase: Boolean,
    val celebrationTier: CoachCelebrationTier,
    val pacingAction: CoachPacingAction,
    val pacingDelayMs: Long,
    val learnerMessage: String?,
    val caregiverNote: String?,
    val slowNarration: Boolean,
    val showCelebrationOverlay: Boolean
)

data class CaregiverProgressSnapshot(
    val stageLabel: String,
    val currentPhraseLabel: String?,
    val phrasesCompletedCount: Int,
    val dailyEssentialsCompleted: Int,
    val dailyEssentialsTotal: Int,
    val sessionLessonCount: Int,
    val successesOnCurrentPhrase: Int,
    val successesNeededToAdvance: Int,
    val suggestBreak: Boolean,
    val coachSummary: String
)
