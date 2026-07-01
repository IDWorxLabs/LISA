package com.idworx.lisa

import kotlin.math.abs

/** Generous absolute cap so long communication sequences are not cut off early. */
const val MIN_SEQUENCE_WINDOW_MS = 15000L

/** Reserved: close overlays (help, quick controls, practice). No speech, no confirmation. */
const val CLOSE_OVERLAY_LEFT_WINKS = 4
const val CLOSE_OVERLAY_RIGHT_WINKS = 0

/** Reserved: open Quick Controls overlay. No speech, no confirmation. */
const val OPEN_QUICK_CONTROLS_LEFT_WINKS = 0
const val OPEN_QUICK_CONTROLS_RIGHT_WINKS = 4

enum class SystemCommandAction {
    OpenQuickControls,
    CloseOverlay,
    SetSpeedFast,
    SetSpeedNormal,
    SetSpeedSlow,
    DecreaseSensitivity,
    IncreaseSensitivity,
    RepeatLastPhrase,
    TogglePauseListening,
    OpenPracticeMode,
    CloseQuickControls
}

data class SystemCommand(
    val left: Int,
    val right: Int,
    val action: SystemCommandAction,
    val labelKey: String
) {
    val sequenceLabel: String get() = "L$left R$right"
}

object LisaSystemLanguage {

    val globalCommands: List<SystemCommand> = listOf(
        SystemCommand(OPEN_QUICK_CONTROLS_LEFT_WINKS, OPEN_QUICK_CONTROLS_RIGHT_WINKS, SystemCommandAction.OpenQuickControls, "open_quick_controls"),
        SystemCommand(CLOSE_OVERLAY_LEFT_WINKS, CLOSE_OVERLAY_RIGHT_WINKS, SystemCommandAction.CloseOverlay, "close_help")
    )

    val quickControlCommands: List<SystemCommand> = listOf(
        SystemCommand(1, 0, SystemCommandAction.SetSpeedFast, "speed_fast"),
        SystemCommand(2, 0, SystemCommandAction.SetSpeedNormal, "speed_normal"),
        SystemCommand(3, 0, SystemCommandAction.SetSpeedSlow, "speed_slow"),
        SystemCommand(0, 1, SystemCommandAction.DecreaseSensitivity, "sensitivity_decrease"),
        SystemCommand(0, 2, SystemCommandAction.IncreaseSensitivity, "sensitivity_increase"),
        SystemCommand(1, 1, SystemCommandAction.RepeatLastPhrase, "repeat_last"),
        SystemCommand(2, 2, SystemCommandAction.TogglePauseListening, "toggle_pause"),
        SystemCommand(3, 3, SystemCommandAction.OpenPracticeMode, "open_practice"),
        SystemCommand(CLOSE_OVERLAY_LEFT_WINKS, CLOSE_OVERLAY_RIGHT_WINKS, SystemCommandAction.CloseQuickControls, "close_quick_controls")
    )

    val trainingCommands: List<SystemCommand> =
        globalCommands.filter { it.action != SystemCommandAction.CloseOverlay || it.left != EMERGENCY_LEFT_WINKS } +
            quickControlCommands.filter { it.action != SystemCommandAction.CloseQuickControls }

    fun allReservedSequences(): Set<Pair<Int, Int>> =
        (globalCommands + quickControlCommands)
            .map { it.left to it.right }
            .toSet() + (EMERGENCY_LEFT_WINKS to EMERGENCY_RIGHT_WINKS)

    fun isReservedSystemSequence(left: Int, right: Int): Boolean =
        (left to right) in allReservedSequences()

    fun resolveGlobalCommand(left: Int, right: Int): SystemCommandAction? = when {
        isEmergencySequence(left, right) -> null // handled by emergency path
        left == OPEN_QUICK_CONTROLS_LEFT_WINKS && right == OPEN_QUICK_CONTROLS_RIGHT_WINKS ->
            SystemCommandAction.OpenQuickControls
        left == CLOSE_OVERLAY_LEFT_WINKS && right == CLOSE_OVERLAY_RIGHT_WINKS ->
            SystemCommandAction.CloseOverlay
        else -> null
    }

    fun resolveQuickControlCommand(left: Int, right: Int): SystemCommandAction? =
        quickControlCommands.firstOrNull { it.left == left && it.right == right }?.action

    fun labelFor(command: SystemCommand, uiStrings: LisaUiStrings): String = when (command.labelKey) {
        "open_quick_controls" -> uiStrings.systemOpenQuickControls
        "close_help" -> uiStrings.systemCloseHelp
        "speed_fast" -> uiStrings.systemSpeedFast
        "speed_normal" -> uiStrings.systemSpeedNormal
        "speed_slow" -> uiStrings.systemSpeedSlow
        "sensitivity_decrease" -> uiStrings.systemSensitivityDecrease
        "sensitivity_increase" -> uiStrings.systemSensitivityIncrease
        "repeat_last" -> uiStrings.systemRepeatLast
        "toggle_pause" -> uiStrings.systemTogglePause
        "open_practice" -> uiStrings.systemOpenPractice
        "close_quick_controls" -> uiStrings.systemCloseQuickControls
        else -> command.labelKey
    }

    fun contextualSystemHelp(uiStrings: LisaUiStrings): List<SystemCommand> = listOf(
        globalCommands.first { it.action == SystemCommandAction.OpenQuickControls },
        globalCommands.first { it.action == SystemCommandAction.CloseOverlay },
        SystemCommand(EMERGENCY_LEFT_WINKS, EMERGENCY_RIGHT_WINKS, SystemCommandAction.CloseOverlay, "emergency")
    )

    fun labelForKey(labelKey: String, uiStrings: LisaUiStrings): String = when (labelKey) {
        "emergency" -> uiStrings.emergency
        else -> trainingCommands.firstOrNull { it.labelKey == labelKey }?.let { labelFor(it, uiStrings) }
            ?: globalCommands.firstOrNull { it.labelKey == labelKey }?.let { labelFor(it, uiStrings) }
            ?: labelKey
    }
}

data class ContextualHelpSuggestion(
    val left: Int,
    val right: Int,
    val phrase: String,
    val englishSubtitle: String? = null,
    val isSystemCommand: Boolean = false,
    val systemLabel: String? = null
)

object ContextualUnknownHelp {

    fun suggestions(
        enteredLeft: Int,
        enteredRight: Int,
        mappings: List<WinkMapping>,
        language: PreferredLanguage,
        uiStrings: LisaUiStrings
    ): List<ContextualHelpSuggestion> {
        val communication = mappings
            .filter { isSequenceEligibleForSpeech(it.left, it.right) }
            .filter { !LisaSystemLanguage.isReservedSystemSequence(it.left, it.right) }
            .sortedBy { abs(it.left - enteredLeft) + abs(it.right - enteredRight) }
            .take(5)
            .map { mapping ->
                ContextualHelpSuggestion(
                    left = mapping.left,
                    right = mapping.right,
                    phrase = mapping.localizedPhrase(language),
                    englishSubtitle = if (language != PreferredLanguage.English && !mapping.isCustom) {
                        LisaCoreVocabulary.text(mapping.vocabularyId, PreferredLanguage.English)
                    } else null
                )
            }

        val system = LisaSystemLanguage.contextualSystemHelp(uiStrings).map { cmd ->
            val label = if (cmd.labelKey == "emergency") uiStrings.emergency else LisaSystemLanguage.labelFor(cmd, uiStrings)
            ContextualHelpSuggestion(
                left = cmd.left,
                right = cmd.right,
                phrase = label,
                isSystemCommand = true,
                systemLabel = label
            )
        }

        return communication + system
    }
}

data class PracticeItem(
    val vocabularyId: String,
    val left: Int,
    val right: Int
)

object PracticeModeCatalog {
    val items: List<PracticeItem> = listOf(
        PracticeItem("hello", 1, 6),
        PracticeItem("yes", 2, 6),
        PracticeItem("no", 0, 7),
        PracticeItem("i_need_water", 0, 3),
        PracticeItem("emergency", 6, 0)
    )
}

// Legacy aliases — use LisaSystemLanguage constants
const val CLOSE_HELP_LEFT_WINKS = CLOSE_OVERLAY_LEFT_WINKS
const val CLOSE_HELP_RIGHT_WINKS = CLOSE_OVERLAY_RIGHT_WINKS
const val QUICK_CONTROLS_LEFT_WINKS = OPEN_QUICK_CONTROLS_LEFT_WINKS
const val QUICK_CONTROLS_RIGHT_WINKS = OPEN_QUICK_CONTROLS_RIGHT_WINKS

fun isCloseHelpSequence(left: Int, right: Int): Boolean =
    left == CLOSE_OVERLAY_LEFT_WINKS && right == CLOSE_OVERLAY_RIGHT_WINKS

fun isQuickControlsSequence(left: Int, right: Int): Boolean =
    left == OPEN_QUICK_CONTROLS_LEFT_WINKS && right == OPEN_QUICK_CONTROLS_RIGHT_WINKS

fun isReservedSystemSequence(left: Int, right: Int): Boolean =
    LisaSystemLanguage.isReservedSystemSequence(left, right)

fun formatWinkSequenceDescription(left: Int, right: Int, uiStrings: LisaUiStrings): String =
    uiStrings.winkSequenceDescription(left, right)

fun formatWinkSequenceShort(left: Int, right: Int): String = "L$left R$right"
