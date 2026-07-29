package com.idworx.lisa

import com.idworx.lisa.features.adjustmentcommitpolicy.AdjustmentCommitPolicyAuthority
import com.idworx.lisa.features.guidedsensitivitylesson.GuidedSensitivityLessonAuthority
import com.idworx.lisa.features.onboardingguide.lessons.TrainingLessonCatalog
import com.idworx.lisa.features.onboardingguide.model.NavigationAction
import com.idworx.lisa.features.zerotouchprinciple.audit.ZeroTouchFileProbe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * RC8.31 — Simplify adjustment copy; rename Cancel / Back → Back on immediate-save screens.
 */
class Rc8_31SimplifyAdjustmentCopyAndBackLabelTest {

    private val english = LisaUiStrings.forLanguage(PreferredLanguage.English)

    private fun read(pathUnderMainJava: String): String {
        val path = "app/src/main/java/com/idworx/lisa/$pathUnderMainJava"
        return ZeroTouchFileProbe.readProjectFile(path)
            ?: error("Missing source: $path")
    }

    private fun sharedAdjustmentPanelSource(): String {
        val ui = read("LisaGuidedModeUi.kt")
        return ui.substringAfter("private fun SharedSettingAdjustmentPanel(")
            .substringBefore("@Composable\nprivate fun PreferencesAdjustmentPanel(")
            .ifBlank {
                ui.substringAfter("private fun SharedSettingAdjustmentPanel(")
                    .substringBefore("@Composable\nprivate fun SettingAdjustmentMeter(")
            }
    }

    @Test
    fun allFourScreensShowOnlyAutoSaveHelperWithoutExtraExplanations() {
        val panel = sharedAdjustmentPanelSource()
        assertTrue(panel.contains("guidedChangesSaveAutomatically"))
        assertEquals("Changes save automatically.", english.guidedChangesSaveAutomatically)
        assertEquals(
            AdjustmentCommitPolicyAuthority.HELPER_CHANGES_SAVE_AUTOMATICALLY,
            english.guidedChangesSaveAutomatically
        )
        assertFalse(panel.contains("guidedResponseTimeMeterHint"))
        assertFalse(panel.contains("Lower = faster"))
        assertFalse(panel.contains("Higher = more time"))
        // No second helper Text after the auto-save line before the meter when().
        val afterHelper = panel.substringAfter("guidedChangesSaveAutomatically")
            .substringBefore("when (adjustMode)")
        assertFalse(afterHelper.contains("Text("))
    }

    @Test
    fun adjustmentMainExitAndRightRailSayBackNotCancelBack() {
        assertEquals("Back", english.guidedBack)
        assertEquals("Back", english.guidedCancelAdjustment)
        val panel = sharedAdjustmentPanelSource()
        assertTrue(panel.contains("title = uiStrings.guidedBack"))
        assertFalse(panel.contains("guidedCancelBack"))
        assertFalse(panel.contains("Cancel / Back"))

        val rail = read("LisaGuidedMode.kt")
            .substringAfter("fun panelActions(uiStrings: LisaUiStrings, context: PanelContext)")
            .substringBefore("val scrollUp = GuidedNavPanelAction(")
        assertTrue(
            rail.contains("PanelContext.Adjustment -> uiStrings.guidedBack") ||
                rail.contains("PanelContext.Adjustment -> uiStrings.guidedCancelAdjustment")
        )
        assertFalse(rail.contains("PanelContext.Adjustment -> uiStrings.guidedCancelAdjustment") &&
            english.guidedCancelAdjustment.contains("Cancel"))
        assertEquals("Back", english.guidedCancelAdjustment)

        val adjustmentActions = GuidedNavigationPanelSpec.panelActions(
            english,
            GuidedNavigationPanelSpec.PanelContext.Adjustment
        )
        val back = adjustmentActions.first { it.kind == GuidedPanelActionKind.Back }
        assertEquals("Back", back.title)
        assertFalse(back.title.contains("Cancel", ignoreCase = true))
        assertEquals("L2 R2", back.sequenceLabel)
        assertEquals("2 Left + 2 Right", back.gestureHint)
    }

    @Test
    fun immediateSaveBehaviourAndSequencesUnchanged() {
        val opened = PreferenceAdjustmentController.openSensitivityAdjust(GuidedNavigationState(), 4)
        val saved = PreferenceAdjustmentController.increaseAndPersist(opened)
            as GuidedSequenceResult.SavePreferencesAdjustment
        assertEquals(5, saved.sensitivityLevel)
        assertEquals(GuidedPreferencesAdjustMode.Sensitivity, saved.newState.preferencesAdjustMode)

        val back = PreferenceAdjustmentController.cancelAdjustment(saved.newState)
        assertEquals(GuidedPreferencesAdjustMode.SettingsMenu, back.preferencesAdjustMode)

        assertEquals("L3 R1", AdjustmentCommitPolicyAuthority.decreaseSequenceLabel())
        assertEquals("L1 R3", AdjustmentCommitPolicyAuthority.increaseSequenceLabel())
        assertEquals("L2 R2", AdjustmentCommitPolicyAuthority.backSequenceLabel())
        assertEquals(6, EMERGENCY_LEFT_WINKS)
        assertEquals(0, EMERGENCY_RIGHT_WINKS)
        assertTrue(GuidedModeNavigation.isOpenMainMenuSequence(4, 6))
    }

    @Test
    fun lesson23AndCatalogueRemainIntact() {
        assertEquals(
            GuidedSensitivityLessonAuthority.ID_ADJUST_SENSITIVITY,
            TrainingLessonCatalog.navigationLessons.last().id
        )
        assertEquals(8, TrainingLessonCatalog.navigationLessons.size)
        assertEquals(
            NavigationAction.AdjustSensitivity,
            TrainingLessonCatalog.navigationLessons.last().action
        )
        assertTrue(
            GuidedSensitivityLessonAuthority.COMPLETION_DETAIL.contains(
                "save automatically",
                ignoreCase = true
            ) ||
                GuidedSensitivityLessonAuthority.PHASE4_INSTRUCTION.contains("L1 R3")
        )
        assertTrue(GuidedSensitivityLessonAuthority.LESSON_CONTEXT.contains("Sensitivity"))
        val panel = sharedAdjustmentPanelSource()
        assertTrue(panel.contains("guidedChangesSaveAutomatically"))
        assertTrue(panel.contains("guidedBack"))
    }
}
