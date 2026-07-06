package com.idworx.lisa.features.experiencepolish.patientcommunicationcoach

import com.idworx.lisa.features.personality.dialogue.DefaultDialogueCatalog

/**
 * Patient communication coach dialogues (Personality Engine catalog).
 */
object PatientCommunicationCoachExperience {

    const val PHASE_NAME: String = "LISA Patient Communication Coach V1"

    fun phraseIntroDialogues(): List<String> = texts("phase_c_coach_phrase")

    fun repeatPhraseDialogues(): List<String> = texts("phase_c_coach_repeat")

    fun slowDownDialogues(): List<String> = texts("phase_c_coach_slow")

    fun difficultyBridgeDialogues(): List<String> = texts("phase_c_coach_bridge")

    fun restSuggestionDialogues(): List<String> = texts("phase_c_coach_rest")

    fun patienceDialogues(): List<String> = texts("phase_c_coach_patience")

    fun minorCelebrationDialogues(): List<String> = texts("phase_c_coach_celebrate_minor")

    fun levelCelebrationDialogues(): List<String> = texts("phase_c_coach_celebrate_level")

    private fun texts(tag: String): List<String> =
        DefaultDialogueCatalog.all("en")
            .filter { it.contextTags.contains(tag) }
            .sortedBy { it.id }
            .map { it.text }
}
