package com.idworx.lisa.features.companionmemory.repository

import com.idworx.lisa.features.companionmemory.model.CompanionMemory
import com.idworx.lisa.features.companionmemory.model.LearningHistoryEntry
import com.idworx.lisa.features.companionmemory.model.LearningMilestone
import com.idworx.lisa.features.companionmemory.model.MemoryCategory
import com.idworx.lisa.features.companionmemory.model.MemoryImportance
import com.idworx.lisa.features.companionmemory.model.MemoryStatistics
import com.idworx.lisa.features.companionmemory.model.PracticeHistoryEntry
import com.idworx.lisa.features.companionmemory.model.PreferenceMemorySnapshot
import com.idworx.lisa.features.companionmemory.model.SessionSummary
import com.idworx.lisa.features.companionmemory.state.CompanionMemoryState
import org.json.JSONArray
import org.json.JSONObject

object CompanionMemorySerializer {

    fun toJson(state: CompanionMemoryState): String = JSONObject().apply {
        put("schemaVersion", state.schemaVersion)
        put("firstLaunchRecorded", state.firstLaunchRecorded)
        put("activeSessionId", state.activeSessionId)
        put("memories", JSONArray(state.memories.map { memoryToJson(it) }))
        put("milestones", JSONArray(state.achievedMilestones.map { it.name }))
        put("sessions", JSONArray(state.sessions.map { sessionToJson(it) }))
        put("learningHistory", JSONArray(state.learningHistory.map { learningToJson(it) }))
        put("practiceHistory", JSONArray(state.practiceHistory.map { practiceToJson(it) }))
        put("preferences", preferencesToJson(state.preferences))
        put("statistics", statisticsToJson(state.statistics))
    }.toString()

    fun fromJson(raw: String): CompanionMemoryState {
        val obj = JSONObject(raw)
        return CompanionMemoryState(
            schemaVersion = obj.optInt("schemaVersion", 1),
            firstLaunchRecorded = obj.optBoolean("firstLaunchRecorded", false),
            activeSessionId = obj.optString("activeSessionId", null),
            memories = obj.optJSONArray("memories")?.let { array ->
                buildList {
                    for (i in 0 until array.length()) {
                        add(memoryFromJson(array.getJSONObject(i)))
                    }
                }
            } ?: emptyList(),
            achievedMilestones = obj.optJSONArray("milestones")?.let { array ->
                buildSet {
                    for (i in 0 until array.length()) {
                        runCatching { add(LearningMilestone.valueOf(array.getString(i))) }
                    }
                }
            } ?: emptySet(),
            sessions = obj.optJSONArray("sessions")?.let { array ->
                buildList {
                    for (i in 0 until array.length()) {
                        add(sessionFromJson(array.getJSONObject(i)))
                    }
                }
            } ?: emptyList(),
            learningHistory = obj.optJSONArray("learningHistory")?.let { array ->
                buildList {
                    for (i in 0 until array.length()) {
                        add(learningFromJson(array.getJSONObject(i)))
                    }
                }
            } ?: emptyList(),
            practiceHistory = obj.optJSONArray("practiceHistory")?.let { array ->
                buildList {
                    for (i in 0 until array.length()) {
                        add(practiceFromJson(array.getJSONObject(i)))
                    }
                }
            } ?: emptyList(),
            preferences = obj.optJSONObject("preferences")?.let { preferencesFromJson(it) }
                ?: PreferenceMemorySnapshot(),
            statistics = obj.optJSONObject("statistics")?.let { statisticsFromJson(it) }
                ?: MemoryStatistics()
        )
    }

    private fun memoryToJson(m: CompanionMemory) = JSONObject().apply {
        put("memoryId", m.memoryId)
        put("category", m.category.name)
        put("title", m.title)
        put("description", m.description)
        put("timestampMs", m.timestampMs)
        put("importance", m.importance.name)
        put("observableEvidence", m.observableEvidence)
        put("tags", JSONArray(m.tags.toList()))
        put("version", m.version)
    }

    private fun memoryFromJson(o: JSONObject) = CompanionMemory(
        memoryId = o.getString("memoryId"),
        category = MemoryCategory.valueOf(o.getString("category")),
        title = o.getString("title"),
        description = o.getString("description"),
        timestampMs = o.getLong("timestampMs"),
        importance = MemoryImportance.valueOf(o.getString("importance")),
        observableEvidence = o.getString("observableEvidence"),
        tags = o.optJSONArray("tags")?.let { arr ->
            buildSet { for (i in 0 until arr.length()) add(arr.getString(i)) }
        } ?: emptySet(),
        version = o.optInt("version", 1)
    )

    private fun sessionToJson(s: SessionSummary) = JSONObject().apply {
        put("sessionId", s.sessionId)
        put("startTimeMs", s.startTimeMs)
        put("endTimeMs", s.endTimeMs)
        put("durationMs", s.durationMs)
        put("lessonsPracticed", s.lessonsPracticed)
        put("lessonsCompleted", s.lessonsCompleted)
        put("navigationExercisesCompleted", s.navigationExercisesCompleted)
        put("successfulCommunicationAttempts", s.successfulCommunicationAttempts)
        put("practiceModeUsed", s.practiceModeUsed)
        put("calibrationUsed", s.calibrationUsed)
        put("settingsChanged", s.settingsChanged)
        put("interruptions", s.interruptions)
    }

    private fun sessionFromJson(o: JSONObject) = SessionSummary(
        sessionId = o.getString("sessionId"),
        startTimeMs = o.getLong("startTimeMs"),
        endTimeMs = o.optLong("endTimeMs", 0L),
        durationMs = o.optLong("durationMs", 0L),
        lessonsPracticed = o.optInt("lessonsPracticed", 0),
        lessonsCompleted = o.optInt("lessonsCompleted", 0),
        navigationExercisesCompleted = o.optInt("navigationExercisesCompleted", 0),
        successfulCommunicationAttempts = o.optInt("successfulCommunicationAttempts", 0),
        practiceModeUsed = o.optBoolean("practiceModeUsed", false),
        calibrationUsed = o.optBoolean("calibrationUsed", false),
        settingsChanged = o.optInt("settingsChanged", 0),
        interruptions = o.optInt("interruptions", 0)
    )

    private fun learningToJson(e: LearningHistoryEntry) = JSONObject().apply {
        put("lessonId", e.lessonId)
        put("phase", e.phase)
        put("completedAtMs", e.completedAtMs)
        put("attemptCount", e.attemptCount)
        put("repeated", e.repeated)
        put("skipped", e.skipped)
    }

    private fun learningFromJson(o: JSONObject) = LearningHistoryEntry(
        lessonId = o.getString("lessonId"),
        phase = o.getString("phase"),
        completedAtMs = if (o.has("completedAtMs") && !o.isNull("completedAtMs")) o.getLong("completedAtMs") else null,
        attemptCount = o.optInt("attemptCount", 0),
        repeated = o.optBoolean("repeated", false),
        skipped = o.optBoolean("skipped", false)
    )

    private fun practiceToJson(e: PracticeHistoryEntry) = JSONObject().apply {
        put("sessionId", e.sessionId)
        put("exerciseId", e.exerciseId)
        put("practiceType", e.practiceType)
        put("durationMs", e.durationMs)
        put("successful", e.successful)
        put("timestampMs", e.timestampMs)
    }

    private fun practiceFromJson(o: JSONObject) = PracticeHistoryEntry(
        sessionId = o.getString("sessionId"),
        exerciseId = o.getString("exerciseId"),
        practiceType = o.getString("practiceType"),
        durationMs = o.optLong("durationMs", 0L),
        successful = o.optBoolean("successful", false),
        timestampMs = o.getLong("timestampMs")
    )

    private fun preferencesToJson(p: PreferenceMemorySnapshot) = JSONObject().apply {
        p.narrationEnabled?.let { put("narrationEnabled", it) }
        p.narrationSpeed?.let { put("narrationSpeed", it.toDouble()) }
        p.narrationVolume?.let { put("narrationVolume", it.toDouble()) }
        p.preferredLanguage?.let { put("preferredLanguage", it) }
        p.textSizeScale?.let { put("textSizeScale", it.toDouble()) }
        p.blinkSensitivity?.let { put("blinkSensitivity", it) }
        put("calibrationCompleted", p.calibrationCompleted)
        put("lastUpdatedMs", p.lastUpdatedMs)
    }

    private fun preferencesFromJson(o: JSONObject) = PreferenceMemorySnapshot(
        narrationEnabled = if (o.has("narrationEnabled")) o.getBoolean("narrationEnabled") else null,
        narrationSpeed = if (o.has("narrationSpeed")) o.getDouble("narrationSpeed").toFloat() else null,
        narrationVolume = if (o.has("narrationVolume")) o.getDouble("narrationVolume").toFloat() else null,
        preferredLanguage = o.optString("preferredLanguage", null),
        textSizeScale = if (o.has("textSizeScale")) o.getDouble("textSizeScale").toFloat() else null,
        blinkSensitivity = if (o.has("blinkSensitivity")) o.getInt("blinkSensitivity") else null,
        calibrationCompleted = o.optBoolean("calibrationCompleted", false),
        lastUpdatedMs = o.optLong("lastUpdatedMs", System.currentTimeMillis())
    )

    private fun statisticsToJson(s: MemoryStatistics) = JSONObject().apply {
        put("totalSessions", s.totalSessions)
        put("totalLessonsCompleted", s.totalLessonsCompleted)
        put("totalPracticeSessions", s.totalPracticeSessions)
        put("averageAttemptsPerLesson", s.averageAttemptsPerLesson.toDouble())
        put("practiceStreakDays", s.practiceStreakDays)
        put("lastSessionEndMs", s.lastSessionEndMs)
    }

    private fun statisticsFromJson(o: JSONObject) = MemoryStatistics(
        totalSessions = o.optInt("totalSessions", 0),
        totalLessonsCompleted = o.optInt("totalLessonsCompleted", 0),
        totalPracticeSessions = o.optInt("totalPracticeSessions", 0),
        averageAttemptsPerLesson = o.optDouble("averageAttemptsPerLesson", 0.0).toFloat(),
        practiceStreakDays = o.optInt("practiceStreakDays", 0),
        lastSessionEndMs = o.optLong("lastSessionEndMs", 0L)
    )
}
