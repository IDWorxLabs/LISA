package com.idworx.lisa.features.corecommunicationreliability.sequence

import com.idworx.lisa.LisaSystemLanguage
import com.idworx.lisa.PreferredLanguage
import com.idworx.lisa.WinkMapping
import com.idworx.lisa.features.corecommunicationreliability.model.CommunicationMode
import com.idworx.lisa.features.corecommunicationreliability.model.SequenceRecognitionResult
import com.idworx.lisa.hasLongerContinuation
import com.idworx.lisa.isEmergencySequence
import com.idworx.lisa.isSequenceEligibleForSpeech

object BlinkSequenceValidator {

    fun validate(
        left: Int,
        right: Int,
        mappings: List<WinkMapping>,
        mode: CommunicationMode,
        listeningPaused: Boolean = false
    ): SequenceRecognitionResult {
        val normalized = BlinkSequenceNormalizer.normalize(left, right)
        val eligible = isSequenceEligibleForSpeech(left, right)
        val reserved = LisaSystemLanguage.isReservedSystemSequence(left, right) ||
            isEmergencySequence(left, right)
        val matches = mappings.filter { it.left == left && it.right == right }
        val ambiguous = matches.size > 1
        val continuation = hasLongerContinuation(left, right, mappings)
        val exactMatch = matches.size == 1

        val blockedReason = when {
            !eligible -> "Sequence incomplete: fewer than minimum winks"
            listeningPaused && mode == CommunicationMode.MAIN -> "Listening paused"
            ambiguous -> "Ambiguous sequence: multiple phrases match"
            mode == CommunicationMode.MAIN && reserved && !isEmergencySequence(left, right) ->
                "Reserved navigation sequence cannot trigger speech"
            mode == CommunicationMode.MAIN && continuation && !exactMatch ->
                "Sequence may be prefix of longer phrase"
            else -> null
        }

        val valid = blockedReason == null && eligible &&
            (!ambiguous) &&
            (exactMatch || isEmergencySequence(left, right) || mode != CommunicationMode.MAIN)

        return SequenceRecognitionResult(
            normalizedSequence = normalized.label,
            left = left,
            right = right,
            valid = valid,
            isComplete = eligible,
            isReserved = reserved,
            isAmbiguous = ambiguous,
            hasContinuation = continuation,
            blockedReason = blockedReason
        )
    }

    fun isPhraseSpeakable(mapping: WinkMapping, language: PreferredLanguage): Boolean {
        val text = mapping.localizedPhrase(language)
        return text.isNotBlank()
    }
}
