package io.bluetape4k.spring.rest.exceptions

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.error
import io.bluetape4k.spring.rest.ApiErrorResponseEntity
import io.bluetape4k.spring.rest.apiErrorResponseEntityOf
import org.springframework.http.HttpStatus
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class ApiExceptionHandler {

    companion object: KLogging() {

        private fun handleApiExceptionOf(
            exception: Throwable,
            status: HttpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
            errorCode: String? = null,
        ): ApiErrorResponseEntity {
            return apiErrorResponseEntityOf(
                statusCode = status.value(),
                errorCode = errorCode,
                message = exception.message,
                stackTraces = exception.stackTrace.asList()
            ).apply {
                logApiError(exception, this)
            }
        }

        private fun logApiError(exception: Throwable, responseEntity: ApiErrorResponseEntity) {
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

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handle(ex: HttpMessageNotReadableException): ApiErrorResponseEntity =
        handleApiExceptionOf(
            exception = ex,
            status = HttpStatus.BAD_REQUEST,
        )

    @ExceptionHandler(ApiBadRequestException::class)
    fun handle(ex: ApiBadRequestException): ApiErrorResponseEntity =
        handleApiExceptionOf(
            exception = ex,
            status = ex.httpStatus,
        )

    @ExceptionHandler(ApiEntityNotFoundException::class)
    fun handle(ex: ApiEntityNotFoundException): ApiErrorResponseEntity =
        handleApiExceptionOf(
            exception = ex,
            status = ex.httpStatus,
        )

    @ExceptionHandler(ApiTooManyRequestsException::class)
    fun handle(ex: ApiTooManyRequestsException): ApiErrorResponseEntity =
        handleApiExceptionOf(
            exception = ex,
            status = ex.httpStatus,
        )

    @ExceptionHandler(ApiForbiddenException::class)
    fun handle(ex: ApiForbiddenException): ApiErrorResponseEntity =
        handleApiExceptionOf(
            exception = ex,
            status = ex.httpStatus,
        )

    @ExceptionHandler(ApiUnauthorizedException::class)
    fun handle(ex: ApiUnauthorizedException): ApiErrorResponseEntity =
        handleApiExceptionOf(
            exception = ex,
            status = ex.httpStatus,
        )

    @ExceptionHandler(ApiInternalServerErrorException::class)
    fun handle(ex: ApiInternalServerErrorException): ApiErrorResponseEntity =
        handleApiExceptionOf(
            exception = ex,
            status = ex.httpStatus,
        )

    @ExceptionHandler(ApiServiceUnavailableException::class)
    fun handle(ex: ApiServiceUnavailableException): ApiErrorResponseEntity =
        handleApiExceptionOf(
            exception = ex,
            status = ex.httpStatus,
        )
}
