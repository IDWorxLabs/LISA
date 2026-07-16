package com.idworx.lisa

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * RC7D.28 — Fully eye-controlled Communication workspace Menu.
 */
class Rc7D_28EyeControlledMainMenuTest {

    private val english = LisaUiStrings.forLanguage(PreferredLanguage.English)
    private val openLeft = GuidedModeNavigation.OPEN_MAIN_MENU_LEFT_WINKS
    private val openRight = GuidedModeNavigation.OPEN_MAIN_MENU_RIGHT_WINKS

    private fun readSource(relativePath: String): String {
        val normalized = relativePath.replace('/', java.io.File.separatorChar)
        val roots = listOfNotNull(
            java.io.File(System.getProperty("user.dir") ?: "."),
            java.io.File(System.getProperty("user.dir") ?: ".").parentFile
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

    private fun openMenu(): MainMenuNavigationState = MainMenuController.open()

    private fun process(
        left: Int,
        right: Int,
        state: MainMenuNavigationState,
        viewportHeightPx: Int = 400,
        maxScrollPx: Int = 800
    ): MainMenuSequenceResult =
        MainMenuController.processSequence(left, right, state, viewportHeightPx, maxScrollPx)

    private fun navigate(result: MainMenuSequenceResult): MainMenuNavigationState =
        (result as MainMenuSequenceResult.Navigate).newState

    // ------------------------------------------------------------------ A. Open Menu gesture

    @Test
    fun openMenuConstantsExistAndMatchAuditChoice() {
        assertEquals(4, openLeft)
        assertEquals(6, openRight)
        assertTrue(GuidedModeNavigation.isOpenMainMenuSequence(openLeft, openRight))
        assertEquals(
            "L4 R6",
            formatWinkSequenceShort(openLeft, openRight)
        )
    }

    @Test
    fun openMenuSequenceIsGloballyUniqueAmongRegistries() {
        val seq = openLeft to openRight
        assertFalse(GuidedModeNavigation.isGlobalNavigationSequence(openLeft, openRight))
        assertFalse(GuidedModeNavigation.isAdjustSettingsEntrySequence(openLeft, openRight))
        assertFalse(isEmergencySequence(openLeft, openRight))
        assertFalse(seq in GuidedPageSequences.slots)
        assertFalse(seq in GuidedPageSequences.extendedSlots)
        assertTrue(seq in GuidedPageSequences.forbiddenForVocabulary)
        assertNull(GuidedCategoryShortcuts.categoryIndexForGesture(openLeft, openRight))
        assertNull(LisaSystemLanguage.resolveGlobalCommand(openLeft, openRight))
        assertNull(LisaSystemLanguage.resolveQuickControlCommand(openLeft, openRight))
        assertTrue(
            ModeScopedGestureAuthority.phraseComposerCommandSequences.values.none { it == seq }
        )
        assertTrue(openLeft < EMERGENCY_LEFT_WINKS)
    }

    @Test
    fun menuButtonDisplaysSequenceFromCanonicalConstants() {
        val expected = formatWinkSequenceShort(openLeft, openRight)
        assertEquals("L4 R6", expected)
        assertEquals(
            expected,
            MainMenuProductionUiAuthority.openMenuSequenceLabel()
        )
        val ui = readSource("app/src/main/java/com/idworx/lisa/LisaAccessibilityUi.kt")
        assertTrue(ui.contains("MainMenuProductionUiAuthority.openMenuSequenceLabel()"))
        assertTrue(ui.contains("subtitle = MainMenuProductionUiAuthority.openMenuSequenceLabel()"))
        val main = readSource("app/src/main/java/com/idworx/lisa/MainActivity.kt")
        assertTrue(main.contains("openMainMenu()"))
        assertTrue(main.contains("onMenuClick = { toggleMenuPanel() }"))
    }

    @Test
    fun entryGestureInertWhileMenuAlreadyOpen() {
        val menu = openMenu()
        assertTrue(
            process(openLeft, openRight, menu) is MainMenuSequenceResult.Unmatched
        )
    }

    // ------------------------------------------------------------------ B. Menu model

    @Test
    fun canonicalDestinationModelCoversAllMenuItems() {
        assertEquals(8, MainMenuCatalog.destinationCount)
        assertEquals(MainMenuDestination.CommunicationProfile, MainMenuCatalog.destinations[0])
        assertEquals(LisaPanel.MyCommunication, MainMenuDestination.CommunicationProfile.panel)
        assertEquals(LisaPanel.VocabularyTraining, MainMenuDestination.PhraseManagement.panel)
        assertEquals(LisaPanel.Voice, MainMenuDestination.Voice.panel)
        assertEquals(LisaPanel.Settings, MainMenuDestination.Settings.panel)
        assertEquals(LisaPanel.AboutLisa, MainMenuDestination.AboutLisa.panel)
        assertEquals(LisaPanel.PrivacyPolicy, MainMenuDestination.PrivacyPolicy.panel)
        assertEquals(LisaPanel.Feedback, MainMenuDestination.Feedback.panel)
        assertEquals(LisaPanel.ReleaseNotes, MainMenuDestination.ReleaseNotes.panel)
    }

    @Test
    fun listEntriesNumberOnlySelectableDestinations() {
        val entries = MainMenuCatalog.listEntries()
        val headers = entries.filterIsInstance<MainMenuListEntry.SectionHeader>()
        val destinations = entries.filterIsInstance<MainMenuListEntry.Destination>()
        assertEquals(3, headers.size)
        assertEquals(8, destinations.size)
        assertEquals(1, destinations.first().selectionIndex)
        assertEquals(8, destinations.last().selectionIndex)
    }

    // ------------------------------------------------------------------ C. Item movement

    @Test
    fun initialSelectionIsFirstDestination() {
        val menu = openMenu()
        assertEquals(0, menu.selectionIndex)
        assertEquals(MainMenuDestination.CommunicationProfile, menu.selectedDestination)
    }

    @Test
    fun moveDownAndUpAdvanceOneDestination() {
        var menu = openMenu()
        menu = navigate(process(GuidedModeNavigation.NEXT_LEFT, GuidedModeNavigation.NEXT_RIGHT, menu))
        assertEquals(1, menu.selectionIndex)
        assertEquals(MainMenuDestination.PhraseManagement, menu.selectedDestination)
        menu = navigate(process(GuidedModeNavigation.PREVIOUS_LEFT, GuidedModeNavigation.PREVIOUS_RIGHT, menu))
        assertEquals(0, menu.selectionIndex)
    }

    @Test
    fun selectionClampsAtTopAndBottom() {
        var menu = openMenu()
        menu = navigate(process(GuidedModeNavigation.PREVIOUS_LEFT, GuidedModeNavigation.PREVIOUS_RIGHT, menu))
        assertEquals(0, menu.selectionIndex)
        repeat(MainMenuCatalog.destinationCount) {
            menu = navigate(process(GuidedModeNavigation.NEXT_LEFT, GuidedModeNavigation.NEXT_RIGHT, menu))
        }
        assertEquals(MainMenuCatalog.destinationCount - 1, menu.selectionIndex)
    }

    @Test
    fun movementDoesNotOpenDestinations() {
        val moved = navigate(
            process(GuidedModeNavigation.NEXT_LEFT, GuidedModeNavigation.NEXT_RIGHT, openMenu())
        )
        assertTrue(moved.isOpen)
        assertEquals(MainMenuDestination.PhraseManagement, moved.selectedDestination)
    }

    // ------------------------------------------------------------------ D. Selection / opening

    @Test
    fun selectOpensSelectedDestination() {
        var menu = navigate(
            process(GuidedModeNavigation.NEXT_LEFT, GuidedModeNavigation.NEXT_RIGHT, openMenu())
        )
        val result = process(GuidedModeNavigation.SELECT_LEFT, GuidedModeNavigation.SELECT_RIGHT, menu)
        assertTrue(result is MainMenuSequenceResult.OpenDestination)
        val open = result as MainMenuSequenceResult.OpenDestination
        assertEquals(MainMenuDestination.PhraseManagement, open.destination)
        assertFalse(open.newState.isOpen)
    }

    @Test
    fun touchAndBlinkShareDestinationPanelMapping() {
        assertEquals(LisaPanel.Settings, MainMenuDestination.Settings.panel)
        assertEquals(
            MainMenuDestination.Settings,
            MainMenuDestination.fromPanel(LisaPanel.Settings)
        )
    }

    // ------------------------------------------------------------------ E. Page navigation

    @Test
    fun pageCountDerivesFromMeasuredContent() {
        assertEquals(1, CategoryViewportPaging.pageCount(viewportHeightPx = 400, maxScrollPx = 0))
        assertEquals(3, CategoryViewportPaging.pageCount(viewportHeightPx = 400, maxScrollPx = 800))
    }

    @Test
    fun pageAnchorsUseZeroAndMaxScroll() {
        assertEquals(0, CategoryViewportPaging.pageAnchorOffsetPx(0, 400, 800))
        assertEquals(800, CategoryViewportPaging.pageAnchorOffsetPx(2, 400, 800))
    }

    @Test
    fun previousAndNextPageJumpDirectlyWithoutItemSteps() {
        var menu = openMenu().copy(viewportPage = 1, viewportPageCount = 3)
        val prev = navigate(
            process(
                GuidedModeNavigation.PREVIOUS_CATEGORY_PAGE_LEFT,
                GuidedModeNavigation.PREVIOUS_CATEGORY_PAGE_RIGHT,
                menu,
                viewportHeightPx = 400,
                maxScrollPx = 800
            )
        )
        assertEquals(0, prev.viewportPage)
        assertEquals(0, prev.scrollRequestPx)
        assertFalse(prev.revealSelection)
        val next = navigate(
            process(
                GuidedModeNavigation.NEXT_CATEGORY_PAGE_LEFT,
                GuidedModeNavigation.NEXT_CATEGORY_PAGE_RIGHT,
                prev,
                viewportHeightPx = 400,
                maxScrollPx = 800
            )
        )
        assertEquals(1, next.viewportPage)
        assertEquals(400, next.scrollRequestPx)
    }

    @Test
    fun pageNavigationDisabledAtEdges() {
        val top = openMenu().copy(viewportPage = 0, viewportPageCount = 3)
        val stayedTop = navigate(
            process(
                GuidedModeNavigation.PREVIOUS_CATEGORY_PAGE_LEFT,
                GuidedModeNavigation.PREVIOUS_CATEGORY_PAGE_RIGHT,
                top,
                viewportHeightPx = 400,
                maxScrollPx = 800
            )
        )
        assertEquals(0, stayedTop.viewportPage)

        val bottom = openMenu().copy(viewportPage = 2, viewportPageCount = 3)
        val stayedBottom = navigate(
            process(
                GuidedModeNavigation.NEXT_CATEGORY_PAGE_LEFT,
                GuidedModeNavigation.NEXT_CATEGORY_PAGE_RIGHT,
                bottom,
                viewportHeightPx = 400,
                maxScrollPx = 800
            )
        )
        assertEquals(2, stayedBottom.viewportPage)
    }

    // ------------------------------------------------------------------ F. Back and Emergency

    @Test
    fun backClosesMenuWithoutOpeningDestination() {
        val result = process(GuidedModeNavigation.BACK_LEFT, GuidedModeNavigation.BACK_RIGHT, openMenu())
        assertTrue(result is MainMenuSequenceResult.CloseMenu)
        assertFalse((result as MainMenuSequenceResult.CloseMenu).newState.isOpen)
    }

    @Test
    fun emergencyHasPriorityOverMenuRouting() {
        val ctx = LisaGestureContext(
            activePanel = LisaPanel.Menu,
            guidedOverlayActive = true,
            guidedScreenMode = GuidedOverlayScreenMode.Vocabulary,
            isAdjustingPreference = false,
            phraseComposerMode = null
        )
        assertEquals(
            GestureRoutingTarget.Emergency,
            ModeScopedGestureAuthority.routingTarget(ctx, EMERGENCY_LEFT_WINKS, EMERGENCY_RIGHT_WINKS)
        )
        assertEquals(
            GestureRoutingTarget.MainMenu,
            ModeScopedGestureAuthority.routingTarget(ctx, GuidedModeNavigation.BACK_LEFT, GuidedModeNavigation.BACK_RIGHT)
        )
    }

    @Test
    fun openMenuRoutesBeforeGuidedOverlayWhenClosed() {
        val ctx = LisaGestureContext(
            activePanel = LisaPanel.None,
            guidedOverlayActive = true,
            guidedScreenMode = GuidedOverlayScreenMode.Vocabulary,
            isAdjustingPreference = false,
            phraseComposerMode = null
        )
        assertEquals(
            GestureRoutingTarget.MainMenu,
            ModeScopedGestureAuthority.routingTarget(ctx, openLeft, openRight)
        )
        assertNotEquals(
            GestureRoutingTarget.GuidedOverlay,
            ModeScopedGestureAuthority.routingTarget(ctx, openLeft, openRight)
        )
    }

    // ------------------------------------------------------------------ G. UI

    @Test
    fun menuUiShowsIndicatorsAndNavigationControls() {
        assertEquals("Item 1 / 8", english.mainMenuItemIndicator(1, 8))
        assertEquals("Page 1 / 2", english.mainMenuPageIndicator(1, 2))
        val ui = readSource("app/src/main/java/com/idworx/lisa/LisaAccessibilityUi.kt")
        assertTrue(ui.contains("mainMenuItemIndicator"))
        assertTrue(ui.contains("mainMenuPageIndicator"))
        assertTrue(ui.contains("MainMenuNavigationControls"))
        assertTrue(ui.contains("MainMenuDestinationRow"))
        assertTrue(ui.contains("DECREASE_VALUE_LEFT").not())
        assertTrue(ui.contains("PREVIOUS_LEFT"))
        assertTrue(ui.contains("PREVIOUS_CATEGORY_PAGE_LEFT"))
        assertTrue(ui.contains("SELECT_LEFT"))
        assertTrue(ui.contains("BACK_LEFT"))
        assertTrue(ui.contains("EMERGENCY_LEFT_WINKS"))
    }

    @Test
    fun menuButtonRemainsBesideClearOutsideMenuMode() {
        val ui = readSource("app/src/main/java/com/idworx/lisa/LisaAccessibilityUi.kt")
        assertTrue(ui.contains("MainMenuProductionUiAuthority.showCommunicationClearAndRepeat(activePanel)"))
        assertTrue(ui.contains("showCommunicationClearAndRepeat"))
        val chromeStart = ui.indexOf("showWorkspaceBottomChrome(phraseComposerActive)")
        val chromeBlock = ui.substring(chromeStart, chromeStart + 2500)
        assertTrue(chromeBlock.contains("onMenuClick"))
        assertTrue(chromeBlock.contains("uiStrings.reset"))
        assertTrue(chromeBlock.contains("mainMenuActive"))
    }

    // ------------------------------------------------------------------ H. Regression

    @Test
    fun adjustSettingsAndCategoryPagingSequencesUnchanged() {
        assertEquals(5 to 5, GuidedModeNavigation.ADJUST_SETTINGS_ENTRY_LEFT to GuidedModeNavigation.ADJUST_SETTINGS_ENTRY_RIGHT)
        assertEquals(4 to 0, GuidedModeNavigation.PREVIOUS_CATEGORY_PAGE_LEFT to GuidedModeNavigation.PREVIOUS_CATEGORY_PAGE_RIGHT)
        assertEquals(0 to 4, GuidedModeNavigation.NEXT_CATEGORY_PAGE_LEFT to GuidedModeNavigation.NEXT_CATEGORY_PAGE_RIGHT)
        assertEquals(GuidedVocabularyCategory.AdjustSettings, GuidedVocabularyCategory.ordered[8])
    }

    @Test
    fun noAndroidSystemKeyboardIntroduced() {
        val ui = readSource("app/src/main/java/com/idworx/lisa/LisaAccessibilityUi.kt")
        assertFalse(ui.contains("KeyboardOptions"))
    }
}
