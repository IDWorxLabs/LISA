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
 * True when [left]/[right] exactly matches a currently visible gesture and at least one *other*
 * visible gesture could still be reached by continuing to blink — e.g. Yes (L2 R1) while Stop
 * (L2 R3) is also visible on the same phrase page. Ambiguous matches must wait for the full
 * configured response-time idle timeout instead of resolving immediately, so a user still building
 * toward a longer visible gesture is never short-circuited into executing a shorter visible prefix
 * of it. Only gestures in [visibleGestures] are considered — hidden/off-screen gestures (a phrase on
 * another page, an inactive category shortcut, an unavailable Previous/Next) never create ambiguity.
 */
fun isAmbiguousVisibleMatch(left: Int, right: Int, visibleGestures: Set<Pair<Int, Int>>): Boolean {
    if (left to right !in visibleGestures) return false
    return visibleGestures.any { (otherLeft, otherRight) ->
        otherLeft >= left && otherRight >= right && (otherLeft != left || otherRight != right)
    }
}

/**
 * True when [left]/[right] is an exact, currently visible match with no ambiguity — safe to resolve
 * as soon as the user stops blinking (see [com.idworx.lisa.GuidedModeNavigation.QUICK_RESOLVE_IDLE_MS])
 * rather than waiting out the full response-time idle timeout. See [isAmbiguousVisibleMatch].
 */
fun isUnambiguousVisibleMatch(left: Int, right: Int, visibleGestures: Set<Pair<Int, Int>>): Boolean =
    (left to right) in visibleGestures && !isAmbiguousVisibleMatch(left, right, visibleGestures)

/**
 * Finalize when idle after the most recent confirmed wink.
 * [sequenceAgeMs] is only used as an absolute safety cap for very long sequences.
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
