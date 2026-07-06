package com.idworx.lisa.features.brain1readiness.reporting

import com.idworx.lisa.features.brain1readiness.model.Brain1ReadinessReport

object Brain1ReadinessSummary {

    fun format(report: Brain1ReadinessReport): String = buildString {
        appendLine("=== LISA Brain 1 Readiness Review ===")
        appendLine("Outcome: ${report.outcome.name}")
        appendLine("Score: ${report.score.overall} (${report.score.band.name})")
        appendLine("Subsystems ready: ${report.score.subsystemsReady}/${report.score.subsystemsTotal}")
        appendLine()
        appendLine("Honest assessment:")
        appendLine(report.honestAssessment)
        appendLine()
        appendLine("Subsystem status:")
        report.subsystemReviews.forEach { review ->
            appendLine("  ${review.subsystem.name}: ${review.status.name} (${review.checksPassed}/${review.checksPerformed})")
        }
        appendLine()
        appendLine("Risk register (${report.risks.size}):")
        report.risks.sortedByDescending { it.severity.ordinal }.take(10).forEach { risk ->
            appendLine("  [${risk.severity.name}] ${risk.description}")
        }
        appendLine()
        appendLine("Gap report (${report.gaps.size}):")
        report.gaps.take(10).forEach { gap ->
            appendLine("  - ${gap.description}")
        }
        appendLine()
        appendLine("Recommendations (${report.recommendations.size}):")
        report.recommendations.take(5).forEach { rec ->
            appendLine("  [${rec.priority}] ${rec.message}")
        }
        appendLine()
        appendLine(report.summary)
    }
}
