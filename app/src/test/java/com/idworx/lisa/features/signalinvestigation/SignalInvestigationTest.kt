package com.idworx.lisa.features.signalinvestigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.io.path.createTempDirectory

class SignalInvestigationTest {

    @Test
    fun releaseBuilds_cannotAccess() {
        assertFalse(SignalInvestigationAccess.isEntryVisible(false))
        assertFalse(SignalInvestigationAccess.isScreenAllowed(false))
    }

    @Test
    fun tiltUsesRoll_notYaw_asPrimary_advancedEngineeringOnly() {
        assertTrue(SignalPoseGuidance.tiltUsesRoll(SignalPosition.HeadTiltLeft))
        assertTrue(SignalPoseGuidance.tiltUsesRoll(SignalPosition.HeadTiltRight))
        assertTrue(SignalPoseGuidance.tiltPrimaryIsRoll(SignalPosition.HeadTiltLeft, null))
        assertEquals(
            "yaw",
            SignalPoseGuidance.targetsFor(SignalPosition.HeadStraight, null).primaryLabel
        )
    }

    @Test
    fun mirroredLeftRight_guidance_isCorrect() {
        assertTrue(SignalPoseGuidance.userYaw(12f) < 0f)
        assertTrue(SignalPoseGuidance.userRoll(12f) < 0f)
        val leftTarget = SignalPoseGuidance.targetsFor(SignalPosition.HeadTiltLeft, null)
        val inLeft = SignalPoseGuidance.evaluate(
            leftTarget,
            SignalPoseGuidance.LivePose(
                faceDetected = true,
                userYaw = 0f,
                userRoll = -12f,
                faceCenterXPct = 50f,
                faceCenterYPct = 50f,
                faceWidthPct = 30f,
                leftOpen = 0.9f,
                rightOpen = 0.9f
            )
        )
        assertTrue(inLeft.inTarget)
    }

    @Test
    fun standardFlow_doesNotRequireHeadTilt_andRecordsNaturalThenSingleEyes() {
        var now = 5_000_000L
        val spoken = mutableListOf<String>()
        val controller = SignalInvestigationController(
            store = SignalInvestigationStore(createTempDirectory("sig-std").toFile()),
            isDebugBuild = true,
            clockMs = { now },
            speak = { spoken += it },
            ttsAvailableProvider = { true }
        )
        assertTrue(controller.open())
        assertTrue(controller.openConditions(SignalInvestigationMode.Standard))
        assertTrue(controller.startStandardInvestigation())
        assertEquals(
            SignalInvestigationMode.Standard,
            controller.live.investigationMode
        )
        assertEquals(
            StandardInvestigationStep.NaturalPosition,
            controller.live.standardStep
        )
        assertTrue(spoken.any { it.contains("naturally", ignoreCase = true) })

        // Face present — no tilt required
        fun openEyes() {
            controller.onSample(
                0.9f, 0.9f, true, 30f, true, headYaw = 0f, headRoll = 25f, // tilt irrelevant
                faceCenterXPct = 50f, faceCenterYPct = 50f
            )
        }
        openEyes()
        now += SignalStandardAuthority.FACE_STABLE_MS + 50
        openEyes()
        assertEquals(
            SignalInvestigationController.FlowPhase.Recording,
            controller.live.flowPhase
        )
        now += SignalStandardAuthority.NATURAL_OPEN_MS + 50
        assertTrue(controller.onTimedTick(now))
        // After natural → left eye wait
        now += SignalStandardAuthority.COMPLETE_PAUSE_MS + 50
        assertTrue(controller.onTimedTick(now))
        assertEquals(StandardInvestigationStep.LeftEye, controller.live.standardStep)
        assertTrue(spoken.any { it.contains("LEFT", ignoreCase = true) })

        // Close left only
        controller.onSample(0.2f, 0.9f, true, 30f, true, 0f, 0f, 50f, 50f)
        assertEquals(
            SignalInvestigationController.FlowPhase.Recording,
            controller.live.flowPhase
        )
        now += SignalStandardAuthority.SINGLE_EYE_RECORD_MS + 50
        assertTrue(controller.onTimedTick(now))
        assertTrue(spoken.any { it.contains("Thank you", ignoreCase = true) })
        // Reopen
        controller.onSample(0.9f, 0.9f, true, 30f, true, 0f, 0f, 50f, 50f)
        now += SignalStandardAuthority.BOTH_OPEN_HOLD_MS + 50
        controller.onSample(0.9f, 0.9f, true, 30f, true, 0f, 0f, 50f, 50f)
        assertEquals(StandardInvestigationStep.RightEye, controller.live.standardStep)
    }

    @Test
    fun standardFlow_skipsSequences_whenQualityPoor() {
        var now = 6_000_000L
        val controller = SignalInvestigationController(
            store = SignalInvestigationStore(createTempDirectory("sig-skip").toFile()),
            isDebugBuild = true,
            clockMs = { now },
            ttsAvailableProvider = { true }
        )
        controller.open()
        controller.openConditions(SignalInvestigationMode.Standard)
        controller.startStandardInvestigation()

        // Usable face, but open≈closed → poor separation → skip L1/L2
        fun feed(l: Float, r: Float) {
            controller.onSample(l, r, true, 24f, true, 0f, 0f, 50f, 50f)
        }
        feed(0.9f, 0.9f)
        now += SignalStandardAuthority.FACE_STABLE_MS + 20
        feed(0.9f, 0.9f)
        // Keep feeding during natural recording so the step advances
        repeat(8) {
            now += 200
            feed(0.85f, 0.85f)
            controller.onTimedTick(now)
        }
        now += SignalStandardAuthority.NATURAL_OPEN_MS
        controller.onTimedTick(now)
        now += SignalStandardAuthority.COMPLETE_PAUSE_MS + 20
        controller.onTimedTick(now)
        assertEquals(StandardInvestigationStep.LeftEye, controller.live.standardStep)
        // Left close — only a couple of samples so sequence quality gate fails
        feed(0.2f, 0.9f)
        now += SignalStandardAuthority.SINGLE_EYE_RECORD_MS + 20
        controller.onTimedTick(now)
        feed(0.9f, 0.9f)
        now += SignalStandardAuthority.BOTH_OPEN_HOLD_MS + 20
        feed(0.9f, 0.9f)
        assertEquals(StandardInvestigationStep.RightEye, controller.live.standardStep)
        feed(0.9f, 0.2f)
        now += SignalStandardAuthority.SINGLE_EYE_RECORD_MS + 20
        controller.onTimedTick(now)
        feed(0.9f, 0.9f)
        now += SignalStandardAuthority.BOTH_OPEN_HOLD_MS + 20
        feed(0.9f, 0.9f)
        assertEquals(StandardInvestigationStep.NaturalBlink, controller.live.standardStep)
        now += SignalStandardAuthority.NATURAL_BLINK_OBSERVE_MS + 20
        controller.onTimedTick(now)
        assertEquals(
            SignalInvestigationController.UiPhase.Report,
            controller.uiPhase
        )
        val text = controller.fullReportText()
        assertTrue(text.contains("STANDARD INVESTIGATION"))
        assertTrue(text.contains("Skipped"))
        assertTrue(text.contains("ADVANCED ENGINEERING TESTS"))
        assertTrue(text.contains("Not run"))
        assertFalse(text.contains("HEAD POSITION TESTS"))
    }

    @Test
    fun advancedEngineering_stillRequiresStablePoseBeforeRecord() {
        var now = 1_000_000L
        val controller = SignalInvestigationController(
            store = SignalInvestigationStore(createTempDirectory("sig-adv").toFile()),
            isDebugBuild = true,
            clockMs = { now },
            ttsAvailableProvider = { true }
        )
        assertTrue(controller.open())
        assertTrue(controller.openConditions(SignalInvestigationMode.AdvancedEngineering))
        assertTrue(controller.startAdvancedEngineering())
        assertEquals(
            SignalInvestigationMode.AdvancedEngineering,
            controller.live.investigationMode
        )
        // Outside target
        repeat(3) {
            controller.onSample(
                0.9f, 0.9f, true, 30f, true, 0f, 25f, 50f, 50f
            )
            now += 200
        }
        assertEquals(
            SignalInvestigationController.FlowPhase.Guiding,
            controller.live.flowPhase
        )
        controller.onSample(0.9f, 0.9f, true, 30f, true, 0f, 0f, 50f, 50f)
        assertEquals(
            SignalInvestigationController.FlowPhase.Stabilizing,
            controller.live.flowPhase
        )
        now += SignalPoseGuidance.STABLE_REQUIRED_MS + 50
        controller.onSample(0.9f, 0.9f, true, 30f, true, 0f, 0f, 50f, 50f)
        assertEquals(
            SignalInvestigationController.FlowPhase.Prepare,
            controller.live.flowPhase
        )
    }

    @Test
    fun caregiverRecommendations_doNotAskPatientToMove() {
        val diagnoses = SignalStandardAuthority.diagnoseEnvironment(
            faceDetected = true,
            faceWidthPct = 15f,
            nullPercent = 20f,
            leftSep = 0.05f,
            rightSep = 0.05f,
            lighting = LightingCondition.Outdoor,
            glasses = GlassesCondition.YES
        )
        val recs = SignalStandardAuthority.caregiverRecommendations(diagnoses, 15f)
        assertTrue(recs.any { it.contains("caregiver", ignoreCase = true) })
        assertTrue(recs.any { it.contains("still", ignoreCase = true) })
        assertFalse(recs.any { it.lowercase().startsWith("move closer") })
    }

    @Test
    fun winkSequence_helpers_matchL1R1_andL2R2() {
        assertTrue(SignalStandardAuthority.isL1R1(listOf("L", "R")))
        assertFalse(SignalStandardAuthority.isL1R1(listOf("R", "L")))
        assertTrue(SignalStandardAuthority.isL2R2(listOf("L", "L", "R", "R")))
        assertFalse(SignalStandardAuthority.isL2R2(listOf("L", "R")))
    }

    @Test
    fun ttsFailure_isReported_andBlocksStartUntilVisualConfirmed() {
        val controller = SignalInvestigationController(
            store = SignalInvestigationStore(createTempDirectory("sig-tts").toFile()),
            isDebugBuild = true,
            ttsAvailableProvider = { false }
        )
        controller.open()
        controller.openConditions(SignalInvestigationMode.Standard)
        assertFalse(controller.startInvestigation())
        assertTrue(controller.live.ttsWarning)
        assertTrue(controller.confirmVisualOnlyWithoutTts())
        assertTrue(controller.startInvestigation())
    }

    @Test
    fun report_includesStandardSection_andPoseControlMetricsStillFormat() {
        val stats = SignalInvestigationReportAuthority.buildPositionStats(
            position = SignalPosition.HeadTiltLeft,
            openLeft = List(20) { 0.92f },
            openRight = List(20) { 0.93f },
            closedLeft = List(15) { 0.28f },
            closedRight = List(15) { 0.30f },
            framesSeen = 40,
            framesRejected = 3,
            nullCount = 0,
            faceWidths = listOf(30f),
            yaws = listOf(0f),
            rolls = listOf(-12f),
            faceCenterYs = listOf(50f),
            targetPoseDescription = "Tilt left",
            targetRangeLabel = "roll -15..-10",
            timeToReachTargetMs = 3500,
            stableInTargetPercent = 94f,
            samplesRejectedPoseMismatch = 2,
            measurementRepeated = false,
            voiceGuidanceEventCount = 7,
            conditionValid = true,
            baselineFaceCenterY = 45f
        )
        val standard = listOf(
            StandardStepReport(
                step = StandardInvestigationStep.NaturalPosition,
                completed = true,
                openLeftAvg = 0.9f,
                openRightAvg = 0.91f,
                sampleCount = 20
            )
        )
        val report = SignalInvestigationReport(
            sessionId = "s1",
            testStartedMs = 1,
            testCompletedMs = 2,
            reportGeneratedMs = 3,
            deviceManufacturer = "Samsung",
            deviceModel = "SM",
            androidVersion = "13",
            appVersionName = "1",
            versionCode = 1,
            isDebugBuild = true,
            glasses = GlassesCondition.YES,
            lighting = LightingCondition.Indoor,
            distanceLabel = "Natural",
            faceWidthPercent = 30f,
            cameraResolution = "1280x720",
            investigationMode = SignalInvestigationMode.Standard,
            baseline = null,
            positions = emptyList(),
            standardSteps = standard,
            environmentDiagnoses = listOf("Soft lighting preferred."),
            naturalBlinkCount = 3,
            l1r1Outcome = "Skipped",
            l2r2Outcome = "Skipped",
            sequencesAttempted = false,
            bestLeftSeparation = null,
            bestRightSeparation = null,
            lowestJitter = null,
            lowestRejected = null,
            bestOverall = null,
            engineeringFindings = listOf("Natural position measured."),
            recommendations = listOf("Keep the patient comfortably still."),
            ttsAvailable = true,
            voiceGuidanceEventsTotal = 4
        )
        val text = SignalInvestigationReportAuthority.formatFullText(report)
        assertTrue(text.contains("STANDARD INVESTIGATION"))
        assertTrue(text.contains("ADVANCED ENGINEERING TESTS"))
        assertTrue(text.contains("Not run"))
        assertTrue(text.contains("Natural Position"))
        assertTrue(text.contains("ENVIRONMENT DIAGNOSES"))
        assertTrue(text.contains("Investigation Mode"))

        val advanced = report.copy(
            investigationMode = SignalInvestigationMode.AdvancedEngineering,
            baseline = stats,
            positions = listOf(stats),
            standardSteps = emptyList()
        )
        val advText = SignalInvestigationReportAuthority.formatFullText(advanced)
        assertTrue(advText.contains("ADVANCED ENGINEERING INVESTIGATION"))
        assertTrue(advText.contains("Stable-in-target %"))
        assertTrue(advText.contains("94"))
    }

    @Test
    fun visualGuide_standardHasNoMovementArrows() {
        val live = SignalInvestigationController.LiveUi(
            recordingStatus = "Look Naturally",
            flowPhase = SignalInvestigationController.FlowPhase.StandardPrompt,
            investigationMode = SignalInvestigationMode.Standard,
            standardStep = StandardInvestigationStep.NaturalPosition,
            faceDetected = true
        )
        val guide = SignalInvestigationVisual.fromLive(live)
        assertEquals(SignalInvestigationVisual.Arrow.None, guide.arrow)
        assertEquals("Look Naturally", guide.status)
    }
}
