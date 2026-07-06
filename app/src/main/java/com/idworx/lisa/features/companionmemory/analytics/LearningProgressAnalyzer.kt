package com.idworx.lisa.features.companionmemory.analytics

import com.idworx.lisa.features.companionmemory.engine.CompanionMemoryEngine
import com.idworx.lisa.features.companionmemory.model.LearningHistoryEntry

object LearningProgressAnalyzer {

    fun mostPracticedLessons(engine: CompanionMemoryEngine, limit: Int = 5): List<LearningHistoryEntry> =
        engine.getLearningHistory()
            .groupBy { it.lessonId }
            .map { (_, entries) -> entries.maxByOrNull { it.attemptCount }!! }
            .sortedByDescending { it.attemptCount }
            .take(limit)

    fun mostSuccessfulLessons(engine: CompanionMemoryEngine, limit: Int = 5): List<LearningHistoryEntry> =
        engine.getLearningHistory()
            .filter { it.completedAtMs != null }
            .sortedBy { it.attemptCount }
            .take(limit)

    fun resumePoint(engine: CompanionMemoryEngine): LearningHistoryEntry? =
        engine.getLearningHistory().lastOrNull { it.completedAtMs == null && !it.skipped }

    fun completedPhaseCount(engine: CompanionMemoryEngine, phase: String): Int =
        engine.getLearningHistory().count {
            it.phase.equals(phase, ignoreCase = true) && it.completedAtMs != null
        }
}
