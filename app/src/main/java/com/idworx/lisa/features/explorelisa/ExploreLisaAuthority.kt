package com.idworx.lisa.features.explorelisa

import com.idworx.lisa.GuidedModeNavigation
import com.idworx.lisa.MainMenuDestination
import com.idworx.lisa.MainMenuDestinationShortcuts
import com.idworx.lisa.MainMenuProductionUiAuthority
import com.idworx.lisa.features.brain1interactionstandard.model.UniversalInteractionGestures
import com.idworx.lisa.features.onboardingguide.model.NavigationAction
import com.idworx.lisa.formatWinkSequenceShort

/**
 * RC8.13 / RC8.14 — Final Guided Learning lesson "Explore LISA".
 *
 * Confidence tour only: the same production blink sequences open Menu, Voice, Settings,
 * Back, and Close. No new gestures. No feature teaching. No duplicated navigation logic.
 *
 * RC8.14 — Open Voice / Open Settings use the Menu direct-destination sequences
 * (Voice L3 R1, Settings L5 R5), never generic Select L1 R1.
 */
object ExploreLisaAuthority {

    const val LESSON_TITLE: String = "Explore LISA"

    const val INTRO_LINE_1: String = "You already know how to use LISA."
    const val INTRO_LINE_2: String = "The same blink sequences work everywhere."

    val introSpeech: String = "$INTRO_LINE_1 $INTRO_LINE_2"

    const val FINAL_LINE_1: String = "You've completed Guided Learning."
    const val FINAL_LINE_2: String =
        "Everything you've learned works throughout LISA."
    const val FINAL_LINE_3: String = "You are ready to communicate."

    val finalSpeech: String =
        "$FINAL_LINE_1 $FINAL_LINE_2 $FINAL_LINE_3"

    const val FINISH_BUTTON_LABEL: String = "Finish"

    /** Catalog ids — order matches [com.idworx.lisa.features.onboardingguide.lessons.TrainingLessonCatalog]. */
    const val ID_OPEN_MENU: String = "explore_open_menu"
    const val ID_SELECT_VOICE: String = "explore_select_voice"
    const val ID_OPEN_VOICE: String = "explore_open_voice"
    const val ID_BACK_VOICE: String = "explore_back_voice"
    const val ID_SELECT_SETTINGS: String = "explore_select_settings"
    const val ID_OPEN_SETTINGS: String = "explore_open_settings"
    const val ID_BACK_SETTINGS: String = "explore_back_settings"
    const val ID_CLOSE_MENU: String = "explore_close_menu"
    const val ID_FINISH: String = "explore_finish"

    val exploreLessonIds: Set<String> = setOf(
        ID_OPEN_MENU,
        ID_SELECT_VOICE,
        ID_OPEN_VOICE,
        ID_BACK_VOICE,
        ID_SELECT_SETTINGS,
        ID_OPEN_SETTINGS,
        ID_BACK_SETTINGS,
        ID_CLOSE_MENU,
        ID_FINISH
    )

    fun isExploreLessonId(id: String): Boolean = id in exploreLessonIds

    fun isExploreAction(action: NavigationAction): Boolean = when (action) {
        NavigationAction.OpenMenu,
        NavigationAction.MenuSelectVoice,
        NavigationAction.OpenVoice,
        NavigationAction.BackFromDestination,
        NavigationAction.MenuSelectSettings,
        NavigationAction.OpenSettings,
        NavigationAction.FinishGuidedLearning -> true
        NavigationAction.CloseMenu -> false // also used by workspace Back lesson
        else -> false
    }

    fun successPhraseForLessonId(lessonId: String): String? = when (lessonId) {
        ID_OPEN_MENU -> "Great."
        ID_SELECT_VOICE -> "Good."
        else -> null
    }

    fun openMenuSequenceLabel(): String = MainMenuProductionUiAuthority.openMenuSequenceLabel()

    fun moveDownSequenceLabel(): String =
        formatWinkSequenceShort(GuidedModeNavigation.NEXT_LEFT, GuidedModeNavigation.NEXT_RIGHT)

    fun voiceSequenceLabel(): String =
        MainMenuDestinationShortcuts.sequenceLabelForDestination(MainMenuDestination.Voice)

    fun settingsSequenceLabel(): String =
        MainMenuDestinationShortcuts.sequenceLabelForDestination(MainMenuDestination.Settings)

    fun voiceGesture(): Pair<Int, Int> =
        MainMenuDestinationShortcuts.gestureForDestination(MainMenuDestination.Voice)

    fun settingsGesture(): Pair<Int, Int> =
        MainMenuDestinationShortcuts.gestureForDestination(MainMenuDestination.Settings)

    fun matchesVoiceDestination(left: Int, right: Int): Boolean =
        MainMenuDestinationShortcuts.destinationForGesture(left, right) == MainMenuDestination.Voice

    fun matchesSettingsDestination(left: Int, right: Int): Boolean =
        MainMenuDestinationShortcuts.destinationForGesture(left, right) == MainMenuDestination.Settings

    fun selectSequenceLabel(): String =
        formatWinkSequenceShort(
            UniversalInteractionGestures.CONFIRM_LEFT,
            UniversalInteractionGestures.CONFIRM_RIGHT
        )

    fun backSequenceLabel(): String =
        formatWinkSequenceShort(GuidedModeNavigation.BACK_LEFT, GuidedModeNavigation.BACK_RIGHT)

    fun closeMenuSequenceLabel(): String = MainMenuProductionUiAuthority.closeMenuSequenceLabel()

    fun finishSequenceLabel(): String = selectSequenceLabel()

    fun highlightDestinationFor(action: NavigationAction): MainMenuDestination? = when (action) {
        NavigationAction.MenuSelectVoice, NavigationAction.OpenVoice -> MainMenuDestination.Voice
        NavigationAction.MenuSelectSettings, NavigationAction.OpenSettings ->
            MainMenuDestination.Settings
        else -> null
    }

    fun instructionFor(action: NavigationAction): String = when (action) {
        NavigationAction.OpenMenu ->
            "$INTRO_LINE_1\n$INTRO_LINE_2\n\nOpen the Menu."
        NavigationAction.MenuSelectVoice -> "Move down until Voice is selected."
        NavigationAction.OpenVoice -> "Open Voice."
        NavigationAction.BackFromDestination -> "Go back."
        NavigationAction.MenuSelectSettings -> "Move down until Settings is selected."
        NavigationAction.OpenSettings -> "Open Settings."
        NavigationAction.CloseMenu -> "Close the Menu."
        NavigationAction.FinishGuidedLearning ->
            "$FINAL_LINE_1\n$FINAL_LINE_2\n$FINAL_LINE_3"
        else -> ""
    }

    /** Sequences used by Explore — must already exist in production (no new catalogue entries). */
    fun usesOnlyExistingSequences(): Boolean {
        val openMenu = GuidedModeNavigation.OPEN_MAIN_MENU_LEFT to GuidedModeNavigation.OPEN_MAIN_MENU_RIGHT
        val moveDown = GuidedModeNavigation.NEXT_LEFT to GuidedModeNavigation.NEXT_RIGHT
        val voice = voiceGesture()
        val settings = settingsGesture()
        val select = UniversalInteractionGestures.CONFIRM_LEFT to UniversalInteractionGestures.CONFIRM_RIGHT
        val back = GuidedModeNavigation.BACK_LEFT to GuidedModeNavigation.BACK_RIGHT
        return openMenu == (4 to 6) &&
            moveDown == (0 to 2) &&
            voice == (3 to 1) &&
            settings == (5 to 5) &&
            select == (1 to 1) &&
            back == (2 to 2)
    }
}
