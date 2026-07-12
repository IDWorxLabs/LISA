package com.idworx.lisa

/**
 * Communication timeline stages shown in Everyday Mode.
 */
enum class CommunicationTimelineStage {
    Watching,
    Listening,
    Understanding,
    Confirming,
    Speaking,
    Delivered
}

fun CommunicationTimelineStage.localizedLabel(strings: LisaUiStrings): String = when (this) {
    CommunicationTimelineStage.Watching -> strings.timelineWatching
    CommunicationTimelineStage.Listening -> strings.timelineListening
    CommunicationTimelineStage.Understanding -> strings.timelineUnderstanding
    CommunicationTimelineStage.Confirming -> strings.timelineConfirming
    CommunicationTimelineStage.Speaking -> strings.timelineSpeaking
    CommunicationTimelineStage.Delivered -> strings.timelineDelivered
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

data class EyeTrackingBannerContext(
    val calibrationActive: Boolean = false,
    val trackingLost: Boolean = false,
    val faceDetected: Boolean = false,
    val eyesDetected: Boolean = false
)

fun EyeTrackingBannerContext.bannerMessage(strings: LisaUiStrings): String = when {
    calibrationActive -> strings.eyeTrackingStatusCalibrating
    trackingLost -> strings.eyeTrackingStatusTrackingLost
    !faceDetected -> strings.eyeTrackingStatusNoFace
    !eyesDetected -> strings.eyeTrackingStatusLookAtCamera
    else -> strings.eyeTrackingStatusWatching
}

private fun LisaCommunicationState.toEyeTrackingBannerDisplay(
    strings: LisaUiStrings,
    eyeTrackingBanner: EyeTrackingBannerContext?,
    timelineStage: CommunicationTimelineStage,
    leftWinkDots: Int,
    rightWinkDots: Int
): LisaUserDisplay = LisaUserDisplay(
    headline = eyeTrackingBanner?.bannerMessage(strings) ?: strings.eyeTrackingStatusWatching,
    subtitle = "",
    timelineStage = timelineStage,
    leftWinkDots = leftWinkDots,
    rightWinkDots = rightWinkDots
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
    LisaCommunicationState.BuildingMessage,
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
    strings: LisaUiStrings,
    pendingPhrase: String?,
    countdown: Int?,
    leftWinkDots: Int = 0,
    rightWinkDots: Int = 0,
    eyeTrackingBanner: EyeTrackingBannerContext? = null
): LisaUserDisplay {
    val timelineStage = toTimelineStage()
    return when (this) {
        LisaCommunicationState.WaitingForFace,
        LisaCommunicationState.Ready,
        LisaCommunicationState.Reset,
        LisaCommunicationState.Cancelled,
        LisaCommunicationState.Listening -> toEyeTrackingBannerDisplay(
            strings = strings,
            eyeTrackingBanner = eyeTrackingBanner,
            timelineStage = timelineStage,
            leftWinkDots = leftWinkDots,
            rightWinkDots = rightWinkDots
        )

        LisaCommunicationState.BuildingMessage,
        LisaCommunicationState.LeftWinkDetected,
        LisaCommunicationState.RightWinkDetected,
        is LisaCommunicationState.Sequence -> LisaUserDisplay(
            headline = strings.buildingMessage,
            subtitle = "${strings.buildingLeftDots(leftWinkDots)}\n${strings.buildingRightDots(rightWinkDots)}",
            timelineStage = timelineStage,
            leftWinkDots = leftWinkDots,
            rightWinkDots = rightWinkDots
        )

        LisaCommunicationState.WaitingForNextWink -> LisaUserDisplay(
            headline = strings.waiting,
            subtitle = strings.takeYourTime,
            timelineStage = timelineStage,
            leftWinkDots = leftWinkDots,
            rightWinkDots = rightWinkDots
        )

        is LisaCommunicationState.PossibleMatch -> LisaUserDisplay(
            headline = strings.waiting,
            subtitle = strings.continueYourSequence,
            phrase = phrase,
            timelineStage = timelineStage,
            showIntentPreview = true,
            leftWinkDots = leftWinkDots,
            rightWinkDots = rightWinkDots
        )

        LisaCommunicationState.ProcessingSequence -> LisaUserDisplay(
            headline = strings.processing,
            subtitle = strings.understandingYourMessage,
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
                headline = strings.iUnderstood,
                subtitle = if (countdown != null) strings.speakingIn else "",
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
            headline = strings.speaking,
            subtitle = "",
            phrase = phrase,
            timelineStage = timelineStage,
            showIntentPreview = true
        )

        LisaCommunicationState.MessageDelivered -> LisaUserDisplay(
            headline = strings.messageDelivered,
            subtitle = "",
            timelineStage = timelineStage
        )

        LisaCommunicationState.EmergencyAlarmActive -> LisaUserDisplay(
            headline = strings.emergency,
            subtitle = strings.callingForHelp,
            timelineStage = timelineStage
        )

        LisaCommunicationState.NoPhraseMatched -> LisaUserDisplay(
            headline = strings.noPhraseMatched,
            subtitle = strings.tryAgainPrompt,
            timelineStage = CommunicationTimelineStage.Watching
        )
    }
}

fun winkDots(count: Int): String =
    if (count <= 0) "—" else "●".repeat(count.coerceAtMost(8))

/** Live partial-sequence label for compose-mode feedback (display only). */
fun formatPartialWinkSequence(left: Int, right: Int): String? = when {
    left <= 0 && right <= 0 -> null
    left > 0 && right > 0 -> formatWinkSequenceShort(left, right)
    left > 0 -> "L$left"
    else -> "R$right"
}

data class ComposerEyeFeedback(
    val eyeTrackingBanner: EyeTrackingBannerContext,
    val leftWinkCount: Int,
    val rightWinkCount: Int,
    val sensitivityLevel: Int,
    val responseTimeSec: Int
) {
    fun bannerMessage(strings: LisaUiStrings): String =
        eyeTrackingBanner.bannerMessage(strings)

    fun partialSequenceLabel(): String? =
        formatPartialWinkSequence(leftWinkCount, rightWinkCount)
}
