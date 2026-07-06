package com.idworx.lisa.features.onboardingguide.services

import com.idworx.lisa.features.onboardingguide.model.TrainingPhase
import com.idworx.lisa.features.onboardingguide.state.GuidedTrainingUiState
import com.idworx.lisa.features.personality.model.AppFeature
import com.idworx.lisa.features.personality.model.DialogueContext
import com.idworx.lisa.features.personality.model.MilestoneType

object TrainingDialogueContext {

    fun from(state: GuidedTrainingUiState, extras: DialogueContextExtras = DialogueContextExtras()): DialogueContext {
        val progress = state.progress
        return DialogueContext(
            feature = AppFeature.GuidedLearning,
            locale = progress.preferences.narrationLanguage,
            currentLessonId = extras.lessonId,
            currentLessonIndex = when (progress.currentPhase) {
                TrainingPhase.CommunicationLesson -> progress.communicationLessonIndex
                TrainingPhase.NavigationLesson -> progress.navigationLessonIndex
                else -> 0
            },
            trainingPhase = progress.currentPhase.name,
            attemptCount = progress.statistics.totalAttempts,
            successCount = progress.statistics.successfulAttempts,
            failureCount = progress.statistics.totalAttempts - progress.statistics.successfulAttempts,
            consecutiveFailures = progress.statistics.consecutiveFailures,
            completedLessonCount = progress.completedLessonIds.size,
            firstSuccessfulBlink = extras.firstSuccessfulBlink,
            firstSpokenPhrase = extras.firstSpokenPhrase,
            returningUser = progress.tutorialStarted && !extras.firstLaunch,
            practiceMode = progress.practiceModeOnly,
            navigationTraining = progress.currentPhase == TrainingPhase.NavigationLesson,
            emergencyTraining = extras.emergencyTraining,
            tutorialSkipped = progress.tutorialSkipped,
            guidedLearningComplete = progress.tutorialCompleted,
            targetPhrase = extras.targetPhrase,
            navigationAction = extras.navigationAction,
            milestoneType = extras.milestoneType,
            celebrationTier = extras.celebrationTier,
            deterministicSeed = extras.deterministicSeed,
            fatigueSuggested = extras.fatigueSuggested,
            caregiverVisible = extras.caregiverVisible
        )
    }

    data class DialogueContextExtras(
        val lessonId: String? = null,
        val targetPhrase: String? = null,
        val navigationAction: String? = null,
        val milestoneType: MilestoneType? = null,
        val celebrationTier: Int = 1,
        val firstSuccessfulBlink: Boolean = false,
        val firstSpokenPhrase: Boolean = false,
        val firstLaunch: Boolean = true,
        val emergencyTraining: Boolean = false,
        val deterministicSeed: Int? = null,
        val fatigueSuggested: Boolean = false,
        val caregiverVisible: Boolean = false
    )
}
