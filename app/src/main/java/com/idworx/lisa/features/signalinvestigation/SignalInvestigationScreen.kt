package com.idworx.lisa.features.signalinvestigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.idworx.lisa.features.onboardingguide.ui.TrainingPrimaryButton
import com.idworx.lisa.features.onboardingguide.ui.TrainingSecondaryButton
import com.idworx.lisa.features.onboardingguide.ui.TrainingSoftBackground
import com.idworx.lisa.ui.theme.LisaBlue
import com.idworx.lisa.ui.theme.LisaBlueDark
import com.idworx.lisa.ui.theme.LisaBlueLight
import com.idworx.lisa.ui.theme.LisaGray
import com.idworx.lisa.ui.theme.LisaWhite
import kotlin.math.ceil
import kotlin.math.min
import kotlinx.coroutines.delay

private val GuideRed = Color(0xFFE53935)
private val GuideAmber = Color(0xFFFFB300)
private val GuideGreen = Color(0xFF43A047)
private val Scrim = Color(0x99000000)

@Composable
fun SignalInvestigationScreen(
    controller: SignalInvestigationController,
    onBack: () -> Unit,
    tickMs: Long = 200L
) {
    var uiPhase by remember { mutableStateOf(controller.uiPhase) }
    var live by remember { mutableStateOf(controller.live) }
    var report by remember { mutableStateOf(controller.lastReport) }
    var reportText by remember { mutableStateOf(controller.lastReportText) }
    var reportPath by remember { mutableStateOf(controller.lastReportFilePath) }
    var glasses by remember { mutableStateOf(controller.glasses) }
    var lighting by remember { mutableStateOf(controller.lighting) }
    val clipboard = LocalClipboardManager.current

    LaunchedEffect(controller) {
        while (true) {
            controller.onTimedTick()
            uiPhase = controller.uiPhase
            live = controller.live
            report = controller.lastReport
            reportText = controller.lastReportText
            reportPath = controller.lastReportFilePath
            glasses = controller.glasses
            lighting = controller.lighting
            delay(tickMs)
        }
    }

    when (uiPhase) {
        SignalInvestigationController.UiPhase.Running -> {
            PositioningAssistant(
                live = live,
                onToggleTechnical = { controller.toggleTechnicalDetails() },
                onExit = {
                    controller.close()
                    onBack()
                }
            )
        }
        else -> {
            TrainingSoftBackground {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "DEBUG ONLY",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = LisaBlueDark,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(LisaBlueLight.copy(alpha = 0.85f), RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = when (uiPhase) {
                                SignalInvestigationController.UiPhase.Hub -> "Signal Investigation"
                                SignalInvestigationController.UiPhase.Conditions -> "Conditions"
                                SignalInvestigationController.UiPhase.Report ->
                                    "✓ Investigation Complete"
                                else -> ""
                            },
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = LisaBlueDark
                        )
                        Text(
                            text = "Exit",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = LisaBlue,
                            modifier = Modifier.clickable {
                                controller.close()
                                onBack()
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    when (uiPhase) {
                        SignalInvestigationController.UiPhase.Hub -> HubBody(
                            onStartStandard = {
                                controller.openConditions(SignalInvestigationMode.Standard)
                            },
                            onStartAdvanced = {
                                controller.openConditions(
                                    SignalInvestigationMode.AdvancedEngineering
                                )
                            },
                            onReturn = {
                                controller.close()
                                onBack()
                            }
                        )
                        SignalInvestigationController.UiPhase.Conditions -> ConditionsBody(
                            live = live,
                            glasses = glasses,
                            lighting = lighting,
                            onGlasses = {
                                controller.glasses = it
                                glasses = it
                            },
                            onLighting = {
                                controller.lighting = it
                                lighting = it
                            },
                            onVisualOnly = { controller.confirmVisualOnlyWithoutTts() },
                            onBegin = { controller.startInvestigation() },
                            onHub = { controller.returnToHub() }
                        )
                        SignalInvestigationController.UiPhase.Report -> ReportBody(
                            reportPath = reportPath,
                            onCopy = {
                                if (reportText.isNotBlank()) {
                                    clipboard.setText(AnnotatedString(reportText))
                                }
                            },
                            onRestart = { controller.restart() },
                            onExit = {
                                controller.close()
                                onBack()
                            }
                        )
                        else -> Unit
                    }
                }
            }
        }
    }
}

@Composable
private fun HubBody(
    onStartStandard: () -> Unit,
    onStartAdvanced: () -> Unit,
    onReturn: () -> Unit
) {
    Text(
        text = "Can LISA detect your eyes\nin a natural, comfortable position?",
        fontSize = 18.sp,
        color = LisaBlueDark,
        textAlign = TextAlign.Center,
        lineHeight = 24.sp
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = "The patient stays still.\nNo head tilting or leaning required.",
        fontSize = 15.sp,
        color = LisaGray,
        textAlign = TextAlign.Center,
        lineHeight = 20.sp
    )
    Spacer(modifier = Modifier.height(20.dp))
    TrainingPrimaryButton(
        text = "Start Assessment",
        onClick = onStartStandard,
        minHeight = 56.dp
    )
    Spacer(modifier = Modifier.height(10.dp))
    TrainingSecondaryButton(
        text = "Advanced Engineering Investigation",
        onClick = onStartAdvanced,
        minHeight = 52.dp
    )
    Spacer(modifier = Modifier.height(8.dp))
    TrainingSecondaryButton(text = "Return", onClick = onReturn, minHeight = 52.dp)
}

@Composable
private fun ConditionsBody(
    live: SignalInvestigationController.LiveUi,
    glasses: GlassesCondition,
    lighting: LightingCondition,
    onGlasses: (GlassesCondition) -> Unit,
    onLighting: (LightingCondition) -> Unit,
    onVisualOnly: () -> Unit,
    onBegin: () -> Unit,
    onHub: () -> Unit
) {
    if (live.ttsWarning) {
        Text(
            text = "Voice unavailable. Continue visual-only?",
            color = LisaBlueDark,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            fontSize = 16.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        TrainingSecondaryButton(
            text = "Continue Visual Only",
            onClick = onVisualOnly,
            minHeight = 52.dp
        )
        Spacer(modifier = Modifier.height(10.dp))
    }
    Text("Glasses", fontWeight = FontWeight.Bold, color = LisaBlueDark, fontSize = 16.sp)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ChoiceChip("YES", glasses == GlassesCondition.YES) { onGlasses(GlassesCondition.YES) }
        ChoiceChip("NO", glasses == GlassesCondition.NO) { onGlasses(GlassesCondition.NO) }
    }
    Spacer(modifier = Modifier.height(10.dp))
    Text("Lighting", fontWeight = FontWeight.Bold, color = LisaBlueDark, fontSize = 16.sp)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        LightingCondition.entries.forEach { cond ->
            ChoiceChip(cond.name, lighting == cond) { onLighting(cond) }
        }
    }
    Spacer(modifier = Modifier.height(14.dp))
    TrainingPrimaryButton(text = "Begin", onClick = onBegin, minHeight = 56.dp)
    Spacer(modifier = Modifier.height(8.dp))
    TrainingSecondaryButton(text = "Back", onClick = onHub, minHeight = 52.dp)
}

@Composable
private fun PositioningAssistant(
    live: SignalInvestigationController.LiveUi,
    onToggleTechnical: () -> Unit,
    onExit: () -> Unit
) {
    val guide = SignalInvestigationVisual.fromLive(live)
    val ringColor = when (guide.tone) {
        SignalInvestigationVisual.Tone.Red -> GuideRed
        SignalInvestigationVisual.Tone.Amber -> GuideAmber
        SignalInvestigationVisual.Tone.Green -> GuideGreen
        SignalInvestigationVisual.Tone.Recording -> GuideRed
        SignalInvestigationVisual.Tone.Neutral -> LisaWhite
    }
    val progress by animateFloatAsState(
        targetValue = guide.progress01,
        animationSpec = tween(durationMillis = 280),
        label = "guideProgress"
    )
    val arrowAlpha by animateFloatAsState(
        targetValue = if (guide.arrow == SignalInvestigationVisual.Arrow.None) 0f else 1f,
        animationSpec = tween(durationMillis = 220),
        label = "arrowAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Scrim)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${live.positionIndex + 1} / ${live.positionTotal}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = LisaWhite
                )
                Text(
                    text = "Exit",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = LisaWhite,
                    modifier = Modifier.clickable(onClick = onExit)
                )
            }

            Spacer(modifier = Modifier.weight(0.08f))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                FaceGuideOverlay(
                    ringColor = ringColor,
                    progress = progress,
                    showRing = guide.tone == SignalInvestigationVisual.Tone.Green ||
                        guide.tone == SignalInvestigationVisual.Tone.Amber ||
                        guide.tone == SignalInvestigationVisual.Tone.Recording ||
                        guide.showPerfectCheck ||
                        guide.showCompleteCheck,
                    recording = guide.showRecordingDot
                )
                if (arrowAlpha > 0.05f) {
                    Text(
                        text = SignalInvestigationVisual.arrowGlyph(guide.arrow),
                        fontSize = 96.sp,
                        fontWeight = FontWeight.Bold,
                        color = ringColor.copy(alpha = arrowAlpha),
                        textAlign = TextAlign.Center
                    )
                }
                if (guide.showNextPosition) {
                    Text(
                        text = "➔",
                        fontSize = 80.sp,
                        fontWeight = FontWeight.Bold,
                        color = LisaWhite,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                if (guide.showRecordingDot) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 8.dp)
                            .size(22.dp)
                            .background(GuideRed, CircleShape)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            AnimatedContent(
                targetState = guide.status,
                transitionSpec = {
                    fadeIn(tween(200)) togetherWith fadeOut(tween(160))
                },
                label = "status"
            ) { status ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (guide.showCompleteCheck) {
                        Text(
                            text = "✓ COMPLETE",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = GuideGreen,
                            textAlign = TextAlign.Center
                        )
                    } else {
                        Text(
                            text = status.uppercase(),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = LisaWhite,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            if (live.environmentHint.isNotBlank() &&
                live.investigationMode == SignalInvestigationMode.Standard
            ) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = live.environmentHint,
                    fontSize = 14.sp,
                    color = GuideAmber,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }

            val showCountdown =
                live.flowPhase == SignalInvestigationController.FlowPhase.Recording ||
                    live.flowPhase == SignalInvestigationController.FlowPhase.Stabilizing ||
                    live.flowPhase == SignalInvestigationController.FlowPhase.Prepare ||
                    live.flowPhase == SignalInvestigationController.FlowPhase.StandardObserve
            if (showCountdown && live.remainingMs > 0L) {
                val seconds = ceil(live.remainingMs.coerceAtLeast(0L) / 1000.0).toInt()
                Text(
                    text = "$seconds",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = LisaWhite.copy(alpha = 0.9f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = if (live.showTechnical) "Technical Details ▲" else "Technical Details ▼",
                fontSize = 14.sp,
                color = LisaWhite.copy(alpha = 0.85f),
                modifier = Modifier.clickable(onClick = onToggleTechnical)
            )
            if (live.showTechnical) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = buildString {
                        append("yaw=${live.technicalYaw?.let { "%.1f".format(it) } ?: "n/a"}  ")
                        append("roll=${live.technicalRoll?.let { "%.1f".format(it) } ?: "n/a"}  ")
                        append("face%=${live.faceWidthPercent?.let { "%.1f".format(it) } ?: "n/a"}\n")
                        append("accepted=${live.technicalAcceptedFrames}  ")
                        append("rejected=${live.technicalRejectedFrames}  ")
                        append("poseReject=${live.technicalPoseMismatchRejects}\n")
                        append("centerY=${live.technicalCenterY?.let { "%.1f".format(it) } ?: "n/a"}  ")
                        append("L=${live.leftProb?.let { "%.2f".format(it) } ?: "n/a"}  ")
                        append("R=${live.rightProb?.let { "%.2f".format(it) } ?: "n/a"}")
                    },
                    fontSize = 12.sp,
                    color = LisaWhite.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun FaceGuideOverlay(
    ringColor: Color,
    progress: Float,
    showRing: Boolean,
    recording: Boolean
) {
    Canvas(modifier = Modifier.fillMaxSize(0.85f)) {
        val minDim = min(size.width, size.height)
        val ovalW = minDim * 0.58f
        val ovalH = minDim * 0.74f
        val left = (size.width - ovalW) / 2f
        val top = (size.height - ovalH) / 2f
        // Neutral head silhouette (oval outline — not a real face)
        drawOval(
            color = Color.White.copy(alpha = 0.55f),
            topLeft = Offset(left, top),
            size = Size(ovalW, ovalH),
            style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
        )
        if (showRing) {
            val stroke = if (recording) 10.dp.toPx() else 14.dp.toPx()
            val pad = 18.dp.toPx()
            val ringLeft = Offset(left - pad, top - pad)
            val ringSize = Size(ovalW + pad * 2, ovalH + pad * 2)
            // Soft full guide ring
            drawArc(
                color = ringColor.copy(alpha = 0.35f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = ringLeft,
                size = ringSize,
                style = Stroke(width = stroke * 0.7f, cap = StrokeCap.Round)
            )
            // Progress fill (stabilize / prepare / record)
            val sweep = 360f * progress.coerceIn(0f, 1f)
            if (sweep > 1f) {
                drawArc(
                    color = ringColor.copy(alpha = 0.95f),
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = ringLeft,
                    size = ringSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }
        }
    }
}

@Composable
private fun ChoiceChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = label,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = if (selected) LisaWhite else LisaBlueDark,
        modifier = Modifier
            .background(
                if (selected) LisaBlue else LisaBlueLight.copy(alpha = 0.7f),
                RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    )
}

@Composable
private fun ReportBody(
    reportPath: String?,
    onCopy: () -> Unit,
    onRestart: () -> Unit,
    onExit: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "✓",
            fontSize = 72.sp,
            fontWeight = FontWeight.Bold,
            color = GuideGreen
        )
        Text(
            text = "Investigation Complete",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = LisaBlueDark,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Full engineering report is ready to copy.",
            fontSize = 15.sp,
            color = LisaGray,
            textAlign = TextAlign.Center
        )
        if (!reportPath.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Saved locally",
                fontSize = 12.sp,
                color = LisaGray
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        TrainingPrimaryButton(text = "Copy Full Report", onClick = onCopy, minHeight = 56.dp)
        Spacer(modifier = Modifier.height(8.dp))
        TrainingSecondaryButton(
            text = "Restart Investigation",
            onClick = onRestart,
            minHeight = 52.dp
        )
        Spacer(modifier = Modifier.height(8.dp))
        TrainingSecondaryButton(text = "Exit", onClick = onExit, minHeight = 52.dp)
    }
}
