package com.idworx.lisa.features.offlinereliability.engine

import com.idworx.lisa.features.corecommunicationreliability.engine.CommunicationPathVerifier
import com.idworx.lisa.features.corecommunicationreliability.engine.CommunicationReliabilityContext
import com.idworx.lisa.features.corecommunicationreliability.engine.CoreCommunicationReliabilityEngines
import com.idworx.lisa.features.offlinereliability.model.OfflineScoreBand
import com.idworx.lisa.features.offlinereliability.validators.CommunicationOfflineValidator
import com.idworx.lisa.features.offlinereliability.validators.EyeTrackingOfflineValidator
import com.idworx.lisa.features.offlinereliability.validators.OfflineDependencyAuditor
import com.idworx.lisa.features.offlinereliability.validators.OfflineFileProbe
import com.idworx.lisa.features.offlinereliability.validators.TTSSpeechOfflineValidator
import com.idworx.lisa.defaultLanguageMappings

object OfflineCapabilityVerifier {

    fun verifyReportGeneration(): Boolean {
        val engine = OfflineReliabilityEngines.createForTests()
        val report = engine.generateReport()
        return report.summary.isNotBlank() &&
            report.evidenceSummary.isNotBlank() &&
            report.validationResults.isNotEmpty()
    }

    fun verifyOfflineCommunicationPath(): Boolean {
        val ctx = CommunicationReliabilityContext(mappings = defaultLanguageMappings())
        val engine = CoreCommunicationReliabilityEngines.createForTests()
        val result = engine.evaluatePhrasePath(ctx, 2, 6)
        return CommunicationPathVerifier.verifyPhraseMatchDeterministic() &&
            result.matchedPhraseId != null &&
            CommunicationOfflineValidator.validate().checksPassed >= 3
    }

    fun verifyOfflineScoreProduced(): Boolean {
        val report = OfflineReliabilityEngines.createForTests().generateReport()
        return report.score.overall in 0..100 &&
            report.score.band != OfflineScoreBand.Critical &&
            report.score.metrics.totalChecks > 0
    }

    fun verifyNoMandatoryCloudDependency(): Boolean =
        OfflineDependencyAuditor.detectMandatoryDependencies().isEmpty()

    fun verifyExistingTtsReused(): Boolean =
        OfflineFileProbe.fileExists("app/src/main/java/com/idworx/lisa/LisaTtsVoiceManager.kt") &&
            TTSSpeechOfflineValidator.validate().checksPassed >= 3

    fun verifyExistingEyeTrackingReused(): Boolean =
        OfflineFileProbe.fileExists("app/src/main/java/com/idworx/lisa/MainActivity.kt") &&
            EyeTrackingOfflineValidator.validate().checksPassed >= 3

    fun verifyExistingBlinkDetectionReused(): Boolean =
        OfflineFileProbe.fileExists(
            "app/src/main/java/com/idworx/lisa/features/corecommunicationreliability/sequence/BlinkSequenceValidator.kt"
        )

    fun verifyExistingCalibrationReused(): Boolean =
        OfflineFileProbe.fileExists(
            "app/src/main/java/com/idworx/lisa/features/calibrationreliability/engine/DefaultCalibrationReliabilityEngine.kt"
        )

    fun verifyExistingGuidedLearningReused(): Boolean =
        OfflineFileProbe.fileExists(
            "app/src/main/java/com/idworx/lisa/features/onboardingguide/services/TrainingProgressStore.kt"
        )

    fun verifyExistingPersonalityReused(): Boolean =
        OfflineFileProbe.fileExists(
            "app/src/main/java/com/idworx/lisa/features/personality/engine/LisaPersonalityEngine.kt"
        )

    fun verifyExistingCompanionMemoryReused(): Boolean =
        OfflineFileProbe.fileExists(
            "app/src/main/java/com/idworx/lisa/features/companionmemory/repository/CompanionMemoryRepository.kt"
        )

    fun verifyNoCommunicationBehaviorChange(): Boolean {
        val ctx = CommunicationReliabilityContext(mappings = defaultLanguageMappings())
        val baseline = CoreCommunicationReliabilityEngines.createForTests()
        val observed = CoreCommunicationReliabilityEngines.createForTests()
        val before = baseline.evaluatePhrasePath(ctx, 2, 6)
        OfflineReliabilityEngines.createForTests().generateReport()
        val after = observed.evaluatePhrasePath(ctx, 2, 6)
        return before.finalOutcome == after.finalOutcome &&
            before.matchedPhraseId == after.matchedPhraseId
    }
}
