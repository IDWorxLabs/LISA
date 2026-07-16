package com.idworx.lisa

/**
 * Canonical duplicate detection for built-in and custom vocabulary (RC7D.9).
 *
 * UI-independent — returns structured matches for composer and storage layers.
 */
enum class PhraseDuplicateSource {
    BuiltIn,
    Custom
}

data class DuplicatePhraseMatch(
    val phrase: String,
    val category: CustomPhraseEngine.CaregiverPhraseCategory,
    val source: PhraseDuplicateSource,
    val sequence: Pair<Int, Int>? = null
)

data class PhraseComposerRuntimeContext(
    val customMappings: List<WinkMapping> = emptyList(),
    val language: PreferredLanguage = PreferredLanguage.English
)

object PhraseDuplicateEngine {

    /** Canonical identity for duplicate comparison — not raw string equality. */
    fun canonicalIdentity(raw: String): String {
        var text = raw.trim()
        if (text.length >= 2 &&
            ((text.first() == '"' && text.last() == '"') || (text.first() == '\'' && text.last() == '\''))
        ) {
            text = text.substring(1, text.length - 1).trim()
        }
        text = text.replace(Regex("\\s+"), " ")
        text = text.trimEnd('.', '!', '?', ',', ';', ':')
        return text.lowercase()
    }

    fun findDuplicate(
        rawPhrase: String,
        customMappings: List<WinkMapping>,
        language: PreferredLanguage = PreferredLanguage.English,
        uiStrings: LisaUiStrings = LisaUiStrings.forLanguage(language)
    ): DuplicatePhraseMatch? {
        val target = canonicalIdentity(rawPhrase)
        if (target.isBlank()) return null

        customMappings.filter { it.isCustom }.forEach { mapping ->
            val phrase = mapping.customPhrase.orEmpty()
            if (canonicalIdentity(phrase) == target) {
                return DuplicatePhraseMatch(
                    phrase = phrase,
                    category = mapping.caregiverCategory ?: CustomPhraseEngine.CaregiverPhraseCategory.Conversation,
                    source = PhraseDuplicateSource.Custom,
                    sequence = mapping.left to mapping.right
                )
            }
        }

        findBuiltInDuplicate(target, language, uiStrings)?.let { return it }
        return null
    }

    /** Reports stored custom mappings that share the same canonical phrase identity. */
    fun auditStoredCustomDuplicates(customMappings: List<WinkMapping>): List<Pair<WinkMapping, WinkMapping>> {
        val custom = customMappings.filter { it.isCustom }
        val findings = mutableListOf<Pair<WinkMapping, WinkMapping>>()
        custom.forEachIndexed { index, first ->
            custom.drop(index + 1).forEach { second ->
                if (canonicalIdentity(first.customPhrase.orEmpty()) == canonicalIdentity(second.customPhrase.orEmpty())) {
                    findings.add(first to second)
                }
            }
        }
        return findings
    }

    private fun findBuiltInDuplicate(
        target: String,
        language: PreferredLanguage,
        uiStrings: LisaUiStrings
    ): DuplicatePhraseMatch? {
        defaultLanguageMappings().forEach { mapping ->
            PreferredLanguage.entries.forEach { lang ->
                if (canonicalIdentity(mapping.localizedPhrase(lang)) == target) {
                    val category = categoryForVocabularyId(mapping.vocabularyId)
                        ?: CustomPhraseEngine.CaregiverPhraseCategory.Conversation
                    return DuplicatePhraseMatch(
                        phrase = mapping.localizedPhrase(language),
                        category = category,
                        source = PhraseDuplicateSource.BuiltIn,
                        sequence = mapping.left to mapping.right
                    )
                }
            }
        }

        GuidedVocabularyCatalog.catalogPhraseEntries(language, uiStrings).forEach { entry ->
            if (canonicalIdentity(entry.phrase) == target) {
                return DuplicatePhraseMatch(
                    phrase = entry.phrase,
                    category = entry.category,
                    source = PhraseDuplicateSource.BuiltIn,
                    sequence = null
                )
            }
        }
        return null
    }

    private fun categoryForVocabularyId(vocabularyId: String): CustomPhraseEngine.CaregiverPhraseCategory? =
        GuidedVocabularyCatalog.categoryForCoreVocabularyId(vocabularyId)?.toCaregiverCategory()
}

internal fun GuidedVocabularyCategory.toCaregiverCategory(): CustomPhraseEngine.CaregiverPhraseCategory? = when (this) {
    GuidedVocabularyCategory.Conversation -> CustomPhraseEngine.CaregiverPhraseCategory.Conversation
    GuidedVocabularyCategory.BasicNeeds -> CustomPhraseEngine.CaregiverPhraseCategory.BasicNeeds
    GuidedVocabularyCategory.Medical -> CustomPhraseEngine.CaregiverPhraseCategory.Medical
    GuidedVocabularyCategory.Family -> CustomPhraseEngine.CaregiverPhraseCategory.Family
    GuidedVocabularyCategory.Custom,
    GuidedVocabularyCategory.PhraseManagement,
    GuidedVocabularyCategory.AdjustSettings,
    GuidedVocabularyCategory.BasicSystemControls,
    GuidedVocabularyCategory.Preferences -> null
}
