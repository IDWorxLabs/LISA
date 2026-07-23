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

    /** Category shortcuts for visible destinations after Settings & Controls migration. */
    private val visibleCategoryShortcuts = listOf(
        "L2 R1" to GuidedVocabularyCategory.Conversation,
        "L1 R2" to GuidedVocabularyCategory.BasicNeeds,
        "L3 R1" to GuidedVocabularyCategory.Medical,
        "L1 R3" to GuidedVocabularyCategory.Family,
        "L3 R2" to GuidedVocabularyCategory.Preferences,
        "L2 R3" to GuidedVocabularyCategory.Custom,
        "L3 R3" to GuidedVocabularyCategory.PhraseManagement
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
                GuidedVocabularyCategory.Preferences,
                GuidedVocabularyCategory.Custom,
                GuidedVocabularyCategory.PhraseManagement,
                GuidedVocabularyCategory.AdjustSettings
            ),
            GuidedVocabularyCategory.ordered
        )
    }

    // 2. Total page count includes Phrase Management.

    @Test
    fun totalPageCountIncludesPhraseManagement() {
        assertEquals(8, GuidedVocabularyCategory.PAGE_COUNT)
        assertEquals(8, GuidedVocabularyCatalog.buildPages(PreferredLanguage.English, english).size)
        assertEquals(8, GuidedVocabularyCatalog.categoryMenuTitles(english).size)
    }

    // 3. Visible category shortcuts match the post-migration ordered list.

    @Test
    fun preRc7B1CategoryShortcutsArePreserved() {
        visibleCategoryShortcuts.forEachIndexed { index, (expectedLabel, category) ->
            assertEquals(category, GuidedVocabularyCategory.ordered[index])
            assertEquals(expectedLabel, GuidedCategoryShortcuts.sequenceLabelForCategory(index))
            val (left, right) = GuidedCategoryShortcuts.gestureForCategory(index)
            assertEquals(index, GuidedCategoryShortcuts.categoryIndexForGesture(left, right))
        }
    }

    // 4. Basic System Controls removed; Preferences inherits former L3 R2 slot index.

    @Test
    fun systemControlsShortcutRestoredToL3R2() {
        assertFalse(GuidedVocabularyCategory.ordered.contains(GuidedVocabularyCategory.BasicSystemControls))
        assertEquals(4, GuidedVocabularyCategory.PREFERENCES_CATEGORY_INDEX)
        assertEquals("L3 R2", GuidedCategoryShortcuts.sequenceLabelForCategory(4))
    }

    // 5. Preferences shortcut after migration.

    @Test
    fun preferencesShortcutRestoredToL2R3() {
        assertEquals(4, GuidedVocabularyCategory.PREFERENCES_CATEGORY_INDEX)
        assertEquals("L3 R2", GuidedCategoryShortcuts.sequenceLabelForCategory(4))
    }

    // 6. Custom receives a safe shortcut via slot-at-index policy.

    @Test
    fun customReceivesNewSafeShortcutOnFinalSlot() {
        assertEquals(5, GuidedVocabularyCategory.CUSTOM_CATEGORY_INDEX)
        val customShortcut = GuidedCategoryShortcuts.sequenceLabelForCategory(5)
        assertEquals("L2 R3", customShortcut)
        assertTrue(GuidedCategoryShortcuts.doNotConflictWithGlobalNavigation())
        val (left, right) = GuidedCategoryShortcuts.gestureForCategory(5)
        assertEquals(5, GuidedCategoryShortcuts.categoryIndexForGesture(left, right))
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

    @Test
    fun phraseManagementReachableThroughCategoryMenuNavigation() {
        var state = GuidedNavigationState(
            screenMode = GuidedOverlayScreenMode.CategoryMenu,
            categoryMenuSelection = 0
        )
        for (expected in 1..GuidedVocabularyCategory.PHRASE_MANAGEMENT_INDEX) {
            val result = GuidedNavigationController.processSequence(
                GuidedModeNavigation.NEXT_LEFT,
                GuidedModeNavigation.NEXT_RIGHT,
                state,
                PreferredLanguage.English,
                english
            )
            assertTrue(
                "Expected Navigate at selection ${expected - 1}, got $result",
                result is GuidedSequenceResult.Navigate
            )
            state = (result as GuidedSequenceResult.Navigate).newState
            assertEquals(expected, state.categoryMenuSelection)
        }
        assertEquals(
            GuidedVocabularyCategory.PhraseManagement,
            GuidedVocabularyCategory.ordered[state.categoryMenuSelection]
        )
        assertEquals(GuidedVocabularyCategory.PHRASE_MANAGEMENT_INDEX, state.categoryMenuSelection)
    }

    // 8. User-created phrases never appear on Custom.

    @Test
    fun userCreatedPhrasesNeverAppearOnCustomPage() {
        val entry = CustomPhraseEngine.CaregiverCustomPhraseEntry(
            phrase = "Please open the curtains.",
            left = 7,
            right = 2,
            category = CustomPhraseEngine.CaregiverPhraseCategory.Conversation
        )
        val pages = pagesWith(entry)
        val customPage = pages.first { it.category == GuidedVocabularyCategory.Custom }
        assertTrue(customPage.entries.isEmpty())
        val conversation = pages.first { it.category == GuidedVocabularyCategory.Conversation }
        assertTrue(conversation.entries.any { it.phrase == "Please open the curtains." })
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

    // 10. Legacy Custom-category storage migrates to Conversation.

    @Test
    fun legacyCustomCategoryStorageMigratesToConversation() {
        val restored = CustomPhraseEngine.parseCustomMappings("7,2|I need my glasses.|Custom\n")
        val migration = CustomPhraseEngine.migrateCustomCategoryMappings(restored)
        assertEquals(1, migration.migratedCount)
        val pages = pagesWith(*CustomPhraseEngine.toCatalogEntries(migration.mappings).toTypedArray())
        val customPage = pages[GuidedVocabularyCategory.CUSTOM_CATEGORY_INDEX]
        assertTrue(customPage.entries.isEmpty())
        val conversation = pages.first { it.category == GuidedVocabularyCategory.Conversation }
        assertTrue(conversation.entries.any { it.phrase == "I need my glasses." })
    }

    // 11. Custom empty state still works.

    @Test
    fun customEmptyStateHintStillAvailable() {
        val pages = GuidedVocabularyCatalog.buildPages(PreferredLanguage.English, english)
        val customPage = pages.first { it.category == GuidedVocabularyCategory.Custom }
        assertTrue(customPage.entries.isEmpty())
        assertEquals("No custom phrases yet.", english.guidedCustomEmptyTitle)
        assertEquals(
            "Open Custom from Categories to create a phrase using your eyes.",
            english.guidedCustomEmptyBody
        )
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
        assertEquals(null, sizesByCategory[GuidedVocabularyCategory.BasicSystemControls])
        assertEquals(4, sizesByCategory[GuidedVocabularyCategory.Preferences])
        assertEquals(0, sizesByCategory[GuidedVocabularyCategory.Custom])
        assertEquals(0, sizesByCategory[GuidedVocabularyCategory.PhraseManagement])
        assertEquals(0, sizesByCategory[GuidedVocabularyCategory.AdjustSettings])
    }

    // 13. No category-navigation or gesture conflicts are introduced.

    @Test
    fun noCategoryNavigationConflicts() {
        val gestures = GuidedCategoryShortcuts.allGestures()
        assertEquals(8, gestures.size)
        assertEquals(gestures.size, gestures.distinct().size)
        assertTrue(GuidedCategoryShortcuts.doNotConflictWithGlobalNavigation())
        assertTrue(GuidedVocabularyCatalogValidation.categoryShortcutLabelsMatchExpectedSlots())
        assertTrue(GuidedNavigationGestureAudit.auditAllModes())
    }

    // 14. Phrase Editor still offers Custom.

    @Test
    fun phraseEditorDoesNotOfferCustomAsDestination() {
        assertFalse(
            CustomPhraseEngine.selectableCategories.contains(CustomPhraseEngine.CaregiverPhraseCategory.Custom)
        )
        assertEquals(
            GuidedVocabularyCategory.Custom,
            CustomPhraseEngine.CaregiverPhraseCategory.Custom.toGuidedCategory()
        )
    }

    // 15. Success screen still reports Custom correctly.

    @Test
    fun successScreenStillReportsCustomCorrectly() {
        val composerUi = readSource("app/src/main/java/com/idworx/lisa/PhraseComposerUi.kt")
        assertTrue(composerUi.contains("phraseCreatedCategoryLine"))
        assertTrue(composerUi.contains("caregiverPhraseCategoryLabel"))
        assertTrue(composerUi.contains("mapping.caregiverCategory"))
        assertEquals("Category: Custom", english.phraseCreatedCategoryLine("Custom"))
    }

    @Test
    fun categoryMenuTitlesMatchPageOrder() {
        val titles = GuidedVocabularyCatalog.categoryMenuTitles(english)
        assertEquals(
            listOf(
                "General Conversation",
                "Basic Needs",
                "Medical",
                "Family",
                "Preferences",
                "Customize Phrases",
                "Phrase Management",
                "Settings & Controls"
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
