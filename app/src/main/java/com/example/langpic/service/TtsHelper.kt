package com.example.langpic.service

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

class TtsHelper(context: Context) {

    private var tts: TextToSpeech? = null
    private var initialized = false

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            initialized = status == TextToSpeech.SUCCESS
        }
    }

    fun speak(word: String, languageCode: String) {
        val tts = tts ?: return
        if (!initialized) return

        tts.stop()

        val locale = when (languageCode) {
            "ja" -> Locale.JAPANESE
            "en" -> Locale.ENGLISH
            else -> Locale.ENGLISH
        }

        val result = tts.setLanguage(locale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            tts.setLanguage(Locale.getDefault())
        }

        tts.speak(word, TextToSpeech.QUEUE_FLUSH, null, "langpic_utterance")
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
