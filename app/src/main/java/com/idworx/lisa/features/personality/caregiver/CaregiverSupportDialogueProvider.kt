package com.idworx.lisa.features.personality.caregiver

import com.idworx.lisa.features.personality.engine.LisaDialogueSelector
import com.idworx.lisa.features.personality.model.CaregiverSupportMoment
import com.idworx.lisa.features.personality.model.DialogueCategory
import com.idworx.lisa.features.personality.model.DialogueContext
import com.idworx.lisa.features.personality.model.LisaDialogue

class CaregiverSupportDialogueProvider(
    private val selector: LisaDialogueSelector
) {
    fun generate(context: DialogueContext, moment: CaregiverSupportMoment): LisaDialogue =
        selector.select(categoryFor(moment), context) { tagFilter(moment, it) }

    fun generateSequence(
        context: DialogueContext,
        moment: CaregiverSupportMoment,
        maxLines: Int
    ): List<LisaDialogue> =
        selector.selectSequence(categoryFor(moment), context) { tagFilter(moment, it) }
            .distinctBy { it.id }
            .take(maxLines.coerceAtLeast(1))

    private fun categoryFor(moment: CaregiverSupportMoment): DialogueCategory = when (moment) {
        CaregiverSupportMoment.PhonePositioning -> DialogueCategory.SettingsGuidance
        CaregiverSupportMoment.CameraLighting -> DialogueCategory.SettingsGuidance
        CaregiverSupportMoment.CalibrationSupport -> DialogueCategory.RecalibrationGuidance
        CaregiverSupportMoment.CaregiverOnlyHint -> DialogueCategory.Instruction
        CaregiverSupportMoment.Troubleshooting -> DialogueCategory.SensitivityGuidance
        CaregiverSupportMoment.ProgressVisibility -> DialogueCategory.Instruction
        CaregiverSupportMoment.TrackingRecovery -> DialogueCategory.Comfort
        CaregiverSupportMoment.WhatToDoNow -> DialogueCategory.Instruction
    }

    private fun tagFilter(moment: CaregiverSupportMoment, dialogue: LisaDialogue): Boolean =
        dialogue.contextTags.contains(tagFor(moment))

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
