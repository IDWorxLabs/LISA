package com.idworx.lisa.features.guidedremoveredundanthelpertext.metadata

object GuidedRemoveRedundantHelperTextMetadata {
    const val PASS_TOKEN: String = "LISA_GUIDED_REMOVE_REDUNDANT_HELPER_TEXT_V1_PASS"
    const val GRADLE_TASK: String = "validateLisaGuidedRemoveRedundantHelperTextV1"

    const val REMOVAL_RULE: String =
        "Every Guided Learning communication and navigation lesson shows only the phrase/title and " +
            "the concise gesture instruction — the redundant \"When you're ready, ...\" helper sentence " +
            "that repeated the gesture is removed system-wide, with no replacement sentence; the space " +
            "is simply left empty and the layout rebalances naturally."
}
