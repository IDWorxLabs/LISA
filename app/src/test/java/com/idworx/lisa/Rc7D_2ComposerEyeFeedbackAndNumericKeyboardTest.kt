package com.idworx.lisa

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** RC7D.2 — composer eye-tracking feedback and numeric keyboard. */
class Rc7D_2ComposerEyeFeedbackAndNumericKeyboardTest {

    private val english = LisaUiStrings.forLanguage(PreferredLanguage.English)
    private val letters = EyeKeyboardLayoutMode.Letters
    private val numbers = EyeKeyboardLayoutMode.Numbers

    private fun keyboardState(
        phrase: String = "",
        layoutMode: EyeKeyboardLayoutMode = letters,
        cursorRow: Int = 0,
        cursorCol: Int = 0
    ) = PhraseComposerState(
        mode = PhraseComposerMode.Keyboard,
        phraseText = phrase,
        keyboardLayoutMode = layoutMode,
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
    fun composeModeDisplaysDynamicEyeTrackingStatus() {
        val ui = readSource("app/src/main/java/com/idworx/lisa/PhraseComposerUi.kt")
        assertTrue(ui.contains("ComposerEyeStatusBar"))
        assertTrue(ui.contains("composerEyeFeedback"))
    }

    @Test
    fun eyesDetectedResolvesToWatchingYourEyes() {
        val banner = EyeTrackingBannerContext(faceDetected = true, eyesDetected = true)
        assertEquals("WATCHING YOUR EYES...", banner.bannerMessage(english))
    }

    @Test
    fun noFaceResolvesToNoFaceDetected() {
        assertEquals(
            "NO FACE DETECTED",
            EyeTrackingBannerContext(faceDetected = false).bannerMessage(english)
        )
    }

    @Test
    fun trackingLostResolvesCorrectly() {
        assertEquals(
            "TRACKING LOST",
            EyeTrackingBannerContext(trackingLost = true, faceDetected = true, eyesDetected = true)
                .bannerMessage(english)
        )
    }

    @Test
    fun composerEyeFeedbackUsesCanonicalBlinkCounts() {
        val main = readSource("app/src/main/java/com/idworx/lisa/MainActivity.kt")
        assertTrue(main.contains("ComposerEyeFeedback("))
        assertTrue(main.contains("uiDiagLeftCount.value"))
        assertTrue(main.contains("uiDiagRightCount.value"))
    }

    @Test
    fun partialSequenceFeedbackUpdatesLive() {
        assertEquals("L2", formatPartialWinkSequence(2, 0))
        assertEquals("L2 R1", formatPartialWinkSequence(2, 1))
    }

    @Test
    fun partialSequenceResetsWhenEmpty() {
        assertNull(formatPartialWinkSequence(0, 0))
    }

    @Test
    fun composerUiShowsBlinkCounters() {
        val ui = readSource("app/src/main/java/com/idworx/lisa/EyeControlledKeyboard.kt")
        assertTrue(ui.contains("leftDots"))
        assertTrue(ui.contains("rightDots"))
    }

    @Test
    fun lettersIsDefaultKeyboardLayout() {
        assertEquals(letters, PhraseComposerController.keyboardEntryState().keyboardLayoutMode)
    }

    @Test
    fun numericLayoutContainsDigitsZeroThroughNine() {
        val digits = KeyboardLayout.numberRows.flatMap { it }.filter { it.isDigit() }.toSet()
        assertEquals(('0'..'9').toSet(), digits)
    }

    @Test
    fun numericLayoutContainsDecimalAndComma() {
        assertTrue(KeyboardLayout.numberRows[2].contains('.'))
        assertTrue(KeyboardLayout.numberRows[2].contains(','))
    }

    @Test
    fun toggleVisibleInCommandPanel() {
        val panel = PhraseComposerController.commandPanelEntries(keyboardState(), english)
        assertTrue(panel.any { it.actionId == PhraseComposerActionId.ToggleKeyboardLayout })
    }

    @Test
    fun toggleHasVisibleBlinkSequence() {
        val entry = commandAction(keyboardState(), PhraseComposerActionId.ToggleKeyboardLayout)
        assertEquals("L4 R1", entry.sequenceLabel)
    }

    @Test
    fun toggleSwitchesFromLettersToNumbers() {
        val state = processCommand(keyboardState(), PhraseComposerActionId.ToggleKeyboardLayout)
        assertEquals(numbers, state.keyboardLayoutMode)
    }

    @Test
    fun toggleSwitchesFromNumbersToLetters() {
        var state = processCommand(keyboardState(), PhraseComposerActionId.ToggleKeyboardLayout)
        state = processCommand(state, PhraseComposerActionId.ToggleKeyboardLayout)
        assertEquals(letters, state.keyboardLayoutMode)
    }

    @Test
    fun phrasePreservedWhileSwitchingLayouts() {
        var state = keyboardState("room")
        state = processCommand(state, PhraseComposerActionId.ToggleKeyboardLayout)
        assertEquals("room", state.phraseText)
    }

    @Test
    fun cursorMovesToQWhenEnteringLetters() {
        val state = processCommand(
            keyboardState(layoutMode = numbers),
            PhraseComposerActionId.ToggleKeyboardLayout
        )
        assertEquals('Q', state.keyboardCursor().currentKey(letters))
    }

    @Test
    fun cursorMovesToOneWhenEnteringNumbers() {
        val state = processCommand(keyboardState(), PhraseComposerActionId.ToggleKeyboardLayout)
        assertEquals('1', state.keyboardCursor().currentKey(numbers))
    }

    @Test
    fun directionalNavigationWorksOnNumericLayout() {
        var state = keyboardState(layoutMode = numbers, cursorRow = 1, cursorCol = 1)
        state = processCommand(state, PhraseComposerActionId.MoveUp)
        assertEquals(0, state.cursorRow)
        assertEquals(1, state.cursorCol)
    }

    @Test
    fun selectingNumberInsertsDigit() {
        var state = keyboardState(layoutMode = numbers)
        state = processCommand(state, PhraseComposerActionId.SelectKey)
        assertEquals("1", state.phraseText)
    }

    @Test
    fun selectingPunctuationOnEmptyPhraseIsSafe() {
        var state = keyboardState(
            layoutMode = numbers,
            cursorRow = 2,
            cursorCol = 0
        )
        state = processCommand(state, PhraseComposerActionId.SelectKey)
        assertEquals("", state.phraseText)
    }

    @Test
    fun backspaceWorksOnMixedText() {
        val state = processCommand(keyboardState("room 12"), PhraseComposerActionId.Backspace)
        assertEquals("room 1", state.phraseText)
    }

    @Test
    fun saveWorksWithMixedNumericPhrase() {
        val state = processCommand(keyboardState("room 12"), PhraseComposerActionId.Save)
        assertEquals(PhraseComposerMode.DestinationCategorySelection, state.mode)
        assertEquals("room 12", state.phraseText)
    }

    @Test
    fun previewWorksWithDecimalPhrase() {
        val state = keyboardState("pain 7.5")
        val result = PhraseComposerController.processSequence(
            commandAction(state, PhraseComposerActionId.Preview).left,
            commandAction(state, PhraseComposerActionId.Preview).right,
            state,
            english
        )
        assertTrue(result is PhraseComposerSequenceResult.Preview)
        assertEquals("pain 7.5", (result as PhraseComposerSequenceResult.Preview).phrase)
    }

    @Test
    fun msgaIsolationRemainsValid() {
        assertTrue(ModeScopedGestureAuthorityAudit.passes())
        assertTrue(PhraseComposerCommandAudit.passes())
    }

    @Test
    fun backAndEmergencySequencesUnchanged() {
        assertEquals(
            GuidedModeNavigation.BACK_LEFT to GuidedModeNavigation.BACK_RIGHT,
            PhraseComposerCommandSequences.sequenceFor(PhraseComposerActionId.Back)
        )
        assertEquals(
            EMERGENCY_LEFT_WINKS to EMERGENCY_RIGHT_WINKS,
            PhraseComposerCommandSequences.emergencySequence
        )
    }

    @Test
    fun keyboardLayoutsAreCompactAndComplete() {
        assertTrue(KeyboardLayout.allKeysReachable())
        assertEquals(4, KeyboardLayout.totalRowCount(letters))
        assertEquals(4, KeyboardLayout.totalRowCount(numbers))
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
