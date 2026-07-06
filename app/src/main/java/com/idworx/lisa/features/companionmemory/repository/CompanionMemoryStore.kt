package com.idworx.lisa.features.companionmemory.repository

import android.content.Context
import com.idworx.lisa.LisaProfileStore
import com.idworx.lisa.features.companionmemory.state.CompanionMemoryState

class CompanionMemoryStore(context: Context) :
    CompanionMemoryRepository,
    CompanionMemoryExportImport {

    private val prefs = context.getSharedPreferences(LisaProfileStore.PREFS_NAME, Context.MODE_PRIVATE)

    override fun load(): CompanionMemoryState {
        val raw = prefs.getString(KEY_STATE, null) ?: return CompanionMemoryState()
        return try {
            CompanionMemorySerializer.fromJson(raw)
        } catch (_: Exception) {
            CompanionMemoryState()
        }
    }

    override fun save(state: CompanionMemoryState) {
        prefs.edit().putString(KEY_STATE, CompanionMemorySerializer.toJson(state)).apply()
    }

    override fun clearLearningMemory() {
        val current = load()
        save(
            current.copy(
                memories = current.memories.filter {
                    it.category !in LEARNING_CATEGORIES
                },
                learningHistory = emptyList(),
                achievedMilestones = current.achievedMilestones.filter {
                    it !in LEARNING_MILESTONES
                }.toSet()
            )
        )
    }

    override fun exportMemory(): String = CompanionMemorySerializer.toJson(load())

    override fun importMemory(json: String): Boolean = try {
        save(CompanionMemorySerializer.fromJson(json))
        true
    } catch (_: Exception) {
        false
    }

    companion object {
        private const val KEY_STATE = "companion_memory_v1_state"

        private val LEARNING_CATEGORIES = setOf(
            com.idworx.lisa.features.companionmemory.model.MemoryCategory.Learning,
            com.idworx.lisa.features.companionmemory.model.MemoryCategory.Milestone,
            com.idworx.lisa.features.companionmemory.model.MemoryCategory.Achievement
        )

        private val LEARNING_MILESTONES = setOf(
            com.idworx.lisa.features.companionmemory.model.LearningMilestone.FirstSuccessfulBlink,
            com.idworx.lisa.features.companionmemory.model.LearningMilestone.FirstWord,
            com.idworx.lisa.features.companionmemory.model.LearningMilestone.FirstPhrase,
            com.idworx.lisa.features.companionmemory.model.LearningMilestone.CommunicationCompleted,
            com.idworx.lisa.features.companionmemory.model.LearningMilestone.NavigationCompleted,
            com.idworx.lisa.features.companionmemory.model.LearningMilestone.GraduationCompleted,
            com.idworx.lisa.features.companionmemory.model.LearningMilestone.EmergencyTrainingCompleted,
            com.idworx.lisa.features.companionmemory.model.LearningMilestone.GuidedLearningCompleted
        )
    }
}

/** In-memory repository for unit tests. */
class InMemoryCompanionMemoryRepository :
    CompanionMemoryRepository,
    CompanionMemoryExportImport {

    private var state: CompanionMemoryState = CompanionMemoryState()

    override fun load(): CompanionMemoryState = state

    override fun save(state: CompanionMemoryState) {
        this.state = state
    }

    override fun clearLearningMemory() {
        state = state.copy(learningHistory = emptyList())
    }

    override fun exportMemory(): String = CompanionMemorySerializer.toJson(state)

    override fun importMemory(json: String): Boolean = try {
        state = CompanionMemorySerializer.fromJson(json)
        true
    } catch (_: Exception) {
        false
    }
}
