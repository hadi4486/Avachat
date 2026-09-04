package com.avachat.app.core

import com.avachat.app.core.data.remote.dto.ChatCompletionRequest
import com.avachat.app.core.data.remote.dto.ChatCompletionResponse
import com.avachat.app.core.data.remote.dto.StreamChunkDto
import com.avachat.app.core.data.remote.ChatCompletionsApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatDtoTest {

    private val json = ChatCompletionsApi.json()

    @Test
    fun `request serializes with max_tokens snake_case and omits nulls`() {
        val encoded = json.encodeToString(
            ChatCompletionRequest.serializer(),
            ChatCompletionRequest(
                model = "gpt-test",
                messages = listOf(com.avachat.app.data.remote.dto.ChatMessageDto("user", "hi")),
                temperature = 0.5,
                maxTokens = 128,
                stream = false
            )
        )
        assertTrue(encoded.contains("\"max_tokens\":128"))
        assertTrue(encoded.contains("\"model\":\"gpt-test\""))
        assertTrue(!encoded.contains("null"))
    }

    @Test
    fun `response parses content`() {
        val payload = """
            {"id":"abc","object":"chat.completion","model":"gpt-test",
             "choices":[{"index":0,"message":{"role":"assistant","content":"Hello!"},"finish_reason":"stop"}],
             "usage":{"prompt_tokens":3,"completion_tokens":2,"total_tokens":5}}
        """.trimIndent()
        val decoded = json.decodeFromString(ChatCompletionResponse.serializer(), payload)
        assertEquals("Hello!", decoded.firstContent)
    }

    @Test
    fun `response with unknown fields parses without crash`() {
        val payload = """
            {"id":"abc","some_future_field":{"nested":[1,2,3]},"choices":[]}
        """.trimIndent()
        val decoded = json.decodeFromString(ChatCompletionResponse.serializer(), payload)
        assertEquals(0, decoded.choices.size)
    }

    @Test
    fun `stream chunk parses delta content`() {
        val payload = """{"choices":[{"delta":{"content":"Hel"}}]}"""
        val chunk = json.decodeFromString(StreamChunkDto.serializer(), payload)
        assertEquals("Hel", chunk.firstDeltaContent)
    }

    @Test
    fun `stream chunk with role-only delta yields null content`() {
        val payload = """{"choices":[{"delta":{"role":"assistant"}}]}"""
        val chunk = json.decodeFromString(StreamChunkDto.serializer(), payload)
        assertEquals(null, chunk.firstDeltaContent)
    }

    @Test
    fun `error object parses`() {
        val payload = """{"error":{"message":"bad key","type":"auth","code":"401"}}"""
        val chunk = json.decodeFromString(StreamChunkDto.serializer(), payload)
        assertEquals("bad key", chunk.error?.message)
    }
}
