package com.idworx.lisa

/**
 * Settings & Controls — adjustable LISA preferences hub.
 *
 * Hub cards (UX): Sensitivity, Response Time, Speech Volume, Speech Speed only.
 * Listening / Repeat / Reset / Help remain available via Quick Controls and other paths;
 * they are intentionally not listed on this hub.
 *
 * Hub rail / gesture model:
 *
 * | Action                | Sequence | Behaviour on hub                      |
 * |-----------------------|----------|---------------------------------------|
 * | Open hub              | L5 R5    | Canonical entry                       |
 * | Scroll Up             | L2 R0    | Move selection up (does not open)     |
 * | Scroll Down           | L0 R2    | Move selection down (does not open)   |
 * | Select / Open         | L1 R1    | Open highlighted setting              |
 * | Speech Volume         | L1 R2    | Direct-open shared adjustment         |
 * | Speech Speed          | L3 R2    | Direct-open shared adjustment         |
 * | Speech Speed (alt)    | L1 R3    | Former “speak more slowly”            |
 * | Decrease / Increase   | L3 R1 / L1 R3 | On value screens                 |
 * | Save / Cancel         | L1 R1 / L2 R2 | Shared confirm / back            |
 */
object SpeechVolumeAuthority {
    const val MIN_LEVEL: Int = 1
    const val MAX_LEVEL: Int = 10
    const val DEFAULT_LEVEL: Int = 10

    fun coerce(level: Int): Int = level.coerceIn(MIN_LEVEL, MAX_LEVEL)

    /** Maps level 1..10 → TTS KEY_PARAM_VOLUME 0.10..1.00. */
    fun toTtsVolume(level: Int): Float = coerce(level) / MAX_LEVEL.toFloat()

    fun percentLabel(level: Int): String = "${(toTtsVolume(level) * 100).toInt()}%"
}

object SpeechSpeedAuthority {
    const val MIN_LEVEL: Int = 1
    const val MAX_LEVEL: Int = 5
    const val DEFAULT_LEVEL: Int = 3

    private val rates: FloatArray = floatArrayOf(0.50f, 0.75f, 1.00f, 1.25f, 1.50f)

    fun coerce(level: Int): Int = level.coerceIn(MIN_LEVEL, MAX_LEVEL)

    fun toSpeechRate(level: Int): Float = rates[coerce(level) - 1]

    fun fromSpeechRate(rate: Float): Int {
        var best = DEFAULT_LEVEL
        var bestDelta = Float.MAX_VALUE
        rates.forEachIndexed { index, candidate ->
            val delta = kotlin.math.abs(candidate - rate)
            if (delta < bestDelta) {
                bestDelta = delta
                best = index + 1
            }
        }
        return best
    }

    fun displayLabel(level: Int, uiStrings: LisaUiStrings): String = when (coerce(level)) {
        1, 2 -> uiStrings.guidedSpeechSpeedSlow
        3 -> uiStrings.guidedSpeechSpeedNormal
        else -> uiStrings.guidedSpeechSpeedFast
    }
}

/** Hub card / action identity for Settings & Controls (and retained internal control actions). */
enum class SettingsControlKind {
    Sensitivity,
    ResponseTime,
    SpeechVolume,
    SpeechSpeed,
    /** Retained for Listening screen / Quick Controls — not a hub card. */
    Listening,
    /** Retained for Quick Controls — not a hub card. */
    RepeatLastMessage,
    /** Retained for Reset handlers — not a hub card. */
    ResetSequence,
    /** Retained for help narration — not a hub card. */
    ShowHelp
}

/**
 * Canonical hub gesture bindings for the four adjustable settings.
 * Labels must match [GuidedNavigationController] SettingsMenu routing.
 */
object SettingsAndControlsHubSequences {
    val SENSITIVITY: Pair<Int, Int> =
        GuidedModeNavigation.PREVIOUS_LEFT to GuidedModeNavigation.PREVIOUS_RIGHT
    val RESPONSE_TIME: Pair<Int, Int> =
        GuidedModeNavigation.NEXT_LEFT to GuidedModeNavigation.NEXT_RIGHT
    val SPEECH_VOLUME: Pair<Int, Int> = 1 to 2
    val SPEECH_SPEED: Pair<Int, Int> = 3 to 2
    /** Alternate open for Speech Speed — former “Speak more slowly” sequence. */
    val SPEECH_SPEED_ALT: Pair<Int, Int> = 1 to 3
    /** Retained for Listening control screen when that mode is open. */
    val LISTENING: Pair<Int, Int> = 2 to 3
    val LISTENING_ALT: Pair<Int, Int> = 3 to 3
    val REPEAT_LAST: Pair<Int, Int> = 2 to 1
    val RESET_SEQUENCE: Pair<Int, Int> = 1 to 4
    val SHOW_HELP: Pair<Int, Int> = 4 to 2
    val BACK: Pair<Int, Int> =
        GuidedModeNavigation.BACK_LEFT to GuidedModeNavigation.BACK_RIGHT

    /** The four adjustable settings shown on the Settings & Controls hub. */
    val HUB_SETTING_KINDS: List<SettingsControlKind> = listOf(
        SettingsControlKind.Sensitivity,
        SettingsControlKind.ResponseTime,
        SettingsControlKind.SpeechVolume,
        SettingsControlKind.SpeechSpeed
    )

    /**
     * Hub gesture map for Settings & Controls selection model:
     * - L2 R0 / L0 R2 move selection (rail Scroll Up / Down) — do not open a card
     * - L1 R1 opens the highlighted setting
     * - L1 R2 / L3 R2 (/ L1 R3 alt) remain direct-open shortcuts for volume / speed
     */
    fun hubDirectOpenKindForGesture(left: Int, right: Int): SettingsControlKind? = when {
        left == SPEECH_VOLUME.first && right == SPEECH_VOLUME.second -> SettingsControlKind.SpeechVolume
        left == SPEECH_SPEED.first && right == SPEECH_SPEED.second -> SettingsControlKind.SpeechSpeed
        left == SPEECH_SPEED_ALT.first && right == SPEECH_SPEED_ALT.second -> SettingsControlKind.SpeechSpeed
        else -> null
    }

    @Deprecated("Hub uses selection + Select; use hubDirectOpenKindForGesture for card shortcuts")
    fun hubSettingKindForGesture(left: Int, right: Int): SettingsControlKind? =
        hubDirectOpenKindForGesture(left, right)

    @Deprecated("Use hubDirectOpenKindForGesture / selection model", ReplaceWith("hubDirectOpenKindForGesture(left, right)"))
    fun kindForGesture(left: Int, right: Int): SettingsControlKind? = hubDirectOpenKindForGesture(left, right)

    fun sequenceLabel(kind: SettingsControlKind): String {
        val pair = when (kind) {
            SettingsControlKind.Sensitivity -> SENSITIVITY
            SettingsControlKind.ResponseTime -> RESPONSE_TIME
            SettingsControlKind.SpeechVolume -> SPEECH_VOLUME
            SettingsControlKind.SpeechSpeed -> SPEECH_SPEED
            SettingsControlKind.Listening -> LISTENING
            SettingsControlKind.RepeatLastMessage -> REPEAT_LAST
            SettingsControlKind.ResetSequence -> RESET_SEQUENCE
            SettingsControlKind.ShowHelp -> SHOW_HELP
        }
        return formatWinkSequenceShort(pair.first, pair.second)
    }
}
