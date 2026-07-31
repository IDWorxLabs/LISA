package com.idworx.lisa.features.feedbackemail

import com.idworx.lisa.MenuDestinationPanelCommand

/**
 * LISA V1 caregiver-assisted Feedback email handoff.
 *
 * Stage 1 speaks a help request via production TTS.
 * Stage 2 opens the existing standards-based mailto handoff for a caregiver to press Send.
 * No backend, no silent send, no email-app / OEM package targeting.
 */
object FeedbackCaregiverAssistAuthority {
    const val SPOKEN_HELP_REQUEST: String = "Please send this email for me."

    /** L1 R1 — primary action on both stages. */
    val PRIMARY_SEQUENCE: Pair<Int, Int> = 1 to 1

    /** L2 R2 — return to Feedback form. */
    val BACK_SEQUENCE: Pair<Int, Int> = 2 to 2

    val stageCommands: List<MenuDestinationPanelCommand> = listOf(
        MenuDestinationPanelCommand.Select,
        MenuDestinationPanelCommand.Back,
        MenuDestinationPanelCommand.Emergency
    )

    fun isPrimarySequence(left: Int, right: Int): Boolean =
        left to right == PRIMARY_SEQUENCE

    fun isBackSequence(left: Int, right: Int): Boolean =
        left to right == BACK_SEQUENCE
}

sealed interface FeedbackCaregiverAssistStep {
    data object SpeakRequest : FeedbackCaregiverAssistStep
    data object OpenEmailApp : FeedbackCaregiverAssistStep
}
