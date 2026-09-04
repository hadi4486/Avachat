package com.avachat.app.core

import com.avachat.app.core.data.remote.ApiHttpException
import com.avachat.app.core.data.remote.StreamingChatClient
import com.avachat.app.core.data.remote.NetworkModule
import com.avachat.app.core.data.remote.dto.ChatCompletionRequest
import com.avachat.app.core.data.remote.dto.ChatMessageDto
import com.avachat.app.core.domain.error.AppError
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class StreamingClientTest {

    private lateinit var server: MockWebServer
    private lateinit var client: StreamingChatClient

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        client = StreamingChatClient(NetworkModule.okHttpClient())
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun request() = ChatCompletionRequest(
        model = "test-model",
        messages = listOf(ChatMessageDto("user", "hello"))
    )

    @Test
    fun `collects deltas until DONE`() = runTest {
        server.enqueue(
            MockResponse()
                .setBody(
                    "data: {\"choices\":[{\"delta\":{\"content\":\"He\"}}]}\n\n" +
                        "data: {\"choices\":[{\"delta\":{\"content\":\"llo\"}}]}\n\n" +
                        "data: [DONE]\n\n"
                )
                .setHeader("Content-Type", "text/event-stream")
        )
        val chunks = client.stream(
            baseUrl = server.url("/v1").toString(),
            apiKey = "test-key",
            request = request()
        ).toList()

        assertEquals(2, chunks.size)
        assertEquals("He", chunks[0].firstDeltaContent)
        assertEquals("llo", chunks[1].firstDeltaContent)
    }

    @Test
    fun `http error surfaces as AppError`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setBody("""{"error":{"message":"bad key"}}""")
        )
        val flow = client.stream(
            baseUrl = server.url("/v1").toString(),
            apiKey = "test-key",
            request = request()
        )
        val error = try {
            flow.toList(); null
        } catch (e: AppError) {
            e
        }
        assertTrue(error is AppError.Unauthorized)
    }

    @Test
    fun `server closing early completes flow normally`() = runTest {
        server.enqueue(
            MockResponse()
                .setBody("data: {\"choices\":[{\"delta\":{\"content\":\"partial\"}}]}\n\n")
        )
        val chunks = client.stream(
            baseUrl = server.url("/v1").toString(),
            apiKey = "k",
            request = request()
        ).toList()
        assertEquals("partial", chunks.first().firstDeltaContent)
    }
}
