package com.idworx.lisa

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.idworx.lisa.ui.theme.SharedKeyboardTheme

private val ComposerEntryBackground = SharedKeyboardTheme.InputCardBackground
private val ComposerEntryHighlight = LisaBlue.copy(alpha = 0.22f)
private val ComposerEntryPartialHighlight = LisaBlue.copy(alpha = 0.12f)

@Composable
fun EyeControlledPhraseComposerOverlay(
    uiStrings: LisaUiStrings,
    state: PhraseComposerState,
    visible: Boolean,
    composerEyeFeedback: ComposerEyeFeedback,
    inputSuspended: Boolean = false,
    onSensitivityDecrease: () -> Unit = {},
    onSensitivityIncrease: () -> Unit = {},
    onResponseTimeDecrease: () -> Unit = {},
    onResponseTimeIncrease: () -> Unit = {},
    onEmergency: () -> Unit,
    onEntrySelected: (PhraseComposerEntry) -> Unit,
    onCommandSelected: (PhraseComposerEntry) -> Unit,
    onKeyTouched: (row: Int, col: Int) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    if (!visible) return

    val entries = PhraseComposerController.visibleEntries(state, uiStrings)
    val commandEntries = PhraseComposerController.commandPanelEntries(state, uiStrings)

    // RC8.2 — same KeyboardWorkspace surface language as Feedback.
    KeyboardWorkspaceSurface(modifier = modifier, scrimmed = true) {
        when (state.mode) {
            PhraseComposerMode.Keyboard -> KeyboardComposerLayout(
                uiStrings = uiStrings,
                state = state,
                composerEyeFeedback = composerEyeFeedback,
                commandEntries = commandEntries,
                inputSuspended = inputSuspended,
                onSensitivityDecrease = onSensitivityDecrease,
                onSensitivityIncrease = onSensitivityIncrease,
                onResponseTimeDecrease = onResponseTimeDecrease,
                onResponseTimeIncrease = onResponseTimeIncrease,
                onEmergency = onEmergency,
                onCommandSelected = onCommandSelected,
                onKeyTouched = onKeyTouched
            )
            else -> NonKeyboardComposerLayout(
                uiStrings = uiStrings,
                state = state,
                entries = entries,
                commandEntries = commandEntries,
                composerEyeFeedback = composerEyeFeedback,
                inputSuspended = inputSuspended,
                onSensitivityDecrease = onSensitivityDecrease,
                onSensitivityIncrease = onSensitivityIncrease,
                onResponseTimeDecrease = onResponseTimeDecrease,
                onResponseTimeIncrease = onResponseTimeIncrease,
                onEmergency = onEmergency,
                onEntrySelected = onEntrySelected,
                onCommandSelected = onCommandSelected
            )
        }
    }
}

@Composable
private fun KeyboardComposerLayout(
    uiStrings: LisaUiStrings,
    state: PhraseComposerState,
    composerEyeFeedback: ComposerEyeFeedback,
    commandEntries: List<PhraseComposerEntry>,
    inputSuspended: Boolean,
    onSensitivityDecrease: () -> Unit,
    onSensitivityIncrease: () -> Unit,
    onResponseTimeDecrease: () -> Unit,
    onResponseTimeIncrease: () -> Unit,
    onEmergency: () -> Unit,
    onCommandSelected: (PhraseComposerEntry) -> Unit,
    onKeyTouched: (row: Int, col: Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        ComposerEyeStatusBar(
            uiStrings = uiStrings,
            eyeFeedback = composerEyeFeedback,
            onSensitivityDecrease = onSensitivityDecrease,
            onSensitivityIncrease = onSensitivityIncrease,
            onResponseTimeDecrease = onResponseTimeDecrease,
            onResponseTimeIncrease = onResponseTimeIncrease
        )

        Spacer(modifier = Modifier.height(SharedKeyboardTheme.TightSpacing))

        ComposerPhraseField(
            uiStrings = uiStrings,
            state = state
        )

        state.errorMessage?.let { message ->
            Spacer(modifier = Modifier.height(SharedKeyboardTheme.TightSpacing))
            ComposerErrorBanner(message = message)
        }

        Spacer(modifier = Modifier.height(SharedKeyboardTheme.SectionSpacing))

        ComposerCommandGrid(
            uiStrings = uiStrings,
            commandEntries = commandEntries,
            inputSuspended = inputSuspended,
            onCommandSelected = onCommandSelected,
            onEmergency = onEmergency,
            modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.height(SharedKeyboardTheme.SectionSpacing))

        BottomAlignedEyeKeyboard(
            uiStrings = uiStrings,
            layoutMode = state.keyboardLayoutMode,
            cursorRow = state.cursorRow,
            cursorCol = state.cursorCol,
            shiftMode = state.keyboardShiftMode,
            inputSuspended = inputSuspended,
            onKeyTouched = onKeyTouched
        )
    }
}

@Composable
private fun NonKeyboardComposerLayout(
    uiStrings: LisaUiStrings,
    state: PhraseComposerState,
    entries: List<PhraseComposerEntry>,
    commandEntries: List<PhraseComposerEntry>,
    composerEyeFeedback: ComposerEyeFeedback,
    inputSuspended: Boolean,
    onSensitivityDecrease: () -> Unit,
    onSensitivityIncrease: () -> Unit,
    onResponseTimeDecrease: () -> Unit,
    onResponseTimeIncrease: () -> Unit,
    onEmergency: () -> Unit,
    onEntrySelected: (PhraseComposerEntry) -> Unit,
    onCommandSelected: (PhraseComposerEntry) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        ComposerEyeStatusBar(
            uiStrings = uiStrings,
            eyeFeedback = composerEyeFeedback,
            onSensitivityDecrease = onSensitivityDecrease,
            onSensitivityIncrease = onSensitivityIncrease,
            onResponseTimeDecrease = onResponseTimeDecrease,
            onResponseTimeIncrease = onResponseTimeIncrease
        )

        Spacer(modifier = Modifier.height(6.dp))

        PhraseComposerHeader(
            uiStrings = uiStrings,
            state = state,
            showTitle = true,
            compact = false
        )

        state.errorMessage?.let { message ->
            Spacer(modifier = Modifier.height(6.dp))
            ComposerErrorBanner(message = message)
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = PhraseComposerController.screenTitle(state, uiStrings),
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = LisaWhite
        )

        if (state.mode == PhraseComposerMode.DestinationCategorySelection) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = uiStrings.phraseComposerDestinationStepBody,
                fontSize = 13.sp,
                color = LisaWhite.copy(alpha = 0.85f),
                lineHeight = 19.sp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

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
                            mapping = mapping,
                            wasEdit = state.wasEdit
                        )
                    }
                }
                PhraseComposerMode.SaveConfirmation -> {
                    SaveConfirmationSummary(
                        uiStrings = uiStrings,
                        state = state
                    )
                }
                PhraseComposerMode.ConfirmDelete -> {
                    Text(
                        text = uiStrings.phraseManagementDeleteConfirmBody,
                        fontSize = 14.sp,
                        color = LisaWhite.copy(alpha = 0.9f),
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "\"" + state.displayPhrase() + "\"",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = LisaWhite
                    )
                }
                PhraseComposerMode.DuplicateWarning -> {
                    state.duplicateMatch?.let { match ->
                        DuplicateWarningSummary(
                            uiStrings = uiStrings,
                            match = match
                        )
                    }
                }
                PhraseComposerMode.CancelConfirm -> {
                    Text(
                        text = uiStrings.phraseComposerCancelConfirmBody,
                        fontSize = 14.sp,
                        color = LisaWhite.copy(alpha = 0.9f),
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                else -> Unit
            }

            entries.forEach { entry ->
                if (state.mode == PhraseComposerMode.SaveConfirmation) {
                    return@forEach
                }
                val highlightLevel = PhraseComposerEntryHighlight.level(
                    entry = entry,
                    leftWinkCount = composerEyeFeedback.leftWinkCount,
                    rightWinkCount = composerEyeFeedback.rightWinkCount
                )
                val confirmedMatch = state.confirmedLeft == entry.left &&
                    state.confirmedRight == entry.right
                PhraseComposerEntryRow(
                    entry = entry,
                    highlightLevel = if (confirmedMatch) {
                        PhraseComposerEntryHighlight.Level.Full
                    } else {
                        highlightLevel
                    },
                    onClick = { onEntrySelected(entry) }
                )
            }
        }

        if (commandEntries.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            when (state.mode) {
                PhraseComposerMode.SaveConfirmation -> {
                    ComposerConfirmationActionGrid(
                        commandEntries = commandEntries,
                        composerEyeFeedback = composerEyeFeedback,
                        inputSuspended = inputSuspended,
                        uiStrings = uiStrings,
                        onCommandSelected = onCommandSelected,
                        onEmergency = onEmergency,
                        primaryActionId = PhraseComposerActionId.ConfirmSave
                    )
                }
                PhraseComposerMode.ConfirmDelete -> {
                    ComposerConfirmationActionGrid(
                        commandEntries = commandEntries,
                        composerEyeFeedback = composerEyeFeedback,
                        inputSuspended = inputSuspended,
                        uiStrings = uiStrings,
                        onCommandSelected = onCommandSelected,
                        onEmergency = onEmergency,
                        primaryActionId = PhraseComposerActionId.ConfirmDelete
                    )
                }
                else -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        commandEntries.forEach { entry ->
                            ComposerCommandCard(
                                entry = entry,
                                enabled = entry.enabled && !inputSuspended,
                                modifier = Modifier.weight(1f),
                                onClick = { if (entry.enabled && !inputSuspended) onCommandSelected(entry) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    ComposerEmergencyCommandCard(
                        uiStrings = uiStrings,
                        onClick = onEmergency
                    )
                }
            }
        } else {
            Spacer(modifier = Modifier.height(8.dp))
            ComposerEmergencyCommandCard(
                uiStrings = uiStrings,
                onClick = onEmergency
            )
        }
    }
}

@Composable
private fun DuplicateWarningSummary(
    uiStrings: LisaUiStrings,
    match: DuplicatePhraseMatch,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ComposerEntryBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = PhraseComposerController.duplicateMessage(match, uiStrings),
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            color = LisaBlueDark,
            lineHeight = 22.sp
        )
        Text(
            text = uiStrings.phraseDuplicateHint,
            fontSize = 13.sp,
            color = LisaBlueDark.copy(alpha = 0.8f),
            lineHeight = 19.sp
        )
    }
}

@Composable
private fun ComposerPhraseField(
    uiStrings: LisaUiStrings,
    state: PhraseComposerState
) {
    KeyboardWorkspaceInputCard(
        title = uiStrings.phraseComposerCurrentPhraseLabel,
        body = state.displayPhrase(),
        placeholder = "—"
    )
}

@Composable
private fun ComposerErrorBanner(message: String) {
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
            Spacer(modifier = Modifier.height(4.dp))
        }
        if (!compact) {
            Text(
                text = uiStrings.phraseComposerCurrentPhraseLabel,
                fontSize = 12.sp,
                color = LisaWhite.copy(alpha = 0.75f)
            )
            Text(
                text = if (state.displayPhrase().isBlank()) "—" else state.displayPhrase(),
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = LisaWhite,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SuccessSummary(
    uiStrings: LisaUiStrings,
    mapping: WinkMapping,
    wasEdit: Boolean = false
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
            text = if (wasEdit) uiStrings.phraseUpdatedSuccess else uiStrings.phraseComposerSuccessTitle,
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
    highlightLevel: PhraseComposerEntryHighlight.Level,
    onClick: () -> Unit
) {
    val background = when (highlightLevel) {
        PhraseComposerEntryHighlight.Level.Full -> ComposerEntryHighlight
        PhraseComposerEntryHighlight.Level.Partial -> ComposerEntryPartialHighlight
        PhraseComposerEntryHighlight.Level.None -> ComposerEntryBackground
    }
    val sequenceBackground = when (highlightLevel) {
        PhraseComposerEntryHighlight.Level.Full -> LisaBlue.copy(alpha = 0.25f)
        PhraseComposerEntryHighlight.Level.Partial -> LisaBlue.copy(alpha = 0.14f)
        PhraseComposerEntryHighlight.Level.None -> LisaSoftGray
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .background(background)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .widthIn(min = 58.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(sequenceBackground)
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
            fontWeight = if (highlightLevel == PhraseComposerEntryHighlight.Level.Full) {
                FontWeight.Bold
            } else {
                FontWeight.SemiBold
            },
            fontSize = 17.sp,
            color = LisaBlueDark,
            lineHeight = 22.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}
