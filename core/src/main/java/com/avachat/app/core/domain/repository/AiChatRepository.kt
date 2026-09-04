package com.avachat.app.core.domain.repository

import com.avachat.app.core.data.remote.ApiHttpException
import com.avachat.app.core.data.remote.BaseUrlNormalizer
import com.avachat.app.core.data.remote.ChatCompletionsApi
import com.avachat.app.core.data.remote.NetworkModule
import com.avachat.app.core.data.remote.StreamingChatClient
import com.avachat.app.core.data.remote.dto.ChatCompletionRequest
import com.avachat.app.core.data.remote.dto.ChatMessageDto
import com.avachat.app.core.data.remote.dto.StreamChunkDto
import com.avachat.app.core.domain.error.AppError
import com.avachat.app.core.domain.model.Message
import com.avachat.app.core.domain.model.ProviderSettings
import com.avachat.app.core.domain.model.Role
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

/**
 * Facade over the network layer. Every public call either succeeds or throws
 * a domain [AppError]; raw technical exceptions never escape to the UI.
 */
class AiChatRepository(
    private val okHttpClient: OkHttpClient,
    private val json: Json = ChatCompletionsApi.json()
) {

    fun sendChat(
        settings: ProviderSettings,
        history: List<Message>,
        streaming: Boolean
    ): Flow<StreamEvent> {
        if (!settings.isConfigured) {
            return flow { throw AppError.InvalidConfiguration("missing_api_config") }
        }
        val normalized = try {
            BaseUrlNormalizer.chatCompletionsUrl(settings.baseUrl)
        } catch (e: IllegalArgumentException) {
            return flow { throw AppError.InvalidConfiguration("invalid_base_url") }
        }
        val request = ChatCompletionRequest(
            model = settings.model,
            messages = history.map {
                ChatMessageDto(role = it.role.name.lowercase(), content = it.content)
            },
            temperature = settings.temperature,
            maxTokens = settings.maxTokens,
            stream = streaming
        )

        return if (streaming) {
            StreamingChatClient(okHttpClient, json).stream(settings.baseUrl, settings.apiKey, request)
                .mapStreamEvents()
        } else {
            flow { emit(nonStreamingCall(normalized, settings, request)) }
                .flowOn(Dispatchers.IO)
        }
    }

    /**
     * Lightweight connectivity check against the configured provider:
     * POST /chat/completions with a single "ping" message. Any valid HTTP
     * response proves reachability + auth.
     */
    suspend fun testConnection(settings: ProviderSettings): Unit = withContext(Dispatchers.IO) {
        val url = try {
            BaseUrlNormalizer.chatCompletionsUrl(settings.baseUrl)
        } catch (e: IllegalArgumentException) {
            throw AppError.InvalidConfiguration("invalid_base_url")
        }
        val body = json.encodeToString(
            ChatCompletionRequest.serializer(),
            ChatCompletionRequest(
                model = settings.model,
                messages = listOf(ChatMessageDto("user", "ping")),
                maxTokens = 1,
                stream = false
            )
        ).toRequestBody("application/json".toMediaType())

        val call = okHttpClient.newCall(
            Request.Builder()
                .url(url)
                .header("Authorization", "Bearer ${settings.apiKey}")
                .post(body)
                .build()
        )
        try {
            call.execute().use { response ->
                when {
                    response.isSuccessful -> return@withContext
                    else -> {
                        val raw = runCatching { response.body?.string() }.getOrNull()
                        val detail = runCatching {
                            json.parseToJsonElement(raw.orEmpty())
                                .jsonObjectOrNull()?.get("error")
                                ?.jsonObjectOrNull()?.get("message")?.jsonPrimitiveOrNull()?.content
                        }.getOrNull()
                        throw AppError.fromHttp(response.code, raw, detail)
                    }
                }
            }
        } catch (e: AppError) {
            throw e
        } catch (e: IOException) {
            throw AppError.fromThrowable(e)
        }
    }

    private suspend fun nonStreamingCall(
        url: String,
        settings: ProviderSettings,
        request: ChatCompletionRequest
    ): StreamEvent {
        val api = ChatCompletionsApi.create(settings.baseUrl, settings.apiKey, okHttpClient)
        return try {
            val response = api.chatCompletions(url, request)
            response.error?.let { err ->
                throw AppError.BadRequest(err.message ?: "provider_error")
            }
            val content = response.firstContent
            if (content.isNullOrBlank()) throw AppError.EmptyResponse
            StreamEvent.Finished(content)
        } catch (e: retrofit2.HttpException) {
            val raw = runCatching { e.response()?.errorBody()?.string() }.getOrNull()
            val detail = runCatching {
                json.parseToJsonElement(raw.orEmpty())
                    .jsonObjectOrNull()?.get("error")
                    ?.jsonObjectOrNull()?.get("message")?.jsonPrimitiveOrNull()?.content
            }.getOrNull()
            throw AppError.fromHttp(e.code(), raw, detail)
        } catch (e: AppError) {
            throw e
        } catch (e: Exception) {
            throw AppError.fromThrowable(e)
        }
    }

    private fun Flow<StreamChunkDto>.mapStreamEvents(): Flow<StreamEvent> = flow {
        val accumulated = StringBuilder()
        try {
            collect { chunk ->
                chunk.error?.let { throw AppError.BadRequest(it.message ?: "provider_error") }
                val delta = chunk.firstDeltaContent
                if (!delta.isNullOrEmpty()) {
                    accumulated.append(delta)
                    emit(StreamEvent.Delta(delta))
                }
            }
            emit(StreamEvent.Finished(accumulated.toString()))
        } catch (c: Throwable) {
            throw when (val e = c) {
                is AppError -> e
                is ApiHttpException -> AppError.fromHttp(e.code, e.errorBody)
                else -> AppError.fromThrowable(c)
            }
        }
    }.flowOn(Dispatchers.IO)

    private companion object {
        // helpers kept tiny to avoid extra imports of JsonElement operators
        private fun kotlinx.serialization.json.JsonElement.jsonObjectOrNull(): kotlinx.serialization.json.JsonObject? =
            this as? kotlinx.serialization.json.JsonObject

        private fun kotlinx.serialization.json.JsonElement.jsonPrimitiveOrNull(): kotlinx.serialization.json.JsonPrimitive? =
            this as? kotlinx.serialization.json.JsonPrimitive
    }
}

/** UI-level events emitted while a chat request is in flight. */
sealed class StreamEvent {
    data class Delta(val text: String) : StreamEvent()
    data class Finished(val fullText: String) : StreamEvent()
}
