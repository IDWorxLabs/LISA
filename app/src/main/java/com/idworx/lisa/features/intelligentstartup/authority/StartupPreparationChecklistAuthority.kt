package com.idworx.lisa.features.intelligentstartup.authority

import com.idworx.lisa.features.intelligentstartup.model.StartupFlowState
import com.idworx.lisa.features.intelligentstartup.model.StartupPhase

/** The four preparation steps LISA reports while starting up, in display order. */
enum class PreparationChecklistStep {
    EyeTracking,
    LoadingProfile,
    PreparingCommunication,
    CalibrationReady
}

/**
 * Single source of truth for the startup preparation checklist.
 *
 * Every row is derived from real [StartupFlowState] readiness — nothing is faked ahead of the
 * production state machine. The last two rows read the Preparing-phase completion flags so they
 * can appear on the Preparing screen itself, and fall back to the later phases for safety.
 */
object StartupPreparationChecklistAuthority {

    val orderedSteps: List<PreparationChecklistStep> = listOf(
        PreparationChecklistStep.EyeTracking,
        PreparationChecklistStep.LoadingProfile,
        PreparationChecklistStep.PreparingCommunication,
        PreparationChecklistStep.CalibrationReady
    )

    fun completedSteps(state: StartupFlowState): List<PreparationChecklistStep> =
        completedSteps(
            phase = state.phase,
            faceDetected = state.faceDetected,
            communicationPrepared = state.communicationPrepared,
            calibrationDecisionReady = state.calibrationDecisionReady
        )

    fun completedSteps(
        phase: StartupPhase,
        faceDetected: Boolean,
        communicationPrepared: Boolean,
        calibrationDecisionReady: Boolean
    ): List<PreparationChecklistStep> = orderedSteps.filter { step ->
        when (step) {
            PreparationChecklistStep.EyeTracking -> faceDetected
            PreparationChecklistStep.LoadingProfile -> profileLoaded(phase)
            PreparationChecklistStep.PreparingCommunication ->
                communicationPrepared || phase.isPastPreparing()
            PreparationChecklistStep.CalibrationReady ->
                calibrationDecisionReady ||
                    phase == StartupPhase.EyeTrackingReady ||
                    phase == StartupPhase.Complete
        }
    }

    private fun profileLoaded(phase: StartupPhase): Boolean = when (phase) {
        StartupPhase.GlassesQuestion,
        StartupPhase.GlassesGuidance,
        StartupPhase.FaceDetection,
        StartupPhase.ProfileResolution -> false
        else -> true
    }

    private fun StartupPhase.isPastPreparing(): Boolean = when (this) {
        StartupPhase.QuickCalibration,
        StartupPhase.CalibrationFailure,
        StartupPhase.EyeTrackingReady,
        StartupPhase.Complete -> true
        else -> false
    }
}
