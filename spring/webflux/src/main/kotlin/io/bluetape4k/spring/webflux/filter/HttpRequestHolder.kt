package io.bluetape4k.spring.webflux.filter

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
object HttpRequestHolder {

    private val REQUEST_KEY = ServerHttpRequest::class.java

    fun getHttpRequest(): Mono<ServerHttpRequest> {
        return Mono.deferContextual { cv ->
            Mono.justOrEmpty(cv.getOrEmpty(REQUEST_KEY))
        }
    }
}
