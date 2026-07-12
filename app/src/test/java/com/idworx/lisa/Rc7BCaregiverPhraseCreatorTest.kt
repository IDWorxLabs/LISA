package com.idworx.lisa

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** RC7B — caregiver phrase creator engine and workflow. */
class Rc7BCaregiverPhraseCreatorTest {

    @Test
    fun emptyPhraseIsRejected() {
        val result = CustomPhraseEngine.validatePhrase("   ", emptyList())
        assertTrue(result is CustomPhraseEngine.PhraseValidationResult.Invalid)
        assertEquals(
            CustomPhraseEngine.PhraseValidationFailure.Empty,
            (result as CustomPhraseEngine.PhraseValidationResult.Invalid).reason
        )
    }

    @Test
    fun whitespaceOnlyPhraseIsRejected() {
        val result = CustomPhraseEngine.validatePhrase("\t\n", emptyList())
        assertTrue(result is CustomPhraseEngine.PhraseValidationResult.Invalid)
    }

    @Test
    fun tooLongPhraseIsRejected() {
        val longPhrase = "a".repeat(CustomPhraseEngine.MAX_PHRASE_LENGTH + 1)
        val result = CustomPhraseEngine.validatePhrase(longPhrase, emptyList())
        assertTrue(result is CustomPhraseEngine.PhraseValidationResult.Invalid)
        assertEquals(
            CustomPhraseEngine.PhraseValidationFailure.TooLong,
            (result as CustomPhraseEngine.PhraseValidationResult.Invalid).reason
        )
    }

    @Test
    fun duplicateCustomPhraseIsRejected() {
        val existing = listOf(
            WinkMapping(7, 7, "glasses", isCustom = true, customPhrase = "I need my glasses.")
        )
        val result = CustomPhraseEngine.validatePhrase("I need my glasses.", existing)
        assertTrue(result is CustomPhraseEngine.PhraseValidationResult.Invalid)
        assertEquals(
            CustomPhraseEngine.PhraseValidationFailure.Duplicate,
            (result as CustomPhraseEngine.PhraseValidationResult.Invalid).reason
        )
    }

    @Test
    fun duplicateBuiltInPhraseIsRejected() {
        val result = CustomPhraseEngine.validatePhrase("Hello", emptyList())
        assertTrue(result is CustomPhraseEngine.PhraseValidationResult.Invalid)
        assertEquals(
            CustomPhraseEngine.PhraseValidationFailure.Duplicate,
            (result as CustomPhraseEngine.PhraseValidationResult.Invalid).reason
        )
    }

    @Test
    fun automaticSequenceAllocationIsDeterministic() {
        val mappings = defaultLanguageMappings()
        val first = CustomPhraseEngine.allocateSequence(
            CustomPhraseEngine.CaregiverPhraseCategory.Medical,
            mappings
        )
        val second = CustomPhraseEngine.allocateSequence(
            CustomPhraseEngine.CaregiverPhraseCategory.Medical,
            mappings
        )
        assertEquals(first, second)
        assertNotNull(first)
        assertFalse(LisaSystemLanguage.isReservedSystemSequence(first!!.first, first.second))
        assertFalse(isEmergencySequence(first.first, first.second))
    }

    @Test
    fun allocatedSequenceDoesNotDuplicateExistingCustomPhrase() {
        val existing = defaultLanguageMappings() + listOf(
            WinkMapping(
                left = 5,
                right = 1,
                vocabularyId = "glasses",
                isCustom = true,
                customPhrase = "I need my glasses.",
                caregiverCategory = CustomPhraseEngine.CaregiverPhraseCategory.Conversation
            )
        )
        val saveResult = CustomPhraseEngine.saveNewPhrase(
            "Please open the window.",
            CustomPhraseEngine.CaregiverPhraseCategory.Conversation,
            existing
        )
        assertTrue(saveResult is CustomPhraseEngine.SavePhraseResult.Success)
        val mapping = (saveResult as CustomPhraseEngine.SavePhraseResult.Success).mapping
        val occupied = CustomPhraseEngine.categoryLocalOccupiedSequences(
            CustomPhraseEngine.CaregiverPhraseCategory.Conversation,
            existing
        )
        assertFalse(occupied.contains(mapping.left to mapping.right))
    }

    @Test
    fun customMappingsPersistCategoryThroughStorageRoundTrip() {
        val original = listOf(
            WinkMapping(
                left = 7,
                right = 2,
                vocabularyId = "I need my glasses.",
                isCustom = true,
                customPhrase = "I need my glasses.",
                caregiverCategory = CustomPhraseEngine.CaregiverPhraseCategory.Medical
            )
        )
        val serialized = CustomPhraseEngine.serializeCustomMappings(original)
        val restored = CustomPhraseEngine.parseCustomMappings(serialized)
        assertEquals(1, restored.size)
        assertEquals(CustomPhraseEngine.CaregiverPhraseCategory.Medical, restored.first().caregiverCategory)
        assertEquals("I need my glasses.", restored.first().customPhrase)
    }

    @Test
    fun catalogEntriesExposeSavedCustomPhraseInSelectedCategory() {
        val mapping = WinkMapping(
            left = 7,
            right = 2,
            vocabularyId = "I need my glasses.",
            isCustom = true,
            customPhrase = "I need my glasses.",
            caregiverCategory = CustomPhraseEngine.CaregiverPhraseCategory.Medical
        )
        val entries = CustomPhraseEngine.toCatalogEntries(listOf(mapping))
        assertEquals(1, entries.size)
        assertEquals(CustomPhraseEngine.CaregiverPhraseCategory.Medical, entries.first().category)
        val pages = GuidedVocabularyCatalog.buildPages(
            PreferredLanguage.English,
            LisaUiStrings(PreferredLanguage.English),
            GuidedCatalogContext(caregiverCustomPhrases = entries)
        )
        val medicalPage = pages.first { it.category == GuidedVocabularyCategory.Medical }
        assertTrue(medicalPage.entries.any { it.phrase == "I need my glasses." && it.left == 7 && it.right == 2 })
    }

    @Test
    fun builtInCatalogEntriesRemainWhenCustomPhraseAdded() {
        val pagesWithoutCustom = GuidedVocabularyCatalog.buildPages(
            PreferredLanguage.English,
            LisaUiStrings(PreferredLanguage.English)
        )
        val pagesWithCustom = GuidedVocabularyCatalog.buildPages(
            PreferredLanguage.English,
            LisaUiStrings(PreferredLanguage.English),
            GuidedCatalogContext(
                caregiverCustomPhrases = listOf(
                    CustomPhraseEngine.CaregiverCustomPhraseEntry(
                        phrase = "Extra phrase",
                        left = 7,
                        right = 3,
                        category = CustomPhraseEngine.CaregiverPhraseCategory.Conversation
                    )
                )
            )
        )
        val conversationWithout = pagesWithoutCustom.first { it.category == GuidedVocabularyCategory.Conversation }
        val conversationWith = pagesWithCustom.first { it.category == GuidedVocabularyCategory.Conversation }
        assertTrue(conversationWith.entries.size > conversationWithout.entries.size)
        assertTrue(
            conversationWith.entries.take(conversationWithout.entries.size).zip(conversationWithout.entries)
                .all { (withEntry, withoutEntry) -> withEntry.phrase == withoutEntry.phrase }
        )
    }

    @Test
    fun phraseComposerUiHasEyeControlledSaveAndSuccessActions() {
        val composer = readSource("app/src/main/java/com/idworx/lisa/PhraseComposerUi.kt")
        val engine = readSource("app/src/main/java/com/idworx/lisa/PhraseComposerEngine.kt")
        assertTrue(composer.contains("phraseCreatedSuccess"))
        assertTrue(engine.contains("phraseEditorCreateAnother"))
        assertTrue(engine.contains("phraseEditorReturnToCommunication"))
        assertTrue(engine.contains("PhraseComposerActionId.Save"))
        assertTrue(engine.contains("PhraseComposerActionId.Preview"))
        assertFalse(composer.contains("OutlinedTextField"))
    }

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
