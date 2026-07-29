package com.idworx.lisa.features.invalidsequencefeedback

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.idworx.lisa.ui.theme.LisaEmergencyRed

/**
 * RC8.44 — shared red invalid-sequence banner for Welcome / guided-entry surfaces.
 * Visual only — never triggers LISA TTS.
 */
@Composable
fun InvalidSequenceWarningBanner(
    warning: UniversalInvalidSequenceAuthority.Warning?,
    modifier: Modifier = Modifier
) {
    if (warning == null) return
    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                liveRegion = LiveRegionMode.Polite
                contentDescription = warning.combinedDisplay()
            }
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = warning.primaryLine,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = LisaEmergencyRed,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = warning.contextLine,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = LisaEmergencyRed,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp)
        )
    }
}
