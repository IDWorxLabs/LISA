package com.idworx.lisa.features.corecommunicationreliability.phrase

import com.idworx.lisa.features.corecommunicationreliability.metadata.CoreCommunicationReliabilityMetadata
import com.idworx.lisa.features.corecommunicationreliability.model.CommunicationMode
import com.idworx.lisa.features.corecommunicationreliability.model.SequenceRecognitionResult
import com.idworx.lisa.features.corecommunicationreliability.sequence.BlinkSequenceValidator

data class PhraseSelectionDecision(
    val allowed: Boolean,
    val blockedReason: String? = null
)

object PhraseSelectionGuard {

    fun evaluate(
        validation: SequenceRecognitionResult,
        match: PhraseMatchResult,
        confidence: Float,
        mode: CommunicationMode,
        listeningPaused: Boolean,
        duplicateBlocked: Boolean,
        emergencyConfirmationRequired: Boolean,
        calibrationAllowsCommunication: Boolean = true
    ): PhraseSelectionDecision {
        if (duplicateBlocked) {
            return PhraseSelectionDecision(false, "Duplicate sequence firing prevented")
        }
        if (listeningPaused && mode == CommunicationMode.MAIN) {
            return PhraseSelectionDecision(false, "Speech blocked while listening is paused")
        }
        if (validation.isAmbiguous || match.matchCount > 1) {
            return PhraseSelectionDecision(false, "Ambiguous phrase match")
        }
        if (validation.blockedReason != null && !match.isEmergency) {
            return PhraseSelectionDecision(false, validation.blockedReason)
        }
        if (mode == CommunicationMode.MAIN && validation.hasContinuation && match.mapping == null) {
            return PhraseSelectionDecision(false, "Incomplete sequence — longer phrase may exist")
        }
        if (match.mapping != null && !BlinkSequenceValidator.isPhraseSpeakable(
                match.mapping,
                com.idworx.lisa.PreferredLanguage.English
            )
        ) {
            return PhraseSelectionDecision(false, "Phrase is not speakable")
        }
        if (match.mapping == null && !match.isEmergency) {
            return PhraseSelectionDecision(false, "No phrase matches sequence")
        }
        if (confidence < CoreCommunicationReliabilityMetadata.CONFIDENCE_THRESHOLD_BLOCK &&
            !match.isEmergency
        ) {
            return PhraseSelectionDecision(false, "Confidence below safe threshold")
        }
        if (match.isEmergency && emergencyConfirmationRequired) {
            return PhraseSelectionDecision(false, "Emergency requires confirmation in current mode")
        }
        if (!calibrationAllowsCommunication) {
            return PhraseSelectionDecision(false, "Communication blocked — calibration health insufficient")
        }
        return PhraseSelectionDecision(true)
    }
}
