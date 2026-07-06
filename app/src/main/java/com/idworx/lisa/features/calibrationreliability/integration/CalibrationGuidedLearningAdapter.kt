package com.idworx.lisa.features.calibrationreliability.integration

import com.idworx.lisa.features.calibrationreliability.engine.CalibrationReliabilityEngine
import com.idworx.lisa.features.calibrationreliability.model.CalibrationHealthState
import com.idworx.lisa.features.onboardingguide.model.TrainingProgress
import com.idworx.lisa.features.onboardingguide.services.AdaptiveLearningOffer
import com.idworx.lisa.features.onboardingguide.services.AdaptiveLearningService

data class CalibrationGuidedLearningDecision(
    val pauseLessons: Boolean,
    val recommendRecalibration: Boolean,
    val resumeAutomaticallyWhenHealthy: Boolean,
    val guidanceMessage: String
)

object CalibrationGuidedLearningAdapter {

    fun evaluate(
        engine: CalibrationReliabilityEngine,
        progress: TrainingProgress
    ): CalibrationGuidedLearningDecision {
        val health = engine.currentHealth()
        val adaptive = AdaptiveLearningService.evaluate(progress, engine)
        return CalibrationGuidedLearningDecision(
            pauseLessons = engine.shouldPauseGuidedLearning(),
            recommendRecalibration = adaptive.showRecalibrate || health == CalibrationHealthState.RecommendRecalibration,
            resumeAutomaticallyWhenHealthy = health == CalibrationHealthState.Healthy ||
                health == CalibrationHealthState.Monitor,
            guidanceMessage = engine.guidanceMessage()
        )
    }

    fun shouldBlockLesson(engine: CalibrationReliabilityEngine): Boolean =
        engine.shouldPauseGuidedLearning()
}

object CalibrationCommunicationReliabilityBridge {

    fun calibrationBlockReason(health: CalibrationHealthState): String? = when (health) {
        CalibrationHealthState.CalibrationInvalid ->
            "Communication paused — calibration quality is insufficient"
        CalibrationHealthState.CalibrationRequired ->
            "Communication paused — calibration is required before speaking"
        else -> null
    }

    fun shouldBlockCommunication(engine: CalibrationReliabilityEngine): Boolean =
        !engine.allowsCommunication()

    fun onCommunicationBlocked(engine: CalibrationReliabilityEngine) {
        engine.recordCommunicationFailure()
    }

    fun onLowConfidenceCommunication(engine: CalibrationReliabilityEngine) {
        engine.recordLowConfidenceCommunication()
    }
}
