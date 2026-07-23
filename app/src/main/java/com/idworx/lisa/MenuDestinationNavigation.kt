package com.idworx.lisa

/**
 * RC7D.31 — one mode-scoped navigation model for every screen reached from Main Menu.
 *
 * Stable action IDs are deliberately independent of rendered list positions. Runtime entries
 * (profiles and installed voices) use namespaced IDs generated from their persistent identity.
 */
sealed interface MenuDestinationScreenMode {
    data object CommunicationProfile : MenuDestinationScreenMode
    data object PhraseManagement : MenuDestinationScreenMode
    data object Voice : MenuDestinationScreenMode
    data object Settings : MenuDestinationScreenMode
    data object AboutLisa : MenuDestinationScreenMode
    data object PrivacyPolicy : MenuDestinationScreenMode
    data object Feedback : MenuDestinationScreenMode
    data object ReleaseNotes : MenuDestinationScreenMode

    companion object {
        fun fromPanel(panel: LisaPanel): MenuDestinationScreenMode? = when (panel) {
            LisaPanel.MyCommunication -> CommunicationProfile
            LisaPanel.VocabularyTraining -> PhraseManagement
            LisaPanel.Voice,
            LisaPanel.VoiceDevice,
            LisaPanel.VoicePremium,
            LisaPanel.VoiceMyVoice,
            LisaPanel.VoiceFamily -> Voice
            LisaPanel.Settings,
            LisaPanel.Recalibration,
            LisaPanel.DeveloperTools,
            LisaPanel.TestingChecklist -> Settings
            LisaPanel.AboutLisa -> AboutLisa
            LisaPanel.PrivacyPolicy -> PrivacyPolicy
            LisaPanel.Feedback -> Feedback
            LisaPanel.ReleaseNotes -> ReleaseNotes
            else -> null
        }
    }
}

@JvmInline
value class MenuDestinationActionId(val value: String) {
    init {
        require(value.isNotBlank()) { "Menu destination action identity must not be blank" }
    }

    companion object {
        val Back = MenuDestinationActionId("navigation.back")
        val ProfileActive = MenuDestinationActionId("profile.active")
        val ProfileName = MenuDestinationActionId("profile.name")
        val ProfileNew = MenuDestinationActionId("profile.new")
        val ProfileDelete = MenuDestinationActionId("profile.delete")
        val VoiceDevice = MenuDestinationActionId("voice.device")
        val VoicePremium = MenuDestinationActionId("voice.premium")
        val VoiceMyVoice = MenuDestinationActionId("voice.my_voice")
        val VoiceFamily = MenuDestinationActionId("voice.family")
        val VoiceTest = MenuDestinationActionId("voice.test")
        val VoiceInstallData = MenuDestinationActionId("voice.install_data")
        val VoiceSystemSettings = MenuDestinationActionId("voice.system_settings")
        val FeedbackWorkedWell = MenuDestinationActionId("feedback.worked_well")
        val FeedbackConfusing = MenuDestinationActionId("feedback.confusing")
        val FeedbackWinks = MenuDestinationActionId("feedback.winks")
        val FeedbackSpeech = MenuDestinationActionId("feedback.speech")
        val FeedbackSave = MenuDestinationActionId("feedback.save")

        fun language(storedValue: String) =
            MenuDestinationActionId("profile.language.$storedValue")

        fun communicationLevel(storedValue: String) =
            MenuDestinationActionId("profile.level.$storedValue")

        fun savedProfile(profileId: String) =
            MenuDestinationActionId("profile.saved.$profileId")

        fun installedVoice(voiceName: String) =
            MenuDestinationActionId("voice.installed.$voiceName")

        fun setting(key: String) = MenuDestinationActionId("settings.$key")
        fun section(key: String) = MenuDestinationActionId("section.$key")
    }
}

enum class MenuDestinationActionType {
    Navigation,
    Toggle,
    Choice,
    Button,
    TextField,
    InformationalBlock,
    ScrollAnchor,
    Save,
    Cancel,
    Back;

    val canReceiveFocus: Boolean
        get() = this != InformationalBlock

    val canActivate: Boolean
        get() = this !in setOf(InformationalBlock, ScrollAnchor)
}

data class MenuDestinationAction(
    val id: MenuDestinationActionId,
    val label: String,
    val actionType: MenuDestinationActionType,
    val isEnabled: Boolean = true,
    val sectionId: String? = null,
    val selected: Boolean = false
) {
    val canReceiveFocus: Boolean
        get() = actionType.canReceiveFocus && isEnabled
}

data class MenuDestinationNavigationCapabilities(
    val supportsItemMovement: Boolean,
    val supportsPageMovement: Boolean,
    val supportsHorizontalMovement: Boolean = false,
    val supportsSelection: Boolean,
    val supportsSave: Boolean = false,
    val supportsCancel: Boolean = false,
    val supportsTextEntry: Boolean = false
) {
    companion object {
        val ReadOnly = MenuDestinationNavigationCapabilities(
            supportsItemMovement = true,
            supportsPageMovement = true,
            supportsSelection = false
        )
        val Interactive = MenuDestinationNavigationCapabilities(
            supportsItemMovement = true,
            supportsPageMovement = true,
            supportsSelection = true
        )
        val Form = Interactive.copy(
            supportsSave = true,
            supportsCancel = true,
            supportsTextEntry = true
        )
    }
}

sealed interface MenuDestinationInteractionStage {
    data object Browsing : MenuDestinationInteractionStage
    data class TextEditing(
        val actionId: MenuDestinationActionId,
        val originalText: String,
        val draftText: String,
        val cursorRow: Int = 0,
        val cursorCol: Int = 0,
        val layoutMode: EyeKeyboardLayoutMode = EyeKeyboardLayoutMode.Letters,
        val shiftMode: KeyboardShiftMode = KeyboardShiftMode.Lowercase,
        val fieldEditingStage: FeedbackFieldEditingStage =
            FeedbackFieldEditingStage.Keyboard,
        val requiresReview: Boolean = false
    ) : MenuDestinationInteractionStage
    data class Confirmation(val confirmation: MenuDestinationConfirmation) :
        MenuDestinationInteractionStage
    data class Nested(val panel: LisaPanel, val parentPanel: LisaPanel) :
        MenuDestinationInteractionStage
}

sealed interface FeedbackFieldEditingStage {
    data object Keyboard : FeedbackFieldEditingStage
    data object Review : FeedbackFieldEditingStage
}

data class MenuFeedbackDraft(
    val workedWell: String = "",
    val confusing: String = "",
    val winkDetection: String = "",
    val speechTiming: String = ""
) {
    fun valueFor(id: MenuDestinationActionId): String = when (id) {
        MenuDestinationActionId.FeedbackWorkedWell -> workedWell
        MenuDestinationActionId.FeedbackConfusing -> confusing
        MenuDestinationActionId.FeedbackWinks -> winkDetection
        MenuDestinationActionId.FeedbackSpeech -> speechTiming
        else -> ""
    }

    fun withValue(id: MenuDestinationActionId, value: String): MenuFeedbackDraft = when (id) {
        MenuDestinationActionId.FeedbackWorkedWell -> copy(workedWell = value)
        MenuDestinationActionId.FeedbackConfusing -> copy(confusing = value)
        MenuDestinationActionId.FeedbackWinks -> copy(winkDetection = value)
        MenuDestinationActionId.FeedbackSpeech -> copy(speechTiming = value)
        else -> this
    }

    val hasContent: Boolean
        get() = workedWell.isNotBlank() || confusing.isNotBlank() ||
            winkDetection.isNotBlank() || speechTiming.isNotBlank()
}

data class MenuDestinationConfirmation(
    val actionId: MenuDestinationActionId,
    val message: String
)

data class MenuDestinationNavigationState(
    val destination: MainMenuDestination,
    val panel: LisaPanel = destination.panel,
    val isActive: Boolean = false,
    val selectedActionId: MenuDestinationActionId? = null,
    val selectedIndex: Int = 0,
    val viewportPage: Int = 0,
    val viewportPageCount: Int = 1,
    val scrollRequestPx: Int? = null,
    val revealSelection: Boolean = false,
    val interactionStage: MenuDestinationInteractionStage =
        MenuDestinationInteractionStage.Browsing,
    val pendingConfirmation: MenuDestinationConfirmation? = null
)

enum class MenuDestinationPanelCommand {
    MoveUp,
    MoveDown,
    PreviousPage,
    NextPage,
    MoveLeft,
    MoveRight,
    Select,
    DoneEditing,
    ContinueEditing,
    Save,
    Cancel,
    Back,
    Emergency
}

object FeedbackKeyboardNavigationAuthority {
    val keyboardCommands = listOf(
        MenuDestinationPanelCommand.MoveUp,
        MenuDestinationPanelCommand.MoveDown,
        MenuDestinationPanelCommand.MoveLeft,
        MenuDestinationPanelCommand.MoveRight,
        MenuDestinationPanelCommand.Select,
        MenuDestinationPanelCommand.DoneEditing,
        MenuDestinationPanelCommand.Back,
        MenuDestinationPanelCommand.Emergency
    )

    val directionCommands = keyboardCommands.take(5)

    fun sequence(command: MenuDestinationPanelCommand): Pair<Int, Int>? = when (command) {
        MenuDestinationPanelCommand.MoveUp -> 2 to 0
        MenuDestinationPanelCommand.MoveDown -> 0 to 2
        MenuDestinationPanelCommand.MoveLeft -> 2 to 1
        MenuDestinationPanelCommand.MoveRight -> 1 to 2
        MenuDestinationPanelCommand.Select -> 1 to 1
        MenuDestinationPanelCommand.DoneEditing -> 3 to 2
        MenuDestinationPanelCommand.Back -> 2 to 2
        MenuDestinationPanelCommand.Emergency -> 6 to 0
        else -> null
    }
}

sealed interface MenuDestinationSequenceResult {
    data class Navigate(val state: MenuDestinationNavigationState) :
        MenuDestinationSequenceResult
    data class Activate(
        val actionId: MenuDestinationActionId,
        val state: MenuDestinationNavigationState
    ) : MenuDestinationSequenceResult
    data class MoveHorizontal(
        val direction: Int,
        val state: MenuDestinationNavigationState
    ) : MenuDestinationSequenceResult
    data class Back(val state: MenuDestinationNavigationState) :
        MenuDestinationSequenceResult
    data object Unmatched : MenuDestinationSequenceResult
}

object MenuDestinationNavigationController {

    fun capabilities(mode: MenuDestinationScreenMode): MenuDestinationNavigationCapabilities =
        when (mode) {
            MenuDestinationScreenMode.AboutLisa,
            MenuDestinationScreenMode.PrivacyPolicy,
            MenuDestinationScreenMode.ReleaseNotes ->
                MenuDestinationNavigationCapabilities.ReadOnly
            MenuDestinationScreenMode.CommunicationProfile,
            MenuDestinationScreenMode.Feedback ->
                MenuDestinationNavigationCapabilities.Form.copy(
                    supportsHorizontalMovement = true
                )
            MenuDestinationScreenMode.PhraseManagement ->
                MenuDestinationNavigationCapabilities.Interactive
            MenuDestinationScreenMode.Voice ->
                MenuDestinationNavigationCapabilities.Interactive.copy(
                    supportsHorizontalMovement = true
                )
            // Primary Settings fits on one page after simplification — no Previous/Next Page.
            MenuDestinationScreenMode.Settings ->
                PrimarySettingsNavigationAuthority.capabilities()
        }

    fun visibleCommands(
        capabilities: MenuDestinationNavigationCapabilities,
        viewportPageCount: Int = 1
    ): List<MenuDestinationPanelCommand> = buildList {
        if (capabilities.supportsItemMovement) {
            add(MenuDestinationPanelCommand.MoveUp)
            add(MenuDestinationPanelCommand.MoveDown)
        }
        // Only advertise paging when content actually spans multiple viewport pages.
        if (capabilities.supportsPageMovement && viewportPageCount > 1) {
            add(MenuDestinationPanelCommand.PreviousPage)
            add(MenuDestinationPanelCommand.NextPage)
        }
        if (capabilities.supportsHorizontalMovement) {
            add(MenuDestinationPanelCommand.MoveLeft)
            add(MenuDestinationPanelCommand.MoveRight)
        }
        if (capabilities.supportsSelection) add(MenuDestinationPanelCommand.Select)
        add(MenuDestinationPanelCommand.Back)
        add(MenuDestinationPanelCommand.Emergency)
    }

    fun open(
        destination: MainMenuDestination,
        panel: LisaPanel,
        actions: List<MenuDestinationAction>
    ): MenuDestinationNavigationState {
        val focusable = focusableActions(actions)
        return MenuDestinationNavigationState(
            destination = destination,
            panel = panel,
            isActive = true,
            selectedActionId = focusable.firstOrNull()?.id,
            selectedIndex = 0,
            revealSelection = focusable.isNotEmpty()
        )
    }

    fun updateActions(
        state: MenuDestinationNavigationState,
        actions: List<MenuDestinationAction>
    ): MenuDestinationNavigationState {
        val focusable = focusableActions(actions)
        if (focusable.isEmpty()) {
            return state.copy(selectedActionId = null, selectedIndex = 0)
        }
        val retained = focusable.indexOfFirst { it.id == state.selectedActionId }
        val index = if (retained >= 0) retained else state.selectedIndex.coerceIn(0, focusable.lastIndex)
        return state.copy(
            selectedIndex = index,
            selectedActionId = focusable[index].id
        )
    }

    fun move(
        state: MenuDestinationNavigationState,
        actions: List<MenuDestinationAction>,
        delta: Int
    ): MenuDestinationNavigationState {
        val current = updateActions(state, actions)
        val focusable = focusableActions(actions)
        if (focusable.isEmpty()) return current
        val next = (current.selectedIndex + delta).coerceIn(0, focusable.lastIndex)
        return current.copy(
            selectedIndex = next,
            selectedActionId = focusable[next].id,
            revealSelection = next != current.selectedIndex,
            scrollRequestPx = null
        )
    }

    fun previousPage(
        state: MenuDestinationNavigationState,
        viewportHeightPx: Int,
        maxScrollPx: Int
    ): MenuDestinationNavigationState =
        movePage(state, -1, viewportHeightPx, maxScrollPx)

    fun nextPage(
        state: MenuDestinationNavigationState,
        viewportHeightPx: Int,
        maxScrollPx: Int
    ): MenuDestinationNavigationState =
        movePage(state, 1, viewportHeightPx, maxScrollPx)

    private fun movePage(
        state: MenuDestinationNavigationState,
        delta: Int,
        viewportHeightPx: Int,
        maxScrollPx: Int
    ): MenuDestinationNavigationState {
        val count = CategoryViewportPaging.pageCount(viewportHeightPx, maxScrollPx)
        val current = CategoryViewportPaging.clampPage(state.viewportPage, count)
        val target = (current + delta).coerceIn(0, count - 1)
        if (target == current) return state.copy(viewportPageCount = count)
        return state.copy(
            viewportPage = target,
            viewportPageCount = count,
            scrollRequestPx = CategoryViewportPaging.pageAnchorOffsetPx(
                target,
                viewportHeightPx,
                maxScrollPx
            ),
            revealSelection = false
        )
    }

    fun syncViewportMetrics(
        state: MenuDestinationNavigationState,
        viewportHeightPx: Int,
        maxScrollPx: Int,
        scrollPx: Int
    ): MenuDestinationNavigationState = state.copy(
        viewportPage = CategoryViewportPaging.currentPageForScroll(
            scrollPx,
            viewportHeightPx,
            maxScrollPx
        ),
        viewportPageCount = CategoryViewportPaging.pageCount(
            viewportHeightPx,
            maxScrollPx
        ),
        scrollRequestPx = null
    )

    fun beginTextEditing(
        state: MenuDestinationNavigationState,
        actionId: MenuDestinationActionId,
        currentText: String,
        requiresReview: Boolean = false
    ): MenuDestinationNavigationState = state.copy(
        interactionStage = MenuDestinationInteractionStage.TextEditing(
            actionId = actionId,
            originalText = currentText,
            draftText = currentText,
            requiresReview = requiresReview
        )
    )

    fun finishKeyboardEditing(
        state: MenuDestinationNavigationState
    ): MenuDestinationNavigationState {
        val stage = state.interactionStage as? MenuDestinationInteractionStage.TextEditing
            ?: return state
        return if (stage.requiresReview) {
            state.copy(
                interactionStage = stage.copy(
                    fieldEditingStage = FeedbackFieldEditingStage.Review
                )
            )
        } else {
            confirmTextEditing(state)
        }
    }

    fun continueKeyboardEditing(
        state: MenuDestinationNavigationState
    ): MenuDestinationNavigationState {
        val stage = state.interactionStage as? MenuDestinationInteractionStage.TextEditing
            ?: return state
        return state.copy(
            interactionStage = stage.copy(
                fieldEditingStage = FeedbackFieldEditingStage.Keyboard
            )
        )
    }

    fun updateTextDraft(
        state: MenuDestinationNavigationState,
        draft: String
    ): MenuDestinationNavigationState {
        val stage = state.interactionStage as? MenuDestinationInteractionStage.TextEditing
            ?: return state
        return state.copy(interactionStage = stage.copy(draftText = draft))
    }

    fun moveTextCursor(
        state: MenuDestinationNavigationState,
        direction: PhraseComposerActionId
    ): MenuDestinationNavigationState {
        val stage = state.interactionStage as? MenuDestinationInteractionStage.TextEditing
            ?: return state
        if (stage.fieldEditingStage != FeedbackFieldEditingStage.Keyboard) return state
        val cursor = KeyboardNavigator.move(
            KeyboardCursor(stage.cursorRow, stage.cursorCol),
            direction,
            stage.layoutMode
        )
        return state.copy(
            interactionStage = stage.copy(cursorRow = cursor.row, cursorCol = cursor.col)
        )
    }

    fun selectTextKey(state: MenuDestinationNavigationState): MenuDestinationNavigationState {
        val stage = state.interactionStage as? MenuDestinationInteractionStage.TextEditing
            ?: return state
        if (stage.fieldEditingStage != FeedbackFieldEditingStage.Keyboard) return state
        return applyTextSlot(
            state,
            KeyboardLayout.slotAt(stage.layoutMode, stage.cursorRow, stage.cursorCol)
        )
    }

    fun touchTextKey(
        state: MenuDestinationNavigationState,
        row: Int,
        col: Int
    ): MenuDestinationNavigationState {
        val stage = state.interactionStage as? MenuDestinationInteractionStage.TextEditing
            ?: return state
        if (stage.fieldEditingStage != FeedbackFieldEditingStage.Keyboard) return state
        return applyTextSlot(
            state.copy(interactionStage = stage.copy(cursorRow = row, cursorCol = col)),
            KeyboardLayout.slotAt(stage.layoutMode, row, col)
        )
    }

    private fun applyTextSlot(
        state: MenuDestinationNavigationState,
        slot: KeyboardSlot?
    ): MenuDestinationNavigationState {
        val stage = state.interactionStage as? MenuDestinationInteractionStage.TextEditing
            ?: return state
        val updated = when (slot) {
            is KeyboardSlot.Character -> {
                val text = KeyboardNavigator.appendSelectedKey(
                    stage.draftText,
                    slot.char,
                    stage.layoutMode,
                    stage.shiftMode
                ) ?: stage.draftText
                stage.copy(
                    draftText = text,
                    shiftMode = KeyboardNavigator.afterLetterInserted(stage.shiftMode)
                )
            }
            KeyboardSlot.Space -> stage.copy(
                draftText = KeyboardNavigator.appendSpace(stage.draftText) ?: stage.draftText
            )
            KeyboardSlot.Backspace -> stage.copy(
                draftText = KeyboardNavigator.backspace(stage.draftText)
            )
            KeyboardSlot.Shift -> stage.copy(
                shiftMode = KeyboardNavigator.nextShiftMode(
                    stage.shiftMode,
                    0L,
                    System.currentTimeMillis()
                )
            )
            null -> stage
        }
        return state.copy(interactionStage = updated)
    }

    fun confirmTextEditing(state: MenuDestinationNavigationState): MenuDestinationNavigationState =
        state.copy(interactionStage = MenuDestinationInteractionStage.Browsing)

    fun cancelCurrentStage(state: MenuDestinationNavigationState): MenuDestinationNavigationState =
        when (state.interactionStage) {
            is MenuDestinationInteractionStage.TextEditing,
            is MenuDestinationInteractionStage.Confirmation ->
                state.copy(
                    interactionStage = MenuDestinationInteractionStage.Browsing,
                    pendingConfirmation = null
                )
            is MenuDestinationInteractionStage.Nested ->
                state.copy(interactionStage = MenuDestinationInteractionStage.Browsing)
            MenuDestinationInteractionStage.Browsing -> state.copy(isActive = false)
        }

    fun processSequence(
        left: Int,
        right: Int,
        state: MenuDestinationNavigationState,
        actions: List<MenuDestinationAction>,
        capabilities: MenuDestinationNavigationCapabilities,
        viewportHeightPx: Int = 0,
        maxScrollPx: Int = 0
    ): MenuDestinationSequenceResult {
        if (!state.isActive) return MenuDestinationSequenceResult.Unmatched
        val current = updateActions(state, actions)
        val focusable = focusableActions(actions)
        val pageCount = CategoryViewportPaging.pageCount(viewportHeightPx, maxScrollPx)
        val canPreviousPage = CategoryViewportPaging.canGoToPreviousPage(current.viewportPage)
        val canNextPage = CategoryViewportPaging.canGoToNextPage(current.viewportPage, pageCount)
        val selected = focusable.getOrNull(current.selectedIndex)
        return when {
            GuidedModeNavigation.isPreviousSequence(left, right) &&
                capabilities.supportsItemMovement -> {
                if (current.selectedIndex <= 0) {
                    MenuDestinationSequenceResult.Unmatched
                } else {
                    MenuDestinationSequenceResult.Navigate(move(state, actions, -1))
                }
            }
            GuidedModeNavigation.isNextSequence(left, right) &&
                capabilities.supportsItemMovement -> {
                if (current.selectedIndex >= focusable.lastIndex) {
                    MenuDestinationSequenceResult.Unmatched
                } else {
                    MenuDestinationSequenceResult.Navigate(move(state, actions, 1))
                }
            }
            GuidedModeNavigation.isPreviousCategoryPageSequence(left, right) &&
                capabilities.supportsPageMovement &&
                pageCount > 1 &&
                canPreviousPage ->
                MenuDestinationSequenceResult.Navigate(
                    previousPage(state, viewportHeightPx, maxScrollPx)
                )
            GuidedModeNavigation.isNextCategoryPageSequence(left, right) &&
                capabilities.supportsPageMovement &&
                pageCount > 1 &&
                canNextPage ->
                MenuDestinationSequenceResult.Navigate(
                    nextPage(state, viewportHeightPx, maxScrollPx)
                )
            left == 2 && right == 1 &&
                capabilities.supportsHorizontalMovement &&
                selected?.actionType == MenuDestinationActionType.Choice ->
                MenuDestinationSequenceResult.MoveHorizontal(-1, state)
            left == 1 && right == 2 &&
                capabilities.supportsHorizontalMovement &&
                selected?.actionType == MenuDestinationActionType.Choice ->
                MenuDestinationSequenceResult.MoveHorizontal(1, state)
            GuidedModeNavigation.isSelectSequence(left, right) &&
                capabilities.supportsSelection -> {
                if (selected?.actionType?.canActivate == true) {
                    MenuDestinationSequenceResult.Activate(selected.id, state)
                } else {
                    MenuDestinationSequenceResult.Unmatched
                }
            }
            GuidedModeNavigation.isBackSequence(left, right) ->
                MenuDestinationSequenceResult.Back(cancelCurrentStage(state))
            else -> MenuDestinationSequenceResult.Unmatched
        }
    }

    fun commandIsEnabled(
        command: MenuDestinationPanelCommand,
        state: MenuDestinationNavigationState,
        actions: List<MenuDestinationAction>,
        canPreviousPage: Boolean,
        canNextPage: Boolean,
        keyboardEditing: Boolean = false
    ): Boolean {
        val focusableCount = focusableActions(actions).size
        val selected = actions.firstOrNull { it.id == state.selectedActionId }
        return when (command) {
            MenuDestinationPanelCommand.MoveUp ->
                keyboardEditing || state.selectedIndex > 0
            MenuDestinationPanelCommand.MoveDown ->
                keyboardEditing || state.selectedIndex < focusableCount - 1
            MenuDestinationPanelCommand.PreviousPage -> canPreviousPage
            MenuDestinationPanelCommand.NextPage -> canNextPage
            MenuDestinationPanelCommand.MoveLeft,
            MenuDestinationPanelCommand.MoveRight ->
                keyboardEditing || selected?.actionType == MenuDestinationActionType.Choice
            else -> true
        }
    }

    private fun focusableActions(actions: List<MenuDestinationAction>) =
        actions.filter(MenuDestinationAction::canReceiveFocus)
}

object MenuDestinationProductionUiAuthority {
    val supportedPanels: Set<LisaPanel> = setOf(
        LisaPanel.MyCommunication,
        LisaPanel.Voice,
        LisaPanel.VoiceDevice,
        LisaPanel.VoicePremium,
        LisaPanel.VoiceMyVoice,
        LisaPanel.VoiceFamily,
        LisaPanel.Settings,
        LisaPanel.Recalibration,
        LisaPanel.DeveloperTools,
        LisaPanel.AboutLisa,
        LisaPanel.PrivacyPolicy,
        LisaPanel.Feedback,
        LisaPanel.TestingChecklist,
        LisaPanel.ReleaseNotes
    )

    fun occupiesMainContentSlot(panel: LisaPanel): Boolean = panel in supportedPanels

    fun destinationForPanel(panel: LisaPanel): MainMenuDestination? = when (panel) {
        LisaPanel.MyCommunication -> MainMenuDestination.CommunicationProfile
        LisaPanel.Voice,
        LisaPanel.VoiceDevice,
        LisaPanel.VoicePremium,
        LisaPanel.VoiceMyVoice,
        LisaPanel.VoiceFamily -> MainMenuDestination.Voice
        LisaPanel.Settings,
        LisaPanel.Recalibration,
        LisaPanel.DeveloperTools,
        LisaPanel.TestingChecklist -> MainMenuDestination.Settings
        LisaPanel.AboutLisa -> MainMenuDestination.AboutLisa
        LisaPanel.PrivacyPolicy -> MainMenuDestination.PrivacyPolicy
        LisaPanel.Feedback -> MainMenuDestination.Feedback
        LisaPanel.ReleaseNotes -> MainMenuDestination.ReleaseNotes
        else -> null
    }
}
