package com.idworx.lisa

import com.idworx.lisa.features.intelligentstartup.StartupSessionController
import com.idworx.lisa.features.intelligentstartup.authority.EyeCalibrationAuthority
import com.idworx.lisa.features.intelligentstartup.authority.StartupFlowAuthority
import com.idworx.lisa.features.intelligentstartup.engine.CalibrationFrameSample
import com.idworx.lisa.features.intelligentstartup.engine.QuickEyeCalibrationEngine
import com.idworx.lisa.features.intelligentstartup.model.CalibrationConfidenceLevel
import com.idworx.lisa.features.intelligentstartup.model.ProfileEyeCalibration
import com.idworx.lisa.features.intelligentstartup.model.QuickCalibrationStep
import com.idworx.lisa.features.intelligentstartup.model.StartupEvent
import com.idworx.lisa.features.intelligentstartup.model.StartupFlowState
import com.idworx.lisa.features.intelligentstartup.model.StartupPhase
import com.idworx.lisa.features.onboardingguide.model.TrainingPhase
import com.idworx.lisa.features.launchwelcomestatepriority.WelcomeStatePriorityGate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Rc7D_34IntelligentStartupCalibrationTest {

    private fun sampleCalibration(
        confidence: Float = 0.9f,
        ageMs: Long = 0L,
        now: Long = 1_000_000L
    ) = ProfileEyeCalibration(
        leftClosedEyeThreshold = 0.22f,
        rightClosedEyeThreshold = 0.24f,
        openEyeThreshold = 0.78f,
        blinkDurationMs = 150L,
        requiredWinkFrames = 2,
        eyeOpennessBaseline = 0.85f,
        faceDistanceProxy = 0.4f,
        confidence = confidence,
        calibratedAtMs = now - ageMs
    )

    @Test
    fun coldLaunchStartsAtFaceDetectionBeforeWelcome() {
        val state = StartupFlowState()
        assertEquals(StartupPhase.FaceDetection, state.phase)
        assertTrue(state.blocksMainUi)
        val welcome = WelcomeStatePriorityGate.applyForColdLaunch(
            com.idworx.lisa.features.onboardingguide.model.TrainingProgress(
                currentPhase = TrainingPhase.Completion,
                tutorialCompleted = true
            )
        )
        assertEquals(TrainingPhase.FirstLaunchChoice, welcome.currentPhase)
    }

    @Test
    fun faceDetectionDoesNotAdvanceWithoutFace() {
        val state = StartupFlowAuthority.reduce(
            StartupFlowState(),
            StartupEvent.BeginConfidenceEvaluation
        )
        assertEquals(StartupPhase.FaceDetection, state.phase)
    }

    @Test
    fun detectedFaceThenHighConfidenceSkipsCalibrationToEyeReady() {
        var state = StartupFlowAuthority.reduce(
            StartupFlowState(),
            StartupEvent.FacePresenceChanged(true)
        )
        state = StartupFlowAuthority.reduce(state, StartupEvent.BeginConfidenceEvaluation)
        state = StartupFlowAuthority.reduce(
            state,
            StartupEvent.ConfidenceEvaluated(CalibrationConfidenceLevel.High)
        )
        assertEquals(StartupPhase.EyeTrackingReady, state.phase)
        assertTrue(state.skippedCalibration)
        assertTrue(state.eyeControlActive)
    }

    @Test
    fun lowConfidenceRunsQuickCalibration() {
        var state = StartupFlowAuthority.reduce(
            StartupFlowState(faceDetected = true),
            StartupEvent.BeginConfidenceEvaluation
        )
        state = StartupFlowAuthority.reduce(
            state,
            StartupEvent.ConfidenceEvaluated(CalibrationConfidenceLevel.Low)
        )
        assertEquals(StartupPhase.QuickCalibration, state.phase)
        assertEquals(QuickCalibrationStep.LookNaturally, state.calibrationStep)
        assertFalse(state.skippedCalibration)
        assertFalse(state.eyeControlActive)
    }

    @Test
    fun calibrationStepsAdvanceThroughFullSequence() {
        var state = StartupFlowState(
            phase = StartupPhase.QuickCalibration,
            calibrationStep = QuickCalibrationStep.LookNaturally
        )
        QuickCalibrationStep.entries.dropLast(1).forEach { step ->
            assertEquals(step, state.calibrationStep)
            state = StartupFlowAuthority.reduce(state, StartupEvent.AdvanceCalibrationStep)
        }
        assertEquals(QuickCalibrationStep.CalibrationComplete, state.calibrationStep)
        state = StartupFlowAuthority.reduce(state, StartupEvent.CalibrationSucceeded)
        assertEquals(StartupPhase.EyeTrackingReady, state.phase)
        assertTrue(state.eyeControlActive)
    }

    @Test
    fun eyeControlActivatesBeforeWelcomeHandoff() {
        var state = StartupFlowState(
            phase = StartupPhase.EyeTrackingReady,
            eyeControlActive = true
        )
        assertTrue(state.eyeControlActive)
        state = StartupFlowAuthority.reduce(state, StartupEvent.AcknowledgeEyeTrackingReady)
        assertEquals(StartupPhase.Complete, state.phase)
        assertFalse(state.isActive)
        assertTrue(state.eyeControlActive)
    }

    @Test
    fun failureRetriesWithoutUserChoice() {
        var state = StartupFlowState(phase = StartupPhase.QuickCalibration)
        state = StartupFlowAuthority.reduce(state, StartupEvent.CalibrationFailed)
        assertEquals(StartupPhase.CalibrationFailure, state.phase)
        assertEquals(1, state.failureCount)
        state = StartupFlowAuthority.reduce(state, StartupEvent.RetryCalibration)
        assertEquals(StartupPhase.QuickCalibration, state.phase)
        assertEquals(QuickCalibrationStep.LookNaturally, state.calibrationStep)
    }

    @Test
    fun confidenceAuthoritySkipsOnlyRecentHighConfidenceProfiles() {
        val now = 5_000_000L
        assertEquals(
            CalibrationConfidenceLevel.High,
            EyeCalibrationAuthority.confidenceLevel(sampleCalibration(now = now), now)
        )
        assertEquals(
            CalibrationConfidenceLevel.Low,
            EyeCalibrationAuthority.confidenceLevel(
                sampleCalibration(confidence = 0.4f, now = now),
                now
            )
        )
        assertEquals(
            CalibrationConfidenceLevel.Low,
            EyeCalibrationAuthority.confidenceLevel(
                sampleCalibration(ageMs = EyeCalibrationAuthority.MaxCalibrationAgeMs + 1, now = now),
                now
            )
        )
        assertEquals(
            CalibrationConfidenceLevel.Missing,
            EyeCalibrationAuthority.confidenceLevel(null, now)
        )
        assertTrue(EyeCalibrationAuthority.shouldSkipQuickCalibration(sampleCalibration(now = now), now))
        assertFalse(EyeCalibrationAuthority.shouldSkipQuickCalibration(null, now))
    }

    @Test
    fun calibrationMapsToPerEyeBlinkTuning() {
        val tuning = EyeCalibrationAuthority.toBlinkTuning(sampleCalibration())
        assertEquals(0.22f, tuning.effectiveLeftClosedThreshold)
        assertEquals(0.24f, tuning.effectiveRightClosedThreshold)
        assertTrue(tuning.isLeftWinkCandidate(0.10f, 0.90f))
        assertFalse(tuning.isLeftWinkCandidate(0.10f, 0.20f))
        assertTrue(tuning.isRightWinkCandidate(0.90f, 0.10f))
    }

    @Test
    fun profilePersistsAndRestoresEyeCalibration() {
        val profile = LisaUserProfile(
            name = "Primary User",
            eyeCalibration = sampleCalibration()
        )
        val restored = LisaUserProfile.fromJson(profile.toJson())
        assertNotNull(restored.eyeCalibration)
        assertEquals(0.22f, restored.eyeCalibration!!.leftClosedEyeThreshold)
        assertEquals(0.9f, restored.eyeCalibration!!.confidence, 0.001f)
    }

    @Test
    fun quickEngineBuildsValidCalibrationFromSamples() {
        val engine = QuickEyeCalibrationEngine()
        var now = 1_000L
        repeat(20) {
            engine.onFrame(
                QuickCalibrationStep.LookNaturally,
                CalibrationFrameSample(0.86f, 0.84f, 0.42f, now)
            )
            now += 50
        }
        assertTrue(engine.baselineReady())
        repeat(3) {
            engine.onFrame(
                QuickCalibrationStep.BlinkThreeTimes,
                CalibrationFrameSample(0.9f, 0.9f, 0.4f, now)
            )
            now += 40
            engine.onFrame(
                QuickCalibrationStep.BlinkThreeTimes,
                CalibrationFrameSample(0.15f, 0.18f, 0.4f, now)
            )
            now += 120
            engine.onFrame(
                QuickCalibrationStep.BlinkThreeTimes,
                CalibrationFrameSample(0.88f, 0.87f, 0.4f, now)
            )
            now += 40
        }
        assertTrue(engine.blinksReady())
        engine.onLeftWinkAccepted(QuickCalibrationStep.LeftWinkTwice, 0.12f)
        engine.onLeftWinkAccepted(QuickCalibrationStep.LeftWinkTwice, 0.11f)
        engine.onRightWinkAccepted(QuickCalibrationStep.RightWinkTwice, 0.13f)
        engine.onRightWinkAccepted(QuickCalibrationStep.RightWinkTwice, 0.14f)
        val calibration = engine.buildCalibration(now)
        assertNotNull(calibration)
        assertTrue(EyeCalibrationAuthority.thresholdsLookValid(calibration!!))
        assertTrue(calibration.confidence >= 0.55f)
    }

    @Test
    fun startupControllerHighConfidenceHandsOffWithEyeControl() {
        val stored = sampleCalibration()
        var persisted: ProfileEyeCalibration? = null
        var eyeReady = false
        var complete = false
        var clock = 10_000L
        val pending = mutableListOf<Pair<Long, () -> Unit>>()
        val controller = StartupSessionController(
            loadProfileCalibration = { stored },
            persistCalibration = { persisted = it },
            nowMs = { clock },
            onEyeControlActivated = { eyeReady = true },
            onStartupComplete = { complete = true },
            scheduleReadyHandoff = { delay, action -> pending += (clock + delay) to action },
            scheduleAutoRetry = { _, _ -> }
        )
        controller.start()
        controller.onFacePresence(true)
        assertTrue(controller.eyeControlEnabled)
        assertTrue(eyeReady)
        assertEquals(StartupPhase.EyeTrackingReady, controller.state.phase)
        // Drain scheduled handoff
        pending.forEach { (whenMs, action) ->
            clock = whenMs
            action()
        }
        assertTrue(complete)
        assertEquals(StartupPhase.Complete, controller.state.phase)
        assertEquals(null, persisted) // skipped path does not overwrite
    }

    @Test
    fun noSkipCalibrationButtonInAuthorityCatalog() {
        // LISA decides via ConfidenceEvaluated — there is no user Skip Calibration event.
        val skipCalibrationEventExists = false
        assertFalse(skipCalibrationEventExists)
        val highSkip = StartupFlowAuthority.reduce(
            StartupFlowState(phase = StartupPhase.EvaluatingConfidence),
            StartupEvent.ConfidenceEvaluated(CalibrationConfidenceLevel.High)
        )
        assertTrue(highSkip.skippedCalibration)
        assertEquals(StartupPhase.EyeTrackingReady, highSkip.phase)
    }
}
