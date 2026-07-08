package com.idworx.lisa

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers the real-device bug: long, fully visible blink sequences (e.g. 6 Left Winks to arm
 * Emergency) were being cut off before the user could finish them whenever they paused close to
 * the per-wink allowance between blinks — because the total-sequence-age safety cutoff
 * ([SequenceProcessingDelay.maxWindowMs]) was too tight relative to the idle allowance itself.
 */
class SequenceProcessingDelayTest {

    @Test
    fun defaultResponseTimeIsFiveSeconds() {
        assertEquals(5, SequenceProcessingDelay.DEFAULT_SECONDS)
        assertEquals(5000L, SequenceProcessingDelay.toMillis(SequenceProcessingDelay.DEFAULT_SECONDS))
        assertEquals(5000L, SEQUENCE_IDLE_TIMEOUT_MS)
    }

    @Test
    fun boundsAreThreeToEightSeconds() {
        assertEquals(3, SequenceProcessingDelay.MIN_SECONDS)
        assertEquals(8, SequenceProcessingDelay.MAX_SECONDS)
        assertEquals(3..8, SequenceProcessingDelay.allowedSeconds)
    }

    @Test
    fun coerceClampsToNewBounds() {
        assertEquals(3, SequenceProcessingDelay.coerce(0))
        assertEquals(3, SequenceProcessingDelay.coerce(1))
        assertEquals(3, SequenceProcessingDelay.coerce(3))
        assertEquals(8, SequenceProcessingDelay.coerce(8))
        assertEquals(8, SequenceProcessingDelay.coerce(20))
    }

    @Test
    fun guidedDefaultMatchesSharedDefault() {
        assertEquals(SequenceProcessingDelay.DEFAULT_SECONDS, SequenceProcessingDelay.GUIDED_DEFAULT_SECONDS)
    }

    @Test
    fun responseTimeControlsStepWithinBounds_matchingSensitivityStyle() {
        var sec = SequenceProcessingDelay.DEFAULT_SECONDS
        repeat(10) { sec = SequenceProcessingDelay.coerce(sec + 1) }
        assertEquals(SequenceProcessingDelay.MAX_SECONDS, sec)
        repeat(20) { sec = SequenceProcessingDelay.coerce(sec - 1) }
        assertEquals(SequenceProcessingDelay.MIN_SECONDS, sec)
    }

    @Test
    fun legacyResponseSpeedEnumSpansTheSameSharedBounds() {
        assertEquals(SequenceProcessingDelay.MIN_SECONDS, ResponseSpeed.Fast.toProcessingDelaySeconds())
        assertEquals(SequenceProcessingDelay.DEFAULT_SECONDS, ResponseSpeed.Normal.toProcessingDelaySeconds())
        assertEquals(SequenceProcessingDelay.MAX_SECONDS, ResponseSpeed.Slow.toProcessingDelaySeconds())
        assertEquals(ResponseSpeed.Normal, ResponseSpeed.default)
    }

    // --- Long visible sequences must not be cut off early -------------------------------------

    @Test
    fun sixLeftWinks_atMaxResponseTime_isNeverCutOffBeforeCompletion() {
        val sec = SequenceProcessingDelay.MAX_SECONDS
        val idleTimeoutMs = SequenceProcessingDelay.toMillis(sec)
        val maxWindowMs = SequenceProcessingDelay.maxWindowMs(sec)
        // Worst case: the user pauses just under the idle allowance between each of the 6 blinks.
        val gapMs = idleTimeoutMs - 1
        for (leftSoFar in 1..EMERGENCY_LEFT_WINKS) {
            val sequenceAgeMs = gapMs * (leftSoFar - 1)
            val cutOff = shouldFinalizeSequence(
                left = leftSoFar,
                right = 0,
                idleMs = 0L,
                sequenceAgeMs = sequenceAgeMs,
                idleTimeoutMs = idleTimeoutMs,
                maxWindowMs = maxWindowMs
            )
            assertFalse("must not cut off at blink $leftSoFar of $EMERGENCY_LEFT_WINKS", cutOff)
        }
    }

    @Test
    fun sixLeftWinks_atDefaultResponseTime_isNeverCutOffBeforeCompletion() {
        val sec = SequenceProcessingDelay.DEFAULT_SECONDS
        val idleTimeoutMs = SequenceProcessingDelay.toMillis(sec)
        val maxWindowMs = SequenceProcessingDelay.maxWindowMs(sec)
        val gapMs = idleTimeoutMs - 1
        for (leftSoFar in 1..EMERGENCY_LEFT_WINKS) {
            val sequenceAgeMs = gapMs * (leftSoFar - 1)
            assertFalse(
                shouldFinalizeSequence(
                    left = leftSoFar,
                    right = 0,
                    idleMs = 0L,
                    sequenceAgeMs = sequenceAgeMs,
                    idleTimeoutMs = idleTimeoutMs,
                    maxWindowMs = maxWindowMs
                )
            )
        }
    }

    @Test
    fun idleAllowance_restartsOnEveryWink_notJustTheFirst() {
        // A near-timeout gap followed by a fresh wink (idleMs reset to 0) must not finalize —
        // proves the allowance is per-wink, not a single fixed window from sequence start.
        val sec = SequenceProcessingDelay.DEFAULT_SECONDS
        val idleTimeoutMs = SequenceProcessingDelay.toMillis(sec)
        val maxWindowMs = SequenceProcessingDelay.maxWindowMs(sec)
        assertFalse(
            shouldFinalizeSequence(
                left = 3,
                right = 0,
                idleMs = 0L,
                sequenceAgeMs = idleTimeoutMs - 1,
                idleTimeoutMs = idleTimeoutMs,
                maxWindowMs = maxWindowMs
            )
        )
    }

    @Test
    fun sequenceFinalizes_onceIdleAllowanceExpires() {
        val sec = SequenceProcessingDelay.DEFAULT_SECONDS
        val idleTimeoutMs = SequenceProcessingDelay.toMillis(sec)
        val maxWindowMs = SequenceProcessingDelay.maxWindowMs(sec)
        assertTrue(
            shouldFinalizeSequence(
                left = 2,
                right = 0,
                idleMs = idleTimeoutMs,
                sequenceAgeMs = idleTimeoutMs,
                idleTimeoutMs = idleTimeoutMs,
                maxWindowMs = maxWindowMs
            )
        )
    }

    @Test
    fun maxWindowMs_scalesWithSelectedSeconds_notACoarseThreeTierBucket() {
        val atMin = SequenceProcessingDelay.maxWindowMs(SequenceProcessingDelay.MIN_SECONDS)
        val atMax = SequenceProcessingDelay.maxWindowMs(SequenceProcessingDelay.MAX_SECONDS)
        assertTrue("max window must grow with the selected response time", atMax > atMin)
    }

    @Test
    fun quickResolveIdleWindow_isShorterThanTheMinimumAllowedResponseTime() {
        assertTrue(GuidedModeNavigation.QUICK_RESOLVE_IDLE_MS < SequenceProcessingDelay.toMillis(SequenceProcessingDelay.MIN_SECONDS))
    }
}
