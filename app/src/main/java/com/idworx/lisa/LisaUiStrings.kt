package com.idworx.lisa

import java.util.Locale

/**
 * User-facing UI strings for LISA V1.1 (South Africa: English, Afrikaans, isiZulu).
 * Developer Mode labels remain English in calling code.
 */
data class LisaUiStrings(val language: PreferredLanguage) {

    private fun t(en: String, af: String, zu: String): String = when (language) {
        PreferredLanguage.English -> en
        PreferredLanguage.Afrikaans -> af
        PreferredLanguage.IsiZulu -> zu
    }

    val lisaFullName: String get() = t(
        "LISA — Locked In Syndrome App",
        "LISA — Locked In Syndrome App",
        "LISA — Locked In Syndrome App"
    )

    val lisaTagline: String get() = t(
        "Assistive communication through intentional eye movements.",
        "Hulpkommunikasie deur doelbewuste oogbewegings.",
        "Ukuxhumana okusizayo ngokunyakazisa kwamehlo okuhloswe ngakho."
    )

    // Main actions
    val menu: String get() = t("Menu", "Kieslys", "Imenyu")
    val close: String get() = t("Close", "Sluit", "Vala")
    val reset: String get() = t("Reset", "Herstel", "Setha kabusha")
    val repeat: String get() = t("Repeat", "Herhaal", "Phinda")
    val back: String get() = t("Back", "Terug", "Emuva")
    val save: String get() = t("Save", "Stoor", "Londoloza")
    val next: String get() = t("Next", "Volgende", "Okulandelayo")
    val startLisa: String get() = t("Start LISA", "Begin LISA", "Qala i-LISA")
    val add: String get() = t("Add", "Voeg by", "Engeza")
    val cancel: String get() = t("Cancel", "Kanselleer", "Khansela")

    // Communication display
    val ready: String get() = t("READY", "GEREE", "SILINDILE")
    val listening: String get() = t("LISTENING...", "LUISTER...", "IYALALELE...")
    val watchingYourEyes: String get() = t(
        "Watching your eyes...",
        "Kykers na jou oë...",
        "Ibabheke amehlo akho..."
    )
    val buildingYourMessage: String get() = t(
        "Building your message...",
        "Bou jou boodskap...",
        "Yakha umlayezo wakho..."
    )
    val buildingMessage: String get() = t("BUILDING MESSAGE", "BOU Boodskap", "YAKHA UMLAYEZO")
    fun buildingLeftDots(count: Int): String = t("Left: ${winkDots(count)}", "Links: ${winkDots(count)}", "Kwesokunxele: ${winkDots(count)}")
    fun buildingRightDots(count: Int): String = t("Right: ${winkDots(count)}", "Regs: ${winkDots(count)}", "Kwesokudla: ${winkDots(count)}")
    val listeningPaused: String get() = t("LISTENING PAUSED", "LUISTER GEPAUZEER", "UKULALELA KUMISWE")
    val tapOrWinkToResume: String get() = t("Use L2 R2 or Resume to continue", "Gebruik L2 R2 of Hervat om voort te gaan", "Sebenzisa L2 R2 noma Qhubeka ukuze uqhubeke")

    // System language
    val systemCommandsTitle: String get() = t("SYSTEM COMMANDS", "STELSELOPDRAGTE", "IMIYALELO YOHLELO")
    val systemCommandsNote: String get() = t(
        "These control LISA. They do not speak aloud.",
        "Hierdie beheer LISA. Hulle praat nie hardop nie.",
        "Lezi zilawula i-LISA. Azikhulumi ngokuzwakalayo."
    )
    val systemOpenQuickControls: String get() = t("Open Quick Controls", "Maak Vinnige Kontroles oop", "Vula Izilawuli Ezisheshayo")
    val systemCloseHelp: String get() = t("Close Help", "Sluit Hulp", "Vala Usizo")
    val systemSpeedFast: String get() = t("Set Response Speed: Fast", "Stel Reaksiespoed: Vinnig", "Setha Isivinini: Shesha")
    val systemSpeedNormal: String get() = t("Set Response Speed: Normal", "Stel Reaksiespoed: Normaal", "Setha Isivinini: Okujwayelekile")
    val systemSpeedSlow: String get() = t("Set Response Speed: Slow", "Stel Reaksiespoed: Stadig", "Setha Isivinini: Kancane")
    val systemSensitivityDecrease: String get() = t("Decrease sensitivity", "Verminder sensitiwiteit", "Nciphisa ukuzwela")
    val systemSensitivityIncrease: String get() = t("Increase sensitivity", "Verhoog sensitiwiteit", "Khulisa ukuzwela")
    val systemRepeatLast: String get() = t("Repeat last phrase", "Herhaal laaste frase", "Phinda ibinzana lokugcina")
    val systemTogglePause: String get() = t("Toggle Pause / Resume listening", "Wissel Pauzeer / Hervat luister", "Shintsha Ukumisa / Qhubeka ukulalela")
    val systemOpenPractice: String get() = t("Open Practice Mode", "Maak Oefenmodus oop", "Vula Imodi Yokuqeqesha")
    val systemCloseQuickControls: String get() = t("Close Quick Controls", "Sluit Vinnige Kontroles", "Vala Izilawuli Ezisheshayo")
    val quickControlsEyeHint: String get() = t(
        "Use the wink sequences shown beside each option.",
        "Gebruik die knip-reekse langs elke opsie.",
        "Sebenzisa uchungechunge lwama-wink obonisa eceleni kwezinketho."
    )
    val quickControlsLanguage: String get() = t("Language", "Taal", "Ulimi")
    val quickControlsVolume: String get() = t("Volume", "Volume", "Ivolumu")
    val quickControlsPauseListening: String get() = t("Pause Listening", "Pauzeer Luister", "Misa Ukulalela")
    val quickControlsResumeListening: String get() = t("Resume Listening", "Hervat Luister", "Qhubeka Ukulalela")
    val quickControlsPracticeMode: String get() = t("Practice Mode", "Oefenmodus", "Imodi Yokuqeqesha")

    // Practice Mode
    val practiceModeTitle: String get() = t("Practice Mode", "Oefenmodus", "Imodi Yokuqeqesha")
    val practiceTrySaying: String get() = t("Try saying:", "Probeer sê:", "Zama ukuthi:")
    val practiceSequence: String get() = t("Sequence:", "Reeks:", "Uchungechunge:")
    val practiceCorrect: String get() = t("Correct", "Korrek", "Kulungile")
    val practiceAlmost: String get() = t("Almost. Try again.", "Amper. Probeer weer.", "Cishe. Zama futhi.")
    val practiceTryAgain: String get() = t("Try again", "Probeer weer", "Zama futhi")
    val practiceCloseHint: String get() = t("L4 R0 closes practice", "L4 R0 sluit oefening", "L4 R0 ivala ukuqeqeshwa")

    // Guided Vocabulary Mode
    val guidedModeTitle: String get() = t("Guided Vocabulary", "Begeleide Woordeskat", "Ulimi Olukhokhelwayo")
    val guidedVocabularyTitle: String get() = t("Vocabulary", "Woordeskat", "Ulimi")
    val guidedCategoryMenuMode: String get() = t("Choose Category", "Kies Kategorie", "Khetha Isigaba")
    val guidedCategoryMenuTitle: String get() = t("Choose a Category", "Kies 'n Kategorie", "Khetha Isigaba")
    val guidedChooseCategoryAction: String get() = t("Choose Category", "Kies Kategorie", "Khetha Isigaba")
    val guidedSelectEnter: String get() = t("Select / Enter", "Kies / Enter", "Khetha / Ngena")
    val guidedSelectEnterHint: String get() = t("1 Left + 1 Right", "1 Links + 1 Regs", "1 Kwesokunxele + 1 Kwesokudla")
    val guidedBack: String get() = t("Back", "Terug", "Emuva")
    val guidedBackHint: String get() = t("2 Left + 2 Right", "2 Links + 2 Regs", "2 Kwesokunxele + 2 Kwesokudla")
    val guidedMoveUpCategory: String get() = t("Move Up Category", "Skuif Kategorie Op", "Hambisa Isigaba Phezulu")
    val guidedMoveDownCategory: String get() = t("Move Down Category", "Skuif Kategorie Af", "Hambisa Isigaba Phansi")
    val guidedOpenSelectedCategory: String get() = t("Open Selected Category", "Open Gekose Kategorie", "Vula Isigaba Esikhethiwe")
    val guidedBackToPhrases: String get() = t("Back to Phrases", "Terug na Frases", "Buyela Emagameni")
    fun guidedPhrasePageIndicator(page: Int, total: Int): String =
        t("Phrases $page / $total", "Frases $page / $total", "Amagama $page / $total")
    fun guidedCategoryIndicator(category: Int, total: Int): String =
        t("Category $category / $total", "Kategorie $category / $total", "Isigaba $category / $total")
    val guidedOpenMode: String get() = t("Open Guided Vocabulary", "Open Begeleide Woordeskat", "Vula Ulimi Olukhokhelwayo")
    fun guidedPageIndicator(page: Int, total: Int): String =
        t("Page $page / $total", "Bladsy $page / $total", "Ikhasi $page / $total")
    private val guidedScrollDownForMorePhrases: String get() = t(
        "Scroll down for more phrases",
        "Blaai af vir meer frases",
        "Skrolela phansi ukuze uthole amagama amaningi"
    )
    private val guidedScrollUpOrDownForMorePhrases: String get() = t(
        "Scroll up or down for more phrases",
        "Blaai op of af vir meer frases",
        "Skrolela phezulu noma phansi ukuze uthole amagama amaningi"
    )
    private val guidedScrollUpForPreviousPhrases: String get() = t(
        "Scroll up for previous phrases",
        "Blaai op vir vorige frases",
        "Skrolela phezulu ukuze uthole amagama angaphambili"
    )

    /**
     * Directional, page-aware phrase-list scroll hint — single source of truth so the hint text
     * always matches the actual [phrasePageIndex]/[phrasePageCount] the runtime is tracking,
     * instead of a generic "scroll for more" string that never changes. Returns null when there
     * is only one phrase page (no scrolling is possible, so no hint is shown).
     */
    fun guidedPhrasePageScrollHint(phrasePageIndex: Int, phrasePageCount: Int): String? {
        if (phrasePageCount <= 1) return null
        val isFirstPage = phrasePageIndex <= 0
        val isLastPage = phrasePageIndex >= phrasePageCount - 1
        return when {
            isFirstPage -> guidedScrollDownForMorePhrases
            isLastPage -> guidedScrollUpForPreviousPhrases
            else -> guidedScrollUpOrDownForMorePhrases
        }
    }
    val guidedScrollUp: String get() = t("Scroll Up", "Blaai Op", "Skrolela Phezulu")
    val guidedPreviousPhrasePage: String get() = t("Previous Phrase Page", "Vorige Frasebladsy", "Ikhasi Lamagama Langaphambili")
    val guidedScrollUpHint: String get() = t("2 Left Winks", "2 Linkerknippe", "Ukucwayiza Okubili Kwesokunxele")
    val guidedScrollDown: String get() = t("Scroll Down", "Blaai Af", "Skrolela Phansi")
    val guidedNextPhrasePage: String get() = t("Next Phrase Page", "Volgende Frasebladsy", "Ikhasi Lamagama Elilandelayo")
    val guidedScrollDownHint: String get() = t("2 Right Winks", "2 Regterknippe", "Ukucwayiza Okubili Kwesokudla")
    /**
     * Single source of truth for every Navigation Panel gesture hint: derives directly from the
     * real left/right counts a gesture requires (never a separately hardcoded copy), and — unlike
     * a blanket "<n> Left + <n> Right" template — reads unambiguously when one side is zero, so a
     * single-eye-only gesture like Emergency (L6 R0) never implies the *other* eye is also needed.
     */
    private fun guidedGestureHint(left: Int, right: Int): String = when {
        left > 0 && right == 0 -> t(
            "$left Left ${if (left == 1) "Wink" else "Winks"}",
            "$left Links",
            "$left Kwesokunxele"
        )
        right > 0 && left == 0 -> t(
            "$right Right ${if (right == 1) "Wink" else "Winks"}",
            "$right Regs",
            "$right Kwesokudla"
        )
        else -> t("$left Left + $right Right", "$left Links + $right Regs", "$left Kwesokunxele + $right Kwesokudla")
    }

    val guidedEmergencyNavTitle: String get() = t("Emergency", "Nood", "Usizo Oluphuthumayo")
    // Interpolates EMERGENCY_LEFT_WINKS/EMERGENCY_RIGHT_WINKS directly (same single source of
    // truth isEmergencySequence checks) instead of a hardcoded "L6 R0" literal.
    val guidedEmergencyNavHint: String get() = guidedGestureHint(EMERGENCY_LEFT_WINKS, EMERGENCY_RIGHT_WINKS)
    val guidedCategoriesNavTitle: String get() = t("Categories", "Kategorieë", "Izigaba")
    val guidedCategoriesNavHint: String get() =
        guidedGestureHint(GuidedModeNavigation.CATEGORIES_LEFT, GuidedModeNavigation.CATEGORIES_RIGHT)
    val guidedEyeTrackingActive: String get() = t("Eyes tracked", "Oë dopgehou", "Amehlo alandwa")
    val guidedEyeTrackingWaiting: String get() = t("Waiting for face", "Wag vir gesig", "Ilinde ubuso")
    val guidedPhraseConfirmed: String get() = t("Spoken", "Gesê", "Kukhulunyiwe")
    val guidedActionConfirmed: String get() = t("Done", "Klaar", "Kwenziwe")
    val guidedHelpSpoken: String get() = t(
        "Blink the sequence beside a phrase to speak it. ${formatWinkSequenceShort(GuidedModeNavigation.CATEGORIES_LEFT, GuidedModeNavigation.CATEGORIES_RIGHT)} opens categories. L2 R0 and L0 R2 scroll pages. L2 R2 goes back.",
        "Knip die reeks langs 'n frase om dit te sê. ${formatWinkSequenceShort(GuidedModeNavigation.CATEGORIES_LEFT, GuidedModeNavigation.CATEGORIES_RIGHT)} open kategorieë. L2 R0 en L0 R2 blaai bladsye. L2 R2 gaan terug.",
        "Cwayiza uchungechunge oluseceleni kwesigwebo ukukukhuluma. I-${formatWinkSequenceShort(GuidedModeNavigation.CATEGORIES_LEFT, GuidedModeNavigation.CATEGORIES_RIGHT)} ivula izigaba. I-L2 R0 ne-L0 R2 iskrolela amakhasi. I-L2 R2 ibuyela emuva."
    )
    val workspaceCommunicationTitle: String get() = t("Communication Workspace", "Kommunikasiewerkruimte", "Indawo Yokuxhumana")

    fun guidedCategoryTitle(category: GuidedVocabularyCategory): String = when (category) {
        GuidedVocabularyCategory.Conversation -> t("Conversation", "Gesprek", "Ingxoxo")
        GuidedVocabularyCategory.BasicNeeds -> t("Basic Needs", "Basiese Behoeftes", "Izidingo Eziyisisekelo")
        GuidedVocabularyCategory.Medical -> t("Medical", "Medies", "Ezesimpilo")
        GuidedVocabularyCategory.Family -> t("Family", "Familie", "Umndeni")
        GuidedVocabularyCategory.BasicSystemControls -> t("Basic System Controls", "Basiese Stelselkontroles", "Izilawuli Zesistimu Eziyisisekelo")
        GuidedVocabularyCategory.Preferences -> t("Preferences", "Voorkeure", "Okuncanyelwayo")
    }

    fun guidedCurrentResponseTime(seconds: Int): String =
        t("Current response time: $seconds seconds", "Huidige reaksietyd: $seconds sekondes", "Isikhathi sokuphendula samanje: $seconds imizuzwana")

    fun guidedSetResponseTimeTo(seconds: Int): String =
        t("Set response time to $seconds seconds", "Stel reaksietyd op $seconds sekondes", "Setha isikhathi sokuphendula ku-$seconds imizuzwana")

    fun guidedCurrentSensitivity(level: Int): String =
        t("Current sensitivity: $level", "Huidige sensitiwiteit: $level", "Ukuzwela kwamanje: $level")

    fun guidedSetSensitivityTo(level: Int): String =
        t("Set sensitivity to $level", "Stel sensitiwiteit op $level", "Setha ukuzwela ku-$level")

    val guidedDecreaseSensitivity: String get() = t("Decrease sensitivity", "Verminder sensitiwiteit", "Nciphisa ukuzwela")
    val guidedIncreaseSensitivity: String get() = t("Increase sensitivity", "Verhoog sensitiwiteit", "Khulisa ukuzwela")
    val guidedAdjustResponseTime: String get() = t("Adjust response time", "Stel reaksietyd", "Lungisa isikhathi sokuphendula")
    val guidedAdjustSensitivity: String get() = t("Adjust sensitivity", "Stel sensitiwiteit", "Lungisa ukuzwela")
    val guidedResponseTimeTitle: String get() = t("Response Time", "Reaksietyd", "Isikhathi Sokuphendula")
    val guidedSensitivityTitle: String get() = t("Sensitivity", "Sensitiwiteit", "Ukuzwela")
    val guidedDecreaseValue: String get() = t("Decrease value", "Verminder waarde", "Nciphisa inani")
    val guidedIncreaseValue: String get() = t("Increase value", "Verhoog waarde", "Khulisa inani")
    val guidedSaveSelectedValue: String get() = t("Save selected value", "Stoor gekose waarde", "Londoloza inani elikhethiwe")
    val guidedCancelAdjustment: String get() = t("Cancel / Back", "Kanselleer / Terug", "Khansela / Emuva")
    val guidedDecreaseResponseTime: String get() = t("Decrease response time", "Verminder reaksietyd", "Nciphisa isikhathi sokuphendula")
    val guidedIncreaseResponseTime: String get() = t("Increase response time", "Verhoog reaksietyd", "Khulisa isikhathi sokuphendula")
    val guidedSaveResponseTime: String get() = t("Select / Save response time", "Kies / Stoor reaksietyd", "Khetha / Londoloza isikhathi sokuphendula")
    val guidedSaveSensitivity: String get() = t("Select / Save sensitivity", "Kies / Stoor sensitiwiteit", "Khetha / Londoloza ukuzwela")
    val guidedCancelToPreferences: String get() = t("Cancel / Back to Preferences", "Kanselleer / Terug na Voorkeure", "Khansela / Buyela Kokuncanyelwayo")
    fun guidedDraftResponseTime(seconds: Int): String =
        t("$seconds seconds", "$seconds sekondes", "$seconds imizuzwana")
    fun guidedDraftSensitivity(level: Int): String = level.toString()
    fun guidedAdjustmentCurrentValueResponseTime(seconds: Int): String =
        t("Current value: $seconds seconds", "Huidige waarde: $seconds sekondes", "Inani lamanje: $seconds imizuzwana")
    fun guidedAdjustmentCurrentValueSensitivity(level: Int): String =
        t("Current value: $level", "Huidige waarde: $level", "Inani lamanje: $level")

    fun listeningStatusLine(sensitivityLevel: Int, responseTimeSec: Int): String =
        t(
            "Sensitivity: $sensitivityLevel · Response time: ${responseTimeSec}s",
            "Sensitiwiteit: $sensitivityLevel · Reaksietyd: ${responseTimeSec}s",
            "Ukuzwela: $sensitivityLevel · Isikhathi sokuphendula: ${responseTimeSec}s"
        )

    fun guidedPreferenceLabel(
        labelKey: String,
        responseTimeSec: Int,
        sensitivityLevel: Int,
        value: Int?
    ): String = when (labelKey) {
        "current_response_time" -> guidedCurrentResponseTime(responseTimeSec)
        "current_sensitivity" -> guidedCurrentSensitivity(sensitivityLevel)
        "adjust_response_time" -> guidedAdjustResponseTime
        "adjust_sensitivity" -> guidedAdjustSensitivity
        "decrease_sensitivity" -> guidedDecreaseSensitivity
        "increase_sensitivity" -> guidedIncreaseSensitivity
        else -> labelKey
    }

    fun guidedPreferenceLabelEnglish(
        labelKey: String,
        responseTimeSec: Int,
        sensitivityLevel: Int,
        value: Int?
    ): String? {
        if (language == PreferredLanguage.English) return null
        return when (labelKey) {
            "current_response_time" -> "Current response time: $responseTimeSec seconds"
            "current_sensitivity" -> "Current sensitivity: $sensitivityLevel"
            "adjust_response_time" -> "Adjust response time"
            "adjust_sensitivity" -> "Adjust sensitivity"
            "decrease_sensitivity" -> "Decrease sensitivity"
            "increase_sensitivity" -> "Increase sensitivity"
            else -> null
        }
    }

    fun guidedPhrase(phraseKey: String): String = when (phraseKey) {
        "stop" -> t("Stop", "Stop", "Misa")
        "i_am_okay" -> t("I am okay", "Ek is oukei", "Ngiyaphila")
        "please_repeat_that" -> t("Please repeat that", "Herhaal dit asseblief", "Ngicela uphinde lokho")
        "i_need_some_water" -> t("I need some water.", "Ek het water nodig.", "Ngidinga amanzi.")
        "i_am_hungry" -> t("I am hungry.", "Ek is honger.", "Ngilambile.")
        "i_need_to_use_the_bathroom" -> t("I need to use the bathroom.", "Ek moet die badkamer toe.", "Ngidinga ukusebenzisa indlu yangasese.")
        "i_am_tired" -> t("I am tired.", "Ek is moeg.", "Ngikhathele.")
        "i_am_cold" -> t("I am cold.", "Ek is koud.", "Ngiyabanda.")
        "i_am_hot" -> t("I am hot.", "Ek is warm.", "Ngiyashisa.")
        "please_help_me_move" -> t("Please help me move.", "Help my om te beweeg asseblief.", "Ngicela ungisize ngihambe.")
        "please_reposition_me" -> t("Please reposition me.", "Herposisioneer my asseblief.", "Ngicela ungibeka ngendlela efanele.")
        "i_am_uncomfortable" -> t("I am uncomfortable.", "Ek voel ongemaklik.", "Angizolile.")
        "i_would_like_some_privacy" -> t("I would like some privacy.", "Ek wil graag privaatheid hê.", "Ngingathanda ubumfihlo.")
        "i_am_in_pain" -> t("I am in pain.", "Ek het pyn.", "Ngibuhlungu.")
        "please_call_the_nurse" -> t("Please call the nurse.", "Bel die verpleegster asseblief.", "Ngicela ubize umongikazi.")
        "i_need_my_medication" -> t("I need my medication.", "Ek het my medisyne nodig.", "Ngidinga umuthi wami.")
        "i_am_having_difficulty_breathing" -> t("I am having difficulty breathing.", "Ek sukkel om asem te haal.", "Ngina nzima ukuphefumula.")
        "i_feel_dizzy" -> t("I feel dizzy.", "Ek voel duizelig.", "Ngizizwa ngidizela.")
        "my_chest_hurts" -> t("My chest hurts.", "My bors seer.", "Ngibuhlungu esifubeni.")
        "i_do_not_feel_well" -> t("I do not feel well.", "Ek voel nie lekker nie.", "Angiphilile kahle.")
        "please_call_the_doctor" -> t("Please call the doctor.", "Bel die dokter asseblief.", "Ngicela ubize udokotela.")
        "please_check_my_body" -> t("Please check my body.", "Kyk my liggaam asseblief.", "Ngicela uhlale umzimba wami.")
        "please_help_me_now" -> t("Please help me now.", "Help my nou asseblief.", "Ngicela ungisize manje.")
        "i_want_to_see_my_mom" -> t("I want to see my mom.", "Ek wil my ma sien.", "Ngifuna ukubona umama wami.")
        "i_want_to_see_my_dad" -> t("I want to see my dad.", "Ek wil my pa sien.", "Ngifuna ukubona ubaba wami.")
        "i_want_to_see_my_wife" -> t("I want to see my wife.", "Ek wil my vrou sien.", "Ngifuna ukubona umkakazi wami.")
        "i_want_to_see_my_husband" -> t("I want to see my husband.", "Ek wil my man sien.", "Ngifuna ukubona umyeni wami.")
        "i_want_to_see_my_child" -> t("I want to see my child.", "Ek wil my kind sien.", "Ngifuna ukubona ingane yami.")
        "i_want_to_see_my_friend" -> t("I want to see my friend.", "Ek wil my vriend sien.", "Ngifuna ukubona umngane wami.")
        "call_my_family" -> t("Call my family.", "Bel my familie.", "Shayela umndeni wami.")
        "i_miss_you" -> t("I miss you.", "Ek mis jou.", "Ngiyakukhumbula.")
        "stay_with_me" -> t("Stay with me.", "Bly by my.", "Hlala kimi.")
        "i_want_to_talk" -> t("I want to talk.", "Ek wil praat.", "Ngifuna ukukhuluma.")
        else -> phraseKey
    }

    fun guidedPhraseEnglish(phraseKey: String): String? {
        if (language == PreferredLanguage.English) return null
        return when (phraseKey) {
            "stop" -> "Stop"
            "i_am_okay" -> "I am okay"
            "please_repeat_that" -> "Please repeat that"
            "i_need_some_water" -> "I need some water."
            "i_am_hungry" -> "I am hungry."
            "i_need_to_use_the_bathroom" -> "I need to use the bathroom."
            "i_am_tired" -> "I am tired."
            "i_am_cold" -> "I am cold."
            "i_am_hot" -> "I am hot."
            "please_help_me_move" -> "Please help me move."
            "please_reposition_me" -> "Please reposition me."
            "i_am_uncomfortable" -> "I am uncomfortable."
            "i_would_like_some_privacy" -> "I would like some privacy."
            "i_am_in_pain" -> "I am in pain."
            "please_call_the_nurse" -> "Please call the nurse."
            "i_need_my_medication" -> "I need my medication."
            "i_am_having_difficulty_breathing" -> "I am having difficulty breathing."
            "i_feel_dizzy" -> "I feel dizzy."
            "my_chest_hurts" -> "My chest hurts."
            "i_do_not_feel_well" -> "I do not feel well."
            "please_call_the_doctor" -> "Please call the doctor."
            "please_check_my_body" -> "Please check my body."
            "please_help_me_now" -> "Please help me now."
            "i_want_to_see_my_mom" -> "I want to see my mom."
            "i_want_to_see_my_dad" -> "I want to see my dad."
            "i_want_to_see_my_wife" -> "I want to see my wife."
            "i_want_to_see_my_husband" -> "I want to see my husband."
            "i_want_to_see_my_child" -> "I want to see my child."
            "i_want_to_see_my_friend" -> "I want to see my friend."
            "call_my_family" -> "Call my family."
            "i_miss_you" -> "I miss you."
            "stay_with_me" -> "Stay with me."
            "i_want_to_talk" -> "I want to talk."
            else -> null
        }
    }

    fun guidedSystemLabel(labelKey: String): String = when (labelKey) {
        "repeat_last" -> t("Repeat the last message.", "Herhaal die laaste boodskap.", "Phinda umlayezo wakamuva.")
        "increase_volume" -> t("Increase the speaking volume.", "Verhoog die spraakvolume.", "Khuphula ivolumu yokukhuluma.")
        "decrease_volume" -> t("Decrease the speaking volume.", "Verlaag die spraakvolume.", "Nciphisa ivolumu yokukhuluma.")
        "slower_speech" -> t("Speak more slowly.", "Praat stadiger.", "Khuluma kancane.")
        "faster_speech" -> t("Speak faster.", "Praat vinniger.", "Khuluma ngokushesha.")
        "pause_listening" -> t("Pause listening.", "Pauzeer luister.", "Misa ukulalela.")
        "resume_listening" -> t("Resume listening.", "Hervat luister.", "Qhubeka ukulalela.")
        "open_menu" -> t("Open the menu.", "Open die kieslys.", "Vula imenyu.")
        "reset_sequence" -> t("Reset my input.", "Herstel my invoer.", "Setha kabusha okokufaka kwami.")
        "help" -> t("Show help.", "Wys hulp.", "Bonisa usizo.")
        else -> labelKey
    }

    fun guidedSystemLabelEnglish(labelKey: String): String? {
        if (language == PreferredLanguage.English) return null
        return when (labelKey) {
            "repeat_last" -> "Repeat the last message."
            "increase_volume" -> "Increase the speaking volume."
            "decrease_volume" -> "Decrease the speaking volume."
            "slower_speech" -> "Speak more slowly."
            "faster_speech" -> "Speak faster."
            "pause_listening" -> "Pause listening."
            "resume_listening" -> "Resume listening."
            "open_menu" -> "Open the menu."
            "reset_sequence" -> "Reset my input."
            "help" -> "Show help."
            else -> null
        }
    }

    val waiting: String get() = t("WAITING...", "WAG...", "IYALINDA...")
    val continueYourSequence: String get() = t(
        "You can continue",
        "Jy kan voortgaan",
        "Ungaqhubeka"
    )
    val takeYourTime: String get() = t(
        "Take your time",
        "Neem jou tyd",
        "Thatha isikhathi sakho"
    )
    val noPhraseMatched: String get() = t(
        "No phrase matched.",
        "Geen frase ooreenstem nie.",
        "Akukho sigwebo esifanayo."
    )
    val tryAgainPrompt: String get() = t(
        "Try again.",
        "Probeer weer.",
        "Zama futhi."
    )
    val processing: String get() = t("PROCESSING...", "VERWERK...", "IYACUBUNGA...")
    val understandingYourMessage: String get() = t(
        "Understanding your message...",
        "Verstaan jou boodskap...",
        "Iyaqonda umlayezo wakho..."
    )
    val iUnderstood: String get() = t("I UNDERSTOOD", "EK HET VERSTAAN", "NGIYIQONDE")
    val speakingIn: String get() = t("Speaking in", "Praat oor", "Izokukhuluma nge")
    val speaking: String get() = t("SPEAKING", "PRAAT", "IYAKHULUMA")
    val messageDelivered: String get() = t("MESSAGE DELIVERED", "BOODSKAP AFGELEWER", "UMLAYEZO UTHOLEWE")
    val emergency: String get() = t("EMERGENCY", "NOOD", "USIZO OLUPHUTHUMAYO")
    val callingForHelp: String get() = t(
        "Calling for help...",
        "Roep om hulp...",
        "Ibizela usizo..."
    )
    val possibleMatch: String get() = t("POSSIBLE MATCH", "MOONTLIKE TREFFER", "KUNGASE KULINGANE")
    val continueOrPause: String get() = t(
        "You can continue",
        "Jy kan voortgaan",
        "Ungaqhubeka"
    )

    // Response speed
    val responseSpeedTitle: String get() = t("Response Speed", "Reaksiespoed", "Isivinini Sempendulo")
    fun responseSpeedLabel(speed: ResponseSpeed): String = when (speed) {
        ResponseSpeed.Fast -> t("Fast", "Vinnig", "Shesha")
        ResponseSpeed.Normal -> t("Normal", "Normaal", "Okujwayelekile")
        ResponseSpeed.Slow -> t("Slow", "Stadig", "Kancane")
    }
    fun responseSpeedDescription(speed: ResponseSpeed): String = when (speed) {
        ResponseSpeed.Fast -> t("Shorter wait", "Korter wag", "Ukulinda okufushane")
        ResponseSpeed.Normal -> t("Recommended", "Aanbeveel", "Kunconywa")
        ResponseSpeed.Slow -> t("More time to finish sequences", "Meer tyd om reekse te voltooi", "Isikhathi esiningi sokuqeda uchungechunge")
    }
    val slower: String get() = t("Slower", "Stadiger", "Kancane kakhulu")
    val faster: String get() = t("Faster", "Vinniger", "Shesha kakhulu")
    val communicationSetupIntro: String get() = t(
        "Adjust how long LISA waits after your last wink before processing a sequence.",
        "Pas aan hoe lank LISA wag na jou laaste knip voordat 'n reeks verwerk word.",
        "Lungisa ukuthi i-LISA ilinda isikhathi esingakanani ngemuva kokucwayiza kwakho kokugcina ngaphambi kokucubunga uchungechunge."
    )
    val settingsSavedToProfileHint: String get() = t(
        "Settings are saved to the active communication profile.",
        "Instellings word op die aktiewe kommunikasieprofiel gestoor.",
        "Izilungiselelo zigcinwa kuphrofayela yokuxhumana esebenzayo."
    )

    // Unknown sequence help
    val unknownHelpHeadline: String get() = t(
        "I'm not sure what you wanted to say.",
        "Ek is nie seker wat jy wou sê nie.",
        "Angiqiniseki ukuthi ubelufuna ukuthini."
    )
    val unknownHelpSubheadline: String get() = t(
        "Did you mean:",
        "Het jy bedoel:",
        "Ubuhlosile ukuthi:"
    )
    val unknownHelpDismissHint: String get() = t(
        "This help closes automatically, or use L4 R0 to close help.",
        "Hierdie hulp sluit outomaties, of gebruik L4 R0 om hulp te sluit.",
        "Lolu suzo luvala ngokuzenzakalelayo, noma sebenzisa L4 R0 ukuvala usizo."
    )
    fun winkSequenceDescription(left: Int, right: Int): String = when {
        left > 0 && right > 0 -> t(
            "$left left, $right right winks",
            "$left links, $right regter knippe",
            "ama-wink angu-$left kwesokunxele, angu-$right kwesokudla"
        )
        left > 0 -> t(
            "$left left winks",
            "$left linkerknippe",
            "ama-wink angu-$left kwesokunxele"
        )
        right > 0 -> t(
            "$right right winks",
            "$right regterknippe",
            "ama-wink angu-$right kwesokudla"
        )
        else -> t("—", "—", "—")
    }

    // Quick Controls
    val quickControlsTitle: String get() = t("Quick Controls", "Vinnige Kontroles", "Izilawuli Ezisheshayo")
    val closeQuickControls: String get() = t("Close", "Sluit", "Vala")

    val wouldNotify: String get() = t("Would notify:", "Sou in kennis stel:", "Kuzomazisa:")

    // Countdown
    val leftWinkCancel: String get() = t("Left wink = Cancel", "Linkerknik = Kanselleer", "Ukucwayiza kwesokunxele = Khansela")
    val rightWinkSpeak: String get() = t("Right wink = Speak now", "Regterknik = Praat nou", "Ukucwayiza kwesokudla = Khuluma manje")
    val editSequence: String get() = t("Edit sequence", "Wysig reeks", "Hlela uchungechunge")

    // Timeline
    val timelineWatching: String get() = t("Watching", "Kyk", "Ibabheka")
    val timelineListening: String get() = t("Listening", "Luister", "Iyalalela")
    val timelineUnderstanding: String get() = t("Understanding", "Verstaan", "Iyaqonda")
    val timelineConfirming: String get() = t("Confirming", "Bevestig", "Iyaqinisekisa")
    val timelineSpeaking: String get() = t("Speaking", "Praat", "Iyakhuluma")
    val timelineDelivered: String get() = t("Delivered", "Afgelewer", "Kulethiwe")

    // Sensitivity
    val sensitivity: String get() = t("Sensitivity", "Sensitiwiteit", "Ukuzwela")
    val sensitivityDecrease: String get() = t("Sensitivity -", "Sensitiwiteit -", "Ukuzwela -")
    val sensitivityIncrease: String get() = t("Sensitivity +", "Sensitiwiteit +", "Ukuzwela +")

    // Menu items
    val myCommunication: String get() = t("My Communication", "My Kommunikasie", "Ukuxhumana Kwami")
    val communicationSetup: String get() = t("Communication Setup", "Kommunikasie-opstelling", "Ukusetha Ukuxhumana")
    val vocabularyTraining: String get() = t("Vocabulary / Training", "Woordeskat / Opleiding", "Ulimi / Ukuqeqeshwa")
    val emergencySetup: String get() = t("Emergency Setup", "Noodopstelling", "Ukusetha Usizo Luphuthumayo")
    val caregiverLinking: String get() = t("Caregiver Linking", "Versorger-koppeling", "Ukuxhumanisa Umphakeli Wokunakekela")
    val settings: String get() = t("Settings", "Instellings", "Izilungiselelo")
    val voice: String get() = t("Voice", "Stem", "Izwi")
    val testingChecklist: String get() = t("Testing Checklist", "Toets-kontrolelys", "Uhlu Lokuhlola")
    val feedback: String get() = t("Feedback", "Terugvoer", "Impendulo")
    val releaseNotes: String get() = t("Release Notes", "Vrystellingsnotas", "Amanothi Okukhishwa")
    val developerTools: String get() = t("Developer Tools", "Developer Tools", "Developer Tools")
    val aboutLisa: String get() = t("About LISA", "Oor LISA", "Mayelana ne-LISA")
    val repeatLastPhrase: String get() = t("Repeat last phrase", "Herhaal laaste frase", "Phinda ibinzana lokugcina")

    // Emergency speech
    val emergencySpeechPhrase: String get() = t(
        "Emergency. I need help.",
        "Noodgeval. Ek het hulp nodig.",
        "Usizo oluphuthumayo. Ngidinga usizo."
    )

    /**
     * Visible confirmation prompt shown the instant Emergency is armed (L6 R0 detected) so the
     * two-step arm → confirm safety flow is never a silent, spoken-only state — real device
     * testing showed the arming gesture appearing to "do nothing" when the only feedback was
     * narration. Blink left-then-right to confirm and actually start the alarm, or blink
     * right-then-left (or simply wait) to cancel.
     */
    val guidedEmergencyAwaitingConfirmMessage: String get() = t(
        "Emergency armed. Blink left then right to confirm, or right then left to cancel.",
        "Noodgeval gereed. Flikker links dan regs om te bevestig, of regs dan links om te kanselleer.",
        "Usizo oluphuthumayo lulungile. Cwayiza kwesokunxele bese kwesokudla ukuqinisekisa, noma kwesokudla bese kwesokunxele ukukhansela."
    )

    fun menuLabel(panel: LisaPanel): String = when (panel) {
        LisaPanel.MyCommunication -> myCommunication
        LisaPanel.CommunicationSetup -> communicationSetup
        LisaPanel.VocabularyTraining -> vocabularyTraining
        LisaPanel.EmergencySetup -> emergencySetup
        LisaPanel.CaregiverLinking -> caregiverLinking
        LisaPanel.Voice -> voice
        LisaPanel.Settings -> settings
        LisaPanel.TestingChecklist -> testingChecklist
        LisaPanel.Feedback -> feedback
        LisaPanel.ReleaseNotes -> releaseNotes
        LisaPanel.DeveloperTools -> developerTools
        LisaPanel.AboutLisa -> aboutLisa
        else -> ""
    }

    val leftLabel: String get() = t("Left", "Links", "Kwesokunxele")
    val rightLabel: String get() = t("Right", "Regs", "Kwesokudla")
    val leftDots: (Int) -> String get() = { c -> t("Left: ${winkDots(c)}", "Links: ${winkDots(c)}", "Kwesokunxele: ${winkDots(c)}") }
    val rightDots: (Int) -> String get() = { c -> t("Right: ${winkDots(c)}", "Regs: ${winkDots(c)}", "Kwesokudla: ${winkDots(c)}") }

    // Vocabulary / training
    val coreVocabulary: (Int) -> String get() = { n -> t("Core vocabulary ($n)", "Kernwoordeskat ($n)", "Ulimi oluyisisekelo ($n)") }
    val customPhrases: String get() = t("Custom phrases", "Pasgemaakte frases", "Ibinzana ezenziwe ngokwezifiso")
    val addCustomSequence: String get() = t("Add a custom sequence", "Voeg pasgemaakte reeks by", "Engeza uchungechunge olwenziwe ngokwezifiso")
    val minSequenceNote: String get() = t(
        "Single winks and natural blinks are ignored.",
        "Enkelknippe en natuurlike knippe word ignoreer.",
        "Ukucwayiza okukodwa nokucwebha okujwayelekile akunakwa."
    )
    val countdownNote: String get() = t(
        "Minimum sequence: $MIN_SEQUENCE_WINKS winks. After detection, a countdown lets you cancel or speak.",
        "Minimum reeks: $MIN_SEQUENCE_WINKS knippe. Na opsporing laat 'n aftelling jou kanselleer of praat.",
        "Ubuncane bochungechunge: ama-wink angu-$MIN_SEQUENCE_WINKS. Ngemuva kokutholakala, ukubala kwezinyathelo kukuvumela ukukhansela noma ukukhuluma."
    )
    fun phraseEnglishSubtitle(vocabularyId: String): String? {
        if (language == PreferredLanguage.English) return null
        if (LisaCoreVocabulary.text(vocabularyId, PreferredLanguage.English).isBlank()) return null
        return LisaCoreVocabulary.text(vocabularyId, PreferredLanguage.English)
    }

    // Onboarding
    val stepOf: (Int, Int) -> String get() = { s, t -> t("Step $s of $t", "Stap $s van $t", "Isinyathelo $s kwangu-$t") }
    val welcomeToLisa: String get() = t("Welcome to LISA", "Welkom by LISA", "Siyakwamukela ku-LISA")
    val onboardingWelcomeBody: String get() = t(
        "LISA — Locked In Syndrome App — helps you communicate using intentional eye movements. This setup takes about one minute.",
        "LISA — Locked In Syndrome App — help jou om met doelbewuste oogbewegings te kommunikeer. Hierdie opstelling neem ongeveer een minute.",
        "I-LISA — Locked In Syndrome App — ikusiza ukuthi uxhumane ngokunyakazisa kwamehlo okuhloswe ngakho. Lokhu kuthatha cishe umzuzu owodwa."
    )
    val whatLisaDoes: String get() = t("What LISA does", "Wat LISA doen", "Okwenziwa yi-LISA")
    val onboardingWhatBody: String get() = t(
        "LISA uses your front camera to detect deliberate wink sequences, matches them to phrases, asks you to confirm, and speaks your message aloud. Emergency sequences can trigger a local alarm.",
        "LISA gebruik jou voorste kamera om doelbewuste knip-reekse te detecteer, pas dit by frases, vra jou om te bevestig, en praat jou boodskap hardop. Noodreekse kan 'n plaaslike alarm aktiveer.",
        "I-LISA isebenzisa ikhamera yangaphambili ukuthola uchungechunge lwama-wink oluhloswe ngakho, lufanise nezisho, likucela ukuqinisekise, bese likhuluma umlayezo wakho ngokuzwakalayo. Uchungechunge lousizo oluphuthumayo lungavula i-alamu yendawo."
    )
    val cameraPermissionTitle: String get() = t("Camera permission", "Kamera-toestemming", "Imvume yekhamera")
    val onboardingCameraBody: String get() = t(
        "LISA needs camera access to see your face and detect intentional winks. Video is processed on your device only — nothing is uploaded during this testing build.",
        "LISA benodig kameratoegang om jou gesig te sien en doelbewuste knippe te detecteer. Video word slegs op jou toestel verwerk — niks word opgelaai tydens hierdie toetsweergawe nie.",
        "I-LISA idinga ukufinyelela ikhamera ukubona ubuso bakho nokuthola ama-wink ahleliwe ngamabomu. Ividiyo icutshungulwa kuphela kudivayisi yakho — akukho okulayishwayo kulolu suku lokuhlola."
    )
    val allowCameraAccess: String get() = t("Allow camera access", "Laat kameratoegang toe", "Vumela ukufinyelela ikhamera")
    val onboardingSafetyTitle: String get() = t("Safety notice", "Veiligheidskennisgewing", "Isaziso sokuphepha")
    val primaryUserProfile: String get() = t("Primary User profile", "Primêre Gebruiker-profiel", "Iphrofayela yomsebenzisi oyinhloko")
    val confirmProfileName: String get() = t(
        "Confirm the name for the main communication profile on this device.",
        "Bevestig die naam vir die hoof kommunikasieprofiel op hierdie toestel.",
        "Qinisekisa igama lephrofayela yokuxhumana eyinhloko kule divayisi."
    )
    val profileName: String get() = t("Profile name", "Profielnaam", "Igama lephrofayela")
    val startLisaTitle: String get() = t("Start LISA", "Begin LISA", "Qala i-LISA")
    val onboardingStartBody: String get() = t(
        "You're ready for local testing. Use Menu → Testing Checklist to verify winks, speech, and emergency before real-world use.",
        "Jy is gereed vir plaaslike toetsing. Gebruik Kieslys → Toets-kontrolelys om knippe, spraak en nood te verifieer voor regte-wêreld gebruik.",
        "Usulungele ukuhlola endaweni. Sebenzisa Imenyu → Uhlu Lokuhlola ukuze uqinisekise ama-wink, inkulumo, nousizo oluphuthumayo ngaphambi kokusetshenziswa kwangempela."
    )
    val primaryUserLabel: (String) -> String get() = { name -> t("Primary User: $name", "Primêre Gebruiker: $name", "Umsebenzisi oyinhloko: $name") }

    // Camera permission screen
    val cameraAccessNeeded: String get() = t("Camera access needed", "Kameratoegang benodig", "Kudingeka ukufinyelela ikhamera")
    val cameraPermissionExplain: String get() = t(
        "LISA uses the front camera to detect your face and intentional wink sequences. Without camera access, LISA cannot listen for your messages.",
        "LISA gebruik die voorste kamera om jou gesig en doelbewuste knip-reekse te detecteer. Sonder kameratoegang kan LISA nie na jou boodskappe luister nie.",
        "I-LISA isebenzisa ikhamera yangaphambili ukuthola ubuso bakho nochungechunge lwama-wink oluhloswe ngamabomu. Ngaphandle kwemvume yekhamera, i-LISA ayikwazi ukulalela imilayezo yakho."
    )
    val cameraOnDeviceOnly: String get() = t(
        "Processing stays on this device. No video is uploaded in this testing build.",
        "Verwerking bly op hierdie toestel. Geen video word opgelaai in hierdie toetsweergawe nie.",
        "Ukuhlaziya kuhlala kule divayisi. Ayikho ividiyo elayishwayo kulolu suku lokuhlola."
    )
    val grantCameraPermission: String get() = t("Grant camera permission", "Gee kameratoestemming", "Nikeza imvume yekhamera")
    val openSettings: String get() = t("Open Settings", "Maak Instellings oop", "Vula Izilungiselelo")
    val cameraDeniedSettingsHint: String get() = t(
        "Camera permission was denied. Open Settings → LISA → Permissions to enable the camera.",
        "Kameratoestemming is geweier. Maak Instellings → LISA → Toestemmings oop om die kamera te aktiveer.",
        "Imvume yekhamera yenqatshwe. Vula Izilungiselelo → LISA → Izimvume ukuze unike amandla ikhamera."
    )

    // Voice platform
    val voiceHomeSubtitle: String get() = t(
        "Choose how LISA speaks.",
        "Kies hoe LISA praat.",
        "Khetha ukuthi i-LISA ikhuluma kanjani."
    )
    val deviceVoiceTitle: String get() = t("Device Voice", "Toestelstem", "Izwi Ledivayisi")
    val deviceVoiceDescription: String get() = t(
        "Uses your phone's built-in offline speech. Always free.",
        "Gebruik jou foon se ingeboude vanlyn spraak. Altyd gratis.",
        "Isebenzisa inkulumo engaxhunyiwe eyakhelwe efonini yakho. Ihlala imahhala."
    )
    val premiumVoicesTitle: String get() = t("Premium Voices", "Premium-stemme", "Amazwi e-Premium")
    val premiumVoicesHomeDescription: String get() = t(
        "Studio-quality South African voices.",
        "Ateljee-gehalte Suid-Afrikaanse stemme.",
        "Amazwi e-South Africa asekwehlukile."
    )
    val myVoiceTitle: String get() = t("My Voice", "My Stem", "Izwi Lami")
    val myVoiceHomeDescription: String get() = t(
        "Use your own natural voice. Record your voice — LISA will eventually recreate it using AI.",
        "Gebruik jou eie natuurlike stem. Neem jou stem op — LISA sal dit uiteindelik met AI herskep.",
        "Sebenzisa izwi lakho elijwayelekile. Rekhoda izwi lakho — i-LISA izolisebenzisa i-AI ngokuhamba kwesikhathi."
    )
    val familyVoiceTitle: String get() = t("Family Voice", "Familiestem", "Izwi Lomndeni")
    val familyVoiceHomeDescription: String get() = t(
        "Allow a trusted family member to donate their voice. Perfect for maintaining familiar communication.",
        "Laat 'n vertroude familielid toe om hul stem te skenk. Perfek vir vertroude kommunikasie.",
        "Vumela ilunga lomndeni elithembekile ukunikeza izwi lalo. Kuhle ukugcina ukuxhumana okujwayelekile."
    )
    val manage: String get() = t("Manage", "Bestuur", "Phatha")
    val comingSoon: String get() = t("Coming Soon", "Binnekort", "Kuyeza Maduze")
    val statusActive: String get() = t("Active", "Aktief", "Iyasebenza")
    val statusInstalled: String get() = t("Installed", "Geïnstalleer", "Ifakiwe")
    val statusUnavailable: String get() = t("Unavailable", "Nie beskikbaar nie", "Ayitholakali")
    val naturalEnglish: String get() = t("Natural English", "Natuurlike Engels", "IsiNgisi Esijwayelekile")
    val naturalAfrikaans: String get() = t("Natural Afrikaans", "Natuurlike Afrikaans", "IsiBhunu Esijwayelekile")
    val naturalIsiZulu: String get() = t("Natural isiZulu", "Natuurlike isiZulu", "IsiZulu Esijwayelekile")
    val southAfricanFemale: String get() = t("South African Female", "Suid-Afrikaanse Vrou", "Owesifazane waseNingizimu Afrika")
    val southAfricanMale: String get() = t("South African Male", "Suid-Afrikaanse Man", "Owesilisa waseNingizimu Afrika")
    val moreVoicesComing: String get() = t("More voices coming…", "Meer stemme kom…", "Amanye amazwi ayeza…")
    val premium: String get() = t("Premium", "Premium", "Premium")
    val female: String get() = t("Female", "Vrou", "Owesifazane")
    val male: String get() = t("Male", "Man", "Owesilisa")
    val preview: String get() = t("Preview", "Voorskou", "Buka kuqala")
    val benefits: String get() = t("Benefits", "Voordele", "Izinzuzo")
    val availableSoon: String get() = t("Available soon", "Binnekort beskikbaar", "Kuyeza maduze")
    val premiumVoicesIntro: String get() = t(
        "Premium Voices provide more natural speech than standard phone voices.",
        "Premium-stemme bied meer natuurlike spraak as standaard foonstemme.",
        "Amazwi e-Premium anikeza inkulumo ejwayelekile kunamazwi afoni ajwayelekile."
    )
    val premiumBenefitPronunciation: String get() = t("Better pronunciation", "Beter uitspraak", "Ukukhuluma okungcono")
    val premiumBenefitAccent: String get() = t("South African accents", "Suid-Afrikaanse aksente", "Amadla e-South Africa")
    val premiumBenefitExpressive: String get() = t("More expressive speech", "Meer uitdrukkingryke spraak", "Inkulumo enobuchule okwengeziwe")
    val premiumBenefitFast: String get() = t("Faster speech generation", "Vinniger spraakgenerering", "Ukukhiqiza inkulumo okusheshayo")
    val premiumBenefitMoreVoices: String get() = t("More voices", "Meer stemme", "Amazwi amaningi")
    val myVoiceIntro: String get() = t(
        "In a future version you will be able to create your own AI voice.",
        "In 'n toekomstige weergawe sal jy jou eie AI-stem kan skep.",
        "Enguqulweni elizayo uzokwazi ukudala izwi lakho le-AI."
    )
    val myVoiceStepRecord: String get() = t("Record voice", "Neem stem op", "Rekhoda izwi")
    val myVoiceStepLearn: String get() = t("AI learns your voice", "AI leer jou stem", "I-AI ifunda izwi lakho")
    val myVoiceStepSpeak: String get() = t("LISA speaks using your voice", "LISA praat met jou stem", "I-LISA ikhuluma ngezwi lakho")
    val familyVoiceIntro: String get() = t(
        "A trusted family member will be able to donate their voice. This allows LISA to communicate using a familiar voice.",
        "'n Vertroude familielid sal hul stem kan skenk. Dit laat LISA toe om met 'n vertroude stem te kommunikeer.",
        "Ilunga lomndeni elithembekile lizokwazi ukunikeza izwi lalo. Lokhu kuvumela i-LISA ukuxhumana ngezwi elijwayelekile."
    )
    val familyVoiceStepRecord: String get() = t(
        "Family member records voice",
        "Familielid neem stem op",
        "Ilunga lomndeni lirekhoda izwi"
    )
    val familyVoiceStepProcess: String get() = t(
        "Voice processed securely",
        "Stem word veilig verwerk",
        "Izwi licutshungulwa ngokuphepha"
    )
    val familyVoiceStepSpeak: String get() = t(
        "LISA speaks using that voice",
        "LISA praat met daardie stem",
        "I-LISA ikhuluma ngalelo zwi"
    )
    val currentLanguage: String get() = t("Current language", "Huidige taal", "Ulimi lwamanje")
    val currentTtsEngine: String get() = t("Current TTS engine", "Huidige TTS-enjin", "Injini ye-TTS yamanje")
    val currentVoice: String get() = t("Current voice", "Huidige stem", "Izwi lamanje")
    val voiceAvailability: String get() = t("Voice availability", "Stembeskikbaarheid", "Ukutholakala kwezwi")
    val installedVoices: String get() = t("Installed voices", "Geïnstalleerde stemme", "Amazwi afakiwe")
    val voiceNotSelected: String get() = t("Not selected", "Nie gekies nie", "Akukhethiwe")

    // Device voice / TTS
    val activeLanguage: String get() = t("Active language", "Aktiewe taal", "Ulimi olusebenzayo")
    val voiceStatus: String get() = t("Voice status", "Stemstatus", "Isimo sezwi")
    val voiceAvailable: String get() = t("Available", "Beskikbaar", "Iyatholakala")
    fun voiceAvailableFor(localeName: String): String = t(
        "Available ($localeName)",
        "Beskikbaar ($localeName)",
        "Iyatholakala ($localeName)"
    )
    val voiceMissingData: String get() = t(
        "Voice data missing — install language pack",
        "Stemdata ontbreek — installeer taalpakket",
        "Idatha yezwi ayikho — faka iphekhi yolimi"
    )
    val voiceNotSupported: String get() = t(
        "Not supported on this device",
        "Word nie op hierdie toestel ondersteun nie",
        "Ayisekelwe kule divayisi"
    )
    val voiceInitializing: String get() = t(
        "Initializing speech engine…",
        "Initialiseer spraak-enjin…",
        "Kulungiselela injini yokukhuluma…"
    )
    val availableVoices: String get() = t("Available voices", "Beskikbare stemme", "Amazwi atholakalayo")
    val noMatchingVoices: String get() = t(
        "No matching voices found for this language.",
        "Geen ooreenstemmende stemme vir hierdie taal gevind nie.",
        "Awukho amazwi afanayo alolu limi."
    )
    val testVoice: String get() = t("Test voice", "Toets stem", "Hlola izwi")
    val installVoiceData: String get() = t("Install voice data", "Installeer stemdata", "Faka idatha yezwi")
    val openTtsSettings: String get() = t("Open Text-to-Speech Settings", "Maak teks-na-spraak-instellings oop", "Vula izilungiselelo ze-Text-to-Speech")
    val poorLocalVoiceWarning: String get() = t(
        "This phone does not have a good local voice for this language yet. Install voice data or choose another TTS engine.",
        "Hierdie foon het nog nie 'n goeie plaaslike stem vir hierdie taal nie. Installeer stemdata of kies 'n ander TTS-enjin.",
        "Le foni alinayo izwi elihle lendawo lolu limi okwamanje. Faka idatha yezwi noma khetha enjini ye-TTS ehlukile."
    )
    val voiceSettingsSavedHint: String get() = t(
        "Voice choice is saved to the active communication profile.",
        "Stemkeuse word op die aktiewe kommunikasieprofiel gestoor.",
        "Ukukhetha kwezwi kugcinwa kuphrofayela yokuxhumana esebenzayo."
    )

    companion object {
        fun forLanguage(language: PreferredLanguage): LisaUiStrings = LisaUiStrings(language)

        fun ttsLocale(language: PreferredLanguage): Locale = when (language) {
            PreferredLanguage.English -> Locale.forLanguageTag("en-ZA")
            PreferredLanguage.Afrikaans -> Locale.forLanguageTag("af-ZA")
            PreferredLanguage.IsiZulu -> Locale.forLanguageTag("zu-ZA")
        }
    }
}
