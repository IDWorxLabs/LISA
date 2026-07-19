package com.idworx.lisa.features.intelligentstartup.engine

import com.idworx.lisa.features.intelligentstartup.model.ProfileEyeCalibration
import com.idworx.lisa.features.intelligentstartup.model.QuickCalibrationStep
import kotlin.math.min

data class CalibrationFrameSample(
    val leftOpenness: Float,
    val rightOpenness: Float,
    val faceWidthNormalized: Float,
    val timestampMs: Long
)

/**
 * Collects per-step samples during Quick Eye Calibration and builds a [ProfileEyeCalibration].
 * Duration target for a healthy run is roughly 5–10 seconds of guided interaction.
 */
class QuickEyeCalibrationEngine {

    private val opennessSamples = mutableListOf<Float>()
    private val distanceSamples = mutableListOf<Float>()
    private val blinkClosePeaks = mutableListOf<Float>()
    private val blinkDurationsMs = mutableListOf<Long>()
    private val leftClosePeaks = mutableListOf<Float>()
    private val rightClosePeaks = mutableListOf<Float>()

    private var blinksSeen = 0
    private var leftWinksSeen = 0
    private var rightWinksSeen = 0

    private var blinkCloseStartMs: Long? = null
    private var bothWereOpen = true
    private var awaitingBlinkReopen = false
    private var lastBlinkClosePeak = 1f

    fun reset() {
        opennessSamples.clear()
        distanceSamples.clear()
        blinkClosePeaks.clear()
        blinkDurationsMs.clear()
        leftClosePeaks.clear()
        rightClosePeaks.clear()
        blinksSeen = 0
        leftWinksSeen = 0
        rightWinksSeen = 0
        blinkCloseStartMs = null
        bothWereOpen = true
        awaitingBlinkReopen = false
        lastBlinkClosePeak = 1f
    }

    /**
     * @return true when a both-eye blink cycle was just completed on this frame.
     */
    fun onFrame(step: QuickCalibrationStep, sample: CalibrationFrameSample): Boolean {
        when (step) {
            QuickCalibrationStep.LookNaturally -> {
                val open = (sample.leftOpenness + sample.rightOpenness) / 2f
                opennessSamples += open
                if (sample.faceWidthNormalized > 0f) {
                    distanceSamples += sample.faceWidthNormalized
                }
                return false
            }
            QuickCalibrationStep.BlinkThreeTimes -> return observeBlinkShape(sample)
            QuickCalibrationStep.LeftWinkTwice,
            QuickCalibrationStep.RightWinkTwice,
            QuickCalibrationStep.CalibrationComplete -> return false
        }
    }

    fun onBothBlinkAccepted(step: QuickCalibrationStep, closePeak: Float, durationMs: Long): Int {
        if (step != QuickCalibrationStep.BlinkThreeTimes) return blinksSeen
        blinksSeen += 1
        blinkClosePeaks += closePeak
        blinkDurationsMs += durationMs.coerceIn(40L, 900L)
        return blinksSeen
    }

    fun onLeftWinkAccepted(step: QuickCalibrationStep, closePeak: Float): Int {
        if (step != QuickCalibrationStep.LeftWinkTwice) return leftWinksSeen
        leftWinksSeen += 1
        leftClosePeaks += closePeak
        return leftWinksSeen
    }

    fun onRightWinkAccepted(step: QuickCalibrationStep, closePeak: Float): Int {
        if (step != QuickCalibrationStep.RightWinkTwice) return rightWinksSeen
        rightWinksSeen += 1
        rightClosePeaks += closePeak
        return rightWinksSeen
    }

    fun baselineReady(minSamples: Int = 12): Boolean =
        opennessSamples.size >= minSamples && distanceSamples.size >= minSamples / 2

    fun blinksReady(required: Int = 3): Boolean = blinksSeen >= required
    fun leftWinksReady(required: Int = 2): Boolean = leftWinksSeen >= required
    fun rightWinksReady(required: Int = 2): Boolean = rightWinksSeen >= required

    fun blinkCount(): Int = blinksSeen
    fun leftWinkCount(): Int = leftWinksSeen
    fun rightWinkCount(): Int = rightWinksSeen

    fun buildCalibration(nowMs: Long): ProfileEyeCalibration? {
        if (opennessSamples.isEmpty()) return null
        val baseline = opennessSamples.average().toFloat()
        val distance = if (distanceSamples.isEmpty()) 0.35f else distanceSamples.average().toFloat()
        val openThreshold = (baseline * 0.92f).coerceIn(0.55f, 0.92f)

        val leftClosed = percentileOr(
            leftClosePeaks.ifEmpty { blinkClosePeaks },
            default = baseline * 0.35f
        ).coerceIn(0.08f, 0.50f)
        val rightClosed = percentileOr(
            rightClosePeaks.ifEmpty { blinkClosePeaks },
            default = baseline * 0.35f
        ).coerceIn(0.08f, 0.50f)

        val duration = if (blinkDurationsMs.isEmpty()) 160L else {
            blinkDurationsMs.sorted()[blinkDurationsMs.size / 2]
        }
        val frames = when {
            duration <= 90L -> 1
            duration <= 180L -> 2
            else -> 3
        }

        val sampleCoverage = listOf(
            opennessSamples.size >= 12,
            blinksSeen >= 3,
            leftWinksSeen >= 2,
            rightWinksSeen >= 2
        ).count { it }
        val confidence = (sampleCoverage / 4f).coerceIn(0.55f, 0.98f)

        return ProfileEyeCalibration(
            leftClosedEyeThreshold = min(leftClosed, openThreshold - 0.08f),
            rightClosedEyeThreshold = min(rightClosed, openThreshold - 0.08f),
            openEyeThreshold = openThreshold,
            blinkDurationMs = duration,
            requiredWinkFrames = frames,
            eyeOpennessBaseline = baseline,
            faceDistanceProxy = distance,
            confidence = confidence,
            calibratedAtMs = nowMs
        )
    }

    private fun observeBlinkShape(sample: CalibrationFrameSample): Boolean {
        val bothOpen = sample.leftOpenness > 0.6f && sample.rightOpenness > 0.6f
        val bothClosed = sample.leftOpenness < 0.45f && sample.rightOpenness < 0.45f
        if (bothOpen) {
            if (awaitingBlinkReopen && blinkCloseStartMs != null) {
                val duration = (sample.timestampMs - blinkCloseStartMs!!).coerceIn(40L, 900L)
                blinksSeen += 1
                blinkClosePeaks += lastBlinkClosePeak
                blinkDurationsMs += duration
                awaitingBlinkReopen = false
                blinkCloseStartMs = null
                bothWereOpen = true
                return true
            }
            bothWereOpen = true
            blinkCloseStartMs = null
            return false
        }
        if (bothWereOpen && bothClosed) {
            blinkCloseStartMs = sample.timestampMs
            lastBlinkClosePeak = min(sample.leftOpenness, sample.rightOpenness)
            awaitingBlinkReopen = true
            bothWereOpen = false
        } else if (awaitingBlinkReopen) {
            lastBlinkClosePeak = min(
                lastBlinkClosePeak,
                min(sample.leftOpenness, sample.rightOpenness)
            )
        }
        return false
    }

    private fun percentileOr(values: List<Float>, default: Float): Float {
        if (values.isEmpty()) return default
        val sorted = values.sorted()
        return sorted[sorted.size / 2]
    }
}
