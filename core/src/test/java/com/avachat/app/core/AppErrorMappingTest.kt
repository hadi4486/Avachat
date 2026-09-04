package com.avachat.app.core

import com.avachat.app.core.domain.error.AppError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppErrorMappingTest {

    @Test
    fun `http 401 maps to unauthorized`() {
        val error = AppError.fromHttp(401, """{"error":{"message":"Invalid key"}}""")
        assertTrue(error is AppError.Unauthorized)
    }

    @Test
    fun `http 429 maps to rate limited`() {
        assertTrue(AppError.fromHttp(429, null) is AppError.RateLimited)
    }

    @Test
    fun `http 404 maps to not found`() {
        assertTrue(AppError.fromHttp(404, null) is AppError.NotFound)
    }

    @Test
    fun `http 500 maps to server`() {
        val error = AppError.fromHttp(503, null)
        assertTrue(error is AppError.Server)
        assertEquals(503, (error as AppError.Server).code)
    }

    @Test
    fun `timeout exception maps to timeout error`() {
        val error = AppError.fromThrowable(java.net.SocketTimeoutException("x"))
        assertTrue(error is AppError.Timeout)
    }

    @Test
    fun `dns failure maps to network error`() {
        val error = AppError.fromThrowable(java.net.UnknownHostException("nope.example.com"))
        assertTrue(error is AppError.Network)
    }

    @Test
    fun `ssl exception maps to ssl error`() {
        val error = AppError.fromThrowable(javax.net.ssl.SSLHandshakeException("cert"))
        assertTrue(error is AppError.Ssl)
    }

    @Test
    fun `serialization exception maps to malformed`() {
        val error = AppError.fromThrowable(kotlinx.serialization.SerializationException("bad json"))
        assertTrue(error is AppError.MalformedResponse)
    }

    @Test
    fun `json detail is extracted into message for bad request`() {
        val error = AppError.fromHttp(400, """{"error":{"message":"model not real"}}""")
        assertTrue(error is AppError.BadRequest)
    }
}
