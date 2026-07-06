package com.idworx.lisa.features.blinkdetectionreliability

import com.idworx.lisa.DEFAULT_SENSITIVITY_LEVEL
import com.idworx.lisa.MAX_SENSITIVITY_LEVEL
import com.idworx.lisa.MIN_SENSITIVITY_LEVEL

/**
 * Tunable blink detection parameters — tuned for reliable on-device wink capture
 * without changing sequence finalization (3 s idle) or phrase speech rules.
 */
data class BlinkDetectionTuning(
    val closedEyeThreshold: Float,
    val openEyeThreshold: Float,
    val requiredWinkFrames: Int,
    val openPrimingFrames: Int = OPEN_PRIMING_FRAMES,
    val cooldownMs: Long = WINK_COOLDOWN_MS,
    val streakGraceFrames: Int = STREAK_GRACE_FRAMES,
    val jitterThresholdIdle: Float = EYE_PROB_JUMP_THRESHOLD_IDLE,
    val jitterThresholdActive: Float = EYE_PROB_JUMP_THRESHOLD_ACTIVE
) {
    companion object {
        /** Minimum gap between accepted blinks on the same eye (was 900 ms — too strict for double-blink phrases). */
        const val WINK_COOLDOWN_MS: Long = 520L

        /** Allow one missed frame before resetting a close streak. */
        const val STREAK_GRACE_FRAMES: Int = 1

        /** Eye must be confidently open for this many frames before a close counts. */
        const val OPEN_PRIMING_FRAMES: Int = 2

        /** Reject noisy frames when idle — slightly relaxed from 0.28. */
        const val EYE_PROB_JUMP_THRESHOLD_IDLE: Float = 0.34f

        /** More tolerant during an in-progress blink sequence or active candidate streak. */
        const val EYE_PROB_JUMP_THRESHOLD_ACTIVE: Float = 0.52f

        fun forSensitivityLevel(level: Int): BlinkDetectionTuning {
            val clamped = level.coerceIn(MIN_SENSITIVITY_LEVEL, MAX_SENSITIVITY_LEVEL)
            val span = (MAX_SENSITIVITY_LEVEL - MIN_SENSITIVITY_LEVEL).coerceAtLeast(1)
            val t = (clamped - MIN_SENSITIVITY_LEVEL).toFloat() / span.toFloat()
            fun lerp(start: Float, end: Float, fraction: Float): Float = start + (end - start) * fraction
            return BlinkDetectionTuning(
                closedEyeThreshold = lerp(0.18f, 0.48f, t),
                openEyeThreshold = lerp(0.82f, 0.57f, t),
                requiredWinkFrames = lerp(3f, 1f, t).toInt().coerceAtLeast(1),
                openPrimingFrames = lerp(3f, 1f, t).toInt().coerceAtLeast(1)
            )
        }

        val default: BlinkDetectionTuning = forSensitivityLevel(DEFAULT_SENSITIVITY_LEVEL)
    }

    fun isLeftWinkCandidate(leftProb: Float, rightProb: Float): Boolean =
        leftProb < closedEyeThreshold && rightProb > openEyeThreshold

    fun isRightWinkCandidate(leftProb: Float, rightProb: Float): Boolean =
        rightProb < closedEyeThreshold && leftProb > openEyeThreshold

    fun isEyeUncertain(prob: Float): Boolean = prob in closedEyeThreshold..openEyeThreshold
}
