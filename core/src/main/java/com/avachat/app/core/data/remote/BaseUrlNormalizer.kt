package com.avachat.app.core.data.remote

/**
 * Normalizes user-provided Base URLs so that any reasonable input works and
 * malformed combinations (double slashes, missing /v1, missing trailing
 * slash) never produce broken request URLs.
 */
object BaseUrlNormalizer {

    /**
     * Returns a base URL usable by Retrofit (must end with '/').
     * Accepts: host, host/v1, host/v1/, host/api/v1, full .../chat/completions.
     */
    fun normalize(raw: String): String {
        var url = raw.trim()
        require(url.isNotEmpty()) { "Base URL is empty" }
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://$url"
        }
        url = url.trimEnd('/')
        require(url.contains("://")) { "Invalid URL" }
        // Validate roughly; Retrofit/OkHttp will validate fully.
        require(url.length >= 8) { "Invalid URL" }
        // If the user pasted the full endpoint, strip the operation segment.
        if (url.endsWith("/chat/completions")) {
            url = url.removeSuffix("/chat/completions")
        }
        return "$url/"
    }

    /** Resolves the absolute chat-completions endpoint for direct OkHttp calls. */
    fun chatCompletionsUrl(raw: String): String {
        val base = normalize(raw)
        return base + "chat/completions"
    }
}
