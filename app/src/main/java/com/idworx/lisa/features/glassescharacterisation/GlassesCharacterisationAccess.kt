package com.idworx.lisa.features.glassescharacterisation

/**
 * Debug-only access gate for Glasses Characterisation.
 * Release builds must not expose entry, UI, storage, or reports.
 */
object GlassesCharacterisationAccess {
    const val ENTRY_TITLE = "Glasses Characterisation"
    const val ENTRY_SUPPORTING =
        "Debug only — does lighting improve open/closed eye separation with glasses?"

    fun isEntryVisible(isDebugBuild: Boolean): Boolean = isDebugBuild
    fun isScreenAllowed(isDebugBuild: Boolean): Boolean = isDebugBuild
}

enum class LightingConditionKind(val displayName: String, val caregiverInstruction: String) {
    Normal(
        "Normal Lighting",
        "Use the room lighting normally used with LISA."
    ),
    Brighter(
        "Brighter Lighting",
        "Caregiver: increase the room lighting without shining a light directly into the user’s eyes."
    ),
    Dimmer(
        "Dimmer Lighting",
        "Caregiver: reduce the room lighting while keeping the user’s face visible."
    );

    fun shortCaregiverChange(): String = when (this) {
        Normal -> "Use the room lighting normally used with LISA."
        Brighter -> "Increase the room lighting."
        Dimmer -> "Reduce the room lighting."
    }
}

enum class LightingSourceLabel(val displayName: String) {
    CeilingLight("Ceiling light"),
    Lamp("Lamp"),
    Daylight("Daylight"),
    Mixed("Mixed lighting"),
    OtherUnknown("Other / Unknown")
}

enum class SignalQualityClass {
    Strong,
    Moderate,
    Weak,
    Unusable
}

enum class DecisionSupportCategory {
    CONTINUE_PERSONALISED_PROFILE_RESEARCH,
    CONTINUE_WITH_ENVIRONMENT_GUIDANCE,
    INSUFFICIENT_EVIDENCE,
    SIGNAL_REMAINS_UNRELIABLE,
    CONSIDER_ALTERNATIVE_EYE_DETECTOR
}

enum class GlassesCharUiPhase {
    Hub,
    Setup,
    /** First lighting prep only (Normal). */
    LightingPrep,
    /** Caregiver lighting change — same session, no menu. */
    LightingTransition,
    Running,
    /** Auto progress after final lighting — no button. */
    Analysing,
    FinalReport
}

enum class GlassesCharFlowPhase {
    WaitFace,
    Stabilize,
    /** Subsequent conditions: match original baseline before recording. */
    ValidateAgainstBaseline,
    PrepareOpen,
    RecordOpen,
    CompleteOpen,
    PrepareLeft,
    RecordLeft,
    RecoverLeft,
    PrepareRight,
    RecordRight,
    RecoverRight,
    ObserveBlink,
    ConditionDone
}

data class PoseSnapshot(
    val faceCenterXPct: Float? = null,
    val faceCenterYPct: Float? = null,
    val faceWidthPct: Float? = null,
    val yaw: Float? = null,
    val roll: Float? = null
)

data class SetupSnapshot(
    val pose: PoseSnapshot = PoseSnapshot(),
    val cameraResolution: String = "n/a",
    val deviceOrientation: String = "n/a",
    val sensitivityLevel: Int = 0,
    val responseTimeLabel: String = "n/a",
    val screenBrightness: Float? = null,
    val ambientLux: Float? = null,
    val standardClosedThreshold: Float = 0f,
    val standardOpenThreshold: Float = 0f
)

data class DistributionSummary(
    val count: Int = 0,
    val average: Float? = null,
    val median: Float? = null,
    val stdDev: Float? = null,
    val min: Float? = null,
    val max: Float? = null,
    val p10: Float? = null,
    val p25: Float? = null,
    val p50: Float? = null,
    val p75: Float? = null,
    val p90: Float? = null
)

data class EyeSeparationMetrics(
    val open: DistributionSummary = DistributionSummary(),
    val closed: DistributionSummary = DistributionSummary(),
    val openP25MinusClosedP75: Float? = null,
    val openMedianMinusClosedMedian: Float? = null,
    val overlapPercent: Float = 100f,
    val closedBelowClosedThrPct: Float = 0f,
    val closedInUncertainPct: Float = 0f,
    val closedAboveOpenThrPct: Float = 0f,
    val openAboveOpenThrPct: Float = 0f,
    val openInUncertainPct: Float = 0f,
    val nullPercent: Float = 0f,
    val rejectedPercent: Float = 0f,
    val jitter: Float? = null
)

data class NaturalBlinkMetrics(
    val durationMs: Long = 0L,
    val blinkCandidates: Int = 0,
    val completedBlinks: Int = 0,
    val cancelledCandidates: Int = 0,
    val falseWinkCandidates: Int = 0,
    val bothEyeClosures: Int = 0,
    val uncertainBandEntries: Int = 0,
    val leftAsymmetryEvents: Int = 0,
    val rightAsymmetryEvents: Int = 0,
    val openInstability: Float? = null
)

data class ConditionResult(
    val condition: LightingConditionKind,
    val sourceLabel: LightingSourceLabel = LightingSourceLabel.OtherUnknown,
    val completed: Boolean = false,
    val endedEarly: Boolean = false,
    val left: EyeSeparationMetrics = EyeSeparationMetrics(),
    val right: EyeSeparationMetrics = EyeSeparationMetrics(),
    val blink: NaturalBlinkMetrics = NaturalBlinkMetrics(),
    val quality: SignalQualityClass = SignalQualityClass.Unusable,
    val usableSampleCount: Int = 0,
    val positionWarningCount: Int = 0,
    val invalidPoseSampleCount: Int = 0,
    val notes: String = ""
)

data class GlassesCharacterisationReport(
    val sessionId: String,
    val testStartedMs: Long,
    val testCompletedMs: Long,
    val reportGeneratedMs: Long,
    val deviceManufacturer: String,
    val deviceModel: String,
    val androidVersion: String,
    val appVersionName: String,
    val versionCode: Int,
    val isDebugBuild: Boolean,
    val setup: SetupSnapshot,
    val conditions: List<ConditionResult>,
    val positionConsistencyMaintained: Boolean = true,
    val phoneMovementWarningCount: Int = 0,
    val patientMovementWarningCount: Int = 0,
    val bestLeftLighting: LightingConditionKind?,
    val bestRightLighting: LightingConditionKind?,
    val bestOverallLighting: LightingConditionKind?,
    val improvementConsistentBothEyes: Boolean,
    val lightingProducedNoMeaningfulImprovement: Boolean,
    val findings: List<String>,
    val caregiverRecommendations: List<String>,
    val decision: DecisionSupportCategory,
    val decisionExplanation: String,
    val limitations: List<String>,
    val incomplete: Boolean = false
)
