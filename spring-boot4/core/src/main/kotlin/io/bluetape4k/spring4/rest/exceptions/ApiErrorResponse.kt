package io.bluetape4k.spring4.rest

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import java.io.Serializable
import java.time.Instant

/**
 * API 오류 응답으로 사용하는 [ResponseEntity] 별칭입니다.
 *
 * ## 동작/계약
 * - 본 별칭은 [ApiErrorBody]를 본문으로 갖는 [ResponseEntity]를 의미합니다.
 * - [apiErrorResponseEntityOf]가 생성하는 응답 타입으로 사용됩니다.
 *
 * ```kotlin
 * val response: ApiErrorResponseEntity =
 *     apiErrorResponseEntityOf(statusCode = 400, message = "bad request")
 * // response.statusCode.value() == 400
 * ```
 */
typealias ApiErrorResponseEntity = ResponseEntity<ApiErrorBody>

/**
 * API 오류 본문 데이터를 표현합니다.
 *
 * ## 동작/계약
 * - `timestamp` 기본값은 인스턴스 생성 시점의 [Instant.now]입니다.
 * - `errorCode`, `message`, `stackTraces`는 생성 인자로 전달한 값을 그대로 보관합니다.
 *
 * ```kotlin
 * val body = ApiErrorBody(message = "invalid input")
 * // body.message == "invalid input"
 * ```
 *
 * @property errorCode 도메인 오류 코드
 * @property timestamp 오류 응답 생성 시각
 * @property message 오류 메시지
 * @property stackTraces 오류 스택 트레이스 목록
 */
data class ApiErrorBody(
    val errorCode: String? = null,
    val timestamp: Instant = Instant.now(),
    val message: String? = null,
    val stackTraces: List<StackTraceElement> = emptyList(),
): Serializable

/**
 * 상태 코드와 오류 본문으로 [ApiErrorResponseEntity]를 생성합니다.
 *
 * ## 동작/계약
 * - [statusCode]로 응답 상태를 설정하고, [ApiErrorBody]를 본문으로 담아 반환합니다.
 * - [stackTraces]를 전달하지 않으면 빈 리스트를 사용합니다.
 *
 * ```kotlin
 * val response = apiErrorResponseEntityOf(statusCode = 404, message = "not found")
 * // response.statusCode.value() == 404
 * ```
 */
fun apiErrorResponseEntityOf(
    statusCode: Int = HttpStatus.INTERNAL_SERVER_ERROR.value(),
    errorCode: String? = null,
    message: String? = null,
    stackTraces: List<StackTraceElement> = emptyList(),
): ApiErrorResponseEntity {
    val body =
        ApiErrorBody(
            errorCode = errorCode,
            message = message,
            stackTraces = stackTraces
        )
    return ResponseEntity.status(statusCode).body(body)
}
