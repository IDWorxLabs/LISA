package com.idworx.lisa

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** RC7C3.1 — keyboard navigation command sequences (MSGA-backed). */
class Rc7C3_1PhraseComposerCommandSequenceOptimizationTest {

    private val english = LisaUiStrings.forLanguage(PreferredLanguage.English)

    private fun keyboardState() = PhraseComposerState(
        mode = PhraseComposerMode.Keyboard,
        selectedCategory = CustomPhraseEngine.CaregiverPhraseCategory.Conversation
    )

    private fun commandAction(actionId: PhraseComposerActionId): PhraseComposerEntry =
        PhraseComposerController.commandPanelEntries(keyboardState(), english)
            .first { it.actionId == actionId }

    @Test
    fun composerCommandAuditPasses() {
        assertTrue(
            "Composer command audit findings: ${PhraseComposerCommandAudit.auditAll()}",
            PhraseComposerCommandAudit.passes()
        )
    }

    @Test
    fun moveUpUsesGlobalPreviousGesture() {
        val assigned = commandAction(PhraseComposerActionId.MoveUp).let { it.left to it.right }
        assertEquals(
            GuidedModeNavigation.PREVIOUS_LEFT to GuidedModeNavigation.PREVIOUS_RIGHT,
            assigned
        )
        assertEquals("L2 R0", formatWinkSequenceShort(assigned.first, assigned.second))
    }

    @Test
    fun moveDownUsesGlobalNextGesture() {
        val assigned = commandAction(PhraseComposerActionId.MoveDown).let { it.left to it.right }
        assertEquals(
            GuidedModeNavigation.NEXT_LEFT to GuidedModeNavigation.NEXT_RIGHT,
            assigned
        )
    }

    @Test
    fun moveLeftUsesCatalogSlotZero() {
        assertEquals(2 to 1, commandAction(PhraseComposerActionId.MoveLeft).let { it.left to it.right })
    }

    @Test
    fun moveRightUsesCatalogSlotOne() {
        assertEquals(1 to 2, commandAction(PhraseComposerActionId.MoveRight).let { it.left to it.right })
    }

    @Test
    fun selectKeyUsesGlobalSelectGesture() {
        val assigned = commandAction(PhraseComposerActionId.SelectKey).let { it.left to it.right }
        assertEquals(
            GuidedModeNavigation.SELECT_LEFT to GuidedModeNavigation.SELECT_RIGHT,
            assigned
        )
    }

    @Test
    fun backUsesGlobalBackGesture() {
        val assigned = commandAction(PhraseComposerActionId.Back).let { it.left to it.right }
        assertEquals(
            GuidedModeNavigation.BACK_LEFT to GuidedModeNavigation.BACK_RIGHT,
            assigned
        )
        assertEquals("L2 R2", formatWinkSequenceShort(assigned.first, assigned.second))
    }

    @Test
    fun noCommandSequenceConflictsAmongPanelActions() {
        val panel = PhraseComposerController.commandPanelEntries(keyboardState(), english)
        val sequences = panel.map { it.left to it.right }
        assertEquals(sequences.size, sequences.distinct().size)
    }

    @Test
    fun keyboardCommandsReuseCommunicationSlotsByMsgaDesign() {
        val moveLeft = PhraseComposerCommandSequences.sequenceFor(PhraseComposerActionId.MoveLeft)!!
        assertEquals(GuidedPageSequences.slotAt(0), moveLeft)
    }

    @Test
    fun emergencyGestureUnchanged() {
        assertEquals(
            EMERGENCY_LEFT_WINKS to EMERGENCY_RIGHT_WINKS,
            PhraseComposerCommandSequences.emergencySequence
        )
    }

    @Test
    fun commandLabelsDeriveFromSequenceForActionNotHardcoded() {
        val moveUp = commandAction(PhraseComposerActionId.MoveUp)
        val expected = PhraseComposerCommandSequences.sequenceFor(PhraseComposerActionId.MoveUp)!!
        assertEquals(formatWinkSequenceShort(expected.first, expected.second), moveUp.sequenceLabel)
    }

    @Test
    fun rc7C3RegressionAuditStillPasses() {
        assertTrue(PhraseComposerCommandAudit.passes())
        assertEquals(PhraseComposerMode.Keyboard, PhraseComposerController.initialState().mode)
    }
}
