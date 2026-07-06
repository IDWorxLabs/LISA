package com.idworx.lisa.features.corecommunicationreliability.speech

import com.idworx.lisa.features.corecommunicationreliability.model.SpeechOutputResult

object SpeechOutputVerifier {

    fun verifyRequest(
        phraseText: String,
        phraseId: String?,
        ttsAvailable: Boolean,
        speechBlocked: Boolean
    ): SpeechOutputResult {
        if (speechBlocked) {
            return SpeechOutputResult(
                requested = false,
                phraseText = phraseText,
                phraseId = phraseId,
                success = false,
                failureReason = "Speech blocked by reliability guard"
            )
        }
        if (!ttsAvailable) {
            return SpeechOutputResult(
                requested = true,
                phraseText = phraseText,
                phraseId = phraseId,
                success = false,
                failureReason = "TTS service unavailable"
            )
        }
        if (phraseText.isBlank()) {
            return SpeechOutputResult(
                requested = false,
                phraseText = phraseText,
                phraseId = phraseId,
                success = false,
                failureReason = "Empty phrase text"
            )
        }
        return SpeechOutputResult(
            requested = true,
            phraseText = phraseText,
            phraseId = phraseId,
            success = true
        )
    }

    fun recordDelivery(
        prior: SpeechOutputResult,
        delivered: Boolean,
        failureReason: String? = null
    ): SpeechOutputResult = prior.copy(
        success = delivered,
        failureReason = if (delivered) null else failureReason,
        timestampMs = System.currentTimeMillis()
    )
}

/**
 * Adapter hook for existing TTS — does not create a duplicate engine.
 * MainActivity calls [onSpeechRequested] before speak() and [onSpeechFinished] after TTS completes.
 */
class SpeechReliabilityAdapter {

    private var lastRequest: SpeechOutputResult? = null

    fun onSpeechRequested(
        phraseText: String,
        phraseId: String?,
        ttsAvailable: Boolean,
        blocked: Boolean = false
    ): SpeechOutputResult {
        lastRequest = SpeechOutputVerifier.verifyRequest(phraseText, phraseId, ttsAvailable, blocked)
        return lastRequest!!
    }

    fun onSpeechFinished(success: Boolean, failureReason: String? = null): SpeechOutputResult? {
        lastRequest = lastRequest?.let { SpeechOutputVerifier.recordDelivery(it, success, failureReason) }
        return lastRequest
    }

    fun lastResult(): SpeechOutputResult? = lastRequest
}
