package io.bluetape4k.spring.webflux.filter

import io.bluetape4k.logging.coroutines.KLoggingChannel
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

/**
 * Webflux 의 [ServerHttpRequest] 정보를 `ReactorContext` 에 보관해서 사용할 수 있는 [WebFilter] 구현체.
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
