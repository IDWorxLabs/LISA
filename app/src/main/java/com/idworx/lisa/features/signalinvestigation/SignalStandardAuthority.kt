package com.idworx.lisa.features.signalinvestigation

/**
 * Standard (still) Signal Investigation — observational helpers only.
 * Does not alter production blink thresholds or BlinkDetectionProcessor.
 */
object SignalStandardAuthority {
    const val NATURAL_OPEN_MS: Long = 5_000L
    const val SINGLE_EYE_RECORD_MS: Long = 3_000L
    const val NATURAL_BLINK_OBSERVE_MS: Long = 12_000L
    const val L1R1_MAX_MS: Long = 30_000L
    const val L2R2_MAX_MS: Long = 40_000L
    const val FACE_STABLE_MS: Long = 1_500L
    const val BOTH_OPEN_HOLD_MS: Long = 1_000L
    const val COMPLETE_PAUSE_MS: Long = 1_200L
    const val VOICE_COOLDOWN_MS: Long = 4_000L

    /** Observational only — not production thresholds. */
    const val OPEN_HINT: Float = 0.65f
    const val CLOSED_HINT: Float = 0.45f
    const val MIN_FACE_WIDTH_PCT: Float = 18f
    const val IDEAL_FACE_WIDTH_PCT: Float = 22f
    const val MAX_NULL_PERCENT_FOR_SEQUENCE: Float = 20f
    const val MIN_SEPARATION_FOR_SEQUENCE: Float = 0.08f
    const val MIN_NATURAL_OPEN_SAMPLES: Int = 15
    const val MIN_CLOSED_SAMPLES: Int = 8

    enum class EyeState {
        Unknown,
        BothOpen,
        LeftClosedOnly,
        RightClosedOnly,
        BothClosed,
        Partial
    }

    fun eyeState(left: Float?, right: Float?): EyeState {
        if (left == null || right == null) return EyeState.Unknown
        val lOpen = left >= OPEN_HINT
        val rOpen = right >= OPEN_HINT
        val lClosed = left <= CLOSED_HINT
        val rClosed = right <= CLOSED_HINT
        return when {
            lOpen && rOpen -> EyeState.BothOpen
            lClosed && rOpen -> EyeState.LeftClosedOnly
            rClosed && lOpen -> EyeState.RightClosedOnly
            lClosed && rClosed -> EyeState.BothClosed
            else -> EyeState.Partial
        }
    }

    fun bothEyesOpen(left: Float?, right: Float?): Boolean =
        eyeState(left, right) == EyeState.BothOpen

    fun leftEyeClosedOnly(left: Float?, right: Float?): Boolean =
        eyeState(left, right) == EyeState.LeftClosedOnly

    fun rightEyeClosedOnly(left: Float?, right: Float?): Boolean =
        eyeState(left, right) == EyeState.RightClosedOnly

    /**
     * Face usable in natural pose — does not require head tilt or distance change.
     * Poor conditions become diagnoses, not movement commands.
     */
    fun faceUsableForNatural(faceDetected: Boolean, faceWidthPct: Float?): Boolean {
        if (!faceDetected) return false
        if (faceWidthPct == null) return faceDetected
        return faceWidthPct >= MIN_FACE_WIDTH_PCT
    }

    fun diagnoseEnvironment(
        faceDetected: Boolean,
        faceWidthPct: Float?,
        nullPercent: Float,
        leftSep: Float?,
        rightSep: Float?,
        lighting: LightingCondition,
        glasses: GlassesCondition
    ): List<String> {
        val out = mutableListOf<String>()
        if (!faceDetected) {
            out += "Face not consistently detected — check camera angle and that the face is fully in frame."
        }
        if (faceWidthPct != null && faceWidthPct < IDEAL_FACE_WIDTH_PCT) {
            out += "Face appears small in frame (width ${"%.1f".format(faceWidthPct)}%) — " +
                "likely camera distance or placement."
        }
        if (nullPercent > 15f) {
            out += "Frequent missing eye probabilities (${"%.0f".format(nullPercent)}%) — " +
                "possible occlusion, reflection, or lighting."
        }
        when (lighting) {
            LightingCondition.Outdoor ->
                out += "Outdoor lighting can cause glare and reflections on glasses or skin."
            LightingCondition.Mixed ->
                out += "Mixed lighting can reduce eye-signal consistency."
            LightingCondition.Indoor -> Unit
        }
        if (glasses == GlassesCondition.YES) {
            out += "Glasses reflections can reduce eye visibility — soft, even indoor light helps."
        }
        val weakL = leftSep != null && leftSep < MIN_SEPARATION_FOR_SEQUENCE
        val weakR = rightSep != null && rightSep < MIN_SEPARATION_FOR_SEQUENCE
        if (weakL || weakR) {
            out += "Low open/closed eye separation — detection may be unreliable in this placement."
        }
        return out.distinct()
    }

    fun caregiverRecommendations(diagnoses: List<String>, faceWidthPct: Float?): List<String> {
        val out = mutableListOf<String>()
        out += "Keep the patient comfortably still. Do not ask them to tilt, lean, or turn."
        if (diagnoses.any { it.contains("small", ignoreCase = true) } ||
            (faceWidthPct != null && faceWidthPct < IDEAL_FACE_WIDTH_PCT)
        ) {
            out += "This may improve if a caregiver adjusts the phone position slightly closer " +
                "or centres the camera on the face."
        }
        if (diagnoses.any {
                it.contains("reflection", ignoreCase = true) ||
                    it.contains("lighting", ignoreCase = true) ||
                    it.contains("glare", ignoreCase = true)
            }
        ) {
            out += "This may improve if a caregiver softens lighting or reduces glare on glasses."
        }
        if (diagnoses.any { it.contains("camera angle", ignoreCase = true) || it.contains("frame", ignoreCase = true) }) {
            out += "This may improve if a caregiver adjusts the phone angle so the full face is visible."
        }
        if (out.size == 1) {
            out += "If signal quality remains poor, prefer Advanced Engineering Investigation " +
                "only with a caregiver present — never as a patient movement test."
        }
        return out.distinct()
    }

    fun qualityAcceptableForSequences(
        openSampleCount: Int,
        leftClosedCount: Int,
        rightClosedCount: Int,
        nullPercent: Float,
        leftSep: Float?,
        rightSep: Float?,
        faceWidthPct: Float?
    ): Boolean {
        if (openSampleCount < MIN_NATURAL_OPEN_SAMPLES) return false
        if (leftClosedCount < MIN_CLOSED_SAMPLES || rightClosedCount < MIN_CLOSED_SAMPLES) return false
        if (nullPercent > MAX_NULL_PERCENT_FOR_SEQUENCE) return false
        if (faceWidthPct != null && faceWidthPct < MIN_FACE_WIDTH_PCT) return false
        val lOk = leftSep != null && leftSep >= MIN_SEPARATION_FOR_SEQUENCE
        val rOk = rightSep != null && rightSep >= MIN_SEPARATION_FOR_SEQUENCE
        return lOk && rOk
    }

    /**
     * Simple open-probability wink edge detector for observational L1/L2 sequences.
     */
    class WinkEdgeTracker {
        private var leftWasClosed = false
        private var rightWasClosed = false

        /** Returns "L", "R", or null for this sample. */
        fun onSample(left: Float?, right: Float?): String? {
            val state = eyeState(left, right)
            var event: String? = null
            when (state) {
                EyeState.LeftClosedOnly -> {
                    if (!leftWasClosed) event = "L"
                    leftWasClosed = true
                    rightWasClosed = false
                }
                EyeState.RightClosedOnly -> {
                    if (!rightWasClosed) event = "R"
                    rightWasClosed = true
                    leftWasClosed = false
                }
                EyeState.BothOpen -> {
                    leftWasClosed = false
                    rightWasClosed = false
                }
                else -> Unit
            }
            return event
        }

        fun reset() {
            leftWasClosed = false
            rightWasClosed = false
        }
    }

    fun isL1R1(order: List<String>): Boolean =
        order == listOf("L", "R")

    fun isL2R2(order: List<String>): Boolean =
        order == listOf("L", "L", "R", "R")

    /** Natural blink: both eyes briefly not fully open then both open again. */
    class NaturalBlinkTracker {
        private var dipped = false
        var blinkCount: Int = 0
            private set

        fun onSample(left: Float?, right: Float?) {
            if (left == null || right == null) return
            val bothOpen = left >= OPEN_HINT && right >= OPEN_HINT
            val bothLow = left < OPEN_HINT && right < OPEN_HINT
            if (bothLow) {
                dipped = true
            } else if (bothOpen && dipped) {
                blinkCount++
                dipped = false
            }
        }

        fun reset() {
            dipped = false
            blinkCount = 0
        }
    }
}

enum class SignalInvestigationMode {
    Standard,
    AdvancedEngineering
}

enum class StandardInvestigationStep(val displayName: String) {
    NaturalPosition("Natural Position"),
    LeftEye("Left Eye"),
    RightEye("Right Eye"),
    NaturalBlink("Natural Blink Behaviour"),
    L1R1("L1 R1"),
    L2R2("L2 R2")
}

data class StandardStepReport(
    val step: StandardInvestigationStep,
    val completed: Boolean,
    val skipped: Boolean = false,
    val openLeftAvg: Float? = null,
    val openRightAvg: Float? = null,
    val closedLeftAvg: Float? = null,
    val closedRightAvg: Float? = null,
    val leftSeparation: Float? = null,
    val rightSeparation: Float? = null,
    val sampleCount: Int = 0,
    val nullPercent: Float = 0f,
    val notes: String = ""
)
