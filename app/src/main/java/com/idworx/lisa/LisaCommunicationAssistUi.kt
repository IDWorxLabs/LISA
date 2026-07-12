package com.idworx.lisa

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.idworx.lisa.ui.theme.LisaBlue
import com.idworx.lisa.ui.theme.LisaBlueDark
import com.idworx.lisa.ui.theme.LisaBlueLight
import com.idworx.lisa.ui.theme.LisaGray
import com.idworx.lisa.ui.theme.LisaSoftGray
import com.idworx.lisa.ui.theme.LisaWhite

@Composable
fun UnknownSequenceHelpOverlay(
    uiStrings: LisaUiStrings,
    suggestions: List<ContextualHelpSuggestion>,
    visible: Boolean
) {
    if (!visible) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(LisaBlueLight)
            .padding(14.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = uiStrings.unknownHelpHeadline,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = LisaBlueDark,
            lineHeight = 21.sp
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = uiStrings.unknownHelpSubheadline,
            fontSize = 13.sp,
            color = LisaBlueDark.copy(alpha = 0.85f),
            lineHeight = 18.sp
        )
        Spacer(Modifier.height(10.dp))
        suggestions.forEach { suggestion ->
            val label = suggestion.systemLabel ?: suggestion.phrase
            Column(modifier = Modifier.padding(vertical = 3.dp)) {
                Text(
                    text = "${formatWinkSequenceShort(suggestion.left, suggestion.right)} — $label",
                    fontSize = 12.sp,
                    color = if (suggestion.isSystemCommand) LisaBlueDark else LisaBlueDark,
                    fontWeight = if (suggestion.isSystemCommand) FontWeight.SemiBold else FontWeight.Normal,
                    lineHeight = 16.sp
                )
                suggestion.englishSubtitle?.let { english ->
                    Text(text = english, fontSize = 10.sp, color = LisaGray, lineHeight = 14.sp)
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = uiStrings.unknownHelpDismissHint,
            fontSize = 10.sp,
            color = LisaGray,
            lineHeight = 14.sp
        )
    }
}

/**
 * The single source of truth for every Quick Controls gesture label — reads the exact same
 * [LisaSystemLanguage.quickControlCommands] entry [LisaSystemLanguage.resolveQuickControlCommand]
 * checks against, so this UI can never show a gesture that differs from what actually executes.
 */
private fun quickControlGesture(action: SystemCommandAction): String =
    LisaSystemLanguage.quickControlCommands.first { it.action == action }.sequenceLabel

@Composable
fun QuickControlsOverlay(
    uiStrings: LisaUiStrings,
    responseSpeed: ResponseSpeed,
    listeningPaused: Boolean,
    onSelectSpeed: (ResponseSpeed) -> Unit,
    onDecreaseSensitivity: () -> Unit,
    onIncreaseSensitivity: () -> Unit,
    onRepeatLastPhrase: () -> Unit,
    onTogglePause: () -> Unit,
    onOpenPractice: () -> Unit,
    onClose: () -> Unit,
    visible: Boolean
) {
    if (!visible) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(LisaBlueLight)
            .padding(14.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = uiStrings.quickControlsTitle,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = LisaBlueDark
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = uiStrings.quickControlsEyeHint,
            fontSize = 11.sp,
            color = LisaGray,
            lineHeight = 15.sp
        )
        Spacer(Modifier.height(10.dp))

        QuickControlSectionLabel(uiStrings.responseSpeedTitle)
        LisaSystemLanguage.quickControlCommands
            .filter { it.action in setOf(SystemCommandAction.SetSpeedFast, SystemCommandAction.SetSpeedNormal, SystemCommandAction.SetSpeedSlow) }
            .forEach { cmd ->
                QuickControlEyeRow(
                    sequence = cmd.sequenceLabel,
                    label = LisaSystemLanguage.labelFor(cmd, uiStrings),
                    highlighted = when (cmd.action) {
                        SystemCommandAction.SetSpeedFast -> responseSpeed == ResponseSpeed.Fast
                        SystemCommandAction.SetSpeedNormal -> responseSpeed == ResponseSpeed.Normal
                        SystemCommandAction.SetSpeedSlow -> responseSpeed == ResponseSpeed.Slow
                        else -> false
                    },
                    onClick = {
                        when (cmd.action) {
                            SystemCommandAction.SetSpeedFast -> onSelectSpeed(ResponseSpeed.Fast)
                            SystemCommandAction.SetSpeedNormal -> onSelectSpeed(ResponseSpeed.Normal)
                            SystemCommandAction.SetSpeedSlow -> onSelectSpeed(ResponseSpeed.Slow)
                            else -> Unit
                        }
                    }
                )
            }

        QuickControlSectionLabel(uiStrings.sensitivity)
        QuickControlEyeRow(
            quickControlGesture(SystemCommandAction.DecreaseSensitivity),
            uiStrings.systemSensitivityDecrease,
            onClick = onDecreaseSensitivity
        )
        QuickControlEyeRow(
            quickControlGesture(SystemCommandAction.IncreaseSensitivity),
            uiStrings.systemSensitivityIncrease,
            onClick = onIncreaseSensitivity
        )

        QuickControlSectionLabel(uiStrings.quickControlsLanguage)
        QuickControlPlannedRow(uiStrings.quickControlsLanguage, uiStrings.touchForNow)
        // TODO: Eye-controlled language selection — left wink cycles, right wink confirms.

        QuickControlSectionLabel(uiStrings.quickControlsVolume)
        QuickControlPlannedRow(uiStrings.quickControlsVolume, uiStrings.touchForNow)
        // TODO: Eye-controlled volume selection — left/right wink adjusts level.

        QuickControlSectionLabel(uiStrings.systemRepeatLast)
        QuickControlEyeRow(
            quickControlGesture(SystemCommandAction.RepeatLastPhrase),
            uiStrings.systemRepeatLast,
            onClick = onRepeatLastPhrase
        )

        QuickControlSectionLabel(if (listeningPaused) uiStrings.quickControlsResumeListening else uiStrings.quickControlsPauseListening)
        QuickControlEyeRow(
            quickControlGesture(SystemCommandAction.TogglePauseListening),
            uiStrings.systemTogglePause,
            highlighted = listeningPaused,
            onClick = onTogglePause
        )

        QuickControlSectionLabel(uiStrings.quickControlsPracticeMode)
        QuickControlEyeRow(
            quickControlGesture(SystemCommandAction.OpenPracticeMode),
            uiStrings.systemOpenPractice,
            onClick = onOpenPractice
        )

        Spacer(Modifier.height(8.dp))
        QuickControlEyeRow(
            quickControlGesture(SystemCommandAction.CloseQuickControls),
            uiStrings.systemCloseQuickControls,
            onClick = onClose
        )
        Spacer(Modifier.height(6.dp))
        OutlinedButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
            Text(uiStrings.closeQuickControls)
        }
    }
}

@Composable
fun PracticeModeOverlay(
    uiStrings: LisaUiStrings,
    language: PreferredLanguage,
    itemIndex: Int,
    feedback: PracticeFeedback?,
    visible: Boolean,
    onClose: () -> Unit
) {
    if (!visible) return
    val item = PracticeModeCatalog.items.getOrNull(itemIndex) ?: return
    val phrase = LisaCoreVocabulary.text(item.vocabularyId, language)
    val english = if (language != PreferredLanguage.English) {
        LisaCoreVocabulary.text(item.vocabularyId, PreferredLanguage.English)
    } else null

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(LisaBlueLight)
            .padding(14.dp)
    ) {
        Text(
            text = uiStrings.practiceModeTitle,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = LisaBlueDark
        )
        Spacer(Modifier.height(10.dp))
        Text(text = uiStrings.practiceTrySaying, fontSize = 13.sp, color = LisaBlueDark.copy(alpha = 0.8f))
        Text(
            text = "\"$phrase\"",
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            color = LisaBlueDark,
            lineHeight = 21.sp
        )
        english?.let {
            Text(text = it, fontSize = 11.sp, color = LisaGray)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = "${uiStrings.practiceSequence} ${formatWinkSequenceShort(item.left, item.right)}",
            fontSize = 12.sp,
            color = LisaBlueDark
        )
        feedback?.let { fb ->
            Spacer(Modifier.height(10.dp))
            Text(
                text = when (fb) {
                    PracticeFeedback.Correct -> uiStrings.practiceCorrect
                    PracticeFeedback.Almost -> uiStrings.practiceAlmost
                    PracticeFeedback.TryAgain -> uiStrings.practiceTryAgain
                },
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = LisaBlueDark,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(LisaSoftGray)
                    .padding(10.dp)
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(text = uiStrings.practiceCloseHint, fontSize = 10.sp, color = LisaGray)
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
            Text(uiStrings.closeQuickControls)
        }
    }
}

enum class PracticeFeedback {
    Correct,
    Almost,
    TryAgain
}

@Composable
private fun QuickControlSectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        color = LisaBlueDark.copy(alpha = 0.55f),
        letterSpacing = 0.5.sp,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

@Composable
private fun QuickControlEyeRow(
    sequence: String,
    label: String,
    highlighted: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (highlighted) LisaBlue.copy(alpha = 0.15f) else LisaWhite)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = sequence,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = LisaBlueDark,
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(LisaSoftGray)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )
        Text(text = label, fontSize = 12.sp, color = LisaBlueDark, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun QuickControlPlannedRow(label: String, touchHint: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(LisaWhite)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 12.sp, color = LisaBlueDark, modifier = Modifier.weight(1f))
        Text(text = touchHint, fontSize = 10.sp, color = LisaGray)
    }
}

@Composable
fun ResponseSpeedPicker(
    uiStrings: LisaUiStrings,
    selected: ResponseSpeed,
    onSelect: (ResponseSpeed) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(LisaWhite)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = uiStrings.responseSpeedTitle,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = LisaBlueDark,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
        )
        ResponseSpeed.entries.forEach { speed ->
            ResponseSpeedRow(
                uiStrings = uiStrings,
                speed = speed,
                selected = speed == selected,
                onSelect = { onSelect(speed) }
            )
        }
    }
}

@Composable
private fun ResponseSpeedRow(
    uiStrings: LisaUiStrings,
    speed: ResponseSpeed,
    selected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onSelect)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = uiStrings.responseSpeedLabel(speed),
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                color = LisaBlueDark
            )
            Text(
                text = uiStrings.responseSpeedDescription(speed),
                fontSize = 11.sp,
                color = LisaGray,
                lineHeight = 15.sp
            )
        }
    }
}
