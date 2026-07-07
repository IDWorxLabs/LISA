package com.idworx.lisa.validation.authority

import com.idworx.lisa.features.guidedtraininggesturemismatch.audit.GuidedTrainingGestureMismatchAuditor
import com.idworx.lisa.features.guidedtraininggesturemismatch.metadata.GuidedTrainingGestureMismatchMetadata
import com.idworx.lisa.features.guidedtraininggesturemismatch.validation.GuidedTrainingGestureMismatchAuthorityV1
import com.idworx.lisa.validation.ValidationOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GuidedTrainingGestureMismatchAuthorityV1Test {

    @Test
    fun fullValidation_passesAndEmitsPassToken() {
        val report = GuidedTrainingGestureMismatchAuthorityV1.validate()
        if (report.outcome != ValidationOutcome.PASS) {
            println("FAILED: ${report.failedChecks}")
        }
        assertEquals("Failed: ${report.failedChecks}", ValidationOutcome.PASS, report.outcome)
        assertEquals(GuidedTrainingGestureMismatchAuthorityV1.PASS_TOKEN, report.passToken)
        assertTrue(report.failedChecks.isEmpty())
        println(report.formatReport())
        println(GuidedTrainingGestureMismatchAuthorityV1.PASS_TOKEN)
    }

    @Test
    fun metadata_definesPassToken() {
        assertEquals(
            "LISA_GUIDED_TRAINING_GESTURE_MISMATCH_V1_PASS",
            GuidedTrainingGestureMismatchMetadata.PASS_TOKEN
        )
    }

    @Test
    fun categoryLesson_gestureEqualsRealWorkspaceGesture() {
        assertTrue(GuidedTrainingGestureMismatchAuditor.categoryLessonGestureEqualsRealWorkspaceGesture())
    }

    @Test
    fun phraseLesson_gestureEqualsRealWorkspacePhraseGesture() {
        assertTrue(GuidedTrainingGestureMismatchAuditor.phraseLessonGestureEqualsRealWorkspacePhraseGesture())
    }

    @Test
    fun navigationLessons_gesturesEqualRealPanelGestures() {
        assertTrue(GuidedTrainingGestureMismatchAuditor.navigationLessonGesturesEqualRealPanelGestures())
    }

    @Test
    fun displayedGesture_equalsAcceptedGestureForEveryNavigationLesson() {
        assertTrue(GuidedTrainingGestureMismatchAuditor.displayedGestureEqualsAcceptedGestureForEveryNavigationLesson())
    }

    @Test
    fun highlightedTarget_gestureEqualsLessonGesture() {
        assertTrue(GuidedTrainingGestureMismatchAuditor.highlightedTargetGestureEqualsLessonGesture())
    }

    @Test
    fun wrongOrHardcoded_categoryGesturesAreRejected() {
        assertTrue(GuidedTrainingGestureMismatchAuditor.wrongOrHardcodedCategoryGesturesAreRejected())
    }

    @Test
    fun correctRealWorkspace_categoryGestureIsAccepted() {
        assertTrue(GuidedTrainingGestureMismatchAuditor.correctRealWorkspaceCategoryGestureIsAccepted())
    }

    @Test
    fun normalWorkspace_usesSameGestureMappingAfterTraining() {
        assertTrue(GuidedTrainingGestureMismatchAuditor.normalWorkspaceUsesSameGestureMappingAfterTraining())
    }

    @Test
    fun mainActivity_fineGateUsesSingleSourceOfTruth() {
        assertTrue(GuidedTrainingGestureMismatchAuditor.mainActivityFineGateUsesSingleSourceOfTruth())
    }
}
