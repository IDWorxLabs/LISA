package com.idworx.lisa.features.eyediagnostic

/**
 * Debug-only gate for Eye Test Mode. Release builds must never expose the entry or screen.
 */
object EyeTestModeAccess {
    const val ENTRY_TITLE = "Eye Test Mode"
    const val ENTRY_SUPPORTING_TEXT = "Debug testing only"

    fun isEntryVisible(isDebugBuild: Boolean): Boolean = isDebugBuild

    fun isScreenAllowed(isDebugBuild: Boolean): Boolean = isDebugBuild
}
