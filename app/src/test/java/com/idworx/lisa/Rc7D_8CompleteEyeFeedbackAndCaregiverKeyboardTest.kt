package com.idworx.lisa

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** RC7D.8 — universal composer eye feedback and complete caregiver keyboard. */
class Rc7D_8CompleteEyeFeedbackAndCaregiverKeyboardTest {

    private val english = LisaUiStrings.forLanguage(PreferredLanguage.English)
    private val letters = EyeKeyboardLayoutMode.Letters
    private val numbers = EyeKeyboardLayoutMode.Numbers

    private fun keyboardState(
        phrase: String = "",
        layoutMode: EyeKeyboardLayoutMode = letters,
        cursorRow: Int = 0,
        cursorCol: Int = 0,
        shiftMode: KeyboardShiftMode = KeyboardShiftMode.Lowercase
    ) = PhraseComposerState(
        mode = PhraseComposerMode.Keyboard,
        phraseText = phrase,
        keyboardLayoutMode = layoutMode,
        cursorRow = cursorRow,
        cursorCol = cursorCol,
        keyboardShiftMode = shiftMode
    )

    private fun commandAction(state: PhraseComposerState, actionId: PhraseComposerActionId) =
        PhraseComposerController.commandPanelEntries(state, english).first { it.actionId == actionId }

    private fun processCommand(state: PhraseComposerState, actionId: PhraseComposerActionId): PhraseComposerState {
        val entry = commandAction(state, actionId)
        return (PhraseComposerController.processSequence(entry.left, entry.right, state, english)
            as PhraseComposerSequenceResult.Navigate).newState
    }

    private fun touchKey(state: PhraseComposerState, row: Int, col: Int): PhraseComposerState =
        (PhraseComposerController.processTouchKey(row, col, state, english)
            as PhraseComposerSequenceResult.Navigate).newState

    @Test
    fun composerEyeStatusBarOnKeyboardScreen() {
        val ui = readSource("app/src/main/java/com/idworx/lisa/PhraseComposerUi.kt")
        assertTrue(ui.contains("KeyboardComposerLayout"))
        assertTrue(ui.contains("ComposerEyeStatusBar"))
    }

    @Test
    fun composerEyeStatusBarOnCategorySelectionScreen() {
        val ui = readSource("app/src/main/java/com/idworx/lisa/PhraseComposerUi.kt")
        assertTrue(ui.contains("NonKeyboardComposerLayout"))
        assertTrue(ui.contains("composerEyeFeedback = composerEyeFeedback"))
    }

    @Test
    fun composerEyeStatusBarOnSaveConfirmationScreen() {
        val ui = readSource("app/src/main/java/com/idworx/lisa/PhraseComposerUi.kt")
        assertTrue(ui.contains("PhraseComposerMode.SaveConfirmation"))
        assertTrue(ui.contains("ComposerEyeStatusBar"))
    }

    @Test
    fun composerEyeStatusBarOnSuccessScreen() {
        val ui = readSource("app/src/main/java/com/idworx/lisa/PhraseComposerUi.kt")
        assertTrue(ui.contains("PhraseComposerMode.Success"))
        assertTrue(ui.contains("ComposerEyeStatusBar"))
    }

    @Test
    fun categoryScreenReceivesLiveBlinkCounters() {
        val ui = readSource("app/src/main/java/com/idworx/lisa/PhraseComposerUi.kt")
        assertTrue(ui.contains("composerEyeFeedback.leftWinkCount"))
        assertTrue(ui.contains("PhraseComposerEntryHighlight.level"))
    }

    @Test
    fun categoryScreenReceivesPartialSequenceHighlight() {
        val entry = PhraseComposerController.visibleEntries(
            PhraseComposerState(mode = PhraseComposerMode.DestinationCategorySelection),
            english
        ).first()
        assertEquals(
            PhraseComposerEntryHighlight.Level.Partial,
            PhraseComposerEntryHighlight.level(entry, leftWinkCount = 1, rightWinkCount = 0)
        )
    }

    @Test
    fun saveConfirmationUsesSameEyeFeedbackSource() {
        val main = readSource("app/src/main/java/com/idworx/lisa/MainActivity.kt")
        assertTrue(main.contains("uiDiagLeftCount.value"))
        assertTrue(main.contains("ComposerEyeFeedback("))
    }

    @Test
    fun successScreenUsesSameEyeFeedbackSource() {
        val root = readSource("app/src/main/java/com/idworx/lisa/LisaAccessibilityUi.kt")
        assertTrue(root.contains("composerEyeFeedback = composerEyeFeedback"))
    }

    @Test
    fun keyboardContainsComma() {
        assertTrue(KeyboardLayout.letterPunctuationRow.contains(','))
        assertTrue(KeyboardLayout.numberRows[2].contains(','))
    }

    @Test
    fun keyboardContainsPeriod() {
        assertTrue(KeyboardLayout.letterPunctuationRow.contains('.'))
        assertTrue(KeyboardLayout.numberRows[2].contains('.'))
    }

    @Test
    fun keyboardContainsApostrophe() {
        assertTrue(KeyboardLayout.letterPunctuationRow.contains('\''))
        assertTrue(KeyboardLayout.numberRows[2].contains('\''))
    }

    @Test
    fun keyboardContainsQuestionMark() {
        assertTrue(KeyboardLayout.letterPunctuationRow.contains('?'))
    }

    @Test
    fun keyboardContainsExclamationMark() {
        assertTrue(KeyboardLayout.letterPunctuationRow.contains('!'))
    }

    @Test
    fun keyboardContainsBackspaceKey() {
        val lettersUtilityRow = KeyboardLayout.utilityRowIndex(letters)
        val numbersUtilityRow = KeyboardLayout.utilityRowIndex(numbers)
        assertEquals(KeyboardSlot.Backspace, KeyboardLayout.slotAt(letters, lettersUtilityRow, 1))
        assertEquals(KeyboardSlot.Backspace, KeyboardLayout.slotAt(numbers, numbersUtilityRow, 0))
    }

    @Test
    fun keyboardBackspaceUsesCanonicalDelete() {
        val utilityRow = KeyboardLayout.utilityRowIndex(letters)
        val afterTouch = touchKey(keyboardState("hello"), utilityRow, 1)
        assertEquals("hell", afterTouch.phraseText)
        val afterEye = processCommand(
            keyboardState("hello").withCursor(KeyboardCursor(utilityRow, 1)),
            PhraseComposerActionId.SelectKey
        )
        assertEquals(afterTouch.phraseText, afterEye.phraseText)
    }

    @Test
    fun keyboardPunctuationUsesCanonicalInsertion() {
        val punctRow = KeyboardLayout.punctuationRowIndex(letters)
        val commaCol = KeyboardLayout.letterPunctuationRow.indexOf(',')
        val viaTouch = touchKey(keyboardState("hi"), punctRow, commaCol)
        assertEquals("hi,", viaTouch.phraseText)
        val viaEye = processCommand(
            keyboardState("hi", cursorRow = punctRow, cursorCol = commaCol),
            PhraseComposerActionId.SelectKey
        )
        assertEquals(viaTouch.phraseText, viaEye.phraseText)
    }

    @Test
    fun touchAndEyeInsertionRemainIdenticalForLetters() {
        val start = keyboardState(cursorRow = 0, cursorCol = 4)
        val viaTouch = touchKey(start, 0, 4)
        val viaEye = processCommand(start, PhraseComposerActionId.SelectKey)
        assertEquals(viaEye.phraseText, viaTouch.phraseText)
    }

    @Test
    fun categorySelectionCompletesFullyByEye() {
        val categories = PhraseComposerController.visibleEntries(
            PhraseComposerState(
                mode = PhraseComposerMode.DestinationCategorySelection,
                phraseText = "headache"
            ),
            english
        )
        val medical = categories.first { it.category == CustomPhraseEngine.CaregiverPhraseCategory.Medical }
        val result = PhraseComposerController.processSequence(
            medical.left,
            medical.right,
            PhraseComposerState(
                mode = PhraseComposerMode.DestinationCategorySelection,
                phraseText = "headache"
            ),
            english
        ) as PhraseComposerSequenceResult.Navigate
        assertEquals(PhraseComposerMode.SaveConfirmation, result.newState.mode)
        assertEquals(CustomPhraseEngine.CaregiverPhraseCategory.Medical, result.newState.selectedCategory)
    }

    @Test
    fun saveConfirmationCompletesFullyByEye() {
        var state = PhraseComposerState(
            mode = PhraseComposerMode.SaveConfirmation,
            phraseText = "headache",
            selectedCategory = CustomPhraseEngine.CaregiverPhraseCategory.Medical,
            pendingAllocatedSequence = 2 to 1
        )
        val confirm = PhraseComposerController.commandPanelEntries(state, english)
            .first { it.actionId == PhraseComposerActionId.ConfirmSave }
        val result = PhraseComposerController.processSequence(
            confirm.left,
            confirm.right,
            state,
            english
        )
        assertTrue(result is PhraseComposerSequenceResult.Save)
    }

    @Test
    fun shiftProducesUppercaseThenReturnsToLowercase() {
        val shiftRow = KeyboardLayout.utilityRowIndex(letters)
        var state = touchKey(keyboardState(), shiftRow, 0)
        assertEquals(KeyboardShiftMode.OneShotUppercase, state.keyboardShiftMode)
        state = touchKey(state, 0, 4)
        assertEquals("T", state.phraseText)
        assertEquals(KeyboardShiftMode.Lowercase, state.keyboardShiftMode)
    }

    @Test
    fun everyKeyboardSlotIsReachable() {
        assertTrue(KeyboardLayout.allKeysReachable(letters))
        assertTrue(KeyboardLayout.allKeysReachable(numbers))
    }

    @Test
    fun rc7d7RegressionSuiteRemainsGreenByAudit() {
        assertTrue(ModeScopedGestureAuthorityAudit.passes())
        assertTrue(PhraseComposerCommandAudit.passes())
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
