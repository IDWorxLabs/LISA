package com.idworx.lisa

/**
 * Guided Navigation Architecture V1 — Communication Workspace layer.
 *
 * Workspace global gestures: L2 R0, L0 R2, L1 R1 (select), L2 R2 (back), L4 R4, L6 R0.
 * Brain 1 decision gestures (L2/R2 choice, L1 R1 confirm, R1 L1 cancel) apply only outside
 * workspace vocabulary contexts — see [com.idworx.lisa.features.experiencepolish.communicationworkspace.WorkspaceGestureLayers].
 * Adjustment-only gestures: L3 R1 decrease, L1 R3 increase.
 * Vocabulary uses short reusable page-local slots from [GuidedPageSequences].
 */
object GuidedModeNavigation {
    const val PREVIOUS_LEFT = 2
    const val PREVIOUS_RIGHT = 0
    const val NEXT_LEFT = 0
    const val NEXT_RIGHT = 2
    const val SELECT_LEFT = 1
    const val SELECT_RIGHT = 1
    const val BACK_LEFT = 2
    const val BACK_RIGHT = 2
    const val CATEGORIES_LEFT = 4
    const val CATEGORIES_RIGHT = 4
    const val DECREASE_VALUE_LEFT = 3
    const val DECREASE_VALUE_RIGHT = 1
    const val INCREASE_VALUE_LEFT = 1
    const val INCREASE_VALUE_RIGHT = 3

    fun isPreviousSequence(left: Int, right: Int): Boolean =
        left == PREVIOUS_LEFT && right == PREVIOUS_RIGHT

    fun isNextSequence(left: Int, right: Int): Boolean =
        left == NEXT_LEFT && right == NEXT_RIGHT

    fun isSelectSequence(left: Int, right: Int): Boolean =
        left == SELECT_LEFT && right == SELECT_RIGHT

    fun isBackSequence(left: Int, right: Int): Boolean =
        left == BACK_LEFT && right == BACK_RIGHT

    fun isCategoriesSequence(left: Int, right: Int): Boolean =
        left == CATEGORIES_LEFT && right == CATEGORIES_RIGHT

    fun isDecreaseValueSequence(left: Int, right: Int): Boolean =
        left == DECREASE_VALUE_LEFT && right == DECREASE_VALUE_RIGHT

    fun isIncreaseValueSequence(left: Int, right: Int): Boolean =
        left == INCREASE_VALUE_LEFT && right == INCREASE_VALUE_RIGHT

    fun isGlobalNavigationSequence(left: Int, right: Int): Boolean =
        isPreviousSequence(left, right) ||
            isNextSequence(left, right) ||
            isSelectSequence(left, right) ||
            isBackSequence(left, right) ||
            isCategoriesSequence(left, right)

    /** @deprecated Use [isPreviousSequence] */
    fun isPreviousPageSequence(left: Int, right: Int): Boolean = isPreviousSequence(left, right)

    /** @deprecated Use [isNextSequence] */
    fun isNextPageSequence(left: Int, right: Int): Boolean = isNextSequence(left, right)

    /** @deprecated Use [isGlobalNavigationSequence] */
    fun isNavigationSequence(left: Int, right: Int): Boolean =
        isPreviousSequence(left, right) || isNextSequence(left, right)
}

/** Reusable per-page vocabulary sequences — same slots on every category page. */
object GuidedPageSequences {
    val slots: List<Pair<Int, Int>> = listOf(
        2 to 1,
        1 to 2,
        3 to 1,
        1 to 3,
        3 to 2,
        2 to 3,
        3 to 3,
        4 to 1,
        1 to 4,
        4 to 2
    )

    fun leftAt(slotIndex: Int): Int = slotAt(slotIndex).first
    fun rightAt(slotIndex: Int): Int = slotAt(slotIndex).second

    fun slotAt(slotIndex: Int): Pair<Int, Int> =
        if (slotIndex < slots.size) {
            slots[slotIndex]
        } else {
            extendedSlots[slotIndex - slots.size]
        }

    /** Extra short sequences for categories with more than [slots] entries (e.g. Preferences). */
    val extendedSlots: List<Pair<Int, Int>> = listOf(
        5 to 1,
        1 to 5,
        5 to 2,
        2 to 4,
        4 to 3,
        3 to 4,
        5 to 3,
        1 to 6,
        6 to 1,
        2 to 5
    )

    fun allSlotsForEntryCount(count: Int): List<Pair<Int, Int>> =
        (0 until count).map { slotAt(it) }

    val forbiddenForVocabulary: Set<Pair<Int, Int>> = setOf(
        1 to 0,
        0 to 1,
        GuidedModeNavigation.PREVIOUS_LEFT to GuidedModeNavigation.PREVIOUS_RIGHT,
        GuidedModeNavigation.NEXT_LEFT to GuidedModeNavigation.NEXT_RIGHT,
        GuidedModeNavigation.SELECT_LEFT to GuidedModeNavigation.SELECT_RIGHT,
        GuidedModeNavigation.BACK_LEFT to GuidedModeNavigation.BACK_RIGHT,
        GuidedModeNavigation.CATEGORIES_LEFT to GuidedModeNavigation.CATEGORIES_RIGHT,
        EMERGENCY_LEFT_WINKS to EMERGENCY_RIGHT_WINKS
    )
}

/** Direct gesture shortcuts for the six top-level categories in Category Menu mode. */
object GuidedCategoryShortcuts {
    const val SHORTCUT_COUNT: Int = GuidedVocabularyCategory.PAGE_COUNT

    fun gestureForCategory(categoryIndex: Int): Pair<Int, Int> =
        GuidedPageSequences.slotAt(categoryIndex.coerceIn(0, SHORTCUT_COUNT - 1))

    fun sequenceLabelForCategory(categoryIndex: Int): String {
        val (left, right) = gestureForCategory(categoryIndex)
        return formatWinkSequenceShort(left, right)
    }

    fun categoryIndexForGesture(left: Int, right: Int): Int? {
        for (index in 0 until SHORTCUT_COUNT) {
            val (slotLeft, slotRight) = gestureForCategory(index)
            if (slotLeft == left && slotRight == right) return index
        }
        return null
    }

    fun allGestures(): List<Pair<Int, Int>> =
        (0 until SHORTCUT_COUNT).map { gestureForCategory(it) }

    fun doNotConflictWithGlobalNavigation(): Boolean =
        allGestures().none { (left, right) ->
            GuidedModeNavigation.isGlobalNavigationSequence(left, right) ||
                isEmergencySequence(left, right)
        }
}

enum class GuidedOverlayScreenMode {
    Vocabulary,
    CategoryMenu
}

enum class GuidedPreferencesAdjustMode {
    None,
    ResponseTime,
    Sensitivity
}

enum class GuidedVocabularyCategory {
    Conversation,
    BasicNeeds,
    Medical,
    Family,
    BasicSystemControls,
    Preferences;

    companion object {
        val ordered: List<GuidedVocabularyCategory> = entries.toList()
        const val PAGE_COUNT: Int = 6
        const val STANDARD_ENTRIES_PER_PAGE: Int = 10
        const val PREFERENCES_CATEGORY_INDEX: Int = 5
    }
}

enum class GuidedEntryKind {
    SpeakPhrase,
    SystemAction
}

enum class GuidedOverlayAction {
    RepeatLastPhrase,
    DecreaseSensitivity,
    IncreaseSensitivity,
    SetSpeedFast,
    SetSpeedSlow,
    TogglePauseListening,
    OpenMenu,
    ResetSequence,
    ShowHelp,
    ShowCurrentResponseTime,
    ShowCurrentSensitivity,
    OpenAdjustResponseTime,
    OpenAdjustSensitivity
}

data class GuidedNavigationState(
    val screenMode: GuidedOverlayScreenMode = GuidedOverlayScreenMode.Vocabulary,
    val categoryIndex: Int = 0,
    val categoryMenuSelection: Int = 0,
    val phrasePageIndex: Int = 0,
    val preferencesAdjustMode: GuidedPreferencesAdjustMode = GuidedPreferencesAdjustMode.None,
    val draftResponseTimeSec: Int = SequenceProcessingDelay.DEFAULT_SECONDS,
    val draftSensitivityLevel: Int = DEFAULT_SENSITIVITY_LEVEL,
    val adjustmentScrollStep: Int = 0
) {
    fun normalized(): GuidedNavigationState = copy(
        categoryIndex = categoryIndex.coerceIn(0, GuidedVocabularyCategory.PAGE_COUNT - 1),
        categoryMenuSelection = categoryMenuSelection.coerceIn(0, GuidedVocabularyCategory.PAGE_COUNT - 1),
        phrasePageIndex = phrasePageIndex.coerceAtLeast(0),
        draftResponseTimeSec = SequenceProcessingDelay.coerce(draftResponseTimeSec),
        draftSensitivityLevel = draftSensitivityLevel.coerceIn(MIN_SENSITIVITY_LEVEL, MAX_SENSITIVITY_LEVEL),
        adjustmentScrollStep = adjustmentScrollStep.coerceAtLeast(0)
    )

    val isPreferencesAdjustmentActive: Boolean
        get() = preferencesAdjustMode != GuidedPreferencesAdjustMode.None

    fun displayResponseTimeSec(savedSec: Int): Int = when (preferencesAdjustMode) {
        GuidedPreferencesAdjustMode.ResponseTime -> SequenceProcessingDelay.coerce(draftResponseTimeSec)
        else -> SequenceProcessingDelay.coerce(savedSec)
    }

    fun displaySensitivityLevel(savedLevel: Int): Int = when (preferencesAdjustMode) {
        GuidedPreferencesAdjustMode.Sensitivity -> draftSensitivityLevel.coerceIn(MIN_SENSITIVITY_LEVEL, MAX_SENSITIVITY_LEVEL)
        else -> savedLevel.coerceIn(MIN_SENSITIVITY_LEVEL, MAX_SENSITIVITY_LEVEL)
    }
}

data class GuidedVocabularyEntry(
    val left: Int,
    val right: Int,
    val phrase: String,
    val englishSubtitle: String? = null,
    val kind: GuidedEntryKind = GuidedEntryKind.SpeakPhrase,
    val systemAction: SystemCommandAction? = null,
    val guidedAction: GuidedOverlayAction? = null,
    val preferenceValue: Int? = null,
    val slotIndex: Int = 0
) {
    val sequenceLabel: String get() = formatWinkSequenceShort(left, right)
}

data class GuidedCategoryPage(
    val category: GuidedVocabularyCategory,
    val title: String,
    val entries: List<GuidedVocabularyEntry>
)

data class GuidedNavPanelAction(
    val symbol: String,
    val title: String,
    val gestureHint: String,
    val sequenceLabel: String
)

object GuidedNavigationPanelSpec {

    enum class PanelContext {
        Vocabulary,
        CategoryMenu,
        Adjustment
    }

    fun panelActions(uiStrings: LisaUiStrings, context: PanelContext): List<GuidedNavPanelAction> {
        val scrollUpTitle = when (context) {
            PanelContext.Vocabulary -> uiStrings.guidedPreviousPhrasePage
            PanelContext.CategoryMenu -> uiStrings.guidedMoveUpCategory
            PanelContext.Adjustment -> uiStrings.guidedScrollUp
        }
        val selectTitle = when (context) {
            PanelContext.Vocabulary -> uiStrings.guidedSelectEnter
            PanelContext.CategoryMenu -> uiStrings.guidedOpenSelectedCategory
            PanelContext.Adjustment -> uiStrings.guidedSaveSelectedValue
        }
        val backTitle = when (context) {
            PanelContext.Vocabulary -> uiStrings.guidedBack
            PanelContext.CategoryMenu -> uiStrings.guidedBackToPhrases
            PanelContext.Adjustment -> uiStrings.guidedCancelAdjustment
        }
        val scrollDownTitle = when (context) {
            PanelContext.Vocabulary -> uiStrings.guidedNextPhrasePage
            PanelContext.CategoryMenu -> uiStrings.guidedMoveDownCategory
            PanelContext.Adjustment -> uiStrings.guidedScrollDown
        }
        return listOf(
            GuidedNavPanelAction("↑↑", scrollUpTitle, uiStrings.guidedScrollUpHint, "L2 R0"),
            GuidedNavPanelAction("✅", selectTitle, uiStrings.guidedSelectEnterHint, "L1 R1"),
            GuidedNavPanelAction("↩", backTitle, uiStrings.guidedBackHint, "L2 R2"),
            GuidedNavPanelAction("☰", uiStrings.guidedCategoriesNavTitle, uiStrings.guidedCategoriesNavHint, "L4 R4"),
            GuidedNavPanelAction("🚨", uiStrings.guidedEmergencyNavTitle, uiStrings.guidedEmergencyNavHint, "L6 R0"),
            GuidedNavPanelAction("↓↓", scrollDownTitle, uiStrings.guidedScrollDownHint, "L0 R2")
        )
    }

    fun vocabularyModeActions(uiStrings: LisaUiStrings): List<GuidedNavPanelAction> =
        panelActions(uiStrings, PanelContext.Vocabulary)

    fun categoryMenuModeActions(uiStrings: LisaUiStrings): List<GuidedNavPanelAction> =
        panelActions(uiStrings, PanelContext.CategoryMenu)

    fun adjustmentModeActions(uiStrings: LisaUiStrings): List<GuidedNavPanelAction> =
        panelActions(uiStrings, PanelContext.Adjustment)

    fun allActionsLabeled(actions: List<GuidedNavPanelAction>): Boolean =
        actions.all { action ->
            action.title.isNotBlank() &&
                action.gestureHint.isNotBlank() &&
                action.sequenceLabel.isNotBlank() &&
                action.symbol.isNotBlank()
        }
}

/** Documents touch targets that must mirror eye-gesture handlers in the guided overlay UI. */
object GuidedTouchNavigationSpec {
    val panelGestures: List<Pair<Int, Int>> = listOf(
        GuidedModeNavigation.PREVIOUS_LEFT to GuidedModeNavigation.PREVIOUS_RIGHT,
        GuidedModeNavigation.SELECT_LEFT to GuidedModeNavigation.SELECT_RIGHT,
        GuidedModeNavigation.BACK_LEFT to GuidedModeNavigation.BACK_RIGHT,
        GuidedModeNavigation.CATEGORIES_LEFT to GuidedModeNavigation.CATEGORIES_RIGHT,
        EMERGENCY_LEFT_WINKS to EMERGENCY_RIGHT_WINKS,
        GuidedModeNavigation.NEXT_LEFT to GuidedModeNavigation.NEXT_RIGHT
    )

    val adjustmentPanelGestures: List<Pair<Int, Int>> = listOf(
        GuidedModeNavigation.DECREASE_VALUE_LEFT to GuidedModeNavigation.DECREASE_VALUE_RIGHT,
        GuidedModeNavigation.INCREASE_VALUE_LEFT to GuidedModeNavigation.INCREASE_VALUE_RIGHT
    ) + panelGestures

    fun touchMirrorsEyeGesture(left: Int, right: Int): Boolean =
        panelGestures.any { it.first == left && it.second == right } ||
            adjustmentPanelGestures.any { it.first == left && it.second == right } ||
            GuidedPageSequences.slots.any { it.first == left && it.second == right } ||
            GuidedPageSequences.extendedSlots.any { it.first == left && it.second == right }
}

data class GuidedCatalogContext(
    val responseTimeSec: Int = SequenceProcessingDelay.DEFAULT_SECONDS,
    val sensitivityLevel: Int = DEFAULT_SENSITIVITY_LEVEL
)

sealed class GuidedSequenceResult {
    data class Navigate(val newState: GuidedNavigationState) : GuidedSequenceResult()
    data class Speak(val entry: GuidedVocabularyEntry) : GuidedSequenceResult()
    data class SystemAction(val entry: GuidedVocabularyEntry) : GuidedSequenceResult()
    data class SavePreferencesAdjustment(
        val newState: GuidedNavigationState,
        val responseTimeSec: Int? = null,
        val sensitivityLevel: Int? = null
    ) : GuidedSequenceResult()
    object Unmatched : GuidedSequenceResult()
}

object PreferenceAdjustmentBarSpec {
    fun responseTimeValues(): List<Int> = SequenceProcessingDelay.allowedSeconds.toList()

    fun sensitivityValues(): List<Int> = (MIN_SENSITIVITY_LEVEL..MAX_SENSITIVITY_LEVEL).toList()

    fun formatResponseTimeTick(seconds: Int): String = "${seconds}s"

    fun formatSensitivityTick(level: Int): String = level.toString()

    fun isHighlighted(tickValue: Int, draftValue: Int): Boolean = tickValue == draftValue
}

object PreferenceAdjustmentController {

    fun openResponseTimeAdjust(state: GuidedNavigationState, currentSec: Int): GuidedNavigationState =
        state.normalized().copy(
            preferencesAdjustMode = GuidedPreferencesAdjustMode.ResponseTime,
            draftResponseTimeSec = SequenceProcessingDelay.coerce(currentSec),
            adjustmentScrollStep = 0
        )

    fun openSensitivityAdjust(state: GuidedNavigationState, currentLevel: Int): GuidedNavigationState =
        state.normalized().copy(
            preferencesAdjustMode = GuidedPreferencesAdjustMode.Sensitivity,
            draftSensitivityLevel = currentLevel.coerceIn(MIN_SENSITIVITY_LEVEL, MAX_SENSITIVITY_LEVEL),
            adjustmentScrollStep = 0
        )

    fun decreaseDraft(state: GuidedNavigationState): GuidedNavigationState = when (state.preferencesAdjustMode) {
        GuidedPreferencesAdjustMode.ResponseTime -> state.copy(
            draftResponseTimeSec = SequenceProcessingDelay.coerce(state.draftResponseTimeSec - 1)
        )
        GuidedPreferencesAdjustMode.Sensitivity -> state.copy(
            draftSensitivityLevel = (state.draftSensitivityLevel - 1)
                .coerceIn(MIN_SENSITIVITY_LEVEL, MAX_SENSITIVITY_LEVEL)
        )
        GuidedPreferencesAdjustMode.None -> state
    }

    fun increaseDraft(state: GuidedNavigationState): GuidedNavigationState = when (state.preferencesAdjustMode) {
        GuidedPreferencesAdjustMode.ResponseTime -> state.copy(
            draftResponseTimeSec = SequenceProcessingDelay.coerce(state.draftResponseTimeSec + 1)
        )
        GuidedPreferencesAdjustMode.Sensitivity -> state.copy(
            draftSensitivityLevel = (state.draftSensitivityLevel + 1)
                .coerceIn(MIN_SENSITIVITY_LEVEL, MAX_SENSITIVITY_LEVEL)
        )
        GuidedPreferencesAdjustMode.None -> state
    }

    fun cancelAdjustment(state: GuidedNavigationState): GuidedNavigationState =
        state.copy(preferencesAdjustMode = GuidedPreferencesAdjustMode.None)

    fun saveAdjustment(state: GuidedNavigationState): GuidedSequenceResult.SavePreferencesAdjustment =
        when (state.preferencesAdjustMode) {
            GuidedPreferencesAdjustMode.ResponseTime -> GuidedSequenceResult.SavePreferencesAdjustment(
                newState = cancelAdjustment(state),
                responseTimeSec = SequenceProcessingDelay.coerce(state.draftResponseTimeSec)
            )
            GuidedPreferencesAdjustMode.Sensitivity -> GuidedSequenceResult.SavePreferencesAdjustment(
                newState = cancelAdjustment(state),
                sensitivityLevel = state.draftSensitivityLevel.coerceIn(MIN_SENSITIVITY_LEVEL, MAX_SENSITIVITY_LEVEL)
            )
            GuidedPreferencesAdjustMode.None -> GuidedSequenceResult.SavePreferencesAdjustment(
                newState = state
            )
        }
}

object GuidedNavigationController {

    fun phrasePageCount(entryCount: Int, visibleCap: Int): Int {
        if (entryCount <= 0) return 1
        return (entryCount + visibleCap - 1) / visibleCap
    }

    fun visiblePhraseEntries(
        entries: List<GuidedVocabularyEntry>,
        phrasePageIndex: Int,
        visibleCap: Int
    ): List<GuidedVocabularyEntry> {
        if (entries.isEmpty()) return emptyList()
        val start = phrasePageIndex.coerceAtLeast(0) * visibleCap
        if (start >= entries.size) return emptyList()
        return entries.subList(start, minOf(start + visibleCap, entries.size))
    }

    fun openCategoryMenu(state: GuidedNavigationState): GuidedNavigationState =
        state.normalized().copy(
            screenMode = GuidedOverlayScreenMode.CategoryMenu,
            categoryMenuSelection = state.categoryIndex
        )

    fun closeCategoryMenu(state: GuidedNavigationState): GuidedNavigationState =
        state.normalized().copy(screenMode = GuidedOverlayScreenMode.Vocabulary)

    fun moveCategorySelectionUp(state: GuidedNavigationState): GuidedNavigationState {
        val normalized = state.normalized()
        return normalized.copy(
            categoryMenuSelection = (normalized.categoryMenuSelection - 1)
                .coerceAtLeast(0)
        )
    }

    fun moveCategorySelectionDown(state: GuidedNavigationState): GuidedNavigationState {
        val normalized = state.normalized()
        return normalized.copy(
            categoryMenuSelection = (normalized.categoryMenuSelection + 1)
                .coerceAtMost(GuidedVocabularyCategory.PAGE_COUNT - 1)
        )
    }

    fun openSelectedCategory(state: GuidedNavigationState): GuidedNavigationState {
        val normalized = state.normalized()
        return normalized.copy(
            screenMode = GuidedOverlayScreenMode.Vocabulary,
            categoryIndex = normalized.categoryMenuSelection,
            phrasePageIndex = 0
        )
    }

    fun openCategoryDirectly(state: GuidedNavigationState, categoryIndex: Int): GuidedNavigationState =
        state.normalized().copy(
            screenMode = GuidedOverlayScreenMode.Vocabulary,
            categoryIndex = categoryIndex.coerceIn(0, GuidedVocabularyCategory.PAGE_COUNT - 1),
            categoryMenuSelection = categoryIndex.coerceIn(0, GuidedVocabularyCategory.PAGE_COUNT - 1),
            phrasePageIndex = 0,
            preferencesAdjustMode = GuidedPreferencesAdjustMode.None,
            adjustmentScrollStep = 0
        )

    fun openCategoryMenuEscapingAdjustment(state: GuidedNavigationState): GuidedNavigationState =
        openCategoryMenu(PreferenceAdjustmentController.cancelAdjustment(state))

    fun scrollAdjustmentContentUp(state: GuidedNavigationState): GuidedNavigationState {
        val normalized = state.normalized()
        return normalized.copy(
            adjustmentScrollStep = (normalized.adjustmentScrollStep - 1).coerceAtLeast(0)
        )
    }

    fun scrollAdjustmentContentDown(state: GuidedNavigationState): GuidedNavigationState {
        val normalized = state.normalized()
        return normalized.copy(adjustmentScrollStep = normalized.adjustmentScrollStep + 1)
    }

    fun movePhrasePagePrevious(state: GuidedNavigationState): GuidedNavigationState {
        val normalized = state.normalized()
        return normalized.copy(
            phrasePageIndex = (normalized.phrasePageIndex - 1).coerceAtLeast(0)
        )
    }

    fun movePhrasePageNext(
        state: GuidedNavigationState,
        entryCount: Int,
        visibleCap: Int
    ): GuidedNavigationState {
        val normalized = state.normalized()
        val maxPage = phrasePageCount(entryCount, visibleCap) - 1
        return normalized.copy(
            phrasePageIndex = (normalized.phrasePageIndex + 1).coerceAtMost(maxPage)
        )
    }

    fun processSequence(
        left: Int,
        right: Int,
        state: GuidedNavigationState,
        language: PreferredLanguage,
        uiStrings: LisaUiStrings,
        visibleEntryCap: Int = GuidedVocabularyCatalog.DEFAULT_VISIBLE_ENTRY_CAP,
        catalogContext: GuidedCatalogContext = GuidedCatalogContext()
    ): GuidedSequenceResult {
        val normalized = state.normalized()

        if (normalized.isPreferencesAdjustmentActive) {
            return processPreferencesAdjustmentGesture(left, right, normalized)
        }

        val currentPage = GuidedVocabularyCatalog.categoryAt(
            pageIndex = normalized.categoryIndex,
            language = language,
            uiStrings = uiStrings,
            catalogContext = catalogContext
        )

        return when (normalized.screenMode) {
            GuidedOverlayScreenMode.CategoryMenu -> processCategoryMenuGesture(
                left = left,
                right = right,
                state = normalized
            )
            GuidedOverlayScreenMode.Vocabulary -> processVocabularyGesture(
                left = left,
                right = right,
                state = normalized,
                pageIndex = normalized.categoryIndex,
                language = language,
                uiStrings = uiStrings,
                catalogContext = catalogContext,
                entryCount = currentPage?.entries?.size ?: 0,
                visibleEntryCap = visibleEntryCap
            )
        }
    }

    private fun processPreferencesAdjustmentGesture(
        left: Int,
        right: Int,
        state: GuidedNavigationState
    ): GuidedSequenceResult = when {
        GuidedModeNavigation.isPreviousSequence(left, right) ->
            GuidedSequenceResult.Navigate(scrollAdjustmentContentUp(state))
        GuidedModeNavigation.isNextSequence(left, right) ->
            GuidedSequenceResult.Navigate(scrollAdjustmentContentDown(state))
        GuidedModeNavigation.isDecreaseValueSequence(left, right) ->
            GuidedSequenceResult.Navigate(PreferenceAdjustmentController.decreaseDraft(state))
        GuidedModeNavigation.isIncreaseValueSequence(left, right) ->
            GuidedSequenceResult.Navigate(PreferenceAdjustmentController.increaseDraft(state))
        GuidedModeNavigation.isSelectSequence(left, right) ->
            PreferenceAdjustmentController.saveAdjustment(state)
        GuidedModeNavigation.isBackSequence(left, right) ->
            GuidedSequenceResult.Navigate(PreferenceAdjustmentController.cancelAdjustment(state))
        GuidedModeNavigation.isCategoriesSequence(left, right) ->
            GuidedSequenceResult.Navigate(openCategoryMenuEscapingAdjustment(state))
        else -> GuidedSequenceResult.Unmatched
    }

    private fun processCategoryMenuGesture(
        left: Int,
        right: Int,
        state: GuidedNavigationState
    ): GuidedSequenceResult = when {
        GuidedModeNavigation.isPreviousSequence(left, right) ->
            GuidedSequenceResult.Navigate(moveCategorySelectionUp(state))
        GuidedModeNavigation.isNextSequence(left, right) ->
            GuidedSequenceResult.Navigate(moveCategorySelectionDown(state))
        GuidedModeNavigation.isSelectSequence(left, right) ->
            GuidedSequenceResult.Navigate(openSelectedCategory(state))
        GuidedModeNavigation.isBackSequence(left, right) ->
            GuidedSequenceResult.Navigate(closeCategoryMenu(state))
        GuidedModeNavigation.isCategoriesSequence(left, right) ->
            GuidedSequenceResult.Navigate(state.normalized())
        else -> GuidedCategoryShortcuts.categoryIndexForGesture(left, right)?.let { categoryIndex ->
            GuidedSequenceResult.Navigate(openCategoryDirectly(state, categoryIndex))
        } ?: GuidedSequenceResult.Unmatched
    }

    private fun processVocabularyGesture(
        left: Int,
        right: Int,
        state: GuidedNavigationState,
        pageIndex: Int,
        language: PreferredLanguage,
        uiStrings: LisaUiStrings,
        catalogContext: GuidedCatalogContext,
        entryCount: Int,
        visibleEntryCap: Int
    ): GuidedSequenceResult = when {
        GuidedModeNavigation.isPreviousSequence(left, right) ->
            GuidedSequenceResult.Navigate(movePhrasePagePrevious(state))
        GuidedModeNavigation.isNextSequence(left, right) ->
            GuidedSequenceResult.Navigate(movePhrasePageNext(state, entryCount, visibleEntryCap))
        GuidedModeNavigation.isCategoriesSequence(left, right) ->
            GuidedSequenceResult.Navigate(openCategoryMenu(state))
        GuidedModeNavigation.isBackSequence(left, right) ->
            GuidedSequenceResult.Unmatched
        else -> {
            val match = GuidedVocabularyCatalog.findMatchOnVisiblePage(
                left = left,
                right = right,
                pageIndex = pageIndex,
                phrasePageIndex = state.phrasePageIndex,
                language = language,
                uiStrings = uiStrings,
                catalogContext = catalogContext,
                visibleEntryCap = visibleEntryCap
            )
            when {
                match == null -> GuidedSequenceResult.Unmatched
                match.guidedAction == GuidedOverlayAction.OpenAdjustResponseTime ->
                    GuidedSequenceResult.Navigate(
                        PreferenceAdjustmentController.openResponseTimeAdjust(state, catalogContext.responseTimeSec)
                    )
                match.guidedAction == GuidedOverlayAction.OpenAdjustSensitivity ->
                    GuidedSequenceResult.Navigate(
                        PreferenceAdjustmentController.openSensitivityAdjust(state, catalogContext.sensitivityLevel)
                    )
                match.kind == GuidedEntryKind.SystemAction -> GuidedSequenceResult.SystemAction(match)
                else -> GuidedSequenceResult.Speak(match)
            }
        }
    }
}

object GuidedVocabularyCatalog {

    const val DEFAULT_VISIBLE_ENTRY_CAP: Int = 6

    private data class CatalogSpec(
        val category: GuidedVocabularyCategory,
        val entries: List<CatalogEntrySpec>
    )

    private sealed class CatalogEntrySpec {
        data class CoreVocabulary(val vocabularyId: String) : CatalogEntrySpec()
        data class Phrase(val phraseKey: String) : CatalogEntrySpec()
        data class System(
            val labelKey: String,
            val systemAction: SystemCommandAction? = null,
            val guidedAction: GuidedOverlayAction? = null
        ) : CatalogEntrySpec()

        data class Preference(
            val labelKey: String,
            val guidedAction: GuidedOverlayAction,
            val value: Int? = null
        ) : CatalogEntrySpec()
    }

    private val catalog: List<CatalogSpec> = listOf(
        CatalogSpec(
            category = GuidedVocabularyCategory.Conversation,
            entries = listOf(
                CatalogEntrySpec.CoreVocabulary("yes"),
                CatalogEntrySpec.CoreVocabulary("no"),
                CatalogEntrySpec.CoreVocabulary("thank_you"),
                CatalogEntrySpec.CoreVocabulary("i_love_you"),
                CatalogEntrySpec.CoreVocabulary("please"),
                CatalogEntrySpec.Phrase("stop"),
                CatalogEntrySpec.Phrase("i_am_okay"),
                CatalogEntrySpec.CoreVocabulary("i_need_help"),
                CatalogEntrySpec.Phrase("please_repeat_that"),
                CatalogEntrySpec.CoreVocabulary("i_dont_understand")
            )
        ),
        CatalogSpec(
            category = GuidedVocabularyCategory.BasicNeeds,
            entries = listOf(
                CatalogEntrySpec.Phrase("i_need_some_water"),
                CatalogEntrySpec.Phrase("i_am_hungry"),
                CatalogEntrySpec.Phrase("i_need_to_use_the_bathroom"),
                CatalogEntrySpec.Phrase("i_am_tired"),
                CatalogEntrySpec.Phrase("i_am_cold"),
                CatalogEntrySpec.Phrase("i_am_hot"),
                CatalogEntrySpec.Phrase("please_help_me_move"),
                CatalogEntrySpec.Phrase("please_reposition_me"),
                CatalogEntrySpec.Phrase("i_am_uncomfortable"),
                CatalogEntrySpec.Phrase("i_would_like_some_privacy")
            )
        ),
        CatalogSpec(
            category = GuidedVocabularyCategory.Medical,
            entries = listOf(
                CatalogEntrySpec.Phrase("i_am_in_pain"),
                CatalogEntrySpec.Phrase("please_call_the_nurse"),
                CatalogEntrySpec.Phrase("i_need_my_medication"),
                CatalogEntrySpec.Phrase("please_help_me_now"),
                CatalogEntrySpec.Phrase("i_am_having_difficulty_breathing"),
                CatalogEntrySpec.Phrase("i_feel_dizzy"),
                CatalogEntrySpec.Phrase("my_chest_hurts"),
                CatalogEntrySpec.Phrase("i_do_not_feel_well"),
                CatalogEntrySpec.Phrase("please_call_the_doctor"),
                CatalogEntrySpec.Phrase("please_check_my_body")
            )
        ),
        CatalogSpec(
            category = GuidedVocabularyCategory.Family,
            entries = listOf(
                CatalogEntrySpec.Phrase("i_want_to_see_my_mom"),
                CatalogEntrySpec.Phrase("i_want_to_see_my_dad"),
                CatalogEntrySpec.Phrase("i_want_to_see_my_wife"),
                CatalogEntrySpec.Phrase("i_want_to_see_my_husband"),
                CatalogEntrySpec.Phrase("i_want_to_see_my_child"),
                CatalogEntrySpec.Phrase("i_want_to_see_my_friend"),
                CatalogEntrySpec.Phrase("call_my_family"),
                CatalogEntrySpec.Phrase("i_miss_you"),
                CatalogEntrySpec.Phrase("stay_with_me"),
                CatalogEntrySpec.Phrase("i_want_to_talk")
            )
        ),
        CatalogSpec(
            category = GuidedVocabularyCategory.BasicSystemControls,
            entries = listOf(
                CatalogEntrySpec.System("repeat_last", systemAction = SystemCommandAction.RepeatLastPhrase),
                CatalogEntrySpec.System("increase_volume", guidedAction = GuidedOverlayAction.IncreaseSensitivity),
                CatalogEntrySpec.System("decrease_volume", systemAction = SystemCommandAction.DecreaseSensitivity),
                CatalogEntrySpec.System("slower_speech", systemAction = SystemCommandAction.SetSpeedSlow),
                CatalogEntrySpec.System("faster_speech", systemAction = SystemCommandAction.SetSpeedFast),
                CatalogEntrySpec.System("pause_listening", guidedAction = GuidedOverlayAction.TogglePauseListening),
                CatalogEntrySpec.System("resume_listening", guidedAction = GuidedOverlayAction.TogglePauseListening),
                CatalogEntrySpec.System("open_menu", guidedAction = GuidedOverlayAction.OpenMenu),
                CatalogEntrySpec.System("reset_sequence", guidedAction = GuidedOverlayAction.ResetSequence),
                CatalogEntrySpec.System("help", guidedAction = GuidedOverlayAction.ShowHelp)
            )
        ),
        CatalogSpec(
            category = GuidedVocabularyCategory.Preferences,
            entries = listOf(
                CatalogEntrySpec.Preference("current_response_time", GuidedOverlayAction.ShowCurrentResponseTime),
                CatalogEntrySpec.Preference("adjust_response_time", GuidedOverlayAction.OpenAdjustResponseTime),
                CatalogEntrySpec.Preference("current_sensitivity", GuidedOverlayAction.ShowCurrentSensitivity),
                CatalogEntrySpec.Preference("adjust_sensitivity", GuidedOverlayAction.OpenAdjustSensitivity)
            )
        )
    )

    fun buildPages(
        language: PreferredLanguage,
        uiStrings: LisaUiStrings,
        catalogContext: GuidedCatalogContext = GuidedCatalogContext()
    ): List<GuidedCategoryPage> =
        catalog.map { spec ->
            GuidedCategoryPage(
                category = spec.category,
                title = uiStrings.guidedCategoryTitle(spec.category),
                entries = spec.entries.mapIndexedNotNull { index, entrySpec ->
                    resolveEntry(
                        spec = entrySpec,
                        slotIndex = index,
                        language = language,
                        uiStrings = uiStrings,
                        catalogContext = catalogContext
                    )
                }
            )
        }

    fun categoryAt(
        pageIndex: Int,
        language: PreferredLanguage,
        uiStrings: LisaUiStrings,
        catalogContext: GuidedCatalogContext = GuidedCatalogContext()
    ): GuidedCategoryPage? =
        buildPages(language, uiStrings, catalogContext)
            .getOrNull(pageIndex.coerceIn(0, GuidedVocabularyCategory.PAGE_COUNT - 1))

    fun categoryMenuTitles(uiStrings: LisaUiStrings): List<String> =
        GuidedVocabularyCategory.ordered.map { uiStrings.guidedCategoryTitle(it) }

    fun currentPageEntries(
        pageIndex: Int,
        language: PreferredLanguage,
        uiStrings: LisaUiStrings,
        catalogContext: GuidedCatalogContext = GuidedCatalogContext()
    ): List<GuidedVocabularyEntry> =
        categoryAt(pageIndex, language, uiStrings, catalogContext)?.entries.orEmpty()

    fun findMatchOnCurrentPage(
        left: Int,
        right: Int,
        pageIndex: Int,
        language: PreferredLanguage,
        uiStrings: LisaUiStrings,
        catalogContext: GuidedCatalogContext = GuidedCatalogContext()
    ): GuidedVocabularyEntry? =
        currentPageEntries(pageIndex, language, uiStrings, catalogContext)
            .firstOrNull { it.left == left && it.right == right }

    /** Resolves a phrase only when it appears on the currently visible phrase page. */
    fun findMatchOnVisiblePage(
        left: Int,
        right: Int,
        pageIndex: Int,
        phrasePageIndex: Int,
        language: PreferredLanguage,
        uiStrings: LisaUiStrings,
        catalogContext: GuidedCatalogContext = GuidedCatalogContext(),
        visibleEntryCap: Int = DEFAULT_VISIBLE_ENTRY_CAP
    ): GuidedVocabularyEntry? =
        GuidedNavigationController.visiblePhraseEntries(
            entries = currentPageEntries(pageIndex, language, uiStrings, catalogContext),
            phrasePageIndex = phrasePageIndex,
            visibleCap = visibleEntryCap
        ).firstOrNull { it.left == left && it.right == right }

    fun visibleEntryCount(screenWidthDp: Int, screenHeightDp: Int): Int = when {
        screenWidthDp >= 600 && screenHeightDp >= 640 -> 10
        screenWidthDp >= 400 && screenHeightDp >= 560 -> 8
        else -> DEFAULT_VISIBLE_ENTRY_CAP
    }

    fun allVocabularyPhraseEntries(language: PreferredLanguage, uiStrings: LisaUiStrings): List<GuidedVocabularyEntry> =
        buildPages(language, uiStrings)
            .flatMap { it.entries }
            .filter { it.kind == GuidedEntryKind.SpeakPhrase }

    /** @deprecated Category paging uses Category Menu mode in V1. */
    fun previousPage(currentPage: Int): Int = (currentPage - 1).coerceAtLeast(0)

    /** @deprecated Category paging uses Category Menu mode in V1. */
    fun nextPage(currentPage: Int): Int =
        (currentPage + 1).coerceAtMost(GuidedVocabularyCategory.PAGE_COUNT - 1)

    private fun resolveEntry(
        spec: CatalogEntrySpec,
        slotIndex: Int,
        language: PreferredLanguage,
        uiStrings: LisaUiStrings,
        catalogContext: GuidedCatalogContext
    ): GuidedVocabularyEntry? {
        val left = GuidedPageSequences.leftAt(slotIndex)
        val right = GuidedPageSequences.rightAt(slotIndex)
        return when (spec) {
            is CatalogEntrySpec.CoreVocabulary -> {
                val mapping = defaultLanguageMappings().firstOrNull { it.vocabularyId == spec.vocabularyId }
                    ?: return null
                GuidedVocabularyEntry(
                    left = left,
                    right = right,
                    phrase = mapping.localizedPhrase(language),
                    englishSubtitle = if (language != PreferredLanguage.English && !mapping.isCustom) {
                        LisaCoreVocabulary.text(spec.vocabularyId, PreferredLanguage.English)
                    } else null,
                    kind = GuidedEntryKind.SpeakPhrase,
                    slotIndex = slotIndex
                )
            }
            is CatalogEntrySpec.Phrase -> GuidedVocabularyEntry(
                left = left,
                right = right,
                phrase = uiStrings.guidedPhrase(spec.phraseKey),
                englishSubtitle = uiStrings.guidedPhraseEnglish(spec.phraseKey),
                kind = GuidedEntryKind.SpeakPhrase,
                slotIndex = slotIndex
            )
            is CatalogEntrySpec.System -> GuidedVocabularyEntry(
                left = left,
                right = right,
                phrase = uiStrings.guidedSystemLabel(spec.labelKey),
                englishSubtitle = uiStrings.guidedSystemLabelEnglish(spec.labelKey),
                kind = GuidedEntryKind.SystemAction,
                systemAction = spec.systemAction,
                guidedAction = spec.guidedAction,
                slotIndex = slotIndex
            )
            is CatalogEntrySpec.Preference -> GuidedVocabularyEntry(
                left = left,
                right = right,
                phrase = uiStrings.guidedPreferenceLabel(
                    labelKey = spec.labelKey,
                    responseTimeSec = catalogContext.responseTimeSec,
                    sensitivityLevel = catalogContext.sensitivityLevel,
                    value = spec.value
                ),
                englishSubtitle = uiStrings.guidedPreferenceLabelEnglish(
                    labelKey = spec.labelKey,
                    responseTimeSec = catalogContext.responseTimeSec,
                    sensitivityLevel = catalogContext.sensitivityLevel,
                    value = spec.value
                ),
                kind = GuidedEntryKind.SystemAction,
                guidedAction = spec.guidedAction,
                preferenceValue = spec.value,
                slotIndex = slotIndex
            )
        }
    }
}

/** Validates that no two important actions share the same gesture within a guided mode. */
object GuidedNavigationGestureAudit {

    data class GestureBinding(val left: Int, val right: Int, val action: String)

    fun globalPanelBindings(): List<GestureBinding> = listOf(
        GestureBinding(2, 0, "ScrollUp"),
        GestureBinding(0, 2, "ScrollDown"),
        GestureBinding(1, 1, "SelectSave"),
        GestureBinding(2, 2, "BackCancel"),
        GestureBinding(4, 4, "Categories"),
        GestureBinding(EMERGENCY_LEFT_WINKS, EMERGENCY_RIGHT_WINKS, "Emergency")
    )

    fun adjustmentOnlyBindings(): List<GestureBinding> = listOf(
        GestureBinding(3, 1, "DecreaseValue"),
        GestureBinding(1, 3, "IncreaseValue")
    )

    fun vocabularyModeBindings(pageIndex: Int): List<GestureBinding> {
        val uiStrings = LisaUiStrings.forLanguage(PreferredLanguage.English)
        val entries = GuidedVocabularyCatalog.currentPageEntries(
            pageIndex = pageIndex,
            language = PreferredLanguage.English,
            uiStrings = uiStrings
        )
        return globalPanelBindings() + entries.map { entry ->
            GestureBinding(entry.left, entry.right, "Phrase:${entry.phrase}")
        }
    }

    fun categoryMenuModeBindings(): List<GestureBinding> =
        globalPanelBindings() + GuidedCategoryShortcuts.allGestures().mapIndexed { index, gesture ->
            GestureBinding(gesture.first, gesture.second, "CategoryShortcut:$index")
        }

    fun preferencesPageBindings(): List<GestureBinding> =
        vocabularyModeBindings(GuidedVocabularyCategory.PREFERENCES_CATEGORY_INDEX)

    fun responseTimeAdjustmentBindings(): List<GestureBinding> =
        globalPanelBindings() + adjustmentOnlyBindings()

    fun sensitivityAdjustmentBindings(): List<GestureBinding> =
        globalPanelBindings() + adjustmentOnlyBindings()

    fun noDuplicateGestures(bindings: List<GestureBinding>): Boolean =
        bindings.groupBy { it.left to it.right }.all { it.value.size == 1 }

    fun auditAllModes(): Boolean =
        noDuplicateGestures(vocabularyModeBindings(0)) &&
            noDuplicateGestures(preferencesPageBindings()) &&
            noDuplicateGestures(categoryMenuModeBindings()) &&
            noDuplicateGestures(responseTimeAdjustmentBindings()) &&
            noDuplicateGestures(sensitivityAdjustmentBindings()) &&
            globalPanelBindings().let { globals ->
                noDuplicateGestures(globals) &&
                    globals.size == 6
            }

    fun adjustmentDoesNotUseScrollGesturesForValues(): Boolean {
        val adjustmentBindings = responseTimeAdjustmentBindings()
        return adjustmentBindings.none { (left, right, action) ->
            action == "DecreaseValue" &&
                GuidedModeNavigation.isPreviousSequence(left, right)
        } && adjustmentBindings.none { (left, right, action) ->
            action == "IncreaseValue" &&
                GuidedModeNavigation.isNextSequence(left, right)
        }
    }

    fun everyModeHasBackCategoriesAndEmergency(): Boolean {
        val required = setOf("BackCancel", "Categories", "Emergency")
        return listOf(
            vocabularyModeBindings(0),
            categoryMenuModeBindings(),
            preferencesPageBindings(),
            responseTimeAdjustmentBindings(),
            sensitivityAdjustmentBindings()
        ).all { bindings ->
            required.all { action -> bindings.any { it.action == action } }
        }
    }
}

object GuidedVocabularyOverlayVisibility {
    fun shouldShowOverlay(
        onboardingCompleted: Boolean,
        cameraPermissionGranted: Boolean,
        emergencyActive: Boolean,
        practiceModeOpen: Boolean,
        quickControlsOpen: Boolean
    ): Boolean =
        onboardingCompleted &&
            cameraPermissionGranted &&
            !emergencyActive &&
            !practiceModeOpen &&
            !quickControlsOpen
}

/**
 * Contextual phrase resolution for Communication Workspace — only visible/open options are eligible.
 */
object WorkspacePhraseResolver {

    fun visibleEntriesForState(
        state: GuidedNavigationState,
        language: PreferredLanguage,
        uiStrings: LisaUiStrings,
        catalogContext: GuidedCatalogContext,
        visibleEntryCap: Int = GuidedVocabularyCatalog.DEFAULT_VISIBLE_ENTRY_CAP
    ): List<GuidedVocabularyEntry> {
        if (state.screenMode != GuidedOverlayScreenMode.Vocabulary) return emptyList()
        return GuidedNavigationController.visiblePhraseEntries(
            entries = GuidedVocabularyCatalog.currentPageEntries(
                state.categoryIndex,
                language,
                uiStrings,
                catalogContext
            ),
            phrasePageIndex = state.phrasePageIndex,
            visibleCap = visibleEntryCap
        )
    }

    /** Mappings used for partial-sequence continuation checks — scoped to the open workspace context. */
    fun continuationMappings(
        state: GuidedNavigationState,
        language: PreferredLanguage,
        uiStrings: LisaUiStrings,
        catalogContext: GuidedCatalogContext,
        visibleEntryCap: Int = GuidedVocabularyCatalog.DEFAULT_VISIBLE_ENTRY_CAP
    ): List<WinkMapping> = when (state.screenMode) {
        GuidedOverlayScreenMode.CategoryMenu ->
            GuidedCategoryShortcuts.allGestures().map { (left, right) ->
                WinkMapping(left, right, "", isCustom = true, customPhrase = "")
            }
        GuidedOverlayScreenMode.Vocabulary ->
            visibleEntriesForState(state, language, uiStrings, catalogContext, visibleEntryCap)
                .map { entry ->
                    WinkMapping(entry.left, entry.right, "", isCustom = true, customPhrase = entry.phrase)
                }
    }
}

object GuidedVocabularyCatalogValidation {
    private val allPages get() =
        GuidedVocabularyCatalog.buildPages(PreferredLanguage.English, LisaUiStrings.forLanguage(PreferredLanguage.English))

    fun usesReservedGlobalSequence(entry: GuidedVocabularyEntry): Boolean =
        GuidedModeNavigation.isGlobalNavigationSequence(entry.left, entry.right) ||
            isEmergencySequence(entry.left, entry.right)

    fun isAlternatingEyeSequence(left: Int, right: Int): Boolean =
        left >= 1 && right >= 1

    fun isForbiddenVocabularySequence(left: Int, right: Int): Boolean =
        (left to right) in GuidedPageSequences.forbiddenForVocabulary

    fun noVocabularyUsesForbiddenSequences(): Boolean =
        allPages.flatMap { it.entries }
            .none { isForbiddenVocabularySequence(it.left, it.right) }

    fun allVocabularyUsesAlternatingEyePattern(): Boolean =
        allPages.flatMap { it.entries }
            .all { isAlternatingEyeSequence(it.left, it.right) }

    fun sequencesUniqueWithinEachPage(): Boolean =
        allPages.all { page ->
            page.entries.map { it.left to it.right }.distinct().size == page.entries.size
        }

    fun sequencesRepeatAcrossPages(): Boolean {
        val standardPages = allPages.filter { it.category != GuidedVocabularyCategory.Preferences }
        val page0 = standardPages[0].entries.map { it.left to it.right }
        return standardPages.drop(1).all { page ->
            page.entries.map { it.left to it.right } == page0
        }
    }

    fun preferencesAppearsAfterBasicSystemControls(): Boolean {
        val titles = GuidedVocabularyCatalog.categoryMenuTitles(
            LisaUiStrings.forLanguage(PreferredLanguage.English)
        )
        return titles.indexOf("Basic System Controls") < titles.indexOf("Preferences")
    }

    fun preferencesEntriesOnlyOnPreferencesPage(): Boolean {
        val preferencesIndex = GuidedVocabularyCategory.PREFERENCES_CATEGORY_INDEX
        val preferencesEntries = allPages[preferencesIndex].entries
        val otherEntries = allPages.filterIndexed { index, _ -> index != preferencesIndex }
            .flatMap { it.entries }
        return preferencesEntries.none { pref ->
            otherEntries.any { it.left == pref.left && it.right == pref.right }
        }
    }

    fun preferencesMatchWorksOnlyWhenOpen(): Boolean {
        val preferencesIndex = GuidedVocabularyCategory.PREFERENCES_CATEGORY_INDEX
        val context = GuidedCatalogContext(responseTimeSec = 3, sensitivityLevel = 5)
        val uiStrings = LisaUiStrings.forLanguage(PreferredLanguage.English)
        val pages = GuidedVocabularyCatalog.buildPages(
            PreferredLanguage.English,
            uiStrings,
            context
        )
        val adjustEntry = pages[preferencesIndex].entries.first { it.phrase.contains("Adjust response time") }
        val onPreferences = GuidedVocabularyCatalog.findMatchOnCurrentPage(
            adjustEntry.left,
            adjustEntry.right,
            preferencesIndex,
            PreferredLanguage.English,
            uiStrings,
            context
        )
        val onConversation = GuidedVocabularyCatalog.findMatchOnCurrentPage(
            adjustEntry.left,
            adjustEntry.right,
            0,
            PreferredLanguage.English,
            uiStrings,
            context
        )
        return onPreferences != null && onConversation == null
    }

    fun preferencesShowsCompactControlsOnly(): Boolean {
        val page = allPages[GuidedVocabularyCategory.PREFERENCES_CATEGORY_INDEX]
        return page.entries.size == 4 &&
            page.entries.none { it.phrase.startsWith("Set response time to") } &&
            page.entries.none { it.phrase.startsWith("Set sensitivity to") }
    }

    fun preferencesHasAdjustResponseTime(): Boolean =
        allPages[GuidedVocabularyCategory.PREFERENCES_CATEGORY_INDEX].entries
            .any { it.phrase.contains("Adjust response time", ignoreCase = true) }

    fun preferencesHasAdjustSensitivity(): Boolean =
        allPages[GuidedVocabularyCategory.PREFERENCES_CATEGORY_INDEX].entries
            .any { it.phrase.contains("Adjust sensitivity", ignoreCase = true) }

    fun categoryShortcutsDoNotConflictWithGlobalNavigation(): Boolean =
        GuidedCategoryShortcuts.doNotConflictWithGlobalNavigation()

    fun categoryShortcutLabelsMatchExpectedSlots(): Boolean {
        val expected = listOf("L2 R1", "L1 R2", "L3 R1", "L1 R3", "L3 R2", "L2 R3")
        return (0 until GuidedVocabularyCategory.PAGE_COUNT).all { index ->
            GuidedCategoryShortcuts.sequenceLabelForCategory(index) == expected[index]
        }
    }

    fun sameSlotDifferentPhrasesAcrossPages(): Boolean {
        val standardPages = allPages.filter { it.category != GuidedVocabularyCategory.Preferences }
        val slot0Phrases = standardPages.map { it.entries.first().phrase }.distinct()
        return slot0Phrases.size == standardPages.size
    }

    fun medicalPageHasNoEmergencyPhrase(): Boolean =
        allPages[2].entries.none { isEmergencySequence(it.left, it.right) } &&
            allPages[2].entries.none { it.phrase.contains("emergency", ignoreCase = true) }

    fun medicalPageHasPleaseHelpMeNow(): Boolean =
        allPages[2].entries.any { it.phrase == "Please help me now." }

    fun maxSequenceWinksReasonable(maxTotal: Int = 9): Boolean =
        allPages.flatMap { it.entries }
            .all { it.left + it.right <= maxTotal }

    fun allGlobalNavigationGesturesReserved(): Boolean =
        GuidedPageSequences.forbiddenForVocabulary.containsAll(
            listOf(
                GuidedModeNavigation.PREVIOUS_LEFT to GuidedModeNavigation.PREVIOUS_RIGHT,
                GuidedModeNavigation.NEXT_LEFT to GuidedModeNavigation.NEXT_RIGHT,
                GuidedModeNavigation.SELECT_LEFT to GuidedModeNavigation.SELECT_RIGHT,
                GuidedModeNavigation.BACK_LEFT to GuidedModeNavigation.BACK_RIGHT,
                GuidedModeNavigation.CATEGORIES_LEFT to GuidedModeNavigation.CATEGORIES_RIGHT,
                EMERGENCY_LEFT_WINKS to EMERGENCY_RIGHT_WINKS,
                1 to 0,
                0 to 1
            )
        )
}
