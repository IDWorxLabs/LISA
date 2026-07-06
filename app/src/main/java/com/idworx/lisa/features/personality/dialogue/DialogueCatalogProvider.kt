package com.idworx.lisa.features.personality.dialogue

import com.idworx.lisa.features.personality.model.DialogueCategory
import com.idworx.lisa.features.personality.model.LisaDialogue

interface LisaDialogueProvider {
    fun dialoguesFor(category: DialogueCategory, locale: String = "en"): List<LisaDialogue>
    fun allDialogues(locale: String = "en"): List<LisaDialogue>
}

interface LisaDialogueGenerator {
    fun generate(category: DialogueCategory, locale: String = "en"): LisaDialogue?
}

interface DialogueCatalogProvider {
    fun catalog(locale: String = "en"): LisaDialogueProvider
}
