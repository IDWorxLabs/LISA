package com.idworx.lisa

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.idworx.lisa.features.eyetrackingstatus.UniversalEyeTrackingHeader
import com.idworx.lisa.features.eyetrackingstatus.EyeTrackingStatusUiState
import com.idworx.lisa.features.intelligentstartup.model.QuickCalibrationStep
import com.idworx.lisa.features.universalsequenceexecution.SettingsRecalibrationRetrySequenceAuthority
import com.idworx.lisa.ui.theme.LisaBlueDark

/**
 * Settings-launched recalibration UI — same step copy and shared blink chrome as
 * Intelligent Startup Quick Calibration (single engine: [QuickEyeCalibrationEngine]).
 */
@Composable
fun SettingsRecalibrationPanel(
    uiStrings: LisaUiStrings,
    state: SettingsRecalibrationState,
    eyeTrackingStatus: EyeTrackingStatusUiState,
    onDecreaseSensitivity: () -> Unit,
    onIncreaseSensitivity: () -> Unit,
    onDecreaseResponseTime: () -> Unit,
    onIncreaseResponseTime: () -> Unit,
    onRetry: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val calibrationStatus = eyeTrackingStatus.copy(
        calibrationInProgress = state.outcome == SettingsRecalibrationOutcome.InProgress,
        statusText = when {
            eyeTrackingStatus.statusText.isNotBlank() -> eyeTrackingStatus.statusText
            !eyeTrackingStatus.faceDetected -> uiStrings.eyeTrackingStatusNoFace
            !eyeTrackingStatus.eyesDetected -> uiStrings.eyeTrackingStatusLookAtCamera
            else -> uiStrings.eyeTrackingStatusCalibrating
        }
    )
    LisaPanelShell(
        title = uiStrings.calibrationTitle,
        onBack = onCancel,
        backLabel = uiStrings.back
    ) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            UniversalEyeTrackingHeader(
                state = calibrationStatus,
                uiStrings = uiStrings,
                showSensitivityControls = true,
                onDecreaseSensitivity = onDecreaseSensitivity,
                onIncreaseSensitivity = onIncreaseSensitivity,
                onDecreaseResponseTime = onDecreaseResponseTime,
                onIncreaseResponseTime = onIncreaseResponseTime,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = uiStrings.calibrationRecalibrateTitle,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = LisaBlueDark,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = buildString {
                    append(if (calibrationStatus.cameraActive) "Camera active" else "Camera inactive")
                    append(" · ")
                    append(
                        when {
                            calibrationStatus.eyesDetected -> "Eyes detected"
                            calibrationStatus.faceDetected -> "Face detected"
                            else -> "No face detected"
                        }
                    )
                },
                fontSize = 13.sp,
                color = LisaBlueDark.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(20.dp))
            when (state.outcome) {
                SettingsRecalibrationOutcome.Failed -> {
                    Text(
                        text = uiStrings.t(
                            "We couldn't calibrate your eyes.",
                            "Ons kon nie jou oë kalibreer nie.",
                            "Asikwazanga ukulungisa amehlo akho."
                        ),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = LisaBlueDark,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = uiStrings.t(
                            "Move closer to the camera or improve the lighting, then retry.",
                            "Beweeg nader aan die kamera of verbeter die beligting, probeer dan weer.",
                            "Sondele ekhamereni noma thuthukisa ukukhanya, bese uzama futhi."
                        ),
                        fontSize = 16.sp,
                        color = LisaBlueDark.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "${uiStrings.calibrationRetryLabel} · ${
                            SettingsRecalibrationRetrySequenceAuthority.sequenceLabel()
                        }",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = LisaBlueDark,
                        modifier = Modifier
                            .clickable {
                                SettingsRecalibrationRetrySequenceAuthority.invokeRetry(onRetry)
                            }
                            .padding(12.dp)
                    )
                }
                SettingsRecalibrationOutcome.Succeeded -> {
                    Text(
                        text = uiStrings.t(
                            "Calibration Complete",
                            "Kalibrering Voltooi",
                            "Ukulungiswa Kuqediwe"
                        ),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = LisaBlueDark,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = uiStrings.t(
                            "Returning to Settings…",
                            "Keer terug na Instellings…",
                            "Kubuyela ku-Izilungiselelo…"
                        ),
                        fontSize = 16.sp,
                        color = LisaBlueDark.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center
                    )
                }
                else -> {
                    val (title, body) = stepCopy(state, uiStrings)
                    Text(
                        title,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = LisaBlueDark,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        body,
                        fontSize = 16.sp,
                        color = LisaBlueDark.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = uiStrings.calibrationCancelHint,
                fontSize = 13.sp,
                color = LisaBlueDark.copy(alpha = 0.65f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private fun stepCopy(
    state: SettingsRecalibrationState,
    uiStrings: LisaUiStrings
): Pair<String, String> = when (state.step) {
    QuickCalibrationStep.LookNaturally ->
        uiStrings.t("Look naturally at the screen.", "Kyk natuurlik na die skerm.", "Bheka ngokwemvelo esikrinini.") to
            uiStrings.t(
                "Learning your eye openness and distance.",
                "Leer jou ooggroottes en afstand.",
                "Sifunda ukuvuleka kwamehlo akho nebanga."
            )
    QuickCalibrationStep.BlinkThreeTimes ->
        uiStrings.t("Blink normally three times.", "Knip normaalweg drie keer.", "Cwayiza kathathu ngokujwayelekile.") to
            "${state.blinksCollected} / 3"
    QuickCalibrationStep.LeftWinkTwice ->
        uiStrings.t("Wink your left eye twice.", "Knip jou linkerenoog twee keer.", "Cwayiza iso lakho langakwesokunxele kabili.") to
            "${state.leftWinksCollected} / 2"
    QuickCalibrationStep.RightWinkTwice ->
        uiStrings.t("Wink your right eye twice.", "Knip jou regteroog twee keer.", "Cwayiza iso lakho langakwesokudla kabili.") to
            "${state.rightWinksCollected} / 2"
    QuickCalibrationStep.CalibrationComplete ->
        uiStrings.t("Calibration Complete", "Kalibrering Voltooi", "Ukulungiswa Kuqediwe") to
            uiStrings.t("Eye tracking is ready.", "Oognasporing is gereed.", "Ukulandelela amehlo sekulungile.")
}
