package com.idworx.lisa

import com.idworx.lisa.ui.theme.LisaWorkspaceVisualStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * RC7D.30 — Main Menu visual consistency with Communication workspace, and full-width Menu button.
 */
class Rc7D_30MainMenuVisualConsistencyTest {

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

    // ------------------------------------------------------------------ A. Background

    @Test
    fun solidWorkspaceBackgroundIsOpaqueCanonicalDarkGrey() {
        val color = MainMenuProductionUiAuthority.solidWorkspaceBackground()
        assertEquals(LisaWorkspaceVisualStyle.SolidPanelBackground, color)
        assertTrue(color.alpha >= 1f)
        assertTrue(MainMenuProductionUiAuthority.isOpaqueWorkspaceBackground(color))
        assertFalse(
            MainMenuProductionUiAuthority.isOpaqueWorkspaceBackground(
                LisaWorkspaceVisualStyle.OverlayPanelBackground
            )
        )
    }

    @Test
    fun mainMenuHostAppliesSolidBackgroundWithoutAlpha() {
        val ui = readSource("app/src/main/java/com/idworx/lisa/LisaAccessibilityUi.kt")
        val host = ui.substringAfter("if (mainMenuActive) {")
            .substringBefore("if (phraseManagementActive)")
        assertTrue(host.contains("solidWorkspaceBackground()"))
        assertTrue(host.contains("LisaWorkspaceVisualStyle.PanelCornerRadius"))
        assertFalse(host.contains("Color.Transparent"))
        assertFalse(host.contains(".copy(alpha"))
    }

    @Test
    fun guidedOverlayKeepsSemiTransparentPanelOutsideMenuMode() {
        val guided = readSource("app/src/main/java/com/idworx/lisa/LisaGuidedModeUi.kt")
        assertTrue(guided.contains("LisaWorkspaceVisualStyle.OverlayPanelBackground"))
        assertTrue(LisaWorkspaceVisualStyle.OverlayPanelBackground.alpha < 1f)
        assertTrue(LisaWorkspaceVisualStyle.OverlayPanelBackground.alpha > 0.5f)
    }

    @Test
    fun guidedOverlayStillSuppressedWhileMenuOpen() {
        assertFalse(
            MainMenuProductionUiAuthority.showGuidedVocabularyOverlay(
                activePanel = LisaPanel.Menu,
                phraseComposerActive = false,
                phraseManagementActive = false
            )
        )
    }

    // ------------------------------------------------------------------ B. Destination cards

    @Test
    fun destinationCardsUseSharedCommunicationCardMetrics() {
        assertEquals(12, LisaWorkspaceVisualStyle.CardCornerRadius.value.toInt())
        assertEquals(12, LisaWorkspaceVisualStyle.CardHorizontalPadding.value.toInt())
        assertEquals(12, LisaWorkspaceVisualStyle.CardVerticalPadding.value.toInt())
        assertEquals(17, LisaWorkspaceVisualStyle.CardTitleSize.value.toInt())
        assertEquals(16, LisaWorkspaceVisualStyle.CardNumberSize.value.toInt())
        val ui = readSource("app/src/main/java/com/idworx/lisa/LisaAccessibilityUi.kt")
        val row = ui.substringAfter("private fun MainMenuDestinationRow")
            .substringBefore("private fun WorkspaceFullWidthActionButton")
        assertTrue(row.contains("LisaWorkspaceVisualStyle.CardCornerRadius"))
        assertTrue(row.contains("LisaWorkspaceVisualStyle.CardTitleSize"))
        assertTrue(row.contains("LisaWorkspaceVisualStyle.CardSelectedBackground"))
        assertTrue(row.contains("LisaWorkspaceVisualStyle.CardBackground"))
        assertTrue(row.contains("defaultMinSize(minHeight = 52.dp)"))
        assertFalse(row.contains("fontSize = 15.sp"))
    }

    @Test
    fun allEightDestinationsRemainInCatalog() {
        assertEquals(8, MainMenuCatalog.destinationCount)
        assertEquals(8, MainMenuCatalog.listEntries().filterIsInstance<MainMenuListEntry.Destination>().size)
    }

    // ------------------------------------------------------------------ C. Text consistency

    @Test
    fun menuTitleAndIndicatorsUseSharedSizes() {
        assertEquals(16, LisaWorkspaceVisualStyle.MenuTitleSize.value.toInt())
        assertEquals(13, LisaWorkspaceVisualStyle.IndicatorSize.value.toInt())
        assertEquals(13, LisaWorkspaceVisualStyle.SectionHeadingSize.value.toInt())
        val ui = readSource("app/src/main/java/com/idworx/lisa/LisaAccessibilityUi.kt")
        assertTrue(ui.contains("LisaWorkspaceVisualStyle.MenuTitleSize"))
        assertTrue(ui.contains("LisaWorkspaceVisualStyle.SectionHeadingSize"))
    }

    @Test
    fun navigationPanelReusesGuidedNavigationActionButton() {
        val ui = readSource("app/src/main/java/com/idworx/lisa/LisaAccessibilityUi.kt")
        val nav = ui.substringAfter("private fun MainMenuNavigationControls")
            .substringBefore("private fun MainMenuCloseRow")
        assertTrue(nav.contains("GuidedNavigationActionButton("))
        assertTrue(nav.contains("GuidedEmergencyNavButton("))
        assertTrue(nav.contains("LisaWorkspaceVisualStyle.NavPanelWidth"))
        assertTrue(nav.contains("LisaWorkspaceVisualStyle.NavPanelBackground"))
        assertFalse(nav.contains("fontSize = 8.sp"))
    }

    // ------------------------------------------------------------------ D. Communication bottom bar

    @Test
    fun clearAbsentFromCommunicationBottomBar() {
        assertFalse(MainMenuProductionUiAuthority.showCommunicationClearAndRepeat(LisaPanel.None))
        assertFalse(MainMenuProductionUiAuthority.showCommunicationClearAndRepeat(LisaPanel.Menu))
        val ui = readSource("app/src/main/java/com/idworx/lisa/LisaAccessibilityUi.kt")
        val chrome = ui.substringAfter("showWorkspaceBottomChrome(phraseComposerActive)")
            .substringBefore("if (activePanel != LisaPanel.None && !phraseManagementActive && !mainMenuActive)")
        assertFalse(chrome.contains("uiStrings.reset"))
        assertFalse(chrome.contains("onReset"))
        assertFalse(chrome.contains("uiStrings.repeat"))
        assertTrue(chrome.contains("WorkspaceFullWidthActionButton("))
        assertTrue(chrome.contains("uiStrings.menu"))
        assertTrue(chrome.contains("openMenuSequenceLabel()"))
    }

    @Test
    fun menuButtonIsFullWidthWithHorizontalSequence() {
        val ui = readSource("app/src/main/java/com/idworx/lisa/LisaAccessibilityUi.kt")
        val button = ui.substringAfter("private fun WorkspaceFullWidthActionButton")
            .substringBefore("private fun MyCommunicationPanel")
        assertTrue(button.contains("fillMaxWidth()"))
        assertTrue(button.contains("Arrangement.Center"))
        assertTrue(button.contains("Spacer(Modifier.width(12.dp))"))
        assertTrue(button.contains("LisaBlue"))
        assertTrue(button.contains("LisaWhite"))
        assertFalse(button.contains("subtitle"))
        assertFalse(button.contains("Column("))
        assertEquals(
            "L4 R6",
            MainMenuProductionUiAuthority.openMenuSequenceLabel()
        )
        assertEquals(
            LisaWorkspaceVisualStyle.FullWidthChromeHorizontalPadding,
            LisaWorkspaceVisualStyle.FullWidthChromeHorizontalPadding
        )
        assertEquals(10, LisaWorkspaceVisualStyle.FullWidthChromeHorizontalPadding.value.toInt())
    }

    @Test
    fun touchAndBlinkStillShareOpenMainMenu() {
        val main = readSource("app/src/main/java/com/idworx/lisa/MainActivity.kt")
        assertTrue(main.contains("private fun openMainMenu()"))
        assertTrue(main.contains("onMenuClick = { toggleMenuPanel() }"))
        assertTrue(main.contains("openMainMenu()"))
    }

    @Test
    fun clearFunctionalityRemainsAvailableGlobally() {
        val main = readSource("app/src/main/java/com/idworx/lisa/MainActivity.kt")
        assertTrue(main.contains("private fun performReset()"))
        val strings = LisaUiStrings.forLanguage(PreferredLanguage.English)
        assertEquals("Clear", strings.reset)
    }

    // ------------------------------------------------------------------ E. Menu-mode bottom

    @Test
    fun menuModeShowsFullWidthCloseWithoutClear() {
        val ui = readSource("app/src/main/java/com/idworx/lisa/LisaAccessibilityUi.kt")
        val chrome = ui.substringAfter("showWorkspaceBottomChrome(phraseComposerActive)")
            .substringBefore("if (activePanel != LisaPanel.None && !phraseManagementActive && !mainMenuActive)")
        assertTrue(chrome.contains("if (mainMenuActive)"))
        assertTrue(chrome.contains("uiStrings.close"))
        assertTrue(chrome.contains("closeMenuSequenceLabel()"))
        assertEquals("L2 R2", MainMenuProductionUiAuthority.closeMenuSequenceLabel())
        assertTrue(MainMenuProductionUiAuthority.showBottomCloseOnly(LisaPanel.Menu))
        assertFalse(MainMenuProductionUiAuthority.showBottomCloseOnly(LisaPanel.None))
    }

    // ------------------------------------------------------------------ F. Regression

    @Test
    fun gestureSequencesUnchanged() {
        assertEquals(4, GuidedModeNavigation.OPEN_MAIN_MENU_LEFT_WINKS)
        assertEquals(6, GuidedModeNavigation.OPEN_MAIN_MENU_RIGHT_WINKS)
        assertEquals(2, GuidedModeNavigation.BACK_LEFT)
        assertEquals(2, GuidedModeNavigation.BACK_RIGHT)
        assertEquals(5 to 5, GuidedModeNavigation.ADJUST_SETTINGS_ENTRY_LEFT to GuidedModeNavigation.ADJUST_SETTINGS_ENTRY_RIGHT)
    }

    @Test
    fun menuSelectionAndPagingStillFunctional() {
        var menu = MainMenuController.open()
        assertEquals(MainMenuDestination.CommunicationProfile, menu.selectedDestination)
        menu = (MainMenuController.processSequence(
            GuidedModeNavigation.NEXT_LEFT,
            GuidedModeNavigation.NEXT_RIGHT,
            menu
        ) as MainMenuSequenceResult.Navigate).newState
        assertEquals(MainMenuDestination.PhraseManagement, menu.selectedDestination)
        val paged = (MainMenuController.processSequence(
            GuidedModeNavigation.NEXT_CATEGORY_PAGE_LEFT,
            GuidedModeNavigation.NEXT_CATEGORY_PAGE_RIGHT,
            menu.copy(viewportPage = 0, viewportPageCount = 3),
            viewportHeightPx = 400,
            maxScrollPx = 800
        ) as MainMenuSequenceResult.Navigate).newState
        assertEquals(1, paged.viewportPage)
        assertEquals(400, paged.scrollRequestPx)
    }

    @Test
    fun emergencyRemainsHighestPriority() {
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
    }

    @Test
    fun noAndroidSystemKeyboardIntroduced() {
        val ui = readSource("app/src/main/java/com/idworx/lisa/LisaAccessibilityUi.kt")
        assertFalse(ui.contains("KeyboardOptions"))
    }
}
