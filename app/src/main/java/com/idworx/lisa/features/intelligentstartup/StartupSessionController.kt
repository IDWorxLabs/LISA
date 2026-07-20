package com.idworx.lisa.features.intelligentstartup

import com.idworx.lisa.LisaUserProfile
import com.idworx.lisa.features.intelligentstartup.authority.CalibrationCompatibilityAuthority
import com.idworx.lisa.features.intelligentstartup.authority.EyeCalibrationAuthority
import com.idworx.lisa.features.intelligentstartup.authority.StartupFlowAuthority
import com.idworx.lisa.features.intelligentstartup.authority.StartupProfileAuthority
import com.idworx.lisa.features.intelligentstartup.authority.StartupProfileResolution
import com.idworx.lisa.features.intelligentstartup.engine.CalibrationFrameSample
import com.idworx.lisa.features.intelligentstartup.engine.QuickEyeCalibrationEngine
import com.idworx.lisa.features.intelligentstartup.model.CalibrationCompatibilityLevel
import com.idworx.lisa.features.intelligentstartup.model.LiveCompatibilitySample
import com.idworx.lisa.features.intelligentstartup.model.ProfileEyeCalibration
import com.idworx.lisa.features.intelligentstartup.model.QuickCalibrationStep
import com.idworx.lisa.features.intelligentstartup.model.StartupEvent
import com.idworx.lisa.features.intelligentstartup.model.StartupFlowState
import com.idworx.lisa.features.intelligentstartup.model.StartupPhase

/**
 * Host-side controller for the intelligent startup flow (RC7D.34 + RC7D.35).
 */
class StartupSessionController(
    private val loadProfiles: () -> List<LisaUserProfile>,
    private val loadProfileCalibration: () -> ProfileEyeCalibration?,
    private val persistCalibration: (ProfileEyeCalibration) -> Unit,
    private val activateProfile: (profileId: String) -> Unit,
    private val createPrimaryUser: (name: String, languageLabel: String, levelLabel: String) -> String,
    private val nowMs: () -> Long = { System.currentTimeMillis() },
    private val onStateChanged: (StartupFlowState) -> Unit = {},
    private val onEyeControlActivated: () -> Unit = {},
    private val onStartupComplete: () -> Unit = {},
    private val scheduleReadyHandoff: (delayMs: Long, action: () -> Unit) -> Unit = { _, action -> action() },
    private val scheduleAutoRetry: (delayMs: Long, action: () -> Unit) -> Unit = { _, action -> action() }
) {
    private val engine = QuickEyeCalibrationEngine()
    private var lookNaturallyStartedMs: Long = 0L
    private var compatibilityStartedMs: Long = 0L
    private val compatibilityOpenness = mutableListOf<Float>()
    private val compatibilityDistance = mutableListOf<Float>()
    private val compatibilitySpacing = mutableListOf<Float>()

    var state: StartupFlowState = StartupFlowState()
        private set

    val isActive: Boolean get() = state.blocksMainUi
    val eyeControlEnabled: Boolean get() = state.eyeControlActive

    fun start() {
        engine.reset()
        compatibilityOpenness.clear()
        compatibilityDistance.clear()
        compatibilitySpacing.clear()
        lookNaturallyStartedMs = 0L
        compatibilityStartedMs = 0L
        state = StartupFlowState()
        publish()
    }

    fun onFacePresence(present: Boolean) {
        if (!isActive) return
        val previous = state.faceDetected
        dispatch(StartupEvent.FacePresenceChanged(present))
        if (state.phase == StartupPhase.FaceDetection && present && !previous) {
            dispatch(StartupEvent.BeginProfileResolution)
            resolveProfiles()
        }
        if (state.phase == StartupPhase.CalibrationFailure && present) {
            scheduleAutoRetry(900L) {
                if (state.phase == StartupPhase.CalibrationFailure) {
                    engine.reset()
                    dispatch(StartupEvent.RetryCalibration)
                }
            }
        }
    }

    fun onFrameSample(
        leftOpenness: Float,
        rightOpenness: Float,
        faceWidthNormalized: Float,
        eyeSpacingProxy: Float = faceWidthNormalized * 0.35f
    ) {
        if (!isActive) return
        when (state.phase) {
            StartupPhase.EvaluatingCompatibility -> {
                collectCompatibilitySample(leftOpenness, rightOpenness, faceWidthNormalized, eyeSpacingProxy)
            }
            StartupPhase.QuickCalibration -> {
                val step = state.calibrationStep
                val sample = CalibrationFrameSample(
                    leftOpenness = leftOpenness,
                    rightOpenness = rightOpenness,
                    faceWidthNormalized = faceWidthNormalized,
                    eyeSpacingProxy = eyeSpacingProxy,
                    timestampMs = nowMs()
                )
                val blinkCompleted = engine.onFrame(step, sample)
                if (step == QuickCalibrationStep.BlinkThreeTimes && blinkCompleted) {
                    state = state.copy(blinksCollected = engine.blinkCount())
                    publish()
                    if (engine.blinksReady()) {
                        dispatch(StartupEvent.AdvanceCalibrationStep)
                    }
                    return
                }
                if (step == QuickCalibrationStep.LookNaturally) {
                    if (lookNaturallyStartedMs == 0L) lookNaturallyStartedMs = nowMs()
                    val elapsed = nowMs() - lookNaturallyStartedMs
                    if (engine.baselineReady() && elapsed >= LOOK_NATURALLY_MIN_MS) {
                        lookNaturallyStartedMs = 0L
                        dispatch(StartupEvent.AdvanceCalibrationStep)
                    }
                }
            }
            else -> Unit
        }
    }

    fun onBothBlinkAccepted(closePeak: Float, durationMs: Long) {
        if (state.phase != StartupPhase.QuickCalibration) return
        if (state.calibrationStep != QuickCalibrationStep.BlinkThreeTimes) return
        val count = engine.onBothBlinkAccepted(state.calibrationStep, closePeak, durationMs)
        state = state.copy(blinksCollected = count)
        publish()
        if (engine.blinksReady()) {
            dispatch(StartupEvent.AdvanceCalibrationStep)
        }
    }

    fun onLeftWinkAccepted(closePeak: Float) {
        if (state.phase != StartupPhase.QuickCalibration) return
        if (state.calibrationStep != QuickCalibrationStep.LeftWinkTwice) return
        val count = engine.onLeftWinkAccepted(state.calibrationStep, closePeak)
        state = state.copy(leftWinksCollected = count)
        publish()
        if (engine.leftWinksReady()) {
            dispatch(StartupEvent.AdvanceCalibrationStep)
        }
    }

    fun onRightWinkAccepted(closePeak: Float) {
        if (state.phase != StartupPhase.QuickCalibration) return
        if (state.calibrationStep != QuickCalibrationStep.RightWinkTwice) return
        val count = engine.onRightWinkAccepted(state.calibrationStep, closePeak)
        state = state.copy(rightWinksCollected = count)
        publish()
        if (engine.rightWinksReady()) {
            finalizeCalibrationSuccess()
        }
    }

    fun setProfileSelectionIndex(index: Int) {
        if (state.phase != StartupPhase.ProfileSelection) return
        val clamped = StartupProfileAuthority.clampSelectionIndex(index, state.profileChoices.size)
        val delta = clamped - state.selectedProfileIndex
        if (delta == 0) return
        repeat(kotlin.math.abs(delta)) {
            moveProfileSelection(if (delta > 0) 1 else -1)
        }
    }

    fun moveProfileSelection(delta: Int) {
        if (state.phase != StartupPhase.ProfileSelection) return
        dispatch(
            if (delta < 0) StartupEvent.MoveProfileSelectionUp
            else StartupEvent.MoveProfileSelectionDown
        )
    }

    /** Profile picker Select — L1 R1 or shared touch select. */
    fun onProfileSelectGesture() {
        if (state.phase != StartupPhase.ProfileSelection) return
        val choice = state.selectedProfileChoice ?: return
        activateProfile(choice.id)
        dispatch(StartupEvent.SelectHighlightedProfile)
        beginCompatibilityCheck()
    }

    fun handleProfileSelectionSequence(left: Int, right: Int): Boolean {
        if (state.phase != StartupPhase.ProfileSelection) return false
        return when {
            left == 2 && right == 0 -> {
                moveProfileSelection(-1)
                true
            }
            left == 0 && right == 2 -> {
                moveProfileSelection(1)
                true
            }
            left == 1 && right == 1 -> {
                onProfileSelectGesture()
                true
            }
            else -> false
        }
    }

    fun updateCreatePrimaryDraft(
        name: String? = null,
        languageLabel: String? = null,
        levelLabel: String? = null
    ) {
        dispatch(
            StartupEvent.CreatePrimaryUserDraftChanged(
                name = name,
                languageLabel = languageLabel,
                levelLabel = levelLabel
            )
        )
    }

    fun confirmCreatePrimaryUser() {
        if (state.phase != StartupPhase.CreatePrimaryUser) return
        val id = createPrimaryUser(
            state.createNameDraft.trim().ifBlank { "Primary User" },
            state.createLanguageLabel,
            state.createLevelLabel
        )
        activateProfile(id)
        dispatch(StartupEvent.PrimaryUserCreated(id))
        engine.reset()
        lookNaturallyStartedMs = 0L
    }

    fun notifyCalibrationTimeoutFailure() {
        if (state.phase != StartupPhase.QuickCalibration) return
        if (state.calibrationStep == QuickCalibrationStep.CalibrationComplete) return
        dispatch(StartupEvent.CalibrationFailed)
        scheduleAutoRetry(1600L) {
            if (state.phase == StartupPhase.CalibrationFailure && state.faceDetected) {
                engine.reset()
                lookNaturallyStartedMs = 0L
                dispatch(StartupEvent.RetryCalibration)
            }
        }
    }

    private fun resolveProfiles() {
        when (val resolution = StartupProfileAuthority.resolve(loadProfiles())) {
            StartupProfileResolution.None ->
                dispatch(StartupEvent.ProfilesResolvedNone(defaultName = "Primary User"))
            is StartupProfileResolution.Single -> {
                activateProfile(resolution.profileId)
                dispatch(StartupEvent.ProfilesResolvedSingle(resolution.profileId))
                beginCompatibilityCheck()
            }
            is StartupProfileResolution.Multiple ->
                dispatch(StartupEvent.ProfilesResolvedMultiple(resolution.choices))
        }
    }

    private fun beginCompatibilityCheck() {
        compatibilityOpenness.clear()
        compatibilityDistance.clear()
        compatibilitySpacing.clear()
        compatibilityStartedMs = nowMs()
        dispatch(StartupEvent.BeginCompatibilityEvaluation)
        scheduleReadyHandoff(COMPATIBILITY_SAMPLE_MS) {
            if (state.phase == StartupPhase.EvaluatingCompatibility) {
                finalizeCompatibility()
            }
        }
    }

    private fun collectCompatibilitySample(
        leftOpenness: Float,
        rightOpenness: Float,
        faceWidthNormalized: Float,
        eyeSpacingProxy: Float
    ) {
        compatibilityOpenness += (leftOpenness + rightOpenness) / 2f
        if (faceWidthNormalized > 0f) compatibilityDistance += faceWidthNormalized
        if (eyeSpacingProxy > 0f) compatibilitySpacing += eyeSpacingProxy
    }

    private fun finalizeCompatibility() {
        val live = if (compatibilityOpenness.isEmpty()) {
            null
        } else {
            LiveCompatibilitySample(
                eyeOpennessBaseline = compatibilityOpenness.average().toFloat(),
                faceDistanceProxy = if (compatibilityDistance.isEmpty()) {
                    0.35f
                } else {
                    compatibilityDistance.average().toFloat()
                },
                eyeSpacingProxy = if (compatibilitySpacing.isEmpty()) {
                    0.35f
                } else {
                    compatibilitySpacing.average().toFloat()
                }
            )
        }
        val stored = loadProfileCalibration()
        val (level, score) = CalibrationCompatibilityAuthority.evaluate(stored, live, nowMs())
        if (stored != null) {
            val updated = CalibrationCompatibilityAuthority.appendHistory(
                stored,
                level,
                score,
                nowMs()
            )
            persistCalibration(updated)
            dispatch(StartupEvent.CalibrationCaptured(updated))
        }
        dispatch(StartupEvent.CompatibilityEvaluated(level))
        if (CalibrationCompatibilityAuthority.shouldSkipQuickCalibration(level)) {
            activateEyeControlAndScheduleWelcome()
        } else {
            engine.reset()
            lookNaturallyStartedMs = 0L
        }
    }

    private fun finalizeCalibrationSuccess() {
        val calibration = engine.buildCalibration(nowMs())
        if (calibration == null || !EyeCalibrationAuthority.thresholdsLookValid(calibration)) {
            notifyCalibrationTimeoutFailure()
            return
        }
        persistCalibration(calibration)
        dispatch(StartupEvent.CalibrationCaptured(calibration))
        dispatch(StartupEvent.AdvanceCalibrationStep)
        scheduleReadyHandoff(SUCCESS_DISPLAY_MS) {
            if (state.phase == StartupPhase.QuickCalibration &&
                state.calibrationStep == QuickCalibrationStep.CalibrationComplete
            ) {
                dispatch(StartupEvent.CalibrationSucceeded)
                activateEyeControlAndScheduleWelcome()
            }
        }
    }

    private fun activateEyeControlAndScheduleWelcome() {
        if (state.eyeControlActive) {
            onEyeControlActivated()
        }
        scheduleReadyHandoff(READY_HANDOFF_MS) {
            if (state.phase == StartupPhase.EyeTrackingReady) {
                dispatch(StartupEvent.AcknowledgeEyeTrackingReady)
                onStartupComplete()
            }
        }
    }

    private fun dispatch(event: StartupEvent) {
        val previousEye = state.eyeControlActive
        state = StartupFlowAuthority.reduce(state, event)
        if (!previousEye && state.eyeControlActive) {
            onEyeControlActivated()
        }
        publish()
    }

    private fun publish() = onStateChanged(state)

    companion object {
        const val LOOK_NATURALLY_MIN_MS = 1500L
        const val SUCCESS_DISPLAY_MS = 1200L
        const val READY_HANDOFF_MS = 900L
        const val COMPATIBILITY_SAMPLE_MS = 1200L
    }
}
