package com.idworx.lisa.features.eyediagnostic

import android.util.Log
import com.idworx.lisa.features.blinkdetectionreliability.BlinkProcessResult

/**
 * Temporary developer-only eye diagnostics for glasses reliability investigation.
 *
 * Observational only: never mutates wink state, never executes gestures/sequences,
 * and never captures or persists camera imagery.
 */
object LisaEyeDiagnostic {
    const val TAG = "LISA_EYE_DIAGNOSTIC"

    /** ~4–5 samples/sec when frames arrive faster than this. */
    const val DEFAULT_MIN_INTERVAL_MS: Long = 220L

    enum class InterpretedEyeState {
        OPEN,
        CLOSED,
        UNCERTAIN,
        NULL
    }

    data class Sample(
        val timestampMs: Long,
        val faceDetected: Boolean,
        val faceCount: Int,
        val boundingBoxWidthPx: Int?,
        val boundingBoxHeightPx: Int?,
        val faceWidthPercentOfImage: Float?,
        val leftEyeOpenProbability: Float?,
        val rightEyeOpenProbability: Float?,
        val eitherProbabilityNull: Boolean,
        val headEulerAngleY: Float?,
        val headEulerAngleZ: Float?,
        val sensitivityLevel: Int,
        val leftEyeClosedThreshold: Float,
        val rightEyeClosedThreshold: Float,
        val openEyeThreshold: Float,
        val interpretedLeftEyeState: InterpretedEyeState,
        val interpretedRightEyeState: InterpretedEyeState,
        val frameAccepted: Boolean,
        val rejectionReason: String?,
        val leftWinkCount: Int,
        val rightWinkCount: Int,
        val sequenceState: String,
        // Extended decision-trace fields (optional for backward-compatible session files).
        val smoothedLeftProbability: Float? = leftEyeOpenProbability,
        val smoothedRightProbability: Float? = rightEyeOpenProbability,
        val leftPreviousState: InterpretedEyeState? = null,
        val rightPreviousState: InterpretedEyeState? = null,
        val leftCandidateState: InterpretedEyeState? = null,
        val rightCandidateState: InterpretedEyeState? = null,
        val leftDecisionReason: String? = null,
        val rightDecisionReason: String? = null,
        val frameDecisionReason: String? = null,
        val leftConsecutiveSupport: Int = 0,
        val rightConsecutiveSupport: Int = 0,
        val requiredSupportCount: Int = 0,
        val leftTransitionAccepted: Boolean = false,
        val rightTransitionAccepted: Boolean = false,
        val leftWinkCandidateActive: Boolean = false,
        val rightWinkCandidateActive: Boolean = false,
        val leftWinkCandidateStarted: Boolean = false,
        val rightWinkCandidateStarted: Boolean = false,
        val leftWinkCandidateCancelled: Boolean = false,
        val rightWinkCandidateCancelled: Boolean = false,
        val candidateCancellationReason: String? = null,
        val leftWinkCompleted: Boolean = false,
        val rightWinkCompleted: Boolean = false,
        val cooldownActiveLeft: Boolean = false,
        val cooldownActiveRight: Boolean = false,
        val msSinceLeftTransition: Long? = null,
        val msSinceRightTransition: Long? = null,
        val closedThresholdCrossedLeft: Boolean = false,
        val closedThresholdCrossedRight: Boolean = false,
        val enteredUncertainWithoutClosedLeft: Boolean = false,
        val enteredUncertainWithoutClosedRight: Boolean = false,
        val reopenFailedLeft: Boolean = false,
        val reopenFailedRight: Boolean = false
    )

    fun interpretEyeState(
        probability: Float?,
        closedThreshold: Float,
        openThreshold: Float
    ): InterpretedEyeState {
        if (probability == null) return InterpretedEyeState.NULL
        return when {
            probability < closedThreshold -> InterpretedEyeState.CLOSED
            probability > openThreshold -> InterpretedEyeState.OPEN
            else -> InterpretedEyeState.UNCERTAIN
        }
    }

    fun faceWidthPercent(boundingBoxWidthPx: Int?, imageWidthPx: Int): Float? {
        if (boundingBoxWidthPx == null || imageWidthPx <= 0) return null
        return (boundingBoxWidthPx.toFloat() / imageWidthPx.toFloat()) * 100f
    }

    /**
     * Maps blink-processor skip/reject flags to a stable rejection reason.
     * Returns null when the frame was accepted into normal wink evaluation.
     */
    fun rejectionReasonFrom(result: BlinkProcessResult): String? = when {
        result.skippedBothUncertain -> "both_eyes_uncertain"
        result.skippedForJitter -> "jitter_skip"
        result.skippedUnstable -> "unstable_frame"
        result.rejectedUnprimed -> "unprimed_close"
        result.rejectedIncompleteShape -> "incomplete_wink_shape"
        else -> null
    }

    fun sampleFromDecisionTrace(
        base: Sample,
        trace: EyeDecisionFrameTrace
    ): Sample = base.copy(
        smoothedLeftProbability = trace.smoothedLeftProb,
        smoothedRightProbability = trace.smoothedRightProb,
        leftPreviousState = trace.leftPreviousState,
        rightPreviousState = trace.rightPreviousState,
        leftCandidateState = trace.leftCandidateState,
        rightCandidateState = trace.rightCandidateState,
        leftDecisionReason = trace.leftDecisionReason.name,
        rightDecisionReason = trace.rightDecisionReason.name,
        frameDecisionReason = trace.frameDecisionReason.name,
        leftConsecutiveSupport = trace.leftConsecutiveSupport,
        rightConsecutiveSupport = trace.rightConsecutiveSupport,
        requiredSupportCount = trace.requiredSupportCount,
        leftTransitionAccepted = trace.leftTransitionAccepted,
        rightTransitionAccepted = trace.rightTransitionAccepted,
        leftWinkCandidateActive = trace.leftWinkCandidateActive,
        rightWinkCandidateActive = trace.rightWinkCandidateActive,
        leftWinkCandidateStarted = trace.leftWinkCandidateStarted,
        rightWinkCandidateStarted = trace.rightWinkCandidateStarted,
        leftWinkCandidateCancelled = trace.leftWinkCandidateCancelled,
        rightWinkCandidateCancelled = trace.rightWinkCandidateCancelled,
        candidateCancellationReason = trace.candidateCancellationReason?.name,
        leftWinkCompleted = trace.leftWinkCompleted,
        rightWinkCompleted = trace.rightWinkCompleted,
        cooldownActiveLeft = trace.cooldownActiveLeft,
        cooldownActiveRight = trace.cooldownActiveRight,
        msSinceLeftTransition = trace.msSinceLeftTransition,
        msSinceRightTransition = trace.msSinceRightTransition,
        closedThresholdCrossedLeft = trace.closedThresholdCrossedLeft,
        closedThresholdCrossedRight = trace.closedThresholdCrossedRight,
        enteredUncertainWithoutClosedLeft = trace.enteredUncertainWithoutClosedLeft,
        enteredUncertainWithoutClosedRight = trace.enteredUncertainWithoutClosedRight,
        reopenFailedLeft = trace.reopenFailedLeft,
        reopenFailedRight = trace.reopenFailedRight
    )

    fun format(sample: Sample): String {
        val faceYesNo = if (sample.faceDetected) "yes" else "no"
        val acceptedYesNo = if (sample.frameAccepted) "accepted" else "rejected"
        val reason = sample.rejectionReason ?: "none"
        return buildString {
            append("ts=").append(sample.timestampMs)
            append(" face=").append(faceYesNo)
            append(" faces=").append(sample.faceCount)
            append(" bboxW=").append(sample.boundingBoxWidthPx ?: "null")
            append(" bboxH=").append(sample.boundingBoxHeightPx ?: "null")
            append(" faceW%=").append(sample.faceWidthPercentOfImage?.let { "%.1f".format(it) } ?: "null")
            append(" Lprob=").append(sample.leftEyeOpenProbability?.let { "%.3f".format(it) } ?: "null")
            append(" Rprob=").append(sample.rightEyeOpenProbability?.let { "%.3f".format(it) } ?: "null")
            append(" nullProb=").append(sample.eitherProbabilityNull)
            append(" yawY=").append(sample.headEulerAngleY?.let { "%.1f".format(it) } ?: "null")
            append(" rollZ=").append(sample.headEulerAngleZ?.let { "%.1f".format(it) } ?: "null")
            append(" sens=").append(sample.sensitivityLevel)
            append(" Lthr=").append("%.3f".format(sample.leftEyeClosedThreshold))
            append(" Rthr=").append("%.3f".format(sample.rightEyeClosedThreshold))
            append(" Othr=").append("%.3f".format(sample.openEyeThreshold))
            append(" Lstate=").append(sample.interpretedLeftEyeState.name)
            append(" Rstate=").append(sample.interpretedRightEyeState.name)
            append(" frame=").append(acceptedYesNo)
            append(" reason=").append(reason)
            append(" Lwinks=").append(sample.leftWinkCount)
            append(" Rwinks=").append(sample.rightWinkCount)
            append(" seq=").append(sample.sequenceState)
            sample.frameDecisionReason?.let { append(" frameReason=").append(it) }
            sample.leftDecisionReason?.let { append(" Ldec=").append(it) }
            sample.rightDecisionReason?.let { append(" Rdec=").append(it) }
        }
    }

    /**
     * Rate-limited diagnostic emitter. When [enabled] is false (release builds), emits nothing.
     */
    class Logger(
        private val enabled: Boolean,
        private val minIntervalMs: Long = DEFAULT_MIN_INTERVAL_MS,
        private val clockMs: () -> Long = { System.currentTimeMillis() },
        private val sink: (tag: String, message: String) -> Unit = { tag, message ->
            Log.i(tag, message)
        }
    ) {
        @Volatile
        private var lastEmitMs: Long = 0L

        fun isEnabled(): Boolean = enabled

        fun maybeEmit(sample: Sample) {
            if (!enabled) return
            val now = clockMs()
            val previous = lastEmitMs
            if (previous != 0L && now - previous < minIntervalMs) return
            lastEmitMs = now
            sink(TAG, format(sample))
        }
    }
}
