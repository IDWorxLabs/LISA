package com.idworx.lisa.features.brain1readiness.engine

import com.idworx.lisa.features.brain1readiness.diagnostics.Brain1ReadinessDiagnostics
import com.idworx.lisa.features.brain1readiness.model.Brain1ReadinessReport
import com.idworx.lisa.features.brain1readiness.model.Brain1ReadinessScore
import com.idworx.lisa.features.brain1readiness.reporting.Brain1ReadinessReportGenerator
import com.idworx.lisa.features.brain1readiness.reviewers.Brain1ReadinessReviewRunner

class DefaultBrain1ReadinessEngine : Brain1ReadinessEngine {

    private var lastReportInternal: Brain1ReadinessReport? = null

    override fun runReview(): Brain1ReadinessReport {
        val report = Brain1ReadinessReportGenerator.generate(Brain1ReadinessReviewRunner.runAllReviewers())
        lastReportInternal = report
        Brain1ReadinessDiagnostics.record(report)
        return report
    }

    override fun generateReport(): Brain1ReadinessReport = runReview()

    override fun lastReport(): Brain1ReadinessReport? = lastReportInternal

    override fun lastScore(): Brain1ReadinessScore? = lastReportInternal?.score
}

object Brain1ReadinessEngines {
    @Volatile
    private var instance: DefaultBrain1ReadinessEngine? = null

    val default: Brain1ReadinessEngine
        get() = instance ?: DefaultBrain1ReadinessEngine().also { instance = it }

    fun createForTests(): DefaultBrain1ReadinessEngine {
        val engine = DefaultBrain1ReadinessEngine()
        instance = engine
        return engine
    }

    fun resetForTests() {
        instance = null
        Brain1ReadinessDiagnostics.clear()
        try {
            com.idworx.lisa.features.androiddevicetesting.protocol.AndroidDeviceTestingProtocols.resetForTests()
        } catch (_: Exception) {
            // Device testing module optional during partial builds
        }
    }
}
