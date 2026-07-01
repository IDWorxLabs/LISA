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
