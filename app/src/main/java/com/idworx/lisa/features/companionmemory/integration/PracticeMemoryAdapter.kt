package com.idworx.lisa.features.companionmemory.integration

import com.idworx.lisa.features.companionmemory.engine.CompanionMemoryEngine
import com.idworx.lisa.features.companionmemory.engine.DefaultCompanionMemoryEngine
import com.idworx.lisa.features.companionmemory.model.MemoryCategory
import com.idworx.lisa.features.companionmemory.model.MemoryEvent
import com.idworx.lisa.features.companionmemory.model.MemoryImportance
import com.idworx.lisa.features.companionmemory.model.PracticeHistoryEntry

object PracticeMemoryAdapter {

    private var practiceSessionStartMs: Long = 0L

    fun onPracticeSessionStarted(engine: CompanionMemoryEngine) {
        practiceSessionStartMs = System.currentTimeMillis()
        engine.recordEvent(
            MemoryEvent("practice_started", evidence = "Practice Mode session started"),
            MemoryCategory.Practice,
            MemoryImportance.Low,
            "Practice started",
            "Practice Mode session began"
        )
    }

    fun onPracticeExerciseCompleted(engine: CompanionMemoryEngine, exerciseId: String, successful: Boolean) {
        val duration = if (practiceSessionStartMs > 0) {
            System.currentTimeMillis() - practiceSessionStartMs
        } else {
            0L
        }
        (engine as? DefaultCompanionMemoryEngine)?.recordPracticeEntry(
            PracticeHistoryEntry(
                sessionId = "practice_${engine.getStatistics().totalPracticeSessions + 1}",
                exerciseId = exerciseId,
                practiceType = "communication",
                durationMs = duration,
                successful = successful,
                timestampMs = System.currentTimeMillis()
            )
        )
        if (successful) {
            engine.recordEvent(
                MemoryEvent("practice_success", evidence = "Completed practice exercise $exerciseId successfully"),
                MemoryCategory.Practice,
                MemoryImportance.Medium,
                "Practice success",
                "Completed practice exercise $exerciseId"
            )
        }
    }

    fun onPracticeSessionEnded(engine: CompanionMemoryEngine) {
        practiceSessionStartMs = 0L
        engine.recordEvent(
            MemoryEvent("practice_ended", evidence = "Practice Mode session ended"),
            MemoryCategory.Practice,
            MemoryImportance.Low,
            "Practice ended",
            "Practice Mode session ended"
        )
    }
}
