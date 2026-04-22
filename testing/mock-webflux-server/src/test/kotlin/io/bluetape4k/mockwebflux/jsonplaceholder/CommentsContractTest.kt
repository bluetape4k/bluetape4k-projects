package io.bluetape4k.mockwebflux.jsonplaceholder

import io.bluetape4k.mockwebflux.AbstractMockWebfluxServerTest
import io.bluetape4k.mockwebflux.jsonplaceholder.model.CommentRecord
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType

/**
 * `GET/POST/PATCH/DELETE /jsonplaceholder/comments` 엔드포인트 계약 테스트 (E30).
 *
 * 댓글 생성(201) → 목록 조회(200) → 단건 조회(200) → 수정(200) → 삭제(2xx) 왕복 흐름을 검증한다.
 */
class CommentsContractTest: AbstractMockWebfluxServerTest() {

    /**
     * E30: POST → GET list → GET one → PATCH → DELETE 왕복 흐름으로 comments CRUD 전 과정을 검증한다.
     */
    @Test
    fun `E30 comments CRUD roundtrip`() {
        val created = client.post().uri("/jsonplaceholder/comments")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(CommentRecord(postId = 1L, id = 0L, name = "n", email = "a@b.c", body = "hi"))
            .exchange()
            .expectStatus().isCreated
            .expectBody(CommentRecord::class.java)
            .returnResult().responseBody
        created.shouldNotBeNull()
        created.id shouldBeGreaterThan 0L

        client.get().uri("/jsonplaceholder/comments")
            .exchange()
            .expectStatus().isOk

        client.get().uri("/jsonplaceholder/comments/${created.id}")
            .exchange()
            .expectStatus().isOk

        client.patch().uri("/jsonplaceholder/comments/${created.id}")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(created.copy(name = "nn"))
            .exchange()
            .expectStatus().isOk

        client.delete().uri("/jsonplaceholder/comments/${created.id}")
            .exchange()
            .expectStatus().is2xxSuccessful
    }
}
