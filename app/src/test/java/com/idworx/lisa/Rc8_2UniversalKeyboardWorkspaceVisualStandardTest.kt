package com.idworx.lisa

import com.idworx.lisa.features.zerotouchprinciple.audit.ZeroTouchFileProbe
import com.idworx.lisa.ui.theme.LisaStatusGreen
import com.idworx.lisa.ui.theme.LisaWorkspaceVisualStyle
import com.idworx.lisa.ui.theme.SharedKeyboardTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * RC8.2 — Phrase Management / Custom Phrase composer shares Feedback's keyboard visual language.
 * Visual standardization only; behavioural authorities are out of scope.
 */
class Rc8_2UniversalKeyboardWorkspaceVisualStandardTest {

    private fun read(relativeUnderMainJava: String): String {
        val path = "app/src/main/java/com/idworx/lisa/$relativeUnderMainJava"
        return ZeroTouchFileProbe.readProjectFile(path)
            ?: error("Missing source file: $path")
    }

    @Test
    fun sharedKeyboardThemeExistsAsCanonicalTokenSource() {
        val theme = read("ui/theme/SharedKeyboardTheme.kt")
        assertTrue(theme.contains("object SharedKeyboardTheme"))
        assertTrue(theme.contains("SurfaceBackground"))
        assertTrue(theme.contains("StatusBackground"))
        assertTrue(theme.contains("InputCardBackground"))
        assertTrue(theme.contains("ActionBackground"))
        assertTrue(theme.contains("KeyboardTrayBackground"))
        assertTrue(theme.contains("KeyBackground"))
        assertEquals(
            LisaWorkspaceVisualStyle.SolidPanelBackground,
            SharedKeyboardTheme.SurfaceBackground
        )
        assertEquals(
            LisaWorkspaceVisualStyle.OutlinedKeyboardNavBackground,
            SharedKeyboardTheme.ActionBackground
        )
        assertEquals(
            LisaWorkspaceVisualStyle.OutlinedKeyboardNavBorder,
            SharedKeyboardTheme.ActionBorder
        )
        assertEquals(
            LisaWorkspaceVisualStyle.OutlinedKeyboardNavContent,
            SharedKeyboardTheme.ActionContent
        )
    }

    @Test
    fun sharedKeyboardWorkspaceChromeComponentsExist() {
        val chrome = read("KeyboardWorkspaceChrome.kt")
        assertTrue(chrome.contains("fun KeyboardWorkspaceSurface"))
        assertTrue(chrome.contains("fun KeyboardWorkspaceStatus"))
        assertTrue(chrome.contains("fun KeyboardWorkspaceInputCard"))
        assertTrue(chrome.contains("fun KeyboardWorkspaceOutlinedAction"))
        assertTrue(chrome.contains("fun KeyboardWorkspaceOutlinedChip"))
        assertTrue(chrome.contains("fun KeyboardWorkspaceOutlinedActionRow"))
        assertTrue(chrome.contains("fun KeyboardWorkspaceClickableActionCard"))
    }

    @Test
    fun composerStatusBarUsesNeutralChromeNotFullWidthGreenOverlay() {
        val keyboard = read("EyeControlledKeyboard.kt")
        val statusStart = keyboard.indexOf("fun ComposerEyeStatusBar")
        assertTrue(statusStart >= 0)
        val status = keyboard.substring(statusStart, statusStart + 2500)
        assertTrue(status.contains("KeyboardWorkspaceStatus"))
        assertFalse(status.contains("background(LisaStatusGreen)"))
        assertFalse(status.contains(".background(LisaStatusGreen"))
        assertEquals(LisaStatusGreen, SharedKeyboardTheme.StatusReadyIndicator)
        assertTrue(SharedKeyboardTheme.StatusBackground != LisaStatusGreen)
    }

    @Test
    fun phraseComposerUsesKeyboardWorkspaceSurfaceAndInputCard() {
        val composer = read("PhraseComposerUi.kt")
        assertTrue(composer.contains("KeyboardWorkspaceSurface"))
        assertTrue(composer.contains("KeyboardWorkspaceInputCard"))
        assertTrue(composer.contains("SharedKeyboardTheme"))
        assertFalse(composer.contains("OverlayPanelBackground"))
    }

    @Test
    fun composerCommandGridUsesSharedOutlinedActionChrome() {
        val grid = read("ComposerCommandGrid.kt")
        assertTrue(grid.contains("KeyboardWorkspaceClickableActionCard"))
        assertTrue(grid.contains("SharedKeyboardTheme"))
        assertFalse(grid.contains("NavActionEnabledBackground"))
        assertFalse(grid.contains("NavActionGridBackground"))
        assertTrue(grid.contains("ComposerEmergencyCommandCard"))
        assertTrue(grid.contains("LisaEmergencyRed"))
    }

    @Test
    fun feedbackWorkspaceConsumesSharedKeyboardChrome() {
        val feedback = read("MenuDestinationWorkspaceUi.kt")
        assertTrue(feedback.contains("SharedKeyboardTheme"))
        assertTrue(feedback.contains("KeyboardWorkspaceInputCard"))
        assertTrue(feedback.contains("KeyboardWorkspaceOutlinedChip"))
        assertTrue(feedback.contains("KeyboardWorkspaceOutlinedAction"))
        assertTrue(feedback.contains("KeyboardWorkspaceOutlinedActionRow"))
        assertEquals(
            SharedKeyboardTheme.SurfaceBackground,
            LisaWorkspaceVisualStyle.SolidPanelBackground
        )
    }

    @Test
    fun eyeKeyboardKeysAndTrayUseSharedTokens() {
        val keyboard = read("EyeControlledKeyboard.kt")
        assertTrue(
            keyboard.contains("SharedKeyboardTheme.KeyBackground") ||
                keyboard.contains("private val KeyBackground = SharedKeyboardTheme.KeyBackground")
        )
        assertTrue(
            keyboard.contains("SharedKeyboardTheme.KeyboardTrayBackground") ||
                keyboard.contains(
                    "private val KeyboardTrayBackground = SharedKeyboardTheme.KeyboardTrayBackground"
                )
        )
        assertTrue(
            keyboard.contains("SharedKeyboardTheme.KeyHighlightFill") ||
                keyboard.contains("private val KeyHighlightFill = SharedKeyboardTheme.KeyHighlightFill")
        )
        assertEquals(
            SharedKeyboardTheme.KeyCornerRadius.value,
            10f,
            0.001f
        )
    }
}
