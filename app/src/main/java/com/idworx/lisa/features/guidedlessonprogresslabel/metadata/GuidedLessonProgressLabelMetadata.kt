package com.idworx.lisa.features.guidedlessonprogresslabel.metadata

object GuidedLessonProgressLabelMetadata {
    const val PASS_TOKEN: String = "LISA_GUIDED_LESSON_PROGRESS_LABEL_V1_PASS"
    const val GRADLE_TASK: String = "validateLisaGuidedLessonProgressLabelV1"

    const val LABEL_RULE: String =
        "Every Guided Learning lesson shows a minimal \"Lesson X of Y\" label above the phrase " +
            "title, sized smaller than the title, centered, in Lisa's secondary blue — no " +
            "progress bar, no percentage. Navigation lessons continue the same numbering."
}
