package io.bluetape4k.spring.webflux.filter

import io.bluetape4k.logging.coroutines.KLoggingChannel
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

/**
 * 현재 요청을 Reactor Context에 저장해 다운스트림에서 조회할 수 있게 하는 [WebFilter]입니다.
 *
 * ## 동작/계약
 * - `exchange.request.mutate().build()`로 요청 스냅샷을 만든 뒤 체인을 실행합니다.
 * - 체인 실행 시 `contextWrite`로 `ServerHttpRequest::class.java` 키에 요청을 저장합니다.
 * - 같은 키를 사용하는 [HttpRequestHolder]에서 요청을 조회할 수 있습니다.
 *
 * ```kotlin
 * @Bean
 * fun httpRequestCapturer(): WebFilter = HttpRequestCapturer()
 * ```
 *
 * @see io.bluetape4k.spring.webflux.filter.HttpRequestHolder
 */
@Component
class HttpRequestCapturer: WebFilter {

    companion object: KLoggingChannel()

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val request = exchange.request.mutate().build()

        return chain.filter(exchange).contextWrite { ctx ->
            ctx.put(ServerHttpRequest::class.java, request)
        }
    }
}
