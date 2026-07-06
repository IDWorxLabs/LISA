package com.idworx.lisa.features.androiddevicetesting.protocol

import com.idworx.lisa.features.androiddevicetesting.metadata.AndroidDeviceTestingMetadata
import com.idworx.lisa.features.androiddevicetesting.model.DeviceTestPlan
import com.idworx.lisa.features.androiddevicetesting.suites.DeviceTestSuiteRegistry
import java.util.UUID

object DeviceTestPlanBuilder {

    fun build(): DeviceTestPlan = DeviceTestPlan(
        planId = UUID.randomUUID().toString(),
        protocolVersion = AndroidDeviceTestingMetadata.PROTOCOL_VERSION,
        suites = DeviceTestSuiteRegistry.allSuites(),
        createdAtMs = System.currentTimeMillis()
    )

    fun totalSteps(plan: DeviceTestPlan): Int =
        plan.suites.sumOf { suite -> suite.cases.sumOf { it.steps.size } }
}
