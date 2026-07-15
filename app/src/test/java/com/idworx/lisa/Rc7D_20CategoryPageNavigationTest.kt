package com.idworx.lisa

import com.idworx.lisa.validation.ValidationOutcome
import com.idworx.lisa.validation.authority.GestureConflictAuthorityV1
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * RC7D.20 — Category Page Navigation.
 *
 * The Category Menu keeps item-by-item Move Up (L2 R0) / Move Down (L0 R2) and adds whole-page
 * jumps Previous Page (L4 R0) / Next Page (L0 R4). Touch and blink share the same canonical
 * controller handlers.
 *
 * RC7D.22 REVISION: pages are now genuine VIEWPORT pages (measured scroll windows via
 * [CategoryViewportPaging]) rather than `selectionIndex / pageSize` logical groupings, so the
 * paging assertions below assert viewport-page transitions and measured anchors. The device-like
 * screen (eight categories, ~seven visible, one viewport of overflow) is a two-page menu.
 */
class Rc7D_20CategoryPageNavigationTest {

    private val english = LisaUiStrings.forLanguage(PreferredLanguage.English)

    // Device-like single-overflow layout: ~7 rows visible of 8, one viewport of scroll room.
    private val viewport = GuidedCategoryMenuScroll.ROW_PITCH_PX * 7
    private val maxScroll = GuidedCategoryMenuScroll.ROW_PITCH_PX

    private val menuActions = GuidedNavigationPanelSpec.panelActions(
        english,
        GuidedNavigationPanelSpec.PanelContext.CategoryMenu
    )

    private fun action(kind: GuidedPanelActionKind): GuidedNavPanelAction =
        menuActions.first { it.kind == kind }

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

    private val prevPage = GuidedModeNavigation.PREVIOUS_CATEGORY_PAGE_LEFT to
        GuidedModeNavigation.PREVIOUS_CATEGORY_PAGE_RIGHT
    private val nextPage = GuidedModeNavigation.NEXT_CATEGORY_PAGE_LEFT to
        GuidedModeNavigation.NEXT_CATEGORY_PAGE_RIGHT

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

    // ===================== BUTTONS AND LABELS =====================

    @Test // 1
    fun categoriesRendersPreviousPage() {
        assertEquals(english.guidedPreviousCategoryPage, action(GuidedPanelActionKind.PreviousCategoryPage).title)
    }

    @Test // 2
    fun previousPageRendersL4R0() {
        assertEquals("L4 R0", action(GuidedPanelActionKind.PreviousCategoryPage).sequenceLabel)
    }

    @Test // 3
    fun categoriesRendersNextPage() {
        assertEquals(english.guidedNextCategoryPage, action(GuidedPanelActionKind.NextCategoryPage).title)
    }

    @Test // 4
    fun nextPageRendersL0R4() {
        assertEquals("L0 R4", action(GuidedPanelActionKind.NextCategoryPage).sequenceLabel)
    }

    @Test // 5
    fun moveUpRemainsL2R0() {
        val moveUp = action(GuidedPanelActionKind.ScrollUp)
        assertEquals("L2 R0", moveUp.sequenceLabel)
        assertEquals(english.guidedMoveUpCategory, moveUp.title)
    }

    @Test // 6
    fun moveDownRemainsL0R2() {
        val moveDown = action(GuidedPanelActionKind.ScrollDown)
        assertEquals("L0 R2", moveDown.sequenceLabel)
        assertEquals(english.guidedMoveDownCategory, moveDown.title)
    }

    @Test // 7
    fun openSelectedCategoryRemainsL1R1() {
        val select = action(GuidedPanelActionKind.Select)
        assertEquals("L1 R1", select.sequenceLabel)
        assertEquals(english.guidedOpenSelectedCategory, select.title)
    }

    @Test // 8
    fun backRemainsL2R2() {
        val back = action(GuidedPanelActionKind.Back)
        assertEquals("L2 R2", back.sequenceLabel)
        assertEquals(english.guidedBackToPhrases, back.title)
    }

    @Test // 9
    fun emergencyRemainsL6R0() {
        assertEquals("L6 R0", action(GuidedPanelActionKind.Emergency).sequenceLabel)
    }

    @Test
    fun categoryMenuPanelRendersAllSevenControls() {
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
        assertTrue(GuidedNavigationPanelSpec.allActionsLabeled(menuActions))
    }

    @Test
    fun vocabularyPanelOrderingUnchanged() {
        val vocab = GuidedNavigationPanelSpec.panelActions(
            english,
            GuidedNavigationPanelSpec.PanelContext.Vocabulary
        )
        // Other auditors depend on this exact positional layout.
        assertEquals(6, vocab.size)
        assertEquals(GuidedPanelActionKind.ScrollUp, vocab[0].kind)
        assertEquals(GuidedPanelActionKind.Select, vocab[1].kind)
        assertEquals(GuidedPanelActionKind.Back, vocab[2].kind)
        assertEquals(GuidedPanelActionKind.Categories, vocab[3].kind)
        assertEquals(GuidedPanelActionKind.Emergency, vocab[4].kind)
        assertEquals(GuidedPanelActionKind.ScrollDown, vocab[5].kind)
    }

    // ===================== PAGE NAVIGATION (viewport pages) =====================

    @Test // 10
    fun page1ReportsPage1Of2() {
        // A single viewport of overflow is a two-page menu; the top scroll position is page 1.
        assertEquals(2, CategoryViewportPaging.pageCount(viewport, maxScroll))
        assertEquals(0, CategoryViewportPaging.currentPageForScroll(0, viewport, maxScroll))
    }

    @Test // 11
    fun nextPageMovesDirectlyToPage2() {
        val result = process(nextPage.first, nextPage.second, menuState(viewportPage = 0))
        assertTrue(result is GuidedSequenceResult.Navigate)
        // Directly advances the viewport page — not a category-selection step.
        assertEquals(1, (result as GuidedSequenceResult.Navigate).newState.categoryViewportPage)
    }

    @Test // 12
    fun phraseManagementSelectedAndVisibleAfterNextPage() {
        val result = process(nextPage.first, nextPage.second, menuState(viewportPage = 0)) as GuidedSequenceResult.Navigate
        // Decorative highlight lands on the final category, and the page-2 anchor reveals the bottom
        // of the list (maxScroll) where Phrase Management lives.
        assertEquals(GuidedVocabularyCategory.PHRASE_MANAGEMENT_INDEX, result.newState.categoryMenuSelection)
        assertEquals(
            maxScroll,
            CategoryViewportPaging.pageAnchorOffsetPx(result.newState.categoryViewportPage, viewport, maxScroll)
        )
    }

    @Test // 13
    fun nextPageDoesNotOpenPhraseManagement() {
        val result = process(nextPage.first, nextPage.second, menuState(viewportPage = 0)) as GuidedSequenceResult.Navigate
        assertEquals(GuidedOverlayScreenMode.CategoryMenu, result.newState.screenMode)
    }

    @Test // 14
    fun selectStillRequiredToOpenPhraseManagement() {
        val onPage2 = process(nextPage.first, nextPage.second, menuState(viewportPage = 0)) as GuidedSequenceResult.Navigate
        val opened = process(
            GuidedModeNavigation.SELECT_LEFT,
            GuidedModeNavigation.SELECT_RIGHT,
            onPage2.newState
        ) as GuidedSequenceResult.Navigate
        assertEquals(GuidedOverlayScreenMode.Vocabulary, opened.newState.screenMode)
        assertEquals(
            CategoryAreaDestination.PhraseManagement,
            CategoryAreaDestination.forCategoryIndex(opened.newState.categoryIndex)
        )
    }

    @Test // 15
    fun previousPageMovesBackToPage1() {
        val result = process(prevPage.first, prevPage.second, menuState(viewportPage = 1))
        assertTrue(result is GuidedSequenceResult.Navigate)
        // Returns directly to the top viewport page (page 1), anchored at offset 0.
        assertEquals(0, (result as GuidedSequenceResult.Navigate).newState.categoryViewportPage)
        assertEquals(0, CategoryViewportPaging.pageAnchorOffsetPx(result.newState.categoryViewportPage, viewport, maxScroll))
    }

    @Test // 16
    fun previousPageSelectsCategoryOneForAUsableHighlight() {
        val result = process(prevPage.first, prevPage.second, menuState(viewportPage = 1)) as GuidedSequenceResult.Navigate
        // Category 1 — visible at the top of page 1 and cannot pull centring back toward page 2.
        assertEquals(0, result.newState.categoryMenuSelection)
    }

    @Test // 17
    fun nextPageInactiveOnFinalPage() {
        val result = process(nextPage.first, nextPage.second, menuState(viewportPage = 1, viewportPageCount = 2))
        assertTrue(result is GuidedSequenceResult.Unmatched)
        assertFalse(CategoryViewportPaging.canGoToNextPage(currentPage = 1, pageCount = 2))
    }

    @Test // 18
    fun previousPageInactiveOnFirstPage() {
        val result = process(prevPage.first, prevPage.second, menuState(viewportPage = 0))
        assertTrue(result is GuidedSequenceResult.Unmatched)
        assertFalse(CategoryViewportPaging.canGoToPreviousPage(0))
    }

    @Test // 19
    fun disabledPageActionsDoNotChangeSelection() {
        assertTrue(process(prevPage.first, prevPage.second, menuState(viewportPage = 0)) is GuidedSequenceResult.Unmatched)
        assertTrue(
            process(nextPage.first, nextPage.second, menuState(viewportPage = 1, viewportPageCount = 2))
                is GuidedSequenceResult.Unmatched
        )
    }

    @Test // 20
    fun pageIndicesCannotMoveOutsideValidBounds() {
        // Next from the last page and Previous from the first page are clamped no-ops — never wrap.
        assertEquals(
            1,
            GuidedNavigationController.nextCategoryPage(menuState(viewportPage = 1, viewportPageCount = 2)).categoryViewportPage
        )
        assertEquals(
            0,
            GuidedNavigationController.previousCategoryPage(menuState(viewportPage = 0)).categoryViewportPage
        )
        // Anchors always stay within the valid scroll range regardless of the requested page.
        for (page in -2..5) {
            assertTrue(CategoryViewportPaging.pageAnchorOffsetPx(page, viewport, maxScroll) in 0..maxScroll)
        }
    }

    // ===================== TOUCH AND BLINK =====================

    @Test // 21 + 22
    fun touchAndBlinkUseSameCanonicalHandler() {
        val main = readSource("app/src/main/java/com/idworx/lisa/MainActivity.kt")
        assertTrue(main.contains("onGuidedNextCategoryPage"))
        assertTrue(main.contains("onGuidedPreviousCategoryPage"))
        assertTrue(main.contains("GuidedModeNavigation.NEXT_CATEGORY_PAGE_LEFT"))
        assertTrue(main.contains("GuidedModeNavigation.PREVIOUS_CATEGORY_PAGE_LEFT"))
        // Both touch handlers route through applyGuidedTouchNavigation -> handleGuidedOverlaySequence,
        // exactly like a finalized blink sequence.
        assertTrue(main.contains("private fun applyGuidedTouchNavigation"))
        assertTrue(main.contains("handleGuidedOverlaySequence(left, right)"))
        assertTrue(GuidedTouchNavigationSpec.touchMirrorsEyeGesture(nextPage.first, nextPage.second))
        assertTrue(GuidedTouchNavigationSpec.touchMirrorsEyeGesture(prevPage.first, prevPage.second))
    }

    @Test // 23
    fun nextPageAcceptedWhileAvailable() {
        assertTrue(process(nextPage.first, nextPage.second, menuState(viewportPage = 0)) is GuidedSequenceResult.Navigate)
    }

    @Test // 24
    fun previousPageAcceptedWhileAvailable() {
        assertTrue(process(prevPage.first, prevPage.second, menuState(viewportPage = 1)) is GuidedSequenceResult.Navigate)
    }

    @Test // 25
    fun pageSequencesDoNotLeakToAnotherMode() {
        val vocabularyState = GuidedNavigationState(
            screenMode = GuidedOverlayScreenMode.Vocabulary,
            categoryIndex = 0
        )
        assertTrue(process(nextPage.first, nextPage.second, vocabularyState) is GuidedSequenceResult.Unmatched)
        assertTrue(process(prevPage.first, prevPage.second, vocabularyState) is GuidedSequenceResult.Unmatched)
        // Mode-scoped: page jumps belong to the Category Menu namespace only.
        val menuNamespace = ModeScopedGestureAuthority.namespaceFor(LisaInteractionMode.CommunicationCategoryMenu)
        assertTrue(menuNamespace.any { it.left to it.right == nextPage })
        assertTrue(menuNamespace.any { it.left to it.right == prevPage })
        val vocabNamespace = ModeScopedGestureAuthority.namespaceFor(LisaInteractionMode.CommunicationVocabulary)
        assertFalse(vocabNamespace.any { it.left to it.right == nextPage })
        assertFalse(vocabNamespace.any { it.left to it.right == prevPage })
    }

    @Test // 26
    fun oneSequencePerformsOneNavigationAction() {
        val afterOne = process(nextPage.first, nextPage.second, menuState(viewportPage = 0)) as GuidedSequenceResult.Navigate
        // Exactly one viewport-page advance, never a cascade of steps.
        assertEquals(1, afterOne.newState.categoryViewportPage)
    }

    // ===================== ITEM NAVIGATION =====================

    @Test // 27
    fun moveDownStillMovesOneCategoryDown() {
        val result = process(
            GuidedModeNavigation.NEXT_LEFT,
            GuidedModeNavigation.NEXT_RIGHT,
            menuState(selection = 0)
        ) as GuidedSequenceResult.Navigate
        assertEquals(1, result.newState.categoryMenuSelection)
        assertEquals(CategoryNavigationCause.ITEM_MOVEMENT, result.newState.categoryNavigationCause)
    }

    @Test // 28
    fun moveUpStillMovesOneCategoryUp() {
        val result = process(
            GuidedModeNavigation.PREVIOUS_LEFT,
            GuidedModeNavigation.PREVIOUS_RIGHT,
            menuState(selection = 3)
        ) as GuidedSequenceResult.Navigate
        assertEquals(2, result.newState.categoryMenuSelection)
        assertEquals(CategoryNavigationCause.ITEM_MOVEMENT, result.newState.categoryNavigationCause)
    }

    @Test // 29
    fun itemNavigationCanCrossPageBoundary() {
        // Custom (index 6) -> Move Down -> Phrase Management (index 7), one item at a time.
        val result = process(
            GuidedModeNavigation.NEXT_LEFT,
            GuidedModeNavigation.NEXT_RIGHT,
            menuState(selection = GuidedVocabularyCategory.CUSTOM_CATEGORY_INDEX)
        ) as GuidedSequenceResult.Navigate
        assertEquals(GuidedVocabularyCategory.PHRASE_MANAGEMENT_INDEX, result.newState.categoryMenuSelection)
        // Item movement is not page movement.
        assertEquals(CategoryNavigationCause.ITEM_MOVEMENT, result.newState.categoryNavigationCause)
    }

    @Test // 30
    fun category8ReachableItemByItem() {
        var state = menuState(selection = 0)
        repeat(GuidedVocabularyCategory.PAGE_COUNT - 1) {
            state = (process(GuidedModeNavigation.NEXT_LEFT, GuidedModeNavigation.NEXT_RIGHT, state)
                as GuidedSequenceResult.Navigate).newState
        }
        assertEquals(GuidedVocabularyCategory.PHRASE_MANAGEMENT_INDEX, state.categoryMenuSelection)
    }

    @Test // 31
    fun category8ReachableThroughNextPage() {
        val result = process(nextPage.first, nextPage.second, menuState(viewportPage = 0)) as GuidedSequenceResult.Navigate
        assertEquals(GuidedVocabularyCategory.PHRASE_MANAGEMENT_INDEX, result.newState.categoryMenuSelection)
    }

    @Test // 32
    fun selectionRemainsVisibleAfterItemMovement() {
        val itemViewport = GuidedCategoryMenuScroll.ROW_PITCH_PX * 6
        val itemMaxScroll = GuidedCategoryMenuScroll.ROW_PITCH_PX * 2
        val offsetAtStart = GuidedCategoryMenuScroll.centeredScrollOffsetPxForIndex(0, itemViewport, itemMaxScroll)
        val offsetAtEnd = GuidedCategoryMenuScroll.centeredScrollOffsetPxForIndex(
            GuidedVocabularyCategory.PHRASE_MANAGEMENT_INDEX,
            itemViewport,
            itemMaxScroll
        )
        assertTrue(offsetAtEnd >= offsetAtStart)
    }

    // ===================== INDICATORS =====================

    @Test // 33
    fun selectedCategoryPositionRemainsAccurate() {
        val ui = readSource("app/src/main/java/com/idworx/lisa/LisaGuidedModeUi.kt")
        // Indicator category number tracks the highlighted menu selection, not the last-opened index.
        assertTrue(ui.contains("guidedCategoryIndicator(categoryMenuSelection + 1"))
    }

    @Test // 34
    fun pagePositionRemainsAccurate() {
        // The page indicator now reports the VIEWPORT page (categoryViewportPage), independent of
        // the selected category.
        assertEquals(1, menuState(viewportPage = 0).categoryViewportPage + 1)
        assertEquals(2, menuState(viewportPage = 1).categoryViewportPage + 1)
        val ui = readSource("app/src/main/java/com/idworx/lisa/LisaGuidedModeUi.kt")
        assertTrue(ui.contains("guidedPageIndicator("))
        assertTrue(ui.contains("categoryViewportPage"))
    }

    @Test // 35
    fun totalCategoryCountMatchesCanonicalDestinationCount() {
        assertEquals(GuidedVocabularyCategory.ordered.size, GuidedVocabularyCategory.PAGE_COUNT)
        assertEquals(8, GuidedVocabularyCategory.PAGE_COUNT)
    }

    @Test // 36
    fun totalPageCountIsCalculatedNotHardcoded() {
        // Derived from measured viewport + scroll overflow, never a fixed page size of seven.
        assertEquals(1, CategoryViewportPaging.pageCount(viewportHeightPx = 1000, maxScrollPx = 0))
        assertEquals(2, CategoryViewportPaging.pageCount(viewportHeightPx = 1000, maxScrollPx = 200))
        assertEquals(2, CategoryViewportPaging.pageCount(viewportHeightPx = 1000, maxScrollPx = 1000))
        assertEquals(3, CategoryViewportPaging.pageCount(viewportHeightPx = 1000, maxScrollPx = 1001))
        assertEquals(3, CategoryViewportPaging.pageCount(viewportHeightPx = 500, maxScrollPx = 1000))
    }

    // ===================== REGRESSION =====================

    @Test // 37
    fun phraseManagementRemainsOutsideChooseCategory() {
        assertFalse(
            CategoryAreaDestination.isAssignableCommunicationCategory(GuidedVocabularyCategory.PhraseManagement)
        )
        assertTrue(CategoryAreaDestination.isManagementDestination(GuidedVocabularyCategory.PhraseManagement))
    }

    @Test // 38
    fun categoryShortcutsRemainUnchanged() {
        assertEquals(
            listOf(2 to 1, 1 to 2, 3 to 1, 1 to 3, 3 to 2, 2 to 3, 3 to 3, 4 to 1),
            GuidedCategoryShortcuts.allGestures()
        )
        assertFalse(GuidedCategoryShortcuts.allGestures().contains(prevPage))
        assertFalse(GuidedCategoryShortcuts.allGestures().contains(nextPage))
    }

    @Test // 39
    fun openingCategoriesRemainsFunctional() {
        val opened = process(
            GuidedModeNavigation.SELECT_LEFT,
            GuidedModeNavigation.SELECT_RIGHT,
            menuState(selection = 0)
        ) as GuidedSequenceResult.Navigate
        assertEquals(GuidedOverlayScreenMode.Vocabulary, opened.newState.screenMode)
        assertEquals(0, opened.newState.categoryIndex)
    }

    @Test // 40
    fun backRemainsFunctional() {
        val back = process(
            GuidedModeNavigation.BACK_LEFT,
            GuidedModeNavigation.BACK_RIGHT,
            menuState(selection = 3)
        ) as GuidedSequenceResult.Navigate
        assertEquals(GuidedOverlayScreenMode.Vocabulary, back.newState.screenMode)
    }

    @Test // 41
    fun emergencyRemainsFunctional() {
        assertTrue(isEmergencySequence(EMERGENCY_LEFT_WINKS, EMERGENCY_RIGHT_WINKS))
        assertTrue(menuActions.any { it.kind == GuidedPanelActionKind.Emergency })
    }

    @Test // 42
    fun rc7d19AutomaticSelectionScrollingRemainsFunctional() {
        val ui = readSource("app/src/main/java/com/idworx/lisa/LisaGuidedModeUi.kt")
        // RC7D.21 selection-driven viewport centring is preserved for item / shortcut / restore.
        assertTrue(ui.contains("GuidedCategoryMenuScroll.centeredScrollOffsetPx"))
        assertTrue(ui.contains("LaunchedEffect(") && ui.contains("categoryMenuSelection"))
        assertTrue(ui.contains("animateScrollTo(target)"))
    }

    @Test // 43
    fun communicationWorkspaceRemainsFunctional() {
        val vocabularyState = GuidedNavigationState(
            screenMode = GuidedOverlayScreenMode.Vocabulary,
            categoryIndex = GuidedVocabularyCategory.BasicNeeds.ordinal
        )
        val scrollDown = process(GuidedModeNavigation.NEXT_LEFT, GuidedModeNavigation.NEXT_RIGHT, vocabularyState)
        assertTrue(scrollDown is GuidedSequenceResult.Navigate)
    }

    @Test // 44
    fun noAndroidSystemKeyboardIntroduced() {
        val ui = readSource("app/src/main/java/com/idworx/lisa/LisaGuidedModeUi.kt")
        assertFalse(ui.contains("TextField"))
        assertFalse(ui.contains("KeyboardOptions"))
    }

    @Test // 45
    fun existingCategoryGestureConflictChecksRemainGreen() {
        assertTrue(GuidedNavigationGestureAudit.auditAllModes())
        assertTrue(ModeScopedGestureAuthorityAudit.passes())
        val report = GestureConflictAuthorityV1.validate()
        assertNotEquals(ValidationOutcome.FAIL, report.outcome)
    }

    // ===================== SEQUENCE SAFETY =====================

    @Test
    fun pageSequencesAreUniqueAndNonConflicting() {
        assertNotEquals(prevPage, nextPage)
        for (seq in listOf(prevPage, nextPage)) {
            assertFalse(GuidedModeNavigation.isGlobalNavigationSequence(seq.first, seq.second))
            assertFalse(isEmergencySequence(seq.first, seq.second))
            assertFalse(GuidedCategoryShortcuts.allGestures().contains(seq))
            assertFalse(GuidedPageSequences.slots.contains(seq))
            assertFalse(GuidedPageSequences.extendedSlots.contains(seq))
        }
        // Category Menu mode bindings register both jumps with no duplicates.
        val bindings = GuidedNavigationGestureAudit.categoryMenuModeBindings()
        assertTrue(GuidedNavigationGestureAudit.noDuplicateGestures(bindings))
        assertTrue(bindings.any { it.action == "PreviousCategoryPage" })
        assertTrue(bindings.any { it.action == "NextCategoryPage" })
    }
}
