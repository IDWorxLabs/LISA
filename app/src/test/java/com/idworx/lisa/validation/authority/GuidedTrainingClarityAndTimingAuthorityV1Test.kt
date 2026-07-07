package com.idworx.lisa.validation.authority

import com.idworx.lisa.features.guidedtrainingclarityandtiming.audit.GuidedTrainingClarityAndTimingAuditor
import com.idworx.lisa.features.guidedtrainingclarityandtiming.metadata.GuidedTrainingClarityAndTimingMetadata
import com.idworx.lisa.features.guidedtrainingclarityandtiming.validation.GuidedTrainingClarityAndTimingAuthorityV1
import com.idworx.lisa.validation.ValidationOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GuidedTrainingClarityAndTimingAuthorityV1Test {

    @Test
    fun fullValidation_passesAndEmitsPassToken() {
        val report = GuidedTrainingClarityAndTimingAuthorityV1.validate()
        if (report.outcome != ValidationOutcome.PASS) {
            println("FAILED: ${report.failedChecks}")
        }
        assertEquals("Failed: ${report.failedChecks}", ValidationOutcome.PASS, report.outcome)
        assertEquals(GuidedTrainingClarityAndTimingAuthorityV1.PASS_TOKEN, report.passToken)
        assertTrue(report.failedChecks.isEmpty())
        println(report.formatReport())
        println(GuidedTrainingClarityAndTimingAuthorityV1.PASS_TOKEN)
    }

    @Test
    fun metadata_definesPassToken() {
        assertEquals(
            "LISA_GUIDED_TRAINING_CLARITY_AND_TIMING_V1_PASS",
            GuidedTrainingClarityAndTimingMetadata.PASS_TOKEN
        )
    }

    @Test
    fun responseTime_adjustableIndependentlyOfSensitivity() {
        assertTrue(GuidedTrainingClarityAndTimingAuditor.responseTimeAdjustableIndependentlyOfSensitivity())
    }

    @Test
    fun defaultGuidedResponseTime_isSlowerThanThreeSeconds() {
        assertTrue(GuidedTrainingClarityAndTimingAuditor.defaultGuidedResponseTimeIsSlowerThanThreeSeconds())
    }

    @Test
    fun activeSequences_notInterruptedByResponseTimer() {
        assertTrue(GuidedTrainingClarityAndTimingAuditor.activeSequencesNotInterruptedByResponseTimer())
    }

    @Test
    fun correctFirstGuidedSequence_triggersPositiveFeedback() {
        assertTrue(GuidedTrainingClarityAndTimingAuditor.correctFirstGuidedSequenceTriggersPositiveFeedback())
    }

    @Test
    fun floatingLessonBubble_remainsPresent() {
        assertTrue(GuidedTrainingClarityAndTimingAuditor.floatingLessonBubbleRemainsPresent())
    }

    @Test
    fun guidedWorkspaceTarget_clearlyRepresented() {
        assertTrue(GuidedTrainingClarityAndTimingAuditor.guidedWorkspaceTargetClearlyRepresented())
    }

    @Test
    fun finalNavigationSuccess_emitsVisualFeedbackBeforeCompletion() {
        assertTrue(GuidedTrainingClarityAndTimingAuditor.finalNavigationSuccessEmitsVisualFeedbackBeforeCompletion())
    }

    @Test
    fun completion_stillHappensAfterFeedbackDelay() {
        assertTrue(GuidedTrainingClarityAndTimingAuditor.completionStillHappensAfterFeedbackDelay())
    }

    @Test
    fun nonFinalNavigationLessons_continueNormally() {
        assertTrue(GuidedTrainingClarityAndTimingAuditor.nonFinalNavigationLessonsContinueNormally())
    }

    @Test
    fun spokenFeedback_stillWorksForNormalAndFinalLessons() {
        assertTrue(GuidedTrainingClarityAndTimingAuditor.spokenFeedbackStillWorksForNormalAndFinalLessons())
    }

    @Test
    fun noRegression_toGuidedResponseTimeBehavior() {
        assertTrue(GuidedTrainingClarityAndTimingAuditor.noRegressionToGuidedResponseTimeBehavior())
    }
}
