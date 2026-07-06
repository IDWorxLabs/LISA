package com.idworx.lisa.features.communicationanalytics.model

data class SuccessRate(
    val rate: Float,
    val successful: Int,
    val total: Int,
    val evidence: String
)

enum class TrendDirection {
    Improving,
    Stable,
    Declining,
    InsufficientData
}

data class CommunicationTrend(
    val metricName: String,
    val direction: TrendDirection,
    val currentValue: Float,
    val priorValue: Float?,
    val evidence: String
)

data class CommunicationMetric(
    val name: String,
    val value: Float,
    val unit: String? = null,
    val evidence: String
)

data class AnalyticsSummary(
    val totalAttempts: Int,
    val successfulCommunications: Int,
    val blockedAttempts: Int,
    val failedSpeech: Int,
    val overallSuccessRate: SuccessRate,
    val falsePositiveRate: Float,
    val falseNegativeRate: Float,
    val averageRetries: Float,
    val averageCompletionTimeMs: Long?,
    val evidence: String
)
