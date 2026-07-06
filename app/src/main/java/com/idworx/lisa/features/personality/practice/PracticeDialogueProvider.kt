package com.idworx.lisa.features.personality.practice

import com.idworx.lisa.features.personality.engine.LisaDialogueSelector
import com.idworx.lisa.features.personality.model.DialogueCategory
import com.idworx.lisa.features.personality.model.DialogueContext
import com.idworx.lisa.features.personality.model.LisaDialogue

class PracticeDialogueProvider(
    private val selector: LisaDialogueSelector
) {
    fun generate(context: DialogueContext): LisaDialogue =
        selector.select(DialogueCategory.Practice, context)
}
