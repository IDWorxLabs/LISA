package com.idworx.lisa.features.communicationanalytics.engine

import com.idworx.lisa.features.communicationanalytics.diagnostics.AnalyticsDiagnostics
import com.idworx.lisa.features.communicationanalytics.metadata.CommunicationAnalyticsMetadata
import com.idworx.lisa.features.communicationanalytics.model.AnalyticsSession
import com.idworx.lisa.features.communicationanalytics.model.CommunicationAnalyticsReport
import com.idworx.lisa.features.communicationanalytics.model.CommunicationAttemptAnalytics
import com.idworx.lisa.features.communicationanalytics.reporting.CommunicationReportGenerator
import com.idworx.lisa.features.corecommunicationreliability.model.SpokenPhraseRecord
import java.util.UUID

object AnalyticsAggregator {

    fun aggregate(attempts: List<CommunicationAttemptAnalytics>): CommunicationAnalyticsReport =
        CommunicationReportGenerator.generate(attempts)

    fun trimAttempts(attempts: MutableList<CommunicationAttemptAnalytics>) {
        while (attempts.size > CommunicationAnalyticsMetadata.MAX_STORED_ATTEMPTS) {
            attempts.removeAt(0)
        }
    }
}

class DefaultCommunicationAnalyticsEngine : CommunicationAnalyticsEngine {

    private val attemptsInternal = mutableListOf<CommunicationAttemptAnalytics>()
    private val sessions = mutableListOf<AnalyticsSession>()
    private var currentSessionInternal: AnalyticsSession? = null
    private var lastReportInternal: CommunicationAnalyticsReport? = null
    private val phraseRetryTracker = mutableMapOf<String, Int>()

    override fun startSession(practiceMode: Boolean, trainingMode: Boolean): AnalyticsSession {
        val session = AnalyticsSession(
            sessionId = UUID.randomUUID().toString(),
            startTimeMs = System.currentTimeMillis(),
            practiceMode = practiceMode,
            trainingMode = trainingMode
        )
        currentSessionInternal = session
        sessions.add(session)
        while (sessions.size > CommunicationAnalyticsMetadata.MAX_STORED_SESSIONS) {
            sessions.removeAt(0)
        }
        return session
    }

    override fun endSession(sessionId: String) {
        val session = sessions.find { it.sessionId == sessionId } ?: return
        session.endTimeMs = System.currentTimeMillis()
        if (currentSessionInternal?.sessionId == sessionId) {
            currentSessionInternal = null
        }
    }

    override fun recordAttempt(attempt: CommunicationAttemptAnalytics) {
        val phraseKey = attempt.phraseId ?: attempt.sequenceLabel
        val retries = phraseRetryTracker.getOrDefault(phraseKey, 0)
        phraseRetryTracker[phraseKey] = retries + 1
        val enriched = attempt.copy(retryCount = retries)
        attemptsInternal.add(enriched)
        currentSessionInternal?.attemptIds?.add(attempt.attemptId)
        AnalyticsAggregator.trimAttempts(attemptsInternal)
        refreshReport()
    }

    override fun recordSpeechDelivery(
        attemptId: String,
        record: SpokenPhraseRecord,
        speechCompleteMs: Long?
    ) {
        val index = attemptsInternal.indexOfLast { it.attemptId == attemptId }
        if (index >= 0) {
            val existing = attemptsInternal[index]
            attemptsInternal[index] = existing.copy(
                speechSuccess = record.speechSuccess,
                timing = existing.timing.copy(
                    speechCompleteMs = speechCompleteMs ?: record.timestampMs,
                    speechRequestMs = existing.timing.speechRequestMs ?: record.timestampMs
                )
            )
            refreshReport()
        }
    }

    override fun attempts(): List<CommunicationAttemptAnalytics> = attemptsInternal.toList()

    override fun currentSession(): AnalyticsSession? = currentSessionInternal

    override fun generateReport(): CommunicationAnalyticsReport {
        refreshReport()
        return lastReportInternal!!
    }

    override fun lastReport(): CommunicationAnalyticsReport? = lastReportInternal

    internal fun resetForTests() {
        attemptsInternal.clear()
        sessions.clear()
        currentSessionInternal = null
        lastReportInternal = null
        phraseRetryTracker.clear()
        AnalyticsDiagnostics.clear()
    }

    private fun refreshReport() {
        if (attemptsInternal.isEmpty()) return
        lastReportInternal = AnalyticsAggregator.aggregate(attemptsInternal)
        AnalyticsDiagnostics.record(lastReportInternal!!, attemptsInternal.toList())
    }
}

object CommunicationAnalyticsEngines {
    @Volatile
    private var instance: DefaultCommunicationAnalyticsEngine? = null

    val default: CommunicationAnalyticsEngine
        get() = instance ?: DefaultCommunicationAnalyticsEngine().also { instance = it }

    fun createForTests(): DefaultCommunicationAnalyticsEngine {
        val engine = DefaultCommunicationAnalyticsEngine()
        instance = engine
        return engine
    }

    fun resetForTests() {
        instance?.resetForTests()
        instance = null
    }
}
