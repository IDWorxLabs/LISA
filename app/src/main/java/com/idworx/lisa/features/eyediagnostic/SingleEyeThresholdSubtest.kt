package com.idworx.lisa.features.eyediagnostic

import kotlin.math.abs

/**
 * Mandatory single-eye threshold probe (one eye per run).
 * Observes probabilities only — never executes production gestures.
 */
class SingleEyeThresholdSubtest(
    private val clockMs: () -> Long = { System.currentTimeMillis() }
) {
    enum class EyeTarget { Left, Right }
    enum class Phase {
        Idle,
        BothOpen,
        CloseHold,
        ReopenHold,
        BetweenReps,
        Complete,
        Failed
    }

    data class Ui(
        val active: Boolean = false,
        val eye: EyeTarget = EyeTarget.Left,
        val phase: Phase = Phase.Idle,
        val instruction: String = "",
        val remainingMs: Long = 0L,
        val cycleIndex: Int = 0,
        val totalCycles: Int = TOTAL_REPS,
        val selectedProb: Float? = null,
        val oppositeProb: Float? = null,
        val selectedState: LisaEyeDiagnostic.InterpretedEyeState =
            LisaEyeDiagnostic.InterpretedEyeState.NULL,
        val closedThreshold: Float = 0f,
        val openThreshold: Float = 0f,
        val currentResult: String = "",
        val outcome: EyeTestComponentOutcome? = null
    )

    companion object {
        const val BOTH_OPEN_MS: Long = 5_000L
        const val CLOSE_HOLD_MS: Long = 2_000L
        const val REOPEN_HOLD_MS: Long = 2_000L
        const val BETWEEN_REPS_MS: Long = 500L
        const val TOTAL_REPS: Int = 3
        /** Opposite eye considered stable if stays above open threshold during close. */
        const val OPPOSITE_STABLE_MIN_OPEN_RATIO: Float = 0.7f
    }

    var ui: Ui = Ui()
        private set

    private var phaseStartedMs: Long = 0L
    private var testStartedMs: Long = 0L
    private var closedThr: Float = 0.25f
    private var openThr: Float = 0.75f
    private var componentId: EyeTestComponentId = EyeTestComponentId.WithoutGlassesLeftEye

    private val selectedAll = mutableListOf<Float>()
    private val oppositeAll = mutableListOf<Float>()
    private val openBaseline = mutableListOf<Float>()
    private val closeHold = mutableListOf<Float>()
    private val reopenHold = mutableListOf<Float>()
    private val oppositeDuringClose = mutableListOf<Float>()

    private var acceptedSamples = 0
    private var rejectedSamples = 0
    private var nullSelected = 0
    private var nullOpposite = 0
    private var timeOpenMs = 0L
    private var timeClosedMs = 0L
    private var timeUncertainMs = 0L
    private var longestUncertainMs = 0L
    private var uncertainRunMs = 0L
    private var uncertainEntries = 0
    private var lastSampleMs = 0L
    private var wasUncertain = false

    private var crossedClosed = false
    private var crossedReopen = false
    private val cycles = mutableListOf<SingleEyeCycleRecord>()
    private val decisionReasons = mutableMapOf<String, Int>()
    private val rejectionReasons = mutableMapOf<String, Int>()

    private var builtResult: SingleEyeComponentResult? = null

    fun start(
        eye: EyeTarget,
        componentId: EyeTestComponentId,
        closedThreshold: Float,
        openThreshold: Float
    ) {
        this.componentId = componentId
        closedThr = closedThreshold
        openThr = openThreshold
        clearBuffers()
        testStartedMs = clockMs()
        phaseStartedMs = testStartedMs
        lastSampleMs = testStartedMs
        builtResult = null
        ui = Ui(
            active = true,
            eye = eye,
            phase = Phase.BothOpen,
            instruction = "Keep both eyes open and steady.",
            remainingMs = BOTH_OPEN_MS,
            cycleIndex = 0,
            closedThreshold = closedThr,
            openThreshold = openThr,
            currentResult = "Recording open baseline…",
            outcome = null
        )
    }

    fun skipAndRecordFailure(): SingleEyeComponentResult {
        val result = finalize(
            EyeTestComponentOutcome.Skipped,
            forceFailureReason = "manual_skip"
        )
        ui = ui.copy(
            active = false,
            phase = Phase.Failed,
            instruction = "Skipped — failure recorded.",
            outcome = EyeTestComponentOutcome.Skipped,
            remainingMs = 0L
        )
        return result
    }

    /** Deactivate without recording (restart / leave flow). */
    fun resetIdle() {
        clearBuffers()
        builtResult = null
        ui = Ui(active = false, phase = Phase.Idle, instruction = "")
    }

    @Deprecated("Use resetIdle or skipAndRecordFailure", ReplaceWith("resetIdle()"))
    fun skip() = resetIdle()

    fun timeoutAndRecordFailure(): SingleEyeComponentResult {
        val result = finalize(
            EyeTestComponentOutcome.TimedOut,
            forceFailureReason = "timeout"
        )
        ui = ui.copy(
            active = false,
            phase = Phase.Failed,
            instruction = "Timed out — failure recorded.",
            outcome = EyeTestComponentOutcome.TimedOut,
            remainingMs = 0L
        )
        return result
    }

    fun endEarlyAndRecordFailure(): SingleEyeComponentResult {
        val result = finalize(
            EyeTestComponentOutcome.EndedEarly,
            forceFailureReason = "ended_early"
        )
        ui = ui.copy(
            active = false,
            phase = Phase.Failed,
            instruction = "Ended early — failure recorded.",
            outcome = EyeTestComponentOutcome.EndedEarly,
            remainingMs = 0L
        )
        return result
    }

    fun resultOrNull(): SingleEyeComponentResult? = builtResult

    fun onTick(nowMs: Long = clockMs()): Boolean {
        if (!ui.active) return false
        val duration = phaseDuration(ui.phase) ?: return false
        val remaining = (duration - (nowMs - phaseStartedMs)).coerceAtLeast(0L)
        ui = ui.copy(remainingMs = remaining)
        if (remaining > 0L) return false
        advancePhase(nowMs)
        return true
    }

    fun onSample(
        selectedEyeProb: Float?,
        oppositeEyeProb: Float?,
        frameAccepted: Boolean,
        rejectionReason: String?,
        decisionReason: String?,
        nowMs: Long = clockMs()
    ) {
        if (!ui.active) return
        val dt = if (lastSampleMs > 0L) (nowMs - lastSampleMs).coerceAtLeast(0L) else 0L
        lastSampleMs = nowMs

        if (frameAccepted) acceptedSamples++ else rejectedSamples++
        if (selectedEyeProb == null) nullSelected++ else selectedAll += selectedEyeProb
        if (oppositeEyeProb == null) nullOpposite++ else oppositeAll += oppositeEyeProb
        rejectionReason?.let { rejectionReasons[it] = (rejectionReasons[it] ?: 0) + 1 }
        decisionReason?.let { decisionReasons[it] = (decisionReasons[it] ?: 0) + 1 }

        val state = LisaEyeDiagnostic.interpretEyeState(selectedEyeProb, closedThr, openThr)
        when (state) {
            LisaEyeDiagnostic.InterpretedEyeState.OPEN -> timeOpenMs += dt
            LisaEyeDiagnostic.InterpretedEyeState.CLOSED -> timeClosedMs += dt
            LisaEyeDiagnostic.InterpretedEyeState.UNCERTAIN -> {
                timeUncertainMs += dt
                uncertainRunMs += dt
                if (!wasUncertain) uncertainEntries++
                wasUncertain = true
                if (uncertainRunMs > longestUncertainMs) longestUncertainMs = uncertainRunMs
            }
            LisaEyeDiagnostic.InterpretedEyeState.NULL -> Unit
        }
        if (state != LisaEyeDiagnostic.InterpretedEyeState.UNCERTAIN) {
            wasUncertain = false
            uncertainRunMs = 0L
        }

        when (ui.phase) {
            Phase.BothOpen -> selectedEyeProb?.let { openBaseline += it }
            Phase.CloseHold -> {
                selectedEyeProb?.let { closeHold += it }
                oppositeEyeProb?.let { oppositeDuringClose += it }
                if (selectedEyeProb != null && selectedEyeProb < closedThr) crossedClosed = true
            }
            Phase.ReopenHold -> {
                selectedEyeProb?.let { reopenHold += it }
                if (selectedEyeProb != null && selectedEyeProb > openThr) crossedReopen = true
            }
            else -> Unit
        }

        ui = ui.copy(
            selectedProb = selectedEyeProb,
            oppositeProb = oppositeEyeProb,
            selectedState = state,
            currentResult = liveResultLabel()
        )
    }

    private fun liveResultLabel(): String = when (ui.phase) {
        Phase.BothOpen -> "Open baseline"
        Phase.CloseHold -> if (crossedClosed) "Closed threshold crossed" else "Waiting for close…"
        Phase.ReopenHold -> if (crossedReopen) "Open threshold crossed" else "Waiting for reopen…"
        Phase.BetweenReps -> "Rest"
        Phase.Complete -> "Complete"
        Phase.Failed -> ui.outcome?.name ?: "Failed"
        Phase.Idle -> ""
    }

    private fun phaseDuration(phase: Phase): Long? = when (phase) {
        Phase.BothOpen -> BOTH_OPEN_MS
        Phase.CloseHold -> CLOSE_HOLD_MS
        Phase.ReopenHold -> REOPEN_HOLD_MS
        Phase.BetweenReps -> BETWEEN_REPS_MS
        else -> null
    }

    private fun advancePhase(nowMs: Long) {
        when (ui.phase) {
            Phase.BothOpen -> enter(Phase.CloseHold, nowMs, closeInstruction())
            Phase.CloseHold -> enter(Phase.ReopenHold, nowMs, "Reopen both eyes and hold.")
            Phase.ReopenHold -> {
                completeCycle()
                if (ui.cycleIndex + 1 >= TOTAL_REPS) {
                    val result = finalize(EyeTestComponentOutcome.Success)
                    ui = ui.copy(
                        active = false,
                        phase = Phase.Complete,
                        instruction = "Left/right eye threshold test finished.",
                        remainingMs = 0L,
                        outcome = result.outcome,
                        currentResult = "Wink recognised ${result.winkRecognisedCount}/$TOTAL_REPS"
                    )
                } else {
                    enter(Phase.BetweenReps, nowMs, "Rest briefly…")
                }
            }
            Phase.BetweenReps -> {
                resetCycleBuffers()
                enter(Phase.BothOpen, nowMs, "Keep both eyes open and steady.")
                ui = ui.copy(cycleIndex = ui.cycleIndex + 1)
            }
            else -> Unit
        }
    }

    private fun completeCycle() {
        val closeAvg = EyeTestStats.avg(closeHold)
        val reopenAvg = EyeTestStats.avg(reopenHold)
        val openAvg = EyeTestStats.avg(openBaseline)
        val lowestClose = closeHold.minOrNull()
        val highestReopen = reopenHold.maxOrNull()
        val oppositeStable = oppositeStableDuringClose()
        val wink = crossedClosed && crossedReopen
        val fail = when {
            wink -> null
            !crossedClosed && !crossedReopen -> "no_closed_or_open_threshold_crossing"
            !crossedClosed -> "closed_threshold_not_crossed"
            !crossedReopen -> "reopen_threshold_not_crossed"
            else -> "wink_shape_incomplete"
        }
        cycles += SingleEyeCycleRecord(
            cycleIndex = ui.cycleIndex,
            openBaselineAvg = openAvg,
            closedHoldAvg = closeAvg,
            reopenAvg = reopenAvg,
            lowestDuringClose = lowestClose,
            highestDuringReopen = highestReopen,
            closedThresholdCrossed = crossedClosed,
            openThresholdCrossed = crossedReopen,
            closeTransitionAccepted = crossedClosed,
            reopenTransitionAccepted = crossedReopen,
            wouldRecogniseWink = wink,
            failureReason = fail,
            oppositeEyeStableDuringClose = oppositeStable
        )
    }

    private fun oppositeStableDuringClose(): Boolean {
        if (oppositeDuringClose.isEmpty()) return false
        val openCount = oppositeDuringClose.count { it > openThr }
        return openCount.toFloat() / oppositeDuringClose.size >= OPPOSITE_STABLE_MIN_OPEN_RATIO
    }

    private fun finalize(
        preferredOutcome: EyeTestComponentOutcome,
        forceFailureReason: String? = null
    ): SingleEyeComponentResult {
        if (ui.phase == Phase.CloseHold || ui.phase == Phase.ReopenHold || ui.phase == Phase.BothOpen) {
            // Capture partial cycle if aborted mid-cycle.
            if (closeHold.isNotEmpty() || reopenHold.isNotEmpty() || openBaseline.isNotEmpty()) {
                if (cycles.none { it.cycleIndex == ui.cycleIndex }) {
                    completeCycle()
                }
            }
        }
        val winkCount = cycles.count { it.wouldRecogniseWink }
        val outcome = when {
            preferredOutcome != EyeTestComponentOutcome.Success -> preferredOutcome
            winkCount == TOTAL_REPS -> EyeTestComponentOutcome.Success
            winkCount == 0 -> EyeTestComponentOutcome.Failed
            else -> EyeTestComponentOutcome.Failed
        }
        if (forceFailureReason != null && cycles.isEmpty()) {
            cycles += SingleEyeCycleRecord(
                cycleIndex = 0,
                openBaselineAvg = EyeTestStats.avg(openBaseline),
                closedHoldAvg = EyeTestStats.avg(closeHold),
                reopenAvg = EyeTestStats.avg(reopenHold),
                lowestDuringClose = closeHold.minOrNull(),
                highestDuringReopen = reopenHold.maxOrNull(),
                closedThresholdCrossed = crossedClosed,
                openThresholdCrossed = crossedReopen,
                closeTransitionAccepted = crossedClosed,
                reopenTransitionAccepted = crossedReopen,
                wouldRecogniseWink = false,
                failureReason = forceFailureReason,
                oppositeEyeStableDuringClose = oppositeStableDuringClose()
            )
        }
        val total = acceptedSamples + rejectedSamples
        val result = SingleEyeComponentResult(
            componentId = componentId,
            eye = ui.eye,
            outcome = outcome,
            closedThreshold = closedThr,
            openThreshold = openThr,
            totalSamples = total,
            acceptedSamples = acceptedSamples,
            rejectedSamples = rejectedSamples,
            selectedMin = selectedAll.minOrNull(),
            selectedMax = selectedAll.maxOrNull(),
            selectedAvg = EyeTestStats.avg(selectedAll),
            selectedMedian = EyeTestStats.median(selectedAll),
            selectedStdDev = EyeTestStats.stdDev(selectedAll),
            oppositeMin = oppositeAll.minOrNull(),
            oppositeMax = oppositeAll.maxOrNull(),
            oppositeAvg = EyeTestStats.avg(oppositeAll),
            oppositeMedian = EyeTestStats.median(oppositeAll),
            oppositeStdDev = EyeTestStats.stdDev(oppositeAll),
            openBaselineAvg = EyeTestStats.avg(openBaseline),
            closedThresholdCrossings = cycles.count { it.closedThresholdCrossed },
            openThresholdCrossings = cycles.count { it.openThresholdCrossed },
            uncertainBandEntries = uncertainEntries,
            timeOpenMs = timeOpenMs,
            timeClosedMs = timeClosedMs,
            timeUncertainMs = timeUncertainMs,
            longestUncertainMs = longestUncertainMs,
            nullSelectedCount = nullSelected,
            nullOppositeCount = nullOpposite,
            nullSelectedPercent = if (total == 0) 0f else nullSelected * 100f / total,
            nullOppositePercent = if (total == 0) 0f else nullOpposite * 100f / total,
            winkRecognisedCount = winkCount,
            cycles = cycles.toList(),
            topDecisionReason = decisionReasons.maxByOrNull { it.value }?.key,
            topRejectionReason = rejectionReasons.maxByOrNull { it.value }?.key,
            elapsedMs = (clockMs() - testStartedMs).coerceAtLeast(0L)
        )
        builtResult = result
        return result
    }

    private fun enter(phase: Phase, nowMs: Long, instruction: String) {
        phaseStartedMs = nowMs
        ui = ui.copy(
            phase = phase,
            instruction = instruction,
            remainingMs = phaseDuration(phase) ?: 0L,
            currentResult = liveResultLabel()
        )
    }

    private fun resetCycleBuffers() {
        openBaseline.clear()
        closeHold.clear()
        reopenHold.clear()
        oppositeDuringClose.clear()
        crossedClosed = false
        crossedReopen = false
    }

    private fun clearBuffers() {
        selectedAll.clear()
        oppositeAll.clear()
        resetCycleBuffers()
        cycles.clear()
        decisionReasons.clear()
        rejectionReasons.clear()
        acceptedSamples = 0
        rejectedSamples = 0
        nullSelected = 0
        nullOpposite = 0
        timeOpenMs = 0L
        timeClosedMs = 0L
        timeUncertainMs = 0L
        longestUncertainMs = 0L
        uncertainRunMs = 0L
        uncertainEntries = 0
        wasUncertain = false
    }

    private fun closeInstruction(): String = when (ui.eye) {
        EyeTarget.Left -> "Close only your LEFT eye and hold."
        EyeTarget.Right -> "Close only your RIGHT eye and hold."
    }
}
