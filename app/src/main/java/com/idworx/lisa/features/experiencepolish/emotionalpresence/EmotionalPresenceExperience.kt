package com.idworx.lisa.features.experiencepolish.emotionalpresence

import com.idworx.lisa.features.personality.dialogue.DefaultDialogueCatalog
import com.idworx.lisa.features.personality.model.PresenceMoment

object EmotionalPresenceExperience {

    const val PHASE_NAME: String = "LISA Emotional Presence V1"

    fun sessionOpeningDialogues(): List<String> = texts(PresenceMoment.SessionOpening)
    fun warmReturnDialogues(): List<String> = texts(PresenceMoment.WarmReturnGreeting)
    fun longPauseDialogues(): List<String> = texts(PresenceMoment.LongPauseEncouragement)
    fun caregiverReassuranceDialogues(): List<String> = texts(PresenceMoment.CaregiverReassurance)
    fun fatigueCheckInDialogues(): List<String> = texts(PresenceMoment.FatigueCheckIn)
    fun emotionalMilestoneDialogues(): List<String> = texts(PresenceMoment.EmotionalMilestone)

    private fun texts(moment: PresenceMoment): List<String> {
        val tag = when (moment) {
            PresenceMoment.SessionOpening -> "presence_session_open"
            PresenceMoment.WarmReturnGreeting -> "presence_return_warm"
            PresenceMoment.LongPauseEncouragement -> "presence_long_pause"
            PresenceMoment.CaregiverReassurance -> "presence_caregiver_reassure"
            PresenceMoment.FatigueCheckIn -> "presence_fatigue_checkin"
            PresenceMoment.EmotionalMilestone -> "presence_emotional_milestone"
        }
        return DefaultDialogueCatalog.all("en")
            .filter { it.contextTags.contains(tag) }
            .sortedBy { it.id }
            .map { it.text }
    }
}
