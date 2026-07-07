package com.idworx.lisa.validation.authority

import com.idworx.lisa.features.realworkspaceguidednavigationtraining.audit.RealWorkspaceGuidedNavigationTrainingAuditor
import com.idworx.lisa.features.realworkspaceguidednavigationtraining.metadata.RealWorkspaceGuidedNavigationTrainingMetadata
import com.idworx.lisa.features.realworkspaceguidednavigationtraining.validation.RealWorkspaceGuidedNavigationTrainingAuthorityV1
import com.idworx.lisa.validation.ValidationOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RealWorkspaceGuidedNavigationTrainingAuthorityV1Test {

    @Test
    fun fullValidation_passesAndEmitsPassToken() {
        val report = RealWorkspaceGuidedNavigationTrainingAuthorityV1.validate()
        if (report.outcome != ValidationOutcome.PASS) {
            println("FAILED: ${report.failedChecks}")
        }
        assertEquals("Failed: ${report.failedChecks}", ValidationOutcome.PASS, report.outcome)
        assertEquals(RealWorkspaceGuidedNavigationTrainingAuthorityV1.PASS_TOKEN, report.passToken)
        assertTrue(report.failedChecks.isEmpty())
        println(report.formatReport())
        println(RealWorkspaceGuidedNavigationTrainingAuthorityV1.PASS_TOKEN)
    }

    @Test
    fun metadata_definesPassToken() {
        assertEquals(
            "LISA_REAL_WORKSPACE_GUIDED_NAVIGATION_TRAINING_V1_PASS",
            RealWorkspaceGuidedNavigationTrainingMetadata.PASS_TOKEN
        )
    }

    @Test
    fun navigationTraining_doesNotUseBlankScreen() {
        assertTrue(RealWorkspaceGuidedNavigationTrainingAuditor.navigationTrainingDoesNotUseBlankScreen())
    }

    @Test
    fun realWorkspace_opensAfterPhraseLessons() {
        assertTrue(RealWorkspaceGuidedNavigationTrainingAuditor.realWorkspaceOpensAfterPhraseLessons())
    }

    @Test
    fun workspace_supportsGuidedTrainingMode() {
        assertTrue(RealWorkspaceGuidedNavigationTrainingAuditor.workspaceSupportsGuidedTrainingMode())
    }

    @Test
    fun currentLessonTarget_isHighlighted() {
        assertTrue(RealWorkspaceGuidedNavigationTrainingAuditor.currentLessonTargetIsHighlighted())
    }

    @Test
    fun onlyTargetGesture_isAccepted() {
        assertTrue(RealWorkspaceGuidedNavigationTrainingAuditor.onlyTargetGestureAccepted())
    }

    @Test
    fun nonTargetGestures_areIgnored() {
        assertTrue(RealWorkspaceGuidedNavigationTrainingAuditor.nonTargetGesturesIgnored())
    }

    @Test
    fun openCategories_usesRealControl() {
        assertTrue(RealWorkspaceGuidedNavigationTrainingAuditor.openCategoriesUsesRealControl())
    }

    @Test
    fun selectCategory_usesRealControl() {
        assertTrue(RealWorkspaceGuidedNavigationTrainingAuditor.selectCategoryUsesRealControl())
    }

    @Test
    fun selectPhrase_usesRealControl() {
        assertTrue(RealWorkspaceGuidedNavigationTrainingAuditor.selectPhraseUsesRealControl())
    }

    @Test
    fun backNextPreviousEmergency_useRealControls() {
        assertTrue(RealWorkspaceGuidedNavigationTrainingAuditor.backNextPreviousEmergencyUseRealControls())
    }

    @Test
    fun normalResolver_doesNotConsumeGuidedGesturesFirst() {
        assertTrue(RealWorkspaceGuidedNavigationTrainingAuditor.normalResolverDoesNotConsumeGuidedGesturesFirst())
    }

    @Test
    fun workspace_returnsToNormalModeAfterCompletion() {
        assertTrue(RealWorkspaceGuidedNavigationTrainingAuditor.workspaceReturnsToNormalModeAfterCompletion())
    }

    @Test
    fun phraseOnlySpeechPolicy_unchangedNoBrain2NoCloud() {
        assertTrue(RealWorkspaceGuidedNavigationTrainingAuditor.phraseOnlySpeechPolicyUnchangedNoBrain2NoCloud())
    }

    @Test
    fun existingGuidedLearningValidations_remainGreen() {
        assertTrue(RealWorkspaceGuidedNavigationTrainingAuditor.existingGuidedLearningValidationsRemainGreen())
    }
}
