package com.idworx.lisa.features.onboardingguide.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.idworx.lisa.ui.theme.LisaBlue
import com.idworx.lisa.ui.theme.LisaBlueDark
import com.idworx.lisa.ui.theme.LisaBlueLight
import com.idworx.lisa.ui.theme.LisaEmergencyRed
import com.idworx.lisa.ui.theme.LisaGray
import com.idworx.lisa.ui.theme.LisaWhite
import kotlinx.coroutines.delay

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
    contentDescription: String = text,
    textStyle: androidx.compose.ui.text.TextStyle? = null,
    secondaryText: String? = null,
    secondaryTextStyle: androidx.compose.ui.text.TextStyle? = null,
    minHeight: androidx.compose.ui.unit.Dp = if (secondaryText != null) 64.dp else 56.dp
) {
    val heightModifier = if (secondaryText != null) {
        Modifier.height(minHeight)
    } else {
        Modifier.height(56.dp)
    }
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .then(heightModifier)
            .semantics { this.contentDescription = contentDescription },
        shape = RoundedCornerShape(28.dp),
        colors = ButtonDefaults.buttonColors(containerColor = LisaBlue)
    ) {
        if (secondaryText == null) {
            Text(
                text = text,
                style = textStyle ?: androidx.compose.ui.text.TextStyle(
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )
            )
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = text,
                    style = textStyle ?: androidx.compose.ui.text.TextStyle(
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Text(
                    text = secondaryText,
                    style = secondaryTextStyle ?: androidx.compose.ui.text.TextStyle(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
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

/**
 * Compact, non-blocking lesson card shown over the real Communication Workspace during Guided
 * Training Mode navigation lessons. Deliberately small — it must never hide the workspace below
 * it, unlike the old full-screen [NavigationLessonScreen].
 */
@Composable
fun GuidedWorkspaceLessonCard(
    lessonNumber: Int?,
    totalLessons: Int?,
    title: String,
    gestureLabel: String,
    /**
     * When non-null, the card shows this brief positive acknowledgement ("Well done.", etc.)
     * instead of the lesson title/gesture — the user sees they succeeded before the next
     * instruction is revealed. Null shows the normal lesson content.
     */
    feedbackMessage: String? = null,
    /**
     * When non-null (and [feedbackMessage] is null), the card shows this brief red "wrong
     * sequence" acknowledgement — the same try-again tone as the early phrase lessons' red
     * feedback — instead of the lesson title/gesture, then returns to the normal lesson content
     * so the learner can try the highlighted action again.
     */
    wrongGestureMessage: String? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.widthIn(max = 210.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                feedbackMessage != null -> LessonCardSuccessGreen.copy(alpha = 0.97f)
                wrongGestureMessage != null -> LisaEmergencyRed.copy(alpha = 0.95f)
                else -> LisaWhite.copy(alpha = 0.98f)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (feedbackMessage != null) {
                Text(
                    text = "\u2713 $feedbackMessage",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = LisaWhite,
                    textAlign = TextAlign.Center
                )
            } else if (wrongGestureMessage != null) {
                Text(
                    text = wrongGestureMessage,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = LisaWhite,
                    textAlign = TextAlign.Center
                )
            } else {
                if (lessonNumber != null && totalLessons != null) {
                    Text(
                        text = "Lesson $lessonNumber of $totalLessons",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = LisaBlueDark.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(2.dp))
                }
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = LisaBlueDark,
                    textAlign = TextAlign.Center
                )
                if (gestureLabel.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Gesture: $gestureLabel",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = LisaBlue,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/** Success green used only for the brief positive-feedback state of [GuidedWorkspaceLessonCard]. */
private val LessonCardSuccessGreen = androidx.compose.ui.graphics.Color(0xFF3E8E51)

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
        AnimatedBlinkCounterRow(
            label = "Left blinks",
            count = eyeTracking.leftBlinkCount
        )
        AnimatedBlinkCounterRow(
            label = "Right blinks",
            count = eyeTracking.rightBlinkCount
        )
    }
}

/**
 * A single blink counter row that gives calm, immediate confirmation the instant a blink is
 * accepted: the count briefly scales up (~18%) and brightens to [LisaBlue], and the dot
 * indicator flashes the same color, then both smoothly settle back to rest over ~250-300ms.
 *
 * Driven entirely by [count] increasing — every accepted blink produces a new, distinct value,
 * so repeated blinks of the same eye each independently retrigger the animation, while a
 * decrease (e.g. a timeout reset back to 0) never plays the "accepted" animation.
 */
@Composable
fun AnimatedBlinkCounterRow(
    label: String,
    count: Int,
    modifier: Modifier = Modifier
) {
    var previousCount by remember { mutableStateOf(count) }
    val pulse = remember { Animatable(0f) }

    LaunchedEffect(count) {
        if (count > previousCount) {
            pulse.animateTo(1f, tween(durationMillis = 90, easing = FastOutSlowInEasing))
            pulse.animateTo(0f, tween(durationMillis = 190, easing = FastOutSlowInEasing))
        }
        previousCount = count
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 16.sp, color = LisaBlueDark.copy(alpha = 0.75f))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(lerp(LisaGray.copy(alpha = 0.35f), LisaBlue, pulse.value))
            )
            Text(
                text = count.toString(),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = lerp(LisaBlueDark, LisaBlue, pulse.value),
                modifier = Modifier.scale(1f + pulse.value * 0.18f)
            )
        }
    }
}

/**
 * Below the eye status panel: briefly shows "✓ Left blink detected" / "✓ Right blink detected"
 * the instant a blink is accepted. Fades in, holds for ~600ms, then fades out automatically —
 * never left permanently visible. Keyed off the raw blink counts so each accepted blink
 * (even in a fast multi-blink sequence) independently retriggers its own fade cycle.
 */
@Composable
fun AcceptedBlinkMessage(
    leftCount: Int,
    rightCount: Int,
    modifier: Modifier = Modifier
) {
    var previousLeft by remember { mutableStateOf(leftCount) }
    var previousRight by remember { mutableStateOf(rightCount) }
    var visible by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }

    LaunchedEffect(leftCount, rightCount) {
        val leftAccepted = leftCount > previousLeft
        val rightAccepted = rightCount > previousRight
        previousLeft = leftCount
        previousRight = rightCount
        if (leftAccepted || rightAccepted) {
            message = if (leftAccepted) "\u2713 Left blink detected" else "\u2713 Right blink detected"
            visible = true
            delay(600L)
            visible = false
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(durationMillis = 150)),
        exit = fadeOut(tween(durationMillis = 200)),
        modifier = modifier
    ) {
        Text(
            text = message,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = LisaBlue,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Top control area shown throughout Guided Mode / Guided Training (Setup and lesson screens).
 * Sensitivity is always shown; Response Time controls appear in the same area right below it
 * whenever [responseTimeSec] is supplied, so every guided lesson — not just one screen — can
 * adjust its own settle time from Guided Mode/session settings (see [com.idworx.lisa.SequenceProcessingDelay]).
 */
@Composable
fun TrainingSensitivityControls(
    sensitivityLevel: Int,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    modifier: Modifier = Modifier,
    minLevel: Int = com.idworx.lisa.MIN_SENSITIVITY_LEVEL,
    maxLevel: Int = com.idworx.lisa.MAX_SENSITIVITY_LEVEL,
    responseTimeSec: Int? = null,
    onDecreaseResponseTime: () -> Unit = {},
    onIncreaseResponseTime: () -> Unit = {},
    responseTimeMinSeconds: Int = com.idworx.lisa.SequenceProcessingDelay.MIN_SECONDS,
    responseTimeMaxSeconds: Int = com.idworx.lisa.SequenceProcessingDelay.MAX_SECONDS,
    compact: Boolean = false
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 8.dp)
    ) {
        TrainingAdjustableValueRow(
            label = "Sensitivity: $sensitivityLevel",
            onDecrease = onDecrease,
            onIncrease = onIncrease,
            decreaseEnabled = sensitivityLevel > minLevel,
            increaseEnabled = sensitivityLevel < maxLevel,
            compact = compact
        )
        if (responseTimeSec != null) {
            TrainingAdjustableValueRow(
                label = "Response time: ${responseTimeSec}s",
                onDecrease = onDecreaseResponseTime,
                onIncrease = onIncreaseResponseTime,
                decreaseEnabled = responseTimeSec > responseTimeMinSeconds,
                increaseEnabled = responseTimeSec < responseTimeMaxSeconds,
                compact = compact
            )
        }
    }
}

@Composable
private fun TrainingAdjustableValueRow(
    label: String,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    decreaseEnabled: Boolean,
    increaseEnabled: Boolean,
    compact: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(if (compact) 10.dp else 12.dp))
            .background(LisaWhite.copy(alpha = 0.92f))
            .padding(
                horizontal = if (compact) 10.dp else 14.dp,
                vertical = if (compact) 6.dp else 10.dp
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = if (compact) 13.sp else 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = LisaBlueDark
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = onDecrease,
                enabled = decreaseEnabled,
                modifier = Modifier.height(if (compact) 32.dp else 36.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = LisaBlue),
                border = BorderStroke(1.dp, LisaBlue.copy(alpha = 0.5f))
            ) {
                Text("−", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick = onIncrease,
                enabled = increaseEnabled,
                modifier = Modifier.height(if (compact) 32.dp else 36.dp),
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
    modifier: Modifier = Modifier,
    minHeight: androidx.compose.ui.unit.Dp = 52.dp,
    contentDescription: String = text
) {
    val heightModifier = when {
        minHeight.value <= 48f -> Modifier.height(48.dp)
        minHeight == 52.dp -> Modifier.height(52.dp)
        else -> Modifier.height(minHeight)
    }
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .then(heightModifier)
            .semantics { this.contentDescription = contentDescription },
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
