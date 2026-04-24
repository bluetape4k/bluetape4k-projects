package io.bluetape4k.mockwebflux.admin

import io.bluetape4k.mockwebflux.AbstractMockWebfluxServerTest
import org.junit.jupiter.api.Test

/**
 * E01: GET /ping 엔드포인트 계약 테스트.
 */
class PingContractTest: AbstractMockWebfluxServerTest() {

    @Test
    fun `ping_returns_pong`() {
        client.get().uri("/ping")
            .exchange()
            .expectStatus().isOk
            .expectBody(String::class.java)
            .isEqualTo("pong")
    }
}
