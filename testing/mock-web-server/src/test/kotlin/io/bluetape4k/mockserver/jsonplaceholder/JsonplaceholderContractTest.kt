package io.bluetape4k.mockserver.jsonplaceholder

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.info
import io.bluetape4k.mockserver.MockServerApplication
import io.bluetape4k.mockserver.jsonplaceholder.model.AlbumRecord
import io.bluetape4k.mockserver.jsonplaceholder.model.CommentRecord
import io.bluetape4k.mockserver.jsonplaceholder.model.PhotoRecord
import io.bluetape4k.mockserver.jsonplaceholder.model.PostRecord
import io.bluetape4k.mockserver.jsonplaceholder.model.TodoRecord
import io.bluetape4k.mockserver.jsonplaceholder.model.UserRecord
import org.hamcrest.Matchers.greaterThan
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import tools.jackson.databind.json.JsonMapper

/**
 * jsonplaceholder `/jsonplaceholder/posts` 엔드포인트에 대한 계약 테스트.
 *
 * MockServerApplication을 MockMvc로 구동하여 CRUD 엔드포인트의 HTTP 응답 상태 코드 및
 * 응답 바디를 검증한다. 테스트 순서는 선언 순서를 따른다.
 */
@SpringBootTest(classes = [MockServerApplication::class])
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class JsonplaceholderContractTest {

    companion object : KLogging()

    @Autowired
    private lateinit var ctx: WebApplicationContext

    @Autowired
    private lateinit var jsonMapper: JsonMapper

    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(ctx).build()
    }

    /**
     * GET /jsonplaceholder/posts → 200, 비어있지 않은 게시글 목록
     */
    @Test
    @Order(1)
    fun `GET posts returns non-empty list with 200`() {
        mockMvc.perform(get("/jsonplaceholder/posts"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$.length()").value(greaterThan(0)))
            .andDo { log.info { "GET /jsonplaceholder/posts → 200" } }
    }

    /**
     * GET /jsonplaceholder/posts/1 → 200, id=1인 PostRecord 반환
     */
    @Test
    @Order(2)
    fun `GET posts by id returns single post with 200`() {
        mockMvc.perform(get("/jsonplaceholder/posts/1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(1))
            .andDo { log.info { "GET /jsonplaceholder/posts/1 → 200" } }
    }

    /**
     * GET /jsonplaceholder/posts/9999 → 404
     */
    @Test
    @Order(3)
    fun `GET posts with non-existent id returns 404`() {
        mockMvc.perform(get("/jsonplaceholder/posts/9999"))
            .andExpect(status().isNotFound)
            .andDo { log.info { "GET /jsonplaceholder/posts/9999 → 404" } }
    }

    /**
     * POST /jsonplaceholder/posts → 201, 새 id가 부여된 PostRecord 반환
     */
    @Test
    @Order(4)
    fun `POST posts creates new post with 201 and new id`() {
        val newPost = PostRecord(id = 0L, userId = 1L, title = "test title", body = "test body")
        val body = jsonMapper.writeValueAsString(newPost)

        mockMvc.perform(
            post("/jsonplaceholder/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value(greaterThan(0)))
            .andExpect(jsonPath("$.userId").value(1))
            .andExpect(jsonPath("$.title").value("test title"))
            .andExpect(jsonPath("$.body").value("test body"))
            .andDo { log.info { "POST /jsonplaceholder/posts → 201" } }
    }

    /**
     * PUT /jsonplaceholder/posts/1 → 200, 수정된 PostRecord 반환
     */
    @Test
    @Order(5)
    fun `PUT posts updates existing post with 200`() {
        val updatedPost = PostRecord(id = 1L, userId = 1L, title = "updated title", body = "updated body")
        val body = jsonMapper.writeValueAsString(updatedPost)

        mockMvc.perform(
            put("/jsonplaceholder/posts/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.title").value("updated title"))
            .andExpect(jsonPath("$.body").value("updated body"))
            .andDo { log.info { "PUT /jsonplaceholder/posts/1 → 200" } }
    }

    /**
     * DELETE /jsonplaceholder/posts/1 → 204
     */
    @Test
    @Order(6)
    fun `DELETE posts removes post with 204`() {
        mockMvc.perform(delete("/jsonplaceholder/posts/1"))
            .andExpect(status().is2xxSuccessful)
            .andDo { log.info { "DELETE /jsonplaceholder/posts/1 → 204" } }
    }

    /**
     * POST /admin/reset → 200, 이후 GET /jsonplaceholder/posts가 비어있지 않음을 확인
     */
    @Test
    @Order(7)
    fun `POST admin reset restores fixtures and posts are non-empty`() {
        mockMvc.perform(post("/admin/reset"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("ok"))
            .andDo { log.info { "POST /admin/reset → 200" } }

        mockMvc.perform(get("/jsonplaceholder/posts"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$.length()").value(greaterThan(0)))
            .andDo { log.info { "GET /jsonplaceholder/posts after reset → 200" } }
    }

    /**
     * E29: posts CRUD 왕복 테스트 — POST → GET → DELETE 흐름.
     */
    @Test
    @Order(8)
    fun `posts_crud_roundtrip`() {
        val body = jsonMapper.writeValueAsString(
            PostRecord(id = 0L, userId = 1L, title = "crud-title", body = "crud-body")
        )
        val mvcResult = mockMvc.perform(
            post("/jsonplaceholder/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        )
            .andExpect(status().isCreated)
            .andReturn()

        val created = jsonMapper.readValue(mvcResult.response.contentAsString, PostRecord::class.java)
        val id = created.id

        mockMvc.perform(get("/jsonplaceholder/posts/$id"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(id))

        mockMvc.perform(delete("/jsonplaceholder/posts/$id"))
            .andExpect(status().is2xxSuccessful)
    }

    /**
     * E30: comments CRUD 왕복 테스트.
     */
    @Test
    @Order(9)
    fun `comments_crud_roundtrip`() {
        val body = jsonMapper.writeValueAsString(
            CommentRecord(postId = 1L, id = 0L, name = "crud", email = "a@b.c", body = "hi")
        )
        val mvcResult = mockMvc.perform(
            post("/jsonplaceholder/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        )
            .andExpect(status().isCreated)
            .andReturn()

        val created = jsonMapper.readValue(mvcResult.response.contentAsString, CommentRecord::class.java)
        val id = created.id

        mockMvc.perform(get("/jsonplaceholder/comments/$id"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(id))

        mockMvc.perform(delete("/jsonplaceholder/comments/$id"))
            .andExpect(status().is2xxSuccessful)
    }

    /**
     * E31: albums CRUD 왕복 테스트.
     */
    @Test
    @Order(10)
    fun `albums_crud_roundtrip`() {
        val body = jsonMapper.writeValueAsString(
            AlbumRecord(userId = 1L, id = 0L, title = "crud-album")
        )
        val mvcResult = mockMvc.perform(
            post("/jsonplaceholder/albums")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        )
            .andExpect(status().isCreated)
            .andReturn()

        val created = jsonMapper.readValue(mvcResult.response.contentAsString, AlbumRecord::class.java)
        val id = created.id

        mockMvc.perform(get("/jsonplaceholder/albums/$id"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(id))

        mockMvc.perform(delete("/jsonplaceholder/albums/$id"))
            .andExpect(status().is2xxSuccessful)
    }

    /**
     * E32: photos CRUD 왕복 테스트.
     */
    @Test
    @Order(11)
    fun `photos_crud_roundtrip`() {
        val body = jsonMapper.writeValueAsString(
            PhotoRecord(
                albumId = 1L,
                id = 0L,
                title = "crud-photo",
                url = "http://example.com/u.png",
                thumbnailUrl = "http://example.com/t.png",
            )
        )
        val mvcResult = mockMvc.perform(
            post("/jsonplaceholder/photos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        )
            .andExpect(status().isCreated)
            .andReturn()

        val created = jsonMapper.readValue(mvcResult.response.contentAsString, PhotoRecord::class.java)
        val id = created.id

        mockMvc.perform(get("/jsonplaceholder/photos/$id"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(id))

        mockMvc.perform(delete("/jsonplaceholder/photos/$id"))
            .andExpect(status().is2xxSuccessful)
    }

    /**
     * E33: todos CRUD 왕복 테스트.
     */
    @Test
    @Order(12)
    fun `todos_crud_roundtrip`() {
        val body = jsonMapper.writeValueAsString(
            TodoRecord(userId = 1L, id = 0L, title = "crud-todo", completed = false)
        )
        val mvcResult = mockMvc.perform(
            post("/jsonplaceholder/todos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        )
            .andExpect(status().isCreated)
            .andReturn()

        val created = jsonMapper.readValue(mvcResult.response.contentAsString, TodoRecord::class.java)
        val id = created.id

        mockMvc.perform(get("/jsonplaceholder/todos/$id"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(id))

        mockMvc.perform(delete("/jsonplaceholder/todos/$id"))
            .andExpect(status().is2xxSuccessful)
    }

    /**
     * E34: users CRUD 왕복 테스트.
     */
    @Test
    @Order(13)
    fun `users_crud_roundtrip`() {
        val body = jsonMapper.writeValueAsString(
            UserRecord(
                id = 0L,
                name = "crud-user",
                username = "crud",
                email = "c@u.io",
                phone = "010",
                website = "crud.dev",
            )
        )
        val mvcResult = mockMvc.perform(
            post("/jsonplaceholder/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        )
            .andExpect(status().isCreated)
            .andReturn()

        val created = jsonMapper.readValue(mvcResult.response.contentAsString, UserRecord::class.java)
        val id = created.id

        mockMvc.perform(get("/jsonplaceholder/users/$id"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(id))

        mockMvc.perform(delete("/jsonplaceholder/users/$id"))
            .andExpect(status().is2xxSuccessful)
    }
}
