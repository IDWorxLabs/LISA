package com.idworx.lisa.features.companionmemory.state

import com.idworx.lisa.features.companionmemory.model.CompanionMemory
import com.idworx.lisa.features.companionmemory.model.LearningHistoryEntry
import com.idworx.lisa.features.companionmemory.model.LearningMilestone
import com.idworx.lisa.features.companionmemory.model.MemoryStatistics
import com.idworx.lisa.features.companionmemory.model.PracticeHistoryEntry
import com.idworx.lisa.features.companionmemory.model.PreferenceMemorySnapshot
import com.idworx.lisa.features.companionmemory.model.SessionSummary

data class CompanionMemoryState(
    val memories: List<CompanionMemory> = emptyList(),
    val achievedMilestones: Set<LearningMilestone> = emptySet(),
    val sessions: List<SessionSummary> = emptyList(),
    val activeSessionId: String? = null,
    val learningHistory: List<LearningHistoryEntry> = emptyList(),
    val practiceHistory: List<PracticeHistoryEntry> = emptyList(),
    val preferences: PreferenceMemorySnapshot = PreferenceMemorySnapshot(),
    val statistics: MemoryStatistics = MemoryStatistics(),
    val firstLaunchRecorded: Boolean = false,
    val schemaVersion: Int = 1
)
