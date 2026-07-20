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
import com.idworx.lisa.features.onboardingguide.ui.EyeTrackingStatusPill
import com.idworx.lisa.features.onboardingguide.ui.LessonEyeStatusPanel
import com.idworx.lisa.features.onboardingguide.ui.TrainingSensitivityControls
import com.idworx.lisa.ui.theme.LisaBlue
import com.idworx.lisa.ui.theme.LisaBlueDark
import com.idworx.lisa.ui.theme.LisaBlueLight
import com.idworx.lisa.ui.theme.LisaWhite

/**
 * Compact live eye-tracking chrome for Welcome, calibration, readiness, and lessons.
 * Renders [EyeTrackingStatusUiState] from the session authority — never owns a detector.
 */
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
    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = buildString {
                    append(state.statusText)
                    append(". Left blinks ")
                    append(state.leftBlinkCount)
                    append(". Right blinks ")
                    append(state.rightBlinkCount)
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 6.dp)
    ) {
        EyeTrackingStatusPill(
            label = state.statusText.ifBlank {
                if (state.calibrationInProgress) {
                    uiStrings.eyeTrackingStatusCalibrating
                } else {
                    uiStrings.eyeTrackingStatusWatching
                }
            },
            active = state.trackingActive || (state.cameraActive && state.eyesDetected)
        )
        BlinkCounterRow(
            uiStrings = uiStrings,
            leftBlinkCount = state.leftBlinkCount,
            rightBlinkCount = state.rightBlinkCount,
            compact = compact
        )
        if (showSensitivityControls) {
            TrainingSensitivityControls(
                sensitivityLevel = state.sensitivity,
                onDecrease = onDecreaseSensitivity,
                onIncrease = onIncreaseSensitivity,
                responseTimeSec = state.responseTimeSeconds,
                onDecreaseResponseTime = onDecreaseResponseTime,
                onIncreaseResponseTime = onIncreaseResponseTime,
                compact = compact,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Expanded panel with camera/face/eyes lines plus live blink counters (readiness / lessons).
 * Uses [LessonEyeStatusPanel] so Guided Learning keeps one coherent status source.
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
        EyeTrackingStatusPill(
            label = state.statusText.ifBlank { uiStrings.eyeTrackingStatusWatching },
            active = state.trackingActive || (state.cameraActive && state.eyesDetected)
        )
        LessonEyeStatusPanel(
            eyeTracking = EyeTrackingStatusUiMapper.toTrainingEyeTracking(state),
            modifier = Modifier.fillMaxWidth()
        )
        BlinkCounterRow(
            uiStrings = uiStrings,
            leftBlinkCount = state.leftBlinkCount,
            rightBlinkCount = state.rightBlinkCount
        )
        if (showSensitivityControls) {
            TrainingSensitivityControls(
                sensitivityLevel = state.sensitivity,
                onDecrease = onDecreaseSensitivity,
                onIncrease = onIncreaseSensitivity,
                responseTimeSec = state.responseTimeSeconds,
                onDecreaseResponseTime = onDecreaseResponseTime,
                onIncreaseResponseTime = onIncreaseResponseTime,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun BlinkCounterRow(
    uiStrings: LisaUiStrings,
    leftBlinkCount: Int,
    rightBlinkCount: Int,
    compact: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(if (compact) 10.dp else 12.dp))
            .background(LisaBlueLight.copy(alpha = 0.85f))
            .border(1.dp, LisaBlue.copy(alpha = 0.35f), RoundedCornerShape(if (compact) 10.dp else 12.dp))
            .padding(
                horizontal = if (compact) 10.dp else 14.dp,
                vertical = if (compact) 6.dp else 10.dp
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
            fontSize = if (compact) 14.sp else 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = LisaBlueDark
        )
        Text(
            text = uiStrings.rightDots(rightBlinkCount),
            fontSize = if (compact) 14.sp else 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = LisaBlueDark,
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
        CompactEyeTrackingHeader(
            state = state,
            uiStrings = uiStrings,
            showSensitivityControls = true,
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
