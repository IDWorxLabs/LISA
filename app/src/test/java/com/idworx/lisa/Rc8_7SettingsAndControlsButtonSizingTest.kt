package com.idworx.lisa

import com.idworx.lisa.features.zerotouchprinciple.audit.ZeroTouchFileProbe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * RC8.7 — Standardize Settings & Controls left-column card sizing.
 * UI-only: equal height/spacing; remove Back grey "2 Left + 2 Right"; no gesture changes.
 */
class Rc8_7SettingsAndControlsButtonSizingTest {

    private fun readUi(): String {
        val path = "app/src/main/java/com/idworx/lisa/LisaGuidedModeUi.kt"
        return ZeroTouchFileProbe.readProjectFile(path)
            ?: error("Missing source file: $path")
    }

    private fun hubPanel(ui: String): String =
        ui.substringAfter("private fun SettingsAndControlsHubPanel(")
            .substringBefore("private fun SettingsHubCard(")

    private fun hubCard(ui: String): String =
        ui.substringAfter("private fun SettingsHubCard(")
            .substringBefore("private fun ListeningControlPanel(")

    @Test
    fun everyLeftColumnCardUsesSharedHeightShapeAndSpacing() {
        val ui = readUi()
        assertTrue(ui.contains("internal object SettingsAndControlsHubVisualStyle"))
        assertTrue(ui.contains("val CardMinHeight"))
        assertTrue(ui.contains("val CardShape"))
        assertTrue(ui.contains("val CardSpacing"))

        val hub = hubPanel(ui)
        // Setting cards (forEach) + Back + Emergency each use the shared weight path.
        assertEquals(3, Regex("""\.weight\(1f\)""").findAll(hub).count())
        assertTrue(hub.contains("cards.forEachIndexed"))
        assertTrue(hub.contains("SettingsAndControlsHubVisualStyle.CardSpacing"))
        assertFalse(hub.contains("Arrangement.SpaceEvenly"))
        assertFalse(hub.contains("AdjustmentInstructionRow("))

        val card = hubCard(ui)
        assertTrue(card.contains("SettingsAndControlsHubVisualStyle.CardMinHeight"))
        assertTrue(card.contains("SettingsAndControlsHubVisualStyle.CardShape"))
        assertFalse(card.contains(".clip("))
    }

    @Test
    fun backCardShowsOnlyTitleAndSequenceWithoutGreyHint() {
        val hub = hubPanel(readUi())
        assertTrue(hub.contains("uiStrings.guidedBack"))
        assertTrue(hub.contains("GuidedModeNavigation.BACK_LEFT"))
        assertTrue(hub.contains("GuidedModeNavigation.BACK_RIGHT"))
        // Must not pass the grey explanatory "2 Left + 2 Right" string into the Back card.
        assertFalse(hub.contains("guidedBackHint"))
        assertFalse(hub.contains("2 Left + 2 Right"))
        // Back uses the shared SettingsHubCard (not AdjustmentInstructionRow).
        val backBlockStart = hub.indexOf("title = uiStrings.guidedBack")
        assertTrue(backBlockStart >= 0)
        val backBlock = hub.substring(
            (backBlockStart - 120).coerceAtLeast(0),
            (backBlockStart + 350).coerceAtMost(hub.length)
        )
        assertTrue(backBlock.contains("SettingsHubCard("))
        assertTrue(backBlock.contains("status = null"))
        assertFalse(backBlock.contains("AdjustmentInstructionRow("))
    }

    @Test
    fun emergencyUsesSharedCardHeightWithEmergencyStyling() {
        val hub = hubPanel(readUi())
        val emergencyIdx = hub.indexOf("title = uiStrings.guidedEmergencyNavTitle")
        assertTrue(emergencyIdx >= 0)
        val emergencyBlock = hub.substring(
            (emergencyIdx - 120).coerceAtLeast(0),
            (emergencyIdx + 320).coerceAtMost(hub.length)
        )
        assertTrue(emergencyBlock.contains("SettingsHubCard("))
        assertTrue(emergencyBlock.contains("emergency = true"))
        assertTrue(emergencyBlock.contains(".weight(1f)"))
        assertTrue(emergencyBlock.contains("EMERGENCY_LEFT_WINKS"))

        val card = hubCard(readUi())
        assertTrue(card.contains("LisaEmergencyRed"))
        assertTrue(card.contains("emergency"))
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
    }

    @Test
    fun hubStillContainsExactlyFourSettingKindsPlusBackAndEmergency() {
        assertEquals(4, SettingsAndControlsHubSequences.HUB_SETTING_KINDS.size)
        val hub = hubPanel(readUi())
        assertTrue(hub.contains("guidedSelectSensitivitySetting"))
        assertTrue(hub.contains("guidedSelectResponseTimeSetting"))
        assertTrue(hub.contains("guidedSelectSpeechVolumeSetting"))
        assertTrue(hub.contains("guidedSelectSpeechSpeedSetting"))
        assertTrue(hub.contains("guidedBack"))
        assertTrue(hub.contains("guidedEmergencyNavTitle"))
    }
}
