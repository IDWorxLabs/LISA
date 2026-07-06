package com.idworx.lisa.features.experiencepolish.emotionalpresence.model

import com.idworx.lisa.features.personality.model.PresenceMoment

/** Tracks emotional presence speech to prevent overtalking. */
data class PresenceSessionTracker(
    val lastPresenceSpokenMs: Long = 0L,
    val lastMoment: PresenceMoment? = null,
    val presenceCountThisHour: Int = 0,
    val hourWindowStartMs: Long = 0L,
    /** Session-opening or warm-return already played this visit. */
    val sessionStartPlayed: Boolean = false,
    val longPauseSpokenThisSequence: Boolean = false
)
