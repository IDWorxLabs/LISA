package com.idworx.lisa

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.idworx.lisa.ui.theme.LisaBlue
import com.idworx.lisa.ui.theme.LisaBlueDark
import com.idworx.lisa.ui.theme.LisaWhite

private val KeyBackground = Color.White.copy(alpha = 0.94f)
private val KeyHighlightFill = LisaBlue.copy(alpha = 0.58f)
private val KeyHighlightBorder = LisaBlueDark
private val KeyboardTrayBackground = Color(0xFF1A2332).copy(alpha = 0.92f)

/**
 * Bottom-anchored keyboard tray for RC7D.4 — familiar mobile keyboard placement.
 */
@Composable
fun BottomAlignedEyeKeyboard(
    uiStrings: LisaUiStrings,
    layoutMode: EyeKeyboardLayoutMode,
    cursorRow: Int,
    cursorCol: Int,
    shiftMode: KeyboardShiftMode = KeyboardShiftMode.Lowercase,
    inputSuspended: Boolean = false,
    onKeyTouched: (row: Int, col: Int) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
            .background(KeyboardTrayBackground)
            .padding(horizontal = 6.dp, vertical = 8.dp)
    ) {
        EyeControlledKeyboardGrid(
            uiStrings = uiStrings,
            layoutMode = layoutMode,
            cursorRow = cursorRow,
            cursorCol = cursorCol,
            shiftMode = shiftMode,
            bottomAnchored = true,
            inputSuspended = inputSuspended,
            onKeyTouched = onKeyTouched
        )
    }
}

/**
 * Full letter and numeric keyboard grids with cursor highlight for the RC7D composer.
 */
@Composable
fun EyeControlledKeyboard(
    uiStrings: LisaUiStrings,
    layoutMode: EyeKeyboardLayoutMode,
    cursorRow: Int,
    cursorCol: Int,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight()
    ) {
        EyeControlledKeyboardGrid(
            uiStrings = uiStrings,
            layoutMode = layoutMode,
            cursorRow = cursorRow,
            cursorCol = cursorCol,
            bottomAnchored = false,
            availableHeightDp = maxHeight.value.toInt()
        )
    }
}

@Composable
private fun EyeControlledKeyboardGrid(
    uiStrings: LisaUiStrings,
    layoutMode: EyeKeyboardLayoutMode,
    cursorRow: Int,
    cursorCol: Int,
    shiftMode: KeyboardShiftMode = KeyboardShiftMode.Lowercase,
    bottomAnchored: Boolean,
    availableHeightDp: Int = 0,
    inputSuspended: Boolean = false,
    onKeyTouched: (row: Int, col: Int) -> Unit = { _, _ -> }
) {
    val keyHeightDp = if (bottomAnchored) {
        ComposerKeyboardLayoutMetrics.bottomAnchoredKeyHeightDp(layoutMode)
    } else {
        ComposerKeyboardLayoutMetrics.keyHeightDp(availableHeightDp, layoutMode)
    }
    val keyFontSp = ComposerKeyboardLayoutMetrics.keyFontSp(keyHeightDp)
    val rowSpacing = ComposerKeyboardLayoutMetrics.ROW_SPACING_DP.dp
    val keyHeight = keyHeightDp.dp

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(rowSpacing)
    ) {
            when (layoutMode) {
                EyeKeyboardLayoutMode.Letters -> {
                    for (row in 0 until KeyboardLayout.LETTER_ROW_COUNT) {
                        KeyboardKeyRow(
                            keys = KeyboardLayout.letterRows[row].map { it.toString() },
                            row = row,
                            cursorRow = cursorRow,
                            cursorCol = cursorCol,
                            keyHeight = keyHeight,
                            keyFontSp = keyFontSp,
                            bottomAnchored = bottomAnchored,
                            spreadHorizontally = bottomAnchored,
                            inputSuspended = inputSuspended,
                            onKeyTouched = onKeyTouched
                        )
                    }
                    KeyboardKeyRow(
                        keys = KeyboardLayout.letterPunctuationRow.map { it.toString() },
                        row = KeyboardLayout.punctuationRowIndex(EyeKeyboardLayoutMode.Letters),
                        cursorRow = cursorRow,
                        cursorCol = cursorCol,
                        keyHeight = keyHeight,
                        keyFontSp = keyFontSp,
                        bottomAnchored = bottomAnchored,
                        spreadHorizontally = true,
                        inputSuspended = inputSuspended,
                        onKeyTouched = onKeyTouched
                    )
                    KeyboardUtilityRow(
                        uiStrings = uiStrings,
                        layoutMode = EyeKeyboardLayoutMode.Letters,
                        cursorRow = cursorRow,
                        cursorCol = cursorCol,
                        shiftMode = shiftMode,
                        keyHeight = keyHeight,
                        keyFontSp = keyFontSp,
                        bottomAnchored = bottomAnchored,
                        inputSuspended = inputSuspended,
                        onKeyTouched = onKeyTouched
                    )
                }
                EyeKeyboardLayoutMode.Numbers -> {
                    for (row in 0 until KeyboardLayout.NUMBER_ROW_COUNT) {
                        KeyboardKeyRow(
                            keys = KeyboardLayout.numberRows[row].map { it.toString() },
                            row = row,
                            cursorRow = cursorRow,
                            cursorCol = cursorCol,
                            keyHeight = keyHeight,
                            keyFontSp = keyFontSp,
                            bottomAnchored = bottomAnchored,
                            spreadHorizontally = true,
                            inputSuspended = inputSuspended,
                            onKeyTouched = onKeyTouched
                        )
                    }
                    KeyboardUtilityRow(
                        uiStrings = uiStrings,
                        layoutMode = EyeKeyboardLayoutMode.Numbers,
                        cursorRow = cursorRow,
                        cursorCol = cursorCol,
                        shiftMode = shiftMode,
                        keyHeight = keyHeight,
                        keyFontSp = keyFontSp,
                        bottomAnchored = bottomAnchored,
                        inputSuspended = inputSuspended,
                        onKeyTouched = onKeyTouched
                    )
                }
            }
            KeyboardSpaceRow(
                layoutMode = layoutMode,
                cursorRow = cursorRow,
                cursorCol = cursorCol,
                label = uiStrings.phraseComposerKeyboardSpaceLabel,
                keyHeight = keyHeight,
                keyFontSp = keyFontSp,
                bottomAnchored = bottomAnchored,
                inputSuspended = inputSuspended,
                onKeyTouched = onKeyTouched
            )
    }
}

@Composable
private fun KeyboardUtilityRow(
    uiStrings: LisaUiStrings,
    layoutMode: EyeKeyboardLayoutMode,
    cursorRow: Int,
    cursorCol: Int,
    shiftMode: KeyboardShiftMode,
    keyHeight: androidx.compose.ui.unit.Dp,
    keyFontSp: Int,
    bottomAnchored: Boolean,
    inputSuspended: Boolean,
    onKeyTouched: (row: Int, col: Int) -> Unit
) {
    val utilityRow = KeyboardLayout.utilityRowIndex(layoutMode)
    val shiftActive = KeyboardNavigator.shiftKeyActive(shiftMode)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (layoutMode == EyeKeyboardLayoutMode.Letters) {
            KeyboardKey(
                label = uiStrings.phraseComposerKeyboardShiftLabel,
                highlighted = cursorRow == utilityRow && cursorCol == 0,
                shiftActive = shiftActive,
                wide = false,
                keyHeight = keyHeight,
                keyFontSp = keyFontSp,
                bottomAnchored = bottomAnchored,
                enabled = !inputSuspended,
                onClick = { onKeyTouched(utilityRow, 0) },
                modifier = Modifier.weight(1f),
                enforceMinWidth = false
            )
            KeyboardKey(
                label = uiStrings.phraseComposerKeyboardBackspaceLabel,
                highlighted = cursorRow == utilityRow && cursorCol == 1,
                wide = false,
                keyHeight = keyHeight,
                keyFontSp = (keyFontSp - 2).coerceAtLeast(ComposerKeyboardLayoutMetrics.MIN_KEY_FONT_SP),
                bottomAnchored = bottomAnchored,
                enabled = !inputSuspended,
                onClick = { onKeyTouched(utilityRow, 1) },
                modifier = Modifier.weight(1.4f),
                enforceMinWidth = false
            )
        } else {
            KeyboardKey(
                label = uiStrings.phraseComposerKeyboardBackspaceLabel,
                highlighted = cursorRow == utilityRow && cursorCol == 0,
                wide = true,
                keyHeight = keyHeight,
                keyFontSp = (keyFontSp - 2).coerceAtLeast(ComposerKeyboardLayoutMetrics.MIN_KEY_FONT_SP),
                bottomAnchored = bottomAnchored,
                enabled = !inputSuspended,
                onClick = { onKeyTouched(utilityRow, 0) },
                modifier = Modifier.fillMaxWidth(),
                enforceMinWidth = false
            )
        }
    }
}

@Composable
private fun KeyboardKeyRow(
    keys: List<String>,
    row: Int,
    cursorRow: Int,
    cursorCol: Int,
    keyHeight: androidx.compose.ui.unit.Dp,
    keyFontSp: Int,
    bottomAnchored: Boolean,
    spreadHorizontally: Boolean = false,
    inputSuspended: Boolean = false,
    onKeyTouched: (row: Int, col: Int) -> Unit = { _, _ -> }
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (spreadHorizontally) {
            Arrangement.spacedBy(4.dp)
        } else {
            Arrangement.Center
        },
        verticalAlignment = Alignment.CenterVertically
    ) {
        keys.forEachIndexed { col, label ->
            KeyboardKey(
                label = label,
                highlighted = cursorRow == row && cursorCol == col,
                wide = false,
                keyHeight = keyHeight,
                keyFontSp = keyFontSp,
                bottomAnchored = bottomAnchored,
                enabled = !inputSuspended,
                onClick = { onKeyTouched(row, col) },
                modifier = if (spreadHorizontally) Modifier.weight(1f) else Modifier,
                enforceMinWidth = !spreadHorizontally
            )
        }
    }
}

@Composable
private fun KeyboardSpaceRow(
    layoutMode: EyeKeyboardLayoutMode,
    cursorRow: Int,
    cursorCol: Int,
    label: String,
    keyHeight: androidx.compose.ui.unit.Dp,
    keyFontSp: Int,
    bottomAnchored: Boolean = false,
    inputSuspended: Boolean = false,
    onKeyTouched: (row: Int, col: Int) -> Unit = { _, _ -> }
) {
    val spaceRow = KeyboardLayout.spaceRowIndex(layoutMode)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        KeyboardKey(
            label = label,
            highlighted = cursorRow == spaceRow && cursorCol == 0,
            wide = true,
            keyHeight = keyHeight,
            keyFontSp = keyFontSp,
            bottomAnchored = bottomAnchored,
            enabled = !inputSuspended,
            isSpace = true,
            onClick = { onKeyTouched(spaceRow, 0) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun KeyboardKey(
    label: String,
    highlighted: Boolean,
    wide: Boolean,
    keyHeight: androidx.compose.ui.unit.Dp,
    keyFontSp: Int,
    bottomAnchored: Boolean = false,
    enabled: Boolean = true,
    isSpace: Boolean = false,
    shiftActive: Boolean = false,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    enforceMinWidth: Boolean = true
) {
    val shape = RoundedCornerShape(10.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val contentDescription = keyboardKeyContentDescription(label, isSpace)
    val fill = when {
        highlighted -> KeyHighlightFill
        shiftActive -> LisaBlue.copy(alpha = 0.35f)
        else -> KeyBackground
    }
    Box(
        modifier = modifier
            .padding(horizontal = if (wide) 0.dp else 3.dp)
            .height(keyHeight)
            .then(
                if (enforceMinWidth && !wide) {
                    Modifier.widthIn(min = if (bottomAnchored) 30.dp else 34.dp)
                } else {
                    Modifier
                }
            )
            .clip(shape)
            .clickable(
                enabled = enabled,
                role = Role.Button,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .semantics { this.contentDescription = contentDescription }
            .background(fill)
            .then(
                if (highlighted || shiftActive) {
                    Modifier.border(
                        width = if (highlighted) 2.5.dp else 1.5.dp,
                        color = KeyHighlightBorder,
                        shape = shape
                    )
                } else {
                    Modifier
                }
            )
            .padding(horizontal = if (wide) 24.dp else 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontWeight = if (highlighted) FontWeight.ExtraBold else FontWeight.Bold,
            fontSize = keyFontSp.sp,
            color = LisaBlueDark,
            textAlign = TextAlign.Center
        )
    }
}

/** Accessibility label for a keyboard key — readable names for punctuation and SPACE. */
internal fun keyboardKeyContentDescription(label: String, isSpace: Boolean = false): String = when {
    isSpace -> "Space"
    label.equals("Shift", ignoreCase = true) -> "Shift"
    label.equals("Backspace", ignoreCase = true) -> "Backspace"
    label == "." -> "Period"
    label == "," -> "Comma"
    label == "?" -> "Question mark"
    label == "!" -> "Exclamation mark"
    label == "-" -> "Hyphen"
    label == "'" -> "Apostrophe"
    label == ":" -> "Colon"
    label == ";" -> "Semicolon"
    else -> label
}

@Composable
fun SaveConfirmationSummary(
    uiStrings: LisaUiStrings,
    state: PhraseComposerState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(KeyBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = uiStrings.phraseComposerSaveConfirmBody,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            color = LisaBlueDark
        )
        Text(
            text = uiStrings.phraseComposerSaveConfirmPhraseLabel,
            fontSize = 13.sp,
            color = LisaBlueDark.copy(alpha = 0.75f)
        )
        Text(
            text = "\"${state.displayPhrase()}\"",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = LisaBlueDark
        )
        androidx.compose.foundation.layout.Spacer(Modifier.height(4.dp))
        Text(
            text = uiStrings.phraseComposerCategoryLabel,
            fontSize = 13.sp,
            color = LisaBlueDark.copy(alpha = 0.75f)
        )
        Text(
            text = state.categoryLabel(uiStrings),
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            color = LisaBlueDark
        )
        state.pendingAllocatedSequence?.let { (left, right) ->
            androidx.compose.foundation.layout.Spacer(Modifier.height(4.dp))
            Text(
                text = uiStrings.phraseComposerSaveConfirmSequenceLabel,
                fontSize = 13.sp,
                color = LisaBlueDark.copy(alpha = 0.75f)
            )
            Text(
                text = formatWinkSequenceShort(left, right),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = LisaBlueDark
            )
        }
    }
}

@Composable
fun ComposerEyeStatusBar(
    uiStrings: LisaUiStrings,
    eyeFeedback: ComposerEyeFeedback,
    onSensitivityDecrease: () -> Unit = {},
    onSensitivityIncrease: () -> Unit = {},
    onResponseTimeDecrease: () -> Unit = {},
    onResponseTimeIncrease: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color.Black.copy(alpha = 0.38f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = eyeFeedback.bannerMessage(uiStrings),
            color = LisaWhite,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            maxLines = 2,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ComposerStatusControlButton(
                text = uiStrings.sensitivityDecrease,
                onClick = onSensitivityDecrease,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = uiStrings.composerSensitivityLine(eyeFeedback.sensitivityLevel),
                color = LisaWhite.copy(alpha = 0.82f),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                modifier = Modifier.weight(1.2f),
                textAlign = TextAlign.Center
            )
            ComposerStatusControlButton(
                text = uiStrings.sensitivityIncrease,
                onClick = onSensitivityIncrease,
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ComposerStatusControlButton(
                text = uiStrings.responseTimeDecrease,
                onClick = onResponseTimeDecrease,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = uiStrings.composerResponseTimeLine(eyeFeedback.responseTimeSec),
                color = LisaWhite.copy(alpha = 0.82f),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                modifier = Modifier.weight(1.2f),
                textAlign = TextAlign.Center
            )
            ComposerStatusControlButton(
                text = uiStrings.responseTimeIncrease,
                onClick = onResponseTimeIncrease,
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = uiStrings.leftDots(eyeFeedback.leftWinkCount),
                color = LisaWhite,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = uiStrings.rightDots(eyeFeedback.rightWinkCount),
                color = LisaWhite,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
        eyeFeedback.partialSequenceLabel()?.let { sequence ->
            Text(
                text = "${uiStrings.phraseComposerPartialSequenceLabel}: $sequence",
                color = LisaWhite.copy(alpha = 0.92f),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ComposerStatusControlButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .background(Color.White.copy(alpha = 0.92f))
            .padding(horizontal = 6.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = LisaBlueDark,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}
