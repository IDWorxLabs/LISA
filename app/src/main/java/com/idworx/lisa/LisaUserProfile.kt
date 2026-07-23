package com.idworx.lisa

import com.idworx.lisa.features.intelligentstartup.model.ProfileEyeCalibration
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class CommunicationLevel(val label: String) {
    Beginner("Beginner"),
    Standard("Standard"),
    Advanced("Advanced");

    companion object {
        fun fromStored(value: String): CommunicationLevel =
            entries.find { it.name == value || it.label == value } ?: Beginner
    }
}

enum class PreferredLanguage(val label: String) {
    English("English"),
    Afrikaans("Afrikaans"),
    IsiZulu("isiZulu");

    companion object {
        val selectable: List<PreferredLanguage> = entries.toList()

        fun fromStored(value: String): PreferredLanguage = when {
            value.equals("English", ignoreCase = true) -> English
            value.equals("Afrikaans", ignoreCase = true) -> Afrikaans
            value.equals("isiZulu", ignoreCase = true) || value.equals("IsiZulu", ignoreCase = true) -> IsiZulu
            else -> entries.find { it.name == value || it.label.equals(value, ignoreCase = true) } ?: English
        }
    }
}

data class LisaUserProfile(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val preferredLanguage: PreferredLanguage = PreferredLanguage.English,
    val communicationLevel: CommunicationLevel = CommunicationLevel.Beginner,
    val sensitivityLevel: Int = DEFAULT_SENSITIVITY_LEVEL,
    val textSizeScale: Float = 1.0f,
    val confirmationCountdownSec: Int = 3,
    val responseSpeed: ResponseSpeed = ResponseSpeed.default,
    val sequenceProcessingDelaySec: Int = SequenceProcessingDelay.DEFAULT_SECONDS,
    val sequenceTimeoutSec: Float = SequenceProcessingDelay.DEFAULT_SECONDS.toFloat(),
    val emergencyVolume: Float = 1.0f,
    /** Communication TTS speaking volume level (1–10). Canonical source for Speech Volume. */
    val speechVolumeLevel: Int = SpeechVolumeAuthority.DEFAULT_LEVEL,
    /** Communication TTS speech-rate level (1–5). Canonical source for Speech Speed. */
    val speechRateLevel: Int = SpeechSpeedAuthority.DEFAULT_LEVEL,
    val developerMode: Boolean = false,
    val selectedTtsVoiceName: String? = null,
    /** RC7D.34 — local per-profile Quick Eye Calibration payload. */
    val eyeCalibration: ProfileEyeCalibration? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toSettingsUiState(): LisaSettingsUiState = LisaSettingsUiState(
        sensitivityLevel = sensitivityLevel,
        textSizeScale = textSizeScale,
        countdownDurationSec = confirmationCountdownSec,
        responseSpeed = responseSpeed,
        sequenceProcessingDelaySec = sequenceProcessingDelaySec,
        sequenceIdleTimeoutSec = sequenceProcessingDelaySec.toFloat(),
        emergencyAlarmVolume = emergencyVolume,
        developerMode = developerMode
    )

    fun withUpdatedSettings(settings: LisaSettingsUiState): LisaUserProfile = copy(
        sensitivityLevel = settings.sensitivityLevel,
        textSizeScale = settings.textSizeScale,
        confirmationCountdownSec = settings.countdownDurationSec,
        responseSpeed = settings.responseSpeed,
        sequenceProcessingDelaySec = settings.sequenceProcessingDelaySec,
        sequenceTimeoutSec = settings.sequenceProcessingDelaySec.toFloat(),
        emergencyVolume = settings.emergencyAlarmVolume,
        developerMode = settings.developerMode,
        updatedAt = System.currentTimeMillis()
    )

    fun withCommunicationLevel(level: CommunicationLevel): LisaUserProfile {
        val defaults = profileDefaultsForLevel(level, sensitivityLevel)
        return copy(
            communicationLevel = level,
            sensitivityLevel = defaults.sensitivityLevel,
            textSizeScale = defaults.textSizeScale,
            confirmationCountdownSec = defaults.confirmationCountdownSec,
            responseSpeed = defaults.responseSpeed,
            sequenceTimeoutSec = defaults.responseSpeed.idleTimeoutMs / 1000f,
            emergencyVolume = defaults.emergencyVolume,
            updatedAt = System.currentTimeMillis()
        )
    }

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("preferredLanguage", preferredLanguage.name)
        put("communicationLevel", communicationLevel.name)
        put("sensitivityLevel", sensitivityLevel)
        put("textSizeScale", textSizeScale.toDouble())
        put("confirmationCountdownSec", confirmationCountdownSec)
        put("responseSpeed", responseSpeed.name)
        put("sequenceProcessingDelaySec", sequenceProcessingDelaySec)
        put("sequenceTimeoutSec", sequenceProcessingDelaySec.toDouble())
        put("emergencyVolume", emergencyVolume.toDouble())
        put("speechVolumeLevel", speechVolumeLevel)
        put("speechRateLevel", speechRateLevel)
        put("developerMode", developerMode)
        if (selectedTtsVoiceName != null) {
            put("selectedTtsVoiceName", selectedTtsVoiceName)
        }
        eyeCalibration?.let { cal ->
            put(
                "eyeCalibration",
                JSONObject().apply {
                    put("leftClosedEyeThreshold", cal.leftClosedEyeThreshold.toDouble())
                    put("rightClosedEyeThreshold", cal.rightClosedEyeThreshold.toDouble())
                    put("openEyeThreshold", cal.openEyeThreshold.toDouble())
                    put("blinkDurationMs", cal.blinkDurationMs)
                    put("requiredWinkFrames", cal.requiredWinkFrames)
                    put("eyeOpennessBaseline", cal.eyeOpennessBaseline.toDouble())
                    put("faceDistanceProxy", cal.faceDistanceProxy.toDouble())
                    put("eyeSpacingProxy", cal.eyeSpacingProxy.toDouble())
                    put("confidence", cal.confidence.toDouble())
                    put("calibratedAtMs", cal.calibratedAtMs)
                    put(
                        "compatibilityHistory",
                        JSONArray().apply {
                            cal.compatibilityHistory.forEach { record ->
                                put(
                                    JSONObject().apply {
                                        put("level", record.level.name)
                                        put("score", record.score.toDouble())
                                        put("evaluatedAtMs", record.evaluatedAtMs)
                                    }
                                )
                            }
                        }
                    )
                }
            )
        }
        put("createdAt", createdAt)
        put("updatedAt", updatedAt)
    }

    companion object {
        fun fromJson(obj: JSONObject): LisaUserProfile = LisaUserProfile(
            id = obj.getString("id"),
            name = obj.getString("name"),
            preferredLanguage = PreferredLanguage.fromStored(obj.optString("preferredLanguage", "English")),
            communicationLevel = CommunicationLevel.fromStored(obj.optString("communicationLevel", "Beginner")),
            sensitivityLevel = obj.optInt("sensitivityLevel", DEFAULT_SENSITIVITY_LEVEL)
                .coerceIn(MIN_SENSITIVITY_LEVEL, MAX_SENSITIVITY_LEVEL),
            textSizeScale = obj.optDouble("textSizeScale", 1.0).toFloat().coerceIn(0.8f, 1.4f),
            confirmationCountdownSec = obj.optInt("confirmationCountdownSec", 3).coerceIn(2, 5),
            responseSpeed = ResponseSpeed.fromStored(
                stored = obj.optString("responseSpeed").takeIf { it.isNotBlank() },
                legacyTimeoutSec = obj.optDouble("sequenceTimeoutSec", SequenceProcessingDelay.DEFAULT_SECONDS.toDouble()).toFloat()
            ),
            sequenceProcessingDelaySec = SequenceProcessingDelay.fromStored(
                value = obj.optInt("sequenceProcessingDelaySec", -1),
                legacySpeed = ResponseSpeed.fromStored(
                    stored = obj.optString("responseSpeed").takeIf { it.isNotBlank() },
                    legacyTimeoutSec = obj.optDouble("sequenceTimeoutSec", SequenceProcessingDelay.DEFAULT_SECONDS.toDouble()).toFloat()
                )
            ),
            sequenceTimeoutSec = SequenceProcessingDelay.fromStored(
                value = obj.optInt("sequenceProcessingDelaySec", -1),
                legacySpeed = ResponseSpeed.fromStored(
                    stored = obj.optString("responseSpeed").takeIf { it.isNotBlank() },
                    legacyTimeoutSec = obj.optDouble("sequenceTimeoutSec", SequenceProcessingDelay.DEFAULT_SECONDS.toDouble()).toFloat()
                )
            ).toFloat(),
            emergencyVolume = obj.optDouble("emergencyVolume", 1.0).toFloat().coerceIn(0.5f, 1f),
            speechVolumeLevel = SpeechVolumeAuthority.coerce(
                obj.optInt("speechVolumeLevel", SpeechVolumeAuthority.DEFAULT_LEVEL)
            ),
            speechRateLevel = SpeechSpeedAuthority.coerce(
                obj.optInt("speechRateLevel", SpeechSpeedAuthority.DEFAULT_LEVEL)
            ),
            developerMode = obj.optBoolean("developerMode", false),
            selectedTtsVoiceName = obj.optString("selectedTtsVoiceName").takeIf { it.isNotBlank() },
            eyeCalibration = obj.optJSONObject("eyeCalibration")?.let { cal ->
                ProfileEyeCalibration(
                    leftClosedEyeThreshold = cal.optDouble("leftClosedEyeThreshold", 0.25).toFloat(),
                    rightClosedEyeThreshold = cal.optDouble("rightClosedEyeThreshold", 0.25).toFloat(),
                    openEyeThreshold = cal.optDouble("openEyeThreshold", 0.75).toFloat(),
                    blinkDurationMs = cal.optLong("blinkDurationMs", 160L),
                    requiredWinkFrames = cal.optInt("requiredWinkFrames", 2),
                    eyeOpennessBaseline = cal.optDouble("eyeOpennessBaseline", 0.8).toFloat(),
                    faceDistanceProxy = cal.optDouble("faceDistanceProxy", 0.35).toFloat(),
                    eyeSpacingProxy = cal.optDouble("eyeSpacingProxy", 0.35).toFloat(),
                    confidence = cal.optDouble("confidence", 0.0).toFloat(),
                    calibratedAtMs = cal.optLong("calibratedAtMs", 0L),
                    compatibilityHistory = cal.optJSONArray("compatibilityHistory")?.let { arr ->
                        buildList {
                            for (i in 0 until arr.length()) {
                                val item = arr.optJSONObject(i) ?: continue
                                val levelName = item.optString("level", "Low")
                                val level = runCatching {
                                    com.idworx.lisa.features.intelligentstartup.model.CalibrationCompatibilityLevel.valueOf(levelName)
                                }.getOrDefault(
                                    com.idworx.lisa.features.intelligentstartup.model.CalibrationCompatibilityLevel.Low
                                )
                                add(
                                    com.idworx.lisa.features.intelligentstartup.model.CalibrationCompatibilityRecord(
                                        level = level,
                                        score = item.optDouble("score", 0.0).toFloat(),
                                        evaluatedAtMs = item.optLong("evaluatedAtMs", 0L)
                                    )
                                )
                            }
                        }
                    } ?: emptyList()
                )
            },
            createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
            updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
        )

        fun createNew(name: String, template: LisaUserProfile? = null): LisaUserProfile {
            val now = System.currentTimeMillis()
            return if (template != null) {
                template.copy(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    eyeCalibration = null,
                    createdAt = now,
                    updatedAt = now
                )
            } else {
                LisaUserProfile(
                    name = name,
                    createdAt = now,
                    updatedAt = now
                )
            }
        }
    }
}

data class ProfileLevelDefaults(
    val sensitivityLevel: Int,
    val textSizeScale: Float,
    val confirmationCountdownSec: Int,
    val responseSpeed: ResponseSpeed,
    val emergencyVolume: Float
)

fun profileDefaultsForLevel(
    level: CommunicationLevel,
    sensitivityOverride: Int? = null
): ProfileLevelDefaults = when (level) {
    CommunicationLevel.Beginner -> ProfileLevelDefaults(
        sensitivityLevel = sensitivityOverride?.coerceIn(MIN_SENSITIVITY_LEVEL, MAX_SENSITIVITY_LEVEL) ?: 2,
        textSizeScale = 1.1f,
        confirmationCountdownSec = 5,
        responseSpeed = ResponseSpeed.Slow,
        emergencyVolume = 1.0f
    )
    CommunicationLevel.Standard -> ProfileLevelDefaults(
        sensitivityLevel = sensitivityOverride?.coerceIn(MIN_SENSITIVITY_LEVEL, MAX_SENSITIVITY_LEVEL)
            ?: DEFAULT_SENSITIVITY_LEVEL,
        textSizeScale = 1.0f,
        confirmationCountdownSec = 3,
        responseSpeed = ResponseSpeed.Normal,
        emergencyVolume = 1.0f
    )
    CommunicationLevel.Advanced -> ProfileLevelDefaults(
        sensitivityLevel = sensitivityOverride?.coerceIn(MIN_SENSITIVITY_LEVEL, MAX_SENSITIVITY_LEVEL) ?: 4,
        textSizeScale = 0.95f,
        confirmationCountdownSec = 2,
        responseSpeed = ResponseSpeed.Fast,
        emergencyVolume = 0.85f
    )
}

data class LisaProfileState(
    val profiles: List<LisaUserProfile>,
    val activeProfileId: String
) {
    val activeProfile: LisaUserProfile?
        get() = profiles.find { it.id == activeProfileId } ?: profiles.firstOrNull()
}

class LisaProfileStore(private val context: android.content.Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)

    fun loadProfiles(): List<LisaUserProfile> {
        val raw = prefs.getString(KEY_PROFILES_JSON, null) ?: return emptyList()
        if (raw.isBlank()) return emptyList()
        return try {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    add(LisaUserProfile.fromJson(array.getJSONObject(i)))
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun loadActiveProfileId(): String? =
        prefs.getString(KEY_ACTIVE_PROFILE_ID, null)?.takeIf { it.isNotBlank() }

    fun saveProfiles(profiles: List<LisaUserProfile>, activeProfileId: String) {
        val array = JSONArray()
        profiles.forEach { array.put(it.toJson()) }
        prefs.edit()
            .putString(KEY_PROFILES_JSON, array.toString())
            .putString(KEY_ACTIVE_PROFILE_ID, activeProfileId)
            .apply()
    }

    fun load(legacySensitivity: Int, legacyDeveloperMode: Boolean): LisaProfileState {
        val (profiles, active) = ensureDefaultProfile(legacySensitivity, legacyDeveloperMode)
        return LisaProfileState(profiles, active.id)
    }

    fun ensureDefaultProfile(
        legacySensitivity: Int,
        legacyDeveloperMode: Boolean
    ): Pair<List<LisaUserProfile>, LisaUserProfile> {
        val existing = loadProfiles()
        if (existing.isNotEmpty()) {
            val activeId = loadActiveProfileId()
            val active = existing.find { it.id == activeId } ?: existing.first()
            if (activeId == null || existing.none { it.id == activeId }) {
                saveProfiles(existing, active.id)
            }
            return existing to active
        }

        val defaultProfile = LisaUserProfile(
            name = "Primary User",
            preferredLanguage = PreferredLanguage.English,
            communicationLevel = CommunicationLevel.Beginner,
            sensitivityLevel = legacySensitivity.coerceIn(MIN_SENSITIVITY_LEVEL, MAX_SENSITIVITY_LEVEL),
            developerMode = legacyDeveloperMode
        )
        saveProfiles(listOf(defaultProfile), defaultProfile.id)
        return listOf(defaultProfile) to defaultProfile
    }

    companion object {
        const val PREFS_NAME = "lisa_prefs"
        private const val KEY_PROFILES_JSON = "profiles_json"
        private const val KEY_ACTIVE_PROFILE_ID = "active_profile_id"
    }
}
