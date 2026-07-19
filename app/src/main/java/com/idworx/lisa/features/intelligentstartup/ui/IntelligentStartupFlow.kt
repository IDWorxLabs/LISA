package com.idworx.lisa.features.intelligentstartup.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.idworx.lisa.LisaUiStrings
import com.idworx.lisa.features.intelligentstartup.model.QuickCalibrationStep
import com.idworx.lisa.features.intelligentstartup.model.StartupFlowState
import com.idworx.lisa.features.intelligentstartup.model.StartupPhase
import com.idworx.lisa.features.onboardingguide.ui.TrainingSoftBackground
import com.idworx.lisa.ui.theme.LisaBlue
import com.idworx.lisa.ui.theme.LisaBlueDark
import kotlinx.coroutines.delay

@Composable
fun IntelligentStartupFlow(
    state: StartupFlowState,
    uiStrings: LisaUiStrings,
    cameraPermissionGranted: Boolean,
    cameraView: @Composable () -> Unit,
    onCalibrationTimeout: () -> Unit = {},
    onRequestCameraPermission: () -> Unit = {}
) {
    LaunchedEffect(cameraPermissionGranted) {
        if (!cameraPermissionGranted) onRequestCameraPermission()
    }
    Box(modifier = Modifier.fillMaxSize()) {
        if (cameraPermissionGranted) {
            Box(modifier = Modifier.matchParentSize().padding(0.dp)) {
                // Camera must run for face/eye learning; keep visual soft background over it.
                Box(modifier = Modifier.matchParentSize()) { cameraView() }
            }
        }
        TrainingSoftBackground {
            when (state.phase) {
                StartupPhase.FaceDetection,
                StartupPhase.EvaluatingConfidence -> FaceDetectionStartupScreen(
                    lookingForFace = state.lookingForFaceMessage || !state.faceDetected,
                    uiStrings = uiStrings
                )
                StartupPhase.QuickCalibration -> {
                    LaunchedEffect(state.calibrationStep) {
                        if (state.calibrationStep != QuickCalibrationStep.CalibrationComplete) {
                            delay(22_000L)
                            onCalibrationTimeout()
                        }
                    }
                    QuickEyeCalibrationScreen(state = state, uiStrings = uiStrings)
                }
                StartupPhase.CalibrationFailure -> CalibrationFailureScreen(uiStrings = uiStrings)
                StartupPhase.EyeTrackingReady -> EyeTrackingReadyScreen(uiStrings = uiStrings)
                StartupPhase.Complete -> Unit
            }
        }
    }
}

@Composable
private fun FaceDetectionStartupScreen(
    lookingForFace: Boolean,
    uiStrings: LisaUiStrings
) {
    StartupCenteredMessage(
        title = if (lookingForFace) {
            uiStrings.t("Looking for your face...", "Soek na jou gesig...", "Sibheka ubuso bakho...")
        } else {
            uiStrings.t("Face found", "Gesig gevind", "Ubuso butholakele")
        },
        body = if (lookingForFace) {
            uiStrings.t(
                "Please look at the camera.",
                "Kyk asseblief na die kamera.",
                "Sicela ubheke ikhamera."
            )
        } else {
            uiStrings.t(
                "Preparing eye tracking…",
                "Berei oognasporing voor…",
                "Silungiselela ukulandelela amehlo…"
            )
        }
    )
}

@Composable
private fun QuickEyeCalibrationScreen(
    state: StartupFlowState,
    uiStrings: LisaUiStrings
) {
    val (title, body) = when (state.calibrationStep) {
        QuickCalibrationStep.LookNaturally ->
            uiStrings.t("Look naturally at the screen.", "Kyk natuurlik na die skerm.", "Bheka ngokwemvelo esikrinini.") to
                uiStrings.t("Learning your eye openness and distance.", "Leer jou ooggroottes en afstand.", "Sifunda ukuvuleka kwamehlo akho nebanga.")
        QuickCalibrationStep.BlinkThreeTimes ->
            uiStrings.t("Blink normally three times.", "Knip normaalweg drie keer.", "Cwayiza kathathu ngokujwayelekile.") to
                progressHint(state.blinksCollected, 3, uiStrings)
        QuickCalibrationStep.LeftWinkTwice ->
            uiStrings.t("Wink your left eye twice.", "Knip jou linkerenoog twee keer.", "Cwayiza iso lakho langakwesokunxele kabili.") to
                progressHint(state.leftWinksCollected, 2, uiStrings)
        QuickCalibrationStep.RightWinkTwice ->
            uiStrings.t("Wink your right eye twice.", "Knip jou regteroog twee keer.", "Cwayiza iso lakho langakwesokudla kabili.") to
                progressHint(state.rightWinksCollected, 2, uiStrings)
        QuickCalibrationStep.CalibrationComplete ->
            uiStrings.t("Calibration Complete", "Kalibrering Voltooi", "Ukulungiswa Kuqediwe") to
                uiStrings.t("Eye tracking is ready.", "Oognasporing is gereed.", "Ukulandelela amehlo sekulungile.")
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (state.calibrationStep == QuickCalibrationStep.CalibrationComplete) {
            SuccessPulse()
            Spacer(Modifier.height(18.dp))
        }
        Text(
            text = title,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = LisaBlueDark,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = body,
            fontSize = 17.sp,
            color = LisaBlueDark.copy(alpha = 0.85f),
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )
    }
}

@Composable
private fun CalibrationFailureScreen(uiStrings: LisaUiStrings) {
    StartupCenteredMessage(
        title = uiStrings.t(
            "We couldn't calibrate your eyes.",
            "Ons kon nie jou oë kalibreer nie.",
            "Asikwazanga ukulungisa amehlo akho."
        ),
        body = uiStrings.t(
            "Please move a little closer to the camera.\nor\nImprove the lighting.",
            "Beweeg asseblief 'n bietjie nader aan die kamera.\nof\nVerbeter die beligting.",
            "Sicela usondele kancane ekhamereni.\nnoma\nThuthukisa ukukhanya."
        )
    )
}

@Composable
private fun EyeTrackingReadyScreen(uiStrings: LisaUiStrings) {
    StartupCenteredMessage(
        title = uiStrings.t("Eye Tracking Ready", "Oognasporing Gereed", "Ukulandelela Amehlo Sekulungile"),
        body = uiStrings.t(
            "You can control LISA with your eyes.",
            "Jy kan LISA met jou oë beheer.",
            "Ungalawula i-LISA ngamehlo akho."
        )
    )
}

@Composable
private fun StartupCenteredMessage(title: String, body: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = LisaBlueDark,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = body,
            fontSize = 17.sp,
            color = LisaBlueDark.copy(alpha = 0.85f),
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )
    }
}

@Composable
private fun SuccessPulse() {
    val transition = rememberInfiniteTransition(label = "cal_success")
    val scale by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(520, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cal_success_scale"
    )
    Box(
        modifier = Modifier
            .size(72.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(LisaBlue),
        contentAlignment = Alignment.Center
    ) {
        Text("✓", color = androidx.compose.ui.graphics.Color.White, fontSize = 34.sp, fontWeight = FontWeight.Bold)
    }
}

private fun progressHint(current: Int, total: Int, uiStrings: LisaUiStrings): String =
    uiStrings.t("$current of $total", "$current van $total", "$current kwangu-$total")

private fun LisaUiStrings.t(en: String, af: String, zu: String): String = when (language) {
    com.idworx.lisa.PreferredLanguage.English -> en
    com.idworx.lisa.PreferredLanguage.Afrikaans -> af
    com.idworx.lisa.PreferredLanguage.IsiZulu -> zu
}
