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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.idworx.lisa.ui.theme.LisaBlue
import java.util.Locale
import com.idworx.lisa.ui.theme.LisaBlueDark
import com.idworx.lisa.ui.theme.LisaEmergencyRed
import com.idworx.lisa.ui.theme.LisaSoftGray
import com.idworx.lisa.ui.theme.LisaWhite

@Composable
fun     LisaRootUI(
    userDisplay: LisaUserDisplay,
    emergencyActive: Boolean,
    developerMode: Boolean,
    showSettings: Boolean,
    showTraining: Boolean,
    countdownActive: Boolean,
    sensitivityLevel: Int,
    developerInfo: DeveloperPanelInfo,
    mappings: List<WinkMapping>,
    onToggleSettings: () -> Unit,
    onDeveloperModeChange: (Boolean) -> Unit,
    onSensitivityDecrease: () -> Unit,
    onSensitivityIncrease: () -> Unit,
    onToggleTraining: () -> Unit,
    onRepeat: () -> Unit,
    onReset: () -> Unit,
    onEditCountdown: () -> Unit,
    onAddMapping: (left: Int, right: Int, phrase: String) -> Unit,
    cameraView: @Composable () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        cameraView()

        if (emergencyActive) {
            EmergencyOverlay()
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CompactTimelineChip(activeStage = userDisplay.timelineStage)

            Spacer(Modifier.height(4.dp))

            if (userDisplay.showIntentPreview && userDisplay.phrase != null) {
                IntentPreviewCard(phrase = userDisplay.phrase, compact = !countdownActive)
                Spacer(Modifier.height(4.dp))
            }

            EverydayCommunicationPanel(
                userDisplay = userDisplay,
                countdownActive = countdownActive,
                onEditCountdown = onEditCountdown
            )

            Spacer(Modifier.height(4.dp))
            CompactSensitivityControls(
                sensitivityLevel = sensitivityLevel,
                onDecrease = onSensitivityDecrease,
                onIncrease = onSensitivityIncrease
            )

            if (!developerMode && (userDisplay.leftWinkDots > 0 || userDisplay.rightWinkDots > 0)) {
                Spacer(Modifier.height(4.dp))
                SequenceProgressDots(
                    leftCount = userDisplay.leftWinkDots,
                    rightCount = userDisplay.rightWinkDots
                )
            }

            if (developerMode) {
                Spacer(Modifier.height(4.dp))
                DeveloperPanel(info = developerInfo)
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
                    text = "Repeat",
                    modifier = Modifier.weight(1f),
                    filled = false,
                    onClick = onRepeat
                )
                LisaActionButton(
                    text = if (showTraining) "Back" else "Training",
                    modifier = Modifier.weight(1f),
                    filled = false,
                    onClick = onToggleTraining
                )
                LisaActionButton(
                    text = "Reset",
                    modifier = Modifier.weight(1f),
                    filled = false,
                    danger = emergencyActive,
                    onClick = onReset
                )
                LisaActionButton(
                    text = "Settings",
                    modifier = Modifier.weight(1f),
                    filled = false,
                    onClick = onToggleSettings
                )
            }

            if (showSettings) {
                Spacer(Modifier.height(10.dp))
                SettingsPanel(
                    developerMode = developerMode,
                    onDeveloperModeChange = onDeveloperModeChange
                )
            }

            if (showTraining) {
                Spacer(Modifier.height(10.dp))
                TrainingPanel(mappings = mappings, onAddMapping = onAddMapping)
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
private fun CompactTimelineChip(activeStage: CommunicationTimelineStage) {
    Text(
        text = activeStage.label,
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
        ) { Text("Sensitivity -", fontSize = 10.sp) }
        Text(
            text = "Sensitivity: $sensitivityLevel",
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
        ) { Text("Sensitivity +", fontSize = 10.sp) }
    }
}

@Composable
private fun SequenceProgressDots(leftCount: Int, rightCount: Int) {
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
            text = "Left: ${winkDots(leftCount)}",
            color = LisaWhite,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = "Right: ${winkDots(rightCount)}",
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
                    text = "Left wink = Cancel",
                    color = LisaWhite.copy(alpha = 0.95f),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Right wink = Speak now",
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
                    Text("Edit", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
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
private fun SettingsPanel(
    developerMode: Boolean,
    onDeveloperModeChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = LisaWhite)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Settings", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = LisaBlueDark)
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Developer Mode", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text(
                        "Show calibration and debug data",
                        fontSize = 12.sp,
                        color = LisaBlueDark.copy(alpha = 0.7f)
                    )
                }
                Switch(
                    checked = developerMode,
                    onCheckedChange = onDeveloperModeChange
                )
            }
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
private fun EmergencyOverlay() {
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
                text = "EMERGENCY",
                color = LisaWhite,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Calling for help...",
                color = LisaWhite,
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun TrainingPanel(
    mappings: List<WinkMapping>,
    onAddMapping: (left: Int, right: Int, phrase: String) -> Unit
) {
    var leftTxt by remember { mutableStateOf("2") }
    var rightTxt by remember { mutableStateOf("0") }
    var phraseTxt by remember { mutableStateOf("") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = LisaWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Training", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = LisaBlueDark)
            Spacer(Modifier.height(8.dp))
            Text("Single winks are ignored.", fontSize = 13.sp, color = LisaBlueDark)
            Text("Natural blinks are ignored.", fontSize = 13.sp, color = LisaBlueDark)
            Text("Only completed sequences are translated.", fontSize = 13.sp, color = LisaBlueDark)
            Text(
                "Minimum sequence: $MIN_SEQUENCE_WINKS winks",
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = LisaBlueDark
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "After a phrase is detected, a short countdown lets you cancel or speak.",
                fontSize = 13.sp,
                color = LisaBlueDark.copy(alpha = 0.85f)
            )
            Spacer(Modifier.height(12.dp))

            val coreMappings = mappings.filter { !it.isCustom }
            val customMappings = mappings.filter { it.isCustom }

            Text("Core vocabulary (${coreMappings.size}):", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Spacer(Modifier.height(6.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 280.dp)
            ) {
                items(coreMappings) { m ->
                    Text(
                        text = "L${m.left} R${m.right} → ${m.phrase}",
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        color = LisaBlueDark
                    )
                    Spacer(Modifier.height(3.dp))
                }
            }

            if (customMappings.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Text("Custom phrases:", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(Modifier.height(6.dp))
                customMappings.forEach { m ->
                    Text(
                        text = "L${m.left} R${m.right} → ${m.phrase}",
                        fontSize = 12.sp,
                        color = LisaBlueDark
                    )
                    Spacer(Modifier.height(3.dp))
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = LisaSoftGray)
            Spacer(Modifier.height(12.dp))

            Text("Add a new sequence:", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    modifier = Modifier.weight(1f),
                    value = leftTxt,
                    onValueChange = { leftTxt = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Left winks") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    modifier = Modifier.weight(1f),
                    value = rightTxt,
                    onValueChange = { rightTxt = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Right winks") },
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
}
