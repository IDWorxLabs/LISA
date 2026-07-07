package com.idworx.lisa.features.onboardingguide.navigation

import com.idworx.lisa.GuidedModeNavigation
import com.idworx.lisa.GuidedVocabularyCategory
import com.idworx.lisa.LisaUiStrings
import com.idworx.lisa.PreferredLanguage
import com.idworx.lisa.features.onboardingguide.model.NavigationAction
import com.idworx.lisa.formatWinkSequenceShort
import com.idworx.lisa.isEmergencySequence

/**
 * Guided Training Mode for the real Communication Workspace — Guided Learning's navigation
 * lessons run *inside* the actual workspace UI instead of a standalone fake screen. [NORMAL]
 * is everyday use; [GUIDED_TRAINING] additionally shows a compact lesson card and highlights
 * the one real control the current lesson is teaching.
 */
enum class GuidedWorkspaceMode {
    NORMAL,
    GUIDED_TRAINING
}

/** The single real workspace control Guided Training highlights for the active lesson. */
enum class GuidedWorkspaceHighlightTarget {
    OpenCategories,
    CategoryRow,
    PhraseRow,
    Back,
    NextPage,
    PreviousPage,
    Emergency
}

/**
 * Where the floating lesson card docks — always above the bottom Menu/Reset row and never at
 * the top (that would sit behind the Listening/Watching-your-eyes banner). The side is chosen
 * automatically so the card never covers the control it is teaching: targets that live in the
 * left-hand category/phrase list dock the card on the right, and targets that live in the
 * right-hand navigation panel dock it on the left.
 */
enum class GuidedWorkspaceLessonCardDock {
    BottomStart,
    BottomEnd
}

/**
 * Maps a Guided Learning navigation lesson onto the real workspace control it teaches. Every
 * lesson highlights exactly one real control — never a mock screen.
 */
object GuidedWorkspaceTrainingSpec {

    /** Index of the real "Conversation" category — the category every lesson trains against. */
    val conversationCategoryIndex: Int = GuidedVocabularyCategory.ordered.indexOf(GuidedVocabularyCategory.Conversation)

    fun highlightTargetFor(action: NavigationAction): GuidedWorkspaceHighlightTarget? = when (action) {
        NavigationAction.OpenCategories -> GuidedWorkspaceHighlightTarget.OpenCategories
        NavigationAction.SelectCategory -> GuidedWorkspaceHighlightTarget.CategoryRow
        NavigationAction.SelectPhrase -> GuidedWorkspaceHighlightTarget.PhraseRow
        NavigationAction.CloseMenu -> GuidedWorkspaceHighlightTarget.Back
        NavigationAction.NextPage -> GuidedWorkspaceHighlightTarget.NextPage
        NavigationAction.PreviousPage -> GuidedWorkspaceHighlightTarget.PreviousPage
        NavigationAction.TriggerEmergency -> GuidedWorkspaceHighlightTarget.Emergency
        else -> null
    }

    /** Compact lesson-card title — what the learner is practicing right now. */
    fun lessonCardTitle(action: NavigationAction, uiStrings: LisaUiStrings): String = when (action) {
        NavigationAction.OpenCategories -> uiStrings.t("Open Categories", "Open Kategorieë", "Vula Izigaba")
        NavigationAction.SelectCategory -> uiStrings.t(
            "Select Conversation category",
            "Kies Gesprek-kategorie",
            "Khetha isigaba Ingxoxo"
        )
        NavigationAction.SelectPhrase -> uiStrings.t(
            "Select a phrase from Conversation",
            "Kies 'n frase van Gesprek",
            "Khetha umusho ku-Ingxoxo"
        )
        NavigationAction.CloseMenu -> uiStrings.t("Go Back", "Gaan Terug", "Buyela Emuva")
        NavigationAction.NextPage -> uiStrings.t("Next Page", "Volgende Bladsy", "Ikhasi Elilandelayo")
        NavigationAction.PreviousPage -> uiStrings.t("Previous Page", "Vorige Bladsy", "Ikhasi Elidlule")
        NavigationAction.TriggerEmergency -> uiStrings.t("Emergency", "Nood", "Usizo Oluphuthumayo")
        NavigationAction.ResetSequence -> uiStrings.t(
            "Reset / Return to main workspace",
            "Herstel / Terug na werkarea",
            "Setha Kabusha / Buyela"
        )
        else -> uiStrings.t("Practice", "Oefen", "Zijwayeze")
    }

    /**
     * The right-hand navigation panel (Back/Next/Previous/Emergency) runs the full height of
     * the workspace, so a card docked there would cover the highlighted button — dock it on the
     * opposite side instead. Category/phrase-list targets live on the left, so their card docks
     * on the right. Either way the card floats above the bottom Menu/Reset row, never at the top.
     */
    fun cardDockFor(highlightTarget: GuidedWorkspaceHighlightTarget?): GuidedWorkspaceLessonCardDock = when (highlightTarget) {
        GuidedWorkspaceHighlightTarget.Back,
        GuidedWorkspaceHighlightTarget.NextPage,
        GuidedWorkspaceHighlightTarget.PreviousPage,
        GuidedWorkspaceHighlightTarget.Emergency -> GuidedWorkspaceLessonCardDock.BottomStart
        GuidedWorkspaceHighlightTarget.OpenCategories,
        GuidedWorkspaceHighlightTarget.CategoryRow,
        GuidedWorkspaceHighlightTarget.PhraseRow,
        null -> GuidedWorkspaceLessonCardDock.BottomEnd
    }

    /** Short "Gesture: <gesture>" hint shown on the compact lesson card. */
    fun lessonCardGestureLabel(action: NavigationAction): String = when (action) {
        NavigationAction.OpenCategories ->
            formatWinkSequenceShort(GuidedModeNavigation.CATEGORIES_LEFT, GuidedModeNavigation.CATEGORIES_RIGHT)
        NavigationAction.SelectCategory ->
            formatWinkSequenceShort(GuidedModeNavigation.SELECT_LEFT, GuidedModeNavigation.SELECT_RIGHT)
        NavigationAction.SelectPhrase -> "Blink the highlighted phrase's gesture"
        NavigationAction.CloseMenu ->
            formatWinkSequenceShort(GuidedModeNavigation.BACK_LEFT, GuidedModeNavigation.BACK_RIGHT)
        NavigationAction.NextPage ->
            formatWinkSequenceShort(GuidedModeNavigation.NEXT_LEFT, GuidedModeNavigation.NEXT_RIGHT)
        NavigationAction.PreviousPage ->
            formatWinkSequenceShort(GuidedModeNavigation.PREVIOUS_LEFT, GuidedModeNavigation.PREVIOUS_RIGHT)
        NavigationAction.TriggerEmergency -> "L6 R0"
        NavigationAction.ResetSequence -> "Tap Reset"
        else -> ""
    }

    private fun LisaUiStrings.t(en: String, af: String, zu: String): String = when (language) {
        PreferredLanguage.English -> en
        PreferredLanguage.Afrikaans -> af
        PreferredLanguage.IsiZulu -> zu
    }
}

/**
 * Resolves which [NavigationAction] a real workspace gesture performs during Guided Training,
 * scoped by current context — the same gesture means different things in different screens
 * (e.g. Select opens a category in the menu, but selects/speaks a phrase in Vocabulary).
 */
object GuidedWorkspaceGestureContext {

    fun nextOrPreviousAction(left: Int, right: Int): NavigationAction? = when {
        GuidedModeNavigation.isNextSequence(left, right) -> NavigationAction.NextPage
        GuidedModeNavigation.isPreviousSequence(left, right) -> NavigationAction.PreviousPage
        else -> null
    }

    fun isTrainingEmergencyPractice(left: Int, right: Int): Boolean = isEmergencySequence(left, right)
}
