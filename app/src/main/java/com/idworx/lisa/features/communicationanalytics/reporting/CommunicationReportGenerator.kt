package com.idworx.lisa.features.communicationanalytics.reporting

import com.idworx.lisa.features.communicationanalytics.metadata.CommunicationAnalyticsMetadata
import com.idworx.lisa.features.communicationanalytics.metrics.AccuracyCalculator
import com.idworx.lisa.features.communicationanalytics.metrics.CalibrationImpactCalculator
import com.idworx.lisa.features.communicationanalytics.metrics.EmergencyAnalyticsCalculator
import com.idworx.lisa.features.communicationanalytics.metrics.FalseNegativeCalculator
import com.idworx.lisa.features.communicationanalytics.metrics.FalsePositiveCalculator
import com.idworx.lisa.features.communicationanalytics.metrics.NavigationAnalyticsCalculator
import com.idworx.lisa.features.communicationanalytics.metrics.PhraseTimingCalculator
import com.idworx.lisa.features.communicationanalytics.metrics.RetryRateCalculator
import com.idworx.lisa.features.communicationanalytics.metrics.SuccessRateCalculator
import com.idworx.lisa.features.communicationanalytics.model.AnalyticsSummary
import com.idworx.lisa.features.communicationanalytics.model.CommunicationAttemptAnalytics
import com.idworx.lisa.features.communicationanalytics.model.CommunicationMetric
import com.idworx.lisa.features.communicationanalytics.model.CommunicationTrend
import com.idworx.lisa.features.communicationanalytics.model.PhraseAnalytics
import com.idworx.lisa.features.communicationanalytics.model.TrendDirection

object TrendAnalyzer {

    fun analyze(attempts: List<CommunicationAttemptAnalytics>): List<CommunicationTrend> {
        if (attempts.size < CommunicationAnalyticsMetadata.TREND_MIN_SAMPLES * 2) {
            return listOf(
                CommunicationTrend(
                    metricName = "communication_success",
                    direction = TrendDirection.InsufficientData,
                    currentValue = 0f,
                    priorValue = null,
                    evidence = "Need at least ${CommunicationAnalyticsMetadata.TREND_MIN_SAMPLES * 2} attempts for trends"
                )
            )
        }
        val midpoint = attempts.size / 2
        val prior = attempts.take(midpoint)
        val recent = attempts.drop(midpoint)
        return listOf(
            trend("communication_success", SuccessRateCalculator.overall(recent).rate, SuccessRateCalculator.overall(prior).rate),
            trend("retry_frequency", RetryRateCalculator.averageRetries(recent), RetryRateCalculator.averageRetries(prior), lowerIsBetter = true),
            trend("accuracy", AccuracyCalculator.accuracyRate(recent), AccuracyCalculator.accuracyRate(prior)),
            trend("false_positive_rate", FalsePositiveCalculator.rate(recent), FalsePositiveCalculator.rate(prior), lowerIsBetter = true),
            trend("speech_latency_ms", PhraseTimingCalculator.averageCompletionTimeMs(recent)?.toFloat() ?: 0f,
                PhraseTimingCalculator.averageCompletionTimeMs(prior)?.toFloat(), lowerIsBetter = true)
        )
    }

    private fun trend(
        name: String,
        current: Float,
        prior: Float?,
        lowerIsBetter: Boolean = false
    ): CommunicationTrend {
        val direction = when {
            prior == null -> TrendDirection.InsufficientData
            kotlin.math.abs(current - prior) < 0.02f -> TrendDirection.Stable
            current > prior -> if (lowerIsBetter) TrendDirection.Declining else TrendDirection.Improving
            else -> if (lowerIsBetter) TrendDirection.Improving else TrendDirection.Declining
        }
        return CommunicationTrend(
            metricName = name,
            direction = direction,
            currentValue = current,
            priorValue = prior,
            evidence = "Compared recent ${current.format()} vs prior ${prior?.format() ?: "n/a"}"
        )
    }

    private fun Float.format(): String = "%.3f".format(this)
}

object ReliabilitySummary {

    fun build(attempts: List<CommunicationAttemptAnalytics>): AnalyticsSummary {
        val overall = SuccessRateCalculator.overall(attempts)
        return AnalyticsSummary(
            totalAttempts = attempts.size,
            successfulCommunications = overall.successful,
            blockedAttempts = attempts.count {
                it.finalOutcome == com.idworx.lisa.features.corecommunicationreliability.model.CommunicationReliabilityOutcome.BLOCKED
            },
            failedSpeech = attempts.count { it.speechSuccess == false },
            overallSuccessRate = overall,
            falsePositiveRate = FalsePositiveCalculator.rate(attempts),
            falseNegativeRate = FalseNegativeCalculator.rate(attempts),
            averageRetries = RetryRateCalculator.averageRetries(attempts),
            averageCompletionTimeMs = PhraseTimingCalculator.averageCompletionTimeMs(attempts),
            evidence = "Summary from ${attempts.size} observable communication attempts"
        )
    }
}

object CommunicationReportGenerator {

    fun generate(attempts: List<CommunicationAttemptAnalytics>): com.idworx.lisa.features.communicationanalytics.model.CommunicationAnalyticsReport {
        val summary = ReliabilitySummary.build(attempts)
        val phraseStats = phraseAnalytics(attempts)
        val calibrationImpact = CalibrationImpactCalculator.analyze(attempts)
        val emergency = EmergencyAnalyticsCalculator.analyze(attempts)
        val navigation = NavigationAnalyticsCalculator.analyze(attempts)
        val trends = TrendAnalyzer.analyze(attempts)
        val (recommendations, warnings) = deriveRecommendationsAndWarnings(summary, trends, calibrationImpact)
        val metrics = buildMetrics(attempts, summary)
        return com.idworx.lisa.features.communicationanalytics.model.CommunicationAnalyticsReport(
            reportId = java.util.UUID.randomUUID().toString(),
            generatedAtMs = System.currentTimeMillis(),
            summary = summary,
            phraseAnalytics = phraseStats,
            calibrationImpact = calibrationImpact,
            emergencyAnalytics = emergency,
            navigationAnalytics = navigation,
            trends = trends,
            recommendations = recommendations,
            warnings = warnings,
            metrics = metrics,
            evidenceSummary = "Report from ${attempts.size} observable attempts at ${System.currentTimeMillis()}"
        )
    }

    private fun phraseAnalytics(attempts: List<CommunicationAttemptAnalytics>): List<PhraseAnalytics> =
        attempts.filter { it.phraseId != null }
            .groupBy { it.phraseId!! }
            .map { (id, list) ->
                val text = list.firstNotNullOfOrNull { it.phraseText }
                val times = list.mapNotNull { it.timing.blinkToSpeechMs }
                PhraseAnalytics(
                    phraseId = id,
                    phraseText = text,
                    attemptCount = list.size,
                    successCount = list.count { SuccessRateCalculator.phraseSuccessRate(listOf(it)) > 0f },
                    blockedCount = list.count {
                        it.finalOutcome == com.idworx.lisa.features.corecommunicationreliability.model.CommunicationReliabilityOutcome.BLOCKED
                    },
                    retryCount = list.sumOf { it.retryCount },
                    successRate = SuccessRateCalculator.phraseSuccessRate(list),
                    averageCompletionTimeMs = if (times.isNotEmpty()) times.average().toLong() else null,
                    fastestCompletionMs = times.minOrNull(),
                    slowestCompletionMs = times.maxOrNull(),
                    evidence = "$id: ${list.size} attempts observed"
                )
            }
            .sortedByDescending { it.attemptCount }

    private fun deriveRecommendationsAndWarnings(
        summary: AnalyticsSummary,
        trends: List<CommunicationTrend>,
        calibrationImpact: com.idworx.lisa.features.communicationanalytics.model.CalibrationImpact
    ): Pair<List<String>, List<String>> {
        val recommendations = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        if (summary.overallSuccessRate.rate < 0.6f && summary.totalAttempts >= 5) {
            recommendations.add("Observed success rate below 60% — review phrase difficulty and calibration quality")
        }
        if (summary.falsePositiveRate > 0.1f) {
            warnings.add("False positive signals above 10% of attempts")
        }
        if (summary.falseNegativeRate > 0.1f) {
            warnings.add("False negative signals above 10% of attempts")
        }
        if (calibrationImpact.blockedByCalibrationState > 0) {
            recommendations.add("Observed ${calibrationImpact.blockedByCalibrationState} attempts blocked by calibration state")
        }
        trends.filter { it.direction == TrendDirection.Declining && it.metricName == "communication_success" }
            .forEach { warnings.add("Communication success trend declining") }
        trends.filter { it.direction == TrendDirection.Improving && it.metricName == "retry_frequency" }
            .forEach { recommendations.add("Retry frequency decreasing — communication may be stabilising") }
        return recommendations to warnings
    }

    private fun buildMetrics(
        attempts: List<CommunicationAttemptAnalytics>,
        summary: AnalyticsSummary
    ): List<CommunicationMetric> = listOf(
        CommunicationMetric("success_rate", summary.overallSuccessRate.rate, evidence = summary.overallSuccessRate.evidence),
        CommunicationMetric("false_positive_rate", summary.falsePositiveRate, evidence = "Duplicate/blocked emergency signals"),
        CommunicationMetric("false_negative_rate", summary.falseNegativeRate, evidence = "Blocked valid phrases or failed speech"),
        CommunicationMetric("average_retries", summary.averageRetries, evidence = "Mean retry count per attempt"),
        CommunicationMetric("accuracy_rate", AccuracyCalculator.accuracyRate(attempts), evidence = "Pass or speech success rate"),
        CommunicationMetric("blocked_rate", AccuracyCalculator.blockedRate(attempts), evidence = "Blocked attempt proportion")
    )
}
