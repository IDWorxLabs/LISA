package com.idworx.lisa

import com.idworx.lisa.features.blinkdetectionreliability.BlinkDetectionTuning
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Launch readiness — sensitivity 5 is the true default; saved values are preserved. */
class DefaultSensitivityLaunchTest {

    @Test
    fun defaultSensitivityConstantIsFive() {
        assertEquals(5, DEFAULT_SENSITIVITY_LEVEL)
        assertEquals(1, MIN_SENSITIVITY_LEVEL)
        assertEquals(10, MAX_SENSITIVITY_LEVEL)
    }

    @Test
    fun freshProfile_defaultsToSensitivityFive() {
        val profile = LisaUserProfile(name = "Fresh")
        assertEquals(5, profile.sensitivityLevel)
        assertEquals(DEFAULT_SENSITIVITY_LEVEL, profile.sensitivityLevel)
    }

    @Test
    fun createNewProfile_defaultsToSensitivityFive() {
        val created = LisaUserProfile.createNew("Primary")
        assertEquals(DEFAULT_SENSITIVITY_LEVEL, created.sensitivityLevel)
    }

    @Test
    fun communicationLevelDefaults_useSensitivityFiveWhenNoOverride() {
        CommunicationLevel.entries.forEach { level ->
            val defaults = profileDefaultsForLevel(level)
            assertEquals(
                "Expected default sensitivity 5 for $level",
                DEFAULT_SENSITIVITY_LEVEL,
                defaults.sensitivityLevel
            )
        }
    }

    @Test
    fun missingSensitivityInJson_fallsBackToFive() {
        val json = JSONObject().apply {
            put("id", "legacy")
            put("name", "Legacy User")
            // deliberately omit sensitivityLevel
        }
        val restored = LisaUserProfile.fromJson(json)
        assertEquals(DEFAULT_SENSITIVITY_LEVEL, restored.sensitivityLevel)
    }

    @Test
    fun savedSensitivityOtherThanFive_isPreserved() {
        val saved = LisaUserProfile(name = "Saved", sensitivityLevel = 7)
        val roundTrip = LisaUserProfile.fromJson(saved.toJson())
        assertEquals(7, roundTrip.sensitivityLevel)
        assertFalse(roundTrip.sensitivityLevel == DEFAULT_SENSITIVITY_LEVEL)
    }

    @Test
    fun settingsUiState_defaultsToSensitivityFive() {
        assertEquals(DEFAULT_SENSITIVITY_LEVEL, LisaSettingsUiState().sensitivityLevel)
    }

    @Test
    fun blinkDetectionTuningDefault_matchesSensitivityFive() {
        val fromDefault = BlinkDetectionTuning.default
        val fromLevelFive = BlinkDetectionTuning.forSensitivityLevel(5)
        assertEquals(fromLevelFive.closedEyeThreshold, fromDefault.closedEyeThreshold, 0f)
        assertEquals(fromLevelFive.openEyeThreshold, fromDefault.openEyeThreshold, 0f)
        // Range / adjustment behaviour unchanged
        val min = BlinkDetectionTuning.forSensitivityLevel(MIN_SENSITIVITY_LEVEL)
        val max = BlinkDetectionTuning.forSensitivityLevel(MAX_SENSITIVITY_LEVEL)
        assertTrue(min.closedEyeThreshold != max.closedEyeThreshold || min.openEyeThreshold != max.openEyeThreshold)
    }

    @Test
    fun withCommunicationLevel_doesNotForceAwayFromExplicitOverride() {
        val profile = LisaUserProfile(name = "P", sensitivityLevel = 8)
        val updated = profile.withCommunicationLevel(CommunicationLevel.Beginner)
        // withCommunicationLevel uses profileDefaultsForLevel(level, sensitivityLevel) —
        // current sensitivity is passed as override, so 8 is preserved.
        assertEquals(8, updated.sensitivityLevel)
    }
}
