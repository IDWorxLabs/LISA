package com.idworx.lisa.features.guidedsuccesstimingfix.metadata

object GuidedSuccessTimingFixMetadata {
    const val PASS_TOKEN: String = "LISA_GUIDED_SUCCESS_TIMING_FIX_V1_PASS"
    const val GRADLE_TASK: String = "validateLisaGuidedSuccessTimingFixV1"

    const val TIMING_RULE: String =
        "After a valid sequence finalizes, LISA speaks the translated phrase first while the " +
            "lesson phrase stays visible, then reveals \"Well done\", briefly pauses, and only " +
            "then advances to the next lesson."
}
