package com.idworx.lisa.validation.authority

import com.idworx.lisa.GuidedModeNavigation
import com.idworx.lisa.features.guidedtrainingexitrefinement.audit.GuidedTrainingExitRefinementAuditor
import com.idworx.lisa.features.guidedtrainingexitrefinement.metadata.GuidedTrainingExitRefinementMetadata
import com.idworx.lisa.features.guidedtrainingexitrefinement.validation.GuidedTrainingExitRefinementAuthorityV1
import com.idworx.lisa.features.onboardingguide.lessons.TrainingLessonCatalog
import com.idworx.lisa.features.onboardingguide.model.NavigationAction
import com.idworx.lisa.validation.ValidationOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GuidedTrainingExitRefinementAuthorityV1Test {

    @Test
    fun fullValidation_passesAndEmitsPassToken() {
        val report = GuidedTrainingExitRefinementAuthorityV1.validate()
        if (report.outcome != ValidationOutcome.PASS) {
            println("FAILED: ${report.failedChecks}")
        }
        assertEquals("Failed: ${report.failedChecks}", ValidationOutcome.PASS, report.outcome)
        assertEquals(GuidedTrainingExitRefinementAuthorityV1.PASS_TOKEN, report.passToken)
        assertTrue(report.failedChecks.isEmpty())
        println(report.formatReport())
        println(GuidedTrainingExitRefinementAuthorityV1.PASS_TOKEN)
    }

    @Test
    fun metadata_definesPassToken() {
        assertEquals(
            "LISA_GUIDED_TRAINING_EXIT_REFINEMENT_V1_PASS",
            GuidedTrainingExitRefinementMetadata.PASS_TOKEN
        )
    }

    @Test
    fun finalNavigationLesson_teachesResetSequenceNotATap() {
        assertEquals(NavigationAction.ResetSequence, TrainingLessonCatalog.navigationLessons.last().action)
        assertTrue(GuidedTrainingExitRefinementAuditor.finalLessonNeverInstructsATap())
    }

    @Test
    fun finishTrainingGesture_isDistinctFromCategoriesAndReserved() {
        assertNotEquals(
            GuidedModeNavigation.CATEGORIES_LEFT to GuidedModeNavigation.CATEGORIES_RIGHT,
            GuidedModeNavigation.FINISH_TRAINING_LEFT to GuidedModeNavigation.FINISH_TRAINING_RIGHT
        )
        assertTrue(
            GuidedModeNavigation.isFinishTrainingSequence(
                GuidedModeNavigation.FINISH_TRAINING_LEFT,
                GuidedModeNavigation.FINISH_TRAINING_RIGHT
            )
        )
    }

    @Test
    fun categoriesGesture_isShorterThanOldEightWinkGesture() {
        val totalWinks = GuidedModeNavigation.CATEGORIES_LEFT + GuidedModeNavigation.CATEGORIES_RIGHT
        assertTrue("Expected Categories gesture shorter than the old 8-wink L4 R4, was $totalWinks winks", totalWinks < 8)
    }

    @Test
    fun completingAllNavigationLessons_reachesCompletionAndNormalWorkspaceMode() {
        assertTrue(GuidedTrainingExitRefinementAuditor.completingAllNavigationLessonsReachesCompletionPhase())
        assertTrue(GuidedTrainingExitRefinementAuditor.communicationModeActiveAfterCompletion())
    }

    @Test
    fun communicationWorkspace_instructionBlockRemovedButCoreElementsRemain() {
        assertTrue(GuidedTrainingExitRefinementAuditor.communicationWorkspaceInstructionBlockRemoved())
        assertTrue(GuidedTrainingExitRefinementAuditor.communicationWorkspaceStillShowsCoreElements())
    }

    @Test
    fun noGestureDuplicationOrConflicts() {
        assertTrue(GuidedTrainingExitRefinementAuditor.noDuplicateReservedGestures())
        assertTrue(GuidedTrainingExitRefinementAuditor.noDuplicateGesturesAcrossWorkspaceModes())
        assertTrue(GuidedTrainingExitRefinementAuditor.noReservedGestureConflicts())
    }

    @Test
    fun existingNeighbouringAuthorities_remainGreen() {
        assertTrue(GuidedTrainingExitRefinementAuditor.existingGuidedTrainingAndNavigationAuthoritiesRemainGreen())
    }
}
