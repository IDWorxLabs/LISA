package com.idworx.lisa.features.accessibilityconsistency.integration

import com.idworx.lisa.features.accessibilityconsistency.model.AccessibilityScore
import com.idworx.lisa.features.accessibilityconsistency.model.AccessibilityScoreBand
import com.idworx.lisa.features.personality.dialogue.DefaultDialogueCatalog
import com.idworx.lisa.features.personality.engine.LisaPersonalityEngine
import com.idworx.lisa.features.personality.engine.LisaPersonalityEngines
import com.idworx.lisa.features.personality.model.AppFeature
import com.idworx.lisa.features.personality.model.DialogueCategory
import com.idworx.lisa.features.personality.model.DialogueContext

object AccessibilityPersonalityAdapter {

    fun guidanceForScore(score: AccessibilityScore?): String {
        val dialogue = when (score?.band) {
            AccessibilityScoreBand.Critical, AccessibilityScoreBand.NeedsImprovement ->
                selectDialogue("a11y_easier")
            AccessibilityScoreBand.Acceptable ->
                selectDialogue("a11y_text_size")
            null -> selectDialogue("a11y_slow_down")
            else -> selectDialogue("a11y_repeat_lesson")
        }
        return dialogue.text
    }

    fun encouragement(personality: LisaPersonalityEngine = LisaPersonalityEngines.default): String =
        personality.generateComfort(DialogueContext(feature = AppFeature.Settings)).text

    fun settingsHint(): String = selectDialogue("a11y_text_size").text

    fun practiceHint(): String = selectDialogue("a11y_repeat_lesson").text

    private fun selectDialogue(id: String) =
        DefaultDialogueCatalog.forCategory(DialogueCategory.SettingsGuidance, "en")
            .firstOrNull { it.id == id }
            ?: DefaultDialogueCatalog.forCategory(DialogueCategory.Instruction, "en")
                .firstOrNull { it.id == id }
            ?: DefaultDialogueCatalog.forCategory(DialogueCategory.SettingsGuidance, "en").first()
}
