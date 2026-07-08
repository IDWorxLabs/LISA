package com.idworx.lisa.features.sequencetimingpolicy.metadata

object SequenceTimingPolicyMetadata {
    const val PASS_TOKEN: String = "LISA_SEQUENCE_TIMING_POLICY_V1_PASS"
    const val GRADLE_TASK: String = "validateLisaSequenceTimingPolicyV1"

    const val TIMING_RULE: String =
        "One authoritative sequence timing policy (SequenceProcessingDelay) app-wide: default " +
            "response time is 5 seconds everywhere (workspace and Guided Training alike), every " +
            "blink/wink restarts the idle timer, and a sequence is only ever finalized when its " +
            "completed gesture is unambiguous against the currently visible gesture set, or the full " +
            "configured idle window has elapsed with no further input. Hidden/off-screen gestures " +
            "never affect ambiguity. No separate/duplicated hardcoded timeout constant is allowed to " +
            "drift out of sync with the selected response time."

    const val AMBIGUITY_RULE: String =
        "A visible gesture may execute immediately (\"quick resolve\") only when it is unambiguous: " +
            "no other currently visible gesture could still be reached by continuing to blink. This " +
            "applies uniformly to phrases, category shortcuts, and the two-eye reserved navigation " +
            "codes (Select L1 R1, Back L2 R2) — e.g. Select must wait rather than fire early when a " +
            "longer visible phrase/category shortcut like Yes (L2 R1) or Stop (L2 R3) numerically " +
            "extends it. Single-eye reserved codes (Previous L2 R0, Next L0 R2, Categories L3 R0, " +
            "Finish Training L0 R3) are structurally distinct from all two-eye vocabulary content and " +
            "keep resolving immediately. Emergency is dispatched before this gate entirely, so it is " +
            "never delayed or cut off."
}
