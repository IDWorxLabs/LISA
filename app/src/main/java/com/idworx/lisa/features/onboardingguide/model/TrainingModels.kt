package com.idworx.lisa.features.onboardingguide.model

import com.idworx.lisa.features.brain1interactionstandard.model.UniversalInteractionGestures

enum class TrainingPhase {
    FirstLaunchChoice,
    Welcome,
    Setup,
    Calibration,
    CommunicationLesson,
    CommunicationMastery,
    NavigationLesson,
    Completion,
    SkipConfirm
}

enum class LessonKind {
    Communication,
    Navigation
}

data class CommunicationLesson(
    val id: String,
    val vocabularyId: String,
    val left: Int,
    val right: Int,
    val displayOrder: Int,
    val difficultyLevel: Int = UniversalInteractionGestures.difficultyLevel(left, right),
    /** Blink order when counts alone are ambiguous: "LR" or "RL". */
    val blinkOrder: String? = null
)

enum class NavigationAction {
    OpenCategories,
    SelectCategory,
    CloseMenu,
    RepeatLastPhrase,
    OpenMenu,
    OpenCommunicationHistory,
    ResetSequence,
    TriggerEmergency,
    OpenQuickControls,
    OpenSettings,
    OpenCaregiver,
    /** Selecting a specific phrase entry inside the real workspace Vocabulary screen. */
    SelectPhrase,
    /** Real workspace "Next Page" gesture (L0 R2). */
    NextPage,
    /** Real workspace "Previous Page" gesture (L2 R0). */
    PreviousPage
}

data class NavigationLesson(
    val id: String,
    val action: NavigationAction,
    val displayOrder: Int
)

data class PracticeStatistics(
    val totalAttempts: Int = 0,
    val successfulAttempts: Int = 0,
    val consecutiveFailures: Int = 0
) {
    fun recordAttempt(success: Boolean): PracticeStatistics = copy(
        totalAttempts = totalAttempts + 1,
        successfulAttempts = successfulAttempts + if (success) 1 else 0,
        consecutiveFailures = if (success) 0 else consecutiveFailures + 1
    )
}

data class TrainingPreferences(
    val narrationEnabled: Boolean = true,
    val narrationSpeed: Float = 0.9f,
    val narrationVolume: Float = 1.0f,
    val narrationLanguage: String = "en",
    /**
     * Guided Mode/Training's own response (settle) time, in seconds — deliberately separate from
     * the everyday Communication Workspace's response speed so a slower Guided Learning default
     * never changes normal daily communication timing. Applies to every guided lesson (phrase
     * lessons and workspace navigation lessons alike), never a single hardcoded lesson.
     */
    val guidedResponseTimeSec: Int = com.idworx.lisa.SequenceProcessingDelay.GUIDED_DEFAULT_SECONDS
)

data class TrainingProgress(
    val tutorialStarted: Boolean = false,
    val tutorialCompleted: Boolean = false,
    val tutorialSkipped: Boolean = false,
    val firstLaunchChoiceMade: Boolean = false,
    val certifiedCommunicator: Boolean = false,
    val calibrationCompleted: Boolean = false,
    val currentPhase: TrainingPhase = TrainingPhase.FirstLaunchChoice,
    val communicationLessonIndex: Int = 0,
    val masteryRoundIndex: Int = 0,
    val masteryPhraseOrder: String = "",
    val navigationLessonIndex: Int = 0,
    val completedLessonIds: Set<String> = emptySet(),
    val statistics: PracticeStatistics = PracticeStatistics(),
    val preferences: TrainingPreferences = TrainingPreferences(),
    val practiceModeOnly: Boolean = false,
    val practiceCommunication: Boolean = false,
    val practiceNavigation: Boolean = false,
    /** Successes on the current phrase before Lisa advances (patient coach pacing). */
    val currentLessonSuccessCount: Int = 0,
    /** Phrases mastered this session — used for fatigue-friendly break suggestions. */
    val sessionLessonsThisVisit: Int = 0
) {
    val isFinished: Boolean get() = tutorialCompleted || tutorialSkipped

    fun communicationLessonComplete(id: String): TrainingProgress = copy(
        completedLessonIds = completedLessonIds + id
    )

    fun navigationLessonComplete(id: String): TrainingProgress = copy(
        completedLessonIds = completedLessonIds + id
    )
}
