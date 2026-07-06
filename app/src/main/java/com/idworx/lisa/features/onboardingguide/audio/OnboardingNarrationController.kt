package com.idworx.lisa.features.onboardingguide.audio

import android.speech.tts.TextToSpeech
import com.idworx.lisa.features.onboardingguide.model.TrainingPreferences
import com.idworx.lisa.features.personality.model.LisaDialogue
import com.idworx.lisa.features.silentwelcome.LisaSpeechPolicy

/**
 * Narration layer over the existing Text-to-Speech engine.
 * Does not create a duplicate speech engine.
 */
class OnboardingNarrationController(
    private val ttsProvider: () -> TextToSpeech?,
    private val preferencesProvider: () -> TrainingPreferences,
    private val onSpeakingChanged: (Boolean) -> Unit = {}
) {
    var onSequenceComplete: (() -> Unit)? = null
    private var queue: List<String> = emptyList()
    private var queueIndex: Int = 0
    private var paused: Boolean = false
    private var lastUtterance: String? = null

    var isSpeaking: Boolean = false
        private set

    fun handleUtteranceStart(utteranceId: String?) {
        if (utteranceId?.startsWith(NARRATION_PREFIX) == true) {
            isSpeaking = true
            onSpeakingChanged(true)
        }
    }

    fun handleUtteranceDone(utteranceId: String?) {
        if (utteranceId?.startsWith(NARRATION_PREFIX) != true) return
        if (paused) return
        if (queueIndex < queue.size - 1) {
            queueIndex++
            speakInternal(queue[queueIndex], utteranceIdFor(queueIndex))
        } else {
            isSpeaking = false
            onSpeakingChanged(false)
            queue = emptyList()
            queueIndex = 0
            onSequenceComplete?.invoke()
        }
    }

    fun speak(text: String) {
        if (!LisaSpeechPolicy.allowsNarration()) return
        lastUtterance = text
        queue = listOf(text)
        queueIndex = 0
        paused = false
        speakInternal(text, utteranceIdFor(0))
    }

    fun speakSequence(lines: List<String>) {
        if (!LisaSpeechPolicy.allowsNarration()) return
        if (lines.isEmpty()) return
        lastUtterance = lines.first()
        queue = lines
        queueIndex = 0
        paused = false
        applyPreferences()
        speakInternal(lines.first(), utteranceIdFor(0))
    }

    fun speakDialogue(dialogue: LisaDialogue) {
        if (!LisaSpeechPolicy.allowsNarration()) return
        applyDialogueTiming(dialogue)
        speak(dialogue.text)
    }

    fun speakDialogueSequence(dialogues: List<LisaDialogue>) {
        if (!LisaSpeechPolicy.allowsNarration()) return
        speakSequence(dialogues.map { it.text })
    }

    private fun applyDialogueTiming(dialogue: LisaDialogue) {
        val tts = ttsProvider() ?: return
        tts.setSpeechRate(dialogue.timing.recommendedSpeechRate.coerceIn(0.5f, 1.5f))
    }

    fun pause() {
        paused = true
        ttsProvider()?.stop()
        isSpeaking = false
        onSpeakingChanged(false)
    }

    fun resume() {
        if (queue.isEmpty()) {
            lastUtterance?.let { speak(it) }
            return
        }
        paused = false
        speakInternal(queue[queueIndex], utteranceIdFor(queueIndex))
    }

    fun repeatLast() {
        lastUtterance?.let { speak(it) }
    }

    fun replayCurrentQueue() {
        if (queue.isEmpty()) {
            repeatLast()
            return
        }
        queueIndex = 0
        paused = false
        speakInternal(queue.first(), utteranceIdFor(0))
    }

    fun skip() {
        ttsProvider()?.stop()
        queue = emptyList()
        queueIndex = 0
        paused = false
        isSpeaking = false
        onSpeakingChanged(false)
    }

    fun applyPreferences() {
        val tts = ttsProvider() ?: return
        val prefs = preferencesProvider()
        tts.setSpeechRate(prefs.narrationSpeed.coerceIn(0.5f, 1.5f))
    }

    private fun speakInternal(text: String, utteranceId: String) {
        val prefs = preferencesProvider()
        if (!prefs.narrationEnabled) {
            isSpeaking = false
            onSpeakingChanged(false)
            return
        }
        applyPreferences()
        val tts = ttsProvider() ?: return
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    private fun utteranceIdFor(index: Int): String = "$NARRATION_PREFIX$index"

    companion object {
        const val NARRATION_PREFIX: String = "LISA_TRAINING_NARRATION_"

        fun isNarrationUtterance(utteranceId: String?): Boolean =
            utteranceId?.startsWith(NARRATION_PREFIX) == true
    }
}
