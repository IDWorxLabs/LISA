package com.idworx.lisa.features.personalisedeyeprofile

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import kotlinx.coroutines.delay

/**
 * Debug-only Personalised Eye Profile wizard (touch-only).
 * Mirrors Eye Test Mode presentation; never drives production gestures.
 */
@Composable
fun PersonalisedEyeProfileScreen(
    controller: PersonalisedEyeProfileController,
    onBack: () -> Unit,
    tickMs: Long = 200L
) {
    var uiPhase by remember { mutableStateOf(controller.uiPhase) }
    var flowPhase by remember { mutableStateOf(controller.flowPhase) }
    var live by remember { mutableStateOf(controller.live) }
    var status by remember { mutableStateOf(controller.statusMessage) }
    var profile by remember { mutableStateOf(controller.profile) }
    var comparison by remember { mutableStateOf(controller.comparisonText) }
    var remainingMs by remember { mutableStateOf(controller.remainingMs()) }
    var reportText by remember { mutableStateOf(controller.lastReportText) }
    var report by remember { mutableStateOf(controller.lastReport) }
    var reportPath by remember { mutableStateOf(controller.lastReportFilePath) }
    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current

    LaunchedEffect(controller) {
        while (true) {
            controller.onTimedTick()
            uiPhase = controller.uiPhase
            flowPhase = controller.flowPhase
            live = controller.live
            status = controller.statusMessage
            profile = controller.profile
            comparison = controller.comparisonText
            remainingMs = controller.remainingMs()
            reportText = controller.lastReportText
            report = controller.lastReport
            reportPath = controller.lastReportFilePath
            delay(tickMs)
        }
    }

    val allowScroll = uiPhase == PersonalisedEyeProfileController.UiPhase.Hub ||
        uiPhase == PersonalisedEyeProfileController.UiPhase.ReviewThresholds ||
        uiPhase == PersonalisedEyeProfileController.UiPhase.Comparison ||
        uiPhase == PersonalisedEyeProfileController.UiPhase.Failed ||
        uiPhase == PersonalisedEyeProfileController.UiPhase.Report

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
            DebugBanner()
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = phaseLabel(uiPhase, flowPhase),
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
                        .clickable {
                            controller.close()
                            onBack()
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            InstructionCard(
                title = live.instructionTitle.ifBlank { PersonalisedEyeProfileAccess.ENTRY_TITLE },
                body = live.instructionBody.ifBlank { PersonalisedEyeProfileAccess.ENTRY_SUPPORTING }
            )

            Spacer(modifier = Modifier.height(10.dp))

            when (uiPhase) {
                PersonalisedEyeProfileController.UiPhase.Hub -> HubBody(
                    profile = profile,
                    onCreate = { controller.startCalibration() },
                    onRecalibrate = { controller.recalibrate() },
                    onValidate = {
                        controller.nextValidationRunNumber()?.let { controller.startValidationRun(it) }
                    },
                    onTest = {
                        if (profile?.validationRun1 != null || profile?.validationRun2 != null) {
                            controller.showComparison()
                        } else {
                            controller.nextValidationRunNumber()
                                ?.let { controller.startValidationRun(it) }
                        }
                    },
                    onDelete = { controller.deletePrototypeProfile() },
                    onReturn = {
                        controller.close()
                        onBack()
                    }
                )
                PersonalisedEyeProfileController.UiPhase.Calibrating -> ActiveFlowBody(
                    live = live,
                    remainingMs = remainingMs,
                    showCountdown = flowPhase != PersonalisedEyeProfileController.FlowPhase.Readiness &&
                        flowPhase != PersonalisedEyeProfileController.FlowPhase.DeriveThresholds,
                    status = status,
                    onSkip = { controller.skipCurrentStep() }
                )
                PersonalisedEyeProfileController.UiPhase.ReviewThresholds -> ReviewBody(
                    profile = profile,
                    onValidate = { controller.startValidationRun(1) },
                    onHub = { controller.returnToHub() }
                )
                PersonalisedEyeProfileController.UiPhase.Validating -> ActiveFlowBody(
                    live = live,
                    remainingMs = remainingMs,
                    showCountdown = true,
                    status = status,
                    onSkip = { controller.skipCurrentStep() },
                    showWinkProgress = flowPhase ==
                        PersonalisedEyeProfileController.FlowPhase.LeftWink5 ||
                        flowPhase == PersonalisedEyeProfileController.FlowPhase.RightWink5 ||
                        flowPhase == PersonalisedEyeProfileController.FlowPhase.ObserveL1R1 ||
                        flowPhase == PersonalisedEyeProfileController.FlowPhase.ObserveL2R2
                )
                PersonalisedEyeProfileController.UiPhase.Comparison -> ComparisonBody(
                    text = comparison,
                    profile = profile,
                    onHub = { controller.returnToHub() },
                    onValidateNext = {
                        controller.nextValidationRunNumber()?.let { controller.startValidationRun(it) }
                    }
                )
                PersonalisedEyeProfileController.UiPhase.Failed -> FailedBody(
                    profile = profile,
                    status = status,
                    onHub = { controller.returnToHub() },
                    onRecalibrate = { controller.recalibrate() },
                    onRetryValidation = {
                        controller.nextValidationRunNumber()?.let { controller.startValidationRun(it) }
                    }
                )
                PersonalisedEyeProfileController.UiPhase.Report -> ReportBody(
                    report = report,
                    reportText = reportText,
                    reportPath = reportPath,
                    canRetryValidation = controller.canRetryValidation(),
                    onCopy = {
                        if (reportText.isNotBlank()) {
                            clipboard.setText(androidx.compose.ui.text.AnnotatedString(reportText))
                        }
                    },
                    onRecalibrate = { controller.recalibrate() },
                    onRetryValidation = {
                        controller.nextValidationRunNumber()?.let { controller.startValidationRun(it) }
                    },
                    onRestartEntire = { controller.restartEntireProfile() },
                    onHub = { controller.returnToHub() }
                )
            }

            if (status.isNotBlank() &&
                uiPhase != PersonalisedEyeProfileController.UiPhase.Failed &&
                uiPhase != PersonalisedEyeProfileController.UiPhase.Comparison &&
                uiPhase != PersonalisedEyeProfileController.UiPhase.Report
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
        }
    }
}

@Composable
private fun DebugBanner() {
    Text(
        text = "DEBUG ONLY — Standard Mode unchanged",
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = LisaBlueDark,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .background(LisaBlueLight.copy(alpha = 0.85f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    )
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
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = LisaBlueDark,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = body,
            fontSize = 16.sp,
            color = LisaBlueDark.copy(alpha = 0.88f),
            textAlign = TextAlign.Center,
            lineHeight = 22.sp,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun HubBody(
    profile: PersonalisedEyeProfile?,
    onCreate: () -> Unit,
    onRecalibrate: () -> Unit,
    onValidate: () -> Unit,
    onTest: () -> Unit,
    onDelete: () -> Unit,
    onReturn: () -> Unit
) {
    val hasProfile = profile != null
    val canValidate = profile?.status == PersonalisedEyeProfileStatus.Calibrated ||
        profile?.status == PersonalisedEyeProfileStatus.ReadyForValidationRun1 ||
        profile?.status == PersonalisedEyeProfileStatus.ValidationRun1Passed ||
        profile?.status == PersonalisedEyeProfileStatus.ReadyForValidationRun2 ||
        profile?.status == PersonalisedEyeProfileStatus.FailedValidation

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        ProfileCard(
            title = "Standard Profile",
            body = "Uses LISA’s current proven default detection settings."
        )
        Spacer(modifier = Modifier.height(10.dp))
        ProfileCard(
            title = "Personalised Profile",
            body = if (hasProfile) {
                "Status: ${profile!!.status}\nCalibrated for this user’s own eye signals."
            } else {
                "Calibrated for this user’s own eye signals.\nNo prototype profile yet."
            }
        )
        Spacer(modifier = Modifier.height(14.dp))
        TrainingPrimaryButton(
            text = if (hasProfile) "Create / Replace Profile" else "Create Personalised Profile",
            onClick = onCreate,
            minHeight = 56.dp
        )
        if (hasProfile) {
            Spacer(modifier = Modifier.height(8.dp))
            TrainingSecondaryButton(
                text = "Recalibrate",
                onClick = onRecalibrate,
                minHeight = 52.dp
            )
        }
        if (canValidate) {
            Spacer(modifier = Modifier.height(8.dp))
            TrainingSecondaryButton(
                text = "Validate Profile",
                onClick = onValidate,
                minHeight = 52.dp
            )
        }
        if (hasProfile) {
            Spacer(modifier = Modifier.height(8.dp))
            TrainingSecondaryButton(
                text = "Test Profile",
                onClick = onTest,
                minHeight = 52.dp
            )
            Spacer(modifier = Modifier.height(8.dp))
            TrainingSecondaryButton(
                text = "Delete Prototype Profile",
                onClick = onDelete,
                minHeight = 52.dp
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        TrainingSecondaryButton(
            text = "Return to Standard Profile",
            onClick = onReturn,
            minHeight = 52.dp
        )
    }
}

@Composable
private fun ProfileCard(title: String, body: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(LisaBlueLight.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = LisaBlueDark
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = body,
            fontSize = 14.sp,
            color = LisaBlueDark.copy(alpha = 0.9f),
            lineHeight = 20.sp
        )
    }
}

@Composable
private fun ActiveFlowBody(
    live: PersonalisedEyeProfileController.LiveUi,
    remainingMs: Long,
    showCountdown: Boolean,
    status: String,
    onSkip: () -> Unit,
    showWinkProgress: Boolean = false
) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        LiveStrip(live = live)
        Spacer(modifier = Modifier.height(10.dp))

        if (live.calibrationGroupLabel.isNotBlank()) {
            Text(
                text = live.calibrationGroupLabel,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = LisaBlueDark,
                textAlign = TextAlign.Center
            )
            if (live.calibrationGroupLabel.contains("Eye Calibration", ignoreCase = true) &&
                live.totalCycles > 0
            ) {
                Text(
                    text = "Calibration ${live.cycleIndex + 1} of ${live.totalCycles}",
                    fontSize = 15.sp,
                    color = LisaGray,
                    modifier = Modifier.padding(top = 2.dp, bottom = 6.dp)
                )
            } else {
                Spacer(modifier = Modifier.height(6.dp))
            }
        }

        if (live.recordingStatus.isNotBlank()) {
            Text(
                text = "Status",
                fontSize = 13.sp,
                color = LisaGray
            )
            Text(
                text = live.recordingStatus,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = when (live.stepSegment) {
                    PersonalisedEyeProfileController.StepSegment.Recording -> LisaBlue
                    PersonalisedEyeProfileController.StepSegment.Complete -> LisaBlueDark
                    else -> LisaBlueDark
                },
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        if (showCountdown &&
            live.stepSegment != PersonalisedEyeProfileController.StepSegment.Complete
        ) {
            val seconds = ceil((remainingMs.coerceAtLeast(0L)) / 1000.0).toInt()
            Text(
                text = "$seconds",
                fontSize = 64.sp,
                fontWeight = FontWeight.Bold,
                color = LisaBlueDark
            )
            Text(
                text = when (live.stepSegment) {
                    PersonalisedEyeProfileController.StepSegment.Prepare -> "seconds to prepare"
                    PersonalisedEyeProfileController.StepSegment.Recording -> "seconds recording"
                    else -> "seconds remaining"
                },
                fontSize = 16.sp,
                color = LisaGray
            )
        } else if (!showCountdown) {
            Text(
                text = if (live.readinessReady) {
                    "Ready — starting…"
                } else {
                    "Hold steady… ${live.readinessStableMs / 1000}s / 3s"
                },
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = LisaBlueDark,
                textAlign = TextAlign.Center
            )
            ReadyRow("Face detected", live.faceDetected)
            ReadyRow("Face size", live.faceWidthPercent != null)
            ReadyRow("Left probability", !live.leftNull)
            ReadyRow("Right probability", !live.rightNull)
            ReadyRow("Frame accepted", live.frameAccepted)
        }
        if (showWinkProgress &&
            live.stepSegment == PersonalisedEyeProfileController.StepSegment.Recording
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Step L${live.stepLeftWinks} R${live.stepRightWinks} · " +
                    "Run L${live.runLeftWinks} R${live.runRightWinks} · FP ${live.falsePositiveWinks}",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = LisaBlueDark,
                textAlign = TextAlign.Center
            )
        }
        if (status.isNotBlank()) {
            Text(
                text = status,
                fontSize = 13.sp,
                color = LisaBlueDark,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        TrainingSecondaryButton(
            text = "Skip and Record Failure",
            onClick = onSkip,
            minHeight = 52.dp
        )
    }
}

@Composable
private fun LiveStrip(live: PersonalisedEyeProfileController.LiveUi) {
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
    }
}

@Composable
private fun ReviewBody(
    profile: PersonalisedEyeProfile?,
    onValidate: () -> Unit,
    onHub: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        if (profile != null) {
            Metric("Left closed / open", thrPair(profile.leftClosedThreshold, profile.leftOpenThreshold))
            Metric("Right closed / open", thrPair(profile.rightClosedThreshold, profile.rightOpenThreshold))
            Metric("Left open baseline", "%.3f".format(profile.leftOpenBaseline))
            Metric("Left closed baseline", "%.3f".format(profile.leftClosedBaseline))
            Metric("Right open baseline", "%.3f".format(profile.rightOpenBaseline))
            Metric("Right closed baseline", "%.3f".format(profile.rightClosedBaseline))
            Metric("Status", profile.status.name)
            if (profile.derivationNotes.isNotBlank()) {
                Text(
                    text = profile.derivationNotes,
                    fontSize = 12.sp,
                    color = LisaGray,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        TrainingPrimaryButton(
            text = "Start Validation Run 1",
            onClick = onValidate,
            minHeight = 56.dp
        )
        Spacer(modifier = Modifier.height(8.dp))
        TrainingSecondaryButton(text = "Return to Hub", onClick = onHub, minHeight = 52.dp)
    }
}

@Composable
private fun ComparisonBody(
    text: String,
    profile: PersonalisedEyeProfile?,
    onHub: () -> Unit,
    onValidateNext: () -> Unit
) {
    val canNext = profile?.status == PersonalisedEyeProfileStatus.ValidationRun1Passed ||
        profile?.status == PersonalisedEyeProfileStatus.ReadyForValidationRun2
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = text.ifBlank { "No comparison data yet." },
            fontSize = 14.sp,
            color = LisaBlueDark,
            lineHeight = 20.sp,
            modifier = Modifier
                .fillMaxWidth()
                .background(LisaBlueLight.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                .padding(12.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        if (canNext) {
            TrainingPrimaryButton(
                text = "Start Validation Run 2",
                onClick = onValidateNext,
                minHeight = 56.dp
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        TrainingSecondaryButton(text = "Return to Hub", onClick = onHub, minHeight = 52.dp)
    }
}

@Composable
private fun ReportBody(
    report: PersonalisedEyeProfileReport?,
    reportText: String,
    reportPath: String?,
    canRetryValidation: Boolean,
    onCopy: () -> Unit,
    onRecalibrate: () -> Unit,
    onRetryValidation: () -> Unit,
    onRestartEntire: () -> Unit,
    onHub: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "PERSONALISED EYE PROFILE REPORT",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = LisaBlueDark,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (report != null) {
            ReportHeaderCard(report)
            Spacer(modifier = Modifier.height(10.dp))
            ReportEyeCard(report.leftEye)
            Spacer(modifier = Modifier.height(8.dp))
            ReportEyeCard(report.rightEye)
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Validation stages",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = LisaBlueDark
            )
            if (report.validationStages.isEmpty()) {
                Text(
                    text = "Validation not run.",
                    fontSize = 14.sp,
                    color = LisaGray,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            } else {
                report.validationStages.forEach { stage ->
                    Text(
                        text = "${stage.name}: ${if (stage.passed) "PASS" else "FAIL"}",
                        fontSize = 14.sp,
                        color = LisaBlueDark,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Failure analysis",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = LisaBlueDark
            )
            Text(
                text = "Overall confidence: ${report.overallConfidence}",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = LisaBlueDark,
                modifier = Modifier.padding(vertical = 4.dp)
            )
            report.failureSummary.forEach { line ->
                Text(
                    text = "• $line",
                    fontSize = 13.sp,
                    color = LisaBlueDark,
                    modifier = Modifier.padding(vertical = 1.dp)
                )
            }
            report.potentialCause?.let { cause ->
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Potential cause",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = LisaBlueDark
                )
                Text(text = cause, fontSize = 13.sp, color = LisaBlueDark)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Recommendations",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = LisaBlueDark
            )
            if (report.recommendations.isEmpty()) {
                Text(
                    text = "None from measured diagnostics.",
                    fontSize = 13.sp,
                    color = LisaGray
                )
            } else {
                report.recommendations.forEach { rec ->
                    Text(
                        text = "• $rec",
                        fontSize = 13.sp,
                        color = LisaBlueDark,
                        modifier = Modifier.padding(vertical = 1.dp)
                    )
                }
            }
        } else {
            Text(
                text = reportText.ifBlank { "Report unavailable." },
                fontSize = 13.sp,
                color = LisaBlueDark
            )
        }
        if (!reportPath.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Saved: $reportPath",
                fontSize = 11.sp,
                color = LisaGray
            )
        }
        Spacer(modifier = Modifier.height(14.dp))
        TrainingPrimaryButton(text = "Copy Full Report", onClick = onCopy, minHeight = 56.dp)
        Spacer(modifier = Modifier.height(8.dp))
        TrainingSecondaryButton(text = "Recalibrate", onClick = onRecalibrate, minHeight = 52.dp)
        Spacer(modifier = Modifier.height(8.dp))
        TrainingSecondaryButton(
            text = if (canRetryValidation) {
                "Retry Validation"
            } else {
                "Retry Validation (requires successful calibration)"
            },
            onClick = {
                if (canRetryValidation) onRetryValidation()
            },
            minHeight = 52.dp
        )
        Spacer(modifier = Modifier.height(8.dp))
        TrainingSecondaryButton(
            text = "Restart Entire Profile",
            onClick = onRestartEntire,
            minHeight = 52.dp
        )
        Spacer(modifier = Modifier.height(8.dp))
        TrainingSecondaryButton(text = "Return to Hub", onClick = onHub, minHeight = 52.dp)
    }
}

@Composable
private fun ReportHeaderCard(report: PersonalisedEyeProfileReport) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(LisaBlueLight.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
            .padding(12.dp)
    ) {
        Text(
            text = PersonalisedEyeProfileReportAuthority.HEADER_TITLE,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = LisaBlueDark
        )
        MetaLine("Session ID", report.sessionId)
        MetaLine(
            "Test Started",
            PersonalisedEyeProfileReportAuthority.formatLocal(report.testStartedMs)
        )
        MetaLine(
            "Test Completed",
            PersonalisedEyeProfileReportAuthority.formatLocal(report.testCompletedMs)
        )
        MetaLine(
            "Report Generated",
            PersonalisedEyeProfileReportAuthority.formatLocal(report.reportGeneratedMs)
        )
        MetaLine("Phone model", "${report.deviceManufacturer} ${report.deviceModel}".trim())
        MetaLine("Android version", report.androidVersion)
        MetaLine("App Version", "${report.appVersionName} (${report.versionCode})")
        MetaLine("Debug Build", report.isDebugBuild.toString())
        MetaLine(
            "Calibration",
            if (report.calibrationPassed) "PASS" else "FAIL"
        )
    }
}

@Composable
private fun ReportEyeCard(eye: EyeCalibrationReportSection) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(LisaWhite, RoundedCornerShape(10.dp))
            .border(1.dp, LisaBlue.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
            .padding(12.dp)
    ) {
        Text(
            text = "${eye.eyeLabel}: ${if (eye.passed) "PASS" else "FAIL"}",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = LisaBlueDark
        )
        MetaLine("OPEN avg/min/max", listOf(eye.openAverage, eye.openMinimum, eye.openMaximum)
            .joinToString(" / ") { it?.let { v -> "%.3f".format(v) } ?: "n/a" })
        MetaLine(
            "CLOSED avg/min/max",
            listOf(eye.closedAverage, eye.closedMinimum, eye.closedMaximum)
                .joinToString(" / ") { it?.let { v -> "%.3f".format(v) } ?: "n/a" }
        )
        MetaLine(
            "Closed / open thr",
            "${eye.derivedClosedThreshold?.let { "%.3f".format(it) } ?: "n/a"} / " +
                (eye.derivedOpenThreshold?.let { "%.3f".format(it) } ?: "n/a")
        )
        MetaLine(
            "Misclassification %",
            "closed↑ ${eye.closedMisclassificationPercent?.let { "%.1f".format(it) } ?: "n/a"} · " +
                "open↓ ${eye.openMisclassificationPercent?.let { "%.1f".format(it) } ?: "n/a"}"
        )
        MetaLine("Separation", eye.separation?.let { "%.3f".format(it) } ?: "n/a")
    }
}

@Composable
private fun MetaLine(label: String, value: String) {
    Text(
        text = "$label: $value",
        fontSize = 12.sp,
        color = LisaBlueDark,
        modifier = Modifier.padding(top = 2.dp)
    )
}

@Composable
private fun FailedBody(
    profile: PersonalisedEyeProfile?,
    status: String,
    onHub: () -> Unit,
    onRecalibrate: () -> Unit,
    onRetryValidation: () -> Unit
) {
    val reasons = profile?.failureReasons.orEmpty()
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = profile?.status?.name ?: "Failed",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = LisaBlueDark
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (reasons.isNotEmpty()) {
            reasons.forEach { reason ->
                Text(
                    text = "• $reason",
                    fontSize = 15.sp,
                    color = LisaBlueDark,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                )
            }
        } else if (status.isNotBlank()) {
            Text(text = status, fontSize = 15.sp, color = LisaBlueDark, textAlign = TextAlign.Center)
        }
        Spacer(modifier = Modifier.height(14.dp))
        TrainingPrimaryButton(text = "Recalibrate", onClick = onRecalibrate, minHeight = 56.dp)
        Spacer(modifier = Modifier.height(8.dp))
        TrainingSecondaryButton(
            text = "Retry Validation",
            onClick = onRetryValidation,
            minHeight = 52.dp
        )
        Spacer(modifier = Modifier.height(8.dp))
        TrainingSecondaryButton(text = "Return to Hub", onClick = onHub, minHeight = 52.dp)
    }
}

@Composable
private fun ReadyRow(label: String, ok: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 15.sp, color = LisaBlueDark)
        Text(
            text = if (ok) "Yes" else "No",
            fontSize = 15.sp,
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

private fun thrPair(closed: Float, open: Float): String =
    "%.3f / %.3f".format(closed, open)

private fun phaseLabel(
    ui: PersonalisedEyeProfileController.UiPhase,
    flow: PersonalisedEyeProfileController.FlowPhase
): String = when (ui) {
    PersonalisedEyeProfileController.UiPhase.Hub -> "Profile hub"
    PersonalisedEyeProfileController.UiPhase.Calibrating -> "Calibrating · $flow"
    PersonalisedEyeProfileController.UiPhase.ReviewThresholds -> "Review thresholds"
    PersonalisedEyeProfileController.UiPhase.Validating -> "Validating · $flow"
    PersonalisedEyeProfileController.UiPhase.Comparison -> "Comparison"
    PersonalisedEyeProfileController.UiPhase.Failed -> "Failed"
    PersonalisedEyeProfileController.UiPhase.Report -> "Full report"
}
