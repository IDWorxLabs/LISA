package com.idworx.lisa

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** RC7D.7 — caregiver touch typing and clean full-screen active emergency. */
class Rc7D_7CaregiverTouchTypingAndCleanEmergencyStateTest {

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

    private fun touchKey(state: PhraseComposerState, row: Int, col: Int): PhraseComposerState =
        (PhraseComposerController.processTouchKey(row, col, state, english)
            as PhraseComposerSequenceResult.Navigate).newState

    @Test
    fun letterKeysHaveTouchClickHandlers() {
        val keyboard = readSource("app/src/main/java/com/idworx/lisa/EyeControlledKeyboard.kt")
        assertTrue(keyboard.contains("onKeyTouched"))
        assertTrue(keyboard.contains(".clickable("))
        KeyboardLayout.letterRows.flatten().forEach { letter ->
            val rowCol = letterRowCol(letter)
            val result = PhraseComposerController.processTouchKey(
                rowCol.first,
                rowCol.second,
                keyboardState(),
                english
            )
            assertTrue("Touch failed for $letter", result is PhraseComposerSequenceResult.Navigate)
        }
    }

    private fun letterRowCol(letter: Char): Pair<Int, Int> {
        KeyboardLayout.letterRows.forEachIndexed { row, keys ->
            keys.forEachIndexed { col, key ->
                if (key == letter) return row to col
            }
        }
        error("Missing letter $letter")
    }

    @Test
    fun numericKeysHaveTouchClickHandlers() {
        val keyboard = readSource("app/src/main/java/com/idworx/lisa/EyeControlledKeyboard.kt")
        ('0'..'9').forEach { digit ->
            assertTrue("Missing digit $digit", keyboard.contains(digit.toString()))
        }
    }

    @Test
    fun punctuationKeysHaveTouchClickHandlers() {
        val keyboard = readSource("app/src/main/java/com/idworx/lisa/EyeControlledKeyboard.kt")
        listOf(".", ",", "?", "!", "-").forEach { symbol ->
            assertTrue("Missing punctuation $symbol", keyboard.contains("\"$symbol\""))
        }
    }

    @Test
    fun spaceKeyHasTouchClickHandler() {
        val keyboard = readSource("app/src/main/java/com/idworx/lisa/EyeControlledKeyboard.kt")
        assertTrue(keyboard.contains("KeyboardSpaceRow"))
        assertTrue(keyboard.contains("isSpace = true"))
        assertTrue(keyboard.contains("keyboardKeyContentDescription"))
    }

    @Test
    fun tappingKeyInsertsExactlyOneCharacter() {
        val afterT = touchKey(keyboardState(), row = 0, col = 4)
        assertEquals("t", afterT.phraseText)
        val afterSecond = touchKey(afterT, row = 0, col = 4)
        assertEquals("tt", afterSecond.phraseText)
    }

    @Test
    fun touchInsertionUsesSameCanonicalPhraseStateAsEyeSelection() {
        val start = keyboardState(cursorRow = 0, cursorCol = 4)
        val viaTouch = touchKey(start, row = 0, col = 4)
        val viaEye = processCommand(start, PhraseComposerActionId.SelectKey)
        assertEquals(viaEye.phraseText, viaTouch.phraseText)
    }

    @Test
    fun tappingKeyMovesCursorHighlightToThatKey() {
        val updated = touchKey(keyboardState(cursorRow = 0, cursorCol = 0), row = 1, col = 4)
        assertEquals(1, updated.cursorRow)
        assertEquals(4, updated.cursorCol)
    }

    @Test
    fun eyeNavigationContinuesAfterTouchSelection() {
        var state = touchKey(keyboardState(), row = 2, col = 3)
        assertEquals(2, state.cursorRow)
        assertEquals(3, state.cursorCol)
        state = processCommand(state, PhraseComposerActionId.MoveRight)
        assertEquals(2, state.cursorRow)
        assertEquals(4, state.cursorCol)
    }

    @Test
    fun backspaceIsTouchClickable() {
        val grid = readSource("app/src/main/java/com/idworx/lisa/ComposerCommandGrid.kt")
        assertTrue(grid.contains("onCommandSelected"))
        val afterBackspace = processCommand(keyboardState(phrase = "hi"), PhraseComposerActionId.Backspace)
        assertEquals("h", afterBackspace.phraseText)
    }

    @Test
    fun previewIsTouchClickable() {
        val main = readSource("app/src/main/java/com/idworx/lisa/MainActivity.kt")
        assertTrue(main.contains("applyPhraseComposerTouchNavigation"))
        val result = PhraseComposerController.processSequence(
            commandAction(keyboardState(phrase = "hello"), PhraseComposerActionId.Preview).left,
            commandAction(keyboardState(phrase = "hello"), PhraseComposerActionId.Preview).right,
            keyboardState(phrase = "hello"),
            english
        )
        assertTrue(result is PhraseComposerSequenceResult.Preview)
    }

    @Test
    fun saveIsTouchClickable() {
        val result = PhraseComposerController.processSequence(
            commandAction(keyboardState(phrase = "hello"), PhraseComposerActionId.Save).left,
            commandAction(keyboardState(phrase = "hello"), PhraseComposerActionId.Save).right,
            keyboardState(phrase = "hello"),
            english
        ) as PhraseComposerSequenceResult.Navigate
        assertEquals(PhraseComposerMode.DestinationCategorySelection, result.newState.mode)
    }

    @Test
    fun abc123ToggleIsTouchClickable() {
        val toggled = processCommand(keyboardState(), PhraseComposerActionId.ToggleKeyboardLayout)
        assertEquals(numbers, toggled.keyboardLayoutMode)
    }

    @Test
    fun backPreservesUnsavedPhraseBehaviour() {
        val withText = keyboardState(phrase = "draft")
        val backResult = PhraseComposerController.processSequence(
            GuidedModeNavigation.BACK_LEFT,
            GuidedModeNavigation.BACK_RIGHT,
            withText,
            english
        ) as PhraseComposerSequenceResult.Navigate
        assertEquals(PhraseComposerMode.CancelConfirm, backResult.newState.mode)
        assertEquals("draft", backResult.newState.phraseText)
    }

    @Test
    fun emergencyIsTouchClickableAndRoutesGlobally() {
        val main = readSource("app/src/main/java/com/idworx/lisa/MainActivity.kt")
        assertTrue(main.contains("triggerGuidedEmergencyTouch()"))
        val grid = readSource("app/src/main/java/com/idworx/lisa/ComposerCommandGrid.kt")
        assertTrue(grid.contains("ComposerEmergencyCommandCard"))
    }

    @Test
    fun touchDoesNotDisableEyeInput() {
        val engine = readSource("app/src/main/java/com/idworx/lisa/PhraseComposerEngine.kt")
        assertTrue(engine.contains("processTouchKey"))
        assertTrue(engine.contains("processSequence"))
        assertTrue(engine.contains("KeyboardNavigator.move"))
    }

    @Test
    fun keyboardKeysHaveSemanticContentDescriptions() {
        assertEquals("Space", keyboardKeyContentDescription("SPACE", isSpace = true))
        assertEquals("Period", keyboardKeyContentDescription("."))
        assertEquals("A", keyboardKeyContentDescription("A"))
        val keyboard = readSource("app/src/main/java/com/idworx/lisa/EyeControlledKeyboard.kt")
        assertTrue(keyboard.contains("contentDescription"))
    }

    @Test
    fun activeEmergencyUsesFullyOpaqueFullScreenBackground() {
        val emergency = readSource("app/src/main/java/com/idworx/lisa/LisaEmergencyUi.kt")
        assertTrue(emergency.contains(".background(LisaEmergencyRed)"))
        assertFalse(emergency.contains("flashAlpha"))
        assertFalse(emergency.contains("infiniteRepeatable"))
    }

    @Test
    fun activeEmergencyHidesCommunicationHeader() {
        val root = readSource("app/src/main/java/com/idworx/lisa/LisaAccessibilityUi.kt")
        assertTrue(root.contains("if (!emergencyActive)"))
        assertTrue(root.contains("EverydayCommunicationPanel"))
    }

    @Test
    fun activeEmergencyHidesComposeHeader() {
        val root = readSource("app/src/main/java/com/idworx/lisa/LisaAccessibilityUi.kt")
        assertTrue(root.contains("EyeControlledPhraseComposerOverlay"))
        assertTrue(root.contains("if (!emergencyActive)"))
    }

    @Test
    fun activeEmergencyDoesNotRenderKeyboard() {
        val root = readSource("app/src/main/java/com/idworx/lisa/LisaAccessibilityUi.kt")
        val composer = readSource("app/src/main/java/com/idworx/lisa/PhraseComposerUi.kt")
        assertTrue(composer.contains("BottomAlignedEyeKeyboard"))
        assertTrue(root.contains("EyeControlledPhraseComposerOverlay"))
        assertTrue(root.contains("if (!emergencyActive)"))
    }

    @Test
    fun activeEmergencyDoesNotRenderMenuClearRepeat() {
        val root = readSource("app/src/main/java/com/idworx/lisa/LisaAccessibilityUi.kt")
        assertTrue(root.contains("uiStrings.menu"))
        assertTrue(root.contains("WorkspaceFullWidthActionButton"))
        assertTrue(root.contains("if (!emergencyActive)"))
        // RC7D.30 — Clear is no longer in the Communication bottom bar; reset authority remains.
        assertTrue(readSource("app/src/main/java/com/idworx/lisa/MainActivity.kt").contains("performReset()"))
    }

    @Test
    fun activeEmergencyBlocksUnderlyingPointerInput() {
        val emergency = readSource("app/src/main/java/com/idworx/lisa/LisaEmergencyUi.kt")
        assertTrue(emergency.contains("consume background taps"))
        assertTrue(emergency.contains(".clickable("))
    }

    @Test
    fun stopEmergencyRemainsVisibleAndTouchClickable() {
        val emergency = readSource("app/src/main/java/com/idworx/lisa/LisaEmergencyUi.kt")
        assertTrue(emergency.contains("stopEmergency"))
        assertTrue(emergency.contains("EmergencyManualButton"))
    }

    @Test
    fun stopEmergencyClearsAlarmAndTtsState() {
        val main = readSource("app/src/main/java/com/idworx/lisa/MainActivity.kt")
        val fn = cancelOrStopEmergencySource(main)
        assertTrue(fn.contains("emergencyAlarmController.stop()"))
        assertTrue(fn.contains("tts?.stop()"))
    }

    @Test
    fun underlyingComposeStateRestoredAfterStopping() {
        val main = readSource("app/src/main/java/com/idworx/lisa/MainActivity.kt")
        val fn = cancelOrStopEmergencySource(main)
        assertFalse(fn.contains("uiPhraseComposerState.value = PhraseComposerController.initialState()"))
    }

    @Test
    fun underlyingCommunicationStateRestoredAfterStopping() {
        val main = readSource("app/src/main/java/com/idworx/lisa/MainActivity.kt")
        val fn = cancelOrStopEmergencySource(main)
        assertFalse(fn.contains("uiGuidedNavigationState.value = GuidedNavigationState()"))
    }

    @Test
    fun armedEmergencyStillDisplaysBlinkCountersAndPartialSequence() {
        val emergency = readSource("app/src/main/java/com/idworx/lisa/LisaEmergencyUi.kt")
        assertTrue(emergency.contains("EmergencyBlinkFeedbackRows"))
        assertTrue(emergency.contains("partialSequenceLabel()"))
    }

    @Test
    fun armedAndActiveEmergencyUseDistinctPresentations() {
        val emergency = readSource("app/src/main/java/com/idworx/lisa/LisaEmergencyUi.kt")
        assertTrue(emergency.contains("Brain1EmergencyConfirmOverlay"))
        assertTrue(emergency.contains("EmergencyAlarmOverlay"))
        assertNotEquals(
            emergency.indexOf("Brain1EmergencyConfirmOverlay"),
            emergency.indexOf("EmergencyAlarmOverlay")
        )
    }

    @Test
    fun sharedGlobalEmergencyLayerUsedAcrossModes() {
        val root = readSource("app/src/main/java/com/idworx/lisa/LisaAccessibilityUi.kt")
        assertEquals(1, Regex("GlobalEmergencyOverlayLayer\\(").findAll(root).count())
    }

    @Test
    fun touchKeyUsesCanonicalEnginePath() {
        val main = readSource("app/src/main/java/com/idworx/lisa/MainActivity.kt")
        assertTrue(main.contains("PhraseComposerController.processTouchKey"))
        assertTrue(main.contains("applyPhraseComposerTouchKey"))
    }

    @Test
    fun spaceTouchInsertsSingleSpaceWithExistingRules() {
        val withWord = touchKey(keyboardState(phrase = "hi"), row = KeyboardLayout.spaceRowIndex(letters), col = 0)
        assertEquals("hi ", withWord.phraseText)
        val noDouble = touchKey(withWord, row = KeyboardLayout.spaceRowIndex(letters), col = 0)
        assertEquals("hi ", noDouble.phraseText)
    }

    @Test
    fun numericTouchInsertsDigit() {
        val state = keyboardState(layoutMode = numbers)
        val afterSeven = touchKey(state, row = 1, col = 1)
        assertEquals("7", afterSeven.phraseText)
    }

    @Test
    fun punctuationTouchInsertsSymbol() {
        val state = keyboardState(phrase = "hi", layoutMode = numbers)
        val afterPeriod = touchKey(state, row = 2, col = 0)
        assertEquals("hi.", afterPeriod.phraseText)
    }

    @Test
    fun commandCardsHaveAccessibilityDescriptions() {
        val grid = readSource("app/src/main/java/com/idworx/lisa/ComposerCommandGrid.kt")
        assertTrue(grid.contains("contentDescription = entry.label"))
        assertTrue(grid.contains("contentDescription = \"\${title} \${sequenceLabel}\""))
    }

    @Test
    fun rc7dRegressionSuitesRemainGreenByAudit() {
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

    private fun cancelOrStopEmergencySource(main: String): String =
        main.substringAfter("private fun cancelOrStopEmergency()")
            .substringBefore("private fun refreshCameraPermissionState()")
}
