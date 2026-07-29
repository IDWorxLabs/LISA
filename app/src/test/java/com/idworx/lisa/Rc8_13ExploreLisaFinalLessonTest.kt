package com.idworx.lisa

import com.idworx.lisa.features.brain1interactionstandard.model.UniversalInteractionGestures
import com.idworx.lisa.features.explorelisa.ExploreLisaAuthority
import com.idworx.lisa.features.onboardingguide.lessons.TrainingLessonCatalog
import com.idworx.lisa.features.onboardingguide.metadata.TrainingMetadata
import com.idworx.lisa.features.onboardingguide.model.NavigationAction
import com.idworx.lisa.features.onboardingguide.model.TrainingPhase
import com.idworx.lisa.features.onboardingguide.model.TrainingProgress
import com.idworx.lisa.features.onboardingguide.navigation.GuidedTrainingNavigator
import com.idworx.lisa.features.onboardingguide.navigation.GuidedWorkspaceTrainingSpec
import com.idworx.lisa.features.onboardingguide.state.TrainingEvent
import com.idworx.lisa.features.zerotouchprinciple.audit.ZeroTouchFileProbe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * RC8.13 — Explore LISA final Guided Learning lesson.
 */
class Rc8_13ExploreLisaFinalLessonTest {

    private val navigator = GuidedTrainingNavigator()

    private fun read(pathUnderMainJava: String): String {
        val path = "app/src/main/java/com/idworx/lisa/$pathUnderMainJava"
        return ZeroTouchFileProbe.readProjectFile(path)
            ?: error("Missing source: $path")
    }

    @Test
    fun catalogAppendsExploreAfterWorkspaceLessonsAndEndsWithFinish() {
        assertEquals(17, TrainingMetadata.NAVIGATION_LESSON_COUNT)
        assertEquals(17, TrainingLessonCatalog.navigationLessons.size)
        assertEquals(NavigationAction.ResetSequence, TrainingLessonCatalog.navigationLessonAt(7)?.action)
        assertEquals(ExploreLisaAuthority.ID_OPEN_MENU, TrainingLessonCatalog.navigationLessonAt(8)?.id)
        assertEquals(NavigationAction.OpenMenu, TrainingLessonCatalog.navigationLessonAt(8)?.action)
        assertEquals(NavigationAction.MenuSelectVoice, TrainingLessonCatalog.navigationLessonAt(9)?.action)
        assertEquals(NavigationAction.OpenVoice, TrainingLessonCatalog.navigationLessonAt(10)?.action)
        assertEquals(NavigationAction.BackFromDestination, TrainingLessonCatalog.navigationLessonAt(11)?.action)
        assertEquals(NavigationAction.MenuSelectSettings, TrainingLessonCatalog.navigationLessonAt(12)?.action)
        assertEquals(NavigationAction.OpenSettings, TrainingLessonCatalog.navigationLessonAt(13)?.action)
        assertEquals(NavigationAction.BackFromDestination, TrainingLessonCatalog.navigationLessonAt(14)?.action)
        assertEquals(NavigationAction.CloseMenu, TrainingLessonCatalog.navigationLessonAt(15)?.action)
        assertEquals(
            NavigationAction.FinishGuidedLearning,
            TrainingLessonCatalog.navigationLessonAt(16)?.action
        )
        assertEquals(ExploreLisaAuthority.ID_FINISH, TrainingLessonCatalog.navigationLessons.last().id)
    }

    @Test
    fun completingEveryNavigationLessonIncludingExploreReachesCompletion() {
        var progress = TrainingProgress(
            currentPhase = TrainingPhase.NavigationLesson,
            navigationLessonIndex = 0,
            tutorialStarted = true
        )
        TrainingLessonCatalog.navigationLessons.forEachIndexed { index, lesson ->
            progress = navigator.reduce(
                progress.copy(
                    navigationLessonIndex = index,
                    currentPhase = TrainingPhase.NavigationLesson
                ),
                TrainingEvent.NavigationActionCompleted(lesson.id)
            )
        }
        assertEquals(TrainingPhase.Completion, progress.currentPhase)
        assertTrue(progress.tutorialCompleted)
        assertTrue(progress.certifiedCommunicator)
    }

    @Test
    fun exploreUsesOnlyExistingProductionSequences() {
        assertTrue(ExploreLisaAuthority.usesOnlyExistingSequences())
        assertEquals("L4 R6", ExploreLisaAuthority.openMenuSequenceLabel())
        assertEquals("L0 R2", ExploreLisaAuthority.moveDownSequenceLabel())
        assertEquals("L3 R1", ExploreLisaAuthority.voiceSequenceLabel())
        assertEquals("L5 R5", ExploreLisaAuthority.settingsSequenceLabel())
        assertEquals("L1 R1", ExploreLisaAuthority.selectSequenceLabel())
        assertEquals("L2 R2", ExploreLisaAuthority.backSequenceLabel())
        assertEquals("L2 R2", ExploreLisaAuthority.closeMenuSequenceLabel())
        assertEquals(
            UniversalInteractionGestures.CONFIRM_LEFT to UniversalInteractionGestures.CONFIRM_RIGHT,
            GuidedModeNavigation.SELECT_LEFT to GuidedModeNavigation.SELECT_RIGHT
        )
        assertEquals(
            GuidedWorkspaceTrainingSpec.lessonCardGestureLabel(NavigationAction.OpenMenu),
            MainMenuProductionUiAuthority.openMenuSequenceLabel()
        )
        assertEquals(
            GuidedWorkspaceTrainingSpec.lessonCardGestureLabel(NavigationAction.OpenVoice),
            ExploreLisaAuthority.voiceSequenceLabel()
        )
        assertEquals(
            GuidedWorkspaceTrainingSpec.lessonCardGestureLabel(NavigationAction.OpenSettings),
            ExploreLisaAuthority.settingsSequenceLabel()
        )
        assertEquals(
            GuidedWorkspaceTrainingSpec.lessonCardGestureLabel(NavigationAction.FinishGuidedLearning),
            ExploreLisaAuthority.finishSequenceLabel()
        )
    }

    @Test
    fun productionNavigationPipelineOwnsExploreActions() {
        val main = read("MainActivity.kt")
        assertTrue(main.contains("NavigationAction.OpenMenu"))
        assertTrue(main.contains("openMainMenu()"))
        assertTrue(main.contains("MainMenuController.processSequence("))
        assertTrue(main.contains("verifyTrainingNavigation(NavigationAction.OpenVoice)"))
        assertTrue(main.contains("verifyTrainingNavigation(NavigationAction.OpenSettings)"))
        assertTrue(main.contains("verifyTrainingNavigation(NavigationAction.BackFromDestination)"))
        assertTrue(main.contains("verifyTrainingNavigation(NavigationAction.FinishGuidedLearning)"))
        assertTrue(main.contains("closeAllPanels()"))
        assertTrue(main.contains("backFromMenuDestination()"))
        // No simulated blink / duplicate gesture tables for Explore.
        assertFalse(main.contains("simulateBlink"))
        assertFalse(main.contains("fakeOpenMenu"))
    }

    @Test
    fun exploreDoesNotDuplicateNavigationLogicInLessonLayer() {
        val authority = read("features/explorelisa/ExploreLisaAuthority.kt")
        assertFalse(authority.contains("openPanel("))
        assertFalse(authority.contains("MainMenuController"))
        assertFalse(authority.contains("leftWinks"))
        assertTrue(authority.contains("MainMenuProductionUiAuthority.openMenuSequenceLabel()"))
    }

    @Test
    fun introAndFinalCopyMatchSpec() {
        assertTrue(ExploreLisaAuthority.introSpeech.contains("You already know how to use LISA."))
        assertTrue(ExploreLisaAuthority.introSpeech.contains("same blink sequences"))
        assertTrue(ExploreLisaAuthority.finalSpeech.contains("You've completed Guided Learning."))
        assertTrue(ExploreLisaAuthority.finalSpeech.contains("ready to communicate"))
        assertEquals("Great.", ExploreLisaAuthority.successPhraseForLessonId(ExploreLisaAuthority.ID_OPEN_MENU))
        assertEquals("Good.", ExploreLisaAuthority.successPhraseForLessonId(ExploreLisaAuthority.ID_SELECT_VOICE))
        assertEquals("Finish", ExploreLisaAuthority.FINISH_BUTTON_LABEL)
    }

    @Test
    fun lessonCardSurfacesFinishButtonForFinalStep() {
        val ui = read("LisaAccessibilityUi.kt")
        assertTrue(ui.contains("onExploreFinishGuidedLearning"))
        assertTrue(ui.contains("FinishGuidedLearning"))
        assertTrue(ui.contains("FINISH_BUTTON_LABEL"))
        val components = read("features/onboardingguide/ui/TrainingComponents.kt")
        assertTrue(components.contains("finishLabel"))
        assertTrue(components.contains("onFinish"))
    }

    @Test
    fun existingWorkspaceSequenceCatalogueUnchanged() {
        assertEquals(4, GuidedModeNavigation.OPEN_MAIN_MENU_LEFT)
        assertEquals(6, GuidedModeNavigation.OPEN_MAIN_MENU_RIGHT)
        assertEquals(0, GuidedModeNavigation.NEXT_LEFT)
        assertEquals(2, GuidedModeNavigation.NEXT_RIGHT)
        assertEquals(1, GuidedModeNavigation.SELECT_LEFT)
        assertEquals(1, GuidedModeNavigation.SELECT_RIGHT)
        assertEquals(2, GuidedModeNavigation.BACK_LEFT)
        assertEquals(2, GuidedModeNavigation.BACK_RIGHT)
        assertEquals(0, GuidedModeNavigation.FINISH_TRAINING_LEFT)
        assertEquals(3, GuidedModeNavigation.FINISH_TRAINING_RIGHT)
        assertEquals(3, GuidedModeNavigation.CATEGORIES_LEFT)
        assertEquals(0, GuidedModeNavigation.CATEGORIES_RIGHT)
    }
}
