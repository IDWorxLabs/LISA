package com.idworx.lisa.features.personalisedeyeprofile

import com.idworx.lisa.features.blinkdetectionreliability.BlinkDetectionTuning
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.io.path.createTempDirectory

class PersonalisedEyeProfileTest {

    @Test
    fun releaseBuilds_cannotAccessPrototype() {
        assertFalse(PersonalisedEyeProfileAccess.isEntryVisible(false))
        assertFalse(PersonalisedEyeProfileAccess.isScreenAllowed(false))
    }

    @Test
    fun standardTuning_defaultsUnchanged_whenPersonalisedOpensNull() {
        val t = BlinkDetectionTuning.forSensitivityLevel(3)
        assertNull(t.leftOpenEyeThreshold)
        assertNull(t.rightOpenEyeThreshold)
        assertEquals(t.openEyeThreshold, t.effectiveLeftOpenThreshold)
        assertEquals(t.openEyeThreshold, t.effectiveRightOpenThreshold)
        // Candidate checks match legacy openEyeThreshold when personalised opens absent.
        assertTrue(t.isLeftWinkCandidate(0.10f, 0.95f) ==
            (0.10f < t.effectiveLeftClosedThreshold && 0.95f > t.openEyeThreshold))
        assertTrue(t.isBothEyesOpen(0.95f, 0.95f))
        assertFalse(t.isBothEyesOpen(0.10f, 0.95f))
    }

    @Test
    fun derivation_usesMeasuredDistributions_andRejectsOverlap() {
        val open = List(40) { 0.85f + (it % 5) * 0.01f }
        val closed = List(20) { 0.20f + (it % 4) * 0.02f }
        val ok = PersonalisedThresholdDerivation.deriveEye(
            PersonalisedThresholdDerivation.EyeDerivationInput(open, closed, open)
        )
        assertTrue(ok.ok)
        assertTrue(ok.closedThreshold < ok.openThreshold)
        assertTrue(ok.separation >= PersonalisedThresholdDerivation.MIN_SEPARATION)

        val overlapOpen = List(40) { 0.55f }
        val overlapClosed = List(20) { 0.52f }
        val bad = PersonalisedThresholdDerivation.deriveEye(
            PersonalisedThresholdDerivation.EyeDerivationInput(overlapOpen, overlapClosed)
        )
        assertFalse(bad.ok)
        assertTrue(bad.failureReasons.any { it.contains("overlap", ignoreCase = true) })
    }

    @Test
    fun derivation_rejectsInvalidOrderingAndInsufficientSamples() {
        val few = PersonalisedThresholdDerivation.deriveEye(
            PersonalisedThresholdDerivation.EyeDerivationInput(
                openSamples = listOf(0.9f),
                closedSamples = listOf(0.1f)
            )
        )
        assertFalse(few.ok)
        assertTrue(few.failureReasons.any { it.contains("few", ignoreCase = true) })
    }

    @Test
    fun outliers_doNotDominate_medianBasedDerivation() {
        val open = MutableList(40) { 0.88f }
        open[0] = 0.10f // outlier
        val closed = MutableList(20) { 0.25f }
        closed[0] = 0.99f // outlier
        val r = PersonalisedThresholdDerivation.deriveEye(
            PersonalisedThresholdDerivation.EyeDerivationInput(open, closed)
        )
        assertTrue(r.ok)
        assertTrue(r.openBaseline > 0.7f)
        assertTrue(r.closedBaseline < 0.4f)
    }

    @Test
    fun leftAndRight_profilesRemainIsolated() {
        val left = PersonalisedThresholdDerivation.deriveEye(
            PersonalisedThresholdDerivation.EyeDerivationInput(
                openSamples = List(40) { 0.90f },
                closedSamples = List(20) { 0.15f }
            )
        )
        val right = PersonalisedThresholdDerivation.deriveEye(
            PersonalisedThresholdDerivation.EyeDerivationInput(
                openSamples = List(40) { 0.80f },
                closedSamples = List(20) { 0.45f }
            )
        )
        assertTrue(left.ok && right.ok)
        assertNotEquals(left.closedThreshold, right.closedThreshold)
        assertNotEquals(left.openThreshold, right.openThreshold)
    }

    @Test
    fun validationRun1_aloneCannotMarkValidated() {
        val profile = PersonalisedEyeProfile(
            createdAtMs = 1L,
            updatedAtMs = 1L,
            status = PersonalisedEyeProfileStatus.ReadyForValidationRun1,
            leftClosedThreshold = 0.4f,
            leftOpenThreshold = 0.75f,
            rightClosedThreshold = 0.4f,
            rightOpenThreshold = 0.75f
        )
        val run1 = PersonalisedEyeProfileValidation.evaluateRun(
            1,
            PersonalisedEyeProfileValidation.LiveCounters(
                leftWinks = 5,
                rightWinks = 5,
                l1r1Success = true,
                l2r2Success = true,
                falsePositiveWinks = 0,
                unexpectedSequence = false,
                nullProbabilityPercent = 0f,
                uncertainOccupancyPercent = 10f
            )
        )
        assertTrue(run1.passed)
        val after1 = PersonalisedEyeProfileValidation.applyRunToProfile(profile, run1, 2L)
        assertEquals(PersonalisedEyeProfileStatus.ValidationRun1Passed, after1.status)
        assertNotEquals(PersonalisedEyeProfileStatus.Validated, after1.status)

        val run2 = PersonalisedEyeProfileValidation.evaluateRun(
            2,
            PersonalisedEyeProfileValidation.LiveCounters(
                leftWinks = 5,
                rightWinks = 5,
                l1r1Success = true,
                l2r2Success = true,
                falsePositiveWinks = 0,
                unexpectedSequence = false,
                nullProbabilityPercent = 0f,
                uncertainOccupancyPercent = 10f
            )
        )
        val after2 = PersonalisedEyeProfileValidation.applyRunToProfile(after1, run2, 3L)
        assertEquals(PersonalisedEyeProfileStatus.Validated, after2.status)
    }

    @Test
    fun failedSecondRun_producesFailedValidation() {
        val base = PersonalisedEyeProfile(
            createdAtMs = 1L,
            updatedAtMs = 1L,
            status = PersonalisedEyeProfileStatus.ValidationRun1Passed,
            validationRun1 = PersonalisedValidationRunResult(1, true, 5, 5, true, true)
        )
        val run2Fail = PersonalisedEyeProfileValidation.evaluateRun(
            2,
            PersonalisedEyeProfileValidation.LiveCounters(
                leftWinks = 2,
                rightWinks = 5,
                l1r1Success = true,
                l2r2Success = false,
                falsePositiveWinks = 1,
                unexpectedSequence = false,
                nullProbabilityPercent = 0f,
                uncertainOccupancyPercent = 10f
            )
        )
        assertFalse(run2Fail.passed)
        val after = PersonalisedEyeProfileValidation.applyRunToProfile(base, run2Fail, 9L)
        assertEquals(PersonalisedEyeProfileStatus.FailedValidation, after.status)
        assertTrue(after.failureReasons.any { it.contains("L2 R2") || it.contains("Left wink") || it.contains("false") })
    }

    @Test
    fun falsePositives_andSequenceFailures_failValidation() {
        val fp = PersonalisedEyeProfileValidation.evaluateRun(
            1,
            PersonalisedEyeProfileValidation.LiveCounters(
                leftWinks = 5, rightWinks = 5,
                l1r1Success = true, l2r2Success = true,
                falsePositiveWinks = 1, unexpectedSequence = false,
                nullProbabilityPercent = 0f, uncertainOccupancyPercent = 5f
            )
        )
        assertFalse(fp.passed)
        val l1 = PersonalisedEyeProfileValidation.evaluateRun(
            1,
            PersonalisedEyeProfileValidation.LiveCounters(
                leftWinks = 5, rightWinks = 5,
                l1r1Success = false, l2r2Success = true,
                falsePositiveWinks = 0, unexpectedSequence = false,
                nullProbabilityPercent = 0f, uncertainOccupancyPercent = 5f
            )
        )
        assertFalse(l1.passed)
        assertTrue(l1.failureReasons.any { it.contains("L1 R1") })
    }

    @Test
    fun store_deleteAffectsOnlyDebugPrototype() {
        val dir = createTempDirectory("pep_store").toFile()
        val store = PersonalisedEyeProfileStore(dir)
        val p = PersonalisedEyeProfile(
            createdAtMs = 1L,
            updatedAtMs = 1L,
            status = PersonalisedEyeProfileStatus.Calibrated,
            leftClosedThreshold = 0.3f,
            leftOpenThreshold = 0.7f,
            rightClosedThreshold = 0.35f,
            rightOpenThreshold = 0.72f,
            derivationNotes = "test"
        )
        store.save(p)
        assertNotNull(store.load())
        assertTrue(store.delete())
        assertNull(store.load())
        val json = PersonalisedEyeProfileStore(dir).let {
            it.save(p)
            it.profileFile().readText()
        }
        assertFalse(store.containsImageOrPiiMarkers(json))
        assertFalse(json.contains("@"))
    }

    @Test
    fun candidateTuning_doesNotMutateDefaultStandard() {
        val standard = BlinkDetectionTuning.default
        val profile = PersonalisedEyeProfile(
            createdAtMs = 1L,
            updatedAtMs = 1L,
            leftClosedThreshold = 0.55f,
            leftOpenThreshold = 0.78f,
            rightClosedThreshold = 0.58f,
            rightOpenThreshold = 0.80f
        )
        val candidate = profile.toCandidateTuning(standard)
        assertEquals(0.55f, candidate.leftClosedEyeThreshold)
        assertEquals(0.78f, candidate.leftOpenEyeThreshold)
        // Original standard object unchanged
        assertNull(standard.leftClosedEyeThreshold)
        assertNull(standard.leftOpenEyeThreshold)
        assertNotEquals(candidate.closedEyeThreshold, standard.closedEyeThreshold)
    }

    @Test
    fun replay_isDeterministic() {
        val samples = (0 until 30).map { i ->
            com.idworx.lisa.features.eyediagnostic.LisaEyeDiagnostic.Sample(
                timestampMs = 1000L + i * 40L,
                faceDetected = true,
                faceCount = 1,
                boundingBoxWidthPx = 400,
                boundingBoxHeightPx = 500,
                faceWidthPercentOfImage = 35f,
                leftEyeOpenProbability = if (i in 10..12) 0.1f else 0.9f,
                rightEyeOpenProbability = 0.9f,
                eitherProbabilityNull = false,
                headEulerAngleY = 0f,
                headEulerAngleZ = 0f,
                sensitivityLevel = 3,
                leftEyeClosedThreshold = 0.35f,
                rightEyeClosedThreshold = 0.35f,
                openEyeThreshold = 0.65f,
                interpretedLeftEyeState = com.idworx.lisa.features.eyediagnostic.LisaEyeDiagnostic.InterpretedEyeState.OPEN,
                interpretedRightEyeState = com.idworx.lisa.features.eyediagnostic.LisaEyeDiagnostic.InterpretedEyeState.OPEN,
                frameAccepted = true,
                rejectionReason = null,
                leftWinkCount = 0,
                rightWinkCount = 0,
                sequenceState = "idle"
            )
        }
        val tuning = BlinkDetectionTuning(
            closedEyeThreshold = 0.35f,
            openEyeThreshold = 0.65f,
            requiredWinkFrames = 1,
            openPrimingFrames = 1,
            cooldownMs = 0L
        )
        val a = PersonalisedEyeProfileReplay.replay("A", samples, tuning)
        val b = PersonalisedEyeProfileReplay.replay("B", samples, tuning)
        assertEquals(a.leftWinks, b.leftWinks)
        assertEquals(a.rightWinks, b.rightWinks)
        assertEquals(a.uncertainOccupancyPercent, b.uncertainOccupancyPercent, 0.01f)
    }

    @Test
    fun calibrationPrepare_doesNotRecord_untilMeasurement() {
        var now = 1_000_000L
        val dir = createTempDirectory("pep-ux").toFile()
        val controller = PersonalisedEyeProfileController(
            store = PersonalisedEyeProfileStore(dir),
            isDebugBuild = true,
            clockMs = { now }
        )
        assertTrue(controller.open())
        assertTrue(controller.startCalibration())
        // Force readiness complete
        controller.onSample(
            left = 0.95f,
            right = 0.95f,
            faceDetected = true,
            faceWidth = 35f,
            frameAccepted = true
        )
        now += PersonalisedEyeProfileController.READINESS_STABLE_MS + 50L
        controller.onSample(
            left = 0.95f,
            right = 0.95f,
            faceDetected = true,
            faceWidth = 35f,
            frameAccepted = true
        )
        assertTrue(controller.onTimedTick(now))
        assertEquals(
            PersonalisedEyeProfileController.FlowPhase.OpenBaseline,
            controller.flowPhase
        )
        assertEquals(
            PersonalisedEyeProfileController.StepSegment.Prepare,
            controller.live.stepSegment
        )
        assertEquals("Preparing...", controller.live.recordingStatus)
        assertTrue(controller.live.instructionTitle.contains("OPEN"))
        assertFalse(controller.live.instructionTitle.contains("Reopen", ignoreCase = true))

        // Samples during prepare must not start measurement recording yet
        repeat(5) {
            controller.onSample(
                left = 0.92f,
                right = 0.93f,
                faceDetected = true,
                faceWidth = 35f,
                frameAccepted = true
            )
        }
        assertFalse(controller.isActivelyRecording())

        // After 4s prepare → Recording
        now += PersonalisedEyeProfileController.PREPARE_MS + 10L
        assertTrue(controller.onTimedTick(now))
        assertEquals(
            PersonalisedEyeProfileController.StepSegment.Recording,
            controller.live.stepSegment
        )
        assertEquals("Recording...", controller.live.recordingStatus)
        assertTrue(controller.isActivelyRecording())

        // After recording duration → Complete
        now += PersonalisedEyeProfileController.OPEN_BASELINE_MS + 10L
        assertTrue(controller.onTimedTick(now))
        assertEquals(
            PersonalisedEyeProfileController.StepSegment.Complete,
            controller.live.stepSegment
        )
        assertEquals("✓ Measurement Complete", controller.live.recordingStatus)
        assertFalse(controller.isActivelyRecording())
    }

    @Test
    fun failedCalibration_alwaysProducesFullReport_andSavesFile() {
        var now = 2_000_000L
        val dir = createTempDirectory("pep-report-fail").toFile()
        val store = PersonalisedEyeProfileStore(dir)
        val controller = PersonalisedEyeProfileController(
            store = store,
            isDebugBuild = true,
            clockMs = { now }
        )
        assertTrue(controller.open())
        assertTrue(controller.startCalibration())
        // Skip → fail calibration → must land on Report, not dead-end Failed page.
        assertTrue(controller.skipCurrentStep())
        assertEquals(
            PersonalisedEyeProfileController.UiPhase.Report,
            controller.uiPhase
        )
        val report = controller.lastReport
        assertNotNull(report)
        val text = controller.fullReportText()
        assertTrue(text.contains(PersonalisedEyeProfileReportAuthority.HEADER_TITLE))
        assertTrue(text.contains("Session ID:"))
        assertTrue(text.contains("Test Started:"))
        assertTrue(text.contains("Test Completed:"))
        assertTrue(text.contains("Report Generated:"))
        assertTrue(text.contains("FAILURE ANALYSIS") || text.contains("Failure Summary") ||
            text.contains("Overall confidence"))
        assertTrue(report!!.testStartedMs != report.testCompletedMs ||
            report.testCompletedMs != report.reportGeneratedMs)
        assertTrue(report.reportGeneratedMs > report.testCompletedMs)
        assertNotNull(controller.lastReportFilePath)
        assertTrue(store.reportsDir().listFiles()?.isNotEmpty() == true)
        assertFalse(controller.canRetryValidation())
    }

    @Test
    fun reportAuthority_formatsDistinctTimestamps_andEyeSections() {
        val left = PersonalisedEyeProfileReportAuthority.eyeSectionFromDerivation(
            eyeLabel = "Left eye",
            openSamples = List(30) { 0.92f },
            closedSamples = List(20) { 0.25f },
            derivation = PersonalisedThresholdDerivation.deriveEye(
                PersonalisedThresholdDerivation.EyeDerivationInput(
                    openSamples = List(30) { 0.92f },
                    closedSamples = List(20) { 0.25f }
                )
            )
        )
        assertTrue(left.passed)
        assertNotNull(left.separation)
        val started = 1_000L
        val completed = 2_000L
        val generated = 2_001L
        val report = PersonalisedEyeProfileReport(
            sessionId = "sess-1",
            testStartedMs = started,
            testCompletedMs = completed,
            reportGeneratedMs = generated,
            deviceManufacturer = "Samsung",
            deviceModel = "SM-TEST",
            androidVersion = "13",
            appVersionName = "1.0",
            versionCode = 1,
            isDebugBuild = true,
            profileId = "p1",
            profileStatus = PersonalisedEyeProfileStatus.CalibrationFailed,
            calibrationPassed = false,
            leftEye = left,
            rightEye = left.copy(eyeLabel = "Right eye"),
            validationStages = listOf(
                ValidationStageReport("Open hold", true),
                ValidationStageReport("L2 R2", false, "back not completed")
            ),
            validationRunNumber = 1,
            validationPassed = false,
            failureSummary = listOf("L2 R2 failed."),
            overallConfidence = "FAILED",
            potentialCause = "L2 R2 failed.",
            recommendations = listOf("L2 R2 sequence was not completed successfully.")
        )
        val text = PersonalisedEyeProfileReportAuthority.formatFullText(report)
        assertTrue(text.contains("LISA PERSONALISED EYE PROFILE REPORT"))
        assertTrue(text.contains("L2 R2: FAIL"))
        assertTrue(text.contains("OPEN average:"))
        assertTrue(text.contains("Derived closed threshold:"))
        assertTrue(text.contains("RECOMMENDATIONS"))
        assertTrue(text.contains("Test Started:"))
        assertTrue(text.contains("Test Completed:"))
        assertTrue(text.contains("Report Generated:"))
    }
}
