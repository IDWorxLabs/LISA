package com.idworx.lisa.features.offlinereliability.engine

import com.idworx.lisa.features.offlinereliability.model.OfflineReliabilityReport

interface OfflineReliabilityEngine {
    fun runValidation(): OfflineReliabilityReport
    fun generateReport(): OfflineReliabilityReport
    fun lastReport(): OfflineReliabilityReport?
    fun lastScore(): com.idworx.lisa.features.offlinereliability.model.OfflineReliabilityScore?
}
