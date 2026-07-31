package com.idworx.lisa.features.securestorage

import android.content.Context
import android.content.SharedPreferences

/**
 * Single entry point for LISA durable preferences.
 *
 * After successful migration, all stores read/write encrypted [lisa_secure_prefs].
 * If Keystore is unavailable before migration completes, legacy plaintext remains readable
 * so user data is not lost — but once migration is marked complete there is no plaintext fallback.
 */
object LisaPreferences {

    @Volatile
    private var cached: SharedPreferences? = null

    @Volatile
    private var testOverride: SharedPreferences? = null

    /** Production and instrumentation entry point. */
    fun get(context: Context): SharedPreferences {
        testOverride?.let { return it }
        cached?.let { return it }
        synchronized(this) {
            testOverride?.let { return it }
            cached?.let { return it }
            val app = context.applicationContext
            LisaSecurePreferencesMigration.runIfNeeded(app)
            val prefs = if (LisaSecurePreferencesMigration.isMigrationComplete(app)) {
                LisaSecureSharedPreferences.create(app)
            } else {
                // Migration not complete (empty device with keystore issue, or failure).
                // Keep reading/writing legacy so data is not destroyed; encryption retries next cold start
                // unless KEYSTORE_UNAVAILABLE was set (then stays on legacy until app update/fix).
                app.getSharedPreferences(LisaSecureCryptoFormat.LEGACY_PREFS_NAME, Context.MODE_PRIVATE)
            }
            cached = prefs
            return prefs
        }
    }

    /** JVM / unit-test override — inject in-memory prefs without AndroidKeyStore. */
    fun setTestOverride(prefs: SharedPreferences?) {
        testOverride = prefs
        cached = null
    }

    fun resetCacheForTests() {
        cached = null
    }
}
