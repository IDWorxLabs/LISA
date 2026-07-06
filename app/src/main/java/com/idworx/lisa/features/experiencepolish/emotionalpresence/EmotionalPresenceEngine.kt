package com.idworx.lisa.features.experiencepolish.emotionalpresence

import com.idworx.lisa.features.experiencepolish.emotionalpresence.model.PresenceSessionTracker
import com.idworx.lisa.features.personality.dialogue.DefaultDialogueCatalog
import com.idworx.lisa.features.personality.engine.LisaPersonalityEngine
import com.idworx.lisa.features.personality.engine.LisaPersonalityEngines
import com.idworx.lisa.features.personality.model.AppFeature
import com.idworx.lisa.features.personality.model.DialogueContext
import com.idworx.lisa.features.personality.model.LisaDialogue
import com.idworx.lisa.features.personality.model.PresenceMoment

/**
 * When Lisa speaks emotionally — rate-limited, brief, observable. All lines from [LisaPersonalityEngine].
 */
object EmotionalPresenceEngine {

    const val LONG_PAUSE_THRESHOLD_MS: Long = 45_000L
    const val MIN_SPEAK_INTERVAL_MS: Long = 90_000L
    const val MAX_PRESENCE_PER_HOUR: Int = 8
    const val MAX_LINES_SESSION_OPEN: Int = 1
    const val MAX_LINES_RETURN_GREETING: Int = 2
    const val MAX_LINES_MILESTONE: Int = 2

    private val personality: LisaPersonalityEngine get() = LisaPersonalityEngines.default

    fun maxLinesFor(moment: PresenceMoment): Int = when (moment) {
        PresenceMoment.SessionOpening -> MAX_LINES_SESSION_OPEN
        PresenceMoment.WarmReturnGreeting -> MAX_LINES_RETURN_GREETING
        PresenceMoment.EmotionalMilestone -> MAX_LINES_MILESTONE
        else -> 1
    }

    fun shouldSpeak(
        moment: PresenceMoment,
        context: DialogueContext,
        tracker: PresenceSessionTracker,
        nowMs: Long = System.currentTimeMillis()
    ): Boolean {
        if (!passesMomentGate(moment, context, tracker)) return false
        if (!passesRateLimit(tracker, nowMs)) return false
        return dialogueLines(context, moment).isNotEmpty()
    }

    fun dialogueLines(context: DialogueContext, moment: PresenceMoment): List<LisaDialogue> =
        personality.generatePresenceSequence(context, moment, maxLinesFor(moment))

    fun dialogueTexts(context: DialogueContext, moment: PresenceMoment): List<String> =
        dialogueLines(context, moment).map { it.text }

    fun caregiverReassurance(context: DialogueContext): String? =
        dialogueLines(context.copy(caregiverVisible = true), PresenceMoment.CaregiverReassurance)
            .firstOrNull()
            ?.text

    fun recordSpoken(
        tracker: PresenceSessionTracker,
        moment: PresenceMoment,
        nowMs: Long = System.currentTimeMillis()
    ): PresenceSessionTracker {
        val hourStart = if (nowMs - tracker.hourWindowStartMs > 3_600_000L) nowMs else tracker.hourWindowStartMs
        val hourCount = if (hourStart != tracker.hourWindowStartMs) 1 else tracker.presenceCountThisHour + 1
        return tracker.copy(
            lastPresenceSpokenMs = nowMs,
            lastMoment = moment,
            presenceCountThisHour = hourCount,
            hourWindowStartMs = hourStart,
            sessionStartPlayed = tracker.sessionStartPlayed ||
                moment == PresenceMoment.SessionOpening ||
                moment == PresenceMoment.WarmReturnGreeting,
            longPauseSpokenThisSequence = tracker.longPauseSpokenThisSequence ||
                moment == PresenceMoment.LongPauseEncouragement
        )
    }

    fun resetSequencePause(tracker: PresenceSessionTracker): PresenceSessionTracker =
        tracker.copy(longPauseSpokenThisSequence = false)

    fun catalogHasPresenceDialogues(): Boolean =
        PresenceMoment.entries.all { moment ->
            DefaultDialogueCatalog.all("en").any { tagFor(moment) in it.contextTags }
        }

    private fun passesMomentGate(
        moment: PresenceMoment,
        context: DialogueContext,
        tracker: PresenceSessionTracker
    ): Boolean = when (moment) {
        PresenceMoment.SessionOpening ->
            !tracker.sessionStartPlayed &&
                !context.returningUser &&
                context.daysSinceLastSession <= 0
        PresenceMoment.WarmReturnGreeting ->
            !tracker.sessionStartPlayed &&
                (context.returningUser || context.daysSinceLastSession > 0)
        PresenceMoment.LongPauseEncouragement ->
            context.idleDurationMs >= LONG_PAUSE_THRESHOLD_MS && !tracker.longPauseSpokenThisSequence
        PresenceMoment.CaregiverReassurance -> context.caregiverVisible
        PresenceMoment.FatigueCheckIn -> context.fatigueSuggested
        PresenceMoment.EmotionalMilestone -> context.celebrationTier >= 2 ||
            context.milestoneType != null
    }

    private fun passesRateLimit(tracker: PresenceSessionTracker, nowMs: Long): Boolean {
        if (tracker.presenceCountThisHour >= MAX_PRESENCE_PER_HOUR) return false
        if (tracker.lastPresenceSpokenMs > 0L &&
            nowMs - tracker.lastPresenceSpokenMs < MIN_SPEAK_INTERVAL_MS
        ) {
            return false
        }
        return true
    }

    private fun tagFor(moment: PresenceMoment): String = when (moment) {
        PresenceMoment.SessionOpening -> "presence_session_open"
        PresenceMoment.WarmReturnGreeting -> "presence_return_warm"
        PresenceMoment.LongPauseEncouragement -> "presence_long_pause"
        PresenceMoment.CaregiverReassurance -> "presence_caregiver_reassure"
        PresenceMoment.FatigueCheckIn -> "presence_fatigue_checkin"
        PresenceMoment.EmotionalMilestone -> "presence_emotional_milestone"
    }
}
