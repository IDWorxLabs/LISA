package com.idworx.lisa.features.intelligentstartup.authority

import com.idworx.lisa.LisaUserProfile
import com.idworx.lisa.features.intelligentstartup.model.StartupProfileChoice

sealed class StartupProfileResolution {
    data object None : StartupProfileResolution()
    data class Single(val profileId: String) : StartupProfileResolution()
    data class Multiple(val choices: List<StartupProfileChoice>) : StartupProfileResolution()
}

/**
 * Pure authority: decide how startup resolves Communication Profiles.
 * No biometric face recognition — identity is caregiver/user selected.
 */
object StartupProfileAuthority {

    fun resolve(profiles: List<LisaUserProfile>): StartupProfileResolution = when {
        profiles.isEmpty() -> StartupProfileResolution.None
        profiles.size == 1 -> StartupProfileResolution.Single(profiles.first().id)
        else -> StartupProfileResolution.Multiple(profiles.map { toChoice(it) })
    }

    fun toChoice(profile: LisaUserProfile): StartupProfileChoice = StartupProfileChoice(
        id = profile.id,
        name = profile.name,
        languageLabel = profile.preferredLanguage.label,
        communicationLevelLabel = profile.communicationLevel.label,
        lastCalibratedAtMs = profile.eyeCalibration?.calibratedAtMs
    )

    fun clampSelectionIndex(index: Int, count: Int): Int {
        if (count <= 0) return 0
        return index.coerceIn(0, count - 1)
    }
}
