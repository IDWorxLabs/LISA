package com.idworx.lisa.features.onboardingguide.navigation

import com.idworx.lisa.features.experiencepolish.patientcommunicationcoach.PatientCommunicationCoachEngine
import com.idworx.lisa.features.onboardingguide.coach.CommunicationCoachEngine
import com.idworx.lisa.features.onboardingguide.lessons.TrainingLessonCatalog
import com.idworx.lisa.features.onboardingguide.metadata.TrainingMetadata
import com.idworx.lisa.features.onboardingguide.model.NavigationAction
import com.idworx.lisa.features.onboardingguide.model.TrainingPhase
import com.idworx.lisa.features.onboardingguide.model.TrainingProgress
import com.idworx.lisa.features.onboardingguide.services.AdaptiveLearningService
import com.idworx.lisa.features.onboardingguide.services.TrainingDialogueContext
import com.idworx.lisa.features.onboardingguide.state.TrainingEvent
import com.idworx.lisa.features.onboardingguide.state.TrainingFeedback
import com.idworx.lisa.features.personality.engine.LisaPersonalityEngines
import com.idworx.lisa.features.silentwelcome.LisaSpeechPolicy

class GuidedTrainingNavigator {

    fun reduce(progress: TrainingProgress, event: TrainingEvent): TrainingProgress = when (event) {
        TrainingEvent.BeginLearning -> progress.copy(
            firstLaunchChoiceMade = true,
            tutorialStarted = true,
            currentPhase = TrainingPhase.Setup,
            sessionLessonsThisVisit = 0,
            currentLessonSuccessCount = 0
        ).let { clearPracticeFlags(it) }

        TrainingEvent.SkipTutorial -> progress.copy(
            firstLaunchChoiceMade = true,
            currentPhase = TrainingPhase.SkipConfirm
        )

        TrainingEvent.ConfirmSkip -> progress.copy(
            tutorialSkipped = true,
            firstLaunchChoiceMade = true,
            currentPhase = TrainingPhase.Completion,
            practiceModeOnly = false,
            practiceCommunication = false,
            practiceNavigation = false
        )

        TrainingEvent.ReturnToTutorial -> progress.copy(
            currentPhase = TrainingPhase.Welcome,
            tutorialSkipped = false
        )

        TrainingEvent.AdvanceSetup -> progress

        TrainingEvent.CompleteSetup -> progress.copy(
            currentPhase = TrainingPhase.CommunicationLesson,
            communicationLessonIndex = 0,
            calibrationCompleted = true
        )

        TrainingEvent.CalibrationComplete -> progress.copy(
            calibrationCompleted = true,
            currentPhase = TrainingPhase.CommunicationLesson,
            communicationLessonIndex = 0
        )

        TrainingEvent.CalibrationRetry -> progress.copy(
            calibrationCompleted = false
        )

        TrainingEvent.FundamentalsComplete -> if (LisaSpeechPolicy.PHRASE_TRANSLATION_ONLY) {
            progress.copy(
                currentPhase = TrainingPhase.NavigationLesson,
                navigationLessonIndex = 0
            )
        } else {
            progress.copy(
                currentPhase = TrainingPhase.CommunicationMastery,
                masteryRoundIndex = 0,
                masteryPhraseOrder = TrainingLessonCatalog.buildMasteryOrder(
                    progress.statistics.successfulAttempts + progress.communicationLessonIndex
                )
            )
        }

        TrainingEvent.MasteryComplete -> progress.copy(
            currentPhase = TrainingPhase.NavigationLesson,
            navigationLessonIndex = 0
        )

        TrainingEvent.SequenceSuccess -> {
            val withAttempt = progress.copy(
                statistics = progress.statistics.recordAttempt(success = true),
                currentLessonSuccessCount = progress.currentLessonSuccessCount + 1
            )
            if (CommunicationCoachEngine.shouldAdvanceAfterSuccess(withAttempt)) {
                markCurrentLessonComplete(
                    withAttempt.copy(
                        sessionLessonsThisVisit = withAttempt.sessionLessonsThisVisit + 1,
                        currentLessonSuccessCount = 0
                    )
                )
            } else {
                withAttempt
            }
        }

        TrainingEvent.SequenceAlmost -> progress.copy(
            statistics = progress.statistics.recordAttempt(success = false)
        )

        TrainingEvent.SequenceRetry -> progress.copy(
            statistics = progress.statistics.recordAttempt(success = false)
        )

        TrainingEvent.AdvanceLesson -> advanceToNextLesson(progress)

        TrainingEvent.PracticeAgain -> progress.copy(
            statistics = progress.statistics.copy(consecutiveFailures = 0),
            currentLessonSuccessCount = 0
        )

        TrainingEvent.ContinueLesson -> progress.copy(
            statistics = progress.statistics.copy(consecutiveFailures = 0)
        )

        TrainingEvent.PatienceAcknowledged -> progress.copy(
            statistics = progress.statistics.copy(consecutiveFailures = 0)
        )

        TrainingEvent.WelcomeNarrationComplete -> progress.copy(
            tutorialStarted = true,
            currentPhase = TrainingPhase.Setup,
            sessionLessonsThisVisit = 0,
            currentLessonSuccessCount = 0
        ).let { clearPracticeFlags(it) }

        TrainingEvent.CompletionNarrationComplete -> progress.copy(
            tutorialCompleted = true,
            certifiedCommunicator = true,
            practiceModeOnly = false,
            practiceCommunication = false,
            practiceNavigation = false
        )

        TrainingEvent.CompleteTraining -> progress.copy(
            tutorialCompleted = true,
            certifiedCommunicator = true,
            currentPhase = TrainingPhase.Completion,
            practiceModeOnly = false,
            practiceCommunication = false,
            practiceNavigation = false
        )

        TrainingEvent.StartUsingLisa -> progress.copy(
            tutorialCompleted = true,
            certifiedCommunicator = true,
            practiceModeOnly = false,
            practiceCommunication = false,
            practiceNavigation = false
        )

        TrainingEvent.ReplayTutorial -> TrainingProgress(
            preferences = progress.preferences
        ).copy(
            tutorialStarted = true,
            firstLaunchChoiceMade = true,
            currentPhase = TrainingPhase.Welcome,
            sessionLessonsThisVisit = 0,
            currentLessonSuccessCount = 0
        )

        TrainingEvent.ResetProgress -> TrainingProgress(preferences = progress.preferences)

        TrainingEvent.PracticeCommunication -> progress.copy(
            practiceModeOnly = true,
            practiceCommunication = true,
            practiceNavigation = false,
            currentPhase = TrainingPhase.CommunicationLesson,
            communicationLessonIndex = 0,
            tutorialStarted = true,
            firstLaunchChoiceMade = true
        )

        TrainingEvent.PracticeNavigation -> progress.copy(
            practiceModeOnly = true,
            practiceNavigation = true,
            practiceCommunication = false,
            currentPhase = TrainingPhase.NavigationLesson,
            navigationLessonIndex = 0,
            tutorialStarted = true,
            firstLaunchChoiceMade = true
        )

        TrainingEvent.ExitPractice -> progress.copy(
            practiceModeOnly = false,
            practiceCommunication = false,
            practiceNavigation = false
        )

        is TrainingEvent.NavigationActionCompleted -> {
            val lesson = TrainingLessonCatalog.navigationLessonAt(progress.navigationLessonIndex)
            if (lesson?.id == event.actionId) {
                markCurrentLessonComplete(progress)
            } else {
                progress
            }
        }

        is TrainingEvent.UpdatePreferences -> progress.copy(preferences = event.transform(progress.preferences))

        TrainingEvent.PauseNarration,
        TrainingEvent.ResumeNarration,
        TrainingEvent.RepeatNarration,
        TrainingEvent.SkipNarration -> progress
    }

    fun feedbackFor(
        event: TrainingEvent,
        seed: Int,
        progress: TrainingProgress = TrainingProgress()
    ): Pair<TrainingFeedback, String?> {
        val uiState = com.idworx.lisa.features.onboardingguide.state.GuidedTrainingUiState(progress = progress)
        val ctx = TrainingDialogueContext.from(
            uiState,
            TrainingDialogueContext.DialogueContextExtras(deterministicSeed = seed)
        )
        val personality = LisaPersonalityEngines.default
        return when (event) {
            TrainingEvent.SequenceSuccess -> TrainingFeedback.Success to
                personality.generateEncouragement(ctx).text
            TrainingEvent.SequenceAlmost -> TrainingFeedback.Almost to
                personality.generateAlmostMessage(ctx).text
            TrainingEvent.SequenceRetry -> TrainingFeedback.Retry to
                personality.generateComfort(ctx).text
            else -> TrainingFeedback.None to null
        }
    }

    fun adaptiveOffer(progress: TrainingProgress) =
        AdaptiveLearningService.evaluate(
            progress,
            com.idworx.lisa.features.calibrationreliability.engine.CalibrationReliabilityEngines.default
        )

    fun expectedNavigationAction(progress: TrainingProgress): NavigationAction? =
        TrainingLessonCatalog.navigationLessonAt(progress.navigationLessonIndex)?.action

    fun navigationActionId(progress: TrainingProgress): String? =
        TrainingLessonCatalog.navigationLessonAt(progress.navigationLessonIndex)?.id

    private fun markCurrentLessonComplete(progress: TrainingProgress): TrainingProgress =
        when (progress.currentPhase) {
            TrainingPhase.CommunicationLesson -> {
                val lesson = TrainingLessonCatalog.communicationLessonAt(progress.communicationLessonIndex)
                val withComplete = lesson?.let { progress.communicationLessonComplete(it.id) } ?: progress
                advanceToNextLesson(withComplete)
            }
            TrainingPhase.CommunicationMastery -> {
                val lesson = TrainingLessonCatalog.masteryLessonAt(
                    progress.masteryRoundIndex,
                    progress.masteryPhraseOrder
                )
                val withComplete = lesson?.let { progress.communicationLessonComplete("mastery_${it.id}") } ?: progress
                advanceToNextLesson(withComplete)
            }
            TrainingPhase.NavigationLesson -> {
                val lesson = TrainingLessonCatalog.navigationLessonAt(progress.navigationLessonIndex)
                val withComplete = lesson?.let { progress.navigationLessonComplete(it.id) } ?: progress
                advanceToNextLesson(withComplete)
            }
            else -> progress
        }

    private fun advanceToNextLesson(progress: TrainingProgress): TrainingProgress = when (progress.currentPhase) {
        TrainingPhase.CommunicationLesson -> {
            val nextIndex = progress.communicationLessonIndex + 1
            val threshold = if (LisaSpeechPolicy.PHRASE_TRANSLATION_ONLY) {
                TrainingMetadata.GUIDED_LEARNING_ESSENTIAL_PHRASE_COUNT
            } else {
                TrainingMetadata.COMMUNICATION_LESSON_COUNT
            }
            if (nextIndex >= threshold) {
                if (progress.practiceCommunication && progress.practiceModeOnly) {
                    progress.copy(practiceModeOnly = false, practiceCommunication = false)
                } else {
                    reduce(progress, TrainingEvent.FundamentalsComplete)
                }
            } else {
                progress.copy(communicationLessonIndex = nextIndex)
            }
        }
        TrainingPhase.CommunicationMastery -> {
            val nextRound = progress.masteryRoundIndex + 1
            if (nextRound >= TrainingMetadata.MASTERY_ROUND_COUNT) {
                if (progress.practiceCommunication && progress.practiceModeOnly) {
                    progress.copy(practiceModeOnly = false, practiceCommunication = false)
                } else {
                    reduce(progress, TrainingEvent.MasteryComplete)
                }
            } else {
                progress.copy(masteryRoundIndex = nextRound)
            }
        }
        TrainingPhase.NavigationLesson -> {
            val nextIndex = progress.navigationLessonIndex + 1
            if (nextIndex >= TrainingMetadata.NAVIGATION_LESSON_COUNT) {
                if (progress.practiceNavigation && progress.practiceModeOnly) {
                    progress.copy(practiceModeOnly = false, practiceNavigation = false)
                } else {
                    progress.copy(
                        tutorialCompleted = true,
                        certifiedCommunicator = true,
                        currentPhase = TrainingPhase.Completion
                    )
                }
            } else {
                progress.copy(navigationLessonIndex = nextIndex)
            }
        }
        else -> progress
    }

    private fun clearPracticeFlags(progress: TrainingProgress): TrainingProgress =
        progress.copy(
            practiceModeOnly = false,
            practiceCommunication = false,
            practiceNavigation = false
        )
}
