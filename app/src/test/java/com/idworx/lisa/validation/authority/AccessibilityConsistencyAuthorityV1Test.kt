package com.idworx.lisa.validation.authority

import com.idworx.lisa.features.accessibilityconsistency.diagnostics.AccessibilityDiagnostics
import com.idworx.lisa.features.accessibilityconsistency.engine.AccessibilityAuditRunner
import com.idworx.lisa.features.accessibilityconsistency.engine.AccessibilityConsistencyEngines
import com.idworx.lisa.features.accessibilityconsistency.engine.AccessibilityVerifier
import com.idworx.lisa.features.accessibilityconsistency.integration.AccessibilityPersonalityAdapter
import com.idworx.lisa.features.accessibilityconsistency.model.AccessibilityScoreBand
import com.idworx.lisa.features.accessibilityconsistency.validators.AccessibilitySettingsValidator
import com.idworx.lisa.features.accessibilityconsistency.validators.CommunicationValidator
import com.idworx.lisa.features.accessibilityconsistency.validators.EmergencyAccessibilityValidator
import com.idworx.lisa.features.accessibilityconsistency.validators.GuidedLearningValidator
import com.idworx.lisa.features.accessibilityconsistency.validators.LayoutValidator
import com.idworx.lisa.features.accessibilityconsistency.validators.NavigationValidator
import com.idworx.lisa.features.accessibilityconsistency.validators.TouchTargetValidator
import com.idworx.lisa.features.accessibilityconsistency.validators.TypographyValidator
import com.idworx.lisa.features.accessibilityconsistency.validation.AccessibilityConsistencyAuthorityV1
import com.idworx.lisa.features.personality.engine.LisaPersonalityEngines
import com.idworx.lisa.validation.ValidationOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AccessibilityConsistencyAuthorityV1Test {

    private lateinit var engine: com.idworx.lisa.features.accessibilityconsistency.engine.DefaultAccessibilityConsistencyEngine

    @Before
    fun setUp() {
        AccessibilityConsistencyEngines.resetForTests()
        engine = AccessibilityConsistencyEngines.createForTests()
    }

    @Test
    fun fullValidation_passesAndEmitsPassToken() {
        val report = AccessibilityConsistencyAuthorityV1.validate()
        if (report.outcome != ValidationOutcome.PASS) {
            println("FAILED: ${report.failedChecks}")
        }
        assertEquals("Failed: ${report.failedChecks}", ValidationOutcome.PASS, report.outcome)
        assertEquals(AccessibilityConsistencyAuthorityV1.PASS_TOKEN, report.passToken)
        assertTrue(report.failedChecks.isEmpty())
        println(report.formatReport())
        println(AccessibilityConsistencyAuthorityV1.PASS_TOKEN)
    }

    @Test
    fun typographyValidation_passes() {
        val result = TypographyValidator.validate()
        assertTrue(result.checksPassed >= 3)
        assertTrue(AccessibilityVerifier.verifyTypographyValidation())
    }

    @Test
    fun touchTargetValidation_checksTrainingButtons() {
        val result = TouchTargetValidator.validate()
        assertTrue(result.checksPassed >= 2)
    }

    @Test
    fun layoutValidation_structureExists() {
        val result = LayoutValidator.validate()
        assertTrue(result.checksPassed >= 2)
    }

    @Test
    fun navigationConsistency_validated() {
        val result = NavigationValidator.validate()
        assertTrue(result.checksPassed >= 3)
    }

    @Test
    fun guidedLearningAccessibility_validated() {
        val result = GuidedLearningValidator.validate()
        assertTrue(result.checksPassed >= 3)
    }

    @Test
    fun communicationAccessibility_validated() {
        val result = CommunicationValidator.validate()
        assertTrue(result.checksPassed >= 3)
    }

    @Test
    fun emergencyAccessibility_validated() {
        val result = EmergencyAccessibilityValidator.validate()
        assertTrue(result.checksPassed >= 3)
    }

    @Test
    fun accessibilitySettings_reused() {
        val result = AccessibilitySettingsValidator.validate()
        assertTrue(result.checksPassed >= 4)
    }

    @Test
    fun accessibilityScoring_produced() {
        assertTrue(AccessibilityVerifier.verifyAuditProducesScore())
        val report = engine.generateReport()
        assertTrue(report.score.overall in 0..100)
        assertNotNull(report.score.band)
    }

    @Test
    fun reportGeneration_complete() {
        assertTrue(AccessibilityVerifier.verifyReportGeneration())
        val report = engine.generateReport()
        assertTrue(report.summary.isNotBlank())
        assertTrue(report.recommendations.isNotEmpty() || report.issues.isEmpty())
    }

    @Test
    fun diagnostics_available() {
        engine.generateReport()
        assertNotNull(AccessibilityDiagnostics.lastReport())
        assertTrue(AccessibilityDiagnostics.formatSummary().contains("Accessibility Diagnostics"))
    }

    @Test
    fun personalityIntegration_providesGuidance() {
        val hint = AccessibilityPersonalityAdapter.settingsHint()
        assertTrue(hint.contains("text size", ignoreCase = true))
        val message = AccessibilityPersonalityAdapter.guidanceForScore(
            com.idworx.lisa.features.accessibilityconsistency.model.AccessibilityScore(
                overall = 50,
                band = AccessibilityScoreBand.NeedsImprovement,
                metrics = com.idworx.lisa.features.accessibilityconsistency.model.AccessibilityMetrics(
                    0, 0, 0, 0, 0, 0, "test"
                ),
                evidence = "test"
            )
        )
        assertTrue(message.isNotBlank())
    }

    @Test
    fun auditRunner_aggregatesValidators() {
        val results = AccessibilityAuditRunner.runAllValidators()
        assertTrue(results.size >= 10)
        val report = AccessibilityAuditRunner.buildReport(results)
        assertTrue(report.audit.checksPerformed > 0)
    }

    @Test
    fun noCommunicationBehaviorChange() {
        assertTrue(AccessibilityVerifier.verifyNoCommunicationBehaviorChange())
    }

    @Test
    fun personalityEngine_notExposingAuditDetails() {
        val message = AccessibilityPersonalityAdapter.encouragement(LisaPersonalityEngines.default)
        assertTrue(!message.contains("A11Y_") && !message.contains("validator"))
    }
}
