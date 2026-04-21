package io.bluetape4k.mockwebflux.jsonplaceholder

import io.bluetape4k.mockwebflux.AbstractMockWebfluxServerTest
import io.bluetape4k.mockwebflux.jsonplaceholder.model.AlbumRecord
import io.bluetape4k.mockwebflux.jsonplaceholder.model.CommentRecord
import io.bluetape4k.mockwebflux.jsonplaceholder.model.PhotoRecord
import io.bluetape4k.mockwebflux.jsonplaceholder.model.PostRecord
import io.bluetape4k.mockwebflux.jsonplaceholder.model.TodoRecord
import io.bluetape4k.mockwebflux.jsonplaceholder.model.UserRecord
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType

/**
 * jsonplaceholder CRUD 엔드포인트 계약 테스트 (E29–E34).
 *
 * 각 리소스(posts, comments, albums, photos, todos, users)에 대해
 * POST → GET list → GET one → PATCH → DELETE 왕복 흐름을 검증한다.
 */
class JsonplaceholderContractTest : AbstractMockWebfluxServerTest() {

    @Test
    fun `E29 posts_crud_roundtrip`() {
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

    @Test
    fun `E30 comments_crud_roundtrip`() {
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

    @Test
    fun `E31 albums_crud_roundtrip`() {
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
    fun `E32 photos_crud_roundtrip`() {
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

    @Test
    fun `E33 todos_crud_roundtrip`() {
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

    @Test
    fun `E34 users_crud_roundtrip`() {
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
