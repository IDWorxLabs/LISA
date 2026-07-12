package com.idworx.lisa

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** RC2 — clarity and simplicity regression checks (no behaviour changes). */
class Rc2UiClarityTest {

    private val english = LisaUiStrings(PreferredLanguage.English)

    @Test
    fun pagePurposeLinesAreConcise() {
        assertTrue(english.communicationProfilePurpose.length <= 60)
        assertTrue(english.vocabularyPurpose.length <= 45)
        assertTrue(english.communicationTimingPurpose.length <= 60)
        assertTrue(english.settingsPurpose.length <= 60)
    }

    @Test
    fun voiceHomeSubtitleMatchesRc2Example() {
        assertEquals("Choose how LISA speaks.", english.voiceHomeSubtitle)
    }

    @Test
    fun communicationTimingPurposeDoesNotRepeatTitle() {
        assertFalse(
            english.communicationTimingPurpose.contains("Communication Timing", ignoreCase = true)
        )
    }

    @Test
    fun buttonLabelsUseShortVerbs() {
        assertEquals("Save", english.saveLabel)
        assertEquals("Delete", english.deleteLabel)
        assertEquals("Preview voice", english.testVoice)
    }

    @Test
    fun vocabularyUsesSingleHelpNote() {
        assertEquals(english.vocabularyHelpNote, english.minSequenceNote)
        assertEquals(english.vocabularyHelpNote, english.countdownNote)
    }

    @Test
    fun feedbackIntroIsSingleLine() {
        assertFalse(english.feedbackIntro.contains("\n"))
        assertTrue(english.feedbackIntro.length <= 40)
    }
}
