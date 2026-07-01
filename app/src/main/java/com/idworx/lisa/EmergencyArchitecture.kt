package com.idworx.lisa

/**
 * Future caregiver-link architecture (not implemented yet).
 *
 * Design goal: one emergency event on the patient's phone should simultaneously:
 * 1. trigger the local alarm on the disabled user's device
 * 2. notify the linked caregiver's phone via push
 * 3. continue until the caregiver acknowledges the alert
 */

// TODO: Firebase-backed caregiver profile for the patient account.
data class CaregiverProfile(
    val caregiverId: String,
    val displayName: String,
    val phoneNumber: String? = null,
    val fcmToken: String? = null
)

// TODO: Paired caregiver device used for emergency fan-out and acknowledgement.
data class LinkedCaregiverDevice(
    val deviceId: String,
    val caregiverProfile: CaregiverProfile,
    val linkedAtEpochMs: Long,
    val isPrimary: Boolean = false
)

/**
 * Placeholder for future emergency notification pipeline.
 *
 * Future flow:
 * - EmergencyAlarmController.start() triggers local alarm + TTS
 * - EmergencyNotificationService.dispatchEmergency() sends FCM to linked caregivers
 * - Caregiver app receives high-priority push and shows acknowledgement UI
 * - Patient alarm continues until EmergencyNotificationService.onAcknowledged()
 */
object EmergencyNotificationService {

    fun notifyCaregiverPlaceholder(
        profileId: String,
        caregivers: List<LisaCaregiver>,
        sequenceLeft: Int = EMERGENCY_LEFT_WINKS,
        sequenceRight: Int = EMERGENCY_RIGHT_WINKS
    ): List<String> {
        val toNotify = caregivers.filter { it.shouldNotifyOnEmergency() }
        val names = toNotify.map { it.name }

        // TODO: Firebase Cloud Messaging — send high-priority data message per caregiver device token
        // TODO: SMS fallback — dial/send SMS to phoneNumber when emergencyContactEnabled is true
        // TODO: Phone call integration — auto-call primary emergency contacts
        // TODO: Persist emergency event until caregiver acknowledgement is received
        // TODO: Emergency acknowledgement — caregiver taps ACK → stop patient alarm remotely

        android.util.Log.i(
            TAG,
            "Emergency L$sequenceLeft R$sequenceRight for profile $profileId — would notify: $names"
        )

        return names
    }

    fun onCaregiverAcknowledgedPlaceholder(emergencyEventId: String) {
        // TODO: Mark event acknowledged and signal patient device to exit emergency mode
    }

    private const val TAG = "LisaEmergency"
}

/** L6 R0 — unique pattern with no prefix overlap with other built-in phrases. */
const val EMERGENCY_LEFT_WINKS = 6
const val EMERGENCY_RIGHT_WINKS = 0

fun isEmergencySequence(left: Int, right: Int): Boolean =
    left == EMERGENCY_LEFT_WINKS && right == EMERGENCY_RIGHT_WINKS
