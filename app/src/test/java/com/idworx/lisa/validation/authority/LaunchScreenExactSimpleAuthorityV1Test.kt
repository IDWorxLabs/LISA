package com.idworx.lisa.validation.authority

import com.idworx.lisa.features.experiencepolish.launchscreenexactsimple.metadata.LaunchScreenExactSimpleMetadata
import com.idworx.lisa.features.experiencepolish.launchscreenexactsimple.validation.LaunchScreenExactSimpleAuthorityV1
import com.idworx.lisa.validation.ValidationOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LaunchScreenExactSimpleAuthorityV1Test {

    @Test
    fun fullValidation_passesAndEmitsPassToken() {
        val report = LaunchScreenExactSimpleAuthorityV1.validate()
        if (report.outcome != ValidationOutcome.PASS) {
            println("FAILED: ${report.failedChecks}")
        }
        assertEquals("Failed: ${report.failedChecks}", ValidationOutcome.PASS, report.outcome)
        assertEquals(LaunchScreenExactSimpleAuthorityV1.PASS_TOKEN, report.passToken)
        assertTrue(report.failedChecks.isEmpty())
        println(report.formatReport())
        println(LaunchScreenExactSimpleAuthorityV1.PASS_TOKEN)
    }

    @Test
    fun metadata_definesPassToken() {
        assertEquals(
            "LISA_LAUNCH_SCREEN_EXACT_SIMPLE_V1_PASS",
            LaunchScreenExactSimpleMetadata.PASS_TOKEN
        )
    }

    @Test
    fun metadata_definesExactSubtitle() {
        assertTrue(LaunchScreenExactSimpleMetadata.SUBTITLE.contains("primary user"))
        assertTrue(LaunchScreenExactSimpleMetadata.SUBTITLE.contains("using only their eyes"))
    }
}
