package com.idworx.lisa.features.corecommunicationreliability.model

enum class CommunicationReliabilityOutcome {
    PASS,
    WARN,
    BLOCKED,
    FAIL
}

enum class CommunicationMode {
    MAIN,
    TRAINING,
    PRACTICE,
    GUIDED,
    QUICK_CONTROLS,
    EMERGENCY
}

enum class PhraseReliabilityAction {
    PROCEED_TO_CONFIRMATION,
    PROCEED_IMMEDIATE,
    BLOCK,
    ROUTE_EMERGENCY,
    ROUTE_EMERGENCY_TRAINING,
    NO_PHRASE
}

data class ReliabilityMetric(
    val name: String,
    val value: Float,
    val unit: String? = null
)

data class SequenceRecognitionResult(
    val normalizedSequence: String,
    val left: Int,
    val right: Int,
    val valid: Boolean,
    val isComplete: Boolean,
    val isReserved: Boolean,
    val isAmbiguous: Boolean,
    val hasContinuation: Boolean,
    val blockedReason: String? = null
)

data class SpeechOutputResult(
    val requested: Boolean,
    val phraseText: String? = null,
    val phraseId: String? = null,
    val success: Boolean = false,
    val failureReason: String? = null,
    val timestampMs: Long = System.currentTimeMillis()
)

data class EmergencySafetyResult(
    val allowed: Boolean,
    val isEmergencySequence: Boolean,
    val trainingMode: Boolean,
    val navigationTraining: Boolean,
    val requiresConfirmation: Boolean,
    val blockedReason: String? = null
)

data class CommunicationAttempt(
    val attemptId: String,
    val timestampMs: Long,
    val rawLeft: Int,
    val rawRight: Int,
    val mode: CommunicationMode,
    val sequenceSummary: String
)

data class CommunicationAttemptResult(
    val action: PhraseReliabilityAction,
    val outcome: CommunicationReliabilityOutcome,
    val phraseId: String? = null,
    val phraseText: String? = null,
    val confidenceScore: Float = 0f,
    val requiresConfirmation: Boolean = true,
    val blockedReason: String? = null,
    val warnings: List<String> = emptyList()
)

data class CommunicationReliabilityReport(
    val attemptId: String,
    val timestampMs: Long,
    val rawSequenceSummary: String,
    val normalizedSequence: String,
    val validationResult: SequenceRecognitionResult,
    val confidenceScore: Float,
    val matchedPhraseId: String? = null,
    val matchedPhraseText: String? = null,
    val confirmationRequired: Boolean,
    val attemptResult: CommunicationAttemptResult,
    val speechOutputResult: SpeechOutputResult? = null,
    val emergencySafetyResult: EmergencySafetyResult? = null,
    val finalOutcome: CommunicationReliabilityOutcome,
    val failureReason: String? = null,
    val warnings: List<String> = emptyList(),
    val metrics: List<ReliabilityMetric> = emptyList()
)

data class SpokenPhraseRecord(
    val recordId: String,
    val phraseId: String?,
    val phraseText: String,
    val timestampMs: Long,
    val sequenceLeft: Int,
    val sequenceRight: Int,
    val sequenceLabel: String,
    val mode: CommunicationMode,
    val emergency: Boolean,
    val speechSuccess: Boolean
)
