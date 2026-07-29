package com.idworx.lisa

import com.idworx.lisa.features.guidedlessonteaching.GuidedLesson16AssessmentPhase
import com.idworx.lisa.features.guidedlessonteaching.GuidedLessonPhaseAdvanceResult
import com.idworx.lisa.features.guidedlessonteaching.GuidedLessonPhaseEngine
import com.idworx.lisa.features.guidedlessonteaching.GuidedLessonPhaseRequiredAction
import com.idworx.lisa.features.guidedlessonteaching.GuidedLessonTeachingSpec
import com.idworx.lisa.features.guidedmedicalcategoryjourney.GuidedMedicalCategoryJourneyAuthority
import com.idworx.lisa.features.onboardingguide.lessons.TrainingLessonCatalog
import com.idworx.lisa.features.onboardingguide.model.NavigationAction
import com.idworx.lisa.features.onboardingguide.navigation.GuidedWorkspaceHighlightTarget
import com.idworx.lisa.features.zerotouchprinciple.audit.ZeroTouchFileProbe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * RC8.24 / RC8.25 / RC8.34 — Lesson 16 Method 1 requires complete Medical opening via
 * scroll+L1 R1 (OPEN_SELECTED); Method 2 then teaches L3 R1; Lesson 17 re-teaches direct alone.
 */
class Rc8_24Lesson16CompleteMedicalCategoryOpeningTest {

    private val uiStrings = LisaUiStrings.forLanguage(PreferredLanguage.English)
    private val authority = GuidedMedicalCategoryJourneyAuthority

    private fun read(pathUnderMainJava: String): String {
        val path = "app/src/main/java/com/idworx/lisa/$pathUnderMainJava"
        return ZeroTouchFileProbe.readProjectFile(path)
            ?: error("Missing source: $path")
    }

    private fun root(): GuidedNavigationState =
        GuidedNavigationController.communicationWorkspaceRoot(GuidedNavigationState())

    private fun moveDown(state: GuidedNavigationState): GuidedNavigationState =
        (GuidedNavigationController.processSequence(
            GuidedModeNavigation.NEXT_LEFT,
            GuidedModeNavigation.NEXT_RIGHT,
            state,
            PreferredLanguage.English,
            uiStrings
        ) as GuidedSequenceResult.Navigate).newState

    private fun openSelected(state: GuidedNavigationState): GuidedNavigationState =
        (GuidedNavigationController.processSequence(
            GuidedModeNavigation.SELECT_LEFT,
            GuidedModeNavigation.SELECT_RIGHT,
            state,
            PreferredLanguage.English,
            uiStrings
        ) as GuidedSequenceResult.Navigate).newState

    private fun openMedicalDirect(state: GuidedNavigationState): GuidedNavigationState =
        (GuidedNavigationController.processSequence(
            authority.openMedicalGesture().first,
            authority.openMedicalGesture().second,
            state,
            PreferredLanguage.English,
            uiStrings
        ) as GuidedSequenceResult.Navigate).newState

    @Test
    fun startsInScrollToMedicalPhase() {
        val full = GuidedLessonTeachingSpec.fullPresentationFor(
            NavigationAction.MoveToMedicalCategory,
            authority.ID_MOVE_TO_MEDICAL,
            uiStrings
        )
        assertEquals(3, full.phases.size)
        assertEquals(
            GuidedLesson16AssessmentPhase.Part1ScrollToMedical,
            GuidedLesson16AssessmentPhase.fromPhaseId(full.phases[0].id)
        )
        assertEquals(
            GuidedLessonPhaseRequiredAction.MoveDownUntilCategorySelected,
            full.phases[0].requiredAction
        )
        assertFalse(full.phases[0].showCompletionFeedback)
        assertEquals(GuidedWorkspaceHighlightTarget.NextPage, full.phases[0].navigationControlHighlight)
    }

    @Test
    fun l0R2MovesSelectionAndSelectingMedicalDoesNotCompletePart1() {
        var state = root()
        assertTrue(authority.isLesson16StartState(state))
        state = moveDown(state)
        assertEquals(1, state.categoryMenuSelection)
        state = moveDown(state)
        assertTrue(authority.isMedicalSelectedInCategoryMenu(state))
        assertFalse(authority.isMedicalPhraseWorkspaceOpen(state))
        assertFalse(authority.isMedicalOpenedViaSelect(state))

        val scrollAdvance = GuidedLessonPhaseEngine.advanceResult(
            GuidedLessonTeachingSpec.fullPresentationFor(
                NavigationAction.MoveToMedicalCategory, null, uiStrings
            ),
            0
        ) as GuidedLessonPhaseAdvanceResult.IntermediatePhaseCompleted
        assertFalse(scrollAdvance.showCompletionFeedback)
        assertEquals(1, scrollAdvance.nextPhaseIndex)
    }

    @Test
    fun selectingMedicalChangesInstructionAndHighlightToOpenSelected() {
        val openPhase = GuidedLessonTeachingSpec.presentationFor(
            NavigationAction.MoveToMedicalCategory,
            authority.ID_MOVE_TO_MEDICAL,
            uiStrings,
            phaseIndex = 1
        )
        assertEquals(
            GuidedLesson16AssessmentPhase.Part1OpenSelectedMedical,
            GuidedLesson16AssessmentPhase.fromPhaseId(
                GuidedLessonTeachingSpec.phasesFor(NavigationAction.MoveToMedicalCategory)[1].id
            )
        )
        assertTrue(
            openPhase.methods.single().instructionalLines.any {
                it.contains("Medical is selected. Use L1 R1 to open it.")
            }
        )
        assertEquals("L1 R1", openPhase.methods.single().highlightedSequence)
        assertEquals(GuidedWorkspaceHighlightTarget.Select, openPhase.navigationControlHighlight)
        assertEquals(authority.medicalCategoryIndex, openPhase.destinationCategoryIndex)
    }

    @Test
    fun l1R1OpensMedicalViaSelectThenIntermediateWellDoneBeforeMethod2() {
        var state = root()
        repeat(authority.downsFromConversationToMedical()) { state = moveDown(state) }
        val before = state
        state = openSelected(state)
        assertTrue(authority.isMedicalOpenedViaSelect(state))
        assertEquals(CategoryNavigationCause.OPEN_SELECTED, state.categoryNavigationCause)
        assertTrue(
            authority.isMethod1OpenCompleted(
                before,
                state,
                GuidedModeNavigation.SELECT_LEFT,
                GuidedModeNavigation.SELECT_RIGHT
            )
        )

        val openAdvance = GuidedLessonPhaseEngine.advanceResult(
            GuidedLessonTeachingSpec.fullPresentationFor(
                NavigationAction.MoveToMedicalCategory, null, uiStrings
            ),
            1
        )
        assertTrue(openAdvance is GuidedLessonPhaseAdvanceResult.IntermediatePhaseCompleted)
        val intermediate = openAdvance as GuidedLessonPhaseAdvanceResult.IntermediatePhaseCompleted
        assertTrue(intermediate.showCompletionFeedback)
        assertTrue(intermediate.resetWorkspaceBeforeNextPhase)
        assertEquals(2, intermediate.nextPhaseIndex)
        assertEquals(authority.MOVE_PHASE1_FEEDBACK_DETAIL, intermediate.completedPhase.completionFeedbackDetail)
        assertEquals("Well done!", intermediate.completedPhase.completionFeedbackMessage)
    }

    @Test
    fun openingAnotherSelectedCategoryDoesNotCountAsMedicalSuccess() {
        var state = root()
        state = moveDown(state)
        state = openSelected(state)
        assertNotEquals(authority.medicalCategoryIndex, state.categoryIndex)
        assertFalse(authority.isMedicalOpenedViaSelect(state))
    }

    @Test
    fun lesson17DirectOpenIsSeparateFromLesson16() {
        val lesson17 = GuidedLessonTeachingSpec.presentationFor(
            NavigationAction.SelectCategory,
            authority.ID_OPEN_MEDICAL,
            uiStrings
        )
        assertTrue(
            lesson17.methods.single().instructionalLines.any {
                it.contains("Use L3 R1 to open Medical directly.")
            }
        )
        val opened = openMedicalDirect(root())
        assertTrue(authority.isMedicalOpenedViaDirectShortcut(opened))
        assertEquals(
            NavigationAction.SelectCategory,
            TrainingLessonCatalog.navigationLessons[1].action
        )
    }

    @Test
    fun l1R1DoesNotSatisfyLesson17DirectSequenceRequirement() {
        var state = root()
        repeat(authority.downsFromConversationToMedical()) { state = moveDown(state) }
        state = openSelected(state)
        assertTrue(authority.isMedicalOpenedViaSelect(state))
        assertFalse(authority.isMedicalOpenedViaDirectShortcut(state))
    }

    @Test
    fun noSyntheticLessonOnlyOpenAndSelectPanelHighlightWired() {
        val main = read("MainActivity.kt")
        assertTrue(main.contains("isMethod1OpenCompleted") || main.contains("isMedicalOpenedViaSelect"))
        assertTrue(main.contains("isMethod2DirectCompleted") || main.contains("isMedicalOpenedViaDirectShortcut"))
        assertTrue(main.contains("OpenSelectedCategory"))
        val ui = read("LisaGuidedModeUi.kt")
        assertTrue(
            ui.contains(
                "GuidedPanelActionKind.Select -> highlightTarget == GuidedWorkspaceHighlightTarget.Select"
            )
        )
        val mode = read("LisaGuidedMode.kt")
        assertTrue(mode.contains("CategoryNavigationCause.OPEN_SELECTED"))
    }
}
