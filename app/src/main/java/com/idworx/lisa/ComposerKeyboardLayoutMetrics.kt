package com.idworx.lisa

/**
 * Responsive keyboard sizing for the eye-controlled composer (RC7D.3).
 * Pure calculations so portrait constraints can be unit-tested without Compose.
 */
object ComposerKeyboardLayoutMetrics {

    const val MIN_KEY_HEIGHT_DP: Int = 28
    const val MAX_KEY_HEIGHT_DP: Int = 72
    const val MIN_KEY_FONT_SP: Int = 16
    const val MAX_KEY_FONT_SP: Int = 28
    const val ROW_SPACING_DP: Int = 4

    fun rowCount(mode: EyeKeyboardLayoutMode): Int = KeyboardLayout.totalRowCount(mode)

    fun keyHeightDp(availableHeightDp: Int, mode: EyeKeyboardLayoutMode): Int {
        if (availableHeightDp <= 0) return MIN_KEY_HEIGHT_DP
        val rows = rowCount(mode)
        val spacingTotal = ROW_SPACING_DP * (rows - 1).coerceAtLeast(0)
        val perRow = (availableHeightDp - spacingTotal) / rows
        return perRow.coerceIn(MIN_KEY_HEIGHT_DP, MAX_KEY_HEIGHT_DP)
    }

    fun keyFontSp(keyHeightDp: Int): Int {
        val scaled = (keyHeightDp * 0.42f).toInt()
        return scaled.coerceIn(MIN_KEY_FONT_SP, MAX_KEY_FONT_SP)
    }

    /** Fixed comfortable key height when the keyboard is bottom-anchored (RC7D.4). */
    fun bottomAnchoredKeyHeightDp(mode: EyeKeyboardLayoutMode): Int = when (mode) {
        EyeKeyboardLayoutMode.Letters -> 44
        EyeKeyboardLayoutMode.Numbers -> 38
    }.coerceIn(MIN_KEY_HEIGHT_DP, MAX_KEY_HEIGHT_DP)
}
