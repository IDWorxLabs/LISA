package com.idworx.lisa.features.accessibilityconsistency.engine

import com.idworx.lisa.features.accessibilityconsistency.diagnostics.AccessibilityDiagnostics
import com.idworx.lisa.features.accessibilityconsistency.model.AccessibilityAudit
import com.idworx.lisa.features.accessibilityconsistency.model.AccessibilityCategory
import com.idworx.lisa.features.accessibilityconsistency.model.AccessibilityIssue
import com.idworx.lisa.features.accessibilityconsistency.model.AccessibilityMetrics
import com.idworx.lisa.features.accessibilityconsistency.model.AccessibilityRecommendation
import com.idworx.lisa.features.accessibilityconsistency.model.AccessibilityReport
import com.idworx.lisa.features.accessibilityconsistency.model.AccessibilityScore
import com.idworx.lisa.features.accessibilityconsistency.model.AccessibilityScoreBand
import com.idworx.lisa.features.accessibilityconsistency.model.AccessibilitySeverity
import com.idworx.lisa.features.accessibilityconsistency.validators.AccessibilitySettingsValidator
import com.idworx.lisa.features.accessibilityconsistency.validators.CognitiveLoadValidator
import com.idworx.lisa.features.accessibilityconsistency.validators.CommunicationValidator
import com.idworx.lisa.features.accessibilityconsistency.validators.ContrastValidator
import com.idworx.lisa.features.accessibilityconsistency.validators.EmergencyAccessibilityValidator
import com.idworx.lisa.features.accessibilityconsistency.validators.GuidedLearningValidator
import com.idworx.lisa.features.accessibilityconsistency.validators.LayoutValidator
import com.idworx.lisa.features.accessibilityconsistency.validators.NavigationValidator
import com.idworx.lisa.features.accessibilityconsistency.validators.ScreenConsistencyValidator
import com.idworx.lisa.features.accessibilityconsistency.validators.TouchTargetValidator
import com.idworx.lisa.features.accessibilityconsistency.validators.TypographyValidator
import com.idworx.lisa.features.accessibilityconsistency.validators.ValidatorResult
import com.idworx.lisa.features.accessibilityconsistency.integration.AccessibilityPersonalityAdapter
import java.util.UUID
import kotlin.math.roundToInt

object AccessibilityAuditRunner {

    fun runAllValidators(): List<ValidatorResult> = listOf(
        TypographyValidator.validate(),
        TouchTargetValidator.validate(),
        ContrastValidator.validate(),
        LayoutValidator.validate(),
        NavigationValidator.validate(),
        AccessibilitySettingsValidator.validate(),
        GuidedLearningValidator.validate(),
        CommunicationValidator.validate(),
        EmergencyAccessibilityValidator.validate(),
        ScreenConsistencyValidator.validate(),
        CognitiveLoadValidator.validate()
    )

    fun aggregateIssues(results: List<ValidatorResult>): List<AccessibilityIssue> =
        results.flatMap { it.issues }

    fun computeScore(results: List<ValidatorResult>): AccessibilityScore {
        val totalChecks = results.sumOf { it.checksPerformed }
        val passedChecks = results.sumOf { it.checksPassed }
        val issues = aggregateIssues(results)
        val critical = issues.count { it.severity == AccessibilitySeverity.Critical }
        val errors = issues.count { it.severity == AccessibilitySeverity.Error }
        val warnings = issues.count { it.severity == AccessibilitySeverity.Warning }

        val base = if (totalChecks > 0) (passedChecks.toFloat() / totalChecks * 100f) else 0f
        val penalty = critical * 20 + errors * 10 + warnings * 3
        val overall = (base - penalty).roundToInt().coerceIn(0, 100)

        val metrics = AccessibilityMetrics(
            typographyChecksPassed = results.find { it.validatorName == "TypographyValidator" }?.checksPassed ?: 0,
            touchTargetChecksPassed = results.find { it.validatorName == "TouchTargetValidator" }?.checksPassed ?: 0,
            navigationChecksPassed = results.find { it.validatorName == "NavigationValidator" }?.checksPassed ?: 0,
            totalChecks = totalChecks,
            totalIssues = issues.size,
            criticalIssues = critical + errors,
            evidence = "$passedChecks of $totalChecks checks passed; ${issues.size} issues observed"
        )

        return AccessibilityScore(
            overall = overall,
            band = AccessibilityScoreBand.fromScore(overall),
            metrics = metrics,
            evidence = metrics.evidence
        )
    }

    fun buildRecommendations(issues: List<AccessibilityIssue>): List<AccessibilityRecommendation> =
        issues.mapNotNull { issue ->
            when (issue.category) {
                AccessibilityCategory.Typography ->
                    AccessibilityRecommendation("REC_Typography", issue.category, "Review text scaling and readable labels on ${issue.screen}", issue.evidence)
                AccessibilityCategory.TouchTarget ->
                    AccessibilityRecommendation("REC_Touch", issue.category, "Ensure interactive controls meet minimum touch size on ${issue.screen}", issue.evidence)
                AccessibilityCategory.CognitiveLoad ->
                    AccessibilityRecommendation("REC_Cog", issue.category, "Consider simplifying ${issue.screen} to reduce cognitive load", issue.evidence)
                AccessibilityCategory.Settings ->
                    AccessibilityRecommendation("REC_Settings", issue.category, "Verify accessibility settings remain integrated", issue.evidence)
                else -> if (issue.severity >= AccessibilitySeverity.Error) {
                    AccessibilityRecommendation("REC_${issue.issueId}", issue.category, issue.description, issue.evidence)
                } else null
            }
        }.distinctBy { it.recommendationId }

    fun buildReport(results: List<ValidatorResult>): AccessibilityReport {
        val issues = aggregateIssues(results)
        val score = computeScore(results)
        val audit = AccessibilityAudit(
            auditId = UUID.randomUUID().toString(),
            timestampMs = System.currentTimeMillis(),
            issues = issues,
            score = score,
            checksPerformed = results.sumOf { it.checksPerformed },
            checksPassed = results.sumOf { it.checksPassed }
        )
        val recommendations = buildRecommendations(issues)
        val warnings = issues.filter { it.severity == AccessibilitySeverity.Warning }.map { it.description }
        return AccessibilityReport(
            reportId = UUID.randomUUID().toString(),
            generatedAtMs = System.currentTimeMillis(),
            audit = audit,
            score = score,
            issues = issues,
            recommendations = recommendations,
            warnings = warnings,
            summary = "Accessibility score ${score.overall} (${score.band.name}): ${audit.checksPassed}/${audit.checksPerformed} checks passed",
            affectedScreens = issues.map { it.screen }.distinct(),
            evidenceSummary = score.evidence
        )
    }
}

class DefaultAccessibilityConsistencyEngine : AccessibilityConsistencyEngine {

    private var lastReportInternal: AccessibilityReport? = null

    override fun runAudit(): AccessibilityAudit {
        val results = AccessibilityAuditRunner.runAllValidators()
        val report = AccessibilityAuditRunner.buildReport(results)
        lastReportInternal = report
        AccessibilityDiagnostics.record(report)
        return report.audit
    }

    override fun generateReport(): AccessibilityReport {
        val report = AccessibilityAuditRunner.buildReport(AccessibilityAuditRunner.runAllValidators())
        lastReportInternal = report
        AccessibilityDiagnostics.record(report)
        return report
    }

    override fun lastReport(): AccessibilityReport? = lastReportInternal

    override fun lastScore(): AccessibilityScore? = lastReportInternal?.score

    override fun guidanceMessage(): String =
        AccessibilityPersonalityAdapter.guidanceForScore(lastReportInternal?.score)
}

object AccessibilityConsistencyEngines {
    @Volatile
    private var instance: DefaultAccessibilityConsistencyEngine? = null

    val default: AccessibilityConsistencyEngine
        get() = instance ?: DefaultAccessibilityConsistencyEngine().also { instance = it }

    fun createForTests(): DefaultAccessibilityConsistencyEngine {
        val engine = DefaultAccessibilityConsistencyEngine()
        instance = engine
        return engine
    }

    fun resetForTests() {
        instance = null
        AccessibilityDiagnostics.clear()
    }
}
