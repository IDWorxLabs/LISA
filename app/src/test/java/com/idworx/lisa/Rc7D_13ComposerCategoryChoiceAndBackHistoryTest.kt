package com.idworx.lisa

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** RC7D.13 — choose-category create flow + true previous-screen Back navigation. */
class Rc7D_13ComposerCategoryChoiceAndBackHistoryTest {

    private val english = LisaUiStrings.forLanguage(PreferredLanguage.English)

    private fun keyboard(phrase: String = "") = PhraseComposerState(
        mode = PhraseComposerMode.Keyboard,
        phraseText = phrase
    )

    private fun command(state: PhraseComposerState, actionId: PhraseComposerActionId) =
        PhraseComposerController.commandPanelEntries(state, english).first { it.actionId == actionId }

    private fun process(
        state: PhraseComposerState,
        actionId: PhraseComposerActionId,
        runtime: PhraseComposerRuntimeContext? = null
    ): PhraseComposerState {
        val entry = if (actionId in setOf(
                PhraseComposerActionId.SelectCategory,
                PhraseComposerActionId.ChooseAnotherCategory,
                PhraseComposerActionId.ContinueEditing,
                PhraseComposerActionId.OpenDuplicateCategory,
                PhraseComposerActionId.ViewInCategory,
                PhraseComposerActionId.CreateAnother
            )
        ) {
            PhraseComposerController.visibleEntries(state, english).firstOrNull { it.actionId == actionId }
                ?: command(state, actionId)
        } else {
            command(state, actionId)
        }
        return (PhraseComposerController.processSequence(
            entry.left,
            entry.right,
            state,
            english,
            runtime
        ) as PhraseComposerSequenceResult.Navigate).newState
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

    @Test
    fun createComposerDoesNotAutoOpenCategory() {
        val state = PhraseComposerController.keyboardEntryState()
        assertEquals(PhraseComposerMode.Keyboard, state.mode)
        assertNull(state.selectedCategory)
        assertTrue(state.navigationHistory.isEmpty())
    }

    @Test
    fun completingPhraseOpensChooseCategoryWithoutOpeningIt() {
        val next = process(keyboard("My stomach hurts"), PhraseComposerActionId.Save)
        assertEquals(PhraseComposerMode.DestinationCategorySelection, next.mode)
        assertNull(next.selectedCategory)
        assertEquals(listOf(PhraseComposerMode.Keyboard), next.navigationHistory)
        assertEquals("My stomach hurts", next.phraseText)
    }

    @Test
    fun allCanonicalCategoriesAreAvailableForChoice() {
        val state = PhraseComposerState(mode = PhraseComposerMode.DestinationCategorySelection)
        val labels = PhraseComposerController.visibleEntries(state, english).map { it.label }
        CustomPhraseEngine.selectableCategories.forEach { category ->
            assertTrue(labels.contains(english.caregiverPhraseCategoryLabel(category)))
        }
        assertFalse(labels.any { it.startsWith("Open ") })
        assertFalse(labels.any { it.startsWith("View in ") })
    }

    @Test
    fun selectingMedicalAssignsMedicalButDoesNotOpenMedical() {
        var state = process(keyboard("My stomach hurts"), PhraseComposerActionId.Save)
        val medical = PhraseComposerController.visibleEntries(state, english)
            .first { it.category == CustomPhraseEngine.CaregiverPhraseCategory.Medical }
        state = (PhraseComposerController.processSequence(
            medical.left, medical.right, state, english
        ) as PhraseComposerSequenceResult.Navigate).newState
        assertEquals(PhraseComposerMode.SaveConfirmation, state.mode)
        assertEquals(CustomPhraseEngine.CaregiverPhraseCategory.Medical, state.selectedCategory)
        assertFalse(state.mode.name.contains("Vocabulary", ignoreCase = true))
    }

    @Test
    fun confirmSaveBackReturnsToChooseCategoryPreservingPhrase() {
        var state = process(keyboard("Need water"), PhraseComposerActionId.Save)
        val category = PhraseComposerController.visibleEntries(state, english).first()
        state = (PhraseComposerController.processSequence(
            category.left, category.right, state, english
        ) as PhraseComposerSequenceResult.Navigate).newState
        val back = PhraseComposerController.processSequence(
            GuidedModeNavigation.BACK_LEFT,
            GuidedModeNavigation.BACK_RIGHT,
            state,
            english
        ) as PhraseComposerSequenceResult.Navigate
        assertEquals(PhraseComposerMode.DestinationCategorySelection, back.newState.mode)
        assertEquals("Need water", back.newState.phraseText)
    }

    @Test
    fun chooseCategoryBackReturnsToComposerPreservingPhrase() {
        val state = process(keyboard("Call family"), PhraseComposerActionId.Save)
        val back = PhraseComposerController.processSequence(
            GuidedModeNavigation.BACK_LEFT,
            GuidedModeNavigation.BACK_RIGHT,
            state,
            english
        ) as PhraseComposerSequenceResult.Navigate
        assertEquals(PhraseComposerMode.Keyboard, back.newState.mode)
        assertEquals("Call family", back.newState.phraseText)
    }

    @Test
    fun successOffersViewInCategoryAndCreateAnotherNotAutoOpen() {
        val success = PhraseComposerState(
            mode = PhraseComposerMode.Success,
            phraseText = "Saved phrase",
            selectedCategory = CustomPhraseEngine.CaregiverPhraseCategory.Family,
            savedMapping = WinkMapping(
                left = 5,
                right = 1,
                vocabularyId = "Saved phrase",
                isCustom = true,
                customPhrase = "Saved phrase",
                caregiverCategory = CustomPhraseEngine.CaregiverPhraseCategory.Family
            ),
            navigationHistory = listOf(
                PhraseComposerMode.Keyboard,
                PhraseComposerMode.DestinationCategorySelection,
                PhraseComposerMode.SaveConfirmation
            )
        )
        val entries = PhraseComposerController.visibleEntries(success, english)
        assertTrue(entries.any { it.actionId == PhraseComposerActionId.ViewInCategory })
        assertTrue(entries.any { it.actionId == PhraseComposerActionId.CreateAnother })
        assertFalse(entries.any { it.actionId == PhraseComposerActionId.ReturnToCommunication })
        val view = entries.first { it.actionId == PhraseComposerActionId.ViewInCategory }
        val result = PhraseComposerController.processSequence(view.left, view.right, success, english)
        assertTrue(result is PhraseComposerSequenceResult.ViewSavedCategory)
        assertEquals(
            CustomPhraseEngine.CaregiverPhraseCategory.Family,
            (result as PhraseComposerSequenceResult.ViewSavedCategory).category
        )
    }

    @Test
    fun createAnotherReturnsToBlankEyeControlledComposer() {
        val success = PhraseComposerState(mode = PhraseComposerMode.Success, phraseText = "x")
        val create = PhraseComposerController.visibleEntries(success, english)
            .first { it.actionId == PhraseComposerActionId.CreateAnother }
        val next = (PhraseComposerController.processSequence(
            create.left, create.right, success, english
        ) as PhraseComposerSequenceResult.Navigate).newState
        assertEquals(PhraseComposerMode.Keyboard, next.mode)
        assertEquals("", next.phraseText)
        assertTrue(next.navigationHistory.isEmpty())
        assertTrue(readSource("app/src/main/java/com/idworx/lisa/PhraseComposerUi.kt").contains("BottomAlignedEyeKeyboard"))
    }

    @Test
    fun duplicateOccursAfterCategorySelectionNotOnComposerSave() {
        val runtime = PhraseComposerRuntimeContext(customMappings = emptyList())
        val afterSave = PhraseComposerController.processSequence(
            command(keyboard("Hello"), PhraseComposerActionId.Save).left,
            command(keyboard("Hello"), PhraseComposerActionId.Save).right,
            keyboard("Hello"),
            english,
            runtime
        ) as PhraseComposerSequenceResult.Navigate
        assertEquals(PhraseComposerMode.DestinationCategorySelection, afterSave.newState.mode)

        val category = PhraseComposerController.visibleEntries(afterSave.newState, english)
            .first { it.category == CustomPhraseEngine.CaregiverPhraseCategory.Conversation }
        val confirm = (PhraseComposerController.processSequence(
            category.left, category.right, afterSave.newState, english, runtime
        ) as PhraseComposerSequenceResult.Navigate).newState
        assertEquals(PhraseComposerMode.SaveConfirmation, confirm.mode)

        val duplicate = PhraseComposerController.processSequence(
            command(confirm, PhraseComposerActionId.ConfirmSave).left,
            command(confirm, PhraseComposerActionId.ConfirmSave).right,
            confirm,
            english,
            runtime
        ) as PhraseComposerSequenceResult.Navigate
        assertEquals(PhraseComposerMode.DuplicateWarning, duplicate.newState.mode)
        assertNotNull(duplicate.newState.duplicateMatch)
    }

    @Test
    fun duplicateOffersChooseAnotherContinueAndViewExisting() {
        val state = PhraseComposerState(
            mode = PhraseComposerMode.DuplicateWarning,
            phraseText = "Hello",
            selectedCategory = CustomPhraseEngine.CaregiverPhraseCategory.Conversation,
            duplicateMatch = DuplicatePhraseMatch(
                phrase = "Hello",
                category = CustomPhraseEngine.CaregiverPhraseCategory.Conversation,
                source = PhraseDuplicateSource.BuiltIn
            ),
            navigationHistory = listOf(
                PhraseComposerMode.Keyboard,
                PhraseComposerMode.DestinationCategorySelection,
                PhraseComposerMode.SaveConfirmation
            )
        )
        val entries = PhraseComposerController.visibleEntries(state, english)
        assertTrue(entries.any { it.actionId == PhraseComposerActionId.ChooseAnotherCategory })
        assertTrue(entries.any { it.actionId == PhraseComposerActionId.ContinueEditing })
        assertTrue(entries.any { it.actionId == PhraseComposerActionId.OpenDuplicateCategory })
        assertFalse(entries.any { it.label.startsWith("Open ") })
    }

    @Test
    fun chooseAnotherCategoryPreservesPhraseAndReturnsToCategorySelection() {
        val state = PhraseComposerState(
            mode = PhraseComposerMode.DuplicateWarning,
            phraseText = "Hello again",
            selectedCategory = CustomPhraseEngine.CaregiverPhraseCategory.Medical,
            duplicateMatch = DuplicatePhraseMatch(
                phrase = "Hello",
                category = CustomPhraseEngine.CaregiverPhraseCategory.Conversation,
                source = PhraseDuplicateSource.BuiltIn
            ),
            navigationHistory = listOf(
                PhraseComposerMode.Keyboard,
                PhraseComposerMode.DestinationCategorySelection,
                PhraseComposerMode.SaveConfirmation
            )
        )
        val choose = PhraseComposerController.visibleEntries(state, english)
            .first { it.actionId == PhraseComposerActionId.ChooseAnotherCategory }
        val next = (PhraseComposerController.processSequence(
            choose.left, choose.right, state, english
        ) as PhraseComposerSequenceResult.Navigate).newState
        assertEquals(PhraseComposerMode.DestinationCategorySelection, next.mode)
        assertEquals("Hello again", next.phraseText)
        assertNull(next.duplicateMatch)
    }

    @Test
    fun continueEditingPreservesPhrase() {
        val state = PhraseComposerState(
            mode = PhraseComposerMode.DuplicateWarning,
            phraseText = "Keep drafting",
            duplicateMatch = DuplicatePhraseMatch(
                phrase = "Hello",
                category = CustomPhraseEngine.CaregiverPhraseCategory.Conversation,
                source = PhraseDuplicateSource.BuiltIn
            )
        )
        val cont = PhraseComposerController.visibleEntries(state, english)
            .first { it.actionId == PhraseComposerActionId.ContinueEditing }
        val next = (PhraseComposerController.processSequence(
            cont.left, cont.right, state, english
        ) as PhraseComposerSequenceResult.Navigate).newState
        assertEquals(PhraseComposerMode.Keyboard, next.mode)
        assertEquals("Keep drafting", next.phraseText)
    }

    @Test
    fun duplicateBackReturnsToConfirmSave() {
        val state = PhraseComposerState(
            mode = PhraseComposerMode.DuplicateWarning,
            phraseText = "Hello",
            selectedCategory = CustomPhraseEngine.CaregiverPhraseCategory.Conversation,
            navigationHistory = listOf(
                PhraseComposerMode.Keyboard,
                PhraseComposerMode.DestinationCategorySelection,
                PhraseComposerMode.SaveConfirmation
            ),
            duplicateMatch = DuplicatePhraseMatch(
                phrase = "Hello",
                category = CustomPhraseEngine.CaregiverPhraseCategory.Conversation,
                source = PhraseDuplicateSource.BuiltIn
            )
        )
        val back = PhraseComposerController.processSequence(
            GuidedModeNavigation.BACK_LEFT,
            GuidedModeNavigation.BACK_RIGHT,
            state,
            english
        ) as PhraseComposerSequenceResult.Navigate
        assertEquals(PhraseComposerMode.SaveConfirmation, back.newState.mode)
        assertEquals("Hello", back.newState.phraseText)
    }

    @Test
    fun successBackReturnsToConfirmSaveWithoutRerunningSave() {
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
        assertEquals(PhraseComposerMode.SaveConfirmation, back.newState.mode)
        assertEquals(mapping, back.newState.savedMapping)
        val confirm = PhraseComposerController.processSequence(
            command(back.newState, PhraseComposerActionId.ConfirmSave).left,
            command(back.newState, PhraseComposerActionId.ConfirmSave).right,
            back.newState,
            english
        ) as PhraseComposerSequenceResult.Navigate
        assertEquals(PhraseComposerMode.Success, confirm.newState.mode)
        assertTrue(confirm.newState.savedMapping != null)
    }

    @Test
    fun noSystemKeyboardAndEyeKeyboardRemain() {
        val ui = readSource("app/src/main/java/com/idworx/lisa/PhraseComposerUi.kt")
        assertTrue(ui.contains("BottomAlignedEyeKeyboard"))
        assertFalse(ui.contains("OutlinedTextField"))
        assertFalse(ui.contains("LocalSoftwareKeyboardController"))
        val main = readSource("app/src/main/java/com/idworx/lisa/MainActivity.kt")
        assertTrue(main.contains("composerReturnAfterCategoryView"))
        assertTrue(main.contains("ViewSavedCategory"))
    }

    @Test
    fun emergencyStillShowsCanonicalL6R0() {
        assertEquals("L6 R0", formatWinkSequenceShort(EMERGENCY_LEFT_WINKS, EMERGENCY_RIGHT_WINKS))
        val grid = readSource("app/src/main/java/com/idworx/lisa/ComposerCommandGrid.kt")
        assertTrue(grid.contains("EMERGENCY_LEFT_WINKS"))
        assertTrue(grid.contains("guidedEmergencyNavTitle"))
    }
}
