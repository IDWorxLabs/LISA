package com.idworx.lisa

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** RC7B.1 — dedicated Custom communication category and page. */
class Rc7B1CustomCategoryTest {

    private val english = LisaUiStrings.forLanguage(PreferredLanguage.English)

    private fun pagesWith(
        vararg phrases: CustomPhraseEngine.CaregiverCustomPhraseEntry
    ): List<GuidedCategoryPage> = GuidedVocabularyCatalog.buildPages(
        PreferredLanguage.English,
        english,
        GuidedCatalogContext(caregiverCustomPhrases = phrases.toList())
    )

    private fun customEntry(
        phrase: String,
        category: CustomPhraseEngine.CaregiverPhraseCategory
    ) = CustomPhraseEngine.CaregiverCustomPhraseEntry(
        phrase = phrase,
        left = 7,
        right = 2,
        category = category
    )

    // 1. Custom is a first-class category in the specified order.

    @Test
    fun customIsFirstClassCategoryOnFinalPage() {
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
        assertEquals(6, GuidedVocabularyCategory.CUSTOM_CATEGORY_INDEX)
    }

    @Test
    fun customTitleIsLocalizedInAllLanguages() {
        assertEquals(
            "Custom",
            LisaUiStrings.forLanguage(PreferredLanguage.English).guidedCategoryTitle(GuidedVocabularyCategory.Custom)
        )
        assertEquals(
            "Pasgemaak",
            LisaUiStrings.forLanguage(PreferredLanguage.Afrikaans).guidedCategoryTitle(GuidedVocabularyCategory.Custom)
        )
        assertEquals(
            "Ngokwezifiso",
            LisaUiStrings.forLanguage(PreferredLanguage.IsiZulu).guidedCategoryTitle(GuidedVocabularyCategory.Custom)
        )
    }

    // 2. Custom no longer maps to Family.

    @Test
    fun customCategoryMapsToCustomPageNotFamily() {
        val mapped = CustomPhraseEngine.CaregiverPhraseCategory.Custom.toGuidedCategory()
        assertEquals(GuidedVocabularyCategory.Custom, mapped)
        assertNotEquals(GuidedVocabularyCategory.Family, mapped)
    }

    @Test
    fun familyCategoryStillMapsToFamily() {
        assertEquals(
            GuidedVocabularyCategory.Family,
            CustomPhraseEngine.CaregiverPhraseCategory.Family.toGuidedCategory()
        )
    }

    // 3. A phrase saved as Custom appears only on the Custom page.

    @Test
    fun customPhraseAppearsOnlyOnCustomPage() {
        val pages = pagesWith(customEntry("Please open the curtains.", CustomPhraseEngine.CaregiverPhraseCategory.Custom))
        val customPage = pages.first { it.category == GuidedVocabularyCategory.Custom }
        assertTrue(customPage.entries.any { it.phrase == "Please open the curtains." })
        pages.filter { it.category != GuidedVocabularyCategory.Custom }.forEach { page ->
            assertTrue(
                "Custom phrase leaked onto ${page.category}",
                page.entries.none { it.phrase == "Please open the curtains." }
            )
        }
    }

    // 4. A phrase saved as Family remains on Family.

    @Test
    fun familyPhraseAppearsOnFamilyPageOnly() {
        val pages = pagesWith(customEntry("Please call my brother.", CustomPhraseEngine.CaregiverPhraseCategory.Family))
        val familyPage = pages.first { it.category == GuidedVocabularyCategory.Family }
        assertTrue(familyPage.entries.any { it.phrase == "Please call my brother." })
        val customPage = pages.first { it.category == GuidedVocabularyCategory.Custom }
        assertTrue(customPage.entries.none { it.phrase == "Please call my brother." })
    }

    // 5. Existing stored Custom phrases load onto Custom without migration loss.

    @Test
    fun storedCustomPhrasesLoadOntoCustomPage() {
        val restored = CustomPhraseEngine.parseCustomMappings("7,2|I need my glasses.|Custom\n")
        assertEquals(1, restored.size)
        assertEquals(CustomPhraseEngine.CaregiverPhraseCategory.Custom, restored.first().caregiverCategory)
        val pages = pagesWith(*CustomPhraseEngine.toCatalogEntries(restored).toTypedArray())
        val customPage = pages.first { it.category == GuidedVocabularyCategory.Custom }
        assertTrue(customPage.entries.any { it.phrase == "I need my glasses." && it.left == 7 && it.right == 2 })
        val familyPage = pages.first { it.category == GuidedVocabularyCategory.Family }
        assertTrue(familyPage.entries.none { it.phrase == "I need my glasses." })
    }

    @Test
    fun legacyMappingsWithoutCategoryAreNotMigratedToFamily() {
        val restored = CustomPhraseEngine.parseCustomMappings("7,2|Old saved phrase.\n")
        assertEquals(1, restored.size)
        assertNotEquals(CustomPhraseEngine.CaregiverPhraseCategory.Family, restored.first().caregiverCategory)
    }

    // 6. The communication page count increases correctly.

    @Test
    fun communicationPageCountIsSeven() {
        assertEquals(7, GuidedVocabularyCategory.PAGE_COUNT)
        assertEquals(7, GuidedVocabularyCatalog.buildPages(PreferredLanguage.English, english).size)
        assertEquals(7, GuidedVocabularyCatalog.categoryMenuTitles(english).size)
    }

    // 7. Previous/next navigation still reaches all pages.

    @Test
    fun categoryMenuNextGestureReachesEveryPage() {
        var state = GuidedNavigationState(screenMode = GuidedOverlayScreenMode.CategoryMenu, categoryMenuSelection = 0)
        for (expected in 1 until GuidedVocabularyCategory.PAGE_COUNT) {
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
        // Every selection opens the matching category page.
        for (index in 0 until GuidedVocabularyCategory.PAGE_COUNT) {
            val opened = GuidedNavigationController.openCategoryDirectly(GuidedNavigationState(), index)
            assertEquals(index, opened.categoryIndex)
        }
    }

    // 8. System Controls remains reachable.

    @Test
    fun systemControlsRemainsReachable() {
        val index = GuidedVocabularyCategory.ordered.indexOf(GuidedVocabularyCategory.BasicSystemControls)
        val opened = GuidedNavigationController.openCategoryDirectly(GuidedNavigationState(), index)
        assertEquals(index, opened.categoryIndex)
        val page = GuidedVocabularyCatalog.categoryAt(index, PreferredLanguage.English, english)
        assertNotNull(page)
        assertEquals(GuidedVocabularyCategory.BasicSystemControls, page!!.category)
        assertTrue(page.entries.isNotEmpty())
        val shortcut = GuidedCategoryShortcuts.gestureForCategory(index)
        assertEquals(index, GuidedCategoryShortcuts.categoryIndexForGesture(shortcut.first, shortcut.second))
    }

    // 9. Custom empty state appears when no custom phrases exist.

    @Test
    fun customPageIsEmptyWithoutCustomPhrases() {
        val pages = GuidedVocabularyCatalog.buildPages(PreferredLanguage.English, english)
        val customPage = pages.first { it.category == GuidedVocabularyCategory.Custom }
        assertTrue(customPage.entries.isEmpty())
        assertEquals("No custom phrases yet.", english.guidedCustomEmptyTitle)
        assertEquals("Add phrases from Menu → Vocabulary.", english.guidedCustomEmptyBody)
    }

    @Test
    fun emptyStateIsRenderedOnlyForEmptyPages() {
        val source = readSource("app/src/main/java/com/idworx/lisa/LisaGuidedModeUi.kt")
        assertTrue(source.contains("guidedCustomEmptyTitle"))
        assertTrue(source.contains("guidedCustomEmptyBody"))
        assertTrue(source.contains("pageEntries.isEmpty()"))
        assertFalse(source.contains("coming soon", ignoreCase = true))
    }

    // 10. Empty state disappears immediately after save.

    @Test
    fun customPagePopulatedImmediatelyAfterSave() {
        val save = CustomPhraseEngine.saveNewPhrase(
            "Please open the curtains.",
            CustomPhraseEngine.CaregiverPhraseCategory.Custom,
            defaultLanguageMappings()
        )
        assertTrue(save is CustomPhraseEngine.SavePhraseResult.Success)
        val mapping = (save as CustomPhraseEngine.SavePhraseResult.Success).mapping
        val entries = CustomPhraseEngine.toCatalogEntries(listOf(mapping))
        val pages = pagesWith(*entries.toTypedArray())
        val customPage = pages.first { it.category == GuidedVocabularyCategory.Custom }
        assertEquals(1, customPage.entries.size)
        assertEquals("Please open the curtains.", customPage.entries.first().phrase)
    }

    // 11. Phrase editor still offers Custom.

    @Test
    fun phraseEditorOffersCustomCategory() {
        assertTrue(
            CustomPhraseEngine.selectableCategories.contains(CustomPhraseEngine.CaregiverPhraseCategory.Custom)
        )
    }

    // 12. Success screen shows phrase, category, and assigned sequence.

    @Test
    fun successScreenShowsPhraseCategoryAndSequence() {
        val source = readSource("app/src/main/java/com/idworx/lisa/LisaAccessibilityUi.kt")
        assertTrue(source.contains("phraseCreatedSuccess"))
        assertTrue(source.contains("phraseCreatedCategoryLine"))
        assertTrue(source.contains("phraseCreatedSequenceLine"))
        assertTrue(source.contains("formatWinkSequenceShort(mapping.left, mapping.right)"))
        assertTrue(source.contains("phraseEditorCreateAnother"))
        assertTrue(source.contains("phraseEditorReturnToCommunication"))
        assertEquals("Category: Custom", english.phraseCreatedCategoryLine("Custom"))
        assertEquals("Blink sequence: L7 R2", english.phraseCreatedSequenceLine("L7 R2"))
    }

    // 13. Built-in catalog content remains unchanged.

    @Test
    fun builtInCatalogContentUnchanged() {
        val pages = GuidedVocabularyCatalog.buildPages(PreferredLanguage.English, english)
        val sizesByCategory = pages.associate { it.category to it.entries.size }
        assertEquals(10, sizesByCategory[GuidedVocabularyCategory.Conversation])
        assertEquals(10, sizesByCategory[GuidedVocabularyCategory.BasicNeeds])
        assertEquals(10, sizesByCategory[GuidedVocabularyCategory.Medical])
        assertEquals(10, sizesByCategory[GuidedVocabularyCategory.Family])
        assertEquals(0, sizesByCategory[GuidedVocabularyCategory.Custom])
        assertEquals(10, sizesByCategory[GuidedVocabularyCategory.BasicSystemControls])
        assertEquals(4, sizesByCategory[GuidedVocabularyCategory.Preferences])
        val familyPage = pages.first { it.category == GuidedVocabularyCategory.Family }
        assertEquals("I want to see my mom.", familyPage.entries.first().phrase)
        assertEquals("I want to talk.", familyPage.entries.last().phrase)
    }

    // 14. No sequence or category-navigation conflicts are introduced.

    @Test
    fun categoryShortcutsHaveNoConflicts() {
        val gestures = GuidedCategoryShortcuts.allGestures()
        assertEquals(GuidedVocabularyCategory.PAGE_COUNT, gestures.size)
        assertEquals(gestures.size, gestures.distinct().size)
        assertTrue(GuidedCategoryShortcuts.doNotConflictWithGlobalNavigation())
        assertTrue(GuidedVocabularyCatalogValidation.categoryShortcutLabelsMatchExpectedSlots())
        assertTrue(GuidedNavigationGestureAudit.auditAllModes())
    }

    @Test
    fun catalogSlotValidationStillPasses() {
        assertTrue(GuidedVocabularyCatalogValidation.sequencesRepeatAcrossPages())
        assertTrue(GuidedVocabularyCatalogValidation.sameSlotDifferentPhrasesAcrossPages())
        assertTrue(GuidedVocabularyCatalogValidation.noVocabularyUsesForbiddenSequences())
        assertTrue(GuidedVocabularyCatalogValidation.sequencesUniqueWithinEachPage())
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
