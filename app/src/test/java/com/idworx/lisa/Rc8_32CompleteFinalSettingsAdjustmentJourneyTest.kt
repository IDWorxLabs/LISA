package com.idworx.lisa

import com.idworx.lisa.features.adjustmentcommitpolicy.AdjustmentCommitPolicyAuthority
import com.idworx.lisa.features.guidedcategorypagenavigation.CategoryPageNavigationAuthority
import com.idworx.lisa.features.guidedlessonteaching.GuidedLessonPhaseRequiredAction
import com.idworx.lisa.features.guidedlessonteaching.GuidedLessonTeachingSpec
import com.idworx.lisa.features.guidedsensitivitylesson.GuidedSensitivityLessonAuthority
import com.idworx.lisa.features.onboardingguide.lessons.TrainingLessonCatalog
import com.idworx.lisa.features.onboardingguide.model.NavigationAction
import com.idworx.lisa.features.onboardingguide.model.TrainingPhase
import com.idworx.lisa.features.onboardingguide.navigation.GuidedWorkspaceHighlightTarget
import com.idworx.lisa.features.onboardingguide.navigation.GuidedWorkspaceTrainingSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * RC8.32 — Complete Final Settings Adjustment Journey and Guided Learning Exit.
 */
class Rc8_32CompleteFinalSettingsAdjustmentJourneyTest {

    private val sensitivity = GuidedSensitivityLessonAuthority
    private val uiStrings = LisaUiStrings.forLanguage(PreferredLanguage.English)

    private fun read(relative: String): String =
        File("src/main/java/com/idworx/lisa/$relative").readText()

    private fun root(): GuidedNavigationState = GuidedNavigationState()

    private fun fullLesson() = GuidedLessonTeachingSpec.fullPresentationFor(
        NavigationAction.AdjustSensitivity,
        sensitivity.ID_ADJUST_SENSITIVITY,
        uiStrings
    )

    // --- Phase model -----------------------------------------------------------------------------

    @Test
    fun lesson23HasFivePhasesAndRemainsLesson23Of23() {
        val full = fullLesson()
        assertEquals(5, full.phases.size)
        assertEquals(sensitivity.PHASE_MOVE_TO_SETTINGS_PAGE, full.phases[0].id)
        assertEquals(sensitivity.PHASE_OPEN_SETTINGS, full.phases[1].id)
        assertEquals(sensitivity.PHASE_OPEN_SENSITIVITY, full.phases[2].id)
        assertEquals(sensitivity.PHASE_ADJUST_SENSITIVITY, full.phases[3].id)
        assertEquals(sensitivity.PHASE_RETURN_TO_SETTINGS, full.phases[4].id)
        assertEquals(
            GuidedLessonPhaseRequiredAction.MoveToSettingsPage,
            full.phases[0].requiredAction
        )
        assertEquals(
            GuidedLessonPhaseRequiredAction.OpenSettingsAndControls,
            full.phases[1].requiredAction
        )
        assertEquals(
            GuidedLessonPhaseRequiredAction.OpenSensitivitySetting,
            full.phases[2].requiredAction
        )
        assertEquals(
            GuidedLessonPhaseRequiredAction.AdjustSensitivity,
            full.phases[3].requiredAction
        )
        assertEquals(
            GuidedLessonPhaseRequiredAction.ReturnToSettingsAndControls,
            full.phases[4].requiredAction
        )
        assertTrue(full.phases.take(4).all { !it.showCompletionFeedback })
        assertTrue(full.phases[4].showCompletionFeedback)
        assertEquals(sensitivity.ID_ADJUST_SENSITIVITY, TrainingLessonCatalog.navigationLessons.last().id)
        assertEquals(8, TrainingLessonCatalog.navigationLessons.size)
        assertEquals(23, TrainingLessonCatalog.guidedLessonProgress(
            com.idworx.lisa.features.onboardingguide.model.TrainingProgress(
                currentPhase = TrainingPhase.NavigationLesson,
                navigationLessonIndex = 7
            )
        )?.first)
        assertEquals(23, TrainingLessonCatalog.guidedLessonProgress(
            com.idworx.lisa.features.onboardingguide.model.TrainingProgress(
                currentPhase = TrainingPhase.NavigationLesson,
                navigationLessonIndex = 7
            )
        )?.second)
    }

    @Test
    fun phaseCopyAndSequencesAreExplicit() {
        val full = fullLesson()
        assertEquals(sensitivity.PHASE1_TITLE, full.phases[0].title)
        assertEquals(sensitivity.LESSON_CONTEXT, full.phases[0].context)
        assertEquals("Use L0 R4 to move to the next page.", full.phases[0].description)
        assertEquals("L0 R4", full.phases[0].rawGestureLabel)
        assertTrue(full.phases[0].methods.isEmpty())
        assertEquals("Use L5 R5 to open Settings & Controls.", full.phases[1].description)
        assertEquals("L5 R5", full.phases[1].rawGestureLabel)
        assertTrue(full.phases[1].methods.isEmpty())
        assertEquals("Use L2 R0 to open Sensitivity.", full.phases[2].description)
        assertEquals("L2 R0", full.phases[2].rawGestureLabel)
        assertTrue(full.phases[3].description!!.contains("L3 R1"))
        assertTrue(full.phases[3].description!!.contains("L1 R3"))
        assertTrue(full.phases[3].rawGestureLabel.contains("Decrease: L3 R1"))
        assertTrue(full.phases[3].rawGestureLabel.contains("Increase: L1 R3"))
        assertEquals("Use L2 R2 to go back to Settings & Controls.", full.phases[4].description)
        assertEquals("L2 R2", full.phases[4].rawGestureLabel)
        assertEquals(GuidedWorkspaceHighlightTarget.CategoryNextPage, full.phases[0].navigationControlHighlight)
        assertEquals(GuidedWorkspaceHighlightTarget.CategoryRow, full.phases[1].navigationControlHighlight)
        assertEquals(GuidedWorkspaceHighlightTarget.SettingsHubSensitivity, full.phases[2].navigationControlHighlight)
        assertEquals(GuidedWorkspaceHighlightTarget.IncreaseOrDecreaseValue, full.phases[3].navigationControlHighlight)
        assertEquals(GuidedWorkspaceHighlightTarget.Back, full.phases[4].navigationControlHighlight)
    }

    // --- Phase 1 ---------------------------------------------------------------------------------

    @Test
    fun phase1RequiresProductionNextPageNotMoveDown() {
        assertTrue(sensitivity.matchesMoveToSettingsPage(0, 4))
        assertFalse(sensitivity.matchesMoveToSettingsPage(0, 2))
        assertEquals("L0 R4", sensitivity.moveToSettingsPageSequenceLabel())
        val start = GuidedNavigationController.communicationWorkspaceRoot(root()).copy(
            categoryViewportPage = 0,
            categoryViewportPageCount = 2,
            categoryNavigationCause = CategoryNavigationCause.MENU_RESTORE
        )
        assertTrue(sensitivity.isMoveToSettingsPageStartState(start))
        assertTrue(CategoryPageNavigationAuthority.isNextPageStartState(start))
        val main = read("MainActivity.kt")
        val prep = main.substringAfter("ID_ADJUST_SENSITIVITY -> {")
            .substringBefore("else -> Unit")
        assertTrue(prep.contains("categoryViewportPage = 0"))
        assertTrue(prep.contains("coerceAtLeast(2)"))
        assertFalse(prep.contains("openSettingsMenu"))
        assertFalse(prep.contains("openHubSetting"))
    }

    // --- Phase 2–3 production --------------------------------------------------------------------

    @Test
    fun sensitivityL2R0OpensProductionSensitivityWhenSelected() {
        assertEquals(
            SettingsControlKind.Sensitivity,
            SettingsAndControlsHubSequences.hubDirectOpenKindForGesture(2, 0)
        )
        assertEquals("L2 R0", sensitivity.openSensitivitySequenceLabel())
        assertTrue(sensitivity.matchesOpenSensitivity(2, 0))
        assertFalse(sensitivity.matchesOpenSensitivity(1, 1))
        val hub = PreferenceAdjustmentController.openSettingsMenu(root())
        assertEquals(0, hub.settingsHubSelection)
        val opened = GuidedNavigationController.processSequence(
            left = 2,
            right = 0,
            state = hub,
            language = PreferredLanguage.English,
            uiStrings = uiStrings,
            catalogContext = GuidedCatalogContext()
        )
        assertTrue(opened is GuidedSequenceResult.Navigate)
        assertEquals(
            GuidedPreferencesAdjustMode.Sensitivity,
            (opened as GuidedSequenceResult.Navigate).newState.preferencesAdjustMode
        )
        // Generic Select L1 R1 remains an alternate production open.
        val viaSelect = GuidedNavigationController.processSequence(
            left = 1,
            right = 1,
            state = hub,
            language = PreferredLanguage.English,
            uiStrings = uiStrings,
            catalogContext = GuidedCatalogContext()
        )
        assertTrue(viaSelect is GuidedSequenceResult.Navigate)
        assertEquals(
            GuidedPreferencesAdjustMode.Sensitivity,
            (viaSelect as GuidedSequenceResult.Navigate).newState.preferencesAdjustMode
        )
        // Scroll Up still works when another card is selected.
        val onVolume = hub.copy(settingsHubSelection = 2)
        val scrolled = GuidedNavigationController.processSequence(
            left = 2,
            right = 0,
            state = onVolume,
            language = PreferredLanguage.English,
            uiStrings = uiStrings,
            catalogContext = GuidedCatalogContext()
        )
        assertTrue(scrolled is GuidedSequenceResult.Navigate)
        assertEquals(1, (scrolled as GuidedSequenceResult.Navigate).newState.settingsHubSelection)
        assertEquals(
            GuidedPreferencesAdjustMode.SettingsMenu,
            scrolled.newState.preferencesAdjustMode
        )
    }

    // --- Phase 4–5 -------------------------------------------------------------------------------

    @Test
    fun eitherAdjustmentDirectionSatisfiesAndBackReturnsToHub() {
        assertTrue(sensitivity.matchesAdjust(3, 1))
        assertTrue(sensitivity.matchesAdjust(1, 3))
        assertTrue(sensitivity.isAdjustCompleted(3, 4))
        assertTrue(sensitivity.isAdjustCompleted(3, 2))
        assertFalse(sensitivity.isAdjustCompleted(3, 3))
        assertFalse(sensitivity.isAdjustCompleted(10, 10))
        val mid = PreferenceAdjustmentController.openSensitivityAdjust(root(), 4)
        val increased = PreferenceAdjustmentController.increaseAndPersist(mid)
        assertTrue(increased is GuidedSequenceResult.SavePreferencesAdjustment)
        val saved = increased as GuidedSequenceResult.SavePreferencesAdjustment
        assertEquals(5, saved.sensitivityLevel)
        assertEquals(GuidedPreferencesAdjustMode.Sensitivity, saved.newState.preferencesAdjustMode)
        val decreased = PreferenceAdjustmentController.decreaseAndPersist(
            PreferenceAdjustmentController.openSensitivityAdjust(root(), 4)
        ) as GuidedSequenceResult.SavePreferencesAdjustment
        assertEquals(3, decreased.sensitivityLevel)
        val afterBack = PreferenceAdjustmentController.cancelAdjustment(saved.newState)
        assertEquals(GuidedPreferencesAdjustMode.SettingsMenu, afterBack.preferencesAdjustMode)
        assertEquals(5, afterBack.draftSensitivityLevel)
        assertTrue(fullLesson().phases.none {
            it.requiredAction == GuidedLessonPhaseRequiredAction.SaveSensitivity
        })
        assertEquals("L2 R2", sensitivity.backSequenceLabel())
        assertEquals(
            AdjustmentCommitPolicyAuthority.HELPER_CHANGES_SAVE_AUTOMATICALLY,
            "Changes save automatically."
        )
    }

    // --- Success + Training Complete -------------------------------------------------------------

    @Test
    fun wellDoneAndTrainingCompleteOfferStartAndRestart() {
        assertEquals("✓ Well done!", sensitivity.WELL_DONE_TITLE)
        assertTrue(sensitivity.COMPLETION_DETAIL.contains("Sensitivity", ignoreCase = true))
        assertTrue(sensitivity.COMPLETION_DETAIL.contains("Response Time", ignoreCase = true))
        assertTrue(sensitivity.COMPLETION_DETAIL.contains("Speech Volume", ignoreCase = true))
        assertTrue(sensitivity.COMPLETION_DETAIL.contains("Speech Speed", ignoreCase = true))
        assertTrue(sensitivity.COMPLETION_DETAIL.contains("save automatically", ignoreCase = true))
        assertTrue(
            sensitivity.COMPLETION_DETAIL.contains("L2 R2") ||
                sensitivity.COMPLETION_DETAIL.contains("go back", ignoreCase = true)
        )
        assertEquals("Training Complete", sensitivity.TRAINING_COMPLETE_TITLE)
        assertTrue(
            sensitivity.TRAINING_COMPLETE_MESSAGE.contains(
                "Follow the blink sequences shown on each button",
                ignoreCase = true
            )
        )
        assertTrue(
            sensitivity.TRAINING_COMPLETE_ADJUSTMENT_HINT.contains("Response Time", ignoreCase = true)
        )
        assertEquals("Start Communicating", sensitivity.START_COMMUNICATING_LABEL)
        assertEquals("Restart Guided Learning", sensitivity.RESTART_GUIDED_LEARNING_LABEL)
        assertEquals("L0 R3", sensitivity.startCommunicatingSequenceLabel())
        assertEquals("L3 R0", sensitivity.restartGuidedLearningSequenceLabel())
        assertTrue(sensitivity.matchesStartCommunicating(0, 3))
        assertTrue(sensitivity.matchesRestartGuidedLearning(3, 0))
        assertFalse(sensitivity.matchesStartCommunicating(3, 0))
        val welcome = read("features/onboardingguide/ui/TrainingWelcomeScreen.kt")
        assertTrue(welcome.contains("START_COMMUNICATING_LABEL"))
        assertTrue(welcome.contains("RESTART_GUIDED_LEARNING_LABEL"))
        assertTrue(welcome.contains("UniversalEyeTrackingHeader"))
        assertTrue(welcome.contains("onStartCommunicating"))
        assertTrue(welcome.contains("onRestartGuidedLearning"))
        val controller = read("features/onboardingguide/services/TrainingSessionController.kt")
        assertTrue(controller.contains("awaitingCompletionChoice"))
        assertTrue(controller.contains("WELL_DONE_TITLE") || controller.contains("✓ Well done!"))
        assertFalse(
            controller.substringAfter("TrainingPhase.Completion -> {")
                .substringBefore("else -> Unit")
                .contains("onTrainingFinished()")
        )
        val main = read("MainActivity.kt")
        assertTrue(main.contains("handleTrainingCompletionChoice"))
        assertTrue(main.contains("restoreSensitivityLessonPreferenceDelayed"))
        assertTrue(main.contains("MoveToSettingsPage"))
        assertTrue(main.contains("ReturnToSettingsAndControls"))
    }

    // --- Regression ------------------------------------------------------------------------------

    @Test
    fun regressionsPreservePageNavSettingsAndImmediateSave() {
        assertEquals("L0 R4", CategoryPageNavigationAuthority.nextPageSequenceLabel())
        assertTrue(GuidedModeNavigation.isAdjustSettingsEntrySequence(5, 5))
        assertEquals(2 to 0, SettingsAndControlsHubSequences.SENSITIVITY)
        assertEquals("L3 R1", AdjustmentCommitPolicyAuthority.decreaseSequenceLabel())
        assertEquals("L1 R3", AdjustmentCommitPolicyAuthority.increaseSequenceLabel())
        assertEquals("L2 R2", AdjustmentCommitPolicyAuthority.backSequenceLabel())
        assertEquals(
            GuidedWorkspaceTrainingSpec.lessonCardGestureLabel(NavigationAction.NextPage),
            "L0 R4"
        )
        assertEquals(
            com.idworx.lisa.features.onboardingguide.metadata.TrainingMetadata
                .GUIDED_LEARNING_ESSENTIAL_PHRASE_COUNT +
                com.idworx.lisa.features.onboardingguide.metadata.TrainingMetadata
                    .NAVIGATION_LESSON_COUNT,
            23
        )
    }
}
