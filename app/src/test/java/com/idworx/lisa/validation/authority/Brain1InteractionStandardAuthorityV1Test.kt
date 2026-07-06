package com.idworx.lisa.validation.authority

import com.idworx.lisa.features.brain1interactionstandard.metadata.Brain1InteractionStandardMetadata
import com.idworx.lisa.features.brain1interactionstandard.model.UniversalInteractionGestures
import com.idworx.lisa.features.brain1interactionstandard.validation.Brain1InteractionStandardAuthorityV1
import com.idworx.lisa.features.onboardingguide.lessons.TrainingLessonCatalog
import com.idworx.lisa.validation.ValidationOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Brain1InteractionStandardAuthorityV1Test {

    @Test
    fun fullValidation_passesAndEmitsPassToken() {
        val report = Brain1InteractionStandardAuthorityV1.validate()
        if (report.outcome != ValidationOutcome.PASS) {
            println("FAILED: ${report.failedChecks}")
        }
        assertEquals("Failed: ${report.failedChecks}", ValidationOutcome.PASS, report.outcome)
        assertEquals(Brain1InteractionStandardAuthorityV1.PASS_TOKEN, report.passToken)
        assertTrue(report.failedChecks.isEmpty())
        println(report.formatReport())
        println(Brain1InteractionStandardAuthorityV1.PASS_TOKEN)
    }

    @Test
    fun metadata_definesNoSingleBlinkRule() {
        assertTrue(Brain1InteractionStandardMetadata.NO_SINGLE_BLINK_RULE.contains("single blink"))
        assertEquals(2, UniversalInteractionGestures.MIN_COMMAND_BLINKS)
    }

    @Test
    fun earliestLessons_areLevelOne() {
        assertTrue(TrainingLessonCatalog.earliestLessonsUseSimpleGestures())
    }
}
