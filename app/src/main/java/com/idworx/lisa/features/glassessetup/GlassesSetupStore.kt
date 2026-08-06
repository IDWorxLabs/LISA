package com.idworx.lisa.features.glassessetup

import android.content.Context
import android.content.SharedPreferences
import com.idworx.lisa.features.securestorage.LisaPreferences

/**
 * Durable setup preference for glasses guidance.
 * Tri-state: absent key / null → unanswered; true/false → answered.
 */
class GlassesSetupStore(
    private val prefs: SharedPreferences
) {
    constructor(context: Context) : this(LisaPreferences.get(context))

    fun normallyUsesGlasses(): Boolean? {
        if (!prefs.contains(KEY)) return null
        return prefs.getBoolean(KEY, false)
    }

    fun setNormallyUsesGlasses(value: Boolean) {
        prefs.edit().putBoolean(KEY, value).apply()
    }

    fun clearForTests() {
        prefs.edit().remove(KEY).apply()
    }

    companion object {
        const val KEY = "normally_uses_glasses"
    }
}
