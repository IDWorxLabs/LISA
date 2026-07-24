package com.idworx.lisa.features.universalsequenceexecution

import com.idworx.lisa.GuidedModeNavigation
import com.idworx.lisa.features.brain1interactionstandard.model.UniversalInteractionGestures
import com.idworx.lisa.features.intelligentstartup.authority.WelcomeEyeNavigationAuthority

/**
 * RC8.12 — Guided Learning readiness Back / Continue share one action authority for touch and blink.
 */
object GuidedReadinessSequenceAuthority {

    enum class Action {
        Back,
        Continue
    }

    val BACK_LEFT: Int = GuidedModeNavigation.BACK_LEFT
    val BACK_RIGHT: Int = GuidedModeNavigation.BACK_RIGHT
    val CONTINUE_LEFT: Int = UniversalInteractionGestures.CONFIRM_LEFT
    val CONTINUE_RIGHT: Int = UniversalInteractionGestures.CONFIRM_RIGHT

    fun backSequenceLabel(): String =
        UniversalSequenceExecutionAuthority.sequenceLabel(BACK_LEFT, BACK_RIGHT)

    fun continueSequenceLabel(): String =
        UniversalSequenceExecutionAuthority.sequenceLabel(CONTINUE_LEFT, CONTINUE_RIGHT)

    fun resolve(left: Int, right: Int, blinkOrder: List<Boolean> = emptyList()): Action? = when {
        UniversalSequenceExecutionAuthority.matches(left, right, BACK_LEFT, BACK_RIGHT) ->
            Action.Back
        UniversalInteractionGestures.isConfirm(left, right, blinkOrder) ||
            UniversalSequenceExecutionAuthority.matches(left, right, CONTINUE_LEFT, CONTINUE_RIGHT) ->
            Action.Continue
        else -> null
    }

    fun invoke(
        action: Action,
        onBack: () -> Unit,
        onContinue: () -> Unit
    ) {
        UniversalSequenceExecutionAuthority.runShared {
            when (action) {
                Action.Back -> onBack()
                Action.Continue -> onContinue()
            }
        }
    }

    fun invokeFromBlink(
        left: Int,
        right: Int,
        blinkOrder: List<Boolean> = emptyList(),
        onBack: () -> Unit,
        onContinue: () -> Unit
    ): Boolean {
        val action = resolve(left, right, blinkOrder) ?: return false
        invoke(action, onBack, onContinue)
        return true
    }
}

/** Catalog constants for Welcome destination / introduction sequences (already Brain1-wired). */
object WelcomeSequenceCatalog {
    val CONTINUE_LEFT: Int = WelcomeEyeNavigationAuthority.continueLeft
    val CONTINUE_RIGHT: Int = WelcomeEyeNavigationAuthority.continueRight
    val START_GUIDED_LEFT: Int = WelcomeEyeNavigationAuthority.startGuidedLearningLeft
    val START_GUIDED_RIGHT: Int = WelcomeEyeNavigationAuthority.startGuidedLearningRight
    val SKIP_LEFT: Int = WelcomeEyeNavigationAuthority.skipToCommunicationLeft
    val SKIP_RIGHT: Int = WelcomeEyeNavigationAuthority.skipToCommunicationRight
    val BACK_LEFT: Int = WelcomeEyeNavigationAuthority.backLeft
    val BACK_RIGHT: Int = WelcomeEyeNavigationAuthority.backRight
}

object MenuSequenceCatalog {
    val SELECT_LEFT: Int = GuidedModeNavigation.SELECT_LEFT
    val SELECT_RIGHT: Int = GuidedModeNavigation.SELECT_RIGHT
    val BACK_LEFT: Int = GuidedModeNavigation.BACK_LEFT
    val BACK_RIGHT: Int = GuidedModeNavigation.BACK_RIGHT
}

object CategorySequenceCatalog {
    val SELECT_LEFT: Int = GuidedModeNavigation.SELECT_LEFT
    val SELECT_RIGHT: Int = GuidedModeNavigation.SELECT_RIGHT
    val BACK_LEFT: Int = GuidedModeNavigation.BACK_LEFT
    val BACK_RIGHT: Int = GuidedModeNavigation.BACK_RIGHT
}

object SettingsRecalibrationRetrySequenceAuthority {
    val RETRY_LEFT: Int = GuidedModeNavigation.SELECT_LEFT
    val RETRY_RIGHT: Int = GuidedModeNavigation.SELECT_RIGHT

    fun sequenceLabel(): String =
        UniversalSequenceExecutionAuthority.sequenceLabel(RETRY_LEFT, RETRY_RIGHT)

    fun matches(left: Int, right: Int): Boolean =
        UniversalSequenceExecutionAuthority.matches(left, right, RETRY_LEFT, RETRY_RIGHT)

    fun invokeRetry(onRetry: () -> Unit) {
        UniversalSequenceExecutionAuthority.runShared(onRetry)
    }
}
