package com.idworx.lisa.features.eyediagnostic

import android.util.Log
import com.idworx.lisa.features.blinkdetectionreliability.BlinkDetectionTuning
import com.idworx.lisa.features.blinkdetectionreliability.BlinkProcessResult

/**
 * Machine-readable reason codes for per-eye and per-frame diagnostic decisions.
 * Observational only — never drives production wink acceptance.
 */
enum class EyeDecisionReasonCode {
    below_closed_threshold,
    above_open_threshold,
    between_thresholds,
    hysteresis_hold_open,
    hysteresis_hold_closed,
    insufficient_consecutive_frames,
    smoothing_window_unstable,
    rapid_state_oscillation,
    jitter_rejected,
    cooldown_active,
    reopen_not_confirmed,
    close_not_confirmed,
    opposite_eye_not_open,
    both_eyes_uncertain,
    both_eyes_closed,
    probability_missing,
    head_pose_rejected,
    face_size_rejected,
    frame_rate_limited,
    unprimed_close,
    incomplete_wink_shape,
    frame_accepted,
    wink_candidate_started,
    wink_candidate_cancelled,
    wink_completed,
    other
}

/**
 * Structured per-frame decision trace for glasses reliability analysis.
 * No smoothing exists in production; smoothed* fields mirror raw for CSV compatibility.
 */
data class EyeDecisionFrameTrace(
    val rawLeftProb: Float?,
    val rawRightProb: Float?,
    val smoothedLeftProb: Float?,
    val smoothedRightProb: Float?,
    val prevLeftProb: Float?,
    val prevRightProb: Float?,
    val leftPreviousState: LisaEyeDiagnostic.InterpretedEyeState,
    val rightPreviousState: LisaEyeDiagnostic.InterpretedEyeState,
    val leftCandidateState: LisaEyeDiagnostic.InterpretedEyeState,
    val rightCandidateState: LisaEyeDiagnostic.InterpretedEyeState,
    val leftFinalState: LisaEyeDiagnostic.InterpretedEyeState,
    val rightFinalState: LisaEyeDiagnostic.InterpretedEyeState,
    val leftDecisionReason: EyeDecisionReasonCode,
    val rightDecisionReason: EyeDecisionReasonCode,
    val frameDecisionReason: EyeDecisionReasonCode,
    val frameAccepted: Boolean,
    val rejectionReason: String?,
    val sensitivity: Int,
    val leftClosedThreshold: Float,
    val rightClosedThreshold: Float,
    val openThreshold: Float,
    val uncertainBandLow: Float,
    val uncertainBandHigh: Float,
    val leftConsecutiveSupport: Int,
    val rightConsecutiveSupport: Int,
    val requiredSupportCount: Int,
    val leftTransitionAccepted: Boolean,
    val rightTransitionAccepted: Boolean,
    val leftWinkCandidateActive: Boolean,
    val rightWinkCandidateActive: Boolean,
    val leftWinkCandidateStarted: Boolean,
    val rightWinkCandidateStarted: Boolean,
    val leftWinkCandidateCancelled: Boolean,
    val rightWinkCandidateCancelled: Boolean,
    val candidateCancellationReason: EyeDecisionReasonCode?,
    val leftWinkCompleted: Boolean,
    val rightWinkCompleted: Boolean,
    val cooldownActiveLeft: Boolean,
    val cooldownActiveRight: Boolean,
    val eligibleForWinkDetection: Boolean,
    val countersWouldIncrement: Boolean,
    val sequenceState: String,
    val msSinceLeftTransition: Long?,
    val msSinceRightTransition: Long?,
    val bothEyesUncertain: Boolean,
    val closedThresholdCrossedLeft: Boolean,
    val closedThresholdCrossedRight: Boolean,
    val enteredUncertainWithoutClosedLeft: Boolean,
    val enteredUncertainWithoutClosedRight: Boolean,
    val reopenFailedLeft: Boolean,
    val reopenFailedRight: Boolean,
    val productionSmoothingPresent: Boolean = false
)

data class EyeDecisionEvent(
    val timestampMs: Long,
    val type: String,
    val detail: String
)

/**
 * Builds a frame decision trace from raw probabilities + production BlinkProcessResult.
 * Pure / observational — does not call the blink processor or mutate wink state.
 */
object EyeDecisionTraceBuilder {
    fun thresholdBandReason(
        probability: Float?,
        closedThreshold: Float,
        openThreshold: Float
    ): EyeDecisionReasonCode {
        if (probability == null) return EyeDecisionReasonCode.probability_missing
        return when {
            probability < closedThreshold -> EyeDecisionReasonCode.below_closed_threshold
            probability > openThreshold -> EyeDecisionReasonCode.above_open_threshold
            else -> EyeDecisionReasonCode.between_thresholds
        }
    }

    fun build(
        rawLeft: Float?,
        rawRight: Float?,
        tuning: BlinkDetectionTuning,
        sensitivity: Int,
        result: BlinkProcessResult?,
        rejectionReason: String?,
        frameAccepted: Boolean,
        sequenceState: String,
        previousLeftState: LisaEyeDiagnostic.InterpretedEyeState,
        previousRightState: LisaEyeDiagnostic.InterpretedEyeState,
        previousLeftCandidateActive: Boolean,
        previousRightCandidateActive: Boolean,
        msSinceLeftTransition: Long?,
        msSinceRightTransition: Long?
    ): EyeDecisionFrameTrace {
        val leftClosed = tuning.effectiveLeftClosedThreshold
        val rightClosed = tuning.effectiveRightClosedThreshold
        val openThr = tuning.openEyeThreshold
        val bandLow = minOf(leftClosed, rightClosed)
        val leftFinal = LisaEyeDiagnostic.interpretEyeState(rawLeft, leftClosed, openThr)
        val rightFinal = LisaEyeDiagnostic.interpretEyeState(rawRight, rightClosed, openThr)
        // Production has no temporal probability smoothing — smoothed mirrors raw.
        val leftReason = thresholdBandReason(rawLeft, leftClosed, openThr)
        val rightReason = thresholdBandReason(rawRight, rightClosed, openThr)

        val leftCandidateActive = result?.leftCandidate == true
        val rightCandidateActive = result?.rightCandidate == true
        val leftStarted = leftCandidateActive && !previousLeftCandidateActive
        val rightStarted = rightCandidateActive && !previousRightCandidateActive
        val leftCancelled = !leftCandidateActive && previousLeftCandidateActive &&
            result?.acceptLeft != true
        val rightCancelled = !rightCandidateActive && previousRightCandidateActive &&
            result?.acceptRight != true

        val cancelReason = when {
            result?.diagnosticReopenIncompleteLeft == true ||
                result?.diagnosticReopenIncompleteRight == true ->
                EyeDecisionReasonCode.reopen_not_confirmed
            result?.rejectedIncompleteShape == true -> EyeDecisionReasonCode.incomplete_wink_shape
            result?.rejectedUnprimed == true -> EyeDecisionReasonCode.unprimed_close
            result?.skippedBothUncertain == true -> EyeDecisionReasonCode.both_eyes_uncertain
            result?.skippedForJitter == true -> EyeDecisionReasonCode.jitter_rejected
            leftCancelled || rightCancelled -> EyeDecisionReasonCode.wink_candidate_cancelled
            else -> null
        }

        val frameReason = when {
            rejectionReason == "both_eyes_uncertain" || result?.skippedBothUncertain == true ->
                EyeDecisionReasonCode.both_eyes_uncertain
            rejectionReason == "jitter_skip" || result?.skippedForJitter == true ->
                EyeDecisionReasonCode.jitter_rejected
            rejectionReason == "null_eye_probabilities" -> EyeDecisionReasonCode.probability_missing
            rejectionReason == "no_face" -> EyeDecisionReasonCode.face_size_rejected
            rejectionReason == "unprimed_close" -> EyeDecisionReasonCode.unprimed_close
            rejectionReason == "incomplete_wink_shape" -> EyeDecisionReasonCode.incomplete_wink_shape
            result?.diagnosticLeftCooldownBlocked == true ||
                result?.diagnosticRightCooldownBlocked == true ->
                EyeDecisionReasonCode.cooldown_active
            frameAccepted -> EyeDecisionReasonCode.frame_accepted
            else -> EyeDecisionReasonCode.other
        }

        val bothUncertain =
            leftFinal == LisaEyeDiagnostic.InterpretedEyeState.UNCERTAIN &&
                rightFinal == LisaEyeDiagnostic.InterpretedEyeState.UNCERTAIN

        val leftSupport = result?.diagnosticLeftSupportCount ?: result?.leftStreak ?: 0
        val rightSupport = result?.diagnosticRightSupportCount ?: result?.rightStreak ?: 0
        val required = result?.diagnosticRequiredWinkFrames
            ?: tuning.requiredWinkFrames

        val leftCrossedClosed = rawLeft != null && rawLeft < leftClosed
        val rightCrossedClosed = rawRight != null && rawRight < rightClosed
        val leftEnteredUncertainNoClosed =
            leftFinal == LisaEyeDiagnostic.InterpretedEyeState.UNCERTAIN && !leftCrossedClosed
        val rightEnteredUncertainNoClosed =
            rightFinal == LisaEyeDiagnostic.InterpretedEyeState.UNCERTAIN && !rightCrossedClosed

        return EyeDecisionFrameTrace(
            rawLeftProb = rawLeft,
            rawRightProb = rawRight,
            smoothedLeftProb = rawLeft,
            smoothedRightProb = rawRight,
            prevLeftProb = result?.diagnosticPrevLeftProb,
            prevRightProb = result?.diagnosticPrevRightProb,
            leftPreviousState = previousLeftState,
            rightPreviousState = previousRightState,
            leftCandidateState = leftFinal,
            rightCandidateState = rightFinal,
            leftFinalState = leftFinal,
            rightFinalState = rightFinal,
            leftDecisionReason = leftReason,
            rightDecisionReason = rightReason,
            frameDecisionReason = frameReason,
            frameAccepted = frameAccepted,
            rejectionReason = rejectionReason,
            sensitivity = sensitivity,
            leftClosedThreshold = leftClosed,
            rightClosedThreshold = rightClosed,
            openThreshold = openThr,
            uncertainBandLow = bandLow,
            uncertainBandHigh = openThr,
            leftConsecutiveSupport = leftSupport,
            rightConsecutiveSupport = rightSupport,
            requiredSupportCount = required,
            leftTransitionAccepted = leftFinal != previousLeftState,
            rightTransitionAccepted = rightFinal != previousRightState,
            leftWinkCandidateActive = leftCandidateActive,
            rightWinkCandidateActive = rightCandidateActive,
            leftWinkCandidateStarted = leftStarted,
            rightWinkCandidateStarted = rightStarted,
            leftWinkCandidateCancelled = leftCancelled,
            rightWinkCandidateCancelled = rightCancelled,
            candidateCancellationReason = cancelReason,
            leftWinkCompleted = result?.acceptLeft == true,
            rightWinkCompleted = result?.acceptRight == true,
            cooldownActiveLeft = result?.diagnosticLeftCooldownBlocked == true,
            cooldownActiveRight = result?.diagnosticRightCooldownBlocked == true,
            eligibleForWinkDetection = frameAccepted && rawLeft != null && rawRight != null,
            countersWouldIncrement = result?.acceptLeft == true || result?.acceptRight == true,
            sequenceState = sequenceState,
            msSinceLeftTransition = msSinceLeftTransition,
            msSinceRightTransition = msSinceRightTransition,
            bothEyesUncertain = bothUncertain,
            closedThresholdCrossedLeft = leftCrossedClosed,
            closedThresholdCrossedRight = rightCrossedClosed,
            enteredUncertainWithoutClosedLeft = leftEnteredUncertainNoClosed,
            enteredUncertainWithoutClosedRight = rightEnteredUncertainNoClosed,
            reopenFailedLeft = result?.diagnosticReopenIncompleteLeft == true,
            reopenFailedRight = result?.diagnosticReopenIncompleteRight == true,
            productionSmoothingPresent = false
        )
    }

    fun formatFrame(trace: EyeDecisionFrameTrace): String = buildString {
        append("Lraw=").append(trace.rawLeftProb?.let { "%.3f".format(it) } ?: "null")
        append(" Rraw=").append(trace.rawRightProb?.let { "%.3f".format(it) } ?: "null")
        append(" Lsm=").append(trace.smoothedLeftProb?.let { "%.3f".format(it) } ?: "null")
        append(" Rsm=").append(trace.smoothedRightProb?.let { "%.3f".format(it) } ?: "null")
        append(" Lprev=").append(trace.leftPreviousState.name)
        append(" Rprev=").append(trace.rightPreviousState.name)
        append(" Lfinal=").append(trace.leftFinalState.name)
        append(" Rfinal=").append(trace.rightFinalState.name)
        append(" Lreason=").append(trace.leftDecisionReason.name)
        append(" Rreason=").append(trace.rightDecisionReason.name)
        append(" frameReason=").append(trace.frameDecisionReason.name)
        append(" accepted=").append(trace.frameAccepted)
        append(" reject=").append(trace.rejectionReason ?: "none")
        append(" Lsup=").append(trace.leftConsecutiveSupport)
        append("/").append(trace.requiredSupportCount)
        append(" Rsup=").append(trace.rightConsecutiveSupport)
        append("/").append(trace.requiredSupportCount)
        append(" Lcand=").append(trace.leftWinkCandidateActive)
        append(" Rcand=").append(trace.rightWinkCandidateActive)
        append(" cancel=").append(trace.candidateCancellationReason?.name ?: "none")
        append(" cdL=").append(trace.cooldownActiveLeft)
        append(" cdR=").append(trace.cooldownActiveRight)
        append(" bothUnc=").append(trace.bothEyesUncertain)
        append(" thrL=").append("%.3f".format(trace.leftClosedThreshold))
        append(" thrR=").append("%.3f".format(trace.rightClosedThreshold))
        append(" open=").append("%.3f".format(trace.openThreshold))
        append(" smooth=false")
    }
}

/**
 * Tracks cross-frame candidate / uncertain / transition events without mutating production.
 */
class EyeDecisionTraceTracker {
    var previousLeftState: LisaEyeDiagnostic.InterpretedEyeState =
        LisaEyeDiagnostic.InterpretedEyeState.NULL
        private set
    var previousRightState: LisaEyeDiagnostic.InterpretedEyeState =
        LisaEyeDiagnostic.InterpretedEyeState.NULL
        private set
    var leftCandidateActive: Boolean = false
        private set
    var rightCandidateActive: Boolean = false
        private set
    var lastFrameAccepted: Boolean? = null
        private set
    var bothUncertainActive: Boolean = false
        private set
    var lastLeftTransitionMs: Long = 0L
        private set
    var lastRightTransitionMs: Long = 0L
        private set

    var leftCandidatesStarted: Int = 0
        private set
    var rightCandidatesStarted: Int = 0
        private set
    var leftCandidatesCancelled: Int = 0
        private set
    var rightCandidatesCancelled: Int = 0
        private set
    var leftWinksCompleted: Int = 0
        private set
    var rightWinksCompleted: Int = 0
        private set
    var leftReopenFailures: Int = 0
        private set
    var rightReopenFailures: Int = 0
        private set
    var leftReopenSuccesses: Int = 0
        private set
    var rightReopenSuccesses: Int = 0
        private set
    var leftCancelReasons: MutableMap<String, Int> = mutableMapOf()
        private set
    var rightCancelReasons: MutableMap<String, Int> = mutableMapOf()
        private set
    var leftClosedCrossings: Int = 0
        private set
    var rightClosedCrossings: Int = 0
        private set
    var leftUncertainNoClosed: Int = 0
        private set
    var rightUncertainNoClosed: Int = 0
        private set
    var cooldownBlocks: Int = 0
        private set

    private var uncertainPeriodStartMs: Long? = null
    private val uncertainDurationsMs = mutableListOf<Long>()
    private var wasLeftClosed = false
    private var wasRightClosed = false

    fun reset() {
        previousLeftState = LisaEyeDiagnostic.InterpretedEyeState.NULL
        previousRightState = LisaEyeDiagnostic.InterpretedEyeState.NULL
        leftCandidateActive = false
        rightCandidateActive = false
        lastFrameAccepted = null
        bothUncertainActive = false
        lastLeftTransitionMs = 0L
        lastRightTransitionMs = 0L
        leftCandidatesStarted = 0
        rightCandidatesStarted = 0
        leftCandidatesCancelled = 0
        rightCandidatesCancelled = 0
        leftWinksCompleted = 0
        rightWinksCompleted = 0
        leftReopenFailures = 0
        rightReopenFailures = 0
        leftReopenSuccesses = 0
        rightReopenSuccesses = 0
        leftCancelReasons.clear()
        rightCancelReasons.clear()
        leftClosedCrossings = 0
        rightClosedCrossings = 0
        leftUncertainNoClosed = 0
        rightUncertainNoClosed = 0
        cooldownBlocks = 0
        uncertainPeriodStartMs = null
        uncertainDurationsMs.clear()
        wasLeftClosed = false
        wasRightClosed = false
    }

    fun msSinceLeftTransition(nowMs: Long): Long? =
        if (lastLeftTransitionMs <= 0L) null else (nowMs - lastLeftTransitionMs).coerceAtLeast(0L)

    fun msSinceRightTransition(nowMs: Long): Long? =
        if (lastRightTransitionMs <= 0L) null else (nowMs - lastRightTransitionMs).coerceAtLeast(0L)

    fun observe(trace: EyeDecisionFrameTrace, nowMs: Long): List<EyeDecisionEvent> {
        val events = mutableListOf<EyeDecisionEvent>()

        if (trace.leftFinalState != previousLeftState) {
            events += EyeDecisionEvent(nowMs, "eye_state_change", "left ${previousLeftState}→${trace.leftFinalState}")
            lastLeftTransitionMs = nowMs
        }
        if (trace.rightFinalState != previousRightState) {
            events += EyeDecisionEvent(nowMs, "eye_state_change", "right ${previousRightState}→${trace.rightFinalState}")
            lastRightTransitionMs = nowMs
        }

        if (trace.leftWinkCandidateStarted) {
            leftCandidatesStarted++
            events += EyeDecisionEvent(nowMs, "wink_candidate_start", "left")
        }
        if (trace.rightWinkCandidateStarted) {
            rightCandidatesStarted++
            events += EyeDecisionEvent(nowMs, "wink_candidate_start", "right")
        }
        if (trace.leftWinkCandidateCancelled) {
            leftCandidatesCancelled++
            val reason = trace.candidateCancellationReason?.name ?: "other"
            leftCancelReasons[reason] = (leftCancelReasons[reason] ?: 0) + 1
            events += EyeDecisionEvent(nowMs, "wink_candidate_cancel", "left:$reason")
        }
        if (trace.rightWinkCandidateCancelled) {
            rightCandidatesCancelled++
            val reason = trace.candidateCancellationReason?.name ?: "other"
            rightCancelReasons[reason] = (rightCancelReasons[reason] ?: 0) + 1
            events += EyeDecisionEvent(nowMs, "wink_candidate_cancel", "right:$reason")
        }
        if (trace.leftWinkCompleted) {
            leftWinksCompleted++
            leftReopenSuccesses++
            events += EyeDecisionEvent(nowMs, "wink_complete", "left")
            events += EyeDecisionEvent(nowMs, "wink_counter_increment", "left")
        }
        if (trace.rightWinkCompleted) {
            rightWinksCompleted++
            rightReopenSuccesses++
            events += EyeDecisionEvent(nowMs, "wink_complete", "right")
            events += EyeDecisionEvent(nowMs, "wink_counter_increment", "right")
        }
        if (trace.reopenFailedLeft) {
            leftReopenFailures++
            events += EyeDecisionEvent(nowMs, "reopen_failed", "left")
        }
        if (trace.reopenFailedRight) {
            rightReopenFailures++
            events += EyeDecisionEvent(nowMs, "reopen_failed", "right")
        }
        if (trace.cooldownActiveLeft || trace.cooldownActiveRight) {
            cooldownBlocks++
            events += EyeDecisionEvent(nowMs, "cooldown_block", "L=${trace.cooldownActiveLeft} R=${trace.cooldownActiveRight}")
        }

        val accepted = trace.frameAccepted
        if (lastFrameAccepted != null && lastFrameAccepted != accepted) {
            events += EyeDecisionEvent(
                nowMs,
                "frame_accept_change",
                if (accepted) "rejected→accepted" else "accepted→rejected"
            )
        }
        lastFrameAccepted = accepted

        if (trace.bothEyesUncertain && !bothUncertainActive) {
            bothUncertainActive = true
            uncertainPeriodStartMs = nowMs
            events += EyeDecisionEvent(nowMs, "both_eyes_uncertain_begin", "start")
        } else if (!trace.bothEyesUncertain && bothUncertainActive) {
            bothUncertainActive = false
            uncertainPeriodStartMs?.let { start ->
                uncertainDurationsMs += (nowMs - start).coerceAtLeast(0L)
            }
            uncertainPeriodStartMs = null
            events += EyeDecisionEvent(nowMs, "both_eyes_uncertain_end", "end")
        }

        if (trace.closedThresholdCrossedLeft && !wasLeftClosed) leftClosedCrossings++
        if (trace.closedThresholdCrossedRight && !wasRightClosed) rightClosedCrossings++
        wasLeftClosed = trace.closedThresholdCrossedLeft
        wasRightClosed = trace.closedThresholdCrossedRight
        if (trace.enteredUncertainWithoutClosedLeft) leftUncertainNoClosed++
        if (trace.enteredUncertainWithoutClosedRight) rightUncertainNoClosed++

        previousLeftState = trace.leftFinalState
        previousRightState = trace.rightFinalState
        leftCandidateActive = trace.leftWinkCandidateActive
        rightCandidateActive = trace.rightWinkCandidateActive
        return events
    }

    fun finishOpenUncertainPeriod(nowMs: Long) {
        if (bothUncertainActive) {
            uncertainPeriodStartMs?.let { start ->
                uncertainDurationsMs += (nowMs - start).coerceAtLeast(0L)
            }
            bothUncertainActive = false
            uncertainPeriodStartMs = null
        }
    }

    fun uncertainDurations(): List<Long> = uncertainDurationsMs.toList()
}

class EyeDecisionTraceLogger(
    private val enabled: Boolean,
    private val minIntervalMs: Long = LisaEyeDiagnostic.DEFAULT_MIN_INTERVAL_MS,
    private val clockMs: () -> Long = { System.currentTimeMillis() },
    private val sink: (tag: String, message: String) -> Unit = { tag, message ->
        Log.i(tag, message)
    }
) {
    companion object {
        const val TAG = "LISA_EYE_DECISION_TRACE"
    }

    @Volatile
    private var lastFrameEmitMs: Long = 0L

    fun isEnabled(): Boolean = enabled

    fun emitEventImmediate(event: EyeDecisionEvent) {
        if (!enabled) return
        sink(TAG, "EVENT type=${event.type} detail=${event.detail} ts=${event.timestampMs}")
    }

    fun maybeEmitFrame(trace: EyeDecisionFrameTrace) {
        if (!enabled) return
        val now = clockMs()
        val previous = lastFrameEmitMs
        if (previous != 0L && now - previous < minIntervalMs) return
        lastFrameEmitMs = now
        sink(TAG, "FRAME ${EyeDecisionTraceBuilder.formatFrame(trace)}")
    }
}

/**
 * Aggregated decision metrics for one Eye Test phase.
 */
data class EyeDecisionAnalysis(
    val leftOpenPercent: Float = 0f,
    val leftClosedPercent: Float = 0f,
    val leftUncertainPercent: Float = 0f,
    val rightOpenPercent: Float = 0f,
    val rightClosedPercent: Float = 0f,
    val rightUncertainPercent: Float = 0f,
    val bothUncertainPercent: Float = 0f,
    val bothUncertainFrameCount: Int = 0,
    val averageUncertainDurationMs: Float? = null,
    val longestUncertainDurationMs: Long = 0L,
    val topLeftDecisionReasons: List<Pair<String, Int>> = emptyList(),
    val topRightDecisionReasons: List<Pair<String, Int>> = emptyList(),
    val topFrameRejectionReasons: List<Pair<String, Int>> = emptyList(),
    val leftCandidatesStarted: Int = 0,
    val leftCandidatesCancelled: Int = 0,
    val topLeftCancelReason: String? = null,
    val rightCandidatesStarted: Int = 0,
    val rightCandidatesCancelled: Int = 0,
    val topRightCancelReason: String? = null,
    val leftReopenSuccesses: Int = 0,
    val leftReopenFailures: Int = 0,
    val rightReopenSuccesses: Int = 0,
    val rightReopenFailures: Int = 0,
    val avgRawLeftSteadyOpen: Float? = null,
    val avgRawRightSteadyOpen: Float? = null,
    val avgRawLeftDuringLeftWinkAttempt: Float? = null,
    val avgRawRightDuringLeftWinkAttempt: Float? = null,
    val avgRawLeftDuringRightWinkAttempt: Float? = null,
    val avgRawRightDuringRightWinkAttempt: Float? = null,
    val minRawLeftDuringLeftWinkAttempt: Float? = null,
    val maxRawLeftDuringLeftWinkAttempt: Float? = null,
    val minRawRightDuringRightWinkAttempt: Float? = null,
    val maxRawRightDuringRightWinkAttempt: Float? = null,
    val leftClosedThresholdCrossings: Int = 0,
    val rightClosedThresholdCrossings: Int = 0,
    val leftEnteredUncertainWithoutClosed: Int = 0,
    val rightEnteredUncertainWithoutClosed: Int = 0,
    val cooldownBlocks: Int = 0,
    val factualFindings: List<String> = emptyList()
) {
    companion object {
        fun fromSamples(
            samples: List<LisaEyeDiagnostic.Sample>,
            tracker: EyeDecisionTraceTracker? = null
        ): EyeDecisionAnalysis {
            val n = samples.size
            if (n == 0) return EyeDecisionAnalysis()

            fun pct(count: Int): Float = count * 100f / n
            val leftOpen = samples.count {
                it.interpretedLeftEyeState == LisaEyeDiagnostic.InterpretedEyeState.OPEN
            }
            val leftClosed = samples.count {
                it.interpretedLeftEyeState == LisaEyeDiagnostic.InterpretedEyeState.CLOSED
            }
            val leftUnc = samples.count {
                it.interpretedLeftEyeState == LisaEyeDiagnostic.InterpretedEyeState.UNCERTAIN
            }
            val rightOpen = samples.count {
                it.interpretedRightEyeState == LisaEyeDiagnostic.InterpretedEyeState.OPEN
            }
            val rightClosed = samples.count {
                it.interpretedRightEyeState == LisaEyeDiagnostic.InterpretedEyeState.CLOSED
            }
            val rightUnc = samples.count {
                it.interpretedRightEyeState == LisaEyeDiagnostic.InterpretedEyeState.UNCERTAIN
            }
            val bothUnc = samples.count {
                it.interpretedLeftEyeState == LisaEyeDiagnostic.InterpretedEyeState.UNCERTAIN &&
                    it.interpretedRightEyeState == LisaEyeDiagnostic.InterpretedEyeState.UNCERTAIN
            }

            val leftReasons = samples.mapNotNull { it.leftDecisionReason }
                .groupingBy { it }.eachCount()
                .entries.sortedByDescending { it.value }.take(5)
                .map { it.key to it.value }
            val rightReasons = samples.mapNotNull { it.rightDecisionReason }
                .groupingBy { it }.eachCount()
                .entries.sortedByDescending { it.value }.take(5)
                .map { it.key to it.value }
            val rejectReasons = samples.mapNotNull { it.rejectionReason }
                .groupingBy { it }.eachCount()
                .entries.sortedByDescending { it.value }.take(5)
                .map { it.key to it.value }

            val steadyOpen = samples.filter {
                it.interpretedLeftEyeState == LisaEyeDiagnostic.InterpretedEyeState.OPEN &&
                    it.interpretedRightEyeState == LisaEyeDiagnostic.InterpretedEyeState.OPEN
            }
            val leftAttempt = samples.filter { it.leftWinkCandidateActive }
            val rightAttempt = samples.filter { it.rightWinkCandidateActive }

            val uncDurations = tracker?.uncertainDurations().orEmpty()
            val avgUnc = uncDurations.takeIf { it.isNotEmpty() }?.average()?.toFloat()
            val maxUnc = uncDurations.maxOrNull() ?: 0L

            val leftStarts = tracker?.leftCandidatesStarted
                ?: samples.count { it.leftWinkCandidateStarted }
            val rightStarts = tracker?.rightCandidatesStarted
                ?: samples.count { it.rightWinkCandidateStarted }
            val leftCancels = tracker?.leftCandidatesCancelled
                ?: samples.count { it.leftWinkCandidateCancelled }
            val rightCancels = tracker?.rightCandidatesCancelled
                ?: samples.count { it.rightWinkCandidateCancelled }
            val topLeftCancel = tracker?.leftCancelReasons?.maxByOrNull { it.value }?.key
                ?: samples.mapNotNull { it.candidateCancellationReason }
                    .groupingBy { it }.eachCount().maxByOrNull { it.value }?.key
            val topRightCancel = tracker?.rightCancelReasons?.maxByOrNull { it.value }?.key
                ?: samples.mapNotNull { it.candidateCancellationReason }
                    .groupingBy { it }.eachCount().maxByOrNull { it.value }?.key

            val leftClosedCross = tracker?.leftClosedCrossings
                ?: samples.count { it.closedThresholdCrossedLeft }
            val rightClosedCross = tracker?.rightClosedCrossings
                ?: samples.count { it.closedThresholdCrossedRight }
            val leftUncNoClosed = tracker?.leftUncertainNoClosed
                ?: samples.count { it.enteredUncertainWithoutClosedLeft }
            val rightUncNoClosed = tracker?.rightUncertainNoClosed
                ?: samples.count { it.enteredUncertainWithoutClosedRight }
            val leftReopenFail = tracker?.leftReopenFailures
                ?: samples.count { it.reopenFailedLeft }
            val rightReopenFail = tracker?.rightReopenFailures
                ?: samples.count { it.reopenFailedRight }
            val leftReopenOk = tracker?.leftReopenSuccesses
                ?: samples.count { it.leftWinkCompleted }
            val rightReopenOk = tracker?.rightReopenSuccesses
                ?: samples.count { it.rightWinkCompleted }

            val findings = mutableListOf<String>()
            val bothUncPct = pct(bothUnc)
            if (bothUncPct >= 5f) {
                findings += "Both eyes remained in the uncertain band for " +
                    "${"%.1f".format(bothUncPct)}% of sampled frames."
            }
            if (rightUnc * 100f / n >= 10f) {
                findings += "With measured samples, the right eye was UNCERTAIN for " +
                    "${"%.1f".format(pct(rightUnc))}% of frames."
            }
            if (leftUnc * 100f / n >= 10f) {
                findings += "With measured samples, the left eye was UNCERTAIN for " +
                    "${"%.1f".format(pct(leftUnc))}% of frames."
            }
            if (leftClosedCross > 0 && leftReopenOk == 0) {
                findings += "The left eye crossed the closed threshold $leftClosedCross times " +
                    "but produced zero completed winks" +
                    if (leftReopenFail > 0) {
                        " because reopen confirmation failed ($leftReopenFail)."
                    } else {
                        "."
                    }
            }
            if (rightClosedCross > 0 && rightReopenOk == 0) {
                findings += "The right eye crossed the closed threshold $rightClosedCross times " +
                    "but produced zero completed winks" +
                    if (rightReopenFail > 0) {
                        " because reopen confirmation failed ($rightReopenFail)."
                    } else {
                        "."
                    }
            }
            if (rightStarts == 0 && rightClosedCross == 0 && rightAttempt.isEmpty()) {
                findings += "No closed-threshold crossing was observed during attempted right winks."
            }
            if (leftStarts == 0 && leftClosedCross == 0 && leftAttempt.isEmpty()) {
                findings += "No closed-threshold crossing was observed during attempted left winks."
            }
            if (leftStarts > 0) {
                findings += "Left wink candidates started $leftStarts times" +
                    if (leftCancels > 0) {
                        " and were cancelled mainly because of ${topLeftCancel ?: "unknown"}."
                    } else {
                        "."
                    }
            }
            if (rightStarts > 0) {
                findings += "Right wink candidates started $rightStarts times" +
                    if (rightCancels > 0) {
                        " and were cancelled mainly because of ${topRightCancel ?: "unknown"}."
                    } else {
                        "."
                    }
            }

            return EyeDecisionAnalysis(
                leftOpenPercent = pct(leftOpen),
                leftClosedPercent = pct(leftClosed),
                leftUncertainPercent = pct(leftUnc),
                rightOpenPercent = pct(rightOpen),
                rightClosedPercent = pct(rightClosed),
                rightUncertainPercent = pct(rightUnc),
                bothUncertainPercent = bothUncPct,
                bothUncertainFrameCount = bothUnc,
                averageUncertainDurationMs = avgUnc,
                longestUncertainDurationMs = maxUnc,
                topLeftDecisionReasons = leftReasons,
                topRightDecisionReasons = rightReasons,
                topFrameRejectionReasons = rejectReasons,
                leftCandidatesStarted = leftStarts,
                leftCandidatesCancelled = leftCancels,
                topLeftCancelReason = topLeftCancel,
                rightCandidatesStarted = rightStarts,
                rightCandidatesCancelled = rightCancels,
                topRightCancelReason = topRightCancel,
                leftReopenSuccesses = leftReopenOk,
                leftReopenFailures = leftReopenFail,
                rightReopenSuccesses = rightReopenOk,
                rightReopenFailures = rightReopenFail,
                avgRawLeftSteadyOpen = steadyOpen.mapNotNull { it.leftEyeOpenProbability }
                    .takeIf { it.isNotEmpty() }?.average()?.toFloat(),
                avgRawRightSteadyOpen = steadyOpen.mapNotNull { it.rightEyeOpenProbability }
                    .takeIf { it.isNotEmpty() }?.average()?.toFloat(),
                avgRawLeftDuringLeftWinkAttempt = leftAttempt.mapNotNull { it.leftEyeOpenProbability }
                    .takeIf { it.isNotEmpty() }?.average()?.toFloat(),
                avgRawRightDuringLeftWinkAttempt = leftAttempt.mapNotNull { it.rightEyeOpenProbability }
                    .takeIf { it.isNotEmpty() }?.average()?.toFloat(),
                avgRawLeftDuringRightWinkAttempt = rightAttempt.mapNotNull { it.leftEyeOpenProbability }
                    .takeIf { it.isNotEmpty() }?.average()?.toFloat(),
                avgRawRightDuringRightWinkAttempt = rightAttempt.mapNotNull { it.rightEyeOpenProbability }
                    .takeIf { it.isNotEmpty() }?.average()?.toFloat(),
                minRawLeftDuringLeftWinkAttempt = leftAttempt.mapNotNull { it.leftEyeOpenProbability }
                    .minOrNull(),
                maxRawLeftDuringLeftWinkAttempt = leftAttempt.mapNotNull { it.leftEyeOpenProbability }
                    .maxOrNull(),
                minRawRightDuringRightWinkAttempt = rightAttempt.mapNotNull { it.rightEyeOpenProbability }
                    .minOrNull(),
                maxRawRightDuringRightWinkAttempt = rightAttempt.mapNotNull { it.rightEyeOpenProbability }
                    .maxOrNull(),
                leftClosedThresholdCrossings = leftClosedCross,
                rightClosedThresholdCrossings = rightClosedCross,
                leftEnteredUncertainWithoutClosed = leftUncNoClosed,
                rightEnteredUncertainWithoutClosed = rightUncNoClosed,
                cooldownBlocks = tracker?.cooldownBlocks
                    ?: samples.count { it.cooldownActiveLeft || it.cooldownActiveRight },
                factualFindings = findings
            )
        }
    }

    fun reportLines(): List<String> = buildList {
        add("--- Decision analysis ---")
        add("Left OPEN/CLOSED/UNCERTAIN %: ${fmt(leftOpenPercent)} / ${fmt(leftClosedPercent)} / ${fmt(leftUncertainPercent)}")
        add("Right OPEN/CLOSED/UNCERTAIN %: ${fmt(rightOpenPercent)} / ${fmt(rightClosedPercent)} / ${fmt(rightUncertainPercent)}")
        add("Both UNCERTAIN: $bothUncertainFrameCount frames (${fmt(bothUncertainPercent)}%)")
        add(
            "Uncertain period avg/longest ms: " +
                "${averageUncertainDurationMs?.let { "%.0f".format(it) } ?: "n/a"} / $longestUncertainDurationMs"
        )
        add("Top left decision reasons: ${formatPairs(topLeftDecisionReasons)}")
        add("Top right decision reasons: ${formatPairs(topRightDecisionReasons)}")
        add("Top frame rejection reasons: ${formatPairs(topFrameRejectionReasons)}")
        add("Left candidates started/cancelled: $leftCandidatesStarted / $leftCandidatesCancelled (top cancel: ${topLeftCancelReason ?: "none"})")
        add("Right candidates started/cancelled: $rightCandidatesStarted / $rightCandidatesCancelled (top cancel: ${topRightCancelReason ?: "none"})")
        add("Left reopen success/fail: $leftReopenSuccesses / $leftReopenFailures")
        add("Right reopen success/fail: $rightReopenSuccesses / $rightReopenFailures")
        add("Closed-threshold crossings L/R: $leftClosedThresholdCrossings / $rightClosedThresholdCrossings")
        add("Entered uncertain without closed L/R: $leftEnteredUncertainWithoutClosed / $rightEnteredUncertainWithoutClosed")
        add("Cooldown/debounce blocks: $cooldownBlocks")
        add("Avg raw L/R steady-open: ${fmtProb(avgRawLeftSteadyOpen)} / ${fmtProb(avgRawRightSteadyOpen)}")
        add(
            "Left-wink attempt raw L min/avg/max: " +
                "${fmtProb(minRawLeftDuringLeftWinkAttempt)} / " +
                "${fmtProb(avgRawLeftDuringLeftWinkAttempt)} / " +
                fmtProb(maxRawLeftDuringLeftWinkAttempt)
        )
        add(
            "Right-wink attempt raw R min/avg/max: " +
                "${fmtProb(minRawRightDuringRightWinkAttempt)} / " +
                "${fmtProb(avgRawRightDuringRightWinkAttempt)} / " +
                fmtProb(maxRawRightDuringRightWinkAttempt)
        )
        factualFindings.forEach { add("Finding: $it") }
    }

    private fun fmt(v: Float): String = "%.1f".format(v)
    private fun fmtProb(v: Float?): String = v?.let { "%.3f".format(it) } ?: "n/a"
    private fun formatPairs(pairs: List<Pair<String, Int>>): String =
        if (pairs.isEmpty()) "none" else pairs.joinToString { "${it.first}=${it.second}" }
}
