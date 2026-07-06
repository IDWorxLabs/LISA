package com.idworx.lisa.features.corecommunicationreliability.sequence

import com.idworx.lisa.formatWinkSequenceShort

object BlinkSequenceNormalizer {

    fun normalize(left: Int, right: Int): NormalizedSequence = NormalizedSequence(
        left = left.coerceAtLeast(0),
        right = right.coerceAtLeast(0),
        label = formatWinkSequenceShort(left.coerceAtLeast(0), right.coerceAtLeast(0))
    )

    data class NormalizedSequence(
        val left: Int,
        val right: Int,
        val label: String
    )
}
