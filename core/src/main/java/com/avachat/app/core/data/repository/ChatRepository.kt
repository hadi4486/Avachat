package com.avachat.app.core.data.repository

import com.avachat.app.core.data.local.ChatDao
import com.avachat.app.core.data.local.ConversationEntity
import com.avachat.app.core.data.local.MessageEntity
import com.avachat.app.core.domain.model.Conversation
import com.avachat.app.core.domain.model.Message
import com.avachat.app.core.domain.model.Role
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Single source of truth for conversations and messages. Persists everything
 * immediately so state survives process death, rotation, and restarts.
 */
class ChatRepository(private val dao: ChatDao) {

    fun observeConversations(): Flow<List<Conversation>> =
        dao.observeConversations().map { list -> list.map { it.toDomain() } }

    fun searchConversations(query: String): Flow<List<Conversation>> =
        dao.searchConversations(query).map { list -> list.map { it.toDomain() } }

    fun observeConversation(id: String): Flow<Conversation?> =
        dao.observeConversation(id).map { it?.toDomain() }

    fun observeMessages(conversationId: String): Flow<List<Message>> =
        dao.observeMessages(conversationId).map { list -> list.map { it.toDomain() } }

    suspend fun getConversation(id: String): Conversation? = dao.getConversation(id)?.toDomain()

    suspend fun getMessages(conversationId: String): List<Message> =
        dao.getMessages(conversationId).map { it.toDomain() }

    suspend fun upsertConversation(conversation: Conversation) {
        dao.upsertConversation(conversation.toEntity())
    }

    /** Creates a conversation if missing and returns its id. */
    suspend fun ensureConversation(conversationId: String, model: String? = null): String {
        val existing = dao.getConversation(conversationId)
        if (existing == null) {
            val now = System.currentTimeMillis()
            dao.upsertConversation(
                ConversationEntity(
                    id = conversationId,
                    title = "",
                    createdAt = now,
                    updatedAt = now,
                    pinned = false,
                    favorite = false,
                    model = model
                )
            )
        }
        return conversationId
    }

    suspend fun renameConversation(id: String, title: String) {
        dao.renameConversation(id, title.trim(), System.currentTimeMillis())
    }

    suspend fun setPinned(id: String, pinned: Boolean) = dao.setPinned(id, pinned)

    suspend fun setFavorite(id: String, favorite: Boolean) = dao.setFavorite(id, favorite)

    suspend fun deleteConversation(id: String) = dao.deleteConversation(id)

    suspend fun deleteAllConversations() = dao.deleteAllConversations()

    suspend fun addMessage(message: Message) = dao.insertMessage(message.toEntity())

    suspend fun updateMessageContent(id: String, content: String) =
        dao.updateMessageContent(id, content)

    suspend fun setMessageError(id: String, isError: Boolean) = dao.setMessageError(id, isError)

    suspend fun deleteMessage(id: String) = dao.deleteMessage(id)

    suspend fun clearMessages(conversationId: String) = dao.clearMessages(conversationId)

    suspend fun touchConversation(id: String) {
        val existing = dao.getConversation(id) ?: return
        dao.upsertConversation(existing.copy(updatedAt = System.currentTimeMillis()))
    }

    /** Auto-title from the first user message if the conversation is untitled. */
    suspend fun maybeAutoTitle(conversationId: String) {
        val conversation = dao.getConversation(conversationId) ?: return
        if (conversation.title.isNotBlank()) return
        val firstUser = dao.getMessages(conversationId).firstOrNull { it.role == Role.USER.name }
        if (firstUser != null) {
            val title = firstUser.content.trim().lineSequence().firstOrNull().orEmpty()
                .take(48)
            if (title.isNotBlank()) {
                dao.renameConversation(conversationId, title, System.currentTimeMillis())
            }
        }
    }
}

private fun ConversationEntity.toDomain() = Conversation(
    id = id,
    title = title,
    createdAt = createdAt,
    updatedAt = updatedAt,
    pinned = pinned,
    favorite = favorite,
    model = model
)

private fun MessageEntity.toDomain() = Message(
    id = id,
    conversationId = conversationId,
    role = runCatching { Role.valueOf(role) }.getOrDefault(Role.ASSISTANT),
    content = content,
    createdAt = createdAt,
    isError = isError
)

private fun Conversation.toEntity() = ConversationEntity(
    id = id,
    title = title,
    createdAt = createdAt,
    updatedAt = updatedAt,
    pinned = pinned,
    favorite = favorite,
    model = model
)

private fun Message.toEntity() = MessageEntity(
    id = id,
    conversationId = conversationId,
    role = role.name,
    content = content,
    createdAt = createdAt,
    isError = isError
)
