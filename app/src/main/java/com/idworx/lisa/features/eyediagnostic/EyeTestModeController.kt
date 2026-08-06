package com.idworx.lisa.features.eyediagnostic

import com.idworx.lisa.GuidedModeNavigation
import com.idworx.lisa.features.brain1interactionstandard.model.UniversalInteractionGestures
import java.io.File

/**
 * Guided Eye Test wizard controller. Observes diagnostics and wink events only —
 * never mutates BlinkDetectionProcessor or executes production gestures.
 *
 * Mandatory flow: Main steps → Left eye → Right eye → Result per phase,
 * then TestComplete → FullResults when all six components are terminal.
 */
class EyeTestModeController(
    private val store: EyeTestSessionStore,
    private val isDebugBuild: Boolean,
    private val clockMs: () -> Long = { System.currentTimeMillis() }
) {
    data class LiveUi(
        val faceDetected: Boolean = false,
        val faceCount: Int = 0,
        val faceWidthPercent: Float? = null,
        val headYaw: Float? = null,
        val headRoll: Float? = null,
        val frameAccepted: Boolean = false,
        val rejectionReason: String? = null,
        val leftProb: Float? = null,
        val rightProb: Float? = null,
        val leftState: LisaEyeDiagnostic.InterpretedEyeState =
            LisaEyeDiagnostic.InterpretedEyeState.NULL,
        val rightState: LisaEyeDiagnostic.InterpretedEyeState =
            LisaEyeDiagnostic.InterpretedEyeState.NULL,
        val leftClosedThr: Float = 0f,
        val rightClosedThr: Float = 0f,
        val openThr: Float = 0f,
        val leftNull: Boolean = true,
        val rightNull: Boolean = true,
        val leftWinks: Int = 0,
        val rightWinks: Int = 0,
        val sequenceState: String = "",
        val sensitivity: Int = 0,
        val responseTimeSec: Int = 0
    )

    private data class DeviceInfo(
        val versionName: String = "",
        val versionCode: Int = 0,
        val manufacturer: String = "",
        val model: String = "",
        val android: String = ""
    )

    var isOpen: Boolean = false
        private set

    var flowState: EyeTestFlowState = EyeTestFlowState.WithoutGlassesPreparation
        private set

    var live: LiveUi = LiveUi()
        private set

    var stepLeftWinks: Int = 0
        private set
    var stepRightWinks: Int = 0
        private set
    var stepBlinkOrder: List<Boolean> = emptyList()
        private set

    var timedStepStartedMs: Long = 0L
        private set

    var preparationEnteredMs: Long = 0L
        private set

    var lastSavedWithoutFile: File? = null
        private set
    var lastSavedWithFile: File? = null
        private set
    var lastSavedCombinedReportFile: File? = null
        private set
    var withoutSummary: EyeTestSessionSummary? = null
        private set
    var withSummary: EyeTestSessionSummary? = null
        private set

    var withoutL1R1Success: Boolean = false
        private set
    var withoutL2R2Success: Boolean = false
        private set
    var withL1R1Success: Boolean = false
        private set
    var withL2R2Success: Boolean = false
        private set

    var withoutL1R1Outcome: EyeTestSequenceOutcome = EyeTestSequenceOutcome.NotCompleted
        private set
    var withoutL2R2Outcome: EyeTestSequenceOutcome = EyeTestSequenceOutcome.NotCompleted
        private set
    var withL1R1Outcome: EyeTestSequenceOutcome = EyeTestSequenceOutcome.NotCompleted
        private set
    var withL2R2Outcome: EyeTestSequenceOutcome = EyeTestSequenceOutcome.NotCompleted
        private set

    var phaseLeftWinksObserved: Int = 0
        private set
    var phaseRightWinksObserved: Int = 0
        private set

    var phaseEndedEarly: Boolean = false
        private set

    var statusMessage: String = ""
        private set

    var copyConfirmation: String = ""
        private set

    /** Last reportGenerated timestamp used by [fullResultsText] (for tests / copy UX). */
    var lastCopiedReportGeneratedMs: Long = 0L
        private set

    var sessionMeta: EyeTestSessionMeta? = null
        private set

    val componentSlots: Map<EyeTestComponentId, EyeTestComponentSlot>
        get() = slots

    val canViewFullResults: Boolean
        get() = EyeTestCombinedReport.allComponentsTerminal(slots)

    val phaseStepRecords: List<EyeTestStepRecord>
        get() = stepRecords.toList()

    val singleEyeSubtest: SingleEyeThresholdSubtest = SingleEyeThresholdSubtest(clockMs)

    /**
     * Progress label; for single-eye states includes cycle progress from [singleEyeSubtest.ui].
     */
    val progressLabelCycle: String
        get() {
            val base = EyeTestFlowAuthority.progressLabel(flowState)
            if (!EyeTestFlowAuthority.isSingleEyeTest(flowState)) return base
            val ui = singleEyeSubtest.ui
            return "$base · Cycle ${ui.cycleIndex + 1} of ${ui.totalCycles}"
        }

    private val slots: MutableMap<EyeTestComponentId, EyeTestComponentSlot> = emptySlots()
    private val phaseBuffer = mutableListOf<LisaEyeDiagnostic.Sample>()
    private val stepBuffer = mutableListOf<LisaEyeDiagnostic.Sample>()
    private val stepRecords = mutableListOf<EyeTestStepRecord>()
    private val phaseDecisionTracker = EyeDecisionTraceTracker()
    private var lastRecordedMs: Long = 0L
    private var phaseStarted: Boolean = false
    private var startingLocked: Boolean = false
    private var advancingLocked: Boolean = false
    private var deviceInfo: DeviceInfo = DeviceInfo()

    fun open(): Boolean {
        if (!EyeTestModeAccess.isScreenAllowed(isDebugBuild)) return false
        isOpen = true
        restartFullTest()
        statusMessage = "Eye Test Mode ready."
        return true
    }

    fun close() {
        isOpen = false
        phaseStarted = false
        startingLocked = false
        advancingLocked = false
        deactivateSingleEyeIfNeeded()
        phaseBuffer.clear()
        stepBuffer.clear()
        stepRecords.clear()
        statusMessage = ""
        copyConfirmation = ""
    }

    fun applySessionDeviceInfo(
        versionName: String,
        versionCode: Int,
        manufacturer: String,
        model: String,
        android: String
    ) {
        deviceInfo = DeviceInfo(
            versionName = versionName,
            versionCode = versionCode,
            manufacturer = manufacturer,
            model = model,
            android = android
        )
        sessionMeta?.let { applyDeviceInfoTo(it) }
    }

    fun restartFullTest() {
        deactivateSingleEyeIfNeeded()
        flowState = EyeTestFlowState.WithoutGlassesPreparation
        phaseBuffer.clear()
        stepBuffer.clear()
        stepRecords.clear()
        lastRecordedMs = 0L
        phaseStarted = false
        startingLocked = false
        advancingLocked = false
        clearStepCounters()
        lastSavedWithoutFile = null
        lastSavedWithFile = null
        lastSavedCombinedReportFile = null
        withoutSummary = null
        withSummary = null
        withoutL1R1Success = false
        withoutL2R2Success = false
        withL1R1Success = false
        withL2R2Success = false
        withoutL1R1Outcome = EyeTestSequenceOutcome.NotCompleted
        withoutL2R2Outcome = EyeTestSequenceOutcome.NotCompleted
        withL1R1Outcome = EyeTestSequenceOutcome.NotCompleted
        withL2R2Outcome = EyeTestSequenceOutcome.NotCompleted
        phaseLeftWinksObserved = 0
        phaseRightWinksObserved = 0
        phaseEndedEarly = false
        timedStepStartedMs = 0L
        preparationEnteredMs = clockMs()
        phaseDecisionTracker.reset()
        sessionMeta = null
        slots.clear()
        slots.putAll(emptySlots())
        lastCopiedReportGeneratedMs = 0L
        copyConfirmation = ""
        singleEyeSubtest.resetIdle()
        statusMessage = "Full test reset."
    }

    fun readiness(): EyeTestFlowAuthority.Readiness =
        EyeTestFlowAuthority.readiness(
            faceDetected = live.faceDetected,
            faceWidthPercent = live.faceWidthPercent,
            leftNull = live.leftNull,
            rightNull = live.rightNull,
            frameAccepted = live.frameAccepted
        )

    fun canStartDiagnosticAnyway(nowMs: Long = clockMs()): Boolean {
        if (!isOpen || !isDebugBuild) return false
        if (EyeTestFlowAuthority.stepKind(flowState) != EyeTestStepKind.Preparation) return false
        if (readiness().ready) return false
        if (preparationEnteredMs <= 0L) return false
        return nowMs - preparationEnteredMs >= EyeTestFlowAuthority.READINESS_ANYWAY_AFTER_MS
    }

    fun startCurrentPhase(): Boolean = beginPhase(requireReady = true)

    fun startDiagnosticAnyway(): Boolean {
        if (!canStartDiagnosticAnyway()) {
            statusMessage = "Start Diagnostic Anyway is not available yet."
            return false
        }
        statusMessage =
            "Eye signals are unstable. Starting now will record the failure for diagnosis."
        return beginPhase(requireReady = false)
    }

    fun retryStep() {
        if (!isOpen) return
        val kind = EyeTestFlowAuthority.stepKind(flowState)
        if (kind == EyeTestStepKind.Preparation ||
            kind == EyeTestStepKind.Result ||
            kind == EyeTestStepKind.TestComplete ||
            kind == EyeTestStepKind.FullResults
        ) return

        if (EyeTestFlowAuthority.isSingleEyeTest(flowState)) {
            val id = EyeTestFlowAuthority.componentId(flowState) ?: return
            clearComponentSlot(id)
            startSingleEyeForCurrentState()
            statusMessage = "Single-eye test retried."
            return
        }

        phaseLeftWinksObserved = (phaseLeftWinksObserved - stepLeftWinks).coerceAtLeast(0)
        phaseRightWinksObserved = (phaseRightWinksObserved - stepRightWinks).coerceAtLeast(0)
        stepBuffer.clear()
        clearStepCounters()
        if (EyeTestFlowAuthority.maxDurationMs(flowState) != null) {
            timedStepStartedMs = clockMs()
        }
        statusMessage = "Step retried."
    }

    fun skipAndRecordFailure(): Boolean {
        if (!isOpen || advancingLocked) return false
        if (!EyeTestFlowAuthority.isFailureTolerantStep(flowState)) return false

        if (EyeTestFlowAuthority.isSingleEyeTest(flowState)) {
            advancingLocked = true
            try {
                val result = singleEyeSubtest.skipAndRecordFailure()
                storeSingleEyeResult(result)
                advanceAfterSingleEye()
                statusMessage = "Single-eye test skipped and recorded as Skipped."
                return true
            } finally {
                advancingLocked = false
            }
        }

        if (!phaseStarted) return false
        recordCurrentStep(EyeTestStepCompletion.Skipped)
        markSequenceFailedIfObserving()
        advanceAfterStepRecorded()
        statusMessage = "Step skipped and recorded as Incomplete."
        return true
    }

    /**
     * End With Glasses phase early: save collected samples, mark remaining steps / single-eye
     * slots, go to With Glasses Result. Preserves Without Glasses summary and session meta.
     */
    fun endPhaseEarly(): Boolean {
        if (!isOpen || advancingLocked) return false
        if (EyeTestFlowAuthority.phaseKind(flowState) != EyeTestSessionKind.WITH_GLASSES) {
            return false
        }
        val kind = EyeTestFlowAuthority.stepKind(flowState)
        if (kind == EyeTestStepKind.Result ||
            kind == EyeTestStepKind.TestComplete ||
            kind == EyeTestStepKind.FullResults
        ) return false

        advancingLocked = true
        try {
            phaseEndedEarly = true
            when {
                EyeTestFlowAuthority.isSingleEyeTest(flowState) -> {
                    val existing = singleEyeSubtest.resultOrNull()
                    if (existing != null && !singleEyeSubtest.ui.active) {
                        storeSingleEyeResult(existing)
                    } else {
                        storeSingleEyeResult(singleEyeSubtest.endEarlyAndRecordFailure())
                    }
                    if (kind == EyeTestStepKind.SingleEyeLeft) {
                        markSingleEyeSlot(
                            EyeTestComponentId.WithGlassesRightEye,
                            EyeTestComponentOutcome.NotCompletedDueToDetectionFailure
                        )
                    }
                    ensureMainSavedAsEndedEarlyIfNeeded()
                    enterState(EyeTestFlowState.WithGlassesResult)
                    statusMessage = "With Glasses phase ended early during single-eye test."
                }
                else -> {
                    if (phaseStarted && EyeTestFlowAuthority.guidedStepNumber(flowState) != null) {
                        recordCurrentStep(EyeTestStepCompletion.NotCompletedDueToDetectionFailure)
                        markSequenceFailedIfObserving()
                    }
                    markRemainingGuidedStepsNotCompleted()
                    commitStepBuffer()
                    saveMainComponent(
                        kind = EyeTestSessionKind.WITH_GLASSES,
                        outcome = EyeTestComponentOutcome.EndedEarly
                    )
                    markSingleEyeSlot(
                        EyeTestComponentId.WithGlassesLeftEye,
                        EyeTestComponentOutcome.NotCompletedDueToDetectionFailure
                    )
                    markSingleEyeSlot(
                        EyeTestComponentId.WithGlassesRightEye,
                        EyeTestComponentOutcome.NotCompletedDueToDetectionFailure
                    )
                    enterState(EyeTestFlowState.WithGlassesResult)
                    statusMessage = "With Glasses phase ended early. Partial results saved."
                }
            }
            return true
        } finally {
            advancingLocked = false
        }
    }

    fun repeatWithoutGlassesPhase() {
        if (!isOpen) return
        deactivateSingleEyeIfNeeded()
        clearComponentSlot(EyeTestComponentId.WithoutGlassesMain)
        clearComponentSlot(EyeTestComponentId.WithoutGlassesLeftEye)
        clearComponentSlot(EyeTestComponentId.WithoutGlassesRightEye)
        withoutSummary = null
        lastSavedWithoutFile = null
        withoutL1R1Success = false
        withoutL2R2Success = false
        withoutL1R1Outcome = EyeTestSequenceOutcome.NotCompleted
        withoutL2R2Outcome = EyeTestSequenceOutcome.NotCompleted
        phaseBuffer.clear()
        stepBuffer.clear()
        stepRecords.clear()
        phaseStarted = false
        phaseEndedEarly = false
        phaseLeftWinksObserved = 0
        phaseRightWinksObserved = 0
        clearStepCounters()
        // Keep WithGlasses slots / summaries and sessionMeta.
        enterState(EyeTestFlowState.WithoutGlassesPreparation)
        statusMessage = "Without Glasses phase reset."
    }

    fun repeatWithGlassesPhase() {
        if (!isOpen) return
        deactivateSingleEyeIfNeeded()
        clearComponentSlot(EyeTestComponentId.WithGlassesMain)
        clearComponentSlot(EyeTestComponentId.WithGlassesLeftEye)
        clearComponentSlot(EyeTestComponentId.WithGlassesRightEye)
        withSummary = null
        lastSavedWithFile = null
        withL1R1Success = false
        withL2R2Success = false
        withL1R1Outcome = EyeTestSequenceOutcome.NotCompleted
        withL2R2Outcome = EyeTestSequenceOutcome.NotCompleted
        phaseBuffer.clear()
        stepBuffer.clear()
        stepRecords.clear()
        phaseStarted = false
        phaseEndedEarly = false
        phaseLeftWinksObserved = 0
        phaseRightWinksObserved = 0
        clearStepCounters()
        enterState(EyeTestFlowState.WithGlassesPreparation)
        statusMessage = "With Glasses phase reset."
    }

    fun continueToWithGlasses() {
        if (flowState != EyeTestFlowState.WithoutGlassesResult) return
        // Keep withoutSummary / lastSavedWithoutFile / WithoutGlasses slots / sessionMeta.
        phaseBuffer.clear()
        stepBuffer.clear()
        stepRecords.clear()
        phaseStarted = false
        phaseEndedEarly = false
        withL1R1Success = false
        withL2R2Success = false
        withL1R1Outcome = EyeTestSequenceOutcome.NotCompleted
        withL2R2Outcome = EyeTestSequenceOutcome.NotCompleted
        phaseLeftWinksObserved = 0
        phaseRightWinksObserved = 0
        clearStepCounters()
        enterState(EyeTestFlowState.WithGlassesPreparation)
    }

    /** WithGlassesResult → TestComplete. Marks meta completed when all six are terminal. */
    fun completeTest(): Boolean {
        if (!isOpen || flowState != EyeTestFlowState.WithGlassesResult) return false
        if (canViewFullResults) {
            sessionMeta?.markCompleted(clockMs())
        }
        enterState(EyeTestFlowState.TestComplete)
        statusMessage = if (canViewFullResults) {
            "Test complete. Full results available."
        } else {
            "Test complete, but not all components are terminal yet."
        }
        return true
    }

    /** TestComplete → FullResults only when all six components are terminal. */
    fun viewFullResults(): Boolean {
        if (!isOpen || flowState != EyeTestFlowState.TestComplete) return false
        if (!canViewFullResults) {
            statusMessage = "Full results require all six components to finish."
            return false
        }
        enterState(EyeTestFlowState.FullResults)
        return true
    }

    @Deprecated("Use viewFullResults()", ReplaceWith("viewFullResults()"))
    fun viewComparison(): Boolean = viewFullResults()

    fun remainingTimedMs(nowMs: Long = clockMs()): Long? {
        val duration = EyeTestFlowAuthority.maxDurationMs(flowState) ?: return null
        if (timedStepStartedMs <= 0L) return duration
        return (duration - (nowMs - timedStepStartedMs)).coerceAtLeast(0L)
    }

    /** UI / clock-driven advancement for timed look/rest, wink/sequence, and single-eye. */
    fun onTimedTick(nowMs: Long = clockMs()): Boolean {
        if (!isOpen || advancingLocked) return false

        if (EyeTestFlowAuthority.isSingleEyeTest(flowState)) {
            return onSingleEyeTimedTick(nowMs)
        }

        if (!phaseStarted) return false
        val duration = EyeTestFlowAuthority.maxDurationMs(flowState) ?: return false
        if (timedStepStartedMs <= 0L) {
            timedStepStartedMs = nowMs
            return false
        }
        if (nowMs - timedStepStartedMs < duration) return false

        when (EyeTestFlowAuthority.stepKind(flowState)) {
            EyeTestStepKind.TimedLook, EyeTestStepKind.Rest -> {
                recordCurrentStep(EyeTestStepCompletion.Success)
                advanceAfterStepRecorded()
            }
            EyeTestStepKind.WinkLeft,
            EyeTestStepKind.WinkRight,
            EyeTestStepKind.ObserveL1R1,
            EyeTestStepKind.ObserveL2R2 -> {
                recordCurrentStep(EyeTestStepCompletion.TimedOut)
                markSequenceFailedIfObserving()
                advanceAfterStepRecorded()
                statusMessage = "Step timed out — recorded as Failed / Incomplete."
            }
            else -> return false
        }
        return true
    }

    /**
     * Production wink acceptance observed while Eye Test Mode is open.
     * Isolated per-step counters — does not execute gestures.
     */
    fun onWinkObserved(isLeft: Boolean) {
        if (!isOpen || !phaseStarted || advancingLocked) return
        if (EyeTestFlowAuthority.isSingleEyeTest(flowState)) return
        when (EyeTestFlowAuthority.stepKind(flowState)) {
            EyeTestStepKind.WinkLeft -> {
                if (isLeft) {
                    stepLeftWinks =
                        (stepLeftWinks + 1).coerceAtMost(EyeTestFlowAuthority.TARGET_WINKS)
                    phaseLeftWinksObserved += 1
                    if (stepLeftWinks >= EyeTestFlowAuthority.TARGET_WINKS) {
                        recordCurrentStep(EyeTestStepCompletion.Success)
                        advanceAfterStepRecorded()
                    }
                }
            }
            EyeTestStepKind.WinkRight -> {
                if (!isLeft) {
                    stepRightWinks =
                        (stepRightWinks + 1).coerceAtMost(EyeTestFlowAuthority.TARGET_WINKS)
                    phaseRightWinksObserved += 1
                    if (stepRightWinks >= EyeTestFlowAuthority.TARGET_WINKS) {
                        recordCurrentStep(EyeTestStepCompletion.Success)
                        advanceAfterStepRecorded()
                    }
                }
            }
            EyeTestStepKind.ObserveL1R1,
            EyeTestStepKind.ObserveL2R2 -> {
                stepBlinkOrder = stepBlinkOrder + isLeft
                if (isLeft) {
                    stepLeftWinks += 1
                    phaseLeftWinksObserved += 1
                } else {
                    stepRightWinks += 1
                    phaseRightWinksObserved += 1
                }
                maybeCompleteSequenceObservation()
            }
            else -> Unit
        }
    }

    /**
     * Completed production sequence counts (after idle finalize), observation only.
     */
    fun onSequenceObserved(left: Int, right: Int, blinkOrder: List<Boolean>) {
        if (!isOpen || !phaseStarted || advancingLocked) return
        if (EyeTestFlowAuthority.isSingleEyeTest(flowState)) return
        when (EyeTestFlowAuthority.stepKind(flowState)) {
            EyeTestStepKind.ObserveL1R1 -> {
                if (UniversalInteractionGestures.isConfirm(left, right, blinkOrder) ||
                    (left == 1 && right == 1 && blinkOrder.isEmpty())
                ) {
                    markSequenceSuccess(l1r1 = true)
                    recordCurrentStep(EyeTestStepCompletion.Success)
                    advanceAfterStepRecorded()
                }
            }
            EyeTestStepKind.ObserveL2R2 -> {
                if (GuidedModeNavigation.isBackSequence(left, right)) {
                    markSequenceSuccess(l2r2 = true)
                    recordCurrentStep(EyeTestStepCompletion.Success)
                    advanceAfterStepRecorded()
                }
            }
            else -> Unit
        }
    }

    fun isObservingL2R2Step(): Boolean =
        isOpen && EyeTestFlowAuthority.isObservingL2R2(flowState)

    fun allowsBlinkBackToExit(): Boolean =
        isOpen && EyeTestFlowAuthority.allowsBlinkBackToExit(flowState)

    fun onSample(sample: LisaEyeDiagnostic.Sample, responseTimeSec: Int) {
        if (!isOpen || !isDebugBuild) return
        live = LiveUi(
            faceDetected = sample.faceDetected,
            faceCount = sample.faceCount,
            faceWidthPercent = sample.faceWidthPercentOfImage,
            headYaw = sample.headEulerAngleY,
            headRoll = sample.headEulerAngleZ,
            frameAccepted = sample.frameAccepted,
            rejectionReason = sample.rejectionReason,
            leftProb = sample.leftEyeOpenProbability,
            rightProb = sample.rightEyeOpenProbability,
            leftState = sample.interpretedLeftEyeState,
            rightState = sample.interpretedRightEyeState,
            leftClosedThr = sample.leftEyeClosedThreshold,
            rightClosedThr = sample.rightEyeClosedThreshold,
            openThr = sample.openEyeThreshold,
            leftNull = sample.leftEyeOpenProbability == null,
            rightNull = sample.rightEyeOpenProbability == null,
            leftWinks = sample.leftWinkCount,
            rightWinks = sample.rightWinkCount,
            sequenceState = sample.sequenceState,
            sensitivity = sample.sensitivityLevel,
            responseTimeSec = responseTimeSec
        )
        sessionMeta?.let { meta ->
            meta.sensitivity = sample.sensitivityLevel
            meta.responseTimeSec = responseTimeSec
        }

        if (EyeTestFlowAuthority.isSingleEyeTest(flowState) && singleEyeSubtest.ui.active) {
            val eye = singleEyeSubtest.ui.eye
            val selected = if (eye == SingleEyeThresholdSubtest.EyeTarget.Left) {
                sample.leftEyeOpenProbability
            } else {
                sample.rightEyeOpenProbability
            }
            val opposite = if (eye == SingleEyeThresholdSubtest.EyeTarget.Left) {
                sample.rightEyeOpenProbability
            } else {
                sample.leftEyeOpenProbability
            }
            singleEyeSubtest.onSample(
                selectedEyeProb = selected,
                oppositeEyeProb = opposite,
                frameAccepted = sample.frameAccepted,
                rejectionReason = sample.rejectionReason,
                decisionReason = sample.frameDecisionReason
                    ?: sample.leftDecisionReason
                    ?: sample.rightDecisionReason,
                nowMs = sample.timestampMs
            )
            // Do not add single-eye samples to main phaseBuffer.
            return
        }

        if (!phaseStarted) return
        val kind = EyeTestFlowAuthority.stepKind(flowState)
        if (kind == EyeTestStepKind.Preparation ||
            kind == EyeTestStepKind.Result ||
            kind == EyeTestStepKind.TestComplete ||
            kind == EyeTestStepKind.FullResults ||
            kind == EyeTestStepKind.SingleEyeLeft ||
            kind == EyeTestStepKind.SingleEyeRight
        ) return
        val now = sample.timestampMs
        if (lastRecordedMs == 0L ||
            now - lastRecordedMs >= LisaEyeDiagnostic.DEFAULT_MIN_INTERVAL_MS
        ) {
            lastRecordedMs = now
            stepBuffer += sample
        }
    }

    /** Observational hook from MainActivity decision-trace pipeline. */
    fun onDecisionTrace(
        trace: EyeDecisionFrameTrace,
        @Suppress("UNUSED_PARAMETER") sourceTracker: EyeDecisionTraceTracker
    ) {
        if (!isOpen || !isDebugBuild || !phaseStarted) return
        if (EyeTestFlowAuthority.isSingleEyeTest(flowState)) return
        phaseDecisionTracker.observe(trace, clockMs())
    }

    fun comparison(): EyeTestComparisonReport =
        EyeTestComparisonReport.compare(
            without = withoutSummary,
            with = withSummary,
            withoutL1R1 = withoutL1R1Success,
            withoutL2R2 = withoutL2R2Success,
            withL1R1 = withL1R1Success,
            withL2R2 = withL2R2Success
        )

    fun fullResultsText(): String {
        val meta = sessionMeta ?: return ""
        val now = clockMs()
        lastCopiedReportGeneratedMs = now
        val text = EyeTestCombinedReport.build(
            meta = meta,
            slots = slots,
            reportGeneratedMs = now,
            withoutMain = withoutSummary,
            withMain = withSummary
        )
        lastSavedCombinedReportFile = store.saveCombinedReport(
            text = text,
            shortSessionId = meta.shortSessionId,
            atMs = now
        )
        return text
    }

    fun markReportCopied() {
        val sid = sessionMeta?.sessionId ?: "n/a"
        copyConfirmation = "New complete test results copied\nTest Session ID: $sid"
        statusMessage = copyConfirmation
    }

    fun phaseSampleCount(): Int = phaseBuffer.size + stepBuffer.size

    /** Test helper: force enter a state. */
    internal fun forceState(state: EyeTestFlowState, started: Boolean = true) {
        phaseStarted = started
        enterState(state)
    }

    private fun beginPhase(requireReady: Boolean): Boolean {
        if (!isOpen || !isDebugBuild || startingLocked) return false
        val kind = EyeTestFlowAuthority.stepKind(flowState)
        if (kind != EyeTestStepKind.Preparation) return false
        if (requireReady && !readiness().ready) {
            statusMessage = readiness().message
            return false
        }
        startingLocked = true
        try {
            phaseBuffer.clear()
            stepBuffer.clear()
            stepRecords.clear()
            phaseDecisionTracker.reset()
            lastRecordedMs = 0L
            phaseStarted = true
            phaseEndedEarly = false
            phaseLeftWinksObserved = 0
            phaseRightWinksObserved = 0
            when (EyeTestFlowAuthority.phaseKind(flowState)) {
                EyeTestSessionKind.WITH_GLASSES -> {
                    withL1R1Success = false
                    withL2R2Success = false
                    withL1R1Outcome = EyeTestSequenceOutcome.NotCompleted
                    withL2R2Outcome = EyeTestSequenceOutcome.NotCompleted
                    clearComponentSlot(EyeTestComponentId.WithGlassesMain)
                    clearComponentSlot(EyeTestComponentId.WithGlassesLeftEye)
                    clearComponentSlot(EyeTestComponentId.WithGlassesRightEye)
                    withSummary = null
                    lastSavedWithFile = null
                }
                EyeTestSessionKind.WITHOUT_GLASSES -> {
                    withoutL1R1Success = false
                    withoutL2R2Success = false
                    withoutL1R1Outcome = EyeTestSequenceOutcome.NotCompleted
                    withoutL2R2Outcome = EyeTestSequenceOutcome.NotCompleted
                    clearComponentSlot(EyeTestComponentId.WithoutGlassesMain)
                    clearComponentSlot(EyeTestComponentId.WithoutGlassesLeftEye)
                    clearComponentSlot(EyeTestComponentId.WithoutGlassesRightEye)
                    withoutSummary = null
                    lastSavedWithoutFile = null
                    // Session meta created only on successful Without Glasses start.
                    if (sessionMeta == null) {
                        val meta = EyeTestSessionMeta.create(nowMs = clockMs())
                        applyDeviceInfoTo(meta)
                        sessionMeta = meta
                    }
                }
                null -> Unit
            }
            clearStepCounters()
            val next = EyeTestFlowAuthority.next(flowState) ?: return false
            enterState(next)
            if (statusMessage.isBlank() || requireReady) {
                statusMessage = "Phase started."
            }
            return true
        } finally {
            startingLocked = false
        }
    }

    private fun enterState(state: EyeTestFlowState) {
        flowState = state
        clearStepCounters()
        stepBuffer.clear()
        lastRecordedMs = 0L
        timedStepStartedMs = if (EyeTestFlowAuthority.maxDurationMs(state) != null) {
            clockMs()
        } else {
            0L
        }
        if (EyeTestFlowAuthority.stepKind(state) == EyeTestStepKind.Preparation) {
            preparationEnteredMs = clockMs()
            phaseStarted = false
        }
        when (EyeTestFlowAuthority.stepKind(state)) {
            EyeTestStepKind.SingleEyeLeft -> {
                // Preferred: commit main when leaving Step 8 / entering left-eye test.
                saveMainComponentIfNeeded(EyeTestComponentOutcome.Success)
                startSingleEyeForCurrentState()
            }
            EyeTestStepKind.SingleEyeRight -> {
                startSingleEyeForCurrentState()
            }
            EyeTestStepKind.Result -> {
                // Main should already be saved; ensure phase is closed.
                phaseStarted = false
            }
            else -> Unit
        }
    }

    private fun advanceAfterStepRecorded() {
        if (advancingLocked) return
        advancingLocked = true
        try {
            commitStepBuffer()
            val next = EyeTestFlowAuthority.next(flowState) ?: return
            enterState(next)
        } finally {
            advancingLocked = false
        }
    }

    private fun onSingleEyeTimedTick(nowMs: Long): Boolean {
        if (singleEyeSubtest.ui.active) {
            singleEyeSubtest.onTick(nowMs)
        }

        val phase = singleEyeSubtest.ui.phase
        if (phase == SingleEyeThresholdSubtest.Phase.Complete ||
            phase == SingleEyeThresholdSubtest.Phase.Failed
        ) {
            val result = singleEyeSubtest.resultOrNull()
            if (result != null) {
                val id = result.componentId
                if (slots[id]?.hasTerminalOutcome != true) {
                    advancingLocked = true
                    try {
                        storeSingleEyeResult(result)
                        advanceAfterSingleEye()
                        statusMessage = if (phase == SingleEyeThresholdSubtest.Phase.Complete) {
                            "Single-eye test finished."
                        } else {
                            "Single-eye test ended (${result.outcome})."
                        }
                        return true
                    } finally {
                        advancingLocked = false
                    }
                }
            }
        }

        if (timedStepStartedMs > 0L &&
            nowMs - timedStepStartedMs >= EyeTestFlowAuthority.SINGLE_EYE_TEST_MAX_MS
        ) {
            if (slots[EyeTestFlowAuthority.componentId(flowState)]?.hasTerminalOutcome == true) {
                return false
            }
            advancingLocked = true
            try {
                val result = if (singleEyeSubtest.ui.active ||
                    singleEyeSubtest.resultOrNull() == null
                ) {
                    singleEyeSubtest.timeoutAndRecordFailure()
                } else {
                    singleEyeSubtest.resultOrNull()!!
                }
                storeSingleEyeResult(result)
                advanceAfterSingleEye()
                statusMessage = "Single-eye test timed out — failure recorded."
                return true
            } finally {
                advancingLocked = false
            }
        }
        return false
    }

    private fun advanceAfterSingleEye() {
        val next = EyeTestFlowAuthority.next(flowState) ?: return
        enterState(next)
    }

    private fun startSingleEyeForCurrentState() {
        val componentId = EyeTestFlowAuthority.componentId(flowState) ?: return
        clearComponentSlot(componentId)
        val eye = when (EyeTestFlowAuthority.stepKind(flowState)) {
            EyeTestStepKind.SingleEyeLeft -> SingleEyeThresholdSubtest.EyeTarget.Left
            EyeTestStepKind.SingleEyeRight -> SingleEyeThresholdSubtest.EyeTarget.Right
            else -> return
        }
        val closed = when (eye) {
            SingleEyeThresholdSubtest.EyeTarget.Left ->
                live.leftClosedThr.takeIf { it > 0f } ?: 0.25f
            SingleEyeThresholdSubtest.EyeTarget.Right ->
                live.rightClosedThr.takeIf { it > 0f } ?: 0.25f
        }
        val open = live.openThr.takeIf { it > 0f } ?: 0.75f
        singleEyeSubtest.start(
            eye = eye,
            componentId = componentId,
            closedThreshold = closed,
            openThreshold = open
        )
        timedStepStartedMs = clockMs()
        phaseStarted = false
    }

    private fun storeSingleEyeResult(result: SingleEyeComponentResult) {
        val slot = slots.getOrPut(result.componentId) { EyeTestComponentSlot(result.componentId) }
        slot.outcome = result.outcome
        slot.singleEyeResult = result
        slot.mainSummary = null
    }

    private fun markSingleEyeSlot(id: EyeTestComponentId, outcome: EyeTestComponentOutcome) {
        val slot = slots.getOrPut(id) { EyeTestComponentSlot(id) }
        if (slot.hasTerminalOutcome) return
        slot.outcome = outcome
        slot.singleEyeResult = null
    }

    private fun clearComponentSlot(id: EyeTestComponentId) {
        slots[id] = EyeTestComponentSlot(id)
    }

    private fun saveMainComponentIfNeeded(outcome: EyeTestComponentOutcome) {
        val kind = EyeTestFlowAuthority.phaseKind(flowState) ?: return
        val mainId = mainComponentId(kind)
        if (slots[mainId]?.hasTerminalOutcome == true) return
        commitStepBuffer()
        saveMainComponent(kind, outcome)
    }

    private fun ensureMainSavedAsEndedEarlyIfNeeded() {
        val mainId = EyeTestComponentId.WithGlassesMain
        if (slots[mainId]?.hasTerminalOutcome == true) return
        commitStepBuffer()
        saveMainComponent(
            kind = EyeTestSessionKind.WITH_GLASSES,
            outcome = EyeTestComponentOutcome.EndedEarly
        )
    }

    private fun saveMainComponent(kind: EyeTestSessionKind, outcome: EyeTestComponentOutcome) {
        phaseDecisionTracker.finishOpenUncertainPeriod(clockMs())
        val samples = phaseBuffer.toList()
        val meta = sessionMeta
        val file = store.saveSession(
            kind = kind,
            samples = samples,
            shortSessionId = meta?.shortSessionId,
            atMs = meta?.testStartedMs ?: clockMs()
        )
        val summary = EyeTestSessionSummary.fromSamples(
            kind = kind,
            samples = samples,
            l1r1Success = when (kind) {
                EyeTestSessionKind.WITHOUT_GLASSES -> withoutL1R1Success
                EyeTestSessionKind.WITH_GLASSES -> withL1R1Success
            },
            l2r2Success = when (kind) {
                EyeTestSessionKind.WITHOUT_GLASSES -> withoutL2R2Success
                EyeTestSessionKind.WITH_GLASSES -> withL2R2Success
            },
            l1r1Outcome = when (kind) {
                EyeTestSessionKind.WITHOUT_GLASSES -> withoutL1R1Outcome
                EyeTestSessionKind.WITH_GLASSES -> withL1R1Outcome
            },
            l2r2Outcome = when (kind) {
                EyeTestSessionKind.WITHOUT_GLASSES -> withoutL2R2Outcome
                EyeTestSessionKind.WITH_GLASSES -> withL2R2Outcome
            },
            leftWinkDetectionsPeak = phaseLeftWinksObserved,
            rightWinkDetectionsPeak = phaseRightWinksObserved,
            stepRecords = stepRecords.toList(),
            phaseEndedEarly = kind == EyeTestSessionKind.WITH_GLASSES &&
                (phaseEndedEarly || outcome == EyeTestComponentOutcome.EndedEarly),
            decisionTracker = phaseDecisionTracker
        )
        val mainId = mainComponentId(kind)
        val slot = slots.getOrPut(mainId) { EyeTestComponentSlot(mainId) }
        slot.outcome = outcome
        slot.mainSummary = summary
        slot.singleEyeResult = null
        when (kind) {
            EyeTestSessionKind.WITHOUT_GLASSES -> {
                lastSavedWithoutFile = file
                withoutSummary = summary
            }
            EyeTestSessionKind.WITH_GLASSES -> {
                lastSavedWithFile = file
                withSummary = summary
            }
        }
        phaseStarted = false
        statusMessage = "Saved ${kind.displayName}: ${file.name} (${samples.size} samples)"
    }

    private fun mainComponentId(kind: EyeTestSessionKind): EyeTestComponentId = when (kind) {
        EyeTestSessionKind.WITHOUT_GLASSES -> EyeTestComponentId.WithoutGlassesMain
        EyeTestSessionKind.WITH_GLASSES -> EyeTestComponentId.WithGlassesMain
    }

    private fun commitStepBuffer() {
        phaseBuffer += stepBuffer
        stepBuffer.clear()
    }

    private fun clearStepCounters() {
        stepLeftWinks = 0
        stepRightWinks = 0
        stepBlinkOrder = emptyList()
    }

    private fun deactivateSingleEyeIfNeeded() {
        if (singleEyeSubtest.ui.active) {
            singleEyeSubtest.resetIdle()
        }
    }

    private fun applyDeviceInfoTo(meta: EyeTestSessionMeta) {
        meta.appVersionName = deviceInfo.versionName
        meta.appVersionCode = deviceInfo.versionCode
        meta.deviceManufacturer = deviceInfo.manufacturer
        meta.deviceModel = deviceInfo.model
        meta.androidVersion = deviceInfo.android
    }

    private fun maybeCompleteSequenceObservation() {
        val left = stepBlinkOrder.count { it }
        val right = stepBlinkOrder.count { !it }
        when (EyeTestFlowAuthority.stepKind(flowState)) {
            EyeTestStepKind.ObserveL1R1 -> {
                if (UniversalInteractionGestures.isConfirm(left, right, stepBlinkOrder)) {
                    markSequenceSuccess(l1r1 = true)
                    recordCurrentStep(EyeTestStepCompletion.Success)
                    advanceAfterStepRecorded()
                }
            }
            EyeTestStepKind.ObserveL2R2 -> {
                if (GuidedModeNavigation.isBackSequence(left, right) &&
                    stepBlinkOrder.size >= 4
                ) {
                    markSequenceSuccess(l2r2 = true)
                    recordCurrentStep(EyeTestStepCompletion.Success)
                    advanceAfterStepRecorded()
                }
            }
            else -> Unit
        }
    }

    private fun markSequenceSuccess(l1r1: Boolean = false, l2r2: Boolean = false) {
        when (EyeTestFlowAuthority.phaseKind(flowState)) {
            EyeTestSessionKind.WITHOUT_GLASSES -> {
                if (l1r1) {
                    withoutL1R1Success = true
                    withoutL1R1Outcome = EyeTestSequenceOutcome.Success
                }
                if (l2r2) {
                    withoutL2R2Success = true
                    withoutL2R2Outcome = EyeTestSequenceOutcome.Success
                }
            }
            EyeTestSessionKind.WITH_GLASSES -> {
                if (l1r1) {
                    withL1R1Success = true
                    withL1R1Outcome = EyeTestSequenceOutcome.Success
                }
                if (l2r2) {
                    withL2R2Success = true
                    withL2R2Outcome = EyeTestSequenceOutcome.Success
                }
            }
            null -> Unit
        }
    }

    private fun markSequenceFailedIfObserving() {
        when (EyeTestFlowAuthority.stepKind(flowState)) {
            EyeTestStepKind.ObserveL1R1 -> setSequenceOutcome(
                l1r1 = true,
                outcome = EyeTestSequenceOutcome.Failed
            )
            EyeTestStepKind.ObserveL2R2 -> setSequenceOutcome(
                l2r2 = true,
                outcome = EyeTestSequenceOutcome.Failed
            )
            else -> Unit
        }
    }

    private fun setSequenceOutcome(
        l1r1: Boolean = false,
        l2r2: Boolean = false,
        outcome: EyeTestSequenceOutcome
    ) {
        when (EyeTestFlowAuthority.phaseKind(flowState)) {
            EyeTestSessionKind.WITHOUT_GLASSES -> {
                if (l1r1 && withoutL1R1Outcome != EyeTestSequenceOutcome.Success) {
                    withoutL1R1Outcome = outcome
                    withoutL1R1Success = false
                }
                if (l2r2 && withoutL2R2Outcome != EyeTestSequenceOutcome.Success) {
                    withoutL2R2Outcome = outcome
                    withoutL2R2Success = false
                }
            }
            EyeTestSessionKind.WITH_GLASSES -> {
                if (l1r1 && withL1R1Outcome != EyeTestSequenceOutcome.Success) {
                    withL1R1Outcome = outcome
                    withL1R1Success = false
                }
                if (l2r2 && withL2R2Outcome != EyeTestSequenceOutcome.Success) {
                    withL2R2Outcome = outcome
                    withL2R2Success = false
                }
            }
            null -> Unit
        }
    }

    private fun recordCurrentStep(completion: EyeTestStepCompletion) {
        val stepNum = EyeTestFlowAuthority.guidedStepNumber(flowState) ?: return
        val kind = EyeTestFlowAuthority.stepKind(flowState)
        val elapsed = if (timedStepStartedMs > 0L) {
            (clockMs() - timedStepStartedMs).coerceAtLeast(0L)
        } else {
            0L
        }
        val accepted = stepBuffer.count { it.frameAccepted }
        val rejected = stepBuffer.count { !it.frameAccepted }
        val nullL = stepBuffer.count { it.leftEyeOpenProbability == null }
        val nullR = stepBuffer.count { it.rightEyeOpenProbability == null }
        val topReject = stepBuffer.mapNotNull { it.rejectionReason }
            .groupingBy { it }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key
        val (target, detected) = when (kind) {
            EyeTestStepKind.WinkLeft ->
                "left wink ×${EyeTestFlowAuthority.TARGET_WINKS}" to
                    "left winks=$stepLeftWinks"
            EyeTestStepKind.WinkRight ->
                "right wink ×${EyeTestFlowAuthority.TARGET_WINKS}" to
                    "right winks=$stepRightWinks"
            EyeTestStepKind.ObserveL1R1 ->
                "L1 R1" to sequenceProgressLabel()
            EyeTestStepKind.ObserveL2R2 ->
                "L2 R2" to sequenceProgressLabel()
            EyeTestStepKind.TimedLook ->
                "steady look" to "elapsed=${elapsed}ms"
            EyeTestStepKind.Rest ->
                "rest" to "elapsed=${elapsed}ms"
            else -> "n/a" to "n/a"
        }
        stepRecords.removeAll { it.stepNumber == stepNum }
        stepRecords += EyeTestStepRecord(
            stepNumber = stepNum,
            title = EyeTestFlowAuthority.instructionTitle(flowState),
            completion = completion,
            targetDescription = target,
            detectedDescription = detected,
            elapsedMs = elapsed,
            acceptedSamples = accepted,
            rejectedSamples = rejected,
            nullLeftSamples = nullL,
            nullRightSamples = nullR,
            mostCommonRejectionReason = topReject
        )
    }

    private fun sequenceProgressLabel(): String {
        val order = stepBlinkOrder.joinToString("") { if (it) "L" else "R" }
        return "L$stepLeftWinks R$stepRightWinks order=$order"
    }

    private fun markRemainingGuidedStepsNotCompleted() {
        val current = EyeTestFlowAuthority.guidedStepNumber(flowState) ?: 0
        val recorded = stepRecords.map { it.stepNumber }.toSet()
        for (n in 1..EyeTestFlowAuthority.TOTAL_GUIDED_STEPS) {
            if (n < current) continue
            if (n in recorded) continue
            val title = when (n) {
                1, 8 -> "Look / steady"
                2 -> "Wink left 5 times"
                3, 5 -> "Rest"
                4 -> "Wink right 5 times"
                6 -> "Perform L1 R1"
                7 -> "Perform L2 R2"
                else -> "Step $n"
            }
            stepRecords += EyeTestStepRecord(
                stepNumber = n,
                title = title,
                completion = EyeTestStepCompletion.NotCompletedDueToDetectionFailure,
                targetDescription = title,
                detectedDescription = "not reached",
                elapsedMs = 0L,
                acceptedSamples = 0,
                rejectedSamples = 0,
                nullLeftSamples = 0,
                nullRightSamples = 0,
                mostCommonRejectionReason = null
            )
            when (n) {
                6 -> if (withL1R1Outcome != EyeTestSequenceOutcome.Success) {
                    withL1R1Outcome = EyeTestSequenceOutcome.NotCompleted
                    withL1R1Success = false
                }
                7 -> if (withL2R2Outcome != EyeTestSequenceOutcome.Success) {
                    withL2R2Outcome = EyeTestSequenceOutcome.NotCompleted
                    withL2R2Success = false
                }
            }
        }
        stepRecords.sortBy { it.stepNumber }
    }

    private companion object {
        fun emptySlots(): MutableMap<EyeTestComponentId, EyeTestComponentSlot> =
            EyeTestComponentId.entries.associateWith { EyeTestComponentSlot(it) }.toMutableMap()
    }
}
