package com.idworx.lisa.features.calibrationreliability.engine

import com.idworx.lisa.features.calibrationreliability.model.CalibrationQualityCategory
import com.idworx.lisa.features.calibrationreliability.model.CalibrationReliabilityOutcome
import com.idworx.lisa.features.calibrationreliability.model.CalibrationSessionSource
import com.idworx.lisa.features.calibrationreliability.model.CalibrationSessionState
import com.idworx.lisa.features.calibrationreliability.scoring.DriftDetector

object CalibrationVerifier {

    fun verifyExcellentCalibration(): Boolean {
        val engine = CalibrationReliabilityEngines.createForTests()
        val session = engine.startSession(totalPoints = 5, source = CalibrationSessionSource.CalibrationUi)
        repeat(5) {
            repeat(3) { engine.recordSuccessfulSample(session) }
            engine.recordPointCompleted(session)
        }
        val result = engine.completeSession(session)
        return result.score.category == CalibrationQualityCategory.Excellent &&
            result.outcome == CalibrationReliabilityOutcome.Pass
    }

    fun verifyPoorCalibrationDetected(): Boolean {
        val engine = CalibrationReliabilityEngines.createForTests()
        val session = engine.startSession(totalPoints = 5)
        engine.recordRejectedSample(session)
        engine.recordRejectedSample(session)
        engine.recordStabilityEvent(session)
        engine.recordStabilityEvent(session)
        engine.recordTrackingGap(session)
        engine.recordPointCompleted(session)
        val result = engine.completeSession(session)
        return result.score.overall < 60 || result.outcome != CalibrationReliabilityOutcome.Pass
    }

    fun verifyIncompleteCalibration(): Boolean {
        val engine = CalibrationReliabilityEngines.createForTests()
        val session = engine.startSession(totalPoints = 5)
        engine.recordSuccessfulSample(session)
        engine.recordPointCompleted(session)
        val result = engine.completeSession(session)
        return result.failureReasons.isNotEmpty() &&
            result.score.sampleCompleteness < 100
    }

    fun verifyInterruptedCalibration(): Boolean {
        val engine = CalibrationReliabilityEngines.createForTests()
        val session = engine.startSession(totalPoints = 5)
        engine.recordInterruption(session)
        engine.recordSuccessfulSample(session)
        val result = engine.completeSession(session)
        return session.state == CalibrationSessionState.Completed &&
            result.failureReasons.any { it.name.contains("Interrupted") }
    }

    fun verifyDriftDetection(): Boolean {
        val engine = CalibrationReliabilityEngines.createForTests()
        val session = engine.startSession(totalPoints = 5)
        repeat(5) {
            repeat(3) { engine.recordSuccessfulSample(session) }
            engine.recordPointCompleted(session)
        }
        engine.completeSession(session)
        repeat(6) { engine.recordCommunicationFailure() }
        val drift = DriftDetector.detect(
            lastResult = engine.lastResult(),
            lastCompletedMs = System.currentTimeMillis(),
            recentCommunicationFailures = 6,
            recentLowConfidenceAttempts = 0,
            retryCountSinceLastCalibration = 0
        )
        return drift.driftDetected
    }

    fun verifyCommunicationBlockedWhenInvalid(): Boolean {
        val engine = CalibrationReliabilityEngines.createForTests()
        val session = engine.startSession(totalPoints = 5)
        repeat(15) { engine.recordRejectedSample(session) }
        repeat(5) { engine.recordStabilityEvent(session) }
        repeat(4) { engine.recordTrackingGap(session) }
        engine.recordInterruption(session)
        engine.completeSession(session)
        return !engine.allowsCommunication() &&
            engine.currentHealth() == com.idworx.lisa.features.calibrationreliability.model.CalibrationHealthState.CalibrationInvalid
    }
}
