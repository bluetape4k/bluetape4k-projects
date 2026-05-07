package io.bluetape4k.mockwebflux.jsonplaceholder

import io.bluetape4k.mockwebflux.AbstractMockWebfluxServerTest
import io.bluetape4k.mockwebflux.jsonplaceholder.model.AlbumRecord
import io.bluetape4k.assertions.shouldBeGreaterThan
import io.bluetape4k.assertions.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType

/**
 * `GET/POST/PATCH/DELETE /jsonplaceholder/albums` 엔드포인트 계약 테스트 (E31).
 *
 * 앨범 생성(201) → 목록 조회(200) → 단건 조회(200) → 수정(200) → 삭제(2xx) 왕복 흐름을 검증한다.
 */
class AlbumsContractTest: AbstractMockWebfluxServerTest() {

    /**
     * E31: POST → GET list → GET one → PATCH → DELETE 왕복 흐름으로 albums CRUD 전 과정을 검증한다.
     */
    @Test
    fun `E31 albums CRUD roundtrip`() {
        val created = client.post().uri("/jsonplaceholder/albums")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(AlbumRecord(userId = 1L, id = 0L, title = "album"))
            .exchange()
            .expectStatus().isCreated
            .expectBody(AlbumRecord::class.java)
            .returnResult().responseBody
        created.shouldNotBeNull()
        created.id shouldBeGreaterThan 0L

        client.get().uri("/jsonplaceholder/albums")
            .exchange()
            .expectStatus().isOk

        client.get().uri("/jsonplaceholder/albums/${created.id}")
            .exchange()
            .expectStatus().isOk

        client.patch().uri("/jsonplaceholder/albums/${created.id}")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(created.copy(title = "album2"))
            .exchange()
            .expectStatus().isOk

        client.delete().uri("/jsonplaceholder/albums/${created.id}")
            .exchange()
            .expectStatus().is2xxSuccessful
    }

    @Test
    fun `userId 필터로 앨범 목록을 조회한다`() {
        val album = client.post().uri("/jsonplaceholder/albums")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(AlbumRecord(userId = 42L, id = 0L, title = "filtered"))
            .exchange()
            .expectStatus().isCreated
            .expectBody(AlbumRecord::class.java)
            .returnResult().responseBody
        album.shouldNotBeNull()

        client.get().uri("/jsonplaceholder/albums?userId=42")
            .exchange()
            .expectStatus().isOk
            .expectBodyList(AlbumRecord::class.java)
            .hasSize(1)
    }

    @Test
    fun `PUT으로 앨범 전체를 교체한다`() {
        val created = client.post().uri("/jsonplaceholder/albums")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(AlbumRecord(userId = 1L, id = 0L, title = "original"))
            .exchange()
            .expectStatus().isCreated
            .expectBody(AlbumRecord::class.java)
            .returnResult().responseBody
        created.shouldNotBeNull()

        val updated = client.put().uri("/jsonplaceholder/albums/${created.id}")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(created.copy(title = "replaced"))
            .exchange()
            .expectStatus().isOk
            .expectBody(AlbumRecord::class.java)
            .returnResult().responseBody
        updated.shouldNotBeNull()
    }
}
