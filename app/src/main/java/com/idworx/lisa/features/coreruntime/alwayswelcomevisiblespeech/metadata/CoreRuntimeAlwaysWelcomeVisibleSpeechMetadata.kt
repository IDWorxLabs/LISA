package com.idworx.lisa.features.coreruntime.alwayswelcomevisiblespeech.metadata

object CoreRuntimeAlwaysWelcomeVisibleSpeechMetadata {

    const val PASS_TOKEN: String = "LISA_CORE_RUNTIME_ALWAYS_WELCOME_VISIBLE_SPEECH_V1_PASS"

    const val LAUNCH_RULE: String =
        "Every cold launch starts at Welcome; saved workspace/training state never restores the first screen."

    const val SEQUENCE_RULE: String =
        "Visible blink sequences finalize after the configured response-time idle allowance " +
            "(default 5s) and speak only matching open-screen phrases."
}
