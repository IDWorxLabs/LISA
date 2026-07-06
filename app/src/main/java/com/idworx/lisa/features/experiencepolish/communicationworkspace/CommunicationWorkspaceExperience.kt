package com.idworx.lisa.features.experiencepolish.communicationworkspace

import com.idworx.lisa.features.personality.dialogue.DefaultDialogueCatalog

/**
 * Phase B — Communication Workspace daily-use polish (Personality Engine catalog).
 */
object CommunicationWorkspaceExperience {

    const val PHASE_NAME: String = "LISA Experience Polish — Phase B: Communication Workspace V1"

    fun entryDialogues(): List<String> = texts("phase_b_workspace_entry")

    fun navigationIntroDialogues(): List<String> = texts("phase_b_nav_intro")

    fun categoryMenuDialogues(): List<String> = texts("phase_b_categories")

    fun phraseSelectionDialogues(): List<String> = texts("phase_b_phrase_select")

    fun backBehaviorDialogues(): List<String> = texts("phase_b_back")

    fun emergencyAccessDialogues(): List<String> = texts("phase_b_emergency")

    fun caregiverHelpDialogues(): List<String> = texts("phase_b_caregiver_help")

    fun fatiguePatienceDialogues(): List<String> = texts("phase_b_patience")

    fun contextHintVocabulary(): String =
        texts("phase_b_hint_vocab").firstOrNull() ?: "Blink a phrase to speak it."

    fun contextHintCategoryMenu(): String =
        texts("phase_b_hint_categories").firstOrNull() ?: "L2/R2 move · L1 R1 open · L2 R2 back"

    fun contextHintAdjustment(): String =
        texts("phase_b_hint_adjust").firstOrNull() ?: "L3 R1 decrease · L1 R3 increase · L1 R1 save · L2 R2 back"

    private fun texts(tag: String): List<String> =
        DefaultDialogueCatalog.all("en")
            .filter { it.contextTags.contains(tag) }
            .sortedBy { it.id }
            .map { it.text }
}
