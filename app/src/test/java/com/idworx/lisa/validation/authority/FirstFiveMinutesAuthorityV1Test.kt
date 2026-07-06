package com.idworx.lisa.validation.authority

import com.idworx.lisa.features.experiencepolish.firstfiveminutes.metadata.FirstFiveMinutesMetadata
import com.idworx.lisa.features.experiencepolish.firstfiveminutes.validation.FirstFiveMinutesAuthorityV1
import com.idworx.lisa.features.onboardingguide.lessons.TrainingLessonCatalog
import com.idworx.lisa.validation.ValidationOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FirstFiveMinutesAuthorityV1Test {

    @Test
    fun fullValidation_passesAndEmitsPassToken() {
        val report = FirstFiveMinutesAuthorityV1.validate()
        if (report.outcome != ValidationOutcome.PASS) {
            println("FAILED: ${report.failedChecks}")
        }
        assertEquals("Failed: ${report.failedChecks}", ValidationOutcome.PASS, report.outcome)
        assertEquals(FirstFiveMinutesAuthorityV1.PASS_TOKEN, report.passToken)
        assertTrue(report.failedChecks.isEmpty())
        println(report.formatReport())
        println(FirstFiveMinutesAuthorityV1.PASS_TOKEN)
    }

    @Test
    fun hello_usesL2() {
        val hello = TrainingLessonCatalog.communicationFundamentals.first()
        assertEquals(2, hello.left)
        assertEquals(0, hello.right)
    }

    @Test
    fun metadata_definesPassToken() {
        assertEquals(
            "LISA_EXPERIENCE_PHASE_A_FIRST_FIVE_MINUTES_V1_PASS",
            FirstFiveMinutesMetadata.PASS_TOKEN
        )
    }
}
