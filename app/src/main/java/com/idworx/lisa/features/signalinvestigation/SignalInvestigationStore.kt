package com.idworx.lisa.features.signalinvestigation

import java.io.File

/**
 * Debug-only storage for Signal Investigation reports.
 * Never writes production settings.
 */
class SignalInvestigationStore(
    private val rootDir: File
) {
    companion object {
        const val RELATIVE_DIR = "debug/signal_investigation"

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
        val stamp = SignalInvestigationReportAuthority.formatFileStamp(reportGeneratedMs)
        val shortId = sessionId.replace("-", "").take(8)
        val file = File(reportsDir(), "signal_report_${stamp}_$shortId.txt")
        file.writeText(fullText)
        return file
    }
}
