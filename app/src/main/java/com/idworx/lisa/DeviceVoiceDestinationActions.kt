package com.idworx.lisa

/** Single source of truth for the live Device Voice controls and their visual order. */
object DeviceVoiceDestinationActionAuthority {
    fun actions(
        state: LisaVoiceSettingsState,
        uiStrings: LisaUiStrings
    ): List<MenuDestinationAction> = buildList {
        state.availableVoices.forEach { voice ->
            add(
                MenuDestinationAction(
                    id = MenuDestinationActionId.installedVoice(voice.name),
                    label = voice.displayLabel,
                    actionType = MenuDestinationActionType.Choice,
                    isEnabled = state.ttsReady,
                    selected = voice.name == state.selectedVoiceName,
                    sectionId = "installed_voices"
                )
            )
        }
        add(
            MenuDestinationAction(
                id = MenuDestinationActionId.VoiceTest,
                label = uiStrings.testVoice,
                actionType = MenuDestinationActionType.Button,
                isEnabled = state.ttsReady,
                sectionId = "voice_actions"
            )
        )
        add(
            MenuDestinationAction(
                id = MenuDestinationActionId.VoiceInstallData,
                label = uiStrings.installVoiceData,
                actionType = MenuDestinationActionType.Button,
                isEnabled = true,
                sectionId = "voice_actions"
            )
        )
        add(
            MenuDestinationAction(
                id = MenuDestinationActionId.VoiceSystemSettings,
                label = uiStrings.openTtsSettings,
                actionType = MenuDestinationActionType.Button,
                isEnabled = true,
                sectionId = "voice_actions"
            )
        )
    }
}
