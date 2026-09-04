package com.avachat.app.core.domain.model

/**
 * Domain-level settings for the AI provider. The API key never leaves this
 * object except to the configured endpoint's Authorization header, and it is
 * excluded from [toString] to keep it out of any accidental logging.
 */
data class ProviderSettings(
    val apiKey: String = "",
    val baseUrl: String = "",
    val model: String = "",
    val temperature: Double = 0.7,
    val maxTokens: Int = 1024,
    val streaming: Boolean = true
) {
    val isConfigured: Boolean
        get() = apiKey.isNotBlank() && baseUrl.isNotBlank() && model.isNotBlank()

    /** Masked form for UI display, e.g. "sk-abc………wxyz" or bullets. */
    val maskedApiKey: String
        get() = when {
            apiKey.isBlank() -> ""
            apiKey.length <= 8 -> "•".repeat(apiKey.length)
            else -> apiKey.take(3) + "•".repeat(12) + apiKey.takeLast(3)
        }

    override fun toString(): String =
        "ProviderSettings(apiKey=${if (apiKey.isBlank()) "<empty>" else "<redacted>"}, baseUrl=$baseUrl, model=$model, temperature=$temperature, maxTokens=$maxTokens, streaming=$streaming)"
}

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class AppLanguage { SYSTEM, ENGLISH, PERSIAN }

data class ChatBehaviorSettings(
    val showTimestamps: Boolean = true,
    val autoScroll: Boolean = true,
    val markdownEnabled: Boolean = true
)

data class AppSettings(
    val provider: ProviderSettings = ProviderSettings(),
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val language: AppLanguage = AppLanguage.SYSTEM,
    val dynamicColors: Boolean = true,
    val chat: ChatBehaviorSettings = ChatBehaviorSettings()
)
