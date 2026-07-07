package com.idworx.lisa.features.guidedtraininglessonfocus.metadata

object GuidedTrainingLessonFocusMetadata {
    const val PASS_TOKEN: String = "LISA_GUIDED_TRAINING_LESSON_FOCUS_V1_PASS"
    const val GRADLE_TASK: String = "validateLisaGuidedTrainingLessonFocusV1"

    const val DESIGN_RULE: String =
        "Guided Training teaches one real workspace target at a time. Every navigation lesson is " +
            "gated twice: the existing coarse gesture-kind gate (acceptedByCurrentNavigationLesson), " +
            "then a row-level GuidedTrainingFocusPolicy check for lessons with several visible " +
            "candidates (Select Category, Select Phrase) so only the highlighted row can act — by " +
            "blink AND by touch. Dimmed rows are functionally disabled, not just faded. Any blocked " +
            "gesture/action shows a brief red 'wrong sequence' acknowledgement on the floating lesson " +
            "card, resets the active blink sequence, and never advances progress or speaks a phrase. " +
            "None of this applies outside Guided Training's NavigationLesson phase."
}
