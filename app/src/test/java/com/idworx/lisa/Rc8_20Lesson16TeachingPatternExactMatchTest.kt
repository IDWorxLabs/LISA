package com.idworx.lisa

import com.idworx.lisa.features.guidedlessonteaching.GuidedLessonTeachingSpec
import com.idworx.lisa.features.guidedmedicalcategoryjourney.GuidedMedicalCategoryJourneyAuthority
import com.idworx.lisa.features.onboardingguide.lessons.TrainingLessonCatalog
import com.idworx.lisa.features.onboardingguide.model.NavigationAction
import com.idworx.lisa.features.onboardingguide.navigation.GuidedWorkspaceHighlightTarget
import com.idworx.lisa.features.onboardingguide.navigation.GuidedWorkspaceTrainingSpec
import com.idworx.lisa.features.zerotouchprinciple.audit.ZeroTouchFileProbe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * RC8.20 / RC8.23 — Lesson 16 Part 1 teaching pattern (scroll method).
 */
class Rc8_20Lesson16TeachingPatternExactMatchTest {

    private val uiStrings = LisaUiStrings.forLanguage(PreferredLanguage.English)

    private fun read(pathUnderMainJava: String): String {
        val path = "app/src/main/java/com/idworx/lisa/$pathUnderMainJava"
        return ZeroTouchFileProbe.readProjectFile(path)
            ?: error("Missing source: $path")
    }

    @Test
    fun popupMatchesReferenceCopyExactly() {
        val teaching = GuidedLessonTeachingSpec.presentationFor(
            NavigationAction.MoveToMedicalCategory,
            GuidedMedicalCategoryJourneyAuthority.ID_MOVE_TO_MEDICAL,
            uiStrings,
            phaseIndex = 0
        )
        assertEquals("Explore Communication", teaching.title)
        assertTrue(teaching.usesStructuredMethods)
        assertEquals("Method 1", teaching.methods[0].title)
        assertEquals(
            listOf("Use L0 R2 to scroll down one category at a time until Medical is selected."),
            teaching.methods[0].instructionalLines
        )
        assertEquals("L0 R2", teaching.methods[0].highlightedSequence)
        assertEquals("L0 R2", teaching.rawGestureLabel)
        val part2 = GuidedLessonTeachingSpec.presentationFor(
            NavigationAction.SelectCategory,
            GuidedMedicalCategoryJourneyAuthority.ID_OPEN_MEDICAL,
            uiStrings
        )
        assertTrue(
            part2.methods.any { method ->
                method.highlightedSequence == "L3 R1" ||
                    method.instructionalLines.any { it.contains("L3 R1") }
            }
        )
    }

    @Test
    fun sequenceIsRenderedInOwnHighlightedBox() {
        val card = read("features/onboardingguide/ui/TrainingComponents.kt")
            .substringAfter("fun GuidedWorkspaceLessonCard(")
            .substringBefore("fun GuidedLessonPhraseTitle(")
        assertTrue(card.contains("GuidedLessonSequenceEmphasisBox"))
        assertTrue(card.contains("method.highlightedSequence"))
        assertTrue(card.contains("RoundedCornerShape(8.dp)"))
        assertTrue(card.contains("LisaBlue.copy(alpha = 0.12f)"))
        assertFalse(card.contains("verticalScroll"))
    }

    @Test
    fun onlyMoveDownReceivesLessonHighlightMedicalIsNotPreHighlighted() {
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
        assertNotEquals(0, teaching.productionTargetCategoryIndex)

        val ui = read("LisaGuidedModeUi.kt")
        assertTrue(ui.contains("selected = isSelectedRow"))
        assertTrue(
            ui.contains(
                "GuidedPanelActionKind.ScrollDown -> highlightTarget == GuidedWorkspaceHighlightTarget.NextPage"
            )
        )
        assertEquals(
            GuidedWorkspaceHighlightTarget.NextPage,
            GuidedWorkspaceTrainingSpec.highlightTargetFor(NavigationAction.MoveToMedicalCategory)
        )
    }

    @Test
    fun productionSelectionStillIndependentAndRc818Intact() {
        val start = GuidedNavigationController.communicationWorkspaceRoot(GuidedNavigationState())
        assertEquals(0, start.categoryMenuSelection)
        assertTrue(GuidedMedicalCategoryJourneyAuthority.isLesson16StartState(start))
        val main = read("MainActivity.kt")
        assertTrue(main.contains("completeNavigationLessonPhase") || main.contains("verifyTrainingNavigation"))
        assertTrue(main.contains("prepareMedicalJourneyLessonWorkspaceIfNeeded"))
        val ui = read("LisaGuidedModeUi.kt")
        assertTrue(ui.contains("val isSelectedRow = index == categoryMenuSelection"))
        assertFalse(ui.contains("selected = isDestinationRow"))
    }
}
