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
    val reset: String get() = t("Clear", "Maak skoon", "Sula")
    val repeat: String get() = t("Repeat", "Herhaal", "Phinda")
    val back: String get() = t("Back", "Terug", "Emuva")
    val save: String get() = t("Save", "Stoor", "Londoloza")
    val next: String get() = t("Next", "Volgende", "Okulandelayo")
    val startLisa: String get() = t("Start LISA", "Begin LISA", "Qala i-LISA")
    val add: String get() = t("Add", "Voeg by", "Engeza")
    val cancel: String get() = t("Cancel", "Kanselleer", "Khansela")

    // Communication display
    val ready: String get() = t("READY", "GEREE", "SILINDILE")
    val eyeTrackingStatusWatching: String get() = t(
        "WATCHING YOUR EYES...",
        "KYK NA JOU OË...",
        "IBHEKA AMEHLO AKHO..."
    )
    val eyeTrackingStatusNoFace: String get() = t(
        "NO FACE DETECTED",
        "GEEN GESIG BESPEUR NIE",
        "AKUKHO UBUSO OBUTHOLAKALAYO"
    )
    val eyeTrackingStatusLookAtCamera: String get() = t(
        "PLEASE LOOK AT THE CAMERA",
        "KYK ASSEBLIEF NA DIE KAMERA",
        "SICELA UBHEKE IKHAMERA"
    )
    val eyeTrackingStatusCalibrating: String get() = t(
        "CALIBRATING EYE TRACKING...",
        "KALIBREER OOGNASPOOR...",
        "KULUNGISELANA UKULANDELELA AMEHLO..."
    )
    val eyeTrackingStatusTrackingLost: String get() = t(
        "TRACKING LOST",
        "NASPOOR VERLORE",
        "UKULANDELELA KULAHLEKILE"
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
    val workspaceCommunicationTitle: String get() = t("Communication", "Kommunikasie", "Ukuxhumana")

    fun guidedCategoryTitle(category: GuidedVocabularyCategory): String = when (category) {
        GuidedVocabularyCategory.Conversation -> t("General Conversation", "Algemene Gesprek", "Ingxoxo Jikelele")
        GuidedVocabularyCategory.BasicNeeds -> t("Basic Needs", "Basiese Behoeftes", "Izidingo Eziyisisekelo")
        GuidedVocabularyCategory.Medical -> t("Medical", "Medies", "Ezesimpilo")
        GuidedVocabularyCategory.Family -> t("Family", "Familie", "Umndeni")
        GuidedVocabularyCategory.Custom -> t("Custom", "Pasgemaak", "Ngokwezifiso")
        GuidedVocabularyCategory.BasicSystemControls -> t("Basic System Controls", "Basiese Stelselkontroles", "Izilawuli Zesistimu Eziyisisekelo")
        GuidedVocabularyCategory.Preferences -> t("Preferences", "Voorkeure", "Okuncanyelwayo")
    }

    val guidedCustomEmptyTitle: String get() = t(
        "No custom phrases yet.",
        "Nog geen pasgemaakte frases nie.",
        "Awekho amabinzana ngokwezifiso okwamanje."
    )
    val guidedCustomEmptyBody: String get() = t(
        "Open Custom from Categories to create a phrase using your eyes.",
        "Open Pasgemaak vanaf Kategorieë om 'n frase met jou oë te skep.",
        "Vula Ngokwezifiso kusuka Ezigabeni ukuze udale ibinzana usebenzisa amehlo akho."
    )

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

    fun composerSensitivityLine(sensitivityLevel: Int): String =
        t(
            "Sensitivity: $sensitivityLevel",
            "Sensitiwiteit: $sensitivityLevel",
            "Ukuzwela: $sensitivityLevel"
        )

    fun composerResponseTimeLine(responseTimeSec: Int): String =
        t(
            "Response time: ${responseTimeSec}s",
            "Reaksietyd: ${responseTimeSec}s",
            "Isikhathi sokuphendula: ${responseTimeSec}s"
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
        "create_phrase" -> t("Create phrase", "Skep frase", "Dala ibinzana")
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
    val emergencyActiveTitle: String get() = t(
        "EMERGENCY ACTIVE",
        "NOOD AKTIEF",
        "USIZO OLUPHUTHUMAYO LUYASEBENZA"
    )
    val emergencyAlarmActiveMessage: String get() = t(
        "LISA is sounding the emergency alarm.",
        "LISA laat die noodalarm klink.",
        "I-LISA ikhalisa i-alamu yosizo oluphuthumayo."
    )
    val cancelEmergency: String get() = t(
        "Cancel Emergency",
        "Kanselleer Nood",
        "Khansela Usizo Oluphuthumayo"
    )
    val stopEmergency: String get() = t(
        "Stop Emergency",
        "Stop Nood",
        "Misa Usizo Oluphuthumayo"
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
    val communicationSetupIntro: String get() = communicationTimingPurpose
    val settingsSavedToProfileHint: String get() = settingsSavedNote

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

    // Response time (top-of-screen quick control — same style as Sensitivity)
    val responseTime: String get() = t("Response time", "Reaksietyd", "Isikhathi sokuphendula")
    val responseTimeDecrease: String get() = t("Response time -", "Reaksietyd -", "Isikhathi sokuphendula -")
    val responseTimeIncrease: String get() = t("Response time +", "Reaksietyd +", "Isikhathi sokuphendula +")

    // Menu items
    val myCommunication: String get() = t("Communication Profile", "Kommunikasieprofiel", "Iphrofayela Yokuxhumana")
    val communicationSetup: String get() = t("Communication Timing", "Kommunikasie-tydsberekening", "Isikhathi Sokuxhumana")
    val vocabularyTraining: String get() = t("Vocabulary", "Woordeskat", "Ulimi")
    val settings: String get() = t("Settings", "Instellings", "Izilungiselelo")
    val voice: String get() = t("Voice", "Stem", "Izwi")
    val testingChecklist: String get() = t("Device Checklist", "Toestel-kontrolelys", "Uhlu Lwedivayisi")
    val feedback: String get() = t("Feedback", "Terugvoer", "Impendulo")
    val releaseNotes: String get() = t("Release Notes", "Vrystellingsnotas", "Amanothi Okukhishwa")
    val developerTools: String get() = t("Developer Tools", "Ontwikkelaarhulpmiddels", "Amathuluzi Othuthukisi")
    val aboutLisa: String get() = t("About LISA", "Oor LISA", "Mayelana ne-LISA")
    val privacyPolicy: String get() = t("Privacy Policy", "Privaatheidsbeleid", "Inqubomgomo Yobumfihlo")
    // Menu sections (RC1)
    val menuSectionCommunication: String get() = t("Communication", "Kommunikasie", "Ukuxhumana")
    val menuSectionApplication: String get() = t("Application", "Program", "Uhlelo")
    val menuSectionSupport: String get() = t("Support", "Ondersteuning", "Ukusekela")

    // RC2 — page purpose lines (one short sentence each)
    val communicationProfilePurpose: String get() = t(
        "Manage your communication preferences.",
        "Bestuur jou kommunikasievoorkeure.",
        "Phatha okuthandayo kwakho kokuxhumana."
    )
    val vocabularyPurpose: String get() = t(
        "Manage your communication phrases.",
        "Bestuur jou kommunikasiefrases.",
        "Phatha amabinzana akho okuxhumana."
    )
    val createPhraseCardTitle: String get() = t("Create a Phrase", "Skep 'n frase", "Dala ibinzana")
    val createPhraseCardDescription: String get() = t(
        "Create your own phrases for Communication.",
        "Skep jou eie frases vir Kommunikasie.",
        "Dala amabinzana akho okuxhumana."
    )
    val createPhraseButton: String get() = t("Create Phrase", "Skep frase", "Dala ibinzana")
    val vocabularyBuiltinNote: String get() = t(
        "Built-in communication phrases are already available on the Communication screen.",
        "Ingeboude kommunikasiefrases is reeds beskikbaar op die Kommunikasie-skerm.",
        "Amabinzana okuxhumana afakiwe asevele atholakala esikrinini Sokuxhumana."
    )
    val vocabularyCustomComposerNote: String get() = t(
        "Open Custom from Categories to create a phrase using your eyes.",
        "Open Pasgemaak vanaf Kategorieë om 'n frase met jou oë te skep.",
        "Vula Ngokwezifiso kusuka Ezigabeni ukuze udale ibinzana usebenzisa amehlo akho."
    )
    val createPhraseTitle: String get() = t("Create Phrase", "Skep frase", "Dala ibinzana")
    val createPhrasePurpose: String get() = t(
        "Add personalised communication phrases.",
        "Voeg persoonlike kommunikasiefrases by.",
        "Engeza amabinzana okuxhumana asikisisele."
    )
    val createPhraseIntroLead: String get() = t(
        "LISA allows personalised communication phrases to be added alongside the built-in vocabulary.",
        "LISA laat persoonlike kommunikasiefrases toe om by die ingeboude woordeskat gevoeg te word.",
        "I-LISA ivumela amabinzana okuxhumana asikisisele engezwe kuhlangene nolimi olufakiwe."
    )
    val createPhraseIntroDetail: String get() = t(
        "These phrases can be customised to match the primary user's daily communication needs.",
        "Hierdie frases kan aangepas word om by die primêre gebruiker se daaglikse kommunikasiebehoeftes te pas.",
        "Lawa mabinana angenziwa ngokwezifiso ukuze afanele izidingo zansuku zonke zomsebenzisi oyinhloko."
    )
    val createPhraseAudienceNote: String get() = t(
        "Caregivers and others helping with setup can add phrases from this screen.",
        "Versorgers en ander wat met opstelling help, kan frases vanaf hierdie skerm byvoeg.",
        "Abanakekeli nabanye abasiza ngokusetha bangengeza amabinzana kusuka kulesi sikrini."
    )
    val createPhraseHowItWorksTitle: String get() = t("How it works", "Hoe dit werk", "Indlela esebenza ngayo")
    val createPhraseStep1Label: String get() = t("Step 1", "Stap 1", "Isinyathelo 1")
    val createPhraseStep1Body: String get() = t(
        "Choose a communication category.",
        "Kies 'n kommunikasiekategorie.",
        "Khetha isigaba sokuxhumana."
    )
    val createPhraseStep1Examples: String get() = t(
        "Examples: General Conversation, Basic Needs, Medical, Family, Custom",
        "Voorbeelde: Algemene Gesprek, Basiese Behoeftes, Medies, Familie, Pasgemaak",
        "Izibonelo: Ingxoxo Jikelele, Izidingo Eziyisisekelo, Ezesimpilo, Umndeni, Ngokwezifiso"
    )
    val createPhraseStep2Label: String get() = t("Step 2", "Stap 2", "Isinyathelo 2")
    val createPhraseStep2Body: String get() = t(
        "Build the phrase using eye-controlled letters and actions.",
        "Bou die frase met oogbeheer letters en aksies.",
        "Yakha ibinzana usebenzisa izinhlamvu nezenzo ezilawulwa ngamehlo."
    )
    val createPhraseStep3Label: String get() = t("Step 3", "Stap 3", "Isinyathelo 3")
    val createPhraseStep3Body: String get() = t(
        "LISA will automatically assign a safe blink sequence and add the phrase to Communication.",
        "LISA sal outomaties 'n veilige knip-reeks toewys en die frase by Kommunikasie voeg.",
        "I-LISA izonikeza ngokuzenzakalelayo uchungechunge lwe-wink oluphephile bese yengeza ibinzana ekuxhumaneni."
    )
    val createPhraseBeginButton: String get() = t("Begin", "Begin", "Qala")
    val phraseEditorTitle: String get() = t("Phrase Editor", "Fraseredigeerder", "Umhleli Webinzana")
    val phraseEditorPurpose: String get() = t(
        "Create a personalised communication phrase.",
        "Skep 'n persoonlike kommunikasiefrase.",
        "Dala ibinzana lokuxhumana elenziwe ngokwezifiso."
    )
    val phraseEditorCategoryLabel: String get() = t("Category", "Kategorie", "Isigaba")
    val phraseEditorPhraseLabel: String get() = t("Phrase", "Frase", "Ibinzana")
    val phraseEditorPreviewLabel: String get() = t("Preview", "Voorskou", "Ukubuka kuqala")
    val phraseEditorPreviewVoice: String get() = t("Preview Voice", "Voorskou stem", "Buka kuqala izwi")
    val phraseEditorSave: String get() = t("Save", "Stoor", "Londoloza")
    val phraseEditorCreateAnother: String get() = t("Create another phrase", "Skep nog 'n frase", "Dala elinye ibinzana")
    val phraseEditorReturnToCommunication: String get() = t("Return to Communication", "Keer terug na Kommunikasie", "Buyela ekuxhumaneni")
    val phraseCreatedSuccess: String get() = t("Phrase created successfully.", "Frase suksesvol geskep.", "Ibinzana lidalwe ngempumelelo.")
    fun phraseCreatedCategoryLine(categoryLabel: String): String =
        t("Category: $categoryLabel", "Kategorie: $categoryLabel", "Isigaba: $categoryLabel")
    fun phraseCreatedSequenceLine(sequenceLabel: String): String =
        t("Blink sequence: $sequenceLabel", "Knipreeks: $sequenceLabel", "Uchungechunge lokucwayiza: $sequenceLabel")
    val phraseComposerTitle: String get() = t("Create Phrase", "Skep frase", "Dala ibinzana")
    val phraseComposerDestinationStepTitle: String get() = t(
        "Where should this phrase appear?",
        "Waar moet hierdie frase verskyn?",
        "Leli binzana kufanele livuke kuphi?"
    )
    val phraseComposerDestinationStepBody: String get() = t(
        "Choose the Communication category for this phrase.",
        "Kies die Kommunikasie-kategorie vir hierdie frase.",
        "Khetha isigaba sokuxhumana saleli binzana."
    )
    val phraseComposerKeyboardTitle: String get() = t("Eye-Controlled Keyboard", "Oogbeheerde sleutelbord", "Ikhibhodi elawulwa yiso")
    val phraseComposerKeyboardSpaceLabel: String get() = t("SPACE", "SPASIE", "ISIKHALA")
    val phraseComposerKeyboardShiftLabel: String get() = t("Shift", "Shift", "Shift")
    val phraseComposerKeyboardBackspaceLabel: String get() = t("Backspace", "Backspace", "Susa")
    val phraseComposerPanelMoveUp: String get() = t("Move Up", "Beweeg op", "Hambisa phezulu")
    val phraseComposerPanelMoveDown: String get() = t("Move Down", "Beweeg af", "Hambisa phansi")
    val phraseComposerPanelMoveLeft: String get() = t("Move Left", "Beweeg links", "Hambisa kwesokunxele")
    val phraseComposerPanelMoveRight: String get() = t("Move Right", "Beweeg regs", "Hambisa kwesokudla")
    val phraseComposerPanelSelectKey: String get() = t("Select Key", "Kies sleutel", "Khetha ukhiye")
    val phraseComposerPanelBackspace: String get() = t("Backspace", "Backspace", "Susa")
    val phraseComposerPanelPreview: String get() = t("Preview Phrase", "Voorskou frase", "Buka kuqala ibinzana")
    val phraseComposerPanelSave: String get() = t("Save Phrase", "Stoor frase", "Londoloza ibinzana")
    val phraseComposerPanelBack: String get() = t("Back", "Terug", "Emuva")
    val phraseComposerPanelShowNumbers: String get() = t("123", "123", "123")
    val phraseComposerPanelShowLetters: String get() = t("ABC", "ABC", "ABC")
    val phraseComposerPartialSequenceLabel: String get() = t("Sequence", "Volgorde", "Uchungechunge")
    val phraseComposerSaveConfirmTitle: String get() = t("Save this phrase?", "Stoor hierdie frase?", "Londoloza leli binzana?")
    val phraseComposerSaveConfirmBody: String get() = t("Save this phrase?", "Stoor hierdie frase?", "Londoloza leli binzana?")
    val phraseComposerSaveConfirmPhraseLabel: String get() = t("Phrase", "Frase", "Ibinzana")
    val phraseComposerSaveConfirmSequenceLabel: String get() = t("Assigned blink sequence", "Toegewysde knippervolgorde", "Ukulandelana kokunyonya okunikeziwe")
    val phraseComposerConfirmSave: String get() = t("Confirm Save", "Bevestig Stoor", "Qinisekisa Ukulondoloza")
    val phraseComposerCancelSave: String get() = t("Cancel", "Kanselleer", "Khansela")
    val phraseComposerCategoryLabel: String get() = t("Category", "Kategorie", "Isigaba")
    val phraseComposerCurrentPhraseLabel: String get() = t("Current phrase", "Huidige frase", "Ibinzana lamanje")
    val phraseComposerSuccessTitle: String get() = t("Phrase saved", "Frase gestoor", "Ibinzana lilondoloziwe")
    val phraseComposerCancelConfirmTitle: String get() = t("Discard this phrase?", "Gooi hierdie frase weg?", "Lahla leli binzana?")
    val phraseComposerCancelConfirmBody: String get() = t(
        "Discard this phrase?",
        "Gooi hierdie frase weg?",
        "Lahla leli binzana?"
    )
    val phraseComposerConfirmCancel: String get() = t("Discard and exit", "Gooi weg en verlaat", "Lahla uphume")
    val phraseComposerKeepComposing: String get() = t("Continue composing", "Gaan voort met skryf", "Qhubeka nokubhala")
    val phraseComposerActionSpace: String get() = t("Space", "Spasie", "Isikhala")
    val phraseComposerActionDelete: String get() = t("Delete", "Skrap", "Susa")
    val phraseComposerActionClear: String get() = t("Clear", "Vee uit", "Sula")
    val phraseComposerActionPreview: String get() = t("Preview", "Voorskou", "Buka kuqala")
    val phraseComposerActionSave: String get() = t("Save", "Stoor", "Londoloza")
    val phraseComposerActionBack: String get() = t("Back", "Terug", "Emuva")
    val phraseComposerActionCancel: String get() = t("Cancel", "Kanselleer", "Khansela")
    val phraseValidationEmpty: String get() = t("Enter a phrase before saving.", "Tik 'n frase in voor jy stoor.", "Faka ibinzana ngaphambi kokulondoloza.")
    val phraseValidationTooLong: String get() = t(
        "Phrase is too long. Shorten it and try again.",
        "Frase is te lank. Verkort dit en probeer weer.",
        "Ibinzana liyinde kakhulu. Lifinye bese uzama futhi."
    )
    val phraseValidationDuplicate: String get() = t(
        "This phrase already exists. Try different wording.",
        "Hierdie frase bestaan reeds. Probeer ander bewoording.",
        "Leli binzana selive likhona. Zama amagama ahlukile."
    )
    val phraseDuplicateWarningTitle: String get() = t(
        "Phrase already exists",
        "Frase bestaan reeds",
        "Ibinzana selive likhona"
    )
    fun phraseDuplicateExistsMessage(match: DuplicatePhraseMatch): String {
        val categoryLabel = caregiverPhraseCategoryLabel(match.category)
        val phrase = match.phrase.uppercase()
        return t(
            "\"$phrase\" already exists in the $categoryLabel category.",
            "\"$phrase\" bestaan reeds in die $categoryLabel-kategorie.",
            "\"$phrase\" selive likhona esigabeni se-$categoryLabel."
        )
    }
    val phraseDuplicateHint: String get() = t(
        "Use the existing phrase instead of creating another copy.",
        "Gebruik die bestaande frase in plaas van nog 'n kopie te skep.",
        "Sebenzisa ibinzana elikhona esikhundleni sokwenza enye ikhophi."
    )
    fun phraseDuplicateOpenCategory(categoryLabel: String): String = t(
        "Open $categoryLabel",
        "Open $categoryLabel",
        "Vula i-$categoryLabel"
    )
    val phraseDuplicateContinueEditing: String get() = t(
        "Continue Editing",
        "Gaan voort met redigering",
        "Qhubeka nokuhlela"
    )
    val phraseValidationNoSequence: String get() = t(
        "No safe blink sequence is available right now. Remove an unused custom phrase and try again.",
        "Geen veilige knip-reeks is tans beskikbaar nie. Verwyder 'n ongebruikte pasgemaakte frase en probeer weer.",
        "Awukho uchungechunge lwe-wink oluphephile olutholakalayo manje. Susa ibinzana elingasetshenziswanga bese uzama futhi."
    )
    fun caregiverPhraseCategoryLabel(category: CustomPhraseEngine.CaregiverPhraseCategory): String = when (category) {
        CustomPhraseEngine.CaregiverPhraseCategory.Conversation -> t("General Conversation", "Algemene Gesprek", "Ingxoxo Jikelele")
        CustomPhraseEngine.CaregiverPhraseCategory.BasicNeeds -> t("Basic Needs", "Basiese Behoeftes", "Izidingo Eziyisisekelo")
        CustomPhraseEngine.CaregiverPhraseCategory.Medical -> t("Medical", "Medies", "Ezesimpilo")
        CustomPhraseEngine.CaregiverPhraseCategory.Family -> t("Family", "Familie", "Umndeni")
        CustomPhraseEngine.CaregiverPhraseCategory.Custom -> t("Custom", "Pasgemaak", "Ngokwezifiso")
    }
    val communicationTimingPurpose: String get() = t(
        "How long LISA waits after your last wink.",
        "Hoe lank LISA wag na jou laaste knip.",
        "Isikhathi i-LISA elinda ngemuva kwakho wokugcina ukucwayiza."
    )
    val settingsPurpose: String get() = t(
        "Adjust detection, communication, display, and learning.",
        "Pas opsporing, kommunikasie, vertoning en leer aan.",
        "Lungisa ukutholwa, ukuxhumana, ukuboniswa nokufunda."
    )
    val feedbackPurpose: String get() = t(
        "Share what worked and what was confusing.",
        "Deel wat gewerk het en wat verwarrend was.",
        "Yabelana ngokuthi yini eyasebenza nokuthi yini eyedidezelayo."
    )
    val deviceChecklistPurpose: String get() = t(
        "Confirm setup is working before daily use.",
        "Bevestig dat opstelling werk voor daaglikse gebruik.",
        "Qinisekisa ukuthi ukusetha kusebenza ngaphambi kokusetshenziswa kwansuku zonke."
    )
    val developerToolsPurpose: String get() = t(
        "Tools for setup and troubleshooting.",
        "Nutsgoed vir opstelling en probleemoplossing.",
        "Amathuluzi okusetha nokuxazulula izinkinga."
    )
    val releaseNotesPurpose: String get() = t(
        "What's included in this version.",
        "Wat in hierdie weergawe ingesluit is.",
        "Okufakiwe kule nguqulo."
    )
    val aboutLisaPurpose: String get() = lisaTagline
    val privacyPolicyPurpose: String get() = t(
        "How LISA handles your camera and personal information.",
        "Hoe LISA jou kamera en persoonlike inligting hanteer.",
        "Indlela i-LISA ejwayisa ngayo ikhamera yakho nolwazi lwakho lomuntu siqu."
    )

    // RC3 — About page
    val aboutWhatIsLisaTitle: String get() = t("What is LISA?", "Wat is LISA?", "Yini i-LISA?")
    val aboutWhatIsLisaBody: String get() = t(
        "LISA helps you communicate with intentional eye winks. Choose a phrase, confirm, and LISA speaks it aloud.",
        "LISA help jou om met doelbewuste oogknippe te kommunikeer. Kies 'n frase, bevestig, en LISA praat dit hardop.",
        "I-LISA ikusiza ukuxhumana ngama-wink ahleliwe ngamabomu. Khetha ibinzana, uqinisekise, bese i-LISA ikhuluma ngokuzwakala."
    )
    val aboutWhoIsLisaForTitle: String get() = t("Who is LISA for?", "Vir wie is LISA?", "I-LISA ingabanini?")
    val aboutWhoIsLisaForBody: String get() = t(
        "People who cannot speak reliably but can use intentional eye movements.",
        "Mense wat nie betroubaar kan praat nie, maar doelbewuste oogbewegings kan gebruik.",
        "Abantu abangakwazi ukukhuluma ngokuthembekile kodwa bangasebenzisa ukunyakaza kwamehlo okuhloswe ngakho."
    )
    val aboutHowLisaWorksTitle: String get() = t("How LISA works", "Hoe LISA werk", "Indlela i-LISA esebenza ngayo")
    val aboutHowLisaWorksBullets: List<String> get() = listOf(
        t("Camera detects your face and winks", "Kamera detecteer jou gesig en knippe", "Ikhamera ithola ubuso bakho nama-wink"),
        t("You blink a phrase sequence", "Jy knip 'n frase-reeks", "Ucwayiza uchungechunge lwebinzana"),
        t("You confirm, then LISA speaks", "Jy bevestig, dan praat LISA", "Uqinisekisa, bese i-LISA ikhuluma"),
        t("Emergency gestures can trigger an alarm", "Noodgebare kan 'n alarm aktiveer", "Izimpawu zosizo oluphuthumayo zingavula i-alamu")
    )
    val aboutPrivacySummaryTitle: String get() = t("Privacy", "Privaatheid", "Ubumfihlo")
    val aboutPrivacySummaryBullets: List<String> get() = listOf(
        t("Camera processing stays on your device", "Kamera-verwerking bly op jou toestel", "Ukucutshungulwa kwekhamera kuhlala kudivayisi yakho"),
        t("Profiles are stored locally", "Profiele word plaaslik gestoor", "Amaphrofayela agcinwa endaweni"),
        t("No cloud account required", "Geen wolkrekening benodig nie", "Akudingeki i-akhawunti yefu"),
        t("Read the full Privacy Policy in Menu", "Lees die volledige Privaatheidsbeleid in Kieslys", "Funda Inqubomgomo Yobumfihlo ephelele ku-Imenyu")
    )
    val aboutSafetyTitle: String get() = t("Safety", "Veiligheid", "Ukuphepha")
    val aboutSafetyBullets: List<String> get() = listOf(
        t("Assistive tool — not a certified medical device", "Hulpmiddel — nie 'n gesertifiseerde mediese toestel nie", "Ithuluzi losizo — hhayi idivayisi yezokwelapha eqinisekisiwe"),
        t("Practice emergency with a caregiver first", "Oefen noodgeval met 'n versorger eers", "Zilolonge usizo oluphuthumayo nomnakekeli kuqala"),
        t("Do not rely on LISA as your only emergency method yet", "Moenie nog op LISA as jou enigste noodmetode staatmaak nie", "Ungathembele ku-LISA njengendlela yakho yodwa yosizo oluphuthumayo okwamanje")
    )
    val aboutVersionTitle: String get() = t("Version", "Weergawe", "Inguqulo")
    val aboutCreatorTitle: String get() = t("Creator", "Skepper", "Umdali")
    val aboutCreatorBody: String get() = t(
        "Lungelo Richard Zungu · Asgard Dynamics",
        "Lungelo Richard Zungu · Asgard Dynamics",
        "Lungelo Richard Zungu · Asgard Dynamics"
    )
    val aboutCopyrightTitle: String get() = t("Copyright", "Kopiereg", "Ilungelo lokushicilela")
    val copyrightNotice: String get() = t(
        "© 2026 Asgard Dynamics. All rights reserved.",
        "© 2026 Asgard Dynamics. Alle regte voorbehou.",
        "© 2026 Asgard Dynamics. Wonke amalungelo agodliwe."
    )
    val aboutSupportTitle: String get() = t("Support", "Ondersteuning", "Ukusekela")
    val aboutSupportWebsite: String get() = t(
        "Website: details will be published at launch",
        "Webwerf: besonderhede sal by bekendstelling gepubliseer word",
        "Iwebhusayithi: imininingwane izokhishwa lapho kukhishwa"
    )
    val aboutSupportEmail: String get() = t(
        "Email: contact details will be published at launch",
        "E-pos: kontakbesonderhede sal by bekendstelling gepubliseer word",
        "I-imeyili: imininingwane yokuxhumana izokhishwa lapho kukhishwa"
    )
    fun versionAndBuildLabel(versionName: String, versionCode: Long): String = t(
        "Version $versionName · Build $versionCode",
        "Weergawe $versionName · Bou $versionCode",
        "Inguqulo $versionName · Ukwakhiwa $versionCode"
    )

    // RC3 — Privacy Policy
    val privacyIntroTitle: String get() = t("Your privacy matters", "Jou privaatheid is belangrik", "Ubumfihlo bakho bubalulekile")
    val privacyIntroBody: String get() = t(
        "LISA is built for assistive communication. We designed it so you stay in control of your information.",
        "LISA is gebou vir hulpkommunikasie. Ons het dit ontwerp sodat jy beheer oor jou inligting behou.",
        "I-LISA yakhelwe ukuxhumana okusizayo. Sayiklanyele ukuze uhlale ulawula ulwazi lwakho."
    )
    val privacyCameraTitle: String get() = t("Camera use", "Kameragebruik", "Ukusetshenziswa kwekhamera")
    val privacyCameraBody: String get() = t(
        "LISA uses your front camera only for eye tracking — to see your face and detect intentional winks. The camera is not used for anything else.",
        "LISA gebruik slegs jou voorste kamera vir oognasporing — om jou gesig te sien en doelbewuste knippe te detecteer. Die kamera word nie vir iets anders gebruik nie.",
        "I-LISA isebenzisa ikhamera yakho yangaphambili kuphela ukulandelela amehlo — ukubona ubuso bakho nokuthola ama-wink ahleliwe ngamabomu. Ikhamera ayisetshenziswa ngenye indlela."
    )
    val privacyOnDeviceTitle: String get() = t("On-device processing", "Verwerking op toestel", "Ukucutshungulwa kudivayisi")
    val privacyOnDeviceBody: String get() = t(
        "Video from your camera is processed on this device. LISA does not upload your camera video or store it for sharing online.",
        "Video van jou kamera word op hierdie toestel verwerk. LISA laai nie jou kameravideo op nie en stoor dit nie vir aanlyn deling nie.",
        "Ividiyo evela kwekhamera yakho icutshungulwa kule divayisi. I-LISA ayilayishi ngevidiyo yakho yekhamera futhi ayigcini ukuze yabelwe ku-inthanethi."
    )
    val privacyNoSellingTitle: String get() = t("We do not sell your data", "Ons verkoop nie jou data nie", "Asithengisi idatha yakho")
    val privacyNoSellingBody: String get() = t(
        "Eye movement data is used only to help LISA understand your winks. It is not sold or shared with advertisers.",
        "Oogbewegingdata word slegs gebruik om LISA te help jou knippe te verstaan. Dit word nie verkoop of met adverteerders gedeel nie.",
        "Idatha yokunyakaza kwamehlo isetshenziswa kuphela ukusiza i-LISA iqonde ama-wink akho. Ayithengiswa noma yabelwana ngayo nabathengisi."
    )
    val privacyYourInfoTitle: String get() = t("Your information", "Jou inligting", "Ulwazi lwakho")
    val privacyYourInfoBody: String get() = t(
        "Communication profiles, vocabulary, and feedback stay on this device unless you choose to export or share them yourself.",
        "Kommunikasieprofiele, woordeskat en terugvoer bly op hierdie toestel tensy jy kies om dit self te stuur of te deel.",
        "Amaphrofayela okuxhumana, ulimi, nempendulo ahlala kule divayisi ngaphandle kokuthi ukhethe ukuwathumela noma ukuwabelana ngawo."
    )
    val privacyControlTitle: String get() = t("You stay in control", "Jy bly in beheer", "Uhlala ulawula")
    val privacyControlBody: String get() = t(
        "You can change settings and delete profiles at any time. LISA does not require a cloud account.",
        "Jy kan enige tyd instellings verander en profiele verwyder. LISA vereis nie 'n wolkrekening nie.",
        "Ungashintsha izilungiselelo futhi usule amaphrofayela noma nini. I-LISA ayidingi i-akhawunti yefu."
    )
    val privacyQuestionsTitle: String get() = t("Questions?", "Vrae?", "Imibuzo?")
    val privacyQuestionsBody: String get() = t(
        "Support contact details will be published when LISA launches on Google Play.",
        "Ondersteuningskontakbesonderhede sal gepubliseer word wanneer LISA op Google Play bekendgestel word.",
        "Imininingwane yokuxhumana yokusekela izokhishwa lapho i-LISA ikhishwa ku-Google Play."
    )

    val vocabularyHelpNote: String get() = t(
        "Use at least $MIN_SEQUENCE_WINKS winks per phrase. A countdown lets you cancel or speak.",
        "Gebruik minstens $MIN_SEQUENCE_WINKS knippe per frase. 'n Aftelling laat jou kanselleer of praat.",
        "Sebenzisa okungenani ama-wink angu-$MIN_SEQUENCE_WINKS ibinzana ngalinye. Ukubala kwezinyathelo kukuvumela ukukhansela noma ukukhuluma."
    )

    // RC1 — panel copy, empty states, and shared labels
    val quickControlsCommunicationTiming: String get() = t("Communication Timing", "Kommunikasie-tydsberekening", "Isikhathi Sokuxhumana")
    val quickControlsPauseCommunication: String get() = t("Pause Communication", "Pauzeer kommunikasie", "Misa ukuxhumana")
    val quickControlsResumeCommunication: String get() = t("Resume Communication", "Hervat kommunikasie", "Qhubeka ukuxhumana")
    val developerToolsIntro: String get() = developerToolsPurpose
    val developerModeTitle: String get() = t("Developer Mode", "Ontwikkelaarmodus", "Imodi Yothuthukisi")
    val developerModeSubtitle: String get() = t(
        "Show detection details on the main screen",
        "Wys opsporingbesonderhede op die hoofskerm",
        "Bonisa imininingwane yokuthola esikrinini esiyinhloko"
    )
    val testingChecklistIntro: String get() = t(
        "Tick each item when it works as expected.",
        "Merk elke item wanneer dit soos verwag werk.",
        "Phawula yinto ngayinye uma isebenza njengoba kulindelwe."
    )
    val feedbackIntro: String get() = t(
        "Saved on this device only.",
        "Gestoor slegs op hierdie toestel.",
        "Kugcinwe kule divayisi kuphela."
    )
    val saveFeedback: String get() = saveLabel
    val profilesEmptyHint: String get() = t(
        "Create a profile to save communication settings for this device.",
        "Skep 'n profiel om kommunikasie-instellings vir hierdie toestel te stoor.",
        "Dala iphrofayela ukuze ulondoloze izilungiselelo zokuxhumana kule divayisi."
    )
    val activeProfileSection: String get() = t("Active profile", "Aktiewe profiel", "Iphrofayela esebenzayo")
    val savedProfilesSection: String get() = t("Saved profiles", "Gestoorde profiele", "Amaphrofayela alondoloziwe")
    val createNewProfile: String get() = t("New profile", "Nuwe profiel", "Iphrofayela entsha")
    val deleteActiveProfile: String get() = t("Delete profile", "Skrap profiel", "Susa iphrofayela")
    val skipToCommunication: String get() = t("Skip to Communication", "Slaan oor na Kommunikasie", "Yeqela uye Ekuxhumaneni")
    val goToCommunication: String get() = t(
        "Let's go to Communication.",
        "Kom ons gaan na Kommunikasie.",
        "Asiye ekuxhumaneni."
    )
    val caregiverAdvancedSkipNavigation: String get() = t(
        "For caregivers: Skip to Navigation Training",
        "Vir versorgers: Slaan oor na Navigasie-opleiding",
        "Kubaphakeli bokunakekela: Yeqela uye kuqeqesho lokuhamba"
    )
    val aboutVersionBody: String get() = t("Version 1.1", "Weergawe 1.1", "Inguqulo 1.1")
    val startGuidedLearning: String get() = t("Start Guided Learning", "Begin Geleide Leer", "Qala Ukufunda Okukhokhelwayo")
    val touchForNow: String get() = t("Use touch", "Gebruik aanraking", "Sebenzisa ukuthinta")
    val settingsSectionDetection: String get() = t("Detection", "Opsporing", "Ukuthola")
    val settingsSectionDisplay: String get() = t("Display", "Vertoning", "Isibonisi")
    val textSize: String get() = t("Text size", "Teksgrootte", "Usayizi wombhalo")
    val settingsSectionCommunication: String get() = t("Communication", "Kommunikasie", "Ukuxhumana")
    val settingsSectionEmergency: String get() = t("Emergency", "Nood", "Usizo oluphuthumayo")
    val settingsSectionAdvanced: String get() = t("Advanced", "Gevorderd", "Okuthuthukile")
    val settingsSectionData: String get() = t("Data", "Data", "Idatha")
    val settingsSectionLearning: String get() = t("Learning", "Leer", "Ukufunda")
    val settingsSectionSupportDiagnostics: String get() = t(
        "Support & Diagnostics",
        "Ondersteuning en Diagnostiek",
        "Ukusekela Nokuhlola"
    )
    val runDeviceCheckTitle: String get() = t("Run Device Check", "Doen toestel-kontrole", "Qala Ukuhlola Idivayisi")
    val runDeviceCheckSubtitle: String get() = t(
        "Confirm camera, wink detection, and emergency alarm.",
        "Bevestig kamera, knip-opsporing en noodalarm.",
        "Qinisekisa ikhamera, ukutholwa kwe-wink, ne-alamu yosizo oluphuthumayo."
    )
    val calibrationTitle: String get() = t("Calibration", "Kalibrasie", "Ukunquma")
    val calibrationSubtitle: String get() = t(
        "Fine-tune eye detection",
        "Stel oog-opsporing fyn",
        "Lungisa ukutholwa kwamehlo"
    )
    val confirmationCountdownTitle: String get() = t("Confirmation time", "Bevestigingstyd", "Isikhathi sokuqinisekisa")
    val emergencyAlarmVolumeTitle: String get() = t("Alarm volume", "Alarmvolume", "Ivolumu ye-alamu")
    val profileBackupTitle: String get() = t("Profile backup", "Profiel-rugsteun", "Isipele sephrofayela")
    val profileBackupSubtitle: String get() = t("Vocabulary and settings", "Woordeskat en instellings", "Ulimi nezilungiselelo")
    val exportLabel: String get() = t("Export", "Voer uit", "Thumela ngaphandle")
    val settingsSavedNote: String get() = t(
        "Saved to your active profile.",
        "Gestoor op jou aktiewe profiel.",
        "Kugcinwe kuphrofayela yakho esebenzayo."
    )
    val profileNameSection: String get() = t("Profile name", "Profielnaam", "Igama lephrofayela")
    val preferredLanguageSection: String get() = t("Preferred language", "Voorkeurtaal", "Ulimi oluthandwayo")
    val communicationLevelSection: String get() = t("Communication level", "Kommunikasievlak", "Izinga lokuxhumana")
    val nameLabel: String get() = t("Name", "Naam", "Igama")
    val saveLabel: String get() = t("Save", "Stoor", "Londoloza")
    val activeLabel: String get() = t("Active", "Aktief", "Iyasebenza")
    val addLabel: String get() = t("Add", "Voeg by", "Engeza")
    val deleteLabel: String get() = t("Delete", "Skrap", "Susa")
    val cancelLabel: String get() = t("Cancel", "Kanselleer", "Khansela")
    val releaseNotesVersionTitle: String get() = t("LISA 1.1", "LISA 1.1", "I-LISA 1.1")
    val clearStopsAlarm: String get() = t("Clear stops alarm", "Maak skoon stop alarm", "Ukusula kumisa i-alamu")
    val caregiverUnderstandsSetup: String get() = t(
        "Caregiver understands how LISA works",
        "Versorger verstaan hoe LISA werk",
        "Umphakeli wokunakekela uyayiqonda i-LISA"
    )
    val openLabel: String get() = t("Open", "Maak oop", "Vula")
    val replayLearningJourney: String get() = t("Replay tutorial", "Herhaal tutoriaal", "Phinda isifundo")
    val practiceCommunication: String get() = t("Practice communication", "Oefen kommunikasie", "Zilolonge ukuxhumana")
    val practiceNavigation: String get() = t("Practice navigation", "Oefen navigasie", "Zilolonge ukuhamba")
    val clearLearningProgress: String get() = t("Clear progress", "Maak vordering skoon", "Sula inqubekela phambili")
    val narrationTitle: String get() = t("Narration", "Vertelling", "Ukulandisa")
    val narrationSubtitle: String get() = t("During training", "Tydens opleiding", "Ngesikhathi sokuqeqeshwa")
    val voiceSpeedTitle: String get() = t("Voice speed", "Stemspoed", "Isivinini sezwi")
    val voiceVolumeTitle: String get() = t("Voice volume", "Stemvolume", "Ivolumu yezwi")
    val tutorialLanguageTitle: String get() = t("Tutorial language", "Tutoriumtaal", "Ulimi lwesifundo")
    val tutorialLanguageSubtitle: String get() = t("Uses your profile language", "Gebruik jou profieltaal", "Isebenzisa ulimi lwephrofayela yakho")
    val currentProfileLabel: String get() = t("Current profile", "Huidige profiel", "Iphrofayela yamanje")

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
        LisaPanel.VocabularyTraining -> vocabularyTraining
        LisaPanel.Voice -> voice
        LisaPanel.Settings -> settings
        LisaPanel.TestingChecklist -> testingChecklist
        LisaPanel.Feedback -> feedback
        LisaPanel.ReleaseNotes -> releaseNotes
        LisaPanel.DeveloperTools -> developerTools
        LisaPanel.AboutLisa -> aboutLisa
        LisaPanel.PrivacyPolicy -> privacyPolicy
        else -> ""
    }

    val leftDots: (Int) -> String get() = { c -> t("Left: ${winkDots(c)}", "Links: ${winkDots(c)}", "Kwesokunxele: ${winkDots(c)}") }
    val rightDots: (Int) -> String get() = { c -> t("Right: ${winkDots(c)}", "Regs: ${winkDots(c)}", "Kwesokudla: ${winkDots(c)}") }

    val minSequenceNote: String get() = vocabularyHelpNote
    val countdownNote: String get() = vocabularyHelpNote

    // Onboarding
    val stepOf: (Int, Int) -> String get() = { s, t -> t("Step $s of $t", "Stap $s van $t", "Isinyathelo $s kwangu-$t") }
    val welcomeToLisa: String get() = t("Welcome to LISA", "Welkom by LISA", "Siyakwamukela ku-LISA")
    val onboardingWelcomeBody: String get() = t(
        "LISA helps you communicate using intentional eye winks. Setup takes about one minute.",
        "LISA help jou om met doelbewuste oogknippe te kommunikeer. Opstelling neem ongeveer een minute.",
        "I-LISA ikusiza ukuxhumana ngama-wink ahleliwe ngamabomu. Ukusetha kuthatha cishe umzuzu owodwa."
    )
    val whatLisaDoes: String get() = t("What LISA does", "Wat LISA doen", "Okwenziwa yi-LISA")
    val onboardingWhatBody: String get() = t(
        "LISA detects wink sequences, matches phrases, asks you to confirm, and speaks aloud.",
        "LISA detecteer knip-reekse, pas frases, vra bevestiging, en praat hardop.",
        "I-LISA ithola ama-wink, ifanisa ibinzana, ikucela ukuqinisekise, bese ikhuluma ngokuzwakala."
    )
    val cameraPermissionTitle: String get() = t("Camera permission", "Kamera-toestemming", "Imvume yekhamera")
    val onboardingCameraBody: String get() = t(
        "LISA uses your front camera for eye tracking only — to see your face and detect winks. Video is processed on this device and is never uploaded.",
        "LISA gebruik jou voorste kamera slegs vir oognasporing — om jou gesig te sien en knippe te detecteer. Video word op hierdie toestel verwerk en word nooit opgelaai nie.",
        "I-LISA isebenzisa ikhamera yakho yangaphambili ukulandelela amehlo kuphela — ukubona ubuso bakho nokuthola ama-wink. Ividiyo icutshungulwa kule divayisi futhi ayilayishwa."
    )
    val allowCameraAccess: String get() = t("Allow camera access", "Laat kameratoegang toe", "Vumela ukufinyelela ikhamera")
    val onboardingSafetyTitle: String get() = t("Safety notice", "Veiligheidskennisgewing", "Isaziso sokuphepha")
    val primaryUserProfile: String get() = t("Primary User profile", "Primêre Gebruiker-profiel", "Iphrofayela yomsebenzisi oyinhloko")
    val confirmProfileName: String get() = t(
        "Name for the profile on this device.",
        "Naam vir die profiel op hierdie toestel.",
        "Igama lephrofayela kule divayisi."
    )
    val profileName: String get() = t("Profile name", "Profielnaam", "Igama lephrofayela")
    val startLisaTitle: String get() = t("Start LISA", "Begin LISA", "Qala i-LISA")
    val onboardingStartBody: String get() = t(
        "Open Menu anytime for vocabulary, voice, and settings.",
        "Maak Kieslys enige tyd oop vir woordeskat, stem en instellings.",
        "Vula Imenyu noma nini ukuze uthole ulimi, izwi nezilungiselelo."
    )
    val primaryUserLabel: (String) -> String get() = { name -> t("Primary User: $name", "Primêre Gebruiker: $name", "Umsebenzisi oyinhloko: $name") }

    // Camera permission screen
    val cameraAccessNeeded: String get() = t("Camera access needed", "Kameratoegang benodig", "Kudingeka ukufinyelela ikhamera")
    val cameraPermissionExplain: String get() = t(
        "LISA needs camera access for eye tracking. Without it, LISA cannot see your winks.",
        "LISA benodig kameratoegang vir oognasporing. Sonder dit kan LISA nie jou knippe sien nie.",
        "I-LISA idinga ukufinyelela ikhamera ukuze ilandelele amehlo. Ngaphandle kwayo, ayikwazi ukubona ama-wink akho."
    )
    val cameraOnDeviceOnly: String get() = t(
        "All video processing happens on this device. LISA does not upload or record your camera video for sharing.",
        "Alle video-verwerking gebeur op hierdie toestel. LISA laai nie jou kameravideo op of neem dit op vir deling nie.",
        "Konke ukucutshungulwa kwevidiyo kwenzeka kule divayisi. I-LISA ayilayishi ngevidiyo yakho yekhamera noma ayirekhode ukuze yabelwe."
    )
    val grantCameraPermission: String get() = t("Grant camera permission", "Gee kameratoestemming", "Nikeza imvume yekhamera")
    val openSettings: String get() = t("Open Settings", "Maak Instellings oop", "Vula Izilungiselelo")
    val cameraDeniedSettingsHint: String get() = t(
        "Camera permission was denied. Open Settings → LISA → Permissions and turn on Camera.",
        "Kameratoestemming is geweier. Maak Instellings → LISA → Toestemmings oop en skakel Kamera aan.",
        "Imvume yekhamera yenqatshwe. Vula Izilungiselelo → LISA → Izimvume bese uvula iKhamera."
    )

    // RC3 — user-facing errors and confirmations
    val cameraStartupFailed: String get() = t(
        "LISA couldn't access the camera. Please check that camera permission is enabled.",
        "LISA kon nie toegang tot die kamera kry nie. Gaan asseblief na dat kameratoestemming aangeskakel is.",
        "I-LISA ayikwazanga ukufinyelela ikhamera. Sicela uhlole ukuthi imvume yekhamera ivuliwe."
    )
    val speechEngineNotReady: String get() = t(
        "Speech is not ready yet. Please wait a moment and try again.",
        "Spraak is nog nie gereed nie. Wag asseblief 'n oomblik en probeer weer.",
        "Inkulumo ayikakulungeli. Sicela ulinde isikhashana bese uzama futhi."
    )
    val voiceInstallerUnavailable: String get() = t(
        "LISA couldn't open the voice installer. Check your device speech settings.",
        "LISA kon nie die steminstallering oopmaak nie. Gaan jou toestel se spraakinstellings na.",
        "I-LISA ayikwazanga ukuvula isifaki sezwi. Hlola izilungiselelo zenkulumo zedivayisi yakho."
    )
    val speechSettingsUnavailable: String get() = t(
        "LISA couldn't open speech settings. Open Settings on your device and look for Text-to-speech.",
        "LISA kon nie spraakinstellings oopmaak nie. Maak Instellings op jou toestel oop en soek Teks-na-spraak.",
        "I-LISA ayikwazanga ukuvula izilungiselelo zenkulumo. Vula Izilungiselelo kudivayisi yakho bese ufuna i-Text-to-speech."
    )
    val feedbackSavedConfirmation: String get() = t(
        "Thank you — your feedback was saved on this device.",
        "Dankie — jou terugvoer is op hierdie toestel gestoor.",
        "Siyabonga — impendulo yakho ilondoloziwe kule divayisi."
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
        "Record your voice for LISA to use later.",
        "Neem jou stem op vir LISA om later te gebruik.",
        "Rekhoda izwi lakho ukuze i-LISA ilisebenzise kamuva."
    )
    val familyVoiceHomeDescription: String get() = t(
        "A family member can share their voice.",
        "'n Familielid kan hul stem deel.",
        "Ilunga lomndeni lingabelana ngezwi lalo."
    )
    val familyVoiceTitle: String get() = t("Family Voice", "Familiestem", "Izwi Lomndeni")
    val manage: String get() = t("Manage", "Bestuur", "Phatha")
    val comingSoon: String get() = t("Coming soon", "Binnekort beskikbaar", "Kuyeza maduzane")
    val statusActive: String get() = t("Active", "Aktief", "Iyasebenza")
    val statusInstalled: String get() = t("Installed", "Geïnstalleer", "Ifakiwe")
    val statusUnavailable: String get() = t("Not available yet", "Nog nie beskikbaar nie", "Ayikatholakali okwamanje")
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
        "More natural speech than standard phone voices.",
        "Meer natuurlike spraak as standaard foonstemme.",
        "Inkulumo ejwayelekile kunamazwi afoni ajwayelekile."
    )
    val premiumBenefitPronunciation: String get() = t("Better pronunciation", "Beter uitspraak", "Ukukhuluma okungcono")
    val premiumBenefitAccent: String get() = t("South African accents", "Suid-Afrikaanse aksente", "Amadla e-South Africa")
    val premiumBenefitExpressive: String get() = t("More expressive speech", "Meer uitdrukkingryke spraak", "Inkulumo enobuchule okwengeziwe")
    val premiumBenefitFast: String get() = t("Faster speech generation", "Vinniger spraakgenerering", "Ukukhiqiza inkulumo okusheshayo")
    val premiumBenefitMoreVoices: String get() = t("More voices", "Meer stemme", "Amazwi amaningi")
    val myVoiceIntro: String get() = t(
        "Record your own voice for LISA to use. This feature will be available in a future update.",
        "Neem jou eie stem op vir LISA om te gebruik. Hierdie funksie sal in 'n toekomstige opdatering beskikbaar wees.",
        "Rekhoda izwi lakho ukuze i-LISA ilisebenzise. Lesi sici sizotholakala esibuyekezweni esizayo."
    )
    val myVoiceStepRecord: String get() = t("Record voice", "Neem stem op", "Rekhoda izwi")
    val myVoiceStepLearn: String get() = t("Voice profile is created", "Stemprofiel word geskep", "Iphrofayela yezwi idalwa")
    val myVoiceStepSpeak: String get() = t("LISA speaks using your voice", "LISA praat met jou stem", "I-LISA ikhuluma ngezwi lakho")
    val familyVoiceIntro: String get() = t(
        "A family member can share a familiar voice for LISA to use.",
        "'n Familielid kan 'n vertroude stem deel vir LISA om te gebruik.",
        "Ilunga lomndeni lingabelana ngezwi elijwayelekile ukuze i-LISA ilisebenzise."
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
    val testVoice: String get() = t("Preview voice", "Voorskou stem", "Buka kuqala izwi")
    val installVoiceData: String get() = t("Install voices", "Installeer stemme", "Faka amazwi")
    val openTtsSettings: String get() = t("Speech settings", "Spraak-instellings", "Izilungiselelo zokukhuluma")
    val poorLocalVoiceWarning: String get() = t(
        "No good local voice for this language. Install voice data or change the speech engine.",
        "Geen goeie plaaslike stem vir hierdie taal nie. Installeer stemdata of verander die spraak-enjin.",
        "Alikho izwi elihle lolu limi. Faka idatha yezwi noma ushintshe injini yokukhuluma."
    )
    val voiceSettingsSavedHint: String get() = t(
        "Saved to your active profile.",
        "Gestoor op jou aktiewe profiel.",
        "Kugcinwe kuphrofayela yakho esebenzayo."
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
