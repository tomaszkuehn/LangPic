package com.example.langpic

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.langpic.ui.navigation.NavGraph
import com.example.langpic.ui.theme.LangPicTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as LangPicApp

        setContent {
            LangPicTheme {
                NavGraph(ttsHelper = app.ttsHelper)
            }
        }
    }
}
