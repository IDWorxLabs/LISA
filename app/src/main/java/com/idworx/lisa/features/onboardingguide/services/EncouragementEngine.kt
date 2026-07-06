package com.idworx.lisa.features.onboardingguide.services

import com.idworx.lisa.features.personality.engine.LisaPersonalityEngine
import com.idworx.lisa.features.personality.engine.LisaPersonalityEngines
import com.idworx.lisa.features.personality.model.LisaDialogue
import com.idworx.lisa.features.personality.model.MilestoneType

/**
 * Delegates to [LisaPersonalityEngine]. Retained for backward compatibility within Guided Learning.
 */
object EncouragementEngine {

    private val personality: LisaPersonalityEngine = LisaPersonalityEngines.default

    fun successMessage(seed: Int = 0): String =
        personality.generateEncouragement(
            TrainingDialogueContext.from(
                com.idworx.lisa.features.onboardingguide.state.GuidedTrainingUiState(),
                TrainingDialogueContext.DialogueContextExtras(deterministicSeed = seed)
            )
        ).text

    fun retryMessage(seed: Int = 0): String =
        personality.generateComfort(
            TrainingDialogueContext.from(
                com.idworx.lisa.features.onboardingguide.state.GuidedTrainingUiState(),
                TrainingDialogueContext.DialogueContextExtras(deterministicSeed = seed)
            )
        ).text

    fun almostMessage(seed: Int = 0): String =
        personality.generateAlmostMessage(
            TrainingDialogueContext.from(
                com.idworx.lisa.features.onboardingguide.state.GuidedTrainingUiState(),
                TrainingDialogueContext.DialogueContextExtras(deterministicSeed = seed)
            )
        ).text

    fun lessonCompleteMessage(): String =
        personality.generateCelebration(
            TrainingDialogueContext.from(
                com.idworx.lisa.features.onboardingguide.state.GuidedTrainingUiState(),
                TrainingDialogueContext.DialogueContextExtras(
                    milestoneType = MilestoneType.FirstSpokenPhrase,
                    celebrationTier = 2,
                    firstSpokenPhrase = true
                )
            )
        ).text

    fun welcomeNarration(): List<String> =
        personality.generateGreetingSequence(
            TrainingDialogueContext.from(
                com.idworx.lisa.features.onboardingguide.state.GuidedTrainingUiState(),
                TrainingDialogueContext.DialogueContextExtras(firstLaunch = true)
            )
        ).map { it.text }

    fun completionNarration(): List<String> =
        personality.generateGraduationMessage(
            TrainingDialogueContext.from(
                com.idworx.lisa.features.onboardingguide.state.GuidedTrainingUiState().copy(
                    progress = com.idworx.lisa.features.onboardingguide.state.GuidedTrainingUiState()
                        .progress.copy(tutorialCompleted = true)
                ),
                TrainingDialogueContext.DialogueContextExtras(
                    milestoneType = MilestoneType.GuidedLearningComplete
                )
            )
        ).map { it.text }

    fun skipConfirmQuestion(): String =
        personality.generateSkipConfirmMessage(
            TrainingDialogueContext.from(
                com.idworx.lisa.features.onboardingguide.state.GuidedTrainingUiState()
            )
        ).text

    fun welcomeDialogues(): List<LisaDialogue> =
        personality.generateGreetingSequence(
            TrainingDialogueContext.from(
                com.idworx.lisa.features.onboardingguide.state.GuidedTrainingUiState(),
                TrainingDialogueContext.DialogueContextExtras(firstLaunch = true)
            )
        )

    fun graduationDialogues(): List<LisaDialogue> =
        personality.generateGraduationMessage(
            TrainingDialogueContext.from(
                com.idworx.lisa.features.onboardingguide.state.GuidedTrainingUiState(),
                TrainingDialogueContext.DialogueContextExtras(
                    milestoneType = MilestoneType.GuidedLearningComplete
                )
            )
        )
}
