package com.idworx.lisa.validation.authority

import com.idworx.lisa.features.gestureduplicateauditandguidedsensitivity.metadata.GestureDuplicateAuditAndGuidedSensitivityMetadata
import com.idworx.lisa.features.gestureduplicateauditandguidedsensitivity.validation.GestureDuplicateAuditAndGuidedSensitivityAuthorityV1
import com.idworx.lisa.features.gesturesequenceaudit.GestureSequenceAuditEngine
import com.idworx.lisa.features.onboardingguide.lessons.TrainingLessonCatalog
import com.idworx.lisa.validation.ValidationOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GestureDuplicateAuditAndGuidedSensitivityAuthorityV1Test {

    @Test
    fun fullValidation_passesAndEmitsPassToken() {
        val report = GestureDuplicateAuditAndGuidedSensitivityAuthorityV1.validate()
        if (report.outcome != ValidationOutcome.PASS) {
            println("FAILED: ${report.failedChecks}")
        }
        assertEquals("Failed: ${report.failedChecks}", ValidationOutcome.PASS, report.outcome)
        assertEquals(GestureDuplicateAuditAndGuidedSensitivityAuthorityV1.PASS_TOKEN, report.passToken)
        assertTrue(report.failedChecks.isEmpty())
        println(report.formatReport())
        println(GestureDuplicateAuditAndGuidedSensitivityAuthorityV1.PASS_TOKEN)
    }

    @Test
    fun noAndPlease_haveDistinctSequences() {
        val no = TrainingLessonCatalog.communicationFundamentals.first { it.vocabularyId == "no" }
        val please = TrainingLessonCatalog.communicationFundamentals.first { it.vocabularyId == "please" }
        assertEquals(1, no.left)
        assertEquals(1, no.right)
        assertEquals(1, please.left)
        assertEquals(2, please.right)
        assertNotEquals(no.left to no.right, please.left to please.right)
    }

    @Test
    fun gestureAudit_essentialsAreUnique() {
        val report = GestureSequenceAuditEngine.auditAll()
        assertTrue(report.guidedEssentialsUnique)
        assertTrue(report.noAndPleaseDistinct)
    }

    @Test
    fun metadata_definesPassToken() {
        assertEquals(
            "LISA_GESTURE_DUPLICATE_AUDIT_AND_GUIDED_SENSITIVITY_V1_PASS",
            GestureDuplicateAuditAndGuidedSensitivityMetadata.PASS_TOKEN
        )
    }
}
