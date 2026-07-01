package com.idworx.lisa

/**
 * Built-in communication language for LISA.
 *
 * Philosophy: accuracy over speed. Single winks and natural blinks never produce speech.
 * Only deliberate multi-wink sequences (minimum [MIN_SEQUENCE_WINKS] total) are evaluated
 * after the idle timeout.
 *
 * Expand this list as the core vocabulary grows. Custom user mappings are stored separately.
 */

data class WinkMapping(
    val left: Int,
    val right: Int,
    val phrase: String,
    val isCustom: Boolean = false
)

/** Minimum total winks (left + right) required before a sequence can produce speech. */
const val MIN_SEQUENCE_WINKS = 2
const val MIN_SENSITIVITY_LEVEL = 1
const val MAX_SENSITIVITY_LEVEL = 5
const val DEFAULT_SENSITIVITY_LEVEL = 3

fun isSequenceEligibleForSpeech(left: Int, right: Int): Boolean =
    left + right >= MIN_SEQUENCE_WINKS

/**
 * Default built-in phrase mappings.
 * Add new entries here — do not scatter phrase definitions elsewhere.
 *
 * Prefix safety: several phrases share a left/right prefix path (e.g. L5 R0 → L5 R1 → L5 R2).
 * [SequenceFinalization] holds partial sequences until idle + no longer continuation, or
 * [LONG_SEQUENCE_TIMEOUT_MS]. Emergency uses L6 R0 so it does not share a prefix chain with
 * L5 R0 "I need suction", L5 R1 "Thank you", or L5 R2 "Stop".
 */
fun defaultLanguageMappings(): List<WinkMapping> = listOf(
    WinkMapping(2, 0, "Yes"),
    WinkMapping(0, 2, "No"),
    WinkMapping(1, 1, "I need help"),
    WinkMapping(2, 1, "I am in pain"),
    WinkMapping(0, 3, "I need water"),
    WinkMapping(3, 1, "I need food"),
    WinkMapping(1, 2, "I need the toilet"),
    WinkMapping(3, 0, "I am tired"),
    WinkMapping(0, 4, "I am cold"),
    WinkMapping(2, 2, "I am hot"),
    WinkMapping(1, 3, "I can't breathe"),
    WinkMapping(3, 2, "Please call someone"),
    WinkMapping(4, 0, "Call my caregiver"),
    WinkMapping(0, 5, "Call my family"),
    WinkMapping(2, 3, "I need medicine"),
    WinkMapping(3, 3, "I feel uncomfortable"),
    WinkMapping(4, 1, "Please move me"),
    WinkMapping(1, 4, "Please turn me"),
    WinkMapping(4, 2, "I want to sleep"),
    WinkMapping(4, 3, "I want to sit up"),
    WinkMapping(2, 4, "I want to lie down"),
    WinkMapping(5, 0, "I need suction"),
    WinkMapping(0, 6, "I need the nurse"),
    WinkMapping(5, 1, "Thank you"),
    WinkMapping(1, 5, "I love you"),
    WinkMapping(4, 4, "I don't understand"),
    WinkMapping(3, 4, "Please repeat"),
    WinkMapping(5, 2, "Stop"),
    WinkMapping(2, 5, "Continue"),
    WinkMapping(6, 0, "Emergency")
)
