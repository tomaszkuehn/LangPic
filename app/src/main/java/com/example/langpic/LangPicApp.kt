package com.example.langpic

import android.app.Application
import com.example.langpic.service.TtsHelper

class LangPicApp : Application() {

    val ttsHelper by lazy { TtsHelper(this) }

    override fun onTerminate() {
        ttsHelper.shutdown()
        super.onTerminate()
    }
}
