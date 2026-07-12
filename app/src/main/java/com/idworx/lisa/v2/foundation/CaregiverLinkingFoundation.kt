package com.idworx.lisa.v2.foundation

import android.content.Context
import com.idworx.lisa.LisaProfileStore
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Version 2 foundation — caregiver linking models and local storage.
 *
 * Not loaded or executed by Version 1 runtime. Retained for a future caregiver
 * notification and linking feature without affecting current emergency behaviour.
 */
enum class CaregiverRelationship(val label: String) {
    Caregiver("Caregiver"),
    Family("Family"),
    Friend("Friend"),
    Nurse("Nurse"),
    Doctor("Doctor"),
    Other("Other");

    companion object {
        fun fromStored(value: String): CaregiverRelationship =
            entries.find { it.name == value || it.label.equals(value, ignoreCase = true) } ?: Caregiver
    }
}

data class LisaCaregiver(
    val id: String = UUID.randomUUID().toString(),
    val profileId: String,
    val name: String,
    val relationship: CaregiverRelationship = CaregiverRelationship.Caregiver,
    val phoneNumber: String = "",
    val email: String = "",
    val emergencyContactEnabled: Boolean = true,
    val canEditVocabulary: Boolean = false,
    val canChangeSettings: Boolean = false,
    val canReceiveEmergencyAlerts: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun shouldNotifyOnEmergency(): Boolean =
        canReceiveEmergencyAlerts || emergencyContactEnabled

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("profileId", profileId)
        put("name", name)
        put("relationship", relationship.name)
        put("phoneNumber", phoneNumber)
        put("email", email)
        put("emergencyContactEnabled", emergencyContactEnabled)
        put("canEditVocabulary", canEditVocabulary)
        put("canChangeSettings", canChangeSettings)
        put("canReceiveEmergencyAlerts", canReceiveEmergencyAlerts)
        put("createdAt", createdAt)
        put("updatedAt", updatedAt)
    }

    companion object {
        fun fromJson(obj: JSONObject): LisaCaregiver = LisaCaregiver(
            id = obj.getString("id"),
            profileId = obj.getString("profileId"),
            name = obj.getString("name"),
            relationship = CaregiverRelationship.fromStored(obj.optString("relationship", "Caregiver")),
            phoneNumber = obj.optString("phoneNumber", ""),
            email = obj.optString("email", ""),
            emergencyContactEnabled = obj.optBoolean("emergencyContactEnabled", true),
            canEditVocabulary = obj.optBoolean("canEditVocabulary", false),
            canChangeSettings = obj.optBoolean("canChangeSettings", false),
            canReceiveEmergencyAlerts = obj.optBoolean("canReceiveEmergencyAlerts", true),
            createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
            updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
        )

        fun createNew(profileId: String, name: String): LisaCaregiver {
            val now = System.currentTimeMillis()
            return LisaCaregiver(
                profileId = profileId,
                name = name.trim().ifBlank { "New caregiver" },
                createdAt = now,
                updatedAt = now
            )
        }
    }
}

class LisaCaregiverStore(context: Context) {
    private val prefs = context.getSharedPreferences(LisaProfileStore.PREFS_NAME, Context.MODE_PRIVATE)

    fun loadAll(): List<LisaCaregiver> {
        val raw = prefs.getString(KEY_CAREGIVERS_JSON, null) ?: return emptyList()
        if (raw.isBlank()) return emptyList()
        return try {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    add(LisaCaregiver.fromJson(array.getJSONObject(i)))
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun loadForProfile(profileId: String): List<LisaCaregiver> =
        loadAll().filter { it.profileId == profileId }

    fun saveAll(caregivers: List<LisaCaregiver>) {
        val array = JSONArray()
        caregivers.forEach { array.put(it.toJson()) }
        prefs.edit().putString(KEY_CAREGIVERS_JSON, array.toString()).apply()
    }

    fun upsert(caregiver: LisaCaregiver) {
        val all = loadAll().toMutableList()
        val index = all.indexOfFirst { it.id == caregiver.id }
        if (index >= 0) {
            all[index] = caregiver
        } else {
            all.add(caregiver)
        }
        saveAll(all)
    }

    fun delete(caregiverId: String) {
        saveAll(loadAll().filterNot { it.id == caregiverId })
    }

    companion object {
        private const val KEY_CAREGIVERS_JSON = "caregivers_json"
    }
}
