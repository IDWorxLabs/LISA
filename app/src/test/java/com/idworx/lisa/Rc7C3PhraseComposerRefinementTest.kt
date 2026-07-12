package com.idworx.lisa

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** RC7C3 — phrase composer refinement (RC7D keyboard workflow). */
class Rc7C3PhraseComposerRefinementTest {

    private val english = LisaUiStrings.forLanguage(PreferredLanguage.English)

    private fun keyboardState(phrase: String = "") = PhraseComposerState(
        mode = PhraseComposerMode.Keyboard,
        selectedCategory = CustomPhraseEngine.CaregiverPhraseCategory.Conversation,
        phraseText = phrase
    )

    private fun commandPanel(state: PhraseComposerState = keyboardState()) =
        PhraseComposerController.commandPanelEntries(state, english)

    private fun commandAction(state: PhraseComposerState, actionId: PhraseComposerActionId) =
        commandPanel(state).first { it.actionId == actionId }

    @Test
    fun composerStartsWithKeyboardMode() {
        assertEquals(PhraseComposerMode.Keyboard, PhraseComposerController.initialState().mode)
    }

    @Test
    fun generalConversationCategoryLabel() {
        val titles = CustomPhraseEngine.selectableCategories.map { english.caregiverPhraseCategoryLabel(it) }
        assertEquals("General Conversation", titles.first())
    }

    @Test
    fun phraseBuiltViaKeyboardSelectAndSpace() {
        var state = keyboardState()
        state = state.copy(cursorRow = 0, cursorCol = 7)
        state = process(state, PhraseComposerActionId.SelectKey)
        state = state.copy(cursorRow = KeyboardLayout.spaceRowIndex(EyeKeyboardLayoutMode.Letters), cursorCol = 0)
        state = process(state, PhraseComposerActionId.SelectKey)
        state = state.copy(cursorRow = 0, cursorCol = 1)
        state = process(state, PhraseComposerActionId.SelectKey)
        assertEquals("i w", state.phraseText)
    }

    @Test
    fun backspaceRemovesOneCharacterAtATime() {
        var state = keyboardState("hello")
        state = process(state, PhraseComposerActionId.Backspace)
        assertEquals("hell", state.phraseText)
    }

    @Test
    fun commandPanelContainsRequiredKeyboardCommands() {
        val panel = commandPanel()
        val actionIds = panel.map { it.actionId }.toSet()
        assertTrue(actionIds.containsAll(PhraseComposerController.keyboardCommandActionIds()))
    }

    @Test
    fun everyComposerActionHasVisibleSequence() {
        assertTrue(PhraseComposerController.everyVisibleEntryHasSequence(keyboardState("Hi"), english))
        var confirmState = process(keyboardState("Hi"), PhraseComposerActionId.Save)
        val categoryEntry = PhraseComposerController.visibleEntries(confirmState, english).first()
        confirmState = (PhraseComposerController.processSequence(
            categoryEntry.left,
            categoryEntry.right,
            confirmState,
            english
        ) as PhraseComposerSequenceResult.Navigate).newState
        assertTrue(PhraseComposerController.everyVisibleEntryHasSequence(confirmState, english))
    }

    @Test
    fun universalBackFromSaveConfirmationReturnsToKeyboard() {
        var state = process(keyboardState("Hi"), PhraseComposerActionId.Save)
        val categoryEntry = PhraseComposerController.visibleEntries(state, english).first()
        state = (PhraseComposerController.processSequence(
            categoryEntry.left,
            categoryEntry.right,
            state,
            english
        ) as PhraseComposerSequenceResult.Navigate).newState
        val back = PhraseComposerController.processSequence(
            GuidedModeNavigation.BACK_LEFT,
            GuidedModeNavigation.BACK_RIGHT,
            state,
            english
        ) as PhraseComposerSequenceResult.Navigate
        assertEquals(PhraseComposerMode.Keyboard, back.newState.mode)
        assertEquals("Hi", back.newState.phraseText)
    }

    @Test
    fun universalBackFromKeyboardWithTextShowsCancelConfirm() {
        val result = PhraseComposerController.processSequence(
            GuidedModeNavigation.BACK_LEFT,
            GuidedModeNavigation.BACK_RIGHT,
            keyboardState("Hi"),
            english
        ) as PhraseComposerSequenceResult.Navigate
        assertEquals(PhraseComposerMode.CancelConfirm, result.newState.mode)
    }

    @Test
    fun universalBackFromEmptyKeyboardExitsCompose() {
        val result = PhraseComposerController.processSequence(
            GuidedModeNavigation.BACK_LEFT,
            GuidedModeNavigation.BACK_RIGHT,
            PhraseComposerController.initialState(),
            english
        )
        assertTrue(result is PhraseComposerSequenceResult.ExitToPreviousPanel)
    }

    @Test
    fun universalBackWiringExistsForSettingsAndComposer() {
        val mainActivity = readSource("app/src/main/java/com/idworx/lisa/MainActivity.kt")
        assertTrue(mainActivity.contains("ModeScopedGestureAuthority.routingTarget"))
        assertTrue(mainActivity.contains("GestureRoutingTarget.SettingsPanelBack"))
    }

    @Test
    fun rc7C2ComposerRoutingAndKeyboardRemainIntact() {
        assertEquals(PhraseComposerMode.Keyboard, PhraseComposerController.initialState().mode)
        assertTrue(KeyboardLayout.allKeysReachable())
        val mainActivity = readSource("app/src/main/java/com/idworx/lisa/MainActivity.kt")
        assertTrue(mainActivity.contains("handlePhraseComposerSequence"))
        assertTrue(readSource("app/src/main/java/com/idworx/lisa/PhraseComposerUi.kt").contains("BottomAlignedEyeKeyboard"))
    }

    private fun process(state: PhraseComposerState, actionId: PhraseComposerActionId): PhraseComposerState {
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
}
