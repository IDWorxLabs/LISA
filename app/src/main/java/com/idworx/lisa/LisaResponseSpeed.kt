package com.idworx.lisa

/** Milliseconds to wait after the last confirmed wink before finalizing a sequence. */
const val SEQUENCE_IDLE_TIMEOUT_MS = 3000L

/** User-selectable delay (seconds) after the last blink before a sequence is processed. */
object SequenceProcessingDelay {
    const val MIN_SECONDS = 1
    const val MAX_SECONDS = 6
    const val DEFAULT_SECONDS = 3

    /**
     * Guided Mode/Training's default settle time — deliberately slower than the everyday
     * Communication Workspace default (3s) so a multi-step lesson gesture (e.g. L4 R4) has time
     * to finish before the sequence is evaluated. Sourced from [TrainingPreferences], never
     * hardcoded per lesson.
     */
    const val GUIDED_DEFAULT_SECONDS = 5

    val allowedSeconds: IntRange = MIN_SECONDS..MAX_SECONDS

    fun coerce(seconds: Int): Int = seconds.coerceIn(MIN_SECONDS, MAX_SECONDS)

    fun toMillis(seconds: Int): Long = coerce(seconds) * 1000L

    fun fromStored(value: Int, legacySpeed: ResponseSpeed? = null): Int {
        if (value in allowedSeconds) return value
        return when (legacySpeed) {
            ResponseSpeed.Fast -> 3
            ResponseSpeed.Normal -> 3
            ResponseSpeed.Slow -> 6
            null -> DEFAULT_SECONDS
        }
    }
}

enum class ResponseSpeed(val idleTimeoutMs: Long) {
    Fast(3000L),
    Normal(3000L),
    Slow(6000L);

    fun slower(): ResponseSpeed = when (this) {
        Fast -> Normal
        Normal -> Slow
        Slow -> Slow
    }

    fun faster(): ResponseSpeed = when (this) {
        Slow -> Normal
        Normal -> Fast
        Fast -> Fast
    }

    fun maxSequenceWindowMs(): Long = maxOf(MIN_SEQUENCE_WINDOW_MS, idleTimeoutMs * 4)

    fun toProcessingDelaySeconds(): Int = SequenceProcessingDelay.coerce((idleTimeoutMs / 1000L).toInt())

    companion object {
        val default: ResponseSpeed = Normal

        fun fromStored(stored: String?, legacyTimeoutSec: Float = default.idleTimeoutMs / 1000f): ResponseSpeed {
            entries.find { it.name.equals(stored, ignoreCase = true) }?.let { return it }
            return when {
                legacyTimeoutSec <= 2.5f -> Fast
                legacyTimeoutSec <= 4.5f -> Normal
                else -> Slow
            }
        }

        fun fromProcessingDelaySeconds(seconds: Int): ResponseSpeed = when (SequenceProcessingDelay.coerce(seconds)) {
            in 1..2 -> Fast
            in 3..4 -> Normal
            else -> Slow
        }
    }
}
