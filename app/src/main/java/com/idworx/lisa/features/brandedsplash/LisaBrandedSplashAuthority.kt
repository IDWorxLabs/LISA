package com.idworx.lisa.features.brandedsplash

/**
 * RC8.41 — presentation-only authority for the branded Compose splash (Stage 2).
 *
 * Does not alter Intelligent Startup reducers, calibration, or destination selection.
 * Dismissal waits until Compose content is ready and the minimum brand hold has elapsed.
 */
object LisaBrandedSplashAuthority {

    /** Minimum time the branded splash remains visible after first Compose frame. */
    const val MIN_VISIBLE_MS: Long = 1_400L

    /** Safety ceiling so splash cannot block startup indefinitely. */
    const val MAX_VISIBLE_MS: Long = 2_800L

    const val COMMUNICATOR: String = "Communicator"
    const val SLOGAN_LINE_1: String = "I can't speak."
    const val SLOGAN_LISA: String = "LISA"
    const val SLOGAN_LINE_2_REST: String = " speaks for me."

    /**
     * @param elapsedMs time since branded splash became visible
     * @param composeContentReady true once Stage 2 has drawn its first frame
     */
    fun shouldKeepShowing(elapsedMs: Long, composeContentReady: Boolean): Boolean {
        if (elapsedMs >= MAX_VISIBLE_MS) return false
        if (!composeContentReady) return true
        return elapsedMs < MIN_VISIBLE_MS
    }

    fun logoWidthFraction(shortestSideDp: Float): Float = when {
        shortestSideDp < 360f -> 0.52f
        shortestSideDp < 600f -> 0.46f
        else -> 0.34f
    }

    fun waveHeightFraction(shortestSideDp: Float): Float = when {
        shortestSideDp < 360f -> 0.28f
        shortestSideDp < 600f -> 0.30f
        else -> 0.26f
    }
}
