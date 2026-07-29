package com.idworx.lisa.features.guidedlessonexecutionauthority

import com.idworx.lisa.GuidedNavigationState
import com.idworx.lisa.GuidedOverlayScreenMode
import com.idworx.lisa.features.guidedmedicalcategoryjourney.GuidedMedicalCategoryJourneyAuthority
import com.idworx.lisa.features.onboardingguide.model.NavigationAction

/**
 * RC8.18 — Global Guided Learning lesson-execution authority.
 *
 * Every navigation lesson must:
 * 1. Begin in the correct production state (matching what the learner sees)
 * 2. Wait for the production blink/touch sequence
 * 3. Execute only through production controllers
 * 4. Verify the production state changed as taught
 * 5. Only then advance
 *
 * Lessons must never auto-open, auto-close, or auto-navigate the action being taught.
 */
object GuidedLessonExecutionAuthority {

    const val ID_WORKSPACE_BACK: String = "nav_back"

    /** Max fraction of the workspace content region a lesson card may occupy. */
    const val LESSON_CARD_MAX_HEIGHT_FRACTION: Float = 0.45f

    /**
     * True when [after] reflects a successful production Back from an open category phrase
     * list into Category Selection — the taught outcome of lesson 19.
     */
    fun isWorkspaceBackCompleted(
        before: GuidedNavigationState,
        after: GuidedNavigationState
    ): Boolean =
        before.screenMode == GuidedOverlayScreenMode.Vocabulary &&
            after.screenMode == GuidedOverlayScreenMode.CategoryMenu &&
            after.preferencesAdjustMode == before.preferencesAdjustMode

    /** Lesson 19 must begin with an open category phrase workspace (Medical after lessons 16–18). */
    fun isWorkspaceBackStartState(state: GuidedNavigationState): Boolean =
        GuidedMedicalCategoryJourneyAuthority.isMedicalPhraseWorkspaceOpen(state)

    /**
     * Entry preparation may restore a prior lesson's completed destination so the learner sees
     * the correct starting UI — but must never execute the action the current lesson teaches.
     */
    fun mayRestorePreconditionOnEntry(action: NavigationAction): Boolean = when (action) {
        // Setup only: land on Category Selection / Medical selection / Medical phrases.
        NavigationAction.MoveToMedicalCategory,
        NavigationAction.SelectCategory,
        NavigationAction.SelectPhrase,
        NavigationAction.CloseMenu -> true
        // These lessons teach opening/closing/moving — never pre-run them on entry.
        NavigationAction.OpenMenu,
        NavigationAction.OpenVoice,
        NavigationAction.OpenSettings,
        NavigationAction.OpenCategories,
        NavigationAction.NextPage,
        NavigationAction.PreviousPage,
        NavigationAction.TriggerEmergency,
        NavigationAction.ResetSequence,
        NavigationAction.FinishGuidedLearning,
        NavigationAction.BackFromDestination,
        NavigationAction.MenuSelectVoice,
        NavigationAction.MenuSelectSettings -> false
        else -> false
    }

    /** Completion must be driven by production state, not gesture recognition alone. */
    fun requiresProductionStateGate(action: NavigationAction): Boolean = when (action) {
        NavigationAction.MoveToMedicalCategory,
        NavigationAction.SelectCategory,
        NavigationAction.SelectPhrase,
        NavigationAction.CloseMenu,
        NavigationAction.MenuSelectVoice,
        NavigationAction.MenuSelectSettings,
        NavigationAction.OpenVoice,
        NavigationAction.OpenSettings,
        NavigationAction.BackFromDestination,
        NavigationAction.TriggerEmergency -> true
        else -> false
    }
}
