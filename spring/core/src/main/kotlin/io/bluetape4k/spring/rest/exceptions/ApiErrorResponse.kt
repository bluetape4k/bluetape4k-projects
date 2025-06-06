package io.bluetape4k.spring.rest

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import java.io.Serializable
import java.time.Instant

typealias ApiErrorResponseEntity = ResponseEntity<ApiErrorBody>

data class ApiErrorBody(
    val errorCode: String? = null,
    val timestamp: Instant = Instant.now(),
    val message: String? = null,
    val stackTraces: List<StackTraceElement> = emptyList(),
): Serializable

fun apiErrorResponseEntityOf(
    statusCode: Int = HttpStatus.INTERNAL_SERVER_ERROR.value(),
    errorCode: String? = null,
    message: String? = null,
    stackTraces: List<StackTraceElement> = emptyList(),
): ApiErrorResponseEntity {
    val body = ApiErrorBody(
        errorCode = errorCode,
        message = message,
        stackTraces = stackTraces
    )
    return ResponseEntity.status(statusCode).body(body)
}
