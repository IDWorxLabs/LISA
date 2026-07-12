package com.idworx.lisa

/**
 * Letter and numeric keyboard layouts for the RC7D eye-controlled phrase composer.
 */
object KeyboardLayout {

    const val LETTER_ROW_COUNT: Int = 3
    /** Horizontal numeric rows before SPACE (RC7D.5). */
    const val NUMBER_ROW_COUNT: Int = 3

    val letterRows: List<List<Char>> = listOf(
        "QWERTYUIOP".toList(),
        "ASDFGHJKL".toList(),
        "ZXCVBNM".toList()
    )

    /** Full-width horizontal numeric layout: 12345 / 67890 / .,?!- / SPACE */
    val numberRows: List<List<Char>> = listOf(
        "12345".toList(),
        "67890".toList(),
        ".,?!-".toList()
    )

    /** @deprecated Use [numberRows]. */
    @Deprecated("Use numberRows", ReplaceWith("numberRows"))
    val numberDigitRows: List<List<Char>> get() = numberRows.take(2)

    /** @deprecated Merged into [numberRows] row 2. */
    @Deprecated("Use numberRows[2]")
    val numberBottomRow: List<Char> get() = numberRows[2].take(3)

    /** @deprecated Merged into [numberRows] row 2. */
    @Deprecated("Use numberRows[2]")
    val punctuationRow: List<Char> get() = numberRows[2].drop(2)

    /** @deprecated Use [NUMBER_ROW_COUNT]. */
    @Deprecated("Use NUMBER_ROW_COUNT", ReplaceWith("NUMBER_ROW_COUNT"))
    const val NUMBER_DIGIT_ROW_COUNT: Int = 2

    /** @deprecated Punctuation is [numberRows][2]. */
    @Deprecated("Use 2")
    const val NUMBER_PUNCTUATION_ROW_INDEX: Int = 2

    /** @deprecated Digit zero is on [numberRows][1]. */
    @Deprecated("Use numberRows")
    const val NUMBER_BOTTOM_ROW_INDEX: Int = 1

    fun spaceRowIndex(mode: EyeKeyboardLayoutMode): Int = when (mode) {
        EyeKeyboardLayoutMode.Letters -> LETTER_ROW_COUNT
        EyeKeyboardLayoutMode.Numbers -> NUMBER_ROW_COUNT
    }

    fun totalRowCount(mode: EyeKeyboardLayoutMode): Int = spaceRowIndex(mode) + 1

    fun rowLength(mode: EyeKeyboardLayoutMode, row: Int): Int = when (mode) {
        EyeKeyboardLayoutMode.Letters -> when (row) {
            in 0 until LETTER_ROW_COUNT -> letterRows[row].size
            spaceRowIndex(EyeKeyboardLayoutMode.Letters) -> 1
            else -> 0
        }
        EyeKeyboardLayoutMode.Numbers -> when (row) {
            in 0 until NUMBER_ROW_COUNT -> numberRows[row].size
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
            in 0 until NUMBER_ROW_COUNT -> numberRows[row].getOrNull(col)
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
