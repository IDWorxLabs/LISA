package com.idworx.lisa.validation.authority

import com.idworx.lisa.features.blinkdetectionreliability.BlinkDetectionProcessor
import com.idworx.lisa.features.blinkdetectionreliability.BlinkDetectionTuning
import com.idworx.lisa.features.blinkdetectionreliability.BlinkEyeProbabilities
import com.idworx.lisa.features.blinkdetectionreliability.simulateAcceptedLeftWink
import com.idworx.lisa.features.blinkdetectionreliability.simulateAcceptedRightWink
import com.idworx.lisa.features.blinkdetectionreliability.metadata.BlinkDetectionReliabilityMetadata
import com.idworx.lisa.features.blinkdetectionreliability.validation.BlinkDetectionReliabilityTuningAuthorityV1
import com.idworx.lisa.validation.ValidationOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BlinkDetectionReliabilityTuningAuthorityV1Test {

    @Test
    fun fullValidation_passesAndEmitsPassToken() {
        val report = BlinkDetectionReliabilityTuningAuthorityV1.validate()
        if (report.outcome != ValidationOutcome.PASS) {
            println("FAILED: ${report.failedChecks}")
        }
        assertEquals("Failed: ${report.failedChecks}", ValidationOutcome.PASS, report.outcome)
        assertEquals(BlinkDetectionReliabilityTuningAuthorityV1.PASS_TOKEN, report.passToken)
        assertTrue(report.failedChecks.isEmpty())
        println(report.formatReport())
        println(BlinkDetectionReliabilityTuningAuthorityV1.PASS_TOKEN)
    }

    @Test
    fun processor_manualOpenCloseOpenSequence() {
        val tuning = BlinkDetectionTuning(
            closedEyeThreshold = 0.35f,
            openEyeThreshold = 0.65f,
            requiredWinkFrames = 1,
            openPrimingFrames = 1,
            cooldownMs = 0L
        )
        val processor = BlinkDetectionProcessor(tuning)
        val open = BlinkEyeProbabilities(0.92f, 0.92f)
        val closed = BlinkEyeProbabilities(0.08f, 0.92f)
        val r1 = processor.processFrame(open, 0L, 0, 0)
        val r2 = processor.processFrame(closed, 16L, 0, 0)
        val r3 = processor.processFrame(open, 32L, 0, 0)
        assertEquals(1, r2.leftStreak)
        assertTrue(r2.leftCandidate)
        assertTrue(r3.acceptLeft)
    }

    @Test
    fun processor_acceptsQuickLeftWink() {
        val tuning = BlinkDetectionTuning(
            closedEyeThreshold = 0.35f,
            openEyeThreshold = 0.65f,
            requiredWinkFrames = 1,
            openPrimingFrames = 1,
            cooldownMs = 0L
        )
        val processor = BlinkDetectionProcessor(tuning)
        val result = processor.simulateAcceptedLeftWink(tuning, nowMs = 500L)
        assertTrue(result.acceptLeft)
    }

    @Test
    fun processor_allowsSecondSameSideBlinkAfterCooldown() {
        val tuning = BlinkDetectionTuning(
            closedEyeThreshold = 0.35f,
            openEyeThreshold = 0.65f,
            requiredWinkFrames = 1,
            openPrimingFrames = 1,
            cooldownMs = BlinkDetectionTuning.WINK_COOLDOWN_MS
        )
        val processor = BlinkDetectionProcessor(tuning)
        val first = processor.simulateAcceptedRightWink(tuning, nowMs = 1000L)
        assertTrue(first.acceptRight)
        val second = processor.simulateAcceptedRightWink(
            tuning,
            nowMs = 1000L + BlinkDetectionTuning.WINK_COOLDOWN_MS + 20L,
            acceptedRight = 1
        )
        assertTrue(second.acceptRight)
    }

    @Test
    fun metadata_definesPassToken() {
        assertEquals(
            "LISA_BLINK_DETECTION_RELIABILITY_TUNING_V1_PASS",
            BlinkDetectionReliabilityMetadata.PASS_TOKEN
        )
    }
}
