package com.idworx.lisa

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** RC7D.15 — Phrase Management blink scroll, Back L2 R2, Emergency L6 R0. */
class Rc7D_15PhraseManagementScrollAndBlinkNavigationTest {

    private val english = LisaUiStrings.forLanguage(PreferredLanguage.English)

    private fun customPhrase(index: Int, category: CustomPhraseEngine.CaregiverPhraseCategory) =
        WinkMapping(
            left = 5,
            right = (index % 3) + 1,
            vocabularyId = "Phrase $index",
            isCustom = true,
            customPhrase = "Phrase $index",
            caregiverCategory = category
        )

    private fun manyPhrases(count: Int): List<WinkMapping> =
        (0 until count).map { index ->
            val slot = GuidedPageSequences.slotAt(index % GuidedPageSequences.slots.size)
            WinkMapping(
                left = slot.first,
                right = slot.second,
                vocabularyId = "Unique phrase $index",
                isCustom = true,
                customPhrase = "Unique phrase $index",
                caregiverCategory = CustomPhraseEngine.selectableCategories[
                    index % CustomPhraseEngine.selectableCategories.size
                ]
            )
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

    @Test
    fun scrollDownAvailableWhenMorePhrasesBelow() {
        val phrases = manyPhrases(PhraseManagementController.PAGE_SIZE + 2)
        assertTrue(PhraseManagementController.canScrollDown(0, phrases.size))
        assertFalse(PhraseManagementController.canScrollUp(0))
        val commands = PhraseManagementController.listCommandEntries(
            PhraseManagementUiState(listPageIndex = 0),
            phrases.size,
            english
        )
        assertTrue(commands.any { it.action == PhraseManagementController.PhraseManagementNavAction.ScrollDown })
        assertFalse(commands.any { it.action == PhraseManagementController.PhraseManagementNavAction.ScrollUp })
        assertEquals(
            GuidedModeNavigation.NEXT_LEFT to GuidedModeNavigation.NEXT_RIGHT,
            commands.first { it.action == PhraseManagementController.PhraseManagementNavAction.ScrollDown }
                .let { it.left to it.right }
        )
    }

    @Test
    fun scrollDownRevealsLaterPhrases() {
        val phrases = manyPhrases(PhraseManagementController.PAGE_SIZE + 3)
        val page0 = PhraseManagementController.visiblePhrases(phrases, 0)
        val after = PhraseManagementController.scrollDown(
            PhraseManagementUiState(listPageIndex = 0),
            phrases.size
        )
        val page1 = PhraseManagementController.visiblePhrases(phrases, after.listPageIndex)
        assertEquals(1, after.listPageIndex)
        assertEquals(phrases[0].customPhrase, page0.first().customPhrase)
        assertEquals(
            phrases[PhraseManagementController.PAGE_SIZE].customPhrase,
            page1.first().customPhrase
        )
    }

    @Test
    fun scrollUpAvailableAfterScrollingDown() {
        val phrases = manyPhrases(PhraseManagementController.PAGE_SIZE + 1)
        val down = PhraseManagementController.scrollDown(
            PhraseManagementUiState(listPageIndex = 0),
            phrases.size
        )
        assertTrue(PhraseManagementController.canScrollUp(down.listPageIndex))
        val commands = PhraseManagementController.listCommandEntries(down, phrases.size, english)
        assertTrue(commands.any { it.action == PhraseManagementController.PhraseManagementNavAction.ScrollUp })
        assertEquals(
            GuidedModeNavigation.PREVIOUS_LEFT to GuidedModeNavigation.PREVIOUS_RIGHT,
            commands.first { it.action == PhraseManagementController.PhraseManagementNavAction.ScrollUp }
                .let { it.left to it.right }
        )
    }

    @Test
    fun cannotScrollBeforeFirstOrPastLast() {
        val phrases = manyPhrases(PhraseManagementController.PAGE_SIZE + 1)
        val atStart = PhraseManagementController.scrollUp(PhraseManagementUiState(listPageIndex = 0))
        assertEquals(0, atStart.listPageIndex)
        val lastPage = PhraseManagementController.pageCount(phrases.size) - 1
        val atEnd = PhraseManagementController.scrollDown(
            PhraseManagementUiState(listPageIndex = lastPage),
            phrases.size
        )
        assertEquals(lastPage, atEnd.listPageIndex)
        assertFalse(PhraseManagementController.canScrollDown(lastPage, phrases.size))
    }

    @Test
    fun repeatedScrollMovesDeterministicallyAndReachesAllPhrases() {
        val phrases = manyPhrases(PhraseManagementController.PAGE_SIZE * 3)
        var state = PhraseManagementUiState()
        val seen = mutableSetOf<String>()
        repeat(PhraseManagementController.pageCount(phrases.size)) {
            PhraseManagementController.visiblePhrases(phrases, state.listPageIndex)
                .forEach { seen.add(it.customPhrase.orEmpty()) }
            if (PhraseManagementController.canScrollDown(state.listPageIndex, phrases.size)) {
                state = PhraseManagementController.scrollDown(state, phrases.size)
            }
        }
        assertEquals(phrases.size, seen.size)
        assertTrue(phrases.all { it.customPhrase in seen })
    }

    @Test
    fun noActiveScrollCommandsWhenAllFitOnOnePage() {
        val phrases = manyPhrases(2)
        val commands = PhraseManagementController.listCommandEntries(
            PhraseManagementUiState(),
            phrases.size,
            english
        )
        assertFalse(commands.any { it.action == PhraseManagementController.PhraseManagementNavAction.ScrollUp })
        assertFalse(commands.any { it.action == PhraseManagementController.PhraseManagementNavAction.ScrollDown })
        assertTrue(commands.any { it.action == PhraseManagementController.PhraseManagementNavAction.Back })
    }

    @Test
    fun backCommandUsesCanonicalL2R2() {
        val back = PhraseManagementController.listCommandEntries(
            PhraseManagementUiState(),
            phraseCount = 0,
            uiStrings = english
        ).first { it.action == PhraseManagementController.PhraseManagementNavAction.Back }
        assertEquals(GuidedModeNavigation.BACK_LEFT, back.left)
        assertEquals(GuidedModeNavigation.BACK_RIGHT, back.right)
        assertEquals("Back", back.label)
        assertEquals("L2 R2", formatWinkSequenceShort(back.left, back.right))
    }

    @Test
    fun afterDeleteClampsToValidPage() {
        val remaining = PhraseManagementController.PAGE_SIZE
        val state = PhraseManagementUiState(listPageIndex = 5)
        val next = PhraseManagementController.afterPhraseListChanged(state, remaining)
        assertEquals(0, next.listPageIndex)
    }

    @Test
    fun pagePreservedInUiStateAcrossDetailsNavigationModel() {
        val state = PhraseManagementUiState(listPageIndex = 2)
        val opened = state.copy(
            screen = PhraseManagementScreen.Details,
            selectedIdentity = CustomPhraseIdentity(5, 1, "kept")
        )
        assertEquals(2, opened.listPageIndex)
        val back = opened.copy(screen = PhraseManagementScreen.List, selectedIdentity = null)
        assertEquals(2, back.listPageIndex)
    }

    @Test
    fun emergencyShowsCanonicalL6R0() {
        assertEquals("Emergency", english.guidedEmergencyNavTitle)
        assertEquals("L6 R0", formatWinkSequenceShort(EMERGENCY_LEFT_WINKS, EMERGENCY_RIGHT_WINKS))
        val ui = readSource("app/src/main/java/com/idworx/lisa/PhraseManagementUi.kt")
        assertTrue(ui.contains("ComposerEmergencyCommandCard"))
        // One Emergency card call site per screen that needs it (List / Details / DeleteConfirm).
        val callSites = Regex("ComposerEmergencyCommandCard\\(").findAll(ui).count()
        assertTrue(callSites in 1..3)
        assertFalse(ui.contains("ComposerEmergencyCommandCard", ignoreCase = false) && ui.contains("LazyColumn"))
    }

    @Test
    fun msgaOwnsPhraseManagementModeAndDoesNotLeakToGuidedScroll() {
        val ctx = LisaGestureContext(
            activePanel = LisaPanel.VocabularyTraining,
            guidedOverlayActive = true,
            guidedScreenMode = GuidedOverlayScreenMode.CategoryMenu,
            isAdjustingPreference = false,
            phraseComposerMode = null
        )
        assertEquals(LisaInteractionMode.PhraseManagement, ModeScopedGestureAuthority.activeMode(ctx))
        assertEquals(
            GestureRoutingTarget.PhraseManagement,
            ModeScopedGestureAuthority.routingTarget(
                ctx,
                GuidedModeNavigation.NEXT_LEFT,
                GuidedModeNavigation.NEXT_RIGHT
            )
        )
        assertEquals(
            GestureRoutingTarget.PhraseManagement,
            ModeScopedGestureAuthority.routingTarget(
                ctx,
                GuidedModeNavigation.BACK_LEFT,
                GuidedModeNavigation.BACK_RIGHT
            )
        )
        assertEquals(
            GestureRoutingTarget.Emergency,
            ModeScopedGestureAuthority.routingTarget(
                ctx,
                EMERGENCY_LEFT_WINKS,
                EMERGENCY_RIGHT_WINKS
            )
        )
    }

    @Test
    fun mainActivityWiresBlinkScrollAndBack() {
        val main = readSource("app/src/main/java/com/idworx/lisa/MainActivity.kt")
        assertTrue(main.contains("handlePhraseManagementSequence"))
        assertTrue(main.contains("scrollPhraseManagementList"))
        assertTrue(main.contains("GestureRoutingTarget.PhraseManagement"))
        assertTrue(main.contains("backFromActivePanel()"))
        assertTrue(main.contains("phraseManagementOpenedFromCategories"))
    }

    @Test
    fun phraseManagementStillOutsideChooseCategoryAndKeepsEyeKeyboard() {
        val labels = PhraseComposerController.visibleEntries(
            PhraseComposerState(mode = PhraseComposerMode.DestinationCategorySelection),
            english
        ).map { it.label }
        assertFalse(labels.contains("Phrase Management"))
        val composerUi = readSource("app/src/main/java/com/idworx/lisa/PhraseComposerUi.kt")
        assertTrue(composerUi.contains("BottomAlignedEyeKeyboard"))
        assertFalse(composerUi.contains("OutlinedTextField"))
        val managementUi = readSource("app/src/main/java/com/idworx/lisa/PhraseManagementUi.kt")
        assertFalse(managementUi.contains("OutlinedTextField"))
        assertFalse(managementUi.contains("LocalSoftwareKeyboardController"))
        assertFalse(managementUi.contains("LazyColumn"))
    }

    @Test
    fun visiblePhraseSlotsUseCanonicalPageSequences() {
        val phrases = manyPhrases(3)
        val slots = PhraseManagementController.visiblePhraseSelectionSlots(phrases, 0)
        assertEquals(3, slots.size)
        assertEquals(GuidedPageSequences.slotAt(0), slots[0].second)
        assertEquals(GuidedPageSequences.slotAt(1), slots[1].second)
    }
}
