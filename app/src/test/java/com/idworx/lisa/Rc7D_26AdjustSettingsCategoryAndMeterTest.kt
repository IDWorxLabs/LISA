package com.idworx.lisa

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * RC7D.26 — Adjust Settings as Category Menu destination 9 + visual adjustment meter.
 */
class Rc7D_26AdjustSettingsCategoryAndMeterTest {

    private val english = LisaUiStrings.forLanguage(PreferredLanguage.English)
    private val entryLeft = GuidedModeNavigation.ADJUST_SETTINGS_ENTRY_LEFT
    private val entryRight = GuidedModeNavigation.ADJUST_SETTINGS_ENTRY_RIGHT

    private fun ctx(sensitivity: Int = 5, responseSec: Int = SequenceProcessingDelay.DEFAULT_SECONDS) =
        GuidedCatalogContext(responseTimeSec = responseSec, sensitivityLevel = sensitivity)

    private fun process(
        left: Int,
        right: Int,
        state: GuidedNavigationState,
        catalogContext: GuidedCatalogContext = ctx()
    ): GuidedSequenceResult =
        GuidedNavigationController.processSequence(
            left = left,
            right = right,
            state = state,
            language = PreferredLanguage.English,
            uiStrings = english,
            catalogContext = catalogContext
        )

    private fun navigate(result: GuidedSequenceResult): GuidedNavigationState =
        (result as GuidedSequenceResult.Navigate).newState

    private fun categoryMenu(): GuidedNavigationState =
        GuidedNavigationState(screenMode = GuidedOverlayScreenMode.CategoryMenu)

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
    fun sharedHeaderNoLongerDisplaysAdjustLabel() {
        val ui = readSource("app/src/main/java/com/idworx/lisa/LisaAccessibilityUi.kt")
        val universal = readSource(
            "app/src/main/java/com/idworx/lisa/features/eyetrackingstatus/UniversalEyeTrackingHeader.kt"
        )
        assertFalse(ui.contains("guidedAdjustSettingsDiscoverabilityLabel"))
        assertFalse(ui.contains("Adjust: L5 R5"))
        assertFalse(ui.contains("onOpenAdjustSettings"))
        assertTrue(ui.contains("UniversalEyeTrackingHeader("))
        assertTrue(universal.contains("uiStrings.sensitivityDecrease"))
        assertTrue(universal.contains("uiStrings.sensitivityIncrease"))
        assertTrue(universal.contains("uiStrings.responseTimeDecrease"))
        assertTrue(universal.contains("uiStrings.responseTimeIncrease"))
        assertTrue(universal.contains("\${uiStrings.sensitivity}: \$safeSensitivity"))
        assertTrue(universal.contains("\${uiStrings.responseTime}: \${safeResponse}s"))
    }

    // ------------------------------------------------------------------ B. Category 9

    @Test
    fun adjustSettingsIsDestinationNine() {
        assertEquals(6, GuidedVocabularyCategory.ADJUST_SETTINGS_INDEX)
        assertEquals(7, GuidedVocabularyCategory.PAGE_COUNT)
        assertEquals(GuidedVocabularyCategory.AdjustSettings, GuidedVocabularyCategory.ordered[6])
        assertEquals(GuidedVocabularyCategory.PhraseManagement, GuidedVocabularyCategory.ordered[5])
        assertEquals(5, GuidedVocabularyCategory.PHRASE_MANAGEMENT_INDEX)
        assertEquals("Settings & Controls", english.guidedCategoryTitle(GuidedVocabularyCategory.AdjustSettings))
    }

    @Test
    fun adjustSettingsSequenceIsCanonicalL5R5() {
        assertEquals(5, entryLeft)
        assertEquals(5, entryRight)
        assertEquals(
            formatWinkSequenceShort(entryLeft, entryRight),
            GuidedCategoryShortcuts.sequenceLabelForCategory(GuidedVocabularyCategory.ADJUST_SETTINGS_INDEX)
        )
        assertEquals(
            GuidedVocabularyCategory.ADJUST_SETTINGS_INDEX,
            GuidedCategoryShortcuts.categoryIndexForGesture(entryLeft, entryRight)
        )
    }

    @Test
    fun blinkL5R5OpensCanonicalSettingsSubMode() {
        val opened = navigate(process(entryLeft, entryRight, categoryMenu()))
        assertEquals(GuidedPreferencesAdjustMode.SettingsMenu, opened.preferencesAdjustMode)
        assertTrue(opened.isSettingsMenuActive)
        assertEquals(GuidedOverlayScreenMode.CategoryMenu, opened.screenMode)
    }

    @Test
    fun selectOnAdjustSettingsDestinationOpensSameSubModeViaRouting() {
        assertEquals(
            CategoryAreaDestination.AdjustSettings,
            CategoryAreaDestination.forCategoryIndex(GuidedVocabularyCategory.ADJUST_SETTINGS_INDEX)
        )
        val main = readSource("app/src/main/java/com/idworx/lisa/MainActivity.kt")
        assertTrue(main.contains("openAdjustSettingsFromCategories"))
        assertTrue(main.contains("CategoryAreaDestination.AdjustSettings"))
        assertTrue(main.contains("PreferenceAdjustmentController.openSettingsMenu"))
    }

    @Test
    fun adjustSettingsIsNotAssignablePhraseCategory() {
        assertNull(GuidedVocabularyCategory.AdjustSettings.toCaregiverCategory())
        assertFalse(
            CategoryAreaDestination.isAssignableCommunicationCategory(GuidedVocabularyCategory.AdjustSettings)
        )
        assertTrue(CategoryAreaDestination.isManagementDestination(GuidedVocabularyCategory.AdjustSettings))
        val pages = GuidedVocabularyCatalog.buildPages(PreferredLanguage.English, english)
        val page = pages[GuidedVocabularyCategory.ADJUST_SETTINGS_INDEX]
        assertEquals(GuidedVocabularyCategory.AdjustSettings, page.category)
        assertTrue(page.entries.isEmpty())
    }

    @Test
    fun moveDownCanReachDestinationNine() {
        var state = categoryMenu()
        repeat(GuidedVocabularyCategory.PAGE_COUNT - 1) {
            state = navigate(
                process(GuidedModeNavigation.NEXT_LEFT, GuidedModeNavigation.NEXT_RIGHT, state)
            )
        }
        assertEquals(GuidedVocabularyCategory.ADJUST_SETTINGS_INDEX, state.categoryMenuSelection)
    }

    @Test
    fun viewportPagingRemainsMeasurementDriven() {
        assertEquals(1, CategoryViewportPaging.pageCount(viewportHeightPx = 1000, maxScrollPx = 0))
        assertEquals(2, CategoryViewportPaging.pageCount(viewportHeightPx = 1000, maxScrollPx = 200))
        val ui = readSource("app/src/main/java/com/idworx/lisa/LisaGuidedModeUi.kt")
        assertTrue(ui.contains("CategoryViewportPaging"))
        assertFalse(ui.contains("hardcodedPageMembershipForAdjustSettings"))
    }

    // ------------------------------------------------------------------ C. Settings sub-mode

    @Test
    fun settingsSubModeOpensSettingsDirectly() {
        val menu = navigate(process(entryLeft, entryRight, categoryMenu()))
        assertEquals(GuidedPreferencesAdjustMode.SettingsMenu, menu.preferencesAdjustMode)
        // Selection model: Scroll Down highlights Response Time; Select opens it.
        val highlighted = navigate(
            process(GuidedModeNavigation.NEXT_LEFT, GuidedModeNavigation.NEXT_RIGHT, menu)
        )
        assertEquals(1, highlighted.settingsHubSelection)
        assertEquals(GuidedPreferencesAdjustMode.SettingsMenu, highlighted.preferencesAdjustMode)
        val response = navigate(
            process(GuidedModeNavigation.SELECT_LEFT, GuidedModeNavigation.SELECT_RIGHT, highlighted)
        )
        assertEquals(GuidedPreferencesAdjustMode.ResponseTime, response.preferencesAdjustMode)
        val backToMenu = navigate(
            process(GuidedModeNavigation.BACK_LEFT, GuidedModeNavigation.BACK_RIGHT, response)
        )
        assertEquals(GuidedPreferencesAdjustMode.SettingsMenu, backToMenu.preferencesAdjustMode)
        // Default selection is Sensitivity — Select opens it without needing Scroll Up.
        val sensitivity = navigate(
            process(GuidedModeNavigation.SELECT_LEFT, GuidedModeNavigation.SELECT_RIGHT, backToMenu)
        )
        assertEquals(GuidedPreferencesAdjustMode.Sensitivity, sensitivity.preferencesAdjustMode)
        val cancelled = navigate(
            process(GuidedModeNavigation.BACK_LEFT, GuidedModeNavigation.BACK_RIGHT, sensitivity)
        )
        assertEquals(GuidedPreferencesAdjustMode.SettingsMenu, cancelled.preferencesAdjustMode)
    }

    @Test
    fun settingsHeaderUsesSettingsTerminologyNotPhrases() {
        // RC7D.27 — selection indicator removed; title remains Adjust Settings.
        assertEquals("Settings & Controls", english.guidedAdjustSettingsTitle)
        val ui = readSource("app/src/main/java/com/idworx/lisa/LisaGuidedModeUi.kt")
        assertFalse(ui.contains("guidedSettingIndicator"))
        assertTrue(ui.contains("guidedAdjustSettingsTitle"))
        assertFalse(
            "Adjust Settings must not keep showing phrase-page indicator while settings menu is active",
            ui.contains("preferencesAdjustMode == GuidedPreferencesAdjustMode.SettingsMenu -> Text(\n                    text = uiStrings.guidedPhrasePageIndicator")
        )
    }

    // ------------------------------------------------------------------ D. Meter calculations

    @Test
    fun meterMinimumMapsToLowestValidLevel() {
        assertEquals(
            1,
            SettingAdjustmentMeterAuthority.activeSegmentCount(1, minimum = 1, maximum = 10, segmentCount = 8)
        )
        assertEquals(
            1,
            SettingAdjustmentMeterAuthority.activeSegmentCount(
                SequenceProcessingDelay.MIN_SECONDS,
                minimum = SequenceProcessingDelay.MIN_SECONDS,
                maximum = SequenceProcessingDelay.MAX_SECONDS
            )
        )
    }

    @Test
    fun meterMaximumActivatesAllSegments() {
        assertEquals(
            8,
            SettingAdjustmentMeterAuthority.activeSegmentCount(10, minimum = 1, maximum = 10, segmentCount = 8)
        )
        assertEquals(
            SettingAdjustmentMeterAuthority.SEGMENT_COUNT,
            SettingAdjustmentMeterAuthority.activeSegmentCount(
                SequenceProcessingDelay.MAX_SECONDS,
                minimum = SequenceProcessingDelay.MIN_SECONDS,
                maximum = SequenceProcessingDelay.MAX_SECONDS
            )
        )
    }

    @Test
    fun meterIntermediateValuesAreDeterministicAndClamped() {
        val mid = SettingAdjustmentMeterAuthority.activeSegmentCount(5, 1, 10, 8)
        assertEquals(mid, SettingAdjustmentMeterAuthority.activeSegmentCount(5, 1, 10, 8))
        assertTrue(mid in 1..8)
        assertEquals(1, SettingAdjustmentMeterAuthority.activeSegmentCount(0, 1, 10, 8))
        assertEquals(8, SettingAdjustmentMeterAuthority.activeSegmentCount(99, 1, 10, 8))
    }

    @Test
    fun meterInvalidRangesAreSafe() {
        assertEquals(0, SettingAdjustmentMeterAuthority.activeSegmentCount(5, 1, 10, segmentCount = 0))
        assertEquals(1, SettingAdjustmentMeterAuthority.activeSegmentCount(5, 5, 5, segmentCount = 8))
        assertEquals(1, SettingAdjustmentMeterAuthority.activeSegmentCount(5, 10, 1, segmentCount = 8))
    }

    @Test
    fun meterUsesExistingSensitivityAndResponseTimeLimits() {
        assertEquals(MIN_SENSITIVITY_LEVEL, 1)
        assertEquals(MAX_SENSITIVITY_LEVEL, 10)
        assertEquals(SequenceProcessingDelay.MIN_SECONDS, 3)
        assertEquals(SequenceProcessingDelay.MAX_SECONDS, 8)
        val sensMin = SettingAdjustmentMeterAuthority.activeSegmentCount(MIN_SENSITIVITY_LEVEL, MIN_SENSITIVITY_LEVEL, MAX_SENSITIVITY_LEVEL)
        val sensMax = SettingAdjustmentMeterAuthority.activeSegmentCount(MAX_SENSITIVITY_LEVEL, MIN_SENSITIVITY_LEVEL, MAX_SENSITIVITY_LEVEL)
        assertEquals(1, sensMin)
        assertEquals(SettingAdjustmentMeterAuthority.SEGMENT_COUNT, sensMax)
    }

    // ------------------------------------------------------------------ E. Sensitivity meter behaviour

    @Test
    fun sensitivityAdjustmentUsesMeterAndSharedAuthority() {
        val opened = PreferenceAdjustmentController.openSensitivityAdjust(categoryMenu(), currentLevel = 5)
        assertEquals(5, opened.draftSensitivityLevel)
        val decreased = PreferenceAdjustmentController.decreaseDraft(opened)
        assertEquals(4, decreased.draftSensitivityLevel)
        assertTrue(
            SettingAdjustmentMeterAuthority.activeSegmentCount(4, MIN_SENSITIVITY_LEVEL, MAX_SENSITIVITY_LEVEL) <
                SettingAdjustmentMeterAuthority.activeSegmentCount(5, MIN_SENSITIVITY_LEVEL, MAX_SENSITIVITY_LEVEL)
        )
        val increased = PreferenceAdjustmentController.increaseDraft(opened)
        assertEquals(6, increased.draftSensitivityLevel)
        val confirming = PreferenceAdjustmentController.beginSaveConfirmation(increased)
        val saved = PreferenceAdjustmentController.saveAdjustment(confirming)
        assertEquals(6, saved.sensitivityLevel)
        assertEquals(GuidedPreferencesAdjustMode.SettingsMenu, saved.newState.preferencesAdjustMode)
        val cancelled = PreferenceAdjustmentController.cancelAdjustment(increased)
        assertEquals(GuidedPreferencesAdjustMode.SettingsMenu, cancelled.preferencesAdjustMode)
    }

    @Test
    fun sensitivityMeterUiShowsRequiredLabels() {
        assertEquals("Sensitivity Adjustment", english.guidedSensitivityAdjustmentTitle)
        assertEquals("Decrease", english.guidedDecreaseShort)
        assertEquals("Increase", english.guidedIncreaseShort)
        val ui = readSource("app/src/main/java/com/idworx/lisa/LisaGuidedModeUi.kt")
        assertTrue(ui.contains("fun SettingAdjustmentMeter("))
        assertTrue(ui.contains("DECREASE_VALUE_LEFT"))
        assertTrue(ui.contains("INCREASE_VALUE_LEFT"))
        assertTrue(ui.contains("SettingAdjustmentMeterAuthority.activeSegmentCount"))
    }

    // ------------------------------------------------------------------ F. Response Time meter behaviour

    @Test
    fun responseTimeAdjustmentUsesMeterAndSharedAuthority() {
        val opened = PreferenceAdjustmentController.openResponseTimeAdjust(
            categoryMenu(),
            currentSec = SequenceProcessingDelay.DEFAULT_SECONDS
        )
        assertEquals(5, opened.draftResponseTimeSec)
        val decreased = PreferenceAdjustmentController.decreaseDraft(opened)
        assertEquals(4, decreased.draftResponseTimeSec)
        val increased = PreferenceAdjustmentController.increaseDraft(opened)
        assertEquals(6, increased.draftResponseTimeSec)
        val confirming = PreferenceAdjustmentController.beginSaveConfirmation(increased)
        val saved = PreferenceAdjustmentController.saveAdjustment(confirming)
        assertEquals(6, saved.responseTimeSec)
        assertEquals(GuidedPreferencesAdjustMode.SettingsMenu, saved.newState.preferencesAdjustMode)
        val cancelled = PreferenceAdjustmentController.cancelAdjustment(decreased)
        assertEquals(GuidedPreferencesAdjustMode.SettingsMenu, cancelled.preferencesAdjustMode)
    }

    @Test
    fun responseTimeMeterUiShowsSecondsAndHint() {
        assertEquals("Response Time Adjustment", english.guidedResponseTimeAdjustmentTitle)
        assertTrue(english.guidedCurrentResponseTime(5).contains("5"))
        assertTrue(english.guidedResponseTimeMeterHint.isNotBlank())
        val ui = readSource("app/src/main/java/com/idworx/lisa/LisaGuidedModeUi.kt")
        assertTrue(ui.contains("guidedResponseTimeMeterHint"))
        assertTrue(ui.contains("formatResponseTimeTick"))
    }

    // ------------------------------------------------------------------ G. Safety and regression

    @Test
    fun adjustmentModeBlocksCategoryShortcuts() {
        val adjusting = PreferenceAdjustmentController.openSensitivityAdjust(categoryMenu(), 5)
        // L3 R1 is Medical shortcut outside adjustment; inside it must decrease only.
        val result = process(
            GuidedModeNavigation.DECREASE_VALUE_LEFT,
            GuidedModeNavigation.DECREASE_VALUE_RIGHT,
            adjusting
        )
        val state = navigate(result)
        assertEquals(GuidedPreferencesAdjustMode.Sensitivity, state.preferencesAdjustMode)
        assertEquals(4, state.draftSensitivityLevel)
        assertEquals(GuidedOverlayScreenMode.CategoryMenu, state.screenMode)
    }

    @Test
    fun emergencyConstantsUnchanged() {
        assertEquals(6, EMERGENCY_LEFT_WINKS)
        assertEquals(0, EMERGENCY_RIGHT_WINKS)
    }

    @Test
    fun phraseManagementRemainsDestinationEight() {
        assertEquals(5, GuidedVocabularyCategory.PHRASE_MANAGEMENT_INDEX)
        assertEquals(
            "Phrase Management",
            english.guidedCategoryTitle(GuidedVocabularyCategory.PhraseManagement)
        )
        assertNotEquals(
            GuidedVocabularyCategory.AdjustSettings,
            GuidedVocabularyCategory.PhraseManagement
        )
    }

    @Test
    fun noAndroidSystemKeyboardIntroduced() {
        val ui = readSource("app/src/main/java/com/idworx/lisa/LisaGuidedModeUi.kt")
        assertFalse(ui.contains("AndroidView"))
        assertFalse(ui.contains("TextField(") && ui.contains("SettingAdjustmentMeter"))
    }

    @Test
    fun existingCanonicalSequencesUnchanged() {
        assertEquals(2 to 0, GuidedModeNavigation.PREVIOUS_LEFT to GuidedModeNavigation.PREVIOUS_RIGHT)
        assertEquals(0 to 2, GuidedModeNavigation.NEXT_LEFT to GuidedModeNavigation.NEXT_RIGHT)
        assertEquals(1 to 1, GuidedModeNavigation.SELECT_LEFT to GuidedModeNavigation.SELECT_RIGHT)
        assertEquals(2 to 2, GuidedModeNavigation.BACK_LEFT to GuidedModeNavigation.BACK_RIGHT)
        assertEquals(3 to 1, GuidedModeNavigation.DECREASE_VALUE_LEFT to GuidedModeNavigation.DECREASE_VALUE_RIGHT)
        assertEquals(1 to 3, GuidedModeNavigation.INCREASE_VALUE_LEFT to GuidedModeNavigation.INCREASE_VALUE_RIGHT)
        assertEquals(5 to 5, entryLeft to entryRight)
    }
}
