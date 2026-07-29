package com.idworx.lisa

import com.idworx.lisa.features.brain1interactionstandard.model.UniversalInteractionGestures
import com.idworx.lisa.features.explorelisa.ExploreLisaAuthority
import com.idworx.lisa.features.guidedcategorypagenavigation.CategoryPageNavigationAuthority
import com.idworx.lisa.features.guidedemergencylesson.GuidedEmergencyLessonAuthority
import com.idworx.lisa.features.guidedlessonexecutionauthority.GuidedLessonExecutionAuthority
import com.idworx.lisa.features.guidedsensitivitylesson.GuidedSensitivityLessonAuthority
import com.idworx.lisa.features.onboardingguide.lessons.TrainingLessonCatalog
import com.idworx.lisa.features.onboardingguide.model.NavigationAction
import com.idworx.lisa.features.onboardingguide.model.TrainingPhase
import com.idworx.lisa.features.onboardingguide.model.TrainingProgress
import com.idworx.lisa.features.onboardingguide.navigation.GuidedWorkspaceTrainingSpec
import com.idworx.lisa.features.zerotouchprinciple.audit.ZeroTouchFileProbe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * RC8.27 — Emergency Active L1 R1 stop, live eye tracking under Emergency.
 * Lesson 23 terminal role updated by RC8.28 (Adjust Sensitivity).
 */
class Rc8_27EmergencyStopEyeTrackingAndLesson23TransitionTest {

    private val uiStrings = LisaUiStrings.forLanguage(PreferredLanguage.English)

    private fun read(pathUnderMainJava: String): String {
        val path = "app/src/main/java/com/idworx/lisa/$pathUnderMainJava"
        return ZeroTouchFileProbe.readProjectFile(path)
            ?: error("Missing source: $path")
    }

    // --- Emergency sequences --------------------------------------------------------------------

    @Test
    fun emergencyTriggerConfirmAndStopSequences() {
        assertEquals("L6 R0", GuidedEmergencyLessonAuthority.TRIGGER_SEQUENCE)
        assertEquals("L1 R1", GuidedEmergencyLessonAuthority.CONFIRM_SEQUENCE)
        assertEquals("R1 L1", GuidedEmergencyLessonAuthority.CANCEL_WHILE_ARMED_SEQUENCE)
        assertEquals("L1 R1", GuidedEmergencyLessonAuthority.STOP_WHILE_ACTIVE_SEQUENCE)
        assertEquals("L1 R1", uiStrings.guidedEmergencyStopSequenceLabel)
        assertEquals("R1 L1", uiStrings.guidedConfirmCancelSequenceLabel)
        assertTrue(
            GuidedEmergencyLessonAuthority.isActiveStopSequence(1, 1, listOf(true, false))
        )
        assertFalse(
            GuidedEmergencyLessonAuthority.isActiveStopSequence(1, 1, listOf(false, true))
        )
        assertTrue(UniversalInteractionGestures.isConfirm(1, 1, listOf(true, false)))
        assertTrue(UniversalInteractionGestures.isCancel(1, 1, listOf(false, true)))
    }

    @Test
    fun emergencyActiveUiShowsStopL1R1NotR1L1() {
        val emergency = read("LisaEmergencyUi.kt")
        val alarm = emergency.substringAfter("fun EmergencyAlarmOverlay(")
            .substringBefore("fun Brain1EmergencyConfirmOverlay(")
        assertTrue(alarm.contains("guidedEmergencyStopSequenceLabel"))
        assertFalse(alarm.contains("guidedConfirmCancelSequenceLabel"))
        assertTrue(alarm.contains("stopEmergency"))
        assertTrue(alarm.contains("eyeTrackingStatusWatching"))
        assertTrue(alarm.contains("EmergencyBlinkFeedbackRows"))
        val confirm = emergency.substringAfter("fun Brain1EmergencyConfirmOverlay(")
        assertTrue(confirm.contains("guidedConfirmCancelSequenceLabel"))
    }

    @Test
    fun activeEmergencyStopUsesIsConfirmAndCancelOrStopEmergency() {
        val main = read("MainActivity.kt")
        val stopWinks = main.substringAfter("private fun processActiveEmergencyStopWinks")
            .substringBefore("private fun processSequenceWinks")
        assertTrue(stopWinks.contains("isConfirm("))
        assertFalse(stopWinks.contains("isCancel("))
        assertTrue(stopWinks.contains("recordWinkSide"))
        assertTrue(stopWinks.contains("cancelOrStopEmergency()"))
        // Diagnostics publish after wink increments so counters update live.
        assertTrue(
            stopWinks.indexOf("leftWinks += 1") < stopWinks.indexOf("updateDiagnostics")
        )
        // Active path has priority — processFrame returns after emergency stop processing.
        val processFrame = main.substringAfter("private fun processFrame")
            .substringBefore("private fun processCountdownWinks")
        assertTrue(processFrame.contains("if (emergencyActive)"))
        assertTrue(processFrame.contains("processActiveEmergencyStopWinks"))
        assertTrue(processFrame.contains("return@addOnSuccessListener"))
    }

    @Test
    fun cameraAnalysisRemainsComposedDuringEmergencyActive() {
        val accessibility = read("LisaAccessibilityUi.kt")
        val root = accessibility.substringAfter("Box(modifier = Modifier.fillMaxSize()) {")
            .substringBefore("GlobalEmergencyOverlayLayer(")
        assertTrue(root.contains("RC8.27"))
        assertTrue(root.contains("cameraView()"))
        assertTrue(root.indexOf("if (cameraPermissionGranted)") < root.indexOf("cameraView()"))
        assertTrue(root.indexOf("cameraView()") < root.indexOf("if (!emergencyActive)"))
        // Workspace (not camera) stays gated.
        assertTrue(root.contains("if (!emergencyActive)"))
    }

    @Test
    fun lesson22CompletesOnlyAfterActiveStop() {
        assertEquals(
            GuidedEmergencyLessonAuthority.Phase.AwaitEmergencyStop,
            GuidedEmergencyLessonAuthority.phase(
                emergencyAwaitingConfirm = false,
                emergencyActive = true
            )
        )
        assertFalse(
            GuidedEmergencyLessonAuthority.mayCompleteAfterStop(
                wasEmergencyActive = false,
                isEmergencyActiveNow = false
            )
        )
        assertTrue(
            GuidedEmergencyLessonAuthority.mayCompleteAfterStop(
                wasEmergencyActive = true,
                isEmergencyActiveNow = false
            )
        )
        assertTrue(
            GuidedEmergencyLessonAuthority.isPreExecuteForbiddenAtEntry(
                emergencyAwaitingConfirm = false,
                emergencyActive = false
            )
        )
        val instruction = GuidedWorkspaceTrainingSpec.lessonCardInstruction(
            NavigationAction.TriggerEmergency
        ).orEmpty()
        assertTrue(instruction.contains("L1 R1"))
        assertFalse(GuidedLessonExecutionAuthority.mayRestorePreconditionOnEntry(
            NavigationAction.TriggerEmergency
        ))
    }

    // --- Lesson 23 ------------------------------------------------------------------------------

    @Test
    fun lesson23IsAdjustSensitivityFinalLesson() {
        val lesson = TrainingLessonCatalog.navigationLessons.first {
            it.id == GuidedSensitivityLessonAuthority.ID_ADJUST_SENSITIVITY
        }
        assertEquals(NavigationAction.AdjustSensitivity, lesson.action)
        val title = GuidedWorkspaceTrainingSpec.lessonCardTitle(lesson.action, uiStrings)
        val instruction = GuidedWorkspaceTrainingSpec.lessonCardInstruction(lesson.action).orEmpty()
        val gesture = GuidedWorkspaceTrainingSpec.lessonCardGestureLabel(lesson.action)
        assertEquals("Adjust Sensitivity", title)
        assertFalse(title.contains("Start Communicating", ignoreCase = true))
        assertFalse(instruction.contains("Finish training", ignoreCase = true))
        assertTrue(instruction.contains("labelled blink sequences", ignoreCase = true))
        assertEquals("L5 R5", gesture)
        val progress = TrainingLessonCatalog.guidedLessonProgress(
            TrainingProgress(
                currentPhase = TrainingPhase.NavigationLesson,
                navigationLessonIndex = TrainingLessonCatalog.navigationLessons
                    .indexOfFirst { it.id == GuidedSensitivityLessonAuthority.ID_ADJUST_SENSITIVITY }
            )
        )
        assertEquals(23 to 23, progress)
    }

    @Test
    fun lesson23IsTerminalWithoutExploreFollowOn() {
        assertEquals(
            NavigationAction.AdjustSensitivity,
            TrainingLessonCatalog.navigationLessons.last().action
        )
        assertTrue(
            TrainingLessonCatalog.navigationLessons.none {
                it.id == ExploreLisaAuthority.ID_OPEN_MENU ||
                    it.action == NavigationAction.FinishGuidedLearning
            }
        )
        assertFalse(
            GuidedLessonExecutionAuthority.mayRestorePreconditionOnEntry(
                NavigationAction.AdjustSensitivity
            )
        )
    }

    // --- Regression -----------------------------------------------------------------------------

    @Test
    fun productionMappingsOutsideEmergencyRemainUnchanged() {
        assertEquals("L0 R4", CategoryPageNavigationAuthority.nextPageSequenceLabel())
        assertEquals("L4 R0", CategoryPageNavigationAuthority.previousPageSequenceLabel())
        assertEquals(
            "L0 R2",
            GuidedWorkspaceTrainingSpec.lessonCardGestureLabel(NavigationAction.MoveToMedicalCategory)
        )
        assertEquals(
            "L3 R1",
            GuidedWorkspaceTrainingSpec.lessonCardGestureLabel(NavigationAction.SelectCategory)
        )
        // Outside Emergency, L1 R1 remains Select / Confirm meaning (not stop).
        assertTrue(GuidedModeNavigation.isSelectSequence(1, 1))
        assertTrue(UniversalInteractionGestures.isConfirm(1, 1, listOf(true, false)))
        // Active emergency path alone uses isConfirm for stop — verified in source.
        val main = read("MainActivity.kt")
        assertTrue(main.contains("MAX_EMERGENCY_VOLUME"))
    }
}
