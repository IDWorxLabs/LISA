package com.idworx.lisa

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** RC7C2 — eye-controlled phrase composer (RC7D keyboard). */
class Rc7C2EyeControlledPhraseComposerTest {

    private val english = LisaUiStrings.forLanguage(PreferredLanguage.English)

    private fun keyboardState(
        phrase: String = "",
        category: CustomPhraseEngine.CaregiverPhraseCategory =
            CustomPhraseEngine.CaregiverPhraseCategory.Conversation,
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

    @Test
    fun composerOpensWithKeyboardMode() {
        val state = PhraseComposerController.initialState()
        assertEquals(PhraseComposerMode.Keyboard, state.mode)
    }

    @Test
    fun keyboardHasFullQwertyLayout() {
        assertTrue(KeyboardLayout.allKeysReachable())
        assertEquals('Q', KeyboardLayout.keyAt(EyeKeyboardLayoutMode.Letters, 0, 0))
        assertEquals('M', KeyboardLayout.keyAt(EyeKeyboardLayoutMode.Letters, 2, 6))
    }

    @Test
    fun selectKeyUpdatesPhrase() {
        var state = keyboardState()
        state = processCommand(state, PhraseComposerActionId.SelectKey)
        assertEquals("q", state.phraseText)
        state = state.copy(cursorCol = 1)
        state = processCommand(state, PhraseComposerActionId.SelectKey)
        assertEquals("qw", state.phraseText)
    }

    @Test
    fun spaceActionWorks() {
        var state = keyboardState("hello", cursorRow = KeyboardLayout.spaceRowIndex(EyeKeyboardLayoutMode.Letters), cursorCol = 0)
        state = processCommand(state, PhraseComposerActionId.SelectKey)
        assertEquals("hello ", state.phraseText)
    }

    @Test
    fun backspaceWorks() {
        var state = keyboardState("hello")
        state = processCommand(state, PhraseComposerActionId.Backspace)
        assertEquals("hell", state.phraseText)
    }

    @Test
    fun previewWorks() {
        val result = PhraseComposerController.processSequence(
            commandAction(keyboardState("hello"), PhraseComposerActionId.Preview).left,
            commandAction(keyboardState("hello"), PhraseComposerActionId.Preview).right,
            keyboardState("hello"),
            english
        )
        assertTrue(result is PhraseComposerSequenceResult.Preview)
    }

    @Test
    fun saveOpensDestinationSelectionBeforePersist() {
        val state = processCommand(keyboardState("hello"), PhraseComposerActionId.Save)
        assertEquals(PhraseComposerMode.DestinationCategorySelection, state.mode)
    }

    @Test
    fun destinationCategorySelectionHasCategories() {
        val entries = PhraseComposerController.visibleEntries(
            PhraseComposerState(mode = PhraseComposerMode.DestinationCategorySelection),
            english
        )
        assertEquals(CustomPhraseEngine.selectableCategories.size, entries.size)
    }

    @Test
    fun categorySelectionLeadsToSaveConfirmation() {
        var state = processCommand(keyboardState("hello"), PhraseComposerActionId.Save)
        val entry = PhraseComposerController.visibleEntries(state, english).first()
        val result = PhraseComposerController.processSequence(
            entry.left, entry.right,
            state,
            english
        ) as PhraseComposerSequenceResult.Navigate
        assertEquals(PhraseComposerMode.SaveConfirmation, result.newState.mode)
    }

    @Test
    fun confirmSaveUsesRc7BSavePath() {
        var state = processCommand(keyboardState("hello"), PhraseComposerActionId.Save)
        val entry = PhraseComposerController.visibleEntries(state, english).first()
        state = (PhraseComposerController.processSequence(entry.left, entry.right, state, english)
            as PhraseComposerSequenceResult.Navigate).newState
        val confirm = PhraseComposerController.commandPanelEntries(state, english)
            .first { it.actionId == PhraseComposerActionId.ConfirmSave }
        val result = PhraseComposerController.processSequence(confirm.left, confirm.right, state, english)
        assertTrue(result is PhraseComposerSequenceResult.Save)
    }

    @Test
    fun successScreenHasFollowUpActions() {
        val entries = PhraseComposerController.visibleEntries(
            PhraseComposerState(
                mode = PhraseComposerMode.Success,
                selectedCategory = CustomPhraseEngine.CaregiverPhraseCategory.Medical,
                savedMapping = WinkMapping(
                    left = 5,
                    right = 1,
                    vocabularyId = "saved",
                    isCustom = true,
                    customPhrase = "saved",
                    caregiverCategory = CustomPhraseEngine.CaregiverPhraseCategory.Medical
                )
            ),
            english
        )
        assertTrue(entries.any { it.actionId == PhraseComposerActionId.CreateAnother })
        assertTrue(entries.any { it.actionId == PhraseComposerActionId.ViewInCategory })
    }

    @Test
    fun categoryAwareAllocationStillUsed() {
        val seq = CustomPhraseEngine.allocateSequence(
            CustomPhraseEngine.CaregiverPhraseCategory.Medical,
            emptyList()
        )
        org.junit.Assert.assertNotNull(seq)
    }

    @Test
    fun noTouchTextInputInComposerUi() {
        val composerUi = readSource("app/src/main/java/com/idworx/lisa/PhraseComposerUi.kt")
        assertFalse(composerUi.contains("OutlinedTextField"))
    }

    @Test
    fun mainActivityRoutesComposerGestures() {
        val mainActivity = readSource("app/src/main/java/com/idworx/lisa/MainActivity.kt")
        assertTrue(mainActivity.contains("handlePhraseComposerSequence"))
        assertTrue(mainActivity.contains("ModeScopedGestureAuthority.routingTarget"))
    }

    @Test
    fun cancelConfirmAllowsReturnToKeyboardOrAbandon() {
        val backResult = PhraseComposerController.processSequence(
            GuidedModeNavigation.BACK_LEFT,
            GuidedModeNavigation.BACK_RIGHT,
            keyboardState("hi"),
            english
        ) as PhraseComposerSequenceResult.Navigate
        assertEquals(PhraseComposerMode.CancelConfirm, backResult.newState.mode)
        val keep = PhraseComposerController.visibleEntries(backResult.newState, english)
            .first { it.actionId == PhraseComposerActionId.KeepComposing }
        val kept = PhraseComposerController.processSequence(keep.left, keep.right, backResult.newState, english)
            as PhraseComposerSequenceResult.Navigate
        assertEquals(PhraseComposerMode.Keyboard, kept.newState.mode)
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
