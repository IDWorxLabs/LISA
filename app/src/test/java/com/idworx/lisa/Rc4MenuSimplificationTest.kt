package com.idworx.lisa

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** RC4 — menu simplification regression checks (presentation only). */
class Rc4MenuSimplificationTest {

    private val english = LisaUiStrings(PreferredLanguage.English)

    @Test
    fun settingsPurposeMentionsCommunication() {
        assertTrue(english.settingsPurpose.contains("communication", ignoreCase = true))
        assertTrue(english.settingsPurpose.length <= 60)
    }

    @Test
    fun responseSpeedRemainsAvailableInStrings() {
        assertEquals("Response Speed", english.responseSpeedTitle)
    }

    @Test
    fun communicationTimingStringNotRequiredAsMenuItem() {
        assertFalse(english.menuLabel(LisaPanel.Settings).contains("Timing", ignoreCase = true))
    }
}
