package com.idworx.lisa

/**
 * Built-in communication language for LISA.
 *
 * Philosophy: accuracy over speed. Single winks and natural blinks never produce speech.
 * Only deliberate multi-wink sequences (minimum [MIN_SEQUENCE_WINKS] total) are evaluated
 * after the idle timeout.
 */

data class MultilingualPhrase(
    val english: String,
    val afrikaans: String,
    val isiZulu: String
) {
    fun text(language: PreferredLanguage): String = when (language) {
        PreferredLanguage.English -> english
        PreferredLanguage.Afrikaans -> afrikaans
        PreferredLanguage.IsiZulu -> isiZulu
    }
}

data class WinkMapping(
    val left: Int,
    val right: Int,
    val vocabularyId: String,
    val isCustom: Boolean = false,
    val customPhrase: String? = null
) {
    /** English text for persistence and legacy paths. */
    val phrase: String
        get() = if (isCustom) customPhrase.orEmpty() else LisaCoreVocabulary.text(vocabularyId, PreferredLanguage.English)

    fun localizedPhrase(language: PreferredLanguage): String =
        if (isCustom) customPhrase.orEmpty() else LisaCoreVocabulary.text(vocabularyId, language)
}

/** Minimum total winks (left + right) required before a sequence can produce speech. */
const val MIN_SEQUENCE_WINKS = 2
const val MIN_SENSITIVITY_LEVEL = 1
const val MAX_SENSITIVITY_LEVEL = 5
const val DEFAULT_SENSITIVITY_LEVEL = 3

fun isSequenceEligibleForSpeech(left: Int, right: Int): Boolean =
    left + right >= MIN_SEQUENCE_WINKS

object LisaCoreVocabulary {
    private data class Entry(val id: String, val left: Int, val right: Int, val phrase: MultilingualPhrase)

    private val entries: List<Entry> = listOf(
        Entry("hello", 1, 6, MultilingualPhrase("Hello", "Hallo", "Sawubona")),
        Entry("goodbye", 1, 2, MultilingualPhrase("Goodbye", "Totsiens", "Sala kahle")),
        Entry("how_are_you", 1, 3, MultilingualPhrase("How are you?", "Hoe gaan dit?", "Unjani?")),
        Entry("i_am_good", 1, 4, MultilingualPhrase("I am good", "Ek is goed", "Ngiphilile")),
        Entry("i_am_not_okay", 1, 5, MultilingualPhrase("I am not okay", "Ek is nie oukei nie", "Angiphilile")),
        Entry("thank_you", 2, 1, MultilingualPhrase("Thank you", "Dankie", "Ngiyabonga")),
        Entry("please", 2, 7, MultilingualPhrase("Please", "Asseblief", "Ngicela")),
        Entry("i_love_you", 2, 3, MultilingualPhrase("I love you", "Ek het jou lief", "Ngiyakuthanda")),
        Entry("yes", 2, 6, MultilingualPhrase("Yes", "Ja", "Yebo")),
        Entry("no", 0, 7, MultilingualPhrase("No", "Nee", "Cha")),
        Entry("please_repeat", 3, 1, MultilingualPhrase("Please repeat", "Herhaal asseblief", "Phinda futhi")),
        Entry("i_dont_understand", 3, 2, MultilingualPhrase("I don't understand", "Ek verstaan nie", "Angiqondi")),
        Entry("i_need_help", 3, 6, MultilingualPhrase("I need help", "Ek het hulp nodig", "Ngidinga usizo")),
        Entry("i_am_in_pain", 2, 4, MultilingualPhrase("I am in pain", "Ek het pyn", "Ngibuhlungu")),
        Entry("i_need_water", 0, 3, MultilingualPhrase("I need water", "Ek het water nodig", "Ngidinga amanzi")),
        Entry("i_need_food", 5, 4, MultilingualPhrase("I need food", "Ek het kos nodig", "Ngidinga ukudla")),
        Entry("i_need_the_toilet", 3, 4, MultilingualPhrase("I need the toilet", "Ek moet toilet toe", "Ngidinga indlu yangasese")),
        Entry("i_need_medicine", 2, 5, MultilingualPhrase("I need medicine", "Ek het medisyne nodig", "Ngidinga umuthi")),
        Entry("i_need_the_nurse", 0, 5, MultilingualPhrase("I need the nurse", "Ek het die verpleegster nodig", "Ngidinga umongikazi")),
        Entry("i_cant_breathe", 3, 5, MultilingualPhrase("I can't breathe", "Ek kan nie asemhaal nie", "Angikwazi ukuphefumula")),
        Entry("i_feel_uncomfortable", 4, 1, MultilingualPhrase("I feel uncomfortable", "Ek voel ongemaklik", "Angizolile")),
        Entry("i_am_cold", 0, 6, MultilingualPhrase("I am cold", "Ek is koud", "Ngiyabanda")),
        Entry("i_am_hot", 4, 2, MultilingualPhrase("I am hot", "Ek is warm", "Ngiyashisa")),
        Entry("please_move_me", 4, 3, MultilingualPhrase("Please move me", "Skuif my asseblief", "Ngicela ungisunde")),
        Entry("please_turn_me", 4, 4, MultilingualPhrase("Please turn me", "Draai my asseblief", "Ngicela ungaphendule")),
        Entry("i_want_to_sit_up", 4, 5, MultilingualPhrase("I want to sit up", "Ek wil op sit", "Ngifuna ukuhlala")),
        Entry("i_want_to_lie_down", 5, 1, MultilingualPhrase("I want to lie down", "Ek wil lê", "Ngifuna ukulala")),
        Entry("call_my_caregiver", 5, 2, MultilingualPhrase("Call my caregiver", "Bel my versorger", "Shaya umnakekeli wami")),
        Entry("call_my_family", 5, 3, MultilingualPhrase("Call my family", "Bel my familie", "Shaya umndeni wami")),
        Entry("emergency", 6, 0, MultilingualPhrase("Emergency", "Noodgeval", "Usizo oluphuthumayo"))
    )

    private val byId: Map<String, Entry> = entries.associateBy { it.id }

    fun text(vocabularyId: String, language: PreferredLanguage): String =
        byId[vocabularyId]?.phrase?.text(language) ?: vocabularyId

    val count: Int get() = entries.size
}

/** Default built-in phrase mappings (30 phrases). Emergency uses L6 R0. */
fun defaultLanguageMappings(): List<WinkMapping> = listOf(
    WinkMapping(1, 6, "hello"), // Remapped from L1 R1 — reserved for Repeat Last Phrase
    WinkMapping(1, 2, "goodbye"),
    WinkMapping(1, 3, "how_are_you"),
    WinkMapping(1, 4, "i_am_good"),
    WinkMapping(1, 5, "i_am_not_okay"),
    WinkMapping(2, 1, "thank_you"),
    WinkMapping(2, 7, "please"), // Remapped from L2 R2 — reserved for Pause / Resume
    WinkMapping(2, 3, "i_love_you"),
    WinkMapping(2, 6, "yes"), // Remapped from L2 R0 — reserved for Response Speed Normal
    WinkMapping(0, 7, "no"), // Remapped from L0 R2 — reserved for Increase Sensitivity
    WinkMapping(3, 1, "please_repeat"),
    WinkMapping(3, 2, "i_dont_understand"),
    WinkMapping(3, 6, "i_need_help"), // Remapped from L3 R3 — reserved for Practice Mode
    WinkMapping(2, 4, "i_am_in_pain"),
    WinkMapping(0, 3, "i_need_water"),
    WinkMapping(5, 4, "i_need_food"), // Remapped from L0 R4 — L0 R4 reserved for Quick Controls (L0 R4)
    WinkMapping(3, 4, "i_need_the_toilet"),
    WinkMapping(2, 5, "i_need_medicine"),
    WinkMapping(0, 5, "i_need_the_nurse"),
    WinkMapping(3, 5, "i_cant_breathe"),
    WinkMapping(4, 1, "i_feel_uncomfortable"),
    WinkMapping(0, 6, "i_am_cold"),
    WinkMapping(4, 2, "i_am_hot"),
    WinkMapping(4, 3, "please_move_me"),
    WinkMapping(4, 4, "please_turn_me"),
    WinkMapping(4, 5, "i_want_to_sit_up"),
    WinkMapping(5, 1, "i_want_to_lie_down"),
    WinkMapping(5, 2, "call_my_caregiver"),
    WinkMapping(5, 3, "call_my_family"),
    WinkMapping(6, 0, "emergency")
)
