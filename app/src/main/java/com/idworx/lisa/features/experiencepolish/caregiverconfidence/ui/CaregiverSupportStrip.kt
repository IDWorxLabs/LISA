package com.idworx.lisa.features.experiencepolish.caregiverconfidence.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.idworx.lisa.features.experiencepolish.caregiverconfidence.model.CaregiverSupportUiState
import com.idworx.lisa.ui.theme.LisaBlueDark
import com.idworx.lisa.ui.theme.LisaGray
import com.idworx.lisa.ui.theme.LisaSoftGray

@Composable
fun CaregiverSupportStrip(
    support: CaregiverSupportUiState?,
    modifier: Modifier = Modifier,
    title: String = "Caregiver",
    lightBackground: Boolean = true
) {
    if (support == null) return
    val primary = support.primaryHint ?: return
    val bg = if (lightBackground) LisaSoftGray else Color.White.copy(alpha = 0.10f)
    val titleColor = if (lightBackground) LisaGray else Color.White.copy(alpha = 0.85f)
    val bodyColor = if (lightBackground) LisaBlueDark.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.72f)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = titleColor
        )
        support.progressLine?.let { progress ->
            Text(
                text = progress,
                fontSize = 12.sp,
                color = titleColor,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Text(
            text = primary.removePrefix("Caregiver: ").trim(),
            fontSize = 13.sp,
            color = bodyColor,
            lineHeight = 18.sp,
            modifier = Modifier.fillMaxWidth()
        )
        support.whatToDoNow?.let { action ->
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Now: ${action.removePrefix("Caregiver: ").trim()}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = bodyColor.copy(alpha = 0.9f),
                lineHeight = 16.sp,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
