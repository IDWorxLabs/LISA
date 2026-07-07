package com.idworx.lisa.features.onboardingguide.lessoninteraction

/**
 * Small, reusable provider of short, friendly acknowledgements Lisa gives immediately after the
 * user correctly completes a guided practice sequence (a real workspace navigation lesson step or
 * a phrase lesson gesture). Deliberately generic — callers pass a rotating index (e.g. how many
 * lessons have been completed so far) so the same phrase is not repeated every time, and no
 * specific lesson name or screen is ever referenced here.
 */
object GuidedFeedbackPhrases {

    private val positivePhrases = listOf(
        "Well done.",
        "Great job.",
        "You did it."
    )

    /** A positive completion phrase, rotating by [index] so it varies across consecutive successes. */
    fun positive(index: Int): String {
        val size = positivePhrases.size
        val safeIndex = ((index % size) + size) % size
        return positivePhrases[safeIndex]
    }

    /**
     * Generic red "wrong sequence" acknowledgement for real-workspace navigation lessons — the
     * same friendly, try-again tone as the early phrase lessons' "Wrong eye" feedback, but never
     * eye-specific, since a navigation lesson's wrong action can be any unrelated gesture or tap,
     * not just a wrong starting eye.
     */
    fun wrongGesture(): String = "Wrong gesture — try again."
}
