package com.idworx.lisa

import kotlin.math.roundToInt

/**
 * RC7D.26 — pure calculation authority for the segmented Adjust Settings level meter.
 *
 * The meter is a visual representation only. It never redefines Sensitivity or Response Time
 * limits or step sizes; callers pass the existing domain min/max/current values.
 */
object SettingAdjustmentMeterAuthority {
    /** Shared segment count for Sensitivity and Response Time — readable on a phone. */
    const val SEGMENT_COUNT: Int = 8

    /**
     * How many of [segmentCount] segments should appear active for [value].
     *
     * - [minimum] activates the lowest valid visual level (1 segment when [segmentCount] > 0)
     * - [maximum] activates every segment
     * - intermediate values map deterministically between those endpoints
     * - results are always clamped; invalid ranges and zero segment counts never crash
     */
    fun activeSegmentCount(
        value: Int,
        minimum: Int,
        maximum: Int,
        segmentCount: Int = SEGMENT_COUNT
    ): Int {
        if (segmentCount <= 0) return 0
        if (maximum <= minimum) return 1.coerceAtMost(segmentCount)
        val clamped = value.coerceIn(minimum, maximum)
        val range = (maximum - minimum).toFloat()
        val steps = (clamped - minimum).toFloat()
        val active = 1 + ((steps / range) * (segmentCount - 1)).roundToInt()
        return active.coerceIn(1, segmentCount)
    }
}
