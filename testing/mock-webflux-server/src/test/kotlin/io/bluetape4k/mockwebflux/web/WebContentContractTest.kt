package io.bluetape4k.mockwebflux.web

import io.bluetape4k.mockwebflux.AbstractMockWebfluxServerTest
import org.amshove.kluent.shouldContain
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

/**
 * web 컨텐츠 엔드포인트 계약 테스트 (E35–E36).
 */
class WebContentContractTest: AbstractMockWebfluxServerTest() {

    @Test
    fun `E35 random_returns_200_html`() {
        val result = client.get().uri("/web/random")
            .exchange()
            .expectStatus().isOk
            .expectBody(String::class.java)
            .returnResult()

        val contentType = result.responseHeaders.contentType?.toString() ?: ""
        contentType shouldContain "text/html"
    }

    @ParameterizedTest
    @ValueSource(strings = ["home", "naver", "google", "login", "article"])
    fun `E36 byName_returns_200_html`(name: String) {
        val body = client.get().uri("/web/$name")
            .exchange()
            .expectStatus().isOk
            .expectBody(String::class.java)
            .returnResult().responseBody ?: ""
        body.lowercase() shouldContain "<html"
    }
}
