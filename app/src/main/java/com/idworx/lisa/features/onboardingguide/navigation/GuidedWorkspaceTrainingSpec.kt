package com.idworx.lisa.features.onboardingguide.navigation

import com.idworx.lisa.EMERGENCY_LEFT_WINKS
import com.idworx.lisa.EMERGENCY_RIGHT_WINKS
import com.idworx.lisa.GuidedCategoryShortcuts
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
            "Start Communicating",
            "Begin Kommunikeer",
            "Qala Ukuxhumana"
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

    /**
     * Short "Gesture: <gesture>" hint shown on the compact lesson card. Every value is derived
     * from the same source the real workspace control itself uses — never a separately
     * hardcoded copy — so the lesson can never teach a gesture that differs from what the
     * highlighted control actually does or displays.
     *
     * [highlightedPhraseGesture] is the *actual* highlighted phrase entry's own sequence label
     * (e.g. from [com.idworx.lisa.GuidedVocabularyEntry.sequenceLabel]) — required for
     * [NavigationAction.SelectPhrase] to show a concrete gesture instead of a generic hint,
     * since which phrase (and therefore which gesture) is highlighted changes at runtime.
     */
    fun lessonCardGestureLabel(action: NavigationAction, highlightedPhraseGesture: String? = null): String = when (action) {
        NavigationAction.OpenCategories ->
            formatWinkSequenceShort(GuidedModeNavigation.CATEGORIES_LEFT, GuidedModeNavigation.CATEGORIES_RIGHT)
        NavigationAction.SelectCategory ->
            // The real category row shows its own direct-shortcut gesture
            // (GuidedCategoryShortcuts.sequenceLabelForCategory) — not the generic Select
            // confirm gesture — so the lesson must show and require exactly that.
            GuidedCategoryShortcuts.sequenceLabelForCategory(conversationCategoryIndex)
        NavigationAction.SelectPhrase ->
            highlightedPhraseGesture ?: "Blink the highlighted phrase's gesture"
        NavigationAction.CloseMenu ->
            formatWinkSequenceShort(GuidedModeNavigation.BACK_LEFT, GuidedModeNavigation.BACK_RIGHT)
        NavigationAction.NextPage ->
            formatWinkSequenceShort(GuidedModeNavigation.NEXT_LEFT, GuidedModeNavigation.NEXT_RIGHT)
        NavigationAction.PreviousPage ->
            formatWinkSequenceShort(GuidedModeNavigation.PREVIOUS_LEFT, GuidedModeNavigation.PREVIOUS_RIGHT)
        NavigationAction.TriggerEmergency ->
            formatWinkSequenceShort(EMERGENCY_LEFT_WINKS, EMERGENCY_RIGHT_WINKS)
        NavigationAction.ResetSequence ->
            // Touch-independent by design — the same gesture that finishes training also
            // performs the real workspace Reset action afterward (MainActivity.performReset()).
            formatWinkSequenceShort(GuidedModeNavigation.FINISH_TRAINING_LEFT, GuidedModeNavigation.FINISH_TRAINING_RIGHT)
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
