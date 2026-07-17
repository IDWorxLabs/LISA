package com.idworx.lisa

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.ScrollState
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.idworx.lisa.ui.theme.LisaWorkspaceVisualStyle

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
            .clip(RoundedCornerShape(LisaWorkspaceVisualStyle.PanelCornerRadius))
            .background(LisaWorkspaceVisualStyle.SolidPanelBackground)
            .padding(LisaWorkspaceVisualStyle.PanelContentPadding)
    ) {
        val spacing = 8.dp
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
            if (textStage != null && widths.usesKeyboardFocusedLayout) {
                Column(modifier = Modifier.fillMaxSize()) {
                    MenuDestinationTextEditor(
                        uiStrings = uiStrings,
                        stage = textStage,
                        fieldTitle = binding.actions.firstOrNull {
                            it.id == textStage.actionId
                        }?.label.orEmpty(),
                        onKeyTouched = binding.onKeyboardKey,
                        modifier = Modifier.weight(1f)
                    )
                    KeyboardFocusedCommandBar(
                        uiStrings = uiStrings,
                        reviewing = textStage.fieldEditingStage ==
                            FeedbackFieldEditingStage.Review,
                        onCommand = binding.onCommand
                    )
                }
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
                        if (textStage != null) {
                            MenuDestinationTextEditor(
                                uiStrings = uiStrings,
                                stage = textStage,
                                fieldTitle = binding.actions.firstOrNull {
                                    it.id == textStage.actionId
                                }?.label.orEmpty(),
                                onKeyTouched = binding.onKeyboardKey
                            )
                        } else {
                            content()
                        }
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
        listOf(
            MenuDestinationPanelCommand.MoveUp,
            MenuDestinationPanelCommand.MoveDown,
            MenuDestinationPanelCommand.MoveLeft,
            MenuDestinationPanelCommand.MoveRight,
            MenuDestinationPanelCommand.Select,
            MenuDestinationPanelCommand.DoneEditing,
            MenuDestinationPanelCommand.Back,
            MenuDestinationPanelCommand.Emergency
        )
    } else if (reviewing) {
        listOf(
            MenuDestinationPanelCommand.Save,
            MenuDestinationPanelCommand.ContinueEditing,
            MenuDestinationPanelCommand.Cancel,
            MenuDestinationPanelCommand.Emergency
        )
    } else {
        MenuDestinationNavigationController.visibleCommands(binding.capabilities)
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
            val enabled = when (command) {
                MenuDestinationPanelCommand.MoveUp ->
                    keyboardEditing || binding.state.selectedIndex > 0
                MenuDestinationPanelCommand.MoveDown ->
                    keyboardEditing || binding.state.selectedIndex <
                        binding.actions.count { it.canReceiveFocus } - 1
                MenuDestinationPanelCommand.PreviousPage -> canPreviousPage
                MenuDestinationPanelCommand.NextPage -> canNextPage
                else -> true
            }
            val (symbol, title, hint, sequence) = commandPresentation(command, uiStrings)
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
                        if (command == MenuDestinationPanelCommand.Select && textStage == null) {
                            binding.state.selectedActionId?.let(binding.onActivate)
                        } else {
                            binding.onCommand(command)
                        }
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
    uiStrings: LisaUiStrings
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
        "✅", uiStrings.mainMenuOpenSelected, uiStrings.guidedSelectEnterHint,
        formatWinkSequenceShort(GuidedModeNavigation.SELECT_LEFT, GuidedModeNavigation.SELECT_RIGHT)
    )
    MenuDestinationPanelCommand.Save -> CommandPresentation(
        "✓", "Save Field", "", formatWinkSequenceShort(1, 1)
    )
    MenuDestinationPanelCommand.DoneEditing -> CommandPresentation(
        "✓", "Done Editing", "",
        ModeScopedGestureAuthority.phraseComposerCommandSequences
            .getValue(PhraseComposerActionId.Save).let {
                formatWinkSequenceShort(it.first, it.second)
            }
    )
    MenuDestinationPanelCommand.ContinueEditing -> CommandPresentation(
        "✎", "Continue Editing", "", formatWinkSequenceShort(1, 3)
    )
    MenuDestinationPanelCommand.Cancel -> CommandPresentation(
        "✕", uiStrings.cancel, "", formatWinkSequenceShort(2, 2)
    )
    MenuDestinationPanelCommand.Back -> CommandPresentation(
        "↩", uiStrings.back, uiStrings.guidedBackHint,
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
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
        )
        Text(
            text = if (stage.fieldEditingStage == FeedbackFieldEditingStage.Review) {
                "Review before saving this field"
            } else {
                "Move L2 R0 / L0 R2 / L2 R1 / L1 R2  •  Select Key L1 R1"
            },
            color = com.idworx.lisa.ui.theme.LisaWhite.copy(alpha = 0.92f),
            fontSize = 12.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
        )
        if (stage.fieldEditingStage == FeedbackFieldEditingStage.Review) {
            FeedbackFieldReview(stage = stage)
        } else {
            Text(
                text = stage.draftText.ifBlank { "Start typing…" },
                color = com.idworx.lisa.ui.theme.LisaBlueDark,
                fontSize = 18.sp,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(LisaWorkspaceVisualStyle.CardBackground)
                    .padding(12.dp)
            )
            BottomAlignedEyeKeyboard(
                uiStrings = uiStrings,
                layoutMode = stage.layoutMode,
                cursorRow = stage.cursorRow,
                cursorCol = stage.cursorCol,
                shiftMode = stage.shiftMode,
                onKeyTouched = onKeyTouched
            )
        }
    }
}

@Composable
private fun FeedbackFieldReview(stage: MenuDestinationInteractionStage.TextEditing) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .background(LisaWorkspaceVisualStyle.CardBackground)
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

@Composable
private fun KeyboardFocusedCommandBar(
    uiStrings: LisaUiStrings,
    reviewing: Boolean,
    onCommand: (MenuDestinationPanelCommand) -> Unit
) {
    val commands = if (reviewing) {
        listOf(
            MenuDestinationPanelCommand.Save,
            MenuDestinationPanelCommand.ContinueEditing,
            MenuDestinationPanelCommand.Cancel,
            MenuDestinationPanelCommand.Emergency
        )
    } else {
        listOf(
            MenuDestinationPanelCommand.Select,
            MenuDestinationPanelCommand.DoneEditing,
            MenuDestinationPanelCommand.Back,
            MenuDestinationPanelCommand.Emergency
        )
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        commands.forEach { command ->
            val presentation = commandPresentation(command, uiStrings)
            OutlinedButton(
                onClick = { onCommand(command) },
                modifier = Modifier.weight(1f)
            ) {
                Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                    Text(
                        presentation.title,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        presentation.sequence,
                        fontSize = 10.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
