package com.idworx.lisa

/**
 * Visible communication states shown as a small white camera overlay.
 * Users should always know what LISA is doing.
 */
sealed class LisaCommunicationState(val displayText: String) {

    data object WaitingForFace : LisaCommunicationState("Waiting for face")
    data object Ready : LisaCommunicationState("Ready")
    data object Listening : LisaCommunicationState("Listening...")
    data object LeftWinkDetected : LisaCommunicationState("Left wink detected")
    data object RightWinkDetected : LisaCommunicationState("Right wink detected")
    data object BuildingMessage : LisaCommunicationState("Building message")
    data object WaitingForNextWink : LisaCommunicationState("Waiting for next wink...")

    data class PossibleMatch(val phrase: String) : LisaCommunicationState("Possible match")
    data object ProcessingSequence : LisaCommunicationState("Processing sequence...")
    data object NoPhraseMatched : LisaCommunicationState("No phrase matched")
    data object WaitingForConfirmation : LisaCommunicationState("Waiting for confirmation")
    data object Cancelled : LisaCommunicationState("Cancelled")

    data class CountdownConfirm(val phrase: String) :
        LisaCommunicationState("Countdown confirm")
    data object Reset : LisaCommunicationState("Reset")
    data object EmergencyAlarmActive : LisaCommunicationState("Emergency Alarm Active")

    data class Detected(val phrase: String) :
        LisaCommunicationState("Detected: $phrase")

    data class Sequence(val left: Int, val right: Int) :
        LisaCommunicationState("Sequence: L$left R$right")

    data class Speaking(val phrase: String) :
        LisaCommunicationState("Speaking: $phrase")

    data object MessageDelivered : LisaCommunicationState("Message delivered")
}
