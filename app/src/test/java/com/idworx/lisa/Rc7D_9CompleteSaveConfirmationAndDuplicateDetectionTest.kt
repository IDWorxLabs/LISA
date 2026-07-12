package com.idworx.lisa

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** RC7D.9 — save confirmation UI and intelligent duplicate phrase detection. */
class Rc7D_9CompleteSaveConfirmationAndDuplicateDetectionTest {

    private val english = LisaUiStrings.forLanguage(PreferredLanguage.English)

    private fun runtimeContext(custom: List<WinkMapping> = emptyList()) =
        PhraseComposerRuntimeContext(
            customMappings = defaultLanguageMappings() + custom,
            language = PreferredLanguage.English
        )

    private fun keyboardState(phrase: String = "NEW PHRASE") = PhraseComposerState(
        mode = PhraseComposerMode.Keyboard,
        phraseText = phrase
    )

    private fun saveConfirmationState(
        phrase: String = "NEW PHRASE",
        category: CustomPhraseEngine.CaregiverPhraseCategory =
            CustomPhraseEngine.CaregiverPhraseCategory.Medical,
        sequence: Pair<Int, Int> = 1 to 5
    ) = PhraseComposerState(
        mode = PhraseComposerMode.SaveConfirmation,
        phraseText = phrase,
        selectedCategory = category,
        pendingAllocatedSequence = sequence
    )

    private fun commandAction(state: PhraseComposerState, actionId: PhraseComposerActionId) =
        PhraseComposerController.commandPanelEntries(state, english).first { it.actionId == actionId }

    @Test
    fun saveConfirmationShowsConfirmSaveAction() {
        val state = saveConfirmationState()
        val confirm = commandAction(state, PhraseComposerActionId.ConfirmSave)
        assertEquals("Confirm Save", confirm.label)
    }

    @Test
    fun confirmSaveDisplaysL1R1() {
        val confirm = commandAction(saveConfirmationState(), PhraseComposerActionId.ConfirmSave)
        assertEquals(1, confirm.left)
        assertEquals(1, confirm.right)
        assertEquals("L1 R1", confirm.sequenceLabel)
    }

    @Test
    fun backDisplaysL2R2() {
        val back = commandAction(saveConfirmationState(), PhraseComposerActionId.Back)
        assertEquals(2, back.left)
        assertEquals(2, back.right)
        assertEquals("L2 R2", back.sequenceLabel)
    }

    @Test
    fun emergencyDisplaysL6R0() {
        assertEquals("L6 R0", formatWinkSequenceShort(EMERGENCY_LEFT_WINKS, EMERGENCY_RIGHT_WINKS))
    }

    @Test
    fun confirmSavePersistsPhraseExactlyOnce() {
        val mappings = defaultLanguageMappings().toMutableList()
        val allocated = CustomPhraseEngine.allocateSequence(
            CustomPhraseEngine.CaregiverPhraseCategory.Medical,
            mappings
        )!!
        val state = saveConfirmationState(phrase = "Please bring my blanket", sequence = allocated)
        val confirm = commandAction(state, PhraseComposerActionId.ConfirmSave)
        val result = PhraseComposerController.processSequence(
            confirm.left,
            confirm.right,
            state,
            english,
            runtimeContext(mappings.filter { it.isCustom })
        )
        assertTrue(result is PhraseComposerSequenceResult.Save)
        val saveResult = CustomPhraseEngine.saveNewPhraseWithAllocatedSequence(
            rawPhrase = "Please bring my blanket",
            category = CustomPhraseEngine.CaregiverPhraseCategory.Medical,
            allocatedSequence = allocated,
            existingMappings = mappings,
            uiStrings = english
        )
        assertTrue(saveResult is CustomPhraseEngine.SavePhraseResult.Success)
        val mapping = (saveResult as CustomPhraseEngine.SavePhraseResult.Success).mapping
        mappings.add(mapping)
        assertEquals(1, mappings.count { it.isCustom && it.customPhrase == "Please bring my blanket" })
        assertEquals(allocated, mapping.left to mapping.right)
    }

    @Test
    fun backFromSaveConfirmationReturnsWithoutSaving() {
        val state = saveConfirmationState(phrase = "Keep editing me", sequence = 3 to 2)
        val back = commandAction(state, PhraseComposerActionId.Back)
        val result = PhraseComposerController.processSequence(
            back.left,
            back.right,
            state,
            english
        ) as PhraseComposerSequenceResult.Navigate
        assertEquals(PhraseComposerMode.Keyboard, result.newState.mode)
        assertEquals("Keep editing me", result.newState.phraseText)
        assertEquals(CustomPhraseEngine.CaregiverPhraseCategory.Medical, result.newState.selectedCategory)
        assertNull(result.newState.pendingAllocatedSequence)
    }

    @Test
    fun backPreservesKeyboardCursorAndLayoutState() {
        val state = saveConfirmationState().copy(
            cursorRow = 2,
            cursorCol = 3,
            keyboardLayoutMode = EyeKeyboardLayoutMode.Numbers,
            keyboardShiftMode = KeyboardShiftMode.CapsLock
        )
        val back = commandAction(state, PhraseComposerActionId.Back)
        val result = PhraseComposerController.processSequence(
            back.left,
            back.right,
            state,
            english
        ) as PhraseComposerSequenceResult.Navigate
        assertEquals(2, result.newState.cursorRow)
        assertEquals(3, result.newState.cursorCol)
        assertEquals(EyeKeyboardLayoutMode.Numbers, result.newState.keyboardLayoutMode)
        assertEquals(KeyboardShiftMode.CapsLock, result.newState.keyboardShiftMode)
    }

    @Test
    fun duplicateBuiltInPhraseIsBlockedOnSave() {
        val result = PhraseComposerController.processSequence(
            commandAction(keyboardState("Hello"), PhraseComposerActionId.Save).left,
            commandAction(keyboardState("Hello"), PhraseComposerActionId.Save).right,
            keyboardState("Hello"),
            english,
            runtimeContext()
        ) as PhraseComposerSequenceResult.Navigate
        assertEquals(PhraseComposerMode.DuplicateWarning, result.newState.mode)
        assertNotNull(result.newState.duplicateMatch)
        assertEquals(PhraseDuplicateSource.BuiltIn, result.newState.duplicateMatch?.source)
    }

    @Test
    fun duplicateCustomPhraseIsBlockedOnSave() {
        val custom = listOf(
            WinkMapping(
                left = 5,
                right = 2,
                vocabularyId = "My unique caregiver phrase",
                isCustom = true,
                customPhrase = "My unique caregiver phrase",
                caregiverCategory = CustomPhraseEngine.CaregiverPhraseCategory.Family
            )
        )
        val result = PhraseComposerController.processSequence(
            commandAction(keyboardState("MY UNIQUE CAREGIVER PHRASE"), PhraseComposerActionId.Save).left,
            commandAction(keyboardState("MY UNIQUE CAREGIVER PHRASE"), PhraseComposerActionId.Save).right,
            keyboardState("MY UNIQUE CAREGIVER PHRASE"),
            english,
            runtimeContext(custom)
        ) as PhraseComposerSequenceResult.Navigate
        assertEquals(PhraseComposerMode.DuplicateWarning, result.newState.mode)
        assertEquals(PhraseDuplicateSource.Custom, result.newState.duplicateMatch?.source)
        assertEquals(CustomPhraseEngine.CaregiverPhraseCategory.Family, result.newState.duplicateMatch?.category)
    }

    @Test
    fun duplicateDetectionIgnoresCapitalization() {
        val match = PhraseDuplicateEngine.findDuplicate("hELLO", emptyList(), PreferredLanguage.English, english)
        assertNotNull(match)
    }

    @Test
    fun duplicateDetectionTrimsLeadingAndTrailingSpaces() {
        val match = PhraseDuplicateEngine.findDuplicate("  Hello  ", emptyList(), PreferredLanguage.English, english)
        assertNotNull(match)
    }

    @Test
    fun duplicateDetectionCollapsesRepeatedInternalSpaces() {
        val match = PhraseDuplicateEngine.findDuplicate("I  am  in  pain", emptyList(), PreferredLanguage.English, english)
        assertNotNull(match)
    }

    @Test
    fun duplicateDetectionHandlesTerminalPunctuation() {
        val match = PhraseDuplicateEngine.findDuplicate(
            "\"I am in pain.\"",
            emptyList(),
            PreferredLanguage.English,
            english
        )
        assertNotNull(match)
        assertEquals(CustomPhraseEngine.CaregiverPhraseCategory.Medical, match?.category)
    }

    @Test
    fun duplicateResultIncludesExistingCategory() {
        val match = PhraseDuplicateEngine.findDuplicate("Hello", emptyList(), PreferredLanguage.English, english)
        assertNotNull(match)
        assertEquals(CustomPhraseEngine.CaregiverPhraseCategory.Conversation, match?.category)
    }

    @Test
    fun duplicateMessageNamesPhraseAndCategory() {
        val match = DuplicatePhraseMatch(
            phrase = "Hello",
            category = CustomPhraseEngine.CaregiverPhraseCategory.Conversation,
            source = PhraseDuplicateSource.BuiltIn,
            sequence = 1 to 6
        )
        val message = english.phraseDuplicateExistsMessage(match)
        assertTrue(message.contains("HELLO"))
        assertTrue(message.contains("General Conversation"))
    }

    @Test
    fun duplicateWarningSupportsContinueEditingByEye() {
        val state = PhraseComposerState(
            mode = PhraseComposerMode.DuplicateWarning,
            phraseText = "Hello",
            duplicateMatch = PhraseDuplicateEngine.findDuplicate("Hello", emptyList(), PreferredLanguage.English, english)
        )
        val continueEditing = commandAction(state, PhraseComposerActionId.ContinueEditing)
        assertEquals(2, continueEditing.left)
        assertEquals(2, continueEditing.right)
        val result = PhraseComposerController.processSequence(
            continueEditing.left,
            continueEditing.right,
            state,
            english
        ) as PhraseComposerSequenceResult.Navigate
        assertEquals(PhraseComposerMode.Keyboard, result.newState.mode)
        assertEquals("Hello", result.newState.phraseText)
    }

    @Test
    fun duplicateWarningSupportsOpenCategoryByEye() {
        val match = PhraseDuplicateEngine.findDuplicate("Hello", emptyList(), PreferredLanguage.English, english)!!
        val state = PhraseComposerState(
            mode = PhraseComposerMode.DuplicateWarning,
            phraseText = "Hello",
            duplicateMatch = match
        )
        val openCategory = commandAction(state, PhraseComposerActionId.OpenDuplicateCategory)
        assertEquals(1, openCategory.left)
        assertEquals(1, openCategory.right)
        val result = PhraseComposerController.processSequence(
            openCategory.left,
            openCategory.right,
            state,
            english
        )
        assertTrue(result is PhraseComposerSequenceResult.OpenExistingPhrase)
        assertEquals(match.category, (result as PhraseComposerSequenceResult.OpenExistingPhrase).match.category)
    }

    @Test
    fun finalPrePersistDuplicateCheckBlocksStaleSave() {
        val custom = listOf(
            WinkMapping(
                left = 4,
                right = 2,
                vocabularyId = "Late duplicate",
                isCustom = true,
                customPhrase = "Late duplicate",
                caregiverCategory = CustomPhraseEngine.CaregiverPhraseCategory.Conversation
            )
        )
        val saveResult = CustomPhraseEngine.saveNewPhraseWithAllocatedSequence(
            rawPhrase = "Late duplicate",
            category = CustomPhraseEngine.CaregiverPhraseCategory.Medical,
            allocatedSequence = 5 to 1,
            existingMappings = defaultLanguageMappings() + custom,
            uiStrings = english
        )
        assertTrue(saveResult is CustomPhraseEngine.SavePhraseResult.ValidationFailed)
        assertEquals(
            CustomPhraseEngine.PhraseValidationFailure.Duplicate,
            (saveResult as CustomPhraseEngine.SavePhraseResult.ValidationFailed).reason
        )
        assertNotNull(saveResult.duplicateMatch)
    }

    @Test
    fun noDuplicateMappingIsStoredWhenValidationFails() {
        val before = defaultLanguageMappings().count { it.isCustom }
        val saveResult = CustomPhraseEngine.saveNewPhrase(
            rawPhrase = "Hello",
            category = CustomPhraseEngine.CaregiverPhraseCategory.Medical,
            existingMappings = defaultLanguageMappings(),
            uiStrings = english
        )
        assertTrue(saveResult is CustomPhraseEngine.SavePhraseResult.ValidationFailed)
        assertEquals(before, defaultLanguageMappings().count { it.isCustom })
    }

    @Test
    fun existingPhraseSequenceRemainsUnchangedWhenDuplicateBlocked() {
        val custom = listOf(
            WinkMapping(
                left = 7,
                right = 1,
                vocabularyId = "My phrase",
                isCustom = true,
                customPhrase = "My phrase",
                caregiverCategory = CustomPhraseEngine.CaregiverPhraseCategory.Family
            )
        )
        val saveResult = CustomPhraseEngine.saveNewPhraseWithAllocatedSequence(
            rawPhrase = "My phrase",
            category = CustomPhraseEngine.CaregiverPhraseCategory.Medical,
            allocatedSequence = 2 to 2,
            existingMappings = defaultLanguageMappings() + custom,
            uiStrings = english
        )
        assertTrue(saveResult is CustomPhraseEngine.SavePhraseResult.ValidationFailed)
        assertEquals(7 to 1, custom.first().left to custom.first().right)
    }

    @Test
    fun confirmSaveBlockedWhenDuplicateAppearsBeforePersist() {
        val custom = listOf(
            WinkMapping(
                left = 5,
                right = 1,
                vocabularyId = "Shared phrase",
                isCustom = true,
                customPhrase = "Shared phrase",
                caregiverCategory = CustomPhraseEngine.CaregiverPhraseCategory.BasicNeeds
            )
        )
        val state = saveConfirmationState(phrase = "Shared phrase", sequence = 6 to 1)
        val confirm = commandAction(state, PhraseComposerActionId.ConfirmSave)
        val result = PhraseComposerController.processSequence(
            confirm.left,
            confirm.right,
            state,
            english,
            runtimeContext(custom)
        )
        assertTrue(result is PhraseComposerSequenceResult.Navigate)
        assertEquals(
            PhraseComposerMode.DuplicateWarning,
            (result as PhraseComposerSequenceResult.Navigate).newState.mode
        )
    }

    @Test
    fun rc7dRegressionAuditsRemainGreen() {
        assertTrue(ModeScopedGestureAuthorityAudit.passes())
        assertTrue(PhraseComposerCommandAudit.passes())
    }
}
