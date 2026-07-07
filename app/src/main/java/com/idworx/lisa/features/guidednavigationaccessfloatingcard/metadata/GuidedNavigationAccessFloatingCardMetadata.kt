package com.idworx.lisa.features.guidednavigationaccessfloatingcard.metadata

object GuidedNavigationAccessFloatingCardMetadata {
    const val PASS_TOKEN: String = "LISA_GUIDED_NAVIGATION_ACCESS_AND_FLOATING_CARD_V1_PASS"
    const val GRADLE_TASK: String = "validateLisaGuidedNavigationAccessAndFloatingCardV1"

    const val DESIGN_RULE: String =
        "The Welcome screen exposes a small caregiver/testing-only 'Skip to Navigation Training' " +
            "link alongside 'Start Guided Learning' and 'Skip to Communication Workspace'. It jumps " +
            "straight to Lesson 16 of 23 inside the real Communication Workspace in " +
            "GuidedWorkspaceMode.GUIDED_TRAINING, bypassing all 15 phrase lessons. The navigation " +
            "lesson card floats above the bottom Menu/Reset row — never at the top behind the " +
            "Listening/Watching-your-eyes banner — and docks on whichever side keeps the highlighted " +
            "real control uncovered. No narration, no Brain 2, no cloud, and the real workspace " +
            "layout is unchanged."
}
