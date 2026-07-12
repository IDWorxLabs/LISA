package com.idworx.lisa

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** RC7D.4 — bottom-aligned keyboard and expanded command grid layout. */
class Rc7D_4BottomAlignedKeyboardLayoutTest {

    private val english = LisaUiStrings.forLanguage(PreferredLanguage.English)
    private val letters = EyeKeyboardLayoutMode.Letters
    private val numbers = EyeKeyboardLayoutMode.Numbers

    @Test
    fun eyeBlinkStatusRemainsAtTop() {
        val ui = readSource("app/src/main/java/com/idworx/lisa/PhraseComposerUi.kt")
        val keyboardLayout = ui.substringAfter("private fun KeyboardComposerLayout")
        assertTrue(keyboardLayout.indexOf("ComposerEyeStatusBar") < keyboardLayout.indexOf("ComposerPhraseField"))
    }

    @Test
    fun currentPhraseBelowStatusArea() {
        val ui = readSource("app/src/main/java/com/idworx/lisa/PhraseComposerUi.kt")
        val keyboardLayout = ui.substringAfter("private fun KeyboardComposerLayout")
        assertTrue(keyboardLayout.indexOf("ComposerPhraseField") < keyboardLayout.indexOf("ComposerCommandGrid"))
    }

    @Test
    fun oldRightSideCommandColumnRemoved() {
        val ui = readSource("app/src/main/java/com/idworx/lisa/PhraseComposerUi.kt")
        assertFalse(ui.contains("PhraseComposerCommandPanel"))
        assertFalse(ui.contains(".width(118.dp)"))
        assertFalse(ui.contains("Row(") && ui.contains("PhraseComposerCommandPanel"))
    }

    @Test
    fun commandsRenderedAboveKeyboard() {
        val ui = readSource("app/src/main/java/com/idworx/lisa/PhraseComposerUi.kt")
        val keyboardLayout = ui.substringAfter("private fun KeyboardComposerLayout")
        assertTrue(keyboardLayout.indexOf("ComposerCommandGrid") < keyboardLayout.indexOf("BottomAlignedEyeKeyboard"))
    }

    @Test
    fun commandGridUsesMultiRowLayout() {
        val wideRows = ComposerCommandGridLayout.commandRows(400)
        assertEquals(2, wideRows.size)
        assertEquals(5, wideRows[0].size)
        assertEquals(5, wideRows[1].size)
    }

    @Test
    fun everyCommandRetainsExistingSequence() {
        assertEquals(2 to 0, PhraseComposerCommandSequences.sequenceFor(PhraseComposerActionId.MoveUp))
        assertEquals(0 to 2, PhraseComposerCommandSequences.sequenceFor(PhraseComposerActionId.MoveDown))
        assertEquals(2 to 1, PhraseComposerCommandSequences.sequenceFor(PhraseComposerActionId.MoveLeft))
        assertEquals(1 to 2, PhraseComposerCommandSequences.sequenceFor(PhraseComposerActionId.MoveRight))
        assertEquals(1 to 1, PhraseComposerCommandSequences.sequenceFor(PhraseComposerActionId.SelectKey))
        assertEquals(3 to 1, PhraseComposerCommandSequences.sequenceFor(PhraseComposerActionId.Backspace))
        assertEquals(1 to 3, PhraseComposerCommandSequences.sequenceFor(PhraseComposerActionId.Preview))
        assertEquals(3 to 2, PhraseComposerCommandSequences.sequenceFor(PhraseComposerActionId.Save))
        assertEquals("L4 R1", commandEntry(PhraseComposerActionId.ToggleKeyboardLayout).sequenceLabel)
        assertEquals(2 to 2, PhraseComposerCommandSequences.sequenceFor(PhraseComposerActionId.Back))
        assertEquals(
            EMERGENCY_LEFT_WINKS to EMERGENCY_RIGHT_WINKS,
            PhraseComposerCommandSequences.emergencySequence
        )
    }

    @Test
    fun emergencyRemainsRedAndVisible() {
        val grid = readSource("app/src/main/java/com/idworx/lisa/ComposerCommandGrid.kt")
        assertTrue(grid.contains("ComposerEmergencyCommandCard"))
        assertTrue(grid.contains("LisaEmergencyRed"))
    }

    @Test
    fun emergencyRemainsGloballyFunctional() {
        val root = readSource("app/src/main/java/com/idworx/lisa/LisaAccessibilityUi.kt")
        assertTrue(root.contains("GlobalEmergencyOverlayLayer"))
    }

    @Test
    fun keyboardRenderedAfterCommandArea() {
        val ui = readSource("app/src/main/java/com/idworx/lisa/PhraseComposerUi.kt")
        assertTrue(ui.contains("BottomAlignedEyeKeyboard"))
    }

    @Test
    fun keyboardIsBottomAligned() {
        val ui = readSource("app/src/main/java/com/idworx/lisa/PhraseComposerUi.kt")
        val layout = ui.substringAfter("private fun KeyboardComposerLayout")
        assertTrue(layout.indexOf("ComposerCommandGrid") < layout.indexOf("BottomAlignedEyeKeyboard"))
        assertTrue(ui.contains("BottomAlignedEyeKeyboard("))
        assertFalse(ui.contains("EyeControlledKeyboard("))
    }

    @Test
    fun noLargeFixedSpacerBelowKeyboard() {
        val ui = readSource("app/src/main/java/com/idworx/lisa/PhraseComposerUi.kt")
        assertFalse(ui.contains("Spacer(Modifier.height(80.dp))"))
        assertFalse(ui.contains("Spacer(Modifier.height(100.dp))"))
    }

    @Test
    fun letterKeyboardRetainsQwertyOrder() {
        assertEquals("QWERTYUIOP", KeyboardLayout.letterRows[0].joinToString(""))
        assertEquals("ASDFGHJKL", KeyboardLayout.letterRows[1].joinToString(""))
        assertEquals("ZXCVBNM", KeyboardLayout.letterRows[2].joinToString(""))
    }

    @Test
    fun numericKeyboardRetainsHorizontalOrder() {
        assertEquals("12345", KeyboardLayout.numberRows[0].joinToString(""))
        assertEquals("67890", KeyboardLayout.numberRows[1].joinToString(""))
        assertEquals(".,?!-", KeyboardLayout.numberRows[2].joinToString(""))
    }

    @Test
    fun spaceRemainsBottomKeyboardRow() {
        assertEquals(
            KeyboardLayout.totalRowCount(letters) - 1,
            KeyboardLayout.spaceRowIndex(letters)
        )
        assertEquals(
            KeyboardLayout.totalRowCount(numbers) - 1,
            KeyboardLayout.spaceRowIndex(numbers)
        )
    }

    @Test
    fun selectedKeyHighlightRemainsStrong() {
        val keyboard = readSource("app/src/main/java/com/idworx/lisa/EyeControlledKeyboard.kt")
        assertTrue(keyboard.contains("KeyHighlightFill"))
        assertTrue(keyboard.contains("border"))
    }

    @Test
    fun layoutFitsCompactPortraitWithoutScrolling() {
        val commandRows = ComposerCommandGridLayout.commandRows(320).flatten().size
        assertTrue(commandRows >= 10)
        val letterKeyHeight = ComposerKeyboardLayoutMetrics.bottomAnchoredKeyHeightDp(letters)
        val numericKeyHeight = ComposerKeyboardLayoutMetrics.bottomAnchoredKeyHeightDp(numbers)
        assertTrue(letterKeyHeight <= ComposerKeyboardLayoutMetrics.MAX_KEY_HEIGHT_DP)
        assertTrue(numericKeyHeight <= ComposerKeyboardLayoutMetrics.MAX_KEY_HEIGHT_DP)
    }

    @Test
    fun commandLabelsRemainReadable() {
        val grid = readSource("app/src/main/java/com/idworx/lisa/ComposerCommandGrid.kt")
        assertTrue(grid.contains("fontSize = 11.sp"))
        assertTrue(grid.contains("defaultMinSize(minHeight = 76.dp)"))
    }

    @Test
    fun blinkCountersAndPartialSequenceUnchanged() {
        val ui = readSource("app/src/main/java/com/idworx/lisa/EyeControlledKeyboard.kt")
        assertTrue(ui.contains("leftDots"))
        assertTrue(ui.contains("partialSequenceLabel"))
        assertTrue(ui.contains("ComposerEyeStatusBar"))
    }

    @Test
    fun narrowScreenUsesAdaptiveGrid() {
        val narrowRows = ComposerCommandGridLayout.commandRows(320)
        assertEquals(3, narrowRows.size)
        assertTrue(narrowRows.flatten().contains(PhraseComposerActionId.ToggleKeyboardLayout))
    }

    @Test
    fun msgaAndPriorRegressionTestsRemainValid() {
        assertTrue(ModeScopedGestureAuthorityAudit.passes())
        assertTrue(PhraseComposerCommandAudit.passes())
    }

    private fun commandEntry(actionId: PhraseComposerActionId): PhraseComposerEntry =
        PhraseComposerController.commandPanelEntries(
            PhraseComposerState(mode = PhraseComposerMode.Keyboard),
            english
        ).first { it.actionId == actionId }

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
