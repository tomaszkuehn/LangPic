package com.example.langpic.service

import android.content.Context

class AppPreferences(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("langpic_prefs", Context.MODE_PRIVATE)

    var typewriterEnabled: Boolean
        get() = prefs.getBoolean("typewriter_enabled", true)
        set(value) = prefs.edit().putBoolean("typewriter_enabled", value).apply()

    companion object {
        @Volatile
        private var INSTANCE: AppPreferences? = null

        fun getInstance(context: Context): AppPreferences {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppPreferences(context).also { INSTANCE = it }
            }
        }
    }
}
