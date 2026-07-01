package com.idworx.lisa

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.idworx.lisa.ui.theme.LisaBlue
import java.util.Locale
import com.idworx.lisa.ui.theme.LisaBlueDark
import com.idworx.lisa.ui.theme.LisaBlueLight
import com.idworx.lisa.ui.theme.LisaEmergencyRed
import com.idworx.lisa.ui.theme.LisaGray
import com.idworx.lisa.ui.theme.LisaSoftGray
import com.idworx.lisa.ui.theme.LisaWhite

@Composable
fun LisaRootUI(
    uiStrings: LisaUiStrings,
    userDisplay: LisaUserDisplay,
    emergencyActive: Boolean,
    emergencyNotifyNames: List<String> = emptyList(),
    developerMode: Boolean,
    activePanel: LisaPanel,
    lastSpoken: String,
    countdownActive: Boolean,
    sensitivityLevel: Int,
    settingsState: LisaSettingsUiState,
    textSizeScale: Float = 1.0f,
    profiles: List<LisaUserProfile> = emptyList(),
    activeProfileId: String = "",
    caregivers: List<LisaCaregiver> = emptyList(),
    developerInfo: DeveloperPanelInfo,
    mappings: List<WinkMapping>,
    onMenuClick: () -> Unit,
    onSelectPanel: (LisaPanel) -> Unit,
    onClosePanel: () -> Unit,
    onBackToMenu: () -> Unit,
    onDeveloperModeChange: (Boolean) -> Unit,
    onSensitivityDecrease: () -> Unit,
    onSensitivityIncrease: () -> Unit,
    onSettingsPlaceholderChange: (LisaSettingsUiState) -> Unit,
    onCreateProfile: () -> Unit = {},
    onSelectProfile: (String) -> Unit = {},
    onUpdateProfile: (LisaUserProfile) -> Unit = {},
    onDeleteProfile: (String) -> Unit = {},
    onAddCaregiver: (LisaCaregiver) -> Unit = {},
    onUpdateCaregiver: (LisaCaregiver) -> Unit = {},
    onDeleteCaregiver: (String) -> Unit = {},
    onRepeat: () -> Unit,
    onReset: () -> Unit,
    onEditCountdown: () -> Unit,
    onAddMapping: (left: Int, right: Int, phrase: String) -> Unit,
    onboardingCompleted: Boolean = true,
    cameraPermissionGranted: Boolean = true,
    cameraPermissionPermanentlyDenied: Boolean = false,
    primaryUserName: String = "Primary User",
    testingChecklist: Map<String, Boolean> = emptyMap(),
    feedbackSavedCount: Int = 0,
    onPrimaryUserNameChange: (String) -> Unit = {},
    onCompleteOnboarding: () -> Unit = {},
    onRequestCameraPermission: () -> Unit = {},
    onOpenAppSettings: () -> Unit = {},
    onSaveFeedback: (String, String, String, String) -> Unit = { _, _, _, _ -> },
    onToggleChecklistItem: (String, Boolean) -> Unit = { _, _ -> },
    voiceSettingsState: LisaVoiceSettingsState = LisaVoiceSettingsState(),
    onSelectTtsVoice: (String) -> Unit = {},
    onTestTtsVoice: () -> Unit = {},
    onInstallTtsVoiceData: () -> Unit = {},
    onOpenTtsSettings: () -> Unit = {},
    cameraView: @Composable () -> Unit
) {
    if (!onboardingCompleted) {
        OnboardingFlow(
            uiStrings = uiStrings,
            primaryUserName = primaryUserName,
            onPrimaryUserNameChange = onPrimaryUserNameChange,
            onRequestCameraPermission = onRequestCameraPermission,
            onComplete = onCompleteOnboarding
        )
        return
    }

    val canRepeat = lastSpoken.isNotBlank()
    val density = LocalDensity.current
    Box(modifier = Modifier.fillMaxSize()) {
        if (cameraPermissionGranted) {
            cameraView()
        } else {
            CameraPermissionScreen(
                uiStrings = uiStrings,
                permanentlyDenied = cameraPermissionPermanentlyDenied,
                onRequestPermission = onRequestCameraPermission,
                onOpenSettings = onOpenAppSettings
            )
        }

        if (emergencyActive) {
            EmergencyOverlay(uiStrings = uiStrings, notifyNames = emergencyNotifyNames)
        }

        CompositionLocalProvider(
            LocalDensity provides Density(density.density, density.fontScale * textSizeScale)
        ) {
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CompactTimelineChip(uiStrings = uiStrings, activeStage = userDisplay.timelineStage)

            Spacer(Modifier.height(4.dp))

            if (userDisplay.showIntentPreview && userDisplay.phrase != null) {
                IntentPreviewCard(phrase = userDisplay.phrase, compact = !countdownActive)
                Spacer(Modifier.height(4.dp))
            }

            EverydayCommunicationPanel(
                uiStrings = uiStrings,
                userDisplay = userDisplay,
                countdownActive = countdownActive,
                onEditCountdown = onEditCountdown
            )

            Spacer(Modifier.height(4.dp))
            CompactSensitivityControls(
                uiStrings = uiStrings,
                sensitivityLevel = sensitivityLevel,
                onDecrease = onSensitivityDecrease,
                onIncrease = onSensitivityIncrease
            )

            if (!developerMode && (userDisplay.leftWinkDots > 0 || userDisplay.rightWinkDots > 0)) {
                Spacer(Modifier.height(4.dp))
                SequenceProgressDots(
                    uiStrings = uiStrings,
                    leftCount = userDisplay.leftWinkDots,
                    rightCount = userDisplay.rightWinkDots
                )
            }

            if (developerMode) {
                Spacer(Modifier.height(4.dp))
                DeveloperPanel(info = developerInfo)
            }
        }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LisaActionButton(
                    text = if (activePanel.isOpen()) uiStrings.close else uiStrings.menu,
                    modifier = Modifier.weight(1f),
                    filled = !activePanel.isOpen(),
                    onClick = onMenuClick
                )
                LisaActionButton(
                    text = uiStrings.reset,
                    modifier = Modifier.weight(1f),
                    filled = false,
                    danger = emergencyActive,
                    onClick = onReset
                )
                if (canRepeat) {
                    LisaActionButton(
                        text = uiStrings.repeat,
                        modifier = Modifier.weight(1f),
                        filled = false,
                        onClick = onRepeat
                    )
                }
            }

            if (activePanel != LisaPanel.None) {
                Spacer(Modifier.height(10.dp))
                when (activePanel) {
                    LisaPanel.Menu -> MenuPanel(
                        uiStrings = uiStrings,
                        canRepeat = canRepeat,
                        onSelectPanel = onSelectPanel,
                        onRepeat = onRepeat,
                        onClose = onClosePanel
                    )
                    LisaPanel.MyCommunication -> MyCommunicationPanel(
                        uiStrings = uiStrings,
                        profiles = profiles,
                        activeProfileId = activeProfileId,
                        onCreateProfile = onCreateProfile,
                        onSelectProfile = onSelectProfile,
                        onUpdateProfile = onUpdateProfile,
                        onDeleteProfile = onDeleteProfile,
                        onBack = onBackToMenu
                    )
                    LisaPanel.CommunicationSetup -> PlaceholderPanel(
                        title = "Communication Setup",
                        description = "Configure how LISA listens and confirms your messages. Coming soon.",
                        onBack = onBackToMenu
                    )
                    LisaPanel.VocabularyTraining -> VocabularyTrainingPanel(
                        uiStrings = uiStrings,
                        preferredLanguage = uiStrings.language,
                        mappings = mappings,
                        onAddMapping = onAddMapping,
                        onBack = onBackToMenu
                    )
                    LisaPanel.EmergencySetup -> PlaceholderPanel(
                        title = "Emergency Setup",
                        description = "Emergency is triggered by L${EMERGENCY_LEFT_WINKS} R${EMERGENCY_RIGHT_WINKS} with no confirmation delay.",
                        onBack = onBackToMenu
                    )
                    LisaPanel.CaregiverLinking -> CaregiverLinkingPanel(
                        caregivers = caregivers,
                        activeProfileId = activeProfileId,
                        activeProfileName = profiles.find { it.id == activeProfileId }?.name ?: "Current profile",
                        onAddCaregiver = onAddCaregiver,
                        onUpdateCaregiver = onUpdateCaregiver,
                        onDeleteCaregiver = onDeleteCaregiver,
                        onBack = onBackToMenu
                    )
                    LisaPanel.Voice -> VoiceHomePanel(
                        uiStrings = uiStrings,
                        onOpenDeviceVoice = { onSelectPanel(LisaPanel.VoiceDevice) },
                        onOpenPremiumVoices = { onSelectPanel(LisaPanel.VoicePremium) },
                        onOpenMyVoice = { onSelectPanel(LisaPanel.VoiceMyVoice) },
                        onOpenFamilyVoice = { onSelectPanel(LisaPanel.VoiceFamily) },
                        onBack = onBackToMenu
                    )
                    LisaPanel.VoiceDevice -> DeviceVoicePanel(
                        uiStrings = uiStrings,
                        state = voiceSettingsState,
                        onSelectVoice = onSelectTtsVoice,
                        onTestVoice = onTestTtsVoice,
                        onInstallVoiceData = onInstallTtsVoiceData,
                        onOpenTtsSettings = onOpenTtsSettings,
                        onBack = { onSelectPanel(LisaPanel.Voice) }
                    )
                    LisaPanel.VoicePremium -> PremiumVoicesPanel(
                        uiStrings = uiStrings,
                        onBack = { onSelectPanel(LisaPanel.Voice) }
                    )
                    LisaPanel.VoiceMyVoice -> MyVoicePanel(
                        uiStrings = uiStrings,
                        onBack = { onSelectPanel(LisaPanel.Voice) }
                    )
                    LisaPanel.VoiceFamily -> FamilyVoicePanel(
                        uiStrings = uiStrings,
                        onBack = { onSelectPanel(LisaPanel.Voice) }
                    )
                    LisaPanel.Settings -> SettingsPanel(
                        settingsState = settingsState,
                        onDeveloperModeChange = onDeveloperModeChange,
                        onSensitivityDecrease = onSensitivityDecrease,
                        onSensitivityIncrease = onSensitivityIncrease,
                        onPlaceholderChange = onSettingsPlaceholderChange,
                        onBack = onBackToMenu
                    )
                    LisaPanel.DeveloperTools -> DeveloperToolsPanel(
                        developerMode = developerMode,
                        onDeveloperModeChange = onDeveloperModeChange,
                        onBack = onBackToMenu
                    )
                    LisaPanel.AboutLisa -> AboutLisaPanel(uiStrings = uiStrings, onBack = onBackToMenu)
                    LisaPanel.Feedback -> FeedbackPanel(
                        uiStrings = uiStrings,
                        savedCount = feedbackSavedCount,
                        onSaveFeedback = onSaveFeedback,
                        onBack = onBackToMenu
                    )
                    LisaPanel.TestingChecklist -> TestingChecklistPanel(
                        uiStrings = uiStrings,
                        checklist = testingChecklist,
                        onToggleItem = onToggleChecklistItem,
                        onBack = onBackToMenu
                    )
                    LisaPanel.ReleaseNotes -> ReleaseNotesPanel(uiStrings = uiStrings, onBack = onBackToMenu)
                    LisaPanel.None -> Unit
                }
            }
        }
    }
}

data class DeveloperPanelInfo(
    val leftEye: String,
    val rightEye: String,
    val leftCount: Int,
    val rightCount: Int,
    val leftFrameStreak: Int,
    val rightFrameStreak: Int,
    val closedThreshold: Float,
    val openThreshold: Float,
    val requiredFrames: Int,
    val sensitivityLevel: Int,
    val detectionState: String
)

@Composable
private fun CompactTimelineChip(uiStrings: LisaUiStrings, activeStage: CommunicationTimelineStage) {
    Text(
        text = activeStage.localizedLabel(uiStrings),
        color = LisaWhite,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(alpha = 0.35f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

@Composable
private fun CompactSensitivityControls(
    uiStrings: LisaUiStrings,
    sensitivityLevel: Int,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color.Black.copy(alpha = 0.35f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        OutlinedButton(
            onClick = onDecrease,
            enabled = sensitivityLevel > MIN_SENSITIVITY_LEVEL,
            modifier = Modifier.height(30.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = LisaWhite)
        ) { Text(uiStrings.sensitivityDecrease, fontSize = 10.sp) }
        Text(
            text = "${uiStrings.sensitivity}: $sensitivityLevel",
            color = LisaWhite,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
        OutlinedButton(
            onClick = onIncrease,
            enabled = sensitivityLevel < MAX_SENSITIVITY_LEVEL,
            modifier = Modifier.height(30.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = LisaWhite)
        ) { Text(uiStrings.sensitivityIncrease, fontSize = 10.sp) }
    }
}

@Composable
private fun SequenceProgressDots(uiStrings: LisaUiStrings, leftCount: Int, rightCount: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(alpha = 0.32f))
            .padding(horizontal = 12.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = uiStrings.leftDots(leftCount),
            color = LisaWhite,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = uiStrings.rightDots(rightCount),
            color = LisaWhite,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun IntentPreviewCard(phrase: String, compact: Boolean = false) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(if (compact) 12.dp else 16.dp))
            .background(LisaWhite.copy(alpha = 0.94f))
            .padding(
                horizontal = if (compact) 14.dp else 18.dp,
                vertical = if (compact) 10.dp else 14.dp
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "💬", fontSize = if (compact) 22.sp else 28.sp)
        Spacer(Modifier.height(4.dp))
        Text(
            text = phrase.uppercase(Locale.getDefault()),
            color = LisaBlueDark,
            fontSize = if (compact) 18.sp else 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            lineHeight = if (compact) 22.sp else 28.sp,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun EverydayCommunicationPanel(
    uiStrings: LisaUiStrings,
    userDisplay: LisaUserDisplay,
    countdownActive: Boolean,
    onEditCountdown: () -> Unit
) {
    val expanded = countdownActive || userDisplay.countdown != null
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(LisaBlue.copy(alpha = if (expanded) 0.78f else 0.62f))
            .padding(
                horizontal = if (expanded) 14.dp else 10.dp,
                vertical = if (expanded) 12.dp else 6.dp
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AnimatedContent(
            targetState = userDisplay.headline,
            transitionSpec = {
                fadeIn(animationSpec = tween(250)) togetherWith fadeOut(animationSpec = tween(200))
            },
            label = "headline"
        ) { headline ->
            Text(
                text = headline,
                color = LisaWhite,
                fontSize = if (expanded) 22.sp else 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                lineHeight = if (expanded) 26.sp else 20.sp,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (userDisplay.subtitle.isNotBlank()) {
            Spacer(Modifier.height(if (expanded) 6.dp else 2.dp))
            Text(
                text = userDisplay.subtitle,
                color = LisaWhite.copy(alpha = 0.95f),
                fontSize = if (expanded) 15.sp else 12.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                lineHeight = if (expanded) 18.sp else 15.sp,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (userDisplay.phrase != null && !userDisplay.showIntentPreview) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = "\"${userDisplay.phrase}\"",
                color = LisaWhite,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                lineHeight = 28.sp,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (userDisplay.countdown != null) {
            Spacer(Modifier.height(16.dp))
            AnimatedContent(
                targetState = userDisplay.countdown,
                transitionSpec = {
                    fadeIn(animationSpec = tween(200)) togetherWith fadeOut(animationSpec = tween(150))
                },
                label = "countdown"
            ) { count ->
                Text(
                    text = count.toString(),
                    color = LisaWhite,
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (userDisplay.showCountdownHints) {
                Spacer(Modifier.height(14.dp))
                Text(
                    text = uiStrings.leftWinkCancel,
                    color = LisaWhite.copy(alpha = 0.95f),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = uiStrings.rightWinkSpeak,
                    color = LisaWhite.copy(alpha = 0.95f),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (countdownActive) {
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onEditCountdown,
                    modifier = Modifier.height(40.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = LisaWhite,
                        containerColor = Color.White.copy(alpha = 0.15f)
                    )
                ) {
                    Text(uiStrings.editSequence, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun DeveloperPanel(info: DeveloperPanelInfo) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.72f))
            .padding(12.dp)
    ) {
        Text("Developer Mode", color = LisaWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Spacer(Modifier.height(6.dp))
        Text("LeftEye=${info.leftEye}", color = LisaWhite, fontSize = 12.sp, lineHeight = 16.sp)
        Text("RightEye=${info.rightEye}", color = LisaWhite, fontSize = 12.sp, lineHeight = 16.sp)
        Text("L=${info.leftCount}  R=${info.rightCount}", color = LisaWhite, fontSize = 12.sp, lineHeight = 16.sp)
        Text(
            "Frames: L-streak=${info.leftFrameStreak} R-streak=${info.rightFrameStreak}",
            color = LisaWhite,
            fontSize = 12.sp,
            lineHeight = 16.sp
        )
        Text(
            "Thresholds: closed=${"%.2f".format(info.closedThreshold)} open=${"%.2f".format(info.openThreshold)}",
            color = LisaWhite,
            fontSize = 12.sp,
            lineHeight = 16.sp
        )
        Text(
            "Required frames: ${info.requiredFrames}",
            color = LisaWhite,
            fontSize = 12.sp,
            lineHeight = 16.sp
        )
        Text(
            "Sensitivity: ${info.sensitivityLevel}",
            color = LisaWhite,
            fontSize = 12.sp,
            lineHeight = 16.sp
        )
        Text(
            "State: ${info.detectionState}",
            color = LisaWhite,
            fontSize = 12.sp,
            lineHeight = 16.sp
        )
    }
}

@Composable
internal fun LisaPanelShell(
    title: String,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = LisaBlueLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (title.isBlank()) Arrangement.End else Arrangement.SpaceBetween
            ) {
                if (title.isNotBlank()) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = LisaBlueDark
                    )
                }
                if (onBack != null) {
                    TextButton(onClick = onBack) {
                        Text("Back", color = LisaBlueDark, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

private data class MenuEntry(val label: String, val panel: LisaPanel)

@Composable
private fun MenuPanel(
    uiStrings: LisaUiStrings,
    canRepeat: Boolean,
    onSelectPanel: (LisaPanel) -> Unit,
    onRepeat: () -> Unit,
    onClose: () -> Unit
) {
    val entries = listOf(
        LisaPanel.MyCommunication,
        LisaPanel.CommunicationSetup,
        LisaPanel.VocabularyTraining,
        LisaPanel.EmergencySetup,
        LisaPanel.CaregiverLinking,
        LisaPanel.Voice,
        LisaPanel.Settings,
        LisaPanel.TestingChecklist,
        LisaPanel.Feedback,
        LisaPanel.ReleaseNotes,
        LisaPanel.DeveloperTools,
        LisaPanel.AboutLisa
    )

    LisaPanelShell(title = uiStrings.menu, onBack = onClose) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 360.dp)
                .verticalScroll(rememberScrollState())
        ) {
        entries.forEach { panel ->
            MenuRow(label = uiStrings.menuLabel(panel), onClick = { onSelectPanel(panel) })
            Spacer(Modifier.height(4.dp))
        }
        if (canRepeat) {
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = LisaBlue.copy(alpha = 0.25f))
            Spacer(Modifier.height(8.dp))
            MenuRow(label = uiStrings.repeatLastPhrase, onClick = onRepeat)
        }
        }
    }
}

@Composable
private fun MenuRow(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(LisaWhite)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = LisaBlueDark
        )
        Text(
            text = "›",
            fontSize = 20.sp,
            color = LisaGray
        )
    }
}

@Composable
private fun MyCommunicationPanel(
    uiStrings: LisaUiStrings,
    profiles: List<LisaUserProfile>,
    activeProfileId: String,
    onCreateProfile: () -> Unit,
    onSelectProfile: (String) -> Unit,
    onUpdateProfile: (LisaUserProfile) -> Unit,
    onDeleteProfile: (String) -> Unit,
    onBack: () -> Unit
) {
    val activeProfile = profiles.find { it.id == activeProfileId } ?: profiles.firstOrNull()
    var editingName by remember(activeProfileId) { mutableStateOf(activeProfile?.name ?: "") }

    LaunchedEffect(activeProfile?.name) {
        editingName = activeProfile?.name ?: ""
    }

    LisaPanelShell(title = uiStrings.myCommunication, onBack = onBack) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 380.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (activeProfile != null) {
                SettingsSectionLabel("Active profile")
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(LisaWhite)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = activeProfile.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = LisaBlueDark
                    )
                    Text(
                        text = "${activeProfile.preferredLanguage.label} · ${activeProfile.communicationLevel.label}",
                        fontSize = 12.sp,
                        color = LisaBlueDark.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "Sensitivity ${activeProfile.sensitivityLevel} · Text ${(activeProfile.textSizeScale * 100).toInt()}%",
                        fontSize = 12.sp,
                        color = LisaBlueDark.copy(alpha = 0.7f)
                    )
                }

                SettingsSectionLabel("Profile name")
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = editingName,
                    onValueChange = { editingName = it },
                    label = { Text("Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        if (editingName != activeProfile.name && editingName.isNotBlank()) {
                            TextButton(onClick = {
                                onUpdateProfile(activeProfile.copy(name = editingName.trim()))
                            }) {
                                Text("Save", fontSize = 12.sp)
                            }
                        }
                    }
                )

                SettingsSectionLabel("Preferred language")
                ProfileOptionGroup(
                    options = PreferredLanguage.selectable.map { it.label },
                    selected = activeProfile.preferredLanguage.label,
                    onSelect = { label ->
                        val language = PreferredLanguage.fromStored(label)
                        onUpdateProfile(activeProfile.copy(preferredLanguage = language))
                    }
                )

                SettingsSectionLabel("Communication level")
                ProfileOptionGroup(
                    options = CommunicationLevel.entries.map { it.label },
                    selected = activeProfile.communicationLevel.label,
                    onSelect = { label ->
                        val level = CommunicationLevel.fromStored(label)
                        onUpdateProfile(activeProfile.withCommunicationLevel(level))
                    }
                )
            }

            SettingsSectionLabel("Saved profiles")
            profiles.forEach { profile ->
                val isActive = profile.id == activeProfileId
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isActive) LisaBlueLight.copy(alpha = 0.35f) else LisaWhite)
                        .clickable { if (!isActive) onSelectProfile(profile.id) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = profile.name,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 14.sp,
                            color = LisaBlueDark
                        )
                        Text(
                            text = profile.communicationLevel.label,
                            fontSize = 11.sp,
                            color = LisaBlueDark.copy(alpha = 0.65f)
                        )
                    }
                    if (isActive) {
                        Text(
                            text = "Active",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = LisaBlue
                        )
                    } else {
                        Text(text = "›", fontSize = 18.sp, color = LisaGray)
                    }
                }
            }

            OutlinedButton(
                onClick = onCreateProfile,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Create new profile")
            }

            if (profiles.size > 1 && activeProfile != null) {
                OutlinedButton(
                    onClick = { onDeleteProfile(activeProfile.id) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = LisaEmergencyRed)
                ) {
                    Text("Delete active profile")
                }
            }
        }
    }
}

@Composable
private fun ProfileOptionGroup(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        options.forEach { option ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (option == selected) LisaBlueLight.copy(alpha = 0.35f) else LisaWhite)
                    .clickable { onSelect(option) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = option,
                    fontWeight = if (option == selected) FontWeight.SemiBold else FontWeight.Medium,
                    fontSize = 14.sp,
                    color = LisaBlueDark
                )
                if (option == selected) {
                    Text(text = "✓", fontSize = 14.sp, color = LisaBlue, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun PlaceholderPanel(
    title: String,
    description: String,
    onBack: () -> Unit
) {
    LisaPanelShell(title = title, onBack = onBack) {
        Text(
            text = description,
            fontSize = 14.sp,
            color = LisaBlueDark.copy(alpha = 0.85f),
            lineHeight = 20.sp
        )
    }
}

@Composable
private fun DeveloperToolsPanel(
    developerMode: Boolean,
    onDeveloperModeChange: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    LisaPanelShell(title = "Developer Tools", onBack = onBack) {
        Text(
            text = "Enable developer mode to show calibration and debug data on the main screen.",
            fontSize = 14.sp,
            color = LisaBlueDark.copy(alpha = 0.85f),
            lineHeight = 20.sp
        )
        Spacer(Modifier.height(12.dp))
        SettingsToggleRow(
            title = "Developer Mode",
            subtitle = "Show live detection data overlay",
            checked = developerMode,
            onCheckedChange = onDeveloperModeChange
        )
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(LisaWhite)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = LisaBlueDark)
            Text(subtitle, fontSize = 12.sp, color = LisaBlueDark.copy(alpha = 0.7f))
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

@Composable
private fun SettingsPanel(
    settingsState: LisaSettingsUiState,
    onDeveloperModeChange: (Boolean) -> Unit,
    onSensitivityDecrease: () -> Unit,
    onSensitivityIncrease: () -> Unit,
    onPlaceholderChange: (LisaSettingsUiState) -> Unit,
    onBack: () -> Unit
) {
    LisaPanelShell(title = "Settings", onBack = onBack) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 360.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SettingsSectionLabel("Detection")
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
                    Text("Sensitivity", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = LisaBlueDark)
                    Text("Level ${settingsState.sensitivityLevel}", fontSize = 12.sp, color = LisaBlueDark.copy(alpha = 0.7f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = onSensitivityDecrease,
                        enabled = settingsState.sensitivityLevel > MIN_SENSITIVITY_LEVEL,
                        modifier = Modifier.height(34.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp)
                    ) { Text("−", fontSize = 16.sp) }
                    OutlinedButton(
                        onClick = onSensitivityIncrease,
                        enabled = settingsState.sensitivityLevel < MAX_SENSITIVITY_LEVEL,
                        modifier = Modifier.height(34.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp)
                    ) { Text("+", fontSize = 16.sp) }
                }
            }

            SettingsToggleRow(
                title = "Calibration",
                subtitle = "Fine-tune eye-open thresholds (developer preview)",
                checked = settingsState.calibrationEnabled,
                onCheckedChange = { onPlaceholderChange(settingsState.copy(calibrationEnabled = it)) }
            )

            SettingsSectionLabel("Display")
            SettingsSliderRow(
                title = "Text size",
                valueLabel = "${(settingsState.textSizeScale * 100).toInt()}%",
                value = settingsState.textSizeScale,
                valueRange = 0.8f..1.4f,
                onValueChange = { onPlaceholderChange(settingsState.copy(textSizeScale = it)) }
            )

            SettingsSectionLabel("Communication")
            SettingsSliderRow(
                title = "Confirmation countdown",
                valueLabel = "${settingsState.countdownDurationSec} sec",
                value = settingsState.countdownDurationSec.toFloat(),
                valueRange = 2f..5f,
                steps = 2,
                onValueChange = {
                    onPlaceholderChange(settingsState.copy(countdownDurationSec = it.toInt()))
                }
            )
            SettingsSliderRow(
                title = "Sequence timeout",
                valueLabel = "${"%.1f".format(settingsState.sequenceIdleTimeoutSec)} sec",
                value = settingsState.sequenceIdleTimeoutSec,
                valueRange = 1.5f..4f,
                onValueChange = { onPlaceholderChange(settingsState.copy(sequenceIdleTimeoutSec = it)) }
            )

            SettingsSectionLabel("Emergency")
            SettingsSliderRow(
                title = "Emergency alarm volume",
                valueLabel = "${(settingsState.emergencyAlarmVolume * 100).toInt()}%",
                value = settingsState.emergencyAlarmVolume,
                valueRange = 0.5f..1f,
                onValueChange = { onPlaceholderChange(settingsState.copy(emergencyAlarmVolume = it)) }
            )

            SettingsSectionLabel("Advanced")
            SettingsToggleRow(
                title = "Developer Mode",
                subtitle = "Show calibration and debug data",
                checked = settingsState.developerMode,
                onCheckedChange = onDeveloperModeChange
            )

            SettingsSectionLabel("Data")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(LisaWhite)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Profile backup / export", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = LisaBlueDark)
                    Text("Save vocabulary and settings", fontSize = 12.sp, color = LisaBlueDark.copy(alpha = 0.7f))
                }
                OutlinedButton(onClick = { }, enabled = false) {
                    Text("Export", fontSize = 12.sp)
                }
            }

            Text(
                text = "Settings are saved to the active communication profile.",
                fontSize = 11.sp,
                color = LisaGray,
                lineHeight = 15.sp
            )
        }
    }
}

@Composable
private fun SettingsSectionLabel(text: String) {
    Text(
        text = text.uppercase(Locale.getDefault()),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = LisaBlueDark.copy(alpha = 0.55f),
        letterSpacing = 0.5.sp
    )
}

@Composable
private fun SettingsSliderRow(
    title: String,
    valueLabel: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    onValueChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(LisaWhite)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = LisaBlueDark)
            Text(valueLabel, fontSize = 12.sp, color = LisaBlueDark.copy(alpha = 0.7f))
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = LisaBlue,
                activeTrackColor = LisaBlue,
                inactiveTrackColor = LisaSoftGray
            )
        )
    }
}

@Composable
private fun VocabularyTrainingPanel(
    uiStrings: LisaUiStrings,
    preferredLanguage: PreferredLanguage,
    mappings: List<WinkMapping>,
    onAddMapping: (left: Int, right: Int, phrase: String) -> Unit,
    onBack: () -> Unit
) {
    var leftTxt by remember { mutableStateOf("2") }
    var rightTxt by remember { mutableStateOf("0") }
    var phraseTxt by remember { mutableStateOf("") }

    LisaPanelShell(title = uiStrings.vocabularyTraining, onBack = onBack) {
        Text(uiStrings.minSequenceNote, fontSize = 13.sp, color = LisaBlueDark)
        Text(
            uiStrings.countdownNote,
            fontSize = 13.sp,
            color = LisaBlueDark.copy(alpha = 0.85f),
            lineHeight = 18.sp
        )
        Spacer(Modifier.height(10.dp))

        val coreMappings = mappings.filter { !it.isCustom }
        val customMappings = mappings.filter { it.isCustom }

        Text(
            uiStrings.coreVocabulary(coreMappings.size),
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = LisaBlueDark
        )
        Spacer(Modifier.height(6.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 280.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(LisaWhite)
                .padding(10.dp)
        ) {
            items(coreMappings) { m ->
                val localized = m.localizedPhrase(preferredLanguage)
                val englishSub = uiStrings.phraseEnglishSubtitle(m.vocabularyId)
                Column {
                    Text(
                        text = "L${m.left} R${m.right} → $localized",
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        color = LisaBlueDark,
                        fontWeight = FontWeight.Medium
                    )
                    if (englishSub != null) {
                        Text(
                            text = englishSub,
                            fontSize = 10.sp,
                            lineHeight = 14.sp,
                            color = LisaGray
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
        }

        if (customMappings.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Text(uiStrings.customPhrases, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = LisaBlueDark)
            Spacer(Modifier.height(6.dp))
            customMappings.forEach { m ->
                Text(
                    text = "L${m.left} R${m.right} → ${m.localizedPhrase(preferredLanguage)}",
                    fontSize = 12.sp,
                    color = LisaBlueDark
                )
                Spacer(Modifier.height(3.dp))
            }
        }

        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = LisaBlue.copy(alpha = 0.25f))
        Spacer(Modifier.height(12.dp))

        Text(uiStrings.addCustomSequence, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = LisaBlueDark)
        Spacer(Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                modifier = Modifier.weight(1f),
                value = leftTxt,
                onValueChange = { leftTxt = it.filter { ch -> ch.isDigit() } },
                label = { Text(uiStrings.leftLabel) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            OutlinedTextField(
                modifier = Modifier.weight(1f),
                value = rightTxt,
                onValueChange = { rightTxt = it.filter { ch -> ch.isDigit() } },
                label = { Text(uiStrings.rightLabel) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
        }

        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = phraseTxt,
            onValueChange = { phraseTxt = it },
            label = { Text("Phrase") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(Modifier.height(10.dp))
        Button(
            onClick = {
                val l = leftTxt.toIntOrNull() ?: 0
                val r = rightTxt.toIntOrNull() ?: 0
                if (!isSequenceEligibleForSpeech(l, r)) return@Button
                onAddMapping(l, r, phraseTxt)
                phraseTxt = ""
            },
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = LisaBlue)
        ) {
            Text("Save sequence")
        }
    }
}

@Composable
private fun LisaActionButton(
    text: String,
    modifier: Modifier = Modifier,
    filled: Boolean,
    danger: Boolean = false,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    if (filled) {
        Button(
            onClick = onClick,
            modifier = modifier.height(44.dp),
            shape = shape,
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
            colors = ButtonDefaults.buttonColors(containerColor = LisaBlue)
        ) {
            Text(text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier.height(44.dp),
            shape = shape,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = if (danger) LisaEmergencyRed else LisaBlueDark,
                containerColor = LisaWhite.copy(alpha = 0.92f)
            )
        ) {
            Text(text, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun EmergencyOverlay(uiStrings: LisaUiStrings, notifyNames: List<String> = emptyList()) {
    val infiniteTransition = rememberInfiniteTransition(label = "emergency_flash")
    val flashAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.88f,
        animationSpec = infiniteRepeatable(animation = tween(600), repeatMode = RepeatMode.Reverse),
        label = "emergency_alpha"
    )

    Box(
        modifier = Modifier
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
            Spacer(Modifier.height(10.dp))
            Text(
                text = uiStrings.callingForHelp,
                color = LisaWhite,
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
            if (notifyNames.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "${uiStrings.wouldNotify} ${notifyNames.joinToString(", ")}",
                    color = LisaWhite.copy(alpha = 0.95f),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        }
    }
}
