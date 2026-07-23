package com.idworx.lisa.features.intelligentstartup.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.idworx.lisa.CommunicationLevel
import com.idworx.lisa.LisaUiStrings
import com.idworx.lisa.PreferredLanguage
import com.idworx.lisa.features.eyetrackingstatus.CompactEyeTrackingHeader
import com.idworx.lisa.features.eyetrackingstatus.EyeTrackingStatusUiState
import com.idworx.lisa.features.intelligentstartup.model.QuickCalibrationStep
import com.idworx.lisa.features.intelligentstartup.model.StartupFlowState
import com.idworx.lisa.features.intelligentstartup.model.StartupPhase
import com.idworx.lisa.features.intelligentstartup.model.StartupProfileChoice
import com.idworx.lisa.features.onboardingguide.ui.TrainingCard
import com.idworx.lisa.features.onboardingguide.ui.TrainingPrimaryButton
import com.idworx.lisa.features.onboardingguide.ui.TrainingSoftBackground
import com.idworx.lisa.formatWinkSequenceShort
import com.idworx.lisa.ui.theme.LisaBlue
import com.idworx.lisa.ui.theme.LisaBlueDark
import com.idworx.lisa.ui.theme.LisaWhite
import com.idworx.lisa.ui.theme.LisaWorkspaceVisualStyle
import kotlinx.coroutines.delay
import java.text.DateFormat
import java.util.Date

@Composable
fun IntelligentStartupFlow(
    state: StartupFlowState,
    uiStrings: LisaUiStrings,
    cameraPermissionGranted: Boolean,
    cameraView: @Composable () -> Unit,
    eyeTrackingStatus: EyeTrackingStatusUiState = EyeTrackingStatusUiState(),
    onCalibrationTimeout: () -> Unit = {},
    onRequestCameraPermission: () -> Unit = {},
    onCreateDraftChange: (name: String?, language: String?, level: String?) -> Unit = { _, _, _ -> },
    onConfirmCreatePrimaryUser: () -> Unit = {},
    onSelectProfileIndex: (Int) -> Unit = {},
    onConfirmSelectedProfile: () -> Unit = {},
    onDecreaseSensitivity: () -> Unit = {},
    onIncreaseSensitivity: () -> Unit = {},
    onDecreaseResponseTime: () -> Unit = {},
    onIncreaseResponseTime: () -> Unit = {}
) {
    LaunchedEffect(cameraPermissionGranted) {
        if (!cameraPermissionGranted) onRequestCameraPermission()
    }
    Box(modifier = Modifier.fillMaxSize()) {
        if (cameraPermissionGranted) {
            Box(modifier = Modifier.matchParentSize()) { cameraView() }
        }
        TrainingSoftBackground {
            when (state.phase) {
                StartupPhase.FaceDetection,
                StartupPhase.ProfileResolution,
                StartupPhase.EvaluatingCompatibility -> FaceDetectionStartupScreen(
                    lookingForFace = state.lookingForFaceMessage || !state.faceDetected,
                    evaluating = state.phase == StartupPhase.EvaluatingCompatibility ||
                        state.phase == StartupPhase.ProfileResolution,
                    uiStrings = uiStrings,
                    eyeTrackingStatus = eyeTrackingStatus,
                    onDecreaseSensitivity = onDecreaseSensitivity,
                    onIncreaseSensitivity = onIncreaseSensitivity,
                    onDecreaseResponseTime = onDecreaseResponseTime,
                    onIncreaseResponseTime = onIncreaseResponseTime
                )
                StartupPhase.CreatePrimaryUser -> CreatePrimaryUserScreen(
                    state = state,
                    uiStrings = uiStrings,
                    eyeTrackingStatus = eyeTrackingStatus,
                    onDraftChange = onCreateDraftChange,
                    onConfirm = onConfirmCreatePrimaryUser,
                    onDecreaseSensitivity = onDecreaseSensitivity,
                    onIncreaseSensitivity = onIncreaseSensitivity,
                    onDecreaseResponseTime = onDecreaseResponseTime,
                    onIncreaseResponseTime = onIncreaseResponseTime
                )
                StartupPhase.ProfileSelection -> StartupProfilePickerScreen(
                    state = state,
                    uiStrings = uiStrings,
                    eyeTrackingStatus = eyeTrackingStatus,
                    onSelectIndex = onSelectProfileIndex,
                    onConfirm = onConfirmSelectedProfile,
                    onDecreaseSensitivity = onDecreaseSensitivity,
                    onIncreaseSensitivity = onIncreaseSensitivity,
                    onDecreaseResponseTime = onDecreaseResponseTime,
                    onIncreaseResponseTime = onIncreaseResponseTime
                )
                StartupPhase.QuickCalibration -> {
                    LaunchedEffect(state.calibrationStep) {
                        if (state.calibrationStep != QuickCalibrationStep.CalibrationComplete) {
                            delay(22_000L)
                            onCalibrationTimeout()
                        }
                    }
                    QuickEyeCalibrationScreen(
                        state = state,
                        uiStrings = uiStrings,
                        eyeTrackingStatus = eyeTrackingStatus,
                        onDecreaseSensitivity = onDecreaseSensitivity,
                        onIncreaseSensitivity = onIncreaseSensitivity,
                        onDecreaseResponseTime = onDecreaseResponseTime,
                        onIncreaseResponseTime = onIncreaseResponseTime
                    )
                }
                StartupPhase.CalibrationFailure -> CalibrationFailureScreen(
                    uiStrings = uiStrings,
                    eyeTrackingStatus = eyeTrackingStatus,
                    onDecreaseSensitivity = onDecreaseSensitivity,
                    onIncreaseSensitivity = onIncreaseSensitivity,
                    onDecreaseResponseTime = onDecreaseResponseTime,
                    onIncreaseResponseTime = onIncreaseResponseTime
                )
                StartupPhase.EyeTrackingReady -> EyeTrackingReadyScreen(
                    uiStrings = uiStrings,
                    eyeTrackingStatus = eyeTrackingStatus,
                    onDecreaseSensitivity = onDecreaseSensitivity,
                    onIncreaseSensitivity = onIncreaseSensitivity,
                    onDecreaseResponseTime = onDecreaseResponseTime,
                    onIncreaseResponseTime = onIncreaseResponseTime
                )
                StartupPhase.Complete -> Unit
            }
        }
    }
}

/** Shared status → transparent blink counter → sensitivity, then screen content. */
@Composable
private fun StartupScreenWithSharedBlinkCounter(
    uiStrings: LisaUiStrings,
    eyeTrackingStatus: EyeTrackingStatusUiState,
    onDecreaseSensitivity: () -> Unit,
    onIncreaseSensitivity: () -> Unit,
    onDecreaseResponseTime: () -> Unit,
    onIncreaseResponseTime: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CompactEyeTrackingHeader(
            state = eyeTrackingStatus,
            uiStrings = uiStrings,
            showSensitivityControls = true,
            compact = true,
            onDecreaseSensitivity = onDecreaseSensitivity,
            onIncreaseSensitivity = onIncreaseSensitivity,
            onDecreaseResponseTime = onDecreaseResponseTime,
            onIncreaseResponseTime = onIncreaseResponseTime,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        content()
    }
}

@Composable
private fun FaceDetectionStartupScreen(
    lookingForFace: Boolean,
    evaluating: Boolean,
    uiStrings: LisaUiStrings,
    eyeTrackingStatus: EyeTrackingStatusUiState,
    onDecreaseSensitivity: () -> Unit,
    onIncreaseSensitivity: () -> Unit,
    onDecreaseResponseTime: () -> Unit,
    onIncreaseResponseTime: () -> Unit
) {
    val title = when {
        evaluating -> uiStrings.t("Preparing…", "Berei voor…", "Silungiselela…")
        lookingForFace -> uiStrings.t("Looking for your face...", "Soek na jou gesig...", "Sibheka ubuso bakho...")
        else -> uiStrings.t("Face found", "Gesig gevind", "Ubuso butholakele")
    }
    val body = when {
        evaluating -> uiStrings.t(
            "Checking your profile and eye calibration.",
            "Kontroleer jou profiel en oogkalibrering.",
            "Sihlola iphrofayela yakho nokulungiswa kwamehlo."
        )
        lookingForFace -> uiStrings.t(
            "Please look at the camera.",
            "Kyk asseblief na die kamera.",
            "Sicela ubheke ikhamera."
        )
        else -> uiStrings.t(
            "Preparing eye tracking…",
            "Berei oognasporing voor…",
            "Silungiselela ukulandelela amehlo…"
        )
    }
    StartupScreenWithSharedBlinkCounter(
        uiStrings = uiStrings,
        eyeTrackingStatus = eyeTrackingStatus,
        onDecreaseSensitivity = onDecreaseSensitivity,
        onIncreaseSensitivity = onIncreaseSensitivity,
        onDecreaseResponseTime = onDecreaseResponseTime,
        onIncreaseResponseTime = onIncreaseResponseTime
    ) {
        StartupCenteredMessage(title = title, body = body, fillRemaining = true)
    }
}

@Composable
private fun CreatePrimaryUserScreen(
    state: StartupFlowState,
    uiStrings: LisaUiStrings,
    eyeTrackingStatus: EyeTrackingStatusUiState,
    onDraftChange: (name: String?, language: String?, level: String?) -> Unit,
    onConfirm: () -> Unit,
    onDecreaseSensitivity: () -> Unit,
    onIncreaseSensitivity: () -> Unit,
    onDecreaseResponseTime: () -> Unit,
    onIncreaseResponseTime: () -> Unit
) {
    StartupScreenWithSharedBlinkCounter(
        uiStrings = uiStrings,
        eyeTrackingStatus = eyeTrackingStatus,
        onDecreaseSensitivity = onDecreaseSensitivity,
        onIncreaseSensitivity = onIncreaseSensitivity,
        onDecreaseResponseTime = onDecreaseResponseTime,
        onIncreaseResponseTime = onIncreaseResponseTime
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = uiStrings.t("Create Primary User", "Skep Primêre Gebruiker", "Dala Umsebenzisi Oyinhloko"),
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = LisaBlueDark,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = uiStrings.t(
                    "Caregiver-assisted setup. Only a name, language, and level are required.",
                    "Versorger-ondersteunde opstelling.",
                    "Ukusetha okusizwa umnakekeli."
                ),
                fontSize = 15.sp,
                color = LisaBlueDark.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(18.dp))
            TrainingCard {
                OutlinedTextField(
                    value = state.createNameDraft,
                    onValueChange = { onDraftChange(it, null, null) },
                    label = { Text(uiStrings.nameLabel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(uiStrings.preferredLanguageSection, fontWeight = FontWeight.SemiBold, color = LisaBlueDark)
                PreferredLanguage.selectable.forEach { language ->
                    ChoiceChipRow(
                        label = language.label,
                        selected = state.createLanguageLabel == language.label,
                        onClick = { onDraftChange(null, language.label, null) }
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(uiStrings.communicationLevelSection, fontWeight = FontWeight.SemiBold, color = LisaBlueDark)
                CommunicationLevel.entries.forEach { level ->
                    ChoiceChipRow(
                        label = level.label,
                        selected = state.createLevelLabel == level.label,
                        onClick = { onDraftChange(null, null, level.label) }
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                TrainingPrimaryButton(
                    text = uiStrings.t("Continue", "Gaan voort", "Qhubeka"),
                    onClick = onConfirm
                )
            }
        }
    }
}

@Composable
private fun StartupProfilePickerScreen(
    state: StartupFlowState,
    uiStrings: LisaUiStrings,
    eyeTrackingStatus: EyeTrackingStatusUiState,
    onSelectIndex: (Int) -> Unit,
    onConfirm: () -> Unit,
    onDecreaseSensitivity: () -> Unit,
    onIncreaseSensitivity: () -> Unit,
    onDecreaseResponseTime: () -> Unit,
    onIncreaseResponseTime: () -> Unit
) {
    StartupScreenWithSharedBlinkCounter(
        uiStrings = uiStrings,
        eyeTrackingStatus = eyeTrackingStatus,
        onDecreaseSensitivity = onDecreaseSensitivity,
        onIncreaseSensitivity = onIncreaseSensitivity,
        onDecreaseResponseTime = onDecreaseResponseTime,
        onIncreaseResponseTime = onIncreaseResponseTime
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = uiStrings.t("Who is using LISA?", "Wie gebruik LISA?", "Ubani osebenzisa i-LISA?"),
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = LisaBlueDark,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = uiStrings.t(
                    "Move Up ${formatWinkSequenceShort(2, 0)}  ·  Move Down ${formatWinkSequenceShort(0, 2)}  ·  Select ${formatWinkSequenceShort(1, 1)}",
                    "Op ${formatWinkSequenceShort(2, 0)}  ·  Af ${formatWinkSequenceShort(0, 2)}  ·  Kies ${formatWinkSequenceShort(1, 1)}",
                    "Phezulu ${formatWinkSequenceShort(2, 0)}  ·  Phansi ${formatWinkSequenceShort(0, 2)}  ·  Khetha ${formatWinkSequenceShort(1, 1)}"
                ),
                fontSize = 13.sp,
                color = LisaBlueDark.copy(alpha = 0.75f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            state.profileChoices.forEachIndexed { index, choice ->
                ProfileChoiceCard(
                    choice = choice,
                    selected = index == state.selectedProfileIndex,
                    onClick = {
                        onSelectIndex(index)
                        onConfirm()
                    }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun ProfileChoiceCard(
    choice: StartupProfileChoice,
    selected: Boolean,
    onClick: () -> Unit
) {
    val dateLabel = choice.lastCalibratedAtMs?.let {
        DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(it))
    } ?: "Not calibrated"
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(LisaWorkspaceVisualStyle.CardCornerRadius))
            .background(
                if (selected) LisaWorkspaceVisualStyle.CardSelectedBackground else LisaWhite
            )
            .then(
                if (selected) Modifier.border(2.dp, LisaBlue, RoundedCornerShape(LisaWorkspaceVisualStyle.CardCornerRadius))
                else Modifier
            )
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Text(choice.name, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = LisaBlueDark, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text("${choice.languageLabel} · ${choice.communicationLevelLabel}", fontSize = 14.sp, color = LisaBlueDark.copy(alpha = 0.75f))
        Text("Last calibration: $dateLabel", fontSize = 12.sp, color = LisaBlueDark.copy(alpha = 0.65f))
    }
}

@Composable
private fun ChoiceChipRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) LisaWorkspaceVisualStyle.CardSelectedBackground else LisaWhite)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = LisaBlueDark, fontSize = 15.sp)
        if (selected) Text("✓", color = LisaBlue, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun QuickEyeCalibrationScreen(
    state: StartupFlowState,
    uiStrings: LisaUiStrings,
    eyeTrackingStatus: EyeTrackingStatusUiState,
    onDecreaseSensitivity: () -> Unit,
    onIncreaseSensitivity: () -> Unit,
    onDecreaseResponseTime: () -> Unit,
    onIncreaseResponseTime: () -> Unit
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
    val calibrationStatus = eyeTrackingStatus.copy(
        calibrationInProgress = true,
        statusText = when {
            eyeTrackingStatus.statusText.isNotBlank() -> eyeTrackingStatus.statusText
            !eyeTrackingStatus.faceDetected -> uiStrings.eyeTrackingStatusNoFace
            !eyeTrackingStatus.eyesDetected -> uiStrings.eyeTrackingStatusLookAtCamera
            else -> uiStrings.eyeTrackingStatusCalibrating
        }
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        CompactEyeTrackingHeader(
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
        if (state.calibrationStep == QuickCalibrationStep.CalibrationComplete) {
            SuccessPulse()
            Spacer(modifier = Modifier.height(18.dp))
        }
        Text(title, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = LisaBlueDark, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(14.dp))
        Text(body, fontSize = 17.sp, color = LisaBlueDark.copy(alpha = 0.85f), textAlign = TextAlign.Center, lineHeight = 24.sp)
    }
}

@Composable
private fun CalibrationFailureScreen(
    uiStrings: LisaUiStrings,
    eyeTrackingStatus: EyeTrackingStatusUiState,
    onDecreaseSensitivity: () -> Unit,
    onIncreaseSensitivity: () -> Unit,
    onDecreaseResponseTime: () -> Unit,
    onIncreaseResponseTime: () -> Unit
) {
    StartupScreenWithSharedBlinkCounter(
        uiStrings = uiStrings,
        eyeTrackingStatus = eyeTrackingStatus,
        onDecreaseSensitivity = onDecreaseSensitivity,
        onIncreaseSensitivity = onIncreaseSensitivity,
        onDecreaseResponseTime = onDecreaseResponseTime,
        onIncreaseResponseTime = onIncreaseResponseTime
    ) {
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
            ),
            fillRemaining = true
        )
    }
}

@Composable
private fun EyeTrackingReadyScreen(
    uiStrings: LisaUiStrings,
    eyeTrackingStatus: EyeTrackingStatusUiState,
    onDecreaseSensitivity: () -> Unit,
    onIncreaseSensitivity: () -> Unit,
    onDecreaseResponseTime: () -> Unit,
    onIncreaseResponseTime: () -> Unit
) {
    StartupScreenWithSharedBlinkCounter(
        uiStrings = uiStrings,
        eyeTrackingStatus = eyeTrackingStatus,
        onDecreaseSensitivity = onDecreaseSensitivity,
        onIncreaseSensitivity = onIncreaseSensitivity,
        onDecreaseResponseTime = onDecreaseResponseTime,
        onIncreaseResponseTime = onIncreaseResponseTime
    ) {
        StartupCenteredMessage(
            title = uiStrings.t("Eye Tracking Ready", "Oognasporing Gereed", "Ukulandelela Amehlo Sekulungile"),
            body = uiStrings.t(
                "You can control LISA with your eyes.",
                "Jy kan LISA met jou oë beheer.",
                "Ungalawula i-LISA ngamehlo akho."
            ),
            fillRemaining = true
        )
    }
}

@Composable
private fun ColumnScope.StartupCenteredMessage(
    title: String,
    body: String,
    fillRemaining: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (fillRemaining) Modifier.weight(1f) else Modifier)
            .padding(horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(title, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = LisaBlueDark, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(14.dp))
        Text(body, fontSize = 17.sp, color = LisaBlueDark.copy(alpha = 0.85f), textAlign = TextAlign.Center, lineHeight = 24.sp)
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
        Text("✓", color = LisaWhite, fontSize = 34.sp, fontWeight = FontWeight.Bold)
    }
}

private fun progressHint(current: Int, total: Int, uiStrings: LisaUiStrings): String =
    uiStrings.t("$current of $total", "$current van $total", "$current kwangu-$total")

private fun LisaUiStrings.t(en: String, af: String, zu: String): String = when (language) {
    PreferredLanguage.English -> en
    PreferredLanguage.Afrikaans -> af
    PreferredLanguage.IsiZulu -> zu
}
