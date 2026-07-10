package com.idworx.lisa

fun currentSequence(left: Int, right: Int): Pair<Int, Int> = left to right

fun findExactMapping(left: Int, right: Int, mappings: List<WinkMapping>): WinkMapping? {
    if (!isSequenceEligibleForSpeech(left, right)) return null
    if (LisaSystemLanguage.isReservedSystemSequence(left, right)) return null
    return mappings.firstOrNull { it.left == left && it.right == right }
}

/**
 * True when any communication mapping can still be reached by adding more winks.
 * System command sequences are excluded from continuation checks.
 */
fun hasLongerContinuation(left: Int, right: Int, mappings: List<WinkMapping>): Boolean =
    mappings.any { mapping ->
        if (LisaSystemLanguage.isReservedSystemSequence(mapping.left, mapping.right)) return@any false
        mapping.left >= left &&
            mapping.right >= right &&
            !(mapping.left == left && mapping.right == right)
    }

/**
 * Finalize ONLY when idle after the most recent confirmed wink — there is no early/quick-resolve
 * fast path anymore. No gesture (phrase, category, navigation, confirm, cancel, or emergency) may
 * execute while the user is still actively blinking; every sequence, ambiguous or not, waits out
 * the full configured response-time idle allowance before it is processed. [sequenceAgeMs] is only
 * used as an absolute safety cap for very long sequences that never go idle.
 */
fun shouldFinalizeSequence(
    left: Int,
    right: Int,
    idleMs: Long,
    sequenceAgeMs: Long,
    idleTimeoutMs: Long,
    maxWindowMs: Long
): Boolean {
    if (left == 0 && right == 0) return false
    if (idleMs >= idleTimeoutMs) return true
    return sequenceAgeMs >= maxWindowMs
}
