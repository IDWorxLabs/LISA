package com.idworx.lisa.validation.authority

import com.idworx.lisa.features.guidedlearninginteractivelessons.metadata.GuidedLearningInteractiveLessonsMetadata
import com.idworx.lisa.features.guidedlearninginteractivelessons.validation.GuidedLearningInteractiveLessonsAuthorityV1
import com.idworx.lisa.features.onboardingguide.lessoninteraction.LessonInteractionEngine
import com.idworx.lisa.features.onboardingguide.lessons.TrainingLessonCatalog
import com.idworx.lisa.validation.ValidationOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GuidedLearningInteractiveLessonsAuthorityV1Test {

    @Test
    fun fullValidation_passesAndEmitsPassToken() {
        val report = GuidedLearningInteractiveLessonsAuthorityV1.validate()
        if (report.outcome != ValidationOutcome.PASS) {
            println("FAILED: ${report.failedChecks}")
        }
        assertEquals("Failed: ${report.failedChecks}", ValidationOutcome.PASS, report.outcome)
        assertEquals(GuidedLearningInteractiveLessonsAuthorityV1.PASS_TOKEN, report.passToken)
        assertTrue(report.failedChecks.isEmpty())
        println(report.formatReport())
        println(GuidedLearningInteractiveLessonsAuthorityV1.PASS_TOKEN)
    }

    @Test
    fun yesLesson_partialRightBlinkProgress() {
        val yes = TrainingLessonCatalog.communicationLessonAt(1)!!
        assertEquals(
            "Right blink 1 of 2",
            LessonInteractionEngine.progressLabel(0, 1, listOf(false), yes)
        )
    }

    @Test
    fun metadata_definesPassToken() {
        assertEquals(
            "LISA_GUIDED_LEARNING_INTERACTIVE_LESSONS_V1_PASS",
            GuidedLearningInteractiveLessonsMetadata.PASS_TOKEN
        )
    }
}
