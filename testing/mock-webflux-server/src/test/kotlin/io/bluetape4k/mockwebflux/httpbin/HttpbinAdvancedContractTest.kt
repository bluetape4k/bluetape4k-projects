package io.bluetape4k.mockwebflux.httpbin

import io.bluetape4k.mockwebflux.AbstractMockWebfluxServerTest
import org.junit.jupiter.api.Test
import java.time.Duration

/**
 * httpbin 고급 엔드포인트 계약 테스트 (E19–E28).
 */
class HttpbinAdvancedContractTest: AbstractMockWebfluxServerTest() {

    @Test
    fun `E19 delay_returns_200_within_timeout`() {
        // WebTestClient 기본 타임아웃은 5s. 10s 로 늘려 안전 여유 확보.
        val extended = client.mutate()
            .responseTimeout(Duration.ofSeconds(10))
            .build()
        extended.get().uri("/httpbin/delay/1")
            .exchange()
            .expectStatus().isOk
    }

    @Test
    fun `E20 redirect_returns_302`() {
        // WebTestClient 는 기본적으로 리다이렉트를 따라가지 않고 302 그대로 반환한다.
        client.get().uri("/httpbin/redirect/1")
            .exchange()
            .expectStatus().isFound
    }

    @Test
    fun `E21 cookies_returns_200`() {
        client.get().uri("/httpbin/cookies")
            .exchange()
            .expectStatus().isOk
    }

    @Test
    fun `E22 cookies_set_returns_302`() {
        // /cookies/set 은 쿠키 설정 후 /httpbin/cookies 로 302 리다이렉트한다.
        client.get().uri("/httpbin/cookies/set?name=val")
            .exchange()
            .expectStatus().isFound
    }

    @Test
    fun `E23 cookies_delete_returns_302`() {
        // 컨트롤러는 /cookies/delete?<name>=<value> 형태를 받는다.
        client.get().uri("/httpbin/cookies/delete?name=val")
            .exchange()
            .expectStatus().isFound
    }

    @Test
    fun `E24 basic_auth_without_header_returns_401`() {
        client.get().uri("/httpbin/basic-auth/user/pass")
            .exchange()
            .expectStatus().isUnauthorized
    }

    @Test
    fun `E25 bearer_without_header_returns_401`() {
        client.get().uri("/httpbin/bearer")
            .exchange()
            .expectStatus().isUnauthorized
    }

    @Test
    fun `E26 cache_with_if_modified_since_returns_304`() {
        client.get().uri("/httpbin/cache")
            .header("If-Modified-Since", "Tue, 01 Jan 2030 00:00:00 GMT")
            .exchange()
            .expectStatus().isNotModified
    }

    @Test
    fun `E27 cache_control_returns_200_with_cache_control_header`() {
        val result = client.get().uri("/httpbin/cache/60")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .returnResult()
        val cacheControl = result.responseHeaders.getFirst("Cache-Control") ?: ""
        require(cacheControl.isNotBlank()) { "Cache-Control header must be present, got: $cacheControl" }
    }

    @Test
    fun `E28 etag_with_if_none_match_returns_304`() {
        client.get().uri("/httpbin/etag/abc")
            .header("If-None-Match", "\"abc\"")
            .exchange()
            .expectStatus().isNotModified
    }

    @Test
    fun `E29 basic_auth_with_valid_credentials_returns_200`() {
        val credentials = java.util.Base64.getEncoder().encodeToString("user:pass".toByteArray())
        client.get().uri("/httpbin/basic-auth/user/pass")
            .header("Authorization", "Basic $credentials")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.authenticated").isEqualTo(true)
    }

    @Test
    fun `E30 bearer_with_token_returns_200`() {
        client.get().uri("/httpbin/bearer")
            .header("Authorization", "Bearer mytoken123")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.authenticated").isEqualTo(true)
    }

    @Test
    fun `E31 cache_without_if_modified_since_returns_200_with_last_modified`() {
        val result = client.get().uri("/httpbin/cache")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .returnResult()
        val lastModified = result.responseHeaders.getFirst("Last-Modified")
        require(!lastModified.isNullOrBlank()) { "Last-Modified header must be present" }
    }

    @Test
    fun `E32 etag_without_if_none_match_returns_200_with_etag_header`() {
        val result = client.get().uri("/httpbin/etag/myetag")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .returnResult()
        val etag = result.responseHeaders.getFirst("ETag")
        require(etag == "\"myetag\"") { "ETag header must be \"myetag\", got: $etag" }
    }

    @Test
    fun `E33 redirect_0_returns_302_to_httpbin_get`() {
        client.get().uri("/httpbin/redirect/0")
            .exchange()
            .expectStatus().isFound
    }
}
