package com.idworx.lisa.validation.authority

import com.idworx.lisa.features.guidedpreferencesgestureconsistency.audit.GuidedPreferencesGestureConsistencyAuditor
import com.idworx.lisa.features.guidedpreferencesgestureconsistency.metadata.GuidedPreferencesGestureConsistencyMetadata
import com.idworx.lisa.features.guidedpreferencesgestureconsistency.validation.GuidedPreferencesGestureConsistencyAuthorityV1
import com.idworx.lisa.validation.ValidationOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GuidedPreferencesGestureConsistencyAuthorityV1Test {

    @Test
    fun fullValidation_passesAndEmitsPassToken() {
        val report = GuidedPreferencesGestureConsistencyAuthorityV1.validate()
        if (report.outcome != ValidationOutcome.PASS) {
            println("FAILED: ${report.failedChecks}")
        }
        assertEquals("Failed: ${report.failedChecks}", ValidationOutcome.PASS, report.outcome)
        assertEquals(GuidedPreferencesGestureConsistencyAuthorityV1.PASS_TOKEN, report.passToken)
        assertTrue(report.failedChecks.isEmpty())
        println(report.formatReport())
        println(GuidedPreferencesGestureConsistencyAuthorityV1.PASS_TOKEN)
    }

    @Test
    fun metadata_definesPassToken() {
        assertEquals(
            "LISA_GUIDED_PREFERENCES_GESTURE_CONSISTENCY_V1_PASS",
            GuidedPreferencesGestureConsistencyMetadata.PASS_TOKEN
        )
    }

    @Test
    fun preferencesGestures_matchExecutableActions() {
        assertTrue(GuidedPreferencesGestureConsistencyAuditor.preferencesGesturesMatchExecutableActions())
    }

    @Test
    fun noHardcodedGestureLiterals_remainInPreferencesPanel() {
        assertTrue(GuidedPreferencesGestureConsistencyAuditor.noHardcodedGestureLiteralsRemainInPreferencesPanel())
    }

    @Test
    fun preferencesLabels_deriveFromSharedGestureAuthority() {
        assertTrue(GuidedPreferencesGestureConsistencyAuditor.preferencesLabelsDeriveFromSharedGestureAuthority())
    }

    @Test
    fun preferencesLabel_isAPureFunctionOfTheSharedConstant() {
        assertTrue(GuidedPreferencesGestureConsistencyAuditor.preferencesLabelIsAPureFunctionOfTheSharedConstant())
    }

    @Test
    fun quickControlsGestureLabels_matchSharedAuthority() {
        assertTrue(GuidedPreferencesGestureConsistencyAuditor.quickControlsGestureLabelsMatchSharedAuthority())
    }

    @Test
    fun productionUiFiles_freeOfHardcodedGestureLiterals() {
        assertTrue(GuidedPreferencesGestureConsistencyAuditor.productionUiFilesFreeOfHardcodedGestureLiterals())
    }
}
