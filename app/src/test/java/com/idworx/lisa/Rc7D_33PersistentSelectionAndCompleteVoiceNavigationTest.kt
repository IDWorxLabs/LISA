package com.idworx.lisa

import com.idworx.lisa.ui.theme.LisaWorkspaceVisualStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Rc7D_33PersistentSelectionAndCompleteVoiceNavigationTest {
    private val strings = LisaUiStrings.forLanguage(PreferredLanguage.English)
    private val profiles = listOf(
        LisaUserProfile(
            id = "primary",
            name = "Primary User",
            preferredLanguage = PreferredLanguage.English,
            communicationLevel = CommunicationLevel.Standard
        ),
        LisaUserProfile(
            id = "secondary",
            name = "Secondary User",
            preferredLanguage = PreferredLanguage.Afrikaans,
            communicationLevel = CommunicationLevel.Beginner
        )
    )

    @Test
    fun canonicalSurfaceHighlightsOnlyTheEnabledNavigationSelection() {
        val selected = MenuDestinationSelectedSurfaceAuthority.visualState(
            MenuDestinationActionId.ProfileName,
            MenuDestinationActionId.ProfileName,
            active = false,
            enabled = true
        )
        val idle = MenuDestinationSelectedSurfaceAuthority.visualState(
            MenuDestinationActionId.ProfileName,
            MenuDestinationActionId.ProfileNew,
            active = false,
            enabled = true
        )

        assertTrue(selected.selected)
        assertFalse(idle.selected)
        assertEquals(
            LisaWorkspaceVisualStyle.CardSelectedBackground,
            MenuDestinationSelectedSurfaceAuthority.background(selected)
        )
        assertNotEquals(
            MenuDestinationSelectedSurfaceAuthority.background(selected),
            MenuDestinationSelectedSurfaceAuthority.background(idle)
        )
    }

    @Test
    fun selectedAndActiveStatesRemainIndependent() {
        val activeEnglish = MenuDestinationSelectedSurfaceAuthority.visualState(
            MenuDestinationActionId.language("English"),
            MenuDestinationActionId.language("Afrikaans"),
            active = true,
            enabled = true
        )
        val selectedAfrikaans = MenuDestinationSelectedSurfaceAuthority.visualState(
            MenuDestinationActionId.language("Afrikaans"),
            MenuDestinationActionId.language("Afrikaans"),
            active = false,
            enabled = true
        )

        assertTrue(activeEnglish.active)
        assertFalse(activeEnglish.selected)
        assertTrue(selectedAfrikaans.selected)
        assertFalse(selectedAfrikaans.active)
    }

    @Test
    fun disabledActionNeverReceivesSelectedStyling() {
        val state = MenuDestinationSelectedSurfaceAuthority.visualState(
            MenuDestinationActionId.VoiceTest,
            MenuDestinationActionId.VoiceTest,
            active = false,
            enabled = false
        )

        assertFalse(state.selected)
        assertFalse(state.enabled)
    }

    @Test
    fun communicationProfileCatalogContainsEveryInteractiveControlInVisualOrder() {
        val ids = CommunicationProfileDestinationActionAuthority.actions(
            profiles,
            "primary",
            strings
        ).map { it.id }

        assertEquals(MenuDestinationActionId.ProfileActive, ids.first())
        assertEquals(MenuDestinationActionId.ProfileName, ids[1])
        assertTrue(PreferredLanguage.selectable.all { MenuDestinationActionId.language(it.label) in ids })
        assertTrue(CommunicationLevel.entries.all {
            MenuDestinationActionId.communicationLevel(it.label) in ids
        })
        assertTrue(profiles.all { MenuDestinationActionId.savedProfile(it.id) in ids })
        assertTrue(MenuDestinationActionId.ProfileNew in ids)
        assertTrue(MenuDestinationActionId.ProfileDelete in ids)
    }

    @Test
    fun profileSectionHeadingsAreNotFocusableActions() {
        val labels = CommunicationProfileDestinationActionAuthority.actions(
            profiles,
            "primary",
            strings
        ).map { it.label.uppercase() }

        assertFalse("ACTIVE PROFILE" in labels)
        assertFalse("PREFERRED LANGUAGE" in labels)
        assertFalse("COMMUNICATION LEVEL" in labels)
        assertFalse("SAVED PROFILES" in labels)
    }

    @Test
    fun profileMoveDownAndUpTraversesEveryEnabledActionWithoutWrapping() {
        val actions = CommunicationProfileDestinationActionAuthority.actions(
            profiles,
            "primary",
            strings
        )
        var state = MenuDestinationNavigationController.open(
            MainMenuDestination.CommunicationProfile,
            LisaPanel.MyCommunication,
            actions
        )
        val visitedDown = mutableListOf(state.selectedActionId)
        repeat(actions.size + 2) {
            state = MenuDestinationNavigationController.move(state, actions, 1)
            visitedDown += state.selectedActionId
        }
        assertEquals(actions.map { it.id }, visitedDown.distinct())
        assertEquals(actions.last().id, state.selectedActionId)

        repeat(actions.size + 2) {
            state = MenuDestinationNavigationController.move(state, actions, -1)
        }
        assertEquals(actions.first().id, state.selectedActionId)
    }

    @Test
    fun recompositionActionRefreshRetainsSelectedActionIdentity() {
        val actions = CommunicationProfileDestinationActionAuthority.actions(
            profiles,
            "primary",
            strings
        )
        val selectedId = MenuDestinationActionId.language("isiZulu")
        val refreshed = MenuDestinationNavigationController.updateActions(
            MenuDestinationNavigationState(
                destination = MainMenuDestination.CommunicationProfile,
                panel = LisaPanel.MyCommunication,
                isActive = true,
                selectedActionId = selectedId,
                selectedIndex = actions.indexOfFirst { it.id == selectedId }
            ),
            actions.map { it.copy() }
        )

        assertEquals(selectedId, refreshed.selectedActionId)
    }

    @Test
    fun viewportSynchronizationAndPagingNeverOverwriteSelection() {
        val selectedId = MenuDestinationActionId.ProfileNew
        val state = MenuDestinationNavigationState(
            destination = MainMenuDestination.CommunicationProfile,
            panel = LisaPanel.MyCommunication,
            isActive = true,
            selectedActionId = selectedId,
            selectedIndex = 9
        )
        val synced = MenuDestinationNavigationController.syncViewportMetrics(state, 300, 900, 450)
        val paged = MenuDestinationNavigationController.nextPage(synced, 300, 900)

        assertEquals(selectedId, synced.selectedActionId)
        assertEquals(selectedId, paged.selectedActionId)
    }

    @Test
    fun keyboardCatalogHasExplicitDirectionsAndOneSelectKeyRoute() {
        val expected = mapOf(
            MenuDestinationPanelCommand.MoveUp to (2 to 0),
            MenuDestinationPanelCommand.MoveDown to (0 to 2),
            MenuDestinationPanelCommand.MoveLeft to (2 to 1),
            MenuDestinationPanelCommand.MoveRight to (1 to 2),
            MenuDestinationPanelCommand.Select to (1 to 1)
        )

        assertEquals(expected.keys.toList(), FeedbackKeyboardNavigationAuthority.directionCommands)
        expected.forEach { (command, sequence) ->
            assertEquals(sequence, FeedbackKeyboardNavigationAuthority.sequence(command))
        }
        assertEquals(
            1,
            FeedbackKeyboardNavigationAuthority.keyboardCommands.count {
                FeedbackKeyboardNavigationAuthority.sequence(it) == (1 to 1)
            }
        )
    }

    @Test
    fun keyboardCatalogContainsNoOpenOrPlaceholderAction() {
        assertFalse(
            FeedbackKeyboardNavigationAuthority.keyboardCommands.any {
                it.name.equals("Open", ignoreCase = true)
            }
        )
        assertEquals(
            FeedbackKeyboardNavigationAuthority.keyboardCommands.size,
            FeedbackKeyboardNavigationAuthority.keyboardCommands.distinct().size
        )
        assertEquals(8, FeedbackKeyboardNavigationAuthority.keyboardCommands.size)
    }

    @Test
    fun feedbackEditingCommandsPreserveDoneBackAndEmergencySequences() {
        assertEquals(
            3 to 2,
            FeedbackKeyboardNavigationAuthority.sequence(MenuDestinationPanelCommand.DoneEditing)
        )
        assertEquals(
            2 to 2,
            FeedbackKeyboardNavigationAuthority.sequence(MenuDestinationPanelCommand.Back)
        )
        assertEquals(
            6 to 0,
            FeedbackKeyboardNavigationAuthority.sequence(MenuDestinationPanelCommand.Emergency)
        )
    }

    @Test
    fun deviceVoiceCatalogMatchesEveryLiveEnabledControlInVisualOrder() {
        val voices = listOf(
            LisaVoiceOption("voice.one", "Voice One", "en-ZA", false),
            LisaVoiceOption("voice.two", "Voice Two", "en-GB", true)
        )
        val actions = DeviceVoiceDestinationActionAuthority.actions(
            LisaVoiceSettingsState(
                availableVoices = voices,
                selectedVoiceName = "voice.one",
                ttsReady = true
            ),
            strings
        )

        assertEquals(
            listOf(
                MenuDestinationActionId.installedVoice("voice.one"),
                MenuDestinationActionId.installedVoice("voice.two"),
                MenuDestinationActionId.VoiceTest,
                MenuDestinationActionId.VoiceInstallData,
                MenuDestinationActionId.VoiceSystemSettings
            ),
            actions.map { it.id }
        )
        assertTrue(actions.all { it.isEnabled })
    }

    @Test
    fun deviceVoiceMoveDownContinuesPastPreviewToInstallAndSettings() {
        val actions = DeviceVoiceDestinationActionAuthority.actions(
            LisaVoiceSettingsState(ttsReady = true),
            strings
        )
        var state = MenuDestinationNavigationController.open(
            MainMenuDestination.Voice,
            LisaPanel.VoiceDevice,
            actions
        )

        assertEquals(MenuDestinationActionId.VoiceTest, state.selectedActionId)
        state = MenuDestinationNavigationController.move(state, actions, 1)
        assertEquals(MenuDestinationActionId.VoiceInstallData, state.selectedActionId)
        state = MenuDestinationNavigationController.move(state, actions, 1)
        assertEquals(MenuDestinationActionId.VoiceSystemSettings, state.selectedActionId)
        state = MenuDestinationNavigationController.move(state, actions, 1)
        assertEquals(MenuDestinationActionId.VoiceSystemSettings, state.selectedActionId)
    }

    @Test
    fun unavailableDeviceVoiceActionsAreSkippedWithoutTrappingSelection() {
        val actions = DeviceVoiceDestinationActionAuthority.actions(
            LisaVoiceSettingsState(
                availableVoices = listOf(
                    LisaVoiceOption("not-ready", "Not Ready", "en-ZA", false)
                ),
                ttsReady = false
            ),
            strings
        )
        var state = MenuDestinationNavigationController.open(
            MainMenuDestination.Voice,
            LisaPanel.VoiceDevice,
            actions
        )

        assertEquals(MenuDestinationActionId.VoiceInstallData, state.selectedActionId)
        state = MenuDestinationNavigationController.move(state, actions, 1)
        assertEquals(MenuDestinationActionId.VoiceSystemSettings, state.selectedActionId)
        assertFalse(actions.first { it.id == MenuDestinationActionId.VoiceTest }.canReceiveFocus)
    }

    @Test
    fun deviceVoiceMoveUpTraversesBackThroughAllEnabledActions() {
        val actions = DeviceVoiceDestinationActionAuthority.actions(
            LisaVoiceSettingsState(ttsReady = true),
            strings
        )
        var state = MenuDestinationNavigationController.open(
            MainMenuDestination.Voice,
            LisaPanel.VoiceDevice,
            actions
        )
        repeat(5) { state = MenuDestinationNavigationController.move(state, actions, 1) }
        state = MenuDestinationNavigationController.move(state, actions, -1)
        assertEquals(MenuDestinationActionId.VoiceInstallData, state.selectedActionId)
        state = MenuDestinationNavigationController.move(state, actions, -1)
        assertEquals(MenuDestinationActionId.VoiceTest, state.selectedActionId)
    }

    @Test
    fun itemRevealAndPageMovementRemainIndependent() {
        val actions = DeviceVoiceDestinationActionAuthority.actions(
            LisaVoiceSettingsState(ttsReady = true),
            strings
        )
        var state = MenuDestinationNavigationController.open(
            MainMenuDestination.Voice,
            LisaPanel.VoiceDevice,
            actions
        )
        state = MenuDestinationNavigationController.move(state, actions, 1)
        assertTrue(state.revealSelection)
        val selected = state.selectedActionId
        state = MenuDestinationNavigationController.nextPage(state, 240, 720)
        assertEquals(selected, state.selectedActionId)
        assertFalse(state.revealSelection)
        state = MenuDestinationNavigationController.move(state, actions, 1)
        assertEquals(MenuDestinationActionId.VoiceSystemSettings, state.selectedActionId)
        assertTrue(state.revealSelection)
    }

    @Test
    fun invalidViewportMeasurementsAreSafeAndKeepSelection() {
        val state = MenuDestinationNavigationState(
            destination = MainMenuDestination.Voice,
            panel = LisaPanel.VoiceDevice,
            isActive = true,
            selectedActionId = MenuDestinationActionId.VoiceInstallData,
            selectedIndex = 1
        )
        val result = MenuDestinationNavigationController.nextPage(state, -1, -1)

        assertEquals(MenuDestinationActionId.VoiceInstallData, result.selectedActionId)
        assertNull(result.scrollRequestPx)
    }
}
