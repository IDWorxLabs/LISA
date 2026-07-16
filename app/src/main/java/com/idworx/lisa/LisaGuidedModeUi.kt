package com.idworx.lisa

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import com.idworx.lisa.ui.theme.LisaBlue
import com.idworx.lisa.ui.theme.LisaBlueDark
import com.idworx.lisa.ui.theme.LisaEmergencyRed
import com.idworx.lisa.ui.theme.LisaGray
import com.idworx.lisa.ui.theme.LisaSoftGray
import com.idworx.lisa.ui.theme.LisaWhite
import com.idworx.lisa.ui.theme.LisaWorkspaceVisualStyle
import com.idworx.lisa.features.onboardingguide.navigation.GuidedWorkspaceHighlightTarget
import com.idworx.lisa.features.onboardingguide.navigation.GuidedWorkspaceTrainingSpec

private val OverlayScrim = Color.Black.copy(alpha = 0.48f)
private val PanelBackground = LisaWorkspaceVisualStyle.OverlayPanelBackground
private val EntryBackground = LisaWorkspaceVisualStyle.CardBackground
private val EntryHighlight = LisaBlue.copy(alpha = 0.22f)
private val NavBackground = LisaWorkspaceVisualStyle.NavPanelBackground
private val CategoryMenuHighlight = LisaWorkspaceVisualStyle.CardSelectedBackground

/** Subtle blue outline + soft glow used to spotlight the real control a Guided Training lesson is teaching. */
private val TrainingHighlightGlow = LisaBlue.copy(alpha = 0.18f)
private val TrainingHighlightBorder = LisaBlue.copy(alpha = 0.85f)

private fun Modifier.guidedTrainingHighlight(active: Boolean, radius: androidx.compose.ui.unit.Dp = 12.dp): Modifier =
    if (active) {
        this
            .background(TrainingHighlightGlow, RoundedCornerShape(radius))
            .border(2.dp, TrainingHighlightBorder, RoundedCornerShape(radius))
    } else {
        this
    }

/**
 * De-emphasises a control that is NOT this lesson's target while Guided Training has an active
 * highlight, so it is unambiguous which single item the user should practice right now.
 */
private const val TrainingDimAlpha = 0.42f

private fun Modifier.guidedTrainingDim(dimmed: Boolean): Modifier =
    if (dimmed) this.alpha(TrainingDimAlpha) else this

@Composable
fun GuidedVocabularyOverlay(
    uiStrings: LisaUiStrings,
    navigationState: GuidedNavigationState,
    categoryPage: GuidedCategoryPage?,
    categoryMenuTitles: List<String>,
    confirmedPhrase: String?,
    confirmedLeft: Int?,
    confirmedRight: Int?,
    visible: Boolean,
    emergencyAwaitingConfirm: Boolean = false,
    onNavigateUp: () -> Unit,
    onSelectEnter: () -> Unit,
    onCancelSaveConfirmation: () -> Unit = {},
    onBack: () -> Unit,
    onNavigateDown: () -> Unit,
    onEmergency: () -> Unit,
    onCategories: () -> Unit,
    onPreviousCategoryPage: () -> Unit = {},
    onNextCategoryPage: () -> Unit = {},
    onDecreaseValue: () -> Unit,
    onIncreaseValue: () -> Unit,
    onPhraseEntry: (GuidedVocabularyEntry) -> Unit,
    onCategoryRow: (Int) -> Unit,
    onCategoryViewportPageState: (pageCount: Int, currentPage: Int) -> Unit = { _, _ -> },
    workspaceMode: com.idworx.lisa.features.onboardingguide.navigation.GuidedWorkspaceMode =
        com.idworx.lisa.features.onboardingguide.navigation.GuidedWorkspaceMode.NORMAL,
    trainingHighlight: GuidedWorkspaceHighlightTarget? = null,
    modifier: Modifier = Modifier
) {
    if (!visible) return

    val configuration = LocalConfiguration.current
    val visibleEntryCap = GuidedVocabularyCatalog.visibleEntryCount(
        configuration.screenWidthDp,
        configuration.screenHeightDp
    )
    val safeState = navigationState.normalized()
    val screenMode = safeState.screenMode
    val categoryIndex = safeState.categoryIndex
    val phrasePageIndex = safeState.phrasePageIndex
    val categoryMenuSelection = safeState.categoryMenuSelection
    val categoryViewportPage = safeState.categoryViewportPage
    val categoryViewportPageCount = safeState.categoryViewportPageCount
    val preferencesAdjustMode = safeState.preferencesAdjustMode
    val isPreferencesPage = categoryIndex == GuidedVocabularyCategory.PREFERENCES_CATEGORY_INDEX
    val isAdjusting = preferencesAdjustMode != GuidedPreferencesAdjustMode.None
    val pageEntries = categoryPage?.entries.orEmpty()
    val phrasePageCount = GuidedNavigationController.phrasePageCount(pageEntries.size, visibleEntryCap)
    val visiblePhraseEntries = GuidedNavigationController.visiblePhraseEntries(
        entries = pageEntries,
        phrasePageIndex = phrasePageIndex,
        visibleCap = visibleEntryCap
    )
    // Guided Training clarity: while a lesson is spotlighting one real control, every other
    // control on screen is quietly de-emphasised so it is unambiguous what to practice next.
    val trainingDimActive = workspaceMode == com.idworx.lisa.features.onboardingguide.navigation.GuidedWorkspaceMode.GUIDED_TRAINING &&
        trainingHighlight != null

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(OverlayScrim)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(14.dp))
                .background(PanelBackground)
                .padding(10.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                GuidedOverlayHeader(
                    uiStrings = uiStrings,
                    screenMode = screenMode,
                    categoryTitle = categoryPage?.title,
                    categoryMenuSelection = categoryMenuSelection,
                    categoryViewportPage = categoryViewportPage,
                    categoryViewportPageCount = categoryViewportPageCount,
                    phrasePageIndex = phrasePageIndex,
                    phrasePageCount = phrasePageCount,
                    preferencesAdjustMode = preferencesAdjustMode
                )

                confirmedPhrase?.let { phrase ->
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "\"$phrase\" — ${uiStrings.guidedPhraseConfirmed}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = LisaWhite,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(LisaBlue.copy(alpha = 0.55f))
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    )
                }

                if (emergencyAwaitingConfirm) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = uiStrings.guidedEmergencyAwaitingConfirmMessage,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = LisaWhite,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(LisaEmergencyRed.copy(alpha = 0.85f))
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    )
                }

                Spacer(Modifier.height(8.dp))

                // RC7D.25 — an active adjustment (either the Adjust Settings selection sub-mode or a
                // specific value adjustment) overlays the workspace regardless of the underlying
                // screen mode, so the entry gesture works identically from the Category Menu and any
                // phrase category page. Backing out restores the untouched underlying screen.
                if (safeState.isSettingsMenuActive) {
                    SettingsMenuPanel(
                        uiStrings = uiStrings,
                        onOpenSensitivity = onNavigateUp,
                        onOpenResponseTime = onNavigateDown,
                        onBack = onBack,
                        onEmergency = onEmergency,
                        modifier = Modifier.weight(1f)
                    )
                } else if (safeState.isSaveConfirmationActive) {
                    SaveConfirmationPanel(
                        uiStrings = uiStrings,
                        adjustMode = preferencesAdjustMode,
                        originalSensitivity = safeState.adjustmentOriginalSensitivity,
                        originalResponseTimeSec = safeState.adjustmentOriginalResponseTimeSec,
                        draftSensitivity = safeState.draftSensitivityLevel,
                        draftResponseTimeSec = safeState.draftResponseTimeSec,
                        onConfirm = onSelectEnter,
                        onCancelConfirmation = onCancelSaveConfirmation,
                        onEmergency = onEmergency,
                        modifier = Modifier.weight(1f)
                    )
                } else if (safeState.isValueAdjustmentActive) {
                    PreferencesAdjustmentPanel(
                        uiStrings = uiStrings,
                        adjustMode = preferencesAdjustMode,
                        draftResponseTimeSec = safeState.draftResponseTimeSec,
                        draftSensitivityLevel = safeState.draftSensitivityLevel,
                        scrollStep = safeState.adjustmentScrollStep,
                        onDecrease = onDecreaseValue,
                        onIncrease = onIncreaseValue,
                        onSave = onSelectEnter,
                        onCancel = onBack,
                        onEmergency = onEmergency,
                        modifier = Modifier.weight(1f)
                    )
                } else when (screenMode) {
                    GuidedOverlayScreenMode.Vocabulary -> {
                        if (categoryPage != null) {
                            Text(
                                text = categoryPage.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = LisaWhite
                            )
                            if (!isPreferencesPage) {
                                val scrollHint = uiStrings.guidedPhrasePageScrollHint(phrasePageIndex, phrasePageCount)
                                if (scrollHint != null) {
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = scrollHint,
                                        fontSize = 11.sp,
                                        color = LisaWhite.copy(alpha = 0.75f)
                                    )
                                }
                            }
                            Spacer(Modifier.height(6.dp))

                            val isCustomPage = categoryPage.category == GuidedVocabularyCategory.Custom
                            val showCustomEmptyHint = isCustomPage && pageEntries.isEmpty()

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (showCustomEmptyHint) {
                                    Text(
                                        text = uiStrings.guidedCustomEmptyTitle,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 16.sp,
                                        color = LisaWhite
                                    )
                                    Text(
                                        text = uiStrings.guidedCustomEmptyBody,
                                        fontSize = 13.sp,
                                        color = LisaWhite.copy(alpha = 0.8f)
                                    )
                                    Spacer(Modifier.height(4.dp))
                                }
                                val entriesToShow = if (isPreferencesPage) pageEntries else visiblePhraseEntries
                                entriesToShow.forEachIndexed { entryIndex, entry ->
                                    val highlighted = confirmedLeft == entry.left &&
                                        confirmedRight == entry.right &&
                                        confirmedPhrase == entry.phrase
                                    val entryTrainingHighlighted = entryIndex == 0 &&
                                        trainingHighlight == GuidedWorkspaceHighlightTarget.PhraseRow
                                    GuidedVocabularyEntryRow(
                                        entry = entry,
                                        highlighted = highlighted,
                                        trainingHighlighted = entryTrainingHighlighted,
                                        trainingDimmed = trainingDimActive && !entryTrainingHighlighted,
                                        onClick = { onPhraseEntry(entry) }
                                    )
                                }
                            }
                        }
                    }
                    GuidedOverlayScreenMode.CategoryMenu -> {
                        Text(
                            text = uiStrings.guidedCategoryMenuTitle,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = LisaWhite
                        )
                        Spacer(Modifier.height(6.dp))
                        // RC7D.22 — ONE canonical, cause-aware scroll coordinator drives the whole
                        // Category Menu. The navigation cause carried in the state decides how it
                        // moves:
                        //   • PAGE_MOVEMENT (Previous/Next Page) scrolls straight to the measured
                        //     viewport-PAGE anchor and deliberately skips selection centring, so a
                        //     page jump is one direct move — never a walk through categories and
                        //     never overridden by RC7D.21 centring.
                        //   • every other cause (Move Up/Down, direct shortcut, menu restore, touch)
                        //     centres the selected row via the RC7D.21 authority.
                        // A second effect measures the real viewport + content and syncs the
                        // canonical viewport-page count / current page back into navigation state so
                        // the header, the button-enabled state and the controller's page-nav gating
                        // all read one source of truth.
                        val categoryMenuScrollState = rememberScrollState()
                        var categoryViewportHeightPx by remember { mutableIntStateOf(0) }
                        var selectedCategoryTopPx by remember { mutableIntStateOf(0) }
                        var selectedCategoryHeightPx by remember { mutableIntStateOf(0) }
                        val categoryMaxScrollPx = categoryMenuScrollState.maxValue
                        val categoryNavigationCause = safeState.categoryNavigationCause
                        LaunchedEffect(
                            categoryMenuSelection,
                            categoryViewportPage,
                            categoryNavigationCause,
                            selectedCategoryTopPx,
                            selectedCategoryHeightPx,
                            categoryViewportHeightPx,
                            categoryMaxScrollPx
                        ) {
                            // Only scroll once real measurements exist; otherwise leave the resting
                            // offset untouched (guards against stale / unavailable coordinates).
                            if (categoryViewportHeightPx > 0 && categoryMaxScrollPx > 0) {
                                val target: Int? = if (
                                    categoryNavigationCause == CategoryNavigationCause.PAGE_MOVEMENT
                                ) {
                                    // PAGE NAVIGATION: authoritative page anchor, selection ignored.
                                    CategoryViewportPaging.pageAnchorOffsetPx(
                                        pageIndex = categoryViewportPage,
                                        viewportHeightPx = categoryViewportHeightPx,
                                        maxScrollPx = categoryMaxScrollPx
                                    )
                                } else if (selectedCategoryHeightPx > 0) {
                                    // ITEM / SHORTCUT / RESTORE: centre the selected row (RC7D.21).
                                    GuidedCategoryMenuScroll.centeredScrollOffsetPx(
                                        selectedItemTopPx = selectedCategoryTopPx,
                                        selectedItemHeightPx = selectedCategoryHeightPx,
                                        viewportHeightPx = categoryViewportHeightPx,
                                        maxScrollPx = categoryMaxScrollPx
                                    )
                                } else {
                                    null
                                }
                                if (target != null &&
                                    GuidedCategoryMenuScroll.shouldAnimateTo(
                                        current = categoryMenuScrollState.value,
                                        target = target
                                    )
                                ) {
                                    categoryMenuScrollState.animateScrollTo(target)
                                }
                            }
                        }
                        // Canonical viewport-page sync. Fires only once the list has settled so it
                        // never pushes an intermediate page mid-animation. A PAGE_MOVEMENT keeps the
                        // controller-set target page (already authoritative); every other cause maps
                        // the settled scroll offset to its viewport page so item movement crossing a
                        // boundary, restore and manual scrolling all keep the indicators accurate.
                        LaunchedEffect(
                            categoryViewportHeightPx,
                            categoryMaxScrollPx,
                            categoryViewportPage,
                            categoryNavigationCause
                        ) {
                            snapshotFlow {
                                categoryMenuScrollState.isScrollInProgress to categoryMenuScrollState.value
                            }.collect { (inProgress, scroll) ->
                                if (!inProgress && categoryViewportHeightPx > 0) {
                                    val pageCount = CategoryViewportPaging.pageCount(
                                        viewportHeightPx = categoryViewportHeightPx,
                                        maxScrollPx = categoryMaxScrollPx
                                    )
                                    val currentPage =
                                        if (categoryNavigationCause == CategoryNavigationCause.PAGE_MOVEMENT) {
                                            categoryViewportPage
                                        } else {
                                            CategoryViewportPaging.currentPageForScroll(
                                                scrollPx = scroll,
                                                viewportHeightPx = categoryViewportHeightPx,
                                                maxScrollPx = categoryMaxScrollPx
                                            )
                                        }
                                    onCategoryViewportPageState(pageCount, currentPage)
                                }
                            }
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .onGloballyPositioned { categoryViewportHeightPx = it.size.height }
                                .verticalScroll(categoryMenuScrollState),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            categoryMenuTitles.forEachIndexed { index, title ->
                                val rowTrainingHighlighted = trainingHighlight == GuidedWorkspaceHighlightTarget.CategoryRow &&
                                    index == GuidedWorkspaceTrainingSpec.conversationCategoryIndex
                                val isSelectedRow = index == categoryMenuSelection
                                GuidedCategoryMenuRow(
                                    title = title,
                                    index = index,
                                    sequenceLabel = GuidedCategoryShortcuts.sequenceLabelForCategory(index),
                                    selected = isSelectedRow,
                                    trainingHighlighted = rowTrainingHighlighted,
                                    trainingDimmed = trainingDimActive && !rowTrainingHighlighted,
                                    onClick = { onCategoryRow(index) },
                                    modifier = if (isSelectedRow) {
                                        Modifier.onGloballyPositioned { coords ->
                                            // Content-space top (unaffected by the scroll layer) so
                                            // the centring target stays stable while scrolling.
                                            selectedCategoryTopPx = coords.positionInParent().y.roundToInt()
                                            selectedCategoryHeightPx = coords.size.height
                                        }
                                    } else {
                                        Modifier
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.width(8.dp))

            GuidedModeNavigationPanel(
                uiStrings = uiStrings,
                panelContext = when {
                    isAdjusting -> GuidedNavigationPanelSpec.PanelContext.Adjustment
                    screenMode == GuidedOverlayScreenMode.CategoryMenu -> GuidedNavigationPanelSpec.PanelContext.CategoryMenu
                    else -> GuidedNavigationPanelSpec.PanelContext.Vocabulary
                },
                canGoPrevious = when {
                    isAdjusting -> true
                    screenMode == GuidedOverlayScreenMode.Vocabulary -> phrasePageIndex > 0
                    else -> categoryMenuSelection > 0
                },
                canGoNext = when {
                    isAdjusting -> true
                    screenMode == GuidedOverlayScreenMode.Vocabulary -> phrasePageIndex < phrasePageCount - 1
                    else -> categoryMenuSelection < GuidedVocabularyCategory.PAGE_COUNT - 1
                },
                canGoPreviousCategoryPage = screenMode == GuidedOverlayScreenMode.CategoryMenu &&
                    CategoryViewportPaging.canGoToPreviousPage(categoryViewportPage),
                canGoNextCategoryPage = screenMode == GuidedOverlayScreenMode.CategoryMenu &&
                    CategoryViewportPaging.canGoToNextPage(categoryViewportPage, categoryViewportPageCount),
                onNavigateUp = onNavigateUp,
                onSelectEnter = onSelectEnter,
                onBack = onBack,
                onCategories = onCategories,
                onNavigateDown = onNavigateDown,
                onPreviousCategoryPage = onPreviousCategoryPage,
                onNextCategoryPage = onNextCategoryPage,
                onEmergency = onEmergency,
                highlightTarget = trainingHighlight
            )
        }
    }
}

@Composable
private fun GuidedOverlayHeader(
    uiStrings: LisaUiStrings,
    screenMode: GuidedOverlayScreenMode,
    categoryTitle: String?,
    categoryMenuSelection: Int,
    categoryViewportPage: Int,
    categoryViewportPageCount: Int,
    phrasePageIndex: Int,
    phrasePageCount: Int,
    preferencesAdjustMode: GuidedPreferencesAdjustMode = GuidedPreferencesAdjustMode.None
) {
    val modeLabel = when {
        preferencesAdjustMode == GuidedPreferencesAdjustMode.SettingsMenu -> uiStrings.guidedAdjustSettingsTitle
        preferencesAdjustMode == GuidedPreferencesAdjustMode.ResponseTime ||
            preferencesAdjustMode == GuidedPreferencesAdjustMode.ConfirmSaveResponseTime ->
            uiStrings.guidedResponseTimeAdjustmentTitle
        preferencesAdjustMode == GuidedPreferencesAdjustMode.Sensitivity ||
            preferencesAdjustMode == GuidedPreferencesAdjustMode.ConfirmSaveSensitivity ->
            uiStrings.guidedSensitivityAdjustmentTitle
        screenMode == GuidedOverlayScreenMode.Vocabulary -> uiStrings.guidedVocabularyTitle
        else -> uiStrings.guidedCategoryMenuMode
    }
    // While plainly browsing phrases, the category's own bold title is already rendered as the
    // single list header just below — repeating "Vocabulary" and the category name up here would
    // only duplicate it, so this small section label is all that shows in that state.
    val isPlainVocabularyBrowsing = screenMode == GuidedOverlayScreenMode.Vocabulary &&
        preferencesAdjustMode == GuidedPreferencesAdjustMode.None
    val isAdjustingPreferences = preferencesAdjustMode != GuidedPreferencesAdjustMode.None
    // RC7D.23 — the Category Menu renders its own "Choose a Category" heading directly above the
    // category cards, so the header must NOT also print the near-identical "Choose Category" mode
    // label (that was the duplicated heading). RC7D.26 — while Adjust Settings is open, always
    // show the settings title even when the underlying screen is still Category Menu.
    val showModeLabel = isAdjustingPreferences ||
        (!isPlainVocabularyBrowsing && screenMode != GuidedOverlayScreenMode.CategoryMenu)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = uiStrings.workspaceCommunicationTitle,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = LisaWhite.copy(alpha = 0.65f)
            )
            if (showModeLabel) {
                Text(
                    text = modeLabel,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = LisaWhite
                )
                if (screenMode == GuidedOverlayScreenMode.Vocabulary &&
                    categoryTitle != null &&
                    !isAdjustingPreferences
                ) {
                    Text(
                        text = categoryTitle,
                        fontSize = 12.sp,
                        color = LisaWhite.copy(alpha = 0.85f)
                    )
                }
            }
        }
        when {
            // RC7D.27 — settings screens do not show phrase-page or Setting 1/2 indicators.
            isAdjustingPreferences -> Unit
            screenMode == GuidedOverlayScreenMode.Vocabulary -> Text(
                text = uiStrings.guidedPhrasePageIndicator(phrasePageIndex + 1, phrasePageCount),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = LisaWhite.copy(alpha = 0.9f),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.14f))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            )
            // RC7D.22 — two INDEPENDENT indicators. "Category X / N" tracks the highlighted
            // selection (canonical ordered destination count). "Page X / Y" reports the current
            // VIEWPORT page — a measured scroll window, not selectionIndex / pageSize — so a
            // partly-visible Category 7 can read Page 1 at the top and Page 2 after a page-down.
            screenMode == GuidedOverlayScreenMode.CategoryMenu -> {
                val categoryTotal = GuidedVocabularyCategory.PAGE_COUNT
                val pageTotal = categoryViewportPageCount.coerceAtLeast(1)
                val currentPage = (categoryViewportPage + 1).coerceIn(1, pageTotal)
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.14f))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = uiStrings.guidedCategoryIndicator(categoryMenuSelection + 1, categoryTotal),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = LisaWhite.copy(alpha = 0.9f)
                    )
                    Text(
                        text = uiStrings.guidedPageIndicator(currentPage, pageTotal),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = LisaWhite.copy(alpha = 0.75f)
                    )
                }
            }
        }
    }
}

@Composable
private fun GuidedCategoryMenuRow(
    title: String,
    index: Int,
    sequenceLabel: String,
    selected: Boolean,
    trainingHighlighted: Boolean = false,
    trainingDimmed: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, enabled = !trainingDimmed, onClick = onClick)
            .background(if (selected) CategoryMenuHighlight else EntryBackground)
            .guidedTrainingHighlight(trainingHighlighted)
            .guidedTrainingDim(trainingDimmed)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "${index + 1}.",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = LisaBlueDark
        )
        Text(
            text = title,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            fontSize = 17.sp,
            color = LisaBlueDark,
            lineHeight = 22.sp,
            modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier
                .widthIn(min = 58.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (selected) LisaBlue.copy(alpha = 0.25f) else LisaSoftGray)
                .padding(horizontal = 8.dp, vertical = 7.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = sequenceLabel,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = LisaBlueDark
            )
        }
    }
}

@Composable
private fun GuidedVocabularyEntryRow(
    entry: GuidedVocabularyEntry,
    highlighted: Boolean,
    trainingHighlighted: Boolean = false,
    trainingDimmed: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, enabled = !trainingDimmed, onClick = onClick)
            .background(if (highlighted) EntryHighlight else EntryBackground)
            .guidedTrainingHighlight(trainingHighlighted)
            .guidedTrainingDim(trainingDimmed)
            .padding(horizontal = 12.dp, vertical = 10.dp),
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
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.phrase,
                fontWeight = FontWeight.SemiBold,
                fontSize = 17.sp,
                color = LisaBlueDark,
                lineHeight = 22.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            entry.englishSubtitle?.let { english ->
                Text(
                    text = english,
                    fontSize = 12.sp,
                    color = LisaGray,
                    lineHeight = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun GuidedModeNavigationPanel(
    uiStrings: LisaUiStrings,
    panelContext: GuidedNavigationPanelSpec.PanelContext,
    canGoPrevious: Boolean,
    canGoNext: Boolean,
    canGoPreviousCategoryPage: Boolean,
    canGoNextCategoryPage: Boolean,
    onNavigateUp: () -> Unit,
    onSelectEnter: () -> Unit,
    onBack: () -> Unit,
    onCategories: () -> Unit,
    onNavigateDown: () -> Unit,
    onPreviousCategoryPage: () -> Unit,
    onNextCategoryPage: () -> Unit,
    onEmergency: () -> Unit,
    highlightTarget: GuidedWorkspaceHighlightTarget? = null
) {
    val actions = GuidedNavigationPanelSpec.panelActions(uiStrings, panelContext)
    // Handler / enabled / highlight are keyed by the action's stable kind, so extra Category Menu
    // page-jump buttons wire correctly without depending on list position (RC7D.20). Touch here
    // calls the exact same canonical handlers a blink sequence drives through the controller.
    fun handlerFor(kind: GuidedPanelActionKind): () -> Unit = when (kind) {
        GuidedPanelActionKind.ScrollUp -> onNavigateUp
        GuidedPanelActionKind.ScrollDown -> onNavigateDown
        GuidedPanelActionKind.PreviousCategoryPage -> onPreviousCategoryPage
        GuidedPanelActionKind.NextCategoryPage -> onNextCategoryPage
        GuidedPanelActionKind.Select -> onSelectEnter
        GuidedPanelActionKind.Back -> onBack
        GuidedPanelActionKind.Categories -> onCategories
        GuidedPanelActionKind.Emergency -> onEmergency
    }
    fun enabledFor(kind: GuidedPanelActionKind): Boolean = when (kind) {
        GuidedPanelActionKind.ScrollUp -> canGoPrevious
        GuidedPanelActionKind.ScrollDown -> canGoNext
        GuidedPanelActionKind.PreviousCategoryPage -> canGoPreviousCategoryPage
        GuidedPanelActionKind.NextCategoryPage -> canGoNextCategoryPage
        else -> true
    }
    fun highlightFor(kind: GuidedPanelActionKind): Boolean = when (kind) {
        GuidedPanelActionKind.ScrollUp -> highlightTarget == GuidedWorkspaceHighlightTarget.PreviousPage
        GuidedPanelActionKind.ScrollDown -> highlightTarget == GuidedWorkspaceHighlightTarget.NextPage
        GuidedPanelActionKind.Back -> highlightTarget == GuidedWorkspaceHighlightTarget.Back
        GuidedPanelActionKind.Categories -> highlightTarget == GuidedWorkspaceHighlightTarget.OpenCategories
        GuidedPanelActionKind.Emergency -> highlightTarget == GuidedWorkspaceHighlightTarget.Emergency
        else -> false
    }
    // De-emphasise every other panel button while a lesson has a specific target to practice.
    val dimActive = highlightTarget != null

    Column(
        modifier = Modifier
            .width(118.dp)
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .background(NavBackground)
            .padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        actions.forEach { action ->
            val highlighted = highlightFor(action.kind)
            val dimmed = dimActive && !highlighted
            if (action.kind == GuidedPanelActionKind.Emergency) {
                GuidedEmergencyNavButton(
                    symbol = action.symbol,
                    title = action.title,
                    gestureHint = action.gestureHint,
                    sequenceLabel = action.sequenceLabel,
                    compact = true,
                    trainingHighlighted = highlighted,
                    trainingDimmed = dimmed,
                    onClick = handlerFor(action.kind)
                )
            } else {
                GuidedNavigationActionButton(
                    symbol = action.symbol,
                    title = action.title,
                    gestureHint = action.gestureHint,
                    sequenceLabel = action.sequenceLabel,
                    enabled = enabledFor(action.kind),
                    compact = true,
                    trainingHighlighted = highlighted,
                    trainingDimmed = dimmed,
                    onClick = handlerFor(action.kind)
                )
            }
        }
    }
}

@Composable
internal fun GuidedEmergencyNavButton(
    symbol: String,
    title: String,
    gestureHint: String,
    sequenceLabel: String,
    compact: Boolean = false,
    trainingHighlighted: Boolean = false,
    trainingDimmed: Boolean = false,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(role = Role.Button, enabled = !trainingDimmed, onClick = onClick)
            .background(LisaEmergencyRed.copy(alpha = 0.15f))
            .guidedTrainingHighlight(trainingHighlighted, radius = 10.dp)
            .guidedTrainingDim(trainingDimmed)
            .padding(
                horizontal = 6.dp,
                vertical = if (compact) 5.dp else 8.dp
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = symbol,
            fontWeight = FontWeight.Bold,
            fontSize = if (compact) 14.sp else 16.sp,
            color = LisaEmergencyRed
        )
        Spacer(Modifier.height(1.dp))
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            fontSize = if (compact) 9.sp else 10.sp,
            color = LisaEmergencyRed,
            lineHeight = 11.sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = gestureHint,
            fontSize = 8.sp,
            color = LisaEmergencyRed.copy(alpha = 0.95f),
            lineHeight = 10.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = sequenceLabel,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            color = LisaEmergencyRed,
            lineHeight = 10.sp
        )
    }
}

@Composable
internal fun GuidedNavigationActionButton(
    symbol: String,
    title: String,
    gestureHint: String,
    sequenceLabel: String,
    enabled: Boolean,
    compact: Boolean = false,
    trainingHighlighted: Boolean = false,
    trainingDimmed: Boolean = false,
    onClick: () -> Unit
) {
    val contentColor = if (enabled) LisaBlueDark else LisaGray
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(
                enabled = enabled && !trainingDimmed,
                role = Role.Button,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .background(if (enabled) LisaBlue.copy(alpha = 0.10f) else LisaSoftGray.copy(alpha = 0.6f))
            .guidedTrainingHighlight(trainingHighlighted, radius = 10.dp)
            .guidedTrainingDim(trainingDimmed)
            .padding(
                horizontal = 4.dp,
                vertical = if (compact) 4.dp else 8.dp
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = symbol,
            fontWeight = FontWeight.Bold,
            fontSize = if (compact) 14.sp else 16.sp,
            color = contentColor
        )
        Spacer(Modifier.height(1.dp))
        Text(
            text = title,
            fontWeight = FontWeight.SemiBold,
            fontSize = if (compact) 8.sp else 10.sp,
            color = contentColor,
            lineHeight = 10.sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = gestureHint,
            fontSize = 7.sp,
            color = contentColor.copy(alpha = 0.9f),
            lineHeight = 9.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = sequenceLabel,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            color = contentColor,
            lineHeight = 10.sp
        )
        if (!compact) {
            Spacer(Modifier.height(4.dp))
            OutlinedButton(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled
            ) {
                Text(text = title, fontSize = 8.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/**
 * RC7D.27 — Adjust Settings menu. Each setting opens immediately on touch or its blink sequence.
 */
@Composable
private fun SettingsMenuPanel(
    uiStrings: LisaUiStrings,
    onOpenSensitivity: () -> Unit,
    onOpenResponseTime: () -> Unit,
    onBack: () -> Unit,
    onEmergency: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = uiStrings.guidedAdjustSettingsTitle,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = LisaWhite
        )
        AdjustmentInstructionRow(
            sequenceLabel = formatWinkSequenceShort(
                GuidedModeNavigation.PREVIOUS_LEFT,
                GuidedModeNavigation.PREVIOUS_RIGHT
            ),
            gestureHint = uiStrings.guidedSelectSensitivitySetting,
            title = uiStrings.guidedSelectSensitivitySetting,
            onClick = onOpenSensitivity
        )
        AdjustmentInstructionRow(
            sequenceLabel = formatWinkSequenceShort(
                GuidedModeNavigation.NEXT_LEFT,
                GuidedModeNavigation.NEXT_RIGHT
            ),
            gestureHint = uiStrings.guidedSelectResponseTimeSetting,
            title = uiStrings.guidedSelectResponseTimeSetting,
            onClick = onOpenResponseTime
        )
        AdjustmentInstructionRow(
            sequenceLabel = formatWinkSequenceShort(GuidedModeNavigation.BACK_LEFT, GuidedModeNavigation.BACK_RIGHT),
            gestureHint = uiStrings.guidedBackHint,
            title = uiStrings.guidedBack,
            onClick = onBack
        )
        AdjustmentInstructionRow(
            sequenceLabel = formatWinkSequenceShort(EMERGENCY_LEFT_WINKS, EMERGENCY_RIGHT_WINKS),
            gestureHint = uiStrings.guidedEmergencyNavTitle,
            title = uiStrings.guidedEmergencyNavTitle,
            onClick = onEmergency,
            emergency = true
        )
    }
}

@Composable
private fun SaveConfirmationPanel(
    uiStrings: LisaUiStrings,
    adjustMode: GuidedPreferencesAdjustMode,
    originalSensitivity: Int,
    originalResponseTimeSec: Int,
    draftSensitivity: Int,
    draftResponseTimeSec: Int,
    onConfirm: () -> Unit,
    onCancelConfirmation: () -> Unit,
    onEmergency: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isSensitivity = adjustMode == GuidedPreferencesAdjustMode.ConfirmSaveSensitivity
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = if (isSensitivity) {
                uiStrings.guidedSaveSensitivityConfirmTitle()
            } else {
                uiStrings.guidedSaveResponseTimeConfirmTitle()
            },
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = LisaWhite
        )
        Text(
            text = if (isSensitivity) {
                uiStrings.guidedSaveConfirmOriginalSensitivity(originalSensitivity)
            } else {
                uiStrings.guidedSaveConfirmOriginalResponseTime(originalResponseTimeSec)
            },
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            color = LisaWhite
        )
        Text(
            text = if (isSensitivity) {
                uiStrings.guidedSaveConfirmNewSensitivity(draftSensitivity)
            } else {
                uiStrings.guidedSaveConfirmNewResponseTime(draftResponseTimeSec)
            },
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            color = LisaWhite
        )
        AdjustmentInstructionRow(
            sequenceLabel = formatWinkSequenceShort(
                GuidedModeNavigation.SELECT_LEFT,
                GuidedModeNavigation.SELECT_RIGHT
            ),
            gestureHint = uiStrings.guidedSelectEnterHint,
            title = uiStrings.guidedConfirmSave,
            onClick = onConfirm
        )
        AdjustmentInstructionRow(
            sequenceLabel = uiStrings.guidedConfirmCancelSequenceLabel,
            gestureHint = uiStrings.guidedCancelSaveConfirmation,
            title = uiStrings.guidedCancelSaveConfirmation,
            onClick = onCancelConfirmation
        )
        AdjustmentInstructionRow(
            sequenceLabel = formatWinkSequenceShort(EMERGENCY_LEFT_WINKS, EMERGENCY_RIGHT_WINKS),
            gestureHint = uiStrings.guidedEmergencyNavTitle,
            title = uiStrings.guidedEmergencyNavTitle,
            onClick = onEmergency,
            emergency = true
        )
    }
}

@Composable
private fun PreferencesAdjustmentPanel(
    uiStrings: LisaUiStrings,
    adjustMode: GuidedPreferencesAdjustMode,
    draftResponseTimeSec: Int,
    draftSensitivityLevel: Int,
    scrollStep: Int,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onEmergency: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    LaunchedEffect(scrollStep) {
        scrollState.animateScrollTo((scrollStep * 72).coerceAtMost(scrollState.maxValue))
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = when (adjustMode) {
                GuidedPreferencesAdjustMode.ResponseTime -> uiStrings.guidedResponseTimeAdjustmentTitle
                GuidedPreferencesAdjustMode.Sensitivity -> uiStrings.guidedSensitivityAdjustmentTitle
                else -> ""
            },
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = LisaWhite
        )
        Text(
            text = when (adjustMode) {
                GuidedPreferencesAdjustMode.ResponseTime ->
                    uiStrings.guidedCurrentResponseTime(draftResponseTimeSec)
                GuidedPreferencesAdjustMode.Sensitivity ->
                    uiStrings.guidedCurrentSensitivity(draftSensitivityLevel)
                else -> ""
            },
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            color = LisaWhite
        )
        if (adjustMode == GuidedPreferencesAdjustMode.ResponseTime) {
            Text(
                text = uiStrings.guidedResponseTimeMeterHint,
                fontSize = 12.sp,
                color = LisaWhite.copy(alpha = 0.75f)
            )
        }

        when (adjustMode) {
            GuidedPreferencesAdjustMode.ResponseTime -> SettingAdjustmentMeter(
                label = uiStrings.guidedResponseTimeTitle,
                currentValueLabel = PreferenceAdjustmentBarSpec.formatResponseTimeTick(draftResponseTimeSec),
                minimumValue = SequenceProcessingDelay.MIN_SECONDS,
                maximumValue = SequenceProcessingDelay.MAX_SECONDS,
                currentValue = draftResponseTimeSec,
                decreaseLabel = uiStrings.guidedDecreaseShort,
                decreaseSequence = formatWinkSequenceShort(
                    GuidedModeNavigation.DECREASE_VALUE_LEFT,
                    GuidedModeNavigation.DECREASE_VALUE_RIGHT
                ),
                increaseLabel = uiStrings.guidedIncreaseShort,
                increaseSequence = formatWinkSequenceShort(
                    GuidedModeNavigation.INCREASE_VALUE_LEFT,
                    GuidedModeNavigation.INCREASE_VALUE_RIGHT
                ),
                onDecrease = onDecrease,
                onIncrease = onIncrease
            )
            GuidedPreferencesAdjustMode.Sensitivity -> SettingAdjustmentMeter(
                label = uiStrings.guidedSensitivityTitle,
                currentValueLabel = PreferenceAdjustmentBarSpec.formatSensitivityTick(draftSensitivityLevel),
                minimumValue = MIN_SENSITIVITY_LEVEL,
                maximumValue = MAX_SENSITIVITY_LEVEL,
                currentValue = draftSensitivityLevel,
                decreaseLabel = uiStrings.guidedDecreaseShort,
                decreaseSequence = formatWinkSequenceShort(
                    GuidedModeNavigation.DECREASE_VALUE_LEFT,
                    GuidedModeNavigation.DECREASE_VALUE_RIGHT
                ),
                increaseLabel = uiStrings.guidedIncreaseShort,
                increaseSequence = formatWinkSequenceShort(
                    GuidedModeNavigation.INCREASE_VALUE_LEFT,
                    GuidedModeNavigation.INCREASE_VALUE_RIGHT
                ),
                onDecrease = onDecrease,
                onIncrease = onIncrease
            )
            else -> Unit
        }

        // Every sequenceLabel below is derived from the exact same GuidedModeNavigation/emergency
        // constants processPreferencesAdjustmentGesture checks against — never a separately
        // hardcoded copy — so this panel can never drift out of sync with what a gesture does.
        // RC7D.27 — Categories is intentionally omitted here; it remains on the right nav panel.
        AdjustmentInstructionRow(
            sequenceLabel = formatWinkSequenceShort(GuidedModeNavigation.SELECT_LEFT, GuidedModeNavigation.SELECT_RIGHT),
            gestureHint = uiStrings.guidedSelectEnterHint,
            title = when (adjustMode) {
                GuidedPreferencesAdjustMode.ResponseTime -> uiStrings.guidedSaveResponseTime
                GuidedPreferencesAdjustMode.Sensitivity -> uiStrings.guidedSaveSensitivity
                else -> uiStrings.guidedSaveSelectedValue
            },
            onClick = onSave
        )
        AdjustmentInstructionRow(
            sequenceLabel = formatWinkSequenceShort(GuidedModeNavigation.BACK_LEFT, GuidedModeNavigation.BACK_RIGHT),
            gestureHint = uiStrings.guidedBackHint,
            title = uiStrings.guidedCancelBack,
            onClick = onCancel
        )
        AdjustmentInstructionRow(
            sequenceLabel = formatWinkSequenceShort(EMERGENCY_LEFT_WINKS, EMERGENCY_RIGHT_WINKS),
            gestureHint = uiStrings.guidedEmergencyNavTitle,
            title = uiStrings.guidedEmergencyNavTitle,
            onClick = onEmergency,
            emergency = true
        )
    }
}

/**
 * RC7D.26 — reusable visual level meter for Sensitivity and Response Time adjustment.
 * Discrete command control (touch sides / blink sequences), never a drag slider.
 */
@Composable
private fun SettingAdjustmentMeter(
    label: String,
    currentValueLabel: String,
    minimumValue: Int,
    maximumValue: Int,
    currentValue: Int,
    decreaseLabel: String,
    decreaseSequence: String,
    increaseLabel: String,
    increaseSequence: String,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit
) {
    val activeCount = SettingAdjustmentMeterAuthority.activeSegmentCount(
        value = currentValue,
        minimum = minimumValue,
        maximum = maximumValue
    )
    val segmentCount = SettingAdjustmentMeterAuthority.SEGMENT_COUNT
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(LisaWhite.copy(alpha = 0.10f))
            .padding(horizontal = 10.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "$label — $currentValueLabel",
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = LisaWhite,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MeterSideControl(
                symbol = "−",
                label = decreaseLabel,
                sequence = decreaseSequence,
                onClick = onDecrease,
                modifier = Modifier.widthIn(min = 72.dp)
            )
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                repeat(segmentCount) { index ->
                    val active = index < activeCount
                    val heightFraction = 0.28f + (index.toFloat() / (segmentCount - 1).coerceAtLeast(1)) * 0.72f
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .height((56.dp * heightFraction))
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            .background(
                                if (active) LisaBlue.copy(alpha = 0.95f)
                                else LisaWhite.copy(alpha = 0.22f)
                            )
                            .then(
                                if (active) {
                                    Modifier.border(
                                        width = 1.5.dp,
                                        color = LisaWhite.copy(alpha = 0.55f),
                                        shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                                    )
                                } else {
                                    Modifier.border(
                                        width = 1.dp,
                                        color = LisaWhite.copy(alpha = 0.18f),
                                        shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                                    )
                                }
                            )
                    )
                }
            }
            MeterSideControl(
                symbol = "+",
                label = increaseLabel,
                sequence = increaseSequence,
                onClick = onIncrease,
                modifier = Modifier.widthIn(min = 72.dp)
            )
        }
    }
}

@Composable
private fun MeterSideControl(
    symbol: String,
    label: String,
    sequence: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .background(EntryBackground)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(text = symbol, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = LisaBlueDark)
        Text(text = label, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = LisaBlueDark, maxLines = 1)
        Text(text = sequence, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = LisaBlueDark, maxLines = 1)
    }
}

@Composable
private fun AdjustmentInstructionRow(
    sequenceLabel: String,
    gestureHint: String,
    title: String,
    onClick: () -> Unit,
    emergency: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .background(
                if (emergency) LisaEmergencyRed.copy(alpha = 0.15f) else EntryBackground
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        val labelColor = if (emergency) LisaEmergencyRed else LisaBlueDark
        Text(text = sequenceLabel, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = labelColor)
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = labelColor)
            Text(text = gestureHint, fontSize = 11.sp, color = if (emergency) LisaEmergencyRed else LisaGray)
        }
    }
}

@Composable
private fun ResponseTimeValueBar(
    selectedSec: Int,
    onSelect: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(LisaWhite.copy(alpha = 0.12f))
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        SequenceProcessingDelay.allowedSeconds.forEach { sec ->
            ValueBarChip(
                label = "[${PreferenceAdjustmentBarSpec.formatResponseTimeTick(sec)}]",
                selected = PreferenceAdjustmentBarSpec.isHighlighted(sec, selectedSec),
                onClick = { onSelect(sec) }
            )
        }
    }
}

@Composable
private fun SensitivityValueBar(
    selectedLevel: Int,
    onSelect: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(LisaWhite.copy(alpha = 0.12f))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            (MIN_SENSITIVITY_LEVEL..5).forEach { level ->
                ValueBarChip(
                    label = "[${PreferenceAdjustmentBarSpec.formatSensitivityTick(level)}]",
                    selected = PreferenceAdjustmentBarSpec.isHighlighted(level, selectedLevel),
                    onClick = { onSelect(level) }
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            (6..MAX_SENSITIVITY_LEVEL).forEach { level ->
                ValueBarChip(
                    label = "[${PreferenceAdjustmentBarSpec.formatSensitivityTick(level)}]",
                    selected = PreferenceAdjustmentBarSpec.isHighlighted(level, selectedLevel),
                    onClick = { onSelect(level) }
                )
            }
        }
    }
}

@Composable
private fun ValueBarChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .background(if (selected) LisaBlue else LisaSoftGray.copy(alpha = 0.85f))
            .padding(horizontal = 8.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 13.sp,
            color = if (selected) LisaWhite else LisaBlueDark
        )
    }
}
