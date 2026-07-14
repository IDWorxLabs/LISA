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
    DuplicateWarning,
    CancelConfirm,
    ConfirmDelete,
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
    ViewInCategory,
    ConfirmCancel,
    KeepComposing,
    ToggleKeyboardLayout,
    OpenDuplicateCategory,
    ChooseAnotherCategory,
    ContinueEditing,
    ConfirmDelete
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
    val keyboardLayoutMode: EyeKeyboardLayoutMode = EyeKeyboardLayoutMode.Letters,
    val keyboardShiftMode: KeyboardShiftMode = KeyboardShiftMode.Lowercase,
    val lastShiftTapEpochMs: Long = 0L,
    val duplicateMatch: DuplicatePhraseMatch? = null,
    /** Stable identity of the custom phrase being edited; null for create-phrase mode. */
    val editingIdentity: CustomPhraseIdentity? = null,
    /** When true, Success summary reflects an update rather than a create. */
    val wasEdit: Boolean = false,
    /** RC7D.10 — vocabulary page containing the verified saved phrase after Return to Communication. */
    val savedPhrasePageIndex: Int = 0,
    /**
     * Modes visited before the current mode (oldest → newest).
     * Back pops the last entry rather than using hardcoded destinations.
     */
    val navigationHistory: List<PhraseComposerMode> = emptyList()
) {
    fun displayPhrase(): String = phraseText.uppercase()

    fun isEditing(): Boolean = editingIdentity != null

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
    data class Update(
        val identity: CustomPhraseIdentity,
        val category: CustomPhraseEngine.CaregiverPhraseCategory,
        val phrase: String
    ) : PhraseComposerSequenceResult()
    data class Delete(val identity: CustomPhraseIdentity) : PhraseComposerSequenceResult()
    data class OpenExistingPhrase(val match: DuplicatePhraseMatch) : PhraseComposerSequenceResult()
    /** Explicit caregiver request to open the category/page of a saved or existing phrase. */
    data class ViewSavedCategory(
        val category: CustomPhraseEngine.CaregiverPhraseCategory,
        val phrasePageIndex: Int,
        val returnComposerState: PhraseComposerState
    ) : PhraseComposerSequenceResult()
    object ReturnToCommunication : PhraseComposerSequenceResult()
    object ExitToPreviousPanel : PhraseComposerSequenceResult()
    object Unmatched : PhraseComposerSequenceResult()
}

/** Live blink highlight level for composer list entries (RC7D.8). */
object PhraseComposerEntryHighlight {
    enum class Level { None, Partial, Full }

    fun level(entry: PhraseComposerEntry, leftWinkCount: Int, rightWinkCount: Int): Level {
        if (leftWinkCount == entry.left && rightWinkCount == entry.right) return Level.Full
        if (leftWinkCount > entry.left ||
            (leftWinkCount == entry.left && rightWinkCount > entry.right)
        ) {
            return Level.None
        }
        if (leftWinkCount == 0 && rightWinkCount == 0) return Level.None
        return Level.Partial
    }
}

object PhraseComposerController {

    /** RC7D.1 — Custom opens directly into keyboard compose mode. */
    fun keyboardEntryState(
        preferredCategory: CustomPhraseEngine.CaregiverPhraseCategory? = null
    ): PhraseComposerState = PhraseComposerState(
        mode = PhraseComposerMode.Keyboard,
        selectedCategory = preferredCategory,
        phraseText = "",
        cursorRow = 0,
        cursorCol = 0,
        errorMessage = null,
        savedMapping = null,
        pendingAllocatedSequence = null,
        confirmedLeft = null,
        confirmedRight = null,
        keyboardLayoutMode = EyeKeyboardLayoutMode.Letters,
        keyboardShiftMode = KeyboardShiftMode.Lowercase,
        lastShiftTapEpochMs = 0L,
        editingIdentity = null,
        wasEdit = false,
        navigationHistory = emptyList()
    )

    fun initialState(): PhraseComposerState = keyboardEntryState()

    fun visibleEntries(state: PhraseComposerState, uiStrings: LisaUiStrings): List<PhraseComposerEntry> =
        when (state.mode) {
            PhraseComposerMode.DestinationCategorySelection -> categoryEntries(uiStrings)
            PhraseComposerMode.SaveConfirmation -> emptyList()
            PhraseComposerMode.DuplicateWarning -> duplicateWarningEntries(state, uiStrings)
            PhraseComposerMode.CancelConfirm -> cancelConfirmEntries(uiStrings)
            PhraseComposerMode.ConfirmDelete -> emptyList()
            PhraseComposerMode.Success -> successEntries(state, uiStrings)
            PhraseComposerMode.Keyboard -> emptyList()
        }

    fun commandPanelEntries(state: PhraseComposerState, uiStrings: LisaUiStrings): List<PhraseComposerEntry> =
        when (state.mode) {
            PhraseComposerMode.Keyboard -> keyboardCommandPanelEntries(state, uiStrings)
            PhraseComposerMode.DestinationCategorySelection -> destinationCommandPanelEntries(uiStrings)
            PhraseComposerMode.SaveConfirmation -> saveConfirmationCommandPanelEntries(state, uiStrings)
            PhraseComposerMode.DuplicateWarning -> duplicateWarningCommandPanelEntries(state, uiStrings)
            PhraseComposerMode.CancelConfirm -> cancelConfirmCommandPanelEntries(uiStrings)
            PhraseComposerMode.ConfirmDelete -> confirmDeleteCommandPanelEntries(uiStrings)
            PhraseComposerMode.Success -> successCommandPanelEntries(uiStrings)
        }

    fun screenTitle(state: PhraseComposerState, uiStrings: LisaUiStrings): String = when (state.mode) {
        PhraseComposerMode.DestinationCategorySelection -> uiStrings.phraseComposerDestinationStepTitle
        PhraseComposerMode.Keyboard ->
            if (state.isEditing()) uiStrings.phraseManagementEditTitle else uiStrings.phraseComposerKeyboardTitle
        PhraseComposerMode.SaveConfirmation ->
            if (state.isEditing()) uiStrings.phraseComposerEditConfirmTitle else uiStrings.phraseComposerSaveConfirmTitle
        PhraseComposerMode.DuplicateWarning -> uiStrings.phraseDuplicateWarningTitle
        PhraseComposerMode.CancelConfirm -> uiStrings.phraseComposerCancelConfirmTitle
        PhraseComposerMode.ConfirmDelete -> uiStrings.phraseManagementDeleteConfirmTitle
        PhraseComposerMode.Success ->
            if (state.wasEdit) uiStrings.phraseComposerUpdateSuccessTitle else uiStrings.phraseComposerSuccessTitle
    }

    /** Opens the eye-controlled composer with an existing custom phrase preloaded for editing. */
    fun editEntryState(mapping: WinkMapping): PhraseComposerState = PhraseComposerState(
        mode = PhraseComposerMode.Keyboard,
        selectedCategory = mapping.caregiverCategory,
        phraseText = mapping.customPhrase.orEmpty(),
        cursorRow = 0,
        cursorCol = 0,
        errorMessage = null,
        savedMapping = null,
        pendingAllocatedSequence = null,
        confirmedLeft = null,
        confirmedRight = null,
        keyboardLayoutMode = EyeKeyboardLayoutMode.Letters,
        keyboardShiftMode = KeyboardShiftMode.Lowercase,
        lastShiftTapEpochMs = 0L,
        editingIdentity = CustomPhraseIdentity.from(mapping),
        wasEdit = false
    )

    /** Eye-controlled delete confirmation for a custom phrase. */
    fun deleteConfirmState(mapping: WinkMapping): PhraseComposerState = PhraseComposerState(
        mode = PhraseComposerMode.ConfirmDelete,
        selectedCategory = mapping.caregiverCategory,
        phraseText = mapping.customPhrase.orEmpty(),
        editingIdentity = CustomPhraseIdentity.from(mapping),
        errorMessage = null
    )

    /** RC7D.7 / RC7D.8 — caregiver touch: move cursor then apply canonical keyboard slot. */
    fun processTouchKey(
        row: Int,
        col: Int,
        state: PhraseComposerState,
        uiStrings: LisaUiStrings
    ): PhraseComposerSequenceResult {
        if (state.mode != PhraseComposerMode.Keyboard) return PhraseComposerSequenceResult.Unmatched
        return applyKeyboardSlotAt(
            state = state,
            row = row,
            col = col,
            uiStrings = uiStrings
        )
    }

    fun processSequence(
        left: Int,
        right: Int,
        state: PhraseComposerState,
        uiStrings: LisaUiStrings,
        runtimeContext: PhraseComposerRuntimeContext? = null
    ): PhraseComposerSequenceResult {
        if (isEmergencySequence(left, right)) return PhraseComposerSequenceResult.Unmatched

        if (GuidedModeNavigation.isBackSequence(left, right)) {
            return handleBackGesture(state, uiStrings)
        }

        commandPanelEntries(state, uiStrings)
            .firstOrNull { it.enabled && it.left == left && it.right == right }
            ?.let { return dispatchAction(it, state, uiStrings, left, right, runtimeContext) }

        visibleEntries(state, uiStrings)
            .firstOrNull { it.left == left && it.right == right }
            ?.let { return dispatchAction(it, state, uiStrings, left, right, runtimeContext) }

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
            confirmedRight = null,
            navigationHistory = if (state.mode == PhraseComposerMode.Success) {
                state.navigationHistory
            } else {
                state.navigationHistory + state.mode
            }
        )
        is CustomPhraseEngine.SavePhraseResult.ValidationFailed -> {
            val duplicateMatch = result.duplicateMatch
            if (result.reason == CustomPhraseEngine.PhraseValidationFailure.Duplicate && duplicateMatch != null) {
                state.copy(
                    mode = PhraseComposerMode.DuplicateWarning,
                    duplicateMatch = duplicateMatch,
                    errorMessage = null,
                    pendingAllocatedSequence = null,
                    confirmedLeft = null,
                    confirmedRight = null,
                    navigationHistory = if (state.mode == PhraseComposerMode.DuplicateWarning) {
                        state.navigationHistory
                    } else {
                        state.navigationHistory + state.mode
                    }
                )
            } else {
                state.copy(
                    mode = PhraseComposerMode.Keyboard,
                    errorMessage = validationMessage(result.reason, uiStrings, duplicateMatch),
                    pendingAllocatedSequence = null,
                    confirmedLeft = null,
                    confirmedRight = null
                )
            }
        }
        CustomPhraseEngine.SavePhraseResult.NoSequenceAvailable -> state.copy(
            mode = PhraseComposerMode.Keyboard,
            errorMessage = uiStrings.phraseValidationNoSequence,
            pendingAllocatedSequence = null,
            confirmedLeft = null,
            confirmedRight = null
        )
    }

    fun applyTransactionSaveResult(
        state: PhraseComposerState,
        result: PhraseSaveTransactionResult,
        uiStrings: LisaUiStrings
    ): PhraseComposerState = when (result) {
        is PhraseSaveTransactionResult.Success -> state.copy(
            mode = PhraseComposerMode.Success,
            savedMapping = result.mapping,
            selectedCategory = result.category,
            savedPhrasePageIndex = result.phrasePageIndex,
            errorMessage = null,
            pendingAllocatedSequence = null,
            confirmedLeft = null,
            confirmedRight = null,
            wasEdit = state.isEditing(),
            editingIdentity = if (state.isEditing()) CustomPhraseIdentity.from(result.mapping) else null,
            navigationHistory = if (state.mode == PhraseComposerMode.Success) {
                state.navigationHistory
            } else {
                state.navigationHistory + state.mode
            }
        )
        is PhraseSaveTransactionResult.Failed -> {
            val duplicateMatch = result.duplicateMatch
            when {
                result.reason == PhraseSaveFailureReason.Duplicate && duplicateMatch != null ->
                    state.copy(
                        mode = PhraseComposerMode.DuplicateWarning,
                        duplicateMatch = duplicateMatch,
                        errorMessage = null,
                        pendingAllocatedSequence = state.pendingAllocatedSequence,
                        confirmedLeft = null,
                        confirmedRight = null,
                        navigationHistory = if (state.mode == PhraseComposerMode.DuplicateWarning) {
                            state.navigationHistory
                        } else {
                            state.navigationHistory + state.mode
                        }
                    )
                result.reason == PhraseSaveFailureReason.NoSequenceAvailable ->
                    state.copy(
                        mode = PhraseComposerMode.SaveConfirmation,
                        errorMessage = uiStrings.phraseValidationNoSequence,
                        pendingAllocatedSequence = null,
                        confirmedLeft = null,
                        confirmedRight = null
                    )
                else -> state.copy(
                    mode = PhraseComposerMode.SaveConfirmation,
                    errorMessage = transactionFailureMessage(result.reason, uiStrings, duplicateMatch),
                    pendingAllocatedSequence = state.pendingAllocatedSequence,
                    confirmedLeft = null,
                    confirmedRight = null
                )
            }
        }
    }

    fun transactionFailureMessage(
        reason: PhraseSaveFailureReason,
        uiStrings: LisaUiStrings,
        duplicateMatch: DuplicatePhraseMatch? = null
    ): String = when (reason) {
        PhraseSaveFailureReason.Empty -> uiStrings.phraseValidationEmpty
        PhraseSaveFailureReason.TooLong -> uiStrings.phraseValidationTooLong
        PhraseSaveFailureReason.Duplicate ->
            duplicateMatch?.let { uiStrings.phraseDuplicateExistsMessage(it) }
                ?: uiStrings.phraseValidationDuplicate
        PhraseSaveFailureReason.NoSequenceAvailable -> uiStrings.phraseValidationNoSequence
        PhraseSaveFailureReason.StorageWriteFailed -> uiStrings.phraseSaveFailed
        PhraseSaveFailureReason.StorageVerificationFailed -> uiStrings.phraseStorageVerificationFailed
        PhraseSaveFailureReason.RuntimeCatalogMissing -> uiStrings.phraseRuntimeCatalogFailed
        PhraseSaveFailureReason.PhraseNotFound -> uiStrings.phraseManagementNotFound
        PhraseSaveFailureReason.MalformedStoredData -> uiStrings.phraseStorageVerificationFailed
    }

    fun validationMessage(
        reason: CustomPhraseEngine.PhraseValidationFailure,
        uiStrings: LisaUiStrings,
        duplicateMatch: DuplicatePhraseMatch? = null
    ): String = when (reason) {
        CustomPhraseEngine.PhraseValidationFailure.Empty -> uiStrings.phraseValidationEmpty
        CustomPhraseEngine.PhraseValidationFailure.TooLong -> uiStrings.phraseValidationTooLong
        CustomPhraseEngine.PhraseValidationFailure.Duplicate ->
            duplicateMatch?.let { uiStrings.phraseDuplicateExistsMessage(it) }
                ?: uiStrings.phraseValidationDuplicate
    }

    fun duplicateMessage(match: DuplicatePhraseMatch, uiStrings: LisaUiStrings): String =
        uiStrings.phraseDuplicateExistsMessage(match)

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

    private fun navigateTo(
        state: PhraseComposerState,
        nextMode: PhraseComposerMode,
        left: Int? = null,
        right: Int? = null,
        clearDuplicate: Boolean = true,
        clearPendingSequence: Boolean = false,
        clearError: Boolean = true,
        transform: (PhraseComposerState) -> PhraseComposerState = { it }
    ): PhraseComposerSequenceResult.Navigate {
        val history = if (state.mode == nextMode) {
            state.navigationHistory
        } else {
            state.navigationHistory + state.mode
        }
        val base = transform(state).copy(
            mode = nextMode,
            navigationHistory = history,
            errorMessage = if (clearError) null else transform(state).errorMessage,
            duplicateMatch = if (clearDuplicate) null else transform(state).duplicateMatch,
            pendingAllocatedSequence = if (clearPendingSequence) null else transform(state).pendingAllocatedSequence,
            confirmedLeft = left,
            confirmedRight = right
        )
        return PhraseComposerSequenceResult.Navigate(base)
    }

    private fun navigateBack(state: PhraseComposerState): PhraseComposerSequenceResult {
        if (state.navigationHistory.isNotEmpty()) {
            val previous = state.navigationHistory.last()
            val rest = state.navigationHistory.dropLast(1)
            return PhraseComposerSequenceResult.Navigate(
                state.copy(
                    mode = previous,
                    navigationHistory = rest,
                    errorMessage = null,
                    duplicateMatch = if (previous == PhraseComposerMode.DuplicateWarning) {
                        state.duplicateMatch
                    } else {
                        null
                    },
                    pendingAllocatedSequence = when (previous) {
                        PhraseComposerMode.SaveConfirmation -> state.pendingAllocatedSequence
                        else -> null
                    },
                    confirmedLeft = GuidedModeNavigation.BACK_LEFT,
                    confirmedRight = GuidedModeNavigation.BACK_RIGHT
                )
            )
        }
        return when (state.mode) {
            PhraseComposerMode.Keyboard -> if (state.phraseText.isNotBlank()) {
                navigateTo(state, PhraseComposerMode.CancelConfirm, GuidedModeNavigation.BACK_LEFT, GuidedModeNavigation.BACK_RIGHT)
            } else {
                PhraseComposerSequenceResult.ExitToPreviousPanel
            }
            PhraseComposerMode.DestinationCategorySelection ->
                PhraseComposerSequenceResult.Navigate(
                    state.copy(
                        mode = PhraseComposerMode.Keyboard,
                        errorMessage = null,
                        confirmedLeft = GuidedModeNavigation.BACK_LEFT,
                        confirmedRight = GuidedModeNavigation.BACK_RIGHT
                    )
                )
            PhraseComposerMode.SaveConfirmation ->
                PhraseComposerSequenceResult.Navigate(
                    state.copy(
                        mode = if (state.isEditing()) {
                            PhraseComposerMode.Keyboard
                        } else {
                            PhraseComposerMode.DestinationCategorySelection
                        },
                        pendingAllocatedSequence = null,
                        errorMessage = null,
                        confirmedLeft = GuidedModeNavigation.BACK_LEFT,
                        confirmedRight = GuidedModeNavigation.BACK_RIGHT
                    )
                )
            PhraseComposerMode.DuplicateWarning ->
                PhraseComposerSequenceResult.Navigate(
                    state.copy(
                        mode = PhraseComposerMode.SaveConfirmation,
                        duplicateMatch = null,
                        errorMessage = null,
                        confirmedLeft = GuidedModeNavigation.BACK_LEFT,
                        confirmedRight = GuidedModeNavigation.BACK_RIGHT
                    )
                )
            PhraseComposerMode.CancelConfirm ->
                PhraseComposerSequenceResult.Navigate(
                    state.copy(
                        mode = PhraseComposerMode.Keyboard,
                        errorMessage = null,
                        confirmedLeft = null,
                        confirmedRight = null
                    )
                )
            PhraseComposerMode.ConfirmDelete -> PhraseComposerSequenceResult.ExitToPreviousPanel
            PhraseComposerMode.Success ->
                PhraseComposerSequenceResult.Navigate(
                    state.copy(
                        mode = PhraseComposerMode.SaveConfirmation,
                        errorMessage = null,
                        confirmedLeft = GuidedModeNavigation.BACK_LEFT,
                        confirmedRight = GuidedModeNavigation.BACK_RIGHT
                    )
                )
        }
    }

    private fun handleBackGesture(
        state: PhraseComposerState,
        uiStrings: LisaUiStrings
    ): PhraseComposerSequenceResult = navigateBack(state)

    private fun dispatchAction(
        match: PhraseComposerEntry,
        state: PhraseComposerState,
        uiStrings: LisaUiStrings,
        left: Int,
        right: Int,
        runtimeContext: PhraseComposerRuntimeContext?
    ): PhraseComposerSequenceResult {
        return when (match.actionId) {
        PhraseComposerActionId.SelectCategory -> {
            val category = match.category ?: return PhraseComposerSequenceResult.Unmatched
            navigateTo(
                state = state,
                nextMode = PhraseComposerMode.SaveConfirmation,
                left = left,
                right = right,
                clearPendingSequence = true
            ) { it.copy(selectedCategory = category) }
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
                    keyboardShiftMode = KeyboardShiftMode.Lowercase,
                    lastShiftTapEpochMs = 0L,
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
            } else if (state.isEditing() && state.selectedCategory != null) {
                // Edit keeps category; confirm before persist — duplicates checked on Confirm Save.
                navigateTo(
                    state = state,
                    nextMode = PhraseComposerMode.SaveConfirmation,
                    left = left,
                    right = right
                ) {
                    it.copy(
                        pendingAllocatedSequence = it.pendingAllocatedSequence
                            ?: it.editingIdentity?.let { id -> id.left to id.right }
                    )
                }
            } else {
                // Create: always choose category first — do not infer or open a category.
                // Keep any caregiver-chosen entry category as a soft preselect only.
                navigateTo(
                    state = state,
                    nextMode = PhraseComposerMode.DestinationCategorySelection,
                    left = left,
                    right = right,
                    clearPendingSequence = true
                )
            }
        }
        PhraseComposerActionId.ConfirmSave -> {
            val category = state.selectedCategory
                ?: return PhraseComposerSequenceResult.Unmatched
            val normalized = CustomPhraseEngine.normalizePhrase(state.phraseText)
            if (normalized.isBlank()) {
                navigateTo(
                    state = state,
                    nextMode = PhraseComposerMode.Keyboard,
                    left = left,
                    right = right
                ) { it.copy(errorMessage = uiStrings.phraseValidationEmpty) }
            } else if (
                state.savedMapping != null &&
                state.savedMapping.caregiverCategory == category &&
                PhraseDuplicateEngine.canonicalIdentity(state.savedMapping.customPhrase.orEmpty()) ==
                PhraseDuplicateEngine.canonicalIdentity(normalized)
            ) {
                // Already saved — re-opening Confirm from Success Back must not save again.
                navigateTo(
                    state = state,
                    nextMode = PhraseComposerMode.Success,
                    left = left,
                    right = right,
                    clearDuplicate = true
                )
            } else {
                duplicateWarningState(state, normalized, runtimeContext, uiStrings, left, right)
                    ?: if (state.isEditing()) {
                        PhraseComposerSequenceResult.Update(
                            identity = state.editingIdentity!!,
                            category = category,
                            phrase = normalized
                        )
                    } else {
                        PhraseComposerSequenceResult.Save(category, normalized)
                    }
            }
        }
        PhraseComposerActionId.CancelSave -> navigateBack(state)
        PhraseComposerActionId.Back -> handleBackGesture(state, uiStrings)
        PhraseComposerActionId.ConfirmCancel -> PhraseComposerSequenceResult.ExitToPreviousPanel
        PhraseComposerActionId.KeepComposing -> navigateBack(state)
        PhraseComposerActionId.CreateAnother -> PhraseComposerSequenceResult.Navigate(keyboardEntryState())
        PhraseComposerActionId.ReturnToCommunication,
        PhraseComposerActionId.ViewInCategory -> viewSavedCategoryResult(state)
        PhraseComposerActionId.OpenDuplicateCategory -> {
            val match = state.duplicateMatch ?: return PhraseComposerSequenceResult.Unmatched
            PhraseComposerSequenceResult.OpenExistingPhrase(match)
        }
        PhraseComposerActionId.ChooseAnotherCategory -> {
            val hist = state.navigationHistory
            val destIdx = hist.indexOfLast { it == PhraseComposerMode.DestinationCategorySelection }
            val restoredHistory = if (destIdx >= 0) hist.take(destIdx) else listOf(PhraseComposerMode.Keyboard)
            PhraseComposerSequenceResult.Navigate(
                state.copy(
                    mode = PhraseComposerMode.DestinationCategorySelection,
                    navigationHistory = restoredHistory,
                    duplicateMatch = null,
                    errorMessage = null,
                    pendingAllocatedSequence = null,
                    confirmedLeft = left,
                    confirmedRight = right
                )
            )
        }
        PhraseComposerActionId.ContinueEditing -> {
            val hist = state.navigationHistory
            val keyboardIdx = hist.indexOfLast { it == PhraseComposerMode.Keyboard }
            val restoredHistory = if (keyboardIdx >= 0) hist.take(keyboardIdx) else emptyList()
            PhraseComposerSequenceResult.Navigate(
                state.copy(
                    mode = PhraseComposerMode.Keyboard,
                    navigationHistory = restoredHistory,
                    duplicateMatch = null,
                    errorMessage = null,
                    confirmedLeft = left,
                    confirmedRight = right
                )
            )
        }
        PhraseComposerActionId.ConfirmDelete -> {
            val identity = state.editingIdentity ?: return PhraseComposerSequenceResult.Unmatched
            PhraseComposerSequenceResult.Delete(identity)
        }
        }
    }

    private fun viewSavedCategoryResult(state: PhraseComposerState): PhraseComposerSequenceResult {
        val mapping = state.savedMapping ?: return PhraseComposerSequenceResult.Unmatched
        val category = mapping.caregiverCategory
            ?: state.selectedCategory
            ?: return PhraseComposerSequenceResult.Unmatched
        return PhraseComposerSequenceResult.ViewSavedCategory(
            category = category,
            phrasePageIndex = state.savedPhrasePageIndex,
            returnComposerState = state
        )
    }

    private fun duplicateWarningState(
        state: PhraseComposerState,
        normalizedPhrase: String,
        runtimeContext: PhraseComposerRuntimeContext?,
        uiStrings: LisaUiStrings,
        left: Int,
        right: Int
    ): PhraseComposerSequenceResult.Navigate? {
        val context = runtimeContext ?: return null
        val mappingsForDuplicate = if (state.editingIdentity != null) {
            context.customMappings.filter {
                !(it.isCustom &&
                    it.left == state.editingIdentity.left &&
                    it.right == state.editingIdentity.right &&
                    PhraseDuplicateEngine.canonicalIdentity(it.customPhrase.orEmpty()) ==
                    PhraseDuplicateEngine.canonicalIdentity(state.editingIdentity.phrase))
            }
        } else {
            context.customMappings
        }
        val duplicate = PhraseDuplicateEngine.findDuplicate(
            rawPhrase = normalizedPhrase,
            customMappings = mappingsForDuplicate,
            language = context.language,
            uiStrings = uiStrings
        ) ?: return null
        return navigateTo(
            state = state,
            nextMode = PhraseComposerMode.DuplicateWarning,
            left = left,
            right = right,
            clearDuplicate = false,
            clearPendingSequence = true
        ) { it.copy(duplicateMatch = duplicate) }
    }

    private fun handleSelectKey(
        state: PhraseComposerState,
        left: Int,
        right: Int,
        uiStrings: LisaUiStrings
    ): PhraseComposerSequenceResult = applyKeyboardSlotAt(
        state = state,
        row = state.cursorRow,
        col = state.cursorCol,
        uiStrings = uiStrings,
        confirmedLeft = left,
        confirmedRight = right
    )

    private fun applyKeyboardSlotAt(
        state: PhraseComposerState,
        row: Int,
        col: Int,
        uiStrings: LisaUiStrings,
        confirmedLeft: Int? = null,
        confirmedRight: Int? = null
    ): PhraseComposerSequenceResult {
        val mode = state.keyboardLayoutMode
        val slot = KeyboardLayout.slotAt(mode, row, col) ?: return PhraseComposerSequenceResult.Unmatched
        val withCursor = state.withCursor(KeyboardCursor(row, col))
        return applyKeyboardSlot(
            state = withCursor,
            slot = slot,
            uiStrings = uiStrings,
            confirmedLeft = confirmedLeft,
            confirmedRight = confirmedRight
        )
    }

    private fun applyKeyboardSlot(
        state: PhraseComposerState,
        slot: KeyboardSlot,
        uiStrings: LisaUiStrings,
        confirmedLeft: Int?,
        confirmedRight: Int?
    ): PhraseComposerSequenceResult {
        val left = confirmedLeft ?: state.confirmedLeft
        val right = confirmedRight ?: state.confirmedRight
        val now = System.currentTimeMillis()
        return when (slot) {
            is KeyboardSlot.Character -> {
                val updated = KeyboardNavigator.appendSelectedKey(
                    currentPhrase = state.phraseText,
                    key = slot.char,
                    layoutMode = state.keyboardLayoutMode,
                    shiftMode = state.keyboardShiftMode
                ) ?: return PhraseComposerSequenceResult.Navigate(
                    state.copy(
                        errorMessage = if (state.phraseText.length >= CustomPhraseEngine.MAX_PHRASE_LENGTH) {
                            uiStrings.phraseValidationTooLong
                        } else {
                            null
                        },
                        confirmedLeft = left,
                        confirmedRight = right
                    )
                )
                PhraseComposerSequenceResult.Navigate(
                    state.copy(
                        phraseText = updated,
                        keyboardShiftMode = KeyboardNavigator.afterLetterInserted(state.keyboardShiftMode),
                        errorMessage = null,
                        confirmedLeft = left,
                        confirmedRight = right
                    )
                )
            }
            KeyboardSlot.Space -> {
                val updated = KeyboardNavigator.appendSpace(state.phraseText)
                    ?: return PhraseComposerSequenceResult.Navigate(
                        state.copy(
                            errorMessage = null,
                            confirmedLeft = left,
                            confirmedRight = right
                        )
                    )
                PhraseComposerSequenceResult.Navigate(
                    state.copy(
                        phraseText = updated,
                        errorMessage = null,
                        confirmedLeft = left,
                        confirmedRight = right
                    )
                )
            }
            KeyboardSlot.Backspace -> PhraseComposerSequenceResult.Navigate(
                state.copy(
                    phraseText = KeyboardNavigator.backspace(state.phraseText),
                    errorMessage = null,
                    confirmedLeft = left,
                    confirmedRight = right
                )
            )
            KeyboardSlot.Shift -> PhraseComposerSequenceResult.Navigate(
                state.copy(
                    keyboardShiftMode = KeyboardNavigator.nextShiftMode(
                        current = state.keyboardShiftMode,
                        lastTapEpochMs = state.lastShiftTapEpochMs,
                        nowEpochMs = now
                    ),
                    lastShiftTapEpochMs = now,
                    errorMessage = null,
                    confirmedLeft = left,
                    confirmedRight = right
                )
            )
        }
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
            PhraseComposerActionId.Save to if (state.isEditing()) {
                uiStrings.phraseManagementSaveEdit
            } else {
                uiStrings.phraseComposerChooseCategoryAction
            },
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

    private fun saveConfirmationCommandPanelEntries(
        state: PhraseComposerState,
        uiStrings: LisaUiStrings
    ): List<PhraseComposerEntry> =
        listOf(
            PhraseComposerEntry(
                left = GuidedModeNavigation.SELECT_LEFT,
                right = GuidedModeNavigation.SELECT_RIGHT,
                label = if (state.isEditing()) {
                    uiStrings.phraseManagementSaveEdit
                } else {
                    uiStrings.phraseComposerConfirmSave
                },
                actionId = PhraseComposerActionId.ConfirmSave
            ),
            PhraseComposerEntry(
                left = GuidedModeNavigation.BACK_LEFT,
                right = GuidedModeNavigation.BACK_RIGHT,
                label = uiStrings.phraseComposerPanelBack,
                actionId = PhraseComposerActionId.Back
            )
        )

    private fun confirmDeleteCommandPanelEntries(uiStrings: LisaUiStrings): List<PhraseComposerEntry> =
        listOf(
            PhraseComposerEntry(
                left = GuidedModeNavigation.SELECT_LEFT,
                right = GuidedModeNavigation.SELECT_RIGHT,
                label = uiStrings.phraseManagementDeleteConfirmAction,
                actionId = PhraseComposerActionId.ConfirmDelete
            ),
            PhraseComposerEntry(
                left = GuidedModeNavigation.BACK_LEFT,
                right = GuidedModeNavigation.BACK_RIGHT,
                label = uiStrings.phraseComposerPanelBack,
                actionId = PhraseComposerActionId.Back
            )
        )

    private fun duplicateWarningEntries(
        state: PhraseComposerState,
        uiStrings: LisaUiStrings
    ): List<PhraseComposerEntry> {
        val categoryLabel = state.duplicateMatch?.category?.let { uiStrings.caregiverPhraseCategoryLabel(it) }
            ?: uiStrings.phraseComposerPanelBack
        return listOf(
            PhraseComposerEntry(
                left = GuidedPageSequences.leftAt(0),
                right = GuidedPageSequences.rightAt(0),
                label = uiStrings.phraseDuplicateChooseAnotherCategory,
                actionId = PhraseComposerActionId.ChooseAnotherCategory
            ),
            PhraseComposerEntry(
                left = GuidedPageSequences.leftAt(1),
                right = GuidedPageSequences.rightAt(1),
                label = uiStrings.phraseDuplicateContinueEditing,
                actionId = PhraseComposerActionId.ContinueEditing
            ),
            PhraseComposerEntry(
                left = GuidedPageSequences.leftAt(2),
                right = GuidedPageSequences.rightAt(2),
                label = uiStrings.phraseDuplicateViewExistingPhrase(categoryLabel),
                actionId = PhraseComposerActionId.OpenDuplicateCategory
            )
        )
    }

    private fun duplicateWarningCommandPanelEntries(
        state: PhraseComposerState,
        uiStrings: LisaUiStrings
    ): List<PhraseComposerEntry> =
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

    private fun successEntries(state: PhraseComposerState, uiStrings: LisaUiStrings): List<PhraseComposerEntry> {
        val categoryLabel = state.savedMapping?.caregiverCategory?.let { uiStrings.caregiverPhraseCategoryLabel(it) }
            ?: state.selectedCategory?.let { uiStrings.caregiverPhraseCategoryLabel(it) }
            ?: uiStrings.phraseComposerPanelBack
        return listOf(
            PhraseComposerEntry(
                left = GuidedPageSequences.leftAt(0),
                right = GuidedPageSequences.rightAt(0),
                label = uiStrings.phraseComposerViewInCategory(categoryLabel),
                actionId = PhraseComposerActionId.ViewInCategory
            ),
            PhraseComposerEntry(
                left = GuidedPageSequences.leftAt(1),
                right = GuidedPageSequences.rightAt(1),
                label = uiStrings.phraseEditorCreateAnother,
                actionId = PhraseComposerActionId.CreateAnother
            )
        )
    }
}
