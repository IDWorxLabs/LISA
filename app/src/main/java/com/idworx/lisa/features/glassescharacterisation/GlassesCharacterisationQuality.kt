package com.idworx.lisa.features.glassescharacterisation

/**
 * Deterministic diagnostic quality classification.
 * Does not activate any profile or change production behaviour.
 */
object GlassesCharacterisationQuality {
    const val MIN_OPEN_SAMPLES: Int = 20
    const val MIN_CLOSED_SAMPLES: Int = 8
    const val STRONG_SEP: Float = 0.18f
    const val MODERATE_SEP: Float = 0.10f
    const val STRONG_OVERLAP_MAX: Float = 35f
    const val MODERATE_OVERLAP_MAX: Float = 60f
    const val MAX_NULL_STRONG: Float = 8f
    const val MAX_NULL_MODERATE: Float = 18f
    const val MAX_REJECT_STRONG: Float = 15f
    const val MAX_REJECT_MODERATE: Float = 30f

    fun classify(left: EyeSeparationMetrics, right: EyeSeparationMetrics): SignalQualityClass {
        val l = eyeClass(left)
        val r = eyeClass(right)
        return when {
            l == SignalQualityClass.Unusable && r == SignalQualityClass.Unusable ->
                SignalQualityClass.Unusable
            l == SignalQualityClass.Strong && r == SignalQualityClass.Strong ->
                SignalQualityClass.Strong
            l == SignalQualityClass.Unusable || r == SignalQualityClass.Unusable ->
                SignalQualityClass.Weak
            l == SignalQualityClass.Weak || r == SignalQualityClass.Weak ->
                SignalQualityClass.Weak
            l == SignalQualityClass.Moderate || r == SignalQualityClass.Moderate ->
                SignalQualityClass.Moderate
            else -> SignalQualityClass.Moderate
        }
    }

    fun eyeClass(m: EyeSeparationMetrics): SignalQualityClass {
        if (m.open.count < MIN_OPEN_SAMPLES || m.closed.count < MIN_CLOSED_SAMPLES) {
            return SignalQualityClass.Unusable
        }
        val sep = m.openP25MinusClosedP75
        if (sep == null) return SignalQualityClass.Unusable
        if (sep < 0.04f) return SignalQualityClass.Unusable
        if (m.nullPercent > 35f || m.rejectedPercent > 45f) return SignalQualityClass.Unusable

        val strong = sep >= STRONG_SEP &&
            m.overlapPercent <= STRONG_OVERLAP_MAX &&
            m.nullPercent <= MAX_NULL_STRONG &&
            m.rejectedPercent <= MAX_REJECT_STRONG &&
            m.closedBelowClosedThrPct >= 50f

        if (strong) return SignalQualityClass.Strong

        val moderate = sep >= MODERATE_SEP &&
            m.overlapPercent <= MODERATE_OVERLAP_MAX &&
            m.nullPercent <= MAX_NULL_MODERATE &&
            m.rejectedPercent <= MAX_REJECT_MODERATE

        return if (moderate) SignalQualityClass.Moderate else SignalQualityClass.Weak
    }

    fun usableSampleCount(left: EyeSeparationMetrics, right: EyeSeparationMetrics): Int =
        left.open.count + left.closed.count + right.open.count + right.closed.count
}
