package io.bluetape4k.examples.idgenerator.controller

import io.bluetape4k.examples.idgenerator.service.InvalidBatchSizeException
import io.bluetape4k.examples.idgenerator.service.UnsupportedGeneratorTypeException
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

/**
 * idgenerator REST 예제의 입력 검증 오류를 JSON 응답으로 변환합니다.
 *
 * ## 동작/계약
 * - 지원하지 않는 type과 batch size 오류는 HTTP 400으로 응답합니다.
 * - 예제 API 사용자가 문제를 재현할 수 있도록 path와 message를 포함합니다.
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
