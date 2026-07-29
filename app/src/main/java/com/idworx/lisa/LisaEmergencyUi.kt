package com.idworx.lisa

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.idworx.lisa.features.brain1interactionstandard.model.Brain1DecisionKind
import com.idworx.lisa.features.brain1interactionstandard.model.Brain1DecisionState
import com.idworx.lisa.features.eyetrackingstatus.BlinkCounterRow
import com.idworx.lisa.ui.theme.LisaEmergencyRed
import com.idworx.lisa.ui.theme.LisaWhite

/** True when Brain1 is waiting for emergency confirm/cancel (L6 R0 armed). */
fun emergencyAwaitingConfirm(brain1Decision: Brain1DecisionState): Boolean =
    brain1Decision.kind == Brain1DecisionKind.EmergencyMode && brain1Decision.isActive

/**
 * Global emergency layer — rendered last in [LisaRootUI] so it sits above Compose Mode,
 * Communication, Settings, and all other panels (RC7D.3).
 *
 * RC8.14 — no emergency volume adjustment on any emergency surface.
 *
 * RC8.26 / RC8.27 — Emergency Active shows live Left/Right blink counters. Stop uses L1 R1
 * (same counts as confirm, distinct phase). Armed Cancel remains R1 L1.
 */
@Composable
fun GlobalEmergencyOverlayLayer(
    uiStrings: LisaUiStrings,
    emergencyActive: Boolean,
    emergencyAwaitingConfirm: Boolean,
    blinkFeedback: ComposerEyeFeedback,
    onCancelOrStopEmergency: () -> Unit,
    onConfirmEmergency: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    when {
        emergencyActive -> EmergencyAlarmOverlay(
            uiStrings = uiStrings,
            blinkFeedback = blinkFeedback,
            onStopEmergency = onCancelOrStopEmergency,
            modifier = modifier
        )
        emergencyAwaitingConfirm -> Brain1EmergencyConfirmOverlay(
            uiStrings = uiStrings,
            blinkFeedback = blinkFeedback,
            onConfirmEmergency = onConfirmEmergency,
            onCancelEmergency = onCancelOrStopEmergency,
            modifier = modifier
        )
    }
}

/** Full-screen opaque alarm after emergency is confirmed — no underlying UI visible (RC7D.7). */
@Composable
fun EmergencyAlarmOverlay(
    uiStrings: LisaUiStrings,
    blinkFeedback: ComposerEyeFeedback,
    onStopEmergency: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundInteraction = remember { MutableInteractionSource() }
    val stopSequence = uiStrings.guidedEmergencyStopSequenceLabel
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(LisaEmergencyRed)
            .clickable(
                enabled = true,
                indication = null,
                interactionSource = backgroundInteraction,
                onClick = { /* consume background taps */ }
            )
    ) {
        // RC8.26 — compact live eye-tracking strip at the top (shared blink state).
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = uiStrings.eyeTrackingStatusWatching,
                color = LisaWhite,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            EmergencyBlinkFeedbackRows(
                uiStrings = uiStrings,
                blinkFeedback = blinkFeedback
            )
        }
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFFB71C1C))
                .padding(horizontal = 18.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = uiStrings.emergencyActiveTitle,
                color = LisaWhite,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.padding(5.dp))
            Text(
                text = uiStrings.emergencyAlarmActiveMessage,
                color = LisaWhite,
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.padding(4.dp))
            Text(
                text = uiStrings.callingForHelp,
                color = LisaWhite,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.padding(10.dp))
            EmergencyManualButton(
                label = uiStrings.stopEmergency,
                sequenceLabel = stopSequence,
                onClick = onStopEmergency
            )
        }
    }
}

/** Full-screen confirm/cancel overlay shown immediately after L6 R0 is armed. */
@Composable
fun Brain1EmergencyConfirmOverlay(
    uiStrings: LisaUiStrings,
    blinkFeedback: ComposerEyeFeedback,
    onConfirmEmergency: () -> Unit,
    onCancelEmergency: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(LisaEmergencyRed.copy(alpha = 0.82f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(LisaEmergencyRed.copy(alpha = 0.95f))
                .padding(horizontal = 18.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = uiStrings.emergency,
                color = LisaWhite,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.padding(8.dp))
            Text(
                text = uiStrings.guidedEmergencyAwaitingConfirmMessage,
                color = LisaWhite,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                lineHeight = 26.sp
            )
            Spacer(modifier = Modifier.padding(8.dp))
            EmergencyBlinkFeedbackRows(
                uiStrings = uiStrings,
                blinkFeedback = blinkFeedback
            )
            Spacer(modifier = Modifier.padding(10.dp))
            // RC8.28 — Confirm and Cancel share equal visual weight.
            EmergencyManualButton(
                label = uiStrings.confirmEmergency,
                sequenceLabel = uiStrings.guidedConfirmSequenceLabel,
                onClick = onConfirmEmergency
            )
            Spacer(modifier = Modifier.padding(6.dp))
            EmergencyManualButton(
                label = uiStrings.cancelEmergency,
                sequenceLabel = uiStrings.guidedConfirmCancelSequenceLabel,
                onClick = onCancelEmergency
            )
        }
    }
}

@Composable
private fun EmergencyBlinkFeedbackRows(
    uiStrings: LisaUiStrings,
    blinkFeedback: ComposerEyeFeedback
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        BlinkCounterRow(
            uiStrings = uiStrings,
            leftBlinkCount = blinkFeedback.leftWinkCount,
            rightBlinkCount = blinkFeedback.rightWinkCount
        )
        blinkFeedback.partialSequenceLabel()?.let { sequence ->
            Text(
                text = "${uiStrings.phraseComposerPartialSequenceLabel}: $sequence",
                color = LisaWhite,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun EmergencyManualButton(
    label: String,
    sequenceLabel: String,
    onClick: () -> Unit
) {
    androidx.compose.material3.Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "$label, $sequenceLabel" },
        shape = RoundedCornerShape(12.dp),
        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
            containerColor = LisaWhite,
            contentColor = LisaEmergencyRed
        )
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = LisaEmergencyRed,
                textAlign = TextAlign.Center
            )
            Text(
                text = sequenceLabel,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = LisaEmergencyRed.copy(alpha = 0.9f),
                textAlign = TextAlign.Center
            )
        }
    }
}
