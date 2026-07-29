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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * RC8.19 / RC8.23 — Lesson 16 Part 1 guided teaching presentation.
 */
class Rc8_19Lesson16GuidedTeachingPresentationTest {

    private val uiStrings = LisaUiStrings.forLanguage(PreferredLanguage.English)

    private fun read(pathUnderMainJava: String): String {
        val path = "app/src/main/java/com/idworx/lisa/$pathUnderMainJava"
        return ZeroTouchFileProbe.readProjectFile(path)
            ?: error("Missing source: $path")
    }

    @Test
    fun lesson16UsesStructuredMethodsAndSequenceEmphasis() {
        val lesson = TrainingLessonCatalog.navigationLessons[0]
        assertEquals(NavigationAction.MoveToMedicalCategory, lesson.action)
        val teaching = GuidedLessonTeachingSpec.presentationFor(
            lesson.action,
            lesson.id,
            uiStrings,
            phaseIndex = 0
        )
        assertTrue(teaching.usesStructuredNextAction)
        assertTrue(teaching.usesStructuredMethods)
        assertEquals(GuidedMedicalCategoryJourneyAuthority.MOVE_LESSON_TITLE, teaching.title)
        assertEquals("Method 1", teaching.methods.single().title)
        assertEquals(
            GuidedMedicalCategoryJourneyAuthority.MOVE_METHOD_1_BODY,
            teaching.methods.single().instructionalLines.first()
        )
        assertEquals("L0 R2", teaching.methods.single().highlightedSequence)
        assertEquals("L0 R2", teaching.rawGestureLabel)
        assertEquals(2, GuidedLessonTeachingSpec.phasesFor(lesson.action).size)
    }

    @Test
    fun lesson16HighlightsOnlyMoveDownControlNotMedicalDestination() {
        val teaching = GuidedLessonTeachingSpec.presentationFor(
            NavigationAction.MoveToMedicalCategory,
            GuidedMedicalCategoryJourneyAuthority.ID_MOVE_TO_MEDICAL,
            uiStrings,
            phaseIndex = 0
        )
        assertEquals(GuidedWorkspaceHighlightTarget.NextPage, teaching.navigationControlHighlight)
        assertEquals(
            GuidedWorkspaceHighlightTarget.NextPage,
            GuidedWorkspaceTrainingSpec.highlightTargetFor(NavigationAction.MoveToMedicalCategory)
        )
        assertNull(teaching.destinationCategoryIndex)
        assertEquals(
            GuidedMedicalCategoryJourneyAuthority.medicalCategoryIndex,
            teaching.productionTargetCategoryIndex
        )
        val ui = read("LisaGuidedModeUi.kt")
        assertTrue(
            ui.contains(
                "GuidedPanelActionKind.ScrollDown -> highlightTarget == GuidedWorkspaceHighlightTarget.NextPage"
            )
        )
        assertTrue(ui.contains("destinationCategoryIndex"))
        assertTrue(ui.contains("val isSelectedRow = index == categoryMenuSelection"))
        assertFalse(ui.contains("selected = isDestinationRow"))
    }

    @Test
    fun lesson16DoesNotPreExecuteNavigation() {
        val start = GuidedNavigationController.communicationWorkspaceRoot(GuidedNavigationState())
        assertTrue(GuidedMedicalCategoryJourneyAuthority.isLesson16StartState(start))
        assertEquals(0, start.categoryMenuSelection)
        assertNotEquals(
            GuidedMedicalCategoryJourneyAuthority.medicalCategoryIndex,
            start.categoryMenuSelection
        )
        val teaching = GuidedLessonTeachingSpec.presentationFor(
            NavigationAction.MoveToMedicalCategory,
            GuidedMedicalCategoryJourneyAuthority.ID_MOVE_TO_MEDICAL,
            uiStrings
        )
        assertNotNull(teaching.productionTargetCategoryIndex)
        assertNull(teaching.destinationCategoryIndex)
        assertEquals(0, start.categoryMenuSelection)
    }

    @Test
    fun teachingModelIsReusableAndLesson17UsesStructuredDirectOpenTeaching() {
        val lesson17 = GuidedLessonTeachingSpec.presentationFor(
            NavigationAction.SelectCategory,
            GuidedMedicalCategoryJourneyAuthority.ID_OPEN_MEDICAL,
            uiStrings
        )
        assertTrue(lesson17.usesStructuredNextAction)
        assertTrue(lesson17.usesStructuredMethods)
        assertEquals(
            GuidedMedicalCategoryJourneyAuthority.medicalCategoryIndex,
            lesson17.destinationCategoryIndex
        )

        val card = read("features/onboardingguide/ui/TrainingComponents.kt")
        assertTrue(card.contains("teaching:"))
        assertTrue(card.contains("usesStructuredNextAction"))
        assertTrue(card.contains("usesStructuredMethods"))
        assertTrue(card.contains("GuidedLessonTeachingMethod") || card.contains("teaching.methods"))

        val ui = read("LisaAccessibilityUi.kt")
        assertTrue(ui.contains("GuidedLessonTeachingSpec.presentationFor"))
        assertTrue(ui.contains("teaching = guidedLessonTeaching"))
        assertTrue(ui.contains("destinationCategoryIndex = guidedDestinationCategoryIndex"))
    }

    @Test
    fun rc8_18ExecutionAuthorityStillIntact() {
        val main = read("MainActivity.kt")
        assertTrue(main.contains("prepareMedicalJourneyLessonWorkspaceIfNeeded"))
        assertTrue(main.contains("NavigationAction.MoveToMedicalCategory -> {"))
        assertTrue(
            main.contains("verifyTrainingNavigationPhase(NavigationAction.MoveToMedicalCategory)") ||
                main.contains("verifyTrainingNavigation(NavigationAction.MoveToMedicalCategory)")
        )
        assertTrue(
            main.contains("categoryMenuSelection ==") &&
                main.contains("GuidedWorkspaceTrainingSpec.medicalCategoryIndex") ||
                main.contains("isMedicalSelectedInCategoryMenu")
        )
    }
}
