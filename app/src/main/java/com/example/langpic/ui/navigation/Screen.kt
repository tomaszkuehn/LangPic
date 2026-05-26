package com.example.langpic.ui.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Game : Screen("game")
    data object Import : Screen("import")
    data object Manage : Screen("manage")
}
