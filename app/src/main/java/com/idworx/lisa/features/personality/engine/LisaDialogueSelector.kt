package com.idworx.lisa.features.personality.engine

import com.idworx.lisa.features.personality.dialogue.DialogueCatalog
import com.idworx.lisa.features.personality.model.DialogueCategory
import com.idworx.lisa.features.personality.model.DialogueContext
import com.idworx.lisa.features.personality.model.LisaDialogue
import com.idworx.lisa.features.personality.model.LisaPersonalityProfile
import com.idworx.lisa.features.personality.model.MilestoneType
import com.idworx.lisa.features.personality.state.DialogueHistoryTracker
import kotlin.random.Random

class LisaDialogueSelector(
    private val catalog: DialogueCatalog,
    private val history: DialogueHistoryTracker,
    private val profile: LisaPersonalityProfile = LisaPersonalityProfile.DEFAULT
) {
    fun select(
        category: DialogueCategory,
        context: DialogueContext,
        filter: (LisaDialogue) -> Boolean = { true }
    ): LisaDialogue {
        val baseCandidates = catalog.forCategory(category, context.locale)
            .filter { passesForbiddenCheck(it.text) }
            .filter { filter(it) }

        val candidates = if (context.deterministicSeed != null) {
            baseCandidates
        } else {
            baseCandidates.filter { !history.wasRecentlyUsed(it.id) }
                .ifEmpty { baseCandidates }
        }

        val selected = if (context.deterministicSeed != null) {
            val sorted = candidates.sortedBy { it.id }
            sorted[context.deterministicSeed % sorted.size.coerceAtLeast(1)]
        } else {
            weightedRandom(candidates)
        } ?: fallback(category, context.locale)

        if (context.deterministicSeed == null) {
            history.record(selected.id, category)
        }
        return selected
    }

    fun selectSequence(
        category: DialogueCategory,
        context: DialogueContext,
        filter: (LisaDialogue) -> Boolean = { true }
    ): List<LisaDialogue> =
        catalog.forCategory(category, context.locale)
            .filter { passesForbiddenCheck(it.text) }
            .filter { filter(it) }
            .ifEmpty { catalog.forCategory(category, context.locale).filter { passesForbiddenCheck(it.text) } }

    fun selectMilestone(type: MilestoneType, context: DialogueContext): LisaDialogue {
        val candidates = catalog.forMilestone(type, context.locale)
            .filter { passesForbiddenCheck(it.text) }
            .ifEmpty { catalog.forCategory(DialogueCategory.MilestoneCelebration, context.locale) }
        val selected = if (context.deterministicSeed != null) {
            candidates.sortedBy { it.id }.firstOrNull()
        } else {
            candidates.firstOrNull() ?: select(DialogueCategory.MilestoneCelebration, context)
        } ?: fallback(DialogueCategory.MilestoneCelebration, context.locale)
        history.record(selected.id, selected.category)
        return selected
    }

    fun selectByIds(ids: List<String>, locale: String = "en"): List<LisaDialogue> =
        ids.mapNotNull { id ->
            catalog.all(locale).find { it.id == id && passesForbiddenCheck(it.text) }
        }

    fun passesForbiddenCheck(text: String): Boolean =
        profile.forbiddenPhrases.none { phrase ->
            text.contains(phrase, ignoreCase = true)
        }

    private fun weightedRandom(candidates: List<LisaDialogue>): LisaDialogue? {
        if (candidates.isEmpty()) return null
        val totalWeight = candidates.sumOf { it.weight.coerceAtLeast(1) }
        var pick = Random.nextInt(totalWeight)
        for (dialogue in candidates) {
            pick -= dialogue.weight.coerceAtLeast(1)
            if (pick < 0) return dialogue
        }
        return candidates.last()
    }

    private fun fallback(category: DialogueCategory, locale: String): LisaDialogue =
        LisaDialogue(
            id = "fallback_${category.name.lowercase()}",
            text = "Take your time.",
            category = category,
            locale = locale
        )
}
