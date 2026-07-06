package com.idworx.lisa.features.experiencepolish.patientcommunicationcoach.model

/**
 * Caregiver-visible coaching progress during communication lessons.
 */
data class CoachUiState(
    val lessonNumber: Int,
    val totalLessons: Int,
    val difficultyLevel: Int,
    val difficultyLabel: String,
    val successfulAttempts: Int,
    val consecutiveFailures: Int,
    val caregiverHint: String,
    val pacingMode: CoachPacingMode,
    val showRepeatSuggestion: Boolean,
    val showSlowDownHint: Boolean,
    val showDifficultyBridge: Boolean,
    val phrasesCompletedCount: Int = 0,
    val sessionLessonsCompleted: Int = 0,
    val successesOnCurrentPhrase: Int = 0,
    val successesNeededToAdvance: Int = 1,
    val dailyEssentialsCompleted: Int = 0,
    val dailyEssentialsTotal: Int = 0
)

enum class CoachPacingMode {
    Normal,
    Slow,
    RestSuggested
}
