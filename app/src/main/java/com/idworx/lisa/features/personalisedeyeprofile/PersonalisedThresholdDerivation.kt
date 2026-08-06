package com.idworx.lisa.features.personalisedeyeprofile

import kotlin.math.sqrt

/**
 * Pure threshold derivation for Personalised Eye Profile.
 *
 * Formula (robust separation margins):
 * 1. From open samples: median, P25, P75
 * 2. From closed samples: median, P25, P75, min
 * 3. separation = openP25 − closedP75  (gap between upper closed and lower open mass)
 * 4. Require separation ≥ [MIN_SEPARATION] else fail (distributions overlap)
 * 5. closedThreshold = closedP75 + [CLOSED_MARGIN_FRAC] × separation
 * 6. openThreshold   = openP25 − [OPEN_MARGIN_FRAC] × separation
 * 7. Require openThreshold − closedThreshold ≥ [MIN_BAND_WIDTH]
 * 8. Clamp to [CLOSED_CLAMP] / [OPEN_CLAMP]
 * 9. Reject if > [MAX_MISCLASSIFY] of closed samples stay above closedThreshold
 *    or > [MAX_MISCLASSIFY] of open samples fall below openThreshold
 *
 * Justified by Eye Test evidence: with glasses, closed values often sit above 0.50
 * while open remains high — a fixed universal threshold fails; per-eye distribution
 * separation must be measured.
 */
object PersonalisedThresholdDerivation {
    const val MIN_SEPARATION: Float = 0.10f
    const val MIN_BAND_WIDTH: Float = 0.08f
    const val CLOSED_MARGIN_FRAC: Float = 0.25f
    const val OPEN_MARGIN_FRAC: Float = 0.20f
    const val MAX_MISCLASSIFY: Float = 0.20f
    const val MIN_OPEN_SAMPLES: Int = 20
    const val MIN_CLOSED_SAMPLES: Int = 10
    val CLOSED_CLAMP: ClosedRange<Float> = 0.15f..0.78f
    val OPEN_CLAMP: ClosedRange<Float> = 0.40f..0.95f

    data class EyeDerivationInput(
        val openSamples: List<Float>,
        val closedSamples: List<Float>,
        val reopenSamples: List<Float> = emptyList()
    )

    data class EyeDerivationResult(
        val ok: Boolean,
        val failureReasons: List<String> = emptyList(),
        val openBaseline: Float = 0f,
        val closedBaseline: Float = 0f,
        val closedMinimum: Float = 0f,
        val reopenMaximum: Float = 0f,
        val closedThreshold: Float = 0f,
        val openThreshold: Float = 0f,
        val uncertaintyLower: Float = 0f,
        val uncertaintyUpper: Float = 0f,
        val separation: Float = 0f,
        val openStats: PersonalisedEyeStats = PersonalisedEyeStats(),
        val closedStats: PersonalisedEyeStats = PersonalisedEyeStats(),
        val notes: String = ""
    )

    fun deriveEye(input: EyeDerivationInput): EyeDerivationResult {
        val open = input.openSamples.filter { it in 0f..1f }
        val closed = input.closedSamples.filter { it in 0f..1f }
        val reopen = input.reopenSamples.filter { it in 0f..1f }
        val failures = mutableListOf<String>()
        if (open.size < MIN_OPEN_SAMPLES) {
            failures += "Too few open-eye samples (${open.size} < $MIN_OPEN_SAMPLES)."
        }
        if (closed.size < MIN_CLOSED_SAMPLES) {
            failures += "Too few closed-eye samples (${closed.size} < $MIN_CLOSED_SAMPLES)."
        }
        if (failures.isNotEmpty()) {
            return EyeDerivationResult(ok = false, failureReasons = failures)
        }
        val openStats = stats(open)
        val closedStats = stats(closed)
        val openP25 = openStats.p25!!
        val closedP75 = closedStats.p75!!
        val separation = openP25 - closedP75
        if (separation < MIN_SEPARATION) {
            return EyeDerivationResult(
                ok = false,
                failureReasons = listOf(
                    "Open and closed signals overlap too much " +
                        "(separation=${"%.3f".format(separation)} < $MIN_SEPARATION)."
                ),
                openStats = openStats,
                closedStats = closedStats,
                separation = separation
            )
        }
        var closedThr = closedP75 + CLOSED_MARGIN_FRAC * separation
        var openThr = openP25 - OPEN_MARGIN_FRAC * separation
        closedThr = closedThr.coerceIn(CLOSED_CLAMP.start, CLOSED_CLAMP.endInclusive)
        openThr = openThr.coerceIn(OPEN_CLAMP.start, OPEN_CLAMP.endInclusive)
        if (openThr - closedThr < MIN_BAND_WIDTH) {
            return EyeDerivationResult(
                ok = false,
                failureReasons = listOf(
                    "Threshold band too narrow after margins " +
                        "(${"%.3f".format(openThr - closedThr)} < $MIN_BAND_WIDTH)."
                ),
                openStats = openStats,
                closedStats = closedStats,
                separation = separation
            )
        }
        if (closedThr >= openThr) {
            return EyeDerivationResult(
                ok = false,
                failureReasons = listOf("Invalid threshold ordering (closed ≥ open)."),
                separation = separation
            )
        }
        val closedMis = closed.count { it >= closedThr }.toFloat() / closed.size
        val openMis = open.count { it <= openThr }.toFloat() / open.size
        if (closedMis > MAX_MISCLASSIFY) {
            failures += "Too many closed samples remain above closed threshold " +
                "(${"%.0f".format(closedMis * 100)}%)."
        }
        if (openMis > MAX_MISCLASSIFY) {
            failures += "Too many open samples fall at/below open threshold " +
                "(${"%.0f".format(openMis * 100)}%)."
        }
        if (failures.isNotEmpty()) {
            return EyeDerivationResult(
                ok = false,
                failureReasons = failures,
                openStats = openStats,
                closedStats = closedStats,
                separation = separation,
                closedThreshold = closedThr,
                openThreshold = openThr
            )
        }
        val notes =
            "closedThr=closedP75+${CLOSED_MARGIN_FRAC}*sep; " +
                "openThr=openP25-${OPEN_MARGIN_FRAC}*sep; sep=openP25-closedP75"
        return EyeDerivationResult(
            ok = true,
            openBaseline = openStats.median ?: openStats.average ?: 0f,
            closedBaseline = closedStats.median ?: closedStats.average ?: 0f,
            closedMinimum = closedStats.min ?: 0f,
            reopenMaximum = reopen.maxOrNull() ?: openStats.max ?: 0f,
            closedThreshold = closedThr,
            openThreshold = openThr,
            uncertaintyLower = closedThr,
            uncertaintyUpper = openThr,
            separation = separation,
            openStats = openStats,
            closedStats = closedStats,
            notes = notes
        )
    }

    fun stats(values: List<Float>): PersonalisedEyeStats {
        if (values.isEmpty()) return PersonalisedEyeStats()
        val sorted = values.sorted()
        val mean = values.average().toFloat()
        val variance = if (values.size < 2) {
            0.0
        } else {
            values.sumOf { (it - mean).toDouble() * (it - mean) } / values.size
        }
        return PersonalisedEyeStats(
            min = sorted.first(),
            max = sorted.last(),
            average = mean,
            median = percentile(sorted, 0.50f),
            stdDev = sqrt(variance).toFloat(),
            p25 = percentile(sorted, 0.25f),
            p75 = percentile(sorted, 0.75f),
            sampleCount = values.size
        )
    }

    fun percentile(sorted: List<Float>, p: Float): Float {
        if (sorted.isEmpty()) return 0f
        if (sorted.size == 1) return sorted[0]
        val idx = (p * (sorted.size - 1)).coerceIn(0f, (sorted.size - 1).toFloat())
        val lo = idx.toInt()
        val hi = (lo + 1).coerceAtMost(sorted.lastIndex)
        val frac = idx - lo
        return sorted[lo] * (1f - frac) + sorted[hi] * frac
    }
}
