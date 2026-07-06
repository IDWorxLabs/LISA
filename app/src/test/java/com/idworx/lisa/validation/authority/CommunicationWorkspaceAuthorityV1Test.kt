package com.idworx.lisa.validation.authority

import com.idworx.lisa.features.experiencepolish.communicationworkspace.metadata.CommunicationWorkspaceMetadata
import com.idworx.lisa.features.experiencepolish.communicationworkspace.validation.CommunicationWorkspaceAuthorityV1
import com.idworx.lisa.validation.ValidationOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CommunicationWorkspaceAuthorityV1Test {

    @Test
    fun fullValidation_passesAndEmitsPassToken() {
        val report = CommunicationWorkspaceAuthorityV1.validate()
        if (report.outcome != ValidationOutcome.PASS) {
            println("FAILED: ${report.failedChecks}")
        }
        assertEquals("Failed: ${report.failedChecks}", ValidationOutcome.PASS, report.outcome)
        assertEquals(CommunicationWorkspaceAuthorityV1.PASS_TOKEN, report.passToken)
        assertTrue(report.failedChecks.isEmpty())
        println(report.formatReport())
        println(CommunicationWorkspaceAuthorityV1.PASS_TOKEN)
    }

    @Test
    fun metadata_definesPassToken() {
        assertEquals(
            "LISA_EXPERIENCE_PHASE_B_COMMUNICATION_WORKSPACE_V1_PASS",
            CommunicationWorkspaceMetadata.PASS_TOKEN
        )
    }
}
