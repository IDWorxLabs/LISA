package com.idworx.lisa.features.eyediagnostic

import com.idworx.lisa.features.blinkdetectionreliability.BlinkDetectionProcessor
import com.idworx.lisa.features.blinkdetectionreliability.BlinkDetectionTuning
import com.idworx.lisa.features.blinkdetectionreliability.BlinkEyeProbabilities
import com.idworx.lisa.features.blinkdetectionreliability.BlinkProcessResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.io.path.createTempDirectory

class EyeDecisionTraceTest {

    private val tuning = BlinkDetectionTuning(
        closedEyeThreshold = 0.30f,
        openEyeThreshold = 0.70f,
        requiredWinkFrames = 2,
        openPrimingFrames = 1,
        cooldownMs = 520L
    )

    @Test
    fun thresholdBandReasons_mapCorrectly() {
        assertEquals(
            EyeDecisionReasonCode.below_closed_threshold,
            EyeDecisionTraceBuilder.thresholdBandReason(0.10f, 0.30f, 0.70f)
        )
        assertEquals(
            EyeDecisionReasonCode.above_open_threshold,
            EyeDecisionTraceBuilder.thresholdBandReason(0.90f, 0.30f, 0.70f)
        )
        assertEquals(
            EyeDecisionReasonCode.between_thresholds,
            EyeDecisionTraceBuilder.thresholdBandReason(0.50f, 0.30f, 0.70f)
        )
        assertEquals(
            EyeDecisionReasonCode.probability_missing,
            EyeDecisionTraceBuilder.thresholdBandReason(null, 0.30f, 0.70f)
        )
    }

    @Test
    fun bothEyesUncertain_frameReasonReported() {
        val trace = EyeDecisionTraceBuilder.build(
            rawLeft = 0.45f,
            rawRight = 0.50f,
            tuning = tuning,
            sensitivity = 3,
            result = BlinkProcessResult(skippedBothUncertain = true, skippedUnstable = true),
            rejectionReason = "both_eyes_uncertain",
            frameAccepted = false,
            sequenceState = "idle",
            previousLeftState = LisaEyeDiagnostic.InterpretedEyeState.OPEN,
            previousRightState = LisaEyeDiagnostic.InterpretedEyeState.OPEN,
            previousLeftCandidateActive = false,
            previousRightCandidateActive = false,
            msSinceLeftTransition = null,
            msSinceRightTransition = null
        )
        assertEquals(EyeDecisionReasonCode.both_eyes_uncertain, trace.frameDecisionReason)
        assertTrue(trace.bothEyesUncertain)
        assertTrue(trace.enteredUncertainWithoutClosedLeft)
        assertTrue(trace.enteredUncertainWithoutClosedRight)
        assertFalse(trace.closedThresholdCrossedLeft)
    }

    @Test
    fun insufficientConsecutiveFrames_reopenFailureDistinguishable() {
        val result = BlinkProcessResult(
            rejectedIncompleteShape = true,
            diagnosticReopenIncompleteLeft = true,
            diagnosticLeftSupportCount = 1,
            diagnosticRequiredWinkFrames = 2,
            diagnosticBothOpen = true
        )
        val trace = EyeDecisionTraceBuilder.build(
            rawLeft = 0.90f,
            rawRight = 0.90f,
            tuning = tuning,
            sensitivity = 3,
            result = result,
            rejectionReason = "incomplete_wink_shape",
            frameAccepted = true,
            sequenceState = "idle",
            previousLeftState = LisaEyeDiagnostic.InterpretedEyeState.CLOSED,
            previousRightState = LisaEyeDiagnostic.InterpretedEyeState.OPEN,
            previousLeftCandidateActive = true,
            previousRightCandidateActive = false,
            msSinceLeftTransition = 100L,
            msSinceRightTransition = null
        )
        assertTrue(trace.reopenFailedLeft)
        assertTrue(trace.leftWinkCandidateCancelled)
        assertEquals(EyeDecisionReasonCode.reopen_not_confirmed, trace.candidateCancellationReason)
        assertFalse(trace.leftWinkCompleted)
    }

    @Test
    fun closedCrossingWithoutReopen_isDistinctFromNoCrossing() {
        val crossed = EyeDecisionTraceBuilder.build(
            rawLeft = 0.10f,
            rawRight = 0.90f,
            tuning = tuning,
            sensitivity = 3,
            result = BlinkProcessResult(leftCandidate = true, leftStreak = 1),
            rejectionReason = null,
            frameAccepted = true,
            sequenceState = "idle",
            previousLeftState = LisaEyeDiagnostic.InterpretedEyeState.OPEN,
            previousRightState = LisaEyeDiagnostic.InterpretedEyeState.OPEN,
            previousLeftCandidateActive = false,
            previousRightCandidateActive = false,
            msSinceLeftTransition = null,
            msSinceRightTransition = null
        )
        assertTrue(crossed.closedThresholdCrossedLeft)
        assertTrue(crossed.leftWinkCandidateStarted)

        val noCross = EyeDecisionTraceBuilder.build(
            rawLeft = 0.50f,
            rawRight = 0.90f,
            tuning = tuning,
            sensitivity = 3,
            result = BlinkProcessResult(),
            rejectionReason = null,
            frameAccepted = true,
            sequenceState = "idle",
            previousLeftState = LisaEyeDiagnostic.InterpretedEyeState.OPEN,
            previousRightState = LisaEyeDiagnostic.InterpretedEyeState.OPEN,
            previousLeftCandidateActive = false,
            previousRightCandidateActive = false,
            msSinceLeftTransition = null,
            msSinceRightTransition = null
        )
        assertFalse(noCross.closedThresholdCrossedLeft)
        assertTrue(noCross.enteredUncertainWithoutClosedLeft)
    }

    @Test
    fun cooldownBlock_isReported() {
        val trace = EyeDecisionTraceBuilder.build(
            rawLeft = 0.90f,
            rawRight = 0.90f,
            tuning = tuning,
            sensitivity = 3,
            result = BlinkProcessResult(diagnosticLeftCooldownBlocked = true),
            rejectionReason = null,
            frameAccepted = true,
            sequenceState = "idle",
            previousLeftState = LisaEyeDiagnostic.InterpretedEyeState.OPEN,
            previousRightState = LisaEyeDiagnostic.InterpretedEyeState.OPEN,
            previousLeftCandidateActive = false,
            previousRightCandidateActive = false,
            msSinceLeftTransition = null,
            msSinceRightTransition = null
        )
        assertTrue(trace.cooldownActiveLeft)
        assertEquals(EyeDecisionReasonCode.cooldown_active, trace.frameDecisionReason)
    }

    @Test
    fun tracker_candidateStartCancelComplete_andUncertainDuration() {
        val tracker = EyeDecisionTraceTracker()
        var now = 1_000L
        val open = frame(0.90f, 0.90f, now)
        tracker.observe(open, now)
        now += 200
        val unc = frame(0.50f, 0.50f, now, bothUncertain = true, accepted = false)
        val begin = tracker.observe(unc, now)
        assertTrue(begin.any { it.type == "both_eyes_uncertain_begin" })
        now += 500
        val still = frame(0.48f, 0.52f, now, bothUncertain = true, accepted = false)
        tracker.observe(still, now)
        now += 300
        val end = frame(0.90f, 0.90f, now)
        val ended = tracker.observe(end, now)
        assertTrue(ended.any { it.type == "both_eyes_uncertain_end" })
        assertEquals(1, tracker.uncertainDurations().size)
        assertTrue(tracker.uncertainDurations().first() >= 700L)

        now += 100
        val startCand = frame(
            0.10f, 0.90f, now,
            leftCandidate = true,
            leftStarted = true
        )
        tracker.observe(startCand, now)
        assertEquals(1, tracker.leftCandidatesStarted)
        now += 100
        val cancel = frame(
            0.50f, 0.90f, now,
            leftCancelled = true,
            cancelReason = EyeDecisionReasonCode.incomplete_wink_shape
        )
        tracker.observe(cancel, now)
        assertEquals(1, tracker.leftCandidatesCancelled)
        assertEquals("incomplete_wink_shape", tracker.leftCancelReasons.keys.first())
    }

    @Test
    fun summaryAggregation_andCsvContainNewFields() {
        val samples = listOf(
            sampleWithDecision(
                left = 0.50f, right = 0.50f,
                leftState = LisaEyeDiagnostic.InterpretedEyeState.UNCERTAIN,
                rightState = LisaEyeDiagnostic.InterpretedEyeState.UNCERTAIN,
                reject = "both_eyes_uncertain",
                leftReason = "between_thresholds",
                rightReason = "between_thresholds"
            ),
            sampleWithDecision(
                left = 0.90f, right = 0.90f,
                leftState = LisaEyeDiagnostic.InterpretedEyeState.OPEN,
                rightState = LisaEyeDiagnostic.InterpretedEyeState.OPEN,
                leftReason = "above_open_threshold",
                rightReason = "above_open_threshold"
            ),
            sampleWithDecision(
                left = 0.10f, right = 0.90f,
                leftState = LisaEyeDiagnostic.InterpretedEyeState.CLOSED,
                rightState = LisaEyeDiagnostic.InterpretedEyeState.OPEN,
                leftReason = "below_closed_threshold",
                rightReason = "above_open_threshold",
                leftCand = true,
                leftStarted = true,
                closedLeft = true
            )
        )
        val analysis = EyeDecisionAnalysis.fromSamples(samples)
        assertTrue(analysis.bothUncertainPercent > 30f)
        assertTrue(analysis.leftCandidatesStarted >= 1)
        assertTrue(analysis.factualFindings.isNotEmpty())

        val summary = EyeTestSessionSummary.fromSamples(
            kind = EyeTestSessionKind.WITH_GLASSES,
            samples = samples
        )
        val lines = summary.phaseResultLines().joinToString("\n")
        assertTrue(lines.contains("Decision analysis"))
        assertTrue(lines.contains("Both UNCERTAIN"))

        val dir = createTempDirectory("eye_decision_csv").toFile()
        val csv = EyeTestSessionStore(dir).toCsv(samples)
        assertTrue(csv.contains("smoothedLeftProb"))
        assertTrue(csv.contains("leftDecisionReason"))
        assertTrue(csv.contains("frameDecisionReason"))
        assertTrue(csv.contains("leftWinkCandidateActive"))
        assertFalse(EyeTestSessionStore(dir).containsImageOrPiiMarkers(csv))
    }

    @Test
    fun comparisonReport_includesDecisionAnalysis() {
        val without = EyeTestSessionSummary.fromSamples(
            EyeTestSessionKind.WITHOUT_GLASSES,
            listOf(
                sampleWithDecision(
                    0.9f, 0.9f,
                    LisaEyeDiagnostic.InterpretedEyeState.OPEN,
                    LisaEyeDiagnostic.InterpretedEyeState.OPEN,
                    leftReason = "above_open_threshold",
                    rightReason = "above_open_threshold"
                )
            )
        )
        val with = EyeTestSessionSummary.fromSamples(
            EyeTestSessionKind.WITH_GLASSES,
            listOf(
                sampleWithDecision(
                    0.5f, 0.5f,
                    LisaEyeDiagnostic.InterpretedEyeState.UNCERTAIN,
                    LisaEyeDiagnostic.InterpretedEyeState.UNCERTAIN,
                    reject = "both_eyes_uncertain",
                    leftReason = "between_thresholds",
                    rightReason = "between_thresholds"
                )
            ),
            phaseEndedEarly = true
        )
        val report = EyeTestComparisonReport.compare(without, with)
        val text = report.buildFullResultsText()
        assertTrue(text.contains("Decision analysis") || text.contains("Decision-trace"))
        assertTrue(text.contains("Both-UNCERTAIN") || text.contains("uncertain"))
        assertNotNull(report.factualFinding)
    }

    @Test
    fun diagnostics_doNotMutateProductionProcessor() {
        val localTuning = BlinkDetectionTuning(
            closedEyeThreshold = 0.35f,
            openEyeThreshold = 0.65f,
            requiredWinkFrames = 1,
            openPrimingFrames = 1,
            cooldownMs = 0L
        )
        val processor = BlinkDetectionProcessor(localTuning)
        val open = BlinkEyeProbabilities(0.92f, 0.92f)
        val close = BlinkEyeProbabilities(0.08f, 0.92f)
        processor.processFrame(open, 0L, 0, 0)
        val mid = processor.processFrame(close, 16L, 0, 0)
        val reason = LisaEyeDiagnostic.rejectionReasonFrom(mid)
        EyeDecisionTraceBuilder.build(
            rawLeft = 0.08f,
            rawRight = 0.92f,
            tuning = localTuning,
            sensitivity = 3,
            result = mid,
            rejectionReason = reason,
            frameAccepted = true,
            sequenceState = "idle",
            previousLeftState = LisaEyeDiagnostic.InterpretedEyeState.OPEN,
            previousRightState = LisaEyeDiagnostic.InterpretedEyeState.OPEN,
            previousLeftCandidateActive = false,
            previousRightCandidateActive = false,
            msSinceLeftTransition = null,
            msSinceRightTransition = null
        )
        val accept = processor.processFrame(open, 32L, 0, 0)
        assertTrue(accept.acceptLeft)

        val logger = EyeDecisionTraceLogger(enabled = false)
        assertFalse(logger.isEnabled())
        logger.emitEventImmediate(EyeDecisionEvent(0L, "x", "y"))
        logger.maybeEmitFrame(
            EyeDecisionTraceBuilder.build(
                rawLeft = 0.5f,
                rawRight = 0.5f,
                tuning = tuning,
                sensitivity = 3,
                result = null,
                rejectionReason = "both_eyes_uncertain",
                frameAccepted = false,
                sequenceState = "idle",
                previousLeftState = LisaEyeDiagnostic.InterpretedEyeState.NULL,
                previousRightState = LisaEyeDiagnostic.InterpretedEyeState.NULL,
                previousLeftCandidateActive = false,
                previousRightCandidateActive = false,
                msSinceLeftTransition = null,
                msSinceRightTransition = null
            )
        )
    }

    @Test
    fun singleEyeSubtest_recordsThresholdCrossingsWithoutGestures() {
        var now = 0L
        val sub = SingleEyeThresholdSubtest(clockMs = { now })
        sub.start(
            eye = SingleEyeThresholdSubtest.EyeTarget.Left,
            componentId = EyeTestComponentId.WithoutGlassesLeftEye,
            closedThreshold = 0.30f,
            openThreshold = 0.70f
        )
        assertTrue(sub.ui.active)
        now += SingleEyeThresholdSubtest.BOTH_OPEN_MS + 1
        sub.onTick(now)
        assertEquals(SingleEyeThresholdSubtest.Phase.CloseHold, sub.ui.phase)
        sub.onSample(0.05f, 0.90f, frameAccepted = true, rejectionReason = null, decisionReason = null, nowMs = now)
        now += SingleEyeThresholdSubtest.CLOSE_HOLD_MS + 1
        sub.onTick(now)
        assertEquals(SingleEyeThresholdSubtest.Phase.ReopenHold, sub.ui.phase)
        sub.onSample(0.95f, 0.95f, frameAccepted = true, rejectionReason = null, decisionReason = null, nowMs = now)
        now += SingleEyeThresholdSubtest.REOPEN_HOLD_MS + 1
        sub.onTick(now)
        // After first cycle may be BetweenReps or next BothOpen
        assertTrue(
            sub.ui.phase == SingleEyeThresholdSubtest.Phase.BetweenReps ||
                sub.ui.phase == SingleEyeThresholdSubtest.Phase.BothOpen ||
                sub.resultOrNull() != null ||
                sub.ui.phase == SingleEyeThresholdSubtest.Phase.Complete
        )
    }

    private fun frame(
        left: Float,
        right: Float,
        now: Long,
        bothUncertain: Boolean = false,
        accepted: Boolean = true,
        leftCandidate: Boolean = false,
        leftStarted: Boolean = false,
        leftCancelled: Boolean = false,
        cancelReason: EyeDecisionReasonCode? = null
    ): EyeDecisionFrameTrace {
        val leftState = LisaEyeDiagnostic.interpretEyeState(left, 0.30f, 0.70f)
        val rightState = LisaEyeDiagnostic.interpretEyeState(right, 0.30f, 0.70f)
        return EyeDecisionFrameTrace(
            rawLeftProb = left,
            rawRightProb = right,
            smoothedLeftProb = left,
            smoothedRightProb = right,
            prevLeftProb = null,
            prevRightProb = null,
            leftPreviousState = LisaEyeDiagnostic.InterpretedEyeState.OPEN,
            rightPreviousState = LisaEyeDiagnostic.InterpretedEyeState.OPEN,
            leftCandidateState = leftState,
            rightCandidateState = rightState,
            leftFinalState = leftState,
            rightFinalState = rightState,
            leftDecisionReason = EyeDecisionTraceBuilder.thresholdBandReason(left, 0.30f, 0.70f),
            rightDecisionReason = EyeDecisionTraceBuilder.thresholdBandReason(right, 0.30f, 0.70f),
            frameDecisionReason = if (bothUncertain) {
                EyeDecisionReasonCode.both_eyes_uncertain
            } else {
                EyeDecisionReasonCode.frame_accepted
            },
            frameAccepted = accepted,
            rejectionReason = if (bothUncertain) "both_eyes_uncertain" else null,
            sensitivity = 3,
            leftClosedThreshold = 0.30f,
            rightClosedThreshold = 0.30f,
            openThreshold = 0.70f,
            uncertainBandLow = 0.30f,
            uncertainBandHigh = 0.70f,
            leftConsecutiveSupport = 0,
            rightConsecutiveSupport = 0,
            requiredSupportCount = 2,
            leftTransitionAccepted = true,
            rightTransitionAccepted = true,
            leftWinkCandidateActive = leftCandidate,
            rightWinkCandidateActive = false,
            leftWinkCandidateStarted = leftStarted,
            rightWinkCandidateStarted = false,
            leftWinkCandidateCancelled = leftCancelled,
            rightWinkCandidateCancelled = false,
            candidateCancellationReason = cancelReason,
            leftWinkCompleted = false,
            rightWinkCompleted = false,
            cooldownActiveLeft = false,
            cooldownActiveRight = false,
            eligibleForWinkDetection = accepted,
            countersWouldIncrement = false,
            sequenceState = "idle",
            msSinceLeftTransition = null,
            msSinceRightTransition = null,
            bothEyesUncertain = bothUncertain,
            closedThresholdCrossedLeft = left < 0.30f,
            closedThresholdCrossedRight = right < 0.30f,
            enteredUncertainWithoutClosedLeft = leftState == LisaEyeDiagnostic.InterpretedEyeState.UNCERTAIN,
            enteredUncertainWithoutClosedRight = rightState == LisaEyeDiagnostic.InterpretedEyeState.UNCERTAIN,
            reopenFailedLeft = false,
            reopenFailedRight = false
        )
    }

    private fun sampleWithDecision(
        left: Float?,
        right: Float?,
        leftState: LisaEyeDiagnostic.InterpretedEyeState,
        rightState: LisaEyeDiagnostic.InterpretedEyeState,
        reject: String? = null,
        leftReason: String? = null,
        rightReason: String? = null,
        leftCand: Boolean = false,
        leftStarted: Boolean = false,
        closedLeft: Boolean = false
    ): LisaEyeDiagnostic.Sample = LisaEyeDiagnostic.Sample(
        timestampMs = 1_700_000_000_000L,
        faceDetected = true,
        faceCount = 1,
        boundingBoxWidthPx = 400,
        boundingBoxHeightPx = 500,
        faceWidthPercentOfImage = 35f,
        leftEyeOpenProbability = left,
        rightEyeOpenProbability = right,
        eitherProbabilityNull = left == null || right == null,
        headEulerAngleY = 1f,
        headEulerAngleZ = -1f,
        sensitivityLevel = 3,
        leftEyeClosedThreshold = 0.30f,
        rightEyeClosedThreshold = 0.30f,
        openEyeThreshold = 0.70f,
        interpretedLeftEyeState = leftState,
        interpretedRightEyeState = rightState,
        frameAccepted = reject == null,
        rejectionReason = reject,
        leftWinkCount = 0,
        rightWinkCount = 0,
        sequenceState = "idle",
        leftDecisionReason = leftReason,
        rightDecisionReason = rightReason,
        frameDecisionReason = reject ?: "frame_accepted",
        leftWinkCandidateActive = leftCand,
        leftWinkCandidateStarted = leftStarted,
        closedThresholdCrossedLeft = closedLeft
    )
}
