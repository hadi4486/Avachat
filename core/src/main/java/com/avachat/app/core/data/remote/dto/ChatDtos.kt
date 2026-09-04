package com.avachat.app.core.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * OpenAI-compatible chat completion request DTOs.
 * Kept intentionally minimal and provider-neutral.
 */
@Serializable
data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessageDto>,
    val temperature: Double? = null,
    @SerialName("max_tokens") val maxTokens: Int? = null,
    val stream: Boolean = false
)

@Serializable
data class ChatMessageDto(
    val role: String,
    val content: String
)

/**
 * Non-streaming response. Fields are nullable because compatible providers
 * vary in what they return; parsing must never crash.
 */
@Serializable
data class ChatCompletionResponse(
    val id: String? = null,
    val `object`: String? = null,
    val model: String? = null,
    val choices: List<ChoiceDto> = emptyList(),
    val usage: UsageDto? = null,
    val error: ApiErrorDto? = null
) {
    val firstContent: String?
        get() = choices.firstOrNull()?.message?.content
}

@Serializable
data class ChoiceDto(
    val index: Int? = null,
    val message: ChatMessageDto? = null,
    @SerialName("finish_reason") val finishReason: String? = null,
    val delta: ChatMessageDto? = null
)

@Serializable
data class UsageDto(
    @SerialName("prompt_tokens") val promptTokens: Int? = null,
    @SerialName("completion_tokens") val completionTokens: Int? = null,
    @SerialName("total_tokens") val totalTokens: Int? = null
)

@Serializable
data class ApiErrorDto(
    val message: String? = null,
    val type: String? = null,
    val code: String? = null
)

/**
 * A single Server-Sent Event payload chunk: `data: {...}`.
 */
@Serializable
data class StreamChunkDto(
    val id: String? = null,
    val model: String? = null,
    val choices: List<ChoiceDto> = emptyList(),
    val error: ApiErrorDto? = null
) {
    val firstDeltaContent: String?
        get() = choices.firstOrNull()?.delta?.content
}
