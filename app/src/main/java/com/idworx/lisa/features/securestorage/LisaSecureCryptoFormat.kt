package com.idworx.lisa.features.securestorage

/**
 * LISA V1 Keystore-backed AES-GCM preference encryption.
 *
 * Envelope (binary, then Base64 for SharedPreferences string storage):
 * - byte 0: format version ([FORMAT_VERSION])
 * - byte 1: IV length (always 12)
 * - bytes 2..(2+ivLen-1): IV
 * - remaining: ciphertext || GCM authentication tag
 *
 * Preference key is bound as GCM AAD so ciphertext cannot be moved between keys.
 */
object LisaSecureCryptoFormat {
    const val FORMAT_VERSION: Byte = 1
    const val IV_BYTES: Int = 12
    const val GCM_TAG_BITS: Int = 128
    const val AES_KEY_BITS: Int = 256
    const val TRANSFORMATION: String = "AES/GCM/NoPadding"
    const val ANDROID_KEYSTORE: String = "AndroidKeyStore"
    const val KEY_ALIAS: String = "lisa_v1_prefs_aes_gcm"
    const val MIGRATION_FORMAT_VERSION: Int = 1

    /** Prefix distinguishing encrypted preference string values from any legacy plaintext. */
    const val VALUE_PREFIX: String = "LISA1:"

    const val LEGACY_PREFS_NAME: String = "lisa_prefs"
    const val SECURE_PREFS_NAME: String = "lisa_secure_prefs"
    /**
     * Tiny bootstrap file — holds only migration completion status as an unencrypted boolean.
     * Justification: must be readable before Keystore is available to decide whether to migrate;
     * contains no user content. All user data lives in [SECURE_PREFS_NAME].
     */
    const val META_PREFS_NAME: String = "lisa_secure_meta"
    const val META_MIGRATION_COMPLETE: String = "migration_complete_v1"
    const val META_MIGRATION_FORMAT: String = "migration_format_v1"
    const val META_KEYSTORE_UNAVAILABLE: String = "keystore_unavailable_v1"
}

class LisaSecureStorageException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)

interface LisaValueCipher {
    fun encrypt(preferenceKey: String, plaintext: ByteArray): ByteArray
    fun decrypt(preferenceKey: String, envelope: ByteArray): ByteArray
}
