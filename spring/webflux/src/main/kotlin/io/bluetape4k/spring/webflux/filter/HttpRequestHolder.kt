package io.bluetape4k.spring.webflux.filter

import io.bluetape4k.logging.coroutines.KLoggingChannel
import org.springframework.http.server.reactive.ServerHttpRequest
import reactor.core.publisher.Mono

/**
 * ReactorContext 에 보관된 [ServerHttpRequest] 정보를 가져오는 유틸리티.
 * HttpRequestCapturer 를 WebFilter로 등록해 놓으면, HttpRequestHoder에서 조회할 수 있다
 *
 * @see io.bluetape4k.spring.webflux.filter.HttpRequestCapturer
 *
 * ```kotlin
 * val request: ServerHttpRequest? = HttpRequestHolder.getHttpRequest().awaitSingleOrNull()
 * ```
 */
object HttpRequestHolder: KLoggingChannel() {

    private val REQUEST_KEY = ServerHttpRequest::class.java

    /**
     * ReactorContext에 저장된 [ServerHttpRequest]를 조회합니다.
     *
     * @return [ServerHttpRequest]가 존재하면 Mono로 반환
     */
    fun getHttpRequest(): Mono<ServerHttpRequest> {
        return Mono.deferContextual { cv ->
            Mono.justOrEmpty(cv.getOrEmpty(REQUEST_KEY))
        }
    }
}
