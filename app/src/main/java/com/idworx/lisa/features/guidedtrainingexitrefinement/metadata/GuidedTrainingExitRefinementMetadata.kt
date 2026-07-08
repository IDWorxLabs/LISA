package com.idworx.lisa.features.guidedtrainingexitrefinement.metadata

object GuidedTrainingExitRefinementMetadata {
    const val PASS_TOKEN: String = "LISA_GUIDED_TRAINING_EXIT_REFINEMENT_V1_PASS"
    const val GRADLE_TASK: String = "validateLisaGuidedTrainingExitRefinementV1"

    const val TOUCH_INDEPENDENT_EXIT_RULE: String =
        "Guided Training ends with a dedicated Finish Training gesture — never a tap — that " +
            "completes the final lesson and returns the learner straight to normal communication."

    const val SHORT_CATEGORIES_RULE: String =
        "Choose Category is one of the most frequently used actions in LISA, so it uses the " +
            "shortest safe gesture available rather than the old 8-wink L4 R4."

    const val SIMPLIFIED_WORKSPACE_RULE: String =
        "The Communication Workspace shows only Vocabulary, the current category, the phrase " +
            "list, and the Navigation Panel — no competing instructional text block."
}
