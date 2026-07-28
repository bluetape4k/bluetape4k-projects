package io.bluetape4k.ktor.core

import io.bluetape4k.support.requireNotBlank
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.Serializable
import java.io.Serializable as JavaSerializable

/**
 * Ktor application에서 사용하는 표준 JSON error payload입니다.
 *
 * ## 계약
 * - [error]는 machine-readable stable code입니다.
 * - [message]는 API client에 노출해도 되는 메시지입니다.
 * - [status]는 Ktor가 전송한 HTTP status code를 반영합니다.
 *
 * ```kotlin
 * val response = ApiErrorResponse.of(
 *     status = HttpStatusCode.BadRequest,
 *     error = "bad_request",
 *     message = "Invalid query parameter"
 * )
 * ```
 *
 * @property error client와 monitoring에서 분기할 수 있는 stable error code입니다.
 * @property message API client에 노출할 수 있는 설명 메시지입니다.
 * @property status HTTP response status code의 정수 값입니다.
 * @property path 오류가 발생한 request path입니다. path를 알 수 없으면 null입니다.
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
         * Ktor [HttpStatusCode]에서 [ApiErrorResponse]를 생성합니다.
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
