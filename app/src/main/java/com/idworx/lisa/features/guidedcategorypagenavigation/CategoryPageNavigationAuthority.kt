package com.idworx.lisa.features.guidedcategorypagenavigation

import com.idworx.lisa.CategoryNavigationCause
import com.idworx.lisa.CategoryViewportPaging
import com.idworx.lisa.GuidedModeNavigation
import com.idworx.lisa.GuidedNavigationState
import com.idworx.lisa.GuidedOverlayScreenMode
import com.idworx.lisa.formatWinkSequenceShort

/**
 * RC8.26 — production Category Menu page-navigation evidence for guided Lessons 20–21.
 *
 * Next Page / Previous Page are viewport page jumps (L0 R4 / L4 R0), never item Move Down/Up
 * (L0 R2 / L2 R0). Completion requires a real [CategoryNavigationCause.PAGE_MOVEMENT] result
 * with a visible page-index and page-content identity change — not a label-only update.
 */
object CategoryPageNavigationAuthority {

    const val ID_NEXT_PAGE: String = "nav_next_page"
    const val ID_PREVIOUS_PAGE: String = "nav_previous_page"

    enum class PageNavigationAction {
        NEXT_PAGE,
        PREVIOUS_PAGE
    }

    /**
     * Production-level page-navigation result metadata. Not lesson-only booleans — reusable
     * evidence for any caller that needs to verify a real viewport page change.
     */
    data class PageNavigationResult(
        val previousPageIndex: Int,
        val resultingPageIndex: Int,
        val navigationAction: PageNavigationAction,
        val sequenceUsed: String,
        val executionIdentifier: Long,
        val visiblePageContentIdentity: String
    )

    fun nextPageSequenceLabel(): String = formatWinkSequenceShort(
        GuidedModeNavigation.NEXT_CATEGORY_PAGE_LEFT,
        GuidedModeNavigation.NEXT_CATEGORY_PAGE_RIGHT
    )

    fun previousPageSequenceLabel(): String = formatWinkSequenceShort(
        GuidedModeNavigation.PREVIOUS_CATEGORY_PAGE_LEFT,
        GuidedModeNavigation.PREVIOUS_CATEGORY_PAGE_RIGHT
    )

    fun matchesNextPage(left: Int, right: Int): Boolean =
        GuidedModeNavigation.isNextCategoryPageSequence(left, right)

    fun matchesPreviousPage(left: Int, right: Int): Boolean =
        GuidedModeNavigation.isPreviousCategoryPageSequence(left, right)

    /** Display page number (1-based) matching the Category Menu header "Page X / Y". */
    fun displayPageNumber(state: GuidedNavigationState): Int =
        (state.categoryViewportPage + 1).coerceIn(1, state.categoryViewportPageCount.coerceAtLeast(1))

    /**
     * Identity of what the learner can see on the current viewport page — changes when the
     * page index or the page-driven selection anchor changes.
     */
    fun visiblePageContentIdentity(state: GuidedNavigationState): String =
        "page=${state.categoryViewportPage};" +
            "count=${state.categoryViewportPageCount};" +
            "selection=${state.categoryMenuSelection};" +
            "cause=${state.categoryNavigationCause}"

    fun isCategoryMenu(state: GuidedNavigationState): Boolean =
        state.screenMode == GuidedOverlayScreenMode.CategoryMenu

    /** Lesson 20 must begin on Page 1 of a multi-page Category Menu. */
    fun isNextPageStartState(state: GuidedNavigationState): Boolean =
        isCategoryMenu(state) &&
            state.categoryViewportPage == 0 &&
            CategoryViewportPaging.canGoToNextPage(
                state.categoryViewportPage,
                state.categoryViewportPageCount
            )

    /** Lesson 21 must begin on Page 2 (preserved from Lesson 20). */
    fun isPreviousPageStartState(state: GuidedNavigationState): Boolean =
        isCategoryMenu(state) &&
            CategoryViewportPaging.canGoToPreviousPage(state.categoryViewportPage)

    fun evaluate(
        before: GuidedNavigationState,
        after: GuidedNavigationState,
        left: Int,
        right: Int,
        executionIdentifier: Long = System.nanoTime()
    ): PageNavigationResult? {
        val action = when {
            matchesNextPage(left, right) -> PageNavigationAction.NEXT_PAGE
            matchesPreviousPage(left, right) -> PageNavigationAction.PREVIOUS_PAGE
            else -> return null
        }
        if (after.categoryNavigationCause != CategoryNavigationCause.PAGE_MOVEMENT) return null
        if (before.categoryViewportPage == after.categoryViewportPage) return null
        if (visiblePageContentIdentity(before) == visiblePageContentIdentity(after)) return null
        val expectedDelta = when (action) {
            PageNavigationAction.NEXT_PAGE -> 1
            PageNavigationAction.PREVIOUS_PAGE -> -1
        }
        if (after.categoryViewportPage != before.categoryViewportPage + expectedDelta) return null
        // Production next/previousCategoryPage also re-anchors selection — reject page-label-only.
        val expectedSelection = when (action) {
            PageNavigationAction.NEXT_PAGE ->
                (com.idworx.lisa.GuidedVocabularyCategory.PAGE_COUNT - 1).coerceAtLeast(0)
            PageNavigationAction.PREVIOUS_PAGE -> 0
        }
        if (after.categoryMenuSelection != expectedSelection) return null
        return PageNavigationResult(
            previousPageIndex = before.categoryViewportPage,
            resultingPageIndex = after.categoryViewportPage,
            navigationAction = action,
            sequenceUsed = formatWinkSequenceShort(left, right),
            executionIdentifier = executionIdentifier,
            visiblePageContentIdentity = visiblePageContentIdentity(after)
        )
    }

    fun isNextPageCompleted(
        before: GuidedNavigationState,
        after: GuidedNavigationState,
        left: Int,
        right: Int
    ): Boolean {
        val result = evaluate(before, after, left, right) ?: return false
        return result.navigationAction == PageNavigationAction.NEXT_PAGE &&
            result.previousPageIndex == 0 &&
            result.resultingPageIndex == 1 &&
            displayPageNumber(after) == 2
    }

    fun isPreviousPageCompleted(
        before: GuidedNavigationState,
        after: GuidedNavigationState,
        left: Int,
        right: Int
    ): Boolean {
        val result = evaluate(before, after, left, right) ?: return false
        return result.navigationAction == PageNavigationAction.PREVIOUS_PAGE &&
            result.previousPageIndex >= 1 &&
            result.resultingPageIndex == 0 &&
            displayPageNumber(after) == 1
    }
}
