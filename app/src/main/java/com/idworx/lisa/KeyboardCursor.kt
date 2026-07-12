package com.idworx.lisa

/** Cursor position on the eye-controlled keyboard composer. */
data class KeyboardCursor(
    val row: Int = 0,
    val col: Int = 0
) {
    fun normalized(mode: EyeKeyboardLayoutMode): KeyboardCursor {
        val maxRow = (KeyboardLayout.totalRowCount(mode) - 1).coerceAtLeast(0)
        val rowClamped = row.coerceIn(0, maxRow)
        val maxCol = (KeyboardLayout.rowLength(mode, rowClamped) - 1).coerceAtLeast(0)
        return copy(row = rowClamped, col = col.coerceIn(0, maxCol))
    }

    fun currentKey(mode: EyeKeyboardLayoutMode): Char? = KeyboardLayout.keyAt(mode, row, col)

    fun isOnSpace(mode: EyeKeyboardLayoutMode): Boolean = KeyboardLayout.isSpaceRow(mode, row)
}
