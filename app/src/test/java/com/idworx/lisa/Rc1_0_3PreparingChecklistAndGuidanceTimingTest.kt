package com.idworx.lisa

import com.idworx.lisa.features.intelligentstartup.StartupSessionController
import com.idworx.lisa.features.intelligentstartup.authority.EyeCalibrationAuthority
import com.idworx.lisa.features.intelligentstartup.authority.PreparationChecklistStep
import com.idworx.lisa.features.intelligentstartup.authority.StartupFlowAuthority
import com.idworx.lisa.features.intelligentstartup.authority.StartupPreparationChecklistAuthority
import com.idworx.lisa.features.intelligentstartup.model.CalibrationCompatibilityLevel
import com.idworx.lisa.features.intelligentstartup.model.ProfileEyeCalibration
import com.idworx.lisa.features.intelligentstartup.model.QuickCalibrationStep
import com.idworx.lisa.features.intelligentstartup.model.StartupEvent
import com.idworx.lisa.features.intelligentstartup.model.StartupFlowState
import com.idworx.lisa.features.intelligentstartup.model.StartupPhase
import com.idworx.lisa.features.zerotouchprinciple.audit.ZeroTouchFileProbe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * RC1.0.3 — the Preparing screen must show all four preparation checks and keep the camera
 * guidance card readable for at least five seconds before calibration starts.
 */
class Rc1_0_3PreparingChecklistAndGuidanceTimingTest {

    // ---------------------------------------------------------------- harness

    /** Deterministic clock + scheduler so timing is asserted, never slept on. */
    private class FakeStartupScheduler {
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
    }

    private fun calibration(confidence: Float, now: Long) = ProfileEyeCalibration(
        leftClosedEyeThreshold = 0.22f,
        rightClosedEyeThreshold = 0.24f,
        openEyeThreshold = 0.78f,
        blinkDurationMs = 150L,
        requiredWinkFrames = 2,
        eyeOpennessBaseline = 0.85f,
        faceDistanceProxy = 0.4f,
        eyeSpacingProxy = 0.14f,
        confidence = confidence,
        calibratedAtMs = now
    )

    private class Harness(storedConfidence: Float?) {
        val scheduler = FakeStartupScheduler()
        var quickCalibrationEntries = 0
        var eyeTrackingReadyEntries = 0
        private var lastPhase: StartupPhase? = null
        val controller: StartupSessionController

        init {
            val stored = storedConfidence?.let {
                ProfileEyeCalibration(
                    leftClosedEyeThreshold = 0.22f,
                    rightClosedEyeThreshold = 0.24f,
                    openEyeThreshold = 0.78f,
                    blinkDurationMs = 150L,
                    requiredWinkFrames = 2,
                    eyeOpennessBaseline = 0.85f,
                    faceDistanceProxy = 0.4f,
                    eyeSpacingProxy = 0.14f,
                    confidence = it,
                    calibratedAtMs = scheduler.now
                )
            }
            var current = stored
            controller = StartupSessionController(
                loadProfiles = {
                    listOf(
                        LisaUserProfile(
                            id = "only",
                            name = "Only",
                            preferredLanguage = PreferredLanguage.English,
                            communicationLevel = CommunicationLevel.Standard,
                            eyeCalibration = current
                        )
                    )
                },
                loadProfileCalibration = { current },
                persistCalibration = { current = it },
                activateProfile = { },
                createPrimaryUser = { _, _, _ -> error("unused") },
                nowMs = { scheduler.now },
                onStateChanged = { state ->
                    if (state.phase != lastPhase) {
                        when (state.phase) {
                            StartupPhase.QuickCalibration -> quickCalibrationEntries++
                            StartupPhase.EyeTrackingReady -> eyeTrackingReadyEntries++
                            else -> Unit
                        }
                        lastPhase = state.phase
                    }
                },
                scheduleReadyHandoff = { delay, action -> scheduler.schedule(delay, action) },
                scheduleAutoRetry = { _, _ -> }
            )
        }

        fun beginPreparing() {
            controller.start()
            controller.onFacePresence(true)
        }

        /** Frames keep arriving while Preparing is on screen, like the live camera does. */
        fun feedFrames(count: Int = 6) {
            repeat(count) { controller.onFrameSample(0.85f, 0.84f, 0.4f, 0.14f) }
        }
    }

    // ---------------------------------------------------------------- checklist

    @Test
    fun preparingScreenShowsAllFourChecksInOrder() {
        val state = StartupFlowState(
            phase = StartupPhase.EvaluatingCompatibility,
            faceDetected = true,
            communicationPrepared = true,
            calibrationDecisionReady = true
        )
        val steps = StartupPreparationChecklistAuthority.completedSteps(state)
        assertEquals(
            listOf(
                PreparationChecklistStep.EyeTracking,
                PreparationChecklistStep.LoadingProfile,
                PreparationChecklistStep.PreparingCommunication,
                PreparationChecklistStep.CalibrationReady
            ),
            steps
        )
    }

    @Test
    fun checklistHasNoFifthOrDuplicateRow() {
        assertEquals(4, PreparationChecklistStep.entries.size)
        assertEquals(4, StartupPreparationChecklistAuthority.orderedSteps.size)
        assertEquals(
            StartupPreparationChecklistAuthority.orderedSteps,
            StartupPreparationChecklistAuthority.orderedSteps.distinct()
        )
        val steps = StartupPreparationChecklistAuthority.completedSteps(
            StartupFlowState(
                phase = StartupPhase.EvaluatingCompatibility,
                faceDetected = true,
                communicationPrepared = true,
                calibrationDecisionReady = true
            )
        )
        assertEquals(steps, steps.distinct())
    }

    @Test
    fun checklistStillDerivesFromProductionReadinessNotFromTheScreen() {
        val searching = StartupPreparationChecklistAuthority.completedSteps(
            StartupFlowState(phase = StartupPhase.FaceDetection, faceDetected = false)
        )
        assertTrue(searching.isEmpty())

        val preparingEarly = StartupPreparationChecklistAuthority.completedSteps(
            StartupFlowState(phase = StartupPhase.EvaluatingCompatibility, faceDetected = true)
        )
        assertEquals(
            listOf(
                PreparationChecklistStep.EyeTracking,
                PreparationChecklistStep.LoadingProfile
            ),
            preparingEarly
        )
        assertFalse(preparingEarly.contains(PreparationChecklistStep.CalibrationReady))
    }

    @Test
    fun controllerMarksLastTwoChecksBeforeLeavingPreparing() {
        val harness = Harness(storedConfidence = 0.2f)
        harness.beginPreparing()
        assertEquals(StartupPhase.EvaluatingCompatibility, harness.controller.state.phase)

        harness.feedFrames()
        harness.scheduler.advanceTo(101_200L)

        val state = harness.controller.state
        assertEquals(StartupPhase.EvaluatingCompatibility, state.phase)
        assertTrue(state.communicationPrepared)
        assertTrue(state.calibrationDecisionReady)
        assertTrue(state.preparationComplete)
        assertEquals(4, StartupPreparationChecklistAuthority.completedSteps(state).size)
    }

    @Test
    fun preparationCompletedDoesNotRoutePhaseByItself() {
        val before = StartupFlowState(
            phase = StartupPhase.EvaluatingCompatibility,
            faceDetected = true
        )
        val after = StartupFlowAuthority.reduce(
            before,
            StartupEvent.PreparationCompleted(CalibrationCompatibilityLevel.Low)
        )
        assertEquals(StartupPhase.EvaluatingCompatibility, after.phase)
        assertTrue(after.communicationPrepared)
        assertTrue(after.calibrationDecisionReady)
    }

    // ---------------------------------------------------------------- timing

    @Test
    fun calibrationCannotBeginBeforeFiveSecondsEvenWhenPreparationIsInstant() {
        val harness = Harness(storedConfidence = 0.2f)
        harness.beginPreparing()
        harness.feedFrames()

        harness.scheduler.advanceTo(101_200L)
        assertEquals(StartupPhase.EvaluatingCompatibility, harness.controller.state.phase)

        harness.scheduler.advanceTo(104_999L)
        assertEquals(StartupPhase.EvaluatingCompatibility, harness.controller.state.phase)
        assertEquals(0, harness.quickCalibrationEntries)
    }

    @Test
    fun calibrationBeginsOnceMinimumDisplayTimeElapses() {
        val harness = Harness(storedConfidence = 0.2f)
        harness.beginPreparing()
        harness.feedFrames()
        harness.scheduler.advanceTo(101_200L)

        harness.scheduler.advanceTo(105_000L)
        assertEquals(StartupPhase.QuickCalibration, harness.controller.state.phase)
        assertEquals(1, harness.quickCalibrationEntries)
    }

    @Test
    fun preparationSlowerThanFiveSecondsIsNotInterruptedAndRoutesImmediately() {
        val harness = Harness(storedConfidence = 0.2f)
        harness.beginPreparing()
        harness.feedFrames()

        // Preparation work only lands at 7s. Nothing may route before it genuinely finishes.
        harness.scheduler.now = 107_000L
        assertEquals(StartupPhase.EvaluatingCompatibility, harness.controller.state.phase)
        assertFalse(harness.controller.state.preparationComplete)

        harness.scheduler.runDue()
        assertEquals(StartupPhase.QuickCalibration, harness.controller.state.phase)
        assertEquals(1, harness.quickCalibrationEntries)
        assertEquals(0, harness.scheduler.pendingCount)
    }

    @Test
    fun repeatedStateUpdatesDoNotRestartTheTimerOrDuplicateNavigation() {
        val harness = Harness(storedConfidence = 0.2f)
        harness.beginPreparing()
        harness.feedFrames()
        harness.scheduler.advanceTo(101_200L)

        // One hand-off timer only, regardless of how many frames/state publishes arrive.
        assertEquals(1, harness.scheduler.pendingCount)
        repeat(5) {
            harness.feedFrames(4)
            harness.scheduler.advanceTo(harness.scheduler.now + 500L)
        }
        harness.scheduler.advanceTo(110_000L)

        assertEquals(StartupPhase.QuickCalibration, harness.controller.state.phase)
        assertEquals(1, harness.quickCalibrationEntries)
        assertEquals(0, harness.scheduler.pendingCount)
    }

    @Test
    fun leavingPreparingInvalidatesTheOldHandoff() {
        val harness = Harness(storedConfidence = 0.2f)
        harness.beginPreparing()
        harness.feedFrames()
        harness.scheduler.advanceTo(101_200L)
        harness.scheduler.advanceTo(105_000L)
        assertEquals(StartupPhase.QuickCalibration, harness.controller.state.phase)

        // A stale hand-off arriving after the phase moved on must be a no-op.
        harness.scheduler.advanceTo(120_000L)
        assertEquals(StartupPhase.QuickCalibration, harness.controller.state.phase)
        assertEquals(1, harness.quickCalibrationEntries)
    }

    @Test
    fun restartingStartupCreatesExactlyOneNewTimer() {
        val harness = Harness(storedConfidence = 0.2f)
        harness.beginPreparing()
        harness.feedFrames()
        harness.scheduler.advanceTo(101_200L)

        harness.controller.start()
        assertFalse(harness.controller.state.communicationPrepared)
        assertFalse(harness.controller.state.calibrationDecisionReady)
        assertEquals(StartupPhase.FaceDetection, harness.controller.state.phase)

        harness.scheduler.advanceTo(106_000L)
        harness.controller.onFacePresence(true)
        assertEquals(StartupPhase.EvaluatingCompatibility, harness.controller.state.phase)
        harness.feedFrames()

        harness.scheduler.advanceTo(107_200L)
        assertEquals(StartupPhase.EvaluatingCompatibility, harness.controller.state.phase)
        harness.scheduler.advanceTo(111_000L)
        assertEquals(StartupPhase.QuickCalibration, harness.controller.state.phase)
        assertEquals(1, harness.quickCalibrationEntries)
        assertEquals(0, harness.scheduler.pendingCount)
    }

    @Test
    fun staleTimerFromAPreviousSessionCannotShortenTheNewGate() {
        val harness = Harness(storedConfidence = 0.2f)
        harness.beginPreparing()
        harness.feedFrames()
        harness.scheduler.advanceTo(101_200L)
        assertEquals(1, harness.scheduler.pendingCount)

        // Restart immediately: the previous hand-off is still queued for 105_000.
        harness.controller.start()
        harness.controller.onFacePresence(true)
        harness.feedFrames()
        harness.scheduler.advanceTo(102_400L)

        harness.scheduler.advanceTo(105_000L)
        assertEquals(StartupPhase.EvaluatingCompatibility, harness.controller.state.phase)

        harness.scheduler.advanceTo(106_200L)
        assertEquals(StartupPhase.QuickCalibration, harness.controller.state.phase)
        assertEquals(1, harness.quickCalibrationEntries)
    }

    @Test
    fun highCompatibilityAlsoRespectsMinimumDisplayTimeAndStillCalibrates() {
        val harness = Harness(storedConfidence = 0.95f)
        harness.beginPreparing()
        harness.feedFrames(12)
        harness.scheduler.advanceTo(101_200L)
        assertEquals(StartupPhase.EvaluatingCompatibility, harness.controller.state.phase)

        harness.scheduler.advanceTo(104_900L)
        assertEquals(StartupPhase.EvaluatingCompatibility, harness.controller.state.phase)

        harness.scheduler.advanceTo(106_000L)
        assertEquals(StartupPhase.QuickCalibration, harness.controller.state.phase)
        assertEquals(0, harness.eyeTrackingReadyEntries)
    }

    @Test
    fun minimumDisplayWindowIsFiveSecondsAndPreparationWorkIsNotDelayed() {
        assertEquals(5000L, StartupSessionController.PREPARING_MIN_DISPLAY_MS)
        assertEquals(1200L, StartupSessionController.COMPATIBILITY_SAMPLE_MS)
    }

    // ---------------------------------------------------------------- regression

    @Test
    fun cameraGuidanceCardAndWordingRemainOnThePreparingScreen() {
        val source = startupFlowSource()
        assertTrue(source.contains("CameraGuidanceCard(uiStrings = uiStrings, tight = tight)"))
        assertTrue(source.contains("For the most accurate eye tracking"))
        assertTrue(source.contains("Look directly at the camera."))
        assertTrue(
            source.contains(
                "LISA detects blinks and winks most accurately when you look towards the front " +
                    "camera while performing eye gestures."
            )
        )
        assertTrue(
            source.contains(
                "Read the instruction on the screen first, then look back at the camera before " +
                    "blinking or winking."
            )
        )
    }

    @Test
    fun preparingScreenKeepsPortraitFitWithoutScrolling() {
        val source = startupFlowSource()
        val screen = source.substringAfter("private fun FaceDetectionStartupScreen(")
            .substringBefore("\n@Composable")
        assertFalse(screen.contains("verticalScroll"))
        assertTrue(screen.contains("if (evaluating) Arrangement.Top else Arrangement.Center"))
        // Short portrait phones get a tighter variant instead of a clipped fourth row.
        assertTrue(screen.contains("BoxWithConstraints"))
        assertTrue(screen.contains("maxHeight < CompactPreparingHeight"))
        assertTrue(source.contains("private val CompactPreparingHeight = 470.dp"))
    }

    @Test
    fun eyeTrackingAndCalibrationAuthoritiesAreUnchanged() {
        assertEquals(0.75f, EyeCalibrationAuthority.HighConfidenceMinimum, 0.0001f)
        assertEquals(7L * 24L * 60L * 60L * 1000L, EyeCalibrationAuthority.MaxCalibrationAgeMs)
        assertTrue(EyeCalibrationAuthority.thresholdsLookValid(calibration(0.9f, 1_000L)))
        assertEquals(
            listOf(
                QuickCalibrationStep.LookNaturally,
                QuickCalibrationStep.BlinkThreeTimes,
                QuickCalibrationStep.LeftWinkTwice,
                QuickCalibrationStep.RightWinkTwice,
                QuickCalibrationStep.CalibrationComplete
            ),
            QuickCalibrationStep.entries.toList()
        )
        assertEquals(1500L, StartupSessionController.LOOK_NATURALLY_MIN_MS)
        assertEquals(1200L, StartupSessionController.SUCCESS_DISPLAY_MS)
        assertEquals(400L, StartupSessionController.READY_HANDOFF_MS)
    }

    @Test
    fun preparingGateDoesNotUseBlockingSleepOrHandlers() {
        val controllerSource = read(
            "app/src/main/java/com/idworx/lisa/features/intelligentstartup/StartupSessionController.kt"
        )
        assertFalse(controllerSource.contains("Thread.sleep"))
        assertFalse(controllerSource.contains("Handler("))
        assertTrue(controllerSource.contains("PREPARING_MIN_DISPLAY_MS"))
        assertTrue(controllerSource.contains("preparingHandoffScheduled"))
        assertTrue(controllerSource.contains("preparingSession"))
    }

    private fun read(path: String): String =
        ZeroTouchFileProbe.readProjectFile(path) ?: error("Missing source: $path")

    private fun startupFlowSource(): String = read(
        "app/src/main/java/com/idworx/lisa/features/intelligentstartup/ui/IntelligentStartupFlow.kt"
    )
}
