package com.kodomo.app.core.security

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** PBKDF2-HMAC-SHA256 hashing for the 4-digit parent PIN. The raw PIN is never persisted. */
object PinHasher {
    private const val ITERATIONS = 120_000
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_BYTES = 16

    data class HashedPin(val hash: String, val salt: String)

    fun hash(pin: String): HashedPin {
        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        return HashedPin(encode(derive(pin, salt)), encode(salt))
    }

    fun verify(pin: String, hashed: HashedPin): Boolean {
        val salt = Base64.decode(hashed.salt, Base64.NO_WRAP)
        val expected = Base64.decode(hashed.hash, Base64.NO_WRAP)
        return MessageDigest.isEqual(expected, derive(pin, salt))
    }

    private fun derive(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun encode(bytes: ByteArray): String = Base64.encodeToString(bytes, Base64.NO_WRAP)
}
