package com.idworx.lisa.features.personality.celebration

import com.idworx.lisa.features.personality.engine.LisaDialogueSelector
import com.idworx.lisa.features.personality.model.DialogueCategory
import com.idworx.lisa.features.personality.model.DialogueContext
import com.idworx.lisa.features.personality.model.LisaDialogue
import com.idworx.lisa.features.personality.model.MilestoneType

class CelebrationDialogueProvider(
    private val selector: LisaDialogueSelector
) {
    fun generate(context: DialogueContext): LisaDialogue = when {
        context.milestoneType != null ->
            selector.selectMilestone(context.milestoneType, context)
        context.firstSpokenPhrase ->
            selector.selectMilestone(MilestoneType.FirstSpokenPhrase, context)
        context.celebrationTier >= 3 ->
            selector.select(DialogueCategory.MilestoneCelebration, context)
        context.celebrationTier >= 2 ->
            selector.select(DialogueCategory.MajorCelebration, context)
        else ->
            selector.select(DialogueCategory.MinorCelebration, context)
    }
}
