package com.idworx.lisa.features.calibrationreliability.model

data class CalibrationMetric(
    val name: String,
    val value: Float,
    val unit: String? = null,
    val evidence: String? = null
)
