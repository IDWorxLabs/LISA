package com.idworx.lisa.features.personality.presence

import com.idworx.lisa.features.personality.engine.LisaDialogueSelector
import com.idworx.lisa.features.personality.model.DialogueCategory
import com.idworx.lisa.features.personality.model.DialogueContext
import com.idworx.lisa.features.personality.model.LisaDialogue
import com.idworx.lisa.features.personality.model.PresenceMoment

class EmotionalPresenceDialogueProvider(
    private val selector: LisaDialogueSelector
) {
    fun generate(context: DialogueContext, moment: PresenceMoment): LisaDialogue =
        selector.select(categoryFor(moment), context) { tagFilter(moment, it) }

    fun generateSequence(
        context: DialogueContext,
        moment: PresenceMoment,
        maxLines: Int
    ): List<LisaDialogue> =
        selector.selectSequence(categoryFor(moment), context) { tagFilter(moment, it) }
            .distinctBy { it.id }
            .take(maxLines.coerceAtLeast(1))

    private fun categoryFor(moment: PresenceMoment): DialogueCategory = when (moment) {
        PresenceMoment.SessionOpening -> DialogueCategory.Welcome
        PresenceMoment.WarmReturnGreeting -> DialogueCategory.ReturnMessage
        PresenceMoment.LongPauseEncouragement -> DialogueCategory.Waiting
        PresenceMoment.CaregiverReassurance -> DialogueCategory.Comfort
        PresenceMoment.FatigueCheckIn -> DialogueCategory.Comfort
        PresenceMoment.EmotionalMilestone -> DialogueCategory.MilestoneCelebration
    }

    private fun tagFilter(moment: PresenceMoment, dialogue: LisaDialogue): Boolean =
        dialogue.contextTags.contains(tagFor(moment))

    private fun tagFor(moment: PresenceMoment): String = when (moment) {
        PresenceMoment.SessionOpening -> "presence_session_open"
        PresenceMoment.WarmReturnGreeting -> "presence_return_warm"
        PresenceMoment.LongPauseEncouragement -> "presence_long_pause"
        PresenceMoment.CaregiverReassurance -> "presence_caregiver_reassure"
        PresenceMoment.FatigueCheckIn -> "presence_fatigue_checkin"
        PresenceMoment.EmotionalMilestone -> "presence_emotional_milestone"
    }
}
