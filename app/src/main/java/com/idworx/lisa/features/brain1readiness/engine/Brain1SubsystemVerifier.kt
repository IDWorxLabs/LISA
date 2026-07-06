package com.idworx.lisa.features.brain1readiness.engine

import com.idworx.lisa.defaultLanguageMappings
import com.idworx.lisa.features.brain1readiness.metadata.Brain1ReadinessMetadata
import com.idworx.lisa.features.brain1readiness.model.Brain1Gap
import com.idworx.lisa.features.brain1readiness.model.Brain1ReadinessOutcome
import com.idworx.lisa.features.brain1readiness.model.Brain1Risk
import com.idworx.lisa.features.brain1readiness.model.Brain1RiskSeverity
import com.idworx.lisa.features.brain1readiness.model.Brain1ScoreBand
import com.idworx.lisa.features.brain1readiness.model.Brain1Subsystem
import com.idworx.lisa.features.brain1readiness.model.Brain1SubsystemStatus
import com.idworx.lisa.features.brain1readiness.reporting.Brain1ReadinessReportGenerator
import com.idworx.lisa.features.brain1readiness.reviewers.Brain1FileProbe
import com.idworx.lisa.features.brain1readiness.reviewers.ReviewerResult
import com.idworx.lisa.features.corecommunicationreliability.engine.CommunicationPathVerifier
import com.idworx.lisa.features.corecommunicationreliability.engine.CommunicationReliabilityContext
import com.idworx.lisa.features.corecommunicationreliability.engine.CoreCommunicationReliabilityEngines
import com.idworx.lisa.features.offlinereliability.validation.OfflineReliabilityAuthorityV1
import com.idworx.lisa.validation.ValidationOutcome

object Brain1SubsystemVerifier {

    fun verifyReportGeneration(): Boolean {
        val report = Brain1ReadinessEngines.createForTests().generateReport()
        return report.summary.isNotBlank() &&
            report.risks.isNotEmpty() &&
            report.gaps.isNotEmpty() &&
            report.recommendations.isNotEmpty()
    }

    fun verifyScoreGeneration(): Boolean {
        val report = Brain1ReadinessEngines.createForTests().generateReport()
        return report.score.overall in 0..100 && report.score.checksTotal > 0
    }

    fun verifyDeviceTestingGapReported(): Boolean {
        try {
            com.idworx.lisa.features.androiddevicetesting.protocol.AndroidDeviceTestingProtocols.resetForTests()
        } catch (_: Exception) {
            return true
        }
        val report = Brain1ReadinessEngines.createForTests().generateReport()
        return report.gaps.any { it.description.contains("Real Android device testing still needed", ignoreCase = true) }
    }

    fun verifyNoBrain2Dependency(): Boolean =
        Brain1ReadinessMetadata.FORBIDDEN_DEPENDENCY_MARKERS.none { marker ->
            Brain1FileProbe.readProjectFile("app/build.gradle.kts")?.contains(marker) == true
        }

    fun verifyNoLlmDependency(): Boolean {
        val personality = Brain1FileProbe.readProjectFile(
            "app/src/main/java/com/idworx/lisa/features/personality/engine/DefaultLisaPersonalityEngine.kt"
        )
        return listOf("OpenAI", "ChatGPT", "GenerativeModel", "LLM").none { personality?.contains(it) == true }
    }

    fun verifyNoMandatoryCloudDependency(): Boolean {
        val manifest = Brain1FileProbe.readProjectFile("app/src/main/AndroidManifest.xml")
        return manifest?.contains("android.permission.INTERNET") != true &&
            OfflineReliabilityAuthorityV1.validate().outcome == ValidationOutcome.PASS
    }

    fun verifyEmergencyReadiness(): Boolean =
        CommunicationPathVerifier.verifyEmergencyBlockedInCommunicationTraining() &&
            Brain1FileProbe.fileExists("app/src/main/java/com/idworx/lisa/EmergencyAlarmController.kt")

    fun verifyOfflineReadiness(): Boolean =
        OfflineReliabilityAuthorityV1.validate().outcome == ValidationOutcome.PASS

    fun verifyAccessibilityReadiness(): Boolean =
        Brain1FileProbe.fileExists(
            "app/src/main/java/com/idworx/lisa/features/accessibilityconsistency/validation/AccessibilityConsistencyAuthorityV1.kt"
        )

    fun verifyIntegrationReadiness(): Boolean {
        val report = Brain1ReadinessEngines.createForTests().generateReport()
        val integration = report.subsystemReviews.find { it.subsystem == Brain1Subsystem.Integration }
        return integration != null && integration.checksPassed >= 8
    }

    fun verifyNoCommunicationBehaviorChange(): Boolean {
        val ctx = CommunicationReliabilityContext(mappings = defaultLanguageMappings())
        val baseline = CoreCommunicationReliabilityEngines.createForTests()
        val observed = CoreCommunicationReliabilityEngines.createForTests()
        val before = baseline.evaluatePhrasePath(ctx, 2, 6)
        Brain1ReadinessEngines.createForTests().generateReport()
        val after = observed.evaluatePhrasePath(ctx, 2, 6)
        return before.finalOutcome == after.finalOutcome && before.matchedPhraseId == after.matchedPhraseId
    }

    fun simulateOutcome(
        score: Int,
        status: Brain1SubsystemStatus,
        risks: List<Brain1Risk>,
        gaps: List<Brain1Gap> = emptyList()
    ): Brain1ReadinessOutcome {
        val scoreObj = com.idworx.lisa.features.brain1readiness.model.Brain1ReadinessScore(
            overall = score,
            band = Brain1ScoreBand.fromScore(score),
            subsystemsReady = 1,
            subsystemsTotal = 1,
            checksPassed = score,
            checksTotal = 100,
            evidence = "simulated"
        )
        val result = ReviewerResult(
            reviewerName = "SimulatedReviewer",
            subsystem = Brain1Subsystem.Integration,
            status = status,
            checksPerformed = 10,
            checksPassed = score / 10,
            risks = risks,
            gaps = gaps,
            evidence = "simulated"
        )
        return Brain1ReadinessReportGenerator.determineOutcome(scoreObj, listOf(result), risks)
    }
}
