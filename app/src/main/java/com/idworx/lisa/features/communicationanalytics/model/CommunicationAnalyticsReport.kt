package com.idworx.lisa.features.communicationanalytics.model

data class CommunicationAnalyticsReport(
    val reportId: String,
    val generatedAtMs: Long,
    val summary: AnalyticsSummary,
    val phraseAnalytics: List<PhraseAnalytics>,
    val calibrationImpact: CalibrationImpact,
    val emergencyAnalytics: EmergencyAnalytics,
    val navigationAnalytics: NavigationAnalytics,
    val trends: List<CommunicationTrend>,
    val recommendations: List<String>,
    val warnings: List<String>,
    val metrics: List<CommunicationMetric>,
    val evidenceSummary: String
)
