package com.avachat.app.core.data.remote

import com.avachat.app.core.data.remote.dto.ChatCompletionRequest
import com.avachat.app.core.data.remote.dto.ChatCompletionResponse
import com.avachat.app.core.data.remote.dto.StreamChunkDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.serialization.json.Json
import java.io.IOException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Url

/**
 * Provider-independent Retrofit interface. The full chat-completions URL is
 * resolved at call time from the user-configured Base URL, so any
 * OpenAI-compatible provider works without code changes.
 */
interface ChatCompletionsApi {

    @POST
    suspend fun chatCompletions(
        @Url url: String,
        @Body body: ChatCompletionRequest
    ): ChatCompletionResponse

    companion object {
        fun create(baseUrl: String, apiKey: String, client: OkHttpClient): ChatCompletionsApi {
            val normalized = BaseUrlNormalizer.normalize(baseUrl)
            return Retrofit.Builder()
                .baseUrl(normalized)
                .client(client)
                .addConverterFactory(
                    json().asConverterFactory("application/json".toMediaType())
                )
                .build()
                .create(ChatCompletionsApi::class.java)
        }

        fun json(): Json = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
            isLenient = true
            coerceInputValues = true
            encodeDefaults = false
        }
    }
}

/**
 * Streaming client implemented directly on OkHttp so the SSE byte stream can
 * be consumed incrementally and coroutine cancellation aborts the HTTP call
 * mid-stream.
 */
class StreamingChatClient(
    private val httpClient: OkHttpClient,
    private val json: Json = ChatCompletionsApi.json()
) {

    /**
     * Emits decoded [StreamChunkDto]s as they arrive. The terminal `[DONE]`
     * event closes the flow normally; IO/HTTP errors surface as exceptions
     * which the repository maps to domain errors.
     */
    fun stream(
        baseUrl: String,
        apiKey: String,
        request: ChatCompletionRequest
    ): Flow<StreamChunkDto> = callbackFlow {
        val url = BaseUrlNormalizer.chatCompletionsUrl(baseUrl)
        val body = json.encodeToString(
            ChatCompletionRequest.serializer(),
            request.copy(stream = true)
        ).toRequestBody("application/json".toMediaType())

        val call = httpClient.newCall(
            Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $apiKey")
                .header("Accept", "text/event-stream")
                .post(body)
                .build()
        )

        // Cancellation from downstream must abort the HTTP call immediately.
        val worker = Thread {
            try {
                call.execute().use { response ->
                    if (!response.isSuccessful) {
                        val errorBody = runCatching { response.body?.string() }.getOrNull()
                        close(ApiHttpException(code = response.code, errorBody = errorBody))
                        return@Thread
                    }
                    val source = response.body?.byteStream() ?: run {
                        close(IllegalStateException("Empty response body"))
                        return@Thread
                    }
                    // SSE parsing: events separated by blank lines; data lines start with "data:".
                    val reader = source.bufferedReader(Charsets.UTF_8)
                    val dataBuffer = StringBuilder()
                    while (true) {
                        val line = reader.readLine() ?: break
                        when {
                            line.startsWith("data:") -> {
                                dataBuffer.append(line.removePrefix("data:").trimStart())
                            }
                            line.isEmpty() && dataBuffer.isNotEmpty() -> {
                                val payload = dataBuffer.toString()
                                dataBuffer.setLength(0)
                                if (payload == DONE_MARKER) {
                                    close()
                                    return@Thread
                                }
                                val chunk = runCatching { json.decodeFromString(StreamChunkDto.serializer(), payload) }
                                    .getOrNull() ?: continue // ignore keep-alive/comment noise
                                trySendBlocking(chunk).getOrNull()
                            }
                        }
                    }
                    close() // server closed without [DONE]; still a normal completion
                }
            } catch (t: Throwable) {
                close(t)
            }
        }
        worker.isDaemon = true
        worker.name = "sse-reader"
        worker.start()

        awaitClose {
            call.cancel() // stops the blocking read and kills the socket
            if (worker.isAlive) {
                worker.interrupt()
            }
        }
    }

    private companion object {
        const val DONE_MARKER = "[DONE]"
    }
}

/** HTTP-level failure carrying the status code and (sanitized) server body. */
class ApiHttpException(
    val code: Int,
    val errorBody: String? = null
) : IOException("HTTP $code")
