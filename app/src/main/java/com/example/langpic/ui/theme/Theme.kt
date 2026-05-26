package com.example.langpic.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = Orange500,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = Orange700,
    secondary = Teal400,
    onSecondary = androidx.compose.ui.graphics.Color.White,
    secondaryContainer = Teal600,
    background = CreamBg,
    onBackground = DarkText,
    surface = androidx.compose.ui.graphics.Color.White,
    onSurface = DarkText,
)

@Composable
fun LangPicTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = AppTypography,
        content = content,
    )
}
