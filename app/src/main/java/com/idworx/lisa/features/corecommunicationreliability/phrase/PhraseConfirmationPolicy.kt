package com.idworx.lisa.features.corecommunicationreliability.phrase

import com.idworx.lisa.features.corecommunicationreliability.metadata.CoreCommunicationReliabilityMetadata

data class ConfirmationDecision(
    val requiresConfirmation: Boolean,
    val allowImmediate: Boolean,
    val reason: String
)

object PhraseConfirmationPolicy {

    fun decide(
        confidence: Float,
        isEmergency: Boolean,
        trainingMode: Boolean
    ): ConfirmationDecision {
        if (isEmergency) {
            return ConfirmationDecision(
                requiresConfirmation = true,
                allowImmediate = false,
                reason = "Emergency phrases always require explicit confirmation path"
            )
        }
        if (trainingMode) {
            return ConfirmationDecision(
                requiresConfirmation = true,
                allowImmediate = false,
                reason = "Training mode uses guided confirmation"
            )
        }
        if (confidence >= CoreCommunicationReliabilityMetadata.CONFIDENCE_THRESHOLD_IMMEDIATE) {
            return ConfirmationDecision(
                requiresConfirmation = true,
                allowImmediate = false,
                reason = "High confidence — standard countdown confirmation applies"
            )
        }
        if (confidence >= CoreCommunicationReliabilityMetadata.CONFIDENCE_THRESHOLD_CONFIRM) {
            return ConfirmationDecision(
                requiresConfirmation = true,
                allowImmediate = false,
                reason = "Medium confidence — confirmation required"
            )
        }
        return ConfirmationDecision(
            requiresConfirmation = true,
            allowImmediate = false,
            reason = "Low confidence — retry recommended"
        )
    }
}
