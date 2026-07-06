package com.idworx.lisa.validation.authority

import com.idworx.lisa.features.guidedsuccesstimingfix.metadata.GuidedSuccessTimingFixMetadata
import com.idworx.lisa.features.guidedsuccesstimingfix.validation.GuidedSuccessTimingFixAuthorityV1
import com.idworx.lisa.validation.ValidationOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GuidedSuccessTimingFixAuthorityV1Test {

    @Test
    fun fullValidation_passesAndEmitsPassToken() {
        val report = GuidedSuccessTimingFixAuthorityV1.validate()
        if (report.outcome != ValidationOutcome.PASS) {
            println("FAILED: ${report.failedChecks}")
        }
        assertEquals("Failed: ${report.failedChecks}", ValidationOutcome.PASS, report.outcome)
        assertEquals(GuidedSuccessTimingFixAuthorityV1.PASS_TOKEN, report.passToken)
        assertTrue(report.failedChecks.isEmpty())
        println(report.formatReport())
        println(GuidedSuccessTimingFixAuthorityV1.PASS_TOKEN)
    }

    @Test
    fun metadata_definesPassToken() {
        assertEquals(
            "LISA_GUIDED_SUCCESS_TIMING_FIX_V1_PASS",
            GuidedSuccessTimingFixMetadata.PASS_TOKEN
        )
    }

    @Test
    fun metadata_definesGradleTask() {
        assertEquals(
            "validateLisaGuidedSuccessTimingFixV1",
            GuidedSuccessTimingFixMetadata.GRADLE_TASK
        )
    }
}
