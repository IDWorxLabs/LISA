package com.idworx.lisa.features.communicationanalytics.metrics

import com.idworx.lisa.features.communicationanalytics.model.CommunicationAttemptAnalytics
import com.idworx.lisa.features.communicationanalytics.model.SuccessRate
import com.idworx.lisa.features.corecommunicationreliability.model.CommunicationReliabilityOutcome
import com.idworx.lisa.features.corecommunicationreliability.model.PhraseReliabilityAction

object SuccessRateCalculator {

    fun overall(attempts: List<CommunicationAttemptAnalytics>): SuccessRate {
        if (attempts.isEmpty()) {
            return SuccessRate(0f, 0, 0, "No communication attempts recorded")
        }
        val successful = attempts.count { isSuccessfulCommunication(it) }
        val rate = successful.toFloat() / attempts.size
        return SuccessRate(
            rate = rate,
            successful = successful,
            total = attempts.size,
            evidence = "$successful successful of ${attempts.size} attempts"
        )
    }

    fun forMode(
        attempts: List<CommunicationAttemptAnalytics>,
        predicate: (CommunicationAttemptAnalytics) -> Boolean
    ): SuccessRate {
        val filtered = attempts.filter(predicate)
        return overall(filtered)
    }

    fun phraseSuccessRate(phraseAttempts: List<CommunicationAttemptAnalytics>): Float {
        if (phraseAttempts.isEmpty()) return 0f
        return phraseAttempts.count { isSuccessfulCommunication(it) }.toFloat() / phraseAttempts.size
    }

    private fun isSuccessfulCommunication(attempt: CommunicationAttemptAnalytics): Boolean =
        attempt.speechSuccess == true ||
            (attempt.finalOutcome == CommunicationReliabilityOutcome.PASS &&
                attempt.action in setOf(
                    PhraseReliabilityAction.PROCEED_TO_CONFIRMATION,
                    PhraseReliabilityAction.PROCEED_IMMEDIATE,
                    PhraseReliabilityAction.ROUTE_EMERGENCY
                ) &&
                attempt.speechSuccess != false)
}

object RetryRateCalculator {

    fun averageRetries(attempts: List<CommunicationAttemptAnalytics>): Float {
        if (attempts.isEmpty()) return 0f
        return attempts.map { it.retryCount }.average().toFloat()
    }

    fun maxRetries(attempts: List<CommunicationAttemptAnalytics>): Int =
        attempts.maxOfOrNull { it.retryCount } ?: 0

    fun retriesPerPhrase(attempts: List<CommunicationAttemptAnalytics>): Map<String, Int> =
        attempts.filter { it.phraseId != null }
            .groupBy { it.phraseId!! }
            .mapValues { (_, list) -> list.sumOf { it.retryCount } }

    fun retriesBeforeSuccess(attempts: List<CommunicationAttemptAnalytics>): Float {
        val successPhrases = attempts.filter { SuccessRateCalculator.phraseSuccessRate(listOf(it)) > 0f }
        if (successPhrases.isEmpty()) return 0f
        return successPhrases.map { it.retryCount }.average().toFloat()
    }
}

object AccuracyCalculator {

    fun accuracyRate(attempts: List<CommunicationAttemptAnalytics>): Float {
        if (attempts.isEmpty()) return 0f
        val correct = attempts.count {
            it.finalOutcome == CommunicationReliabilityOutcome.PASS ||
                it.speechSuccess == true
        }
        return correct.toFloat() / attempts.size
    }

    fun blockedRate(attempts: List<CommunicationAttemptAnalytics>): Float {
        if (attempts.isEmpty()) return 0f
        return attempts.count { it.finalOutcome == CommunicationReliabilityOutcome.BLOCKED }.toFloat() / attempts.size
    }

    fun lowConfidenceRate(attempts: List<CommunicationAttemptAnalytics>): Float {
        if (attempts.isEmpty()) return 0f
        return attempts.count {
            it.confidenceScore < com.idworx.lisa.features.communicationanalytics.metadata.CommunicationAnalyticsMetadata.LOW_CONFIDENCE_THRESHOLD
        }.toFloat() / attempts.size
    }
}

object FalsePositiveCalculator {

    fun count(attempts: List<CommunicationAttemptAnalytics>): Int =
        attempts.count { isFalsePositiveSignal(it) }

    fun rate(attempts: List<CommunicationAttemptAnalytics>): Float {
        if (attempts.isEmpty()) return 0f
        return count(attempts).toFloat() / attempts.size
    }

    fun isFalsePositiveSignal(attempt: CommunicationAttemptAnalytics): Boolean {
        val reason = attempt.blockedReason?.lowercase() ?: ""
        return attempt.duplicateBlocked ||
            reason.contains("duplicate") ||
            (attempt.emergency && attempt.finalOutcome == CommunicationReliabilityOutcome.BLOCKED) ||
            (attempt.speechSuccess == true && attempt.confidenceScore < 0.45f)
    }
}

object FalseNegativeCalculator {

    fun count(attempts: List<CommunicationAttemptAnalytics>): Int =
        attempts.count { isFalseNegativeSignal(it) }

    fun rate(attempts: List<CommunicationAttemptAnalytics>): Float {
        if (attempts.isEmpty()) return 0f
        return count(attempts).toFloat() / attempts.size
    }

    fun isFalseNegativeSignal(attempt: CommunicationAttemptAnalytics): Boolean {
        val reason = attempt.blockedReason?.lowercase() ?: ""
        return (attempt.phraseId != null && attempt.finalOutcome == CommunicationReliabilityOutcome.BLOCKED &&
            !attempt.calibrationBlocked && !attempt.duplicateBlocked) ||
            (attempt.calibrationBlocked && attempt.phraseId != null) ||
            (attempt.action == PhraseReliabilityAction.PROCEED_TO_CONFIRMATION && attempt.speechSuccess == false) ||
            reason.contains("confidence below") && attempt.phraseId != null
    }
}

object PhraseTimingCalculator {

    fun averageCompletionTimeMs(attempts: List<CommunicationAttemptAnalytics>): Long? {
        val times = attempts.mapNotNull { it.timing.blinkToSpeechMs }
        if (times.isEmpty()) return null
        return times.average().toLong()
    }

    fun averageSequenceToMatchMs(attempts: List<CommunicationAttemptAnalytics>): Long? {
        val times = attempts.mapNotNull { it.timing.sequenceToMatchMs }
        if (times.isEmpty()) return null
        return times.average().toLong()
    }

    fun timingDistribution(attempts: List<CommunicationAttemptAnalytics>): Map<String, Long> {
        val completions = attempts.mapNotNull { it.timing.blinkToSpeechMs }
        if (completions.isEmpty()) return emptyMap()
        val sorted = completions.sorted()
        return mapOf(
            "p50_ms" to sorted[sorted.size / 2],
            "p90_ms" to sorted[(sorted.size * 0.9).toInt().coerceAtMost(sorted.lastIndex)],
            "min_ms" to sorted.first(),
            "max_ms" to sorted.last()
        )
    }
}

object CalibrationImpactCalculator {

    fun analyze(attempts: List<CommunicationAttemptAnalytics>): com.idworx.lisa.features.communicationanalytics.model.CalibrationImpact {
        val byHealth = attempts.groupBy { it.calibrationHealth?.name ?: "unknown" }
            .mapValues { it.value.size }
        val byScoreBand = attempts.groupBy { scoreBand(it.calibrationScore) }
        val successByBand = byScoreBand.mapValues { (_, list) ->
            SuccessRateCalculator.overall(list).rate
        }
        val retryByBand = byScoreBand.mapValues { (_, list) ->
            RetryRateCalculator.averageRetries(list)
        }
        val blockedByCalibration = attempts.count { it.calibrationBlocked }
        return com.idworx.lisa.features.communicationanalytics.model.CalibrationImpact(
            attemptsByHealthState = byHealth,
            successRateByCalibrationScoreBand = successByBand,
            retryRateByCalibrationQuality = retryByBand,
            blockedByCalibrationState = blockedByCalibration,
            communicationAfterRecalibration = attempts.count {
                it.calibrationHealth?.name == "Healthy" && it.speechSuccess == true
            },
            evidence = "Observed ${attempts.size} attempts correlated with calibration health states"
        )
    }

    private fun scoreBand(score: Int?): String = when {
        score == null -> "unknown"
        score >= 90 -> "excellent"
        score >= 75 -> "good"
        score >= 60 -> "acceptable"
        score >= 40 -> "poor"
        else -> "failed"
    }
}

object NavigationAnalyticsCalculator {

    fun analyze(attempts: List<CommunicationAttemptAnalytics>): com.idworx.lisa.features.communicationanalytics.model.NavigationAnalytics {
        val navAttempts = attempts.filter { it.navigationTraining || it.sequenceLabel.contains("nav", ignoreCase = true) }
        val successful = navAttempts.count { it.finalOutcome == CommunicationReliabilityOutcome.PASS }
        val conflicts = navAttempts.count {
            it.blockedReason?.contains("ambiguous", ignoreCase = true) == true
        }
        return com.idworx.lisa.features.communicationanalytics.model.NavigationAnalytics(
            gestureAttempts = navAttempts.size,
            successfulGestures = successful,
            retries = navAttempts.sumOf { it.retryCount },
            conflicts = conflicts,
            falsePositiveSignals = navAttempts.count { FalsePositiveCalculator.isFalsePositiveSignal(it) },
            averageCompletionTimeMs = PhraseTimingCalculator.averageCompletionTimeMs(navAttempts),
            evidence = "Observed ${navAttempts.size} navigation-related attempts"
        )
    }
}

object EmergencyAnalyticsCalculator {

    fun analyze(attempts: List<CommunicationAttemptAnalytics>): com.idworx.lisa.features.communicationanalytics.model.EmergencyAnalytics {
        val emergencyAttempts = attempts.filter { it.emergency || it.emergencyTraining }
        return com.idworx.lisa.features.communicationanalytics.model.EmergencyAnalytics(
            activationAttempts = emergencyAttempts.count { it.emergency && !it.emergencyTraining },
            trainingActivations = emergencyAttempts.count { it.emergencyTraining },
            blockedActivations = emergencyAttempts.count {
                it.finalOutcome == CommunicationReliabilityOutcome.BLOCKED
            },
            duplicatePreventions = emergencyAttempts.count { it.duplicateBlocked },
            confirmationSuccesses = emergencyAttempts.count {
                it.action == PhraseReliabilityAction.ROUTE_EMERGENCY &&
                    it.finalOutcome != CommunicationReliabilityOutcome.BLOCKED
            },
            falsePositiveSignals = emergencyAttempts.count { FalsePositiveCalculator.isFalsePositiveSignal(it) },
            falseNegativeSignals = emergencyAttempts.count { FalseNegativeCalculator.isFalseNegativeSignal(it) },
            safetyInterventions = emergencyAttempts.count {
                it.finalOutcome == CommunicationReliabilityOutcome.BLOCKED &&
                    it.blockedReason != null
            },
            evidence = "Observed ${emergencyAttempts.size} emergency-related attempts"
        )
    }
}
