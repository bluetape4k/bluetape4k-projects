package io.bluetape4k.mockwebflux.httpbin

import io.bluetape4k.mockwebflux.AbstractMockWebfluxServerTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContain
import org.junit.jupiter.api.Test

/**
 * httpbin 스트리밍/인코딩 엔드포인트 계약 테스트 (E15–E18).
 */
class HttpbinStreamContractTest: AbstractMockWebfluxServerTest() {

    @Test
    fun `E15 gzip_returns_200`() {
        client.get().uri("/httpbin/gzip")
            .exchange()
            .expectStatus().isOk
    }

    @Test
    fun `E16 deflate_returns_200`() {
        client.get().uri("/httpbin/deflate")
            .exchange()
            .expectStatus().isOk
    }

    @Test
    fun `E17 stream_returns_5_items`() {
        // NDJSON 응답은 줄바꿈으로 구분된 JSON 문자열을 포함한다.
        // 서버 인코딩에 따라 줄바꿈 구분자 또는 단일 body로 들어올 수 있으므로
        // 각 요소의 `id` 필드 개수로 요소 수를 검증한다.
        val body = client.get().uri("/httpbin/stream/5")
            .exchange()
            .expectStatus().isOk
            .expectBody(String::class.java)
            .returnResult().responseBody ?: ""
        val idCount = Regex("\\\\\"id\\\\\"").findAll(body).count()
            .let { if (it > 0) it else Regex("\"id\"").findAll(body).count() }
        idCount shouldBeEqualTo 5
    }

    @Test
    fun `E18 image_returns_200_with_image_content_type`() {
        val result = client.get().uri("/httpbin/image/png")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .returnResult()

        val contentType = result.responseHeaders.contentType?.toString() ?: ""
        contentType shouldContain "image"
    }
}
