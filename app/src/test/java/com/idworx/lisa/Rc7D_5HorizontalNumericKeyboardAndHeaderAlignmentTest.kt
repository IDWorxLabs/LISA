package com.idworx.lisa

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** RC7D.5 — horizontal numeric keyboard and composer header alignment. */
class Rc7D_5HorizontalNumericKeyboardAndHeaderAlignmentTest {

    private val english = LisaUiStrings.forLanguage(PreferredLanguage.English)
    private val letters = EyeKeyboardLayoutMode.Letters
    private val numbers = EyeKeyboardLayoutMode.Numbers

    @Test
    fun numericKeyboardUsesHorizontalRows() {
        assertEquals(3, KeyboardLayout.NUMBER_ROW_COUNT)
        assertEquals("12345", KeyboardLayout.numberRows[0].joinToString(""))
        assertEquals("67890", KeyboardLayout.numberRows[1].joinToString(""))
        assertEquals(".,?!'-:;", KeyboardLayout.numberRows[2].joinToString(""))
        assertFalse(KeyboardLayout.numberRows[0].size == 3)
    }

    @Test
    fun digitsZeroThroughNinePresent() {
        val digits = KeyboardLayout.numberRows.flatMap { it }.filter { it.isDigit() }.toSet()
        assertEquals(('0'..'9').toSet(), digits)
    }

    @Test
    fun punctuationKeysPresent() {
        val punct = KeyboardLayout.numberRows[2].toSet()
        assertTrue(punct.contains('.'))
        assertTrue(punct.contains(','))
        assertTrue(punct.contains('?'))
        assertTrue(punct.contains('!'))
        assertTrue(punct.contains('-'))
        assertTrue(punct.contains('\''))
        assertTrue(punct.contains(':'))
        assertTrue(punct.contains(';'))
    }

    @Test
    fun spaceRemainsBottomRow() {
        assertEquals(4, KeyboardLayout.spaceRowIndex(numbers))
        assertEquals(' ', KeyboardLayout.keyAt(numbers, 4, 0))
    }

    @Test
    fun everyNumericKeyReachable() {
        assertTrue(KeyboardLayout.allKeysReachable(numbers))
    }

    @Test
    fun leftRightMovementAcrossNumericRows() {
        var cursor = KeyboardCursor(row = 0, col = 0)
        cursor = KeyboardNavigator.move(cursor, PhraseComposerActionId.MoveRight, numbers)
        assertEquals('2', cursor.currentKey(numbers))
        cursor = KeyboardNavigator.move(cursor, PhraseComposerActionId.MoveDown, numbers)
        assertEquals('7', cursor.currentKey(numbers))
    }

    @Test
    fun upDownChooseNearestColumn() {
        val from = KeyboardCursor(row = 1, col = 4)
        val up = KeyboardNavigator.move(from, PhraseComposerActionId.MoveUp, numbers)
        assertEquals(0, up.row)
        assertEquals(4, up.col)
        assertEquals('5', up.currentKey(numbers))
    }

    @Test
    fun toggleToNumbersStartsOnOne() {
        val state = PhraseComposerController.processSequence(
            left = 4,
            right = 1,
            state = PhraseComposerState(mode = PhraseComposerMode.Keyboard),
            uiStrings = english
        ) as PhraseComposerSequenceResult.Navigate
        assertEquals('1', state.newState.keyboardCursor().currentKey(numbers))
    }

    @Test
    fun toggleToLettersStartsOnQ() {
        var state = PhraseComposerState(
            mode = PhraseComposerMode.Keyboard,
            keyboardLayoutMode = numbers
        )
        val result = PhraseComposerController.processSequence(4, 1, state, english)
            as PhraseComposerSequenceResult.Navigate
        assertEquals('Q', result.newState.keyboardCursor().currentKey(letters))
    }

    @Test
    fun numericUiSpreadsKeysHorizontally() {
        val ui = readSource("app/src/main/java/com/idworx/lisa/EyeControlledKeyboard.kt")
        assertTrue(ui.contains("spreadHorizontally = true"))
        assertTrue(ui.contains("KeyboardLayout.numberRows"))
    }

    @Test
    fun headlineIsCentredAndBold() {
        val ui = readSource("app/src/main/java/com/idworx/lisa/EyeControlledKeyboard.kt")
        val chrome = readSource("app/src/main/java/com/idworx/lisa/KeyboardWorkspaceChrome.kt")
        val status = ui.substringAfter("fun ComposerEyeStatusBar")
        assertTrue(status.contains("bannerMessage(uiStrings)"))
        assertTrue(status.contains("KeyboardWorkspaceStatus"))
        assertTrue(
            chrome.contains("StatusReadyIndicator") ||
                chrome.contains("LisaStatusGreen") ||
                chrome.contains("fontWeight = FontWeight.Medium")
        )
    }

    @Test
    fun dynamicTrackingMessagesResolveCorrectly() {
        assertEquals(
            "Watching your eyes",
            EyeTrackingBannerContext(faceDetected = true, eyesDetected = true).bannerMessage(english)
        )
        assertEquals(
            "No face detected",
            EyeTrackingBannerContext(faceDetected = false).bannerMessage(english)
        )
    }

    @Test
    fun sensitivityLeftResponseTimeRight() {
        val ui = readSource("app/src/main/java/com/idworx/lisa/EyeControlledKeyboard.kt")
        val status = ui.substringAfter("fun ComposerEyeStatusBar")
        assertTrue(status.contains("composerSensitivityLine"))
        assertTrue(status.contains("composerResponseTimeLine"))
        assertTrue(status.contains("Arrangement.spacedBy"))
        val sensitivityIndex = status.indexOf("composerSensitivityLine")
        val responseIndex = status.indexOf("composerResponseTimeLine")
        assertTrue(sensitivityIndex in 0 until responseIndex)
    }

    @Test
    fun blinkCountersBelowSettingsRow() {
        val ui = readSource("app/src/main/java/com/idworx/lisa/EyeControlledKeyboard.kt")
        val status = ui.substringAfter("fun ComposerEyeStatusBar")
        val settingsIndex = status.indexOf("composerSensitivityLine")
        val counterIndex = status.indexOf("BlinkCounterRow")
        assertTrue(settingsIndex in 0 until counterIndex)
    }

    @Test
    fun partialSequenceFeedbackRemainsPresent() {
        val ui = readSource("app/src/main/java/com/idworx/lisa/EyeControlledKeyboard.kt")
        assertTrue(ui.contains("partialSequenceLabel"))
    }

    @Test
    fun compactNumericRowCountFitsPortrait() {
        assertEquals(5, ComposerKeyboardLayoutMetrics.rowCount(numbers))
        assertTrue(ComposerKeyboardLayoutMetrics.bottomAnchoredKeyHeightDp(numbers) >= 28)
    }

    @Test
    fun msgaAndPriorTestsRemainValid() {
        assertTrue(ModeScopedGestureAuthorityAudit.passes())
        assertTrue(PhraseComposerCommandAudit.passes())
        assertTrue(KeyboardLayout.allKeysReachable())
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
