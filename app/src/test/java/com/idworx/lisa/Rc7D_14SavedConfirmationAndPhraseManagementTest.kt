package com.idworx.lisa

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** RC7D.14 — Saved confirmation wording + Phrase Management Categories destination. */
class Rc7D_14SavedConfirmationAndPhraseManagementTest {

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

    @Test
    fun verifiedCreateSuccessHeadingIsExactSaved() {
        assertEquals("Saved", english.phraseComposerSuccessTitle)
        assertEquals("Saved", english.phraseCreatedSuccess)
        val success = PhraseComposerState(
            mode = PhraseComposerMode.Success,
            phraseText = "Hello nurse",
            selectedCategory = CustomPhraseEngine.CaregiverPhraseCategory.Medical,
            savedMapping = WinkMapping(
                left = 5,
                right = 1,
                vocabularyId = "Hello nurse",
                isCustom = true,
                customPhrase = "Hello nurse",
                caregiverCategory = CustomPhraseEngine.CaregiverPhraseCategory.Medical
            )
        )
        assertEquals("Saved", PhraseComposerController.screenTitle(success, english))
    }

    @Test
    fun savedNotUsedForDuplicateOrFailureModes() {
        assertEquals(
            "Phrase already exists",
            PhraseComposerController.screenTitle(
                PhraseComposerState(mode = PhraseComposerMode.DuplicateWarning),
                english
            )
        )
        assertEquals(
            "Save this phrase?",
            PhraseComposerController.screenTitle(
                PhraseComposerState(mode = PhraseComposerMode.SaveConfirmation),
                english
            )
        )
    }

    @Test
    fun successStillOffersViewInAndCreateAnother() {
        val success = PhraseComposerState(
            mode = PhraseComposerMode.Success,
            selectedCategory = CustomPhraseEngine.CaregiverPhraseCategory.Family,
            savedMapping = WinkMapping(
                left = 5,
                right = 1,
                vocabularyId = "Saved phrase",
                isCustom = true,
                customPhrase = "Saved phrase",
                caregiverCategory = CustomPhraseEngine.CaregiverPhraseCategory.Family
            )
        )
        val entries = PhraseComposerController.visibleEntries(success, english)
        assertTrue(entries.any { it.actionId == PhraseComposerActionId.ViewInCategory })
        assertTrue(entries.any { it.actionId == PhraseComposerActionId.CreateAnother })
    }

    @Test
    fun returningToSuccessDoesNotPersistAgain() {
        val mapping = WinkMapping(
            left = 5,
            right = 1,
            vocabularyId = "Already saved",
            isCustom = true,
            customPhrase = "Already saved",
            caregiverCategory = CustomPhraseEngine.CaregiverPhraseCategory.BasicNeeds
        )
        val success = PhraseComposerState(
            mode = PhraseComposerMode.Success,
            phraseText = "Already saved",
            selectedCategory = CustomPhraseEngine.CaregiverPhraseCategory.BasicNeeds,
            savedMapping = mapping,
            navigationHistory = listOf(
                PhraseComposerMode.Keyboard,
                PhraseComposerMode.DestinationCategorySelection,
                PhraseComposerMode.SaveConfirmation
            )
        )
        val back = PhraseComposerController.processSequence(
            GuidedModeNavigation.BACK_LEFT,
            GuidedModeNavigation.BACK_RIGHT,
            success,
            english
        ) as PhraseComposerSequenceResult.Navigate
        val confirm = PhraseComposerController.commandPanelEntries(back.newState, english)
            .first { it.actionId == PhraseComposerActionId.ConfirmSave }
        val again = PhraseComposerController.processSequence(
            confirm.left,
            confirm.right,
            back.newState,
            english
        ) as PhraseComposerSequenceResult.Navigate
        assertEquals(PhraseComposerMode.Success, again.newState.mode)
        assertEquals(mapping, again.newState.savedMapping)
    }

    @Test
    fun categoriesDisplaysPhraseManagement() {
        val titles = GuidedVocabularyCatalog.categoryMenuTitles(english)
        assertTrue(titles.contains("Phrase Management"))
        assertEquals(
            "Phrase Management",
            english.guidedCategoryTitle(GuidedVocabularyCategory.PhraseManagement)
        )
        assertEquals(
            GuidedVocabularyCategory.PHRASE_MANAGEMENT_INDEX,
            titles.indexOf("Phrase Management")
        )
    }

    @Test
    fun phraseManagementIsEyeSelectableDestination() {
        assertEquals(
            CategoryAreaDestination.PhraseManagement,
            CategoryAreaDestination.forCategoryIndex(GuidedVocabularyCategory.PHRASE_MANAGEMENT_INDEX)
        )
        val (left, right) = GuidedCategoryShortcuts.gestureForCategory(
            GuidedVocabularyCategory.PHRASE_MANAGEMENT_INDEX
        )
        assertEquals(
            GuidedVocabularyCategory.PHRASE_MANAGEMENT_INDEX,
            GuidedCategoryShortcuts.categoryIndexForGesture(left, right)
        )
    }

    @Test
    fun phraseManagementNotInChooseCategory() {
        val state = PhraseComposerState(mode = PhraseComposerMode.DestinationCategorySelection)
        val labels = PhraseComposerController.visibleEntries(state, english).map { it.label }
        assertFalse(labels.contains("Phrase Management"))
        assertFalse(
            CustomPhraseEngine.selectableCategories.any {
                english.caregiverPhraseCategoryLabel(it) == "Phrase Management"
            }
        )
        assertEquals(4, CustomPhraseEngine.selectableCategories.size)
    }

    @Test
    fun phraseManagementCannotBeAssignedOrSerializedAsCategory() {
        assertFalse(
            CategoryAreaDestination.isAssignableCommunicationCategory(
                GuidedVocabularyCategory.PhraseManagement
            )
        )
        assertTrue(
            CategoryAreaDestination.isManagementDestination(
                GuidedVocabularyCategory.PhraseManagement
            )
        )
        assertEquals(null, GuidedVocabularyCategory.PhraseManagement.toCaregiverCategory())
        val pages = GuidedVocabularyCatalog.buildPages(PreferredLanguage.English, english)
        val management = pages.first { it.category == GuidedVocabularyCategory.PhraseManagement }
        assertTrue(management.entries.isEmpty())
    }

    @Test
    fun emptyStateUsesCanonicalCopy() {
        assertEquals("No saved phrases yet", english.vocabularyEmptyState)
        assertEquals(
            "Phrases you create and save will appear here.",
            english.vocabularyEmptyHint
        )
        assertEquals("Phrase Management", english.vocabularyTraining)
    }

    @Test
    fun managementListsOnlyCustomPhrasesViaRepositoryApi() {
        val custom = WinkMapping(
            left = 5,
            right = 2,
            vocabularyId = "My custom",
            isCustom = true,
            customPhrase = "My custom",
            caregiverCategory = CustomPhraseEngine.CaregiverPhraseCategory.Medical
        )
        val listed = CustomPhraseRepository.listCustomPhrases(listOf(custom) + defaultLanguageMappings())
        assertTrue(listed.all { it.isCustom })
        assertTrue(listed.any { it.customPhrase == "My custom" })
        assertFalse(listed.any { !it.isCustom })
    }

    @Test
    fun editDeleteActionsAndEmergencyRemainCanonical() {
        assertEquals("Edit Phrase", english.phraseManagementEditPhrase)
        assertEquals("Delete Phrase", english.phraseManagementDeletePhrase)
        assertEquals("Delete this phrase?", english.phraseManagementDeleteConfirmTitle)
        assertEquals("L6 R0", formatWinkSequenceShort(EMERGENCY_LEFT_WINKS, EMERGENCY_RIGHT_WINKS))
        val managementUi = readSource("app/src/main/java/com/idworx/lisa/PhraseManagementUi.kt")
        val controller = readSource("app/src/main/java/com/idworx/lisa/PhraseManagementController.kt")
        assertTrue(managementUi.contains("ComposerEmergencyCommandCard"))
        assertTrue(controller.contains("phraseManagementEditPhrase"))
        assertTrue(controller.contains("phraseManagementDeletePhrase"))
        assertFalse(managementUi.contains("OutlinedTextField"))
        assertFalse(managementUi.contains("LocalSoftwareKeyboardController"))
    }

    @Test
    fun mainActivityWiresPhraseManagementFromCategories() {
        val main = readSource("app/src/main/java/com/idworx/lisa/MainActivity.kt")
        assertTrue(main.contains("openPhraseManagementFromCategories"))
        assertTrue(main.contains("CategoryAreaDestination.PhraseManagement"))
        assertTrue(main.contains("phraseManagementOpenedFromCategories"))
        assertTrue(main.contains("LisaPanel.VocabularyTraining"))
    }

    @Test
    fun composerUiUsesCanonicalSavedHeading() {
        val ui = readSource("app/src/main/java/com/idworx/lisa/PhraseComposerUi.kt")
        assertTrue(ui.contains("phraseComposerSuccessTitle"))
        assertTrue(ui.contains("BottomAlignedEyeKeyboard"))
        assertFalse(ui.contains("OutlinedTextField"))
    }

    @Test
    fun createAnotherAndViewStillOnSuccessEntries() {
        assertNotNull(
            PhraseComposerController.visibleEntries(
                PhraseComposerState(
                    mode = PhraseComposerMode.Success,
                    savedMapping = WinkMapping(
                        left = 2,
                        right = 1,
                        vocabularyId = "x",
                        isCustom = true,
                        customPhrase = "x",
                        caregiverCategory = CustomPhraseEngine.CaregiverPhraseCategory.Conversation
                    )
                ),
                english
            ).firstOrNull { it.actionId == PhraseComposerActionId.ViewInCategory }
        )
    }
}
