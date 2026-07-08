package com.idworx.lisa.validation.authority

import com.idworx.lisa.GuidedModeNavigation
import com.idworx.lisa.features.guidedcurriculumandnavigationcontext.metadata.GuidedCurriculumAndNavigationContextMetadata
import com.idworx.lisa.features.guidedcurriculumandnavigationcontext.validation.GuidedCurriculumAndNavigationContextAuthorityV1
import com.idworx.lisa.features.onboardingguide.metadata.TrainingMetadata
import com.idworx.lisa.features.onboardingguide.navigation.NavigationTrainingGestureHandler
import com.idworx.lisa.features.onboardingguide.model.NavigationAction
import com.idworx.lisa.validation.ValidationOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GuidedCurriculumAndNavigationContextAuthorityV1Test {

    @Test
    fun fullValidation_passesAndEmitsPassToken() {
        val report = GuidedCurriculumAndNavigationContextAuthorityV1.validate()
        if (report.outcome != ValidationOutcome.PASS) {
            println("FAILED: ${report.failedChecks}")
        }
        assertEquals("Failed: ${report.failedChecks}", ValidationOutcome.PASS, report.outcome)
        assertEquals(GuidedCurriculumAndNavigationContextAuthorityV1.PASS_TOKEN, report.passToken)
        assertTrue(report.failedChecks.isEmpty())
        println(report.formatReport())
        println(GuidedCurriculumAndNavigationContextAuthorityV1.PASS_TOKEN)
    }

    @Test
    fun metadata_definesPassToken() {
        assertEquals(
            "LISA_GUIDED_CURRICULUM_AND_NAVIGATION_CONTEXT_V1_PASS",
            GuidedCurriculumAndNavigationContextMetadata.PASS_TOKEN
        )
    }

    @Test
    fun essentialPhraseCount_isFifteen() {
        assertEquals(15, TrainingMetadata.GUIDED_LEARNING_ESSENTIAL_PHRASE_COUNT)
        assertTrue(TrainingMetadata.GUIDED_LEARNING_ESSENTIAL_PHRASE_COUNT in 10..15)
    }

    @Test
    fun categoriesGesture_resolvesToOpenCategories() {
        val action = NavigationTrainingGestureHandler.resolveAction(
            GuidedModeNavigation.CATEGORIES_LEFT,
            GuidedModeNavigation.CATEGORIES_RIGHT
        )
        assertEquals(NavigationAction.OpenCategories, action)
    }
}
