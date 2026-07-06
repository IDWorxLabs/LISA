package com.idworx.lisa.features.androiddevicetesting.integration

import com.idworx.lisa.features.androiddevicetesting.protocol.AndroidDeviceTestingProtocols
import com.idworx.lisa.features.androiddevicetesting.integration.Brain1DeviceTestingBridge

/**
 * Bridge for Brain 1 Readiness Review to detect real Android device testing evidence.
 */
object Brain1DeviceTestingIntegration {

    fun protocolExists(): Boolean = try {
        Class.forName("com.idworx.lisa.features.androiddevicetesting.protocol.AndroidDeviceTestingProtocol")
        true
    } catch (_: Exception) {
        false
    }

    fun hasRecordedDeviceEvidence(): Boolean =
        AndroidDeviceTestingProtocols.default.hasRecordedDeviceEvidence()

    fun deviceTestingGapStillApplies(): Boolean = !hasRecordedDeviceEvidence()

    fun readinessEvidenceSummary(): String =
        Brain1DeviceTestingBridge.summarizeForReadiness()
}
