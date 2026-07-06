package com.idworx.lisa.features.personality.instruction

import com.idworx.lisa.features.personality.engine.LisaDialogueSelector
import com.idworx.lisa.features.personality.model.DialogueCategory
import com.idworx.lisa.features.personality.model.DialogueContext
import com.idworx.lisa.features.personality.model.LisaDialogue

class InstructionDialogueProvider(
    private val selector: LisaDialogueSelector
) {
    fun generate(context: DialogueContext): LisaDialogue {
        val filter: (LisaDialogue) -> Boolean = if (context.consecutiveFailures >= 3) {
            { it.contextTags.contains("struggling") || it.id.startsWith("instr_") }
        } else {
            { it.id.startsWith("instr_") }
        }
        val base = selector.select(DialogueCategory.Instruction, context, filter)
        val phrase = context.targetPhrase
        return if (phrase != null && !base.text.contains(phrase, ignoreCase = true)) {
            base.copy(text = "${base.text} Try saying $phrase.")
        } else {
            base
        }
    }
}
