package com.avachat.app.core.presentation.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.avachat.app.core.data.repository.ChatRepository
import com.avachat.app.core.domain.error.AppError
import com.avachat.app.core.domain.model.AppSettings
import com.avachat.app.core.domain.model.Conversation
import com.avachat.app.core.domain.model.Message
import com.avachat.app.core.domain.model.Role
import com.avachat.app.core.domain.repository.AiChatRepository
import com.avachat.app.core.domain.repository.StreamEvent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class ChatUiState(
    val conversationId: String? = null,
    val messages: List<Message> = emptyList(),
    val conversationTitle: String = "",
    val generating: Boolean = false,
    val streamingDraft: String = "",
    val offline: Boolean = false,
    val model: String = "",
    val lastError: AppError? = null,
    val pendingUserText: String = ""
)

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModel(
    private val chatRepository: ChatRepository,
    private val aiRepository: AiChatRepository,
    settingsRepository: com.avachat.app.core.data.settings.SettingsRepository,
    private val isOnline: () -> Boolean
) : ViewModel() {

    // Re-read on every call so streaming/settings changes apply immediately.
    private val settingsFlow = settingsRepository.settings
    private val settings: AppSettings get() = latestSettings
    private var latestSettings: AppSettings = AppSettings()

    init {
        // Keep the latest snapshot for synchronous access.
        settingsFlow.onEach { latestSettings = it }
            .launchIn(viewModelScope)
        viewModelScope.launch {
            while (true) {
                _offline.value = !isOnline()
                delay(10_000)
            }
        }
    }

    private val _currentConversationId = MutableStateFlow<String?>(null)
    private val _generating = MutableStateFlow(false)
    private val _streamingDraft = MutableStateFlow("")
    private val _offline = MutableStateFlow(false)
    private val _lastError = MutableStateFlow<AppError?>(null)
    private val _pendingUserText = MutableStateFlow("")
    private val _conversationTitle = MutableStateFlow("")

    private var generationJob: Job? = null

    val uiState: StateFlow<ChatUiState> = combine(
        _currentConversationId.flatMapLatest { id ->
            if (id == null) flowOf(emptyList()) else chatRepository.observeMessages(id)
        },
        _currentConversationId.flatMapLatest { id ->
            if (id == null) flowOf<Conversation?>(null) else chatRepository.observeConversation(id)
        },
        combine(
            _generating,
            _streamingDraft,
            _offline,
            _lastError,
            _pendingUserText
        ) { generating, draft, offline, lastError, pendingText ->
            ChatExtras(generating, draft, offline, lastError, pendingText)
        }
    ) { messages, conversation, extras ->
        ChatUiState(
            conversationId = _currentConversationId.value,
            messages = messages,
            conversationTitle = conversation?.displayTitle ?: _conversationTitle.value,
            generating = extras.generating,
            streamingDraft = extras.draft,
            offline = extras.offline,
            lastError = extras.lastError,
            pendingUserText = extras.pendingText,
            model = latestSettings.provider.model
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ChatUiState())

    /** Flattened extras bundle so the outer combine stays within 3 flows. */
    private data class ChatExtras(
        val generating: Boolean,
        val draft: String,
        val offline: Boolean,
        val lastError: AppError?,
        val pendingText: String
    )

    init {
        viewModelScope.launch {
            while (true) {
                _offline.value = !isOnline()
                delay(10_000)
            }
        }
    }

    fun openConversation(id: String) {
        cancelGeneration()
        _lastError.value = null
        _conversationTitle.value = ""
        _currentConversationId.value = id
    }

    fun newChat() {
        cancelGeneration()
        _lastError.value = null
        _conversationTitle.value = ""
        _currentConversationId.value = UUID.randomUUID().toString()
    }

    fun setOffline(offline: Boolean) {
        _offline.value = offline
    }

    fun updatePendingText(text: String) {
        _pendingUserText.value = text
    }

    fun clearError() {
        _lastError.value = null
    }

    fun sendMessage(rawText: String) {
        val text = rawText.trim()
        if (text.isEmpty() || _generating.value) return

        if (!isOnline()) {
            _offline.value = true
            _pendingUserText.value = rawText // preserve unsent text
            return
        }

        val conversationId = _currentConversationId.value ?: UUID.randomUUID().toString().also {
            _currentConversationId.value = it
        }

        generationJob = viewModelScope.launch {
            val userMessage = Message(
                conversationId = conversationId,
                role = Role.USER,
                content = text
            )
            chatRepository.ensureConversation(conversationId, settings.provider.model)
            chatRepository.addMessage(userMessage)
            chatRepository.maybeAutoTitle(conversationId)
            _pendingUserText.value = ""
            runGeneration(conversationId)
        }
    }

    fun stopGeneration() {
        val draft = _streamingDraft.value
        val conversationId = _currentConversationId.value
        cancelGeneration()
        if (conversationId != null && draft.isNotBlank()) {
            viewModelScope.launch {
                // Persist the partial answer so nothing silently disappears.
                chatRepository.addMessage(
                    Message(conversationId = conversationId, role = Role.ASSISTANT, content = draft)
                )
                chatRepository.touchConversation(conversationId)
            }
        }
    }

    /** Regenerates the last assistant answer (removes it, asks again). */
    fun regenerate() {
        val conversationId = _currentConversationId.value ?: return
        if (_generating.value || !isOnline()) {
            if (!isOnline()) _offline.value = true
            return
        }
        generationJob = viewModelScope.launch {
            val messages = chatRepository.getMessages(conversationId)
            messages.lastOrNull { it.role == Role.ASSISTANT && !it.isError }
                ?.let { chatRepository.deleteMessage(it.id) }
            runGeneration(conversationId)
        }
    }

    /** Retries after an error bubble (removes the error message). */
    fun retry() {
        val conversationId = _currentConversationId.value ?: return
        if (_generating.value || !isOnline()) {
            if (!isOnline()) _offline.value = true
            return
        }
        generationJob = viewModelScope.launch {
            chatRepository.getMessages(conversationId)
                .lastOrNull { it.isError }
                ?.let { chatRepository.deleteMessage(it.id) }
            runGeneration(conversationId)
        }
    }

    fun deleteMessage(id: String) {
        viewModelScope.launch { chatRepository.deleteMessage(id) }
    }

    fun clearCurrentChat() {
        val conversationId = _currentConversationId.value ?: return
        cancelGeneration()
        viewModelScope.launch { chatRepository.clearMessages(conversationId) }
    }

    private fun cancelGeneration() {
        generationJob?.cancel()
        generationJob = null
        _generating.value = false
        _streamingDraft.value = ""
    }

    private suspend fun runGeneration(conversationId: String) {
        _generating.value = true
        _streamingDraft.value = ""
        _lastError.value = null

        try {
            val history = chatRepository.getMessages(conversationId)
                .filter { !it.isError }
                .takeLast(30)

            val useStreaming = settings.provider.streaming
            if (useStreaming) {
                var receivedAny = false
                aiRepository.sendChat(settings.provider, history, streaming = true)
                    .collect { event ->
                        when (event) {
                            is StreamEvent.Delta -> {
                                receivedAny = true
                                _streamingDraft.value += event.text
                            }
                            is StreamEvent.Finished -> {
                                val text = event.fullText.ifBlank { _streamingDraft.value }
                                _streamingDraft.value = ""
                                if (text.isBlank() && !receivedAny) throw AppError.EmptyResponse
                                chatRepository.addMessage(
                                    Message(
                                        conversationId = conversationId,
                                        role = Role.ASSISTANT,
                                        content = text
                                    )
                                )
                                chatRepository.touchConversation(conversationId)
                            }
                        }
                    }
            } else {
                val result = try {
                    aiRepository.sendChat(settings.provider, history, streaming = false).first()
                } catch (e: NoSuchElementException) {
                    throw AppError.EmptyResponse
                }
                when (result) {
                    is StreamEvent.Finished -> {
                        if (result.fullText.isBlank()) throw AppError.EmptyResponse
                        chatRepository.addMessage(
                            Message(
                                conversationId = conversationId,
                                role = Role.ASSISTANT,
                                content = result.fullText
                            )
                        )
                        chatRepository.touchConversation(conversationId)
                    }
                    is StreamEvent.Delta -> Unit // unreachable for non-streaming
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: AppError) {
            showError(e, conversationId)
        } catch (e: Exception) {
            showError(AppError.fromThrowable(e), conversationId)
        } finally {
            _generating.value = false
            _streamingDraft.value = ""
        }
    }

    private suspend fun showError(error: AppError, conversationId: String) {
        _lastError.value = error
        chatRepository.addMessage(
            Message(conversationId = conversationId, role = Role.ASSISTANT, content = "", isError = true)
        )
    }

    class Factory(
        private val chatRepository: ChatRepository,
        private val aiRepository: AiChatRepository,
        private val settingsRepository: com.avachat.app.core.data.settings.SettingsRepository,
        private val isOnline: () -> Boolean
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ChatViewModel(chatRepository, aiRepository, settingsRepository, isOnline) as T
    }
}
