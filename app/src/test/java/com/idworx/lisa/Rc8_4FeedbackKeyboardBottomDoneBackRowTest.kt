package com.idworx.lisa

import com.idworx.lisa.features.zerotouchprinciple.audit.ZeroTouchFileProbe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * RC8.4 — Feedback keyboard bottom row is exactly Done | Back (equal shared chrome).
 * Emergency stays on the shared bar; sequences L3 R2 / L2 R2 / L6 R0 unchanged.
 */
class Rc8_4FeedbackKeyboardBottomDoneBackRowTest {

    private val english = LisaUiStrings.forLanguage(PreferredLanguage.English)

    private fun read(relativeUnderMainJava: String): String {
        val path = "app/src/main/java/com/idworx/lisa/$relativeUnderMainJava"
        return ZeroTouchFileProbe.readProjectFile(path)
            ?: error("Missing source file: $path")
    }

    private fun editorSource(): String {
        val workspace = read("MenuDestinationWorkspaceUi.kt")
        val start = workspace.indexOf("fun MenuDestinationTextEditor")
        val end = workspace.indexOf("fun FeedbackKeyboardDirectionLegend", start)
        return workspace.substring(start, end)
    }

    @Test
    fun bottomRowUsesSharedTwoActionComponentWithDoneLeftBackRight() {
        val chrome = read("KeyboardWorkspaceChrome.kt")
        assertTrue(chrome.contains("fun KeyboardWorkspaceBottomActionRow"))
        assertTrue(chrome.contains("data class KeyboardWorkspaceBottomAction"))
        val editor = editorSource()
        assertTrue(editor.contains("KeyboardWorkspaceBottomActionRow("))
        val rowStart = editor.indexOf("KeyboardWorkspaceBottomActionRow(")
        val row = editor.substring(rowStart)
        val doneIdx = row.indexOf("MenuDestinationPanelCommand.DoneEditing")
        val backIdx = row.indexOf("MenuDestinationPanelCommand.Back")
        assertTrue(doneIdx >= 0)
        assertTrue(backIdx > doneIdx)
        assertFalse(row.contains("MenuDestinationPanelCommand.Select"))
        assertFalse(row.contains("MenuDestinationPanelCommand.Emergency"))
        assertEquals(
            2,
            Regex("KeyboardWorkspaceBottomAction\\(").findAll(row).count()
        )
    }

    @Test
    fun doneAndBackDisplayCanonicalSequences() {
        assertEquals(
            3 to 2,
            FeedbackKeyboardNavigationAuthority.sequence(MenuDestinationPanelCommand.DoneEditing)
        )
        assertEquals(
            2 to 2,
            FeedbackKeyboardNavigationAuthority.sequence(MenuDestinationPanelCommand.Back)
        )
        assertEquals(
            formatWinkSequenceShort(3, 2),
            formatWinkSequenceShort(
                FeedbackKeyboardNavigationAuthority.sequence(
                    MenuDestinationPanelCommand.DoneEditing
                )!!.first,
                FeedbackKeyboardNavigationAuthority.sequence(
                    MenuDestinationPanelCommand.DoneEditing
                )!!.second
            )
        )
        assertEquals(
            formatWinkSequenceShort(
                GuidedModeNavigation.BACK_LEFT,
                GuidedModeNavigation.BACK_RIGHT
            ),
            formatWinkSequenceShort(2, 2)
        )
        assertEquals("Back", english.back)
    }

    @Test
    fun verticalOrderKeepsEmergencyAboveKeyboardAndBottomRowBelow() {
        val editor = editorSource()
        val emergencyIdx = editor.indexOf("EmergencyActionBar(")
        val keyboardIdx = editor.indexOf("BottomAlignedEyeKeyboard(")
        val bottomIdx = editor.indexOf("KeyboardWorkspaceBottomActionRow(")
        assertTrue(emergencyIdx >= 0)
        assertTrue(keyboardIdx > emergencyIdx)
        assertTrue(bottomIdx > keyboardIdx)
    }

    @Test
    fun emergencyRemainsOnlyOnSharedBarNotBottomRow() {
        val editor = editorSource()
        assertTrue(editor.contains("EmergencyActionBar("))
        val row = editor.substring(editor.indexOf("KeyboardWorkspaceBottomActionRow("))
        assertFalse(row.contains("Emergency"))
        assertFalse(row.contains("MenuDestinationPanelCommand.Select"))
        assertFalse(editor.contains("fun KeyboardFocusedCommandBar"))
    }

    @Test
    fun blinkBackCancelsKeyboardEditingWithoutSubmitting() {
        val editing = MenuDestinationNavigationController.beginTextEditing(
            state = MenuDestinationNavigationState(
                destination = MainMenuDestination.Feedback,
                panel = LisaPanel.Feedback,
                isActive = true
            ),
            actionId = MenuDestinationActionId.FeedbackWorkedWell,
            currentText = "draft kept policy",
            requiresReview = true
        )
        val withDraft = MenuDestinationNavigationController.updateTextDraft(editing, "unsaved")
        val cancelled = MenuDestinationNavigationController.cancelCurrentStage(withDraft)
        assertEquals(MenuDestinationInteractionStage.Browsing, cancelled.interactionStage)
        assertFalse(
            cancelled.interactionStage is MenuDestinationInteractionStage.TextEditing
        )
    }

    @Test
    fun blinkDoneFinishesKeyboardIntoReviewWithoutChangingSequences() {
        val editing = MenuDestinationNavigationController.beginTextEditing(
            state = MenuDestinationNavigationState(
                destination = MainMenuDestination.Feedback,
                panel = LisaPanel.Feedback,
                isActive = true
            ),
            actionId = MenuDestinationActionId.FeedbackWorkedWell,
            currentText = "",
            requiresReview = true
        )
        val drafted = MenuDestinationNavigationController.updateTextDraft(editing, "ready")
        val finished = MenuDestinationNavigationController.finishKeyboardEditing(drafted)
        val stage = finished.interactionStage as MenuDestinationInteractionStage.TextEditing
        assertEquals(FeedbackFieldEditingStage.Review, stage.fieldEditingStage)
        assertEquals("ready", stage.draftText)
        assertEquals(3 to 2, FeedbackKeyboardNavigationAuthority.sequence(
            MenuDestinationPanelCommand.DoneEditing
        ))
        assertEquals(2 to 2, FeedbackKeyboardNavigationAuthority.sequence(
            MenuDestinationPanelCommand.Back
        ))
        assertEquals(6 to 0, FeedbackKeyboardNavigationAuthority.sequence(
            MenuDestinationPanelCommand.Emergency
        ))
    }

    @Test
    fun touchBackCommandMatchesBlinkBackAuthorityPath() {
        // Touch routes MenuDestinationPanelCommand.Back; blink uses isBackSequence → same cancel.
        assertTrue(GuidedModeNavigation.isBackSequence(2, 2))
        assertEquals(
            2 to 2,
            FeedbackKeyboardNavigationAuthority.sequence(MenuDestinationPanelCommand.Back)
        )
        val main = read("MainActivity.kt")
        assertTrue(main.contains("MenuDestinationPanelCommand.Back"))
        assertTrue(main.contains("GuidedModeNavigation.isBackSequence(left, right)"))
        assertTrue(main.contains("backFromMenuDestination()"))
    }
}
