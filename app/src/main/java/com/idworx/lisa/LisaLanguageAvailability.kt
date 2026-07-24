package com.idworx.lisa

/**
 * Version 1 language availability for Communication Profile and TTS routing.
 *
 * Version 1: English is the only selectable and functional preferred language.
 * Afrikaans and isiZulu remain visible as planned Version 2 options and must not
 * be routed through the current Samsung/system TTS.
 *
 * Version 2 product intent (not implemented here):
 * - Downloadable language packs
 * - Downloadable or selectable TTS voices
 * - Choice of supported speech engines
 * - Language-specific pronunciation validation
 * - Afrikaans support
 * - isiZulu support
 * - Potential additional languages later
 *
 * Do not add fake download buttons, placeholder stores, or non-functional voice selectors.
 */
object LisaLanguageAvailabilityAuthority {

    /** Languages shown in Communication Profile Preferred Language (includes Version 2 placeholders). */
    val displayedLanguages: List<PreferredLanguage>
        get() = PreferredLanguage.entries.toList()

    /** Languages that may become the active preferred language in Version 1. */
    val version1SelectableLanguages: List<PreferredLanguage>
        get() = listOf(PreferredLanguage.English)

    fun isSelectableInVersion1(language: PreferredLanguage): Boolean =
        language == PreferredLanguage.English

    /** Coerce any non–Version-1 language to English for profile use and TTS. */
    fun coerceForVersion1(language: PreferredLanguage): PreferredLanguage =
        if (isSelectableInVersion1(language)) language else PreferredLanguage.English

    /**
     * Recover legacy profiles that stored Afrikaans/isiZulu from older builds.
     * Preserves the profile identity and resets preferred language to English.
     *
     * @return recovered profiles and whether any preferred language was reset
     */
    fun recoverProfilesForVersion1(
        profiles: List<LisaUserProfile>
    ): Pair<List<LisaUserProfile>, Boolean> {
        var didReset = false
        val recovered = profiles.map { profile ->
            if (isSelectableInVersion1(profile.preferredLanguage)) {
                profile
            } else {
                didReset = true
                profile.copy(
                    preferredLanguage = PreferredLanguage.English,
                    updatedAt = System.currentTimeMillis()
                )
            }
        }
        return recovered to didReset
    }

    /** Short supporting line under future language options (fits mobile without truncation). */
    fun version2StatusLine(uiStrings: LisaUiStrings): String =
        uiStrings.languageVersion2StatusShort

    /** Calm message when the user activates a Version 2 language option. */
    fun version2ActivationMessage(uiStrings: LisaUiStrings): String =
        uiStrings.languageVersion2InfoMessage

    /** One-time calm explanation after legacy language recovery. */
    fun legacyLanguageResetMessage(uiStrings: LisaUiStrings): String =
        uiStrings.languageResetToEnglishMessage
}
