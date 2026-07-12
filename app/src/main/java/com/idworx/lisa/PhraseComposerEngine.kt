package com.idworx.lisa

/**
 * Eye-controlled phrase composer — QWERTY keyboard, cursor navigation, and save confirmation.
 *
 * UI-agnostic; emits intents consumed by MainActivity which reuses [CustomPhraseEngine] for save.
 */
enum class PhraseComposerMode {
    DestinationCategorySelection,
    Keyboard,
    SaveConfirmation,
    CancelConfirm,
    Success
}

enum class PhraseComposerActionId {
    SelectCategory,
    MoveUp,
    MoveDown,
    MoveLeft,
    MoveRight,
    SelectKey,
    Backspace,
    Preview,
    Save,
    Back,
    ConfirmSave,
    CancelSave,
    CreateAnother,
    ReturnToCommunication,
    ConfirmCancel,
    KeepComposing,
    ToggleKeyboardLayout
}

data class PhraseComposerEntry(
    val left: Int,
    val right: Int,
    val label: String,
    val actionId: PhraseComposerActionId,
    val category: CustomPhraseEngine.CaregiverPhraseCategory? = null,
    val enabled: Boolean = true
) {
    val sequenceLabel: String get() = formatWinkSequenceShort(left, right)
}

data class PhraseComposerState(
    val mode: PhraseComposerMode = PhraseComposerMode.Keyboard,
    val selectedCategory: CustomPhraseEngine.CaregiverPhraseCategory? = null,
    val phraseText: String = "",
    val cursorRow: Int = 0,
    val cursorCol: Int = 0,
    val errorMessage: String? = null,
    val savedMapping: WinkMapping? = null,
    val pendingAllocatedSequence: Pair<Int, Int>? = null,
    val confirmedLeft: Int? = null,
    val confirmedRight: Int? = null,
    val keyboardLayoutMode: EyeKeyboardLayoutMode = EyeKeyboardLayoutMode.Letters
) {
    fun displayPhrase(): String = phraseText.uppercase()

    fun categoryLabel(uiStrings: LisaUiStrings): String =
        selectedCategory?.let { uiStrings.caregiverPhraseCategoryLabel(it) }.orEmpty()

    fun keyboardCursor(): KeyboardCursor = KeyboardCursor(cursorRow, cursorCol)

    fun withCursor(cursor: KeyboardCursor): PhraseComposerState =
        copy(cursorRow = cursor.row, cursorCol = cursor.col)
}

sealed class PhraseComposerSequenceResult {
    data class Navigate(val newState: PhraseComposerState) : PhraseComposerSequenceResult()
    data class Preview(val phrase: String) : PhraseComposerSequenceResult()
    data class Save(val category: CustomPhraseEngine.CaregiverPhraseCategory, val phrase: String) : PhraseComposerSequenceResult()
    object ReturnToCommunication : PhraseComposerSequenceResult()
    object ExitToPreviousPanel : PhraseComposerSequenceResult()
    object Unmatched : PhraseComposerSequenceResult()
}

object PhraseComposerController {

    /** RC7D.1 — Custom opens directly into keyboard compose mode. */
    fun keyboardEntryState(): PhraseComposerState = PhraseComposerState(
        mode = PhraseComposerMode.Keyboard,
        selectedCategory = null,
        phraseText = "",
        cursorRow = 0,
        cursorCol = 0,
        errorMessage = null,
        savedMapping = null,
        pendingAllocatedSequence = null,
        confirmedLeft = null,
        confirmedRight = null,
        keyboardLayoutMode = EyeKeyboardLayoutMode.Letters
    )

    fun initialState(): PhraseComposerState = keyboardEntryState()

    fun visibleEntries(state: PhraseComposerState, uiStrings: LisaUiStrings): List<PhraseComposerEntry> =
        when (state.mode) {
            PhraseComposerMode.DestinationCategorySelection -> categoryEntries(uiStrings)
            PhraseComposerMode.SaveConfirmation -> saveConfirmationEntries(uiStrings)
            PhraseComposerMode.CancelConfirm -> cancelConfirmEntries(uiStrings)
            PhraseComposerMode.Success -> successEntries(uiStrings)
            PhraseComposerMode.Keyboard -> emptyList()
        }

    fun commandPanelEntries(state: PhraseComposerState, uiStrings: LisaUiStrings): List<PhraseComposerEntry> =
        when (state.mode) {
            PhraseComposerMode.Keyboard -> keyboardCommandPanelEntries(state, uiStrings)
            PhraseComposerMode.DestinationCategorySelection -> destinationCommandPanelEntries(uiStrings)
            PhraseComposerMode.SaveConfirmation -> saveConfirmationCommandPanelEntries(uiStrings)
            PhraseComposerMode.CancelConfirm -> cancelConfirmCommandPanelEntries(uiStrings)
            PhraseComposerMode.Success -> successCommandPanelEntries(uiStrings)
        }

    fun screenTitle(state: PhraseComposerState, uiStrings: LisaUiStrings): String = when (state.mode) {
        PhraseComposerMode.DestinationCategorySelection -> uiStrings.phraseComposerDestinationStepTitle
        PhraseComposerMode.Keyboard -> uiStrings.phraseComposerKeyboardTitle
        PhraseComposerMode.SaveConfirmation -> uiStrings.phraseComposerSaveConfirmTitle
        PhraseComposerMode.CancelConfirm -> uiStrings.phraseComposerCancelConfirmTitle
        PhraseComposerMode.Success -> uiStrings.phraseComposerSuccessTitle
    }

    fun processSequence(
        left: Int,
        right: Int,
        state: PhraseComposerState,
        uiStrings: LisaUiStrings
    ): PhraseComposerSequenceResult {
        if (isEmergencySequence(left, right)) return PhraseComposerSequenceResult.Unmatched

        if (GuidedModeNavigation.isBackSequence(left, right)) {
            return handleBackGesture(state, uiStrings)
        }

        commandPanelEntries(state, uiStrings)
            .firstOrNull { it.enabled && it.left == left && it.right == right }
            ?.let { return dispatchAction(it, state, uiStrings, left, right) }

        visibleEntries(state, uiStrings)
            .firstOrNull { it.left == left && it.right == right }
            ?.let { return dispatchAction(it, state, uiStrings, left, right) }

        return PhraseComposerSequenceResult.Unmatched
    }

    fun applySaveResult(
        state: PhraseComposerState,
        result: CustomPhraseEngine.SavePhraseResult,
        uiStrings: LisaUiStrings
    ): PhraseComposerState = when (result) {
        is CustomPhraseEngine.SavePhraseResult.Success -> state.copy(
            mode = PhraseComposerMode.Success,
            savedMapping = result.mapping,
            selectedCategory = result.mapping.caregiverCategory,
            errorMessage = null,
            pendingAllocatedSequence = null,
            confirmedLeft = null,
            confirmedRight = null
        )
        is CustomPhraseEngine.SavePhraseResult.ValidationFailed -> state.copy(
            mode = PhraseComposerMode.Keyboard,
            errorMessage = validationMessage(result.reason, uiStrings),
            pendingAllocatedSequence = null,
            confirmedLeft = null,
            confirmedRight = null
        )
        CustomPhraseEngine.SavePhraseResult.NoSequenceAvailable -> state.copy(
            mode = PhraseComposerMode.Keyboard,
            errorMessage = uiStrings.phraseValidationNoSequence,
            pendingAllocatedSequence = null,
            confirmedLeft = null,
            confirmedRight = null
        )
    }

    fun validationMessage(reason: CustomPhraseEngine.PhraseValidationFailure, uiStrings: LisaUiStrings): String =
        when (reason) {
            CustomPhraseEngine.PhraseValidationFailure.Empty -> uiStrings.phraseValidationEmpty
            CustomPhraseEngine.PhraseValidationFailure.TooLong -> uiStrings.phraseValidationTooLong
            CustomPhraseEngine.PhraseValidationFailure.Duplicate -> uiStrings.phraseValidationDuplicate
        }

    fun everyVisibleEntryHasSequence(state: PhraseComposerState, uiStrings: LisaUiStrings): Boolean {
        val hasSequence = { entry: PhraseComposerEntry ->
            entry.left >= 0 && entry.right >= 0 &&
                (entry.left > 0 || entry.right > 0) &&
                entry.sequenceLabel.isNotBlank()
        }
        return visibleEntries(state, uiStrings).all(hasSequence) &&
            commandPanelEntries(state, uiStrings).all(hasSequence)
    }

    fun keyboardCommandActionIds(): Set<PhraseComposerActionId> = setOf(
        PhraseComposerActionId.MoveUp,
        PhraseComposerActionId.MoveDown,
        PhraseComposerActionId.MoveLeft,
        PhraseComposerActionId.MoveRight,
        PhraseComposerActionId.SelectKey,
        PhraseComposerActionId.Backspace,
        PhraseComposerActionId.Preview,
        PhraseComposerActionId.Save,
        PhraseComposerActionId.ToggleKeyboardLayout,
        PhraseComposerActionId.Back
    )

    fun toggleKeyboardLayoutLabel(state: PhraseComposerState, uiStrings: LisaUiStrings): String =
        when (state.keyboardLayoutMode) {
            EyeKeyboardLayoutMode.Letters -> uiStrings.phraseComposerPanelShowNumbers
            EyeKeyboardLayoutMode.Numbers -> uiStrings.phraseComposerPanelShowLetters
        }

    private fun handleBackGesture(
        state: PhraseComposerState,
        uiStrings: LisaUiStrings
    ): PhraseComposerSequenceResult = when (state.mode) {
        PhraseComposerMode.DestinationCategorySelection -> PhraseComposerSequenceResult.Navigate(
            state.copy(
                mode = PhraseComposerMode.Keyboard,
                errorMessage = null,
                confirmedLeft = GuidedModeNavigation.BACK_LEFT,
                confirmedRight = GuidedModeNavigation.BACK_RIGHT
            )
        )
        PhraseComposerMode.Keyboard -> if (state.phraseText.isNotBlank()) {
            PhraseComposerSequenceResult.Navigate(
                state.copy(
                    mode = PhraseComposerMode.CancelConfirm,
                    errorMessage = null,
                    confirmedLeft = GuidedModeNavigation.BACK_LEFT,
                    confirmedRight = GuidedModeNavigation.BACK_RIGHT
                )
            )
        } else {
            PhraseComposerSequenceResult.ExitToPreviousPanel
        }
        PhraseComposerMode.SaveConfirmation -> PhraseComposerSequenceResult.Navigate(
            state.copy(
                mode = PhraseComposerMode.Keyboard,
                pendingAllocatedSequence = null,
                errorMessage = null,
                confirmedLeft = null,
                confirmedRight = null
            )
        )
        PhraseComposerMode.CancelConfirm -> PhraseComposerSequenceResult.Navigate(
            state.copy(
                mode = PhraseComposerMode.Keyboard,
                errorMessage = null,
                confirmedLeft = null,
                confirmedRight = null
            )
        )
        PhraseComposerMode.Success -> PhraseComposerSequenceResult.ReturnToCommunication
    }

    private fun dispatchAction(
        match: PhraseComposerEntry,
        state: PhraseComposerState,
        uiStrings: LisaUiStrings,
        left: Int,
        right: Int
    ): PhraseComposerSequenceResult {
        return when (match.actionId) {
        PhraseComposerActionId.SelectCategory -> {
            val category = match.category ?: return PhraseComposerSequenceResult.Unmatched
            PhraseComposerSequenceResult.Navigate(
                state.copy(
                    mode = PhraseComposerMode.SaveConfirmation,
                    selectedCategory = category,
                    errorMessage = null,
                    pendingAllocatedSequence = null,
                    confirmedLeft = left,
                    confirmedRight = right
                )
            )
        }
        PhraseComposerActionId.MoveUp,
        PhraseComposerActionId.MoveDown,
        PhraseComposerActionId.MoveLeft,
        PhraseComposerActionId.MoveRight -> {
            val moved = KeyboardNavigator.move(state.keyboardCursor(), match.actionId, state.keyboardLayoutMode)
            PhraseComposerSequenceResult.Navigate(
                state.withCursor(moved).copy(
                    errorMessage = null,
                    confirmedLeft = left,
                    confirmedRight = right
                )
            )
        }
        PhraseComposerActionId.SelectKey -> handleSelectKey(state, left, right, uiStrings)
        PhraseComposerActionId.ToggleKeyboardLayout -> {
            val newMode = when (state.keyboardLayoutMode) {
                EyeKeyboardLayoutMode.Letters -> EyeKeyboardLayoutMode.Numbers
                EyeKeyboardLayoutMode.Numbers -> EyeKeyboardLayoutMode.Letters
            }
            val cursor = KeyboardLayout.initialCursor(newMode)
            PhraseComposerSequenceResult.Navigate(
                state.copy(
                    keyboardLayoutMode = newMode,
                    cursorRow = cursor.row,
                    cursorCol = cursor.col,
                    errorMessage = null,
                    confirmedLeft = left,
                    confirmedRight = right
                )
            )
        }
        PhraseComposerActionId.Backspace -> PhraseComposerSequenceResult.Navigate(
            state.copy(
                phraseText = KeyboardNavigator.backspace(state.phraseText),
                errorMessage = null,
                confirmedLeft = left,
                confirmedRight = right
            )
        )
        PhraseComposerActionId.Preview -> {
            val normalized = CustomPhraseEngine.normalizePhrase(state.phraseText)
            if (normalized.isBlank()) {
                PhraseComposerSequenceResult.Navigate(
                    state.copy(
                        errorMessage = uiStrings.phraseValidationEmpty,
                        confirmedLeft = left,
                        confirmedRight = right
                    )
                )
            } else {
                PhraseComposerSequenceResult.Preview(normalized)
            }
        }
        PhraseComposerActionId.Save -> {
            val normalized = CustomPhraseEngine.normalizePhrase(state.phraseText)
            if (normalized.isBlank()) {
                PhraseComposerSequenceResult.Navigate(
                    state.copy(
                        errorMessage = uiStrings.phraseValidationEmpty,
                        confirmedLeft = left,
                        confirmedRight = right
                    )
                )
            } else {
                PhraseComposerSequenceResult.Navigate(
                    state.copy(
                        mode = PhraseComposerMode.DestinationCategorySelection,
                        errorMessage = null,
                        selectedCategory = null,
                        pendingAllocatedSequence = null,
                        confirmedLeft = left,
                        confirmedRight = right
                    )
                )
            }
        }
        PhraseComposerActionId.ConfirmSave -> {
            val category = state.selectedCategory
                ?: return PhraseComposerSequenceResult.Unmatched
            val normalized = CustomPhraseEngine.normalizePhrase(state.phraseText)
            if (normalized.isBlank()) {
                PhraseComposerSequenceResult.Navigate(
                    state.copy(
                        mode = PhraseComposerMode.Keyboard,
                        errorMessage = uiStrings.phraseValidationEmpty,
                        confirmedLeft = left,
                        confirmedRight = right
                    )
                )
            } else {
                PhraseComposerSequenceResult.Save(category, normalized)
            }
        }
        PhraseComposerActionId.CancelSave -> PhraseComposerSequenceResult.Navigate(
            state.copy(
                mode = PhraseComposerMode.Keyboard,
                pendingAllocatedSequence = null,
                errorMessage = null,
                confirmedLeft = left,
                confirmedRight = right
            )
        )
        PhraseComposerActionId.Back -> handleBackGesture(state, uiStrings)
        PhraseComposerActionId.ConfirmCancel -> PhraseComposerSequenceResult.ExitToPreviousPanel
        PhraseComposerActionId.KeepComposing -> PhraseComposerSequenceResult.Navigate(
            state.copy(
                mode = PhraseComposerMode.Keyboard,
                errorMessage = null,
                confirmedLeft = left,
                confirmedRight = right
            )
        )
        PhraseComposerActionId.CreateAnother -> PhraseComposerSequenceResult.Navigate(keyboardEntryState())
        PhraseComposerActionId.ReturnToCommunication -> PhraseComposerSequenceResult.ReturnToCommunication
        }
    }

    private fun handleSelectKey(
        state: PhraseComposerState,
        left: Int,
        right: Int,
        uiStrings: LisaUiStrings
    ): PhraseComposerSequenceResult {
        val key = state.keyboardCursor().currentKey(state.keyboardLayoutMode)
            ?: return PhraseComposerSequenceResult.Unmatched
        val updated = KeyboardNavigator.appendSelectedKey(state.phraseText, key, state.keyboardLayoutMode)
            ?: return PhraseComposerSequenceResult.Navigate(
                state.copy(
                    errorMessage = if (key == ' ') {
                        null
                    } else {
                        uiStrings.phraseValidationTooLong
                    },
                    confirmedLeft = left,
                    confirmedRight = right
                )
            )
        return PhraseComposerSequenceResult.Navigate(
            state.copy(
                phraseText = updated,
                errorMessage = null,
                confirmedLeft = left,
                confirmedRight = right
            )
        )
    }

    private fun keyboardCommandPanelEntries(
        state: PhraseComposerState,
        uiStrings: LisaUiStrings
    ): List<PhraseComposerEntry> {
        val labelsByAction = mapOf(
            PhraseComposerActionId.MoveUp to uiStrings.phraseComposerPanelMoveUp,
            PhraseComposerActionId.MoveDown to uiStrings.phraseComposerPanelMoveDown,
            PhraseComposerActionId.MoveLeft to uiStrings.phraseComposerPanelMoveLeft,
            PhraseComposerActionId.MoveRight to uiStrings.phraseComposerPanelMoveRight,
            PhraseComposerActionId.SelectKey to uiStrings.phraseComposerPanelSelectKey,
            PhraseComposerActionId.Backspace to uiStrings.phraseComposerPanelBackspace,
            PhraseComposerActionId.Preview to uiStrings.phraseComposerPanelPreview,
            PhraseComposerActionId.Save to uiStrings.phraseComposerPanelSave,
            PhraseComposerActionId.ToggleKeyboardLayout to toggleKeyboardLayoutLabel(state, uiStrings),
            PhraseComposerActionId.Back to uiStrings.phraseComposerPanelBack
        )
        return PhraseComposerCommandSequences.keyboardPanelActionOrder.map { actionId ->
            val (panelLeft, panelRight) = PhraseComposerCommandSequences.sequenceFor(actionId)
                ?: error("Missing keyboard sequence for $actionId")
            PhraseComposerEntry(
                left = panelLeft,
                right = panelRight,
                label = labelsByAction.getValue(actionId),
                actionId = actionId
            )
        }
    }

    private fun destinationCommandPanelEntries(uiStrings: LisaUiStrings): List<PhraseComposerEntry> =
        listOf(
            PhraseComposerEntry(
                left = GuidedModeNavigation.BACK_LEFT,
                right = GuidedModeNavigation.BACK_RIGHT,
                label = uiStrings.phraseComposerPanelBack,
                actionId = PhraseComposerActionId.Back
            )
        )

    private fun saveConfirmationCommandPanelEntries(uiStrings: LisaUiStrings): List<PhraseComposerEntry> =
        listOf(
            PhraseComposerEntry(
                left = GuidedModeNavigation.BACK_LEFT,
                right = GuidedModeNavigation.BACK_RIGHT,
                label = uiStrings.phraseComposerPanelBack,
                actionId = PhraseComposerActionId.Back
            )
        )

    private fun cancelConfirmCommandPanelEntries(uiStrings: LisaUiStrings): List<PhraseComposerEntry> =
        listOf(
            PhraseComposerEntry(
                left = GuidedModeNavigation.BACK_LEFT,
                right = GuidedModeNavigation.BACK_RIGHT,
                label = uiStrings.phraseComposerPanelBack,
                actionId = PhraseComposerActionId.Back
            )
        )

    private fun successCommandPanelEntries(uiStrings: LisaUiStrings): List<PhraseComposerEntry> =
        listOf(
            PhraseComposerEntry(
                left = GuidedModeNavigation.BACK_LEFT,
                right = GuidedModeNavigation.BACK_RIGHT,
                label = uiStrings.phraseComposerPanelBack,
                actionId = PhraseComposerActionId.Back
            )
        )

    private fun categoryEntries(uiStrings: LisaUiStrings): List<PhraseComposerEntry> =
        CustomPhraseEngine.selectableCategories.mapIndexed { index, category ->
            val (left, right) = GuidedPageSequences.slotAt(index)
            PhraseComposerEntry(
                left = left,
                right = right,
                label = uiStrings.caregiverPhraseCategoryLabel(category),
                actionId = PhraseComposerActionId.SelectCategory,
                category = category
            )
        }

    private fun saveConfirmationEntries(uiStrings: LisaUiStrings): List<PhraseComposerEntry> =
        listOf(
            PhraseComposerEntry(
                left = GuidedPageSequences.leftAt(0),
                right = GuidedPageSequences.rightAt(0),
                label = uiStrings.phraseComposerConfirmSave,
                actionId = PhraseComposerActionId.ConfirmSave
            ),
            PhraseComposerEntry(
                left = GuidedPageSequences.leftAt(1),
                right = GuidedPageSequences.rightAt(1),
                label = uiStrings.phraseComposerCancelSave,
                actionId = PhraseComposerActionId.CancelSave
            )
        )

    private fun cancelConfirmEntries(uiStrings: LisaUiStrings): List<PhraseComposerEntry> =
        listOf(
            PhraseComposerEntry(
                left = GuidedPageSequences.leftAt(0),
                right = GuidedPageSequences.rightAt(0),
                label = uiStrings.phraseComposerConfirmCancel,
                actionId = PhraseComposerActionId.ConfirmCancel
            ),
            PhraseComposerEntry(
                left = GuidedPageSequences.leftAt(1),
                right = GuidedPageSequences.rightAt(1),
                label = uiStrings.phraseComposerKeepComposing,
                actionId = PhraseComposerActionId.KeepComposing
            )
        )

    private fun successEntries(uiStrings: LisaUiStrings): List<PhraseComposerEntry> =
        listOf(
            PhraseComposerEntry(
                left = GuidedPageSequences.leftAt(0),
                right = GuidedPageSequences.rightAt(0),
                label = uiStrings.phraseEditorCreateAnother,
                actionId = PhraseComposerActionId.CreateAnother
            ),
            PhraseComposerEntry(
                left = GuidedPageSequences.leftAt(1),
                right = GuidedPageSequences.rightAt(1),
                label = uiStrings.phraseEditorReturnToCommunication,
                actionId = PhraseComposerActionId.ReturnToCommunication
            )
        )
}
