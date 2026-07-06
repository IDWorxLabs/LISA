package com.idworx.lisa.features.brain1readiness.engine

import com.idworx.lisa.features.brain1readiness.model.Brain1ReadinessReport
import com.idworx.lisa.features.brain1readiness.model.Brain1ReadinessScore

interface Brain1ReadinessEngine {
    fun runReview(): Brain1ReadinessReport
    fun generateReport(): Brain1ReadinessReport
    fun lastReport(): Brain1ReadinessReport?
    fun lastScore(): Brain1ReadinessScore?
}
