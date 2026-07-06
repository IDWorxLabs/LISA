package com.idworx.lisa.validation.authority

import com.idworx.lisa.features.communicationanalytics.diagnostics.AnalyticsDiagnostics
import com.idworx.lisa.features.communicationanalytics.engine.AnalyticsVerifier
import com.idworx.lisa.features.communicationanalytics.engine.CommunicationAnalyticsEngines
import com.idworx.lisa.features.communicationanalytics.integration.AnalyticsPersonalityAdapter
import com.idworx.lisa.features.communicationanalytics.integration.CommunicationAnalyticsBridge
import com.idworx.lisa.features.communicationanalytics.metrics.CalibrationImpactCalculator
import com.idworx.lisa.features.communicationanalytics.metrics.EmergencyAnalyticsCalculator
import com.idworx.lisa.features.communicationanalytics.metrics.FalseNegativeCalculator
import com.idworx.lisa.features.communicationanalytics.metrics.FalsePositiveCalculator
import com.idworx.lisa.features.communicationanalytics.metrics.NavigationAnalyticsCalculator
import com.idworx.lisa.features.communicationanalytics.metrics.PhraseTimingCalculator
import com.idworx.lisa.features.communicationanalytics.metrics.RetryRateCalculator
import com.idworx.lisa.features.communicationanalytics.metrics.SuccessRateCalculator
import com.idworx.lisa.features.communicationanalytics.model.AttemptTiming
import com.idworx.lisa.features.communicationanalytics.model.CommunicationAttemptAnalytics
import com.idworx.lisa.features.communicationanalytics.model.TrendDirection
import com.idworx.lisa.features.communicationanalytics.reporting.CommunicationReportGenerator
import com.idworx.lisa.features.communicationanalytics.reporting.TrendAnalyzer
import com.idworx.lisa.features.communicationanalytics.validation.CommunicationAccuracyAnalyticsAuthorityV1
import com.idworx.lisa.features.corecommunicationreliability.model.CommunicationReliabilityOutcome
import com.idworx.lisa.features.corecommunicationreliability.model.PhraseReliabilityAction
import com.idworx.lisa.features.personality.engine.LisaPersonalityEngines
import com.idworx.lisa.validation.ValidationOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CommunicationAccuracyAnalyticsAuthorityV1Test {

    private lateinit var engine: com.idworx.lisa.features.communicationanalytics.engine.DefaultCommunicationAnalyticsEngine

    @Before
    fun setUp() {
        CommunicationAnalyticsEngines.resetForTests()
        CommunicationAnalyticsBridge.detach()
        engine = CommunicationAnalyticsEngines.createForTests()
    }

    @Test
    fun fullValidation_passesAndEmitsPassToken() {
        val report = CommunicationAccuracyAnalyticsAuthorityV1.validate()
        if (report.outcome != ValidationOutcome.PASS) {
            println("FAILED: ${report.failedChecks}")
        }
        assertEquals("Failed: ${report.failedChecks}", ValidationOutcome.PASS, report.outcome)
        assertEquals(CommunicationAccuracyAnalyticsAuthorityV1.PASS_TOKEN, report.passToken)
        assertTrue(report.failedChecks.isEmpty())
        println(report.formatReport())
        println(CommunicationAccuracyAnalyticsAuthorityV1.PASS_TOKEN)
    }

    @Test
    fun successRate_calculatedCorrectly() {
        assertTrue(AnalyticsVerifier.verifySuccessRateCalculation())
        engine.recordAttempt(successAttempt("yes"))
        engine.recordAttempt(successAttempt("no"))
        engine.recordAttempt(blockedAttempt())
        val rate = SuccessRateCalculator.overall(engine.attempts())
        assertEquals(3, rate.total)
        assertTrue(rate.rate > 0.5f)
    }

    @Test
    fun retryRate_calculated() {
        repeat(3) { engine.recordAttempt(successAttempt("yes")) }
        assertTrue(RetryRateCalculator.averageRetries(engine.attempts()) >= 0f)
        assertTrue(RetryRateCalculator.maxRetries(engine.attempts()) >= 0)
    }

    @Test
    fun falsePositives_detected() {
        assertTrue(AnalyticsVerifier.verifyFalsePositiveDetection())
        engine.recordAttempt(
            blockedAttempt().copy(
                duplicateBlocked = true,
                blockedReason = "Duplicate sequence firing prevented"
            )
        )
        assertTrue(FalsePositiveCalculator.count(engine.attempts()) >= 1)
    }

    @Test
    fun falseNegatives_detected() {
        assertTrue(AnalyticsVerifier.verifyFalseNegativeDetection())
        engine.recordAttempt(
            blockedAttempt().copy(
                phraseId = "yes",
                blockedReason = "Confidence below safe threshold"
            )
        )
        assertTrue(FalseNegativeCalculator.count(engine.attempts()) >= 1)
    }

    @Test
    fun phraseTiming_measured() {
        val attempt = successAttempt("yes").copy(
            timing = AttemptTiming(
                firstBlinkMs = 1000L,
                sequenceCompleteMs = 1200L,
                phraseMatchMs = 1250L,
                speechRequestMs = 1300L,
                speechCompleteMs = 1800L
            )
        )
        engine.recordAttempt(attempt)
        assertEquals(800L, PhraseTimingCalculator.averageCompletionTimeMs(engine.attempts()))
    }

    @Test
    fun phraseStatistics_generated() {
        repeat(5) { engine.recordAttempt(successAttempt("yes")) }
        repeat(2) { engine.recordAttempt(blockedAttempt()) }
        val report = engine.generateReport()
        assertTrue(report.phraseAnalytics.any { it.phraseId == "yes" })
        assertTrue(report.phraseAnalytics.first { it.phraseId == "yes" }.attemptCount >= 5)
    }

    @Test
    fun calibrationImpact_analyzed() {
        engine.recordAttempt(successAttempt("yes").copy(calibrationScore = 85, calibrationBlocked = false))
        engine.recordAttempt(blockedAttempt().copy(calibrationBlocked = true, calibrationScore = 35))
        val impact = CalibrationImpactCalculator.analyze(engine.attempts())
        assertTrue(impact.attemptsByHealthState.isNotEmpty())
        assertTrue(impact.blockedByCalibrationState >= 1)
    }

    @Test
    fun navigationMetrics_calculated() {
        engine.recordAttempt(successAttempt("nav").copy(navigationTraining = true))
        val nav = NavigationAnalyticsCalculator.analyze(engine.attempts())
        assertTrue(nav.gestureAttempts >= 1)
    }

    @Test
    fun emergencyMetrics_calculated() {
        engine.recordAttempt(
            blockedAttempt().copy(emergency = true, blockedReason = "Emergency requires confirmation")
        )
        val emergency = EmergencyAnalyticsCalculator.analyze(engine.attempts())
        assertTrue(emergency.activationAttempts >= 1 || emergency.blockedActivations >= 1)
    }

    @Test
    fun trends_generated() {
        repeat(12) { i ->
            if (i < 6) engine.recordAttempt(blockedAttempt()) else engine.recordAttempt(successAttempt("yes"))
        }
        val trends = TrendAnalyzer.analyze(engine.attempts())
        assertTrue(trends.isNotEmpty())
        assertTrue(trends.any { it.direction != TrendDirection.InsufficientData } || trends.size > 1)
    }

    @Test
    fun reportGeneration_complete() {
        repeat(3) { engine.recordAttempt(successAttempt("yes")) }
        val report = CommunicationReportGenerator.generate(engine.attempts())
        assertNotNull(report.summary)
        assertTrue(report.metrics.isNotEmpty())
        assertTrue(report.evidenceSummary.isNotBlank())
    }

    @Test
    fun diagnostics_available() {
        repeat(2) { engine.recordAttempt(successAttempt("yes")) }
        engine.generateReport()
        assertNotNull(AnalyticsDiagnostics.lastReport())
        assertTrue(AnalyticsDiagnostics.formatSummary().contains("Communication Analytics"))
    }

    @Test
    fun personalityIntegration_supportiveMessages() {
        val message = AnalyticsPersonalityAdapter.supportiveMessageForAnalytics(
            LisaPersonalityEngines.default,
            consecutiveFailures = 5,
            recommendRecalibration = true
        )
        assertNotNull(message)
        assertTrue(message!!.isNotBlank())
    }

    @Test
    fun noBehaviorChange_withAnalyticsAttached() {
        assertTrue(AnalyticsVerifier.verifyNoBehaviorChange())
    }

    private fun successAttempt(phraseId: String) = CommunicationAttemptAnalytics(
        attemptId = java.util.UUID.randomUUID().toString(),
        timestampMs = System.currentTimeMillis(),
        mode = com.idworx.lisa.features.corecommunicationreliability.model.CommunicationMode.MAIN,
        calibrationHealth = null,
        calibrationScore = 80,
        phraseId = phraseId,
        phraseText = phraseId,
        confidenceScore = 0.9f,
        action = PhraseReliabilityAction.PROCEED_TO_CONFIRMATION,
        finalOutcome = CommunicationReliabilityOutcome.PASS,
        blockedReason = null,
        emergency = false,
        emergencyTraining = false,
        navigationTraining = false,
        communicationTraining = false,
        practiceMode = false,
        speechSuccess = true,
        timing = AttemptTiming(firstBlinkMs = System.currentTimeMillis() - 400, speechCompleteMs = System.currentTimeMillis()),
        sequenceLabel = "L2 R6"
    )

    private fun blockedAttempt() = CommunicationAttemptAnalytics(
        attemptId = java.util.UUID.randomUUID().toString(),
        timestampMs = System.currentTimeMillis(),
        mode = com.idworx.lisa.features.corecommunicationreliability.model.CommunicationMode.MAIN,
        calibrationHealth = null,
        calibrationScore = null,
        phraseId = null,
        phraseText = null,
        confidenceScore = 0.2f,
        action = PhraseReliabilityAction.BLOCK,
        finalOutcome = CommunicationReliabilityOutcome.BLOCKED,
        blockedReason = "No phrase matched",
        emergency = false,
        emergencyTraining = false,
        navigationTraining = false,
        communicationTraining = false,
        practiceMode = false,
        sequenceLabel = "L0 R0"
    )
}
