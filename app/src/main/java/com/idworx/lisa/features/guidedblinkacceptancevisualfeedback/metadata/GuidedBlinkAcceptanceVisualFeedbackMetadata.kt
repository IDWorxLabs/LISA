package com.idworx.lisa.features.guidedblinkacceptancevisualfeedback.metadata

object GuidedBlinkAcceptanceVisualFeedbackMetadata {
    const val PASS_TOKEN: String = "LISA_GUIDED_BLINK_ACCEPTANCE_VISUAL_FEEDBACK_V1_PASS"
    const val GRADLE_TASK: String = "validateLisaGuidedBlinkAcceptanceVisualFeedbackV1"

    const val FEEDBACK_RULE: String =
        "Every accepted blink during Guided Learning gives calm, immediate visual confirmation: " +
            "the detected eye's counter briefly pulses (scale + brighten to LisaBlue, ~250-300ms), " +
            "its circular indicator flashes, and a \"\u2713 Left/Right blink detected\" message fades " +
            "in below the eye status panel for ~600ms before fading out. Each accepted blink in a " +
            "sequence retriggers all three independently; wrong-eye blinks and timeout resets never do."
}
