package com.avachat.app.ui.settings

import com.avachat.app.BuildConfig
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock as VisibilityIcon
import androidx.compose.material.icons.filled.Lock as VisibilityOffIcon
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.avachat.app.AvaChatApp
import com.avachat.app.R
import com.avachat.app.core.domain.model.AppLanguage
import com.avachat.app.core.domain.model.ThemeMode
import com.avachat.app.core.presentation.settings.ConnectionTestState
import com.avachat.app.core.presentation.settings.SettingsViewModel
import com.avachat.app.ui.components.LabeledSlider
import com.avachat.app.ui.components.SettingsRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsState()
    val provider = state.settings.provider
    val context = LocalContext.current

    // Local editable copy; committed on Save.
    var apiKey by remember(provider.apiKey) { mutableStateOf(provider.apiKey) }
    var baseUrl by remember(provider.baseUrl) { mutableStateOf(provider.baseUrl) }
    var model by remember(provider.model) { mutableStateOf(provider.model) }
    var temperature by remember(provider.temperature) { mutableStateOf(provider.temperature) }
    var maxTokens by remember(provider.maxTokens) { mutableStateOf(provider.maxTokens) }
    var streaming by remember(provider.streaming) { mutableStateOf(provider.streaming) }
    var showKey by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cancel)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionCard(title = stringResource(R.string.section_provider)) {
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text(stringResource(R.string.api_key)) },
                    singleLine = true,
                    visualTransformation = if (showKey) VisualTransformation.None
                    else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { showKey = !showKey }) {
                            Icon(
                                if (showKey) Icons.Filled.VisibilityOffIcon else Icons.Filled.VisibilityIcon,
                                contentDescription = stringResource(
                                    if (showKey) R.string.hide_api_key else R.string.show_api_key
                                )
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = { baseUrl = it },
                    label = { Text(stringResource(R.string.base_url)) },
                    singleLine = true,
                    supportingText = {
                        Text("https://api.example.com/v1", style = MaterialTheme.typography.bodySmall)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it },
                    label = { Text(stringResource(R.string.model)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                LabeledSlider(
                    label = stringResource(R.string.temperature),
                    value = temperature.toFloat(),
                    onValueChange = { temperature = it.toDouble() },
                    valueRange = 0f..2f,
                    steps = 19
                )
                LabeledSlider(
                    label = stringResource(R.string.max_tokens),
                    value = maxTokens.toFloat(),
                    onValueChange = { maxTokens = it.toInt() },
                    valueRange = 64f..8192f
                )
                SettingsRow(
                    title = stringResource(R.string.streaming),
                    checked = streaming,
                    onCheckedChange = { streaming = it }
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = {
                            viewModel.saveProvider(provider.copy(
                                apiKey = apiKey,
                                baseUrl = baseUrl,
                                model = model,
                                temperature = temperature,
                                maxTokens = maxTokens,
                                streaming = streaming
                            ))
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text(stringResource(R.string.save)) }
                    OutlinedButton(
                        onClick = viewModel::testConnection,
                        modifier = Modifier.weight(1f)
                    ) {
                        when (state.testState) {
                            ConnectionTestState.TESTING -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(Modifier.size(8.dp))
                                Text(stringResource(R.string.testing))
                            }
                            ConnectionTestState.SUCCESS -> Text(stringResource(R.string.connection_success))
                            ConnectionTestState.FAILED -> Text(stringResource(R.string.connection_failed))
                            ConnectionTestState.IDLE -> Text(stringResource(R.string.test_connection))
                        }
                    }
                }
                if (state.testState == ConnectionTestState.FAILED) {
                    val message = state.testMessage?.let { key ->
                        ErrorText(key)
                    }
                    if (message != null) message
                }
            }

            SectionCard(title = stringResource(R.string.section_appearance)) {
                val themeLabels = listOf(
                    stringResource(R.string.theme_system),
                    stringResource(R.string.theme_light),
                    stringResource(R.string.theme_dark)
                )
                Text(stringResource(R.string.theme), style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeMode.entries.forEachIndexed { index, mode ->
                        FilterChip(
                            selected = state.settings.themeMode == mode,
                            onClick = {
                                viewModel.saveAppearance(
                                    mode,
                                    state.settings.language,
                                    state.settings.dynamicColors
                                )
                            },
                            label = { Text(themeLabels[index]) }
                        )
                    }
                }
                Text(stringResource(R.string.language), style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppLanguage.entries.forEach { language ->
                        FilterChip(
                            selected = state.settings.language == language,
                            onClick = {
                                viewModel.saveAppearance(
                                    state.settings.themeMode,
                                    language,
                                    state.settings.dynamicColors
                                )
                                AvaChatApp.applyAppLanguage(language)
                            },
                            label = {
                                Text(
                                    when (language) {
                                        AppLanguage.SYSTEM -> stringResource(R.string.language_system)
                                        AppLanguage.ENGLISH -> stringResource(R.string.language_english)
                                        AppLanguage.PERSIAN -> stringResource(R.string.language_persian)
                                    }
                                )
                            }
                        )
                    }
                }
                SettingsRow(
                    title = stringResource(R.string.dynamic_colors),
                    checked = state.settings.dynamicColors,
                    onCheckedChange = { checked ->
                        viewModel.saveAppearance(
                            state.settings.themeMode,
                            state.settings.language,
                            checked
                        )
                    }
                )
            }

            SectionCard(title = stringResource(R.string.section_chat)) {
                SettingsRow(
                    title = stringResource(R.string.show_timestamps),
                    checked = state.settings.chat.showTimestamps,
                    onCheckedChange = { checked ->
                        viewModel.saveChatBehavior(
                            state.settings.chat.copy(showTimestamps = checked)
                        )
                    }
                )
                SettingsRow(
                    title = stringResource(R.string.auto_scroll),
                    checked = state.settings.chat.autoScroll,
                    onCheckedChange = { checked ->
                        viewModel.saveChatBehavior(
                            state.settings.chat.copy(autoScroll = checked)
                        )
                    }
                )
                SettingsRow(
                    title = stringResource(R.string.markdown),
                    checked = state.settings.chat.markdownEnabled,
                    onCheckedChange = { checked ->
                        viewModel.saveChatBehavior(
                            state.settings.chat.copy(markdownEnabled = checked)
                        )
                    }
                )
            }

            SectionCard(title = stringResource(R.string.section_storage)) {
                OutlinedButton(
                    onClick = {
                        viewModel.resetAllSettings()
                        confirmReset = false
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.reset_settings)) }
            }

            SectionCard(title = stringResource(R.string.section_about)) {
                Row {
                    Text(stringResource(R.string.version))
                    Spacer(Modifier.size(8.dp))
                    Text(
                        BuildConfig.VERSION_NAME,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            TextButton(
                onClick = { confirmReset = true },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) { Text(stringResource(R.string.reset_application)) }
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text(stringResource(R.string.reset_settings_title)) },
            confirmButton = {
                TextButton(onClick = { viewModel.resetAllSettings(); confirmReset = false }) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun ErrorText(userMessageKey: String) {
    val resId = when (userMessageKey) {
        "error_network" -> R.string.error_network
        "error_timeout" -> R.string.error_timeout
        "error_rate_limited" -> R.string.error_rate_limited
        "error_unauthorized" -> R.string.error_unauthorized
        "error_forbidden" -> R.string.error_forbidden
        "error_bad_request" -> R.string.error_bad_request
        "error_not_found" -> R.string.error_not_found
        "error_ssl" -> R.string.error_ssl
        "error_malformed" -> R.string.error_malformed
        "error_empty_response" -> R.string.error_empty_response
        "error_invalid_config" -> R.string.error_invalid_config
        "error_offline" -> R.string.error_offline
        else -> R.string.error_unknown
    }
    Text(
        text = stringResource(resId),
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodyMedium
    )
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            content()
        }
    }
}
