package com.idworx.lisa.features.personality.greetings

import com.idworx.lisa.features.personality.dialogue.DefaultDialogueCatalog
import com.idworx.lisa.features.personality.engine.LisaDialogueSelector
import com.idworx.lisa.features.personality.model.DialogueCategory
import com.idworx.lisa.features.personality.model.DialogueContext
import com.idworx.lisa.features.personality.model.LisaDialogue

class GreetingDialogueProvider(
    private val selector: LisaDialogueSelector
) {
    fun generate(context: DialogueContext): LisaDialogue {
        val tag = when {
            context.returningUser || context.daysSinceLastSession > 0 -> "returning"
            else -> "first_launch"
        }
        return selector.select(DialogueCategory.Greeting, context) {
            it.contextTags.contains(tag) || it.contextTags.isEmpty()
        }
    }

    fun generateFirstLaunchSequence(context: DialogueContext): List<LisaDialogue> =
        DefaultDialogueCatalog.forCategory(DialogueCategory.Greeting, context.locale)
            .filter { it.contextTags.contains("first_conversation") || it.contextTags.contains("first_launch") }
            .sortedBy { it.id }
            .distinctBy { it.id }

    fun generateReturningSequence(context: DialogueContext): List<LisaDialogue> =
        selector.selectSequence(DialogueCategory.Greeting, context) {
            it.contextTags.contains("returning")
        }.take(3)
}
