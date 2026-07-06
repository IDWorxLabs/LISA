package com.idworx.lisa.validation.authority

import com.idworx.lisa.features.guidedlessonprogresslabel.metadata.GuidedLessonProgressLabelMetadata
import com.idworx.lisa.features.guidedlessonprogresslabel.validation.GuidedLessonProgressLabelAuthorityV1
import com.idworx.lisa.features.onboardingguide.lessons.TrainingLessonCatalog
import com.idworx.lisa.features.onboardingguide.metadata.TrainingMetadata
import com.idworx.lisa.features.onboardingguide.model.TrainingPhase
import com.idworx.lisa.features.onboardingguide.model.TrainingProgress
import com.idworx.lisa.validation.ValidationOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GuidedLessonProgressLabelAuthorityV1Test {

    @Test
    fun fullValidation_passesAndEmitsPassToken() {
        val report = GuidedLessonProgressLabelAuthorityV1.validate()
        if (report.outcome != ValidationOutcome.PASS) {
            println("FAILED: ${report.failedChecks}")
        }
        assertEquals("Failed: ${report.failedChecks}", ValidationOutcome.PASS, report.outcome)
        assertEquals(GuidedLessonProgressLabelAuthorityV1.PASS_TOKEN, report.passToken)
        assertTrue(report.failedChecks.isEmpty())
        println(report.formatReport())
        println(GuidedLessonProgressLabelAuthorityV1.PASS_TOKEN)
    }

    @Test
    fun metadata_definesPassToken() {
        assertEquals(
            "LISA_GUIDED_LESSON_PROGRESS_LABEL_V1_PASS",
            GuidedLessonProgressLabelMetadata.PASS_TOKEN
        )
    }

    @Test
    fun firstEssentialLesson_isLessonOneOfFifteenPlusEightNavigation() {
        val progress = TrainingProgress(
            currentPhase = TrainingPhase.CommunicationLesson,
            communicationLessonIndex = 0
        )
        val (current, total) = TrainingLessonCatalog.guidedLessonProgress(progress)!!
        assertEquals(1, current)
        assertEquals(
            TrainingMetadata.GUIDED_LEARNING_ESSENTIAL_PHRASE_COUNT + TrainingMetadata.NAVIGATION_LESSON_COUNT,
            total
        )
    }

    @Test
    fun lastEssentialLesson_isFifteenOfTotal() {
        val progress = TrainingProgress(
            currentPhase = TrainingPhase.CommunicationLesson,
            communicationLessonIndex = TrainingMetadata.GUIDED_LEARNING_ESSENTIAL_PHRASE_COUNT - 1
        )
        val (current, _) = TrainingLessonCatalog.guidedLessonProgress(progress)!!
        assertEquals(TrainingMetadata.GUIDED_LEARNING_ESSENTIAL_PHRASE_COUNT, current)
    }

    @Test
    fun firstNavigationLesson_continuesNumberingRightAfterEssentials() {
        val progress = TrainingProgress(
            currentPhase = TrainingPhase.NavigationLesson,
            navigationLessonIndex = 0
        )
        val (current, total) = TrainingLessonCatalog.guidedLessonProgress(progress)!!
        assertEquals(TrainingMetadata.GUIDED_LEARNING_ESSENTIAL_PHRASE_COUNT + 1, current)
        assertEquals(
            TrainingMetadata.GUIDED_LEARNING_ESSENTIAL_PHRASE_COUNT + TrainingMetadata.NAVIGATION_LESSON_COUNT,
            total
        )
    }

    @Test
    fun nonLessonPhases_returnNull() {
        assertNull(
            TrainingLessonCatalog.guidedLessonProgress(
                TrainingProgress(currentPhase = TrainingPhase.Welcome)
            )
        )
        assertNull(
            TrainingLessonCatalog.guidedLessonProgress(
                TrainingProgress(currentPhase = TrainingPhase.Completion)
            )
        )
    }
}
