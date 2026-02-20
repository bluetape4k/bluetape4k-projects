package io.bluetape4k.spring.webflux.filter

import io.bluetape4k.spring.webflux.AbstractWebfluxTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.util.concurrent.atomic.AtomicReference

class HttpRequestFilterTest: AbstractWebfluxTest() {

    @Test
    fun `holder returns request from reactor context`() {
        val request = MockServerHttpRequest.get("/test").build()

        val mono = HttpRequestHolder.getHttpRequest()
            .contextWrite { ctx -> ctx.put(ServerHttpRequest::class.java, request) }

        StepVerifier.create(mono)
            .expectNextMatches { it.uri.path == "/test" }
            .verifyComplete()
    }

    @Test
    fun `holder returns empty when request is missing`() {
        StepVerifier.create(HttpRequestHolder.getHttpRequest())
            .verifyComplete()
    }

    @Test
    fun `capturer stores request in reactor context`() {
        val request = MockServerHttpRequest.get("/captured").build()
        val exchange = MockServerWebExchange.from(request)
        val capturer = HttpRequestCapturer()
        val captured = AtomicReference<ServerHttpRequest?>()

        val chain = WebFilterChain { _: ServerWebExchange ->
            HttpRequestHolder.getHttpRequest()
                .doOnNext { captured.set(it) }
                .then()
        }

        StepVerifier.create(capturer.filter(exchange, chain))
            .verifyComplete()

        captured.get().shouldNotBeNull()
        captured.get()?.uri?.path shouldBeEqualTo "/captured"
    }

    @Test
    fun `redirect filter rewrites path`() {
        val filter = object: AbstractRedirectWebFilter("/swagger", "/") {}
        val exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/").build())
        val redirectedPath = AtomicReference<String?>()

        val chain = WebFilterChain { ex ->
            redirectedPath.set(ex.request.uri.path)
            Mono.empty()
        }

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete()
        redirectedPath.get() shouldBeEqualTo "/swagger"
    }

    @Test
    fun `redirect filter keeps non matching path`() {
        val filter = object: AbstractRedirectWebFilter("/swagger", "/") {}
        val exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api").build())
        val redirectedPath = AtomicReference<String?>()

        val chain = WebFilterChain { ex ->
            redirectedPath.set(ex.request.uri.path)
            Mono.empty()
        }

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete()
        redirectedPath.get() shouldBeEqualTo "/api"
    }
}
