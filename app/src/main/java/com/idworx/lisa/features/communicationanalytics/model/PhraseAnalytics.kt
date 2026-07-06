package com.idworx.lisa.features.communicationanalytics.model

data class PhraseAnalytics(
    val phraseId: String?,
    val phraseText: String?,
    val attemptCount: Int,
    val successCount: Int,
    val blockedCount: Int,
    val retryCount: Int,
    val successRate: Float,
    val averageCompletionTimeMs: Long?,
    val fastestCompletionMs: Long?,
    val slowestCompletionMs: Long?,
    val evidence: String
)

data class CalibrationImpact(
    val attemptsByHealthState: Map<String, Int>,
    val successRateByCalibrationScoreBand: Map<String, Float>,
    val retryRateByCalibrationQuality: Map<String, Float>,
    val blockedByCalibrationState: Int,
    val communicationAfterRecalibration: Int,
    val evidence: String
)

data class EmergencyAnalytics(
    val activationAttempts: Int,
    val trainingActivations: Int,
    val blockedActivations: Int,
    val duplicatePreventions: Int,
    val confirmationSuccesses: Int,
    val falsePositiveSignals: Int,
    val falseNegativeSignals: Int,
    val safetyInterventions: Int,
    val evidence: String
)

data class NavigationAnalytics(
    val gestureAttempts: Int,
    val successfulGestures: Int,
    val retries: Int,
    val conflicts: Int,
    val falsePositiveSignals: Int,
    val averageCompletionTimeMs: Long?,
    val evidence: String
)
