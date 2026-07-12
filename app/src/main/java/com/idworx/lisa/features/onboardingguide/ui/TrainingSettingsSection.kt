package com.idworx.lisa.features.onboardingguide.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.idworx.lisa.LisaUiStrings
import com.idworx.lisa.features.onboardingguide.coach.CommunicationCoachEngine
import com.idworx.lisa.features.onboardingguide.model.TrainingProgress
import com.idworx.lisa.features.onboardingguide.model.TrainingPreferences
import com.idworx.lisa.ui.theme.LisaBlueDark
import java.util.Locale

@Composable
fun TrainingSettingsSection(
    uiStrings: LisaUiStrings,
    preferences: TrainingPreferences,
    learningProgress: TrainingProgress? = null,
    onReplayTutorial: () -> Unit,
    onPracticeCommunication: () -> Unit,
    onPracticeNavigation: () -> Unit,
    onResetProgress: () -> Unit,
    onPreferencesChange: (TrainingPreferences) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = uiStrings.settingsSectionLearning.uppercase(Locale.getDefault()),
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            color = LisaBlueDark.copy(alpha = 0.55f),
            letterSpacing = 0.5.sp,
            modifier = Modifier.padding(start = 2.dp, top = 4.dp, bottom = 4.dp)
        )

        learningProgress?.let { progress ->
            val snapshot = CommunicationCoachEngine.caregiverSnapshot(
                progress,
                CommunicationCoachEngine.currentCommunicationLesson(progress),
                CommunicationCoachEngine.currentCommunicationLesson(progress)?.vocabularyId
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .padding(horizontal = 4.dp, vertical = 6.dp)
            ) {
                Text(
                    text = snapshot.coachSummary,
                    fontSize = 12.sp,
                    color = LisaBlueDark.copy(alpha = 0.85f),
                    lineHeight = 18.sp
                )
            }
        }

        LearningActionRow(uiStrings.replayLearningJourney, uiStrings.openLabel, onReplayTutorial)
        LearningActionRow(uiStrings.practiceCommunication, uiStrings.openLabel, onPracticeCommunication)
        LearningActionRow(uiStrings.practiceNavigation, uiStrings.openLabel, onPracticeNavigation)
        LearningActionRow(uiStrings.clearLearningProgress, uiStrings.openLabel, onResetProgress)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(uiStrings.narrationTitle, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = LisaBlueDark)
                Text(uiStrings.narrationSubtitle, fontSize = 12.sp, color = LisaBlueDark.copy(alpha = 0.7f))
            }
            Switch(
                checked = preferences.narrationEnabled,
                onCheckedChange = { onPreferencesChange(preferences.copy(narrationEnabled = it)) }
            )
        }

        SettingsSliderRowLocal(
            title = uiStrings.voiceSpeedTitle,
            valueLabel = "${(preferences.narrationSpeed * 100).toInt()}%",
            value = preferences.narrationSpeed,
            valueRange = 0.5f..1.5f,
            onValueChange = { onPreferencesChange(preferences.copy(narrationSpeed = it)) }
        )

        SettingsSliderRowLocal(
            title = uiStrings.voiceVolumeTitle,
            valueLabel = "${(preferences.narrationVolume * 100).toInt()}%",
            value = preferences.narrationVolume,
            valueRange = 0.5f..1.0f,
            onValueChange = { onPreferencesChange(preferences.copy(narrationVolume = it)) }
        )
    }
}

@Composable
private fun LearningActionRow(label: String, actionLabel: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = LisaBlueDark, modifier = Modifier.weight(1f))
        OutlinedButton(onClick = onClick) {
            Text(actionLabel, fontSize = 12.sp)
        }
    }
}

@Composable
private fun SettingsSliderRowLocal(
    title: String,
    valueLabel: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .padding(horizontal = 4.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = LisaBlueDark)
            Text(valueLabel, fontSize = 12.sp, color = LisaBlueDark.copy(alpha = 0.7f))
        }
        androidx.compose.material3.Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
