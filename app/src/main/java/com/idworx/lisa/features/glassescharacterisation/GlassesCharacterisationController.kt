package com.idworx.lisa.features.glassescharacterisation

import com.idworx.lisa.features.blinkdetectionreliability.BlinkDetectionTuning

/**
 * Debug-only Glasses Characterisation controller.
 * Observational optical-condition investigation — no production actions.
 */
class GlassesCharacterisationController(
    private val store: GlassesCharacterisationStore,
    private val isDebugBuild: Boolean,
    private val clockMs: () -> Long = { System.currentTimeMillis() },
    private val speak: (String) -> Unit = {},
    private val playCue: (AudioCue) -> Unit = {},
    private val ttsAvailableProvider: () -> Boolean = { true },
    private val standardTuningProvider: () -> BlinkDetectionTuning = { BlinkDetectionTuning.default }
) {
    enum class AudioCue { RecordStart, RecordEnd }

    data class LiveUi(
        val title: String = "",
        val body: String = "",
        val status: String = "",
        val remainingMs: Long = 0L,
        val progress01: Float = 0f,
        val conditionIndex: Int = 0,
        val conditionTotal: Int = LightingConditionKind.entries.size,
        val conditionName: String = "",
        val cycleIndex: Int = 0,
        val cycleTotal: Int = GlassesCharacterisationMetrics.CLOSE_CYCLES,
        val flowPhase: GlassesCharFlowPhase = GlassesCharFlowPhase.WaitFace,
        val uiPhase: GlassesCharUiPhase = GlassesCharUiPhase.Hub,
        val leftProb: Float? = null,
        val rightProb: Float? = null,
        val faceDetected: Boolean = false,
        val positionWarning: Boolean = false,
        val showTechnical: Boolean = false,
        val technicalLine: String = "",
        val conditionResultSummary: String = "",
        val qualityLabel: String = "",
        val ttsWarning: Boolean = false,
        val sourceLabel: LightingSourceLabel = LightingSourceLabel.OtherUnknown,
        val completedConditionName: String = "",
        val nextConditionName: String = "",
        val caregiverTransitionHint: String = "",
        val analyseMessage: String = "",
        val sessionIdShort: String = ""
    )

    private data class DeviceInfo(
        val versionName: String = "",
        val versionCode: Int = 0,
        val manufacturer: String = "",
        val model: String = "",
        val android: String = ""
    )

    private class ConditionBuffers {
        val openLeft = mutableListOf<Float>()
        val openRight = mutableListOf<Float>()
        val closedLeftSelected = mutableListOf<Float>()
        val closedLeftOpposite = mutableListOf<Float>()
        val closedRightSelected = mutableListOf<Float>()
        val closedRightOpposite = mutableListOf<Float>()
        var framesSeen = 0
        var nullCount = 0
        var rejected = 0
        var invalidPose = 0
        var positionWarnings = 0
        var blinkCandidates = 0
        var completedBlinks = 0
        var cancelledCandidates = 0
        var falseWinkCandidates = 0
        var bothEyeClosures = 0
        var uncertainEntries = 0
        var leftAsym = 0
        var rightAsym = 0
        val openSeries = mutableListOf<Float>()
    }

    var isOpen: Boolean = false
        private set
    var uiPhase: GlassesCharUiPhase = GlassesCharUiPhase.Hub
        private set
    var live: LiveUi = LiveUi()
        private set
    var statusMessage: String = ""
        private set
    var sourceLabel: LightingSourceLabel = LightingSourceLabel.OtherUnknown
    var lastReport: GlassesCharacterisationReport? = null
        private set
    var lastReportText: String = ""
        private set
    var lastReportFilePath: String? = null
        private set
    var showTechnicalDetails: Boolean = false
    var copyConfirmation: String = ""

    private var deviceInfo = DeviceInfo()
    private var setup = SetupSnapshot()
    private var baselinePose = PoseSnapshot()
    private var baselineCaptured = false
    private var sessionId = ""
    private var testStartedMs = 0L
    private var testCompletedMs = 0L
    private var conditionIndex = 0
    private var flowPhase = GlassesCharFlowPhase.WaitFace
    private var phaseStartedMs = 0L
    private var stableSinceMs = 0L
    private var cycleIndex = 0
    private var eyeRecoveryOpenSinceMs = 0L
    private var lastSpoken = ""
    private var lastSpokenMs = 0L
    private var ttsAvailable = true
    private var allowVisualOnly = false
    private var positionWarned = false
    private var advancingLocked = false
    private val conditionResults = linkedMapOf<LightingConditionKind, ConditionResult>()
    private var currentBuf = ConditionBuffers()
    private var blinkDipped = false
    private var lastPose = PoseSnapshot()
    private var lastLeft: Float? = null
    private var lastRight: Float? = null
    private var lastFaceDetected = false
    private var cameraResolution = "n/a"
    private var sessionPhoneWarnings = 0
    private var sessionPatientWarnings = 0
    private var positionConsistencyOk = true
    private var analyseStep = 0
    private var lastCompletedCondition: LightingConditionKind? = null
    private var pendingNextCondition: LightingConditionKind? = null

    fun open(): Boolean {
        if (!GlassesCharacterisationAccess.isScreenAllowed(isDebugBuild)) return false
        isOpen = true
        uiPhase = GlassesCharUiPhase.Hub
        live = LiveUi()
        ttsAvailable = ttsAvailableProvider()
        statusMessage = "Glasses Characterisation — debug only. Glasses = YES."
        refreshUi()
        return true
    }

    fun close() {
        isOpen = false
        resetAll()
        uiPhase = GlassesCharUiPhase.Hub
        live = LiveUi()
        statusMessage = ""
        copyConfirmation = ""
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

    fun applyRuntimeSetup(
        sensitivityLevel: Int,
        responseTimeLabel: String,
        screenBrightness: Float?,
        ambientLux: Float?,
        deviceOrientation: String,
        cameraResolution: String?
    ) {
        val tuning = standardTuningProvider()
        setup = setup.copy(
            sensitivityLevel = sensitivityLevel,
            responseTimeLabel = responseTimeLabel,
            screenBrightness = screenBrightness,
            ambientLux = ambientLux,
            deviceOrientation = deviceOrientation,
            cameraResolution = cameraResolution ?: this.cameraResolution,
            standardClosedThreshold = tuning.closedEyeThreshold,
            standardOpenThreshold = tuning.openEyeThreshold
        )
        if (cameraResolution != null) this.cameraResolution = cameraResolution
    }

    fun confirmVisualOnlyWithoutTts(): Boolean {
        if (!isOpen) return false
        allowVisualOnly = true
        refreshUi()
        return true
    }

    fun returnToHub() {
        if (!isOpen) return
        resetAll()
        uiPhase = GlassesCharUiPhase.Hub
        statusMessage = "Returned to hub."
        refreshUi()
    }

    fun openSetup(): Boolean {
        if (!isOpen || !isDebugBuild) return false
        uiPhase = GlassesCharUiPhase.Setup
        ttsAvailable = ttsAvailableProvider()
        refreshUi()
        return true
    }

    fun startTest(): Boolean {
        if (!isOpen || !isDebugBuild) return false
        ttsAvailable = ttsAvailableProvider()
        if (!ttsAvailable && !allowVisualOnly) {
            statusMessage = "TTS unavailable. Confirm visual-only to continue."
            refreshUi()
            return false
        }
        resetRunKeepSetup()
        sessionId = GlassesCharacterisationReportAuthority.newSessionId()
        testStartedMs = clockMs()
        conditionIndex = 0
        uiPhase = GlassesCharUiPhase.LightingPrep
        statusMessage = "One continuous session — Normal Lighting first."
        speakOnce("Keep both eyes open.", force = false)
        refreshUi()
        return true
    }

    /**
     * Single session continue button:
     * - LightingPrep (Normal): begin first recordings + baseline once
     * - LightingTransition: validate pose vs baseline, then next lighting recordings
     */
    fun iAmReady(): Boolean {
        if (!isOpen) return false
        return when (uiPhase) {
            GlassesCharUiPhase.LightingPrep -> beginFirstCondition()
            GlassesCharUiPhase.LightingTransition -> beginNextConditionAfterTransition()
            else -> false
        }
    }

    /** @deprecated Use [iAmReady]. Kept for call-site compatibility. */
    fun lightingReady(): Boolean = iAmReady()

    /** @deprecated Transitions are automatic via [iAmReady]. */
    fun continueNextCondition(): Boolean = iAmReady()

    private fun beginFirstCondition(): Boolean {
        if (uiPhase != GlassesCharUiPhase.LightingPrep) return false
        currentBuf = ConditionBuffers()
        cycleIndex = 0
        positionWarned = false
        flowPhase = GlassesCharFlowPhase.WaitFace
        phaseStartedMs = clockMs()
        stableSinceMs = 0L
        uiPhase = GlassesCharUiPhase.Running
        speakOnce("Lighting ready.", force = true)
        refreshUi()
        return true
    }

    private fun beginNextConditionAfterTransition(): Boolean {
        if (uiPhase != GlassesCharUiPhase.LightingTransition) return false
        val next = pendingNextCondition ?: return false
        conditionIndex = LightingConditionKind.entries.indexOf(next)
        currentBuf = ConditionBuffers()
        cycleIndex = 0
        positionWarned = false
        flowPhase = GlassesCharFlowPhase.ValidateAgainstBaseline
        phaseStartedMs = clockMs()
        stableSinceMs = 0L
        uiPhase = GlassesCharUiPhase.Running
        speakOnce("Lighting ready.", force = true)
        refreshUi()
        return true
    }

    fun retryStep(): Boolean {
        if (!isOpen || uiPhase != GlassesCharUiPhase.Running) return false
        phaseStartedMs = clockMs()
        stableSinceMs = 0L
        eyeRecoveryOpenSinceMs = 0L
        when (flowPhase) {
            GlassesCharFlowPhase.RecordOpen, GlassesCharFlowPhase.PrepareOpen ->
                flowPhase = GlassesCharFlowPhase.Stabilize
            GlassesCharFlowPhase.RecordLeft, GlassesCharFlowPhase.PrepareLeft ->
                flowPhase = GlassesCharFlowPhase.PrepareLeft
            GlassesCharFlowPhase.RecordRight, GlassesCharFlowPhase.PrepareRight ->
                flowPhase = GlassesCharFlowPhase.PrepareRight
            else -> Unit
        }
        refreshUi()
        return true
    }

    fun skipAndRecordFailure(): Boolean {
        if (!isOpen || uiPhase != GlassesCharUiPhase.Running) return false
        finalizeCondition(completed = false, endedEarly = true, notes = "Step skipped / failure recorded.")
        return true
    }

    fun endConditionEarly(): Boolean {
        if (!isOpen || uiPhase != GlassesCharUiPhase.Running) return false
        finalizeCondition(completed = false, endedEarly = true, notes = "Condition ended early.")
        return true
    }

    fun endFullTestEarly(): Boolean {
        if (!isOpen) return false
        when (uiPhase) {
            GlassesCharUiPhase.Running ->
                finalizeCondition(
                    completed = false,
                    endedEarly = true,
                    notes = "Full test ended early.",
                    forceEndSession = true
                )
            GlassesCharUiPhase.LightingTransition,
            GlassesCharUiPhase.LightingPrep,
            GlassesCharUiPhase.Analysing,
            GlassesCharUiPhase.Setup ->
                publishReport(incomplete = true)
            else -> Unit
        }
        return true
    }

    fun restart(): Boolean = openSetup().also {
        if (it) statusMessage = "Restart — confirm setup."
    }

    fun toggleTechnicalDetails() {
        showTechnicalDetails = !showTechnicalDetails
        refreshUi()
    }

    fun selectSourceLabel(label: LightingSourceLabel) {
        sourceLabel = label
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
            setup = setup.copy(cameraResolution = cameraResolution)
        }
        lastLeft = left
        lastRight = right
        lastFaceDetected = faceDetected
        val pose = PoseSnapshot(faceCenterXPct, faceCenterYPct, faceWidth, headYaw, headRoll)
        lastPose = pose
        if (uiPhase != GlassesCharUiPhase.Running) {
            refreshUi()
            return
        }
        val now = clockMs()
        val drift = baselineCaptured &&
            GlassesCharacterisationMetrics.poseDriftExcessive(baselinePose, pose)
        if (drift) {
            positionConsistencyOk = false
            if (!positionWarned) {
                currentBuf.positionWarnings++
                sessionPhoneWarnings++
                positionWarned = true
                speakOnce("Phone position changed.", force = true)
            }
        } else if (positionWarned && baselineCaptured) {
            positionWarned = false
            speakOnce("Position restored.", force = true)
        }

        val scoring = isScoringPhase()
        if (scoring) {
            currentBuf.framesSeen++
            if (left == null || right == null) currentBuf.nullCount++
            if (!frameAccepted) currentBuf.rejected++
            if (drift) {
                currentBuf.invalidPose++
                refreshUi()
                return
            }
        }

        when (flowPhase) {
            GlassesCharFlowPhase.WaitFace -> {
                if (faceDetected && left != null && right != null) {
                    flowPhase = GlassesCharFlowPhase.Stabilize
                    stableSinceMs = now
                    phaseStartedMs = now
                }
            }
            GlassesCharFlowPhase.Stabilize -> {
                if (!faceDetected || left == null || right == null ||
                    !GlassesCharacterisationMetrics.bothEyesOpen(left, right)
                ) {
                    stableSinceMs = 0L
                } else if (stableSinceMs <= 0L) {
                    stableSinceMs = now
                } else if (now - stableSinceMs >= GlassesCharacterisationMetrics.FACE_STABLE_MS) {
                    // Baseline once per session only.
                    if (!baselineCaptured) {
                        baselinePose = pose
                        setup = setup.copy(pose = pose)
                        baselineCaptured = true
                    }
                    beginPrepareOpen(now)
                }
            }
            GlassesCharFlowPhase.ValidateAgainstBaseline -> {
                if (!faceDetected || left == null || right == null) {
                    stableSinceMs = 0L
                } else if (drift) {
                    stableSinceMs = 0L
                } else if (!GlassesCharacterisationMetrics.bothEyesOpen(left, right)) {
                    stableSinceMs = 0L
                } else if (stableSinceMs <= 0L) {
                    stableSinceMs = now
                } else if (
                    now - stableSinceMs >= GlassesCharacterisationMetrics.BASELINE_REVALIDATE_MS
                ) {
                    // Do not re-capture baseline — recordings only.
                    beginPrepareOpen(now)
                }
            }
            GlassesCharFlowPhase.PrepareOpen -> Unit
            GlassesCharFlowPhase.RecordOpen -> {
                if (!drift && frameAccepted && faceDetected) {
                    left?.let { currentBuf.openLeft += it; currentBuf.openSeries += it }
                    right?.let { currentBuf.openRight += it }
                }
            }
            GlassesCharFlowPhase.PrepareLeft, GlassesCharFlowPhase.PrepareRight -> Unit
            GlassesCharFlowPhase.RecordLeft -> {
                if (!drift && frameAccepted && faceDetected) {
                    left?.let { currentBuf.closedLeftSelected += it }
                    right?.let { currentBuf.closedLeftOpposite += it }
                }
            }
            GlassesCharFlowPhase.RecordRight -> {
                if (!drift && frameAccepted && faceDetected) {
                    right?.let { currentBuf.closedRightSelected += it }
                    left?.let { currentBuf.closedRightOpposite += it }
                }
            }
            GlassesCharFlowPhase.RecoverLeft, GlassesCharFlowPhase.RecoverRight -> {
                if (GlassesCharacterisationMetrics.bothEyesOpen(left, right)) {
                    if (eyeRecoveryOpenSinceMs <= 0L) eyeRecoveryOpenSinceMs = now
                } else {
                    eyeRecoveryOpenSinceMs = 0L
                }
            }
            GlassesCharFlowPhase.ObserveBlink -> observeBlink(left, right)
            else -> Unit
        }
        refreshUi()
    }

    fun onTimedTick(nowMs: Long = clockMs()): Boolean {
        if (!isOpen || advancingLocked) return false
        if (uiPhase == GlassesCharUiPhase.Analysing) {
            return onAnalyseTick(nowMs)
        }
        if (uiPhase != GlassesCharUiPhase.Running) return false
        when (flowPhase) {
            GlassesCharFlowPhase.PrepareOpen -> {
                if (remainingMs(nowMs) > 0L) { refreshUi(); return false }
                beginRecordOpen(nowMs)
                return true
            }
            GlassesCharFlowPhase.RecordOpen -> {
                if (remainingMs(nowMs) > 0L) { refreshUi(); return false }
                endRecordOpen(nowMs)
                return true
            }
            GlassesCharFlowPhase.CompleteOpen -> {
                if (remainingMs(nowMs) > 0L) { refreshUi(); return false }
                beginPrepareLeft(nowMs)
                return true
            }
            GlassesCharFlowPhase.PrepareLeft -> {
                if (remainingMs(nowMs) > 0L) { refreshUi(); return false }
                beginRecordLeft(nowMs)
                return true
            }
            GlassesCharFlowPhase.RecordLeft -> {
                if (remainingMs(nowMs) > 0L) { refreshUi(); return false }
                endRecordLeft(nowMs)
                return true
            }
            GlassesCharFlowPhase.RecoverLeft -> {
                if (nowMs - phaseStartedMs < GlassesCharacterisationMetrics.RECOVERY_MIN_MS) {
                    refreshUi(); return false
                }
                if (eyeRecoveryOpenSinceMs > 0L &&
                    nowMs - eyeRecoveryOpenSinceMs >= 800L
                ) {
                    cycleIndex++
                    if (cycleIndex >= GlassesCharacterisationMetrics.CLOSE_CYCLES) {
                        cycleIndex = 0
                        beginPrepareRight(nowMs)
                    } else {
                        beginPrepareLeft(nowMs)
                    }
                    return true
                }
                refreshUi()
                return false
            }
            GlassesCharFlowPhase.PrepareRight -> {
                if (remainingMs(nowMs) > 0L) { refreshUi(); return false }
                beginRecordRight(nowMs)
                return true
            }
            GlassesCharFlowPhase.RecordRight -> {
                if (remainingMs(nowMs) > 0L) { refreshUi(); return false }
                endRecordRight(nowMs)
                return true
            }
            GlassesCharFlowPhase.RecoverRight -> {
                if (nowMs - phaseStartedMs < GlassesCharacterisationMetrics.RECOVERY_MIN_MS) {
                    refreshUi(); return false
                }
                if (eyeRecoveryOpenSinceMs > 0L &&
                    nowMs - eyeRecoveryOpenSinceMs >= 800L
                ) {
                    cycleIndex++
                    if (cycleIndex >= GlassesCharacterisationMetrics.CLOSE_CYCLES) {
                        beginObserveBlink(nowMs)
                    } else {
                        beginPrepareRight(nowMs)
                    }
                    return true
                }
                refreshUi()
                return false
            }
            GlassesCharFlowPhase.ObserveBlink -> {
                if (remainingMs(nowMs) > 0L) { refreshUi(); return false }
                finalizeCondition(completed = true, endedEarly = false, notes = "")
                return true
            }
            GlassesCharFlowPhase.ValidateAgainstBaseline,
            GlassesCharFlowPhase.Stabilize -> {
                refreshUi()
                return false
            }
            else -> {
                refreshUi()
                return false
            }
        }
    }

    private fun onAnalyseTick(nowMs: Long): Boolean {
        if (nowMs - phaseStartedMs < GlassesCharacterisationMetrics.ANALYSE_STEP_MS) {
            refreshUi()
            return false
        }
        phaseStartedMs = nowMs
        analyseStep++
        when (analyseStep) {
            1 -> {
                statusMessage = "Comparing lighting conditions…"
                refreshUi()
                return true
            }
            2 -> {
                statusMessage = "Generating engineering report…"
                refreshUi()
                return true
            }
            else -> {
                publishReport(incomplete = false)
                return true
            }
        }
    }

    fun remainingMs(nowMs: Long = clockMs()): Long {
        val duration = when (flowPhase) {
            GlassesCharFlowPhase.PrepareOpen,
            GlassesCharFlowPhase.PrepareLeft,
            GlassesCharFlowPhase.PrepareRight -> GlassesCharacterisationMetrics.PREPARE_MS
            GlassesCharFlowPhase.RecordOpen -> GlassesCharacterisationMetrics.OPEN_RECORD_MS
            GlassesCharFlowPhase.RecordLeft,
            GlassesCharFlowPhase.RecordRight -> GlassesCharacterisationMetrics.CLOSE_RECORD_MS
            GlassesCharFlowPhase.CompleteOpen -> GlassesCharacterisationMetrics.COMPLETE_PAUSE_MS
            GlassesCharFlowPhase.ObserveBlink -> GlassesCharacterisationMetrics.BLINK_OBSERVE_MS
            GlassesCharFlowPhase.Stabilize -> {
                if (stableSinceMs <= 0L) return GlassesCharacterisationMetrics.FACE_STABLE_MS
                return (GlassesCharacterisationMetrics.FACE_STABLE_MS - (nowMs - stableSinceMs))
                    .coerceAtLeast(0L)
            }
            GlassesCharFlowPhase.ValidateAgainstBaseline -> {
                if (stableSinceMs <= 0L) return GlassesCharacterisationMetrics.BASELINE_REVALIDATE_MS
                return (
                    GlassesCharacterisationMetrics.BASELINE_REVALIDATE_MS - (nowMs - stableSinceMs)
                    ).coerceAtLeast(0L)
            }
            else -> return 0L
        }
        if (phaseStartedMs <= 0L) return duration
        return (duration - (nowMs - phaseStartedMs)).coerceAtLeast(0L)
    }

    fun fullReportText(): String = lastReportText

    fun markCopyDone() {
        copyConfirmation = "New Glasses Characterisation report copied. Session ID: $sessionId"
    }

    private fun currentCondition(): LightingConditionKind =
        LightingConditionKind.entries[conditionIndex.coerceIn(0, LightingConditionKind.entries.lastIndex)]

    private fun isScoringPhase(): Boolean = when (flowPhase) {
        GlassesCharFlowPhase.RecordOpen,
        GlassesCharFlowPhase.RecordLeft,
        GlassesCharFlowPhase.RecordRight,
        GlassesCharFlowPhase.ObserveBlink -> true
        else -> false
    }

    private fun beginPrepareOpen(nowMs: Long) {
        flowPhase = GlassesCharFlowPhase.PrepareOpen
        phaseStartedMs = nowMs
        speakOnce("Keep both eyes open.", force = true)
        refreshUi()
    }

    private fun beginRecordOpen(nowMs: Long) {
        flowPhase = GlassesCharFlowPhase.RecordOpen
        phaseStartedMs = nowMs
        playCue(AudioCue.RecordStart)
        speakOnce("Recording.", force = true)
        refreshUi()
    }

    private fun endRecordOpen(nowMs: Long) {
        playCue(AudioCue.RecordEnd)
        speakOnce("Done.", force = true)
        flowPhase = GlassesCharFlowPhase.CompleteOpen
        phaseStartedMs = nowMs
        refreshUi()
    }

    private fun beginPrepareLeft(nowMs: Long) {
        flowPhase = GlassesCharFlowPhase.PrepareLeft
        phaseStartedMs = nowMs
        eyeRecoveryOpenSinceMs = 0L
        speakOnce("Close only your left eye.", force = true)
        refreshUi()
    }

    private fun beginRecordLeft(nowMs: Long) {
        flowPhase = GlassesCharFlowPhase.RecordLeft
        phaseStartedMs = nowMs
        playCue(AudioCue.RecordStart)
        speakOnce("Recording.", force = true)
        refreshUi()
    }

    private fun endRecordLeft(nowMs: Long) {
        playCue(AudioCue.RecordEnd)
        speakOnce("Open both eyes now.", force = true)
        flowPhase = GlassesCharFlowPhase.RecoverLeft
        phaseStartedMs = nowMs
        eyeRecoveryOpenSinceMs = 0L
        refreshUi()
    }

    private fun beginPrepareRight(nowMs: Long) {
        flowPhase = GlassesCharFlowPhase.PrepareRight
        phaseStartedMs = nowMs
        eyeRecoveryOpenSinceMs = 0L
        speakOnce("Close only your right eye.", force = true)
        refreshUi()
    }

    private fun beginRecordRight(nowMs: Long) {
        flowPhase = GlassesCharFlowPhase.RecordRight
        phaseStartedMs = nowMs
        playCue(AudioCue.RecordStart)
        speakOnce("Recording.", force = true)
        refreshUi()
    }

    private fun endRecordRight(nowMs: Long) {
        playCue(AudioCue.RecordEnd)
        speakOnce("Open both eyes now.", force = true)
        flowPhase = GlassesCharFlowPhase.RecoverRight
        phaseStartedMs = nowMs
        eyeRecoveryOpenSinceMs = 0L
        refreshUi()
    }

    private fun beginObserveBlink(nowMs: Long) {
        flowPhase = GlassesCharFlowPhase.ObserveBlink
        phaseStartedMs = nowMs
        blinkDipped = false
        speakOnce("Keep looking naturally at the phone.", force = true)
        refreshUi()
    }

    private fun observeBlink(left: Float?, right: Float?) {
        val tuning = standardTuningProvider()
        if (left == null || right == null) return
        if (tuning.isEyeUncertain(left) || tuning.isEyeUncertain(right)) {
            currentBuf.uncertainEntries++
        }
        val bothOpen = GlassesCharacterisationMetrics.bothEyesOpen(left, right)
        val bothLow = left < GlassesCharacterisationMetrics.OPEN_HINT &&
            right < GlassesCharacterisationMetrics.OPEN_HINT
        val leftOnly = GlassesCharacterisationMetrics.leftClosedOnly(left, right)
        val rightOnly = GlassesCharacterisationMetrics.rightClosedOnly(left, right)
        if (bothLow) {
            if (!blinkDipped) currentBuf.blinkCandidates++
            blinkDipped = true
            currentBuf.bothEyeClosures++
        } else if (bothOpen && blinkDipped) {
            currentBuf.completedBlinks++
            blinkDipped = false
        } else if (leftOnly || rightOnly) {
            currentBuf.falseWinkCandidates++
            if (leftOnly) currentBuf.leftAsym++ else currentBuf.rightAsym++
            if (blinkDipped) {
                currentBuf.cancelledCandidates++
                blinkDipped = false
            }
        }
        currentBuf.openSeries += ((left + right) / 2f)
    }

    private fun finalizeCondition(
        completed: Boolean,
        endedEarly: Boolean,
        notes: String,
        forceEndSession: Boolean = false
    ) {
        val tuning = standardTuningProvider()
        val closedThr = tuning.closedEyeThreshold
        val openThr = tuning.openEyeThreshold
        val uncertain = { p: Float -> tuning.isEyeUncertain(p) }
        val left = GlassesCharacterisationMetrics.eyeMetrics(
            openSamples = currentBuf.openLeft.toList(),
            closedSamples = currentBuf.closedLeftSelected.toList(),
            nullCount = currentBuf.nullCount,
            framesSeen = currentBuf.framesSeen,
            rejectedCount = currentBuf.rejected + currentBuf.invalidPose,
            closedThr = closedThr,
            openThr = openThr,
            isUncertain = uncertain
        )
        val right = GlassesCharacterisationMetrics.eyeMetrics(
            openSamples = currentBuf.openRight.toList(),
            closedSamples = currentBuf.closedRightSelected.toList(),
            nullCount = currentBuf.nullCount,
            framesSeen = currentBuf.framesSeen,
            rejectedCount = currentBuf.rejected + currentBuf.invalidPose,
            closedThr = closedThr,
            openThr = openThr,
            isUncertain = uncertain
        )
        val blink = NaturalBlinkMetrics(
            durationMs = GlassesCharacterisationMetrics.BLINK_OBSERVE_MS,
            blinkCandidates = currentBuf.blinkCandidates,
            completedBlinks = currentBuf.completedBlinks,
            cancelledCandidates = currentBuf.cancelledCandidates,
            falseWinkCandidates = currentBuf.falseWinkCandidates,
            bothEyeClosures = currentBuf.bothEyeClosures,
            uncertainBandEntries = currentBuf.uncertainEntries,
            leftAsymmetryEvents = currentBuf.leftAsym,
            rightAsymmetryEvents = currentBuf.rightAsym,
            openInstability = GlassesCharacterisationMetrics.stdDev(currentBuf.openSeries)
        )
        val quality = GlassesCharacterisationQuality.classify(left, right)
        val result = ConditionResult(
            condition = currentCondition(),
            sourceLabel = sourceLabel,
            completed = completed,
            endedEarly = endedEarly,
            left = left,
            right = right,
            blink = blink,
            quality = quality,
            usableSampleCount = GlassesCharacterisationQuality.usableSampleCount(left, right),
            positionWarningCount = currentBuf.positionWarnings,
            invalidPoseSampleCount = currentBuf.invalidPose,
            notes = notes
        )
        // One condition cannot overwrite another — keyed by lighting kind.
        conditionResults[currentCondition()] = result
        flowPhase = GlassesCharFlowPhase.ConditionDone
        lastCompletedCondition = currentCondition()
        val voiceDone = when (currentCondition()) {
            LightingConditionKind.Normal -> "Normal lighting complete."
            LightingConditionKind.Brighter -> "Brighter lighting complete."
            LightingConditionKind.Dimmer -> "Dimmer lighting complete."
        }
        speakOnce(voiceDone, force = true)

        val nextIdx = conditionIndex + 1
        if (forceEndSession || nextIdx >= LightingConditionKind.entries.size) {
            if (forceEndSession) {
                publishReport(incomplete = true)
            } else {
                beginAnalysing()
            }
        } else {
            pendingNextCondition = LightingConditionKind.entries[nextIdx]
            uiPhase = GlassesCharUiPhase.LightingTransition
            when (pendingNextCondition) {
                LightingConditionKind.Brighter ->
                    speakOnce("Increase the room lighting.", force = true)
                LightingConditionKind.Dimmer ->
                    speakOnce("Reduce the room lighting.", force = true)
                else -> Unit
            }
            refreshUi()
        }
    }

    private fun beginAnalysing() {
        uiPhase = GlassesCharUiPhase.Analysing
        analyseStep = 0
        phaseStartedMs = clockMs()
        statusMessage = "Analysing measurements…"
        refreshUi()
    }

    private fun publishReport(incomplete: Boolean) {
        advancingLocked = true
        try {
            val completed = clockMs()
            testCompletedMs = completed
            val generated = completed + 1L
            val conditions = LightingConditionKind.entries.map { kind ->
                conditionResults[kind] ?: ConditionResult(
                    condition = kind,
                    completed = false,
                    endedEarly = true,
                    notes = "Skipped"
                )
            }
            val (decision, explanation) = GlassesCharacterisationComparison.decide(conditions)
            val bestL = GlassesCharacterisationComparison.bestForEye(conditions, true)
            val bestR = GlassesCharacterisationComparison.bestForEye(conditions, false)
            val bestO = GlassesCharacterisationComparison.bestOverall(conditions)
            val findings = GlassesCharacterisationComparison.findings(conditions)
            val recs = GlassesCharacterisationComparison.caregiverRecommendations(decision, bestO)
            val limitations = listOf(
                "Does not measure glare directly.",
                "Does not capture images or video.",
                "Observational Standard thresholds only — production detection unchanged.",
                "Single controlled session: lighting was the intended variable.",
                "Patient remained in natural position; lighting changes directed to caregiver.",
                "Engineering guidance only — not a production profile activation."
            )
            val report = GlassesCharacterisationReport(
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
                setup = setup,
                conditions = conditions,
                positionConsistencyMaintained = positionConsistencyOk,
                phoneMovementWarningCount = sessionPhoneWarnings,
                patientMovementWarningCount = sessionPatientWarnings,
                bestLeftLighting = bestL,
                bestRightLighting = bestR,
                bestOverallLighting = bestO,
                improvementConsistentBothEyes =
                    GlassesCharacterisationComparison.improvementConsistentBothEyes(conditions),
                lightingProducedNoMeaningfulImprovement =
                    GlassesCharacterisationComparison.noMeaningfulImprovement(conditions),
                findings = findings,
                caregiverRecommendations = recs,
                decision = decision,
                decisionExplanation = explanation,
                limitations = limitations,
                incomplete = incomplete
            )
            val text = GlassesCharacterisationReportAuthority.formatFullText(report)
            lastReport = report
            lastReportText = text
            lastReportFilePath = try {
                store.saveReport(text, sessionId, generated).absolutePath
            } catch (_: Exception) {
                null
            }
            uiPhase = GlassesCharUiPhase.FinalReport
            speakOnce("Investigation complete.", force = true)
            statusMessage = "Glasses characterisation complete."
            refreshUi()
        } finally {
            advancingLocked = false
        }
    }

    private fun speakOnce(text: String, force: Boolean) {
        val now = clockMs()
        if (!force) {
            if (text == lastSpoken &&
                now - lastSpokenMs < GlassesCharacterisationMetrics.VOICE_COOLDOWN_MS
            ) {
                return
            }
            if (now - lastSpokenMs < GlassesCharacterisationMetrics.VOICE_COOLDOWN_MS / 2) return
        }
        lastSpoken = text
        lastSpokenMs = now
        if (ttsAvailable) speak(text)
    }

    private fun refreshUi() {
        val rem = remainingMs()
        val duration = when (flowPhase) {
            GlassesCharFlowPhase.PrepareOpen,
            GlassesCharFlowPhase.PrepareLeft,
            GlassesCharFlowPhase.PrepareRight -> GlassesCharacterisationMetrics.PREPARE_MS.toFloat()
            GlassesCharFlowPhase.RecordOpen -> GlassesCharacterisationMetrics.OPEN_RECORD_MS.toFloat()
            GlassesCharFlowPhase.RecordLeft,
            GlassesCharFlowPhase.RecordRight -> GlassesCharacterisationMetrics.CLOSE_RECORD_MS.toFloat()
            GlassesCharFlowPhase.ObserveBlink -> GlassesCharacterisationMetrics.BLINK_OBSERVE_MS.toFloat()
            GlassesCharFlowPhase.Stabilize -> GlassesCharacterisationMetrics.FACE_STABLE_MS.toFloat()
            GlassesCharFlowPhase.ValidateAgainstBaseline ->
                GlassesCharacterisationMetrics.BASELINE_REVALIDATE_MS.toFloat()
            else -> 0f
        }
        val progress = if (duration <= 0f) {
            when (uiPhase) {
                GlassesCharUiPhase.Analysing ->
                    ((analyseStep + 1) / 3f).coerceIn(0f, 1f)
                else -> 0f
            }
        } else {
            (1f - rem / duration).coerceIn(0f, 1f)
        }
        val status = when {
            uiPhase == GlassesCharUiPhase.Analysing -> when (analyseStep) {
                0 -> "ANALYSING"
                1 -> "COMPARING"
                else -> "GENERATING REPORT"
            }
            flowPhase == GlassesCharFlowPhase.WaitFace -> "FIND FACE"
            flowPhase == GlassesCharFlowPhase.Stabilize -> "LOOK NATURALLY"
            flowPhase == GlassesCharFlowPhase.ValidateAgainstBaseline ->
                if (positionWarned) "RESTORE POSITION" else "CHECKING POSITION"
            flowPhase == GlassesCharFlowPhase.PrepareOpen -> "BOTH EYES OPEN"
            flowPhase == GlassesCharFlowPhase.RecordOpen -> "RECORDING"
            flowPhase == GlassesCharFlowPhase.CompleteOpen -> "COMPLETE"
            flowPhase == GlassesCharFlowPhase.PrepareLeft -> "LEFT EYE — CLOSE"
            flowPhase == GlassesCharFlowPhase.RecordLeft -> "RECORDING"
            flowPhase == GlassesCharFlowPhase.RecoverLeft -> "OPEN BOTH EYES"
            flowPhase == GlassesCharFlowPhase.PrepareRight -> "RIGHT EYE — CLOSE"
            flowPhase == GlassesCharFlowPhase.RecordRight -> "RECORDING"
            flowPhase == GlassesCharFlowPhase.RecoverRight -> "OPEN BOTH EYES"
            flowPhase == GlassesCharFlowPhase.ObserveBlink -> "LOOK NATURALLY"
            flowPhase == GlassesCharFlowPhase.ConditionDone -> "CONDITION COMPLETE"
            else -> ""
        }
        val completedName = lastCompletedCondition?.displayName.orEmpty()
        val nextName = pendingNextCondition?.displayName.orEmpty()
        val result = conditionResults[currentCondition()]
        live = LiveUi(
            title = when (uiPhase) {
                GlassesCharUiPhase.Hub -> GlassesCharacterisationAccess.ENTRY_TITLE
                GlassesCharUiPhase.Setup -> "Setup"
                GlassesCharUiPhase.LightingPrep -> LightingConditionKind.Normal.displayName
                GlassesCharUiPhase.LightingTransition -> "Next lighting"
                GlassesCharUiPhase.Running -> currentCondition().displayName
                GlassesCharUiPhase.Analysing -> "One moment"
                GlassesCharUiPhase.FinalReport -> "Investigation Complete"
            },
            body = when (uiPhase) {
                GlassesCharUiPhase.Hub ->
                    "One continuous session. Only lighting changes."
                GlassesCharUiPhase.Setup ->
                    "Glasses = YES. Patient stays still. Caregiver changes lighting only."
                GlassesCharUiPhase.LightingPrep ->
                    LightingConditionKind.Normal.caregiverInstruction
                GlassesCharUiPhase.LightingTransition ->
                    pendingNextCondition?.caregiverInstruction.orEmpty()
                GlassesCharUiPhase.Running -> if (positionWarned) {
                    "Caregiver: return the phone to approximately the previous position."
                } else {
                    ""
                }
                GlassesCharUiPhase.Analysing -> statusMessage
                GlassesCharUiPhase.FinalReport ->
                    lastReport?.decision?.name ?: "Report ready"
            },
            status = status,
            remainingMs = rem,
            progress01 = progress,
            conditionIndex = conditionIndex,
            conditionName = currentCondition().displayName,
            cycleIndex = cycleIndex,
            flowPhase = flowPhase,
            uiPhase = uiPhase,
            leftProb = lastLeft,
            rightProb = lastRight,
            faceDetected = lastFaceDetected,
            positionWarning = positionWarned,
            showTechnical = showTechnicalDetails,
            technicalLine = "L=${lastLeft?.let { "%.2f".format(it) } ?: "n/a"} " +
                "R=${lastRight?.let { "%.2f".format(it) } ?: "n/a"} " +
                "w=${lastPose.faceWidthPct?.let { "%.1f".format(it) } ?: "n/a"} " +
                "yaw=${lastPose.yaw?.let { "%.1f".format(it) } ?: "n/a"} " +
                "roll=${lastPose.roll?.let { "%.1f".format(it) } ?: "n/a"}",
            conditionResultSummary = result?.let { "Quality: ${it.quality}" }.orEmpty(),
            qualityLabel = result?.quality?.name.orEmpty(),
            ttsWarning = !ttsAvailable && !allowVisualOnly,
            sourceLabel = sourceLabel,
            completedConditionName = completedName,
            nextConditionName = nextName,
            caregiverTransitionHint = pendingNextCondition?.shortCaregiverChange().orEmpty(),
            analyseMessage = statusMessage,
            sessionIdShort = sessionId.replace("-", "").take(8)
        )
    }

    private fun resetRunKeepSetup() {
        conditionResults.clear()
        currentBuf = ConditionBuffers()
        conditionIndex = 0
        cycleIndex = 0
        baselineCaptured = false
        baselinePose = PoseSnapshot()
        flowPhase = GlassesCharFlowPhase.WaitFace
        phaseStartedMs = 0L
        stableSinceMs = 0L
        lastSpoken = ""
        lastSpokenMs = 0L
        positionWarned = false
        copyConfirmation = ""
        lastReport = null
        lastReportText = ""
        lastReportFilePath = null
        sessionPhoneWarnings = 0
        sessionPatientWarnings = 0
        positionConsistencyOk = true
        analyseStep = 0
        lastCompletedCondition = null
        pendingNextCondition = null
    }

    private fun resetAll() {
        resetRunKeepSetup()
        setup = SetupSnapshot()
        sessionId = ""
        testStartedMs = 0L
        testCompletedMs = 0L
    }
}
