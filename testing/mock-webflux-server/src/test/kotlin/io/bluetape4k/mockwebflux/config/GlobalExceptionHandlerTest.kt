package io.bluetape4k.mockwebflux.config

import io.bluetape4k.mockwebflux.AbstractMockWebfluxServerTest
import org.junit.jupiter.api.Test

/**
 * [GlobalExceptionHandler] 통합 테스트.
 *
 * 실제 컨트롤러 엔드포인트를 통해 예외가 발생하도록 유도하여
 * GlobalExceptionHandler의 각 핸들러 메서드가 올바른 HTTP 응답을 반환하는지 검증한다.
 */
class GlobalExceptionHandlerTest: AbstractMockWebfluxServerTest() {

    @Test
    fun `존재하지 않는 앨범 ID 조회 시 404를 반환한다`() {
        client.get().uri("/jsonplaceholder/albums/999999999")
            .exchange()
            .expectStatus().isNotFound
            .expectBody()
            .jsonPath("$.error").isNotEmpty
    }

    @Test
    fun `존재하지 않는 게시글 ID 조회 시 404를 반환한다`() {
        client.get().uri("/jsonplaceholder/posts/999999999")
            .exchange()
            .expectStatus().isNotFound
            .expectBody()
            .jsonPath("$.error").isNotEmpty
    }

    @Test
    fun `delay에 범위 초과 값 전달 시 400을 반환한다`() {
        client.get().uri("/httpbin/delay/99")
            .exchange()
            .expectStatus().isBadRequest
            .expectBody()
            .jsonPath("$.error").isNotEmpty
    }

    @Test
    fun `delay에 음수 전달 시 400을 반환한다`() {
        client.get().uri("/httpbin/delay/-1")
            .exchange()
            .expectStatus().isBadRequest
    }
}
