package com.idworx.lisa.features.brain1readiness.reporting

import com.idworx.lisa.features.brain1readiness.metadata.Brain1ReadinessMetadata
import com.idworx.lisa.features.brain1readiness.model.Brain1Evidence
import com.idworx.lisa.features.brain1readiness.model.Brain1Gap
import com.idworx.lisa.features.brain1readiness.model.Brain1ReadinessOutcome
import com.idworx.lisa.features.brain1readiness.model.Brain1ReadinessReport
import com.idworx.lisa.features.brain1readiness.model.Brain1ReadinessScore
import com.idworx.lisa.features.brain1readiness.model.Brain1Recommendation
import com.idworx.lisa.features.brain1readiness.model.Brain1Risk
import com.idworx.lisa.features.brain1readiness.model.Brain1RiskSeverity
import com.idworx.lisa.features.brain1readiness.model.Brain1ScoreBand
import com.idworx.lisa.features.brain1readiness.model.Brain1SubsystemReview
import com.idworx.lisa.features.brain1readiness.model.Brain1Subsystem
import com.idworx.lisa.features.brain1readiness.model.Brain1SubsystemStatus
import com.idworx.lisa.features.brain1readiness.reviewers.ReviewerResult
import java.util.UUID
import kotlin.math.roundToInt

object Brain1ReadinessReportGenerator {

    fun computeScore(results: List<ReviewerResult>, risks: List<Brain1Risk>): Brain1ReadinessScore {
        val totalChecks = results.sumOf { it.checksPerformed }
        val passedChecks = results.sumOf { it.checksPassed }
        val ready = results.count { it.status == Brain1SubsystemStatus.Ready }
        val base = if (totalChecks > 0) (passedChecks.toFloat() / totalChecks * 100f) else 0f
        val penalty = risks.count { it.severity == Brain1RiskSeverity.Critical } * 30 +
            risks.count { it.severity == Brain1RiskSeverity.High } * 10 +
            risks.count { it.severity == Brain1RiskSeverity.Medium } * 3 +
            results.count { it.status == Brain1SubsystemStatus.Blocked } * 20 +
            results.count { it.status == Brain1SubsystemStatus.Missing } * 15
        val overall = (base - penalty).roundToInt().coerceIn(0, 100)
        return Brain1ReadinessScore(
            overall = overall,
            band = Brain1ScoreBand.fromScore(overall),
            subsystemsReady = ready,
            subsystemsTotal = results.size,
            checksPassed = passedChecks,
            checksTotal = totalChecks,
            evidence = "$ready/${results.size} subsystems ready; $passedChecks/$totalChecks checks passed"
        )
    }

    fun determineOutcome(
        score: Brain1ReadinessScore,
        results: List<ReviewerResult>,
        risks: List<Brain1Risk>
    ): Brain1ReadinessOutcome {
        if (risks.any { it.severity == Brain1RiskSeverity.Critical }) return Brain1ReadinessOutcome.BLOCKED
        if (results.any { it.status == Brain1SubsystemStatus.Blocked }) return Brain1ReadinessOutcome.BLOCKED
        if (results.any { it.status == Brain1SubsystemStatus.Missing }) return Brain1ReadinessOutcome.NOT_READY
        if (score.overall < Brain1ReadinessMetadata.SCORE_NOT_READY) return Brain1ReadinessOutcome.BLOCKED
        if (score.overall < Brain1ReadinessMetadata.SCORE_READY_WITH_WARNINGS) return Brain1ReadinessOutcome.NOT_READY
        // Honest: device testing gaps mean READY_WITH_WARNINGS unless score is very high and no medium+ risks
        val hasMediumPlusRisk = risks.any { it.severity >= Brain1RiskSeverity.Medium }
        val hasDeviceGaps = results.any { it.subsystem == Brain1Subsystem.DeviceTesting && it.gaps.isNotEmpty() }
        return when {
            score.overall >= Brain1ReadinessMetadata.SCORE_READY &&
                !hasMediumPlusRisk &&
                results.all { it.status == Brain1SubsystemStatus.Ready } ->
                Brain1ReadinessOutcome.READY_FOR_DEVICE_TESTING
            score.overall >= Brain1ReadinessMetadata.SCORE_READY_WITH_WARNINGS ||
                hasDeviceGaps || hasMediumPlusRisk ->
                Brain1ReadinessOutcome.READY_WITH_WARNINGS
            else -> Brain1ReadinessOutcome.NOT_READY
        }
    }

    fun buildRecommendations(
        results: List<ReviewerResult>,
        risks: List<Brain1Risk>,
        gaps: List<Brain1Gap>
    ): List<Brain1Recommendation> {
        val recs = mutableListOf<Brain1Recommendation>()
        results.filter { it.status != Brain1SubsystemStatus.Ready }.forEach { r ->
            recs.add(
                Brain1Recommendation(
                    "REC_${r.subsystem.name}",
                    r.subsystem,
                    "Strengthen ${r.subsystem.name} before user testing: ${r.evidence}",
                    if (r.status == Brain1SubsystemStatus.Blocked) "Critical" else "High"
                )
            )
        }
        risks.filter { it.severity >= Brain1RiskSeverity.Medium }.forEach { risk ->
            recs.add(
                Brain1Recommendation(
                    "REC_${risk.riskId}",
                    risk.subsystem,
                    risk.description,
                    risk.severity.name
                )
            )
        }
        gaps.take(5).forEach { gap ->
            recs.add(
                Brain1Recommendation(
                    "REC_${gap.gapId}",
                    gap.subsystem,
                    gap.description,
                    if (gap.blocksReadiness) "Critical" else "Medium"
                )
            )
        }
        return recs.distinctBy { it.recommendationId }
    }

    fun generate(results: List<ReviewerResult>): Brain1ReadinessReport {
        val risks = results.flatMap { it.risks }.distinctBy { it.riskId }
        val gaps = results.flatMap { it.gaps }.distinctBy { it.gapId }
        val score = computeScore(results, risks)
        val outcome = determineOutcome(score, results, risks)
        val subsystemReviews = results.map { r ->
            Brain1SubsystemReview(
                subsystem = r.subsystem,
                status = r.status,
                reviewerName = r.reviewerName,
                checksPassed = r.checksPassed,
                checksPerformed = r.checksPerformed,
                evidence = r.evidence
            )
        }
        val evidence = results.map { r ->
            Brain1Evidence(
                "EV_${r.subsystem.name}",
                r.subsystem,
                r.evidence,
                r.reviewerName
            )
        }
        val recommendations = buildRecommendations(results, risks, gaps)
        val honestAssessment = buildHonestAssessment(outcome, gaps, risks)
        return Brain1ReadinessReport(
            reportId = UUID.randomUUID().toString(),
            generatedAtMs = System.currentTimeMillis(),
            outcome = outcome,
            score = score,
            subsystemReviews = subsystemReviews,
            risks = risks,
            gaps = gaps,
            recommendations = recommendations,
            evidence = evidence,
            summary = "Brain 1 readiness: ${outcome.name} (score ${score.overall}/${score.band.name})",
            honestAssessment = honestAssessment,
            evidenceSummary = score.evidence
        )
    }

    private fun buildHonestAssessment(
        outcome: Brain1ReadinessOutcome,
        gaps: List<Brain1Gap>,
        risks: List<Brain1Risk>
    ): String = when (outcome) {
        Brain1ReadinessOutcome.READY_FOR_DEVICE_TESTING ->
            "Phase 1 subsystems are integrated and validated. Ready for real Android device testing — not yet ready for patient use."
        Brain1ReadinessOutcome.READY_WITH_WARNINGS ->
            "Core Brain 1 communication path is protected, but ${gaps.size} testing gaps and ${risks.count { it.severity >= Brain1RiskSeverity.Medium }} medium+ risks remain before device testing."
        Brain1ReadinessOutcome.NOT_READY ->
            "Major Brain 1 integration or subsystem gaps remain. Address failed reviewers before device testing."
        Brain1ReadinessOutcome.BLOCKED ->
            "Critical safety, communication, offline, or calibration blockers detected. Do not proceed to device testing."
    }
}
