package com.kodomo.app.core.voice

import java.security.MessageDigest

/**
 * Maps a sentence to the file name of its pre-recorded narration clip (`assets/voice/<key>.mp3`).
 * The same function is used by the build-time exporter, so recorded clips and runtime lookups always agree.
 */
object VoiceKeys {
    fun normalize(text: String): String = text.trim().replace(Regex("\\s+"), " ")

    fun key(text: String): String {
        val digest = MessageDigest.getInstance("SHA-1").digest(normalize(text).toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }.take(20)
    }
}
