package com.idworx.lisa

import com.idworx.lisa.features.intelligentstartup.StartupSessionController
import com.idworx.lisa.features.intelligentstartup.authority.CalibrationCompatibilityAuthority
import com.idworx.lisa.features.intelligentstartup.authority.StartupCalibrationRequirementAuthority
import com.idworx.lisa.features.intelligentstartup.authority.StartupFlowAuthority
import com.idworx.lisa.features.intelligentstartup.model.CalibrationCompatibilityLevel
import com.idworx.lisa.features.intelligentstartup.model.CalibrationConfidenceLevel
import com.idworx.lisa.features.intelligentstartup.model.ProfileEyeCalibration
import com.idworx.lisa.features.intelligentstartup.model.QuickCalibrationStep
import com.idworx.lisa.features.intelligentstartup.model.StartupEvent
import com.idworx.lisa.features.intelligentstartup.model.StartupFlowState
import com.idworx.lisa.features.intelligentstartup.model.StartupPhase
import com.idworx.lisa.features.zerotouchprinciple.audit.ZeroTouchFileProbe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * RC1.0.4 — Version 1 startup route is Splash → Preparing → Quick Calibration → Welcome.
 *
 * Guards the two release-blocking defects found on the Samsung: a duplicate four-tick readiness
 * screen after Preparing, and quick calibration being skipped for an already-calibrated profile.
 */
class Rc1_0_4StartupRouteAndMandatoryCalibrationTest {

    // ---------------------------------------------------------------- harness

    private class Clock {
        var now: Long = 100_000L
        private val pending = mutableListOf<Pair<Long, () -> Unit>>()

        val pendingCount: Int get() = pending.size

        fun schedule(delayMs: Long, action: () -> Unit) {
            pending += (now + delayMs) to action
        }

        fun runDue() {
            while (true) {
                val next = pending.filter { it.first <= now }.minByOrNull { it.first } ?: return
                pending.remove(next)
                next.second()
            }
        }

        fun advanceTo(target: Long) {
            now = target
            runDue()
        }

        fun advanceBy(deltaMs: Long) = advanceTo(now + deltaMs)
    }

    /** Drives the real controller with a deterministic clock and a previously calibrated profile. */
    private class Session(storedConfidence: Float? = 0.95f) {
        val clock = Clock()
        var startupCompleted = false
        var persisted: ProfileEyeCalibration? = null
        val controller: StartupSessionController

        init {
            var stored = storedConfidence?.let {
                ProfileEyeCalibration(
                    leftClosedEyeThreshold = 0.22f,
                    rightClosedEyeThreshold = 0.24f,
                    openEyeThreshold = 0.78f,
                    blinkDurationMs = 150L,
                    requiredWinkFrames = 2,
                    eyeOpennessBaseline = 0.85f,
                    faceDistanceProxy = 0.42f,
                    eyeSpacingProxy = 0.147f,
                    confidence = it,
                    calibratedAtMs = clock.now
                )
            }
            controller = StartupSessionController(
                loadProfiles = {
                    listOf(
                        LisaUserProfile(
                            id = "returning",
                            name = "Returning User",
                            preferredLanguage = PreferredLanguage.English,
                            communicationLevel = CommunicationLevel.Standard,
                            eyeCalibration = stored
                        )
                    )
                },
                loadProfileCalibration = { stored },
                persistCalibration = {
                    stored = it
                    persisted = it
                },
                activateProfile = { },
                createPrimaryUser = { _, _, _ -> error("unused") },
                nowMs = { clock.now },
                onStartupComplete = { startupCompleted = true },
                scheduleReadyHandoff = { delay, action -> clock.schedule(delay, action) },
                scheduleAutoRetry = { _, _ -> }
            )
        }

        val phase: StartupPhase get() = controller.state.phase
        val step: QuickCalibrationStep get() = controller.state.calibrationStep

        /** Preparing screen, both gates satisfied, landing in quick calibration. */
        fun reachQuickCalibration() {
            controller.start()
            controller.onFacePresence(true)
            repeat(8) { controller.onFrameSample(0.86f, 0.84f, 0.42f, 0.147f) }
            clock.advanceBy(1_200L)
            clock.advanceTo(105_000L)
        }

        /** Real baseline capture: 20 frames plus the production minimum dwell. */
        fun completeBaselineStep() {
            repeat(10) {
                controller.onFrameSample(0.86f, 0.84f, 0.42f, 0.147f)
                clock.advanceBy(50L)
            }
            clock.advanceBy(StartupSessionController.LOOK_NATURALLY_MIN_MS)
            repeat(10) {
                controller.onFrameSample(0.86f, 0.84f, 0.42f, 0.147f)
                clock.advanceBy(50L)
            }
        }

        fun blink(times: Int) = repeat(times) {
            controller.onBothBlinkAccepted(closePeak = 0.15f, durationMs = 150L)
        }

        fun leftWink(times: Int) = repeat(times) {
            controller.onLeftWinkAccepted(closePeak = 0.12f)
        }

        fun rightWink(times: Int) = repeat(times) {
            controller.onRightWinkAccepted(closePeak = 0.13f)
        }
    }

    private fun read(path: String): String =
        ZeroTouchFileProbe.readProjectFile(path) ?: error("Missing source: $path")

    private fun startupFlowSource() = read(
        "app/src/main/java/com/idworx/lisa/features/intelligentstartup/ui/IntelligentStartupFlow.kt"
    )

    // ---------------------------------------------------------------- route order

    @Test
    fun preparingIsTheOnlySurfaceForTheThreePreCalibrationPhases() {
        val source = startupFlowSource().replace("\r\n", "\n")
        assertTrue(source.contains("StartupPhase.GlassesQuestion -> GlassesQuestionScreen("))
        assertTrue(source.contains("StartupPhase.GlassesGuidance -> GlassesGuidanceScreen("))
        assertTrue(source.contains("StartupPhase.FaceDetection,"))
        assertTrue(source.contains("StartupPhase.ProfileResolution,"))
        assertTrue(
            source.contains(
                "StartupPhase.EvaluatingCompatibility -> FaceDetectionStartupScreen("
            )
        )
        assertEquals(StartupPhase.FaceDetection, StartupFlowState().phase)
    }

    @Test
    fun preparingDoesNotRouteBeforeFiveSeconds() {
        val session = Session()
        session.controller.start()
        session.controller.onFacePresence(true)
        repeat(8) { session.controller.onFrameSample(0.86f, 0.84f, 0.42f, 0.147f) }
        session.clock.advanceTo(101_200L)
        assertTrue(session.controller.state.preparationComplete)

        session.clock.advanceTo(104_999L)
        assertEquals(StartupPhase.EvaluatingCompatibility, session.phase)
    }

    @Test
    fun preparationCompletedAloneDoesNotRoute() {
        val after = StartupFlowAuthority.reduce(
            StartupFlowState(phase = StartupPhase.EvaluatingCompatibility, faceDetected = true),
            StartupEvent.PreparationCompleted(CalibrationCompatibilityLevel.High)
        )
        assertEquals(StartupPhase.EvaluatingCompatibility, after.phase)
        assertFalse(after.skippedCalibration)
    }

    @Test
    fun fiveSecondsAloneDoesNotRouteWhenPreparationIsIncomplete() {
        val session = Session()
        session.controller.start()
        session.controller.onFacePresence(true)
        // Never let the compatibility sample land: preparation stays unfinished.
        session.clock.now = 130_000L
        assertEquals(StartupPhase.EvaluatingCompatibility, session.phase)
        assertFalse(session.controller.state.preparationComplete)
        assertEquals(0L, session.controller.remainingPreparingGuidanceMs())
    }

    @Test
    fun bothGatesRouteFromPreparingIntoQuickCalibration() {
        val session = Session()
        session.reachQuickCalibration()
        assertEquals(StartupPhase.QuickCalibration, session.phase)
        assertEquals(QuickCalibrationStep.LookNaturally, session.step)
        assertFalse(session.controller.state.skippedCalibration)
    }

    @Test
    fun preparingNeverRoutesDirectlyToWelcome() {
        val preparing = StartupFlowState(
            phase = StartupPhase.EvaluatingCompatibility,
            faceDetected = true,
            communicationPrepared = true,
            calibrationDecisionReady = true
        )
        val events = listOf(
            StartupEvent.BeginCompatibilityEvaluation,
            StartupEvent.FacePresenceChanged(true),
            StartupEvent.PreparationCompleted(CalibrationCompatibilityLevel.High),
            StartupEvent.CompatibilityEvaluated(CalibrationCompatibilityLevel.High),
            StartupEvent.CompatibilityEvaluated(CalibrationCompatibilityLevel.Medium),
            StartupEvent.CompatibilityEvaluated(CalibrationCompatibilityLevel.Low),
            StartupEvent.ConfidenceEvaluated(CalibrationConfidenceLevel.High),
            StartupEvent.ConfidenceEvaluated(CalibrationConfidenceLevel.Low),
            StartupEvent.ConfidenceEvaluated(CalibrationConfidenceLevel.Missing),
            StartupEvent.AcknowledgeEyeTrackingReady,
            StartupEvent.CalibrationSucceeded,
            StartupEvent.AdvanceCalibrationStep
        )
        events.forEach { event ->
            val next = StartupFlowAuthority.reduce(preparing, event)
            assertFalse(
                "Preparing must never reach Welcome via $event",
                next.phase == StartupPhase.Complete || next.phase == StartupPhase.EyeTrackingReady
            )
        }
    }

    @Test
    fun noDuplicateReadinessPhaseExistsInTheStateMachine() {
        assertEquals(
            listOf(
                "GlassesQuestion",
                "GlassesGuidance",
                "FaceDetection",
                "ProfileResolution",
                "CreatePrimaryUser",
                "ProfileSelection",
                "EvaluatingCompatibility",
                "QuickCalibration",
                "CalibrationFailure",
                "EyeTrackingReady",
                "Complete"
            ),
            StartupPhase.entries.map { it.name }
        )
    }

    @Test
    fun quickCalibrationReachesWelcomeOnlyAfterSuccessfulCompletion() {
        val session = Session()
        session.reachQuickCalibration()
        session.completeBaselineStep()
        assertEquals(QuickCalibrationStep.BlinkThreeTimes, session.step)
        session.blink(3)
        session.leftWink(2)
        session.rightWink(2)
        assertEquals(QuickCalibrationStep.CalibrationComplete, session.step)
        assertFalse(session.startupCompleted)

        session.clock.advanceBy(StartupSessionController.SUCCESS_DISPLAY_MS)
        assertEquals(StartupPhase.EyeTrackingReady, session.phase)
        session.clock.advanceBy(StartupSessionController.READY_HANDOFF_MS)
        assertEquals(StartupPhase.Complete, session.phase)
        assertTrue(session.startupCompleted)
        assertNotNull(session.persisted)
    }

    // ---------------------------------------------------------------- checklist surface

    @Test
    fun readinessRowsRenderOnlyOnThePreparingSurface() {
        val source = startupFlowSource()
        assertEquals(1, Regex("PreparationProgressList\\(").findAll(source).count() - 1)

        val handoff = source.substringAfter("private fun EyeTrackingReadyScreen(")
            .substringBefore("\n@Composable")
        assertFalse(handoff.contains("PreparationProgressList"))
        assertFalse(handoff.contains("PreparationStep."))
    }

    @Test
    fun cameraGuidanceCardStaysVisibleForTheWholePreparingPhase() {
        val source = startupFlowSource().replace("\r\n", "\n")
        assertTrue(source.contains("CameraGuidanceCard(uiStrings = uiStrings, tight = tight)"))
        // The card is bound to the same flag the checklist uses, so neither can outlive the other.
        assertTrue(source.contains("evaluating = state.phase == StartupPhase.EvaluatingCompatibility ||"))
        assertTrue(source.contains("state.phase == StartupPhase.ProfileResolution"))
    }

    @Test
    fun calibrationReadyRowDoesNotMeanTheUserIsCalibrated() {
        val ready = StartupFlowState(
            phase = StartupPhase.EvaluatingCompatibility,
            faceDetected = true,
            communicationPrepared = true,
            calibrationDecisionReady = true
        )
        assertTrue(ready.preparationComplete)
        assertFalse(ready.skippedCalibration)
        assertFalse(ready.eyeControlActive)
        assertEquals(QuickCalibrationStep.LookNaturally, ready.calibrationStep)

        val routed = StartupFlowAuthority.reduce(
            ready,
            StartupEvent.CompatibilityEvaluated(CalibrationCompatibilityLevel.High)
        )
        assertEquals(StartupPhase.QuickCalibration, routed.phase)
    }

    // ---------------------------------------------------------------- calibration sequence

    @Test
    fun quickCalibrationStartsAtBaselineThenAsksForThreeNormalBlinks() {
        val session = Session()
        session.reachQuickCalibration()
        assertEquals(QuickCalibrationStep.LookNaturally, session.step)
        assertEquals(0, session.controller.state.blinksCollected)

        session.completeBaselineStep()
        assertEquals(QuickCalibrationStep.BlinkThreeTimes, session.step)
    }

    @Test
    fun threeBothEyeBlinksAreRequiredBeforeTheLeftWinkStep() {
        val session = Session()
        session.reachQuickCalibration()
        session.completeBaselineStep()

        session.blink(1)
        assertEquals(QuickCalibrationStep.BlinkThreeTimes, session.step)
        assertEquals(1, session.controller.state.blinksCollected)
        session.blink(1)
        assertEquals(QuickCalibrationStep.BlinkThreeTimes, session.step)
        session.blink(1)
        assertEquals(QuickCalibrationStep.LeftWinkTwice, session.step)
    }

    @Test
    fun twoLeftWinksAreRequiredBeforeTheRightWinkStep() {
        val session = Session()
        session.reachQuickCalibration()
        session.completeBaselineStep()
        session.blink(3)

        session.leftWink(1)
        assertEquals(QuickCalibrationStep.LeftWinkTwice, session.step)
        assertEquals(1, session.controller.state.leftWinksCollected)
        session.leftWink(1)
        assertEquals(QuickCalibrationStep.RightWinkTwice, session.step)
    }

    @Test
    fun twoRightWinksCompleteCalibration() {
        val session = Session()
        session.reachQuickCalibration()
        session.completeBaselineStep()
        session.blink(3)
        session.leftWink(2)

        session.rightWink(1)
        assertEquals(QuickCalibrationStep.RightWinkTwice, session.step)
        session.rightWink(1)
        assertEquals(QuickCalibrationStep.CalibrationComplete, session.step)
    }

    @Test
    fun calibrationCannotAutoCompleteWithoutRealInput() {
        val session = Session()
        session.reachQuickCalibration()
        session.clock.advanceTo(200_000L)
        assertEquals(StartupPhase.QuickCalibration, session.phase)
        assertEquals(QuickCalibrationStep.LookNaturally, session.step)

        session.completeBaselineStep()
        session.clock.advanceTo(300_000L)
        assertEquals(QuickCalibrationStep.BlinkThreeTimes, session.step)
        assertEquals(0, session.controller.state.blinksCollected)
        assertFalse(session.startupCompleted)
    }

    @Test
    fun wrongEyeInputDoesNotAdvanceTheWrongPhase() {
        val session = Session()
        session.reachQuickCalibration()
        session.completeBaselineStep()

        // Winks during the blink step, and blinks/right-winks during the left-wink step.
        session.leftWink(3)
        session.rightWink(3)
        assertEquals(QuickCalibrationStep.BlinkThreeTimes, session.step)
        assertEquals(0, session.controller.state.leftWinksCollected)

        session.blink(3)
        assertEquals(QuickCalibrationStep.LeftWinkTwice, session.step)
        session.rightWink(2)
        assertEquals(QuickCalibrationStep.LeftWinkTwice, session.step)
        assertEquals(0, session.controller.state.rightWinksCollected)
    }

    @Test
    fun welcomeIsBlockedUntilCalibrationCompletes() {
        val session = Session()
        session.reachQuickCalibration()
        session.completeBaselineStep()
        session.blink(3)
        session.leftWink(2)
        session.rightWink(1)

        session.clock.advanceTo(400_000L)
        assertFalse(session.startupCompleted)
        assertEquals(StartupPhase.QuickCalibration, session.phase)
    }

    @Test
    fun calibrationProgressResetsOnAFreshStartupSession() {
        val session = Session()
        session.reachQuickCalibration()
        session.completeBaselineStep()
        session.blink(2)
        assertEquals(2, session.controller.state.blinksCollected)

        session.controller.start()
        assertEquals(StartupPhase.FaceDetection, session.phase)
        assertEquals(QuickCalibrationStep.LookNaturally, session.step)
        assertEquals(0, session.controller.state.blinksCollected)
        assertEquals(0, session.controller.state.leftWinksCollected)
        assertEquals(0, session.controller.state.rightWinksCollected)
        assertFalse(session.controller.state.communicationPrepared)
        assertFalse(session.controller.state.calibrationDecisionReady)
    }

    // ---------------------------------------------------------------- persistence

    @Test
    fun storedHighConfidenceProfileDoesNotBypassCalibration() {
        val session = Session(storedConfidence = 0.98f)
        session.reachQuickCalibration()
        assertEquals(StartupPhase.QuickCalibration, session.phase)
        assertFalse(session.controller.state.skippedCalibration)
        // The compatibility score is still evaluated and recorded truthfully.
        assertNotNull(session.controller.state.compatibilityLevel)
    }

    @Test
    fun restartWithAFreshlySavedCalibrationStillRunsCalibrationAgain() {
        val session = Session(storedConfidence = 0.95f)
        session.reachQuickCalibration()
        session.completeBaselineStep()
        session.blink(3)
        session.leftWink(2)
        session.rightWink(2)
        session.clock.advanceBy(StartupSessionController.SUCCESS_DISPLAY_MS)
        session.clock.advanceBy(StartupSessionController.READY_HANDOFF_MS)
        assertEquals(StartupPhase.Complete, session.phase)
        assertNotNull(session.persisted)

        session.startupCompleted = false
        session.controller.start()
        session.controller.onFacePresence(true)
        repeat(8) { session.controller.onFrameSample(0.86f, 0.84f, 0.42f, 0.147f) }
        session.clock.advanceBy(1_200L)
        assertEquals(StartupPhase.EvaluatingCompatibility, session.phase)
        session.clock.advanceBy(5_000L)
        assertEquals(StartupPhase.QuickCalibration, session.phase)
        assertFalse(session.startupCompleted)
    }

    @Test
    fun secureStorageCarriesNoCalibrationCompletionFlag() {
        val files = listOf(
            "LisaPreferences.kt",
            "LisaSecurePreferencesMigration.kt",
            "LisaSecureSharedPreferences.kt",
            "LisaSecureTypedCodec.kt"
        )
        files.forEach { name ->
            val source = read("app/src/main/java/com/idworx/lisa/features/securestorage/$name")
            assertFalse(
                "$name must not persist calibration completion state",
                source.contains("calibrationComplete", ignoreCase = true) ||
                    source.contains("calibrationRequired", ignoreCase = true) ||
                    source.contains("skipCalibration", ignoreCase = true)
            )
        }
    }

    @Test
    fun startupRoutingIgnoresTrainingAndOnboardingProgress() {
        val reducer = read(
            "app/src/main/java/com/idworx/lisa/features/intelligentstartup/authority/" +
                "StartupFlowAuthority.kt"
        )
        assertFalse(reducer.contains("TrainingProgress"))
        assertFalse(reducer.contains("onboarding", ignoreCase = true))
        assertFalse(reducer.contains("Preferences"))
    }

    // ---------------------------------------------------------------- version 1 rule

    @Test
    fun version1AlwaysRequiresQuickCalibration() {
        assertTrue(StartupCalibrationRequirementAuthority.VERSION_1_ALWAYS_CALIBRATES)
        CalibrationCompatibilityLevel.entries.forEach { level ->
            assertTrue(
                "Version 1 must calibrate at $level",
                StartupCalibrationRequirementAuthority.requiresQuickCalibration(level)
            )
            assertFalse(StartupCalibrationRequirementAuthority.skipsQuickCalibration(level))
        }
        // The underlying compatibility rule is preserved for a future returning-user shortcut.
        assertTrue(
            CalibrationCompatibilityAuthority.shouldSkipQuickCalibration(
                CalibrationCompatibilityLevel.High
            )
        )
    }

    @Test
    fun controllerRoutesThroughTheVersion1RequirementAuthority() {
        val controller = read(
            "app/src/main/java/com/idworx/lisa/features/intelligentstartup/" +
                "StartupSessionController.kt"
        )
        assertTrue(
            controller.contains(
                "StartupCalibrationRequirementAuthority.skipsQuickCalibration(level)"
            )
        )
        assertFalse(
            controller.contains(
                "CalibrationCompatibilityAuthority.shouldSkipQuickCalibration(level)"
            )
        )
    }
}
