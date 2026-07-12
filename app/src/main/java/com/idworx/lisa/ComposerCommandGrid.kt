package com.idworx.lisa

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import com.idworx.lisa.ui.theme.LisaBlue
import com.idworx.lisa.ui.theme.LisaBlueDark
import com.idworx.lisa.ui.theme.LisaEmergencyRed
import com.idworx.lisa.ui.theme.LisaGray
import com.idworx.lisa.ui.theme.LisaSoftGray
import com.idworx.lisa.ui.theme.LisaWhite

private val CommandCardBackground = LisaWhite.copy(alpha = 0.94f)
private val CommandGridBackground = LisaWhite.copy(alpha = 0.88f)

/**
 * Large navigation and composer command grid above the bottom-aligned keyboard (RC7D.4).
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
                .clip(RoundedCornerShape(12.dp))
                .background(CommandGridBackground)
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
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

@Composable
private fun ComposerCommandGridRow(
    actionIds: List<PhraseComposerActionId>,
    commandEntries: List<PhraseComposerEntry>,
    inputSuspended: Boolean,
    onCommandSelected: (PhraseComposerEntry) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
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
    modifier: Modifier = Modifier
) {
    val contentColor = if (enabled) LisaBlueDark else LisaGray
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .defaultMinSize(minHeight = 76.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, LisaBlue.copy(alpha = 0.18f), RoundedCornerShape(12.dp))
            .clickable(
                enabled = enabled,
                role = Role.Button,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .semantics { contentDescription = entry.label }
            .background(if (enabled) CommandCardBackground else LisaSoftGray.copy(alpha = 0.65f))
            .padding(horizontal = 6.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = composerCommandSymbol(entry.actionId),
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            color = contentColor
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = entry.label,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp,
            color = contentColor,
            lineHeight = 13.sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = entry.sequenceLabel,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            color = contentColor,
            lineHeight = 13.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun ComposerEmergencyCommandCard(
    uiStrings: LisaUiStrings,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 64.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = uiStrings.emergency }
            .background(LisaEmergencyRed.copy(alpha = 0.15f))
            .border(1.5.dp, LisaEmergencyRed.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "🚨",
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            color = LisaEmergencyRed
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = uiStrings.guidedEmergencyNavTitle,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = LisaEmergencyRed,
            textAlign = TextAlign.Center
        )
        Text(
            text = formatWinkSequenceShort(EMERGENCY_LEFT_WINKS, EMERGENCY_RIGHT_WINKS),
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = LisaEmergencyRed,
            lineHeight = 14.sp
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
    PhraseComposerActionId.ConfirmSave -> "💾"
    PhraseComposerActionId.CancelSave -> "✕"
    else -> "•"
}
