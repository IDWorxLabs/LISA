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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
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
import com.idworx.lisa.features.experiencepolish.caregiverconfidence.CaregiverConfidenceEngine
import com.idworx.lisa.features.onboardingguide.navigation.GuidedWorkspaceHighlightTarget
import com.idworx.lisa.features.onboardingguide.navigation.GuidedWorkspaceTrainingSpec

private val OverlayScrim = Color.Black.copy(alpha = 0.48f)
private val PanelBackground = Color(0xFF0D1B2A).copy(alpha = 0.72f)
private val EntryBackground = LisaWhite.copy(alpha = 0.90f)
private val EntryHighlight = LisaBlue.copy(alpha = 0.22f)
private val NavBackground = LisaWhite.copy(alpha = 0.88f)
private val CategoryMenuHighlight = LisaBlue.copy(alpha = 0.30f)

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
    onNavigateUp: () -> Unit,
    onSelectEnter: () -> Unit,
    onBack: () -> Unit,
    onNavigateDown: () -> Unit,
    onEmergency: () -> Unit,
    onCategories: () -> Unit,
    onDecreaseValue: () -> Unit,
    onIncreaseValue: () -> Unit,
    onChooseCategory: () -> Unit,
    onPhraseEntry: (GuidedVocabularyEntry) -> Unit,
    onCategoryRow: (Int) -> Unit,
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
                    categoryIndex = categoryIndex,
                    phrasePageIndex = phrasePageIndex,
                    phrasePageCount = phrasePageCount,
                    preferencesAdjustMode = preferencesAdjustMode,
                    contextHint = when {
                        isAdjusting -> uiStrings.workspaceContextHintAdjustment()
                        screenMode == GuidedOverlayScreenMode.CategoryMenu ->
                            uiStrings.workspaceContextHintCategoryMenu()
                        else -> uiStrings.workspaceContextHintVocabulary()
                    }
                )

                CaregiverHelpStrip(uiStrings = uiStrings)

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

                Spacer(Modifier.height(8.dp))

                when (screenMode) {
                    GuidedOverlayScreenMode.Vocabulary -> {
                        if (isAdjusting) {
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
                                onCategories = onCategories,
                                onEmergency = onEmergency,
                                modifier = Modifier.weight(1f)
                            )
                        } else if (categoryPage != null) {
                            Text(
                                text = categoryPage.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = LisaWhite
                            )
                            if (!isPreferencesPage && pageEntries.size > visibleEntryCap) {
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = uiStrings.guidedScrollForMore,
                                    fontSize = 11.sp,
                                    color = LisaWhite.copy(alpha = 0.75f)
                                )
                            }
                            Spacer(Modifier.height(6.dp))

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (!isPreferencesPage) {
                                    val openCategoriesHighlighted = trainingHighlight == GuidedWorkspaceHighlightTarget.OpenCategories
                                    GuidedCategoryMenuAccessRow(
                                        title = uiStrings.guidedChooseCategoryAction,
                                        sequenceLabel = "L4 R4",
                                        highlighted = confirmedLeft == GuidedModeNavigation.CATEGORIES_LEFT &&
                                            confirmedRight == GuidedModeNavigation.CATEGORIES_RIGHT &&
                                            confirmedPhrase == uiStrings.guidedChooseCategoryAction,
                                        trainingHighlighted = openCategoriesHighlighted,
                                        trainingDimmed = trainingDimActive && !openCategoriesHighlighted,
                                        onClick = onChooseCategory
                                    )
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
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            categoryMenuTitles.forEachIndexed { index, title ->
                                val rowTrainingHighlighted = trainingHighlight == GuidedWorkspaceHighlightTarget.CategoryRow &&
                                    index == GuidedWorkspaceTrainingSpec.conversationCategoryIndex
                                GuidedCategoryMenuRow(
                                    title = title,
                                    index = index,
                                    sequenceLabel = GuidedCategoryShortcuts.sequenceLabelForCategory(index),
                                    selected = index == categoryMenuSelection,
                                    trainingHighlighted = rowTrainingHighlighted,
                                    trainingDimmed = trainingDimActive && !rowTrainingHighlighted,
                                    onClick = { onCategoryRow(index) }
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
                onNavigateUp = onNavigateUp,
                onSelectEnter = onSelectEnter,
                onBack = onBack,
                onCategories = onCategories,
                onNavigateDown = onNavigateDown,
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
    categoryIndex: Int,
    phrasePageIndex: Int,
    phrasePageCount: Int,
    preferencesAdjustMode: GuidedPreferencesAdjustMode = GuidedPreferencesAdjustMode.None,
    contextHint: String? = null
) {
    val modeLabel = when {
        preferencesAdjustMode == GuidedPreferencesAdjustMode.ResponseTime -> uiStrings.guidedResponseTimeTitle
        preferencesAdjustMode == GuidedPreferencesAdjustMode.Sensitivity -> uiStrings.guidedSensitivityTitle
        screenMode == GuidedOverlayScreenMode.Vocabulary -> uiStrings.guidedVocabularyTitle
        else -> uiStrings.guidedCategoryMenuMode
    }
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
            Text(
                text = modeLabel,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = LisaWhite
            )
            if (screenMode == GuidedOverlayScreenMode.Vocabulary && categoryTitle != null) {
                Text(
                    text = categoryTitle,
                    fontSize = 12.sp,
                    color = LisaWhite.copy(alpha = 0.85f)
                )
            }
            contextHint?.let { hint ->
                Text(
                    text = hint,
                    fontSize = 11.sp,
                    color = LisaWhite.copy(alpha = 0.78f),
                    lineHeight = 15.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Text(
            text = when (screenMode) {
                GuidedOverlayScreenMode.Vocabulary ->
                    uiStrings.guidedPhrasePageIndicator(phrasePageIndex + 1, phrasePageCount)
                GuidedOverlayScreenMode.CategoryMenu ->
                    uiStrings.guidedCategoryIndicator(categoryIndex + 1, GuidedVocabularyCategory.PAGE_COUNT)
            },
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = LisaWhite.copy(alpha = 0.9f),
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White.copy(alpha = 0.14f))
                .padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

@Composable
private fun CaregiverHelpStrip(uiStrings: LisaUiStrings) {
    val gestureHint = CaregiverConfidenceEngine.workspaceGestureHint()
        ?: uiStrings.workspaceCaregiverHelpLegend
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.10f))
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Text(
            text = uiStrings.workspaceCaregiverHelpTitle,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = LisaWhite.copy(alpha = 0.85f)
        )
        Text(
            text = gestureHint.removePrefix("Caregiver: ").trim(),
            fontSize = 9.sp,
            color = LisaWhite.copy(alpha = 0.72f),
            lineHeight = 12.sp,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = uiStrings.workspacePatienceHint,
            fontSize = 9.sp,
            color = LisaWhite.copy(alpha = 0.6f),
            lineHeight = 11.sp
        )
    }
}

@Composable
private fun GuidedCategoryMenuAccessRow(
    title: String,
    sequenceLabel: String,
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
            .background(if (highlighted) EntryHighlight else LisaBlue.copy(alpha = 0.35f))
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
                .background(LisaWhite.copy(alpha = 0.85f))
                .padding(horizontal = 8.dp, vertical = 7.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = sequenceLabel,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = LisaBlueDark
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = LisaWhite,
                lineHeight = 22.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
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
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
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
    onNavigateUp: () -> Unit,
    onSelectEnter: () -> Unit,
    onBack: () -> Unit,
    onCategories: () -> Unit,
    onNavigateDown: () -> Unit,
    onEmergency: () -> Unit,
    highlightTarget: GuidedWorkspaceHighlightTarget? = null
) {
    val actions = GuidedNavigationPanelSpec.panelActions(uiStrings, panelContext)
    val handlers = listOf(
        onNavigateUp,
        onSelectEnter,
        onBack,
        onCategories,
        onEmergency,
        onNavigateDown
    )
    val enabledFlags = listOf(canGoPrevious, true, true, true, true, canGoNext)
    // Index order mirrors GuidedNavigationPanelSpec.panelActions: Previous, Select, Back, Categories, Emergency, Next.
    val highlightFlags = listOf(
        highlightTarget == GuidedWorkspaceHighlightTarget.PreviousPage,
        false,
        highlightTarget == GuidedWorkspaceHighlightTarget.Back,
        highlightTarget == GuidedWorkspaceHighlightTarget.OpenCategories,
        highlightTarget == GuidedWorkspaceHighlightTarget.Emergency,
        highlightTarget == GuidedWorkspaceHighlightTarget.NextPage
    )
    // De-emphasise every other panel button while a lesson has a specific target to practice.
    val dimActive = highlightTarget != null
    val dimFlags = highlightFlags.map { isTarget -> dimActive && !isTarget }

    Column(
        modifier = Modifier
            .width(118.dp)
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .background(NavBackground)
            .padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        actions.forEachIndexed { index, action ->
            if (action.sequenceLabel == "L6 R0") {
                GuidedEmergencyNavButton(
                    symbol = action.symbol,
                    title = action.title,
                    gestureHint = action.gestureHint,
                    sequenceLabel = action.sequenceLabel,
                    compact = true,
                    trainingHighlighted = highlightFlags[index],
                    trainingDimmed = dimFlags[index],
                    onClick = handlers[index]
                )
            } else {
                GuidedNavigationActionButton(
                    symbol = action.symbol,
                    title = action.title,
                    gestureHint = action.gestureHint,
                    sequenceLabel = action.sequenceLabel,
                    enabled = enabledFlags[index],
                    compact = true,
                    trainingHighlighted = highlightFlags[index],
                    trainingDimmed = dimFlags[index],
                    onClick = handlers[index]
                )
            }
        }
    }
}

@Composable
private fun GuidedEmergencyNavButton(
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
private fun GuidedNavigationActionButton(
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
    onCategories: () -> Unit,
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
                GuidedPreferencesAdjustMode.ResponseTime -> uiStrings.guidedResponseTimeTitle
                GuidedPreferencesAdjustMode.Sensitivity -> uiStrings.guidedSensitivityTitle
                GuidedPreferencesAdjustMode.None -> ""
            },
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = LisaWhite
        )
        Text(
            text = when (adjustMode) {
                GuidedPreferencesAdjustMode.ResponseTime ->
                    uiStrings.guidedAdjustmentCurrentValueResponseTime(draftResponseTimeSec)
                GuidedPreferencesAdjustMode.Sensitivity ->
                    uiStrings.guidedAdjustmentCurrentValueSensitivity(draftSensitivityLevel)
                GuidedPreferencesAdjustMode.None -> ""
            },
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            color = LisaWhite
        )

        when (adjustMode) {
            GuidedPreferencesAdjustMode.ResponseTime -> ResponseTimeValueBar(
                selectedSec = draftResponseTimeSec,
                onSelect = { }
            )
            GuidedPreferencesAdjustMode.Sensitivity -> SensitivityValueBar(
                selectedLevel = draftSensitivityLevel,
                onSelect = { }
            )
            GuidedPreferencesAdjustMode.None -> Unit
        }

        AdjustmentInstructionRow(
            sequenceLabel = "L3 R1",
            gestureHint = uiStrings.guidedDecreaseValue,
            title = when (adjustMode) {
                GuidedPreferencesAdjustMode.ResponseTime -> uiStrings.guidedDecreaseResponseTime
                GuidedPreferencesAdjustMode.Sensitivity -> uiStrings.guidedDecreaseSensitivity
                else -> uiStrings.guidedDecreaseValue
            },
            onClick = onDecrease
        )
        AdjustmentInstructionRow(
            sequenceLabel = "L1 R3",
            gestureHint = uiStrings.guidedIncreaseValue,
            title = when (adjustMode) {
                GuidedPreferencesAdjustMode.ResponseTime -> uiStrings.guidedIncreaseResponseTime
                GuidedPreferencesAdjustMode.Sensitivity -> uiStrings.guidedIncreaseSensitivity
                else -> uiStrings.guidedIncreaseValue
            },
            onClick = onIncrease
        )
        AdjustmentInstructionRow(
            sequenceLabel = "L1 R1",
            gestureHint = uiStrings.guidedSelectEnterHint,
            title = when (adjustMode) {
                GuidedPreferencesAdjustMode.ResponseTime -> uiStrings.guidedSaveResponseTime
                GuidedPreferencesAdjustMode.Sensitivity -> uiStrings.guidedSaveSensitivity
                else -> uiStrings.guidedSaveSelectedValue
            },
            onClick = onSave
        )
        AdjustmentInstructionRow(
            sequenceLabel = "L2 R2",
            gestureHint = uiStrings.guidedBackHint,
            title = uiStrings.guidedCancelToPreferences,
            onClick = onCancel
        )
        AdjustmentInstructionRow(
            sequenceLabel = "L4 R4",
            gestureHint = uiStrings.guidedCategoriesNavTitle,
            title = uiStrings.guidedCategoriesNavTitle,
            onClick = onCategories
        )
        AdjustmentInstructionRow(
            sequenceLabel = "L6 R0",
            gestureHint = uiStrings.guidedEmergencyNavTitle,
            title = uiStrings.guidedEmergencyNavTitle,
            onClick = onEmergency,
            emergency = true
        )
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
