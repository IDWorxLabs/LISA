package com.idworx.lisa.validation.authority

import com.idworx.lisa.features.guidedprogresswordingpolish.audit.GuidedProgressWordingPolishAuditor
import com.idworx.lisa.features.guidedprogresswordingpolish.metadata.GuidedProgressWordingPolishMetadata
import com.idworx.lisa.features.guidedprogresswordingpolish.validation.GuidedProgressWordingPolishAuthorityV1
import com.idworx.lisa.validation.ValidationOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GuidedProgressWordingPolishAuthorityV1Test {

    @Test
    fun fullValidation_passesAndEmitsPassToken() {
        val report = GuidedProgressWordingPolishAuthorityV1.validate()
        if (report.outcome != ValidationOutcome.PASS) {
            println("FAILED: ${report.failedChecks}")
        }
        assertEquals("Failed: ${report.failedChecks}", ValidationOutcome.PASS, report.outcome)
        assertEquals(GuidedProgressWordingPolishAuthorityV1.PASS_TOKEN, report.passToken)
        assertTrue(report.failedChecks.isEmpty())
        println(report.formatReport())
        println(GuidedProgressWordingPolishAuthorityV1.PASS_TOKEN)
    }

    @Test
    fun metadata_definesPassToken() {
        assertEquals(
            "LISA_GUIDED_PROGRESS_WORDING_POLISH_V1_PASS",
            GuidedProgressWordingPolishMetadata.PASS_TOKEN
        )
    }

    @Test
    fun progress_displaysXOfYBlinksWording() {
        assertTrue(GuidedProgressWordingPolishAuditor.progressDisplaysXOfYBlinksWording())
    }

    @Test
    fun totalBlinkCount_remainsCorrect() {
        assertTrue(GuidedProgressWordingPolishAuditor.totalBlinkCountRemainsCorrect())
    }

    @Test
    fun progress_incrementsCorrectlyAfterEveryAcceptedBlink() {
        assertTrue(GuidedProgressWordingPolishAuditor.progressIncrementsCorrectlyAfterEveryAcceptedBlink())
    }

    @Test
    fun waitingLabel_remainsUnchanged() {
        assertTrue(GuidedProgressWordingPolishAuditor.waitingLabelRemainsUnchanged())
    }

    @Test
    fun sequenceComplete_stillAppearsAfterFinalBlink() {
        assertTrue(GuidedProgressWordingPolishAuditor.sequenceCompleteStillAppearsAfterFinalBlink())
    }

    @Test
    fun existingGuidedLearningValidations_remainGreen() {
        assertTrue(GuidedProgressWordingPolishAuditor.existingGuidedLearningValidationsRemainGreen())
    }
}
