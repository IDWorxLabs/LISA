package com.idworx.lisa.features.onboardingguide.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.idworx.lisa.ui.theme.LisaBlue
import com.idworx.lisa.ui.theme.LisaBlueDark
import com.idworx.lisa.ui.theme.LisaBlueLight
import com.idworx.lisa.ui.theme.LisaGray
import com.idworx.lisa.ui.theme.LisaWhite

@Composable
fun TrainingSoftBackground(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LisaBlueLight),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
fun TrainingProgressIndicator(
    currentLesson: Int,
    totalLessons: Int,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = LisaGray
        )
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { if (totalLessons == 0) 0f else currentLesson.toFloat() / totalLessons },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = LisaBlue,
            trackColor = LisaWhite
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Lesson $currentLesson of $totalLessons",
            fontSize = 13.sp,
            color = LisaBlueDark.copy(alpha = 0.75f)
        )
    }
}

@Composable
fun TrainingLisaLogo(modifier: Modifier = Modifier.size(96.dp), animated: Boolean = true) {
    val transition = rememberInfiniteTransition(label = "lisaPulse")
    val scale by transition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logoScale"
    )
    Box(
        modifier = modifier
            .then(if (animated) Modifier.scale(scale) else Modifier)
            .clip(CircleShape)
            .background(LisaBlue),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "LISA",
            color = LisaWhite,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp
        )
    }
}

@Composable
fun TrainingPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentDescription: String = text
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .semantics { this.contentDescription = contentDescription },
        shape = RoundedCornerShape(28.dp),
        colors = ButtonDefaults.buttonColors(containerColor = LisaBlue)
    ) {
        Text(text = text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** Live camera/face/eye status for Guided Learning setup and lessons. */
data class TrainingEyeTrackingState(
    val cameraActive: Boolean = false,
    val faceDetected: Boolean = false,
    val eyesDetected: Boolean = false,
    val leftBlinkCount: Int = 0,
    val rightBlinkCount: Int = 0,
    /** Brief label shown when a blink is accepted (e.g. "Left blink accepted"). */
    val acceptedBlinkLabel: String? = null
)

/**
 * Minimal "Lesson X of Y" label shown above the phrase/navigation title on every Guided
 * Learning lesson. Deliberately text-only — no progress bar, no percentage.
 */
@Composable
fun GuidedLessonProgressLabel(
    current: Int,
    total: Int,
    modifier: Modifier = Modifier
) {
    Text(
        text = "Lesson $current of $total",
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        color = LisaBlueDark.copy(alpha = 0.75f),
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth()
    )
}

/** Responsive phrase title for Guided Learning — wraps long phrases across up to 3 lines. */
@Composable
fun GuidedLessonPhraseTitle(
    phrase: String,
    modifier: Modifier = Modifier
) {
    val normalized = phrase.uppercase()
    val fontSize = guidedLessonPhraseFontSize(phrase)
    val lineHeight = (fontSize.value * 1.3f).sp
    Text(
        text = normalized,
        fontSize = fontSize,
        fontWeight = FontWeight.Bold,
        color = LisaBlueDark,
        textAlign = TextAlign.Center,
        lineHeight = lineHeight,
        maxLines = 3,
        modifier = modifier.fillMaxWidth()
    )
}

fun guidedLessonPhraseFontSize(phrase: String): androidx.compose.ui.unit.TextUnit {
    val normalized = phrase.uppercase()
    val wordCount = normalized.split(" ").count { it.isNotBlank() }
    return when {
        wordCount >= 4 -> when {
            normalized.length <= 20 -> 28.sp
            else -> 24.sp
        }
        normalized.length <= 12 -> 40.sp
        normalized.length <= 16 -> 32.sp
        normalized.length <= 24 -> 28.sp
        else -> 24.sp
    }
}

@Composable
fun EyeTrackingStatusPill(
    label: String,
    active: Boolean,
    modifier: Modifier = Modifier
) {
    val bg = if (active) LisaBlue.copy(alpha = 0.12f) else LisaGray.copy(alpha = 0.12f)
    val dot = if (active) LisaBlue else LisaGray
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(dot)
        )
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = LisaBlueDark.copy(alpha = if (active) 0.92f else 0.65f)
        )
    }
}

@Composable
fun LessonEyeStatusPanel(
    eyeTracking: TrainingEyeTrackingState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(LisaWhite.copy(alpha = 0.92f))
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SetupStatusLine(
            label = "Camera",
            value = if (eyeTracking.cameraActive) "Active" else "Waiting"
        )
        SetupStatusLine(
            label = "Eyes",
            value = when {
                !eyeTracking.cameraActive -> "Waiting"
                !eyeTracking.faceDetected || !eyeTracking.eyesDetected -> "Not detected"
                else -> "Detected"
            }
        )
        SetupStatusLine(
            label = "Left blinks",
            value = eyeTracking.leftBlinkCount.toString()
        )
        SetupStatusLine(
            label = "Right blinks",
            value = eyeTracking.rightBlinkCount.toString()
        )
        eyeTracking.acceptedBlinkLabel?.let { label ->
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = LisaBlue,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun TrainingSensitivityControls(
    sensitivityLevel: Int,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    modifier: Modifier = Modifier,
    minLevel: Int = com.idworx.lisa.MIN_SENSITIVITY_LEVEL,
    maxLevel: Int = com.idworx.lisa.MAX_SENSITIVITY_LEVEL
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(LisaWhite.copy(alpha = 0.92f))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Sensitivity: $sensitivityLevel",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = LisaBlueDark
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = onDecrease,
                enabled = sensitivityLevel > minLevel,
                modifier = Modifier.height(36.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = LisaBlue),
                border = BorderStroke(1.dp, LisaBlue.copy(alpha = 0.5f))
            ) {
                Text("−", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick = onIncrease,
                enabled = sensitivityLevel < maxLevel,
                modifier = Modifier.height(36.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = LisaBlue),
                border = BorderStroke(1.dp, LisaBlue.copy(alpha = 0.5f))
            ) {
                Text("+", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun BlinkDetectionDiagnosticsPanel(
    diagnostics: com.idworx.lisa.features.blinkdetectionreliability.BlinkDetectionDiagnostics,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(LisaGray.copy(alpha = 0.10f))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "Blink diagnostics",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = LisaBlueDark.copy(alpha = 0.7f)
        )
        SetupStatusLine(
            label = "Camera",
            value = if (diagnostics.cameraActive) "Active" else "Off"
        )
        SetupStatusLine(label = "Eyes", value = if (diagnostics.eyesDetected) "Yes" else "No")
        SetupStatusLine(label = "L signal", value = diagnostics.leftEyeSignal)
        SetupStatusLine(label = "R signal", value = diagnostics.rightEyeSignal)
        SetupStatusLine(
            label = "Candidate",
            value = when {
                diagnostics.leftCandidate && diagnostics.rightCandidate -> "L+R"
                diagnostics.leftCandidate -> "Left"
                diagnostics.rightCandidate -> "Right"
                else -> "—"
            }
        )
        SetupStatusLine(label = "Streak", value = "L${diagnostics.leftStreak} R${diagnostics.rightStreak}")
        SetupStatusLine(
            label = "Accepted",
            value = "L${diagnostics.acceptedLeftCount} R${diagnostics.acceptedRightCount}"
        )
        if (diagnostics.skippedForJitter) {
            Text(
                text = "Frame skipped (jitter)",
                fontSize = 11.sp,
                color = LisaGray
            )
        }
    }
}

@Composable
fun SetupDetectionStatusRow(
    cameraActive: Boolean,
    faceDetected: Boolean,
    eyesDetected: Boolean,
    uiStrings: com.idworx.lisa.LisaUiStrings,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SetupStatusLine(
            label = uiStrings.t("Camera", "Kamera", "Ikhamera"),
            value = if (cameraActive) uiStrings.t("Active", "Aktief", "Iyasebenza")
            else uiStrings.t("Waiting", "Wag", "Ilinde")
        )
        SetupStatusLine(
            label = uiStrings.t("Face", "Gesig", "Ubuso"),
            value = if (faceDetected) uiStrings.t("Detected", "Bespeur", "Kutholakele")
            else uiStrings.t("Not detected", "Nie bespeur nie", "Akutholakalanga")
        )
        SetupStatusLine(
            label = uiStrings.t("Eyes", "Oë", "Amehlo"),
            value = if (eyesDetected) uiStrings.t("Detected", "Bespeur", "Kutholakele")
            else uiStrings.t("Not detected", "Nie bespeur nie", "Akutholakalanga")
        )
    }
}

@Composable
private fun SetupStatusLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 16.sp, color = LisaBlueDark.copy(alpha = 0.75f))
        Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = LisaBlueDark)
    }
}

private fun com.idworx.lisa.LisaUiStrings.t(en: String, af: String, zu: String): String =
    when (language) {
        com.idworx.lisa.PreferredLanguage.English -> en
        com.idworx.lisa.PreferredLanguage.Afrikaans -> af
        com.idworx.lisa.PreferredLanguage.IsiZulu -> zu
    }

@Composable
fun TrainingSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(26.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = LisaBlue),
        border = BorderStroke(1.dp, LisaBlue)
    ) {
        Text(text = text, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = LisaBlue)
    }
}

@Composable
fun TrainingCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = LisaWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            content()
        }
    }
}

@Composable
fun TrainingCelebrationOverlay(visible: Boolean) {
    if (!visible) return
    val transition = rememberInfiniteTransition(label = "celebrate")
    val alpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "celebrateAlpha"
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(alpha)
            .background(LisaBlue.copy(alpha = 0.15f))
    )
}

@Composable
fun SimplifiedGestureDisplay(
    left: Int,
    right: Int,
    modifier: Modifier = Modifier
) {
    Text(
        text = formatWinkGestureFriendly(left, right),
        fontSize = 24.sp,
        fontWeight = FontWeight.SemiBold,
        color = LisaBlue,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
fun WinkSequenceDisplay(
    left: Int,
    right: Int,
    currentLeft: Int = 0,
    currentRight: Int = 0,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Blink sequence",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = LisaBlueDark
        )
        Text(
            text = "L$left  R$right",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = LisaBlue,
            textAlign = TextAlign.Center
        )
        if (currentLeft > 0 || currentRight > 0) {
            Text(
                text = "Your sequence: L$currentLeft R$currentRight",
                fontSize = 16.sp,
                color = LisaBlueDark.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun NarrationControls(
    isSpeaking: Boolean,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onRepeat: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    RowButtons(
        modifier = modifier,
        buttons = listOf(
            if (isSpeaking) "Pause" to onPause else "Resume" to onResume,
            "Repeat" to onRepeat,
            "Skip" to onSkip
        )
    )
}

@Composable
private fun RowButtons(
    modifier: Modifier = Modifier,
    buttons: List<Pair<String, () -> Unit>>
) {
    androidx.compose.foundation.layout.Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        buttons.forEach { (label, action) ->
            OutlinedButton(
                onClick = action,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(label, fontSize = 12.sp)
            }
        }
    }
}
