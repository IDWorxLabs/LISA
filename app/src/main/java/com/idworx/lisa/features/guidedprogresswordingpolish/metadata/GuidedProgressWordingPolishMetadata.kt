package com.idworx.lisa.features.guidedprogresswordingpolish.metadata

object GuidedProgressWordingPolishMetadata {
    const val PASS_TOKEN: String = "LISA_GUIDED_PROGRESS_WORDING_POLISH_V1_PASS"
    const val GRADLE_TASK: String = "validateLisaGuidedProgressWordingPolishV1"

    const val WORDING_RULE: String =
        "Detected Progress reads \"X of Y blinks\" (e.g. \"1 of 4 blinks\") instead of " +
            "\"Blink X of Y\", so the learner reads it as a count of total blinks completed " +
            "rather than a gesture-step label. The total, increment behavior, waiting label, " +
            "and \"\u2713 Sequence complete\" completion state are all unchanged — this is wording only."
}
