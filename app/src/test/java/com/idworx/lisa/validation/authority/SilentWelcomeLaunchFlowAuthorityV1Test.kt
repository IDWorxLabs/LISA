package com.idworx.lisa.validation.authority

import com.idworx.lisa.features.silentwelcome.metadata.SilentWelcomeLaunchFlowMetadata
import com.idworx.lisa.features.silentwelcome.validation.SilentWelcomeLaunchFlowAuthorityV1
import com.idworx.lisa.validation.ValidationOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SilentWelcomeLaunchFlowAuthorityV1Test {

    @Test
    fun fullValidation_passesAndEmitsPassToken() {
        val report = SilentWelcomeLaunchFlowAuthorityV1.validate()
        if (report.outcome != ValidationOutcome.PASS) {
            println("FAILED: ${report.failedChecks}")
        }
        assertEquals("Failed: ${report.failedChecks}", ValidationOutcome.PASS, report.outcome)
        assertEquals(SilentWelcomeLaunchFlowAuthorityV1.PASS_TOKEN, report.passToken)
        assertTrue(report.failedChecks.isEmpty())
        println(report.formatReport())
        println(SilentWelcomeLaunchFlowAuthorityV1.PASS_TOKEN)
    }

    @Test
    fun metadata_definesPassToken() {
        assertEquals(
            "LISA_SILENT_WELCOME_LAUNCH_FLOW_V1_PASS",
            SilentWelcomeLaunchFlowMetadata.PASS_TOKEN
        )
    }
}
