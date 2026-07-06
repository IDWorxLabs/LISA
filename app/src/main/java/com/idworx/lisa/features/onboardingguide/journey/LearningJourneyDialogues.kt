package com.idworx.lisa.features.onboardingguide.journey

import com.idworx.lisa.features.onboardingguide.metadata.TrainingMetadata
import com.idworx.lisa.features.onboardingguide.model.TrainingPhase
import com.idworx.lisa.features.onboardingguide.model.TrainingProgress
import com.idworx.lisa.features.personality.dialogue.DefaultDialogueCatalog
import com.idworx.lisa.features.personality.model.DialogueCategory
import com.idworx.lisa.features.personality.model.LisaDialogue

/**
 * Learning Journey V1 — stage dialogue and transition helpers (Personality Engine catalog).
 */
object LearningJourneyDialogues {

    fun dialoguesForTag(tag: String): List<LisaDialogue> =
        DefaultDialogueCatalog.forCategory(DialogueCategory.Instruction, "en")
            .filter { it.contextTags.contains(tag) }
            .sortedBy { it.id }
            .ifEmpty {
                DefaultDialogueCatalog.forCategory(DialogueCategory.MilestoneCelebration, "en")
                    .filter { it.contextTags.contains(tag) }
                    .sortedBy { it.id }
            }

    fun calibrationIntro(): List<String> = textsForTag("journey_calibration_intro")

    fun calibrationExcellent(): List<String> = textsForTag("journey_calibration_excellent")

    fun calibrationAcceptable(): List<String> = textsForTag("journey_calibration_acceptable")

    fun calibrationPoor(): List<String> = textsForTag("journey_calibration_poor")

    fun fundamentalsComplete(): List<String> = textsForTag("journey_fundamentals_complete")

    fun masteryIntro(): List<String> = textsForTag("journey_mastery_intro")

    fun masteryComplete(): List<String> = textsForTag("journey_mastery_complete")

    fun workspaceIntro(): List<String> = textsForTag("journey_workspace_intro")

    fun workspaceComplete(): List<String> = textsForTag("journey_workspace_complete")

    fun certification(): List<String> = textsForTag("journey_certification")

    private fun textsForTag(tag: String): List<String> =
        DefaultDialogueCatalog.all("en")
            .filter { it.contextTags.contains(tag) }
            .sortedBy { it.id }
            .map { it.text }
}

object LearningJourneyStages {

    fun stageNumber(phase: TrainingPhase): Int = when (phase) {
        TrainingPhase.FirstLaunchChoice -> 0
        TrainingPhase.Welcome -> 1
        TrainingPhase.Setup -> 2
        TrainingPhase.Calibration -> 3
        TrainingPhase.CommunicationLesson -> 4
        TrainingPhase.CommunicationMastery -> 5
        TrainingPhase.NavigationLesson -> 6
        TrainingPhase.Completion -> 7
        TrainingPhase.SkipConfirm -> 0
    }

    fun isJourneyPhase(phase: TrainingPhase): Boolean =
        phase != TrainingPhase.SkipConfirm && phase != TrainingPhase.FirstLaunchChoice

    fun fundamentalsComplete(progress: TrainingProgress): Boolean =
        progress.communicationLessonIndex >= TrainingMetadata.COMMUNICATION_LESSON_COUNT

    fun masteryComplete(progress: TrainingProgress): Boolean =
        progress.masteryRoundIndex >= TrainingMetadata.MASTERY_ROUND_COUNT &&
            progress.masteryPhraseOrder.isNotBlank()
}
