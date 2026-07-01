package com.idworx.lisa

/** Fallback when profile response speed is unavailable (matches Normal). */
const val SEQUENCE_IDLE_TIMEOUT_MS = 5000L

enum class ResponseSpeed(val idleTimeoutMs: Long) {
    Fast(3000L),
    Normal(5000L),
    Slow(7000L);

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

    /** Generous cap — idle timer is based on last wink; this is an absolute safety limit only. */
    fun maxSequenceWindowMs(): Long = maxOf(MIN_SEQUENCE_WINDOW_MS, idleTimeoutMs * 4)

    companion object {
        val default: ResponseSpeed = Normal

        fun fromStored(stored: String?, legacyTimeoutSec: Float = Normal.idleTimeoutMs / 1000f): ResponseSpeed {
            entries.find { it.name.equals(stored, ignoreCase = true) }?.let { return it }
            return when {
                legacyTimeoutSec <= 3.5f -> Fast
                legacyTimeoutSec <= 6.0f -> Normal
                else -> Slow
            }
        }
    }
}
