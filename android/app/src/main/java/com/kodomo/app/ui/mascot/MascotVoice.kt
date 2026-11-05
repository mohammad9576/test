package com.kodomo.app.ui.mascot

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.kodomo.app.core.voice.VoiceKeys
import com.kodomo.app.domain.engine.MascotLine
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Voice of the Kodomo robot and narrator of books & lessons.
 *
 * 1. Pre-recorded narration clips bundled in `assets/voice/<key>.mp3` (works on every phone, no Persian TTS needed).
 *    Word highlighting is estimated from the clip duration proportionally to word length.
 * 2. Fallback: the device's Persian text-to-speech engine when one is installed.
 * 3. Otherwise the sentence is skipped silently (the text is always visible on screen).
 */
@Singleton
class MascotVoice @Inject constructor(@ApplicationContext private val context: Context) {

    interface Listener {
        fun onWord(start: Int, end: Int) {}
        fun onDone() {}
    }

    private val main = Handler(Looper.getMainLooper())
    private val clips: Set<String> = runCatching { context.assets.list("voice")?.map { it.removeSuffix(".mp3") }?.toSet() }
        .getOrNull().orEmpty()
    private val maleClips: Set<String> = runCatching { context.assets.list("voice_m")?.map { it.removeSuffix(".mp3") }?.toSet() }
        .getOrNull().orEmpty()

    private var player: MediaPlayer? = null
    private var generation = 0
    @Volatile private var ttsReady = false
    @Volatile var persianTtsAvailable: Boolean = false
        private set

    private val tts: TextToSpeech = TextToSpeech(context.applicationContext) { status ->
        if (status == TextToSpeech.SUCCESS) onTtsReady()
    }

    /** True when the sentence can be voiced on this device (clip or Persian TTS). */
    fun canSpeak(text: String): Boolean = VoiceKeys.key(text) in clips || persianTtsAvailable

    fun hasClip(text: String): Boolean = VoiceKeys.key(text) in clips

    fun speak(text: String, listener: Listener? = null) = speakAll(listOf(text), listener)

    /** Speaks and suspends until the sentence has finished (or [timeoutMillis] passed / nothing could be played). */
    suspend fun speakAndWait(text: String, timeoutMillis: Long = 8_000) {
        val done = kotlinx.coroutines.CompletableDeferred<Unit>()
        speakAll(listOf(text), object : Listener { override fun onDone() { done.complete(Unit) } })
        kotlinx.coroutines.withTimeoutOrNull(timeoutMillis) { done.await() }
    }

    /**
     * The voice character: playback pitch/speed applied to every clip and to the device TTS, so each companion
     * (panda, lamb, dolphin …) speaks with its own voice from the same recordings.
     */
    data class Style(val pitch: Float, val speed: Float, val male: Boolean = false)

    @Volatile var style: Style = Style(1.08f, 1.0f)
        set(value) {
            field = value
            if (ttsReady) {
                tts.setPitch(1.1f * value.pitch)
                tts.setSpeechRate(0.95f * value.speed)
            }
        }

    /** Speaks a robot line: the personalised text through TTS if available, otherwise the recorded name-free variant. */
    fun speakLine(line: MascotLine) {
        if (persianTtsAvailable && line.speech.none { hasClip(it) }) speak(line.text) else speakAll(line.speech)
    }

    /** Plays sentences one after another. Word callbacks refer to offsets inside the sentence currently playing. */
    fun speakAll(texts: List<String>, listener: Listener? = null) {
        main.post {
            stopInternal()
            val token = ++generation
            playNext(texts.map(VoiceKeys::normalize).filter { it.isNotEmpty() }, 0, token, listener)
        }
    }

    fun stop() {
        main.post { stopInternal() }
    }

    private fun stopInternal() {
        generation++
        main.removeCallbacksAndMessages(WORD_TOKEN)
        player?.run {
            runCatching { stop() }
            release()
        }
        player = null
        if (ttsReady) tts.stop()
    }

    private fun playNext(texts: List<String>, index: Int, token: Int, listener: Listener?) {
        if (token != generation) return
        if (index >= texts.size) {
            listener?.onDone()
            return
        }
        val text = texts[index]
        val next: () -> Unit = { main.post { playNext(texts, index + 1, token, listener) } }
        when {
            VoiceKeys.key(text) in clips -> playClip(text, token, listener, next)
            persianTtsAvailable -> speakTts(text, token, listener, next)
            else -> next()
        }
    }

    private fun playClip(text: String, token: Int, listener: Listener?, onComplete: () -> Unit) {
        try {
            val key = VoiceKeys.key(text)
            val folder = if (style.male && key in maleClips) "voice_m" else "voice"
            val afd = context.assets.openFd("$folder/$key.mp3")
            val mp = MediaPlayer()
            mp.setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
            mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            afd.close()
            mp.setOnCompletionListener {
                if (player === mp) {
                    mp.release()
                    player = null
                }
                if (token == generation) onComplete()
            }
            mp.prepare()
            player = mp
            runCatching { mp.playbackParams = android.media.PlaybackParams().setPitch(style.pitch).setSpeed(style.speed) }
            mp.start()
            if (listener != null) scheduleWords(text, (mp.duration / style.speed).toLong(), token, listener)
        } catch (e: Exception) {
            Log.w(TAG, "Unable to play narration clip", e)
            onComplete()
        }
    }

    /** Estimates when each word is spoken: duration is split proportionally to word length (plus a pause weight). */
    private fun scheduleWords(text: String, durationMs: Long, token: Int, listener: Listener) {
        val words = Regex("\\S+").findAll(text).toList()
        if (words.isEmpty() || durationMs <= 0) return
        val weights = words.map { it.value.length + 2 }
        val total = weights.sum().toFloat()
        val usable = durationMs * 0.94f
        var elapsed = 0f
        words.forEachIndexed { i, match ->
            val at = (elapsed / total * usable).toLong()
            main.postAtTime({ if (token == generation) listener.onWord(match.range.first, match.range.last + 1) }, WORD_TOKEN, android.os.SystemClock.uptimeMillis() + at)
            elapsed += weights[i]
        }
    }

    private fun speakTts(text: String, token: Int, listener: Listener?, onComplete: () -> Unit) {
        val id = UUID.randomUUID().toString()
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String) = Unit

            override fun onDone(utteranceId: String) {
                if (utteranceId == id) main.post { if (token == generation) onComplete() }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String) {
                if (utteranceId == id) main.post { if (token == generation) onComplete() }
            }

            override fun onRangeStart(utteranceId: String, start: Int, end: Int, frame: Int) {
                if (utteranceId == id) main.post { if (token == generation) listener?.onWord(start, end) }
            }
        })
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, Bundle(), id)
    }

    private fun onTtsReady() {
        val result = tts.setLanguage(Locale("fa", "IR"))
        persianTtsAvailable = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
        tts.setSpeechRate(0.95f * style.speed)
        tts.setPitch(1.1f * style.pitch)
        ttsReady = true
        Log.i(TAG, "Narration clips: ${clips.size}, Persian TTS available: $persianTtsAvailable")
    }

    private companion object {
        const val TAG = "MascotVoice"
        val WORD_TOKEN = Any()
    }
}
