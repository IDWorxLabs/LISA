package com.idworx.lisa

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** RC7D.16 — Phrase Management sequence-label clarity. */
class Rc7D_16PhraseManagementSequenceLabelClarityTest {

    private val english = LisaUiStrings.forLanguage(PreferredLanguage.English)

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
    fun listLabelsDistinguishSpeakFromOpenDetails() {
        assertEquals("Speak L5 R1", english.phraseManagementSpeakSequence("L5 R1"))
        assertEquals("Open details L2 R1", english.phraseManagementOpenDetailsSequence("L2 R1"))
        val ui = readSource("app/src/main/java/com/idworx/lisa/PhraseManagementUi.kt")
        assertTrue(ui.contains("phraseManagementSpeakSequence"))
        assertTrue(ui.contains("phraseManagementOpenDetailsSequence"))
    }

    @Test
    fun detailsActionsExposeCanonicalBlinkSequences() {
        val actions = PhraseManagementController.detailsActionEntries(english)
        assertEquals(3, actions.size)
        assertEquals(english.phraseManagementEditPhrase, actions[0].label)
        assertEquals(english.phraseManagementMoveCategory, actions[1].label)
        assertEquals(english.phraseManagementDeletePhrase, actions[2].label)
        assertEquals(GuidedPageSequences.slotAt(0), actions[0].left to actions[0].right)
        assertEquals(GuidedPageSequences.slotAt(1), actions[1].left to actions[1].right)
        assertEquals(GuidedPageSequences.slotAt(2), actions[2].left to actions[2].right)
        assertEquals("L2 R1", formatWinkSequenceShort(actions[0].left, actions[0].right))
        assertEquals("L1 R2", formatWinkSequenceShort(actions[1].left, actions[1].right))
        assertEquals("L3 R1", formatWinkSequenceShort(actions[2].left, actions[2].right))
    }

    @Test
    fun detailsUiRendersActionSequenceLabels() {
        val ui = readSource("app/src/main/java/com/idworx/lisa/PhraseManagementUi.kt")
        assertTrue(ui.contains("detailsActionEntries"))
        assertTrue(ui.contains("sequenceLabel = formatWinkSequenceShort"))
        val main = readSource("app/src/main/java/com/idworx/lisa/MainActivity.kt")
        assertTrue(main.contains("PhraseDetailsAction.Edit"))
        assertTrue(main.contains("PhraseDetailsAction.Move"))
        assertTrue(main.contains("PhraseDetailsAction.Delete"))
    }

    @Test
    fun noAndroidImeAndEyeKeyboardRemain() {
        val management = readSource("app/src/main/java/com/idworx/lisa/PhraseManagementUi.kt")
        assertFalse(management.contains("OutlinedTextField"))
        assertFalse(management.contains("LocalSoftwareKeyboardController"))
        val composer = readSource("app/src/main/java/com/idworx/lisa/PhraseComposerUi.kt")
        assertTrue(composer.contains("BottomAlignedEyeKeyboard"))
    }
}
