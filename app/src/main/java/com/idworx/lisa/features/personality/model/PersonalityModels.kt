package com.idworx.lisa.features.personality.model

enum class DialogueCategory {
    Greeting,
    Welcome,
    Instruction,
    Encouragement,
    Comfort,
    Waiting,
    NavigationGuidance,
    Practice,
    MinorCelebration,
    MajorCelebration,
    MilestoneCelebration,
    Graduation,
    SessionCompletion,
    ReturnMessage,
    SettingsGuidance,
    RecalibrationGuidance,
    SensitivityGuidance,
    EmergencyTrainingGuidance
}

enum class DialogueTone {
    Warm,
    Gentle,
    Patient,
    Calm,
    Encouraging,
    Respectful,
    Professional,
    Compassionate
}

enum class DialoguePriority {
    Low,
    Normal,
    High,
    Critical
}

enum class DialogueQueueMode {
    Flush,
    Add,
    Interrupt
}

enum class MilestoneType {
    FirstSuccessfulBlink,
    FirstSpokenPhrase,
    LessonComplete,
    CommunicationTrainingComplete,
    NavigationTrainingComplete,
    GuidedLearningComplete,
    PracticeSessionStart,
    ReturningUser
}

enum class AppFeature {
    GuidedLearning,
    PracticeMode,
    Communication,
    Navigation,
    Settings,
    Caregiver,
    Emergency
}

/** Emotional presence moments — short, observable, never chatbot-like. */
enum class PresenceMoment {
    SessionOpening,
    WarmReturnGreeting,
    LongPauseEncouragement,
    CaregiverReassurance,
    FatigueCheckIn,
    EmotionalMilestone
}

/** Caregiver confidence moments — plain language, action-oriented, never technical jargon. */
enum class CaregiverSupportMoment {
    PhonePositioning,
    CameraLighting,
    CalibrationSupport,
    CaregiverOnlyHint,
    Troubleshooting,
    ProgressVisibility,
    TrackingRecovery,
    WhatToDoNow
}

data class DialogueTiming(
    val pauseBeforeMs: Long = 0L,
    val pauseAfterMs: Long = 400L,
    val recommendedSpeechRate: Float = 0.9f,
    val recommendedVolume: Float = 1.0f,
    val queueMode: DialogueQueueMode = DialogueQueueMode.Flush
)

data class DialogueContext(
    val feature: AppFeature = AppFeature.GuidedLearning,
    val locale: String = "en",
    val currentLessonId: String? = null,
    val currentLessonIndex: Int = 0,
    val trainingPhase: String? = null,
    val attemptCount: Int = 0,
    val successCount: Int = 0,
    val failureCount: Int = 0,
    val consecutiveFailures: Int = 0,
    val completedLessonCount: Int = 0,
    val firstSuccessfulBlink: Boolean = false,
    val firstSpokenPhrase: Boolean = false,
    val returningUser: Boolean = false,
    val practiceMode: Boolean = false,
    val navigationTraining: Boolean = false,
    val emergencyTraining: Boolean = false,
    val daysSinceLastSession: Int = 0,
    val tutorialSkipped: Boolean = false,
    val guidedLearningComplete: Boolean = false,
    val targetPhrase: String? = null,
    val navigationAction: String? = null,
    val milestoneType: MilestoneType? = null,
    val celebrationTier: Int = 1,
    val deterministicSeed: Int? = null,
    /** Milliseconds since last blink activity — for gentle long-pause encouragement. */
    val idleDurationMs: Long = 0L,
    /** Caregiver may be reading the screen alongside the user. */
    val caregiverVisible: Boolean = false,
    /** Session or coach suggests rest — fatigue-aware check-in only. */
    val fatigueSuggested: Boolean = false,
    /** Setup wizard step (0-based) for caregiver positioning hints. */
    val setupStep: Int = 0,
    /** Calibration dot progress for caregiver support. */
    val calibrationDotIndex: Int = 0,
    val calibrationTotalDots: Int = 5,
    /** Face currently visible to camera. */
    val faceDetected: Boolean = true,
    /** Calibration quality was poor on last attempt. */
    val calibrationPoor: Boolean = false
)

data class LisaDialogue(
    val id: String,
    val text: String,
    val category: DialogueCategory,
    val tone: DialogueTone = DialogueTone.Warm,
    val priority: DialoguePriority = DialoguePriority.Normal,
    val timing: DialogueTiming = DialogueTiming(),
    val interruptible: Boolean = true,
    val repeatable: Boolean = true,
    val locale: String = "en",
    val milestoneType: MilestoneType? = null,
    val lessonId: String? = null,
    val contextTags: Set<String> = emptySet(),
    val weight: Int = 1
)

private val PERSONALITY_SAFETY_RULES: List<String> = listOf(
    "Never infer private emotions",
    "Never punish mistakes",
    "Never rush the user",
    "Use observable state only"
)

private val PERSONALITY_FORBIDDEN_TONES: List<String> = listOf(
    "robotic", "cold", "clinical", "condescending", "overly_cheerful",
    "childish", "sarcastic", "judgmental", "rushed", "harsh"
)

private val PERSONALITY_FORBIDDEN_PHRASES: List<String> = listOf(
    "Wrong", "Failed", "Incorrect", "Bad", "Try harder",
    "You are doing it wrong", "That was not good",
    "You should have done better", "You are failing",
    "This is easy", "Why can't you do it",
    "I know you are frustrated", "I know you are sad",
    "I know how you feel",
    "This will improve your condition",
    "This therapy will help you recover",
    "This will treat your symptoms"
)

private val PERSONALITY_MEDICAL_BOUNDARIES: List<String> = listOf(
    "No treatment claims",
    "No recovery promises",
    "Practice-focused language only"
)

private val PERSONALITY_AI_RULES: List<String> = listOf(
    "AI output must pass forbidden phrase filter",
    "AI output must use LisaDialogue model",
    "AI provider implements LisaDialogueGenerator interface"
)

data class LisaPersonalityProfile(
    val communicationStyle: String = "warm_rehabilitation_companion",
    val vocabularyPreference: String = "simple_accessible",
    val speakingPace: Float = 0.9f,
    val sentenceComplexity: String = "short_clear",
    val encouragementPhilosophy: String = "calm_meaningful_not_excessive",
    val teachingPhilosophy: String = "patient_step_by_step",
    val celebrationPhilosophy: String = "subtle_respectful_tiered",
    val comfortPhilosophy: String = "never_punish_never_rush",
    val emotionalSafetyRules: List<String> = PERSONALITY_SAFETY_RULES,
    val forbiddenToneRules: List<String> = PERSONALITY_FORBIDDEN_TONES,
    val forbiddenPhrases: List<String> = PERSONALITY_FORBIDDEN_PHRASES,
    val medicalClaimBoundaries: List<String> = PERSONALITY_MEDICAL_BOUNDARIES,
    val aiExtensionRules: List<String> = PERSONALITY_AI_RULES,
    val localizationHooks: List<String> = listOf("locale_field", "catalog_provider", "context_locale")
) {
    companion object {
        val DEFAULT: LisaPersonalityProfile = LisaPersonalityProfile()

        val DEFAULT_SAFETY_RULES: List<String> get() = PERSONALITY_SAFETY_RULES
        val DEFAULT_FORBIDDEN_TONES: List<String> get() = PERSONALITY_FORBIDDEN_TONES
        val DEFAULT_FORBIDDEN_PHRASES: List<String> get() = PERSONALITY_FORBIDDEN_PHRASES
        val DEFAULT_MEDICAL_BOUNDARIES: List<String> get() = PERSONALITY_MEDICAL_BOUNDARIES
        val DEFAULT_AI_RULES: List<String> get() = PERSONALITY_AI_RULES
    }
}
