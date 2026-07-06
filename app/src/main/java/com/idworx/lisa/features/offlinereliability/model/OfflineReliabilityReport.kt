package com.idworx.lisa.features.offlinereliability.model

enum class OfflineCapabilityStatus {
    Ready,
    Degraded,
    Unavailable,
    Unknown
}

enum class OfflineCapability {
    EyeTracking,
    BlinkDetection,
    Calibration,
    SequenceRecognition,
    PhraseMatching,
    PhraseSelection,
    PhraseSpeech,
    CommunicationHistory,
    PracticeMode,
    GuidedLearning,
    Navigation,
    AccessibilitySettings,
    PersonalityDialogue,
    CompanionMemory,
    EmergencyCommunication,
    EmergencyTraining,
    Settings,
    TextToSpeech
}

enum class OfflineSeverity {
    Info,
    Warning,
    Error,
    Critical
}

enum class OfflineScoreBand {
    Excellent,
    Good,
    Acceptable,
    NeedsImprovement,
    Critical;

    companion object {
        fun fromScore(score: Int): OfflineScoreBand = when {
            score >= com.idworx.lisa.features.offlinereliability.metadata.OfflineReliabilityMetadata.SCORE_EXCELLENT -> Excellent
            score >= com.idworx.lisa.features.offlinereliability.metadata.OfflineReliabilityMetadata.SCORE_GOOD -> Good
            score >= com.idworx.lisa.features.offlinereliability.metadata.OfflineReliabilityMetadata.SCORE_ACCEPTABLE -> Acceptable
            score >= com.idworx.lisa.features.offlinereliability.metadata.OfflineReliabilityMetadata.SCORE_NEEDS_IMPROVEMENT -> NeedsImprovement
            else -> Critical
        }
    }
}

data class OfflineRequirement(
    val requirementId: String,
    val capability: OfflineCapability,
    val description: String,
    val mandatory: Boolean = true
)

data class OfflineWarning(
    val warningId: String,
    val capability: OfflineCapability,
    val message: String,
    val evidence: String
)

data class OfflineRecommendation(
    val recommendationId: String,
    val capability: OfflineCapability,
    val message: String,
    val evidence: String
)

data class OfflineValidationResult(
    val validatorName: String,
    val capability: OfflineCapability,
    val status: OfflineCapabilityStatus,
    val checksPerformed: Int,
    val checksPassed: Int,
    val warnings: List<OfflineWarning>,
    val evidence: String
)

data class OfflineReliabilityMetrics(
    val capabilitiesReady: Int,
    val capabilitiesTotal: Int,
    val totalChecks: Int,
    val checksPassed: Int,
    val warnings: Int,
    val criticalDependencies: Int,
    val evidence: String
)

data class OfflineReliabilityScore(
    val overall: Int,
    val band: OfflineScoreBand,
    val metrics: OfflineReliabilityMetrics,
    val evidence: String
)

data class OfflineReliabilityReport(
    val reportId: String,
    val generatedAtMs: Long,
    val score: OfflineReliabilityScore,
    val validationResults: List<OfflineValidationResult>,
    val capabilities: Map<OfflineCapability, OfflineCapabilityStatus>,
    val warnings: List<OfflineWarning>,
    val recommendations: List<OfflineRecommendation>,
    val detectedDependencies: List<String>,
    val summary: String,
    val brain1OfflineReady: Boolean,
    val evidenceSummary: String
)
