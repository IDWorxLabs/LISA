package com.idworx.lisa.features.communicationanalytics.engine

import com.idworx.lisa.defaultLanguageMappings
import com.idworx.lisa.features.communicationanalytics.engine.CommunicationAnalyticsEngines.createForTests
import com.idworx.lisa.features.communicationanalytics.metrics.AccuracyCalculator
import com.idworx.lisa.features.communicationanalytics.metrics.FalseNegativeCalculator
import com.idworx.lisa.features.communicationanalytics.metrics.FalsePositiveCalculator
import com.idworx.lisa.features.communicationanalytics.metrics.RetryRateCalculator
import com.idworx.lisa.features.communicationanalytics.metrics.SuccessRateCalculator
import com.idworx.lisa.features.communicationanalytics.integration.CommunicationAnalyticsBridge
import com.idworx.lisa.features.communicationanalytics.model.AttemptTiming
import com.idworx.lisa.features.communicationanalytics.model.CommunicationAttemptAnalytics
import com.idworx.lisa.features.communicationanalytics.model.TrendDirection
import com.idworx.lisa.features.communicationanalytics.reporting.TrendAnalyzer
import com.idworx.lisa.features.corecommunicationreliability.engine.CommunicationReliabilityContext
import com.idworx.lisa.features.corecommunicationreliability.engine.CoreCommunicationReliabilityEngines
import com.idworx.lisa.features.corecommunicationreliability.model.CommunicationReliabilityOutcome
import com.idworx.lisa.features.corecommunicationreliability.model.PhraseReliabilityAction

object AnalyticsVerifier {

    fun verifySuccessRateCalculation(): Boolean {
        val engine = createForTests()
        recordSuccess(engine, "yes")
        recordSuccess(engine, "no")
        recordBlocked(engine)
        val report = engine.generateReport()
        return report.summary.overallSuccessRate.total == 3 &&
            report.summary.overallSuccessRate.successful >= 2
    }

    fun verifyFalsePositiveDetection(): Boolean {
        val engine = createForTests()
        engine.recordAttempt(sampleAttempt(blockedReason = "Duplicate sequence firing prevented", duplicateBlocked = true))
        return FalsePositiveCalculator.count(engine.attempts()) >= 1
    }

    fun verifyFalseNegativeDetection(): Boolean {
        val engine = createForTests()
        engine.recordAttempt(
            sampleAttempt(
                phraseId = "yes",
                outcome = CommunicationReliabilityOutcome.BLOCKED,
                blockedReason = "Confidence below safe threshold"
            )
        )
        return FalseNegativeCalculator.count(engine.attempts()) >= 1
    }

    fun verifyReportGeneration(): Boolean {
        val engine = createForTests()
        repeat(3) { recordSuccess(engine, "yes") }
        val report = engine.generateReport()
        return report.phraseAnalytics.isNotEmpty() &&
            report.metrics.isNotEmpty() &&
            report.evidenceSummary.isNotBlank()
    }

    fun verifyTrendGeneration(): Boolean {
        val engine = createForTests()
        repeat(12) { i ->
            if (i < 6) recordBlocked(engine) else recordSuccess(engine, "yes")
        }
        val trends = TrendAnalyzer.analyze(engine.attempts())
        return trends.any { it.direction != TrendDirection.InsufficientData || trends.size > 1 }
    }

    fun verifyNoBehaviorChange(): Boolean {
        val ctx = CommunicationReliabilityContext(mappings = defaultLanguageMappings())
        val withoutAnalytics = CoreCommunicationReliabilityEngines.createForTests()
        val withAnalytics = CoreCommunicationReliabilityEngines.createForTests()
        val baseline = withoutAnalytics.evaluatePhrasePath(ctx, 2, 6)
        CommunicationAnalyticsBridge.attach(createForTests())
        val observed = withAnalytics.evaluatePhrasePath(ctx, 2, 6)
        CommunicationAnalyticsBridge.detach()
        return baseline.finalOutcome == observed.finalOutcome &&
            baseline.matchedPhraseId == observed.matchedPhraseId &&
            baseline.attemptResult.action == observed.attemptResult.action
    }

    private fun recordSuccess(engine: DefaultCommunicationAnalyticsEngine, phraseId: String) {
        engine.recordAttempt(
            sampleAttempt(
                phraseId = phraseId,
                outcome = CommunicationReliabilityOutcome.PASS,
                action = PhraseReliabilityAction.PROCEED_TO_CONFIRMATION,
                speechSuccess = true
            )
        )
    }

    private fun recordBlocked(engine: DefaultCommunicationAnalyticsEngine) {
        engine.recordAttempt(
            sampleAttempt(
                outcome = CommunicationReliabilityOutcome.BLOCKED,
                action = PhraseReliabilityAction.BLOCK,
                blockedReason = "No phrase matched"
            )
        )
    }

    private fun sampleAttempt(
        phraseId: String? = null,
        outcome: CommunicationReliabilityOutcome = CommunicationReliabilityOutcome.PASS,
        action: PhraseReliabilityAction = PhraseReliabilityAction.PROCEED_TO_CONFIRMATION,
        blockedReason: String? = null,
        duplicateBlocked: Boolean = false,
        speechSuccess: Boolean? = null
    ): CommunicationAttemptAnalytics = CommunicationAttemptAnalytics(
        attemptId = java.util.UUID.randomUUID().toString(),
        timestampMs = System.currentTimeMillis(),
        mode = com.idworx.lisa.features.corecommunicationreliability.model.CommunicationMode.MAIN,
        calibrationHealth = null,
        calibrationScore = null,
        phraseId = phraseId,
        phraseText = phraseId,
        confidenceScore = 0.9f,
        action = action,
        finalOutcome = outcome,
        blockedReason = blockedReason,
        emergency = false,
        emergencyTraining = false,
        navigationTraining = false,
        communicationTraining = false,
        practiceMode = false,
        speechSuccess = speechSuccess,
        duplicateBlocked = duplicateBlocked,
        calibrationBlocked = blockedReason?.contains("calibration", ignoreCase = true) == true,
        timing = AttemptTiming(firstBlinkMs = System.currentTimeMillis() - 500, speechCompleteMs = System.currentTimeMillis()),
        sequenceLabel = "L2 R6"
    )
}
