package com.idworx.lisa.features.experiencepolish.caregiverconfidence

import com.idworx.lisa.features.calibrationreliability.model.CalibrationHealthState
import com.idworx.lisa.features.calibrationreliability.model.CalibrationRecommendation
import com.idworx.lisa.features.experiencepolish.caregiverconfidence.model.CaregiverSupportUiState
import com.idworx.lisa.features.onboardingguide.model.TrainingPhase
import com.idworx.lisa.features.personality.dialogue.DefaultDialogueCatalog
import com.idworx.lisa.features.personality.engine.LisaPersonalityEngine
import com.idworx.lisa.features.personality.engine.LisaPersonalityEngines
import com.idworx.lisa.features.personality.model.AppFeature
import com.idworx.lisa.features.personality.model.CaregiverSupportMoment
import com.idworx.lisa.features.personality.model.DialogueContext

/**
 * Plain-language caregiver support — visible hints only, never technical jargon.
 * All text from [LisaPersonalityEngine].
 */
object CaregiverConfidenceEngine {

    private val personality: LisaPersonalityEngine get() = LisaPersonalityEngines.default

    fun hintText(context: DialogueContext, moment: CaregiverSupportMoment): String? =
        personality.generateCaregiverSupportDialogue(context.copy(caregiverVisible = true), moment)
            .text
            .takeIf { it.isNotBlank() }

    fun setupSupport(setupStep: Int): CaregiverSupportUiState {
        val ctx = baseContext(AppFeature.GuidedLearning).copy(setupStep = setupStep, caregiverVisible = true)
        return when (setupStep) {
            0 -> CaregiverSupportUiState(
                primaryHint = hintText(ctx, CaregiverSupportMoment.PhonePositioning),
                whatToDoNow = hintText(ctx, CaregiverSupportMoment.WhatToDoNow),
                moment = CaregiverSupportMoment.PhonePositioning
            )
            1 -> CaregiverSupportUiState(
                primaryHint = hintText(ctx, CaregiverSupportMoment.CaregiverOnlyHint),
                whatToDoNow = whatToDoNowForSetupStep(setupStep, ctx),
                moment = CaregiverSupportMoment.CaregiverOnlyHint
            )
            else -> CaregiverSupportUiState(
                primaryHint = hintText(ctx, CaregiverSupportMoment.CameraLighting),
                whatToDoNow = whatToDoNowForSetupStep(setupStep, ctx),
                moment = CaregiverSupportMoment.CameraLighting
            )
        }
    }

    fun calibrationSupport(
        dotIndex: Int,
        totalDots: Int,
        calibrationPoor: Boolean = false
    ): CaregiverSupportUiState {
        val ctx = baseContext(AppFeature.GuidedLearning).copy(
            calibrationDotIndex = dotIndex,
            calibrationTotalDots = totalDots,
            calibrationPoor = calibrationPoor,
            caregiverVisible = true
        )
        val moment = if (calibrationPoor) {
            CaregiverSupportMoment.Troubleshooting
        } else {
            CaregiverSupportMoment.CalibrationSupport
        }
        return CaregiverSupportUiState(
            primaryHint = hintText(ctx, moment),
            progressLine = "Calibration: ${dotIndex.coerceAtMost(totalDots)} of $totalDots",
            whatToDoNow = hintText(ctx, CaregiverSupportMoment.WhatToDoNow),
            moment = moment
        )
    }

    fun communicationSupport(
        facePresent: Boolean,
        calibrationHealth: CalibrationHealthState,
        consecutiveFailures: Int = 0
    ): CaregiverSupportUiState {
        val ctx = baseContext(AppFeature.Communication).copy(
            faceDetected = facePresent,
            caregiverVisible = true,
            consecutiveFailures = consecutiveFailures
        )
        if (!facePresent) {
            return CaregiverSupportUiState(
                primaryHint = hintText(ctx, CaregiverSupportMoment.TrackingRecovery),
                whatToDoNow = hintText(ctx, CaregiverSupportMoment.WhatToDoNow),
                moment = CaregiverSupportMoment.TrackingRecovery
            )
        }
        val needsTroubleshooting = calibrationHealth == CalibrationHealthState.CalibrationInvalid ||
            calibrationHealth == CalibrationHealthState.CalibrationRequired ||
            consecutiveFailures >= 3
        if (needsTroubleshooting) {
            return CaregiverSupportUiState(
                primaryHint = hintText(ctx, CaregiverSupportMoment.Troubleshooting),
                whatToDoNow = hintText(ctx, CaregiverSupportMoment.WhatToDoNow),
                moment = CaregiverSupportMoment.Troubleshooting
            )
        }
        return CaregiverSupportUiState(
            primaryHint = hintText(ctx, CaregiverSupportMoment.CaregiverOnlyHint),
            moment = CaregiverSupportMoment.CaregiverOnlyHint
        )
    }

    fun lessonProgressSupport(
        lessonNumber: Int,
        totalLessons: Int,
        successes: Int
    ): CaregiverSupportUiState {
        val ctx = baseContext(AppFeature.GuidedLearning).copy(
            currentLessonIndex = lessonNumber - 1,
            completedLessonCount = successes,
            caregiverVisible = true
        )
        return CaregiverSupportUiState(
            primaryHint = hintText(ctx, CaregiverSupportMoment.ProgressVisibility),
            progressLine = "Lesson $lessonNumber of $totalLessons · $successes successes",
            moment = CaregiverSupportMoment.ProgressVisibility
        )
    }

    fun recommendationHint(recommendation: CalibrationRecommendation): String? {
        val moment = when (recommendation) {
            CalibrationRecommendation.AdjustPhonePosition -> CaregiverSupportMoment.PhonePositioning
            CalibrationRecommendation.ImproveLighting -> CaregiverSupportMoment.CameraLighting
            CalibrationRecommendation.ReduceMovement,
            CalibrationRecommendation.CleanCamera,
            CalibrationRecommendation.PracticeAgain -> CaregiverSupportMoment.Troubleshooting
            CalibrationRecommendation.RepeatCalibration -> CaregiverSupportMoment.CalibrationSupport
            CalibrationRecommendation.None -> return null
        }
        return hintText(baseContext(AppFeature.Settings), moment)
    }

    fun trainingPhaseSupport(phase: TrainingPhase, setupStep: Int = 0): CaregiverSupportUiState? = when (phase) {
        TrainingPhase.Setup -> setupSupport(setupStep)
        TrainingPhase.Calibration -> calibrationSupport(0, 5)
        else -> null
    }

    fun catalogHasAllMoments(): Boolean =
        CaregiverSupportMoment.entries.all { moment ->
            DefaultDialogueCatalog.all("en").any { tagFor(moment) in it.contextTags }
        }

    private fun whatToDoNowForSetupStep(step: Int, ctx: DialogueContext): String? {
        val lines = personality.generateCaregiverSupportSequence(ctx, CaregiverSupportMoment.WhatToDoNow, 5)
        return lines.getOrNull(step.coerceIn(0, lines.lastIndex))?.text
    }

    private fun baseContext(feature: AppFeature) = DialogueContext(
        feature = feature,
        locale = "en",
        caregiverVisible = true
    )

    private fun tagFor(moment: CaregiverSupportMoment): String = when (moment) {
        CaregiverSupportMoment.PhonePositioning -> "cg_phone_position"
        CaregiverSupportMoment.CameraLighting -> "cg_camera_lighting"
        CaregiverSupportMoment.CalibrationSupport -> "cg_calibration_support"
        CaregiverSupportMoment.CaregiverOnlyHint -> "cg_caregiver_hint"
        CaregiverSupportMoment.Troubleshooting -> "cg_troubleshooting"
        CaregiverSupportMoment.ProgressVisibility -> "cg_progress_visibility"
        CaregiverSupportMoment.TrackingRecovery -> "cg_tracking_recovery"
        CaregiverSupportMoment.WhatToDoNow -> "cg_what_to_do_now"
    }
}
