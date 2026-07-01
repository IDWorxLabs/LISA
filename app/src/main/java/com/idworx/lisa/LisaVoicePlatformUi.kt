package com.idworx.lisa

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.idworx.lisa.ui.theme.LisaBlue
import com.idworx.lisa.ui.theme.LisaBlueDark
import com.idworx.lisa.ui.theme.LisaGray
import com.idworx.lisa.ui.theme.LisaSoftGray
import com.idworx.lisa.ui.theme.LisaWhite

private val VoiceActiveGreen = androidx.compose.ui.graphics.Color(0xFF2E7D52)
private val VoiceComingSoonGray = androidx.compose.ui.graphics.Color(0xFF6B7C8F)

// ── Voice Home ──────────────────────────────────────────────────────────────

@Composable
fun VoiceHomePanel(
    uiStrings: LisaUiStrings,
    onOpenDeviceVoice: () -> Unit,
    onOpenPremiumVoices: () -> Unit,
    onOpenMyVoice: () -> Unit,
    onOpenFamilyVoice: () -> Unit,
    onBack: () -> Unit
) {
    val packs = VoicePlatformCatalog.homePacks(uiStrings)
    LisaPanelShell(title = "", onBack = onBack) {
        VoiceScrollColumn {
            VoiceHero(title = uiStrings.voice, subtitle = uiStrings.voiceHomeSubtitle)
            Spacer(Modifier.height(4.dp))
            packs.forEach { pack ->
                val onOpen = when (pack.category) {
                    VoiceCategory.Device -> onOpenDeviceVoice
                    VoiceCategory.Premium -> onOpenPremiumVoices
                    VoiceCategory.MyVoice -> onOpenMyVoice
                    VoiceCategory.Family -> onOpenFamilyVoice
                }
                VoiceCategoryCard(
                    uiStrings = uiStrings,
                    pack = pack,
                    onOpen = onOpen
                )
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

// ── Device Voice ─────────────────────────────────────────────────────────────

@Composable
fun DeviceVoicePanel(
    uiStrings: LisaUiStrings,
    state: LisaVoiceSettingsState,
    onSelectVoice: (String) -> Unit,
    onTestVoice: () -> Unit,
    onInstallVoiceData: () -> Unit,
    onOpenTtsSettings: () -> Unit,
    onBack: () -> Unit
) {
    LisaPanelShell(title = uiStrings.deviceVoiceTitle, onBack = onBack) {
        VoiceScrollColumn {
            VoiceInfoCard(uiStrings.currentLanguage, state.language.label)
            VoiceInfoCard(uiStrings.currentTtsEngine, state.ttsEngineLabel)
            VoiceInfoCard(
                uiStrings.currentVoice,
                state.currentVoiceLabel ?: uiStrings.voiceNotSelected
            )
            VoiceInfoCard(
                uiStrings.voiceAvailability,
                if (state.ttsReady) {
                    LisaTtsVoiceManager.availabilityLabel(state.languageCheck, uiStrings)
                } else {
                    uiStrings.voiceInitializing
                }
            )

            if (state.showPoorVoiceWarning) {
                VoiceNoticeCard(uiStrings.poorLocalVoiceWarning)
            }

            VoiceSectionLabel(uiStrings.installedVoices)
            if (!state.ttsReady) {
                VoiceInfoCard(uiStrings.installedVoices, uiStrings.voiceInitializing)
            } else if (state.availableVoices.isEmpty()) {
                VoiceInfoCard(uiStrings.installedVoices, uiStrings.noMatchingVoices)
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(LisaWhite)
                ) {
                    state.availableVoices.forEachIndexed { index, voice ->
                        DeviceVoiceRow(
                            voice = voice,
                            selected = voice.name == state.selectedVoiceName,
                            onSelect = { onSelectVoice(voice.name) }
                        )
                        if (index < state.availableVoices.lastIndex) {
                            HorizontalDivider(color = LisaBlue.copy(alpha = 0.12f))
                        }
                    }
                }
            }

            Button(
                onClick = onTestVoice,
                enabled = state.ttsReady,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = LisaBlueDark)
            ) {
                Text(uiStrings.testVoice)
            }
            OutlinedButton(onClick = onInstallVoiceData, modifier = Modifier.fillMaxWidth()) {
                Text(uiStrings.installVoiceData)
            }
            OutlinedButton(onClick = onOpenTtsSettings, modifier = Modifier.fillMaxWidth()) {
                Text(uiStrings.openTtsSettings)
            }
            Text(
                text = uiStrings.voiceSettingsSavedHint,
                fontSize = 11.sp,
                color = LisaGray,
                lineHeight = 15.sp
            )
        }
    }
}

// ── Premium Voices ───────────────────────────────────────────────────────────

@Composable
fun PremiumVoicesPanel(
    uiStrings: LisaUiStrings,
    onBack: () -> Unit
) {
    val pack = VoicePlatformCatalog.packForCategory(VoiceCategory.Premium, uiStrings)
    val previews = VoicePlatformCatalog.premiumPreviews(uiStrings)
    val benefits = VoicePlatformCatalog.premiumBenefits(uiStrings)

    LisaPanelShell(title = uiStrings.premiumVoicesTitle, onBack = onBack) {
        VoiceScrollColumn {
            Text(
                text = uiStrings.premiumVoicesIntro,
                fontSize = 13.sp,
                color = LisaBlueDark.copy(alpha = 0.85f),
                lineHeight = 18.sp
            )
            Spacer(Modifier.height(8.dp))
            VoiceSectionLabel(uiStrings.benefits)
            benefits.forEach { benefit ->
                Text(
                    text = "• $benefit",
                    fontSize = 12.sp,
                    color = LisaBlueDark.copy(alpha = 0.8f),
                    lineHeight = 17.sp,
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
            VoiceSectionLabel(uiStrings.availableSoon)
            previews.forEach { preview ->
                PremiumVoicePreviewCard(uiStrings = uiStrings, preview = preview)
                Spacer(Modifier.height(8.dp))
            }
            pack?.highlights?.takeIf { it.isNotEmpty() }?.let { highlights ->
                VoiceNoticeCard(
                    highlights.joinToString("\n") + "\n\n" + uiStrings.moreVoicesComing
                )
            }
        }
    }
}

// ── My Voice ─────────────────────────────────────────────────────────────────

@Composable
fun MyVoicePanel(uiStrings: LisaUiStrings, onBack: () -> Unit) {
    val steps = VoicePlatformCatalog.myVoiceSteps(uiStrings)
    LisaPanelShell(title = uiStrings.myVoiceTitle, onBack = onBack) {
        VoiceScrollColumn {
            Text(
                text = uiStrings.myVoiceIntro,
                fontSize = 13.sp,
                color = LisaBlueDark.copy(alpha = 0.85f),
                lineHeight = 18.sp
            )
            Spacer(Modifier.height(12.dp))
            VoiceProcessSteps(steps)
            Spacer(Modifier.height(16.dp))
            ComingSoonBanner(uiStrings.comingSoon)
        }
    }
}

// ── Family Voice ─────────────────────────────────────────────────────────────

@Composable
fun FamilyVoicePanel(uiStrings: LisaUiStrings, onBack: () -> Unit) {
    val steps = VoicePlatformCatalog.familyVoiceSteps(uiStrings)
    LisaPanelShell(title = uiStrings.familyVoiceTitle, onBack = onBack) {
        VoiceScrollColumn {
            Text(
                text = uiStrings.familyVoiceIntro,
                fontSize = 13.sp,
                color = LisaBlueDark.copy(alpha = 0.85f),
                lineHeight = 18.sp
            )
            Spacer(Modifier.height(12.dp))
            VoiceProcessSteps(steps)
            Spacer(Modifier.height(16.dp))
            ComingSoonBanner(uiStrings.comingSoon)
        }
    }
}

// ── Shared components ────────────────────────────────────────────────────────

@Composable
private fun VoiceScrollColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 460.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content
    )
}

@Composable
private fun VoiceHero(title: String, subtitle: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = LisaBlueDark
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = subtitle,
            fontSize = 14.sp,
            color = LisaBlueDark.copy(alpha = 0.75f),
            lineHeight = 19.sp
        )
    }
}

@Composable
private fun VoiceCategoryCard(
    uiStrings: LisaUiStrings,
    pack: VoicePack,
    onOpen: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(LisaWhite)
            .clickable(onClick = onOpen)
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            VoiceCategoryIcon(category = pack.category)
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = pack.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = LisaBlueDark
                    )
                    VoiceStatusBadge(status = pack.status, uiStrings = uiStrings)
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = pack.description,
                    fontSize = 12.sp,
                    color = LisaBlueDark.copy(alpha = 0.75f),
                    lineHeight = 17.sp
                )
                if (pack.category == VoiceCategory.Premium) {
                    if (pack.highlights.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        pack.highlights.forEach { highlight ->
                            Text(
                                text = highlight,
                                fontSize = 11.sp,
                                color = LisaGray,
                                lineHeight = 15.sp
                            )
                        }
                    }
                    if (pack.previews.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        pack.previews.take(2).forEach { preview ->
                            Text(
                                text = preview.displayName,
                                fontSize = 11.sp,
                                color = LisaGray,
                                lineHeight = 15.sp
                            )
                        }
                        Text(
                            text = uiStrings.moreVoicesComing,
                            fontSize = 11.sp,
                            color = LisaGray,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }
                }
                if (pack.status == VoiceStatus.Active && pack.actionLabel != null) {
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = onOpen,
                        modifier = Modifier.height(36.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp)
                    ) {
                        Text(pack.actionLabel, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun VoiceCategoryIcon(category: VoiceCategory) {
    val (glyph, _) = when (category) {
        VoiceCategory.Device -> "📱" to LisaBlue
        VoiceCategory.Premium -> "✦" to LisaBlueDark
        VoiceCategory.MyVoice -> "🎙" to LisaBlueDark
        VoiceCategory.Family -> "👨‍👩‍👧" to LisaBlueDark
    }
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(LisaSoftGray),
        contentAlignment = Alignment.Center
    ) {
        Text(text = glyph, fontSize = 22.sp)
    }
}

@Composable
private fun VoiceStatusBadge(status: VoiceStatus, uiStrings: LisaUiStrings) {
    val (label, color) = when (status) {
        VoiceStatus.Active -> uiStrings.statusActive to VoiceActiveGreen
        VoiceStatus.ComingSoon -> uiStrings.comingSoon to VoiceComingSoonGray
        VoiceStatus.Installed -> uiStrings.statusInstalled to VoiceActiveGreen
        VoiceStatus.Unavailable -> uiStrings.statusUnavailable to VoiceComingSoonGray
    }
    Text(
        text = label.uppercase(),
        fontSize = 9.sp,
        fontWeight = FontWeight.Bold,
        color = color,
        letterSpacing = 0.6.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

@Composable
private fun PremiumVoicePreviewCard(uiStrings: LisaUiStrings, preview: VoicePreview) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(LisaWhite)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = preview.displayName,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = LisaBlueDark
            )
            Text(
                text = preview.subtitle,
                fontSize = 11.sp,
                color = LisaGray
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { }, enabled = false, modifier = Modifier.height(34.dp)) {
                Text(uiStrings.preview, fontSize = 12.sp)
            }
            Text(
                text = uiStrings.comingSoon,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = VoiceComingSoonGray,
                modifier = Modifier.align(Alignment.CenterVertically)
            )
        }
    }
}

@Composable
private fun VoiceProcessSteps(steps: List<String>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(LisaWhite)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        steps.forEachIndexed { index, step ->
            Text(
                text = step,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = LisaBlueDark,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
            if (index < steps.lastIndex) {
                Text(
                    text = "↓",
                    fontSize = 18.sp,
                    color = LisaBlue,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun ComingSoonBanner(label: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(LisaBlueDark.copy(alpha = 0.1f))
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label.uppercase(),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = LisaBlueDark,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun VoiceInfoCard(title: String, body: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(LisaWhite)
            .padding(12.dp)
    ) {
        Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = LisaBlueDark)
        Spacer(Modifier.height(4.dp))
        Text(body, fontSize = 12.sp, color = LisaBlueDark.copy(alpha = 0.75f), lineHeight = 16.sp)
    }
}

@Composable
private fun VoiceNoticeCard(text: String) {
    Text(
        text = text,
        fontSize = 12.sp,
        color = LisaBlueDark,
        lineHeight = 17.sp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(LisaSoftGray)
            .padding(12.dp)
    )
}

@Composable
private fun VoiceSectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = LisaBlueDark.copy(alpha = 0.55f),
        letterSpacing = 0.5.sp
    )
}

@Composable
private fun DeviceVoiceRow(
    voice: LisaVoiceOption,
    selected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = voice.displayLabel,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = LisaBlueDark,
                lineHeight = 17.sp
            )
            if (voice.isNetwork) {
                Text(text = "Requires internet", fontSize = 11.sp, color = LisaGray)
            }
        }
    }
}
