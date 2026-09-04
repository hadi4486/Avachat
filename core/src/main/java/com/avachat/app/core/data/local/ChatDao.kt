package com.avachat.app.core.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "conversations", indices = [Index("updatedAt")])
data class ConversationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val pinned: Boolean,
    val favorite: Boolean,
    val model: String?
)

@Suppress("FunctionName")
@Entity(tableName = "messages", indices = [Index("conversationId"), Index("createdAt")])
data class MessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val role: String,
    val content: String,
    val createdAt: Long,
    val isError: Boolean
)
@Dao
interface ChatDao {


    @Query("SELECT * FROM conversations ORDER BY pinned DESC, updatedAt DESC")
    fun observeConversations(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE id = :id")
    suspend fun getConversation(id: String): ConversationEntity?

    @Query("SELECT * FROM conversations WHERE id = :id")
    fun observeConversation(id: String): Flow<ConversationEntity?>

    @Query(
        "SELECT * FROM conversations WHERE title LIKE '%' || :query || '%' " +
            "OR id IN (SELECT DISTINCT conversationId FROM messages WHERE content LIKE '%' || :query || '%') " +
            "ORDER BY pinned DESC, updatedAt DESC"
    )
    fun searchConversations(query: String): Flow<List<ConversationEntity>>

    @Upsert
    suspend fun upsertConversation(conversation: ConversationEntity)

    @Query("UPDATE conversations SET title = :title, updatedAt = :updatedAt WHERE id = :id")
    suspend fun renameConversation(id: String, title: String, updatedAt: Long)

    @Query("UPDATE conversations SET pinned = :pinned WHERE id = :id")
    suspend fun setPinned(id: String, pinned: Boolean)

    @Query("UPDATE conversations SET favorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: String, favorite: Boolean)

    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun deleteConversation(id: String)

    @Query("DELETE FROM conversations")
    suspend fun deleteAllConversations()

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY createdAt ASC, id ASC")
    fun observeMessages(conversationId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY createdAt ASC, id ASC")
    suspend fun getMessages(conversationId: String): List<MessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)

    @Update
    suspend fun updateMessage(message: MessageEntity)

    @Query("UPDATE messages SET content = :content WHERE id = :id")
    suspend fun updateMessageContent(id: String, content: String)

    @Query("UPDATE messages SET isError = :isError WHERE id = :id")
    suspend fun setMessageError(id: String, isError: Boolean)

    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun deleteMessage(id: String)

    @Query("DELETE FROM messages WHERE conversationId = :conversationId")
    suspend fun clearMessages(conversationId: String)

    @Query("SELECT COUNT(*) FROM messages WHERE conversationId = :conversationId")
    suspend fun messageCount(conversationId: String): Int
}
