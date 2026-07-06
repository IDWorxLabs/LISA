package com.idworx.lisa.features.calibrationreliability.recovery

import com.idworx.lisa.features.calibrationreliability.model.CalibrationFailureReason
import com.idworx.lisa.features.calibrationreliability.model.CalibrationRecommendation
import com.idworx.lisa.features.calibrationreliability.model.CalibrationSession
import com.idworx.lisa.features.calibrationreliability.model.CalibrationSessionState

object CalibrationRetryPolicy {

    private const val MAX_RETRIES = 5
    private const val BACKOFF_MS = 30_000L

    fun canRetry(session: CalibrationSession, totalRetries: Int): RetryDecision {
        if (totalRetries >= MAX_RETRIES) {
            return RetryDecision(allowed = false, reason = "Maximum calibration retries reached")
        }
        if (session.state == CalibrationSessionState.Abandoned) {
            return RetryDecision(allowed = false, reason = "Session was abandoned")
        }
        val elapsed = System.currentTimeMillis() - session.startTimeMs
        if (session.retries > 0 && elapsed < BACKOFF_MS) {
            return RetryDecision(allowed = false, reason = "Retry backoff period active", waitMs = BACKOFF_MS - elapsed)
        }
        return RetryDecision(allowed = true)
    }
}

data class RetryDecision(
    val allowed: Boolean,
    val reason: String? = null,
    val waitMs: Long = 0L
)

object CalibrationResumeManager {

    fun canResume(session: CalibrationSession): ResumeDecision {
        if (session.state != CalibrationSessionState.Paused &&
            session.state != CalibrationSessionState.Interrupted
        ) {
            return ResumeDecision(false, "Session is not paused or interrupted")
        }
        if (session.pointsCompleted >= session.totalPoints) {
            return ResumeDecision(false, "All points already completed")
        }
        return ResumeDecision(true, remainingPoints = session.totalPoints - session.pointsCompleted)
    }

    fun resume(session: CalibrationSession): CalibrationSession {
        session.state = CalibrationSessionState.InProgress
        return session
    }
}

data class ResumeDecision(
    val allowed: Boolean,
    val reason: String? = null,
    val remainingPoints: Int = 0
)

object CalibrationRecoveryPlanner {

    fun plan(
        session: CalibrationSession,
        failureReasons: List<CalibrationFailureReason>
    ): List<CalibrationRecommendation> {
        val recommendations = linkedSetOf<CalibrationRecommendation>()
        failureReasons.forEach { reason ->
            when (reason) {
                CalibrationFailureReason.IncompleteSamples,
                CalibrationFailureReason.MissingCoverage,
                CalibrationFailureReason.Timeout -> recommendations.add(CalibrationRecommendation.RepeatCalibration)
                CalibrationFailureReason.InsufficientStability,
                CalibrationFailureReason.TrackingDiscontinuity -> {
                    recommendations.add(CalibrationRecommendation.ReduceMovement)
                    recommendations.add(CalibrationRecommendation.AdjustPhonePosition)
                }
                CalibrationFailureReason.ExcessiveRejects -> {
                    recommendations.add(CalibrationRecommendation.ImproveLighting)
                    recommendations.add(CalibrationRecommendation.CleanCamera)
                }
                CalibrationFailureReason.InterruptedSession -> recommendations.add(CalibrationRecommendation.RepeatCalibration)
                CalibrationFailureReason.LowRepeatability -> recommendations.add(CalibrationRecommendation.PracticeAgain)
                CalibrationFailureReason.UserCancelled -> recommendations.add(CalibrationRecommendation.None)
                CalibrationFailureReason.NoObservableEvidence -> recommendations.add(CalibrationRecommendation.RepeatCalibration)
            }
        }
        if (session.stabilityEvents + session.gazeDeviationEvents >= 3) {
            recommendations.add(CalibrationRecommendation.ReduceMovement)
        }
        if (session.trackingGaps >= 2) {
            recommendations.add(CalibrationRecommendation.CleanCamera)
            recommendations.add(CalibrationRecommendation.ImproveLighting)
        }
        if (recommendations.isEmpty()) {
            recommendations.add(CalibrationRecommendation.RepeatCalibration)
        }
        return recommendations.filter { it != CalibrationRecommendation.None }.ifEmpty {
            listOf(CalibrationRecommendation.RepeatCalibration)
        }
    }
}
