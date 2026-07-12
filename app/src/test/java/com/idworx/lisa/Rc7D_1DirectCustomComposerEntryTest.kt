package com.idworx.lisa

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** RC7D.1 — direct Custom composer entry and full-screen compose mode. */
class Rc7D_1DirectCustomComposerEntryTest {

    private val english = LisaUiStrings.forLanguage(PreferredLanguage.English)

    private fun keyboardState(
        phrase: String = "",
        cursorRow: Int = 0,
        cursorCol: Int = 0,
        category: CustomPhraseEngine.CaregiverPhraseCategory? = null
    ) = PhraseComposerState(
        mode = PhraseComposerMode.Keyboard,
        selectedCategory = category,
        phraseText = phrase,
        cursorRow = cursorRow,
        cursorCol = cursorCol
    )

    private fun destinationState(phrase: String = "hello") = PhraseComposerState(
        mode = PhraseComposerMode.DestinationCategorySelection,
        phraseText = phrase
    )

    private fun commandAction(state: PhraseComposerState, actionId: PhraseComposerActionId) =
        PhraseComposerController.commandPanelEntries(state, english).first { it.actionId == actionId }

    private fun processCommand(state: PhraseComposerState, actionId: PhraseComposerActionId): PhraseComposerState {
        val entry = commandAction(state, actionId)
        return (PhraseComposerController.processSequence(entry.left, entry.right, state, english)
            as PhraseComposerSequenceResult.Navigate).newState
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

    private fun extractFunction(signature: String): String {
        val source = readSource("app/src/main/java/com/idworx/lisa/LisaAccessibilityUi.kt")
        val start = source.indexOf(signature)
        assertTrue("Expected $signature", start >= 0)
        val openBrace = source.indexOf('{', start)
        var depth = 0
        for (index in openBrace until source.length) {
            when (source[index]) {
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) return source.substring(start, index +  1)
                }
            }
        }
        error("Unterminated function: $signature")
    }

    // 1–2. Custom opens compose mode; no Custom vocabulary page.

    @Test
    fun customCategoryIndexOpensComposeModeInMainActivity() {
        val main = readSource("app/src/main/java/com/idworx/lisa/MainActivity.kt")
        assertTrue(main.contains("openComposeModeFromCustom"))
        assertTrue(main.contains("GuidedVocabularyCategory.CUSTOM_CATEGORY_INDEX"))
        assertTrue(main.contains("PhraseComposerController.keyboardEntryState()"))
        assertTrue(main.contains("LisaPanel.PhraseEditor"))
    }

    @Test
    fun customPageNeverRendersStoredPhrases() {
        val pages = GuidedVocabularyCatalog.buildPages(
            PreferredLanguage.English,
            english,
            GuidedCatalogContext(
                caregiverCustomPhrases = listOf(
                    CustomPhraseEngine.CaregiverCustomPhraseEntry(
                        phrase = "I want to watch a movie",
                        left = 5,
                        right = 1,
                        category = CustomPhraseEngine.CaregiverPhraseCategory.Conversation
                    )
                )
            )
        )
        val customPage = pages.first { it.category == GuidedVocabularyCategory.Custom }
        assertTrue(customPage.entries.isEmpty())
        val conversation = pages.first { it.category == GuidedVocabularyCategory.Conversation }
        assertTrue(conversation.entries.any { it.phrase == "I want to watch a movie" })
    }

    // 3–4. No Create phrase card; Custom absent from destination choices.

    @Test
    fun vocabularyPanelShowsCustomComposerNoteNotCreateCard() {
        val panel = extractFunction("private fun VocabularyTrainingPanel")
        assertTrue(panel.contains("vocabularyCustomComposerNote"))
        assertFalse(panel.contains("VocabularyCreatePhraseCard"))
    }

    @Test
    fun destinationCategoriesExcludeCustom() {
        assertFalse(CustomPhraseEngine.selectableCategories.contains(CustomPhraseEngine.CaregiverPhraseCategory.Custom))
        assertEquals(4, CustomPhraseEngine.selectableCategories.size)
    }

    // 5–8. Keyboard is first screen; full QWERTY rendered.

    @Test
    fun keyboardIsFirstComposerScreen() {
        val state = PhraseComposerController.keyboardEntryState()
        assertEquals(PhraseComposerMode.Keyboard, state.mode)
        assertEquals(null, state.selectedCategory)
        assertEquals(0, state.cursorRow)
        assertEquals(0, state.cursorCol)
        assertEquals('Q', state.keyboardCursor().currentKey(EyeKeyboardLayoutMode.Letters))
    }

    @Test
    fun fullQwertyKeyboardRendered() {
        assertTrue(KeyboardLayout.allKeysReachable())
        assertEquals(10, KeyboardLayout.rowLength(EyeKeyboardLayoutMode.Letters, 0))
        assertEquals(9, KeyboardLayout.rowLength(EyeKeyboardLayoutMode.Letters, 1))
        assertEquals(7, KeyboardLayout.rowLength(EyeKeyboardLayoutMode.Letters, 2))
        assertEquals(
            1,
            KeyboardLayout.rowLength(
                EyeKeyboardLayoutMode.Letters,
                KeyboardLayout.spaceRowIndex(EyeKeyboardLayoutMode.Letters)
            )
        )
    }

    // 9. Communication workspace hidden while composing.

    @Test
    fun fullScreenComposeHidesCommunicationWorkspace() {
        val ui = readSource("app/src/main/java/com/idworx/lisa/LisaAccessibilityUi.kt")
        assertTrue(ui.contains("phraseComposerActive"))
        assertTrue(ui.contains("if (!phraseComposerActive)"))
        assertTrue(ui.contains("EyeControlledPhraseComposerOverlay"))
        val composerUi = readSource("app/src/main/java/com/idworx/lisa/PhraseComposerUi.kt")
        assertTrue(composerUi.contains("BottomAlignedEyeKeyboard"))
    }

    // 10–13. Composer panel commands and cursor behaviour.

    @Test
    fun composerPanelHasAllRequiredCommandsWithSequences() {
        val panel = PhraseComposerController.commandPanelEntries(keyboardState(), english)
        val ids = panel.map { it.actionId }.toSet()
        assertTrue(ids.containsAll(PhraseComposerController.keyboardCommandActionIds()))
        assertTrue(panel.all { it.sequenceLabel.isNotBlank() })
    }

    @Test
    fun cursorReachesEveryKey() {
        for (row in 0 until KeyboardLayout.totalRowCount(EyeKeyboardLayoutMode.Letters)) {
            for (col in 0 until KeyboardLayout.rowLength(EyeKeyboardLayoutMode.Letters, row)) {
                assertNotNull(KeyboardLayout.slotAt(EyeKeyboardLayoutMode.Letters, row, col))
            }
        }
    }

    @Test
    fun selectInsertsHighlightedKey() {
        val state = processCommand(keyboardState(), PhraseComposerActionId.SelectKey)
        assertEquals("q", state.phraseText)
    }

    @Test
    fun backspaceRemovesOneCharacter() {
        val state = processCommand(keyboardState("hello"), PhraseComposerActionId.Backspace)
        assertEquals("hell", state.phraseText)
    }

    // 14–18. Save → destination → confirm flow.

    @Test
    fun saveOpensDestinationSelection() {
        val state = processCommand(keyboardState("water"), PhraseComposerActionId.Save)
        assertEquals(PhraseComposerMode.DestinationCategorySelection, state.mode)
        assertEquals("water", state.phraseText)
    }

    @Test
    fun destinationBackReturnsToKeyboardWithStatePreserved() {
        val result = PhraseComposerController.processSequence(
            GuidedModeNavigation.BACK_LEFT,
            GuidedModeNavigation.BACK_RIGHT,
            destinationState("water"),
            english
        ) as PhraseComposerSequenceResult.Navigate
        assertEquals(PhraseComposerMode.Keyboard, result.newState.mode)
        assertEquals("water", result.newState.phraseText)
    }

    @Test
    fun categorySelectionOpensSaveConfirmationBeforePersist() {
        val entry = PhraseComposerController.visibleEntries(destinationState("water"), english).first()
        val result = PhraseComposerController.processSequence(
            entry.left,
            entry.right,
            destinationState("water"),
            english
        ) as PhraseComposerSequenceResult.Navigate
        assertEquals(PhraseComposerMode.SaveConfirmation, result.newState.mode)
        assertEquals("water", result.newState.phraseText)
    }

    @Test
    fun confirmSaveDispatchesSaveIntentNotImmediateSuccess() {
        var state = destinationState("water")
        val categoryEntry = PhraseComposerController.visibleEntries(state, english).first()
        state = (PhraseComposerController.processSequence(
            categoryEntry.left,
            categoryEntry.right,
            state,
            english
        ) as PhraseComposerSequenceResult.Navigate).newState
        val confirm = PhraseComposerController.commandPanelEntries(state, english)
            .first { it.actionId == PhraseComposerActionId.ConfirmSave }
        val result = PhraseComposerController.processSequence(confirm.left, confirm.right, state, english)
        assertTrue(result is PhraseComposerSequenceResult.Save)
    }

    @Test
    fun cancelSaveDoesNotPersist() {
        var state = destinationState("water")
        val categoryEntry = PhraseComposerController.visibleEntries(state, english).first()
        state = (PhraseComposerController.processSequence(
            categoryEntry.left,
            categoryEntry.right,
            state,
            english
        ) as PhraseComposerSequenceResult.Navigate).newState
        val back = PhraseComposerController.commandPanelEntries(state, english)
            .first { it.actionId == PhraseComposerActionId.Back }
        state = (PhraseComposerController.processSequence(back.left, back.right, state, english)
            as PhraseComposerSequenceResult.Navigate).newState
        assertEquals(PhraseComposerMode.Keyboard, state.mode)
        assertEquals("water", state.phraseText)
    }

    // 19–20. Return opens destination category (MainActivity wiring).

    @Test
    fun returnToCommunicationOpensDestinationCategory() {
        val main = readSource("app/src/main/java/com/idworx/lisa/MainActivity.kt")
        assertTrue(main.contains("exitComposeMode(openDestinationCategory = savedCategory)"))
        assertTrue(main.contains("openCategoryDirectly"))
    }

    // 21–22. Back / discard behaviour.

    @Test
    fun emptyBackExitsComposeDirectly() {
        val result = PhraseComposerController.processSequence(
            GuidedModeNavigation.BACK_LEFT,
            GuidedModeNavigation.BACK_RIGHT,
            keyboardState(),
            english
        )
        assertTrue(result is PhraseComposerSequenceResult.ExitToPreviousPanel)
    }

    @Test
    fun nonEmptyBackRequiresDiscardConfirmation() {
        val result = PhraseComposerController.processSequence(
            GuidedModeNavigation.BACK_LEFT,
            GuidedModeNavigation.BACK_RIGHT,
            keyboardState("hi"),
            english
        ) as PhraseComposerSequenceResult.Navigate
        assertEquals(PhraseComposerMode.CancelConfirm, result.newState.mode)
    }

    // 23–24. Emergency and MSGA isolation.

    @Test
    fun emergencyRemainsAvailableInComposerMode() {
        val context = LisaGestureContext(
            activePanel = LisaPanel.PhraseEditor,
            guidedOverlayActive = false,
            guidedScreenMode = null,
            isAdjustingPreference = false,
            phraseComposerMode = PhraseComposerMode.Keyboard
        )
        assertEquals(
            GestureRoutingTarget.Emergency,
            ModeScopedGestureAuthority.routingTarget(context, EMERGENCY_LEFT_WINKS, EMERGENCY_RIGHT_WINKS)
        )
    }

    @Test
    fun communicationGesturesInactiveDuringComposeMode() {
        val context = LisaGestureContext(
            activePanel = LisaPanel.PhraseEditor,
            guidedOverlayActive = false,
            guidedScreenMode = null,
            isAdjustingPreference = false,
            phraseComposerMode = PhraseComposerMode.Keyboard
        )
        assertEquals(
            GestureRoutingTarget.PhraseComposer,
            ModeScopedGestureAuthority.routingTarget(
                context,
                GuidedModeNavigation.SELECT_LEFT,
                GuidedModeNavigation.SELECT_RIGHT
            )
        )
    }

    // 25. Single canonical composer path.

    @Test
    fun noDuplicateOldComposerRoutesRemain() {
        val main = readSource("app/src/main/java/com/idworx/lisa/MainActivity.kt")
        assertFalse(main.contains("openPhraseComposer(LisaPanel.CreatePhrase)"))
        val ui = readSource("app/src/main/java/com/idworx/lisa/PhraseComposerUi.kt")
        assertTrue(ui.contains("BottomAlignedEyeKeyboard"))
        assertFalse(ui.contains("phraseComposerAlphabetPageTitle"))
    }

    // 5. Migration of legacy Custom-category data.

    @Test
    fun customCategoryDataMigratesToGeneralConversation() {
        val legacy = listOf(
            WinkMapping(
                left = 5,
                right = 1,
                vocabularyId = "I want to watch a movie",
                isCustom = true,
                customPhrase = "I want to watch a movie",
                caregiverCategory = CustomPhraseEngine.CaregiverPhraseCategory.Custom
            )
        )
        val migration = CustomPhraseEngine.migrateCustomCategoryMappings(legacy)
        assertEquals(1, migration.migratedCount)
        assertEquals(
            CustomPhraseEngine.CaregiverPhraseCategory.Conversation,
            migration.mappings.single().caregiverCategory
        )
        assertEquals("I want to watch a movie", migration.mappings.single().customPhrase)
    }

    @Test
    fun migrationIsIdempotent() {
        val once = CustomPhraseEngine.migrateCustomCategoryMappings(
            listOf(
                WinkMapping(
                    left = 5,
                    right = 1,
                    vocabularyId = "test",
                    isCustom = true,
                    customPhrase = "test",
                    caregiverCategory = CustomPhraseEngine.CaregiverPhraseCategory.Custom
                )
            )
        )
        val twice = CustomPhraseEngine.migrateCustomCategoryMappings(once.mappings)
        assertEquals(0, twice.migratedCount)
    }

    @Test
    fun createAnotherResetsToKeyboardEntry() {
        val success = PhraseComposerState(mode = PhraseComposerMode.Success)
        val entry = PhraseComposerController.visibleEntries(success, english)
            .first { it.actionId == PhraseComposerActionId.CreateAnother }
        val result = PhraseComposerController.processSequence(entry.left, entry.right, success, english)
            as PhraseComposerSequenceResult.Navigate
        assertEquals(PhraseComposerMode.Keyboard, result.newState.mode)
        assertEquals("", result.newState.phraseText)
    }
}
