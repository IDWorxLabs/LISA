package com.idworx.lisa.features.blinkdetectionreliability

import kotlin.math.abs

data class BlinkEyeProbabilities(
    val userLeft: Float,
    val userRight: Float
)

data class BlinkProcessResult(
    val acceptLeft: Boolean = false,
    val acceptRight: Boolean = false,
    val leftCandidate: Boolean = false,
    val rightCandidate: Boolean = false,
    val leftStreak: Int = 0,
    val rightStreak: Int = 0,
    val skippedForJitter: Boolean = false,
    val skippedBothUncertain: Boolean = false,
    val skippedUnstable: Boolean = false,
    val rejectedUnprimed: Boolean = false,
    val rejectedIncompleteShape: Boolean = false,
    /**
     * Observational fields for debug decision tracing only.
     * Populated alongside production decisions; never alter wink acceptance.
     */
    val diagnosticPrevLeftProb: Float? = null,
    val diagnosticPrevRightProb: Float? = null,
    val diagnosticLeftUncertain: Boolean = false,
    val diagnosticRightUncertain: Boolean = false,
    val diagnosticBothOpen: Boolean = false,
    val diagnosticLeftPrimed: Boolean = false,
    val diagnosticRightPrimed: Boolean = false,
    val diagnosticLeftCooldownBlocked: Boolean = false,
    val diagnosticRightCooldownBlocked: Boolean = false,
    val diagnosticRequiredWinkFrames: Int = 0,
    val diagnosticOpenPrimingFrames: Int = 0,
    val diagnosticLeftSupportCount: Int = 0,
    val diagnosticRightSupportCount: Int = 0,
    val diagnosticReopenIncompleteLeft: Boolean = false,
    val diagnosticReopenIncompleteRight: Boolean = false,
    val diagnosticClosedThresholdLeft: Float = 0f,
    val diagnosticClosedThresholdRight: Float = 0f,
    val diagnosticOpenThreshold: Float = 0f
)

/**
 * Stateful per-frame blink detector requiring open → close → open shape,
 * with priming, streak grace, and sequence-aware jitter tolerance.
 */
class BlinkDetectionProcessor(
    var tuning: BlinkDetectionTuning = BlinkDetectionTuning.default
) {
    private var prevLeft: Float? = null
    private var prevRight: Float? = null
    private var leftPrimingFrames = 0
    private var rightPrimingFrames = 0
    private var leftClosingFrames = 0
    private var rightClosingFrames = 0
    private var leftCloseGrace = 0
    private var rightCloseGrace = 0
    private var leftCountedThisGesture = false
    private var rightCountedThisGesture = false
    private var lastLeftAcceptedMs = 0L
    private var lastRightAcceptedMs = 0L

    var lastLeftCandidate: Boolean = false
        private set
    var lastRightCandidate: Boolean = false
        private set

    fun processFrame(
        eyes: BlinkEyeProbabilities,
        nowMs: Long,
        acceptedLeftCount: Int,
        acceptedRightCount: Int
    ): BlinkProcessResult {
        val leftProb = eyes.userLeft
        val rightProb = eyes.userRight
        val capturedPrevLeft = prevLeft
        val capturedPrevRight = prevRight
        val hasActiveSequence = acceptedLeftCount > 0 || acceptedRightCount > 0
        val hasActiveClose = leftClosingFrames > 0 || rightClosingFrames > 0
        val bothOpen = tuning.isBothEyesOpen(leftProb, rightProb)

        if (bothOpen) {
            leftCountedThisGesture = false
            rightCountedThisGesture = false
        }

        val leftUncertain = tuning.isEyeUncertain(leftProb)
        val rightUncertain = tuning.isEyeUncertain(rightProb)
        val leftCandidate = tuning.isLeftWinkCandidate(leftProb, rightProb)
        val rightCandidate = tuning.isRightWinkCandidate(leftProb, rightProb)
        lastLeftCandidate = leftCandidate
        lastRightCandidate = rightCandidate

        var rejectedUnprimed = false
        var rejectedIncompleteShape = false

        if (leftUncertain && rightUncertain && !hasActiveSequence && !hasActiveClose) {
            resetTracking()
            prevLeft = leftProb
            prevRight = rightProb
            return BlinkProcessResult(
                skippedBothUncertain = true,
                skippedUnstable = true,
                diagnosticPrevLeftProb = capturedPrevLeft,
                diagnosticPrevRightProb = capturedPrevRight,
                diagnosticLeftUncertain = true,
                diagnosticRightUncertain = true,
                diagnosticBothOpen = bothOpen,
                diagnosticRequiredWinkFrames = tuning.requiredWinkFrames,
                diagnosticOpenPrimingFrames = tuning.openPrimingFrames,
                diagnosticClosedThresholdLeft = tuning.effectiveLeftClosedThreshold,
                diagnosticClosedThresholdRight = tuning.effectiveRightClosedThreshold,
                diagnosticOpenThreshold = tuning.openEyeThreshold
            )
        }

        val completingBlinkReopen = bothOpen && hasActiveClose
        if (!leftCandidate && !rightCandidate &&
            !completingBlinkReopen &&
            shouldSkipForJitter(leftProb, rightProb, hasActiveSequence || hasActiveClose)
        ) {
            prevLeft = leftProb
            prevRight = rightProb
            return BlinkProcessResult(
                leftCandidate = leftCandidate,
                rightCandidate = rightCandidate,
                leftStreak = leftClosingFrames,
                rightStreak = rightClosingFrames,
                skippedForJitter = true,
                skippedUnstable = !hasActiveSequence,
                diagnosticPrevLeftProb = capturedPrevLeft,
                diagnosticPrevRightProb = capturedPrevRight,
                diagnosticLeftUncertain = leftUncertain,
                diagnosticRightUncertain = rightUncertain,
                diagnosticBothOpen = bothOpen,
                diagnosticLeftSupportCount = leftClosingFrames,
                diagnosticRightSupportCount = rightClosingFrames,
                diagnosticRequiredWinkFrames = tuning.requiredWinkFrames,
                diagnosticOpenPrimingFrames = tuning.openPrimingFrames,
                diagnosticClosedThresholdLeft = tuning.effectiveLeftClosedThreshold,
                diagnosticClosedThresholdRight = tuning.effectiveRightClosedThreshold,
                diagnosticOpenThreshold = tuning.openEyeThreshold
            )
        }

        val leftPrimed = leftPrimingFrames >= tuning.openPrimingFrames || hasActiveSequence
        val rightPrimed = rightPrimingFrames >= tuning.openPrimingFrames || hasActiveSequence

        if (leftCandidate) {
            leftCloseGrace = 0
            if (leftPrimed) {
                leftClosingFrames++
            } else {
                rejectedUnprimed = true
            }
        } else if (leftClosingFrames > 0 && !bothOpen) {
            if (leftCloseGrace < tuning.streakGraceFrames) {
                leftCloseGrace++
            } else {
                if (leftClosingFrames < tuning.requiredWinkFrames) {
                    rejectedIncompleteShape = true
                }
                leftClosingFrames = 0
                leftCloseGrace = 0
            }
        }

        if (rightCandidate) {
            rightCloseGrace = 0
            if (rightPrimed) {
                rightClosingFrames++
            } else {
                rejectedUnprimed = true
            }
        } else if (rightClosingFrames > 0 && !bothOpen) {
            if (rightCloseGrace < tuning.streakGraceFrames) {
                rightCloseGrace++
            } else {
                if (rightClosingFrames < tuning.requiredWinkFrames) {
                    rejectedIncompleteShape = true
                }
                rightClosingFrames = 0
                rightCloseGrace = 0
            }
        }

        val completedLeftClose = leftClosingFrames
        val completedRightClose = rightClosingFrames

        val leftCooldownBlocked = bothOpen &&
            completedLeftClose >= tuning.requiredWinkFrames &&
            leftPrimed &&
            !leftCountedThisGesture &&
            nowMs - lastLeftAcceptedMs < tuning.cooldownMs
        val rightCooldownBlocked = bothOpen &&
            completedRightClose >= tuning.requiredWinkFrames &&
            rightPrimed &&
            !rightCountedThisGesture &&
            nowMs - lastRightAcceptedMs < tuning.cooldownMs

        val acceptLeft = bothOpen &&
            completedLeftClose >= tuning.requiredWinkFrames &&
            leftPrimed &&
            !leftCountedThisGesture &&
            nowMs - lastLeftAcceptedMs >= tuning.cooldownMs

        val acceptRight = bothOpen &&
            completedRightClose >= tuning.requiredWinkFrames &&
            rightPrimed &&
            !rightCountedThisGesture &&
            nowMs - lastRightAcceptedMs >= tuning.cooldownMs

        var reopenIncompleteLeft = false
        var reopenIncompleteRight = false
        if (bothOpen) {
            leftPrimingFrames = (leftPrimingFrames + 1).coerceAtMost(tuning.openPrimingFrames + 2)
            rightPrimingFrames = (rightPrimingFrames + 1).coerceAtMost(tuning.openPrimingFrames + 2)
            if (completedLeftClose > 0 && !acceptLeft && completedLeftClose < tuning.requiredWinkFrames) {
                rejectedIncompleteShape = true
                reopenIncompleteLeft = true
            }
            if (completedRightClose > 0 && !acceptRight && completedRightClose < tuning.requiredWinkFrames) {
                rejectedIncompleteShape = true
                reopenIncompleteRight = true
            }
            leftClosingFrames = 0
            leftCloseGrace = 0
            rightClosingFrames = 0
            rightCloseGrace = 0
        }

        if (acceptLeft) {
            leftCountedThisGesture = true
            lastLeftAcceptedMs = nowMs
        }
        if (acceptRight) {
            rightCountedThisGesture = true
            lastRightAcceptedMs = nowMs
        }

        prevLeft = leftProb
        prevRight = rightProb

        return BlinkProcessResult(
            acceptLeft = acceptLeft,
            acceptRight = acceptRight,
            leftCandidate = leftCandidate,
            rightCandidate = rightCandidate,
            leftStreak = completedLeftClose,
            rightStreak = completedRightClose,
            rejectedUnprimed = rejectedUnprimed,
            rejectedIncompleteShape = rejectedIncompleteShape,
            diagnosticPrevLeftProb = capturedPrevLeft,
            diagnosticPrevRightProb = capturedPrevRight,
            diagnosticLeftUncertain = leftUncertain,
            diagnosticRightUncertain = rightUncertain,
            diagnosticBothOpen = bothOpen,
            diagnosticLeftPrimed = leftPrimed,
            diagnosticRightPrimed = rightPrimed,
            diagnosticLeftCooldownBlocked = leftCooldownBlocked,
            diagnosticRightCooldownBlocked = rightCooldownBlocked,
            diagnosticRequiredWinkFrames = tuning.requiredWinkFrames,
            diagnosticOpenPrimingFrames = tuning.openPrimingFrames,
            diagnosticLeftSupportCount = completedLeftClose,
            diagnosticRightSupportCount = completedRightClose,
            diagnosticReopenIncompleteLeft = reopenIncompleteLeft,
            diagnosticReopenIncompleteRight = reopenIncompleteRight,
            diagnosticClosedThresholdLeft = tuning.effectiveLeftClosedThreshold,
            diagnosticClosedThresholdRight = tuning.effectiveRightClosedThreshold,
            diagnosticOpenThreshold = tuning.openEyeThreshold
        )
    }

    fun resetGestureFlags() {
        leftCountedThisGesture = false
        rightCountedThisGesture = false
    }

    fun resetSequence() {
        resetTracking()
        lastLeftCandidate = false
        lastRightCandidate = false
    }

    fun resetCooldowns() {
        lastLeftAcceptedMs = 0L
        lastRightAcceptedMs = 0L
    }

    fun clearPreviousProbabilities() {
        prevLeft = null
        prevRight = null
    }

    private fun resetTracking() {
        prevLeft = null
        prevRight = null
        leftPrimingFrames = 0
        rightPrimingFrames = 0
        leftClosingFrames = 0
        rightClosingFrames = 0
        leftCloseGrace = 0
        rightCloseGrace = 0
        leftCountedThisGesture = false
        rightCountedThisGesture = false
    }

    private fun shouldSkipForJitter(leftProb: Float, rightProb: Float, lenient: Boolean): Boolean {
        val pLeft = prevLeft ?: return false
        val pRight = prevRight ?: return false
        val threshold = if (lenient) tuning.jitterThresholdActive else tuning.jitterThresholdIdle
        return abs(leftProb - pLeft) > threshold || abs(rightProb - pRight) > threshold
    }
}

/** Simulate open → close → open for unit tests and validation. */
fun BlinkDetectionProcessor.simulateAcceptedLeftWink(
    tuning: BlinkDetectionTuning = this.tuning,
    nowMs: Long = 1000L,
    acceptedLeft: Int = 0,
    acceptedRight: Int = 0
): BlinkProcessResult {
    val open = BlinkEyeProbabilities(0.92f, 0.92f)
    val closed = BlinkEyeProbabilities(0.08f, 0.92f)
    repeat(tuning.openPrimingFrames) {
        processFrame(open, nowMs + it * 16L, acceptedLeft, acceptedRight)
    }
    repeat(tuning.requiredWinkFrames) {
        processFrame(closed, nowMs + 100L + it * 16L, acceptedLeft, acceptedRight)
    }
    return processFrame(open, nowMs + 200L, acceptedLeft, acceptedRight)
}

fun BlinkDetectionProcessor.simulateAcceptedRightWink(
    tuning: BlinkDetectionTuning = this.tuning,
    nowMs: Long = 1000L,
    acceptedLeft: Int = 0,
    acceptedRight: Int = 0
): BlinkProcessResult {
    val open = BlinkEyeProbabilities(0.92f, 0.92f)
    val closed = BlinkEyeProbabilities(0.92f, 0.08f)
    repeat(tuning.openPrimingFrames) {
        processFrame(open, nowMs + it * 16L, acceptedLeft, acceptedRight)
    }
    repeat(tuning.requiredWinkFrames) {
        processFrame(closed, nowMs + 100L + it * 16L, acceptedLeft, acceptedRight)
    }
    return processFrame(open, nowMs + 200L, acceptedLeft, acceptedRight)
}
