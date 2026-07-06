package com.idworx.lisa.features.onboardingguide.state

import com.idworx.lisa.features.onboardingguide.coach.CaregiverProgressSnapshot
import com.idworx.lisa.features.onboardingguide.coach.CoachLessonDecision
import com.idworx.lisa.features.experiencepolish.patientcommunicationcoach.model.CoachUiState
import com.idworx.lisa.features.onboardingguide.model.TrainingPhase
import com.idworx.lisa.features.onboardingguide.model.TrainingProgress
import com.idworx.lisa.features.onboardingguide.services.AdaptiveLearningOffer

enum class TrainingFeedback {
    None,
    Success,
    Almost,
    Retry
}

/** Live blink feedback during Guided Learning communication lessons. */
data class LessonInteractionState(
    val detectedProgress: String? = null,
    val successVisualMessage: String? = null,
    val retryVisualMessage: String? = null,
    val wrongEyeMessage: String? = null,
    val liveLeftBlinks: Int = 0,
    val liveRightBlinks: Int = 0,
    val awaitingSuccessSpeech: Boolean = false
)

data class GuidedTrainingUiState(
    val progress: TrainingProgress = TrainingProgress(),
    val feedback: TrainingFeedback = TrainingFeedback.None,
    val feedbackMessage: String? = null,
    val narrationSpeaking: Boolean = false,
    val showCelebration: Boolean = false,
    val adaptiveOffer: AdaptiveLearningOffer? = null,
    val coachUiState: CoachUiState? = null,
    val caregiverSnapshot: CaregiverProgressSnapshot? = null,
    val coachDecision: CoachLessonDecision? = null,
    val coachInstruction: String? = null,
    val coachPacingBlocked: Boolean = false,
    val setupStep: Int = 0,
    val awaitingNavigationAction: Boolean = false,
    val leftWinkDots: Int = 0,
    val rightWinkDots: Int = 0,
    val calibrationDotIndex: Int = 0,
    val calibrationTotalDots: Int = 5,
    val calibrationPoorRetry: Boolean = false,
    val lessonInteraction: LessonInteractionState = LessonInteractionState(),
    val brain1Decision: com.idworx.lisa.features.brain1interactionstandard.model.Brain1DecisionState =
        com.idworx.lisa.features.brain1interactionstandard.model.Brain1DecisionState()
) {
    val phase: TrainingPhase get() = progress.currentPhase
    val isActive: Boolean get() = !progress.isFinished || progress.practiceModeOnly

    companion object {
        fun fromProgress(progress: TrainingProgress): GuidedTrainingUiState =
            GuidedTrainingUiState(progress = progress)
    }
}

sealed class TrainingEvent {
    data object BeginLearning : TrainingEvent()
    data object SkipTutorial : TrainingEvent()
    data object ConfirmSkip : TrainingEvent()
    data object ReturnToTutorial : TrainingEvent()
    data object AdvanceSetup : TrainingEvent()
    data object CompleteSetup : TrainingEvent()
    data object SequenceSuccess : TrainingEvent()
    data object SequenceAlmost : TrainingEvent()
    data object SequenceRetry : TrainingEvent()
    data object AdvanceLesson : TrainingEvent()
    data object PracticeAgain : TrainingEvent()
    data object ContinueLesson : TrainingEvent()
    data object CompleteTraining : TrainingEvent()
    data object StartUsingLisa : TrainingEvent()
    data object ReplayTutorial : TrainingEvent()
    data object ResetProgress : TrainingEvent()
    data object PracticeCommunication : TrainingEvent()
    data object PracticeNavigation : TrainingEvent()
    data object ExitPractice : TrainingEvent()
    data object PauseNarration : TrainingEvent()
    data object ResumeNarration : TrainingEvent()
    data object RepeatNarration : TrainingEvent()
    data object SkipNarration : TrainingEvent()
    data object WelcomeNarrationComplete : TrainingEvent()
    data object CompletionNarrationComplete : TrainingEvent()
    data object PatienceAcknowledged : TrainingEvent()
    data object CalibrationComplete : TrainingEvent()
    data object CalibrationRetry : TrainingEvent()
    data object FundamentalsComplete : TrainingEvent()
    data object MasteryComplete : TrainingEvent()
    data class NavigationActionCompleted(val actionId: String) : TrainingEvent()
    data class UpdatePreferences(val transform: (com.idworx.lisa.features.onboardingguide.model.TrainingPreferences) ->
        com.idworx.lisa.features.onboardingguide.model.TrainingPreferences) : TrainingEvent()
}
