package com.idworx.lisa

import com.idworx.lisa.features.brain1interactionstandard.model.UniversalInteractionGestures
import com.idworx.lisa.features.explorelisa.ExploreLisaAuthority
import com.idworx.lisa.features.guidedsensitivitylesson.GuidedSensitivityLessonAuthority
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
 * RC8.13 originally appended Explore LISA as the final guided block.
 * RC8.28 removes Explore from the active catalogue; production Explore sequences remain.
 */
class Rc8_13ExploreLisaFinalLessonTest {

    private val navigator = GuidedTrainingNavigator()

    private fun read(pathUnderMainJava: String): String {
        val path = "app/src/main/java/com/idworx/lisa/$pathUnderMainJava"
        return ZeroTouchFileProbe.readProjectFile(path)
            ?: error("Missing source: $path")
    }

    @Test
    fun catalogEndsWithAdjustSensitivityAndExcludesExplore() {
        assertEquals(8, TrainingMetadata.NAVIGATION_LESSON_COUNT)
        assertEquals(8, TrainingLessonCatalog.navigationLessons.size)
        assertEquals(
            GuidedSensitivityLessonAuthority.ID_ADJUST_SENSITIVITY,
            TrainingLessonCatalog.navigationLessons.last().id
        )
        assertEquals(
            NavigationAction.AdjustSensitivity,
            TrainingLessonCatalog.navigationLessons.last().action
        )
        assertTrue(
            TrainingLessonCatalog.navigationLessons.none {
                ExploreLisaAuthority.isExploreLessonId(it.id) ||
                    it.action == NavigationAction.FinishGuidedLearning ||
                    it.action == NavigationAction.ResetSequence
            }
        )
    }

    @Test
    fun completingEveryNavigationLessonReachesCompletion() {
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
    fun productionNavigationPipelineStillOwnsExploreActionsForNormalUse() {
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
    fun introAndFinalCopyRemainAvailableOnExploreAuthority() {
        assertTrue(ExploreLisaAuthority.introSpeech.contains("You already know how to use LISA."))
        assertTrue(ExploreLisaAuthority.introSpeech.contains("same blink sequences"))
        assertTrue(ExploreLisaAuthority.finalSpeech.contains("You've completed Guided Learning."))
        assertTrue(ExploreLisaAuthority.finalSpeech.contains("ready to communicate"))
        assertEquals("Great.", ExploreLisaAuthority.successPhraseForLessonId(ExploreLisaAuthority.ID_OPEN_MENU))
        assertEquals("Good.", ExploreLisaAuthority.successPhraseForLessonId(ExploreLisaAuthority.ID_SELECT_VOICE))
        assertEquals("Finish", ExploreLisaAuthority.FINISH_BUTTON_LABEL)
    }

    @Test
    fun lessonCardFinishHookRemainsForCompatibility() {
        val ui = read("LisaAccessibilityUi.kt")
        assertTrue(ui.contains("onExploreFinishGuidedLearning"))
        assertTrue(ui.contains("FinishGuidedLearning"))
        assertTrue(ui.contains("FINISH_BUTTON_LABEL"))
        val components = read("features/onboardingguide/ui/TrainingComponents.kt")
        assertTrue(components.contains("finishLabel"))
    }

    @Test
    fun finalGuidedLessonIsAdjustSensitivityNotExploreFinish() {
        assertEquals(
            GuidedSensitivityLessonAuthority.LESSON_TITLE,
            GuidedWorkspaceTrainingSpec.lessonCardTitle(
                NavigationAction.AdjustSensitivity,
                LisaUiStrings.forLanguage(PreferredLanguage.English)
            )
        )
        assertEquals(
            "L5 R5",
            GuidedWorkspaceTrainingSpec.lessonCardGestureLabel(NavigationAction.AdjustSensitivity)
        )
    }
}
