package com.idworx.lisa.features.companionmemory.engine

import com.idworx.lisa.features.companionmemory.metadata.CompanionMemoryMetadata
import com.idworx.lisa.features.companionmemory.model.CompanionMemory
import com.idworx.lisa.features.companionmemory.model.GreetingContext
import com.idworx.lisa.features.companionmemory.model.GreetingScenario
import com.idworx.lisa.features.companionmemory.model.LearningHistoryEntry
import com.idworx.lisa.features.companionmemory.model.LearningMilestone
import com.idworx.lisa.features.companionmemory.model.MemoryCategory
import com.idworx.lisa.features.companionmemory.model.MemoryEvent
import com.idworx.lisa.features.companionmemory.model.MemoryImportance
import com.idworx.lisa.features.companionmemory.model.MemoryStatistics
import com.idworx.lisa.features.companionmemory.model.PracticeHistoryEntry
import com.idworx.lisa.features.companionmemory.model.PreferenceMemorySnapshot
import com.idworx.lisa.features.companionmemory.model.SessionSummary
import com.idworx.lisa.features.companionmemory.repository.CompanionMemoryExportImport
import com.idworx.lisa.features.companionmemory.repository.CompanionMemoryRepository
import com.idworx.lisa.features.companionmemory.state.CompanionMemoryState
import java.util.UUID
import java.util.concurrent.TimeUnit

object MemoryPrivacyGuard {

    val FORBIDDEN_EMOTION_PATTERNS: List<String> = listOf(
        "user was sad", "user was frustrated", "user was nervous", "user was happy",
        "user misses family", "user enjoys", "user feels", "user is angry",
        "user is anxious", "user is depressed", "i know you are", "i know how you feel"
    )

    fun isObservableEvidenceValid(evidence: String): Boolean {
        if (evidence.isBlank()) return false
        val lower = evidence.lowercase()
        return FORBIDDEN_EMOTION_PATTERNS.none { lower.contains(it) }
    }
}

class DefaultCompanionMemoryEngine(
    private val repository: CompanionMemoryRepository
) : CompanionMemoryEngine {

    private val index = MemoryIndex(repository.load())

    private fun persist() = repository.save(index.state())

    override fun recordEvent(
        event: MemoryEvent,
        category: MemoryCategory,
        importance: MemoryImportance,
        title: String,
        description: String
    ): CompanionMemory? {
        if (!MemoryPrivacyGuard.isObservableEvidenceValid(event.evidence)) return null
        if (!MemoryPrivacyGuard.isObservableEvidenceValid(description)) return null
        val memory = CompanionMemory(
            memoryId = UUID.randomUUID().toString(),
            category = category,
            title = title,
            description = description,
            timestampMs = event.timestampMs,
            importance = importance,
            observableEvidence = event.evidence,
            tags = setOf(event.eventType)
        )
        val state = index.state()
        val updatedMemories = applyRetentionPolicy(state.memories + memory)
        index.update(state.copy(memories = updatedMemories))
        persist()
        return memory
    }

    override fun recordMilestone(milestone: LearningMilestone, evidence: String): CompanionMemory? {
        if (!MemoryPrivacyGuard.isObservableEvidenceValid(evidence)) return null
        if (index.hasMilestone(milestone)) return null
        val state = index.state()
        index.update(state.copy(achievedMilestones = state.achievedMilestones + milestone))
        return recordEvent(
            event = MemoryEvent("milestone_${milestone.name}", evidence = evidence),
            category = MemoryCategory.Milestone,
            importance = if (milestone == LearningMilestone.GraduationCompleted ||
                milestone == LearningMilestone.GuidedLearningCompleted
            ) {
                MemoryImportance.Permanent
            } else {
                MemoryImportance.Milestone
            },
            title = milestone.name.replace(Regex("([A-Z])"), " $1").trim(),
            description = evidence
        )
    }

    override fun recordSession(summary: SessionSummary) {
        val state = index.state()
        val existing = state.sessions.indexOfFirst { it.sessionId == summary.sessionId }
        val sessions = if (existing >= 0) {
            state.sessions.toMutableList().apply { set(existing, summary) }
        } else {
            state.sessions + summary
        }
        index.update(
            state.copy(
                sessions = sessions,
                statistics = state.statistics.copy(
                    totalSessions = sessions.size,
                    lastSessionEndMs = summary.endTimeMs.takeIf { it > 0 } ?: state.statistics.lastSessionEndMs
                )
            )
        )
        persist()
    }

    override fun startSession(): String {
        val sessionId = UUID.randomUUID().toString()
        val summary = SessionSummary(sessionId = sessionId, startTimeMs = System.currentTimeMillis())
        val state = index.state()
        val wasFirstLaunch = !state.firstLaunchRecorded
        index.update(
            state.copy(
                activeSessionId = sessionId,
                sessions = state.sessions + summary,
                firstLaunchRecorded = true
            )
        )
        if (wasFirstLaunch) {
            recordEvent(
                event = MemoryEvent("first_launch", evidence = "Application session started for the first time"),
                category = MemoryCategory.Session,
                importance = MemoryImportance.Permanent,
                title = "First launch",
                description = "User opened LISA for the first time"
            )
        }
        persist()
        return sessionId
    }

    override fun endSession() {
        val state = index.state()
        val sessionId = state.activeSessionId ?: return
        val session = state.sessions.find { it.sessionId == sessionId } ?: return
        val ended = session.copy(
            endTimeMs = System.currentTimeMillis(),
            durationMs = System.currentTimeMillis() - session.startTimeMs
        )
        recordSession(ended)
        index.update(
            index.state().copy(
                activeSessionId = null,
                statistics = index.state().statistics.copy(lastSessionEndMs = ended.endTimeMs)
            )
        )
        persist()
    }

    override fun recordPreference(snapshot: PreferenceMemorySnapshot) {
        val state = index.state()
        index.update(state.copy(preferences = snapshot.copy(lastUpdatedMs = System.currentTimeMillis())))
        recordEvent(
            event = MemoryEvent("preference_updated", evidence = "User preference snapshot updated at ${snapshot.lastUpdatedMs}"),
            category = MemoryCategory.Preference,
            importance = MemoryImportance.Medium,
            title = "Preferences updated",
            description = "Application preferences were updated"
        )
        persist()
    }

    override fun getGreetingContext(): GreetingContext {
        val state = index.state()
        val lastEnd = state.statistics.lastSessionEndMs
        val daysSince = if (lastEnd > 0L) {
            TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - lastEnd).toInt()
        } else {
            0
        }
        val guidedComplete = LearningMilestone.GuidedLearningCompleted in state.achievedMilestones
        val graduationComplete = LearningMilestone.GraduationCompleted in state.achievedMilestones
        val commComplete = LearningMilestone.CommunicationCompleted in state.achievedMilestones
        val navComplete = LearningMilestone.NavigationCompleted in state.achievedMilestones
        val unfinished = index.findUnfinishedLesson()?.lessonId
        val isFirstActiveSession = state.sessions.size <= 1 && lastEnd == 0L && state.activeSessionId != null
        val scenario = when {
            isFirstActiveSession -> GreetingScenario.FirstLaunch
            graduationComplete -> GreetingScenario.GraduationComplete
            guidedComplete -> GreetingScenario.GuidedLearningComplete
            unfinished != null -> GreetingScenario.UnfinishedLesson
            state.statistics.practiceStreakDays >= 3 -> GreetingScenario.PracticeStreak
            daysSince >= 30 -> GreetingScenario.ReturningAfterMonth
            daysSince >= 7 -> GreetingScenario.ReturningAfterWeek
            daysSince >= 1 -> GreetingScenario.ReturningTomorrow
            daysSince == 0 && lastEnd > 0 -> GreetingScenario.ReturningToday
            else -> GreetingScenario.ReturningUser
        }
        return GreetingContext(
            scenario = scenario,
            isFirstLaunch = isFirstActiveSession,
            daysSinceLastSession = daysSince,
            guidedLearningComplete = guidedComplete,
            graduationComplete = graduationComplete,
            unfinishedLessonId = unfinished,
            practiceStreakDays = state.statistics.practiceStreakDays,
            completedCommunicationTraining = commComplete,
            completedNavigationTraining = navComplete,
            returningUser = state.firstLaunchRecorded && lastEnd > 0,
            tutorialSkipped = state.memories.any { it.tags.contains("tutorial_skipped") }
        )
    }

    override fun getRecentAchievements(limit: Int): List<CompanionMemory> =
        index.recentAchievements(limit)

    override fun getLearningHistory(): List<LearningHistoryEntry> = index.state().learningHistory

    override fun getPracticeHistory(): List<PracticeHistoryEntry> = index.state().practiceHistory

    override fun getCommunicationHistory(): List<LearningHistoryEntry> =
        index.state().learningHistory.filter { it.phase.contains("Communication", ignoreCase = true) }

    override fun getMilestones(): Set<LearningMilestone> = index.state().achievedMilestones

    override fun getStatistics(): MemoryStatistics = index.state().statistics

    override fun clearLearningMemory() {
        repository.clearLearningMemory()
        index.update(repository.load())
    }

    override fun exportMemory(): String =
        (repository as? CompanionMemoryExportImport)?.exportMemory()
            ?: throw UnsupportedOperationException("Repository does not support export")

    override fun importMemory(json: String): Boolean =
        (repository as? CompanionMemoryExportImport)?.importMemory(json)?.also {
            if (it) index.update(repository.load())
        } ?: false

    fun recordLearningEntry(entry: LearningHistoryEntry) {
        val state = index.state()
        val existing = state.learningHistory.find { it.lessonId == entry.lessonId && it.phase == entry.phase }
        val history = if (existing != null) {
            state.learningHistory.map { if (it.lessonId == entry.lessonId && it.phase == entry.phase) entry else it }
        } else {
            state.learningHistory + entry
        }
        val completedCount = history.count { it.completedAtMs != null }
        index.update(
            state.copy(
                learningHistory = history,
                statistics = state.statistics.copy(
                    totalLessonsCompleted = completedCount,
                    averageAttemptsPerLesson = if (completedCount > 0) {
                        history.filter { it.completedAtMs != null }.map { it.attemptCount.toFloat() }.average().toFloat()
                    } else 0f
                )
            )
        )
        persist()
    }

    fun recordPracticeEntry(entry: PracticeHistoryEntry) {
        val state = index.state()
        index.update(
            state.copy(
                practiceHistory = state.practiceHistory + entry,
                statistics = state.statistics.copy(
                    totalPracticeSessions = state.statistics.totalPracticeSessions + 1
                )
            )
        )
        updateActiveSession { it.copy(practiceModeUsed = true) }
        persist()
    }

    fun incrementSessionLessonCompleted() {
        updateActiveSession { it.copy(lessonsCompleted = it.lessonsCompleted + 1) }
        persist()
    }

    fun incrementSessionCommunicationSuccess() {
        updateActiveSession { it.copy(successfulCommunicationAttempts = it.successfulCommunicationAttempts + 1) }
        persist()
    }

    fun incrementSessionNavigationCompleted() {
        updateActiveSession { it.copy(navigationExercisesCompleted = it.navigationExercisesCompleted + 1) }
        persist()
    }

    private fun updateActiveSession(transform: (SessionSummary) -> SessionSummary) {
        val state = index.state()
        val sessionId = state.activeSessionId ?: return
        val session = state.sessions.find { it.sessionId == sessionId } ?: return
        val updated = state.sessions.map { if (it.sessionId == sessionId) transform(it) else it }
        index.update(state.copy(sessions = updated))
    }

    private fun applyRetentionPolicy(memories: List<CompanionMemory>): List<CompanionMemory> {
        val permanent = memories.filter {
            it.importance == MemoryImportance.Permanent || it.importance == MemoryImportance.Milestone
        }
        val rest = memories.filter {
            it.importance != MemoryImportance.Permanent && it.importance != MemoryImportance.Milestone
        }
        val low = rest.filter { it.importance == MemoryImportance.Low }
        val nonLow = rest.filter { it.importance != MemoryImportance.Low }
        val trimmedLow = if (low.size > CompanionMemoryMetadata.MAX_LOW_IMPORTANCE_MEMORIES) {
            low.sortedByDescending { it.timestampMs }.take(CompanionMemoryMetadata.MAX_LOW_IMPORTANCE_MEMORIES)
        } else {
            low
        }
        return permanent + nonLow + trimmedLow
    }
}

object CompanionMemoryEngines {
    @Volatile
    private var instance: DefaultCompanionMemoryEngine? = null

    fun init(context: android.content.Context): CompanionMemoryEngine {
        instance = DefaultCompanionMemoryEngine(
            com.idworx.lisa.features.companionmemory.repository.CompanionMemoryStore(context.applicationContext)
        )
        return instance!!
    }

    fun init(repository: CompanionMemoryRepository): CompanionMemoryEngine {
        instance = DefaultCompanionMemoryEngine(repository)
        return instance!!
    }

    val default: CompanionMemoryEngine
        get() = instance ?: DefaultCompanionMemoryEngine(
            com.idworx.lisa.features.companionmemory.repository.InMemoryCompanionMemoryRepository()
        ).also { instance = it }

    fun createForTests(
        repository: CompanionMemoryRepository =
            com.idworx.lisa.features.companionmemory.repository.InMemoryCompanionMemoryRepository()
    ): DefaultCompanionMemoryEngine = DefaultCompanionMemoryEngine(repository)
}
