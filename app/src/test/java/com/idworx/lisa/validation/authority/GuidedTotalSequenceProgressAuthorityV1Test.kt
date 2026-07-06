package com.idworx.lisa.validation.authority

import com.idworx.lisa.features.guidedtotalsequenceprogress.audit.GuidedTotalSequenceProgressAuditor
import com.idworx.lisa.features.guidedtotalsequenceprogress.metadata.GuidedTotalSequenceProgressMetadata
import com.idworx.lisa.features.guidedtotalsequenceprogress.validation.GuidedTotalSequenceProgressAuthorityV1
import com.idworx.lisa.validation.ValidationOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GuidedTotalSequenceProgressAuthorityV1Test {

    @Test
    fun fullValidation_passesAndEmitsPassToken() {
        val report = GuidedTotalSequenceProgressAuthorityV1.validate()
        if (report.outcome != ValidationOutcome.PASS) {
            println("FAILED: ${report.failedChecks}")
        }
        assertEquals("Failed: ${report.failedChecks}", ValidationOutcome.PASS, report.outcome)
        assertEquals(GuidedTotalSequenceProgressAuthorityV1.PASS_TOKEN, report.passToken)
        assertTrue(report.failedChecks.isEmpty())
        println(report.formatReport())
        println(GuidedTotalSequenceProgressAuthorityV1.PASS_TOKEN)
    }

    @Test
    fun metadata_definesPassToken() {
        assertEquals(
            "LISA_GUIDED_TOTAL_SEQUENCE_PROGRESS_V1_PASS",
            GuidedTotalSequenceProgressMetadata.PASS_TOKEN
        )
    }

    @Test
    fun progress_usesTotalSequenceLength() {
        assertTrue(GuidedTotalSequenceProgressAuditor.progressUsesTotalSequenceLength())
    }

    @Test
    fun total_equalsLeftPlusRightRequirements() {
        assertTrue(GuidedTotalSequenceProgressAuditor.totalEqualsLeftPlusRightRequirements())
    }

    @Test
    fun progress_incrementsOnEveryAcceptedBlink() {
        assertTrue(GuidedTotalSequenceProgressAuditor.progressIncrementsOnEveryAcceptedBlink())
    }

    @Test
    fun waitingLabel_reflectsNextExpectedEye() {
        assertTrue(GuidedTotalSequenceProgressAuditor.waitingLabelReflectsNextExpectedEye())
    }

    @Test
    fun finalAcceptedBlink_showsCompleteSequence() {
        assertTrue(GuidedTotalSequenceProgressAuditor.finalAcceptedBlinkShowsCompleteSequence())
    }

    @Test
    fun existingGuidedLearningValidations_remainGreen() {
        assertTrue(GuidedTotalSequenceProgressAuditor.existingGuidedLearningValidationsRemainGreen())
    }
}
