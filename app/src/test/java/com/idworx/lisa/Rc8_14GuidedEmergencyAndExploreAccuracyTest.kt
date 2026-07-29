package com.idworx.lisa

import com.idworx.lisa.features.brain1interactionstandard.model.UniversalInteractionGestures
import com.idworx.lisa.features.explorelisa.ExploreLisaAuthority
import com.idworx.lisa.features.onboardingguide.lessons.TrainingLessonCatalog
import com.idworx.lisa.features.onboardingguide.model.NavigationAction
import com.idworx.lisa.features.onboardingguide.navigation.GuidedWorkspaceTrainingSpec
import com.idworx.lisa.features.universalsequenceexecution.UniversalSequenceExecutionAuthority
import com.idworx.lisa.features.zerotouchprinciple.audit.ZeroTouchFileProbe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * RC8.14 — Emergency volume removal, Stop Emergency R1 L1, Explore accuracy + compact card.
 */
class Rc8_14GuidedEmergencyAndExploreAccuracyTest {

    private fun read(pathUnderMainJava: String): String {
        val path = "app/src/main/java/com/idworx/lisa/$pathUnderMainJava"
        return ZeroTouchFileProbe.readProjectFile(path)
            ?: error("Missing source: $path")
    }

    // --- Emergency volume removed ----------------------------------------------------------------

    @Test
    fun emergencySurfacesHaveNoVolumeAdjustmentControls() {
        val emergency = read("LisaEmergencyUi.kt")
        assertFalse(emergency.contains("EmergencyAlarmVolumeRow"))
        assertFalse(emergency.contains("Alarm volume"))
        assertFalse(emergency.contains("emergencyAlarmVolume"))
        assertFalse(emergency.contains("onDecreaseAlarmVolume"))
        assertFalse(emergency.contains("onIncreaseAlarmVolume"))
        assertFalse(emergency.contains("DECREASE_VALUE_LEFT"))
        assertFalse(emergency.contains("INCREASE_VALUE_LEFT"))
        assertFalse(emergency.contains("100%"))
        val ui = read("LisaAccessibilityUi.kt")
        assertFalse(ui.contains("onDecreaseEmergencyAlarmVolume"))
        assertFalse(ui.contains("onIncreaseEmergencyAlarmVolume"))
        val main = read("MainActivity.kt")
        assertTrue(main.contains("MAX_EMERGENCY_VOLUME"))
        assertFalse(
            main.substringAfter("private fun startEmergencyMode()")
                .substringBefore("private fun cancelOrStopEmergency")
                .contains("emergencyVolume")
        )
    }

    @Test
    fun activeEmergencyDisplaysStopEmergencyWithR1L1() {
        val emergency = read("LisaEmergencyUi.kt")
        val alarm = emergency.substringAfter("fun EmergencyAlarmOverlay(")
            .substringBefore("fun Brain1EmergencyConfirmOverlay(")
        assertTrue(alarm.contains("stopEmergency"))
        assertTrue(alarm.contains("guidedConfirmCancelSequenceLabel"))
        assertEquals("R1 L1", LisaUiStrings.forLanguage(PreferredLanguage.English).guidedConfirmCancelSequenceLabel)
        assertTrue(
            UniversalInteractionGestures.isCancel(
                UniversalInteractionGestures.CONFIRM_LEFT,
                UniversalInteractionGestures.CONFIRM_RIGHT,
                listOf(false, true) // right then left
            )
        )
        assertFalse(
            UniversalInteractionGestures.isCancel(
                UniversalInteractionGestures.CONFIRM_LEFT,
                UniversalInteractionGestures.CONFIRM_RIGHT,
                listOf(true, false) // left then right = confirm, not cancel
            )
        )
    }

    @Test
    fun stopEmergencyTouchAndBlinkShareCancelOrStopEmergency() {
        val emergency = read("LisaEmergencyUi.kt")
        assertTrue(emergency.contains("onStopEmergency = onCancelOrStopEmergency"))
        val main = read("MainActivity.kt")
        assertTrue(main.contains("onCancelOrStopEmergency = { cancelOrStopEmergency() }"))
        assertTrue(main.contains("processActiveEmergencyStopWinks"))
        val stopWinks = main.substringAfter("private fun processActiveEmergencyStopWinks")
            .substringBefore("private fun processSequenceWinks")
        assertTrue(stopWinks.contains("UniversalInteractionGestures"))
        assertTrue(stopWinks.contains("isCancel("))
        assertTrue(stopWinks.contains("cancelOrStopEmergency()"))
        val cancelFn = main.substringAfter("private fun cancelOrStopEmergency() {")
            .substringBefore("\n    private fun ")
        assertTrue(cancelFn.contains("advanceEmergencyLesson"))
        assertTrue(cancelFn.contains("verifyTrainingNavigation(NavigationAction.TriggerEmergency)"))
    }

    // --- Explore Voice / Settings direct destinations --------------------------------------------

    @Test
    fun exploreOpenVoiceRequiresProductionL3R1NotSelect() {
        assertEquals("L3 R1", ExploreLisaAuthority.voiceSequenceLabel())
        assertEquals(3 to 1, ExploreLisaAuthority.voiceGesture())
        assertTrue(ExploreLisaAuthority.matchesVoiceDestination(3, 1))
        assertFalse(ExploreLisaAuthority.matchesVoiceDestination(1, 1))
        assertEquals(
            "L3 R1",
            GuidedWorkspaceTrainingSpec.lessonCardGestureLabel(NavigationAction.OpenVoice)
        )
        val openVoice = TrainingLessonCatalog.navigationLessons
            .first { it.id == ExploreLisaAuthority.ID_OPEN_VOICE }
        assertEquals(NavigationAction.OpenVoice, openVoice.action)
        // Lesson numbers: 15 phrases + explore_open_voice index 10 → lesson 26 of 32
        val progressPair = TrainingLessonCatalog.guidedLessonProgress(
            com.idworx.lisa.features.onboardingguide.model.TrainingProgress(
                currentPhase = com.idworx.lisa.features.onboardingguide.model.TrainingPhase.NavigationLesson,
                navigationLessonIndex = TrainingLessonCatalog.navigationLessons
                    .indexOfFirst { it.id == ExploreLisaAuthority.ID_OPEN_VOICE }
            )
        )
        assertEquals(26 to 32, progressPair)
    }

    @Test
    fun exploreOpenSettingsRequiresProductionL5R5NotSelect() {
        assertEquals("L5 R5", ExploreLisaAuthority.settingsSequenceLabel())
        assertEquals(5 to 5, ExploreLisaAuthority.settingsGesture())
        assertTrue(ExploreLisaAuthority.matchesSettingsDestination(5, 5))
        assertFalse(ExploreLisaAuthority.matchesSettingsDestination(1, 1))
        assertEquals(
            "L5 R5",
            GuidedWorkspaceTrainingSpec.lessonCardGestureLabel(NavigationAction.OpenSettings)
        )
        val openSettingsIndex = TrainingLessonCatalog.navigationLessons
            .indexOfFirst { it.id == ExploreLisaAuthority.ID_OPEN_SETTINGS }
        val progressPair = TrainingLessonCatalog.guidedLessonProgress(
            com.idworx.lisa.features.onboardingguide.model.TrainingProgress(
                currentPhase = com.idworx.lisa.features.onboardingguide.model.TrainingPhase.NavigationLesson,
                navigationLessonIndex = openSettingsIndex
            )
        )
        assertEquals(29 to 32, progressPair)
    }

    @Test
    fun exploreMenuAndFinishSequencesUnchanged() {
        assertEquals("L4 R6", ExploreLisaAuthority.openMenuSequenceLabel())
        assertEquals("L1 R1", ExploreLisaAuthority.finishSequenceLabel())
        assertEquals(
            "L4 R6",
            GuidedWorkspaceTrainingSpec.lessonCardGestureLabel(NavigationAction.OpenMenu)
        )
        assertEquals(
            "L1 R1",
            GuidedWorkspaceTrainingSpec.lessonCardGestureLabel(NavigationAction.FinishGuidedLearning)
        )
        val finishIndex = TrainingLessonCatalog.navigationLessons
            .indexOfFirst { it.id == ExploreLisaAuthority.ID_FINISH }
        val progressPair = TrainingLessonCatalog.guidedLessonProgress(
            com.idworx.lisa.features.onboardingguide.model.TrainingProgress(
                currentPhase = com.idworx.lisa.features.onboardingguide.model.TrainingPhase.NavigationLesson,
                navigationLessonIndex = finishIndex
            )
        )
        assertEquals(32 to 32, progressPair)
        val openMenuIndex = TrainingLessonCatalog.navigationLessons
            .indexOfFirst { it.id == ExploreLisaAuthority.ID_OPEN_MENU }
        assertEquals(
            24 to 32,
            TrainingLessonCatalog.guidedLessonProgress(
                com.idworx.lisa.features.onboardingguide.model.TrainingProgress(
                    currentPhase = com.idworx.lisa.features.onboardingguide.model.TrainingPhase.NavigationLesson,
                    navigationLessonIndex = openMenuIndex
                )
            )
        )
    }

    @Test
    fun exploreCardsWrapContentAndAvoidFullHeight() {
        val components = read("features/onboardingguide/ui/TrainingComponents.kt")
        val card = components.substringAfter("fun GuidedWorkspaceLessonCard(")
            .substringBefore("fun GuidedLessonPhraseTitle(")
        assertTrue(card.contains("compact: Boolean = true") || card.contains("compact: Boolean = false"))
        assertTrue(card.contains("wrapContentHeight()"))
        assertFalse(card.contains("verticalScroll"))
        assertFalse(card.contains("fillMaxHeight()"))
        assertTrue(
            card.contains("widthIn(max = GuidedWorkspaceLessonCardAuthority.MaxCardWidth)") ||
                card.contains("widthIn(max = if (compact) 200.dp else 210.dp)")
        )
        val ui = read("LisaAccessibilityUi.kt")
        // RC8.16 — all real-workspace lessons 16–32 use the shared compact card inside the
        // workspace content Box below UniversalEyeTrackingHeader.
        assertTrue(ui.contains("compact = true"))
        assertTrue(ui.contains("cardDockForLesson("))
        assertTrue(ui.contains("UniversalEyeTrackingHeader("))
        assertTrue(
            ui.indexOf("UniversalEyeTrackingHeader(") <
                ui.indexOf("GuidedWorkspaceLessonCard(")
        )
    }

    @Test
    fun exploreOpensVoiceAndSettingsThroughProductionDestinationShortcuts() {
        val main = read("MainActivity.kt")
        assertTrue(main.contains("matchesVoiceDestination(left, right)"))
        assertTrue(main.contains("matchesSettingsDestination(left, right)"))
        assertTrue(main.contains("MainMenuController.processSequence("))
        assertTrue(main.contains("MainMenuDestinationShortcuts") || 
            read("features/explorelisa/ExploreLisaAuthority.kt").contains("MainMenuDestinationShortcuts"))
        assertTrue(ExploreLisaAuthority.usesOnlyExistingSequences())
        // Production Menu assignments unchanged.
        assertEquals(
            3 to 1,
            MainMenuDestinationShortcuts.gestureForDestination(MainMenuDestination.Voice)
        )
        assertEquals(
            5 to 5,
            MainMenuDestinationShortcuts.gestureForDestination(MainMenuDestination.Settings)
        )
        assertEquals(4, GuidedModeNavigation.OPEN_MAIN_MENU_LEFT)
        assertEquals(6, GuidedModeNavigation.OPEN_MAIN_MENU_RIGHT)
        assertEquals(1, GuidedModeNavigation.SELECT_LEFT)
        assertEquals(1, GuidedModeNavigation.SELECT_RIGHT)
    }

    @Test
    fun rc8_12TouchBlinkParityCatalogStillClean() {
        assertTrue(UniversalSequenceExecutionAuthority.debugValidateAuditedCatalog().isEmpty())
    }

    @Test
    fun exploreIntroCopyIsCompact() {
        assertEquals("You already know how to use LISA.", ExploreLisaAuthority.INTRO_LINE_1)
        assertEquals("The same blink sequences work everywhere.", ExploreLisaAuthority.INTRO_LINE_2)
        assertFalse(ExploreLisaAuthority.instructionFor(NavigationAction.OpenMenu).contains("Let's explore"))
        assertTrue(ExploreLisaAuthority.instructionFor(NavigationAction.OpenMenu).contains("Open the Menu."))
        assertEquals("Open Voice.", ExploreLisaAuthority.instructionFor(NavigationAction.OpenVoice))
        assertEquals("Open Settings.", ExploreLisaAuthority.instructionFor(NavigationAction.OpenSettings))
    }
}
