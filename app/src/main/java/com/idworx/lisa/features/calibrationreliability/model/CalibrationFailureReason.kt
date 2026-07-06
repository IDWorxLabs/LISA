package com.idworx.lisa.features.calibrationreliability.model

enum class CalibrationFailureReason {
    IncompleteSamples,
    InterruptedSession,
    InsufficientStability,
    LowRepeatability,
    ExcessiveRejects,
    MissingCoverage,
    TrackingDiscontinuity,
    UserCancelled,
    Timeout,
    NoObservableEvidence
}
