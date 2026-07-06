package com.idworx.lisa.features.companionmemory.engine

import com.idworx.lisa.features.companionmemory.model.CompanionMemory
import com.idworx.lisa.features.companionmemory.model.GreetingContext
import com.idworx.lisa.features.companionmemory.model.LearningHistoryEntry
import com.idworx.lisa.features.companionmemory.model.LearningMilestone
import com.idworx.lisa.features.companionmemory.model.MemoryCategory
import com.idworx.lisa.features.companionmemory.model.MemoryEvent
import com.idworx.lisa.features.companionmemory.model.MemoryStatistics
import com.idworx.lisa.features.companionmemory.model.PracticeHistoryEntry
import com.idworx.lisa.features.companionmemory.model.PreferenceMemorySnapshot
import com.idworx.lisa.features.companionmemory.model.SessionSummary
import com.idworx.lisa.features.companionmemory.state.CompanionMemoryState

class MemoryIndex(private var state: CompanionMemoryState) {

    fun state(): CompanionMemoryState = state

    fun update(newState: CompanionMemoryState) {
        state = newState
    }

    fun memoriesByCategory(category: MemoryCategory): List<CompanionMemory> =
        state.memories.filter { it.category == category }

    fun hasMilestone(milestone: LearningMilestone): Boolean =
        milestone in state.achievedMilestones

    fun recentAchievements(limit: Int = 5): List<CompanionMemory> =
        state.memories
            .filter { it.importance >= com.idworx.lisa.features.companionmemory.model.MemoryImportance.High }
            .sortedByDescending { it.timestampMs }
            .take(limit)

    fun findUnfinishedLesson(): LearningHistoryEntry? =
        state.learningHistory.lastOrNull { it.completedAtMs == null && !it.skipped }
}
