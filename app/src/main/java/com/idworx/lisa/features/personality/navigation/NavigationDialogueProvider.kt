package com.idworx.lisa.features.personality.navigation

import com.idworx.lisa.features.personality.engine.LisaDialogueSelector
import com.idworx.lisa.features.personality.model.DialogueCategory
import com.idworx.lisa.features.personality.model.DialogueContext
import com.idworx.lisa.features.personality.model.LisaDialogue

class NavigationDialogueProvider(
    private val selector: LisaDialogueSelector
) {
    fun generate(context: DialogueContext): LisaDialogue {
        if (context.emergencyTraining) {
            return selector.select(DialogueCategory.EmergencyTrainingGuidance, context)
        }
        return selector.select(DialogueCategory.NavigationGuidance, context) {
            context.navigationAction == null ||
                it.text.contains(context.navigationAction, ignoreCase = true) ||
                it.id.startsWith("nav_")
        }
    }
}
