package com.idworx.lisa.features.blinkdetectionreliability.metadata

object BlinkDetectionReliabilityMetadata {

    const val PASS_TOKEN: String = "LISA_BLINK_DETECTION_RELIABILITY_TUNING_V1_PASS"

    const val TUNING_RULE: String =
        "Blink detection uses forgiving cooldown, streak grace, and sequence-aware jitter tolerance."

    const val SEQUENCE_RULE: String =
        "Sequence finalization remains 3 s idle after last accepted blink; phrase speech only."
}
