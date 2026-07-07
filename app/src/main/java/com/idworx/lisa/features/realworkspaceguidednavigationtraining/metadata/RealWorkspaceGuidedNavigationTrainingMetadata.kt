package com.idworx.lisa.features.realworkspaceguidednavigationtraining.metadata

object RealWorkspaceGuidedNavigationTrainingMetadata {
    const val PASS_TOKEN: String = "LISA_REAL_WORKSPACE_GUIDED_NAVIGATION_TRAINING_V1_PASS"
    const val GRADLE_TASK: String = "validateLisaRealWorkspaceGuidedNavigationTrainingV1"

    const val DESIGN_RULE: String =
        "After the 15 phrase lessons, Guided Learning's 8 navigation lessons run inside the real " +
            "Communication Workspace (GuidedWorkspaceMode.GUIDED_TRAINING) instead of the old blank " +
            "NavigationLessonScreen. Each lesson highlights exactly one real control with a subtle " +
            "blue outline, accepts only that lesson's target gesture, and ignores every other " +
            "workspace gesture until the correct action is performed. When all 8 lessons are done the " +
            "workspace returns to GuidedWorkspaceMode.NORMAL — no Brain 2, no narration, no cloud, " +
            "phrase-only speech unchanged."
}
