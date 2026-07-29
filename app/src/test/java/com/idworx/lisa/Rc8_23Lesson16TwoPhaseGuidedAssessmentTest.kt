package com.idworx.lisa

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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * RC8.23 / RC8.25 — Lesson 16 multi-stage scroll+open (direct open is Lesson 17).
 */
class Rc8_23Lesson16TwoPhaseGuidedAssessmentTest {

    private val uiStrings = LisaUiStrings.forLanguage(PreferredLanguage.English)

    private fun read(pathUnderMainJava: String): String {
        val path = "app/src/main/java/com/idworx/lisa/$pathUnderMainJava"
        return ZeroTouchFileProbe.readProjectFile(path)
            ?: error("Missing source: $path")
    }

    @Test
    fun lesson16HasScrollThenOpenStagesOnly() {
        val full = GuidedLessonTeachingSpec.fullPresentationFor(
            NavigationAction.MoveToMedicalCategory,
            GuidedMedicalCategoryJourneyAuthority.ID_MOVE_TO_MEDICAL,
            uiStrings
        )
        assertTrue(full.isMultiPhase)
        assertEquals(2, full.phases.size)
        assertEquals(
            GuidedLessonPhaseRequiredAction.MoveDownUntilCategorySelected,
            full.phases[0].requiredAction
        )
        assertEquals(
            GuidedLessonPhaseRequiredAction.OpenSelectedCategory,
            full.phases[1].requiredAction
        )
        assertFalse(full.phases[0].resetWorkspaceBeforeNextPhase)
        assertFalse(full.phases[1].resetWorkspaceBeforeNextPhase)
    }

    @Test
    fun part1TeachesManualNavigationThenOpenSelected() {
        val part1Scroll = GuidedLessonTeachingSpec.presentationFor(
            NavigationAction.MoveToMedicalCategory,
            GuidedMedicalCategoryJourneyAuthority.ID_MOVE_TO_MEDICAL,
            uiStrings,
            phaseIndex = 0
        )
        assertEquals("Explore Communication", part1Scroll.title)
        assertEquals("Method 1", part1Scroll.methods.single().title)
        assertTrue(
            part1Scroll.methods.single().instructionalLines.any {
                it.contains("Use L0 R2 to scroll down one category at a time until Medical is selected.")
            }
        )
        assertEquals("L0 R2", part1Scroll.methods.single().highlightedSequence)
        assertEquals(GuidedWorkspaceHighlightTarget.NextPage, part1Scroll.navigationControlHighlight)
        assertNull(part1Scroll.destinationCategoryIndex)

        val part1Open = GuidedLessonTeachingSpec.presentationFor(
            NavigationAction.MoveToMedicalCategory,
            GuidedMedicalCategoryJourneyAuthority.ID_MOVE_TO_MEDICAL,
            uiStrings,
            phaseIndex = 1
        )
        assertEquals(GuidedWorkspaceHighlightTarget.Select, part1Open.navigationControlHighlight)
        assertEquals("L1 R1", part1Open.methods.single().highlightedSequence)
        assertTrue(
            part1Open.methods.single().instructionalLines.any {
                it.contains("Medical is selected. Use L1 R1 to open it.")
            }
        )
    }

    @Test
    fun lesson17OwnsDirectJumpOpen() {
        val lesson17 = GuidedLessonTeachingSpec.presentationFor(
            NavigationAction.SelectCategory,
            GuidedMedicalCategoryJourneyAuthority.ID_OPEN_MEDICAL,
            uiStrings
        )
        assertTrue(
            lesson17.methods.single().instructionalLines.any {
                it.contains("Use L3 R1 to open Medical directly.")
            }
        )
        assertEquals("L3 R1", lesson17.methods.single().highlightedSequence)
        assertEquals(
            GuidedMedicalCategoryJourneyAuthority.medicalCategoryIndex,
            lesson17.destinationCategoryIndex
        )
        assertNull(lesson17.navigationControlHighlight)
    }

    @Test
    fun phaseEngineCompletesLesson16AfterOpenStage() {
        val full = GuidedLessonTeachingSpec.fullPresentationFor(
            NavigationAction.MoveToMedicalCategory,
            null,
            uiStrings
        )
        val afterScroll = GuidedLessonPhaseEngine.advanceResult(full, 0)
            as GuidedLessonPhaseAdvanceResult.IntermediatePhaseCompleted
        assertEquals(1, afterScroll.nextPhaseIndex)
        assertFalse(afterScroll.showCompletionFeedback)

        assertTrue(
            GuidedLessonPhaseEngine.advanceResult(full, 1) is
                GuidedLessonPhaseAdvanceResult.FinalPhaseCompleted
        )
    }

    @Test
    fun productionPathsSupportScrollOpenThenDirectOpen() {
        val start = GuidedNavigationController.communicationWorkspaceRoot(GuidedNavigationState())
        assertTrue(GuidedMedicalCategoryJourneyAuthority.isLesson16StartState(start))

        var scrolled = start
        repeat(GuidedMedicalCategoryJourneyAuthority.downsFromConversationToMedical()) {
            scrolled = (GuidedNavigationController.processSequence(
                GuidedModeNavigation.NEXT_LEFT,
                GuidedModeNavigation.NEXT_RIGHT,
                scrolled,
                PreferredLanguage.English,
                uiStrings
            ) as GuidedSequenceResult.Navigate).newState
        }
        assertTrue(GuidedMedicalCategoryJourneyAuthority.isMedicalSelectedInCategoryMenu(scrolled))

        val opened = (GuidedNavigationController.processSequence(
            GuidedModeNavigation.SELECT_LEFT,
            GuidedModeNavigation.SELECT_RIGHT,
            scrolled,
            PreferredLanguage.English,
            uiStrings
        ) as GuidedSequenceResult.Navigate).newState
        assertTrue(GuidedMedicalCategoryJourneyAuthority.isMedicalOpenedViaSelect(opened))

        val reset = GuidedNavigationController.communicationWorkspaceRoot(GuidedNavigationState())
        val jumped = (GuidedNavigationController.processSequence(
            GuidedMedicalCategoryJourneyAuthority.openMedicalGesture().first,
            GuidedMedicalCategoryJourneyAuthority.openMedicalGesture().second,
            reset,
            PreferredLanguage.English,
            uiStrings
        ) as GuidedSequenceResult.Navigate).newState
        assertTrue(GuidedMedicalCategoryJourneyAuthority.isMedicalOpenedViaDirectShortcut(jumped))

        assertEquals(
            NavigationAction.SelectCategory,
            TrainingLessonCatalog.navigationLessons[1].action
        )
        val main = read("MainActivity.kt")
        assertTrue(main.contains("completeNavigationLessonPhase"))
        assertTrue(main.contains("handleMoveToMedicalLessonPhase"))
    }

    @Test
    fun teachingModelIsReusableForFutureMultiPhaseLessons() {
        val full = GuidedLessonTeachingSpec.fullPresentationFor(
            NavigationAction.MoveToMedicalCategory,
            null,
            uiStrings
        )
        full.phases.forEach { phase ->
            assertTrue(phase.id.isNotBlank())
            assertTrue(phase.title.isNotBlank())
            assertTrue(phase.methods.isNotEmpty() || phase.description != null)
        }
        val single = GuidedLessonTeachingSpec.fullPresentationFor(
            NavigationAction.NextPage,
            "nav_next_page",
            uiStrings
        )
        assertFalse(single.isMultiPhase)
        assertEquals(
            GuidedLessonPhaseAdvanceResult.SingleStepLesson,
            GuidedLessonPhaseEngine.advanceResult(single, 0)
        )
    }

    @Test
    fun cardShowsFeedbackDetailAndPhaseAwareTeaching() {
        val card = read("features/onboardingguide/ui/TrainingComponents.kt")
        assertTrue(card.contains("feedbackDetail"))
        val host = read("LisaAccessibilityUi.kt")
        assertTrue(host.contains("phaseIndex = guidedTrainingState.navigationLessonPhaseIndex"))
        assertTrue(host.contains("navigationFeedbackDetail"))
        val session = read("features/onboardingguide/services/TrainingSessionController.kt")
        assertTrue(session.contains("completeNavigationLessonPhase"))
        assertTrue(session.contains("showCompletionFeedback"))
    }
}
