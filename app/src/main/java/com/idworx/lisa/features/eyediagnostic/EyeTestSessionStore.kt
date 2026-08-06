package com.idworx.lisa.features.eyediagnostic

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Local debug-only session persistence. Stores CSV numeric/state rows only — never images or PII.
 */
class EyeTestSessionStore(
    private val rootDir: File,
    private val clockMs: () -> Long = { System.currentTimeMillis() }
) {
    companion object {
        const val RELATIVE_DIR = "debug/eye_test"
        private const val CSV_HEADER =
            "timestampMs,faceDetected,faceCount,bboxW,bboxH,faceWidthPercent," +
                "leftProb,rightProb,smoothedLeftProb,smoothedRightProb," +
                "leftNull,rightNull,eitherNull," +
                "yawY,rollZ,sensitivity,leftClosedThr,rightClosedThr,openThr," +
                "leftPrevState,rightPrevState,leftCandidateState,rightCandidateState," +
                "leftState,rightState," +
                "leftDecisionReason,rightDecisionReason,frameDecisionReason," +
                "leftSupport,rightSupport,requiredSupport," +
                "leftTransitionAccepted,rightTransitionAccepted," +
                "leftWinkCandidateActive,rightWinkCandidateActive," +
                "candidateCancellationReason,cooldownActiveLeft,cooldownActiveRight," +
                "msSinceLeftTransition,msSinceRightTransition," +
                "frameAccepted,rejectionReason," +
                "leftWinks,rightWinks,sequenceState"

        /** Legacy header prefix for compatibility checks with older session files. */
        const val LEGACY_CSV_HEADER_PREFIX =
            "timestampMs,faceDetected,faceCount,bboxW,bboxH,faceWidthPercent,leftProb,rightProb"

        fun defaultRoot(filesDir: File): File = File(filesDir, RELATIVE_DIR)
    }

    init {
        if (!rootDir.exists()) rootDir.mkdirs()
    }

    fun directory(): File = rootDir

    fun buildTimestampedFileName(
        kind: EyeTestSessionKind,
        atMs: Long = clockMs(),
        shortSessionId: String? = null
    ): String {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date(atMs))
        val sid = shortSessionId?.takeIf { it.isNotBlank() }?.let { "_$it" } ?: ""
        return "lisa_eye_test_${kind.fileToken}_${stamp}$sid.csv"
    }

    fun buildCombinedReportFileName(
        atMs: Long = clockMs(),
        shortSessionId: String
    ): String {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date(atMs))
        return "lisa_eye_test_${stamp}_${shortSessionId}.txt"
    }

    fun saveSession(
        kind: EyeTestSessionKind,
        samples: List<LisaEyeDiagnostic.Sample>,
        shortSessionId: String? = null,
        atMs: Long = clockMs()
    ): File {
        if (!rootDir.exists()) rootDir.mkdirs()
        val file = File(rootDir, buildTimestampedFileName(kind, atMs, shortSessionId))
        file.writeText(toCsv(samples))
        return file
    }

    fun saveCombinedReport(text: String, shortSessionId: String, atMs: Long = clockMs()): File {
        if (!rootDir.exists()) rootDir.mkdirs()
        val file = File(rootDir, buildCombinedReportFileName(atMs, shortSessionId))
        file.writeText(text)
        return file
    }

    fun toCsv(samples: List<LisaEyeDiagnostic.Sample>): String = buildString {
        appendLine(CSV_HEADER)
        samples.forEach { s ->
            append(s.timestampMs).append(',')
            append(s.faceDetected).append(',')
            append(s.faceCount).append(',')
            append(s.boundingBoxWidthPx ?: "").append(',')
            append(s.boundingBoxHeightPx ?: "").append(',')
            append(s.faceWidthPercentOfImage?.let { "%.2f".format(it) } ?: "").append(',')
            append(s.leftEyeOpenProbability?.let { "%.4f".format(it) } ?: "").append(',')
            append(s.rightEyeOpenProbability?.let { "%.4f".format(it) } ?: "").append(',')
            append(s.smoothedLeftProbability?.let { "%.4f".format(it) } ?: "").append(',')
            append(s.smoothedRightProbability?.let { "%.4f".format(it) } ?: "").append(',')
            append(s.leftEyeOpenProbability == null).append(',')
            append(s.rightEyeOpenProbability == null).append(',')
            append(s.eitherProbabilityNull).append(',')
            append(s.headEulerAngleY?.let { "%.2f".format(it) } ?: "").append(',')
            append(s.headEulerAngleZ?.let { "%.2f".format(it) } ?: "").append(',')
            append(s.sensitivityLevel).append(',')
            append("%.4f".format(s.leftEyeClosedThreshold)).append(',')
            append("%.4f".format(s.rightEyeClosedThreshold)).append(',')
            append("%.4f".format(s.openEyeThreshold)).append(',')
            append(s.leftPreviousState?.name ?: "").append(',')
            append(s.rightPreviousState?.name ?: "").append(',')
            append(s.leftCandidateState?.name ?: "").append(',')
            append(s.rightCandidateState?.name ?: "").append(',')
            append(s.interpretedLeftEyeState.name).append(',')
            append(s.interpretedRightEyeState.name).append(',')
            append(escapeCsv(s.leftDecisionReason ?: "")).append(',')
            append(escapeCsv(s.rightDecisionReason ?: "")).append(',')
            append(escapeCsv(s.frameDecisionReason ?: "")).append(',')
            append(s.leftConsecutiveSupport).append(',')
            append(s.rightConsecutiveSupport).append(',')
            append(s.requiredSupportCount).append(',')
            append(s.leftTransitionAccepted).append(',')
            append(s.rightTransitionAccepted).append(',')
            append(s.leftWinkCandidateActive).append(',')
            append(s.rightWinkCandidateActive).append(',')
            append(escapeCsv(s.candidateCancellationReason ?: "")).append(',')
            append(s.cooldownActiveLeft).append(',')
            append(s.cooldownActiveRight).append(',')
            append(s.msSinceLeftTransition ?: "").append(',')
            append(s.msSinceRightTransition ?: "").append(',')
            append(s.frameAccepted).append(',')
            append(escapeCsv(s.rejectionReason ?: "")).append(',')
            append(s.leftWinkCount).append(',')
            append(s.rightWinkCount).append(',')
            append(escapeCsv(s.sequenceState))
            appendLine()
        }
    }

    fun containsImageOrPiiMarkers(csv: String): Boolean {
        val lower = csv.lowercase(Locale.US)
        return lower.contains("data:image") ||
            lower.contains("base64") ||
            lower.contains(".jpg") ||
            lower.contains(".png") ||
            lower.contains("@") ||
            lower.contains("password") ||
            lower.contains("keystore")
    }

    private fun escapeCsv(value: String): String {
        if (value.contains(',') || value.contains('"') || value.contains('\n')) {
            return "\"" + value.replace("\"", "\"\"") + "\""
        }
        return value
    }
}
