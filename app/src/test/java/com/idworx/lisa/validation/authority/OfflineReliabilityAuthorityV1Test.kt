package com.idworx.lisa.validation.authority

import com.idworx.lisa.features.offlinereliability.diagnostics.OfflineDiagnostics
import com.idworx.lisa.features.offlinereliability.engine.OfflineCapabilityRunner
import com.idworx.lisa.features.offlinereliability.engine.OfflineCapabilityVerifier
import com.idworx.lisa.features.offlinereliability.engine.OfflineReliabilityEngines
import com.idworx.lisa.features.offlinereliability.model.OfflineScoreBand
import com.idworx.lisa.features.offlinereliability.validation.OfflineReliabilityAuthorityV1
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
import com.idworx.lisa.features.offlinereliability.validators.AccessibilityOfflineValidator
import com.idworx.lisa.features.personality.engine.LisaPersonalityEngines
import com.idworx.lisa.features.personality.model.DialogueContext
import com.idworx.lisa.validation.ValidationOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class OfflineReliabilityAuthorityV1Test {

    private lateinit var engine: com.idworx.lisa.features.offlinereliability.engine.DefaultOfflineReliabilityEngine

    @Before
    fun setUp() {
        OfflineReliabilityEngines.resetForTests()
        engine = OfflineReliabilityEngines.createForTests()
    }

    @Test
    fun fullValidation_passesAndEmitsPassToken() {
        val report = OfflineReliabilityAuthorityV1.validate()
        if (report.outcome != ValidationOutcome.PASS) {
            println("FAILED: ${report.failedChecks}")
        }
        assertEquals("Failed: ${report.failedChecks}", ValidationOutcome.PASS, report.outcome)
        assertEquals(OfflineReliabilityAuthorityV1.PASS_TOKEN, report.passToken)
        assertTrue(report.failedChecks.isEmpty())
        println(report.formatReport())
        println(OfflineReliabilityAuthorityV1.PASS_TOKEN)
    }

    @Test
    fun offlineCommunicationPath_validated() {
        assertTrue(OfflineCapabilityVerifier.verifyOfflineCommunicationPath())
        val result = CommunicationOfflineValidator.validate()
        assertTrue(result.checksPassed >= 3)
    }

    @Test
    fun offlineCalibration_validated() {
        val result = CalibrationOfflineValidator.validate()
        assertTrue(result.checksPassed >= 2)
    }

    @Test
    fun offlineGuidedLearning_validated() {
        val result = GuidedLearningOfflineValidator.validate()
        assertTrue(result.checksPassed >= 4)
    }

    @Test
    fun offlineSpeech_validated() {
        val result = TTSSpeechOfflineValidator.validate()
        assertTrue(result.checksPassed >= 3)
        assertTrue(OfflineCapabilityVerifier.verifyExistingTtsReused())
    }

    @Test
    fun offlineCompanionMemory_validated() {
        val result = CompanionMemoryOfflineValidator.validate()
        assertTrue(result.checksPassed >= 3)
    }

    @Test
    fun offlinePersonalityDialogue_validated() {
        val result = PersonalityOfflineValidator.validate()
        assertTrue(result.checksPassed >= 3)
        val line = LisaPersonalityEngines.default.generateEncouragement(DialogueContext(deterministicSeed = 1)).text
        assertTrue(line.isNotBlank())
    }

    @Test
    fun offlineEmergencyCommunication_validated() {
        val result = EmergencyOfflineValidator.validate()
        assertTrue(result.checksPassed >= 3)
    }

    @Test
    fun offlineSettings_validated() {
        val result = SettingsOfflineValidator.validate()
        assertTrue(result.checksPassed >= 3)
    }

    @Test
    fun offlineAccessibility_validated() {
        val result = AccessibilityOfflineValidator.validate()
        assertTrue(result.checksPassed >= 2)
    }

    @Test
    fun offlineEyeTrackingAndBlink_validated() {
        assertTrue(EyeTrackingOfflineValidator.validate().checksPassed >= 3)
        assertTrue(BlinkDetectionOfflineValidator.validate().checksPassed >= 2)
    }

    @Test
    fun offlineScoring_produced() {
        assertTrue(OfflineCapabilityVerifier.verifyOfflineScoreProduced())
        val report = engine.generateReport()
        assertTrue(report.score.overall in 0..100)
        assertNotNull(report.score.band)
        assertTrue(report.score.band != OfflineScoreBand.Critical)
    }

    @Test
    fun offlineReports_generated() {
        assertTrue(OfflineCapabilityVerifier.verifyReportGeneration())
        val report = engine.generateReport()
        assertTrue(report.summary.isNotBlank())
        assertTrue(report.brain1OfflineReady)
        assertTrue(report.validationResults.size >= 10)
    }

    @Test
    fun offlineDiagnostics_available() {
        engine.generateReport()
        assertNotNull(OfflineDiagnostics.lastReport())
        assertTrue(OfflineDiagnostics.formatSummary().contains("Offline Reliability Diagnostics"))
    }

    @Test
    fun noMandatoryCloudDependency() {
        assertTrue(OfflineDependencyAuditor.detectMandatoryDependencies().isEmpty())
        assertTrue(OfflineCapabilityVerifier.verifyNoMandatoryCloudDependency())
    }

    @Test
    fun capabilityRunner_aggregatesValidators() {
        val results = OfflineCapabilityRunner.runAllValidators()
        assertTrue(results.size >= 10)
        val report = OfflineCapabilityRunner.buildReport(results)
        assertTrue(report.score.metrics.totalChecks > 0)
    }

    @Test
    fun noCommunicationBehaviorChange() {
        assertTrue(OfflineCapabilityVerifier.verifyNoCommunicationBehaviorChange())
    }
}
