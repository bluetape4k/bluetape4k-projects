package io.bluetape4k.mockwebflux.httpbin

import io.bluetape4k.mockwebflux.AbstractMockWebfluxServerTest
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType

/**
 * httpbin 기본 엔드포인트 계약 테스트 (E03–E14).
 */
class HttpbinContractTest: AbstractMockWebfluxServerTest() {

    @Test
    fun `E03 get_returns_200_with_url_field`() {
        client.get().uri("/httpbin/get")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.url").exists()
    }

    @Test
    fun `E04 post_returns_200`() {
        client.post().uri("/httpbin/post")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(mapOf("hello" to "world"))
            .exchange()
            .expectStatus().isOk
    }

    @Test
    fun `E05 put_returns_200`() {
        client.put().uri("/httpbin/put")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(mapOf("hello" to "world"))
            .exchange()
            .expectStatus().isOk
    }

    @Test
    fun `E06 patch_returns_200`() {
        client.patch().uri("/httpbin/patch")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(mapOf("hello" to "world"))
            .exchange()
            .expectStatus().isOk
    }

    @Test
    fun `E07 delete_returns_200`() {
        client.delete().uri("/httpbin/delete")
            .exchange()
            .expectStatus().isOk
    }

    @Test
    fun `E08 headers_returns_200_with_headers_field`() {
        client.get().uri("/httpbin/headers")
            .header("X-Test-Header", "hello")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.headers").exists()
    }

    @Test
    fun `E09 ip_returns_200_with_origin_field`() {
        client.get().uri("/httpbin/ip")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.origin").exists()
    }

    @Test
    fun `E10 user_agent_returns_200`() {
        client.get().uri("/httpbin/user-agent")
            .header("User-Agent", "bluetape4k-test")
            .exchange()
            .expectStatus().isOk
    }

    @Test
    fun `E11 uuid_returns_200_with_uuid_field`() {
        client.get().uri("/httpbin/uuid")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.uuid").exists()
    }

    @Test
    fun `E12 anything_returns_200`() {
        client.get().uri("/httpbin/anything/foo/bar")
            .exchange()
            .expectStatus().isOk
    }

    @Test
    fun `E13 status_endpoint_returns_requested_status`() {
        client.get().uri("/httpbin/status/200")
            .exchange()
            .expectStatus().isOk

        client.get().uri("/httpbin/status/404")
            .exchange()
            .expectStatus().isNotFound
    }

    @Test
    fun `E14 bytes_returns_200`() {
        client.get().uri("/httpbin/bytes/100")
            .exchange()
            .expectStatus().isOk
    }
}
