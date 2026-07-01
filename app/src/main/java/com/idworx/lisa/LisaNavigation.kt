package com.idworx.lisa

enum class LisaPanel {
    None,
    Menu,
    MyCommunication,
    CommunicationSetup,
    VocabularyTraining,
    EmergencySetup,
    CaregiverLinking,
    Settings,
    DeveloperTools,
    AboutLisa,
    Feedback,
    TestingChecklist,
    ReleaseNotes
}

fun LisaPanel.isOpen(): Boolean = this != LisaPanel.None

data class LisaSettingsUiState(
    val sensitivityLevel: Int = DEFAULT_SENSITIVITY_LEVEL,
    val textSizeScale: Float = 1.0f,
    val countdownDurationSec: Int = 3,
    val sequenceIdleTimeoutSec: Float = 2.5f,
    val emergencyAlarmVolume: Float = 1.0f,
    val calibrationEnabled: Boolean = false,
    val developerMode: Boolean = false
)
