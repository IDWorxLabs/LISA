package com.idworx.lisa.features.personalisedeyeprofile

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

/**
 * Engineering diagnostic report for the debug-only Personalised Eye Profile prototype.
 * Pure formatting / recommendation logic — does not alter thresholds or validation rules.
 */
data class EyeCalibrationReportSection(
    val eyeLabel: String,
    val openAverage: Float? = null,
    val openMinimum: Float? = null,
    val openMaximum: Float? = null,
    val closedAverage: Float? = null,
    val closedMinimum: Float? = null,
    val closedMaximum: Float? = null,
    val derivedClosedThreshold: Float? = null,
    val derivedOpenThreshold: Float? = null,
    val closedMisclassificationPercent: Float? = null,
    val openMisclassificationPercent: Float? = null,
    val separation: Float? = null,
    val passed: Boolean = false,
    val failureReasons: List<String> = emptyList(),
    val openSampleCount: Int = 0,
    val closedSampleCount: Int = 0
)

data class ValidationStageReport(
    val name: String,
    val passed: Boolean,
    val detail: String = ""
)

data class PersonalisedEyeProfileReport(
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
    val profileId: String?,
    val profileStatus: PersonalisedEyeProfileStatus?,
    val calibrationPassed: Boolean,
    val leftEye: EyeCalibrationReportSection,
    val rightEye: EyeCalibrationReportSection,
    val validationStages: List<ValidationStageReport>,
    val validationRunNumber: Int?,
    val validationPassed: Boolean?,
    val failureSummary: List<String>,
    val overallConfidence: String,
    val potentialCause: String?,
    val recommendations: List<String>,
    val comparisonSnippet: String = "",
    val calibrationDiagnostics: Map<String, String> = emptyMap()
)

object PersonalisedEyeProfileReportAuthority {
    const val HEADER_TITLE = "LISA PERSONALISED EYE PROFILE REPORT"

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

    fun eyeSectionFromDerivation(
        eyeLabel: String,
        openSamples: List<Float>,
        closedSamples: List<Float>,
        derivation: PersonalisedThresholdDerivation.EyeDerivationResult?
    ): EyeCalibrationReportSection {
        val openStats = PersonalisedThresholdDerivation.stats(openSamples.filter { it in 0f..1f })
        val closedStats = PersonalisedThresholdDerivation.stats(closedSamples.filter { it in 0f..1f })
        val closedThr = derivation?.closedThreshold?.takeIf { it > 0f }
        val openThr = derivation?.openThreshold?.takeIf { it > 0f }
        val closedMis = if (closedThr != null && closedSamples.isNotEmpty()) {
            closedSamples.count { it >= closedThr }.toFloat() / closedSamples.size * 100f
        } else {
            null
        }
        val openMis = if (openThr != null && openSamples.isNotEmpty()) {
            openSamples.count { it <= openThr }.toFloat() / openSamples.size * 100f
        } else {
            null
        }
        val passed = derivation?.ok == true
        return EyeCalibrationReportSection(
            eyeLabel = eyeLabel,
            openAverage = openStats.average ?: derivation?.openStats?.average,
            openMinimum = openStats.min ?: derivation?.openStats?.min,
            openMaximum = openStats.max ?: derivation?.openStats?.max,
            closedAverage = closedStats.average ?: derivation?.closedStats?.average,
            closedMinimum = closedStats.min ?: derivation?.closedStats?.min,
            closedMaximum = closedStats.max ?: derivation?.closedStats?.max,
            derivedClosedThreshold = closedThr ?: derivation?.closedThreshold?.takeIf { derivation.ok },
            derivedOpenThreshold = openThr ?: derivation?.openThreshold?.takeIf { derivation.ok },
            closedMisclassificationPercent = closedMis,
            openMisclassificationPercent = openMis,
            separation = derivation?.separation,
            passed = passed,
            failureReasons = derivation?.failureReasons.orEmpty(),
            openSampleCount = openSamples.size,
            closedSampleCount = closedSamples.size
        )
    }

    /**
     * Factual recommendations only — derived from measured counters / stats.
     * Never invents causes that are not supported by the data.
     */
    fun recommendations(
        left: EyeCalibrationReportSection,
        right: EyeCalibrationReportSection,
        diagnostics: Map<String, String>,
        validation: PersonalisedValidationRunResult?,
        calibrationPassed: Boolean
    ): List<String> {
        val out = mutableListOf<String>()
        val faceWidth = diagnostics["minFaceWidthPercent"]?.toFloatOrNull()
        if (faceWidth != null && faceWidth < 22f) {
            out += "Face too small (min face width ${"%.1f".format(faceWidth)}% of frame)."
        }
        val rejectedPct = diagnostics["rejectedFramePercent"]?.toFloatOrNull()
        if (rejectedPct != null && rejectedPct > 20f) {
            out += "Too many rejected frames (${"%.1f".format(rejectedPct)}%)."
        }
        val nullPct = diagnostics["nullProbabilityPercent"]?.toFloatOrNull()
            ?: validation?.nullProbabilityPercent
        if (nullPct != null && nullPct > 10f) {
            out += "Excessive null probabilities (${"%.1f".format(nullPct)}%)."
        }
        val leftOpenStd = diagnostics["leftOpenStdDev"]?.toFloatOrNull()
        val rightOpenStd = diagnostics["rightOpenStdDev"]?.toFloatOrNull()
        if ((leftOpenStd != null && leftOpenStd > 0.08f) ||
            (rightOpenStd != null && rightOpenStd > 0.08f)
        ) {
            out += "High probability jitter during open baseline " +
                "(L σ=${leftOpenStd?.let { "%.3f".format(it) } ?: "n/a"}, " +
                "R σ=${rightOpenStd?.let { "%.3f".format(it) } ?: "n/a"})."
        }
        val lightingProxy = diagnostics["openRangeLeft"]?.toFloatOrNull()
        val lightingProxyR = diagnostics["openRangeRight"]?.toFloatOrNull()
        if ((lightingProxy != null && lightingProxy > 0.25f) ||
            (lightingProxyR != null && lightingProxyR > 0.25f)
        ) {
            out += "Lighting too inconsistent (open-eye probability range large)."
        }
        fun overlapHint(section: EyeCalibrationReportSection) {
            val sep = section.separation
            if (sep != null && sep < PersonalisedThresholdDerivation.MIN_SEPARATION) {
                out += "${section.eyeLabel}: closed samples overlap open samples " +
                    "(separation=${"%.3f".format(sep)} < " +
                    "${PersonalisedThresholdDerivation.MIN_SEPARATION})."
            }
            val mis = section.closedMisclassificationPercent
            if (mis != null && mis > PersonalisedThresholdDerivation.MAX_MISCLASSIFY * 100f) {
                out += "${section.eyeLabel}: poor eye separation — " +
                    "${"%.0f".format(mis)}% of closed samples remain above closed threshold."
            }
        }
        overlapHint(left)
        overlapHint(right)
        if (!calibrationPassed && out.none { it.contains("overlap") || it.contains("separation") }) {
            val reasons = left.failureReasons + right.failureReasons
            if (reasons.any { it.contains("unstable", ignoreCase = true) }) {
                out += "Opposite eye was unstable during single-eye close holds."
            }
            if (reasons.any { it.contains("few", ignoreCase = true) }) {
                out += "Insufficient usable calibration samples."
            }
        }
        if (validation != null) {
            if (validation.falsePositiveWinks > 0) {
                out += "False wink detections during steady-open validation " +
                    "(${validation.falsePositiveWinks})."
            }
            if (!validation.l1r1Success) out += "L1 R1 sequence was not completed successfully."
            if (!validation.l2r2Success) out += "L2 R2 sequence was not completed successfully."
            if (validation.uncertainOccupancyPercent >
                PersonalisedEyeProfileValidation.MAX_UNCERTAIN_PERCENT
            ) {
                out += "Uncertain-band occupancy too high " +
                    "(${"%.1f".format(validation.uncertainOccupancyPercent)}%)."
            }
        }
        return out.distinct()
    }

    fun potentialCause(
        left: EyeCalibrationReportSection,
        right: EyeCalibrationReportSection,
        calibrationPassed: Boolean,
        validation: PersonalisedValidationRunResult?
    ): String? {
        if (!calibrationPassed) {
            val sepL = left.separation
            val sepR = right.separation
            if ((sepL != null && sepL < PersonalisedThresholdDerivation.MIN_SEPARATION) ||
                (sepR != null && sepR < PersonalisedThresholdDerivation.MIN_SEPARATION)
            ) {
                return "Insufficient separation between open and closed probability distributions."
            }
            val misL = left.closedMisclassificationPercent
            val misR = right.closedMisclassificationPercent
            if ((misL != null && misL > 20f) || (misR != null && misR > 20f)) {
                return "Closed-eye probabilities remain too high relative to derived closed thresholds."
            }
            val reasons = left.failureReasons + right.failureReasons
            if (reasons.isNotEmpty()) return reasons.first()
            return "Calibration did not produce a usable per-eye threshold profile."
        }
        if (validation != null && !validation.passed) {
            return validation.failureReasons.firstOrNull()
                ?: "Validation run did not meet all required stage criteria."
        }
        return null
    }

    fun overallConfidence(
        calibrationPassed: Boolean,
        validationPassed: Boolean?,
        profileStatus: PersonalisedEyeProfileStatus?
    ): String = when {
        profileStatus == PersonalisedEyeProfileStatus.Validated -> "VALIDATED"
        validationPassed == true &&
            profileStatus == PersonalisedEyeProfileStatus.ValidationRun1Passed ->
            "VALIDATION RUN 1 PASSED (run 2 still required)"
        validationPassed == true -> "VALIDATION PASSED"
        !calibrationPassed -> "FAILED"
        validationPassed == false -> "FAILED"
        calibrationPassed && validationPassed == null -> "CALIBRATED — AWAITING VALIDATION"
        else -> "INCOMPLETE"
    }

    fun formatFullText(report: PersonalisedEyeProfileReport): String = buildString {
        appendLine(HEADER_TITLE)
        appendLine()
        appendLine("Session ID: ${report.sessionId}")
        appendLine("Test Started: ${formatLocal(report.testStartedMs)}")
        appendLine("Test Completed: ${formatLocal(report.testCompletedMs)}")
        appendLine("Report Generated: ${formatLocal(report.reportGeneratedMs)}")
        appendLine("Phone model: ${report.deviceManufacturer} ${report.deviceModel}".trim())
        appendLine("Android version: ${report.androidVersion}")
        appendLine("App Version: ${report.appVersionName} (${report.versionCode})")
        appendLine("Debug Build: ${report.isDebugBuild}")
        appendLine("Profile ID: ${report.profileId ?: "n/a"}")
        appendLine("Profile Status: ${report.profileStatus ?: "n/a"}")
        appendLine()
        appendLine("======== CALIBRATION RESULTS ========")
        appendLine("Calibration outcome: ${if (report.calibrationPassed) "PASS" else "FAIL"}")
        appendEye(report.leftEye)
        appendEye(report.rightEye)
        if (report.calibrationDiagnostics.isNotEmpty()) {
            appendLine()
            appendLine("--- Calibration diagnostics ---")
            report.calibrationDiagnostics.forEach { (k, v) -> appendLine("$k: $v") }
        }
        appendLine()
        appendLine("======== VALIDATION RESULTS ========")
        if (report.validationRunNumber == null && report.validationStages.isEmpty()) {
            appendLine("Validation: NOT RUN (calibration did not produce a candidate profile).")
        } else {
            appendLine("Validation run: ${report.validationRunNumber ?: "n/a"}")
            appendLine(
                "Validation outcome: " + when (report.validationPassed) {
                    true -> "PASS"
                    false -> "FAIL"
                    null -> "n/a"
                }
            )
            report.validationStages.forEach { stage ->
                appendLine(
                    "${stage.name}: ${if (stage.passed) "PASS" else "FAIL"}" +
                        if (stage.detail.isNotBlank()) " — ${stage.detail}" else ""
                )
            }
        }
        appendLine()
        appendLine("======== FAILURE ANALYSIS ========")
        appendLine("Overall confidence: ${report.overallConfidence}")
        if (report.failureSummary.isEmpty()) {
            appendLine("No failure summary (workflow completed without recorded failures).")
        } else {
            appendLine("Failure Summary")
            report.failureSummary.forEach { appendLine("• $it") }
        }
        report.potentialCause?.let {
            appendLine()
            appendLine("Potential cause")
            appendLine(it)
        }
        appendLine()
        appendLine("======== RECOMMENDATIONS ========")
        if (report.recommendations.isEmpty()) {
            appendLine("None — measured diagnostics did not trigger factual recommendations.")
        } else {
            report.recommendations.forEach { appendLine("• $it") }
        }
        if (report.comparisonSnippet.isNotBlank()) {
            appendLine()
            appendLine("======== COMPARISON SNAPSHOT ========")
            appendLine(report.comparisonSnippet.trimEnd())
        }
        appendLine()
        appendLine("======== END OF REPORT ========")
    }

    private fun StringBuilder.appendEye(eye: EyeCalibrationReportSection) {
        appendLine()
        appendLine("--- ${eye.eyeLabel} ---")
        appendLine("OPEN average: ${fmt(eye.openAverage)}")
        appendLine("OPEN minimum: ${fmt(eye.openMinimum)}")
        appendLine("OPEN maximum: ${fmt(eye.openMaximum)}")
        appendLine("CLOSED average: ${fmt(eye.closedAverage)}")
        appendLine("CLOSED minimum: ${fmt(eye.closedMinimum)}")
        appendLine("CLOSED maximum: ${fmt(eye.closedMaximum)}")
        appendLine("Derived closed threshold: ${fmt(eye.derivedClosedThreshold)}")
        appendLine("Derived open threshold: ${fmt(eye.derivedOpenThreshold)}")
        appendLine(
            "Misclassification % (closed above thr): " +
                (eye.closedMisclassificationPercent?.let { "%.1f".format(it) } ?: "n/a")
        )
        appendLine(
            "Misclassification % (open at/below thr): " +
                (eye.openMisclassificationPercent?.let { "%.1f".format(it) } ?: "n/a")
        )
        appendLine("Separation: ${fmt(eye.separation)}")
        appendLine("Samples open/closed: ${eye.openSampleCount}/${eye.closedSampleCount}")
        appendLine("PASS / FAIL: ${if (eye.passed) "PASS" else "FAIL"}")
        eye.failureReasons.forEach { appendLine("  reason: $it") }
    }

    private fun fmt(v: Float?): String = v?.let { "%.3f".format(it) } ?: "n/a"
}
