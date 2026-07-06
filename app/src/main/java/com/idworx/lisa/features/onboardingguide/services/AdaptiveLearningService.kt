package com.idworx.lisa.features.onboardingguide.services

import com.idworx.lisa.features.calibrationreliability.engine.CalibrationReliabilityEngine
import com.idworx.lisa.features.calibrationreliability.model.CalibrationHealthState
import com.idworx.lisa.features.experiencepolish.patientcommunicationcoach.PatientCommunicationCoachEngine
import com.idworx.lisa.features.onboardingguide.model.TrainingProgress

data class AdaptiveLearningOffer(
    val showHelp: Boolean,
    val showPracticeAgain: Boolean,
    val showPatiencePrompt: Boolean,
    val showReduceSensitivity: Boolean,
    val showRecalibrate: Boolean,
    val instructionalPauseMultiplier: Float,
    val showRepeatPhrase: Boolean = false,
    val caregiverCoachHint: String? = null,
    val pacingDelayMs: Long = 1_200L
)

object AdaptiveLearningService {

    private const val STRUGGLE_THRESHOLD = 3
    private const val HEAVY_STRUGGLE_THRESHOLD = 5

    fun evaluate(
        progress: TrainingProgress,
        calibrationEngine: CalibrationReliabilityEngine? = null
    ): AdaptiveLearningOffer {
        val failures = progress.statistics.consecutiveFailures
        val calibrationHealth = calibrationEngine?.currentHealth()
        val calibrationStruggling = calibrationHealth == CalibrationHealthState.CalibrationInvalid ||
            calibrationHealth == CalibrationHealthState.RecommendRecalibration
        val base = AdaptiveLearningOffer(
            showHelp = failures >= STRUGGLE_THRESHOLD || calibrationStruggling,
            showPracticeAgain = failures >= STRUGGLE_THRESHOLD,
            showPatiencePrompt = failures >= STRUGGLE_THRESHOLD,
            showReduceSensitivity = failures >= HEAVY_STRUGGLE_THRESHOLD,
            showRecalibrate = failures >= HEAVY_STRUGGLE_THRESHOLD || calibrationStruggling,
            instructionalPauseMultiplier = when {
                failures >= HEAVY_STRUGGLE_THRESHOLD || calibrationStruggling -> 1.8f
                failures >= STRUGGLE_THRESHOLD -> 1.4f
                else -> 1.0f
            }
        )
        return PatientCommunicationCoachEngine.enrichAdaptiveOffer(progress, base)
    }

    fun isRecalibrationAvailable(calibrationEngine: CalibrationReliabilityEngine? = null): Boolean =
        calibrationEngine != null
}
