package com.idworx.lisa.features.experiencepolish.communicationworkspace.metadata

object CommunicationWorkspaceMetadata {
    const val VERSION: String = "V1"
    const val PASS_TOKEN: String = "LISA_EXPERIENCE_PHASE_B_COMMUNICATION_WORKSPACE_V1_PASS"
    const val GRADLE_TASK: String = "validateLisaExperiencePhaseBCommunicationWorkspaceV1"

    const val GESTURE_LAYER_RULE: String =
        "Brain 1 decision gestures (L2/R2 choice, L1 R1 confirm, R1 L1 cancel) apply only to " +
            "decision flows. Workspace navigation uses its own global gestures."

    val WORKSPACE_NAVIGATION_GESTURES: Map<String, String> = mapOf(
        "L2_R0" to "Scroll up / previous page",
        "L0_R2" to "Scroll down / next page",
        "L1_R1" to "Select phrase or confirm adjustment",
        "L2_R2" to "Back / return to previous screen",
        "L4_R4" to "Open category menu",
        "L6_R0" to "Emergency (requires confirmation)"
    )

    val CAREGIVER_HELP_TOPICS: List<String> = listOf(
        "Phrase selection",
        "Category navigation",
        "Back behavior",
        "Emergency access",
        "Gesture legend visible on screen"
    )
}
