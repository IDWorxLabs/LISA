package com.idworx.lisa

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** RC7E rollback — restore RC7D.8 composer without predictive suggestions. */
class Rc7E_RollbackToRc7D8Test {

    private val english = LisaUiStrings.forLanguage(PreferredLanguage.English)

    private fun keyboardState(phrase: String = "") = PhraseComposerState(
        mode = PhraseComposerMode.Keyboard,
        phraseText = phrase
    )

    private fun commandAction(state: PhraseComposerState, actionId: PhraseComposerActionId) =
        PhraseComposerController.commandPanelEntries(state, english).first { it.actionId == actionId }

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

    @Test
    fun noSuggestedWordsUiInComposerLayout() {
        val ui = readSource("app/src/main/java/com/idworx/lisa/PhraseComposerUi.kt")
        assertFalse(ui.contains("ComposerSuggestionPanel"))
        assertFalse(ui.contains("Suggested Words"))
        assertFalse(ui.contains("phraseComposerSuggestionsLabel"))
        assertFalse(ui.contains("state.suggestions"))
    }

    @Test
    fun noSuggestionInteractionModeInMsga() {
        val msga = readSource("app/src/main/java/com/idworx/lisa/ModeScopedGestureAuthority.kt")
        assertFalse(msga.contains("PhraseComposerSuggestions"))
        assertFalse(msga.contains("PhrasePredictionSequences"))
        assertFalse(msga.contains("phraseComposerSuggestionsActive"))
    }

    @Test
    fun noPredictionEngineWiredIntoMainActivity() {
        val main = readSource("app/src/main/java/com/idworx/lisa/MainActivity.kt")
        assertFalse(main.contains("PhrasePredictionEngine"))
        assertFalse(main.contains("PredictionPersistence"))
        assertFalse(main.contains("publishComposerState"))
        assertFalse(main.contains("applyPhraseComposerSuggestion"))
        assertFalse(main.contains("recordPredictionLearning"))
    }

    @Test
    fun noPredictionEngineFilesRemain() {
        val roots = listOfNotNull(
            java.io.File(System.getProperty("user.dir")),
            java.io.File(System.getProperty("user.dir")).parentFile
        )
        val predictionFiles = listOf(
            "PhrasePredictionEngine.kt",
            "PredictionHistory.kt",
            "PredictionDictionary.kt",
            "PredictionScorer.kt",
            "PredictionSession.kt",
            "PredictionPersistence.kt",
            "PhrasePredictionSequences.kt",
            "PredictionAudit.kt"
        )
        predictionFiles.forEach { name ->
            val exists = roots.any { root ->
                root.resolve("app/src/main/java/com/idworx/lisa/$name").isFile
            }
            assertFalse("RC7E file should be removed: $name", exists)
        }
    }

    @Test
    fun composerCommandSequencesRetainRc7d8Meanings() {
        val state = keyboardState()
        listOf(
            PhraseComposerActionId.MoveLeft,
            PhraseComposerActionId.MoveRight,
            PhraseComposerActionId.Backspace,
            PhraseComposerActionId.Preview,
            PhraseComposerActionId.Save
        ).forEach { actionId ->
            val entry = commandAction(state, actionId)
            assertEquals(actionId, entry.actionId)
            val result = PhraseComposerController.processSequence(entry.left, entry.right, state, english)
            assertTrue("$actionId should not be unmatched", result !is PhraseComposerSequenceResult.Unmatched)
        }
    }

    @Test
    fun keyboardComposerLayoutRestoredWithoutSuggestionPanel() {
        val ui = readSource("app/src/main/java/com/idworx/lisa/PhraseComposerUi.kt")
        val keyboardSection = ui.substringAfter("private fun KeyboardComposerLayout")
            .substringBefore("private fun NonKeyboardComposerLayout")
        assertTrue(keyboardSection.contains("ComposerEyeStatusBar"))
        assertTrue(keyboardSection.contains("ComposerPhraseField"))
        assertTrue(keyboardSection.contains("ComposerCommandGrid"))
        assertTrue(keyboardSection.contains("BottomAlignedEyeKeyboard"))
        assertFalse(keyboardSection.contains("ComposerSuggestionPanel"))
    }

    @Test
    fun emptyPhraseShowsNoFalseMaximumLengthError() {
        val state = keyboardState()
        val punctuationRow = KeyboardLayout.punctuationRowIndex(EyeKeyboardLayoutMode.Letters)
        val slot = KeyboardLayout.slotAt(EyeKeyboardLayoutMode.Letters, punctuationRow, 0)
        assertTrue(slot is KeyboardSlot.Character)
        val result = PhraseComposerController.processTouchKey(
            row = punctuationRow,
            col = 0,
            state = state,
            uiStrings = english
        ) as PhraseComposerSequenceResult.Navigate
        assertNull(result.newState.errorMessage)
    }

    @Test
    fun rc7d8AndMsgaAuditsRemainGreen() {
        assertTrue(ModeScopedGestureAuthorityAudit.passes())
        assertTrue(PhraseComposerCommandAudit.passes())
    }
}
