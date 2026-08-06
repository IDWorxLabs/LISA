package com.idworx.lisa.features.glassescharacterisation

import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Pure metrics for Glasses Characterisation — observational only.
 * Does not alter Standard Mode thresholds.
 */
object GlassesCharacterisationMetrics {
    const val FACE_STABLE_MS: Long = 3_000L
    const val PREPARE_MS: Long = 4_000L
    const val OPEN_RECORD_MS: Long = 10_000L
    const val CLOSE_RECORD_MS: Long = 2_000L
    const val COMPLETE_PAUSE_MS: Long = 1_000L
    const val RECOVERY_MIN_MS: Long = 3_000L
    const val BLINK_OBSERVE_MS: Long = 20_000L
    const val CLOSE_CYCLES: Int = 5
    const val VOICE_COOLDOWN_MS: Long = 3_500L
    /** Brief re-check against session baseline before each subsequent lighting. */
    const val BASELINE_REVALIDATE_MS: Long = 1_500L
    const val ANALYSE_STEP_MS: Long = 900L

    /** Position consistency tolerances (natural pose — not precise targets). */
    const val MAX_FACE_CENTER_DELTA_PCT: Float = 12f
    const val MAX_FACE_WIDTH_DELTA_PCT: Float = 8f
    const val MAX_YAW_DELTA_DEG: Float = 12f
    const val MAX_ROLL_DELTA_DEG: Float = 12f

    const val OPEN_HINT: Float = 0.65f
    const val CLOSED_HINT: Float = 0.45f

    fun percentile(sorted: List<Float>, p: Float): Float? {
        if (sorted.isEmpty()) return null
        if (sorted.size == 1) return sorted[0]
        val idx = (p * (sorted.size - 1)).coerceIn(0f, (sorted.size - 1).toFloat())
        val lo = idx.toInt()
        val hi = (lo + 1).coerceAtMost(sorted.lastIndex)
        val frac = idx - lo
        return sorted[lo] * (1f - frac) + sorted[hi] * frac
    }

    fun stdDev(values: List<Float>): Float? {
        if (values.isEmpty()) return null
        if (values.size == 1) return 0f
        val mean = values.average()
        val variance = values.sumOf { (it - mean) * (it - mean) } / values.size
        return sqrt(variance).toFloat()
    }

    fun summarize(values: List<Float>): DistributionSummary {
        if (values.isEmpty()) return DistributionSummary()
        val s = values.sorted()
        return DistributionSummary(
            count = s.size,
            average = s.average().toFloat(),
            median = percentile(s, 0.50f),
            stdDev = stdDev(s),
            min = s.first(),
            max = s.last(),
            p10 = percentile(s, 0.10f),
            p25 = percentile(s, 0.25f),
            p50 = percentile(s, 0.50f),
            p75 = percentile(s, 0.75f),
            p90 = percentile(s, 0.90f)
        )
    }

    /**
     * Overlap % of the closed range that intersects the open range
     * (using P10..P90 windows when available).
     */
    fun overlapPercent(open: DistributionSummary, closed: DistributionSummary): Float {
        val oLo = open.p10 ?: open.min ?: return 100f
        val oHi = open.p90 ?: open.max ?: return 100f
        val cLo = closed.p10 ?: closed.min ?: return 100f
        val cHi = closed.p90 ?: closed.max ?: return 100f
        if (oHi <= oLo || cHi <= cLo) return 100f
        val interLo = maxOf(oLo, cLo)
        val interHi = minOf(oHi, cHi)
        if (interHi <= interLo) return 0f
        val closedSpan = (cHi - cLo).coerceAtLeast(1e-6f)
        return ((interHi - interLo) / closedSpan * 100f).coerceIn(0f, 100f)
    }

    fun proportion(values: List<Float>, predicate: (Float) -> Boolean): Float {
        if (values.isEmpty()) return 0f
        return values.count(predicate) * 100f / values.size
    }

    fun eyeMetrics(
        openSamples: List<Float>,
        closedSamples: List<Float>,
        nullCount: Int,
        framesSeen: Int,
        rejectedCount: Int,
        closedThr: Float,
        openThr: Float,
        isUncertain: (Float) -> Boolean
    ): EyeSeparationMetrics {
        val open = summarize(openSamples)
        val closed = summarize(closedSamples)
        val sepP = if (open.p25 != null && closed.p75 != null) open.p25 - closed.p75 else null
        val sepM = if (open.median != null && closed.median != null) {
            open.median - closed.median
        } else {
            null
        }
        val nullPct = if (framesSeen <= 0) 0f else nullCount * 100f / framesSeen
        val rejPct = if (framesSeen <= 0) 0f else rejectedCount * 100f / framesSeen
        return EyeSeparationMetrics(
            open = open,
            closed = closed,
            openP25MinusClosedP75 = sepP,
            openMedianMinusClosedMedian = sepM,
            overlapPercent = overlapPercent(open, closed),
            closedBelowClosedThrPct = proportion(closedSamples) { it < closedThr },
            closedInUncertainPct = proportion(closedSamples) { isUncertain(it) },
            closedAboveOpenThrPct = proportion(closedSamples) { it > openThr },
            openAboveOpenThrPct = proportion(openSamples) { it > openThr },
            openInUncertainPct = proportion(openSamples) { isUncertain(it) },
            nullPercent = nullPct,
            rejectedPercent = rejPct,
            jitter = open.stdDev
        )
    }

    fun poseDriftExcessive(baseline: PoseSnapshot, current: PoseSnapshot): Boolean {
        fun delta(a: Float?, b: Float?): Float? =
            if (a == null || b == null) null else abs(a - b)
        val cx = delta(baseline.faceCenterXPct, current.faceCenterXPct)
        val cy = delta(baseline.faceCenterYPct, current.faceCenterYPct)
        val w = delta(baseline.faceWidthPct, current.faceWidthPct)
        val yaw = delta(baseline.yaw, current.yaw)
        val roll = delta(baseline.roll, current.roll)
        if (cx != null && cx > MAX_FACE_CENTER_DELTA_PCT) return true
        if (cy != null && cy > MAX_FACE_CENTER_DELTA_PCT) return true
        if (w != null && w > MAX_FACE_WIDTH_DELTA_PCT) return true
        if (yaw != null && yaw > MAX_YAW_DELTA_DEG) return true
        if (roll != null && roll > MAX_ROLL_DELTA_DEG) return true
        return false
    }

    fun bothEyesOpen(left: Float?, right: Float?): Boolean =
        left != null && right != null && left >= OPEN_HINT && right >= OPEN_HINT

    fun leftClosedOnly(left: Float?, right: Float?): Boolean =
        left != null && right != null && left <= CLOSED_HINT && right >= OPEN_HINT

    fun rightClosedOnly(left: Float?, right: Float?): Boolean =
        left != null && right != null && right <= CLOSED_HINT && left >= OPEN_HINT
}
