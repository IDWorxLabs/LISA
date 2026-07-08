package com.idworx.lisa.features.gestureshadowprevention.metadata

object GestureShadowPreventionMetadata {
    const val PASS_TOKEN: String = "LISA_GESTURE_SHADOW_PREVENTION_V1_PASS"
    const val GRADLE_TASK: String = "validateLisaGestureShadowPreventionV1"

    const val PRESERVE_NAVIGATION_RULE: String =
        "Categories keeps L3 R0 and Finish Training keeps L0 R3 — the shadowing fix reassigns " +
            "the colliding vocabulary phrases, never the navigation gestures they improved."

    const val PHRASE_REACHABILITY_RULE: String =
        "Every default-language vocabulary phrase — in every category — has its own gesture " +
            "that is free of every reserved navigation, system, quick-control, and emergency " +
            "sequence, so no phrase can ever become unreachable through blink input."

    const val GENERAL_PREVENTION_RULE: String =
        "GestureSequenceAuditEngine now cross-checks every workspace default phrase against " +
            "global navigation, system commands, and the emergency trigger before they can be " +
            "introduced, so a future navigation or system gesture change cannot silently shadow " +
            "a vocabulary phrase again."
}
