package com.idworx.lisa.features.eyediagnostic

/**
 * Aggregate metrics for one captured Eye Test session.
 */
data class EyeTestSessionSummary(
    val kind: EyeTestSessionKind,
    val sampleCount: Int,
    val acceptedFramePercent: Float,
    val nullLeftPercent: Float,
    val nullRightPercent: Float,
    val averageLeftOpenProbability: Float?,
    val averageRightOpenProbability: Float?,
    val leftWinkDetectionsPeak: Int,
    val rightWinkDetectionsPeak: Int,
    val leftWinkTarget: Int = EyeTestFlowAuthority.TARGET_WINKS,
    val rightWinkTarget: Int = EyeTestFlowAuthority.TARGET_WINKS,
    val mostCommonRejectionReason: String?,
    val sampleSizeTooSmall: Boolean,
    val l1r1Success: Boolean = false,
    val l2r2Success: Boolean = false,
    val l1r1Outcome: EyeTestSequenceOutcome = EyeTestSequenceOutcome.NotCompleted,
    val l2r2Outcome: EyeTestSequenceOutcome = EyeTestSequenceOutcome.NotCompleted,
    val stepRecords: List<EyeTestStepRecord> = emptyList(),
    val completedStepCount: Int = 0,
    val failedStepCount: Int = 0,
    val timedOutStepCount: Int = 0,
    val skippedStepCount: Int = 0,
    val notCompletedDueToDetectionFailureCount: Int = 0,
    val phaseEndedEarly: Boolean = false,
    val decisionAnalysis: EyeDecisionAnalysis = EyeDecisionAnalysis()
) {
    companion object {
        const val MIN_RELIABLE_SAMPLES: Int = 40

        fun fromSamples(
            kind: EyeTestSessionKind,
            samples: List<LisaEyeDiagnostic.Sample>,
            l1r1Success: Boolean = false,
            l2r2Success: Boolean = false,
            l1r1Outcome: EyeTestSequenceOutcome = if (l1r1Success) {
                EyeTestSequenceOutcome.Success
            } else {
                EyeTestSequenceOutcome.NotCompleted
            },
            l2r2Outcome: EyeTestSequenceOutcome = if (l2r2Success) {
                EyeTestSequenceOutcome.Success
            } else {
                EyeTestSequenceOutcome.NotCompleted
            },
            leftWinkDetectionsPeak: Int? = null,
            rightWinkDetectionsPeak: Int? = null,
            stepRecords: List<EyeTestStepRecord> = emptyList(),
            phaseEndedEarly: Boolean = false,
            decisionTracker: EyeDecisionTraceTracker? = null
        ): EyeTestSessionSummary {
            val n = samples.size
            val accepted = samples.count { it.frameAccepted }
            val nullLeft = samples.count { it.leftEyeOpenProbability == null }
            val nullRight = samples.count { it.rightEyeOpenProbability == null }
            val leftVals = samples.mapNotNull { it.leftEyeOpenProbability }
            val rightVals = samples.mapNotNull { it.rightEyeOpenProbability }
            val reasonCounts = samples.mapNotNull { it.rejectionReason }
                .groupingBy { it }
                .eachCount()
            val topReason = reasonCounts.maxByOrNull { it.value }?.key
            val leftPeak = leftWinkDetectionsPeak
                ?: samples.maxOfOrNull { it.leftWinkCount }
                ?: 0
            val rightPeak = rightWinkDetectionsPeak
                ?: samples.maxOfOrNull { it.rightWinkCount }
                ?: 0
            return EyeTestSessionSummary(
                kind = kind,
                sampleCount = n,
                acceptedFramePercent = if (n == 0) 0f else accepted * 100f / n,
                nullLeftPercent = if (n == 0) 0f else nullLeft * 100f / n,
                nullRightPercent = if (n == 0) 0f else nullRight * 100f / n,
                averageLeftOpenProbability = leftVals.takeIf { it.isNotEmpty() }?.average()?.toFloat(),
                averageRightOpenProbability = rightVals.takeIf { it.isNotEmpty() }?.average()?.toFloat(),
                leftWinkDetectionsPeak = leftPeak,
                rightWinkDetectionsPeak = rightPeak,
                mostCommonRejectionReason = topReason,
                sampleSizeTooSmall = n < MIN_RELIABLE_SAMPLES,
                l1r1Success = l1r1Success,
                l2r2Success = l2r2Success,
                l1r1Outcome = l1r1Outcome,
                l2r2Outcome = l2r2Outcome,
                stepRecords = stepRecords,
                completedStepCount = stepRecords.count { it.completion == EyeTestStepCompletion.Success },
                failedStepCount = stepRecords.count {
                    it.completion == EyeTestStepCompletion.TimedOut
                },
                timedOutStepCount = stepRecords.count {
                    it.completion == EyeTestStepCompletion.TimedOut
                },
                skippedStepCount = stepRecords.count {
                    it.completion == EyeTestStepCompletion.Skipped
                },
                notCompletedDueToDetectionFailureCount = stepRecords.count {
                    it.completion == EyeTestStepCompletion.NotCompletedDueToDetectionFailure
                },
                phaseEndedEarly = phaseEndedEarly,
                decisionAnalysis = EyeDecisionAnalysis.fromSamples(samples, decisionTracker)
            )
        }
    }

    fun sequenceLabel(outcome: EyeTestSequenceOutcome): String = when (outcome) {
        EyeTestSequenceOutcome.Success -> "Success"
        EyeTestSequenceOutcome.Failed -> "Failed"
        EyeTestSequenceOutcome.NotCompleted -> "Not completed"
    }

    fun phaseResultLines(): List<String> = buildList {
        add("Completed steps: $completedStepCount")
        add("Failed / timed-out steps: $timedOutStepCount")
        add("Skipped steps: $skippedStepCount")
        add("Not completed due to detection failure: $notCompletedDueToDetectionFailureCount")
        add("Total samples: $sampleCount")
        add("Accepted-frame percentage: ${"%.1f".format(acceptedFramePercent)}%")
        add("Null left-eye percentage: ${"%.1f".format(nullLeftPercent)}%")
        add("Null right-eye percentage: ${"%.1f".format(nullRightPercent)}%")
        add("Left wink target / detected: $leftWinkTarget / $leftWinkDetectionsPeak")
        add("Right wink target / detected: $rightWinkTarget / $rightWinkDetectionsPeak")
        add("L1 R1: ${sequenceLabel(l1r1Outcome)}")
        add("L2 R2: ${sequenceLabel(l2r2Outcome)}")
        add("Most common rejection reason: ${mostCommonRejectionReason ?: "none"}")
        if (phaseEndedEarly) {
            add("Phase ended early: yes (incomplete session)")
        }
        stepRecords.forEach { add(it.reportLine()) }
        addAll(decisionAnalysis.reportLines())
    }
}

data class EyeTestComparisonReport(
    val withoutGlasses: EyeTestSessionSummary?,
    val withGlasses: EyeTestSessionSummary?,
    val bothPresent: Boolean,
    val sampleSizeWarning: String?,
    val incompleteSessionWarning: String?,
    val factualFinding: String?,
    val withoutL1R1Success: Boolean = false,
    val withoutL2R2Success: Boolean = false,
    val withL1R1Success: Boolean = false,
    val withL2R2Success: Boolean = false,
    val decisionFindings: List<String> = emptyList()
) {
    companion object {
        fun compare(
            without: EyeTestSessionSummary?,
            with: EyeTestSessionSummary?,
            withoutL1R1: Boolean = without?.l1r1Success == true,
            withoutL2R2: Boolean = without?.l2r2Success == true,
            withL1R1: Boolean = with?.l1r1Success == true,
            withL2R2: Boolean = with?.l2r2Success == true
        ): EyeTestComparisonReport {
            val both = without != null && with != null
            val sampleWarning = when {
                !both -> "Complete both Without Glasses and With Glasses sessions to compare."
                without!!.sampleSizeTooSmall || with!!.sampleSizeTooSmall ->
                    "Sample size is too small for a reliable comparison (need at least " +
                        "${EyeTestSessionSummary.MIN_RELIABLE_SAMPLES} samples per session)."
                else -> null
            }
            val incompleteWarning = when {
                with?.phaseEndedEarly == true ->
                    "With Glasses session was incomplete (ended early). " +
                        "Recorded samples: ${with.sampleCount}. Comparison still generated."
                with != null && (
                    with.timedOutStepCount > 0 ||
                        with.skippedStepCount > 0 ||
                        with.notCompletedDueToDetectionFailureCount > 0 ||
                        with.l1r1Outcome != EyeTestSequenceOutcome.Success ||
                        with.l2r2Outcome != EyeTestSequenceOutcome.Success
                    ) && without?.l1r1Outcome == EyeTestSequenceOutcome.Success ->
                    "With Glasses session did not complete all detection steps normally."
                else -> null
            }
            val finding = when {
                with == null || without == null -> null
                with.phaseEndedEarly ||
                    with.timedOutStepCount > 0 ||
                    with.skippedStepCount > 0 ||
                    with.notCompletedDueToDetectionFailureCount > 0 ||
                    with.l1r1Outcome != EyeTestSequenceOutcome.Success ||
                    with.l2r2Outcome != EyeTestSequenceOutcome.Success ->
                    "With glasses, the test could not complete normally because eye events " +
                        "were not detected within the allowed time."
                else -> null
            }
            val decisionFindings = buildList {
                with?.decisionAnalysis?.factualFindings?.forEach { add("With glasses: $it") }
                if (both && without != null && with != null) {
                    val a = without.decisionAnalysis
                    val b = with.decisionAnalysis
                    appendDecisionDiff(this, a, b)
                }
            }
            return EyeTestComparisonReport(
                withoutGlasses = without,
                withGlasses = with,
                bothPresent = both,
                sampleSizeWarning = sampleWarning,
                incompleteSessionWarning = incompleteWarning,
                factualFinding = finding,
                withoutL1R1Success = withoutL1R1,
                withoutL2R2Success = withoutL2R2,
                withL1R1Success = withL1R1,
                withL2R2Success = withL2R2,
                decisionFindings = decisionFindings
            )
        }

        private fun appendDecisionDiff(
            out: MutableList<String>,
            a: EyeDecisionAnalysis,
            b: EyeDecisionAnalysis
        ) {
            out += "Both-UNCERTAIN occupancy: ${"%.1f".format(a.bothUncertainPercent)}% → " +
                "${"%.1f".format(b.bothUncertainPercent)}%"
            out += "Left UNCERTAIN %: ${"%.1f".format(a.leftUncertainPercent)} → " +
                "${"%.1f".format(b.leftUncertainPercent)}"
            out += "Right UNCERTAIN %: ${"%.1f".format(a.rightUncertainPercent)} → " +
                "${"%.1f".format(b.rightUncertainPercent)}"
            out += "Left candidates started/cancelled: " +
                "${a.leftCandidatesStarted}/${a.leftCandidatesCancelled} → " +
                "${b.leftCandidatesStarted}/${b.leftCandidatesCancelled}"
            out += "Right candidates started/cancelled: " +
                "${a.rightCandidatesStarted}/${a.rightCandidatesCancelled} → " +
                "${b.rightCandidatesStarted}/${b.rightCandidatesCancelled}"
            out += "Left reopen fail: ${a.leftReopenFailures} → ${b.leftReopenFailures}"
            out += "Right reopen fail: ${a.rightReopenFailures} → ${b.rightReopenFailures}"
            out += "Cooldown blocks: ${a.cooldownBlocks} → ${b.cooldownBlocks}"
            out += "Closed crossings L/R: " +
                "${a.leftClosedThresholdCrossings}/${a.rightClosedThresholdCrossings} → " +
                "${b.leftClosedThresholdCrossings}/${b.rightClosedThresholdCrossings}"
        }
    }

    fun highlightDifference(withoutValue: Float?, withValue: Float?, threshold: Float = 15f): Boolean {
        if (withoutValue == null || withValue == null) return false
        return kotlin.math.abs(withoutValue - withValue) >= threshold
    }

    fun buildFullResultsText(): String = buildString {
        appendLine("LISA Eye Test Mode — Full Results")
        appendLine()
        appendLine("=== Without Glasses ===")
        withoutGlasses?.phaseResultLines()?.forEach { appendLine(it) }
            ?: appendLine("(no session)")
        appendLine()
        appendLine("=== With Glasses ===")
        withGlasses?.phaseResultLines()?.forEach { appendLine(it) }
            ?: appendLine("(no session)")
        appendLine()
        appendLine("=== Comparison ===")
        if (bothPresent) {
            val a = withoutGlasses!!
            val b = withGlasses!!
            appendLine(
                "Accepted-frame % difference: " +
                    "${"%.1f".format(b.acceptedFramePercent - a.acceptedFramePercent)}"
            )
            appendLine(
                "Null left % difference: " +
                    "${"%.1f".format(b.nullLeftPercent - a.nullLeftPercent)}"
            )
            appendLine(
                "Null right % difference: " +
                    "${"%.1f".format(b.nullRightPercent - a.nullRightPercent)}"
            )
            appendLine(
                "Avg left open difference: " +
                    formatProbDiff(a.averageLeftOpenProbability, b.averageLeftOpenProbability)
            )
            appendLine(
                "Avg right open difference: " +
                    formatProbDiff(a.averageRightOpenProbability, b.averageRightOpenProbability)
            )
            appendLine(
                "Left wink detections: ${a.leftWinkDetectionsPeak} → ${b.leftWinkDetectionsPeak}"
            )
            appendLine(
                "Right wink detections: ${a.rightWinkDetectionsPeak} → ${b.rightWinkDetectionsPeak}"
            )
            appendLine(
                "L1 R1: ${a.sequenceLabel(a.l1r1Outcome)} → ${b.sequenceLabel(b.l1r1Outcome)}"
            )
            appendLine(
                "L2 R2: ${a.sequenceLabel(a.l2r2Outcome)} → ${b.sequenceLabel(b.l2r2Outcome)}"
            )
            appendLine(
                "Top rejection: ${a.mostCommonRejectionReason ?: "none"} → " +
                    (b.mostCommonRejectionReason ?: "none")
            )
            appendLine()
            appendLine("=== Decision-trace comparison ===")
            decisionFindings.forEach { appendLine(it) }
        } else {
            appendLine(sampleSizeWarning ?: "Both phases required.")
        }
        incompleteSessionWarning?.let {
            appendLine()
            appendLine("WARNING: $it")
        }
        factualFinding?.let {
            appendLine()
            appendLine("Finding: $it")
        }
        appendLine()
        appendLine("Notes: production has no probability smoothing (smoothed=raw).")
        appendLine("Outcome legend: Success | Failed (timeout) | Skipped (manual) | " +
            "Not completed due to detection failure | Phase ended early")
    }

    private fun formatProbDiff(a: Float?, b: Float?): String {
        if (a == null || b == null) return "n/a"
        return "%.3f".format(b - a)
    }
}
