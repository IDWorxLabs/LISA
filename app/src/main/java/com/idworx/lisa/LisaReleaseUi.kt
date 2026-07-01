package com.idworx.lisa

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.idworx.lisa.ui.theme.LisaBlue
import com.idworx.lisa.ui.theme.LisaBlueDark
import com.idworx.lisa.ui.theme.LisaBlueLight
import com.idworx.lisa.ui.theme.LisaGray
import com.idworx.lisa.ui.theme.LisaWhite
import java.util.Locale

@Composable
fun OnboardingFlow(
    primaryUserName: String,
    onPrimaryUserNameChange: (String) -> Unit,
    onRequestCameraPermission: () -> Unit,
    onComplete: () -> Unit
) {
    var step by remember { mutableIntStateOf(0) }
    var editingName by remember(primaryUserName) { mutableStateOf(primaryUserName) }

    LaunchedEffect(primaryUserName) {
        editingName = primaryUserName
    }

    val totalSteps = 6
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LisaBlueLight)
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = LisaWhite)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Step ${step + 1} of $totalSteps",
                    fontSize = 12.sp,
                    color = LisaGray,
                    fontWeight = FontWeight.Medium
                )
                LinearProgressIndicator(
                    progress = { (step + 1).toFloat() / totalSteps },
                    modifier = Modifier.fillMaxWidth(),
                    color = LisaBlue
                )

                when (step) {
                    0 -> OnboardingBlock(
                        title = "Welcome to LISA",
                        body = "LISA — Look Into Speaking Assistant — helps you communicate using intentional eye movements. This setup takes about one minute."
                    )
                    1 -> OnboardingBlock(
                        title = "What LISA does",
                        body = "LISA uses your front camera to detect deliberate wink sequences, matches them to phrases, asks you to confirm, and speaks your message aloud. Emergency sequences can trigger a local alarm."
                    )
                    2 -> OnboardingBlock(
                        title = "Camera permission",
                        body = "LISA needs camera access to see your face and detect intentional winks. Video is processed on your device only — nothing is uploaded during this testing build.",
                        extra = {
                            OutlinedButton(
                                onClick = onRequestCameraPermission,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Allow camera access")
                            }
                        }
                    )
                    3 -> OnboardingBlock(
                        title = "Safety notice",
                        bullets = listOf(
                            "LISA is an assistive communication tool, not a certified medical device.",
                            "Test with supervision before relying on it.",
                            "Do not use LISA as your only emergency system yet.",
                            "Practice emergency and reset with a caregiver present."
                        )
                    )
                    4 -> {
                        Text(
                            text = "Primary User profile",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = LisaBlueDark
                        )
                        Text(
                            text = "Confirm the name for the main communication profile on this device.",
                            fontSize = 14.sp,
                            color = LisaBlueDark.copy(alpha = 0.85f),
                            lineHeight = 20.sp
                        )
                        OutlinedTextField(
                            value = editingName,
                            onValueChange = { editingName = it },
                            label = { Text("Profile name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                    5 -> OnboardingBlock(
                        title = "Start LISA",
                        body = "You're ready for local testing. Use Menu → Testing Checklist to verify winks, speech, and emergency before real-world use.",
                        extra = {
                            Text(
                                text = "Primary User: $editingName",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = LisaBlueDark
                            )
                        }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (step > 0) {
                        OutlinedButton(
                            onClick = { step -= 1 },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) { Text("Back") }
                    }
                    Button(
                        onClick = {
                            if (step == 4) {
                                onPrimaryUserNameChange(editingName.trim().ifBlank { "Primary User" })
                            }
                            if (step < totalSteps - 1) {
                                step += 1
                            } else {
                                onRequestCameraPermission()
                                onComplete()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LisaBlue)
                    ) {
                        Text(if (step == totalSteps - 1) "Start LISA" else "Next")
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingBlock(
    title: String,
    body: String? = null,
    bullets: List<String>? = null,
    extra: @Composable (() -> Unit)? = null
) {
    Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = LisaBlueDark)
    body?.let {
        Text(it, fontSize = 14.sp, color = LisaBlueDark.copy(alpha = 0.88f), lineHeight = 20.sp)
    }
    bullets?.forEach { item ->
        Text(
            text = "• $item",
            fontSize = 14.sp,
            color = LisaBlueDark.copy(alpha = 0.88f),
            lineHeight = 20.sp
        )
    }
    extra?.invoke()
}

@Composable
fun CameraPermissionScreen(
    permanentlyDenied: Boolean,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LisaBlueLight)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(LisaWhite)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Camera access needed",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = LisaBlueDark,
                textAlign = TextAlign.Center
            )
            Text(
                text = "LISA uses the front camera to detect your face and intentional wink sequences. Without camera access, LISA cannot listen for your messages.",
                fontSize = 14.sp,
                color = LisaBlueDark.copy(alpha = 0.85f),
                lineHeight = 20.sp,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Processing stays on this device. No video is uploaded in this testing build.",
                fontSize = 13.sp,
                color = LisaGray,
                lineHeight = 18.sp,
                textAlign = TextAlign.Center
            )
            if (permanentlyDenied) {
                Button(
                    onClick = onOpenSettings,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = LisaBlue)
                ) {
                    Text("Open Settings")
                }
                Text(
                    text = "Camera permission was denied. Open Settings → LISA → Permissions to enable the camera.",
                    fontSize = 12.sp,
                    color = LisaGray,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )
            } else {
                Button(
                    onClick = onRequestPermission,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = LisaBlue)
                ) {
                    Text("Grant camera permission")
                }
            }
        }
    }
}

@Composable
fun FeedbackPanel(
    savedCount: Int,
    onSaveFeedback: (
        whatWorkedWell: String,
        whatWasConfusing: String,
        winkDetectionFeedback: String,
        speechTimingFeedback: String
    ) -> Unit,
    onBack: () -> Unit
) {
    var workedWell by remember { mutableStateOf("") }
    var confusing by remember { mutableStateOf("") }
    var winkDetection by remember { mutableStateOf("") }
    var speechTiming by remember { mutableStateOf("") }

    LisaPanelShell(title = "Feedback", onBack = onBack) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 420.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Help improve LISA during local testing. Responses are saved on this device only.",
                fontSize = 13.sp,
                color = LisaBlueDark.copy(alpha = 0.8f),
                lineHeight = 18.sp
            )
            if (savedCount > 0) {
                Text(
                    text = "$savedCount response${if (savedCount == 1) "" else "s"} saved locally",
                    fontSize = 12.sp,
                    color = LisaGray
                )
            }
            FeedbackField("What worked well?", workedWell) { workedWell = it }
            FeedbackField("What was confusing?", confusing) { confusing = it }
            FeedbackField("Did LISA detect your winks correctly?", winkDetection) { winkDetection = it }
            FeedbackField("Did speech happen at the right time?", speechTiming) { speechTiming = it }
            Button(
                onClick = {
                    onSaveFeedback(workedWell, confusing, winkDetection, speechTiming)
                    workedWell = ""
                    confusing = ""
                    winkDetection = ""
                    speechTiming = ""
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = LisaBlue),
                enabled = workedWell.isNotBlank() || confusing.isNotBlank() ||
                    winkDetection.isNotBlank() || speechTiming.isNotBlank()
            ) {
                Text("Save feedback")
            }
        }
    }
}

@Composable
private fun FeedbackField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 13.sp) },
        modifier = Modifier.fillMaxWidth(),
        minLines = 2,
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
fun TestingChecklistPanel(
    checklist: Map<String, Boolean>,
    onToggleItem: (String, Boolean) -> Unit,
    onBack: () -> Unit
) {
    LisaPanelShell(title = "Testing Checklist", onBack = onBack) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 420.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Complete before sharing with family and friends. Check each item as you verify it.",
                fontSize = 13.sp,
                color = LisaBlueDark.copy(alpha = 0.8f),
                lineHeight = 18.sp
            )
            TestingChecklistItem.entries.forEach { item ->
                val checked = checklist[item.key] == true
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(LisaWhite)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = checked,
                        onCheckedChange = { onToggleItem(item.key, it) }
                    )
                    Text(
                        text = item.label,
                        fontSize = 14.sp,
                        color = LisaBlueDark,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun ReleaseNotesPanel(onBack: () -> Unit) {
    LisaPanelShell(title = "Release Notes", onBack = onBack) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 420.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "LISA 1.1 Local Testing Build",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = LisaBlueDark
            )
            ReleaseNotesSection(
                bullets = listOf(
                    "Camera-first communication",
                    "Wink sequence language",
                    "Confirmation before speech",
                    "Emergency alarm",
                    "Local profiles",
                    "Local caregiver linking",
                    "About LISA",
                    "Testing checklist"
                )
            )
            Text(
                text = "Version: 1.1 local testing build",
                fontSize = 12.sp,
                color = LisaGray
            )
        }
    }
}

@Composable
private fun ReleaseNotesSection(bullets: List<String>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(LisaWhite)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        bullets.forEach { item ->
            Text(
                text = "• $item",
                fontSize = 14.sp,
                color = LisaBlueDark.copy(alpha = 0.88f),
                lineHeight = 20.sp
            )
        }
    }
}
