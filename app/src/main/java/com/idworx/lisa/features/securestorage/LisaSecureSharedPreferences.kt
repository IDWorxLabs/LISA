package com.idworx.lisa.features.securestorage

import android.content.Context
import android.content.SharedPreferences
import com.idworx.lisa.features.securestorage.LisaSecureTypedCodec.TypedValue
import java.util.concurrent.CopyOnWriteArrayList

/**
 * SharedPreferences facade that encrypts every stored value with [LisaValueCipher].
 * Synchronous reads/writes match existing LISA store usage. Listeners are supported.
 */
class LisaSecureSharedPreferences private constructor(
    private val delegate: SharedPreferences,
    private val cipher: LisaValueCipher
) : SharedPreferences {

    private val listeners = CopyOnWriteArrayList<SharedPreferences.OnSharedPreferenceChangeListener>()

    override fun getAll(): MutableMap<String, *> {
        val out = LinkedHashMap<String, Any?>()
        for (key in delegate.all.keys) {
            val typed = readTypedOrNull(key) ?: continue
            out[key] = when (typed) {
                is TypedValue.StringValue -> typed.value
                is TypedValue.BooleanValue -> typed.value
                is TypedValue.IntValue -> typed.value
                is TypedValue.LongValue -> typed.value
                is TypedValue.FloatValue -> typed.value
                is TypedValue.StringSetValue -> typed.value
            }
        }
        return out
    }

    override fun getString(key: String?, defValue: String?): String? {
        if (key == null) return defValue
        return when (val typed = readTypedOrNull(key)) {
            is TypedValue.StringValue -> typed.value
            null -> defValue
            else -> defValue
        }
    }

    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? {
        if (key == null) return defValues
        return when (val typed = readTypedOrNull(key)) {
            is TypedValue.StringSetValue -> typed.value.toMutableSet()
            null -> defValues
            else -> defValues
        }
    }

    override fun getInt(key: String?, defValue: Int): Int {
        if (key == null) return defValue
        return when (val typed = readTypedOrNull(key)) {
            is TypedValue.IntValue -> typed.value
            null -> defValue
            else -> defValue
        }
    }

    override fun getLong(key: String?, defValue: Long): Long {
        if (key == null) return defValue
        return when (val typed = readTypedOrNull(key)) {
            is TypedValue.LongValue -> typed.value
            null -> defValue
            else -> defValue
        }
    }

    override fun getFloat(key: String?, defValue: Float): Float {
        if (key == null) return defValue
        return when (val typed = readTypedOrNull(key)) {
            is TypedValue.FloatValue -> typed.value
            null -> defValue
            else -> defValue
        }
    }

    override fun getBoolean(key: String?, defValue: Boolean): Boolean {
        if (key == null) return defValue
        return when (val typed = readTypedOrNull(key)) {
            is TypedValue.BooleanValue -> typed.value
            null -> defValue
            else -> defValue
        }
    }

    override fun contains(key: String?): Boolean {
        if (key == null) return false
        return delegate.contains(key)
    }

    override fun edit(): SharedPreferences.Editor = EditorImpl()

    override fun registerOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener?
    ) {
        if (listener != null) listeners.addIfAbsent(listener)
    }

    override fun unregisterOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener?
    ) {
        if (listener != null) listeners.remove(listener)
    }

    private fun readTypedOrNull(key: String): TypedValue? {
        val stored = delegate.getString(key, null) ?: return null
        return try {
            LisaSecureTypedCodec.fromStorageString(cipher, key, stored)
        } catch (_: LisaSecureStorageException) {
            // Tampered / wrong key / keystore failure — treat as missing without leaking details.
            null
        }
    }

    private fun notifyChanged(keys: Collection<String>) {
        for (key in keys) {
            for (listener in listeners) {
                listener.onSharedPreferenceChanged(this, key)
            }
        }
    }

    private inner class EditorImpl : SharedPreferences.Editor {
        private val pending = LinkedHashMap<String, TypedValue?>()
        private var clearAll = false

        override fun putString(key: String?, value: String?): SharedPreferences.Editor {
            if (key != null) pending[key] = value?.let { TypedValue.StringValue(it) }
            return this
        }

        override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor {
            if (key != null) pending[key] = values?.let { TypedValue.StringSetValue(it.toSet()) }
            return this
        }

        override fun putInt(key: String?, value: Int): SharedPreferences.Editor {
            if (key != null) pending[key] = TypedValue.IntValue(value)
            return this
        }

        override fun putLong(key: String?, value: Long): SharedPreferences.Editor {
            if (key != null) pending[key] = TypedValue.LongValue(value)
            return this
        }

        override fun putFloat(key: String?, value: Float): SharedPreferences.Editor {
            if (key != null) pending[key] = TypedValue.FloatValue(value)
            return this
        }

        override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor {
            if (key != null) pending[key] = TypedValue.BooleanValue(value)
            return this
        }

        override fun remove(key: String?): SharedPreferences.Editor {
            if (key != null) pending[key] = null
            return this
        }

        override fun clear(): SharedPreferences.Editor {
            clearAll = true
            pending.clear()
            return this
        }

        override fun commit(): Boolean {
            val changed = applyToDelegate(commit = true)
            return changed
        }

        override fun apply() {
            applyToDelegate(commit = false)
        }

        private fun applyToDelegate(commit: Boolean): Boolean {
            val editor = delegate.edit()
            if (clearAll) editor.clear()
            val changedKeys = LinkedHashSet<String>()
            if (clearAll) {
                changedKeys.addAll(delegate.all.keys)
            }
            for ((key, typed) in pending) {
                changedKeys.add(key)
                if (typed == null) {
                    editor.remove(key)
                } else {
                    val stored = LisaSecureTypedCodec.toStorageString(cipher, key, typed)
                    editor.putString(key, stored)
                }
            }
            val ok = if (commit) editor.commit() else {
                editor.apply()
                true
            }
            if (ok) notifyChanged(changedKeys)
            return ok
        }
    }

    companion object {
        fun wrap(delegate: SharedPreferences, cipher: LisaValueCipher): LisaSecureSharedPreferences =
            LisaSecureSharedPreferences(delegate, cipher)

        fun create(
            context: Context,
            cipher: LisaValueCipher = AndroidKeystoreAesGcmCipher()
        ): LisaSecureSharedPreferences {
            val delegate = context.applicationContext.getSharedPreferences(
                LisaSecureCryptoFormat.SECURE_PREFS_NAME,
                Context.MODE_PRIVATE
            )
            return wrap(delegate, cipher)
        }
    }
}
