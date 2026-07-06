package com.idworx.lisa.features.communicationanalytics.engine

import com.idworx.lisa.features.communicationanalytics.model.AnalyticsSession
import com.idworx.lisa.features.communicationanalytics.model.CommunicationAnalyticsReport
import com.idworx.lisa.features.communicationanalytics.model.CommunicationAttemptAnalytics
import com.idworx.lisa.features.corecommunicationreliability.model.SpokenPhraseRecord

interface CommunicationAnalyticsEngine {
    fun startSession(practiceMode: Boolean = false, trainingMode: Boolean = false): AnalyticsSession
    fun endSession(sessionId: String)
    fun recordAttempt(attempt: CommunicationAttemptAnalytics)
    fun recordSpeechDelivery(attemptId: String, record: SpokenPhraseRecord, speechCompleteMs: Long? = null)
    fun attempts(): List<CommunicationAttemptAnalytics>
    fun currentSession(): AnalyticsSession?
    fun generateReport(): CommunicationAnalyticsReport
    fun lastReport(): CommunicationAnalyticsReport?
}
