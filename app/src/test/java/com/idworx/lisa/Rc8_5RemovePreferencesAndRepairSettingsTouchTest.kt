package com.idworx.lisa

import com.idworx.lisa.features.zerotouchprinciple.audit.ZeroTouchFileProbe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * RC8.5 — Preferences removed from Communication; Settings & Controls touch opens via openHubSetting.
 */
class Rc8_5RemovePreferencesAndRepairSettingsTouchTest {

    private val english = LisaUiStrings.forLanguage(PreferredLanguage.English)
    private val catalogContext = GuidedCatalogContext(
        responseTimeSec = SequenceProcessingDelay.DEFAULT_SECONDS,
        sensitivityLevel = DEFAULT_SENSITIVITY_LEVEL,
        speechVolumeLevel = SpeechVolumeAuthority.DEFAULT_LEVEL,
        speechSpeedLevel = SpeechSpeedAuthority.DEFAULT_LEVEL
    )

    private fun read(relativeUnderMainJava: String): String {
        val path = "app/src/main/java/com/idworx/lisa/$relativeUnderMainJava"
        return ZeroTouchFileProbe.readProjectFile(path)
            ?: error("Missing source file: $path")
    }

    @Test
    fun preferencesRemovedFromCommunicationCategoryMenu() {
        val titles = GuidedVocabularyCatalog.categoryMenuTitles(english)
        assertFalse(titles.contains("Preferences"))
        assertFalse(GuidedVocabularyCategory.Preferences in GuidedVocabularyCategory.ordered)
        assertTrue(titles.contains("Settings & Controls"))
        assertEquals(7, GuidedVocabularyCategory.PAGE_COUNT)
        assertEquals(7, GuidedVocabularyCategory.ordered.size)
        assertEquals(GuidedVocabularyCategory.Custom, GuidedVocabularyCategory.ordered[4])
        assertEquals(
            GuidedVocabularyCategory.AdjustSettings,
            GuidedVocabularyCategory.ordered[GuidedVocabularyCategory.ADJUST_SETTINGS_INDEX]
        )
    }

    @Test
    fun noCategoryShortcutOpensPreferences() {
        for (index in 0 until GuidedVocabularyCategory.PAGE_COUNT) {
            val destination = CategoryAreaDestination.forCategoryIndex(index)
            if (destination is CategoryAreaDestination.CommunicationCategory) {
                assertFalse(destination.category == GuidedVocabularyCategory.Preferences)
            }
        }
        assertEquals(
            CategoryAreaDestination.AdjustSettings,
            CategoryAreaDestination.forCategoryIndex(GuidedVocabularyCategory.ADJUST_SETTINGS_INDEX)
        )
    }

    @Test
    fun staleOutOfRangeIndexFallsBackSafely() {
        val stale = GuidedNavigationState(
            screenMode = GuidedOverlayScreenMode.Vocabulary,
            categoryIndex = 7, // pre-RC8.5 Adjust Settings
            categoryMenuSelection = 7
        ).normalized()
        assertEquals(GuidedVocabularyCategory.ADJUST_SETTINGS_INDEX, stale.categoryIndex)
        assertEquals(
            GuidedVocabularyCategory.AdjustSettings,
            GuidedVocabularyCategory.ordered[stale.categoryIndex]
        )
        val recovered = PreferenceAdjustmentController.recoverFromRemovedPreferences(
            GuidedNavigationState(categoryIndex = 4)
        )
        assertEquals(GuidedOverlayScreenMode.CategoryMenu, recovered.screenMode)
        assertEquals(GuidedPreferencesAdjustMode.None, recovered.preferencesAdjustMode)
    }

    @Test
    fun settingsHubTouchUsesOpenHubSettingNotScrollSequences() {
        val main = read("MainActivity.kt")
        val blockStart = main.indexOf("onGuidedSettingsControl = { kind ->")
        assertTrue(blockStart >= 0)
        val block = main.substring(blockStart, blockStart + 2200)
        assertTrue(block.contains("PreferenceAdjustmentController.openHubSetting"))
        assertTrue(block.contains("SettingsControlKind.Sensitivity"))
        assertTrue(block.contains("SettingsControlKind.ResponseTime"))
        // Must not route Sensitivity/Response Time hub taps through scroll-only sequences alone.
        assertFalse(
            block.contains("SettingsControlKind.Sensitivity -> SettingsAndControlsHubSequences.SENSITIVITY")
        )
    }

    @Test
    fun openHubSettingMatchesBlinkSelectAuthorityForAllFourCards() {
        val hub = PreferenceAdjustmentController.openSettingsMenu(GuidedNavigationState())
        SettingsAndControlsHubSequences.HUB_SETTING_KINDS.forEachIndexed { index, kind ->
            val focused = hub.copy(settingsHubSelection = index)
            val viaSelect = PreferenceAdjustmentController.openSelectedHubSetting(
                focused,
                catalogContext
            )
            val viaTouch = PreferenceAdjustmentController.openHubSetting(
                focused,
                kind,
                catalogContext
            )
            assertEquals(viaSelect.preferencesAdjustMode, viaTouch.preferencesAdjustMode)
            assertEquals(
                when (kind) {
                    SettingsControlKind.Sensitivity -> GuidedPreferencesAdjustMode.Sensitivity
                    SettingsControlKind.ResponseTime -> GuidedPreferencesAdjustMode.ResponseTime
                    SettingsControlKind.SpeechVolume -> GuidedPreferencesAdjustMode.SpeechVolume
                    SettingsControlKind.SpeechSpeed -> GuidedPreferencesAdjustMode.SpeechSpeed
                    else -> error("unexpected hub kind")
                },
                viaTouch.preferencesAdjustMode
            )
        }
    }

    @Test
    fun hubCardClickableCoversFullCardSurface() {
        val ui = read("LisaGuidedModeUi.kt")
        val cardStart = ui.indexOf("private fun SettingsHubCard")
        assertTrue(cardStart >= 0)
        val card = ui.substring(cardStart, cardStart + 1800)
        assertTrue(card.contains(".clickable("))
        assertTrue(card.contains("onClick = onClick"))
        // Clickable is on the outer Row, not an inner Text-only target.
        val clickableIdx = card.indexOf(".clickable(")
        val firstTextIdx = card.indexOf("Text(")
        assertTrue(clickableIdx in 0 until firstTextIdx)
    }

    @Test
    fun settingsBlinkSequencesUnchanged() {
        assertEquals(
            GuidedModeNavigation.PREVIOUS_LEFT to GuidedModeNavigation.PREVIOUS_RIGHT,
            SettingsAndControlsHubSequences.SENSITIVITY
        )
        assertEquals(
            GuidedModeNavigation.NEXT_LEFT to GuidedModeNavigation.NEXT_RIGHT,
            SettingsAndControlsHubSequences.RESPONSE_TIME
        )
        assertEquals(1 to 2, SettingsAndControlsHubSequences.SPEECH_VOLUME)
        assertEquals(3 to 2, SettingsAndControlsHubSequences.SPEECH_SPEED)
        assertEquals(
            GuidedModeNavigation.BACK_LEFT to GuidedModeNavigation.BACK_RIGHT,
            SettingsAndControlsHubSequences.BACK
        )
        assertEquals(6 to 0, EMERGENCY_LEFT_WINKS to EMERGENCY_RIGHT_WINKS)
        assertTrue(GuidedModeNavigation.isAdjustSettingsEntrySequence(5, 5))
    }

    @Test
    fun settingsHubBackReturnsToUnderlyingCommunicationScreen() {
        val fromCategories = GuidedNavigationState(
            screenMode = GuidedOverlayScreenMode.CategoryMenu
        )
        val hub = PreferenceAdjustmentController.openSettingsMenu(fromCategories)
        assertEquals(GuidedPreferencesAdjustMode.SettingsMenu, hub.preferencesAdjustMode)
        val backed = PreferenceAdjustmentController.exitSettingsMenu(hub)
        assertEquals(GuidedPreferencesAdjustMode.None, backed.preferencesAdjustMode)
        assertEquals(GuidedOverlayScreenMode.CategoryMenu, backed.screenMode)
    }
}
