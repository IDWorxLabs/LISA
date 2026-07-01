package com.idworx.lisa

/** Max time to hold a partial sequence that may still extend toward a longer mapping. */
const val LONG_SEQUENCE_TIMEOUT_MS = 8000L

fun currentSequence(left: Int, right: Int): Pair<Int, Int> = left to right

fun findExactMapping(left: Int, right: Int, mappings: List<WinkMapping>): WinkMapping? {
    if (!isSequenceEligibleForSpeech(left, right)) return null
    return mappings.firstOrNull { it.left == left && it.right == right }
}

/**
 * True when any mapping can still be reached by adding more left and/or right winks
 * without removing winks already counted.
 */
fun hasLongerContinuation(left: Int, right: Int, mappings: List<WinkMapping>): Boolean =
    mappings.any { mapping ->
        mapping.left >= left &&
            mapping.right >= right &&
            !(mapping.left == left && mapping.right == right)
    }

/**
 * Prefix-aware finalization: do not finalize at the normal idle timeout when the current
 * L/R count could still grow into a longer valid mapping. In that case, hold until
 * [LONG_SEQUENCE_TIMEOUT_MS] or until the user adds more winks.
 */
fun shouldFinalizeSequence(
    left: Int,
    right: Int,
    idleMs: Long,
    sequenceAgeMs: Long,
    idleTimeoutMs: Long,
    maxWindowMs: Long,
    mappings: List<WinkMapping>
): Boolean {
    if (left == 0 && right == 0) return false

    if (!isSequenceEligibleForSpeech(left, right)) {
        return idleMs >= idleTimeoutMs || sequenceAgeMs >= maxWindowMs
    }

    if (sequenceAgeMs >= LONG_SEQUENCE_TIMEOUT_MS) return true

    if (hasLongerContinuation(left, right, mappings)) return false

    return idleMs >= idleTimeoutMs || sequenceAgeMs >= maxWindowMs
}
