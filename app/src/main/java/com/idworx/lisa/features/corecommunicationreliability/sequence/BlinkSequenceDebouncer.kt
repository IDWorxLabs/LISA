package com.idworx.lisa.features.corecommunicationreliability.sequence

import com.idworx.lisa.features.corecommunicationreliability.metadata.CoreCommunicationReliabilityMetadata

class BlinkSequenceDebouncer(
    private val debounceWindowMs: Long = CoreCommunicationReliabilityMetadata.DEFAULT_DEBOUNCE_MS
) {
    private var lastSequence: Pair<Int, Int>? = null
    private var lastFireMs: Long = 0L

    fun shouldAllow(left: Int, right: Int, nowMs: Long = System.currentTimeMillis()): Boolean {
        val seq = left to right
        if (lastSequence == seq && nowMs - lastFireMs < debounceWindowMs) {
            return false
        }
        lastSequence = seq
        lastFireMs = nowMs
        return true
    }

    fun reset() {
        lastSequence = null
        lastFireMs = 0L
    }

    fun isDuplicate(left: Int, right: Int, nowMs: Long = System.currentTimeMillis()): Boolean {
        val seq = left to right
        return lastSequence == seq && nowMs - lastFireMs < debounceWindowMs
    }
}
