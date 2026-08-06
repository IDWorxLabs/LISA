package com.idworx.lisa.features.personalisedeyeprofile

import com.idworx.lisa.GuidedModeNavigation
import com.idworx.lisa.features.blinkdetectionreliability.BlinkDetectionProcessor
import com.idworx.lisa.features.blinkdetectionreliability.BlinkDetectionTuning
import com.idworx.lisa.features.blinkdetectionreliability.BlinkEyeProbabilities
import com.idworx.lisa.features.brain1interactionstandard.model.UniversalInteractionGestures
import com.idworx.lisa.features.eyediagnostic.EyeTestFlowAuthority

/**
 * Debug-only Personalised Eye Profile wizard controller.
 * Observes via a candidate [BlinkDetectionProcessor] only — never calls production gesture APIs.
 */
class PersonalisedEyeProfileController(
    private val store: PersonalisedEyeProfileStore,
    private val isDebugBuild: Boolean,
    private val clockMs: () -> Long = { System.currentTimeMillis() }
) {
    enum class UiPhase {
        Hub,
        Calibrating,
        ReviewThresholds,
        Validating,
        Comparison,
        Failed,
        /** Always-finish engineering report (pass or fail). */
        Report
    }

    enum class FlowPhase {
        Idle,
        Readiness,
        OpenBaseline,
        LeftOpenBrief,
        LeftCloseHold,
        LeftReopenHold,
        RightOpenBrief,
        RightCloseHold,
        RightReopenHold,
        DeriveThresholds,
        ReadyForValidationRun1,
        SteadyOpen,
        LeftWink5,
        RestAfterLeft,
        RightWink5,
        RestAfterRight,
        ObserveL1R1,
        ObserveL2R2,
        FinalSteadyOpen,
        ValidationComplete
    }

    /**
     * UX-only step segments. Sample collection and wink scoring run only during [Recording].
     * Threshold math and validation rules are unchanged.
     */
    enum class StepSegment {
        None,
        Prepare,
        Recording,
        Complete
    }

    data class LiveUi(
        val faceDetected: Boolean = false,
        val faceWidthPercent: Float? = null,
        val frameAccepted: Boolean = false,
        val leftProb: Float? = null,
        val rightProb: Float? = null,
        val leftNull: Boolean = true,
        val rightNull: Boolean = true,
        val readinessReady: Boolean = false,
        val readinessStableMs: Long = 0L,
        val cycleIndex: Int = 0,
        val totalCycles: Int = LEFT_RIGHT_CYCLES,
        val stepLeftWinks: Int = 0,
        val stepRightWinks: Int = 0,
        val runLeftWinks: Int = 0,
        val runRightWinks: Int = 0,
        val falsePositiveWinks: Int = 0,
        val l1r1Success: Boolean = false,
        val l2r2Success: Boolean = false,
        val remainingMs: Long = 0L,
        val instructionTitle: String = "",
        val instructionBody: String = "",
        val stepSegment: StepSegment = StepSegment.None,
        /** Preparing... / Recording... / ✓ Measurement Complete */
        val recordingStatus: String = "",
        /** e.g. Left Eye Calibration */
        val calibrationGroupLabel: String = ""
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

    var uiPhase: UiPhase = UiPhase.Hub
        private set

    var flowPhase: FlowPhase = FlowPhase.Idle
        private set

    var profile: PersonalisedEyeProfile? = null
        private set

    var statusMessage: String = ""
        private set

    var live: LiveUi = LiveUi()
        private set

    var lastValidationResult: PersonalisedValidationRunResult? = null
        private set

    var comparisonText: String = ""
        private set

    var lastReport: PersonalisedEyeProfileReport? = null
        private set

    var lastReportText: String = ""
        private set

    var lastReportFilePath: String? = null
        private set

    private var deviceInfo: DeviceInfo = DeviceInfo()
    private var phaseStartedMs: Long = 0L
    private var readinessStableSinceMs: Long = 0L
    private var cycleIndex: Int = 0
    private var advancingLocked: Boolean = false
    private var activeValidationRun: Int = 0
    private var stepSegment: StepSegment = StepSegment.None
    private var segmentStartedMs: Long = 0L
    private var recordingDurationMs: Long = 0L
    private var pendingNextPhase: FlowPhase? = null

    private var sessionId: String = ""
    private var testStartedMs: Long = 0L
    private var testCompletedMs: Long = 0L
    private var lastLeftDerivation: PersonalisedThresholdDerivation.EyeDerivationResult? = null
    private var lastRightDerivation: PersonalisedThresholdDerivation.EyeDerivationResult? = null
    private var calibrationSucceeded: Boolean = false
    private val validationStageOutcomes = mutableListOf<ValidationStageReport>()

    private var calFramesSeen: Int = 0
    private var calFramesRejected: Int = 0
    private var calNullLeft: Int = 0
    private var calNullRight: Int = 0
    private var calMinFaceWidth: Float? = null
    private val calFaceWidths = mutableListOf<Float>()

    private val leftOpenSamples = mutableListOf<Float>()
    private val rightOpenSamples = mutableListOf<Float>()
    private val leftClosedSamples = mutableListOf<Float>()
    private val rightClosedSamples = mutableListOf<Float>()
    private val leftReopenSamples = mutableListOf<Float>()
    private val rightReopenSamples = mutableListOf<Float>()
    private val oppositeDuringClose = mutableListOf<Float>()
    private var openBaselineLeftMedian: Float = 0f
    private var openBaselineRightMedian: Float = 0f

    private var candidateProcessor: BlinkDetectionProcessor? = null
    private var runLeftWinks: Int = 0
    private var runRightWinks: Int = 0
    private var stepLeftWinks: Int = 0
    private var stepRightWinks: Int = 0
    private var stepBlinkOrder: List<Boolean> = emptyList()
    private var falsePositiveWinks: Int = 0
    private var unexpectedSequence: Boolean = false
    private var l1r1Success: Boolean = false
    private var l2r2Success: Boolean = false
    private var nullSamples: Int = 0
    private var totalSamples: Int = 0
    private var uncertainSamples: Int = 0

    fun open(): Boolean {
        if (!PersonalisedEyeProfileAccess.isScreenAllowed(isDebugBuild)) return false
        isOpen = true
        profile = store.load()
        uiPhase = UiPhase.Hub
        flowPhase = FlowPhase.Idle
        statusMessage = if (profile != null) {
            "Loaded prototype profile (${profile!!.status})."
        } else {
            "No prototype profile yet."
        }
        refreshLiveInstructions()
        return true
    }

    fun close() {
        isOpen = false
        resetTransientState()
        uiPhase = UiPhase.Hub
        flowPhase = FlowPhase.Idle
        statusMessage = ""
        live = LiveUi()
    }

    fun applyDeviceInfo(
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
    }

    fun deletePrototypeProfile(): Boolean {
        if (!isOpen) return false
        val ok = store.delete()
        profile = null
        lastValidationResult = null
        comparisonText = ""
        uiPhase = UiPhase.Hub
        flowPhase = FlowPhase.Idle
        statusMessage = if (ok) {
            "Prototype profile deleted. Standard Mode untouched."
        } else {
            "Delete failed."
        }
        refreshLiveInstructions()
        return ok
    }

    fun returnToHub() {
        if (!isOpen) return
        resetTransientState()
        uiPhase = UiPhase.Hub
        flowPhase = FlowPhase.Idle
        statusMessage = "Returned to profile hub."
        refreshLiveInstructions()
    }

    fun startCalibration(): Boolean {
        if (!isOpen || !isDebugBuild) return false
        clearCalibrationBuffers()
        clearSessionDiagnostics()
        cycleIndex = 0
        lastValidationResult = null
        comparisonText = ""
        lastReport = null
        lastReportText = ""
        lastReportFilePath = null
        sessionId = PersonalisedEyeProfileReportAuthority.newSessionId()
        testStartedMs = clockMs()
        testCompletedMs = 0L
        calibrationSucceeded = false
        lastLeftDerivation = null
        lastRightDerivation = null
        validationStageOutcomes.clear()
        val now = clockMs()
        profile = PersonalisedEyeProfile(
            createdAtMs = now,
            updatedAtMs = now,
            deviceManufacturer = deviceInfo.manufacturer,
            deviceModel = deviceInfo.model,
            androidVersion = deviceInfo.android,
            appVersionName = deviceInfo.versionName,
            versionCode = deviceInfo.versionCode,
            status = PersonalisedEyeProfileStatus.CalibrationInProgress
        )
        uiPhase = UiPhase.Calibrating
        enterFlow(FlowPhase.Readiness, now)
        statusMessage = "Calibration started — hold steady for readiness."
        return true
    }

    fun recalibrate(): Boolean {
        if (!isOpen || !isDebugBuild) return false
        profile?.let {
            it.validationRun1 = null
            it.validationRun2 = null
            it.falsePositiveCount = 0
            it.failureReasons = emptyList()
            it.status = PersonalisedEyeProfileStatus.Draft
            it.updatedAtMs = clockMs()
        }
        return startCalibration()
    }

    fun startValidationRun(runNumber: Int): Boolean {
        if (!isOpen || !isDebugBuild) return false
        if (runNumber != 1 && runNumber != 2) {
            statusMessage = "Validation run must be 1 or 2."
            return false
        }
        val p = profile
        if (p == null) {
            statusMessage = "No profile to validate. Calibrate first."
            return false
        }
        val allowed = when (runNumber) {
            1 -> p.status == PersonalisedEyeProfileStatus.Calibrated ||
                p.status == PersonalisedEyeProfileStatus.ReadyForValidationRun1 ||
                p.status == PersonalisedEyeProfileStatus.FailedValidation
            2 -> p.status == PersonalisedEyeProfileStatus.ValidationRun1Passed ||
                p.status == PersonalisedEyeProfileStatus.ReadyForValidationRun2
            else -> false
        }
        if (!allowed) {
            statusMessage = "Profile status ${p.status} cannot start Run $runNumber."
            return false
        }
        if (runNumber == 2 && p.status == PersonalisedEyeProfileStatus.ValidationRun1Passed) {
            p.status = PersonalisedEyeProfileStatus.ReadyForValidationRun2
        }
        if (runNumber == 1 && p.status == PersonalisedEyeProfileStatus.Calibrated) {
            p.status = PersonalisedEyeProfileStatus.ReadyForValidationRun1
        }
        activeValidationRun = runNumber
        resetValidationCounters()
        validationStageOutcomes.clear()
        if (sessionId.isBlank()) {
            sessionId = PersonalisedEyeProfileReportAuthority.newSessionId()
            testStartedMs = clockMs()
        }
        candidateProcessor = BlinkDetectionProcessor(p.toCandidateTuning())
        uiPhase = UiPhase.Validating
        enterFlow(FlowPhase.SteadyOpen, clockMs())
        statusMessage = "Validation Run $runNumber started."
        return true
    }

    fun restartEntireProfile(): Boolean = startCalibration()

    fun fullReportText(): String = lastReportText

    fun canRetryValidation(): Boolean {
        val p = profile ?: return false
        return calibrationSucceeded ||
            p.status == PersonalisedEyeProfileStatus.Calibrated ||
            p.status == PersonalisedEyeProfileStatus.ReadyForValidationRun1 ||
            p.status == PersonalisedEyeProfileStatus.ValidationRun1Passed ||
            p.status == PersonalisedEyeProfileStatus.ReadyForValidationRun2 ||
            p.status == PersonalisedEyeProfileStatus.FailedValidation
    }

    fun skipCurrentStep(): Boolean {
        if (!isOpen || advancingLocked) return false
        when (uiPhase) {
            UiPhase.Calibrating -> {
                failCalibration(listOf("Calibration step skipped by user."))
                return true
            }
            UiPhase.Validating -> {
                unexpectedSequence = true
                finishValidationRun(forcedFailure = listOf("Validation step skipped by user."))
                return true
            }
            else -> {
                statusMessage = "Nothing to skip."
                return false
            }
        }
    }

    fun showComparison(): Boolean {
        if (!isOpen) return false
        comparisonText = buildComparisonText()
        uiPhase = UiPhase.Comparison
        flowPhase = FlowPhase.Idle
        statusMessage = "Side-by-side comparison (observational)."
        refreshLiveInstructions()
        return true
    }

    fun continueFromReview(): Boolean {
        if (!isOpen || uiPhase != UiPhase.ReviewThresholds) return false
        uiPhase = UiPhase.Hub
        flowPhase = FlowPhase.ReadyForValidationRun1
        statusMessage = "Ready for Validation Run 1."
        refreshLiveInstructions()
        return true
    }

    fun onSample(
        left: Float?,
        right: Float?,
        faceDetected: Boolean,
        faceWidth: Float?,
        frameAccepted: Boolean
    ) {
        if (!isOpen || !isDebugBuild) return
        val readiness = EyeTestFlowAuthority.readiness(
            faceDetected = faceDetected,
            faceWidthPercent = faceWidth,
            leftNull = left == null,
            rightNull = right == null,
            frameAccepted = frameAccepted
        )
        val now = clockMs()
        if (flowPhase == FlowPhase.Readiness) {
            if (readiness.ready && readiness.signalStable) {
                if (readinessStableSinceMs <= 0L) readinessStableSinceMs = now
            } else {
                readinessStableSinceMs = 0L
            }
        }
        val stableMs = if (readinessStableSinceMs > 0L) {
            (now - readinessStableSinceMs).coerceAtLeast(0L)
        } else {
            0L
        }
        live = live.copy(
            faceDetected = faceDetected,
            faceWidthPercent = faceWidth,
            frameAccepted = frameAccepted,
            leftProb = left,
            rightProb = right,
            leftNull = left == null,
            rightNull = right == null,
            readinessReady = readiness.ready && readiness.signalStable &&
                stableMs >= READINESS_STABLE_MS,
            readinessStableMs = stableMs,
            cycleIndex = cycleIndex,
            remainingMs = remainingMs(now),
            stepLeftWinks = stepLeftWinks,
            stepRightWinks = stepRightWinks,
            runLeftWinks = runLeftWinks,
            runRightWinks = runRightWinks,
            falsePositiveWinks = falsePositiveWinks,
            l1r1Success = l1r1Success,
            l2r2Success = l2r2Success,
            stepSegment = stepSegment,
            recordingStatus = recordingStatusLabel()
        )

        // Collect / score only during Recording — never during Prepare or Complete.
        if (isActivelyRecording()) {
            when (uiPhase) {
                UiPhase.Calibrating -> {
                    calFramesSeen++
                    if (!frameAccepted) calFramesRejected++
                    if (left == null) calNullLeft++
                    if (right == null) calNullRight++
                    faceWidth?.let {
                        calFaceWidths += it
                        calMinFaceWidth = minOf(calMinFaceWidth ?: it, it)
                    }
                    onCalibrationSample(left, right, frameAccepted)
                }
                UiPhase.Validating -> onValidationSample(left, right, now)
                else -> Unit
            }
        }
        refreshLiveInstructions()
    }

    fun onTimedTick(nowMs: Long = clockMs()): Boolean {
        if (!isOpen || advancingLocked) return false
        live = live.copy(
            remainingMs = remainingMs(nowMs),
            stepSegment = stepSegment,
            recordingStatus = recordingStatusLabel()
        )
        return when (uiPhase) {
            UiPhase.Calibrating -> onCalibrationTick(nowMs)
            UiPhase.Validating -> onValidationTick(nowMs)
            else -> false
        }
    }

    fun remainingMs(nowMs: Long = clockMs()): Long {
        if (usesStepSegments(flowPhase) && stepSegment != StepSegment.None) {
            val duration = when (stepSegment) {
                StepSegment.Prepare -> PREPARE_MS
                StepSegment.Recording -> recordingDurationMs
                StepSegment.Complete -> MEASUREMENT_COMPLETE_MS
                StepSegment.None -> return 0L
            }
            if (segmentStartedMs <= 0L) return duration
            return (duration - (nowMs - segmentStartedMs)).coerceAtLeast(0L)
        }
        val duration = phaseDurationMs(flowPhase) ?: return 0L
        if (phaseStartedMs <= 0L) return duration
        return (duration - (nowMs - phaseStartedMs)).coerceAtLeast(0L)
    }

    fun isActivelyRecording(): Boolean = stepSegment == StepSegment.Recording

    private fun recordingStatusLabel(): String = when (stepSegment) {
        StepSegment.Prepare -> "Preparing..."
        StepSegment.Recording -> "Recording..."
        StepSegment.Complete -> "✓ Measurement Complete"
        StepSegment.None -> ""
    }

    fun nextValidationRunNumber(): Int? {
        val p = profile ?: return null
        return when (p.status) {
            PersonalisedEyeProfileStatus.Calibrated,
            PersonalisedEyeProfileStatus.ReadyForValidationRun1,
            PersonalisedEyeProfileStatus.FailedValidation -> 1
            PersonalisedEyeProfileStatus.ValidationRun1Passed,
            PersonalisedEyeProfileStatus.ReadyForValidationRun2 -> 2
            else -> null
        }
    }

    private fun onCalibrationSample(left: Float?, right: Float?, frameAccepted: Boolean) {
        if (!frameAccepted) return
        when (flowPhase) {
            FlowPhase.OpenBaseline -> {
                left?.let { leftOpenSamples += it }
                right?.let { rightOpenSamples += it }
            }
            FlowPhase.LeftOpenBrief -> {
                left?.let { leftOpenSamples += it }
                right?.let { rightOpenSamples += it }
            }
            FlowPhase.LeftCloseHold -> {
                left?.let { leftClosedSamples += it }
                right?.let { oppositeDuringClose += it }
            }
            FlowPhase.LeftReopenHold -> {
                left?.let { leftReopenSamples += it }
            }
            FlowPhase.RightOpenBrief -> {
                left?.let { leftOpenSamples += it }
                right?.let { rightOpenSamples += it }
            }
            FlowPhase.RightCloseHold -> {
                right?.let { rightClosedSamples += it }
                left?.let { oppositeDuringClose += it }
            }
            FlowPhase.RightReopenHold -> {
                right?.let { rightReopenSamples += it }
            }
            else -> Unit
        }
    }

    private fun onCalibrationTick(nowMs: Long): Boolean {
        when (flowPhase) {
            FlowPhase.Readiness -> {
                if (live.readinessReady) {
                    openBaselineLeftMedian = 0f
                    openBaselineRightMedian = 0f
                    enterFlow(FlowPhase.OpenBaseline, nowMs)
                    statusMessage = "Open-eye baseline — keep both eyes open."
                    return true
                }
                return false
            }
            FlowPhase.OpenBaseline,
            FlowPhase.LeftOpenBrief,
            FlowPhase.LeftCloseHold,
            FlowPhase.LeftReopenHold,
            FlowPhase.RightOpenBrief,
            FlowPhase.RightCloseHold,
            FlowPhase.RightReopenHold -> {
                return advanceSegmentedCalibrationTick(nowMs)
            }
            else -> return false
        }
    }

    private fun advanceSegmentedCalibrationTick(nowMs: Long): Boolean {
        if (remainingMs(nowMs) > 0L) return false
        when (stepSegment) {
            StepSegment.Prepare -> {
                beginRecordingSegment(nowMs)
                return true
            }
            StepSegment.Recording -> {
                if (!finishCalibrationRecordingChecks()) return true
                beginCompleteSegment(nowMs)
                return true
            }
            StepSegment.Complete -> {
                advanceCalibrationAfterComplete(nowMs)
                return true
            }
            StepSegment.None -> return false
        }
    }

    /**
     * End-of-recording checks only (same rules as before). Returns false if calibration failed.
     */
    private fun finishCalibrationRecordingChecks(): Boolean {
        when (flowPhase) {
            FlowPhase.LeftCloseHold -> {
                if (!oppositeEyeStable(openBaselineRightMedian)) {
                    failCalibration(listOf("Opposite eye (right) was unstable during left-eye close."))
                    return false
                }
            }
            FlowPhase.RightCloseHold -> {
                if (!oppositeEyeStable(openBaselineLeftMedian)) {
                    failCalibration(listOf("Opposite eye (left) was unstable during right-eye close."))
                    return false
                }
            }
            else -> Unit
        }
        return true
    }

    private fun advanceCalibrationAfterComplete(nowMs: Long) {
        when (flowPhase) {
            FlowPhase.OpenBaseline -> {
                openBaselineLeftMedian =
                    PersonalisedThresholdDerivation.stats(leftOpenSamples).median ?: 0f
                openBaselineRightMedian =
                    PersonalisedThresholdDerivation.stats(rightOpenSamples).median ?: 0f
                cycleIndex = 0
                oppositeDuringClose.clear()
                enterFlow(FlowPhase.LeftOpenBrief, nowMs)
                statusMessage =
                    "Left Eye Calibration — cycle 1 of $LEFT_RIGHT_CYCLES."
            }
            FlowPhase.LeftOpenBrief -> {
                oppositeDuringClose.clear()
                enterFlow(FlowPhase.LeftCloseHold, nowMs)
            }
            FlowPhase.LeftCloseHold -> {
                enterFlow(FlowPhase.LeftReopenHold, nowMs)
            }
            FlowPhase.LeftReopenHold -> {
                if (cycleIndex + 1 >= LEFT_RIGHT_CYCLES) {
                    cycleIndex = 0
                    oppositeDuringClose.clear()
                    enterFlow(FlowPhase.RightOpenBrief, nowMs)
                    statusMessage =
                        "Right Eye Calibration — cycle 1 of $LEFT_RIGHT_CYCLES."
                } else {
                    cycleIndex += 1
                    oppositeDuringClose.clear()
                    enterFlow(FlowPhase.LeftOpenBrief, nowMs)
                    statusMessage =
                        "Left Eye Calibration — cycle ${cycleIndex + 1} of $LEFT_RIGHT_CYCLES."
                }
            }
            FlowPhase.RightOpenBrief -> {
                oppositeDuringClose.clear()
                enterFlow(FlowPhase.RightCloseHold, nowMs)
            }
            FlowPhase.RightCloseHold -> {
                enterFlow(FlowPhase.RightReopenHold, nowMs)
            }
            FlowPhase.RightReopenHold -> {
                if (cycleIndex + 1 >= LEFT_RIGHT_CYCLES) {
                    deriveAndSave(nowMs)
                } else {
                    cycleIndex += 1
                    oppositeDuringClose.clear()
                    enterFlow(FlowPhase.RightOpenBrief, nowMs)
                    statusMessage =
                        "Right Eye Calibration — cycle ${cycleIndex + 1} of $LEFT_RIGHT_CYCLES."
                }
            }
            else -> Unit
        }
    }

    private fun onValidationTick(nowMs: Long): Boolean {
        when (flowPhase) {
            FlowPhase.SteadyOpen,
            FlowPhase.LeftWink5,
            FlowPhase.RestAfterLeft,
            FlowPhase.RightWink5,
            FlowPhase.RestAfterRight,
            FlowPhase.ObserveL1R1,
            FlowPhase.ObserveL2R2,
            FlowPhase.FinalSteadyOpen -> {
                return advanceSegmentedValidationTick(nowMs)
            }
            else -> return false
        }
    }

    private fun advanceSegmentedValidationTick(nowMs: Long): Boolean {
        if (remainingMs(nowMs) > 0L) return false
        when (stepSegment) {
            StepSegment.Prepare -> {
                beginRecordingSegment(nowMs)
                return true
            }
            StepSegment.Recording -> {
                beginCompleteSegment(nowMs)
                return true
            }
            StepSegment.Complete -> {
                val next = pendingNextPhase ?: nextValidationPhaseAfter(flowPhase)
                pendingNextPhase = null
                recordValidationStageForLeaving(flowPhase)
                when (next) {
                    FlowPhase.ValidationComplete -> finishValidationRun()
                    else -> {
                        applyValidationPhaseEntrySideEffects(next)
                        enterFlow(next, nowMs)
                    }
                }
                return true
            }
            StepSegment.None -> return false
        }
    }

    private fun nextValidationPhaseAfter(phase: FlowPhase): FlowPhase = when (phase) {
        FlowPhase.SteadyOpen -> FlowPhase.LeftWink5
        FlowPhase.LeftWink5 -> FlowPhase.RestAfterLeft
        FlowPhase.RestAfterLeft -> FlowPhase.RightWink5
        FlowPhase.RightWink5 -> FlowPhase.RestAfterRight
        FlowPhase.RestAfterRight -> FlowPhase.ObserveL1R1
        FlowPhase.ObserveL1R1 -> FlowPhase.ObserveL2R2
        FlowPhase.ObserveL2R2 -> FlowPhase.FinalSteadyOpen
        FlowPhase.FinalSteadyOpen -> FlowPhase.ValidationComplete
        else -> FlowPhase.ValidationComplete
    }

    private fun applyValidationPhaseEntrySideEffects(phase: FlowPhase) {
        when (phase) {
            FlowPhase.LeftWink5 -> {
                stepLeftWinks = 0
                stepRightWinks = 0
                statusMessage = "Wink LEFT eye 5 times."
            }
            FlowPhase.RightWink5 -> {
                stepLeftWinks = 0
                stepRightWinks = 0
                statusMessage = "Wink RIGHT eye 5 times."
            }
            FlowPhase.RestAfterLeft, FlowPhase.RestAfterRight -> {
                statusMessage = "Rest — eyes open."
            }
            FlowPhase.ObserveL1R1 -> {
                stepLeftWinks = 0
                stepRightWinks = 0
                stepBlinkOrder = emptyList()
                statusMessage = "Perform L1 R1 (left then right)."
            }
            FlowPhase.ObserveL2R2 -> {
                stepLeftWinks = 0
                stepRightWinks = 0
                stepBlinkOrder = emptyList()
                statusMessage = "Perform L2 R2 (back sequence)."
            }
            FlowPhase.FinalSteadyOpen -> {
                statusMessage = "Final steady open — keep both eyes open."
            }
            else -> Unit
        }
    }

    /**
     * Early completion of a recording step (e.g. 5 winks reached) — brief Complete then next.
     */
    private fun finishRecordingStepEarly(next: FlowPhase, nowMs: Long) {
        pendingNextPhase = next
        beginCompleteSegment(nowMs)
    }

    private fun deriveAndSave(nowMs: Long) {
        advancingLocked = true
        try {
            enterFlow(FlowPhase.DeriveThresholds, nowMs)
            val left = PersonalisedThresholdDerivation.deriveEye(
                PersonalisedThresholdDerivation.EyeDerivationInput(
                    openSamples = leftOpenSamples.toList(),
                    closedSamples = leftClosedSamples.toList(),
                    reopenSamples = leftReopenSamples.toList()
                )
            )
            val right = PersonalisedThresholdDerivation.deriveEye(
                PersonalisedThresholdDerivation.EyeDerivationInput(
                    openSamples = rightOpenSamples.toList(),
                    closedSamples = rightClosedSamples.toList(),
                    reopenSamples = rightReopenSamples.toList()
                )
            )
            if (!left.ok || !right.ok) {
                val reasons = buildList {
                    if (!left.ok) addAll(left.failureReasons.map { "Left: $it" })
                    if (!right.ok) addAll(right.failureReasons.map { "Right: $it" })
                }
                lastLeftDerivation = left
                lastRightDerivation = right
                calibrationSucceeded = false
                failCalibration(reasons)
                return
            }
            lastLeftDerivation = left
            lastRightDerivation = right
            calibrationSucceeded = true
            val counts = mapOf(
                "leftOpen" to leftOpenSamples.size,
                "rightOpen" to rightOpenSamples.size,
                "leftClosed" to leftClosedSamples.size,
                "rightClosed" to rightClosedSamples.size,
                "leftReopen" to leftReopenSamples.size,
                "rightReopen" to rightReopenSamples.size
            )
            val notes = "L: ${left.notes}; R: ${right.notes}"
            val existing = profile
            val saved = PersonalisedEyeProfile(
                profileId = existing?.profileId ?: java.util.UUID.randomUUID().toString(),
                createdAtMs = existing?.createdAtMs ?: nowMs,
                updatedAtMs = nowMs,
                deviceManufacturer = deviceInfo.manufacturer,
                deviceModel = deviceInfo.model,
                androidVersion = deviceInfo.android,
                appVersionName = deviceInfo.versionName,
                versionCode = deviceInfo.versionCode,
                calibrationConditionLabel = existing?.calibrationConditionLabel
                    ?: "glasses_or_user_condition",
                leftOpenBaseline = left.openBaseline,
                leftClosedBaseline = left.closedBaseline,
                leftClosedMinimum = left.closedMinimum,
                leftReopenMaximum = left.reopenMaximum,
                leftClosedThreshold = left.closedThreshold,
                leftOpenThreshold = left.openThreshold,
                leftUncertaintyLower = left.uncertaintyLower,
                leftUncertaintyUpper = left.uncertaintyUpper,
                rightOpenBaseline = right.openBaseline,
                rightClosedBaseline = right.closedBaseline,
                rightClosedMinimum = right.closedMinimum,
                rightReopenMaximum = right.reopenMaximum,
                rightClosedThreshold = right.closedThreshold,
                rightOpenThreshold = right.openThreshold,
                rightUncertaintyLower = right.uncertaintyLower,
                rightUncertaintyUpper = right.uncertaintyUpper,
                requiredConsecutiveCloseFrames = 2,
                requiredConsecutiveReopenFrames = 1,
                calibrationSampleCounts = counts,
                validationRun1 = null,
                validationRun2 = null,
                falsePositiveCount = 0,
                status = PersonalisedEyeProfileStatus.ReadyForValidationRun1,
                failureReasons = emptyList(),
                derivationNotes = notes
            )
            // Also mark Calibrated semantically via ReadyForValidationRun1 (post-derive).
            saved.status = PersonalisedEyeProfileStatus.ReadyForValidationRun1
            store.save(saved)
            profile = saved
            flowPhase = FlowPhase.ReadyForValidationRun1
            uiPhase = UiPhase.ReviewThresholds
            statusMessage = "Thresholds derived. Ready for Validation Run 1."
            refreshLiveInstructions()
        } finally {
            advancingLocked = false
        }
    }

    private fun failCalibration(reasons: List<String>) {
        val now = clockMs()
        val p = profile?.copy(updatedAtMs = now) ?: PersonalisedEyeProfile(
            createdAtMs = now,
            updatedAtMs = now,
            deviceManufacturer = deviceInfo.manufacturer,
            deviceModel = deviceInfo.model,
            androidVersion = deviceInfo.android,
            appVersionName = deviceInfo.versionName,
            versionCode = deviceInfo.versionCode
        )
        p.status = PersonalisedEyeProfileStatus.CalibrationFailed
        p.failureReasons = reasons
        p.updatedAtMs = now
        store.save(p)
        profile = p
        calibrationSucceeded = false
        // If derivation was never run (e.g. opposite-eye unstable), still compute snapshots.
        if (lastLeftDerivation == null && leftOpenSamples.isNotEmpty()) {
            lastLeftDerivation = PersonalisedThresholdDerivation.deriveEye(
                PersonalisedThresholdDerivation.EyeDerivationInput(
                    openSamples = leftOpenSamples.toList(),
                    closedSamples = leftClosedSamples.toList(),
                    reopenSamples = leftReopenSamples.toList()
                )
            )
        }
        if (lastRightDerivation == null && rightOpenSamples.isNotEmpty()) {
            lastRightDerivation = PersonalisedThresholdDerivation.deriveEye(
                PersonalisedThresholdDerivation.EyeDerivationInput(
                    openSamples = rightOpenSamples.toList(),
                    closedSamples = rightClosedSamples.toList(),
                    reopenSamples = rightReopenSamples.toList()
                )
            )
        }
        flowPhase = FlowPhase.Idle
        statusMessage = reasons.joinToString(" ")
        publishFinalReport(validationResult = null)
    }

    private fun recordValidationStageForLeaving(phase: FlowPhase) {
        when (phase) {
            FlowPhase.SteadyOpen -> validationStageOutcomes += ValidationStageReport(
                name = "Open hold",
                passed = falsePositiveWinks == 0,
                detail = "falsePositives=$falsePositiveWinks"
            )
            FlowPhase.LeftWink5 -> validationStageOutcomes += ValidationStageReport(
                name = "Left wink",
                passed = stepLeftWinks >= TARGET_WINKS || runLeftWinks >= TARGET_WINKS,
                detail = "detected=$stepLeftWinks (run=$runLeftWinks)"
            )
            FlowPhase.RightWink5 -> validationStageOutcomes += ValidationStageReport(
                name = "Right wink",
                passed = stepRightWinks >= TARGET_WINKS || runRightWinks >= TARGET_WINKS,
                detail = "detected=$stepRightWinks (run=$runRightWinks)"
            )
            FlowPhase.ObserveL1R1 -> validationStageOutcomes += ValidationStageReport(
                name = "L1 R1",
                passed = l1r1Success,
                detail = if (l1r1Success) "confirm sequence detected" else "confirm not completed"
            )
            FlowPhase.ObserveL2R2 -> validationStageOutcomes += ValidationStageReport(
                name = "L2 R2",
                passed = l2r2Success,
                detail = if (l2r2Success) "back sequence detected" else "back not completed"
            )
            FlowPhase.FinalSteadyOpen -> {
                // Attribute final-open FPs as any increase beyond prior stages is already in total.
                validationStageOutcomes += ValidationStageReport(
                    name = "Final open hold",
                    passed = falsePositiveWinks == 0,
                    detail = "falsePositivesTotal=$falsePositiveWinks"
                )
            }
            else -> Unit
        }
    }

    private fun oppositeEyeStable(openBaseline: Float): Boolean {
        if (oppositeDuringClose.isEmpty()) return false
        val threshold = maxOf(openBaseline * OPPOSITE_OPEN_FRAC, OPPOSITE_OPEN_FLOOR)
        val openCount = oppositeDuringClose.count { it >= threshold }
        return openCount.toFloat() / oppositeDuringClose.size >= OPPOSITE_STABLE_RATIO
    }

    private fun onValidationSample(left: Float?, right: Float?, nowMs: Long) {
        totalSamples++
        if (left == null || right == null) {
            nullSamples++
            return
        }
        val processor = candidateProcessor ?: return
        val tuning = processor.tuning
        if (tuning.isEyeUncertain(left) || tuning.isEyeUncertain(right)) {
            uncertainSamples++
        }
        val result = processor.processFrame(
            BlinkEyeProbabilities(left, right),
            nowMs,
            acceptedLeftCount = runLeftWinks,
            acceptedRightCount = runRightWinks
        )
        if (result.acceptLeft || result.acceptRight) {
            handleCandidateAccept(isLeft = result.acceptLeft, alsoRight = result.acceptRight)
        }
    }

    private fun handleCandidateAccept(isLeft: Boolean, alsoRight: Boolean) {
        val accepts = buildList {
            if (isLeft) add(true)
            if (alsoRight) add(false)
        }
        for (leftWink in accepts) {
            when (flowPhase) {
                FlowPhase.SteadyOpen, FlowPhase.FinalSteadyOpen -> {
                    falsePositiveWinks++
                }
                FlowPhase.LeftWink5 -> {
                    if (leftWink) {
                        stepLeftWinks++
                        runLeftWinks++
                        if (stepLeftWinks >= TARGET_WINKS) {
                            finishRecordingStepEarly(FlowPhase.RestAfterLeft, clockMs())
                        }
                    } else {
                        unexpectedSequence = true
                    }
                }
                FlowPhase.RightWink5 -> {
                    if (!leftWink) {
                        stepRightWinks++
                        runRightWinks++
                        if (stepRightWinks >= TARGET_WINKS) {
                            finishRecordingStepEarly(FlowPhase.RestAfterRight, clockMs())
                        }
                    } else {
                        unexpectedSequence = true
                    }
                }
                FlowPhase.ObserveL1R1 -> {
                    stepBlinkOrder = stepBlinkOrder + leftWink
                    if (leftWink) stepLeftWinks++ else stepRightWinks++
                    if (UniversalInteractionGestures.isConfirm(
                            stepLeftWinks,
                            stepRightWinks,
                            stepBlinkOrder
                        )
                    ) {
                        l1r1Success = true
                        finishRecordingStepEarly(FlowPhase.ObserveL2R2, clockMs())
                    }
                }
                FlowPhase.ObserveL2R2 -> {
                    stepBlinkOrder = stepBlinkOrder + leftWink
                    if (leftWink) stepLeftWinks++ else stepRightWinks++
                    if (GuidedModeNavigation.isBackSequence(stepLeftWinks, stepRightWinks)) {
                        l2r2Success = true
                        finishRecordingStepEarly(FlowPhase.FinalSteadyOpen, clockMs())
                    }
                }
                FlowPhase.RestAfterLeft, FlowPhase.RestAfterRight -> {
                    // Ignore winks during rest; count as unexpected noise.
                    unexpectedSequence = true
                }
                else -> Unit
            }
        }
        live = live.copy(
            stepLeftWinks = stepLeftWinks,
            stepRightWinks = stepRightWinks,
            runLeftWinks = runLeftWinks,
            runRightWinks = runRightWinks,
            falsePositiveWinks = falsePositiveWinks,
            l1r1Success = l1r1Success,
            l2r2Success = l2r2Success,
            stepSegment = stepSegment,
            recordingStatus = recordingStatusLabel()
        )
    }

    private fun finishValidationRun(forcedFailure: List<String> = emptyList()) {
        advancingLocked = true
        try {
            val p = profile ?: return
            val nullPct = if (totalSamples == 0) {
                0f
            } else {
                nullSamples * 100f / totalSamples
            }
            val uncertainPct = if (totalSamples == 0) {
                0f
            } else {
                uncertainSamples * 100f / totalSamples
            }
            var result = PersonalisedEyeProfileValidation.evaluateRun(
                activeValidationRun,
                PersonalisedEyeProfileValidation.LiveCounters(
                    leftWinks = runLeftWinks,
                    rightWinks = runRightWinks,
                    l1r1Success = l1r1Success,
                    l2r2Success = l2r2Success,
                    falsePositiveWinks = falsePositiveWinks,
                    unexpectedSequence = unexpectedSequence,
                    nullProbabilityPercent = nullPct,
                    uncertainOccupancyPercent = uncertainPct
                )
            )
            if (forcedFailure.isNotEmpty()) {
                result = result.copy(
                    passed = false,
                    failureReasons = result.failureReasons + forcedFailure
                )
            }
            val updated = PersonalisedEyeProfileValidation.applyRunToProfile(
                profile = p,
                result = result,
                nowMs = clockMs()
            )
            // Guard: Run 1 alone must never yield Validated.
            if (activeValidationRun == 1 &&
                updated.status == PersonalisedEyeProfileStatus.Validated
            ) {
                updated.status = PersonalisedEyeProfileStatus.ValidationRun1Passed
            }
            if (activeValidationRun == 1 && result.passed) {
                updated.status = PersonalisedEyeProfileStatus.ValidationRun1Passed
            }
            store.save(updated)
            profile = updated
            lastValidationResult = result
            comparisonText = buildComparisonText()
            flowPhase = FlowPhase.ValidationComplete
            statusMessage = if (result.passed) {
                "Validation Run $activeValidationRun passed."
            } else {
                result.failureReasons.joinToString(" ")
            }
            candidateProcessor = null
            publishFinalReport(validationResult = result)
        } finally {
            advancingLocked = false
        }
    }

    private fun publishFinalReport(validationResult: PersonalisedValidationRunResult?) {
        val completed = clockMs()
        testCompletedMs = completed
        // Ensure Report Generated is always a distinct later timestamp.
        val generated = completed + 1L
        if (sessionId.isBlank()) {
            sessionId = PersonalisedEyeProfileReportAuthority.newSessionId()
        }
        if (testStartedMs <= 0L) testStartedMs = completed
        val left = PersonalisedEyeProfileReportAuthority.eyeSectionFromDerivation(
            eyeLabel = "Left eye",
            openSamples = leftOpenSamples.toList(),
            closedSamples = leftClosedSamples.toList(),
            derivation = lastLeftDerivation
        )
        val right = PersonalisedEyeProfileReportAuthority.eyeSectionFromDerivation(
            eyeLabel = "Right eye",
            openSamples = rightOpenSamples.toList(),
            closedSamples = rightClosedSamples.toList(),
            derivation = lastRightDerivation
        )
        val diagnostics = buildCalibrationDiagnosticsMap()
        val failureSummary = buildList {
            if (!calibrationSucceeded) {
                addAll(profile?.failureReasons.orEmpty())
                left.failureReasons.forEach { add("Left eye — $it") }
                right.failureReasons.forEach { add("Right eye — $it") }
                left.closedMisclassificationPercent?.let {
                    if (it > PersonalisedThresholdDerivation.MAX_MISCLASSIFY * 100f) {
                        add(
                            "Left eye — ${"%.0f".format(it)}% of closed samples remained " +
                                "above the derived closed threshold."
                        )
                    }
                }
                right.closedMisclassificationPercent?.let {
                    if (it > PersonalisedThresholdDerivation.MAX_MISCLASSIFY * 100f) {
                        add(
                            "Right eye — ${"%.0f".format(it)}% of closed samples remained " +
                                "above the derived closed threshold."
                        )
                    }
                }
            }
            validationResult?.failureReasons?.forEach { add(it) }
            validationStageOutcomes.filter { !it.passed }.forEach {
                add("${it.name} FAILED${if (it.detail.isNotBlank()) " (${it.detail})" else ""}")
            }
        }.distinct()
        val recs = PersonalisedEyeProfileReportAuthority.recommendations(
            left = left,
            right = right,
            diagnostics = diagnostics,
            validation = validationResult,
            calibrationPassed = calibrationSucceeded
        )
        val cause = PersonalisedEyeProfileReportAuthority.potentialCause(
            left = left,
            right = right,
            calibrationPassed = calibrationSucceeded,
            validation = validationResult
        )
        val report = PersonalisedEyeProfileReport(
            sessionId = sessionId,
            testStartedMs = testStartedMs,
            testCompletedMs = testCompletedMs,
            reportGeneratedMs = generated,
            deviceManufacturer = deviceInfo.manufacturer,
            deviceModel = deviceInfo.model,
            androidVersion = deviceInfo.android,
            appVersionName = deviceInfo.versionName,
            versionCode = deviceInfo.versionCode,
            isDebugBuild = isDebugBuild,
            profileId = profile?.profileId,
            profileStatus = profile?.status,
            calibrationPassed = calibrationSucceeded,
            leftEye = left,
            rightEye = right,
            validationStages = validationStageOutcomes.toList(),
            validationRunNumber = validationResult?.runNumber ?: activeValidationRun.takeIf { it > 0 },
            validationPassed = validationResult?.passed,
            failureSummary = failureSummary,
            overallConfidence = PersonalisedEyeProfileReportAuthority.overallConfidence(
                calibrationPassed = calibrationSucceeded,
                validationPassed = validationResult?.passed,
                profileStatus = profile?.status
            ),
            potentialCause = cause,
            recommendations = recs,
            comparisonSnippet = comparisonText,
            calibrationDiagnostics = diagnostics
        )
        val text = PersonalisedEyeProfileReportAuthority.formatFullText(report)
        lastReport = report
        lastReportText = text
        lastReportFilePath = try {
            store.saveReport(text, sessionId, generated).absolutePath
        } catch (_: Exception) {
            null
        }
        uiPhase = UiPhase.Report
        refreshLiveInstructions()
    }

    private fun buildCalibrationDiagnosticsMap(): Map<String, String> {
        val openL = PersonalisedThresholdDerivation.stats(leftOpenSamples)
        val openR = PersonalisedThresholdDerivation.stats(rightOpenSamples)
        val rejectedPct = if (calFramesSeen == 0) {
            0f
        } else {
            calFramesRejected * 100f / calFramesSeen
        }
        val nullPct = if (calFramesSeen == 0) {
            0f
        } else {
            (calNullLeft + calNullRight) * 50f / calFramesSeen
        }
        return buildMap {
            put("framesSeen", calFramesSeen.toString())
            put("framesRejected", calFramesRejected.toString())
            put("rejectedFramePercent", "%.2f".format(rejectedPct))
            put("nullProbabilityPercent", "%.2f".format(nullPct))
            calMinFaceWidth?.let { put("minFaceWidthPercent", "%.2f".format(it)) }
            openL.stdDev?.let { put("leftOpenStdDev", "%.4f".format(it)) }
            openR.stdDev?.let { put("rightOpenStdDev", "%.4f".format(it)) }
            if (openL.min != null && openL.max != null) {
                put("openRangeLeft", "%.4f".format(openL.max - openL.min))
            }
            if (openR.min != null && openR.max != null) {
                put("openRangeRight", "%.4f".format(openR.max - openR.min))
            }
        }
    }

    private fun clearSessionDiagnostics() {
        calFramesSeen = 0
        calFramesRejected = 0
        calNullLeft = 0
        calNullRight = 0
        calMinFaceWidth = null
        calFaceWidths.clear()
    }

    private fun buildComparisonText(): String {
        val p = profile
        val standard = BlinkDetectionTuning.default
        return buildString {
            appendLine("=== Observational comparison (Standard Mode unchanged) ===")
            appendLine()
            appendLine("Standard Profile")
            appendLine(
                "  closed=${"%.3f".format(standard.closedEyeThreshold)} " +
                    "open=${"%.3f".format(standard.openEyeThreshold)}"
            )
            appendLine("  (Production detector not run in this prototype.)")
            appendLine()
            appendLine("Personalised Candidate")
            if (p == null) {
                appendLine("  No profile.")
            } else {
                appendLine(
                    "  L closed/open=${"%.3f".format(p.leftClosedThreshold)}/" +
                        "${"%.3f".format(p.leftOpenThreshold)}"
                )
                appendLine(
                    "  R closed/open=${"%.3f".format(p.rightClosedThreshold)}/" +
                        "${"%.3f".format(p.rightOpenThreshold)}"
                )
                appendLine("  status=${p.status}")
                p.validationRun1?.let { appendRun("Run 1", it) }
                p.validationRun2?.let { appendRun("Run 2", it) }
                if (p.failureReasons.isNotEmpty()) {
                    appendLine("  failures: ${p.failureReasons.joinToString("; ")}")
                }
            }
        }
    }

    private fun StringBuilder.appendRun(label: String, r: PersonalisedValidationRunResult) {
        appendLine(
            "  $label: passed=${r.passed} L${r.leftWinksDetected} R${r.rightWinksDetected} " +
                "L1R1=${r.l1r1Success} L2R2=${r.l2r2Success} FP=${r.falsePositiveWinks} " +
                "unc=${"%.1f".format(r.uncertainOccupancyPercent)}%"
        )
        if (r.failureReasons.isNotEmpty()) {
            appendLine("    reasons: ${r.failureReasons.joinToString("; ")}")
        }
    }

    private fun enterFlow(phase: FlowPhase, nowMs: Long) {
        flowPhase = phase
        phaseStartedMs = nowMs
        pendingNextPhase = null
        when (phase) {
            FlowPhase.ObserveL1R1, FlowPhase.ObserveL2R2,
            FlowPhase.LeftWink5, FlowPhase.RightWink5 -> {
                stepLeftWinks = 0
                stepRightWinks = 0
                stepBlinkOrder = emptyList()
            }
            else -> Unit
        }
        if (usesStepSegments(phase)) {
            stepSegment = StepSegment.Prepare
            segmentStartedMs = nowMs
            recordingDurationMs = phaseDurationMs(phase) ?: 0L
        } else {
            stepSegment = StepSegment.None
            segmentStartedMs = 0L
            recordingDurationMs = 0L
        }
        live = live.copy(
            cycleIndex = cycleIndex,
            remainingMs = remainingMs(nowMs),
            stepLeftWinks = stepLeftWinks,
            stepRightWinks = stepRightWinks,
            stepSegment = stepSegment,
            recordingStatus = recordingStatusLabel(),
            calibrationGroupLabel = calibrationGroupLabelFor(phase)
        )
        refreshLiveInstructions()
    }

    private fun usesStepSegments(phase: FlowPhase): Boolean = when (phase) {
        FlowPhase.OpenBaseline,
        FlowPhase.LeftOpenBrief,
        FlowPhase.LeftCloseHold,
        FlowPhase.LeftReopenHold,
        FlowPhase.RightOpenBrief,
        FlowPhase.RightCloseHold,
        FlowPhase.RightReopenHold,
        FlowPhase.SteadyOpen,
        FlowPhase.LeftWink5,
        FlowPhase.RestAfterLeft,
        FlowPhase.RightWink5,
        FlowPhase.RestAfterRight,
        FlowPhase.ObserveL1R1,
        FlowPhase.ObserveL2R2,
        FlowPhase.FinalSteadyOpen -> true
        else -> false
    }

    private fun beginRecordingSegment(nowMs: Long) {
        stepSegment = StepSegment.Recording
        segmentStartedMs = nowMs
        live = live.copy(
            remainingMs = remainingMs(nowMs),
            stepSegment = stepSegment,
            recordingStatus = recordingStatusLabel()
        )
        refreshLiveInstructions()
    }

    private fun beginCompleteSegment(nowMs: Long) {
        stepSegment = StepSegment.Complete
        segmentStartedMs = nowMs
        live = live.copy(
            remainingMs = remainingMs(nowMs),
            stepSegment = stepSegment,
            recordingStatus = recordingStatusLabel()
        )
        refreshLiveInstructions()
    }

    private fun phaseDurationMs(phase: FlowPhase): Long? = when (phase) {
        FlowPhase.Readiness -> null
        FlowPhase.OpenBaseline -> OPEN_BASELINE_MS
        FlowPhase.LeftOpenBrief, FlowPhase.RightOpenBrief -> OPEN_BRIEF_MS
        FlowPhase.LeftCloseHold, FlowPhase.RightCloseHold -> CLOSE_HOLD_MS
        FlowPhase.LeftReopenHold, FlowPhase.RightReopenHold -> REOPEN_HOLD_MS
        FlowPhase.SteadyOpen, FlowPhase.FinalSteadyOpen -> STEADY_OPEN_MS
        FlowPhase.RestAfterLeft, FlowPhase.RestAfterRight -> REST_MS
        FlowPhase.LeftWink5, FlowPhase.RightWink5 -> WINK_STEP_MS
        FlowPhase.ObserveL1R1, FlowPhase.ObserveL2R2 -> SEQUENCE_MS
        else -> null
    }

    private fun refreshLiveInstructions() {
        val (title, body) = instructionsFor(flowPhase, uiPhase)
        live = live.copy(
            instructionTitle = title,
            instructionBody = body,
            cycleIndex = cycleIndex,
            totalCycles = LEFT_RIGHT_CYCLES,
            stepSegment = stepSegment,
            recordingStatus = recordingStatusLabel(),
            calibrationGroupLabel = calibrationGroupLabelFor(flowPhase),
            remainingMs = remainingMs()
        )
    }

    private fun calibrationGroupLabelFor(flow: FlowPhase): String = when (flow) {
        FlowPhase.OpenBaseline -> "Open Eyes Baseline"
        FlowPhase.LeftOpenBrief, FlowPhase.LeftCloseHold, FlowPhase.LeftReopenHold ->
            "Left Eye Calibration"
        FlowPhase.RightOpenBrief, FlowPhase.RightCloseHold, FlowPhase.RightReopenHold ->
            "Right Eye Calibration"
        FlowPhase.SteadyOpen, FlowPhase.FinalSteadyOpen -> "Validation — Steady Open"
        FlowPhase.LeftWink5 -> "Validation — Left Winks"
        FlowPhase.RightWink5 -> "Validation — Right Winks"
        FlowPhase.RestAfterLeft, FlowPhase.RestAfterRight -> "Validation — Rest"
        FlowPhase.ObserveL1R1 -> "Validation — L1 R1"
        FlowPhase.ObserveL2R2 -> "Validation — L2 R2"
        else -> ""
    }

    private fun instructionsFor(flow: FlowPhase, ui: UiPhase): Pair<String, String> = when {
        ui == UiPhase.Hub -> PersonalisedEyeProfileAccess.ENTRY_TITLE to
            PersonalisedEyeProfileAccess.ENTRY_SUPPORTING
        ui == UiPhase.ReviewThresholds -> "Review thresholds" to
            "Derived per-eye closed/open thresholds. Standard Mode is unchanged."
        ui == UiPhase.Report -> "PERSONALISED EYE PROFILE REPORT" to
            "Engineering diagnostic report (pass or fail). Standard Mode unchanged."
        ui == UiPhase.Comparison -> "Comparison" to
            "Observational Standard vs Personalised results."
        ui == UiPhase.Failed -> "Failed" to
            (profile?.failureReasons?.joinToString("\n") ?: statusMessage)
        else -> when (flow) {
            FlowPhase.Readiness -> "Readiness" to
                "Centre your face. Hold steady for 3 seconds when ready."
            FlowPhase.OpenBaseline -> "BOTH EYES — OPEN" to
                prepareOrMeasureBody(
                    prepare = "Keep both eyes open and look straight ahead.\n\nPreparing...",
                    measure = "Hold both eyes open.\n\nLook straight ahead."
                )
            FlowPhase.LeftOpenBrief -> "LEFT EYE — OPEN" to
                prepareOrMeasureBody(
                    prepare = "Keep both eyes open.\n\nPreparing...",
                    measure = "Hold both eyes open."
                )
            FlowPhase.LeftCloseHold -> "LEFT EYE — CLOSE" to
                prepareOrMeasureBody(
                    prepare = "Close ONLY your LEFT eye.\nKeep your RIGHT eye open.\n\nPreparing...",
                    measure = "Hold your LEFT eye closed.\nKeep your RIGHT eye open."
                )
            FlowPhase.LeftReopenHold -> "LEFT EYE — OPEN" to
                prepareOrMeasureBody(
                    prepare = "Open both eyes.\n\nPreparing...",
                    measure = "Hold both eyes open."
                )
            FlowPhase.RightOpenBrief -> "RIGHT EYE — OPEN" to
                prepareOrMeasureBody(
                    prepare = "Keep both eyes open.\n\nPreparing...",
                    measure = "Hold both eyes open."
                )
            FlowPhase.RightCloseHold -> "RIGHT EYE — CLOSE" to
                prepareOrMeasureBody(
                    prepare = "Close ONLY your RIGHT eye.\nKeep your LEFT eye open.\n\nPreparing...",
                    measure = "Hold your RIGHT eye closed.\nKeep your LEFT eye open."
                )
            FlowPhase.RightReopenHold -> "RIGHT EYE — OPEN" to
                prepareOrMeasureBody(
                    prepare = "Open both eyes.\n\nPreparing...",
                    measure = "Hold both eyes open."
                )
            FlowPhase.DeriveThresholds -> "Deriving thresholds" to
                "Computing per-eye thresholds from measured samples…"
            FlowPhase.SteadyOpen -> "BOTH EYES — OPEN" to
                prepareOrMeasureBody(
                    prepare = "Keep both eyes open.\nAny wink will count as a false positive.\n\nPreparing...",
                    measure = "Keep both eyes open.\nAny wink counts as a false positive."
                )
            FlowPhase.LeftWink5 -> "LEFT EYE — CLOSE" to
                prepareOrMeasureBody(
                    prepare = "Get ready to wink your LEFT eye 5 times at a normal speed.\n\nPreparing...",
                    measure = "Wink your LEFT eye 5 times at a normal speed."
                )
            FlowPhase.RestAfterLeft, FlowPhase.RestAfterRight -> "BOTH EYES — OPEN" to
                prepareOrMeasureBody(
                    prepare = "Keep both eyes open and rest.\n\nPreparing...",
                    measure = "Keep both eyes open and rest."
                )
            FlowPhase.RightWink5 -> "RIGHT EYE — CLOSE" to
                prepareOrMeasureBody(
                    prepare = "Get ready to wink your RIGHT eye 5 times at a normal speed.\n\nPreparing...",
                    measure = "Wink your RIGHT eye 5 times at a normal speed."
                )
            FlowPhase.ObserveL1R1 -> "L1 R1" to
                prepareOrMeasureBody(
                    prepare = "Get ready for confirm: left wink, then right wink.\n\nPreparing...",
                    measure = "Perform confirm: left wink, then right wink."
                )
            FlowPhase.ObserveL2R2 -> "L2 R2" to
                prepareOrMeasureBody(
                    prepare = "Get ready for back: two left, then two right.\n\nPreparing...",
                    measure = "Perform back sequence: two left, then two right."
                )
            FlowPhase.FinalSteadyOpen -> "BOTH EYES — OPEN" to
                prepareOrMeasureBody(
                    prepare = "Keep both eyes open until recording ends.\n\nPreparing...",
                    measure = "Keep both eyes open until the timer ends."
                )
            FlowPhase.ReadyForValidationRun1 -> "Ready" to
                "Calibration complete. Start Validation Run 1 when ready."
            FlowPhase.ValidationComplete -> "Validation complete" to
                statusMessage
            else -> "Personalised Eye Profile" to statusMessage
        }
    }

    private fun prepareOrMeasureBody(prepare: String, measure: String): String = when (stepSegment) {
        StepSegment.Prepare -> prepare
        StepSegment.Complete -> "✓ Measurement Complete"
        else -> measure
    }

    private fun resetValidationCounters() {
        runLeftWinks = 0
        runRightWinks = 0
        stepLeftWinks = 0
        stepRightWinks = 0
        stepBlinkOrder = emptyList()
        falsePositiveWinks = 0
        unexpectedSequence = false
        l1r1Success = false
        l2r2Success = false
        nullSamples = 0
        totalSamples = 0
        uncertainSamples = 0
    }

    private fun clearCalibrationBuffers() {
        leftOpenSamples.clear()
        rightOpenSamples.clear()
        leftClosedSamples.clear()
        rightClosedSamples.clear()
        leftReopenSamples.clear()
        rightReopenSamples.clear()
        oppositeDuringClose.clear()
        openBaselineLeftMedian = 0f
        openBaselineRightMedian = 0f
        cycleIndex = 0
        readinessStableSinceMs = 0L
    }

    private fun resetTransientState() {
        clearCalibrationBuffers()
        resetValidationCounters()
        clearSessionDiagnostics()
        candidateProcessor = null
        activeValidationRun = 0
        phaseStartedMs = 0L
        advancingLocked = false
        stepSegment = StepSegment.None
        segmentStartedMs = 0L
        recordingDurationMs = 0L
        pendingNextPhase = null
        validationStageOutcomes.clear()
    }

    companion object {
        const val READINESS_STABLE_MS: Long = 3_000L
        const val PREPARE_MS: Long = 4_000L
        const val MEASUREMENT_COMPLETE_MS: Long = 1_000L
        const val OPEN_BASELINE_MS: Long = 10_000L
        const val OPEN_BRIEF_MS: Long = 2_000L
        const val CLOSE_HOLD_MS: Long = 2_000L
        const val REOPEN_HOLD_MS: Long = 2_000L
        const val STEADY_OPEN_MS: Long = 10_000L
        const val REST_MS: Long = 5_000L
        const val WINK_STEP_MS: Long = 60_000L
        const val SEQUENCE_MS: Long = 45_000L
        const val LEFT_RIGHT_CYCLES: Int = 5
        const val TARGET_WINKS: Int = 5
        const val OPPOSITE_STABLE_RATIO: Float = 0.7f
        const val OPPOSITE_OPEN_FRAC: Float = 0.85f
        const val OPPOSITE_OPEN_FLOOR: Float = 0.6f
    }
}
