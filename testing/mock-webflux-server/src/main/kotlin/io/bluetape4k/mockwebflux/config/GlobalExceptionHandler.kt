package io.bluetape4k.mockwebflux.config

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.warn
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

/**
 * WebFlux 전역 예외 처리기.
 *
 * 애플리케이션 전체에서 발생하는 예외를 적절한 HTTP 응답으로 변환한다.
 * Spring WebFlux 환경에서는 [ProblemDetail] 대신 [ResponseEntity]를 사용한다.
 */
@RestControllerAdvice
class GlobalExceptionHandler {
    companion object: KLogging()

    /**
     * 존재하지 않는 리소스 요청 시 404 응답을 반환한다.
     *
     * @param ex 발생한 예외
     * @return 404 상태와 오류 메시지를 담은 [ResponseEntity]
     */
    @ExceptionHandler(NoSuchElementException::class)
    fun handleNotFound(ex: NoSuchElementException): ResponseEntity<Map<String, String>> {
        log.warn { "Resource not found: ${ex.message}" }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(mapOf("error" to (ex.message ?: "Not found")))
    }

    /**
     * 잘못된 요청 파라미터 예외를 400 응답으로 변환한다.
     *
     * @param ex 발생한 예외
     * @return 400 상태와 오류 메시지를 담은 [ResponseEntity]
     */
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleBadRequest(ex: IllegalArgumentException): ResponseEntity<Map<String, String>> {
        log.warn { "Bad request: ${ex.message}" }
        return ResponseEntity.badRequest()
            .body(mapOf("error" to (ex.message ?: "Bad request")))
    }
}
