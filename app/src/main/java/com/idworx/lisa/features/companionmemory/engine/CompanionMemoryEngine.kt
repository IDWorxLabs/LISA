package com.idworx.lisa.features.companionmemory.engine

import com.idworx.lisa.features.companionmemory.model.CompanionMemory
import com.idworx.lisa.features.companionmemory.model.GreetingContext
import com.idworx.lisa.features.companionmemory.model.LearningHistoryEntry
import com.idworx.lisa.features.companionmemory.model.LearningMilestone
import com.idworx.lisa.features.companionmemory.model.MemoryEvent
import com.idworx.lisa.features.companionmemory.model.MemoryStatistics
import com.idworx.lisa.features.companionmemory.model.PracticeHistoryEntry
import com.idworx.lisa.features.companionmemory.model.PreferenceMemorySnapshot
import com.idworx.lisa.features.companionmemory.model.SessionSummary

interface CompanionMemoryEngine {
    fun recordEvent(event: MemoryEvent, category: com.idworx.lisa.features.companionmemory.model.MemoryCategory, importance: com.idworx.lisa.features.companionmemory.model.MemoryImportance, title: String, description: String): CompanionMemory?
    fun recordMilestone(milestone: LearningMilestone, evidence: String): CompanionMemory?
    fun recordSession(summary: SessionSummary)
    fun startSession(): String
    fun endSession()
    fun recordPreference(snapshot: PreferenceMemorySnapshot)
    fun getGreetingContext(): GreetingContext
    fun getRecentAchievements(limit: Int = 5): List<CompanionMemory>
    fun getLearningHistory(): List<LearningHistoryEntry>
    fun getPracticeHistory(): List<PracticeHistoryEntry>
    fun getCommunicationHistory(): List<LearningHistoryEntry>
    fun getMilestones(): Set<LearningMilestone>
    fun getStatistics(): MemoryStatistics
    fun clearLearningMemory()
    fun exportMemory(): String
    fun importMemory(json: String): Boolean
}
