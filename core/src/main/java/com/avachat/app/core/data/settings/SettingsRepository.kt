package com.avachat.app.core.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.avachat.app.core.domain.model.AppSettings
import com.avachat.app.core.domain.model.AppLanguage
import com.avachat.app.core.domain.model.ChatBehaviorSettings
import com.avachat.app.core.domain.model.ProviderSettings
import com.avachat.app.core.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * DataStore-backed settings repository. Reads always emit the current
 * snapshot; writes are atomic transactions.
 */
class SettingsRepository(private val dataStore: DataStore<Preferences>) {

    private object Keys {
        val API_KEY = stringPreferencesKey("api_key")
        val BASE_URL = stringPreferencesKey("base_url")
        val MODEL = stringPreferencesKey("model")
        val TEMPERATURE = doublePreferencesKey("temperature")
        val MAX_TOKENS = intPreferencesKey("max_tokens")
        val STREAMING = booleanPreferencesKey("streaming")
        val THEME = stringPreferencesKey("theme")
        val LANGUAGE = stringPreferencesKey("language")
        val DYNAMIC_COLORS = booleanPreferencesKey("dynamic_colors")
        val SHOW_TIMESTAMPS = booleanPreferencesKey("show_timestamps")
        val AUTO_SCROLL = booleanPreferencesKey("auto_scroll")
        val MARKDOWN = booleanPreferencesKey("markdown_enabled")
    }

    val settings: Flow<AppSettings> = dataStore.data.map { prefs ->
        AppSettings(
            provider = ProviderSettings(
                apiKey = prefs[Keys.API_KEY] ?: "",
                baseUrl = prefs[Keys.BASE_URL] ?: "",
                model = prefs[Keys.MODEL] ?: "",
                temperature = prefs[Keys.TEMPERATURE] ?: 0.7,
                maxTokens = prefs[Keys.MAX_TOKENS] ?: 1024,
                streaming = prefs[Keys.STREAMING] ?: true
            ),
            themeMode = runCatching {
                ThemeMode.valueOf(prefs[Keys.THEME] ?: ThemeMode.SYSTEM.name)
            }.getOrDefault(ThemeMode.SYSTEM),
            language = runCatching {
                AppLanguage.valueOf(prefs[Keys.LANGUAGE] ?: AppLanguage.SYSTEM.name)
            }.getOrDefault(AppLanguage.SYSTEM),
            dynamicColors = prefs[Keys.DYNAMIC_COLORS] ?: true,
            chat = ChatBehaviorSettings(
                showTimestamps = prefs[Keys.SHOW_TIMESTAMPS] ?: true,
                autoScroll = prefs[Keys.AUTO_SCROLL] ?: true,
                markdownEnabled = prefs[Keys.MARKDOWN] ?: true
            )
        )
    }

    suspend fun saveProvider(provider: ProviderSettings) {
        dataStore.edit { prefs ->
            prefs[Keys.API_KEY] = provider.apiKey
            prefs[Keys.BASE_URL] = provider.baseUrl.trim()
            prefs[Keys.MODEL] = provider.model.trim()
            prefs[Keys.TEMPERATURE] = provider.temperature
            prefs[Keys.MAX_TOKENS] = provider.maxTokens
            prefs[Keys.STREAMING] = provider.streaming
        }
    }

    suspend fun saveAppearance(themeMode: ThemeMode, language: AppLanguage, dynamicColors: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.THEME] = themeMode.name
            prefs[Keys.LANGUAGE] = language.name
            prefs[Keys.DYNAMIC_COLORS] = dynamicColors
        }
    }

    suspend fun saveChatBehavior(chat: ChatBehaviorSettings) {
        dataStore.edit { prefs ->
            prefs[Keys.SHOW_TIMESTAMPS] = chat.showTimestamps
            prefs[Keys.AUTO_SCROLL] = chat.autoScroll
            prefs[Keys.MARKDOWN] = chat.markdownEnabled
        }
    }

    suspend fun resetAll() {
        dataStore.edit { it.clear() }
    }
}
