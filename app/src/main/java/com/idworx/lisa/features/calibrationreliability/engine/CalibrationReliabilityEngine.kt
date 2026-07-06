package com.idworx.lisa.features.calibrationreliability.engine

import com.idworx.lisa.features.calibrationreliability.model.CalibrationHealthState
import com.idworx.lisa.features.calibrationreliability.model.CalibrationReliabilityReport
import com.idworx.lisa.features.calibrationreliability.model.CalibrationResult
import com.idworx.lisa.features.calibrationreliability.model.CalibrationSession
import com.idworx.lisa.features.calibrationreliability.model.CalibrationSessionSource
import com.idworx.lisa.features.calibrationreliability.recovery.ResumeDecision
import com.idworx.lisa.features.calibrationreliability.recovery.RetryDecision

interface CalibrationReliabilityEngine {
    fun startSession(
        totalPoints: Int = 5,
        sensitivityLevel: Int? = null,
        source: CalibrationSessionSource = CalibrationSessionSource.CalibrationUi
    ): CalibrationSession

    fun recordSuccessfulSample(session: CalibrationSession)
    fun recordRejectedSample(session: CalibrationSession)
    fun recordPointCompleted(session: CalibrationSession)
    fun recordPointSkipped(session: CalibrationSession)
    fun recordStabilityEvent(session: CalibrationSession)
    fun recordGazeDeviation(session: CalibrationSession)
    fun recordIncompleteFixation(session: CalibrationSession)
    fun recordPause(session: CalibrationSession)
    fun recordTrackingGap(session: CalibrationSession)
    fun recordInterruption(session: CalibrationSession)
    fun pauseSession(session: CalibrationSession): CalibrationSession
    fun completeSession(session: CalibrationSession): CalibrationResult
    fun abandonSession(session: CalibrationSession): CalibrationResult

    fun evaluateRetry(session: CalibrationSession): RetryDecision
    fun evaluateResume(session: CalibrationSession): ResumeDecision
    fun resumeSession(session: CalibrationSession): CalibrationSession

    fun currentHealth(): CalibrationHealthState
    fun allowsCommunication(): Boolean
    fun shouldPauseGuidedLearning(): Boolean
    fun currentReport(): CalibrationReliabilityReport
    fun guidanceMessage(): String
    fun lastResult(): CalibrationResult?

    fun recordCommunicationFailure()
    fun recordLowConfidenceCommunication()
    fun notifySensitivityAdjusted(level: Int)
}
