package com.idworx.lisa

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.idworx.lisa.ui.theme.LisaBlue
import com.idworx.lisa.ui.theme.LisaBlueDark
import com.idworx.lisa.ui.theme.LisaEmergencyRed
import com.idworx.lisa.ui.theme.LisaSoftGray
import com.idworx.lisa.ui.theme.LisaWhite

private val ComposerOverlayScrim = Color.Black.copy(alpha = 0.48f)
private val ComposerPanelBackground = Color(0xFF0D1B2A).copy(alpha = 0.72f)
private val ComposerEntryBackground = Color.White.copy(alpha = 0.94f)
private val ComposerEntryHighlight = LisaBlue.copy(alpha = 0.22f)
/** Matches the bright Communication command panel ([LisaGuidedModeUi] NavBackground). */
private val ComposerCommandPanelBackground = Color.White.copy(alpha = 0.88f)

@Composable
fun EyeControlledPhraseComposerOverlay(
    uiStrings: LisaUiStrings,
    state: PhraseComposerState,
    visible: Boolean,
    composerEyeFeedback: ComposerEyeFeedback,
    inputSuspended: Boolean = false,
    onEmergency: () -> Unit,
    onEntrySelected: (PhraseComposerEntry) -> Unit,
    onCommandSelected: (PhraseComposerEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!visible) return

    val entries = PhraseComposerController.visibleEntries(state, uiStrings)
    val commandEntries = PhraseComposerController.commandPanelEntries(state, uiStrings)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ComposerOverlayScrim)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(14.dp))
                .background(ComposerPanelBackground)
                .padding(10.dp)
        ) {
            Column(modifier = Modifier.weight(1f).fillMaxSize()) {
                if (state.mode == PhraseComposerMode.Keyboard) {
                    ComposerEyeStatusBar(
                        uiStrings = uiStrings,
                        eyeFeedback = composerEyeFeedback
                    )
                    Spacer(Modifier.height(4.dp))
                }

                PhraseComposerHeader(
                    uiStrings = uiStrings,
                    state = state,
                    showTitle = state.mode != PhraseComposerMode.Keyboard,
                    compact = state.mode == PhraseComposerMode.Keyboard
                )

                state.errorMessage?.let { message ->
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = message,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = LisaWhite,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(LisaEmergencyRed.copy(alpha = 0.85f))
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    )
                }

                Spacer(Modifier.height(6.dp))

                if (state.mode != PhraseComposerMode.Keyboard) {
                    Text(
                        text = PhraseComposerController.screenTitle(state, uiStrings),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = LisaWhite
                    )
                }

                if (state.mode == PhraseComposerMode.DestinationCategorySelection) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = uiStrings.phraseComposerDestinationStepBody,
                        fontSize = 13.sp,
                        color = LisaWhite.copy(alpha = 0.85f),
                        lineHeight = 19.sp
                    )
                }

                Spacer(Modifier.height(6.dp))

                when (state.mode) {
                    PhraseComposerMode.Keyboard -> {
                        EyeControlledKeyboard(
                            uiStrings = uiStrings,
                            layoutMode = state.keyboardLayoutMode,
                            cursorRow = state.cursorRow,
                            cursorCol = state.cursorCol,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    else -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            when (state.mode) {
                                PhraseComposerMode.Success -> {
                                    state.savedMapping?.let { mapping ->
                                        SuccessSummary(
                                            uiStrings = uiStrings,
                                            mapping = mapping
                                        )
                                    }
                                }
                                PhraseComposerMode.SaveConfirmation -> {
                                    SaveConfirmationSummary(
                                        uiStrings = uiStrings,
                                        state = state
                                    )
                                }
                                PhraseComposerMode.CancelConfirm -> {
                                    Text(
                                        text = uiStrings.phraseComposerCancelConfirmBody,
                                        fontSize = 14.sp,
                                        color = LisaWhite.copy(alpha = 0.9f),
                                        lineHeight = 20.sp
                                    )
                                    Spacer(Modifier.height(4.dp))
                                }
                                else -> Unit
                            }

                            entries.forEach { entry ->
                                val highlighted = state.confirmedLeft == entry.left &&
                                    state.confirmedRight == entry.right
                                PhraseComposerEntryRow(
                                    entry = entry,
                                    highlighted = highlighted,
                                    onClick = { onEntrySelected(entry) }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.width(8.dp))

            PhraseComposerCommandPanel(
                uiStrings = uiStrings,
                commandEntries = commandEntries,
                inputSuspended = inputSuspended,
                onCommandSelected = onCommandSelected,
                onEmergency = onEmergency
            )
        }
    }
}

@Composable
private fun PhraseComposerHeader(
    uiStrings: LisaUiStrings,
    state: PhraseComposerState,
    showTitle: Boolean = true,
    compact: Boolean = false
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (showTitle) {
            Text(
                text = uiStrings.phraseComposerTitle,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = LisaWhite.copy(alpha = 0.65f)
            )
        }
        if (state.selectedCategory != null &&
            (state.mode == PhraseComposerMode.Keyboard ||
                state.mode == PhraseComposerMode.SaveConfirmation ||
                state.mode == PhraseComposerMode.Success)
        ) {
            Text(
                text = uiStrings.phraseComposerCategoryLabel,
                fontSize = 12.sp,
                color = LisaWhite.copy(alpha = 0.75f)
            )
            Text(
                text = state.categoryLabel(uiStrings),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = LisaWhite
            )
            Spacer(Modifier.height(4.dp))
        }
        Text(
            text = uiStrings.phraseComposerCurrentPhraseLabel,
            fontSize = if (compact) 11.sp else 12.sp,
            color = LisaWhite.copy(alpha = 0.75f)
        )
        Text(
            text = if (state.displayPhrase().isBlank()) "—" else state.displayPhrase(),
            fontWeight = FontWeight.Bold,
            fontSize = if (compact) 18.sp else 22.sp,
            color = LisaWhite,
            maxLines = if (compact) 2 else 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SuccessSummary(
    uiStrings: LisaUiStrings,
    mapping: WinkMapping
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ComposerEntryBackground)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = uiStrings.phraseCreatedSuccess,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = LisaBlueDark
        )
        Text(
            text = "\"${mapping.customPhrase.orEmpty()}\"",
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = LisaBlueDark
        )
        Text(
            text = uiStrings.phraseCreatedCategoryLine(
                uiStrings.caregiverPhraseCategoryLabel(
                    mapping.caregiverCategory ?: CustomPhraseEngine.CaregiverPhraseCategory.Conversation
                )
            ),
            fontSize = 13.sp,
            color = LisaBlueDark.copy(alpha = 0.9f)
        )
        Text(
            text = uiStrings.phraseCreatedSequenceLine(
                formatWinkSequenceShort(mapping.left, mapping.right)
            ),
            fontSize = 13.sp,
            color = LisaBlueDark.copy(alpha = 0.9f)
        )
    }
}

@Composable
private fun PhraseComposerEntryRow(
    entry: PhraseComposerEntry,
    highlighted: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .background(if (highlighted) ComposerEntryHighlight else ComposerEntryBackground)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .widthIn(min = 58.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (highlighted) LisaBlue.copy(alpha = 0.25f) else LisaSoftGray)
                .padding(horizontal = 8.dp, vertical = 7.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = entry.sequenceLabel,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = LisaBlueDark
            )
        }
        Text(
            text = entry.label,
            fontWeight = FontWeight.SemiBold,
            fontSize = 17.sp,
            color = LisaBlueDark,
            lineHeight = 22.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun PhraseComposerCommandPanel(
    uiStrings: LisaUiStrings,
    commandEntries: List<PhraseComposerEntry>,
    inputSuspended: Boolean,
    onCommandSelected: (PhraseComposerEntry) -> Unit,
    onEmergency: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(118.dp)
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .background(ComposerCommandPanelBackground)
            .padding(5.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        commandEntries.forEach { entry ->
            PhraseComposerCommandButton(
                entry = entry,
                enabled = entry.enabled && !inputSuspended,
                onClick = { if (entry.enabled && !inputSuspended) onCommandSelected(entry) }
            )
        }
        GuidedEmergencyNavButton(
            symbol = "🚨",
            title = uiStrings.guidedEmergencyNavTitle,
            gestureHint = uiStrings.guidedEmergencyNavHint,
            sequenceLabel = formatWinkSequenceShort(EMERGENCY_LEFT_WINKS, EMERGENCY_RIGHT_WINKS),
            compact = true,
            onClick = onEmergency
        )
    }
}

@Composable
private fun PhraseComposerCommandButton(
    entry: PhraseComposerEntry,
    enabled: Boolean,
    onClick: () -> Unit
) {
    GuidedNavigationActionButton(
        symbol = commandSymbol(entry.actionId),
        title = entry.label,
        gestureHint = entry.label,
        sequenceLabel = entry.sequenceLabel,
        enabled = enabled,
        compact = true,
        onClick = onClick
    )
}

private fun commandSymbol(actionId: PhraseComposerActionId): String = when (actionId) {
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
