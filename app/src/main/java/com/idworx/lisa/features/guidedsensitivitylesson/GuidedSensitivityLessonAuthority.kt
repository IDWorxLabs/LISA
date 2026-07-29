package com.idworx.lisa.features.guidedsensitivitylesson

import com.idworx.lisa.CategoryNavigationCause
import com.idworx.lisa.GuidedModeNavigation
import com.idworx.lisa.GuidedNavigationState
import com.idworx.lisa.GuidedOverlayScreenMode
import com.idworx.lisa.GuidedPreferencesAdjustMode
import com.idworx.lisa.GuidedVocabularyCategory
import com.idworx.lisa.MAX_SENSITIVITY_LEVEL
import com.idworx.lisa.MIN_SENSITIVITY_LEVEL
import com.idworx.lisa.PreferenceAdjustmentController
import com.idworx.lisa.SettingsAndControlsHubSequences
import com.idworx.lisa.formatWinkSequenceShort
import com.idworx.lisa.features.adjustmentcommitpolicy.AdjustmentCommitPolicyAuthority
import com.idworx.lisa.features.guidedcategorypagenavigation.CategoryPageNavigationAuthority
import com.idworx.lisa.features.guidedlessonteaching.GuidedLessonPhaseRequiredAction
import com.idworx.lisa.features.guidedlessonteaching.GuidedLessonTeachingPhase
import com.idworx.lisa.features.guidedlessonteaching.GuidedLessonTeachingPresentation
import com.idworx.lisa.features.onboardingguide.navigation.GuidedWorkspaceHighlightTarget

/**
 * RC8.32 — Final guided lesson: complete Settings adjustment journey through production UI.
 *
 * Phases (visible lesson remains Lesson 23 of 23 throughout):
 * 1. MoveToSettingsPage — L0 R4 to Page 2
 * 2. OpenSettingsAndControls — L5 R5
 * 3. OpenSensitivity — L2 R0 (labelled Sensitivity card direct open)
 * 4. AdjustSensitivity — L3 R1 decrease or L1 R3 increase (IMMEDIATE_SAVE)
 * 5. ReturnToSettingsAndControls — L2 R2 Back
 *
 * Preference policy: snapshot saved Sensitivity at lesson entry; after Well done
 * acknowledgement / before Start Communicating or Restart Guided Learning, restore the
 * snapshot so guided practice does not permanently overwrite the established preference.
 */
object GuidedSensitivityLessonAuthority {

    const val ID_ADJUST_SENSITIVITY: String = "nav_adjust_sensitivity"

    const val LESSON_TITLE: String = "Adjust Settings"
    /** Visible catalogue / Well done still refer to the Sensitivity assessment. */
    const val LESSON_ASSESSMENT_TITLE: String = "Adjust Sensitivity"
    const val LESSON_CONTEXT: String =
        "You will learn how to adjust settings such as Sensitivity and Response Time. We will use Sensitivity as the example."
    const val LESSON_INTRO: String = LESSON_CONTEXT

    const val TRAINING_COMPLETE_TITLE: String = "Training Complete"
    const val TRAINING_COMPLETE_MESSAGE: String =
        "You now know how to use LISA. Follow the blink sequences shown on each button as you move through the app."
    const val TRAINING_COMPLETE_ADJUSTMENT_HINT: String =
        "You can use the same adjustment flow for Sensitivity, Response Time, Speech Volume and Speech Speed."
    const val START_COMMUNICATING_LABEL: String = "Start Communicating"
    const val RESTART_GUIDED_LEARNING_LABEL: String = "Restart Guided Learning"
    /** @deprecated RC8.32 — use [START_COMMUNICATING_LABEL]. */
    const val START_USING_LISA_LABEL: String = START_COMMUNICATING_LABEL

    const val PHASE_MOVE_TO_SETTINGS_PAGE: String = "MoveToSettingsPage"
    const val PHASE_OPEN_SETTINGS: String = "OpenSettingsAndControls"
    const val PHASE_OPEN_SENSITIVITY: String = "OpenSensitivity"
    const val PHASE_ADJUST_SENSITIVITY: String = "AdjustSensitivity"
    const val PHASE_RETURN_TO_SETTINGS: String = "ReturnToSettingsAndControls"
    /** @deprecated RC8.32 — renamed to [PHASE_ADJUST_SENSITIVITY]. */
    const val PHASE_CHANGE_SENSITIVITY: String = PHASE_ADJUST_SENSITIVITY

    const val PHASE1_TITLE: String = "Adjust Settings"
    const val PHASE2_TITLE: String = "Open Settings & Controls"
    const val PHASE3_TITLE: String = "Open Sensitivity"
    const val PHASE4_TITLE: String = "Adjust Sensitivity"
    const val PHASE5_TITLE: String = "Return to Settings"

    const val PHASE1_INSTRUCTION: String = "Use L0 R4 to move to the next page."
    const val PHASE2_INSTRUCTION: String = "Use L5 R5 to open Settings & Controls."
    const val PHASE3_INSTRUCTION: String = "Use L2 R0 to open Sensitivity."
    const val PHASE4_INSTRUCTION: String =
        "Use L3 R1 to decrease or L1 R3 to increase Sensitivity."
    const val PHASE5_INSTRUCTION: String = "Use L2 R2 to go back to Settings & Controls."

    const val PHASE4_DECREASE_SEQUENCE_LABEL: String = "Decrease: L3 R1"
    const val PHASE4_INCREASE_SEQUENCE_LABEL: String = "Increase: L1 R3"

    const val WELL_DONE_TITLE: String = "✓ Well done!"
    const val COMPLETION_DETAIL: String =
        "You adjusted Sensitivity and returned to Settings & Controls. " +
            "Changes save automatically. Use the same flow for Response Time, Speech Volume " +
            "and Speech Speed — open a setting, use L3 R1 or L1 R3 to adjust, then L2 R2 to go back."

    /** @deprecated RC8.32 — use [COMPLETION_DETAIL]. */
    const val INCREASE_COMPLETION_DETAIL: String = COMPLETION_DETAIL

    fun moveToSettingsPageSequenceLabel(): String =
        CategoryPageNavigationAuthority.nextPageSequenceLabel()

    fun openSettingsSequenceLabel(): String = formatWinkSequenceShort(
        GuidedModeNavigation.ADJUST_SETTINGS_ENTRY_LEFT,
        GuidedModeNavigation.ADJUST_SETTINGS_ENTRY_RIGHT
    )

    fun openSensitivitySequenceLabel(): String = formatWinkSequenceShort(
        SettingsAndControlsHubSequences.SENSITIVITY.first,
        SettingsAndControlsHubSequences.SENSITIVITY.second
    )

    fun decreaseSequenceLabel(): String = AdjustmentCommitPolicyAuthority.decreaseSequenceLabel()

    fun increaseSequenceLabel(): String = AdjustmentCommitPolicyAuthority.increaseSequenceLabel()

    fun adjustSequencesLabel(): String =
        "$PHASE4_DECREASE_SEQUENCE_LABEL\n$PHASE4_INCREASE_SEQUENCE_LABEL"

    /**
     * RC8.33 — visible instruction strings for a phase (context + instruction), each unique.
     * Used by tests to prove the card model does not duplicate the next-action sentence.
     */
    fun visibleInstructionLinesForPhase(phaseIndex: Int): List<String> {
        val phase = teachingPresentation().phases.getOrNull(phaseIndex) ?: return emptyList()
        return buildList {
            phase.context?.takeIf { it.isNotBlank() }?.let { add(it) }
            phase.description?.takeIf { it.isNotBlank() }?.let { add(it) }
            phase.methods.forEach { method ->
                method.instructionalLines.forEach { line ->
                    if (line.isNotBlank() && line != phase.description && line != phase.context) {
                        add(line)
                    }
                }
            }
        }
    }

    fun instructionAppearsOnce(phaseIndex: Int, instruction: String): Boolean {
        val phase = teachingPresentation().phases.getOrNull(phaseIndex) ?: return false
        var count = 0
        if (phase.context == instruction) count++
        if (phase.description == instruction) count++
        phase.methods.forEach { method ->
            if (method.title == instruction) count++
            method.instructionalLines.forEach { if (it == instruction) count++ }
        }
        return count == 1
    }

    fun backSequenceLabel(): String = AdjustmentCommitPolicyAuthority.backSequenceLabel()

    fun startCommunicatingSequenceLabel(): String = formatWinkSequenceShort(
        GuidedModeNavigation.FINISH_TRAINING_LEFT,
        GuidedModeNavigation.FINISH_TRAINING_RIGHT
    )

    fun restartGuidedLearningSequenceLabel(): String = formatWinkSequenceShort(
        GuidedModeNavigation.CATEGORIES_LEFT,
        GuidedModeNavigation.CATEGORIES_RIGHT
    )

    fun matchesMoveToSettingsPage(left: Int, right: Int): Boolean =
        CategoryPageNavigationAuthority.matchesNextPage(left, right)

    fun matchesOpenSettings(left: Int, right: Int): Boolean =
        GuidedModeNavigation.isAdjustSettingsEntrySequence(left, right)

    fun matchesOpenSensitivity(left: Int, right: Int): Boolean =
        left == SettingsAndControlsHubSequences.SENSITIVITY.first &&
            right == SettingsAndControlsHubSequences.SENSITIVITY.second

    fun matchesDecrease(left: Int, right: Int): Boolean =
        GuidedModeNavigation.isDecreaseValueSequence(left, right)

    fun matchesIncrease(left: Int, right: Int): Boolean =
        GuidedModeNavigation.isIncreaseValueSequence(left, right)

    fun matchesAdjust(left: Int, right: Int): Boolean =
        matchesDecrease(left, right) || matchesIncrease(left, right)

    fun matchesReturnToSettings(left: Int, right: Int): Boolean =
        GuidedModeNavigation.isBackSequence(left, right)

    fun matchesStartCommunicating(left: Int, right: Int): Boolean =
        GuidedModeNavigation.isFinishTrainingSequence(left, right)

    fun matchesRestartGuidedLearning(left: Int, right: Int): Boolean =
        GuidedModeNavigation.isCategoriesSequence(left, right)

    fun isSettingsHubOpen(state: GuidedNavigationState): Boolean =
        state.preferencesAdjustMode == GuidedPreferencesAdjustMode.SettingsMenu

    fun isSensitivityAdjustmentOpen(state: GuidedNavigationState): Boolean =
        state.preferencesAdjustMode == GuidedPreferencesAdjustMode.Sensitivity

    fun isCategoryMenuStartState(state: GuidedNavigationState): Boolean =
        state.screenMode == GuidedOverlayScreenMode.CategoryMenu &&
            state.preferencesAdjustMode == GuidedPreferencesAdjustMode.None

    /** Lesson 23 begins on Category Menu Page 1 of a multi-page menu (Settings on Page 2). */
    fun isMoveToSettingsPageStartState(state: GuidedNavigationState): Boolean =
        CategoryPageNavigationAuthority.isNextPageStartState(state)

    fun isSettingsVisibleOnCategoryPage(state: GuidedNavigationState): Boolean =
        isCategoryMenuStartState(state) &&
            state.categoryViewportPage == 1 &&
            CategoryPageNavigationAuthority.displayPageNumber(state) == 2 &&
            state.categoryMenuSelection == GuidedVocabularyCategory.ADJUST_SETTINGS_INDEX

    fun isMoveToSettingsPageCompleted(
        before: GuidedNavigationState,
        after: GuidedNavigationState,
        left: Int,
        right: Int
    ): Boolean {
        if (!matchesMoveToSettingsPage(left, right)) return false
        if (!CategoryPageNavigationAuthority.isNextPageCompleted(before, after, left, right)) {
            return false
        }
        return isSettingsVisibleOnCategoryPage(after) ||
            (
                after.categoryNavigationCause == CategoryNavigationCause.PAGE_MOVEMENT &&
                    after.categoryViewportPage == 1 &&
                    isCategoryMenuStartState(after)
                )
    }

    /**
     * Ensure at least one adjustment direction can succeed. Prep only when a single-level
     * range would otherwise block both directions (not applicable for 1..10). At max/min,
     * the opposite direction remains valid without prep; keep mid-range unchanged.
     */
    fun practiceStartingSensitivity(savedLevel: Int): Int =
        savedLevel.coerceIn(MIN_SENSITIVITY_LEVEL, MAX_SENSITIVITY_LEVEL)

    fun expectedSensitivityAfterIncrease(startLevel: Int): Int =
        (startLevel + 1).coerceIn(MIN_SENSITIVITY_LEVEL, MAX_SENSITIVITY_LEVEL)

    fun expectedSensitivityAfterDecrease(startLevel: Int): Int =
        (startLevel - 1).coerceIn(MIN_SENSITIVITY_LEVEL, MAX_SENSITIVITY_LEVEL)

    fun isIncreaseCompleted(beforeDraft: Int, afterDraft: Int, startLevel: Int): Boolean =
        afterDraft == expectedSensitivityAfterIncrease(startLevel) &&
            afterDraft == beforeDraft + 1

    fun isDecreaseCompleted(beforeDraft: Int, afterDraft: Int, startLevel: Int): Boolean =
        afterDraft == expectedSensitivityAfterDecrease(startLevel) &&
            afterDraft == beforeDraft - 1

    /** Either valid one-step adjustment; boundary no-ops fail. */
    fun isAdjustCompleted(beforeDraft: Int, afterDraft: Int): Boolean =
        afterDraft == beforeDraft + 1 || afterDraft == beforeDraft - 1

    fun teachingPresentation(): GuidedLessonTeachingPresentation {
        // RC8.33 — one instruction per phase: context (phase 1 only) + description instruction,
        // empty methods so the card does not re-render the same sentence under a method title.
        val movePage = GuidedLessonTeachingPhase(
            id = PHASE_MOVE_TO_SETTINGS_PAGE,
            title = PHASE1_TITLE,
            context = LESSON_CONTEXT,
            description = PHASE1_INSTRUCTION,
            methods = emptyList(),
            rawGestureLabel = moveToSettingsPageSequenceLabel(),
            navigationControlHighlight = GuidedWorkspaceHighlightTarget.CategoryNextPage,
            requiredAction = GuidedLessonPhaseRequiredAction.MoveToSettingsPage,
            showCompletionFeedback = false
        )
        val openSettings = GuidedLessonTeachingPhase(
            id = PHASE_OPEN_SETTINGS,
            title = PHASE2_TITLE,
            description = PHASE2_INSTRUCTION,
            methods = emptyList(),
            rawGestureLabel = openSettingsSequenceLabel(),
            navigationControlHighlight = GuidedWorkspaceHighlightTarget.CategoryRow,
            destinationCategoryIndex = GuidedVocabularyCategory.ADJUST_SETTINGS_INDEX,
            requiredAction = GuidedLessonPhaseRequiredAction.OpenSettingsAndControls,
            showCompletionFeedback = false
        )
        val openSensitivity = GuidedLessonTeachingPhase(
            id = PHASE_OPEN_SENSITIVITY,
            title = PHASE3_TITLE,
            description = PHASE3_INSTRUCTION,
            methods = emptyList(),
            rawGestureLabel = openSensitivitySequenceLabel(),
            navigationControlHighlight = GuidedWorkspaceHighlightTarget.SettingsHubSensitivity,
            requiredAction = GuidedLessonPhaseRequiredAction.OpenSensitivitySetting,
            showCompletionFeedback = false
        )
        val adjust = GuidedLessonTeachingPhase(
            id = PHASE_ADJUST_SENSITIVITY,
            title = PHASE4_TITLE,
            description = PHASE4_INSTRUCTION,
            methods = emptyList(),
            rawGestureLabel = adjustSequencesLabel(),
            navigationControlHighlight = GuidedWorkspaceHighlightTarget.IncreaseOrDecreaseValue,
            requiredAction = GuidedLessonPhaseRequiredAction.AdjustSensitivity,
            showCompletionFeedback = false
        )
        val returnToSettings = GuidedLessonTeachingPhase(
            id = PHASE_RETURN_TO_SETTINGS,
            title = PHASE5_TITLE,
            description = PHASE5_INSTRUCTION,
            methods = emptyList(),
            rawGestureLabel = backSequenceLabel(),
            navigationControlHighlight = GuidedWorkspaceHighlightTarget.Back,
            requiredAction = GuidedLessonPhaseRequiredAction.ReturnToSettingsAndControls,
            showCompletionFeedback = true,
            completionFeedbackMessage = WELL_DONE_TITLE,
            completionFeedbackDetail = COMPLETION_DETAIL
        )
        return GuidedLessonTeachingPresentation(
            title = PHASE1_TITLE,
            context = LESSON_CONTEXT,
            description = PHASE1_INSTRUCTION,
            methods = emptyList(),
            rawGestureLabel = moveToSettingsPageSequenceLabel(),
            navigationControlHighlight = GuidedWorkspaceHighlightTarget.CategoryNextPage,
            destinationCategoryIndex = GuidedVocabularyCategory.ADJUST_SETTINGS_INDEX,
            phases = listOf(movePage, openSettings, openSensitivity, adjust, returnToSettings)
        )
    }

    /** Production open of Settings hub (same as L5 R5 / Adjust Settings entry). */
    fun openSettingsMenu(state: GuidedNavigationState): GuidedNavigationState =
        PreferenceAdjustmentController.openSettingsMenu(state)
}
