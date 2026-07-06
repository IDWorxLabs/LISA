package com.idworx.lisa.features.brain1interactionstandard.model

/**
 * Universal Brain 1 interaction gestures — two-blink minimum for commands.
 * Confirm/cancel require blink order (L1 R1 vs R1 L1).
 */
object UniversalInteractionGestures {

    const val MIN_COMMAND_BLINKS: Int = 2

    /** L2 — Option A / Primary choice */
    const val OPTION_A_LEFT: Int = 2
    const val OPTION_A_RIGHT: Int = 0

    /** R2 — Option B / Secondary choice */
    const val OPTION_B_LEFT: Int = 0
    const val OPTION_B_RIGHT: Int = 2

    /** L1 R1 — Confirm / Yes / Proceed (left blink first) */
    const val CONFIRM_LEFT: Int = 1
    const val CONFIRM_RIGHT: Int = 1

    fun isOptionA(left: Int, right: Int): Boolean =
        left == OPTION_A_LEFT && right == OPTION_A_RIGHT

    fun isOptionB(left: Int, right: Int): Boolean =
        left == OPTION_B_LEFT && right == OPTION_B_RIGHT

    fun isConfirm(left: Int, right: Int, blinkOrder: List<Boolean>): Boolean =
        left == CONFIRM_LEFT && right == CONFIRM_RIGHT &&
            BlinkSequenceOrder.isLeftThenRight(blinkOrder)

    fun isCancel(left: Int, right: Int, blinkOrder: List<Boolean>): Boolean =
        left == CONFIRM_LEFT && right == CONFIRM_RIGHT &&
            BlinkSequenceOrder.isRightThenLeft(blinkOrder)

    fun totalBlinks(left: Int, right: Int): Int = left + right

    fun isValidCommand(left: Int, right: Int): Boolean =
        totalBlinks(left, right) >= MIN_COMMAND_BLINKS

    fun difficultyLevel(left: Int, right: Int): Int = when (totalBlinks(left, right)) {
        2 -> 1
        3 -> 2
        4 -> 3
        5 -> 4
        else -> 5
    }

    fun label(left: Int, right: Int): String = "L$left R$right"
}
