package io.bluetape4k.spring.webflux.filter

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.support.requireNotBlank
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

/**
 * 요청 Path를 redirect 합니다.
 * 보통 REST API Server의 root path를 redirect 해서 swagger page 로 redirect 할 때 사용할 수 있습니다.
 *
 * ```
 * @Component
 * class RedirectToSwaggerWebFilter: AbstractRedirectWebFilter(SWAGGER_PATH, ROOT_PATH) {
 *
 *     companion object: KLogging() {
 *         const val ROOT_PATH = "/"
 *         const val SWAGGER_PATH = "/swagger-ui.html"
 *     }
 * }
 * ```
 *
 * @property requestPath    요청한 Path (ex: "/")
 * @property redirectPath redirect할 Path (ex: "/swagger-ui.html")
 */
abstract class AbstractRedirectWebFilter(
    val redirectPath: String,
    val requestPath: String = ROOT_PATH,
): WebFilter {

    companion object: KLoggingChannel() {
        const val ROOT_PATH = "/"
    }

    init {
        requestPath.requireNotBlank("checkPath")
        redirectPath.requireNotBlank("redirectPath")
    }

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val redirectExchange = when (exchange.request.uri.path) {
            requestPath -> exchange.mutate().request(exchange.request.mutate().path(redirectPath).build()).build()
            else        -> exchange
        }

        return chain.filter(redirectExchange)
    }
}
