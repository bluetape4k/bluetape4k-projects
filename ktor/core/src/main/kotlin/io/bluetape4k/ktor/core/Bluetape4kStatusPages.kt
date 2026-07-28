package io.bluetape4k.ktor.core

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.plugins.statuspages.StatusPagesConfig
import io.ktor.server.request.path
import io.ktor.server.response.respond
import kotlinx.coroutines.CancellationException

/**
 * 일반 application exception에 대한 기본 JSON error response를 등록합니다.
 *
 * ## Contract
 * - [IllegalArgumentException] is mapped to HTTP 400.
 * - Unhandled exceptions are mapped to HTTP 500.
 * - [CancellationException] is rethrown so structured concurrency is preserved.
 *
 * ```kotlin
 * install(StatusPages) {
 *     bluetape4kErrorResponses()
 * }
 * ```
 */
fun StatusPagesConfig.bluetape4kErrorResponses() {
    exception<IllegalArgumentException> { call, cause ->
        call.respondApiError(
            status = HttpStatusCode.BadRequest,
            error = "bad_request",
            message = cause.message ?: "Bad request"
        )
    }
    exception<Throwable> { call, cause ->
        if (cause is CancellationException) {
            throw cause
        }
        call.respondApiError(
            status = HttpStatusCode.InternalServerError,
            error = "internal_server_error",
            message = "Internal server error"
        )
    }
}

/**
 * 표준 bluetape4k JSON error payload로 응답합니다.
 */
suspend fun ApplicationCall.respondApiError(
    status: HttpStatusCode,
    error: String,
    message: String,
) {
    respond(
        status = status,
        message = ApiErrorResponse.of(
            status = status,
            error = error,
            message = message,
            path = request.path()
        )
    )
}
