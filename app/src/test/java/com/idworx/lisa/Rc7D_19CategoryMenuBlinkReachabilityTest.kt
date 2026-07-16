package com.idworx.lisa

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * RC7D.19 — every Communication Workspace category, including Phrase Management,
 * must be reachable by blink Move Down / Move Up alone (no manual touch scrolling).
 */
class Rc7D_19CategoryMenuBlinkReachabilityTest {

    private val english = LisaUiStrings.forLanguage(PreferredLanguage.English)

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

    private fun next(state: GuidedNavigationState): GuidedNavigationState {
        val result = GuidedNavigationController.processSequence(
            GuidedModeNavigation.NEXT_LEFT,
            GuidedModeNavigation.NEXT_RIGHT,
            state,
            PreferredLanguage.English,
            english
        )
        assertTrue("Expected Navigate from selection ${state.categoryMenuSelection}", result is GuidedSequenceResult.Navigate)
        return (result as GuidedSequenceResult.Navigate).newState
    }

    private fun previous(state: GuidedNavigationState): GuidedNavigationState {
        val result = GuidedNavigationController.processSequence(
            GuidedModeNavigation.PREVIOUS_LEFT,
            GuidedModeNavigation.PREVIOUS_RIGHT,
            state,
            PreferredLanguage.English,
            english
        )
        assertTrue("Expected Navigate from selection ${state.categoryMenuSelection}", result is GuidedSequenceResult.Navigate)
        return (result as GuidedSequenceResult.Navigate).newState
    }

    @Test
    fun categoryCountEqualsNavigationAndRenderedCounts() {
        val titles = GuidedVocabularyCatalog.categoryMenuTitles(english)
        assertEquals(GuidedVocabularyCategory.PAGE_COUNT, GuidedVocabularyCategory.ordered.size)
        assertEquals(GuidedVocabularyCategory.PAGE_COUNT, titles.size)
        assertEquals(9, titles.size)
        assertEquals("Phrase Management", titles[GuidedVocabularyCategory.PHRASE_MANAGEMENT_INDEX])
        assertTrue(GuidedCategoryMenuScroll.canReachEveryCategoryByBlink())
    }

    @Test
    fun moveDownReachesEveryCategoryIncludingPhraseManagement() {
        var state = GuidedNavigationState(
            screenMode = GuidedOverlayScreenMode.CategoryMenu,
            categoryMenuSelection = 0
        )
        val visited = mutableListOf(state.categoryMenuSelection)
        repeat(GuidedVocabularyCategory.PAGE_COUNT - 1) {
            state = next(state)
            visited.add(state.categoryMenuSelection)
        }
        assertEquals((0 until GuidedVocabularyCategory.PAGE_COUNT).toList(), visited)
        assertEquals(GuidedVocabularyCategory.ADJUST_SETTINGS_INDEX, state.categoryMenuSelection)
        assertEquals(
            GuidedVocabularyCategory.AdjustSettings,
            GuidedVocabularyCategory.ordered[state.categoryMenuSelection]
        )
    }

    @Test
    fun moveUpReturnsThroughEveryCategoryToFirst() {
        var state = GuidedNavigationState(
            screenMode = GuidedOverlayScreenMode.CategoryMenu,
            categoryMenuSelection = GuidedVocabularyCategory.PAGE_COUNT - 1
        )
        val visited = mutableListOf(state.categoryMenuSelection)
        repeat(GuidedVocabularyCategory.PAGE_COUNT - 1) {
            state = previous(state)
            visited.add(state.categoryMenuSelection)
        }
        assertEquals(
            (GuidedVocabularyCategory.PAGE_COUNT - 1 downTo 0).toList(),
            visited
        )
        assertEquals(0, state.categoryMenuSelection)
    }

    @Test
    fun moveDownDoesNotStopAtCustom() {
        var state = GuidedNavigationState(
            screenMode = GuidedOverlayScreenMode.CategoryMenu,
            categoryMenuSelection = GuidedVocabularyCategory.CUSTOM_CATEGORY_INDEX
        )
        assertTrue(state.categoryMenuSelection < GuidedVocabularyCategory.PAGE_COUNT - 1)
        state = next(state)
        assertEquals(GuidedVocabularyCategory.PHRASE_MANAGEMENT_INDEX, state.categoryMenuSelection)
    }

    @Test
    fun smallScreenAutoScrollKeepsFinalSelectionRevealed() {
        // RC7D.21 — the canonical centring authority still keeps the final selection revealed on a
        // small viewport (~7 rows visible, so only one row of scroll room): both Custom and Phrase
        // Management clamp to the content bottom, and Phrase Management is never left below the fold.
        val viewportForSevenVisibleRows = GuidedCategoryMenuScroll.ROW_PITCH_PX * 7
        val maxScrollForSevenVisibleRows = GuidedCategoryMenuScroll.ROW_PITCH_PX
        val offsetAtCustom = GuidedCategoryMenuScroll.centeredScrollOffsetPxForIndex(
            GuidedVocabularyCategory.CUSTOM_CATEGORY_INDEX,
            viewportForSevenVisibleRows,
            maxScrollForSevenVisibleRows
        )
        val offsetAtPhraseManagement = GuidedCategoryMenuScroll.centeredScrollOffsetPxForIndex(
            GuidedVocabularyCategory.PHRASE_MANAGEMENT_INDEX,
            viewportForSevenVisibleRows,
            maxScrollForSevenVisibleRows
        )
        assertTrue(offsetAtPhraseManagement >= offsetAtCustom)
        assertEquals(maxScrollForSevenVisibleRows, offsetAtPhraseManagement)
        assertTrue(offsetAtPhraseManagement > 0)
    }

    @Test
    fun categoryMenuUiWiresSelectionDrivenAutoScroll() {
        val ui = readSource("app/src/main/java/com/idworx/lisa/LisaGuidedModeUi.kt")
        // RC7D.21 canonical centring authority, selection-driven.
        assertTrue(ui.contains("GuidedCategoryMenuScroll.centeredScrollOffsetPx"))
        assertTrue(ui.contains("LaunchedEffect(") && ui.contains("categoryMenuSelection"))
        assertTrue(ui.contains("animateScrollTo(target)"))
        assertTrue(ui.contains("categoryMenuScrollState"))
        // Must not recreate a scroll state that ignores selection.
        assertFalse(
            ui.contains("verticalScroll(rememberScrollState()),\n                            verticalArrangement = Arrangement.spacedBy(8.dp)\n                        ) {\n                            categoryMenuTitles.forEachIndexed")
        )
    }

    @Test
    fun noHardcodedSevenAsNavigationCeiling() {
        assertEquals(9, GuidedVocabularyCategory.PAGE_COUNT)
        assertEquals(7, GuidedVocabularyCategory.PHRASE_MANAGEMENT_INDEX)
        assertEquals(6, GuidedVocabularyCategory.CUSTOM_CATEGORY_INDEX)
        val moveDownCap = GuidedVocabularyCategory.PAGE_COUNT - 1
        assertEquals(GuidedVocabularyCategory.ADJUST_SETTINGS_INDEX, moveDownCap)
        assertFalse(moveDownCap == GuidedVocabularyCategory.CUSTOM_CATEGORY_INDEX)
        assertFalse(moveDownCap == GuidedVocabularyCategory.PHRASE_MANAGEMENT_INDEX)
    }

    @Test
    fun selectionOpenStillRoutesPhraseManagementDestination() {
        val state = GuidedNavigationState(
            screenMode = GuidedOverlayScreenMode.CategoryMenu,
            categoryMenuSelection = GuidedVocabularyCategory.PHRASE_MANAGEMENT_INDEX
        )
        val opened = GuidedNavigationController.openSelectedCategory(state)
        assertEquals(
            CategoryAreaDestination.PhraseManagement,
            CategoryAreaDestination.forCategoryIndex(opened.categoryIndex)
        )
    }
}
