package com.idworx.lisa.features.calibrationreliability.model

enum class CalibrationHealthState {
    Healthy,
    Monitor,
    RecommendRecalibration,
    CalibrationRequired,
    CalibrationInvalid
}

enum class CalibrationSessionState {
    NotStarted,
    InProgress,
    Paused,
    Completed,
    Interrupted,
    Abandoned
}

enum class CalibrationReliabilityOutcome {
    Pass,
    Marginal,
    Fail
}
