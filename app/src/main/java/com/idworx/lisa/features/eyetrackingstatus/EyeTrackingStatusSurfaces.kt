package com.idworx.lisa.features.eyetrackingstatus

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.idworx.lisa.LisaUiStrings
import com.idworx.lisa.features.onboardingguide.ui.LessonEyeStatusPanel
import com.idworx.lisa.ui.theme.LisaBlue
import com.idworx.lisa.ui.theme.LisaBlueDark
import com.idworx.lisa.ui.theme.LisaWhite

/**
 * @deprecated Prefer [UniversalEyeTrackingHeader] — retained as a thin alias so older call sites
 * and auditors resolve to the single Communication-style visual authority.
 */
@Deprecated(
    message = "Use UniversalEyeTrackingHeader",
    replaceWith = ReplaceWith(
        "UniversalEyeTrackingHeader(state, uiStrings, onDecreaseSensitivity, onIncreaseSensitivity, " +
            "onDecreaseResponseTime, onIncreaseResponseTime, showSensitivityControls, compact, " +
            "modifier = modifier)",
        "com.idworx.lisa.features.eyetrackingstatus.UniversalEyeTrackingHeader"
    )
)
@Composable
fun CompactEyeTrackingHeader(
    state: EyeTrackingStatusUiState,
    uiStrings: LisaUiStrings,
    showSensitivityControls: Boolean = true,
    onDecreaseSensitivity: () -> Unit = {},
    onIncreaseSensitivity: () -> Unit = {},
    onDecreaseResponseTime: () -> Unit = {},
    onIncreaseResponseTime: () -> Unit = {},
    compact: Boolean = false,
    modifier: Modifier = Modifier
) {
    UniversalEyeTrackingHeader(
        state = state,
        uiStrings = uiStrings,
        showSensitivityControls = showSensitivityControls,
        onDecreaseSensitivity = onDecreaseSensitivity,
        onIncreaseSensitivity = onIncreaseSensitivity,
        onDecreaseResponseTime = onDecreaseResponseTime,
        onIncreaseResponseTime = onIncreaseResponseTime,
        compact = compact,
        modifier = modifier
    )
}

/**
 * Readiness panel: universal Communication-style header + optional camera/eyes detail.
 * Blink counts live only inside [UniversalEyeTrackingHeader].
 */
@Composable
fun ExpandedEyeTrackingStatusPanel(
    state: EyeTrackingStatusUiState,
    uiStrings: LisaUiStrings,
    showSensitivityControls: Boolean = true,
    onDecreaseSensitivity: () -> Unit = {},
    onIncreaseSensitivity: () -> Unit = {},
    onDecreaseResponseTime: () -> Unit = {},
    onIncreaseResponseTime: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        UniversalEyeTrackingHeader(
            state = state,
            uiStrings = uiStrings,
            showSensitivityControls = showSensitivityControls,
            onDecreaseSensitivity = onDecreaseSensitivity,
            onIncreaseSensitivity = onIncreaseSensitivity,
            onDecreaseResponseTime = onDecreaseResponseTime,
            onIncreaseResponseTime = onIncreaseResponseTime,
            modifier = Modifier.fillMaxWidth()
        )
        LessonEyeStatusPanel(
            eyeTracking = EyeTrackingStatusUiMapper.toTrainingEyeTracking(state),
            showBlinkCounters = false,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Shared Left/Right blink counter row for surfaces that already sit under
 * [UniversalEyeTrackingHeader] chrome (keyboard / emergency) and only need the count strip.
 *
 * Displays physical-left / physical-right counts via [LisaUiStrings.leftDots] /
 * [LisaUiStrings.rightDots]. Does not own blink detection state.
 */
@Composable
fun BlinkCounterRow(
    uiStrings: LisaUiStrings,
    leftBlinkCount: Int,
    rightBlinkCount: Int,
    compact: Boolean = false,
    modifier: Modifier = Modifier
) {
    val style = TransparentBlinkCounterStyle
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(style.CornerRadius))
            .background(style.Background)
            .padding(
                horizontal = if (compact) style.CompactHorizontalPadding else style.HorizontalPadding,
                vertical = if (compact) style.CompactVerticalPadding else style.VerticalPadding
            )
            .semantics {
                contentDescription =
                    "Left blinks $leftBlinkCount. Right blinks $rightBlinkCount"
            },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = uiStrings.leftDots(leftBlinkCount),
            fontSize = if (compact) style.CompactFontSize else style.FontSize,
            fontWeight = style.LabelWeight,
            color = style.LabelColor
        )
        Text(
            text = uiStrings.rightDots(rightBlinkCount),
            fontSize = if (compact) style.CompactFontSize else style.FontSize,
            fontWeight = style.LabelWeight,
            color = style.LabelColor,
            textAlign = TextAlign.End
        )
    }
}

/** Calibration-friendly status strip that stays above instructional copy. */
@Composable
fun CalibrationEyeTrackingStatusStrip(
    state: EyeTrackingStatusUiState,
    uiStrings: LisaUiStrings,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(LisaWhite.copy(alpha = 0.94f))
            .border(1.dp, LisaBlue.copy(alpha = 0.28f), RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        UniversalEyeTrackingHeader(
            state = state,
            uiStrings = uiStrings,
            showSensitivityControls = true,
            onDecreaseSensitivity = {},
            onIncreaseSensitivity = {},
            onDecreaseResponseTime = {},
            onIncreaseResponseTime = {},
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = buildString {
                append(if (state.cameraActive) "Camera active" else "Camera inactive")
                append(" · ")
                append(
                    when {
                        state.eyesDetected -> "Eyes detected"
                        state.faceDetected -> "Face detected"
                        else -> "No face detected"
                    }
                )
            },
            fontSize = 13.sp,
            color = LisaBlueDark.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
