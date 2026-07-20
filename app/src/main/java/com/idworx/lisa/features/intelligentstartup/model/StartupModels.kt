package com.idworx.lisa.features.intelligentstartup.model

/**
 * RC7D.34/35 — Intelligent Startup Flow phases.
 * Splash remains OS-owned; this machine prepares eye tracking before Welcome.
 */
enum class StartupPhase {
    FaceDetection,
    ProfileResolution,
    CreatePrimaryUser,
    ProfileSelection,
    EvaluatingCompatibility,
    QuickCalibration,
    CalibrationFailure,
    EyeTrackingReady,
    Complete
}

enum class QuickCalibrationStep {
    LookNaturally,
    BlinkThreeTimes,
    LeftWinkTwice,
    RightWinkTwice,
    CalibrationComplete;

    fun next(): QuickCalibrationStep? = when (this) {
        LookNaturally -> BlinkThreeTimes
        BlinkThreeTimes -> LeftWinkTwice
        LeftWinkTwice -> RightWinkTwice
        RightWinkTwice -> CalibrationComplete
        CalibrationComplete -> null
    }
}

enum class CalibrationConfidenceLevel {
    Missing,
    Low,
    High
}

enum class CalibrationCompatibilityLevel {
    High,
    Medium,
    Low
}

/**
 * Per-profile eye calibration payload. Stored locally on [com.idworx.lisa.LisaUserProfile].
 * Does NOT store biometric facial identity.
 */
data class ProfileEyeCalibration(
    val leftClosedEyeThreshold: Float,
    val rightClosedEyeThreshold: Float,
    val openEyeThreshold: Float,
    val blinkDurationMs: Long,
    val requiredWinkFrames: Int,
    val eyeOpennessBaseline: Float,
    val faceDistanceProxy: Float,
    val eyeSpacingProxy: Float = 0.35f,
    val confidence: Float,
    val calibratedAtMs: Long,
    val compatibilityHistory: List<CalibrationCompatibilityRecord> = emptyList()
)

data class CalibrationCompatibilityRecord(
    val level: CalibrationCompatibilityLevel,
    val score: Float,
    val evaluatedAtMs: Long
)

data class LiveCompatibilitySample(
    val eyeOpennessBaseline: Float,
    val faceDistanceProxy: Float,
    val eyeSpacingProxy: Float,
    val leftCloseCharacteristic: Float? = null,
    val rightCloseCharacteristic: Float? = null,
    val blinkDurationMs: Long? = null
)

data class StartupProfileChoice(
    val id: String,
    val name: String,
    val languageLabel: String,
    val communicationLevelLabel: String,
    val lastCalibratedAtMs: Long?
)

data class StartupFlowState(
    val phase: StartupPhase = StartupPhase.FaceDetection,
    val faceDetected: Boolean = false,
    val lookingForFaceMessage: Boolean = true,
    val profileChoices: List<StartupProfileChoice> = emptyList(),
    val selectedProfileIndex: Int = 0,
    val selectedProfileId: String? = null,
    val createNameDraft: String = "Primary User",
    val createLanguageLabel: String = "English",
    val createLevelLabel: String = "Beginner",
    val calibrationStep: QuickCalibrationStep = QuickCalibrationStep.LookNaturally,
    val blinksCollected: Int = 0,
    val leftWinksCollected: Int = 0,
    val rightWinksCollected: Int = 0,
    val failureCount: Int = 0,
    val eyeControlActive: Boolean = false,
    val calibration: ProfileEyeCalibration? = null,
    val compatibilityLevel: CalibrationCompatibilityLevel? = null,
    val skippedCalibration: Boolean = false,
    val isActive: Boolean = true
) {
    val blocksMainUi: Boolean
        get() = isActive && phase != StartupPhase.Complete

    val selectedProfileChoice: StartupProfileChoice?
        get() = profileChoices.getOrNull(selectedProfileIndex)
}

sealed class StartupEvent {
    data class FacePresenceChanged(val present: Boolean) : StartupEvent()
    data object BeginProfileResolution : StartupEvent()
    data class ProfilesResolvedNone(val defaultName: String) : StartupEvent()
    data class ProfilesResolvedSingle(val profileId: String) : StartupEvent()
    data class ProfilesResolvedMultiple(val choices: List<StartupProfileChoice>) : StartupEvent()
    data class CreatePrimaryUserDraftChanged(
        val name: String? = null,
        val languageLabel: String? = null,
        val levelLabel: String? = null
    ) : StartupEvent()
    data class PrimaryUserCreated(val profileId: String) : StartupEvent()
    data object MoveProfileSelectionUp : StartupEvent()
    data object MoveProfileSelectionDown : StartupEvent()
    data object SelectHighlightedProfile : StartupEvent()
    data object BeginCompatibilityEvaluation : StartupEvent()
    data class CompatibilityEvaluated(val level: CalibrationCompatibilityLevel) : StartupEvent()
    /** Retained for RC7D.34 callers; maps onto compatibility routing. */
    data class ConfidenceEvaluated(val level: CalibrationConfidenceLevel) : StartupEvent()
    data object AdvanceCalibrationStep : StartupEvent()
    data object CalibrationSucceeded : StartupEvent()
    data object CalibrationFailed : StartupEvent()
    data object RetryCalibration : StartupEvent()
    data object AcknowledgeEyeTrackingReady : StartupEvent()
    data class CalibrationCaptured(val calibration: ProfileEyeCalibration) : StartupEvent()
}
