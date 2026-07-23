package com.idworx.lisa

enum class LisaPanel {
    None,
    Menu,
    MyCommunication,
    VocabularyTraining,
    CreatePhrase,
    PhraseEditor,
    Voice,
    VoiceDevice,
    VoicePremium,
    VoiceMyVoice,
    VoiceFamily,
    Settings,
    Recalibration,
    DeveloperTools,
    AboutLisa,
    PrivacyPolicy,
    Feedback,
    TestingChecklist,
    ReleaseNotes
}

fun LisaPanel.isOpen(): Boolean = this != LisaPanel.None

data class LisaSettingsUiState(
    val sensitivityLevel: Int = DEFAULT_SENSITIVITY_LEVEL,
    val textSizeScale: Float = 1.0f,
    val countdownDurationSec: Int = 3,
    val responseSpeed: ResponseSpeed = ResponseSpeed.default,
    val sequenceProcessingDelaySec: Int = SequenceProcessingDelay.DEFAULT_SECONDS,
    val sequenceIdleTimeoutSec: Float = SequenceProcessingDelay.DEFAULT_SECONDS.toFloat(),
    val emergencyAlarmVolume: Float = 1.0f,
    val calibrationEnabled: Boolean = false,
    val developerMode: Boolean = false
)
