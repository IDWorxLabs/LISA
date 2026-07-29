package com.idworx.lisa

import com.idworx.lisa.features.brain1interactionstandard.model.UniversalInteractionGestures
import com.idworx.lisa.features.intelligentstartup.authority.WelcomeEyeNavigationAuthority
import com.idworx.lisa.features.intelligentstartup.authority.WelcomeStage
import com.idworx.lisa.features.intelligentstartup.authority.WelcomeStageAction
import com.idworx.lisa.features.invalidsequencefeedback.UniversalInvalidSequenceAuthority
import com.idworx.lisa.features.invalidsequencefeedback.UniversalInvalidSequenceAuthority.Decision
import com.idworx.lisa.features.invalidsequencefeedback.UniversalInvalidSequenceAuthority.Surface
import com.idworx.lisa.features.onboardingguide.lessoninteraction.LessonInteractionEngine
import com.idworx.lisa.features.onboardingguide.lessoninteraction.GuidedFeedbackPhrases
import com.idworx.lisa.features.onboardingguide.model.CommunicationLesson
import com.idworx.lisa.features.universalsequenceexecution.GuidedReadinessSequenceAuthority
import com.idworx.lisa.features.zerotouchprinciple.audit.ZeroTouchFileProbe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * RC8.44 — universal invalid-sequence warning across Welcome and guided-entry surfaces.
 */
class Rc8_44UniversalInvalidSequenceWarningTest {

    private val leftThenRight = listOf(true, false)

    private fun readMain(path: String): String =
        ZeroTouchFileProbe.readProjectFile("app/src/main/java/com/idworx/lisa/$path")
            ?: error("Missing $path")

    // --- Universal authority ---

    @Test
    fun validSequenceReturnsExecuteDecision() {
        val decision = UniversalInvalidSequenceAuthority.evaluateWelcome(
            stage = WelcomeStage.BlinkSequenceIntroduction,
            left = 1,
            right = 1,
            blinkOrder = leftThenRight,
            matchesValidAction = true
        )
        assertEquals(Decision.ExecuteValidAction, decision)
    }

    @Test
    fun unrelatedCompletedSequenceReturnsInvalidWarning() {
        val decision = UniversalInvalidSequenceAuthority.evaluateWelcome(
            stage = WelcomeStage.BlinkSequenceIntroduction,
            left = 0,
            right = 2,
            blinkOrder = emptyList(),
            matchesValidAction = false
        )
        assertTrue(decision is Decision.ShowInvalidWarning)
        val warning = (decision as Decision.ShowInvalidWarning).warning
        assertTrue(warning.contextLine.contains("L1 R1"))
        assertTrue(warning.contextLine.contains("continue", ignoreCase = true))
    }

    @Test
    fun incompleteSequenceDoesNotShowInvalidFeedback() {
        val decision = UniversalInvalidSequenceAuthority.evaluate(
            surface = Surface.WelcomeIntroduction,
            left = 1,
            right = 0,
            matchesValidAction = false
        )
        assertEquals(Decision.NotApplicable, decision)
    }

    @Test
    fun globalEmergencyGetsPriorityViaNotApplicable() {
        val decision = UniversalInvalidSequenceAuthority.evaluateGuidedReadiness(
            left = EMERGENCY_LEFT_WINKS,
            right = EMERGENCY_RIGHT_WINKS,
            blinkOrder = emptyList(),
            matchesValidAction = false
        )
        assertEquals(Decision.NotApplicable, decision)
    }

    @Test
    fun lessonSpecificMismatchMessagesRemainUnchanged() {
        val lesson = CommunicationLesson(
            id = "test",
            vocabularyId = "yes",
            left = 1,
            right = 0,
            displayOrder = 1
        )
        val msg = LessonInteractionEngine.wrongEyeRestartFeedbackMessage(lesson, restartCount = 0)
        assertTrue(msg.contains("Wrong eye"))
        assertTrue(msg.contains("blink left to start again") || msg.contains("start again"))
        assertEquals("Wrong gesture — try again.", GuidedFeedbackPhrases.wrongGesture())
        val authority = readMain("features/invalidsequencefeedback/UniversalInvalidSequenceAuthority.kt")
        assertFalse(authority.contains("Wrong eye — blink left to start again"))
        assertFalse(authority.contains("Wrong gesture — try again."))
    }

    @Test
    fun warningDoesNotExecuteAnyAction() {
        val intro = WelcomeEyeNavigationAuthority.resolve(
            WelcomeStage.BlinkSequenceIntroduction, 0, 2
        )
        assertEquals(WelcomeStageAction.None, intro)
        val dest = WelcomeEyeNavigationAuthority.resolve(
            WelcomeStage.DestinationSelection, 1, 1, leftThenRight
        )
        assertEquals(WelcomeStageAction.None, dest)
        assertNull(GuidedReadinessSequenceAuthority.resolve(0, 2))
        val warning = UniversalInvalidSequenceAuthority.buildWarning(Surface.WelcomeIntroduction)
        assertNotNull(warning)
        // Decision path for unrelated is ShowInvalidWarning only — no ExecuteValidAction.
        val decision = UniversalInvalidSequenceAuthority.evaluateWelcome(
            WelcomeStage.BlinkSequenceIntroduction, 2, 0, emptyList(), matchesValidAction = false
        )
        assertTrue(decision is Decision.ShowInvalidWarning)
    }

    @Test
    fun warningCopyAlwaysIncludesWhatUserCanDoNext() {
        listOf(
            Surface.WelcomeIntroduction,
            Surface.WelcomeDestination,
            Surface.GuidedReadiness
        ).forEach { surface ->
            val warning = UniversalInvalidSequenceAuthority.buildWarning(surface)
            assertTrue(warning.contextLine.startsWith("Use "))
            UniversalInvalidSequenceAuthority.validSequences(surface).forEach { seq ->
                assertTrue(
                    "context must include ${seq.label} for $surface",
                    warning.contextLine.contains(seq.label)
                )
            }
        }
    }

    // --- Welcome introduction ---

    @Test
    fun welcomeL1R1ContinuesAndUnrelatedWarns() {
        assertEquals(
            WelcomeStageAction.ContinueToDestinationSelection,
            WelcomeEyeNavigationAuthority.resolve(
                WelcomeStage.BlinkSequenceIntroduction, 1, 1, leftThenRight
            )
        )
        val warnL0R2 = UniversalInvalidSequenceAuthority.evaluateWelcome(
            WelcomeStage.BlinkSequenceIntroduction, 0, 2, emptyList(), false
        )
        val warnL2R0 = UniversalInvalidSequenceAuthority.evaluateWelcome(
            WelcomeStage.BlinkSequenceIntroduction, 2, 0, emptyList(), false
        )
        assertTrue(warnL0R2 is Decision.ShowInvalidWarning)
        assertTrue(warnL2R0 is Decision.ShowInvalidWarning)
        val copy = (warnL0R2 as Decision.ShowInvalidWarning).warning
        assertTrue(copy.contextLine.contains("L1 R1"))
    }

    // --- Destination selection ---

    @Test
    fun destinationValidSequencesExecuteAndUnrelatedWarns() {
        assertEquals(
            WelcomeStageAction.StartGuidedLearning,
            WelcomeEyeNavigationAuthority.resolve(WelcomeStage.DestinationSelection, 2, 0)
        )
        assertEquals(
            WelcomeStageAction.SkipToCommunication,
            WelcomeEyeNavigationAuthority.resolve(WelcomeStage.DestinationSelection, 0, 2)
        )
        assertEquals(
            WelcomeStageAction.BackToIntroduction,
            WelcomeEyeNavigationAuthority.resolve(WelcomeStage.DestinationSelection, 2, 2)
        )
        val warn = UniversalInvalidSequenceAuthority.evaluateWelcome(
            WelcomeStage.DestinationSelection, 1, 1, leftThenRight, false
        )
        assertTrue(warn is Decision.ShowInvalidWarning)
        val copy = (warn as Decision.ShowInvalidWarning).warning
        assertEquals(UniversalInvalidSequenceAuthority.PRIMARY_LINE_MULTI_PAGE, copy.primaryLine)
        assertTrue(copy.contextLine.contains("L2 R0"))
        assertTrue(copy.contextLine.contains("L0 R2"))
        assertTrue(copy.contextLine.contains("L2 R2"))
    }

    // --- Guided readiness ---

    @Test
    fun guidedReadinessValidAndUnrelated() {
        assertEquals(
            GuidedReadinessSequenceAuthority.Action.Continue,
            GuidedReadinessSequenceAuthority.resolve(1, 1, leftThenRight)
        )
        assertEquals(
            GuidedReadinessSequenceAuthority.Action.Back,
            GuidedReadinessSequenceAuthority.resolve(2, 2)
        )
        val warn = UniversalInvalidSequenceAuthority.evaluateGuidedReadiness(
            left = 0,
            right = 2,
            blinkOrder = emptyList(),
            matchesValidAction = false
        )
        assertTrue(warn is Decision.ShowInvalidWarning)
        val copy = (warn as Decision.ShowInvalidWarning).warning
        assertTrue(copy.contextLine.contains("L1 R1"))
        assertTrue(copy.contextLine.contains("L2 R2"))
        assertTrue(copy.contextLine.contains("continue", ignoreCase = true))
        assertTrue(copy.contextLine.contains("back", ignoreCase = true))
    }

    // --- Lifecycle / wiring ---

    @Test
    fun controllerAppliesWarningAndClearsOnNavigationAndNewAttempt() {
        val controller = readMain("features/onboardingguide/services/TrainingSessionController.kt")
        assertTrue(controller.contains("applyInvalidSequenceWarning("))
        assertTrue(controller.contains("clearInvalidSequenceWarning()"))
        assertTrue(controller.contains("UniversalInvalidSequenceAuthority"))
        assertTrue(controller.contains("invalidSequenceWarning = null"))
        assertTrue(controller.contains("WARNING_CLEAR_MS"))
        // New attempt clears stale warning.
        assertTrue(controller.contains("clearWarning") || controller.contains("invalidSequenceWarning != null && (left > 0 || right > 0)"))
        // Welcome unrelated path shows warning instead of silent consume.
        val welcomeFn = controller.substringAfter("fun handleWelcomeStageInteraction(")
            .substringBefore("fun advanceWelcomeToDestinationSelection(")
        assertTrue(welcomeFn.contains("applyInvalidSequenceWarning"))
        // Readiness consumes unrelated sequences (no fall-through).
        val readyFn = controller.substringAfter("fun handleSetupReadinessInteraction(")
            .substringBefore("fun applyInvalidSequenceWarning(")
        assertTrue(readyFn.contains("applyInvalidSequenceWarning"))
        assertTrue(readyFn.contains("return true"))
    }

    @Test
    fun warningUiWiredOnWelcomeAndReadinessWithoutDuplicateInsets() {
        val welcome = readMain("features/onboardingguide/ui/TrainingWelcomeScreen.kt")
        assertTrue(welcome.contains("InvalidSequenceWarningBanner"))
        assertTrue(welcome.contains("invalidSequenceWarning"))
        assertFalse(welcome.contains("systemBarsPadding()"))
        val setup = readMain("features/onboardingguide/ui/TrainingSetupScreen.kt")
        assertTrue(setup.contains("InvalidSequenceWarningBanner"))
        assertTrue(setup.contains("invalidSequenceWarning"))
        val flow = readMain("features/onboardingguide/ui/GuidedTrainingFlow.kt")
        assertTrue(flow.contains("invalidSequenceWarning = state.invalidSequenceWarning"))
        val banner = readMain("features/invalidsequencefeedback/InvalidSequenceWarningBanner.kt")
        assertTrue(banner.contains("LisaEmergencyRed"))
        assertFalse(banner.contains("TextToSpeech"))
        assertFalse(banner.contains("narration"))
        assertFalse(banner.contains("speakSequence"))
    }

    @Test
    fun rc843InsetAuthorityRemainsUnchanged() {
        val insets = readMain("features/systembarinsets/LisaSystemBarInsetAuthority.kt")
        assertTrue(insets.contains("systemBarsPadding()"))
        assertTrue(insets.contains("safeApplicationContent()"))
        val main = readMain("MainActivity.kt")
        assertTrue(main.contains("LisaSystemBarInsetAuthority"))
        assertTrue(main.contains("safeApplicationContent()"))
        assertEquals(1, Regex("safeApplicationContent\\(\\)").findAll(main).count())
    }

    @Test
    fun stateHoldsSingleTransientWarningField() {
        val state = readMain("features/onboardingguide/state/GuidedTrainingUiState.kt")
        assertTrue(state.contains("invalidSequenceWarning"))
        assertEquals(
            1,
            Regex("val invalidSequenceWarning").findAll(state).count()
        )
    }

    @Test
    fun confirmGestureConstantsUnchanged() {
        assertEquals(1, UniversalInteractionGestures.CONFIRM_LEFT)
        assertEquals(1, UniversalInteractionGestures.CONFIRM_RIGHT)
        assertEquals(1, WelcomeEyeNavigationAuthority.continueLeft)
        assertEquals(2, WelcomeEyeNavigationAuthority.startGuidedLearningLeft)
        assertEquals(0, WelcomeEyeNavigationAuthority.skipToCommunicationLeft)
        assertEquals(2, GuidedReadinessSequenceAuthority.BACK_LEFT)
    }
}
