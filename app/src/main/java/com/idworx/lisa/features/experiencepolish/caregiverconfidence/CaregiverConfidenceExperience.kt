package com.idworx.lisa.features.experiencepolish.caregiverconfidence

import com.idworx.lisa.features.personality.dialogue.DefaultDialogueCatalog
import com.idworx.lisa.features.personality.model.CaregiverSupportMoment

object CaregiverConfidenceExperience {

    const val PHASE_NAME: String = "LISA Caregiver Confidence V1"

    fun phonePositionDialogues(): List<String> = texts(CaregiverSupportMoment.PhonePositioning)
    fun cameraLightingDialogues(): List<String> = texts(CaregiverSupportMoment.CameraLighting)
    fun calibrationSupportDialogues(): List<String> = texts(CaregiverSupportMoment.CalibrationSupport)
    fun caregiverHintDialogues(): List<String> = texts(CaregiverSupportMoment.CaregiverOnlyHint)
    fun troubleshootingDialogues(): List<String> = texts(CaregiverSupportMoment.Troubleshooting)
    fun progressVisibilityDialogues(): List<String> = texts(CaregiverSupportMoment.ProgressVisibility)
    fun trackingRecoveryDialogues(): List<String> = texts(CaregiverSupportMoment.TrackingRecovery)
    fun whatToDoNowDialogues(): List<String> = texts(CaregiverSupportMoment.WhatToDoNow)

    private fun texts(moment: CaregiverSupportMoment): List<String> {
        val tag = when (moment) {
            CaregiverSupportMoment.PhonePositioning -> "cg_phone_position"
            CaregiverSupportMoment.CameraLighting -> "cg_camera_lighting"
            CaregiverSupportMoment.CalibrationSupport -> "cg_calibration_support"
            CaregiverSupportMoment.CaregiverOnlyHint -> "cg_caregiver_hint"
            CaregiverSupportMoment.Troubleshooting -> "cg_troubleshooting"
            CaregiverSupportMoment.ProgressVisibility -> "cg_progress_visibility"
            CaregiverSupportMoment.TrackingRecovery -> "cg_tracking_recovery"
            CaregiverSupportMoment.WhatToDoNow -> "cg_what_to_do_now"
        }
        return DefaultDialogueCatalog.all("en")
            .filter { it.contextTags.contains(tag) }
            .sortedBy { it.id }
            .map { it.text }
    }
}
