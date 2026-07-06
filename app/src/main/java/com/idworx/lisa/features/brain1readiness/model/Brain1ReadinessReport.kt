package com.idworx.lisa.features.brain1readiness.model

enum class Brain1ReadinessOutcome {
    READY_FOR_DEVICE_TESTING,
    READY_WITH_WARNINGS,
    NOT_READY,
    BLOCKED
}

enum class Brain1SubsystemStatus {
    Ready,
    Degraded,
    Missing,
    Blocked
}

enum class Brain1Subsystem {
    GuidedLearning,
    PersonalityEngine,
    CompanionMemory,
    CoreCommunication,
    CalibrationReliability,
    CommunicationAnalytics,
    AccessibilityConsistency,
    OfflineReliability,
    EmergencySpeech,
    PracticeMode,
    Settings,
    CommunicationHistory,
    TextToSpeech,
    EyeTracking,
    BlinkDetection,
    Navigation,
    DeviceTesting,
    Integration
}

enum class Brain1RiskSeverity {
    Low,
    Medium,
    High,
    Critical
}

enum class Brain1ScoreBand {
    ReadyForDeviceTesting,
    ReadyWithWarnings,
    NotReady,
    Blocked;

    companion object {
        fun fromScore(score: Int): Brain1ScoreBand = when {
            score >= com.idworx.lisa.features.brain1readiness.metadata.Brain1ReadinessMetadata.SCORE_READY ->
                ReadyForDeviceTesting
            score >= com.idworx.lisa.features.brain1readiness.metadata.Brain1ReadinessMetadata.SCORE_READY_WITH_WARNINGS ->
                ReadyWithWarnings
            score >= com.idworx.lisa.features.brain1readiness.metadata.Brain1ReadinessMetadata.SCORE_NOT_READY ->
                NotReady
            else -> Blocked
        }
    }
}

data class Brain1Evidence(
    val evidenceId: String,
    val subsystem: Brain1Subsystem,
    val description: String,
    val source: String
)

data class Brain1Risk(
    val riskId: String,
    val subsystem: Brain1Subsystem,
    val severity: Brain1RiskSeverity,
    val description: String,
    val evidence: String
)

data class Brain1Gap(
    val gapId: String,
    val subsystem: Brain1Subsystem,
    val description: String,
    val blocksReadiness: Boolean = false
)

data class Brain1Recommendation(
    val recommendationId: String,
    val subsystem: Brain1Subsystem,
    val message: String,
    val priority: String
)

data class Brain1ReadinessScore(
    val overall: Int,
    val band: Brain1ScoreBand,
    val subsystemsReady: Int,
    val subsystemsTotal: Int,
    val checksPassed: Int,
    val checksTotal: Int,
    val evidence: String
)

data class Brain1SubsystemReview(
    val subsystem: Brain1Subsystem,
    val status: Brain1SubsystemStatus,
    val reviewerName: String,
    val checksPassed: Int,
    val checksPerformed: Int,
    val evidence: String
)

data class Brain1ReadinessReport(
    val reportId: String,
    val generatedAtMs: Long,
    val outcome: Brain1ReadinessOutcome,
    val score: Brain1ReadinessScore,
    val subsystemReviews: List<Brain1SubsystemReview>,
    val risks: List<Brain1Risk>,
    val gaps: List<Brain1Gap>,
    val recommendations: List<Brain1Recommendation>,
    val evidence: List<Brain1Evidence>,
    val summary: String,
    val honestAssessment: String,
    val evidenceSummary: String
)
