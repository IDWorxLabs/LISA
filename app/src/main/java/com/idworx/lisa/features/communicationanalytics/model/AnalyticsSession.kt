package com.idworx.lisa.features.communicationanalytics.model

import com.idworx.lisa.features.calibrationreliability.model.CalibrationHealthState
import com.idworx.lisa.features.corecommunicationreliability.model.CommunicationMode
import com.idworx.lisa.features.corecommunicationreliability.model.CommunicationReliabilityOutcome
import com.idworx.lisa.features.corecommunicationreliability.model.PhraseReliabilityAction

data class AttemptTiming(
    val firstBlinkMs: Long? = null,
    val sequenceCompleteMs: Long? = null,
    val phraseMatchMs: Long? = null,
    val speechRequestMs: Long? = null,
    val speechCompleteMs: Long? = null
) {
    val blinkToSpeechMs: Long?
        get() = if (firstBlinkMs != null && speechCompleteMs != null) speechCompleteMs - firstBlinkMs else null

    val sequenceToMatchMs: Long?
        get() = if (sequenceCompleteMs != null && phraseMatchMs != null) phraseMatchMs - sequenceCompleteMs else null
}

data class CommunicationAttemptAnalytics(
    val attemptId: String,
    val timestampMs: Long,
    val mode: CommunicationMode,
    val calibrationHealth: CalibrationHealthState?,
    val calibrationScore: Int?,
    val phraseId: String?,
    val phraseText: String?,
    val confidenceScore: Float,
    val action: PhraseReliabilityAction,
    val finalOutcome: CommunicationReliabilityOutcome,
    val blockedReason: String?,
    val emergency: Boolean,
    val emergencyTraining: Boolean,
    val navigationTraining: Boolean,
    val communicationTraining: Boolean,
    val practiceMode: Boolean,
    val speechSuccess: Boolean? = null,
    val speechFailureReason: String? = null,
    val retryCount: Int = 0,
    val duplicateBlocked: Boolean = false,
    val calibrationBlocked: Boolean = false,
    val timing: AttemptTiming = AttemptTiming(),
    val sequenceLabel: String
)

data class AnalyticsSession(
    val sessionId: String,
    val startTimeMs: Long,
    var endTimeMs: Long? = null,
    val attemptIds: MutableList<String> = mutableListOf(),
    val practiceMode: Boolean = false,
    val trainingMode: Boolean = false
) {
    val durationMs: Long
        get() = (endTimeMs ?: System.currentTimeMillis()) - startTimeMs

    val attemptCount: Int get() = attemptIds.size
}
