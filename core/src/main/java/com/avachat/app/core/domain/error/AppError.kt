package com.avachat.app.core.domain.error

/**
 * Centralized error model. Technical exceptions are translated once, here,
 * into stable categories the UI can localize and render.
 */
sealed class AppError : Exception() {

    abstract val userMessageKey: String

    data class Network(override val cause: Throwable? = null) : AppError() {
        override val userMessageKey: String = "error_network"
    }

    data class Timeout(override val cause: Throwable? = null) : AppError() {
        override val userMessageKey: String = "error_timeout"
    }

    data class Server(val code: Int, val detail: String? = null) : AppError() {
        override val userMessageKey: String = "error_server"
    }

    data class RateLimited(val retryAfterSeconds: Long? = null) : AppError() {
        override val userMessageKey: String = "error_rate_limited"
    }

    data class Unauthorized(val detail: String? = null) : AppError() {
        override val userMessageKey: String = "error_unauthorized"
    }

    data class Forbidden(val detail: String? = null) : AppError() {
        override val userMessageKey: String = "error_forbidden"
    }

    data class BadRequest(val detail: String? = null) : AppError() {
        override val userMessageKey: String = "error_bad_request"
    }

    data class NotFound(val detail: String? = null) : AppError() {
        override val userMessageKey: String = "error_not_found"
    }

    data class Ssl(override val cause: Throwable? = null) : AppError() {
        override val userMessageKey: String = "error_ssl"
    }

    data class MalformedResponse(override val cause: Throwable? = null) : AppError() {
        override val userMessageKey: String = "error_malformed"
    }

    data object EmptyResponse : AppError() {
        override val userMessageKey: String = "error_empty_response"
    }

    data class InvalidConfiguration(val reason: String) : AppError() {
        override val userMessageKey: String = "error_invalid_config"
    }

    data object Offline : AppError() {
        override val userMessageKey: String = "error_offline"
    }

    data class Unknown(override val cause: Throwable? = null) : AppError() {
        override val userMessageKey: String = "error_unknown"
    }

    companion object {
        fun fromHttp(code: Int, body: String?, jsonDetail: String? = null): AppError = when (code) {
            400 -> BadRequest(jsonDetail ?: body?.take(300))
            401 -> Unauthorized(jsonDetail ?: body?.take(300))
            403 -> Forbidden(jsonDetail ?: body?.take(300))
            404 -> NotFound(jsonDetail ?: body?.take(300))
            408 -> Timeout()
            409 -> Server(code, jsonDetail ?: body?.take(300))
            429 -> RateLimited()
            in 500..599 -> Server(code, jsonDetail ?: body?.take(300))
            else -> Server(code, jsonDetail ?: body?.take(300))
        }

        fun fromThrowable(t: Throwable): AppError = when {
            t is AppError -> t
            t is java.net.SocketTimeoutException -> Timeout(t)
            t is javax.net.ssl.SSLException -> Ssl(t)
            t is java.net.ConnectException || t is java.net.UnknownHostException -> Network(t)
            t is java.io.IOException -> Network(t)
            t is kotlinx.serialization.SerializationException -> MalformedResponse(t)
            else -> Unknown(t)
        }
    }
}
