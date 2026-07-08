package com.idworx.lisa.features.experiencepolish.communicationworkspace.metadata

import com.idworx.lisa.GuidedModeNavigation

object CommunicationWorkspaceMetadata {
    const val VERSION: String = "V1"
    const val PASS_TOKEN: String = "LISA_EXPERIENCE_PHASE_B_COMMUNICATION_WORKSPACE_V1_PASS"
    const val GRADLE_TASK: String = "validateLisaExperiencePhaseBCommunicationWorkspaceV1"

    const val GESTURE_LAYER_RULE: String =
        "Brain 1 decision gestures (L2/R2 choice, L1 R1 confirm, R1 L1 cancel) apply only to " +
            "decision flows. Workspace navigation uses its own global gestures."

    /** Keys are derived from [GuidedModeNavigation] so this map can never drift from the real gestures. */
    val WORKSPACE_NAVIGATION_GESTURES: Map<String, String> = mapOf(
        "L2_R0" to "Scroll up / previous page",
        "L0_R2" to "Scroll down / next page",
        "L1_R1" to "Select phrase or confirm adjustment",
        "L2_R2" to "Back / return to previous screen",
        "L${GuidedModeNavigation.CATEGORIES_LEFT}_R${GuidedModeNavigation.CATEGORIES_RIGHT}" to "Open category menu",
        "L6_R0" to "Emergency (requires confirmation)",
        "L${GuidedModeNavigation.FINISH_TRAINING_LEFT}_R${GuidedModeNavigation.FINISH_TRAINING_RIGHT}" to
            "Finish Guided Training and start communicating"
    )

    val CAREGIVER_HELP_TOPICS: List<String> = listOf(
        "Phrase selection",
        "Category navigation",
        "Back behavior",
        "Emergency access",
        "Contextual gesture support on the Accessibility panel"
    )
}
