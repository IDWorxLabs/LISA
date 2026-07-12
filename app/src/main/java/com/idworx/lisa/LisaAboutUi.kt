package com.idworx.lisa

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.idworx.lisa.ui.theme.LisaBlueDark
import com.idworx.lisa.ui.theme.LisaWhite
import java.util.Locale

@Composable
fun AboutLisaPanel(
    uiStrings: LisaUiStrings,
    appVersionInfo: LisaAppVersionInfo,
    onBack: () -> Unit
) {
    LisaPanelShell(title = uiStrings.aboutLisa, onBack = onBack, backLabel = uiStrings.back) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 420.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PanelPurposeLine(uiStrings.aboutLisaPurpose)
            AboutHeroBlock(uiStrings = uiStrings)

            AboutSection(title = uiStrings.aboutWhatIsLisaTitle, body = uiStrings.aboutWhatIsLisaBody)
            AboutSection(title = uiStrings.aboutWhoIsLisaForTitle, body = uiStrings.aboutWhoIsLisaForBody)
            AboutSection(title = uiStrings.aboutHowLisaWorksTitle, bullets = uiStrings.aboutHowLisaWorksBullets)
            AboutSection(title = uiStrings.aboutPrivacySummaryTitle, bullets = uiStrings.aboutPrivacySummaryBullets)
            AboutSection(title = uiStrings.aboutSafetyTitle, bullets = uiStrings.aboutSafetyBullets)
            AboutSection(
                title = uiStrings.aboutVersionTitle,
                body = uiStrings.versionAndBuildLabel(appVersionInfo.versionName, appVersionInfo.versionCode)
            )
            AboutSection(title = uiStrings.aboutCreatorTitle, body = uiStrings.aboutCreatorBody)
            AboutSection(title = uiStrings.aboutCopyrightTitle, body = uiStrings.copyrightNotice)
            AboutSection(
                title = uiStrings.aboutSupportTitle,
                bullets = listOf(uiStrings.aboutSupportWebsite, uiStrings.aboutSupportEmail)
            )
        }
    }
}

@Composable
private fun AboutHeroBlock(uiStrings: LisaUiStrings) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(LisaWhite)
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.splash_logo),
            contentDescription = uiStrings.aboutLisa,
            modifier = Modifier.size(72.dp)
        )
        Text(
            text = "LISA",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = LisaBlueDark,
            textAlign = TextAlign.Center
        )
        Text(
            text = uiStrings.lisaFullName,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = LisaBlueDark.copy(alpha = 0.75f),
            textAlign = TextAlign.Center
        )
        Text(
            text = uiStrings.lisaTagline,
            fontSize = 12.sp,
            color = LisaBlueDark.copy(alpha = 0.65f),
            lineHeight = 16.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun AboutSection(
    title: String,
    body: String? = null,
    bullets: List<String>? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        AboutSectionLabel(title)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(LisaWhite)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            if (body != null) {
                Text(
                    text = body,
                    fontSize = 13.sp,
                    color = LisaBlueDark.copy(alpha = 0.88f),
                    lineHeight = 18.sp
                )
            }
            bullets?.forEach { item ->
                Text(
                    text = "• $item",
                    fontSize = 13.sp,
                    color = LisaBlueDark.copy(alpha = 0.88f),
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
private fun AboutSectionLabel(text: String) {
    Text(
        text = text.uppercase(Locale.getDefault()),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = LisaBlueDark.copy(alpha = 0.55f),
        letterSpacing = 0.5.sp
    )
}
