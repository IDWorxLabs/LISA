package com.idworx.lisa.validation.authority

import com.idworx.lisa.features.guidedtraininglessonfocus.audit.GuidedTrainingLessonFocusAuditor
import com.idworx.lisa.features.guidedtraininglessonfocus.metadata.GuidedTrainingLessonFocusMetadata
import com.idworx.lisa.features.guidedtraininglessonfocus.validation.GuidedTrainingLessonFocusAuthorityV1
import com.idworx.lisa.validation.ValidationOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GuidedTrainingLessonFocusAuthorityV1Test {

    @Test
    fun fullValidation_passesAndEmitsPassToken() {
        val report = GuidedTrainingLessonFocusAuthorityV1.validate()
        if (report.outcome != ValidationOutcome.PASS) {
            println("FAILED: ${report.failedChecks}")
        }
        assertEquals("Failed: ${report.failedChecks}", ValidationOutcome.PASS, report.outcome)
        assertEquals(GuidedTrainingLessonFocusAuthorityV1.PASS_TOKEN, report.passToken)
        assertTrue(report.failedChecks.isEmpty())
        println(report.formatReport())
        println(GuidedTrainingLessonFocusAuthorityV1.PASS_TOKEN)
    }

    @Test
    fun metadata_definesPassToken() {
        assertEquals(
            "LISA_GUIDED_TRAINING_LESSON_FOCUS_V1_PASS",
            GuidedTrainingLessonFocusMetadata.PASS_TOKEN
        )
    }

    @Test
    fun phraseGestures_blockedDuringUnrelatedLesson() {
        assertTrue(GuidedTrainingLessonFocusAuditor.phraseGesturesBlockedDuringUnrelatedLesson())
    }

    @Test
    fun onlyHighlightedPhrase_selectableDuringSelectPhraseLesson() {
        assertTrue(GuidedTrainingLessonFocusAuditor.onlyHighlightedPhraseSelectableDuringSelectPhraseLesson())
    }

    @Test
    fun wrongGesture_showsRedFeedback() {
        assertTrue(GuidedTrainingLessonFocusAuditor.wrongGestureShowsRedFeedback())
    }

    @Test
    fun wrongGesture_resetsSequenceWithoutAdvancing() {
        assertTrue(GuidedTrainingLessonFocusAuditor.wrongGestureResetsSequenceWithoutAdvancing())
    }

    @Test
    fun dimmedItems_areFunctionallyInactive() {
        assertTrue(GuidedTrainingLessonFocusAuditor.dimmedItemsAreFunctionallyInactive())
    }

    @Test
    fun normalWorkspace_unaffectedOutsideGuidedTraining() {
        assertTrue(GuidedTrainingLessonFocusAuditor.normalWorkspaceUnaffectedOutsideGuidedTraining())
    }

    @Test
    fun correctExpectedGesture_stillCompletesLesson() {
        assertTrue(GuidedTrainingLessonFocusAuditor.correctExpectedGestureStillCompletesLesson())
    }

    @Test
    fun existingResponseTimeAndPositiveFeedback_stillPasses() {
        assertTrue(GuidedTrainingLessonFocusAuditor.existingResponseTimeAndPositiveFeedbackStillPasses())
    }

    @Test
    fun emergencyTouch_rejectedWhenOffTarget() {
        assertTrue(GuidedTrainingLessonFocusAuditor.emergencyTouchRejectedWhenOffTarget())
    }

    @Test
    fun emergencyTouch_showsRedFeedbackAndResetsWithoutAdvancing() {
        assertTrue(GuidedTrainingLessonFocusAuditor.emergencyTouchShowsRedFeedbackAndResetsWithoutAdvancing())
    }

    @Test
    fun emergencyTouch_succeedsDuringEmergencyLesson() {
        assertTrue(GuidedTrainingLessonFocusAuditor.emergencyTouchSucceedsDuringEmergencyLesson())
    }

    @Test
    fun blinkPathEmergency_behaviorUnchanged() {
        assertTrue(GuidedTrainingLessonFocusAuditor.blinkPathEmergencyBehaviorUnchanged())
    }
}
