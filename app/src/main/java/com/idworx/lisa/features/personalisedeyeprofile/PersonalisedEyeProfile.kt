package com.idworx.lisa.features.personalisedeyeprofile

import com.idworx.lisa.features.blinkdetectionreliability.BlinkDetectionTuning
import java.util.UUID

enum class PersonalisedEyeProfileStatus {
    Draft,
    CalibrationInProgress,
    CalibrationFailed,
    Calibrated,
    ReadyForValidationRun1,
    ValidationRun1Passed,
    ReadyForValidationRun2,
    Validated,
    FailedValidation,
    Disabled
}

data class PersonalisedEyeStats(
    val min: Float? = null,
    val max: Float? = null,
    val average: Float? = null,
    val median: Float? = null,
    val stdDev: Float? = null,
    val p25: Float? = null,
    val p75: Float? = null,
    val sampleCount: Int = 0
)

data class PersonalisedValidationRunResult(
    val runNumber: Int,
    val passed: Boolean,
    val leftWinksDetected: Int = 0,
    val rightWinksDetected: Int = 0,
    val l1r1Success: Boolean = false,
    val l2r2Success: Boolean = false,
    val falsePositiveWinks: Int = 0,
    val unexpectedSequence: Boolean = false,
    val nullProbabilityPercent: Float = 0f,
    val uncertainOccupancyPercent: Float = 0f,
    val failureReasons: List<String> = emptyList()
)

/**
 * Debug-only personalised eye profile. Never written into production settings.
 */
data class PersonalisedEyeProfile(
    val profileId: String = UUID.randomUUID().toString(),
    val createdAtMs: Long,
    var updatedAtMs: Long,
    val deviceManufacturer: String = "",
    val deviceModel: String = "",
    val androidVersion: String = "",
    val appVersionName: String = "",
    val versionCode: Int = 0,
    val calibrationConditionLabel: String = "glasses_or_user_condition",
    val leftOpenBaseline: Float = 0f,
    val leftClosedBaseline: Float = 0f,
    val leftClosedMinimum: Float = 0f,
    val leftReopenMaximum: Float = 0f,
    val leftClosedThreshold: Float = 0f,
    val leftOpenThreshold: Float = 0f,
    val leftUncertaintyLower: Float = 0f,
    val leftUncertaintyUpper: Float = 0f,
    val rightOpenBaseline: Float = 0f,
    val rightClosedBaseline: Float = 0f,
    val rightClosedMinimum: Float = 0f,
    val rightReopenMaximum: Float = 0f,
    val rightClosedThreshold: Float = 0f,
    val rightOpenThreshold: Float = 0f,
    val rightUncertaintyLower: Float = 0f,
    val rightUncertaintyUpper: Float = 0f,
    val requiredConsecutiveCloseFrames: Int = 2,
    val requiredConsecutiveReopenFrames: Int = 1,
    val calibrationSampleCounts: Map<String, Int> = emptyMap(),
    var validationRun1: PersonalisedValidationRunResult? = null,
    var validationRun2: PersonalisedValidationRunResult? = null,
    var falsePositiveCount: Int = 0,
    var status: PersonalisedEyeProfileStatus = PersonalisedEyeProfileStatus.Draft,
    var failureReasons: List<String> = emptyList(),
    val derivationNotes: String = ""
) {
    fun toCandidateTuning(base: BlinkDetectionTuning = BlinkDetectionTuning.default): BlinkDetectionTuning =
        base.copy(
            closedEyeThreshold = (leftClosedThreshold + rightClosedThreshold) / 2f,
            openEyeThreshold = (leftOpenThreshold + rightOpenThreshold) / 2f,
            requiredWinkFrames = requiredConsecutiveCloseFrames,
            openPrimingFrames = requiredConsecutiveReopenFrames.coerceAtLeast(1),
            leftClosedEyeThreshold = leftClosedThreshold,
            rightClosedEyeThreshold = rightClosedThreshold,
            leftOpenEyeThreshold = leftOpenThreshold,
            rightOpenEyeThreshold = rightOpenThreshold
        )

    fun isUsableValidated(): Boolean = status == PersonalisedEyeProfileStatus.Validated
}

object PersonalisedEyeProfileAccess {
    const val ENTRY_TITLE = "Eye Tracking Profile Prototype"
    const val ENTRY_SUPPORTING = "Debug only — Standard Mode unchanged"

    fun isEntryVisible(isDebugBuild: Boolean): Boolean = isDebugBuild
    fun isScreenAllowed(isDebugBuild: Boolean): Boolean = isDebugBuild
}
