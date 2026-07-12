package com.idworx.lisa

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** RC7C3.2 — Mode-Scoped Gesture Authority (MSGA) with RC7D keyboard composer. */
class Rc7C3_2ModeScopedGestureAuthorityTest {

    private val english = LisaUiStrings.forLanguage(PreferredLanguage.English)

    private fun communicationContext(
        screenMode: GuidedOverlayScreenMode = GuidedOverlayScreenMode.Vocabulary
    ): LisaGestureContext = LisaGestureContext(
        activePanel = LisaPanel.None,
        guidedOverlayActive = true,
        guidedScreenMode = screenMode,
        isAdjustingPreference = false,
        phraseComposerMode = null
    )

    private fun keyboardContext(): LisaGestureContext = LisaGestureContext(
        activePanel = LisaPanel.PhraseEditor,
        guidedOverlayActive = true,
        guidedScreenMode = GuidedOverlayScreenMode.Vocabulary,
        isAdjustingPreference = false,
        phraseComposerMode = PhraseComposerMode.Keyboard
    )

    private fun settingsContext(panel: LisaPanel = LisaPanel.Settings): LisaGestureContext =
        LisaGestureContext(
            activePanel = panel,
            guidedOverlayActive = false,
            guidedScreenMode = null,
            isAdjustingPreference = false,
            phraseComposerMode = null
        )

    @Test
    fun msgaSelfAuditPasses() {
        assertTrue(
            "MSGA audit findings: ${ModeScopedGestureAuthorityAudit.auditAll()}",
            ModeScopedGestureAuthorityAudit.passes()
        )
    }

    @Test
    fun phraseComposerCommandAuditPasses() {
        assertTrue(
            "Composer command audit findings: ${PhraseComposerCommandAudit.auditAll()}",
            PhraseComposerCommandAudit.passes()
        )
    }

    @Test
    fun everyInteractionModeOwnsIndependentNamespace() {
        val modesWithNamespace = listOf(
            LisaInteractionMode.CommunicationVocabulary,
            LisaInteractionMode.CommunicationCategoryMenu,
            LisaInteractionMode.PhraseComposerKeyboard,
            LisaInteractionMode.PhraseComposerDestinationCategory,
            LisaInteractionMode.PhraseComposerSaveConfirmation,
            LisaInteractionMode.PhraseComposerDuplicateWarning,
            LisaInteractionMode.PhraseComposerCancelConfirm,
            LisaInteractionMode.PhraseComposerSuccess
        )
        modesWithNamespace.forEach { mode ->
            assertTrue(
                "$mode must register gesture bindings",
                ModeScopedGestureAuthority.namespaceFor(mode).isNotEmpty()
            )
        }
    }

    @Test
    fun keyboardModeOwnsNavigationCommandNamespace() {
        val commands = ModeScopedGestureAuthority.phraseComposerKeyboardCommandBindings()
        assertEquals(10, commands.size)
        assertTrue(commands.any { it.label == PhraseComposerActionId.MoveLeft.name })
        assertTrue(commands.any { it.label == PhraseComposerActionId.ToggleKeyboardLayout.name })
    }

    @Test
    fun globalGesturesRemainAvailableEverywhere() {
        assertTrue(ModeScopedGestureAuthority.isGlobalGesture(GuidedModeNavigation.BACK_LEFT, GuidedModeNavigation.BACK_RIGHT))
        assertTrue(ModeScopedGestureAuthority.isGlobalGesture(EMERGENCY_LEFT_WINKS, EMERGENCY_RIGHT_WINKS))
    }

    @Test
    fun keyboardGesturesIgnoredWhileCommunicationIsActive() {
        val moveLeft = ModeScopedGestureAuthority.phraseComposerCommandSequences
            .getValue(PhraseComposerActionId.MoveLeft)
        assertNotEquals(
            GestureRoutingTarget.PhraseComposer,
            ModeScopedGestureAuthority.routingTarget(
                communicationContext(),
                moveLeft.first,
                moveLeft.second
            )
        )
    }

    @Test
    fun communicationGesturesIgnoredWhileKeyboardIsActive() {
        val moveLeft = ModeScopedGestureAuthority.phraseComposerCommandSequences
            .getValue(PhraseComposerActionId.MoveLeft)
        assertEquals(
            GestureRoutingTarget.PhraseComposer,
            ModeScopedGestureAuthority.routingTarget(
                keyboardContext(),
                moveLeft.first,
                moveLeft.second
            )
        )
    }

    @Test
    fun keyboardUsesNavigationOptimizedGestures() {
        assertEquals(2 to 0, PhraseComposerCommandSequences.sequenceFor(PhraseComposerActionId.MoveUp))
        assertEquals(0 to 2, PhraseComposerCommandSequences.sequenceFor(PhraseComposerActionId.MoveDown))
        assertEquals(2 to 1, PhraseComposerCommandSequences.sequenceFor(PhraseComposerActionId.MoveLeft))
        assertEquals(1 to 2, PhraseComposerCommandSequences.sequenceFor(PhraseComposerActionId.MoveRight))
        assertEquals(1 to 1, PhraseComposerCommandSequences.sequenceFor(PhraseComposerActionId.SelectKey))
        assertEquals(3 to 1, PhraseComposerCommandSequences.sequenceFor(PhraseComposerActionId.Backspace))
        assertEquals(1 to 3, PhraseComposerCommandSequences.sequenceFor(PhraseComposerActionId.Preview))
        assertEquals(3 to 2, PhraseComposerCommandSequences.sequenceFor(PhraseComposerActionId.Save))
    }

    @Test
    fun keyboardCommandPanelHasNoInternalConflicts() {
        val panel = PhraseComposerController.commandPanelEntries(
            PhraseComposerState(mode = PhraseComposerMode.Keyboard),
            english
        )
        val sequences = panel.map { it.left to it.right }
        assertEquals(sequences.size, sequences.distinct().size)
    }

    @Test
    fun crossModeSlotReuseIsIntentionalNotAmbiguousWithinMode() {
        val comm = ModeScopedGestureAuthority.communicationVocabularyBindings()
            .map { it.left to it.right }.toSet()
        val keyboard = ModeScopedGestureAuthority.phraseComposerKeyboardCommandBindings()
            .map { it.left to it.right }.toSet()
        assertTrue(comm.intersect(keyboard).isNotEmpty())
    }

    @Test
    fun mainActivityRoutesThroughMsgaAuthority() {
        val mainActivitySource = readSource("app/src/main/java/com/idworx/lisa/MainActivity.kt")
        assertTrue(mainActivitySource.contains("ModeScopedGestureAuthority.routingTarget"))
        assertTrue(mainActivitySource.contains("buildGestureContext()"))
        assertTrue(mainActivitySource.contains("GestureRoutingTarget.PhraseComposer"))
    }

    @Test
    fun rc7DRegressionSuiteStillPasses() {
        assertTrue(PhraseComposerCommandAudit.passes())
        assertEquals(PhraseComposerMode.Keyboard, PhraseComposerController.initialState().mode)
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
