package com.idworx.lisa

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * RC7D.44 — Single active gesture scope: Main Menu (and other layered panels) must not
 * leak Communication phrase preview / WAITING / phrase-path processing.
 */
class Rc7D_44GestureScopeIsolationTest {

    private fun menuContext(): LisaGestureContext = LisaGestureContext(
        activePanel = LisaPanel.Menu,
        guidedOverlayActive = true,
        guidedScreenMode = GuidedOverlayScreenMode.Vocabulary,
        isAdjustingPreference = false,
        phraseComposerMode = null
    )

    private fun communicationContext(): LisaGestureContext = LisaGestureContext(
        activePanel = LisaPanel.None,
        guidedOverlayActive = true,
        guidedScreenMode = GuidedOverlayScreenMode.Vocabulary,
        isAdjustingPreference = false,
        phraseComposerMode = null
    )

    private fun settingsDestinationContext(): LisaGestureContext = LisaGestureContext(
        activePanel = LisaPanel.Settings,
        guidedOverlayActive = true,
        guidedScreenMode = GuidedOverlayScreenMode.Vocabulary,
        isAdjustingPreference = false,
        phraseComposerMode = null
    )

    @Test
    fun mainMenuIsSoleOrdinaryGestureScope() {
        assertEquals(
            LisaInteractionMode.MainMenu,
            ModeScopedGestureAuthority.activeMode(menuContext())
        )
        assertFalse(ModeScopedGestureAuthority.communicationPhraseFeedbackActive(menuContext()))
        assertTrue(ModeScopedGestureAuthority.suspendsCommunicationPhraseProcessing(LisaPanel.Menu))
        assertEquals(
            GestureRoutingTarget.MainMenu,
            ModeScopedGestureAuthority.routingTarget(menuContext(), 2, 1)
        )
        assertEquals(
            GestureRoutingTarget.MainMenu,
            ModeScopedGestureAuthority.routingTarget(menuContext(), 0, 2)
        )
    }

    @Test
    fun communicationPhraseFeedbackActiveOnlyInCommunicationModes() {
        assertTrue(ModeScopedGestureAuthority.communicationPhraseFeedbackActive(communicationContext()))
        assertTrue(
            ModeScopedGestureAuthority.communicationPhraseFeedbackActive(
                communicationContext().copy(guidedScreenMode = GuidedOverlayScreenMode.CategoryMenu)
            )
        )
        assertFalse(ModeScopedGestureAuthority.communicationPhraseFeedbackActive(menuContext()))
        assertFalse(
            ModeScopedGestureAuthority.communicationPhraseFeedbackActive(settingsDestinationContext())
        )
        assertFalse(
            ModeScopedGestureAuthority.communicationPhraseFeedbackActive(
                LisaGestureContext(
                    activePanel = LisaPanel.PhraseEditor,
                    guidedOverlayActive = true,
                    guidedScreenMode = GuidedOverlayScreenMode.Vocabulary,
                    isAdjustingPreference = false,
                    phraseComposerMode = PhraseComposerMode.Keyboard
                )
            )
        )
        assertFalse(
            ModeScopedGestureAuthority.communicationPhraseFeedbackActive(
                LisaGestureContext(
                    activePanel = LisaPanel.VocabularyTraining,
                    guidedOverlayActive = true,
                    guidedScreenMode = GuidedOverlayScreenMode.Vocabulary,
                    isAdjustingPreference = false,
                    phraseComposerMode = null
                )
            )
        )
    }

    @Test
    fun unmatchedMenuSequenceDoesNotFallThroughToCommunicationPhrasePath() {
        // Destination shortcut L2 R1 opens Communication Profile — still Menu route, not phrases.
        assertEquals(
            GestureRoutingTarget.MainMenu,
            ModeScopedGestureAuthority.routingTarget(menuContext(), 2, 1)
        )
        assertNotEquals(
            GestureRoutingTarget.CommunicationPhrasePath,
            ModeScopedGestureAuthority.routingTarget(menuContext(), 9, 9)
        )
        val unmatched = MainMenuController.processSequence(9, 9, MainMenuController.open())
        assertEquals(MainMenuSequenceResult.Unmatched, unmatched)
    }

    @Test
    fun settingsPanelUnmatchedDoesNotFallThroughToCommunication() {
        val legacySettings = LisaGestureContext(
            activePanel = LisaPanel.CreatePhrase,
            guidedOverlayActive = true,
            guidedScreenMode = GuidedOverlayScreenMode.Vocabulary,
            isAdjustingPreference = false,
            phraseComposerMode = null
        )
        assertEquals(
            LisaInteractionMode.SettingsPanel,
            ModeScopedGestureAuthority.activeMode(legacySettings)
        )
        assertEquals(
            GestureRoutingTarget.ScopeUnmatched,
            ModeScopedGestureAuthority.routingTarget(legacySettings, 2, 1)
        )
        assertEquals(
            GestureRoutingTarget.SettingsPanelBack,
            ModeScopedGestureAuthority.routingTarget(
                legacySettings,
                GuidedModeNavigation.BACK_LEFT,
                GuidedModeNavigation.BACK_RIGHT
            )
        )
    }

    @Test
    fun emergencyRemainsGlobalOverMenu() {
        assertEquals(
            GestureRoutingTarget.Emergency,
            ModeScopedGestureAuthority.routingTarget(
                menuContext(),
                EMERGENCY_LEFT_WINKS,
                EMERGENCY_RIGHT_WINKS
            )
        )
    }

    @Test
    fun layeredPanelsSuspendPhraseProcessing() {
        listOf(
            LisaPanel.Menu,
            LisaPanel.Settings,
            LisaPanel.VocabularyTraining,
            LisaPanel.PhraseEditor,
            LisaPanel.AboutLisa,
            LisaPanel.Feedback,
            LisaPanel.Recalibration
        ).forEach { panel ->
            assertTrue(
                "$panel must suspend Communication phrase processing",
                ModeScopedGestureAuthority.suspendsCommunicationPhraseProcessing(panel)
            )
        }
        assertFalse(
            ModeScopedGestureAuthority.suspendsCommunicationPhraseProcessing(LisaPanel.None)
        )
    }

    @Test
    fun menuShortcutStillOpensDestinationWithoutPhrasePath() {
        val (left, right) = MainMenuDestinationShortcuts.gestureForDestination(
            MainMenuDestination.Settings
        )
        assertEquals(
            GestureRoutingTarget.MainMenu,
            ModeScopedGestureAuthority.routingTarget(menuContext(), left, right)
        )
        val result = MainMenuController.processSequence(left, right, MainMenuController.open())
        assertTrue(result is MainMenuSequenceResult.OpenDestination)
        assertEquals(
            MainMenuDestination.Settings,
            (result as MainMenuSequenceResult.OpenDestination).destination
        )
    }

    @Test
    fun intentPreviewSuppressedInRootUiWhileMenuOpen() {
        val ui = readSource("app/src/main/java/com/idworx/lisa/LisaAccessibilityUi.kt")
        assertTrue(ui.contains("suspendsCommunicationPhraseProcessing(activePanel)"))
        assertTrue(ui.contains("IntentPreviewCard"))
    }

    @Test
    fun mainActivitySuspendsAndResumesPhraseProcessingOnPanelTransitions() {
        val main = readSource("app/src/main/java/com/idworx/lisa/MainActivity.kt")
        assertTrue(main.contains("suspendCommunicationPhraseProcessing"))
        assertTrue(main.contains("resumeCommunicationPhraseProcessing"))
        assertTrue(main.contains("communicationPhraseFeedbackActive"))
        assertTrue(main.contains("GestureRoutingTarget.ScopeUnmatched"))
        assertTrue(main.contains("activeScopeContinuationMappings"))
        // Phrase path hard gate while layered scopes own input.
        assertTrue(main.contains("Hard isolation: never evaluate Communication phrases"))
    }

    @Test
    fun possibleMatchDisplayRequiresCommunicationOwnedFeedback() {
        val display = LisaCommunicationState.PossibleMatch("STOP").toUserDisplay(
            strings = LisaUiStrings.forLanguage(PreferredLanguage.English),
            pendingPhrase = null,
            countdown = null
        )
        assertEquals("STOP", display.phrase)
        assertTrue(display.showIntentPreview)
        // Isolation is enforced before this state is set while Menu is open — authority gate.
        assertFalse(ModeScopedGestureAuthority.communicationPhraseFeedbackActive(menuContext()))
    }

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
}
