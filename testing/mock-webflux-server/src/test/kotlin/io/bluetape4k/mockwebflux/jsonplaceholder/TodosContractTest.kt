package io.bluetape4k.mockwebflux.jsonplaceholder

import io.bluetape4k.mockwebflux.AbstractMockWebfluxServerTest
import io.bluetape4k.mockwebflux.jsonplaceholder.model.TodoRecord
import io.bluetape4k.assertions.shouldBeGreaterThan
import io.bluetape4k.assertions.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType

/**
 * `GET/POST/PATCH/DELETE /jsonplaceholder/todos` 엔드포인트 계약 테스트 (E33).
 *
 * 할 일 생성(201) → 목록 조회(200) → 단건 조회(200) → 수정(200) → 삭제(2xx) 왕복 흐름을 검증한다.
 */
class TodosContractTest: AbstractMockWebfluxServerTest() {

    /**
     * E33: POST → GET list → GET one → PATCH → DELETE 왕복 흐름으로 todos CRUD 전 과정을 검증한다.
     */
    @Test
    fun `E33 todos CRUD roundtrip`() {
        val created = client.post().uri("/jsonplaceholder/todos")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(TodoRecord(userId = 1L, id = 0L, title = "t", completed = false))
            .exchange()
            .expectStatus().isCreated
            .expectBody(TodoRecord::class.java)
            .returnResult().responseBody
        created.shouldNotBeNull()
        created.id shouldBeGreaterThan 0L

        client.get().uri("/jsonplaceholder/todos")
            .exchange()
            .expectStatus().isOk

        client.get().uri("/jsonplaceholder/todos/${created.id}")
            .exchange()
            .expectStatus().isOk

        client.patch().uri("/jsonplaceholder/todos/${created.id}")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(created.copy(completed = true))
            .exchange()
            .expectStatus().isOk

        client.delete().uri("/jsonplaceholder/todos/${created.id}")
            .exchange()
            .expectStatus().is2xxSuccessful
    }
}
