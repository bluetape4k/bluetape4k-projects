package io.bluetape4k.ktor.core

import io.bluetape4k.support.requireNotBlank
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.Serializable
import java.io.Serializable as JavaSerializable

/**
 * Standard JSON error payload for Ktor applications.
 *
 * ## Contract
 * - [error] is a stable machine-readable code.
 * - [message] is safe to expose to API clients.
 * - [status] mirrors the HTTP status code sent by Ktor.
 *
 * ```kotlin
 * val response = ApiErrorResponse.of(
 *     status = HttpStatusCode.BadRequest,
 *     error = "bad_request",
 *     message = "Invalid query parameter"
 * )
 * ```
 */
@Serializable
data class ApiErrorResponse(
    val error: String,
    val message: String,
    val status: Int,
    val path: String? = null,
): JavaSerializable {

    init {
        error.requireNotBlank("error")
        message.requireNotBlank("message")
    }

    companion object {
        private const val serialVersionUID: Long = 1L

        /**
         * Creates an [ApiErrorResponse] from a Ktor [HttpStatusCode].
         */
        fun of(
            status: HttpStatusCode,
            error: String,
            message: String,
            path: String? = null,
        ): ApiErrorResponse =
            ApiErrorResponse(
                error = error,
                message = message,
                status = status.value,
                path = path
            )
    }
}
