package com.idworx.lisa

/**
 * Keyboard composer command-panel sequences — owned by [ModeScopedGestureAuthority].
 *
 * RC7D navigation panel reuses global movement/select gestures plus mode-scoped catalog slots.
 */
object PhraseComposerCommandSequences {

    /** Keyboard navigation panel order (RC7D). */
    val keyboardPanelActionOrder: List<PhraseComposerActionId> = listOf(
        PhraseComposerActionId.MoveUp,
        PhraseComposerActionId.MoveDown,
        PhraseComposerActionId.MoveLeft,
        PhraseComposerActionId.MoveRight,
        PhraseComposerActionId.SelectKey,
        PhraseComposerActionId.Backspace,
        PhraseComposerActionId.Preview,
        PhraseComposerActionId.Save,
        PhraseComposerActionId.ToggleKeyboardLayout,
        PhraseComposerActionId.Back
    )

    val emergencySequence: Pair<Int, Int> = EMERGENCY_LEFT_WINKS to EMERGENCY_RIGHT_WINKS

    fun sequenceFor(actionId: PhraseComposerActionId): Pair<Int, Int>? =
        ModeScopedGestureAuthority.phraseComposerCommandSequences[actionId]

    fun allKeyboardCommandSequences(): Map<PhraseComposerActionId, Pair<Int, Int>> =
        ModeScopedGestureAuthority.phraseComposerCommandSequences

    fun winkTotal(sequence: Pair<Int, Int>): Int = sequence.first + sequence.second
}

/**
 * Self-audit for keyboard command-panel sequences (delegates to MSGA).
 */
object PhraseComposerCommandAudit {

    data class Finding(val message: String)

    fun auditAll(): List<Finding> = buildList {
        addAll(auditEveryCommandHasSequence())
        addAll(auditNoDuplicateCommandSequences())
        addAll(auditKeyboardNavigationSequences())
        addAll(auditEmergencyUnchanged())
        addAll(auditBackUnchanged())
    }

    fun passes(): Boolean = auditAll().isEmpty()

    private fun auditEveryCommandHasSequence(): List<Finding> {
        val missing = PhraseComposerCommandSequences.keyboardPanelActionOrder.filter { actionId ->
            PhraseComposerCommandSequences.sequenceFor(actionId) == null
        }
        return if (missing.isEmpty()) emptyList() else {
            listOf(Finding("Keyboard commands missing sequences: $missing"))
        }
    }

    private fun auditNoDuplicateCommandSequences(): List<Finding> {
        val sequences = PhraseComposerCommandSequences.keyboardPanelActionOrder.mapNotNull { actionId ->
            PhraseComposerCommandSequences.sequenceFor(actionId)?.let { actionId to it }
        }
        val dupes = sequences.groupBy { it.second }.filter { it.value.size > 1 }
        return if (dupes.isEmpty()) emptyList() else {
            listOf(Finding("Duplicate keyboard command sequences: ${dupes.keys}"))
        }
    }

    private fun auditKeyboardNavigationSequences(): List<Finding> =
        ModeScopedGestureAuthorityAudit.auditKeyboardNavigationSequences()
            .map { Finding(it.message) }

    private fun auditEmergencyUnchanged(): List<Finding> =
        if (PhraseComposerCommandSequences.emergencySequence == EMERGENCY_LEFT_WINKS to EMERGENCY_RIGHT_WINKS) {
            emptyList()
        } else {
            listOf(Finding("Emergency gesture must remain L6 R0"))
        }

    private fun auditBackUnchanged(): List<Finding> {
        val back = PhraseComposerCommandSequences.sequenceFor(PhraseComposerActionId.Back)
        return if (back == (GuidedModeNavigation.BACK_LEFT to GuidedModeNavigation.BACK_RIGHT)) {
            emptyList()
        } else {
            listOf(Finding("Back must remain L2 R2"))
        }
    }
}
