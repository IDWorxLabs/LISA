package com.idworx.lisa.validation.authority

import com.idworx.lisa.features.experiencepolish.guidedlearningsimplification.metadata.GuidedLearningSimplificationMetadata
import com.idworx.lisa.features.experiencepolish.guidedlearningsimplification.validation.GuidedLearningSimplificationAuthorityV1
import com.idworx.lisa.features.onboardingguide.ui.formatWinkGestureFriendly
import com.idworx.lisa.features.onboardingguide.ui.formatWinkInstruction
import com.idworx.lisa.validation.ValidationOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GuidedLearningSimplificationAuthorityV1Test {

    @Test
    fun fullValidation_passesAndEmitsPassToken() {
        val report = GuidedLearningSimplificationAuthorityV1.validate()
        if (report.outcome != ValidationOutcome.PASS) {
            println("FAILED: ${report.failedChecks}")
        }
        assertEquals("Failed: ${report.failedChecks}", ValidationOutcome.PASS, report.outcome)
        assertEquals(GuidedLearningSimplificationAuthorityV1.PASS_TOKEN, report.passToken)
        assertTrue(report.failedChecks.isEmpty())
        println(report.formatReport())
        println(GuidedLearningSimplificationAuthorityV1.PASS_TOKEN)
    }

    @Test
    fun gestureFriendly_usesPlainLanguage() {
        assertEquals("Blink Left Twice", formatWinkGestureFriendly(2, 0))
        assertFalse(formatWinkGestureFriendly(2, 0).contains("L2"))
    }

    @Test
    fun instruction_isSimple() {
        assertTrue(formatWinkInstruction(2, 0).contains("blink left twice"))
    }

    @Test
    fun metadata_definesPassToken() {
        assertEquals(
            "LISA_GUIDED_LEARNING_SIMPLIFICATION_V1_PASS",
            GuidedLearningSimplificationMetadata.PASS_TOKEN
        )
    }
}
