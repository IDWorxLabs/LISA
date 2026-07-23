package com.idworx.lisa

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Rc7D_31EyeControlledMenuDestinationsTest {

    private val destination = MainMenuDestination.CommunicationProfile

    private fun action(
        id: String,
        type: MenuDestinationActionType = MenuDestinationActionType.Button,
        enabled: Boolean = true
    ) = MenuDestinationAction(
        MenuDestinationActionId(id),
        id,
        type,
        enabled
    )

    private fun open(actions: List<MenuDestinationAction>) =
        MenuDestinationNavigationController.open(
            destination,
            LisaPanel.MyCommunication,
            actions
        )

    @Test
    fun canonicalDestinationModesCoverEveryMainMenuDestination() {
        assertEquals(
            MenuDestinationScreenMode.CommunicationProfile,
            MenuDestinationScreenMode.fromPanel(LisaPanel.MyCommunication)
        )
        assertEquals(
            MenuDestinationScreenMode.PhraseManagement,
            MenuDestinationScreenMode.fromPanel(LisaPanel.VocabularyTraining)
        )
        assertEquals(
            MenuDestinationScreenMode.Voice,
            MenuDestinationScreenMode.fromPanel(LisaPanel.VoiceDevice)
        )
        assertEquals(
            MenuDestinationScreenMode.Settings,
            MenuDestinationScreenMode.fromPanel(LisaPanel.TestingChecklist)
        )
        assertEquals(
            MenuDestinationScreenMode.AboutLisa,
            MenuDestinationScreenMode.fromPanel(LisaPanel.AboutLisa)
        )
        assertEquals(
            MenuDestinationScreenMode.PrivacyPolicy,
            MenuDestinationScreenMode.fromPanel(LisaPanel.PrivacyPolicy)
        )
        assertEquals(
            MenuDestinationScreenMode.Feedback,
            MenuDestinationScreenMode.fromPanel(LisaPanel.Feedback)
        )
        assertEquals(
            MenuDestinationScreenMode.ReleaseNotes,
            MenuDestinationScreenMode.fromPanel(LisaPanel.ReleaseNotes)
        )
    }

    @Test
    fun stableActionIdentityDoesNotDependOnRenderedIndex() {
        val id = MenuDestinationActionId.savedProfile("persistent-id")
        val first = listOf(action("before"), action(id.value))
        val reordered = first.reversed()
        assertEquals(id, reordered.first { it.id == id }.id)
        assertNotEquals(first.indexOfFirst { it.id == id }, reordered.indexOfFirst { it.id == id })
    }

    @Test
    fun informationalHeadingsAreNeverSelectable() {
        val actions = listOf(
            action("heading", MenuDestinationActionType.InformationalBlock),
            action("choice", MenuDestinationActionType.Choice)
        )
        val state = open(actions)
        assertEquals(MenuDestinationActionId("choice"), state.selectedActionId)
    }

    @Test
    fun disabledControlsAreSkipped() {
        val actions = listOf(
            action("first"),
            action("available-soon", enabled = false),
            action("last")
        )
        val moved = MenuDestinationNavigationController.move(open(actions), actions, 1)
        assertEquals(MenuDestinationActionId("last"), moved.selectedActionId)
        assertEquals(1, moved.selectedIndex)
    }

    @Test
    fun movementIsOneActionAtATimeAndClampsWithoutWrapping() {
        val actions = listOf(action("one"), action("two"), action("three"))
        val initial = open(actions)
        assertEquals("one", initial.selectedActionId?.value)
        assertEquals("one", MenuDestinationNavigationController.move(initial, actions, -1).selectedActionId?.value)
        val second = MenuDestinationNavigationController.move(initial, actions, 1)
        assertEquals("two", second.selectedActionId?.value)
        val last = MenuDestinationNavigationController.move(second, actions, 20)
        assertEquals("three", last.selectedActionId?.value)
    }

    @Test
    fun selectActivatesCurrentEnabledActionByStableId() {
        val actions = listOf(action("one"), action("two"))
        val state = MenuDestinationNavigationController.move(open(actions), actions, 1)
        val result = MenuDestinationNavigationController.processSequence(
            1,
            1,
            state,
            actions,
            MenuDestinationNavigationCapabilities.Interactive
        )
        assertTrue(result is MenuDestinationSequenceResult.Activate)
        assertEquals(
            MenuDestinationActionId("two"),
            (result as MenuDestinationSequenceResult.Activate).actionId
        )
    }

    @Test
    fun scrollAnchorsMoveButCannotBeActivated() {
        val actions = listOf(action("section", MenuDestinationActionType.ScrollAnchor))
        val result = MenuDestinationNavigationController.processSequence(
            1,
            1,
            open(actions),
            actions,
            MenuDestinationNavigationCapabilities.ReadOnly
        )
        assertEquals(MenuDestinationSequenceResult.Unmatched, result)
    }

    @Test
    fun readOnlyCapabilitiesHideIrrelevantSelectAndHorizontalCommands() {
        val commands = MenuDestinationNavigationController.visibleCommands(
            capabilities = MenuDestinationNavigationCapabilities.ReadOnly,
            viewportPageCount = 2
        )
        assertTrue(MenuDestinationPanelCommand.PreviousPage in commands)
        assertTrue(MenuDestinationPanelCommand.NextPage in commands)
        assertTrue(MenuDestinationPanelCommand.Back in commands)
        assertTrue(MenuDestinationPanelCommand.Emergency in commands)
        assertFalse(MenuDestinationPanelCommand.Select in commands)
        assertFalse(MenuDestinationPanelCommand.MoveLeft in commands)
        assertFalse(MenuDestinationPanelCommand.MoveRight in commands)
    }

    @Test
    fun pageCommandsHiddenWhenContentFitsOneViewport() {
        val commands = MenuDestinationNavigationController.visibleCommands(
            capabilities = MenuDestinationNavigationCapabilities.Interactive,
            viewportPageCount = 1
        )
        assertFalse(MenuDestinationPanelCommand.PreviousPage in commands)
        assertFalse(MenuDestinationPanelCommand.NextPage in commands)
    }

    @Test
    fun formCapabilitiesExposeMovementSelectionAndHorizontalKeyboardMovement() {
        val capabilities = MenuDestinationNavigationController.capabilities(
            MenuDestinationScreenMode.Feedback
        )
        val commands = MenuDestinationNavigationController.visibleCommands(capabilities)
        assertTrue(MenuDestinationPanelCommand.MoveUp in commands)
        assertTrue(MenuDestinationPanelCommand.MoveDown in commands)
        assertTrue(MenuDestinationPanelCommand.Select in commands)
        assertTrue(MenuDestinationPanelCommand.MoveLeft in commands)
        assertTrue(MenuDestinationPanelCommand.MoveRight in commands)
    }

    @Test
    fun pageMovementUsesDirectViewportAnchors() {
        val state = open(emptyList())
        val next = MenuDestinationNavigationController.nextPage(state, 100, 250)
        assertEquals(1, next.viewportPage)
        assertEquals(100, next.scrollRequestPx)
        assertFalse(next.revealSelection)
    }

    @Test
    fun pageMovementClampsAtBothBoundariesWithoutWrapping() {
        val initial = open(emptyList())
        val previous = MenuDestinationNavigationController.previousPage(initial, 100, 250)
        assertEquals(0, previous.viewportPage)
        assertNull(previous.scrollRequestPx)
        val last = initial.copy(viewportPage = 2, viewportPageCount = 3)
        val next = MenuDestinationNavigationController.nextPage(last, 100, 200)
        assertEquals(2, next.viewportPage)
    }

    @Test
    fun invalidViewportMeasurementsAreSafe() {
        val state = open(emptyList())
        val next = MenuDestinationNavigationController.nextPage(state, 0, -10)
        assertEquals(0, next.viewportPage)
        assertEquals(1, next.viewportPageCount)
    }

    @Test
    fun pageMovementDoesNotChangeSelectionOrActivateAnything() {
        val actions = listOf(action("one"), action("two"))
        val state = open(actions)
        val paged = MenuDestinationNavigationController.nextPage(state, 100, 300)
        assertEquals(state.selectedActionId, paged.selectedActionId)
        assertEquals(state.selectedIndex, paged.selectedIndex)
    }

    @Test
    fun selectionRevealAndPageScrollUseMutuallyExclusiveCauses() {
        val actions = listOf(action("one"), action("two"))
        val selected = MenuDestinationNavigationController.move(open(actions), actions, 1)
        assertTrue(selected.revealSelection)
        assertNull(selected.scrollRequestPx)
        val paged = MenuDestinationNavigationController.nextPage(selected, 100, 300)
        assertFalse(paged.revealSelection)
        assertEquals(100, paged.scrollRequestPx)
    }

    @Test
    fun textEditingPreservesOriginalAndDraftSeparately() {
        val state = MenuDestinationNavigationController.beginTextEditing(
            open(listOf(action("field", MenuDestinationActionType.TextField))),
            MenuDestinationActionId("field"),
            "original"
        )
        val edited = MenuDestinationNavigationController.updateTextDraft(state, "draft")
        val stage = edited.interactionStage as MenuDestinationInteractionStage.TextEditing
        assertEquals("original", stage.originalText)
        assertEquals("draft", stage.draftText)
    }

    @Test
    fun cancellingTextEditingReturnsToBrowsingWithoutReplacingCallerState() {
        val state = MenuDestinationNavigationController.beginTextEditing(
            open(listOf(action("field", MenuDestinationActionType.TextField))),
            MenuDestinationActionId("field"),
            "kept"
        )
        val edited = MenuDestinationNavigationController.updateTextDraft(state, "discarded")
        val cancelled = MenuDestinationNavigationController.cancelCurrentStage(edited)
        assertEquals(MenuDestinationInteractionStage.Browsing, cancelled.interactionStage)
        assertTrue(cancelled.isActive)
    }

    @Test
    fun eyeKeyboardMovementReusesKeyboardNavigatorAndClamps() {
        val state = MenuDestinationNavigationController.beginTextEditing(
            open(emptyList()),
            MenuDestinationActionId.ProfileName,
            ""
        )
        val left = MenuDestinationNavigationController.moveTextCursor(
            state,
            PhraseComposerActionId.MoveLeft
        )
        val stage = left.interactionStage as MenuDestinationInteractionStage.TextEditing
        assertEquals(0, stage.cursorRow)
        assertEquals(0, stage.cursorCol)
        val right = MenuDestinationNavigationController.moveTextCursor(
            left,
            PhraseComposerActionId.MoveRight
        )
        assertEquals(1, (right.interactionStage as MenuDestinationInteractionStage.TextEditing).cursorCol)
    }

    @Test
    fun eyeKeyboardSelectAndTouchUseSameKeyAuthority() {
        val state = MenuDestinationNavigationController.beginTextEditing(
            open(emptyList()),
            MenuDestinationActionId.ProfileName,
            ""
        )
        val blink = MenuDestinationNavigationController.selectTextKey(state)
        val touch = MenuDestinationNavigationController.touchTextKey(state, 0, 0)
        assertEquals(
            (blink.interactionStage as MenuDestinationInteractionStage.TextEditing).draftText,
            (touch.interactionStage as MenuDestinationInteractionStage.TextEditing).draftText
        )
    }

    @Test
    fun backHierarchyCancelsEditingBeforeClosingDestination() {
        val browsing = open(emptyList())
        val editing = MenuDestinationNavigationController.beginTextEditing(
            browsing,
            MenuDestinationActionId.ProfileName,
            "name"
        )
        val firstBack = MenuDestinationNavigationController.cancelCurrentStage(editing)
        assertTrue(firstBack.isActive)
        assertEquals(MenuDestinationInteractionStage.Browsing, firstBack.interactionStage)
        val secondBack = MenuDestinationNavigationController.cancelCurrentStage(firstBack)
        assertFalse(secondBack.isActive)
    }

    @Test
    fun nestedBackReturnsToBrowsingBeforeTopLevelExit() {
        val nested = open(emptyList()).copy(
            panel = LisaPanel.VoiceDevice,
            interactionStage = MenuDestinationInteractionStage.Nested(
                LisaPanel.VoiceDevice,
                LisaPanel.Voice
            )
        )
        val back = MenuDestinationNavigationController.cancelCurrentStage(nested)
        assertTrue(back.isActive)
        assertEquals(MenuDestinationInteractionStage.Browsing, back.interactionStage)
    }

    @Test
    fun destinationModeInterceptsCommandsBeforeUnderlyingWorkspace() {
        val context = LisaGestureContext(
            activePanel = LisaPanel.AboutLisa,
            guidedOverlayActive = true,
            guidedScreenMode = GuidedOverlayScreenMode.CategoryMenu,
            isAdjustingPreference = false,
            phraseComposerMode = null
        )
        assertEquals(
            LisaInteractionMode.MainMenuDestination,
            ModeScopedGestureAuthority.activeMode(context)
        )
        assertEquals(
            GestureRoutingTarget.MainMenuDestination,
            ModeScopedGestureAuthority.routingTarget(context, 0, 4)
        )
    }

    @Test
    fun emergencyRemainsHighestPriorityInEveryDestination() {
        LisaPanel.entries
            .filter(MenuDestinationProductionUiAuthority::occupiesMainContentSlot)
            .forEach { panel ->
                val context = LisaGestureContext(
                    activePanel = panel,
                    guidedOverlayActive = true,
                    guidedScreenMode = GuidedOverlayScreenMode.Vocabulary,
                    isAdjustingPreference = false,
                    phraseComposerMode = null
                )
                assertEquals(
                    GestureRoutingTarget.Emergency,
                    ModeScopedGestureAuthority.routingTarget(
                        context,
                        EMERGENCY_LEFT_WINKS,
                        EMERGENCY_RIGHT_WINKS
                    )
                )
            }
    }

    @Test
    fun openMainMenuGestureCannotReplaceAnActiveDestination() {
        val context = LisaGestureContext(
            activePanel = LisaPanel.Feedback,
            guidedOverlayActive = false,
            guidedScreenMode = null,
            isAdjustingPreference = false,
            phraseComposerMode = null
        )
        assertEquals(
            GestureRoutingTarget.MainMenuDestination,
            ModeScopedGestureAuthority.routingTarget(
                context,
                GuidedModeNavigation.OPEN_MAIN_MENU_LEFT,
                GuidedModeNavigation.OPEN_MAIN_MENU_RIGHT
            )
        )
    }

    @Test
    fun mainMenuSelectionAndViewportSurviveDestinationRoundTrip() {
        val before = MainMenuNavigationState(
            isOpen = true,
            selectionIndex = 6,
            viewportPage = 2,
            viewportPageCount = 4
        )
        val closed = MainMenuController.close(before)
        val restored = MainMenuController.open(closed)
        assertEquals(6, restored.selectionIndex)
        assertEquals(2, restored.viewportPage)
        assertEquals(4, restored.viewportPageCount)
    }

    @Test
    fun everyInteractiveDestinationOccupiesFullScreenProductionSlot() {
        listOf(
            LisaPanel.MyCommunication,
            LisaPanel.Voice,
            LisaPanel.VoiceDevice,
            LisaPanel.Settings,
            LisaPanel.AboutLisa,
            LisaPanel.PrivacyPolicy,
            LisaPanel.Feedback,
            LisaPanel.ReleaseNotes
        ).forEach {
            assertTrue(MenuDestinationProductionUiAuthority.occupiesMainContentSlot(it))
        }
        assertFalse(MenuDestinationProductionUiAuthority.occupiesMainContentSlot(LisaPanel.Menu))
    }

    @Test
    fun profileRuntimeActionIdsReachAllLanguagesLevelsAndSavedProfiles() {
        assertEquals(
            PreferredLanguage.selectable.size,
            PreferredLanguage.selectable.map {
                MenuDestinationActionId.language(it.label)
            }.distinct().size
        )
        assertEquals(
            CommunicationLevel.entries.size,
            CommunicationLevel.entries.map {
                MenuDestinationActionId.communicationLevel(it.label)
            }.distinct().size
        )
        assertNotEquals(
            MenuDestinationActionId.savedProfile("one"),
            MenuDestinationActionId.savedProfile("two")
        )
    }

    @Test
    fun feedbackDraftPreservesConfirmedFieldsAndSupportsCancelPolicy() {
        val original = MenuFeedbackDraft(workedWell = "kept")
        val changed = original.withValue(MenuDestinationActionId.FeedbackWinks, "accurate")
        assertEquals("kept", changed.workedWell)
        assertEquals("accurate", changed.winkDetection)
        assertTrue(changed.hasContent)
        assertEquals(original, original.withValue(MenuDestinationActionId("unknown"), "ignored"))
    }

    @Test
    fun destinationSourcesReuseEyeKeyboardAndDoNotUseEditableSystemTextFields() {
        val root = source("LisaAccessibilityUi.kt")
        val release = source("LisaReleaseUi.kt")
        val workspace = source("MenuDestinationWorkspaceUi.kt")
        val profile = root.substringAfter("private fun MyCommunicationPanel(")
            .substringBefore("private fun PlaceholderPanel(")
        val feedback = release.substringAfter("fun FeedbackPanel(")
            .substringBefore("fun TestingChecklistPanel(")
        assertFalse(profile.contains("OutlinedTextField("))
        assertFalse(feedback.contains("OutlinedTextField("))
        assertTrue(workspace.contains("BottomAlignedEyeKeyboard("))
    }

    @Test
    fun phraseManagementKeepsItsExistingModeAndController() {
        val context = LisaGestureContext(
            activePanel = LisaPanel.VocabularyTraining,
            guidedOverlayActive = false,
            guidedScreenMode = null,
            isAdjustingPreference = false,
            phraseComposerMode = null
        )
        assertEquals(
            LisaInteractionMode.PhraseManagement,
            ModeScopedGestureAuthority.activeMode(context)
        )
        assertEquals(
            GestureRoutingTarget.PhraseManagement,
            ModeScopedGestureAuthority.routingTarget(context, 0, 2)
        )
    }

    private fun source(fileName: String): String {
        val candidates = listOf(
            File("src/main/java/com/idworx/lisa/$fileName"),
            File("app/src/main/java/com/idworx/lisa/$fileName")
        )
        return candidates.firstOrNull(File::exists)?.readText()
            ?: error("Missing production source $fileName")
    }
}
