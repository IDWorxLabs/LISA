package com.idworx.lisa.features.personality.state

import com.idworx.lisa.features.personality.model.DialogueCategory

class DialogueHistoryTracker(
    private val maxHistorySize: Int = 32,
    private val categoryCooldownSize: Int = 3
) {
    private val globalHistory = ArrayDeque<String>()
    private val categoryHistory = mutableMapOf<DialogueCategory, ArrayDeque<String>>()

    fun record(dialogueId: String, category: DialogueCategory) {
        globalHistory.addLast(dialogueId)
        while (globalHistory.size > maxHistorySize) {
            globalHistory.removeFirst()
        }
        val catQueue = categoryHistory.getOrPut(category) { ArrayDeque() }
        catQueue.addLast(dialogueId)
        while (catQueue.size > categoryCooldownSize) {
            catQueue.removeFirst()
        }
    }

    fun wasRecentlyUsed(dialogueId: String): Boolean =
        dialogueId in globalHistory

    fun wasRecentlyUsedInCategory(dialogueId: String, category: DialogueCategory): Boolean =
        dialogueId in (categoryHistory[category] ?: emptyList())

    fun clear() {
        globalHistory.clear()
        categoryHistory.clear()
    }

    fun recentIds(): List<String> = globalHistory.toList()

    fun recentIdsForCategory(category: DialogueCategory): List<String> =
        categoryHistory[category]?.toList() ?: emptyList()
}
