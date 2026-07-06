package com.idworx.lisa.features.corecommunicationreliability.phrase

import com.idworx.lisa.PreferredLanguage
import com.idworx.lisa.WinkMapping
import com.idworx.lisa.findExactMapping
import com.idworx.lisa.isEmergencySequence

data class PhraseMatchResult(
    val mapping: WinkMapping?,
    val phraseId: String?,
    val phraseText: String?,
    val matchCount: Int,
    val isEmergency: Boolean
)

object PhraseMatchVerifier {

    fun verify(
        left: Int,
        right: Int,
        mappings: List<WinkMapping>,
        language: PreferredLanguage
    ): PhraseMatchResult {
        if (isEmergencySequence(left, right)) {
            return PhraseMatchResult(
                mapping = mappings.firstOrNull { it.left == left && it.right == right },
                phraseId = "emergency",
                phraseText = mappings.firstOrNull { it.left == left && it.right == right }
                    ?.localizedPhrase(language),
                matchCount = 1,
                isEmergency = true
            )
        }
        val exactMatches = mappings.filter { it.left == left && it.right == right }
        val mapping = findExactMapping(left, right, mappings)
        return PhraseMatchResult(
            mapping = mapping,
            phraseId = mapping?.vocabularyId,
            phraseText = mapping?.localizedPhrase(language),
            matchCount = exactMatches.size,
            isEmergency = false
        )
    }
}
