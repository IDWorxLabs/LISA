package com.idworx.lisa.features.personalisedeyeprofile

/**
 * Strict validation rules for Personalised Eye Profile prototype.
 * Never executes production gestures — observational scoring only.
 */
object PersonalisedEyeProfileValidation {
    const val TARGET_WINKS: Int = 5
    const val MAX_FALSE_POSITIVES: Int = 0
    const val MAX_NULL_PERCENT: Float = 15f
    const val MAX_UNCERTAIN_PERCENT: Float = 55f

    data class LiveCounters(
        val leftWinks: Int,
        val rightWinks: Int,
        val l1r1Success: Boolean,
        val l2r2Success: Boolean,
        val falsePositiveWinks: Int,
        val unexpectedSequence: Boolean,
        val nullProbabilityPercent: Float,
        val uncertainOccupancyPercent: Float
    )

    fun evaluateRun(runNumber: Int, counters: LiveCounters): PersonalisedValidationRunResult {
        val failures = mutableListOf<String>()
        if (counters.leftWinks < TARGET_WINKS) {
            failures += "Left winks detected ${counters.leftWinks} < $TARGET_WINKS."
        }
        if (counters.rightWinks < TARGET_WINKS) {
            failures += "Right winks detected ${counters.rightWinks} < $TARGET_WINKS."
        }
        if (!counters.l1r1Success) failures += "L1 R1 failed."
        if (!counters.l2r2Success) failures += "L2 R2 failed."
        if (counters.falsePositiveWinks > MAX_FALSE_POSITIVES) {
            failures += "Too many false winks (${counters.falsePositiveWinks})."
        }
        if (counters.unexpectedSequence) failures += "Unexpected sequence detected."
        if (counters.nullProbabilityPercent > MAX_NULL_PERCENT) {
            failures += "Null probability rate too high " +
                "(${"%.1f".format(counters.nullProbabilityPercent)}%)."
        }
        if (counters.uncertainOccupancyPercent > MAX_UNCERTAIN_PERCENT) {
            failures += "Uncertain-band occupancy too high " +
                "(${"%.1f".format(counters.uncertainOccupancyPercent)}%)."
        }
        return PersonalisedValidationRunResult(
            runNumber = runNumber,
            passed = failures.isEmpty(),
            leftWinksDetected = counters.leftWinks,
            rightWinksDetected = counters.rightWinks,
            l1r1Success = counters.l1r1Success,
            l2r2Success = counters.l2r2Success,
            falsePositiveWinks = counters.falsePositiveWinks,
            unexpectedSequence = counters.unexpectedSequence,
            nullProbabilityPercent = counters.nullProbabilityPercent,
            uncertainOccupancyPercent = counters.uncertainOccupancyPercent,
            failureReasons = failures
        )
    }

    fun applyRunToProfile(
        profile: PersonalisedEyeProfile,
        result: PersonalisedValidationRunResult,
        nowMs: Long
    ): PersonalisedEyeProfile {
        val updated = profile.copy(updatedAtMs = nowMs)
        return when (result.runNumber) {
            1 -> {
                updated.validationRun1 = result
                updated.falsePositiveCount = result.falsePositiveWinks
                if (result.passed) {
                    updated.status = PersonalisedEyeProfileStatus.ValidationRun1Passed
                    updated.failureReasons = emptyList()
                } else {
                    updated.status = PersonalisedEyeProfileStatus.FailedValidation
                    updated.failureReasons = result.failureReasons
                }
                // Run 1 alone never marks Validated.
                if (updated.status == PersonalisedEyeProfileStatus.Validated) {
                    updated.status = PersonalisedEyeProfileStatus.ValidationRun1Passed
                }
                updated
            }
            2 -> {
                updated.validationRun2 = result
                updated.falsePositiveCount =
                    (updated.validationRun1?.falsePositiveWinks ?: 0) + result.falsePositiveWinks
                val run1Passed = updated.validationRun1?.passed == true
                when {
                    run1Passed && result.passed -> {
                        updated.status = PersonalisedEyeProfileStatus.Validated
                        updated.failureReasons = emptyList()
                    }
                    else -> {
                        updated.status = PersonalisedEyeProfileStatus.FailedValidation
                        updated.failureReasons = buildList {
                            if (!run1Passed) add("Validation Run 1 did not pass.")
                            addAll(result.failureReasons)
                        }
                    }
                }
                updated
            }
            else -> updated.copy(
                status = PersonalisedEyeProfileStatus.FailedValidation,
                failureReasons = listOf("Invalid validation run number.")
            )
        }
    }
}
