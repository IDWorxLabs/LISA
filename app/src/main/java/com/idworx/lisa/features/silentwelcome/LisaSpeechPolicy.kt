package com.idworx.lisa.features.silentwelcome

/**
 * When [PHRASE_TRANSLATION_ONLY] is true, LISA speaks only completed blink-sequence phrases
 * (e.g. HELLO → "Hello"). All lesson, setup, coaching, and presence narration is suppressed.
 */
object LisaSpeechPolicy {

    const val PHRASE_TRANSLATION_ONLY: Boolean = true

    fun allowsNarration(): Boolean = !PHRASE_TRANSLATION_ONLY

    fun allowsPhraseTranslation(): Boolean = true
}
