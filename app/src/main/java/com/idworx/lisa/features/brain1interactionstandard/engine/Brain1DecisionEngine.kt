package com.idworx.lisa.features.brain1interactionstandard.engine

import com.idworx.lisa.features.brain1interactionstandard.model.Brain1DecisionKind
import com.idworx.lisa.features.brain1interactionstandard.model.Brain1DecisionOutcome
import com.idworx.lisa.features.brain1interactionstandard.model.Brain1DecisionPhase
import com.idworx.lisa.features.brain1interactionstandard.model.Brain1DecisionState
import com.idworx.lisa.features.brain1interactionstandard.model.UniversalInteractionGestures

/**
 * Universal decision model: Choose → Lisa repeats → Confirm → Execute.
 */
object Brain1DecisionEngine {

    fun handleSequence(
        state: Brain1DecisionState,
        left: Int,
        right: Int,
        blinkOrder: List<Boolean> = emptyList()
    ): Pair<Brain1DecisionState, Brain1DecisionOutcome> {
        if (!UniversalInteractionGestures.isValidCommand(left, right)) {
            return state to Brain1DecisionOutcome.None
        }

        return when (state.phase) {
            Brain1DecisionPhase.Idle -> handleChoice(state, left, right)
            Brain1DecisionPhase.AwaitingConfirm -> handleConfirm(state, left, right, blinkOrder)
            Brain1DecisionPhase.ChoiceMade -> handleConfirm(state, left, right, blinkOrder)
        }
    }

    fun beginDecision(kind: Brain1DecisionKind): Brain1DecisionState =
        Brain1DecisionState(kind = kind, phase = Brain1DecisionPhase.Idle)

    /** Prompt-only decisions (reset, replay, recalibration, emergency) skip straight to confirm. */
    fun beginAwaitingConfirm(kind: Brain1DecisionKind): Brain1DecisionState =
        Brain1DecisionState(
            kind = kind,
            phase = Brain1DecisionPhase.AwaitingConfirm,
            choiceLabel = choiceLabel(kind)
        )

    private fun handleChoice(
        state: Brain1DecisionState,
        left: Int,
        right: Int
    ): Pair<Brain1DecisionState, Brain1DecisionOutcome> {
        val detected = when {
            UniversalInteractionGestures.isOptionA(left, right) ->
                when (state.kind) {
                    Brain1DecisionKind.FirstLaunchGuidedLearning,
                    Brain1DecisionKind.FirstLaunchSkipWorkspace ->
                        Brain1DecisionKind.FirstLaunchGuidedLearning
                    Brain1DecisionKind.EmergencyMode -> Brain1DecisionKind.EmergencyMode
                    Brain1DecisionKind.ResetLearningProgress -> Brain1DecisionKind.ResetLearningProgress
                    Brain1DecisionKind.ReplayLearning -> Brain1DecisionKind.ReplayLearning
                    Brain1DecisionKind.Recalibration -> Brain1DecisionKind.Recalibration
                    Brain1DecisionKind.None -> Brain1DecisionKind.None
                }
            UniversalInteractionGestures.isOptionB(left, right) ->
                when (state.kind) {
                    Brain1DecisionKind.FirstLaunchGuidedLearning,
                    Brain1DecisionKind.FirstLaunchSkipWorkspace ->
                        Brain1DecisionKind.FirstLaunchSkipWorkspace
                    else -> state.kind
                }
            else -> null
        }

        if (detected == null || detected == Brain1DecisionKind.None) {
            return state to Brain1DecisionOutcome.None
        }

        val label = choiceLabel(detected)
        val next = state.copy(
            kind = detected,
            phase = Brain1DecisionPhase.AwaitingConfirm,
            choiceLabel = label
        )
        return next to Brain1DecisionOutcome.ChoiceDetected(detected, label)
    }

    private fun handleConfirm(
        state: Brain1DecisionState,
        left: Int,
        right: Int,
        blinkOrder: List<Boolean>
    ): Pair<Brain1DecisionState, Brain1DecisionOutcome> = when {
        UniversalInteractionGestures.isConfirm(left, right, blinkOrder) ->
            state.clear() to Brain1DecisionOutcome.Confirmed(state.kind)
        UniversalInteractionGestures.isCancel(left, right, blinkOrder) ->
            state.copy(phase = Brain1DecisionPhase.Idle, choiceLabel = null) to
                Brain1DecisionOutcome.ChooseAgain
        else -> state to Brain1DecisionOutcome.None
    }

    fun choiceLabel(kind: Brain1DecisionKind): String = when (kind) {
        Brain1DecisionKind.FirstLaunchGuidedLearning -> "Guided Learning"
        Brain1DecisionKind.FirstLaunchSkipWorkspace -> "Communication Workspace"
        Brain1DecisionKind.EmergencyMode -> "Emergency Mode"
        Brain1DecisionKind.ResetLearningProgress -> "Reset Learning Progress"
        Brain1DecisionKind.ReplayLearning -> "Replay Learning"
        Brain1DecisionKind.Recalibration -> "Recalibration"
        Brain1DecisionKind.None -> ""
    }
}
