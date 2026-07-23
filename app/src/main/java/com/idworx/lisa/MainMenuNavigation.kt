package com.idworx.lisa

/**
 * RC7D.28 — Fully eye-controllable Communication workspace Menu.
 *
 * Destinations are stable identities (not list indices). Section headers are visible but never
 * selectable. Item movement skips headers; viewport paging reuses [CategoryViewportPaging].
 */
enum class MainMenuSection {
    Communication,
    Application,
    Support
}

enum class MainMenuDestination {
    CommunicationProfile,
    PhraseManagement,
    Voice,
    Settings,
    AboutLisa,
    PrivacyPolicy,
    Feedback,
    ReleaseNotes;

    val section: MainMenuSection
        get() = when (this) {
            CommunicationProfile, PhraseManagement, Voice -> MainMenuSection.Communication
            Settings, AboutLisa, PrivacyPolicy -> MainMenuSection.Application
            Feedback, ReleaseNotes -> MainMenuSection.Support
        }

    /** Existing LisaPanel route — touch and blink share this mapping. */
    val panel: LisaPanel
        get() = when (this) {
            CommunicationProfile -> LisaPanel.MyCommunication
            PhraseManagement -> LisaPanel.VocabularyTraining
            Voice -> LisaPanel.Voice
            Settings -> LisaPanel.Settings
            AboutLisa -> LisaPanel.AboutLisa
            PrivacyPolicy -> LisaPanel.PrivacyPolicy
            Feedback -> LisaPanel.Feedback
            ReleaseNotes -> LisaPanel.ReleaseNotes
        }

    companion object {
        val ordered: List<MainMenuDestination> = entries

        fun fromPanel(panel: LisaPanel): MainMenuDestination? =
            ordered.firstOrNull { it.panel == panel }
    }
}

sealed class MainMenuListEntry {
    data class SectionHeader(val section: MainMenuSection) : MainMenuListEntry()
    data class Destination(
        val destination: MainMenuDestination,
        /** 1-based index among selectable destinations only. */
        val selectionIndex: Int
    ) : MainMenuListEntry()
}

object MainMenuCatalog {
    val destinations: List<MainMenuDestination> = MainMenuDestination.ordered

    val destinationCount: Int get() = destinations.size

    fun title(destination: MainMenuDestination, uiStrings: LisaUiStrings): String =
        uiStrings.menuLabel(destination.panel)

    fun sectionTitle(section: MainMenuSection, uiStrings: LisaUiStrings): String = when (section) {
        MainMenuSection.Communication -> uiStrings.menuSectionCommunication
        MainMenuSection.Application -> uiStrings.menuSectionApplication
        MainMenuSection.Support -> uiStrings.menuSectionSupport
    }

    fun accessibilityDescription(destination: MainMenuDestination, uiStrings: LisaUiStrings): String {
        val index = destinations.indexOf(destination) + 1
        val sequence = MainMenuDestinationShortcuts.sequenceLabelForDestination(destination)
        return "${uiStrings.mainMenuItemAccessibility(
            title(destination, uiStrings),
            index,
            destinationCount
        )}, $sequence"
    }

    /** Render order: section header then its destinations (headers are not selectable). */
    fun listEntries(): List<MainMenuListEntry> {
        val out = mutableListOf<MainMenuListEntry>()
        var selectionIndex = 0
        MainMenuSection.entries.forEach { section ->
            out += MainMenuListEntry.SectionHeader(section)
            destinations.filter { it.section == section }.forEach { destination ->
                selectionIndex += 1
                out += MainMenuListEntry.Destination(destination, selectionIndex)
            }
        }
        return out
    }
}

/**
 * Direct blink shortcuts for Main Menu destinations — same architecture as
 * [GuidedCategoryShortcuts] in the Communication Category Menu.
 *
 * Displayed labels and processed gestures always come from this authority.
 * Settings uses the canonical L5 R5 entry sequence (same as Adjust Settings).
 */
object MainMenuDestinationShortcuts {
    val SHORTCUT_COUNT: Int get() = MainMenuCatalog.destinationCount

    fun indexOf(destination: MainMenuDestination): Int =
        MainMenuCatalog.destinations.indexOf(destination)

    fun gestureForDestination(destination: MainMenuDestination): Pair<Int, Int> =
        gestureForIndex(indexOf(destination))

    fun gestureForIndex(destinationIndex: Int): Pair<Int, Int> {
        val index = destinationIndex.coerceIn(0, SHORTCUT_COUNT - 1)
        if (MainMenuCatalog.destinations[index] == MainMenuDestination.Settings) {
            return GuidedModeNavigation.ADJUST_SETTINGS_ENTRY_LEFT to
                GuidedModeNavigation.ADJUST_SETTINGS_ENTRY_RIGHT
        }
        return GuidedPageSequences.slotAt(index)
    }

    fun sequenceLabelForDestination(destination: MainMenuDestination): String {
        val (left, right) = gestureForDestination(destination)
        return formatWinkSequenceShort(left, right)
    }

    fun sequenceLabelForIndex(destinationIndex: Int): String {
        val (left, right) = gestureForIndex(destinationIndex)
        return formatWinkSequenceShort(left, right)
    }

    fun destinationForGesture(left: Int, right: Int): MainMenuDestination? {
        for (index in 0 until SHORTCUT_COUNT) {
            val (slotLeft, slotRight) = gestureForIndex(index)
            if (slotLeft == left && slotRight == right) {
                return MainMenuCatalog.destinations[index]
            }
        }
        return null
    }

    fun allGestures(): List<Pair<Int, Int>> =
        (0 until SHORTCUT_COUNT).map { gestureForIndex(it) }

    fun doNotConflictWithCommandNavigation(): Boolean {
        val command = setOf(
            GuidedModeNavigation.PREVIOUS_LEFT to GuidedModeNavigation.PREVIOUS_RIGHT,
            GuidedModeNavigation.NEXT_LEFT to GuidedModeNavigation.NEXT_RIGHT,
            GuidedModeNavigation.PREVIOUS_CATEGORY_PAGE_LEFT to
                GuidedModeNavigation.PREVIOUS_CATEGORY_PAGE_RIGHT,
            GuidedModeNavigation.NEXT_CATEGORY_PAGE_LEFT to
                GuidedModeNavigation.NEXT_CATEGORY_PAGE_RIGHT,
            GuidedModeNavigation.SELECT_LEFT to GuidedModeNavigation.SELECT_RIGHT,
            GuidedModeNavigation.BACK_LEFT to GuidedModeNavigation.BACK_RIGHT,
            GuidedModeNavigation.OPEN_MAIN_MENU_LEFT to
                GuidedModeNavigation.OPEN_MAIN_MENU_RIGHT,
            EMERGENCY_LEFT_WINKS to EMERGENCY_RIGHT_WINKS
        )
        return allGestures().none { it in command } &&
            allGestures().distinct().size == allGestures().size
    }
}

data class MainMenuNavigationState(
    val isOpen: Boolean = false,
    /** Index into [MainMenuCatalog.destinations] (0-based). */
    val selectionIndex: Int = 0,
    val viewportPage: Int = 0,
    val viewportPageCount: Int = 1,
    val scrollRequestPx: Int? = null,
    val revealSelection: Boolean = false
) {
    fun normalized(): MainMenuNavigationState {
        val count = MainMenuCatalog.destinationCount
        return copy(
            selectionIndex = selectionIndex.coerceIn(0, (count - 1).coerceAtLeast(0)),
            viewportPage = CategoryViewportPaging.clampPage(viewportPage, viewportPageCount.coerceAtLeast(1)),
            viewportPageCount = viewportPageCount.coerceAtLeast(1)
        )
    }

    val selectedDestination: MainMenuDestination
        get() = MainMenuCatalog.destinations[normalized().selectionIndex]
}

sealed class MainMenuSequenceResult {
    data class Navigate(val newState: MainMenuNavigationState) : MainMenuSequenceResult()
    data class OpenDestination(val destination: MainMenuDestination, val newState: MainMenuNavigationState) :
        MainMenuSequenceResult()
    data class CloseMenu(val newState: MainMenuNavigationState) : MainMenuSequenceResult()
    data object Unmatched : MainMenuSequenceResult()
}

object MainMenuController {

    fun open(
        state: MainMenuNavigationState = MainMenuNavigationState()
    ): MainMenuNavigationState = state.normalized().copy(
        isOpen = true,
        revealSelection = true,
        scrollRequestPx = null
    )

    fun close(state: MainMenuNavigationState = MainMenuNavigationState()): MainMenuNavigationState =
        state.normalized().copy(
            isOpen = false,
            revealSelection = false,
            scrollRequestPx = null
        )

    fun selectDestination(state: MainMenuNavigationState, index: Int): MainMenuNavigationState =
        state.normalized().copy(
            selectionIndex = index.coerceIn(0, MainMenuCatalog.destinationCount - 1),
            revealSelection = true,
            scrollRequestPx = null
        )

    fun moveSelectionUp(state: MainMenuNavigationState): MainMenuNavigationState {
        val normalized = state.normalized()
        return normalized.copy(
            selectionIndex = (normalized.selectionIndex - 1).coerceAtLeast(0),
            revealSelection = true,
            scrollRequestPx = null
        )
    }

    fun moveSelectionDown(state: MainMenuNavigationState): MainMenuNavigationState {
        val normalized = state.normalized()
        val max = MainMenuCatalog.destinationCount - 1
        return normalized.copy(
            selectionIndex = (normalized.selectionIndex + 1).coerceAtMost(max),
            revealSelection = true,
            scrollRequestPx = null
        )
    }

    fun previousPage(state: MainMenuNavigationState, viewportHeightPx: Int, maxScrollPx: Int): MainMenuNavigationState {
        val count = CategoryViewportPaging.pageCount(viewportHeightPx, maxScrollPx)
        val current = CategoryViewportPaging.clampPage(state.viewportPage, count)
        if (!CategoryViewportPaging.canGoToPreviousPage(current)) return state.copy(viewportPageCount = count)
        val page = current - 1
        return state.copy(
            viewportPage = page,
            viewportPageCount = count,
            scrollRequestPx = CategoryViewportPaging.pageAnchorOffsetPx(page, viewportHeightPx, maxScrollPx),
            revealSelection = false
        )
    }

    fun nextPage(state: MainMenuNavigationState, viewportHeightPx: Int, maxScrollPx: Int): MainMenuNavigationState {
        val count = CategoryViewportPaging.pageCount(viewportHeightPx, maxScrollPx)
        val current = CategoryViewportPaging.clampPage(state.viewportPage, count)
        if (!CategoryViewportPaging.canGoToNextPage(current, count)) return state.copy(viewportPageCount = count)
        val page = current + 1
        return state.copy(
            viewportPage = page,
            viewportPageCount = count,
            scrollRequestPx = CategoryViewportPaging.pageAnchorOffsetPx(page, viewportHeightPx, maxScrollPx),
            revealSelection = false
        )
    }

    fun syncViewportMetrics(
        state: MainMenuNavigationState,
        viewportHeightPx: Int,
        maxScrollPx: Int,
        scrollPx: Int
    ): MainMenuNavigationState {
        val count = CategoryViewportPaging.pageCount(viewportHeightPx, maxScrollPx)
        val page = CategoryViewportPaging.currentPageForScroll(scrollPx, viewportHeightPx, maxScrollPx)
        return state.copy(viewportPage = page, viewportPageCount = count)
    }

    /**
     * Mode-scoped gesture routing while the Menu is open. Emergency is matched upstream.
     * Open-Menu entry is unmatched here so it cannot reopen or stack the menu.
     *
     * Command-tier navigation (Up/Down/Page/Select/Back) is checked first; Content-tier
     * destination shortcuts open immediately without changing selection first.
     */
    fun processSequence(
        left: Int,
        right: Int,
        state: MainMenuNavigationState,
        viewportHeightPx: Int = 0,
        maxScrollPx: Int = 0
    ): MainMenuSequenceResult {
        if (!state.isOpen) return MainMenuSequenceResult.Unmatched
        val normalized = state.normalized()
        val pageCount = CategoryViewportPaging.pageCount(viewportHeightPx, maxScrollPx)
        return when {
            GuidedModeNavigation.isOpenMainMenuSequence(left, right) ->
                MainMenuSequenceResult.Unmatched
            GuidedModeNavigation.isPreviousSequence(left, right) ->
                MainMenuSequenceResult.Navigate(moveSelectionUp(normalized))
            GuidedModeNavigation.isNextSequence(left, right) ->
                MainMenuSequenceResult.Navigate(moveSelectionDown(normalized))
            GuidedModeNavigation.isPreviousCategoryPageSequence(left, right) &&
                pageCount > 1 ->
                MainMenuSequenceResult.Navigate(previousPage(normalized, viewportHeightPx, maxScrollPx))
            GuidedModeNavigation.isNextCategoryPageSequence(left, right) &&
                pageCount > 1 ->
                MainMenuSequenceResult.Navigate(nextPage(normalized, viewportHeightPx, maxScrollPx))
            GuidedModeNavigation.isSelectSequence(left, right) ->
                MainMenuSequenceResult.OpenDestination(normalized.selectedDestination, close(normalized))
            GuidedModeNavigation.isBackSequence(left, right) ->
                MainMenuSequenceResult.CloseMenu(close(normalized))
            else -> MainMenuDestinationShortcuts.destinationForGesture(left, right)?.let { destination ->
                MainMenuSequenceResult.OpenDestination(destination, close(normalized))
            } ?: MainMenuSequenceResult.Unmatched
        }
    }
}

/**
 * RC7D.29 — production root UI selection for Main Menu mode.
 *
 * RC7D.28 built the eye-controlled Menu composable but left it under the guided workspace
 * (`weight(1f)` overlay still drawn; Menu only as a bottom sheet). This authority is the single
 * place the live [LisaRootUI] path consults so Menu replaces the workspace content slot.
 */
object MainMenuProductionUiAuthority {

    /** Main Menu occupies the central content slot (same pattern as Phrase Management). */
    fun occupiesMainContentSlot(activePanel: LisaPanel): Boolean =
        activePanel == LisaPanel.Menu

    /** Guided Communication workspace must not render while Main Menu owns the content slot. */
    fun showGuidedVocabularyOverlay(
        activePanel: LisaPanel,
        phraseComposerActive: Boolean,
        phraseManagementActive: Boolean
    ): Boolean =
        !occupiesMainContentSlot(activePanel) &&
            PhraseManagementController.showGuidedVocabularyOverlayAlongsideManagement(
                phraseComposerActive = phraseComposerActive,
                phraseManagementActive = phraseManagementActive
            )

    /** Clear / Repeat no longer appear in the Communication workspace bottom bar (RC7D.30). */
    @Suppress("UNUSED_PARAMETER")
    fun showCommunicationClearAndRepeat(activePanel: LisaPanel): Boolean = false

    /**
     * Workspace bottom chrome: shown when composer is closed.
     * Communication mode: full-width Menu only.
     * Menu mode: full-width Close only.
     */
    fun showWorkspaceBottomChrome(phraseComposerActive: Boolean): Boolean =
        !phraseComposerActive

    fun showBottomCloseOnly(activePanel: LisaPanel): Boolean =
        occupiesMainContentSlot(activePanel)

    /** Solid dark workspace panel — never alpha, never transparent. */
    fun solidWorkspaceBackground(): androidx.compose.ui.graphics.Color =
        com.idworx.lisa.ui.theme.LisaWorkspaceVisualStyle.SolidPanelBackground

    fun isOpaqueWorkspaceBackground(color: androidx.compose.ui.graphics.Color): Boolean =
        color.alpha >= 1f && color == solidWorkspaceBackground()

    fun openMenuSequenceLabel(): String =
        formatWinkSequenceShort(
            GuidedModeNavigation.OPEN_MAIN_MENU_LEFT_WINKS,
            GuidedModeNavigation.OPEN_MAIN_MENU_RIGHT_WINKS
        )

    fun closeMenuSequenceLabel(): String =
        formatWinkSequenceShort(
            GuidedModeNavigation.BACK_LEFT,
            GuidedModeNavigation.BACK_RIGHT
        )
}
