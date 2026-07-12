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

@Composable
fun OnboardingFlow(
    uiStrings: LisaUiStrings,
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
                    text = uiStrings.stepOf(step + 1, totalSteps),
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
                        title = uiStrings.welcomeToLisa,
                        body = uiStrings.onboardingWelcomeBody
                    )
                    1 -> OnboardingBlock(
                        title = uiStrings.whatLisaDoes,
                        body = uiStrings.onboardingWhatBody
                    )
                    2 -> OnboardingBlock(
                        title = uiStrings.cameraPermissionTitle,
                        body = uiStrings.onboardingCameraBody,
                        extra = {
                            OutlinedButton(
                                onClick = onRequestCameraPermission,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(uiStrings.allowCameraAccess)
                            }
                        }
                    )
                    3 -> OnboardingBlock(
                        title = uiStrings.onboardingSafetyTitle,
                        bullets = listOf(
                            uiStrings.t("LISA is an assistive communication tool, not a certified medical device.", "LISA is 'n hulpkommunikasiehulpmiddel, nie 'n gesertifiseerde mediese toestel nie.", "I-LISA iyithuluzi lokuxhumana olusizayo, hhayi idivayisi yezokwelapha eqinisekisiwe."),
                            uiStrings.t("Test with supervision before relying on it.", "Toets met toesig voordat jy daarop staatmaak.", "Hlola ngokuqapha ngaphambi kokuthembela kuyo."),
                            uiStrings.t("Do not use LISA as your only emergency system yet.", "Moenie LISA nog as jou enigste noodstelsel gebruik nie.", "Ungasebenzisi i-LISA njengohlelo lwakho oluphuthumayo kuphela okwamanje."),
                            uiStrings.t("Practice emergency and reset with a caregiver present.", "Oefen nood en herstel met 'n versorger teenwoordig.", "Zilolonge usizo oluphuthumayo nokusetha kabusha nomnakekeli okhona.")
                        )
                    )
                    4 -> {
                        Text(
                            text = uiStrings.primaryUserProfile,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = LisaBlueDark
                        )
                        Text(
                            text = uiStrings.confirmProfileName,
                            fontSize = 14.sp,
                            color = LisaBlueDark.copy(alpha = 0.85f),
                            lineHeight = 20.sp
                        )
                        OutlinedTextField(
                            value = editingName,
                            onValueChange = { editingName = it },
                            label = { Text(uiStrings.profileName) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                    5 -> OnboardingBlock(
                        title = uiStrings.startLisaTitle,
                        body = uiStrings.onboardingStartBody,
                        extra = {
                            Text(
                                text = uiStrings.primaryUserLabel(editingName),
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
                        ) { Text(uiStrings.back) }
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
                        Text(if (step == totalSteps - 1) uiStrings.startLisa else uiStrings.next)
                    }
                }
            }
        }
    }
}

/** Exposed for onboarding safety bullets. */
internal fun LisaUiStrings.t(en: String, af: String, zu: String): String = when (language) {
    PreferredLanguage.English -> en
    PreferredLanguage.Afrikaans -> af
    PreferredLanguage.IsiZulu -> zu
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
    uiStrings: LisaUiStrings,
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
                text = uiStrings.cameraAccessNeeded,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = LisaBlueDark,
                textAlign = TextAlign.Center
            )
            Text(
                text = uiStrings.cameraPermissionExplain,
                fontSize = 14.sp,
                color = LisaBlueDark.copy(alpha = 0.85f),
                lineHeight = 20.sp,
                textAlign = TextAlign.Center
            )
            Text(
                text = uiStrings.cameraOnDeviceOnly,
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
                    Text(uiStrings.openSettings)
                }
                Text(
                    text = uiStrings.cameraDeniedSettingsHint,
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
                    Text(uiStrings.grantCameraPermission)
                }
            }
        }
    }
}

@Composable
fun FeedbackPanel(
    uiStrings: LisaUiStrings,
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

    val q1 = uiStrings.t("What worked well?", "Wat het goed gewerk?", "Yini eyasebenza kahle?")
    val q2 = uiStrings.t("What was confusing?", "Wat was verwarrend?", "Yini eyedidezelayo?")
    val q3 = uiStrings.t("Did LISA detect your winks correctly?", "Het LISA jou knippe korrek opgespoor?", "Ingabe i-LISA ithole ama-wink akho ngokulungile?")
    val q4 = uiStrings.t("Did speech happen at the right time?", "Het spraak op die regte tyd plaasgevind?", "Ingabe inkulumo yenzekile ngesikhathi esifanele?")

    LisaPanelShell(title = uiStrings.feedback, onBack = onBack, backLabel = uiStrings.back) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 420.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PanelPurposeLine(uiStrings.feedbackPurpose)
            Text(
                text = uiStrings.feedbackIntro,
                fontSize = 11.sp,
                color = LisaGray,
                lineHeight = 15.sp
            )
            FeedbackField(q1, workedWell) { workedWell = it }
            FeedbackField(q2, confusing) { confusing = it }
            FeedbackField(q3, winkDetection) { winkDetection = it }
            FeedbackField(q4, speechTiming) { speechTiming = it }
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
                Text(uiStrings.saveFeedback)
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
    uiStrings: LisaUiStrings,
    checklist: Map<String, Boolean>,
    onToggleItem: (String, Boolean) -> Unit,
    onBack: () -> Unit
) {
    LisaPanelShell(title = uiStrings.testingChecklist, onBack = onBack, backLabel = uiStrings.back) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 420.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PanelPurposeLine(uiStrings.deviceChecklistPurpose)
            Text(
                text = uiStrings.testingChecklistIntro,
                fontSize = 11.sp,
                color = LisaGray,
                lineHeight = 15.sp,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            TestingChecklistItem.entries.forEach { item ->
                val checked = checklist[item.key] == true
                val label = uiStrings.testingChecklistLabel(item)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(LisaWhite)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = checked, onCheckedChange = { onToggleItem(item.key, it) })
                    Text(text = label, fontSize = 14.sp, color = LisaBlueDark, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

private fun LisaUiStrings.testingChecklistLabel(item: TestingChecklistItem): String = when (item) {
    TestingChecklistItem.CameraOpens -> t("Camera opens", "Kamera maak oop", "Ikhamera ivula")
    TestingChecklistItem.FaceDetected -> t("Face detected", "Gesig opgespoor", "Ubuso batholakele")
    TestingChecklistItem.LeftWinkDetected -> t("Left wink detected correctly", "Linkerknik korrek opgespoor", "Ukucwayiza kwesokunxele kutholwe kahle")
    TestingChecklistItem.RightWinkDetected -> t("Right wink detected correctly", "Regterknik korrek opgespoor", "Ukucwayiza kwesokudla kutholwe kahle")
    TestingChecklistItem.YesSequenceWorks -> t("Yes sequence works", "Ja-reeks werk", "Uchungechunge lwe-Yebo luyasebenza")
    TestingChecklistItem.NoSequenceWorks -> t("No sequence works", "Nee-reeks werk", "Uchungechunge lwe-Cha luyasebenza")
    TestingChecklistItem.EmergencyAlarmTested -> t("Emergency alarm tested", "Noodalarm getoets", "I-alamu yosizo oluphuthumayo ihloliwe")
    TestingChecklistItem.ResetStopsAlarm -> clearStopsAlarm
    TestingChecklistItem.CaregiverKnowsTestBuild -> caregiverUnderstandsSetup
}

@Composable
fun ReleaseNotesPanel(
    uiStrings: LisaUiStrings,
    appVersionInfo: LisaAppVersionInfo,
    onBack: () -> Unit
) {
    LisaPanelShell(title = uiStrings.releaseNotes, onBack = onBack, backLabel = uiStrings.back) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 420.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PanelPurposeLine(uiStrings.releaseNotesPurpose)
            Text(
                text = uiStrings.releaseNotesVersionTitle,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = LisaBlueDark
            )
            val bullets = listOf(
                uiStrings.t("Camera-first communication", "Kamera-eerste kommunikasie", "Ukuxhumana okuqala ngekhamera"),
                uiStrings.t("Wink sequence language", "Knip-reeks taal", "Ulimi lochungechunge lama-wink"),
                uiStrings.t("Confirmation before speech", "Bevestiging voor spraak", "Ukuqinisekisa ngaphambi kokukhuluma"),
                uiStrings.t("Emergency alarm", "Noodalarm", "I-alamu yosizo oluphuthumayo"),
                uiStrings.t("Communication profiles", "Kommunikasieprofiele", "Amaphrofayela okuxhumana"),
                uiStrings.aboutLisa,
                uiStrings.testingChecklist
            )
            bullets.forEach { item ->
                Text(text = "• $item", fontSize = 14.sp, color = LisaBlueDark.copy(alpha = 0.88f), lineHeight = 20.sp)
            }
            Text(
                text = uiStrings.versionAndBuildLabel(appVersionInfo.versionName, appVersionInfo.versionCode),
                fontSize = 12.sp,
                color = LisaGray
            )
        }
    }
}
