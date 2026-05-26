package com.example.langpic.service

import android.content.Context
import android.speech.tts.TextToSpeech
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
        if (!isLanguageAvailable(languageCode)) return

        tts.stop()
        tts.setLanguage(localeFor(languageCode))
        tts.speak(word, TextToSpeech.QUEUE_FLUSH, null, "langpic_utterance")
    }

    fun isLanguageAvailable(languageCode: String): Boolean {
        val tts = tts ?: return false
        val result = tts.isLanguageAvailable(localeFor(languageCode))
        return result == TextToSpeech.LANG_AVAILABLE || result == TextToSpeech.LANG_COUNTRY_AVAILABLE || result == TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }

    private fun localeFor(code: String): Locale = when (code) {
        "ja" -> Locale.JAPANESE
        "en" -> Locale.ENGLISH
        else -> Locale.forLanguageTag(code)
    }
}
