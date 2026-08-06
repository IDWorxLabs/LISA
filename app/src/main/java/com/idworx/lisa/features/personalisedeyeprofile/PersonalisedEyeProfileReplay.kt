package com.idworx.lisa.features.personalisedeyeprofile

import com.idworx.lisa.features.blinkdetectionreliability.BlinkDetectionProcessor
import com.idworx.lisa.features.blinkdetectionreliability.BlinkDetectionTuning
import com.idworx.lisa.features.blinkdetectionreliability.BlinkEyeProbabilities
import com.idworx.lisa.features.eyediagnostic.LisaEyeDiagnostic

/**
 * Offline replay of recorded Eye Test samples against Standard vs Personalised thresholds.
 * Deterministic — no images required.
 */
object PersonalisedEyeProfileReplay {
    data class ReplayResult(
        val label: String,
        val leftWinks: Int,
        val rightWinks: Int,
        val falsePositivesDuringOpen: Int,
        val missedLeftTarget: Int,
        val missedRightTarget: Int,
        val candidateStarts: Int,
        val candidateCancelsApprox: Int,
        val reopenFailuresApprox: Int,
        val uncertainFrames: Int,
        val totalFrames: Int,
        val uncertainOccupancyPercent: Float,
        val l1r1Likely: Boolean,
        val l2r2Likely: Boolean
    )

    fun replay(
        label: String,
        samples: List<LisaEyeDiagnostic.Sample>,
        tuning: BlinkDetectionTuning,
        targetLeft: Int = 5,
        targetRight: Int = 5
    ): ReplayResult {
        val processor = BlinkDetectionProcessor(tuning)
        var left = 0
        var right = 0
        var falseOpen = 0
        var candidates = 0
        var prevLeftCand = false
        var prevRightCand = false
        var cancels = 0
        var reopenFails = 0
        var uncertain = 0
        var inSteadyOpen = true
        samples.forEachIndexed { index, s ->
            val lp = s.leftEyeOpenProbability
            val rp = s.rightEyeOpenProbability
            if (lp == null || rp == null) return@forEachIndexed
            // Heuristic: first/last 10% treated as steady-open windows for false-positive count.
            val frac = index.toFloat() / samples.size.coerceAtLeast(1)
            inSteadyOpen = frac < 0.12f || frac > 0.88f
            val result = processor.processFrame(
                BlinkEyeProbabilities(lp, rp),
                s.timestampMs,
                acceptedLeftCount = left,
                acceptedRightCount = right
            )
            if (result.leftCandidate && !prevLeftCand) candidates++
            if (result.rightCandidate && !prevRightCand) candidates++
            if (!result.leftCandidate && prevLeftCand && !result.acceptLeft) cancels++
            if (!result.rightCandidate && prevRightCand && !result.acceptRight) cancels++
            if (result.rejectedIncompleteShape) reopenFails++
            prevLeftCand = result.leftCandidate
            prevRightCand = result.rightCandidate
            if (tuning.isEyeUncertain(lp) || tuning.isEyeUncertain(rp)) uncertain++
            if (result.acceptLeft) {
                left++
                if (inSteadyOpen) falseOpen++
            }
            if (result.acceptRight) {
                right++
                if (inSteadyOpen) falseOpen++
            }
        }
        val n = samples.size.coerceAtLeast(1)
        return ReplayResult(
            label = label,
            leftWinks = left,
            rightWinks = right,
            falsePositivesDuringOpen = falseOpen,
            missedLeftTarget = (targetLeft - left).coerceAtLeast(0),
            missedRightTarget = (targetRight - right).coerceAtLeast(0),
            candidateStarts = candidates,
            candidateCancelsApprox = cancels,
            reopenFailuresApprox = reopenFails,
            uncertainFrames = uncertain,
            totalFrames = samples.size,
            uncertainOccupancyPercent = uncertain * 100f / n,
            l1r1Likely = left >= 1 && right >= 1,
            l2r2Likely = left >= 2 && right >= 2
        )
    }

    fun compareReport(standard: ReplayResult, personalised: ReplayResult): String = buildString {
        appendLine("=== Replay comparison ===")
        appendLine("Standard: L${standard.leftWinks} R${standard.rightWinks} " +
            "FP=${standard.falsePositivesDuringOpen} unc=${"%.1f".format(standard.uncertainOccupancyPercent)}%")
        appendLine("Personalised: L${personalised.leftWinks} R${personalised.rightWinks} " +
            "FP=${personalised.falsePositivesDuringOpen} unc=${"%.1f".format(personalised.uncertainOccupancyPercent)}%")
        appendLine("Missed L/R Standard: ${standard.missedLeftTarget}/${standard.missedRightTarget}")
        appendLine("Missed L/R Personalised: ${personalised.missedLeftTarget}/${personalised.missedRightTarget}")
    }
}
