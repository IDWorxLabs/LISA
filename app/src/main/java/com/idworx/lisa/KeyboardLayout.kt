package com.idworx.lisa

/**
 * Letter and numeric keyboard layouts for the RC7D eye-controlled phrase composer.
 */
object KeyboardLayout {

    const val LETTER_ROW_COUNT: Int = 3
    const val NUMBER_DIGIT_ROW_COUNT: Int = 3
    const val NUMBER_BOTTOM_ROW_INDEX: Int = 3
    const val NUMBER_PUNCTUATION_ROW_INDEX: Int = 4

    val letterRows: List<List<Char>> = listOf(
        "QWERTYUIOP".toList(),
        "ASDFGHJKL".toList(),
        "ZXCVBNM".toList()
    )

    val numberDigitRows: List<List<Char>> = listOf(
        "123".toList(),
        "456".toList(),
        "789".toList()
    )

    val numberBottomRow: List<Char> = listOf('0', '.', ',')
    val punctuationRow: List<Char> = listOf('?', '!', '-')

    fun spaceRowIndex(mode: EyeKeyboardLayoutMode): Int = when (mode) {
        EyeKeyboardLayoutMode.Letters -> LETTER_ROW_COUNT
        EyeKeyboardLayoutMode.Numbers -> NUMBER_PUNCTUATION_ROW_INDEX + 1
    }

    fun totalRowCount(mode: EyeKeyboardLayoutMode): Int = spaceRowIndex(mode) + 1

    fun rowLength(mode: EyeKeyboardLayoutMode, row: Int): Int = when (mode) {
        EyeKeyboardLayoutMode.Letters -> when (row) {
            in 0 until LETTER_ROW_COUNT -> letterRows[row].size
            spaceRowIndex(EyeKeyboardLayoutMode.Letters) -> 1
            else -> 0
        }
        EyeKeyboardLayoutMode.Numbers -> when (row) {
            in 0 until NUMBER_DIGIT_ROW_COUNT -> numberDigitRows[row].size
            NUMBER_BOTTOM_ROW_INDEX -> numberBottomRow.size
            NUMBER_PUNCTUATION_ROW_INDEX -> punctuationRow.size
            spaceRowIndex(EyeKeyboardLayoutMode.Numbers) -> 1
            else -> 0
        }
    }

    fun keyAt(mode: EyeKeyboardLayoutMode, row: Int, col: Int): Char? = when (mode) {
        EyeKeyboardLayoutMode.Letters -> when (row) {
            in 0 until LETTER_ROW_COUNT -> letterRows[row].getOrNull(col)
            spaceRowIndex(EyeKeyboardLayoutMode.Letters) -> if (col == 0) ' ' else null
            else -> null
        }
        EyeKeyboardLayoutMode.Numbers -> when (row) {
            in 0 until NUMBER_DIGIT_ROW_COUNT -> numberDigitRows[row].getOrNull(col)
            NUMBER_BOTTOM_ROW_INDEX -> numberBottomRow.getOrNull(col)
            NUMBER_PUNCTUATION_ROW_INDEX -> punctuationRow.getOrNull(col)
            spaceRowIndex(EyeKeyboardLayoutMode.Numbers) -> if (col == 0) ' ' else null
            else -> null
        }
    }

    fun isSpaceRow(mode: EyeKeyboardLayoutMode, row: Int): Boolean = row == spaceRowIndex(mode)

    fun initialCursor(mode: EyeKeyboardLayoutMode): KeyboardCursor = KeyboardCursor(row = 0, col = 0)

    fun allKeysReachable(mode: EyeKeyboardLayoutMode): Boolean {
        for (row in 0 until totalRowCount(mode)) {
            for (col in 0 until rowLength(mode, row)) {
                if (keyAt(mode, row, col) == null) return false
            }
        }
        return true
    }

    fun allKeysReachable(): Boolean =
        EyeKeyboardLayoutMode.entries.all { allKeysReachable(it) }

    /** @deprecated Use [totalRowCount] with [EyeKeyboardLayoutMode.Letters]. */
    @Deprecated("Use layout-aware APIs", ReplaceWith("totalRowCount(EyeKeyboardLayoutMode.Letters)"))
    const val TOTAL_ROW_COUNT: Int = LETTER_ROW_COUNT + 1

    /** @deprecated Use [spaceRowIndex]. */
    @Deprecated("Use layout-aware APIs", ReplaceWith("spaceRowIndex(EyeKeyboardLayoutMode.Letters)"))
    const val SPACE_ROW_INDEX: Int = LETTER_ROW_COUNT
}
