package com.idworx.lisa

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.idworx.lisa.ui.theme.LisaBlueDark
import com.idworx.lisa.ui.theme.LisaWhite
import java.util.Locale

@Composable
fun PrivacyPolicyPanel(uiStrings: LisaUiStrings, onBack: () -> Unit) {
    LisaPanelShell(title = uiStrings.privacyPolicy, onBack = onBack, backLabel = uiStrings.back) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 420.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PanelPurposeLine(uiStrings.privacyPolicyPurpose)

            PolicySection(title = uiStrings.privacyIntroTitle, body = uiStrings.privacyIntroBody)
            PolicySection(title = uiStrings.privacyCameraTitle, body = uiStrings.privacyCameraBody)
            PolicySection(title = uiStrings.privacyOnDeviceTitle, body = uiStrings.privacyOnDeviceBody)
            PolicySection(title = uiStrings.privacyNoSellingTitle, body = uiStrings.privacyNoSellingBody)
            PolicySection(title = uiStrings.privacyYourInfoTitle, body = uiStrings.privacyYourInfoBody)
            PolicySection(title = uiStrings.privacyControlTitle, body = uiStrings.privacyControlBody)
            PolicySection(title = uiStrings.privacyQuestionsTitle, body = uiStrings.privacyQuestionsBody)
        }
    }
}

@Composable
private fun PolicySection(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        PolicySectionLabel(title)
        Text(
            text = body,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(LisaWhite)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            fontSize = 13.sp,
            color = LisaBlueDark.copy(alpha = 0.88f),
            lineHeight = 18.sp
        )
    }
}

@Composable
private fun PolicySectionLabel(text: String) {
    Text(
        text = text.uppercase(Locale.getDefault()),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = LisaBlueDark.copy(alpha = 0.55f),
        letterSpacing = 0.5.sp
    )
}
