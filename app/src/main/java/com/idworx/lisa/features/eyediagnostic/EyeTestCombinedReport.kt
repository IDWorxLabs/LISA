package com.idworx.lisa.features.eyediagnostic

/**
 * Builds the mandatory six-component combined Eye Test report.
 */
object EyeTestCombinedReport {
    const val HEADER_TITLE = "LISA EYE TEST — NEW COMPLETE REPORT"

    fun allComponentsTerminal(slots: Map<EyeTestComponentId, EyeTestComponentSlot>): Boolean =
        EyeTestComponentId.entries.all { slots[it]?.hasTerminalOutcome == true }

    fun build(
        meta: EyeTestSessionMeta,
        slots: Map<EyeTestComponentId, EyeTestComponentSlot>,
        reportGeneratedMs: Long,
        withoutMain: EyeTestSessionSummary?,
        withMain: EyeTestSessionSummary?
    ): String {
        val generatedLocal = EyeTestSessionMeta.formatLocal(reportGeneratedMs)
        return buildString {
            appendLine(HEADER_TITLE)
            appendLine("Test Session ID: ${meta.sessionId}")
            appendLine("Test Started: ${meta.testStartedLocal}")
            appendLine(
                "Test Completed: ${meta.testCompletedLocal ?: "(in progress)"}"
            )
            appendLine("Report Generated: $generatedLocal")
            appendLine()
            appendLine("Date / local time: ${meta.testStartedLocal}")
            appendLine("Timezone offset: ${meta.timezoneOffset}")
            appendLine("Epoch start ms: ${meta.testStartedMs}")
            appendLine("Epoch completed ms: ${meta.testCompletedMs ?: "n/a"}")
            appendLine("Epoch report ms: $reportGeneratedMs")
            appendLine("Unique session UUID: ${meta.sessionId}")
            appendLine("App versionName: ${meta.appVersionName}")
            appendLine("versionCode: ${meta.appVersionCode}")
            appendLine("Device manufacturer: ${meta.deviceManufacturer}")
            appendLine("Device model: ${meta.deviceModel}")
            appendLine("Android version: ${meta.androidVersion}")
            appendLine("Sensitivity: ${meta.sensitivity}")
            appendLine("Response time (sec): ${meta.responseTimeSec}")
            appendLine("Diagnostic sample rate ms: ${meta.diagnosticSampleRateMs}")
            appendLine()

            appendComponent(this, "=== 1. Without Glasses · Main Test ===", slots[EyeTestComponentId.WithoutGlassesMain])
            appendSingleEye(this, "=== 2. Without Glasses · Left Eye Threshold Test ===", slots[EyeTestComponentId.WithoutGlassesLeftEye])
            appendSingleEye(this, "=== 3. Without Glasses · Right Eye Threshold Test ===", slots[EyeTestComponentId.WithoutGlassesRightEye])
            appendComponent(this, "=== 4. With Glasses · Main Test ===", slots[EyeTestComponentId.WithGlassesMain])
            appendSingleEye(this, "=== 5. With Glasses · Left Eye Threshold Test ===", slots[EyeTestComponentId.WithGlassesLeftEye])
            appendSingleEye(this, "=== 6. With Glasses · Right Eye Threshold Test ===", slots[EyeTestComponentId.WithGlassesRightEye])

            appendLine("=== Main phase comparison ===")
            val mainCompare = EyeTestComparisonReport.compare(withoutMain, withMain)
            appendLine(mainCompare.buildFullResultsText())
            appendLine()

            appendLine("=== Left-eye threshold comparison ===")
            appendEyeCompare(
                this,
                slots[EyeTestComponentId.WithoutGlassesLeftEye]?.singleEyeResult,
                slots[EyeTestComponentId.WithGlassesLeftEye]?.singleEyeResult,
                "left"
            )
            appendLine()
            appendLine("=== Right-eye threshold comparison ===")
            appendEyeCompare(
                this,
                slots[EyeTestComponentId.WithoutGlassesRightEye]?.singleEyeResult,
                slots[EyeTestComponentId.WithGlassesRightEye]?.singleEyeResult,
                "right"
            )
            appendLine()
            appendLine("=== Automatic factual findings ===")
            factualFindings(slots).forEach { appendLine("- $it") }
            if (!allComponentsTerminal(slots)) {
                appendLine()
                appendLine("WARNING: Report generated before all six components reached a terminal outcome.")
            }
        }
    }

    private fun appendComponent(
        out: StringBuilder,
        title: String,
        slot: EyeTestComponentSlot?
    ) {
        out.appendLine(title)
        if (slot == null) {
            out.appendLine("(missing)")
            out.appendLine()
            return
        }
        out.appendLine("Outcome: ${slot.outcome ?: "NotCompleted"}")
        slot.mainSummary?.phaseResultLines()?.forEach { out.appendLine(it) }
            ?: out.appendLine("(no main summary)")
        out.appendLine()
    }

    private fun appendSingleEye(
        out: StringBuilder,
        title: String,
        slot: EyeTestComponentSlot?
    ) {
        out.appendLine(title)
        if (slot?.singleEyeResult == null) {
            out.appendLine("Outcome: ${slot?.outcome ?: "NotCompleted"}")
            out.appendLine("(no single-eye result)")
            out.appendLine()
            return
        }
        slot.singleEyeResult!!.reportLines().forEach { out.appendLine(it) }
        out.appendLine()
    }

    private fun appendEyeCompare(
        out: StringBuilder,
        without: SingleEyeComponentResult?,
        with: SingleEyeComponentResult?,
        eyeLabel: String
    ) {
        if (without == null || with == null) {
            out.appendLine("Incomplete $eyeLabel-eye comparison (need both phases).")
            return
        }
        out.appendLine(
            "Open baseline avg: ${fmt(without.openBaselineAvg)} → ${fmt(with.openBaselineAvg)}"
        )
        out.appendLine(
            "Closed-hold avg (cycle means): " +
                "${fmt(avgCycleClose(without))} → ${fmt(avgCycleClose(with))}"
        )
        out.appendLine(
            "Min during close attempts: ${fmt(without.selectedMin)} → ${fmt(with.selectedMin)}"
        )
        out.appendLine(
            "Max during reopen: " +
                "${fmt(without.cycles.mapNotNull { it.highestDuringReopen }.maxOrNull())} → " +
                fmt(with.cycles.mapNotNull { it.highestDuringReopen }.maxOrNull())
        )
        out.appendLine(
            "Closed-threshold crossings: ${without.closedThresholdCrossings}/3 → " +
                "${with.closedThresholdCrossings}/3"
        )
        out.appendLine(
            "Reopen-threshold crossings: ${without.openThresholdCrossings}/3 → " +
                "${with.openThresholdCrossings}/3"
        )
        out.appendLine(
            "Uncertain-band entries: ${without.uncertainBandEntries} → ${with.uncertainBandEntries}"
        )
        out.appendLine(
            "Wink recognised: ${without.winkRecognisedCount}/3 → ${with.winkRecognisedCount}/3"
        )
        out.appendLine(
            "Top close/reopen failure (with): " +
                (with.cycles.mapNotNull { it.failureReason }.groupingBy { it }.eachCount()
                    .maxByOrNull { it.value }?.key ?: "none")
        )
    }

    fun factualFindings(slots: Map<EyeTestComponentId, EyeTestComponentSlot>): List<String> {
        val findings = mutableListOf<String>()
        fun eyePair(
            withoutId: EyeTestComponentId,
            withId: EyeTestComponentId,
            label: String
        ) {
            val a = slots[withoutId]?.singleEyeResult ?: return
            val b = slots[withId]?.singleEyeResult ?: return
            findings += "With glasses, the $label eye crossed the closed threshold " +
                "${b.closedThresholdCrossings} of 3 times, compared with " +
                "${a.closedThresholdCrossings} of 3 without glasses."
            val withCloseSamples = b.cycles.count { it.closedHoldAvg != null }
            if (b.totalSamples > 0) {
                val uncPct = if (b.timeOpenMs + b.timeClosedMs + b.timeUncertainMs > 0) {
                    b.timeUncertainMs * 100f /
                        (b.timeOpenMs + b.timeClosedMs + b.timeUncertainMs).toFloat()
                } else {
                    0f
                }
                findings += "With glasses, the $label eye spent " +
                    "${"%.1f".format(uncPct)}% of measured single-eye time in the uncertain band."
            }
            findings += "With glasses, the selected $label eye reopened above the open threshold in " +
                "${b.openThresholdCrossings} of 3 cycles."
            val stable = b.cycles.count { it.oppositeEyeStableDuringClose }
            findings += "The opposite eye remained stable during $stable of 3 attempted $label closes."
            if (
                a.closedThresholdCrossings == b.closedThresholdCrossings &&
                a.winkRecognisedCount == b.winkRecognisedCount &&
                kotlin.math.abs((a.openBaselineAvg ?: 0f) - (b.openBaselineAvg ?: 0f)) < 0.05f
            ) {
                findings += "No meaningful difference was measured for $label-eye open baseline / wink counts."
            }
            if (a.totalSamples < 10 || b.totalSamples < 10) {
                findings += "Sample size was insufficient for a strong $label-eye conclusion."
            }
            @Suppress("UNUSED_VARIABLE")
            val unused = withCloseSamples
        }
        eyePair(
            EyeTestComponentId.WithoutGlassesLeftEye,
            EyeTestComponentId.WithGlassesLeftEye,
            "left"
        )
        eyePair(
            EyeTestComponentId.WithoutGlassesRightEye,
            EyeTestComponentId.WithGlassesRightEye,
            "right"
        )
        val withMain = slots[EyeTestComponentId.WithGlassesMain]?.mainSummary
        if (withMain != null && withMain.decisionAnalysis.bothUncertainPercent >= 5f) {
            findings += "With glasses, both eyes were UNCERTAIN for " +
                "${"%.1f".format(withMain.decisionAnalysis.bothUncertainPercent)}% of main-test samples."
        }
        if (findings.isEmpty()) {
            findings += "No automatic findings generated (incomplete data)."
        }
        return findings
    }

    private fun avgCycleClose(r: SingleEyeComponentResult): Float? =
        EyeTestStats.avg(r.cycles.mapNotNull { it.closedHoldAvg })

    private fun fmt(v: Float?): String = v?.let { "%.3f".format(it) } ?: "n/a"
}
