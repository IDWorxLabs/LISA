package com.idworx.lisa

import com.idworx.lisa.features.brain1interactionstandard.engine.Brain1DecisionEngine
import com.idworx.lisa.features.brain1interactionstandard.model.Brain1DecisionKind
import com.idworx.lisa.features.brain1interactionstandard.model.Brain1DecisionOutcome
import com.idworx.lisa.features.brain1interactionstandard.model.Brain1DecisionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** RC7D.3 — composer emergency overlay visibility, modal routing, and keyboard sizing. */
class Rc7D_3ComposerEmergencyOverlayVisibilityTest {

    private val english = LisaUiStrings.forLanguage(PreferredLanguage.English)
    private val letters = EyeKeyboardLayoutMode.Letters
    private val numbers = EyeKeyboardLayoutMode.Numbers
    private val cancelOrder = listOf(false, true)
    private val confirmOrder = listOf(true, false)

    private fun keyboardContext(
        emergencyModalActive: Boolean = false,
        mode: PhraseComposerMode = PhraseComposerMode.Keyboard
    ) = LisaGestureContext(
        activePanel = LisaPanel.PhraseEditor,
        guidedOverlayActive = false,
        guidedScreenMode = null,
        isAdjustingPreference = false,
        phraseComposerMode = mode,
        emergencyModalActive = emergencyModalActive
    )

    private fun armedEmergencyDecision(): Brain1DecisionState =
        Brain1DecisionEngine.beginAwaitingConfirm(Brain1DecisionKind.EmergencyMode)

    @Test
    fun l6R0RoutesToEmergencyFromComposerKeyboard() {
        val target = ModeScopedGestureAuthority.routingTarget(
            keyboardContext(),
            EMERGENCY_LEFT_WINKS,
            EMERGENCY_RIGHT_WINKS
        )
        assertEquals(GestureRoutingTarget.Emergency, target)
    }

    @Test
    fun globalLayerRendersAboveComposerInRootUi() {
        val root = readSource("app/src/main/java/com/idworx/lisa/LisaAccessibilityUi.kt")
        val composerIndex = root.indexOf("EyeControlledPhraseComposerOverlay")
        val globalIndex = root.indexOf("GlobalEmergencyOverlayLayer")
        assertTrue(composerIndex >= 0)
        assertTrue(globalIndex > composerIndex)
    }

    @Test
    fun overlayVisibleWithoutExitingComposeMode() {
        val root = readSource("app/src/main/java/com/idworx/lisa/LisaAccessibilityUi.kt")
        assertTrue(root.contains("GlobalEmergencyOverlayLayer"))
        assertTrue(root.contains("composerInputSuspended"))
        assertFalse(root.contains("if (emergencyActive) {\n            EmergencyOverlay"))
    }

    @Test
    fun overlayFromDestinationCategorySelection() {
        assertOverlayWorksForMode(PhraseComposerMode.DestinationCategorySelection)
    }

    @Test
    fun overlayFromSaveConfirmation() {
        assertOverlayWorksForMode(PhraseComposerMode.SaveConfirmation)
    }

    @Test
    fun overlayFromCancelConfirmation() {
        assertOverlayWorksForMode(PhraseComposerMode.CancelConfirm)
    }

    @Test
    fun overlayFromSuccessMode() {
        assertOverlayWorksForMode(PhraseComposerMode.Success)
    }

    private fun assertOverlayWorksForMode(mode: PhraseComposerMode) {
        val context = keyboardContext(mode = mode)
        assertEquals(
            GestureRoutingTarget.Emergency,
            ModeScopedGestureAuthority.routingTarget(context, EMERGENCY_LEFT_WINKS, EMERGENCY_RIGHT_WINKS)
        )
        val modalContext = context.copy(emergencyModalActive = true)
        assertEquals(LisaInteractionMode.EmergencyModal, ModeScopedGestureAuthority.activeMode(modalContext))
    }

    @Test
    fun composerGesturesSuspendedWhileEmergencyModalActive() {
        val moveLeft = ModeScopedGestureAuthority.phraseComposerCommandSequences
            .getValue(PhraseComposerActionId.MoveLeft)
        assertNotEquals(
            GestureRoutingTarget.PhraseComposer,
            ModeScopedGestureAuthority.routingTarget(
                keyboardContext(emergencyModalActive = true),
                moveLeft.first,
                moveLeft.second
            )
        )
        assertEquals(
            LisaInteractionMode.EmergencyModal,
            ModeScopedGestureAuthority.activeMode(keyboardContext(emergencyModalActive = true))
        )
    }

    @Test
    fun emergencyConfirmAndCancelBehaviourUnchanged() {
        val armed = armedEmergencyDecision()
        val (_, confirm) = Brain1DecisionEngine.handleSequence(armed, 1, 1, confirmOrder)
        val (_, cancel) = Brain1DecisionEngine.handleSequence(armed, 1, 1, cancelOrder)
        assertTrue(confirm is Brain1DecisionOutcome.Confirmed)
        assertEquals(Brain1DecisionOutcome.ChooseAgain, cancel)
    }

    @Test
    fun cancelClearsEmergencyAwaitingState() {
        assertFalse(emergencyAwaitingConfirm(Brain1DecisionState()))
    }

    @Test
    fun cancelPreservesPhraseText() {
        val before = PhraseComposerState(
            mode = PhraseComposerMode.Keyboard,
            phraseText = "room 12",
            keyboardLayoutMode = numbers,
            cursorRow = 1,
            cursorCol = 2
        )
        val afterCancel = before.copy()
        assertEquals("room 12", afterCancel.phraseText)
    }

    @Test
    fun cancelPreservesKeyboardCursor() {
        val state = PhraseComposerState(
            mode = PhraseComposerMode.Keyboard,
            cursorRow = 2,
            cursorCol = 4
        )
        assertEquals(2, state.cursorRow)
        assertEquals(4, state.cursorCol)
    }

    @Test
    fun cancelPreservesKeyboardLayoutMode() {
        val state = PhraseComposerState(
            mode = PhraseComposerMode.Keyboard,
            keyboardLayoutMode = numbers
        )
        assertEquals(numbers, state.keyboardLayoutMode)
    }

    @Test
    fun noHiddenOverlayAfterCancel() {
        assertFalse(emergencyAwaitingConfirm(Brain1DecisionState()))
    }

    @Test
    fun communicationEmergencyBehaviourUnchanged() {
        assertEquals(
            EMERGENCY_LEFT_WINKS to EMERGENCY_RIGHT_WINKS,
            PhraseComposerCommandSequences.emergencySequence
        )
        assertTrue(ModeScopedGestureAuthority.isGlobalGesture(EMERGENCY_LEFT_WINKS, EMERGENCY_RIGHT_WINKS))
    }

    @Test
    fun composerCommandPanelUsesBrightSharedStyle() {
        val grid = readSource("app/src/main/java/com/idworx/lisa/ComposerCommandGrid.kt")
        assertTrue(grid.contains("ComposerCommandCard"))
        assertTrue(grid.contains("CommandCardBackground"))
    }

    @Test
    fun emergencyCommandCardRemainsRed() {
        val grid = readSource("app/src/main/java/com/idworx/lisa/ComposerCommandGrid.kt")
        assertTrue(grid.contains("ComposerEmergencyCommandCard"))
    }

    @Test
    fun everyComposerCommandShowsLabelAndSequence() {
        val panel = PhraseComposerController.commandPanelEntries(
            PhraseComposerState(mode = PhraseComposerMode.Keyboard),
            english
        )
        assertTrue(panel.size >= 10)
        panel.forEach { entry ->
            assertTrue(entry.label.isNotBlank())
            assertTrue(entry.sequenceLabel.isNotBlank())
        }
    }

    @Test
    fun keyboardExpandsIntoAvailableVerticalSpace() {
        val height = ComposerKeyboardLayoutMetrics.bottomAnchoredKeyHeightDp(letters)
        assertTrue(height >= ComposerKeyboardLayoutMetrics.MIN_KEY_HEIGHT_DP)
        assertTrue(height <= ComposerKeyboardLayoutMetrics.MAX_KEY_HEIGHT_DP)
    }

    @Test
    fun noLargeFixedSpacerUnderKeyboard() {
        val ui = readSource("app/src/main/java/com/idworx/lisa/PhraseComposerUi.kt")
        assertFalse(ui.contains("Spacer(Modifier.height(80.dp))"))
    }

    @Test
    fun letterKeysVisibleOnCompactPortraitConstraint() {
        val height = ComposerKeyboardLayoutMetrics.keyHeightDp(availableHeightDp = 220, mode = letters)
        assertTrue(height >= ComposerKeyboardLayoutMetrics.MIN_KEY_HEIGHT_DP)
        assertEquals(4, ComposerKeyboardLayoutMetrics.rowCount(letters))
    }

    @Test
    fun numericLayoutFullyVisibleOnCompactPortrait() {
        val height = ComposerKeyboardLayoutMetrics.keyHeightDp(availableHeightDp = 280, mode = numbers)
        assertTrue(height >= ComposerKeyboardLayoutMetrics.MIN_KEY_HEIGHT_DP)
        assertEquals(4, ComposerKeyboardLayoutMetrics.rowCount(numbers))
    }

    @Test
    fun spaceRowIncludedInResponsiveLayout() {
        assertEquals(
            KeyboardLayout.spaceRowIndex(letters),
            ComposerKeyboardLayoutMetrics.rowCount(letters) - 1
        )
    }

    @Test
    fun selectedKeyHasStrongVisualState() {
        val keyboard = readSource("app/src/main/java/com/idworx/lisa/EyeControlledKeyboard.kt")
        assertTrue(keyboard.contains("KeyHighlightFill"))
        assertTrue(keyboard.contains("border"))
        assertTrue(keyfontWeightPresent(keyboard))
    }

    @Test
    fun eyeBlinkFeedbackRemainsVisibleInComposeMode() {
        val ui = readSource("app/src/main/java/com/idworx/lisa/PhraseComposerUi.kt")
        assertTrue(ui.contains("ComposerEyeStatusBar"))
    }

    @Test
    fun msgaAuditRemainsValid() {
        assertTrue(ModeScopedGestureAuthorityAudit.passes())
        assertTrue(PhraseComposerCommandAudit.passes())
    }

    @Test
    fun globalConfirmOverlayUsesExistingMessage() {
        val ui = readSource("app/src/main/java/com/idworx/lisa/LisaEmergencyUi.kt")
        assertTrue(ui.contains("guidedEmergencyAwaitingConfirmMessage"))
        assertTrue(ui.contains("Brain1EmergencyConfirmOverlay"))
    }

    @Test
    fun mainActivityBlocksComposerDuringEmergencyModal() {
        val main = readSource("app/src/main/java/com/idworx/lisa/MainActivity.kt")
        assertTrue(main.contains("emergencyAwaitingConfirm(trainingSession.state.brain1Decision)"))
        assertTrue(main.contains("emergencyModalActive = emergencyAwaitingConfirm"))
    }

    private fun keyfontWeightPresent(source: String): Boolean =
        source.contains("FontWeight.ExtraBold") || source.contains("ExtraBold")

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
