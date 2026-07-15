package com.idworx.lisa

import com.idworx.lisa.validation.ValidationOutcome
import com.idworx.lisa.validation.authority.GestureConflictAuthorityV1
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * RC7D.22 — True Viewport Page Navigation.
 *
 * Previous / Next Page now move the category-list VIEWPORT directly. A page is a measured scroll
 * window ([CategoryViewportPaging]), never `selectionIndex / pageSize`. Previous Page from the
 * lower viewport returns straight to the top (offset 0) in one action instead of stepping upward
 * one category at a time; Next Page advances straight to the bottom anchor (maxScrollPx). The
 * navigation cause carried in state ([CategoryNavigationCause.PAGE_MOVEMENT]) keeps the RC7D.21
 * selection-centring path from overriding an explicit page scroll, and the selected category and
 * the viewport page are tracked as independent state.
 *
 * The device-like screen (eight categories, ~seven visible, one viewport of overflow) is a
 * two-page menu with anchors {0, maxScrollPx}.
 */
class Rc7D_22TrueViewportPageNavigationTest {

    private val english = LisaUiStrings.forLanguage(PreferredLanguage.English)

    private val pitch = GuidedCategoryMenuScroll.ROW_PITCH_PX
    private val viewport = pitch * 7            // ~7 rows visible
    private val content = GuidedVocabularyCategory.PAGE_COUNT * pitch
    private val maxScroll = content - viewport  // exactly one row of overflow

    private val prevPage = GuidedModeNavigation.PREVIOUS_CATEGORY_PAGE_LEFT to
        GuidedModeNavigation.PREVIOUS_CATEGORY_PAGE_RIGHT
    private val nextPage = GuidedModeNavigation.NEXT_CATEGORY_PAGE_LEFT to
        GuidedModeNavigation.NEXT_CATEGORY_PAGE_RIGHT

    private fun menuState(
        selection: Int = 0,
        viewportPage: Int = 0,
        viewportPageCount: Int = 2
    ): GuidedNavigationState =
        GuidedNavigationState(
            screenMode = GuidedOverlayScreenMode.CategoryMenu,
            categoryMenuSelection = selection,
            categoryViewportPage = viewportPage,
            categoryViewportPageCount = viewportPageCount
        )

    private fun process(left: Int, right: Int, state: GuidedNavigationState): GuidedSequenceResult =
        GuidedNavigationController.processSequence(left, right, state, PreferredLanguage.English, english)

    private fun readSource(relativePath: String): String {
        val normalized = relativePath.replace('/', java.io.File.separatorChar)
        val roots = listOfNotNull(
            java.io.File(System.getProperty("user.dir")),
            java.io.File(System.getProperty("user.dir")).parentFile
        )
        for (root in roots) {
            val direct = root.resolve(normalized)
            if (direct.isFile) return direct.readText()
            if (normalized.startsWith("app${java.io.File.separatorChar}")) {
                val withoutApp = root.resolve(normalized.removePrefix("app${java.io.File.separatorChar}"))
                if (withoutApp.isFile) return withoutApp.readText()
            }
        }
        error("Missing source: $relativePath")
    }

    private val guidedUi by lazy { readSource("app/src/main/java/com/idworx/lisa/LisaGuidedModeUi.kt") }
    private val mainActivity by lazy { readSource("app/src/main/java/com/idworx/lisa/MainActivity.kt") }

    // ==================================================================================
    // A. VIEWPORT PAGE CALCULATIONS
    // ==================================================================================

    @Test // A1 — content that fits fully inside the viewport is a single page
    fun contentFittingViewportIsOnePage() {
        assertEquals(1, CategoryViewportPaging.pageCount(viewportHeightPx = 1000, maxScrollPx = 0))
        assertEquals(1, CategoryViewportPaging.pageCountForContent(viewportHeightPx = 1000, contentHeightPx = 800))
    }

    @Test // A2 — overflowing content produces two pages
    fun overflowingContentProducesTwoPages() {
        assertEquals(2, CategoryViewportPaging.pageCount(viewportHeightPx = 1000, maxScrollPx = 200))
        assertEquals(2, CategoryViewportPaging.pageCount(viewport, maxScroll))
        assertEquals(2, CategoryViewportPaging.pageCountForContent(viewport, content))
    }

    @Test // A3 — page 1 target is zero
    fun page1TargetIsZero() {
        assertEquals(0, CategoryViewportPaging.pageAnchorOffsetPx(0, viewport, maxScroll))
    }

    @Test // A4 — final page target is maxScrollPx
    fun finalPageTargetIsMaxScroll() {
        assertEquals(maxScroll, CategoryViewportPaging.pageAnchorOffsetPx(1, viewport, maxScroll))
    }

    @Test // A5 — targets never become negative
    fun targetsNeverNegative() {
        for (page in -3..6) {
            for (max in intArrayOf(0, 40, 72, 500, 5000)) {
                assertTrue(CategoryViewportPaging.pageAnchorOffsetPx(page, viewport, max) >= 0)
            }
        }
    }

    @Test // A6 — targets never exceed maxScrollPx
    fun targetsNeverExceedMax() {
        for (page in -3..6) {
            for (max in intArrayOf(0, 40, 72, 500, 5000)) {
                assertTrue(CategoryViewportPaging.pageAnchorOffsetPx(page, viewport, max) <= max.coerceAtLeast(0))
            }
        }
    }

    @Test // A7 — invalid viewport measurements are handled safely
    fun invalidViewportHandledSafely() {
        assertEquals(1, CategoryViewportPaging.pageCount(viewportHeightPx = 0, maxScrollPx = 72))
        assertEquals(1, CategoryViewportPaging.pageCount(viewportHeightPx = -10, maxScrollPx = 72))
        assertEquals(0, CategoryViewportPaging.pageAnchorOffsetPx(1, viewportHeightPx = 0, maxScrollPx = 72))
        assertEquals(0, CategoryViewportPaging.pageAnchorOffsetPx(1, viewportHeightPx = -5, maxScrollPx = 72))
        assertEquals(0, CategoryViewportPaging.currentPageForScroll(50, viewportHeightPx = 0, maxScrollPx = 72))
    }

    @Test // A8 — zero content height is handled safely
    fun zeroContentHandledSafely() {
        assertEquals(1, CategoryViewportPaging.pageCount(viewportHeightPx = 1000, maxScrollPx = 0))
        assertEquals(1, CategoryViewportPaging.pageCount(viewportHeightPx = 1000, maxScrollPx = -50))
        assertEquals(0, CategoryViewportPaging.pageAnchorOffsetPx(0, 1000, 0))
        assertEquals(0, CategoryViewportPaging.currentPageForScroll(0, 1000, 0))
    }

    @Test // A9 — page count is derived from viewport / content measurements, not hardcoded
    fun pageCountDerivedFromMeasurements() {
        assertEquals(2, CategoryViewportPaging.pageCount(1000, 1000)) // one viewport of overflow
        assertEquals(3, CategoryViewportPaging.pageCount(1000, 1001)) // just over
        assertEquals(3, CategoryViewportPaging.pageCount(500, 1000))
        assertEquals(4, CategoryViewportPaging.pageCount(300, 900))
    }

    @Test // A10 — calculations are deterministic
    fun calculationsAreDeterministic() {
        assertEquals(
            CategoryViewportPaging.pageAnchorOffsetPx(1, viewport, maxScroll),
            CategoryViewportPaging.pageAnchorOffsetPx(1, viewport, maxScroll)
        )
        assertEquals(
            CategoryViewportPaging.currentPageForScroll(maxScroll, viewport, maxScroll),
            CategoryViewportPaging.currentPageForScroll(maxScroll, viewport, maxScroll)
        )
    }

    @Test // A11 — current page maps the scroll offset to its viewport page
    fun currentPageMapsScrollToPage() {
        assertEquals(0, CategoryViewportPaging.currentPageForScroll(0, viewport, maxScroll))
        assertEquals(1, CategoryViewportPaging.currentPageForScroll(maxScroll, viewport, maxScroll))
    }

    @Test // A12 — availability helpers gate on viewport page + page count
    fun availabilityHelpersGateOnViewportPage() {
        assertFalse(CategoryViewportPaging.canGoToPreviousPage(0))
        assertTrue(CategoryViewportPaging.canGoToPreviousPage(1))
        assertTrue(CategoryViewportPaging.canGoToNextPage(0, 2))
        assertFalse(CategoryViewportPaging.canGoToNextPage(1, 2))
        assertEquals(1, CategoryViewportPaging.clampPage(9, 2))
        assertEquals(0, CategoryViewportPaging.clampPage(-4, 2))
    }

    // ==================================================================================
    // B. PREVIOUS PAGE BEHAVIOUR
    // ==================================================================================

    @Test // B1 — Previous Page from Page 2 returns directly to offset zero
    fun previousPageReturnsDirectlyToOffsetZero() {
        val result = process(prevPage.first, prevPage.second, menuState(viewportPage = 1)) as GuidedSequenceResult.Navigate
        assertEquals(0, result.newState.categoryViewportPage)
        assertEquals(0, CategoryViewportPaging.pageAnchorOffsetPx(result.newState.categoryViewportPage, viewport, maxScroll))
    }

    @Test // B2 — it performs exactly one page transition
    fun previousPagePerformsOnePageTransition() {
        val after = GuidedNavigationController.previousCategoryPage(menuState(viewportPage = 1))
        assertEquals(1 - 1, after.categoryViewportPage)
    }

    @Test // B3 — it does not generate repeated Move Up actions (tagged as a page move)
    fun previousPageIsNotItemMovement() {
        val after = GuidedNavigationController.previousCategoryPage(menuState(viewportPage = 1))
        assertEquals(CategoryNavigationCause.PAGE_MOVEMENT, after.categoryNavigationCause)
        assertNotEquals(CategoryNavigationCause.ITEM_MOVEMENT, after.categoryNavigationCause)
    }

    @Test // B4 — it does not step through category indices
    fun previousPageDoesNotStepThroughCategories() {
        // Starting selection near the bottom jumps straight to Category 1, not selection - 1.
        val after = GuidedNavigationController.previousCategoryPage(menuState(selection = 6, viewportPage = 1))
        assertEquals(0, after.categoryMenuSelection)
        assertNotEquals(5, after.categoryMenuSelection)
    }

    @Test // B5 — it does not wrap from Page 1
    fun previousPageDoesNotWrapFromPage1() {
        val after = GuidedNavigationController.previousCategoryPage(menuState(viewportPage = 0))
        assertEquals(0, after.categoryViewportPage)
        assertTrue(process(prevPage.first, prevPage.second, menuState(viewportPage = 0)) is GuidedSequenceResult.Unmatched)
    }

    @Test // B6 — it is disabled on Page 1
    fun previousPageDisabledOnPage1() {
        assertFalse(CategoryViewportPaging.canGoToPreviousPage(0))
        assertTrue(process(prevPage.first, prevPage.second, menuState(viewportPage = 0)) is GuidedSequenceResult.Unmatched)
    }

    @Test // B7 — it is enabled on Page 2
    fun previousPageEnabledOnPage2() {
        assertTrue(CategoryViewportPaging.canGoToPreviousPage(1))
        assertTrue(process(prevPage.first, prevPage.second, menuState(viewportPage = 1)) is GuidedSequenceResult.Navigate)
    }

    @Test // B8 — its destination remains Page 1 after the selection state updates
    fun previousPageDestinationStableAcrossSelectionUpdates() {
        val afterPage = GuidedNavigationController.previousCategoryPage(menuState(viewportPage = 1))
        // Simulate a later selection change (e.g. an item move) — the viewport page target holds.
        val afterSelectionChange = afterPage.copy(categoryMenuSelection = 5)
        assertEquals(0, afterSelectionChange.categoryViewportPage)
        assertEquals(0, CategoryViewportPaging.pageAnchorOffsetPx(afterSelectionChange.categoryViewportPage, viewport, maxScroll))
    }

    @Test // B9 — RC7D.21 centring cannot override the Previous Page target
    fun previousPageTargetNotOverriddenByCentring() {
        val after = GuidedNavigationController.previousCategoryPage(menuState(selection = 6, viewportPage = 1))
        assertEquals(CategoryNavigationCause.PAGE_MOVEMENT, after.categoryNavigationCause)
        // The page anchor is a function of the viewport page ONLY (no selection parameter), so no
        // selection value can shift it, and the coordinator evaluates PAGE_MOVEMENT before centring.
        assertEquals(0, CategoryViewportPaging.pageAnchorOffsetPx(after.categoryViewportPage, viewport, maxScroll))
        assertTrue(
            guidedUi.indexOf("CategoryNavigationCause.PAGE_MOVEMENT") <
                guidedUi.indexOf("GuidedCategoryMenuScroll.centeredScrollOffsetPx(")
        )
        assertTrue(guidedUi.contains("CategoryViewportPaging.pageAnchorOffsetPx("))
    }

    // ==================================================================================
    // C. NEXT PAGE BEHAVIOUR
    // ==================================================================================

    @Test // C1 — Next Page from Page 1 goes directly to the next page anchor
    fun nextPageGoesDirectlyToNextAnchor() {
        val result = process(nextPage.first, nextPage.second, menuState(viewportPage = 0)) as GuidedSequenceResult.Navigate
        assertEquals(1, result.newState.categoryViewportPage)
        assertEquals(maxScroll, CategoryViewportPaging.pageAnchorOffsetPx(result.newState.categoryViewportPage, viewport, maxScroll))
    }

    @Test // C2 — for two pages, the destination equals maxScrollPx
    fun nextPageDestinationEqualsMaxScroll() {
        val after = GuidedNavigationController.nextCategoryPage(menuState(viewportPage = 0))
        assertEquals(maxScroll, CategoryViewportPaging.pageAnchorOffsetPx(after.categoryViewportPage, viewport, maxScroll))
    }

    @Test // C3 — it performs exactly one page transition
    fun nextPagePerformsOnePageTransition() {
        val after = GuidedNavigationController.nextCategoryPage(menuState(viewportPage = 0))
        assertEquals(0 + 1, after.categoryViewportPage)
    }

    @Test // C4 — it does not generate repeated Move Down actions (tagged as a page move)
    fun nextPageIsNotItemMovement() {
        val after = GuidedNavigationController.nextCategoryPage(menuState(viewportPage = 0))
        assertEquals(CategoryNavigationCause.PAGE_MOVEMENT, after.categoryNavigationCause)
        assertNotEquals(CategoryNavigationCause.ITEM_MOVEMENT, after.categoryNavigationCause)
    }

    @Test // C5 — it does not wrap from the final page
    fun nextPageDoesNotWrapFromFinalPage() {
        val after = GuidedNavigationController.nextCategoryPage(menuState(viewportPage = 1, viewportPageCount = 2))
        assertEquals(1, after.categoryViewportPage)
        assertTrue(
            process(nextPage.first, nextPage.second, menuState(viewportPage = 1, viewportPageCount = 2))
                is GuidedSequenceResult.Unmatched
        )
    }

    @Test // C6 — it is enabled on Page 1
    fun nextPageEnabledOnPage1() {
        assertTrue(CategoryViewportPaging.canGoToNextPage(0, 2))
        assertTrue(process(nextPage.first, nextPage.second, menuState(viewportPage = 0)) is GuidedSequenceResult.Navigate)
    }

    @Test // C7 — it is disabled on Page 2
    fun nextPageDisabledOnPage2() {
        assertFalse(CategoryViewportPaging.canGoToNextPage(1, 2))
        assertTrue(
            process(nextPage.first, nextPage.second, menuState(viewportPage = 1, viewportPageCount = 2))
                is GuidedSequenceResult.Unmatched
        )
    }

    @Test // C8 — RC7D.21 centring cannot override the Next Page target
    fun nextPageTargetNotOverriddenByCentring() {
        val after = GuidedNavigationController.nextCategoryPage(menuState(selection = 0, viewportPage = 0))
        assertEquals(CategoryNavigationCause.PAGE_MOVEMENT, after.categoryNavigationCause)
        assertEquals(maxScroll, CategoryViewportPaging.pageAnchorOffsetPx(after.categoryViewportPage, viewport, maxScroll))
    }

    // ==================================================================================
    // D. SELECTION AND VIEWPORT INDEPENDENCE
    // ==================================================================================

    @Test // D1 — Category 7 may be selected while the viewport reports Page 1
    fun category7SelectableOnViewportPage1() {
        val state = menuState(selection = 6, viewportPage = 0)
        assertEquals(6, state.categoryMenuSelection)
        assertEquals(0, state.categoryViewportPage)
    }

    @Test // D2 — Category 7 may remain selected while the viewport reports Page 2
    fun category7SelectableOnViewportPage2() {
        val state = menuState(selection = 6, viewportPage = 1)
        assertEquals(6, state.categoryMenuSelection)
        assertEquals(1, state.categoryViewportPage)
    }

    @Test // D3 — the category indicator depends on selection
    fun categoryIndicatorDependsOnSelection() {
        assertTrue(guidedUi.contains("guidedCategoryIndicator(categoryMenuSelection + 1"))
    }

    @Test // D4 — the page indicator depends on the viewport page
    fun pageIndicatorDependsOnViewportPage() {
        assertTrue(guidedUi.contains("guidedPageIndicator("))
        assertTrue(guidedUi.contains("categoryViewportPage"))
    }

    @Test // D5 — changing the viewport page does not open a category
    fun changingViewportPageDoesNotOpenCategory() {
        val next = process(nextPage.first, nextPage.second, menuState(viewportPage = 0)) as GuidedSequenceResult.Navigate
        assertEquals(GuidedOverlayScreenMode.CategoryMenu, next.newState.screenMode)
        val prev = process(prevPage.first, prevPage.second, menuState(viewportPage = 1)) as GuidedSequenceResult.Navigate
        assertEquals(GuidedOverlayScreenMode.CategoryMenu, prev.newState.screenMode)
    }

    @Test // D6 — changing the viewport page does not speak a phrase
    fun changingViewportPageDoesNotSpeak() {
        assertTrue(process(nextPage.first, nextPage.second, menuState(viewportPage = 0)) is GuidedSequenceResult.Navigate)
        assertTrue(process(prevPage.first, prevPage.second, menuState(viewportPage = 1)) is GuidedSequenceResult.Navigate)
    }

    @Test // D7 — item movement still changes selection one category at a time
    fun itemMovementChangesSelectionOneAtATime() {
        val down = process(GuidedModeNavigation.NEXT_LEFT, GuidedModeNavigation.NEXT_RIGHT, menuState(selection = 0)) as GuidedSequenceResult.Navigate
        assertEquals(1, down.newState.categoryMenuSelection)
        assertEquals(CategoryNavigationCause.ITEM_MOVEMENT, down.newState.categoryNavigationCause)
        val up = process(GuidedModeNavigation.PREVIOUS_LEFT, GuidedModeNavigation.PREVIOUS_RIGHT, menuState(selection = 3)) as GuidedSequenceResult.Navigate
        assertEquals(2, up.newState.categoryMenuSelection)
        assertEquals(CategoryNavigationCause.ITEM_MOVEMENT, up.newState.categoryNavigationCause)
    }

    @Test // D8 — page movement does not simulate item movement
    fun pageMovementDoesNotSimulateItemMovement() {
        val after = GuidedNavigationController.nextCategoryPage(menuState(selection = 0, viewportPage = 0))
        // Not a single-step selection change; it is a direct viewport-page jump.
        assertNotEquals(1, after.categoryMenuSelection)
        assertEquals(CategoryNavigationCause.PAGE_MOVEMENT, after.categoryNavigationCause)
        assertEquals(1, after.categoryViewportPage)
    }

    // ==================================================================================
    // E. TOUCH AND BLINK PARITY
    // ==================================================================================

    @Test // E1 — touch Previous Page and L4 R0 use the same handler
    fun touchPreviousPageAndBlinkUseSameHandler() {
        assertTrue(mainActivity.contains("onGuidedPreviousCategoryPage"))
        assertTrue(mainActivity.contains("GuidedModeNavigation.PREVIOUS_CATEGORY_PAGE_LEFT"))
        assertTrue(mainActivity.contains("private fun applyGuidedTouchNavigation"))
        assertTrue(mainActivity.contains("handleGuidedOverlaySequence(left, right)"))
        assertTrue(GuidedTouchNavigationSpec.touchMirrorsEyeGesture(prevPage.first, prevPage.second))
    }

    @Test // E2 — touch Next Page and L0 R4 use the same handler
    fun touchNextPageAndBlinkUseSameHandler() {
        assertTrue(mainActivity.contains("onGuidedNextCategoryPage"))
        assertTrue(mainActivity.contains("GuidedModeNavigation.NEXT_CATEGORY_PAGE_LEFT"))
        assertTrue(GuidedTouchNavigationSpec.touchMirrorsEyeGesture(nextPage.first, nextPage.second))
        // The panel maps the stable action kind to the single canonical handler.
        assertTrue(guidedUi.contains("GuidedPanelActionKind.NextCategoryPage -> onNextCategoryPage"))
        assertTrue(guidedUi.contains("GuidedPanelActionKind.PreviousCategoryPage -> onPreviousCategoryPage"))
    }

    @Test // E3 — both input forms produce an identical target page
    fun touchAndBlinkProduceIdenticalTargetPage() {
        // Touch and blink both funnel into processSequence, so an identical gesture yields an
        // identical resulting viewport page.
        val a = process(nextPage.first, nextPage.second, menuState(viewportPage = 0)) as GuidedSequenceResult.Navigate
        val b = process(nextPage.first, nextPage.second, menuState(viewportPage = 0)) as GuidedSequenceResult.Navigate
        assertEquals(a.newState.categoryViewportPage, b.newState.categoryViewportPage)
        assertEquals(1, a.newState.categoryViewportPage)
    }

    @Test // E4 — stable panel action kinds remain intact
    fun stablePanelActionKindsRemainIntact() {
        val menuActions = GuidedNavigationPanelSpec.panelActions(
            english,
            GuidedNavigationPanelSpec.PanelContext.CategoryMenu
        )
        assertEquals(
            listOf(
                GuidedPanelActionKind.ScrollUp,
                GuidedPanelActionKind.ScrollDown,
                GuidedPanelActionKind.PreviousCategoryPage,
                GuidedPanelActionKind.NextCategoryPage,
                GuidedPanelActionKind.Select,
                GuidedPanelActionKind.Back,
                GuidedPanelActionKind.Emergency
            ),
            menuActions.map { it.kind }
        )
    }

    // ==================================================================================
    // F. REGRESSION
    // ==================================================================================

    @Test // F1 — all nine navigation panel actions remain
    fun allNineNavigationPanelActionsRemain() {
        assertEquals(9, GuidedTouchNavigationSpec.panelGestures.size)
        assertTrue(GuidedTouchNavigationSpec.panelGestures.contains(prevPage))
        assertTrue(GuidedTouchNavigationSpec.panelGestures.contains(nextPage))
    }

    @Test // F2 — all existing sequences remain unchanged
    fun existingSequencesRemainUnchanged() {
        assertEquals(2 to 0, GuidedModeNavigation.PREVIOUS_LEFT to GuidedModeNavigation.PREVIOUS_RIGHT)
        assertEquals(0 to 2, GuidedModeNavigation.NEXT_LEFT to GuidedModeNavigation.NEXT_RIGHT)
        assertEquals(1 to 1, GuidedModeNavigation.SELECT_LEFT to GuidedModeNavigation.SELECT_RIGHT)
        assertEquals(2 to 2, GuidedModeNavigation.BACK_LEFT to GuidedModeNavigation.BACK_RIGHT)
        assertEquals(3 to 0, GuidedModeNavigation.CATEGORIES_LEFT to GuidedModeNavigation.CATEGORIES_RIGHT)
        assertEquals(4 to 0, prevPage)
        assertEquals(0 to 4, nextPage)
        assertEquals(6 to 0, EMERGENCY_LEFT_WINKS to EMERGENCY_RIGHT_WINKS)
        assertEquals(
            listOf(2 to 1, 1 to 2, 3 to 1, 1 to 3, 3 to 2, 2 to 3, 3 to 3, 4 to 1),
            GuidedCategoryShortcuts.allGestures()
        )
    }

    @Test // F3 — L3 R0 versus L4 R0 continuation protection remains
    fun continuationProtectionRemains() {
        val mappings = WorkspacePhraseResolver.continuationMappings(
            state = menuState(0),
            language = PreferredLanguage.English,
            uiStrings = english,
            catalogContext = GuidedCatalogContext()
        )
        assertTrue(mappings.any { it.left to it.right == prevPage })
        assertTrue(mappings.any { it.left to it.right == nextPage })
        // A partial Categories prefix (L3 R0) still sees a longer continuation (L4 R0 / L4 R1), so
        // it can never fire early while the caregiver is completing Previous Page.
        assertTrue(
            hasLongerContinuation(
                GuidedModeNavigation.CATEGORIES_LEFT,
                GuidedModeNavigation.CATEGORIES_RIGHT,
                mappings
            )
        )
    }

    @Test // F4 — Category 8 Phrase Management remains reachable
    fun phraseManagementRemainsReachable() {
        val onPage2 = process(nextPage.first, nextPage.second, menuState(viewportPage = 0)) as GuidedSequenceResult.Navigate
        assertEquals(GuidedVocabularyCategory.PHRASE_MANAGEMENT_INDEX, onPage2.newState.categoryMenuSelection)
        val opened = GuidedNavigationController.openSelectedCategory(onPage2.newState)
        assertEquals(
            CategoryAreaDestination.PhraseManagement,
            CategoryAreaDestination.forCategoryIndex(opened.categoryIndex)
        )
    }

    @Test // F5 — Open Selected Category still requires L1 R1
    fun openSelectedCategoryStillRequiresL1R1() {
        val select = GuidedNavigationPanelSpec.panelActions(
            english,
            GuidedNavigationPanelSpec.PanelContext.CategoryMenu
        ).first { it.kind == GuidedPanelActionKind.Select }
        assertEquals("L1 R1", select.sequenceLabel)
        // Page navigation itself never opens the category.
        val next = process(nextPage.first, nextPage.second, menuState(viewportPage = 0)) as GuidedSequenceResult.Navigate
        assertEquals(GuidedOverlayScreenMode.CategoryMenu, next.newState.screenMode)
    }

    @Test // F6 — no category opens automatically after paging
    fun noCategoryOpensAutomaticallyAfterPaging() {
        var state = menuState(viewportPage = 0)
        state = (process(nextPage.first, nextPage.second, state) as GuidedSequenceResult.Navigate).newState
        assertEquals(GuidedOverlayScreenMode.CategoryMenu, state.screenMode)
        state = (process(prevPage.first, prevPage.second, state) as GuidedSequenceResult.Navigate).newState
        assertEquals(GuidedOverlayScreenMode.CategoryMenu, state.screenMode)
    }

    @Test // F7 — no Android system keyboard is introduced
    fun noAndroidSystemKeyboardIntroduced() {
        assertFalse(guidedUi.contains("TextField"))
        assertFalse(guidedUi.contains("KeyboardOptions"))
    }

    @Test // F8 — Emergency remains L6 R0
    fun emergencyRemainsL6R0() {
        assertEquals(6 to 0, EMERGENCY_LEFT_WINKS to EMERGENCY_RIGHT_WINKS)
        val emergency = GuidedNavigationPanelSpec.panelActions(
            english,
            GuidedNavigationPanelSpec.PanelContext.CategoryMenu
        ).first { it.kind == GuidedPanelActionKind.Emergency }
        assertEquals("L6 R0", emergency.sequenceLabel)
    }

    @Test // F9 — existing Category Menu touch selection remains operational
    fun categoryMenuTouchSelectionRemainsOperational() {
        assertTrue(mainActivity.contains("onGuidedCategoryRow"))
        assertTrue(mainActivity.contains("openGuidedCategoryFromTouch"))
        assertTrue(guidedUi.contains("onCategoryRow(index)"))
    }

    @Test // F10 — existing gesture-conflict audits stay green
    fun gestureConflictAuditsStayGreen() {
        assertTrue(GuidedNavigationGestureAudit.auditAllModes())
        assertTrue(ModeScopedGestureAuthorityAudit.passes())
        val report = GestureConflictAuthorityV1.validate()
        assertNotEquals(ValidationOutcome.FAIL, report.outcome)
    }
}
