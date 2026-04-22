package io.bluetape4k.mockwebflux.jsonplaceholder

import io.bluetape4k.mockwebflux.AbstractMockWebfluxServerTest
import io.bluetape4k.mockwebflux.jsonplaceholder.model.UserRecord
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType

/**
 * `GET/POST/PATCH/DELETE /jsonplaceholder/users` 엔드포인트 계약 테스트 (E34).
 *
 * 사용자 생성(201) → 목록 조회(200) → 단건 조회(200) → 수정(200) → 삭제(2xx) 왕복 흐름을 검증한다.
 */
class UsersContractTest: AbstractMockWebfluxServerTest() {

    /**
     * E34: POST → GET list → GET one → PATCH → DELETE 왕복 흐름으로 users CRUD 전 과정을 검증한다.
     */
    @Test
    fun `E34 users CRUD roundtrip`() {
        val created = client.post().uri("/jsonplaceholder/users")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(
                UserRecord(
                    id = 0L,
                    name = "u",
                    username = "un",
                    email = "u@u.io",
                    phone = "010",
                    website = "u.dev",
                )
            )
            .exchange()
            .expectStatus().isCreated
            .expectBody(UserRecord::class.java)
            .returnResult().responseBody
        created.shouldNotBeNull()
        created.id shouldBeGreaterThan 0L

        client.get().uri("/jsonplaceholder/users")
            .exchange()
            .expectStatus().isOk

        client.get().uri("/jsonplaceholder/users/${created.id}")
            .exchange()
            .expectStatus().isOk

        client.patch().uri("/jsonplaceholder/users/${created.id}")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(created.copy(name = "u2"))
            .exchange()
            .expectStatus().isOk

        client.delete().uri("/jsonplaceholder/users/${created.id}")
            .exchange()
            .expectStatus().is2xxSuccessful
    }
}
