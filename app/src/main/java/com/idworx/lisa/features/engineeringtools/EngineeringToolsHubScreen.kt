package com.idworx.lisa.features.engineeringtools

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.idworx.lisa.features.eyediagnostic.EyeTestModeAccess
import com.idworx.lisa.features.glassescharacterisation.GlassesCharacterisationAccess
import com.idworx.lisa.features.onboardingguide.ui.TrainingPrimaryButton
import com.idworx.lisa.features.onboardingguide.ui.TrainingSoftBackground
import com.idworx.lisa.features.personalisedeyeprofile.PersonalisedEyeProfileAccess
import com.idworx.lisa.features.signalinvestigation.SignalInvestigationAccess
import com.idworx.lisa.ui.theme.LisaBlueDark
import com.idworx.lisa.ui.theme.LisaBlueLight
import com.idworx.lisa.ui.theme.LisaWhite

@Composable
fun EngineeringToolsHubScreen(
    onOpenTool: (EngineeringToolsHubAccess.Tool) -> Unit,
    onBack: () -> Unit
) {
    TrainingSoftBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = EngineeringToolsHubAccess.HUB_TITLE,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = LisaBlueDark,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = EngineeringToolsHubAccess.HUB_SUPPORTING,
                fontSize = 14.sp,
                color = LisaBlueDark.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(20.dp))
            HubToolRow(
                title = EyeTestModeAccess.ENTRY_TITLE,
                supporting = EyeTestModeAccess.ENTRY_SUPPORTING_TEXT,
                onClick = { onOpenTool(EngineeringToolsHubAccess.Tool.EyeTestMode) }
            )
            HubToolRow(
                title = PersonalisedEyeProfileAccess.ENTRY_TITLE,
                supporting = PersonalisedEyeProfileAccess.ENTRY_SUPPORTING,
                onClick = { onOpenTool(EngineeringToolsHubAccess.Tool.PersonalisedEyeProfile) }
            )
            HubToolRow(
                title = SignalInvestigationAccess.ENTRY_TITLE,
                supporting = SignalInvestigationAccess.ENTRY_SUPPORTING,
                onClick = { onOpenTool(EngineeringToolsHubAccess.Tool.SignalInvestigation) }
            )
            HubToolRow(
                title = GlassesCharacterisationAccess.ENTRY_TITLE,
                supporting = GlassesCharacterisationAccess.ENTRY_SUPPORTING,
                onClick = { onOpenTool(EngineeringToolsHubAccess.Tool.GlassesCharacterisation) }
            )
            Spacer(modifier = Modifier.height(20.dp))
            TrainingPrimaryButton(
                text = "Back",
                onClick = onBack,
                minHeight = 52.dp
            )
        }
    }
}

@Composable
private fun HubToolRow(
    title: String,
    supporting: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(LisaWhite)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = title,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            color = LisaBlueDark
        )
        Text(
            text = supporting,
            fontSize = 13.sp,
            color = LisaBlueDark.copy(alpha = 0.75f),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(LisaBlueLight.copy(alpha = 0.55f))
                .padding(8.dp)
        )
    }
}
