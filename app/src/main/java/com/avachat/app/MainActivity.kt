package com.avachat.app

import android.content.Context
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.avachat.app.core.data.remote.NetworkModule
import com.avachat.app.core.domain.model.AppSettings
import com.avachat.app.core.domain.model.AppLanguage
import com.avachat.app.core.presentation.chat.ChatViewModel
import com.avachat.app.core.presentation.history.HistoryViewModel
import com.avachat.app.core.presentation.settings.SettingsViewModel
import com.avachat.app.ui.chat.ChatScreen
import com.avachat.app.ui.history.HistoryScreen
import com.avachat.app.ui.settings.SettingsScreen
import com.avachat.app.ui.theme.AvaChatTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val app = application as AvaChatApp
        setContent {
            val appSettings by app.settingsRepository.settings
                .collectAsState(initial = AppSettings())
            AvaChatTheme(
                themeMode = appSettings.themeMode,
                dynamicColors = appSettings.dynamicColors
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AvaNavHost(app)
                }
            }
        }
    }
}

@Composable
private fun AvaNavHost(app: AvaChatApp) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "${Routes.CHAT}?conversationId={conversationId}"
    ) {
        composable(
            route = "${Routes.CHAT}?conversationId={conversationId}",
            arguments = listOf(navArgument("conversationId") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            })
        ) { entry ->
            val conversationId = entry.arguments?.getString("conversationId")
            val viewModel: ChatViewModel = viewModel(
                factory = ChatViewModel.Factory(
                    chatRepository = app.chatRepository,
                    aiRepository = app.aiRepository,
                    settingsRepository = app.settingsRepository,
                    isOnline = { NetworkModule.isOnline(app) }
                )
            )
            androidx.compose.runtime.LaunchedEffect(conversationId) {
                when {
                    conversationId != null -> viewModel.openConversation(conversationId)
                    else -> Unit
                }
            }
            ChatScreen(
                viewModel = viewModel,
                onOpenHistory = { navController.navigate(Routes.HISTORY) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }
        composable(Routes.HISTORY) {
            val viewModel: HistoryViewModel = viewModel(
                factory = HistoryViewModel.Factory(app.chatRepository)
            )
            HistoryScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onOpenConversation = { id ->
                    // Replace the stack so Back from chat exits the app cleanly
                    // and the chat always opens the chosen conversation.
                    navController.navigate("${Routes.CHAT}?conversationId=$id") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.SETTINGS) {
            val viewModel: SettingsViewModel = viewModel(
                factory = SettingsViewModel.Factory(app.settingsRepository, app.aiRepository)
            )
            SettingsScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
    }
}

private object Routes {
    const val CHAT = "chat"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
}
