package com.idworx.lisa

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * RC7D.27 — Simplified settings entry and header cleanup; RC8.30 — immediate save (no ConfirmSave*).
 */
class Rc7D_27SimplifiedSettingsFlowTest {

    private val english = LisaUiStrings.forLanguage(PreferredLanguage.English)

    private fun ctx(sensitivity: Int = 5, responseSec: Int = SequenceProcessingDelay.DEFAULT_SECONDS) =
        GuidedCatalogContext(responseTimeSec = responseSec, sensitivityLevel = sensitivity)

    private fun process(
        left: Int,
        right: Int,
        state: GuidedNavigationState,
        catalogContext: GuidedCatalogContext = ctx(),
        blinkOrder: List<Boolean> = emptyList()
    ): GuidedSequenceResult =
        GuidedNavigationController.processSequence(
            left = left,
            right = right,
            state = state,
            language = PreferredLanguage.English,
            uiStrings = english,
            catalogContext = catalogContext,
            blinkOrder = blinkOrder
        )

    private fun navigate(result: GuidedSequenceResult): GuidedNavigationState =
        (result as GuidedSequenceResult.Navigate).newState

    private fun menu(): GuidedNavigationState =
        PreferenceAdjustmentController.openSettingsMenu(
            GuidedNavigationState(screenMode = GuidedOverlayScreenMode.CategoryMenu)
        )

    private fun readSource(relativePath: String): String {
        val normalized = relativePath.replace('/', java.io.File.separatorChar)
        val roots = listOfNotNull(
            java.io.File(System.getProperty("user.dir") ?: "."),
            java.io.File(System.getProperty("user.dir") ?: ".").parentFile
        )
        for (root in roots) {
            val direct = root.resolve(normalized)
            if (direct.isFile) return direct.readText()
            if (normalized.startsWith("app${java.io.File.separatorChar}")) {
                val withoutApp = root.resolve(normalized.removePrefix("app${java.io.File.separatorChar}"))
                if (withoutApp.isFile) return withoutApp.readText()
            }
        }
        error("Missing source: $relativePath")
    }

    // ------------------------------------------------------------------ A. Header cleanup

    @Test
    fun sharedHeaderRemovesCombinedSummaryLine() {
        val ui = readSource("app/src/main/java/com/idworx/lisa/LisaAccessibilityUi.kt")
        val universal = readSource(
            "app/src/main/java/com/idworx/lisa/features/eyetrackingstatus/UniversalEyeTrackingHeader.kt"
        )
        assertFalse(universal.contains("listeningStatusLine"))
        assertTrue(universal.contains("uiStrings.sensitivityDecrease"))
        assertTrue(universal.contains("uiStrings.sensitivityIncrease"))
        assertTrue(universal.contains("uiStrings.responseTimeDecrease"))
        assertTrue(universal.contains("uiStrings.responseTimeIncrease"))
        assertTrue(universal.contains("\${uiStrings.sensitivity}: \$safeSensitivity"))
        assertTrue(universal.contains("\${uiStrings.responseTime}: \${safeResponse}s"))
        assertTrue(ui.contains("guidedResponseTimeControlsVisible = guidedWorkspaceTrainingActive"))
    }

    // ------------------------------------------------------------------ B. Hub selection model

    @Test
    fun selectOpensHighlightedSensitivityAdjustment() {
        val opened = navigate(
            process(GuidedModeNavigation.SELECT_LEFT, GuidedModeNavigation.SELECT_RIGHT, menu(), ctx(7))
        )
        assertEquals(GuidedPreferencesAdjustMode.Sensitivity, opened.preferencesAdjustMode)
        assertEquals(7, opened.draftSensitivityLevel)
        assertEquals(7, opened.adjustmentOriginalSensitivity)
    }

    @Test
    fun scrollDownThenSelectOpensResponseTimeAdjustment() {
        val highlighted = navigate(
            process(GuidedModeNavigation.NEXT_LEFT, GuidedModeNavigation.NEXT_RIGHT, menu(), ctx(responseSec = 6))
        )
        assertEquals(1, highlighted.settingsHubSelection)
        assertEquals(GuidedPreferencesAdjustMode.SettingsMenu, highlighted.preferencesAdjustMode)
        val opened = navigate(
            process(GuidedModeNavigation.SELECT_LEFT, GuidedModeNavigation.SELECT_RIGHT, highlighted, ctx(responseSec = 6))
        )
        assertEquals(GuidedPreferencesAdjustMode.ResponseTime, opened.preferencesAdjustMode)
        assertEquals(6, opened.draftResponseTimeSec)
        assertEquals(6, opened.adjustmentOriginalResponseTimeSec)
    }

    @Test
    fun settingsHubUsesSelectionAndSelectDoesNotFallThroughFromScroll() {
        val ui = readSource("app/src/main/java/com/idworx/lisa/LisaGuidedModeUi.kt")
        assertTrue(ui.contains("fun SettingsMenuPanel(") || ui.contains("SettingsAndControlsHubPanel"))
        assertTrue(ui.contains("guidedOpenSelectedSetting") || ui.contains("PanelContext.SettingsHub"))
        assertTrue(ui.contains("settingsHubSelection"))
        // RC8.32 — labelled Sensitivity sequence L2 R0 opens Sensitivity when selected.
        val openedViaLabel = navigate(
            process(GuidedModeNavigation.PREVIOUS_LEFT, GuidedModeNavigation.PREVIOUS_RIGHT, menu())
        )
        assertEquals(GuidedPreferencesAdjustMode.Sensitivity, openedViaLabel.preferencesAdjustMode)
        // Select remains an alternate open for the highlighted Sensitivity card.
        val opened = navigate(
            process(GuidedModeNavigation.SELECT_LEFT, GuidedModeNavigation.SELECT_RIGHT, menu())
        )
        assertEquals(GuidedPreferencesAdjustMode.Sensitivity, opened.preferencesAdjustMode)
    }

    // ------------------------------------------------------------------ C. Cancel label and behaviour

    @Test
    fun adjustmentExitLabelIsBackWithoutCancel() {
        assertEquals("Back", english.guidedBack)
        assertEquals("Back", english.guidedCancelAdjustment)
        assertFalse(english.guidedCancelAdjustment.contains("Cancel", ignoreCase = true))
        assertFalse(english.guidedCancelAdjustment.contains("Preferences", ignoreCase = true))
        val ui = readSource("app/src/main/java/com/idworx/lisa/LisaGuidedModeUi.kt")
        val panel = ui.substringAfter("fun SharedSettingAdjustmentPanel(")
            .substringBefore("\n/** @deprecated Replaced by [SettingsAndControlsHubPanel]")
        assertTrue(panel.contains("guidedBack"))
        assertFalse(panel.contains("guidedCancelBack"))
        assertFalse(panel.contains("Cancel / Back"))
    }

    @Test
    fun adjustmentCancelRestoresOriginalAndReturnsToSettingsMenu() {
        val editing = PreferenceAdjustmentController.openSensitivityAdjust(menu(), 5)
        val bumped = PreferenceAdjustmentController.increaseDraft(editing)
        assertEquals(6, bumped.draftSensitivityLevel)
        val cancelled = PreferenceAdjustmentController.cancelAdjustment(bumped)
        assertEquals(GuidedPreferencesAdjustMode.SettingsMenu, cancelled.preferencesAdjustMode)
        // Draft is discarded for display purposes when reopening — original was 5.
        val reopened = PreferenceAdjustmentController.openSensitivityAdjust(cancelled, 5)
        assertEquals(5, reopened.draftSensitivityLevel)
    }

    // ------------------------------------------------------------------ D. RC8.30 immediate save

    @Test
    fun increasePersistsImmediatelyAndStaysOnAdjustmentScreen() {
        val editing = PreferenceAdjustmentController.openSensitivityAdjust(menu(), 5)
        val saved = PreferenceAdjustmentController.increaseAndPersist(editing)
            as GuidedSequenceResult.SavePreferencesAdjustment
        assertEquals(6, saved.sensitivityLevel)
        assertEquals(GuidedPreferencesAdjustMode.Sensitivity, saved.newState.preferencesAdjustMode)
        assertEquals(6, saved.newState.draftSensitivityLevel)
        assertTrue(saved.newState.isValueAdjustmentActive)
        assertFalse(saved.newState.isSaveConfirmationActive)
    }

    @Test
    fun increaseViaGesturePersistsAndBackReturnsToSettingsMenu() {
        val editing = PreferenceAdjustmentController.openSensitivityAdjust(menu(), 4)
        val saved = PreferenceAdjustmentController.increaseAndPersist(editing)
            as GuidedSequenceResult.SavePreferencesAdjustment
        assertEquals(5, saved.sensitivityLevel)
        val back = navigate(
            process(
                GuidedModeNavigation.BACK_LEFT,
                GuidedModeNavigation.BACK_RIGHT,
                saved.newState
            )
        )
        assertEquals(GuidedPreferencesAdjustMode.SettingsMenu, back.preferencesAdjustMode)
    }

    @Test
    fun beginSaveConfirmationIsNoOpUnderImmediateSave() {
        val editing = PreferenceAdjustmentController.increaseDraft(
            PreferenceAdjustmentController.openResponseTimeAdjust(menu(), 5)
        )
        val unchanged = PreferenceAdjustmentController.beginSaveConfirmation(editing)
        assertEquals(GuidedPreferencesAdjustMode.ResponseTime, unchanged.preferencesAdjustMode)
        assertEquals(6, unchanged.draftResponseTimeSec)
        assertFalse(unchanged.isSaveConfirmationActive)
    }

    @Test
    fun selectIsUnmatchedOnAdjustmentScreens() {
        val editing = PreferenceAdjustmentController.openSensitivityAdjust(menu(), 5)
        assertTrue(
            process(GuidedModeNavigation.SELECT_LEFT, GuidedModeNavigation.SELECT_RIGHT, editing)
                is GuidedSequenceResult.Unmatched
        )
        assertTrue(
            process(GuidedModeNavigation.DECREASE_VALUE_LEFT, GuidedModeNavigation.DECREASE_VALUE_RIGHT, editing)
                is GuidedSequenceResult.SavePreferencesAdjustment
        )
        assertTrue(
            process(GuidedModeNavigation.BACK_LEFT, GuidedModeNavigation.BACK_RIGHT, editing)
                is GuidedSequenceResult.Navigate
        )
    }

    @Test
    fun adjustmentPanelShowsAutoSaveHintWithoutSaveRow() {
        assertTrue(english.guidedChangesSaveAutomatically.isNotBlank())
        val ui = readSource("app/src/main/java/com/idworx/lisa/LisaGuidedModeUi.kt")
        val panel = ui.substringAfter("private fun SharedSettingAdjustmentPanel(")
            .substringBefore("\n/** @deprecated Replaced by [SettingsAndControlsHubPanel]")
        assertTrue(panel.contains("guidedChangesSaveAutomatically"))
        assertFalse(panel.contains("onSave"))
        // Legacy confirmation strings remain for enum stability; panel no longer routes to ConfirmSave*.
        assertEquals("Save Sensitivity?", english.guidedSaveSensitivityConfirmTitle())
        assertTrue(ui.contains("fun SaveConfirmationPanel("))
    }

    // ------------------------------------------------------------------ E. Categories-card removal

    @Test
    fun adjustmentContentOmitsCategoriesCardButPanelRetainsIt() {
        val ui = readSource("app/src/main/java/com/idworx/lisa/LisaGuidedModeUi.kt")
        val panel = ui.substringAfter("fun SharedSettingAdjustmentPanel(")
            .substringBefore("\n/** @deprecated Replaced by [SettingsAndControlsHubPanel]")
        assertFalse(panel.contains("guidedCategoriesNavTitle"))
        assertFalse(panel.contains("CATEGORIES_LEFT"))
        assertTrue(ui.contains("GuidedPanelActionKind.Categories"))
        assertEquals(3, GuidedModeNavigation.CATEGORIES_LEFT)
        assertEquals(0, GuidedModeNavigation.CATEGORIES_RIGHT)
    }

    // ------------------------------------------------------------------ F. Meter / routing regression

    @Test
    fun meterAndSequencesRemain() {
        assertEquals(8, SettingAdjustmentMeterAuthority.SEGMENT_COUNT)
        assertEquals(3 to 1, GuidedModeNavigation.DECREASE_VALUE_LEFT to GuidedModeNavigation.DECREASE_VALUE_RIGHT)
        assertEquals(1 to 3, GuidedModeNavigation.INCREASE_VALUE_LEFT to GuidedModeNavigation.INCREASE_VALUE_RIGHT)
        assertEquals(5 to 5, GuidedModeNavigation.ADJUST_SETTINGS_ENTRY_LEFT to GuidedModeNavigation.ADJUST_SETTINGS_ENTRY_RIGHT)
        assertEquals(6 to 0, EMERGENCY_LEFT_WINKS to EMERGENCY_RIGHT_WINKS)
        assertEquals(GuidedVocabularyCategory.AdjustSettings, GuidedVocabularyCategory.ordered[6])
    }

    @Test
    fun noAndroidSystemKeyboardIntroduced() {
        val ui = readSource("app/src/main/java/com/idworx/lisa/LisaGuidedModeUi.kt")
        assertFalse(ui.contains("KeyboardOptions"))
    }
}
