package com.idworx.lisa.validation.authority

import com.idworx.lisa.features.guidedwrongblinkrestartssequence.metadata.GuidedWrongBlinkRestartsSequenceMetadata
import com.idworx.lisa.features.guidedwrongblinkrestartssequence.validation.GuidedWrongBlinkRestartsSequenceAuthorityV1
import com.idworx.lisa.features.onboardingguide.lessoninteraction.LessonInteractionEngine
import com.idworx.lisa.features.onboardingguide.lessons.TrainingLessonCatalog
import com.idworx.lisa.validation.ValidationOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GuidedWrongBlinkRestartsSequenceAuthorityV1Test {

    @Test
    fun fullValidation_passesAndEmitsPassToken() {
        val report = GuidedWrongBlinkRestartsSequenceAuthorityV1.validate()
        if (report.outcome != ValidationOutcome.PASS) {
            println("FAILED: ${report.failedChecks}")
        }
        assertEquals("Failed: ${report.failedChecks}", ValidationOutcome.PASS, report.outcome)
        assertEquals(GuidedWrongBlinkRestartsSequenceAuthorityV1.PASS_TOKEN, report.passToken)
        assertTrue(report.failedChecks.isEmpty())
        println(report.formatReport())
        println(GuidedWrongBlinkRestartsSequenceAuthorityV1.PASS_TOKEN)
    }

    @Test
    fun metadata_definesPassToken() {
        assertEquals(
            "LISA_GUIDED_WRONG_BLINK_RESTARTS_SEQUENCE_V1_PASS",
            GuidedWrongBlinkRestartsSequenceMetadata.PASS_TOKEN
        )
    }

    @Test
    fun foodLesson_wrongEyeAfterPartialProgressRequiresRestart() {
        val food = TrainingLessonCatalog.communicationFundamentals
            .first { it.vocabularyId == "i_need_food" }
        assertTrue(
            LessonInteractionEngine.isWrongEyeBlink(
                isLeft = true,
                left = 2,
                right = 0,
                blinkOrder = listOf(true, true),
                lesson = food
            )
        )
        val message = LessonInteractionEngine.wrongEyeRestartFeedbackMessage(food, 0)
        assertTrue(message.contains("start again"))
    }

    @Test
    fun foodLesson_fullSequenceMatchesAfterRestart() {
        val food = TrainingLessonCatalog.communicationFundamentals
            .first { it.vocabularyId == "i_need_food" }
        assertTrue(
            LessonInteractionEngine.lessonMatchesGesture(
                food,
                left = 2,
                right = 1,
                blinkOrder = listOf(true, true, false)
            )
        )
        assertNull(
            LessonInteractionEngine.progressLabel(0, 0, emptyList(), food)
        )
    }
}
