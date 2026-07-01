package com.idworx.lisa

/**
 * Communication timeline stages shown in Everyday Mode.
 */
enum class CommunicationTimelineStage(val label: String) {
    Watching("Watching"),
    Listening("Listening"),
    Understanding("Understanding"),
    Confirming("Confirming"),
    Speaking("Speaking"),
    Delivered("Delivered")
}

/**
 * User-facing communication display for Everyday Mode.
 * Natural language only — no technical labels.
 */
data class LisaUserDisplay(
    val headline: String,
    val subtitle: String = "",
    val phrase: String? = null,
    val countdown: Int? = null,
    val timelineStage: CommunicationTimelineStage = CommunicationTimelineStage.Watching,
    val showCountdownHints: Boolean = false,
    val showIntentPreview: Boolean = false,
    val leftWinkDots: Int = 0,
    val rightWinkDots: Int = 0
)

fun LisaCommunicationState.toTimelineStage(): CommunicationTimelineStage = when (this) {
    LisaCommunicationState.WaitingForFace,
    LisaCommunicationState.Ready,
    LisaCommunicationState.Reset,
    LisaCommunicationState.Cancelled,
    LisaCommunicationState.NoPhraseMatched -> CommunicationTimelineStage.Watching

    LisaCommunicationState.Listening,
    LisaCommunicationState.LeftWinkDetected,
    LisaCommunicationState.RightWinkDetected,
    is LisaCommunicationState.Sequence,
    LisaCommunicationState.WaitingForNextWink,
    is LisaCommunicationState.PossibleMatch -> CommunicationTimelineStage.Listening

    LisaCommunicationState.ProcessingSequence -> CommunicationTimelineStage.Understanding

    is LisaCommunicationState.Detected,
    is LisaCommunicationState.CountdownConfirm,
    LisaCommunicationState.WaitingForConfirmation -> CommunicationTimelineStage.Confirming

    is LisaCommunicationState.Speaking -> CommunicationTimelineStage.Speaking

    LisaCommunicationState.MessageDelivered -> CommunicationTimelineStage.Delivered

    LisaCommunicationState.EmergencyAlarmActive -> CommunicationTimelineStage.Confirming
}

fun LisaCommunicationState.toUserDisplay(
    pendingPhrase: String?,
    countdown: Int?,
    leftWinkDots: Int = 0,
    rightWinkDots: Int = 0
): LisaUserDisplay {
    val timelineStage = toTimelineStage()
    return when (this) {
        LisaCommunicationState.WaitingForFace,
        LisaCommunicationState.Ready,
        LisaCommunicationState.Reset,
        LisaCommunicationState.Cancelled -> LisaUserDisplay(
            headline = "LISTENING...",
            subtitle = "Watching your eyes...",
            timelineStage = timelineStage,
            leftWinkDots = leftWinkDots,
            rightWinkDots = rightWinkDots
        )

        LisaCommunicationState.Listening,
        LisaCommunicationState.LeftWinkDetected,
        LisaCommunicationState.RightWinkDetected,
        is LisaCommunicationState.Sequence -> LisaUserDisplay(
            headline = "LISTENING...",
            subtitle = "Building your message...",
            timelineStage = timelineStage,
            leftWinkDots = leftWinkDots,
            rightWinkDots = rightWinkDots
        )

        LisaCommunicationState.WaitingForNextWink -> LisaUserDisplay(
            headline = "WAITING...",
            subtitle = "You can continue your sequence...",
            timelineStage = timelineStage,
            leftWinkDots = leftWinkDots,
            rightWinkDots = rightWinkDots
        )

        is LisaCommunicationState.PossibleMatch -> LisaUserDisplay(
            headline = "POSSIBLE MATCH",
            subtitle = "Continue or pause longer",
            phrase = phrase,
            timelineStage = timelineStage,
            showIntentPreview = true,
            leftWinkDots = leftWinkDots,
            rightWinkDots = rightWinkDots
        )

        LisaCommunicationState.ProcessingSequence -> LisaUserDisplay(
            headline = "PROCESSING...",
            subtitle = "Understanding your message...",
            timelineStage = timelineStage,
            leftWinkDots = leftWinkDots,
            rightWinkDots = rightWinkDots
        )

        is LisaCommunicationState.Detected,
        is LisaCommunicationState.CountdownConfirm,
        LisaCommunicationState.WaitingForConfirmation -> {
            val phrase = pendingPhrase
                ?: (this as? LisaCommunicationState.Detected)?.phrase
                ?: (this as? LisaCommunicationState.CountdownConfirm)?.phrase
            LisaUserDisplay(
                headline = "I UNDERSTOOD",
                subtitle = if (countdown != null) "Speaking in" else "",
                phrase = phrase,
                countdown = countdown,
                timelineStage = timelineStage,
                showCountdownHints = countdown != null,
                showIntentPreview = phrase != null,
                leftWinkDots = leftWinkDots,
                rightWinkDots = rightWinkDots
            )
        }

        is LisaCommunicationState.Speaking -> LisaUserDisplay(
            headline = "SPEAKING",
            subtitle = "",
            phrase = phrase,
            timelineStage = timelineStage,
            showIntentPreview = true
        )

        LisaCommunicationState.MessageDelivered -> LisaUserDisplay(
            headline = "MESSAGE DELIVERED",
            subtitle = "",
            timelineStage = timelineStage
        )

        LisaCommunicationState.EmergencyAlarmActive -> LisaUserDisplay(
            headline = "EMERGENCY",
            subtitle = "Calling for help...",
            timelineStage = timelineStage
        )

        LisaCommunicationState.NoPhraseMatched -> LisaUserDisplay(
            headline = "LISTENING...",
            subtitle = "Watching your eyes...",
            timelineStage = CommunicationTimelineStage.Watching
        )
    }
}

fun winkDots(count: Int): String =
    if (count <= 0) "—" else "●".repeat(count.coerceAtMost(8))
