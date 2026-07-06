package com.idworx.lisa.features.androiddevicetesting.integration

import com.idworx.lisa.features.androiddevicetesting.protocol.AndroidDeviceTestingProtocols
import com.idworx.lisa.features.androiddevicetesting.protocol.DeviceTestPlanBuilder

object Brain1DeviceTestingBridge {

    fun summarizeForReadiness(): String {
        val hasEvidence = AndroidDeviceTestingProtocols.default.hasRecordedDeviceEvidence()
        val plan = DeviceTestPlanBuilder.build()
        val totalSteps = DeviceTestPlanBuilder.totalSteps(plan)
        return if (hasEvidence) {
            "Real Android device testing evidence recorded via protocol V1"
        } else {
            "Android device testing protocol V1 available; $totalSteps steps await real hardware evidence"
        }
    }

    fun primaryGapDescription(): String =
        if (AndroidDeviceTestingProtocols.default.hasRecordedDeviceEvidence()) {
            "Additional device conditions and long-session testing may still be needed"
        } else {
            "Real Android device testing still needed"
        }
}
