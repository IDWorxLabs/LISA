package com.idworx.lisa.features.onboardingguide.navigation

import com.idworx.lisa.EMERGENCY_LEFT_WINKS
import com.idworx.lisa.EMERGENCY_RIGHT_WINKS
import com.idworx.lisa.GuidedCategoryShortcuts
import com.idworx.lisa.GuidedModeNavigation
import com.idworx.lisa.GuidedVocabularyCategory
import com.idworx.lisa.LisaUiStrings
import com.idworx.lisa.PreferredLanguage
import com.idworx.lisa.features.guidedmedicalcategoryjourney.GuidedMedicalCategoryJourneyAuthority
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
    /**
     * Item Move Down Category (ScrollDown / L0 R2) — used by Lesson 16.
     * Not the viewport Next Page control.
     */
    NextPage,
    /**
     * Item Move Up Category (ScrollUp / L2 R0).
     * Not the viewport Previous Page control.
     */
    PreviousPage,
    /** RC8.26 — production Category Menu Next Page control (L0 R4). */
    CategoryNextPage,
    /** RC8.26 — production Category Menu Previous Page control (L4 R0). */
    CategoryPreviousPage,
    Emergency,
    /** RC8.24 — Open Selected Category panel control (L1 R1). */
    Select,
    /** RC8.28 — Settings hub Sensitivity card / Select Setting. */
    SettingsHubSensitivity,
    /** RC8.28 — Increase value control (L1 R3) on Sensitivity adjustment. */
    IncreaseValue
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

    /**
     * RC8.15 — Medical is the category lessons 16–18 train against (open → enter → speak).
     * Kept as the single training-category index shared by highlight, gesture label, and gates.
     */
    val medicalCategoryIndex: Int = GuidedMedicalCategoryJourneyAuthority.medicalCategoryIndex

    /** @deprecated Prefer [medicalCategoryIndex] — Conversation is no longer the trained category. */
    val conversationCategoryIndex: Int = GuidedVocabularyCategory.ordered.indexOf(GuidedVocabularyCategory.Conversation)

    fun highlightTargetFor(action: NavigationAction): GuidedWorkspaceHighlightTarget? = when (action) {
        NavigationAction.OpenCategories -> GuidedWorkspaceHighlightTarget.OpenCategories
        // RC8.19 — Lesson 16 teaches Move Down Category (ScrollDown panel control maps to NextPage).
        // Destination Medical is highlighted separately via destinationCategoryIndex — not CategoryRow
        // selection (RC8.17 single production selection authority remains intact).
        NavigationAction.MoveToMedicalCategory -> GuidedWorkspaceHighlightTarget.NextPage
        NavigationAction.SelectCategory -> null
        NavigationAction.SelectPhrase -> GuidedWorkspaceHighlightTarget.PhraseRow
        NavigationAction.CloseMenu -> GuidedWorkspaceHighlightTarget.Back
        // RC8.26 — Lessons 20–21 highlight viewport page controls, not Move Down/Up.
        NavigationAction.NextPage -> GuidedWorkspaceHighlightTarget.CategoryNextPage
        NavigationAction.PreviousPage -> GuidedWorkspaceHighlightTarget.CategoryPreviousPage
        NavigationAction.TriggerEmergency -> GuidedWorkspaceHighlightTarget.Emergency
        NavigationAction.AdjustSensitivity -> GuidedWorkspaceHighlightTarget.CategoryRow
        // Explore LISA highlights live on Main Menu rows / panels — not workspace chrome.
        NavigationAction.OpenMenu,
        NavigationAction.MenuSelectVoice,
        NavigationAction.OpenVoice,
        NavigationAction.BackFromDestination,
        NavigationAction.MenuSelectSettings,
        NavigationAction.OpenSettings,
        NavigationAction.FinishGuidedLearning -> null
        else -> null
    }

    /** Compact lesson-card title — what the learner is practicing right now. */
    fun lessonCardTitle(action: NavigationAction, uiStrings: LisaUiStrings): String = when (action) {
        NavigationAction.OpenCategories -> uiStrings.t("Open Categories", "Open Kategorieë", "Vula Izigaba")
        NavigationAction.MoveToMedicalCategory -> GuidedMedicalCategoryJourneyAuthority.MOVE_LESSON_TITLE
        NavigationAction.SelectCategory -> GuidedMedicalCategoryJourneyAuthority.OPEN_DIRECT_TITLE
        NavigationAction.SelectPhrase -> GuidedMedicalCategoryJourneyAuthority.sayPhraseLessonTitle(
            uiStrings.language,
            uiStrings
        )
        NavigationAction.CloseMenu -> uiStrings.t("Go Back", "Gaan Terug", "Buyela Emuva")
        NavigationAction.NextPage -> uiStrings.t("Next Page", "Volgende Bladsy", "Ikhasi Elilandelayo")
        NavigationAction.PreviousPage -> uiStrings.t("Previous Page", "Vorige Bladsy", "Ikhasi Elidlule")
        NavigationAction.TriggerEmergency -> uiStrings.t("Emergency", "Nood", "Usizo Oluphuthumayo")
        NavigationAction.AdjustSensitivity ->
            com.idworx.lisa.features.guidedsensitivitylesson.GuidedSensitivityLessonAuthority.LESSON_TITLE
        NavigationAction.ResetSequence -> uiStrings.t("Reset", "Herstel", "Setha Kabusha")
        NavigationAction.OpenMenu,
        NavigationAction.MenuSelectVoice,
        NavigationAction.OpenVoice,
        NavigationAction.BackFromDestination,
        NavigationAction.MenuSelectSettings,
        NavigationAction.OpenSettings,
        NavigationAction.FinishGuidedLearning ->
            com.idworx.lisa.features.explorelisa.ExploreLisaAuthority.LESSON_TITLE
        else -> uiStrings.t("Practice", "Oefen", "Zijwayeze")
    }

    /**
     * Concise instruction under the title for lessons 16–32. Explore LISA steps (including
     * Close Menu / Finish) pass [lessonId] so workspace Back ([NavigationAction.CloseMenu])
     * stays distinct from Explore Close Menu.
     */
    fun lessonCardInstruction(action: NavigationAction, lessonId: String? = null): String? {
        if (lessonId != null &&
            com.idworx.lisa.features.explorelisa.ExploreLisaAuthority.isExploreLessonId(lessonId)
        ) {
            if (action == NavigationAction.CloseMenu &&
                lessonId == com.idworx.lisa.features.explorelisa.ExploreLisaAuthority.ID_CLOSE_MENU
            ) {
                return com.idworx.lisa.features.explorelisa.ExploreLisaAuthority.instructionFor(
                    NavigationAction.CloseMenu
                )
            }
            val text = com.idworx.lisa.features.explorelisa.ExploreLisaAuthority.instructionFor(action)
            return text.takeIf { it.isNotBlank() }
        }
        return when (action) {
            // RC8.19 — Lesson 16 card uses GuidedLessonTeachingPresentation; description only here.
            NavigationAction.MoveToMedicalCategory ->
                GuidedMedicalCategoryJourneyAuthority.MOVE_DESCRIPTION
            NavigationAction.SelectCategory ->
                GuidedMedicalCategoryJourneyAuthority.OPEN_DIRECT_BODY
            NavigationAction.SelectPhrase ->
                GuidedMedicalCategoryJourneyAuthority.SAY_PHRASE_BODY
            NavigationAction.CloseMenu -> "Go back to categories."
            NavigationAction.NextPage -> "Move to the next page."
            NavigationAction.PreviousPage -> "Move to the previous page."
            NavigationAction.TriggerEmergency ->
                "Practice Emergency: arm, confirm, then stop with L1 R1."
            NavigationAction.AdjustSensitivity ->
                com.idworx.lisa.features.guidedsensitivitylesson.GuidedSensitivityLessonAuthority.LESSON_INTRO
            NavigationAction.ResetSequence ->
                "Reset your input sequence."
            NavigationAction.OpenCategories -> "Open Categories."
            else -> null
        }
    }

    fun lessonCardTitleForLesson(
        action: NavigationAction,
        lessonId: String?,
        uiStrings: LisaUiStrings
    ): String {
        if (lessonId != null &&
            com.idworx.lisa.features.explorelisa.ExploreLisaAuthority.isExploreLessonId(lessonId)
        ) {
            return com.idworx.lisa.features.explorelisa.ExploreLisaAuthority.LESSON_TITLE
        }
        return lessonCardTitle(action, uiStrings)
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
        GuidedWorkspaceHighlightTarget.CategoryNextPage,
        GuidedWorkspaceHighlightTarget.CategoryPreviousPage,
        GuidedWorkspaceHighlightTarget.Emergency,
        GuidedWorkspaceHighlightTarget.Select,
        GuidedWorkspaceHighlightTarget.SettingsHubSensitivity,
        GuidedWorkspaceHighlightTarget.IncreaseValue -> GuidedWorkspaceLessonCardDock.BottomStart
        GuidedWorkspaceHighlightTarget.OpenCategories,
        GuidedWorkspaceHighlightTarget.CategoryRow,
        GuidedWorkspaceHighlightTarget.PhraseRow,
        null -> GuidedWorkspaceLessonCardDock.BottomEnd
    }

    /**
     * RC8.14 — Explore LISA card docks away from the production control being practiced
     * (Menu bottom chrome, Voice/Settings rows).
     */
    fun cardDockForLesson(action: NavigationAction, lessonId: String?): GuidedWorkspaceLessonCardDock {
        if (lessonId != null &&
            com.idworx.lisa.features.explorelisa.ExploreLisaAuthority.isExploreLessonId(lessonId)
        ) {
            return when (action) {
                NavigationAction.OpenMenu,
                NavigationAction.CloseMenu,
                NavigationAction.FinishGuidedLearning -> GuidedWorkspaceLessonCardDock.BottomEnd
                NavigationAction.MenuSelectVoice,
                NavigationAction.OpenVoice,
                NavigationAction.MenuSelectSettings,
                NavigationAction.OpenSettings,
                NavigationAction.BackFromDestination -> GuidedWorkspaceLessonCardDock.BottomStart
                else -> GuidedWorkspaceLessonCardDock.BottomEnd
            }
        }
        return cardDockFor(highlightTargetFor(action))
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
        NavigationAction.MoveToMedicalCategory ->
            GuidedMedicalCategoryJourneyAuthority.moveDownSequenceLabel()
        NavigationAction.SelectCategory ->
            // The real category row shows its own direct-shortcut gesture
            // (GuidedCategoryShortcuts.sequenceLabelForCategory) — not the generic Select
            // confirm gesture — so the lesson must show and require exactly that.
            GuidedCategoryShortcuts.sequenceLabelForCategory(medicalCategoryIndex)
        NavigationAction.SelectPhrase ->
            highlightedPhraseGesture
                ?: GuidedMedicalCategoryJourneyAuthority.firstMedicalPhraseEntry().sequenceLabel
        NavigationAction.CloseMenu ->
            formatWinkSequenceShort(GuidedModeNavigation.BACK_LEFT, GuidedModeNavigation.BACK_RIGHT)
        // RC8.26 — real viewport Next/Previous Page (not Move Down/Up L0 R2 / L2 R0).
        NavigationAction.NextPage ->
            formatWinkSequenceShort(
                GuidedModeNavigation.NEXT_CATEGORY_PAGE_LEFT,
                GuidedModeNavigation.NEXT_CATEGORY_PAGE_RIGHT
            )
        NavigationAction.PreviousPage ->
            formatWinkSequenceShort(
                GuidedModeNavigation.PREVIOUS_CATEGORY_PAGE_LEFT,
                GuidedModeNavigation.PREVIOUS_CATEGORY_PAGE_RIGHT
            )
        NavigationAction.TriggerEmergency ->
            formatWinkSequenceShort(EMERGENCY_LEFT_WINKS, EMERGENCY_RIGHT_WINKS)
        NavigationAction.AdjustSensitivity ->
            com.idworx.lisa.features.guidedsensitivitylesson.GuidedSensitivityLessonAuthority
                .openSettingsSequenceLabel()
        NavigationAction.ResetSequence ->
            // Touch-independent by design — the same gesture that finishes training also
            // performs the real workspace Reset action afterward (MainActivity.performReset()).
            formatWinkSequenceShort(GuidedModeNavigation.FINISH_TRAINING_LEFT, GuidedModeNavigation.FINISH_TRAINING_RIGHT)
        NavigationAction.OpenMenu ->
            com.idworx.lisa.features.explorelisa.ExploreLisaAuthority.openMenuSequenceLabel()
        NavigationAction.MenuSelectVoice,
        NavigationAction.MenuSelectSettings ->
            com.idworx.lisa.features.explorelisa.ExploreLisaAuthority.moveDownSequenceLabel()
        NavigationAction.OpenVoice ->
            com.idworx.lisa.features.explorelisa.ExploreLisaAuthority.voiceSequenceLabel()
        NavigationAction.OpenSettings ->
            com.idworx.lisa.features.explorelisa.ExploreLisaAuthority.settingsSequenceLabel()
        NavigationAction.FinishGuidedLearning ->
            com.idworx.lisa.features.explorelisa.ExploreLisaAuthority.finishSequenceLabel()
        NavigationAction.BackFromDestination ->
            com.idworx.lisa.features.explorelisa.ExploreLisaAuthority.backSequenceLabel()
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
