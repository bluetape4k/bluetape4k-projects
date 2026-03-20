package io.bluetape4k.spring.rest.exceptions

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.error
import io.bluetape4k.spring.rest.ApiErrorResponseEntity
import io.bluetape4k.spring.rest.apiErrorResponseEntityOf
import org.springframework.http.HttpStatus
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

/**
 * REST API 예외를 표준 오류 응답으로 변환하는 예외 처리기입니다.
 *
 * ## 동작/계약
 * - 매핑된 예외를 [ApiErrorResponseEntity]로 변환하고 상태 코드를 예외 유형에 맞춰 설정합니다.
 * - 응답 생성 시 예외 메시지와 스택 트레이스를 본문에 담고 로그를 기록합니다.
 *
 * ```kotlin
 * val handler = ApiExceptionHandler()
 * val response = handler.handle(ApiBadRequestException("invalid input"))
 * // response.statusCode.value() == 400
 * ```
 */
@RestControllerAdvice
class ApiExceptionHandler {
    companion object: KLogging() {
        private fun handleApiExceptionOf(
            exception: Throwable,
            status: HttpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
            errorCode: String? = null,
        ): ApiErrorResponseEntity =
            apiErrorResponseEntityOf(
                statusCode = status.value(),
                errorCode = errorCode,
                message = exception.message,
                stackTraces = exception.stackTrace.asList()
            ).apply {
                logApiError(exception, this)
            }

        private fun logApiError(
            exception: Throwable,
            responseEntity: ApiErrorResponseEntity,
        ) {
            log.error(exception) {
                """
                API Error occurred:
                    status=${responseEntity.statusCode},
                    errorCode=${responseEntity.body?.errorCode},
                    message=${responseEntity.body?.message}
                """.trimIndent()
            }
        }
    }

    /**
     * 역직렬화 실패 예외를 `400 Bad Request` 응답으로 변환합니다.
     *
     * ## 동작/계약
     * - 상태 코드를 [HttpStatus.BAD_REQUEST]로 고정합니다.
     * - 예외 메시지와 스택 트레이스를 오류 본문에 포함합니다.
     *
     * ```kotlin
     * val ex = HttpMessageNotReadableException("invalid body")
     * val response = ApiExceptionHandler().handle(ex)
     * // response.statusCode.value() == 400
     * ```
     */
    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handle(ex: HttpMessageNotReadableException): ApiErrorResponseEntity =
        handleApiExceptionOf(
            exception = ex,
            status = HttpStatus.BAD_REQUEST
        )

    /**
     * [ApiBadRequestException]을 해당 상태 코드 응답으로 변환합니다.
     *
     * ## 동작/계약
     * - 상태 코드는 [ApiBadRequestException.httpStatus] 값을 사용합니다.
     *
     * ```kotlin
     * val response = ApiExceptionHandler().handle(ApiBadRequestException("invalid"))
     * // response.statusCode.value() == 400
     * ```
     */
    @ExceptionHandler(ApiBadRequestException::class)
    fun handle(ex: ApiBadRequestException): ApiErrorResponseEntity =
        handleApiExceptionOf(
            exception = ex,
            status = ex.httpStatus
        )

    /**
     * [ApiEntityNotFoundException]을 해당 상태 코드 응답으로 변환합니다.
     *
     * ## 동작/계약
     * - 상태 코드는 [ApiEntityNotFoundException.httpStatus] 값을 사용합니다.
     *
     * ```kotlin
     * val response = ApiExceptionHandler().handle(ApiEntityNotFoundException("missing"))
     * // response.statusCode.value() == 404
     * ```
     */
    @ExceptionHandler(ApiEntityNotFoundException::class)
    fun handle(ex: ApiEntityNotFoundException): ApiErrorResponseEntity =
        handleApiExceptionOf(
            exception = ex,
            status = ex.httpStatus
        )

    /**
     * [ApiTooManyRequestsException]을 해당 상태 코드 응답으로 변환합니다.
     *
     * ## 동작/계약
     * - 상태 코드는 [ApiTooManyRequestsException.httpStatus] 값을 사용합니다.
     *
     * ```kotlin
     * val response = ApiExceptionHandler().handle(ApiTooManyRequestsException("limit"))
     * // response.statusCode.value() == 429
     * ```
     */
    @ExceptionHandler(ApiTooManyRequestsException::class)
    fun handle(ex: ApiTooManyRequestsException): ApiErrorResponseEntity =
        handleApiExceptionOf(
            exception = ex,
            status = ex.httpStatus
        )

    /**
     * [ApiForbiddenException]을 해당 상태 코드 응답으로 변환합니다.
     *
     * ## 동작/계약
     * - 상태 코드는 [ApiForbiddenException.httpStatus] 값을 사용합니다.
     *
     * ```kotlin
     * val response = ApiExceptionHandler().handle(ApiForbiddenException("forbidden"))
     * // response.statusCode.value() == 403
     * ```
     */
    @ExceptionHandler(ApiForbiddenException::class)
    fun handle(ex: ApiForbiddenException): ApiErrorResponseEntity =
        handleApiExceptionOf(
            exception = ex,
            status = ex.httpStatus
        )

    /**
     * [ApiUnauthorizedException]을 해당 상태 코드 응답으로 변환합니다.
     *
     * ## 동작/계약
     * - 상태 코드는 [ApiUnauthorizedException.httpStatus] 값을 사용합니다.
     *
     * ```kotlin
     * val response = ApiExceptionHandler().handle(ApiUnauthorizedException("unauthorized"))
     * // response.statusCode.value() == 401
     * ```
     */
    @ExceptionHandler(ApiUnauthorizedException::class)
    fun handle(ex: ApiUnauthorizedException): ApiErrorResponseEntity =
        handleApiExceptionOf(
            exception = ex,
            status = ex.httpStatus
        )

    /**
     * [ApiInternalServerErrorException]을 해당 상태 코드 응답으로 변환합니다.
     *
     * ## 동작/계약
     * - 상태 코드는 [ApiInternalServerErrorException.httpStatus] 값을 사용합니다.
     *
     * ```kotlin
     * val response = ApiExceptionHandler().handle(ApiInternalServerErrorException("boom"))
     * // response.statusCode.value() == 500
     * ```
     */
    @ExceptionHandler(ApiInternalServerErrorException::class)
    fun handle(ex: ApiInternalServerErrorException): ApiErrorResponseEntity =
        handleApiExceptionOf(
            exception = ex,
            status = ex.httpStatus
        )

    /**
     * [ApiServiceUnavailableException]을 해당 상태 코드 응답으로 변환합니다.
     *
     * ## 동작/계약
     * - 상태 코드는 [ApiServiceUnavailableException.httpStatus] 값을 사용합니다.
     *
     * ```kotlin
     * val response = ApiExceptionHandler().handle(ApiServiceUnavailableException("down"))
     * // response.statusCode.value() == 503
     * ```
     */
    @ExceptionHandler(ApiServiceUnavailableException::class)
    fun handle(ex: ApiServiceUnavailableException): ApiErrorResponseEntity =
        handleApiExceptionOf(
            exception = ex,
            status = ex.httpStatus
        )
}
