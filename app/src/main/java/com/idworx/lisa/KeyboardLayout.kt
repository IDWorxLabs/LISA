package com.idworx.lisa

/** A key on the eye-controlled composer keyboard — character or caregiver action (RC7D.8). */
sealed class KeyboardSlot {
    data class Character(val char: Char) : KeyboardSlot()
    data object Space : KeyboardSlot()
    data object Backspace : KeyboardSlot()
    data object Shift : KeyboardSlot()
}

/** Letter case mode for Shift / Caps Lock (RC7D.8). */
enum class KeyboardShiftMode {
    Lowercase,
    OneShotUppercase,
    CapsLock
}

/**
 * Letter and numeric keyboard layouts for the RC7D eye-controlled phrase composer.
 */
object KeyboardLayout {

    const val LETTER_ROW_COUNT: Int = 3
    /** Digit + punctuation rows before utility row (RC7D.5 / RC7D.8). */
    const val NUMBER_ROW_COUNT: Int = 3

    val letterRows: List<List<Char>> = listOf(
        "QWERTYUIOP".toList(),
        "ASDFGHJKL".toList(),
        "ZXCVBNM".toList()
    )

    /** Punctuation row below QWERTY in letters mode. */
    val letterPunctuationRow: List<Char> = ",.'?!-:;".toList()

    /** Full-width horizontal numeric layout: 12345 / 67890 / punctuation / utility / SPACE */
    val numberRows: List<List<Char>> = listOf(
        "12345".toList(),
        "67890".toList(),
        ".,?!'-:;".toList()
    )

    fun punctuationRowIndex(mode: EyeKeyboardLayoutMode): Int = when (mode) {
        EyeKeyboardLayoutMode.Letters -> LETTER_ROW_COUNT
        EyeKeyboardLayoutMode.Numbers -> 2
    }

    fun utilityRowIndex(mode: EyeKeyboardLayoutMode): Int = when (mode) {
        EyeKeyboardLayoutMode.Letters -> LETTER_ROW_COUNT + 1
        EyeKeyboardLayoutMode.Numbers -> NUMBER_ROW_COUNT
    }

    fun spaceRowIndex(mode: EyeKeyboardLayoutMode): Int = utilityRowIndex(mode) + 1

    fun totalRowCount(mode: EyeKeyboardLayoutMode): Int = spaceRowIndex(mode) + 1

    fun isUtilityRow(mode: EyeKeyboardLayoutMode, row: Int): Boolean = row == utilityRowIndex(mode)

    fun isSpaceRow(mode: EyeKeyboardLayoutMode, row: Int): Boolean = row == spaceRowIndex(mode)

    fun rowLength(mode: EyeKeyboardLayoutMode, row: Int): Int = when (mode) {
        EyeKeyboardLayoutMode.Letters -> when (row) {
            in 0 until LETTER_ROW_COUNT -> letterRows[row].size
            punctuationRowIndex(EyeKeyboardLayoutMode.Letters) -> letterPunctuationRow.size
            utilityRowIndex(EyeKeyboardLayoutMode.Letters) -> 2
            spaceRowIndex(EyeKeyboardLayoutMode.Letters) -> 1
            else -> 0
        }
        EyeKeyboardLayoutMode.Numbers -> when (row) {
            in 0 until NUMBER_ROW_COUNT -> numberRows[row].size
            utilityRowIndex(EyeKeyboardLayoutMode.Numbers) -> 1
            spaceRowIndex(EyeKeyboardLayoutMode.Numbers) -> 1
            else -> 0
        }
    }

    fun slotAt(mode: EyeKeyboardLayoutMode, row: Int, col: Int): KeyboardSlot? = when (mode) {
        EyeKeyboardLayoutMode.Letters -> when (row) {
            in 0 until LETTER_ROW_COUNT -> letterRows[row].getOrNull(col)?.let { KeyboardSlot.Character(it) }
            punctuationRowIndex(EyeKeyboardLayoutMode.Letters) ->
                letterPunctuationRow.getOrNull(col)?.let { KeyboardSlot.Character(it) }
            utilityRowIndex(EyeKeyboardLayoutMode.Letters) -> when (col) {
                0 -> KeyboardSlot.Shift
                1 -> KeyboardSlot.Backspace
                else -> null
            }
            spaceRowIndex(EyeKeyboardLayoutMode.Letters) -> if (col == 0) KeyboardSlot.Space else null
            else -> null
        }
        EyeKeyboardLayoutMode.Numbers -> when (row) {
            in 0 until NUMBER_ROW_COUNT -> numberRows[row].getOrNull(col)?.let { KeyboardSlot.Character(it) }
            utilityRowIndex(EyeKeyboardLayoutMode.Numbers) -> if (col == 0) KeyboardSlot.Backspace else null
            spaceRowIndex(EyeKeyboardLayoutMode.Numbers) -> if (col == 0) KeyboardSlot.Space else null
            else -> null
        }
    }

    /** Character at position, or space character on the space row — for legacy callers. */
    fun keyAt(mode: EyeKeyboardLayoutMode, row: Int, col: Int): Char? = when (val slot = slotAt(mode, row, col)) {
        is KeyboardSlot.Character -> slot.char
        KeyboardSlot.Space -> ' '
        else -> null
    }

    fun initialCursor(mode: EyeKeyboardLayoutMode): KeyboardCursor = KeyboardCursor(row = 0, col = 0)

    fun allKeysReachable(mode: EyeKeyboardLayoutMode): Boolean {
        for (row in 0 until totalRowCount(mode)) {
            for (col in 0 until rowLength(mode, row)) {
                if (slotAt(mode, row, col) == null) return false
            }
        }
        return true
    }

    fun allKeysReachable(): Boolean =
        EyeKeyboardLayoutMode.entries.all { allKeysReachable(it) }

    /** @deprecated Use [numberRows]. */
    @Deprecated("Use numberRows", ReplaceWith("numberRows"))
    val numberDigitRows: List<List<Char>> get() = numberRows.take(2)

    /** @deprecated Use [totalRowCount] with [EyeKeyboardLayoutMode.Letters]. */
    @Deprecated("Use layout-aware APIs", ReplaceWith("totalRowCount(EyeKeyboardLayoutMode.Letters)"))
    const val TOTAL_ROW_COUNT: Int = LETTER_ROW_COUNT + 1

    /** @deprecated Use [spaceRowIndex]. */
    @Deprecated("Use layout-aware APIs", ReplaceWith("spaceRowIndex(EyeKeyboardLayoutMode.Letters)"))
    const val SPACE_ROW_INDEX: Int = LETTER_ROW_COUNT
}
