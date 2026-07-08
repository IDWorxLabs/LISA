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

/**
 * Prompt-only decisions skip straight to [Brain1DecisionPhase.AwaitingConfirm] (see
 * [com.idworx.lisa.features.brain1interactionstandard.engine.Brain1DecisionEngine.beginAwaitingConfirm]) —
 * there is no alternate choice to make, so a cancel gesture must fully clear the decision (and,
 * for Emergency, its armed/awaiting-confirm banner) instead of re-prompting the same decision.
 * Contrast with the choice-based kinds (FirstLaunchGuidedLearning/FirstLaunchSkipWorkspace), whose
 * "choose again" cancel legitimately re-opens the original A/B choice.
 */
val Brain1DecisionKind.isPromptOnly: Boolean
    get() = this == Brain1DecisionKind.EmergencyMode ||
        this == Brain1DecisionKind.ResetLearningProgress ||
        this == Brain1DecisionKind.ReplayLearning ||
        this == Brain1DecisionKind.Recalibration

sealed class Brain1DecisionOutcome {
    data object None : Brain1DecisionOutcome()
    data object ChooseAgain : Brain1DecisionOutcome()
    data class Confirmed(val kind: Brain1DecisionKind) : Brain1DecisionOutcome()
    data class ChoiceDetected(val kind: Brain1DecisionKind, val label: String) : Brain1DecisionOutcome()
}
