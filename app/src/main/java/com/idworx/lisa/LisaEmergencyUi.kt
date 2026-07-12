package com.idworx.lisa

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.idworx.lisa.features.brain1interactionstandard.model.Brain1DecisionKind
import com.idworx.lisa.features.brain1interactionstandard.model.Brain1DecisionState
import com.idworx.lisa.ui.theme.LisaEmergencyRed
import com.idworx.lisa.ui.theme.LisaWhite

/** True when Brain1 is waiting for emergency confirm/cancel (L6 R0 armed). */
fun emergencyAwaitingConfirm(brain1Decision: Brain1DecisionState): Boolean =
    brain1Decision.kind == Brain1DecisionKind.EmergencyMode && brain1Decision.isActive

/**
 * Global emergency layer — rendered last in [LisaRootUI] so it sits above Compose Mode,
 * Communication, Settings, and all other panels (RC7D.3).
 */
@Composable
fun GlobalEmergencyOverlayLayer(
    uiStrings: LisaUiStrings,
    emergencyActive: Boolean,
    emergencyAwaitingConfirm: Boolean,
    modifier: Modifier = Modifier
) {
    when {
        emergencyActive -> EmergencyAlarmOverlay(uiStrings = uiStrings, modifier = modifier)
        emergencyAwaitingConfirm -> Brain1EmergencyConfirmOverlay(
            uiStrings = uiStrings,
            modifier = modifier
        )
    }
}

/** Full-screen flashing alarm after emergency is confirmed. */
@Composable
fun EmergencyAlarmOverlay(
    uiStrings: LisaUiStrings,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "emergency_flash")
    val flashAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.88f,
        animationSpec = infiniteRepeatable(animation = tween(600), repeatMode = RepeatMode.Reverse),
        label = "emergency_alpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(LisaEmergencyRed.copy(alpha = flashAlpha)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = uiStrings.emergency,
                color = LisaWhite,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.padding(5.dp))
            Text(
                text = uiStrings.callingForHelp,
                color = LisaWhite,
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }
    }
}

/** Full-screen confirm/cancel overlay shown immediately after L6 R0 is armed. */
@Composable
fun Brain1EmergencyConfirmOverlay(
    uiStrings: LisaUiStrings,
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
        }
    }
}
