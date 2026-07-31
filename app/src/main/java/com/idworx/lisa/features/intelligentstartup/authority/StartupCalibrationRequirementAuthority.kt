package com.idworx.lisa.features.intelligentstartup.authority

import com.idworx.lisa.features.intelligentstartup.model.CalibrationCompatibilityLevel

/**
 * Single decision point for "must this launch run quick eye calibration before Welcome?".
 *
 * Version 1 rule: yes, always. Every launch runs the quick calibration sequence, so a stored
 * profile calibration can never route the user past it. The compatibility score is still
 * evaluated and recorded truthfully by [CalibrationCompatibilityAuthority] — it just no longer
 * decides the route. Flipping [VERSION_1_ALWAYS_CALIBRATES] restores the compatibility-based
 * returning-user shortcut without touching the state machine.
 */
object StartupCalibrationRequirementAuthority {

    const val VERSION_1_ALWAYS_CALIBRATES = true

    fun requiresQuickCalibration(level: CalibrationCompatibilityLevel): Boolean =
        VERSION_1_ALWAYS_CALIBRATES ||
            CalibrationCompatibilityAuthority.requiresQuickCalibration(level)

    fun skipsQuickCalibration(level: CalibrationCompatibilityLevel): Boolean =
        !requiresQuickCalibration(level)
}
