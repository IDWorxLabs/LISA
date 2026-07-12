package com.idworx.lisa

/**
 * Emergency gesture constants and Version 2 caregiver-notification design notes.
 *
 * Version 1 emergency behaviour is local only:
 * L6 R0 → alarm → spoken emergency message → emergency overlay.
 */

/** L6 R0 — unique pattern with no prefix overlap with other built-in phrases. */
const val EMERGENCY_LEFT_WINKS = 6
const val EMERGENCY_RIGHT_WINKS = 0

fun isEmergencySequence(left: Int, right: Int): Boolean =
    left == EMERGENCY_LEFT_WINKS && right == EMERGENCY_RIGHT_WINKS

// --- Version 2 foundation (not wired in Version 1) ---

/** Future Firebase-backed caregiver profile for remote emergency notification. */
data class CaregiverProfile(
    val caregiverId: String,
    val displayName: String,
    val phoneNumber: String? = null,
    val fcmToken: String? = null
)

/** Future paired caregiver device used for emergency fan-out and acknowledgement. */
data class LinkedCaregiverDevice(
    val deviceId: String,
    val caregiverProfile: CaregiverProfile,
    val linkedAtEpochMs: Long,
    val isPrimary: Boolean = false
)
