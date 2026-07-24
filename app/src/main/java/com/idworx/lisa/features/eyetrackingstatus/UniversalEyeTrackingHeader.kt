package com.idworx.lisa.features.eyetrackingstatus

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.idworx.lisa.LisaUiStrings
import com.idworx.lisa.MAX_SENSITIVITY_LEVEL
import com.idworx.lisa.MIN_SENSITIVITY_LEVEL
import com.idworx.lisa.SequenceProcessingDelay
import com.idworx.lisa.ui.theme.LisaBlue
import com.idworx.lisa.ui.theme.LisaWhite

/**
 * Canonical visual metrics for the app-wide eye-tracking header.
 * Authority: Communication workspace blue title bar + dark translucent control panel.
 * Do not introduce Welcome / Guided Learning visual variants.
 */
object UniversalEyeTrackingHeaderStyle {
    val TitleCornerRadius: Dp = 10.dp
    val TitleBackground: Color = LisaBlue.copy(alpha = 0.55f)
    val TitleHorizontalPadding: Dp = 10.dp
    val TitleVerticalPadding: Dp = 8.dp
    val CompactTitleVerticalPadding: Dp = 6.dp
    val TitleFontSize: TextUnit = 15.sp
    val CompactTitleFontSize: TextUnit = 14.sp
    val TitleColor: Color = LisaWhite
    val TitleWeight: FontWeight = FontWeight.SemiBold

    val PanelCornerRadius: Dp = 10.dp
    val PanelBackground: Color = Color.Black.copy(alpha = 0.35f)
    val PanelHorizontalPadding: Dp = 8.dp
    val PanelVerticalPadding: Dp = 6.dp
    val CompactPanelVerticalPadding: Dp = 5.dp
    val PanelSpacing: Dp = 4.dp
    val CompactPanelSpacing: Dp = 3.dp

    val GapTitleToPanel: Dp = 4.dp
    val CompactGapTitleToPanel: Dp = 3.dp

    val ControlButtonHeight: Dp = 30.dp
    val CompactControlButtonHeight: Dp = 28.dp
    val ControlLabelFontSize: TextUnit = 11.sp
    val CompactControlLabelFontSize: TextUnit = 10.sp
    val ControlContentColor: Color = LisaWhite

    val CounterFontSize: TextUnit = 12.sp
    val CompactCounterFontSize: TextUnit = 11.sp
    val CounterLabelColor: Color = LisaWhite
    val CounterWeight: FontWeight = FontWeight.Medium

    const val CanonicalComposableName: String = "UniversalEyeTrackingHeader"
}

/**
 * Universal eye-tracking header used from Welcome through Guided Learning and Communication.
 *
 * Visual identity (Communication authority):
 * - Full-width blue “Watching your eyes” title bar
 * - Dark translucent panel with live Left/Right blink counts, Sensitivity, Response time
 *
 * Blink counts must come from the existing shared session state — this composable never
 * owns detection or a second counter source.
 */
@Composable
fun UniversalEyeTrackingHeader(
    state: EyeTrackingStatusUiState,
    uiStrings: LisaUiStrings,
    onDecreaseSensitivity: () -> Unit,
    onIncreaseSensitivity: () -> Unit,
    onDecreaseResponseTime: () -> Unit,
    onIncreaseResponseTime: () -> Unit,
    showSensitivityControls: Boolean = true,
    compact: Boolean = false,
    guidedResponseTimeControlsVisible: Boolean = false,
    guidedResponseTimeSec: Int = SequenceProcessingDelay.GUIDED_DEFAULT_SECONDS,
    onDecreaseGuidedResponseTime: () -> Unit = {},
    onIncreaseGuidedResponseTime: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    UniversalEyeTrackingHeader(
        uiStrings = uiStrings,
        statusText = state.statusText.ifBlank {
            if (state.calibrationInProgress) {
                uiStrings.eyeTrackingStatusCalibrating
            } else {
                uiStrings.eyeTrackingStatusWatching
            }
        },
        leftBlinkCount = state.leftBlinkCount,
        rightBlinkCount = state.rightBlinkCount,
        sensitivityLevel = state.sensitivity,
        responseTimeSec = state.responseTimeSeconds,
        onDecreaseSensitivity = onDecreaseSensitivity,
        onIncreaseSensitivity = onIncreaseSensitivity,
        onDecreaseResponseTime = onDecreaseResponseTime,
        onIncreaseResponseTime = onIncreaseResponseTime,
        showSensitivityControls = showSensitivityControls && state.controlsEnabled,
        compact = compact,
        guidedResponseTimeControlsVisible = guidedResponseTimeControlsVisible,
        guidedResponseTimeSec = guidedResponseTimeSec,
        onDecreaseGuidedResponseTime = onDecreaseGuidedResponseTime,
        onIncreaseGuidedResponseTime = onIncreaseGuidedResponseTime,
        modifier = modifier
    )
}

@Composable
fun UniversalEyeTrackingHeader(
    uiStrings: LisaUiStrings,
    statusText: String,
    leftBlinkCount: Int,
    rightBlinkCount: Int,
    sensitivityLevel: Int,
    responseTimeSec: Int,
    onDecreaseSensitivity: () -> Unit,
    onIncreaseSensitivity: () -> Unit,
    onDecreaseResponseTime: () -> Unit,
    onIncreaseResponseTime: () -> Unit,
    showSensitivityControls: Boolean = true,
    compact: Boolean = false,
    guidedResponseTimeControlsVisible: Boolean = false,
    guidedResponseTimeSec: Int = SequenceProcessingDelay.GUIDED_DEFAULT_SECONDS,
    onDecreaseGuidedResponseTime: () -> Unit = {},
    onIncreaseGuidedResponseTime: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val style = UniversalEyeTrackingHeaderStyle
    val titlePadV = if (compact) style.CompactTitleVerticalPadding else style.TitleVerticalPadding
    val panelPadV = if (compact) style.CompactPanelVerticalPadding else style.PanelVerticalPadding
    val panelSpacing = if (compact) style.CompactPanelSpacing else style.PanelSpacing
    val gap = if (compact) style.CompactGapTitleToPanel else style.GapTitleToPanel
    val titleSize = if (compact) style.CompactTitleFontSize else style.TitleFontSize
    val controlSize = if (compact) style.CompactControlLabelFontSize else style.ControlLabelFontSize
    val buttonHeight = if (compact) style.CompactControlButtonHeight else style.ControlButtonHeight
    val counterSize = if (compact) style.CompactCounterFontSize else style.CounterFontSize
    val safeSensitivity = sensitivityLevel.coerceIn(MIN_SENSITIVITY_LEVEL, MAX_SENSITIVITY_LEVEL)
    val safeResponse = SequenceProcessingDelay.coerce(responseTimeSec)
    val safeGuidedResponse = SequenceProcessingDelay.coerce(guidedResponseTimeSec)
    val buttonColors = ButtonDefaults.outlinedButtonColors(contentColor = style.ControlContentColor)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = buildString {
                    append(statusText)
                    append(". Left blinks ")
                    append(leftBlinkCount)
                    append(". Right blinks ")
                    append(rightBlinkCount)
                    if (showSensitivityControls) {
                        append(". Sensitivity ")
                        append(safeSensitivity)
                        append(". Response time ")
                        append(if (guidedResponseTimeControlsVisible) safeGuidedResponse else safeResponse)
                        append(" seconds")
                    }
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = statusText,
            color = style.TitleColor,
            fontSize = titleSize,
            fontWeight = style.TitleWeight,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(style.TitleCornerRadius))
                .background(style.TitleBackground)
                .padding(horizontal = style.TitleHorizontalPadding, vertical = titlePadV)
        )

        Spacer(modifier = Modifier.height(gap))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(style.PanelCornerRadius))
                .background(style.PanelBackground)
                .padding(horizontal = style.PanelHorizontalPadding, vertical = panelPadV),
            verticalArrangement = Arrangement.spacedBy(panelSpacing)
        ) {
            // Live Left/Right blink feedback from the shared session counts.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = uiStrings.leftDots(leftBlinkCount),
                    fontSize = counterSize,
                    fontWeight = style.CounterWeight,
                    color = style.CounterLabelColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = uiStrings.rightDots(rightBlinkCount),
                    fontSize = counterSize,
                    fontWeight = style.CounterWeight,
                    color = style.CounterLabelColor,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (showSensitivityControls) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(
                        onClick = onDecreaseSensitivity,
                        enabled = safeSensitivity > MIN_SENSITIVITY_LEVEL,
                        modifier = Modifier.height(buttonHeight),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        colors = buttonColors
                    ) { Text(uiStrings.sensitivityDecrease, fontSize = controlSize) }
                    Text(
                        text = "${uiStrings.sensitivity}: $safeSensitivity",
                        color = style.ControlContentColor,
                        fontSize = controlSize,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    OutlinedButton(
                        onClick = onIncreaseSensitivity,
                        enabled = safeSensitivity < MAX_SENSITIVITY_LEVEL,
                        modifier = Modifier.height(buttonHeight),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        colors = buttonColors
                    ) { Text(uiStrings.sensitivityIncrease, fontSize = controlSize) }
                }

                if (guidedResponseTimeControlsVisible) {
                    UniversalResponseTimeRow(
                        label = "${uiStrings.responseTime}: ${safeGuidedResponse}s",
                        onDecrease = onDecreaseGuidedResponseTime,
                        onIncrease = onIncreaseGuidedResponseTime,
                        decreaseEnabled = safeGuidedResponse > SequenceProcessingDelay.MIN_SECONDS,
                        increaseEnabled = safeGuidedResponse < SequenceProcessingDelay.MAX_SECONDS,
                        decreaseLabel = uiStrings.responseTimeDecrease,
                        increaseLabel = uiStrings.responseTimeIncrease,
                        buttonHeight = buttonHeight,
                        controlSize = controlSize,
                        buttonColors = buttonColors
                    )
                } else {
                    UniversalResponseTimeRow(
                        label = "${uiStrings.responseTime}: ${safeResponse}s",
                        onDecrease = onDecreaseResponseTime,
                        onIncrease = onIncreaseResponseTime,
                        decreaseEnabled = safeResponse > SequenceProcessingDelay.MIN_SECONDS,
                        increaseEnabled = safeResponse < SequenceProcessingDelay.MAX_SECONDS,
                        decreaseLabel = uiStrings.responseTimeDecrease,
                        increaseLabel = uiStrings.responseTimeIncrease,
                        buttonHeight = buttonHeight,
                        controlSize = controlSize,
                        buttonColors = buttonColors
                    )
                }
            }
        }
    }
}

@Composable
private fun UniversalResponseTimeRow(
    label: String,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    decreaseEnabled: Boolean,
    increaseEnabled: Boolean,
    decreaseLabel: String,
    increaseLabel: String,
    buttonHeight: Dp,
    controlSize: TextUnit,
    buttonColors: androidx.compose.material3.ButtonColors
) {
    val style = UniversalEyeTrackingHeaderStyle
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        OutlinedButton(
            onClick = onDecrease,
            enabled = decreaseEnabled,
            modifier = Modifier.height(buttonHeight),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
            colors = buttonColors
        ) { Text(decreaseLabel, fontSize = controlSize) }
        Text(
            text = label,
            color = style.ControlContentColor,
            fontSize = controlSize,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        OutlinedButton(
            onClick = onIncrease,
            enabled = increaseEnabled,
            modifier = Modifier.height(buttonHeight),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
            colors = buttonColors
        ) { Text(increaseLabel, fontSize = controlSize) }
    }
}
