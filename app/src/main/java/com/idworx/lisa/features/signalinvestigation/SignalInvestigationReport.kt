package com.idworx.lisa.features.signalinvestigation

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import kotlin.math.abs

object SignalInvestigationAccess {
    const val ENTRY_TITLE = "Signal Investigation"
    const val ENTRY_SUPPORTING =
        "Debug only — can LISA detect this person's eyes in their natural position?"

    fun isEntryVisible(isDebugBuild: Boolean): Boolean = isDebugBuild
    fun isScreenAllowed(isDebugBuild: Boolean): Boolean = isDebugBuild
}

enum class SignalPosition(val displayName: String) {
    HeadStraight("Head Straight"),
    HeadTiltLeft("Head Tilt Left"),
    HeadTiltRight("Head Tilt Right"),
    PhoneHigher("Phone Higher"),
    PhoneLower("Phone Lower"),
    Closer("Closer"),
    Further("Further")
}

enum class GlassesCondition { YES, NO }
enum class LightingCondition { Indoor, Outdoor, Mixed }

data class PositionSignalStats(
    val position: SignalPosition,
    val openLeftAvg: Float? = null,
    val openRightAvg: Float? = null,
    val openLeftMin: Float? = null,
    val openRightMin: Float? = null,
    val openLeftMax: Float? = null,
    val openRightMax: Float? = null,
    val closedLeftAvg: Float? = null,
    val closedRightAvg: Float? = null,
    val closedLeftMin: Float? = null,
    val closedRightMin: Float? = null,
    val closedLeftMax: Float? = null,
    val closedRightMax: Float? = null,
    val leftSeparation: Float? = null,
    val rightSeparation: Float? = null,
    val leftJitter: Float? = null,
    val rightJitter: Float? = null,
    val nullPercent: Float = 0f,
    val rejectedPercent: Float = 0f,
    val frameAcceptancePercent: Float = 0f,
    val avgFaceWidthPercent: Float? = null,
    val avgHeadYaw: Float? = null,
    val avgHeadRoll: Float? = null,
    val openSampleCount: Int = 0,
    val closedSampleCount: Int = 0,
    val framesSeen: Int = 0,
    val framesRejected: Int = 0,
    // Pose-control extensions
    val targetPoseDescription: String = "",
    val targetRangeLabel: String = "",
    val achievedAvgYaw: Float? = null,
    val achievedAvgRoll: Float? = null,
    val achievedFaceCenterOffsetY: Float? = null,
    val achievedFaceWidthPercent: Float? = null,
    val timeToReachTargetMs: Long = 0L,
    val stableInTargetPercent: Float = 0f,
    val samplesRejectedPoseMismatch: Int = 0,
    val measurementRepeated: Boolean = false,
    val voiceGuidanceEventCount: Int = 0,
    val conditionValid: Boolean = false
)

data class SignalInvestigationReport(
    val sessionId: String,
    val testStartedMs: Long,
    val testCompletedMs: Long,
    val reportGeneratedMs: Long,
    val deviceManufacturer: String,
    val deviceModel: String,
    val androidVersion: String,
    val appVersionName: String,
    val versionCode: Int,
    val isDebugBuild: Boolean,
    val glasses: GlassesCondition,
    val lighting: LightingCondition,
    val distanceLabel: String,
    val faceWidthPercent: Float?,
    val cameraResolution: String,
    val investigationMode: SignalInvestigationMode = SignalInvestigationMode.Standard,
    val baseline: PositionSignalStats?,
    val positions: List<PositionSignalStats>,
    val standardSteps: List<StandardStepReport> = emptyList(),
    val environmentDiagnoses: List<String> = emptyList(),
    val naturalBlinkCount: Int = 0,
    val l1r1Outcome: String = "n/a",
    val l2r2Outcome: String = "n/a",
    val sequencesAttempted: Boolean = false,
    val bestLeftSeparation: SignalPosition?,
    val bestRightSeparation: SignalPosition?,
    val lowestJitter: SignalPosition?,
    val lowestRejected: SignalPosition?,
    val bestOverall: SignalPosition?,
    val engineeringFindings: List<String>,
    val recommendations: List<String>,
    val ttsAvailable: Boolean = true,
    val voiceGuidanceEventsTotal: Int = 0
)

object SignalInvestigationReportAuthority {
    const val HEADER_TITLE = "LISA SIGNAL INVESTIGATION REPORT"

    fun newSessionId(): String = UUID.randomUUID().toString()

    fun formatLocal(ms: Long): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS Z", Locale.US)
        fmt.timeZone = TimeZone.getDefault()
        return fmt.format(Date(ms))
    }

    fun formatFileStamp(ms: Long): String {
        val fmt = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
        fmt.timeZone = TimeZone.getDefault()
        return fmt.format(Date(ms))
    }

    fun percentile(sorted: List<Float>, p: Float): Float {
        if (sorted.isEmpty()) return 0f
        if (sorted.size == 1) return sorted[0]
        val idx = (p * (sorted.size - 1)).coerceIn(0f, (sorted.size - 1).toFloat())
        val lo = idx.toInt()
        val hi = (lo + 1).coerceAtMost(sorted.lastIndex)
        val frac = idx - lo
        return sorted[lo] * (1f - frac) + sorted[hi] * frac
    }

    fun stdDev(values: List<Float>): Float? {
        if (values.size < 2) return if (values.isEmpty()) null else 0f
        val mean = values.average()
        val variance = values.sumOf { (it - mean) * (it - mean) } / values.size
        return kotlin.math.sqrt(variance).toFloat()
    }

    fun separation(open: List<Float>, closed: List<Float>): Float? {
        if (open.size < 5 || closed.size < 5) return null
        val o = open.sorted()
        val c = closed.sorted()
        return percentile(o, 0.25f) - percentile(c, 0.75f)
    }

    fun buildPositionStats(
        position: SignalPosition,
        openLeft: List<Float>,
        openRight: List<Float>,
        closedLeft: List<Float>,
        closedRight: List<Float>,
        framesSeen: Int,
        framesRejected: Int,
        nullCount: Int,
        faceWidths: List<Float>,
        yaws: List<Float>,
        rolls: List<Float>,
        faceCenterYs: List<Float> = emptyList(),
        targetPoseDescription: String = "",
        targetRangeLabel: String = "",
        timeToReachTargetMs: Long = 0L,
        stableInTargetPercent: Float = 0f,
        samplesRejectedPoseMismatch: Int = 0,
        measurementRepeated: Boolean = false,
        voiceGuidanceEventCount: Int = 0,
        conditionValid: Boolean = false,
        baselineFaceCenterY: Float? = null
    ): PositionSignalStats {
        val accepted = (framesSeen - framesRejected).coerceAtLeast(0)
        val nullPct = if (framesSeen == 0) 0f else nullCount * 100f / framesSeen
        val rejectedPct = if (framesSeen == 0) 0f else framesRejected * 100f / framesSeen
        val acceptPct = if (framesSeen == 0) 0f else accepted * 100f / framesSeen
        val avgCenterY = faceCenterYs.averageOrNull()
        return PositionSignalStats(
            position = position,
            openLeftAvg = openLeft.averageOrNull(),
            openRightAvg = openRight.averageOrNull(),
            openLeftMin = openLeft.minOrNull(),
            openRightMin = openRight.minOrNull(),
            openLeftMax = openLeft.maxOrNull(),
            openRightMax = openRight.maxOrNull(),
            closedLeftAvg = closedLeft.averageOrNull(),
            closedRightAvg = closedRight.averageOrNull(),
            closedLeftMin = closedLeft.minOrNull(),
            closedRightMin = closedRight.minOrNull(),
            closedLeftMax = closedLeft.maxOrNull(),
            closedRightMax = closedRight.maxOrNull(),
            leftSeparation = separation(openLeft, closedLeft),
            rightSeparation = separation(openRight, closedRight),
            leftJitter = stdDev(openLeft),
            rightJitter = stdDev(openRight),
            nullPercent = nullPct,
            rejectedPercent = rejectedPct,
            frameAcceptancePercent = acceptPct,
            avgFaceWidthPercent = faceWidths.averageOrNull(),
            avgHeadYaw = yaws.averageOrNull(),
            avgHeadRoll = rolls.averageOrNull(),
            openSampleCount = minOf(openLeft.size, openRight.size),
            closedSampleCount = minOf(closedLeft.size, closedRight.size),
            framesSeen = framesSeen,
            framesRejected = framesRejected,
            targetPoseDescription = targetPoseDescription,
            targetRangeLabel = targetRangeLabel,
            achievedAvgYaw = yaws.averageOrNull(),
            achievedAvgRoll = rolls.averageOrNull(),
            achievedFaceCenterOffsetY = if (avgCenterY != null && baselineFaceCenterY != null) {
                avgCenterY - baselineFaceCenterY
            } else {
                null
            },
            achievedFaceWidthPercent = faceWidths.averageOrNull(),
            timeToReachTargetMs = timeToReachTargetMs,
            stableInTargetPercent = stableInTargetPercent,
            samplesRejectedPoseMismatch = samplesRejectedPoseMismatch,
            measurementRepeated = measurementRepeated,
            voiceGuidanceEventCount = voiceGuidanceEventCount,
            conditionValid = conditionValid
        )
    }

    fun pickBest(positions: List<PositionSignalStats>): SignalInvestigationReportPick {
        val withSepL = positions.filter { it.leftSeparation != null }
        val withSepR = positions.filter { it.rightSeparation != null }
        val withJitter = positions.filter { it.leftJitter != null && it.rightJitter != null }
        val bestL = withSepL.maxByOrNull { it.leftSeparation!! }?.position
        val bestR = withSepR.maxByOrNull { it.rightSeparation!! }?.position
        val lowestJ = withJitter.minByOrNull {
            ((it.leftJitter ?: 1f) + (it.rightJitter ?: 1f)) / 2f
        }?.position
        val lowestRej = positions.minByOrNull { it.rejectedPercent }?.position
        val bestOverall = positions.maxByOrNull { score(it) }?.position
        return SignalInvestigationReportPick(bestL, bestR, lowestJ, lowestRej, bestOverall)
    }

    fun score(s: PositionSignalStats): Float {
        val sep = ((s.leftSeparation ?: 0f) + (s.rightSeparation ?: 0f)) / 2f
        val jitter = ((s.leftJitter ?: 0.2f) + (s.rightJitter ?: 0.2f)) / 2f
        return sep * 2f - jitter * 3f - s.rejectedPercent / 100f - s.nullPercent / 100f
    }

    fun engineeringFindings(
        baseline: PositionSignalStats?,
        positions: List<PositionSignalStats>
    ): List<String> {
        val out = mutableListOf<String>()
        val base = baseline ?: return out
        val baseL = base.leftSeparation
        val baseR = base.rightSeparation
        for (p in positions) {
            if (p.stableInTargetPercent > 0f) {
                out += "${p.position.displayName} remained in the requested range for " +
                    "${"%.0f".format(p.stableInTargetPercent)}% of recording."
            }
            if (!p.conditionValid) {
                out += "${p.position.displayName} could not be maintained reliably " +
                    "(pose-invalid or insufficient valid samples)."
            } else if (p.measurementRepeated) {
                out += "${p.position.displayName} required a repeated measurement."
            }
            if (p.samplesRejectedPoseMismatch > 0) {
                out += "${p.position.displayName}: ${p.samplesRejectedPoseMismatch} samples " +
                    "rejected for pose mismatch."
            }
            if (p.position == SignalPosition.HeadStraight) continue
            val l = p.leftSeparation
            val r = p.rightSeparation
            if (baseL != null && baseL > 0.01f && l != null) {
                val pct = ((l - baseL) / abs(baseL)) * 100f
                if (abs(pct) >= 10f) {
                    out += if (pct > 0f) {
                        "Left-eye separation improved by ${"%.0f".format(pct)}% under ${p.position.displayName}."
                    } else {
                        "Left-eye separation decreased by ${"%.0f".format(-pct)}% under ${p.position.displayName}."
                    }
                }
            }
            if (baseR != null && baseR > 0.01f && r != null) {
                val pct = ((r - baseR) / abs(baseR)) * 100f
                if (abs(pct) >= 10f) {
                    out += if (pct > 0f) {
                        "Right-eye separation improved by ${"%.0f".format(pct)}% under ${p.position.displayName}."
                    } else {
                        "Right-eye separation decreased by ${"%.0f".format(-pct)}% under ${p.position.displayName}."
                    }
                }
            }
            if (p.conditionValid) {
                out += "${p.position.displayName} result is based on valid pose-controlled samples."
            }
        }
        val best = positions.maxByOrNull { score(it) }
        if (best != null) {
            out += "Best overall signal score measured at ${best.position.displayName}."
        }
        return out.distinct()
    }

    fun recommendations(
        positions: List<PositionSignalStats>,
        bestOverall: SignalPosition?,
        glasses: GlassesCondition
    ): List<String> {
        val out = mutableListOf<String>()
        bestOverall?.let {
            out += "Prefer ${it.displayName} for strongest measured open/closed separation and stability."
        }
        val highReject = positions.filter { it.rejectedPercent > 25f }
        if (highReject.isNotEmpty()) {
            out += "Avoid ${highReject.joinToString { it.position.displayName }} — high rejected-frame rate."
        }
        val lowSep = positions.filter {
            (it.leftSeparation ?: 0f) < 0.10f || (it.rightSeparation ?: 0f) < 0.10f
        }
        if (lowSep.isNotEmpty()) {
            out += "Poor open/closed separation at: " +
                lowSep.joinToString { it.position.displayName } + "."
        }
        val highNull = positions.filter { it.nullPercent > 10f }
        if (highNull.isNotEmpty()) {
            out += "Excessive null probabilities at: " +
                highNull.joinToString { it.position.displayName } + "."
        }
        if (glasses == GlassesCondition.YES) {
            val anyLow = positions.any { (it.leftSeparation ?: 0f) < 0.12f }
            if (anyLow) {
                out += "With glasses, measured separation is sensitive to head/phone angle — use best-condition pose."
            }
        }
        val smallFace = positions.mapNotNull { it.avgFaceWidthPercent }.minOrNull()
        if (smallFace != null && smallFace < 22f) {
            out += "Face width often small (min ${"%.1f".format(smallFace)}%) — " +
                "a caregiver may improve this by adjusting phone distance."
        }
        return out.distinct()
    }

    fun formatFullText(report: SignalInvestigationReport): String = buildString {
        appendLine(HEADER_TITLE)
        appendLine()
        appendLine("Session ID")
        appendLine(report.sessionId)
        appendLine("Investigation Mode")
        appendLine(
            when (report.investigationMode) {
                SignalInvestigationMode.Standard -> "STANDARD INVESTIGATION"
                SignalInvestigationMode.AdvancedEngineering -> "ADVANCED ENGINEERING INVESTIGATION"
            }
        )
        appendLine("Test Started")
        appendLine(formatLocal(report.testStartedMs))
        appendLine("Test Completed")
        appendLine(formatLocal(report.testCompletedMs))
        appendLine("Report Generated")
        appendLine(formatLocal(report.reportGeneratedMs))
        appendLine()
        appendLine("Device")
        appendLine("${report.deviceManufacturer} ${report.deviceModel}".trim())
        appendLine("Android Version")
        appendLine(report.androidVersion)
        appendLine("App Version")
        appendLine("${report.appVersionName} (${report.versionCode})")
        appendLine("Debug Build")
        appendLine(report.isDebugBuild.toString())
        appendLine("TTS Available")
        appendLine(report.ttsAvailable.toString())
        appendLine("Voice guidance events")
        appendLine(report.voiceGuidanceEventsTotal.toString())
        appendLine()
        appendLine("========================================")
        appendLine("TEST CONDITIONS")
        appendLine("========================================")
        appendLine()
        appendLine("Glasses:")
        appendLine(report.glasses.name)
        appendLine()
        appendLine("Lighting:")
        appendLine(report.lighting.name)
        appendLine()
        appendLine("Distance:")
        appendLine(report.distanceLabel)
        appendLine()
        appendLine("Face Width %")
        appendLine(report.faceWidthPercent?.let { "%.1f".format(it) } ?: "n/a")
        appendLine()
        appendLine("Camera Resolution")
        appendLine(report.cameraResolution)
        appendLine()

        if (report.investigationMode == SignalInvestigationMode.Standard ||
            report.standardSteps.isNotEmpty()
        ) {
            appendLine("========================================")
            appendLine("STANDARD INVESTIGATION")
            appendLine("========================================")
            appendLine()
            appendLine("Purpose: Can LISA reliably detect this person's eyes in natural position?")
            appendLine()
            for (s in report.standardSteps) {
                appendLine(s.step.displayName)
                appendLine(
                    when {
                        s.skipped -> "Skipped"
                        s.completed -> "Completed"
                        else -> "Incomplete"
                    }
                )
                appendLine("Open Left/Right avg: ${fmt(s.openLeftAvg)} / ${fmt(s.openRightAvg)}")
                appendLine("Closed Left/Right avg: ${fmt(s.closedLeftAvg)} / ${fmt(s.closedRightAvg)}")
                appendLine("Left/Right separation: ${fmt(s.leftSeparation)} / ${fmt(s.rightSeparation)}")
                appendLine("Samples: ${s.sampleCount}")
                appendLine("Null %: ${"%.1f".format(s.nullPercent)}")
                if (s.notes.isNotBlank()) appendLine("Notes: ${s.notes}")
                appendLine()
            }
            appendLine("Natural blink count observed")
            appendLine(report.naturalBlinkCount.toString())
            appendLine("L1 R1 outcome")
            appendLine(report.l1r1Outcome)
            appendLine("L2 R2 outcome")
            appendLine(report.l2r2Outcome)
            appendLine("Sequences attempted")
            appendLine(report.sequencesAttempted.toString())
            appendLine()
        }

        if (report.investigationMode == SignalInvestigationMode.AdvancedEngineering ||
            report.positions.any { it.position != SignalPosition.HeadStraight && it.openSampleCount > 0 }
        ) {
            appendLine("========================================")
            appendLine("ADVANCED ENGINEERING TESTS")
            appendLine("========================================")
            appendLine()
            appendLine("These tests are optional engineering measurements.")
            appendLine("They are not part of the standard patient assessment.")
            appendLine()
            appendLine("BASELINE")
            appendLine()
            appendPositionBlock(report.baseline)
            appendLine()
            for (p in report.positions) {
                appendLine(p.position.displayName)
                appendLine()
                appendPositionBlock(p)
                appendLine()
                appendLine("...")
                appendLine()
            }
            appendLine("========================================")
            appendLine("SIGNAL QUALITY (ADVANCED)")
            appendLine("========================================")
            appendLine()
            for (p in report.positions) {
                appendLine("${p.position.displayName}")
                appendLine("Left eye separation: ${fmt(p.leftSeparation)}")
                appendLine("Right eye separation: ${fmt(p.rightSeparation)}")
                appendLine("Left jitter: ${fmt(p.leftJitter)}")
                appendLine("Right jitter: ${fmt(p.rightJitter)}")
                appendLine("Null %: ${"%.1f".format(p.nullPercent)}")
                appendLine("Rejected %: ${"%.1f".format(p.rejectedPercent)}")
                appendLine()
            }
            appendLine("========================================")
            appendLine("BEST CONDITIONS (ADVANCED)")
            appendLine("========================================")
            appendLine()
            appendLine("Highest left-eye separation")
            appendLine(report.bestLeftSeparation?.displayName ?: "n/a")
            appendLine()
            appendLine("Highest right-eye separation")
            appendLine(report.bestRightSeparation?.displayName ?: "n/a")
            appendLine()
            appendLine("Lowest jitter")
            appendLine(report.lowestJitter?.displayName ?: "n/a")
            appendLine()
            appendLine("Lowest rejected frames")
            appendLine(report.lowestRejected?.displayName ?: "n/a")
            appendLine()
            appendLine("Best overall signal")
            appendLine(report.bestOverall?.displayName ?: "n/a")
            appendLine()
        } else if (report.investigationMode == SignalInvestigationMode.Standard) {
            appendLine("========================================")
            appendLine("ADVANCED ENGINEERING TESTS")
            appendLine("========================================")
            appendLine()
            appendLine("Not run. Advanced Engineering Investigation was not started.")
            appendLine()
        }

        appendLine("========================================")
        appendLine("ENVIRONMENT DIAGNOSES")
        appendLine("========================================")
        appendLine()
        if (report.environmentDiagnoses.isEmpty()) {
            appendLine("No environment concerns flagged from measured evidence.")
        } else {
            report.environmentDiagnoses.forEach { appendLine("• $it") }
        }
        appendLine()
        appendLine("========================================")
        appendLine("ENGINEERING FINDINGS")
        appendLine("========================================")
        appendLine()
        if (report.engineeringFindings.isEmpty()) {
            appendLine("No comparative findings from measured positions.")
        } else {
            report.engineeringFindings.forEach { appendLine("• $it") }
        }
        appendLine()
        appendLine("========================================")
        appendLine("RECOMMENDATIONS")
        appendLine("========================================")
        appendLine()
        appendLine("Generated ONLY from measured evidence.")
        appendLine("Directed to caregivers where movement of equipment may help.")
        appendLine()
        if (report.recommendations.isEmpty()) {
            appendLine("None.")
        } else {
            report.recommendations.forEach { appendLine("• $it") }
        }
        appendLine()
        appendLine("========================================")
        appendLine("END OF REPORT")
        appendLine("========================================")
    }

        private fun StringBuilder.appendPositionBlock(stats: PositionSignalStats?) {
        if (stats == null) {
            appendLine("(no data)")
            return
        }
        appendLine(stats.position.displayName)
        appendLine()
        appendLine("Open Left: ${fmt(stats.openLeftAvg)} (min ${fmt(stats.openLeftMin)}, max ${fmt(stats.openLeftMax)})")
        appendLine("Open Right: ${fmt(stats.openRightAvg)} (min ${fmt(stats.openRightMin)}, max ${fmt(stats.openRightMax)})")
        appendLine()
        appendLine("Closed Left: ${fmt(stats.closedLeftAvg)} (min ${fmt(stats.closedLeftMin)}, max ${fmt(stats.closedLeftMax)})")
        appendLine("Closed Right: ${fmt(stats.closedRightAvg)} (min ${fmt(stats.closedRightMin)}, max ${fmt(stats.closedRightMax)})")
        appendLine()
        appendLine("Frame Acceptance: ${"%.1f".format(stats.frameAcceptancePercent)}%")
        appendLine()
        appendLine("Rejected Frames: ${stats.framesRejected} (${"%.1f".format(stats.rejectedPercent)}%)")
        appendLine("Face width %: ${fmt(stats.avgFaceWidthPercent)}")
        appendLine("Head yaw/roll: ${fmt(stats.avgHeadYaw)} / ${fmt(stats.avgHeadRoll)}")
        appendLine("Target pose: ${stats.targetPoseDescription.ifBlank { "n/a" }}")
        appendLine("Target range: ${stats.targetRangeLabel.ifBlank { "n/a" }}")
        appendLine("Achieved avg yaw/roll: ${fmt(stats.achievedAvgYaw)} / ${fmt(stats.achievedAvgRoll)}")
        appendLine("Achieved face-centre offset Y: ${fmt(stats.achievedFaceCenterOffsetY)}")
        appendLine("Achieved face width %: ${fmt(stats.achievedFaceWidthPercent)}")
        appendLine("Time to reach target ms: ${stats.timeToReachTargetMs}")
        appendLine("Stable-in-target %: ${"%.1f".format(stats.stableInTargetPercent)}")
        appendLine("Samples rejected pose mismatch: ${stats.samplesRejectedPoseMismatch}")
        appendLine("Measurement repeated: ${stats.measurementRepeated}")
        appendLine("Voice guidance events: ${stats.voiceGuidanceEventCount}")
        appendLine("Condition valid: ${stats.conditionValid}")
    }

    private fun fmt(v: Float?): String = v?.let { "%.3f".format(it) } ?: "n/a"

    private fun List<Float>.averageOrNull(): Float? =
        if (isEmpty()) null else average().toFloat()
}

data class SignalInvestigationReportPick(
    val bestLeftSeparation: SignalPosition?,
    val bestRightSeparation: SignalPosition?,
    val lowestJitter: SignalPosition?,
    val lowestRejected: SignalPosition?,
    val bestOverall: SignalPosition?
)
