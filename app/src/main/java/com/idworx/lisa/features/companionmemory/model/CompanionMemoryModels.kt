package com.idworx.lisa.features.companionmemory.model

enum class MemoryCategory {
    Learning,
    Communication,
    Navigation,
    Practice,
    Greeting,
    Calibration,
    Accessibility,
    Personality,
    Achievement,
    Preference,
    Session,
    Milestone
}

enum class MemoryImportance {
    Low,
    Medium,
    High,
    Milestone,
    Permanent
}

enum class LearningMilestone {
    MetLisa,
    FirstSuccessfulBlink,
    FirstWord,
    FirstPhrase,
    CalibrationComplete,
    TenPhrases,
    CommunicationFundamentalsComplete,
    CommunicationMasteryComplete,
    CommunicationCompleted,
    WorkspaceNavigationComplete,
    NavigationCompleted,
    LisaCertifiedCommunicator,
    PracticeCompleted,
    GraduationCompleted,
    EmergencyTrainingCompleted,
    GuidedLearningCompleted,
    FirstSuccessfulCalibration,
    CalibrationImproved,
    RecalibrationCompleted
}

enum class GreetingScenario {
    FirstLaunch,
    ReturningToday,
    ReturningTomorrow,
    ReturningAfterWeek,
    ReturningAfterMonth,
    GuidedLearningComplete,
    GraduationComplete,
    PracticeStreak,
    UnfinishedLesson,
    ReturningUser
}

data class MemoryEvent(
    val eventType: String,
    val timestampMs: Long = System.currentTimeMillis(),
    val evidence: String,
    val metadata: Map<String, String> = emptyMap()
)

data class CompanionMemory(
    val memoryId: String,
    val category: MemoryCategory,
    val title: String,
    val description: String,
    val timestampMs: Long,
    val importance: MemoryImportance,
    val observableEvidence: String,
    val tags: Set<String> = emptySet(),
    val version: Int = 1
)

data class SessionSummary(
    val sessionId: String,
    val startTimeMs: Long,
    val endTimeMs: Long = 0L,
    val durationMs: Long = 0L,
    val lessonsPracticed: Int = 0,
    val lessonsCompleted: Int = 0,
    val navigationExercisesCompleted: Int = 0,
    val successfulCommunicationAttempts: Int = 0,
    val practiceModeUsed: Boolean = false,
    val calibrationUsed: Boolean = false,
    val settingsChanged: Int = 0,
    val interruptions: Int = 0
)

data class GreetingContext(
    val scenario: GreetingScenario,
    val isFirstLaunch: Boolean = false,
    val daysSinceLastSession: Int = 0,
    val guidedLearningComplete: Boolean = false,
    val graduationComplete: Boolean = false,
    val unfinishedLessonId: String? = null,
    val practiceStreakDays: Int = 0,
    val completedCommunicationTraining: Boolean = false,
    val completedNavigationTraining: Boolean = false,
    val tutorialSkipped: Boolean = false,
    val returningUser: Boolean = false
)

data class PreferenceMemorySnapshot(
    val narrationEnabled: Boolean? = null,
    val narrationSpeed: Float? = null,
    val narrationVolume: Float? = null,
    val preferredLanguage: String? = null,
    val textSizeScale: Float? = null,
    val blinkSensitivity: Int? = null,
    val calibrationCompleted: Boolean = false,
    val lastUpdatedMs: Long = System.currentTimeMillis()
)

data class LearningHistoryEntry(
    val lessonId: String,
    val phase: String,
    val completedAtMs: Long? = null,
    val attemptCount: Int = 0,
    val repeated: Boolean = false,
    val skipped: Boolean = false
)

data class PracticeHistoryEntry(
    val sessionId: String,
    val exerciseId: String,
    val practiceType: String,
    val durationMs: Long,
    val successful: Boolean,
    val timestampMs: Long
)

data class MemoryStatistics(
    val totalSessions: Int = 0,
    val totalLessonsCompleted: Int = 0,
    val totalPracticeSessions: Int = 0,
    val averageAttemptsPerLesson: Float = 0f,
    val practiceStreakDays: Int = 0,
    val lastSessionEndMs: Long = 0L
)
