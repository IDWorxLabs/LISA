package com.idworx.lisa

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
        val availableHeightDp = maxHeight.value.toInt()
        val keyHeightDp = ComposerKeyboardLayoutMetrics.keyHeightDp(availableHeightDp, layoutMode)
        val keyFontSp = ComposerKeyboardLayoutMetrics.keyFontSp(keyHeightDp)
        val rowSpacing = ComposerKeyboardLayoutMetrics.ROW_SPACING_DP.dp
        val keyHeight = keyHeightDp.dp

        Column(
            modifier = Modifier.fillMaxSize(),
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
                            keyFontSp = keyFontSp
                        )
                    }
                }
                EyeKeyboardLayoutMode.Numbers -> {
                    for (row in 0 until KeyboardLayout.NUMBER_DIGIT_ROW_COUNT) {
                        KeyboardKeyRow(
                            keys = KeyboardLayout.numberDigitRows[row].map { it.toString() },
                            row = row,
                            cursorRow = cursorRow,
                            cursorCol = cursorCol,
                            keyHeight = keyHeight,
                            keyFontSp = keyFontSp
                        )
                    }
                    KeyboardKeyRow(
                        keys = KeyboardLayout.numberBottomRow.map { it.toString() },
                        row = KeyboardLayout.NUMBER_BOTTOM_ROW_INDEX,
                        cursorRow = cursorRow,
                        cursorCol = cursorCol,
                        keyHeight = keyHeight,
                        keyFontSp = keyFontSp
                    )
                    KeyboardKeyRow(
                        keys = KeyboardLayout.punctuationRow.map { it.toString() },
                        row = KeyboardLayout.NUMBER_PUNCTUATION_ROW_INDEX,
                        cursorRow = cursorRow,
                        cursorCol = cursorCol,
                        keyHeight = keyHeight,
                        keyFontSp = keyFontSp
                    )
                }
            }
            KeyboardSpaceRow(
                layoutMode = layoutMode,
                cursorRow = cursorRow,
                cursorCol = cursorCol,
                label = uiStrings.phraseComposerKeyboardSpaceLabel,
                keyHeight = keyHeight,
                keyFontSp = keyFontSp
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
    keyFontSp: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        keys.forEachIndexed { col, label ->
            KeyboardKey(
                label = label,
                highlighted = cursorRow == row && cursorCol == col,
                wide = false,
                keyHeight = keyHeight,
                keyFontSp = keyFontSp
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
    keyFontSp: Int
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
            keyFontSp = keyFontSp
        )
    }
}

@Composable
private fun KeyboardKey(
    label: String,
    highlighted: Boolean,
    wide: Boolean,
    keyHeight: androidx.compose.ui.unit.Dp,
    keyFontSp: Int
) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = Modifier
            .padding(horizontal = 3.dp)
            .height(keyHeight)
            .widthIn(min = if (wide) 200.dp else 34.dp)
            .clip(shape)
            .background(if (highlighted) KeyHighlightFill else KeyBackground)
            .then(
                if (highlighted) {
                    Modifier.border(width = 2.5.dp, color = KeyHighlightBorder, shape = shape)
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
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color.Black.copy(alpha = 0.38f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            text = eyeFeedback.bannerMessage(uiStrings),
            color = LisaWhite,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            maxLines = 1
        )
        Text(
            text = uiStrings.listeningStatusLine(
                eyeFeedback.sensitivityLevel,
                eyeFeedback.responseTimeSec
            ),
            color = LisaWhite.copy(alpha = 0.82f),
            fontSize = 10.sp,
            maxLines = 1
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
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
