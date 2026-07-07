package com.idworx.lisa.features.onboardingguide.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.idworx.lisa.LisaUiStrings
import com.idworx.lisa.features.onboardingguide.services.TrainingSessionController
import com.idworx.lisa.ui.theme.LisaBlueDark

@Composable
fun TrainingSetupScreen(
    uiStrings: LisaUiStrings,
    step: Int,
    eyeTracking: TrainingEyeTrackingState,
    sensitivityLevel: Int = com.idworx.lisa.DEFAULT_SENSITIVITY_LEVEL,
    onDecreaseSensitivity: () -> Unit = {},
    onIncreaseSensitivity: () -> Unit = {},
    responseTimeSec: Int = com.idworx.lisa.SequenceProcessingDelay.GUIDED_DEFAULT_SECONDS,
    onDecreaseResponseTime: () -> Unit = {},
    onIncreaseResponseTime: () -> Unit = {},
    onRequestCameraPermission: () -> Unit,
    onAdvance: () -> Unit,
    onBack: () -> Unit
) {
    val watchingLabel = when {
        !eyeTracking.cameraActive -> uiStrings.t("Waiting for camera", "Wag vir kamera", "Ilinde ikhamera")
        !eyeTracking.faceDetected || !eyeTracking.eyesDetected ->
            uiStrings.t("Eyes not detected", "Oë nie bespeur nie", "Amehlo awatholakalanga")
        else -> uiStrings.t("Watching your eyes", "Kyk na jou oë", "Ibona amehlo akho")
    }
    val eyesReady = eyeTracking.eyesDetected

    TrainingSoftBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            EyeTrackingStatusPill(
                label = watchingLabel,
                active = eyeTracking.cameraActive && eyeTracking.eyesDetected,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            TrainingSensitivityControls(
                sensitivityLevel = sensitivityLevel,
                onDecrease = onDecreaseSensitivity,
                onIncrease = onIncreaseSensitivity,
                responseTimeSec = responseTimeSec,
                onDecreaseResponseTime = onDecreaseResponseTime,
                onIncreaseResponseTime = onIncreaseResponseTime,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )

            when (step) {
                TrainingSessionController.SETUP_STEP_EYE_DETECTION -> {
                    Text(
                        text = uiStrings.t("Let's get ready", "Kom ons maak gereed", "Masilungiselele"),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = LisaBlueDark,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = uiStrings.t(
                            "Position the phone so your eyes are clearly visible.",
                            "Plaas die foon sodat jou oë duidelik sigbaar is.",
                            "Beka ifoni ukuze amehlo akho abonakale kahle."
                        ),
                        fontSize = 17.sp,
                        color = LisaBlueDark.copy(alpha = 0.85f),
                        lineHeight = 24.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(28.dp))
                    TrainingCard {
                        SetupDetectionStatusRow(
                            cameraActive = eyeTracking.cameraActive,
                            faceDetected = eyeTracking.faceDetected,
                            eyesDetected = eyeTracking.eyesDetected,
                            uiStrings = uiStrings
                        )
                    }
                    if (!eyeTracking.cameraActive) {
                        Spacer(Modifier.height(24.dp))
                        TrainingPrimaryButton(
                            text = uiStrings.allowCameraAccess,
                            onClick = onRequestCameraPermission
                        )
                    }
                }

                else -> {
                    Text(
                        text = uiStrings.t("Ready to begin", "Gereed om te begin", "Silungile ukuqala"),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = LisaBlueDark,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = if (eyesReady) {
                            uiStrings.t(
                                "Your eyes are visible.",
                                "Jou oë is sigbaar.",
                                "Amehlo akho ayabonakala."
                            )
                        } else {
                            uiStrings.t(
                                "Look at the phone until your eyes are detected.",
                                "Kyk na die foon totdat jou oë bespeur word.",
                                "Bheka ifoni uze amehlo akho atholakale."
                            )
                        },
                        fontSize = 17.sp,
                        color = LisaBlueDark.copy(alpha = 0.85f),
                        lineHeight = 24.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(24.dp))
                    TrainingCard {
                        SetupDetectionStatusRow(
                            cameraActive = eyeTracking.cameraActive,
                            faceDetected = eyeTracking.faceDetected,
                            eyesDetected = eyeTracking.eyesDetected,
                            uiStrings = uiStrings
                        )
                    }
                    Spacer(Modifier.height(28.dp))
                    OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                        Text(uiStrings.back)
                    }
                    Spacer(Modifier.height(12.dp))
                    TrainingPrimaryButton(
                        text = uiStrings.t(
                            "Continue to first lesson",
                            "Gaan voort na eerste les",
                            "Qhubeka ngesifundo sokuqala"
                        ),
                        onClick = onAdvance,
                        enabled = eyesReady && eyeTracking.cameraActive
                    )
                }
            }
        }
    }
}

private fun LisaUiStrings.t(en: String, af: String, zu: String): String = when (language) {
    com.idworx.lisa.PreferredLanguage.English -> en
    com.idworx.lisa.PreferredLanguage.Afrikaans -> af
    com.idworx.lisa.PreferredLanguage.IsiZulu -> zu
}
