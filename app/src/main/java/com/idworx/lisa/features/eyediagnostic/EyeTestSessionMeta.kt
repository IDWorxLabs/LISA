package com.idworx.lisa.features.eyediagnostic

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import kotlin.math.sqrt

/**
 * Terminal outcome for one of the six mandatory Eye Test components.
 */
enum class EyeTestComponentOutcome {
    Success,
    TimedOut,
    Skipped,
    Failed,
    EndedEarly,
    NotCompletedDueToDetectionFailure
}

data class EyeTestSessionMeta(
    val sessionId: String,
    val shortSessionId: String,
    val testStartedMs: Long,
    val testStartedLocal: String,
    val timezoneOffset: String,
    var testCompletedMs: Long? = null,
    var testCompletedLocal: String? = null,
    var appVersionName: String = "",
    var appVersionCode: Int = 0,
    var deviceManufacturer: String = "",
    var deviceModel: String = "",
    var androidVersion: String = "",
    var sensitivity: Int = 0,
    var responseTimeSec: Int = 0,
    var diagnosticSampleRateMs: Long = LisaEyeDiagnostic.DEFAULT_MIN_INTERVAL_MS
) {
    companion object {
        fun create(
            nowMs: Long = System.currentTimeMillis(),
            uuid: UUID = UUID.randomUUID()
        ): EyeTestSessionMeta {
            val id = uuid.toString()
            val short = id.substring(0, 8)
            return EyeTestSessionMeta(
                sessionId = id,
                shortSessionId = short,
                testStartedMs = nowMs,
                testStartedLocal = formatLocal(nowMs),
                timezoneOffset = formatOffset(nowMs)
            )
        }

        fun formatLocal(epochMs: Long): String {
            val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.US)
            fmt.timeZone = TimeZone.getDefault()
            return fmt.format(Date(epochMs))
        }

        fun formatOffset(epochMs: Long): String {
            val offsetMs = TimeZone.getDefault().getOffset(epochMs)
            val totalMin = offsetMs / 60_000
            val sign = if (totalMin >= 0) "+" else "-"
            val abs = kotlin.math.abs(totalMin)
            val h = abs / 60
            val m = abs % 60
            return String.format(Locale.US, "UTC%s%02d:%02d", sign, h, m)
        }

        fun stampForFile(epochMs: Long): String {
            val fmt = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
            fmt.timeZone = TimeZone.getDefault()
            return fmt.format(Date(epochMs))
        }
    }

    fun markCompleted(nowMs: Long) {
        testCompletedMs = nowMs
        testCompletedLocal = formatLocal(nowMs)
    }

    fun reportGeneratedNow(nowMs: Long = System.currentTimeMillis()): String = formatLocal(nowMs)
}

data class SingleEyeCycleRecord(
    val cycleIndex: Int,
    val openBaselineAvg: Float?,
    val closedHoldAvg: Float?,
    val reopenAvg: Float?,
    val lowestDuringClose: Float?,
    val highestDuringReopen: Float?,
    val closedThresholdCrossed: Boolean,
    val openThresholdCrossed: Boolean,
    val closeTransitionAccepted: Boolean,
    val reopenTransitionAccepted: Boolean,
    val wouldRecogniseWink: Boolean,
    val failureReason: String?,
    val oppositeEyeStableDuringClose: Boolean
)

data class SingleEyeComponentResult(
    val componentId: EyeTestComponentId,
    val eye: SingleEyeThresholdSubtest.EyeTarget,
    val outcome: EyeTestComponentOutcome,
    val closedThreshold: Float,
    val openThreshold: Float,
    val totalSamples: Int,
    val acceptedSamples: Int,
    val rejectedSamples: Int,
    val selectedMin: Float?,
    val selectedMax: Float?,
    val selectedAvg: Float?,
    val selectedMedian: Float?,
    val selectedStdDev: Float?,
    val oppositeMin: Float?,
    val oppositeMax: Float?,
    val oppositeAvg: Float?,
    val oppositeMedian: Float?,
    val oppositeStdDev: Float?,
    val openBaselineAvg: Float?,
    val closedThresholdCrossings: Int,
    val openThresholdCrossings: Int,
    val uncertainBandEntries: Int,
    val timeOpenMs: Long,
    val timeClosedMs: Long,
    val timeUncertainMs: Long,
    val longestUncertainMs: Long,
    val nullSelectedCount: Int,
    val nullOppositeCount: Int,
    val nullSelectedPercent: Float,
    val nullOppositePercent: Float,
    val winkRecognisedCount: Int,
    val cycles: List<SingleEyeCycleRecord>,
    val topDecisionReason: String?,
    val topRejectionReason: String?,
    val elapsedMs: Long
) {
    fun reportLines(): List<String> = buildList {
        add("Component: $componentId ($eye)")
        add("Outcome: $outcome")
        add("Thresholds closed/open: ${"%.3f".format(closedThreshold)} / ${"%.3f".format(openThreshold)}")
        add("Samples total/accepted/rejected: $totalSamples / $acceptedSamples / $rejectedSamples")
        add(
            "Selected raw min/max/avg/median/std: " +
                "${fmt(selectedMin)} / ${fmt(selectedMax)} / ${fmt(selectedAvg)} / " +
                "${fmt(selectedMedian)} / ${fmt(selectedStdDev)}"
        )
        add(
            "Opposite raw min/max/avg/median/std: " +
                "${fmt(oppositeMin)} / ${fmt(oppositeMax)} / ${fmt(oppositeAvg)} / " +
                "${fmt(oppositeMedian)} / ${fmt(oppositeStdDev)}"
        )
        add("Open baseline avg: ${fmt(openBaselineAvg)}")
        add("Closed/open threshold crossings: $closedThresholdCrossings / $openThresholdCrossings")
        add("Uncertain-band entries: $uncertainBandEntries")
        add("Time OPEN/CLOSED/UNCERTAIN ms: $timeOpenMs / $timeClosedMs / $timeUncertainMs")
        add("Longest UNCERTAIN ms: $longestUncertainMs")
        add("Null selected/opposite %: ${"%.1f".format(nullSelectedPercent)} / ${"%.1f".format(nullOppositePercent)}")
        add("Production-style wink recognised count: $winkRecognisedCount / ${cycles.size}")
        add("Top decision reason: ${topDecisionReason ?: "none"}")
        add("Top rejection reason: ${topRejectionReason ?: "none"}")
        add("Elapsed ms: $elapsedMs")
        cycles.forEach { c ->
            add(
                "Cycle ${c.cycleIndex + 1}: closeMin=${fmt(c.lowestDuringClose)} " +
                    "reopenMax=${fmt(c.highestDuringReopen)} " +
                    "closedX=${c.closedThresholdCrossed} openX=${c.openThresholdCrossed} " +
                    "wink=${c.wouldRecogniseWink} oppositeStable=${c.oppositeEyeStableDuringClose} " +
                    "fail=${c.failureReason ?: "none"}"
            )
        }
    }

    private fun fmt(v: Float?): String = v?.let { "%.3f".format(it) } ?: "n/a"
}

data class EyeTestComponentSlot(
    val id: EyeTestComponentId,
    var outcome: EyeTestComponentOutcome? = null,
    var mainSummary: EyeTestSessionSummary? = null,
    var singleEyeResult: SingleEyeComponentResult? = null
) {
    val hasTerminalOutcome: Boolean get() = outcome != null
}

object EyeTestStats {
    fun median(values: List<Float>): Float? {
        if (values.isEmpty()) return null
        val sorted = values.sorted()
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 0) {
            (sorted[mid - 1] + sorted[mid]) / 2f
        } else {
            sorted[mid]
        }
    }

    fun stdDev(values: List<Float>): Float? {
        if (values.size < 2) return null
        val mean = values.average()
        val varSum = values.sumOf { (it - mean) * (it - mean) }
        return sqrt(varSum / values.size).toFloat()
    }

    fun avg(values: List<Float>): Float? =
        values.takeIf { it.isNotEmpty() }?.average()?.toFloat()
}
