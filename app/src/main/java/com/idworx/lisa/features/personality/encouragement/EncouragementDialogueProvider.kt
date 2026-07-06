package com.idworx.lisa.features.personality.encouragement

import com.idworx.lisa.features.personality.dialogue.DialogueCatalog
import com.idworx.lisa.features.personality.engine.LisaDialogueSelector
import com.idworx.lisa.features.personality.model.DialogueCategory
import com.idworx.lisa.features.personality.model.DialogueContext
import com.idworx.lisa.features.personality.model.LisaDialogue

class EncouragementDialogueProvider(
    private val catalog: DialogueCatalog,
    private val selector: LisaDialogueSelector
) {
    fun generate(context: DialogueContext): LisaDialogue =
        selector.select(DialogueCategory.Encouragement, context)

    fun generateAlmost(context: DialogueContext): LisaDialogue =
        selector.select(DialogueCategory.Comfort, context) {
            it.contextTags.contains("almost") || it.id.startsWith("com_")
        }.let { comfort ->
            if (comfort.id.startsWith("fallback")) {
                LisaDialogue(
                    id = "enc_almost",
                    text = "Almost there.",
                    category = DialogueCategory.Encouragement,
                    locale = context.locale
                )
            } else comfort
        }
}
