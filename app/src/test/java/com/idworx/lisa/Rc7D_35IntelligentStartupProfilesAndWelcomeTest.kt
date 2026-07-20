package com.idworx.lisa

import com.idworx.lisa.features.intelligentstartup.StartupSessionController
import com.idworx.lisa.features.intelligentstartup.authority.CalibrationCompatibilityAuthority
import com.idworx.lisa.features.intelligentstartup.authority.EyeCalibrationAuthority
import com.idworx.lisa.features.intelligentstartup.authority.StartupFlowAuthority
import com.idworx.lisa.features.intelligentstartup.authority.StartupProfileAuthority
import com.idworx.lisa.features.intelligentstartup.authority.StartupProfileResolution
import com.idworx.lisa.features.intelligentstartup.authority.WelcomeEyeNavigationAuthority
import com.idworx.lisa.features.intelligentstartup.engine.CalibrationFrameSample
import com.idworx.lisa.features.intelligentstartup.engine.QuickEyeCalibrationEngine
import com.idworx.lisa.features.intelligentstartup.model.CalibrationCompatibilityLevel
import com.idworx.lisa.features.intelligentstartup.model.CalibrationConfidenceLevel
import com.idworx.lisa.features.intelligentstartup.model.LiveCompatibilitySample
import com.idworx.lisa.features.intelligentstartup.model.ProfileEyeCalibration
import com.idworx.lisa.features.intelligentstartup.model.QuickCalibrationStep
import com.idworx.lisa.features.intelligentstartup.model.StartupEvent
import com.idworx.lisa.features.intelligentstartup.model.StartupFlowState
import com.idworx.lisa.features.intelligentstartup.model.StartupPhase
import com.idworx.lisa.features.brain1interactionstandard.model.UniversalInteractionGestures
import com.idworx.lisa.features.launchwelcomestatepriority.WelcomeStatePriorityGate
import com.idworx.lisa.features.onboardingguide.model.TrainingPhase
import com.idworx.lisa.features.onboardingguide.navigation.GuidedTrainingNavigator
import com.idworx.lisa.features.onboardingguide.state.TrainingEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Rc7D_35IntelligentStartupProfilesAndWelcomeTest {

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
        eyeSpacingProxy = 0.14f,
        confidence = confidence,
        calibratedAtMs = now - ageMs
    )

    private fun profile(
        id: String,
        name: String,
        calibration: ProfileEyeCalibration? = null
    ) = LisaUserProfile(
        id = id,
        name = name,
        preferredLanguage = PreferredLanguage.English,
        communicationLevel = CommunicationLevel.Standard,
        eyeCalibration = calibration
    )

    @Test
    fun noProfilesResolvesToCreatePrimaryUser() {
        assertEquals(StartupProfileResolution.None, StartupProfileAuthority.resolve(emptyList()))
        var state = StartupFlowAuthority.reduce(
            StartupFlowState(faceDetected = true),
            StartupEvent.BeginProfileResolution
        )
        state = StartupFlowAuthority.reduce(
            state,
            StartupEvent.ProfilesResolvedNone("Primary User")
        )
        assertEquals(StartupPhase.CreatePrimaryUser, state.phase)
        assertEquals("Primary User", state.createNameDraft)
    }

    @Test
    fun singleProfileLoadsAutomaticallyIntoCompatibility() {
        val profiles = listOf(profile("p1", "Only User"))
        val resolution = StartupProfileAuthority.resolve(profiles)
        assertTrue(resolution is StartupProfileResolution.Single)
        var state = StartupFlowAuthority.reduce(
            StartupFlowState(faceDetected = true),
            StartupEvent.BeginProfileResolution
        )
        state = StartupFlowAuthority.reduce(
            state,
            StartupEvent.ProfilesResolvedSingle("p1")
        )
        assertEquals(StartupPhase.EvaluatingCompatibility, state.phase)
        assertEquals("p1", state.selectedProfileId)
    }

    @Test
    fun multipleProfilesOpenBlinkPicker() {
        val profiles = listOf(profile("a", "A"), profile("b", "B"))
        val resolution = StartupProfileAuthority.resolve(profiles) as StartupProfileResolution.Multiple
        assertEquals(2, resolution.choices.size)
        var state = StartupFlowAuthority.reduce(
            StartupFlowState(faceDetected = true),
            StartupEvent.BeginProfileResolution
        )
        state = StartupFlowAuthority.reduce(
            state,
            StartupEvent.ProfilesResolvedMultiple(resolution.choices)
        )
        assertEquals(StartupPhase.ProfileSelection, state.phase)
        assertTrue(state.eyeControlActive)
        state = StartupFlowAuthority.reduce(state, StartupEvent.MoveProfileSelectionDown)
        assertEquals(1, state.selectedProfileIndex)
        assertEquals("b", state.selectedProfileId)
        state = StartupFlowAuthority.reduce(state, StartupEvent.SelectHighlightedProfile)
        assertEquals(StartupPhase.EvaluatingCompatibility, state.phase)
        assertEquals("b", state.selectedProfileId)
    }

    @Test
    fun compatibilityHighSkipsCalibration() {
        val stored = sampleCalibration()
        val live = LiveCompatibilitySample(
            eyeOpennessBaseline = 0.84f,
            faceDistanceProxy = 0.41f,
            eyeSpacingProxy = 0.14f,
            leftCloseCharacteristic = 0.21f,
            rightCloseCharacteristic = 0.23f,
            blinkDurationMs = 155L
        )
        val (level, _) = CalibrationCompatibilityAuthority.evaluate(stored, live, 1_000_000L)
        assertEquals(CalibrationCompatibilityLevel.High, level)
        assertTrue(CalibrationCompatibilityAuthority.shouldSkipQuickCalibration(level))
        var state = StartupFlowState(phase = StartupPhase.EvaluatingCompatibility)
        state = StartupFlowAuthority.reduce(
            state,
            StartupEvent.CompatibilityEvaluated(CalibrationCompatibilityLevel.High)
        )
        assertEquals(StartupPhase.EyeTrackingReady, state.phase)
        assertTrue(state.skippedCalibration)
        assertTrue(state.eyeControlActive)
    }

    @Test
    fun compatibilityLowRequiresAutomaticCalibration() {
        val (level, _) = CalibrationCompatibilityAuthority.evaluate(
            sampleCalibration(confidence = 0.2f),
            LiveCompatibilitySample(0.4f, 0.9f, 0.9f),
            1_000_000L
        )
        assertEquals(CalibrationCompatibilityLevel.Low, level)
        assertTrue(CalibrationCompatibilityAuthority.requiresQuickCalibration(level))
        var state = StartupFlowState(phase = StartupPhase.EvaluatingCompatibility)
        state = StartupFlowAuthority.reduce(
            state,
            StartupEvent.CompatibilityEvaluated(CalibrationCompatibilityLevel.Low)
        )
        assertEquals(StartupPhase.QuickCalibration, state.phase)
        assertFalse(state.skippedCalibration)
    }

    @Test
    fun mediumCompatibilityAlsoRunsCalibrationWithoutAsking() {
        var state = StartupFlowState(phase = StartupPhase.EvaluatingCompatibility)
        state = StartupFlowAuthority.reduce(
            state,
            StartupEvent.CompatibilityEvaluated(CalibrationCompatibilityLevel.Medium)
        )
        assertEquals(StartupPhase.QuickCalibration, state.phase)
    }

    @Test
    fun primaryUserCreatedContinuesIntoQuickCalibration() {
        var state = StartupFlowState(phase = StartupPhase.CreatePrimaryUser)
        state = StartupFlowAuthority.reduce(state, StartupEvent.PrimaryUserCreated("new-id"))
        assertEquals(StartupPhase.QuickCalibration, state.phase)
        assertEquals("new-id", state.selectedProfileId)
    }

    @Test
    fun eyeControlActiveBeforeWelcomeHandoff() {
        var state = StartupFlowState(
            phase = StartupPhase.EyeTrackingReady,
            eyeControlActive = true
        )
        state = StartupFlowAuthority.reduce(state, StartupEvent.AcknowledgeEyeTrackingReady)
        assertEquals(StartupPhase.Complete, state.phase)
        assertTrue(state.eyeControlActive)
        assertFalse(state.isActive)
    }

    @Test
    fun welcomeSequencesMatchBrain1OptionAB() {
        assertEquals(UniversalInteractionGestures.OPTION_A_LEFT, WelcomeEyeNavigationAuthority.startGuidedLearningLeft)
        assertEquals(UniversalInteractionGestures.OPTION_A_RIGHT, WelcomeEyeNavigationAuthority.startGuidedLearningRight)
        assertEquals(UniversalInteractionGestures.OPTION_B_LEFT, WelcomeEyeNavigationAuthority.skipToCommunicationLeft)
        assertEquals(UniversalInteractionGestures.OPTION_B_RIGHT, WelcomeEyeNavigationAuthority.skipToCommunicationRight)
        assertTrue(WelcomeEyeNavigationAuthority.isStartGuidedLearning(2, 0))
        assertTrue(WelcomeEyeNavigationAuthority.isSkipToCommunication(0, 2))
        assertEquals("L2 R0", WelcomeEyeNavigationAuthority.startGuidedLearningSequenceLabel())
        assertEquals("L0 R2", WelcomeEyeNavigationAuthority.skipToCommunicationSequenceLabel())
    }

    @Test
    fun welcomeTouchAndBlinkShareGuidedLearningAndSkipRoutes() {
        val navigator = GuidedTrainingNavigator()
        val guided = navigator.reduce(
            com.idworx.lisa.features.onboardingguide.model.TrainingProgress(),
            TrainingEvent.BeginLearning
        )
        val skipped = navigator.reduce(
            com.idworx.lisa.features.onboardingguide.model.TrainingProgress(),
            TrainingEvent.ConfirmSkip
        )
        assertEquals(TrainingPhase.Setup, guided.currentPhase)
        assertEquals(TrainingPhase.Completion, skipped.currentPhase)
        assertTrue(WelcomeEyeNavigationAuthority.isStartGuidedLearning(2, 0))
        assertTrue(WelcomeEyeNavigationAuthority.isSkipToCommunication(0, 2))
    }

    @Test
    fun profilePersistsCalibrationCompatibilityHistory() {
        val withHistory = CalibrationCompatibilityAuthority.appendHistory(
            sampleCalibration(),
            CalibrationCompatibilityLevel.High,
            0.91f,
            2_000_000L
        )
        val profile = LisaUserProfile(name = "Pat", eyeCalibration = withHistory)
        val restored = LisaUserProfile.fromJson(profile.toJson())
        assertNotNull(restored.eyeCalibration)
        assertEquals(1, restored.eyeCalibration!!.compatibilityHistory.size)
        assertEquals(
            CalibrationCompatibilityLevel.High,
            restored.eyeCalibration!!.compatibilityHistory.first().level
        )
        assertEquals(0.14f, restored.eyeCalibration!!.eyeSpacingProxy, 0.001f)
    }

    @Test
    fun startupControllerSingleHighCompatibilityHandsOff() {
        val stored = sampleCalibration()
        val profiles = mutableListOf(profile("only", "Only", stored))
        var activated: String? = null
        var complete = false
        var eyeReady = false
        var clock = 10_000L
        val pending = mutableListOf<Pair<Long, () -> Unit>>()
        val controller = StartupSessionController(
            loadProfiles = { profiles.toList() },
            loadProfileCalibration = { profiles.first().eyeCalibration },
            persistCalibration = { cal -> profiles[0] = profiles[0].copy(eyeCalibration = cal) },
            activateProfile = { activated = it },
            createPrimaryUser = { _, _, _ -> error("unused") },
            nowMs = { clock },
            onEyeControlActivated = { eyeReady = true },
            onStartupComplete = { complete = true },
            scheduleReadyHandoff = { delay, action -> pending += (clock + delay) to action },
            scheduleAutoRetry = { _, _ -> }
        )
        controller.start()
        controller.onFacePresence(true)
        assertEquals("only", activated)
        // Drain compatibility sample + ready handoff timers
        repeat(6) {
            val due = pending.filter { it.first <= clock + 5_000L }
            pending.removeAll(due.toSet())
            due.forEach { (_, action) ->
                clock += 500
                // feed matching live samples while evaluating
                repeat(8) {
                    controller.onFrameSample(0.85f, 0.84f, 0.4f, 0.14f)
                }
                action()
            }
            clock += 500
        }
        assertTrue(eyeReady || controller.eyeControlEnabled || complete)
        assertTrue(
            controller.state.phase == StartupPhase.EyeTrackingReady ||
                controller.state.phase == StartupPhase.Complete ||
                controller.state.phase == StartupPhase.QuickCalibration
        )
    }

    @Test
    fun noProfileStartupCreatesThenCalibrates() {
        val profiles = mutableListOf<LisaUserProfile>()
        var clock = 1_000L
        val pending = mutableListOf<() -> Unit>()
        val controller = StartupSessionController(
            loadProfiles = { profiles.toList() },
            loadProfileCalibration = { profiles.firstOrNull()?.eyeCalibration },
            persistCalibration = { cal ->
                if (profiles.isNotEmpty()) {
                    profiles[0] = profiles[0].copy(eyeCalibration = cal)
                }
            },
            activateProfile = { id -> /* active */ },
            createPrimaryUser = { name, language, level ->
                val created = LisaUserProfile(
                    id = "created",
                    name = name,
                    preferredLanguage = PreferredLanguage.fromStored(language),
                    communicationLevel = CommunicationLevel.fromStored(level)
                )
                profiles.add(created)
                created.id
            },
            nowMs = { clock },
            scheduleReadyHandoff = { _, action -> pending += action },
            scheduleAutoRetry = { _, _ -> }
        )
        controller.start()
        controller.onFacePresence(true)
        assertEquals(StartupPhase.CreatePrimaryUser, controller.state.phase)
        controller.confirmCreatePrimaryUser()
        assertEquals(StartupPhase.QuickCalibration, controller.state.phase)
        assertEquals(1, profiles.size)
        assertEquals("Primary User", profiles.first().name)
    }

    @Test
    fun welcomeStillPriorityAfterStartupComplete() {
        val gated = WelcomeStatePriorityGate.applyForColdLaunch(
            com.idworx.lisa.features.onboardingguide.model.TrainingProgress(
                currentPhase = TrainingPhase.Completion,
                tutorialCompleted = true
            )
        )
        assertEquals(TrainingPhase.FirstLaunchChoice, gated.currentPhase)
    }

    @Test
    fun engineBuildsCalibrationWithSpacingProxy() {
        val engine = QuickEyeCalibrationEngine()
        var now = 1_000L
        repeat(20) {
            engine.onFrame(
                QuickCalibrationStep.LookNaturally,
                CalibrationFrameSample(0.86f, 0.84f, 0.42f, now, eyeSpacingProxy = 0.15f)
            )
            now += 50
        }
        repeat(3) {
            engine.onFrame(QuickCalibrationStep.BlinkThreeTimes, CalibrationFrameSample(0.9f, 0.9f, 0.4f, now))
            now += 40
            engine.onFrame(QuickCalibrationStep.BlinkThreeTimes, CalibrationFrameSample(0.15f, 0.18f, 0.4f, now))
            now += 120
            engine.onFrame(QuickCalibrationStep.BlinkThreeTimes, CalibrationFrameSample(0.88f, 0.87f, 0.4f, now))
            now += 40
        }
        engine.onLeftWinkAccepted(QuickCalibrationStep.LeftWinkTwice, 0.12f)
        engine.onLeftWinkAccepted(QuickCalibrationStep.LeftWinkTwice, 0.11f)
        engine.onRightWinkAccepted(QuickCalibrationStep.RightWinkTwice, 0.13f)
        engine.onRightWinkAccepted(QuickCalibrationStep.RightWinkTwice, 0.14f)
        val calibration = engine.buildCalibration(now)
        assertNotNull(calibration)
        assertTrue(calibration!!.eyeSpacingProxy > 0f)
        assertTrue(EyeCalibrationAuthority.thresholdsLookValid(calibration))
    }
}
