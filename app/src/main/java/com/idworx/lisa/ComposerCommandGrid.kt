package com.idworx.lisa

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.idworx.lisa.ui.theme.LisaEmergencyRed
import com.idworx.lisa.ui.theme.SharedKeyboardTheme

/**
 * Large navigation and composer command grid above the bottom-aligned keyboard (RC7D.4).
 * RC8.2 — outlined KeyboardWorkspace action chrome (Feedback visual standard).
 */
@Composable
fun ComposerCommandGrid(
    uiStrings: LisaUiStrings,
    commandEntries: List<PhraseComposerEntry>,
    inputSuspended: Boolean,
    onCommandSelected: (PhraseComposerEntry) -> Unit,
    onEmergency: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val screenWidthDp = maxWidth.value.toInt()
        val rows = ComposerCommandGridLayout.commandRows(screenWidthDp)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SharedKeyboardTheme.ActionGridBackground)
                .padding(vertical = 2.dp),
            verticalArrangement = Arrangement.spacedBy(SharedKeyboardTheme.SectionSpacing)
        ) {
            rows.forEach { rowActions ->
                ComposerCommandGridRow(
                    actionIds = rowActions,
                    commandEntries = commandEntries,
                    inputSuspended = inputSuspended,
                    onCommandSelected = onCommandSelected
                )
            }
            ComposerEmergencyCommandCard(
                uiStrings = uiStrings,
                onClick = onEmergency
            )
        }
    }
}

/** Large eye-selectable confirmation actions for save and duplicate screens (RC7D.9). */
@Composable
fun ComposerConfirmationActionGrid(
    commandEntries: List<PhraseComposerEntry>,
    composerEyeFeedback: ComposerEyeFeedback,
    inputSuspended: Boolean,
    uiStrings: LisaUiStrings,
    onCommandSelected: (PhraseComposerEntry) -> Unit,
    onEmergency: () -> Unit,
    modifier: Modifier = Modifier,
    primaryActionId: PhraseComposerActionId = PhraseComposerActionId.ConfirmSave
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(SharedKeyboardTheme.ActionGridBackground)
            .padding(vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(SharedKeyboardTheme.SectionSpacing)
    ) {
        commandEntries.forEach { entry ->
            val highlightLevel = PhraseComposerEntryHighlight.level(
                entry = entry,
                leftWinkCount = composerEyeFeedback.leftWinkCount,
                rightWinkCount = composerEyeFeedback.rightWinkCount
            )
            ComposerConfirmationCommandCard(
                entry = entry,
                enabled = entry.enabled && !inputSuspended,
                primary = entry.actionId == primaryActionId,
                highlightLevel = highlightLevel,
                onClick = { if (entry.enabled && !inputSuspended) onCommandSelected(entry) }
            )
        }
        ComposerEmergencyCommandCard(
            uiStrings = uiStrings,
            onClick = onEmergency
        )
    }
}

@Composable
private fun ComposerConfirmationCommandCard(
    entry: PhraseComposerEntry,
    enabled: Boolean,
    primary: Boolean,
    highlightLevel: PhraseComposerEntryHighlight.Level,
    onClick: () -> Unit
) {
    val selected = enabled && (
        primary || highlightLevel != PhraseComposerEntryHighlight.Level.None
        )
    KeyboardWorkspaceClickableActionCard(
        title = entry.label,
        sequenceLabel = entry.sequenceLabel,
        onClick = onClick,
        enabled = enabled,
        selected = selected,
        icon = composerCommandSymbol(entry.actionId),
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(
                minHeight = if (primary) 84.dp else SharedKeyboardTheme.ActionMinHeight
            )
    )
}

@Composable
private fun ComposerCommandGridRow(
    actionIds: List<PhraseComposerActionId>,
    commandEntries: List<PhraseComposerEntry>,
    inputSuspended: Boolean,
    onCommandSelected: (PhraseComposerEntry) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(SharedKeyboardTheme.ActionRowSpacing)
    ) {
        actionIds.forEach { actionId ->
            val entry = ComposerCommandGridLayout.resolveEntry(actionId, commandEntries)
            if (entry != null) {
                ComposerCommandCard(
                    entry = entry,
                    enabled = entry.enabled && !inputSuspended,
                    modifier = Modifier.weight(1f),
                    onClick = { if (entry.enabled && !inputSuspended) onCommandSelected(entry) }
                )
            }
        }
    }
}

@Composable
fun ComposerCommandCard(
    entry: PhraseComposerEntry,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false
) {
    KeyboardWorkspaceClickableActionCard(
        title = entry.label,
        sequenceLabel = entry.sequenceLabel,
        onClick = onClick,
        enabled = enabled,
        selected = selected,
        icon = composerCommandSymbol(entry.actionId),
        modifier = modifier
    )
}

/**
 * RC8.3 — shared Emergency bar for Custom Phrases and Feedback keyboard workspaces.
 * Prefer [EmergencyActionBar] at Feedback call sites; both resolve to this one style.
 */
@Composable
fun EmergencyActionBar(
    uiStrings: LisaUiStrings,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) = ComposerEmergencyCommandCard(
    uiStrings = uiStrings,
    onClick = onClick,
    modifier = modifier
)

@Composable
fun ComposerEmergencyCommandCard(
    uiStrings: LisaUiStrings,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sequenceLabel = formatWinkSequenceShort(EMERGENCY_LEFT_WINKS, EMERGENCY_RIGHT_WINKS)
    val title = uiStrings.guidedEmergencyNavTitle
    // RC7D.24 — single horizontal row: label on the left, the emergency icon in the centre and the
    // wink sequence on the right. Emergency colour/visibility/workflow unchanged (RC8.2 / RC8.3
    // visual passes do not restyle Emergency).
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 72.dp)
            .clip(RoundedCornerShape(SharedKeyboardTheme.ActionCornerRadius))
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = "${title} ${sequenceLabel}" }
            .background(LisaEmergencyRed.copy(alpha = 0.15f))
            .border(1.5.dp, LisaEmergencyRed.copy(alpha = 0.55f), RoundedCornerShape(SharedKeyboardTheme.ActionCornerRadius))
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = LisaEmergencyRed,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "🚨",
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = LisaEmergencyRed
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = sequenceLabel,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = LisaEmergencyRed,
            lineHeight = 16.sp,
            textAlign = TextAlign.Start,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

internal fun composerCommandSymbol(actionId: PhraseComposerActionId): String = when (actionId) {
    PhraseComposerActionId.MoveUp -> "↑"
    PhraseComposerActionId.MoveDown -> "↓"
    PhraseComposerActionId.MoveLeft -> "←"
    PhraseComposerActionId.MoveRight -> "→"
    PhraseComposerActionId.SelectKey -> "✓"
    PhraseComposerActionId.Backspace -> "⌫"
    PhraseComposerActionId.Preview -> "🔊"
    PhraseComposerActionId.Save -> "💾"
    PhraseComposerActionId.Back -> "↩"
    PhraseComposerActionId.ToggleKeyboardLayout -> "⌨"
    PhraseComposerActionId.ConfirmSave -> "✓"
    PhraseComposerActionId.CancelSave -> "✕"
    PhraseComposerActionId.OpenDuplicateCategory -> "📂"
    PhraseComposerActionId.ContinueEditing -> "✎"
    PhraseComposerActionId.ConfirmDelete -> "🗑"
    PhraseComposerActionId.ViewInCategory -> "📂"
    PhraseComposerActionId.ChooseAnotherCategory -> "☰"
    else -> "•"
}
