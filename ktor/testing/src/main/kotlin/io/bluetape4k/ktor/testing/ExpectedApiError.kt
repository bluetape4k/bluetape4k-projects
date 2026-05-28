package io.bluetape4k.ktor.testing

import io.bluetape4k.support.requireNotBlank
import io.ktor.http.HttpStatusCode
import java.io.Serializable

/**
 * Expected standard bluetape4k API error payload for Ktor response assertions.
 *
 * ## Contract
 * - [status] is matched against both the HTTP response and JSON payload status.
 * - [error] is the stable machine-readable error code.
 * - [message] and [path] are verified only when present.
 */
data class ExpectedApiError(
    val status: HttpStatusCode,
    val error: String,
    val message: String? = null,
    val path: String? = null,
): Serializable {

    init {
        error.requireNotBlank("error")
        message?.requireNotBlank("message")
        path?.requireNotBlank("path")
    }

    companion object {
        private const val serialVersionUID: Long = 1L
    }
}
