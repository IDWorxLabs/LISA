package com.idworx.lisa

import com.idworx.lisa.features.explorelisa.ExploreLisaAuthority
import com.idworx.lisa.features.guidedcategorypagenavigation.CategoryPageNavigationAuthority
import com.idworx.lisa.features.guidedemergencylesson.GuidedEmergencyLessonAuthority
import com.idworx.lisa.features.guidedlessonexecutionauthority.GuidedLessonExecutionAuthority
import com.idworx.lisa.features.guidedlessonteaching.GuidedLessonPhaseRequiredAction
import com.idworx.lisa.features.guidedlessonteaching.GuidedLessonTeachingSpec
import com.idworx.lisa.features.guidedsensitivitylesson.GuidedCatalogueMigrationRc828
import com.idworx.lisa.features.guidedsensitivitylesson.GuidedSensitivityLessonAuthority
import com.idworx.lisa.features.onboardingguide.lessons.TrainingLessonCatalog
import com.idworx.lisa.features.onboardingguide.metadata.TrainingMetadata
import com.idworx.lisa.features.onboardingguide.model.NavigationAction
import com.idworx.lisa.features.onboardingguide.model.TrainingPhase
import com.idworx.lisa.features.onboardingguide.model.TrainingProgress
import com.idworx.lisa.features.onboardingguide.navigation.GuidedTrainingNavigator
import com.idworx.lisa.features.onboardingguide.navigation.GuidedWorkspaceHighlightTarget
import com.idworx.lisa.features.onboardingguide.navigation.GuidedWorkspaceTrainingSpec
import com.idworx.lisa.features.onboardingguide.state.TrainingEvent
import com.idworx.lisa.features.zerotouchprinciple.audit.ZeroTouchFileProbe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * RC8.28 — Balanced Emergency Confirm/Cancel + final Adjust Sensitivity lesson (23 of 23).
 */
class Rc8_28BalancedEmergencyAndAdjustSensitivityLessonTest {

    private val uiStrings = LisaUiStrings.forLanguage(PreferredLanguage.English)
    private val sensitivity = GuidedSensitivityLessonAuthority
    private val navigator = GuidedTrainingNavigator()

    private fun read(pathUnderMainJava: String): String {
        val path = "app/src/main/java/com/idworx/lisa/$pathUnderMainJava"
        return ZeroTouchFileProbe.readProjectFile(path)
            ?: error("Missing source: $path")
    }

    // --- Emergency confirmation ------------------------------------------------------------------

    @Test
    fun armedEmergencyShowsConfirmAndCancelEqually() {
        val emergency = read("LisaEmergencyUi.kt")
        val confirm = emergency.substringAfter("fun Brain1EmergencyConfirmOverlay(")
            .substringBefore("@Composable\nprivate fun EmergencyBlinkFeedbackRows")
        assertTrue(confirm.contains("confirmEmergency"))
        assertTrue(confirm.contains("guidedConfirmSequenceLabel"))
        assertTrue(confirm.contains("cancelEmergency"))
        assertTrue(confirm.contains("guidedConfirmCancelSequenceLabel"))
        assertTrue(
            confirm.indexOf("confirmEmergency") < confirm.indexOf("cancelEmergency")
        )
        assertEquals("Confirm Emergency", uiStrings.confirmEmergency)
        assertEquals("L1 R1", uiStrings.guidedConfirmSequenceLabel)
        assertEquals("R1 L1", uiStrings.guidedConfirmCancelSequenceLabel)
        assertTrue(confirm.contains("EmergencyBlinkFeedbackRows"))
        assertTrue(confirm.contains("EmergencyManualButton"))
        // Confirm is a control, not only paragraph text.
        assertTrue(confirm.contains("onConfirmEmergency"))
        assertTrue(confirm.contains("onCancelEmergency"))
    }

    @Test
    fun confirmAndCancelUseProductionPathsAndLesson22Gates() {
        val main = read("MainActivity.kt")
        assertTrue(main.contains("confirmArmedEmergencyFromTouch()"))
        assertTrue(main.contains("startEmergencyMode()"))
        assertTrue(
            GuidedEmergencyLessonAuthority.mayCompleteAfterStop(
                wasEmergencyActive = true,
                isEmergencyActiveNow = false
            )
        )
        assertFalse(
            GuidedEmergencyLessonAuthority.mayCompleteAfterStop(
                wasEmergencyActive = false,
                isEmergencyActiveNow = false
            )
        )
        assertEquals("L1 R1", GuidedEmergencyLessonAuthority.CONFIRM_SEQUENCE)
        assertEquals("R1 L1", GuidedEmergencyLessonAuthority.CANCEL_WHILE_ARMED_SEQUENCE)
        assertEquals("L1 R1", GuidedEmergencyLessonAuthority.STOP_WHILE_ACTIVE_SEQUENCE)
        // RC8.27 camera composition under Emergency Active remains.
        val accessibility = read("LisaAccessibilityUi.kt")
        assertTrue(accessibility.contains("RC8.27"))
        assertTrue(accessibility.contains("cameraView()"))
    }

    // --- Catalogue -------------------------------------------------------------------------------

    @Test
    fun guidedCatalogueContainsExactly23Lessons() {
        assertEquals(8, TrainingMetadata.NAVIGATION_LESSON_COUNT)
        assertEquals(8, TrainingLessonCatalog.navigationLessons.size)
        val progress = TrainingLessonCatalog.guidedLessonProgress(
            TrainingProgress(
                currentPhase = TrainingPhase.NavigationLesson,
                navigationLessonIndex = 0
            )
        )
        assertEquals(16 to 23, progress)
        val finalProgress = TrainingLessonCatalog.guidedLessonProgress(
            TrainingProgress(
                currentPhase = TrainingPhase.NavigationLesson,
                navigationLessonIndex = 7
            )
        )
        assertEquals(23 to 23, finalProgress)
        assertNull(TrainingLessonCatalog.navigationLessonAt(8))
        assertEquals(0, TrainingLessonCatalog.navigationLessons.count {
            ExploreLisaAuthority.isExploreLessonId(it.id)
        })
        assertTrue(
            TrainingLessonCatalog.navigationLessons.none {
                it.action == NavigationAction.FinishGuidedLearning ||
                    it.action == NavigationAction.ResetSequence ||
                    it.id == "nav_reset"
            }
        )
    }

    @Test
    fun lessons1Through22RetainMeaningAndLesson23IsSensitivity() {
        val nav = TrainingLessonCatalog.navigationLessons
        assertEquals(NavigationAction.MoveToMedicalCategory, nav[0].action)
        assertEquals(NavigationAction.SelectCategory, nav[1].action)
        assertEquals(NavigationAction.SelectPhrase, nav[2].action)
        assertEquals(NavigationAction.CloseMenu, nav[3].action)
        assertEquals(NavigationAction.NextPage, nav[4].action)
        assertEquals(NavigationAction.PreviousPage, nav[5].action)
        assertEquals(NavigationAction.TriggerEmergency, nav[6].action)
        assertEquals(NavigationAction.AdjustSensitivity, nav[7].action)
        assertEquals(sensitivity.ID_ADJUST_SENSITIVITY, nav[7].id)
        assertEquals(
            sensitivity.LESSON_TITLE,
            GuidedWorkspaceTrainingSpec.lessonCardTitle(nav[7].action, uiStrings)
        )
        assertEquals(nav.size, nav.map { it.id }.toSet().size)
    }

    @Test
    fun completingLesson23CompletesGuidedLearning() {
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

    // --- Lesson 23 phases ------------------------------------------------------------------------

    @Test
    fun lesson23IntroductionAndPhaseModel() {
        val intro = GuidedWorkspaceTrainingSpec.lessonCardInstruction(
            NavigationAction.AdjustSensitivity
        ).orEmpty()
        assertTrue(intro.contains("Sensitivity", ignoreCase = true))
        assertTrue(intro.contains("Response Time", ignoreCase = true))
        assertFalse(intro.contains("memorise", ignoreCase = true))
        val full = GuidedLessonTeachingSpec.fullPresentationFor(
            NavigationAction.AdjustSensitivity,
            sensitivity.ID_ADJUST_SENSITIVITY,
            uiStrings
        )
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
        assertTrue(
            full.phases.none { it.requiredAction == GuidedLessonPhaseRequiredAction.SaveSensitivity }
        )
        assertEquals(GuidedWorkspaceHighlightTarget.CategoryNextPage, full.phases[0].navigationControlHighlight)
        assertEquals(GuidedWorkspaceHighlightTarget.CategoryRow, full.phases[1].navigationControlHighlight)
        assertEquals(GuidedWorkspaceHighlightTarget.SettingsHubSensitivity, full.phases[2].navigationControlHighlight)
        assertEquals(GuidedWorkspaceHighlightTarget.IncreaseOrDecreaseValue, full.phases[3].navigationControlHighlight)
        assertEquals(GuidedWorkspaceHighlightTarget.Back, full.phases[4].navigationControlHighlight)
        assertEquals("L0 R4", sensitivity.moveToSettingsPageSequenceLabel())
        assertEquals("L5 R5", sensitivity.openSettingsSequenceLabel())
        assertEquals("L2 R0", sensitivity.openSensitivitySequenceLabel())
        assertEquals("L1 R3", sensitivity.increaseSequenceLabel())
        assertFalse(
            GuidedLessonExecutionAuthority.mayRestorePreconditionOnEntry(
                NavigationAction.AdjustSensitivity
            )
        )
    }

    @Test
    fun lesson23ProductionSensitivityJourneyHelpers() {
        assertTrue(sensitivity.matchesOpenSettings(5, 5))
        assertTrue(GuidedModeNavigation.isAdjustSettingsEntrySequence(5, 5))
        assertTrue(sensitivity.matchesOpenSensitivity(2, 0))
        assertTrue(sensitivity.matchesIncrease(1, 3))
        assertTrue(sensitivity.matchesDecrease(3, 1))
        assertTrue(sensitivity.matchesAdjust(1, 3))
        assertTrue(sensitivity.matchesAdjust(3, 1))
        assertEquals(MAX_SENSITIVITY_LEVEL, sensitivity.practiceStartingSensitivity(MAX_SENSITIVITY_LEVEL))
        assertEquals(3, sensitivity.practiceStartingSensitivity(3))
        assertEquals(4, sensitivity.expectedSensitivityAfterIncrease(3))
        assertTrue(sensitivity.isIncreaseCompleted(beforeDraft = 3, afterDraft = 4, startLevel = 3))
        assertTrue(sensitivity.isAdjustCompleted(beforeDraft = 3, afterDraft = 4))
        assertTrue(sensitivity.isAdjustCompleted(beforeDraft = 3, afterDraft = 2))
        assertFalse(sensitivity.isAdjustCompleted(beforeDraft = 3, afterDraft = 3))
        val open = PreferenceAdjustmentController.openSettingsMenu(GuidedNavigationState())
        assertEquals(GuidedPreferencesAdjustMode.SettingsMenu, open.preferencesAdjustMode)
        assertTrue(sensitivity.isSettingsHubOpen(open))
        assertFalse(sensitivity.isSensitivityAdjustmentOpen(open))
    }

    @Test
    fun lesson23EntryDoesNotPreOpenSettingsOrSensitivity() {
        val main = read("MainActivity.kt")
        val prep = main.substringAfter("ID_ADJUST_SENSITIVITY -> {")
            .substringBefore("else -> Unit")
        assertTrue(prep.contains("communicationWorkspaceRoot"))
        assertTrue(prep.contains("practiceStartingSensitivity"))
        assertFalse(prep.contains("openSettingsMenu"))
        assertFalse(prep.contains("GuidedPreferencesAdjustMode.Sensitivity"))
        assertFalse(prep.contains("applySensitivityLevel(practiceStart, persist = true)"))
        assertTrue(prep.contains("persist = false") || prep.contains("persist=false"))
        assertTrue(main.contains("restoreSensitivityLessonPreferenceIfNeeded"))
        assertTrue(main.contains("handleAdjustSensitivityLessonPhase"))
    }

    @Test
    fun completionCopyAndStartUsingLisa() {
        assertEquals("Training Complete", sensitivity.TRAINING_COMPLETE_TITLE)
        assertTrue(
            sensitivity.TRAINING_COMPLETE_MESSAGE.contains(
                "Follow the blink sequences shown on each button",
                ignoreCase = true
            )
        )
        assertEquals("Start Communicating", sensitivity.START_COMMUNICATING_LABEL)
        assertEquals("Restart Guided Learning", sensitivity.RESTART_GUIDED_LEARNING_LABEL)
        val welcome = read("features/onboardingguide/ui/TrainingWelcomeScreen.kt")
        assertTrue(welcome.contains("TRAINING_COMPLETE_TITLE"))
        assertTrue(welcome.contains("TRAINING_COMPLETE_MESSAGE"))
        assertTrue(welcome.contains("START_COMMUNICATING_LABEL") || welcome.contains("START_USING_LISA_LABEL"))
        assertTrue(welcome.contains("RESTART_GUIDED_LEARNING_LABEL"))
        val controller = read("features/onboardingguide/services/TrainingSessionController.kt")
        assertTrue(controller.contains("✓ Well done!") || controller.contains("WELL_DONE_TITLE"))
        assertTrue(controller.contains("COMPLETION_DETAIL") || controller.contains("TRAINING_COMPLETE"))
    }

    // --- Migration -------------------------------------------------------------------------------

    @Test
    fun migrationMapsOldLessons23Through32ToNewLesson23() {
        val midExplore = GuidedCatalogueMigrationRc828.migrate(
            TrainingProgress(
                currentPhase = TrainingPhase.NavigationLesson,
                navigationLessonIndex = 10
            )
        )
        assertEquals(7, midExplore.navigationLessonIndex)
        assertEquals(TrainingPhase.NavigationLesson, midExplore.currentPhase)
        val oldReset = GuidedCatalogueMigrationRc828.migrate(
            TrainingProgress(
                currentPhase = TrainingPhase.NavigationLesson,
                navigationLessonIndex = 7
            )
        )
        assertEquals(7, oldReset.navigationLessonIndex)
        val early = GuidedCatalogueMigrationRc828.migrate(
            TrainingProgress(
                currentPhase = TrainingPhase.NavigationLesson,
                navigationLessonIndex = 3
            )
        )
        assertEquals(3, early.navigationLessonIndex)
        val completed = GuidedCatalogueMigrationRc828.migrate(
            TrainingProgress(
                tutorialCompleted = true,
                certifiedCommunicator = true,
                currentPhase = TrainingPhase.Completion,
                navigationLessonIndex = 16
            )
        )
        assertTrue(completed.tutorialCompleted)
        assertEquals(TrainingPhase.Completion, completed.currentPhase)
        assertTrue(completed.navigationLessonIndex in 0 until TrainingMetadata.NAVIGATION_LESSON_COUNT)
        val invalid = GuidedCatalogueMigrationRc828.migrate(
            TrainingProgress(
                currentPhase = TrainingPhase.NavigationLesson,
                navigationLessonIndex = 99
            )
        )
        assertEquals(7, invalid.navigationLessonIndex)
        assertNotNull(TrainingLessonCatalog.navigationLessonAt(invalid.navigationLessonIndex))
        val store = read("features/onboardingguide/services/TrainingProgressStore.kt")
        assertTrue(store.contains("GuidedCatalogueMigrationRc828"))
    }

    // --- Regression ------------------------------------------------------------------------------

    @Test
    fun lessons16Through22SequencesRemainIntact() {
        assertEquals(
            "L0 R2",
            GuidedWorkspaceTrainingSpec.lessonCardGestureLabel(NavigationAction.MoveToMedicalCategory)
        )
        assertEquals(
            "L3 R1",
            GuidedWorkspaceTrainingSpec.lessonCardGestureLabel(NavigationAction.SelectCategory)
        )
        assertEquals(
            "L2 R2",
            GuidedWorkspaceTrainingSpec.lessonCardGestureLabel(NavigationAction.CloseMenu)
        )
        assertEquals("L0 R4", CategoryPageNavigationAuthority.nextPageSequenceLabel())
        assertEquals("L4 R0", CategoryPageNavigationAuthority.previousPageSequenceLabel())
        assertEquals(
            "L6 R0",
            GuidedWorkspaceTrainingSpec.lessonCardGestureLabel(NavigationAction.TriggerEmergency)
        )
        assertEquals(
            NavigationAction.TriggerEmergency,
            TrainingLessonCatalog.navigationLessons[6].action
        )
        assertNotEquals(
            ExploreLisaAuthority.ID_FINISH,
            TrainingLessonCatalog.navigationLessons.last().id
        )
        // Production Explore destinations remain available outside guided catalogue.
        assertEquals("L5 R5", ExploreLisaAuthority.settingsSequenceLabel())
        assertTrue(ExploreLisaAuthority.usesOnlyExistingSequences())
    }
}
