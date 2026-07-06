package com.idworx.lisa

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class LisaFeedbackEntry(
    val id: String = UUID.randomUUID().toString(),
    val whatWorkedWell: String,
    val whatWasConfusing: String,
    val winkDetectionFeedback: String,
    val speechTimingFeedback: String,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("whatWorkedWell", whatWorkedWell)
        put("whatWasConfusing", whatWasConfusing)
        put("winkDetectionFeedback", winkDetectionFeedback)
        put("speechTimingFeedback", speechTimingFeedback)
        put("createdAt", createdAt)
    }

    companion object {
        fun fromJson(obj: JSONObject): LisaFeedbackEntry = LisaFeedbackEntry(
            id = obj.getString("id"),
            whatWorkedWell = obj.optString("whatWorkedWell", ""),
            whatWasConfusing = obj.optString("whatWasConfusing", ""),
            winkDetectionFeedback = obj.optString("winkDetectionFeedback", ""),
            speechTimingFeedback = obj.optString("speechTimingFeedback", ""),
            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
        )
    }
}

enum class TestingChecklistItem(val key: String, val label: String) {
    CameraOpens("camera_opens", "Camera opens"),
    FaceDetected("face_detected", "Face detected"),
    LeftWinkDetected("left_wink_detected", "Left wink detected correctly"),
    RightWinkDetected("right_wink_detected", "Right wink detected correctly"),
    YesSequenceWorks("yes_sequence_works", "Yes sequence works"),
    NoSequenceWorks("no_sequence_works", "No sequence works"),
    EmergencyAlarmTested("emergency_alarm_tested", "Emergency alarm tested"),
    ResetStopsAlarm("reset_stops_alarm", "Reset stops alarm"),
    CaregiverKnowsTestBuild("caregiver_knows_test_build", "Caregiver knows this is a test build")
}

class LisaReleaseStore(context: Context) {
    private val prefs = context.getSharedPreferences(LisaProfileStore.PREFS_NAME, Context.MODE_PRIVATE)

    fun isOnboardingCompleted(): Boolean =
        prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)

    fun setOnboardingCompleted(completed: Boolean = true) {
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, completed).apply()
    }

    fun isWorkspaceEntryIntroCompleted(): Boolean =
        prefs.getBoolean(KEY_WORKSPACE_ENTRY_INTRO, false)

    fun setWorkspaceEntryIntroCompleted(completed: Boolean = true) {
        prefs.edit().putBoolean(KEY_WORKSPACE_ENTRY_INTRO, completed).apply()
    }

    fun wasCameraPermissionRequested(): Boolean =
        prefs.getBoolean(KEY_CAMERA_REQUESTED_ONCE, false)

    fun markCameraPermissionRequested() {
        prefs.edit().putBoolean(KEY_CAMERA_REQUESTED_ONCE, true).apply()
    }

    fun loadChecklist(): Map<String, Boolean> {
        val raw = prefs.getString(KEY_TESTING_CHECKLIST, null) ?: return emptyMap()
        return try {
            val obj = JSONObject(raw)
            buildMap {
                TestingChecklistItem.entries.forEach { item ->
                    put(item.key, obj.optBoolean(item.key, false))
                }
            }
        } catch (_: Exception) {
            emptyMap()
        }
    }

    fun saveChecklistItem(key: String, checked: Boolean) {
        val current = try {
            val raw = prefs.getString(KEY_TESTING_CHECKLIST, null)
            if (raw.isNullOrBlank()) JSONObject() else JSONObject(raw)
        } catch (_: Exception) {
            JSONObject()
        }
        current.put(key, checked)
        prefs.edit().putString(KEY_TESTING_CHECKLIST, current.toString()).apply()
    }

    fun saveFeedback(entry: LisaFeedbackEntry) {
        val all = loadFeedback().toMutableList()
        all.add(0, entry)
        val array = JSONArray()
        all.forEach { array.put(it.toJson()) }
        prefs.edit().putString(KEY_FEEDBACK_JSON, array.toString()).apply()
    }

    fun loadFeedback(): List<LisaFeedbackEntry> {
        val raw = prefs.getString(KEY_FEEDBACK_JSON, null) ?: return emptyList()
        if (raw.isBlank()) return emptyList()
        return try {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    add(LisaFeedbackEntry.fromJson(array.getJSONObject(i)))
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    companion object {
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        private const val KEY_WORKSPACE_ENTRY_INTRO = "workspace_entry_intro_completed"
        private const val KEY_CAMERA_REQUESTED_ONCE = "camera_requested_once"
        private const val KEY_TESTING_CHECKLIST = "testing_checklist"
        private const val KEY_FEEDBACK_JSON = "feedback_json"
    }
}
