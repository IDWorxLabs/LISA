package com.idworx.lisa.features.sequencetimingpolicy.metadata

object SequenceTimingPolicyMetadata {
    const val PASS_TOKEN: String = "LISA_SEQUENCE_TIMING_POLICY_V1_PASS"
    const val GRADLE_TASK: String = "validateLisaSequenceTimingPolicyV1"

    const val TIMING_RULE: String =
        "One authoritative sequence timing policy (SequenceProcessingDelay) app-wide: default " +
            "response time is 5 seconds everywhere (workspace and Guided Training alike), every " +
            "blink/wink restarts the idle timer, and NO gesture — phrase, category, navigation, " +
            "confirm, cancel, or Emergency — is ever executed while the user is still adding " +
            "blinks/winks. A sequence is finalized exactly once, only after the full configured " +
            "idle window has elapsed with no further input. Hidden/off-screen gestures never " +
            "execute. No separate/duplicated hardcoded timeout constant is allowed to drift out of " +
            "sync with the selected response time."

    const val NO_EARLY_EXECUTION_RULE: String =
        "There is no early-execution / quick-resolve / \"unambiguous visible match can execute " +
            "early\" fast path anywhere in the app. QUICK_RESOLVE_IDLE_MS and " +
            "isQuicklyResolvableGesture() have been removed entirely. This applies uniformly to " +
            "phrases, category shortcuts, the reserved navigation codes (Previous, Next, Select, " +
            "Back, Categories, Finish Training), Confirm/Cancel, and Emergency — e.g. Stop (L2 R3) " +
            "and Yes (L2 R1) both simply wait for the full idle window, and Emergency (L6 R0) is " +
            "processed through the exact same idle-timeout gate as everything else rather than a " +
            "separate immediate short-circuit. Reliability is prioritized over speed."
}
