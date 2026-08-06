package com.idworx.lisa.features.glassescharacterisation

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import kotlinx.coroutines.delay

private val GuideGreen = Color(0xFF43A047)
private val GuideAmber = Color(0xFFFFB300)
private val GuideRed = Color(0xFFE53935)
private val Scrim = Color(0x99000000)

@Composable
fun GlassesCharacterisationScreen(
    controller: GlassesCharacterisationController,
    onBack: () -> Unit,
    tickMs: Long = 200L
) {
    var uiPhase by remember { mutableStateOf(controller.uiPhase) }
    var live by remember { mutableStateOf(controller.live) }
    var reportText by remember { mutableStateOf(controller.lastReportText) }
    var reportPath by remember { mutableStateOf(controller.lastReportFilePath) }
    var copyMsg by remember { mutableStateOf(controller.copyConfirmation) }
    var source by remember { mutableStateOf(controller.sourceLabel) }
    val clipboard = LocalClipboardManager.current

    LaunchedEffect(controller) {
        while (true) {
            controller.onTimedTick()
            uiPhase = controller.uiPhase
            live = controller.live
            reportText = controller.lastReportText
            reportPath = controller.lastReportFilePath
            copyMsg = controller.copyConfirmation
            source = controller.sourceLabel
            delay(tickMs)
        }
    }

    when (uiPhase) {
        GlassesCharUiPhase.Running -> RunningOverlay(
            live = live,
            onToggleTechnical = { controller.toggleTechnicalDetails() },
            onRetry = { controller.retryStep() },
            onSkip = { controller.skipAndRecordFailure() },
            onEndCondition = { controller.endConditionEarly() },
            onEndTest = { controller.endFullTestEarly() },
            onExit = {
                controller.close()
                onBack()
            }
        )
        else -> TrainingSoftBackground {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "DEBUG ONLY — Glasses = YES",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = LisaBlueDark,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(LisaBlueLight.copy(alpha = 0.85f), RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = live.title,
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
                Spacer(Modifier.height(12.dp))
                when (uiPhase) {
                    GlassesCharUiPhase.Hub -> {
                        Text(
                            text = "Does lighting improve open/closed\neye separation with glasses?",
                            fontSize = 18.sp,
                            color = LisaBlueDark,
                            textAlign = TextAlign.Center,
                            lineHeight = 24.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Patient stays still.\nCaregiver changes lighting only.",
                            fontSize = 15.sp,
                            color = LisaGray,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(16.dp))
                        if (live.ttsWarning) {
                            TrainingSecondaryButton(
                                text = "Continue Visual Only",
                                onClick = { controller.confirmVisualOnlyWithoutTts() },
                                minHeight = 52.dp
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                        TrainingPrimaryButton(
                            text = "Start",
                            onClick = { controller.openSetup() },
                            minHeight = 56.dp
                        )
                        Spacer(Modifier.height(8.dp))
                        TrainingSecondaryButton(
                            text = "Return",
                            onClick = {
                                controller.close()
                                onBack()
                            },
                            minHeight = 52.dp
                        )
                    }
                    GlassesCharUiPhase.Setup -> {
                        Text(
                            text = live.body,
                            fontSize = 16.sp,
                            color = LisaBlueDark,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(12.dp))
                        Text("Lighting source (optional)", fontWeight = FontWeight.Bold, color = LisaBlueDark)
                        Spacer(Modifier.height(6.dp))
                        LightingSourceLabel.entries.chunked(2).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                row.forEach { label ->
                                    Chip(label.displayName, source == label) {
                                        controller.selectSourceLabel(label)
                                        source = label
                                    }
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                        }
                        Spacer(Modifier.height(12.dp))
                        TrainingPrimaryButton(
                            text = "Begin Characterisation",
                            onClick = { controller.startTest() },
                            minHeight = 56.dp
                        )
                        Spacer(Modifier.height(8.dp))
                        TrainingSecondaryButton(
                            text = "Back",
                            onClick = { controller.returnToHub() },
                            minHeight = 52.dp
                        )
                    }
                    GlassesCharUiPhase.LightingPrep -> {
                        Text(
                            text = "Session ${live.sessionIdShort}",
                            fontSize = 12.sp,
                            color = LisaGray
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = live.title,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = LisaBlueDark,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = live.body,
                            fontSize = 18.sp,
                            color = LisaBlueDark,
                            textAlign = TextAlign.Center,
                            lineHeight = 24.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(LisaBlueLight.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                                .padding(16.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Do not move the patient, phone, distance, or glasses.",
                            fontSize = 14.sp,
                            color = GuideAmber,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(16.dp))
                        TrainingPrimaryButton(
                            text = "I'm Ready",
                            onClick = { controller.iAmReady() },
                            minHeight = 56.dp
                        )
                        Spacer(Modifier.height(8.dp))
                        TrainingSecondaryButton(
                            text = "End Full Test Early",
                            onClick = { controller.endFullTestEarly() },
                            minHeight = 52.dp
                        )
                    }
                    GlassesCharUiPhase.LightingTransition -> {
                        Text("✓", fontSize = 48.sp, color = GuideGreen)
                        Text(
                            text = "${live.completedConditionName} Complete",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = LisaBlueDark,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(12.dp))
                        Text("↓", fontSize = 28.sp, color = LisaBlueDark)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Next: ${live.nextConditionName}",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = LisaBlueDark,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "Caregiver:\n${live.caregiverTransitionHint}",
                            fontSize = 18.sp,
                            color = LisaBlueDark,
                            textAlign = TextAlign.Center,
                            lineHeight = 24.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(LisaBlueLight.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                                .padding(16.dp)
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "DO NOT move:\n✓ Patient  ✓ Phone  ✓ Distance  ✓ Glasses",
                            fontSize = 15.sp,
                            color = LisaBlueDark,
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Press I'm Ready when only the lighting has changed.",
                            fontSize = 14.sp,
                            color = LisaGray,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(16.dp))
                        TrainingPrimaryButton(
                            text = "I'm Ready",
                            onClick = { controller.iAmReady() },
                            minHeight = 56.dp
                        )
                        Spacer(Modifier.height(8.dp))
                        TrainingSecondaryButton(
                            text = "End Full Test Early",
                            onClick = { controller.endFullTestEarly() },
                            minHeight = 52.dp
                        )
                    }
                    GlassesCharUiPhase.Analysing -> {
                        Text(
                            text = live.status,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = LisaBlueDark,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = live.analyseMessage.ifBlank { "Please wait…" },
                            fontSize = 16.sp,
                            color = LisaGray,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(16.dp))
                        LinearProgressIndicator(
                            progress = { live.progress01 },
                            modifier = Modifier.fillMaxWidth().height(10.dp),
                            color = GuideGreen,
                            trackColor = LisaBlueLight
                        )
                    }
                    GlassesCharUiPhase.FinalReport -> {
                        Text("✓", fontSize = 56.sp, color = GuideGreen)
                        Text(
                            text = "Investigation Complete",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = LisaBlueDark,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = live.body,
                            fontSize = 14.sp,
                            color = LisaGray,
                            textAlign = TextAlign.Center
                        )
                        if (copyMsg.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text(copyMsg, color = GuideGreen, textAlign = TextAlign.Center, fontSize = 14.sp)
                        }
                        if (!reportPath.isNullOrBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text("Saved locally", fontSize = 12.sp, color = LisaGray)
                        }
                        Spacer(Modifier.height(16.dp))
                        TrainingPrimaryButton(
                            text = "Copy Full Report",
                            onClick = {
                                if (reportText.isNotBlank()) {
                                    clipboard.setText(AnnotatedString(reportText))
                                    controller.markCopyDone()
                                    copyMsg = controller.copyConfirmation
                                }
                            },
                            minHeight = 56.dp
                        )
                        Spacer(Modifier.height(8.dp))
                        TrainingSecondaryButton(
                            text = "Share Full Report",
                            onClick = {
                                if (reportText.isNotBlank()) {
                                    clipboard.setText(AnnotatedString(reportText))
                                    controller.markCopyDone()
                                    copyMsg = controller.copyConfirmation
                                }
                            },
                            minHeight = 52.dp
                        )
                        Spacer(Modifier.height(8.dp))
                        TrainingSecondaryButton(
                            text = "Restart Test",
                            onClick = { controller.restart() },
                            minHeight = 52.dp
                        )
                        Spacer(Modifier.height(8.dp))
                        TrainingSecondaryButton(
                            text = "Exit",
                            onClick = {
                                controller.close()
                                onBack()
                            },
                            minHeight = 52.dp
                        )
                    }
                    else -> Unit
                }
            }
        }
    }
}

@Composable
private fun RunningOverlay(
    live: GlassesCharacterisationController.LiveUi,
    onToggleTechnical: () -> Unit,
    onRetry: () -> Unit,
    onSkip: () -> Unit,
    onEndCondition: () -> Unit,
    onEndTest: () -> Unit,
    onExit: () -> Unit
) {
    val recording = live.status == "RECORDING"
    Box(Modifier.fillMaxSize().background(Scrim)) {
        Column(
            Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "${live.conditionIndex + 1}/${live.conditionTotal}  ${live.conditionName}",
                    color = LisaWhite,
                    fontWeight = FontWeight.Bold
                )
                Text("Exit", color = LisaWhite, fontWeight = FontWeight.Bold, modifier = Modifier.clickable(onClick = onExit))
            }
            Spacer(Modifier.weight(0.2f))
            Text(
                text = live.status,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = when {
                    recording -> GuideRed
                    live.status == "COMPLETE" -> GuideGreen
                    else -> LisaWhite
                },
                textAlign = TextAlign.Center
            )
            if (live.flowPhase == GlassesCharFlowPhase.PrepareLeft ||
                live.flowPhase == GlassesCharFlowPhase.RecordLeft ||
                live.flowPhase == GlassesCharFlowPhase.RecoverLeft ||
                live.flowPhase == GlassesCharFlowPhase.PrepareRight ||
                live.flowPhase == GlassesCharFlowPhase.RecordRight ||
                live.flowPhase == GlassesCharFlowPhase.RecoverRight
            ) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Cycle ${live.cycleIndex + 1}/${live.cycleTotal}",
                    color = LisaWhite.copy(alpha = 0.85f),
                    fontSize = 16.sp
                )
            }
            Spacer(Modifier.height(16.dp))
            LinearProgressIndicator(
                progress = { live.progress01 },
                modifier = Modifier.fillMaxWidth().height(10.dp),
                color = if (recording) GuideRed else GuideGreen,
                trackColor = LisaWhite.copy(alpha = 0.25f)
            )
            if (live.remainingMs > 0L) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "${ceil(live.remainingMs / 1000.0).toInt()}",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = LisaWhite
                )
            }
            if (live.positionWarning) {
                Spacer(Modifier.height(12.dp))
                Text(
                    live.body.ifBlank { "Caregiver: return the phone to the previous position." },
                    color = GuideAmber,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.weight(0.35f))
            Text(
                if (live.showTechnical) "Technical Details ▲" else "Technical Details ▼",
                color = LisaWhite.copy(alpha = 0.8f),
                modifier = Modifier.clickable(onClick = onToggleTechnical)
            )
            if (live.showTechnical) {
                Text(live.technicalLine, color = LisaWhite.copy(alpha = 0.7f), fontSize = 12.sp, textAlign = TextAlign.Center)
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallAction("Retry", onRetry)
                SmallAction("Skip", onSkip)
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallAction("End Condition", onEndCondition)
                SmallAction("End Test", onEndTest)
            }
        }
    }
}

@Composable
private fun SmallAction(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        color = LisaWhite,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .background(LisaBlue.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    )
}

@Composable
private fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = label,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = if (selected) LisaWhite else LisaBlueDark,
        modifier = Modifier
            .background(
                if (selected) LisaBlue else LisaBlueLight.copy(alpha = 0.7f),
                RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    )
}
