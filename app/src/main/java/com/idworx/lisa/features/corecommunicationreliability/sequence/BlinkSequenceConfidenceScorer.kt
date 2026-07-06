package com.idworx.lisa.features.corecommunicationreliability.sequence

import com.idworx.lisa.features.corecommunicationreliability.model.SequenceRecognitionResult

/**
 * Deterministic confidence scoring using observable sequence indicators only.
 * Does not claim camera or ML accuracy — scores structural validity.
 */
object BlinkSequenceConfidenceScorer {

    fun score(
        validation: SequenceRecognitionResult,
        duplicateBlocked: Boolean,
        exactPhraseMatch: Boolean
    ): Float {
        if (duplicateBlocked) return 0f
        if (!validation.isComplete) return 0.1f

        var score = 0.4f
        if (exactPhraseMatch) score += 0.35f
        if (!validation.isAmbiguous) score += 0.1f
        if (!validation.hasContinuation) score += 0.1f
        if (!validation.isReserved || validation.left == 6 && validation.right == 0) score += 0.05f
        if (validation.blockedReason != null) score -= 0.4f

        return score.coerceIn(0f, 1f)
    }
}
