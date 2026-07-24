package com.idworx.lisa

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.ScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.idworx.lisa.ui.theme.LisaWorkspaceVisualStyle
import com.idworx.lisa.ui.theme.SharedKeyboardTheme

data class MenuDestinationUiBinding(
    val state: MenuDestinationNavigationState,
    val actions: List<MenuDestinationAction>,
    val capabilities: MenuDestinationNavigationCapabilities,
    val onCommand: (MenuDestinationPanelCommand) -> Unit,
    val onActivate: (MenuDestinationActionId) -> Unit,
    val onKeyboardKey: (row: Int, col: Int) -> Unit,
    val onViewportMetrics: (viewportHeightPx: Int, maxScrollPx: Int, scrollPx: Int) -> Unit
)

internal val LocalMenuDestinationScrollState =
    compositionLocalOf<ScrollState?> { null }

internal val LocalMenuDestinationSelectedAction =
    compositionLocalOf<MenuDestinationActionId?> { null }

internal val LocalMenuDestinationActivateAction =
    compositionLocalOf<(MenuDestinationActionId) -> Unit> { {} }

@Composable
internal fun rememberDestinationScrollState(): ScrollState =
    LocalMenuDestinationScrollState.current ?: rememberScrollState()

/**
 * Full-screen destination host below the shared eye-tracking header.
 *
 * The left content owns one shared [ScrollState]; the right command panel never enters that
 * scroll container. Page requests and selection-reveal requests are cause-aware so they cannot
 * fight each other or create an animation loop.
 */
@Composable
fun MenuDestinationWorkspace(
    uiStrings: LisaUiStrings,
    binding: MenuDestinationUiBinding,
    content: @Composable () -> Unit
) {
    val state = binding.state
    val scrollStatesByPanel = remember { mutableMapOf<LisaPanel, ScrollState>() }
    val scrollState = scrollStatesByPanel.getOrPut(state.panel) { ScrollState(0) }
    var viewportHeightPx by remember { mutableIntStateOf(0) }
    val actionCount = binding.actions.count { it.canReceiveFocus }

    LaunchedEffect(state.scrollRequestPx, state.revealSelection, scrollState.maxValue) {
        val explicitTarget = state.scrollRequestPx
        val revealTarget = if (
            explicitTarget == null &&
            state.revealSelection &&
            actionCount > 1 &&
            scrollState.maxValue > 0
        ) {
            (state.selectedIndex.toFloat() / (actionCount - 1) * scrollState.maxValue).toInt()
        } else {
            null
        }
        val target = (explicitTarget ?: revealTarget)?.coerceIn(0, scrollState.maxValue)
        if (target != null && target != scrollState.value) {
            scrollState.animateScrollTo(target)
        }
    }

    LaunchedEffect(scrollState.maxValue, viewportHeightPx) {
        snapshotFlow {
            Triple(scrollState.isScrollInProgress, scrollState.value, scrollState.maxValue)
        }.collect { (inProgress, scrollPx, maxScrollPx) ->
            if (!inProgress && viewportHeightPx > 0) {
                binding.onViewportMetrics(viewportHeightPx, maxScrollPx, scrollPx)
            }
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .semantics {
                stateDescription =
                    "Page ${state.viewportPage + 1} of ${state.viewportPageCount}"
            }
            .clip(RoundedCornerShape(SharedKeyboardTheme.SurfaceCornerRadius))
            .background(SharedKeyboardTheme.SurfaceBackground)
            .padding(SharedKeyboardTheme.SurfaceContentPadding)
    ) {
        val spacing = SharedKeyboardTheme.SectionSpacing
        val textStage =
            state.interactionStage as? MenuDestinationInteractionStage.TextEditing
        val widths = DestinationWorkspaceWidthAuthority.calculateDestinationWorkspaceWidths(
            availableWidthDp = maxWidth,
            horizontalSpacingDp = spacing,
            keyboardActive = textStage != null
        )
        CompositionLocalProvider(
            LocalMenuDestinationScrollState provides scrollState,
            LocalMenuDestinationSelectedAction provides state.selectedActionId,
            LocalMenuDestinationActivateAction provides binding.onActivate
        ) {
            // RC8.3 — Feedback keyboard / review always use full-width Custom Phrases chrome
            // (Emergency bar + single bottom Done). Side nav would duplicate Emergency.
            if (textStage != null) {
                MenuDestinationTextEditor(
                    uiStrings = uiStrings,
                    stage = textStage,
                    fieldTitle = binding.actions.firstOrNull {
                        it.id == textStage.actionId
                    }?.label.orEmpty(),
                    onKeyTouched = binding.onKeyboardKey,
                    onCommand = binding.onCommand,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(spacing)
                ) {
                    Box(
                        modifier = Modifier
                            .width(widths.contentWidthDp)
                            .fillMaxHeight()
                            .onGloballyPositioned { viewportHeightPx = it.size.height }
                    ) {
                        content()
                    }
                    MenuDestinationNavigationPanel(
                        uiStrings = uiStrings,
                        binding = binding,
                        panelWidth = widths.navigationWidthDp,
                        canPreviousPage =
                            CategoryViewportPaging.canGoToPreviousPage(state.viewportPage),
                        canNextPage = CategoryViewportPaging.canGoToNextPage(
                            state.viewportPage,
                            state.viewportPageCount
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun MenuDestinationNavigationPanel(
    uiStrings: LisaUiStrings,
    binding: MenuDestinationUiBinding,
    panelWidth: androidx.compose.ui.unit.Dp,
    canPreviousPage: Boolean,
    canNextPage: Boolean
) {
    val textStage =
        binding.state.interactionStage as? MenuDestinationInteractionStage.TextEditing
    val keyboardEditing =
        textStage?.fieldEditingStage == FeedbackFieldEditingStage.Keyboard
    val reviewing =
        textStage?.fieldEditingStage == FeedbackFieldEditingStage.Review
    val commands = if (keyboardEditing) {
        FeedbackKeyboardNavigationAuthority.keyboardCommands
    } else if (reviewing) {
        listOf(
            MenuDestinationPanelCommand.Save,
            MenuDestinationPanelCommand.ContinueEditing,
            MenuDestinationPanelCommand.Cancel,
            MenuDestinationPanelCommand.Emergency
        )
    } else {
        MenuDestinationNavigationController.visibleCommands(
            capabilities = binding.capabilities,
            viewportPageCount = binding.state.viewportPageCount
        )
    }
    Column(
        modifier = Modifier
            .width(panelWidth)
            .fillMaxHeight()
            .clip(RoundedCornerShape(LisaWorkspaceVisualStyle.NavPanelCornerRadius))
            .background(LisaWorkspaceVisualStyle.NavPanelBackground)
            .padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        commands.forEach { command ->
            val enabled = MenuDestinationNavigationController.commandIsEnabled(
                command = command,
                state = binding.state,
                actions = binding.actions,
                canPreviousPage = canPreviousPage,
                canNextPage = canNextPage,
                keyboardEditing = keyboardEditing
            )
            val (symbol, title, hint, sequence) = commandPresentation(
                command,
                uiStrings,
                keyboardContext = keyboardEditing
            )
            if (command == MenuDestinationPanelCommand.Emergency) {
                GuidedEmergencyNavButton(
                    symbol = symbol,
                    title = title,
                    gestureHint = hint,
                    sequenceLabel = sequence,
                    compact = true,
                    onClick = { binding.onCommand(command) }
                )
            } else {
                GuidedNavigationActionButton(
                    symbol = symbol,
                    title = title,
                    gestureHint = hint,
                    sequenceLabel = sequence,
                    enabled = enabled,
                    compact = true,
                    onClick = {
                        // Touch and blink share handleMenuDestinationCommand / processSequence.
                        binding.onCommand(command)
                    }
                )
            }
        }
    }
}

private data class CommandPresentation(
    val symbol: String,
    val title: String,
    val hint: String,
    val sequence: String
)

private fun commandPresentation(
    command: MenuDestinationPanelCommand,
    uiStrings: LisaUiStrings,
    keyboardContext: Boolean = false
): CommandPresentation = when (command) {
    MenuDestinationPanelCommand.MoveUp -> CommandPresentation(
        "↑↑", uiStrings.mainMenuMoveUp, uiStrings.guidedScrollUpHint,
        formatWinkSequenceShort(GuidedModeNavigation.PREVIOUS_LEFT, GuidedModeNavigation.PREVIOUS_RIGHT)
    )
    MenuDestinationPanelCommand.MoveDown -> CommandPresentation(
        "↓↓", uiStrings.mainMenuMoveDown, uiStrings.guidedScrollDownHint,
        formatWinkSequenceShort(GuidedModeNavigation.NEXT_LEFT, GuidedModeNavigation.NEXT_RIGHT)
    )
    MenuDestinationPanelCommand.PreviousPage -> CommandPresentation(
        "⏮", uiStrings.mainMenuPreviousPage, uiStrings.guidedPreviousCategoryPageHint,
        formatWinkSequenceShort(
            GuidedModeNavigation.PREVIOUS_CATEGORY_PAGE_LEFT,
            GuidedModeNavigation.PREVIOUS_CATEGORY_PAGE_RIGHT
        )
    )
    MenuDestinationPanelCommand.NextPage -> CommandPresentation(
        "⏭", uiStrings.mainMenuNextPage, uiStrings.guidedNextCategoryPageHint,
        formatWinkSequenceShort(
            GuidedModeNavigation.NEXT_CATEGORY_PAGE_LEFT,
            GuidedModeNavigation.NEXT_CATEGORY_PAGE_RIGHT
        )
    )
    MenuDestinationPanelCommand.MoveLeft -> CommandPresentation(
        "←", uiStrings.phraseComposerPanelMoveLeft, "",
        formatWinkSequenceShort(2, 1)
    )
    MenuDestinationPanelCommand.MoveRight -> CommandPresentation(
        "→", uiStrings.phraseComposerPanelMoveRight, "",
        formatWinkSequenceShort(1, 2)
    )
    MenuDestinationPanelCommand.Select -> CommandPresentation(
        "✅",
        if (keyboardContext) uiStrings.phraseComposerPanelSelectKey else uiStrings.mainMenuOpenSelected,
        uiStrings.guidedSelectEnterHint,
        formatWinkSequenceShort(GuidedModeNavigation.SELECT_LEFT, GuidedModeNavigation.SELECT_RIGHT)
    )
    MenuDestinationPanelCommand.Save -> CommandPresentation(
        "✓", "Save Field", "", formatWinkSequenceShort(1, 1)
    )
    MenuDestinationPanelCommand.DoneEditing -> {
        val doneSequence = FeedbackKeyboardNavigationAuthority.sequence(
            MenuDestinationPanelCommand.DoneEditing
        )!!
        CommandPresentation(
            "✓",
            "Done",
            "",
            formatWinkSequenceShort(doneSequence.first, doneSequence.second)
        )
    }
    MenuDestinationPanelCommand.ContinueEditing -> CommandPresentation(
        "✎", "Continue Editing", "", formatWinkSequenceShort(1, 3)
    )
    MenuDestinationPanelCommand.Cancel -> CommandPresentation(
        "✕", uiStrings.cancel, "", formatWinkSequenceShort(2, 2)
    )
    MenuDestinationPanelCommand.Back -> CommandPresentation(
        "↩",
        uiStrings.back,
        uiStrings.guidedBackHint,
        formatWinkSequenceShort(GuidedModeNavigation.BACK_LEFT, GuidedModeNavigation.BACK_RIGHT)
    )
    MenuDestinationPanelCommand.Emergency -> CommandPresentation(
        "🚨", uiStrings.emergency, uiStrings.guidedEmergencyNavHint,
        formatWinkSequenceShort(EMERGENCY_LEFT_WINKS, EMERGENCY_RIGHT_WINKS)
    )
}

@Composable
private fun MenuDestinationTextEditor(
    uiStrings: LisaUiStrings,
    stage: MenuDestinationInteractionStage.TextEditing,
    fieldTitle: String,
    onKeyTouched: (row: Int, col: Int) -> Unit,
    onCommand: (MenuDestinationPanelCommand) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = fieldTitle,
            color = com.idworx.lisa.ui.theme.LisaWhite,
            fontSize = 16.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth().padding(bottom = SharedKeyboardTheme.TightSpacing)
        )
        if (stage.fieldEditingStage == FeedbackFieldEditingStage.Review) {
            Text(
                text = "Review before saving this field",
                color = com.idworx.lisa.ui.theme.LisaWhite.copy(alpha = 0.92f),
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth().padding(bottom = SharedKeyboardTheme.TightSpacing)
            )
            FeedbackFieldReview(
                stage = stage,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.height(SharedKeyboardTheme.SectionSpacing))
            // RC8.3 — sole Emergency control for Feedback text editing.
            EmergencyActionBar(
                uiStrings = uiStrings,
                onClick = { onCommand(MenuDestinationPanelCommand.Emergency) }
            )
            Spacer(modifier = Modifier.height(SharedKeyboardTheme.TightSpacing))
            FeedbackReviewCommandRow(
                uiStrings = uiStrings,
                onCommand = onCommand
            )
        } else {
            FeedbackKeyboardDirectionLegend(
                uiStrings = uiStrings,
                modifier = Modifier.fillMaxWidth().padding(bottom = SharedKeyboardTheme.TightSpacing)
            )
            KeyboardWorkspaceInputCard(
                title = null,
                body = stage.draftText,
                placeholder = "Start typing…",
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                maxLines = Int.MAX_VALUE
            )
            Spacer(modifier = Modifier.height(SharedKeyboardTheme.SectionSpacing))
            // RC8.3 — same Emergency bar as Custom Phrases; no second Emergency control.
            EmergencyActionBar(
                uiStrings = uiStrings,
                onClick = { onCommand(MenuDestinationPanelCommand.Emergency) }
            )
            Spacer(modifier = Modifier.height(SharedKeyboardTheme.SectionSpacing))
            BottomAlignedEyeKeyboard(
                uiStrings = uiStrings,
                layoutMode = stage.layoutMode,
                cursorRow = stage.cursorRow,
                cursorCol = stage.cursorCol,
                shiftMode = stage.shiftMode,
                onKeyTouched = onKeyTouched
            )
            Spacer(modifier = Modifier.height(SharedKeyboardTheme.TightSpacing))
            // RC8.4 — Done | Back only; Emergency stays on the shared bar above the keyboard.
            val done = commandPresentation(
                MenuDestinationPanelCommand.DoneEditing,
                uiStrings,
                keyboardContext = true
            )
            val back = commandPresentation(
                MenuDestinationPanelCommand.Back,
                uiStrings,
                keyboardContext = true
            )
            KeyboardWorkspaceBottomActionRow(
                actions = listOf(
                    KeyboardWorkspaceBottomAction(
                        title = done.title,
                        sequenceLabel = done.sequence,
                        onClick = { onCommand(MenuDestinationPanelCommand.DoneEditing) }
                    ),
                    KeyboardWorkspaceBottomAction(
                        title = back.title,
                        sequenceLabel = back.sequence,
                        onClick = { onCommand(MenuDestinationPanelCommand.Back) }
                    )
                )
            )
        }
    }
}

@Composable
private fun FeedbackKeyboardDirectionLegend(
    uiStrings: LisaUiStrings,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        FeedbackKeyboardNavigationAuthority.directionCommands.forEach { command ->
            val presentation = commandPresentation(command, uiStrings, keyboardContext = true)
            KeyboardWorkspaceOutlinedChip(
                title = presentation.title,
                sequenceLabel = presentation.sequence,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun FeedbackFieldReview(
    stage: MenuDestinationInteractionStage.TextEditing,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(SharedKeyboardTheme.InputCardCornerRadius))
            .background(SharedKeyboardTheme.InputCardBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Review your feedback",
            color = com.idworx.lisa.ui.theme.LisaBlueDark,
            fontSize = 18.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
        )
        Text(
            text = stage.draftText.ifBlank { "This field will be cleared." },
            color = com.idworx.lisa.ui.theme.LisaBlueDark,
            fontSize = 17.sp,
            lineHeight = 23.sp,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "Save Field  L1 R1  •  Continue Editing  L1 R3  •  Cancel Changes  L2 R2",
            color = com.idworx.lisa.ui.theme.LisaBlueDark,
            fontSize = 14.sp,
            lineHeight = 20.sp
        )
    }
}

/** Review-stage actions only — Emergency lives exclusively on [EmergencyActionBar]. */
@Composable
private fun FeedbackReviewCommandRow(
    uiStrings: LisaUiStrings,
    onCommand: (MenuDestinationPanelCommand) -> Unit
) {
    val commands = listOf(
        MenuDestinationPanelCommand.Save,
        MenuDestinationPanelCommand.ContinueEditing,
        MenuDestinationPanelCommand.Cancel
    )
    KeyboardWorkspaceOutlinedActionRow {
        commands.forEach { command ->
            val presentation = commandPresentation(command, uiStrings, keyboardContext = false)
            KeyboardWorkspaceOutlinedAction(
                title = presentation.title,
                sequenceLabel = presentation.sequence,
                onClick = { onCommand(command) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}
