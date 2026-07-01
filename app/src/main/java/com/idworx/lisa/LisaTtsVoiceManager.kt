package com.idworx.lisa

import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import java.util.Locale

enum class TtsLanguageAvailability {
    Available,
    MissingData,
    NotSupported
}

data class TtsLanguageCheck(
    val availability: TtsLanguageAvailability,
    val resolvedLocale: Locale?,
    val checkedLocales: List<Locale>
)

data class LisaVoiceOption(
    val name: String,
    val displayLabel: String,
    val localeTag: String,
    val isNetwork: Boolean
)

data class LisaVoiceSettingsState(
    val language: PreferredLanguage = PreferredLanguage.English,
    val languageCheck: TtsLanguageCheck = TtsLanguageCheck(
        availability = TtsLanguageAvailability.NotSupported,
        resolvedLocale = null,
        checkedLocales = emptyList()
    ),
    val availableVoices: List<LisaVoiceOption> = emptyList(),
    val selectedVoiceName: String? = null,
    val currentVoiceLabel: String? = null,
    val ttsEngineLabel: String = "—",
    val showPoorVoiceWarning: Boolean = false,
    val ttsReady: Boolean = false
)

object LisaTtsVoiceManager {

    fun localeCandidates(language: PreferredLanguage): List<Locale> = when (language) {
        PreferredLanguage.English -> listOf(
            Locale.forLanguageTag("en-ZA"),
            Locale.forLanguageTag("en-US"),
            Locale.forLanguageTag("en-GB")
        )
        PreferredLanguage.Afrikaans -> listOf(
            Locale.forLanguageTag("af-ZA"),
            Locale.forLanguageTag("af")
        )
        PreferredLanguage.IsiZulu -> listOf(
            Locale.forLanguageTag("zu-ZA"),
            Locale.forLanguageTag("zu")
        )
    }

    fun checkLanguage(tts: TextToSpeech, language: PreferredLanguage): TtsLanguageCheck {
        val candidates = localeCandidates(language)
        var missingDataLocale: Locale? = null

        for (locale in candidates) {
            when (tts.isLanguageAvailable(locale)) {
                TextToSpeech.LANG_AVAILABLE,
                TextToSpeech.LANG_COUNTRY_AVAILABLE,
                TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE -> {
                    return TtsLanguageCheck(
                        availability = TtsLanguageAvailability.Available,
                        resolvedLocale = locale,
                        checkedLocales = candidates
                    )
                }
                TextToSpeech.LANG_MISSING_DATA -> {
                    if (missingDataLocale == null) missingDataLocale = locale
                }
            }
        }

        return if (missingDataLocale != null) {
            TtsLanguageCheck(
                availability = TtsLanguageAvailability.MissingData,
                resolvedLocale = missingDataLocale,
                checkedLocales = candidates
            )
        } else {
            TtsLanguageCheck(
                availability = TtsLanguageAvailability.NotSupported,
                resolvedLocale = null,
                checkedLocales = candidates
            )
        }
    }

    fun resolveBestLocale(tts: TextToSpeech, language: PreferredLanguage): Locale? {
        val check = checkLanguage(tts, language)
        return check.resolvedLocale ?: localeCandidates(language).firstOrNull()
    }

    fun voicesForLanguage(voices: Set<Voice>?, language: PreferredLanguage): List<Voice> {
        val candidates = localeCandidates(language)
        return voices.orEmpty()
            .filter { voice -> voiceMatchesCandidates(voice, candidates) }
            .sortedWith(compareByDescending<Voice> { voiceScore(it, candidates) }.thenBy { it.name })
    }

    fun toVoiceOptions(voices: List<Voice>): List<LisaVoiceOption> =
        voices.map { voice ->
            LisaVoiceOption(
                name = voice.name,
                displayLabel = formatVoiceLabel(voice),
                localeTag = voice.locale.toLanguageTag(),
                isNetwork = voice.isNetworkConnectionRequired
            )
        }

    fun buildSettingsState(
        tts: TextToSpeech?,
        profile: LisaUserProfile,
        ttsEngineLabel: String = "—"
    ): LisaVoiceSettingsState {
        if (tts == null) {
            return LisaVoiceSettingsState(
                language = profile.preferredLanguage,
                selectedVoiceName = profile.selectedTtsVoiceName,
                ttsEngineLabel = ttsEngineLabel,
                ttsReady = false
            )
        }

        val language = profile.preferredLanguage
        val languageCheck = checkLanguage(tts, language)
        val matchingVoices = voicesForLanguage(tts.voices, language)
        val voiceOptions = toVoiceOptions(matchingVoices)
        val selectedName = profile.selectedTtsVoiceName?.takeIf { saved ->
            matchingVoices.any { it.name == saved }
        } ?: matchingVoices.firstOrNull()?.name
        val currentLabel = voiceOptions.find { it.name == selectedName }?.displayLabel

        return LisaVoiceSettingsState(
            language = language,
            languageCheck = languageCheck,
            availableVoices = voiceOptions,
            selectedVoiceName = selectedName,
            currentVoiceLabel = currentLabel,
            ttsEngineLabel = ttsEngineLabel,
            showPoorVoiceWarning = shouldShowPoorVoiceWarning(language, languageCheck, matchingVoices),
            ttsReady = true
        )
    }

    fun applyForProfile(tts: TextToSpeech, profile: LisaUserProfile) {
        val locale = resolveBestLocale(tts, profile.preferredLanguage)
            ?: LisaUiStrings.ttsLocale(profile.preferredLanguage)
        tts.language = locale

        val matchingVoices = voicesForLanguage(tts.voices, profile.preferredLanguage)
        val selectedVoice = profile.selectedTtsVoiceName
            ?.let { saved -> matchingVoices.find { it.name == saved } }
            ?: matchingVoices.firstOrNull()

        if (selectedVoice != null) {
            tts.voice = selectedVoice
        }
    }

    fun samplePhrase(language: PreferredLanguage): String = when (language) {
        PreferredLanguage.English -> "Hello. This is LISA speaking."
        PreferredLanguage.Afrikaans -> "Hallo. LISA praat."
        PreferredLanguage.IsiZulu -> "Sawubona. LISA ikhuluma."
    }

    fun availabilityLabel(check: TtsLanguageCheck, uiStrings: LisaUiStrings): String = when (check.availability) {
        TtsLanguageAvailability.Available -> {
            val locale = check.resolvedLocale?.displayName ?: uiStrings.voiceAvailable
            uiStrings.voiceAvailableFor(locale)
        }
        TtsLanguageAvailability.MissingData -> uiStrings.voiceMissingData
        TtsLanguageAvailability.NotSupported -> uiStrings.voiceNotSupported
    }

    private fun shouldShowPoorVoiceWarning(
        language: PreferredLanguage,
        check: TtsLanguageCheck,
        voices: List<Voice>
    ): Boolean {
        if (language == PreferredLanguage.English) return false
        if (check.availability != TtsLanguageAvailability.Available) return true
        val localVoices = voices.filterNot { it.isNetworkConnectionRequired }
        return localVoices.isEmpty()
    }

    private fun voiceMatchesCandidates(voice: Voice, candidates: List<Locale>): Boolean =
        candidates.any { candidate -> localeMatches(voice.locale, candidate) }

    private fun localeMatches(voiceLocale: Locale, candidate: Locale): Boolean {
        if (!voiceLocale.language.equals(candidate.language, ignoreCase = true)) return false
        if (candidate.country.isBlank()) return true
        return voiceLocale.country.isBlank() ||
            voiceLocale.country.equals(candidate.country, ignoreCase = true)
    }

    private fun voiceScore(voice: Voice, candidates: List<Locale>): Int {
        var score = 0
        val index = candidates.indexOfFirst { localeMatches(voice.locale, it) }
        if (index >= 0) score += (candidates.size - index) * 10
        if (!voice.isNetworkConnectionRequired) score += 5
        return score
    }

    private fun formatVoiceLabel(voice: Voice): String {
        val localeLabel = voice.locale.displayName
        val networkSuffix = if (voice.isNetworkConnectionRequired) " (online)" else ""
        return "$localeLabel · ${voice.name}$networkSuffix"
    }
}
