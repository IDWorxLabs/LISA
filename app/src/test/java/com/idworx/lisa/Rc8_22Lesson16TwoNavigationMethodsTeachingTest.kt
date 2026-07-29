package com.idworx.lisa

import com.idworx.lisa.features.guidedlessonteaching.GuidedLessonTeachingSpec
import com.idworx.lisa.features.guidedmedicalcategoryjourney.GuidedMedicalCategoryJourneyAuthority
import com.idworx.lisa.features.guidedworkspacelessoncard.GuidedWorkspaceLessonCardAuthority
import com.idworx.lisa.features.onboardingguide.lessons.TrainingLessonCatalog
import com.idworx.lisa.features.onboardingguide.model.NavigationAction
import com.idworx.lisa.features.onboardingguide.navigation.GuidedWorkspaceHighlightTarget
import com.idworx.lisa.features.onboardingguide.navigation.GuidedWorkspaceTrainingSpec
import com.idworx.lisa.features.zerotouchprinciple.audit.ZeroTouchFileProbe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * RC8.22 / RC8.23 — Lesson 16 still teaches both navigation methods (now as sequential phases).
 */
class Rc8_22Lesson16TwoNavigationMethodsTeachingTest {

    private val uiStrings = LisaUiStrings.forLanguage(PreferredLanguage.English)

    private fun read(pathUnderMainJava: String): String {
        val path = "app/src/main/java/com/idworx/lisa/$pathUnderMainJava"
        return ZeroTouchFileProbe.readProjectFile(path)
            ?: error("Missing source: $path")
    }

    @Test
    fun popupExplainsTwoWaysAcrossSequentialPhases() {
        val full = GuidedLessonTeachingSpec.fullPresentationFor(
            NavigationAction.MoveToMedicalCategory,
            GuidedMedicalCategoryJourneyAuthority.ID_MOVE_TO_MEDICAL,
            uiStrings
        )
        assertEquals("Explore Communication", full.title)
        assertEquals(2, full.phases.size)

        val method1 = full.phases[0].methods.single()
        assertEquals("Method 1", method1.title)
        assertTrue(
            method1.instructionalLines.any {
                it.contains("Use L0 R2 to scroll down one category at a time until Medical is selected.")
            }
        )
        assertEquals("L0 R2", method1.highlightedSequence)

        assertEquals("L1 R1", full.phases[1].methods.single().highlightedSequence)

        val lesson17 = GuidedLessonTeachingSpec.presentationFor(
            NavigationAction.SelectCategory,
            GuidedMedicalCategoryJourneyAuthority.ID_OPEN_MEDICAL,
            uiStrings
        )
        assertEquals("L3 R1", lesson17.methods.single().highlightedSequence)
    }

    @Test
    fun cardRendersSeparateMethodBlocksWithSequenceBoxes() {
        val card = read("features/onboardingguide/ui/TrainingComponents.kt")
            .substringAfter("fun GuidedWorkspaceLessonCard(")
            .substringBefore("fun GuidedLessonPhraseTitle(")
        assertTrue(card.contains("usesStructuredMethods"))
        assertTrue(card.contains("teaching.methods.forEachIndexed"))
        assertTrue(card.contains("GuidedLessonSequenceEmphasisBox"))
        assertTrue(card.contains("method.highlightedSequence"))
        assertFalse(card.contains("verticalScroll"))
    }

    @Test
    fun onlyMoveDownIsGuidedHighlightInPart1MedicalNotPreHighlighted() {
        val teaching = GuidedLessonTeachingSpec.presentationFor(
            NavigationAction.MoveToMedicalCategory,
            TrainingLessonCatalog.navigationLessons[0].id,
            uiStrings,
            phaseIndex = 0
        )
        assertEquals(GuidedWorkspaceHighlightTarget.NextPage, teaching.navigationControlHighlight)
        assertNull(teaching.destinationCategoryIndex)
        assertEquals(
            GuidedMedicalCategoryJourneyAuthority.medicalCategoryIndex,
            teaching.productionTargetCategoryIndex
        )
        assertEquals(
            GuidedWorkspaceHighlightTarget.NextPage,
            GuidedWorkspaceTrainingSpec.highlightTargetFor(NavigationAction.MoveToMedicalCategory)
        )
    }

    @Test
    fun rc821ProductionBehaviourUnchangedForScrollPhase() {
        var state = GuidedNavigationController.communicationWorkspaceRoot(GuidedNavigationState())
        assertTrue(GuidedMedicalCategoryJourneyAuthority.isLesson16StartState(state))
        state = (GuidedNavigationController.processSequence(
            GuidedModeNavigation.NEXT_LEFT,
            GuidedModeNavigation.NEXT_RIGHT,
            state,
            PreferredLanguage.English,
            uiStrings
        ) as GuidedSequenceResult.Navigate).newState
        assertEquals(1, state.categoryMenuSelection)
        state = (GuidedNavigationController.processSequence(
            GuidedModeNavigation.NEXT_LEFT,
            GuidedModeNavigation.NEXT_RIGHT,
            state,
            PreferredLanguage.English,
            uiStrings
        ) as GuidedSequenceResult.Navigate).newState
        assertTrue(GuidedMedicalCategoryJourneyAuthority.isMedicalSelectedInCategoryMenu(state))

        val main = read("MainActivity.kt")
        assertTrue(main.contains("completeNavigationLessonPhase") || main.contains("verifyTrainingNavigation"))
        assertEquals(0.45f, GuidedWorkspaceLessonCardAuthority.MaxHeightFraction, 0.001f)
    }

    @Test
    fun teachingModelSupportsMultipleMethodsForFutureLessons() {
        val full = GuidedLessonTeachingSpec.fullPresentationFor(
            NavigationAction.MoveToMedicalCategory,
            null,
            uiStrings
        )
        assertTrue(full.phases.size >= 2)
        full.phases.forEach { phase ->
            assertTrue(phase.methods.isNotEmpty())
            phase.methods.forEach { method ->
                assertTrue(method.title.isNotBlank())
                assertTrue(method.instructionalLines.isNotEmpty())
                assertFalse(method.highlightedSequence.isNullOrBlank())
            }
        }
        val legacy = GuidedLessonTeachingSpec.presentationFor(
            NavigationAction.NextPage,
            "nav_next_page",
            uiStrings
        )
        assertFalse(legacy.usesStructuredMethods)
        assertTrue(legacy.methods.isEmpty())
    }
}
