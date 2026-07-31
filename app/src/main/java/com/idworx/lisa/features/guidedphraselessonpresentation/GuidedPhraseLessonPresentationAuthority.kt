package com.idworx.lisa.features.guidedphraselessonpresentation

import com.idworx.lisa.LisaUiStrings
import com.idworx.lisa.PreferredLanguage
import com.idworx.lisa.features.onboardingguide.ui.formatWinkGestureFriendly
import com.idworx.lisa.formatWinkSequenceShort

/**
 * Central presentation for Guided Learning phrase lessons (Communication lessons that teach
 * speaking a production phrase via an eye sequence).
 *
 * Layout order:
 * 1. Intent label — "To say"
 * 2. Phrase — e.g. HELLO
 * 3. Natural-language instruction — e.g. Blink Left Twice
 * 4. Compact production sequence — e.g. L2 R0 (from real left/right counts)
 *
 * Navigation / settings / emergency lessons must not use [intentLabel].
 */
object GuidedPhraseLessonPresentationAuthority {

    fun intentLabel(uiStrings: LisaUiStrings): String = when (uiStrings.language) {
        PreferredLanguage.English -> "To say"
        PreferredLanguage.Afrikaans -> "Om te sê"
        PreferredLanguage.IsiZulu -> "Ukuthi"
    }

    fun naturalLanguageInstruction(left: Int, right: Int): String =
        formatWinkGestureFriendly(left, right)

    fun compactSequence(left: Int, right: Int): String =
        formatWinkSequenceShort(left, right)

    /** True for Communication phrase lessons; false for navigation/settings/action lessons. */
    fun isPhraseSpeakLesson(lessonUsesCommunicationScreen: Boolean): Boolean =
        lessonUsesCommunicationScreen
}
