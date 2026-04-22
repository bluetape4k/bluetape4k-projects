package io.bluetape4k.mockwebflux.jsonplaceholder

import io.bluetape4k.mockwebflux.AbstractMockWebfluxServerTest
import io.bluetape4k.mockwebflux.jsonplaceholder.model.PostRecord
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType

/**
 * `GET/POST/PATCH/DELETE /jsonplaceholder/posts` 엔드포인트 계약 테스트 (E29).
 *
 * [WebTestClient]를 통해 게시글 생성(201) → 목록 조회(200) → 단건 조회(200) → 수정(200) → 삭제(2xx)
 * 왕복 흐름을 검증한다.
 */
class PostsContractTest: AbstractMockWebfluxServerTest() {

    /**
     * E29: POST → GET list → GET one → PATCH → DELETE 왕복 흐름으로 posts CRUD 전 과정을 검증한다.
     */
    @Test
    fun `E29 posts CRUD roundtrip`() {
        val created = client.post().uri("/jsonplaceholder/posts")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(PostRecord(id = 0L, userId = 1L, title = "t1", body = "b1"))
            .exchange()
            .expectStatus().isCreated
            .expectBody(PostRecord::class.java)
            .returnResult().responseBody
        created.shouldNotBeNull()
        created.id shouldBeGreaterThan 0L

        client.get().uri("/jsonplaceholder/posts")
            .exchange()
            .expectStatus().isOk

        client.get().uri("/jsonplaceholder/posts/${created.id}")
            .exchange()
            .expectStatus().isOk

        client.patch().uri("/jsonplaceholder/posts/${created.id}")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(PostRecord(id = created.id, userId = 1L, title = "t2", body = "b2"))
            .exchange()
            .expectStatus().isOk

        client.delete().uri("/jsonplaceholder/posts/${created.id}")
            .exchange()
            .expectStatus().is2xxSuccessful
    }
}
