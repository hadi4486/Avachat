package com.avachat.app.core.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.avachat.app.core.data.repository.ChatRepository
import com.avachat.app.core.domain.model.Conversation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HistoryUiState(
    val conversations: List<Conversation> = emptyList(),
    val query: String = "",
    val loading: Boolean = true
)

class HistoryViewModel(
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")

    val uiState: StateFlow<HistoryUiState> = combine(
        _query.flatMapLatest { q ->
            if (q.isBlank()) chatRepository.observeConversations()
            else chatRepository.searchConversations(q.trim())
        },
        _query
    ) { conversations, query ->
        HistoryUiState(conversations = conversations, query = query, loading = false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

    fun search(query: String) {
        _query.value = query
    }

    fun rename(id: String, title: String) {
        if (title.isBlank()) return
        viewModelScope.launch { chatRepository.renameConversation(id, title) }
    }

    fun delete(id: String) {
        viewModelScope.launch { chatRepository.deleteConversation(id) }
    }

    fun deleteAll() {
        viewModelScope.launch { chatRepository.deleteAllConversations() }
    }

    fun togglePin(id: String, current: Boolean) {
        viewModelScope.launch { chatRepository.setPinned(id, !current) }
    }

    fun toggleFavorite(id: String, current: Boolean) {
        viewModelScope.launch { chatRepository.setFavorite(id, !current) }
    }

    class Factory(private val chatRepository: ChatRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            HistoryViewModel(chatRepository) as T
    }
}
