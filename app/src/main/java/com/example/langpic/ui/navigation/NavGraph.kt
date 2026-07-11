package com.example.langpic.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.langpic.data.database.AppDatabase
import com.example.langpic.data.repository.LessonRepository
import com.example.langpic.service.TtsHelper
import com.example.langpic.ui.screen.game.GameScreen
import com.example.langpic.ui.screen.game.GameViewModel
import com.example.langpic.ui.screen.home.HomeScreen
import com.example.langpic.ui.screen.importzip.ImportScreen
import com.example.langpic.ui.screen.importzip.ImportViewModel
import com.example.langpic.ui.screen.manage.ManageScreen
import com.example.langpic.ui.screen.manage.ManageViewModel

@Composable
fun NavGraph(ttsHelper: TtsHelper) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val db = AppDatabase.getInstance(context)
    val repository = LessonRepository(db.lessonPackDao(), db.lessonItemDao())

    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) {
            HomeScreen(
                onPlay = { navController.navigate(Screen.Game.route) },
                onImport = { navController.navigate(Screen.Import.route) },
                onManage = { navController.navigate(Screen.Manage.route) },
            )
        }

        composable(Screen.Game.route) {
            val vm: GameViewModel = viewModel()
            GameScreen(
                viewModel = vm,
                ttsHelper = ttsHelper,
                repository = repository,
                onBack = { navController.popBackStack() },
            )
        }

        composable(Screen.Import.route) {
            val vm: ImportViewModel = viewModel()
            ImportScreen(
                viewModel = vm,
                repository = repository,
                ttsHelper = ttsHelper,
                onBack = { navController.popBackStack() },
            )
        }

        composable(Screen.Manage.route) {
            val vm: ManageViewModel = viewModel()
            ManageScreen(
                viewModel = vm,
                repository = repository,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
