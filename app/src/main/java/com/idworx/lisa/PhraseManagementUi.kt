package com.idworx.lisa

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.idworx.lisa.ui.theme.LisaBlue
import com.idworx.lisa.ui.theme.LisaBlueDark
import com.idworx.lisa.ui.theme.LisaBlueLight
import com.idworx.lisa.ui.theme.LisaEmergencyRed
import com.idworx.lisa.ui.theme.LisaGray
import com.idworx.lisa.ui.theme.LisaSoftGray
import com.idworx.lisa.ui.theme.LisaWhite

enum class PhraseManagementScreen {
    List,
    Details,
    Edit,
    Move,
    DeleteConfirm
}

data class PhraseManagementUiState(
    val screen: PhraseManagementScreen = PhraseManagementScreen.List,
    val selectedIdentity: CustomPhraseIdentity? = null,
    val editText: String = "",
    val moveTargetCategory: CustomPhraseEngine.CaregiverPhraseCategory? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    /** RC7D.15 — blink-controlled list page for custom phrases. */
    val listPageIndex: Int = 0
)

@Composable
fun VocabularyManagementPanel(
    uiStrings: LisaUiStrings,
    customPhrases: List<WinkMapping>,
    managementState: PhraseManagementUiState,
    onSelectPhrase: (CustomPhraseIdentity) -> Unit,
    onBackToList: () -> Unit,
    onBackToMenu: () -> Unit,
    onOpenEdit: () -> Unit,
    onOpenMove: () -> Unit,
    onOpenDeleteConfirm: () -> Unit,
    onEditTextChange: (String) -> Unit,
    onSaveEdit: () -> Unit,
    onSelectMoveCategory: (CustomPhraseEngine.CaregiverPhraseCategory) -> Unit,
    onConfirmMove: () -> Unit,
    onConfirmDelete: () -> Unit,
    onCancelSubScreen: () -> Unit,
    onScrollUp: () -> Unit = {},
    onScrollDown: () -> Unit = {},
    onEmergency: () -> Unit = {}
) {
    when (managementState.screen) {
        PhraseManagementScreen.List -> VocabularyPhraseListPanel(
            uiStrings = uiStrings,
            customPhrases = customPhrases,
            managementState = managementState,
            onSelectPhrase = onSelectPhrase,
            onBack = onBackToMenu,
            onScrollUp = onScrollUp,
            onScrollDown = onScrollDown,
            onEmergency = onEmergency
        )
        PhraseManagementScreen.Details -> {
            val mapping = customPhrases.firstOrNull {
                managementState.selectedIdentity?.let { id -> CustomPhraseIdentity.from(it) == id } == true
            }
            if (mapping == null) {
                VocabularyPhraseListPanel(
                    uiStrings = uiStrings,
                    customPhrases = customPhrases,
                    managementState = managementState,
                    onSelectPhrase = onSelectPhrase,
                    onBack = onBackToMenu,
                    onScrollUp = onScrollUp,
                    onScrollDown = onScrollDown,
                    onEmergency = onEmergency
                )
            } else {
                PhraseDetailsPanel(
                    uiStrings = uiStrings,
                    mapping = mapping,
                    errorMessage = managementState.errorMessage,
                    successMessage = managementState.successMessage,
                    onBack = onBackToList,
                    onEdit = onOpenEdit,
                    onMove = onOpenMove,
                    onDelete = onOpenDeleteConfirm,
                    onEmergency = onEmergency
                )
            }
        }
        PhraseManagementScreen.Edit -> PhraseEditPanel(
            uiStrings = uiStrings,
            editText = managementState.editText,
            errorMessage = managementState.errorMessage,
            onSave = onSaveEdit,
            onBack = onCancelSubScreen
        )
        PhraseManagementScreen.Move -> PhraseMovePanel(
            uiStrings = uiStrings,
            selectedCategory = managementState.moveTargetCategory,
            errorMessage = managementState.errorMessage,
            successMessage = managementState.successMessage,
            onSelectCategory = onSelectMoveCategory,
            onConfirmMove = onConfirmMove,
            onBack = onCancelSubScreen
        )
        PhraseManagementScreen.DeleteConfirm -> {
            val phrase = managementState.selectedIdentity?.phrase.orEmpty()
            PhraseDeleteConfirmPanel(
                uiStrings = uiStrings,
                phrase = phrase,
                errorMessage = managementState.errorMessage,
                onConfirmDelete = onConfirmDelete,
                onCancel = onCancelSubScreen,
                onEmergency = onEmergency
            )
        }
    }
}

@Composable
private fun VocabularyPhraseListPanel(
    uiStrings: LisaUiStrings,
    customPhrases: List<WinkMapping>,
    managementState: PhraseManagementUiState,
    onSelectPhrase: (CustomPhraseIdentity) -> Unit,
    onBack: () -> Unit,
    onScrollUp: () -> Unit,
    onScrollDown: () -> Unit,
    onEmergency: () -> Unit
) {
    val pageIndex = PhraseManagementController.coercePage(
        managementState.listPageIndex,
        customPhrases.size
    )
    val visible = PhraseManagementController.visiblePhrases(customPhrases, pageIndex)
    val commands = PhraseManagementController.listCommandEntries(
        state = managementState.copy(listPageIndex = pageIndex),
        phraseCount = customPhrases.size,
        uiStrings = uiStrings
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(LisaBlueLight)
            .padding(16.dp)
    ) {
        Text(
            text = uiStrings.vocabularyTraining,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            color = LisaBlueDark
        )
        Spacer(modifier = Modifier.height(6.dp))
        PanelPurposeLine(uiStrings.vocabularyPurpose)
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = uiStrings.vocabularyCustomPhrasesSection,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            color = LisaBlueDark,
            letterSpacing = 0.5.sp
        )
        if (customPhrases.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = PhraseManagementController.pageIndicatorLabel(pageIndex, customPhrases.size),
                fontSize = 12.sp,
                color = LisaGray
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (customPhrases.isEmpty()) {
                Text(
                    text = uiStrings.vocabularyEmptyState,
                    fontSize = 14.sp,
                    color = LisaBlueDark.copy(alpha = 0.85f),
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = uiStrings.vocabularyEmptyHint,
                    fontSize = 13.sp,
                    color = LisaGray,
                    lineHeight = 18.sp
                )
            } else {
                val slots = PhraseManagementController.visiblePhraseSelectionSlots(
                    customPhrases,
                    pageIndex
                )
                slots.forEach { (mapping, sequence) ->
                    CustomPhraseListCard(
                        uiStrings = uiStrings,
                        mapping = mapping,
                        sequenceLabel = formatWinkSequenceShort(sequence.first, sequence.second),
                        onClick = { onSelectPhrase(CustomPhraseIdentity.from(mapping)) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        PhraseManagementCommandStrip(
            commands = commands,
            onScrollUp = onScrollUp,
            onScrollDown = onScrollDown,
            onBack = onBack
        )
        Spacer(modifier = Modifier.height(8.dp))
        ComposerEmergencyCommandCard(
            uiStrings = uiStrings,
            onClick = onEmergency
        )
    }
}

@Composable
private fun PhraseManagementCommandStrip(
    commands: List<PhraseManagementController.VisibleCommand>,
    onScrollUp: () -> Unit,
    onScrollDown: () -> Unit,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        commands.forEach { command ->
            PhraseManagementNavCard(
                command = command,
                modifier = Modifier.weight(1f),
                onClick = {
                    when (command.action) {
                        PhraseManagementController.PhraseManagementNavAction.ScrollUp -> onScrollUp()
                        PhraseManagementController.PhraseManagementNavAction.ScrollDown -> onScrollDown()
                        PhraseManagementController.PhraseManagementNavAction.Back -> onBack()
                    }
                }
            )
        }
    }
}

@Composable
private fun PhraseManagementNavCard(
    command: PhraseManagementController.VisibleCommand,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sequenceLabel = formatWinkSequenceShort(command.left, command.right)
    Column(
        modifier = modifier
            .defaultMinSize(minHeight = 72.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, LisaBlue.copy(alpha = 0.18f), RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = "${command.label} $sequenceLabel" }
            .background(LisaWhite)
            .padding(horizontal = 6.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = command.symbol,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = LisaBlueDark
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = command.label,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp,
            color = LisaBlueDark,
            lineHeight = 13.sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = sequenceLabel,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = LisaBlueDark,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

@Composable
private fun CustomPhraseListCard(
    uiStrings: LisaUiStrings,
    mapping: WinkMapping,
    sequenceLabel: String,
    onClick: () -> Unit
) {
    val category = mapping.caregiverCategory ?: CustomPhraseEngine.CaregiverPhraseCategory.Conversation
    val speakSequence = formatWinkSequenceShort(mapping.left, mapping.right)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(LisaWhite)
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Text(
            text = "\"${mapping.customPhrase.orEmpty().uppercase()}\"",
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            color = LisaBlueDark
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = uiStrings.caregiverPhraseCategoryLabel(category),
            fontSize = 13.sp,
            color = LisaBlueDark.copy(alpha = 0.75f)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = uiStrings.phraseManagementSpeakSequence(speakSequence),
            fontSize = 12.sp,
            color = LisaGray
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = uiStrings.phraseManagementOpenDetailsSequence(sequenceLabel),
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = LisaBlue
        )
    }
}

@Composable
private fun PhraseDetailsPanel(
    uiStrings: LisaUiStrings,
    mapping: WinkMapping,
    errorMessage: String?,
    successMessage: String?,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit,
    onEmergency: () -> Unit
) {
    val category = mapping.caregiverCategory ?: CustomPhraseEngine.CaregiverPhraseCategory.Conversation
    val backSequence = formatWinkSequenceShort(
        GuidedModeNavigation.BACK_LEFT,
        GuidedModeNavigation.BACK_RIGHT
    )
    LisaPanelShell(
        title = uiStrings.phraseManagementDetailsTitle,
        onBack = onBack,
        backLabel = "${uiStrings.back} $backSequence"
    ) {
        DetailRow(uiStrings.phraseEditorPhraseLabel, "\"${mapping.customPhrase.orEmpty().uppercase()}\"")
        DetailRow(uiStrings.phraseEditorCategoryLabel, uiStrings.caregiverPhraseCategoryLabel(category))
        DetailRow(
            uiStrings.phraseManagementSequenceLabel,
            formatWinkSequenceShort(mapping.left, mapping.right)
        )
        successMessage?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = it, fontSize = 13.sp, color = LisaBlue)
        }
        errorMessage?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = it, fontSize = 13.sp, color = LisaEmergencyRed)
        }
        Spacer(modifier = Modifier.height(12.dp))
        PhraseManagementController.detailsActionEntries(uiStrings).forEach { entry ->
            ManagementActionButton(
                text = entry.label,
                sequenceLabel = formatWinkSequenceShort(entry.left, entry.right),
                onClick = {
                    when (entry.action) {
                        PhraseManagementController.PhraseDetailsAction.Edit -> onEdit()
                        PhraseManagementController.PhraseDetailsAction.Move -> onMove()
                        PhraseManagementController.PhraseDetailsAction.Delete -> onDelete()
                    }
                },
                destructive = entry.action == PhraseManagementController.PhraseDetailsAction.Delete
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        Spacer(modifier = Modifier.height(4.dp))
        ComposerEmergencyCommandCard(
            uiStrings = uiStrings,
            onClick = onEmergency
        )
    }
}

@Composable
private fun PhraseEditPanel(
    uiStrings: LisaUiStrings,
    editText: String,
    errorMessage: String?,
    onSave: () -> Unit,
    onBack: () -> Unit
) {
    val backSequence = formatWinkSequenceShort(
        GuidedModeNavigation.BACK_LEFT,
        GuidedModeNavigation.BACK_RIGHT
    )
    LisaPanelShell(
        title = uiStrings.phraseManagementEditTitle,
        onBack = onBack,
        backLabel = "${uiStrings.back} $backSequence"
    ) {
        Text(
            text = editText.ifBlank { "—" },
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            color = LisaBlueDark
        )
        errorMessage?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = it, fontSize = 13.sp, color = LisaEmergencyRed)
        }
        Spacer(modifier = Modifier.height(12.dp))
        ManagementActionButton(text = uiStrings.phraseManagementSaveEdit, onClick = onSave)
    }
}

@Composable
private fun PhraseMovePanel(
    uiStrings: LisaUiStrings,
    selectedCategory: CustomPhraseEngine.CaregiverPhraseCategory?,
    errorMessage: String?,
    successMessage: String?,
    onSelectCategory: (CustomPhraseEngine.CaregiverPhraseCategory) -> Unit,
    onConfirmMove: () -> Unit,
    onBack: () -> Unit
) {
    val backSequence = formatWinkSequenceShort(
        GuidedModeNavigation.BACK_LEFT,
        GuidedModeNavigation.BACK_RIGHT
    )
    LisaPanelShell(
        title = uiStrings.phraseManagementMoveTitle,
        onBack = onBack,
        backLabel = "${uiStrings.back} $backSequence"
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CustomPhraseEngine.selectableCategories.forEach { category ->
                val selected = selectedCategory == category
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selected) LisaSoftGray else LisaWhite)
                        .clickable { onSelectCategory(category) }
                        .padding(14.dp)
                ) {
                    Text(
                        text = uiStrings.caregiverPhraseCategoryLabel(category),
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        fontSize = 14.sp,
                        color = LisaBlueDark
                    )
                }
            }
        }
        successMessage?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = it, fontSize = 13.sp, color = LisaBlue)
        }
        errorMessage?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = it, fontSize = 13.sp, color = LisaEmergencyRed)
        }
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onConfirmMove,
            enabled = selectedCategory != null,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = LisaBlue)
        ) {
            Text(uiStrings.phraseManagementConfirmMove, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun PhraseDeleteConfirmPanel(
    uiStrings: LisaUiStrings,
    phrase: String,
    errorMessage: String?,
    onConfirmDelete: () -> Unit,
    onCancel: () -> Unit,
    onEmergency: () -> Unit
) {
    val backSequence = formatWinkSequenceShort(
        GuidedModeNavigation.BACK_LEFT,
        GuidedModeNavigation.BACK_RIGHT
    )
    LisaPanelShell(
        title = uiStrings.phraseManagementDeleteConfirmTitle,
        onBack = onCancel,
        backLabel = "${uiStrings.back} $backSequence"
    ) {
        Text(
            text = "\"${phrase.uppercase()}\"",
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            color = LisaBlueDark
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = uiStrings.phraseManagementDeleteConfirmBody,
            fontSize = 14.sp,
            color = LisaBlueDark.copy(alpha = 0.85f)
        )
        errorMessage?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = it, fontSize = 13.sp, color = LisaEmergencyRed)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onConfirmDelete,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = LisaEmergencyRed)
        ) {
            Text(uiStrings.phraseManagementDeleteConfirmAction, fontWeight = FontWeight.SemiBold)
        }
        Spacer(modifier = Modifier.height(8.dp))
        ManagementActionButton(text = uiStrings.phraseManagementCancel, onClick = onCancel)
        Spacer(modifier = Modifier.height(12.dp))
        ComposerEmergencyCommandCard(
            uiStrings = uiStrings,
            onClick = onEmergency
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(text = label, fontSize = 12.sp, color = LisaGray)
        Text(text = value, fontSize = 15.sp, color = LisaBlueDark, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ManagementActionButton(
    text: String,
    onClick: () -> Unit,
    destructive: Boolean = false,
    sequenceLabel: String? = null
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (destructive) LisaEmergencyRed else LisaBlue
        )
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text, fontWeight = FontWeight.SemiBold)
            if (!sequenceLabel.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = sequenceLabel,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}
