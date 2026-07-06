package com.idworx.lisa.validation.authority

import com.idworx.lisa.features.experiencepolish.emotionalpresence.EmotionalPresenceEngine
import com.idworx.lisa.features.experiencepolish.emotionalpresence.metadata.EmotionalPresenceMetadata
import com.idworx.lisa.features.experiencepolish.emotionalpresence.model.PresenceSessionTracker
import com.idworx.lisa.features.experiencepolish.emotionalpresence.validation.EmotionalPresenceAuthorityV1
import com.idworx.lisa.features.personality.model.DialogueContext
import com.idworx.lisa.features.personality.model.PresenceMoment
import com.idworx.lisa.validation.ValidationOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EmotionalPresenceAuthorityV1Test {

    @Test
    fun fullValidation_passesAndEmitsPassToken() {
        val report = EmotionalPresenceAuthorityV1.validate()
        if (report.outcome != ValidationOutcome.PASS) {
            println("FAILED: ${report.failedChecks}")
        }
        assertEquals("Failed: ${report.failedChecks}", ValidationOutcome.PASS, report.outcome)
        assertEquals(EmotionalPresenceAuthorityV1.PASS_TOKEN, report.passToken)
        assertTrue(report.failedChecks.isEmpty())
        println(report.formatReport())
        println(EmotionalPresenceAuthorityV1.PASS_TOKEN)
    }

    @Test
    fun rateLimit_blocksSecondSpeechWithinInterval() {
        val tracker = PresenceSessionTracker(
            lastPresenceSpokenMs = 1_000L,
            presenceCountThisHour = 1,
            hourWindowStartMs = 1_000L
        )
        val ctx = DialogueContext(idleDurationMs = EmotionalPresenceEngine.LONG_PAUSE_THRESHOLD_MS)
        assertFalse(
            EmotionalPresenceEngine.shouldSpeak(
                PresenceMoment.LongPauseEncouragement,
                ctx,
                tracker,
                nowMs = 60_000L
            )
        )
    }

    @Test
    fun sessionOpening_onlyOncePerSession() {
        val ctx = DialogueContext()
        val tracker = PresenceSessionTracker(sessionStartPlayed = true)
        assertFalse(
            EmotionalPresenceEngine.shouldSpeak(PresenceMoment.SessionOpening, ctx, tracker)
        )
    }

    @Test
    fun warmReturn_requiresReturningUser() {
        val ctx = DialogueContext(returningUser = true, daysSinceLastSession = 2)
        assertTrue(
            EmotionalPresenceEngine.shouldSpeak(
                PresenceMoment.WarmReturnGreeting,
                ctx,
                PresenceSessionTracker()
            )
        )
    }

    @Test
    fun catalog_hasAllPresenceTags() {
        assertTrue(EmotionalPresenceEngine.catalogHasPresenceDialogues())
    }

    @Test
    fun metadata_definesPassToken() {
        assertEquals(
            "LISA_EMOTIONAL_PRESENCE_V1_PASS",
            EmotionalPresenceMetadata.PASS_TOKEN
        )
    }
}
