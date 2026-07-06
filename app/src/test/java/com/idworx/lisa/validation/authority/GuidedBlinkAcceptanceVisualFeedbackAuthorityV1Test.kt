package com.idworx.lisa.validation.authority

import com.idworx.lisa.features.guidedblinkacceptancevisualfeedback.audit.GuidedBlinkAcceptanceVisualFeedbackAuditor
import com.idworx.lisa.features.guidedblinkacceptancevisualfeedback.metadata.GuidedBlinkAcceptanceVisualFeedbackMetadata
import com.idworx.lisa.features.guidedblinkacceptancevisualfeedback.validation.GuidedBlinkAcceptanceVisualFeedbackAuthorityV1
import com.idworx.lisa.validation.ValidationOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GuidedBlinkAcceptanceVisualFeedbackAuthorityV1Test {

    @Test
    fun fullValidation_passesAndEmitsPassToken() {
        val report = GuidedBlinkAcceptanceVisualFeedbackAuthorityV1.validate()
        if (report.outcome != ValidationOutcome.PASS) {
            println("FAILED: ${report.failedChecks}")
        }
        assertEquals("Failed: ${report.failedChecks}", ValidationOutcome.PASS, report.outcome)
        assertEquals(GuidedBlinkAcceptanceVisualFeedbackAuthorityV1.PASS_TOKEN, report.passToken)
        assertTrue(report.failedChecks.isEmpty())
        println(report.formatReport())
        println(GuidedBlinkAcceptanceVisualFeedbackAuthorityV1.PASS_TOKEN)
    }

    @Test
    fun metadata_definesPassToken() {
        assertEquals(
            "LISA_GUIDED_BLINK_ACCEPTANCE_VISUAL_FEEDBACK_V1_PASS",
            GuidedBlinkAcceptanceVisualFeedbackMetadata.PASS_TOKEN
        )
    }

    @Test
    fun acceptedLeftBlink_animatesOnlyLeftCounter() {
        assertTrue(GuidedBlinkAcceptanceVisualFeedbackAuditor.acceptedLeftBlinkAnimatesOnlyLeftCounter())
    }

    @Test
    fun acceptedRightBlink_animatesOnlyRightCounter() {
        assertTrue(GuidedBlinkAcceptanceVisualFeedbackAuditor.acceptedRightBlinkAnimatesOnlyRightCounter())
    }

    @Test
    fun counterPulse_completesWithinDesignBudget() {
        assertTrue(GuidedBlinkAcceptanceVisualFeedbackAuditor.counterPulseCompletesSuccessfully())
    }

    @Test
    fun indicatorFlash_onlyForDetectedEye() {
        assertTrue(GuidedBlinkAcceptanceVisualFeedbackAuditor.indicatorFlashOccursOnlyForDetectedEye())
    }

    @Test
    fun acceptedBlinkMessage_appearsAndAutoHides() {
        assertTrue(GuidedBlinkAcceptanceVisualFeedbackAuditor.acceptedBlinkMessageAppearsAndAutoHides())
    }

    @Test
    fun multipleAcceptedBlinks_eachIndependentlyTrigger() {
        assertTrue(GuidedBlinkAcceptanceVisualFeedbackAuditor.multipleAcceptedBlinksTriggerMultipleAnimations())
    }

    @Test
    fun wrongEyeBlink_neverTriggersAcceptedAnimation() {
        assertTrue(GuidedBlinkAcceptanceVisualFeedbackAuditor.wrongEyeBlinkDoesNotTriggerAcceptedAnimation())
    }

    @Test
    fun partialTimeoutReset_stillWorks() {
        assertTrue(GuidedBlinkAcceptanceVisualFeedbackAuditor.partialTimeoutResetStillWorks())
    }

    @Test
    fun phraseOnlySpeech_remainsUnchanged() {
        assertTrue(GuidedBlinkAcceptanceVisualFeedbackAuditor.phraseOnlySpeechUnchanged())
    }

    @Test
    fun existingGuidedLearningValidations_remainGreen() {
        assertTrue(GuidedBlinkAcceptanceVisualFeedbackAuditor.existingGuidedLearningValidationsRemainGreen())
    }
}
