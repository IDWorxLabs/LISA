package com.idworx.lisa

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** RC7D.10 — custom phrase save integrity and phrase management. */
class Rc7D_10CustomPhraseSaveIntegrityTest {

    private val english = LisaUiStrings.forLanguage(PreferredLanguage.English)

    @Before
    fun setUp() {
        CustomPhraseRepository.testStorage = mutableMapOf()
    }

    @After
    fun tearDown() {
        CustomPhraseRepository.testStorage = null
    }

    private fun baseMappings(): List<WinkMapping> = defaultLanguageMappings()

    private fun saveConfirmationState(
        phrase: String,
        category: CustomPhraseEngine.CaregiverPhraseCategory =
            CustomPhraseEngine.CaregiverPhraseCategory.Medical,
        sequence: Pair<Int, Int>
    ) = PhraseComposerState(
        mode = PhraseComposerMode.SaveConfirmation,
        phraseText = phrase,
        selectedCategory = category,
        pendingAllocatedSequence = sequence
    )

    private fun createCustom(
        phrase: String,
        category: CustomPhraseEngine.CaregiverPhraseCategory,
        mappings: List<WinkMapping>? = null
    ): PhraseSaveTransactionResult {
        val allMappings = mappings ?: (baseMappings() + CustomPhraseRepository.loadCustomMappings())
        val sequence = CustomPhraseEngine.allocateSequence(category, allMappings)!!
        return CustomPhraseRepository.createPhrase(
            rawPhrase = phrase,
            category = category,
            allocatedSequence = sequence,
            existingMappings = allMappings,
            language = PreferredLanguage.English,
            uiStrings = english
        )
    }

    @Test
    fun confirmSaveWritesMappingToPersistentStorage() {
        val result = createCustom("I have a headache", CustomPhraseEngine.CaregiverPhraseCategory.Medical)
        assertTrue(result is PhraseSaveTransactionResult.Success)
        val mapping = (result as PhraseSaveTransactionResult.Success).mapping
        assertTrue(CustomPhraseRepository.verifyPersistedMapping(mapping))
        assertEquals(1, CustomPhraseRepository.loadCustomMappings().size)
    }

    @Test
    fun savePerformsReadAfterWriteVerification() {
        val result = createCustom("Need water please", CustomPhraseEngine.CaregiverPhraseCategory.BasicNeeds)
        assertTrue(result is PhraseSaveTransactionResult.Success)
        val success = result as PhraseSaveTransactionResult.Success
        assertTrue(success.persisted)
        assertTrue(CustomPhraseRepository.verifyPersistedMapping(success.mapping))
    }

    @Test
    fun successImpossibleIfPersistenceVerificationFails() {
        CustomPhraseRepository.testStorage = object : LinkedHashMap<String, String>() {
            override fun put(key: String, value: String): String? {
                super.put(key, value)
                remove(key)
                return value
            }
        }
        val result = createCustom("Broken save phrase", CustomPhraseEngine.CaregiverPhraseCategory.Family)
        assertTrue(result is PhraseSaveTransactionResult.Failed)
        assertEquals(
            PhraseSaveFailureReason.StorageVerificationFailed,
            (result as PhraseSaveTransactionResult.Failed).reason
        )
    }

    @Test
    fun successScreenBlockedWhenVerificationFails() {
        val state = saveConfirmationState("Verify me", sequence = 1 to 5)
        val failed = PhraseSaveTransactionResult.Failed(PhraseSaveFailureReason.StorageVerificationFailed)
        val next = PhraseComposerController.applyTransactionSaveResult(state, failed, english)
        assertEquals(PhraseComposerMode.SaveConfirmation, next.mode)
        assertNull(next.savedMapping)
        assertNotNull(next.errorMessage)
    }

    @Test
    fun mappingsStateUpdatesFromVerifiedStoredData() {
        val result = createCustom("Runtime refresh phrase", CustomPhraseEngine.CaregiverPhraseCategory.Medical)
        assertTrue(result is PhraseSaveTransactionResult.Success)
        val runtime = baseMappings() + CustomPhraseRepository.loadCustomMappings()
        assertTrue(runtime.any { it.isCustom && it.customPhrase == "Runtime refresh phrase" })
    }

    @Test
    fun newlySavedPhraseAppearsImmediatelyInSelectedCategory() {
        val result = createCustom("Post save visible", CustomPhraseEngine.CaregiverPhraseCategory.Medical)
        assertTrue(result is PhraseSaveTransactionResult.Success)
        val success = result as PhraseSaveTransactionResult.Success
        val runtime = baseMappings() + CustomPhraseRepository.loadCustomMappings()
        val context = GuidedCatalogContext(
            caregiverCustomPhrases = CustomPhraseEngine.toCatalogEntries(runtime.filter { it.isCustom })
        )
        val page = GuidedVocabularyCatalog.categoryAt(
            GuidedVocabularyCategory.Medical.ordinal,
            PreferredLanguage.English,
            english,
            context
        )!!
        assertTrue(page.entries.any { it.phrase == "Post save visible" })
        assertTrue(success.runtimeVisible)
    }

    @Test
    fun returnToCommunicationOpensDestinationCategoryPage() {
        val result = createCustom("Navigate back phrase", CustomPhraseEngine.CaregiverPhraseCategory.Medical)
        assertTrue(result is PhraseSaveTransactionResult.Success)
        val success = result as PhraseSaveTransactionResult.Success
        val nav = GuidedNavigationController.openCategoryAtPage(
            GuidedNavigationState(),
            GuidedVocabularyCategory.Medical.ordinal,
            success.phrasePageIndex
        )
        assertEquals(GuidedVocabularyCategory.Medical.ordinal, nav.categoryIndex)
        assertEquals(success.phrasePageIndex, nav.phrasePageIndex)
    }

    @Test
    fun savedPhraseSurvivesAppRestartReload() {
        val result = createCustom("Restart survivor", CustomPhraseEngine.CaregiverPhraseCategory.Family)
        assertTrue(result is PhraseSaveTransactionResult.Success)
        val reloaded = CustomPhraseRepository.loadCustomMappings()
        assertEquals(1, reloaded.size)
        assertEquals("Restart survivor", reloaded.first().customPhrase)
    }

    @Test
    fun exactSequenceSurvivesRestart() {
        val result = createCustom("Sequence survivor", CustomPhraseEngine.CaregiverPhraseCategory.Conversation)
        assertTrue(result is PhraseSaveTransactionResult.Success)
        val mapping = (result as PhraseSaveTransactionResult.Success).mapping
        val reloaded = CustomPhraseRepository.loadCustomMappings().first()
        assertEquals(mapping.left, reloaded.left)
        assertEquals(mapping.right, reloaded.right)
    }

    @Test
    fun vocabularyListsAllCustomPhrases() {
        createCustom("Alpha phrase", CustomPhraseEngine.CaregiverPhraseCategory.Medical)
        createCustom("Beta phrase", CustomPhraseEngine.CaregiverPhraseCategory.Family)
        val listed = CustomPhraseRepository.listCustomPhrases(baseMappings() + CustomPhraseRepository.loadCustomMappings())
        assertEquals(2, listed.size)
    }

    @Test
    fun builtInPhrasesAreNotManageableCustomPhrases() {
        val listed = CustomPhraseRepository.listCustomPhrases(baseMappings())
        assertTrue(listed.isEmpty())
        assertTrue(baseMappings().none { it.isCustom })
    }

    @Test
    fun editUpdatesPhraseTextImmediately() {
        val created = createCustom("Old text", CustomPhraseEngine.CaregiverPhraseCategory.Medical)
        val mapping = (created as PhraseSaveTransactionResult.Success).mapping
        val identity = CustomPhraseIdentity.from(mapping)
        val runtime = baseMappings() + CustomPhraseRepository.loadCustomMappings()
        val updated = CustomPhraseRepository.updatePhraseText(
            identity = identity,
            rawPhrase = "New text",
            existingMappings = runtime,
            uiStrings = english
        )
        assertTrue(updated is PhraseManagementResult.Success)
        val stored = CustomPhraseRepository.loadCustomMappings().first()
        assertEquals("New text", stored.customPhrase)
    }

    @Test
    fun editSurvivesRestart() {
        val created = createCustom("Persist edit", CustomPhraseEngine.CaregiverPhraseCategory.BasicNeeds)
        val identity = CustomPhraseIdentity.from((created as PhraseSaveTransactionResult.Success).mapping)
        val runtime = baseMappings() + CustomPhraseRepository.loadCustomMappings()
        CustomPhraseRepository.updatePhraseText(identity, "Persist edit updated", runtime, uiStrings = english)
        assertEquals("Persist edit updated", CustomPhraseRepository.loadCustomMappings().first().customPhrase)
    }

    @Test
    fun editBlocksDuplicates() {
        createCustom("First phrase", CustomPhraseEngine.CaregiverPhraseCategory.Medical)
        val second = createCustom("Second unique", CustomPhraseEngine.CaregiverPhraseCategory.Medical)
        val identity = CustomPhraseIdentity.from((second as PhraseSaveTransactionResult.Success).mapping)
        val runtime = baseMappings() + CustomPhraseRepository.loadCustomMappings()
        val result = CustomPhraseRepository.updatePhraseText(
            identity = identity,
            rawPhrase = "First phrase",
            existingMappings = runtime,
            uiStrings = english
        )
        assertTrue(result is PhraseManagementResult.Failed)
        assertEquals(PhraseSaveFailureReason.Duplicate, (result as PhraseManagementResult.Failed).reason)
    }

    @Test
    fun editDoesNotMatchPhraseAgainstItself() {
        val created = createCustom("Keep same", CustomPhraseEngine.CaregiverPhraseCategory.Family)
        val identity = CustomPhraseIdentity.from((created as PhraseSaveTransactionResult.Success).mapping)
        val runtime = baseMappings() + CustomPhraseRepository.loadCustomMappings()
        val result = CustomPhraseRepository.updatePhraseText(
            identity = identity,
            rawPhrase = "Keep same",
            existingMappings = runtime,
            uiStrings = english
        )
        assertTrue(result is PhraseManagementResult.Success)
    }

    @Test
    fun moveRemovesPhraseFromOldCategory() {
        createCustom("Move me", CustomPhraseEngine.CaregiverPhraseCategory.Medical)
        val identity = CustomPhraseIdentity.from(CustomPhraseRepository.loadCustomMappings().first())
        val runtime = baseMappings() + CustomPhraseRepository.loadCustomMappings()
        val moved = CustomPhraseRepository.movePhrase(
            identity = identity,
            targetCategory = CustomPhraseEngine.CaregiverPhraseCategory.Family,
            existingMappings = runtime,
            uiStrings = english
        )
        assertTrue(moved is PhraseManagementResult.Success)
        val context = GuidedCatalogContext(
            caregiverCustomPhrases = CustomPhraseEngine.toCatalogEntries(CustomPhraseRepository.loadCustomMappings())
        )
        val medical = GuidedVocabularyCatalog.categoryAt(
            GuidedVocabularyCategory.Medical.ordinal,
            PreferredLanguage.English,
            english,
            context
        )!!
        assertFalse(medical.entries.any { it.phrase == "Move me" })
    }

    @Test
    fun moveAddsPhraseToNewCategory() {
        createCustom("Move target", CustomPhraseEngine.CaregiverPhraseCategory.Medical)
        val identity = CustomPhraseIdentity.from(CustomPhraseRepository.loadCustomMappings().first())
        val runtime = baseMappings() + CustomPhraseRepository.loadCustomMappings()
        CustomPhraseRepository.movePhrase(
            identity = identity,
            targetCategory = CustomPhraseEngine.CaregiverPhraseCategory.Family,
            existingMappings = runtime,
            uiStrings = english
        )
        val context = GuidedCatalogContext(
            caregiverCustomPhrases = CustomPhraseEngine.toCatalogEntries(CustomPhraseRepository.loadCustomMappings())
        )
        val family = GuidedVocabularyCatalog.categoryAt(
            GuidedVocabularyCategory.Family.ordinal,
            PreferredLanguage.English,
            english,
            context
        )!!
        assertTrue(family.entries.any { it.phrase == "Move target" })
    }

    @Test
    fun movePreservesSequenceWhenSafe() {
        val created = createCustom("Sequence keep", CustomPhraseEngine.CaregiverPhraseCategory.Medical)
        val original = (created as PhraseSaveTransactionResult.Success).mapping
        val identity = CustomPhraseIdentity.from(original)
        val runtime = baseMappings() + CustomPhraseRepository.loadCustomMappings()
        val moved = CustomPhraseRepository.movePhrase(
            identity = identity,
            targetCategory = CustomPhraseEngine.CaregiverPhraseCategory.Family,
            existingMappings = runtime,
            uiStrings = english
        ) as PhraseManagementResult.Success
        assertEquals(original.left to original.right, moved.mapping.left to moved.mapping.right)
    }

    @Test
    fun moveReallocatesWhenSequenceConflicts() {
        createCustom("Family occupant", CustomPhraseEngine.CaregiverPhraseCategory.Family)
        val familyMapping = CustomPhraseRepository.loadCustomMappings().first()
        val familySeq = familyMapping.left to familyMapping.right
        createCustom("Medical mover", CustomPhraseEngine.CaregiverPhraseCategory.Medical)
        val medicalMapping = CustomPhraseRepository.loadCustomMappings()
            .first { it.customPhrase == "Medical mover" }
            .copy(left = familySeq.first, right = familySeq.second)
        CustomPhraseRepository.writeCustomMappings(
            listOf(familyMapping, medicalMapping)
        )
        val identity = CustomPhraseIdentity.from(medicalMapping)
        val runtime = baseMappings() + CustomPhraseRepository.loadCustomMappings()
        val moved = CustomPhraseRepository.movePhrase(
            identity = identity,
            targetCategory = CustomPhraseEngine.CaregiverPhraseCategory.Family,
            existingMappings = runtime,
            uiStrings = english
        )
        assertTrue(moved is PhraseManagementResult.Success)
        val finalMapping = (moved as PhraseManagementResult.Success).mapping
        assertFalse(finalMapping.left == familySeq.first && finalMapping.right == familySeq.second)
    }

    @Test
    fun deleteRequiresConfirmationUiPresent() {
        val source = readSource("app/src/main/java/com/idworx/lisa/PhraseManagementUi.kt")
        assertTrue(source.contains("PhraseManagementScreen.DeleteConfirm"))
        assertTrue(source.contains("phraseManagementDeleteConfirmTitle"))
    }

    @Test
    fun deleteRemovesExactlyOneMapping() {
        createCustom("Delete one", CustomPhraseEngine.CaregiverPhraseCategory.Medical)
        createCustom("Keep one", CustomPhraseEngine.CaregiverPhraseCategory.Medical)
        val toDelete = CustomPhraseRepository.loadCustomMappings()
            .first { it.customPhrase == "Delete one" }
        val identity = CustomPhraseIdentity.from(toDelete)
        val runtime = baseMappings() + CustomPhraseRepository.loadCustomMappings()
        val result = CustomPhraseRepository.deletePhrase(identity, runtime)
        assertTrue(result is PhraseManagementResult.Success)
        assertEquals(1, CustomPhraseRepository.loadCustomMappings().size)
        assertEquals("Keep one", CustomPhraseRepository.loadCustomMappings().first().customPhrase)
    }

    @Test
    fun deleteSurvivesRestart() {
        createCustom("Delete restart", CustomPhraseEngine.CaregiverPhraseCategory.BasicNeeds)
        val identity = CustomPhraseIdentity.from(CustomPhraseRepository.loadCustomMappings().first())
        CustomPhraseRepository.deletePhrase(identity, baseMappings() + CustomPhraseRepository.loadCustomMappings())
        assertTrue(CustomPhraseRepository.loadCustomMappings().isEmpty())
    }

    @Test
    fun builtInPhrasesCannotBeEditedMovedOrDeleted() {
        val builtIn = baseMappings().first()
        val identity = CustomPhraseIdentity(builtIn.left, builtIn.right, builtIn.phrase)
        val runtime = baseMappings()
        assertTrue(CustomPhraseRepository.updatePhraseText(identity, "Hack", runtime, uiStrings = english)
            is PhraseManagementResult.Failed)
        assertTrue(CustomPhraseRepository.movePhrase(
            identity,
            CustomPhraseEngine.CaregiverPhraseCategory.Medical,
            runtime,
            uiStrings = english
        ) is PhraseManagementResult.Failed)
        assertTrue(CustomPhraseRepository.deletePhrase(identity, runtime) is PhraseManagementResult.Failed)
    }

    @Test
    fun saveErrorsPreserveComposerState() {
        val state = saveConfirmationState("Hold my text", sequence = 1 to 5)
        val failed = PhraseSaveTransactionResult.Failed(PhraseSaveFailureReason.StorageWriteFailed)
        val next = PhraseComposerController.applyTransactionSaveResult(state, failed, english)
        assertEquals("Hold my text", next.phraseText)
        assertEquals(CustomPhraseEngine.CaregiverPhraseCategory.Medical, next.selectedCategory)
        assertEquals(1 to 5, next.pendingAllocatedSequence)
    }

    @Test
    fun storageAuditDetectsMalformedOrDuplicateEntries() {
        CustomPhraseRepository.testStorage!!["custom_maps"] =
            "1,1|Duplicate|Medical\n1,2|Duplicate|Medical\nbadline\n2,3||Medical\n"
        val runtime = baseMappings() + CustomPhraseRepository.loadCustomMappings()
        val findings = CustomPhraseRepository.auditStoredMappings(runtime, uiStrings = english)
        assertTrue(findings.any { it.detail.contains("Duplicate", ignoreCase = true) })
        assertTrue(findings.any { it.detail.contains("empty phrase", ignoreCase = true) })
    }

    @Test
    fun customPhraseOnPaginatedMedicalPageUsesNonZeroPageIndex() {
        val mappings = baseMappings().toMutableList()
        val category = CustomPhraseEngine.CaregiverPhraseCategory.Medical
        val sequence = CustomPhraseEngine.allocateSequence(category, mappings)!!
        val result = CustomPhraseRepository.createPhrase(
            rawPhrase = "Paginated medical phrase",
            category = category,
            allocatedSequence = sequence,
            existingMappings = mappings,
            uiStrings = english
        ) as PhraseSaveTransactionResult.Success
        if (result.phrasePageIndex > 0) {
            val pageZero = GuidedNavigationController.openCategoryDirectly(
                GuidedNavigationState(),
                GuidedVocabularyCategory.Medical.ordinal
            )
            val pageTarget = GuidedNavigationController.openCategoryAtPage(
                GuidedNavigationState(),
                GuidedVocabularyCategory.Medical.ordinal,
                result.phrasePageIndex
            )
            assertEquals(0, pageZero.phrasePageIndex)
            assertTrue(pageTarget.phrasePageIndex > 0)
        } else {
            val builtInCount = GuidedVocabularyCatalog.categoryAt(
                GuidedVocabularyCategory.Medical.ordinal,
                PreferredLanguage.English,
                english
            )!!.entries.size
            assertTrue(builtInCount <= GuidedVocabularyCatalog.DEFAULT_VISIBLE_ENTRY_CAP)
        }
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
