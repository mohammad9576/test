package com.kodomo.app.ui.mascot

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Text-to-speech voice of the Kodomo robot. Prefers Persian (fa-IR) and falls back to the device default.
 * Provides word-range callbacks used by the book reader to highlight words in sync with narration.
 */
@Singleton
class MascotVoice @Inject constructor(@ApplicationContext context: Context) {

    interface Listener {
        fun onWord(start: Int, end: Int) {}
        fun onDone() {}
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private val listeners = ConcurrentHashMap<String, Listener>()
    private val pending = mutableListOf<Pair<String, Listener?>>()
    @Volatile private var ready = false
    @Volatile var persianAvailable: Boolean = false
        private set

    private val tts: TextToSpeech = TextToSpeech(context.applicationContext) { status ->
        if (status == TextToSpeech.SUCCESS) onReady()
    }

    private fun onReady() {
        val persian = Locale("fa", "IR")
        val result = tts.setLanguage(persian)
        persianAvailable = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
        if (!persianAvailable) tts.setLanguage(Locale.getDefault())
        tts.setSpeechRate(0.92f)
        tts.setPitch(1.15f)
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String) = Unit

            override fun onDone(utteranceId: String) {
                listeners.remove(utteranceId)?.let { l -> mainHandler.post { l.onDone() } }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String) {
                listeners.remove(utteranceId)?.let { l -> mainHandler.post { l.onDone() } }
            }

            override fun onRangeStart(utteranceId: String, start: Int, end: Int, frame: Int) {
                listeners[utteranceId]?.let { l -> mainHandler.post { l.onWord(start, end) } }
            }
        })
        ready = true
        synchronized(pending) {
            pending.forEach { (text, listener) -> speakNow(text, listener) }
            pending.clear()
        }
    }

    fun speak(text: String, listener: Listener? = null) {
        if (!ready) {
            synchronized(pending) { pending.add(text to listener) }
            return
        }
        speakNow(text, listener)
    }

    private fun speakNow(text: String, listener: Listener?) {
        val id = UUID.randomUUID().toString()
        if (listener != null) listeners[id] = listener
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, Bundle(), id)
    }

    fun stop() {
        listeners.clear()
        synchronized(pending) { pending.clear() }
        if (ready) tts.stop()
    }
}
