package com.idworx.lisa.features.eyediagnostic

import com.idworx.lisa.features.blinkdetectionreliability.BlinkDetectionProcessor
import com.idworx.lisa.features.blinkdetectionreliability.BlinkDetectionTuning
import com.idworx.lisa.features.blinkdetectionreliability.BlinkEyeProbabilities
import com.idworx.lisa.features.blinkdetectionreliability.BlinkProcessResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Focused proofs for temporary glasses eye diagnostics.
 * Diagnostics must observe only — never alter wink decisions or execute sequences.
 */
class LisaEyeDiagnosticTest {

    @Test
    fun releaseBuildLogger_doesNotEmitDiagnosticEyeData() {
        val emitted = mutableListOf<String>()
        val logger = LisaEyeDiagnostic.Logger(
            enabled = false, // release: BuildConfig.DEBUG == false
            minIntervalMs = 0L,
            sink = { _, message -> emitted += message }
        )
        logger.maybeEmit(sample(left = 0.9f, right = 0.1f, eitherNull = false))
        logger.maybeEmit(sample(left = null, right = null, eitherNull = true))
        assertTrue(emitted.isEmpty())
        assertFalse(logger.isEnabled())
    }

    @Test
    fun nullEyeProbabilities_areReportedCorrectly() {
        val sample = sample(
            left = null,
            right = 0.82f,
            eitherNull = true,
            rejectionReason = "null_eye_probabilities",
            frameAccepted = false
        )
        assertEquals(LisaEyeDiagnostic.InterpretedEyeState.NULL, sample.interpretedLeftEyeState)
        assertEquals(
            LisaEyeDiagnostic.InterpretedEyeState.OPEN,
            LisaEyeDiagnostic.interpretEyeState(0.82f, closedThreshold = 0.25f, openThreshold = 0.75f)
        )
        val line = LisaEyeDiagnostic.format(sample)
        assertTrue(line.contains("Lprob=null"))
        assertTrue(line.contains("nullProb=true"))
        assertTrue(line.contains("Lstate=NULL"))
        assertTrue(line.contains("reason=null_eye_probabilities"))
        assertTrue(line.contains("frame=rejected"))
    }

    @Test
    fun format_includesInterpretedStatesAndRejectionReasons() {
        val line = LisaEyeDiagnostic.format(
            sample(
                left = 0.12f,
                right = 0.88f,
                eitherNull = false,
                leftState = LisaEyeDiagnostic.InterpretedEyeState.CLOSED,
                rightState = LisaEyeDiagnostic.InterpretedEyeState.OPEN,
                rejectionReason = "jitter_skip",
                frameAccepted = false,
                sequenceState = "L1R0|BuildingMessage|cd=false|em=false"
            )
        )
        assertTrue(line.contains("Lstate=CLOSED"))
        assertTrue(line.contains("Rstate=OPEN"))
        assertTrue(line.contains("reason=jitter_skip"))
        assertTrue(line.contains("frame=rejected"))
        assertTrue(line.contains("Lwinks=1"))
        assertTrue(line.contains("Rwinks=0"))
        assertTrue(line.contains("seq=L1R0|BuildingMessage|cd=false|em=false"))
        assertTrue(line.contains("sens=3"))
        assertTrue(line.contains("Lthr="))
        assertTrue(line.contains("Rthr="))
        assertFalse(line.contains("http"))
        assertFalse(line.contains("@"))
        assertFalse(line.contains("password"))
    }

    @Test
    fun diagnostics_doNotChangeProductionEyeStateDecisions() {
        val tuning = BlinkDetectionTuning(
            closedEyeThreshold = 0.35f,
            openEyeThreshold = 0.65f,
            requiredWinkFrames = 1,
            openPrimingFrames = 1,
            cooldownMs = 0L
        )
        val processor = BlinkDetectionProcessor(tuning)
        val open = BlinkEyeProbabilities(0.92f, 0.92f)
        val leftClose = BlinkEyeProbabilities(0.08f, 0.92f)

        processor.processFrame(open, 0L, 0, 0)
        processor.processFrame(leftClose, 16L, 0, 0)
        val accept = processor.processFrame(open, 32L, 0, 0)
        assertTrue(accept.acceptLeft)

        // Observational helpers must not mutate processor outcome on a fresh identical path.
        val processor2 = BlinkDetectionProcessor(tuning)
        processor2.processFrame(open, 0L, 0, 0)
        val mid = processor2.processFrame(leftClose, 16L, 0, 0)
        val reason = LisaEyeDiagnostic.rejectionReasonFrom(mid)
        val leftState = LisaEyeDiagnostic.interpretEyeState(0.08f, 0.35f, 0.65f)
        val rightState = LisaEyeDiagnostic.interpretEyeState(0.92f, 0.35f, 0.65f)
        assertEquals(LisaEyeDiagnostic.InterpretedEyeState.CLOSED, leftState)
        assertEquals(LisaEyeDiagnostic.InterpretedEyeState.OPEN, rightState)
        // Soft/hard reason mapping must not alter the next production decision:
        val accept2 = processor2.processFrame(open, 32L, 0, 0)
        assertEquals(accept.acceptLeft, accept2.acceptLeft)
        assertEquals(accept.acceptRight, accept2.acceptRight)
        assertTrue(reason == null || reason == "unprimed_close" || reason == "incomplete_wink_shape")
    }

    @Test
    fun diagnostics_doNotExecuteGesturesOrSequencesThemselves() {
        var gestureCalls = 0
        val logger = LisaEyeDiagnostic.Logger(
            enabled = true,
            minIntervalMs = 0L,
            clockMs = { 1_000L },
            sink = { _, _ ->
                // Logger sink is observational text only — no gesture hook exists.
                gestureCalls += 0
            }
        )
        repeat(3) {
            logger.maybeEmit(
                sample(
                    left = 0.1f,
                    right = 0.9f,
                    eitherNull = false,
                    leftWinkCount = 0,
                    rightWinkCount = 0,
                    sequenceState = "idle"
                )
            )
        }
        assertEquals(0, gestureCalls)
        // Formatting / interpretation APIs have no side-effect callbacks.
        LisaEyeDiagnostic.interpretEyeState(null, 0.2f, 0.8f)
        LisaEyeDiagnostic.rejectionReasonFrom(
            BlinkProcessResult(skippedForJitter = true, skippedUnstable = true)
        )
        assertEquals(0, gestureCalls)
    }

    @Test
    fun rateLimit_emitsAboutFourToFiveSamplesPerSecond() {
        val emitted = mutableListOf<Long>()
        var now = 0L
        val logger = LisaEyeDiagnostic.Logger(
            enabled = true,
            minIntervalMs = LisaEyeDiagnostic.DEFAULT_MIN_INTERVAL_MS,
            clockMs = { now },
            sink = { _, _ -> emitted += now }
        )
        // Simulate ~30 FPS for one second.
        while (now <= 1000L) {
            logger.maybeEmit(sample(left = 0.8f, right = 0.8f, eitherNull = false))
            now += 33L
        }
        assertTrue("expected ~4-5 samples, got ${emitted.size}", emitted.size in 4..6)
    }

    @Test
    fun rejectionReasonFrom_mapsProcessorSkips() {
        assertEquals(
            "both_eyes_uncertain",
            LisaEyeDiagnostic.rejectionReasonFrom(
                BlinkProcessResult(skippedBothUncertain = true, skippedUnstable = true)
            )
        )
        assertEquals(
            "jitter_skip",
            LisaEyeDiagnostic.rejectionReasonFrom(
                BlinkProcessResult(skippedForJitter = true, skippedUnstable = true)
            )
        )
        assertNull(LisaEyeDiagnostic.rejectionReasonFrom(BlinkProcessResult()))
    }

    private fun sample(
        left: Float?,
        right: Float?,
        eitherNull: Boolean,
        leftState: LisaEyeDiagnostic.InterpretedEyeState =
            LisaEyeDiagnostic.interpretEyeState(left, 0.25f, 0.75f),
        rightState: LisaEyeDiagnostic.InterpretedEyeState =
            LisaEyeDiagnostic.interpretEyeState(right, 0.25f, 0.75f),
        rejectionReason: String? = null,
        frameAccepted: Boolean = true,
        leftWinkCount: Int = 1,
        rightWinkCount: Int = 0,
        sequenceState: String = "L1R0|Listening|cd=false|em=false"
    ): LisaEyeDiagnostic.Sample = LisaEyeDiagnostic.Sample(
        timestampMs = 1_700_000_000_000L,
        faceDetected = true,
        faceCount = 1,
        boundingBoxWidthPx = 420,
        boundingBoxHeightPx = 520,
        faceWidthPercentOfImage = 35.0f,
        leftEyeOpenProbability = left,
        rightEyeOpenProbability = right,
        eitherProbabilityNull = eitherNull,
        headEulerAngleY = 2.5f,
        headEulerAngleZ = -1.0f,
        sensitivityLevel = 3,
        leftEyeClosedThreshold = 0.247f,
        rightEyeClosedThreshold = 0.247f,
        openEyeThreshold = 0.764f,
        interpretedLeftEyeState = leftState,
        interpretedRightEyeState = rightState,
        frameAccepted = frameAccepted,
        rejectionReason = rejectionReason,
        leftWinkCount = leftWinkCount,
        rightWinkCount = rightWinkCount,
        sequenceState = sequenceState
    )
}
