package com.idworx.lisa.features.intelligentstartup.authority

import com.idworx.lisa.GuidedModeNavigation
import com.idworx.lisa.features.brain1interactionstandard.model.UniversalInteractionGestures
import com.idworx.lisa.formatWinkSequenceShort

/** RC7D.37 — Welcome is two explicit stages after Eye Tracking Ready. */
enum class WelcomeStage {
    BlinkSequenceIntroduction,
    DestinationSelection
}

/** Stage-scoped Welcome actions. Does not invent sequences. */
sealed class WelcomeStageAction {
    data object ContinueToDestinationSelection : WelcomeStageAction()
    data object BackToIntroduction : WelcomeStageAction()
    data object StartGuidedLearning : WelcomeStageAction()
    data object SkipToCommunication : WelcomeStageAction()
    data object None : WelcomeStageAction()
}

/**
 * Canonical Welcome eye sequences and stage-scoped routing.
 * Touch labels and blink routing must stay identical.
 *
 * Sequences (unchanged constants):
 * - L1 R1 — Continue (introduction only)
 * - L2 R0 — Start Guided Learning (destination only)
 * - L0 R2 — Skip to Communication (destination only)
 * - L2 R2 — Back to introduction (destination only)
 */
object WelcomeEyeNavigationAuthority {
    val startGuidedLearningLeft: Int = UniversalInteractionGestures.OPTION_A_LEFT
    val startGuidedLearningRight: Int = UniversalInteractionGestures.OPTION_A_RIGHT
    val skipToCommunicationLeft: Int = UniversalInteractionGestures.OPTION_B_LEFT
    val skipToCommunicationRight: Int = UniversalInteractionGestures.OPTION_B_RIGHT

    val continueLeft: Int = UniversalInteractionGestures.CONFIRM_LEFT
    val continueRight: Int = UniversalInteractionGestures.CONFIRM_RIGHT

    val backLeft: Int = GuidedModeNavigation.BACK_LEFT
    val backRight: Int = GuidedModeNavigation.BACK_RIGHT

    fun initialStage(): WelcomeStage = WelcomeStage.BlinkSequenceIntroduction

    fun startGuidedLearningSequenceLabel(): String =
        formatWinkSequenceShort(startGuidedLearningLeft, startGuidedLearningRight)

    fun skipToCommunicationSequenceLabel(): String =
        formatWinkSequenceShort(skipToCommunicationLeft, skipToCommunicationRight)

    fun continueSequenceLabel(): String =
        formatWinkSequenceShort(continueLeft, continueRight)

    fun backSequenceLabel(): String =
        formatWinkSequenceShort(backLeft, backRight)

    fun startGuidedLearningInstruction(): String = "Blink left twice"

    fun skipToCommunicationInstruction(): String = "Blink right twice"

    fun continueInstruction(): String = "Blink once with each eye"

    fun backInstruction(): String = "Blink left twice and right twice"

    fun howToChooseTitle(): String = "How to choose an option"

    fun notationExplanationBody(): String =
        "L means your left eye.\n" +
            "R means your right eye.\n" +
            "The number tells you how many times to blink."

    fun notationCompleteLeftExample(): String =
        "L2 R0 means blink your left eye twice and do not blink your right eye."

    fun notationCompleteRightExample(): String =
        "L0 R2 means do not blink your left eye and blink your right eye twice."

    /** @deprecated RC7D.37 — standalone L2/R2 lines removed; use complete sequence examples. */
    fun notationLeftExample(): String = notationCompleteLeftExample()

    /** @deprecated RC7D.37 — standalone L2/R2 lines removed; use complete sequence examples. */
    fun notationRightExample(): String = notationCompleteRightExample()

    fun notationZeroLeftExample(): String = notationCompleteLeftExample()

    fun notationZeroRightExample(): String = notationCompleteRightExample()

    fun destinationSelectionTitle(): String = "Choose where to begin"

    fun destinationSelectionSubtitle(): String = "Use your eyes or touch an option."

    fun continueButtonLabel(): String = "Continue"

    /** RC7D.40 — human-readable instruction plus technical sequence on one line. */
    fun combinedActionHint(instruction: String, sequenceLabel: String): String =
        "$instruction · $sequenceLabel"

    fun continueContentDescription(): String =
        "${continueButtonLabel()}, blink once with each eye, ${continueSequenceLabel()}"

    fun isStartGuidedLearning(left: Int, right: Int): Boolean =
        UniversalInteractionGestures.isOptionA(left, right)

    fun isSkipToCommunication(left: Int, right: Int): Boolean =
        UniversalInteractionGestures.isOptionB(left, right)

    fun isContinue(left: Int, right: Int, blinkOrder: List<Boolean>): Boolean =
        UniversalInteractionGestures.isConfirm(left, right, blinkOrder)

    fun isContinueCounts(left: Int, right: Int): Boolean =
        left == continueLeft && right == continueRight

    fun isBack(left: Int, right: Int): Boolean =
        GuidedModeNavigation.isBackSequence(left, right)

    /**
     * Resolve a completed sequence for the active Welcome stage.
     * Introduction never starts Guided Learning / Communication.
     * Destination never treats L1 R1 as Continue.
     */
    fun resolve(
        stage: WelcomeStage,
        left: Int,
        right: Int,
        blinkOrder: List<Boolean> = emptyList()
    ): WelcomeStageAction = when (stage) {
        WelcomeStage.BlinkSequenceIntroduction -> when {
            isContinue(left, right, blinkOrder) -> WelcomeStageAction.ContinueToDestinationSelection
            // Counts-only fallback for touch-mirrored tests without blink order samples.
            blinkOrder.isEmpty() && isContinueCounts(left, right) ->
                WelcomeStageAction.ContinueToDestinationSelection
            else -> WelcomeStageAction.None
        }
        WelcomeStage.DestinationSelection -> when {
            isBack(left, right) -> WelcomeStageAction.BackToIntroduction
            isStartGuidedLearning(left, right) -> WelcomeStageAction.StartGuidedLearning
            isSkipToCommunication(left, right) -> WelcomeStageAction.SkipToCommunication
            else -> WelcomeStageAction.None
        }
    }

    /** After Continue / Back, the completed command must not leak into the next stage. */
    fun stageAfterAction(stage: WelcomeStage, action: WelcomeStageAction): WelcomeStage = when (action) {
        WelcomeStageAction.ContinueToDestinationSelection -> WelcomeStage.DestinationSelection
        WelcomeStageAction.BackToIntroduction -> WelcomeStage.BlinkSequenceIntroduction
        else -> stage
    }

    fun consumesCommand(action: WelcomeStageAction): Boolean =
        action != WelcomeStageAction.None
}
