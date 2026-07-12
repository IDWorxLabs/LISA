package com.idworx.lisa

/**
 * Keyboard composer command grid row layout (RC7D.4).
 * Pure layout spec for unit tests and responsive grid building.
 */
object ComposerCommandGridLayout {

    const val WIDE_GRID_MIN_WIDTH_DP: Int = 340

    val navigationRow: List<PhraseComposerActionId> = listOf(
        PhraseComposerActionId.MoveUp,
        PhraseComposerActionId.MoveDown,
        PhraseComposerActionId.MoveLeft,
        PhraseComposerActionId.MoveRight,
        PhraseComposerActionId.SelectKey
    )

    val actionRow: List<PhraseComposerActionId> = listOf(
        PhraseComposerActionId.Backspace,
        PhraseComposerActionId.Preview,
        PhraseComposerActionId.Save,
        PhraseComposerActionId.ToggleKeyboardLayout,
        PhraseComposerActionId.Back
    )

    val narrowNavigationRow: List<PhraseComposerActionId> = listOf(
        PhraseComposerActionId.MoveUp,
        PhraseComposerActionId.MoveDown,
        PhraseComposerActionId.MoveLeft,
        PhraseComposerActionId.MoveRight
    )

    val narrowSelectRow: List<PhraseComposerActionId> = listOf(
        PhraseComposerActionId.SelectKey,
        PhraseComposerActionId.Backspace,
        PhraseComposerActionId.Preview,
        PhraseComposerActionId.Save
    )

    val narrowUtilityRow: List<PhraseComposerActionId> = listOf(
        PhraseComposerActionId.ToggleKeyboardLayout,
        PhraseComposerActionId.Back
    )

    fun commandRows(screenWidthDp: Int): List<List<PhraseComposerActionId>> =
        if (screenWidthDp >= WIDE_GRID_MIN_WIDTH_DP) {
            listOf(navigationRow, actionRow)
        } else {
            listOf(narrowNavigationRow, narrowSelectRow, narrowUtilityRow)
        }

    fun resolveEntry(
        actionId: PhraseComposerActionId,
        commandEntries: List<PhraseComposerEntry>
    ): PhraseComposerEntry? = commandEntries.firstOrNull { it.actionId == actionId }
}
