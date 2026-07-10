package com.idworx.lisa.validation.authority

import com.idworx.lisa.features.sequencetimingpolicy.audit.SequenceTimingPolicyAuditor
import com.idworx.lisa.features.sequencetimingpolicy.metadata.SequenceTimingPolicyMetadata
import com.idworx.lisa.features.sequencetimingpolicy.validation.SequenceTimingPolicyAuthorityV1
import com.idworx.lisa.validation.ValidationOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SequenceTimingPolicyAuthorityV1Test {

    @Test
    fun fullValidation_passesAndEmitsPassToken() {
        val report = SequenceTimingPolicyAuthorityV1.validate()
        if (report.outcome != ValidationOutcome.PASS) {
            println("FAILED: ${report.failedChecks}")
        }
        assertEquals("Failed: ${report.failedChecks}", ValidationOutcome.PASS, report.outcome)
        assertEquals(SequenceTimingPolicyAuthorityV1.PASS_TOKEN, report.passToken)
        assertTrue(report.failedChecks.isEmpty())
        println(report.formatReport())
        println(SequenceTimingPolicyAuthorityV1.PASS_TOKEN)
    }

    @Test
    fun metadata_definesPassToken() {
        assertEquals("LISA_SEQUENCE_TIMING_POLICY_V1_PASS", SequenceTimingPolicyMetadata.PASS_TOKEN)
    }

    @Test
    fun defaultResponseTime_isFiveSecondsEverywhere() {
        assertTrue(SequenceTimingPolicyAuditor.defaultResponseTimeIsFiveSecondsEverywhere())
    }

    @Test
    fun noHardcodedThreeSecondLiteral_orSeparatePartialTimeoutConstant_remains() {
        assertTrue(SequenceTimingPolicyAuditor.noHardcodedThreeSecondMillisecondLiteralRemains())
    }

    @Test
    fun quickResolveIdleConstant_noLongerExistsAnywhere() {
        assertTrue(SequenceTimingPolicyAuditor.noQuickResolveIdleConstantRemainsAnywhere())
    }

    @Test
    fun quickResolveFunction_noLongerExistsInMainActivity() {
        assertTrue(SequenceTimingPolicyAuditor.noQuickResolveFunctionRemainsInMainActivity())
    }

    @Test
    fun finalizeDecision_isGatedSolelyByShouldFinalizeSequence() {
        assertTrue(SequenceTimingPolicyAuditor.finalizeIsGatedSolelyByShouldFinalizeSequence())
    }

    @Test
    fun emergency_hasNoShortCircuitBeforeTheIdleTimeoutGate() {
        assertTrue(SequenceTimingPolicyAuditor.emergencyHasNoShortCircuitBeforeTheIdleTimeoutGate())
    }

    @Test
    fun stop_doesNotTriggerYesEarly_bothWaitForTheFullIdleWindow() {
        assertTrue(SequenceTimingPolicyAuditor.stopDoesNotTriggerYesEarly_bothWaitForTheFullIdleWindow())
    }

    @Test
    fun emergencyBuildUp_doesNotTriggerShorterNavActionsEarly() {
        assertTrue(SequenceTimingPolicyAuditor.emergencyBuildUpDoesNotTriggerShorterNavActionsEarly())
    }

    @Test
    fun confirmAndCancel_waitForTheFullIdleWindow() {
        assertTrue(SequenceTimingPolicyAuditor.confirmAndCancelWaitForTheFullIdleWindow())
    }

    @Test
    fun everyNewBlink_restartsTheIdleTimer() {
        assertTrue(SequenceTimingPolicyAuditor.everyNewBlinkRestartsTheIdleTimer())
    }

    @Test
    fun hiddenPhrasePageEntries_neverExecute() {
        assertTrue(SequenceTimingPolicyAuditor.hiddenPhrasePageEntryNeverExecutes())
    }

    @Test
    fun emergency_neverCutOffAcrossFiveSecondGaps() {
        assertTrue(SequenceTimingPolicyAuditor.emergencySequenceNeverCutOffAcrossFiveSecondGapsBetweenEachBlink())
    }

    @Test
    fun confirmAndCancel_remainOrderSensitive() {
        assertTrue(SequenceTimingPolicyAuditor.confirmAndCancelRemainOrderSensitive())
    }

    @Test
    fun guidedTraining_usesItsOwnAdjustableDelay_sourcedFromTheSamePolicy() {
        assertTrue(SequenceTimingPolicyAuditor.guidedLearningAndWorkspaceShareTheSamePolicyObject())
        assertTrue(SequenceTimingPolicyAuditor.guidedTrainingUsesItsOwnAdjustableDelayNotAHardcodedOne())
    }

    @Test
    fun responseTimeControls_updateTheActualRuntimeIdleTimeout() {
        assertTrue(SequenceTimingPolicyAuditor.responseTimeControlsUpdateTheActualRuntimeIdleTimeout())
    }

    @Test
    fun everyFinalizeDecision_usesTheAuthoritativeIdleTimeout() {
        assertTrue(SequenceTimingPolicyAuditor.mainActivityUsesTheAuthoritativeIdleTimeoutAtEveryFinalizeDecision())
    }

    @Test
    fun testClassAndGradleTask_areRegistered() {
        assertTrue(SequenceTimingPolicyAuditor.testClassExists())
        assertTrue(SequenceTimingPolicyAuditor.gradleTaskRegistered())
    }
}
