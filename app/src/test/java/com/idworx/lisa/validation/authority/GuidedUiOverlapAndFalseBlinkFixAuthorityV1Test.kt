package com.idworx.lisa.validation.authority

import com.idworx.lisa.features.guideduioverlapandfalseblinkfix.metadata.GuidedUiOverlapAndFalseBlinkFixMetadata
import com.idworx.lisa.features.guideduioverlapandfalseblinkfix.validation.GuidedUiOverlapAndFalseBlinkFixAuthorityV1
import com.idworx.lisa.features.onboardingguide.ui.guidedLessonPhraseFontSize
import com.idworx.lisa.validation.ValidationOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GuidedUiOverlapAndFalseBlinkFixAuthorityV1Test {

    @Test
    fun fullValidation_passesAndEmitsPassToken() {
        val report = GuidedUiOverlapAndFalseBlinkFixAuthorityV1.validate()
        if (report.outcome != ValidationOutcome.PASS) {
            println("FAILED: ${report.failedChecks}")
        }
        assertEquals("Failed: ${report.failedChecks}", ValidationOutcome.PASS, report.outcome)
        assertEquals(GuidedUiOverlapAndFalseBlinkFixAuthorityV1.PASS_TOKEN, report.passToken)
        assertTrue(report.failedChecks.isEmpty())
        println(report.formatReport())
        println(GuidedUiOverlapAndFalseBlinkFixAuthorityV1.PASS_TOKEN)
    }

    @Test
    fun metadata_definesPassToken() {
        assertEquals(
            "LISA_GUIDED_UI_OVERLAP_AND_FALSE_BLINK_FIX_V1_PASS",
            GuidedUiOverlapAndFalseBlinkFixMetadata.PASS_TOKEN
        )
    }

    @Test
    fun longPhrase_usesResponsiveFont() {
        val fontSize = guidedLessonPhraseFontSize("I WANT TO LIE DOWN")
        assertTrue(fontSize.value <= 28f)
    }
}
