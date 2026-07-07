package com.idworx.lisa.features.guidedpreferencesgestureconsistency.metadata

object GuidedPreferencesGestureConsistencyMetadata {
    const val PASS_TOKEN: String = "LISA_GUIDED_PREFERENCES_GESTURE_CONSISTENCY_V1_PASS"
    const val GRADLE_TASK: String = "validateLisaGuidedPreferencesGestureConsistencyV1"

    const val DESIGN_RULE: String =
        "No screen in LISA ever owns a second copy of a gesture. The Preferences / Adjustment " +
            "panel (Decrease, Increase, Save, Cancel, Categories, Emergency) and the Quick " +
            "Controls overlay (Sensitivity −/+, Repeat Last, Pause/Resume, Practice Mode, Close) " +
            "both render their gesture badges by calling formatWinkSequenceShort(...) directly on " +
            "the same GuidedModeNavigation / EMERGENCY_* constants and LisaSystemLanguage." +
            "quickControlCommands entries that GuidedNavigationController.processPreferencesAdjustmentGesture " +
            "and LisaSystemLanguage.resolveQuickControlCommand actually check against — never a " +
            "second hardcoded literal that could silently drift out of sync."
}
