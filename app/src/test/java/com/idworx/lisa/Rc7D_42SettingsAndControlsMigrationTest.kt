package com.idworx.lisa

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Settings & Controls — merge Basic System Controls into the Adjust Settings destination.
 */
class Rc7D_42SettingsAndControlsMigrationTest {

    private val english = LisaUiStrings.forLanguage(PreferredLanguage.English)

    private fun ctx(
        sensitivity: Int = 5,
        responseSec: Int = SequenceProcessingDelay.DEFAULT_SECONDS,
        volume: Int = SpeechVolumeAuthority.DEFAULT_LEVEL,
        speed: Int = SpeechSpeedAuthority.DEFAULT_LEVEL,
        listeningPaused: Boolean = false
    ) = GuidedCatalogContext(
        responseTimeSec = responseSec,
        sensitivityLevel = sensitivity,
        speechVolumeLevel = volume,
        speechSpeedLevel = speed,
        listeningPaused = listeningPaused
    )

    private fun process(
        left: Int,
        right: Int,
        state: GuidedNavigationState,
        catalogContext: GuidedCatalogContext = ctx(),
        blinkOrder: List<Boolean> = emptyList()
    ): GuidedSequenceResult =
        GuidedNavigationController.processSequence(
            left = left,
            right = right,
            state = state,
            language = PreferredLanguage.English,
            uiStrings = english,
            catalogContext = catalogContext,
            blinkOrder = blinkOrder
        )

    private fun navigate(result: GuidedSequenceResult): GuidedNavigationState =
        (result as GuidedSequenceResult.Navigate).newState

    private fun hub(): GuidedNavigationState =
        PreferenceAdjustmentController.openSettingsMenu(
            GuidedNavigationState(screenMode = GuidedOverlayScreenMode.CategoryMenu)
        )

    @Test
    fun communicationOpensUnifiedSettingsAndControlsDestination() {
        val opened = navigate(
            process(
                GuidedModeNavigation.ADJUST_SETTINGS_ENTRY_LEFT,
                GuidedModeNavigation.ADJUST_SETTINGS_ENTRY_RIGHT,
                GuidedNavigationState()
            )
        )
        assertEquals(GuidedPreferencesAdjustMode.SettingsMenu, opened.preferencesAdjustMode)
        assertEquals("Settings & Controls", english.guidedAdjustSettingsTitle)
        assertEquals(
            "Settings & Controls",
            english.guidedCategoryTitle(GuidedVocabularyCategory.AdjustSettings)
        )
    }

    @Test
    fun basicSystemControlsNoLongerAppearsAsPhraseCategory() {
        val titles = GuidedVocabularyCatalog.categoryMenuTitles(english)
        assertFalse(titles.any { it.contains("Basic System Controls", ignoreCase = true) })
        assertFalse(GuidedVocabularyCategory.ordered.contains(GuidedVocabularyCategory.BasicSystemControls))
        assertEquals(7, GuidedVocabularyCategory.PAGE_COUNT)
        assertEquals(7, titles.size)
        assertEquals(7, GuidedVocabularyCatalog.buildPages(PreferredLanguage.English, english).size)
    }

    @Test
    fun categoryCountAndPagingRecalculatedDynamically() {
        assertEquals(GuidedVocabularyCategory.ordered.size, GuidedVocabularyCategory.PAGE_COUNT)
        assertEquals(6, GuidedVocabularyCategory.ADJUST_SETTINGS_INDEX)
        assertFalse(GuidedVocabularyCategory.Preferences in GuidedVocabularyCategory.ordered)
        assertTrue(GuidedVocabularyCatalogValidation.categoryShortcutLabelsMatchExpectedSlots())
    }

    @Test
    fun sensitivityOpensSharedAdjustmentScreen() {
        val state = navigate(
            process(
                GuidedModeNavigation.SELECT_LEFT,
                GuidedModeNavigation.SELECT_RIGHT,
                hub()
            )
        )
        assertEquals(GuidedPreferencesAdjustMode.Sensitivity, state.preferencesAdjustMode)
    }

    @Test
    fun responseTimeOpensSharedAdjustmentScreen() {
        val highlighted = navigate(
            process(
                GuidedModeNavigation.NEXT_LEFT,
                GuidedModeNavigation.NEXT_RIGHT,
                hub()
            )
        )
        assertEquals(1, highlighted.settingsHubSelection)
        val state = navigate(
            process(
                GuidedModeNavigation.SELECT_LEFT,
                GuidedModeNavigation.SELECT_RIGHT,
                highlighted
            )
        )
        assertEquals(GuidedPreferencesAdjustMode.ResponseTime, state.preferencesAdjustMode)
    }

    @Test
    fun speechVolumeOpensSharedAdjustmentScreen() {
        val state = navigate(
            process(
                SettingsAndControlsHubSequences.SPEECH_VOLUME.first,
                SettingsAndControlsHubSequences.SPEECH_VOLUME.second,
                hub(),
                ctx(volume = 7)
            )
        )
        assertEquals(GuidedPreferencesAdjustMode.SpeechVolume, state.preferencesAdjustMode)
        assertEquals(7, state.draftSpeechVolumeLevel)
        assertEquals(7, state.adjustmentOriginalSpeechVolumeLevel)
    }

    @Test
    fun speechSpeedOpensSharedAdjustmentScreen() {
        val viaPrimary = navigate(
            process(
                SettingsAndControlsHubSequences.SPEECH_SPEED.first,
                SettingsAndControlsHubSequences.SPEECH_SPEED.second,
                hub(),
                ctx(speed = 2)
            )
        )
        assertEquals(GuidedPreferencesAdjustMode.SpeechSpeed, viaPrimary.preferencesAdjustMode)
        assertEquals(2, viaPrimary.draftSpeechSpeedLevel)

        val viaAlt = navigate(
            process(
                SettingsAndControlsHubSequences.SPEECH_SPEED_ALT.first,
                SettingsAndControlsHubSequences.SPEECH_SPEED_ALT.second,
                hub()
            )
        )
        assertEquals(GuidedPreferencesAdjustMode.SpeechSpeed, viaAlt.preferencesAdjustMode)
    }

    @Test
    fun adjustmentScreensSupportDecreaseIncreaseSaveAndCancel() {
        var state = PreferenceAdjustmentController.openSpeechVolumeAdjust(hub(), 5)
        state = PreferenceAdjustmentController.decreaseDraft(state)
        assertEquals(4, state.draftSpeechVolumeLevel)
        state = PreferenceAdjustmentController.increaseDraft(state)
        assertEquals(5, state.draftSpeechVolumeLevel)

        val confirm = PreferenceAdjustmentController.beginSaveConfirmation(state)
        assertEquals(GuidedPreferencesAdjustMode.ConfirmSaveSpeechVolume, confirm.preferencesAdjustMode)

        val cancelledConfirm = PreferenceAdjustmentController.cancelSaveConfirmation(confirm)
        assertEquals(GuidedPreferencesAdjustMode.SpeechVolume, cancelledConfirm.preferencesAdjustMode)
        assertEquals(5, cancelledConfirm.draftSpeechVolumeLevel)

        val cancelled = PreferenceAdjustmentController.cancelAdjustment(state)
        assertEquals(GuidedPreferencesAdjustMode.SettingsMenu, cancelled.preferencesAdjustMode)
    }

    @Test
    fun savePersistsSelectedValueInResult() {
        val state = PreferenceAdjustmentController.openSpeechSpeedAdjust(hub(), 3).let {
            PreferenceAdjustmentController.increaseDraft(it)
        }
        val confirm = PreferenceAdjustmentController.beginSaveConfirmation(state)
        val save = PreferenceAdjustmentController.saveAdjustment(confirm)
        assertEquals(4, save.speechSpeedLevel)
        assertEquals(GuidedPreferencesAdjustMode.SettingsMenu, save.newState.preferencesAdjustMode)
    }

    @Test
    fun cancelRestoresWithoutSaveResult() {
        val opened = PreferenceAdjustmentController.openSpeechVolumeAdjust(hub(), 8)
        val changed = PreferenceAdjustmentController.decreaseDraft(opened)
        assertEquals(7, changed.draftSpeechVolumeLevel)
        val cancelled = PreferenceAdjustmentController.cancelAdjustment(changed)
        assertEquals(GuidedPreferencesAdjustMode.SettingsMenu, cancelled.preferencesAdjustMode)
        // Draft fields may remain on hub state; persistence only occurs via SavePreferencesAdjustment.
        val saveOnHub = PreferenceAdjustmentController.saveAdjustment(cancelled)
        assertEquals(null, saveOnHub.speechVolumeLevel)
    }

    @Test
    fun settingsAndControlsHubContainsExactlyFourSettingCards() {
        assertEquals(4, SettingsAndControlsHubSequences.HUB_SETTING_KINDS.size)
        assertEquals(
            listOf(
                SettingsControlKind.Sensitivity,
                SettingsControlKind.ResponseTime,
                SettingsControlKind.SpeechVolume,
                SettingsControlKind.SpeechSpeed
            ),
            SettingsAndControlsHubSequences.HUB_SETTING_KINDS
        )
        val ui = readSource("app/src/main/java/com/idworx/lisa/LisaGuidedModeUi.kt")
        val hub = ui.substringAfter("private fun SettingsAndControlsHubPanel(")
            .substringBefore("private fun SettingsHubCard(")
        assertTrue(hub.contains("guidedSelectSensitivitySetting"))
        assertTrue(hub.contains("guidedSelectResponseTimeSetting"))
        assertTrue(hub.contains("guidedSelectSpeechVolumeSetting"))
        assertTrue(hub.contains("guidedSelectSpeechSpeedSetting"))
        assertFalse(hub.contains("guidedCommunicationControlsSection"))
        assertFalse(hub.contains("guidedSelectListeningSetting"))
        assertFalse(hub.contains("guidedRepeatLastMessageAction"))
        assertFalse(hub.contains("guidedResetSequenceAction"))
        assertFalse(hub.contains("guidedShowHelpAction"))
        assertFalse(hub.contains("SettingsControlKind.Listening"))
        assertFalse(hub.contains("SettingsControlKind.RepeatLastMessage"))
        assertFalse(hub.contains("SettingsControlKind.ResetSequence"))
        assertFalse(hub.contains("SettingsControlKind.ShowHelp"))
    }

    @Test
    fun hubNoLongerOpensListeningOrCommunicationActions() {
        assertTrue(
            process(
                SettingsAndControlsHubSequences.LISTENING.first,
                SettingsAndControlsHubSequences.LISTENING.second,
                hub()
            ) is GuidedSequenceResult.Unmatched
        )
        assertTrue(
            process(
                SettingsAndControlsHubSequences.REPEAT_LAST.first,
                SettingsAndControlsHubSequences.REPEAT_LAST.second,
                hub()
            ) is GuidedSequenceResult.Unmatched
        )
        assertTrue(
            process(
                SettingsAndControlsHubSequences.RESET_SEQUENCE.first,
                SettingsAndControlsHubSequences.RESET_SEQUENCE.second,
                hub()
            ) is GuidedSequenceResult.Unmatched
        )
        assertTrue(
            process(
                SettingsAndControlsHubSequences.SHOW_HELP.first,
                SettingsAndControlsHubSequences.SHOW_HELP.second,
                hub()
            ) is GuidedSequenceResult.Unmatched
        )
    }

    @Test
    fun listeningControlScreenStillWorksWhenOpenedInternally() {
        val listening = PreferenceAdjustmentController.openListeningControl(hub())
        assertEquals(GuidedPreferencesAdjustMode.Listening, listening.preferencesAdjustMode)
        val toggle = process(
            GuidedModeNavigation.SELECT_LEFT,
            GuidedModeNavigation.SELECT_RIGHT,
            listening
        )
        assertTrue(toggle is GuidedSequenceResult.SettingsControlAction)
        assertEquals(
            SettingsControlKind.Listening,
            (toggle as GuidedSequenceResult.SettingsControlAction).kind
        )
    }

    @Test
    fun openMenuPhraseRemovedWhileGlobalMenuRemains() {
        val pages = GuidedVocabularyCatalog.buildPages(PreferredLanguage.English, english)
        assertTrue(pages.none { page ->
            page.entries.any { it.guidedAction == GuidedOverlayAction.OpenMenu }
        })
        assertTrue(GuidedModeNavigation.isOpenMainMenuSequence(4, 6))
    }

    @Test
    fun existingBlinkSequencesStillTriggerCorrectActions() {
        // Sensitivity decrease / increase / save / cancel unchanged.
        var state = PreferenceAdjustmentController.openSensitivityAdjust(hub(), 5)
        state = navigate(
            process(
                GuidedModeNavigation.DECREASE_VALUE_LEFT,
                GuidedModeNavigation.DECREASE_VALUE_RIGHT,
                state
            )
        )
        assertEquals(4, state.draftSensitivityLevel)
        state = navigate(
            process(
                GuidedModeNavigation.INCREASE_VALUE_LEFT,
                GuidedModeNavigation.INCREASE_VALUE_RIGHT,
                state
            )
        )
        assertEquals(5, state.draftSensitivityLevel)
        state = navigate(
            process(GuidedModeNavigation.SELECT_LEFT, GuidedModeNavigation.SELECT_RIGHT, state)
        )
        assertEquals(GuidedPreferencesAdjustMode.ConfirmSaveSensitivity, state.preferencesAdjustMode)
        state = navigate(
            process(GuidedModeNavigation.BACK_LEFT, GuidedModeNavigation.BACK_RIGHT, hub())
        )
        assertEquals(GuidedPreferencesAdjustMode.None, state.preferencesAdjustMode)
    }

    @Test
    fun noPrefixShadowingAmongHubSequences() {
        val pairs = listOf(
            SettingsAndControlsHubSequences.SENSITIVITY,
            SettingsAndControlsHubSequences.RESPONSE_TIME,
            SettingsAndControlsHubSequences.SPEECH_VOLUME,
            SettingsAndControlsHubSequences.SPEECH_SPEED,
            SettingsAndControlsHubSequences.BACK
        )
        assertEquals(pairs.size, pairs.distinct().size)
        pairs.forEach { (left, right) ->
            assertFalse(isEmergencySequence(left, right))
        }
    }

    @Test
    fun emergencyRemainsReachableConceptuallyFromEveryNewScreen() {
        assertEquals(
            null,
            SettingsAndControlsHubSequences.hubSettingKindForGesture(
                EMERGENCY_LEFT_WINKS,
                EMERGENCY_RIGHT_WINKS
            )
        )
        listOf(
            GuidedPreferencesAdjustMode.SettingsMenu,
            GuidedPreferencesAdjustMode.Sensitivity,
            GuidedPreferencesAdjustMode.ResponseTime,
            GuidedPreferencesAdjustMode.SpeechVolume,
            GuidedPreferencesAdjustMode.SpeechSpeed
        ).forEach { mode ->
            val state = hub().copy(preferencesAdjustMode = mode)
            assertTrue(state.isPreferencesAdjustmentActive || mode == GuidedPreferencesAdjustMode.SettingsMenu)
            assertEquals(
                null,
                SettingsAndControlsHubSequences.hubSettingKindForGesture(
                    EMERGENCY_LEFT_WINKS,
                    EMERGENCY_RIGHT_WINKS
                )
            )
        }
    }

    @Test
    fun hubCardsLayoutDoesNotClipValuesAndHasSingleHeading() {
        val ui = readSource("app/src/main/java/com/idworx/lisa/LisaGuidedModeUi.kt")
        val hub = ui.substringAfter("private fun SettingsAndControlsHubPanel(")
            .substringBefore("private fun SettingsHubCard(")
        val card = ui.substringAfter("private fun SettingsHubCard(")
            .substringBefore("private fun ListeningControlPanel(")
        // Shared equal-height cards — no clip, shared min height + weight for responsive sizing.
        assertTrue(hub.contains("SettingsAndControlsHubVisualStyle.CardSpacing"))
        assertTrue(hub.contains(".weight(1f)"))
        assertTrue(card.contains("SettingsAndControlsHubVisualStyle.CardMinHeight"))
        assertFalse(card.contains(".clip("))
        assertTrue(card.contains("heightIn(min"))
        // Single primary heading lives in GuidedOverlayHeader — hub body has no duplicate title.
        assertFalse(hub.contains("guidedAdjustSettingsTitle"))
        assertTrue(ui.contains("preferencesAdjustMode == GuidedPreferencesAdjustMode.SettingsMenu -> uiStrings.guidedAdjustSettingsTitle"))
    }

    @Test
    fun scrollUpAndDownChangeSelectionWithoutOpening() {
        var state = hub()
        assertEquals(0, state.settingsHubSelection)
        assertTrue(
            process(GuidedModeNavigation.PREVIOUS_LEFT, GuidedModeNavigation.PREVIOUS_RIGHT, state)
                is GuidedSequenceResult.Unmatched
        )
        state = navigate(process(GuidedModeNavigation.NEXT_LEFT, GuidedModeNavigation.NEXT_RIGHT, state))
        assertEquals(1, state.settingsHubSelection)
        assertEquals(GuidedPreferencesAdjustMode.SettingsMenu, state.preferencesAdjustMode)
        state = navigate(process(GuidedModeNavigation.NEXT_LEFT, GuidedModeNavigation.NEXT_RIGHT, state))
        assertEquals(2, state.settingsHubSelection)
        state = navigate(process(GuidedModeNavigation.PREVIOUS_LEFT, GuidedModeNavigation.PREVIOUS_RIGHT, state))
        assertEquals(1, state.settingsHubSelection)
        assertEquals(GuidedPreferencesAdjustMode.SettingsMenu, state.preferencesAdjustMode)
    }

    @Test
    fun selectOpensOnlyHighlightedSettingAndRailActionsDoNotFallThrough() {
        val actions = GuidedNavigationPanelSpec.panelActions(english, GuidedNavigationPanelSpec.PanelContext.SettingsHub)
        assertEquals(
            GuidedPanelActionKind.Select,
            actions.first { it.kind == GuidedPanelActionKind.Select }.kind
        )
        assertEquals("Select Setting", english.guidedOpenSelectedSetting)
        assertEquals(
            english.guidedOpenSelectedSetting,
            actions.first { it.kind == GuidedPanelActionKind.Select }.title
        )
        assertEquals(
            english.guidedSaveSelectedValue,
            GuidedNavigationPanelSpec.panelActions(english, GuidedNavigationPanelSpec.PanelContext.Adjustment)
                .first { it.kind == GuidedPanelActionKind.Select }.title
        )

        // Scroll Down must not open; Select opens Response Time after highlight.
        val scrolled = navigate(
            process(GuidedModeNavigation.NEXT_LEFT, GuidedModeNavigation.NEXT_RIGHT, hub())
        )
        assertEquals(GuidedPreferencesAdjustMode.SettingsMenu, scrolled.preferencesAdjustMode)
        val opened = navigate(
            process(GuidedModeNavigation.SELECT_LEFT, GuidedModeNavigation.SELECT_RIGHT, scrolled)
        )
        assertEquals(GuidedPreferencesAdjustMode.ResponseTime, opened.preferencesAdjustMode)

        // Direct card shortcuts still open volume/speed without changing Select semantics.
        assertEquals(
            GuidedPreferencesAdjustMode.SpeechVolume,
            navigate(
                process(
                    SettingsAndControlsHubSequences.SPEECH_VOLUME.first,
                    SettingsAndControlsHubSequences.SPEECH_VOLUME.second,
                    hub()
                )
            ).preferencesAdjustMode
        )
    }

    @Test
    fun hubBackCategoriesAndEmergencyDoNotOpenSelectedCard() {
        val state = navigate(
            process(GuidedModeNavigation.NEXT_LEFT, GuidedModeNavigation.NEXT_RIGHT, hub())
        )
        assertEquals(1, state.settingsHubSelection)

        val back = navigate(
            process(GuidedModeNavigation.BACK_LEFT, GuidedModeNavigation.BACK_RIGHT, state)
        )
        assertEquals(GuidedPreferencesAdjustMode.None, back.preferencesAdjustMode)

        val categories = navigate(
            process(
                GuidedModeNavigation.CATEGORIES_LEFT,
                GuidedModeNavigation.CATEGORIES_RIGHT,
                state
            )
        )
        assertEquals(GuidedOverlayScreenMode.CategoryMenu, categories.screenMode)
        assertEquals(GuidedPreferencesAdjustMode.None, categories.preferencesAdjustMode)

        // Emergency is not claimed by hub routing — remains globally available.
        assertEquals(
            null,
            SettingsAndControlsHubSequences.hubDirectOpenKindForGesture(
                EMERGENCY_LEFT_WINKS,
                EMERGENCY_RIGHT_WINKS
            )
        )
        assertFalse(GuidedModeNavigation.isSelectSequence(EMERGENCY_LEFT_WINKS, EMERGENCY_RIGHT_WINKS))
    }

    @Test
    fun settingsHubRailContextWiresDistinctActionsAndDisablesPageControls() {
        val ui = readSource("app/src/main/java/com/idworx/lisa/LisaGuidedModeUi.kt")
        assertTrue(ui.contains("PanelContext.SettingsHub"))
        assertTrue(ui.contains("safeState.isSettingsMenuActive -> GuidedNavigationPanelSpec.PanelContext.SettingsHub"))
        // Category page jumps are Category Menu only — not shown on Settings hub chrome.
        val actions = GuidedNavigationPanelSpec.panelActions(english, GuidedNavigationPanelSpec.PanelContext.SettingsHub)
        assertFalse(actions.any { it.kind == GuidedPanelActionKind.PreviousCategoryPage })
        assertFalse(actions.any { it.kind == GuidedPanelActionKind.NextCategoryPage })
        assertTrue(actions.any { it.kind == GuidedPanelActionKind.ScrollUp })
        assertTrue(actions.any { it.kind == GuidedPanelActionKind.ScrollDown })
        assertTrue(actions.any { it.kind == GuidedPanelActionKind.Select })
        assertTrue(actions.any { it.kind == GuidedPanelActionKind.Back })
        assertTrue(actions.any { it.kind == GuidedPanelActionKind.Categories })
        assertTrue(actions.any { it.kind == GuidedPanelActionKind.Emergency })
        // Disabled Scroll Up at index 0: canGoPrevious false — clickable(enabled=false) does not fire.
        assertTrue(ui.contains("enabled = enabled && !trainingDimmed"))
        assertTrue(
            ui.contains("safeState.isSettingsMenuActive -> safeState.settingsHubSelection > 0")
        )
    }

    @Test
    fun touchAndEyeShareSameHubActionAuthority() {
        assertTrue(
            GuidedTouchNavigationSpec.touchMirrorsEyeGesture(
                GuidedModeNavigation.PREVIOUS_LEFT,
                GuidedModeNavigation.PREVIOUS_RIGHT
            )
        )
        assertTrue(
            GuidedTouchNavigationSpec.touchMirrorsEyeGesture(
                GuidedModeNavigation.NEXT_LEFT,
                GuidedModeNavigation.NEXT_RIGHT
            )
        )
        assertTrue(
            GuidedTouchNavigationSpec.touchMirrorsEyeGesture(
                GuidedModeNavigation.SELECT_LEFT,
                GuidedModeNavigation.SELECT_RIGHT
            )
        )
        assertTrue(
            GuidedTouchNavigationSpec.touchMirrorsEyeGesture(
                GuidedModeNavigation.BACK_LEFT,
                GuidedModeNavigation.BACK_RIGHT
            )
        )
        assertTrue(
            GuidedTouchNavigationSpec.touchMirrorsEyeGesture(
                GuidedModeNavigation.CATEGORIES_LEFT,
                GuidedModeNavigation.CATEGORIES_RIGHT
            )
        )
        assertTrue(
            GuidedTouchNavigationSpec.touchMirrorsEyeGesture(
                EMERGENCY_LEFT_WINKS,
                EMERGENCY_RIGHT_WINKS
            )
        )
        val main = readSource("app/src/main/java/com/idworx/lisa/MainActivity.kt")
        assertTrue(main.contains("onGuidedNavigateUp = { applyGuidedTouchNavigation(GuidedModeNavigation.PREVIOUS_LEFT"))
        assertTrue(main.contains("onGuidedNavigateDown = { applyGuidedTouchNavigation(GuidedModeNavigation.NEXT_LEFT"))
        assertTrue(main.contains("onGuidedSelectEnter = {"))
        assertTrue(main.contains("onGuidedBack = { applyGuidedTouchNavigation(GuidedModeNavigation.BACK_LEFT"))
        assertTrue(main.contains("onGuidedCategories = { applyGuidedTouchNavigation(GuidedModeNavigation.CATEGORIES_LEFT"))
    }

    @Test
    fun openHubSettingFromControllerMatchesCardTouchAuthority() {
        val sensitivity = PreferenceAdjustmentController.openHubSetting(
            hub(),
            SettingsControlKind.Sensitivity,
            ctx(sensitivity = 7)
        )
        assertEquals(GuidedPreferencesAdjustMode.Sensitivity, sensitivity.preferencesAdjustMode)
        assertEquals(7, sensitivity.draftSensitivityLevel)

        val volume = PreferenceAdjustmentController.openHubSetting(
            hub(),
            SettingsControlKind.SpeechVolume,
            ctx(volume = 9)
        )
        assertEquals(GuidedPreferencesAdjustMode.SpeechVolume, volume.preferencesAdjustMode)
        assertEquals(9, volume.draftSpeechVolumeLevel)
    }

    @Test
    fun physicalLeftRightMappingRemainsCorrectOnMeters() {
        assertEquals(3, GuidedModeNavigation.DECREASE_VALUE_LEFT)
        assertEquals(1, GuidedModeNavigation.DECREASE_VALUE_RIGHT)
        assertEquals(1, GuidedModeNavigation.INCREASE_VALUE_LEFT)
        assertEquals(3, GuidedModeNavigation.INCREASE_VALUE_RIGHT)
    }

    @Test
    fun speechAuthoritiesMapToTtsRanges() {
        assertEquals(1.0f, SpeechVolumeAuthority.toTtsVolume(10), 0.001f)
        assertEquals(0.1f, SpeechVolumeAuthority.toTtsVolume(1), 0.001f)
        assertEquals(1.0f, SpeechSpeedAuthority.toSpeechRate(3), 0.001f)
        assertEquals(0.5f, SpeechSpeedAuthority.toSpeechRate(1), 0.001f)
        assertEquals(1.5f, SpeechSpeedAuthority.toSpeechRate(5), 0.001f)
    }

    private fun readSource(relativePath: String): String {
        val normalized = relativePath.replace('/', java.io.File.separatorChar)
        val roots = listOfNotNull(
            java.io.File(System.getProperty("user.dir") ?: "."),
            java.io.File(System.getProperty("user.dir") ?: ".").parentFile
        )
        for (root in roots) {
            val direct = root.resolve(normalized)
            if (direct.isFile) return direct.readText()
            if (normalized.startsWith("app${java.io.File.separatorChar}")) {
                val withoutApp = root.resolve(normalized.removePrefix("app${java.io.File.separatorChar}"))
                if (withoutApp.isFile) return withoutApp.readText()
            }
        }
        error("Missing source: $relativePath")
    }
}
