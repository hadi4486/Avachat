package com.avachat.app.core.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.avachat.app.core.data.remote.NetworkModule
import com.avachat.app.core.data.settings.SettingsRepository
import com.avachat.app.core.domain.error.AppError
import com.avachat.app.core.domain.model.AppSettings
import com.avachat.app.core.domain.model.AppLanguage
import com.avachat.app.core.domain.model.ChatBehaviorSettings
import com.avachat.app.core.domain.model.ProviderSettings
import com.avachat.app.core.domain.model.ThemeMode
import com.avachat.app.core.domain.repository.AiChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ConnectionTestState { IDLE, TESTING, SUCCESS, FAILED }

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val testState: ConnectionTestState = ConnectionTestState.IDLE,
    val testMessage: String? = null
)

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val aiRepository: AiChatRepository
) : ViewModel() {

    private val testState = MutableStateFlow(ConnectionTestState.IDLE)
    private val testMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.settings,
        testState,
        testMessage
    ) { settings, state, message ->
        SettingsUiState(settings = settings, testState = state, testMessage = message)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun saveProvider(provider: ProviderSettings) {
        viewModelScope.launch {
            settingsRepository.saveProvider(provider.copy(apiKey = provider.apiKey.trim()))
            resetTest()
        }
    }

    fun saveAppearance(theme: ThemeMode, language: AppLanguage, dynamicColors: Boolean) {
        viewModelScope.launch {
            settingsRepository.saveAppearance(theme, language, dynamicColors)
        }
    }

    fun saveChatBehavior(behavior: ChatBehaviorSettings) {
        viewModelScope.launch {
            settingsRepository.saveChatBehavior(behavior)
        }
    }

    fun testConnection() {
        val current = uiState.value.settings.provider
        viewModelScope.launch {
            testState.value = ConnectionTestState.TESTING
            testMessage.value = null
            try {
                aiRepository.testConnection(current)
                testState.value = ConnectionTestState.SUCCESS
            } catch (e: AppError) {
                testState.value = ConnectionTestState.FAILED
                testMessage.value = e.userMessageKey
            } catch (e: Exception) {
                testState.value = ConnectionTestState.FAILED
                testMessage.value = AppError.fromThrowable(e).userMessageKey
            }
        }
    }

    fun resetTest() {
        testState.value = ConnectionTestState.IDLE
        testMessage.value = null
    }

    fun resetAllSettings() {
        viewModelScope.launch {
            settingsRepository.resetAll()
            resetTest()
        }
    }

    class Factory(
        private val settingsRepository: SettingsRepository,
        private val aiRepository: AiChatRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SettingsViewModel(settingsRepository, aiRepository) as T
    }
}
