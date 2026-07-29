package com.idworx.lisa

import com.idworx.lisa.features.adjustmentcommitpolicy.AdjustableSettingKind
import com.idworx.lisa.features.adjustmentcommitpolicy.AdjustmentCommitPolicy
import com.idworx.lisa.features.adjustmentcommitpolicy.AdjustmentCommitPolicyAuthority
import com.idworx.lisa.features.adjustmentcommitpolicy.AdjustmentDirection
import com.idworx.lisa.features.guidedlessonteaching.GuidedLessonPhaseRequiredAction
import com.idworx.lisa.features.guidedlessonteaching.GuidedLessonTeachingSpec
import com.idworx.lisa.features.guidedsensitivitylesson.GuidedSensitivityLessonAuthority
import com.idworx.lisa.features.onboardingguide.lessons.TrainingLessonCatalog
import com.idworx.lisa.features.onboardingguide.model.NavigationAction
import com.idworx.lisa.features.onboardingguide.navigation.GuidedWorkspaceTrainingSpec
import com.idworx.lisa.features.zerotouchprinciple.audit.ZeroTouchFileProbe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * RC8.30 — IMMEDIATE_SAVE for Sensitivity, Response Time, Speech Volume, Speech Speed.
 */
class Rc8_30ImmediateSaveAllAdjustableSettingsTest {

    private val uiStrings = LisaUiStrings.forLanguage(PreferredLanguage.English)
    private val sensitivity = GuidedSensitivityLessonAuthority
    private val policy = AdjustmentCommitPolicyAuthority

    private fun read(pathUnderMainJava: String): String {
        val path = "app/src/main/java/com/idworx/lisa/$pathUnderMainJava"
        return ZeroTouchFileProbe.readProjectFile(path)
            ?: error("Missing source: $path")
    }

    private fun root() = GuidedNavigationState()

    private fun process(
        left: Int,
        right: Int,
        state: GuidedNavigationState,
        blinkOrder: List<Boolean> = emptyList()
    ): GuidedSequenceResult =
        GuidedNavigationController.processSequence(
            left = left,
            right = right,
            state = state,
            language = PreferredLanguage.English,
            uiStrings = uiStrings,
            catalogContext = GuidedCatalogContext(
                responseTimeSec = state.draftResponseTimeSec.takeIf { it > 0 }
                    ?: SequenceProcessingDelay.DEFAULT_SECONDS,
                sensitivityLevel = state.draftSensitivityLevel.takeIf { it > 0 }
                    ?: DEFAULT_SENSITIVITY_LEVEL,
                speechVolumeLevel = state.draftSpeechVolumeLevel.takeIf { it > 0 }
                    ?: SpeechVolumeAuthority.DEFAULT_LEVEL,
                speechSpeedLevel = state.draftSpeechSpeedLevel.takeIf { it > 0 }
                    ?: SpeechSpeedAuthority.DEFAULT_LEVEL
            ),
            blinkOrder = blinkOrder
        )

    // --- Shared policy ---------------------------------------------------------------------------

    @Test
    fun allFourSettingsUseImmediateSavePolicy() {
        assertEquals(
            AdjustmentCommitPolicy.IMMEDIATE_SAVE,
            policy.policyFor(GuidedPreferencesAdjustMode.Sensitivity)
        )
        assertEquals(
            AdjustmentCommitPolicy.IMMEDIATE_SAVE,
            policy.policyFor(GuidedPreferencesAdjustMode.ResponseTime)
        )
        assertEquals(
            AdjustmentCommitPolicy.IMMEDIATE_SAVE,
            policy.policyFor(GuidedPreferencesAdjustMode.SpeechVolume)
        )
        assertEquals(
            AdjustmentCommitPolicy.IMMEDIATE_SAVE,
            policy.policyFor(GuidedPreferencesAdjustMode.SpeechSpeed)
        )
        assertTrue(policy.allAdjustableSettingsUseImmediateSave())
        assertEquals("L1 R3", policy.increaseSequenceLabel())
        assertEquals("L3 R1", policy.decreaseSequenceLabel())
        assertEquals("L2 R2", policy.backSequenceLabel())
    }

    @Test
    fun noAdjustmentScreenRequiresSeparateSaveOrConfirmation() {
        val guided = read("LisaGuidedMode.kt")
        assertTrue(guided.contains("decreaseAndPersist"))
        assertTrue(guided.contains("increaseAndPersist"))
        assertTrue(
            guided.contains("GuidedModeNavigation.isSelectSequence(left, right) ->") &&
                guided.substringAfter("private fun processValueAdjustmentGesture")
                    .substringBefore("private fun processSaveConfirmationGesture")
                    .contains("GuidedSequenceResult.Unmatched")
        )
        val ui = read("LisaGuidedModeUi.kt")
        assertTrue(ui.contains("guidedChangesSaveAutomatically"))
        assertFalse(
            ui.substringAfter("private fun SharedSettingAdjustmentPanel(")
                .substringBefore("private fun PreferencesAdjustmentPanel(")
                .contains("onSave")
        )
        assertFalse(
            ui.substringAfter("private fun SharedSettingAdjustmentPanel(")
                .substringBefore("@Composable\nprivate fun SettingAdjustmentMeter(")
                .contains("guidedSaveSensitivity")
        )
    }

    // --- Sensitivity -----------------------------------------------------------------------------

    @Test
    fun sensitivityIncreaseAndDecreasePersistImmediatelyAndStayOpen() {
        val opened = PreferenceAdjustmentController.openSensitivityAdjust(root(), 4)
        val increased = PreferenceAdjustmentController.increaseAndPersist(opened)
        assertTrue(increased is GuidedSequenceResult.SavePreferencesAdjustment)
        val inc = increased as GuidedSequenceResult.SavePreferencesAdjustment
        assertEquals(5, inc.sensitivityLevel)
        assertEquals(GuidedPreferencesAdjustMode.Sensitivity, inc.newState.preferencesAdjustMode)
        assertEquals(5, inc.newState.draftSensitivityLevel)

        val decreased = PreferenceAdjustmentController.decreaseAndPersist(inc.newState)
        val dec = decreased as GuidedSequenceResult.SavePreferencesAdjustment
        assertEquals(4, dec.sensitivityLevel)
        assertEquals(GuidedPreferencesAdjustMode.Sensitivity, dec.newState.preferencesAdjustMode)

        val atMax = PreferenceAdjustmentController.openSensitivityAdjust(root(), MAX_SENSITIVITY_LEVEL)
        val blocked = PreferenceAdjustmentController.increaseAndPersist(atMax)
        assertTrue(blocked is GuidedSequenceResult.Navigate)
        assertEquals(MAX_SENSITIVITY_LEVEL, (blocked as GuidedSequenceResult.Navigate).newState.draftSensitivityLevel)

        val viaBlink = process(
            GuidedModeNavigation.INCREASE_VALUE_LEFT,
            GuidedModeNavigation.INCREASE_VALUE_RIGHT,
            opened
        )
        assertTrue(viaBlink is GuidedSequenceResult.SavePreferencesAdjustment)
        assertEquals(5, (viaBlink as GuidedSequenceResult.SavePreferencesAdjustment).sensitivityLevel)
    }

    @Test
    fun sensitivityHasNoSaveL1R1AndBackPreservesValue() {
        val opened = PreferenceAdjustmentController.openSensitivityAdjust(root(), 3)
        val afterInc = PreferenceAdjustmentController.increaseAndPersist(opened)
            as GuidedSequenceResult.SavePreferencesAdjustment
        val select = process(
            GuidedModeNavigation.SELECT_LEFT,
            GuidedModeNavigation.SELECT_RIGHT,
            afterInc.newState
        )
        assertEquals(GuidedSequenceResult.Unmatched, select)
        val back = process(
            GuidedModeNavigation.BACK_LEFT,
            GuidedModeNavigation.BACK_RIGHT,
            afterInc.newState
        ) as GuidedSequenceResult.Navigate
        assertEquals(GuidedPreferencesAdjustMode.SettingsMenu, back.newState.preferencesAdjustMode)
        // Re-open shows the already-persisted draft (aligned originals).
        val reopen = PreferenceAdjustmentController.openSensitivityAdjust(
            back.newState,
            afterInc.sensitivityLevel!!
        )
        assertEquals(4, reopen.draftSensitivityLevel)
    }

    // --- Response Time ---------------------------------------------------------------------------

    @Test
    fun responseTimeIncreaseDecreasePersistAndRespectBounds() {
        val opened = PreferenceAdjustmentController.openResponseTimeAdjust(root(), 5)
        val increased = PreferenceAdjustmentController.increaseAndPersist(opened)
            as GuidedSequenceResult.SavePreferencesAdjustment
        assertEquals(6, increased.responseTimeSec)
        assertEquals(GuidedPreferencesAdjustMode.ResponseTime, increased.newState.preferencesAdjustMode)

        val decreased = PreferenceAdjustmentController.decreaseAndPersist(increased.newState)
            as GuidedSequenceResult.SavePreferencesAdjustment
        assertEquals(5, decreased.responseTimeSec)

        val atMin = PreferenceAdjustmentController.openResponseTimeAdjust(
            root(),
            SequenceProcessingDelay.MIN_SECONDS
        )
        val blocked = PreferenceAdjustmentController.decreaseAndPersist(atMin)
        assertTrue(blocked is GuidedSequenceResult.Navigate)

        assertEquals("L3 R1", policy.decreaseSequenceLabel())
        assertEquals("L1 R3", policy.increaseSequenceLabel())
        assertNull(
            PreferenceAdjustmentController.beginSaveConfirmation(opened).preferencesAdjustMode
                .takeIf { it == GuidedPreferencesAdjustMode.ConfirmSaveResponseTime }
        )
        assertEquals(
            GuidedPreferencesAdjustMode.ResponseTime,
            PreferenceAdjustmentController.beginSaveConfirmation(opened).preferencesAdjustMode
        )
    }

    // --- Speech Volume ---------------------------------------------------------------------------

    @Test
    fun speechVolumePersistsImmediatelyAndRespectsBounds() {
        val opened = PreferenceAdjustmentController.openSpeechVolumeAdjust(root(), 5)
        val increased = PreferenceAdjustmentController.increaseAndPersist(opened)
            as GuidedSequenceResult.SavePreferencesAdjustment
        assertEquals(6, increased.speechVolumeLevel)
        assertEquals(GuidedPreferencesAdjustMode.SpeechVolume, increased.newState.preferencesAdjustMode)

        val atMin = PreferenceAdjustmentController.openSpeechVolumeAdjust(
            root(),
            SpeechVolumeAuthority.MIN_LEVEL
        )
        assertTrue(
            PreferenceAdjustmentController.decreaseAndPersist(atMin) is GuidedSequenceResult.Navigate
        )
        val atMax = PreferenceAdjustmentController.openSpeechVolumeAdjust(
            root(),
            SpeechVolumeAuthority.MAX_LEVEL
        )
        assertTrue(
            PreferenceAdjustmentController.increaseAndPersist(atMax) is GuidedSequenceResult.Navigate
        )
        assertEquals("10%", SpeechVolumeAuthority.percentLabel(SpeechVolumeAuthority.MIN_LEVEL))
        assertEquals("100%", SpeechVolumeAuthority.percentLabel(SpeechVolumeAuthority.MAX_LEVEL))
    }

    // --- Speech Speed ----------------------------------------------------------------------------

    @Test
    fun speechSpeedPersistsImmediatelyAndRespectsBounds() {
        val opened = PreferenceAdjustmentController.openSpeechSpeedAdjust(root(), 3)
        val increased = PreferenceAdjustmentController.increaseAndPersist(opened)
            as GuidedSequenceResult.SavePreferencesAdjustment
        assertEquals(4, increased.speechSpeedLevel)
        assertEquals(GuidedPreferencesAdjustMode.SpeechSpeed, increased.newState.preferencesAdjustMode)

        val decreased = PreferenceAdjustmentController.decreaseAndPersist(increased.newState)
            as GuidedSequenceResult.SavePreferencesAdjustment
        assertEquals(3, decreased.speechSpeedLevel)

        val atMin = PreferenceAdjustmentController.openSpeechSpeedAdjust(
            root(),
            SpeechSpeedAuthority.MIN_LEVEL
        )
        assertTrue(
            PreferenceAdjustmentController.decreaseAndPersist(atMin) is GuidedSequenceResult.Navigate
        )
        val atMax = PreferenceAdjustmentController.openSpeechSpeedAdjust(
            root(),
            SpeechSpeedAuthority.MAX_LEVEL
        )
        assertTrue(
            PreferenceAdjustmentController.increaseAndPersist(atMax) is GuidedSequenceResult.Navigate
        )
    }

    // --- Lesson 23 -------------------------------------------------------------------------------

    @Test
    fun lesson23TeachesSensitivityImmediateSaveWithoutSavePhase() {
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
        assertTrue(
            full.phases.none {
                it.requiredAction == GuidedLessonPhaseRequiredAction.SaveSensitivity
            }
        )
        assertEquals(
            GuidedLessonPhaseRequiredAction.AdjustSensitivity,
            full.phases[3].requiredAction
        )
        assertTrue(full.phases[4].showCompletionFeedback)
        assertTrue(full.phases[0].description!!.contains("L0 R4") ||
            full.phases[0].context!!.contains("Sensitivity"))
        assertTrue(
            sensitivity.COMPLETION_DETAIL.contains("Response Time", ignoreCase = true)
        )
        assertTrue(
            sensitivity.COMPLETION_DETAIL.contains("Speech Volume", ignoreCase = true)
        )
        assertTrue(
            sensitivity.COMPLETION_DETAIL.contains("Speech Speed", ignoreCase = true)
        )
        val instruction = GuidedWorkspaceTrainingSpec.lessonCardInstruction(
            NavigationAction.AdjustSensitivity
        ).orEmpty()
        assertTrue(instruction.contains("Sensitivity", ignoreCase = true))
        assertTrue(instruction.contains("Response Time", ignoreCase = true))
        assertFalse(instruction.contains("Use L1 R1 to save", ignoreCase = true))
        assertTrue(full.phases[0].description!!.contains("L0 R4"))
        assertEquals(
            NavigationAction.AdjustSensitivity,
            TrainingLessonCatalog.navigationLessons.last().action
        )
        assertEquals(8, TrainingLessonCatalog.navigationLessons.size)
    }

    @Test
    fun commitResultReportsSettingDirectionAndBoundary() {
        val opened = PreferenceAdjustmentController.openSensitivityAdjust(root(), MAX_SENSITIVITY_LEVEL)
        val result = PreferenceAdjustmentController.adjustAndPersist(
            opened,
            AdjustmentDirection.Increase
        )
        assertTrue(result is GuidedSequenceResult.Navigate)
        val mid = PreferenceAdjustmentController.openSensitivityAdjust(root(), 4)
        val saved = PreferenceAdjustmentController.adjustAndPersist(
            mid,
            AdjustmentDirection.Increase
        ) as GuidedSequenceResult.SavePreferencesAdjustment
        assertEquals(5, saved.sensitivityLevel)
        assertNotNull(policy.settingKindFor(GuidedPreferencesAdjustMode.Sensitivity))
        assertEquals(
            AdjustableSettingKind.SpeechVolume,
            policy.settingKindFor(GuidedPreferencesAdjustMode.SpeechVolume)
        )
    }

    @Test
    fun settingsHubSelectAndEmergencySequencesRemainIntact() {
        assertTrue(GuidedModeNavigation.isSelectSequence(1, 1))
        assertTrue(GuidedModeNavigation.isAdjustSettingsEntrySequence(5, 5))
        assertEquals(6, EMERGENCY_LEFT_WINKS)
        assertEquals(0, EMERGENCY_RIGHT_WINKS)
        assertTrue(GuidedModeNavigation.isOpenMainMenuSequence(4, 6))
        // Hub Select still opens Sensitivity.
        val hub = PreferenceAdjustmentController.openSettingsMenu(root())
        val opened = PreferenceAdjustmentController.openSelectedHubSetting(
            hub,
            GuidedCatalogContext(sensitivityLevel = 4)
        )
        assertEquals(GuidedPreferencesAdjustMode.Sensitivity, opened.preferencesAdjustMode)
    }
}
