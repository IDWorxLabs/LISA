package com.idworx.lisa.validation.authority

import com.idworx.lisa.features.guidedremoveredundanthelpertext.audit.GuidedRemoveRedundantHelperTextAuditor
import com.idworx.lisa.features.guidedremoveredundanthelpertext.metadata.GuidedRemoveRedundantHelperTextMetadata
import com.idworx.lisa.features.guidedremoveredundanthelpertext.validation.GuidedRemoveRedundantHelperTextAuthorityV1
import com.idworx.lisa.validation.ValidationOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GuidedRemoveRedundantHelperTextAuthorityV1Test {

    @Test
    fun fullValidation_passesAndEmitsPassToken() {
        val report = GuidedRemoveRedundantHelperTextAuthorityV1.validate()
        if (report.outcome != ValidationOutcome.PASS) {
            println("FAILED: ${report.failedChecks}")
        }
        assertEquals("Failed: ${report.failedChecks}", ValidationOutcome.PASS, report.outcome)
        assertEquals(GuidedRemoveRedundantHelperTextAuthorityV1.PASS_TOKEN, report.passToken)
        assertTrue(report.failedChecks.isEmpty())
        println(report.formatReport())
        println(GuidedRemoveRedundantHelperTextAuthorityV1.PASS_TOKEN)
    }

    @Test
    fun metadata_definesPassToken() {
        assertEquals(
            "LISA_GUIDED_REMOVE_REDUNDANT_HELPER_TEXT_V1_PASS",
            GuidedRemoveRedundantHelperTextMetadata.PASS_TOKEN
        )
    }

    @Test
    fun noCommunicationLesson_rendersHelperSentence() {
        assertTrue(GuidedRemoveRedundantHelperTextAuditor.noCommunicationLessonRendersHelperSentence())
    }

    @Test
    fun noNavigationLesson_rendersHelperSentence() {
        assertTrue(GuidedRemoveRedundantHelperTextAuditor.noNavigationLessonRendersHelperSentence())
    }

    @Test
    fun removal_isSystemWideNotPerLesson() {
        assertTrue(GuidedRemoveRedundantHelperTextAuditor.removalIsSystemWideNotPerLesson())
    }

    @Test
    fun gestureInstruction_remainsVisible() {
        assertTrue(GuidedRemoveRedundantHelperTextAuditor.gestureInstructionRemainsVisible())
    }

    @Test
    fun phraseTitle_remainsVisible() {
        assertTrue(GuidedRemoveRedundantHelperTextAuditor.phraseTitleRemainsVisible())
    }

    @Test
    fun lessonLayout_remainsVerticallyBalanced() {
        assertTrue(GuidedRemoveRedundantHelperTextAuditor.lessonLayoutRemainsVerticallyBalanced())
    }

    @Test
    fun existingGuidedLearningFunctionality_isUnchanged() {
        assertTrue(GuidedRemoveRedundantHelperTextAuditor.existingGuidedLearningFunctionalityUnchanged())
    }
}
