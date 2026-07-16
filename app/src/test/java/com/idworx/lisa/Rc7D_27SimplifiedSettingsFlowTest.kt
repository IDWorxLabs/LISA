package com.idworx.lisa

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * RC7D.27 — Simplified settings entry, save confirmation, and header cleanup.
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
        val compact = ui.substringAfter("fun CompactSensitivityControls(")
            .substringBefore("\n@Composable\nprivate fun SequenceProgressDots")
        assertFalse(compact.contains("listeningStatusLine"))
        assertTrue(compact.contains("sensitivityDecrease"))
        assertTrue(compact.contains("sensitivityIncrease"))
        assertTrue(compact.contains("responseTimeDecrease"))
        assertTrue(compact.contains("responseTimeIncrease"))
        assertTrue(compact.contains("\${uiStrings.sensitivity}: \$sensitivityLevel"))
        assertTrue(compact.contains("\${uiStrings.responseTime}: \${responseTimeSec}s"))
    }

    // ------------------------------------------------------------------ B. Direct setting entry

    @Test
    fun l2r0DirectlyOpensSensitivityAdjustment() {
        val opened = navigate(
            process(GuidedModeNavigation.PREVIOUS_LEFT, GuidedModeNavigation.PREVIOUS_RIGHT, menu(), ctx(7))
        )
        assertEquals(GuidedPreferencesAdjustMode.Sensitivity, opened.preferencesAdjustMode)
        assertEquals(7, opened.draftSensitivityLevel)
        assertEquals(7, opened.adjustmentOriginalSensitivity)
    }

    @Test
    fun l0r2DirectlyOpensResponseTimeAdjustment() {
        val opened = navigate(
            process(GuidedModeNavigation.NEXT_LEFT, GuidedModeNavigation.NEXT_RIGHT, menu(), ctx(responseSec = 6))
        )
        assertEquals(GuidedPreferencesAdjustMode.ResponseTime, opened.preferencesAdjustMode)
        assertEquals(6, opened.draftResponseTimeSec)
        assertEquals(6, opened.adjustmentOriginalResponseTimeSec)
    }

    @Test
    fun settingsMenuHasNoOpenSelectedOrSelectionState() {
        val ui = readSource("app/src/main/java/com/idworx/lisa/LisaGuidedModeUi.kt")
        assertTrue(ui.contains("fun SettingsMenuPanel("))
        assertFalse(ui.contains("guidedOpenSelectedSetting"))
        assertFalse(ui.contains("guidedSettingIndicator"))
        assertFalse(ui.contains("settingsMenuSelection"))
        val unmatched = process(
            GuidedModeNavigation.SELECT_LEFT,
            GuidedModeNavigation.SELECT_RIGHT,
            menu()
        )
        assertTrue(unmatched is GuidedSequenceResult.Unmatched)
    }

    // ------------------------------------------------------------------ C. Cancel label and behaviour

    @Test
    fun cancelLabelIsCancelBackWithoutPreferences() {
        assertEquals("Cancel / Back", english.guidedCancelBack)
        assertEquals("Cancel / Back", english.guidedCancelToPreferences)
        assertFalse(english.guidedCancelBack.contains("Preferences", ignoreCase = true))
        val ui = readSource("app/src/main/java/com/idworx/lisa/LisaGuidedModeUi.kt")
        val panel = ui.substringAfter("fun PreferencesAdjustmentPanel(")
            .substringBefore("\n/**\n * RC7D.26")
        assertTrue(panel.contains("guidedCancelBack"))
        assertFalse(panel.contains("Back to Preferences"))
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

    // ------------------------------------------------------------------ D. Save confirmation

    @Test
    fun firstSaveEntersConfirmationWithoutPersisting() {
        val editing = PreferenceAdjustmentController.increaseDraft(
            PreferenceAdjustmentController.openSensitivityAdjust(menu(), 5)
        )
        val confirming = navigate(
            process(
                GuidedModeNavigation.SELECT_LEFT,
                GuidedModeNavigation.SELECT_RIGHT,
                editing,
                blinkOrder = listOf(true, false)
            )
        )
        assertEquals(GuidedPreferencesAdjustMode.ConfirmSaveSensitivity, confirming.preferencesAdjustMode)
        assertEquals(6, confirming.draftSensitivityLevel)
        assertFalse(confirming.isValueAdjustmentActive)
        assertTrue(confirming.isSaveConfirmationActive)
    }

    @Test
    fun confirmPersistsAndReturnsToSettingsMenu() {
        val confirming = PreferenceAdjustmentController.beginSaveConfirmation(
            PreferenceAdjustmentController.increaseDraft(
                PreferenceAdjustmentController.openSensitivityAdjust(menu(), 4)
            )
        )
        val saved = PreferenceAdjustmentController.saveAdjustment(confirming)
        assertEquals(5, saved.sensitivityLevel)
        assertEquals(GuidedPreferencesAdjustMode.SettingsMenu, saved.newState.preferencesAdjustMode)
    }

    @Test
    fun confirmationCancelReturnsToEditingWithDraftIntact() {
        val confirming = PreferenceAdjustmentController.beginSaveConfirmation(
            PreferenceAdjustmentController.increaseDraft(
                PreferenceAdjustmentController.openResponseTimeAdjust(menu(), 5)
            )
        )
        val back = navigate(
            process(1, 1, confirming, blinkOrder = listOf(false, true))
        )
        assertEquals(GuidedPreferencesAdjustMode.ResponseTime, back.preferencesAdjustMode)
        assertEquals(6, back.draftResponseTimeSec)
    }

    @Test
    fun confirmationBlocksUnderlyingCommands() {
        val confirming = PreferenceAdjustmentController.beginSaveConfirmation(
            PreferenceAdjustmentController.openSensitivityAdjust(menu(), 5)
        )
        assertTrue(
            process(GuidedModeNavigation.DECREASE_VALUE_LEFT, GuidedModeNavigation.DECREASE_VALUE_RIGHT, confirming)
                is GuidedSequenceResult.Unmatched
        )
        assertTrue(
            process(GuidedModeNavigation.BACK_LEFT, GuidedModeNavigation.BACK_RIGHT, confirming)
                is GuidedSequenceResult.Unmatched
        )
        assertTrue(
            process(GuidedModeNavigation.CATEGORIES_LEFT, GuidedModeNavigation.CATEGORIES_RIGHT, confirming)
                is GuidedSequenceResult.Unmatched
        )
    }

    @Test
    fun confirmationShowsOriginalAndNewValuesInUi() {
        assertEquals("Save Sensitivity?", english.guidedSaveSensitivityConfirmTitle())
        assertEquals("Current saved value: 5", english.guidedSaveConfirmOriginalSensitivity(5))
        assertEquals("New value: 7", english.guidedSaveConfirmNewSensitivity(7))
        assertEquals("Save Response Time?", english.guidedSaveResponseTimeConfirmTitle())
        assertTrue(english.guidedSaveConfirmOriginalResponseTime(5).contains("5s"))
        assertTrue(english.guidedSaveConfirmNewResponseTime(6).contains("6s"))
        assertEquals("R1 L1", english.guidedConfirmCancelSequenceLabel)
        val ui = readSource("app/src/main/java/com/idworx/lisa/LisaGuidedModeUi.kt")
        assertTrue(ui.contains("fun SaveConfirmationPanel("))
    }

    // ------------------------------------------------------------------ E. Categories-card removal

    @Test
    fun adjustmentContentOmitsCategoriesCardButPanelRetainsIt() {
        val ui = readSource("app/src/main/java/com/idworx/lisa/LisaGuidedModeUi.kt")
        val panel = ui.substringAfter("fun PreferencesAdjustmentPanel(")
            .substringBefore("\n/**\n * RC7D.26")
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
        assertEquals(GuidedVocabularyCategory.AdjustSettings, GuidedVocabularyCategory.ordered[8])
    }

    @Test
    fun noAndroidSystemKeyboardIntroduced() {
        val ui = readSource("app/src/main/java/com/idworx/lisa/LisaGuidedModeUi.kt")
        assertFalse(ui.contains("KeyboardOptions"))
    }
}
