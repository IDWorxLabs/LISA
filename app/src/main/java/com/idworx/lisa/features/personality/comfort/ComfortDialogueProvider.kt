package com.idworx.lisa.features.personality.comfort

import com.idworx.lisa.features.personality.engine.LisaDialogueSelector
import com.idworx.lisa.features.personality.model.DialogueCategory
import com.idworx.lisa.features.personality.model.DialogueContext
import com.idworx.lisa.features.personality.model.LisaDialogue

class ComfortDialogueProvider(
    private val selector: LisaDialogueSelector
) {
    fun generate(context: DialogueContext): LisaDialogue {
        val filter: (LisaDialogue) -> Boolean = if (context.consecutiveFailures >= 3) {
            { it.contextTags.contains("struggling") || it.id.startsWith("com_") }
        } else {
            { it.id.startsWith("com_") }
        }
        return selector.select(DialogueCategory.Comfort, context, filter)
    }
}
