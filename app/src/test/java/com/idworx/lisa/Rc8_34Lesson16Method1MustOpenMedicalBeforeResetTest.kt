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
import com.idworx.lisa.features.onboardingguide.services.TrainingSessionController
import com.idworx.lisa.features.zerotouchprinciple.audit.ZeroTouchFileProbe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * RC8.34 — Lesson 16 Method 1 completes only after Medical is visibly open via OPEN_SELECTED;
 * Well done keeps Medical open; reset then Method 2 (DIRECT_SHORTCUT) completes Lesson 16.
 */
class Rc8_34Lesson16Method1MustOpenMedicalBeforeResetTest {

    private val uiStrings = LisaUiStrings.forLanguage(PreferredLanguage.English)
    private val authority = GuidedMedicalCategoryJourneyAuthority

    private fun read(pathUnderMainJava: String): String {
        val path = "app/src/main/java/com/idworx/lisa/$pathUnderMainJava"
        return ZeroTouchFileProbe.readProjectFile(path)
            ?: error("Missing source: $path")
    }

    private fun root() =
        GuidedNavigationController.communicationWorkspaceRoot(GuidedNavigationState())

    private fun moveDown(state: GuidedNavigationState) =
        (GuidedNavigationController.processSequence(
            GuidedModeNavigation.NEXT_LEFT,
            GuidedModeNavigation.NEXT_RIGHT,
            state,
            PreferredLanguage.English,
            uiStrings
        ) as GuidedSequenceResult.Navigate).newState

    private fun openSelected(state: GuidedNavigationState) =
        (GuidedNavigationController.processSequence(
            GuidedModeNavigation.SELECT_LEFT,
            GuidedModeNavigation.SELECT_RIGHT,
            state,
            PreferredLanguage.English,
            uiStrings
        ) as GuidedSequenceResult.Navigate).newState

    private fun openMedicalDirect(state: GuidedNavigationState) =
        (GuidedNavigationController.processSequence(
            authority.openMedicalGesture().first,
            authority.openMedicalGesture().second,
            state,
            PreferredLanguage.English,
            uiStrings
        ) as GuidedSequenceResult.Navigate).newState

    private fun fullLesson16() = GuidedLessonTeachingSpec.fullPresentationFor(
        NavigationAction.MoveToMedicalCategory,
        authority.ID_MOVE_TO_MEDICAL,
        uiStrings
    )

    // --- Method 1 structure ---------------------------------------------------------------------

    @Test
    fun lesson16StartsInMethod1ScrollToMedical() {
        val full = fullLesson16()
        assertEquals(3, full.phases.size)
        assertEquals(
            GuidedLesson16AssessmentPhase.Method1ScrollToMedical,
            GuidedLesson16AssessmentPhase.fromPhaseId(full.phases[0].id)
        )
        assertEquals(
            GuidedLessonPhaseRequiredAction.MoveDownUntilCategorySelected,
            full.phases[0].requiredAction
        )
        assertEquals(GuidedWorkspaceHighlightTarget.NextPage, full.phases[0].navigationControlHighlight)
        assertNull(full.phases[0].destinationCategoryIndex)
        assertFalse(full.phases[0].showCompletionFeedback)
    }

    @Test
    fun l0R2UsesProductionMoveDownUntilMedicalSelected() {
        var state = root()
        assertTrue(authority.isLesson16StartState(state))
        repeat(authority.downsFromConversationToMedical()) {
            state = moveDown(state)
            assertEquals(CategoryNavigationCause.ITEM_MOVEMENT, state.categoryNavigationCause)
        }
        assertTrue(authority.isMedicalSelectedInCategoryMenu(state))
        assertFalse(authority.isMedicalPhraseWorkspaceOpen(state))
    }

    @Test
    fun medicalSelectionAdvancesSilentlyAndDoesNotCompleteMethod1() {
        val afterScroll = GuidedLessonPhaseEngine.advanceResult(fullLesson16(), 0)
            as GuidedLessonPhaseAdvanceResult.IntermediatePhaseCompleted
        assertFalse(afterScroll.showCompletionFeedback)
        assertFalse(afterScroll.resetWorkspaceBeforeNextPhase)
        assertEquals(1, afterScroll.nextPhaseIndex)

        var state = root()
        repeat(authority.downsFromConversationToMedical()) { state = moveDown(state) }
        assertFalse(
            authority.isMethod1OpenCompleted(
                before = state,
                after = state,
                left = GuidedModeNavigation.SELECT_LEFT,
                right = GuidedModeNavigation.SELECT_RIGHT
            )
        )
        assertFalse(authority.isMedicalOpenedViaSelect(state))
    }

    @Test
    fun openPhaseCardAndHighlightsRequireL1R1() {
        val open = GuidedLessonTeachingSpec.presentationFor(
            NavigationAction.MoveToMedicalCategory,
            authority.ID_MOVE_TO_MEDICAL,
            uiStrings,
            phaseIndex = 1
        )
        assertEquals(
            GuidedLesson16AssessmentPhase.Method1OpenSelectedMedical,
            GuidedLesson16AssessmentPhase.fromPhaseId(fullLesson16().phases[1].id)
        )
        assertEquals(
            GuidedLessonPhaseRequiredAction.OpenSelectedCategory,
            fullLesson16().phases[1].requiredAction
        )
        assertTrue(
            open.methods.single().instructionalLines.any {
                it.contains("Medical is selected. Use L1 R1 to open it.")
            }
        )
        assertEquals("L1 R1", open.methods.single().highlightedSequence)
        assertEquals(GuidedWorkspaceHighlightTarget.Select, open.navigationControlHighlight)
        assertEquals(authority.medicalCategoryIndex, open.destinationCategoryIndex)
    }

    // --- Method 1 completion gate ---------------------------------------------------------------

    @Test
    fun method1CompletesOnlyAfterVisibleOpenSelectedMedical() {
        var before = root()
        repeat(authority.downsFromConversationToMedical()) { before = moveDown(before) }
        val after = openSelected(before)

        assertTrue(authority.isMedicalOpenedViaSelect(after))
        assertEquals(CategoryNavigationCause.OPEN_SELECTED, after.categoryNavigationCause)
        assertTrue(authority.isMedicalPhraseWorkspaceOpen(after))
        assertTrue(
            authority.isMethod1OpenCompleted(
                before,
                after,
                GuidedModeNavigation.SELECT_LEFT,
                GuidedModeNavigation.SELECT_RIGHT
            )
        )

        val advance = GuidedLessonPhaseEngine.advanceResult(fullLesson16(), 1)
            as GuidedLessonPhaseAdvanceResult.IntermediatePhaseCompleted
        assertTrue(advance.showCompletionFeedback)
        assertTrue(advance.resetWorkspaceBeforeNextPhase)
        assertEquals(2, advance.nextPhaseIndex)
        assertEquals(authority.MOVE_PHASE_FEEDBACK_TITLE, advance.completedPhase.completionFeedbackMessage)
        assertEquals(
            "You selected Medical and opened it using L1 R1.",
            advance.completedPhase.completionFeedbackDetail
        )
    }

    @Test
    fun anotherCategoryOpenDoesNotCompleteMethod1() {
        var before = root()
        before = moveDown(before) // leave Conversation → next category, not Medical
        val after = openSelected(before)
        assertNotEquals(authority.medicalCategoryIndex, after.categoryIndex)
        assertFalse(
            authority.isMethod1OpenCompleted(
                before,
                after,
                GuidedModeNavigation.SELECT_LEFT,
                GuidedModeNavigation.SELECT_RIGHT
            )
        )
    }

    @Test
    fun l3R1DoesNotSatisfyMethod1OpenGate() {
        var before = root()
        repeat(authority.downsFromConversationToMedical()) { before = moveDown(before) }
        val gesture = authority.openMedicalGesture()
        val after = openMedicalDirect(before)
        assertTrue(authority.isMedicalOpenedViaDirectShortcut(after))
        assertFalse(
            authority.isMethod1OpenCompleted(before, after, gesture.first, gesture.second)
        )
        assertFalse(
            authority.isMethod1OpenCompleted(
                before,
                after,
                GuidedModeNavigation.SELECT_LEFT,
                GuidedModeNavigation.SELECT_RIGHT
            )
        )
    }

    @Test
    fun staleOpenSelectedDoesNotSatisfyMethod1() {
        var selected = root()
        repeat(authority.downsFromConversationToMedical()) { selected = moveDown(selected) }
        val alreadyOpen = openSelected(selected)
        assertTrue(authority.isMedicalOpenedViaSelect(alreadyOpen))
        // Replaying L1 R1 while already open must not count as a fresh Method 1 completion.
        assertFalse(
            authority.isMethod1OpenCompleted(
                alreadyOpen,
                alreadyOpen,
                GuidedModeNavigation.SELECT_LEFT,
                GuidedModeNavigation.SELECT_RIGHT
            )
        )
    }

    @Test
    fun method1WellDoneKeepsMedicalVisibleUntilAckThenResets() {
        val openPhase = fullLesson16().phases[1]
        assertTrue(openPhase.showCompletionFeedback)
        assertTrue(openPhase.resetWorkspaceBeforeNextPhase)
        assertEquals(1_600L, TrainingSessionController.NAVIGATION_FEEDBACK_VISIBLE_MS)

        val main = read("MainActivity.kt")
        val resetHook = main.substringAfter("onNavigationPhaseAdvanced = { resetWorkspace ->")
            .substringBefore("startupSession =")
        assertTrue(resetHook.contains("communicationWorkspaceRoot"))
        assertTrue(resetHook.contains("closeWorkspacePanelsOnly"))
        // Acknowledgement window runs first; reset is deferred inside the feedback delay callback.
        val session = read("features/onboardingguide/services/TrainingSessionController.kt")
        val feedbackFn = session.substringAfter("private fun beginIntermediatePhaseFeedback")
            .substringBefore("fun verifyNavigation(")
        assertTrue(feedbackFn.contains("NAVIGATION_FEEDBACK_VISIBLE_MS"))
        assertTrue(feedbackFn.contains("onNavigationPhaseAdvanced?.invoke(advance.resetWorkspaceBeforeNextPhase)"))
        assertTrue(
            feedbackFn.indexOf("NAVIGATION_FEEDBACK_VISIBLE_MS") <
                feedbackFn.indexOf("onNavigationPhaseAdvanced?.invoke")
        )
    }

    @Test
    fun resetYieldsMethod2StartWithoutAdvancingToLesson17() {
        val afterOpen = GuidedLessonPhaseEngine.advanceResult(fullLesson16(), 1)
            as GuidedLessonPhaseAdvanceResult.IntermediatePhaseCompleted
        assertEquals(2, afterOpen.nextPhaseIndex)
        assertNotEquals(
            TrainingLessonCatalog.navigationLessons[1].action,
            NavigationAction.MoveToMedicalCategory
        )
        // Still Lesson 16 until final Method 2 phase completes.
        assertTrue(
            GuidedLessonPhaseEngine.advanceResult(fullLesson16(), 2) is
                GuidedLessonPhaseAdvanceResult.FinalPhaseCompleted
        )

        val reset = root()
        assertTrue(authority.isMethod2StartState(reset))
        assertFalse(authority.isMedicalPhraseWorkspaceOpen(reset))
        assertEquals(CategoryNavigationCause.MENU_RESTORE, reset.categoryNavigationCause)
        assertNotEquals(CategoryNavigationCause.OPEN_SELECTED, reset.categoryNavigationCause)
        assertNotEquals(CategoryNavigationCause.DIRECT_SHORTCUT, reset.categoryNavigationCause)
    }

    // --- Method 2 -------------------------------------------------------------------------------

    @Test
    fun method2CardHighlightsDirectMedicalAndL3R1() {
        val method2 = GuidedLessonTeachingSpec.presentationFor(
            NavigationAction.MoveToMedicalCategory,
            authority.ID_MOVE_TO_MEDICAL,
            uiStrings,
            phaseIndex = 2
        )
        assertEquals(
            GuidedLesson16AssessmentPhase.Method2DirectOpenMedical,
            GuidedLesson16AssessmentPhase.fromPhaseId(fullLesson16().phases[2].id)
        )
        assertEquals(
            GuidedLessonPhaseRequiredAction.CategoryShortcutJump,
            fullLesson16().phases[2].requiredAction
        )
        assertTrue(
            method2.methods.single().instructionalLines.any {
                it.contains("Use L3 R1 to open Medical directly.")
            }
        )
        assertEquals("L3 R1", method2.methods.single().highlightedSequence)
        assertNull(method2.navigationControlHighlight)
        assertEquals(authority.medicalCategoryIndex, method2.destinationCategoryIndex)
        assertNotEquals(GuidedWorkspaceHighlightTarget.NextPage, method2.navigationControlHighlight)
        assertNotEquals(GuidedWorkspaceHighlightTarget.Select, method2.navigationControlHighlight)
    }

    @Test
    fun method2RequiresFreshDirectShortcutVisibleOpen() {
        val before = root()
        val after = openMedicalDirect(before)
        assertEquals(CategoryNavigationCause.DIRECT_SHORTCUT, after.categoryNavigationCause)
        assertTrue(authority.isMedicalPhraseWorkspaceOpen(after))
        assertTrue(
            authority.isMethod2DirectCompleted(
                before,
                after,
                authority.openMedicalGesture().first,
                authority.openMedicalGesture().second
            )
        )

        // Selection alone / L1 R1 / stale Method 1 open must not satisfy Method 2.
        var selected = root()
        repeat(authority.downsFromConversationToMedical()) { selected = moveDown(selected) }
        assertFalse(
            authority.isMethod2DirectCompleted(
                selected,
                selected,
                authority.openMedicalGesture().first,
                authority.openMedicalGesture().second
            )
        )
        val viaSelect = openSelected(selected)
        assertFalse(
            authority.isMethod2DirectCompleted(
                selected,
                viaSelect,
                GuidedModeNavigation.SELECT_LEFT,
                GuidedModeNavigation.SELECT_RIGHT
            )
        )
        assertFalse(
            authority.isMethod2DirectCompleted(
                viaSelect,
                viaSelect,
                authority.openMedicalGesture().first,
                authority.openMedicalGesture().second
            )
        )
    }

    @Test
    fun lesson16CompletesOnlyAfterMethod2FinalPhase() {
        assertTrue(
            GuidedLessonPhaseEngine.advanceResult(fullLesson16(), 0) is
                GuidedLessonPhaseAdvanceResult.IntermediatePhaseCompleted
        )
        assertTrue(
            GuidedLessonPhaseEngine.advanceResult(fullLesson16(), 1) is
                GuidedLessonPhaseAdvanceResult.IntermediatePhaseCompleted
        )
        val final = GuidedLessonPhaseEngine.advanceResult(fullLesson16(), 2)
            as GuidedLessonPhaseAdvanceResult.FinalPhaseCompleted
        assertEquals(authority.OPEN_DIRECT_FEEDBACK_DETAIL, final.completedPhase.completionFeedbackDetail)
        assertEquals(
            NavigationAction.SelectCategory,
            TrainingLessonCatalog.navigationLessons[1].action
        )
    }

    // --- Wiring / regression --------------------------------------------------------------------

    @Test
    fun mainActivityUsesMethodGatesAndAcceptsBothSequences() {
        val main = read("MainActivity.kt")
        assertTrue(main.contains("isMethod1OpenCompleted"))
        assertTrue(main.contains("isMethod2DirectCompleted"))
        val accept = main.substringAfter("private fun acceptsMoveToMedicalPhaseGesture")
            .substringBefore("private fun isNavigationLessonOffTargetAttempt")
        assertTrue(accept.contains("matchesOpenSelected"))
        assertTrue(accept.contains("matchesOpenMedical"))
        assertFalse(
            accept.substringAfter("CategoryShortcutJump ->")
                .substringBefore("// RC8.28")
                .trim()
                .startsWith("false")
        )
        val handler = main.substringAfter("private fun handleMoveToMedicalLessonPhase")
            .substringBefore("private fun acceptsAdjustSensitivityPhaseGesture")
        assertFalse(handler.contains("direct Medical open belongs to Lesson 17"))
        assertTrue(handler.contains("CategoryShortcutJump"))
    }

    @Test
    fun productionPathsOutsideGuidedLearningUnchanged() {
        var state = root()
        repeat(authority.downsFromConversationToMedical()) { state = moveDown(state) }
        state = openSelected(state)
        assertTrue(authority.isMedicalOpenedViaSelect(state))

        val direct = openMedicalDirect(root())
        assertTrue(authority.isMedicalOpenedViaDirectShortcut(direct))

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
    }

    @Test
    fun lifecycleNamesAndNoMethod1CompleteBooleanAsSourceOfTruth() {
        assertEquals("Method1Success", GuidedLesson16AssessmentPhase.LIFECYCLE_METHOD1_SUCCESS)
        assertEquals("ResetForMethod2", GuidedLesson16AssessmentPhase.LIFECYCLE_RESET_FOR_METHOD2)
        assertEquals("Method2Success", GuidedLesson16AssessmentPhase.LIFECYCLE_METHOD2_SUCCESS)
        assertEquals("Completed", GuidedLesson16AssessmentPhase.LIFECYCLE_COMPLETED)

        val authoritySrc = read("features/guidedmedicalcategoryjourney/GuidedMedicalCategoryJourneyAuthority.kt")
        assertFalse(authoritySrc.contains("method1Complete"))
        assertTrue(authoritySrc.contains("isMethod1OpenCompleted"))
        assertTrue(authoritySrc.contains("isMethod2DirectCompleted"))
    }

    @Test
    fun lesson16CardRemainsCompactWithoutDuplicateInstruction() {
        val full = fullLesson16()
        full.phases.forEach { phase ->
            val lines = phase.methods.flatMap { it.instructionalLines }
            assertEquals(lines.toSet().size, lines.size)
            if (phase.description != null) {
                assertFalse(lines.any { it == phase.description })
            }
        }
        val cardAuthority = read("features/guidedworkspacelessoncard/GuidedWorkspaceLessonCardAuthority.kt")
        assertTrue(cardAuthority.contains("MaxHeightFraction") || cardAuthority.contains("0.45"))
    }
}
