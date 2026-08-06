package com.idworx.lisa.features.glassessetup

import com.idworx.lisa.LisaUiStrings

/**
 * Production choices on Welcome “Choose where to begin”.
 * Research tools never appear on this screen (debug or release).
 */
object WelcomeLaunchDestinationAuthority {

    fun productionChoiceTitles(uiStrings: LisaUiStrings): List<String> = listOf(
        uiStrings.startGuidedLearning,
        uiStrings.skipToCommunication,
        uiStrings.back
    )

    /** Always false — Choose where to begin is production-only. */
    fun allowsDiagnosticWelcomeEntries(isDebugBuild: Boolean): Boolean = false

    fun allowsDiagnosticNavigation(isDebugBuild: Boolean): Boolean = isDebugBuild

    fun allowsDiagnosticStorageInit(isDebugBuild: Boolean): Boolean = isDebugBuild

    fun allowsEngineeringToolsHub(isDebugBuild: Boolean): Boolean = isDebugBuild
}
