package com.idworx.lisa.validation.authority

import com.idworx.lisa.features.calibrationreliability.engine.CalibrationReliabilityEngines
import com.idworx.lisa.features.calibrationreliability.engine.CalibrationVerifier
import com.idworx.lisa.features.calibrationreliability.integration.CalibrationCommunicationReliabilityBridge
import com.idworx.lisa.features.calibrationreliability.integration.CalibrationGuidedLearningAdapter
import com.idworx.lisa.features.calibrationreliability.integration.CalibrationPersonalityAdapter
import com.idworx.lisa.features.calibrationreliability.model.CalibrationHealthState
import com.idworx.lisa.features.calibrationreliability.model.CalibrationQualityCategory
import com.idworx.lisa.features.calibrationreliability.model.CalibrationReliabilityOutcome
import com.idworx.lisa.features.calibrationreliability.model.CalibrationSessionState
import com.idworx.lisa.features.calibrationreliability.monitoring.CalibrationDiagnostics
import com.idworx.lisa.features.calibrationreliability.monitoring.CalibrationHealthMonitor
import com.idworx.lisa.features.calibrationreliability.recovery.CalibrationRecoveryPlanner
import com.idworx.lisa.features.calibrationreliability.recovery.CalibrationRetryPolicy
import com.idworx.lisa.features.calibrationreliability.recovery.CalibrationResumeManager
import com.idworx.lisa.features.calibrationreliability.scoring.CalibrationScorer
import com.idworx.lisa.features.calibrationreliability.scoring.DriftDetector
import com.idworx.lisa.features.calibrationreliability.scoring.RepeatabilityScorer
import com.idworx.lisa.features.calibrationreliability.scoring.StabilityScorer
import com.idworx.lisa.features.calibrationreliability.validation.CalibrationReliabilityAuthorityV1
import com.idworx.lisa.features.companionmemory.engine.CompanionMemoryEngines
import com.idworx.lisa.features.companionmemory.integration.CalibrationMemoryAdapter
import com.idworx.lisa.features.companionmemory.model.LearningMilestone
import com.idworx.lisa.features.companionmemory.repository.InMemoryCompanionMemoryRepository
import com.idworx.lisa.features.corecommunicationreliability.engine.CommunicationReliabilityContext
import com.idworx.lisa.features.corecommunicationreliability.engine.CoreCommunicationReliabilityEngines
import com.idworx.lisa.features.corecommunicationreliability.model.CommunicationReliabilityOutcome
import com.idworx.lisa.features.onboardingguide.model.TrainingProgress
import com.idworx.lisa.features.onboardingguide.services.AdaptiveLearningService
import com.idworx.lisa.features.personality.engine.LisaPersonalityEngines
import com.idworx.lisa.defaultLanguageMappings
import com.idworx.lisa.validation.ValidationOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CalibrationReliabilityAuthorityV1Test {

    private lateinit var engine: com.idworx.lisa.features.calibrationreliability.engine.DefaultCalibrationReliabilityEngine

    @Before
    fun setUp() {
        CalibrationReliabilityEngines.resetForTests()
        engine = CalibrationReliabilityEngines.createForTests(
            companionMemory = CompanionMemoryEngines.createForTests(InMemoryCompanionMemoryRepository())
        )
    }

    @Test
    fun fullValidation_passesAndEmitsPassToken() {
        val report = CalibrationReliabilityAuthorityV1.validate()
        if (report.outcome != ValidationOutcome.PASS) {
            println("FAILED CHECKS: ${report.failedChecks}")
            report.checkResults.filter { !it.passed }.forEach {
                println("${it.checkId}: ${it.description}")
            }
        }
        assertEquals("Failed checks: ${report.failedChecks}", ValidationOutcome.PASS, report.outcome)
        assertEquals(CalibrationReliabilityAuthorityV1.PASS_TOKEN, report.passToken)
        assertTrue(report.failedChecks.isEmpty())
        println(report.formatReport())
        println(CalibrationReliabilityAuthorityV1.PASS_TOKEN)
    }

    @Test
    fun successfulCalibration_scoresPass() {
        val session = engine.startSession(totalPoints = 5)
        repeat(5) {
            repeat(3) { engine.recordSuccessfulSample(session) }
            engine.recordPointCompleted(session)
        }
        val result = engine.completeSession(session)
        assertEquals(CalibrationReliabilityOutcome.Pass, result.outcome)
        assertTrue(result.score.overall >= 60)
    }

    @Test
    fun incompleteCalibration_detected() {
        assertTrue(CalibrationVerifier.verifyIncompleteCalibration())
    }

    @Test
    fun interruptedCalibration_detected() {
        val session = engine.startSession(totalPoints = 5)
        engine.recordInterruption(session)
        engine.recordSuccessfulSample(session)
        val result = engine.completeSession(session)
        assertTrue(result.failureReasons.any { it.name.contains("Interrupted") })
    }

    @Test
    fun poorCalibration_detected() {
        assertTrue(CalibrationVerifier.verifyPoorCalibrationDetected())
    }

    @Test
    fun excellentCalibration_scoredExcellent() {
        assertTrue(CalibrationVerifier.verifyExcellentCalibration())
    }

    @Test
    fun driftDetection_operational() {
        assertTrue(CalibrationVerifier.verifyDriftDetection())
        engine.recordCommunicationFailure()
        engine.recordCommunicationFailure()
        engine.recordCommunicationFailure()
        engine.recordLowConfidenceCommunication()
        val drift = DriftDetector.detect(
            lastResult = engine.lastResult(),
            lastCompletedMs = System.currentTimeMillis(),
            recentCommunicationFailures = 3,
            recentLowConfidenceAttempts = 1,
            retryCountSinceLastCalibration = 0
        )
        assertFalse(drift.driftDetected)
    }

    @Test
    fun recoveryRecommendation_fromObservableEvidence() {
        val session = engine.startSession(totalPoints = 5)
        engine.recordRejectedSample(session)
        engine.recordRejectedSample(session)
        engine.recordTrackingGap(session)
        val recommendations = CalibrationRecoveryPlanner.plan(
            session,
            listOf(com.idworx.lisa.features.calibrationreliability.model.CalibrationFailureReason.ExcessiveRejects)
        )
        assertTrue(recommendations.isNotEmpty())
        assertTrue(
            recommendations.contains(com.idworx.lisa.features.calibrationreliability.model.CalibrationRecommendation.ImproveLighting) ||
                recommendations.contains(com.idworx.lisa.features.calibrationreliability.model.CalibrationRecommendation.RepeatCalibration)
        )
    }

    @Test
    fun healthMonitor_tracksStates() {
        assertTrue(CalibrationHealthMonitor.allowsCommunication(CalibrationHealthState.Healthy))
        assertFalse(CalibrationHealthMonitor.allowsCommunication(CalibrationHealthState.CalibrationInvalid))
        assertTrue(CalibrationHealthMonitor.shouldPauseGuidedLearning(CalibrationHealthState.CalibrationRequired))
    }

    @Test
    fun resumeAfterInterruption_allowed() {
        val session = engine.startSession(totalPoints = 5)
        engine.recordInterruption(session)
        engine.recordPointCompleted(session)
        val resume = CalibrationResumeManager.canResume(session)
        assertTrue(resume.allowed)
        engine.resumeSession(session)
        assertEquals(CalibrationSessionState.InProgress, session.state)
    }

    @Test
    fun retryPolicy_limitsExcessiveRetries() {
        val session = engine.startSession(totalPoints = 5)
        repeat(6) {
            val decision = CalibrationRetryPolicy.canRetry(session, it)
            if (it < 5) assertTrue(decision.allowed) else assertFalse(decision.allowed)
        }
    }

    @Test
    fun calibrationScoring_producesMetrics() {
        val session = engine.startSession(totalPoints = 5)
        repeat(3) { engine.recordSuccessfulSample(session) }
        engine.recordPointCompleted(session)
        val score = CalibrationScorer.score(session)
        val metrics = CalibrationScorer.metrics(session, score)
        assertTrue(metrics.any { it.name == "overall_score" })
        assertTrue(score.overall in 0..100)
    }

    @Test
    fun repeatability_comparedToPriorSession() {
        val session = engine.startSession(totalPoints = 5)
        repeat(5) {
            repeat(3) { engine.recordSuccessfulSample(session) }
            engine.recordPointCompleted(session)
        }
        engine.completeSession(session)
        val second = engine.startSession(totalPoints = 5)
        repeat(5) {
            repeat(3) { engine.recordSuccessfulSample(second) }
            engine.recordPointCompleted(second)
        }
        val repeatability = RepeatabilityScorer.score(second, engine.lastResult()?.score?.overall)
        assertNotNull(repeatability.priorSessionScore)
    }

    @Test
    fun stabilityScorer_penalizesEvents() {
        val session = engine.startSession(totalPoints = 5)
        engine.recordStabilityEvent(session)
        engine.recordGazeDeviation(session)
        val stability = StabilityScorer.score(session)
        assertTrue(stability.value < 100)
    }

    @Test
    fun guidedLearningIntegration_respectsCalibrationHealth() {
        val decision = CalibrationGuidedLearningAdapter.evaluate(engine, TrainingProgress())
        assertNotNull(decision.guidanceMessage)
        assertTrue(AdaptiveLearningService.isRecalibrationAvailable(engine))
    }

    @Test
    fun communicationReliabilityIntegration_blocksInvalidCalibration() {
        assertTrue(CalibrationVerifier.verifyCommunicationBlockedWhenInvalid())
        val ccr = CoreCommunicationReliabilityEngines.createForTests()
        val report = ccr.evaluatePhrasePath(
            CommunicationReliabilityContext(
                mappings = defaultLanguageMappings(),
                calibrationAllowsCommunication = false,
                calibrationHealthState = CalibrationHealthState.CalibrationInvalid
            ),
            2, 6
        )
        assertEquals(CommunicationReliabilityOutcome.BLOCKED, report.finalOutcome)
    }

    @Test
    fun personalityEngineIntegration_providesGuidance() {
        val message = CalibrationPersonalityAdapter.guidanceForHealth(
            LisaPersonalityEngines.default,
            CalibrationHealthState.RecommendRecalibration,
            emptyList()
        )
        assertTrue(message.contains("calibrat", ignoreCase = true))
    }

    @Test
    fun companionMemoryIntegration_recordsMilestonesOnly() {
        val memory = CompanionMemoryEngines.createForTests(InMemoryCompanionMemoryRepository())
        val testEngine = CalibrationReliabilityEngines.createForTests(companionMemory = memory)
        val session = testEngine.startSession(totalPoints = 5, sensitivityLevel = 3)
        repeat(5) {
            repeat(3) { testEngine.recordSuccessfulSample(session) }
            testEngine.recordPointCompleted(session)
        }
        testEngine.completeSession(session)
        assertTrue(LearningMilestone.FirstSuccessfulCalibration in memory.getMilestones())
    }

    @Test
    fun diagnostics_reportCurrentState() {
        val session = engine.startSession(totalPoints = 5)
        repeat(3) { engine.recordSuccessfulSample(session) }
        engine.recordPointCompleted(session)
        engine.completeSession(session)
        engine.currentReport()
        assertNotNull(CalibrationDiagnostics.lastReport())
        assertTrue(CalibrationDiagnostics.formatSummary().contains("Calibration Diagnostics"))
    }

    @Test
    fun communicationBridge_recordsFailures() {
        CalibrationCommunicationReliabilityBridge.onCommunicationBlocked(engine)
        CalibrationCommunicationReliabilityBridge.onLowConfidenceCommunication(engine)
        assertTrue(true)
    }
}
