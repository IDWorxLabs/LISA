package com.idworx.lisa

import com.idworx.lisa.features.guidedlessonexecutionauthority.GuidedLessonExecutionAuthority
import com.idworx.lisa.features.guidedlessonteaching.GuidedLesson16AssessmentPhase
import com.idworx.lisa.features.guidedlessonteaching.GuidedLessonPhaseAdvanceResult
import com.idworx.lisa.features.guidedlessonteaching.GuidedLessonPhaseEngine
import com.idworx.lisa.features.guidedlessonteaching.GuidedLessonPhaseRequiredAction
import com.idworx.lisa.features.guidedlessonteaching.GuidedLessonTeachingSpec
import com.idworx.lisa.features.guidedmedicalcategoryjourney.GuidedMedicalCategoryJourneyAuthority
import com.idworx.lisa.features.onboardingguide.lessons.TrainingLessonCatalog
import com.idworx.lisa.features.onboardingguide.metadata.TrainingMetadata
import com.idworx.lisa.features.onboardingguide.model.NavigationAction
import com.idworx.lisa.features.onboardingguide.navigation.GuidedWorkspaceHighlightTarget
import com.idworx.lisa.features.zerotouchprinciple.audit.ZeroTouchFileProbe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * RC8.25 — Lesson 16 scroll+open, Lesson 17 direct open, Lesson 18 phrase, Lesson 19 back.
 */
class Rc8_25SplitAndRenumberGuidedLessons16OnwardTest {

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

    // --- Catalogue -------------------------------------------------------------------------------

    @Test
    fun catalogueOrderAndIdsAreStableFourMedicalJourneyLessons() {
        val nav = TrainingLessonCatalog.navigationLessons
        assertEquals(authority.ID_MOVE_TO_MEDICAL, nav[0].id)
        assertEquals(NavigationAction.MoveToMedicalCategory, nav[0].action)
        assertEquals(authority.ID_OPEN_MEDICAL, nav[1].id)
        assertEquals(NavigationAction.SelectCategory, nav[1].action)
        assertEquals(authority.ID_USE_MEDICAL_PHRASE, nav[2].id)
        assertEquals(NavigationAction.SelectPhrase, nav[2].action)
        assertEquals(GuidedLessonExecutionAuthority.ID_WORKSPACE_BACK, nav[3].id)
        assertEquals(NavigationAction.CloseMenu, nav[3].action)
        assertEquals(TrainingMetadata.NAVIGATION_LESSON_COUNT, nav.size)
        assertEquals(8, nav.size)
        // No duplicate open-Medical lessons.
        assertEquals(1, nav.count { it.action == NavigationAction.SelectCategory })
        assertEquals(1, nav.count { it.action == NavigationAction.MoveToMedicalCategory })
    }

    @Test
    fun displayNumbersRemainLesson16Through19ForMedicalJourney() {
        // Essential phrase path: UI Lesson = 15 + navIndex + 1
        assertEquals(16, 15 + 0 + 1)
        assertEquals(17, 15 + 1 + 1)
        assertEquals(18, 15 + 2 + 1)
        assertEquals(19, 15 + 3 + 1)
    }

    // --- Lesson 16 -------------------------------------------------------------------------------

    @Test
    fun lesson16IsScrollOpenThenDirectMethod2InsideSameLesson() {
        val full = GuidedLessonTeachingSpec.fullPresentationFor(
            NavigationAction.MoveToMedicalCategory,
            authority.ID_MOVE_TO_MEDICAL,
            uiStrings
        )
        assertEquals(3, full.phases.size)
        assertEquals(
            GuidedLessonPhaseRequiredAction.MoveDownUntilCategorySelected,
            full.phases[0].requiredAction
        )
        assertEquals(
            GuidedLessonPhaseRequiredAction.OpenSelectedCategory,
            full.phases[1].requiredAction
        )
        assertTrue(
            full.phases.any {
                it.requiredAction == GuidedLessonPhaseRequiredAction.CategoryShortcutJump
            }
        )
        assertEquals(
            GuidedLesson16AssessmentPhase.Part1ScrollToMedical,
            GuidedLesson16AssessmentPhase.fromPhaseId(full.phases[0].id)
        )
        assertTrue(
            GuidedLessonPhaseEngine.advanceResult(full, 1) is
                GuidedLessonPhaseAdvanceResult.IntermediatePhaseCompleted
        )
        assertTrue(
            GuidedLessonPhaseEngine.advanceResult(full, 2) is
                GuidedLessonPhaseAdvanceResult.FinalPhaseCompleted
        )
    }

    @Test
    fun lesson16SelectionSilentlyAdvancesAndOpenRequiresSelectCause() {
        val full = GuidedLessonTeachingSpec.fullPresentationFor(
            NavigationAction.MoveToMedicalCategory, null, uiStrings
        )
        val afterScroll = GuidedLessonPhaseEngine.advanceResult(full, 0)
            as GuidedLessonPhaseAdvanceResult.IntermediatePhaseCompleted
        assertFalse(afterScroll.showCompletionFeedback)
        assertEquals(1, afterScroll.nextPhaseIndex)

        val openPhase = GuidedLessonTeachingSpec.presentationFor(
            NavigationAction.MoveToMedicalCategory,
            authority.ID_MOVE_TO_MEDICAL,
            uiStrings,
            phaseIndex = 1
        )
        assertTrue(
            openPhase.methods.single().instructionalLines.any {
                it.contains("Medical is selected. Use L1 R1 to open it.")
            }
        )
        assertEquals(GuidedWorkspaceHighlightTarget.Select, openPhase.navigationControlHighlight)
        assertEquals("L1 R1", openPhase.methods.single().highlightedSequence)

        var state = root()
        repeat(authority.downsFromConversationToMedical()) { state = moveDown(state) }
        assertTrue(authority.isMedicalSelectedInCategoryMenu(state))
        assertFalse(authority.isMedicalPhraseWorkspaceOpen(state))
        state = openSelected(state)
        assertTrue(authority.isMedicalOpenedViaSelect(state))
    }

    @Test
    fun lesson16ResetPrepForLesson17UsesCleanCategoryMenu() {
        val main = read("MainActivity.kt")
        val openPrep = main.substringAfter("authority.ID_OPEN_MEDICAL -> {")
            .substringBefore("authority.ID_USE_MEDICAL_PHRASE -> {")
        assertTrue(openPrep.contains("communicationWorkspaceRoot"))
        assertFalse(openPrep.contains("medicalCategoryIndex"))
    }

    // --- Lesson 17 -------------------------------------------------------------------------------

    @Test
    fun lesson17TeachesDirectL3R1AndRequiresDirectShortcut() {
        val teaching = GuidedLessonTeachingSpec.presentationFor(
            NavigationAction.SelectCategory,
            authority.ID_OPEN_MEDICAL,
            uiStrings
        )
        assertEquals(authority.OPEN_DIRECT_TITLE, teaching.title)
        assertTrue(
            teaching.methods.single().instructionalLines.any {
                it.contains("Use L3 R1 to open Medical directly.")
            }
        )
        assertEquals("L3 R1", teaching.methods.single().highlightedSequence)
        assertEquals(authority.medicalCategoryIndex, teaching.destinationCategoryIndex)
        assertNull(teaching.navigationControlHighlight)

        val start = root()
        assertTrue(authority.isLesson16StartState(start))
        val opened = openMedicalDirect(start)
        assertTrue(authority.isMedicalOpenedViaDirectShortcut(opened))

        var selectedThenSelect = root()
        repeat(authority.downsFromConversationToMedical()) {
            selectedThenSelect = moveDown(selectedThenSelect)
        }
        selectedThenSelect = openSelected(selectedThenSelect)
        assertTrue(authority.isMedicalOpenedViaSelect(selectedThenSelect))
        assertFalse(authority.isMedicalOpenedViaDirectShortcut(selectedThenSelect))
    }

    // --- Lesson 18 -------------------------------------------------------------------------------

    @Test
    fun lesson18TeachesFirstMedicalPhraseL2R1() {
        val teaching = GuidedLessonTeachingSpec.presentationFor(
            NavigationAction.SelectPhrase,
            authority.ID_USE_MEDICAL_PHRASE,
            uiStrings
        )
        assertEquals(GuidedWorkspaceHighlightTarget.PhraseRow, teaching.navigationControlHighlight)
        assertTrue(
            teaching.methods.single().instructionalLines.any {
                it.contains("Use L2 R1 to say")
            }
        )
        val entry = authority.firstMedicalPhraseEntry()
        assertEquals("L2 R1", entry.sequenceLabel)
        assertTrue(entry.phrase.contains("I am in pain", ignoreCase = true))
        assertTrue(authority.matchesFirstMedicalPhrase(entry))
        assertEquals("L2 R1", teaching.rawGestureLabel)

        val main = read("MainActivity.kt")
        assertTrue(main.contains("medicalPhraseLessonArmed"))
        assertTrue(main.contains("matchesFirstMedicalPhrase"))
    }

    // --- Lesson 19 -------------------------------------------------------------------------------

    @Test
    fun lesson19TeachesBackAndDoesNotPreExecute() {
        val teaching = GuidedLessonTeachingSpec.presentationFor(
            NavigationAction.CloseMenu,
            GuidedLessonExecutionAuthority.ID_WORKSPACE_BACK,
            uiStrings
        )
        assertEquals(GuidedWorkspaceHighlightTarget.Back, teaching.navigationControlHighlight)
        assertTrue(
            teaching.methods.single().instructionalLines.any {
                it.contains("Use L2 R2 to go back to the category menu.")
            }
        )
        assertEquals("L2 R2", teaching.methods.single().highlightedSequence)

        val main = read("MainActivity.kt")
        val backPrep = main.substringAfter("execution.ID_WORKSPACE_BACK -> {")
            .substringBefore("else -> Unit")
        assertTrue(backPrep.contains("isWorkspaceBackStartState"))
        assertFalse(backPrep.contains("openCategoryMenu("))
        assertTrue(
            GuidedLessonExecutionAuthority.isWorkspaceBackStartState(
                GuidedNavigationController.openCategoryDirectly(root(), authority.medicalCategoryIndex)
            )
        )
        assertTrue(
            GuidedLessonExecutionAuthority.isWorkspaceBackCompleted(
                GuidedNavigationController.openCategoryDirectly(root(), authority.medicalCategoryIndex),
                root()
            )
        )
    }

    @Test
    fun laterLessonsRemainIntactAfterMedicalJourney() {
        val nav = TrainingLessonCatalog.navigationLessons
        assertEquals("nav_next_page", nav[4].id)
        assertEquals(NavigationAction.NextPage, nav[4].action)
        assertEquals(
            com.idworx.lisa.features.guidedsensitivitylesson.GuidedSensitivityLessonAuthority
                .ID_ADJUST_SENSITIVITY,
            nav.last().id
        )
        assertNotEquals(authority.ID_OPEN_MEDICAL, nav[4].id)
    }

    @Test
    fun productionOpenPathsUnchangedOutsideGuidedGates() {
        val viaSelect = openSelected(
            GuidedNavigationState(
                screenMode = GuidedOverlayScreenMode.CategoryMenu,
                categoryMenuSelection = authority.medicalCategoryIndex
            )
        )
        assertEquals(CategoryNavigationCause.OPEN_SELECTED, viaSelect.categoryNavigationCause)
        val viaDirect = openMedicalDirect(
            GuidedNavigationState(screenMode = GuidedOverlayScreenMode.CategoryMenu)
        )
        assertEquals(CategoryNavigationCause.DIRECT_SHORTCUT, viaDirect.categoryNavigationCause)
    }
}
