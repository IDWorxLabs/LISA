package com.idworx.lisa

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** RC7D.12 — Emergency L6 R0 label, SharedPreferences verify-with-context, eye-controlled edit/delete. */
class Rc7D_12EmergencyLabelSaveVerifyAndEditDeleteTest {

    private val english = LisaUiStrings.forLanguage(PreferredLanguage.English)

    @Before
    fun setUp() {
        CustomPhraseRepository.testStorage = mutableMapOf()
    }

    @After
    fun tearDown() {
        CustomPhraseRepository.testStorage = null
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

    // --- Emergency label ---

    @Test
    fun composerEmergencyCardRendersEmergencyAndL6R0() {
        val card = readSource("app/src/main/java/com/idworx/lisa/ComposerCommandGrid.kt")
        val start = card.indexOf("fun ComposerEmergencyCommandCard")
        assertTrue(start >= 0)
        val body = card.substring(start, start + 1200)
        assertTrue(body.contains("guidedEmergencyNavTitle"))
        assertTrue(body.contains("EMERGENCY_LEFT_WINKS"))
        assertTrue(body.contains("EMERGENCY_RIGHT_WINKS"))
        assertTrue(body.contains("formatWinkSequenceShort"))
        assertEquals("L6 R0", formatWinkSequenceShort(EMERGENCY_LEFT_WINKS, EMERGENCY_RIGHT_WINKS))
        assertEquals("Emergency", english.guidedEmergencyNavTitle)
    }

    @Test
    fun saveConfirmationRendersOneEmergencyCardOnly() {
        val grid = readSource("app/src/main/java/com/idworx/lisa/ComposerCommandGrid.kt")
        val confirmGrid = grid.substring(
            grid.indexOf("fun ComposerConfirmationActionGrid"),
            grid.indexOf("fun ComposerCommandGridRow")
        )
        assertEquals(1, Regex("ComposerEmergencyCommandCard\\(").findAll(confirmGrid).count())
        val ui = readSource("app/src/main/java/com/idworx/lisa/PhraseComposerUi.kt")
        assertTrue(ui.contains("PhraseComposerMode.SaveConfirmation -> {"))
        assertTrue(ui.contains("ComposerConfirmationActionGrid("))
    }

    @Test
    fun editAndDeleteConfirmationUseSingleEmergencyCard() {
        val grid = readSource("app/src/main/java/com/idworx/lisa/ComposerCommandGrid.kt")
        val confirmGrid = grid.substring(
            grid.indexOf("fun ComposerConfirmationActionGrid"),
            grid.indexOf("@Composable\nprivate fun ComposerCommandGridRow").let { if (it < 0) grid.indexOf("fun ComposerCommandGridRow") else it }
        )
        assertEquals(1, Regex("ComposerEmergencyCommandCard\\(").findAll(confirmGrid).count())
        val ui = readSource("app/src/main/java/com/idworx/lisa/PhraseComposerUi.kt")
        assertTrue(ui.contains("primaryActionId = PhraseComposerActionId.ConfirmDelete"))
        assertTrue(ui.contains("PhraseComposerMode.ConfirmDelete"))
    }

    @Test
    fun emergencyL6R0RemainsRoutedWhileComposerVisible() {
        val context = LisaGestureContext(
            activePanel = LisaPanel.PhraseEditor,
            guidedOverlayActive = false,
            guidedScreenMode = null,
            isAdjustingPreference = false,
            phraseComposerMode = PhraseComposerMode.Keyboard
        )
        assertEquals(
            GestureRoutingTarget.Emergency,
            ModeScopedGestureAuthority.routingTarget(context, EMERGENCY_LEFT_WINKS, EMERGENCY_RIGHT_WINKS)
        )
        val confirmContext = context.copy(phraseComposerMode = PhraseComposerMode.ConfirmDelete)
        assertEquals(
            GestureRoutingTarget.Emergency,
            ModeScopedGestureAuthority.routingTarget(confirmContext, EMERGENCY_LEFT_WINKS, EMERGENCY_RIGHT_WINKS)
        )
    }

    // --- Save verify with same storage ---

    @Test
    fun persistAndVerifyPassesSameContextToReadBack() {
        val repo = readSource("app/src/main/java/com/idworx/lisa/CustomPhraseRepository.kt")
        assertTrue(repo.contains("verifyPersistedMapping(expectedMapping, context)"))
        assertTrue(repo.contains("applicationContext"))
        assertFalse(
            repo.contains(
                "if (!verifyPersistedMapping(expectedMapping)) {"
            )
        )
    }

    @Test
    fun validPhraseSavesAfterConfirmL1R1() {
        val category = CustomPhraseEngine.CaregiverPhraseCategory.Medical
        val sequence = CustomPhraseEngine.allocateSequence(category, defaultLanguageMappings())!!
        val result = CustomPhraseRepository.createPhrase(
            rawPhrase = "My stomach hurts",
            category = category,
            allocatedSequence = sequence,
            existingMappings = defaultLanguageMappings(),
            uiStrings = english
        )
        assertTrue(result is PhraseSaveTransactionResult.Success)
        val success = result as PhraseSaveTransactionResult.Success
        assertTrue(success.persisted)
        assertTrue(CustomPhraseRepository.verifyPersistedMapping(success.mapping))
        assertEquals("Saved", english.phraseCreatedSuccess)
        assertEquals("Saved", english.phraseComposerSuccessTitle)
    }

    @Test
    fun successfulSaveDoesNotEmitVerificationFailed() {
        val category = CustomPhraseEngine.CaregiverPhraseCategory.BasicNeeds
        val sequence = CustomPhraseEngine.allocateSequence(category, defaultLanguageMappings())!!
        val result = CustomPhraseRepository.createPhrase(
            rawPhrase = "Need more water",
            category = category,
            allocatedSequence = sequence,
            existingMappings = defaultLanguageMappings(),
            uiStrings = english
        )
        assertTrue(result is PhraseSaveTransactionResult.Success)
        assertFalse(result is PhraseSaveTransactionResult.Failed)
    }

    @Test
    fun saveDoesNotDependOnSystemIme() {
        val composerUi = readSource("app/src/main/java/com/idworx/lisa/PhraseComposerUi.kt")
        assertFalse(composerUi.contains("OutlinedTextField"))
        assertFalse(composerUi.contains("LocalSoftwareKeyboardController"))
        assertFalse(composerUi.contains("FocusRequester"))
        assertTrue(composerUi.contains("BottomAlignedEyeKeyboard"))
        val management = readSource("app/src/main/java/com/idworx/lisa/PhraseManagementUi.kt")
        assertFalse(management.contains("OutlinedTextField"))
    }

    @Test
    fun repeatedConfirmCreatesOnlyOnePhrase() {
        val category = CustomPhraseEngine.CaregiverPhraseCategory.Family
        val sequence = CustomPhraseEngine.allocateSequence(category, defaultLanguageMappings())!!
        val first = CustomPhraseRepository.createPhrase(
            rawPhrase = "Call my sister",
            category = category,
            allocatedSequence = sequence,
            existingMappings = defaultLanguageMappings(),
            uiStrings = english
        )
        assertTrue(first is PhraseSaveTransactionResult.Success)
        val second = CustomPhraseRepository.createPhrase(
            rawPhrase = "Call my sister",
            category = category,
            allocatedSequence = sequence,
            existingMappings = defaultLanguageMappings() + CustomPhraseRepository.loadCustomMappings(),
            uiStrings = english
        )
        assertTrue(second is PhraseSaveTransactionResult.Failed)
        assertEquals(PhraseSaveFailureReason.Duplicate, (second as PhraseSaveTransactionResult.Failed).reason)
        assertEquals(1, CustomPhraseRepository.loadCustomMappings().size)
    }

    // --- Edit / Delete ---

    @Test
    fun customPhraseExposesEditAndDeleteThroughManagement() {
        val management = readSource("app/src/main/java/com/idworx/lisa/PhraseManagementUi.kt")
        val controller = readSource("app/src/main/java/com/idworx/lisa/PhraseManagementController.kt")
        assertTrue(management.contains("detailsActionEntries") || controller.contains("phraseManagementEditPhrase"))
        assertTrue(controller.contains("phraseManagementEditPhrase"))
        assertTrue(controller.contains("phraseManagementDeletePhrase"))
        val main = readSource("app/src/main/java/com/idworx/lisa/MainActivity.kt")
        assertTrue(main.contains("openComposerForEdit"))
        assertTrue(main.contains("openComposerForDelete"))
        assertTrue(main.contains("editEntryState"))
        assertTrue(main.contains("deleteConfirmState"))
    }

    @Test
    fun builtInPhrasesCannotBeEditedOrDeleted() {
        val builtIn = defaultLanguageMappings().first()
        val identity = CustomPhraseIdentity(builtIn.left, builtIn.right, builtIn.phrase)
        assertTrue(
            CustomPhraseRepository.updatePhraseText(identity, "Hack", defaultLanguageMappings(), uiStrings = english)
                is PhraseManagementResult.Failed
        )
        assertTrue(
            CustomPhraseRepository.deletePhrase(identity, defaultLanguageMappings())
                is PhraseManagementResult.Failed
        )
    }

    @Test
    fun editComposerPreloadsPhraseAndKeepsKeyboard() {
        val mapping = WinkMapping(
            left = 5,
            right = 1,
            vocabularyId = "Need pain relief",
            isCustom = true,
            customPhrase = "Need pain relief",
            caregiverCategory = CustomPhraseEngine.CaregiverPhraseCategory.Medical
        )
        val state = PhraseComposerController.editEntryState(mapping)
        assertEquals(PhraseComposerMode.Keyboard, state.mode)
        assertEquals("Need pain relief", state.phraseText)
        assertEquals(CustomPhraseIdentity.from(mapping), state.editingIdentity)
        assertTrue(state.isEditing())
        assertEquals(english.phraseManagementEditTitle, PhraseComposerController.screenTitle(state, english))
        assertTrue(readSource("app/src/main/java/com/idworx/lisa/PhraseComposerUi.kt").contains("BottomAlignedEyeKeyboard"))
    }

    @Test
    fun saveChangesUpdatesSameIdentityWithoutDuplicate() {
        val created = CustomPhraseRepository.createPhrase(
            rawPhrase = "Original edit phrase",
            category = CustomPhraseEngine.CaregiverPhraseCategory.Medical,
            allocatedSequence = CustomPhraseEngine.allocateSequence(
                CustomPhraseEngine.CaregiverPhraseCategory.Medical,
                defaultLanguageMappings()
            )!!,
            existingMappings = defaultLanguageMappings(),
            uiStrings = english
        ) as PhraseSaveTransactionResult.Success
        val identity = CustomPhraseIdentity.from(created.mapping)
        val runtime = defaultLanguageMappings() + CustomPhraseRepository.loadCustomMappings()
        val updated = CustomPhraseRepository.updatePhraseText(
            identity = identity,
            rawPhrase = "Updated edit phrase",
            existingMappings = runtime,
            uiStrings = english
        )
        assertTrue(updated is PhraseManagementResult.Success)
        val stored = CustomPhraseRepository.loadCustomMappings()
        assertEquals(1, stored.size)
        assertEquals("Updated edit phrase", stored.first().customPhrase)
        assertEquals(english.phraseUpdatedSuccess, "Phrase updated")
    }

    @Test
    fun deleteRequiresConfirmAndRemovesStableIdentity() {
        val created = CustomPhraseRepository.createPhrase(
            rawPhrase = "Delete me please",
            category = CustomPhraseEngine.CaregiverPhraseCategory.Conversation,
            allocatedSequence = CustomPhraseEngine.allocateSequence(
                CustomPhraseEngine.CaregiverPhraseCategory.Conversation,
                defaultLanguageMappings()
            )!!,
            existingMappings = defaultLanguageMappings(),
            uiStrings = english
        ) as PhraseSaveTransactionResult.Success
        val mapping = created.mapping
        val deleteState = PhraseComposerController.deleteConfirmState(mapping)
        assertEquals(PhraseComposerMode.ConfirmDelete, deleteState.mode)
        val confirm = PhraseComposerController.commandPanelEntries(deleteState, english)
            .first { it.actionId == PhraseComposerActionId.ConfirmDelete }
        assertEquals(GuidedModeNavigation.SELECT_LEFT, confirm.left)
        assertEquals(GuidedModeNavigation.SELECT_RIGHT, confirm.right)
        val result = PhraseComposerController.processSequence(
            confirm.left,
            confirm.right,
            deleteState,
            english
        )
        assertTrue(result is PhraseComposerSequenceResult.Delete)
        assertEquals(CustomPhraseIdentity.from(mapping), (result as PhraseComposerSequenceResult.Delete).identity)

        val deleted = CustomPhraseRepository.deletePhrase(
            CustomPhraseIdentity.from(mapping),
            defaultLanguageMappings() + CustomPhraseRepository.loadCustomMappings()
        )
        assertTrue(deleted is PhraseManagementResult.Success)
        assertTrue(CustomPhraseRepository.loadCustomMappings().isEmpty())
        assertEquals("Phrase deleted", english.phraseDeletedSuccess)
    }

    @Test
    fun backCancelsDeleteWithoutPersistenceChange() {
        val created = CustomPhraseRepository.createPhrase(
            rawPhrase = "Keep me",
            category = CustomPhraseEngine.CaregiverPhraseCategory.Family,
            allocatedSequence = CustomPhraseEngine.allocateSequence(
                CustomPhraseEngine.CaregiverPhraseCategory.Family,
                defaultLanguageMappings()
            )!!,
            existingMappings = defaultLanguageMappings(),
            uiStrings = english
        ) as PhraseSaveTransactionResult.Success
        val deleteState = PhraseComposerController.deleteConfirmState(created.mapping)
        val back = PhraseComposerController.processSequence(
            GuidedModeNavigation.BACK_LEFT,
            GuidedModeNavigation.BACK_RIGHT,
            deleteState,
            english
        )
        assertTrue(back is PhraseComposerSequenceResult.ExitToPreviousPanel)
        assertEquals(1, CustomPhraseRepository.loadCustomMappings().size)
    }

    @Test
    fun deletingOnlyCustomPhraseLeavesValidCatalogState() {
        val created = CustomPhraseRepository.createPhrase(
            rawPhrase = "Only custom",
            category = CustomPhraseEngine.CaregiverPhraseCategory.Medical,
            allocatedSequence = CustomPhraseEngine.allocateSequence(
                CustomPhraseEngine.CaregiverPhraseCategory.Medical,
                defaultLanguageMappings()
            )!!,
            existingMappings = defaultLanguageMappings(),
            uiStrings = english
        ) as PhraseSaveTransactionResult.Success
        CustomPhraseRepository.deletePhrase(
            CustomPhraseIdentity.from(created.mapping),
            defaultLanguageMappings() + CustomPhraseRepository.loadCustomMappings()
        )
        val page = GuidedVocabularyCatalog.categoryAt(
            GuidedVocabularyCategory.Medical.ordinal,
            PreferredLanguage.English,
            english
        )
        assertNotNull(page)
        assertTrue(page!!.entries.none { it.phrase == "Only custom" })
    }

    @Test
    fun noSystemKeyboardCodeReintroduced() {
        val main = readSource("app/src/main/java/com/idworx/lisa/MainActivity.kt")
        assertFalse(main.contains("LocalSoftwareKeyboardController"))
        assertFalse(main.contains("onPhraseComposerPhraseTextChanged"))
        assertFalse(main.contains("PhraseComposerMode.Saving"))
        assertTrue(main.contains("applicationContext"))
    }
}
