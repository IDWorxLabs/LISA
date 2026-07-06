package com.idworx.lisa.features.corecommunicationreliability.metadata

object CoreCommunicationReliabilityMetadata {
    const val FEATURE_NAME: String = "LISA Core Communication Reliability"
    const val VERSION: String = "V1"
    const val CONFIDENCE_THRESHOLD_BLOCK: Float = 0.45f
    const val CONFIDENCE_THRESHOLD_CONFIRM: Float = 0.65f
    const val CONFIDENCE_THRESHOLD_IMMEDIATE: Float = 0.85f
    const val DEFAULT_DEBOUNCE_MS: Long = 900L
    const val MAX_HISTORY_ENTRIES: Int = 100
}
