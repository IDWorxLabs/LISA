package com.idworx.lisa.features.eyediagnostic

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import java.io.File
import kotlin.math.ceil
import kotlinx.coroutines.delay

/**
 * Guided one-step-at-a-time Eye Test Mode UI (debug only).
 * Mandatory flow: main steps → left/right single-eye → result per phase,
 * then TestComplete → FullResults. Failure-tolerant: timeouts, skip, end-phase-early.
 */
@Composable
fun EyeTestModeScreen(
    controller: EyeTestModeController,
    onBack: () -> Unit,
    tickMs: Long = 200L
) {
    var flowState by remember { mutableStateOf(controller.flowState) }
    var live by remember { mutableStateOf(controller.live) }
    var remainingMs by remember { mutableStateOf(controller.remainingTimedMs()) }
    var stepLeft by remember { mutableStateOf(controller.stepLeftWinks) }
    var stepRight by remember { mutableStateOf(controller.stepRightWinks) }
    var status by remember { mutableStateOf(controller.statusMessage) }
    var progressLabel by remember { mutableStateOf(controller.progressLabelCycle) }
    var sessionMeta by remember { mutableStateOf(controller.sessionMeta) }
    var singleEyeUi by remember { mutableStateOf(controller.singleEyeSubtest.ui) }
    var canViewFullResults by remember { mutableStateOf(controller.canViewFullResults) }
    var copyConfirmation by remember { mutableStateOf(controller.copyConfirmation) }
    var canStartAnyway by remember { mutableStateOf(false) }
    var showEndPhaseConfirm by remember { mutableStateOf(false) }
    var techExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val readiness = remember(live) { controller.readiness() }

    LaunchedEffect(controller) {
        while (true) {
            controller.onTimedTick()
            flowState = controller.flowState
            live = controller.live
            remainingMs = controller.remainingTimedMs()
            stepLeft = controller.stepLeftWinks
            stepRight = controller.stepRightWinks
            status = controller.statusMessage
            progressLabel = controller.progressLabelCycle
            sessionMeta = controller.sessionMeta
            singleEyeUi = controller.singleEyeSubtest.ui
            canViewFullResults = controller.canViewFullResults
            copyConfirmation = controller.copyConfirmation
            canStartAnyway = controller.canStartDiagnosticAnyway()
            delay(tickMs)
        }
    }

    val kind = EyeTestFlowAuthority.stepKind(flowState)
    val phase = EyeTestFlowAuthority.phaseKind(flowState)
    val allowScroll = kind == EyeTestStepKind.Result ||
        kind == EyeTestStepKind.TestComplete ||
        kind == EyeTestStepKind.FullResults
    val showLiveStrip = kind != EyeTestStepKind.Preparation &&
        kind != EyeTestStepKind.Result &&
        kind != EyeTestStepKind.TestComplete &&
        kind != EyeTestStepKind.FullResults &&
        kind != EyeTestStepKind.SingleEyeLeft &&
        kind != EyeTestStepKind.SingleEyeRight
    val showEndPhaseEarly = phase == EyeTestSessionKind.WITH_GLASSES &&
        kind != EyeTestStepKind.Result &&
        kind != EyeTestStepKind.TestComplete &&
        kind != EyeTestStepKind.FullResults

    if (showEndPhaseConfirm) {
        AlertDialog(
            onDismissRequest = { showEndPhaseConfirm = false },
            title = { Text("End phase early?") },
            text = {
                Text("End this phase and save the results collected so far?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showEndPhaseConfirm = false
                        controller.endPhaseEarly()
                    }
                ) {
                    Text("End and Save", color = LisaBlueDark)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndPhaseConfirm = false }) {
                    Text("Cancel", color = LisaGray)
                }
            }
        )
    }

    TrainingSoftBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .then(
                    if (allowScroll) Modifier.verticalScroll(rememberScrollState())
                    else Modifier
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = progressLabel,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = LisaBlueDark,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "Exit",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = LisaBlue,
                    modifier = Modifier
                        .clickable(onClick = onBack)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            InstructionCard(
                title = EyeTestFlowAuthority.instructionTitle(flowState),
                body = EyeTestFlowAuthority.instructionBody(flowState)
            )

            if (showLiveStrip) {
                Spacer(modifier = Modifier.height(8.dp))
                DiagnosticStatusStrip(live = live)
            }

            Spacer(modifier = Modifier.height(10.dp))

            when (kind) {
                EyeTestStepKind.Preparation -> PreparationBody(
                    phase = phase ?: EyeTestSessionKind.WITHOUT_GLASSES,
                    readiness = readiness,
                    canStartAnyway = canStartAnyway,
                    onStart = { controller.startCurrentPhase() },
                    onStartAnyway = { controller.startDiagnosticAnyway() }
                )
                EyeTestStepKind.TimedLook,
                EyeTestStepKind.Rest -> TimedBody(remainingMs = remainingMs)
                EyeTestStepKind.WinkLeft -> WinkBody(
                    label = "Left winks detected",
                    current = stepLeft,
                    target = EyeTestFlowAuthority.TARGET_WINKS,
                    remainingMs = remainingMs,
                    onRetry = { controller.retryStep() },
                    onSkip = { controller.skipAndRecordFailure() }
                )
                EyeTestStepKind.WinkRight -> WinkBody(
                    label = "Right winks detected",
                    current = stepRight,
                    target = EyeTestFlowAuthority.TARGET_WINKS,
                    remainingMs = remainingMs,
                    onRetry = { controller.retryStep() },
                    onSkip = { controller.skipAndRecordFailure() }
                )
                EyeTestStepKind.ObserveL1R1 -> SequenceBody(
                    progress = "Observed: L$stepLeft R$stepRight",
                    hint = "Need L1 R1 (left then right)",
                    remainingMs = remainingMs,
                    onRetry = { controller.retryStep() },
                    onSkip = { controller.skipAndRecordFailure() }
                )
                EyeTestStepKind.ObserveL2R2 -> SequenceBody(
                    progress = "Observed: L$stepLeft R$stepRight",
                    hint = "Need L2 R2 — Exit is touch only on this step",
                    remainingMs = remainingMs,
                    onRetry = { controller.retryStep() },
                    onSkip = { controller.skipAndRecordFailure() }
                )
                EyeTestStepKind.SingleEyeLeft,
                EyeTestStepKind.SingleEyeRight -> SingleEyeBody(
                    ui = singleEyeUi,
                    onRetry = { controller.retryStep() },
                    onSkip = { controller.skipAndRecordFailure() }
                )
                EyeTestStepKind.Result -> ResultBody(
                    summary = if (flowState == EyeTestFlowState.WithoutGlassesResult) {
                        controller.withoutSummary
                    } else {
                        controller.withSummary
                    },
                    primaryLabel = if (flowState == EyeTestFlowState.WithoutGlassesResult) {
                        "Continue to With Glasses"
                    } else {
                        "Complete Test"
                    },
                    secondaryLabel = if (flowState == EyeTestFlowState.WithoutGlassesResult) {
                        "Repeat Without Glasses"
                    } else {
                        "Repeat With Glasses"
                    },
                    onPrimary = {
                        if (flowState == EyeTestFlowState.WithoutGlassesResult) {
                            controller.continueToWithGlasses()
                        } else {
                            controller.completeTest()
                        }
                    },
                    onSecondary = {
                        if (flowState == EyeTestFlowState.WithoutGlassesResult) {
                            controller.repeatWithoutGlassesPhase()
                        } else {
                            controller.repeatWithGlassesPhase()
                        }
                    },
                    status = status
                )
                EyeTestStepKind.TestComplete -> TestCompleteBody(
                    completedAtLocal = sessionMeta?.testCompletedLocal
                        ?: sessionMeta?.testStartedLocal,
                    canViewFullResults = canViewFullResults,
                    onViewFullResults = { controller.viewFullResults() },
                    onRestart = { controller.restartFullTest() },
                    status = status
                )
                EyeTestStepKind.FullResults -> FullResultsBody(
                    report = controller.comparison(),
                    findings = EyeTestCombinedReport.factualFindings(controller.componentSlots),
                    copyConfirmation = copyConfirmation,
                    withoutFile = controller.lastSavedWithoutFile,
                    withFile = controller.lastSavedWithFile,
                    combinedFile = controller.lastSavedCombinedReportFile,
                    onCopyFullResults = {
                        val text = controller.fullResultsText()
                        controller.markReportCopied()
                        shareText(
                            context,
                            EyeTestCombinedReport.HEADER_TITLE,
                            text
                        )
                    },
                    onExportCombined = {
                        val text = controller.fullResultsText().ifBlank {
                            controller.lastSavedCombinedReportFile?.readText().orEmpty()
                        }
                        if (text.isNotBlank()) {
                            shareText(
                                context,
                                EyeTestCombinedReport.HEADER_TITLE,
                                text
                            )
                        }
                    },
                    onRestart = { controller.restartFullTest() },
                    onExit = onBack
                )
            }

            if (showEndPhaseEarly) {
                Spacer(modifier = Modifier.height(10.dp))
                TrainingSecondaryButton(
                    text = "End Phase Early",
                    onClick = { showEndPhaseConfirm = true },
                    minHeight = 52.dp
                )
            }

            if (status.isNotBlank() &&
                kind != EyeTestStepKind.Result &&
                kind != EyeTestStepKind.TestComplete &&
                kind != EyeTestStepKind.FullResults
            ) {
                Text(
                    text = status,
                    fontSize = 13.sp,
                    color = LisaBlueDark.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (techExpanded) "Hide technical details" else "Technical details",
                fontSize = 14.sp,
                color = LisaBlue,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clickable { techExpanded = !techExpanded }
                    .padding(6.dp)
            )
            if (techExpanded) {
                TechnicalDetails(live = live, readiness = readiness)
            }

            Spacer(modifier = Modifier.height(10.dp))
            SessionMetaFooter(sessionMeta = sessionMeta)
        }
    }
}

@Composable
private fun SessionMetaFooter(sessionMeta: EyeTestSessionMeta?) {
    val shortId = sessionMeta?.shortSessionId ?: "—"
    val started = sessionMeta?.testStartedLocal ?: "—"
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(LisaBlueLight.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Session ID: $shortId",
            fontSize = 12.sp,
            color = LisaBlueDark.copy(alpha = 0.9f),
            textAlign = TextAlign.Center
        )
        Text(
            text = "Started: $started",
            fontSize = 12.sp,
            color = LisaGray,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun DiagnosticStatusStrip(live: EyeTestModeController.LiveUi) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(LisaBlueLight.copy(alpha = 0.65f), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Text(
            text = "Face: ${if (live.faceDetected) "Detected" else "Not detected"}",
            fontSize = 13.sp,
            color = LisaBlueDark
        )
        Text(
            text = "Left probability: ${live.leftProb?.let { "%.2f".format(it) } ?: "Null"}",
            fontSize = 13.sp,
            color = LisaBlueDark
        )
        Text(
            text = "Right probability: ${live.rightProb?.let { "%.2f".format(it) } ?: "Null"}",
            fontSize = 13.sp,
            color = LisaBlueDark
        )
        Text(
            text = "Frame: ${if (live.frameAccepted) "Accepted" else "Rejected"}",
            fontSize = 13.sp,
            color = LisaBlueDark
        )
        Text(
            text = "Rejection reason: ${live.rejectionReason ?: "none"}",
            fontSize = 13.sp,
            color = LisaBlueDark
        )
    }
}

@Composable
private fun InstructionCard(title: String, body: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(LisaWhite, RoundedCornerShape(16.dp))
            .border(1.dp, LisaBlue.copy(alpha = 0.28f), RoundedCornerShape(16.dp))
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = LisaBlueDark,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = body,
            fontSize = 17.sp,
            color = LisaBlueDark.copy(alpha = 0.88f),
            textAlign = TextAlign.Center,
            lineHeight = 24.sp,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun PreparationBody(
    phase: EyeTestSessionKind,
    readiness: EyeTestFlowAuthority.Readiness,
    canStartAnyway: Boolean,
    onStart: () -> Unit,
    onStartAnyway: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        ReadyRow("Face detected", readiness.faceDetected)
        ReadyRow("Face size", readiness.faceSizeOk)
        ReadyRow("Left eye probability available", readiness.leftEyeDetected)
        ReadyRow("Right eye probability available", readiness.rightEyeDetected)
        ReadyRow("Signal stability", readiness.signalStable)
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = readiness.message,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (readiness.ready) LisaBlueDark else LisaBlue,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(14.dp))
        TrainingPrimaryButton(
            text = when (phase) {
                EyeTestSessionKind.WITHOUT_GLASSES -> "Start Without Glasses Test"
                EyeTestSessionKind.WITH_GLASSES -> "Start With Glasses Test"
            },
            onClick = onStart,
            enabled = readiness.ready,
            minHeight = 64.dp
        )
        if (canStartAnyway) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Eye signals are unstable. Starting now will record the failure for diagnosis.",
                fontSize = 14.sp,
                color = LisaBlueDark,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            TrainingSecondaryButton(
                text = "Start Diagnostic Anyway",
                onClick = onStartAnyway,
                minHeight = 56.dp
            )
        }
    }
}

@Composable
private fun TimedBody(remainingMs: Long?) {
    val seconds = ceil(((remainingMs ?: 0L).coerceAtLeast(0L)) / 1000.0).toInt()
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "$seconds",
            fontSize = 72.sp,
            fontWeight = FontWeight.Bold,
            color = LisaBlueDark
        )
        Text(
            text = "seconds remaining",
            fontSize = 18.sp,
            color = LisaGray
        )
        Text(
            text = "Recording automatically…",
            fontSize = 15.sp,
            color = LisaBlueDark.copy(alpha = 0.75f),
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun WinkBody(
    label: String,
    current: Int,
    target: Int,
    remainingMs: Long?,
    onRetry: () -> Unit,
    onSkip: () -> Unit
) {
    val seconds = ceil(((remainingMs ?: 0L).coerceAtLeast(0L)) / 1000.0).toInt()
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "$label: $current / $target",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = LisaBlueDark,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Time remaining: $seconds seconds",
            fontSize = 18.sp,
            color = LisaGray,
            modifier = Modifier.padding(top = 8.dp)
        )
        Spacer(modifier = Modifier.height(14.dp))
        TrainingSecondaryButton(text = "Retry Step", onClick = onRetry, minHeight = 52.dp)
        Spacer(modifier = Modifier.height(8.dp))
        TrainingSecondaryButton(
            text = "Skip and Record Failure",
            onClick = onSkip,
            minHeight = 52.dp
        )
    }
}

@Composable
private fun SequenceBody(
    progress: String,
    hint: String,
    remainingMs: Long?,
    onRetry: () -> Unit,
    onSkip: () -> Unit
) {
    val seconds = ceil(((remainingMs ?: 0L).coerceAtLeast(0L)) / 1000.0).toInt()
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = progress,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = LisaBlueDark
        )
        Text(
            text = hint,
            fontSize = 16.sp,
            color = LisaGray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            text = "Time remaining: $seconds seconds",
            fontSize = 18.sp,
            color = LisaGray,
            modifier = Modifier.padding(top = 8.dp)
        )
        Spacer(modifier = Modifier.height(14.dp))
        TrainingSecondaryButton(text = "Retry Step", onClick = onRetry, minHeight = 52.dp)
        Spacer(modifier = Modifier.height(8.dp))
        TrainingSecondaryButton(
            text = "Skip and Record Failure",
            onClick = onSkip,
            minHeight = 52.dp
        )
    }
}

@Composable
private fun SingleEyeBody(
    ui: SingleEyeThresholdSubtest.Ui,
    onRetry: () -> Unit,
    onSkip: () -> Unit
) {
    val seconds = ceil((ui.remainingMs.coerceAtLeast(0L)) / 1000.0).toInt()
    val eyeLabel = when (ui.eye) {
        SingleEyeThresholdSubtest.EyeTarget.Left -> "Left"
        SingleEyeThresholdSubtest.EyeTarget.Right -> "Right"
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = ui.instruction.ifBlank { "Follow the on-screen instruction." },
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = LisaBlueDark,
            textAlign = TextAlign.Center,
            lineHeight = 30.sp,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "$seconds",
            fontSize = 64.sp,
            fontWeight = FontWeight.Bold,
            color = LisaBlueDark
        )
        Text(
            text = "seconds remaining",
            fontSize = 16.sp,
            color = LisaGray
        )
        Spacer(modifier = Modifier.height(10.dp))
        Metric("Selected eye", eyeLabel)
        Metric("Cycle", "${ui.cycleIndex + 1} of ${ui.totalCycles}")
        Metric(
            "Selected probability",
            ui.selectedProb?.let { "%.3f".format(it) } ?: "null"
        )
        Metric("Interpreted state", ui.selectedState.name)
        Metric(
            "Closed / open threshold",
            "%.2f / %.2f".format(ui.closedThreshold, ui.openThreshold)
        )
        Metric("Current result", ui.currentResult.ifBlank { "—" })
        Metric(
            "Opposite eye (control)",
            ui.oppositeProb?.let { "%.3f".format(it) } ?: "null"
        )
        ui.outcome?.let { Metric("Outcome", it.name) }
        Spacer(modifier = Modifier.height(14.dp))
        TrainingSecondaryButton(text = "Retry Step", onClick = onRetry, minHeight = 52.dp)
        Spacer(modifier = Modifier.height(8.dp))
        TrainingSecondaryButton(
            text = "Skip and Record Failure",
            onClick = onSkip,
            minHeight = 52.dp
        )
    }
}

@Composable
private fun ResultBody(
    summary: EyeTestSessionSummary?,
    primaryLabel: String,
    secondaryLabel: String,
    onPrimary: () -> Unit,
    onSecondary: () -> Unit,
    status: String
) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        if (summary != null) {
            Metric("Completed steps", summary.completedStepCount.toString())
            Metric("Timed-out steps", summary.timedOutStepCount.toString())
            Metric("Skipped steps", summary.skippedStepCount.toString())
            Metric(
                "Not completed (detection)",
                summary.notCompletedDueToDetectionFailureCount.toString()
            )
            Metric("Samples captured", summary.sampleCount.toString())
            Metric("Accepted frames", pct(summary.acceptedFramePercent))
            Metric("Null left eye", pct(summary.nullLeftPercent))
            Metric("Null right eye", pct(summary.nullRightPercent))
            Metric(
                "Left winks",
                "${summary.leftWinkDetectionsPeak} / ${summary.leftWinkTarget}"
            )
            Metric(
                "Right winks",
                "${summary.rightWinkDetectionsPeak} / ${summary.rightWinkTarget}"
            )
            Metric("L1 R1", summary.sequenceLabel(summary.l1r1Outcome))
            Metric("L2 R2", summary.sequenceLabel(summary.l2r2Outcome))
            Metric("Top rejection", summary.mostCommonRejectionReason ?: "none")
            Metric(
                "Both UNCERTAIN %",
                pct(summary.decisionAnalysis.bothUncertainPercent)
            )
            if (summary.phaseEndedEarly) {
                Metric("Phase ended early", "Yes")
            }
        }
        if (status.isNotBlank()) {
            Text(
                text = status,
                fontSize = 14.sp,
                color = LisaBlueDark,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
        TrainingPrimaryButton(text = primaryLabel, onClick = onPrimary, minHeight = 64.dp)
        Spacer(modifier = Modifier.height(10.dp))
        TrainingSecondaryButton(text = secondaryLabel, onClick = onSecondary, minHeight = 56.dp)
    }
}

@Composable
private fun TestCompleteBody(
    completedAtLocal: String?,
    canViewFullResults: Boolean,
    onViewFullResults: () -> Unit,
    onRestart: () -> Unit,
    status: String
) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "New test completed at ${completedAtLocal ?: "—"}",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = LisaBlueDark,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .background(LisaBlueLight.copy(alpha = 0.7f), RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "Complete Test already done.",
            fontSize = 15.sp,
            color = LisaGray,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        if (status.isNotBlank()) {
            Text(
                text = status,
                fontSize = 14.sp,
                color = LisaBlueDark,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        TrainingPrimaryButton(
            text = "View Full Results",
            onClick = onViewFullResults,
            enabled = canViewFullResults,
            minHeight = 64.dp
        )
        if (!canViewFullResults) {
            Text(
                text = "Full results require all six components to finish.",
                fontSize = 13.sp,
                color = LisaGray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        TrainingSecondaryButton(text = "Restart Full Test", onClick = onRestart, minHeight = 52.dp)
    }
}

@Composable
private fun FullResultsBody(
    report: EyeTestComparisonReport,
    findings: List<String>,
    copyConfirmation: String,
    withoutFile: File?,
    withFile: File?,
    combinedFile: File?,
    onCopyFullResults: () -> Unit,
    onExportCombined: () -> Unit,
    onRestart: () -> Unit,
    onExit: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = EyeTestCombinedReport.HEADER_TITLE,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = LisaBlueDark,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        report.sampleSizeWarning?.let {
            Text(
                text = it,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = LisaBlueDark,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        report.incompleteSessionWarning?.let {
            Text(
                text = it,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = LisaBlue,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        report.factualFinding?.let {
            Text(
                text = it,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = LisaBlueDark,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        if (report.bothPresent) {
            val a = report.withoutGlasses!!
            val b = report.withGlasses!!
            CompareRow(
                "Accepted frames",
                pct(a.acceptedFramePercent),
                pct(b.acceptedFramePercent),
                report.highlightDifference(a.acceptedFramePercent, b.acceptedFramePercent)
            )
            CompareRow(
                "Null left",
                pct(a.nullLeftPercent),
                pct(b.nullLeftPercent),
                report.highlightDifference(a.nullLeftPercent, b.nullLeftPercent)
            )
            CompareRow(
                "Null right",
                pct(a.nullRightPercent),
                pct(b.nullRightPercent),
                report.highlightDifference(a.nullRightPercent, b.nullRightPercent)
            )
            CompareRow(
                "Avg left open",
                a.averageLeftOpenProbability?.let { "%.3f".format(it) } ?: "—",
                b.averageLeftOpenProbability?.let { "%.3f".format(it) } ?: "—",
                false
            )
            CompareRow(
                "Avg right open",
                a.averageRightOpenProbability?.let { "%.3f".format(it) } ?: "—",
                b.averageRightOpenProbability?.let { "%.3f".format(it) } ?: "—",
                false
            )
            CompareRow(
                "Left winks",
                a.leftWinkDetectionsPeak.toString(),
                b.leftWinkDetectionsPeak.toString(),
                false
            )
            CompareRow(
                "Right winks",
                a.rightWinkDetectionsPeak.toString(),
                b.rightWinkDetectionsPeak.toString(),
                false
            )
            CompareRow(
                "L1 R1",
                a.sequenceLabel(a.l1r1Outcome),
                b.sequenceLabel(b.l1r1Outcome),
                a.l1r1Outcome != b.l1r1Outcome
            )
            CompareRow(
                "L2 R2",
                a.sequenceLabel(a.l2r2Outcome),
                b.sequenceLabel(b.l2r2Outcome),
                a.l2r2Outcome != b.l2r2Outcome
            )
            CompareRow(
                "Top rejection",
                a.mostCommonRejectionReason ?: "none",
                b.mostCommonRejectionReason ?: "none",
                false
            )
        }
        if (report.decisionFindings.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Decision findings",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = LisaBlueDark
            )
            report.decisionFindings.forEach { finding ->
                Text(
                    text = "• $finding",
                    fontSize = 14.sp,
                    color = LisaBlueDark,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
        if (findings.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Automatic factual findings",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = LisaBlueDark
            )
            findings.forEach { finding ->
                Text(
                    text = "• $finding",
                    fontSize = 14.sp,
                    color = LisaBlueDark,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
        if (copyConfirmation.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = copyConfirmation,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = LisaBlueDark,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(LisaBlueLight.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                    .padding(10.dp)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        TrainingPrimaryButton(
            text = "Copy Full Results",
            onClick = onCopyFullResults,
            minHeight = 56.dp
        )
        Spacer(modifier = Modifier.height(8.dp))
        TrainingSecondaryButton(
            text = "Export Combined Text Report / Share",
            onClick = onExportCombined,
            minHeight = 52.dp
        )
        if (withoutFile != null || withFile != null || combinedFile != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = buildString {
                    combinedFile?.let { appendLine("Combined: ${it.name}") }
                    withoutFile?.let { appendLine("Without CSV: ${it.name}") }
                    withFile?.let { appendLine("With CSV: ${it.name}") }
                }.trim(),
                fontSize = 12.sp,
                color = LisaGray,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        TrainingSecondaryButton(text = "Restart Full Test", onClick = onRestart, minHeight = 52.dp)
        Spacer(modifier = Modifier.height(8.dp))
        TrainingSecondaryButton(text = "Exit Eye Test Mode", onClick = onExit, minHeight = 52.dp)
    }
}

@Composable
private fun TechnicalDetails(
    live: EyeTestModeController.LiveUi,
    readiness: EyeTestFlowAuthority.Readiness
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(LisaBlueLight.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Metric("Face", if (live.faceDetected) "Yes" else "No")
        Metric("Faces", live.faceCount.toString())
        Metric("Face width %", live.faceWidthPercent?.let { "%.1f".format(it) } ?: "—")
        Metric(
            "Yaw / Roll",
            "${live.headYaw?.let { "%.1f".format(it) } ?: "—"} / " +
                (live.headRoll?.let { "%.1f".format(it) } ?: "—")
        )
        Metric(
            "L / R prob",
            "${live.leftProb?.let { "%.3f".format(it) } ?: "null"} / " +
                (live.rightProb?.let { "%.3f".format(it) } ?: "null")
        )
        Metric("Frame", if (live.frameAccepted) "Accepted" else "Rejected")
        Metric("Reason", live.rejectionReason ?: "none")
        Metric("Signal stable", if (readiness.signalStable) "Yes" else "No")
        Metric("Ready", if (readiness.ready) "Yes" else "No")
    }
}

@Composable
private fun ReadyRow(label: String, ok: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 16.sp, color = LisaBlueDark)
        Text(
            text = if (ok) "Yes" else "No",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = if (ok) LisaBlueDark else LisaBlue
        )
    }
}

@Composable
private fun Metric(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 15.sp, color = LisaGray, modifier = Modifier.weight(1f))
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = LisaBlueDark,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun CompareRow(label: String, without: String, with: String, highlight: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(
                if (highlight) LisaBlue.copy(alpha = 0.12f) else LisaBlueLight.copy(alpha = 0.5f),
                RoundedCornerShape(8.dp)
            )
            .padding(8.dp)
    ) {
        Text(text = label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = LisaBlueDark)
        Text(text = "Without glasses: $without", fontSize = 14.sp, color = LisaBlueDark)
        Text(text = "With glasses: $with", fontSize = 14.sp, color = LisaBlueDark)
    }
}

private fun pct(value: Float): String = "%.1f%%".format(value)

private fun shareText(context: Context, subject: String, text: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(send, "Share eye diagnostic results"))
}
