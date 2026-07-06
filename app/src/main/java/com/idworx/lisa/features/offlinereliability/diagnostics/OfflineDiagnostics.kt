package com.idworx.lisa.features.offlinereliability.diagnostics

import com.idworx.lisa.features.offlinereliability.model.OfflineReliabilityReport

object OfflineLogger {

    private val log = mutableListOf<String>()

    fun log(message: String) {
        log.add("${System.currentTimeMillis()}: $message")
        while (log.size > 200) log.removeAt(0)
    }

    fun recent(limit: Int = 50): List<String> = log.takeLast(limit)

    fun clear() = log.clear()
}

object OfflineDiagnostics {

    private var lastReport: OfflineReliabilityReport? = null

    fun record(report: OfflineReliabilityReport) {
        lastReport = report
        OfflineLogger.log(
            "Offline audit ${report.reportId}: score=${report.score.overall} ready=${report.score.metrics.capabilitiesReady}/${report.score.metrics.capabilitiesTotal}"
        )
    }

    fun lastReport(): OfflineReliabilityReport? = lastReport

    fun formatSummary(): String {
        val report = lastReport ?: return "No offline diagnostics available"
        return buildString {
            appendLine("Offline Reliability Diagnostics")
            appendLine("Score: ${report.score.overall} (${report.score.band.name})")
            appendLine("Brain 1 offline ready: ${report.brain1OfflineReady}")
            appendLine("Capabilities ready: ${report.score.metrics.capabilitiesReady}/${report.score.metrics.capabilitiesTotal}")
            appendLine("Checks: ${report.score.metrics.checksPassed}/${report.score.metrics.totalChecks}")
            appendLine("Warnings: ${report.warnings.size}")
            appendLine("Critical dependencies: ${report.detectedDependencies.size}")
            report.detectedDependencies.take(5).forEach { appendLine("Dependency: $it") }
            appendLine("Eye tracking: ${report.capabilities[com.idworx.lisa.features.offlinereliability.model.OfflineCapability.EyeTracking]}")
            appendLine("Communication: ${report.capabilities[com.idworx.lisa.features.offlinereliability.model.OfflineCapability.PhraseMatching]}")
            appendLine("Guided Learning: ${report.capabilities[com.idworx.lisa.features.offlinereliability.model.OfflineCapability.GuidedLearning]}")
            appendLine("Emergency: ${report.capabilities[com.idworx.lisa.features.offlinereliability.model.OfflineCapability.EmergencyCommunication]}")
            appendLine("TTS: ${report.capabilities[com.idworx.lisa.features.offlinereliability.model.OfflineCapability.TextToSpeech]}")
            report.recommendations.take(3).forEach { appendLine("Recommendation: ${it.message}") }
        }
    }

    fun clear() {
        lastReport = null
        OfflineLogger.clear()
    }
}
