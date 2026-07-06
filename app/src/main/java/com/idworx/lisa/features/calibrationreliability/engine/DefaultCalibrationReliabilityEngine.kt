package com.idworx.lisa.features.calibrationreliability.engine

import com.idworx.lisa.features.calibrationreliability.integration.CalibrationPersonalityAdapter
import com.idworx.lisa.features.calibrationreliability.metadata.CalibrationReliabilityMetadata
import com.idworx.lisa.features.calibrationreliability.model.CalibrationFailureReason
import com.idworx.lisa.features.calibrationreliability.model.CalibrationHealthState
import com.idworx.lisa.features.calibrationreliability.model.CalibrationQualityCategory
import com.idworx.lisa.features.calibrationreliability.model.CalibrationRecommendation
import com.idworx.lisa.features.calibrationreliability.model.CalibrationReliabilityOutcome
import com.idworx.lisa.features.calibrationreliability.model.CalibrationReliabilityReport
import com.idworx.lisa.features.calibrationreliability.model.CalibrationResult
import com.idworx.lisa.features.calibrationreliability.model.CalibrationSession
import com.idworx.lisa.features.calibrationreliability.model.CalibrationSessionSource
import com.idworx.lisa.features.calibrationreliability.model.CalibrationSessionState
import com.idworx.lisa.features.calibrationreliability.monitoring.CalibrationDiagnostics
import com.idworx.lisa.features.calibrationreliability.monitoring.CalibrationHealthMonitor
import com.idworx.lisa.features.calibrationreliability.monitoring.CalibrationLifecycleTracker
import com.idworx.lisa.features.calibrationreliability.recovery.CalibrationRecoveryPlanner
import com.idworx.lisa.features.calibrationreliability.recovery.CalibrationResumeManager
import com.idworx.lisa.features.calibrationreliability.recovery.CalibrationRetryPolicy
import com.idworx.lisa.features.calibrationreliability.recovery.ResumeDecision
import com.idworx.lisa.features.calibrationreliability.recovery.RetryDecision
import com.idworx.lisa.features.calibrationreliability.scoring.CalibrationScorer
import com.idworx.lisa.features.calibrationreliability.scoring.DriftDetector
import com.idworx.lisa.features.companionmemory.engine.CompanionMemoryEngine
import com.idworx.lisa.features.companionmemory.integration.CalibrationMemoryAdapter
import com.idworx.lisa.features.personality.engine.LisaPersonalityEngine
import com.idworx.lisa.features.personality.engine.LisaPersonalityEngines

class DefaultCalibrationReliabilityEngine(
    private val personality: LisaPersonalityEngine = LisaPersonalityEngines.default,
    private val companionMemory: CompanionMemoryEngine =
        com.idworx.lisa.features.companionmemory.engine.CompanionMemoryEngines.default
) : CalibrationReliabilityEngine {

    private var lastResultInternal: CalibrationResult? = null
    private var lastCompletedMs: Long? = null
    private var priorOverallScore: Int? = null
    private var hasLegacySensitivity: Boolean = true
    private var communicationFailures: Int = 0
    private var lowConfidenceAttempts: Int = 0
    private var totalRetries: Int = 0
    private var lastRecommendations: List<CalibrationRecommendation> = emptyList()

    override fun startSession(
        totalPoints: Int,
        sensitivityLevel: Int?,
        source: CalibrationSessionSource
    ): CalibrationSession {
        CalibrationLifecycleTracker.recordSessionStart()
        return CalibrationSession(
            totalPoints = totalPoints,
            sensitivityLevel = sensitivityLevel,
            source = source,
            state = CalibrationSessionState.InProgress
        )
    }

    override fun recordSuccessfulSample(session: CalibrationSession) {
        session.successfulSamples++
    }

    override fun recordRejectedSample(session: CalibrationSession) {
        session.rejectedSamples++
    }

    override fun recordPointCompleted(session: CalibrationSession) {
        session.pointsCompleted = (session.pointsCompleted + 1).coerceAtMost(session.totalPoints)
    }

    override fun recordPointSkipped(session: CalibrationSession) {
        session.pointsSkipped++
    }

    override fun recordStabilityEvent(session: CalibrationSession) {
        session.stabilityEvents++
    }

    override fun recordGazeDeviation(session: CalibrationSession) {
        session.gazeDeviationEvents++
    }

    override fun recordIncompleteFixation(session: CalibrationSession) {
        session.incompleteFixations++
    }

    override fun recordPause(session: CalibrationSession) {
        session.pauses++
    }

    override fun recordTrackingGap(session: CalibrationSession) {
        session.trackingGaps++
    }

    override fun recordInterruption(session: CalibrationSession) {
        session.interruptions++
        session.state = CalibrationSessionState.Interrupted
    }

    override fun pauseSession(session: CalibrationSession): CalibrationSession {
        session.state = CalibrationSessionState.Paused
        return session
    }

    override fun completeSession(session: CalibrationSession): CalibrationResult {
        session.endTimeMs = System.currentTimeMillis()
        session.state = CalibrationSessionState.Completed
        CalibrationLifecycleTracker.recordSessionComplete()

        val score = CalibrationScorer.score(session, priorOverallScore)
        val failureReasons = deriveFailureReasons(session, score)
        val outcome = deriveOutcome(score, failureReasons)
        val recommendations = CalibrationRecoveryPlanner.plan(session, failureReasons)
        val metrics = CalibrationScorer.metrics(session, score)
        val evidence = buildEvidenceSummary(session, score)

        val result = CalibrationResult(
            session = session,
            score = score,
            outcome = outcome,
            failureReasons = failureReasons,
            recommendations = recommendations,
            evidenceSummary = evidence,
            metrics = metrics
        )

        CalibrationMemoryAdapter.onCalibrationCompleted(companionMemory, result, priorOverallScore)
        priorOverallScore = score.overall
        lastResultInternal = result
        lastCompletedMs = session.endTimeMs
        lastRecommendations = recommendations
        hasLegacySensitivity = false
        refreshDiagnostics(session.sessionId, result)
        return result
    }

    override fun abandonSession(session: CalibrationSession): CalibrationResult {
        session.state = CalibrationSessionState.Abandoned
        session.endTimeMs = System.currentTimeMillis()
        val score = CalibrationScorer.score(session, priorOverallScore)
        val failureReasons = listOf(CalibrationFailureReason.UserCancelled)
        val result = CalibrationResult(
            session = session,
            score = score,
            outcome = CalibrationReliabilityOutcome.Fail,
            failureReasons = failureReasons,
            recommendations = listOf(CalibrationRecommendation.RepeatCalibration),
            evidenceSummary = "Calibration abandoned by user",
            metrics = CalibrationScorer.metrics(session, score)
        )
        lastResultInternal = result
        lastRecommendations = result.recommendations
        refreshDiagnostics(session.sessionId, result)
        return result
    }

    override fun evaluateRetry(session: CalibrationSession): RetryDecision {
        totalRetries++
        session.retries++
        CalibrationLifecycleTracker.recordRetry()
        return CalibrationRetryPolicy.canRetry(session, totalRetries)
    }

    override fun evaluateResume(session: CalibrationSession): ResumeDecision =
        CalibrationResumeManager.canResume(session)

    override fun resumeSession(session: CalibrationSession): CalibrationSession =
        CalibrationResumeManager.resume(session)

    override fun currentHealth(): CalibrationHealthState {
        val drift = currentDrift()
        return CalibrationHealthMonitor.evaluate(
            lastResultInternal,
            lastCompletedMs,
            drift,
            hasLegacySensitivity
        )
    }

    override fun allowsCommunication(): Boolean =
        CalibrationHealthMonitor.allowsCommunication(currentHealth())

    override fun shouldPauseGuidedLearning(): Boolean =
        CalibrationHealthMonitor.shouldPauseGuidedLearning(currentHealth())

    override fun currentReport(): CalibrationReliabilityReport {
        val drift = currentDrift()
        val health = CalibrationHealthMonitor.evaluate(
            lastResultInternal,
            lastCompletedMs,
            drift,
            hasLegacySensitivity
        )
        return CalibrationReliabilityReport(
            sessionId = lastResultInternal?.session?.sessionId,
            healthState = health,
            lastScore = lastResultInternal?.score,
            lastResult = lastResultInternal,
            driftDetected = drift.driftDetected,
            driftReason = drift.reason,
            recommendations = lastRecommendations,
            allowsCommunication = CalibrationHealthMonitor.allowsCommunication(health),
            metrics = lastResultInternal?.metrics ?: emptyList(),
            evidenceSummary = lastResultInternal?.evidenceSummary ?: "No calibration completed"
        )
    }

    override fun guidanceMessage(): String =
        CalibrationPersonalityAdapter.guidanceForHealth(
            personality,
            currentHealth(),
            lastRecommendations
        )

    override fun lastResult(): CalibrationResult? = lastResultInternal

    override fun recordCommunicationFailure() {
        communicationFailures++
    }

    override fun recordLowConfidenceCommunication() {
        lowConfidenceAttempts++
    }

    override fun notifySensitivityAdjusted(level: Int) {
        hasLegacySensitivity = true
        val session = startSession(
            totalPoints = 1,
            sensitivityLevel = level,
            source = CalibrationSessionSource.SensitivityAdjustment
        )
        repeat(3) { recordSuccessfulSample(session) }
        recordPointCompleted(session)
        completeSession(session)
    }

    internal fun resetCountersForTests() {
        communicationFailures = 0
        lowConfidenceAttempts = 0
        totalRetries = 0
        lastResultInternal = null
        lastCompletedMs = null
        priorOverallScore = null
        hasLegacySensitivity = true
        lastRecommendations = emptyList()
    }

    private fun currentDrift() = DriftDetector.detect(
        lastResult = lastResultInternal,
        lastCompletedMs = lastCompletedMs,
        recentCommunicationFailures = communicationFailures,
        recentLowConfidenceAttempts = lowConfidenceAttempts,
        retryCountSinceLastCalibration = CalibrationLifecycleTracker.retryCountSince(lastCompletedMs)
    )

    private fun refreshDiagnostics(sessionId: String?, result: CalibrationResult) {
        val drift = currentDrift()
        val report = CalibrationReliabilityReport(
            sessionId = sessionId,
            healthState = currentHealth(),
            lastScore = result.score,
            lastResult = result,
            driftDetected = drift.driftDetected,
            driftReason = drift.reason,
            recommendations = result.recommendations,
            allowsCommunication = allowsCommunication(),
            metrics = result.metrics,
            evidenceSummary = result.evidenceSummary
        )
        CalibrationDiagnostics.record(report, result, drift)
    }

    private fun deriveFailureReasons(
        session: CalibrationSession,
        score: com.idworx.lisa.features.calibrationreliability.model.CalibrationScore
    ): List<CalibrationFailureReason> {
        val reasons = mutableListOf<CalibrationFailureReason>()
        if (session.completionRatio < 0.5f) reasons.add(CalibrationFailureReason.IncompleteSamples)
        if (session.interruptions > 0) reasons.add(CalibrationFailureReason.InterruptedSession)
        if (score.eyeStability < 40) reasons.add(CalibrationFailureReason.InsufficientStability)
        if (session.rejectedSamples > session.successfulSamples) reasons.add(CalibrationFailureReason.ExcessiveRejects)
        if (session.pointsCompleted + session.pointsSkipped < session.totalPoints / 2) {
            reasons.add(CalibrationFailureReason.MissingCoverage)
        }
        if (session.trackingGaps >= 2) reasons.add(CalibrationFailureReason.TrackingDiscontinuity)
        if (score.repeatability < 50 && priorOverallScore != null) reasons.add(CalibrationFailureReason.LowRepeatability)
        if (session.totalSamples == 0) reasons.add(CalibrationFailureReason.NoObservableEvidence)
        return reasons
    }

    private fun deriveOutcome(
        score: com.idworx.lisa.features.calibrationreliability.model.CalibrationScore,
        failureReasons: List<CalibrationFailureReason>
    ): CalibrationReliabilityOutcome = when {
        score.category == CalibrationQualityCategory.Failed ||
            score.overall < CalibrationReliabilityMetadata.MIN_COMMUNICATION_SCORE ||
            failureReasons.contains(CalibrationFailureReason.NoObservableEvidence) ->
            CalibrationReliabilityOutcome.Fail
        score.overall < CalibrationReliabilityMetadata.PASS_SCORE_THRESHOLD ||
            failureReasons.isNotEmpty() ->
            CalibrationReliabilityOutcome.Marginal
        else -> CalibrationReliabilityOutcome.Pass
    }

    private fun buildEvidenceSummary(
        session: CalibrationSession,
        score: com.idworx.lisa.features.calibrationreliability.model.CalibrationScore
    ): String =
        "Score ${score.overall} (${score.category.name}): ${session.pointsCompleted}/${session.totalPoints} points, " +
            "${session.successfulSamples} accepted, ${session.rejectedSamples} rejected, " +
            "${session.stabilityEvents} stability events, ${session.trackingGaps} tracking gaps"
}

object CalibrationReliabilityEngines {
    @Volatile
    private var instance: DefaultCalibrationReliabilityEngine? = null

    val default: CalibrationReliabilityEngine
        get() = instance ?: DefaultCalibrationReliabilityEngine().also { instance = it }

    fun createForTests(
        personality: LisaPersonalityEngine = LisaPersonalityEngines.default,
        companionMemory: CompanionMemoryEngine =
            com.idworx.lisa.features.companionmemory.engine.CompanionMemoryEngines.default
    ): DefaultCalibrationReliabilityEngine {
        val engine = DefaultCalibrationReliabilityEngine(personality, companionMemory)
        instance = engine
        return engine
    }

    fun resetForTests() {
        instance?.let { (it as? DefaultCalibrationReliabilityEngine)?.resetCountersForTests() }
        instance = null
        CalibrationLifecycleTracker.reset()
        CalibrationDiagnostics.clear()
    }
}
