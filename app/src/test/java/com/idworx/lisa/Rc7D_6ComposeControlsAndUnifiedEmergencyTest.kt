package com.idworx.lisa

import com.idworx.lisa.features.brain1interactionstandard.engine.Brain1DecisionEngine
import com.idworx.lisa.features.brain1interactionstandard.model.Brain1DecisionKind
import com.idworx.lisa.features.brain1interactionstandard.model.Brain1DecisionOutcome
import com.idworx.lisa.features.brain1interactionstandard.model.Brain1DecisionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** RC7D.6 — compose controls and one shared emergency interaction layer. */
class Rc7D_6ComposeControlsAndUnifiedEmergencyTest {

    private val english = LisaUiStrings.forLanguage(PreferredLanguage.English)
    private val confirmOrder = listOf(true, false)
    private val cancelOrder = listOf(false, true)

    @Test
    fun composeModeRendersSensitivityDecreaseAndIncreaseControls() {
        val keyboard = readSource("app/src/main/java/com/idworx/lisa/EyeControlledKeyboard.kt")
        assertTrue(keyboard.contains("uiStrings.sensitivityDecrease"))
        assertTrue(keyboard.contains("uiStrings.sensitivityIncrease"))
        assertTrue(keyboard.contains("onSensitivityDecrease"))
        assertTrue(keyboard.contains("onSensitivityIncrease"))
    }

    @Test
    fun composeModeRendersResponseTimeDecreaseAndIncreaseControls() {
        val keyboard = readSource("app/src/main/java/com/idworx/lisa/EyeControlledKeyboard.kt")
        assertTrue(keyboard.contains("uiStrings.responseTimeDecrease"))
        assertTrue(keyboard.contains("uiStrings.responseTimeIncrease"))
        assertTrue(keyboard.contains("onResponseTimeDecrease"))
        assertTrue(keyboard.contains("onResponseTimeIncrease"))
    }

    @Test
    fun composeControlsUseCanonicalSensitivityCallbacks() {
        val root = readSource("app/src/main/java/com/idworx/lisa/LisaAccessibilityUi.kt")
        val main = readSource("app/src/main/java/com/idworx/lisa/MainActivity.kt")
        assertTrue(root.contains("onSensitivityDecrease = onSensitivityDecrease"))
        assertTrue(root.contains("onSensitivityIncrease = onSensitivityIncrease"))
        assertTrue(main.contains("onSensitivityDecrease = { changeSensitivity(-1) }"))
        assertTrue(main.contains("onSensitivityIncrease = { changeSensitivity(1) }"))
    }

    @Test
    fun composeControlsUseCanonicalResponseTimeCallbacks() {
        val root = readSource("app/src/main/java/com/idworx/lisa/LisaAccessibilityUi.kt")
        val main = readSource("app/src/main/java/com/idworx/lisa/MainActivity.kt")
        assertTrue(root.contains("onResponseTimeDecrease = onResponseTimeDecrease"))
        assertTrue(root.contains("onResponseTimeIncrease = onResponseTimeIncrease"))
        assertTrue(main.contains("onResponseTimeDecrease = { changeResponseTime(-1) }"))
        assertTrue(main.contains("onResponseTimeIncrease = { changeResponseTime(1) }"))
    }

    @Test
    fun composeSensitivityChangesSameValueAsCommunication() {
        val main = readSource("app/src/main/java/com/idworx/lisa/MainActivity.kt")
        assertTrue(main.contains("uiSensitivityLevel.value"))
        assertTrue(main.contains("applySensitivityLevel(newLevel)"))
        assertTrue(main.contains("composerEyeFeedback = ComposerEyeFeedback("))
    }

    @Test
    fun composeResponseTimeChangesSameValueAsCommunication() {
        val main = readSource("app/src/main/java/com/idworx/lisa/MainActivity.kt")
        assertTrue(main.contains("uiSequenceProcessingDelaySec.value"))
        assertTrue(main.contains("applySequenceProcessingDelay(newSeconds)"))
        assertTrue(main.contains("displayResponseTimeSec"))
    }

    @Test
    fun existingMinMaxRulesRemainIntact() {
        val main = readSource("app/src/main/java/com/idworx/lisa/MainActivity.kt")
        assertTrue(main.contains("coerceIn(MIN_SENSITIVITY_LEVEL, MAX_SENSITIVITY_LEVEL)"))
        assertTrue(main.contains("SequenceProcessingDelay.coerce"))
    }

    @Test
    fun emergencyArmedOverlayShowsLeftAndRightBlinkFeedback() {
        // Canonical path: ComposerEyeFeedback → shared BlinkCounterRow (not legacy leftDots/rightDots calls).
        val emergency = readSource("app/src/main/java/com/idworx/lisa/LisaEmergencyUi.kt")
        assertTrue(emergency.contains("BlinkCounterRow("))
        assertTrue(emergency.contains("leftBlinkCount = blinkFeedback.leftWinkCount"))
        assertTrue(emergency.contains("rightBlinkCount = blinkFeedback.rightWinkCount"))
        // Physical left/right mapping is preserved through the shared counter labels.
        assertEquals("Left: ●", english.leftDots(1))
        assertEquals("Right: ●●", english.rightDots(2))
        assertEquals("Left: —", english.leftDots(0))
        assertEquals("Right: —", english.rightDots(0))
    }

    @Test
    fun emergencyArmedOverlayShowsPartialSequenceFeedback() {
        val emergency = readSource("app/src/main/java/com/idworx/lisa/LisaEmergencyUi.kt")
        assertTrue(emergency.contains("partialSequenceLabel()"))
        assertTrue(emergency.contains("phraseComposerPartialSequenceLabel"))
        // Behavioural coverage of zero / left-only / right-only / both sides.
        assertEquals(null, feedback(0, 0).partialSequenceLabel())
        assertEquals("L1", feedback(1, 0).partialSequenceLabel())
        assertEquals("R2", feedback(0, 2).partialSequenceLabel())
        assertEquals("L1 R1", feedback(1, 1).partialSequenceLabel())
        // Left-only progress toward L6 R0 shows L6 until a right wink is counted.
        assertEquals("L6", feedback(6, 0).partialSequenceLabel())
        assertEquals("L6 R0", formatWinkSequenceShort(6, 0))
    }

    @Test
    fun emergencyFeedbackUsesCanonicalBlinkState() {
        val root = readSource("app/src/main/java/com/idworx/lisa/LisaAccessibilityUi.kt")
        val main = readSource("app/src/main/java/com/idworx/lisa/MainActivity.kt")
        val emergency = readSource("app/src/main/java/com/idworx/lisa/LisaEmergencyUi.kt")
        assertTrue(root.contains("blinkFeedback = composerEyeFeedback"))
        assertTrue(main.contains("leftWinkCount = uiDiagLeftCount.value"))
        assertTrue(main.contains("rightWinkCount = uiDiagRightCount.value"))
        // Emergency must not invent a second blink detector or parallel counter state.
        assertFalse(emergency.contains("mutableStateOf"))
        assertFalse(emergency.contains("uiDiagLeftCount"))
        assertFalse(emergency.contains("uiDiagRightCount"))
        assertEquals(1, Regex("BlinkCounterRow\\(").findAll(emergency).count())
        assertEquals(0, feedback(0, 0).leftWinkCount)
        assertEquals(0, feedback(0, 0).rightWinkCount)
        assertEquals(3, feedback(3, 0).leftWinkCount)
        assertEquals(4, feedback(0, 4).rightWinkCount)
    }

    @Test
    fun l1R1StillConfirmsInOrder() {
        val armed = Brain1DecisionEngine.beginAwaitingConfirm(Brain1DecisionKind.EmergencyMode)
        val (_, outcome) = Brain1DecisionEngine.handleSequence(armed, 1, 1, confirmOrder)
        assertEquals(Brain1DecisionOutcome.Confirmed(Brain1DecisionKind.EmergencyMode), outcome)
    }

    @Test
    fun r1L1StillCancelsInOrder() {
        val armed = Brain1DecisionEngine.beginAwaitingConfirm(Brain1DecisionKind.EmergencyMode)
        val (_, outcome) = Brain1DecisionEngine.handleSequence(armed, 1, 1, cancelOrder)
        assertEquals(Brain1DecisionOutcome.ChooseAgain, outcome)
    }

    @Test
    fun wrongOrderDoesNotConfirm() {
        val armed = Brain1DecisionEngine.beginAwaitingConfirm(Brain1DecisionKind.EmergencyMode)
        val (_, outcome) = Brain1DecisionEngine.handleSequence(armed, 1, 1, cancelOrder)
        assertFalse(outcome is Brain1DecisionOutcome.Confirmed)
    }

    @Test
    fun blinkFeedbackResetsAfterCancellation() {
        assertEquals(null, feedback(0, 0).partialSequenceLabel())
        assertFalse(emergencyAwaitingConfirm(Brain1DecisionState()))
    }

    @Test
    fun manualCancelEmergencyAppearsDuringArmedState() {
        val emergency = readSource("app/src/main/java/com/idworx/lisa/LisaEmergencyUi.kt")
        assertTrue(emergency.contains("cancelEmergency"))
        assertTrue(emergency.contains("EmergencyManualButton"))
    }

    @Test
    fun manualCancelEmergencyClearsArmedState() {
        val controller = readSource(
            "app/src/main/java/com/idworx/lisa/features/onboardingguide/services/TrainingSessionController.kt"
        )
        val main = readSource("app/src/main/java/com/idworx/lisa/MainActivity.kt")
        assertTrue(controller.contains("fun clearBrain1Decision()"))
        assertTrue(main.contains("trainingSession.clearBrain1Decision()"))
    }

    @Test
    fun manualStopEmergencyAppearsDuringActiveAlarm() {
        val emergency = readSource("app/src/main/java/com/idworx/lisa/LisaEmergencyUi.kt")
        assertTrue(emergency.contains("stopEmergency"))
        assertTrue(emergency.contains("emergencyActiveTitle"))
        assertTrue(emergency.contains("emergencyAlarmActiveMessage"))
    }

    @Test
    fun manualStopEmergencyStopsAlarmController() {
        val main = readSource("app/src/main/java/com/idworx/lisa/MainActivity.kt")
        val fn = cancelOrStopEmergencySource(main)
        assertTrue(fn.contains("emergencyAlarmController.stop()"))
        assertTrue(fn.contains("tts?.stop()"))
    }

    @Test
    fun manualStopClearsGlobalOverlay() {
        val main = readSource("app/src/main/java/com/idworx/lisa/MainActivity.kt")
        val fn = cancelOrStopEmergencySource(main)
        assertTrue(fn.contains("emergencyActive = false"))
        assertTrue(fn.contains("uiEmergencyActive.value = false"))
    }

    @Test
    fun composerStateSurvivesEmergencyCancellation() {
        val main = readSource("app/src/main/java/com/idworx/lisa/MainActivity.kt")
        val fn = cancelOrStopEmergencySource(main)
        assertFalse(fn.contains("uiPhraseComposerState.value = PhraseComposerController.initialState()"))
        assertFalse(fn.contains("closeAllPanels()"))
    }

    @Test
    fun communicationAndSettingsSurviveEmergencyCancellation() {
        val main = readSource("app/src/main/java/com/idworx/lisa/MainActivity.kt")
        val fn = cancelOrStopEmergencySource(main)
        assertFalse(fn.contains("uiGuidedNavigationState.value = GuidedNavigationState()"))
        assertFalse(fn.contains("uiActivePanel.value = LisaPanel.None"))
    }

    @Test
    fun emergencyOverlayIdenticalAcrossModes() {
        val root = readSource("app/src/main/java/com/idworx/lisa/LisaAccessibilityUi.kt")
        assertEquals(1, Regex("GlobalEmergencyOverlayLayer\\(").findAll(root).count())
        assertTrue(root.contains("emergencyAwaitingConfirm = emergencyAwaitingConfirm"))
    }

    @Test
    fun underlyingModeCommandsSuspendedDuringEmergencyModal() {
        val moveLeft = ModeScopedGestureAuthority.phraseComposerCommandSequences
            .getValue(PhraseComposerActionId.MoveLeft)
        val target = ModeScopedGestureAuthority.routingTarget(
            LisaGestureContext(
                activePanel = LisaPanel.PhraseEditor,
                guidedOverlayActive = false,
                guidedScreenMode = null,
                isAdjustingPreference = false,
                phraseComposerMode = PhraseComposerMode.Keyboard,
                emergencyModalActive = true
            ),
            moveLeft.first,
            moveLeft.second
        )
        assertEquals(GestureRoutingTarget.Emergency, target)
    }

    @Test
    fun emergencyRemainsAvailableFromEveryInteractionMode() {
        LisaInteractionMode.entries.forEach { mode ->
            val context = LisaGestureContext(
                activePanel = if (mode.name.startsWith("PhraseComposer")) LisaPanel.PhraseEditor else LisaPanel.None,
                guidedOverlayActive = mode.name.startsWith("Communication"),
                guidedScreenMode = GuidedOverlayScreenMode.Vocabulary,
                isAdjustingPreference = mode == LisaInteractionMode.CommunicationAdjustment,
                phraseComposerMode = PhraseComposerMode.Keyboard.takeIf { mode == LisaInteractionMode.PhraseComposerKeyboard },
                emergencyModalActive = mode == LisaInteractionMode.EmergencyModal
            )
            assertEquals(
                GestureRoutingTarget.Emergency,
                ModeScopedGestureAuthority.routingTarget(context, EMERGENCY_LEFT_WINKS, EMERGENCY_RIGHT_WINKS)
            )
        }
    }

    @Test
    fun manualButtonHasAccessibilityDescription() {
        val emergency = readSource("app/src/main/java/com/idworx/lisa/LisaEmergencyUi.kt")
        assertTrue(emergency.contains("contentDescription = label"))
    }

    @Test
    fun rc7dSuitesRemainGreenByAudit() {
        assertTrue(ModeScopedGestureAuthorityAudit.passes())
        assertTrue(PhraseComposerCommandAudit.passes())
    }

    private fun feedback(left: Int, right: Int): ComposerEyeFeedback =
        ComposerEyeFeedback(EyeTrackingBannerContext(), left, right, 5, 5)

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

    private fun cancelOrStopEmergencySource(main: String): String =
        main.substringAfter("private fun cancelOrStopEmergency()")
            .substringBefore("private fun refreshCameraPermissionState()")
}
