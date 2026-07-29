package com.idworx.lisa.features.guidedemergencylesson

/**
 * RC8.27 — explicit Lesson 22 emergency practice phase model.
 *
 * Trigger: L6 R0
 * Confirm (armed): L1 R1
 * Cancel (armed): R1 L1
 * Stop (active): L1 R1
 *
 * Completion requires stopping the real active alarm with L1 R1. Arming, confirming, or merely
 * showing Emergency Active must never complete the lesson. Cancelling while only armed is safe
 * and returns to [AwaitEmergencyTrigger] without completing.
 */
object GuidedEmergencyLessonAuthority {

    const val ID_EMERGENCY: String = "nav_emergency"

    const val TRIGGER_SEQUENCE: String = "L6 R0"
    const val CONFIRM_SEQUENCE: String = "L1 R1"
    const val CANCEL_WHILE_ARMED_SEQUENCE: String = "R1 L1"
    const val STOP_WHILE_ACTIVE_SEQUENCE: String = "L1 R1"

    enum class Phase {
        AwaitEmergencyTrigger,
        AwaitEmergencyConfirmation,
        EmergencyActive,
        AwaitEmergencyStop,
        Completed
    }

    fun phase(
        emergencyAwaitingConfirm: Boolean,
        emergencyActive: Boolean,
        lessonCompleted: Boolean = false
    ): Phase = when {
        lessonCompleted -> Phase.Completed
        emergencyActive -> Phase.AwaitEmergencyStop
        emergencyAwaitingConfirm -> Phase.AwaitEmergencyConfirmation
        else -> Phase.AwaitEmergencyTrigger
    }

    /** True once the active alarm has actually been stopped (Stage 4). */
    fun mayCompleteAfterStop(
        wasEmergencyActive: Boolean,
        isEmergencyActiveNow: Boolean
    ): Boolean = wasEmergencyActive && !isEmergencyActiveNow

    fun isPreExecuteForbiddenAtEntry(
        emergencyAwaitingConfirm: Boolean,
        emergencyActive: Boolean
    ): Boolean = !emergencyAwaitingConfirm && !emergencyActive

    /** Active-alarm stop is L1 R1 (confirm order), never R1 L1. */
    fun isActiveStopSequence(left: Int, right: Int, blinkOrder: List<Boolean>): Boolean =
        com.idworx.lisa.features.brain1interactionstandard.model.UniversalInteractionGestures
            .isConfirm(left, right, blinkOrder)
}
