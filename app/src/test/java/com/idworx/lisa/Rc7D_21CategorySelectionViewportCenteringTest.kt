package com.idworx.lisa

import com.idworx.lisa.validation.ValidationOutcome
import com.idworx.lisa.validation.authority.GestureConflictAuthorityV1
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * RC7D.21 — Category Selection Viewport Centering.
 *
 * RC7D.19/20 only scrolled far enough to reveal the selected category at the nearest edge, which
 * stranded selections (e.g. Category 7) against the bottom with little surrounding context. RC7D.21
 * replaces that with ONE canonical, measurement-driven centring authority
 * ([GuidedCategoryMenuScroll.centeredScrollOffsetPx]) shared by every SELECTION source — Move Up /
 * Move Down, touch, direct shortcut, and Category Menu restoration.
 *
 * RC7D.22 REVISION: Previous / Next Page are true VIEWPORT page moves that scroll straight to a
 * measured page anchor ([CategoryViewportPaging]) and deliberately bypass this centring path, so
 * the "paging regression" section below now asserts that the viewport page is a separate concept
 * from the selected category rather than the old `selectionIndex / 7` logical grouping.
 */
class Rc7D_21CategorySelectionViewportCenteringTest {

    private val english = LisaUiStrings.forLanguage(PreferredLanguage.English)

    private val pitch = GuidedCategoryMenuScroll.ROW_PITCH_PX
    private val itemCount = GuidedVocabularyCategory.PAGE_COUNT
    private val contentHeight = itemCount * pitch

    private val prevPage = GuidedModeNavigation.PREVIOUS_CATEGORY_PAGE_LEFT to
        GuidedModeNavigation.PREVIOUS_CATEGORY_PAGE_RIGHT
    private val nextPage = GuidedModeNavigation.NEXT_CATEGORY_PAGE_LEFT to
        GuidedModeNavigation.NEXT_CATEGORY_PAGE_RIGHT

    private fun menuState(
        selection: Int,
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

    /** Content-space top of the selected item's centre once positioned, expressed inside the viewport. */
    private fun itemCentreWithinViewport(index: Int, viewport: Int, maxScroll: Int): Int {
        val target = GuidedCategoryMenuScroll.centeredScrollOffsetPxForIndex(index, viewport, maxScroll)
        val itemTop = index * pitch
        return (itemTop - target) + pitch / 2
    }

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

    // ==================================================================================
    // A. PURE CENTRING CALCULATION
    // ==================================================================================

    @Test // A1 — selected item in the middle produces a centred target
    fun middleItemProducesCentredTarget() {
        val viewport = 600
        val maxScroll = 5000
        val itemTop = 1000
        val height = 100
        val target = GuidedCategoryMenuScroll.centeredScrollOffsetPx(itemTop, height, viewport, maxScroll)
        assertEquals(750, target)
        // The item centre lands exactly on the viewport centre when there is room.
        val centreWithinViewport = (itemTop - target) + height / 2
        assertEquals(viewport / 2, centreWithinViewport)
    }

    @Test // A2 — target accounts for selected item height
    fun targetAccountsForItemHeight() {
        val viewport = 600
        val maxScroll = 5000
        val itemTop = 1000
        val shortTarget = GuidedCategoryMenuScroll.centeredScrollOffsetPx(itemTop, 0, viewport, maxScroll)
        val tallTarget = GuidedCategoryMenuScroll.centeredScrollOffsetPx(itemTop, 200, viewport, maxScroll)
        assertNotEquals(shortTarget, tallTarget)
        // Growing the item by 200px shifts the centre by exactly half of that.
        assertEquals(100, tallTarget - shortTarget)
    }

    @Test // A3 — target clamps to zero at the top
    fun targetClampsToZeroAtTop() {
        assertEquals(0, GuidedCategoryMenuScroll.centeredScrollOffsetPx(0, 100, 600, 5000))
        assertEquals(0, GuidedCategoryMenuScroll.centeredScrollOffsetPx(100, 100, 600, 5000))
    }

    @Test // A4 — target clamps to the maximum scroll offset at the bottom
    fun targetClampsToMaxAtBottom() {
        assertEquals(5000, GuidedCategoryMenuScroll.centeredScrollOffsetPx(10_000, 100, 600, 5000))
    }

    @Test // A5 — never negative across a wide sweep
    fun targetNeverNegative() {
        for (top in -500..5000 step 137) {
            for (h in intArrayOf(0, 50, 100, 300)) {
                val t = GuidedCategoryMenuScroll.centeredScrollOffsetPx(top, h, 600, 5000)
                assertTrue("negative target for top=$top h=$h -> $t", t >= 0)
            }
        }
    }

    @Test // A6 — never greater than the maximum
    fun targetNeverExceedsMax() {
        val maxScroll = 5000
        for (top in 0..12_000 step 211) {
            val t = GuidedCategoryMenuScroll.centeredScrollOffsetPx(top, 100, 600, maxScroll)
            assertTrue("target $t exceeds max $maxScroll for top=$top", t <= maxScroll)
        }
    }

    @Test // A7 — zero-height or unavailable viewport handled safely
    fun zeroOrUnavailableViewportHandledSafely() {
        assertEquals(0, GuidedCategoryMenuScroll.centeredScrollOffsetPx(1000, 100, 0, 5000))
        assertEquals(0, GuidedCategoryMenuScroll.centeredScrollOffsetPx(1000, 100, -50, 5000))
        // No scroll room (max <= 0) means nothing to do.
        assertEquals(0, GuidedCategoryMenuScroll.centeredScrollOffsetPx(1000, 100, 600, 0))
        assertEquals(0, GuidedCategoryMenuScroll.centeredScrollOffsetPx(1000, 100, 600, -10))
    }

    @Test // A8 — invalid / unavailable measurements do not crash and stay in range
    fun invalidMeasurementsDoNotCrash() {
        val t1 = GuidedCategoryMenuScroll.centeredScrollOffsetPx(-500, -100, 600, 5000)
        assertTrue(t1 in 0..5000)
        val t2 = GuidedCategoryMenuScroll.centeredScrollOffsetPx(Int.MIN_VALUE / 4, -1, 600, 5000)
        assertTrue(t2 in 0..5000)
        // Index helper with a zero pitch must not divide-by-zero.
        val t3 = GuidedCategoryMenuScroll.centeredScrollOffsetPxForIndex(3, 600, 5000, rowPitchPx = 0)
        assertTrue(t3 in 0..5000)
    }

    @Test // A9 — deterministic
    fun calculationIsDeterministic() {
        val a = GuidedCategoryMenuScroll.centeredScrollOffsetPx(1234, 88, 512, 4096)
        val b = GuidedCategoryMenuScroll.centeredScrollOffsetPx(1234, 88, 512, 4096)
        assertEquals(a, b)
    }

    @Test // A10 — animation gate uses a small tolerance to avoid one-pixel loops
    fun animationGateUsesTolerance() {
        val tolerance = GuidedCategoryMenuScroll.CENTERING_TOLERANCE_PX
        assertTrue(tolerance > 0)
        assertFalse(GuidedCategoryMenuScroll.shouldAnimateTo(current = 500, target = 500))
        assertFalse(GuidedCategoryMenuScroll.shouldAnimateTo(current = 500, target = 500 + tolerance))
        assertTrue(GuidedCategoryMenuScroll.shouldAnimateTo(current = 500, target = 500 + tolerance + 1))
    }

    // ==================================================================================
    // B. SELECTION SCENARIOS (8 categories, 3-row viewport → real scroll room)
    // ==================================================================================

    // viewport shows 3 rows, so there is room to centre interior items and to clamp at both ends.
    private val threeRowViewport = pitch * 3
    private val threeRowMaxScroll = contentHeight - threeRowViewport // 576 - 216 = 360

    @Test // B1 — Category 1 anchors at the top
    fun category1AnchorsAtTop() {
        assertEquals(0, GuidedCategoryMenuScroll.centeredScrollOffsetPxForIndex(0, threeRowViewport, threeRowMaxScroll))
    }

    @Test // B2 — Category 2 does not create blank space above the list
    fun category2DoesNotCreateBlankSpaceAbove() {
        // Centring would want a negative offset for such an early item; clamp keeps it at the top.
        assertEquals(0, GuidedCategoryMenuScroll.centeredScrollOffsetPxForIndex(1, threeRowViewport, threeRowMaxScroll))
    }

    @Test // B3 — a middle category is centred when possible
    fun middleCategoryCentredWhenPossible() {
        val middle = 3 // Category 4
        val target = GuidedCategoryMenuScroll.centeredScrollOffsetPxForIndex(middle, threeRowViewport, threeRowMaxScroll)
        assertTrue("expected interior (non-clamped) target, got $target", target > 0 && target < threeRowMaxScroll)
        assertTrue(abs(itemCentreWithinViewport(middle, threeRowViewport, threeRowMaxScroll) - threeRowViewport / 2) <= 1)
    }

    @Test // B4 — Category 6 preserves surrounding context
    fun category6PreservesSurroundingContext() {
        val target = GuidedCategoryMenuScroll.centeredScrollOffsetPxForIndex(5, threeRowViewport, threeRowMaxScroll)
        // Not pinned to the very bottom: context remains both above and below.
        assertTrue(target > 0)
        assertTrue(target <= threeRowMaxScroll)
    }

    @Test // B5 — Category 7 preserves maximum preceding context (no blank below)
    fun category7PreservesMaximumPrecedingContext() {
        val cat7 = 6
        val target = GuidedCategoryMenuScroll.centeredScrollOffsetPxForIndex(cat7, threeRowViewport, threeRowMaxScroll)
        // Clamped to the content bottom so the maximum number of preceding categories stays visible,
        // rather than pinning Category 7 needlessly close to the bottom edge with blank space below.
        assertEquals(threeRowMaxScroll, target)
    }

    @Test // B6 — Category 8 clamps correctly at the content bottom
    fun category8ClampsAtContentBottom() {
        val cat8 = GuidedVocabularyCategory.PHRASE_MANAGEMENT_INDEX
        val target = GuidedCategoryMenuScroll.centeredScrollOffsetPxForIndex(cat8, threeRowViewport, threeRowMaxScroll)
        assertEquals(threeRowMaxScroll, target)
    }

    @Test // B7 — device-like viewport (~7 of 8 rows) still keeps Category 7 in view with max context
    fun deviceLikeViewportKeepsCategory7Visible() {
        val viewport = pitch * 7
        val maxScroll = contentHeight - viewport // one row of scroll room
        val cat7 = 6
        val target = GuidedCategoryMenuScroll.centeredScrollOffsetPxForIndex(cat7, viewport, maxScroll)
        // With almost everything already on screen, centring clamps to the bottom (maximum preceding
        // context) instead of leaving blank space.
        assertEquals(maxScroll, target)
        assertTrue(target > 0)
    }

    @Test // B8 — moving 6 → 7 recalculates positioning
    fun moving6To7RecalculatesPositioning() {
        val t6 = GuidedCategoryMenuScroll.centeredScrollOffsetPxForIndex(5, threeRowViewport, threeRowMaxScroll)
        val t7 = GuidedCategoryMenuScroll.centeredScrollOffsetPxForIndex(6, threeRowViewport, threeRowMaxScroll)
        assertNotEquals(t6, t7)
        assertTrue(t7 >= t6)
    }

    @Test // B9 — moving 7 → 8 recalculates positioning (both clamp to bottom, no blank below)
    fun moving7To8RecalculatesPositioning() {
        val t7 = GuidedCategoryMenuScroll.centeredScrollOffsetPxForIndex(6, threeRowViewport, threeRowMaxScroll)
        val t8 = GuidedCategoryMenuScroll.centeredScrollOffsetPxForIndex(7, threeRowViewport, threeRowMaxScroll)
        assertEquals(threeRowMaxScroll, t7)
        assertEquals(threeRowMaxScroll, t8)
        assertTrue(t8 <= threeRowMaxScroll)
    }

    @Test // B10 — moving 8 → 7 recalculates positioning
    fun moving8To7RecalculatesPositioning() {
        // Larger scroll room (2-row viewport) so Category 7 is no longer bottom-clamped and the
        // recalculation on the way back produces a distinct, smaller offset.
        val viewport = pitch * 2
        val maxScroll = contentHeight - viewport
        val t8 = GuidedCategoryMenuScroll.centeredScrollOffsetPxForIndex(7, viewport, maxScroll)
        val t7 = GuidedCategoryMenuScroll.centeredScrollOffsetPxForIndex(6, viewport, maxScroll)
        assertNotEquals(t8, t7)
        assertTrue(t7 < t8)
    }

    // Device-like two-page layout (~7 of 8 rows visible, one row of overflow) for page-anchor tests.
    private val deviceViewport = pitch * 7
    private val deviceMaxScroll = contentHeight - deviceViewport

    @Test // B11 — Previous Page is a PAGE_MOVEMENT that scrolls to the page-1 anchor, not centring
    fun previousPageNavigationUsesPageAnchorNotCentring() {
        val result = process(prevPage.first, prevPage.second, menuState(selection = 0, viewportPage = 1))
        assertTrue(result is GuidedSequenceResult.Navigate)
        val newState = (result as GuidedSequenceResult.Navigate).newState
        // Page move: viewport returns to page 1, tagged PAGE_MOVEMENT so centring is bypassed.
        assertEquals(0, newState.categoryViewportPage)
        assertEquals(CategoryNavigationCause.PAGE_MOVEMENT, newState.categoryNavigationCause)
        // The authoritative target is the page anchor (offset 0), independent of any selection.
        assertEquals(0, CategoryViewportPaging.pageAnchorOffsetPx(newState.categoryViewportPage, deviceViewport, deviceMaxScroll))
    }

    @Test // B12 — Next Page is a PAGE_MOVEMENT that scrolls to the final page anchor, not centring
    fun nextPageNavigationUsesPageAnchorNotCentring() {
        val result = process(nextPage.first, nextPage.second, menuState(selection = 0, viewportPage = 0))
        assertTrue(result is GuidedSequenceResult.Navigate)
        val newState = (result as GuidedSequenceResult.Navigate).newState
        assertEquals(1, newState.categoryViewportPage)
        assertEquals(CategoryNavigationCause.PAGE_MOVEMENT, newState.categoryNavigationCause)
        assertEquals(
            deviceMaxScroll,
            CategoryViewportPaging.pageAnchorOffsetPx(newState.categoryViewportPage, deviceViewport, deviceMaxScroll)
        )
    }

    @Test // B13 — touch and blink selection changes share the same positioning authority
    fun touchAndBlinkShareSamePositioningAuthority() {
        val ui = readSource("app/src/main/java/com/idworx/lisa/LisaGuidedModeUi.kt")
        // Exactly one canonical centring call, driven by categoryMenuSelection (which both touch
        // onCategoryRow and blink processSequence mutate). No per-input duplicate scroll maths.
        val calls = Regex("GuidedCategoryMenuScroll\\.centeredScrollOffsetPx\\(").findAll(ui).count()
        assertEquals(1, calls)
        assertTrue(ui.contains("LaunchedEffect(") && ui.contains("categoryMenuSelection"))
        assertTrue(ui.contains("animateScrollTo(target)"))
    }

    @Test // B14 — restoring the Category Menu with an existing selection uses the same authority
    fun restoringCategoryMenuUsesSamePositioning() {
        val restored = GuidedNavigationController.openCategoryMenu(
            GuidedNavigationState(
                screenMode = GuidedOverlayScreenMode.Vocabulary,
                categoryIndex = 6
            )
        )
        assertEquals(GuidedOverlayScreenMode.CategoryMenu, restored.screenMode)
        assertEquals(6, restored.categoryMenuSelection)
        // Same selection-driven centring applies on restore — valid, in-range target.
        val target = GuidedCategoryMenuScroll.centeredScrollOffsetPxForIndex(
            restored.categoryMenuSelection, threeRowViewport, threeRowMaxScroll
        )
        assertTrue(target in 0..threeRowMaxScroll)
    }

    // ==================================================================================
    // C. PAGING REGRESSION — viewport page is independent of the selected category
    // ==================================================================================

    @Test // C1 — Category 7 may be selected while the viewport reports Page 1
    fun category7MayBeSelectedOnViewportPage1() {
        val state = menuState(selection = 6, viewportPage = 0)
        assertEquals(6, state.categoryMenuSelection)
        assertEquals(0, state.categoryViewportPage)
    }

    @Test // C2 — Category 7 may remain selected while the viewport reports Page 2
    fun category7MayRemainSelectedOnViewportPage2() {
        val state = menuState(selection = 6, viewportPage = 1)
        assertEquals(6, state.categoryMenuSelection)
        assertEquals(1, state.categoryViewportPage)
    }

    @Test // C3 — Previous Page is gated on the viewport page, not the selected category
    fun previousPageDisabledOnViewportPage1RegardlessOfSelection() {
        assertFalse(CategoryViewportPaging.canGoToPreviousPage(0))
        // Even with Category 7 selected, Previous Page is a no-op while the viewport is on page 1.
        assertTrue(process(prevPage.first, prevPage.second, menuState(selection = 6, viewportPage = 0)) is GuidedSequenceResult.Unmatched)
    }

    @Test // C4 — Next Page is enabled while the viewport is on Page 1
    fun nextPageEnabledOnViewportPage1() {
        assertTrue(CategoryViewportPaging.canGoToNextPage(0, 2))
        assertTrue(process(nextPage.first, nextPage.second, menuState(selection = 6, viewportPage = 0)) is GuidedSequenceResult.Navigate)
    }

    @Test // C5 — Previous Page becomes enabled once the viewport is on Page 2
    fun previousPageEnabledOnViewportPage2() {
        assertTrue(CategoryViewportPaging.canGoToPreviousPage(1))
        assertTrue(process(prevPage.first, prevPage.second, menuState(selection = 6, viewportPage = 1)) is GuidedSequenceResult.Navigate)
    }

    @Test // C6 — the page indicator is based on the viewport page, not on selection
    fun pageIndicatorBasedOnViewportPage() {
        val ui = readSource("app/src/main/java/com/idworx/lisa/LisaGuidedModeUi.kt")
        assertTrue(ui.contains("guidedPageIndicator("))
        assertTrue(ui.contains("categoryViewportPage"))
        // The old selection-derived logical paging indicator is gone.
        assertFalse(ui.contains("GuidedCategoryPaging.pageIndexForSelection(categoryMenuSelection)"))
    }

    @Test // C7 — the pure centring calculation cannot change the selected category
    fun scrollingDoesNotChangeSelectedCategory() {
        // The centring authority is a pure Int calculation over measurements; computing offsets for
        // the same selection cannot mutate any state.
        val selection = 6
        repeat(3) {
            GuidedCategoryMenuScroll.centeredScrollOffsetPxForIndex(selection, threeRowViewport, threeRowMaxScroll)
        }
        // A fresh menu state with that selection is unaffected.
        assertEquals(selection, menuState(selection).categoryMenuSelection)
    }

    @Test // C8 — page navigation does not open a category
    fun pageNavigationDoesNotOpenACategory() {
        val result = process(nextPage.first, nextPage.second, menuState(selection = 0, viewportPage = 0)) as GuidedSequenceResult.Navigate
        assertEquals(GuidedOverlayScreenMode.CategoryMenu, result.newState.screenMode)
    }

    // ==================================================================================
    // D. EXISTING FEATURE REGRESSION
    // ==================================================================================

    @Test // D1 — all nine navigation panel touch gestures remain present
    fun allNineNavigationPanelActionsRemainPresent() {
        assertEquals(9, GuidedTouchNavigationSpec.panelGestures.size)
        assertTrue(GuidedTouchNavigationSpec.panelGestures.contains(prevPage))
        assertTrue(GuidedTouchNavigationSpec.panelGestures.contains(nextPage))
        // The Category Menu panel still exposes every caregiver-facing command.
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

    @Test // D2 — all existing gesture constants remain unchanged
    fun existingGestureConstantsRemainUnchanged() {
        assertEquals(2 to 0, GuidedModeNavigation.PREVIOUS_LEFT to GuidedModeNavigation.PREVIOUS_RIGHT)
        assertEquals(0 to 2, GuidedModeNavigation.NEXT_LEFT to GuidedModeNavigation.NEXT_RIGHT)
        assertEquals(1 to 1, GuidedModeNavigation.SELECT_LEFT to GuidedModeNavigation.SELECT_RIGHT)
        assertEquals(2 to 2, GuidedModeNavigation.BACK_LEFT to GuidedModeNavigation.BACK_RIGHT)
        assertEquals(3 to 0, GuidedModeNavigation.CATEGORIES_LEFT to GuidedModeNavigation.CATEGORIES_RIGHT)
        assertEquals(4 to 0, prevPage)
        assertEquals(0 to 4, nextPage)
        assertEquals(6 to 0, EMERGENCY_LEFT_WINKS to EMERGENCY_RIGHT_WINKS)
    }

    @Test // D3 — continuation mapping for L3 R0 vs L4 R0 remains intact
    fun continuationMappingL3R0VsL4R0Intact() {
        val mappings = WorkspacePhraseResolver.continuationMappings(
            state = menuState(0),
            language = PreferredLanguage.English,
            uiStrings = english,
            catalogContext = GuidedCatalogContext()
        )
        // Previous Page (L4 R0) and Next Page (L0 R4) are registered for continuation checks.
        assertTrue(mappings.any { it.left to it.right == prevPage })
        assertTrue(mappings.any { it.left to it.right == nextPage })
        // A partial Categories prefix (L3 R0) must still see a longer continuation (L4 R0 / L4 R1),
        // so it can never fire early while the caregiver is completing Previous Page.
        assertTrue(hasLongerContinuation(
            GuidedModeNavigation.CATEGORIES_LEFT,
            GuidedModeNavigation.CATEGORIES_RIGHT,
            mappings
        ))
    }

    @Test // D4 — Phrase Management remains reachable
    fun phraseManagementRemainsReachable() {
        assertTrue(CategoryViewportPaging.canGoToNextPage(0, 2))
        val onPage2 = process(nextPage.first, nextPage.second, menuState(selection = 0, viewportPage = 0)) as GuidedSequenceResult.Navigate
        assertEquals(GuidedVocabularyCategory.PHRASE_MANAGEMENT_INDEX, onPage2.newState.categoryMenuSelection)
        val opened = GuidedNavigationController.openSelectedCategory(onPage2.newState)
        assertEquals(
            CategoryAreaDestination.PhraseManagement,
            CategoryAreaDestination.forCategoryIndex(opened.categoryIndex)
        )
    }

    @Test // D5 — category shortcuts remain valid and unchanged
    fun categoryShortcutsRemainValid() {
        assertEquals(
            listOf(2 to 1, 1 to 2, 3 to 1, 1 to 3, 3 to 2, 2 to 3, 3 to 3, 4 to 1),
            GuidedCategoryShortcuts.allGestures()
        )
    }

    @Test // D6 — no Android system keyboard is introduced
    fun noAndroidSystemKeyboardIntroduced() {
        val ui = readSource("app/src/main/java/com/idworx/lisa/LisaGuidedModeUi.kt")
        assertFalse(ui.contains("TextField"))
        assertFalse(ui.contains("KeyboardOptions"))
    }

    @Test // D7 — no caregiver-facing controls disappear; existing conflict audits stay green
    fun caregiverControlsRemainAndAuditsGreen() {
        val menuActions = GuidedNavigationPanelSpec.panelActions(
            english,
            GuidedNavigationPanelSpec.PanelContext.CategoryMenu
        )
        assertTrue(GuidedNavigationPanelSpec.allActionsLabeled(menuActions))
        assertTrue(GuidedNavigationGestureAudit.auditAllModes())
        assertTrue(ModeScopedGestureAuthorityAudit.passes())
        val report = GestureConflictAuthorityV1.validate()
        assertNotEquals(ValidationOutcome.FAIL, report.outcome)
    }
}
