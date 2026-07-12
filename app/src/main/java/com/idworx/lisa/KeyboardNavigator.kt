package com.idworx.lisa

/**
 * Cursor movement and character insertion for the eye-controlled keyboard composer.
 */
object KeyboardNavigator {

    fun move(
        cursor: KeyboardCursor,
        direction: PhraseComposerActionId,
        layoutMode: EyeKeyboardLayoutMode
    ): KeyboardCursor {
        val row = cursor.row
        val col = cursor.col
        return when (direction) {
            PhraseComposerActionId.MoveUp -> moveUp(row, col, layoutMode)
            PhraseComposerActionId.MoveDown -> moveDown(row, col, layoutMode)
            PhraseComposerActionId.MoveLeft -> moveLeft(row, col, layoutMode)
            PhraseComposerActionId.MoveRight -> moveRight(row, col, layoutMode)
            else -> cursor
        }
    }

    fun appendSelectedKey(
        currentPhrase: String,
        key: Char,
        layoutMode: EyeKeyboardLayoutMode
    ): String? {
        if (key == ' ') return appendSpace(currentPhrase)
        if (currentPhrase.length >= CustomPhraseEngine.MAX_PHRASE_LENGTH) return null
        return when {
            key.isLetter() && layoutMode == EyeKeyboardLayoutMode.Letters ->
                currentPhrase + key.lowercaseChar()
            key.isDigit() -> currentPhrase + key
            key in PUNCTUATION_KEYS -> {
                if (currentPhrase.isEmpty()) null else currentPhrase + key
            }
            else -> null
        }
    }

    fun appendSpace(currentPhrase: String): String? {
        if (currentPhrase.isEmpty()) return null
        if (currentPhrase.endsWith(' ')) return null
        if (currentPhrase.length >= CustomPhraseEngine.MAX_PHRASE_LENGTH) return null
        return currentPhrase + ' '
    }

    fun backspace(currentPhrase: String): String =
        if (currentPhrase.isEmpty()) "" else currentPhrase.dropLast(1)

    private val PUNCTUATION_KEYS = setOf('.', ',', '?', '!', '-')

    private fun moveUp(row: Int, col: Int, layoutMode: EyeKeyboardLayoutMode): KeyboardCursor {
        if (row <= 0) return KeyboardCursor(row, col)
        val targetRow = row - 1
        return KeyboardCursor(targetRow, nearestColumn(col, layoutMode, targetRow))
    }

    private fun moveDown(row: Int, col: Int, layoutMode: EyeKeyboardLayoutMode): KeyboardCursor {
        val maxRow = KeyboardLayout.totalRowCount(layoutMode) - 1
        if (row >= maxRow) return KeyboardCursor(row, col)
        val targetRow = row + 1
        val targetCol = if (KeyboardLayout.isSpaceRow(layoutMode, targetRow)) {
            0
        } else {
            nearestColumn(col, layoutMode, targetRow)
        }
        return KeyboardCursor(targetRow, targetCol)
    }

    private fun moveLeft(row: Int, col: Int, layoutMode: EyeKeyboardLayoutMode): KeyboardCursor {
        if (col > 0) return KeyboardCursor(row, col - 1)
        return KeyboardCursor(row, col)
    }

    private fun moveRight(row: Int, col: Int, layoutMode: EyeKeyboardLayoutMode): KeyboardCursor {
        val maxCol = KeyboardLayout.rowLength(layoutMode, row) - 1
        if (col < maxCol) return KeyboardCursor(row, col + 1)
        return KeyboardCursor(row, col)
    }

    /** Closest valid column when moving between rows of different lengths. */
    internal fun nearestColumn(col: Int, layoutMode: EyeKeyboardLayoutMode, targetRow: Int): Int {
        val maxCol = (KeyboardLayout.rowLength(layoutMode, targetRow) - 1).coerceAtLeast(0)
        return col.coerceIn(0, maxCol)
    }
}
