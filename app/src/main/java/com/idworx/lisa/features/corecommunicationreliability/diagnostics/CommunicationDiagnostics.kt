package com.idworx.lisa.features.corecommunicationreliability.diagnostics

import com.idworx.lisa.features.corecommunicationreliability.history.CommunicationReliabilityHistoryRecorder
import com.idworx.lisa.features.corecommunicationreliability.model.CommunicationReliabilityReport
import com.idworx.lisa.features.corecommunicationreliability.model.ReliabilityMetric

object CommunicationReliabilityLogger {

    private val log = mutableListOf<String>()

    fun log(message: String) {
        log.add("${System.currentTimeMillis()}: $message")
        while (log.size > 200) log.removeAt(0)
    }

    fun recent(limit: Int = 50): List<String> = log.takeLast(limit)

    fun clear() = log.clear()
}

object CommunicationDiagnostics {

    private var lastReport: CommunicationReliabilityReport? = null
    private var lastBlockedReason: String? = null
    private var emergencySafetyStatus: String = "idle"
    private val attemptObservers = mutableListOf<(CommunicationReliabilityReport, AnalyticsObservationContext?) -> Unit>()
    private var pendingObservationContext: AnalyticsObservationContext? = null

    data class AnalyticsObservationContext(
        val mode: com.idworx.lisa.features.corecommunicationreliability.model.CommunicationMode =
            com.idworx.lisa.features.corecommunicationreliability.model.CommunicationMode.MAIN,
        val calibrationHealth: com.idworx.lisa.features.calibrationreliability.model.CalibrationHealthState? = null,
        val navigationTraining: Boolean = false,
        val communicationTraining: Boolean = false,
        val practiceMode: Boolean = false,
        val sequenceStartMs: Long? = null
    )

    fun setPendingObservationContext(context: AnalyticsObservationContext?) {
        pendingObservationContext = context
    }

    fun addAttemptObserver(observer: (CommunicationReliabilityReport, AnalyticsObservationContext?) -> Unit) {
        attemptObservers.add(observer)
    }

    fun clearAttemptObservers() {
        attemptObservers.clear()
    }

    fun recordAttempt(report: CommunicationReliabilityReport) {
        lastReport = report
        if (report.finalOutcome == com.idworx.lisa.features.corecommunicationreliability.model.CommunicationReliabilityOutcome.BLOCKED) {
            lastBlockedReason = report.failureReason ?: report.attemptResult.blockedReason
        }
        CommunicationReliabilityLogger.log(
            "Attempt ${report.attemptId}: ${report.normalizedSequence} → ${report.finalOutcome}"
        )
        val context = pendingObservationContext
        attemptObservers.forEach { it(report, context) }
    }

    fun recordEmergencyStatus(status: String) {
        emergencySafetyStatus = status
        CommunicationReliabilityLogger.log("Emergency: $status")
    }

    fun lastAttempt(): CommunicationReliabilityReport? = lastReport

    fun lastBlockedReason(): String? = lastBlockedReason

    fun lastMatchedPhrase(): String? = lastReport?.matchedPhraseText

    fun lastSequence(): String? = lastReport?.normalizedSequence

    fun recentMetrics(): List<ReliabilityMetric> = lastReport?.metrics ?: emptyList()

    fun emergencyStatus(): String = emergencySafetyStatus

    fun lastSpeechResult(): String {
        val history = CommunicationReliabilityHistoryRecorder.last()
        return when {
            history == null -> "none"
            history.speechSuccess -> "success: ${history.phraseText}"
            else -> "failed"
        }
    }

    fun snapshot(): DiagnosticsSnapshot = DiagnosticsSnapshot(
        lastSequence = lastSequence(),
        lastMatchedPhrase = lastMatchedPhrase(),
        lastBlockedReason = lastBlockedReason(),
        lastSpeechResult = lastSpeechResult(),
        emergencyStatus = emergencySafetyStatus,
        recentMetrics = recentMetrics(),
        historyCount = CommunicationReliabilityHistoryRecorder.count()
    )

    data class DiagnosticsSnapshot(
        val lastSequence: String?,
        val lastMatchedPhrase: String?,
        val lastBlockedReason: String?,
        val lastSpeechResult: String,
        val emergencyStatus: String,
        val recentMetrics: List<ReliabilityMetric>,
        val historyCount: Int
    )
}
