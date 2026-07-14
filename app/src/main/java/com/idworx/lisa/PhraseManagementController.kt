package com.idworx.lisa

/**
 * RC7D.15 / RC7D.17A — page-based, blink-controlled Phrase Management list.
 * Uses canonical PREVIOUS (L2 R0) / NEXT (L0 R2) scroll sequences.
 */
object PhraseManagementController {

    /**
     * Compact page size so heading + indicator + phrase cards + fixed nav + Emergency
     * fit on small physical phones without clipping fixed controls.
     */
    const val PAGE_SIZE: Int = 3

    /** Whether Phrase Management occupies the main workspace slot (like the composer). */
    fun occupiesMainContentSlot(activePanel: LisaPanel): Boolean =
        activePanel == LisaPanel.VocabularyTraining

    /**
     * Compact-device budget: reserve fixed chrome (header + command strip + Emergency)
     * and confirm remaining height can hold [PAGE_SIZE] phrase cards.
     */
    fun compactLayoutFits(
        screenHeightDp: Int,
        headerDp: Int = 88,
        commandStripDp: Int = 168,
        emergencyDp: Int = 76,
        phraseCardDp: Int = 108,
        bottomChromeDp: Int = 72
    ): Boolean {
        val fixed = headerDp + commandStripDp + emergencyDp + bottomChromeDp
        val remaining = screenHeightDp - fixed
        return remaining >= phraseCardDp // at least one readable card; paging covers the rest
    }

    fun pageCount(phraseCount: Int): Int =
        GuidedNavigationController.phrasePageCount(phraseCount, PAGE_SIZE)

    fun coercePage(pageIndex: Int, phraseCount: Int): Int {
        val last = (pageCount(phraseCount) - 1).coerceAtLeast(0)
        return pageIndex.coerceIn(0, last)
    }

    fun visiblePhrases(phrases: List<WinkMapping>, pageIndex: Int): List<WinkMapping> {
        if (phrases.isEmpty()) return emptyList()
        val page = coercePage(pageIndex, phrases.size)
        val start = page * PAGE_SIZE
        if (start >= phrases.size) return emptyList()
        return phrases.subList(start, minOf(start + PAGE_SIZE, phrases.size))
    }

    fun canScrollUp(pageIndex: Int): Boolean = pageIndex > 0

    fun canScrollDown(pageIndex: Int, phraseCount: Int): Boolean =
        phraseCount > 0 && pageIndex < pageCount(phraseCount) - 1

    fun scrollUp(state: PhraseManagementUiState): PhraseManagementUiState =
        state.copy(listPageIndex = (state.listPageIndex - 1).coerceAtLeast(0))

    fun scrollDown(state: PhraseManagementUiState, phraseCount: Int): PhraseManagementUiState =
        state.copy(listPageIndex = coercePage(state.listPageIndex + 1, phraseCount))

    fun afterPhraseListChanged(
        state: PhraseManagementUiState,
        remainingCount: Int
    ): PhraseManagementUiState =
        state.copy(listPageIndex = coercePage(state.listPageIndex, remainingCount))

    fun pageIndicatorLabel(pageIndex: Int, phraseCount: Int): String {
        val pages = pageCount(phraseCount).coerceAtLeast(1)
        val page = coercePage(pageIndex, phraseCount) + 1
        return "$page / $pages"
    }

    data class VisibleCommand(
        val left: Int,
        val right: Int,
        val label: String,
        val symbol: String,
        val action: PhraseManagementNavAction,
        val enabled: Boolean = true
    )

    enum class PhraseManagementNavAction {
        ScrollUp,
        ScrollDown,
        Back
    }

    /** Explicit exit destinations for Phrase Management Back — not history-pop. */
    enum class PhraseManagementExitDestination {
        CommunicationWorkspace
    }

    /**
     * RC7D.17 — always show Scroll Up / Scroll Down / Back to Communication.
     * Scroll controls stay visible when inactive; [VisibleCommand.enabled] reflects availability.
     */
    fun listCommandEntries(
        state: PhraseManagementUiState,
        phraseCount: Int,
        uiStrings: LisaUiStrings
    ): List<VisibleCommand> = listOf(
        VisibleCommand(
            left = GuidedModeNavigation.PREVIOUS_LEFT,
            right = GuidedModeNavigation.PREVIOUS_RIGHT,
            label = uiStrings.guidedScrollUp,
            symbol = "↑",
            action = PhraseManagementNavAction.ScrollUp,
            enabled = canScrollUp(state.listPageIndex)
        ),
        VisibleCommand(
            left = GuidedModeNavigation.NEXT_LEFT,
            right = GuidedModeNavigation.NEXT_RIGHT,
            label = uiStrings.guidedScrollDown,
            symbol = "↓",
            action = PhraseManagementNavAction.ScrollDown,
            enabled = canScrollDown(state.listPageIndex, phraseCount)
        ),
        VisibleCommand(
            left = GuidedModeNavigation.BACK_LEFT,
            right = GuidedModeNavigation.BACK_RIGHT,
            label = uiStrings.phraseManagementBackToCommunication,
            symbol = "↩",
            action = PhraseManagementNavAction.Back,
            enabled = true
        )
    )

    fun visiblePhraseSelectionSlots(
        phrases: List<WinkMapping>,
        pageIndex: Int
    ): List<Pair<WinkMapping, Pair<Int, Int>>> =
        visiblePhrases(phrases, pageIndex).mapIndexed { index, mapping ->
            mapping to GuidedPageSequences.slotAt(index)
        }

    enum class PhraseDetailsAction {
        Edit,
        Move,
        Delete
    }

    data class DetailsActionEntry(
        val left: Int,
        val right: Int,
        val label: String,
        val action: PhraseDetailsAction
    )

    /** Canonical details actions — same slot order as visible list selection slots 0–2. */
    fun detailsActionEntries(uiStrings: LisaUiStrings): List<DetailsActionEntry> = listOf(
        DetailsActionEntry(
            left = GuidedPageSequences.leftAt(0),
            right = GuidedPageSequences.rightAt(0),
            label = uiStrings.phraseManagementEditPhrase,
            action = PhraseDetailsAction.Edit
        ),
        DetailsActionEntry(
            left = GuidedPageSequences.leftAt(1),
            right = GuidedPageSequences.rightAt(1),
            label = uiStrings.phraseManagementMoveCategory,
            action = PhraseDetailsAction.Move
        ),
        DetailsActionEntry(
            left = GuidedPageSequences.leftAt(2),
            right = GuidedPageSequences.rightAt(2),
            label = uiStrings.phraseManagementDeletePhrase,
            action = PhraseDetailsAction.Delete
        )
    )
}
