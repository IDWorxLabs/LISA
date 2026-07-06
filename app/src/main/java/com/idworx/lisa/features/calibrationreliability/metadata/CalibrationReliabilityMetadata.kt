package com.idworx.lisa.features.calibrationreliability.metadata

object CalibrationReliabilityMetadata {
    const val FEATURE_NAME: String = "LISA Calibration Reliability"
    const val VERSION: String = "V1"
    const val SCHEMA_VERSION: Int = 1
    const val DEFAULT_CALIBRATION_POINTS: Int = 5
    const val MAX_STORED_SESSIONS: Int = 10
    const val HEALTHY_AGE_MS: Long = 7L * 24 * 60 * 60 * 1000
    const val MONITOR_AGE_MS: Long = 14L * 24 * 60 * 60 * 1000
    const val DRIFT_FAILURE_THRESHOLD: Int = 3
    const val MAX_RETRY_ATTEMPTS: Int = 3
    const val STABILITY_EVENT_PENALTY: Int = 4
    const val REJECT_SAMPLE_PENALTY: Int = 3
    const val TRACKING_GAP_PENALTY: Int = 8
    const val INTERRUPTION_PENALTY: Int = 6
    const val MIN_COMMUNICATION_SCORE: Int = 40
    const val MONITOR_SCORE_THRESHOLD: Int = 75
    const val PASS_SCORE_THRESHOLD: Int = 60
}
