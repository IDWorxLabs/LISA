package com.idworx.lisa

/**
 * Milliseconds to wait after the last confirmed wink before finalizing a sequence, before the
 * active profile's own response time has loaded. Kept equal to [SequenceProcessingDelay.DEFAULT_SECONDS]
 * so there is a single source of truth for "default response time" — see [applySequenceProcessingDelay]
 * usages in MainActivity, which overwrite this with the profile's real value immediately on launch.
 */
const val SEQUENCE_IDLE_TIMEOUT_MS = 5000L

/** User-selectable delay (seconds) after the last blink before a sequence is processed. */
object SequenceProcessingDelay {
    const val MIN_SECONDS = 3
    const val MAX_SECONDS = 8
    const val DEFAULT_SECONDS = 5

    /**
     * Guided Mode/Training's default settle time — same default as the everyday Communication
     * Workspace ([DEFAULT_SECONDS]) so a multi-step lesson gesture (e.g. Categories) has time to
     * finish before the sequence is evaluated. Sourced from [TrainingPreferences], never
     * hardcoded per lesson.
     */
    const val GUIDED_DEFAULT_SECONDS = DEFAULT_SECONDS

    /**
     * How many multiples of the idle timeout the *total* sequence is allowed to run for before a
     * hard safety cutoff kicks in — deliberately generous (comfortably more than
     * [com.idworx.lisa.EMERGENCY_LEFT_WINKS] gaps at the slowest allowed response time) so a long,
     * fully visible gesture like 6 Left Winks is never cut off mid-sequence just because the user
     * paused close to the per-wink allowance between blinks. This is a runaway-input safety net,
     * not a practical limiter: real gestures finish either by going idle for [toMillis] or by
     * resolving to an unambiguous match well before this is ever reached.
     */
    private const val MAX_WINDOW_MULTIPLIER = 8

    val allowedSeconds: IntRange = MIN_SECONDS..MAX_SECONDS

    fun coerce(seconds: Int): Int = seconds.coerceIn(MIN_SECONDS, MAX_SECONDS)

    fun toMillis(seconds: Int): Long = coerce(seconds) * 1000L

    /**
     * Total-sequence-age safety cutoff — see [MAX_WINDOW_MULTIPLIER]. Always derived from the
     * exact selected [seconds], never a coarse 3-tier bucket, so it scales correctly across the
     * whole [allowedSeconds] range instead of under-allowing at the slower end.
     */
    fun maxWindowMs(seconds: Int): Long =
        maxOf(MIN_SEQUENCE_WINDOW_MS, toMillis(seconds) * MAX_WINDOW_MULTIPLIER)

    fun fromStored(value: Int, legacySpeed: ResponseSpeed? = null): Int {
        if (value in allowedSeconds) return value
        return when (legacySpeed) {
            ResponseSpeed.Fast -> MIN_SECONDS
            ResponseSpeed.Normal -> DEFAULT_SECONDS
            ResponseSpeed.Slow -> MAX_SECONDS
            null -> DEFAULT_SECONDS
        }
    }
}

/**
 * Legacy 3-tier Fast/Normal/Slow picker (Settings > Communication Setup) — kept in sync with
 * [SequenceProcessingDelay]'s min/default/max so it always spans the same real range the numeric
 * response-time +/- controls do, instead of drifting into its own separate scale.
 */
enum class ResponseSpeed(val idleTimeoutMs: Long) {
    Fast(SequenceProcessingDelay.MIN_SECONDS * 1000L),
    Normal(SequenceProcessingDelay.DEFAULT_SECONDS * 1000L),
    Slow(SequenceProcessingDelay.MAX_SECONDS * 1000L);

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
            SequenceProcessingDelay.MIN_SECONDS -> Fast
            in (SequenceProcessingDelay.MIN_SECONDS + 1) until SequenceProcessingDelay.MAX_SECONDS -> Normal
            else -> Slow
        }
    }
}
