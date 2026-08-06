package com.idworx.lisa.features.eyediagnostic

/**
 * Explicit Eye Test Mode wizard states — one screen / one step at a time.
 * Mandatory single-eye tests sit between Step 8 and the phase Result.
 */
enum class EyeTestFlowState {
    WithoutGlassesPreparation,
    WithoutGlassesStep1,
    WithoutGlassesStep2,
    WithoutGlassesStep3,
    WithoutGlassesStep4,
    WithoutGlassesStep5,
    WithoutGlassesStep6,
    WithoutGlassesStep7,
    WithoutGlassesStep8,
    WithoutGlassesLeftEyeTest,
    WithoutGlassesRightEyeTest,
    WithoutGlassesResult,
    WithGlassesPreparation,
    WithGlassesStep1,
    WithGlassesStep2,
    WithGlassesStep3,
    WithGlassesStep4,
    WithGlassesStep5,
    WithGlassesStep6,
    WithGlassesStep7,
    WithGlassesStep8,
    WithGlassesLeftEyeTest,
    WithGlassesRightEyeTest,
    WithGlassesResult,
    TestComplete,
    FullResults
}

enum class EyeTestStepKind {
    Preparation,
    TimedLook,
    WinkLeft,
    WinkRight,
    Rest,
    ObserveL1R1,
    ObserveL2R2,
    SingleEyeLeft,
    SingleEyeRight,
    Result,
    TestComplete,
    FullResults
}

enum class EyeTestComponentId {
    WithoutGlassesMain,
    WithoutGlassesLeftEye,
    WithoutGlassesRightEye,
    WithGlassesMain,
    WithGlassesLeftEye,
    WithGlassesRightEye
}

object EyeTestFlowAuthority {
    const val TOTAL_GUIDED_STEPS: Int = 8
    const val LOOK_AHEAD_MS: Long = 10_000L
    const val REST_MS: Long = 5_000L
    const val FINAL_LOOK_MS: Long = 10_000L
    const val TARGET_WINKS: Int = 5
    const val WINK_STEP_MAX_MS: Long = 30_000L
    const val L1R1_STEP_MAX_MS: Long = 30_000L
    const val L2R2_STEP_MAX_MS: Long = 40_000L
    /** Wall-clock max for one mandatory single-eye threshold test. */
    const val SINGLE_EYE_TEST_MAX_MS: Long = 90_000L
    const val READINESS_ANYWAY_AFTER_MS: Long = 10_000L
    const val MIN_FACE_WIDTH_PERCENT: Float = 18f
    const val MAX_FACE_WIDTH_PERCENT: Float = 75f

    val stateOrder: List<EyeTestFlowState> = listOf(
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

    fun stepKind(state: EyeTestFlowState): EyeTestStepKind = when (state) {
        EyeTestFlowState.WithoutGlassesPreparation,
        EyeTestFlowState.WithGlassesPreparation -> EyeTestStepKind.Preparation
        EyeTestFlowState.WithoutGlassesStep1,
        EyeTestFlowState.WithGlassesStep1,
        EyeTestFlowState.WithoutGlassesStep8,
        EyeTestFlowState.WithGlassesStep8 -> EyeTestStepKind.TimedLook
        EyeTestFlowState.WithoutGlassesStep2,
        EyeTestFlowState.WithGlassesStep2 -> EyeTestStepKind.WinkLeft
        EyeTestFlowState.WithoutGlassesStep3,
        EyeTestFlowState.WithoutGlassesStep5,
        EyeTestFlowState.WithGlassesStep3,
        EyeTestFlowState.WithGlassesStep5 -> EyeTestStepKind.Rest
        EyeTestFlowState.WithoutGlassesStep4,
        EyeTestFlowState.WithGlassesStep4 -> EyeTestStepKind.WinkRight
        EyeTestFlowState.WithoutGlassesStep6,
        EyeTestFlowState.WithGlassesStep6 -> EyeTestStepKind.ObserveL1R1
        EyeTestFlowState.WithoutGlassesStep7,
        EyeTestFlowState.WithGlassesStep7 -> EyeTestStepKind.ObserveL2R2
        EyeTestFlowState.WithoutGlassesLeftEyeTest,
        EyeTestFlowState.WithGlassesLeftEyeTest -> EyeTestStepKind.SingleEyeLeft
        EyeTestFlowState.WithoutGlassesRightEyeTest,
        EyeTestFlowState.WithGlassesRightEyeTest -> EyeTestStepKind.SingleEyeRight
        EyeTestFlowState.WithoutGlassesResult,
        EyeTestFlowState.WithGlassesResult -> EyeTestStepKind.Result
        EyeTestFlowState.TestComplete -> EyeTestStepKind.TestComplete
        EyeTestFlowState.FullResults -> EyeTestStepKind.FullResults
    }

    fun guidedStepNumber(state: EyeTestFlowState): Int? = when (state) {
        EyeTestFlowState.WithoutGlassesStep1, EyeTestFlowState.WithGlassesStep1 -> 1
        EyeTestFlowState.WithoutGlassesStep2, EyeTestFlowState.WithGlassesStep2 -> 2
        EyeTestFlowState.WithoutGlassesStep3, EyeTestFlowState.WithGlassesStep3 -> 3
        EyeTestFlowState.WithoutGlassesStep4, EyeTestFlowState.WithGlassesStep4 -> 4
        EyeTestFlowState.WithoutGlassesStep5, EyeTestFlowState.WithGlassesStep5 -> 5
        EyeTestFlowState.WithoutGlassesStep6, EyeTestFlowState.WithGlassesStep6 -> 6
        EyeTestFlowState.WithoutGlassesStep7, EyeTestFlowState.WithGlassesStep7 -> 7
        EyeTestFlowState.WithoutGlassesStep8, EyeTestFlowState.WithGlassesStep8 -> 8
        else -> null
    }

    fun phaseKind(state: EyeTestFlowState): EyeTestSessionKind? = when (state) {
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
        EyeTestFlowState.WithoutGlassesResult -> EyeTestSessionKind.WITHOUT_GLASSES
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
        EyeTestFlowState.WithGlassesResult -> EyeTestSessionKind.WITH_GLASSES
        EyeTestFlowState.TestComplete,
        EyeTestFlowState.FullResults -> null
    }

    fun componentId(state: EyeTestFlowState): EyeTestComponentId? = when (state) {
        EyeTestFlowState.WithoutGlassesStep1,
        EyeTestFlowState.WithoutGlassesStep2,
        EyeTestFlowState.WithoutGlassesStep3,
        EyeTestFlowState.WithoutGlassesStep4,
        EyeTestFlowState.WithoutGlassesStep5,
        EyeTestFlowState.WithoutGlassesStep6,
        EyeTestFlowState.WithoutGlassesStep7,
        EyeTestFlowState.WithoutGlassesStep8 -> EyeTestComponentId.WithoutGlassesMain
        EyeTestFlowState.WithoutGlassesLeftEyeTest -> EyeTestComponentId.WithoutGlassesLeftEye
        EyeTestFlowState.WithoutGlassesRightEyeTest -> EyeTestComponentId.WithoutGlassesRightEye
        EyeTestFlowState.WithGlassesStep1,
        EyeTestFlowState.WithGlassesStep2,
        EyeTestFlowState.WithGlassesStep3,
        EyeTestFlowState.WithGlassesStep4,
        EyeTestFlowState.WithGlassesStep5,
        EyeTestFlowState.WithGlassesStep6,
        EyeTestFlowState.WithGlassesStep7,
        EyeTestFlowState.WithGlassesStep8 -> EyeTestComponentId.WithGlassesMain
        EyeTestFlowState.WithGlassesLeftEyeTest -> EyeTestComponentId.WithGlassesLeftEye
        EyeTestFlowState.WithGlassesRightEyeTest -> EyeTestComponentId.WithGlassesRightEye
        else -> null
    }

    fun timedDurationMs(state: EyeTestFlowState): Long? = when (stepKind(state)) {
        EyeTestStepKind.TimedLook -> when (guidedStepNumber(state)) {
            1 -> LOOK_AHEAD_MS
            8 -> FINAL_LOOK_MS
            else -> LOOK_AHEAD_MS
        }
        EyeTestStepKind.Rest -> REST_MS
        else -> null
    }

    fun maxDurationMs(state: EyeTestFlowState): Long? = when (stepKind(state)) {
        EyeTestStepKind.TimedLook, EyeTestStepKind.Rest -> timedDurationMs(state)
        EyeTestStepKind.WinkLeft, EyeTestStepKind.WinkRight -> WINK_STEP_MAX_MS
        EyeTestStepKind.ObserveL1R1 -> L1R1_STEP_MAX_MS
        EyeTestStepKind.ObserveL2R2 -> L2R2_STEP_MAX_MS
        EyeTestStepKind.SingleEyeLeft, EyeTestStepKind.SingleEyeRight -> SINGLE_EYE_TEST_MAX_MS
        else -> null
    }

    fun isFailureTolerantStep(state: EyeTestFlowState): Boolean = when (stepKind(state)) {
        EyeTestStepKind.WinkLeft,
        EyeTestStepKind.WinkRight,
        EyeTestStepKind.ObserveL1R1,
        EyeTestStepKind.ObserveL2R2,
        EyeTestStepKind.SingleEyeLeft,
        EyeTestStepKind.SingleEyeRight -> true
        else -> false
    }

    fun isSingleEyeTest(state: EyeTestFlowState): Boolean =
        stepKind(state) == EyeTestStepKind.SingleEyeLeft ||
            stepKind(state) == EyeTestStepKind.SingleEyeRight

    fun next(state: EyeTestFlowState): EyeTestFlowState? {
        val i = stateOrder.indexOf(state)
        if (i < 0 || i >= stateOrder.lastIndex) return null
        return stateOrder[i + 1]
    }

    fun isObservingL2R2(state: EyeTestFlowState): Boolean =
        stepKind(state) == EyeTestStepKind.ObserveL2R2

    fun allowsBlinkBackToExit(state: EyeTestFlowState): Boolean =
        !isObservingL2R2(state) &&
            stepKind(state) != EyeTestStepKind.ObserveL1R1 &&
            stepKind(state) != EyeTestStepKind.WinkLeft &&
            stepKind(state) != EyeTestStepKind.WinkRight &&
            !isSingleEyeTest(state)

    fun instructionTitle(state: EyeTestFlowState): String = when (stepKind(state)) {
        EyeTestStepKind.Preparation -> when (phaseKind(state)) {
            EyeTestSessionKind.WITHOUT_GLASSES -> "Without Glasses Test"
            EyeTestSessionKind.WITH_GLASSES -> "With Glasses Test"
            null -> "Eye Test Mode"
        }
        EyeTestStepKind.TimedLook -> when (guidedStepNumber(state)) {
            8 -> "Final steady look"
            else -> "Look straight ahead"
        }
        EyeTestStepKind.WinkLeft -> "Wink left 5 times"
        EyeTestStepKind.WinkRight -> "Wink right 5 times"
        EyeTestStepKind.Rest -> "Rest"
        EyeTestStepKind.ObserveL1R1 -> "Perform L1 R1"
        EyeTestStepKind.ObserveL2R2 -> "Perform L2 R2"
        EyeTestStepKind.SingleEyeLeft -> "Left Eye Threshold Test"
        EyeTestStepKind.SingleEyeRight -> "Right Eye Threshold Test"
        EyeTestStepKind.Result -> "Phase result"
        EyeTestStepKind.TestComplete -> "Test complete"
        EyeTestStepKind.FullResults -> "Full combined results"
    }

    fun instructionBody(state: EyeTestFlowState): String = when (stepKind(state)) {
        EyeTestStepKind.Preparation -> when (phaseKind(state)) {
            EyeTestSessionKind.WITHOUT_GLASSES ->
                "Remove your glasses and position yourself as you normally use LISA."
            EyeTestSessionKind.WITH_GLASSES ->
                "Put on the glasses that caused difficulty. Keep the phone, lighting, distance, and position as close as possible to the first test."
            null -> ""
        }
        EyeTestStepKind.TimedLook ->
            "Look straight ahead and keep both eyes open."
        EyeTestStepKind.WinkLeft ->
            "Wink your left eye 5 times."
        EyeTestStepKind.WinkRight ->
            "Wink your right eye 5 times."
        EyeTestStepKind.Rest ->
            "Keep both eyes open and rest."
        EyeTestStepKind.ObserveL1R1 ->
            "Wink left once, then wink right once."
        EyeTestStepKind.ObserveL2R2 ->
            "Wink left twice, then wink right twice."
        EyeTestStepKind.SingleEyeLeft ->
            "Mandatory left-eye threshold probe. Follow each instruction. No production gestures run."
        EyeTestStepKind.SingleEyeRight ->
            "Mandatory right-eye threshold probe. Follow each instruction. No production gestures run."
        EyeTestStepKind.Result ->
            "This phase’s main and single-eye results are saved."
        EyeTestStepKind.TestComplete ->
            "All six mandatory components finished. Open Full Results when ready."
        EyeTestStepKind.FullResults ->
            "Combined Without Glasses and With Glasses report for this session."
    }

    fun progressLabel(state: EyeTestFlowState): String {
        val phase = phaseKind(state)?.displayName ?: "Eye Test"
        val step = guidedStepNumber(state)
        return when (stepKind(state)) {
            EyeTestStepKind.SingleEyeLeft -> "$phase · Left Eye Test"
            EyeTestStepKind.SingleEyeRight -> "$phase · Right Eye Test"
            EyeTestStepKind.TimedLook,
            EyeTestStepKind.WinkLeft,
            EyeTestStepKind.WinkRight,
            EyeTestStepKind.Rest,
            EyeTestStepKind.ObserveL1R1,
            EyeTestStepKind.ObserveL2R2 ->
                if (step != null) "$phase · Main Test · Step $step of $TOTAL_GUIDED_STEPS"
                else phase
            EyeTestStepKind.Result -> "$phase · Result"
            EyeTestStepKind.TestComplete -> "Test Complete"
            EyeTestStepKind.FullResults -> "Full Results"
            EyeTestStepKind.Preparation -> phase
        }
    }

    fun readiness(
        faceDetected: Boolean,
        faceWidthPercent: Float?,
        leftNull: Boolean,
        rightNull: Boolean,
        frameAccepted: Boolean = false
    ): Readiness {
        val faceOk = faceDetected
        val width = faceWidthPercent
        val sizeOk = width != null &&
            width in MIN_FACE_WIDTH_PERCENT..MAX_FACE_WIDTH_PERCENT
        val leftOk = !leftNull
        val rightOk = !rightNull
        val signalStable = faceOk && leftOk && rightOk && frameAccepted
        val ready = faceOk && sizeOk && leftOk && rightOk
        val message = when {
            ready && signalStable -> "Ready to start."
            ready && !signalStable -> "Eyes detected, but signal is unstable."
            !faceOk -> "Face not detected. Centre your face in the camera."
            !leftOk || !rightOk -> "Both eyes must be detected (eye probabilities available)."
            !sizeOk -> "Move closer or farther so your face size looks normal for LISA."
            else -> "Not ready."
        }
        return Readiness(
            ready = ready,
            faceDetected = faceOk,
            faceSizeOk = sizeOk,
            leftEyeDetected = leftOk,
            rightEyeDetected = rightOk,
            signalStable = signalStable,
            message = message
        )
    }

    data class Readiness(
        val ready: Boolean,
        val faceDetected: Boolean,
        val faceSizeOk: Boolean,
        val leftEyeDetected: Boolean,
        val rightEyeDetected: Boolean,
        val signalStable: Boolean,
        val message: String
    )
}
