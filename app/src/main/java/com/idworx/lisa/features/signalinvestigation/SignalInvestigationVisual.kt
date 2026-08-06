package com.idworx.lisa.features.signalinvestigation

/**
 * UI-only mapping for Signal Investigation visual cues.
 * Standard mode: calm status, no movement arrows.
 * Advanced mode: pose arrows (engineering only).
 */
object SignalInvestigationVisual {
    enum class Arrow {
        None, Left, Right, Up, Down, Closer, Farther, TiltMore, TiltLess
    }

    enum class Tone {
        Red, Amber, Green, Recording, Neutral
    }

    data class Guide(
        val status: String,
        val arrow: Arrow,
        val tone: Tone,
        val progress01: Float,
        val showPerfectCheck: Boolean,
        val showRecordingDot: Boolean,
        val showCompleteCheck: Boolean,
        val eyesClosedHint: Boolean,
        val showNextPosition: Boolean = false
    )

    fun fromLive(live: SignalInvestigationController.LiveUi): Guide {
        if (live.investigationMode == SignalInvestigationMode.Standard) {
            return fromStandard(live)
        }
        val duration = phaseDurationMs(live.flowPhase, live.captureKind)
        return from(
            flowPhase = live.flowPhase,
            captureKind = live.captureKind,
            band = live.targetBand,
            correction = live.correction,
            remainingMs = live.remainingMs,
            phaseDurationMs = duration
        )
    }

    private fun fromStandard(live: SignalInvestigationController.LiveUi): Guide {
        val status = live.recordingStatus.ifBlank { "Look Naturally" }
        val duration = when {
            live.flowPhase == SignalInvestigationController.FlowPhase.Recording &&
                live.standardStep == StandardInvestigationStep.NaturalPosition ->
                SignalStandardAuthority.NATURAL_OPEN_MS
            live.flowPhase == SignalInvestigationController.FlowPhase.Recording ->
                SignalStandardAuthority.SINGLE_EYE_RECORD_MS
            live.flowPhase == SignalInvestigationController.FlowPhase.Complete ->
                SignalStandardAuthority.COMPLETE_PAUSE_MS
            live.flowPhase == SignalInvestigationController.FlowPhase.StandardObserve &&
                live.standardStep == StandardInvestigationStep.NaturalBlink ->
                SignalStandardAuthority.NATURAL_BLINK_OBSERVE_MS
            live.flowPhase == SignalInvestigationController.FlowPhase.StandardObserve &&
                live.standardStep == StandardInvestigationStep.L1R1 ->
                SignalStandardAuthority.L1R1_MAX_MS
            live.flowPhase == SignalInvestigationController.FlowPhase.StandardObserve &&
                live.standardStep == StandardInvestigationStep.L2R2 ->
                SignalStandardAuthority.L2R2_MAX_MS
            else -> 0L
        }
        val progress = if (duration <= 0L) {
            0f
        } else {
            (1f - (live.remainingMs.toFloat() / duration.toFloat())).coerceIn(0f, 1f)
        }
        val recording = live.flowPhase == SignalInvestigationController.FlowPhase.Recording
        val complete = live.flowPhase == SignalInvestigationController.FlowPhase.Complete
        val faceOk = live.faceDetected
        val tone = when {
            recording -> Tone.Recording
            complete -> Tone.Green
            faceOk -> Tone.Green
            live.environmentHint.isNotBlank() -> Tone.Amber
            else -> Tone.Amber
        }
        return Guide(
            status = status,
            arrow = Arrow.None,
            tone = tone,
            progress01 = progress,
            showPerfectCheck = faceOk && !recording && !complete,
            showRecordingDot = recording,
            showCompleteCheck = complete,
            eyesClosedHint = live.standardStep == StandardInvestigationStep.LeftEye ||
                live.standardStep == StandardInvestigationStep.RightEye,
            showNextPosition = false
        )
    }

    fun from(
        flowPhase: SignalInvestigationController.FlowPhase,
        captureKind: SignalInvestigationController.CaptureKind,
        band: SignalPoseGuidance.Band,
        correction: String,
        remainingMs: Long,
        phaseDurationMs: Long
    ): Guide {
        val progress = if (phaseDurationMs <= 0L) {
            0f
        } else {
            (1f - (remainingMs.toFloat() / phaseDurationMs.toFloat())).coerceIn(0f, 1f)
        }
        return when (flowPhase) {
            SignalInvestigationController.FlowPhase.Guiding -> guiding(band, correction)
            SignalInvestigationController.FlowPhase.Stabilizing -> Guide(
                status = "Hold Still",
                arrow = Arrow.None,
                tone = Tone.Green,
                progress01 = progress,
                showPerfectCheck = true,
                showRecordingDot = false,
                showCompleteCheck = false,
                eyesClosedHint = false
            )
            SignalInvestigationController.FlowPhase.Prepare -> Guide(
                status = if (captureKind == SignalInvestigationController.CaptureKind.Closed) {
                    "Close Eyes"
                } else {
                    "Hold Still"
                },
                arrow = Arrow.None,
                tone = Tone.Amber,
                progress01 = progress,
                showPerfectCheck = true,
                showRecordingDot = false,
                showCompleteCheck = false,
                eyesClosedHint = captureKind == SignalInvestigationController.CaptureKind.Closed
            )
            SignalInvestigationController.FlowPhase.Recording -> Guide(
                status = "Recording",
                arrow = Arrow.None,
                tone = Tone.Recording,
                progress01 = progress,
                showPerfectCheck = false,
                showRecordingDot = true,
                showCompleteCheck = false,
                eyesClosedHint = captureKind == SignalInvestigationController.CaptureKind.Closed
            )
            SignalInvestigationController.FlowPhase.Complete -> Guide(
                status = "Complete",
                arrow = Arrow.None,
                tone = Tone.Green,
                progress01 = 1f,
                showPerfectCheck = false,
                showRecordingDot = false,
                showCompleteCheck = true,
                eyesClosedHint = false
            )
            SignalInvestigationController.FlowPhase.EyeRecovery,
            SignalInvestigationController.FlowPhase.StandardRecovery -> Guide(
                status = "Open Eyes",
                arrow = Arrow.None,
                tone = Tone.Amber,
                progress01 = 0f,
                showPerfectCheck = false,
                showRecordingDot = false,
                showCompleteCheck = false,
                eyesClosedHint = false
            )
            SignalInvestigationController.FlowPhase.PositionResult -> {
                val showNext = remainingMs <= phaseDurationMs / 2L
                Guide(
                    status = if (showNext) "Next" else "Complete",
                    arrow = Arrow.None,
                    tone = Tone.Green,
                    progress01 = 1f,
                    showPerfectCheck = false,
                    showRecordingDot = false,
                    showCompleteCheck = !showNext,
                    eyesClosedHint = false,
                    showNextPosition = showNext
                )
            }
            else -> Guide(
                status = "Look Naturally",
                arrow = Arrow.None,
                tone = Tone.Amber,
                progress01 = 0f,
                showPerfectCheck = false,
                showRecordingDot = false,
                showCompleteCheck = false,
                eyesClosedHint = false
            )
        }
    }

    private fun guiding(band: SignalPoseGuidance.Band, correction: String): Guide {
        val c = correction.lowercase()
        val (status, arrow) = when {
            c.contains("closer") -> "Move Closer" to Arrow.Closer
            c.contains("farther") || c.contains("move back") -> "Move Back" to Arrow.Farther
            c.contains("phone slightly higher") ||
                (c.contains("move") && c.contains("higher")) -> "Move Up" to Arrow.Up
            c.contains("phone slightly lower") ||
                (c.contains("move") && c.contains("lower")) -> "Move Down" to Arrow.Down
            c.contains("more left") -> "Move Left" to Arrow.Left
            c.contains("more right") -> "Move Right" to Arrow.Right
            c.contains("tilt a little more") || c.contains("tilt more") ->
                "Tilt More" to Arrow.TiltMore
            c.contains("reduce the tilt") || c.contains("tilt less") ->
                "Tilt Less" to Arrow.TiltLess
            c.contains("centre your face") || c.contains("center your face") ->
                "Centre Face" to Arrow.None
            c.contains("position correct") || c == "hold still" ->
                "Perfect" to Arrow.None
            else -> when (band) {
                SignalPoseGuidance.Band.InTarget -> "Perfect" to Arrow.None
                SignalPoseGuidance.Band.TooLittle -> "Adjust" to Arrow.None
                SignalPoseGuidance.Band.TooFar -> "Adjust" to Arrow.None
                SignalPoseGuidance.Band.Unknown -> "Find Face" to Arrow.None
            }
        }
        val tone = when {
            band == SignalPoseGuidance.Band.InTarget || status == "Perfect" -> Tone.Green
            band == SignalPoseGuidance.Band.TooLittle -> Tone.Amber
            band == SignalPoseGuidance.Band.TooFar -> Tone.Red
            else -> Tone.Red
        }
        return Guide(
            status = status,
            arrow = arrow,
            tone = tone,
            progress01 = 0f,
            showPerfectCheck = status == "Perfect",
            showRecordingDot = false,
            showCompleteCheck = false,
            eyesClosedHint = false
        )
    }

    fun phaseDurationMs(
        flowPhase: SignalInvestigationController.FlowPhase,
        captureKind: SignalInvestigationController.CaptureKind
    ): Long = when (flowPhase) {
        SignalInvestigationController.FlowPhase.Prepare -> SignalPoseGuidance.PREPARE_MS
        SignalInvestigationController.FlowPhase.Recording ->
            if (captureKind == SignalInvestigationController.CaptureKind.Open) {
                SignalPoseGuidance.OPEN_RECORD_MS
            } else {
                SignalPoseGuidance.CLOSED_RECORD_MS
            }
        SignalInvestigationController.FlowPhase.Stabilizing -> SignalPoseGuidance.STABLE_REQUIRED_MS
        SignalInvestigationController.FlowPhase.Complete -> SignalPoseGuidance.COMPLETE_MS
        SignalInvestigationController.FlowPhase.PositionResult ->
            SignalPoseGuidance.POSITION_RESULT_MS
        else -> 0L
    }

    fun shortVoiceForCorrection(correction: String): String {
        val guide = guiding(SignalPoseGuidance.Band.TooLittle, correction)
        return shortVoice(guide.status)
    }

    fun shortVoice(status: String): String = when (status) {
        "Move Left" -> "Move left."
        "Move Right" -> "Move right."
        "Move Up" -> "Move up."
        "Move Down" -> "Move down."
        "Move Closer" -> "Closer."
        "Move Back" -> "Back."
        "Tilt More" -> "Tilt more."
        "Tilt Less" -> "Tilt less."
        "Hold Still", "Perfect" -> "Hold still."
        "Recording" -> "Recording."
        "Complete" -> "Done."
        "Open Eyes" -> "Open both eyes."
        "Close Eyes" -> "Close both eyes."
        "Centre Face", "Find Face" -> "Find the camera."
        else -> ""
    }

    fun arrowGlyph(arrow: Arrow): String = when (arrow) {
        Arrow.None -> ""
        Arrow.Left -> "←"
        Arrow.Right -> "→"
        Arrow.Up -> "↑"
        Arrow.Down -> "↓"
        Arrow.Closer -> "⊕"
        Arrow.Farther -> "⊖"
        Arrow.TiltMore -> "↺"
        Arrow.TiltLess -> "↻"
    }
}
