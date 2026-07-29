package com.idworx.lisa

import com.idworx.lisa.features.guidedlessonexecutionauthority.GuidedLessonExecutionAuthority
import com.idworx.lisa.features.guidedlessonexecutionauthority.audit.GuidedLessonExecutionAuditor
import com.idworx.lisa.features.guidedmedicalcategoryjourney.GuidedMedicalCategoryJourneyAuthority
import com.idworx.lisa.features.guidedworkspacelessoncard.GuidedWorkspaceLessonCardAuthority
import com.idworx.lisa.features.onboardingguide.lessons.TrainingLessonCatalog
import com.idworx.lisa.features.onboardingguide.model.NavigationAction
import com.idworx.lisa.features.onboardingguide.navigation.GuidedWorkspaceTrainingSpec
import com.idworx.lisa.features.zerotouchprinciple.audit.ZeroTouchFileProbe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * RC8.18 — Guided Learning lesson cards never scroll; lessons never pre-execute taught actions;
 * lesson 19 waits for production Back.
 */
class Rc8_18GuidedLessonCardAndStateAuthorityTest {

    private val uiStrings = LisaUiStrings.forLanguage(PreferredLanguage.English)

    private fun read(pathUnderMainJava: String): String {
        val path = "app/src/main/java/com/idworx/lisa/$pathUnderMainJava"
        return ZeroTouchFileProbe.readProjectFile(path)
            ?: error("Missing source: $path")
    }

    @Test
    fun lessonCardsNeverUseInternalScrolling() {
        assertTrue(GuidedLessonExecutionAuditor.lessonCardHasNoInternalScroll())
        val card = read("features/onboardingguide/ui/TrainingComponents.kt")
            .substringAfter("fun GuidedWorkspaceLessonCard(")
            .substringBefore("fun GuidedLessonPhraseTitle(")
        assertFalse(card.contains("verticalScroll"))
        assertFalse(card.contains("rememberScrollState"))
        assertTrue(card.contains("wrapContentHeight()"))
    }

    @Test
    fun lessonCardMaxHeightIsWorkspaceFractionNotFixedScrollCap() {
        assertEquals(0.45f, GuidedWorkspaceLessonCardAuthority.MaxHeightFraction, 0.001f)
        assertEquals(
            GuidedLessonExecutionAuthority.LESSON_CARD_MAX_HEIGHT_FRACTION,
            GuidedWorkspaceLessonCardAuthority.MaxHeightFraction,
            0.001f
        )
        assertTrue(GuidedLessonExecutionAuditor.lessonCardUsesWorkspaceHeightFraction())
        val ui = read("LisaAccessibilityUi.kt")
        assertTrue(ui.contains("BoxWithConstraints"))
        assertTrue(ui.contains("MaxHeightFraction"))
        assertTrue(ui.contains("heightIn(max = lessonCardMaxHeight)"))
    }

    @Test
    fun lesson16CopyFitsWithoutScrollingAndKeepsRequiredMeaning() {
        val instruction = GuidedWorkspaceTrainingSpec.lessonCardInstruction(
            NavigationAction.MoveToMedicalCategory,
            GuidedMedicalCategoryJourneyAuthority.ID_MOVE_TO_MEDICAL
        ).orEmpty()
        assertEquals(GuidedMedicalCategoryJourneyAuthority.MOVE_DESCRIPTION, instruction)
        assertTrue(
            instruction.contains("Scroll", ignoreCase = true) ||
                instruction.contains("open", ignoreCase = true)
        )
        assertFalse(instruction.contains("Next, you'll practise"))
        assertTrue(instruction.length < 180)
    }

    @Test
    fun lesson19BeginsWithMedicalOpenAndWaitsForProductionBack() {
        assertEquals(
            GuidedLessonExecutionAuthority.ID_WORKSPACE_BACK,
            TrainingLessonCatalog.navigationLessons[3].id
        )
        assertEquals(NavigationAction.CloseMenu, TrainingLessonCatalog.navigationLessons[3].action)
        assertEquals(
            "Go back to categories.",
            GuidedWorkspaceTrainingSpec.lessonCardInstruction(
                NavigationAction.CloseMenu,
                GuidedLessonExecutionAuthority.ID_WORKSPACE_BACK
            )
        )
        assertEquals(
            "L2 R2",
            GuidedWorkspaceTrainingSpec.lessonCardGestureLabel(NavigationAction.CloseMenu)
        )

        val medicalOpen = GuidedNavigationController.openCategoryDirectly(
            GuidedNavigationState(),
            GuidedMedicalCategoryJourneyAuthority.medicalCategoryIndex
        )
        assertTrue(GuidedLessonExecutionAuthority.isWorkspaceBackStartState(medicalOpen))

        val afterBack = (GuidedNavigationController.processSequence(
            GuidedModeNavigation.BACK_LEFT,
            GuidedModeNavigation.BACK_RIGHT,
            medicalOpen,
            PreferredLanguage.English,
            uiStrings
        ) as GuidedSequenceResult.Navigate).newState
        assertTrue(
            GuidedLessonExecutionAuthority.isWorkspaceBackCompleted(medicalOpen, afterBack)
        )
        assertEquals(GuidedOverlayScreenMode.CategoryMenu, afterBack.screenMode)
        assertFalse(
            GuidedLessonExecutionAuthority.isWorkspaceBackCompleted(medicalOpen, medicalOpen)
        )
    }

    @Test
    fun lesson19EntryDoesNotPreExecuteBack() {
        assertTrue(GuidedLessonExecutionAuditor.mainActivityDoesNotAutoBackOnLesson19Entry())
        assertTrue(GuidedLessonExecutionAuditor.mainActivityGatesWorkspaceBackOnProductionState())
        assertTrue(GuidedLessonExecutionAuditor.workspaceBackLessonIdIsNavBack())
        assertTrue(GuidedLessonExecutionAuditor.authorityForbidsPreExecutingTaughtAction())
        val main = read("MainActivity.kt")
        assertTrue(main.contains("isWorkspaceBackCompleted"))
        assertTrue(main.contains("isWorkspaceBackStartState"))
        // Touch and blink both gate completion on production Category Selection.
        assertTrue(main.contains("applyGuidedTouchNavigation"))
        val touchBack = main.substringAfter("private fun applyGuidedTouchNavigation(")
            .substringBefore("private fun updateGuidedCategoryViewportPageState")
        assertTrue(touchBack.contains("ID_WORKSPACE_BACK"))
        assertTrue(touchBack.contains("isWorkspaceBackCompleted"))
    }

    @Test
    fun noProductionSequencesChanged() {
        assertEquals(2 to 2, GuidedModeNavigation.BACK_LEFT to GuidedModeNavigation.BACK_RIGHT)
        assertEquals(0 to 2, GuidedModeNavigation.NEXT_LEFT to GuidedModeNavigation.NEXT_RIGHT)
        assertEquals(3 to 1, GuidedCategoryShortcuts.gestureForCategory(2))
        assertEquals(2 to 1, GuidedPageSequences.slotAt(0))
    }

    @Test
    fun headerAndBottomChromeRemainOutsideCardHost() {
        val ui = read("LisaAccessibilityUi.kt")
        val headerIdx = ui.indexOf("UniversalEyeTrackingHeader(")
        val workspaceIdx = ui.indexOf("BoxWithConstraints(")
        val cardIdx = ui.indexOf("GuidedWorkspaceLessonCard(")
        val bottomChromeIdx = ui.indexOf("showWorkspaceBottomChrome")
        assertTrue(headerIdx >= 0 && workspaceIdx > headerIdx)
        assertTrue(cardIdx > workspaceIdx)
        assertTrue(bottomChromeIdx > cardIdx || bottomChromeIdx > workspaceIdx)
    }
}
