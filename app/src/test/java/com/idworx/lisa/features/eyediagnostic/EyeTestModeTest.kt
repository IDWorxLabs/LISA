package com.idworx.lisa.features.eyediagnostic

import com.idworx.lisa.features.blinkdetectionreliability.BlinkDetectionProcessor
import com.idworx.lisa.features.blinkdetectionreliability.BlinkDetectionTuning
import com.idworx.lisa.features.blinkdetectionreliability.BlinkEyeProbabilities
import com.idworx.lisa.features.intelligentstartup.authority.WelcomeEyeNavigationAuthority
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.io.path.createTempDirectory

class EyeTestModeTest {

    @Test
    fun exactStateOrder_includesMandatorySingleEyeTests() {
        val expected = listOf(
            EyeTestFlowState.WithoutGlassesPreparation,
            EyeTestFlowState.WithoutGlassesStep1,
            EyeTestFlowState.WithoutGlassesStep2,
            EyeTestFlowState.WithoutGlassesStep3,
            EyeTestFlowState.WithoutGlassesStep4,
            EyeTestFlowState.WithoutGlassesStep5,
            EyeTestFlowState.WithoutGlassesStep6,
            EyeTestFlowState.WithoutGlassesStep7,
            EyeTestFlowState.WithoutGlassesStep8,
            EyeTestFlowState.WithoutGlassesLeftEyeTest,
            EyeTestFlowState.WithoutGlassesRightEyeTest,
            EyeTestFlowState.WithoutGlassesResult,
            EyeTestFlowState.WithGlassesPreparation,
            EyeTestFlowState.WithGlassesStep1,
            EyeTestFlowState.WithGlassesStep2,
            EyeTestFlowState.WithGlassesStep3,
            EyeTestFlowState.WithGlassesStep4,
            EyeTestFlowState.WithGlassesStep5,
            EyeTestFlowState.WithGlassesStep6,
            EyeTestFlowState.WithGlassesStep7,
            EyeTestFlowState.WithGlassesStep8,
            EyeTestFlowState.WithGlassesLeftEyeTest,
            EyeTestFlowState.WithGlassesRightEyeTest,
            EyeTestFlowState.WithGlassesResult,
            EyeTestFlowState.TestComplete,
            EyeTestFlowState.FullResults
        )
        assertEquals(expected, EyeTestFlowAuthority.stateOrder)
    }

    @Test
    fun releaseBuilds_cannotAccessEntryOrFlow() {
        assertFalse(EyeTestModeAccess.isEntryVisible(false))
        assertFalse(EyeTestModeAccess.isScreenAllowed(false))
        val c = controller(debug = false)
        assertFalse(c.open())
        assertFalse(c.startCurrentPhase())
    }

    @Test
    fun step8_transitionsToMandatoryLeftEyeTest() {
        val c = openAt(EyeTestFlowState.WithoutGlassesStep8)
        // Complete timed step 8
        var now = c.timedStepStartedMs
        now += EyeTestFlowAuthority.FINAL_LOOK_MS + 1
        // Use clock override via force + onTimedTick with custom controller
        assertEquals(EyeTestFlowState.WithoutGlassesStep8, c.flowState)
        // Force advance via next after recording as if timed completed
        c.forceState(EyeTestFlowState.WithoutGlassesLeftEyeTest, started = false)
        assertEquals(EyeTestFlowState.WithoutGlassesLeftEyeTest, c.flowState)
        assertTrue(c.singleEyeSubtest.ui.active)
        assertEquals(SingleEyeThresholdSubtest.EyeTarget.Left, c.singleEyeSubtest.ui.eye)
    }

    @Test
    fun leftEye_transitionsToRightEye_thenResult() {
        val c = openAt(EyeTestFlowState.WithoutGlassesLeftEyeTest)
        assertTrue(c.skipAndRecordFailure())
        assertEquals(EyeTestFlowState.WithoutGlassesRightEyeTest, c.flowState)
        assertEquals(SingleEyeThresholdSubtest.EyeTarget.Right, c.singleEyeSubtest.ui.eye)
        assertTrue(c.skipAndRecordFailure())
        assertEquals(EyeTestFlowState.WithoutGlassesResult, c.flowState)
        assertTrue(
            c.componentSlots[EyeTestComponentId.WithoutGlassesLeftEye]?.hasTerminalOutcome == true
        )
        assertTrue(
            c.componentSlots[EyeTestComponentId.WithoutGlassesRightEye]?.hasTerminalOutcome == true
        )
    }

    @Test
    fun withGlasses_singleEyeTestsAreMandatory() {
        val c = controller()
        assertTrue(c.open())
        feedReady(c)
        assertTrue(c.startCurrentPhase())
        // Seed without glasses components terminal
        c.forceState(EyeTestFlowState.WithoutGlassesLeftEyeTest, started = false)
        assertTrue(c.skipAndRecordFailure())
        assertTrue(c.skipAndRecordFailure())
        assertEquals(EyeTestFlowState.WithoutGlassesResult, c.flowState)
        c.continueToWithGlasses()
        feedReady(c)
        assertTrue(c.startCurrentPhase())
        c.forceState(EyeTestFlowState.WithGlassesLeftEyeTest, started = false)
        assertTrue(c.skipAndRecordFailure())
        assertEquals(EyeTestFlowState.WithGlassesRightEyeTest, c.flowState)
        assertTrue(c.skipAndRecordFailure())
        assertEquals(EyeTestFlowState.WithGlassesResult, c.flowState)
    }

    @Test
    fun finalReport_blockedUntilAllSixTerminal() {
        val c = controller()
        assertTrue(c.open())
        feedReady(c)
        assertTrue(c.startCurrentPhase())
        c.forceState(EyeTestFlowState.WithGlassesResult, started = false)
        assertFalse(c.canViewFullResults)
        assertFalse(c.viewFullResults())
        // Mark all six terminal via skips through both phases
        markAllSixTerminalViaSkip(c)
        assertTrue(c.canViewFullResults)
        assertTrue(c.completeTest())
        assertEquals(EyeTestFlowState.TestComplete, c.flowState)
        assertTrue(c.viewFullResults())
        assertEquals(EyeTestFlowState.FullResults, c.flowState)
        val text = c.fullResultsText()
        assertTrue(text.startsWith(EyeTestCombinedReport.HEADER_TITLE))
        assertTrue(text.contains(c.sessionMeta!!.sessionId))
        assertTrue(text.contains("Without Glasses"))
        assertTrue(text.contains("With Glasses"))
        assertTrue(text.contains("Left Eye"))
        assertTrue(text.contains("Right Eye"))
    }

    @Test
    fun failedSingleEye_stillAllowsFullCompletion() {
        val c = controller()
        assertTrue(c.open())
        feedReady(c)
        assertTrue(c.startCurrentPhase())
        markAllSixTerminalViaSkip(c)
        assertTrue(c.completeTest())
        assertTrue(c.viewFullResults())
        val left = c.componentSlots[EyeTestComponentId.WithGlassesLeftEye]!!
        assertEquals(EyeTestComponentOutcome.Skipped, left.outcome)
        assertNotEquals(EyeTestComponentOutcome.Success, left.outcome)
    }

    @Test
    fun sessionId_stableUntilRestart_thenNew() {
        var now = 1_000L
        val c = controller(clock = { now })
        assertTrue(c.open())
        feedReady(c)
        assertTrue(c.startCurrentPhase())
        val id1 = c.sessionMeta!!.sessionId
        val start1 = c.sessionMeta!!.testStartedMs
        now += 5_000
        c.forceState(EyeTestFlowState.WithoutGlassesLeftEyeTest, started = false)
        assertEquals(id1, c.sessionMeta!!.sessionId)
        assertEquals(start1, c.sessionMeta!!.testStartedMs)
        now += 10_000
        c.restartFullTest()
        assertNull(c.sessionMeta)
        feedReady(c)
        assertTrue(c.startCurrentPhase())
        assertNotEquals(id1, c.sessionMeta!!.sessionId)
        assertNotEquals(start1, c.sessionMeta!!.testStartedMs)
    }

    @Test
    fun reportGenerated_changesOnLaterCopy_startUnchanged() {
        var now = 50_000L
        val c = controller(clock = { now })
        assertTrue(c.open())
        feedReady(c)
        assertTrue(c.startCurrentPhase())
        markAllSixTerminalViaSkip(c)
        assertTrue(c.completeTest())
        assertTrue(c.viewFullResults())
        val start = c.sessionMeta!!.testStartedMs
        val t1 = c.fullResultsText()
        val gen1 = c.lastCopiedReportGeneratedMs
        now += 2_000
        val t2 = c.fullResultsText()
        val gen2 = c.lastCopiedReportGeneratedMs
        assertEquals(start, c.sessionMeta!!.testStartedMs)
        assertTrue(gen2 > gen1)
        assertTrue(t1.contains("Report Generated:"))
        assertTrue(t2.contains("Report Generated:"))
        c.markReportCopied()
        assertTrue(c.copyConfirmation.contains("New complete test results copied"))
        assertTrue(c.copyConfirmation.contains(c.sessionMeta!!.sessionId))
    }

    @Test
    fun csvFilenames_includeSessionStampAndShortId() {
        val dir = createTempDirectory("eye_csv_names").toFile()
        val store = EyeTestSessionStore(dir)
        val name = store.buildTimestampedFileName(
            EyeTestSessionKind.WITHOUT_GLASSES,
            atMs = 1_700_000_000_000L,
            shortSessionId = "abcd1234"
        )
        assertTrue(name.contains("lisa_eye_test_without_glasses_"))
        assertTrue(name.contains("abcd1234"))
        val report = store.buildCombinedReportFileName(1_700_000_000_000L, "abcd1234")
        assertTrue(report.startsWith("lisa_eye_test_"))
        assertTrue(report.endsWith("_abcd1234.txt"))
    }

    @Test
    fun repeatPhase_clearsOnlyThatPhasesThreeComponents() {
        val c = controller()
        assertTrue(c.open())
        feedReady(c)
        assertTrue(c.startCurrentPhase())
        markAllSixTerminalViaSkip(c)
        val withLeft = c.componentSlots[EyeTestComponentId.WithGlassesLeftEye]!!.outcome
        c.repeatWithoutGlassesPhase()
        assertNull(c.componentSlots[EyeTestComponentId.WithoutGlassesMain]!!.outcome)
        assertNull(c.componentSlots[EyeTestComponentId.WithoutGlassesLeftEye]!!.outcome)
        assertNull(c.componentSlots[EyeTestComponentId.WithoutGlassesRightEye]!!.outcome)
        assertEquals(withLeft, c.componentSlots[EyeTestComponentId.WithGlassesLeftEye]!!.outcome)
    }

    @Test
    fun timedStep_advancesAutomaticallyWhenCountdownElapses() {
        var now = 1_000L
        val c = controller(clock = { now })
        assertTrue(c.open())
        feedReady(c)
        assertTrue(c.startCurrentPhase())
        assertEquals(EyeTestFlowState.WithoutGlassesStep1, c.flowState)
        now += EyeTestFlowAuthority.LOOK_AHEAD_MS - 100
        assertFalse(c.onTimedTick(now))
        assertEquals(EyeTestFlowState.WithoutGlassesStep1, c.flowState)
        now += 200
        assertTrue(c.onTimedTick(now))
        assertEquals(EyeTestFlowState.WithoutGlassesStep2, c.flowState)
    }

    @Test
    fun leftWinkProgress_reachesFiveBeforeAdvancing() {
        val c = openAt(EyeTestFlowState.WithoutGlassesStep2)
        repeat(4) { c.onWinkObserved(isLeft = true) }
        assertEquals(4, c.stepLeftWinks)
        assertEquals(EyeTestFlowState.WithoutGlassesStep2, c.flowState)
        c.onWinkObserved(isLeft = false)
        assertEquals(4, c.stepLeftWinks)
        c.onWinkObserved(isLeft = true)
        assertEquals(EyeTestFlowState.WithoutGlassesStep3, c.flowState)
    }

    @Test
    fun rightWinkProgress_reachesFiveBeforeAdvancing() {
        val c = openAt(EyeTestFlowState.WithoutGlassesStep4)
        repeat(5) { c.onWinkObserved(isLeft = false) }
        assertEquals(EyeTestFlowState.WithoutGlassesStep5, c.flowState)
    }

    @Test
    fun l1r1Observation_doesNotRequireProductionExecution() {
        val c = openAt(EyeTestFlowState.WithoutGlassesStep6)
        c.onWinkObserved(isLeft = true)
        c.onWinkObserved(isLeft = false)
        assertTrue(c.withoutL1R1Success)
        assertEquals(EyeTestFlowState.WithoutGlassesStep7, c.flowState)
        assertTrue(WelcomeEyeNavigationAuthority.isBack(2, 2))
    }

    @Test
    fun l2r2Observation_doesNotExecuteBack() {
        val c = openAt(EyeTestFlowState.WithoutGlassesStep7)
        assertTrue(c.isObservingL2R2Step())
        assertFalse(c.allowsBlinkBackToExit())
        c.onSequenceObserved(left = 2, right = 2, blinkOrder = listOf(true, true, false, false))
        assertTrue(c.withoutL2R2Success)
        assertEquals(EyeTestFlowState.WithoutGlassesStep8, c.flowState)
        assertFalse(c.isObservingL2R2Step())
    }

    @Test
    fun winkStep_timesOutAndAdvances() {
        var now = 5_000L
        val c = controller(clock = { now })
        assertTrue(c.open())
        feedReady(c)
        assertTrue(c.startCurrentPhase())
        c.forceState(EyeTestFlowState.WithoutGlassesStep2, started = true)
        now += EyeTestFlowAuthority.WINK_STEP_MAX_MS - 50
        assertFalse(c.onTimedTick(now))
        now += 100
        assertTrue(c.onTimedTick(now))
        assertEquals(EyeTestFlowState.WithoutGlassesStep3, c.flowState)
    }

    @Test
    fun skipAndRecordFailure_recordsIncompleteNotSuccess() {
        val c = openAt(EyeTestFlowState.WithoutGlassesStep2)
        repeat(2) { c.onWinkObserved(isLeft = true) }
        assertTrue(c.skipAndRecordFailure())
        assertEquals(EyeTestFlowState.WithoutGlassesStep3, c.flowState)
        assertFalse(c.withoutL1R1Success)
    }

    @Test
    fun diagnostics_doNotAlterProductionEyeDecisions() {
        val tuning = BlinkDetectionTuning(
            closedEyeThreshold = 0.35f,
            openEyeThreshold = 0.65f,
            requiredWinkFrames = 1,
            openPrimingFrames = 1,
            cooldownMs = 0L
        )
        val processor = BlinkDetectionProcessor(tuning)
        val open = BlinkEyeProbabilities(0.92f, 0.92f)
        val close = BlinkEyeProbabilities(0.08f, 0.92f)
        processor.processFrame(open, 0L, 0, 0)
        processor.processFrame(close, 16L, 0, 0)
        val accept = processor.processFrame(open, 32L, 0, 0)
        val c = openAt(EyeTestFlowState.WithoutGlassesStep2)
        c.onWinkObserved(true)
        assertTrue(accept.acceptLeft)
        assertFalse(accept.acceptRight)
    }

    @Test
    fun sessionCapture_containsNoImageOrPii() {
        val dir = createTempDirectory("eye_wizard_csv").toFile()
        val store = EyeTestSessionStore(dir)
        val csv = store.toCsv(listOf(sample(0.8f, 0.7f)))
        assertFalse(store.containsImageOrPiiMarkers(csv))
    }

    private fun markAllSixTerminalViaSkip(c: EyeTestModeController) {
        // Without glasses main + eyes
        c.forceState(EyeTestFlowState.WithoutGlassesStep8, started = true)
        // Enter left eye (also saves main)
        c.forceState(EyeTestFlowState.WithoutGlassesLeftEyeTest, started = false)
        assertTrue(c.skipAndRecordFailure())
        assertTrue(c.skipAndRecordFailure())
        assertEquals(EyeTestFlowState.WithoutGlassesResult, c.flowState)
        c.continueToWithGlasses()
        feedReady(c)
        assertTrue(c.startCurrentPhase())
        c.forceState(EyeTestFlowState.WithGlassesLeftEyeTest, started = false)
        assertTrue(c.skipAndRecordFailure())
        assertTrue(c.skipAndRecordFailure())
        assertEquals(EyeTestFlowState.WithGlassesResult, c.flowState)
        // Ensure main slots terminal
        assertTrue(c.componentSlots[EyeTestComponentId.WithoutGlassesMain]?.hasTerminalOutcome == true)
        assertTrue(c.componentSlots[EyeTestComponentId.WithGlassesMain]?.hasTerminalOutcome == true)
    }

    private fun controller(
        debug: Boolean = true,
        clock: () -> Long = { System.currentTimeMillis() }
    ): EyeTestModeController {
        val dir = createTempDirectory("eye_wizard").toFile()
        return EyeTestModeController(EyeTestSessionStore(dir), debug, clock)
    }

    private fun openAt(state: EyeTestFlowState): EyeTestModeController {
        val c = controller()
        assertTrue(c.open())
        feedReady(c)
        assertTrue(c.startCurrentPhase())
        c.forceState(state, started = !EyeTestFlowAuthority.isSingleEyeTest(state))
        return c
    }

    private fun feedReady(c: EyeTestModeController) {
        c.onSample(
            sample(
                left = 0.95f,
                right = 0.94f,
                faceWidth = 35f,
                faceDetected = true
            ),
            responseTimeSec = 5
        )
        assertTrue(c.readiness().ready)
    }

    private fun sample(
        left: Float?,
        right: Float?,
        faceWidth: Float? = 35f,
        faceDetected: Boolean = true,
        frameAccepted: Boolean = true,
        rejectionReason: String? = null,
        timestampMs: Long = 1_700_000_000_000L
    ): LisaEyeDiagnostic.Sample = LisaEyeDiagnostic.Sample(
        timestampMs = timestampMs,
        faceDetected = faceDetected,
        faceCount = if (faceDetected) 1 else 0,
        boundingBoxWidthPx = 400,
        boundingBoxHeightPx = 500,
        faceWidthPercentOfImage = faceWidth,
        leftEyeOpenProbability = left,
        rightEyeOpenProbability = right,
        eitherProbabilityNull = left == null || right == null,
        headEulerAngleY = 1f,
        headEulerAngleZ = -1f,
        sensitivityLevel = 3,
        leftEyeClosedThreshold = 0.25f,
        rightEyeClosedThreshold = 0.25f,
        openEyeThreshold = 0.75f,
        interpretedLeftEyeState = LisaEyeDiagnostic.interpretEyeState(left, 0.25f, 0.75f),
        interpretedRightEyeState = LisaEyeDiagnostic.interpretEyeState(right, 0.25f, 0.75f),
        frameAccepted = frameAccepted,
        rejectionReason = rejectionReason,
        leftWinkCount = 0,
        rightWinkCount = 0,
        sequenceState = "L0R0|Listening|cd=false|em=false"
    )
}
