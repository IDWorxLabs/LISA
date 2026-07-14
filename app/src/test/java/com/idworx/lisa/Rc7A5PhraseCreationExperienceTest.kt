package com.idworx.lisa

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** RC7A.5 — phrase creation introduction and editor placeholder flow. */
class Rc7A5PhraseCreationExperienceTest {

    private val english = LisaUiStrings(PreferredLanguage.English)

    @Test
    fun createPhrasePageUsesProductionPurposeAndIntro() {
        assertEquals("Add personalised communication phrases.", english.createPhrasePurpose)
        assertTrue(english.createPhraseIntroLead.contains("personalised", ignoreCase = true))
        assertTrue(english.createPhraseAudienceNote.contains("Caregivers", ignoreCase = true))
        assertFalse(english.createPhraseIntroLead.contains("future", ignoreCase = true))
        assertFalse(english.createPhraseIntroDetail.contains("coming soon", ignoreCase = true))
    }

    @Test
    fun createPhrasePageExplainsThreeStepWorkflow() {
        val panel = extractFunction("private fun CreatePhrasePanel")
        assertTrue(panel.contains("PhraseCreationStepCard"))
        assertTrue(panel.contains("createPhraseStep1Body"))
        assertTrue(panel.contains("createPhraseStep2Body"))
        assertTrue(panel.contains("createPhraseStep3Body"))
        assertTrue(panel.contains("createPhraseStep1Examples"))
    }

    @Test
    fun createPhrasePageHasBeginAction() {
        val panel = extractFunction("private fun CreatePhrasePanel")
        assertTrue(panel.contains("createPhraseBeginButton"))
        assertTrue(panel.contains("onBegin"))
    }

    @Test
    fun phraseComposerPageIsWiredInRootUi() {
        val source = readSource("app/src/main/java/com/idworx/lisa/LisaAccessibilityUi.kt")
        assertTrue(source.contains("EyeControlledPhraseComposerOverlay"))
        assertTrue(source.contains("LisaPanel.PhraseEditor -> Unit"))
        assertTrue(source.contains("phraseComposerActive"))
    }

    @Test
    fun navigationFlowOpensEyeControlledComposerFromCustom() {
        val mainActivity = readSource("app/src/main/java/com/idworx/lisa/MainActivity.kt")
        assertTrue(mainActivity.contains("openComposeModeFromCustom"))
        assertTrue(mainActivity.contains("CategoryAreaDestination.CreateCustomPhrase"))
        assertTrue(mainActivity.contains("PhraseComposerController.keyboardEntryState()"))
    }

    @Test
    fun phraseEditorPanelEnumAdded() {
        assertTrue(LisaPanel.entries.contains(LisaPanel.PhraseEditor))
    }

    private fun extractFunction(signature: String): String {
        val source = readSource("app/src/main/java/com/idworx/lisa/LisaAccessibilityUi.kt")
        val start = source.indexOf(signature)
        assertTrue("Expected $signature", start >= 0)
        val paramClose = source.indexOf(") {", start)
        assertTrue("Expected function body for $signature", paramClose >= 0)
        val openBrace = paramClose + 2
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
