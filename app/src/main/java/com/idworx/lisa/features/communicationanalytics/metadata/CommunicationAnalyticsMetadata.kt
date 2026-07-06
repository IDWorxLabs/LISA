package com.idworx.lisa.features.communicationanalytics.metadata

object CommunicationAnalyticsMetadata {
    const val FEATURE_NAME: String = "LISA Communication Accuracy Analytics"
    const val VERSION: String = "V1"
    const val MAX_STORED_ATTEMPTS: Int = 500
    const val MAX_STORED_SESSIONS: Int = 50
    const val TREND_MIN_SAMPLES: Int = 5
    const val DUPLICATE_WINDOW_MS: Long = 900L
    const val LOW_CONFIDENCE_THRESHOLD: Float = 0.65f
}
