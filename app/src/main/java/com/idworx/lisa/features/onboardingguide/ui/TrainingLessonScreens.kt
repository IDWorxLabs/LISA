package com.idworx.lisa.features.onboardingguide.ui

import androidx.compose.foundation.background
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.idworx.lisa.features.silentwelcome.LisaSpeechPolicy
import com.idworx.lisa.LisaUiStrings
import com.idworx.lisa.features.experiencepolish.patientcommunicationcoach.model.CoachUiState
import com.idworx.lisa.features.eyetrackingstatus.CompactEyeTrackingHeader
import com.idworx.lisa.features.eyetrackingstatus.EyeTrackingStatusUiMapper
import com.idworx.lisa.features.eyetrackingstatus.EyeTrackingStatusUiState
import com.idworx.lisa.features.onboardingguide.coach.CaregiverProgressSnapshot
import com.idworx.lisa.features.onboardingguide.services.AdaptiveLearningOffer
import com.idworx.lisa.features.blinkdetectionreliability.BlinkDetectionDiagnostics
import com.idworx.lisa.features.onboardingguide.state.LessonInteractionState
import com.idworx.lisa.features.onboardingguide.state.TrainingFeedback
import com.idworx.lisa.ui.theme.LisaBlue
import com.idworx.lisa.ui.theme.LisaBlueDark
import com.idworx.lisa.ui.theme.LisaEmergencyRed
import com.idworx.lisa.ui.theme.LisaGray
import com.idworx.lisa.ui.theme.LisaSoftGray

/** Legacy coach strip — retained for workspace/settings; not shown during simplified Guided Learning. */
@Composable
fun CaregiverCoachProgressStrip(
    coachUiState: CoachUiState?,
    caregiverSnapshot: CaregiverProgressSnapshot?,
    coachInstruction: String?,
    modifier: Modifier = Modifier
) {
    val hint = coachInstruction ?: coachUiState?.caregiverHint ?: caregiverSnapshot?.coachSummary ?: return
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(LisaSoftGray, androidx.compose.foundation.shape.RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            text = hint,
            fontSize = 13.sp,
            color = LisaBlueDark.copy(alpha = 0.85f),
            lineHeight = 18.sp,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Minimal Guided Learning lesson — phrase, gesture, one instruction. Lisa controls progression.
 */
@Composable
fun CommunicationLessonScreen(
    phrase: String,
    left: Int,
    right: Int,
    instruction: String,
    eyeTracking: TrainingEyeTrackingState = TrainingEyeTrackingState(),
    lessonInteraction: LessonInteractionState = LessonInteractionState(),
    feedback: TrainingFeedback = TrainingFeedback.None,
    feedbackMessage: String? = null,
    blinkDiagnostics: BlinkDetectionDiagnostics = BlinkDetectionDiagnostics(),
    showBlinkDiagnostics: Boolean = false,
    sensitivityLevel: Int = com.idworx.lisa.DEFAULT_SENSITIVITY_LEVEL,
    onDecreaseSensitivity: () -> Unit = {},
    onIncreaseSensitivity: () -> Unit = {},
    responseTimeSec: Int = com.idworx.lisa.SequenceProcessingDelay.GUIDED_DEFAULT_SECONDS,
    onDecreaseResponseTime: () -> Unit = {},
    onIncreaseResponseTime: () -> Unit = {},
    coachPacingBlocked: Boolean = false,
    lessonNumber: Int? = null,
    totalLessons: Int? = null,
    onLessonNarration: () -> Unit,
    uiStrings: LisaUiStrings = LisaUiStrings(com.idworx.lisa.PreferredLanguage.English),
    eyeTrackingStatus: EyeTrackingStatusUiState? = null
) {
    LaunchedEffect(phrase, coachPacingBlocked) {
        if (!coachPacingBlocked && LisaSpeechPolicy.allowsNarration()) {
            onLessonNarration()
        }
    }

    val watchingLabel = when {
        !eyeTracking.cameraActive -> "Waiting for camera"
        !eyeTracking.faceDetected || !eyeTracking.eyesDetected -> "Eyes not detected"
        eyeTracking.eyesDetected -> "Eyes detected"
        else -> "Watching your eyes"
    }

    val panelLeft = if (lessonInteraction.liveLeftBlinks > 0) {
        lessonInteraction.liveLeftBlinks
    } else {
        eyeTracking.leftBlinkCount
    }
    val panelRight = if (lessonInteraction.liveRightBlinks > 0) {
        lessonInteraction.liveRightBlinks
    } else {
        eyeTracking.rightBlinkCount
    }
    val panelTracking = eyeTracking.copy(leftBlinkCount = panelLeft, rightBlinkCount = panelRight)
    val status = (eyeTrackingStatus ?: EyeTrackingStatusUiMapper.fromTraining(
        uiStrings = uiStrings,
        eyeTracking = panelTracking,
        sensitivity = sensitivityLevel,
        responseTimeSeconds = responseTimeSec,
        leftBlinkCount = panelLeft,
        rightBlinkCount = panelRight
    )).copy(
        statusText = watchingLabel,
        leftBlinkCount = panelLeft,
        rightBlinkCount = panelRight,
        sensitivity = sensitivityLevel,
        responseTimeSeconds = responseTimeSec
    )

    TrainingSoftBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            // RC7D.36 — shared compact blink counter is mandatory on every Guided Learning lesson.
            // CompactEyeTrackingHeader renders EyeTrackingStatusPill + BlinkCounterRow + sensitivity.
            CompactEyeTrackingHeader(
                state = status,
                uiStrings = uiStrings,
                showSensitivityControls = true,
                onDecreaseSensitivity = onDecreaseSensitivity,
                onIncreaseSensitivity = onIncreaseSensitivity,
                onDecreaseResponseTime = onDecreaseResponseTime,
                onIncreaseResponseTime = onIncreaseResponseTime,
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .padding(bottom = 8.dp)
            )
            // Detailed left/right counters (LessonEyeStatusPanel) stay visible for lesson feedback.
            LessonEyeStatusPanel(
                eyeTracking = panelTracking,
                modifier = Modifier
                    .fillMaxWidth(0.82f)
                    .padding(bottom = 4.dp)
            )
            AcceptedBlinkMessage(
                leftCount = panelLeft,
                rightCount = panelRight,
                modifier = Modifier
                    .fillMaxWidth(0.82f)
                    .padding(bottom = 8.dp)
            )
            if (showBlinkDiagnostics) {
                BlinkDetectionDiagnosticsPanel(
                    diagnostics = blinkDiagnostics,
                    modifier = Modifier
                        .fillMaxWidth(0.82f)
                        .padding(bottom = 12.dp)
                )
            }
            if (lessonInteraction.wrongEyeMessage != null) {
                Text(
                    text = lessonInteraction.wrongEyeMessage,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = LisaEmergencyRed,
                    textAlign = TextAlign.Center,
                    lineHeight = 28.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )
            }
            if (lessonInteraction.successVisualMessage == null &&
                lessonInteraction.retryVisualMessage == null &&
                lessonInteraction.detectedProgress != null
            ) {
                Text(
                    text = "Detected Progress",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = LisaGray,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = lessonInteraction.detectedProgress,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = LisaBlue,
                    textAlign = TextAlign.Center,
                    lineHeight = 26.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                )
                if (lessonInteraction.waitingForLabel != null) {
                    Text(
                        text = "Waiting for:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = LisaGray,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = lessonInteraction.waitingForLabel,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = LisaBlueDark,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp)
                    )
                } else {
                    Text(
                        text = "\u2713 Sequence complete",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = LisaBlue,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp)
                    )
                }
            }
            if (lessonInteraction.successVisualMessage != null) {
                Text(
                    text = lessonInteraction.successVisualMessage,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = LisaBlue,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                )
            }
            if (lessonInteraction.retryVisualMessage != null) {
                Text(
                    text = lessonInteraction.retryVisualMessage,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = LisaEmergencyRed.copy(alpha = 0.78f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                )
            }
            if (lessonInteraction.successVisualMessage == null &&
                lessonInteraction.retryVisualMessage == null
            ) {
                if (lessonNumber != null && totalLessons != null) {
                    GuidedLessonProgressLabel(
                        current = lessonNumber,
                        total = totalLessons,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                GuidedLessonPhraseTitle(
                    phrase = phrase,
                    modifier = Modifier.padding(bottom = 28.dp)
                )
                SimplifiedGestureDisplay(left = left, right = right)
                Spacer(Modifier.height(28.dp))
            }
        }
    }
}

@Composable
fun NavigationLessonScreen(
    title: String,
    instruction: String,
    awaitingAction: Boolean,
    lessonNumber: Int? = null,
    totalLessons: Int? = null,
    onNarration: () -> Unit
) {
    LaunchedEffect(title) {
        onNarration()
    }

    TrainingSoftBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (lessonNumber != null && totalLessons != null) {
                GuidedLessonProgressLabel(
                    current = lessonNumber,
                    total = totalLessons,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            Text(
                text = title,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = LisaBlueDark,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(20.dp))
            if (awaitingAction) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "I'll wait until you're ready.",
                    fontSize = 16.sp,
                    color = LisaGray,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun TrainingCalibrationScreen(
    dotIndex: Int,
    totalDots: Int,
    onCalibrationStarted: () -> Unit,
    onAdvanceDot: () -> Unit
) {
    LaunchedEffect(Unit) {
        onCalibrationStarted()
    }
    LaunchedEffect(dotIndex) {
        if (dotIndex < totalDots) {
            kotlinx.coroutines.delay(4500L)
            onAdvanceDot()
        }
    }

    val pulseTransition = rememberInfiniteTransition(label = "calDotPulse")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "calDotScale"
    )

    val dotAlignment = when (dotIndex.coerceAtMost(totalDots - 1) % 5) {
        0 -> Alignment.Center
        1 -> Alignment.TopCenter
        2 -> Alignment.CenterEnd
        3 -> Alignment.BottomCenter
        else -> Alignment.CenterStart
    }

    TrainingSoftBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            TrainingLisaLogo(modifier = Modifier.size(96.dp))
            Spacer(Modifier.height(32.dp))
            Text(
                text = "Look at the blue dot",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = LisaBlueDark,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = dotAlignment
            ) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .scale(pulseScale)
                        .background(Color(0xFF0D47A1), CircleShape)
                )
            }
        }
    }
}

private fun LisaUiStrings.t(en: String, af: String, zu: String): String = when (language) {
    com.idworx.lisa.PreferredLanguage.English -> en
    com.idworx.lisa.PreferredLanguage.Afrikaans -> af
    com.idworx.lisa.PreferredLanguage.IsiZulu -> zu
}
