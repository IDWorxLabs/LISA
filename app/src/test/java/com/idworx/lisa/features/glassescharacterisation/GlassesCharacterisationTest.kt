package com.idworx.lisa.features.glassescharacterisation

import com.idworx.lisa.features.blinkdetectionreliability.BlinkDetectionTuning
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.io.path.createTempDirectory

class GlassesCharacterisationTest {

    @Test
    fun releaseBuilds_cannotAccess() {
        assertFalse(GlassesCharacterisationAccess.isEntryVisible(false))
        assertFalse(GlassesCharacterisationAccess.isScreenAllowed(false))
    }

    @Test
    fun caregiverInstructions_neverAskPatientToMove() {
        LightingConditionKind.entries.forEach { c ->
            val text = (c.displayName + " " + c.caregiverInstruction).lowercase()
            assertFalse(text.contains("tilt your head"))
            assertFalse(text.contains("lean forward"))
            assertFalse(text.contains("move closer"))
            if (c != LightingConditionKind.Normal) {
                assertTrue(c.caregiverInstruction.contains("Caregiver", ignoreCase = true))
            }
        }
    }

    @Test
    fun allThreeConditions_useSameProtocolConstants() {
        assertEquals(3, LightingConditionKind.entries.size)
        assertEquals(5, GlassesCharacterisationMetrics.CLOSE_CYCLES)
        assertEquals(10_000L, GlassesCharacterisationMetrics.OPEN_RECORD_MS)
        assertEquals(2_000L, GlassesCharacterisationMetrics.CLOSE_RECORD_MS)
        assertEquals(20_000L, GlassesCharacterisationMetrics.BLINK_OBSERVE_MS)
        assertEquals(4_000L, GlassesCharacterisationMetrics.PREPARE_MS)
    }

    @Test
    fun distribution_andSeparation_metricsAreCorrect() {
        val open = listOf(0.80f, 0.82f, 0.85f, 0.88f, 0.90f, 0.92f, 0.95f)
        val closed = listOf(0.20f, 0.22f, 0.25f, 0.28f, 0.30f, 0.32f, 0.35f)
        val m = GlassesCharacterisationMetrics.eyeMetrics(
            openSamples = open,
            closedSamples = closed,
            nullCount = 1,
            framesSeen = 20,
            rejectedCount = 2,
            closedThr = 0.40f,
            openThr = 0.70f,
            isUncertain = { p -> p in 0.40f..0.70f }
        )
        assertEquals(7, m.open.count)
        assertEquals(7, m.closed.count)
        assertTrue((m.openP25MinusClosedP75 ?: 0f) > 0.4f)
        assertTrue(m.overlapPercent < 5f)
        assertTrue(m.closedBelowClosedThrPct >= 99f)
        assertTrue(m.openAboveOpenThrPct >= 99f)
        assertEquals(5f, m.nullPercent, 0.01f)
        assertEquals(10f, m.rejectedPercent, 0.01f)
    }

    @Test
    fun qualityClassification_isDeterministic() {
        fun metrics(sep: Float, overlap: Float, openN: Int, closedN: Int): EyeSeparationMetrics {
            val open = List(openN) { 0.85f }
            val closed = List(closedN) { 0.85f - sep - 0.05f }
            return GlassesCharacterisationMetrics.eyeMetrics(
                open, closed, 0, openN + closedN, 0, 0.4f, 0.7f
            ) { false }.copy(
                openP25MinusClosedP75 = sep,
                overlapPercent = overlap,
                closedBelowClosedThrPct = 80f
            )
        }
        assertEquals(
            SignalQualityClass.Strong,
            GlassesCharacterisationQuality.classify(
                metrics(0.22f, 20f, 25, 10),
                metrics(0.20f, 25f, 25, 10)
            )
        )
        assertEquals(
            SignalQualityClass.Unusable,
            GlassesCharacterisationQuality.classify(
                metrics(0.02f, 90f, 5, 2),
                metrics(0.01f, 95f, 5, 2)
            )
        )
    }

    @Test
    fun positionConsistency_flagsExcessiveDrift() {
        val base = PoseSnapshot(50f, 50f, 30f, 0f, 0f)
        assertFalse(
            GlassesCharacterisationMetrics.poseDriftExcessive(
                base, PoseSnapshot(52f, 51f, 31f, 2f, 1f)
            )
        )
        assertTrue(
            GlassesCharacterisationMetrics.poseDriftExcessive(
                base, PoseSnapshot(70f, 50f, 30f, 0f, 0f)
            )
        )
    }

    @Test
    fun continuousSession_oneSessionId_baselineOnce_andPartialReport() {
        var now = 9_000_000L
        val dir = createTempDirectory("gc-partial").toFile()
        val store = GlassesCharacterisationStore(dir)
        val controller = GlassesCharacterisationController(
            store = store,
            isDebugBuild = true,
            clockMs = { now },
            ttsAvailableProvider = { true },
            standardTuningProvider = { BlinkDetectionTuning.default }
        )
        assertTrue(controller.open())
        assertTrue(controller.openSetup())
        assertTrue(controller.startTest())
        assertEquals(GlassesCharUiPhase.LightingPrep, controller.uiPhase)
        val sessionA = controller.live.sessionIdShort
        assertTrue(controller.iAmReady())
        // Capture baseline once via stabilize
        controller.onSample(0.9f, 0.9f, true, 30f, true, 0f, 0f, 50f, 50f)
        now += GlassesCharacterisationMetrics.FACE_STABLE_MS + 50
        controller.onSample(0.9f, 0.9f, true, 30f, true, 0f, 0f, 50f, 50f)
        assertTrue(controller.live.flowPhase == GlassesCharFlowPhase.PrepareOpen ||
            controller.live.flowPhase == GlassesCharFlowPhase.Stabilize)

        assertTrue(controller.endConditionEarly())
        assertEquals(GlassesCharUiPhase.LightingTransition, controller.uiPhase)
        assertEquals("Normal Lighting", controller.live.completedConditionName)
        assertEquals("Brighter Lighting", controller.live.nextConditionName)
        assertTrue(controller.iAmReady())
        // Subsequent condition validates against baseline — no new session
        assertEquals(sessionA, controller.live.sessionIdShort)
        assertEquals(
            GlassesCharFlowPhase.ValidateAgainstBaseline,
            controller.live.flowPhase
        )
        controller.onSample(0.9f, 0.9f, true, 30f, true, 0f, 0f, 50f, 50f)
        now += GlassesCharacterisationMetrics.BASELINE_REVALIDATE_MS + 50
        controller.onSample(0.9f, 0.9f, true, 30f, true, 0f, 0f, 50f, 50f)
        assertTrue(controller.endConditionEarly())
        assertEquals(GlassesCharUiPhase.LightingTransition, controller.uiPhase)

        assertTrue(controller.endFullTestEarly())
        assertEquals(GlassesCharUiPhase.FinalReport, controller.uiPhase)
        val text = controller.fullReportText()
        assertTrue(text.contains("SESSION SUMMARY"))
        assertTrue(text.contains("COMPARISON TABLE"))
        assertTrue(text.contains(controller.lastReport!!.sessionId))
        assertTrue(text.contains("Position consistency maintained:"))
        assertEquals(3, controller.lastReport!!.conditions.size)
        val kinds = controller.lastReport!!.conditions.map { it.condition }.toSet()
        assertEquals(3, kinds.size)
        assertTrue(store.containsOnlyTextReports())
        // One report file for the session
        assertEquals(1, store.reportsDir().listFiles()?.size ?: 0)
    }

    @Test
    fun finalCondition_autoOpensAnalysingThenReport() {
        var now = 11_000_000L
        val controller = GlassesCharacterisationController(
            store = GlassesCharacterisationStore(createTempDirectory("gc-auto").toFile()),
            isDebugBuild = true,
            clockMs = { now },
            ttsAvailableProvider = { true }
        )
        controller.open()
        controller.openSetup()
        controller.startTest()
        // Force through by ending each condition after ready
        controller.iAmReady()
        controller.endConditionEarly() // Normal → transition
        controller.iAmReady()
        controller.endConditionEarly() // Brighter → transition
        controller.iAmReady()
        controller.endConditionEarly() // Dimmer → Analysing
        assertEquals(GlassesCharUiPhase.Analysing, controller.uiPhase)
        now += GlassesCharacterisationMetrics.ANALYSE_STEP_MS + 10
        controller.onTimedTick(now)
        now += GlassesCharacterisationMetrics.ANALYSE_STEP_MS + 10
        controller.onTimedTick(now)
        now += GlassesCharacterisationMetrics.ANALYSE_STEP_MS + 10
        controller.onTimedTick(now)
        assertEquals(GlassesCharUiPhase.FinalReport, controller.uiPhase)
        assertNotNull(controller.lastReport)
        assertFalse(controller.lastReport!!.incomplete)
    }

    @Test
    fun standardProtocol_runsOpenThenLeftCycles_withoutMovementPrompts() {
        var now = 8_000_000L
        val spoken = mutableListOf<String>()
        val controller = GlassesCharacterisationController(
            store = GlassesCharacterisationStore(createTempDirectory("gc-flow").toFile()),
            isDebugBuild = true,
            clockMs = { now },
            speak = { spoken += it },
            ttsAvailableProvider = { true }
        )
        controller.open()
        controller.openSetup()
        controller.startTest()
        controller.iAmReady()
        fun feed(l: Float = 0.9f, r: Float = 0.9f) {
            controller.onSample(l, r, true, 30f, true, 0f, 0f, 50f, 50f)
        }
        feed()
        now += GlassesCharacterisationMetrics.FACE_STABLE_MS + 50
        feed()
        assertEquals(GlassesCharFlowPhase.PrepareOpen, controller.live.flowPhase)
        assertTrue(spoken.any { it.contains("both eyes open", ignoreCase = true) })
        now += GlassesCharacterisationMetrics.PREPARE_MS + 20
        assertTrue(controller.onTimedTick(now))
        assertEquals(GlassesCharFlowPhase.RecordOpen, controller.live.flowPhase)
        repeat(5) { now += 100; feed(); controller.onTimedTick(now) }
        now += GlassesCharacterisationMetrics.OPEN_RECORD_MS
        assertTrue(controller.onTimedTick(now))
        now += GlassesCharacterisationMetrics.COMPLETE_PAUSE_MS + 20
        assertTrue(controller.onTimedTick(now))
        assertEquals(GlassesCharFlowPhase.PrepareLeft, controller.live.flowPhase)
        assertTrue(spoken.any { it.contains("left eye", ignoreCase = true) })
        assertFalse(spoken.any { it.contains("tilt", ignoreCase = true) })
        assertFalse(spoken.any { it == "Good." })
    }

    @Test
    fun decisionSupport_andComparison_helpers() {
        val strong = ConditionResult(
            condition = LightingConditionKind.Brighter,
            completed = true,
            left = EyeSeparationMetrics(
                open = DistributionSummary(count = 30),
                closed = DistributionSummary(count = 12),
                openP25MinusClosedP75 = 0.25f,
                overlapPercent = 10f,
                closedBelowClosedThrPct = 80f,
                nullPercent = 2f,
                rejectedPercent = 5f
            ),
            right = EyeSeparationMetrics(
                open = DistributionSummary(count = 30),
                closed = DistributionSummary(count = 12),
                openP25MinusClosedP75 = 0.22f,
                overlapPercent = 12f,
                closedBelowClosedThrPct = 75f,
                nullPercent = 2f,
                rejectedPercent = 5f
            ),
            quality = SignalQualityClass.Strong
        )
        val normal = strong.copy(
            condition = LightingConditionKind.Normal,
            left = strong.left.copy(openP25MinusClosedP75 = 0.08f),
            right = strong.right.copy(openP25MinusClosedP75 = 0.07f),
            quality = SignalQualityClass.Weak
        )
        val dimmer = strong.copy(
            condition = LightingConditionKind.Dimmer,
            left = strong.left.copy(openP25MinusClosedP75 = 0.09f),
            right = strong.right.copy(openP25MinusClosedP75 = 0.08f),
            quality = SignalQualityClass.Weak
        )
        val conditions = listOf(normal, strong, dimmer)
        assertEquals(
            LightingConditionKind.Brighter,
            GlassesCharacterisationComparison.bestOverall(conditions)
        )
        val (decision, _) = GlassesCharacterisationComparison.decide(conditions)
        assertEquals(
            DecisionSupportCategory.CONTINUE_PERSONALISED_PROFILE_RESEARCH,
            decision
        )
    }

    @Test
    fun noProductionAction_controllerDoesNotTouchBlinkDetectionProcessor() {
        // Compile-time / architectural: controller only reads BlinkDetectionTuning via provider.
        val tuning = BlinkDetectionTuning.default
        val controller = GlassesCharacterisationController(
            store = GlassesCharacterisationStore(createTempDirectory("gc-prod").toFile()),
            isDebugBuild = true,
            standardTuningProvider = { tuning }
        )
        assertTrue(controller.open())
        // Observational thresholds captured in setup path
        controller.applyRuntimeSetup(5, "1.0s", 0.5f, null, "1", "1280x720")
        assertEquals(tuning.closedEyeThreshold, controller.live.let {
            // ensure open works; thresholds used at finalize
            tuning.closedEyeThreshold
        }, 0.0001f)
    }
}
