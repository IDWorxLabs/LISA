package com.idworx.lisa.features.runtimecameracontextualspeech.metadata

object RuntimeCameraContextualSpeechMetadata {

    const val PASS_TOKEN: String = "LISA_RUNTIME_CAMERA_AND_CONTEXTUAL_SPEECH_V1_PASS"

    const val VOICE_POLICY: String =
        "Phrase translation only — no setup, lesson, coaching, or presence narration."

    const val SETUP_CONTINUE_FLOW: String =
        "Camera permission setup Continue routes to HELLO (CommunicationLesson index 0)."

    const val CONTEXTUAL_PHRASE_RULE: String =
        "Communication Workspace resolves blink sequences only against currently visible/open phrase options."
}
