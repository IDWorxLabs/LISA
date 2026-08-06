package com.idworx.lisa.features.glassescharacterisation

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

object GlassesCharacterisationReportAuthority {
    const val HEADER_TITLE = "LISA GLASSES CHARACTERISATION REPORT"

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

    fun formatFullText(report: GlassesCharacterisationReport): String = buildString {
        appendLine(HEADER_TITLE)
        appendLine()
        appendLine("Session ID")
        appendLine(report.sessionId)
        appendLine("Test Started")
        appendLine(formatLocal(report.testStartedMs))
        appendLine("Test Completed")
        appendLine(formatLocal(report.testCompletedMs))
        appendLine("Report Generated")
        appendLine(formatLocal(report.reportGeneratedMs))
        appendLine("Device")
        appendLine("${report.deviceManufacturer} ${report.deviceModel}".trim())
        appendLine("Android")
        appendLine(report.androidVersion)
        appendLine("App Version")
        appendLine("${report.appVersionName} (${report.versionCode})")
        appendLine("Debug Build")
        appendLine(report.isDebugBuild.toString())
        appendLine("Glasses: YES")
        appendLine("Incomplete")
        appendLine(report.incomplete.toString())
        appendLine()

        appendLine("========================================")
        appendLine("SESSION SUMMARY")
        appendLine("========================================")
        appendLine()
        appendLine("Lighting sequence:")
        appendLine("Normal")
        appendLine("↓")
        appendLine("Brighter")
        appendLine("↓")
        appendLine("Dimmer")
        appendLine()
        appendLine("All completed within one controlled experiment.")
        appendLine()
        appendLine("Position consistency maintained:")
        appendLine(if (report.positionConsistencyMaintained) "YES" else "NO")
        appendLine("Phone movement warnings:")
        appendLine(report.phoneMovementWarningCount.toString())
        appendLine("Patient movement warnings:")
        appendLine(report.patientMovementWarningCount.toString())
        appendLine()

        appendLine("========================================")
        appendLine("1. TEST SETUP")
        appendLine("========================================")
        appendLine()
        appendSetup(report.setup)
        appendLine()

        appendLine("========================================")
        appendLine("2. POSITION CONSISTENCY")
        appendLine("========================================")
        appendLine()
        appendLine("Baseline face centre X/Y %")
        appendLine(
            "${fmt(report.setup.pose.faceCenterXPct)} / ${fmt(report.setup.pose.faceCenterYPct)}"
        )
        appendLine("Baseline face width %")
        appendLine(fmt(report.setup.pose.faceWidthPct))
        appendLine("Baseline yaw / roll")
        appendLine("${fmt(report.setup.pose.yaw)} / ${fmt(report.setup.pose.roll)}")
        for (c in report.conditions) {
            appendLine("${c.condition.displayName}: position warnings=${c.positionWarningCount}, " +
                "invalid pose samples=${c.invalidPoseSampleCount}")
        }
        appendLine()

        for ((idx, c) in report.conditions.withIndex()) {
            appendLine("========================================")
            appendLine("${3 + idx}. ${c.condition.displayName.uppercase()}")
            appendLine("========================================")
            appendLine()
            appendCondition(c)
            appendLine()
        }

        appendLine("========================================")
        appendLine("COMPARISON TABLE")
        appendLine("========================================")
        appendLine()
        appendLine(
            "Lighting".padEnd(16) +
                "Left Sep".padEnd(12) +
                "Right Sep".padEnd(12) +
                "Quality"
        )
        for (c in report.conditions) {
            appendLine(
                c.condition.displayName.padEnd(16) +
                    fmt(c.left.openP25MinusClosedP75).padEnd(12) +
                    fmt(c.right.openP25MinusClosedP75).padEnd(12) +
                    c.quality.name
            )
        }
        appendLine(
            "Best Overall".padEnd(16) +
                (report.bestOverallLighting?.displayName ?: "n/a")
        )
        appendLine()

        appendLine("========================================")
        appendLine("6. LEFT-EYE COMPARISON")
        appendLine("========================================")
        appendLine()
        for (c in report.conditions) {
            appendLine(c.condition.displayName)
            appendLine("Separation (P25-P75): ${fmt(c.left.openP25MinusClosedP75)}")
            appendLine("Overlap %: ${"%.1f".format(c.left.overlapPercent)}")
            appendLine("Closed below closed-thr %: ${"%.1f".format(c.left.closedBelowClosedThrPct)}")
            appendLine("Open above open-thr %: ${"%.1f".format(c.left.openAboveOpenThrPct)}")
            appendLine("Uncertain (closed) %: ${"%.1f".format(c.left.closedInUncertainPct)}")
            appendLine("Jitter: ${fmt(c.left.jitter)}")
            appendLine("Null %: ${"%.1f".format(c.left.nullPercent)}")
            appendLine("Rejected %: ${"%.1f".format(c.left.rejectedPercent)}")
            appendLine()
        }

        appendLine("========================================")
        appendLine("7. RIGHT-EYE COMPARISON")
        appendLine("========================================")
        appendLine()
        for (c in report.conditions) {
            appendLine(c.condition.displayName)
            appendLine("Separation (P25-P75): ${fmt(c.right.openP25MinusClosedP75)}")
            appendLine("Overlap %: ${"%.1f".format(c.right.overlapPercent)}")
            appendLine("Closed below closed-thr %: ${"%.1f".format(c.right.closedBelowClosedThrPct)}")
            appendLine("Open above open-thr %: ${"%.1f".format(c.right.openAboveOpenThrPct)}")
            appendLine("Uncertain (closed) %: ${"%.1f".format(c.right.closedInUncertainPct)}")
            appendLine("Jitter: ${fmt(c.right.jitter)}")
            appendLine("Null %: ${"%.1f".format(c.right.nullPercent)}")
            appendLine("Rejected %: ${"%.1f".format(c.right.rejectedPercent)}")
            appendLine()
        }

        appendLine("========================================")
        appendLine("8. NATURAL BLINK COMPARISON")
        appendLine("========================================")
        appendLine()
        for (c in report.conditions) {
            appendLine(c.condition.displayName)
            appendLine("Completed blinks: ${c.blink.completedBlinks}")
            appendLine("Candidates: ${c.blink.blinkCandidates}")
            appendLine("Cancelled: ${c.blink.cancelledCandidates}")
            appendLine("False wink candidates: ${c.blink.falseWinkCandidates}")
            appendLine("Both-eye closures: ${c.blink.bothEyeClosures}")
            appendLine("Uncertain-band entries: ${c.blink.uncertainBandEntries}")
            appendLine("Open instability: ${fmt(c.blink.openInstability)}")
            appendLine()
        }

        appendLine("========================================")
        appendLine("9. BEST LIGHTING CONDITIONS")
        appendLine("========================================")
        appendLine()
        appendLine("Best lighting for left eye")
        appendLine(report.bestLeftLighting?.displayName ?: "n/a")
        appendLine("Best lighting for right eye")
        appendLine(report.bestRightLighting?.displayName ?: "n/a")
        appendLine("Best overall lighting")
        appendLine(report.bestOverallLighting?.displayName ?: "n/a")
        appendLine("Improvement consistent across both eyes")
        appendLine(report.improvementConsistentBothEyes.toString())
        appendLine("Lighting produced no meaningful improvement")
        appendLine(report.lightingProducedNoMeaningfulImprovement.toString())
        appendLine()

        appendLine("========================================")
        appendLine("10. ENGINEERING FINDINGS")
        appendLine("========================================")
        appendLine()
        report.findings.forEach { appendLine("• $it") }
        appendLine()

        appendLine("========================================")
        appendLine("11. CAREGIVER RECOMMENDATIONS")
        appendLine("========================================")
        appendLine()
        report.caregiverRecommendations.forEach { appendLine("• $it") }
        appendLine()

        appendLine("========================================")
        appendLine("12. DECISION SUPPORT")
        appendLine("========================================")
        appendLine()
        appendLine("Category")
        appendLine(report.decision.name)
        appendLine()
        appendLine(report.decisionExplanation)
        appendLine()
        appendLine("This is engineering guidance only. It is not a production decision.")
        appendLine()

        appendLine("========================================")
        appendLine("13. LIMITATIONS")
        appendLine("========================================")
        appendLine()
        report.limitations.forEach { appendLine("• $it") }
        appendLine()

        appendLine("========================================")
        appendLine("14. END OF REPORT")
        appendLine("========================================")
    }

    private fun StringBuilder.appendSetup(s: SetupSnapshot) {
        appendLine("Camera resolution: ${s.cameraResolution}")
        appendLine("Device orientation: ${s.deviceOrientation}")
        appendLine("App sensitivity level: ${s.sensitivityLevel}")
        appendLine("Response time: ${s.responseTimeLabel}")
        appendLine("Screen brightness: ${fmt(s.screenBrightness)}")
        appendLine("Ambient lux: ${fmt(s.ambientLux)}")
        appendLine("Standard closed threshold (observational): ${fmt(s.standardClosedThreshold)}")
        appendLine("Standard open threshold (observational): ${fmt(s.standardOpenThreshold)}")
        appendLine("No images, video, face crops, or personal identifiers are stored.")
    }

    private fun StringBuilder.appendCondition(c: ConditionResult) {
        appendLine("Source label: ${c.sourceLabel.displayName}")
        appendLine("Completed: ${c.completed}")
        appendLine("Ended early: ${c.endedEarly}")
        appendLine("Quality: ${c.quality}")
        appendLine("Usable samples: ${c.usableSampleCount}")
        appendLine("Notes: ${c.notes.ifBlank { "n/a" }}")
        appendLine()
        appendLine("LEFT EYE")
        appendEye(c.left)
        appendLine()
        appendLine("RIGHT EYE")
        appendEye(c.right)
        appendLine()
        appendLine("NATURAL BLINK")
        appendLine("Completed: ${c.blink.completedBlinks}  Candidates: ${c.blink.blinkCandidates}")
    }

    private fun StringBuilder.appendEye(m: EyeSeparationMetrics) {
        appendLine("Open n=${m.open.count} avg=${fmt(m.open.average)} med=${fmt(m.open.median)} " +
            "sd=${fmt(m.open.stdDev)} min=${fmt(m.open.min)} max=${fmt(m.open.max)}")
        appendLine("Open P10/P25/P50/P75/P90: ${fmt(m.open.p10)}/${fmt(m.open.p25)}/" +
            "${fmt(m.open.p50)}/${fmt(m.open.p75)}/${fmt(m.open.p90)}")
        appendLine("Closed n=${m.closed.count} avg=${fmt(m.closed.average)} med=${fmt(m.closed.median)} " +
            "sd=${fmt(m.closed.stdDev)} min=${fmt(m.closed.min)} max=${fmt(m.closed.max)}")
        appendLine("Closed P10/P25/P50/P75/P90: ${fmt(m.closed.p10)}/${fmt(m.closed.p25)}/" +
            "${fmt(m.closed.p50)}/${fmt(m.closed.p75)}/${fmt(m.closed.p90)}")
        appendLine("Separation openP25-closedP75: ${fmt(m.openP25MinusClosedP75)}")
        appendLine("Separation median: ${fmt(m.openMedianMinusClosedMedian)}")
        appendLine("Overlap %: ${"%.1f".format(m.overlapPercent)}")
        appendLine("Closed < closedThr %: ${"%.1f".format(m.closedBelowClosedThrPct)}")
        appendLine("Closed in uncertain %: ${"%.1f".format(m.closedInUncertainPct)}")
        appendLine("Closed > openThr %: ${"%.1f".format(m.closedAboveOpenThrPct)}")
        appendLine("Open > openThr %: ${"%.1f".format(m.openAboveOpenThrPct)}")
        appendLine("Open in uncertain %: ${"%.1f".format(m.openInUncertainPct)}")
        appendLine("Null %: ${"%.1f".format(m.nullPercent)}")
        appendLine("Rejected %: ${"%.1f".format(m.rejectedPercent)}")
        appendLine("Jitter: ${fmt(m.jitter)}")
    }

    private fun fmt(v: Float?): String = v?.let { "%.3f".format(it) } ?: "n/a"
}

class GlassesCharacterisationStore(private val rootDir: File) {
    companion object {
        const val RELATIVE_DIR = "debug/glasses_characterisation"
        fun defaultRoot(filesDir: File): File = File(filesDir, RELATIVE_DIR)
    }

    init {
        if (!rootDir.exists()) rootDir.mkdirs()
    }

    fun reportsDir(): File {
        val dir = File(rootDir, "reports")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun saveReport(fullText: String, sessionId: String, reportGeneratedMs: Long): File {
        val stamp = GlassesCharacterisationReportAuthority.formatFileStamp(reportGeneratedMs)
        val shortId = sessionId.replace("-", "").take(8)
        val file = File(reportsDir(), "glasses_characterisation_${stamp}_$shortId.txt")
        file.writeText(fullText)
        return file
    }

    /** Privacy: only text reports under reports/; never images. */
    fun containsOnlyTextReports(): Boolean {
        val dir = reportsDir()
        val files = dir.listFiles() ?: return true
        return files.all { it.isFile && it.name.endsWith(".txt") }
    }
}
