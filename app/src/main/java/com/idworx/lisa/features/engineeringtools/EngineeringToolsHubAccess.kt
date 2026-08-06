package com.idworx.lisa.features.engineeringtools

/**
 * Debug-only Engineering Tools Hub — not part of production onboarding.
 * Release builds must never show or open this hub.
 */
object EngineeringToolsHubAccess {
    const val HUB_TITLE = "Engineering Tools Hub"
    const val HUB_SUPPORTING =
        "Internal research tools. Not shown on Choose where to begin."

    fun isHubAllowed(isDebugBuild: Boolean): Boolean = isDebugBuild

    fun isEntryVisible(isDebugBuild: Boolean): Boolean = isDebugBuild

    enum class Tool {
        EyeTestMode,
        PersonalisedEyeProfile,
        SignalInvestigation,
        GlassesCharacterisation
    }
}
