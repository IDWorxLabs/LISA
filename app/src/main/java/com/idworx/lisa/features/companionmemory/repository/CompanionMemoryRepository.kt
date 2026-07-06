package com.idworx.lisa.features.companionmemory.repository

import com.idworx.lisa.features.companionmemory.state.CompanionMemoryState

interface CompanionMemoryRepository {
    fun load(): CompanionMemoryState
    fun save(state: CompanionMemoryState)
    fun clearLearningMemory()
}

interface CompanionMemoryExportImport {
    fun exportMemory(): String
    fun importMemory(json: String): Boolean
}
