package com.avachat.app.core

import com.avachat.app.core.data.remote.BaseUrlNormalizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BaseUrlNormalizerTest {

    @Test
    fun `normalizes host without path`() {
        assertEquals("https://example.com/", BaseUrlNormalizer.normalize("https://example.com"))
    }

    @Test
    fun `normalizes host with v1`() {
        assertEquals("https://example.com/v1/", BaseUrlNormalizer.normalize("https://example.com/v1"))
    }

    @Test
    fun `keeps trailing slash`() {
        assertEquals("https://example.com/v1/", BaseUrlNormalizer.normalize("https://example.com/v1/"))
    }

    @Test
    fun `strips pasted chat completions endpoint`() {
        assertEquals(
            "https://example.com/v1/",
            BaseUrlNormalizer.normalize("https://example.com/v1/chat/completions")
        )
    }

    @Test
    fun `resolves chat completions url without double slash`() {
        assertEquals(
            "https://example.com/v1/chat/completions",
            BaseUrlNormalizer.chatCompletionsUrl("https://example.com/v1")
        )
        assertEquals(
            "https://example.com/v1/chat/completions",
            BaseUrlNormalizer.chatCompletionsUrl("https://example.com/v1/")
        )
        assertEquals(
            "https://example.com/v1/chat/completions",
            BaseUrlNormalizer.chatCompletionsUrl("https://example.com/v1/chat/completions")
        )
    }

    @Test
    fun `adds https scheme when missing`() {
        assertEquals("https://example.com/v1/", BaseUrlNormalizer.normalize("example.com/v1"))
    }

    @Test
    fun `rejects empty url`() {
        assertThrows(IllegalArgumentException::class.java) {
            BaseUrlNormalizer.normalize("   ")
        }
    }
}
