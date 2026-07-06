package com.idworx.lisa.features.offlinereliability.engine

import com.idworx.lisa.features.offlinereliability.diagnostics.OfflineDiagnostics
import com.idworx.lisa.features.offlinereliability.model.OfflineCapability
import com.idworx.lisa.features.offlinereliability.model.OfflineCapabilityStatus
import com.idworx.lisa.features.offlinereliability.model.OfflineRecommendation
import com.idworx.lisa.features.offlinereliability.model.OfflineReliabilityMetrics
import com.idworx.lisa.features.offlinereliability.model.OfflineReliabilityReport
import com.idworx.lisa.features.offlinereliability.model.OfflineReliabilityScore
import com.idworx.lisa.features.offlinereliability.model.OfflineScoreBand
import com.idworx.lisa.features.offlinereliability.model.OfflineValidationResult
import com.idworx.lisa.features.offlinereliability.model.OfflineWarning
import com.idworx.lisa.features.offlinereliability.validators.AccessibilityOfflineValidator
import com.idworx.lisa.features.offlinereliability.validators.BlinkDetectionOfflineValidator
import com.idworx.lisa.features.offlinereliability.validators.CalibrationOfflineValidator
import com.idworx.lisa.features.offlinereliability.validators.CommunicationOfflineValidator
import com.idworx.lisa.features.offlinereliability.validators.CompanionMemoryOfflineValidator
import com.idworx.lisa.features.offlinereliability.validators.EmergencyOfflineValidator
import com.idworx.lisa.features.offlinereliability.validators.EyeTrackingOfflineValidator
import com.idworx.lisa.features.offlinereliability.validators.GuidedLearningOfflineValidator
import com.idworx.lisa.features.offlinereliability.validators.OfflineDependencyAuditor
import com.idworx.lisa.features.offlinereliability.validators.PersonalityOfflineValidator
import com.idworx.lisa.features.offlinereliability.validators.SettingsOfflineValidator
import com.idworx.lisa.features.offlinereliability.validators.TTSSpeechOfflineValidator
import java.util.UUID
import kotlin.math.roundToInt

object OfflineCapabilityRunner {

    fun runAllValidators(): List<OfflineValidationResult> = listOf(
        EyeTrackingOfflineValidator.validate(),
        BlinkDetectionOfflineValidator.validate(),
        CommunicationOfflineValidator.validate(),
        CalibrationOfflineValidator.validate(),
        PersonalityOfflineValidator.validate(),
        CompanionMemoryOfflineValidator.validate(),
        GuidedLearningOfflineValidator.validate(),
        AccessibilityOfflineValidator.validate(),
        EmergencyOfflineValidator.validate(),
        SettingsOfflineValidator.validate(),
        TTSSpeechOfflineValidator.validate()
    )

    fun computeScore(results: List<OfflineValidationResult>, criticalDeps: Int): OfflineReliabilityScore {
        val totalChecks = results.sumOf { it.checksPerformed }
        val passedChecks = results.sumOf { it.checksPassed }
        val ready = results.count { it.status == OfflineCapabilityStatus.Ready }
        val warnings = results.sumOf { it.warnings.size }

        val base = if (totalChecks > 0) (passedChecks.toFloat() / totalChecks * 100f) else 0f
        val penalty = criticalDeps * 25 + warnings * 2 + results.count { it.status == OfflineCapabilityStatus.Unavailable } * 10
        val overall = (base - penalty).roundToInt().coerceIn(0, 100)

        val metrics = OfflineReliabilityMetrics(
            capabilitiesReady = ready,
            capabilitiesTotal = results.size,
            totalChecks = totalChecks,
            checksPassed = passedChecks,
            warnings = warnings,
            criticalDependencies = criticalDeps,
            evidence = "$ready/${results.size} capabilities ready; $passedChecks/$totalChecks checks passed"
        )

        return OfflineReliabilityScore(
            overall = overall,
            band = OfflineScoreBand.fromScore(overall),
            metrics = metrics,
            evidence = metrics.evidence
        )
    }

    fun buildRecommendations(results: List<OfflineValidationResult>, deps: List<String>): List<OfflineRecommendation> {
        val recs = mutableListOf<OfflineRecommendation>()
        results.filter { it.status != OfflineCapabilityStatus.Ready }.forEach { r ->
            recs.add(
                OfflineRecommendation(
                    "REC_${r.capability.name}",
                    r.capability,
                    "Verify ${r.capability.name} remains fully offline-capable",
                    r.evidence
                )
            )
        }
        deps.forEachIndexed { i, dep ->
            recs.add(
                OfflineRecommendation(
                    "REC_DEP_$i",
                    OfflineCapability.PhraseSpeech,
                    "Remove or make optional mandatory network dependency: $dep",
                    "Offline dependency audit"
                )
            )
        }
        return recs.distinctBy { it.recommendationId }
    }

    fun buildReport(results: List<OfflineValidationResult>): OfflineReliabilityReport {
        val deps = OfflineDependencyAuditor.detectMandatoryDependencies()
        val score = computeScore(results, deps.size)
        val warnings = results.flatMap { it.warnings }
        val capabilities = results.associate { it.capability to it.status }
        val recommendations = buildRecommendations(results, deps)
        val brain1Ready = deps.isEmpty() && results.all { it.status != OfflineCapabilityStatus.Unavailable }

        return OfflineReliabilityReport(
            reportId = UUID.randomUUID().toString(),
            generatedAtMs = System.currentTimeMillis(),
            score = score,
            validationResults = results,
            capabilities = capabilities,
            warnings = warnings,
            recommendations = recommendations,
            detectedDependencies = deps,
            summary = "Offline score ${score.overall} (${score.band.name}): ${score.metrics.capabilitiesReady}/${score.metrics.capabilitiesTotal} capabilities ready",
            brain1OfflineReady = brain1Ready,
            evidenceSummary = score.evidence
        )
    }
}

class DefaultOfflineReliabilityEngine : OfflineReliabilityEngine {

    private var lastReportInternal: OfflineReliabilityReport? = null

    override fun runValidation(): OfflineReliabilityReport {
        val report = OfflineCapabilityRunner.buildReport(OfflineCapabilityRunner.runAllValidators())
        lastReportInternal = report
        OfflineDiagnostics.record(report)
        return report
    }

    override fun generateReport(): OfflineReliabilityReport = runValidation()

    override fun lastReport(): OfflineReliabilityReport? = lastReportInternal

    override fun lastScore(): OfflineReliabilityScore? = lastReportInternal?.score
}

object OfflineReliabilityEngines {
    @Volatile
    private var instance: DefaultOfflineReliabilityEngine? = null

    val default: OfflineReliabilityEngine
        get() = instance ?: DefaultOfflineReliabilityEngine().also { instance = it }

    fun createForTests(): DefaultOfflineReliabilityEngine {
        val engine = DefaultOfflineReliabilityEngine()
        instance = engine
        return engine
    }

    fun resetForTests() {
        instance = null
        OfflineDiagnostics.clear()
    }
}
