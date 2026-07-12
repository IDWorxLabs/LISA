package com.idworx.lisa

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** RC7B.2 — preserve pre-RC7B.1 category shortcuts; Custom on final page. */
class Rc7B2PreserveCategoryShortcutsTest {

    private val english = LisaUiStrings.forLanguage(PreferredLanguage.English)

    /** Category shortcuts before RC7B.1 (six categories). */
    private val preRc7B1Shortcuts = listOf(
        "L2 R1" to GuidedVocabularyCategory.Conversation,
        "L1 R2" to GuidedVocabularyCategory.BasicNeeds,
        "L3 R1" to GuidedVocabularyCategory.Medical,
        "L1 R3" to GuidedVocabularyCategory.Family,
        "L3 R2" to GuidedVocabularyCategory.BasicSystemControls,
        "L2 R3" to GuidedVocabularyCategory.Preferences
    )

    private fun pagesWith(
        vararg phrases: CustomPhraseEngine.CaregiverCustomPhraseEntry
    ): List<GuidedCategoryPage> = GuidedVocabularyCatalog.buildPages(
        PreferredLanguage.English,
        english,
        GuidedCatalogContext(caregiverCustomPhrases = phrases.toList())
    )

    // 1. Page order is exactly the required final order.

    @Test
    fun pageOrderMatchesRequiredFinalOrder() {
        assertEquals(
            listOf(
                GuidedVocabularyCategory.Conversation,
                GuidedVocabularyCategory.BasicNeeds,
                GuidedVocabularyCategory.Medical,
                GuidedVocabularyCategory.Family,
                GuidedVocabularyCategory.BasicSystemControls,
                GuidedVocabularyCategory.Preferences,
                GuidedVocabularyCategory.Custom
            ),
            GuidedVocabularyCategory.ordered
        )
    }

    // 2. Total page count remains 7.

    @Test
    fun totalPageCountRemainsSeven() {
        assertEquals(7, GuidedVocabularyCategory.PAGE_COUNT)
        assertEquals(7, GuidedVocabularyCatalog.buildPages(PreferredLanguage.English, english).size)
        assertEquals(7, GuidedVocabularyCatalog.categoryMenuTitles(english).size)
    }

    // 3. Every pre-RC7B.1 category shortcut matches its original value.

    @Test
    fun preRc7B1CategoryShortcutsArePreserved() {
        preRc7B1Shortcuts.forEachIndexed { index, (expectedLabel, category) ->
            assertEquals(category, GuidedVocabularyCategory.ordered[index])
            assertEquals(expectedLabel, GuidedCategoryShortcuts.sequenceLabelForCategory(index))
            val (left, right) = GuidedCategoryShortcuts.gestureForCategory(index)
            assertEquals(index, GuidedCategoryShortcuts.categoryIndexForGesture(left, right))
        }
    }

    // 4. System Controls shortcut is restored.

    @Test
    fun systemControlsShortcutRestoredToL3R2() {
        val index = GuidedVocabularyCategory.ordered.indexOf(GuidedVocabularyCategory.BasicSystemControls)
        assertEquals(4, index)
        assertEquals("L3 R2", GuidedCategoryShortcuts.sequenceLabelForCategory(index))
    }

    // 5. Preferences shortcut is restored.

    @Test
    fun preferencesShortcutRestoredToL2R3() {
        assertEquals(5, GuidedVocabularyCategory.PREFERENCES_CATEGORY_INDEX)
        assertEquals("L2 R3", GuidedCategoryShortcuts.sequenceLabelForCategory(5))
    }

    // 6. Custom receives a new safe shortcut via slot-at-index policy.

    @Test
    fun customReceivesNewSafeShortcutOnFinalSlot() {
        assertEquals(6, GuidedVocabularyCategory.CUSTOM_CATEGORY_INDEX)
        val customShortcut = GuidedCategoryShortcuts.sequenceLabelForCategory(6)
        assertEquals("L3 R3", customShortcut)
        assertTrue(preRc7B1Shortcuts.none { it.first == customShortcut })
        assertTrue(GuidedCategoryShortcuts.doNotConflictWithGlobalNavigation())
        val (left, right) = GuidedCategoryShortcuts.gestureForCategory(6)
        assertEquals(6, GuidedCategoryShortcuts.categoryIndexForGesture(left, right))
    }

    // 7. Custom remains reachable through page navigation.

    @Test
    fun customReachableThroughCategoryMenuNavigation() {
        var state = GuidedNavigationState(
            screenMode = GuidedOverlayScreenMode.CategoryMenu,
            categoryMenuSelection = 0
        )
        for (expected in 1..GuidedVocabularyCategory.CUSTOM_CATEGORY_INDEX) {
            val result = GuidedNavigationController.processSequence(
                GuidedModeNavigation.NEXT_LEFT,
                GuidedModeNavigation.NEXT_RIGHT,
                state,
                PreferredLanguage.English,
                english
            )
            assertTrue(result is GuidedSequenceResult.Navigate)
            state = (result as GuidedSequenceResult.Navigate).newState
            assertEquals(expected, state.categoryMenuSelection)
        }
        assertEquals(GuidedVocabularyCategory.Custom, GuidedVocabularyCategory.ordered[state.categoryMenuSelection])
        val opened = GuidedNavigationController.openSelectedCategory(state)
        assertEquals(GuidedVocabularyCategory.CUSTOM_CATEGORY_INDEX, opened.categoryIndex)
    }

    // 8. A saved Custom phrase appears only on Custom.

    @Test
    fun customPhraseAppearsOnlyOnCustomPage() {
        val entry = CustomPhraseEngine.CaregiverCustomPhraseEntry(
            phrase = "Please open the curtains.",
            left = 7,
            right = 2,
            category = CustomPhraseEngine.CaregiverPhraseCategory.Custom
        )
        val pages = pagesWith(entry)
        val customPage = pages.first { it.category == GuidedVocabularyCategory.Custom }
        assertTrue(customPage.entries.any { it.phrase == "Please open the curtains." })
        pages.filter { it.category != GuidedVocabularyCategory.Custom }.forEach { page ->
            assertTrue(page.entries.none { it.phrase == "Please open the curtains." })
        }
    }

    // 9. A saved Family phrase appears only on Family.

    @Test
    fun familyPhraseAppearsOnlyOnFamilyPage() {
        val entry = CustomPhraseEngine.CaregiverCustomPhraseEntry(
            phrase = "Please call my brother.",
            left = 7,
            right = 3,
            category = CustomPhraseEngine.CaregiverPhraseCategory.Family
        )
        val pages = pagesWith(entry)
        val familyPage = pages.first { it.category == GuidedVocabularyCategory.Family }
        assertTrue(familyPage.entries.any { it.phrase == "Please call my brother." })
        val customPage = pages.first { it.category == GuidedVocabularyCategory.Custom }
        assertTrue(customPage.entries.none { it.phrase == "Please call my brother." })
    }

    // 10. Existing stored Custom phrases still load without migration.

    @Test
    fun storedCustomPhrasesLoadWithoutMigration() {
        val restored = CustomPhraseEngine.parseCustomMappings("7,2|I need my glasses.|Custom\n")
        assertEquals(1, restored.size)
        assertEquals(CustomPhraseEngine.CaregiverPhraseCategory.Custom, restored.first().caregiverCategory)
        val pages = pagesWith(*CustomPhraseEngine.toCatalogEntries(restored).toTypedArray())
        val customPage = pages[GuidedVocabularyCategory.CUSTOM_CATEGORY_INDEX]
        assertEquals(GuidedVocabularyCategory.Custom, customPage.category)
        assertTrue(customPage.entries.any { it.phrase == "I need my glasses." })
    }

    // 11. Custom empty state still works.

    @Test
    fun customEmptyStateStillWorks() {
        val customPage = GuidedVocabularyCatalog.buildPages(PreferredLanguage.English, english)
            .first { it.category == GuidedVocabularyCategory.Custom }
        assertTrue(customPage.entries.isEmpty())
        assertEquals("No custom phrases yet.", english.guidedCustomEmptyTitle)
        assertEquals("Add phrases from Menu → Vocabulary.", english.guidedCustomEmptyBody)
    }

    // 12. Built-in category contents and entry counts remain unchanged.

    @Test
    fun builtInCategoryContentsUnchanged() {
        val pages = GuidedVocabularyCatalog.buildPages(PreferredLanguage.English, english)
        val sizesByCategory = pages.associate { it.category to it.entries.size }
        assertEquals(10, sizesByCategory[GuidedVocabularyCategory.Conversation])
        assertEquals(10, sizesByCategory[GuidedVocabularyCategory.BasicNeeds])
        assertEquals(10, sizesByCategory[GuidedVocabularyCategory.Medical])
        assertEquals(10, sizesByCategory[GuidedVocabularyCategory.Family])
        assertEquals(10, sizesByCategory[GuidedVocabularyCategory.BasicSystemControls])
        assertEquals(4, sizesByCategory[GuidedVocabularyCategory.Preferences])
        assertEquals(0, sizesByCategory[GuidedVocabularyCategory.Custom])
    }

    // 13. No category-navigation or gesture conflicts are introduced.

    @Test
    fun noCategoryNavigationConflicts() {
        val gestures = GuidedCategoryShortcuts.allGestures()
        assertEquals(7, gestures.size)
        assertEquals(gestures.size, gestures.distinct().size)
        assertTrue(GuidedCategoryShortcuts.doNotConflictWithGlobalNavigation())
        assertTrue(GuidedVocabularyCatalogValidation.categoryShortcutLabelsMatchExpectedSlots())
        assertTrue(GuidedNavigationGestureAudit.auditAllModes())
    }

    // 14. Phrase Editor still offers Custom.

    @Test
    fun phraseEditorStillOffersCustom() {
        assertTrue(
            CustomPhraseEngine.selectableCategories.contains(CustomPhraseEngine.CaregiverPhraseCategory.Custom)
        )
        assertNotEquals(
            GuidedVocabularyCategory.Family,
            CustomPhraseEngine.CaregiverPhraseCategory.Custom.toGuidedCategory()
        )
    }

    // 15. Success screen still reports Custom correctly.

    @Test
    fun successScreenStillReportsCustomCorrectly() {
        val source = readSource("app/src/main/java/com/idworx/lisa/LisaAccessibilityUi.kt")
        assertTrue(source.contains("phraseCreatedCategoryLine"))
        assertTrue(source.contains("caregiverPhraseCategoryLabel"))
        assertTrue(source.contains("mapping.caregiverCategory"))
        assertEquals("Category: Custom", english.phraseCreatedCategoryLine("Custom"))
    }

    @Test
    fun categoryMenuTitlesMatchPageOrder() {
        val titles = GuidedVocabularyCatalog.categoryMenuTitles(english)
        assertEquals(
            listOf(
                "Conversation",
                "Basic Needs",
                "Medical",
                "Family",
                "Basic System Controls",
                "Preferences",
                "Custom"
            ),
            titles
        )
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
}
