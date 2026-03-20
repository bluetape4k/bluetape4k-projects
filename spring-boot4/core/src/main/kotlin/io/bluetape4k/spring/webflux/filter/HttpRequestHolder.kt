package io.bluetape4k.spring.webflux.filter

import io.bluetape4k.logging.coroutines.KLoggingChannel
import org.springframework.http.server.reactive.ServerHttpRequest
import reactor.core.publisher.Mono

/**
 * Reactor Context에 저장된 [ServerHttpRequest]를 조회하는 유틸리티입니다.
 *
 * ## 동작/계약
 * - 키는 `ServerHttpRequest::class.java`를 사용하며, [HttpRequestCapturer]가 같은 키로 저장한 값을 읽습니다.
 * - 현재 구독 컨텍스트에 요청이 없으면 빈 `Mono`를 반환합니다.
 * - 컨텍스트 접근은 `Mono.deferContextual`로 지연되어 구독 시점의 값을 사용합니다.
 *
 * ```kotlin
 * val request = HttpRequestHolder.getHttpRequest().awaitSingleOrNull()
 * // request == null (컨텍스트에 값이 없을 때)
 * ```
 *
 * @see io.bluetape4k.spring.webflux.filter.HttpRequestCapturer
 */
object HttpRequestHolder: KLoggingChannel() {
    private val REQUEST_KEY = ServerHttpRequest::class.java

    /**
     * 현재 Reactor Context에서 [ServerHttpRequest]를 조회합니다.
     *
     * ## 동작/계약
     * - 컨텍스트에 값이 있으면 해당 요청을 단일 값 `Mono`로 반환합니다.
     * - 컨텍스트에 값이 없으면 완료만 발생하는 빈 `Mono`를 반환합니다.
     *
     * ```kotlin
     * val mono = HttpRequestHolder.getHttpRequest()
     * // mono는 컨텍스트 값이 있을 때만 onNext를 발생시킨다.
     * ```
     */
    fun getHttpRequest(): Mono<ServerHttpRequest> =
        Mono.deferContextual { cv ->
            Mono.justOrEmpty(cv.getOrEmpty(REQUEST_KEY))
        }
}
