package com.idworx.lisa.features.guidedsensitivitylesson

import com.idworx.lisa.GuidedModeNavigation
import com.idworx.lisa.GuidedNavigationState
import com.idworx.lisa.GuidedOverlayScreenMode
import com.idworx.lisa.GuidedPreferencesAdjustMode
import com.idworx.lisa.GuidedVocabularyCategory
import com.idworx.lisa.MAX_SENSITIVITY_LEVEL
import com.idworx.lisa.MIN_SENSITIVITY_LEVEL
import com.idworx.lisa.PreferenceAdjustmentController
import com.idworx.lisa.formatWinkSequenceShort
import com.idworx.lisa.features.guidedlessonteaching.GuidedLessonPhaseRequiredAction
import com.idworx.lisa.features.guidedlessonteaching.GuidedLessonTeachingMethod
import com.idworx.lisa.features.guidedlessonteaching.GuidedLessonTeachingPhase
import com.idworx.lisa.features.guidedlessonteaching.GuidedLessonTeachingPresentation
import com.idworx.lisa.features.onboardingguide.navigation.GuidedWorkspaceHighlightTarget

/**
 * RC8.28 — Final guided lesson: Adjust Sensitivity through production Settings & Controls.
 *
 * Phases:
 * 1. OpenSettingsAndControls — L5 R5
 * 2. OpenSensitivity — L1 R1 (Select Setting)
 * 3. ChangeSensitivity — L1 R3 (Increase once)
 * 4. SaveSensitivity — L1 R1 (begin confirm + confirm save)
 *
 * Preference policy: snapshot the user's saved Sensitivity at lesson entry; after successful
 * save and completion acknowledgement, restore the snapshot so guided practice does not
 * permanently overwrite the established preference.
 */
object GuidedSensitivityLessonAuthority {

    const val ID_ADJUST_SENSITIVITY: String = "nav_adjust_sensitivity"

    const val LESSON_TITLE: String = "Adjust Sensitivity"
    const val LESSON_INTRO: String =
        "You have learned the Communication controls. The same labelled blink sequences work throughout LISA. Let’s practise one adjustment."

    const val TRAINING_COMPLETE_TITLE: String = "Training Complete"
    const val TRAINING_COMPLETE_MESSAGE: String =
        "You now know how to use LISA. Follow the blink sequences shown on each button as you move through the app."
    const val START_USING_LISA_LABEL: String = "Start Using LISA"

    const val PHASE_OPEN_SETTINGS: String = "OpenSettingsAndControls"
    const val PHASE_OPEN_SENSITIVITY: String = "OpenSensitivity"
    const val PHASE_CHANGE_SENSITIVITY: String = "ChangeSensitivity"
    const val PHASE_SAVE_SENSITIVITY: String = "SaveSensitivity"

    fun openSettingsSequenceLabel(): String = formatWinkSequenceShort(
        GuidedModeNavigation.ADJUST_SETTINGS_ENTRY_LEFT,
        GuidedModeNavigation.ADJUST_SETTINGS_ENTRY_RIGHT
    )

    fun openSensitivitySequenceLabel(): String = formatWinkSequenceShort(
        GuidedModeNavigation.SELECT_LEFT,
        GuidedModeNavigation.SELECT_RIGHT
    )

    fun increaseSequenceLabel(): String = formatWinkSequenceShort(
        GuidedModeNavigation.INCREASE_VALUE_LEFT,
        GuidedModeNavigation.INCREASE_VALUE_RIGHT
    )

    fun saveSequenceLabel(): String = openSensitivitySequenceLabel()

    fun matchesOpenSettings(left: Int, right: Int): Boolean =
        GuidedModeNavigation.isAdjustSettingsEntrySequence(left, right)

    fun matchesOpenSensitivity(left: Int, right: Int): Boolean =
        GuidedModeNavigation.isSelectSequence(left, right)

    fun matchesIncrease(left: Int, right: Int): Boolean =
        GuidedModeNavigation.isIncreaseValueSequence(left, right)

    fun matchesSave(left: Int, right: Int): Boolean =
        GuidedModeNavigation.isSelectSequence(left, right)

    fun isSettingsHubOpen(state: GuidedNavigationState): Boolean =
        state.preferencesAdjustMode == GuidedPreferencesAdjustMode.SettingsMenu

    fun isSensitivityAdjustmentOpen(state: GuidedNavigationState): Boolean =
        state.preferencesAdjustMode == GuidedPreferencesAdjustMode.Sensitivity

    fun isSaveConfirmationOpen(state: GuidedNavigationState): Boolean =
        state.preferencesAdjustMode == GuidedPreferencesAdjustMode.ConfirmSaveSensitivity

    fun isCategoryMenuStartState(state: GuidedNavigationState): Boolean =
        state.screenMode == GuidedOverlayScreenMode.CategoryMenu &&
            state.preferencesAdjustMode == GuidedPreferencesAdjustMode.None

    /**
     * Ensure the lesson can increase Sensitivity once. If already at max, return one step below
     * max for temporary practice — caller applies this as non-persistent prep.
     */
    fun practiceStartingSensitivity(savedLevel: Int): Int {
        val coerced = savedLevel.coerceIn(MIN_SENSITIVITY_LEVEL, MAX_SENSITIVITY_LEVEL)
        return if (coerced >= MAX_SENSITIVITY_LEVEL) {
            (MAX_SENSITIVITY_LEVEL - 1).coerceAtLeast(MIN_SENSITIVITY_LEVEL)
        } else {
            coerced
        }
    }

    fun expectedSensitivityAfterIncrease(startLevel: Int): Int =
        (startLevel + 1).coerceIn(MIN_SENSITIVITY_LEVEL, MAX_SENSITIVITY_LEVEL)

    fun isIncreaseCompleted(beforeDraft: Int, afterDraft: Int, startLevel: Int): Boolean =
        afterDraft == expectedSensitivityAfterIncrease(startLevel) &&
            afterDraft == beforeDraft + 1

    fun teachingPresentation(): GuidedLessonTeachingPresentation {
        val openSettings = GuidedLessonTeachingPhase(
            id = PHASE_OPEN_SETTINGS,
            title = LESSON_TITLE,
            description = LESSON_INTRO,
            methods = listOf(
                GuidedLessonTeachingMethod(
                    title = "Open Settings",
                    instructionalLines = listOf("Use L5 R5 to open Settings & Controls."),
                    highlightedSequence = openSettingsSequenceLabel()
                )
            ),
            rawGestureLabel = openSettingsSequenceLabel(),
            navigationControlHighlight = GuidedWorkspaceHighlightTarget.CategoryRow,
            destinationCategoryIndex = GuidedVocabularyCategory.ADJUST_SETTINGS_INDEX,
            requiredAction = GuidedLessonPhaseRequiredAction.OpenSettingsAndControls,
            showCompletionFeedback = false
        )
        val openSensitivity = GuidedLessonTeachingPhase(
            id = PHASE_OPEN_SENSITIVITY,
            title = LESSON_TITLE,
            description = "Sensitivity is selected. Use L1 R1 to open it.",
            methods = listOf(
                GuidedLessonTeachingMethod(
                    title = "Open Sensitivity",
                    instructionalLines = listOf("Sensitivity is selected. Use L1 R1 to open it."),
                    highlightedSequence = openSensitivitySequenceLabel()
                )
            ),
            rawGestureLabel = openSensitivitySequenceLabel(),
            navigationControlHighlight = GuidedWorkspaceHighlightTarget.Select,
            requiredAction = GuidedLessonPhaseRequiredAction.OpenSensitivitySetting,
            showCompletionFeedback = false
        )
        val change = GuidedLessonTeachingPhase(
            id = PHASE_CHANGE_SENSITIVITY,
            title = LESSON_TITLE,
            description = "Use L1 R3 to increase Sensitivity once.",
            methods = listOf(
                GuidedLessonTeachingMethod(
                    title = "Increase",
                    instructionalLines = listOf("Use L1 R3 to increase Sensitivity once."),
                    highlightedSequence = increaseSequenceLabel()
                )
            ),
            rawGestureLabel = increaseSequenceLabel(),
            navigationControlHighlight = GuidedWorkspaceHighlightTarget.IncreaseValue,
            requiredAction = GuidedLessonPhaseRequiredAction.IncreaseSensitivityOnce,
            showCompletionFeedback = false
        )
        val save = GuidedLessonTeachingPhase(
            id = PHASE_SAVE_SENSITIVITY,
            title = LESSON_TITLE,
            description = "Use L1 R1 to save the selected Sensitivity.",
            methods = listOf(
                GuidedLessonTeachingMethod(
                    title = "Save",
                    instructionalLines = listOf("Use L1 R1 to save the selected Sensitivity."),
                    highlightedSequence = saveSequenceLabel()
                )
            ),
            rawGestureLabel = saveSequenceLabel(),
            navigationControlHighlight = GuidedWorkspaceHighlightTarget.Select,
            requiredAction = GuidedLessonPhaseRequiredAction.SaveSensitivity,
            showCompletionFeedback = true,
            completionFeedbackMessage = "Well done!",
            completionFeedbackDetail = TRAINING_COMPLETE_MESSAGE
        )
        return GuidedLessonTeachingPresentation(
            title = LESSON_TITLE,
            description = LESSON_INTRO,
            methods = openSettings.methods,
            rawGestureLabel = openSettingsSequenceLabel(),
            navigationControlHighlight = GuidedWorkspaceHighlightTarget.CategoryRow,
            destinationCategoryIndex = GuidedVocabularyCategory.ADJUST_SETTINGS_INDEX,
            phases = listOf(openSettings, openSensitivity, change, save)
        )
    }

    /** Production open of Settings hub (same as L5 R5 / Adjust Settings entry). */
    fun openSettingsMenu(state: GuidedNavigationState): GuidedNavigationState =
        PreferenceAdjustmentController.openSettingsMenu(state)
}
