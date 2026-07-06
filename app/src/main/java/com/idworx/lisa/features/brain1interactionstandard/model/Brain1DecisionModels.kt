package com.idworx.lisa.features.brain1interactionstandard.model

enum class Brain1DecisionKind {
    None,
    FirstLaunchGuidedLearning,
    FirstLaunchSkipWorkspace,
    EmergencyMode,
    ResetLearningProgress,
    ReplayLearning,
    Recalibration
}

enum class Brain1DecisionPhase {
    Idle,
    ChoiceMade,
    AwaitingConfirm
}

data class Brain1DecisionState(
    val kind: Brain1DecisionKind = Brain1DecisionKind.None,
    val phase: Brain1DecisionPhase = Brain1DecisionPhase.Idle,
    val choiceLabel: String? = null
) {
    val isActive: Boolean get() = kind != Brain1DecisionKind.None

    fun clear(): Brain1DecisionState = Brain1DecisionState()
}

sealed class Brain1DecisionOutcome {
    data object None : Brain1DecisionOutcome()
    data object ChooseAgain : Brain1DecisionOutcome()
    data class Confirmed(val kind: Brain1DecisionKind) : Brain1DecisionOutcome()
    data class ChoiceDetected(val kind: Brain1DecisionKind, val label: String) : Brain1DecisionOutcome()
}
