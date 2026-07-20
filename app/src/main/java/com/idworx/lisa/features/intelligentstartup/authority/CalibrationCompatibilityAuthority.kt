package com.idworx.lisa.features.intelligentstartup.authority

import com.idworx.lisa.features.intelligentstartup.model.CalibrationCompatibilityLevel
import com.idworx.lisa.features.intelligentstartup.model.CalibrationCompatibilityRecord
import com.idworx.lisa.features.intelligentstartup.model.LiveCompatibilitySample
import com.idworx.lisa.features.intelligentstartup.model.ProfileEyeCalibration
import kotlin.math.abs

/**
 * Determines whether the current eyes appear compatible with a saved calibration.
 * Not facial recognition — compares live openness / distance / blink traits to stored values.
 */
object CalibrationCompatibilityAuthority {

    const val HighScoreMinimum = 0.78f
    const val MediumScoreMinimum = 0.55f
    const val MaxFreshAgeMs: Long = EyeCalibrationAuthority.MaxCalibrationAgeMs

    fun evaluate(
        stored: ProfileEyeCalibration?,
        live: LiveCompatibilitySample?,
        nowMs: Long
    ): Pair<CalibrationCompatibilityLevel, Float> {
        if (stored == null || !EyeCalibrationAuthority.thresholdsLookValid(stored)) {
            return CalibrationCompatibilityLevel.Low to 0f
        }
        if (nowMs - stored.calibratedAtMs > MaxFreshAgeMs) {
            return CalibrationCompatibilityLevel.Low to 0.2f
        }
        if (stored.confidence < EyeCalibrationAuthority.HighConfidenceMinimum * 0.85f) {
            // Weak stored confidence cannot be HIGH even if live matches.
            val score = if (live == null) {
                stored.confidence * 0.85f
            } else {
                score(stored, live) * 0.85f
            }
            return levelFor(score.coerceAtMost(MediumScoreMinimum + 0.01f)) to score
        }
        if (live == null) {
            // No live sample yet — fall back to stored confidence age gate only.
            return if (stored.confidence >= EyeCalibrationAuthority.HighConfidenceMinimum) {
                CalibrationCompatibilityLevel.High to stored.confidence
            } else {
                CalibrationCompatibilityLevel.Medium to stored.confidence
            }
        }
        val score = score(stored, live)
        return levelFor(score) to score
    }

    fun shouldSkipQuickCalibration(level: CalibrationCompatibilityLevel): Boolean =
        level == CalibrationCompatibilityLevel.High

    fun requiresQuickCalibration(level: CalibrationCompatibilityLevel): Boolean =
        level == CalibrationCompatibilityLevel.Low || level == CalibrationCompatibilityLevel.Medium

    fun appendHistory(
        calibration: ProfileEyeCalibration,
        level: CalibrationCompatibilityLevel,
        score: Float,
        nowMs: Long,
        maxEntries: Int = 8
    ): ProfileEyeCalibration {
        val entry = CalibrationCompatibilityRecord(level, score, nowMs)
        val history = (calibration.compatibilityHistory + entry).takeLast(maxEntries)
        return calibration.copy(compatibilityHistory = history)
    }

    private fun levelFor(score: Float): CalibrationCompatibilityLevel = when {
        score >= HighScoreMinimum -> CalibrationCompatibilityLevel.High
        score >= MediumScoreMinimum -> CalibrationCompatibilityLevel.Medium
        else -> CalibrationCompatibilityLevel.Low
    }

    private fun score(stored: ProfileEyeCalibration, live: LiveCompatibilitySample): Float {
        val openness = similarity(stored.eyeOpennessBaseline, live.eyeOpennessBaseline, tolerance = 0.22f)
        val distance = similarity(stored.faceDistanceProxy, live.faceDistanceProxy, tolerance = 0.28f)
        val spacing = similarity(stored.eyeSpacingProxy, live.eyeSpacingProxy, tolerance = 0.30f)
        val left = live.leftCloseCharacteristic?.let {
            similarity(stored.leftClosedEyeThreshold, it, tolerance = 0.20f)
        } ?: 0.7f
        val right = live.rightCloseCharacteristic?.let {
            similarity(stored.rightClosedEyeThreshold, it, tolerance = 0.20f)
        } ?: 0.7f
        val duration = live.blinkDurationMs?.let {
            similarity(
                stored.blinkDurationMs.toFloat(),
                it.toFloat(),
                tolerance = 220f
            )
        } ?: 0.7f
        return (
            openness * 0.28f +
                distance * 0.18f +
                spacing * 0.14f +
                left * 0.14f +
                right * 0.14f +
                duration * 0.12f
            ).coerceIn(0f, 1f)
    }

    private fun similarity(expected: Float, actual: Float, tolerance: Float): Float {
        val delta = abs(expected - actual)
        return (1f - delta / tolerance).coerceIn(0f, 1f)
    }
}
