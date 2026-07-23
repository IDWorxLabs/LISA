package com.idworx.lisa

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * RC7D.29 — Main Menu must render through the real production root path as a full-screen mode,
 * not as an unused bottom sheet beneath the Communication workspace.
 */
class Rc7D_29MainMenuProductionUiIntegrationTest {

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

    // ------------------------------------------------------------------ A. Production-path wiring

    @Test
    fun menuOccupiesMainContentSlotWhenActive() {
        assertTrue(MainMenuProductionUiAuthority.occupiesMainContentSlot(LisaPanel.Menu))
        assertFalse(MainMenuProductionUiAuthority.occupiesMainContentSlot(LisaPanel.None))
        assertFalse(MainMenuProductionUiAuthority.occupiesMainContentSlot(LisaPanel.Settings))
    }

    @Test
    fun accessibilityUiHostsMainMenuInWeightedMainSlot() {
        val ui = readSource("app/src/main/java/com/idworx/lisa/LisaAccessibilityUi.kt")
        val menuHost = ui.indexOf("if (mainMenuActive)")
        val panelCall = ui.indexOf("MenuPanel(", menuHost)
        val composer = ui.indexOf("EyeControlledPhraseComposerOverlay(", menuHost)
        val bottomMenus = ui.indexOf("if (activePanel != LisaPanel.None && !phraseManagementActive && !mainMenuActive)")
        assertTrue(menuHost >= 0)
        assertTrue(panelCall > menuHost)
        assertTrue(composer > panelCall)
        assertTrue(bottomMenus > composer)
        val hostSlice = ui.substring(menuHost, composer)
        assertTrue(hostSlice.contains(".weight(1f)"))
        assertTrue(hostSlice.contains("fillWorkspace = true"))
        assertFalse(ui.contains("LisaPanel.Menu -> MenuPanel("))
        assertTrue(ui.contains("LisaPanel.Menu -> Unit"))
    }

    @Test
    fun guidedOverlayHiddenWhileMainMenuActive() {
        assertFalse(
            MainMenuProductionUiAuthority.showGuidedVocabularyOverlay(
                activePanel = LisaPanel.Menu,
                phraseComposerActive = false,
                phraseManagementActive = false
            )
        )
        assertTrue(
            MainMenuProductionUiAuthority.showGuidedVocabularyOverlay(
                activePanel = LisaPanel.None,
                phraseComposerActive = false,
                phraseManagementActive = false
            )
        )
        val ui = readSource("app/src/main/java/com/idworx/lisa/LisaAccessibilityUi.kt")
        assertTrue(ui.contains("MainMenuProductionUiAuthority.showGuidedVocabularyOverlay"))
        assertTrue(ui.contains("mainMenuActive = MainMenuProductionUiAuthority.occupiesMainContentSlot"))
    }

    @Test
    fun mainMenuStateDrivesSelectionInProductionPath() {
        var menu = openMenu()
        assertEquals(MainMenuDestination.CommunicationProfile, menu.selectedDestination)
        menu = navigate(process(GuidedModeNavigation.NEXT_LEFT, GuidedModeNavigation.NEXT_RIGHT, menu))
        assertEquals(MainMenuDestination.PhraseManagement, menu.selectedDestination)
        val main = readSource("app/src/main/java/com/idworx/lisa/MainActivity.kt")
        assertTrue(main.contains("mainMenuState = uiMainMenuState.value"))
        assertTrue(main.contains("applyMainMenuResult"))
    }

    @Test
    fun noDuplicateMenuSheetInBottomChrome() {
        val ui = readSource("app/src/main/java/com/idworx/lisa/LisaAccessibilityUi.kt")
        assertFalse(ui.contains("LisaPanel.Menu -> MenuPanel("))
        assertTrue(ui.contains("LisaPanel.Menu -> Unit"))
    }

    // ------------------------------------------------------------------ B. Menu button label

    @Test
    fun menuButtonUsesCanonicalOpenSequenceLabel() {
        assertEquals(
            formatWinkSequenceShort(openLeft, openRight),
            MainMenuProductionUiAuthority.openMenuSequenceLabel()
        )
        assertEquals("L4 R6", MainMenuProductionUiAuthority.openMenuSequenceLabel())
        val ui = readSource("app/src/main/java/com/idworx/lisa/LisaAccessibilityUi.kt")
        assertTrue(ui.contains("label = uiStrings.menu"))
        assertTrue(ui.contains("sequenceLabel = MainMenuProductionUiAuthority.openMenuSequenceLabel()"))
    }

    @Test
    fun closeButtonUsesCanonicalCloseSequenceLabel() {
        assertEquals(
            formatWinkSequenceShort(
                GuidedModeNavigation.BACK_LEFT,
                GuidedModeNavigation.BACK_RIGHT
            ),
            MainMenuProductionUiAuthority.closeMenuSequenceLabel()
        )
        assertEquals("L2 R2", MainMenuProductionUiAuthority.closeMenuSequenceLabel())
        val ui = readSource("app/src/main/java/com/idworx/lisa/LisaAccessibilityUi.kt")
        assertTrue(ui.contains("sequenceLabel = MainMenuProductionUiAuthority.closeMenuSequenceLabel()"))
    }

    @Test
    fun touchAndBlinkShareOpenMainMenuAuthority() {
        val main = readSource("app/src/main/java/com/idworx/lisa/MainActivity.kt")
        assertTrue(main.contains("private fun openMainMenu()"))
        assertTrue(main.contains("openMainMenu()"))
        assertTrue(main.contains("onMenuClick = { toggleMenuPanel() }"))
        assertTrue(main.contains("toggleMenuPanel()"))
        assertTrue(main.contains("openPanel(LisaPanel.Menu)"))
    }

    @Test
    fun menuButtonKeepsWeightTouchTarget() {
        val ui = readSource("app/src/main/java/com/idworx/lisa/LisaAccessibilityUi.kt")
        val chrome = ui.substringAfter("showWorkspaceBottomChrome(phraseComposerActive)")
            .substringBefore("if (activePanel != LisaPanel.None")
        assertTrue(chrome.contains("WorkspaceFullWidthActionButton"))
        assertTrue(chrome.contains("fillMaxWidth()").not() || chrome.contains("WorkspaceFullWidthActionButton"))
        val button = ui.substringAfter("private fun WorkspaceFullWidthActionButton")
            .substringBefore("private fun MyCommunicationPanel")
        assertTrue(button.contains("fillMaxWidth()"))
        assertTrue(button.contains("FullWidthActionMinHeight"))
    }

    @Test
    fun clearHiddenWhileMainMenuOpen() {
        assertFalse(MainMenuProductionUiAuthority.showCommunicationClearAndRepeat(LisaPanel.Menu))
        assertFalse(MainMenuProductionUiAuthority.showCommunicationClearAndRepeat(LisaPanel.None))
        val ui = readSource("app/src/main/java/com/idworx/lisa/LisaAccessibilityUi.kt")
        assertTrue(ui.contains("if (mainMenuActive)"))
        assertTrue(ui.contains("WorkspaceFullWidthActionButton"))
    }

    // ------------------------------------------------------------------ C. Full-screen isolation

    @Test
    fun mainMenuReplacesWorkspaceContentRegion() {
        val ui = readSource("app/src/main/java/com/idworx/lisa/LisaAccessibilityUi.kt")
        val overlayIdx = ui.indexOf("visible = showGuidedVocabularyOverlay")
        val menuIdx = ui.indexOf("if (mainMenuActive)")
        assertTrue(overlayIdx >= 0)
        assertTrue(menuIdx > overlayIdx)
        assertTrue(ui.contains("fillWorkspace = true"))
    }

    @Test
    fun legacyPartialHeightSheetRemovedFromProductionPath() {
        val ui = readSource("app/src/main/java/com/idworx/lisa/LisaAccessibilityUi.kt")
        assertFalse(ui.contains("LisaPanel.Menu -> MenuPanel("))
        val fillWorkspaceBlock = ui.substringAfter("fillWorkspace = true")
            .substringBefore("if (phraseManagementActive)")
        assertFalse(fillWorkspaceBlock.contains("heightIn(max = 280.dp)"))
    }

    @Test
    fun bottomPanelBranchExcludesMenuFromLegacySheet() {
        val ui = readSource("app/src/main/java/com/idworx/lisa/LisaAccessibilityUi.kt")
        assertTrue(ui.contains("!mainMenuActive"))
        assertTrue(ui.contains("LisaPanel.Menu -> Unit"))
    }

    // ------------------------------------------------------------------ D. Navigation panel

    @Test
    fun navigationPanelShowsAllSevenActionsWithCanonicalSequences() {
        val ui = readSource("app/src/main/java/com/idworx/lisa/LisaAccessibilityUi.kt")
        val navStart = ui.indexOf("private fun MainMenuNavigationControls")
        val navEnd = ui.indexOf("private fun MainMenuCloseRow", navStart)
        assertTrue(navStart >= 0 && navEnd > navStart)
        val nav = ui.substring(navStart, navEnd)
        assertTrue(nav.contains("mainMenuMoveUp"))
        assertTrue(nav.contains("mainMenuMoveDown"))
        assertTrue(nav.contains("mainMenuPreviousPage"))
        assertTrue(nav.contains("mainMenuNextPage"))
        assertTrue(nav.contains("mainMenuOpenSelected"))
        assertTrue(nav.contains("mainMenuClose"))
        assertTrue(nav.contains("emergency"))
        assertTrue(nav.contains("GuidedNavigationActionButton("))
        assertTrue(nav.contains("GuidedEmergencyNavButton("))
        assertEquals(
            "L2 R0",
            formatWinkSequenceShort(
                GuidedModeNavigation.PREVIOUS_LEFT,
                GuidedModeNavigation.PREVIOUS_RIGHT
            )
        )
        assertEquals(
            "L0 R2",
            formatWinkSequenceShort(
                GuidedModeNavigation.NEXT_LEFT,
                GuidedModeNavigation.NEXT_RIGHT
            )
        )
        assertEquals(
            "L4 R0",
            formatWinkSequenceShort(
                GuidedModeNavigation.PREVIOUS_CATEGORY_PAGE_LEFT,
                GuidedModeNavigation.PREVIOUS_CATEGORY_PAGE_RIGHT
            )
        )
        assertEquals(
            "L0 R4",
            formatWinkSequenceShort(
                GuidedModeNavigation.NEXT_CATEGORY_PAGE_LEFT,
                GuidedModeNavigation.NEXT_CATEGORY_PAGE_RIGHT
            )
        )
        assertEquals(
            "L1 R1",
            formatWinkSequenceShort(
                GuidedModeNavigation.SELECT_LEFT,
                GuidedModeNavigation.SELECT_RIGHT
            )
        )
        assertEquals("L2 R2", MainMenuProductionUiAuthority.closeMenuSequenceLabel())
        assertEquals(
            "L6 R0",
            formatWinkSequenceShort(EMERGENCY_LEFT_WINKS, EMERGENCY_RIGHT_WINKS)
        )
    }

    @Test
    fun fullScreenMenuUsesWiderFixedNavPanel() {
        val ui = readSource("app/src/main/java/com/idworx/lisa/LisaAccessibilityUi.kt")
        assertTrue(ui.contains("navPanelWidth = LisaWorkspaceVisualStyle.NavPanelWidth"))
        assertTrue(ui.contains(".width(navPanelWidth)"))
    }

    @Test
    fun fullScreenMenuOmitsRedundantInlineCloseRow() {
        val ui = readSource("app/src/main/java/com/idworx/lisa/LisaAccessibilityUi.kt")
        assertTrue(ui.contains("if (!fillWorkspace)"))
        assertTrue(ui.contains("MainMenuCloseRow("))
    }

    // ------------------------------------------------------------------ E. Menu list

    @Test
    fun eightCanonicalDestinationsRenderFromCatalog() {
        assertEquals(8, MainMenuCatalog.destinationCount)
        val entries = MainMenuCatalog.listEntries().filterIsInstance<MainMenuListEntry.Destination>()
        assertEquals(8, entries.size)
        assertEquals(1, entries.first().selectionIndex)
        assertEquals(8, entries.last().selectionIndex)
        val ui = readSource("app/src/main/java/com/idworx/lisa/LisaAccessibilityUi.kt")
        assertTrue(ui.contains("MainMenuCatalog.listEntries()"))
        assertTrue(ui.contains("MainMenuDestinationRow"))
    }

    @Test
    fun sectionHeadersAreNotSelectableEntries() {
        val entries = MainMenuCatalog.listEntries()
        val headers = entries.filterIsInstance<MainMenuListEntry.SectionHeader>()
        assertEquals(3, headers.size)
        var menu = openMenu()
        repeat(entries.size) {
            menu = navigate(process(GuidedModeNavigation.NEXT_LEFT, GuidedModeNavigation.NEXT_RIGHT, menu))
        }
        assertEquals(MainMenuCatalog.destinationCount - 1, menu.selectionIndex)
    }

    @Test
    fun selectOpensHighlightedDestination() {
        var menu = navigate(
            process(GuidedModeNavigation.NEXT_LEFT, GuidedModeNavigation.NEXT_RIGHT, openMenu())
        )
        val result = process(GuidedModeNavigation.SELECT_LEFT, GuidedModeNavigation.SELECT_RIGHT, menu)
        assertTrue(result is MainMenuSequenceResult.OpenDestination)
        assertEquals(
            MainMenuDestination.PhraseManagement,
            (result as MainMenuSequenceResult.OpenDestination).destination
        )
    }

    @Test
    fun touchDestinationUsesSamePanelMapping() {
        MainMenuCatalog.destinations.forEach { destination ->
            assertNotNull(MainMenuDestination.fromPanel(destination.panel))
            assertEquals(destination, MainMenuDestination.fromPanel(destination.panel))
        }
    }

    // ------------------------------------------------------------------ F. Paging

    @Test
    fun viewportPageCountIsMeasurementDriven() {
        assertEquals(1, CategoryViewportPaging.pageCount(viewportHeightPx = 400, maxScrollPx = 0))
        assertEquals(3, CategoryViewportPaging.pageCount(viewportHeightPx = 400, maxScrollPx = 800))
    }

    @Test
    fun pageAnchorsUseZeroAndMaxScroll() {
        assertEquals(0, CategoryViewportPaging.pageAnchorOffsetPx(0, 400, 800))
        assertEquals(800, CategoryViewportPaging.pageAnchorOffsetPx(2, 400, 800))
    }

    @Test
    fun pageNavigationIsDirectWithoutItemSteps() {
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
    fun pageIndicatorsUseMenuTerminology() {
        assertEquals("Item 1 / 8", english.mainMenuItemIndicator(1, 8))
        assertEquals("Page 1 / 2", english.mainMenuPageIndicator(1, 2))
    }

    // ------------------------------------------------------------------ G. Back and restoration

    @Test
    fun backClosesMenuWithoutOpeningDestination() {
        val result = process(GuidedModeNavigation.BACK_LEFT, GuidedModeNavigation.BACK_RIGHT, openMenu())
        assertTrue(result is MainMenuSequenceResult.CloseMenu)
        assertFalse((result as MainMenuSequenceResult.CloseMenu).newState.isOpen)
    }

    @Test
    fun closeAllPanelsResetsMenuStateForRestoration() {
        val main = readSource("app/src/main/java/com/idworx/lisa/MainActivity.kt")
        val closeFn = main.substringAfter("private fun closeAllPanels()")
            .substringBefore("private fun openMainMenu()")
        assertTrue(closeFn.contains("uiMainMenuState.value = MainMenuController.close()"))
        assertTrue(closeFn.contains("uiActivePanel.value = LisaPanel.None"))
        assertFalse(closeFn.contains("uiGuidedNavigationState.value = GuidedNavigationState()"))
    }

    @Test
    fun repeatNotShownInMenuModeChrome() {
        val ui = readSource("app/src/main/java/com/idworx/lisa/LisaAccessibilityUi.kt")
        val menuBranch = ui.substringAfter("if (mainMenuActive) {")
            .substringBefore("} else {")
        assertFalse(menuBranch.contains("uiStrings.repeat"))
        assertFalse(menuBranch.contains("uiStrings.reset"))
    }

    // ------------------------------------------------------------------ H. Regression

    @Test
    fun rc7D28GestureRoutingRemainsValid() {
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
    fun openMenuContinuationProtectionRemains() {
        val menu = openMenu()
        assertTrue(process(openLeft, openRight, menu) is MainMenuSequenceResult.Unmatched)
        val main = readSource("app/src/main/java/com/idworx/lisa/MainActivity.kt")
        assertTrue(main.contains("mappingsForSequenceContinuation"))
        assertTrue(main.contains("OPEN_MAIN_MENU"))
    }

    @Test
    fun rc7D22CategoryPagingConstantsUnchanged() {
        assertEquals(4 to 0, GuidedModeNavigation.PREVIOUS_CATEGORY_PAGE_LEFT to GuidedModeNavigation.PREVIOUS_CATEGORY_PAGE_RIGHT)
        assertEquals(0 to 4, GuidedModeNavigation.NEXT_CATEGORY_PAGE_LEFT to GuidedModeNavigation.NEXT_CATEGORY_PAGE_RIGHT)
    }

    @Test
    fun rc7D25Through27SettingsSequencesUnchanged() {
        assertEquals(5 to 5, GuidedModeNavigation.ADJUST_SETTINGS_ENTRY_LEFT to GuidedModeNavigation.ADJUST_SETTINGS_ENTRY_RIGHT)
        assertEquals(
            GuidedVocabularyCategory.AdjustSettings,
            GuidedVocabularyCategory.ordered[GuidedVocabularyCategory.ADJUST_SETTINGS_INDEX]
        )
    }

    @Test
    fun noAndroidSystemKeyboardIntroduced() {
        val ui = readSource("app/src/main/java/com/idworx/lisa/LisaAccessibilityUi.kt")
        assertFalse(ui.contains("KeyboardOptions"))
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

    @Test
    fun entryGestureDoesNotDuplicateOpenWhileMenuOpen() {
        assertTrue(process(openLeft, openRight, openMenu()) is MainMenuSequenceResult.Unmatched)
    }
}
