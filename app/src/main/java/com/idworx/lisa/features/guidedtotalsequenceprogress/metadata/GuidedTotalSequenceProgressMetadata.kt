package com.idworx.lisa.features.guidedtotalsequenceprogress.metadata

object GuidedTotalSequenceProgressMetadata {
    const val PASS_TOKEN: String = "LISA_GUIDED_TOTAL_SEQUENCE_PROGRESS_V1_PASS"
    const val GRADLE_TASK: String = "validateLisaGuidedTotalSequenceProgressV1"

    const val PROGRESS_RULE: String =
        "Detected Progress reports the WHOLE gesture sequence, not just the current eye " +
            "section: \"X of Y blinks\" where Y = lesson.left + lesson.right and X is the total " +
            "accepted blinks so far, incrementing on every accepted blink. \"Waiting for:\" still " +
            "names the next expected eye; once the sequence is fully matched it is replaced by " +
            "\"\u2713 Sequence complete\"."
}
