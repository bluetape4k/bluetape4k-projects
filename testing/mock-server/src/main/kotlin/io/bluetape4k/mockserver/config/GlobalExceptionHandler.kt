package io.bluetape4k.mockserver.config

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.warn
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

/**
 * 전역 예외 처리기.
 *
 * 애플리케이션 전체에서 발생하는 예외를 적절한 HTTP 응답으로 변환한다.
 */
@RestControllerAdvice
class GlobalExceptionHandler {
    companion object : KLogging()

    /**
     * 존재하지 않는 리소스 요청 시 404 응답을 반환한다.
     *
     * @param ex 발생한 예외
     * @return 404 ProblemDetail 응답
     */
    @ExceptionHandler(NoSuchElementException::class)
    fun handleNotFound(ex: NoSuchElementException): ProblemDetail {
        log.warn { "Resource not found: ${ex.message}" }
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.message ?: "Resource not found")
    }

    /**
     * 잘못된 요청 파라미터 예외를 400 응답으로 변환한다.
     *
     * @param ex 발생한 예외
     * @return 400 ProblemDetail 응답
     */
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleBadRequest(ex: IllegalArgumentException): ProblemDetail {
        log.warn { "Bad request: ${ex.message}" }
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.message ?: "Bad request")
    }
}
