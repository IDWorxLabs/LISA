package com.idworx.lisa.validation.authority

import com.idworx.lisa.features.calibrationreliability.model.CalibrationHealthState
import com.idworx.lisa.features.experiencepolish.caregiverconfidence.CaregiverConfidenceEngine
import com.idworx.lisa.features.experiencepolish.caregiverconfidence.metadata.CaregiverConfidenceMetadata
import com.idworx.lisa.features.experiencepolish.caregiverconfidence.validation.CaregiverConfidenceAuthorityV1
import com.idworx.lisa.validation.ValidationOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CaregiverConfidenceAuthorityV1Test {

    @Test
    fun fullValidation_passesAndEmitsPassToken() {
        val report = CaregiverConfidenceAuthorityV1.validate()
        if (report.outcome != ValidationOutcome.PASS) {
            println("FAILED: ${report.failedChecks}")
        }
        assertEquals("Failed: ${report.failedChecks}", ValidationOutcome.PASS, report.outcome)
        assertEquals(CaregiverConfidenceAuthorityV1.PASS_TOKEN, report.passToken)
        assertTrue(report.failedChecks.isEmpty())
        println(report.formatReport())
        println(CaregiverConfidenceAuthorityV1.PASS_TOKEN)
    }

    @Test
    fun setupSupport_providesWhatToDoNow() {
        val support = CaregiverConfidenceEngine.setupSupport(0)
        assertNotNull(support.primaryHint)
        assertNotNull(support.whatToDoNow)
    }

    @Test
    fun trackingRecovery_whenFaceLost() {
        val support = CaregiverConfidenceEngine.communicationSupport(
            facePresent = false,
            calibrationHealth = CalibrationHealthState.Healthy
        )
        assertNotNull(support.primaryHint)
        assertTrue(support.primaryHint!!.contains("face", ignoreCase = true) ||
            support.primaryHint!!.contains("camera", ignoreCase = true) ||
            support.primaryHint!!.contains("view", ignoreCase = true))
    }

    @Test
    fun catalog_hasAllMoments() {
        assertTrue(CaregiverConfidenceEngine.catalogHasAllMoments())
    }

    @Test
    fun metadata_definesPassToken() {
        assertEquals(
            "LISA_CAREGIVER_CONFIDENCE_V1_PASS",
            CaregiverConfidenceMetadata.PASS_TOKEN
        )
    }
}
