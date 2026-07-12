package com.idworx.lisa

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** RC7D — eye-controlled QWERTY keyboard composer. */
class Rc7DEyeControlledKeyboardComposerTest {

    private val english = LisaUiStrings.forLanguage(PreferredLanguage.English)

    private fun categoryState() = PhraseComposerState(mode = PhraseComposerMode.DestinationCategorySelection)

    private fun keyboardState(
        phrase: String = "",
        category: CustomPhraseEngine.CaregiverPhraseCategory =
            CustomPhraseEngine.CaregiverPhraseCategory.Medical,
        cursorRow: Int = 0,
        cursorCol: Int = 0
    ) = PhraseComposerState(
        mode = PhraseComposerMode.Keyboard,
        selectedCategory = category,
        phraseText = phrase,
        cursorRow = cursorRow,
        cursorCol = cursorCol
    )

    private fun commandAction(state: PhraseComposerState, actionId: PhraseComposerActionId) =
        PhraseComposerController.commandPanelEntries(state, english).first { it.actionId == actionId }

    private fun processCommand(state: PhraseComposerState, actionId: PhraseComposerActionId): PhraseComposerState {
        val entry = commandAction(state, actionId)
        return (PhraseComposerController.processSequence(entry.left, entry.right, state, english)
            as PhraseComposerSequenceResult.Navigate).newState
    }

    private fun selectCategory(categoryIndex: Int, phrase: String = "water"): PhraseComposerState {
        var state = processCommand(keyboardState(phrase), PhraseComposerActionId.Save)
        val entry = PhraseComposerController.visibleEntries(state, english)[categoryIndex]
        return (PhraseComposerController.processSequence(entry.left, entry.right, state, english)
            as PhraseComposerSequenceResult.Navigate).newState
    }

    @Test
    fun composerOpensWithKeyboardMode() {
        val state = PhraseComposerController.initialState()
        assertEquals(PhraseComposerMode.Keyboard, state.mode)
        assertEquals(null, state.selectedCategory)
    }

    @Test
    fun keyboardLayoutRendersAllKeys() {
        assertTrue(KeyboardLayout.allKeysReachable())
        assertEquals(10, KeyboardLayout.rowLength(EyeKeyboardLayoutMode.Letters, 0))
        assertEquals(9, KeyboardLayout.rowLength(EyeKeyboardLayoutMode.Letters, 1))
        assertEquals(7, KeyboardLayout.rowLength(EyeKeyboardLayoutMode.Letters, 2))
        assertEquals(8, KeyboardLayout.rowLength(EyeKeyboardLayoutMode.Letters, KeyboardLayout.punctuationRowIndex(EyeKeyboardLayoutMode.Letters)))
        assertEquals(2, KeyboardLayout.rowLength(EyeKeyboardLayoutMode.Letters, KeyboardLayout.utilityRowIndex(EyeKeyboardLayoutMode.Letters)))
        assertEquals(1, KeyboardLayout.rowLength(EyeKeyboardLayoutMode.Letters, KeyboardLayout.spaceRowIndex(EyeKeyboardLayoutMode.Letters)))
    }

    @Test
    fun everyKeyboardKeyIsReachableByCursor() {
        val letters = EyeKeyboardLayoutMode.Letters
        for (row in 0 until KeyboardLayout.totalRowCount(letters)) {
            for (col in 0 until KeyboardLayout.rowLength(letters, row)) {
                assertNotNull(KeyboardLayout.slotAt(letters, row, col))
            }
        }
    }

    @Test
    fun cursorNavigationMovesInAllDirections() {
        var state = keyboardState(cursorRow = 1, cursorCol = 4)
        state = processCommand(state, PhraseComposerActionId.MoveUp)
        assertEquals(0, state.cursorRow)
        assertEquals(4, state.cursorCol)
        state = processCommand(state, PhraseComposerActionId.MoveDown)
        assertEquals(1, state.cursorRow)
        state = processCommand(state, PhraseComposerActionId.MoveLeft)
        assertEquals(3, state.cursorCol)
        state = processCommand(state, PhraseComposerActionId.MoveRight)
        assertEquals(4, state.cursorCol)
    }

    @Test
    fun selectInsertsCharacterAtCursor() {
        var state = keyboardState(cursorRow = 0, cursorCol = 0)
        state = processCommand(state, PhraseComposerActionId.SelectKey)
        assertEquals("q", state.phraseText)
    }

    @Test
    fun spaceInsertsOneSpaceWithoutLeadingOrDouble() {
        var state = keyboardState(cursorRow = KeyboardLayout.spaceRowIndex(EyeKeyboardLayoutMode.Letters), cursorCol = 0)
        state = processCommand(state, PhraseComposerActionId.SelectKey)
        assertEquals("", state.phraseText)
        state = keyboardState(phrase = "hi", cursorRow = KeyboardLayout.spaceRowIndex(EyeKeyboardLayoutMode.Letters), cursorCol = 0)
        state = processCommand(state, PhraseComposerActionId.SelectKey)
        assertEquals("hi ", state.phraseText)
        state = processCommand(state, PhraseComposerActionId.SelectKey)
        assertEquals("hi ", state.phraseText)
    }

    @Test
    fun backspaceRemovesOneCharacter() {
        var state = keyboardState(phrase = "hello")
        state = processCommand(state, PhraseComposerActionId.Backspace)
        assertEquals("hell", state.phraseText)
        state = processCommand(state, PhraseComposerActionId.Backspace)
        assertEquals("hel", state.phraseText)
    }

    @Test
    fun backspaceOnEmptyPhraseIsSafe() {
        val state = processCommand(keyboardState(), PhraseComposerActionId.Backspace)
        assertEquals("", state.phraseText)
    }

    @Test
    fun previewSpeaksViaPreviewResult() {
        val result = PhraseComposerController.processSequence(
            commandAction(keyboardState("hello"), PhraseComposerActionId.Preview).left,
            commandAction(keyboardState("hello"), PhraseComposerActionId.Preview).right,
            keyboardState("hello"),
            english
        )
        assertTrue(result is PhraseComposerSequenceResult.Preview)
        assertEquals("hello", (result as PhraseComposerSequenceResult.Preview).phrase)
    }

    @Test
    fun saveOpensDestinationSelectionNotImmediatePersist() {
        val state = processCommand(keyboardState("water"), PhraseComposerActionId.Save)
        assertEquals(PhraseComposerMode.DestinationCategorySelection, state.mode)
    }

    @Test
    fun cancelSaveReturnsToKeyboardWithPhraseIntact() {
        var state = processCommand(keyboardState("water"), PhraseComposerActionId.Save)
        val categoryEntry = PhraseComposerController.visibleEntries(state, english).first()
        state = (PhraseComposerController.processSequence(categoryEntry.left, categoryEntry.right, state, english)
            as PhraseComposerSequenceResult.Navigate).newState
        val back = PhraseComposerController.commandPanelEntries(state, english)
            .first { it.actionId == PhraseComposerActionId.Back }
        state = (PhraseComposerController.processSequence(back.left, back.right, state, english)
            as PhraseComposerSequenceResult.Navigate).newState
        assertEquals(PhraseComposerMode.Keyboard, state.mode)
        assertEquals("water", state.phraseText)
    }

    @Test
    fun confirmSaveDispatchesSaveIntent() {
        var state = processCommand(keyboardState("water"), PhraseComposerActionId.Save)
        val categoryEntry = PhraseComposerController.visibleEntries(state, english)[2]
        state = (PhraseComposerController.processSequence(categoryEntry.left, categoryEntry.right, state, english)
            as PhraseComposerSequenceResult.Navigate).newState
        val confirm = PhraseComposerController.commandPanelEntries(state, english)
            .first { it.actionId == PhraseComposerActionId.ConfirmSave }
        val result = PhraseComposerController.processSequence(confirm.left, confirm.right, state, english)
        assertTrue(result is PhraseComposerSequenceResult.Save)
        assertEquals(CustomPhraseEngine.CaregiverPhraseCategory.Medical, (result as PhraseComposerSequenceResult.Save).category)
    }

    @Test
    fun categorySelectionOpensSaveConfirmation() {
        val state = selectCategory(2)
        assertEquals(PhraseComposerMode.SaveConfirmation, state.mode)
        assertEquals(CustomPhraseEngine.CaregiverPhraseCategory.Medical, state.selectedCategory)
        assertEquals("water", state.phraseText)
    }

    @Test
    fun keyboardCommandPanelHasNavigationCommands() {
        val panel = PhraseComposerController.commandPanelEntries(keyboardState(), english)
        val ids = panel.map { it.actionId }.toSet()
        assertTrue(ids.contains(PhraseComposerActionId.MoveUp))
        assertTrue(ids.contains(PhraseComposerActionId.MoveDown))
        assertTrue(ids.contains(PhraseComposerActionId.MoveLeft))
        assertTrue(ids.contains(PhraseComposerActionId.MoveRight))
        assertTrue(ids.contains(PhraseComposerActionId.SelectKey))
        assertTrue(ids.contains(PhraseComposerActionId.Backspace))
        assertTrue(ids.contains(PhraseComposerActionId.Preview))
        assertTrue(ids.contains(PhraseComposerActionId.Save))
        assertTrue(ids.contains(PhraseComposerActionId.Back))
    }

    @Test
    fun everyCommandDisplaysBlinkSequence() {
        val state = keyboardState("test")
        assertTrue(PhraseComposerController.everyVisibleEntryHasSequence(state, english))
        assertTrue(PhraseComposerController.everyVisibleEntryHasSequence(categoryState(), english))
    }

    @Test
    fun msgaIsolationPreservedForKeyboardCommands() {
        assertTrue(ModeScopedGestureAuthorityAudit.passes())
        val commContext = LisaGestureContext(
            activePanel = LisaPanel.None,
            guidedOverlayActive = true,
            guidedScreenMode = GuidedOverlayScreenMode.Vocabulary,
            isAdjustingPreference = false,
            phraseComposerMode = null
        )
        val moveLeft = ModeScopedGestureAuthority.phraseComposerCommandSequences
            .getValue(PhraseComposerActionId.MoveLeft)
        assertEquals(
            GestureRoutingTarget.GuidedOverlay,
            ModeScopedGestureAuthority.routingTarget(commContext, moveLeft.first, moveLeft.second)
        )
    }

    @Test
    fun backFromEmptyKeyboardExitsCompose() {
        val result = PhraseComposerController.processSequence(
            GuidedModeNavigation.BACK_LEFT,
            GuidedModeNavigation.BACK_RIGHT,
            keyboardState(),
            english
        )
        assertTrue(result is PhraseComposerSequenceResult.ExitToPreviousPanel)
    }

    @Test
    fun backFromKeyboardWithTextShowsCancelConfirm() {
        val result = PhraseComposerController.processSequence(
            GuidedModeNavigation.BACK_LEFT,
            GuidedModeNavigation.BACK_RIGHT,
            keyboardState("hi"),
            english
        ) as PhraseComposerSequenceResult.Navigate
        assertEquals(PhraseComposerMode.CancelConfirm, result.newState.mode)
    }

    @Test
    fun successPageActionsUnchanged() {
        val success = PhraseComposerState(
            mode = PhraseComposerMode.Success,
            savedMapping = WinkMapping(
                left = 5,
                right = 1,
                vocabularyId = "test",
                isCustom = true,
                customPhrase = "water",
                caregiverCategory = CustomPhraseEngine.CaregiverPhraseCategory.Medical
            )
        )
        val entries = PhraseComposerController.visibleEntries(success, english)
        assertTrue(entries.any { it.actionId == PhraseComposerActionId.CreateAnother })
        assertTrue(entries.any { it.actionId == PhraseComposerActionId.ReturnToCommunication })
    }

    @Test
    fun categoryAwareAllocationEngineUnchanged() {
        val mappings = emptyList<WinkMapping>()
        val sequence = CustomPhraseEngine.allocateSequence(
            CustomPhraseEngine.CaregiverPhraseCategory.Medical,
            mappings
        )
        assertNotNull(sequence)
        assertTrue(CustomPhraseEngine.allocationSelfAuditPasses(
            CustomPhraseEngine.CaregiverPhraseCategory.Medical,
            sequence!!,
            mappings
        ))
    }

    @Test
    fun keyboardUiSourceExists() {
        val keyboardUi = readSource("app/src/main/java/com/idworx/lisa/EyeControlledKeyboard.kt")
        assertTrue(keyboardUi.contains("EyeControlledKeyboard"))
        assertTrue(keyboardUi.contains("KeyboardLayout.letterRows"))
        val composerUi = readSource("app/src/main/java/com/idworx/lisa/PhraseComposerUi.kt")
        assertFalse(composerUi.contains("phraseComposerAlphabetPageTitle"))
        assertFalse(composerUi.contains("Letters 1/5"))
    }

    @Test
    fun phraseComposerCommandAuditPasses() {
        assertTrue(
            "Keyboard command audit: ${PhraseComposerCommandAudit.auditAll()}",
            PhraseComposerCommandAudit.passes()
        )
    }

    private fun assertNotNull(value: Any?) {
        org.junit.Assert.assertNotNull(value)
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
