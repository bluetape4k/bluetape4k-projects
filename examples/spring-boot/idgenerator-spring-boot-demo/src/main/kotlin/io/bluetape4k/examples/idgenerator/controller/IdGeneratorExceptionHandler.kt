package io.bluetape4k.examples.idgenerator.controller

import io.bluetape4k.examples.idgenerator.service.InvalidBatchSizeException
import io.bluetape4k.examples.idgenerator.service.UnsupportedGeneratorTypeException
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

/**
 * idgenerator REST demo의 input validation error를 JSON response로 변환합니다.
 *
 * ## Behavior
 * - Unsupported generator types and batch size errors return HTTP 400.
 * - Responses include the request path and message so example API users can reproduce the problem.
 */
@RestControllerAdvice
class IdGeneratorExceptionHandler {

    @ExceptionHandler(UnsupportedGeneratorTypeException::class)
    fun handleUnsupportedType(
        ex: UnsupportedGeneratorTypeException,
        request: HttpServletRequest,
    ): ResponseEntity<ErrorResponse> =
        badRequest(ex.message ?: "Unsupported generator type", request)

    @ExceptionHandler(InvalidBatchSizeException::class)
    fun handleInvalidBatchSize(
        ex: InvalidBatchSizeException,
        request: HttpServletRequest,
    ): ResponseEntity<ErrorResponse> =
        badRequest(ex.message ?: "Invalid batch size", request)

    private fun badRequest(message: String, request: HttpServletRequest): ResponseEntity<ErrorResponse> =
        ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(
                ErrorResponse(
                    status = HttpStatus.BAD_REQUEST.value(),
                    error = HttpStatus.BAD_REQUEST.reasonPhrase,
                    message = message,
                    path = request.requestURI,
                ),
            )
}
