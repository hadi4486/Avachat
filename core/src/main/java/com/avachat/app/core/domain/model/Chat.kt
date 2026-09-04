package com.avachat.app.core.domain.model

import java.util.UUID

enum class Role { USER, ASSISTANT, SYSTEM }

data class Message(
    val id: String = UUID.randomUUID().toString(),
    val conversationId: String,
    val role: Role,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
    val isError: Boolean = false
)

data class Conversation(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val pinned: Boolean = false,
    val favorite: Boolean = false,
    val model: String? = null
) {
    val displayTitle: String
        get() = title.ifBlank { "New Chat" }
}
