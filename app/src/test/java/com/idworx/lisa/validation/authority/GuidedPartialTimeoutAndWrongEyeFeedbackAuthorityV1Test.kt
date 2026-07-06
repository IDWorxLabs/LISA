package com.idworx.lisa.validation.authority

import com.idworx.lisa.features.guidedpartialtimeoutandwrongeyefeedback.metadata.GuidedPartialTimeoutAndWrongEyeFeedbackMetadata
import com.idworx.lisa.features.guidedpartialtimeoutandwrongeyefeedback.validation.GuidedPartialTimeoutAndWrongEyeFeedbackAuthorityV1
import com.idworx.lisa.features.onboardingguide.lessoninteraction.LessonInteractionEngine
import com.idworx.lisa.features.onboardingguide.lessons.TrainingLessonCatalog
import com.idworx.lisa.validation.ValidationOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GuidedPartialTimeoutAndWrongEyeFeedbackAuthorityV1Test {

    @Test
    fun fullValidation_passesAndEmitsPassToken() {
        val report = GuidedPartialTimeoutAndWrongEyeFeedbackAuthorityV1.validate()
        if (report.outcome != ValidationOutcome.PASS) {
            println("FAILED: ${report.failedChecks}")
        }
        assertEquals("Failed: ${report.failedChecks}", ValidationOutcome.PASS, report.outcome)
        assertEquals(GuidedPartialTimeoutAndWrongEyeFeedbackAuthorityV1.PASS_TOKEN, report.passToken)
        assertTrue(report.failedChecks.isEmpty())
        println(report.formatReport())
        println(GuidedPartialTimeoutAndWrongEyeFeedbackAuthorityV1.PASS_TOKEN)
    }

    @Test
    fun metadata_definesPassToken() {
        assertEquals(
            "LISA_GUIDED_PARTIAL_TIMEOUT_AND_WRONG_EYE_FEEDBACK_V1_PASS",
            GuidedPartialTimeoutAndWrongEyeFeedbackMetadata.PASS_TOKEN
        )
    }

    @Test
    fun wrongEye_detectsLeftBlinkOnYesLesson() {
        val yes = TrainingLessonCatalog.communicationLessonAt(1)!!
        assertTrue(
            LessonInteractionEngine.isWrongEyeBlink(
                isLeft = true,
                left = 0,
                right = 0,
                blinkOrder = emptyList(),
                lesson = yes
            )
        )
        assertFalse(
            LessonInteractionEngine.isWrongEyeBlink(
                isLeft = false,
                left = 0,
                right = 0,
                blinkOrder = emptyList(),
                lesson = yes
            )
        )
    }

    @Test
    fun partialTimeout_usesFiveSecondIdle() {
        assertEquals(5_000L, LessonInteractionEngine.PARTIAL_SEQUENCE_IDLE_MS)
    }
}
