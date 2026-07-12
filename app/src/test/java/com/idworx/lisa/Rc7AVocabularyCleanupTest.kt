package com.idworx.lisa

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** RC7A — Vocabulary page management cleanup (presentation only). */
class Rc7AVocabularyCleanupTest {

    private val english = LisaUiStrings(PreferredLanguage.English)

    @Test
    fun vocabularyPanelNoLongerRendersCoreVocabularyList() {
        val panel = extractFunction("private fun VocabularyTrainingPanel")
        assertFalse(panel.contains("coreVocabulary"))
        assertFalse(panel.contains("LazyColumn"))
        assertFalse(panel.contains("coreMappings"))
    }

    @Test
    fun vocabularyPanelNoLongerRendersSystemCommandsDocumentation() {
        val panel = extractFunction("private fun VocabularyTrainingPanel")
        assertFalse(panel.contains("SystemCommandsTrainingSection"))
        assertFalse(panel.contains("systemCommandsTitle"))
    }

    @Test
    fun manualSequenceEditorRemovedFromVocabularyPage() {
        val panel = extractFunction("private fun VocabularyTrainingPanel")
        assertFalse(panel.contains("leftLabel"))
        assertFalse(panel.contains("rightLabel"))
        assertFalse(panel.contains("OutlinedTextField"))
        assertFalse(panel.contains("onAddMapping"))
        assertFalse(panel.contains("Save sequence"))
    }

    @Test
    fun vocabularyPageContainsCreatePhraseEntryPoint() {
        val panel = extractFunction("private fun VocabularyTrainingPanel")
        assertTrue(panel.contains("VocabularyCreatePhraseCard"))
        assertTrue(panel.contains("onOpenCreatePhrase"))
        val card = extractFunction("private fun VocabularyCreatePhraseCard")
        assertTrue(card.contains("createPhraseCardTitle"))
        assertTrue(card.contains("createPhraseCardDescription"))
        assertTrue(card.contains("createPhraseButton"))
    }

    @Test
    fun createPhraseIntroductionPageExists() {
        val source = readSource("app/src/main/java/com/idworx/lisa/LisaAccessibilityUi.kt")
        assertTrue(source.contains("private fun CreatePhrasePanel"))
        assertTrue(source.contains("createPhraseIntroLead"))
        assertTrue(source.contains("createPhraseHowItWorksTitle"))
        assertTrue(source.contains("createPhraseBeginButton"))
        assertTrue(source.contains("LisaPanel.CreatePhrase -> CreatePhrasePanel"))
        assertFalse(source.contains("createPhrasePlaceholderBody"))
    }

    @Test
    fun createPhraseNavigationReturnsToVocabulary() {
        val mainActivity = readSource("app/src/main/java/com/idworx/lisa/MainActivity.kt")
        assertTrue(mainActivity.contains("LisaPanel.CreatePhrase"))
        assertTrue(
            mainActivity.contains("openPanel(LisaPanel.CreatePhrase, LisaPanel.VocabularyTraining)")
        )
    }

    @Test
    fun communicationScreenVocabularyOverlayRemains() {
        val source = readSource("app/src/main/java/com/idworx/lisa/LisaAccessibilityUi.kt")
        assertTrue(source.contains("GuidedVocabularyOverlay"))
    }

    @Test
    fun createPhrasePanelEnumAdded() {
        assertTrue(LisaPanel.entries.contains(LisaPanel.CreatePhrase))
    }

    @Test
    fun vocabularyPurposeReflectsManagementFocus() {
        assertEquals("Manage your communication phrases.", english.vocabularyPurpose)
        assertTrue(english.vocabularyBuiltinNote.contains("Communication screen", ignoreCase = true))
    }

    private fun extractFunction(signature: String): String {
        val source = readSource("app/src/main/java/com/idworx/lisa/LisaAccessibilityUi.kt")
        val start = source.indexOf(signature)
        assertTrue("Expected $signature", start >= 0)
        val openBrace = source.indexOf('{', start)
        var depth = 0
        for (index in openBrace until source.length) {
            when (source[index]) {
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) return source.substring(start, index + 1)
                }
            }
        }
        error("Unterminated function: $signature")
    }

    private fun readSource(relativePath: String): String {
        val normalized = relativePath.replace('/', File.separatorChar)
        val roots = listOfNotNull(
            File(System.getProperty("user.dir")),
            File(System.getProperty("user.dir")).parentFile
        )
        for (root in roots) {
            val direct = root.resolve(normalized)
            if (direct.isFile) return direct.readText()
            if (normalized.startsWith("app${File.separatorChar}")) {
                val withoutApp = root.resolve(normalized.removePrefix("app${File.separatorChar}"))
                if (withoutApp.isFile) return withoutApp.readText()
            }
        }
        error("Missing source: $relativePath")
    }
}
