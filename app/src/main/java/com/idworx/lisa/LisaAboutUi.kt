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
import com.idworx.lisa.ui.theme.LisaGray
import com.idworx.lisa.ui.theme.LisaWhite
import java.util.Locale

@Composable
fun AboutLisaPanel(onBack: () -> Unit) {
    LisaPanelShell(title = "About LISA", onBack = onBack) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 420.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AboutHeroBlock()

            AboutSection(
                title = "What is LISA?",
                body = "LISA is an assistive communication app designed to help people with locked-in syndrome and severe motor impairment communicate using intentional eye movements. LISA watches the user's eyes, interprets blink and wink sequences, and turns selected messages into spoken speech."
            )

            AboutSection(
                title = "Who is LISA for?",
                body = "LISA is for people with locked-in syndrome, severe motor impairment, and others who cannot speak reliably but can use intentional eye movements to communicate."
            )

            AboutSection(
                title = "Mission",
                body = "Our mission is to help people who cannot speak regain a practical, reliable voice."
            )

            AboutSection(
                title = "How LISA works",
                bullets = listOf(
                    "The camera detects the face and eyes.",
                    "Intentional wink sequences are recognized.",
                    "LISA matches the sequence to a phrase.",
                    "The user confirms the phrase.",
                    "LISA speaks it aloud.",
                    "Emergency sequences can trigger an alarm."
                )
            )

            AboutSection(
                title = "Privacy",
                bullets = listOf(
                    "Camera processing happens on the device.",
                    "Profiles and caregiver data are stored locally for now.",
                    "No cloud account is used yet.",
                    "No caregiver notifications are sent yet.",
                    "Future cloud features will require clear consent."
                )
            )

            AboutSection(
                title = "Safety notice",
                bullets = listOf(
                    "LISA is an assistive communication tool.",
                    "It is not a certified medical device yet.",
                    "It should not be the only emergency method until professionally validated.",
                    "Emergency features must be tested with caregivers before real dependency."
                )
            )

            AboutSection(
                title = "Version",
                body = "Version: 1.1 local testing build\nStage: Real-world family/friend testing preparation"
            )

            AboutSection(
                title = "Creator / Organization",
                body = "Created by Lungelo Richard Zungu\nProduct: LISA\nOrganization: Asgard Dynamics"
            )

            AboutSection(
                title = "Support / Contact",
                bullets = listOf(
                    "Website: Coming soon",
                    "Support email: Coming soon",
                    "Feedback: Coming soon"
                )
            )

            AboutSection(
                title = "Roadmap",
                bullets = listOf(
                    "Personalized calibration",
                    "Real caregiver phone alerts",
                    "Cloud profiles",
                    "iOS version",
                    "Video calling with blink-to-speech",
                    "Clinical testing and accessibility validation"
                )
            )
        }
    }
}

@Composable
private fun AboutHeroBlock() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(LisaWhite)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "LISA",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = LisaBlueDark
        )
        Text(
            text = "Look Into Speaking Assistant",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = LisaBlueDark.copy(alpha = 0.8f)
        )
        Text(
            text = "Assistive communication through intentional eye movements.",
            fontSize = 13.sp,
            color = LisaGray,
            lineHeight = 18.sp
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
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (body != null) {
                Text(
                    text = body,
                    fontSize = 14.sp,
                    color = LisaBlueDark.copy(alpha = 0.88f),
                    lineHeight = 20.sp
                )
            }
            bullets?.forEach { item ->
                Text(
                    text = "• $item",
                    fontSize = 14.sp,
                    color = LisaBlueDark.copy(alpha = 0.88f),
                    lineHeight = 20.sp
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
