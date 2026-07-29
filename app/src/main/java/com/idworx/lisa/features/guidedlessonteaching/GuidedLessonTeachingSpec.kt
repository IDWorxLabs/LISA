package com.idworx.lisa.features.guidedlessonteaching

import com.idworx.lisa.LisaUiStrings
import com.idworx.lisa.PreferredLanguage
import com.idworx.lisa.features.guidedlessonexecutionauthority.GuidedLessonExecutionAuthority
import com.idworx.lisa.features.guidedmedicalcategoryjourney.GuidedMedicalCategoryJourneyAuthority
import com.idworx.lisa.features.onboardingguide.model.NavigationAction
import com.idworx.lisa.features.onboardingguide.navigation.GuidedWorkspaceHighlightTarget
import com.idworx.lisa.features.onboardingguide.navigation.GuidedWorkspaceTrainingSpec
import com.idworx.lisa.formatWinkSequenceShort
import com.idworx.lisa.GuidedModeNavigation

/**
 * RC8.19–RC8.25 — Resolves [GuidedLessonTeachingPresentation] for Guided Learning lessons 16–32.
 *
 * RC8.25 catalogue (medical journey):
 * - Lesson 16 (`nav_move_medical`): scroll to Medical → open with L1 R1
 * - Lesson 17 (`nav_select_category`): open Medical directly with L3 R1
 * - Lesson 18 (`nav_select_phrase`): say first Medical phrase
 * - Lesson 19 (`nav_back`): Back to category menu
 */
object GuidedLessonTeachingSpec {

    const val NEXT_ACTION_HEADING: String = "Next Action:"

    fun presentationFor(
        action: NavigationAction,
        lessonId: String?,
        uiStrings: LisaUiStrings,
        highlightedPhraseGesture: String? = null,
        phaseIndex: Int = 0
    ): GuidedLessonTeachingPresentation {
        val full = fullPresentationFor(action, lessonId, uiStrings, highlightedPhraseGesture)
        return full.forPhaseIndex(phaseIndex)
    }

    fun fullPresentationFor(
        action: NavigationAction,
        lessonId: String?,
        uiStrings: LisaUiStrings,
        highlightedPhraseGesture: String? = null
    ): GuidedLessonTeachingPresentation {
        when {
            action == NavigationAction.MoveToMedicalCategory ->
                return lesson16ScrollAndOpenPresentation()
            action == NavigationAction.SelectCategory &&
                (lessonId == null || lessonId == GuidedMedicalCategoryJourneyAuthority.ID_OPEN_MEDICAL) ->
                return lesson17DirectOpenPresentation()
            action == NavigationAction.SelectPhrase &&
                (lessonId == null || lessonId == GuidedMedicalCategoryJourneyAuthority.ID_USE_MEDICAL_PHRASE) ->
                return lesson18SayPhrasePresentation(uiStrings, highlightedPhraseGesture)
            action == NavigationAction.CloseMenu &&
                lessonId == GuidedLessonExecutionAuthority.ID_WORKSPACE_BACK ->
                return lesson19BackPresentation()
        }
        return GuidedLessonTeachingPresentation(
            title = GuidedWorkspaceTrainingSpec.lessonCardTitleForLesson(action, lessonId, uiStrings),
            description = GuidedWorkspaceTrainingSpec.lessonCardInstruction(action, lessonId),
            methods = emptyList(),
            nextActionHeading = null,
            nextActionSteps = emptyList(),
            sequenceEmphasis = null,
            rawGestureLabel = GuidedWorkspaceTrainingSpec.lessonCardGestureLabel(
                action,
                highlightedPhraseGesture
            ),
            navigationControlHighlight = GuidedWorkspaceTrainingSpec.highlightTargetFor(action),
            productionTargetCategoryIndex = null,
            destinationCategoryIndex = null,
            phases = emptyList()
        )
    }

    fun phasesFor(action: NavigationAction): List<GuidedLessonTeachingPhase> =
        fullPresentationFor(
            action,
            lessonId = null,
            uiStrings = LisaUiStrings.forLanguage(PreferredLanguage.English)
        ).phases

    fun activePhase(
        action: NavigationAction,
        phaseIndex: Int
    ): GuidedLessonTeachingPhase? {
        val phases = phasesFor(action)
        if (phases.isEmpty()) return null
        return phases.getOrNull(
            GuidedLessonPhaseEngine.clampPhaseIndex(
                GuidedLessonTeachingPresentation(
                    title = "",
                    rawGestureLabel = "",
                    phases = phases
                ),
                phaseIndex
            )
        )
    }

    fun destinationCategoryIndexFor(action: NavigationAction, phaseIndex: Int = 0): Int? =
        presentationFor(
            action,
            lessonId = null,
            uiStrings = LisaUiStrings.forLanguage(PreferredLanguage.English),
            phaseIndex = phaseIndex
        ).destinationCategoryIndex

    fun productionTargetCategoryIndexFor(action: NavigationAction): Int? =
        when (action) {
            NavigationAction.MoveToMedicalCategory,
            NavigationAction.SelectCategory ->
                GuidedMedicalCategoryJourneyAuthority.medicalCategoryIndex
            else -> null
        }

    private fun lesson16ScrollAndOpenPresentation(): GuidedLessonTeachingPresentation {
        val scrollSequence = GuidedMedicalCategoryJourneyAuthority.moveDownSequenceLabel()
        val selectSequence = GuidedMedicalCategoryJourneyAuthority.openSelectedSequenceLabel()
        val medicalIndex = GuidedMedicalCategoryJourneyAuthority.medicalCategoryIndex

        val scroll = GuidedLessonTeachingPhase(
            id = GuidedMedicalCategoryJourneyAuthority.PHASE_ID_PART1_SCROLL,
            title = GuidedMedicalCategoryJourneyAuthority.MOVE_LESSON_TITLE,
            description = null,
            methods = listOf(
                GuidedLessonTeachingMethod(
                    title = GuidedMedicalCategoryJourneyAuthority.MOVE_METHOD_1_TITLE,
                    instructionalLines = listOf(
                        GuidedMedicalCategoryJourneyAuthority.MOVE_METHOD_1_BODY
                    ),
                    highlightedSequence = scrollSequence
                )
            ),
            rawGestureLabel = scrollSequence,
            navigationControlHighlight = GuidedWorkspaceHighlightTarget.NextPage,
            productionTargetCategoryIndex = medicalIndex,
            destinationCategoryIndex = null,
            requiredAction = GuidedLessonPhaseRequiredAction.MoveDownUntilCategorySelected,
            showCompletionFeedback = false,
            resetWorkspaceBeforeNextPhase = false
        )
        val open = GuidedLessonTeachingPhase(
            id = GuidedMedicalCategoryJourneyAuthority.PHASE_ID_PART1_OPEN,
            title = GuidedMedicalCategoryJourneyAuthority.MOVE_LESSON_TITLE,
            description = null,
            methods = listOf(
                GuidedLessonTeachingMethod(
                    title = GuidedMedicalCategoryJourneyAuthority.MOVE_METHOD_1_OPEN_TITLE,
                    instructionalLines = listOf(
                        GuidedMedicalCategoryJourneyAuthority.MOVE_METHOD_1_OPEN_BODY
                    ),
                    highlightedSequence = selectSequence
                )
            ),
            rawGestureLabel = selectSequence,
            navigationControlHighlight = GuidedWorkspaceHighlightTarget.Select,
            productionTargetCategoryIndex = medicalIndex,
            destinationCategoryIndex = medicalIndex,
            requiredAction = GuidedLessonPhaseRequiredAction.OpenSelectedCategory,
            completionFeedbackMessage = GuidedMedicalCategoryJourneyAuthority.MOVE_PHASE_FEEDBACK_TITLE,
            completionFeedbackDetail = GuidedMedicalCategoryJourneyAuthority.MOVE_PHASE1_FEEDBACK_DETAIL,
            showCompletionFeedback = true,
            resetWorkspaceBeforeNextPhase = false
        )
        return GuidedLessonTeachingPresentation(
            title = GuidedMedicalCategoryJourneyAuthority.MOVE_LESSON_TITLE,
            description = GuidedMedicalCategoryJourneyAuthority.MOVE_DESCRIPTION,
            methods = scroll.methods,
            rawGestureLabel = scrollSequence,
            navigationControlHighlight = GuidedWorkspaceHighlightTarget.NextPage,
            productionTargetCategoryIndex = medicalIndex,
            destinationCategoryIndex = null,
            phases = listOf(scroll, open)
        )
    }

    private fun lesson17DirectOpenPresentation(): GuidedLessonTeachingPresentation {
        val medicalSequence = GuidedMedicalCategoryJourneyAuthority.openMedicalSequenceLabel()
        val medicalIndex = GuidedMedicalCategoryJourneyAuthority.medicalCategoryIndex
        return GuidedLessonTeachingPresentation(
            title = GuidedMedicalCategoryJourneyAuthority.OPEN_DIRECT_TITLE,
            description = null,
            methods = listOf(
                GuidedLessonTeachingMethod(
                    title = GuidedMedicalCategoryJourneyAuthority.MOVE_METHOD_2_TITLE,
                    instructionalLines = listOf(
                        GuidedMedicalCategoryJourneyAuthority.OPEN_DIRECT_BODY
                    ),
                    highlightedSequence = medicalSequence
                )
            ),
            rawGestureLabel = medicalSequence,
            navigationControlHighlight = null,
            productionTargetCategoryIndex = medicalIndex,
            destinationCategoryIndex = medicalIndex,
            phases = emptyList()
        )
    }

    private fun lesson18SayPhrasePresentation(
        uiStrings: LisaUiStrings,
        highlightedPhraseGesture: String?
    ): GuidedLessonTeachingPresentation {
        val entry = GuidedMedicalCategoryJourneyAuthority.firstMedicalPhraseEntry(
            uiStrings.language,
            uiStrings
        )
        val sequence = highlightedPhraseGesture?.takeIf { it.isNotBlank() }
            ?: entry.sequenceLabel
        return GuidedLessonTeachingPresentation(
            title = GuidedMedicalCategoryJourneyAuthority.sayPhraseLessonTitle(
                uiStrings.language,
                uiStrings
            ),
            description = null,
            methods = listOf(
                GuidedLessonTeachingMethod(
                    title = "Say the phrase",
                    instructionalLines = listOf(
                        GuidedMedicalCategoryJourneyAuthority.SAY_PHRASE_BODY
                    ),
                    highlightedSequence = sequence
                )
            ),
            rawGestureLabel = sequence,
            navigationControlHighlight = GuidedWorkspaceHighlightTarget.PhraseRow,
            productionTargetCategoryIndex = GuidedMedicalCategoryJourneyAuthority.medicalCategoryIndex,
            destinationCategoryIndex = null,
            phases = emptyList()
        )
    }

    private fun lesson19BackPresentation(): GuidedLessonTeachingPresentation {
        val backSequence = formatWinkSequenceShort(
            GuidedModeNavigation.BACK_LEFT,
            GuidedModeNavigation.BACK_RIGHT
        )
        return GuidedLessonTeachingPresentation(
            title = "Go Back",
            description = null,
            methods = listOf(
                GuidedLessonTeachingMethod(
                    title = "Back",
                    instructionalLines = listOf(
                        GuidedMedicalCategoryJourneyAuthority.BACK_TO_CATEGORIES_BODY
                    ),
                    highlightedSequence = backSequence
                )
            ),
            rawGestureLabel = backSequence,
            navigationControlHighlight = GuidedWorkspaceHighlightTarget.Back,
            productionTargetCategoryIndex = null,
            destinationCategoryIndex = null,
            phases = emptyList()
        )
    }
}
