package com.idworx.lisa.features.signalinvestigation

/**
 * Debug-only Signal Investigation.
 *
 * Standard mode: calm natural-position assessment (no patient movement).
 * Advanced Engineering mode: optional pose/distance tests (caregiver / engineer).
 * Observational only — does not alter Standard Mode or production thresholds.
 */
class SignalInvestigationController(
    private val store: SignalInvestigationStore,
    private val isDebugBuild: Boolean,
    private val clockMs: () -> Long = { System.currentTimeMillis() },
    private val speak: (String) -> Unit = {},
    private val playCue: (AudioCue) -> Unit = {},
    private val ttsAvailableProvider: () -> Boolean = { true }
) {
    enum class UiPhase { Hub, Conditions, Running, Report }

    enum class FlowPhase {
        // Shared / advanced
        Guiding,
        Stabilizing,
        Prepare,
        Recording,
        Complete,
        EyeRecovery,
        PositionResult,
        // Standard still assessment
        StandardPrompt,
        StandardWaitEye,
        StandardObserve,
        StandardRecovery
    }

    enum class CaptureKind { Open, Closed }

    enum class AudioCue { RecordStart, RecordEnd }

    data class LiveUi(
        val instructionTitle: String = "",
        val instructionBody: String = "",
        val correction: String = "",
        val targetBand: SignalPoseGuidance.Band = SignalPoseGuidance.Band.Unknown,
        val recordingStatus: String = "",
        val remainingMs: Long = 0L,
        val flowPhase: FlowPhase = FlowPhase.Guiding,
        val captureKind: CaptureKind = CaptureKind.Open,
        val positionLabel: String = "",
        val positionIndex: Int = 0,
        val positionTotal: Int = 0,
        val investigationMode: SignalInvestigationMode = SignalInvestigationMode.Standard,
        val standardStep: StandardInvestigationStep? = null,
        val leftProb: Float? = null,
        val rightProb: Float? = null,
        val faceWidthPercent: Float? = null,
        val faceDetected: Boolean = false,
        val frameAccepted: Boolean = false,
        val technicalYaw: Float? = null,
        val technicalRoll: Float? = null,
        val technicalCenterY: Float? = null,
        val technicalAcceptedFrames: Int = 0,
        val technicalRejectedFrames: Int = 0,
        val technicalPoseMismatchRejects: Int = 0,
        val showTechnical: Boolean = false,
        val ttsWarning: Boolean = false,
        val environmentHint: String = ""
    )

    private data class DeviceInfo(
        val versionName: String = "",
        val versionCode: Int = 0,
        val manufacturer: String = "",
        val model: String = "",
        val android: String = ""
    )

    private data class PositionBuffers(
        val openLeft: MutableList<Float> = mutableListOf(),
        val openRight: MutableList<Float> = mutableListOf(),
        val closedLeft: MutableList<Float> = mutableListOf(),
        val closedRight: MutableList<Float> = mutableListOf(),
        val faceWidths: MutableList<Float> = mutableListOf(),
        val yaws: MutableList<Float> = mutableListOf(),
        val rolls: MutableList<Float> = mutableListOf(),
        val faceCenterYs: MutableList<Float> = mutableListOf(),
        var framesSeen: Int = 0,
        var framesRejected: Int = 0,
        var nullCount: Int = 0,
        var poseMismatchRejects: Int = 0,
        var inTargetFrames: Int = 0,
        var recordingFrames: Int = 0,
        var timeToReachMs: Long = 0L,
        var voiceEvents: Int = 0,
        var repeated: Boolean = false,
        var targetDescription: String = "",
        var targetRangeLabel: String = ""
    )

    private data class StandardBuffers(
        val openLeft: MutableList<Float> = mutableListOf(),
        val openRight: MutableList<Float> = mutableListOf(),
        val leftClosedLeft: MutableList<Float> = mutableListOf(),
        val leftClosedRight: MutableList<Float> = mutableListOf(),
        val rightClosedLeft: MutableList<Float> = mutableListOf(),
        val rightClosedRight: MutableList<Float> = mutableListOf(),
        val faceWidths: MutableList<Float> = mutableListOf(),
        var framesSeen: Int = 0,
        var nullCount: Int = 0
    )

    var isOpen: Boolean = false
        private set
    var uiPhase: UiPhase = UiPhase.Hub
        private set
    var live: LiveUi = LiveUi()
        private set
    var statusMessage: String = ""
        private set
    var glasses: GlassesCondition = GlassesCondition.YES
    var lighting: LightingCondition = LightingCondition.Indoor
    var lastReport: SignalInvestigationReport? = null
        private set
    var lastReportText: String = ""
        private set
    var lastReportFilePath: String? = null
        private set
    var showTechnicalDetails: Boolean = false
    var investigationMode: SignalInvestigationMode = SignalInvestigationMode.Standard
        private set

    private var deviceInfo = DeviceInfo()
    private var cameraResolution: String = "n/a"
    private var sessionId: String = ""
    private var testStartedMs: Long = 0L
    private var testCompletedMs: Long = 0L
    private var positionIndex: Int = 0
    private var captureKind: CaptureKind = CaptureKind.Open
    private var flowPhase: FlowPhase = FlowPhase.Guiding
    private var phaseStartedMs: Long = 0L
    private var stableSinceMs: Long = 0L
    private var positionStartedMs: Long = 0L
    private var lastSpoken: String = ""
    private var lastSpokenMs: Long = 0L
    private var lastCountdownSecond: Int = -1
    private var spokenOpenEyesForStep: Boolean = false
    private var baselinePose: SignalPoseGuidance.BaselinePose? = null
    private var lastPose: SignalPoseGuidance.LivePose? = null
    private var lastGuidance: SignalPoseGuidance.GuidanceResult? = null
    private val buffers = linkedMapOf<SignalPosition, PositionBuffers>()
    private var advancingLocked: Boolean = false
    private var totalVoiceEvents: Int = 0
    private var ttsAvailable: Boolean = true
    private var allowVisualOnly: Boolean = false
    private var eyeRecoveryOpenSinceMs: Long = 0L
    private var pendingStartMode: SignalInvestigationMode = SignalInvestigationMode.Standard

    // Standard flow state
    private var standardStep: StandardInvestigationStep = StandardInvestigationStep.NaturalPosition
    private val standardBuf = StandardBuffers()
    private val winkTracker = SignalStandardAuthority.WinkEdgeTracker()
    private val blinkTracker = SignalStandardAuthority.NaturalBlinkTracker()
    private var winkOrder = mutableListOf<String>()
    private var faceStableSinceMs: Long = 0L
    private var l1r1Outcome: String = "n/a"
    private var l2r2Outcome: String = "n/a"
    private var sequencesAttempted: Boolean = false
    private var environmentDiagnoses: List<String> = emptyList()
    private var standardStepReports = mutableListOf<StandardStepReport>()
    private var announcedStep: StandardInvestigationStep? = null

    fun open(): Boolean {
        if (!SignalInvestigationAccess.isScreenAllowed(isDebugBuild)) return false
        isOpen = true
        uiPhase = UiPhase.Hub
        live = LiveUi()
        ttsAvailable = ttsAvailableProvider()
        statusMessage = "Signal Investigation — debug only."
        refreshUi()
        return true
    }

    fun close() {
        isOpen = false
        resetRun()
        uiPhase = UiPhase.Hub
        live = LiveUi()
        statusMessage = ""
    }

    fun applyDeviceInfo(
        versionName: String,
        versionCode: Int,
        manufacturer: String,
        model: String,
        android: String
    ) {
        deviceInfo = DeviceInfo(versionName, versionCode, manufacturer, model, android)
    }

    fun confirmVisualOnlyWithoutTts(): Boolean {
        if (!isOpen) return false
        allowVisualOnly = true
        statusMessage = "Continuing without voice guidance."
        refreshUi()
        return true
    }

    fun returnToHub() {
        if (!isOpen) return
        resetRun()
        uiPhase = UiPhase.Hub
        statusMessage = "Returned to hub."
        refreshUi()
    }

    fun openConditions(mode: SignalInvestigationMode = SignalInvestigationMode.Standard): Boolean {
        if (!isOpen || !isDebugBuild) return false
        pendingStartMode = mode
        uiPhase = UiPhase.Conditions
        ttsAvailable = ttsAvailableProvider()
        statusMessage = if (!ttsAvailable && !allowVisualOnly) {
            "TTS unavailable — confirm visual-only before starting."
        } else {
            when (mode) {
                SignalInvestigationMode.Standard -> "Set conditions, then begin standard assessment."
                SignalInvestigationMode.AdvancedEngineering ->
                    "Advanced Engineering — optional pose tests. Caregiver may assist."
            }
        }
        refreshUi()
        return true
    }

    /** Default: standard still assessment. */
    fun startInvestigation(): Boolean = startWithMode(pendingStartMode)

    fun startStandardInvestigation(): Boolean {
        pendingStartMode = SignalInvestigationMode.Standard
        return startWithMode(SignalInvestigationMode.Standard)
    }

    fun startAdvancedEngineering(): Boolean {
        pendingStartMode = SignalInvestigationMode.AdvancedEngineering
        return startWithMode(SignalInvestigationMode.AdvancedEngineering)
    }

    private fun startWithMode(mode: SignalInvestigationMode): Boolean {
        if (!isOpen || !isDebugBuild) return false
        ttsAvailable = ttsAvailableProvider()
        if (!ttsAvailable && !allowVisualOnly) {
            statusMessage = "TTS unavailable. Confirm visual-only to continue."
            refreshUi()
            return false
        }
        resetRun()
        investigationMode = mode
        sessionId = SignalInvestigationReportAuthority.newSessionId()
        testStartedMs = clockMs()
        uiPhase = UiPhase.Running
        when (mode) {
            SignalInvestigationMode.Standard -> {
                standardStep = StandardInvestigationStep.NaturalPosition
                enterStandardStep(clockMs(), announce = true)
            }
            SignalInvestigationMode.AdvancedEngineering -> {
                positionIndex = 0
                captureKind = CaptureKind.Open
                SignalPosition.entries.forEach { buffers[it] = PositionBuffers() }
                enterGuiding(clockMs(), announce = true)
            }
        }
        statusMessage = "Investigation started."
        return true
    }

    fun restart(): Boolean = openConditions(investigationMode).also {
        if (it) statusMessage = "Restart — set conditions again."
    }

    fun toggleTechnicalDetails() {
        showTechnicalDetails = !showTechnicalDetails
        refreshUi()
    }

    fun onSample(
        left: Float?,
        right: Float?,
        faceDetected: Boolean,
        faceWidth: Float?,
        frameAccepted: Boolean,
        headYaw: Float? = null,
        headRoll: Float? = null,
        faceCenterXPct: Float? = null,
        faceCenterYPct: Float? = null,
        imageWidthPx: Int? = null,
        imageHeightPx: Int? = null
    ) {
        if (!isOpen || !isDebugBuild) return
        if (imageWidthPx != null && imageHeightPx != null && imageWidthPx > 0 && imageHeightPx > 0) {
            cameraResolution = "${imageWidthPx}x$imageHeightPx"
        }
        val userYaw = headYaw?.let { SignalPoseGuidance.userYaw(it) }
        val userRoll = headRoll?.let { SignalPoseGuidance.userRoll(it) }
        val pose = SignalPoseGuidance.LivePose(
            faceDetected = faceDetected,
            userYaw = userYaw,
            userRoll = userRoll,
            faceCenterXPct = faceCenterXPct,
            faceCenterYPct = faceCenterYPct,
            faceWidthPct = faceWidth,
            leftOpen = left,
            rightOpen = right
        )
        lastPose = pose
        if (uiPhase != UiPhase.Running) {
            refreshUi()
            return
        }
        val now = clockMs()
        if (investigationMode == SignalInvestigationMode.Standard) {
            onStandardSample(pose, left, right, faceDetected, faceWidth, frameAccepted, now)
            refreshUi()
            return
        }
        val pos = currentPosition() ?: return
        val target = SignalPoseGuidance.targetsFor(pos, baselinePose)
        val guidance = SignalPoseGuidance.evaluate(target, pose)
        lastGuidance = guidance
        when (flowPhase) {
            FlowPhase.Guiding -> onGuiding(guidance, now)
            FlowPhase.Stabilizing -> onStabilizing(guidance, now)
            FlowPhase.Prepare -> onPrepareSample(guidance, now)
            FlowPhase.Recording -> onRecordingSample(
                pose, guidance, left, right, faceDetected, faceWidth, frameAccepted, now
            )
            FlowPhase.EyeRecovery -> onEyeRecovery(pose, now)
            else -> Unit
        }
        refreshUi()
    }

    fun onTimedTick(nowMs: Long = clockMs()): Boolean {
        if (!isOpen || advancingLocked || uiPhase != UiPhase.Running) return false
        if (investigationMode == SignalInvestigationMode.Standard) {
            return onStandardTick(nowMs)
        }
        when (flowPhase) {
            FlowPhase.Prepare -> {
                speakCountdown(nowMs)
                if (remainingMs(nowMs) > 0L) {
                    refreshUi()
                    return false
                }
                beginRecording(nowMs)
                return true
            }
            FlowPhase.Recording -> {
                if (remainingMs(nowMs) > 0L) {
                    refreshUi()
                    return false
                }
                endRecording(nowMs)
                return true
            }
            FlowPhase.Complete -> {
                if (remainingMs(nowMs) > 0L) {
                    refreshUi()
                    return false
                }
                afterComplete(nowMs)
                return true
            }
            FlowPhase.PositionResult -> {
                if (remainingMs(nowMs) > 0L) {
                    refreshUi()
                    return false
                }
                advancePosition(nowMs)
                return true
            }
            FlowPhase.EyeRecovery -> {
                if (nowMs - phaseStartedMs > 12_000L) {
                    speakOnce("Open both eyes.", force = false)
                    phaseStartedMs = nowMs
                }
                refreshUi()
                return false
            }
            else -> {
                refreshUi()
                return false
            }
        }
    }

    fun remainingMs(nowMs: Long = clockMs()): Long {
        if (investigationMode == SignalInvestigationMode.Standard) {
            return standardRemainingMs(nowMs)
        }
        val duration = when (flowPhase) {
            FlowPhase.Prepare -> SignalPoseGuidance.PREPARE_MS
            FlowPhase.Recording -> if (captureKind == CaptureKind.Open) {
                SignalPoseGuidance.OPEN_RECORD_MS
            } else {
                SignalPoseGuidance.CLOSED_RECORD_MS
            }
            FlowPhase.Complete -> SignalPoseGuidance.COMPLETE_MS
            FlowPhase.PositionResult -> SignalPoseGuidance.POSITION_RESULT_MS
            FlowPhase.Stabilizing -> {
                if (stableSinceMs <= 0L) return SignalPoseGuidance.STABLE_REQUIRED_MS
                return (SignalPoseGuidance.STABLE_REQUIRED_MS - (nowMs - stableSinceMs))
                    .coerceAtLeast(0L)
            }
            else -> return 0L
        }
        if (phaseStartedMs <= 0L) return duration
        return (duration - (nowMs - phaseStartedMs)).coerceAtLeast(0L)
    }

    fun fullReportText(): String = lastReportText

    // -------------------------------------------------------------------------
    // Standard still assessment
    // -------------------------------------------------------------------------

    private fun enterStandardStep(nowMs: Long, announce: Boolean) {
        phaseStartedMs = nowMs
        faceStableSinceMs = 0L
        eyeRecoveryOpenSinceMs = 0L
        winkTracker.reset()
        winkOrder.clear()
        when (standardStep) {
            StandardInvestigationStep.NaturalPosition -> {
                flowPhase = FlowPhase.StandardPrompt
                if (announce) speakOnce("Please look naturally at the phone.", force = true)
            }
            StandardInvestigationStep.LeftEye -> {
                flowPhase = FlowPhase.StandardWaitEye
                if (announce) speakOnce("When ready, close only your LEFT eye.", force = true)
            }
            StandardInvestigationStep.RightEye -> {
                flowPhase = FlowPhase.StandardWaitEye
                if (announce) speakOnce("When ready, close only your RIGHT eye.", force = true)
            }
            StandardInvestigationStep.NaturalBlink -> {
                flowPhase = FlowPhase.StandardObserve
                blinkTracker.reset()
                if (announce) speakOnce("Blink naturally when you need to.", force = true)
            }
            StandardInvestigationStep.L1R1 -> {
                flowPhase = FlowPhase.StandardObserve
                sequencesAttempted = true
                if (announce) speakOnce("When ready, blink left once, then right once.", force = true)
            }
            StandardInvestigationStep.L2R2 -> {
                flowPhase = FlowPhase.StandardObserve
                if (announce) speakOnce("When ready, blink left twice, then right twice.", force = true)
            }
        }
        announcedStep = standardStep
        refreshUi()
    }

    private fun onStandardSample(
        pose: SignalPoseGuidance.LivePose,
        left: Float?,
        right: Float?,
        faceDetected: Boolean,
        faceWidth: Float?,
        frameAccepted: Boolean,
        nowMs: Long
    ) {
        standardBuf.framesSeen++
        if (left == null || right == null) standardBuf.nullCount++
        faceWidth?.let { standardBuf.faceWidths += it }

        val usable = SignalStandardAuthority.faceUsableForNatural(faceDetected, faceWidth)
        if (usable) {
            if (faceStableSinceMs <= 0L) faceStableSinceMs = nowMs
        } else {
            faceStableSinceMs = 0L
        }

        when (flowPhase) {
            FlowPhase.StandardPrompt -> {
                if (standardStep == StandardInvestigationStep.NaturalPosition &&
                    usable &&
                    nowMs - faceStableSinceMs >= SignalStandardAuthority.FACE_STABLE_MS
                ) {
                    beginStandardRecording(nowMs)
                }
            }
            FlowPhase.StandardWaitEye -> {
                val ready = when (standardStep) {
                    StandardInvestigationStep.LeftEye ->
                        SignalStandardAuthority.leftEyeClosedOnly(left, right)
                    StandardInvestigationStep.RightEye ->
                        SignalStandardAuthority.rightEyeClosedOnly(left, right)
                    else -> false
                }
                if (ready) beginStandardRecording(nowMs)
            }
            FlowPhase.Recording -> {
                if (!frameAccepted || !faceDetected) return
                when (standardStep) {
                    StandardInvestigationStep.NaturalPosition -> {
                        left?.let { standardBuf.openLeft += it }
                        right?.let { standardBuf.openRight += it }
                    }
                    StandardInvestigationStep.LeftEye -> {
                        left?.let { standardBuf.leftClosedLeft += it }
                        right?.let { standardBuf.leftClosedRight += it }
                    }
                    StandardInvestigationStep.RightEye -> {
                        left?.let { standardBuf.rightClosedLeft += it }
                        right?.let { standardBuf.rightClosedRight += it }
                    }
                    else -> Unit
                }
            }
            FlowPhase.StandardObserve -> {
                when (standardStep) {
                    StandardInvestigationStep.NaturalBlink -> blinkTracker.onSample(left, right)
                    StandardInvestigationStep.L1R1, StandardInvestigationStep.L2R2 -> {
                        winkTracker.onSample(left, right)?.let { winkOrder += it }
                        if (standardStep == StandardInvestigationStep.L1R1 &&
                            SignalStandardAuthority.isL1R1(winkOrder)
                        ) {
                            l1r1Outcome = "Success"
                            finishStandardStep(nowMs, completed = true)
                        } else if (standardStep == StandardInvestigationStep.L2R2 &&
                            SignalStandardAuthority.isL2R2(winkOrder)
                        ) {
                            l2r2Outcome = "Success"
                            finishStandardStep(nowMs, completed = true)
                        }
                    }
                    else -> Unit
                }
            }
            FlowPhase.StandardRecovery -> {
                if (SignalStandardAuthority.bothEyesOpen(left, right)) {
                    if (eyeRecoveryOpenSinceMs <= 0L) eyeRecoveryOpenSinceMs = nowMs
                    if (nowMs - eyeRecoveryOpenSinceMs >= SignalStandardAuthority.BOTH_OPEN_HOLD_MS) {
                        finishStandardStep(nowMs, completed = true)
                    }
                } else {
                    eyeRecoveryOpenSinceMs = 0L
                }
            }
            else -> Unit
        }
    }

    private fun beginStandardRecording(nowMs: Long) {
        flowPhase = FlowPhase.Recording
        phaseStartedMs = nowMs
        playCue(AudioCue.RecordStart)
        speakOnce("Recording.", force = true)
        refreshUi()
    }

    private fun onStandardTick(nowMs: Long): Boolean {
        when (flowPhase) {
            FlowPhase.Recording -> {
                if (standardRemainingMs(nowMs) > 0L) {
                    refreshUi()
                    return false
                }
                playCue(AudioCue.RecordEnd)
                speakOnce("Done.", force = true)
                when (standardStep) {
                    StandardInvestigationStep.LeftEye,
                    StandardInvestigationStep.RightEye -> {
                        flowPhase = FlowPhase.StandardRecovery
                        phaseStartedMs = nowMs
                        eyeRecoveryOpenSinceMs = 0L
                        speakOnce("Thank you. Open both eyes.", force = true)
                    }
                    else -> {
                        flowPhase = FlowPhase.Complete
                        phaseStartedMs = nowMs
                    }
                }
                refreshUi()
                return true
            }
            FlowPhase.StandardObserve -> {
                if (standardRemainingMs(nowMs) > 0L) {
                    refreshUi()
                    return false
                }
                // Timeout
                when (standardStep) {
                    StandardInvestigationStep.NaturalBlink -> {
                        speakOnce("Done.", force = true)
                        finishStandardStep(nowMs, completed = true)
                    }
                    StandardInvestigationStep.L1R1 -> {
                        if (l1r1Outcome == "n/a") l1r1Outcome = "Not completed"
                        speakOnce("Done.", force = true)
                        finishStandardStep(nowMs, completed = false)
                    }
                    StandardInvestigationStep.L2R2 -> {
                        if (l2r2Outcome == "n/a") l2r2Outcome = "Not completed"
                        speakOnce("Done.", force = true)
                        finishStandardStep(nowMs, completed = false)
                    }
                    else -> Unit
                }
                return true
            }
            FlowPhase.Complete -> {
                if (standardRemainingMs(nowMs) > 0L) {
                    refreshUi()
                    return false
                }
                finishStandardStep(nowMs, completed = true)
                return true
            }
            FlowPhase.StandardRecovery -> {
                if (nowMs - phaseStartedMs > 15_000L) {
                    speakOnce("Open both eyes.", force = false)
                    phaseStartedMs = nowMs
                }
                refreshUi()
                return false
            }
            else -> {
                refreshUi()
                return false
            }
        }
    }

    private fun standardRemainingMs(nowMs: Long): Long {
        val duration = when {
            flowPhase == FlowPhase.Recording &&
                standardStep == StandardInvestigationStep.NaturalPosition ->
                SignalStandardAuthority.NATURAL_OPEN_MS
            flowPhase == FlowPhase.Recording -> SignalStandardAuthority.SINGLE_EYE_RECORD_MS
            flowPhase == FlowPhase.Complete -> SignalStandardAuthority.COMPLETE_PAUSE_MS
            flowPhase == FlowPhase.StandardObserve &&
                standardStep == StandardInvestigationStep.NaturalBlink ->
                SignalStandardAuthority.NATURAL_BLINK_OBSERVE_MS
            flowPhase == FlowPhase.StandardObserve &&
                standardStep == StandardInvestigationStep.L1R1 ->
                SignalStandardAuthority.L1R1_MAX_MS
            flowPhase == FlowPhase.StandardObserve &&
                standardStep == StandardInvestigationStep.L2R2 ->
                SignalStandardAuthority.L2R2_MAX_MS
            else -> return 0L
        }
        if (phaseStartedMs <= 0L) return duration
        return (duration - (nowMs - phaseStartedMs)).coerceAtLeast(0L)
    }

    private fun finishStandardStep(nowMs: Long, completed: Boolean) {
        recordStandardStepReport(completed = completed, skipped = false)
        val next = nextStandardStep(completed)
        if (next == null) {
            publishStandardReport()
            return
        }
        standardStep = next
        enterStandardStep(nowMs, announce = true)
    }

    private fun nextStandardStep(lastCompleted: Boolean): StandardInvestigationStep? {
        return when (standardStep) {
            StandardInvestigationStep.NaturalPosition -> StandardInvestigationStep.LeftEye
            StandardInvestigationStep.LeftEye -> StandardInvestigationStep.RightEye
            StandardInvestigationStep.RightEye -> StandardInvestigationStep.NaturalBlink
            StandardInvestigationStep.NaturalBlink -> {
                if (qualityOkForSequences()) {
                    StandardInvestigationStep.L1R1
                } else {
                    // Skip L1/L2 — record skip reports
                    standardStepReports += StandardStepReport(
                        step = StandardInvestigationStep.L1R1,
                        completed = false,
                        skipped = true,
                        notes = "Skipped — detection quality not yet acceptable."
                    )
                    standardStepReports += StandardStepReport(
                        step = StandardInvestigationStep.L2R2,
                        completed = false,
                        skipped = true,
                        notes = "Skipped — detection quality not yet acceptable."
                    )
                    l1r1Outcome = "Skipped"
                    l2r2Outcome = "Skipped"
                    null
                }
            }
            StandardInvestigationStep.L1R1 -> {
                if (l1r1Outcome == "Success") {
                    StandardInvestigationStep.L2R2
                } else {
                    standardStepReports += StandardStepReport(
                        step = StandardInvestigationStep.L2R2,
                        completed = false,
                        skipped = true,
                        notes = "Skipped — L1 R1 did not pass."
                    )
                    l2r2Outcome = "Skipped"
                    null
                }
            }
            StandardInvestigationStep.L2R2 -> null
        }
    }

    private fun qualityOkForSequences(): Boolean {
        val openN = minOf(standardBuf.openLeft.size, standardBuf.openRight.size)
        val leftSep = SignalInvestigationReportAuthority.separation(
            standardBuf.openLeft, standardBuf.leftClosedLeft
        )
        val rightSep = SignalInvestigationReportAuthority.separation(
            standardBuf.openRight, standardBuf.rightClosedRight
        )
        val nullPct = if (standardBuf.framesSeen == 0) {
            0f
        } else {
            standardBuf.nullCount * 100f / standardBuf.framesSeen
        }
        val faceW = standardBuf.faceWidths.averageOrNull()
        return SignalStandardAuthority.qualityAcceptableForSequences(
            openSampleCount = openN,
            leftClosedCount = standardBuf.leftClosedLeft.size,
            rightClosedCount = standardBuf.rightClosedRight.size,
            nullPercent = nullPct,
            leftSep = leftSep,
            rightSep = rightSep,
            faceWidthPct = faceW
        )
    }

    private fun recordStandardStepReport(completed: Boolean, skipped: Boolean) {
        val leftSep = SignalInvestigationReportAuthority.separation(
            standardBuf.openLeft, standardBuf.leftClosedLeft
        )
        val rightSep = SignalInvestigationReportAuthority.separation(
            standardBuf.openRight, standardBuf.rightClosedRight
        )
        val nullPct = if (standardBuf.framesSeen == 0) {
            0f
        } else {
            standardBuf.nullCount * 100f / standardBuf.framesSeen
        }
        val notes = when (standardStep) {
            StandardInvestigationStep.NaturalBlink ->
                "Natural blinks observed: ${blinkTracker.blinkCount}"
            StandardInvestigationStep.L1R1 -> "Outcome: $l1r1Outcome"
            StandardInvestigationStep.L2R2 -> "Outcome: $l2r2Outcome"
            else -> ""
        }
        // Avoid duplicate if already recorded as skip
        if (standardStepReports.any { it.step == standardStep }) return
        standardStepReports += StandardStepReport(
            step = standardStep,
            completed = completed,
            skipped = skipped,
            openLeftAvg = standardBuf.openLeft.averageOrNull(),
            openRightAvg = standardBuf.openRight.averageOrNull(),
            closedLeftAvg = when (standardStep) {
                StandardInvestigationStep.LeftEye -> standardBuf.leftClosedLeft.averageOrNull()
                StandardInvestigationStep.RightEye -> standardBuf.rightClosedLeft.averageOrNull()
                else -> standardBuf.leftClosedLeft.averageOrNull()
            },
            closedRightAvg = when (standardStep) {
                StandardInvestigationStep.LeftEye -> standardBuf.leftClosedRight.averageOrNull()
                StandardInvestigationStep.RightEye -> standardBuf.rightClosedRight.averageOrNull()
                else -> standardBuf.rightClosedRight.averageOrNull()
            },
            leftSeparation = leftSep,
            rightSeparation = rightSep,
            sampleCount = when (standardStep) {
                StandardInvestigationStep.NaturalPosition -> standardBuf.openLeft.size
                StandardInvestigationStep.LeftEye -> standardBuf.leftClosedLeft.size
                StandardInvestigationStep.RightEye -> standardBuf.rightClosedRight.size
                StandardInvestigationStep.NaturalBlink -> blinkTracker.blinkCount
                else -> winkOrder.size
            },
            nullPercent = nullPct,
            notes = notes
        )
    }

    private fun publishStandardReport() {
        advancingLocked = true
        try {
            val completed = clockMs()
            testCompletedMs = completed
            val generated = completed + 1L
            val faceW = standardBuf.faceWidths.averageOrNull()
            val nullPct = if (standardBuf.framesSeen == 0) {
                0f
            } else {
                standardBuf.nullCount * 100f / standardBuf.framesSeen
            }
            val leftSep = SignalInvestigationReportAuthority.separation(
                standardBuf.openLeft, standardBuf.leftClosedLeft
            )
            val rightSep = SignalInvestigationReportAuthority.separation(
                standardBuf.openRight, standardBuf.rightClosedRight
            )
            environmentDiagnoses = SignalStandardAuthority.diagnoseEnvironment(
                faceDetected = lastPose?.faceDetected == true || standardBuf.openLeft.isNotEmpty(),
                faceWidthPct = faceW,
                nullPercent = nullPct,
                leftSep = leftSep,
                rightSep = rightSep,
                lighting = lighting,
                glasses = glasses
            )
            val recs = SignalStandardAuthority.caregiverRecommendations(
                environmentDiagnoses, faceW
            )
            val findings = buildList {
                add("Standard investigation measured eyes in the patient's natural position.")
                add("No head-tilt or distance movement was requested of the patient.")
                if (leftSep != null) {
                    add("Left-eye open/closed separation: ${"%.3f".format(leftSep)}.")
                }
                if (rightSep != null) {
                    add("Right-eye open/closed separation: ${"%.3f".format(rightSep)}.")
                }
                add("Natural blinks observed: ${blinkTracker.blinkCount}.")
                add("L1 R1: $l1r1Outcome. L2 R2: $l2r2Outcome.")
            }
            val distance = when {
                faceW == null -> "Natural placement (measured)"
                faceW >= 32f -> "Natural placement (close — face width ${"%.1f".format(faceW)}%)"
                faceW <= 22f -> "Natural placement (far — face width ${"%.1f".format(faceW)}%)"
                else -> "Natural placement (face width ${"%.1f".format(faceW)}%)"
            }
            val report = SignalInvestigationReport(
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
                glasses = glasses,
                lighting = lighting,
                distanceLabel = distance,
                faceWidthPercent = faceW,
                cameraResolution = cameraResolution,
                investigationMode = SignalInvestigationMode.Standard,
                baseline = null,
                positions = emptyList(),
                standardSteps = standardStepReports.toList(),
                environmentDiagnoses = environmentDiagnoses,
                naturalBlinkCount = blinkTracker.blinkCount,
                l1r1Outcome = l1r1Outcome,
                l2r2Outcome = l2r2Outcome,
                sequencesAttempted = sequencesAttempted,
                bestLeftSeparation = null,
                bestRightSeparation = null,
                lowestJitter = null,
                lowestRejected = null,
                bestOverall = null,
                engineeringFindings = findings,
                recommendations = recs,
                ttsAvailable = ttsAvailable,
                voiceGuidanceEventsTotal = totalVoiceEvents
            )
            val text = SignalInvestigationReportAuthority.formatFullText(report)
            lastReport = report
            lastReportText = text
            lastReportFilePath = try {
                store.saveReport(text, sessionId, generated).absolutePath
            } catch (_: Exception) {
                null
            }
            uiPhase = UiPhase.Report
            speakOnce("Done.", force = true)
            statusMessage = "Standard investigation complete."
            refreshUi()
        } finally {
            advancingLocked = false
        }
    }

    // -------------------------------------------------------------------------
    // Advanced engineering (pose) — retained, not in default flow
    // -------------------------------------------------------------------------

    private fun currentPosition(): SignalPosition? =
        SignalPosition.entries.getOrNull(positionIndex)

    private fun enterGuiding(nowMs: Long, announce: Boolean) {
        flowPhase = FlowPhase.Guiding
        phaseStartedMs = nowMs
        stableSinceMs = 0L
        spokenOpenEyesForStep = false
        lastCountdownSecond = -1
        val pos = currentPosition() ?: return
        val target = SignalPoseGuidance.targetsFor(pos, baselinePose)
        buffers[pos]?.targetDescription = target.description
        buffers[pos]?.targetRangeLabel =
            "${target.primaryLabel} ${"%.1f".format(target.min)}..${"%.1f".format(target.max)}"
        if (announce) {
            speakOnce(
                when (pos) {
                    SignalPosition.HeadTiltLeft -> "Tilt left."
                    SignalPosition.HeadTiltRight -> "Tilt right."
                    SignalPosition.PhoneHigher -> "Phone higher."
                    SignalPosition.PhoneLower -> "Phone lower."
                    SignalPosition.Closer -> "Closer."
                    SignalPosition.Further -> "Further."
                    SignalPosition.HeadStraight -> "Look naturally."
                },
                force = true
            )
        }
        positionStartedMs = nowMs
        refreshUi()
    }

    private fun onGuiding(guidance: SignalPoseGuidance.GuidanceResult, nowMs: Long) {
        if (guidance.inTarget) {
            flowPhase = FlowPhase.Stabilizing
            stableSinceMs = nowMs
            phaseStartedMs = nowMs
            // No "Good / Hold still" chatter — visual ring leads.
        }
    }

    private fun onStabilizing(guidance: SignalPoseGuidance.GuidanceResult, nowMs: Long) {
        if (!guidance.inTarget) {
            flowPhase = FlowPhase.Guiding
            stableSinceMs = 0L
            return
        }
        if (nowMs - stableSinceMs >= SignalPoseGuidance.STABLE_REQUIRED_MS) {
            val pos = currentPosition()
            if (pos != null && buffers[pos]?.timeToReachMs == 0L) {
                buffers[pos]?.timeToReachMs = nowMs - positionStartedMs
            }
            beginPrepare(nowMs)
        }
    }

    private fun beginPrepare(nowMs: Long) {
        flowPhase = FlowPhase.Prepare
        phaseStartedMs = nowMs
        lastCountdownSecond = -1
        if (captureKind == CaptureKind.Closed) {
            speakOnce("Close both eyes.", force = true)
        }
        refreshUi()
    }

    private fun onPrepareSample(guidance: SignalPoseGuidance.GuidanceResult, nowMs: Long) {
        if (!guidance.inTarget) {
            flowPhase = FlowPhase.Guiding
            stableSinceMs = 0L
            phaseStartedMs = nowMs
        }
    }

    private fun speakCountdown(nowMs: Long) {
        val rem = remainingMs(nowMs)
        val sec = ((rem + 999) / 1000).toInt().coerceIn(0, 4)
        if (sec in 1..4) lastCountdownSecond = sec
    }

    private fun beginRecording(nowMs: Long) {
        flowPhase = FlowPhase.Recording
        phaseStartedMs = nowMs
        playCue(AudioCue.RecordStart)
        speakOnce("Recording.", force = true)
        refreshUi()
    }

    private fun onRecordingSample(
        pose: SignalPoseGuidance.LivePose,
        guidance: SignalPoseGuidance.GuidanceResult,
        left: Float?,
        right: Float?,
        faceDetected: Boolean,
        faceWidth: Float?,
        frameAccepted: Boolean,
        nowMs: Long
    ) {
        val pos = currentPosition() ?: return
        val buf = buffers.getOrPut(pos) { PositionBuffers() }
        buf.framesSeen++
        buf.recordingFrames++
        if (!frameAccepted) buf.framesRejected++
        if (left == null || right == null) buf.nullCount++
        if (!guidance.inTarget) {
            buf.poseMismatchRejects++
            buf.framesRejected++
            return
        }
        buf.inTargetFrames++
        pose.userYaw?.let { buf.yaws += it }
        pose.userRoll?.let { buf.rolls += it }
        faceWidth?.let { buf.faceWidths += it }
        pose.faceCenterYPct?.let { buf.faceCenterYs += it }
        if (!frameAccepted || !faceDetected) return
        when (captureKind) {
            CaptureKind.Open -> {
                left?.let { buf.openLeft += it }
                right?.let { buf.openRight += it }
            }
            CaptureKind.Closed -> {
                left?.let { buf.closedLeft += it }
                right?.let { buf.closedRight += it }
            }
        }
    }

    private fun endRecording(nowMs: Long) {
        val pos = currentPosition()
        val buf = pos?.let { buffers[it] }
        val rejectPct = if (buf == null || buf.recordingFrames == 0) {
            100f
        } else {
            buf.poseMismatchRejects * 100f / buf.recordingFrames
        }
        playCue(AudioCue.RecordEnd)
        if (rejectPct > SignalPoseGuidance.MAX_POSE_REJECT_PERCENT) {
            buf?.repeated = true
            speakOnce("Again.", force = true)
            if (buf != null && captureKind == CaptureKind.Open) {
                buf.openLeft.clear(); buf.openRight.clear()
            }
            if (buf != null && captureKind == CaptureKind.Closed) {
                buf.closedLeft.clear(); buf.closedRight.clear()
            }
            buf?.poseMismatchRejects = 0
            buf?.inTargetFrames = 0
            buf?.recordingFrames = 0
            enterGuiding(nowMs, announce = true)
            return
        }
        flowPhase = FlowPhase.Complete
        phaseStartedMs = nowMs
        if (captureKind == CaptureKind.Open) {
            speakOnce("Done.", force = true)
        } else {
            spokenOpenEyesForStep = true
            speakOnce("Done.", force = true)
            speakOnce("Open both eyes.", force = true)
        }
        refreshUi()
    }

    private fun afterComplete(nowMs: Long) {
        if (captureKind == CaptureKind.Open) {
            captureKind = CaptureKind.Closed
            flowPhase = FlowPhase.Stabilizing
            stableSinceMs = nowMs
            phaseStartedMs = nowMs
            refreshUi()
            return
        }
        flowPhase = FlowPhase.EyeRecovery
        phaseStartedMs = nowMs
        eyeRecoveryOpenSinceMs = 0L
        if (!spokenOpenEyesForStep) {
            speakOnce("Open both eyes.", force = true)
            spokenOpenEyesForStep = true
        }
        refreshUi()
    }

    private fun onEyeRecovery(pose: SignalPoseGuidance.LivePose, nowMs: Long) {
        if (nowMs - phaseStartedMs < SignalPoseGuidance.RECOVERY_MIN_MS) return
        if (!SignalPoseGuidance.bothEyesOpen(pose.leftOpen, pose.rightOpen)) {
            eyeRecoveryOpenSinceMs = 0L
            return
        }
        if (eyeRecoveryOpenSinceMs <= 0L) eyeRecoveryOpenSinceMs = nowMs
        if (nowMs - eyeRecoveryOpenSinceMs >= 1_000L) {
            finalizePositionAndShowResult(nowMs)
        }
    }

    private fun finalizePositionAndShowResult(nowMs: Long) {
        val pos = currentPosition() ?: return
        if (pos == SignalPosition.HeadStraight) {
            captureBaselineFromBuffers(pos)
        }
        flowPhase = FlowPhase.PositionResult
        phaseStartedMs = nowMs
        speakOnce("Done.", force = true)
        refreshUi()
    }

    private fun captureBaselineFromBuffers(pos: SignalPosition) {
        val buf = buffers[pos] ?: return
        val yaw = buf.yaws.averageOrNull() ?: 0f
        val roll = buf.rolls.averageOrNull() ?: 0f
        val cy = buf.faceCenterYs.averageOrNull() ?: 50f
        val w = buf.faceWidths.averageOrNull() ?: 28f
        baselinePose = SignalPoseGuidance.BaselinePose(yaw, roll, 50f, cy, w)
    }

    private fun advancePosition(nowMs: Long) {
        captureKind = CaptureKind.Open
        if (positionIndex + 1 >= SignalPosition.entries.size) {
            publishAdvancedReport()
        } else {
            positionIndex++
            enterGuiding(nowMs, announce = true)
        }
    }

    private fun publishAdvancedReport() {
        advancingLocked = true
        try {
            val completed = clockMs()
            testCompletedMs = completed
            val generated = completed + 1L
            val baseCy = baselinePose?.faceCenterYPct
            val stats = SignalPosition.entries.map { pos ->
                val b = buffers[pos] ?: PositionBuffers()
                val stablePct = if (b.recordingFrames == 0) {
                    0f
                } else {
                    b.inTargetFrames * 100f / b.recordingFrames
                }
                val valid = b.openLeft.size >= 5 && b.closedLeft.size >= 5 &&
                    stablePct >= (100f - SignalPoseGuidance.MAX_POSE_REJECT_PERCENT)
                SignalInvestigationReportAuthority.buildPositionStats(
                    position = pos,
                    openLeft = b.openLeft.toList(),
                    openRight = b.openRight.toList(),
                    closedLeft = b.closedLeft.toList(),
                    closedRight = b.closedRight.toList(),
                    framesSeen = b.framesSeen,
                    framesRejected = b.framesRejected,
                    nullCount = b.nullCount,
                    faceWidths = b.faceWidths.toList(),
                    yaws = b.yaws.toList(),
                    rolls = b.rolls.toList(),
                    faceCenterYs = b.faceCenterYs.toList(),
                    targetPoseDescription = b.targetDescription,
                    targetRangeLabel = b.targetRangeLabel,
                    timeToReachTargetMs = b.timeToReachMs,
                    stableInTargetPercent = stablePct,
                    samplesRejectedPoseMismatch = b.poseMismatchRejects,
                    measurementRepeated = b.repeated,
                    voiceGuidanceEventCount = b.voiceEvents,
                    conditionValid = valid,
                    baselineFaceCenterY = baseCy
                )
            }
            val baseline = stats.firstOrNull { it.position == SignalPosition.HeadStraight }
            val pick = SignalInvestigationReportAuthority.pickBest(stats)
            val faceWidths = stats.mapNotNull { it.avgFaceWidthPercent }
            val faceW = if (faceWidths.isEmpty()) null else faceWidths.average().toFloat()
            val distance = when {
                faceW == null -> "Measured"
                faceW >= 32f -> "Measured (close — face width ${"%.1f".format(faceW)}%)"
                faceW <= 22f -> "Measured (far — face width ${"%.1f".format(faceW)}%)"
                else -> "Measured (face width ${"%.1f".format(faceW)}%)"
            }
            val findings = listOf(
                "Advanced Engineering Investigation — pose/distance tests were run.",
                "These tests are not part of the standard patient assessment."
            ) + SignalInvestigationReportAuthority.engineeringFindings(baseline, stats)
            val recs = SignalInvestigationReportAuthority.recommendations(
                positions = stats,
                bestOverall = pick.bestOverall,
                glasses = glasses
            ).map {
                it.replace("move closer", "a caregiver may move the phone closer", ignoreCase = true)
            }
            val report = SignalInvestigationReport(
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
                glasses = glasses,
                lighting = lighting,
                distanceLabel = distance,
                faceWidthPercent = faceW,
                cameraResolution = cameraResolution,
                investigationMode = SignalInvestigationMode.AdvancedEngineering,
                baseline = baseline,
                positions = stats,
                standardSteps = emptyList(),
                environmentDiagnoses = emptyList(),
                naturalBlinkCount = 0,
                l1r1Outcome = "n/a",
                l2r2Outcome = "n/a",
                sequencesAttempted = false,
                bestLeftSeparation = pick.bestLeftSeparation,
                bestRightSeparation = pick.bestRightSeparation,
                lowestJitter = pick.lowestJitter,
                lowestRejected = pick.lowestRejected,
                bestOverall = pick.bestOverall,
                engineeringFindings = findings,
                recommendations = recs,
                ttsAvailable = ttsAvailable,
                voiceGuidanceEventsTotal = totalVoiceEvents
            )
            val text = SignalInvestigationReportAuthority.formatFullText(report)
            lastReport = report
            lastReportText = text
            lastReportFilePath = try {
                store.saveReport(text, sessionId, generated).absolutePath
            } catch (_: Exception) {
                null
            }
            uiPhase = UiPhase.Report
            speakOnce("Done.", force = true)
            statusMessage = "Advanced engineering investigation complete."
            refreshUi()
        } finally {
            advancingLocked = false
        }
    }

    private fun speakOnce(text: String, force: Boolean) {
        val now = clockMs()
        val cooldown = if (investigationMode == SignalInvestigationMode.Standard) {
            SignalStandardAuthority.VOICE_COOLDOWN_MS
        } else {
            SignalPoseGuidance.VOICE_COOLDOWN_MS
        }
        if (!force) {
            if (text == lastSpoken && now - lastSpokenMs < cooldown) return
            if (now - lastSpokenMs < cooldown / 2) return
        }
        lastSpoken = text
        lastSpokenMs = now
        totalVoiceEvents++
        if (investigationMode == SignalInvestigationMode.AdvancedEngineering) {
            currentPosition()?.let {
                buffers[it]?.voiceEvents = (buffers[it]?.voiceEvents ?: 0) + 1
            }
        }
        if (ttsAvailable) speak(text)
    }

    private fun refreshUi() {
        val envHint = if (investigationMode == SignalInvestigationMode.Standard &&
            lastPose != null &&
            !SignalStandardAuthority.faceUsableForNatural(
                lastPose!!.faceDetected,
                lastPose!!.faceWidthPct
            )
        ) {
            "Waiting for a clear face view — caregiver may adjust the phone."
        } else {
            ""
        }
        val status = standardStatusLabel()
        live = live.copy(
            instructionTitle = when (uiPhase) {
                UiPhase.Hub -> SignalInvestigationAccess.ENTRY_TITLE
                UiPhase.Conditions -> when (pendingStartMode) {
                    SignalInvestigationMode.Standard -> "Conditions"
                    SignalInvestigationMode.AdvancedEngineering -> "Advanced Engineering"
                }
                UiPhase.Report -> "Investigation Complete"
                UiPhase.Running -> ""
            },
            instructionBody = when (uiPhase) {
                UiPhase.Hub ->
                    "Natural-position assessment. The patient stays still."
                UiPhase.Conditions ->
                    if (!ttsAvailable && !allowVisualOnly) {
                        "Voice unavailable — confirm visual-only before starting."
                    } else {
                        "Glasses and lighting, then begin."
                    }
                else -> ""
            },
            correction = lastGuidance?.correction.orEmpty(),
            targetBand = lastGuidance?.band ?: SignalPoseGuidance.Band.Unknown,
            recordingStatus = status,
            remainingMs = remainingMs(),
            flowPhase = flowPhase,
            captureKind = captureKind,
            positionLabel = when (investigationMode) {
                SignalInvestigationMode.Standard -> standardStep.displayName
                SignalInvestigationMode.AdvancedEngineering ->
                    currentPosition()?.displayName.orEmpty()
            },
            positionIndex = when (investigationMode) {
                SignalInvestigationMode.Standard ->
                    StandardInvestigationStep.entries.indexOf(standardStep)
                SignalInvestigationMode.AdvancedEngineering -> positionIndex
            },
            positionTotal = when (investigationMode) {
                SignalInvestigationMode.Standard -> StandardInvestigationStep.entries.size
                SignalInvestigationMode.AdvancedEngineering -> SignalPosition.entries.size
            },
            investigationMode = investigationMode,
            standardStep = if (investigationMode == SignalInvestigationMode.Standard) {
                standardStep
            } else {
                null
            },
            leftProb = lastPose?.leftOpen,
            rightProb = lastPose?.rightOpen,
            faceWidthPercent = lastPose?.faceWidthPct,
            faceDetected = lastPose?.faceDetected == true,
            technicalYaw = lastPose?.userYaw,
            technicalRoll = lastPose?.userRoll,
            technicalCenterY = lastPose?.faceCenterYPct,
            technicalAcceptedFrames = when (investigationMode) {
                SignalInvestigationMode.Standard -> standardBuf.openLeft.size
                else -> currentPosition()?.let { buffers[it]?.inTargetFrames } ?: 0
            },
            technicalRejectedFrames = when (investigationMode) {
                SignalInvestigationMode.Standard -> standardBuf.nullCount
                else -> currentPosition()?.let { buffers[it]?.framesRejected } ?: 0
            },
            technicalPoseMismatchRejects =
                currentPosition()?.let { buffers[it]?.poseMismatchRejects } ?: 0,
            showTechnical = showTechnicalDetails,
            ttsWarning = !ttsAvailable && !allowVisualOnly,
            environmentHint = envHint
        )
    }

    private fun standardStatusLabel(): String {
        if (investigationMode != SignalInvestigationMode.Standard) {
            return when (flowPhase) {
                FlowPhase.Guiding -> "Align"
                FlowPhase.Stabilizing -> "Hold Still"
                FlowPhase.Prepare -> "Hold Still"
                FlowPhase.Recording -> "Recording"
                FlowPhase.Complete -> "Complete"
                FlowPhase.EyeRecovery -> "Open Eyes"
                FlowPhase.PositionResult -> "Complete"
                else -> ""
            }
        }
        return when (flowPhase) {
            FlowPhase.StandardPrompt -> "Look Naturally"
            FlowPhase.StandardWaitEye -> when (standardStep) {
                StandardInvestigationStep.LeftEye -> "Close Left Eye"
                StandardInvestigationStep.RightEye -> "Close Right Eye"
                else -> "Wait"
            }
            FlowPhase.Recording -> "Recording"
            FlowPhase.StandardRecovery -> "Open Eyes"
            FlowPhase.Complete -> "Complete"
            FlowPhase.StandardObserve -> when (standardStep) {
                StandardInvestigationStep.NaturalBlink -> "Blink Naturally"
                StandardInvestigationStep.L1R1 -> "L1 R1"
                StandardInvestigationStep.L2R2 -> "L2 R2"
                else -> "Observe"
            }
            else -> standardStep.displayName
        }
    }

    private fun resetRun() {
        buffers.clear()
        positionIndex = 0
        captureKind = CaptureKind.Open
        flowPhase = FlowPhase.Guiding
        phaseStartedMs = 0L
        stableSinceMs = 0L
        baselinePose = null
        lastPose = null
        lastGuidance = null
        advancingLocked = false
        totalVoiceEvents = 0
        lastSpoken = ""
        lastSpokenMs = 0L
        standardStep = StandardInvestigationStep.NaturalPosition
        standardBuf.openLeft.clear()
        standardBuf.openRight.clear()
        standardBuf.leftClosedLeft.clear()
        standardBuf.leftClosedRight.clear()
        standardBuf.rightClosedLeft.clear()
        standardBuf.rightClosedRight.clear()
        standardBuf.faceWidths.clear()
        standardBuf.framesSeen = 0
        standardBuf.nullCount = 0
        winkTracker.reset()
        blinkTracker.reset()
        winkOrder.clear()
        faceStableSinceMs = 0L
        l1r1Outcome = "n/a"
        l2r2Outcome = "n/a"
        sequencesAttempted = false
        environmentDiagnoses = emptyList()
        standardStepReports.clear()
        announcedStep = null
        investigationMode = SignalInvestigationMode.Standard
    }

    private fun List<Float>.averageOrNull(): Float? =
        if (isEmpty()) null else average().toFloat()

    companion object {
        const val PREPARE_MS = SignalPoseGuidance.PREPARE_MS
        const val OPEN_MS = SignalPoseGuidance.OPEN_RECORD_MS
        const val CLOSED_MS = SignalPoseGuidance.CLOSED_RECORD_MS
        const val COMPLETE_MS = SignalPoseGuidance.COMPLETE_MS
        const val STABLE_MS = SignalPoseGuidance.STABLE_REQUIRED_MS
    }
}
