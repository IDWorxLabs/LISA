package com.idworx.lisa

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * RC7D.17A — Phrase Management fixed controls must materialize in the real runtime UI host,
 * not only exist as unrendered controller/source strings.
 */
class Rc7D_17PhraseManagementFixedNavigationControlsTest {

    private val english = LisaUiStrings.forLanguage(PreferredLanguage.English)

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

    private fun commands(page: Int, phraseCount: Int) =
        PhraseManagementController.listCommandEntries(
            PhraseManagementUiState(listPageIndex = page),
            phraseCount,
            english
        )

    private fun command(
        action: PhraseManagementController.PhraseManagementNavAction,
        page: Int = 0,
        phraseCount: Int = PhraseManagementController.PAGE_SIZE + 2
    ) = commands(page, phraseCount).first { it.action == action }

    // --- RUNTIME HOST PATH ---

    @Test
    fun vocabularyTrainingOccupiesMainContentSlot() {
        assertTrue(PhraseManagementController.occupiesMainContentSlot(LisaPanel.VocabularyTraining))
        assertFalse(PhraseManagementController.occupiesMainContentSlot(LisaPanel.Menu))
        assertFalse(PhraseManagementController.occupiesMainContentSlot(LisaPanel.None))
    }

    @Test
    fun accessibilityUiHostsPhraseManagementInWeightedMainSlot() {
        val ui = readSource("app/src/main/java/com/idworx/lisa/LisaAccessibilityUi.kt")
        val phraseHost = ui.indexOf("if (phraseManagementActive)")
        val panelCall = ui.indexOf("VocabularyManagementPanel(", phraseHost)
        val composer = ui.indexOf("EyeControlledPhraseComposerOverlay(", phraseHost)
        val bottomMenus = ui.indexOf("if (activePanel != LisaPanel.None && !phraseManagementActive)")
        assertTrue(phraseHost >= 0)
        assertTrue(panelCall > phraseHost)
        assertTrue(composer > panelCall)
        assertTrue(bottomMenus > composer)
        // Main slot uses weight(1f); bottom branch must not render a second copy.
        val hostSlice = ui.substring(phraseHost, composer)
        assertTrue(hostSlice.contains(".weight(1f)"))
        assertTrue(hostSlice.contains("VocabularyManagementPanel("))
        assertEquals(1, Regex("VocabularyManagementPanel\\(").findAll(ui).count())
        assertTrue(ui.contains("LisaPanel.VocabularyTraining -> Unit"))
    }

    @Test
    fun guidedOverlayHiddenWhilePhraseManagementActive() {
        assertFalse(
            PhraseManagementController.showGuidedVocabularyOverlayAlongsideManagement(
                phraseComposerActive = false,
                phraseManagementActive = true
            )
        )
        assertTrue(
            PhraseManagementController.showSharedBlinkStatusHeader(phraseComposerActive = false)
        )
        val ui = readSource("app/src/main/java/com/idworx/lisa/LisaAccessibilityUi.kt")
        assertTrue(ui.contains("showSharedBlinkStatusHeader"))
        assertTrue(ui.contains("showGuidedVocabularyOverlayAlongsideManagement"))
    }

    @Test
    fun listPanelUsesBoundedPhraseAreaAboveFixedCommands() {
        val ui = readSource("app/src/main/java/com/idworx/lisa/PhraseManagementUi.kt")
        val listStart = ui.indexOf("private fun VocabularyPhraseListPanel")
        val stripStart = ui.indexOf("private fun PhraseManagementCommandStrip")
        val list = ui.substring(listStart, stripStart)
        assertTrue(list.contains("fillMaxSize()"))
        assertTrue(list.contains(".weight(1f)"))
        assertTrue(list.contains("PhraseManagementCommandStrip("))
        assertTrue(list.contains("ComposerEmergencyCommandCard("))
        val weightIdx = list.indexOf(".weight(1f)")
        val stripIdx = list.indexOf("PhraseManagementCommandStrip(")
        val emergencyIdx = list.indexOf("ComposerEmergencyCommandCard(")
        assertTrue(weightIdx in 0 until stripIdx)
        assertTrue(stripIdx in 0 until emergencyIdx)
        // Commands must not live inside the weighted phrase Box's forEach.
        val slotsIdx = list.indexOf("slots.forEach")
        assertTrue(slotsIdx in 0 until stripIdx)
        assertTrue(list.indexOf("verticalScroll", slotsIdx - 80) < slotsIdx)
    }

    // --- VISIBLE LABELS / SEQUENCES ---

    @Test
    fun rendersScrollUpL2R0() {
        val scrollUp = command(PhraseManagementController.PhraseManagementNavAction.ScrollUp)
        assertEquals("Scroll Up", scrollUp.label)
        assertEquals("L2 R0", formatWinkSequenceShort(scrollUp.left, scrollUp.right))
    }

    @Test
    fun rendersScrollDownL0R2() {
        val scrollDown = command(PhraseManagementController.PhraseManagementNavAction.ScrollDown)
        assertEquals("Scroll Down", scrollDown.label)
        assertEquals("L0 R2", formatWinkSequenceShort(scrollDown.left, scrollDown.right))
    }

    @Test
    fun rendersBackToCommunicationL2R2() {
        val back = command(PhraseManagementController.PhraseManagementNavAction.Back)
        assertEquals("Back to Communication", back.label)
        assertEquals("L2 R2", formatWinkSequenceShort(back.left, back.right))
    }

    @Test
    fun rendersEmergencyL6R0() {
        assertEquals("Emergency", english.guidedEmergencyNavTitle)
        assertEquals("L6 R0", formatWinkSequenceShort(EMERGENCY_LEFT_WINKS, EMERGENCY_RIGHT_WINKS))
        val ui = readSource("app/src/main/java/com/idworx/lisa/PhraseManagementUi.kt")
        val list = ui.substring(
            ui.indexOf("private fun VocabularyPhraseListPanel"),
            ui.indexOf("private fun PhraseManagementCommandStrip")
        )
        assertTrue(list.contains("ComposerEmergencyCommandCard("))
    }

    @Test
    fun controlsPresentOnFirstAndLastPageIncludingDisabled() {
        val phrases = manyPhrases(PhraseManagementController.PAGE_SIZE + 2)
        val first = commands(0, phrases.size)
        val lastPage = PhraseManagementController.pageCount(phrases.size) - 1
        val last = commands(lastPage, phrases.size)
        assertEquals(3, first.size)
        assertEquals(3, last.size)
        assertFalse(first.first { it.action == PhraseManagementController.PhraseManagementNavAction.ScrollUp }.enabled)
        assertTrue(first.first { it.action == PhraseManagementController.PhraseManagementNavAction.ScrollDown }.enabled)
        assertTrue(last.first { it.action == PhraseManagementController.PhraseManagementNavAction.ScrollUp }.enabled)
        assertFalse(last.first { it.action == PhraseManagementController.PhraseManagementNavAction.ScrollDown }.enabled)
        first.forEach {
            assertTrue(it.label.isNotBlank())
            assertTrue(formatWinkSequenceShort(it.left, it.right).startsWith("L"))
        }
        last.forEach {
            assertTrue(it.label.isNotBlank())
            assertTrue(formatWinkSequenceShort(it.left, it.right).startsWith("L"))
        }
    }

    // --- PAGE SIZE / SMALL SCREEN ---

    @Test
    fun pageSizeIsThreeForCompactPhysicalScreens() {
        assertEquals(3, PhraseManagementController.PAGE_SIZE)
        assertTrue(PhraseManagementController.compactLayoutFits(screenHeightDp = 640))
        assertTrue(PhraseManagementController.compactLayoutFits(screenHeightDp = 720))
    }

    @Test
    fun pageIndicatorUsesCompactPageSize() {
        val phrases = manyPhrases(PhraseManagementController.PAGE_SIZE * 2 + 1)
        assertEquals("1 / 3", PhraseManagementController.pageIndicatorLabel(0, phrases.size))
        assertEquals("2 / 3", PhraseManagementController.pageIndicatorLabel(1, phrases.size))
        assertEquals("3 / 3", PhraseManagementController.pageIndicatorLabel(2, phrases.size))
    }

    // --- TOUCH / BLINK HANDLERS ---

    @Test
    fun touchScrollChangesPage() {
        val phrases = manyPhrases(PhraseManagementController.PAGE_SIZE + 1)
        val down = PhraseManagementController.scrollDown(PhraseManagementUiState(), phrases.size)
        assertEquals(1, down.listPageIndex)
        val up = PhraseManagementController.scrollUp(down)
        assertEquals(0, up.listPageIndex)
    }

    @Test
    fun touchAndBlinkShareHandlersAndBackExitsToCommunicationWorkspace() {
        val main = readSource("app/src/main/java/com/idworx/lisa/MainActivity.kt")
        assertTrue(main.contains("onPhraseManagementScrollUp = { scrollPhraseManagementList(up = true) }"))
        assertTrue(main.contains("onPhraseManagementScrollDown = { scrollPhraseManagementList(up = false) }"))
        assertTrue(main.contains("scrollPhraseManagementList(up = true)"))
        assertTrue(main.contains("scrollPhraseManagementList(up = false)"))
        assertTrue(main.contains("PhraseManagementExitDestination.CommunicationWorkspace"))
        assertTrue(main.contains("exitPhraseManagement("))
        assertTrue(main.contains("closeCategoryMenu("))
    }

    @Test
    fun blinkSequencesRouteWhilePhraseManagementActive() {
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
                ctx, GuidedModeNavigation.PREVIOUS_LEFT, GuidedModeNavigation.PREVIOUS_RIGHT
            )
        )
        assertEquals(
            GestureRoutingTarget.PhraseManagement,
            ModeScopedGestureAuthority.routingTarget(
                ctx, GuidedModeNavigation.NEXT_LEFT, GuidedModeNavigation.NEXT_RIGHT
            )
        )
        assertEquals(
            GestureRoutingTarget.PhraseManagement,
            ModeScopedGestureAuthority.routingTarget(
                ctx, GuidedModeNavigation.BACK_LEFT, GuidedModeNavigation.BACK_RIGHT
            )
        )
        assertEquals(
            GestureRoutingTarget.Emergency,
            ModeScopedGestureAuthority.routingTarget(
                ctx, EMERGENCY_LEFT_WINKS, EMERGENCY_RIGHT_WINKS
            )
        )
    }

    // --- REGRESSION ---

    @Test
    fun speakAndOpenDetailsLabelsRemain() {
        assertTrue(english.phraseManagementSpeakSequence("L2 R1").contains("Speak"))
        assertTrue(english.phraseManagementOpenDetailsSequence("L2 R1").contains("Open details"))
        val ui = readSource("app/src/main/java/com/idworx/lisa/PhraseManagementUi.kt")
        assertTrue(ui.contains("phraseManagementSpeakSequence"))
        assertTrue(ui.contains("phraseManagementOpenDetailsSequence"))
    }

    @Test
    fun detailsEditMoveDeleteRemain() {
        val details = PhraseManagementController.detailsActionEntries(english)
        assertEquals(3, details.size)
        assertEquals(PhraseManagementController.PhraseDetailsAction.Edit, details[0].action)
        assertEquals(PhraseManagementController.PhraseDetailsAction.Move, details[1].action)
        assertEquals(PhraseManagementController.PhraseDetailsAction.Delete, details[2].action)
    }

    @Test
    fun commandStripUsesTwoRowGridForNarrowScreens() {
        val ui = readSource("app/src/main/java/com/idworx/lisa/PhraseManagementUi.kt")
        val strip = ui.substring(ui.indexOf("private fun PhraseManagementCommandStrip"))
        assertTrue(strip.contains("Two-row grid"))
        assertTrue(strip.contains("ScrollUp"))
        assertTrue(strip.contains("ScrollDown"))
        assertTrue(strip.contains("Back"))
    }

    @Test
    fun eyeKeyboardIntact() {
        val composerUi = readSource("app/src/main/java/com/idworx/lisa/PhraseComposerUi.kt")
        assertTrue(composerUi.contains("BottomAlignedEyeKeyboard"))
        assertFalse(composerUi.contains("OutlinedTextField"))
    }
}
