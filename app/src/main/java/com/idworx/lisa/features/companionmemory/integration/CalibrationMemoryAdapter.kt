package com.idworx.lisa.features.companionmemory.integration

import com.idworx.lisa.features.calibrationreliability.model.CalibrationQualityCategory
import com.idworx.lisa.features.calibrationreliability.model.CalibrationResult
import com.idworx.lisa.features.companionmemory.engine.CompanionMemoryEngine
import com.idworx.lisa.features.companionmemory.model.LearningMilestone
import com.idworx.lisa.features.companionmemory.model.MemoryCategory
import com.idworx.lisa.features.companionmemory.model.MemoryEvent
import com.idworx.lisa.features.companionmemory.model.MemoryImportance
import com.idworx.lisa.features.companionmemory.model.PreferenceMemorySnapshot

object CalibrationMemoryAdapter {

    fun onCalibrationCompleted(
        engine: CompanionMemoryEngine,
        result: CalibrationResult,
        priorOverallScore: Int?
    ) {
        val score = result.score.overall
        val category = result.score.category

        if (category != CalibrationQualityCategory.Failed &&
            LearningMilestone.FirstSuccessfulCalibration !in engine.getMilestones()
        ) {
            engine.recordMilestone(
                LearningMilestone.FirstSuccessfulCalibration,
                "First calibration completed with score $score (${category.name})"
            )
        }

        if (priorOverallScore != null && score > priorOverallScore + 10) {
            engine.recordMilestone(
                LearningMilestone.CalibrationImproved,
                "Calibration improved from $priorOverallScore to $score"
            )
        }

        if (result.session.retries > 0 && result.score.overall >= 60) {
            engine.recordMilestone(
                LearningMilestone.RecalibrationCompleted,
                "Recalibration completed with score $score after ${result.session.retries} retries"
            )
        }

        engine.recordPreference(
            PreferenceMemorySnapshot(
                calibrationCompleted = result.score.overall >= 60,
                blinkSensitivity = result.session.sensitivityLevel
            )
        )

        if (result.score.overall >= 75) {
            engine.recordEvent(
                MemoryEvent(
                    "calibration_completed",
                    evidence = result.evidenceSummary
                ),
                MemoryCategory.Calibration,
                MemoryImportance.Medium,
                "Calibration completed",
                result.evidenceSummary
            )
        }
    }
}
