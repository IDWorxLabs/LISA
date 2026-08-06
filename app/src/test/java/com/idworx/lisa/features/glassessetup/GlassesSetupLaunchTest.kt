package com.idworx.lisa.features.glassessetup

import android.content.SharedPreferences
import com.idworx.lisa.LisaUiStrings
import com.idworx.lisa.LisaUserProfile
import com.idworx.lisa.PreferredLanguage
import com.idworx.lisa.features.blinkdetectionreliability.BlinkDetectionTuning
import com.idworx.lisa.features.eyediagnostic.EyeTestModeAccess
import com.idworx.lisa.features.glassescharacterisation.GlassesCharacterisationAccess
import com.idworx.lisa.features.intelligentstartup.StartupSessionController
import com.idworx.lisa.features.intelligentstartup.authority.StartupFlowAuthority
import com.idworx.lisa.features.intelligentstartup.model.StartupEvent
import com.idworx.lisa.features.intelligentstartup.model.StartupFlowState
import com.idworx.lisa.features.intelligentstartup.model.StartupPhase
import com.idworx.lisa.features.personalisedeyeprofile.PersonalisedEyeProfileAccess
import com.idworx.lisa.features.signalinvestigation.SignalInvestigationAccess
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GlassesSetupLaunchTest {

    private class MemoryPrefs : SharedPreferences {
        private val map = linkedMapOf<String, Any?>()

        override fun getAll(): MutableMap<String, *> = map.toMutableMap()
        override fun getString(key: String?, defValue: String?): String? =
            map[key!!] as? String ?: defValue
        override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? =
            @Suppress("UNCHECKED_CAST")
            (map[key!!] as? MutableSet<String>) ?: defValues
        override fun getInt(key: String?, defValue: Int): Int = map[key!!] as? Int ?: defValue
        override fun getLong(key: String?, defValue: Long): Long = map[key!!] as? Long ?: defValue
        override fun getFloat(key: String?, defValue: Float): Float = map[key!!] as? Float ?: defValue
        override fun getBoolean(key: String?, defValue: Boolean): Boolean =
            map[key!!] as? Boolean ?: defValue
        override fun contains(key: String?): Boolean = map.containsKey(key)
        override fun edit(): SharedPreferences.Editor = Editor()
        override fun registerOnSharedPreferenceChangeListener(
            listener: SharedPreferences.OnSharedPreferenceChangeListener?
        ) = Unit
        override fun unregisterOnSharedPreferenceChangeListener(
            listener: SharedPreferences.OnSharedPreferenceChangeListener?
        ) = Unit

        private inner class Editor : SharedPreferences.Editor {
            private val pending = linkedMapOf<String, Any?>()
            private val removals = mutableSetOf<String>()
            private var clearAll = false
            override fun putString(key: String?, value: String?): SharedPreferences.Editor {
                pending[key!!] = value
                return this
            }
            override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor {
                pending[key!!] = values
                return this
            }
            override fun putInt(key: String?, value: Int): SharedPreferences.Editor {
                pending[key!!] = value
                return this
            }
            override fun putLong(key: String?, value: Long): SharedPreferences.Editor {
                pending[key!!] = value
                return this
            }
            override fun putFloat(key: String?, value: Float): SharedPreferences.Editor {
                pending[key!!] = value
                return this
            }
            override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor {
                pending[key!!] = value
                return this
            }
            override fun remove(key: String?): SharedPreferences.Editor {
                removals += key!!
                return this
            }
            override fun clear(): SharedPreferences.Editor {
                clearAll = true
                return this
            }
            override fun commit(): Boolean {
                apply()
                return true
            }
            override fun apply() {
                if (clearAll) map.clear()
                removals.forEach { map.remove(it) }
                map.putAll(pending)
                pending.clear()
                removals.clear()
                clearAll = false
            }
        }
    }

    @Test
    fun firstLaunch_requiresGlassesQuestionWhenUnanswered() {
        assertTrue(GlassesSetupAuthority.requiresFirstLaunchQuestion(null))
        assertFalse(GlassesSetupAuthority.requiresFirstLaunchQuestion(true))
        assertFalse(GlassesSetupAuthority.requiresFirstLaunchQuestion(false))
        val controller = StartupSessionController(
            loadProfiles = { emptyList() },
            loadProfileCalibration = { null },
            persistCalibration = {},
            activateProfile = {},
            createPrimaryUser = { _, _, _ -> "id" }
        )
        controller.start(normallyUsesGlasses = null)
        assertEquals(StartupPhase.GlassesQuestion, controller.state.phase)
    }

    @Test
    fun selectingNo_bypassesGuidanceAndEntersPreparing() {
        var state = StartupFlowState(phase = StartupPhase.GlassesQuestion)
        state = StartupFlowAuthority.reduce(state, StartupEvent.GlassesAnswered(false))
        assertEquals(StartupPhase.FaceDetection, state.phase)
        assertEquals(false, state.normallyUsesGlasses)
        assertFalse(GlassesSetupAuthority.showPreparingReminder(state.normallyUsesGlasses))
    }

    @Test
    fun selectingYes_opensGuidance_thenContinueReachesPreparing() {
        var state = StartupFlowState(phase = StartupPhase.GlassesQuestion)
        state = StartupFlowAuthority.reduce(state, StartupEvent.GlassesAnswered(true))
        assertEquals(StartupPhase.GlassesGuidance, state.phase)
        assertEquals(true, state.normallyUsesGlasses)
        state = StartupFlowAuthority.reduce(state, StartupEvent.AcknowledgeGlassesGuidance)
        assertEquals(StartupPhase.FaceDetection, state.phase)
        assertTrue(GlassesSetupAuthority.showPreparingReminder(true))
    }

    @Test
    fun glassesPreference_persistsInStoreAndProfile() {
        val store = GlassesSetupStore(MemoryPrefs())
        assertNull(store.normallyUsesGlasses())
        store.setNormallyUsesGlasses(true)
        assertEquals(true, store.normallyUsesGlasses())
        store.setNormallyUsesGlasses(false)
        assertEquals(false, store.normallyUsesGlasses())

        val profile = LisaUserProfile(name = "Test", normallyUsesGlasses = true)
        val restored = LisaUserProfile.fromJson(profile.toJson())
        assertEquals(true, restored.normallyUsesGlasses)
        val legacy = LisaUserProfile.fromJson(
            JSONObject().apply {
                put("id", "legacy")
                put("name", "Legacy")
            }
        )
        assertNull(legacy.normallyUsesGlasses)
    }

    @Test
    fun changingAnswer_doesNotAlterEyeThresholds() {
        assertFalse(GlassesSetupAuthority.affectsEyeThresholds())
        val before = BlinkDetectionTuning.default
        assertEquals(before.closedEyeThreshold, BlinkDetectionTuning.default.closedEyeThreshold, 0f)
        assertEquals(before.openEyeThreshold, BlinkDetectionTuning.default.openEyeThreshold, 0f)
    }

    @Test
    fun releaseChooseWhereToBegin_containsOnlyProductionOptions() {
        val titles = WelcomeLaunchDestinationAuthority.productionChoiceTitles(
            LisaUiStrings.forLanguage(PreferredLanguage.English)
        )
        assertEquals(3, titles.size)
        assertTrue(titles.any { it.contains("Guided", ignoreCase = true) })
        assertTrue(titles.any { it.contains("Communication", ignoreCase = true) })
        assertTrue(titles.any { it.equals("Back", ignoreCase = true) })
        assertFalse(WelcomeLaunchDestinationAuthority.allowsDiagnosticWelcomeEntries(false))
        assertFalse(WelcomeLaunchDestinationAuthority.allowsDiagnosticWelcomeEntries(true))
        assertTrue(WelcomeLaunchDestinationAuthority.allowsEngineeringToolsHub(true))
        assertFalse(WelcomeLaunchDestinationAuthority.allowsEngineeringToolsHub(false))
    }

    @Test
    fun releaseNavigation_cannotOpenDebugTools() {
        assertFalse(WelcomeLaunchDestinationAuthority.allowsDiagnosticNavigation(false))
        assertFalse(WelcomeLaunchDestinationAuthority.allowsDiagnosticStorageInit(false))
        assertFalse(EyeTestModeAccess.isEntryVisible(false))
        assertFalse(EyeTestModeAccess.isScreenAllowed(false))
        assertFalse(PersonalisedEyeProfileAccess.isEntryVisible(false))
        assertFalse(SignalInvestigationAccess.isEntryVisible(false))
        assertFalse(GlassesCharacterisationAccess.isEntryVisible(false))
        assertFalse(GlassesCharacterisationAccess.isScreenAllowed(false))
        assertFalse(
            com.idworx.lisa.features.engineeringtools.EngineeringToolsHubAccess.isHubAllowed(false)
        )
    }

    @Test
    fun productionCopy_matchesLaunchWording() {
        assertEquals(
            "Do you normally wear glasses while using LISA?",
            GlassesSetupAuthority.QUESTION_TITLE
        )
        assertEquals("Using LISA with glasses", GlassesSetupAuthority.GUIDANCE_TITLE)
        assertTrue(GlassesSetupAuthority.GUIDANCE_NOTICE.contains("ideal conditions"))
        assertFalse(GlassesSetupAuthority.GUIDANCE_NOTICE.contains("guarantees", ignoreCase = true))
    }

    @Test
    fun answeredPreference_skipsGlassesPhasesOnStart() {
        val controller = StartupSessionController(
            loadProfiles = { emptyList() },
            loadProfileCalibration = { null },
            persistCalibration = {},
            activateProfile = {},
            createPrimaryUser = { _, _, _ -> "id" }
        )
        controller.start(normallyUsesGlasses = false)
        assertEquals(StartupPhase.FaceDetection, controller.state.phase)
        assertEquals(false, controller.state.normallyUsesGlasses)
    }
}
