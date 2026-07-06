package com.idworx.lisa.features.calibrationreliability.scoring

import com.idworx.lisa.features.calibrationreliability.metadata.CalibrationReliabilityMetadata
import com.idworx.lisa.features.calibrationreliability.model.CalibrationMetric
import com.idworx.lisa.features.calibrationreliability.model.CalibrationQualityCategory
import com.idworx.lisa.features.calibrationreliability.model.CalibrationScore
import com.idworx.lisa.features.calibrationreliability.model.CalibrationSession
import com.idworx.lisa.features.calibrationreliability.model.RepeatabilityScore
import com.idworx.lisa.features.calibrationreliability.model.StabilityScore
import kotlin.math.roundToInt

object StabilityScorer {

    fun score(session: CalibrationSession): StabilityScore {
        val eventCount = session.stabilityEvents +
            session.gazeDeviationEvents +
            session.incompleteFixations +
            session.pauses
        val penalty = eventCount * CalibrationReliabilityMetadata.STABILITY_EVENT_PENALTY
        val value = (100 - penalty).coerceIn(0, 100)
        return StabilityScore(
            value = value,
            stabilityEvents = session.stabilityEvents,
            gazeDeviationEvents = session.gazeDeviationEvents,
            incompleteFixations = session.incompleteFixations,
            pauses = session.pauses
        )
    }
}

object RepeatabilityScorer {

    fun score(session: CalibrationSession, priorOverallScore: Int?): RepeatabilityScore {
        if (priorOverallScore == null) {
            return RepeatabilityScore(
                value = 70,
                priorSessionScore = null,
                variance = 0,
                improved = false,
                regressed = false
            )
        }
        val provisional = scoreSessionComponents(session, priorOverallScore)
        val variance = kotlin.math.abs(provisional - priorOverallScore)
        val improved = provisional > priorOverallScore + 5
        val regressed = provisional < priorOverallScore - 10
        val value = when {
            variance <= 5 -> 95
            variance <= 10 -> 85
            variance <= 20 -> 70
            variance <= 30 -> 55
            else -> 40
        }.let { base ->
            when {
                improved -> (base + 5).coerceAtMost(100)
                regressed -> (base - 10).coerceAtLeast(0)
                else -> base
            }
        }
        return RepeatabilityScore(
            value = value,
            priorSessionScore = priorOverallScore,
            variance = variance,
            improved = improved,
            regressed = regressed
        )
    }

    private fun scoreSessionComponents(session: CalibrationSession, @Suppress("UNUSED_PARAMETER") prior: Int): Int {
        val completeness = (session.completionRatio * 100).roundToInt()
        val consistency = (session.sampleAcceptanceRatio * 100).roundToInt()
        return ((completeness + consistency) / 2).coerceIn(0, 100)
    }
}

object CalibrationScorer {

    fun score(session: CalibrationSession, priorOverallScore: Int? = null): CalibrationScore {
        val stability = StabilityScorer.score(session)
        val repeatability = RepeatabilityScorer.score(session, priorOverallScore)

        val sampleCompleteness = scoreCompleteness(session)
        val sampleConsistency = scoreConsistency(session)
        val coverage = scoreCoverage(session)
        val trackingContinuity = scoreTrackingContinuity(session)

        val overall = weightedAverage(
            sampleCompleteness to 25,
            stability.value to 20,
            sampleConsistency to 20,
            repeatability.value to 15,
            coverage to 10,
            trackingContinuity to 10
        )

        return CalibrationScore(
            overall = overall,
            category = CalibrationQualityCategory.fromScore(overall),
            sampleCompleteness = sampleCompleteness,
            eyeStability = stability.value,
            sampleConsistency = sampleConsistency,
            repeatability = repeatability.value,
            coverage = coverage,
            trackingContinuity = trackingContinuity,
            stabilityScore = stability,
            repeatabilityScore = repeatability
        )
    }

    fun metrics(session: CalibrationSession, score: CalibrationScore): List<CalibrationMetric> = listOf(
        CalibrationMetric("overall_score", score.overall.toFloat(), evidence = "Weighted observable calibration score"),
        CalibrationMetric("points_completed", session.pointsCompleted.toFloat(), evidence = "${session.pointsCompleted}/${session.totalPoints} points"),
        CalibrationMetric("successful_samples", session.successfulSamples.toFloat(), evidence = "Accepted calibration samples"),
        CalibrationMetric("rejected_samples", session.rejectedSamples.toFloat(), evidence = "Rejected calibration samples"),
        CalibrationMetric("stability_events", session.stabilityEvents.toFloat(), evidence = "Unexpected movement during sampling"),
        CalibrationMetric("tracking_gaps", session.trackingGaps.toFloat(), evidence = "Tracking continuity interruptions"),
        CalibrationMetric("completion_ratio", session.completionRatio, evidence = "Point completion ratio"),
        CalibrationMetric("acceptance_ratio", session.sampleAcceptanceRatio, evidence = "Sample acceptance ratio")
    )

    private fun scoreCompleteness(session: CalibrationSession): Int {
        val pointScore = (session.completionRatio * 100).roundToInt()
        val skipPenalty = session.pointsSkipped * 8
        return (pointScore - skipPenalty).coerceIn(0, 100)
    }

    private fun scoreConsistency(session: CalibrationSession): Int {
        if (session.totalSamples == 0) return 0
        val base = (session.sampleAcceptanceRatio * 100).roundToInt()
        val rejectPenalty = session.rejectedSamples * CalibrationReliabilityMetadata.REJECT_SAMPLE_PENALTY
        return (base - rejectPenalty).coerceIn(0, 100)
    }

    private fun scoreCoverage(session: CalibrationSession): Int {
        val completed = session.pointsCompleted
        val skipped = session.pointsSkipped
        if (completed + skipped == 0) return 0
        val effective = completed.toFloat() / (completed + skipped)
        return (effective * 100).roundToInt().coerceIn(0, 100)
    }

    private fun scoreTrackingContinuity(session: CalibrationSession): Int {
        val penalty = session.trackingGaps * CalibrationReliabilityMetadata.TRACKING_GAP_PENALTY +
            session.interruptions * CalibrationReliabilityMetadata.INTERRUPTION_PENALTY
        return (100 - penalty).coerceIn(0, 100)
    }

    private fun weightedAverage(vararg components: Pair<Int, Int>): Int {
        val totalWeight = components.sumOf { it.second }
        if (totalWeight == 0) return 0
        val weighted = components.sumOf { it.first * it.second }
        return (weighted.toFloat() / totalWeight).roundToInt().coerceIn(0, 100)
    }
}
