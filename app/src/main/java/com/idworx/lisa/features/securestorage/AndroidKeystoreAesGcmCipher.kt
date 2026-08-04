package com.idworx.lisa.features.securestorage

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Production cipher: non-exportable AES-256 key in AndroidKeyStore, AES/GCM/NoPadding,
 * Keystore-generated IV per encryption (caller-provided IV is not permitted when
 * [KeyGenParameterSpec.Builder.setRandomizedEncryptionRequired] is true), preference key as AAD.
 *
 * Envelope layout is unchanged: version | ivLen | iv | ciphertext||tag.
 * Decrypt always uses the IV stored in the envelope ([GCMParameterSpec]).
 */
class AndroidKeystoreAesGcmCipher(
    private val keyAlias: String = LisaSecureCryptoFormat.KEY_ALIAS
) : LisaValueCipher {

    override fun encrypt(preferenceKey: String, plaintext: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(LisaSecureCryptoFormat.TRANSFORMATION)
        // Android Keystore rejects caller-provided IVs for ENCRYPT_MODE when randomized
        // encryption is required. Let the Keystore generate the IV, then persist cipher.iv.
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        cipher.updateAAD(preferenceKey.toByteArray(Charsets.UTF_8))
        val ciphertext = cipher.doFinal(plaintext)
        val iv = cipher.iv
            ?: throw LisaSecureStorageException("Keystore did not return a GCM IV after encrypt")
        if (iv.size != LisaSecureCryptoFormat.IV_BYTES) {
            throw LisaSecureStorageException(
                "Unexpected Keystore IV length: ${iv.size} (expected ${LisaSecureCryptoFormat.IV_BYTES})"
            )
        }
        return buildEnvelope(iv, ciphertext)
    }

    override fun decrypt(preferenceKey: String, envelope: ByteArray): ByteArray {
        val (iv, ciphertext) = parseEnvelope(envelope)
        val cipher = Cipher.getInstance(LisaSecureCryptoFormat.TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            getOrCreateKey(),
            GCMParameterSpec(LisaSecureCryptoFormat.GCM_TAG_BITS, iv)
        )
        cipher.updateAAD(preferenceKey.toByteArray(Charsets.UTF_8))
        return try {
            cipher.doFinal(ciphertext)
        } catch (t: Throwable) {
            throw LisaSecureStorageException("Authenticated decrypt failed", t)
        }
    }

    fun ensureKeyAvailable(): SecretKey = getOrCreateKey()

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(LisaSecureCryptoFormat.ANDROID_KEYSTORE).apply { load(null) }
        val existing = keyStore.getKey(keyAlias, null) as? SecretKey
        if (existing != null) return existing

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            LisaSecureCryptoFormat.ANDROID_KEYSTORE
        )
        val spec = KeyGenParameterSpec.Builder(
            keyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setKeySize(LisaSecureCryptoFormat.AES_KEY_BITS)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    companion object {
        fun buildEnvelope(iv: ByteArray, ciphertext: ByteArray): ByteArray {
            require(iv.size == LisaSecureCryptoFormat.IV_BYTES) {
                "IV must be ${LisaSecureCryptoFormat.IV_BYTES} bytes"
            }
            val out = ByteArray(2 + iv.size + ciphertext.size)
            out[0] = LisaSecureCryptoFormat.FORMAT_VERSION
            out[1] = iv.size.toByte()
            System.arraycopy(iv, 0, out, 2, iv.size)
            System.arraycopy(ciphertext, 0, out, 2 + iv.size, ciphertext.size)
            return out
        }

        fun parseEnvelope(envelope: ByteArray): Pair<ByteArray, ByteArray> {
            if (envelope.size < 2 + LisaSecureCryptoFormat.IV_BYTES + 1) {
                throw LisaSecureStorageException("Encrypted envelope too short")
            }
            if (envelope[0] != LisaSecureCryptoFormat.FORMAT_VERSION) {
                throw LisaSecureStorageException("Unsupported encryption format version")
            }
            val ivLen = envelope[1].toInt() and 0xFF
            if (ivLen != LisaSecureCryptoFormat.IV_BYTES) {
                throw LisaSecureStorageException("Unexpected IV length")
            }
            if (envelope.size < 2 + ivLen + 1) {
                throw LisaSecureStorageException("Encrypted envelope truncated")
            }
            val iv = envelope.copyOfRange(2, 2 + ivLen)
            val ciphertext = envelope.copyOfRange(2 + ivLen, envelope.size)
            return iv to ciphertext
        }
    }
}

/**
 * Software AES-GCM cipher for JVM unit tests. Uses an in-memory key — never for production.
 * Caller-provided IVs are allowed here (not Android Keystore).
 */
class SoftwareAesGcmCipher(
    private val keyBytes: ByteArray = ByteArray(32).also { SecureRandom().nextBytes(it) }
) : LisaValueCipher {
    private val secureRandom = SecureRandom()
    private val secretKey = javax.crypto.spec.SecretKeySpec(keyBytes, "AES")

    override fun encrypt(preferenceKey: String, plaintext: ByteArray): ByteArray {
        val iv = ByteArray(LisaSecureCryptoFormat.IV_BYTES).also { secureRandom.nextBytes(it) }
        val cipher = Cipher.getInstance(LisaSecureCryptoFormat.TRANSFORMATION)
        cipher.init(
            Cipher.ENCRYPT_MODE,
            secretKey,
            GCMParameterSpec(LisaSecureCryptoFormat.GCM_TAG_BITS, iv)
        )
        cipher.updateAAD(preferenceKey.toByteArray(Charsets.UTF_8))
        return AndroidKeystoreAesGcmCipher.buildEnvelope(iv, cipher.doFinal(plaintext))
    }

    override fun decrypt(preferenceKey: String, envelope: ByteArray): ByteArray {
        val (iv, ciphertext) = AndroidKeystoreAesGcmCipher.parseEnvelope(envelope)
        val cipher = Cipher.getInstance(LisaSecureCryptoFormat.TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            secretKey,
            GCMParameterSpec(LisaSecureCryptoFormat.GCM_TAG_BITS, iv)
        )
        cipher.updateAAD(preferenceKey.toByteArray(Charsets.UTF_8))
        return try {
            cipher.doFinal(ciphertext)
        } catch (t: Throwable) {
            throw LisaSecureStorageException("Authenticated decrypt failed", t)
        }
    }
}
