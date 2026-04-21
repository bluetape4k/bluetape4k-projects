package io.bluetape4k.mockwebflux.jsonplaceholder

import io.bluetape4k.mockwebflux.AbstractMockWebfluxServerTest
import io.bluetape4k.mockwebflux.jsonplaceholder.model.PhotoRecord
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType

/**
 * `GET/POST/PATCH/DELETE /jsonplaceholder/photos` 엔드포인트 계약 테스트 (E32).
 *
 * 사진 생성(201) → 목록 조회(200) → 단건 조회(200) → 수정(200) → 삭제(2xx) 왕복 흐름을 검증한다.
 */
class PhotosContractTest : AbstractMockWebfluxServerTest() {

    /**
     * E32: POST → GET list → GET one → PATCH → DELETE 왕복 흐름으로 photos CRUD 전 과정을 검증한다.
     */
    @Test
    fun `E32 photos CRUD roundtrip`() {
        val created = client.post().uri("/jsonplaceholder/photos")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(
                PhotoRecord(
                    albumId = 1L, id = 0L, title = "p",
                    url = "http://e.com/u.png",
                    thumbnailUrl = "http://e.com/t.png",
                )
            )
            .exchange()
            .expectStatus().isCreated
            .expectBody(PhotoRecord::class.java)
            .returnResult().responseBody
        created.shouldNotBeNull()
        created.id shouldBeGreaterThan 0L

        client.get().uri("/jsonplaceholder/photos")
            .exchange()
            .expectStatus().isOk

        client.get().uri("/jsonplaceholder/photos/${created.id}")
            .exchange()
            .expectStatus().isOk

        client.patch().uri("/jsonplaceholder/photos/${created.id}")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(created.copy(title = "p2"))
            .exchange()
            .expectStatus().isOk

        client.delete().uri("/jsonplaceholder/photos/${created.id}")
            .exchange()
            .expectStatus().is2xxSuccessful
    }
}
