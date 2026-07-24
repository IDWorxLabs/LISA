package com.idworx.lisa

import com.idworx.lisa.features.zerotouchprinciple.audit.ZeroTouchFileProbe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * RC8.3 — Feedback keyboard uses the Custom Phrases Emergency bar and a single bottom Done control.
 * Visual/layout only; sequences and emergency behaviour stay unchanged.
 */
class Rc8_3FeedbackKeyboardEmergencyBarAlignmentTest {

    private fun read(relativeUnderMainJava: String): String {
        val path = "app/src/main/java/com/idworx/lisa/$relativeUnderMainJava"
        return ZeroTouchFileProbe.readProjectFile(path)
            ?: error("Missing source file: $path")
    }

    @Test
    fun feedbackUsesSharedEmergencyActionBarNotASecondStyle() {
        val workspace = read("MenuDestinationWorkspaceUi.kt")
        val grid = read("ComposerCommandGrid.kt")
        assertTrue(grid.contains("fun EmergencyActionBar"))
        assertTrue(grid.contains("fun ComposerEmergencyCommandCard"))
        val editorStart = workspace.indexOf("fun MenuDestinationTextEditor")
        val editorEnd = workspace.indexOf("fun FeedbackKeyboardDirectionLegend", editorStart)
        val editor = workspace.substring(editorStart, editorEnd)
        assertTrue(editor.contains("EmergencyActionBar("))
        assertFalse(editor.contains("GuidedEmergencyNavButton("))
        // Keyboard editing column must not reinstate the four-button bottom row.
        assertFalse(workspace.contains("fun KeyboardFocusedCommandBar"))
        assertEquals(2, Regex("EmergencyActionBar\\(").findAll(editor).count())
    }

    @Test
    fun feedbackKeyboardVerticalOrderPlacesEmergencyAboveKeyboardAndDoneBelow() {
        val workspace = read("MenuDestinationWorkspaceUi.kt")
        val editorStart = workspace.indexOf("fun MenuDestinationTextEditor")
        assertTrue(editorStart >= 0)
        val editorEnd = workspace.indexOf("fun FeedbackKeyboardDirectionLegend", editorStart)
        val editor = workspace.substring(editorStart, editorEnd)
        val emergencyIdx = editor.indexOf("EmergencyActionBar(")
        val keyboardIdx = editor.indexOf("BottomAlignedEyeKeyboard(")
        val bottomIdx = editor.indexOf("KeyboardWorkspaceBottomActionRow(")
        assertTrue(emergencyIdx >= 0)
        assertTrue(keyboardIdx > emergencyIdx)
        assertTrue(bottomIdx > keyboardIdx)
    }

    @Test
    fun keyboardBottomControlIsOnlyDoneWithL3R2() {
        val workspace = read("MenuDestinationWorkspaceUi.kt")
        assertTrue(workspace.contains("KeyboardWorkspaceBottomActionRow"))
        assertEquals(
            3 to 2,
            FeedbackKeyboardNavigationAuthority.sequence(MenuDestinationPanelCommand.DoneEditing)
        )
        assertEquals(
            2 to 2,
            FeedbackKeyboardNavigationAuthority.sequence(MenuDestinationPanelCommand.Back)
        )
        assertEquals(
            6 to 0,
            FeedbackKeyboardNavigationAuthority.sequence(MenuDestinationPanelCommand.Emergency)
        )
        assertTrue(workspace.contains("\"Done\""))
        val reviewRow = workspace.substring(workspace.indexOf("fun FeedbackReviewCommandRow"))
        assertFalse(reviewRow.contains("MenuDestinationPanelCommand.Emergency"))
        assertFalse(reviewRow.contains("MenuDestinationPanelCommand.Select"))
        assertFalse(reviewRow.contains("MenuDestinationPanelCommand.DoneEditing"))
        assertFalse(reviewRow.contains("MenuDestinationPanelCommand.Back"))
    }

    @Test
    fun sharedBottomDoneAndEmergencyReuseDesignSystem() {
        val chrome = read("KeyboardWorkspaceChrome.kt")
        assertTrue(chrome.contains("fun KeyboardWorkspaceBottomActionRow"))
        assertTrue(chrome.contains("fun KeyboardWorkspaceBottomDoneButton"))
        assertTrue(chrome.contains("KeyboardWorkspaceClickableActionCard"))
        assertTrue(chrome.contains("SharedKeyboardTheme.ActionMinHeight"))
        val grid = read("ComposerCommandGrid.kt")
        assertTrue(grid.contains("LisaEmergencyRed.copy(alpha = 0.15f)"))
        assertTrue(grid.contains("\"\\uD83D\\uDEA8\"") || grid.contains("\"🚨\""))
    }

    @Test
    fun emergencySequenceUnchanged() {
        assertEquals(
            EMERGENCY_LEFT_WINKS to EMERGENCY_RIGHT_WINKS,
            PhraseComposerCommandSequences.emergencySequence
        )
        assertEquals(6, EMERGENCY_LEFT_WINKS)
        assertEquals(0, EMERGENCY_RIGHT_WINKS)
    }
}
