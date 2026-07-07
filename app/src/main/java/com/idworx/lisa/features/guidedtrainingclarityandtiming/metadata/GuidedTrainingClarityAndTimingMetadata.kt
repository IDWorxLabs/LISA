package com.idworx.lisa.features.guidedtrainingclarityandtiming.metadata

object GuidedTrainingClarityAndTimingMetadata {
    const val PASS_TOKEN: String = "LISA_GUIDED_TRAINING_CLARITY_AND_TIMING_V1_PASS"
    const val GRADLE_TASK: String = "validateLisaGuidedTrainingClarityAndTimingV1"

    const val DESIGN_RULE: String =
        "Guided Mode / Guided Training has its own adjustable Response Time control, shown in the " +
            "same top control area as Sensitivity, sourced from TrainingPreferences.guidedResponseTimeSec " +
            "(default 5s — slower than the everyday 3s workspace default) and never hardcoded to a single " +
            "lesson. Every new blink during an active guided sequence restarts this settle timer instead of " +
            "an aggressive fixed timeout. After the first correct real-workspace navigation gesture, Lisa " +
            "gives a brief, rotating positive acknowledgement (spoken through the existing narration/TTS " +
            "system and shown on the floating lesson card) before the next instruction appears. The floating " +
            "lesson bubble remains, and while it has an active target the real workspace visually " +
            "de-emphasises every control that is not that target."
}
