package com.idworx.lisa.features.guidedtraininggesturemismatch.metadata

object GuidedTrainingGestureMismatchMetadata {
    const val PASS_TOKEN: String = "LISA_GUIDED_TRAINING_GESTURE_MISMATCH_V1_PASS"
    const val GRADLE_TASK: String = "validateLisaGuidedTrainingGestureMismatchV1"

    const val DESIGN_RULE: String =
        "Guided Training never has its own copy of a gesture. Every lesson's displayed gesture, " +
            "accepted gesture, and validation target are all read from the exact same function the " +
            "real workspace control itself calls: category lessons from " +
            "GuidedCategoryShortcuts.sequenceLabelForCategory/categoryIndexForGesture (the same " +
            "lookup GuidedCategoryMenuRow renders and GuidedNavigationController.processCategoryMenuGesture " +
            "executes), phrase lessons from the actual highlighted GuidedVocabularyEntry's own " +
            "sequenceLabel/left/right, and every navigation-panel lesson (Open Categories, Back, " +
            "Next, Previous, Emergency) from the same GuidedModeNavigation/EMERGENCY_* constants " +
            "GuidedNavigationPanelSpec.panelActions renders on the real buttons. Nothing is ever a " +
            "second hardcoded string that could silently drift out of sync."
}
