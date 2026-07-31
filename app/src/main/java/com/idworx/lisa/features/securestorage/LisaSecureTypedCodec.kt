package com.idworx.lisa.features.securestorage

import org.json.JSONArray
import java.nio.charset.StandardCharsets
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Typed plaintext codec for preference values before AES-GCM encryption.
 * Format: single type tag byte as ASCII char + UTF-8 payload.
 *
 * Uses Kotlin Base64 (not java.util.Base64) so minSdk 24 and JVM unit tests both work
 * without requiring API 26 or core library desugaring.
 */
@OptIn(ExperimentalEncodingApi::class)
object LisaSecureTypedCodec {
    private const val TAG_STRING = 'S'
    private const val TAG_BOOLEAN = 'B'
    private const val TAG_INT = 'I'
    private const val TAG_LONG = 'L'
    private const val TAG_FLOAT = 'F'
    private const val TAG_STRING_SET = 'E'

    sealed class TypedValue {
        data class StringValue(val value: String) : TypedValue()
        data class BooleanValue(val value: Boolean) : TypedValue()
        data class IntValue(val value: Int) : TypedValue()
        data class LongValue(val value: Long) : TypedValue()
        data class FloatValue(val value: Float) : TypedValue()
        data class StringSetValue(val value: Set<String>) : TypedValue()
    }

    fun encode(value: TypedValue): ByteArray = when (value) {
        is TypedValue.StringValue -> (TAG_STRING + value.value).toByteArray(StandardCharsets.UTF_8)
        is TypedValue.BooleanValue ->
            (TAG_BOOLEAN + if (value.value) "1" else "0").toByteArray(StandardCharsets.UTF_8)
        is TypedValue.IntValue -> (TAG_INT + value.value.toString()).toByteArray(StandardCharsets.UTF_8)
        is TypedValue.LongValue -> (TAG_LONG + value.value.toString()).toByteArray(StandardCharsets.UTF_8)
        is TypedValue.FloatValue ->
            (TAG_FLOAT + value.value.toBits().toString()).toByteArray(StandardCharsets.UTF_8)
        is TypedValue.StringSetValue -> {
            val array = JSONArray()
            value.value.forEach { array.put(it) }
            (TAG_STRING_SET + array.toString()).toByteArray(StandardCharsets.UTF_8)
        }
    }

    fun decode(bytes: ByteArray): TypedValue {
        if (bytes.isEmpty()) throw LisaSecureStorageException("Empty typed payload")
        val text = String(bytes, StandardCharsets.UTF_8)
        val tag = text[0]
        val payload = text.substring(1)
        return when (tag) {
            TAG_STRING -> TypedValue.StringValue(payload)
            TAG_BOOLEAN -> TypedValue.BooleanValue(payload == "1")
            TAG_INT -> TypedValue.IntValue(payload.toInt())
            TAG_LONG -> TypedValue.LongValue(payload.toLong())
            TAG_FLOAT -> TypedValue.FloatValue(Float.fromBits(payload.toInt()))
            TAG_STRING_SET -> {
                val array = JSONArray(payload)
                val set = LinkedHashSet<String>()
                for (i in 0 until array.length()) {
                    set.add(array.getString(i))
                }
                TypedValue.StringSetValue(set)
            }
            else -> throw LisaSecureStorageException("Unknown typed payload tag")
        }
    }

    fun toStorageString(cipher: LisaValueCipher, preferenceKey: String, value: TypedValue): String {
        val envelope = cipher.encrypt(preferenceKey, encode(value))
        val encoded = Base64.Default.encode(envelope)
        return LisaSecureCryptoFormat.VALUE_PREFIX + encoded
    }

    fun fromStorageString(cipher: LisaValueCipher, preferenceKey: String, stored: String): TypedValue {
        if (!stored.startsWith(LisaSecureCryptoFormat.VALUE_PREFIX)) {
            throw LisaSecureStorageException("Value is not an encrypted LISA envelope")
        }
        val b64 = stored.removePrefix(LisaSecureCryptoFormat.VALUE_PREFIX)
        val envelope = Base64.Default.decode(b64)
        return decode(cipher.decrypt(preferenceKey, envelope))
    }
}
