package com.idworx.lisa.features.calibrationreliability.integration

import com.idworx.lisa.features.calibrationreliability.engine.CalibrationReliabilityEngine
import com.idworx.lisa.features.calibrationreliability.model.CalibrationHealthState
import com.idworx.lisa.features.calibrationreliability.model.CalibrationRecommendation
import com.idworx.lisa.features.calibrationreliability.model.CalibrationSessionSource
import com.idworx.lisa.features.experiencepolish.caregiverconfidence.CaregiverConfidenceEngine
import com.idworx.lisa.features.personality.dialogue.DefaultDialogueCatalog
import com.idworx.lisa.features.personality.engine.LisaPersonalityEngine
import com.idworx.lisa.features.personality.model.AppFeature
import com.idworx.lisa.features.personality.model.DialogueCategory
import com.idworx.lisa.features.personality.model.DialogueContext

object CalibrationPersonalityAdapter {

    fun guidanceForHealth(
        personality: LisaPersonalityEngine,
        health: CalibrationHealthState,
        recommendations: List<CalibrationRecommendation>
    ): String {
        val context = DialogueContext(
            feature = AppFeature.Settings,
            consecutiveFailures = if (health == CalibrationHealthState.CalibrationInvalid) 3 else 0
        )
        val dialogue = when (health) {
            CalibrationHealthState.Healthy -> selectDialogue("recal_ok", context)
            CalibrationHealthState.Monitor -> selectDialogue("recal_monitor", context)
            CalibrationHealthState.RecommendRecalibration -> selectDialogue("recal_retry", context)
            CalibrationHealthState.CalibrationRequired -> selectDialogue("recal_required", context)
            CalibrationHealthState.CalibrationInvalid -> selectDialogue("recal_invalid", context)
        }
        val recommendationHint = recommendationHint(recommendations)
        return if (recommendationHint != null) "${dialogue.text} $recommendationHint" else dialogue.text
    }

    fun encouragementDuringCalibration(personality: LisaPersonalityEngine): String =
        selectDialogue("recal_encourage", DialogueContext(feature = AppFeature.Settings)).text

    fun milestoneCelebration(personality: LisaPersonalityEngine): String =
        personality.generateEncouragement(DialogueContext(feature = AppFeature.Settings)).text

    private fun selectDialogue(preferredId: String, context: DialogueContext) =
        DefaultDialogueCatalog.forCategory(DialogueCategory.RecalibrationGuidance, context.locale)
            .firstOrNull { it.id == preferredId }
            ?: DefaultDialogueCatalog.forCategory(DialogueCategory.RecalibrationGuidance, context.locale).first()

    private fun recommendationHint(recommendations: List<CalibrationRecommendation>): String? =
        recommendations.firstNotNullOfOrNull { CaregiverConfidenceEngine.recommendationHint(it) }
}

object ExistingSensitivityCalibrationAdapter {

    /**
     * Wraps existing blink sensitivity adjustment as observable calibration evidence.
     * Does not replace eye tracking or calibration UI — records evidence only.
     */
    fun recordSensitivityEvidence(engine: CalibrationReliabilityEngine, sensitivityLevel: Int) {
        val session = engine.startSession(
            totalPoints = 1,
            sensitivityLevel = sensitivityLevel,
            source = CalibrationSessionSource.SensitivityAdjustment
        )
        repeat(3) { engine.recordSuccessfulSample(session) }
        engine.recordPointCompleted(session)
        engine.completeSession(session)
    }
}
