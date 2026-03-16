package io.bluetape4k.spring.webflux.filter

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.support.requireNotBlank
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

/**
 * 요청 URI 경로가 [requestPath]와 일치할 때 요청 경로를 [redirectPath]로 치환해 다음 필터로 전달합니다.
 *
 * ## 동작/계약
 * - 생성 시 `requestPath`, `redirectPath`는 `blank`를 허용하지 않으며, 위반 시 예외가 발생합니다.
 * - [ServerWebExchange.request]의 `uri.path`가 [requestPath]와 정확히 같을 때만 경로를 바꾼 복제 요청을 사용합니다.
 * - 경로가 다르면 원본 [ServerWebExchange]를 그대로 다음 체인에 전달합니다.
 *
 * ```kotlin
 * @Component
 * class RedirectToSwaggerWebFilter: AbstractRedirectWebFilter("/swagger-ui.html", "/")
 * ```
 *
 * @property requestPath 매칭할 요청 경로 (예: `/`)
 * @property redirectPath 치환할 대상 경로 (예: `/swagger-ui.html`)
 */
abstract class AbstractRedirectWebFilter(
    val redirectPath: String,
    val requestPath: String = ROOT_PATH,
): WebFilter {

    companion object: KLoggingChannel() {
        /**
         * 기본 매칭 요청 경로(`/`)입니다.
         */
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
