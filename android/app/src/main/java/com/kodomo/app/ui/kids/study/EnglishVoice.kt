package com.kodomo.app.ui.kids.study

import android.content.Context
import android.speech.tts.TextToSpeech
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/** English pronunciation for the English course (device TTS, en-US, slow and clear). */
@Singleton
class EnglishVoice @Inject constructor(@ApplicationContext context: Context) {
    @Volatile var available = false
        private set
    private val tts = TextToSpeech(context.applicationContext) { status ->
        if (status == TextToSpeech.SUCCESS) onReady()
    }

    private fun onReady() {
        val r = tts.setLanguage(Locale.US)
        available = r != TextToSpeech.LANG_MISSING_DATA && r != TextToSpeech.LANG_NOT_SUPPORTED
        tts.setSpeechRate(0.8f)
        tts.setPitch(1.1f)
    }

    fun say(text: String) {
        if (available) tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, text.hashCode().toString())
    }
}
