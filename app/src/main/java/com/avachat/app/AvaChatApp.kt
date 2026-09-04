package com.avachat.app

import android.app.Application
import androidx.datastore.preferences.preferencesDataStore
import com.avachat.app.core.data.local.ChatDatabase
import com.avachat.app.core.data.remote.NetworkModule
import com.avachat.app.core.data.repository.ChatRepository
import com.avachat.app.core.data.settings.SettingsRepository
import com.avachat.app.core.domain.model.AppLanguage
import com.avachat.app.core.domain.repository.AiChatRepository
import androidx.core.os.LocaleListCompat
import androidx.appcompat.app.AppCompatDelegate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

private val android.content.Context.settingsDataStore by androidx.datastore.preferences.preferencesDataStore(
    name = "avachat_settings"
)

/**
 * Minimal manual dependency graph; intentionally no DI framework to keep the
 * app lightweight.
 */
class AvaChatApp : Application() {

    lateinit var chatRepository: ChatRepository
        private set
    lateinit var aiRepository: AiChatRepository
        private set
    lateinit var settingsRepository: SettingsRepository
        private set

    override fun onCreate() {
        super.onCreate()
        val dao = ChatDatabase.get(this).chatDao()
        chatRepository = ChatRepository(dao)
        aiRepository = AiChatRepository(NetworkModule.okHttpClient())
        settingsRepository = SettingsRepository(settingsDataStore)

        // Apply persisted language at process start (per-app locales).
        val language = runBlocking { settingsRepository.settings.first().language }
        applyAppLanguage(language)
    }

    companion object {
        fun applyAppLanguage(language: AppLanguage) {
            val tag = when (language) {
                AppLanguage.SYSTEM -> null
                AppLanguage.ENGLISH -> "en"
                AppLanguage.PERSIAN -> "fa"
            }
            AppCompatDelegate.setApplicationLocales(
                if (tag == null) LocaleListCompat.getEmptyLocaleList()
                else LocaleListCompat.forLanguageTags(tag)
            )
        }
    }
}
