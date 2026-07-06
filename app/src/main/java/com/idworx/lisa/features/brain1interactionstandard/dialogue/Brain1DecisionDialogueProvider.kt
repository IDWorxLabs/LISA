package com.idworx.lisa.features.brain1interactionstandard.dialogue

import com.idworx.lisa.features.brain1interactionstandard.model.Brain1DecisionKind
import com.idworx.lisa.features.personality.dialogue.DefaultDialogueCatalog
import com.idworx.lisa.features.personality.model.LisaDialogue

object Brain1DecisionDialogueProvider {

    fun firstLaunchPrompt(): List<String> = texts("b1_first_launch")

    fun repeatChoice(label: String): List<String> =
        texts("b1_repeat_choice").map { it.replace("{choice}", label) }

    fun confirmPrompt(): List<String> = texts("b1_confirm")

    fun chooseAgainPrompt(): List<String> = texts("b1_choose_again")

    fun skipWarning(): List<String> = texts("b1_skip_warning")

    fun emergencyRepeat(): List<String> = texts("b1_emergency_repeat")

    fun resetRepeat(): List<String> = texts("b1_reset_repeat")

    fun recalibrationPrompt(): List<String> = texts("b1_recal_prompt")

    fun calibrationIntroAfterConfirm(): List<String> = texts("b1_cal_intro")

    fun calibrationDotNarration(index: Int): String =
        texts("b1_cal_dot").getOrElse(index) { "Please look at the dot." }

    fun calibrationDotSuccess(): List<String> = texts("b1_cal_success")

    private fun texts(tag: String): List<String> =
        DefaultDialogueCatalog.all("en")
            .filter { it.contextTags.contains(tag) }
            .sortedBy { it.id }
            .map { it.text }

    fun asDialogues(lines: List<String>): List<LisaDialogue> =
        lines.mapIndexed { i, text ->
            LisaDialogue(
                id = "b1_runtime_$i",
                text = text,
                category = com.idworx.lisa.features.personality.model.DialogueCategory.Instruction,
                locale = "en"
            )
        }

    fun repeatForKind(kind: Brain1DecisionKind, label: String): List<String> = when (kind) {
        Brain1DecisionKind.FirstLaunchSkipWorkspace ->
            skipWarning() + repeatChoice(label) + confirmPrompt() + chooseAgainPrompt()
        Brain1DecisionKind.EmergencyMode ->
            emergencyRepeat() + confirmPrompt() + chooseAgainPrompt()
        Brain1DecisionKind.ResetLearningProgress ->
            resetRepeat() + confirmPrompt() + chooseAgainPrompt()
        Brain1DecisionKind.ReplayLearning ->
            repeatChoice(label) + confirmPrompt() + chooseAgainPrompt()
        Brain1DecisionKind.Recalibration ->
            recalibrationPrompt() + confirmPrompt() + chooseAgainPrompt()
        else -> repeatChoice(label) + confirmPrompt() + chooseAgainPrompt()
    }
}
